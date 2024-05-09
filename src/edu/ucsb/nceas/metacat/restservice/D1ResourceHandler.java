package edu.ucsb.nceas.metacat.restservice;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.exceptions.MarshallingException;
import org.dataone.mimemultipart.MultipartRequest;
import org.dataone.mimemultipart.MultipartRequestResolver;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;

import edu.ucsb.nceas.metacat.AuthSession;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.multipart.MultipartRequestWithSysmeta;
import edu.ucsb.nceas.metacat.restservice.multipart.StreamingMultipartRequestResolver;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SessionData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
/**
 * 
 * Base class for handling D1 REST calls in Metacat
 * 
 * @author leinfelder
 */
public class D1ResourceHandler {

    /**HTTP Verb GET*/
    public static final byte GET = 1;
    /**HTTP Verb POST*/
    public static final byte POST = 2;
    /**HTTP Verb PUT*/
    public static final byte PUT = 3;
    /**HTTP Verb DELETE*/
    public static final byte DELETE = 4;
    /**HTTP Verb HEAD*/
    public static final byte HEAD = 5;

    /** Maximum size of uploads, defaults to 1GB if not set in property file */
    protected static int MAX_UPLOAD_SIZE = 1000000000;

    /*
     * API Resources
     */
    protected static final String RESOURCE_BASE_URL = "d1";

    protected static final String RESOURCE_OBJECTS = "object";
    protected static final String RESOURCE_META = "meta";
    protected static final String RESOURCE_LOG = "log";

    protected static final String RESOURCE_QUERY = "query";

    protected static final String RESOURCE_IS_AUTHORIZED = "isAuthorized";
    protected static final String RESOURCE_ACCESS_RULES = "accessRules";

    protected static final String RESOURCE_VIEWS = "views";


    /*
     * API Functions used as URL parameters
     */
    protected static final String FUNCTION_NAME_INSERT = "insert";
    protected static final String FUNCTION_NAME_UPDATE = "update";

    protected static AuthSession auth = null;
    protected static int authCacheSize = Settings.getConfiguration().getInt("auth.groupCacheSize", 100);
    protected static boolean enableAppendLdapGroups =
            Settings.getConfiguration().getBoolean("dataone.session.appendLdapGroups.enabled", true);

    private static Log logMetacat = LogFactory.getLog(D1ResourceHandler.class);
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected boolean enableSessionFromHeader = false;
    protected String proxyKey = null;

    protected Hashtable<String, String[]> params;
    protected Map<String, List<String>> multipartparams;

    // D1 certificate-based authentication
    protected Session session;

    /**Initializes new instance by setting servlet context,request and response
     * @param request  the request that the handler will handle
     * @param response  the response that the handler will send back
     */
    public D1ResourceHandler(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        try {
            MAX_UPLOAD_SIZE = Integer.parseInt(PropertyService.getProperty("dataone.max_upload_size"));
            enableSessionFromHeader = Boolean.parseBoolean(PropertyService.getProperty("dataone.certificate.fromHttpHeader.enabled"));
            proxyKey = PropertyService.getProperty("dataone.certificate.fromHttpHeader.proxyKey");
        } catch (PropertyNotFoundException e) {
            // Just use our default as no max size is set in the properties file
            logMetacat.warn("Property not found: " + "dataone.max_upload_size");
        }

    }

