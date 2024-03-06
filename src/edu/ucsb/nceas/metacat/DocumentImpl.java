package edu.ucsb.nceas.metacat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.dataone.SyncAccessPolicy;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.XMLSchema;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A class that represents an XML document. It can be created with a simple document identifier from
 * a database connection. It also will write an XML text document to a database connection using
 * SAX.
 */
public class DocumentImpl {
    /* Constants */
    public static final String SCHEMA = "Schema";
    public static final String NONAMESPACESCHEMA = "NoNamespaceSchema";
    public static final String DTD = "DTD";
    public static final String EML200 = "eml200";
    public static final String EML210 = "eml210";
    public static final String EXTERNALSCHEMALOCATIONPROPERTY =
        "http://apache.org/xml/properties/schema/external-schemaLocation";
    public static final String EXTERNALNONAMESPACESCHEMALOCATIONPROPERTY =
        "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";
    public static final String REVISIONTABLE = "xml_revisions";
    public static final String DOCUMENTTABLE = "xml_documents";
    public static final String BIN = "BIN";
    public static final String DECLARATIONHANDLERPROPERTY =
        "http://xml.org/sax/properties/declaration-handler";
    public static final String LEXICALPROPERTY = "http://xml.org/sax/properties/lexical-handler";
    public static final String VALIDATIONFEATURE = "http://xml.org/sax/features/validation";
    public static final String SCHEMAVALIDATIONFEATURE =
        "http://apache.org/xml/features/validation/schema";
    public static final String FULLSCHEMAVALIDATIONFEATURE =
        "http://apache.org/xml/features/validation/schema-full-checking";
    public static final String NAMESPACEFEATURE = "http://xml.org/sax/features/namespaces";
    public static final String NAMESPACEPREFIXESFEATURE =
        "http://xml.org/sax/features/namespace-prefixes";

    public static final String EML2_0_0NAMESPACE;
    public static final String EML2_0_1NAMESPACE;
    public static final String EML2_1_0NAMESPACE;
    public static final String EML2_1_1NAMESPACE;
    public static final String EML2_2_0NAMESPACE;
    public static final String RDF_SYNTAX_NAMESPACE;

    static {
        String eml200NameSpace = null;
        String eml201NameSpace = null;
        String eml210NameSpace = null;
        String eml211NameSpace = null;
        String eml220NameSpace = null;
        String rdfNameSpace = null;
        try {
            eml200NameSpace = PropertyService.getProperty("xml.eml2_0_0namespace");
            eml201NameSpace = PropertyService.getProperty("xml.eml2_0_1namespace");
            eml210NameSpace = PropertyService.getProperty("xml.eml2_1_0namespace");
            eml211NameSpace = PropertyService.getProperty("xml.eml2_1_1namespace");
            eml220NameSpace = PropertyService.getProperty("xml.eml2_2_0namespace");
            rdfNameSpace = PropertyService.getProperty("xml.rdf_syntax_namespace");
        } catch (PropertyNotFoundException pnfe) {
            System.err.println("Could not get property in static block: " + pnfe.getMessage());
        }

        EML2_2_0NAMESPACE = eml220NameSpace;
        EML2_1_1NAMESPACE = eml211NameSpace;
        // "eml://ecoinformatics.org/eml-2.1.1";
        EML2_1_0NAMESPACE = eml210NameSpace;
        // "eml://ecoinformatics.org/eml-2.1.0";
        EML2_0_1NAMESPACE = eml201NameSpace;
        // "eml://ecoinformatics.org/eml-2.0.1";
        EML2_0_0NAMESPACE = eml200NameSpace;
        // "eml://ecoinformatics.org/eml-2.0.0";
        RDF_SYNTAX_NAMESPACE = rdfNameSpace;
    }

    public static final String DOCNAME = "docname";
    public static final String PUBLICID = "publicid";
    public static final String SYSTEMID = "systemid";
    static final int ALL = 1;
    static final int WRITE = 2;
    static final int READ = 4;
    protected DBConnection connection = null;
    protected String docname = null;
    protected String doctype = null;
    private String validateType = null; //base on dtd or schema
    private Date createdate = null;
    private Date updatedate = null;
    private String system_id = null;
    private String userowner = null;
    private String userupdated = null;
    protected String docid = null; // without revision
    private int rev;
    protected long rootnodeid;

    private static Log logMetacat = LogFactory.getLog(DocumentImpl.class);

    /**
     * Default constructor
     */
    public DocumentImpl() {

    }

    /**
     * Constructor used to create a document and read the document information from the database. If
     * readNodes is false, then the node data is not read at this time, but is deferred until it is
     * needed (such as when a call to toXml() is made).
     *
     * @param conn      the database connection from which to read the document
     * @param readNodes flag indicating whether the xmlnodes should be read
     */
    public DocumentImpl(String accNum, boolean readNodes) throws McdbException {
        try {
            //this.conn = conn;
            this.docid = DocumentUtil.getDocIdFromAccessionNumber(accNum);
            this.rev = DocumentUtil.getRevisionFromAccessionNumber(accNum);

            // Look up the document information
            getDocumentInfo(docid, rev);
        } catch (McdbException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new McdbException("Error reading document: " + docid);
        }
    }

    /**
     * Constructor, creates document from database connection, used for reading the document
     *
     * @param conn  the database connection from which to read the document
     * @param docid the identifier of the document to be created
     */
    public DocumentImpl(String docid) throws McdbException {
        this(docid, true);
    }

    /**
     * Construct a new document instance, writing the contents to the database. This method is
     * called from DBSAXHandler because we need to know the root element name for documents without
     * a DOCTYPE before creating it.
     *
     * In this constructor, the docid is without rev. There is a string rev to specify the revision
     * user want to upadate. The revion is only need to be greater than current one. It is not need
     * to be sequent number just after current one. So it is only used in update action
     *
     * @param conn       the JDBC Connection to which all information is written
     * @param rootnodeid - sequence id of the root node in the document
     * @param docname    - the name of DTD, i.e. the name immediately following the DOCTYPE keyword
     *                   ( should be the root element name ) or the root element name if no DOCTYPE
     *                   declaration provided (Oracle's and IBM parsers are not aware if it is not
     *                   the root element name)
     * @param doctype    - Public ID of the DTD, i.e. the name immediately following the PUBLIC
     *                   keyword in DOCTYPE declaration or the docname if no Public ID provided or
     *                   null if no DOCTYPE declaration provided
     * @param docid      the docid to use for the UPDATE, no version number
     * @param newVersion,   need to be update
     * @param action     the action to be performed (INSERT OR UPDATE)
     * @param user       the user that owns the document
     * @param catalogId  the identifier of catalog which this document belongs to
     * @param createDate  the created date of this document
     * @param updateDate  the updated date of this document
     */
    public DocumentImpl(
        DBConnection conn, long rootNodeId, String docName, String docType, String docId,
        String newRevision, String action, String user, String catalogId,
        Date createDate, Date updateDate) throws SQLException, Exception {
        this.connection = conn;
        this.rootnodeid = rootNodeId;
        this.docname = docName;
        this.doctype = docType;
        this.docid = docId;
        this.rev = Integer.parseInt(newRevision);
        writeDocumentToDB(action, user, catalogId, createDate, updateDate);
    }

