package edu.ucsb.nceas.metacat.admin.upgrade;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.NodeComparator;
import edu.ucsb.nceas.metacat.NodeRecord;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.SystemMetadataFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.ParseLSIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * Historically, early versions of Metacat only store the metadata documents in the xml_nodes and 
 * xml_nodes_revsions tables rather than the files. Now, we drop the both tables in Metacat 3.0.0. 
 * This class is to make sure that all metadata objects have been serialized into the files.
 * @author tao
 *
 */
public class XMLNodesToFilesChecker {
    private final static String PREFIX_SQL = "SELECT docid, rev, rootnodeid, catalog_id, " 
                                                + "doctype, docname FROM ";
    private final static String APPENDIX_SQL = " WHERE rootnodeid >= 0 " 
                                               + " AND docid NOT LIKE 'autogen.%'";
    private final static String XML_DOCUMENTS = "xml_documents";
    private final static String XML_REVISIONS = "xml_revisions";
    private final static String XML_NODES = "xml_nodes";
    private final static String XML_NODES_REVISIONS = "xml_nodes_revisions";
    private final static String INLINE = "inline";
    private final static String SHA1 = "SHA-1";
    private final static String SUCCESS_DOC_LOG_FILE = "success_export_xml_nodes_to_file_docid";
    private final static String FAILURE_DOC_LOG_FILE = "failure_export_xml_nodes_to_file_docid";
    
    private static String document_dir = null;
    private static String inline_dir = null;
    private static String log_dir = null;
    private static Log logMetacat = LogFactory.getLog("DBtoFilesChecker");

    
    /**
     * Default constructor
     * @throws PropertyNotFoundException 
     * @throws IOException 
     */
    public XMLNodesToFilesChecker() throws PropertyNotFoundException {
        if (document_dir == null) {
            document_dir = PropertyService.getProperty("application.documentfilepath");
            logMetacat.debug("XMLNodestoFilesChecker.DBtoFilesChecker - The document directory is " 
                                + document_dir);
        }
        if (inline_dir == null)  {
            inline_dir = PropertyService.getProperty("application.inlinedatafilepath");
            logMetacat.debug("XMLNodestoFilesChecker.DBtoFilesChecker - The inline data directory is " 
                    + inline_dir);
        }
        if (log_dir == null)  {
            log_dir = PropertyService.getProperty("application.tempDir");
            logMetacat.debug("XMLNodestoFilesChecker.DBtoFilesChecker - The log directory is " 
                    + log_dir);
        }
    }
    
    /**
     * This method does the job - make sure all metadata object are stored in the files.
     * Note: this method must be called before running the 3.0.0 upgrade sql script, which will
     * drop the related tables.
     * @throws SQLException 
     * @throws IOException 
     */
    public void check() throws SQLException, IOException {
        checkXmlDocumentsTable();
        checkXmlRevisionsTable();
    }
    
    /**
     * Check the objects in the xml_documents table
     * @throws SQLException 
     * @throws IOException 
     */
    private void checkXmlDocumentsTable() throws SQLException, IOException {
        PreparedStatement rs = runSQL(XML_DOCUMENTS);
        checkIfFilesExist(rs, XML_DOCUMENTS);
    }
    
    /**
     * Check the objects in the xml_documents table
     * @throws SQLException 
     * @throws IOException
     */
    private void checkXmlRevisionsTable() throws SQLException, IOException {
        PreparedStatement rs = runSQL(XML_REVISIONS);
        checkIfFilesExist(rs, XML_REVISIONS);
    }
    
