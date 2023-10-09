package edu.ucsb.nceas.metacat.admin.upgrade;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * Historically, early versions of Metacat only store the metadata documents in the xml_nodes and 
 * xml_nodes_revsions tables rather than the files. Now, we drop the both tables in Metacat 3.0.0. 
 * This class is to make sure that all metadata objects have been serialized into the files.
 * @author tao
 *
 */
public class XMLNodesToFilesChecker {
    private final static String PREFIX_SQL = "SELECT docid, rev, rootnodeid FROM ";
    private final static String APPENDIX_SQL = " WHERE rootnodeid >= 0 AND doctype != 'BIN' " 
                                               + " AND docid NOT LIKE 'autogen.%'";
    private final static String XML_DOCUMENTS = "xml_documents";
    private final static String XML_REVISIONS = "xml_revisions";
    
    private static String DOCUMENT_DIR = null;
    private static Log logMetacat = LogFactory.getLog("DBtoFilesChecker");

    
    /**
     * Default constructor
     * @throws PropertyNotFoundException 
     */
    public XMLNodesToFilesChecker() throws PropertyNotFoundException {
        if (DOCUMENT_DIR == null) {
            DOCUMENT_DIR = PropertyService.getProperty("application.documentfilepath");
            logMetacat.debug("XMLNodestoFilesChecker.DBtoFilesChecker - The document directory is " 
                                + DOCUMENT_DIR);
        }
    }
    
    /**
     * This method does the job - make sure all metadata object are stored in the files.
     * Note: this method must be called before running the 3.0.0 upgrade sql script, which will
     * drop the related tables.
     * @throws SQLException 
     */
    public void check() throws SQLException {
        checkXmlDocumentsTable();
        checkXmlRevisionsTable();
    }
    
    /**
     * Check the objects in the xml_documents table
     * @throws SQLException 
     */
    private void checkXmlDocumentsTable() throws SQLException {
        ResultSet rs = runSQL(XML_DOCUMENTS);
        checkIfFilesExist(rs, XML_DOCUMENTS);
    }
    
    /**
     * Check the objects in the xml_documents table
     * @throws SQLException 
     */
    private void checkXmlRevisionsTable() throws SQLException {
        ResultSet rs = runSQL(XML_REVISIONS);
        checkIfFilesExist(rs, XML_REVISIONS);
    }
    
    /**
     * Iterate the result set to check if a docid exists in the file system. If it does exist,
     * skip it; otherwise export the database record on the xml_node table to the file system. 
     * @param result
     * @throws SQLException
     */
    private void checkIfFilesExist(ResultSet result, String tableName) throws SQLException {
        if (result != null) {
            while (result.next()) {
                String docId = null;
                int rev = -1;
                try {
                    docId = result.getString(1);
                    rev = result.getInt(2);
                    long rootNodeId = result.getLong(3);
                    String path = DOCUMENT_DIR + File.separator + docId + "." + rev;
                    File documentFile = new File(path);
                    if (documentFile.exists()) {
                        continue;
                    } else {
                        exportXMLnodesToFile(docId, rev, rootNodeId, tableName);
                    }
                } catch (Exception e) {
                    logMetacat.warn("XMLNodestoFilesChecker.checkIfFilesExist - " 
                                + "can't check if the metadata object " + docId + rev 
                                + " exists in the directory " + DOCUMENT_DIR 
                                + " since " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Export an object form xml_nodes/xml_nodes_revision table to a file.
     * Also check if the system metadata and identifier table have the record. 
     * @param docId
     * @param rev
     * @param rootNodeId
     */
    private void exportXMLnodesToFile(String docId, int rev, long rootNodeId, String tableName) {
        
    }
    
    
    /**
     * Run the sql query against the given table. The query will select docid, revision and the 
     * root node id for the metadata objects  
     * @param tableName  the table where the query be applied to
     * @return  the result set of the query
     * @throws SQLException 
     */
    private ResultSet runSQL(String tableName) throws SQLException {
        ResultSet result = null;
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
          result = pstmt.getResultSet();
        } finally {
           try {
               if(pstmt != null) {
                   pstmt.close();
               }
           } finally {
               DBConnectionPool.returnDBConnection(conn, serialNumber);
           }
        }
        return result;
    }
}
