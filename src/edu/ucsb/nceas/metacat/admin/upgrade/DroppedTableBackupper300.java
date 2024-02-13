package edu.ucsb.nceas.metacat.admin.upgrade;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;

/**
 * The class to backup dropper tables to the csv files during the upgrade process to 3.0.0
 * @author tao
 *
 */
public class DroppedTableBackupper300 {

    private static final String REPLICATION_HEADER =
                                        "serverid,server,last_checked,replicate,datareplicate,hub";
    private static final String REVISIONS_HEADER = "revisionid,docid,rootnodeid,docname,doctype,"
                                      + "user_owner,user_updated,server_location,rev,date_created,"
                                      + "date_updated,public_access,catalog_id";
    private static final String DOCUMENTS_HEADER = "docid,rootnodeid,docname,doctype,user_owner,"
            + "user_updated,server_location,rev,date_created,date_updated,public_access,catalog_id";
    private static final String XML_NODES_HEADER = "nodeid,nodeindex,nodetype,nodename,nodeprefix,"
                               + "nodedata,parentnodeid,rootnodeid,docid,date_created,date_updated,"
                               + "nodedatanumerical,nodedatadate";
    private static Log logMetacat = LogFactory.getLog(DroppedTableBackupper300.class);
    private String backupPath;

    public enum NodeTableName {
        XML_NODES("xml_nodes"),
        XML_NODES_REVISIONS("xml_nodes_revisions");


        private String tableName;

        /**
         * Constructing an enum with the associated table name
         * @param tableName  the table associated with the enum
         */
        NodeTableName(String tableName) {
            this.tableName = tableName;
        }

        /**
         * Get the table name associated with the enum
         * @return the table name of this enum
         */
        public String getTableName() {
            return this.tableName;
        }
    };

    /**
     * Constructor
     * @param backupPath  the path where the csv backup files will be stored
     * @throws AdminException
     */
    public DroppedTableBackupper300(String backupPath) throws AdminException {
        this.backupPath = backupPath;
        if(this.backupPath == null || this.backupPath.trim().equals("")) {
            throw new AdminException("DroppedTableBackupper300.constructor - "
                                                + "The backup Path cannot be null or blank");
        }
        if (!this.backupPath.endsWith(File.separator)) {
            this.backupPath = this.backupPath + File.separator;
        }
        logMetacat.debug("DroppedTableBackupper300.constructor - the backup path is "
                                                                                + this.backupPath);
    }

    /**
     * This method will backup those tables:
     * xml_replication, xml_revisions and xml_documents
     * @throws AdminException
     */
    public void backup() throws AdminException {
        try {
            backupReplication();
            backupRevisions();
            backupDocuments();
        } catch (IOException | SQLException e) {
            throw new AdminException("DroppedTableBackupper300.backup - failed to backup tables"
                                                    + " since " + e.getMessage());
        }
    }

    /**
     * Backup the records whose root node ids are in the list and in the given table
     * @param table  the table the records locate at
     * @param rootNodeIds  the list of root node id which the records have
     * @throws AdminException
     */
    public void backupNodesTable(NodeTableName table, List<Long> rootNodeIds)
                                                                         throws AdminException {
        try {
            String tableName = table.getTableName();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupPath
                                                                    + getFileName(tableName)))) {
                writer.write(XML_NODES_HEADER);
                if (rootNodeIds != null) {
                    for (int i=0; i< rootNodeIds.size(); i++) {
                        long rootNodeId = rootNodeIds.get(i).longValue();
                        String query = "SELECT " + XML_NODES_HEADER + " FROM " + tableName
                                                                + " WHERE rootnodeid=" + rootNodeId;
                        backupTable(query, writer);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new AdminException("DroppedTableBackupper300.backupNodesTable - failed to backup"
                            + " the table " + table.getTableName() + " since " + e.getMessage());
        }
    }

    /**
     * Backup the xml_replication table to a csv file
     * @throws IOException
     * @throws SQLException
     */
    private void backupReplication() throws IOException, SQLException {
        String tableName = "xml_replication";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupPath
                                                            + getFileName(tableName)))) {
            String query = "SELECT " + REPLICATION_HEADER + " FROM " + tableName;
            writer.write(REPLICATION_HEADER);
            backupTable(query, writer);
        }
    }

    /**
     * Backup the xml_revisions table
     * @throws IOException
     * @throws SQLException
     */
    private void backupRevisions() throws IOException, SQLException {
        String tableName = "xml_revisions";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupPath
                                                            + getFileName(tableName)))) {
            String query = "SELECT " + REVISIONS_HEADER + " FROM " + tableName;
            writer.write(REVISIONS_HEADER);
            backupTable(query, writer);
        }
    }

    /**
     * Backup the xml_documents table
     * @throws IOException
     * @throws SQLException
     */
    private void backupDocuments() throws IOException, SQLException {
        String tableName = "xml_documents";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupPath
                                                            + getFileName(tableName)))) {
            String query = "SELECT " + DOCUMENTS_HEADER + " FROM " + tableName;
            writer.write(DOCUMENTS_HEADER);
            backupTable(query, writer);
        }
    }

    /**
     * Backup the result of the selection query to a csv file.
     * @param query  the query will be run
     * @param writer  the writer will write the content of the query result into a csv file
     * @throws SQLException
     * @throws IOException
     */
    private void backupTable(String query, BufferedWriter writer) throws SQLException, IOException {
        ResultSet result = runQuery(query);
        ResultSetMetaData metaData = result.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (result.next()) {
            writer.newLine();
            StringBuffer line = new StringBuffer("");
            for (int i = 1; i <= columnCount; i++) {
                Object valueObject = result.getObject(i);
                String valueString = "";
                if (valueObject != null) {
                    valueString = valueObject.toString();
                }

                if (valueObject instanceof String) {
                    valueString = "\"" + escapeDoubleQuotes(valueString) + "\"";
                }

                line.append(valueString);
                if (i != columnCount) {
                    line.append(",");
                }
                logMetacat.debug("DroppedTableBackupper300.backupTable - the row string is "
                                + line.toString());
            }
            writer.write(line.toString());
        }
    }


    /**
     * Get the ResultSet object after running the given query
     * @param query  the query will be run
     * @return the ResultSet object
     * @throws SQLException
     */
    protected ResultSet runQuery(String query) throws SQLException {
        ResultSet result = null;
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            dbConn = DBConnectionPool.getDBConnection("DroppedTableBackupper.runQuery");
            serialNumber = dbConn.getCheckOutSerialNumber();
            PreparedStatement stmt = dbConn.prepareStatement(query);
            result = stmt.executeQuery();
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return result;
    }

    /**
     * Get a file name concatenating the base name and current time.
     * @param baseName  the name will be appended by current time
     * @return the file name concatenating the base name and current time.
     */
    private String getFileName(String baseName) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String dateTimeInfo = dateFormat.format(new Date());
        return baseName.concat(String.format("_%s.csv", dateTimeInfo));
    }

    /**
     * Get the backup path
     * @return the backup path
     */
    public String getBackupPath() {
        return this.backupPath;
    }

    /**
     * Escape the double quotes in the given string
     * @param value  the string will be escaped
     * @return the escaped string
     */
    private String escapeDoubleQuotes(String value) {
        return value.replaceAll("\"", "\"\"");
    }
}