    /**
     * Register a document that resides on the filesystem with the database. (ie, just an entry in
     * xml_documents). Creates a reference to a filesystem document (used for non-xml data files).
     * This class only be called in MetaCatServerlet.
     *
     * @param docname    - the name of DTD, i.e. the name immediately following the DOCTYPE keyword
     *                   ( should be the root element name ) or the root element name if no DOCTYPE
     *                   declaration provided (Oracle's and IBM parsers are not aware if it is not
     *                   the root element name)
     * @param doctype    - Public ID of the DTD, i.e. the name immediately following the PUBLIC
     *                   keyword in DOCTYPE declaration or the docname if no Public ID provided or
     *                   null if no DOCTYPE declaration provided
     * @param accnum     the accession number to use for the INSERT OR UPDATE, which includes a
     *                   revision number for this revision of the document (e.g., knb.1.1)
     * @param user       the user that owns the document
     */
    public static void registerDocument(String docname, String doctype, String accnum, String user)
                                        throws SQLException, AccessionNumberException, Exception {
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            conn = DBConnectionPool.getDBConnection("DocumentImpl.registerDocumentInreplication");
            serialNumber = conn.getCheckOutSerialNumber();
            conn.setAutoCommit(false);
            String action = null;
            String docIdWithoutRev = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            int userSpecifyRev = DocumentUtil.getRevisionFromAccessionNumber(accnum);
            action = checkRevInXMLDocuments(docIdWithoutRev, userSpecifyRev);
            logMetacat.debug("after check rev, the action is " + action);
            if (action.equalsIgnoreCase("UPDATE")) {
                archiveDocToRevision(conn, docIdWithoutRev, user);
            }
            String rev = Integer.toString(userSpecifyRev);
            modifyRecordsInGivenTable(DOCUMENTTABLE, action, docIdWithoutRev, doctype, docname,
                                      user, rev, null, null, conn);
            // null and null is createdate and updatedate
            // null will create current time
            conn.commit();
            conn.setAutoCommit(true);
        } catch (Exception e) {
            conn.rollback();
            conn.setAutoCommit(true);
            throw e;
        } finally {
            //check in DBConnection
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }

    }

    /**
     * This method will insert or update xml-documents or xml_revision table
     * @param tableName  the name of the table to which will be insert
     * @param action  the action to be performed (INSERT OR UPDATE)
     * @param docid  the docid of the document
     * @param doctype  the type of the document
     * @param docname  the name of the document
     * @param user  the owner of the document
     * @param rev  the revision of the document
     * @param createDate  the created date of the document
     * @param updateDate  the updated date of the document
     * @param dbconn  the JDBC Connection to which all information is written
     * @throws Exception
     */
    private static void modifyRecordsInGivenTable(
        String tableName, String action, String docid, String doctype, String docname, String user,
        String rev, Date createDate, Date updateDate, DBConnection dbconn)
        throws Exception {

        PreparedStatement pstmt = null;
        int revision = Integer.parseInt(rev);
        String sqlDateString = DatabaseService.getInstance().getDBAdapter().getDateTimeFunction();
        Date today = new Date(Calendar.getInstance().getTimeInMillis());

        if (createDate == null) {
            createDate = today;
        }

        if (updateDate == null) {
            updateDate = today;
        }

        try {

            StringBuffer sql = new StringBuffer();
            if (action != null && action.equals("INSERT")) {

                sql.append("insert into ");
                sql.append(tableName);
                sql.append(" (docid, docname, doctype, ");
                sql.append("user_owner, user_updated, rev, date_created, ");
                sql.append("date_updated) values (");
                sql.append("?, ");
                sql.append("?, ");
                sql.append("?, ");
                sql.append("?, ");
                sql.append("?, ");
                sql.append("?, ");
                sql.append("?, ");
                sql.append("? )");
                // set the values
                pstmt = dbconn.prepareStatement(sql.toString());
                pstmt.setString(1, docid);
                pstmt.setString(2, docname);
                pstmt.setString(3, doctype);
                pstmt.setString(4, user);
                pstmt.setString(5, user);
                pstmt.setInt(6, revision);
                pstmt.setTimestamp(7, new Timestamp(createDate.getTime()));
                pstmt.setTimestamp(8, new Timestamp(updateDate.getTime()));

            } else if (action != null && action.equals("UPDATE")) {

                sql.append("update xml_documents set docname = ?,");
                sql.append("user_updated = ?, ");
                sql.append("rev = ?, ");
                sql.append("date_updated = ?");
                sql.append(" where docid = ? ");
                // set the values
                pstmt = dbconn.prepareStatement(sql.toString());
                pstmt.setString(1, docname);
                pstmt.setString(2, user);
                pstmt.setInt(3, revision);
                pstmt.setTimestamp(4, new Timestamp(updateDate.getTime()));
                pstmt.setString(5, docid);
            }
            logMetacat.debug(
                "DocumentImpl.modifyRecordsInGivenTable - executing SQL: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();

        } catch (Exception e) {
            logMetacat.debug("Caught a general exception: " + e.getMessage());
            throw e;
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    /*
     * This method will determine if we need to insert or update xml_document base
     * on given docid, rev and rev in xml_documents table
     */
    private static String checkRevInXMLDocuments(String docid, int userSpecifyRev)
        throws Exception {
        String action = null;
        logMetacat.debug("The docid without rev is " + docid);
        logMetacat.debug("The user specifyRev: " + userSpecifyRev);
        // Revision for this docid in current database
        int revInDataBase = DBUtil.getLatestRevisionInDocumentTable(docid);
        logMetacat.debug("The rev in data base: " + revInDataBase);
        // String to store the revision
//        String rev = null;

        //revIndataBase=-1, there is no record in xml_documents table
        //the document is a new one for local server, inert it into table
        //user specified rev should be great than 0
        if (revInDataBase == -1 && userSpecifyRev >= 0) {
            // rev equals user specified
//            rev = (new Integer(userSpecifyRev)).toString();
            // action should be INSERT
            action = "INSERT";
        }
        //rev is greater the last revsion number and revInDataBase isn't -1
        // it is a updated file
        else if (userSpecifyRev > revInDataBase && revInDataBase >= 0) {
            // rev equals user specified
//            rev = (new Integer(userSpecifyRev)).toString();
            // action should be update
            action = "UPDATE";
        }
        // local server has newer version, then notify the remote server
        else if (userSpecifyRev < revInDataBase && revInDataBase > 0) {
            throw new Exception(
                "Local server: " + SystemUtil.getServerURL() + " has newer revision of doc: "
                    + docid + "." + revInDataBase
                    + ". Please notify the remote server's administrator.");
        }
        //other situation
        else {

            throw new Exception(
                "The docid" + docid + "'s revision number couldn't be " + userSpecifyRev);
        }
        return action;
    }


    /**
     * get the document name
     */
    public String getDocname() {
        return docname;
    }

    /**
     * get the document type (which is the PublicID)
     */
    public String getDoctype() {
        return doctype;
    }

    /**
     * get the system identifier
     */
    public String getSystemID() {
        return system_id;
    }

    /**
     * get the root node identifier
     */
    public long getRootNodeID() {
        return rootnodeid;
    }

    /**
     * get the creation date
     */
    public Date getCreateDate() {
        return createdate;
    }

    /**
     * get the update date
     */
    public Date getUpdateDate() {
        return updatedate;
    }

    /**
     * Get the document identifier (docid)
     */
    public String getDocID() {
        return docid;
    }

    public String getUserowner() {
        return userowner;
    }

    public String getUserupdated() {
        return userupdated;
    }

    public int getRev() {
        return rev;
    }

    public String getValidateType() {
        return validateType;
    }

    /**
     * Print a string representation of the XML document NOTE: this detects the character encoding,
     * or uses the XML default
     */
    public String toString(String user, String[] groups, boolean withInlinedata) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            this.toXml(out, user, groups, withInlinedata);
        } catch (McdbException mcdbe) {
            return null;
        }
        String encoding = null;
        String document = null;
        try {
            XmlStreamReader xsr = new XmlStreamReader(new ByteArrayInputStream(out.toByteArray()));
            encoding = xsr.getEncoding();
            document = out.toString(encoding);
        } catch (Exception e) {
            document = out.toString();
        }
        return document;
    }

    /**
     * Print a string representation of the XML document NOTE: this detects the character encoding,
     * or uses the XML default
     */
    public String toString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String userName = null;
        String[] groupNames = null;
        boolean withInlineData = true;
        try {
            this.toXml(out, userName, groupNames, withInlineData);
        } catch (McdbException mcdbe) {
            logMetacat.warn("Could not convert documentImpl to xml: " + mcdbe.getMessage());
            return null;
        }
        String encoding = null;
        String document = null;
        try {
            XmlStreamReader xsr = new XmlStreamReader(new ByteArrayInputStream(out.toByteArray()));
            encoding = xsr.getEncoding();
            document = out.toString(encoding);
        } catch (Exception e) {
            document = out.toString();
        }
        return document;
    }

    public byte[] getBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String userName = null;
        String[] groupNames = null;
        boolean withInlineData = true;
        try {
            this.toXml(out, userName, groupNames, withInlineData);
        } catch (McdbException mcdbe) {
            logMetacat.warn("Could not convert documentImpl to xml: " + mcdbe.getMessage());
            return null;
        }
        return out.toByteArray();
    }


    /**
     * Print a text representation of the XML document to a Writer
     *
     * @param pw the Writer to which we print the document Now we decide no matter
     *           withinInlineData's value, the document will
     */
    public InputStream toXml(OutputStream out, String user, String[] groups, boolean withInLineData)
        throws McdbException {
        String documentDir = null;
        String documentPath = null;
        FileOutputStream fos = null;
        try {
            String separator = PropertyService.getProperty("document.accNumSeparator");
            documentDir = PropertyService.getProperty("application.documentfilepath");
            documentPath = documentDir + FileUtil.getFS() + docid + separator + rev;

            if (FileUtil.getFileStatus(documentPath) == FileUtil.DOES_NOT_EXIST
                || FileUtil.getFileSize(documentPath) == 0) {
                throw new McdbException("Could not read file: " + documentPath
                                            + " : since it doesn't exist or the sieze is 0.");
            }
        } catch (PropertyNotFoundException pnfe) {
            throw new McdbException(
                "Could not write file: " + documentPath + " : " + pnfe.getMessage());
        } finally {
            IOUtils.closeQuietly(fos);
        }

        if (FileUtil.getFileSize(documentPath) == 0) {
            throw new McdbException(
                "Attempting to read a zero length document from disk: " + documentPath);
        }

        return readFromFileSystem(out, user, groups, documentPath);
    }


    /**
     * Read the XML document from the file system and write to a Writer. Strip out any inline data
     * that the user does not have permission to read.
     *
     * @param pw           the Writer to which we print the document
     * @param user         the user we will use to verify inline data access
     * @param groups       the groups we will use to verify inline data access
     * @param documentPath the location of the document on disk
     */
    public InputStream readFromFileSystem(
        OutputStream out, String user, String[] groups, String documentPath) throws McdbException {

        String xmlFileContents = null;
        String encoding = null;

        try {

            // get a list of inline data sections that are not readable
            // by this user
            String fullDocid = docid + "." + rev;
            Hashtable<String, String> unReadableInlineDataList =
                PermissionController.getUnReadableInlineDataIdList(fullDocid, user, groups);

            // If this is for each unreadable section, strip the inline data
            // from the doc
            if (unReadableInlineDataList.size() > 0 && doctype != null) {

                // detect and use correct encoding
                xmlFileContents = FileUtil.readFileToString(documentPath);
                // guess the encoding from default bytes
                XmlStreamReader xsr =
                    new XmlStreamReader(new ByteArrayInputStream(xmlFileContents.getBytes()));
                encoding = xsr.getEncoding();
                xsr.close();
                // reread the contents using the correct encoding
                if (encoding != null) {
                    xmlFileContents = FileUtil.readFileToString(documentPath, encoding);
                }

                Set<String> inlineKeySet = unReadableInlineDataList.keySet();
                boolean pre210Doc =
                    doctype.equals(EML2_0_0NAMESPACE) || doctype.equals(EML2_0_1NAMESPACE);

                for (String inlineKey : inlineKeySet) {
                    String inlineValue = unReadableInlineDataList.get(inlineKey);
                    if (inlineValue.startsWith(docid)) {
                        // There are two different methods of stripping inline data depending
                        // on whether this is a 2.0.1 or earlier doc or 2.1.0 and later.  This
                        // is because of eml schema changes for inline access.
                        if (pre210Doc) {
                            xmlFileContents = stripInline20XData(xmlFileContents, inlineKey);
                        } else {
                            xmlFileContents = stripInline21XData(xmlFileContents, inlineKey);
                        }
                    }
                }
            }

            // will get input either from string content or file on disk
            InputStream is = null;

            // get the input stream
            if (xmlFileContents != null) {
                is = IOUtils.toInputStream(xmlFileContents, encoding);
            } else {
                is = new FileInputStream(documentPath);
            }

            // send it to out
            if (out != null) {
                IOUtils.copyLarge(is, out);
            }
            // return the stream
            return is;

        } catch (UtilException e) {
            throw new McdbException(e.getMessage());
        } catch (IOException e) {
            throw new McdbException(e.getMessage());
        }

    }

    /**
     * Write an XML document to the file system.
     *
     * @param xml       the document we want to write out
     * @param accNumber the document id which is used to name the output file
     */
    private static void writeToFileSystem(
        byte[] xml, String accNumber, Checksum checksum, File objectFile)
        throws McdbException, InvalidSystemMetadata, IOException {

        // write the document to disk
        String documentDir = null;
        String documentPath = null;
        boolean needCalculateChecksum = false;
        String checksumValue = null;
        MessageDigest md = null;

        try {
            documentDir = PropertyService.getProperty("application.documentfilepath");
            documentPath = documentDir + FileUtil.getFS() + accNumber;

            if (xml == null || xml.equals("")) {
                throw new McdbException(
                    "Attempting to write a file with no xml content: " + documentPath);
            }

            if (accNumber == null) {
                throw new McdbException(
                    "Could not write document file.  Accession Number number is null");
            }

            if (FileUtil.getFileStatus(documentPath) >= FileUtil.EXISTS_ONLY) {
                throw new McdbException("The file you are trying to write already exists "
                                            + " in metacat.  Please update your version number.");
            }

            if (accNumber != null && (
                FileUtil.getFileStatus(documentPath) == FileUtil.DOES_NOT_EXIST
                    || FileUtil.getFileSize(documentPath) == 0)) {
                if (objectFile != null && objectFile.exists()) {
                    logMetacat.info(
                        "DocumentImpl.writeToFileSystem - the object file already exists at the "
                            + "temp location and the checksum was checked. Metacat only needs to move"
                            + " it to the permanent position "
                            + documentPath);
                    File permanentFile = new File(documentPath);
                    FileUtils.moveFile(objectFile, permanentFile);
                } else {
                    logMetacat.info(
                        "DocumentImpl.writeToFileSystem - Metacat needs to write the metadata "
                            + "bytes into the file  "
                            + documentPath);
                    if (checksum != null) {
                        needCalculateChecksum = true;
                        checksumValue = checksum.getValue();
                        logMetacat.info(
                            "DocumentImpl.writeToFileSystem - the checksum from the system "
                            + "metadata is "
                                + checksumValue);
                        if (checksumValue == null || checksumValue.trim().equals("")) {
                            logMetacat.error(
                                "DocumentImpl.writeToFileSystem - the checksum value from the "
                                + "system metadata shouldn't be null or blank");
                            throw new InvalidSystemMetadata(
                                "1180",
                                "The checksum value from the system metadata shouldn't be null or"
                                + " blank.");
                        }
                        String algorithm = checksum.getAlgorithm();
                        logMetacat.info(
                            "DocumentImpl.writeToFileSystem - the algorithm to calculate the "
                                + "checksum from the system metadata is "
                                + algorithm);
                        if (algorithm == null || algorithm.trim().equals("")) {
                            logMetacat.error(
                                "DocumentImpl.writeToFileSystem - the algorithm to calculate the "
                                + "checksum from the system metadata shouldn't be null or blank");
                            throw new InvalidSystemMetadata(
                                "1180",
                                "The algorithm to calculate the checksum from the system metadata"
                                + " shouldn't be null or blank.");
                        }
                        try {
                            md = MessageDigest.getInstance(algorithm);
                        } catch (NoSuchAlgorithmException ee) {
                            logMetacat.error(
                                "DocumentImpl.writeToFileSystem - we don't support the algorithm "
                                    + algorithm + " to calculate the checksum.", ee);
                            throw new InvalidSystemMetadata(
                                "1180", "The algorithm " + algorithm
                                + " to calculate the checksum is not supported: "
                                + ee.getMessage());
                        }
                    }

                    OutputStream fos = null;
                    try {
                        if (needCalculateChecksum) {
                            logMetacat.info(
                                "DocumentImpl.writeToFileSystem - we need to compute the checksum"
                                + " since it is from DataONE API");
                            fos = new DigestOutputStream(new FileOutputStream(documentPath), md);
                        } else {
                            logMetacat.info(
                                "DocumentImpl.writeToFileSystem - we don't need to compute the "
                                + "checksum since it is from Metacat API or the checksum has been"
                                + " verified.");
                            fos = new FileOutputStream(documentPath);
                        }

                        IOUtils.write(xml, fos);
                        fos.flush();
                        fos.close();
                        if (needCalculateChecksum) {
                            String localChecksum = DatatypeConverter.printHexBinary(md.digest());
                            logMetacat.info(
                                "DocumentImpl.writeToFileSystem - the check sum calculated from "
                                + "the saved local file is "
                                    + localChecksum);
                            if (localChecksum == null || localChecksum.trim().equals("")
                                || !localChecksum.equalsIgnoreCase(checksumValue)) {
                                logMetacat.error(
                                    "DocumentImpl.writeToFileSystem - the check sum calculated "
                                    + "from the saved local file is "
                                        + localChecksum
                                        + ". But it doesn't match the value from the system "
                                        + "metadata "
                                        + checksumValue);
                                File newFile = new File(documentPath);
                                boolean success = newFile.delete();
                                logMetacat.info("Delete the file " + newFile.getAbsolutePath()
                                                    + " sucessfully? " + success);
                                throw new InvalidSystemMetadata(
                                    "1180", "The checksum calculated from the saved local file is "
                                    + localChecksum
                                    + ". But it doesn't match the value from the system metadata "
                                    + checksumValue + ".");
                            }
                        }
                    } catch (IOException ioe) {
                        throw new McdbException(
                            "Could not write file: " + documentPath + " : " + ioe.getMessage());
                    } finally {
                        IOUtils.closeQuietly(fos);
                    }
                }

            }

        } catch (PropertyNotFoundException pnfe) {
            throw new McdbException(
                "Could not write file: " + documentPath + " : " + pnfe.getMessage());
        }
    }

    /**
     * Deletes a doc or data file from the filesystem using the accession number.
     *
     * @param accNumber
     * @param isXml
     * @throws McdbException
     */
    public static void deleteFromFileSystem(String accNumber, boolean isXml) throws McdbException {

        // must have an id
        if (accNumber == null) {
            throw new McdbException("Could not delete file.  Accession Number number is null");
        }

        // remove the document from disk
        String documentPath = null;

        // get the correct location on disk
        documentPath = getFilePath(accNumber, isXml);
        // delete it if it exists
        if (accNumber != null && FileUtil.getFileStatus(documentPath) != FileUtil.DOES_NOT_EXIST) {
            try {
                FileUtil.deleteFile(documentPath);
            } catch (IOException ioe) {
                throw new McdbException(
                    "Could not delete file: " + documentPath + " : " + ioe.getMessage());
            }
        }

    }

    private static String getFilePath(String accNumber, boolean isXml) throws McdbException {
        if (accNumber == null) {
            throw new McdbException(
                "Could not get the file path since the Accession Number number is null");
        }
        String documentPath = null;
        try {
            String documentDir = null;

            // get the correct location on disk
            if (isXml) {
                documentDir = PropertyService.getProperty("application.documentfilepath");
            } else {
                documentDir = PropertyService.getProperty("application.datafilepath");
            }
            documentPath = documentDir + FileUtil.getFS() + accNumber;
            return documentPath;

        } catch (PropertyNotFoundException pnfe) {
            throw new McdbException(
                pnfe.getClass().getName() + ": Could not delete file because: " + documentPath
                    + " : " + pnfe.getMessage());
        }
    }

    /**
     * Strip out an inline data section from a 2.0.X version document. This assumes that the inline
     * element is within a distribution element and the id for the distribution is the same as the
     * subtreeid in the xml_access table.
     *
     * @param xmlFileContents the contents of the file
     * @param inLineKey       the unique key for this inline element
     */
    private String stripInline20XData(String xmlFileContents, String inLineId)
        throws McdbException {
        String changedString = xmlFileContents;

        Pattern distStartPattern = Pattern.compile("<distribution", Pattern.CASE_INSENSITIVE);
        Pattern distEndPattern = Pattern.compile("</distribution>", Pattern.CASE_INSENSITIVE);
        Pattern idPattern = Pattern.compile("id=\"" + inLineId);
        Pattern inlinePattern = Pattern.compile("<inline>.*</inline>");

        Matcher distStartMatcher = distStartPattern.matcher(xmlFileContents);
        Matcher distEndMatcher = distEndPattern.matcher(xmlFileContents);
        Matcher idMatcher = idPattern.matcher(xmlFileContents);
        Matcher inlineMatcher = inlinePattern.matcher(xmlFileContents);

        // loop through the document looking for distribution elements
        while (distStartMatcher.find()) {
            // find the index of the corresponding end element
            int distStart = distStartMatcher.end();
            if (!distEndMatcher.find(distStart)) {
                throw new McdbException("Could not find end tag for distribution");
            }
            int distEnd = distEndMatcher.start();

            // look for our inline id within the range of this distribution.
            idMatcher.region(distStart, distEnd);
            if (idMatcher.find()) {
                // if this distribution has the desired id, replace any
                // <inline>.*</inline> in this distribution with <inline></inline>
                inlineMatcher.region(distStart, distEnd);
                if (inlineMatcher.find()) {
                    changedString = inlineMatcher.replaceAll("<inline></inline>");
                } else {
                    logMetacat.warn(
                        "Could not find an inline element for distribution: " + inLineId);
                }
            }

        }

        return changedString;
    }

    /**
     * Strip out an inline data section from a 2.1.X version document. This assumes that the inline
     * element is within a distribution element and the subtreeid in the xml_access table is an
     * integer that represents the nth distribution element in the document.
     *
     * @param xmlFileContents the contents of the file
     * @param inLineKey       the unique key for this inline element
     */
    private String stripInline21XData(String xmlFileContents, String inLineId)
        throws McdbException {
        int distributionIndex = Integer.valueOf(inLineId);
        String changedString = xmlFileContents;
        Pattern distStartPattern = Pattern.compile("<distribution", Pattern.CASE_INSENSITIVE);
        Pattern distEndPattern = Pattern.compile("</distribution>", Pattern.CASE_INSENSITIVE);
        Pattern inlinePattern = Pattern.compile("<inline>.*</inline>");
        Matcher matcher = distStartPattern.matcher(xmlFileContents);

        // iterate through distributions until we find the nth match.  The nth match
        // corresponds to the inLineKey that was passed in.  Use the end of that match
        // to set the start range we will search for the inline element.
        for (int i = 0; i < distributionIndex; i++) {
            if (!matcher.find()) {
                throw new McdbException("Could not find distribution number " + (i + 1));
            }
        }
        int distStart = matcher.end();

        // find the end tag for the distribution.  Use the beginning of that match to set
        // the end range we will search for the inline element
        matcher.usePattern(distEndPattern);
        if (!matcher.find(distStart)) {
            throw new McdbException("Could not find end tag for distribution");
        }
        int distEnd = matcher.start();

        // set the inline search range
        matcher.region(distStart, distEnd);

        // match any inline elements and replace with an empty inline element
        matcher.usePattern(inlinePattern);
        if (matcher.find()) {
            changedString = matcher.replaceAll("<inline></inline>");
        } else {
            logMetacat.warn("Could not find an inline element for distribution: " + inLineId);
        }

        return changedString;
    }

    private boolean isRevisionOnly(String docid, int revision) throws Exception {
        //System.out.println("inRevisionOnly given "+ docid + "."+ revision);
        DBConnection dbconn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        String newid = docid;

        try {
            dbconn = DBConnectionPool.getDBConnection("DocumentImpl.isRevisionOnly");
            serialNumber = dbconn.getCheckOutSerialNumber();
            pstmt =
                dbconn.prepareStatement("select rev from xml_documents " + "where docid like ?");
            pstmt.setString(1, newid);
            logMetacat.debug("DocumentImpl.isRevisionOnly - executing SQL: " + pstmt.toString());
            pstmt.execute();
            ResultSet rs = pstmt.getResultSet();
            boolean tablehasrows = rs.next();
            //if (rev.equals("newest") || rev.equals("all")) { return false; }

            if (tablehasrows) {
                int r = rs.getInt(1);
                //System.out.println("the rev in xml_documents table is "+r);
                pstmt.close();
                if (revision == r) { //the current revision
                    // in in xml_documents
                    //System.out.println("returning false");
                    return false;
                } else if (revision < r) { //the current
                    // revision is in
                    // xml_revisions.
                    return true;
                } else if (revision > r) { //error, rev
                    // cannot be
                    // greater than r
                    throw new Exception("requested revision cannot be greater than "
                                            + "the latest revision number.");
                }
            } else {
                // if we couldn't find it in xml_documents we
                // need to find it in xml_revision table
                Vector<Integer> revList = DBUtil.getRevListFromRevisionTable(docid);

                if (revList != null && !revList.isEmpty()) {
                    return true;
                }
            }
            // Get miss docid and rev, throw to McdDocNotFoundException
            String missDocId = docid;
            String missRevision = Integer.toString(revision);
            throw new McdbDocNotFoundException(
                "the requested docid '" + docid.toString() + "' does not exist", missDocId,
                missRevision);
        }//try
        finally {
            pstmt.close();
            DBConnectionPool.returnDBConnection(dbconn, serialNumber);
        }//finally
    }

    /**
     * Look up the document type information from the database
     *
     * @param docid  the id of the document to look up
     * @param revsion  the revision of the document
     */
    private void getDocumentInfo(String docid, int revision) throws McdbException, Exception {
        DBConnection dbconn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        String table = "xml_documents";

        try {
            if (isRevisionOnly(docid, revision)) { //pull the document from xml_revisions
                // instead of from xml_documents;
                table = "xml_revisions";
            }
        }
        // catch a McdbDocNotFoundException throw it
        catch (McdbDocNotFoundException notFound) {
            throw notFound;
        } catch (Exception e) {

            logMetacat.error("DocumentImpl.getDocumentInfo - general error: " + e.getMessage());
            throw e;
        }

        try {
            dbconn = DBConnectionPool.getDBConnection("DocumentImpl.getDocumentInfo");
            serialNumber = dbconn.getCheckOutSerialNumber();
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT docname, doctype, rootnodeid, ");
            sql.append("date_created, date_updated, user_owner, user_updated,");
            sql.append(" rev");
            sql.append(" FROM ").append(table);
            sql.append(" WHERE docid LIKE ? ");
            sql.append(" and rev = ? ");

            pstmt = dbconn.prepareStatement(sql.toString());
            pstmt.setString(1, docid);
            pstmt.setInt(2, revision);

            logMetacat.debug("DocumentImpl.getDocumentInfo - executing SQL: " + pstmt.toString());
            pstmt.execute();
            ResultSet rs = pstmt.getResultSet();
            boolean tableHasRows = rs.next();
            if (tableHasRows) {
                this.docname = rs.getString(1);
                this.doctype = rs.getString(2);
                this.rootnodeid = rs.getLong(3);
                this.createdate = rs.getTimestamp(4);
                this.updatedate = rs.getTimestamp(5);
                this.userowner = rs.getString(6);
                this.userupdated = rs.getString(7);
                this.rev = rs.getInt(8);
            }
            pstmt.close();

            if (this.doctype != null && !XMLSchemaService.getInstance()
                .getNonXMLMetadataFormatList().contains(doctype)) {
                pstmt = dbconn.prepareStatement(
                    "SELECT system_id, entry_type " + "FROM xml_catalog " + "WHERE public_id = ?");
                //should increase usage count again
                dbconn.increaseUsageCount(1);
                // Bind the values to the query
                pstmt.setString(1, doctype);

                logMetacat.debug(
                    "DocumentImpl.getDocumentInfo - executing SQL: " + pstmt.toString());
                pstmt.execute();
                rs = pstmt.getResultSet();
                tableHasRows = rs.next();
                if (tableHasRows) {
                    this.system_id = rs.getString(1);
                    // system id may not have server url on front.  Add it if not.
                    if (!system_id.startsWith("http://")) {
                        system_id = SystemUtil.getContextURL() + system_id;
                    }
                    this.validateType = rs.getString(2);

                }
                pstmt.close();
            }
        } catch (SQLException e) {
            logMetacat.error(
                "DocumentImpl.getDocumentInfo - Error in DocumentImpl.getDocumentInfo: "
                    + e.getMessage());
            throw new McdbException(
                "DocumentImpl.getDocumentInfo - Error accessing database connection: ", e);
        } finally {
            try {
                pstmt.close();
            } catch (SQLException ee) {
                logMetacat.error("DocumentImpl.getDocumentInfo - General error" + ee.getMessage());
            } finally {
                DBConnectionPool.returnDBConnection(dbconn, serialNumber);
            }
        }

        if (this.docname == null) {
            throw new McdbDocNotFoundException(
                "Document not found: " + docid, docid, Integer.toString(revision));
        }
    }

    /**
     * Creates the SQL code and inserts new document into DB connection
     * @param action  it can be insert or update
     * @param user  the owner of the document
     * @param catalogid  the catalog identifier to which the document belongs
     * @param createDate  the created date of the document
     * @param updateDate  the updated date of the document
     * @throws SQLException
     * @throws Exception
     */
    private void writeDocumentToDB(
        String action, String user, String catalogid, Date createDate,
        Date updateDate) throws SQLException, Exception {
        Date today = Calendar.getInstance().getTime();
        if (createDate == null) {
            createDate = today;
        }
        if (updateDate == null) {
            updateDate = today;
        }

        try {
            PreparedStatement pstmt = null;

            if (action.equals("INSERT")) {
                String sql = null;
                if (catalogid != null) {
                    sql = "INSERT INTO xml_documents "
                        + "(docid, rootnodeid, docname, doctype, user_owner, "
                        + "user_updated, date_created, date_updated, "
                        + "rev, catalog_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                } else {
                    sql = "INSERT INTO xml_documents "
                        + "(docid, rootnodeid, docname, doctype, user_owner, "
                        + "user_updated, date_created, date_updated, "
                        + "rev) VALUES (?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?)";
                }
                pstmt = connection.prepareStatement(sql);
                // Increase dbconnection usage count
                connection.increaseUsageCount(1);

                //note that the server_location is set to 1.
                //this means that "localhost" in the xml_replication table must
                //always be the first entry!!!!!

                // Bind the values to the query
                pstmt.setString(1, this.docid);
                pstmt.setLong(2, rootnodeid);
                pstmt.setString(3, docname);
                pstmt.setString(4, doctype);
                pstmt.setString(5, user);
                pstmt.setString(6, user);
                // dates
                pstmt.setTimestamp(7, new Timestamp(createDate.getTime()));
                pstmt.setTimestamp(8, new Timestamp(updateDate.getTime()));
                pstmt.setInt(9, rev);

                if (catalogid != null) {
                    pstmt.setInt(10, Integer.parseInt(catalogid));
                }

            } else if (action.equals("UPDATE")) {
                int thisrev = DBUtil.getLatestRevisionInDocumentTable(docid);
                logMetacat.debug("DocumentImpl.writeDocumentToDB - this revision is: " + thisrev);
                // Save the old document publicaccessentry in a backup table
                archiveDocToRevision(connection, docid, user);
                //if the updated vesion is not greater than current one,
                //throw it into a exception
                if (rev <= thisrev) {
                    throw new Exception(
                        "Next revision number couldn't be less" + " than or equal " + thisrev);
                } else {
                    //set the user specified revision
                    thisrev = rev;
                }
                logMetacat.debug("DocumentImpl.writeDocumentToDB - final revision is: " + thisrev);

                // Update the new document to reflect the new node tree
                String updateSql = null;
                if (catalogid != null) {
                    updateSql =
                        "UPDATE xml_documents " + "SET rootnodeid = ?, docname = ?, doctype = ?, "
                            + "user_updated = ?, date_updated = ?, "
                            + "rev = ?, "
                            + "catalog_id = ? " + "WHERE docid = ?";
                } else {
                    updateSql =
                        "UPDATE xml_documents " + "SET rootnodeid = ?, docname = ?, doctype = ?, "
                            + "user_updated = ?, date_updated = ?, "
                            + "rev = ? "
                            + "WHERE docid = ?";
                }
                // Increase dbconnection usage count
                pstmt = connection.prepareStatement(updateSql);
                connection.increaseUsageCount(1);
                // Bind the values to the query
                pstmt.setLong(1, rootnodeid);
                pstmt.setString(2, docname);
                pstmt.setString(3, doctype);
                pstmt.setString(4, user);
                pstmt.setTimestamp(5, new Timestamp(updateDate.getTime()));
                pstmt.setInt(6, thisrev);

                if (catalogid != null) {
                    pstmt.setInt(7, Integer.parseInt(catalogid));
                    pstmt.setString(8, this.docid);
                } else {
                    pstmt.setString(7, this.docid);
                }

            } else {
                logMetacat.error(
                    "DocumentImpl.writeDocumentToDB - Action not supported: " + action);
            }

            // Do the insertion
            logMetacat.debug("DocumentImpl.writeDocumentToDB - executing SQL: " + pstmt.toString());
            pstmt.execute();

            pstmt.close();
        } catch (SQLException sqle) {
            logMetacat.error("DocumentImpl.writeDocumentToDB - SQL error: " + sqle.getMessage());
            throw sqle;
        } catch (Exception e) {
            logMetacat.error("DocumentImpl.writeDocumentToDB - General error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Parse and write an XML file to the database
     * @param conn  the JDBC connection to the database
     * @param dtd  the dtd to be uploaded on server's file system
     * @param action  the action to be performed (INSERT or UPDATE)
     * @param accnum  the docid + rev# to use on INSERT or UPDATE
     * @param user  the user that owns the document
     * @param groups  the groups to which user belongs
     * @param ruleBase  the type (dtd, schema or et al) of validation
     * @param needValidation  flag indicating if it needs a validate
     * @param encoding  the encoding of the xml document
     * @param xmlBytes  the content of the xml document
     * @param schemaLocation  the schema location string
     * @param checksum  the checksum of the xml document
     * @param objectFile  the temporary file of the object 
     * @return accnum
     * @throws Exception
     */
    public static String write(DBConnection conn, Reader dtd, String action,
            String accnum, String user, String[] groups, String ruleBase,
        boolean needValidation, String encoding, byte[] xmlBytes, String schemaLocation,
        Checksum checksum, File objectFile) throws Exception {
        // NEW - WHEN CLIENT ALWAYS PROVIDE ACCESSION NUMBER INCLUDING REV IN IT

        // Get the xml as a string so we can write to file later
        InputStreamReader xmlReader = new InputStreamReader(new ByteArrayInputStream(xmlBytes));
        AccessionNumber ac = new AccessionNumber(accnum, action);
        String docid = ac.getDocid();
        String rev = ac.getRev();

        if (action.equals("UPDATE")) {
            // check for 'write' permission for 'user' to update this document
            // use the previous revision to check the permissions
            String docIdWithoutRev = DocumentUtil.getSmartDocId(accnum);
            int latestRev = DBUtil.getLatestRevisionInDocumentTable(docIdWithoutRev);
            String latestDocId =
                docIdWithoutRev + PropertyService.getProperty("document.accNumSeparator")
                    + latestRev;
            if (!hasWritePermission(user, groups, latestDocId) && !AuthUtil.isAdministrator(
                user, groups)) {
                throw new Exception(
                    "User " + user + " does not have permission to update XML Document #"
                        + latestDocId);
            }
        }
        XMLReader parser = null;
        try {
            Vector<String> guidsToSync = new Vector<String>();
            Vector<XMLSchema> schemaList = XMLSchemaService.getInstance().
                                                            findSchemasInXML(xmlReader);
            // null and null are createtime and updatetime
            // null will create current time
            //false means it is not a revision doc
            parser = initializeParser(schemaList, dtd, ruleBase, needValidation, schemaLocation);
            xmlReader = new InputStreamReader(new ByteArrayInputStream(xmlBytes));
            conn.setAutoCommit(false);
            parser.parse(new InputSource(xmlReader));
            //write the file to disk
            writeToFileSystem(xmlBytes, accnum, checksum, objectFile);

            conn.commit();
            conn.setAutoCommit(true);

            if (guidsToSync.size() > 0) {
                try {
                    SyncAccessPolicy syncAP = new SyncAccessPolicy();
                    syncAP.sync(guidsToSync);
                } catch (Exception e) {
                    logMetacat.error(
                        "Error syncing pids with CN: " + " Exception " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logMetacat.error("DocumentImpl.write - Problem with parsing: " + e.getMessage());
            conn.rollback();
            conn.setAutoCommit(true);
            throw e;
        }
        return (accnum);
    }



    /**
     * Archive an object from the xml_documents table to the xml_revision table (including other
     * changes as well). Or delete an object totally from the db. The parameter "removeAll" decides
     * which action will be taken.
     *
     * @param accnum       the local id (including the rev) will be applied.
     * @param user         the subject who does the action.
     * @param groups       the groups which the user belongs to.
     * @param notifyServer the server will be notified in the replication. It can be null.
     * @param removeAll    it will be the delete action if this is true; otherwise it will be the
     *                     archive action
     * @throws SQLException
     * @throws InsufficientKarmaException
     * @throws McdbDocNotFoundException
     * @throws Exception
     */
    public static void delete(
        String accnum, String user, String[] groups, String notifyServer, boolean removeAll)
        throws SQLException, InsufficientKarmaException, McdbDocNotFoundException, Exception {
        //default, we only match the docid part on archive action
        boolean ignoreRev = true;
        delete(accnum, ignoreRev, user, groups, notifyServer, removeAll);
    }

    /**
     * Archive an object from the xml_documents table to the xml_revision table (including other
     * changes as well). Or delete an object totally from the db. The parameter "removeAll" decides
     * which action will be taken.
     *
     * @param accnum       the local id (including the rev) will be applied.
     * @param ignoreRev    if the archive action should only match docid and ignore the rev
     * @param user         the subject who does the action.
     * @param groups       the groups which the user belongs to.
     * @param notifyServer the server will be notified in the replication. It can be null.
     * @param removeAll    it will be the delete action if this is true; otherwise it will be the
     *                     archive action.
     * @throws SQLException
     * @throws InsufficientKarmaException
     * @throws McdbDocNotFoundException
     * @throws Exception
     */
    public static void delete(
        String accnum, boolean ignoreRev, String user, String[] groups, String notifyServer,
        boolean removeAll)
        throws SQLException, InsufficientKarmaException, McdbDocNotFoundException, Exception {

        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        boolean isXML = true;
        boolean inRevisionTable = false;
        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("DocumentImpl.delete");
            serialNumber = conn.getCheckOutSerialNumber();

            // CLIENT SHOULD ALWAYS PROVIDE ACCESSION NUMBER INCLUDING REV
            //AccessionNumber ac = new AccessionNumber(accnum, "DELETE");
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            int rev = DocumentUtil.getRevisionFromAccessionNumber(accnum);
            ;

            // Check if the document exists.
            if (!removeAll) {
                //this only archives a document from xml_documents to xml_revisions
                logMetacat.info("DocumentImp.delete - archive the document " + accnum);
                pstmt =
                    conn.prepareStatement("SELECT rev, docid FROM xml_documents WHERE docid = ?");
                pstmt.setString(1, docid);
                logMetacat.debug("DocumentImpl.delete - executing SQL: " + pstmt.toString());
                pstmt.execute();
                ResultSet rs = pstmt.getResultSet();
                if (!rs.next()) {
                    rs.close();
                    pstmt.close();
                    conn.increaseUsageCount(1);
                    throw new McdbDocNotFoundException(
                        "Docid " + accnum + " does not exist. Please check that you have also "
                            + "specified the revision number of the document.");
                } else {
                    //Get the rev from the xml_table. If the value is greater than the one user
                    // specified, we will use this one.
                    //In ReplicationHandler.handleDeleteSingleDocument method, the code use "1"
                    // as the revision number not matther what is the actual value
                    int revFromTable = rs.getInt(1);
                    if (!ignoreRev && revFromTable != rev) {
                        pstmt.close();
                        conn.increaseUsageCount(1);
                        throw new McdbDocNotFoundException(
                            "Docid " + accnum + " does not exist. Please check that you have also "
                                + "specified the revision number of the document.");
                    }
                    if (revFromTable > rev) {
                        logMetacat.info(
                            "DocumentImpl.delete - in the archive the user specified rev - " + rev
                                + "is less than the version in xml_document table - " + revFromTable
                                + ". We will use the one from table.");
                        rev = revFromTable;
                    }
                    rs.close();
                    pstmt.close();
                    conn.increaseUsageCount(1);

                }
            } else {
                logMetacat.info("DocumentImp.delete - complete delete the document " + accnum);
                pstmt = conn.prepareStatement(
                    "SELECT * FROM xml_documents WHERE docid = ? and rev = ?");
                pstmt.setString(1, docid);
                pstmt.setInt(2, rev);
                logMetacat.debug("DocumentImpl.delete - executing SQL: " + pstmt.toString());
                pstmt.execute();
                ResultSet rs = pstmt.getResultSet();
                if (!rs.next()) {
                    //look at the xml_revisions table
                    logMetacat.debug("DocumentImpl.delete - look at the docid " + accnum
                                         + " in the xml_revision table");
                    pstmt = conn.prepareStatement(
                        "SELECT * FROM xml_revisions WHERE docid = ? AND rev = ?");
                    pstmt.setString(1, docid);
                    pstmt.setInt(2, rev);
                    logMetacat.debug("DocumentImpl.delete - executing SQL: " + pstmt.toString());
                    pstmt.execute();
                    rs = pstmt.getResultSet();
                    if (!rs.next()) {
                        rs.close();
                        pstmt.close();
                        conn.increaseUsageCount(1);
                        throw new McdbDocNotFoundException("Docid " + accnum
                                                               + " does not exist. Please check "
                                                               + "and try to delete it again.");
                    } else {
                        rs.close();
                        pstmt.close();
                        conn.increaseUsageCount(1);
                        inRevisionTable = true;
                    }
                } else {
                    rs.close();
                    pstmt.close();
                    conn.increaseUsageCount(1);
                }
            }

            // get the type of deleting docid, this will be used in forcereplication
            String type = null;
            if (!inRevisionTable) {
                type = getDocTypeFromDB(conn, "xml_documents", docid);
            } else {
                type = getDocTypeFromDB(conn, "xml_revisions", docid);
            }
            logMetacat.info("DocumentImpl.delete - the deleting doc type is " + type + "...");
            if (type != null && type.trim().equals("BIN")) {
                isXML = false;
            }

            logMetacat.info("DocumentImpl.delete - Start deleting doc " + docid + "...");
            double start = System.currentTimeMillis() / 1000;
            // check for 'write' permission for 'user' to delete this document
            if (!hasAllPermission(user, groups, accnum)) {
                if (!AuthUtil.isAdministrator(user, groups)) {
                    throw new InsufficientKarmaException(
                        "User " + user + " does not have permission to delete XML Document #"
                            + accnum);
                }
            }

            conn.setAutoCommit(false);
            if (!inRevisionTable) {
                // Copy the record to the xml_revisions table if not a full delete
                if (!removeAll) {
                    archiveDocToRevision(conn, docid, user);
                    logMetacat.info("DocumentImpl.delete - calling archiveDocAndNodesRevision");
                }
                double afterArchiveDocAndNode = System.currentTimeMillis() / 1000;
                logMetacat.info(
                    "DocumentImpl.delete - The time for archiveDocAndNodesRevision is " + (
                        afterArchiveDocAndNode - start));
                // Delete it from xml_documents table
                logMetacat.info("DocumentImpl.delete - deleting from xml_documents");
                pstmt = conn.prepareStatement("DELETE FROM xml_documents WHERE docid = ?");
                pstmt.setString(1, docid);
                logMetacat.debug("DocumentImpl.delete - running sql: " + pstmt.toString());
                pstmt.execute();
                pstmt.close();
                //Usaga count increase 1
                conn.increaseUsageCount(1);
                double afterDeleteDoc = System.currentTimeMillis() / 1000;
            } else {
                logMetacat.info("DocumentImpl.delete - deleting from xml_revisions");
                pstmt =
                    conn.prepareStatement("DELETE FROM xml_revisions WHERE docid = ? AND rev = ?");
                pstmt.setString(1, docid);
                pstmt.setInt(2, rev);
                logMetacat.debug("DocumentImpl.delete - running sql: " + pstmt.toString());
                pstmt.execute();
                pstmt.close();
                conn.increaseUsageCount(1);
            }


            // set as archived in the systemMetadata  if it is not a complete removal
            String pid = IdentifierManager.getInstance().getGUID(docid, rev);
            Identifier guid = new Identifier();
            guid.setValue(pid);

            //update systemmetadata table and solr index
            SystemMetadata sysMeta = SystemMetadataManager.getInstance().get(guid);
            if (sysMeta != null) {
                //sysMeta.setSerialVersion(sysMeta.getSerialVersion().add(BigInteger.ONE));
                sysMeta.setArchived(true);
                //sysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
                if (!removeAll) {
                    SystemMetadataManager.getInstance().store(sysMeta);
                    MetacatSolrIndex.getInstance().submit(guid, sysMeta, false);
                } else {
                    try {
                        SystemMetadataManager.getInstance().delete(guid);
                        MetacatSolrIndex.getInstance().submitDeleteTask(guid, sysMeta);
                    } catch (RuntimeException ee) {
                        logMetacat.warn(
                            "we catch the run time exception in deleting system metadata "
                                + ee.getMessage());
                        throw new Exception("DocumentImpl.delete -" + ee.getMessage());
                    }
                }

            }

            // only commit if all of this was successful
            conn.commit();
            conn.setAutoCommit(true);

            // remove the file if called for
            if (removeAll) {
                deleteFromFileSystem(accnum, isXML);
            }

            double end = System.currentTimeMillis() / 1000;
            logMetacat.info("DocumentImpl.delete - total delete time is:  " + (end - start));

        } catch (Exception e) {
            // rollback the delete if there was an error
            conn.rollback();
            logMetacat.error("DocumentImpl.delete -  Error: " + e.getMessage());
            throw e;
        } finally {
            try {
                // close preparedStatement
                if (pstmt != null) {
                    pstmt.close();
                }
            } finally {
                //check in DBonnection
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }

    }

    /**
     * Get the doc type for a given docid. If we don't find, null will be returned
     * @param conn  the db connection which will be used to connect to database
     * @param tableName  the table name which will be looked up
     * @param docidWithoutRev  the given docid
     * @return the doc type
     * @throws SQLException
     */
    private static String getDocTypeFromDB(
        DBConnection conn, String tableName, String docidWithoutRev) throws SQLException {
        String type = null;
        String sql = "SELECT DOCTYPE FROM " + tableName + " WHERE docid LIKE ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, docidWithoutRev);
            try (ResultSet result = stmt.executeQuery()) {
                boolean hasResult = result.next();
                if (hasResult) {
                    type = result.getString(1);
                }
            }
        }
        logMetacat.debug(
            "DocumentImpl.getDocTypeFromDB - The type of docid " + docidWithoutRev + " is " + type);
        return type;
    }

    /**
     * Check for "WRITE" permission on @docid for @user and/or @groups from DB connection
     */
    public static boolean hasWritePermission(String user, String[] groups, String docid)
        throws SQLException, Exception {
        // Check for WRITE permission on @docid for @user and/or @groups
        PermissionController controller = new PermissionController(docid);
        return controller.hasPermission(user, groups, AccessControlInterface.WRITESTRING);
    }


    /**
     * Check for "ALL" or "CHMOD" permission on @docid for @user and/or @groups from DB connection
     */
    public static boolean hasAllPermission(String user, String[] groups, String docid)
        throws SQLException, Exception {
        // Check for either ALL or CHMOD permission on @docid for @user and/or @groups
        PermissionController controller = new PermissionController(docid);
        boolean hasAll = controller.hasPermission(user, groups, AccessControlInterface.ALLSTRING);
        boolean hasChmod =
            controller.hasPermission(user, groups, AccessControlInterface.CHMODSTRING);
        return hasAll || hasChmod;
    }

    /**
     * Set up the parser handlers for writing the document to the database
     * @param schemaList  the list of schema will be used
     * @param dtd  the dtd content
     * @param ruleBase  the validation base - schema or dtd
     * @param needValidation  if the document needs to be validated
     * @param schemaLocation  the string contains the schema location
     * @return the XMLReader object
     * @throws PropertyNotFoundException
     * @throws SAXException
     * @throws ServiceFailure
     */
    public static XMLReader initializeParser( Vector<XMLSchema> schemaList, Reader dtd,
                  String ruleBase, boolean needValidation, String schemaLocation)
                          throws ServiceFailure, PropertyNotFoundException, SAXException {
        XMLReader parser = null;
        // handler
        DBSAXHandler chandler;
        EntityResolver eresolver;
        DTDHandler dtdhandler;
        // Get an instance of the parser
        String parserName = PropertyService.getProperty("xml.saxparser");
        parser = XMLReaderFactory.createXMLReader(parserName);
        //XMLSchemaService.getInstance().populateRegisteredSchemaList();
        //create a DBSAXHandler object which has the revision
        // specification
        chandler = new DBSAXHandler();
        parser.setContentHandler((ContentHandler) chandler);
        parser.setErrorHandler((ErrorHandler) chandler);
        parser.setProperty(DECLARATIONHANDLERPROPERTY, chandler);
        parser.setProperty(LEXICALPROPERTY, chandler);
        if (ruleBase != null && (ruleBase.equals(SCHEMA) || ruleBase.equals(EML200)
            || ruleBase.equals(EML210)) && needValidation) {
            XMLSchemaService xmlss = XMLSchemaService.getInstance();
            logMetacat.info("DocumentImpl.initalizeParser - Using General schema parser");
            // turn on schema validation feature
            parser.setFeature(VALIDATIONFEATURE, true);
            parser.setFeature(NAMESPACEFEATURE, true);
            parser.setFeature(SCHEMAVALIDATIONFEATURE, true);

            boolean allSchemasRegistered = xmlss.areAllSchemasRegistered(schemaList);
            if (xmlss.useFullSchemaValidation() && !allSchemasRegistered && !ruleBase.equals(
                EML210) && !ruleBase.equals(EML200)) {
                parser.setFeature(FULLSCHEMAVALIDATIONFEATURE, true);
            }
            logMetacat.info("DocumentImpl.initalizeParser - Generic external schema location: "
                                + schemaLocation);
            // Set external schemalocation.
            if (schemaLocation != null && !(schemaLocation.trim()).equals("")) {
                parser.setProperty(EXTERNALSCHEMALOCATIONPROPERTY, schemaLocation);
            } else {
                throw new ServiceFailure("0000", "The schema for the document "
                                        + " can't be found in any place. So we can't validate"
                                        + " the xml instance.");
            }
        } else if (ruleBase != null && ruleBase.equals(NONAMESPACESCHEMA) && needValidation) {
            logMetacat.info("DocumentImpl.initalizeParser - Using General schema parser");
            // turn on schema validation feature
            parser.setFeature(VALIDATIONFEATURE, true);
            parser.setFeature(NAMESPACEFEATURE, true);
            parser.setFeature(SCHEMAVALIDATIONFEATURE, true);
            logMetacat.info(
                "DocumentImpl.initalizeParser - Generic external no-namespace schema location: "
                    + schemaLocation);
            // Set external schemalocation.
            if (schemaLocation != null && !(schemaLocation.trim()).equals("")) {
                parser.setProperty(EXTERNALNONAMESPACESCHEMALOCATIONPROPERTY, schemaLocation);
            } else {
                throw new ServiceFailure("0000", "The schema for the document "
                                        + " can't be found in any place. So we can't validate"
                                        + " the xml instance.");
            }
        } else if (ruleBase != null && ruleBase.equals(DTD) && needValidation) {
            logMetacat.info("DocumentImpl.initalizeParser - Using dtd parser");
            // turn on dtd validaton feature
            parser.setFeature(VALIDATIONFEATURE, true);
            eresolver = new DBEntityResolver((DBSAXHandler) chandler, dtd);
            dtdhandler = new DBDTDHandler();
            parser.setEntityResolver((EntityResolver) eresolver);
            parser.setDTDHandler((DTDHandler) dtdhandler);
        } else {
            logMetacat.info("DocumentImpl.initalizeParser - Using other parser");
            // non validation
            parser.setFeature(VALIDATIONFEATURE, false);
            eresolver = new DBEntityResolver((DBSAXHandler) chandler, dtd);
            dtdhandler = new DBDTDHandler();
            parser.setEntityResolver((EntityResolver) eresolver);
            parser.setDTDHandler((DTDHandler) dtdhandler);
        }
        return parser;
    }

    /**
     * This method will move a document record from the xml_documents table
     * to the xml_revisions table
     * @param dbconn  the jdbc connection will be used to execute query
     * @param docid  the docid of the document
     * @param user  the user who request the action
     * @throws Exception
     */
    private static void archiveDocToRevision(
        DBConnection dbconn, String docid, String user) throws Exception {
        String sysdate = DatabaseService.getInstance().getDBAdapter().getDateTimeFunction();
        PreparedStatement pstmt = null;
        try {
            //Move the document information to xml_revisions table...
            double start = System.currentTimeMillis() / 1000;
            pstmt = dbconn.prepareStatement(
                "INSERT INTO xml_revisions " + "(docid, rootnodeid, docname, doctype, "
                    + "user_owner, user_updated, date_created, date_updated, "
                    + "rev, catalog_id) "
                    + "SELECT ?, rootnodeid, docname, doctype, "
                    + "user_owner, ?, date_created, date_updated, "
                    + "rev, catalog_id " + "FROM xml_documents "
                    + "WHERE docid = ?");

            // Increase dbconnection usage count
            dbconn.increaseUsageCount(1);
            // Bind the values to the query and execute it
            pstmt.setString(1, docid);
            pstmt.setString(2, user);
            pstmt.setString(3, docid);
            logMetacat.debug(
                "DocumentImpl.archiveDocToRevision - executing SQL: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();
            double end = System.currentTimeMillis() / 1000;
            logMetacat.debug(
                "DocumentImpl.archiveDocToRevision - moving docs from xml_documents to "
                + "xml_revision takes "
                    + (end - start));

        } catch (SQLException e) {
            logMetacat.error(
                "DocumentImpl.archiveDocToRevision - SQL error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logMetacat.error(
                "DocumentImpl.archiveDocToRevision - General error: " + e.getMessage());
            throw e;
        } finally {
            try {
                pstmt.close();
            } catch (SQLException ee) {
                logMetacat.error(
                    "DocumentImpl.archiveDocToRevision - SQL error when closing prepared "
                    + "statement: "
                        + ee.getMessage());
            }
        }
    }
}
