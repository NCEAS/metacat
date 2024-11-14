package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.SystemMetadataFactory;
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
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private final static int TIME_OUT_DAYS = 5;
    private final static int MAX_SET_SIZE = 10000;
    private static Log logMetacat = LogFactory.getLog(HashStoreUpgrader.class);
    private String dataPath;
    private String documentPath;
    private String info = null;
    private static HashStoreConverter converter;
    private ChecksumsManager checksumsManager = new ChecksumsManager();
    private File backupDir;
    private static int timeout = TIME_OUT_DAYS;
    private int maxSetSize = MAX_SET_SIZE;
    protected static int nThreads;
    private static boolean isCN = false;

    static {
        // use a shared executor service with nThreads == one less than available processors (or one
        // if there's only 1 processor), and limited by the max number of database connections.
        int availDbConn;
        try {
            // Limit to DB conn pool size minus 5 for other processes
            availDbConn =
                Integer.parseInt(PropertyService.getProperty("database.maximumConnections")) - 5;
            availDbConn = Math.max(1, availDbConn); // In case "database.maximumConnections" < 6
        } catch (PropertyNotFoundException e) {
            logMetacat.warn(
                "unable to find database.maximumConnections property!"
                + "Defaulting available DN connections to 195", e);
            availDbConn = 195;
        }
        nThreads = Runtime.getRuntime().availableProcessors();
        nThreads--;                                 // Leave 1 main thread for execution
        nThreads = Math.max(1, nThreads);           // In case only 1 processor is available
        nThreads = Math.min(availDbConn, nThreads); // Limit to available DB pool connections
        logMetacat.debug("The size of the thread pool to do the conversion job is " + nThreads);
        logMetacat.debug("Available DB Connections were (tot - 5): " + availDbConn);
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
            timeoutStr = PropertyService.getProperty("storage.hashstore.converterTimeoutDays");
            timeout = Integer.parseInt(timeoutStr);
        } catch (PropertyNotFoundException e) {
            logMetacat.debug("Metacat doesn't set timeout and it uses the default one - "
                                 + TIME_OUT_DAYS);
        } catch (NumberFormatException e) {
            logMetacat.warn(
                "Metacat sets wrong timeout " + timeoutStr + ", so it uses the default one - "
                    + TIME_OUT_DAYS);
        }
        String sizeStr = null;
        try {
            sizeStr = PropertyService.getProperty("storage.hashstore.converterSetSize");
            maxSetSize = Integer.parseInt(sizeStr);
        } catch (PropertyNotFoundException e) {
            logMetacat.debug("Metacat doesn't set the set size and it uses the default one - "
                                 + MAX_SET_SIZE);
        } catch (NumberFormatException e) {
            logMetacat.debug(
                "Metacat sets a wrong set size " + sizeStr + " and it uses the default one - "
                    + MAX_SET_SIZE);
        }
        try {
            String nodeType = PropertyService.getProperty("dataone.nodeType");
            if (nodeType != null && nodeType.equalsIgnoreCase("cn")) {
                logMetacat.debug("The node type is " + nodeType + ". So it is a CN");
                isCN = true;
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("The dataone.nodeType property can't be found, so we assume it is "
                                 + "not a cn");
        }
    }

    @Override
    public boolean upgrade() throws AdminException {
        ExecutorService executor;
        logMetacat.debug("It is ready for the conversion. The timeout for the executor service is "
                             + timeout +" and the max size of the future set is " + maxSetSize);
        long start = System.currentTimeMillis();
        StringBuffer infoBuffer = new StringBuffer();
        boolean append = true;
        Set<Future> futures = Collections.synchronizedSet(new HashSet<>());
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
                                    if (sysMeta == null) {
                                        // This is for the case that the object somehow hasn't been
                                        // transformed to the DataONE object: no system metadata
                                        // This method does not only create the system metadata,
                                        // but also create the map in the identifier table.
                                        sysMeta =
                                            SystemMetadataFactory.createSystemMetadata(finalId);
                                        try {
                                            SystemMetadataManager.lock(pid);
                                            SystemMetadataManager.getInstance().store(sysMeta);
                                        } finally {
                                            SystemMetadataManager.unLock(pid);
                                        }
                                    }
                                    if (sysMeta != null) {
                                        SystemMetadata finalSysMeta = sysMeta;
                                        Path path = resolve(finalSysMeta); // it may be null for cn
                                        if (finalSysMeta.getChecksum() != null) {
                                            String checksum = finalSysMeta.getChecksum().getValue();
                                            String algorithm =
                                                finalSysMeta.getChecksum().getAlgorithm()
                                                    .toUpperCase();
                                            logMetacat.debug(
                                                "Trying to convert the storage to hashstore"
                                                    + " for " + id + " with checksum: " + checksum
                                                    + " algorithm: " + algorithm
                                                    + " and file path(may be null): "
                                                    + path.toString());
                                            Future<?> future = executor.submit(() -> {
                                                convert(path, finalId, finalSysMeta, checksum,
                                                        algorithm, nonMatchingChecksumWriter,
                                                        noSuchAlgorithmWriter, generalWriter,
                                                        savingChecksumTableWriter);
                                            });
                                            futures.add(future);
                                            if (futures.size() >= maxSetSize) {
                                                //When it reaches the max size, we need to remove
                                                // the complete futures from the set. So the set can
                                                // be reused again. So we can avoid the issue of
                                                // out of memory.
                                                removeCompleteFuture(futures);
                                            }
                                        } else {
                                            logMetacat.error(
                                                "There is no checksum info for id " + id
                                                    + " in the systemmetadata and Metacat "
                                                    + "cannot convert it to hashstore.");
                                            writeToFile(id, noChecksumInSysmetaWriter);
                                        }
                                    } else {
                                        String error = "The id " + id + " can't be converted"
                                            + " to the hashstore since its system metadata is null";
                                        logMetacat.error(error);
                                        Exception e = new Exception(error);
                                        writeToFile(id, e, generalWriter);
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
            // Add a comment at the end of the non-matching checksum file
            if (nonMatchingChecksumFile != null && nonMatchingChecksumFile.exists()
                && nonMatchingChecksumFile.length() > 0) {
                String message = "If this instance is a registered member node of the DataONE "
                    + "network, please submit the file to knb-help@nceas.ucsb.edu so can we make "
                    + "changes on the CN part as well.";
                try (BufferedWriter noChecksumInSysmetaWriter2 = new BufferedWriter(new FileWriter(
                    noChecksumInSysmetaFile, append))) {
                    writeToFile(message, noChecksumInSysmetaWriter2);
                }
            }
            handleErrorFile(nonMatchingChecksumFile, infoBuffer);
            handleErrorFile(noSuchAlgorithmFile, infoBuffer);
            handleErrorFile(generalFile, infoBuffer);
            handleErrorFile(noChecksumInSysmetaFile, infoBuffer);
            handleErrorFile(savingChecksumFile, infoBuffer);
            this.info = infoBuffer.toString();
            long end = System.currentTimeMillis();
            logMetacat.info("The conversion took " + (end - start)/60000 + " minutes.");
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
     * This method removes the Future objects from a set which have the done status.
     * So the free space can be used again. If it cannot remove any one, it will wait and try
     * again until some space was freed up.
     * @param futures  the set which hold the futures to be checked
     */
    protected void removeCompleteFuture(Set<Future> futures) {
        if (futures != null) {
            int originalSize = futures.size();
            if (originalSize > 0) {
                //A 100GB file takes 15 minutes to convert, and 800GB takes 2 hours. Although the
                // method cannot remove futures after 2 hours tries, new tasks can only be
                // submitted every 2 hours when the set limit is reached, which slows the
                // addition of futures and prevents out-of-memory issues.
                for (int i = 0; i < 7200; i++) {
                    futures.removeIf(Future::isDone);
                    if (futures.size() >= originalSize) {
                        logMetacat.debug("Metacat could not remove any complete futures and will "
                                             + "wait for a while and try again.");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            logMetacat.warn("Waiting future is interrupted " + e.getMessage());
                        }
                    } else {
                        logMetacat.debug("Metacat removed some complete futures from the set.");
                        break;
                    }
                }
            }
        }
    }


    /**
     * Run a query to select the list of candidate pid from the systemmetadata which will be
     * converted. If the pid exists in the checksums table, we consider it is in the hashstore and
     * wouldn't convert it. Both the checksums table and hashstore are introduced in 3.1.0.
     * Also, it will pick up unsuccessfully converted docids to the dataone identifiers.
     * @return a ResultSet object which contains the list of identifiers:
     * @throws SQLException
     */
    protected ResultSet initCandidateList() throws SQLException {
        // Iterate the systemmetadata table
        String query =
            "(WITH docid_rev (docid, rev) AS (SELECT docid, rev FROM xml_documents UNION SELECT "
                + "docid, rev FROM  xml_revisions) SELECT CONCAT(d.docid, '.', d.rev) AS guid "
                + "FROM docid_rev d LEFT JOIN identifier i ON d.docid=i.docid and d.rev=i.rev "
                + "WHERE i.docid IS NULL) UNION (SELECT s.guid FROM systemmetadata s LEFT JOIN "
                + "checksums c ON s.guid = c.guid WHERE c.guid IS NULL);";
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
     * Get the object path for the given pid. When a not-found exception arises, it will throw
     * the exception if it is not cn; otherwise returns null. The reason is that cn doesn't harvest
     * the data objects so the data objects don't have the bytes in cn.
     * @param sysMeta  the system metadata associated with the object
     * @return  the object path. Null will be returned if there is no object found.
     * @throws SQLException
     * @throws McdbDocNotFoundException
     * @throws FileNotFoundException
     */
    protected Path resolve(SystemMetadata sysMeta )
        throws SQLException, McdbDocNotFoundException, FileNotFoundException {
        Identifier pid = sysMeta.getIdentifier();
        String localId;
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        } catch (McdbDocNotFoundException e) {
            // If we can't find them in the identifier table, throw an exception for the mns.
            // Note: those old metacat docid originally without system data already created the
            // records in the systemmetadata and identifier tables during the call of the
            // SystemMetadataFactory. createSystemMetadata method.
            if (isCN) {
                return null;
            } else {
                throw e;
            }
        }
        Path path;
        try {
            // This method will look both the data and documents directories.
            File file = SystemMetadataFactory.getFileFromLegacyStore(localId);
            path = file.toPath();
        } catch (FileNotFoundException e) {
            if (isCN) {
                return null;
            } else {
                throw e;
            }
        }
        logMetacat.debug("The object path for " + pid.getValue() + " is " + path);
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

    /**
     * Transform the SystemMetadata object to an InputStream object
     * @param sysMeta  the object will be transformed
     * @return the InputStream object which represents the SystemMetadata object. Return null if
     * the given SystemMetadata object is null.
     * @throws IOException
     * @throws MarshallingException
     */
    protected static InputStream convertSystemMetadata(SystemMetadata sysMeta)
        throws IOException, MarshallingException {
        if (sysMeta != null) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                TypeMarshaller.marshalTypeToOutputStream(sysMeta, out);
                byte[] content = out.toByteArray();
                return new ByteArrayInputStream(content);
            }
        } else {
            return null;
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
        try (InputStream sysMetaInput = convertSystemMetadata(sysMeta)) {
            metadata = converter.convert(path, finalId, sysMetaInput, checksum, algorithm);
        } catch (NonMatchingChecksumException e) {
            logMetacat.info("Continue to move the object " + finalId + "to hashstore even though "
                                + "its checksum in the system metadata didn't match Hashstore's "
                                + "calculation: " + e.getMessage());
            try {
                metadata =
                    reConvertUnMatchedChecksumObject(path, finalId, sysMeta, e.getHexDigests(),
                                                     nonMatchingChecksumWriter);
            } catch (Exception ee) {
                writeToFile(finalId, e, generalWriter);
                return;
            }
        } catch (NoSuchAlgorithmException e) {
            logMetacat.error("Cannot move the object " + finalId + " to hashstore since "
                                + e.getMessage());
            writeToFile(finalId, noSuchAlgorithmWriter);
            return;
        } catch (Exception e) {
            logMetacat.error("Cannot move the object " + finalId + " to hashstore since "
                                + e.getMessage(), e);
            writeToFile(finalId, e, generalWriter);
            return;
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

    private ObjectMetadata reConvertUnMatchedChecksumObject(
        Path path, String finalId, SystemMetadata sysMeta, Map<String, String> checksums,
        BufferedWriter nonMatchingChecksumWriter)
        throws MarshallingException, IOException, NoSuchAlgorithmException, InterruptedException,
        ServiceFailure, InvalidRequest {
        ObjectMetadata metadata = null;
        String originalChecksum = sysMeta.getChecksum().getValue();
        String algorithm = sysMeta.getChecksum().getAlgorithm().toUpperCase();
        // Modify the System Metadata with the new checksum from hashstore
        String newChecksum = checksums.get(algorithm);
        if (newChecksum == null || newChecksum.isBlank()) {
            throw new NoSuchAlgorithmException("HashStore doesn't have the checksum with the "
                                                   + "algorithm " + algorithm);
        }
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(algorithm);
        checksum.setValue(newChecksum);
        sysMeta.setChecksum(checksum);
        //Save the new system metadata with correct the checksum to database and hashstore
        //false means no modification date change
        SystemMetadataManager.getInstance().store(sysMeta, false,
                                                  SystemMetadataManager.SysMetaVersion.UNCHECKED);
        try (InputStream sysMetaInput = convertSystemMetadata(sysMeta)) {
            metadata = converter.convert(path, finalId, sysMetaInput, newChecksum, algorithm);
        }
        String info =
            "Object: " + finalId + " was converted successfully, but original " + algorithm + " "
                + "checksum (" + originalChecksum
                + ") was incorrect. System metadata was updated with new checksum (" + newChecksum
                + "). Old object for reference: " + path;
        // Check if the user just put a wrong algorithm. If there is a match, Metacat gives the
        // users more info.
        for (String key : checksums.keySet()) {
            String value = checksums.get(key);
            if (value != null && value.equals(originalChecksum)) {
                logMetacat.debug(
                    "find the algorithm " + key + " with the original checksum " + originalChecksum
                        + " in the checksum maps from the hashstore. The users just may put a "
                        + "wrong algorithm in the system metadata.");
                info = info + " Note: the original checksum would have been correct if the " + key
                        + " algorithm was used. Was the wrong algorithm recorded?";
                break;
            }
        }
        writeToFile(info, nonMatchingChecksumWriter);
        return metadata;
    }

}
