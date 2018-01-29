/**
 *  '$RCSfile$'
 *  Copyright: 2011 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: Serhan AKIN $'
 *     '$Date: 2009-06-13 15:28:13 +0300  $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ucsb.nceas.metacat.restservice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dataone.mimemultipart.MultipartRequest;
import org.dataone.mimemultipart.MultipartRequestResolver;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;

import edu.ucsb.nceas.metacat.AuthSession;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.SessionService;
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
    
    protected ServletContext servletContext;
    protected static Logger logMetacat;
    protected MetacatHandler handler;
    protected HttpServletRequest request;
    protected HttpServletResponse response;

    protected Hashtable<String, String[]> params;
    protected Map<String, List<String>> multipartparams;
    
    // D1 certificate-based authentication
    protected Session session;

    /**Initializes new instance by setting servlet context,request and response*/
    public D1ResourceHandler(ServletContext servletContext,
            HttpServletRequest request, HttpServletResponse response) {
        this.servletContext = servletContext;
        this.request = request;
        this.response = response;
        logMetacat = Logger.getLogger(D1ResourceHandler.class);
		try {
			MAX_UPLOAD_SIZE = Integer.parseInt(PropertyService.getProperty("dataone.max_upload_size"));
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
        logMetacat = Logger.getLogger(D1ResourceHandler.class);
        try {
  
        	// first try the usual methods
        	session = PortalCertificateManager.getInstance().getSession(request);
        	
            // last resort, check for Metacat sessionid
            if (session == null) {
	            SessionData sessionData = RequestUtil.getSessionData(request);
				if (sessionData != null) {
					// is it not the public session?
					if (!SessionService.getInstance().getPublicSession().getUserName().equals(sessionData.getUserName())) {
						session = new Session();
						String userName = sessionData.getUserName();
						String[] groupNames = sessionData.getGroupNames();
						Subject userSubject = new Subject();
						userSubject.setValue(userName);
						session.setSubject(userSubject);
						SubjectInfo subjectInfo = new SubjectInfo();
						Person person = new Person();
						person.setSubject(userSubject);
						if (groupNames != null && groupNames.length > 0) {
							for (String groupName: groupNames) {
								Group group = new Group();
								group.setGroupName(groupName);
								Subject groupSubject = new Subject();
								groupSubject.setValue(groupName);
								group.setSubject(groupSubject);
								subjectInfo.addGroup(group);
								person.addIsMemberOf(groupSubject);
							}
						}
						subjectInfo.addPerson(person);
						session.setSubjectInfo(subjectInfo);
					}
				}
            } else {
                //The session is not null. However, the if we got the session is from a token, the ldap group information for is missing if we logged in by the ldap account.
                //here we just patch it.
                Subject subject = session.getSubject();
                if(subject != null) {
                    String dn = subject.getValue();
                    logMetacat.debug("D1ReourceHandler.handle - the subject dn in the session is "+dn+" This dn will be used to look up the group information");
                    if(dn != null) {
                        String username = null;
                        String password = null;
                       
                        String[] groups = null;
                        try {
                            AuthSession auth = new AuthSession();
                            groups = auth.getGroups(username, password, dn);
                        } catch (Exception e) {
                            logMetacat.warn("D1ReourceHandler.handle - we can't get group information for the user "+dn+" from the authentication interface since :", e);
                        }

                        if(groups != null) {
                            SubjectInfo subjectInfo = session.getSubjectInfo();
                            if(subjectInfo != null) {
                                logMetacat.debug("D1ReourceHandler.handle - the subject information is NOT null when we try to figure out the group information.");
                                //we don't overwrite the existing subject info, just add the new groups informations
                                List<Person> persons = subjectInfo.getPersonList();
                                Person targetPerson = null;
                                if(persons != null) {
                                    for(Person person : persons) {
                                        if(person.getSubject().equals(subject)) {
                                            targetPerson = person;
                                            logMetacat.debug("D1ReourceHandler.handle - we find a person with the subject "+dn+" in the subject info.");
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
                                    logMetacat.debug("D1ReourceHandler.handle - create the group "+groups[i]+" for an existing subject info.");
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
                                logMetacat.debug("D1ReourceHandler.handle - the subject information is NOT null when we try to figure out the group information.");
                                subjectInfo = new SubjectInfo();
                                Person person = new Person();
                                person.setSubject(subject);
                                for (int i=0; i<groups.length; i++) {
                                    logMetacat.debug("D1ReourceHandler.handle - create the group "+groups[i]+" for a new subject info.");
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
            }
			
            // initialize the parameters
            params = new Hashtable<String, String[]>();
            initParams();

            // create the handler for interacting with Metacat
            Timer timer = new Timer();
            handler = new MetacatHandler(timer);

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
        
        if (files.keySet() == null) {
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
	            logMetacat.debug("param value: " + params.get(key));
	        }
	        logMetacat.debug("done iterating the request...");
        }
        
        return files;
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
     * locate the boundary marker for an MMP
     * @param is
     * @return
     * @throws IOException
     */
    protected static String[] findBoundaryString(InputStream is)
        throws IOException {
        String[] endResult = new String[2];
        String boundary = "";
        String searchString = "boundary=";
        byte[] b = new byte[1024];
        int numbytes = is.read(b, 0, 1024);

        while(numbytes != -1)
        {
            String s = new String(b, 0, numbytes);
            int searchStringIndex = s.indexOf(searchString);
            
            if(s.indexOf("\"", searchStringIndex + searchString.length() + 1) == -1)
            { //the end of the boundary is in the next byte array
                boundary = s.substring(searchStringIndex + searchString.length() + 1, s.length());
            }
            else if(!boundary.startsWith("--"))
            { //we can read the whole boundary from this byte array
                boundary = s.substring(searchStringIndex + searchString.length() + 1, 
                    s.indexOf("\"", searchStringIndex + searchString.length() + 1));
                boundary = "--" + boundary;
                endResult[0] = boundary;
                endResult[1] = s.substring(s.indexOf("\"", searchStringIndex + searchString.length() + 1) + 1,
                        s.length());
                break;
            }
            else
            { //we're now reading the 2nd byte array to get the rest of the boundary
                searchString = "\"";
                searchStringIndex = s.indexOf(searchString);
                boundary += s.substring(0, searchStringIndex);
                boundary = "--" + boundary;
                endResult[0] = boundary;
                endResult[1] = s.substring(s.indexOf("\"", searchStringIndex + searchString.length() + 1) + 1,
                        s.length());
                break;
            }
        }
        return endResult;
    }
    
    /**
     * return the directory where temp files are stored
     * @return
     */
    protected static File getTempDirectory()
    {
        File tmpDir = null;
        Logger logMetacat = Logger.getLogger(D1ResourceHandler.class);
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
            e.printStackTrace();
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
        if( e instanceof NotFound) {
            logMetacat.info("D1ResourceHandler: Serializing exception with code " + e.getCode() + ": " + e.getMessage());
        } else {
            logMetacat.error("D1ResourceHandler: Serializing exception with code " + e.getCode() + ": " + e.getMessage(), e);
        }
        //e.printStackTrace();
        
        try {
            IOUtils.write(e.serialize(BaseException.FMT_XML), out);
        } catch (IOException e1) {
            logMetacat.error("Error writing exception to stream. " 
                    + e1.getMessage());
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
            try
            {
                result = URLDecoder.decode(s, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                result = URLDecoder.decode(s);
            }
            logMetacat.info("D1ResourceHandler.decode - the string after decoding is "+result);
            System.out.println("After decoded: " + result);
        }
        
        return result;
    }
}