    /**
     * This function is called from REST API servlet and handles each request
     *
     * @param httpVerb (GET, POST, PUT or DELETE)
     */
    public void handle(byte httpVerb) {
        logMetacat = LogFactory.getLog(D1ResourceHandler.class);
        try {

            // first try the usual methods
            session = PortalCertificateManager.getInstance().getSession(request);

            // last resort, check for Metacat sessionid
            if (session == null) {
                SessionData sessionData = RequestUtil.getSessionData(request);
                if (sessionData != null) {
                    // is it not the public session?
                    if (!SessionService.getInstance().getPublicSession().getUserName().equals(sessionData.getUserName())) {
                        String userName = sessionData.getUserName();
                        String[] groupNames = sessionData.getGroupNames();
                        session = AuthUtil.buildSession(userName, groupNames);
                    }
                }
            } else {
                //The session is not null. However, if we got the session is from a token, the local
                //ldap group information is missing when we logged in by the ldap account.
                //Here we just patch it (d1_portal only patches the dataone groups)
                if (enableAppendLdapGroups) {
                    logMetacat.debug("Metacat is configured to append the"
                                    + " local ldap group information to a session.");
                    Subject subject = session.getSubject();
                    if(subject != null) {
                        String dn = subject.getValue();
                        logMetacat.debug("The subject dn in the session is "
                                + dn + " This dn will be used to look up the group information");
                        if(dn != null) {
                            String username = null;
                            String password = null;
                            String[] groups = null;
                            if (auth == null) {
                                try {
                                    synchronized (D1ResourceHandler.class) {
                                        if (auth == null) {
                                            auth = new AuthSession(authCacheSize);
                                        }
                                    }
                                    groups = auth.getGroups(username, password, dn);
                               } catch (Exception e) {
                                   logMetacat.warn("We can't get group information for the user "
                                             + dn + " from the authentication interface since: ", e);
                               }
                            } else {
                                try {
                                    groups = auth.getGroups(username, password, dn);
                               } catch (Exception e) {
                                   logMetacat.warn("We can't get group information for the user "
                                           + dn + " from the authentication interface since: ", e);
                               }
                            }

                            if(groups != null) {
                                SubjectInfo subjectInfo = session.getSubjectInfo();
                                if(subjectInfo != null) {
                                    logMetacat.debug("D1ResourceHandler.handle - the subject "
                                                     + "information is NOT null when we try to "
                                                     + "figure out the group information.");
                                    //we don't overwrite the existing subject info, just add the new groups informations
                                    List<Person> persons = subjectInfo.getPersonList();
                                    Person targetPerson = null;
                                    if(persons != null) {
                                        for(Person person : persons) {
                                            if(person.getSubject().equals(subject)) {
                                                targetPerson = person;
                                                logMetacat.debug("We find a person with the subject "
                                                                 + dn + " in the subject info.");
                                                break;
                                            }
                                        }
                                    }
                                    boolean newPerson = false;
                                    if(targetPerson == null) {
                                        newPerson = true;
                                        targetPerson = new Person();
                                        targetPerson.setSubject(subject);
                                    }
                                    for (int i=0; i<groups.length; i++) {
                                        logMetacat.debug("D1ReourceHandler.handle - create the group "
                                                  + groups[i] + " for an existing subject info.");
                                        Group group = new Group();
                                        group.setGroupName(groups[i]);
                                        Subject groupSubject = new Subject();
                                        groupSubject.setValue(groups[i]);
                                        group.setSubject(groupSubject);
                                        subjectInfo.addGroup(group);
                                        targetPerson.addIsMemberOf(groupSubject);
                                    }
                                    if(newPerson) {
                                        subjectInfo.addPerson(targetPerson);
                                    }
                                } else {
                                    logMetacat.debug("The subject information is NOT null when we "
                                                    + "try to figure out the group information.");
                                    subjectInfo = new SubjectInfo();
                                    Person person = new Person();
                                    person.setSubject(subject);
                                    for (int i=0; i<groups.length; i++) {
                                        logMetacat.debug("Create the group " + groups[i]
                                                        + " for a new subject info.");
                                        Group group = new Group();
                                        group.setGroupName(groups[i]);
                                        Subject groupSubject = new Subject();
                                        groupSubject.setValue(groups[i]);
                                        group.setSubject(groupSubject);
                                        subjectInfo.addGroup(group);
                                        person.addIsMemberOf(groupSubject);
                                    }
                                    subjectInfo.addPerson(person);
                                    session.setSubjectInfo(subjectInfo);
                                }
                            }
                        }
                    }
                } else {
                    logMetacat.debug("Metacat is configured NOT to append the local ldap group "
                                     + "information to a session.");
                }

            }

            if (session == null) {
                // If certificate or token sessions are not established, get a session object from values in the request headers,
                // but only if this feature is enabled in the metacat.properties file
                getSessionFromHeader();
            }

            // initialize the parameters
            params = new Hashtable<String, String[]>();
            initParams();
        } catch (Exception e) {
            // TODO: more D1 structure when failing here
            response.setStatus(400);
            printError("Incorrect resource!", response);
            logMetacat.error(e.getClass() + ": " + e.getMessage(), e);
        }
    }


