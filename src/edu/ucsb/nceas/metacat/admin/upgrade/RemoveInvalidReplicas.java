package edu.ucsb.nceas.metacat.admin.upgrade;
/**
 *  '$RCSfile$'
 *    Purpose: A Class for upgrading the database to version 1.5
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Saurabh Garg
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


import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * Used for forcibly removing invalid replicas from a target node.
 * These replicas exist from legacy Metacat replication that may have introduced 
 * whitespace differences and entity escaping in the XML representations.
 * Removing these replicas is considered 'safe' because the source node[s] house the 
 * original content that should be re-replicated (by DataONE).
 * @see https://redmine.dataone.org/issues/3539
 * @author leinfelder
 *
 */
public class RemoveInvalidReplicas implements UpgradeUtilityInterface {

	protected static Log log = LogFactory.getLog(RemoveInvalidReplicas.class);
	
    private String driver = null;
    private String url = null;
    private String user = null;
    private String password = null;
    
    private boolean dryRun = false;

	private int serverLocation = 0;
	
    public int getServerLocation() {
		return serverLocation;
	}

	public void setServerLocation(int serverLocation) {
		this.serverLocation = serverLocation;
	}

    public boolean upgrade() throws AdminException {
        
    	boolean success = true;	
    	
    	// do not allow this. ever.
    	if (serverLocation == 1) {
    		throw new AdminException("This is a DESTRUCTIVE action. Cannot remove original objects from home server: " + serverLocation);
    	}
    	
        Connection sqlca = null;
        PreparedStatement pstmt = null;
        
        try {
        	
			log.debug("dryRun: " + dryRun);

        	// get the properties
    		driver = PropertyService.getProperty("database.driver");
    	    url = PropertyService.getProperty("database.connectionURI");
    	    user = PropertyService.getProperty("database.user");
    	    password = PropertyService.getProperty("database.password");
    	    
	        // Create a JDBC connection to the database    
	        Driver d = (Driver) Class.forName(driver).newInstance();
	        DriverManager.registerDriver(d);
	        sqlca = DriverManager.getConnection(url, user, password);
	        sqlca.setAutoCommit(true);       
	        
	        // find the replicas that failed to synch
			List<String> invalidReplicas = new ArrayList<String>();
			pstmt = sqlca.prepareStatement(
					"SELECT distinct guid " +
					"FROM xml_documents xml, identifier id, access_log log " +
					"WHERE id.docid = xml.docid " +
					"AND id.rev = xml.rev " +
					"AND log.docid = id.docid || '.' || id.rev " +
					"AND xml.server_location = ? " +
					"AND log.event = ? " +
					"UNION " +
					"SELECT distinct guid " +
					"FROM xml_revisions xml, identifier id, access_log log " +
					"WHERE id.docid = xml.docid " +
					"AND id.rev = xml.rev " +
					"AND log.docid = id.docid || '.' || id.rev " +
					"AND xml.server_location = ? " +
					"AND log.event = ? ");
			pstmt.setInt(1, serverLocation);
			pstmt.setString(2, Event.SYNCHRONIZATION_FAILED.xmlValue());
			pstmt.setInt(3, serverLocation);
			pstmt.setString(4, Event.SYNCHRONIZATION_FAILED.xmlValue());
			log.debug("Finding invalid (failed replicas with SQL: " + pstmt.toString());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				invalidReplicas.add(rs.getString(1));
			}

			log.debug("invalidReplicas count: " + invalidReplicas.size());

			// prepare statement for removing from identifier table
			pstmt = sqlca.prepareStatement("DELETE FROM identifier WHERE guid = ? ");

			// for using the MN API as the MN itself
			MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
			Session session = new Session();
	        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
	        session.setSubject(subject);
	        
			// remove them from the system in two steps
			for (String identifier: invalidReplicas) {
				
				log.debug("Removing invalid replica: " + identifier);
						
				// using the MN.delete() method first
				Identifier pid = new Identifier();
				pid.setValue(identifier);
				try {
					if (!dryRun) {
						MNodeService.getInstance(request).delete(session, pid);
						log.debug("Deleted invalid replica object: " + pid.getValue());

						// remove SM from database
						IdentifierManager.getInstance().deleteSystemMetadata(identifier);
						log.debug("Deleted invalid replica SystemMetadata for: " + pid.getValue());

						// remove the identifier from the database
						if (IdentifierManager.getInstance().mappingExists(identifier)) {
							String localId = IdentifierManager.getInstance().getLocalId(identifier);
							IdentifierManager.getInstance().removeMapping(identifier, localId);
							log.debug("Removed localId mapping: " + localId);
						} else {
							// remove from the identifier table manually
							pstmt.setString(1, identifier);									
							int count = pstmt.executeUpdate();
							log.debug("Removed identifier entry with SQL: " + pstmt.toString());

							// warn if we saw something unexpected
							if (count <= 0) {
								log.warn("Delete returned unexpected count for pid: " + identifier);
							}
						}
						
						// purge from Hz map
						HazelcastService.getInstance().getSystemMetadataMap().evict(pid);
						log.debug("Evicted identifier from HZ map: " + pid.getValue());

					}
				} catch (Exception e) {
					log.error("Could not delete invalid replica: " + identifier, e);
					continue;
				}
			}
			
        } catch (Exception e) {
        	// TODO Auto-generated catch block
			e.printStackTrace();
        	success = false;
		} finally {
			// clean up
			if (sqlca != null) {
				try {
					sqlca.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
            	
		log.debug("Done removing failed/invalid replicas");

    	return success;
    }

    public static void main(String [] args){

        try {
        	// set up the properties based on the test/deployed configuration of the workspace
        	SortedProperties testProperties = 
				new SortedProperties("test/test.properties");
			testProperties.load();
			String metacatContextDir = testProperties.getProperty("metacat.contextDir");
			PropertyService.getInstance(metacatContextDir + "/WEB-INF");
			
			// now run it
            RemoveInvalidReplicas upgrader = new RemoveInvalidReplicas();
            if (args.length > 0) {
            	String serverLocation = args[0];
            	upgrader.setServerLocation(Integer.parseInt(serverLocation));
            }
            //upgrader.dryRun = true;
	        upgrader.upgrade();
            
        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
