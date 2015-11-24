package edu.ucsb.nceas.metacat.admin.upgrade;
/**
 *  '$RCSfile$'
 *  Copyright: 2012 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matthew Jones
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * Optionally upgrade all of the existing identifiers in the tables to be official
 * DOIs that are assigned by a DOI registration authority.  This class first checks
 * if GUIDs should be converted to DOIs, and if so, updates all relevant
 * tables and resources in Metacat, and then registers each of the newly minted
 * identifiers with the EZID service. You must have an EZID account for this to
 * work properly.
 * 
 * Whether or not identifiers should be converted is determined by several metacat.properties.
 * First, only upgrade if 'guid.assignGUIDs' is true.  Next, for each server registered in 
 * the xml_replication table, only convert the documents belonging to that server if
 * there is a corresponding property that gives the 'shoulder' or 'prefix'' to be used to
 * convert existing IDs to GUIDs.  For example, if servers 1 & 2 are registered in 
 * xml_replication, then only convert the IDs for server 1 to GUIDs if the
 * property 'guid.ezid.doishoulder.1' defines a valid doi shoulder.
 * 
 * NOTE: Metacat v2.0.5 has changed the name of the systemmetadata* tables to accommodate Oracle
 * length restrictions. The table names have been updated in this utility to enable
 * users to run this utility on v2.0.5+. You can only run this version of the utility on 
 * Metacat deployments that have been upgraded to 2.0.5+
 * 
 * @author jones
 */
public class GenerateGlobalIdentifiers implements UpgradeUtilityInterface {

	private static Log log = LogFactory.getLog(GenerateGlobalIdentifiers.class);
    private String driver = null;
    private String url = null;
    private String user = null;
    private String password = null;
//    private String eziduser = null;
//    private String ezidPassword = null;

    /**
     * The main upgrade() procedure, which first upgrades identifiers in the database,
     * then registers these with EZID.
     */
    public boolean upgrade() throws AdminException {
        boolean success = false;
        log.debug("Upgrading identifiers to DOIs");
        
        // Check if assignDOIs is true, and if EZID is configured
        boolean shouldUpgrade = false;
        try {
            shouldUpgrade = new Boolean(PropertyService.getProperty("guid.assignGUIDs")).booleanValue();
            if (shouldUpgrade) {
                log.debug("Upgrading identifiers to DOIs");

                success = updateIdentifierReferences();                
            }

        } catch (PropertyNotFoundException e) {
            // When the guid properties are not found, do nothing
            shouldUpgrade = false;
            success = true;
        }
        		
    	return success;
    }
    
    /**
     * Loop through the servers registered in this instance of Metacat, and for
     * each server determine if we should convert to GUIDs for the documents associated
     * with that server.  If so, convert the identifier table to use a GUID for all
     * documents for that server found in xml_documents and xml_replication.
     * @return true if all conversions succeed
     * @throws AdminException
     */
    private boolean updateIdentifierReferences() throws AdminException {

        log.debug("Updating identifier references...");

        try {
            driver = PropertyService.getProperty("database.driver");
            url = PropertyService.getProperty("database.connectionURI");
            user = PropertyService.getProperty("database.user");
            password = PropertyService.getProperty("database.password");
//            eziduser = PropertyService.getProperty("guid.ezid.username");
//            ezidPassword = PropertyService.getProperty("guid.ezid.password");
            
        } catch (PropertyNotFoundException e) {
            String msg = "PropertyNotFound while trying to convert identifiers: " + e.getMessage();
            log.error(msg, e);
            throw new AdminException(msg);
        }

        // Create a connection, and start a transaction for which FK constraint checking is deferred
        Connection con = openDatabaseConnection();
        deferConstraints(con);
        // Copy the identifier.guid column for later use
        createDuplicateGUIDColumn(con);

        // Look up the maximum server number from the database
        int maxServerId = lookupMaxServers(con);
        
        // Loop across each replica server, checking if it is to have DOIs assigned
        // This is determined by the presence of a shoulder in the configuration file for
        // that server number
        for (int server = 1; server <= maxServerId; server++) {
            
            // Get the server's shoulder, or skip if not found
            try {
                String shoulder = PropertyService.getProperty("guid.ezid.doishoulder." + server);
                log.debug("Processing shoulder for server " + server + ": " + shoulder);

                // Update identifiers table (but only for local documents, not replicated documents)
                convertIdentifierTable(con, server, shoulder);
                
            } catch (PropertyNotFoundException e) {
                // The shoulder was not found for this server, so we just skip it
                log.debug("No shoulder found: " + e.getMessage());
            }
        }
        
        // Convert other tables once, after the identifier table conversion has established the new
        // global identifiers to be used
        // This incudes: xml_access table, including accessfileid field, systemmetadata, 
        // smreplicationpolicy,and smreplicationstatus tables
        convertIdentifierField(con, "xml_access", "guid");
        convertIdentifierField(con, "xml_access", "accessfileid");
        convertIdentifierField(con, "systemmetadata", "guid");
        convertIdentifierField(con, "smreplicationpolicy", "guid");
        convertIdentifierField(con, "smreplicationstatus", "guid");
        convertIdentifierField(con, "smmediatypeproperties", "guid");

        // Now clean up the temporary column we created, but note this makes it much
        // harder to revert our changes
        dropDuplicateGUIDColumn(con);
        
        commitAndCloseConnection(con);

        /*      
        // The following SQL commands will revert the changes made by this script as long as the identifier.old_guid column is intact
        start transaction;
        update smreplicationstatus set guid = id.old_guid FROM identifier id WHERE smreplicationstatus.guid = id.guid;
        update smreplicationpolicy set guid = id.old_guid FROM identifier id WHERE smreplicationpolicy.guid = id.guid;
        update systemmetadata set guid = id.old_guid FROM identifier id WHERE systemmetadata.guid = id.guid;
        update xml_access set accessfileid = id.old_guid FROM identifier id WHERE xml_access.accessfileid = id.guid;
        update xml_access set guid = id.old_guid FROM identifier id WHERE xml_access.guid = id.guid;
        update identifier set guid = old_guid;
        alter table identifier drop column old_guid;
        commit;
        */
        
        return true;
    }

