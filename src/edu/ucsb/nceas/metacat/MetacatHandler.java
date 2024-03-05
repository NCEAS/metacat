package edu.ucsb.nceas.metacat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.ecoinformatics.eml.EMLParser;

import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.event.MetacatDocumentEvent;
import edu.ucsb.nceas.metacat.event.MetacatEventService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.LSIDUtil;
import edu.ucsb.nceas.utilities.ParseLSIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * General entry point for the Metacat server which is called from various mechanisms such as the
 * standard MetacatServlet class and the various web service servlets such as RestServlet class.
 * All application logic should be encapsulated in this class, and the calling classes should only
 * contain parameter marshaling and unmarshalling code, delegating all else to this MetacatHandler
 * instance.
 *
 * @author Matthew Jones
 */
public class MetacatHandler {
    private static Log logMetacat = LogFactory.getLog(MetacatHandler.class);

    // Constants -- these should be final in a servlet
    private static final String PROLOG = "<?xml version=\"1.0\"?>";
    private static final String SUCCESS = "<success>";
    private static final String SUCCESSCLOSE = "</success>";
    private static final String ERROR = "<error>";
    private static final String ERRORCLOSE = "</error>";
    private static final String NOT_SUPPORT_MESSAGE =
        PROLOG + "\n" + ERROR + "The original Metacat API has been replaced, "
            + "and so this request is no longer supported. "
            + "Equivalent API methods now are available through "
            + "the DataONE API (see <https://knb.ecoinformatics.org/api>)." + ERRORCLOSE;


    /**
     * Default constructor.
     */
    public MetacatHandler() {

    }

