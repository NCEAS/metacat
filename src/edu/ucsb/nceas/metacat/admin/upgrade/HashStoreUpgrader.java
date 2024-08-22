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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    private File backupDir;

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
        String backup = PropertyService.getProperty("application.backupDir");
        backupDir = new File(backup);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    @Override
    public boolean upgrade() throws AdminException {
        StringBuffer infoBuffer = new StringBuffer();
        ArrayList<Future> futures = new ArrayList<>();
        boolean append = true;
        try {
            File nonMatchingChecksumFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("nonMatchingChecksum"));
            File noSuchAlgorithmFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("noSuchAlgorithm"));
            File generalConvertFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("generalConvertError"));
            File noChecksumInSysmetaFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("noChecksumInSysmeta"));
            File savingChecksumFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("savingChecksumTableError"));
            File generalFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("generalError"));
            try (BufferedWriter noChecksumInSysmetaWriter = new BufferedWriter(new FileWriter(
                                    noChecksumInSysmetaFile, append));
                 BufferedWriter generalWriter = new BufferedWriter(
                     new FileWriter(generalFile, append))) {
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
                                        logMetacat.debug("Trying to convert the storage to hashstore"
                                                             + " for " + id + " with checksum: "
                                                             + checksum + " algorithm: " + algorithm
                                                             + " and file path(may be null): "
                                                             + path.toString());
                                        Future<?> future = executor.submit(() -> {
                                            convert(path, finalId, sysMetaInput, checksum,
                                                    algorithm, nonMatchingChecksumFile,
                                                    noSuchAlgorithmFile, generalConvertFile,
                                                    savingChecksumFile);
                                        });
                                        futures.add(future);
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
                                writeToFile(id, e, generalWriter);
                            }
                        }
                    }
                } catch (SQLException e) {
                    logMetacat.error("Error while going through the systemmetadata table: "
                                         + e.getMessage());
                    throw new AdminException(e.getMessage());
                }
                for (Future future : futures) {
                    while (!future.isDone()) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            logMetacat.warn("Waiting future is interrupted " + e.getMessage());
                        }
                    }
                }
                handleErrorFile(nonMatchingChecksumFile, infoBuffer);
                handleErrorFile(noSuchAlgorithmFile, infoBuffer);
                handleErrorFile(generalFile, infoBuffer);
                handleErrorFile(noChecksumInSysmetaFile, infoBuffer);
                handleErrorFile(savingChecksumFile, infoBuffer);
                handleErrorFile(generalConvertFile, infoBuffer);
                if (!infoBuffer.isEmpty()) {
                    infoBuffer.append("The conversion succeeded! However, some objects failed to be "
                                          + "converted. The above files contain those identifiers. "
                                          + "Please try to fix the issues.");
                }
                this.info = infoBuffer.toString();
            }
        } catch (IOException e) {
           logMetacat.error("Can not create the files to log the failed ids: "
                                + e.getMessage());
           throw new AdminException(e.getMessage());
        }
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
        PreparedStatement stmt = null;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("HashStoreUpgrader.initCandidateList");
            serialNumber = dbConn.getCheckOutSerialNumber();
            stmt = dbConn.prepareStatement(query); // can't use the try-resource statement
            rs = stmt.executeQuery();
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            if (stmt != null) {
                try {
                    stmt.closeOnCompletion();
                } catch (SQLException e) {
                    logMetacat.warn("Can't close the query statement: " + e.getMessage());
                }
            }
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
            path = Paths.get(dataPath + localId);
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

    protected void writeToFile(String message, Exception e,  BufferedWriter writer) {
        try {
            if (writer != null) {
                writer.write(message + " " + e.getMessage() + "\n");
            }
        } catch (Exception ee) {
            logMetacat.warn("Can not write the error message to a file since " + ee.getMessage());
        }
    }

    protected static InputStream convertSystemMetadata(SystemMetadata sysMeta)
        throws IOException, MarshallingException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            TypeMarshaller.marshalTypeToOutputStream(sysMeta, out);
            byte[] content = out.toByteArray();
            return new ByteArrayInputStream(content);
        }
    }

    private void handleErrorFile(File file, StringBuffer buffer) {
        if (file != null && file.exists()) {
            try {
                if (file.length() > 0) {
                    buffer.append(file.getCanonicalPath() + "\n");
                } else {
                    file.delete();
                }
            } catch (Exception e) {
                logMetacat.warn("There is an error to handle the error file ");
            }
        }
    }

    private void convert(Path path, String finalId, InputStream sysMetaInput, String checksum,
                         String algorithm, File nonMatchingChecksumFile, File noSuchAlgorithmFile,
                         File generalConvertFile, File savingChecksumFile) {
        boolean append = true;
        ObjectMetadata metadata = null;
        try (BufferedWriter nonMatchingChecksumWriter =
                 new BufferedWriter(new FileWriter(nonMatchingChecksumFile, append));
             BufferedWriter noSuchAlgorithmWriter =
                 new BufferedWriter(new FileWriter(noSuchAlgorithmFile, append));
             BufferedWriter generalWriter =
                 new BufferedWriter(new FileWriter(generalConvertFile, append))) {
            try {
                metadata =
                    converter.convert(path, finalId, sysMetaInput,
                                      checksum, algorithm);
            } catch (NonMatchingChecksumException e) {
                logMetacat.warn("Cannot move the object " + finalId
                                    + "to hashstore since "
                                    + e.getMessage());
                writeToFile(finalId, nonMatchingChecksumWriter);
            } catch (NoSuchAlgorithmException e) {
                logMetacat.warn("Cannot move the object " + finalId
                                    + " to hashstore since "
                                    + e.getMessage());
                writeToFile(finalId, noSuchAlgorithmWriter);
            } catch (Exception e) {
                logMetacat.warn("Cannot move the object " + finalId
                                    + " to hashstore since "
                                    + e.getMessage());
                writeToFile(finalId, e, generalWriter);
            }
        } catch (IOException io) {
            logMetacat.error("Cannot open log file writer for " + finalId + " since "
                                 + io.getMessage());
        }

        // Save the checksums into checksum table
        DBConnection dbConn = null;
        int serialNumber = -1;
        try (BufferedWriter savingChecksumTableWriter =
                 new BufferedWriter(new FileWriter(savingChecksumFile, append))) {
            try {
                // Get a database connection from the pool
                dbConn = DBConnectionPool.getDBConnection(
                    "HashStoreUpgrader.upgrade");
                serialNumber = dbConn.getCheckOutSerialNumber();
                if (metadata != null) {
                    Identifier identifier = new Identifier();
                    identifier.setValue(finalId);
                    checksumsManager.save(
                        identifier, metadata.hexDigests(), dbConn);
                }
            } catch (Exception e) {
                logMetacat.warn(
                    "Cannot save checksums for " + finalId
                        + " since " + e.getMessage());
                writeToFile(finalId, savingChecksumTableWriter);
            } finally {
                DBConnectionPool.returnDBConnection(
                    dbConn, serialNumber);
            }
        } catch (IOException io) {
            logMetacat.error("Cannot open log file writer for " + finalId + " to save into "
                                 + "the checksum table since " + io.getMessage());
        }
    }

}
