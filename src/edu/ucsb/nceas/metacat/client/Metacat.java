package edu.ucsb.nceas.metacat.client;

import java.io.Reader;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.util.Vector;

/**
 *  This interface provides methods for initializing and logging in to a
 *  Metacat server, and then querying, reading, transforming, inserting,
 *  updating and deleting documents from that server.
 */
public interface Metacat
{
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
        MetacatInaccessibleException, DocumentNotFoundException, MetacatException;

    /**
     * Read inline data from the metacat server session, accessed by
     * inlinedataid, and returned as a Reader.
     *
     * @param inlinedataid the identifier of the document to be read
     * @return a Reader for accessing the document
     * @throws InsufficientKarmaException when the user has insufficent rights
     *                                    for the operation
     * @throws MetacatInaccessibleException when the metacat server can not be
     *                                    reached or does not respond
     * @throws MetacatException when the metacat server generates another error
     */
    public InputStream readInlineData(String inlinedataid)
        throws InsufficientKarmaException,
        MetacatInaccessibleException, MetacatException;

    /**
     * Query the metacat document store with the given metacat-compatible
     * query document, and return the result set as a Reader.
     *
     * @param xmlQuery a Reader for accessing the XML version of the query
     * @return a Reader for accessing the result set
     */
    public Reader query(Reader xmlQuery) throws MetacatInaccessibleException,
                                                IOException;
    
    
    /**
     * Query the metacat document store with the given metacat-compatible
     * query document and qformat, and return the result set as a Reader.
     *
     * @param xmlQuery a Reader for accessing the XML version of the query
     * @param qformat the format of return doc. It can be xml, knb, lter and etal.
     * @return a Reader for accessing the result set
     */
    public Reader query(Reader xmlQuery, String qformat) throws MetacatInaccessibleException,
            IOException;

    /**
     * Insert an XML document into the repository, making it available for 
     * searching using the query() methods.
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
        MetacatInaccessibleException;

    /**
     * Update an XML document in the repository by providing a new version of
     * the XML document.  The new version is placed in the search index, 
     * and older versions are archived (accessible by read(), but not in the 
     * search index).
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
        MetacatInaccessibleException;

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
        MetacatInaccessibleException;

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
    public String upload(String docid, String fileName,
                         InputStream fileData, int size)
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
    public String delete(String docid)
        throws InsufficientKarmaException, MetacatException,
        MetacatInaccessibleException;

    public String getAccessControl(String docid) 
		throws InsufficientKarmaException, MetacatException,MetacatInaccessibleException;
    
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
        throws InsufficientKarmaException, MetacatException, MetacatInaccessibleException;

    public String setAccess(String docid, String accessBlock)
    	throws InsufficientKarmaException, MetacatException, MetacatInaccessibleException;

    /**
     * When the MetacatFactory creates an instance it needs to set the
     * MetacatUrl to which connections should be made.
     *
     * @param metacatUrl the URL for the metacat server
     */
    public void setMetacatUrl(String metacatUrl);

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
     * Get the logged in user for this session.
     *
     * @returns the response received from the server for action=getloggeduserinfo 
     */
    public String getloggedinuserinfo() throws MetacatInaccessibleException;

    /**
     * The method will return the latest revision in metacat server 
     * for a given document id. If some error happens, this method will throw
     * an exception.   
     * @param docId String  the given docid you want to use. the docid it self
     *                      can have or haven't revision number
     * @throws MetacatException
     */
    public int getNewestDocRevision(String docId) throws MetacatException;

    /**
     * Return the highest document id for a given scope.  This is used by
     * clients to make it easier to determine the next free identifier in a
     * sequence for a given scope.  
     * @param scope String  the scope to use for looking up the latest id
     * @throws MetacatException when an error occurs
     */
    public String getLastDocid(String scope) throws MetacatException;
    
    /**
     * return a list of all docids that match a given scope.  if scope is null
     * return all docids registered in the system
     * @param scope String  the scope to use to limit the docid query
     * @throws MetacatException when an error occurs
     */
    public Vector getAllDocids(String scope) throws MetacatException;
    
    /**
     * return true of the given docid is registered, false if not
     * @param scope String  the scope to use to limit the docid query
     * @throws MetacatException when an error occurs
     */
    public boolean isRegistered(String docid) throws MetacatException;
    
    /**
     * Returns the character encoding used used when communicating with Metacat.
     * @return character encoding name
     */
    public String getEncoding();
    
    /**
     * Returns the character encoding used used when communicating with Metacat.
     * @param encoding The encoding (i.e. "UTF-8")
     */
    public void setEncoding(String encoding);
    
}
