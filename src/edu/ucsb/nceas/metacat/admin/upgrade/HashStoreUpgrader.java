package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.systemmetadata.ChecksumsManager;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.DatabaseUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Tao
 * This class will upgrade the old metacat file storage (/var/metacat/documents and
 * /var/metacat/data) into the HashStore storage.
 */
public class HashStoreUpgrader implements UpgradeUtilityInterface {
    private final static int TIME_OUT = 5;
    private static Log logMetacat = LogFactory.getLog(HashStoreUpgrader.class);
    private String dataPath;
    private String documentPath;
    private String info = null;
    private static HashStoreConverter converter;
    private ChecksumsManager checksumsManager = new ChecksumsManager();
    private File backupDir;
    private static int timeout = TIME_OUT;
    private static int nThreads = 1;

    static {
        // use a shared executor service with nThreads == one less than available processors or one
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        nThreads = availableProcessors;
        nThreads--;
        nThreads = Math.max(1, nThreads);
        logMetacat.debug("The size of the thread pool to do the conversion job is " + nThreads);

    }

    /**
     * Constructor
     * @throws PropertyNotFoundException
     * @throws ServiceFailure
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws ServiceException
     * @throws MetacatUtilException
     * @throws GeneralPropertyException
     */
    public HashStoreUpgrader()
        throws GeneralPropertyException, ServiceFailure, IOException, NoSuchAlgorithmException,
        ServiceException, MetacatUtilException, PropertyNotFoundException {
        try {
            MetacatInitializer.getStorage();
        } catch (ServiceFailure e) {
            if (DatabaseUtil.isDatabaseConfigured() && PropertyService.arePropertiesConfigured()) {
                MetacatInitializer.initStorage();
            } else {
                throw e;
            }
        }
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
        String timeoutStr = null;
        try {
            timeoutStr = PropertyService.getProperty("storage.hashstore.converterTimeout");
            timeout = Integer.parseInt(timeoutStr);
        } catch (PropertyNotFoundException e) {
            logMetacat.debug("Metacat doesn't set timeout and it uses the default one - "
                                 + TIME_OUT);
        } catch (NumberFormatException e) {
            logMetacat.warn(
                "Metacat sets wrong timeout " + timeoutStr + ", so it uses the default one - "
                    + TIME_OUT);
        }
    }

