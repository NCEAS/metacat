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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dataone.client.v2.formats.ObjectFormatCache;
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
import org.dataone.service.types.v2.MediaType;
import org.dataone.service.types.v2.MediaTypeProperty;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.accesscontrol.XMLAccessAccess;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
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
    
    /*public SystemMetadata asSystemMetadata(Date dateUploaded, String rightsHolder,
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
    }*/
    
    /**
     * return a hash of all of the info that is in the systemmetadata table
     * @param localId
     * @return
     */
    /*public Hashtable<String, String> getSystemMetadataInfo(String localId)
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
    }*/
    
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
          "replication_allowed, number_replicas, obsoletes, obsoleted_by, serial_version, archived, series_id, file_name, media_type " +
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
                String series_id = rs.getString(18);
                String file_name = rs.getString(19);
                String media_type = rs.getString(20);

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
                if(series_id != null) {
                    Identifier seriesId = new Identifier();
                    seriesId.setValue(series_id);
                    sysMeta.setSeriesId(seriesId);
                }
                if(file_name != null ) {
                    sysMeta.setFileName(file_name);
                }
                
                if(media_type != null ) {
                    MediaType mediaType = new MediaType();
                    mediaType.setName(media_type);
                    // get media type properties from another table.
                    String mediaTypePropertyQuery = "select name, value from smmediatypeproperties where guid = ?";
                    PreparedStatement stmt2 = dbConn.prepareStatement(mediaTypePropertyQuery);
                    stmt2.setString(1, guid);
                    ResultSet rs2 = stmt2.executeQuery();
                    while (rs2.next()) {
                        String name = rs2.getString(1);
                        String value = rs2.getString(2);
                        MediaTypeProperty property = new MediaTypeProperty();
                        property.setName(name);
                        property.setValue(value);
                        mediaType.addProperty(property);
                    }
                    sysMeta.setMediaType(mediaType);
                    rs2.close();
                    stmt2.close();
                }
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
			"from smReplicationPolicy where guid = ? and policy = ? order by policy_id ASC";
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
    public boolean identifierExists(String guid) throws SQLException
    {
        boolean idExists = false;
        try {
            String id = getLocalId(guid);
            if (id != null) {
                idExists = true;
            }
        } catch (McdbDocNotFoundException e) {
        	// try system metadata only
        	    //this will check if the guid field on the system metadata table has the id
        		idExists = systemMetadataPIDExists(guid);
        		if(!idExists) {
        		    //if the guid field of the system metadata table doesn't have the id,
        		    //we will check if the serial_id field of the system metadata table has it
        		    idExists=systemMetadataSIDExists(guid);
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
    public boolean mappingExists(String guid) throws SQLException
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
        PreparedStatement stmt = null;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getGUID");
            serialNumber = dbConn.getCheckOutSerialNumber();
            
            // Execute the insert statement
            stmt = dbConn.prepareStatement(query);
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
            if(rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            logMetacat.error("Error while looking up the guid: " 
                    + e.getMessage());
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                logMetacat.warn("Couldn't close the prepared statement since "+e.getMessage());
            } finally {
                // Return database connection to the pool
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }
            
        }
        
        return guid;
    }
    
    /**
     * Get the pid of the head (current) version of objects match the specified sid.
     * 1. locate all candidate chain-ends for S1:
     *      determined by:  seriesId == S1 AND (obsoletedBy == null  OR obsoletedBy.seriesId != S1) // these are the type1 and type2 ends
     *      If obsoletedBy is missing, we generally consider it a type 2 end except:
     *      there is another object in the chain (has the same series id) that obsoletes the missing object. 
     * 2. if only 1 candidate chain-end, return it as the HEAD
     * 3. otherwise return the one in the chain with the latest dateUploaded value. However, we find that dateUpload doesn't refect the obsoletes information
     * (espically on the cn), so we will check osoletes information as well. https://redmine.dataone.org/issues/7624
     * @param sid specified sid which should match.
     * @return the pid of the head version. The null will be returned if there is no pid found.
     * @throws SQLException 
     */
    public Identifier getHeadPID(Identifier sid) throws SQLException {
        Identifier pid = null;
        if(sid != null && sid.getValue() != null && !sid.getValue().trim().equals("")) {
            logMetacat.debug("getting pid of the head version for matching the sid: " + sid.getValue());
            String sql = "select guid, obsoleted_by, obsoletes from systemMetadata where series_id = ? order by date_uploaded DESC";
            DBConnection dbConn = null;
            int serialNumber = -1;
            PreparedStatement stmt = null;
            PreparedStatement stmt2 = null;
            ResultSet rs = null;
            ResultSet result = null;
            //int endsCount = 0;
            boolean hasError = false;
            HashMap<String, String> obsoletesIdGuidMap = new HashMap<String, String>();//the key is an obsoletes id, the value is an guid
            Vector<Identifier> endsList = new Vector<Identifier>();//the vector storing ends
            try {
                // Get a database connection from the pool
                dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getHeadPID");
                serialNumber = dbConn.getCheckOutSerialNumber();
                // Execute the insert statement
                stmt = dbConn.prepareStatement(sql);
                stmt.setString(1, sid.getValue());
                rs = stmt.executeQuery();
                boolean hasNext = rs.next();
                boolean first = true;
                Identifier firstOne = new Identifier();//since the sql using the desc order, the first one has the latest upload date.
                if (hasNext) 
                {
                    while(hasNext) {
                        String guidStr = rs.getString(1);
                        String obsoletedByStr = rs.getString(2);
                        String obsoletesStr = rs.getString(3);
                        Identifier guid = new Identifier();
                        guid.setValue(guidStr);
                        if(obsoletesStr != null && !obsoletesStr.trim().equals("")) {
                            if(obsoletesIdGuidMap.containsKey(obsoletesStr) && !guidStr.equals(obsoletesIdGuidMap.get(obsoletesStr))) {
                                logMetacat.error("Both id "+guidStr+" and id "+obsoletesIdGuidMap.get(obsoletesStr)+" obsoletes the id"+obsoletesStr+
                                        ". It is illegal. So the head pid maybe is wrong.");
                                hasError = true;
                            } 
                            logMetacat.debug("Put "+guidStr+"(a value) Obsoletes "+obsoletesStr+" (a key) into the vector.");
                            obsoletesIdGuidMap.put(obsoletesStr, guidStr);
                        }
                        if(first) {
                            firstOne = guid;
                            first =false;
                        }
                        //SystemMetadata sysmeta = HazelcastService.getInstance().getSystemMetadataMap().get(guid);
                        //if(sysmeta.getObsoletedBy() == null) {
                        if(obsoletedByStr == null || obsoletedByStr.trim().equals("")) {
                            //type 1 end
                            logMetacat.debug(""+guidStr+" is a type 1 end for sid "+sid.getValue());
                            //pid = guid;
                            //endsCount++;
                            endsList.add(guid);
                        } else {
                            //Identifier obsoletedBy = sysmeta.getObsoletedBy();
                            Identifier obsoletedBy = new Identifier();
                            obsoletedBy.setValue(obsoletedByStr);
                            //SystemMetadata obsoletedBySysmeta = HazelcastService.getInstance().getSystemMetadataMap().get(obsoletedBy);
                            String sql2 = "select series_id, guid from systemMetadata where guid = ? ";
                            stmt2 = dbConn.prepareStatement(sql2);
                            stmt2.setString(1, obsoletedByStr);
                            result = stmt2.executeQuery();
                            boolean next = result.next();
                            //if(obsoletedBySysmeta != null) {
                            if(next) {
                                logMetacat.debug("The object "+obsoletedBy+" which obsoletes "+guidStr+" does have a system metadata on the table.");
                                //Identifier sidInObsoletedBy = obsoletedBySysmeta.getSeriesId();
                                String sidInObsoletedBy = result.getString(1);
                                if(sidInObsoletedBy == null|| !sidInObsoletedBy.equals(sid.getValue())) {
                                    // type 2 end
                                    logMetacat.debug(""+guidStr+" is a type 2 end for sid "+sid.getValue()+ "since it is obsoleted by the object "+sidInObsoletedBy+
                                            " which has a different sid or no sids");
                                    //pid = guid;
                                    //endsCount++;
                                    endsList.add(guid);
                                }
                            } else {
                                logMetacat.debug("The object "+obsoletedBy+" which obsoletes "+guidStr+" is missing on the host.");
                                //obsoletedBySysmeta doesn't exist; it means the object is missing
                                //generally, we consider it we generally consider it a type 2 end except:
                                 //there is another object in the chain (has the same series id) that obsoletes the missing object. 
                                /*String sql2 = "select guid from systemMetadata where  obsoletes = ? and series_id = ?";
                                PreparedStatement stmt2 = dbConn.prepareStatement(sql2);
                                stmt2.setString(1, obsoletedBy.getValue());
                                stmt2.setString(2, sid.getValue());
                                ResultSet result = stmt2.executeQuery();
                                boolean next = result.next();
                                int count = 0;
                                while(next) {
                                    count++;
                                    next = result.next();
                                }
                                if(count == 0) {
                                    //the exception (another object in the chain (has the same series id) that obsoletes the missing object) doesn't exist
                                    // it is a type 2 end
                                    logMetacat.debug(""+guidStr+" is a type 2 end for sid "+sid.getValue());
                                    pid = guid;
                                    endsCount++;
                                } else if (count ==1) {
                                    // it is not end, do nothing;
                                } else {
                                    // something is wrong - there are more than one objects obsolete the missing object!
                                    hasError = true;
                                    break;
                                }*/
                                if(obsoletesIdGuidMap != null && obsoletesIdGuidMap.containsKey(obsoletedByStr)) {
                                   //This is the exception - another object in the chain (has the same series id) that obsoletes the missing object
                                    //The obsoletesIdGuidMap maintains the relationship (with the same  series id)
                                    logMetacat.debug("Though the object "+obsoletedBy+" which obsoletes "+guidStr+" is missing."+
                                            " However, there is another object "+obsoletesIdGuidMap.get(obsoletedByStr)+" in the chain obsoleting it. So it is not an end.");
                                  
                                } else {
                                    //the exception (another object in the chain (has the same series id) that obsoletes the missing object) doesn't exist
                                    // it is a type 2 end
                                    logMetacat.debug(""+guidStr+" is a type 2 end for sid "+sid.getValue());
                                    //pid = guid;
                                    //endsCount++;
                                    endsList.add(guid);
                                }
                            }
                        }
                        hasNext = rs.next();
                    }
                    if(hasError) {
                        logMetacat.info("The sid chain "+sid.getValue()+" was messed up and we will return the object with the latest upload date.");
                        pid = firstOne;
                    } else {
                        if(endsList.size() == 1) {
                            //it has one end and it is an ideal chain. We already assign the guid to the pid. So do nothing.
                            logMetacat.info("It is an ideal chain for sid "+sid.getValue());
                            pid = endsList.get(0);
                        } else if (endsList.size() ==0) {
                            logMetacat.info("This is weird situation and we don't find any end. We use the latest DateOfupload");
                            pid=checkObsoletesChain(firstOne, obsoletesIdGuidMap);
                        } else if(endsList.size() >1) {
                            // it is not an ideal chain, use the one with latest upload date(the first one in the result set since we have the desc order)
                            logMetacat.info("It is NOT an ideal chain for sid "+sid.getValue());
                            pid = checkObsoletesChain(endsList.get(0), obsoletesIdGuidMap);
                        }
                    }
                    
                } else {
                    //it is not a sid or at least we don't have anything to match it.
                    //do nothing, so null will be returned
                    logMetacat.info("We don't find anything matching the id "+sid.getValue()+" as sid. The null will be returned since it is probably a pid");
                }
                
                
            } catch (SQLException e) {
                logMetacat.error("Error while get the head pid for the sid "+sid.getValue()+" : " 
                        + e.getMessage());
                throw e;
            } finally {
                try {
                    if(rs != null) {
                        rs.close();
                    }
                    if(result != null) {
                        result.close();
                    }
                    if(stmt != null) {
                        stmt.close();
                    }
                    if(stmt2 != null) {
                        stmt2.close();
                    }
                } catch (Exception e) {
                    logMetacat.warn("Couldn't close the prepared statement since "+e.getMessage());
                } finally {
                    // Return database connection to the pool
                    DBConnectionPool.returnDBConnection(dbConn, serialNumber);
                }
            }
        }
        if(pid != null && sid != null) {
            logMetacat.info("The head of chain for sid "+sid.getValue()+"  --is--  "+pid.getValue());
        } else if(pid == null && sid != null) {
            logMetacat.info("The head of chain for sid "+sid.getValue()+" is null. So it is pid.");
        }
        
        return pid;
    }
    
    /*
     * For the non-ideal chain, we used to return the latest Dateupload object as the head pid. However, Dateupload
     * sometimes doesn't refect the obsoletes chain. We need to check if any other objects obsoletes it recursively.
     * see ticket:https://redmine.dataone.org/issues/7624
     */
    private Identifier checkObsoletesChain(Identifier latestDateUpload, HashMap<String, String>obsoletesIdGuidMap) {
        Identifier pid = latestDateUpload;
        if(obsoletesIdGuidMap != null && latestDateUpload != null && obsoletesIdGuidMap.containsKey(latestDateUpload.getValue())) {
            logMetacat.debug("Another object obsoletes the lasted uploaded object "+latestDateUpload.getValue());
            //another object obsoletes the lastedDateUpload object
            String pidStr = obsoletesIdGuidMap.get(latestDateUpload.getValue());
            while (obsoletesIdGuidMap.containsKey(pidStr)) {
                pidStr = obsoletesIdGuidMap.get(pidStr);
                logMetacat.debug("Another object "+pidStr+" obsoletes the object ");
            }
            pid = new Identifier();
            pid.setValue(pidStr);
            
        }
        if(pid != null && latestDateUpload != null){
            logMetacat.debug("IdnetifierManager.checkObsoletesChain - The final return value is "+pid.getValue()+ " for given value "+latestDateUpload.getValue());
        }
        return pid;
    }
    /**
     * Check if the specified sid object exists on the serial id field on the system metadata table
     * @param sid
     * @return true if it exists; false otherwise.
     * @throws SQLException
     */
    public boolean systemMetadataSIDExists(Identifier sid) throws SQLException {
        if (sid != null && sid.getValue() != null && !sid.getValue().trim().equals("")) {
            return systemMetadataSIDExists(sid.getValue());
        } else {
            return false;
        }
    }
    
    /**
     * Check if the specified sid exists on the serial id field on the system metadata table
     * @param id
     * @return true if it exists; false otherwise.
     */
    public boolean systemMetadataSIDExists(String sid) throws SQLException {
        boolean exists = false;
        logMetacat.debug("Check if the  sid: " + sid +" exists on the series_id field of the system metadata table.");
        if(sid != null && !sid.trim().equals("")) {
            String sql = "select guid from systemMetadata where series_id = ?";
            DBConnection dbConn = null;
            int serialNumber = -1;
            PreparedStatement stmt = null;
            try {
                // Get a database connection from the pool
                dbConn = DBConnectionPool.getDBConnection("IdentifierManager.serialIdExists");
                serialNumber = dbConn.getCheckOutSerialNumber();
                // Execute the insert statement
                stmt = dbConn.prepareStatement(sql);
                stmt.setString(1, sid);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) 
                {
                    exists = true;
                } 
                if(rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                logMetacat.error("Error while checking if the sid "+sid+" exists on the series_id field of the system metadata table: " 
                        + e.getMessage());
                throw e;
            } finally {
                try {
                    if(stmt != null) {
                        stmt.close();
                    }
                } catch (Exception e) {
                    logMetacat.warn("Couldn't close the prepared statement since "+e.getMessage());
                } finally {
                    // Return database connection to the pool
                    DBConnectionPool.returnDBConnection(dbConn, serialNumber);
                }
            }
        }
        return exists;
    }
    
    /**
     * Determine if the specified identifier object exists or not.
     * @param pid - the specified identifier
     * @return true if it is exists.
     * @throws SQLException
     * @throws NullPointerException
     */
    public boolean systemMetadataPIDExists(Identifier pid) throws SQLException {
        if (pid != null && pid.getValue() != null && !pid.getValue().trim().equals("")) {
            return systemMetadataPIDExists(pid.getValue());
        } else {
            return false;
        }
    }
    
    public boolean systemMetadataPIDExists(String guid) throws SQLException {
		logMetacat.debug("looking up system metadata for guid " + guid);
		boolean exists = false;
		String query = "select guid from systemmetadata where guid = ?";
		DBConnection dbConn = null;
		int serialNumber = -1;
		PreparedStatement stmt = null;
		if(guid != null && !guid.trim().equals("")) {
		    try {
	            // Get a database connection from the pool
	            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.systemMetadataExisits");
	            serialNumber = dbConn.getCheckOutSerialNumber();

	            // Execute the insert statement
	            stmt = dbConn.prepareStatement(query);
	            stmt.setString(1, guid);
	            ResultSet rs = stmt.executeQuery();
	            if (rs.next()) {
	                exists = true;
	            }
	            if(rs != null) {
	                rs.close();
	            }

	        } catch (SQLException e) {
	            logMetacat.error("Error while looking up the system metadata: "
	                    + e.getMessage());
	            throw e;
	        } finally {
	            try {
	                if(stmt != null) {
	                    stmt.close();
	                }
	            } catch (Exception e) {
	                logMetacat.warn("Couldn't close the prepared statement since "+e.getMessage());
	            } finally {
	                // Return database connection to the pool
	                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
	            }
	        }
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
        	if (!IdentifierManager.getInstance().systemMetadataPIDExists(guid)) {
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
            throw new SQLException("Can't save system metadata "+e.getMessage());
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
        String obsoletedBy, BigInteger serialVersion, String seriesId, 
        String fileName, MediaType mediaType, DBConnection dbConn) throws SQLException  {
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        try {
            dbConn.setAutoCommit(false);
            // Execute the insert statement
            String query = "update " + TYPE_SYSTEM_METADATA + 
                " set (date_uploaded, rights_holder, checksum, checksum_algorithm, " +
                "origin_member_node, authoritive_member_node, date_modified, " +
                "submitter, object_format, size, archived, replication_allowed, number_replicas, " +
                "obsoletes, obsoleted_by, serial_version, series_id, file_name, media_type) " +
                "= (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?) where guid = ?";
            stmt = dbConn.prepareStatement(query);
            
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
            if(serialVersion != null) {
                stmt.setString(16, serialVersion.toString());
            } else {
                stmt.setString(16, null);
            }
            
            stmt.setString(17, seriesId);
            stmt.setString(18, fileName);
            if (mediaType == null) {
                stmt.setString(19, null);
            } else {
                stmt.setString(19, mediaType.getName());
            }
            //where clause
            stmt.setString(20, guid);
            logMetacat.debug("stmt: " + stmt.toString());
            //execute
            int rows = stmt.executeUpdate();
            
            //insert media type properties into another table
            if(mediaType != null && mediaType.getPropertyList() != null) {
                String sql2 = "insert into smmediatypeproperties " + 
                        "(guid, name, value) " + "values (?, ?, ?)";
                stmt2 = dbConn.prepareStatement(sql2);
                for(MediaTypeProperty item : mediaType.getPropertyList()) {
                    if(item != null) {
                        String name = item.getName();
                        String value = item.getValue();
                        stmt2.setString(1, guid);
                        stmt2.setString(2, name);
                        stmt2.setString(3, value);
                        logMetacat.debug("insert media type properties query: " + stmt2.toString());
                        int row =stmt2.executeUpdate();
                    }
                    
                }
            }
            dbConn.commit();
            dbConn.setAutoCommit(true);
        } catch (Exception e) {
            dbConn.rollback();
            dbConn.setAutoCommit(true);
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if(stmt != null) {
                stmt.close();
            }
            if(stmt2 != null) {
                stmt2.close();
            }
        }
        
               
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
		    sm.getSeriesId() == null ? null: sm.getSeriesId().getValue(),
		    sm.getFileName() == null ? null: sm.getFileName(),
		    sm.getMediaType() == null ? null: sm.getMediaType(),
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
    		// implies all permission, rather than just CHMOD
    		return AccessControlInterface.ALL;
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
    public String getLocalId(String guid) throws McdbDocNotFoundException, SQLException {
      
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
          throw e;
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
     * @param nodeId
     * @param start
     * @param count
     * @return ObjectList
     * @throws SQLException 
     * @throws ServiceException 
     * @throws PropertyNotFoundException 
     */
    public ObjectList querySystemMetadata(Date startTime, Date endTime,
        ObjectFormatIdentifier objectFormatId, NodeReference nodeId,
        int start, int count, Identifier identifier, boolean isSID) 
        throws SQLException, PropertyNotFoundException, ServiceException {
        ObjectList ol = new ObjectList();
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement countStmt=null;
        ResultSet totalResult=null;
        PreparedStatement fieldStmt = null;
        ResultSet rs= null;

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
            boolean f4 = false;


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
            
            if(identifier != null && identifier.getValue() != null && !identifier.getValue().equals("")) {
                if (!f1 && !f2 && !f3 ) {
                    if(isSID) {
                        whereClauseSql += " where series_id = ?";
                    } else {
                        whereClauseSql += " where guid = ?";
                    }
                    
                } else {
                    if(isSID) {
                        whereClauseSql += " and series_id = ?";
                    } else {
                        whereClauseSql += " and guid = ?";
                    }
                }
                f4 = true;
            }

            /*if (!replicaStatus) {
                String currentNodeId = PropertyService.getInstance().getProperty("dataone.nodeId");
                if (!f1 && !f2 && !f3 && !f4) {
                    whereClauseSql += " where authoritive_member_node = '" +
                        currentNodeId.trim() + "'";
                } else {
                    whereClauseSql += " and authoritive_member_node = '" +
                        currentNodeId.trim() + "'";
                }
            }*/
            
            if (nodeId != null && nodeId.getValue() != null && !nodeId.getValue().trim().equals("")) {
                if (!f1 && !f2 && !f3 && !f4) {
                    whereClauseSql += " where authoritive_member_node = '" +
                        nodeId.getValue().trim() + "'";
                } else {
                    whereClauseSql += " and authoritive_member_node = '" +
                        nodeId.getValue().trim() + "'";
                }
            }
           
            
            // connection
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.querySystemMetadata");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // the field query
            String orderBySql = " order by guid ";
            String fieldQuery = fieldSql + whereClauseSql + orderBySql;
            String finalQuery = DatabaseService.getInstance().getDBAdapter().getPagedQuery(fieldQuery, start, count);
            fieldStmt = dbConn.prepareStatement(finalQuery);
            
            // construct the count query and statment
            String countQuery = countSql + whereClauseSql;
            countStmt = dbConn.prepareStatement(countQuery);

            if (f1 && f2 && f3 && f4) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                fieldStmt.setString(3, objectFormatId.getValue());
                fieldStmt.setString(4, identifier.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                countStmt.setString(3, objectFormatId.getValue());
                countStmt.setString(4, identifier.getValue());
            } if (f1 && f2 && f3 && !f4) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                fieldStmt.setString(3, objectFormatId.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                countStmt.setString(3, objectFormatId.getValue());
            } else if (f1 && f2 && !f3 && f4) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                fieldStmt.setString(3, identifier.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                countStmt.setString(3, identifier.getValue());
            } else if (f1 && f2 && !f3 && !f4) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setTimestamp(2, new Timestamp(endTime.getTime()));
            } else if (f1 && !f2 && f3 && f4) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setString(2, objectFormatId.getValue());
                fieldStmt.setString(3, identifier.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setString(2, objectFormatId.getValue());
                countStmt.setString(3, identifier.getValue());
            } else if (f1 && !f2 && f3 && !f4) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setString(2, objectFormatId.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setString(2, objectFormatId.getValue());
            } else if (f1 && !f2 && !f3 && f4) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                fieldStmt.setString(2, identifier.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                countStmt.setString(2, identifier.getValue());
            } else if (f1 && !f2 && !f3 && !f4) {
                fieldStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                // count
                countStmt.setTimestamp(1, new Timestamp(startTime.getTime()));
            } else if (!f1 && f2 && f3 && f4) {
                fieldStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                fieldStmt.setString(2, objectFormatId.getValue());
                fieldStmt.setString(3, identifier.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                countStmt.setString(2, objectFormatId.getValue());
                countStmt.setString(3, identifier.getValue());
            } else if (!f1 && f2 && f3 && !f4) {
                fieldStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                fieldStmt.setString(2, objectFormatId.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                countStmt.setString(2, objectFormatId.getValue());
            } else if (!f1 && !f2 && f3 && f4) {
                fieldStmt.setString(1, objectFormatId.getValue());
                fieldStmt.setString(2, identifier.getValue());
                // count
                countStmt.setString(1, objectFormatId.getValue());
                countStmt.setString(2, identifier.getValue());
            } else if (!f1 && !f2 && f3 && !f4) {
                fieldStmt.setString(1, objectFormatId.getValue());
                // count
                countStmt.setString(1, objectFormatId.getValue());
            } else if (!f1 && f2 && !f3 && f4) {
                fieldStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                fieldStmt.setString(2, identifier.getValue());
                // count
                countStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                countStmt.setString(2, identifier.getValue());
            } else if (!f1 && f2 && !f3 && !f4) {
                fieldStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
                // count
                countStmt.setTimestamp(1, new Timestamp(endTime.getTime()));
            } else if (!f1 && !f2 && !f3 && f4) {
                fieldStmt.setString(1, identifier.getValue());
                // count
                countStmt.setString(1, identifier.getValue());
            } else if (!f1 && !f2 && !f3 && !f4) {
                //do nothing
            }

            logMetacat.debug("list objects fieldStmt: " + fieldStmt.toString());
            
            logMetacat.debug("list objects countStmt: " + countStmt.toString());
            
            // get the total object count no matter what
            int total = 0;
            totalResult = countStmt.executeQuery();
            if (totalResult.next()) {
            	total = totalResult.getInt(1);
            }
            
            logMetacat.debug("list objects total: " + total);

        	// set the totals
        	ol.setStart(start);
            ol.setCount(count);
            
            // retrieve the actual records if requested
            if (count != 0) {
            	
                rs = fieldStmt.executeQuery();
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
            ol.setTotal(total);
        } finally {
            // Return database connection to the pool
            try {
                if(totalResult !=null ){
                    totalResult.close();
                }
                if(countStmt!=null ) {
                    countStmt.close();
                }
                if(rs != null) {
                    rs.close();
                }
                if(fieldStmt != null) {
                    fieldStmt.close();
                }
                
            } catch (SQLException sql) {
                
            }
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            
        }
        if(ol != null) {
            logMetacat.debug("list objects start(before returning): " + ol.getStart());
            logMetacat.debug("list objects count: " + ol.getCount());
            logMetacat.debug("list objects total: " + ol.getTotal());
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
            
            // remove the smmediatypeproperties
            query = "delete from smmediatypeproperties " + 
                    "where guid = ?";
            stmt = dbConn.prepareStatement(query);
            stmt.setString(1, guid);
            logMetacat.debug("delete smmediatypeproperties: " + stmt.toString());
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

