package edu.ucsb.nceas.metacat.client.rest;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import edu.ucsb.nceas.metacat.client.DocumentNotFoundException;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;

/**
 *  This interface provides methods for initializing and logging in to a
 *  Metacat REST API, and then querying, reading, transforming, inserting,
 *  updating and deleting documents from that server.
 */
public interface MetacatRest
{
	/** HTTP Verb GET*/
	public static final String GET = "GET";
	/** HTTP Verb POST*/	
	public static final String POST = "POST";
	/** HTTP Verb PUT*/
	public static final String PUT = "PUT";
	/** HTTP Verb DELETE*/
	public static final String DELETE = "DELETE";

	/*
	 * API Resources
	 */

	/** API OBJECTS Resource which handles with document operations*/
	public static final String RESOURCE_OBJECTS = "object";
	/** API SESSION Resource which handles with user session operations*/
	public static final String RESOURCE_SESSION = "session";
	/** API IDENTIFIER Resource which controls object unique identifier operations*/
	public static final String RESOURCE_IDENTIFIER = "identifier";
	
	/*
	 * API Functions used as URL parameters
	 */
	/** Function keyword*/
	public static final String FUNCTION_KEYWORD = "op";
	/** Function name for login function*/
	public static final String FUNCTION_NAME_LOGIN = "login";
	/** Function name for logout function*/	
	public static final String FUNCTION_NAME_LOGOUT = "logout";
	/** Function name for isregistered function*/
	public static final String FUNCTION_NAME_ISREGISTERED = "isregistered";
	/** Function name for getalldocids function*/
	public static final String FUNCTION_NAME_GETALLDOCS = "getalldocids";
	/** Function name for getnextrevision function*/
	public static final String FUNCTION_NAME_GETNEXTREV = "getnextrevision";
	/** Function name for getnextobject function*/
	public static final String FUNCTION_NAME_GETNEXTOBJ = "getnextobject";
	/** Function name for insert function*/
	public static final String FUNCTION_NAME_INSERT = "insert";
	/** Function name for update function*/
	public static final String FUNCTION_NAME_UPDATE= "update";
	
    /**
     *  Method used to log in to a metacat server through REST API
     *
     *  @param username   the username of the user, like an LDAP DN
     *  @param password   the password for that user for authentication
     *  @return the response string from metacat in XML format
     *  @throws MetacatAuthException when the username/password could
     *                    not be authenticated
     */

    public String login(String username, String password)
           throws MetacatAuthException, MetacatInaccessibleException;

    /**
     *  Method used to log out a metacat server. The Metacat server will end
     *  the session when this call is invoked.
     *
     *  @return the response string from metacat in XML format
     *  @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     */
    public String logout() throws MetacatInaccessibleException,
        MetacatException;

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
    public Reader getObject(String docid, String outputFile) throws InsufficientKarmaException,
        MetacatInaccessibleException, DocumentNotFoundException, MetacatException;

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
    public Reader authenticatedGetObject(String docid, String outputFile) throws InsufficientKarmaException,
		MetacatInaccessibleException, DocumentNotFoundException, MetacatException;
    
    /**
     * Query the metacat document store with the given metacat-compatible
     * query document, and return the result set as a Reader.
     *
     * @param xmlQuery a Reader for accessing the XML version of the query
     * @return a Reader for accessing the result set
     */
    public Reader query(Reader xmlQuery) throws MetacatInaccessibleException,IOException;
    
    public Reader authenticatedQuery(Reader xmlQuery) throws MetacatInaccessibleException,IOException;
    
    /**
     * Create an XML document into the repository, making it available for 
     * searching using the query() methods.
     *
     * @param docid the docid to insert the document
     * @param xmlDocument a Reader for accessing the XML document to be inserted
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
        throws InsufficientKarmaException, MetacatException, IOException,
        MetacatInaccessibleException;

    /**
     * Update an XML document into the repository, making it available for 
     * searching using the query() methods.
     *
     * @param docid the docid to insert the document
     * @param xmlDocument a Reader for accessing the XML document to be inserted
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
        throws InsufficientKarmaException, MetacatException, IOException,
        MetacatInaccessibleException;
    

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
        throws InsufficientKarmaException, MetacatException,
        MetacatInaccessibleException;

    /**
     * When the MetacatFactory creates an instance it needs to set the
     * MetacatUrl to which connections should be made.
     *
     * @param metacatUrl the URL for the metacat server
     */
    public void setContextRootUrl(String contextRootUrl) ;

    /**
     * Get the session identifier for this session.
     *
     * @returns the sessionId as a String, or null if the session is invalid
     */
    public String getSessionId();

    /**
     * Set the session identifier for this session.  This identifier was
     * previously established with a call to login.  To continue to use the
     * same session, set the session id before making a call to one of the
     * metacat access methods (e.g., read, query, insert, etc.).
     *
     * @param String the sessionId from a previously established session
     */
    public void setSessionId(String sessionId);


    /**
     * Return the highest document id for a given scope.  This is used by
     * clients to make it easier to determine the next free identifier in a
     * sequence for a given scope.
     * @param scope String  the scope to use for looking up the latest id
     * @throws MetacatException when an error occurs
     */
    public String getNextObject(String scope) throws MetacatException;

    /**
     * The method will return the latest revision in metacat server
     * for a given document id. If some error happens, this method will throw
     * an exception.
     * @param identifierId String  the given docid you want to use. the docid it self
     *                      can have or haven't revision number
     * @throws MetacatException
     */
    public int getNextRevision(String identifierId) throws MetacatException;
    
    /**
     * return a list of all docids that match a given scope.  if scope is null
     * return all docids registered in the system
     * @param scope String  the scope to use to limit the docid query
     * @throws MetacatException when an error occurs
     */
    public Vector<String> getAllDocids(String scope) throws MetacatException;
    
    /**
     * return true of the given docid is registered, false if not
     * @param scope String  the scope to use to limit the docid query
     * @throws MetacatException when an error occurs
     */
    public boolean isRegistered(String identifierId) throws MetacatException;
    
    /**
     * Adds identifierId (Metacat Server does not support it!)
     * @param identifierId String  the given docid you want to use.
     * @throws MetacatException when an error occurs
     */
    public String addLSID(String identifierId) throws MetacatException;
}
