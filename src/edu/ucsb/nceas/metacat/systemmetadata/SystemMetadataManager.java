package edu.ucsb.nceas.metacat.systemmetadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
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
import org.dataone.service.util.TypeMarshaller;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.accesscontrol.XMLAccessAccess;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.utilities.access.XMLAccessDAO;


public class SystemMetadataManager {
    private static Log logMetacat = LogFactory.getLog(SystemMetadataManager.class);
    
    private static SystemMetadataManager manager = null;
    private final static int TIME_OUT_MILLISEC = 1000;
    private final static ArrayList<String> lockedIds = new ArrayList<String>(100);

    public enum SysMetaVersion {CHECKED, UNCHECKED};

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
                logMetacat.debug("SystemMetadataManager.get - loading from store: "
                                    + pid.getValue());
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
     * Note: This method is not thread safe. Please put it into a try-finally statement.
     * Before call this method, you need to call the lock method first in the `try` block and
     * unLock method in the `finally` block.
     * The modification time will be changed and system metadata version will be checked during
     * the process
     * @param sysmeta  the new system metadata will be inserted
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    public void store(SystemMetadata sysmeta) 
                                        throws InvalidRequest, ServiceFailure{
        // Set changeModifyTime true
        store(sysmeta, true, SysMetaVersion.CHECKED);
    }

