package edu.ucsb.nceas.metacat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;

import edu.ucsb.nceas.metacat.systemmetadata.ChecksumsManager;
import edu.ucsb.nceas.metacat.systemmetadata.MCSystemMetadata;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.ecoinformatics.eml.EMLParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandler;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.service.XMLSchema;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
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
    private static final String ERROR = "<error>";
    private static final String ERRORCLOSE = "</error>";
    private static final String NOT_SUPPORT_MESSAGE =
        PROLOG + "\n" + ERROR + "The original Metacat API has been replaced, "
            + "and so this request is no longer supported. "
            + "Equivalent API methods now are available through "
            + "the DataONE API (see <https://knb.ecoinformatics.org/api>)." + ERRORCLOSE;

    public enum Action {INSERT, UPDATE};

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
     * Read a document from metacat and return the InputStream.
     * @param localId  the metacat docid to read
     * @param dataType  the type of the object associated with docid
     * @return objectStream  the document as an InputStream
     * @throws InsufficientKarmaException
     * @throws ParseLSIDException
     * @throws PropertyNotFoundException
     * @throws McdbException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws ServiceFailure
     * @throws NoSuchAlgorithmException
     * @throws IllegalArgumentException
     */
    public static InputStream read(String localId, String dataType)
        throws ParseLSIDException, PropertyNotFoundException, McdbException, SQLException,
                                ClassNotFoundException, IOException, IllegalArgumentException,
                                    NoSuchAlgorithmException, ServiceFailure {
        logMetacat.debug("MetacatHandler.read() called and the data type is " + dataType);
        // Check if the object storage directory is in a good condition
        InputStream inputStream = null;
        // be sure we have a local ID from an LSID
        if (localId.startsWith("urn:")) {
            try {
                localId = LSIDUtil.getDocId(localId, true);
            } catch (ParseLSIDException ple) {
                logMetacat.debug(
                    "There was a problem parsing the LSID. The " + "error message was: "
                        + ple.getMessage());
                throw ple;
            }
        }
        String docid = DocumentUtil.getDocIdFromAccessionNumber(localId);
        int rev = DocumentUtil.getRevisionFromAccessionNumber(localId);
        String id = IdentifierManager.getInstance().getGUID(docid, rev);
        logMetacat.debug("The docid is " + docid + " and the revision is " + rev
                            + " for the local id " + localId + " And the pid is " + id);
        Identifier pid = new Identifier();
        pid.setValue(id);
        inputStream = read(pid);
        return inputStream;
    }

    /**
     * Read the object input stream for a given pid
     * @param pid  the pid which will be read
     * @return  the input stream representation of the object
     * @throws IllegalArgumentException
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws ServiceFailure
     * @throws IOException
     * @throws McdbException
     */
    public static InputStream read(Identifier pid) throws IllegalArgumentException,
                                                    FileNotFoundException, NoSuchAlgorithmException,
                                                    ServiceFailure, IOException, McdbException {
        if (pid == null || pid.getValue() == null || pid.getValue().isBlank()) {
            throw new IllegalArgumentException("Pid should not be blank in the read method");
        }
        try {
            return MetacatInitializer.getStorage().retrieveObject(pid);
        } catch (FileNotFoundException e) {
            throw new McdbException("Metacat cannot find the object with id " + pid.getValue()
                                        + " since " + e.getMessage());
        }
    }

    /**
     * Save the object into disk
     * @param sysmeta  the system metadata of the object, which contains the identifier
     * @param changeModificationDate  if Metacat needs to update the modification date of the system
     *              metadata when Metacat stores it.
     * @param action  the action of the request: Action.INSERT (new) or Action.UPDATE
     * @param docType  the type of object - data (BIN) or metadata
     * @param object  the input stream contains the content of the object
     * @param preSys  the system metadata of the obsoleted object in the update action.
     *                It will be ignored in the insert action.
     * @param user  the user initializing the request
     * @return the local Metacat docid for this object. It also serves as its file name.
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws IllegalArgumentException
     * @throws McdbException
     */
    public String save(SystemMetadata sysmeta, boolean changeModificationDate, Action action,
                        String docType, InputStream object, SystemMetadata preSys, String user)
                         throws InvalidRequest, ServiceFailure, InvalidSystemMetadata, IOException,
                               IllegalArgumentException, NoSuchAlgorithmException, McdbException {
        String localId = null;
        if (sysmeta == null) {
            throw new InvalidRequest("1181", "Metacat cannot save the object "
                            + " into disk since its system metadata is blank");
        }
        Identifier pid = sysmeta.getIdentifier();
        if (pid == null || pid.getValue() == null || pid.getValue().isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot save an object with a blank pid");
        }
        if (docType == null || docType.isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot save the object for "
                                     + pid.getValue() + " into disk since the doc type is blank.");
        }
        if (action == null) {
            throw new InvalidRequest("1181", "Metacat cannot save the object for "
                    + pid.getValue() + " into disk since the action value (which should be `insert`"
                    + " or `update`) is blank.");
        }
        if (user == null || user.isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot save the object for "
                            + pid.getValue() + " into disk since the client identity is blank");
        }
        if (docType != null && !docType.equals(DocumentImpl.BIN)) {
            // Handle the metadata objects and it needs validation
            validateSciMeta(pid, sysmeta.getFormatId());
        }
        int serialNumber = -1;
        DBConnection conn = null;
        try {
            conn = DBConnectionPool.getDBConnection("MetacatHandler.save");
            serialNumber = conn.getCheckOutSerialNumber();
            StringBuffer error = new StringBuffer();
            error.append("Metacat cannot save the object ").append(pid.getValue())
                                                                .append(" into disk since ");
            try {
                conn.setAutoCommit(false);
                Identifier prePid = null;
                if (preSys != null) {
                    prePid = preSys.getIdentifier();
                }
                // Register the new object into the xml_documents and identifier table.
                localId = registerToDB(pid, action, conn, user, docType, prePid);
                // Store the checksums
                if (sysmeta instanceof MCSystemMetadata) {
                    ChecksumsManager manager = new ChecksumsManager();
                    manager.save(pid, ((MCSystemMetadata) sysmeta).getChecksums(), conn);
                }
                // Save the system metadata for the new object
                // Since this is a new object, we don't need to check system metadata version
                SystemMetadataManager.getInstance().store(sysmeta, changeModificationDate, conn,
                                                SystemMetadataManager.SysMetaVersion.UNCHECKED);
                if (action == Action.UPDATE) {
                    if(preSys ==  null) {
                        throw new InvalidRequest("1181", "Metacat cannot save the object for "
                                + pid.getValue() + " into disk since the system metadata of the "
                                + "obsoleted object should not be blank.");
                    }
                    //It is update, we need to store the system metadata of the obsoleted pid as well
                    // We need to check if the previous system has the latest version
                    // Set changeModifyTime true
                    SystemMetadataManager.getInstance().store(preSys, true, conn,
                                                      SystemMetadataManager.SysMetaVersion.CHECKED);
                }
                conn.commit();
            } catch (Exception e) {
                error = clearUp(e, error, conn);
                throw new ServiceFailure("1190", error.toString());
            }
        } catch (SQLException e) {
            throw new ServiceFailure("1190", "Metacat cannot save the object into disk since "
                                    + " it can't get a DBConnection: "+ e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                logMetacat.warn("Metacat cannot set back autoCommit true for DBConnection since "
                                + e.getMessage());
            }
            // Return database connection to the pool
            if (conn != null) {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
            IOUtils.closeQuietly(object);
        }
        return localId;
    }

    /**
     * Clear up database and object file when the save failed. Try to restore the original state.
     * @param e  the exception arose in the save method
     * @param error  the string buffer holding the error message
     * @param conn  the connection to db
     * @return the string buffer which holds all error message.
     */
    private StringBuffer clearUp(Exception e, StringBuffer error, DBConnection conn) {
        error.append(e.getMessage());
        // Clean up database
        try {
            conn.rollback();
        } catch (SQLException ee) {
            error.append(" Moreover, it cannot roll back the change in DB since ")
                            .append(ee.getMessage());
        }
        return error;
    }

    /**
     * Register the dataone identifier into database (the xml_documents/revisions, identifier
     * tables). It also generate a local Metacat doc id.
     * @param pid  the dataone identifier
     * @param action  insert or update
     * @param conn  the connection to db
     * @param user  the user who requests the action
     * @param docType  BIN for data objects; the format id for metadata objects
     * @param prePid  the old identifier which will be updated. If this is an update action, you
     *                 should specify it. For the insert action, it is ignored and can be null.
     * @return the generated Metacat local docid
     * @throws ServiceFailure
     * @throws InvalidRequest
     */
    protected String registerToDB(Identifier pid, Action action, DBConnection conn,
                                String user, String docType, Identifier prePid)
                                                    throws ServiceFailure, InvalidRequest {
        if (pid == null || pid.getValue() == null || pid.getValue().isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot register "
                                        + "a blank identifier into database.");
        }
        if (docType == null || docType.isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot register "
                    + pid.getValue() + " into database since the doc type is blank.");
        }
        String localId;
        if (action == Action.INSERT) {
            localId = DocumentUtil.generateDocumentId(1);
        } else {
            // Update action
            //localid should already exist in the identifier table, so just find it
            if (prePid == null || prePid.getValue() == null || prePid.getValue().isBlank()) {
                throw new InvalidRequest("1181", "Metacat cannot register "
                                   + "records into database since it tries to update a blank pid.");
            }
            if (docType.equals(DocumentImpl.BIN)) {
                localId = DocumentUtil.generateDocumentId(1);
            } else {
                try {
                    logMetacat.debug("looking in identifier table for pid " + prePid.getValue());
                    localId = IdentifierManager.getInstance().getLocalId(prePid.getValue());
                    logMetacat.debug("localId: " + localId + " for the pid " + prePid.getValue());
                    //increment the revision
                    String docid = localId.substring(0, localId.lastIndexOf("."));
                    String revS = localId.substring(localId.lastIndexOf(".") + 1);
                    int rev = Integer.parseInt(revS);
                    rev++;
                    localId = docid + "." + rev;
                    logMetacat.debug("incremented localId: " + localId);
                } catch (McdbDocNotFoundException e) {
                    throw new ServiceFailure(
                        "1190", "The object " + "pid " + pid.getValue()
                        + " should have been in the identifier table, but it wasn't: "
                        + e.getMessage());

                } catch (SQLException e) {
                    throw new ServiceFailure(
                        "1190", "Metacat couldn't identify if the pid " + pid.getValue()
                            + " is in the identifier table since " + e.getMessage());
                }
            }
        }
        logMetacat.debug("Mapping pid " + pid.getValue() + " with docid " + localId);
        try {
            IdentifierManager.getInstance().createMapping(pid.getValue(), localId, conn);
            String docName = docType;
            if (docType.equals(DocumentImpl.BIN)) {
                docName = localId;
            }
            logMetacat.debug("Register the docid " + localId + " with doc type " + docType
                             + " into xml_documents/xml_revsions table");
            DocumentImpl.registerDocument(docName, docType, conn, localId, user);
        } catch (PropertyNotFoundException | MetacatException | SQLException
                                                | AccessionNumberException e) {
            throw new ServiceFailure("1190", "Metacat couldn't register " + pid.getValue()
                                  + " into the database since " + e.getMessage());
        }
        return localId;
    }

    /**
     * Validate a scientific metadata object. It will throw an InvalidRequest if it is invalid.
     * @param pid  the identifier of the object which will be validate. The identifier can be used
     *              to get the content of the object.
     * @param formatId  format id of the object
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws ServiceException
     * @throws PropertyNotFoundException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws IllegalArgumentException
     * @throws McdbException
     * @throws SAXException
     * @throws MetacatException
     */
    protected void validateSciMeta(Identifier pid, ObjectFormatIdentifier formatId)
                                         throws InvalidRequest, ServiceFailure, IOException,
                                IllegalArgumentException, NoSuchAlgorithmException, McdbException {
        NonXMLMetadataHandler handler =
                NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
            if (handler != null) {
                // a non-xml metadata object path
                logMetacat.debug("Validate the non-XML scientific metadata object.");
                try (InputStream content = read(pid)) {
                    handler.validate(content);
                }
            } else {
                // an XML object
                logMetacat.debug("Validate the XML scientific metadata object.");
                validateXmlSciMeta(pid, formatId.getValue());
            }
    }

    /**
     * Validate an XML object. If it is not valid, an InvalidRequest will be thrown.
     * @param pid  the identifier of the object which will be validate. The identifier can be used
     *              to get the content of the object.
     * @param formatId  format id of the object
     * @throws IOException
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws NoSuchAlgorithmException
     * @throws IllegalArgumentException
     * @throws McdbException
     */
    protected void validateXmlSciMeta(Identifier pid, String formatId)
                                 throws IOException,ServiceFailure, InvalidRequest,
                                IllegalArgumentException, NoSuchAlgorithmException, McdbException {
        boolean needValidation = false;
        String rule = null;
        String namespace = null;
        String schemaLocation = null;
        byte[] object = null;
        try (InputStream content = read(pid)) {
            object = IOUtils.toByteArray(content);
        }
        String doctext = new String(object, StandardCharsets.UTF_8);
        StringReader xmlReader = null;
        try {
            xmlReader = new StringReader(doctext);
            needValidation = needDTDValidation(xmlReader);
            if (needValidation) {
                // set a dtd base validation parser
                logMetacat.debug(
                    "MetacatHandler.handleInsertOrUpdateAction - the xml object will be "
                        + "validate by a dtd");
                rule = DocumentImpl.DTD;
            } else {
                XMLSchemaService.getInstance().doRefresh();
                xmlReader = new StringReader(doctext);
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
                            throw new ServiceFailure("1190", "The namespace " + namespace
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
                                + "name space.");
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
            xmlReader = new StringReader(doctext);
            Vector<XMLSchema> schemaList = XMLSchemaService.findSchemasInXML(xmlReader);
            xmlReader = new StringReader(doctext);
            // set the dtd part null;
            XMLReader parser = DocumentImpl.initializeParser(schemaList, null, rule, needValidation,
                                                             schemaLocation);
            try {
                parser.parse(new InputSource(xmlReader));
            } catch (SAXException e) {
                throw new InvalidRequest("1181", "Invalid metadata: " + e.getMessage());
            }
        } catch (ServiceException | MetacatException | PropertyNotFoundException | SAXException e) {
            throw new ServiceFailure("1190", "Metacat cannot validate the object since "
                                                                            + e.getMessage());
        } finally {
            if (xmlReader != null) {
                // We don't use try-resource since the xmlReade object is assigned multiple times.
                IOUtils.closeQuietly(xmlReader);
            }
        }
    }

    /**
     * Parse XML Document to look for <!DOCTYPE ... PUBLIC/SYSTEM ... > in order to decide whether
     * to use validation parser
     */
    private boolean needDTDValidation(StringReader xmlreader) throws IOException {
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
