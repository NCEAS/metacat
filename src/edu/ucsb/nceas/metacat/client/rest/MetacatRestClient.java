package edu.ucsb.nceas.metacat.client.rest;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsb.nceas.metacat.client.DocumentNotFoundException;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.utilities.IOUtil;
import edu.ucsb.nceas.utilities.XMLUtilities;


public class MetacatRestClient implements MetacatRest {
    
	/** The session identifier for the session */
	private String sessionId;
	
	/** The URL string for the metacat REST API*/
	private String contextRootUrl;

    /**
     * Constructor to create a new instance. 
     */ 
	public MetacatRestClient(String contextRootUrl){
		setContextRootUrl(contextRootUrl);
	}

    /**
     *  Method used to log in to a metacat server through REST API. Implementations will need
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
	    String urlParams = FUNCTION_KEYWORD+"="+FUNCTION_NAME_LOGIN;
		String postData = "username="+username+"&password="+password;		
		String response = null;
		

		try {
			response = sendData(RESOURCE_SESSION, POST, urlParams, postData, "application/x-www-form-urlencoded", null, null);
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
     *  Method used to log out a metacat server. The Metacat server will end
     *  the session when this call is invoked.
     *
     *  @return the response string from metacat in XML format
     *  @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     */
	public String logout()throws MetacatInaccessibleException,MetacatException {
		String urlParams = FUNCTION_KEYWORD+"="+FUNCTION_NAME_LOGOUT ;
		String postData = "sessionid="+sessionId;
		String response;
		try {
			response = sendData(RESOURCE_SESSION, POST, urlParams, postData,"application/x-www-form-urlencoded", null, null);
		} catch (Exception e) {
			throw new MetacatInaccessibleException(e.getMessage());
		}
		return response;
	}


    /**
     * Read a public XML document , accessed by docid, and returned as a Reader.
     *
     * @param docid the identifier of the document to be read
     * @param outputFile name of the file to be written to local (optional)
     * @return a Reader for accessing the document
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     */
	public Reader getObject(String docid, String outFile) throws InsufficientKarmaException,
	MetacatInaccessibleException, DocumentNotFoundException, MetacatException{
		String resource = RESOURCE_OBJECTS+"/"+docid; 

		try {
			String response = sendData(resource, GET, null, null, null, null, outFile);

			if (response != null ) {
				if (response != null && response.indexOf("<error>") != -1) {
					if (response.indexOf("does not have permission") != -1) {
						throw new InsufficientKarmaException(response);
					} else if(response.indexOf("does not exist") != -1) {
						throw new DocumentNotFoundException(response);
					} else {
						throw new MetacatException(response);                	
					} 
				} else {
					return new StringReader(response);
				}
			} else 
				throw new MetacatException(response);

		} catch(IOException ioe){
			throw new MetacatInaccessibleException(ioe.getMessage());
		}	
	}


    /**
     * Read XML document from server session, accessed by docid, and returned as a Reader.
     *
     * @param docid the identifier of the document to be read
     * @param outputFile name of the file to be written to local (optional)
     * @return a Reader for accessing the document
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     */ 
	public Reader authenticatedGetObject(String docid, String outFile) throws InsufficientKarmaException,
	MetacatInaccessibleException, DocumentNotFoundException, MetacatException{
		String resource = RESOURCE_OBJECTS+"/"+docid; 

		try {
			String response = sendData(resource, GET, "sessionid="+sessionId, null, null, null, outFile);

			if (response != null ) {
				if (response != null && response.indexOf("<error>") != -1) {
					if (response.indexOf("does not have permission") != -1) {
						throw new InsufficientKarmaException(response);
					} else if(response.indexOf("does not exist") != -1) {
						throw new DocumentNotFoundException(response);
					} else {
						throw new MetacatException(response);                	
					} 
				} else {
					return new StringReader(response);
				}
			} else 
				throw new MetacatException(response);

		} catch(IOException ioe){
			throw new MetacatInaccessibleException(ioe.getMessage());
		}	
	}


    /**
     * Query the metacat document store with the given Ecogrid-compatible
     * query document and return the Ecogrid result set as a Reader.
     *
     * @param xmlQuery a Reader for accessing the XML version of the query
     * @return a Reader for accessing the result set
     */
	
