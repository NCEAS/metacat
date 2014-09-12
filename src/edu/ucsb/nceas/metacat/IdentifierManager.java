/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: jones $'
 *     '$Date: 2010-02-03 17:58:12 -0900 (Wed, 03 Feb 2010) $'
 * '$Revision: 5211 $'
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

package edu.ucsb.nceas.metacat;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dataone.client.ObjectFormatCache;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;

import edu.ucsb.nceas.metacat.accesscontrol.XMLAccessAccess;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.utilities.access.XMLAccessDAO;

/**
 * Manage the relationship between Metacat local identifiers (LocalIDs) that are
 * codified as the (docid, rev) pair with globally unique string identifiers
 * (GUIDs) that are opaque strings.  This class provides methods to manage these
 * identifiers, and to search for and look up LocalIDs based on their GUID and
 * vice versa. IdentifierManager is a singleton.
 * 
 * @author Matthew Jones
 */
public class IdentifierManager {
    
    public static final String TYPE_SYSTEM_METADATA = "systemmetadata";
    public static final String TYPE_IDENTIFIER = "identifier";
  
    /**
     * The single instance of the manager that is always returned.
     */
    private static IdentifierManager self = null;
    private Logger logMetacat = Logger.getLogger(IdentifierManager.class);

    /**
     * A private constructor that initializes the class when getInstance() is
     * called.
     */
    private IdentifierManager() {}

    /**
     * Return the single instance of the manager after initializing it if it
     * wasn't previously initialized.
     * 
     * @return the single IdentifierManager instance
     */
    public static IdentifierManager getInstance()
    {
        if (self == null) {
            self = new IdentifierManager();
        }
        return self;
    }
    
    public SystemMetadata asSystemMetadata(Date dateUploaded, String rightsHolder,
            String checksum, String checksumAlgorithm, String originMemberNode,
            String authoritativeMemberNode, Date dateModified, String submitter, 
            String guid, String fmtidStr, BigInteger size, BigInteger serialVersion) {
        SystemMetadata sysMeta = new SystemMetadata();

        Identifier sysMetaId = new Identifier();
        sysMetaId.setValue(guid);
        sysMeta.setIdentifier(sysMetaId);
        sysMeta.setDateUploaded(dateUploaded);
        Subject rightsHolderSubject = new Subject();
        rightsHolderSubject.setValue(rightsHolder);
        sysMeta.setRightsHolder(rightsHolderSubject);
        Checksum checksumObject = new Checksum();
        checksumObject.setValue(checksum);
        checksumObject.setAlgorithm(checksumAlgorithm);
        sysMeta.setChecksum(checksumObject);
        NodeReference omn = new NodeReference();
        omn.setValue(originMemberNode);
        sysMeta.setOriginMemberNode(omn);
        NodeReference amn = new NodeReference();
        amn.setValue(authoritativeMemberNode);
        sysMeta.setAuthoritativeMemberNode(amn);
        sysMeta.setDateSysMetadataModified(dateModified);
        Subject submitterSubject = new Subject();
        submitterSubject.setValue(submitter);
        sysMeta.setSubmitter(submitterSubject);
        ObjectFormatIdentifier fmtid = null;
        try {
        	ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        	formatId.setValue(fmtidStr);
        	fmtid = ObjectFormatCache.getInstance().getFormat(formatId).getFormatId();
        	sysMeta.setFormatId(fmtid);
        	
        } catch (BaseException nfe) {
            logMetacat.error("The objectFormat " + fmtidStr +
          	" is not registered. Setting the default format id.");
            fmtid = new ObjectFormatIdentifier();
            fmtid.setValue("application/octet-stream");
            sysMeta.setFormatId(fmtid);
            
        }
        sysMeta.setSize(size);
        sysMeta.setSerialVersion(serialVersion);
        
        return sysMeta;
    }
    
