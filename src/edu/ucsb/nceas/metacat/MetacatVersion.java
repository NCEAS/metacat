package edu.ucsb.nceas.metacat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * @author jones
 *
 * Version represents the current version information for this Metacat instance.
 */
public class MetacatVersion extends Version {
    public MetacatVersion(String versionID) {
    	super(versionID);
    }
	
	/**
	 * Get the current version string for this metacat instance. It is currently
	 * stored in the metacat.properties file.
	 * 
	 * @return a string indicating the version of Metacat running
	 */
	public static String getVersionID() throws PropertyNotFoundException {
		String version = PropertyService.getProperty("application.metacatVersion");
		return version;
	}
    
    /**
	 * Get the version number in an XML document.
	 * 
	 * @return the version wrapped in an XML document
	 */
    public static String getVersionAsXml() throws SQLException {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<version>");
        sb.append(getVersionFromDB());
        sb.append("</version>");
        return sb.toString();
    }
    
    
    /**
     * Get the version number from DB
     * @return  a string of the Metacat version from DB
     * @throws SQLException
     */
    public static String getVersionFromDB() throws SQLException {
        DBConnection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String version = null;
        int serialNumber = -1;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("MetacatVersion.getVersionFromDB()");
            serialNumber = conn.getCheckOutSerialNumber();
            pstmt = conn.prepareStatement("SELECT version FROM db_version WHERE status = ?");
            pstmt.setInt(1, 1);
            rs = pstmt.executeQuery();
            if(rs.next()) {
                version = rs.getString(1);
            }
            pstmt.close();
            rs.close();
        } catch (SQLException e) {
            throw new SQLException("MetacatVersion.getVersionFromDB - sql error: " + e.getMessage());
        }finally {
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
        return version;
    }
}
