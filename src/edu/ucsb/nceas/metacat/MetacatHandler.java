/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

package edu.ucsb.nceas.metacat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.ecoinformatics.eml.EMLParser;

import au.com.bytecode.opencsv.CSVWriter;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlForSingleFile;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlList;
import edu.ucsb.nceas.metacat.cart.CartManager;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.common.query.EnabledQueryEngines;
import edu.ucsb.nceas.metacat.common.resourcemap.ResourceMapNamespaces;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.dataone.SyncAccessPolicy;
import edu.ucsb.nceas.metacat.dataone.SystemMetadataFactory;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.dataquery.DataQuery;
import edu.ucsb.nceas.metacat.event.MetacatDocumentEvent;
import edu.ucsb.nceas.metacat.event.MetacatEventService;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.replication.ForceReplicationHandler;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.spatial.SpatialHarvester;
import edu.ucsb.nceas.metacat.spatial.SpatialQuery;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SessionData;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.LSIDUtil;
import edu.ucsb.nceas.utilities.ParseLSIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * General entry point for the Metacat server which is called from various 
 * mechanisms such as the standard MetacatServlet class and the various web
 * service servlets such as RestServlet class.  All application logic should be
 * encapsulated in this class, and the calling classes should only contain
 * parameter marshaling and demarshaling code, delegating all else to this
 * MetacatHandler instance.
 * @author Matthew Jones
 */
public class MetacatHandler {

    private static boolean _sitemapScheduled = false;
    
    private static Log logMetacat = LogFactory.getLog(MetacatHandler.class);

    // Constants -- these should be final in a servlet    
    private static final String PROLOG = "<?xml version=\"1.0\"?>";
    private static final String SUCCESS = "<success>";
    private static final String SUCCESSCLOSE = "</success>";
    private static final String ERROR = "<error>";
    private static final String ERRORCLOSE = "</error>";
    public static final String FGDCDOCTYPE = "metadata";
    private static final String NOT_SUPPORT_MESSAGE = PROLOG +  "\n" + ERROR + 
                                "The original Metacat API has been replaced, " + 
                                "and so this request is no longer supported. " + 
                                "Equivalent API methods now are available through " +
                                "the DataONE API (see <https://knb.ecoinformatics.org/api>)." 
                                + ERRORCLOSE;
    
	private static Timer timer;
	
	/**
	 * Constructor with a timer object. This constructor will be used in the MetacatServlet class which
	 * is the only place to handle the site map generation.
	 * @param timer  the timer used to schedule the site map generation
	 */
    public MetacatHandler(Timer timer) {
    	    this.timer = timer;
    }
    
    /**
     * Default constructor. It will be used in the DataONE API, which doesn't need to handle the timer.
     */
    public MetacatHandler() {
        
    }
    