    /**
     * Send back the not-support message
     *
     * @param response
     * @throws IOException
     */
    protected void sendNotSupportMessage(HttpServletResponse response) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            response.setStatus(301);
            out.println(NOT_SUPPORT_MESSAGE);
        }
    }

    /**
     * Read a document from metacat and return the InputStream. The dataType will be null.
     *
     * @param docid - the metacat docid to read
     * @return the document as an input stream
     * @throws PropertyNotFoundException
     * @throws ClassNotFoundException
     * @throws ParseLSIDException
     * @throws McdbException
     * @throws SQLException
     * @throws IOException
     */
    public static InputStream read(String docid)
        throws PropertyNotFoundException, ClassNotFoundException, ParseLSIDException, McdbException,
        SQLException, IOException {
        String dataType = null;
        return read(docid, dataType);
    }

    /**
     * Read a document from metacat and return the InputStream.  The XML or data document should be
     * on disk, but if not, read from the metacat database.
     *
     * @param docid    - the metacat docid to read
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
    public static InputStream read(String docid, String dataType)
        throws ParseLSIDException, PropertyNotFoundException, McdbException, SQLException,
        ClassNotFoundException, IOException {
        logMetacat.debug("MetacatHandler.read() called and the data type is " + dataType);

        InputStream inputStream = null;

        // be sure we have a local ID from an LSID
        if (docid.startsWith("urn:")) {
            try {
                docid = LSIDUtil.getDocId(docid, true);
            } catch (ParseLSIDException ple) {
                logMetacat.debug(
                    "There was a problem parsing the LSID. The " + "error message was: "
                        + ple.getMessage());
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
            // accommodate old clients that send docids without revision numbers
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
                // this is a metadata document
                // Get the xml (will try disk then DB)
                try {
                    // force the InputStream to be returned
                    OutputStream nout = null;
                    inputStream = doc.toXml(nout, null, null, true);
                } catch (McdbException e) {
                    // report the error
                    logMetacat.error(
                        "MetacatHandler.readFromMetacat() " + "- could not read document " + docid
                            + ": " + e.getMessage(), e);
                }
            }
        }
        return inputStream;
    }

    /**
     * Read a file from Metacat's configured file system data directory.
     *
     * @param filename The full path file name of the file to read
     * @return fileInputStream  The file to read as a FileInputStream
     */
    private static FileInputStream readFromFilesystem(String filename)
        throws McdbDocNotFoundException {

        logMetacat.debug("MetacatHandler.readFromFilesystem() called.");

        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(filename);

        } catch (FileNotFoundException fnfe) {
            logMetacat.warn("There was an error reading the file " + filename + ". The error was: "
                                + fnfe.getMessage());
            throw new McdbDocNotFoundException(fnfe.getMessage());

        }

        return fileInputStream;
    }


    /**
     * Handle the database putdocument request and write an XML document to the database connection
     * @param user  the user who sent the request
     * @param groups  the groups to which the user belongs
     * @param encoding  the encoding of the xml document
     * @param xmlBytes  the content of the xml document
     * @param formatId  the format id of the xml document
     * @param checksum  the checksum of the xml document
     * @param objectFile  the temporary file which the xml document is stored
     * @param docid  the docid of the document
     * @param action  the action to be performed (INSERT or UPDATE)
     * @return docid
     */
    public String handleInsertOrUpdateAction( String user, String[] groups, String encoding,
                                    byte[] xmlBytes, String formatId,Checksum checksum,
                                    File objectFile, String docid, String action) {
        DBConnection dbConn = null;
        int serialNumber = -1;
        String output = "";
        if (docid == null || docid.trim().equals("")) {
            String msg = this.PROLOG + this.ERROR + "Docid not specified" + this.ERRORCLOSE;
            return msg;
        }

        try {
            if (!AuthUtil.canInsertOrUpdate(user, groups)) {
                String msg = this.PROLOG + this.ERROR + "User '" + user
                    + "' is not allowed to insert or update. Check the Allowed and Denied "
                    + "Submitters lists"
                    + this.ERRORCLOSE;
                logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - " + "User '" + user
                                     + "' not allowed to insert and update");
                return msg;
            }
        } catch (MetacatUtilException ue) {
            logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - "
                                 + "Could not determine if user could insert or update: "
                                 + ue.getMessage(), ue);
            String msg = this.PROLOG + this.ERROR + "MetacatHandler.handleInsertOrUpdateAction - "
                + "Could not determine if user could insert or update: " + ue.getMessage()
                + this.ERRORCLOSE;
            return msg;
        }

        try {
            StringReader dtd = null;
            String doctext = new String(xmlBytes, encoding);
            StringReader xmlReader = new StringReader(doctext);
            boolean needValidation = false;
            String rule = null;
            String namespace = null;
            String schemaLocation = null;
            try {
                // look inside XML Document for <!DOCTYPE ... PUBLIC/SYSTEM ...
                // >
                // in order to decide whether to use validation parser
                needValidation = needDTDValidation(xmlReader);
                if (needValidation) {
                    // set a dtd base validation parser
                    logMetacat.debug(
                        "MetacatHandler.handleInsertOrUpdateAction - the xml object will be "
                            + "validate by a dtd");
                    rule = DocumentImpl.DTD;
                } else {
                    XMLSchemaService.getInstance().doRefresh();
                    namespace = XMLSchemaService.findDocumentNamespace(xmlReader);
                    if (namespace != null) {
                        logMetacat.debug(
                            "MetacatHandler.handleInsertOrUpdateAction - the xml object will be "
                                + "validated by a schema which has a target namespace: "
                                + namespace);
                        schemaLocation = XMLSchemaService.getInstance()
                            .findNamespaceAndSchemaLocalLocation(formatId, namespace);
                        if (namespace.compareTo(DocumentImpl.EML2_0_0NAMESPACE) == 0
                            || namespace.compareTo(DocumentImpl.EML2_0_1NAMESPACE) == 0) {
                            // set eml2 base     validation parser
                            rule = DocumentImpl.EML200;
                            needValidation = true;
                            // using emlparser to check id validation
                            @SuppressWarnings("unused") EMLParser parser =
                                new EMLParser(doctext);
                        } else if (namespace.compareTo(DocumentImpl.EML2_1_0NAMESPACE) == 0
                            || namespace.compareTo(DocumentImpl.EML2_1_1NAMESPACE) == 0
                            || namespace.compareTo(DocumentImpl.EML2_2_0NAMESPACE) == 0) {
                            // set eml2 base validation parser
                            rule = DocumentImpl.EML210;
                            needValidation = true;
                            // using emlparser to check id validation
                            @SuppressWarnings("unused") EMLParser parser =
                                new EMLParser(doctext);
                        } else {
                            if (!XMLSchemaService.isNamespaceRegistered(namespace)) {
                                throw new Exception("The namespace " + namespace
                                                        + " used in the xml object hasn't been "
                                                        + "registered in the Metacat. Metacat "
                                                        + "can't validate the object and rejected"
                                                        + " it. Please contact the operator of "
                                                        + "the Metacat for regsitering the "
                                                        + "namespace.");
                            }
                            // set schema base validation parser
                            rule = DocumentImpl.SCHEMA;
                            needValidation = true;
                        }
                    } else {
                        xmlReader = new StringReader(doctext);
                        String noNamespaceSchemaLocationAttr =
                            XMLSchemaService.findNoNamespaceSchemaLocationAttr(xmlReader);
                        if (noNamespaceSchemaLocationAttr != null) {
                            logMetacat.debug(
                                "MetacatHandler.handleInsertOrUpdateAction - the xml object will "
                                    + "be validated by a schema which deoe NOT have a target "
                                    + "namespace.");
                            schemaLocation = XMLSchemaService.getInstance()
                                .findNoNamespaceSchemaLocalLocation(formatId,
                                                                    noNamespaceSchemaLocationAttr);
                            rule = DocumentImpl.NONAMESPACESCHEMA;
                            needValidation = true;
                        } else {
                            logMetacat.debug(
                                "MetacatHandler.handleInsertOrUpdateAction - the xml object will "
                                    + "NOT be validated.");
                            rule = "";
                            needValidation = false;
                        }

                    }
                }

                String newdocid = null;

                String doAction = null;
                if (action.equals("insert") || action.equals("insertmultipart")) {
                    doAction = "INSERT";
                } else if (action.equals("update")) {
                    doAction = "UPDATE";
                } else {
                    String msg = this.PROLOG + this.ERROR
                                   + "MetacatHandler.handleInsertOrUpdateAction - "
                                   + "Could not handle this action: " + action
                                   + this.ERRORCLOSE;
                        return msg;
                }

                try {
                    // get a connection from the pool
                    dbConn = DBConnectionPool.getDBConnection(
                        "Metacathandler.handleInsertOrUpdateAction");
                    serialNumber = dbConn.getCheckOutSerialNumber();

                    // write the document to the database and disk
                    String accNumber = docid;
                    logMetacat.info( "MetacatHandler.handleInsertOrUpdateAction - "
                               + doAction + " " + accNumber + " with needValidation "
                               + needValidation + " and validation type " + rule);
                    Identifier identifier = new Identifier();
                    identifier.setValue(accNumber);
                    if (!D1NodeService.isValidIdentifier(identifier)) {
                        String error = "The docid " + accNumber
                            + " is not valid since it is null or contians the white space(s).";
                        logMetacat.warn("MetacatHandler.handleInsertOrUpdateAction - " + error);
                        throw new Exception(error);
                    }

                    newdocid = DocumentImpl.write(dbConn, dtd, doAction, accNumber,
                                              user,groups, rule, needValidation, encoding, xmlBytes,
                                              schemaLocation,checksum, objectFile);

                    // alert listeners of this event
                    MetacatDocumentEvent mde = new MetacatDocumentEvent();
                    mde.setDocid(accNumber);
                    mde.setDoctype(namespace);
                    mde.setAction(doAction);
                    mde.setUser(user);
                    mde.setGroups(groups);
                    MetacatEventService.getInstance().notifyMetacatEventObservers(mde);
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
                logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - "
                                     + "Null pointer error when writing eml "
                                     + "document to the database: " + npe.getMessage());
                npe.printStackTrace();
            }
        } catch (Exception e) {
            //response.setContentType("text/xml");
            output += this.PROLOG;
            output += this.ERROR;
            output += e.getMessage();
            output += this.ERRORCLOSE;
            logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - "
                                 + "General error when writing the xml object "
                                 + "document to the database: " + e.getMessage(), e);
        }
        return output;
    }

    /**
     * Parse XML Document to look for <!DOCTYPE ... PUBLIC/SYSTEM ... > in order to decide whether
     * to use validation parser
     */
    private static boolean needDTDValidation(StringReader xmlreader) throws IOException {
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
            if ((st.pop()).equals(">") && ((st.peek()).equals("PUBLIC") | (st.pop()).equals(
                "SYSTEM")) && (st.pop()).equals("<!DOCTYPE")) {
                validate = true && !commented;
            }
        }

        logMetacat.info("MetacatHandler.needDTDValidation - Validation for dtd is " + validate);
        return validate;
    }


}