    @Override
    public boolean upgrade() throws AdminException {
        ExecutorService executor;
        logMetacat.debug("It is ready for the conversion. The timeout for the executor service is "
                             + timeout);
        StringBuffer infoBuffer = new StringBuffer();
        boolean append = true;
        try {
            executor = Executors.newFixedThreadPool(nThreads);
            File nonMatchingChecksumFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("nonMatchingChecksum"));
            File noSuchAlgorithmFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("noSuchAlgorithm"));
            File noChecksumInSysmetaFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("noChecksumInSysmeta"));
            File savingChecksumFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("savingChecksumTableError"));
            File generalFile =
                new File(backupDir, XMLNodesToFilesChecker.getFileName("generalError"));
            try (BufferedWriter noChecksumInSysmetaWriter = new BufferedWriter(new FileWriter(
                                    noChecksumInSysmetaFile, append));
                 BufferedWriter generalWriter = new BufferedWriter(
                     new FileWriter(generalFile, append));
                 BufferedWriter nonMatchingChecksumWriter =
                     new BufferedWriter(new FileWriter(nonMatchingChecksumFile, append));
                BufferedWriter noSuchAlgorithmWriter =
                    new BufferedWriter(new FileWriter(noSuchAlgorithmFile, append));
                 BufferedWriter savingChecksumTableWriter =
                     new BufferedWriter(new FileWriter(savingChecksumFile, append))) {
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
                                            convert(path, finalId, sysMeta, checksum,
                                                    algorithm, nonMatchingChecksumWriter,
                                                    noSuchAlgorithmWriter, generalWriter,
                                                    savingChecksumTableWriter);
                                        });
                                    } else {
                                        logMetacat.error("There is no checksum info for id " + id +
                                                            " in the systemmetadata and Metacat "
                                                            + "cannot convert it to hashstore.");
                                        writeToFile(id, noChecksumInSysmetaWriter);
                                    }
                                }
                            } catch (Exception e) {
                                logMetacat.error("Cannot move the object " + id + " to hashstore "
                                                  + "since " + e.getMessage(), e);
                                writeToFile(id, e, generalWriter);
                            }
                        }
                    }
                } catch (SQLException e) {
                    logMetacat.error("Error while going through the systemmetadata table: "
                                         + e.getMessage());
                    throw new AdminException(e.getMessage());
                }
                // Shut down the executor service and wait the submitted jobs to be completed
                executor.shutdown();
                try {
                    //Based on the java doc:
                    //Blocks until all tasks have completed execution after a shutdown request,
                    // or the timeout occurs, or the current thread is interrupted, whichever
                    // happens first. So five days doesn't mean it will wait five days.
                    executor.awaitTermination(timeout, TimeUnit.DAYS);
                } catch (InterruptedException e) {
                    throw new RuntimeException("The waiting of completeness of the executor's "
                                                   + "jobs was interrupted: " + e.getMessage());
                }
            }
            handleErrorFile(nonMatchingChecksumFile, infoBuffer);
            handleErrorFile(noSuchAlgorithmFile, infoBuffer);
            handleErrorFile(generalFile, infoBuffer);
            handleErrorFile(noChecksumInSysmetaFile, infoBuffer);
            handleErrorFile(savingChecksumFile, infoBuffer);
            this.info = infoBuffer.toString();
        } catch (IOException e) {
           logMetacat.error("Can not create the files to log the failed ids: "
                                + e.getMessage());
           throw new AdminException(e.getMessage());
        } catch (RuntimeException e) {
            logMetacat.error("There was a runtime exception: "
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

    protected static void writeToFile(String message, BufferedWriter writer) {
        try {
           if (writer != null) {
               writer.write(message + "\n");
           }
        } catch (Exception e) {
            logMetacat.warn("Can not write the error message to a file since " + e.getMessage());
        }
    }

    protected static void writeToFile(String message, Exception e,  BufferedWriter writer) {
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
                    buffer.append(file.getCanonicalPath() + "<br>");
                } else {
                    file.delete();
                }
            } catch (Exception e) {
                logMetacat.warn("There is an error to handle the error file ");
            }
        }
    }

    private void convert(Path path, String finalId, SystemMetadata sysMeta, String checksum,
                         String algorithm, BufferedWriter nonMatchingChecksumWriter,
                         BufferedWriter noSuchAlgorithmWriter,
                         BufferedWriter generalWriter, BufferedWriter savingChecksumTableWriter) {
        ObjectMetadata metadata = null;
        try {
            try (InputStream sysMetaInput = convertSystemMetadata(sysMeta)) {
                metadata =
                    converter.convert(path, finalId, sysMetaInput, checksum, algorithm);
            }
        } catch (NonMatchingChecksumException e) {
            logMetacat.error("Cannot move the object " + finalId + "to hashstore since "
                                + e.getMessage());
            writeToFile(finalId, nonMatchingChecksumWriter);
        } catch (NoSuchAlgorithmException e) {
            logMetacat.error("Cannot move the object " + finalId + " to hashstore since "
                                + e.getMessage());
            writeToFile(finalId, noSuchAlgorithmWriter);
        } catch (Exception e) {
            logMetacat.error("Cannot move the object " + finalId + " to hashstore since "
                                + e.getMessage(), e);
            writeToFile(finalId, e, generalWriter);
        }
        // Save the checksums into checksum table
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("HashStoreUpgrader.convert");
            serialNumber = dbConn.getCheckOutSerialNumber();
            if (metadata != null) {
                Identifier identifier = new Identifier();
                identifier.setValue(finalId);
                checksumsManager.save(identifier, metadata.hexDigests(), dbConn);
            }
        } catch (Exception e) {
            logMetacat.error("Cannot save checksums for " + finalId
                    + " since " + e.getMessage(), e);
            writeToFile(finalId, e, savingChecksumTableWriter);
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

}