    /**
     * subclasses should provide a more useful implementation
     * @return
     */
    protected boolean isD1Enabled() {

        return true;
    }

    protected String parseTrailing(String resource, String token) {
        // get the rest
        String extra = null;
        if (resource.indexOf(token) != -1) {
            // what comes after the token?
            extra = resource.substring(resource.indexOf(token) + token.length());
            // remove the slash
            if (extra.startsWith("/")) {
                extra = extra.substring(1);
            }
            // is there anything left?
            if (extra.length() == 0) {
                extra = null;
            }
        }
        return extra;
    }

    /**
     * Parse string parameters from the mime multipart entity of the request.
     * Populates the multipartparams map
     * 
     * @throws IOException
     * @throws FileUploadException
     * @throws Exception
     */
    protected void collectMultipartParams() 
        throws IOException, FileUploadException, Exception {

        File tmpDir = getTempDirectory();
        MultipartRequest mr = null;

        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Parsing rights holder info from the mime multipart entity");

        // handle MMP inputs
        MultipartRequestResolver mrr = 
            new MultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE, 0);

        mr = mrr.resolveMultipart(request);
        logMetacat.debug("Resolved the rights holder info from the mime multipart entity.");

        // we only have params in this MMP entity
        multipartparams = mr.getMultipartParameters();

    }

    /**
     * Process the MMP request that includes files for each param
     * @return map of param key and the temp file that contains the encoded information
     * @throws ServiceFailure
     * @throws InvalidRequest
     */
    protected Map<String, File> collectMultipartFiles() 
        throws ServiceFailure, InvalidRequest {

        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Disassembling MIME multipart form");

        // handle MMP inputs
        File tmpDir = getTempDirectory();
        logMetacat.debug("temp dir: " + tmpDir.getAbsolutePath());
        MultipartRequestResolver mrr = 
            new MultipartRequestResolver(tmpDir.getAbsolutePath(),  MAX_UPLOAD_SIZE, 0);
        MultipartRequest mr = null;
            try {
                  mr = mrr.resolveMultipart(request);

            } catch (Exception e) {
                throw new ServiceFailure("1202", 
                        "Could not resolve multipart files: " + e.getMessage());
            }
        logMetacat.debug("resolved multipart request");
        Map<String, File> files = mr.getMultipartFiles();
        if (files == null) {
            throw new ServiceFailure("1202", "no multipart files found");
        }
        logMetacat.debug("got multipart files");

        if (files.keySet().isEmpty()) {
            logMetacat.error("No file keys in MMP request.");
            throw new ServiceFailure("1202", "No file keys found in MMP.");
        }

        multipartparams = mr.getMultipartParameters();

            // for logging purposes, dump out the key-value pairs that constitute the request
            // 3 types exist: request params, multipart params, and multipart files
        if (logMetacat.isDebugEnabled()) {
            Iterator<String> it = files.keySet().iterator();
            logMetacat.debug("iterating through files");
            while (it.hasNext()) {
                String key = it.next();
                logMetacat.debug("files key: " + key);
                logMetacat.debug("files value: " + files.get(key));
            }

            it = multipartparams.keySet().iterator();
            logMetacat.debug("iterating through multipartparams");
            while (it.hasNext()) {
                String key = (String)it.next();
                logMetacat.debug("multipartparams key: " + key);
                logMetacat.debug("multipartparams value: " + multipartparams.get(key));
            }

            it = params.keySet().iterator();
            logMetacat.debug("iterating through params");
            while (it.hasNext()) {
                String key = (String)it.next();
                logMetacat.debug("param key: " + key);
                logMetacat.debug("param value: " + Arrays.toString(params.get(key)));
            }
            logMetacat.debug("done iterating the request...");
        }

        return files;
    }

    /**
     * Parse the request by the streaming multiple part handler. This method is good for the
     * cn.create and mn.create/update methods.
     * @return  the MultipartRequestWithSysmeta object which includes the stored object file
     *           with its checksum and the system metadata about this object
     * @throws IOException
     * @throws FileUploadException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws NoSuchAlgorithmException
     * @throws MarshallingException
     */
    protected MultipartRequestWithSysmeta collectObjectFiles() throws IOException,
                             FileUploadException, InstantiationException, IllegalAccessException,
                             NoSuchAlgorithmException, MarshallingException {
        logMetacat.debug("Disassembling MIME multipart form with object files");
        // handle MMP inputs
        File tmpDir = getTempDirectory();
        logMetacat.debug("temp dir: " + tmpDir.getAbsolutePath());
        StreamingMultipartRequestResolver resolver = new StreamingMultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE);
        MultipartRequestWithSysmeta mq = null;
        mq = (MultipartRequestWithSysmeta)resolver.resolveMultipart(request);
        multipartparams = mq.getMultipartParameters();
        return mq;
    }

        /**
     *  copies request parameters to a hashtable which is given as argument to 
     *  native metacathandler functions  
     */
    protected void initParams() {

        String name = null;
        String[] value = null;
        Enumeration paramlist = request.getParameterNames();
        while (paramlist.hasMoreElements()) {
            name = (String) paramlist.nextElement();
            value = request.getParameterValues(name);
            params.put(name, value);
        }
    }

    /**
     * Collect the multipart params from the request
     * @throws Exception
     */
    protected void initMultipartParams() throws Exception {

        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Disassembling MIME multipart form");

        // handle MMP inputs
        File tmpDir = getTempDirectory();
        logMetacat.debug("temp dir: " + tmpDir.getAbsolutePath());
        MultipartRequestResolver mrr =
            new MultipartRequestResolver(tmpDir.getAbsolutePath(),  MAX_UPLOAD_SIZE, 0);
        MultipartRequest mr = mrr.resolveMultipart(request);

        multipartparams = mr.getMultipartParameters();
    }


    /**
     * return the directory where temp files are stored
     * @return
     */
    protected static File getTempDirectory()
    {
        File tmpDir = null;
        Log logMetacat = LogFactory.getLog(D1ResourceHandler.class);
        try {
            tmpDir = new File(PropertyService.getProperty("application.tempDir"));
        }
        catch(PropertyNotFoundException pnfe) {
            logMetacat.error("D1ResourceHandler.writeMMPPartstoFiles: " +
                    "application.tmpDir not found.  Using /tmp instead.");
            tmpDir = new File("/tmp");
        }
        return tmpDir;
    }

    /**
     * Prints xml response
     * @param message Message to be displayed
     * @param response Servlet response that xml message will be printed
     * */
    protected void printError(String message, HttpServletResponse response) {
        try {
            logMetacat.error("D1ResourceHandler: Printing error to servlet response: " + message);
            PrintWriter out = response.getWriter();
            response.setContentType("text/xml");
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println(message);
            out.println("</error>");
            out.close();
        } catch (IOException e) {
            logMetacat.error("Can't send back the error message since: " + e.getMessage());
        }
    }

    /**
     * serialize a D1 exception using jibx
     * @param e
     * @param out
     */
    protected void serializeException(BaseException e, OutputStream out) {
        // TODO: Use content negotiation to determine which return format to use
        response.setContentType("text/xml");
        response.setStatus(e.getCode());
        if (e instanceof NotFound || e instanceof NotAuthorized || e instanceof InvalidRequest
                                                                  || e instanceof InvalidToken) {
            logMetacat.info("D1ResourceHandler: Serializing exception with code "
                            + e.getCode() + ": " + e.getMessage());
        } else {
            logMetacat.error("D1ResourceHandler: Serializing exception with code "
                            + e.getCode() + ": " + e.getMessage(), e);
        }
        try {
            IOUtils.write(e.serialize(BaseException.FMT_XML), out, StandardCharsets.UTF_8);
        } catch (IOException e1) {
            logMetacat.error("Error writing exception to stream. " 
                    + e1.getMessage());
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * A method to decode the given string which is a part of a uri.
     * The default encoding is utf-8. If the utf-8 is not support in this system, the default one in the systme will be used.
     * @param s
     * @return null if the given string is null
     */
    public static String decode(String s) {
        String result = null;
        if(s != null) {
            try {
                result = URLDecoder.decode(s, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                try {
                    result = URLDecoder.decode(s, System.getProperty("file.encoding"));
                } catch (UnsupportedEncodingException ee) {
                    logMetacat.error("Can't decode the uri since " + ee.getMessage());
                }
            }
            logMetacat.info("D1ResourceHandler.decode - the string after decoding is " + result);
        }

        return result;
    }

    /**
     * Get the session from the header of the request.
     * This mechanism is disabled by default due to network security conditions needed for it to be secure
     *
     */
    protected void getSessionFromHeader() {
        if (enableSessionFromHeader) {
            logMetacat.debug("In the route to get the session from a http header");
            //check the shared key between Metacat and the http server:
            if (proxyKey == null || proxyKey.trim().equals("")) {
                logMetacat.warn("Metacat is not configured to handle the feature passing "
                                + " the certificate by headers since the proxy key is blank");
                return;
            }
            String proxyKeyFromHttp = (String) request.getHeader("X-Proxy-Key");
            if (proxyKeyFromHttp == null || proxyKeyFromHttp.trim().equals("")) {
                logMetacat.warn("The value of the header X-Proxy-Key is null or blank. "
                                 + "So Metacat do NOT trust the request.");
                return;
            }
            if (!proxyKey.equals(proxyKeyFromHttp)) {
                logMetacat.warn("The value of the header X-Proxy-Key does not match the one "
                                + " stored in Metacat. So Metacat do NOT trust the request.");
                return;
            }

            String verify = (String) request.getHeader("Ssl-Client-Verify");
            logMetacat.info("D1ResourceHandler.getSessionFromHeader - the status of the ssl client "
                            + "verification is " + verify);
            if (verify != null && verify.equalsIgnoreCase("SUCCESS")) {
                //Metacat only looks up the dn from the header when the ssl client was verified.
                //We confirmed the client couldn't overwrite the value of the header Ssl-Client-Subject-Dn
                String dn = (String) request.getHeader("Ssl-Client-Subject-Dn");
                logMetacat.info("The ssl client was verified and the subject from the header is " + dn);
                if (dn != null) {
                    Subject subject = new Subject();
                    subject .setValue(dn);
                    session = new Session();
                    session.setSubject(subject);

                    SubjectInfo subjectInfo = null;
                    try {
                        subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
                    } catch (Exception be) {
                        logMetacat.warn("Can not get subject information for subject" + dn
                                        + " since " + be.getMessage());
                    }
                    if (subjectInfo == null) {
                        subjectInfo = new SubjectInfo();
                        Person person = new Person();
                        person.setSubject(subject);
                        person.setFamilyName("Unknown");
                        person.addGivenName("Unknown");
                        subjectInfo.setPersonList(Arrays.asList(person));
                    }
                    session.setSubjectInfo(subjectInfo);
                }
            }
        }
    }
}
