package edu.ucsb.nceas.metacat.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsb.nceas.utilities.IOUtil;
import edu.ucsb.nceas.utilities.XMLUtilities;
import java.io.File;


/**
 *  This interface provides methods for initializing and logging in to a
 *  Metacat server, and then querying, reading, transforming, inserting,
 *  updating and deleting documents from that server.
 */
public class MetacatClient implements Metacat {
    /** The URL string for the metacat server */
    private String metacatUrl;
    
    /** The session identifier for the session */
    private String sessionId;
    
    /** The default character encoding, can be changed by client */
    private String encoding = "UTF-8";
    
    public static void main(String[] args) {
    	try {
    		Metacat mc = 
    			MetacatFactory.createMetacatConnection(args[0]);
    		
    		InputStream r = mc.read(args[1]);
    		FileOutputStream fos = new FileOutputStream(args[2]);
    		BufferedOutputStream bfos = new BufferedOutputStream(fos);

            int c = r.read();
            while(c != -1)
            {
              bfos.write(c);
              c = r.read();
            }
            bfos.flush();
            bfos.close();
            fos.flush();
            fos.close();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    /**
     * Constructor to create a new instance. Protected because instances
     * should only be created by the factory MetacatFactory.
     */
    protected MetacatClient() {
        this.metacatUrl = null;
        this.sessionId = null;
    }
    
    /**
     *  Method used to log in to a metacat server. Implementations will need
     *  to cache a cookie value to make the session persistent.  Each time a
     *  call is made to one of the other methods (e.g., read), the cookie will
     *  need to be passed back to the metacat server along with the request.
     *
     *  @param username   the username of the user, like an LDAP DN
     *  @param password   the password for that user for authentication
     *  @return the response string from metacat in XML format
     *  @throws MetacatAuthException when the username/password could
     *                    not be authenticated
     */
    public String login(String username, String password)
    throws MetacatAuthException, MetacatInaccessibleException {
        Properties prop = new Properties();
        prop.put("action", "login");
        prop.put("qformat", "xml");
        prop.put("username", username);
        prop.put("password", password);
//        if (this.sessionId != null) {
//        	prop.put("sessionid", sessionId);
//        }
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        if (response.indexOf("<login>") == -1) {
            setSessionId("");
            throw new MetacatAuthException(response);
        } else {
            int start = response.indexOf("<sessionId>") + 11;
            int end = response.indexOf("</sessionId>");
            if ((start != -1) && (end != -1)) {
                setSessionId(response.substring(start,end));
            }
        }
        return response;
    }
    
    /**
     *  Method used to log in to a metacat server. Implementations will need
     *  to cache a cookie value to make the session persistent.  Each time a
     *  call is made to one of the other methods (e.g., read), the cookie will
     *  need to be passed back to the metacat server along with the request.
     *
     *  @param username   the username of the user, like an LDAP DN
     *  @param password   the password for that user for authentication
     *  @return the response string from metacat in XML format
     *  @throws MetacatAuthException when the username/password could
     *                    not be authenticated
     */
    public String getloggedinuserinfo() throws MetacatInaccessibleException {
        Properties prop = new Properties();
        prop.put("action", "getloggedinuserinfo");
        prop.put("qformat", "xml");
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        return response;
    }
    
    /**
     *  Method used to log out a metacat server. The Metacat server will end
     *  the session when this call is invoked.
     *
     *  @return the response string from metacat in XML format
     *  @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     */
    public String logout() throws MetacatInaccessibleException, MetacatException {
        Properties prop = new Properties();
        prop.put("action", "logout");
        prop.put("qformat", "xml");
        if (this.sessionId != null) {
        	prop.put("sessionid", sessionId);
        }
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        if (response.indexOf("<logout>") == -1) {
            throw new MetacatException(response);
        }
        setSessionId("");
        return response;
    }
    
    /**
     *  Method used to log in to a metacat server. Implementations will need
     *  to cache a cookie value to make the session persistent.  Each time a
     *  call is made to one of the other methods (e.g., read), the cookie will
     *  need to be passed back to the metacat server along with the request.
     *
     *  @param username   the username of the user, like an LDAP DN
     *  @param password   the password for that user for authentication
     *  @return the response string from metacat in XML format
     *  @throws MetacatAuthException when the username/password could
     *                    not be authenticated
     */
    public String validateSession(String sessionId)
    		throws MetacatAuthException, MetacatInaccessibleException {
    	
        Properties prop = new Properties();
        prop.put("action", "validatesession");
        prop.put("sessionid", sessionId);
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        if (response.indexOf("<validateSession><status>") == -1) {
            setSessionId("");
            throw new MetacatAuthException(response);
        } 
        
        return response;
    }
    
   

	/**
     *  Method used to log in to a metacat server. Implementations will need
     *  to cache a cookie value to make the session persistent.  Each time a
     *  call is made to one of the other methods (e.g., read), the cookie will
     *  need to be passed back to the metacat server along with the request.
     *
     *  @param username   the username of the user, like an LDAP DN
     *  @param password   the password for that user for authentication
     *  @return the response string from metacat in XML format
     *  @throws MetacatAuthException when the username/password could
     *                    not be authenticated
     */
    public String isAuthorized(String resourceLsid, String permission, String sessionId)
    		throws MetacatAuthException, MetacatInaccessibleException {
    	
        Properties prop = new Properties();
        prop.put("action", "isauthorized");
        prop.put("resourceLsid", resourceLsid);
        prop.put("permission", permission);
        prop.put("sessionId", sessionId);
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        if (response.indexOf("<resourceAuthorization>") == -1) {
        	System.out.println("invalid response: " + response);
            throw new MetacatAuthException(response);
        } 
        
        return response;
    }
    
    /**
     * Read an XML document from the metacat server session, accessed by docid,
     * and returned as a Reader.
     *
     * @param docid the identifier of the document to be read
     * @return a Reader for accessing the document
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     */
    public InputStream read(String docid) throws InsufficientKarmaException,
            MetacatInaccessibleException, MetacatException, DocumentNotFoundException {
    	Reader r = null;
        
        Properties prop = new Properties();
        prop.put("action", "read");
        prop.put("qformat", "xml");
        prop.put("docid", docid);
        InputStream response = null;
        try {
            response = sendParameters(prop);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        BufferedInputStream bis = new BufferedInputStream(response);
        r = new InputStreamReader(bis);
        try {
        	bis.mark(512);
            char[] characters = new char[512];
            int len = r.read(characters, 0, 512);
            StringWriter sw = new StringWriter();
            sw.write(characters, 0, len);
            String message = sw.toString();
            sw.close();
            bis.reset();
            if (message.indexOf("<error>") != -1) {
                if (message.indexOf("does not have permission") != -1) {
                    throw new InsufficientKarmaException(message);
                } else if(message.indexOf("does not exist") != -1) {
                    throw new DocumentNotFoundException(message);
                } else {
                    throw new MetacatException(message);
                }
            }
        } catch (IOException ioe) {
            throw new MetacatException(
                    "MetacatClient: Error converting Reader to String."
                    + ioe.getMessage());
        }
        return bis;
    }
    
    
    /**
     * Read inline data from the metacat server session, accessed by
     * inlinedataid and returned as a Reader.
     *
     * @param inlinedataid the identifier of the data to be read
     * @return a Reader for accessing the document
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     */
    public InputStream readInlineData(String inlinedataid)
    throws InsufficientKarmaException,
            MetacatInaccessibleException, MetacatException {
        Reader r = null;
        
        Properties prop = new Properties();
        prop.put("action", "readinlinedata");
        prop.put("inlinedataid", inlinedataid);
        
        InputStream response = null;
        try {
            response = sendParameters(prop);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        BufferedInputStream bis = new BufferedInputStream(response);
        r = new InputStreamReader(bis);
        try {
        	bis.mark(512);
            char[] characters = new char[512];
            int len = r.read(characters, 0, 512);
            StringWriter sw = new StringWriter();
            sw.write(characters, 0, len);
            String message = sw.toString();
            sw.close();
            bis.reset();
            if (message.indexOf("<error>") != -1) {
                if (message.indexOf("does not have permission") != -1) {
                    throw new InsufficientKarmaException(message);
                } else {
                    throw new MetacatException(message);
                }
            }
        } catch (IOException ioe) {
            throw new MetacatException(
                    "MetacatClient: Error converting Reader to String."
                    + ioe.getMessage());
        }
        
        return bis;
    }
    
    /**
     * Query the metacat document store with the given metacat-compatible
     * query document and default qformat xml, and return the result set as a Reader.
     *
     * @param xmlQuery a Reader for accessing the XML version of the query
     * @return a Reader for accessing the result set
     */
    public Reader query(Reader xmlQuery) throws MetacatInaccessibleException,
            IOException {
        String qformat = "xml";
        return query(xmlQuery, qformat);
    }
    
    /**
     * Query the metacat document store with the given metacat-compatible
     * query document and qformat, and return the result set as a Reader.
     *
     * @param xmlQuery a Reader for accessing the XML version of the query
     * @param qformat the format of return doc. It can be xml, knb, lter and etal.
     * @return a Reader for accessing the result set
     */
    public Reader query(Reader xmlQuery, String qformat) throws MetacatInaccessibleException,
            IOException {
        Reader reader = null;
        String query = null;
        try {
            query = IOUtil.getAsString(xmlQuery, true);
        } catch (IOException ioE) {
            throw ioE;
        }
        
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "squery");
        prop.put("qformat", qformat);
        prop.put("query", query);
        
        InputStream response = null;
        try {
            response = sendParameters(prop);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        reader = new InputStreamReader(response);
        return reader;
    }
    
    /**
     * Insert an XML document into the repository.
     *
     * @param docid the docid to insert the document
     * @param xmlDocument a Reader for accessing the XML document to be inserted
     * @param schema a Reader for accessing the DTD or XML Schema for
     *               the document
     * @return the metacat response message
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     * @throws IOException when there is an error reading the xml document
     */
    public String insert(String docid, Reader xmlDocument, Reader schema)
    	throws InsufficientKarmaException, MetacatException, IOException,
            MetacatInaccessibleException {

        String doctext = null;
        String schematext = null;
        try {
            doctext = IOUtil.getAsString(xmlDocument, true);
            if (schema != null) {
                schematext = IOUtil.getAsString(schema, true);
            }
        } catch (IOException ioE) {
            throw ioE;
        }
        
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "insert");
        prop.put("docid", docid);
        prop.put("doctext", doctext);
        if (schematext != null) {
            prop.put("dtdtext", schematext);
        }

//        if (sessionId != null) {
//            prop.put("sessionid", sessionId);
//        }
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        // Check for an error condition
        if (response.indexOf("<error>") != -1) {
            if (response.indexOf("does not have permission") != -1) {
                throw new InsufficientKarmaException(response);
            } else {
                throw new MetacatException(response);
            }
        }
        
        return response;
    }
    
    /**
     * Update an XML document in the repository.
     *
     * @param docid the docid to update
     * @param xmlDocument a Reader for accessing the XML text to be updated
     * @param schema a Reader for accessing the DTD or XML Schema for
     *               the document
     * @return the metacat response message
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     * @throws IOException when there is an error reading the xml document
     */
    public String update(String docid, Reader xmlDocument, Reader schema)
    throws InsufficientKarmaException, MetacatException, IOException,
            MetacatInaccessibleException {
        String doctext = null;
        String schematext = null;
        try {
            doctext = IOUtil.getAsString(xmlDocument, true);
            if (schema != null) {
                schematext = IOUtil.getAsString(schema, true);
            }
        } catch (IOException ioE) {
            throw ioE;
        }
        
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "update");
        prop.put("docid", docid);
        prop.put("doctext", doctext);
        if (schematext != null) {
            prop.put("dtdtext", schematext);
        }
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        // Check for an error condition
        if (response.indexOf("<error>") != -1) {
            if (response.indexOf("does not have permission") != -1) {
                throw new InsufficientKarmaException(response);
            } else {
                throw new MetacatException(response);
            }
        }
        
        return response;
    }
    
    /**
     * Upload a data document into the repository. Data files are stored on 
     * metacat and may be in any format (binary or text), but they are all
     * treated as if they were binary.  Data files are not searched by the
     * query() methods because they are not loaded into the XML store because
     * they are not XML documents.  The File parameter is used to determine a
     * name for the uploaded document.
     *
     * @param docid the identifier to be used for the document
     * @param file the File to be uploaded
     * @param document a InputStream containing the data to be uploaded
     * @return the metacat response message
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     * @throws IOException when there is an error reading the xml document
     */
    public String upload(String docid, File file)
    throws InsufficientKarmaException, MetacatException, IOException,
            MetacatInaccessibleException {
        
    	HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(
        		CoreProtocolPNames.PROTOCOL_VERSION, 
        	    HttpVersion.HTTP_1_1);
    	httpclient.getParams().setParameter(
    			CoreProtocolPNames.HTTP_CONTENT_CHARSET, 
    			encoding);
    	 
    	HttpPost post = new HttpPost(metacatUrl);
    	MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
    	 
    	// For File parameters
    	entity.addPart("datafile", new FileBody(file));
    	
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "upload");
        prop.put("docid", docid);
        
        // For usual String parameters
        Enumeration<Object> keys = prop.keys();
        while (keys.hasMoreElements()) {
        	String key = (String) keys.nextElement();
        	String value = prop.getProperty(key);
        	entity.addPart(key, new StringBody(value, Charset.forName(encoding)));
        }
        
        post.setHeader("Cookie", "JSESSIONID="+ this.sessionId);
        post.setEntity(entity);
    	
        String response = null;
        try {
        	response = EntityUtils.toString(httpclient.execute(post).getEntity(), encoding);
        	httpclient.getConnectionManager().shutdown();
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        // Check for an error condition
        if (response.indexOf("<error>") != -1) {
            if (response.indexOf("does not have permission") != -1) {
                throw new InsufficientKarmaException(response);
            } else {
                throw new MetacatException(response);
            }
        }
        
        return response;
    }
    
    /**
     * Upload a data document into the repository. Data files are stored on 
     * metacat and may be in any format (binary or text), but they are all
     * treated as if they were binary.  Data files are not searched by the
     * query() methods because they are not loaded into the XML store because
     * they are not XML documents. The name for the document is set explicitly
     * using the filename parameter.
     *
     * @param docid the identifier to be used for the document
     * @param filename the name to be used in the MIME description of the uploaded file
     * @param document a InputStream containing the data to be uploaded
     * @return the metacat response message
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     * @throws IOException when there is an error reading the xml document
     */
    public String upload(String docid, String filename, InputStream fileData,
            int size)
            throws InsufficientKarmaException, MetacatException, IOException,
            MetacatInaccessibleException {
        
    	HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(
        		CoreProtocolPNames.PROTOCOL_VERSION, 
        	    HttpVersion.HTTP_1_1);
    	httpclient.getParams().setParameter(
    			CoreProtocolPNames.HTTP_CONTENT_CHARSET, 
    			encoding);
    	 
    	HttpPost post = new HttpPost(metacatUrl);
    	MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
    	
    	// For File parameters
    	AbstractContentBody content = null;
    	if (size < 0) {
        	content = new InputStreamBody(fileData, filename);
        	//content = new ByteArrayBody(IOUtils.toByteArray(fileData),  filename);
    	} else {
    		content = new InputStreamKnownSizeBody(fileData, filename, size);
    	}    	
    	entity.addPart("datafile", content);
    	
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "upload");
        prop.put("docid", docid);
        
        // For usual String parameters
        Enumeration<Object> keys = prop.keys();
        while (keys.hasMoreElements()) {
        	String key = (String) keys.nextElement();
        	String value = prop.getProperty(key);
        	entity.addPart(key, new StringBody(value, Charset.forName(encoding)));
        }
    
        post.setHeader("Cookie", "JSESSIONID="+ this.sessionId);
        post.setEntity(entity);
    	
        String response = null;
        try {
        	response = EntityUtils.toString(httpclient.execute(post).getEntity(), encoding);
        	httpclient.getConnectionManager().shutdown();
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        // Check for an error condition
        if (response.indexOf("<error>") != -1) {
            if (response.indexOf("does not have permission") != -1) {
                throw new InsufficientKarmaException(response);
            } else {
                throw new MetacatException(response);
            }
        }
        
        return response;
    }
    
    /**
     * Delete an XML document in the repository.
     *
     * @param docid the docid to delete
     * @return the metacat response message
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     */
    public String delete(String docid)
    throws InsufficientKarmaException, MetacatException,
            MetacatInaccessibleException {
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "delete");
        prop.put("docid", docid);
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        // Check for an error condition
        if (response.indexOf("<error>") != -1) {
            if (response.indexOf("does not have permission") != -1) {
                throw new InsufficientKarmaException(response);
            } else {
                throw new MetacatException(response);
            }
        }
        return response;
    }
    
    /**
     * get the access control info for a given document id.
     *
     * @param _docid the docid of the document for which the access should be applied.
     *
     * @return the metacat access xml
     */
    public String getAccessControl(String docid) 
    	throws InsufficientKarmaException, MetacatException,MetacatInaccessibleException {
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "getaccesscontrol");
        prop.put("docid", docid);
       
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        // Check for an error condition
        if (response.indexOf("<error>") != -1) {
            if (response.indexOf("does not have permission") != -1) {
                throw new InsufficientKarmaException(response);
            } else {
                throw new MetacatException(response);
            }
        }
        return response;
    }
    
