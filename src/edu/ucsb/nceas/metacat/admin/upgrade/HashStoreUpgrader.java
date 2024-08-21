package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.systemmetadata.ChecksumsManager;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.exceptions.MarshallingException;
import org.dataone.hashstore.ObjectMetadata;
import org.dataone.hashstore.exceptions.NonMatchingChecksumException;
import org.dataone.hashstore.hashstoreconverter.HashStoreConverter;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Tao
 * This class will upgrade the old metacat file storage (/var/metacat/documents and
 * /var/metacat/data) into the HashStore storage.
 */
public class HashStoreUpgrader implements UpgradeUtilityInterface {
    private static Log logMetacat = LogFactory.getLog(HashStoreUpgrader.class);
    private String dataPath;
    private String documentPath;
    private String info = null;
    private static ExecutorService executor;
    private static HashStoreConverter converter;
    private ChecksumsManager checksumsManager = new ChecksumsManager();
    private BufferedWriter nonMatchingChecksumWriter;
    private BufferedWriter noSuchAlgorithmWriter;
    private BufferedWriter generalWriter;
    private BufferedWriter noChecksumInSysmetaWriter;
    private BufferedWriter savingChecksumTableWriter;

    static {
        // use a shared executor service with nThreads == one less than available processors or one
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int nThreads = availableProcessors;
        nThreads--;
        nThreads = Math.max(1, nThreads);
        logMetacat.debug("The size of the thread pool to do the conversion job is " + nThreads);
        executor = Executors.newFixedThreadPool(nThreads);
    }

    /**
     * Constructor
     * @throws PropertyNotFoundException
     */
    public HashStoreUpgrader()
        throws PropertyNotFoundException, ServiceFailure, IOException, NoSuchAlgorithmException {
        converter = new HashStoreConverter(MetacatInitializer.getStorage().getStoreProperties());
        documentPath = PropertyService.getProperty("application.documentfilepath");
        if (!documentPath.endsWith("/")) {
            documentPath = documentPath + "/";
        }
        logMetacat.debug("The document directory path of Metacat is " + documentPath);
        dataPath = PropertyService.getProperty("application.datafilepath");
        if (!dataPath.endsWith("/")) {
            dataPath = dataPath + "/";
        }
        logMetacat.debug("The data directory path of Metacat is " + dataPath);
        initErrorWriter();

    }

    private void initErrorWriter() {
        boolean append = true;
        try {
            String backupDir = PropertyService.getProperty("application.backupDir");
            File backup = new File(backupDir);
            if (!backup.exists()) {
                backup.mkdirs();
            }
            nonMatchingChecksumWriter = new BufferedWriter(new FileWriter(
              new File(backup, XMLNodesToFilesChecker.getFileName("nonMatchingChecksum")), append));
            noSuchAlgorithmWriter = new BufferedWriter(new FileWriter(
                new File(backup, XMLNodesToFilesChecker.getFileName("noSuchAlgorithm")), append));
            generalWriter = new BufferedWriter(new FileWriter(
                new File(backup, XMLNodesToFilesChecker.getFileName("generalError")), append));
            noChecksumInSysmetaWriter = new BufferedWriter(new FileWriter(
              new File(backup, XMLNodesToFilesChecker.getFileName("noChecksumInSysmeta")), append));
            savingChecksumTableWriter = new BufferedWriter(new FileWriter(
                new File(backup, XMLNodesToFilesChecker.getFileName("savingChecksumTableError")),
                append));
        } catch (Exception e) {
            logMetacat.error(
                "Cannot initialize the error writer for HashStoreUpgrader " + e.getMessage());
        }
    }