    /**
     * Send back the not-support message
     * @param response
     * @throws IOException
     */
    private void sendNotSupportMessage(HttpServletResponse response) throws IOException {
        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.println(NOT_SUPPORT_MESSAGE);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    protected void handleDataquery(
            Hashtable<String, String[]> params,
            HttpServletResponse response,
            String sessionId) throws PropertyNotFoundException, IOException {
        sendNotSupportMessage(response);
    }
    
    protected void handleEditCart(
            Hashtable<String, String[]> params,
            HttpServletResponse response,
            String sessionId) throws PropertyNotFoundException, IOException {
        sendNotSupportMessage(response);
    }
    // ///////////////////////////// METACAT SPATIAL ///////////////////////////
    
    /**
     * handles all spatial queries -- these queries may include any of the
     * queries supported by the WFS / WMS standards
     * 
     * handleSQuery(out, params, response, username, groupnames, sess_id);
     * @throws HandlerException 
     */
    protected void handleSpatialQuery(Hashtable<String, String[]> params,
            HttpServletResponse response,
            String username, String[] groupnames,
            String sess_id) throws PropertyNotFoundException, IOException {
        sendNotSupportMessage(response);
    }
    
    // LOGIN & LOGOUT SECTION
    /**
     * Handle the login request. Create a new session object. Do user
     * authentication through the session.
     * @throws IOException 
     */
    public void handleLoginAction(Writer out, Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuthSession sess = null;
        
        if(params.get("username") == null){
            response.setContentType("text/xml");
            out.write("<?xml version=\"1.0\"?>");
            out.write("<error>");
            out.write("Username not specified");
            out.write("</error>");
            return;
        }
        
        // }
        
        if(params.get("password") == null){
            response.setContentType("text/xml");
            out.write("<?xml version=\"1.0\"?>");
            out.write("<error>");
            out.write("Password not specified");
            out.write("</error>");
            return;
        }
        
        String un = (params.get("username"))[0];
        logMetacat.info("MetacatHandler.handleLoginAction - user " + un + " is trying to login");
        String pw = (params.get("password"))[0];
        
        String qformat = "xml";
        if (params.get("qformat") != null) {
            qformat = (params.get("qformat"))[0];
        }
        
        try {
            sess = new AuthSession();
        } catch (Exception e) {
            String errorMsg = "MetacatServlet.handleLoginAction - Problem in MetacatServlet.handleLoginAction() authenticating session: "
                + e.getMessage();
            logMetacat.error(errorMsg);
            out.write(errorMsg);
            e.printStackTrace(System.out);
            return;
        }
        boolean isValid = sess.authenticate(request, un, pw);
        
        //if authenticate is true, store the session
        if (isValid) {
            HttpSession session = sess.getSessions();
            String id = session.getId();
            
            logMetacat.debug("MetacatHandler.handleLoginAction - Store session id " + id
                    + " which has username" + session.getAttribute("username")
                    + " into hash in login method");
            try {
                //System.out.println("registering session with id " + id);
                //System.out.println("username: " + (String) session.getAttribute("username"));
                SessionService.getInstance().registerSession(id, 
                        (String) session.getAttribute("username"), 
                        (String[]) session.getAttribute("groupnames"), 
                        (String) session.getAttribute("password"), 
                        (String) session.getAttribute("name"));
                
                    
            } catch (ServiceException se) {
                String errorMsg = "MetacatServlet.handleLoginAction - service problem registering session: "
                        + se.getMessage();
                logMetacat.error("MetacatHandler.handleLoginAction - " + errorMsg);
                out.write(errorMsg);
                se.printStackTrace(System.out);
                return;
            }           
        }
                
        // format and transform the output
        if (qformat.equals("xml")) {
            response.setContentType("text/xml");
            out.write(sess.getMessage());
        } else {
            try {
                DBTransform trans = new DBTransform();
                response.setContentType("text/html");
                trans.transformXMLDocument(sess.getMessage(),
                        "-//NCEAS//login//EN", "-//W3C//HTML//EN", qformat,
                        out, null, null);
            } catch (Exception e) {               
                logMetacat.error("MetacatHandler.handleLoginAction - General error"
                        + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }
    
    /**
     * Handle the logout request. Close the connection.
     * @throws IOException 
     */
    public void handleLogoutAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        sendNotSupportMessage(response);
    }
    
    // END OF LOGIN & LOGOUT SECTION
    
    // SQUERY & QUERY SECTION
    /**
     * Retrieve the squery xml, execute it and display it
     *
     * @param out the output stream to the client
     * @param params the Hashtable of parameters that should be included in the
     *            squery.
     * @param response the response object linked to the client
     * @param user the user name (it maybe different to the one in param)
     * @param groups the group array
     * @param sessionid  the sessionid
     */
    protected void handleSQuery(Hashtable<String, String[]> params,
            HttpServletResponse response, String user, String[] groups,
            String sessionid) throws PropertyNotFoundException, IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Create the xml query, execute it and display the results.
     *
     * @param out the output stream to the client
     * @param params the Hashtable of parameters that should be included in the
     *            squery.
     * @param response the response object linked to the client
     * @throws IOException 
     * @throws UnsupportedEncodingException 
     */
    protected void handleQuery(Hashtable<String, String[]> params,
            HttpServletResponse response, String user, String[] groups,
            String sessionid) throws PropertyNotFoundException, UnsupportedEncodingException, IOException {
        sendNotSupportMessage(response);
    }
    
    // END OF SQUERY & QUERY SECTION
    
    //Export section
    /**
     * Handle the "export" request of data package from Metacat in zip format
     *
     * @param params the Hashtable of HTTP request parameters
     * @param response the HTTP response object linked to the client
     * @param user the username sent the request
     * @param groups the user's groupnames
     */
    protected void handleExportAction(Hashtable<String, String[]> params,
            HttpServletResponse response,
            String user, String[] groups, String passWord) throws IOException{
        sendNotSupportMessage(response);
    }
    
    /**
     * In eml2 document, the xml can have inline data and data was stripped off
     * and store in file system. This action can be used to read inline data
     * only
     *
     * @param params the Hashtable of HTTP request parameters
     * @param response the HTTP response object linked to the client
     * @param user the username sent the request
     * @param groups the user's groupnames
     */
    protected void handleReadInlineDataAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response,
            String user, String passWord, String[] groups) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Handle the "read" request of metadata/data files from Metacat or any
     * files from Internet; transformed metadata XML document into HTML
     * presentation if requested; zip files when more than one were requested.
     *
     * @param params the Hashtable of HTTP request parameters
     * @param request the HTTP request object linked to the client
     * @param response the HTTP response object linked to the client
     * @param user the username sent the request
     * @param groups the user's groupnames
     */
    public void handleReadAction(Hashtable<String, String[]> params, HttpServletRequest request,
            HttpServletResponse response, String user, String passWord,
            String[] groups) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Read a document from metacat and return the InputStream. The dataType will be null.
     * @param docid - the metacat docid to read
     * @return the document as an input stream
     * @throws PropertyNotFoundException
     * @throws ClassNotFoundException
     * @throws ParseLSIDException
     * @throws McdbException
     * @throws SQLException
     * @throws IOException
     */
    public static InputStream read(String docid) throws PropertyNotFoundException, ClassNotFoundException, 
                               ParseLSIDException, McdbException, SQLException, IOException {
        String dataType = null;
        return read(docid, dataType);
    }
    
    /**
     * Read a document from metacat and return the InputStream.  The XML or
     * data document should be on disk, but if not, read from the metacat database.
     * 
     * @param  docid - the metacat docid to read
     * @param dataType - the type of the object associated with docid
     * @return objectStream - the document as an InputStream
     * @throws InsufficientKarmaException
     * @throws ParseLSIDException
     * @throws PropertyNotFoundException
     * @throws McdbException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
	public static InputStream read(String docid, String dataType) throws ParseLSIDException,
			PropertyNotFoundException, McdbException, SQLException,
			ClassNotFoundException, IOException {
		logMetacat.debug("MetacatHandler.read() called and the data type is " + dataType);

		InputStream inputStream = null;

		// be sure we have a local ID from an LSID
		if (docid.startsWith("urn:")) {
			try {
				docid = LSIDUtil.getDocId(docid, true);
			} catch (ParseLSIDException ple) {
				logMetacat.debug("There was a problem parsing the LSID. The "
						+ "error message was: " + ple.getMessage());
				throw ple;
			}
		}
		
        if (dataType != null && dataType.equalsIgnoreCase(D1NodeService.METADATA)) {
            logMetacat.debug("MetacatHandler.read - the data type is specified as the meta data");
            String filepath = PropertyService.getProperty("application.documentfilepath");
            // ensure it is a directory path
            if (!(filepath.endsWith("/"))) {
                filepath += "/";
            }
            String filename = filepath + docid;
            inputStream = readFromFilesystem(filename);
		} else {
		 // accomodate old clients that send docids without revision numbers
	        docid = DocumentUtil.appendRev(docid);
	        DocumentImpl doc = new DocumentImpl(docid, false);
		    // deal with data or metadata cases
	        if (doc.getRootNodeID() == 0) {
	            // this is a data file
	            // get the path to the file to read
	            try {
	                String filepath = PropertyService.getProperty("application.datafilepath");
	                // ensure it is a directory path
	                if (!(filepath.endsWith("/"))) {
	                    filepath += "/";
	                }
	                String filename = filepath + docid;
	                inputStream = readFromFilesystem(filename);
	            } catch (PropertyNotFoundException pnf) {
	                logMetacat.debug("There was a problem finding the "
	                        + "application.datafilepath property. The error "
	                        + "message was: " + pnf.getMessage());
	                throw pnf;
	            } // end try()
	        } else {
	            // this is an metadata document
	            // Get the xml (will try disk then DB)
	            try {
	                // force the InputStream to be returned
	                OutputStream nout = null;
	                inputStream = doc.toXml(nout, null, null, true);
	            } catch (McdbException e) {
	                // report the error
	                logMetacat.error(
	                        "MetacatHandler.readFromMetacat() "
	                                + "- could not read document " + docid + ": "
	                                + e.getMessage(), e);
	            }
	        }
		}
		return inputStream;
	}
    
    /**
     * Read a file from Metacat's configured file system data directory.
     *
     * @param filename  The full path file name of the file to read
     * 
     * @return fileInputStream  The file to read as a FileInputStream
     */
    private static FileInputStream readFromFilesystem(String filename) 
      throws FileNotFoundException {
        
        logMetacat.debug("MetacatHandler.readFromFilesystem() called.");
        
        FileInputStream fileInputStream = null;
        
        try {
          fileInputStream = new FileInputStream(filename);

        } catch ( FileNotFoundException fnfe ) {
          logMetacat.debug("There was an error reading the file " +
                           filename + ". The error was: "         +
                           fnfe.getMessage());
          throw fnfe;
           
        }
        
      return fileInputStream;  
    }
    
    
    /**
     * Handle the database putdocument request and write an XML document to the
     * database connection
     * @param userAgent 
     * @param generateSystemMetadata 
     */
    public String handleInsertOrUpdateAction(String ipAddress, String userAgent,
            HttpServletResponse response, PrintWriter out, Hashtable<String, String[]> params,
            String user, String[] groups, boolean generateSystemMetadata, boolean writeAccessRules, byte[] xmlBytes, String formatId, Checksum checksum, File objectFile) {
        DBConnection dbConn = null;
        int serialNumber = -1;
        String output = "";
        String qformat = null;
        if(params.containsKey("qformat"))
        {
          qformat = params.get("qformat")[0];
        }
        
        if(params.get("docid") == null){
            String msg = this.PROLOG +
                         this.ERROR  +
                         "Docid not specified" +
                         this.ERRORCLOSE;
            if(out != null)
            {
                out.println(msg);
                logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - Docid not specified");
            }
            return msg; 
        }
        
        try {
            if (!AuthUtil.canInsertOrUpdate(user, groups)) {
                String msg = this.PROLOG +
                             this.ERROR +
                             "User '" + 
                             user + 
                             "' is not allowed to insert or update. Check the Allowed and Denied Submitters lists" +
                             this.ERRORCLOSE;
                if(out != null)
                {
                  out.println(msg);
                }
                
                logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - " + 
                		         "User '" + 
                		         user + 
                		         "' not allowed to insert and update");
                return msg;
            }
        } catch (MetacatUtilException ue) {
            logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - " + 
            		         "Could not determine if user could insert or update: " + 
            		         ue.getMessage(), ue);
            String msg = this.PROLOG +
                    this.ERROR +
                    "MetacatHandler.handleInsertOrUpdateAction - " + 
                             "Could not determine if user could insert or update: " + 
                             ue.getMessage() +
                    this.ERRORCLOSE;
            if(out != null)
            {
              out.println(msg);
            }
            return msg;
        }
        
        try {
          // Get the document indicated
          logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - params: " + 
        		           params.toString());
          
          String[] doctext = params.get("doctext");
          String pub = null;
          if (params.containsKey("public")) {
              pub = params.get("public")[0];
          }
          
          StringReader dtd = null;
          if (params.containsKey("dtdtext")) {
              String[] dtdtext = params.get("dtdtext");
              try {
                  if (!dtdtext[0].equals("")) {
                      dtd = new StringReader(dtdtext[0]);
                  }
              } catch (NullPointerException npe) {
              }
          }
          
          if(doctext == null){
              String msg = this.PROLOG +
                           this.ERROR +
                           "Document text not submitted." +
                           this.ERRORCLOSE;
              if(out != null)
              {
                out.println(msg);
              }
              
              // TODO: this should really throw an exception
              return msg;
          }
          
          logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - " + 
        		           "the xml document in metacat servlet (before parsing):\n" + 
        		           doctext[0]);
          StringReader xmlReader = new StringReader(doctext[0]);
          boolean validate = false;
          DocumentImplWrapper documentWrapper = null;
          String namespace = null;
          String schemaLocation = null;
          try {
            // look inside XML Document for <!DOCTYPE ... PUBLIC/SYSTEM ...
            // >
            // in order to decide whether to use validation parser
            validate = needDTDValidation(xmlReader);
            if (validate) {
                // set a dtd base validation parser
                logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - the xml object will be validate by a dtd");
                String rule = DocumentImpl.DTD;
                documentWrapper = new DocumentImplWrapper(rule, validate, writeAccessRules);
            } else {
                XMLSchemaService.getInstance().doRefresh();
                namespace = XMLSchemaService.findDocumentNamespace(xmlReader);
                if (namespace != null) {
                    logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - the xml object will be validated by a schema which has a target namespace: "+namespace);
                    schemaLocation = XMLSchemaService.getInstance().findNamespaceAndSchemaLocalLocation(formatId, namespace);
                    if (namespace.compareTo(DocumentImpl.EML2_0_0NAMESPACE) == 0
                            || namespace.compareTo(
                            DocumentImpl.EML2_0_1NAMESPACE) == 0) {
                        // set eml2 base     validation parser
                        String rule = DocumentImpl.EML200;
                        // using emlparser to check id validation
                        @SuppressWarnings("unused")
                        EMLParser parser = new EMLParser(doctext[0]);
                        documentWrapper = new DocumentImplWrapper(rule, true, writeAccessRules);
                    } else if (
                    		namespace.compareTo(DocumentImpl.EML2_1_0NAMESPACE) == 0
                    		|| namespace.compareTo(DocumentImpl.EML2_1_1NAMESPACE) == 0 || namespace.compareTo(DocumentImpl.EML2_2_0NAMESPACE) == 0) {
                        // set eml2 base validation parser
                        String rule = DocumentImpl.EML210;
                        // using emlparser to check id validation
                        @SuppressWarnings("unused")
                        EMLParser parser = new EMLParser(doctext[0]);
                        documentWrapper = new DocumentImplWrapper(rule, true, writeAccessRules);
                    } else {
                        if(!XMLSchemaService.isNamespaceRegistered(namespace)) {
                            throw new Exception("The namespace "+namespace+" used in the xml object hasn't been registered in the Metacat. Metacat can't validate the object and rejected it. Please contact the operator of the Metacat for regsitering the namespace.");
                        }
                        // set schema base validation parser
                        String rule = DocumentImpl.SCHEMA;
                        documentWrapper = new DocumentImplWrapper(rule, true, writeAccessRules);
                    }
                } else {
                    xmlReader = new StringReader(doctext[0]);
                    String noNamespaceSchemaLocationAttr = XMLSchemaService.findNoNamespaceSchemaLocationAttr(xmlReader);
                    if(noNamespaceSchemaLocationAttr != null) {
                        logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - the xml object will be validated by a schema which deoe NOT have a target namespace.");
                        schemaLocation = XMLSchemaService.getInstance().findNoNamespaceSchemaLocalLocation(formatId, noNamespaceSchemaLocationAttr);
                        String rule = DocumentImpl.NONAMESPACESCHEMA;
                        documentWrapper = new DocumentImplWrapper(rule, true, writeAccessRules);
                    } else {
                        logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - the xml object will NOT be validated.");
                        documentWrapper = new DocumentImplWrapper("", false, writeAccessRules);
                    }
                    
                }
            }
            
            String[] action = params.get("action");
            String[] docid = params.get("docid");
            String newdocid = null;
            
            String doAction = null;
            if (action[0].equals("insert") || action[0].equals("insertmultipart")) {
                doAction = "INSERT";
                
            } else if (action[0].equals("update")) {
                doAction = "UPDATE";
                
            }
            
            try {
              // get a connection from the pool
              dbConn = DBConnectionPool
                      .getDBConnection("Metacathandler.handleInsertOrUpdateAction");
              serialNumber = dbConn.getCheckOutSerialNumber();
              
              // write the document to the database and disk
              String accNumber = docid[0];
              logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - " + 
                doAction + " " + accNumber + "...");
              Identifier identifier = new Identifier();
              identifier.setValue(accNumber);
              if(!D1NodeService.isValidIdentifier(identifier)) {
                  String error = "The docid "+accNumber +" is not valid since it is null or contians the white space(s).";
                  logMetacat.warn("MetacatHandler.handleInsertOrUpdateAction - " +error);
                  throw new Exception(error);
              }
              
              /*if (accNumber == null || accNumber.equals("")) {
                  logMetacat.warn("MetacatHandler.handleInsertOrUpdateAction - " +
                		          "writing with null acnumber");
                  newdocid = documentWrapper.write(dbConn, doctext[0], pub, dtd,
                          doAction, null, user, groups);
                  EventLog.getInstance().log(ipAddress, userAgent, user, "", action[0]);
              
              } else {*/
              newdocid = documentWrapper.write(dbConn, doctext[0], pub, dtd,
                          doAction, accNumber, user, groups, xmlBytes, schemaLocation, checksum, objectFile);
            
              EventLog.getInstance().log(ipAddress, userAgent, user, accNumber, action[0]);
              
              //}
              
              // alert listeners of this event
              MetacatDocumentEvent mde = new MetacatDocumentEvent();
              mde.setDocid(accNumber);
              mde.setDoctype(namespace);
              mde.setAction(doAction);
              mde.setUser(user);
              mde.setGroups(groups);
              MetacatEventService.getInstance().notifyMetacatEventObservers(mde);
              
              // if it was called from Metacat API, we want to generate system metadata for it
              if ( generateSystemMetadata ) {
                
                SystemMetadata sysMeta = null;
				// it's possible that system metadata exists although
                // older clients don't support it. Try updates first.
                try {
                	// get the docid parts
                	String docidWithoutRev = DocumentUtil.getSmartDocId(newdocid);
                    int rev = IdentifierManager.getInstance().getLatestRevForLocalId(newdocid);
                	String guid = IdentifierManager.getInstance().getGUID(docidWithoutRev, rev);
                	sysMeta  = IdentifierManager.getInstance().getSystemMetadata(guid);
                	// TODO: need to update? we just looked it up
                	//IdentifierManager.getInstance().updateSystemMetadata(sysMeta);
                  
                } catch ( McdbDocNotFoundException mnfe) {
                  // handle inserts
                  try {
                   // create the system metadata. During the creatation, the data file in the eml may need to be reindexed.
                   boolean reindexDataObject = true;
                   sysMeta = SystemMetadataFactory.createSystemMetadata(reindexDataObject, newdocid, true, false);
                    
                    // save it to the map
                    HazelcastService.getInstance().getSystemMetadataMap().put(sysMeta.getIdentifier(), sysMeta);
                    
                    // submit for indexing
                    MetacatSolrIndex.getInstance().submit(sysMeta.getIdentifier(), sysMeta, null, true);
                    
                    // [re]index the resource map now that everything is saved
                    // see: https://projects.ecoinformatics.org/ecoinfo/issues/6520
                    Identifier potentialOreIdentifier = new Identifier();
        			potentialOreIdentifier.setValue(SystemMetadataFactory.RESOURCE_MAP_PREFIX + sysMeta.getIdentifier().getValue());
        			SystemMetadata oreSystemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(potentialOreIdentifier);
        			if (oreSystemMetadata != null) {
                        MetacatSolrIndex.getInstance().submit(oreSystemMetadata.getIdentifier(), oreSystemMetadata, null, false);
                        if (oreSystemMetadata.getObsoletes() != null) {
                            logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - submit the index task to reindex the obsoleted resource map " + 
                                              oreSystemMetadata.getObsoletes().getValue());
                            boolean isSysmetaChangeOnly = true;
                            SystemMetadata obsoletedOresysmeta = HazelcastService.getInstance().getSystemMetadataMap().get(oreSystemMetadata.getObsoletes());
                            MetacatSolrIndex.getInstance().submit(oreSystemMetadata.getObsoletes(), obsoletedOresysmeta, isSysmetaChangeOnly, null, true);
                        }
        			}
                    
                  } catch ( McdbDocNotFoundException dnfe ) {
                    logMetacat.warn(
                      "There was a problem finding the localId " +
                      newdocid + "The error was: " + dnfe.getMessage());
                    throw dnfe;
            
                  } catch ( AccessionNumberException ane ) {
            
                    logMetacat.error(
                      "There was a problem creating the accession number " +
                      "for " + newdocid + ". The error was: " + ane.getMessage());
                    throw ane;
                    
                  } // end try()
                                        
                }
                
              } // end if()
              
              
            } finally {
                // Return db connection
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }
            
            // set content type and other response header fields first
            //response.setContentType("text/xml");
            output += this.PROLOG;
            output += this.SUCCESS;
            output += "<docid>" + newdocid + "</docid>";
            output += this.SUCCESSCLOSE;
            
          } catch (NullPointerException npe) {
              //response.setContentType("text/xml");
              output += this.PROLOG;
              output += this.ERROR;
              output += npe.getMessage();
              output += this.ERRORCLOSE;
              logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - " +
            		          "Null pointer error when writing eml " +
            		          "document to the database: " + 
            		          npe.getMessage());
              npe.printStackTrace();
          }
        } catch (Exception e) {
            //response.setContentType("text/xml");
            output += this.PROLOG;
            output += this.ERROR;
            output += e.getMessage();
            output += this.ERRORCLOSE;
            logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - " +
            		        "General error when writing the xml object " +
            		        "document to the database: " + 
            		        e.getMessage(), e);
            e.printStackTrace();
        }
        
        if (qformat == null || qformat.equals("xml")) {
            if(response != null && out != null)
            {
              response.setContentType("text/xml");
              out.println(output);
            }
            return output;
        } else {
            try {
                DBTransform trans = new DBTransform();
                response.setContentType("text/html");
                trans.transformXMLDocument(output,
                        "message", "-//W3C//HTML//EN", qformat,
                        out, null, null);
                return output;
            } catch (Exception e) {
                
                logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - " +
                		         "General error: " + 
                		         e.getMessage());
                e.printStackTrace(System.out);
            }
        }
        return output;
    }
    
    /**
     * Parse XML Document to look for <!DOCTYPE ... PUBLIC/SYSTEM ... > in
     * order to decide whether to use validation parser
     */
    private static boolean needDTDValidation(StringReader xmlreader)
    throws IOException {
        StringBuffer cbuff = new StringBuffer();
        java.util.Stack<String> st = new java.util.Stack<String>();
        boolean validate = false;
        boolean commented = false;
        int c;
        int inx;
        
        // read from the stream until find the keywords
        while ((st.empty() || st.size() < 4) && ((c = xmlreader.read()) != -1)) {
            cbuff.append((char) c);
            
            if ((inx = cbuff.toString().indexOf("<!--")) != -1) {
                commented = true;
            }
            
            // "<!DOCTYPE" keyword is found; put it in the stack
            if ((inx = cbuff.toString().indexOf("<!DOCTYPE")) != -1) {
                cbuff = new StringBuffer();
                st.push("<!DOCTYPE");
            }
            // "PUBLIC" keyword is found; put it in the stack
            if ((inx = cbuff.toString().indexOf("PUBLIC")) != -1) {
                cbuff = new StringBuffer();
                st.push("PUBLIC");
            }
            // "SYSTEM" keyword is found; put it in the stack
            if ((inx = cbuff.toString().indexOf("SYSTEM")) != -1) {
                cbuff = new StringBuffer();
                st.push("SYSTEM");
            }
            // ">" character is found; put it in the stack
            // ">" is found twice: fisrt from <?xml ...?>
            // and second from <!DOCTYPE ... >
            if ((inx = cbuff.toString().indexOf(">")) != -1) {
                cbuff = new StringBuffer();
                st.push(">");
            }
        }
        
        // close the stream
        xmlreader.reset();
        
        // check the stack whether it contains the keywords:
        // "<!DOCTYPE", "PUBLIC" or "SYSTEM", and ">" in this order
        if (st.size() == 4) {
            if ((st.pop()).equals(">")
            && ((st.peek()).equals("PUBLIC") | (st.pop()).equals("SYSTEM"))
                    && (st.pop()).equals("<!DOCTYPE")) {
                validate = true && !commented;
            }
        }
        
        logMetacat.info("MetacatHandler.needDTDValidation - Validation for dtd is " + 
        		        validate);
        return validate;
    }
    
    // END OF INSERT/UPDATE SECTION
    
    /**
     * Handle the database delete request and delete an XML document from the
     * database connection
     */
    public void handleDeleteAction(Hashtable<String, String[]> params,
      HttpServletRequest request, HttpServletResponse response,
      String user, String[] groups) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Handle the validation request and return the results to the requestor
     */
    protected void handleValidateAction(HttpServletResponse response, Hashtable<String, String[]> params) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Look up the pid (guid)-to-docid mapping.
     * Returns XML on the response, e.g.:
     * <docid>sample.1.1</docid>
     * 
     * @param params
     * @param response
     * @throws IOException
     */
    protected void handleGetDocid(Hashtable<String, String[]> params, HttpServletResponse response) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Handle "getrevsionanddoctype" action Given a docid, return it's current
     * revision and doctype from data base The output is String look like
     * "rev;doctype"
     */
    protected void handleGetRevisionAndDocTypeAction(HttpServletResponse response,
            Hashtable<String, String[]> params) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Handle "getaccesscontrol" action. Read Access Control List from db
     * connection in XML format
     */
    protected void handleGetAccessControlAction(Hashtable<String,String[]> params, 
            HttpServletResponse response, String username,
            String[] groupnames) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Handle the "getprincipals" action. Read all principals from
     * authentication scheme in XML format
     * @throws IOException 
     */
    protected void handleGetPrincipalsAction(Writer out, String user,
            String password) throws IOException {
        try {
            AuthSession auth = new AuthSession();
            String principals = auth.getPrincipals(user, password);
            out.write(principals);
            
        } catch (Exception e) {
            out.write("<?xml version=\"1.0\"?>");
            out.write("<error>");
            out.write(e.getMessage());
            out.write("</error>");
        }
    }
    
    /**
     * Handle "getdoctypes" action. Read all doctypes from db connection in XML
     * format
     */
    protected void handleGetDoctypesAction(PrintWriter out, Hashtable<String, 
    		String[]> params, HttpServletResponse response) {
        try {
            DBUtil dbutil = new DBUtil();
            String doctypes = dbutil.readDoctypes();
            out.println(doctypes);
        } catch (Exception e) {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println(e.getMessage());
            out.println("</error>");
        }
    }
    
    /**
     * Handle the "getdtdschema" action. Read DTD or Schema file for a given
     * doctype from Metacat catalog system
     */
    protected void handleGetDTDSchemaAction(Hashtable<String, 
    		String[]> params, HttpServletResponse response) throws IOException{
        sendNotSupportMessage(response);
    }
    
    /**
     * Check if the document is registered in either the xml_documents or xml_revisions table
     * @param out the writer to write the xml results to
     * @param params request parameters
     * @param response the http servlet response
     */
    public void handleIdIsRegisteredAction(Hashtable<String, 
    		String[]> params, HttpServletResponse response) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Handle the "getalldocids" action. return a list of all docids registered
     * in the system
     */
    public void handleGetAllDocidsAction(Hashtable<String, 
    		String[]> params, HttpServletResponse response) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Handle the "getlastdocid" action. Get the latest docid with rev number
     * from db connection in XML format
     */
    public void handleGetMaxDocidAction(Hashtable<String, 
    		String[]> params, HttpServletResponse response) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Print a report from the event log based on filter parameters passed in
     * from the web.
     *
     * @param params the parameters from the web request
     * @param request the http request object for getting request details
     * @param response the http response object for writing output
     */
    protected void handleGetLogAction(Hashtable<String, String[]> params, 
    		HttpServletRequest request, HttpServletResponse response, 
    		String username, String[] groups, String sessionId) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Rebuild the index for one or more documents. If the docid parameter is
     * provided, rebuild for just that one document or list of documents. If
     * not, then rebuild the index for all documents in the xml_documents table.
     * 
     * @param params
     *            the parameters from the web request
     * @param request
     *            the http request object for getting request details
     * @param response
     *            the http response object for writing output
     * @param username
     *            the username of the authenticated user
     */
    protected void handleBuildIndexAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response,
            String username, String[] groups) throws IOException {
        sendNotSupportMessage(response);
    }
    
    /**
     * Rebuild the index for one or more documents. If the "pid" parameter is
     * provided, rebuild for just that one document (or list of documents). If
     * not, an error message will be returned.
     * 
     * @param params
     *            the parameters from the web request
     * @param request
     *            the http request object for getting request details
     * @param response
     *            the http response object for writing output
     * @param username
     *            the username of the authenticated user
     */
    protected void handleReindexAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response,
            String username, String[] groups) {
        
        // Get all of the parameters in the correct formats
        String[] pid = params.get("pid");
        PrintWriter out = null;
        // Process the documents
        StringBuffer results = new StringBuffer();
        // Rebuild the indices for appropriate documents
        try {
            response.setContentType("text/xml");
            out = response.getWriter();
            
            if (pid == null || pid.length == 0) {
                //report the error
                results = new StringBuffer();
                results.append("<error>");
                results.append("The parameter - pid is missing. Please check your parameter list.");
                results.append("</error>");
                //out.close(); it will be closed in the finally statement
                return;
            }
            // TODO: Check that the user is allowed to reindex this object, allow everyone for open annotations
            boolean isAuthorized = true;
   			String docid = IdentifierManager.getInstance().getLocalId(pid[0]);
			isAuthorized = DocumentImpl.hasWritePermission(username, groups, docid);
			if(!isAuthorized) {
			    isAuthorized = AuthUtil.isAdministrator(username, groups);
			}
			

            if (!isAuthorized) {
                results.append("<error>");
                results.append("The user \"" + username +
                        "\" is not authorized for this action.");
                results.append("</error>");
                //out.close(); it will be closed in the finally statement
                return;
            }
            
           
            
                Vector<String> successList = new Vector<String>();
                Vector<String> failedList = new Vector<String>();
                
                // Only process the requested documents
                for (int i = 0; i < pid.length; i++) {
                    String id = pid[i];
                    logMetacat.info("queueing doc index for pid " + id);
                    Identifier identifier = new Identifier();
                    identifier.setValue(id);
					SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(identifier);
					if (sysMeta == null) {
					     failedList.add(id);
					     logMetacat.info("no system metadata was found for pid " + id);
					} else {
						try {
							// submit for indexing
						    Map<String, List<Object>> fields = EventLog.getInstance().getIndexFields(identifier, Event.READ.xmlValue());
	                        MetacatSolrIndex.getInstance().submit(identifier, sysMeta, fields, false);
						} catch (Exception e) {
							failedList.add(id);
						    logMetacat.info("Error submitting to index for pid " + id);
						    continue;
						}
					    successList.add(id);
					    logMetacat.info("done queueing doc index for pid " + id);
					}
                }
                results.append("<results>\n");
                if(successList.size() >0) {
                    results.append("<success>\n");
                    for(String id : successList) {
                        results.append("<pid>" + id + "</pid>\n");
                    }
                    results.append("<note>The object(s) was/were submitted to the index queue successfully. However, this doesn't mean they were indexed successfully.</note>");
                    results.append("</success>");
                }
                
                if(failedList.size()>0) {
                    results.append("<error>\n");
                    for(String id : failedList) {
                        results.append("<pid>" + id + "</pid>\n");
                    }
                    results.append("<note>The object(s) couldn't be submitted to the index queue.</note>");
                    results.append("</error>");
                }
                results.append("</results>\n");
            
        } catch (Exception e) {
            logMetacat.error("MetacatHandler.handleReindex action - " +
            		         e.getMessage());
            e.printStackTrace();
            results.append("<error>");
            results.append("There was an error - "+e.getMessage());
            results.append("</error>");
        } finally {
            logMetacat.debug("================= in the finally statement");
            if(out != null) {
                logMetacat.debug("================= in the finally statement which out is not null");
                out.print(results.toString());
                out.close();
            }
        }
    }
    
    
    /**
     *Rebuild the index for all documents in the systemMetadata table.
     * 
     * @param params
     *            the parameters from the web request
     * @param request
     *            the http request object for getting request details
     * @param response
     *            the http response object for writing output
     * @param username
     *            the username of the authenticated user
     */
    protected void handleReindexAllAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response,
            String username, String[] groups) {
        
      
        
        // Rebuild the indices for all documents which are in the systemmetadata table
        PrintWriter out = null;
     // Process the documents
        StringBuffer results = new StringBuffer();
        try {
            response.setContentType("text/xml");
            out = response.getWriter();
            
            // Check that the user is authenticated as an administrator account
            if (!AuthUtil.isAdministrator(username, groups)) {
                out.print("<error>");
                out.print("The user \"" + username +
                        "\" is not authorized for this action.");
                out.print("</error>");
                out.close();
                return;
            }
          
           // Process all of the documents
           logMetacat.info("queueing doc index for all documents");
           try {
                    Runnable indexAll = new Runnable () {
                       public void run() {
                           List<String> resourceMapFormats = ResourceMapNamespaces.getNamespaces();
                           //System.out.println("MetacatHandler.handleReindexAllAction - the resource map format list is "+resourceMapFormats);
                           buildAllNonResourceMapIndex(resourceMapFormats);
                           buildAllResourceMapIndex(resourceMapFormats);
                          
                       }
                    };
                    Thread thread = new Thread(indexAll);
                    thread.start();
                    results.append("<success>");
                    results.append("The indexall action was accepted by the Metacat and it is working on the background right now. It doesn't guarantee all objects will be reindexed successfully. You may monitor the process through the Metacat log file.");
                    results.append("</success>");
                    logMetacat.info("done queueing index for all documents");
           } catch (Exception e) {
                    // report the error
                    results = new StringBuffer();
                    results.append("<error>");
                    results.append(e.getMessage());
                    results.append("</error>");
           }
          
        } catch (IOException e) {
            logMetacat.error("MetacatHandler.handleBuildIndexAction - " +
                             "Could not open http response for writing: " + 
                             e.getMessage());
            e.printStackTrace();
        } catch (MetacatUtilException ue) {
            logMetacat.error("MetacatHandler.handleBuildIndexAction - " +
                             "Could not determine if user is administrator: " + 
                             ue.getMessage());
            ue.printStackTrace();
        } finally {
            if(out != null) {
                out.print(results.toString());
                out.close();
            }
           
        }
    }
    
    
    /*
     * Index all non-resourcemap objects first. We don't put the list of pids in a vector anymore.
     */
    private void buildAllNonResourceMapIndex(List<String> resourceMapFormatList) {
        boolean firstTime = true;
        String sql = "select guid from systemmetadata";
        if (resourceMapFormatList != null && resourceMapFormatList.size() > 0) {
            for (String format :resourceMapFormatList) {
                if (format != null && !format.trim().equals("")) {
                    if (firstTime) {
                        sql = sql + " where object_format !='" + format + "'";
                        firstTime = false;
                    } else {
                        sql = sql + " and object_format !='" + format + "'";
                    }
                }
                
            }
            sql = sql + " order by date_uploaded asc";
        }
        logMetacat.info("MetacatHandler.buildAllNonResourceMapIndex - the final query is " + sql);
        try {
            long size = buildIndexFromQuery(sql);
            logMetacat.info("MetacatHandler.buildAllNonResourceMapIndex - the number of non-resource map objects is " + size + " being submitted to the index queue.");
        } catch (Exception e) {
            logMetacat.error("MetacatHandler.buildAllNonResourceMapIndex - can't index the objects since: " 
                    + e.getMessage());
        }
    }
    
    /*
     * Index all resource map objects. We don't put the list of pids in a vector anymore.
     */
    private void buildAllResourceMapIndex(List<String> resourceMapFormatList) {
        String sql = "select guid from systemmetadata";
        if (resourceMapFormatList != null && resourceMapFormatList.size() > 0) {
            boolean firstTime = true;
            for (String format :resourceMapFormatList) {
                if (format != null && !format.trim().equals("")) {
                    if (firstTime) {
                        sql = sql + " where object_format ='" + format + "'";
                        firstTime=false;
                    } else {
                        sql = sql + " or object_format ='" + format + "'";
                    }   
                }
            }
            sql = sql + " order by date_uploaded asc";
        }
        logMetacat.info("MetacatHandler.buildAllResourceMapIndex - the final query is " + sql);
        try {
            long size = buildIndexFromQuery(sql);
            logMetacat.info("MetacatHandler.buildAllResourceMapIndex - the number of resource map objects is " + size + " being submitted to the index queue.");
       } catch (Exception e) {
           logMetacat.error("MetacatHandler.buildAllResourceMapIndex - can't index the objects since: " 
                   + e.getMessage());
       }
    }
    
    /*
     * Build index of objects selecting from the given sql query.
     */
    private long buildIndexFromQuery(String sql) throws SQLException {
        DBConnection dbConn = null;
        long i = 0;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("MetacatHandler.buildIndexFromQuery");
            serialNumber = dbConn.getCheckOutSerialNumber();
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String guid = null;
                try {
                    guid = rs.getString(1);
                    Identifier identifier = new Identifier();
                    identifier.setValue(guid);
                    SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(identifier);
                    if (sysMeta != null) {
                        // submit for indexing
                        Map<String, List<Object>> fields = EventLog.getInstance().getIndexFields(identifier, Event.READ.xmlValue());
                        MetacatSolrIndex.getInstance().submit(identifier, sysMeta, fields, false);
                        i++;
                        logMetacat.debug("MetacatHandler.buildIndexFromQuery - queued SystemMetadata for indexing in the buildIndexFromQuery on pid: " + guid);
                    }
                } catch (Exception ee) {
                    logMetacat.warn("MetacatHandler.buildIndexFromQuery - can't queue the object " + guid + " for indexing since: " + ee.getMessage());
                }
            } 
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw e;
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return i;
    }
 
    /**
     * Handle documents passed to metacat that are encoded using the
     * "multipart/form-data" mime type. This is typically used for uploading
     * data files which may be binary and large.
     */
    protected void handleMultipartForm(HttpServletRequest request,
            HttpServletResponse response) {
        PrintWriter out = null;
        String action = null;
        File tempFile = null;
        
        // Get the out stream
        try {
            out = response.getWriter();
        } catch (IOException ioe2) {
            logMetacat.error("MetacatHandler.handleMultipartForm - " +
                             "Fatal Error: couldn't get response " + 
                             "output stream.");
            ioe2.printStackTrace(System.out);
            return;
        }
        
        // Parse the multipart form, and save the parameters in a Hashtable and
        // save the FileParts in a hashtable
        
        Hashtable<String,String[]> params = new Hashtable<String,String[]>();
        Hashtable<String,String> fileList = new Hashtable<String,String>();
        
        // Get the session information
        String username = "public";
        String password = null;
        String[] groupnames = null;
        String sess_id = null;
        
        // be aware of session expiration on every request
        SessionData sessionData = RequestUtil.getSessionData(request);
        
        if (sessionData != null) {
            username = sessionData.getUserName();
            password = sessionData.getPassword();
            groupnames = sessionData.getGroupNames();
            sess_id = sessionData.getId();
        }
        try {
            if (!AuthUtil.canInsertOrUpdate(username, groupnames)) {
                String msg = this.PROLOG +
                             this.ERROR +
                             "User '" + 
                             username + 
                             "' is not allowed to upload data" +
                             this.ERRORCLOSE;
                if(out != null)
                {
                  out.println(msg);
                }
                
                logMetacat.error("MetacatHandler.handleMultipartForm - " + 
                                 "User '" + 
                                 username + 
                                 "' not allowed to upload");
                out.close();
                return;
            }
        } catch (Exception e) {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println("Error: problem to determine if the user can upload data objects: " + e.getMessage());
            out.println("</error>");
            out.close();
            return;
        }
       
        
        int sizeLimit = 1000;
        String tmpDir = "/tmp";
        try {
            sizeLimit = 
                (new Integer(PropertyService.getProperty("replication.datafilesizelimit"))).intValue();
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("MetacatHandler.handleMultipartForm - " +
            		         "Could not determine data file size limit.  Using 1000. " + 
            		         pnfe.getMessage());
            pnfe.printStackTrace(System.out);
        }
        try {
            tmpDir = PropertyService.getProperty("application.tempDir");
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("MetacatHandler.handleMultipartForm - " +
            		         "Could not determine temp dir, using default. " + 
            		         pnfe.getMessage());
            pnfe.printStackTrace(System.out);
        }
        logMetacat.debug("MetacatHandler.handleMultipartForm - " +
        		         "The size limit of uploaded data files is: " + 
        		         sizeLimit);
        
        try {
        	boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        	// Create a factory for disk-based file items
        	DiskFileItemFactory factory = new DiskFileItemFactory();

        	// Configure a repository (to ensure a secure temp location is used)
        	File repository = new File(tmpDir);
        	factory.setRepository(repository);

        	// Create a new file upload handler
        	ServletFileUpload upload = new ServletFileUpload(factory);

        	// Parse the request
        	List<FileItem> items = upload.parseRequest(request);
        	
        	Iterator<FileItem> iter = items.iterator();
        	while (iter.hasNext()) {
        		FileItem item = iter.next();
        		String name = item.getFieldName();
        		
        	    if (item.isFormField()) {
        	    	
                    // it's a parameter part
                    String[] values = new String[1];
                    values[0] = item.getString();
                    params.put(name, values);
                    if (name.equals("action")) {
                        action = values[0];
                    }
                } else {
                    // it's a file part
                    String fileName = item.getName();                    

                    // write to disk
                    tempFile = MetacatUtil.writeTempUploadFile(item, fileName);
                    fileList.put(name, tempFile.getAbsolutePath());
                    fileList.put("filename", fileName);
                    fileList.put("name", tempFile.getAbsolutePath());
                }
            }
        } catch (Exception ioe) {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println("Error: problem reading multipart data: " + ioe.getMessage());
            out.println("</error>");
            out.close();
            return;
        }
        
       

        if (action.equals("upload")) {
            if (username != null && !username.equals("public")) {
                handleUploadAction(request, out, params, fileList, username,
                        groupnames, response);
            } else {                
                out.println("<?xml version=\"1.0\"?>");
                out.println("<error>");
                
                out.println("Permission denied for upload action");
                out.println("</error>");
            }
        } else if(action.equals("insertmultipart")) {
          if (username != null && !username.equals("public")) {
            logMetacat.debug("MetacatHandler.handleMultipartForm - handling multipart insert");
              handleInsertMultipartAction(request, response,
                            out, params, fileList, username, groupnames);
          } else {
              out.println("<?xml version=\"1.0\"?>");
              out.println("<error>");
              out.println("Permission denied for insertmultipart action");
              out.println("</error>");
          }
        } else {
            /*
             * try { out = response.getWriter(); } catch (IOException ioe2) {
             * System.err.println("Fatal Error: couldn't get response output
             * stream.");
             */
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println("Error: action not registered.  Please report this error.");
            out.println("</error>");
        }
        
        // clean up the temp file
        if (tempFile != null && tempFile.exists()) {
        	tempFile.delete();
        }
        out.close();
    }
    
    /**
     * Handle the upload action by saving the attached file to disk and
     * registering it in the Metacat db
     */
    private void handleInsertMultipartAction(HttpServletRequest request, 
            HttpServletResponse response,
            PrintWriter out, Hashtable<String,String[]> params, Hashtable<String,String> fileList,
            String username, String[] groupnames)
    {

      String action = null;
      String docid = null;
      String qformat = null;
      String output = "";
      
      /*
       * response.setContentType("text/xml"); try { out =
       * response.getWriter(); } catch (IOException ioe2) {
       * System.err.println("Fatal Error: couldn't get response output
       * stream.");
       */
      if(params.containsKey("qformat")) 
      {
          qformat = params.get("qformat")[0];
      }
      
      if (params.containsKey("docid")) 
      {
          docid = params.get("docid")[0];
      }
      
      Identifier identifier = new Identifier();
      identifier.setValue(docid);
      if(!D1NodeService.isValidIdentifier(identifier)) {
          output += this.PROLOG;
          output += this.ERROR;
          output += "The docid "+docid +" is not valid since it is null or contians the white space(s).";
          output += this.ERRORCLOSE;
          logMetacat.warn("MetacatHandler.handleInsertMultipartAction - " +
                          "The docid "+docid +" is not valid since it is null or contians the white space(s).");
          if (qformat == null || qformat.equals("xml")) {
              response.setContentType("text/xml");
              String cleanMessage = StringEscapeUtils.escapeXml(output);
              out.println(cleanMessage);
          } else {
              try {
                  DBTransform trans = new DBTransform();
                  response.setContentType("text/html");
                  trans.transformXMLDocument(output,
                          "message", "-//W3C//HTML//EN", qformat,
                          out, null, null);
              } catch (Exception e) {

                  logMetacat.error("MetacatHandler.handleInsertMultipartAction - General error: "
                          + e.getMessage());
                  e.printStackTrace(System.out);
              }
          }
          return;
      }
      
      
      
      // Make sure we have a docid and datafile
      if (docid != null && fileList.containsKey("datafile")) 
      {
        logMetacat.info("MetacatHandler.handleInsertMultipartAction - " +
        		        "Uploading data docid: " + docid);
        // Get a reference to the file part of the form
        //FilePart filePart = (FilePart) fileList.get("datafile");
        String fileName = fileList.get("filename");
        logMetacat.debug("MetacatHandler.handleInsertMultipartAction - " +
        		         "Uploading filename: " + fileName);
        // Check if the right file existed in the uploaded data
        if (fileName != null) 
        {
              
          try 
          {
              //logMetacat.info("Upload datafile " + docid
              // +"...", 10);
              //If document get lock data file grant
            if (DocumentImpl.getDataFileLockGrant(docid)) 
            {
              // Save the data file to disk using "docid" as the name
              String datafilepath = PropertyService.getProperty("application.datafilepath");
              File dataDirectory = new File(datafilepath);
              dataDirectory.mkdirs();
              File newFile = null;
    //          File tempFile = null;
              String tempFileName = fileList.get("name");
              String newFileName = dataDirectory + File.separator + docid;
              long size = 0;
              boolean fileExists = false;
                      
              try 
              {
                newFile = new File(newFileName);
                fileExists = newFile.exists();
                logMetacat.info("MetacatHandler.handleInsertMultipartAction - " +
                		        "new file status is: " + fileExists);
                if(fileExists)
                {
                  newFile.delete();
                  newFile.createNewFile();
                  fileExists = false;
                }
                
                if ( fileExists == false ) 
                {
                    // copy file to desired output location
                    try 
                    {
                        MetacatUtil.copyFile(tempFileName, newFileName);
                    } 
                    catch (IOException ioe) 
                    {
                        logMetacat.error("MetacatHandler.handleInsertMultipartAction - " +
                        		         "IO Exception copying file: " +
                                         ioe.getMessage());
                        ioe.printStackTrace(System.out);
                    }
                    size = newFile.length();
                    if (size == 0) 
                    {
                        throw new IOException("MetacatHandler.handleInsertMultipartAction - " +
                        		              "Uploaded file is 0 bytes!");
                    }
                }
                logMetacat.info("MetacatHandler.handleInsertMultipartAction - " +
                		        "Uploading the following to Metacat:" +
                                fileName + ", " + docid + ", " +
                                username + ", " + groupnames);
                
                // Read the file with appropriate encoding
                XmlStreamReader xsr = new XmlStreamReader(new FileInputStream(newFile));
                String encoding = xsr.getEncoding();
                Reader fr = new InputStreamReader(new FileInputStream(newFile), encoding);
                
                char[] c = new char[1024];
                int numread = fr.read(c, 0, 1024);
                StringBuffer sb = new StringBuffer();
                while(numread != -1)
                {
                  sb.append(c, 0, numread);
                  numread = fr.read(c, 0, 1024);
                }
                
                Enumeration<String> keys = params.keys();
                while(keys.hasMoreElements())
                { //convert the params to arrays
                  String key = keys.nextElement();
                  String[] paramValue = params.get(key);
                  String[] s = new String[1];
                  s[0] = paramValue[0];
                  params.put(key, s);
                }
                //add the doctext to the params
                String doctext = sb.toString();
                String[] doctextArr = new String[1];
                doctextArr[0] = doctext;
                params.put("doctext", doctextArr);
                boolean writeAccessRules = true;
                //call the insert routine
                String formatId = null;
                Checksum checksum = null;//for Metacat API, we don't calculate the checksum
                File file = null;
                handleInsertOrUpdateAction(request.getRemoteAddr(), request.getHeader("User-Agent"), response, out, 
                          params, username, groupnames, true, writeAccessRules, null, formatId, checksum, file);
              }
              catch(Exception e)
              {
                throw e;
              }
            }
          }
          catch(Exception e)
          {
              logMetacat.error("MetacatHandler.handleInsertMultipartAction - " +
            		           "error uploading text file via multipart: " + 
            		           e.getMessage());
              e.printStackTrace(System.out);;
          }
        }
      }
    }
    
    /**
     * Handle the upload action by saving the attached file to disk and
     * registering it in the Metacat db
     */
    private void handleUploadAction(HttpServletRequest request,
            PrintWriter out, Hashtable<String, String[]> params, 
            Hashtable<String,String> fileList, String username, String[] groupnames, 
            HttpServletResponse response) {
        
        String docid = null;
        String qformat = null;
        String output = "";

        /*
         * response.setContentType("text/xml"); try { out =
         * response.getWriter(); } catch (IOException ioe2) {
         * System.err.println("Fatal Error: couldn't get response output
         * stream.");
         */

        if (params.containsKey("docid")) {
            docid = params.get("docid")[0];
        }
        
        Identifier identifier = new Identifier();
        identifier.setValue(docid);
        if(!D1NodeService.isValidIdentifier(identifier)) {
            output += this.PROLOG;
            output += this.ERROR;
            output += "The docid "+docid +" is not valid since it is null or contians the white space(s).";
            output += this.ERRORCLOSE;
            logMetacat.warn("MetacatHandler.handleUploadAction - " +
                            "The docid "+docid +" is not valid since it is null or contians the white space(s).");
        } else {
         // Make sure we have a docid and datafile
            if (docid != null && fileList.containsKey("datafile")) {
                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                "Uploading data docid: " + docid);
                // Get a reference to the file part of the form
                //FilePart filePart = (FilePart) fileList.get("datafile");
                String fileName = fileList.get("filename");
                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                "Uploading filename: " + fileName);
                // Check if the right file existed in the uploaded data
                if (fileName != null) {

                    try {
                        //logMetacat.info("Upload datafile " + docid
                        // +"...", 10);
                        //If document get lock data file grant
                        if (DocumentImpl.getDataFileLockGrant(docid)) {
                            // Save the data file to disk using "docid" as the name
                            String datafilepath = PropertyService.getProperty("application.datafilepath");
                            File dataDirectory = new File(datafilepath);
                            dataDirectory.mkdirs();
                            File newFile = null;
                            //                    File tempFile = null;
                            String tempFileName = fileList.get("name");
                            String newFileName = dataDirectory + File.separator + docid;
                            long size = 0;
                            boolean fileExists = false;

                            try {
                                newFile = new File(newFileName);
                                fileExists = newFile.exists();
                                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                                "new file status is: " + fileExists);
                                if ( fileExists == false ) {
                                    // copy file to desired output location
                                    try {
                                        MetacatUtil.copyFile(tempFileName, newFileName);
                                    } catch (IOException ioe) {
                                        logMetacat.error("IO Exception copying file: " +
                                                ioe.getMessage());
                                        ioe.printStackTrace(System.out);
                                    }
                                    size = newFile.length();
                                    if (size == 0) {
                                        throw new IOException("Uploaded file is 0 bytes!");
                                    }
                                } // Latent bug here if the file already exists, then the
                                  // conditional fails but the document is still registered.
                                  // maybe this never happens because we already requested a lock?
                                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                                "Uploading the following to Metacat:" +
                                                fileName + ", " + docid + ", " +
                                                username + ", " + groupnames);
                                //register the file in the database (which generates
                                // an exception
                                //if the docid is not acceptable or other untoward
                                // things happen
                                DocumentImpl.registerDocument(fileName, "BIN", docid,
                                        username, groupnames);
                                
                                // generate system metadata about the doc
                                SystemMetadata sm = SystemMetadataFactory.createSystemMetadata(docid, false, false);
                                
                                // manage it in the store
                                HazelcastService.getInstance().getSystemMetadataMap().put(sm.getIdentifier(), sm);
                                
                                // submit for indexing
                                MetacatSolrIndex.getInstance().submit(sm.getIdentifier(), sm, null, true);
                                
                            } catch (Exception ee) {
                                // If the file did not exist before this method was 
                                // called and an exception is generated while 
                                // creating or registering it, then we want to delete
                                // the file from disk because the operation failed.
                                // However, if the file already existed before the 
                                // method was called, then the exception probably
                                // occurs when registering the document, and so we
                                // want to leave the old file in place.
                                if ( fileExists == false ) {
                                    newFile.delete();
                                }
                                
                                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                                "in Exception: fileExists is " + fileExists);
                                logMetacat.error("MetacatHandler.handleUploadAction - " +
                                                 "Upload Error: " + ee.getMessage());
                                throw ee;
                            }

                            EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"),
                                    username, docid, "upload");
                            // Force replication this data file
                            // To data file, "insert" and update is same
                            // The fourth parameter is null. Because it is
                            // notification server
                            // and this method is in MetaCatServerlet. It is
                            // original command,
                            // not get force replication info from another metacat
                            ForceReplicationHandler frh = new ForceReplicationHandler(
                                    docid, "insert", false, null);
                            logMetacat.debug("MetacatHandler.handleUploadAction - " +
                                             "ForceReplicationHandler created: " + 
                                             frh.toString());

                            // set content type and other response header fields
                            // first
                            output += "<?xml version=\"1.0\"?>";
                            output += "<success>";
                            output += "<docid>" + docid + "</docid>";
                            output += "<size>" + size + "</size>";
                            output += "</success>";
                        }

                    } catch (Exception e) {

                        output += "<?xml version=\"1.0\"?>";
                        output += "<error>";
                        output += e.getMessage();
                        output += "</error>";
                    }
                } else {
                    // the field did not contain a file
                    output += "<?xml version=\"1.0\"?>";
                    output += "<error>";
                    output += "The uploaded data did not contain a valid file.";
                    output += "</error>";
                }
            } else {
                // Error bcse docid missing or file missing
                output += "<?xml version=\"1.0\"?>";
                output += "<error>";
                output += "The uploaded data did not contain a valid docid "
                    + "or valid file.";
                output += "</error>";
            }
        }


        if(params.containsKey("qformat")) {
            qformat = params.get("qformat")[0];
        }
        
        if (qformat == null || qformat.equals("xml")) {
            response.setContentType("text/xml");
            out.println(output);
        } else {
            try {
                DBTransform trans = new DBTransform();
                response.setContentType("text/html");
                trans.transformXMLDocument(output,
                        "message", "-//W3C//HTML//EN", qformat,
                        out, null, null);
            } catch (Exception e) {

                logMetacat.error("MetacatHandler.handleUploadAction - General error: "
                        + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }
    
    /*
     * A method to handle set access action
     */
    protected void handleSetAccessAction(PrintWriter out, Hashtable<String, String[]> params,
            String username, HttpServletRequest request, HttpServletResponse response) {
        
        String permission = null;
        String permType = null;
        String permOrder = null;
        Vector<String> errorList = new Vector<String>();
        String error = null;
        Vector<String> successList = new Vector<String>();
        String success = null;
        boolean isEmlPkgMember = false;
        
		SystemMetadata mnSysMeta = null;
		Session session = null;
		Identifier pid = new Identifier();
        String guid = null;
		AccessPolicy mnAccessPolicy = null;
		SystemMetadata cnSysMeta = null;
        
        String[] docList = params.get("docid");
        String[] principalList = params.get("principal");
        String[] permissionList = params.get("permission");
        String[] permTypeList = params.get("permType");
        String[] permOrderList = params.get("permOrder");
        String[] qformatList = params.get("qformat");
        String[] accessBlock = params.get("accessBlock");
        if(accessBlock != null) {
            if (docList == null) {
                errorList.addElement("MetacatHandler.handleSetAccessAction - " +
                		             "Doc id missing.  Please check your " +
                		             "parameter list, it should look like: " + 
                		             "?action=setaccess&docid=<doc_id>&accessBlock=<access_section>");
                outputResponse(successList, errorList, out);
                return;
            } else {
                // look-up pid assuming docid
                guid = docList[0];
                try {
   	             String docid = DocumentUtil.getDocIdFromAccessionNumber(docList[0]);
   	             int rev = DocumentUtil.getRevisionFromAccessionNumber(docList[0]);
   	             guid = IdentifierManager.getInstance().getGUID(docid, rev);
               	 logMetacat.debug("Setting access on found pid: " + guid);
                } catch (McdbDocNotFoundException e) {
               	 // log the warning
               	 logMetacat.warn("No pid found for [assumed] docid: " + docList[0]);
                } catch (Exception e) {
                	logMetacat.warn("Error looking up pid for [assumed] dociid: " + docList[0]);
                }
            }
            try {
            	
              	logMetacat.debug("Setting access for docid: " + docList[0]);
                AccessControlForSingleFile accessControl = 
                    new AccessControlForSingleFile(docList[0]);
                accessControl.insertPermissions(accessBlock[0]);
                successList.addElement("MetacatHandler.handleSetAccessAction - " +
                		               "successfully replaced access block for doc id: " + 
                		               docList[0]);
                
                // force hazelcast to update system metadata
                HazelcastService.getInstance().refreshSystemMetadataEntry(guid);
         
                // Update the CN with the modified access policy
                logMetacat.debug("Setting CN access policy for pid: " + guid);

    			try {
    				ArrayList<String> guids = new ArrayList<String>(Arrays.asList(guid));
    				SyncAccessPolicy syncAP = new SyncAccessPolicy();

    				logMetacat.debug("Trying to syncing access policy for pid: "
    						+ guid);
    				syncAP.sync(guids);
    			} catch (Exception e) {
    				logMetacat.error("Error syncing pid: " + guid
    						+ " Exception " + e.getMessage());
                    e.printStackTrace(System.out);
    			}
            } catch(AccessControlException ace) {
                errorList.addElement("MetacatHandler.handleSetAccessAction - " +
                		             "access control error when setting " + 
                                     "access block: " + ace.getMessage());
            }
            outputResponse(successList, errorList, out);
            return;
        }
        
        // Make sure the parameter is not null
        if (docList == null || principalList == null || permTypeList == null
                || permissionList == null) {
            error = "MetacatHandler.handleSetAccessAction - Please check " +
                    "your parameter list, it should look like: "
                    + "?action=setaccess&docid=pipeline.1.1&principal=public"
                    + "&permission=read&permType=allow&permOrder=allowFirst";
            errorList.addElement(error);
            outputResponse(successList, errorList, out);
            return;
        }
        
        // Only select first element for permission, type and order
        permission = permissionList[0];
        permType = permTypeList[0];
        System.out.println("permission in MetacatHandler.handleSetAccessAction: " + permission);
        
        if (permOrderList != null) {
            permOrder = permOrderList[0];
        }
        
        // Get package doctype set
        Vector<String> packageSet = null;
        try {
            packageSet = 
                MetacatUtil.getOptionList(PropertyService.getProperty("xml.packagedoctypeset"));
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("MetacatHandler.handleSetAccessAction - " +
            		         "Could not find package doctype set.  Setting to null: " 
                    + pnfe.getMessage());
        }
        //debug
        if (packageSet != null) {
            for (int i = 0; i < packageSet.size(); i++) {
                logMetacat.debug("MetacatHandler.handleSetAccessAction - " +
                		         "doctype in package set: " + 
                		         packageSet.elementAt(i));
            }
        }
        
        // handle every accessionNumber
        for (int i = 0; i < docList.length; i++) {
            String docid = docList[i];

            if (docid.startsWith("urn:")) {
                try {
                    String actualDocId = LSIDUtil.getDocId(docid, false);
                    docid = actualDocId;
                } catch (ParseLSIDException ple) {
                    logMetacat.error("MetacatHandler.handleSetAccessAction - " +
                            "could not parse lsid: " + docid + " : " + ple.getMessage()); 
                    ple.printStackTrace(System.out);
                }
            }
            String accessionNumber = docid;
            String owner = null;
            String publicId = null;
            // Get document owner and public id
            try {
                owner = getFieldValueForDoc(accessionNumber, "user_owner");
                publicId = getFieldValueForDoc(accessionNumber, "doctype");
            } catch (Exception e) {
                logMetacat.error("MetacatHandler.handleSetAccessAction - " +
                		         "Error in handleSetAccessAction: " + 
                		         e.getMessage());
                e.printStackTrace(System.out);
                error = "Error in set access control for document - " + 
                        accessionNumber + e.getMessage();
                errorList.addElement(error);
                continue;
            }
            //check if user is the owner. Only owner can do owner
            if (username == null || owner == null || !username.equals(owner)) {
                error = "User - " + username + " does not have permission to set "
                        + "access control for docid - " + accessionNumber;
                errorList.addElement(error);
                System.out.println("user " + username + " does not have permission to set " + 
                        "access control for docid " + accessionNumber);
                continue;
            }
            
            //*** Find out if the document is a Member of an EML package.
            //*** (for efficiency, only check if it isn't already true
            //*** for the case of multiple files).
            if (isEmlPkgMember == false)
                isEmlPkgMember = (DBUtil.findDataSetDocIdForGivenDocument(accessionNumber) != null);
            
            // If docid publicid is BIN data file or other beta4, 6 package document
            // we could not do set access control. Because we don't want inconsistent
            // to its access docuemnt
            if (publicId != null && packageSet != null
                    && packageSet.contains(publicId) && isEmlPkgMember) {
                error = "Could not set access control to document " + accessionNumber
                        + "because it is in a pakcage and it has a access file for it";
                errorList.addElement(error);
                System.out.println("this is a beta4 or 6 document so we can't set the access element.");
                continue;
            }
            // for every principle
            for (int j = 0; j < principalList.length; j++) {
                String principal = principalList[j];
                try {
                    //insert permission
                    AccessControlForSingleFile accessControl = 
                        new AccessControlForSingleFile(accessionNumber);
                    //System.out.println("permission in MetacatHandler: " + 
                    //                   l.longValue());
                    //System.out.println("permission in MetacatHandler: " + 
                    //                   Integer.valueOf(AccessControlList.intValue(permission)).longValue());
                    accessControl.insertPermissions(principal, 
                      Integer.valueOf(AccessControlList.intValue(permission)).longValue(), 
                      permType, permOrder, null, null);
                    
                    // refresh using guid
                    guid = accessionNumber;
                    try {
          	             String tempDocid = DocumentUtil.getDocIdFromAccessionNumber(accessionNumber);
          	             int rev = DocumentUtil.getRevisionFromAccessionNumber(accessionNumber);
          	             guid = IdentifierManager.getInstance().getGUID(tempDocid, rev);
                      	 logMetacat.debug("Found pid: " + guid);
                    } catch (Exception e) {
                       	logMetacat.warn("Error looking up pid for [assumed] docid: " + accessionNumber);
                    }
                    
            		// force hazelcast to refresh system metadata
                    HazelcastService.getInstance().refreshSystemMetadataEntry(guid);
                    
                    logMetacat.debug("Synching CN access policy for pid: " + guid);

        			try {
        				ArrayList<String> guids = new ArrayList<String>(Arrays.asList(guid));
        				SyncAccessPolicy syncAP = new SyncAccessPolicy();
        				logMetacat.debug("Trying to syncing access policy for pid: " + guid);
        				syncAP.sync(guids);
        			} catch (Exception e) {
        				logMetacat.error("Error syncing pids: " + guid
        						+ " Exception " + e.getMessage(), e);
        			}
                } catch (Exception ee) {
                    logMetacat.error("MetacatHandler.handleSetAccessAction - " +
                    		         "Error inserting permission: " + 
                    		         ee.getMessage(), ee);
                    error = "Failed to set access control for document "
                            + accessionNumber + " because " + ee.getMessage();
                    errorList.addElement(error);
                    continue;
                }
            }
            

            //force replication when this action is called
            boolean isXml = true;
            if (publicId.equalsIgnoreCase("BIN")) {
                isXml = false;
            }
            ForceReplicationHandler frh = 
                new ForceReplicationHandler(accessionNumber, isXml, null);
            logMetacat.debug("MetacatHandler.handleSetAccessAction - " +
            		         "ForceReplicationHandler created: " + frh.toString());
            
        }
        if (errorList.isEmpty()) {
        	successList.addElement("MetacatHandler.handleSetAccessAction - " +
        			               "successfully added individual access for doc id: " + 
        			               docList[0]);
        }
        if (params.get("forwardto")  != null) {
            try {
                RequestUtil.forwardRequest(request, response, params);
            } catch (MetacatUtilException mue) {
                logMetacat.error("metaCatServlet.handleSetAccessAction - could not forward " +
                        "request. Sending output to response writer");
                mue.printStackTrace(System.out);
                outputResponse(successList, errorList, out);
            }               
        } else {
            outputResponse(successList, errorList, out);
        }
    }
    
    /*
     * A method try to determin a docid's public id, if couldn't find null will
     * be returned.
     */
    private String getFieldValueForDoc(String accessionNumber, String fieldName)
    throws Exception {
        if (accessionNumber == null || accessionNumber.equals("")
        || fieldName == null || fieldName.equals("")) { throw new Exception(
                "Docid or field name was not specified"); }
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String fieldValue = null;
        String docId = null;
        DBConnection conn = null;
        int serialNumber = -1;
        
        // get rid of revision if access number has
        docId = DocumentUtil.getDocIdFromString(accessionNumber);
        try {
            //check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("MetacatHandler.getPublicIdForDoc");
            serialNumber = conn.getCheckOutSerialNumber();
            pstmt = conn.prepareStatement("SELECT " + fieldName
                    + " FROM xml_documents " + "WHERE docid = ? ");
            
            pstmt.setString(1, docId);
            pstmt.execute();
            rs = pstmt.getResultSet();
            boolean hasRow = rs.next();
        //    int perm = 0;
            if (hasRow) {
                fieldValue = rs.getString(1);
            } else {
                throw new Exception("Could not find document: "
                        + accessionNumber);
            }
        } catch (Exception e) {
            logMetacat.error("MetacatHandler.getFieldValueForDoc - General error: "
                    + e.getMessage());
            throw e;
        } finally {
            try {
                rs.close();
                pstmt.close();
                
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
        return fieldValue;
    }
    
    /*
     * Get the list of documents from the database and return the list in an
     * Vector of identifiers.
     *
     * @ returns the array of identifiers
     */
    private Vector<String> getDocumentList() throws SQLException {
        Vector<String> docList = new Vector<String>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        DBConnection conn = null;
        int serialNumber = -1;
        
        try {
            //check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("MetacatHandler.getDocumentList");
            serialNumber = conn.getCheckOutSerialNumber();
            pstmt = conn.prepareStatement("SELECT docid, rev"
                    + " FROM xml_documents ");
            pstmt.execute();
            rs = pstmt.getResultSet();
            while (rs.next()) {
                String docid = rs.getString(1);
                String rev = rs.getString(2);
                docList.add(docid + "." + rev);
            }
        } catch (SQLException e) {
            logMetacat.error("MetacatHandler.getDocumentList - General exception: "
                    + e.getMessage());
            throw e;
        } finally {
            try {
                rs.close();
                pstmt.close();
                
            } catch (SQLException se) {
                logMetacat.error("MetacatHandler.getDocumentList - General exception: "
                        + se.getMessage());
                throw se;
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
        return docList;
    }
    
    /*
     * A method to output setAccess action result
     */
    private void outputResponse(Vector<String> successList, Vector<String> errorList,
            Writer out) {
    	try {
	        boolean error = false;
	        boolean success = false;
	        // Output prolog
	        out.write(PROLOG);
	        // output success message
	        if (successList != null) {
	            for (int i = 0; i < successList.size(); i++) {
	                out.write(SUCCESS);
	                out.write(successList.elementAt(i));
	                out.write(SUCCESSCLOSE);
	                success = true;
	            }
	        }
	        // output error message
	        if (errorList != null) {
	            for (int i = 0; i < errorList.size(); i++) {
	                out.write(ERROR);
	                out.write(errorList.elementAt(i));
	                out.write(ERRORCLOSE);
	                error = true;
	            }
	        }
	        
	        // if no error and no success info, send a error that nothing happened
	        if (!error && !success) {
	            out.write(ERROR);
	            out.write("Nothing happend for setaccess action");
	            out.write(ERRORCLOSE);
	        }
    	} catch (Exception e) {
			logMetacat.error(e.getMessage(), e);
		} 
    }
    
    /**
     * Schedule the sitemap generator to run periodically and update all
     * of the sitemap files for search indexing engines
     */
    protected void scheduleSitemapGeneration() {
        if (_sitemapScheduled) {
            logMetacat.debug("MetacatHandler.scheduleSitemapGeneration: Tried to call " + 
            "scheduleSitemapGeneration() when a sitemap was already scheduld. Doing nothing.");

            return;
        }

        String directoryName = null;
        long sitemapInterval = 0;
        
        try {
            directoryName = SystemUtil.getContextDir() + FileUtil.getFS() + "sitemaps";
            sitemapInterval = 
                Integer.parseInt(PropertyService.getProperty("sitemap.interval"));
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("MetacatHandler.scheduleSitemapGeneration - " +
                                "Could not run site map generation because property " +
                                "could not be found: " + pnfe.getMessage());
        }
        
        File directory = new File(directoryName);
        directory.mkdirs();

        // Determine sitemap location and entry base URLs. Prepends the 
        // secure server URL from the metacat configuration if the 
        // values in the properties don't start with 'http' (e.g., '/view')
        String serverUrl = "";
        String locationBase = "";
        String entryBase = "";
        String portalBase = "";
        List<String> portalFormats = new ArrayList();

        try {
            serverUrl = SystemUtil.getSecureServerURL();
            locationBase = PropertyService.getProperty("sitemap.location.base");
            entryBase = PropertyService.getProperty("sitemap.entry.base");
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("MetacatHandler.scheduleSitemapGeneration - " +
                                "Could not run site map generation because property " +
                                "could not be found: " + pnfe.getMessage());
        }

        try {
            portalBase = PropertyService.getProperty("sitemap.entry.portal.base");
            portalFormats.addAll(
                Arrays.asList(PropertyService.getProperty("sitemap.entry.portal.formats").split(";"))
            );
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.info("MetacatHandler.scheduleSitemapGeneration - " +
                            "Could not get portal-specific sitemap properties: " + pnfe.getMessage());
        }

        // Prepend server URL to locationBase if needed
        if (!locationBase.startsWith("http")) {
            locationBase = serverUrl + locationBase;
        }

        // Prepend server URL to entryBase if needed
        if (!entryBase.startsWith("http")) {
            entryBase = serverUrl + entryBase;
        }

        // Prepend server URL to portalBase if needed
        if (!portalBase.startsWith("http")) {
            portalBase = serverUrl + portalBase;
        }

        Sitemap smap = new Sitemap(directory, locationBase, entryBase, portalBase, portalFormats);
        long firstDelay = 10000; // in milliseconds

        timer.schedule(smap, firstDelay, sitemapInterval);
        _sitemapScheduled = true;
    }

    /**
     * @param sitemapScheduled toggle the _sitemapScheduled flag
     */
    public void set_sitemapScheduled(boolean sitemapScheduled) {
        _sitemapScheduled = sitemapScheduled;
    }
}