    /**
     * Open a JDBC connection that will be used for all of the subsequent methods that
     * access or modify the database.  All changes are done in one transaction, so need
     * to be done using a single connection.
     * @return the Connection created
     * @throws AdminException
     */
    private Connection openDatabaseConnection() throws AdminException {
        Connection con = null;
            try {
                Driver d = (Driver) Class.forName(driver).newInstance();
                DriverManager.registerDriver(d);
                con = DriverManager.getConnection(url, user, password);
            } catch (InstantiationException e) {
                String msg = "InstantiationException updating creating DB connection: " + e.getMessage();
                log.error(msg, e);
                throw new AdminException(msg);
            } catch (IllegalAccessException e) {
                String msg = "IllegalAccessException updating creating DB connection: " + e.getMessage();
                log.error(msg, e);
                throw new AdminException(msg);
            } catch (ClassNotFoundException e) {
                String msg = "ClassNotFoundException updating creating DB connection: " + e.getMessage();
                log.error(msg, e);
                throw new AdminException(msg);
            } catch (SQLException e) {
                String msg = "SQLException updating creating DB connection: " + e.getMessage();
                log.error(msg, e);
                throw new AdminException(msg);
            }
            
            return con;
    }

    /**
     * Start a transaction on the given connection, and defer all constraints so
     * that changes to foreign keys will not be checked until after all of the 
     * identifiers have been updated in all tables.  Without this, we would get
     * FK constraint violations as we changed the tables.
     * @param con the connection to the database
     * @throws AdminException
     */
    private void deferConstraints(Connection con) throws AdminException {
                
        try {
            con.setAutoCommit(false);
            Statement sqlStatement = con.createStatement();
            boolean hasResults = sqlStatement.execute("SET CONSTRAINTS ALL DEFERRED;");
            log.debug("Constraints Deferred.");
        } catch (SQLException e) {
            String msg = "SQLException while disabling constraints: " + e.getMessage();
            log.error(msg, e);
            throw new AdminException(msg);
        }
    }

    /**
     * Temporarily save the current identifier.guid in a backup column (old_guid), to be used
     * for updating additional tables. 
     * @param con the connection to the database with an open transaction
     * @throws AdminException
     */
    private void createDuplicateGUIDColumn(Connection con) throws AdminException {
        try {
            Statement sqlStatement = con.createStatement();
            boolean hasResults = sqlStatement.execute("ALTER TABLE identifier ADD COLUMN old_guid TEXT;");
            log.debug("old_guid column added.");
            sqlStatement = con.createStatement();
            hasResults = sqlStatement.execute("UPDATE identifier SET old_guid = guid;");
            log.debug("Copied original identifiers to old_guid column.");
        } catch (SQLException e) {
            String msg = "SQLException while duplicating GUID column: " + e.getMessage();
            log.error(msg, e);
            throw new AdminException(msg);
        }        
    }
    
    /**
     * Drop the temporary GUID backup column (old_guid) once upgrading identifiers
     * is complete. 
     * @param con the connection to the database with an open transaction
     * @throws AdminException
     */
    private void dropDuplicateGUIDColumn(Connection con) throws AdminException {
        try {
            Statement sqlStatement = con.createStatement();
            boolean hasResults = sqlStatement.execute("ALTER TABLE identifier DROP COLUMN old_guid;");
            log.debug("old_guid column dropped.");
        } catch (SQLException e) {
            String msg = "SQLException dropping GUID column: " + e.getMessage();
            log.error(msg, e);
            throw new AdminException(msg);
        }        
    }
    
