package edu.ucsb.nceas.metacat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.ecoinformatics.eml.EMLParser;

import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;

import edu.ucsb.nceas.metacat.common.resourcemap.ResourceMapNamespaces;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.dataone.SystemMetadataFactory;
import edu.ucsb.nceas.metacat.event.MetacatDocumentEvent;
import edu.ucsb.nceas.metacat.event.MetacatEventService;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.index.queue.IndexGenerator;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
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
    protected void sendNotSupportMessage(HttpServletResponse response) throws IOException {
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
    public void handleLoginAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        sendNotSupportMessage(response);
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
      throws McdbDocNotFoundException {

        logMetacat.debug("MetacatHandler.readFromFilesystem() called.");

        FileInputStream fileInputStream = null;

        try {
          fileInputStream = new FileInputStream(filename);

        } catch ( FileNotFoundException fnfe ) {
          logMetacat.warn("There was an error reading the file " +
                           filename + ". The error was: "         +
                           fnfe.getMessage());
          throw new McdbDocNotFoundException(fnfe.getMessage());

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
                    SystemMetadataManager.getInstance().store(sysMeta);

                    // submit for indexing
                    MetacatSolrIndex.getInstance().submit(sysMeta.getIdentifier(), sysMeta, true);

                    // [re]index the resource map now that everything is saved
                    // see: https://projects.ecoinformatics.org/ecoinfo/issues/6520
                    Identifier potentialOreIdentifier = new Identifier();
                    potentialOreIdentifier.setValue(SystemMetadataFactory.RESOURCE_MAP_PREFIX + sysMeta.getIdentifier().getValue());
                    SystemMetadata oreSystemMetadata = SystemMetadataManager.getInstance().get(potentialOreIdentifier);
                    if (oreSystemMetadata != null) {
                        MetacatSolrIndex.getInstance().submit(oreSystemMetadata.getIdentifier(), oreSystemMetadata, false);
                        if (oreSystemMetadata.getObsoletes() != null) {
                            logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - submit the index task to reindex the obsoleted resource map " +
                                              oreSystemMetadata.getObsoletes().getValue());
                            boolean isSysmetaChangeOnly = true;
                            SystemMetadata obsoletedOresysmeta = SystemMetadataManager.getInstance().get(oreSystemMetadata.getObsoletes());
                            MetacatSolrIndex.getInstance().submit(oreSystemMetadata.getObsoletes(), obsoletedOresysmeta, isSysmetaChangeOnly, true);
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
    protected void handleGetPrincipalsAction(HttpServletResponse response, String user,
            String password) throws IOException {
        sendNotSupportMessage(response);
    }

    /**
     * Handle "getdoctypes" action. Read all doctypes from db connection in XML
     * format
     */
    protected void handleGetDoctypesAction(Hashtable<String,
            String[]> params, HttpServletResponse response) throws IOException {
        sendNotSupportMessage(response);
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
                    SystemMetadata sysMeta = SystemMetadataManager.getInstance().get(identifier);
                    if (sysMeta == null) {
                         failedList.add(id);
                         logMetacat.info("no system metadata was found for pid " + id);
                    } else {
                        try {
                            // submit for indexing
                            MetacatSolrIndex.getInstance().submit(identifier, sysMeta, false);
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
                    SystemMetadata sysMeta = SystemMetadataManager.getInstance().get(identifier);
                    if (sysMeta != null) {
                        // submit for indexing
                        boolean isSysmetaChangeOnly = false;
                        boolean followRevisions = false;
                        MetacatSolrIndex.getInstance()
                            .submit(identifier, sysMeta, isSysmetaChangeOnly, followRevisions,
                                    IndexGenerator.LOW_PRIORITY);
                        i++;
                        logMetacat.debug("MetacatHandler.buildIndexFromQuery - queued "
                                             + "SystemMetadata for indexing in the "
                                             + "buildIndexFromQuery on pid: " + guid);
                    }
                } catch (Exception ee) {
                    logMetacat.warn(
                        "MetacatHandler.buildIndexFromQuery - can't queue the object " + guid
                            + " for indexing since: " + ee.getMessage());
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
            HttpServletResponse response) throws IOException {
        sendNotSupportMessage(response);
    }


    /*
     * A method to handle set access action
     */
    protected void handleSetAccessAction(Hashtable<String, String[]> params,
            String username, HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
        sendNotSupportMessage(response);
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
