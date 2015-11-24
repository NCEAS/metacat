package edu.ucsb.nceas.metacat.admin.upgrade;
/**
 *  '$RCSfile$'
 *    Purpose: A Class for upgrading the database to version 1.5
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Saurabh Garg
 *
 *   '$Author: leinfelder $'
 *     '$Date: 2011-03-29 18:23:38 +0000 (Tue, 29 Mar 2011) $'
 * '$Revision: 6025 $'
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


import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.SortedProperties;

public class UpgradeNodeDataDatetime implements UpgradeUtilityInterface {

	protected static Log log = LogFactory.getLog(UpgradeNodeDataDatetime.class);
	
    private String driver = null;
    private String url = null;
    private String user = null;
    private String password = null;

    public boolean upgrade() throws AdminException {
    	String[] tablesToUpdate = {"xml_nodes", "xml_nodes_revisions", "xml_path_index"};
        boolean success = true;
    	// update each table that needs updating
        try {
	        for (String tableName: tablesToUpdate) {
	        	success = success && upgradeSingleTable(tableName);
	        }
    	} catch (Exception e) {
			AdminException ae = new AdminException("Error upgrading tables: " + e.getMessage());
			ae.initCause(e);
			throw ae;
		}
    	
        return success;
    }
    
    /**
     * Utility method to retrieve a connection
     * @return db connection based on metacat configuration
     * @throws AdminException
     */
    private Connection getConnection() throws AdminException {

    	Connection conn = null;
    	try {
	    	// get the properties
			driver = PropertyService.getProperty("database.driver");
		    url = PropertyService.getProperty("database.connectionURI");
		    user = PropertyService.getProperty("database.user");
		    password = PropertyService.getProperty("database.password");
		    
	        // Create a JDBC connection to the database    	
	        Driver d = (Driver) Class.forName(driver).newInstance();
	        DriverManager.registerDriver(d);
	        conn = DriverManager.getConnection(url, user, password);
    	} catch (Exception e) {
			String msg = "Could not get connection: " + e.getMessage();
			log.error(msg);
			AdminException ae = new AdminException(msg);
			ae.initCause(e);
			throw ae;
		} 
    	return conn;
    	
    }
    
    /**
     * Look up the node data values that need converting to datetime fields
     * @param tableName
     * @return
     * @throws AdminException
     */
    private Map<String, Timestamp> getDataValues(String tableName) throws AdminException {
    	
		Map<String, Timestamp> dataValues = new TreeMap<String, Timestamp>();

    	try {
    		
        	Connection conn = getConnection();
	        
	        Statement sqlStatement = null;
	        ResultSet rset = null;
	        String nodeid = null;
            String nodedata = null;
            int count = 0;

			// select the nodes that are not null and look like dates
	        sqlStatement = conn .createStatement();
	        rset = sqlStatement.executeQuery(
	            "SELECT DISTINCT NODEID, NODEDATA "
	            + "FROM " + tableName
	            + " WHERE nodedata IS NOT NULL "
	            + " AND nodedata LIKE '____-__-__%' "
	            );
	
	        count = 0;
	        while (rset.next()) {
	
	            nodeid = rset.getString(1);
	            nodedata = rset.getString(2);
	
	            if (!nodedata.trim().equals("")) {
	            	try {
	            		Calendar dataDateValue = DatatypeConverter.parseDateTime(nodedata);
	                    Timestamp dataTimestamp = new Timestamp(dataDateValue.getTimeInMillis());
	                    dataValues.put(nodeid, dataTimestamp);
	                    log.debug("parsed: " + nodedata);
	                    count++;
	                    if (count%5 == 0) {
	                        log.debug(count + "...");
	                    }	
	            	} catch (Exception e) {
						// probably not a valid date
	            		continue;
					}
	            }              
	        }
	        log.warn("Converting table: " + tableName + " with " + count + " values found.");
	        rset.close();
	        sqlStatement.close();
	        conn.close();
    	} catch (Exception e) {
    		String msg = "Error parsing date time node data: " + e.getMessage();
    		log.error(msg, e);
            throw new AdminException(msg);
        }
    	return dataValues;
    }
    
    /**
     * Upgrade the nodedata fields that have datetime values in them
     * @param tableName the table that should be upgraded
     * @return
     * @throws Exception
     */
    private boolean upgradeSingleTable(String tableName) throws Exception {
    	
    	Map<String, Timestamp> dataValues = getDataValues(tableName);
    	
    	// get the connection
    	Connection conn = getConnection();
    	
    	// prepare a reusable statement
    	PreparedStatement pstmt = conn.prepareStatement(
                "update " + tableName +
                " set nodedatadate = ? " +
                " where nodeid = ? ");
    	
    	// update each value
    	for (String nodeid: dataValues.keySet()) {
    		Timestamp dataTimestamp = dataValues.get(nodeid);			
            pstmt.setTimestamp(1, dataTimestamp);
            pstmt.setInt(2, Integer.valueOf(nodeid));
            log.debug("Updating values: " + pstmt.toString());
            try {
            	pstmt.execute();
            } catch (SQLException sqle) {
				// probably too large a date
            	log.warn("DB error when updating date value, likely can ignore as some errors are to be expected. " + sqle.getMessage());
            	continue;
			}
    	}
    	
    	// clean up
    	if (conn != null) {
    		conn.close();
    	}
    	
    	return true;
    	
    }

    public static void main(String [] ags){

        try {
        	// set up the properties based on the test/deployed configuration of the workspace
        	SortedProperties testProperties = 
				new SortedProperties("test/test.properties");
			testProperties.load();
			String metacatContextDir = testProperties.getProperty("metacat.contextDir");
			PropertyService.getInstance(metacatContextDir + "/WEB-INF");
			// now run it
            UpgradeNodeDataDatetime upgrader = new UpgradeNodeDataDatetime();
	        upgrader.upgrade();
	        
	        // test the converter
            //Calendar date = DatatypeConverter.parseDate("2011-03-17");
            //System.out.println("date:" + DatatypeConverter.printDate(date));
            
        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