    /**
     * Iterate the result set to check if a docid exists in the file system. If it does exist,
     * skip it; otherwise export the database record on the xml_node table to the file system. 
     * @param result
     * @throws SQLException
     * @throws IOException 
     */
    private void checkIfFilesExist(PreparedStatement pstmt, String tableName) throws SQLException, 
                                                                                    IOException {
        boolean append = true;
        BufferedWriter success_writer = null;
        BufferedWriter failure_writer = null;
        ResultSet result = null;
        try {
            success_writer = new BufferedWriter(new FileWriter(log_dir + File.separator 
                                                    + SUCCESS_DOC_LOG_FILE, append));
            failure_writer = new BufferedWriter(new FileWriter(log_dir + File.separator 
                                                    + FAILURE_DOC_LOG_FILE, append));
            if (pstmt != null) {
                result = pstmt.getResultSet();
                while (result != null && result.next()) {
                    String docId = null;
                    int rev = -1;
                    boolean fileOriginExists = true;
                    File documentFile = null;
                    try {
                        docId = result.getString(1);
                        rev = result.getInt(2);
                        long rootNodeId = result.getLong(3);
                        long catalogId = result.getLong(4);
                        String docType = result.getString(5);
                        String docName = result.getString(6);
                        String path = document_dir + File.separator + docId + "." + rev;
                        documentFile = new File(path);
                        if (documentFile.exists()) {
                            fileOriginExists = true;
                            logMetacat.debug("XMLNOdesToFileChecker.checkIfFilesExist - The file " 
                                                + documentFile.getAbsolutePath() +" exists.");
                            continue; //do nothting since the file exists
                        } else {
                            logMetacat.debug("XMLNOdesToFileChecker.checkIfFilesExist - The file " 
                                             + documentFile.getAbsolutePath() +" doesnot exist.");
                            fileOriginExists = false;
                            //The file doesn't exist. Let's export the data from db to the file system.
                            boolean isDTD = false;
                            String[] systemIdEntyTypePair = getSystemIdAndTypePair(catalogId);
                            String systemId = systemIdEntyTypePair[0];
                            String entryType = systemIdEntyTypePair[1];
                            logMetacat.debug("XMLNodestoFilesChecker.checkIfFilesExist - " 
                                            + " the system id is " + systemId + " and entry type is " 
                                            + entryType + " for the catalog id " + catalogId);
                            if (entryType != null && entryType.equals(DocumentImpl.DTD)) {
                                isDTD = true;
                            }
                            exportXMLnodesToFile(docId, rev, rootNodeId, tableName, isDTD, systemId, 
                                                 docType, docName);
                            //Log the docid into success file
                            try {
                                success_writer.write(docId + "." + rev + "\n");
                            } catch (IOException ioe) {
                                logMetacat.warn("XMLNodestoFilesChecker.checkIfFilesExist - can't "
                                                + "log the docid " + docId + "." + rev 
                                                + " into the success log file since " 
                                                + ioe.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        logMetacat.warn("XMLNodestoFilesChecker.checkIfFilesExist - " 
                                    + "can't check if the metadata object " + docId + rev 
                                    + " exists in the directory " + document_dir 
                                    + " since " + e.getMessage());
                        if (!fileOriginExists && documentFile != null) {
                            logMetacat.debug("XMLNodestoFilesChecker.checkIfFilesExist - delete " 
                                                + " the just created file " 
                                                    + documentFile.getAbsolutePath());
                            documentFile.delete();
                        }
                        //Log the docid into success file
                        try {
                            failure_writer.write(docId + "." + rev +"\n");
                        } catch (IOException ioe) {
                            logMetacat.warn("XMLNodestoFilesChecker.checkIfFilesExist - can't "
                                    + "log the docid " + docId + "." + rev 
                                    + " into the failure log file since " 
                                    + ioe.getMessage());
                        }
                        
                    }
                }
            }
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException e) {
                    logMetacat.warn("XMLNodestoFilesChecker.checkIfFilesExist - can't close " 
                            + " the result set since " + e.getMessage());
                }
            }
            
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    logMetacat.warn("XMLNodestoFilesChecker.checkIfFilesExist - can't close " 
                            + " the result set since " + e.getMessage());
                }
            }
            if (success_writer != null) {
                try {
                    success_writer.close();
                } catch (IOException ee) {
                    logMetacat.warn("XMLNodestoFilesChecker.checkIfFilesExist - can't close " 
                                    + " the success_writer since " + ee.getMessage());
                }
            }
            if (failure_writer != null) {
                failure_writer.close();
            }
        }
    }
    
    /**
     * Get system id and entry type (DTD, Schema et all) for the given catalog id
     * @param catalogId  the catalog id which will be looked for
     * @return an array of string. First element is the system id; the second one is the entry type
     * @throws SQLException
     */
    private String[] getSystemIdAndTypePair(long catalogId) throws SQLException {
        String[] result = new String[2];
        ResultSet rs = null;
        String sql = "SELECT system_id, entry_type FROM xml_catalog WHERE catalog_id=?";
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        try {
            //check out DBConnection
            conn=DBConnectionPool.getDBConnection("DBtoFilesChecker.getSystemIdAndTypePair");
            serialNumber=conn.getCheckOutSerialNumber();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, catalogId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                result[0] = rs.getString(1);
                result[1] = rs.getString(2);
            }
        } finally {
            try {
                if (pstmt != null) {
                     pstmt.close();
                }
                if (rs != null) {
                     rs.close();
                }
            } finally {
               DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
        return result;
    }
    
    /**
     * Export an object form xml_nodes/xml_nodes_revision table to a file.
     * Also check if the system metadata and identifier table have the record. 
     * @param docId
     * @param rev
     * @param rootNodeId
     * @throws IOException 
     * @throws McdbException 
     * @throws SQLException 
     * @throws NoSuchAlgorithmException 
     * @throws SAXException 
     * @throws HandlerException 
     * @throws MarshallingException 
     * @throws BaseException 
     * @throws ParseLSIDException 
     * @throws InsufficientKarmaException 
     * @throws AccessionNumberException 
     * @throws AccessException 
     * @throws AccessControlException 
     * @throws PropertyNotFoundException 
     * @throws ClassNotFoundException 
     */
    private void exportXMLnodesToFile(String docId, int rev, long rootNodeId, String tableName, 
                                boolean isDTD, String systemId, String docType, String docName)
                                        throws McdbException, IOException, SQLException, 
                                               NoSuchAlgorithmException, ClassNotFoundException, 
                                               PropertyNotFoundException, AccessControlException, 
                                               AccessException, AccessionNumberException, 
                                               InsufficientKarmaException, ParseLSIDException, 
                                               BaseException, MarshallingException, 
                                               HandlerException, SAXException {
        boolean hasSysmeta = false;
        String pid = docId + "." + rev;
        Identifier identifier = new Identifier();
        identifier.setValue(pid);
        String checksumAlgorithm = SHA1;
        Checksum checksum = null;
        SystemMetadata sysmeta = SystemMetadataManager.getInstance().get(identifier);
        if (sysmeta != null ) {
            hasSysmeta = true;
            checksum = sysmeta.getChecksum();
            if (checksum != null && checksum.getAlgorithm() != null 
                                 && !checksum.getAlgorithm().trim().equals("")) {
                checksumAlgorithm = checksum.getAlgorithm();
            }
        }
        String path = document_dir + File.separator + pid;
        File documentFile = new File(path);
        MessageDigest md = MessageDigest.getInstance(checksumAlgorithm);
        DigestOutputStream output = null;
        try {
            output = new DigestOutputStream(new FileOutputStream(documentFile), md);
            toXmlFromDb(output, tableName, rootNodeId, isDTD, systemId, docType, docName);
            logMetacat.debug("XMLNodestoFilesChecker.exportXMLnodesToFile - successfully wrote the " 
                                + " meta data object to the path " + path);
        } finally {
            if (output != null) {
                output.close();
            }
        }
        String localChecksum = DatatypeConverter.printHexBinary(md.digest());
        if (hasSysmeta) {
            //make sure the checksum in the existing system metadata match the checksum of the file
            //we just created. If they don't match, set the new checksum into the existing sysmeta
            if (checksum == null || checksum.getValue() == null || checksum.getAlgorithm() == null 
                        || !checksum.getAlgorithm().equals(checksumAlgorithm)
                        || !checksum.getValue().equals(localChecksum) ) {
                // Set the new checksum
                Checksum newChecksum = new Checksum();
                newChecksum.setAlgorithm(checksumAlgorithm);
                newChecksum.setValue(localChecksum);
                sysmeta.setChecksum(newChecksum);
                storeSystemMetadataToDB(sysmeta);
            }
        } else {
            //We need to generate the system metadata
            boolean includeORE = false;
            boolean downloadData = false;
            SystemMetadata newSysmeta = SystemMetadataFactory.
                                            createSystemMetadata(pid, includeORE, downloadData);
            storeSystemMetadataToDB(newSysmeta);
        }
        //Register the docid and guid in the identifier table if they don't exist.
        if (!IdentifierManager.getInstance().mappingExists(pid)) {
            IdentifierManager.getInstance().createMapping(pid, pid);
        }
    }
    
    /**
     * Store the given system metadata object into DB only
     * @param sysmeta  the system metadata will be stored
     * @throws SQLException
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    private void storeSystemMetadataToDB(SystemMetadata sysmeta) throws SQLException, InvalidRequest, 
                                                                            ServiceFailure {
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            //check out DBConnection
            conn=DBConnectionPool.getDBConnection("DBtoFilesChecker.storeSystemMetadata");
            serialNumber=conn.getCheckOutSerialNumber();
            boolean modifyTimeStamp = true;
            boolean onlyToDB = true;
            SystemMetadataManager.getInstance().store(sysmeta, modifyTimeStamp, conn, onlyToDB);
        } finally {
           DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
    }
    
    /**
     * Print a text representation of the XML document to an OutputStream object
     * @param outputStream  the OutputStream object will be get the content
     * @param rootnodeid  the root node id of the document
     * @param validateType  if this document is based on DTD
     * @param systemId  the system id of the document
     * @param doctype  the doc type of this document
     * @param docname  the doc name of this document
     * @throws McdbException
     * @throws IOException
     */
    private void toXmlFromDb(OutputStream outputStream, String tableName, long rootnodeid, 
                            boolean isDTD, String systemId, String doctype, String docname) 
                                    throws McdbException, IOException {
        // flag for process eml2
        boolean proccessEml2 = false;
        boolean storedDTD = false;//flag to inidate publicid or system
        // id stored in db or not
        boolean firstElement = true;
        String dbDocName = null;
        String dbPublicID = null;
        String dbSystemID = null;

        if (doctype != null
                && (doctype.equals(DocumentImpl.EML2_0_0NAMESPACE)
                        || doctype.equals(DocumentImpl.EML2_0_1NAMESPACE) 
                        || doctype.equals(DocumentImpl.EML2_1_0NAMESPACE)
                        || doctype.equals(DocumentImpl.EML2_1_1NAMESPACE) 
                        || doctype.equals(DocumentImpl.EML2_2_0NAMESPACE) )) {
            proccessEml2 = true;
        }
        // flag for process inline data
        boolean processInlineData = false;
        TreeSet<NodeRecord> nodeRecordLists = getNodeRecordList(rootnodeid, tableName);
        // Note: we haven't stored the encoding, so we use the default for XML
        String encoding = "UTF-8";
        Writer out = new OutputStreamWriter(outputStream, encoding);
        Stack<NodeRecord> openElements = new Stack<NodeRecord>();
        boolean atRootElement = true;
        boolean previousNodeWasElement = false;
        // Step through all of the node records we were given
        Iterator<NodeRecord> it = nodeRecordLists.iterator();
        while (it.hasNext()) {
            NodeRecord currentNode = it.next();
            logMetacat.debug("[Got Node ID: " + currentNode.getNodeId() + " ("
                    + currentNode.getParentNodeId() + ", " + currentNode.getNodeIndex()
                    + ", " + currentNode.getNodeType() + ", " + currentNode.getNodeName()
                    + ", " + currentNode.getNodeData() + ")]");
            // Print the end tag for the previous node if needed
            //
            // This is determined by inspecting the parent nodeid for the
            // currentNode. If it is the same as the nodeid of the last element
            // that was pushed onto the stack, then we are still in that
            // previous
            // parent element, and we do nothing. However, if it differs, then
            // we
            // have returned to a level above the previous parent, so we go into
            // a loop and pop off nodes and print out their end tags until we
            // get
            // the node on the stack to match the currentNode parentnodeid
            //
            // So, this of course means that we rely on the list of elements
            // having been sorted in a depth first traversal of the nodes, which
            // is handled by the NodeComparator class used by the TreeSet
            if (!atRootElement) {
                NodeRecord currentElement = openElements.peek();
                if (currentNode.getParentNodeId() != currentElement.getNodeId()) {
                    while (currentNode.getParentNodeId() != currentElement.getNodeId()) {
                        currentElement = (NodeRecord) openElements.pop();
                        logMetacat.debug("\n POPPED: "
                                + currentElement.getNodeName());
                        if (previousNodeWasElement) {
                            out.write(">");
                            previousNodeWasElement = false;
                        }
                        if (currentElement.getNodePrefix() != null) {
                            out.write("</" + currentElement.getNodePrefix() + ":"
                                    + currentElement.getNodeName() + ">");
                        } else {
                            out.write("</" + currentElement.getNodeName() + ">");
                        }
                        currentElement = openElements.peek();
                    }
                }
            }

            // Handle the DOCUMENT node
            if (currentNode.getNodeType().equals("DOCUMENT")) {
                out.write("<?xml version=\"1.0\"?>");
                // Handle the ELEMENT nodes
            } else if (currentNode.getNodeType().equals("ELEMENT")) {
                if (atRootElement) {
                    atRootElement = false;
                } else {
                    if (previousNodeWasElement) {
                        out.write(">");
                    }
                }
                // if publicid or system is not stored into db send it out by
                // default
                if (!storedDTD & firstElement) {
                    if (docname != null && isDTD) {
                        if ((doctype != null) && (systemId != null)) {
                            out.write("<!DOCTYPE " + docname + " PUBLIC \""
                                    + doctype + "\" \"" + systemId + "\">");
                        } else {
                            out.write("<!DOCTYPE " + docname + ">");
                        }
                    }
                }
                firstElement = false;
                openElements.push(currentNode);
                logMetacat.debug("\n PUSHED: " + currentNode.getNodeName());
                previousNodeWasElement = true;
                if (currentNode.getNodePrefix() != null) {
                    out.write("<" + currentNode.getNodePrefix() + ":"
                            + currentNode.getNodeName());
                } else {
                    out.write("<" + currentNode.getNodeName());
                }

                // if currentNode is inline and handle eml2, set flag process
                // on
                if (currentNode.getNodeName() != null
                        && currentNode.getNodeName().equals(INLINE)
                        && proccessEml2) {
                    processInlineData = true;
                }

                // Handle the ATTRIBUTE nodes
            } else if (currentNode.getNodeType().equals("ATTRIBUTE")) {
                if (currentNode.getNodePrefix() != null) {
                    out.write(" " + currentNode.getNodePrefix() + ":"
                            + currentNode.getNodeName() + "=\""
                            + currentNode.getNodeData() + "\"");
                } else {
                    out.write(" " + currentNode.getNodeName() + "=\""
                            + currentNode.getNodeData() + "\"");
                }

                // Handle the NAMESPACE nodes
            } else if (currentNode.getNodeType().equals("NAMESPACE")) {
                String nsprefix = " xmlns:";
                if(currentNode.getNodeName() == null || currentNode.getNodeName().trim().equals(""))
                {
                  nsprefix = " xmlns";
                }
                
                out.write(nsprefix + currentNode.getNodeName() + "=\""
                          + currentNode.getNodeData() + "\"");

                // Handle the TEXT nodes
            } else if (currentNode.getNodeType().equals("TEXT")) {
                if (previousNodeWasElement) {
                    out.write(">");
                }
                if (!processInlineData) {
                    // if it is not inline data just out put data
                    out.write(currentNode.getNodeData());
                } else {
                    // if it is inline data first to get the inline data
                    // internal id
                    String fileName = currentNode.getNodeData();
                    //user want to see it, pull out from file system and 
                    // output it for inline data, the data base only store 
                    // the file name, so we can combine the file name and
                    // inline data file path, to get it
                    Reader reader = readInlineDataFromFileSystem(fileName, encoding);
                    char[] characterArray = new char[4 * 1024];
                    try {
                        int length = reader.read(characterArray);
                        while (length != -1) {
                            out.write(new String(characterArray, 0,
                                            length));
                            out.flush();
                            length = reader.read(characterArray);
                        }
                        reader.close();
                    } catch (IOException e) {
                        throw new McdbException(e.getMessage());
                    }
                    // reset proccess inline data false
                    processInlineData = false;
                }// in inlinedata part
                previousNodeWasElement = false;
                // Handle the COMMENT nodes
            } else if (currentNode.getNodeType().equals("COMMENT")) {
                if (previousNodeWasElement) {
                    out.write(">");
                }
                out.write("<!--" + currentNode.getNodeData() + "-->");
                previousNodeWasElement = false;
                // Handle the PI nodes
            } else if (currentNode.getNodeType().equals("PI")) {
                if (previousNodeWasElement) {
                    out.write(">");
                }
                out.write("<?" + currentNode.getNodeName() + " "
                        + currentNode.getNodeData() + "?>");
                previousNodeWasElement = false;
                // Handle the DTD nodes (docname, publicid, systemid)
            } else if (currentNode.getNodeType().equals(DocumentImpl.DTD)) {
                storedDTD = true;
                if (currentNode.getNodeName().equals(DocumentImpl.DOCNAME)) {
                    dbDocName = currentNode.getNodeData();
                }
                if (currentNode.getNodeName().equals(DocumentImpl.PUBLICID)) {
                    dbPublicID = currentNode.getNodeData();
                }
                if (currentNode.getNodeName().equals(DocumentImpl.SYSTEMID)) {
                    dbSystemID = currentNode.getNodeData();
                    // send out <!doctype .../>
                    if (dbDocName != null) {
                        if ((dbPublicID != null) && (dbSystemID != null)) {

                            out
                                    .write("<!DOCTYPE " + dbDocName
                                            + " PUBLIC \"" + dbPublicID
                                            + "\" \"" + dbSystemID + "\">");
                        } else {

                            out.write("<!DOCTYPE " + dbDocName + ">");
                        }
                    }

                    //reset these variable
                    dbDocName = null;
                    dbPublicID = null;
                    dbSystemID = null;
                }
                // Handle any other node type (do nothing)
            } else {
                // Any other types of nodes are not handled.
                // Probably should throw an exception here to indicate this
            }
            
            out.flush();
        }
        // Print the final end tag for the root element
        while (!openElements.empty()) {
            NodeRecord currentElement = (NodeRecord) openElements.pop();
            logMetacat.debug("\n POPPED: " + currentElement.getNodeName());
            if (currentElement.getNodePrefix() != null) {
                out.write("</" + currentElement.getNodePrefix() + ":"
                        + currentElement.getNodeName() + ">");
            } else {
                out.write("</" + currentElement.getNodeName() + ">");
            }
        }
        out.flush();
    }
    
    /**
     * Read the inline data from a file
     * @param fileName
     * @param encoding
     * @return
     * @throws McdbException
     */
    private static Reader readInlineDataFromFileSystem(String fileName, String encoding)
            throws McdbException {
        // BufferedReader stringReader = null;
        Reader fileReader = null;
        try {
            // the new file name will look like path/docid.rev.2
            File inlineDataDirectory = new File(inline_dir);
            File dataFile = new File(inlineDataDirectory, fileName);
            fileReader = new InputStreamReader(new FileInputStream(dataFile), encoding);
        } catch (Exception e) {
            throw new McdbException(e.getMessage());
        }
        // return stringReader;
        return fileReader;
    }
    
    /**
     * Look up the node data from the database
     * @param rootnodeid  the id of the root node of the node tree to look up
     * @param table  the table name which Metacat will find the nodes list
     */
    private TreeSet<NodeRecord> getNodeRecordList(long rootnodeid, String table) throws McdbException {
        PreparedStatement pstmt = null;
        DBConnection dbconn = null;
        int serialNumber = -1;
        TreeSet<NodeRecord> nodeRecordList = new TreeSet<NodeRecord>(new NodeComparator());
        long nodeid = 0;
        long parentnodeid = 0;
        long nodeindex = 0;
        String nodetype = null;
        String nodename = null;
        String nodeprefix = null;
        String nodedata = null;
        float nodedatanumerical = -1;
        Timestamp nodedatadate = null;
        //System.out.println("in getNodeREcorelist !!!!!!!!!!!3");
        if (table.equals(XML_DOCUMENTS)) {
            table = XML_NODES;
        } else {
            table = XML_NODES_REVISIONS;
        }
        try {
            dbconn = DBConnectionPool
                    .getDBConnection("XMLNodesToFilesChecker.getNodeRecordList");
            serialNumber = dbconn.getCheckOutSerialNumber();
            pstmt = dbconn
                    .prepareStatement("SELECT nodeid,parentnodeid,nodeindex, "
                            + "nodetype,nodename,nodeprefix,nodedata, nodedatanumerical, nodedatadate "
                            + "FROM " + table + " WHERE rootnodeid = ?");
            // Bind the values to the query
            pstmt.setLong(1, rootnodeid);
            //System.out.println("in getNodeREcorelist !!!!!!!!!!!4");
            logMetacat.debug("XMLNodesToFilesChecker.getNodeRecordList - executing SQL: " 
                                + pstmt.toString());
            pstmt.execute();
            ResultSet rs = pstmt.getResultSet();
            //System.out.println("in getNodeREcorelist !!!!!!!!!!!5");
            boolean tableHasRows = rs.next();
            while (tableHasRows) {
                //System.out.println("in getNodeREcorelist !!!!!!!!!!!6");
                nodeid = rs.getLong(1);
                parentnodeid = rs.getLong(2);
                nodeindex = rs.getLong(3);
                nodetype = rs.getString(4);
                nodename = rs.getString(5);
                nodeprefix = rs.getString(6);
                nodedata = rs.getString(7);
                try {
                    logMetacat.debug("XMLNodesToFilesChecker.getNodeRecordList - " 
                                    + "Node data in read process before normalize=== " + nodedata);
                    nodedata = MetacatUtil.normalize(nodedata);
                    logMetacat.debug("XMLNodesToFilesChecker.getNodeRecordList - " 
                                    + "Node data in read process after normalize==== " + nodedata);
                } catch (java.lang.StringIndexOutOfBoundsException SIO){
                    logMetacat.warn("XMLNodesToFilesChecker.getNodeRecordList - " 
                                        + "StringIndexOutOfBoundsException in normalize() while " 
                                        + "reading the document");
                }
                nodedatanumerical = rs.getFloat(8);
                nodedatadate = rs.getTimestamp(9);
                // add the data to the node record list hashtable
                NodeRecord currentRecord = new NodeRecord(nodeid, parentnodeid,
                        nodeindex, nodetype, nodename, nodeprefix, nodedata, nodedatanumerical, nodedatadate);
                nodeRecordList.add(currentRecord);
                // Advance to the next node
                tableHasRows = rs.next();
            }
            pstmt.close();
            //System.out.println("in getNodeREcorelist !!!!!!!!!!!7");
        } catch (SQLException e) {
            throw new McdbException("Error in DocumentImpl.getNodeRecordList "
                    + e.getMessage());
        } finally {
            try {
                pstmt.close();
            } catch (SQLException ee) {
                logMetacat.error("XMLNodesToFilesChecker.getNodeRecordList - General error: "
                                + ee.getMessage());
            } finally {
                DBConnectionPool.returnDBConnection(dbconn, serialNumber);
            }
        }
        //System.out.println("in getNodeREcorelist !!!!!!!!!!!8");
        return nodeRecordList;
    }
    
    /**
     * Run the sql query against the given table. The query will select docid, revision and the 
     * root node id for the metadata objects  
     * @param tableName  the table where the query be applied to
     * @return  the result set of the query
     * @throws SQLException 
     */
    private PreparedStatement runSQL(String tableName) throws SQLException {
        String sql = PREFIX_SQL + tableName + APPENDIX_SQL;
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        try {
          //check out DBConnection
          conn=DBConnectionPool.getDBConnection("DBtoFilesChecker.runSQL");
          serialNumber=conn.getCheckOutSerialNumber();
          pstmt = conn.prepareStatement(sql);
          pstmt.execute();
        } finally {
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
        return pstmt;
    }
}
