package edu.ucsb.nceas.metacat;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
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
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.FileUtil;
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
    
    private static boolean filterWhiteSpaces =
            Settings.getConfiguration().getBoolean("dataone.listingidentifier.filteringwhitespaces", true);

    /**
     * The single instance of the manager that is always returned.
     */
    private static IdentifierManager self = null;
    private Log logMetacat = LogFactory.getLog(IdentifierManager.class);

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
        Boolean replicationAllowed = Boolean.valueOf(false);
        BigInteger numberOfReplicas = new BigInteger("-1");
        BigInteger serialVersion = new BigInteger("-1");
        Boolean archived = Boolean.valueOf(false);

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
                replicationAllowed = Boolean.valueOf(rs.getBoolean(12));
                numberOfReplicas = new BigInteger(rs.getString(13));
                String obsoletes = rs.getString(14);
                String obsoletedBy = rs.getString(15);
                serialVersion = new BigInteger(rs.getString(16));
                archived = Boolean.valueOf(rs.getBoolean(17));
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
     * Get all pids in the series chain
     * @param sid  the id of the series chain
     * @return  a list of pid in the chain
     * @throws SQLException
     */
    public List<String> getAllPidsInChain(String sid) throws SQLException {
        Vector<String> guids = new Vector<String>();
        String sql = "select guid from systemmetadata where series_id=?";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getAllPidsInChain");
            serialNumber = dbConn.getCheckOutSerialNumber();
            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            stmt.setString(1, sid);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String guid = rs.getString(1);
                guids.add(guid);
            }
            stmt.close();
        } finally {
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
     * (in that order)  Can return true for both PIDs and SIDs. 
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
     * Get the list of identifiers which system metadata matches the given format id and original
     * member node id and guid or series id start with the scheme (doi for example).
     * @param formatId  the format id of the identifier must match the given formatId. 
     * @param nodeId  the original member node of the identifier must match the given nodeId. 
     * @param scheme  the guid or series id must start with the given scheme (doi for exampe)
     * @return the list of identifier string. An empty list will be returned if nothing was found.
     */
    public List<String> getGUIDs(String formatId, String nodeId, String scheme) {
        List<String> guids = new ArrayList<String>();
        String query = "select guid from systemmetadata where object_format = ? and origin_member_node = ? and ( guid like ? or series_id like ?)";
        String guid = null;
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement stmt = null;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getGUIDs");
            serialNumber = dbConn.getCheckOutSerialNumber();
            stmt = dbConn.prepareStatement(query);
            stmt.setString(1, formatId);
            stmt.setString(2, nodeId);
            stmt.setString(3, scheme + "%");
            stmt.setString(4, scheme + "%");
            ResultSet rs = stmt.executeQuery();
            boolean found = rs.next();
            while (found) {
                guid = rs.getString(1);
                guids.add(guid);
                found = rs.next();
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
                logMetacat.warn("Couldn't close the prepared statement since " + e.getMessage());
            } finally {
                // Return database connection to the pool
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }
        }
        return guids;
    }

    /**
     * Get the pid of the head (current) version of objects match the specified sid.
     * 1. locate all candidate chain-ends for S1:
     *      determined by:  seriesId == S1 AND (obsoletedBy == null  OR obsoletedBy.seriesId != S1)
     *      these are the type1 and type2 ends
     *      If obsoletedBy is missing, we generally consider it a type 2 end except:
     *      there is another object in the chain (has the same series id) that obsoletes the missing object.
     * 2. if only 1 candidate chain-end, return it as the HEAD
     * 3. otherwise return the one in the chain with the latest dateUploaded value. However, we find
     * that dateUpload doesn't refect the obsoletes information
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

            boolean hasError = false;
            //the key is an obsoletes id, the value is an guid
            HashMap<String, String> obsoletesIdGuidMap = new HashMap<String, String>();
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
                                   //This is the exception - another object in the chain (has the same series id)
                                    //that obsoletes the missing object
                                    //The obsoletesIdGuidMap maintains the relationship (with the same  series id)
                                    logMetacat.debug("Though the object " + obsoletedBy
                                               + " which obsoletes " + guidStr + " is missing."
                                               + " However, there is another object "
                                               + obsoletesIdGuidMap.get(obsoletedByStr)
                                               + " in the chain obsoleting it. So it is not an end.");
                                  
                                } else {
                                    //the exception (another object in the chain (has the same series id)
                                    //that obsoletes the missing object) doesn't exist
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
                        logMetacat.info("The sid chain " + sid.getValue()
                        + " was messed up and we will return the object with the latest upload date.");
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
                            // it is not an ideal chain, use the one with latest upload date
                            //(the first one in the result set since we have the desc order)
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
              rev = Integer.parseInt(acc.getRev());
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
              throw new McdbDocNotFoundException("Docid not found in the " + TYPE_IDENTIFIER
                  + " table: " + guid);
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
            boolean f5 = false;


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


            if (nodeId != null && nodeId.getValue() != null && !nodeId.getValue().trim().equals("")) {
                if (!f1 && !f2 && !f3 && !f4) {
                    whereClauseSql += " where authoritive_member_node = '" +
                        nodeId.getValue().trim() + "'";
                } else {
                    whereClauseSql += " and authoritive_member_node = '" +
                        nodeId.getValue().trim() + "'";
                }
                f5 = true;
            }

          //add a filter to remove pids whith white spaces
            if(filterWhiteSpaces) {
                logMetacat.debug("IdnetifierManager.querySystemMetadata - the default value of the "
                                  + "property \"dataone.listingidentifier.filteringwhitespaces\" is "
                                  + "true, so we will filter the white spaces in the query");
                if(!f1 && !f2 && !f3 && !f4 && !f5) {
                    whereClauseSql += " where guid not like '% %' ";
                } else {
                    whereClauseSql += " and guid not like '% %' ";
                }
            } else {
                logMetacat.debug("IdnetifierManager.querySystemMetadata - the property "
                                 + "\"dataone.listingidentifier.filteringwhitespaces\" is "
                     + "configured to be false, so we don't filter the white spaces in the query.");
            }


            // connection
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.querySystemMetadata");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // the field query
            String orderBySql = " order by date_modified DESC ";
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

            logMetacat.info("list objects fieldStmt: " + fieldStmt.toString());

            logMetacat.info("list objects countStmt: " + countStmt.toString());

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
     * Create a mapping between the dataone identifier and local docid in the identifier table
     * @param guid  the dataone identifier
     * @param localId  the local docid
     * @throws SQLException
     * @throws AccessionNumberException
     * @throws NumberFormatException
     */
    public void createMapping(String guid, String localId) {
        int serialNumber = -1;
        DBConnection dbConn = null;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.createMapping");
            serialNumber = dbConn.getCheckOutSerialNumber();
            createMapping(guid, localId, dbConn);
        } catch (SQLException e) {
            logMetacat.error("SQL error while creating a mapping to the "
                            + TYPE_IDENTIFIER + " identifier: " + e.getMessage());
        } catch (NumberFormatException e) {
            logMetacat.error("NumberFormat error while creating a mapping to the "
                           + TYPE_IDENTIFIER + " identifier: " + e.getMessage());
        } catch (AccessionNumberException e) {
            logMetacat.error("AccessionNumber error while creating a mapping to the "
                           + TYPE_IDENTIFIER + " identifier: " + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Create a mapping between the dataone identifier and local docid in the identifier table
     * @param guid  the dataone identifier
     * @param localId  the local metacat docid
     * @throws SQLException
     * @throws AccessionNumberException
     * @throws NumberFormatException
     */
    public void createMapping(String guid, String localId, DBConnection dbConn)
                            throws NumberFormatException, AccessionNumberException, SQLException {
        // Parse the localId into scope and rev parts
        AccessionNumber acc = new AccessionNumber(localId, "NOACTION");
        String docid = acc.getDocid();
        int rev = 1;
        if (acc.getRev() != null) {
          rev = Integer.parseInt(acc.getRev());
        }
        // Execute the insert statement
        String query = "insert into " + TYPE_IDENTIFIER + " (guid, docid, rev) values (?, ?, ?)";
        try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
            stmt.setString(1, guid);
            stmt.setString(2, docid);
            stmt.setInt(3, rev);
            logMetacat.debug("mapping query: " + stmt.toString());
            int rows = stmt.executeUpdate();
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
              rev = Integer.parseInt(acc.getRev());
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
     *Get the the file path for the given object local id
     * @param localId
     * @param isScienceMetadata
     * @return
     * @throws PropertyNotFoundException
     */
    public String getObjectFilePath(String localId, boolean isScienceMetadata) throws PropertyNotFoundException {
        String documentPath = null;
        if (localId != null) {      
            String documentDir = null;
            // get the correct location on disk
            if (isScienceMetadata) {
                documentDir = PropertyService.getProperty("application.documentfilepath");
            } else {
                documentDir = PropertyService.getProperty("application.datafilepath");
            }
            documentPath = documentDir + FileUtil.getFS() + localId;
        }
        logMetacat.debug("IdentifierManager.getObjectFilePath - the file path for the object with localId "
                        + localId + " which is scienceMetacat " + isScienceMetadata + ", is "
                        + documentPath + ". If the value is null, this means we can't find it.");
        return documentPath;
    }

    /**
     * IF the given localId exists on the xml_revisions table
     * @param localId
     * @return
     * @throws SQLException
     */
    public boolean existsInXmlLRevisionTable(String docid, int rev) throws SQLException{
        boolean exist =false;
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        logMetacat.info("IdentifierManager.existsInXmlLRevisionTable - the docid is "+docid +" and rev is "+rev);
        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("IdentifierManager.existsInXmlLRevisionTable");
            serialNumber = conn.getCheckOutSerialNumber();
            // Check if the document exists in xml_revisions table.
            //this only archives a document from xml_documents to xml_revisions (also archive the xml_nodes table as well)
            logMetacat.debug("IdentifierManager.existsInXmlLRevisionTable - check if the document "
                              + docid + "." + rev + " exists in the xml_revision table");
            pstmt = conn.prepareStatement("SELECT rev, docid FROM xml_revisions WHERE docid = ? AND rev = ?");
            pstmt.setString(1, docid);
            pstmt.setInt(2, rev);
            logMetacat.debug("IdentifierManager.existsInXmlLRevisionTable - executing SQL: " + pstmt.toString());
            pstmt.execute();
            rs = pstmt.getResultSet();
            if(rs.next()){
                exist = true;
            }
            conn.increaseUsageCount(1);
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(conn, serialNumber);
            if(rs != null) {
                rs.close();
            }
            if(pstmt != null) {
                pstmt.close();
            }
        }
        logMetacat.info("IdentifierManager.existsInXmlLRevisionTable - Does the docid " + docid
                        + "." + rev + " exist in the xml_revision table? - " + exist);
        return exist;
    }

    /**
     * Determine if the given pid exists on the identifier table.
     * @param pid must be a PID
     * @return true if it exists; false otherwise.
     * @throws SQLException
     */
    public boolean existsInIdentifierTable(Identifier pid) throws SQLException {
        boolean exists = false;
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            if(pid != null && pid.getValue() != null && !pid.getValue().trim().equals("")) {
                //check out DBConnection
                conn = DBConnectionPool.getDBConnection("IdentifierManager.existsInIdentifierTable");
                serialNumber = conn.getCheckOutSerialNumber();
                // Check if the document exists in xml_revisions table.
                //this only archives a document from xml_documents to xml_revisions (also archive the xml_nodes table as well)
                logMetacat.debug("IdentifierManager.existsInIdentifierTable - check if the document "
                                  + pid.getValue() + " exists in the identifier table");
                pstmt = conn.prepareStatement("SELECT guid FROM identifier WHERE guid = ?");
                pstmt.setString(1, pid.getValue());
                logMetacat.debug("IdentifierManager.existsInXmlLRevisionTable - executing SQL: " + pstmt.toString());
                pstmt.execute();
                rs = pstmt.getResultSet();
                if(rs.next()){
                    exists = true;
                }
                conn.increaseUsageCount(1);
            }

        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(conn, serialNumber);
            if(rs != null) {
                rs.close();
            }
            if(pstmt != null) {
                pstmt.close();
            }
        }
        logMetacat.info("IdentifierManager.existsInIdentifierTable - Does the guid "
                     + pid.getValue() + " exist in the xml_revision table? - " + exists);
        return exists;
    }

    /**
     * Get the list of guids whose modification dates are in the range of
     * the given start and end times.
     * @param start  the start time of the range
     * @param end  the end time of the range
     * @return  list of guids whose modification dates are in the range
     */
    public List<String> getGUIDsByTimeRange(Date start, Date end) {
        Vector<String> guids = new Vector<String>();
        String sql = "select guid from systemmetadata";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            boolean hasStart = false;
            boolean hasEnd = false;
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IdentifierManager.getAllGUIDs");
            serialNumber = dbConn.getCheckOutSerialNumber();
            if (start != null) {
                sql = sql + " where date_modified >= ?";
                hasStart = true;
            }

            if (end != null) {
                hasEnd = true;
                if (!hasStart) {
                    sql = sql + " where date_modified <= ?";
                } else {
                    sql = sql + " and date_modified <= ?";
                }
            }
            sql = sql + " ORDER BY date_modified asc";
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            if (hasStart && hasEnd) {
                stmt.setTimestamp(1, new Timestamp(start.getTime()));
                stmt.setTimestamp(2, new Timestamp(end.getTime()));
            } else if (hasStart && !hasEnd) {
                stmt.setTimestamp(1, new Timestamp(start.getTime()));
            } else if (!hasStart && hasEnd) {
                stmt.setTimestamp(1, new Timestamp(end.getTime()));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String guid = rs.getString(1);
                guids.add(guid);
            }
            stmt.close();
        } catch (SQLException e) {
            logMetacat.error("Error while retrieving the guid: "
                    + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return guids;
    }
}