	public Reader query(Reader xmlQuery) throws MetacatInaccessibleException,IOException {
		String response = null;

		try{	
			response = sendData(RESOURCE_OBJECTS, POST, null, null, "text/xml", xmlQuery, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MetacatInaccessibleException(e.getMessage());
		}
		if (response != null)
			return new StringReader(response);
		return null;
	}

    /**
     * Query (as an authenticated user) the metacat document store with the given Ecogrid-compatible
     * query document and return the Ecogrid result set as a Reader.
     *
     * @param xmlQuery a Reader for accessing the XML version of the query
     * @return a Reader for accessing the result set
     */
	public Reader authenticatedQuery(Reader xmlQuery) throws MetacatInaccessibleException,IOException {
		String response = null;
		try{
			response = sendData(RESOURCE_OBJECTS, POST, "sessionid="+sessionId, null, "text/xml", xmlQuery, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MetacatInaccessibleException(e.getMessage());
		}
		if (response != null)
			return new StringReader(response);		
		return null;
	}

	/**
     * Create an XML document in the repository.
     *
     * @param docid the docid to insert the document
     * @param xmlDocument a Reader for accessing the XML document to be inserted
     * @param isInsert whether the operation is update or insert
     * 
     * 
     * @return the metacat response message
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     * @throws IOException when there is an error reading the xml document
     */
    public String create(String docid, Reader xmlDocument)
    throws InsufficientKarmaException, MetacatException, IOException, MetacatInaccessibleException {
        return putObject(docid, xmlDocument, true);
    }
    
    /**
     * Update an XML document in the repository, replacing an existing document.
     *
     * @param docid the docid to insert the document
     * @param xmlDocument a Reader for accessing the XML document to be inserted
     * @param isInsert whether the operation is update or insert
     * 
     * 
     * @return the metacat response message
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     * @throws IOException when there is an error reading the xml document
     */
    public String update(String docid, Reader xmlDocument)
    throws InsufficientKarmaException, MetacatException, IOException, MetacatInaccessibleException {
        return putObject(docid, xmlDocument, false);
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
	public String deleteObject(String docid)
	throws InsufficientKarmaException, MetacatException,MetacatInaccessibleException {
		String resource = RESOURCE_OBJECTS+"/"+docid;
		String urlParams = "sessionid="+sessionId+"&";
		String response = null;
		try{
			response = sendData(resource, DELETE, urlParams, null, null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
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
	 * Return the highest document id for a given scope.  This is used by
	 * clients to make it easier to determine the next free identifier in a
	 * sequence for a given scope.
	 * @param scope String  the scope to use for looking up the latest id
	 * @throws MetacatException when an error occurs
	 */

	public String getNextObject(String scope) throws MetacatException {
		String lastIdentifier = "";
		String resource = RESOURCE_IDENTIFIER;
		String urlParams = FUNCTION_KEYWORD+"="+FUNCTION_NAME_GETNEXTOBJ;
		if (scope != null)
			urlParams += "&scope="+scope; 
		String response = null;
		try {
			response = sendData(resource, GET, urlParams, null, null, null, null);
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
	 * The method will return the latest revision in metacat server
	 * for a given document id. If some error happens, this method will throw
	 * an exception.
	 * @param identifierId String  the given docid you want to use. the docid it self
	 *                      can have or haven't revision number
	 * @throws MetacatException
	 */
	public int getNextRevision(String identifierId) throws MetacatException {
		int rev = 0;

		String resource = RESOURCE_IDENTIFIER+"/"+identifierId;
		String urlParams = FUNCTION_KEYWORD+"="+FUNCTION_NAME_GETNEXTREV;

		String response = null;
		try {
			response = sendData(resource, GET, urlParams, null, null, null, null);

	         // Check for an error condition            
            if (response.indexOf("<error>") != -1 ) {
                throw new MetacatException(response);
            }
            
			Reader responseReader = new StringReader(response);
			Node root =
				XMLUtilities.getXMLReaderAsDOMTreeRootNode(responseReader);
			Node docidNode =
				XMLUtilities.getNodeWithXPath(root, "/next-revision");
			rev = Integer.parseInt(docidNode.getFirstChild().getNodeValue());                        

		} catch (Exception e) {
			e.printStackTrace();
			throw new MetacatException(e.getMessage());
		}
		return rev;
	}

	/**
	 * return a list of all docids that match a given scope.  if scope is null
	 * return all docids registered in the system
	 * @param scope String  the scope to use to limit the docid query
	 * @throws MetacatException when an error occurs
	 */
	public Vector<String> getAllDocids(String scope) throws MetacatException {
		Vector<String> resultVec = new Vector<String>();
		String resource = RESOURCE_IDENTIFIER;
		String urlParams = FUNCTION_KEYWORD+"="+FUNCTION_NAME_GETALLDOCS;
		if (scope != null)
			urlParams += "&scope="+scope; 
		String response = null;
		try {
			response = sendData(resource, GET, urlParams, null, null, null, null);
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
	public boolean isRegistered(String identifierId) throws MetacatException {
		String resource = RESOURCE_IDENTIFIER+"/"+identifierId;
		String urlParams = FUNCTION_KEYWORD+"="+FUNCTION_NAME_ISREGISTERED;

		String response = null;
		try {
			response = sendData(resource, GET, urlParams, null, null, null, null);
			// Check for an error condition
			if (response.indexOf("<error>") != -1) {
				throw new MetacatException(response);
			} else {
				Reader responseReader = new StringReader(response);
				StringBuffer sb = new StringBuffer();
				char[] c = new char[1024];
				int numread = responseReader.read(c, 0, 1024);
				while(numread != -1) {
					sb.append(new String(c, 0, numread));
					numread = responseReader.read(c, 0, 1024);
				}

				String responseStr = sb.toString();
				if(responseStr.indexOf("true") != -1) {
					return true;
				}
				return false;
			}
		} catch (Exception e) {
			throw new MetacatException(e.getMessage());
		}
	}

	/**
	 * Adds identifierId
	 * @param identifierId String  the given docid you want to use.
	 * @throws MetacatException when an error occurs
	 */
	public String addLSID(String identifierId) throws MetacatException {
		String resource = RESOURCE_IDENTIFIER+"/"+identifierId;
		String response = null;
		try {
			response = sendData(resource, PUT, null, null, null, null, null);
		} catch (Exception e) {
			throw new MetacatException(e.getMessage());
		}	
		return response;
	}

	public void setContextRootUrl(String contextRootUrl) {
		if (!contextRootUrl.endsWith("/"))
			contextRootUrl += "/";
		this.contextRootUrl = contextRootUrl;

	}


	public String getSessionId() {
		return sessionId;
	}


	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
     * Put (Insert/Update) an XML document into the repository.
     *
     * @param docid the docid to insert the document
     * @param xmlDocument a Reader for accessing the XML document to be inserted
     * @param isInsert whether the operation is update or insert
     * 
     * @return the metacat response message
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     * @throws IOException when there is an error reading the xml document
     */
    private String putObject(String docid, Reader xmlDocument, boolean isInsert)
    throws InsufficientKarmaException, MetacatException, IOException, MetacatInaccessibleException {
    	String resource = RESOURCE_OBJECTS+"/"+docid;
    	String urlParams = "sessionid="+sessionId+"&";
    
    	if (isInsert)
    		urlParams += FUNCTION_KEYWORD+"="+FUNCTION_NAME_INSERT;
    	else
    		urlParams += FUNCTION_KEYWORD+"="+FUNCTION_NAME_UPDATE;
    
    	String response = null;
    	try{
    		response = sendData(resource, PUT, urlParams, null, "text/xml", xmlDocument, null);
    	} catch (Exception e) {
    		e.printStackTrace();
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
	 * Send request to metacat REST API. 
	 * @param resource Resource name to be accessed
	 * @param method HTTP verb, shoudl be one of GET,POST,DELETE,PUT
	 * @param urlParameters QueryString to be added to the end of server url
	 * @param postData QueryString to be printed to output stream
	 * @param contentType Content Type of the data to be posted. eg: "text/xml" or "application/x-www-form-urlencoded"
	 * @param postFileReader Reader of file to be posted
	 * @param outputFile File name to be saved returning from API
	 */
	private String sendData(String resource, String method, String urlParamaters,
	        String postData, String contentType,
			Reader postFileReader, String outputFile) throws IOException {
		HttpURLConnection connection = null ;

		String restURL = contextRootUrl+resource;

		if (urlParamaters != null) {
			if (restURL.indexOf("?") == -1)				
				restURL += "?";
			restURL += urlParamaters; 
		}

		URL u = new URL(restURL);
		URLConnection uc = u.openConnection();
		connection= (HttpURLConnection) uc;

		if (contentType!=null)
			connection.setRequestProperty("Content-Type",contentType);

		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod(method);

		if (!method.equals(GET)) {
			if (postFileReader != null) {
				postData = IOUtil.getAsString(postFileReader, true);
			}
			if (postData != null){
				OutputStream out = connection.getOutputStream();
				Writer wout = new OutputStreamWriter(out);		
				wout.write(postData); 
				wout.flush();
				wout.close();
			}			
		}
		return readStream(connection.getInputStream(),outputFile);
	}
	
	/**
	 * 
	 * Reads Input stream and return readed data, optionaly write the data to specified file 
	 * */
	private String readStream(InputStream is, String writeFile) throws IOException{
		BufferedReader in = new BufferedReader(
				new InputStreamReader(
						is));

		String inputLine;
		StringBuffer b = new StringBuffer();
		FileWriter writer = null;
		if (writeFile!=null)
			writer =  new FileWriter(writeFile);
		while ((inputLine = in.readLine()) != null) { 
			//System.out.println(inputLine+"\n");
			b.append(inputLine+"\n");
			if (writeFile!=null)
				writer.write(inputLine);
		}
		if (writeFile!=null) {
			writer.flush();
			writer.close();			
		}
		in.close();
		return b.toString();
	}

}