    /**
     * return a hash of all of the info that is in the systemmetadata table
     * @param localId
     * @return
     */
    public Hashtable<String, String> getSystemMetadataInfo(String localId)
    throws McdbDocNotFoundException
    {
        try
        {
            AccessionNumber acc = new AccessionNumber(localId, "NONE");
            localId = acc.getDocid();
        }
        catch(Exception e)
        {
            //do nothing. just try the localId as it is
        }
        Hashtable<String, String> h = new Hashtable<String, String>();
        String sql = "select guid, date_uploaded, rights_holder, checksum, checksum_algorithm, " +
          "origin_member_node, authoritive_member_node, date_modified, submitter, object_format, size " +
          "from systemmetadata where docid = ?";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try 
        {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getDocumentInfo");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            stmt.setString(1, localId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) 
            {
                String guid = rs.getString(1);
                Timestamp dateUploaded = rs.getTimestamp(2);
                String rightsHolder = rs.getString(3);
                String checksum = rs.getString(4);
                String checksumAlgorithm = rs.getString(5);
                String originMemberNode = rs.getString(6);
                String authoritativeMemberNode = rs.getString(7);
                Timestamp dateModified = rs.getTimestamp(8);
                String submitter = rs.getString(9);
                String objectFormat = rs.getString(10);
                long size = new Long(rs.getString(11)).longValue();
                
                h.put("guid", guid);
                h.put("date_uploaded", new Long(dateUploaded.getTime()).toString());
                h.put("rights_holder", rightsHolder);
                h.put("checksum", checksum);
                h.put("checksum_algorithm", checksumAlgorithm);
                h.put("origin_member_node", originMemberNode);
                h.put("authoritative_member_node", authoritativeMemberNode);
                h.put("date_modified", new Long(dateModified.getTime()).toString());
                h.put("submitter", submitter);
                h.put("object_format", objectFormat);
                h.put("size", new Long(size).toString());
                
                stmt.close();
            } 
            else
            {
                stmt.close();
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
                throw new McdbDocNotFoundException("2Could not find document " + localId);
            }
            
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
            logMetacat.error("Error while getting system metadata info for localid " + localId + " : "  
                    + e.getMessage());
        } 
        finally 
        {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return h;
    }
    
    /**
     * return a hash of all of the info that is in the systemmetadata table
     * @param guid
     * @return
     * @throws McdbDocNotFoundException 
     */
    public SystemMetadata getSystemMetadata(String guid)
    	throws McdbDocNotFoundException
    {
        
        SystemMetadata sysMeta = new SystemMetadata();
        String sql = "select guid, date_uploaded, rights_holder, checksum, checksum_algorithm, " +
          "origin_member_node, authoritive_member_node, date_modified, submitter, object_format, size, " +
          "replication_allowed, number_replicas, obsoletes, obsoleted_by, serial_version, archived " +
          "from systemmetadata where guid = ?";
        DBConnection dbConn = null;
        int serialNumber = -1;
        Boolean replicationAllowed = new Boolean(false);
        BigInteger numberOfReplicas = new BigInteger("-1");
        BigInteger serialVersion = new BigInteger("-1");
        Boolean archived = new Boolean(false);

        try 
        {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getSystemMetadata");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            stmt.setString(1, guid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) 
            {
                Timestamp dateUploaded = rs.getTimestamp(2);
                String rightsHolder = rs.getString(3);
                String checksum = rs.getString(4);
                String checksumAlgorithm = rs.getString(5);
                String originMemberNode = rs.getString(6);
                String authoritativeMemberNode = rs.getString(7);
                Timestamp dateModified = rs.getTimestamp(8);
                String submitter = rs.getString(9);
                String fmtidStr = rs.getString(10);
                BigInteger size = new BigInteger(rs.getString(11));
                replicationAllowed = new Boolean(rs.getBoolean(12));
                numberOfReplicas = new BigInteger(rs.getString(13));
                String obsoletes = rs.getString(14);
                String obsoletedBy = rs.getString(15);
                serialVersion = new BigInteger(rs.getString(16));
                archived = new Boolean(rs.getBoolean(17));

                Identifier sysMetaId = new Identifier();
                sysMetaId.setValue(guid);
                sysMeta.setIdentifier(sysMetaId);
                sysMeta.setSerialVersion(serialVersion);
                sysMeta.setDateUploaded(dateUploaded);
                Subject rightsHolderSubject = new Subject();
                rightsHolderSubject.setValue(rightsHolder);
                sysMeta.setRightsHolder(rightsHolderSubject);
                Checksum checksumObject = new Checksum();
                checksumObject.setValue(checksum);
                checksumObject.setAlgorithm(checksumAlgorithm);
                sysMeta.setChecksum(checksumObject);
                if (originMemberNode != null) {
	                NodeReference omn = new NodeReference();
	                omn.setValue(originMemberNode);
	                sysMeta.setOriginMemberNode(omn);
                }
                if (authoritativeMemberNode != null) {
	                NodeReference amn = new NodeReference();
	                amn.setValue(authoritativeMemberNode);
	                sysMeta.setAuthoritativeMemberNode(amn);
                }
                sysMeta.setDateSysMetadataModified(dateModified);
                if (submitter != null) {
	                Subject submitterSubject = new Subject();
	                submitterSubject.setValue(submitter);
	                sysMeta.setSubmitter(submitterSubject);
                }
                ObjectFormatIdentifier fmtid = new ObjectFormatIdentifier();
                fmtid.setValue(fmtidStr);
            	sysMeta.setFormatId(fmtid);
                sysMeta.setSize(size);
                if (obsoletes != null) {
	                Identifier obsoletesId = new Identifier();
	                obsoletesId.setValue(obsoletes);
	                sysMeta.setObsoletes(obsoletesId);
                }
                if (obsoletedBy != null) {
		            Identifier obsoletedById = new Identifier();
		            obsoletedById.setValue(obsoletedBy);
		            sysMeta.setObsoletedBy(obsoletedById);
                }
                sysMeta.setArchived(archived);
                stmt.close();
            } 
            else
            {
                stmt.close();
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
                throw new McdbDocNotFoundException("Could not find " + guid);
            }
            
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
            logMetacat.error("Error while getting system metadata for guid " + guid + " : "  
                    + e.getMessage());
        } 
        finally 
        {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }

        // populate the replication policy
        ReplicationPolicy replicationPolicy = new ReplicationPolicy();
        if ( numberOfReplicas != null  && numberOfReplicas.intValue() != -1 ) {
            replicationPolicy.setNumberReplicas(numberOfReplicas.intValue());
            
        }
        
        if ( replicationAllowed != null ) {
            replicationPolicy.setReplicationAllowed(replicationAllowed);
            
        }
        replicationPolicy.setBlockedMemberNodeList(getReplicationPolicy(guid, "blocked"));
        replicationPolicy.setPreferredMemberNodeList(getReplicationPolicy(guid, "preferred"));
		    sysMeta.setReplicationPolicy(replicationPolicy);
		
		    // look up replication status
		    sysMeta.setReplicaList(getReplicationStatus(guid));
		
		    // look up access policy
		    try {
		    	sysMeta.setAccessPolicy(getAccessPolicy(guid));
		    } catch (AccessException e) {
		    	throw new McdbDocNotFoundException(e);
		    }
        
        return sysMeta;
    }
    
    
    private List<NodeReference> getReplicationPolicy(String guid, String policy)
		throws McdbDocNotFoundException {
		
		List<NodeReference> nodes = new ArrayList<NodeReference>();
		String sql = "select guid, policy, member_node " +
			"from smReplicationPolicy where guid = ? and policy = ?";
	    DBConnection dbConn = null;
	    int serialNumber = -1;
	    try {
	        // Get a database connection from the pool
	        dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getReplicationPolicy");
	        serialNumber = dbConn.getCheckOutSerialNumber();
	
	        // Execute the statement
	        PreparedStatement stmt = dbConn.prepareStatement(sql);
	        stmt.setString(1, guid);
	        stmt.setString(2, policy);
	        ResultSet rs = stmt.executeQuery();
	        while (rs.next()) 
	        {
	            String memberNode = rs.getString(3);
	            NodeReference node = new NodeReference();
	            node.setValue(memberNode);
	            nodes.add(node);
	        
	        } 
	        stmt.close();
	        
	    } catch (SQLException e) {
	        logMetacat.error("Error while getting system metadata replication policy for guid " + guid, e);
	    } 
	    finally {
	        // Return database connection to the pool
	        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
	    }
	    
	    return nodes;
	}
    
    private List<Replica> getReplicationStatus(String guid) throws McdbDocNotFoundException {
		
		List<Replica> replicas = new ArrayList<Replica>();
		String sql = "select guid, member_node, status, date_verified " +
			"from smReplicationStatus where guid = ?";
	    DBConnection dbConn = null;
	    int serialNumber = -1;
	    try {
	        // Get a database connection from the pool
	        dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getReplicas");
	        serialNumber = dbConn.getCheckOutSerialNumber();
	
	        // Execute the statement
	        PreparedStatement stmt = dbConn.prepareStatement(sql);
	        stmt.setString(1, guid);
	        ResultSet rs = stmt.executeQuery();
	        while (rs.next()) 
	        {
	            String memberNode = rs.getString(2);
	            String status = rs.getString(3);
	            java.sql.Timestamp verified = rs.getTimestamp(4);
	            
	            Replica replica = new Replica();	            
	            NodeReference node = new NodeReference();
	            node.setValue(memberNode);
	            replica.setReplicaMemberNode(node);
	            replica.setReplicationStatus(ReplicationStatus.valueOf(status));
	            replica.setReplicaVerified(new Date(verified.getTime()));
	            replicas.add(replica);
	        } 
	        stmt.close();
	        
	    } catch (SQLException e) {
	        logMetacat.error("Error while getting system metadata replication policy for guid " + guid, e);
	    } 
	    finally {
	        // Return database connection to the pool
	        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
	    }
	    
	    return replicas;
	}
    
    
    /**
     * return the newest rev for a given localId
     * @param localId
     * @return
     */
    public int getLatestRevForLocalId(String localId)
        throws McdbDocNotFoundException
    {
        try
        {
            AccessionNumber acc = new AccessionNumber(localId, "NONE");
            localId = acc.getDocid();
        }
        catch(Exception e)
        {
            //do nothing. just try the localId as it is
        }
        int rev = 0;
        String sql = "select rev from xml_documents where docid like ? ";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try 
        {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getLatestRevForLocalId");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            stmt.setString(1, localId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) 
            {
                rev = rs.getInt(1);
                stmt.close();
            } 
            else
            {
                stmt.close();
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
                throw new McdbDocNotFoundException("While trying to get the latest rev, could not find document " + localId);
            }
        } 
        catch (SQLException e) 
        {
            logMetacat.error("Error while looking up the guid: " 
                    + e.getMessage());
        } 
        finally 
        {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return rev;
    }
    
    /**
     * return all local ids in the object store that do not have associated
     * system metadata
     */
    public List<String> getLocalIdsWithNoSystemMetadata(boolean includeRevisions, int serverLocation)
    {
        Vector<String> ids = new Vector<String>();
        String sql = "select docid, rev from xml_documents " +
        		"where docid not in " +
        		"(select docid from identifier where guid in (select guid from systemmetadata))";
        if (serverLocation > 0) {
        	sql = sql + " and server_location = ? ";
        }
        
        String revisionSql = "select docid, rev from xml_revisions " +
				"where docid not in " +
				"(select docid from identifier where guid in (select guid from systemmetadata))";
        if (serverLocation > 0) {
        	revisionSql = revisionSql + " and server_location = ? ";
        }
        
        if (includeRevisions) {
        	sql = sql + " UNION ALL " + revisionSql;
        }
        
        DBConnection dbConn = null;
        int serialNumber = -1;
        try 
        {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getLocalIdsWithNoSystemMetadata");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            // set params based on what we have in the query string
            if (serverLocation > 0) {
            	stmt.setInt(1, serverLocation);
            	if (includeRevisions) {
            		stmt.setInt(2, serverLocation);
            	}
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) 
            {
                String localid = rs.getString(1);
                String rev = rs.getString(2);
                localid += "." + rev;
                logMetacat.debug("id to add SM for: " + localid);
                ids.add(localid);
            } 
            stmt.close();
        } 
        catch (SQLException e) 
        {
            logMetacat.error("Error while looking up the guid: " 
                    + e.getMessage());
        } 
        finally 
        {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        
        return ids;
    }
    
    /**
     * return a listing of all local ids in the object store
     * @return a list of all local ids in metacat
     */
    public List<String> getAllLocalIds()
    // seems to be an unnecessary and restrictive throw -rnahf 13-Sep-2011
    //    throws Exception
    {
        Vector<String> ids = new Vector<String>();
        String sql = "select docid from xml_documents";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try 
        {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getAllLocalIds");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) 
            {
                String localid = rs.getString(1);
                ids.add(localid);
            } 
            stmt.close();
        } 
        catch (SQLException e) 
        {
            logMetacat.error("Error while looking up the guid: " 
                    + e.getMessage());
        } 
        finally 
        {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return ids;
    }
    
    
    /**
     * return a listing of all guids in the object store
     * @return a list of all GUIDs in metacat
     */
    public List<String> getAllSystemMetadataGUIDs()
    {
        Vector<String> guids = new Vector<String>();
        String sql = "select guid from systemmetadata";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try 
        {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getAllGUIDs");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) 
            {
                String guid = rs.getString(1);
                guids.add(guid);
            } 
            stmt.close();
        } 
        catch (SQLException e) 
        {
            logMetacat.error("Error while retrieving the guid: " 
                    + e.getMessage());
        } 
        finally 
        {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return guids;
    }
    
    
    
    /**
     * returns a list of system metadata-only guids since the given date
     * @return a list of system ids in metacat that do not correspond to objects
     * TODO: need to check which server they are on
     */
    public List<String> getUpdatedSystemMetadataIds(Date since)
       throws Exception
    {
        List<String> ids = new Vector<String>();
        String sql = 
        	"select guid from " + TYPE_SYSTEM_METADATA +
        	" where guid not in " +
        	" (select guid from " + TYPE_IDENTIFIER + ") " +
        	" and date_modified > ?";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try 
        {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getUpdatedSystemMetadataIds");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            stmt.setDate(1, new java.sql.Date(since.getTime()));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) 
            {
                String guid = rs.getString(1);
                ids.add(guid);
            } 
            stmt.close();
        } 
        catch (SQLException e) 
        {
            logMetacat.error("Error while looking up the updated guids: " 
                    + e.getMessage());
        } 
        finally 
        {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return ids;
    }
    
    /**
     * returns a list of system metadata-only guids since the given date
     * @return a list of system ids in metacat that do not correspond to objects
     * TODO: need to check which server they are on
     */
    public Date getLastModifiedDate() throws Exception {
        Date maxDate = null;

        List<String> ids = new Vector<String>();
        String sql = 
        	"select max(date_modified) from " + TYPE_SYSTEM_METADATA;
        DBConnection dbConn = null;
        int serialNumber = -1;
        try 
        {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getLastModifiedDate");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
            	maxDate = rs.getDate(1);
            } 
            stmt.close();
        } 
        catch (SQLException e) 
        {
            logMetacat.error("Error while looking up the latest update date: " 
                    + e.getMessage());
        } 
        finally 
        {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return maxDate;
    }

    
    /**
     * Determine if an identifier exists already, returning true if so.
     * NOTE: looks in the identifier and system metadata table for a match
     * (in that order)
     * 
     * @param guid the global identifier to look up
     * @return boolean true if the identifier exists
     */
    public boolean identifierExists(String guid)
    {
        boolean idExists = false;
        try {
            String id = getLocalId(guid);
            if (id != null) {
                idExists = true;
            }
        } catch (McdbDocNotFoundException e) {
        	// try system metadata only
        	try {
        		idExists = systemMetadataExists(guid);
            } catch (Exception e2) {
            	idExists = false;
            }
        }
        return idExists;
    }
    
    /**
     * Determine if an identifier mapping exists already, 
     * returning true if so.
     * 
     * @param guid the global identifier to look up
     * @return boolean true if the identifier exists
     */
    public boolean mappingExists(String guid)
    {
        boolean idExists = false;
        try {
            String id = getLocalId(guid);
            if (id != null) {
                idExists = true;
            }
        } catch (McdbDocNotFoundException e) {
        	// nope!
        }
        return idExists;
    }
    
    /**
     * 
     * @param guid
     * @param rev
     * @return
     */
    public String generateLocalId(String guid, int rev)
    {
        return generateLocalId(guid, rev, false);
    }

    /**
     * Given a global identifier (guid), create a suitable local identifier that
     * follows Metacat's docid semantics and format (scope.id.rev), and create
     * a mapping between these two identifiers.  This effectively reserves both
     * the global and the local identifier, as they will now be present in the
     * identifier mapping table.  
     * 
     * REMOVED feature: If the incoming guid has the syntax of a
     * Metacat docid (scope.id.rev), then simply use it.
     * WHY: because "test.1.001" becomes "test.1.1" which is not correct for DataONE
     * identifier use (those revision numbers are just chartacters and should not be interpreted)
     * 
     * @param guid the global string identifier
     * @param rev the revision number to be used in the localId
     * @return String containing the localId to be used for Metacat operations
     */
    public String generateLocalId(String guid, int rev, boolean isSystemMetadata) 
    {
        String localId = "";
        boolean conformsToDocidFormat = false;
        
        // BRL -- do not allow Metacat-conforming IDs to be used:
        // test.1.001 becomes test.1.1 which is NOT correct for DataONE identifiers
        // Check if the guid passed in is already in docid (scope.id.rev) format
//        try {
//            AccessionNumber acc = new AccessionNumber(guid, "NONE");
//            if (new Integer(acc.getRev()).intValue() > 0) {
//                conformsToDocidFormat = true;
//            }
//        } catch (NumberFormatException e) {
//            // No action needed, simply detecting invalid AccessionNumbers
//        } catch (AccessionNumberException e) {
//            // No action needed, simply detecting invalid AccessionNumbers
//        } catch (SQLException e) {
//            // No action needed, simply detecting invalid AccessionNumbers
//        }
        
        if (conformsToDocidFormat) {
            // if it conforms, use it for both guid and localId
            localId = guid;
        } else {
            // if not, then generate a new unique localId
            localId = DocumentUtil.generateDocumentId(rev);
        }
        
        // Register this new pair in the identifier mapping table
        logMetacat.debug("creating mapping in generateLocalId");
        if(!isSystemMetadata)
        { //don't do this if we're generating for system metadata
            createMapping(guid, localId);
        }
        
        return localId;
    }
    
    /**
     * given a local identifer, look up the guid.  Throw McdbDocNotFoundException
     * if the docid, rev is not found in the identifiers or systemmetadata tables
     *
     * @param docid the docid to look up
     * @param rev the revision of the docid to look up
     * @return String containing the mapped guid
     * @throws McdbDocNotFoundException if the docid, rev is not found
     */
    public String getGUID(String docid, int rev)
      throws McdbDocNotFoundException
    {
        logMetacat.debug("getting guid for " + docid);
        String query = "select guid from identifier where docid = ? and rev = ?";
        String guid = null;
        
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getGUID");
            serialNumber = dbConn.getCheckOutSerialNumber();
            
            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(query);
            stmt.setString(1, docid);
            stmt.setInt(2, rev);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) 
            {
                guid = rs.getString(1);
            } 
            else
            {
            	throw new McdbDocNotFoundException("No guid registered for docid " + docid + "." + rev);
            }
            
        } catch (SQLException e) {
            logMetacat.error("Error while looking up the guid: " 
                    + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        
        return guid;
    }
    
    public boolean systemMetadataExists(String guid) {
		logMetacat.debug("looking up system metadata for guid " + guid);
		boolean exists = false;
		String query = "select guid from systemmetadata where guid = ?";

		DBConnection dbConn = null;
		int serialNumber = -1;
		try {
			// Get a database connection from the pool
			dbConn = DBConnectionPool.getDBConnection("IdentifierManager.systemMetadataExisits");
			serialNumber = dbConn.getCheckOutSerialNumber();

			// Execute the insert statement
			PreparedStatement stmt = dbConn.prepareStatement(query);
			stmt.setString(1, guid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				exists = true;
			}

		} catch (SQLException e) {
			logMetacat.error("Error while looking up the system metadata: "
					+ e.getMessage());
		} finally {
			// Return database connection to the pool
			DBConnectionPool.returnDBConnection(dbConn, serialNumber);
		}

		return exists;
	}
    
    /**
     * creates a system metadata mapping and adds additional fields from sysmeta
     * to the table for quick searching.
     * 
     * @param guid the id to insert
     * @param localId the systemMetadata object to get the local id for
     * @throws McdbDocNotFoundException 
     * @throws SQLException 
     * @throws InvalidSystemMetadata 
     */
    public void insertOrUpdateSystemMetadata(SystemMetadata sysmeta) 
        throws McdbDocNotFoundException, SQLException, InvalidSystemMetadata {
    	String guid = sysmeta.getIdentifier().getValue();
    	
    	 // Get a database connection from the pool
        DBConnection dbConn = DBConnectionPool.getDBConnection("IdentifierManager.insertSystemMetadata");
        int serialNumber = dbConn.getCheckOutSerialNumber();
        
        try {
        	// use a single transaction for it all
        	dbConn.setAutoCommit(false);
        	
	    	// insert the record if needed
        	if (!IdentifierManager.getInstance().systemMetadataExists(guid)) {
    	        insertSystemMetadata(guid, dbConn);
			}
	        // update with the values
	        updateSystemMetadata(sysmeta, dbConn);
	        
	        // commit if we got here with no errors
	        dbConn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            logMetacat.error("Error while creating " + TYPE_SYSTEM_METADATA + " record: " + guid, e );
            dbConn.rollback();
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        
        
    }
        
    
    /**
     * update a mapping
     * @param guid
     * @param localId
     */
    public void updateMapping(String guid, String localId)
    {
    	
        logMetacat.debug("$$$$$$$$$$$$$$ updating mapping table");
        int serialNumber = -1;
        DBConnection dbConn = null;
        try {
            // Parse the localId into scope and rev parts
            AccessionNumber acc = new AccessionNumber(localId, "NOACTION");
            String docid = acc.getDocid();
            int rev = 1;
            if(acc.getRev() != null)
            {
              rev = (new Integer(acc.getRev()).intValue());
            }

            // Get a database connection from the pool
            dbConn = 
                DBConnectionPool.getDBConnection("IdentifierManager.updateMapping");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the update statement
            String query = "update " + TYPE_IDENTIFIER + " set (docid, rev) = (?, ?) where guid = ?";
            PreparedStatement stmt = dbConn.prepareStatement(query);
            stmt.setString(1, docid);
            stmt.setInt(2, rev);
            stmt.setString(3, guid);
            int rows = stmt.executeUpdate();

            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            logMetacat.error("SQL error while updating a mapping identifier: " 
                    + e.getMessage());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            logMetacat.error("NumberFormat error while updating a mapping identifier: " 
                    + e.getMessage());
        } catch (AccessionNumberException e) {
            e.printStackTrace();
            logMetacat.error("AccessionNumber error while updating a mapping identifier: " 
                    + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        logMetacat.debug("done updating mapping");
    }
        
    private void updateSystemMetadataFields(long dateUploaded, String rightsHolder,
        String checksum, String checksumAlgorithm, String originMemberNode, 
        String authoritativeMemberNode, long modifiedDate, String submitter, 
        String guid, String objectFormat, BigInteger size, boolean archived,
        boolean replicationAllowed, int numberReplicas, String obsoletes,
        String obsoletedBy, BigInteger serialVersion, DBConnection dbConn) throws SQLException  {
  
        // Execute the insert statement
        String query = "update " + TYPE_SYSTEM_METADATA + 
            " set (date_uploaded, rights_holder, checksum, checksum_algorithm, " +
            "origin_member_node, authoritive_member_node, date_modified, " +
            "submitter, object_format, size, archived, replication_allowed, number_replicas, " +
            "obsoletes, obsoleted_by, serial_version) " +
            "= (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) where guid = ?";
        PreparedStatement stmt = dbConn.prepareStatement(query);
        
        //data values
        stmt.setTimestamp(1, new java.sql.Timestamp(dateUploaded));
        stmt.setString(2, rightsHolder);
        stmt.setString(3, checksum);
        stmt.setString(4, checksumAlgorithm);
        stmt.setString(5, originMemberNode);
        stmt.setString(6, authoritativeMemberNode);
        stmt.setTimestamp(7, new java.sql.Timestamp(modifiedDate));
        stmt.setString(8, submitter);
        stmt.setString(9, objectFormat);
        stmt.setString(10, size.toString());
        stmt.setBoolean(11, archived);
        stmt.setBoolean(12, replicationAllowed);
        stmt.setInt(13, numberReplicas);
        stmt.setString(14, obsoletes);
        stmt.setString(15, obsoletedBy);
        stmt.setString(16, serialVersion.toString());

        //where clause
        stmt.setString(17, guid);
        logMetacat.debug("stmt: " + stmt.toString());
        //execute
        int rows = stmt.executeUpdate();

        stmt.close();
               
    }
    
    private void insertReplicationPolicy(String guid, String policy, List<String> memberNodes, DBConnection dbConn) throws SQLException
    {
           
        // remove existing values first
        String delete = "delete from smReplicationPolicy " + 
        "where guid = ? and policy = ?";
        PreparedStatement stmt = dbConn.prepareStatement(delete);
        //data values
        stmt.setString(1, guid);
        stmt.setString(2, policy);
        //execute
        int deletedCount = stmt.executeUpdate();
        stmt.close();
        
        for (String memberNode: memberNodes) {
            // Execute the insert statement
            String insert = "insert into smReplicationPolicy " + 
                "(guid, policy, member_node) " +
                "values (?, ?, ?)";
            PreparedStatement insertStatement = dbConn.prepareStatement(insert);
            
            //data values
            insertStatement.setString(1, guid);
            insertStatement.setString(2, policy);
            insertStatement.setString(3, memberNode);
            
            logMetacat.debug("smReplicationPolicy sql: " + insertStatement.toString());

            //execute
            int rows = insertStatement.executeUpdate();
            insertStatement.close();
        }
        
    }
    
    private void insertReplicationStatus(String guid, List<Replica> replicas, DBConnection dbConn) throws SQLException {
       
        // remove existing values first
        String delete = "delete from smReplicationStatus " + 
        "where guid = ?";
        PreparedStatement stmt = dbConn.prepareStatement(delete);
        //data values
        stmt.setString(1, guid);
        //execute
        int deletedCount = stmt.executeUpdate();
        stmt.close();
        
        if (replicas != null) {
            for (Replica replica: replicas) {
	            // Execute the insert statement
	            String insert = "insert into smReplicationStatus " + 
	                "(guid, member_node, status, date_verified) " +
	                "values (?, ?, ?, ?)";
	            PreparedStatement insertStatement = dbConn.prepareStatement(insert);
	            
	            //data values
	            String memberNode = replica.getReplicaMemberNode().getValue();
	            String status = replica.getReplicationStatus().toString();
	            java.sql.Timestamp sqlDate = new java.sql.Timestamp(replica.getReplicaVerified().getTime());
	            insertStatement.setString(1, guid);
	            insertStatement.setString(2, memberNode);
	            insertStatement.setString(3, status);
	            insertStatement.setTimestamp(4, sqlDate);

	            logMetacat.debug("smReplicationStatus sql: " + insertStatement.toString());
	            
	            //execute
	            int rows = insertStatement.executeUpdate();
	            insertStatement.close();
            }
        }
       
    }
    
    /**
     * Insert the system metadata fields into the db
     * @param sm
     * @throws McdbDocNotFoundException 
     * @throws SQLException 
     * @throws InvalidSystemMetadata 
     * @throws AccessException 
     */
    public void updateSystemMetadata(SystemMetadata sm, DBConnection dbConn) 
      throws McdbDocNotFoundException, SQLException, InvalidSystemMetadata, AccessException {
    	
      Boolean replicationAllowed = false;
		  Integer numberReplicas = -1;
    	ReplicationPolicy replicationPolicy = sm.getReplicationPolicy();
    	if (replicationPolicy != null) {
    		replicationAllowed = replicationPolicy.getReplicationAllowed();
    		numberReplicas = replicationPolicy.getNumberReplicas();
    		replicationAllowed = replicationAllowed == null ? false: replicationAllowed;
    		numberReplicas = numberReplicas == null ? -1: numberReplicas;
    	}
    	
    	// the main systemMetadata fields
		  updateSystemMetadataFields(
				sm.getDateUploaded() == null ? null: sm.getDateUploaded().getTime(),
				sm.getRightsHolder() == null ? null: sm.getRightsHolder().getValue(), 
				sm.getChecksum() == null ? null: sm.getChecksum().getValue(), 
				sm.getChecksum() == null ? null: sm.getChecksum().getAlgorithm(), 
				sm.getOriginMemberNode() == null ? null: sm.getOriginMemberNode().getValue(),
				sm.getAuthoritativeMemberNode() == null ? null: sm.getAuthoritativeMemberNode().getValue(), 
				sm.getDateSysMetadataModified() == null ? null: sm.getDateSysMetadataModified().getTime(),
				sm.getSubmitter() == null ? null: sm.getSubmitter().getValue(), 
		    sm.getIdentifier().getValue(),
		    sm.getFormatId() == null ? null: sm.getFormatId().getValue(),
		    sm.getSize(),
		    sm.getArchived() == null ? false: sm.getArchived(),
		    replicationAllowed, 
		    numberReplicas,
		    sm.getObsoletes() == null ? null:sm.getObsoletes().getValue(),
		    sm.getObsoletedBy() == null ? null: sm.getObsoletedBy().getValue(),
		    sm.getSerialVersion(),
		    dbConn
        );
        
        String guid = sm.getIdentifier().getValue();
        
        // save replication policies
        if (replicationPolicy != null) {
		    List<String> nodes = null;
		    String policy = null;
		    
		    // check for null 
		    if (replicationPolicy.getBlockedMemberNodeList() != null) {
			    nodes = new ArrayList<String>();
			    policy = "blocked";
			    for (NodeReference node: replicationPolicy.getBlockedMemberNodeList()) {
			    	nodes.add(node.getValue());
			    }
			    this.insertReplicationPolicy(guid, policy, nodes, dbConn);
		    }
		    
		    if (replicationPolicy.getPreferredMemberNodeList() != null) {
			    nodes = new ArrayList<String>();
			    policy = "preferred";
			    for (NodeReference node: replicationPolicy.getPreferredMemberNodeList()) {
			    	nodes.add(node.getValue());
			    }
		        this.insertReplicationPolicy(guid, policy, nodes, dbConn);
		    }
        }
        
        // save replica information
        this.insertReplicationStatus(guid, sm.getReplicaList(), dbConn);
        
        // save access policy
        AccessPolicy accessPolicy = sm.getAccessPolicy();
        if (accessPolicy != null) {
			this.insertAccessPolicy(guid, accessPolicy);
        }
    }
    
    /**
     * Creates Metacat access rules and inserts them
     * @param accessPolicy
     * @throws McdbDocNotFoundException
     * @throws AccessException
     */
    private void insertAccessPolicy(String guid, AccessPolicy accessPolicy) throws McdbDocNotFoundException, AccessException {
    	
    	// check for the existing permOrder so that we remain compatible with it (DataONE does not care)
        XMLAccessAccess accessController  = new XMLAccessAccess();
		String existingPermOrder = AccessControlInterface.ALLOWFIRST;
        Vector<XMLAccessDAO> existingAccess = accessController.getXMLAccessForDoc(guid);
        if (existingAccess != null && existingAccess.size() > 0) {
        	existingPermOrder = existingAccess.get(0).getPermOrder();
        }
        
    	List<XMLAccessDAO> accessDAOs = new ArrayList<XMLAccessDAO>();
        for (AccessRule accessRule: accessPolicy.getAllowList()) {
        	List<Subject> subjects = accessRule.getSubjectList();
        	List<Permission> permissions = accessRule.getPermissionList();
        	for (Subject subject: subjects) {
    			XMLAccessDAO accessDAO = new XMLAccessDAO();
        		accessDAO.setPrincipalName(subject.getValue());
    			accessDAO.setGuid(guid);
    			accessDAO.setPermType(AccessControlInterface.ALLOW);
				accessDAO.setPermOrder(existingPermOrder);
    			if (permissions != null) {
	    			for (Permission permission: permissions) {
	    				Long metacatPermission = new Long(convertPermission(permission));
	        			accessDAO.addPermission(metacatPermission);
	    			}
    			}
    			accessDAOs.add(accessDAO);
        	}
        }
        
        
        // remove all existing allow records
        accessController.deleteXMLAccessForDoc(guid, AccessControlInterface.ALLOW);
        // add the ones we can for this guid
        accessController.insertAccess(guid, accessDAOs);
        
        
    }
    
    /**
     * Lookup access policy from Metacat
     * @param guid
     * @return
     * @throws McdbDocNotFoundException
     * @throws AccessException
     */
    public AccessPolicy getAccessPolicy(String guid) throws McdbDocNotFoundException, AccessException {
        AccessPolicy accessPolicy = new AccessPolicy();

    	// use GUID to look up the access
        XMLAccessAccess accessController  = new XMLAccessAccess();
        List<XMLAccessDAO> accessDAOs = accessController.getXMLAccessForDoc(guid);
        
        for (XMLAccessDAO accessDAO: accessDAOs) {
        	// only add allow rule
        	if (accessDAO.getPermType().equals(AccessControlInterface.ALLOW)) {
	        	AccessRule accessRule = new AccessRule();    	
	        	List <Permission> permissions = convertPermission(accessDAO.getPermission().intValue());
	        	// cannot include if we have no permissions
	        	if (permissions == null || permissions.isEmpty()) {
	        		logMetacat.warn("skipping empty access rule permissions for " + guid);
	        		continue;
	        	}
	        	accessRule.setPermissionList(permissions);
	        	Subject subject = new Subject();
	        	subject.setValue(accessDAO.getPrincipalName());
	        	accessRule.addSubject(subject);
	            accessPolicy.addAllow(accessRule);
        	}
        }
        return accessPolicy;
    }
    
    public int convertPermission(Permission permission) {
    	if (permission.equals(Permission.READ)) {
    		return AccessControlInterface.READ;
    	}
    	if (permission.equals(Permission.WRITE)) {
    		return AccessControlInterface.WRITE;
    	}
    	if (permission.equals(Permission.CHANGE_PERMISSION)) {
    		return AccessControlInterface.CHMOD;
    	}
		return -1;
    }
    
    public List<Permission> convertPermission(int permission) {
    	
    	List<Permission> permissions = new ArrayList<Permission>();
    	if (permission == AccessControlInterface.ALL) {
    		permissions.add(Permission.READ);
    		permissions.add(Permission.WRITE);
    		permissions.add(Permission.CHANGE_PERMISSION);
    		return permissions;
    	}
    	
    	if ((permission & AccessControlInterface.CHMOD) == AccessControlInterface.CHMOD) {
    		permissions.add(Permission.CHANGE_PERMISSION);
    	}
    	if ((permission & AccessControlInterface.READ) == AccessControlInterface.READ) {
    		permissions.add(Permission.READ);
    	}
    	if ((permission & AccessControlInterface.WRITE) == AccessControlInterface.WRITE) {
    		permissions.add(Permission.WRITE);
    	}
    	
		return permissions;
    }
    
    /**
     * Lookup a localId given the GUID. If
     * the identifier is not found, throw an exception.
     * 
     * @param guid the global identifier to look up
     * @return String containing the corresponding LocalId
     * @throws McdbDocNotFoundException if the identifier is not found
     */
    public String getLocalId(String guid) throws McdbDocNotFoundException {
      
      String db_guid = "";
      String docid = "";
      int rev = 0;
      
      String query = "select guid, docid, rev from " + TYPE_IDENTIFIER + " where guid = ?";
      
      DBConnection dbConn = null;
      int serialNumber = -1;
      try {
          // Get a database connection from the pool
          dbConn = DBConnectionPool.getDBConnection("Identifier.getLocalId");
          serialNumber = dbConn.getCheckOutSerialNumber();
          
          // Execute the insert statement
          PreparedStatement stmt = dbConn.prepareStatement(query);
          stmt.setString(1, guid);
          ResultSet rs = stmt.executeQuery();
          if (rs.next()) {
              db_guid = rs.getString(1);
              docid = rs.getString(2);
              rev = rs.getInt(3);
              assert(db_guid.equals(guid));
          } else {
              throw new McdbDocNotFoundException("Document not found:" + guid);
          }
          stmt.close();
      } catch (SQLException e) {
          logMetacat.error("Error while looking up the local identifier: " 
                  + e.getMessage());
      } finally {
          // Return database connection to the pool
          DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }
      return docid + "." + rev;
    }
    
    /**
     * query the systemmetadata table based on the given parameters
     * @param startTime
     * @param endTime
     * @param objectFormat
     * @param replicaStatus
     * @param start
     * @param count
     * @return ObjectList
     * @throws SQLException 
     * @throws ServiceException 
     * @throws PropertyNotFoundException 
     */
    public ObjectList querySystemMetadata(Date startTime, Date endTime,
        ObjectFormatIdentifier objectFormatId, boolean replicaStatus,
        int start, int count) 
        throws SQLException, PropertyNotFoundException, ServiceException {
        ObjectList ol = new ObjectList();
        DBConnection dbConn = null;
        int serialNumber = -1;

        try {
            String fieldSql = "select guid, date_uploaded, rights_holder, checksum, "
                    + "checksum_algorithm, origin_member_node, authoritive_member_node, "
                    + "date_modified, submitter, object_format, size from systemmetadata";
            
            // handle special case quickly
            String countSql = "select count(guid) from systemmetadata";
            
            // the clause
            String whereClauseSql = "";

            boolean f1 = false;
            boolean f2 = false;
            boolean f3 = false;

            if (startTime != null) {
                whereClauseSql += " where systemmetadata.date_modified >= ?";
                f1 = true;
            }

            if (endTime != null) {
                if (!f1) {
                    whereClauseSql += " where systemmetadata.date_modified < ?";
                } else {
                    whereClauseSql += " and systemmetadata.date_modified < ?";
                }
                f2 = true;
            }

            if (objectFormatId != null) {
                if (!f1 && !f2) {
                    whereClauseSql += " where object_format = ?";
                } else {
                    whereClauseSql += " and object_format = ?";
                }
                f3 = true;
            }

            if (!replicaStatus) {
                String currentNodeId = PropertyService.getInstance().getProperty("dataone.nodeId");
                if (!f1 && !f2 && !f3) {
                    whereClauseSql += " where authoritive_member_node = '" +
                        currentNodeId.trim() + "'";
                } else {
                    whereClauseSql += " and authoritive_member_node = '" +
                        currentNodeId.trim() + "'";
                }
            }
            
            // connection
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.querySystemMetadata");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // the field query
            String orderBySql = " order by guid ";
            String fieldQuery = fieldSql + whereClauseSql + orderBySql;
            String finalQuery = DatabaseService.getInstance().getDBAdapter().getPagedQuery(fieldQuery, start, count);
            PreparedStatement fieldStmt = dbConn.prepareStatement(finalQuery);
            
            // construct the count query and statment
            String countQuery = countSql + whereClauseSql;
            PreparedStatement countStmt = dbConn.prepareStatement(countQuery);

            if (f1 && f2 && f3) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                fieldStmt.setString(3, objectFormatId.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                countStmt.setString(3, objectFormatId.getValue());
            } else if (f1 && f2 && !f3) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
            } else if (f1 && !f2 && f3) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setString(2, objectFormatId.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setString(2, objectFormatId.getValue());
            } else if (f1 && !f2 && !f3) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
            } else if (!f1 && f2 && f3) {
                fieldStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                fieldStmt.setString(2, objectFormatId.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                countStmt.setString(2, objectFormatId.getValue());
            } else if (!f1 && !f2 && f3) {
                fieldStmt.setString(1, objectFormatId.getValue());
                // count
                countStmt.setString(1, objectFormatId.getValue());
            } else if (!f1 && f2 && !f3) {
                fieldStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                // count
                countStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
            }

            logMetacat.debug("list objects fieldStmt: " + fieldStmt.toString());
            
            logMetacat.debug("list objects countStmt: " + countStmt.toString());
            
            // get the total object count no matter what
            int total = 0;
            ResultSet totalResult = countStmt.executeQuery();
            if (totalResult.next()) {
            	total = totalResult.getInt(1);
            }
            
            logMetacat.debug("list objects total: " + total);

        	// set the totals
        	ol.setStart(start);
            ol.setCount(count);
            ol.setTotal(total);
            
            // retrieve the actual records if requested
            if (count != 0) {
            	
                ResultSet rs = fieldStmt.executeQuery();
	            while (rs.next()) {                
	                
	                String guid = rs.getString(1);
	                logMetacat.debug("query found object with guid " + guid);
	                // Timestamp dateUploaded = rs.getTimestamp(2);
	                // String rightsHolder = rs.getString(3);
	                String checksum = rs.getString(4);
	                String checksumAlgorithm = rs.getString(5);
	                // String originMemberNode = rs.getString(6);
	                // String authoritiveMemberNode = rs.getString(7);
	                Timestamp dateModified = rs.getTimestamp(8);
	                // String submitter = rs.getString(9);
	                String fmtidStr = rs.getString(10);
	                String sz = rs.getString(11);
	                BigInteger size = new BigInteger("0");
	
	                if (sz != null && !sz.trim().equals("")) {
	                    size = new BigInteger(rs.getString(11));
	                }
	
	                ObjectInfo oi = new ObjectInfo();
	
	                Identifier id = new Identifier();
	                id.setValue(guid);
	                oi.setIdentifier(id);
	
	                if (dateModified != null) {
	                    oi.setDateSysMetadataModified(dateModified);
	                }
	
	                Checksum cs = new Checksum();
	                cs.setValue(checksum);
	                try {
	                    // cs.setAlgorithm(ChecksumAlgorithm.valueOf(checksumAlgorithm));
	                    cs.setAlgorithm(checksumAlgorithm);
	                } catch (Exception e) {
	                    logMetacat.error("could not parse checksum algorithm", e);
	                    continue;
	                }
	                oi.setChecksum(cs);
	
	                // set the format type
	                ObjectFormatIdentifier fmtid = new ObjectFormatIdentifier();
	                fmtid.setValue(fmtidStr);
	                oi.setFormatId(fmtid);
	
	                oi.setSize(size);
	
	                ol.addObjectInfo(oi);                    

	            }
	            
	            logMetacat.debug("list objects count: " + ol.sizeObjectInfoList());

	            // set the actual count retrieved
	            ol.setCount(ol.sizeObjectInfoList());
	
	        }
            
        }

        finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }

        return ol;
    }
    
    /**
     * create a mapping in the identifier table
     * @param guid
     * @param localId
     */
    public void createMapping(String guid, String localId)
    {        
        
        int serialNumber = -1;
        DBConnection dbConn = null;
        try {

            // Parse the localId into scope and rev parts
            AccessionNumber acc = new AccessionNumber(localId, "NOACTION");
            String docid = acc.getDocid();
            int rev = 1;
            if (acc.getRev() != null) {
              rev = (new Integer(acc.getRev()).intValue());
            }

            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.createMapping");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            String query = "insert into " + TYPE_IDENTIFIER + " (guid, docid, rev) values (?, ?, ?)";
            PreparedStatement stmt = dbConn.prepareStatement(query);
            stmt.setString(1, guid);
            stmt.setString(2, docid);
            stmt.setInt(3, rev);
            logMetacat.debug("mapping query: " + stmt.toString());
            int rows = stmt.executeUpdate();

            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            logMetacat.error("createGenericMapping: SQL error while creating a mapping to the " + TYPE_IDENTIFIER + " identifier: " 
                    + e.getMessage());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            logMetacat.error("createGenericMapping: NumberFormat error while creating a mapping to the " + TYPE_IDENTIFIER + " identifier: " 
                    + e.getMessage());
        } catch (AccessionNumberException e) {
            e.printStackTrace();
            logMetacat.error("createGenericMapping: AccessionNumber error while creating a mapping to the " + TYPE_IDENTIFIER + " identifier: " 
                    + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }
    
    /**
     * remove a mapping in the identifier table
     * @param guid
     * @param localId
     */
    public void removeMapping(String guid, String localId)
    {        
        
        int serialNumber = -1;
        DBConnection dbConn = null;
        try {

            // Parse the localId into scope and rev parts
            AccessionNumber acc = new AccessionNumber(localId, "NOACTION");
            String docid = acc.getDocid();
            int rev = 1;
            if (acc.getRev() != null) {
              rev = (new Integer(acc.getRev()).intValue());
            }

            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.removeMapping");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            String query = "DELETE FROM " + TYPE_IDENTIFIER + " WHERE guid = ? AND docid = ? AND rev = ?";
            PreparedStatement stmt = dbConn.prepareStatement(query);
            stmt.setString(1, guid);
            stmt.setString(2, docid);
            stmt.setInt(3, rev);
            logMetacat.debug("remove mapping query: " + stmt.toString());
            int rows = stmt.executeUpdate();

            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            logMetacat.error("removeMapping: SQL error while removing a mapping to the " + TYPE_IDENTIFIER + " identifier: " 
                    + e.getMessage());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            logMetacat.error("removeMapping: NumberFormat error while removing a mapping to the " + TYPE_IDENTIFIER + " identifier: " 
                    + e.getMessage());
        } catch (AccessionNumberException e) {
            e.printStackTrace();
            logMetacat.error("removeMapping: AccessionNumber error while removing a mapping to the " + TYPE_IDENTIFIER + " identifier: " 
                    + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }
    
    /**
     * create the systemmetadata record
     * @param guid
     * @param dbConn 
     * @throws SQLException 
     */
    private void insertSystemMetadata(String guid, DBConnection dbConn) throws SQLException
    {        

        // Execute the insert statement
        String query = "insert into " + TYPE_SYSTEM_METADATA + " (guid) values (?)";
        PreparedStatement stmt = dbConn.prepareStatement(query);
        stmt.setString(1, guid);
        logMetacat.debug("system metadata query: " + stmt.toString());
        int rows = stmt.executeUpdate();

        stmt.close();
        
    }
    
    public boolean deleteSystemMetadata(String guid)
    {        
    	boolean success = false;
        int serialNumber = -1;
        DBConnection dbConn = null;
        String query = null;
        PreparedStatement stmt = null;
        int rows = 0;
        try {

            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.deleteSystemMetadata");
            serialNumber = dbConn.getCheckOutSerialNumber();
            dbConn.setAutoCommit(false);
           
            // remove the smReplicationPolicy
            query = "delete from smReplicationPolicy " + 
            "where guid = ?";
            stmt = dbConn.prepareStatement(query);
            stmt.setString(1, guid);
            logMetacat.debug("delete smReplicationPolicy: " + stmt.toString());
            rows = stmt.executeUpdate();
            stmt.close();
            
            // remove the smReplicationStatus
            query = "delete from smReplicationStatus " + 
            "where guid = ?";
            stmt = dbConn.prepareStatement(query);
            stmt.setString(1, guid);
            logMetacat.debug("delete smReplicationStatus: " + stmt.toString());
            rows = stmt.executeUpdate();
            stmt.close();
            
            // remove main system metadata entry
            query = "delete from " + TYPE_SYSTEM_METADATA + " where guid = ? ";
            stmt = dbConn.prepareStatement(query);
            stmt.setString(1, guid);
            logMetacat.debug("delete system metadata: " + stmt.toString());
            rows = stmt.executeUpdate();
            stmt.close();
            
            dbConn.commit();
            dbConn.setAutoCommit(true);
            success = true;
            // TODO: remove the access?
            // Metacat keeps "deleted" documents so we should not remove access rules.
            
        } catch (Exception e) {
            e.printStackTrace();
            logMetacat.error("Error while deleting " + TYPE_SYSTEM_METADATA + " record: " + guid, e );
            try {
				dbConn.rollback();
			} catch (SQLException sqle) {
	            logMetacat.error("Error while rolling back delete for record: " + guid, sqle );
			}
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return success;
    }
    
    public void updateAuthoritativeMemberNodeId(String existingMemberNodeId, String newMemberNodeId)
    {
        DBConnection dbConn = null;
        int serialNumber = -1;
        
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.updateAuthoritativeMemberNodeId");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the insert statement
            String query = "update " + TYPE_SYSTEM_METADATA + 
                " set authoritive_member_node = ? " +
                " where authoritive_member_node = ?";
            PreparedStatement stmt = dbConn.prepareStatement(query);
            
            //data values
            stmt.setString(1, newMemberNodeId);
            stmt.setString(2, existingMemberNodeId);

            logMetacat.debug("stmt: " + stmt.toString());
            //execute
            int rows = stmt.executeUpdate();

            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            logMetacat.error("updateSystemMetadataFields: SQL error while updating system metadata: " 
                    + e.getMessage());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            logMetacat.error("updateSystemMetadataFields: NumberFormat error while updating system metadata: " 
                    + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }
}

