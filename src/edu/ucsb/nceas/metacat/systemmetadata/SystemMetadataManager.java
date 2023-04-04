/**
 *  '$RCSfile$'
 *  Copyright: 2023 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
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
package edu.ucsb.nceas.metacat.systemmetadata;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.MediaType;
import org.dataone.service.types.v2.MediaTypeProperty;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.accesscontrol.XMLAccessAccess;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.utilities.access.XMLAccessDAO;


public class SystemMetadataManager {
    private static Log logMetacat = LogFactory.getLog(SystemMetadataManager.class);
    
    private static SystemMetadataManager manager = null;
    private final static int TIME_OUT = 3000;
    private static ArrayList<String> lockedIds = new ArrayList<String>(100); 
    
    /**
     * Private constructor
     */
    private SystemMetadataManager() {
        
    }
    
    /**
     * Get the singleton SystemMetadataManager instance
     * @return  the instance of SystemMetadataManager
     */
    public static SystemMetadataManager getInstance() {
        if (manager == null) {
            synchronized (SystemMetadataManager.class) {
                if (manager == null) {
                    manager = new SystemMetadataManager();
                }
            }
        }
        return manager;
    }
    
    /**
     * Get the system metadata associated with the given identifier from the store.
     * If the returned value is null, this means the system metadata is not found
     * @param pid  the identifier to determine the system metadata
     * @return  the system metadata associated with the given identifier
     * @throws ServiceFailure
     */
    public SystemMetadata get(Identifier pid) throws ServiceFailure {
        SystemMetadata sm = null;
        try {
            if (pid != null && pid.getValue() != null && !pid.getValue().trim().equals("")) {
                logMetacat.debug("SystemMetadataManager.get - loading from store: " + pid.getValue());
                sm = IdentifierManager.getInstance().getSystemMetadata(pid.getValue());
            } 
        } catch (McdbDocNotFoundException e) {
            logMetacat.warn("could not load system metadata for: " +  pid.getValue());
            return null;
        } catch (Exception e) {
            throw new ServiceFailure("0000", e.getMessage());
        }
        return sm;
    }
   
    
    /**
     * Store a system metadata record into the store
     * @param sysmeta  the new system metadata will be inserted
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    public void store(SystemMetadata sysmeta) throws InvalidRequest, ServiceFailure {
        if (sysmeta != null) {
            Identifier pid = sysmeta.getIdentifier();
            if (pid != null && pid.getValue() != null & !pid.getValue().trim().equals("")) {
                //Check if there is another thread is storing the system metadata for the same pid
                //The synchronized keyword makes the lockedIds.contains and lockedIds.add methods can be accessed by one thread (atomic).
                synchronized (lockedIds) {
                    while (lockedIds.contains(pid.getValue())) {
                        try {
                            lockedIds.wait(TIME_OUT);
                        } catch (InterruptedException e) {
                            logMetacat.info("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue() + 
                                             " the lock waiting was interrupted " + e.getMessage());
                        }
                    }
                    lockedIds.add(pid.getValue());
                }
                //Try to write the system metadata into db and remove the pid from the vector and wake up the waiting threads. 
                DBConnection dbConn = null;
                int serialNumber = -1;
                try {
                    logMetacat.debug("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue());
                    dbConn = DBConnectionPool.getDBConnection("IdentifierManager.store");
                    serialNumber = dbConn.getCheckOutSerialNumber();
                    // use a single transaction for it all
                    dbConn.setAutoCommit(false);
                    // insert the record if needed
                    if (!IdentifierManager.getInstance().systemMetadataPIDExists(pid.getValue())) {
                        insertSystemMetadata(pid.getValue(), dbConn);
                    }
                    // update with the values
                    updateSystemMetadata(sysmeta, dbConn);
                    // commit if we got here with no errors
                    dbConn.commit();
                } catch (McdbDocNotFoundException e) {
                    if (dbConn != null) {
                        try {
                            dbConn.rollback();
                        } catch (SQLException ee) {
                            logMetacat.error("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue() + 
                                    " we can't roll back the database changes since " + ee.getMessage());
                        }
                    }
                    throw new InvalidRequest("0000", "SystemMetadataManager.store - can't store the system metadata for pid " + pid.getValue() + " since " + e.getMessage());
                } catch (SQLException e) {
                    if (dbConn != null) {
                        try {
                            dbConn.rollback();
                        } catch (SQLException ee) {
                            logMetacat.error("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue() + 
                                    " we can't roll back the database changes since " + ee.getMessage());
                        }
                    }
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store the system metadata for pid " + pid.getValue() + " since " + e.getMessage());
                } catch (InvalidSystemMetadata e) {
                    if (dbConn != null) {
                        try {
                            dbConn.rollback();
                        } catch (SQLException ee) {
                            logMetacat.error("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue() + 
                                    " we can't roll back the database changes since " + ee.getMessage());
                        }
                    }
                    throw new InvalidRequest("0000", "SystemMetadataManager.store - can't store the system metadata for pid " + pid.getValue() + " since " + e.getMessage());
                } catch (AccessException e) {
                    if (dbConn != null) {
                        try {
                            dbConn.rollback();
                        } catch (SQLException ee) {
                            logMetacat.error("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue() + 
                                    " we can't roll back the database changes since " + ee.getMessage());
                        }
                    }
                    throw new InvalidRequest("0000", "SystemMetadataManager.store - can't store the system metadata for pid " + pid.getValue() + " since " + e.getMessage());
                } catch (RuntimeException e) {
                    if (dbConn != null) {
                        try {
                            dbConn.rollback();
                        } catch (SQLException ee) {
                            logMetacat.error("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue() + 
                                    " we can't roll back the database changes since " + ee.getMessage());
                        }
                    }
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store the system metadata for pid " + pid.getValue() + " since " + e.getMessage());
                } finally {
                    if (dbConn != null) {
                        // Return database connection to the pool
                        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
                    }
                    try {
                        synchronized (lockedIds) {
                            lockedIds.remove(pid.getValue());
                            lockedIds.notifyAll();
                        }
                    } catch (RuntimeException e) {
                        logMetacat.error("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue() + 
                                " we can't move the id from the control list (lockedIds) since " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Delete a system metadata record from the store
     * @param id  the identifier to determine the system metadata record
     * @throws NotFound
     * @throws ServiceFailure
     */
    public void delete(Identifier id) throws NotFound, ServiceFailure {
        if(id != null && id.getValue() != null && !id.getValue().trim().equals("")) {
            logMetacat.debug("SystemMetadataManager.delete - delete the identifier" + id.getValue());
            boolean success = IdentifierManager.getInstance().deleteSystemMetadata(id.getValue());
            if(!success) {
                throw new ServiceFailure("0000", "SystemMetadataManager.delete - the system metadata of guid - " + id.getValue()+" can't be removed successfully.");
            }
        }
    }
    
    /**
     * Lock the system metadata for the given id
     * @param id  the identifier of the system metadata will be locked
     */
    public void lock(Identifier id) {
        
    }
    
    /**
     * unlock the system metadata for the given id
     * @param id  the identifier of the system metadata will be unlocked
     */
    public void unlock(Identifier id) {
        
    }

    
    /**
     * create the systemmetadata record
     * @param guid
     * @param dbConn 
     * @throws SQLException 
     */
    private void insertSystemMetadata(String guid, DBConnection dbConn) throws SQLException {        
        // Execute the insert statement
        String query = "insert into " + IdentifierManager.TYPE_SYSTEM_METADATA + " (guid) values (?)";
        PreparedStatement stmt = dbConn.prepareStatement(query);
        stmt.setString(1, guid);
        logMetacat.debug("system metadata query: " + stmt.toString());
        int rows = stmt.executeUpdate();
        stmt.close();    
    }
    
    
    /**
     * Insert the system metadata fields into the db
     * @param sm
     * @throws McdbDocNotFoundException 
     * @throws SQLException 
     * @throws InvalidSystemMetadata 
     * @throws AccessException 
     */
    protected void updateSystemMetadata(SystemMetadata sm, DBConnection dbConn) 
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
            this.insertAccessPolicy(guid, accessPolicy, dbConn);
        }
    }
    
    /*
     * Update the fields of the system metadata with the given value
     */
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
            String query = "update " + IdentifierManager.TYPE_SYSTEM_METADATA + 
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
    
    /*
     * Insert the replication policy into database
     */
    private void insertReplicationPolicy(String guid, String policy, List<String> memberNodes, DBConnection dbConn) throws SQLException {
           
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
    
    /*
     * Insert the replication status into the database
     */
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
     * Creates Metacat access rules and inserts them
     * @param accessPolicy
     * @throws McdbDocNotFoundException
     * @throws AccessException
     * @throws InvalidSystemMetadata 
     */
    private void insertAccessPolicy(String guid, AccessPolicy accessPolicy, DBConnection conn) 
                                    throws McdbDocNotFoundException, AccessException, InvalidSystemMetadata, SQLException {
        
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
                        if(permission == null) {
                            throw new InvalidSystemMetadata("4956", "The Permission shouldn't be null. It may result from sepcifying a permission by a typo, which is not one of read, write and changePermission.");
                        }
                        Long metacatPermission = new Long(convertPermission(permission));
                        accessDAO.addPermission(metacatPermission);
                    }
                }
                accessDAOs.add(accessDAO);
            }
        }
        // remove all existing allow records
        accessController.deleteXMLAccessForDoc(guid, AccessControlInterface.ALLOW, conn);
        // add the ones we can for this guid
        accessController.insertAccess(guid, accessDAOs, conn);
    }
    
    /**
     * Utility method to convert a permission object to an integer
     * @param permission  the permission which needs to be convert
     * @return  the integer presentation of the permission
     */
    public static int convertPermission(Permission permission) {
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
}