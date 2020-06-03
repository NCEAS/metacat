/**
 *    Purpose: Implements a service for managing a Hazelcast cluster member
 *  Copyright: 2020 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
    public static final String TABLE = "quota_usages";
    public static final String USAGEID = "usage_id";
    public static final String QUOTAID = "quota_id";
    public static final String INSTANCEID = "instance_id";
    public static final String QUANTITY = "quantity";
    public static final String DATEREPORTED = "date_reported";
    
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
        try {
            dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.createUsage");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String fields = QUOTAID + "," + INSTANCEID + "," + QUANTITY;
            String values = "?,?,?";
            if (date != null) {
                fields = fields + "," + DATEREPORTED;
                values = values + ",?";
            } 
            String query = "insert into " + TABLE + " ( " + fields + " ) values ( " + values + " )";
            stmt = dbConn.prepareStatement(query);
            stmt.setInt(1, usage.getQuotaId());
            stmt.setString(2, usage.getInstanceId());
            stmt.setDouble(3, usage.getQuantity());
            if (date != null) {
                stmt.setTimestamp(4, new Timestamp(date.getTime()));
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
     * Set the reported date for a given usage
     * @param usageId  the id of the usage will be set
     * @param date  the date to report to the bookkeeper server
     * @throws SQLException 
     */
    public static void setReportedDate(int usageId, Date date) throws SQLException {
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement stmt = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.setReportedDate");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query = "update " + TABLE + " set " + DATEREPORTED + " = ? " + " where " + USAGEID + "=?" ;
            stmt = dbConn.prepareStatement(query);
            stmt.setTimestamp(1, new Timestamp(date.getTime()));
            stmt.setInt(2, usageId);
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
     * Get the list of usages which haven't been reprorted to the bookkeeper server.
     * The indication is the reported date is null in the usages table
     * @return  the list of usages which usages which haven't been reprorted to the bookkeeper server
     * @throws SQLException 
     */
    public static List<Usage> getUnReportedUsages() throws SQLException {
        List<Usage> list = new ArrayList<Usage>();
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.getUnReportedUsages");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query = "select " + USAGEID + ", " + QUOTAID + "," + INSTANCEID + ", " + QUANTITY + " from " + TABLE + " where " + DATEREPORTED + " is null" ;
            stmt = dbConn.prepareStatement(query);
            logMetacat.debug("QuotaDBManager.getUnReportedUsages - the update query is " + stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                Usage usage = new Usage();
                usage.setId(rs.getInt(1));
                usage.setQuotaId(rs.getInt(2));
                usage.setInstanceId(rs.getString(3));
                usage.setQuantity(rs.getDouble(4));
                list.add(usage);
            }
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
        return list;
    }
    

}