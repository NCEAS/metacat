package edu.ucsb.nceas.metacat.systemmetadata;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The class to save, get and query checksums from db for a pid
 * @author Tao
 */
public class ChecksumsManager {
    private static Log logMetacat = LogFactory.getLog(ChecksumsManager.class);

    /**
     * Save the checksums into database. The callers need to provide the database connection
     * @param id  the identifier which has the checksums
     * @param checksums  the checksums map whose key is algorithm and value is the checksum value
     * @param dbConn  the connection used to connect database
     * @throws SQLException
     * @throws ServiceFailure
     */
    public void save(Identifier id, Map<String, String> checksums, DBConnection dbConn)
        throws SQLException, ServiceFailure {
        if (id != null && !id.getValue().isBlank()) {
            String pid = id.getValue();
            if (checksums != null) {
                String query = "INSERT INTO checksums (guid, checksum_algorithm, checksum) values"
                    + " (?, ?, ?);";
                try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                    for (String algorithm : checksums.keySet()) {
                        if (algorithm != null && !algorithm.isBlank()
                            && checksums.get(algorithm) != null && !checksums.get(algorithm)
                            .isBlank()) {
                            // Execute the insert statement. It will continue even though one
                            // fails
                            try {
                                stmt.setString(1, pid);
                                stmt.setString(2, algorithm);
                                stmt.setString(3, checksums.get(algorithm));
                                stmt.execute();
                                logMetacat.debug(
                                    "Save the checksum " + checksums.get(algorithm) + " with"
                                        + " algorithm " + algorithm + " for " + pid + " into "
                                        + "db.");
                            } catch (Exception e) {
                                throw new ServiceFailure
                                ("0000", "Metacat cannot save the checksum " + checksums.get(
                                        algorithm) + " with algorithm " + algorithm
                                        + " for " + pid + " into db since "
                                        + e.getMessage());
                            }
                        }
                    }
                }
            } else {
                throw new ServiceFailure("0000", "The checksums from the storage system shouldn't be "
                    + "null");
            }
        }
    }


    /**
     * Find all checksums for the given pid. An empty list will be returned if nothing was found.
     * @param pid  the identifier to identify the checksum
     * @return a list of checksums for the pid. An empty list will be returned if nothing was found.
     * @throws InvalidRequest
     * @throws SQLException
     */
    public List<Checksum> get(Identifier pid) throws InvalidRequest, SQLException {
        if (pid == null || pid.getValue().isBlank()) {
            throw new InvalidRequest(
                "0000", "ChecksumsManager.get - the pid should not be blank in the request");
        }
        List<Checksum> list = new ArrayList<>();
        String query = "SELECT checksum_algorithm, checksum from checksums where guid=?";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("ChecksumsManager.get");
            serialNumber = dbConn.getCheckOutSerialNumber();
            try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                stmt.setString(1, pid.getValue());
                try (ResultSet resultSet = stmt.executeQuery()) {
                    while (resultSet.next()) {
                        String algorithm = resultSet.getString(1);
                        String checksum_value = resultSet.getString(2);
                        if (algorithm != null && !algorithm.isBlank() && checksum_value != null
                            && !checksum_value.isBlank()) {
                            Checksum checksum = new Checksum();
                            checksum.setAlgorithm(algorithm);
                            checksum.setValue(checksum_value);
                            list.add(checksum);
                            logMetacat.debug(
                                "Metacat found the checksum " + checksum_value + " " + "with"
                                    + "checksum algorithm " + algorithm + " for the " + "object "
                                    + pid.getValue());
                        }
                    }
                }
            }
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return list;
    }

    /**
     * Find the list of pid which has the checksum matching the given value. An empty list will
     * be returned if no matching.
     * @param checksum  the checksum should be matched
     * @return  the list of pid matching the given checksum. An empty list will be returned if no
     * matching
     * @throws InvalidRequest
     * @throws SQLException
     */
    public List<Identifier> query(Checksum checksum) throws InvalidRequest, SQLException {
        if (checksum == null || checksum.getAlgorithm() == null || checksum.getAlgorithm().isBlank()
            || checksum.getValue() == null || checksum.getValue().isBlank()) {
            throw new InvalidRequest("0000", "ChecksumsManager.query - the given checksum should "
                + "not be null or the algorithm/value pair should not be blank");
        }
        List<Identifier> list = new ArrayList<>();
        String query = "SELECT guid from checksums where checksum_algorithm=? and checksum=?;";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("ChecksumsManager.query");
            serialNumber = dbConn.getCheckOutSerialNumber();
            try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                stmt.setString(1, checksum.getAlgorithm());
                stmt.setString(2, checksum.getValue());
                try (ResultSet resultSet = stmt.executeQuery()) {
                    while (resultSet.next()) {
                        String guid = resultSet.getString(1);
                        if (guid != null && !guid.isBlank()) {
                            Identifier pid = new Identifier();
                            pid.setValue(guid);
                            list.add(pid);
                            logMetacat.debug("Metacat found the id " + guid + " " + "with checksum "
                                                 + checksum.getValue() + " and checksum algorithm "
                                                 + checksum.getAlgorithm());
                        }
                    }
                }
            }
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return list;
    }

    /**
     * Delete the checksums from the table for the given pid
     * @param pid  the pid which will be deleted
     * @param conn  the connection which will be used to delete
     * @throws SQLException
     */
    public void delete(Identifier pid, DBConnection conn) throws SQLException {
        if (pid != null && !pid.getValue().isBlank()) {
            String query = "DELETE FROM checksums where guid=?;";
            try (PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, pid.getValue());
                pst.execute();
            }
        }
    }

}
