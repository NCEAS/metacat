package edu.ucsb.nceas.metacat.dataone.quota;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.bookkeeper.api.Usage;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;

/**
 * Represents a class to manipulate the usages table for the quota service in the postgresql database
 * @author tao
 *
 */
public class QuotaDBManager {
    public static final String TABLE = "quota_usage_events";
    public static final String USAGELOCALID = "usage_local_id";
    public static final String USAGEREMOTEID = "usage_remote_id";
    public static final String QUOTAID = "quota_id";
    public static final String INSTANCEID = "instance_id";
    public static final String QUANTITY = "quantity";
    public static final String STATUS = "status";
    public static final String DATEREPORTED = "date_reported";
    public static final String OBJECT = "object";
    public static final String NODEID = "node_id";
    public static final String QUOTASUBJECT = "quota_subject";
    public static final String QUOTATYPE = "quota_type";
    public static final String REQUESTOR = "requestor";
    
    private static Log logMetacat  = LogFactory.getLog(QuotaDBManager.class);
    
    /**
     * Create a usage record in the usages table with the given date as the reported date
     * If the date is null, it will create a usage record without the reported date
     * @param usage  the usage will be record into the db table
     * @param date  the reported date associated with the usage. If it is null, the reported date will be blank in the table
     * @throws SQLException 
     */
    public static void createUsage(Usage usage, Date date) throws SQLException {
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement stmt = null;
        boolean quotaIdExist = false;
        try {
            dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.createUsage");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String fields = DATEREPORTED + "," + INSTANCEID + "," + QUANTITY + "," + OBJECT + "," + STATUS + "," + NODEID + "," + QUOTASUBJECT + "," + QUOTATYPE + "," + REQUESTOR;
            String values = "?,?,?,?,?,?,?,?,?";
            if (usage.getQuotaId() != null && usage.getQuotaId().intValue() > 0) {
                fields = fields + "," + QUOTAID;
                values = values + ",?";
                quotaIdExist = true;
            } 
            if (usage.getId() != null && usage.getId().intValue() > 0) {
                fields = fields + "," + USAGEREMOTEID;
                values = values + ",?";
            }
            String query = "insert into " + TABLE + " ( " + fields + " ) values ( " + values + " )";
            stmt = dbConn.prepareStatement(query);
            if (date == null) {
                stmt.setTimestamp(1, null);
            } else {
                stmt.setTimestamp(1, new Timestamp(date.getTime()));
            }
            stmt.setString(2, usage.getInstanceId());
            stmt.setDouble(3, usage.getQuantity());
            stmt.setString(4, usage.getObject());
            stmt.setString(5, usage.getStatus());
            stmt.setString(6, usage.getNodeId());
            
            if (usage instanceof LocalUsage ) {
                LocalUsage localUsage = (LocalUsage) usage;
                stmt.setString(7, localUsage.getSubscriber());
                stmt.setString(8, localUsage.getQuotaType());
                stmt.setString(9, localUsage.getRequestor());
                
            } else {
                stmt.setString(7, null);
                stmt.setString(8, null);
                stmt.setString(9, null);
            }
            
            if (quotaIdExist) {
                stmt.setInt(10, usage.getQuotaId());
            }
            if (usage.getId() != null && usage.getId().intValue() > 0) {
                if (quotaIdExist) {
                    stmt.setInt(11, usage.getId());
                } else {
                    stmt.setInt(10, usage.getId());
                }
            }
            logMetacat.debug("QuotaDBManager.createUsage - the create usage query is " + stmt.toString());
            int rows = stmt.executeUpdate();
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            if (stmt != null) {
                stmt.close();
            }
        }
    }
    
    /**
     * Set the reported date and remote id for a given usage local id
     * @param localId  the local id of the usage will be set
     * @param date  the date to report to the bookkeeper server
     * @param remoteId  the remote id will be set
     * @throws SQLException 
     */
    public static void setReportedDateAndRemoteId(int localId, Date date, int remoteId) throws SQLException {
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement stmt = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.setReportedDate");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query = "update " + TABLE + " set " + DATEREPORTED + " = ? " + "," + USAGEREMOTEID + "=?" + " where " + USAGELOCALID + "=?" ;
            stmt = dbConn.prepareStatement(query);
            stmt.setTimestamp(1, new Timestamp(date.getTime()));
            stmt.setInt(2, remoteId);
            stmt.setInt(3, localId);
            logMetacat.debug("QuotaDBManager.setReportedDate - the update query is " + stmt.toString());
            int rows = stmt.executeUpdate();
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            if (stmt != null) {
                stmt.close();
            }
        }
    }
    
    
    /**
     * Get the result set from the table which haven't been reported to the bookkeeper server.
     * The indication is the reported date is null in the usages table
     * @return  the result set which usages which haven't been reported to the bookkeeper server
     * @throws SQLException 
     */
    public static ResultSet getUnReportedUsages() throws SQLException {
        //List<Usage> list = new ArrayList<Usage>();
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.getUnReportedUsages");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query = "select " + USAGELOCALID + ", " + QUOTAID + "," + INSTANCEID + ", " + QUANTITY + "," + OBJECT + "," + STATUS + "," + NODEID + " from " + TABLE + " where " + DATEREPORTED + " is null order by " + USAGELOCALID + " ASC" ;
            stmt = dbConn.prepareStatement(query);
            logMetacat.debug("QuotaDBManager.getUnReportedUsages - the select query is " + query);
            rs = stmt.executeQuery();
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return rs;
    }
    
    /**
     * Find the remote usage id in the local database for the given quota id and instance id
     * @param quotaId  the quota id associated with the usage
     * @param instanceId  the instance id associated with the usage
     * @return  the remote usage id. If it is -1, which means there is no remote usage id found.
     * @throws SQLException
     */
    public static int lookupRemoteUsageId(int quotaId, String instanceId) throws SQLException {
        int remoteId = BookKeeperClient.DEFAULT_REMOTE_USAGE_ID;
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.lookupRemoteUsageId");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query = "select " + USAGEREMOTEID + " from " + TABLE + " where " + QUOTAID + "=" + quotaId + " and " + INSTANCEID + "='" + instanceId + "'";
            stmt = dbConn.prepareStatement(query);
            logMetacat.debug("QuotaDBManager.lookupRemoteUsageId - the select query is " + query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                //make sure it is greater than 0, so it is not empty.
                if (rs.getInt(1) > 0) {
                    remoteId = rs.getInt(1);//It may have multiple rows. They all should have the same value. So we only choose the first one.
                    logMetacat.debug("QuotaDBManager.lookupRemoteUsageId - in the local db, Metacat find the cached remote usage id " + remoteId + " with quota id " + quotaId + " and instance id " + instanceId);
                    break;
                }
            }
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        logMetacat.debug("QuotaDBManager.lookupRemoteUsageId - From the local db, the cached remote usage id is " + remoteId + " with quota id " + quotaId + " and instance id " + instanceId + 
                         ". If it is " + BookKeeperClient.DEFAULT_REMOTE_USAGE_ID + ", which means we don't find one in the local database.");
        return remoteId;
    }
    

}
