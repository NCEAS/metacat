package edu.ucsb.nceas.metacat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.XMLSchema;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
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
     * Register a document that resides on the filesystem with the database. (ie, just an entry in
     * xml_documents). Creates a reference to a filesystem document (used for non-xml data files).
     *
     * @param docname    - the name of DTD, i.e. the name immediately following the DOCTYPE keyword
     *                   ( should be the root element name ) or the root element name if no DOCTYPE
     *                   declaration provided (Oracle's and IBM parsers are not aware if it is not
     *                   the root element name)
     * @param doctype    - Public ID of the DTD, i.e. the name immediately following the PUBLIC
     *                   keyword in DOCTYPE declaration or the docname if no Public ID provided or
     *                   null if no DOCTYPE declaration provided
     * @param conn       the connection to database
     * @param accnum     the accession number to use for the INSERT OR UPDATE, which includes a
     *                   revision number for this revision of the document (e.g., knb.1.1)
     * @param user       the user that owns the document
     * @throws SQLException
     * @throws AccessionNumberException
     * @throws MetacatException
     * @throws PropertyNotFoundException
     */
    public static void registerDocument(String docname, String doctype, DBConnection conn,
                                        String accnum, String user)
                                        throws SQLException, AccessionNumberException,
                                                PropertyNotFoundException, MetacatException {
        String action = null;
        String docIdWithoutRev = DocumentUtil.getDocIdFromAccessionNumber(accnum);
        int userSpecifyRev = DocumentUtil.getRevisionFromAccessionNumber(accnum);
        int revInDataBase = DBUtil.getLatestRevisionInDocumentTable(docIdWithoutRev);
        action = checkRevInXMLDocuments(revInDataBase, docIdWithoutRev, userSpecifyRev);
        logMetacat.debug("after check rev, the action is " + action);
        if (action.equalsIgnoreCase("UPDATE")) {
            archiveDocToRevision(conn, docIdWithoutRev, revInDataBase, user);
        }
        String rev = Integer.toString(userSpecifyRev);
        // null and null is createdate and updatedate
        // null will create current time
        modifyRecordDocumentsTable(docIdWithoutRev, doctype, docname, user, rev, conn);
    }

    /**
     * This method will insert or update xml-documents or xml_revision table
     * @param docid  the docid of the document
     * @param doctype  the type of the document
     * @param docname  the name of the document
     * @param user  the owner of the document
     * @param rev  the revision of the document
     * @param dbconn  the JDBC Connection to which all information is written
     * @throws SQLException
     */
    private static void modifyRecordDocumentsTable(String docid, String doctype, String docname,
            String user, String rev, DBConnection dbconn) throws SQLException {
        PreparedStatement pstmt = null;
        int revision = Integer.parseInt(rev);
        Date today = new Date(Calendar.getInstance().getTimeInMillis());
        Date createDate = today;
        Date updateDate = today;
        int catalogId = getCatalogId(doctype);

        try {
            StringBuffer sql = new StringBuffer();
            sql.append("insert into ");
            sql.append("xml_documents");
            sql.append(" (docid, docname, doctype, ");
            sql.append("user_owner, user_updated, rev, date_created, ");
            sql.append("date_updated, rootnodeid, catalog_id) values (");
            sql.append("?, ");
            sql.append("?, ");
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
            if (!doctype.equals(BIN)) {
                pstmt.setLong(9, DBSAXHandler.NODE_ID);
            } else {
                pstmt.setNull(9, Types.BIGINT);
            }
            if (catalogId != -1) {
                pstmt.setInt(10, catalogId);
            } else {
                pstmt.setNull(10, Types.BIGINT);
            }
            logMetacat.debug("Executing SQL: " + pstmt.toString());
            pstmt.execute();
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    /*
     * This method will determine if we need to insert or update xml_document base
     * on the given rev in xml_documents table, user-specified rev and docid.
     */
    private static String checkRevInXMLDocuments(int revInDataBase, String docid, int userSpecifyRev)
                                        throws MetacatException, PropertyNotFoundException {
        String action = null;
        logMetacat.debug("The docid without rev is " + docid);
        logMetacat.debug("The user specifyRev: " + userSpecifyRev);
        // Revision for this docid in current database
        logMetacat.debug("The rev in data base: " + revInDataBase);

        //revIndataBase=-1, there is no record in xml_documents table
        //the document is a new one for local server, inert it into table
        //user specified rev should be great than 0
        if (revInDataBase == -1 && userSpecifyRev >= 0) {
            // rev equals user specified
            // action should be INSERT
            action = "INSERT";
        }
        //rev is greater the last revsion number and revInDataBase isn't -1
        // it is a updated file
        else if (userSpecifyRev > revInDataBase && revInDataBase >= 0) {
            // rev equals user specified
            // action should be update
            action = "UPDATE";
        }
        // local server has newer version, then notify the remote server
        else if (userSpecifyRev < revInDataBase && revInDataBase > 0) {
            throw new MetacatException(
                "Local server: " + SystemUtil.getServerURL() + " has newer revision of doc: "
                    + docid + "." + revInDataBase
                    + ". Please notify the remote server's administrator.");
        }
        //other situation
        else {
            throw new MetacatException(
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
     * Deletes a doc or data file from the filesystem using identifier.
     *
     * @param id  the identifier of the object which will deleted
     * @throws InterruptedException
     * @throws IOException
     * @throws ServiceFailure
     * @throws NoSuchAlgorithmException
     * @throws IllegalArgumentException
     */
    protected static void deleteFromFileSystem(Identifier id) throws IllegalArgumentException,
                                                    NoSuchAlgorithmException, ServiceFailure,
                                                    IOException, InterruptedException {
        MetacatInitializer.getStorage().deleteObject(id);
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
     * Delete an object totally from the db and file system. It doesn't check permission
     * @param accnum  the local id (including the rev) will be deleted
     * @param guid  the dataone identifier associated with accnum
     * @throws SQLException
     * @throws InvalidRequest
     * @throws McdbDocNotFoundException
     * @throws ServiceFailure
     */
    public static void delete(String accnum, Identifier guid) throws SQLException, InvalidRequest,
                                                        McdbDocNotFoundException, ServiceFailure {
        if (accnum == null || accnum.isBlank()) {
            throw new InvalidRequest("0000",
                                        "DcoumentImple.delete - the docid can't be null or blank");
        }
        if (guid == null || guid.getValue() == null || guid.getValue().isBlank()) {
            throw new InvalidRequest("0000", "DcoumentImple.delete -The pid can't be null or blank");
        }
        DBConnection conn = null;
        int serialNumber = -1;

        boolean inRevisionTable = false;
        double start = System.currentTimeMillis() / 1000;
        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("DocumentImpl.delete");
            serialNumber = conn.getCheckOutSerialNumber();
            // CLIENT SHOULD ALWAYS PROVIDE ACCESSION NUMBER INCLUDING REV
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            int rev = DocumentUtil.getRevisionFromAccessionNumber(accnum);
            String type = null;
            // Check if the document exists.
            logMetacat.info("DocumentImp.delete - completely delete the document " + accnum);
            String query = "SELECT doctype, docid FROM xml_documents WHERE docid = ? and rev = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, docid);
                pstmt.setInt(2, rev);
                logMetacat.debug("DocumentImpl.delete - executing SQL: " + pstmt.toString());
                pstmt.execute();
                try (ResultSet rs = pstmt.getResultSet()) {
                    if (!rs.next()) {
                        //look at the xml_revisions table
                        logMetacat.debug("DocumentImpl.delete - look at the docid " + accnum
                                             + " in the xml_revision table");
                        String query2 = "SELECT doctype, docid FROM xml_revisions WHERE docid = ? "
                                          + "AND rev = ?";
                        try (PreparedStatement pstmt2 = conn.prepareStatement(query2)) {
                            pstmt2.setString(1, docid);
                            pstmt2.setInt(2, rev);
                            logMetacat.debug("DocumentImpl.delete - executing SQL: "
                                                                               + pstmt2.toString());
                            pstmt2.execute();
                            try (ResultSet rs2 = pstmt2.getResultSet()) {
                                if (!rs2.next()) {
                                    conn.increaseUsageCount(1);
                                    throw new McdbDocNotFoundException("Docid " + accnum
                                                        + " does not exist in eiter xml_documents "
                                                        + "or xml_revisions table. "
                                                        + "Please check and try again.");
                                } else {
                                    type = rs2.getString(1);
                                    logMetacat.debug("DocumentImpl.delete - the docid " + accnum
                                            + " is in the xml_revisions table");
                                    conn.increaseUsageCount(1);
                                    inRevisionTable = true;
                                }
                            }
                        }
                    } else {
                        type = rs.getString(1);
                        logMetacat.debug("DocumentImpl.delete - the docid " + accnum + " and type "
                                + type + " is in the xml_document table");
                        conn.increaseUsageCount(1);
                    }
                }
            }
            logMetacat.info("DocumentImpl.delete - the deleting doc type is " + type + "...");
            logMetacat.debug("DocumentImpl.delete - Start deleting doc " + docid + "...");
            try {
                conn.setAutoCommit(false);
                if (inRevisionTable) {
                    logMetacat.debug("DocumentImpl.delete - deleting from xml_revisions");
                    String deleteQuery = "DELETE FROM xml_revisions WHERE docid = ? AND rev = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {
                        pstmt.setString(1, docid);
                        pstmt.setInt(2, rev);
                        logMetacat.debug("DocumentImpl.delete - running sql: " + pstmt.toString());
                        pstmt.execute();
                        conn.increaseUsageCount(1);
                    }
                } else {
                    // Delete it from xml_documents table
                    logMetacat.debug("DocumentImpl.delete - deleting from xml_documents");
                    String deleteQuery = "DELETE FROM xml_documents WHERE docid = ? AND rev = ?";
                    try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteQuery)) {
                        pstmtDelete.setString(1, docid);
                        pstmtDelete.setInt(2, rev);
                        logMetacat.debug("DocumentImpl.delete - running sql: "
                                                                         + pstmtDelete.toString());
                        pstmtDelete.execute();
                        conn.increaseUsageCount(1);
                    }
                }
                //update systemmetadata table and solr index
                SystemMetadata sysMeta = SystemMetadataManager.getInstance().get(guid);
                if (sysMeta != null) {
                    SystemMetadataManager.getInstance().delete(guid, conn);
                }
                // only commit if all of this was successful
                conn.commit();
                try {
                    MetacatSolrIndex.getInstance().submitDeleteTask(guid, sysMeta);
                } catch (Exception ee) {
                    logMetacat.error("DocumentImpl.delete - Metacat failed to submit index task: "
                                                                           + ee.getMessage());
                }
                // This line will delete system metadata in hastore as well
                try {
                    deleteFromFileSystem(guid);
                } catch (Exception ee) {
                    logMetacat.error("Can not delete the object or the associated system "
                                         + "metadata from Hashstore for object " + guid.getValue()
                                         + " since " + ee.getMessage() + ", even though we "
                                         + "successfully removed it from the Metacat database. You"
                                         + " have to manually to remove them from Hashstore.");
                }
            } catch (Exception e) {
                // rollback the delete if there was an error
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException sqe) {
                        throw new ServiceFailure("0000", "DocumentImpl.delete - failed for "
                                                      + guid.getValue() + " since "
                                                      + e.getMessage()
                                                      + " Also the database cannot roll back since "
                                                      + sqe.getMessage());
                    }
                }
                logMetacat.error("DocumentImpl.delete -  failed for " + guid.getValue()
                                                                      + " since " + e.getMessage());
                throw new ServiceFailure("0000", "DocumentImpl.delete - failed for "
                                                     + guid.getValue() + " since "+ e.getMessage());
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logMetacat.warn("DocumentImpl.delete - Metacat can't set DBConnection "
                                    + "auto-commit back to true since " + e.getMessage());
                }
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
        double end = System.currentTimeMillis() / 1000;
        logMetacat.info("DocumentImpl.delete - total delete time is:  " + (end - start));
    }

    /**
     * Archive an object. Set the archived flag true, also move the object from the xml_documents
     * table to the xml_revisions table if it exists in the xml_documents table.
     * This method will submit the reindex task as well.
     * Note: this method doesn't lock identifiers for storing the system metadata
     * @param accnum  the local id (including the revision) will be applied.
     * @param guid  the dataone identifier associated with the given accnum
     * @param user  the identity of operator
     * @param changeDateModified  if it needs to change the dateModified field in the system metadata
     * @param sysMetaCheck  check whether the version of the provided '@param sysMeta'
     *                       matches the version of the existing system metadata
     * @throws SQLException
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    public static void archive(String accnum, Identifier guid, String user,
                boolean changeDateModified, SystemMetadataManager.SysMetaVersion sysMetaCheck)
                                            throws SQLException, InvalidRequest, ServiceFailure {
        if (accnum == null || accnum.isBlank()) {
            throw new InvalidRequest("0000",
                                        "DcoumentImple.delete - the docid can't be null or blank");
        }
        if (guid == null || guid.getValue() == null || guid.getValue().isBlank()) {
            throw new InvalidRequest("0000", "DcoumentImple.delete -The pid can't be null or blank");
        }
        DBConnection conn = null;
        int serialNumber = -1;
        double start = System.currentTimeMillis() / 1000;
        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("DocumentImpl.archive");
            serialNumber = conn.getCheckOutSerialNumber();
            // CLIENT SHOULD ALWAYS PROVIDE ACCESSION NUMBER INCLUDING REV
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            int rev = DocumentUtil.getRevisionFromAccessionNumber(accnum);
            // Check if the document exists.
            //this only archives a document from xml_documents to xml_revisions
            logMetacat.debug("DocumentImp.archive - archive the document " + accnum);
            SystemMetadata backup = SystemMetadataManager.getInstance().get(guid);
            if (backup == null) {
                throw new InvalidRequest(
                    "0000", "Can't archive this object " + guid.getValue()
                    + " since the system metadata can't be found");
            }
            try {
                conn.setAutoCommit(false);
                // Copy the record to the xml_revisions table if it exists in
                // the xml_documents table and also delete the item from xml_documents
                archiveDocToRevision(conn, docid, rev, user);
                //update systemmetadata table and solr index
                SystemMetadata sysMeta = SystemMetadataManager.getInstance().get(guid);
                if (sysMeta != null) {
                    sysMeta.setArchived(true);
                    SystemMetadataManager.getInstance().store(sysMeta, changeDateModified,
                                                                conn, sysMetaCheck);
                } else {
                    throw new InvalidRequest(
                        "0000", "Can't archive this object " + guid.getValue()
                        + " since the system metadata can't be found");
                }
                // only commit if all of this was successful
                conn.commit();
                try {
                    //followRevisions is set false
                    MetacatSolrIndex.getInstance().submit(guid, sysMeta, false);
                } catch (Exception ee) {
                    logMetacat.error("DocumentImpl.archive - Metacat failed to submit index task: "
                                                                           + ee.getMessage());
                }
            } catch (Exception e) {
                // rollback the archive action if there was an error
                StringBuffer error = new StringBuffer();
                error.append("DocumentImpl.archive - failed for ");
                error.append(guid.getValue());
                String errorString = SystemMetadataManager.storeRollBack(guid, e, conn, backup);
                error.append(errorString);
                logMetacat.error(error.toString());
                throw new ServiceFailure("0000", errorString);
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logMetacat.warn("DocumentImpl.archive - Metacat can't set DBConnection "
                                    + "auto-commit back to true since " + e.getMessage());
                }
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
        double end = System.currentTimeMillis() / 1000;
        logMetacat.info("DocumentImpl.archive - total delete time is:  " + (end - start));
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
        DBSAXHandler handler;
        EntityResolver eresolver;
        DTDHandler dtdhandler;
        // Get an instance of the parser
        String parserName = PropertyService.getProperty("xml.saxparser");
        parser = XMLReaderFactory.createXMLReader(parserName);
        handler = new DBSAXHandler();
        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);
        parser.setProperty(DECLARATIONHANDLERPROPERTY, handler);
        parser.setProperty(LEXICALPROPERTY, handler);
        boolean valid = ruleBase != null && needValidation;
        if (valid && (ruleBase.equals(SCHEMA)
                      || ruleBase.equals(EML200)
                      || ruleBase.equals(EML210))) {
            XMLSchemaService xmlss = XMLSchemaService.getInstance();
            logMetacat.info("DocumentImpl.initalizeParser - Using General schema parser");
            // turn on schema validation feature
            parser.setFeature(VALIDATIONFEATURE, true);
            parser.setFeature(NAMESPACEFEATURE, true);
            parser.setFeature(SCHEMAVALIDATIONFEATURE, true);

            boolean allSchemasRegistered = XMLSchemaService.areAllSchemasRegistered(schemaList);
            if (xmlss.useFullSchemaValidation() && !allSchemasRegistered
                                      && !ruleBase.equals(EML210) && !ruleBase.equals(EML200)) {
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
        } else if (valid && ruleBase.equals(NONAMESPACESCHEMA)) {
            logMetacat.info("DocumentImpl.initalizeParser - Using General schema parser");
            // turn on schema validation feature
            parser.setFeature(VALIDATIONFEATURE, true);
            parser.setFeature(NAMESPACEFEATURE, true);
            parser.setFeature(SCHEMAVALIDATIONFEATURE, true);
            logMetacat.info(
                "DocumentImpl.initalizeParser - Generic external no-namespace schema location: "
                    + schemaLocation);
            // Set external schemalocation.
            if (schemaLocation != null && !schemaLocation.isBlank()) {
                parser.setProperty(EXTERNALNONAMESPACESCHEMALOCATIONPROPERTY, schemaLocation);
            } else {
                throw new ServiceFailure("0000", "The schema for the document "
                                        + " can't be found in any place. So we can't validate"
                                        + " the xml instance.");
            }
        } else if (valid && ruleBase.equals(DTD)) {
            logMetacat.info("DocumentImpl.initalizeParser - Using dtd parser");
            // turn on dtd validaton feature
            parser.setFeature(VALIDATIONFEATURE, true);
            eresolver = new DBEntityResolver((DBSAXHandler) handler, dtd);
            dtdhandler = new DBDTDHandler();
            parser.setEntityResolver((EntityResolver) eresolver);
            parser.setDTDHandler((DTDHandler) dtdhandler);
        } else {
            logMetacat.info("DocumentImpl.initalizeParser - Using other parser");
            // non validation
            parser.setFeature(VALIDATIONFEATURE, false);
            eresolver = new DBEntityResolver((DBSAXHandler) handler, dtd);
            dtdhandler = new DBDTDHandler();
            parser.setEntityResolver((EntityResolver) eresolver);
            parser.setDTDHandler((DTDHandler) dtdhandler);
        }
        return parser;
    }

    /**
     * This method will copy a document record from the xml_documents table
     * to the xml_revisions table if the record exists in the xml_documents table.
     * It also will delete the record from the xml_documents if it exists.
     * @param dbconn  the jdbc connection will be used to execute query
     * @param docid  the docid of the document
     * @param rev  the revision of the document
     * @param user  the user who request the action
     * @throws SQLException
     */
    private static void archiveDocToRevision(DBConnection dbconn, String docid, int rev, String user)
                                                                            throws SQLException {
        //Move the document information to xml_revisions table...
        double start = System.currentTimeMillis() / 1000;
        try (PreparedStatement pstmt = dbconn.prepareStatement(
                "INSERT INTO xml_revisions " + "(docid, rootnodeid, docname, doctype, "
                    + "user_owner, user_updated, date_created, date_updated, "
                    + "rev, catalog_id) "
                    + "SELECT ?, rootnodeid, docname, doctype, "
                    + "user_owner, ?, date_created, date_updated, "
                    + "rev, catalog_id " + "FROM xml_documents "
                    + "WHERE docid = ? AND rev = ?")) {
            // Increase dbconnection usage count
            dbconn.increaseUsageCount(1);
            // Bind the values to the query and execute it
            pstmt.setString(1, docid);
            pstmt.setString(2, user);
            pstmt.setString(3, docid);
            pstmt.setInt(4, rev);
            logMetacat.debug("DocumentImpl.archiveDocToRevision - Executing SQL: "
                                + pstmt.toString());
            pstmt.execute();
            // Delete the record on xml_documents
            String deleteQuery = "DELETE FROM xml_documents WHERE docid = ? AND rev = ?";
            try (PreparedStatement pstmt2 = dbconn.prepareStatement(deleteQuery)) {
                pstmt2.setString(1, docid);
                pstmt2.setInt(2, rev);
                logMetacat.debug("Running sql: " + pstmt2.toString());
                pstmt2.execute();
                //Usaga count increase 1
                dbconn.increaseUsageCount(1);
            }
            double end = System.currentTimeMillis() / 1000;
            logMetacat.debug(
                "DocumentImpl.archiveDocToRevision - moving docs from xml_documents to "
                + "xml_revision takes "
                    + (end - start));
        }
    }

    /**
     * Get the catalog id for the given document type
     * @param docType  the document type which will be checked
     * @return the catalog id associated with the document type. -1 will be returned if Metacat
     *         cannot find it.
     * @throws SQLException
     */
    private static int getCatalogId(String docType) throws SQLException {
        int catalogId = -1;
        // Because this is select statement and it needn't to roll back
        DBConnection dbConn = null;
        int serialNumber = -1;
        if (docType != null && !docType.isBlank()) {
            try {
                // Get dbconnection
                dbConn = DBConnectionPool
                        .getDBConnection("DBSAXHandler.startElement");
                serialNumber = dbConn.getCheckOutSerialNumber();
                String sql = "SELECT catalog_id FROM xml_catalog WHERE public_id = ?";
                try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
                    pstmt.setString(1, docType);
                    ResultSet rs = pstmt.executeQuery();
                    boolean hasRow = rs.next();
                    if (hasRow) {
                        catalogId = rs.getInt(1);
                    }
                }
            }//try
            finally {
                // Return dbconnection
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }//finally
        }
        logMetacat.debug("The catalog id for " + docType + " is " + catalogId);
        return catalogId;
    }
}