    /**
     * Store a system metadata record into the store
     * Note: This method is not thread safe. Please put it into a try-finally statement.
     * Before call this method, you need to call the lock method first in the `try` block and
     * unLock method in the `finally` block.
     * @param sysmeta  the new system metadata will be inserted
     * @param changeModifyTime  if we need to change the modify time
     * @param sysMetaCheck  check whether the version of the provided '@param sysmeta'
     *                       matches the version of the existing metadata
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    public void store(SystemMetadata sysmeta, boolean changeModifyTime, SysMetaVersion sysMetaCheck)
                                                        throws InvalidRequest, ServiceFailure {
        if (sysmeta != null) {
            Identifier pid = sysmeta.getIdentifier();
            SystemMetadata backupCopy  = get(pid);
            if (pid != null && pid.getValue() != null && !pid.getValue().trim().equals("")) {
                DBConnection dbConn = null;
                int serialNumber = -1;
                try {
                    dbConn = DBConnectionPool.getDBConnection("SystemMetadataManager.store");
                    serialNumber = dbConn.getCheckOutSerialNumber();
                    // use a single transaction for it all
                    dbConn.setAutoCommit(false);
                    // store with the values
                    try {
                        store(sysmeta, changeModifyTime, dbConn, sysMetaCheck);
                        // commit if we got here with no errors
                        dbConn.commit();
                    } catch (Exception e) {
                        storeRollBack(pid, e, dbConn, backupCopy);
                        if (e instanceof InvalidRequest ie) {
                            throw ie;
                        }
                        throw new ServiceFailure ("0000", "SystemMetadataManager.store - "
                                + "storing system metadata to the store for " + pid.getValue()
                                + " failed since " + e.getMessage());
                    }
                } catch (SQLException e) {
                    throw new ServiceFailure("0000", 
                            "SystemMetadataManager.store - can't store the system metadata for pid "
                             + pid.getValue() + " since " + e.getMessage());
                } finally {
                    if (dbConn != null) {
                        // Return database connection to the pool
                        try {
                            dbConn.setAutoCommit(true);
                        } catch (SQLException e) {
                            logMetacat.warn("SystemMetadataManager.store - can't set the "
                                            + "auto-commit back to true for the connection since "
                                            + e.getMessage());
                        }
                        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
                    }
                }
            } else {
                throw new InvalidRequest("0000", "SystemMetadataManager.store - the identifier "
                        + "field in system metadata should not be null or blank.");
            }
        } else {
            throw new InvalidRequest("0000", "SystemMetadataManager.store - the system metadata "
                                     + "object should not be null.");
        }
    }

    /**
     * Store a system metadata record into the store.
     * Note: This method is not thread safe. Please put it into a try-finally statement.
     * Before call this method, you need to call the lock method first in the `try` block and
     * unLock method in the `finally` block.
     * The calling code is responsible for (a) setting setAutoCommit(false) before passing the
     * DBConnection, and (b) calling commit() after this method has finished execution.
     * @param sysmeta  the new system metadata will be inserted
     * @param changeModifyTime  if we need to change the modify time
     * @param dbConn  the db connection will be used during storing the system metadata into db
     * @param sysMetaCheck  if Metacat needs to check the version of the coming system metadata
     *                      matching the version of the existing one.
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    public void store(SystemMetadata sysmeta, boolean changeModifyTime, DBConnection dbConn,
                               SysMetaVersion sysMetaCheck) throws InvalidRequest, ServiceFailure {
        if (sysmeta != null) {
            Identifier pid = sysmeta.getIdentifier();
            if (pid != null && pid.getValue() != null && !pid.getValue().trim().equals("")) {
                try {
                    //Check if the system metadata is based on the latest version
                    try {
                        if (sysMetaCheck == SysMetaVersion.CHECKED) {
                            SystemMetadataValidator.hasLatestVersion(sysmeta);
                        }
                    } catch (edu.ucsb.nceas.metacat.systemmetadata.InvalidSystemMetadata e) {
                        String error = "SystemMetadataManager.store - "
                                        + "can't store the system metadata for pid "
                                        + pid.getValue() + " since " + e.getMessage();
                        logMetacat.error(error);
                        throw new InvalidRequest("0000", error);
                    }

                    if (changeModifyTime) {
                        Date now = Calendar.getInstance().getTime();
                        sysmeta.setDateSysMetadataModified(now);
                    }
                    // Get rid of the extra information attached to MCSystemMetadata so Metacat can
                    // store it successfully
                    if (sysmeta instanceof MCSystemMetadata) {
                        sysmeta = MCSystemMetadata.convert((MCSystemMetadata)sysmeta);
                    }
                    logMetacat.debug("SystemMetadataManager.store - storing system metadata "
                                    + "to store: " + pid.getValue());
                    // insert the record if needed
                    if (!IdentifierManager.getInstance().systemMetadataPIDExists(pid.getValue())) {
                        insertSystemMetadata(pid.getValue(), dbConn);
                    }
                    // update with the values
                    updateSystemMetadata(sysmeta, dbConn);
                    // store the system metadata into hashstore
                    storeToHashStore(pid, sysmeta);
                } catch (McdbDocNotFoundException e) {
                    throw new InvalidRequest("0000", "SystemMetadataManager.store - can't "
                                                + "store the system metadata for pid "
                                                + pid.getValue() + " since " + e.getMessage());
                } catch (SQLException e) {
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store "
                                                + "the system metadata for pid " + pid.getValue()
                                                + " since " + e.getMessage());
                } catch (InvalidSystemMetadata e) {
                    throw new InvalidRequest("0000", "SystemMetadataManager.store - can't store "
                                            + "the system metadata for pid " + pid.getValue()
                                            + " since " + e.getMessage());
                } catch (AccessException e) {

                    throw new InvalidRequest("0000", "SystemMetadataManager.store - can't store "
                                                + " the system metadata for pid " + pid.getValue()
                                                + " since " + e.getMessage());
                } catch (RuntimeException e) {
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store "
                                                + "the system metadata for pid " + pid.getValue()
                                                + " since " + e.getMessage());
                } catch (IOException e) {
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store "
                                                + "the system metadata for pid " + pid.getValue()
                                                + " since IOException " + e.getMessage());
                } catch (MarshallingException e) {
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store "
                            + "the system metadata for pid " + pid.getValue()
                            + " since a MarshallingException " + e.getMessage());
                } catch (NoSuchAlgorithmException e) {
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store "
                            + "the system metadata for pid " + pid.getValue()
                            + " since " + e.getMessage());
                } catch (InterruptedException e) {
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store "
                            + "the system metadata for pid " + pid.getValue()
                            + " since " + e.getMessage());
                } catch (InvocationTargetException e) {
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store "
                        + "the system metadata for pid " + pid.getValue()
                        + " since " + e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store "
                        + "the system metadata for pid " + pid.getValue()
                        + " since " + e.getMessage());
                }
            } else {
                throw new InvalidRequest("0000", "SystemMetadataManager.store - the identifier "
                        + "field in system metadata should not be null or blank.");
            }
        } else {
            throw new InvalidRequest("0000", "SystemMetadataManager.store - the system metadata "
                                     + "object should not be null.");
        }
    }

    /**
     * RollBack the change in database and hashtore when the store methods failed
     * @param pid  the pid Metacat would like to save
     * @param e  the exception causes the failure of store.
     * @param conn  the connection used to store system metadata into database
     * @param backupCopies  the original copies of system metadata before Metacat modified them
     */
    public void storeRollBack(Identifier pid, Exception e, DBConnection conn,
                              SystemMetadata... backupCopies) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (Exception ex) {
                logMetacat.error("Storing Systemmetadata for " + pid.getValue() + " failed since "
                                     + e.getMessage()
                                     + " and also the database roll back failed since"
                                     + ex.getMessage());
            }
        }
        // Restore hashstore into previous status
        if (backupCopies != null) {
            for (SystemMetadata backupCopy : backupCopies) {
                if (backupCopy == null) {
                    // Null means there was no system metadata before. So delete it from hashstore
                    try {
                        MetacatInitializer.getStorage().deleteMetadata(pid, MetacatInitializer
                            .getStorage().getDefaultNameSpace());
                    } catch (Exception ex) {
                        logMetacat.error(
                            "Storing system metadata failed for " + pid.getValue()
                                + " since " + e.getMessage()
                                + ". Also Metacat cannot restore the previous status "
                                + "in Hashstore " + " by " + "deleting the system "
                                + "metadata since " + ex.getMessage());
                    }
                } else {
                    //restore the backup copy
                    try {
                        //set changeModifyTime false
                        storeToHashStore(backupCopy.getIdentifier(), backupCopy);
                    } catch (Exception ee) {
                        logMetacat.error(
                            "Storing system metadata failed for " + backupCopy.getIdentifier()
                                .getValue() + " since " + e.getMessage() + ". Also Metacat cannot "
                                + "restore the " + "previous status " + "in "
                                + "Hashstore by restoring the " + "backup system "
                                + "metadata since " + ee.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Store the system metadata into hashstore.
     * @param pid  the pid associated with the system metadata
     * @param sysmeta  the system metadata object will be stored
     * @throws IOException
     * @throws MarshallingException
     * @throws ServiceFailure
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     * @throws IllegalArgumentException
     */
    private void storeToHashStore(Identifier pid, SystemMetadata sysmeta)
        throws IOException, MarshallingException, ServiceFailure, NoSuchAlgorithmException,
        InterruptedException, IllegalArgumentException {
        if (pid == null || pid.getValue().isBlank() || sysmeta == null) {
            throw new IllegalArgumentException(
                "SystemMetadataManager.storeToHashStore - the pid or "
                    + "the system metadata object should not be null.");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            TypeMarshaller.marshalTypeToOutputStream(sysmeta, out);
            byte[] content = out.toByteArray();
            try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
                MetacatInitializer.getStorage().storeMetadata(in, pid);
            }
        }
    }

    /**
     * Lock a PID so only one thread can modify the system metadata in database/file system.
     * Note: put the lock and store method in the try block while the unLock method in the final
     * block.
     * @param pid  the identifier which will be locked
     * @throws RuntimeException
     */
    public void lock(Identifier pid) throws RuntimeException {
        if (pid != null && pid.getValue() != null && !pid.getValue().isBlank()) {
            //Check if there is another thread is storing the system metadata for the same
            //pid. If not, secure the lock; otherwise wait until the lock is available.
            synchronized (lockedIds) {
                while (lockedIds.contains(pid.getValue())) {
                    logMetacat.info("SystemMetadataManager.lock - waiting for the lock "
                                        + " to modify the system metadata for " + pid.getValue());
                    try {
                        lockedIds.wait(TIME_OUT_MILLISEC);
                    } catch (InterruptedException e) {
                        logMetacat.info("SystemMetadataManager.lock - modifying system"
                                            + " metadata in the store: " + pid.getValue()
                                            + ". The lock waiting was interrupted "
                                            + e.getMessage());
                    }
                }
                lockedIds.add(pid.getValue());
            }
        }
    }

    /**
     * Unlock a pid so allow another thread to modify the system metadata in database/file system
     * Note: put this method in the final block while put the lock and store method in the try block
     * @param pid  the identifier which will be unlocked
     */
    public void unLock(Identifier pid) {
        if (pid != null && pid.getValue() != null && !pid.getValue().isBlank()) {
            try {
                // Release the lock
                synchronized (lockedIds) {
                    lockedIds.remove(pid.getValue());
                    lockedIds.notifyAll();
                }
            } catch (RuntimeException e) {
                logMetacat.error(
                    "SystemMetadataManager.unLock - we can't move the id " + pid.getValue()
                        + "from the control list (lockedIds) since " + e.getMessage());
            }
        }
    }

    /**
     * Delete a system metadata record from the store
     * @param id  the identifier to determine the system metadata record
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    public void delete(Identifier id) throws InvalidRequest, ServiceFailure {
            DBConnection dbConn = null;
            int serialNumber = -1;
            try {
                 // Get a database connection from the pool
                dbConn = DBConnectionPool.getDBConnection("SystemMetadataManager.delete");
                serialNumber = dbConn.getCheckOutSerialNumber();
                dbConn.setAutoCommit(false);
                try {
                    delete(id, dbConn);
                    dbConn.commit();
                } catch (Exception e) {
                    try {
                        dbConn.rollback();
                    } catch (SQLException ee) {
                        throw new ServiceFailure("0000", "SystemMetadataManager.delete - "
                                + "the system metadata of guid - " + id.getValue()
                                + " can't be removed successfully since " + e.getMessage()
                                + " Also, the system metadata change can't roll back since "
                                + ee.getMessage());
                    }
                    throw new ServiceFailure("0000", "SystemMetadataManager.delete - "
                            + "the system metadata of guid - " + id.getValue()
                            + " can't be removed successfully since " + e.getMessage());
                }
            } catch (SQLException e) {
                throw new ServiceFailure("0000", "SystemMetadataManager.delete - "
                        + "the system metadata of guid - " + id.getValue()
                        + " can't be removed successfully since " + e.getMessage());
            } finally {
                if (dbConn != null) {
                    try {
                        dbConn.setAutoCommit(true);
                    } catch (SQLException e) {
                        logMetacat.warn("SystemMetadataManager.delete - Metacat can't set the "
                                        + "DBConnection object auto-commit true back.");
                    }
                    // Return database connection to the pool
                    DBConnectionPool.returnDBConnection(dbConn, serialNumber);
                }
            }
    }

    /**
     * create the systemmetadata record
     * @param guid
     * @param dbConn 
     * @throws SQLException 
     */
    private void insertSystemMetadata(String guid, DBConnection dbConn) throws SQLException {
        // Execute the insert statement
        String query = "insert into " + IdentifierManager.TYPE_SYSTEM_METADATA 
                        + " (guid) values (?)";
        try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
            stmt.setString(1, guid);
            logMetacat.debug("system metadata query: " + stmt.toString());
            stmt.executeUpdate();
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
        } catch (Exception e) {
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
    private void insertReplicationPolicy(String guid, String policy, 
                                List<String> memberNodes, DBConnection dbConn) throws SQLException {

        // remove existing values first
        String delete = "delete from smReplicationPolicy " + "where guid = ? and policy = ?";
        try (PreparedStatement stmt = dbConn.prepareStatement(delete)) {
            //data values
            stmt.setString(1, guid);
            stmt.setString(2, policy);
            //execute
            stmt.executeUpdate();
        }


        for (String memberNode: memberNodes) {
            // Execute the insert statement
            String insert = "insert into smReplicationPolicy " + "(guid, policy, member_node) "
                             + "values (?, ?, ?)";
            try (PreparedStatement insertStatement = dbConn.prepareStatement(insert)) {
                //data values
                insertStatement.setString(1, guid);
                insertStatement.setString(2, policy);
                insertStatement.setString(3, memberNode);
                logMetacat.debug("smReplicationPolicy sql: " + insertStatement.toString());
                //execute
                insertStatement.executeUpdate();
            }
        }

    }

    /*
     * Insert the replication status into the database
     */
    private void insertReplicationStatus(String guid, List<Replica> replicas, DBConnection dbConn) 
                                                                            throws SQLException {

        // remove existing values first
        String delete = "delete from smReplicationStatus " + "where guid = ?";
        try (PreparedStatement stmt = dbConn.prepareStatement(delete)) {
          //data values
            stmt.setString(1, guid);
            //execute
            stmt.executeUpdate();
        }

        if (replicas != null) {
            for (Replica replica: replicas) {
                // Execute the insert statement
                String insert = "insert into smReplicationStatus "
                          + "(guid, member_node, status, date_verified) " + "values (?, ?, ?, ?)";
                try (PreparedStatement insertStatement = dbConn.prepareStatement(insert)) {
                    //data values
                    String memberNode = replica.getReplicaMemberNode().getValue();
                    String status = replica.getReplicationStatus().toString();
                    java.sql.Timestamp sqlDate =
                                     new java.sql.Timestamp(replica.getReplicaVerified().getTime());
                    insertStatement.setString(1, guid);
                    insertStatement.setString(2, memberNode);
                    insertStatement.setString(3, status);
                    insertStatement.setTimestamp(4, sqlDate);

                    logMetacat.debug("smReplicationStatus sql: " + insertStatement.toString());
                    //execute
                    insertStatement.executeUpdate();
                }


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
                                    throws McdbDocNotFoundException, AccessException, 
                                           InvalidSystemMetadata, SQLException {

        // check for the existing permOrder so that we remain compatible with 
        //it (DataONE does not care)
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
                            throw new InvalidSystemMetadata("4956", 
                                    "The Permission shouldn't be null. It may result from " 
                                            + "sepcifying a permission by a typo, which is not one " 
                                            + "of read, write and changePermission.");
                        }
                        Long metacatPermission = Long.valueOf(convertPermission(permission));
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

    /**
     * Delete the system metadata for the given guid with the DBConnection object
     * @param guid  the identifier of the object whose system metadata will be deleted
     * @param dbConn  the DBConnection object which will execute the delete actions
     * @throws InvalidRequest
     * @throws SQLException
     */
    public void delete(Identifier guid, DBConnection dbConn) throws InvalidRequest, SQLException {
        if(guid != null && guid.getValue() != null && !guid.getValue().trim().equals("")
                                                                            && dbConn != null) {
            try {
                //Check if there is another thread is storing the system metadata for the same
                //pid. If not, secure the lock; otherwise wait until the lock is available.
                lock(guid);
                logMetacat.debug("SystemMetadataManager.delete - delete the identifier"
                                + guid.getValue());
                // remove the smReplicationPolicy
                String query = "delete from smReplicationPolicy where guid = ?";
                try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                    stmt.setString(1, guid.getValue());
                    logMetacat.debug("delete smReplicationPolicy: " + stmt.toString());
                    stmt.executeUpdate();
                }
                // remove the smReplicationStatus
                query = "delete from smReplicationStatus where guid = ?";
                try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                    stmt.setString(1, guid.getValue());
                    logMetacat.debug("delete smReplicationStatus: " + stmt.toString());
                    stmt.executeUpdate();
                }
                // remove the smmediatypeproperties
                query = "delete from smMediaTypeProperties where guid = ?";
                try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                    stmt.setString(1, guid.getValue());
                    logMetacat.debug("delete smMediaTypeProperties: " + stmt.toString());
                    stmt.executeUpdate();
                }
                // remove the xml_access
                query = "delete from xml_access where guid = ?";
                try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                    stmt.setString(1, guid.getValue());
                    logMetacat.debug("delete xml_access: " + stmt.toString());
                    stmt.executeUpdate();
                }
                // remove main system metadata entry
                query = "delete from " + IdentifierManager.TYPE_SYSTEM_METADATA + " where guid = ? ";
                try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                    stmt.setString(1, guid.getValue());
                    logMetacat.debug("delete system metadata: " + stmt.toString());
                    stmt.executeUpdate();
                }
            } finally {
                unLock(guid);
            }

        } else {
            throw new InvalidRequest("0000", "SystemMetadataManager.delete - the given pid or "
                                    + " the DBConnection object can't be null.");
        }
    }
}