    @Override
    public boolean upgrade() throws AdminException {
        StringBuffer infoBuffer = new StringBuffer();
        try {
            try (ResultSet rs = initCandidateList()) {
                if (rs != null) {
                    while (rs.next()) {
                        String id = null;
                        try {
                            id = rs.getString(1);
                            if (id != null && !id.isBlank()) {
                                final String finalId = id;
                                Identifier pid = new Identifier();
                                pid.setValue(finalId);
                                SystemMetadata sysMeta =
                                    SystemMetadataManager.getInstance().get(pid);
                                Path path = resolve(sysMeta); // it may be null
                                InputStream sysMetaInput = convertSystemMetadata(sysMeta);
                                if (sysMeta.getChecksum() != null) {
                                    String checksum = sysMeta.getChecksum().getValue();
                                    String algorithm =
                                        sysMeta.getChecksum().getAlgorithm().toUpperCase();
                                    logMetacat.debug("Trying to convert the storage to hashstore "
                                                         + "for " + id + " with checksum: "
                                                         + checksum + " algorithm: " + algorithm
                                                         + " and file path(may be null): "
                                                         + path.toString());
                                    executor.submit(() -> {
                                        ObjectMetadata metadata = null;
                                        try {
                                            metadata = converter.convert(path, finalId, sysMetaInput,
                                                                  checksum, algorithm);
                                        } catch (NonMatchingChecksumException e) {
                                            logMetacat.warn("Cannot move the object " + finalId
                                                                + "to hashstore since "
                                                                + e.getMessage());
                                            writeToFile(finalId, nonMatchingChecksumWriter);
                                        } catch (NoSuchAlgorithmException e) {
                                            logMetacat.warn("Cannot move the object " + finalId +
                                                                " to hashstore since "
                                                                + e.getMessage());
                                            writeToFile(finalId, noSuchAlgorithmWriter);
                                        } catch (Exception e) {
                                            logMetacat.warn("Cannot move the object " + finalId +
                                                                " to hashstore since "
                                                                + e.getMessage());
                                            writeToFile(finalId, generalWriter);
                                        }
                                        DBConnection dbConn = null;
                                        int serialNumber = -1;
                                        try {
                                            // Get a database connection from the pool
                                            dbConn = DBConnectionPool.getDBConnection(
                                                "HashStoreUpgrader.upgrade");
                                            serialNumber = dbConn.getCheckOutSerialNumber();
                                            if (metadata != null) {
                                                checksumsManager.save(
                                                    pid, metadata.hexDigests(), dbConn);
                                            }
                                        } catch (Exception e) {
                                            logMetacat.warn(
                                                "Cannot save checksums for " + finalId + " since "
                                                    + e.getMessage());
                                            writeToFile(finalId, savingChecksumTableWriter);
                                        } finally {
                                            DBConnectionPool.returnDBConnection(
                                                dbConn, serialNumber);
                                        }
                                    });
                                } else {
                                    logMetacat.warn("There is no checksum info for id " + id +
                                                        " in the systemmetadata and Metacat "
                                                        + "cannot convert it to hashstore.");
                                    writeToFile(id, noChecksumInSysmetaWriter);
                                }
                            }
                        } catch (Exception e) {
                            logMetacat.warn("Cannot move the object " + id + " to hashstore since "
                                                 + e.getMessage());
                            writeToFile(id, generalWriter);
                        }
                    }

                }
            }
        } catch (SQLException e) {
           logMetacat.error("Error while going through the systemmetadata table: "
                                + e.getMessage());
           throw new AdminException(e.getMessage());
        }
        this.info = infoBuffer.toString();
        return true;
    }

    /**
     * Run a query to select the list of candidate pid from the systemmetadata which will be
     * converted. If the pid exists in the checksums table, we consider it is in the hashstore and
     * wouldn't convert it. Both the checksums table and hashstore are introduced in 3.1.0.
     * @return a ResultSet object which contains the list of identifiers:
     * @throws SQLException
     */
    protected ResultSet initCandidateList() throws SQLException {
        // Iterate the systemmetadata table
        String query =
            "SELECT s.guid FROM systemmetadata s LEFT JOIN checksums c ON s.guid = c.guid WHERE "
                + "c.guid IS NULL;";
        DBConnection dbConn = null;
        ResultSet rs = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("HashStoreUpgrader.initCandidateList");
            serialNumber = dbConn.getCheckOutSerialNumber();
            try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                rs = stmt.executeQuery();
            }
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return rs;
    }

    /**
     * Get the info which users need to handle
     * @return the info string generated by the update method
     */
    public String getInfo() {
        return this.info;
    }

    /**
     * Get the object path for the given pid
     * @param sysMeta  the system metadata associated with the object
     * @return  the object path. Null will be returned if there is no object found.
     * @throws SQLException
     */
    protected Path resolve(SystemMetadata sysMeta ) throws SQLException {
        Identifier pid = sysMeta.getIdentifier();
        String localId;
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        } catch (McdbDocNotFoundException e) {
            return null;
        }
        Path path;
        if (D1NodeService.isScienceMetadata(sysMeta)) {
            path = Paths.get(documentPath + localId);
        } else {
            path = Paths.get(documentPath + localId);
        }
        logMetacat.debug("The object path for " + pid.getValue() + " is " + path.toString());
        return path;
    }

    protected void writeToFile(String message, BufferedWriter writer) {
        try {
           if (writer != null) {
               writer.write(message + "\n");
           }
        } catch (Exception e) {
            logMetacat.warn("Can not write the error message to a file since " + e.getMessage());
        }
    }

    private InputStream convertSystemMetadata(SystemMetadata sysMeta)
        throws IOException, MarshallingException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            TypeMarshaller.marshalTypeToOutputStream(sysMeta, out);
            byte[] content = out.toByteArray();
            return new ByteArrayInputStream(content);
        }
    }
}