    /**
     * set the access on an XML document in the repository.
     *
     * @param _docid the docid of the document for which the access should be applied.
     *
     * @param _principal the document's principal
     *
     * @param _permission the access permission to be applied to the docid
     *  {e.g. read,write,all}
     *
     * @param _permType the permission type to be applied to the document
     *  {e.g. allow or deny}
     *
     * @param _permOrder the order that the document's permissions should be
     *  processed {e.g. denyFirst or allowFirst}
     *
     *
     * @return the metacat response message
     *
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     */
    public String setAccess(String docid, String principal, String
            permission, String permType, String permOrder )
            throws InsufficientKarmaException, MetacatException,
            MetacatInaccessibleException {
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "setaccess");
        prop.put("docid", docid);
        prop.put("principal", principal);
        prop.put("permission", permission);
        prop.put("permType", permType);
        prop.put("permOrder", permOrder);
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        // Check for an error condition
        if (response.indexOf("<error>") != -1) {
            if (response.indexOf("does not have permission") != -1) {
                throw new InsufficientKarmaException(response);
            } else {
                throw new MetacatException(response);
            }
        }
        return response;
    }
    
    /**
	 * Set access for a given doc id. The access is represented in an access
	 * block of xml. All existing access will be replaced with the access
	 * provided in the access block.
	 * 
	 * @param docid
	 *            the doc id for the doc we want to update
	 * @param accessBlock
	 *            the xml access block. This is the same structure as that
	 *            returned by the getdocumentinfo action in metacat.
	 * @return a string holding the response xml
	 */
    public String setAccess(String docid, String accessBlock)
            throws InsufficientKarmaException, MetacatException, MetacatInaccessibleException {
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "setaccess");
        prop.put("docid", docid);
        prop.put("accessBlock", accessBlock);
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        
        // Check for an error condition
        if (response.indexOf("<error>") != -1) {
            if (response.indexOf("does not have permission") != -1) {
                throw new InsufficientKarmaException(response);
            } else {
                throw new MetacatException(response);
            }
        }
        return response;
    }
    
    /**
     * When the MetacatFactory creates an instance it needs to set the
     * MetacatUrl to which connections should be made.
     *
     * @param metacatUrl the URL for the metacat server
     */
    public void setMetacatUrl(String metacatUrl) {
        this.metacatUrl = metacatUrl;
    }
    
    /**
     * Get the session identifier for this session.  This is only valid if
     * the login methods has been called successfully for this Metacat object
     * beforehand.
     *
     * @returns the sessionId as a String, or null if the session is invalid
     */
    public String getSessionId() {
        return this.sessionId;
    }
    
    /**
     * Set the session identifier for this session.  This identifier was
     * previously established with a call to login.  To continue to use the
     * same session, set the session id before making a call to one of the
     * metacat access methods (e.g., read, query, insert, etc.).
     *
     * @param String the sessionId from a previously established session
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * The method will return the latest revision in metacat server
     * for a given document id. If some error happens, this method will throw
     * an exception.
     * @param docId String  the given docid you want to use. the docid it self
     *                      can have or haven't revision number
     * @throws MetacatException
     */
    public int getNewestDocRevision(String docId) throws MetacatException {
        int rev = 0;
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "getrevisionanddoctype");
        prop.put("docid", docId);
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
            // Check for an error condition
            if (response.indexOf("<error>") != -1) {
                throw new MetacatException(response);
            }
            //parseRevisionResponse will return null if there is an
            //error that it can't handle
            String revStr = parserRevisionResponse(response);
            Integer revObj = new Integer(revStr);
            rev = revObj.intValue();
        } catch (Exception e) {
            throw new MetacatException(e.getMessage());
        }
        return rev;
    }
    
    /**
     * Return the highest document id for a given scope.  This is used by
     * clients to make it easier to determine the next free identifier in a
     * sequence for a given scope.
     * @param scope String  the scope to use for looking up the latest id
     * @throws MetacatException when an error occurs
     */
    public String getLastDocid(String scope) throws MetacatException {
        String lastIdentifier = "";
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "getlastdocid");
        prop.put("scope", scope);
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
            // Check for an error condition
            if (response.indexOf("<error>") != -1) {
                throw new MetacatException(response);
            } else {
                Reader responseReader = new StringReader(response);
                Node root =
                        XMLUtilities.getXMLReaderAsDOMTreeRootNode(responseReader);
                Node docidNode =
                        XMLUtilities.getNodeWithXPath(root, "/lastDocid/docid");
                lastIdentifier = docidNode.getFirstChild().getNodeValue();
            }
        } catch (Exception e) {
            throw new MetacatException(e.getMessage());
        }
        return lastIdentifier;
    }
    
    /**
     * return a list of all docids that match a given scope.  if scope is null
     * return all docids registered in the system
     * @param scope String  the scope to use to limit the docid query
     * @throws MetacatException when an error occurs
     */
    public Vector getAllDocids(String scope) throws MetacatException {
        Vector resultVec = new Vector();
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "getalldocids");
        if(scope != null) {
            prop.put("scope", scope);
        }
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
            // Check for an error condition
            if (response.indexOf("<error>") != -1) {
                throw new MetacatException(response);
            } else {
                Reader responseReader = new StringReader(response);
                Node root =
                        XMLUtilities.getXMLReaderAsDOMTreeRootNode(responseReader);
                NodeList nlist = root.getChildNodes();
                for(int i=0; i<nlist.getLength(); i++) {
                    Node n = nlist.item(i);
                    if(n.getNodeName().equals("docid")) {
                        //add the content to the return vector
                        String nodeVal = n.getFirstChild().getNodeValue();
                        resultVec.addElement(nodeVal);
                    }
                }
                
            }
        } catch (Exception e) {
            throw new MetacatException(e.getMessage());
        }
        return resultVec;
    }
    
    /**
     * return true of the given docid is registered, false if not
     * @param scope String  the scope to use to limit the docid query
     * @throws MetacatException when an error occurs
     */
    public boolean isRegistered(String docid) throws MetacatException {
        Vector resultVec = new Vector();
        //set up properties
        Properties prop = new Properties();
        prop.put("action", "isregistered");
        if(docid == null) {
            throw new MetacatException("<error>Cannot check if a null docid " +
                    "is registered.</error>");
        }
        prop.put("docid", docid);
        
        String response = null;
        try {
        	InputStream result = sendParameters(prop);
            response = IOUtils.toString(result, encoding);
            // Check for an error condition
            if (response.indexOf("<error>") != -1) {
                throw new MetacatException(response);
            } else {
                if (response.indexOf("true") != -1) {
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            throw new MetacatException(e.getMessage());
        }
    }
    
    /**
     * Send a request to Metacat.  An alternative to the sentData method.
     * Allows for sending multiple parameters with the same name, 
     * different names, or any combo.  Send properties where the entry 
     * key contains the "param key", 
     * and the entry value contains the "param value".  
     * Constraint: no multi-valued parameters are supported
     *
     * @return InputStream as returned by Metacat
     * @param args Properties of the parameters to be sent to Metacat, where,
     *      key = param key
     *      value = param value
     * @throws java.lang.Exception thrown
     */
    synchronized public InputStream sendParameters(Properties prop) throws Exception {
        InputStream result = null;
        try {
        	HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(
            		CoreProtocolPNames.PROTOCOL_VERSION, 
            	    HttpVersion.HTTP_1_1);
        	httpclient.getParams().setParameter(
        			CoreProtocolPNames.HTTP_CONTENT_CHARSET, 
        			encoding);
            HttpPost post = new HttpPost(metacatUrl);
            //set the params
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            Enumeration<Object> keys = prop.keys();
            while (keys.hasMoreElements()) {
            	String key = (String) keys.nextElement();
            	String value = prop.getProperty(key);
            	NameValuePair nvp = new BasicNameValuePair(key, value);
            	nameValuePairs.add(nvp);
            }
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs, encoding));
            post.setHeader("Cookie", "JSESSIONID="+ this.sessionId);
            HttpResponse httpResponse = httpclient.execute(post);
            result = httpResponse.getEntity().getContent();
            //httpclient.getConnectionManager().shutdown();
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        return result;
    }
    
    /**
     * Send a request to Metacat.  
     * NOTE: Send properties where the entry 
     * key contains the "param value", 
     * and the entry value contains the "param name".  
     * Constraint: param values must be unique.
     *
     * @return InputStream as returned by Metacat
     * @param args Properties of the parameters to be sent to Metacat, where,
     *      key = param value
     *      value = param name
     * @throws java.lang.Exception thrown
     */
    synchronized public InputStream sendParametersInverted(Properties prop) throws Exception {
        InputStream result = null;
        try {
        	HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(
            		CoreProtocolPNames.PROTOCOL_VERSION, 
            	    HttpVersion.HTTP_1_1);
        	httpclient.getParams().setParameter(
        			CoreProtocolPNames.HTTP_CONTENT_CHARSET, 
        			encoding);
            HttpPost post = new HttpPost(metacatUrl);
            //set the params
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            Enumeration<Object> keys = prop.keys();
            while (keys.hasMoreElements()) {
            	// NOTE: using the value as the key for multi-valued parameters
            	String key = (String) keys.nextElement();
            	String value = prop.getProperty(key);
            	NameValuePair nvp = new BasicNameValuePair(value, key);
            	nameValuePairs.add(nvp);
            }
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs, encoding));
            post.setHeader("Cookie", "JSESSIONID="+ this.sessionId);
            HttpResponse httpResponse = httpclient.execute(post);
            result = httpResponse.getEntity().getContent();
            //httpclient.getConnectionManager().shutdown();
        } catch (Exception e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }
        return result;
    }
    
    /*
     * "getversionanddoctype" action will return a string from metacat server.
     * The string format is "revision;doctype"(This is bad idea, we should use xml)
     * This method will get revision string from the response string
     */
    private String parserRevisionResponse(String response) throws Exception {
        String revision = null;
        if (response != null) {
            if(response.indexOf("<error>") != -1) {
                if(response.indexOf("There is not record") != -1) {
                    return "0";
                } else {
                    return null;
                }
            } else {
                int firstSemiCol = response.indexOf(";");
                revision = response.substring(0, firstSemiCol);
            }
        }
        return revision;
    }
    
    /**
     * JSP API: This is a convenience method to reduce the amount of code in a Metacat Client
     * JSP.  It handles creating/reusing an instance of a MetacatClient.
     * @param request Since this is intended to be used by a JSP, it is passed the
     * available "request" variable (the HttpServletRequest).
     * @throws edu.ucsb.nceas.metacat.client.MetacatInaccessibleException Received by MetacatFactory.
     * @return MetacatClient instance.
     */
    public static MetacatClient getMetacatClient(HttpServletRequest request) throws MetacatInaccessibleException {
        MetacatClient                       result;
        String                              metacatPath = "http://%1$s%2$s/metacat";
        String                              host, context;
        javax.servlet.http.HttpSession      session;
        
        session = request.getSession();
        result = (MetacatClient) session.getAttribute("MetacatClient");
        if (result == null) {
            host = request.getHeader("host");
            context = request.getContextPath();
            metacatPath = metacatPath.replaceFirst("%1$s", host);
            metacatPath = metacatPath.replaceFirst("%2$s", context);
            result = (MetacatClient) MetacatFactory.createMetacatConnection(metacatPath);
            session.setAttribute("MetacatClient", result);
        }
        return(result);
    }

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
    
}

class InputStreamKnownSizeBody extends InputStreamBody {
	private int length;

	public InputStreamKnownSizeBody(
			final InputStream in, 
			final String filename,
			final int length) {
		super(in, filename);
		this.length = length;
	}

	@Override
	public long getContentLength() {
		return this.length;
	}
}