    /**
     * Query the database to determine which servers are registered and can be
     * inspected for potential identifier upgrades.
     * @param con the connection to the database with an open transaction
     * @throws AdminException
     */
    private int lookupMaxServers(Connection con) throws AdminException {
        int maxServers = 0;
        try {
            Statement sqlStatement = con.createStatement();
            boolean hasResults = sqlStatement.execute("SELECT max(serverid) AS maxserverid FROM xml_replication;");
            if (hasResults) {
                ResultSet rs = sqlStatement.getResultSet();
                if (rs.next()) {
                    maxServers = rs.getInt(1);
                    log.debug("MaxServerID: " + maxServers);
                } else {
                    String msg = "Could not determine max serverid; database query cursor had zero rows.";
                    log.error(msg);
                    throw new AdminException(msg);
                }
            } else {
                String msg = "Could not determine max serverid; database query failed to return results.";
                log.error(msg);
                throw new AdminException(msg);
            }
            
        } catch (SQLException e) {
            String msg = "SQLException while looking up serverId: " + e.getMessage();
            log.error(msg, e);
            throw new AdminException(msg);
        }        
        return maxServers;
    }

    /**
     * Convert identifiers to DOI GUIDs in the identifier table for any documents found in
     * xml_documents or xml_revisions that reside on a server for which a DOI shoulder prefix
     * has been assigned.
     * 
     * @param con the connection to the database with an open transaction
     * @param server the id of the server whose documents are to be converted
     * @param shoulder the DOI shoulder to be used for converting
     * @return true if conversion is successful
     * @throws AdminException
     */
    private boolean convertIdentifierTable(Connection con, int server, String shoulder) throws AdminException {
        PreparedStatement pstmt = null;
        try {
            
            String sql_1 =
                "UPDATE identifier " + 
                "SET    guid = p.doi " + 
                "FROM (" +
                       "SELECT  guid, ? || guid as doi " + 
                       "FROM    identifier id2, ";
            String sql_2 =
                " xd " +
                       "WHERE   id2.docid = xd.docid AND id2.rev = xd.rev " +   
                       "AND xd.server_location = ? " +
                     ") p " +
                "WHERE identifier.guid = p.guid;";         

            // Convert identifiers found in xml_documents
            pstmt = con.prepareStatement(sql_1 + "xml_documents" + sql_2);
            pstmt.setString(1, shoulder);
            pstmt.setInt(2, server);
            pstmt.execute();
            
            // Convert identifiers found in xml_revisions
            pstmt = con.prepareStatement(sql_1 + "xml_revisions" + sql_2);
            pstmt.setString(1, shoulder);
            pstmt.setInt(2, server);
            pstmt.execute();
            
            log.debug("Finished shoulder for server " + server + ": " + shoulder);

        } catch (SQLException e) {
            String msg = "SQLException updating identifier table: " + e.getMessage();
            log.error(msg, e);
            throw new AdminException(msg);
        }

        return true;
    }

    /**
     * Convert the given field in the given table to use the new identifier.guid that
     * has been assigned.
     * 
     * @param con the connection to the database with an open transaction
     * @param table the name of the table with identifiers to be converted
     * @param field the name of the field containing identifiers to be converted
     * @return true if conversion was successful
     * @throws AdminException
     */
    private boolean convertIdentifierField(Connection con, String table, String field) throws AdminException {
        PreparedStatement pstmt = null;
        try {

            String sql_1 =
                "UPDATE " + table + " " +
                "SET " + field + " = id.guid " + 
                "FROM identifier id " +
                "WHERE " + table + "."+ field + " = id.old_guid;";
            
            // Convert identifiers found in the given table and field
            pstmt = con.prepareStatement(sql_1);
            pstmt.execute();
                        
            log.debug("Finished converting table " + table + " (" + field +")" );

        } catch (SQLException e) {
            String msg = "SQLException while converting identifier field: " + e.getMessage();
            log.error(msg, e);
            throw new AdminException(msg);
        }

        return true;
    }

    /**
     * Commit the current transaction on the connection, and close the connection.
     * @param con the connection to the database with an open transaction
     * @return true if the connection is successfully closed
     * @throws AdminException
     */
    private boolean commitAndCloseConnection(Connection con) throws AdminException {
        try {
                        
            log.debug("Committing and closing connection.");
            con.commit();
            con.close();
        } catch (SQLException e) {
            String msg = "SQLException while commiting and closing connection: " + e.getMessage();
            log.error(msg, e);
            throw new AdminException(msg);
        } 
        
        return true;
    }

    /**
     * Main method, solely for testing.  Not used in ordinary operation.
     */
    public static void main(String [] ags){

        try {
        	// set up the properties based on the test/deployed configuration of the workspace
        	SortedProperties testProperties = 
				new SortedProperties("test/test.properties");
			testProperties.load();
			String metacatContextDir = testProperties.getProperty("metacat.contextDir");
			PropertyService.getInstance(metacatContextDir + "/WEB-INF");
			// now run it
            GenerateGlobalIdentifiers upgrader = new GenerateGlobalIdentifiers();
	        upgrader.upgrade();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PropertyNotFoundException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (AdminException e) {
            e.printStackTrace();
        }
    }
}
