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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dataone.client.auth.CertificateManager;
import org.dataone.mimemultipart.MultipartRequest;
import org.dataone.mimemultipart.MultipartRequestResolver;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.ExceptionHandler;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.xml.sax.SAXException;

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
	private static int MAX_UPLOAD_SIZE = 1000000000;

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

    
    /*
     * API Functions used as URL parameters
     */
    protected static final String FUNCTION_NAME_INSERT = "insert";
    protected static final String FUNCTION_NAME_UPDATE = "update";
    
    protected ServletContext servletContext;
    protected Logger logMetacat;
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
  
        	// initialize the session - three options
        	// #1
        	// load session from certificate in request
            session = CertificateManager.getInstance().getSession(request);
            
            // #2
            if (session == null) {
	        	// check for session-based certificate from the portal
            	try {
		        	String configurationFileName = servletContext.getInitParameter("oa4mp:client.config.file");
		        	String configurationFilePath = servletContext.getRealPath(configurationFileName);
		        	PortalCertificateManager portalManager = new PortalCertificateManager(configurationFilePath);
		        	logMetacat.debug("Initialized the PortalCertificateManager using config file: " + configurationFilePath);
		        	X509Certificate certificate = portalManager.getCertificate(request);
		        	logMetacat.debug("Retrieved certificate: " + certificate);
			    	PrivateKey key = portalManager.getPrivateKey(request);
			    	logMetacat.debug("Retrieved key: " + key);
			    	if (certificate != null && key != null) {
			        	request.setAttribute("javax.servlet.request.X509Certificate", certificate);
			        	logMetacat.debug("Added certificate to the request: " + certificate.toString());
			    	}
			    	
		            // reload session from certificate that we jsut set in request
		            session = CertificateManager.getInstance().getSession(request);
            	} catch (Throwable t) {
            		// don't require configured OAuth4MyProxy
            		logMetacat.error(t.getMessage(), t);
            	}
            }
            
            // #3
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
     * Parse the BaseException information for replication status failures if any
     * 
     * @return failure  the BaseException failure, one of it's subclasses, or null
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws JiBXException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws IOException 
     */
    protected BaseException collectReplicationStatus() 
        throws ServiceFailure, InvalidRequest, IOException, 
        InstantiationException, IllegalAccessException, JiBXException {
        
        BaseException failure = null;
        File tmpDir = getTempDirectory();
        MultipartRequest mr = null;
        Map<String, File> mmFileParts = null;
        File exceptionFile = null;
        InputStream exceptionFileStream = null;

        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Parsing BaseException from the mime multipart entity");

        // handle MMP inputs
        MultipartRequestResolver mrr = 
            new MultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE, 0);

        try {
            mr = mrr.resolveMultipart(request);
            logMetacat.debug("Resolved the replication status BaseException multipart request.");
            
        } catch (IOException e) {
            throw new ServiceFailure("4700", "Couldn't resolve the multipart request: " +
                e.getMessage());
            
        } catch (FileUploadException e) {
            throw new ServiceFailure("4700", "Couldn't resolve the multipart request: " +
                e.getMessage());
            
        } catch (Exception e) {
            throw new ServiceFailure("4700", "Couldn't resolve the multipart request: " +
                e.getMessage());
            
        }

        // get the map of file parts
        mmFileParts = mr.getMultipartFiles();
        
        if ( mmFileParts == null || mmFileParts.keySet() == null) {
            logMetacat.debug("BaseException for setReplicationStatus is null");            
        }
        
        multipartparams = mr.getMultipartParameters();
        exceptionFile = mmFileParts.get("failure");
        
        if ( exceptionFile != null && exceptionFile.length() > 0 ) {
            
            // deserialize the BaseException subclass
            exceptionFileStream = new FileInputStream(exceptionFile);
            try {
                failure = ExceptionHandler.deserializeXml(exceptionFileStream, 
                    "Replication failed for an unknown reason.");
                
            } catch (ParserConfigurationException e) {
                throw new ServiceFailure("4700", "Couldn't parse the replication failure exception: " +
                        e.getMessage());
                
            } catch (SAXException e) {
                throw new ServiceFailure("4700", "Couldn't traverse the replication failure exception: " +
                        e.getMessage());
                
            }
                
        }
        
        
        return failure;
        
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
     * Parse the replication policy document out of the mime-multipart form data
     * 
     * @return policy  the encoded policy
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws JiBXException
     */
    protected ReplicationPolicy collectReplicationPolicy() 
        throws ServiceFailure, InvalidRequest, IOException, InstantiationException, 
        IllegalAccessException, JiBXException {
        
        ReplicationPolicy policy = null;
        File tmpDir = getTempDirectory();
        MultipartRequest mr = null;
        Map<String, File> mmFileParts = null;
        File replPolicyFile = null;
        InputStream replPolicyStream = null;
        
        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Parsing ReplicationPolicy from the mime multipart entity");

        // handle MMP inputs
        MultipartRequestResolver mrr = 
            new MultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE, 0);
        
        try {
            mr = mrr.resolveMultipart(request);
            logMetacat.debug("Resolved the ReplicationPolicy multipart request.");
            
        } catch (IOException e) {
            throw new ServiceFailure("4882", "Couldn't resolve the multipart request: " +
                e.getMessage());
            
        } catch (FileUploadException e) {
            throw new ServiceFailure("4882", "Couldn't resolve the multipart request: " +
                e.getMessage());
            
        } catch (Exception e) {
            throw new ServiceFailure("4882", "Couldn't resolve the multipart request: " +
                e.getMessage());
            
        }
        
        // get the map of file parts
        mmFileParts = mr.getMultipartFiles();
        
        if ( mmFileParts == null || mmFileParts.keySet() == null) {
            throw new InvalidRequest("4883", "The multipart request must include " +
                "a file with the name 'policy'.");
            
        }
        
        multipartparams = mr.getMultipartParameters();
        replPolicyFile = mmFileParts.get("policy");
        
        if ( replPolicyFile == null ) {
            throw new InvalidRequest("4883", "The multipart request must include " +
            "a file with the name 'policy'.");
            
        }
        
        
        // deserialize the ReplicationPolicy
        replPolicyStream = new FileInputStream(replPolicyFile);
        policy = TypeMarshaller.unmarshalTypeFromStream(ReplicationPolicy.class, replPolicyStream);
        
        return policy;
        
    }

    /**
     * Parse the replica metadata document out of the mime-multipart form data
     * 
     * @return replica  the encoded replica
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws JiBXException
     */
    protected Replica collectReplicaMetadata() 
        throws ServiceFailure, InvalidRequest {
        
        Replica replica = null;
        File tmpDir = getTempDirectory();
        MultipartRequest mr = null;
        Map<String, File> mmFileParts = null;
        File replicaFile = null;
        InputStream replicaStream = null;
        
        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Parsing Replica from the mime multipart entity");

        // handle MMP inputs
        MultipartRequestResolver mrr = 
            new MultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE, 0);
        
        try {
            mr = mrr.resolveMultipart(request);
            logMetacat.debug("Resolved the Replica multipart request.");
            
        } catch (IOException e) {
            throw new ServiceFailure("4852", "Couldn't resolve the multipart request: " +
                e.getMessage());
            
        } catch (FileUploadException e) {
            throw new ServiceFailure("4852", "Couldn't resolve the multipart request: " +
                    e.getMessage());
            
        } catch (Exception e) {
            throw new ServiceFailure("4852", "Couldn't resolve the multipart request: " +
                    e.getMessage());
            
        }
        
        // get the map of file parts
        mmFileParts = mr.getMultipartFiles();
        
        if ( mmFileParts == null || mmFileParts.keySet() == null) {
            throw new InvalidRequest("4853", "The multipart request must include " +
                "a file with the name 'replicaMetadata'.");
            
        }
        
        multipartparams = mr.getMultipartParameters();
        replicaFile = mmFileParts.get("replicaMetadata");
        
        if ( replicaFile == null ) {
            throw new InvalidRequest("4853", "The multipart request must include " +
            "a file with the name 'replicaMetadata'.");
            
        }
        
        
        // deserialize the ReplicationPolicy
        try {
            replicaStream = new FileInputStream(replicaFile);
        } catch (FileNotFoundException e) {
            throw new ServiceFailure("4852", "Couldn't find the multipart file: " +
                    e.getMessage());
            
        }
        
        try {
            replica = TypeMarshaller.unmarshalTypeFromStream(Replica.class, replicaStream);
        } catch (IOException e) {
            throw new ServiceFailure("4852", "Couldn't deserialize the replica document: " +
                    e.getMessage());
            
        } catch (InstantiationException e) {
            throw new ServiceFailure("4852", "Couldn't deserialize the replica document: " +
                    e.getMessage());
            
        } catch (IllegalAccessException e) {
            throw new ServiceFailure("4852", "Couldn't deserialize the replica document: " +
                    e.getMessage());
            
        } catch (JiBXException e) {
            throw new ServiceFailure("4852", "Couldn't deserialize the replica document: " +
                    e.getMessage());
            
        }
        
        return replica;
        
    }
    
    protected AccessPolicy collectAccessPolicy() 
        throws IOException, ServiceFailure, InvalidRequest, JiBXException, 
        InstantiationException, IllegalAccessException, ParserConfigurationException, 
        SAXException  {
		
		// Read the incoming data from its Mime Multipart encoding
		logMetacat.debug("Disassembling MIME multipart form");
		InputStream ap = null;

		// handle MMP inputs
		File tmpDir = getTempDirectory();
		logMetacat.debug("temp dir: " + tmpDir.getAbsolutePath());
		MultipartRequestResolver mrr = 
			new MultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE, 0);
		MultipartRequest mr = null;
		try {
			mr = mrr.resolveMultipart(request);
		} catch (Exception e) {
			throw new ServiceFailure("2161", 
					"Could not resolve multipart: " + e.getMessage());
		}
		logMetacat.debug("resolved multipart request");
		Map<String, File> files = mr.getMultipartFiles();
		if (files == null || files.keySet() == null) {
			throw new InvalidRequest("2163",
					"must have multipart file with name 'accessPolicy'");
		}
		logMetacat.debug("got multipart files");

		multipartparams = mr.getMultipartParameters();

		File apFile = files.get("accessPolicy");
		if (apFile == null) {
			throw new InvalidRequest("2163",
					"Missing the required file-part 'accessPolicy' from the multipart request.");
		}
		logMetacat.debug("apFile: " + apFile.getAbsolutePath());
		ap = new FileInputStream(apFile);
	
		AccessPolicy accessPolicy = TypeMarshaller.unmarshalTypeFromStream(AccessPolicy.class, ap);
		return accessPolicy;
	}
    
    protected SystemMetadata collectSystemMetadata() 
        throws IOException, FileUploadException, ServiceFailure, InvalidRequest, 
        JiBXException, InstantiationException, IllegalAccessException  {
		
		// Read the incoming data from its Mime Multipart encoding
		logMetacat.debug("Disassembling MIME multipart form");
		InputStream sysmeta = null;

		// handle MMP inputs
		File tmpDir = getTempDirectory();
		logMetacat.debug("temp dir: " + tmpDir.getAbsolutePath());
		MultipartRequestResolver mrr = 
			new MultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE, 0);
		MultipartRequest mr = null;
		try {
			mr = mrr.resolveMultipart(request);
			
		} catch (Exception e) {
		  if ( logMetacat.isDebugEnabled() ) {
		      e.printStackTrace();
		      
		  }
			throw new ServiceFailure("1202", 
					"Could not resolve multipart: " + e.getMessage());
			
		}
		logMetacat.debug("resolved multipart request");
		Map<String, File> files = mr.getMultipartFiles();
		if (files == null) {
			throw new ServiceFailure("1202",
					"register meta must have multipart file with name 'sysmeta'");
		}
		logMetacat.debug("got multipart files");

		if (files.keySet() == null) {
			logMetacat.error("No file keys in MMP request.");
			throw new ServiceFailure(
					"1202",
					"No file keys found in MMP.  "
							+ "register meta must have multipart file with name 'sysmeta'");
		}

		// for logging purposes, dump out the key-value pairs that
		// constitute the request
		// 3 types exist: request params, multipart params, and
		// multipart files
		Iterator it = files.keySet().iterator();
		logMetacat.debug("iterating through request parts: " + it);
		while (it.hasNext()) {
			String key = (String) it.next();
			logMetacat.debug("files key: " + key);
			logMetacat.debug("files value: " + files.get(key));
		}

		multipartparams = mr.getMultipartParameters();
		it = multipartparams.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			logMetacat.debug("multipartparams key: " + key);
			logMetacat.debug("multipartparams value: " + multipartparams.get(key));
		}

		it = params.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			logMetacat.debug("param key: " + key);
			logMetacat.debug("param value: " + params.get(key));
		}
		logMetacat.debug("done iterating the request...");

		File smFile = files.get("sysmeta");
		if (smFile == null) {
			throw new InvalidRequest("1102",
					"Missing the required file-part 'sysmeta' from the multipart request.");
		}
		logMetacat.debug("smFile: " + smFile.getAbsolutePath());
		sysmeta = new FileInputStream(smFile);
	
		logMetacat.debug("Commence creation...");
		SystemMetadata systemMetadata = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmeta);
		return systemMetadata;
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
        	new MultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE, 0);
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
			new MultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE, 0);
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
        
        logMetacat.error("D1ResourceHandler: Serializing exception with code " + e.getCode() + ": " + e.getMessage());
        e.printStackTrace();
        
        try {
            IOUtils.write(e.serialize(BaseException.FMT_XML), out);
        } catch (IOException e1) {
            logMetacat.error("Error writing exception to stream. " 
                    + e1.getMessage());
        }
    }

}
