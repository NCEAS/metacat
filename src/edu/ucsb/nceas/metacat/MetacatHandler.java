package edu.ucsb.nceas.metacat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.ecoinformatics.eml.EMLParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.common.Settings;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandler;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.metacat.restservice.multipart.StreamingMultipartRequestResolver;
import edu.ucsb.nceas.metacat.service.XMLSchema;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.LSIDUtil;
import edu.ucsb.nceas.utilities.ParseLSIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * General entry point for the Metacat server which is called from various mechanisms such as the
 * standard MetacatServlet class and the various web service servlets such as RestServlet class.
 * All application logic should be encapsulated in this class, and the calling classes should only
 * contain parameter marshaling and unmarshalling code, delegating all else to this MetacatHandler
 * instance.
 *
 * @author Matthew Jones
 */
public class MetacatHandler {
    private static Log logMetacat = LogFactory.getLog(MetacatHandler.class);

    // Constants -- these should be final in a servlet
    private static final String PROLOG = "<?xml version=\"1.0\"?>";
    private static final String ERROR = "<error>";
    private static final String ERRORCLOSE = "</error>";
    private static final String NOT_SUPPORT_MESSAGE =
        PROLOG + "\n" + ERROR + "The original Metacat API has been replaced, "
            + "and so this request is no longer supported. "
            + "Equivalent API methods now are available through "
            + "the DataONE API (see <https://knb.ecoinformatics.org/api>)." + ERRORCLOSE;
    private static String dataDir = null;
    private static String metadataDir = null;

    public enum Action {INSERT, UPDATE};

    /**
     * Default constructor.
     */
    public MetacatHandler() {
        String documentPath = null;
        try {
            documentPath = PropertyService.getProperty("application.documentfilepath");
            if (!documentPath.endsWith("/")) {
                documentPath = documentPath + "/";
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Can't find the document file path property " + e.getMessage());
            documentPath = null;
        }
        Path dir = initializeDir(documentPath);
        if (dir != null) {
            // The metadata storage directory either was successfully created and exists with
            // the proper permissions. So we just use it as the value of metadataDir.
            metadataDir = documentPath;
        }
        logMetacat.info("The Metacat metadata storage directory is " + metadataDir);
        String dataPath = null;
        try {
            dataPath = PropertyService.getProperty("application.datafilepath");
            if (!dataPath.endsWith("/")) {
                dataPath = dataPath + "/";
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Can't find the data file path property " + e.getMessage());
            dataPath = null;
        }
        Path dir2 = initializeDir(dataPath);
        if (dir2 != null) {
            // The data storage directory either was successfully created or exists with
            // the proper permissions. So we just use it as the value of dataDir
            dataDir = dataPath;
        }
        logMetacat.info("The Metacat data storage directory is " + dataDir);
    }

    /**
     * Create a directory based on the given path if it is necessary.
     * If Metacat can't read/write/exectue an exiting directory or create a new directory, null will
     * be return. The return value is an indicator if we can use the given path as the storage path.
     * @param path  the path of the directory
     * @return the path of the created directory or existing directory with proper permissions.
     *         Return null if bad things happen.
     */
    protected Path initializeDir(String path) {
        Path dir;
        if (path != null && !path.isBlank()) {
            dir = Paths.get(path);
            if (!Files.exists(dir)) {
                // The new directory route
                try {
                    dir = Files.createDirectories(dir);
                } catch (Exception e) {
                    // Sallow the exception and return null.
                    // We will check if the dataDir and documentDir are null before using them
                    logMetacat.error("Can't create the object storage directory " + path
                                    + " sicne " + e.getMessage());
                    dir = null;
                }
            } else if (!Files.isReadable(dir) || !Files.isWritable(dir)
                                        || !Files.isExecutable(dir) || !Files.isDirectory(dir)) {
                // The bad case for the existing directory route. Other cases we just use the
                // existing directory.
                // If we can't read/write/execute the directory or the file is not a directory,
                // set it null
                logMetacat.error("Metacat cannot read/write/create the object storage directory "
                                + path + " specified in the property. Or it is not a directory.");
                dir = null;
            }
        } else {
            logMetacat.error("Users should specify the data and document directory in "
                                + "the property files ");
            dir = null;
        }
        return dir;
    }

    /**
     * Check if the object storage directories exist.
     * @throws IOException
     */
    private static void checkObjectDirs() throws IOException {
        if (dataDir == null || metadataDir == null) {
            throw new IOException("Metacat doesn't have the valid data or metadata "
                                      + "storage directories. Either they are not set in the "
                                      + "properties file or Tomcat can read/write/excute it.");
        }
    }

    /**
     * Send back the not-support message
     *
     * @param response
     * @throws IOException
     */
    protected void sendNotSupportMessage(HttpServletResponse response) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            response.setStatus(301);
            out.println(NOT_SUPPORT_MESSAGE);
        }
    }

    /**
     * Get the metadata object directory
     * @return the metadata object directory
     */
    public String getMetadataDir() {
        return this.metadataDir;
    }

    /**
     * Get the data object directory
     * @return the data object directory
     */
    public String getDataDir() {
        return this.dataDir;
    }

    /**
     * Read a document from metacat and return the InputStream.
     * @param localId  the metacat docid to read
     * @param dataType  the type of the object associated with docid
     * @return objectStream  the document as an InputStream
     * @throws InsufficientKarmaException
     * @throws ParseLSIDException
     * @throws PropertyNotFoundException
     * @throws McdbException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static InputStream read(String localId, String dataType)
        throws ParseLSIDException, PropertyNotFoundException, McdbException, SQLException,
        ClassNotFoundException, IOException {
        logMetacat.debug("MetacatHandler.read() called and the data type is " + dataType);
        // Check if the object storage directory is in a good condition
        checkObjectDirs();
        InputStream inputStream = null;

        // be sure we have a local ID from an LSID
        if (localId.startsWith("urn:")) {
            try {
                localId = LSIDUtil.getDocId(localId, true);
            } catch (ParseLSIDException ple) {
                logMetacat.debug(
                    "There was a problem parsing the LSID. The " + "error message was: "
                        + ple.getMessage());
                throw ple;
            }
        }
        if (dataType != null && dataType.equalsIgnoreCase(D1NodeService.METADATA)) {
            logMetacat.debug("MetacatHandler.read - the data type is specified as the meta data");
            try {
                inputStream = readFromFilesystem(metadataDir, localId);
            } catch (McdbDocNotFoundException e) {
                inputStream = readFromFilesystem(dataDir, localId);
            }
        } else {
            // deal with data case
            try {
                inputStream = readFromFilesystem(dataDir, localId);
            } catch (McdbDocNotFoundException e) {
                inputStream = readFromFilesystem(metadataDir, localId);
            }
        }
        return inputStream;
    }

    /**
     * Save the object into disk
     * @param sysmeta  the system metadata of the object, which contains the identifier
     * @param changeModificationDate  if Metacat needs to update the modification date of the system
     *              metadata when Metacat stores it.
     * @param action  the action of the request: Action.INSERT (new) or Action.UPDATE
     * @param docType  the type of object - data (BIN) or metadata
     * @param object  the input stream contains the content of the object
     * @param preSys  the system metadata of the obsoleted object in the update action.
     *                It will be ignored in the insert action.
     * @param user  the user initializing the request
     * @return the local Metacat docid for this object. It also serves as its file name.
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws IOException
     */
    public String save(SystemMetadata sysmeta, boolean changeModificationDate, Action action,
                        String docType, InputStream object, SystemMetadata preSys, String user)
                                                            throws InvalidRequest, ServiceFailure,
                                                             InvalidSystemMetadata, IOException {
        String localId = null;
        if (sysmeta == null) {
            throw new InvalidRequest("1181", "Metacat cannot save the object "
                            + " into disk since its system metadata is blank");
        }
        Identifier pid = sysmeta.getIdentifier();
        if (pid == null || pid.getValue() == null || pid.getValue().isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot save an object with a blank pid");
        }
        if (docType == null || docType.isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot save the object for "
                                     + pid.getValue() + " into disk since the doc type is blank.");
        }
        if (action == null) {
            throw new InvalidRequest("1181", "Metacat cannot save the object for "
                    + pid.getValue() + " into disk since the action value (which should be `insert`"
                    + " or `update`) is blank.");
        }
        if (user == null || user.isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot save the object for "
                            + pid.getValue() + " into disk since the client identity is blank");
        }
        InputStream dataStream = null; // we will assign different stream for this variable.
        if (!docType.equals(DocumentImpl.BIN)) {
            // Handle the metadata objects and it needs validation
            // Validation will consume the input stream. If the original input
            // stream (the case is in the MN.replicate method) can't be reset, it will be an issue.
            // So we put it in the memory. This has been a long practice and doesn't cause issues.
            // The hash-map storage will fix it eventually
            byte[] metaBytes = IOUtils.toByteArray(object);
            validateSciMeta(metaBytes, sysmeta.getFormatId());
            if (object instanceof DetailedFileInputStream detailedStream) {
                if (detailedStream.getExpectedChecksum() == null) {
                    dataStream = new ByteArrayInputStream(metaBytes);
                    if (detailedStream.getFile() != null) {
                        // Metacat needs to delete the temp file
                        StreamingMultipartRequestResolver.deleteTempFile(detailedStream.getFile());
                    }
                    IOUtils.closeQuietly(object);
                    IOUtils.closeQuietly(detailedStream);
                    logMetacat.info("The DetailedInputStream doesn't have the checksum."
                              + "So Metacat will use ByteArrayInputStream as the source for "
                              + "saving to disk because of its higher performance.");
                } else {
                    dataStream = object;
                    logMetacat.info("The DetailedInputStream does have the checksum."
                            + "So Metacat will use DetailedInputStream as the source for "
                            + "saving to disk because of no need to calculate checksum.");
                }
            } else {
                dataStream = new ByteArrayInputStream(metaBytes);
                IOUtils.closeQuietly(object);
                logMetacat.info("Be default, Metacat will use ByteArrayInputStream as "
                              + "the source for saving to disk.");
            }
        } else {
            dataStream = object;
            logMetacat.info("In the data route, dataStream will use the original object stream.");
        }
        int serialNumber = -1;
        DBConnection conn = null;
        try {
            conn = DBConnectionPool.getDBConnection("MetacatHandler.save");
            serialNumber = conn.getCheckOutSerialNumber();
            Path newObject = null;
            StringBuffer error = new StringBuffer();
            error.append("Metacat cannot save the object ").append(pid.getValue())
                                                                .append(" into disk since ");
            try {
                conn.setAutoCommit(false);
                Identifier prePid = null;
                if (preSys != null) {
                    prePid = preSys.getIdentifier();
                }
                // Register the new object into the xml_documents and identifier table.
                localId = registerToDB(pid, action, conn, user, docType, prePid);
                // Save the system metadata for the new object
                // Set needChangeModificationTime true
                SystemMetadataManager.getInstance().store(sysmeta, changeModificationDate, conn);
                if (action == Action.UPDATE) {
                    if(preSys ==  null) {
                        throw new InvalidRequest("1181", "Metacat cannot save the object for "
                                + pid.getValue() + " into disk since the system metadata of the "
                                + "obsoleted object should not be blank.");
                    }
                    //It is update, we need to store the system metadata of the obsoleted pid as well
                    // Set changeModifyTime true
                    SystemMetadataManager.getInstance().store(preSys, true, conn);
                }
                // Save bytes into disk. If the localId already exists, the saveBytes should
                // throw an exception
                newObject = saveBytes(dataStream, localId, sysmeta.getChecksum(), docType, pid);
                conn.commit();
            } catch (InvalidSystemMetadata e) {
                error = clearUp(e, newObject, error, conn);
                throw new InvalidSystemMetadata("1180", error.toString());
            } catch (Exception e) {
                error = clearUp(e, newObject, error, conn);
                throw new ServiceFailure("1190", error.toString());
            }
        } catch (SQLException e) {
            throw new ServiceFailure("1190", "Metacat cannot save the object into disk since "
                                    + " it can't get a DBConnection: "+ e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                logMetacat.warn("Metacat cannot set back autoCommit true for DBConnection since "
                                + e.getMessage());
            }
            // Return database connection to the pool
            if (conn != null) {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
            IOUtils.closeQuietly(dataStream);
        }
        return localId;
    }

    /**
     * Clear up database and object file when the save failed. Try to restore the original state.
     * @param e  the exception arose in the save method
     * @param newObject  the object which might create in the save method
     * @param error  the string buffer holding the error message
     * @param conn  the connection to db
     * @return the string buffer which holds all error message.
     */
    private StringBuffer clearUp(Exception e, Path newObject,
                                                    StringBuffer error, DBConnection conn) {
        error.append(e.getMessage());
        // Clean up database
        try {
            conn.rollback();
        } catch (SQLException ee) {
            error.append(" Moreover, it cannot roll back the change in DB since ")
                            .append(ee.getMessage());
        }
        // Metacat also needs to delete the new created file. Null is the indicator if an object
        // was created.
        if (newObject != null) {
            // This means that we successfully created a new object in the saveBytes method.
            // Note: even though the dir+localId may physically point to an existing object,
            // the newObject still should be null when the saveBytes method throws an exception.
            try {
                Files.delete(newObject);
            } catch (IOException ie) {
                error.append(". Moreover, it cannot delete generated object since ")
                                                                .append(ie.getMessage());
            }
        }
        return error;
    }

    /**
     * Read a file from Metacat's configured file system object directories.
     * @param dir  the directory where the file is located
     * @param filename  The file name of the file to read
     * @return InputStream  The file to read as an InputStream object
     */
    private static InputStream readFromFilesystem(String dir, String filename)
        throws McdbDocNotFoundException, IOException {

        logMetacat.debug("MetacatHandler.readFromFilesystem() called.");

        InputStream inputStream = null;

        try {
            inputStream = Files.newInputStream(Paths.get(dir,filename));

        } catch (NoSuchFileException fnfe) {
            logMetacat.warn("There was an error reading the file " + filename + ". The error was: "
                                + fnfe.getMessage());
            throw new McdbDocNotFoundException(fnfe.getMessage());

        }

        return inputStream;
    }


    /**
     * Save the bytes from a input stream into disk.
     * @param object  the input stream of the object
     * @param localId  the docid which serves as the file name
     * @param checksum  checksum of the input stream from the system metadata declaration.
     * @param docType  the type of object. BIN means data; otherwise means metadata
     * @param pid  the identifier of object. It is only used for debug information.
     * @return the file which has the input stream content
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws InvalidRequest
     */
    protected Path saveBytes(InputStream object, String localId, Checksum checksum,
                                                            String docType, Identifier pid)
                                      throws ServiceFailure, InvalidSystemMetadata, InvalidRequest {
        try {
            checkObjectDirs();
        } catch (IOException e) {
            throw new ServiceFailure("1190", e.getMessage());
        }
        if (docType == null || docType.isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot save bytes for "
                                     + pid.getValue() + " into disk since the doc type is blank.");
        }
        String objectDirectory = null;
        if (!docType.equals(DocumentImpl.BIN)) {
            objectDirectory = metadataDir;
        } else {
            objectDirectory = dataDir;
        }
        logMetacat.debug("File " + localId + " will be saved to " + objectDirectory);
        // The object stream will be closed in the writeStreamToFile method
        return writeStreamToFile(objectDirectory, localId, object, checksum, pid);
    }

    /**
     * Write a stream to a file. This method prevents the overwriting of an existing object by
     * using the Files.move and Files.copy methods.
     *
     * @param dir  the directory to write to
     * @param localId  the file name to write to
     * @param dataStream  the object bytes as an input stream
     * @param cheksum  the checksum from system metadata. We need to compare it to the one, which
     *                  Metacat calculate during the saving process.
     * @param pid  only for debugging purpose
     * @return a Path object - the new file created
     *
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws InvalidRequest
     */
    private Path writeStreamToFile(String dir, String localId, InputStream dataStream,
                                          Checksum checksum, Identifier pid) throws ServiceFailure,
                                                            InvalidSystemMetadata, InvalidRequest {
        if(checksum == null) {
             throw new InvalidSystemMetadata("1180", "The checksum from the system metadata for "
                              + " saving " + pid.getValue() + " into disk shouldn't be null.");
        }
        if (localId == null || localId.isBlank()) {
             throw new InvalidRequest("1181", "The docid which will be used as file name for "
                                 + " saving " + pid.getValue() + " to disk should not be blank");
        }
        if (dataStream == null) {
            throw new InvalidRequest("1181", "The source stream for saving "
                                     + pid.getValue() + " into disk should not be blank");
        }
        Path newFile = null;
        logMetacat.debug("Starting to write to disk.");
        newFile = Paths.get(dir, localId);
        File tempFile = null;
        logMetacat.debug( "The file path for writing is: " + newFile + " for the data object pid "
                          + pid.getValue());
        try {
            String checksumValue = checksum.getValue();
            logMetacat.debug("The checksum value from the system " + "metadata is "
                                + checksumValue + " for the object " + pid.getValue());
            if (checksumValue == null || checksumValue.isBlank()) {
                throw new InvalidSystemMetadata("1180",
                       "The checksum value from the system metadata shouldn't be null or blank.");
            }
            String algorithm = checksum.getAlgorithm();
            logMetacat.debug("The algorithm to calculate the checksum "
                                + "from the system metadata is "
                                + algorithm + " for the data object " + pid.getValue());
            if (algorithm == null || algorithm.isBlank()) {
                throw new InvalidSystemMetadata("1180",
                                "The algorithm to calculate the checksum from the system metadata "
                                 + "shouldn't be null or blank.");
            }
            long start = System.currentTimeMillis();
            //if the input stream is an object DetailedFileInputStream, it means this object
            // may already have the checksum information when Metacat save request from the clients.
            if (dataStream instanceof DetailedFileInputStream) {
                DetailedFileInputStream stream = (DetailedFileInputStream) dataStream;
                tempFile = stream.getFile();
                Checksum expectedChecksum = stream.getExpectedChecksum();
                if (expectedChecksum != null) {
                    //Good, Metacat has already calculated the checksum during the first place.
                    //This Metacat calculated checksum is considered as the true value.
                    //We need to compare the true value with the declaration from the systemmetadata
                    String expectedAlgorithm = expectedChecksum.getAlgorithm();
                    String expectedChecksumValue = expectedChecksum.getValue();
                    if (expectedAlgorithm != null
                                               && expectedAlgorithm.equalsIgnoreCase(algorithm)) {
                        //The algorithm is the same and the checksum is same, we just need to
                        // move the file from the temporary location (serialized by the
                        // multiple parts handler) to the permanent location
                        if (expectedChecksumValue != null
                            && expectedChecksumValue.equalsIgnoreCase(checksumValue)) {
                            //**********************************************************************
                            //**********************************************************************
                            // This is the first place (the total places is two) to write bytes to
                            // the permanent object storage location. This is a fast one - moving.
                            // According to documentation, By default, the move method attempts to
                            // move the file to the target file, failing if the target file
                            // exists except if the source and target are the same file, in
                            // which case this method has no effect. So It should not overwrite an
                            // existing data object.
                            // https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html
                            Files.move(tempFile.toPath(), newFile);
                            long end = System.currentTimeMillis();
                            logMetacat.info("Metacat only needs the move the object file from "
                                    + "temporary location to the permanent location for the object "
                                    + pid.getValue());
                            logMetacat.info(Settings.PERFORMANCELOG + pid.getValue()
                                    + Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD
                                    + " Only move the data file from the temporary location "
                                    + "to the permanent location since the multiparts handler"
                                    + " has calculated the checksum"
                                    + Settings.PERFORMANCELOG_DURATION + (end - start)/1000);
                            return newFile;
                        } else {
                            throw new InvalidSystemMetadata("1180", "The check sum calculated "
                                   + "from the saved local file is "
                                    + expectedChecksumValue
                                    + ". But it doesn't match the value from the system "
                                    + "metadata " + checksumValue
                                    + " for the object " + pid.getValue());
                        }
                    } else {
                        logMetacat.info( "The checksum algorithm which "
                                        + "the multipart handler used is "
                                        + expectedAlgorithm
                                        + " and it is different to one on the system metadata "
                                        + algorithm + ". So we have to calculate again.");
                    }
                }
            }
            //**************************************************************************************
            //**************************************************************************************
            // This is the second place (the total places is two) to write bytes to
            // the permanent object storage location. This is a slow one - Writing bytes into disk
            // while calculating the checksum. This place handle all other scenarios except the
            // first quick one, which already has the checksum and we can compare it to the one from
            // the system metadata.
            // According to documentation, by default, the Files.copy fails if the target file
            // already exists or is a symbolic link. So It should not overwrite an
            // existing data object, which is good.
            // https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html
            MessageDigest md = MessageDigest.getInstance(algorithm);//use the one from the systemeta
            try (DigestInputStream is = new DigestInputStream(dataStream, md)) {
                Files.copy(is, newFile);
            }
            String localChecksum = DatatypeConverter.printHexBinary(md.digest());
            logMetacat.debug("The check sum calculated from the finally saved process is "
                             + localChecksum);
            if (localChecksum == null || localChecksum.isBlank()
                || !localChecksum.equalsIgnoreCase(checksumValue)) {
                StringBuffer error = new StringBuffer();
                error.append("The checksum calculated from the saved local file is ")
                     .append(localChecksum)
                     .append(". But it doesn't match the value from the system metadata ")
                     .append(checksumValue).append(" for the object ").append(pid.getValue());
                // The new object file was already generated even though the checksum in the
                // system metadata is incorrect. So we have to delete it to restore to the original
                // state and throw an exception
                try {
                    Files.delete(newFile);
                } catch (IOException io) {
                    error.append(" Moreover Metacat tried to delete the already generated file ")
                         .append(newFile).append(" but failed since ").append(io.getMessage());
                }
                logMetacat.error(error.toString());
                throw new InvalidSystemMetadata("1180", error.toString());
            }
            long end = System.currentTimeMillis();
            logMetacat.info(Settings.PERFORMANCELOG + pid.getValue()
                    + Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD
                    + " Need to read the data file from the temporary location and write it "
                    + "to the permanent location since the multiparts handler has NOT "
                    + "calculated the checksum"
                    + Settings.PERFORMANCELOG_DURATION
                    + (end - start) / 1000);
            if (tempFile != null) {
                // Clean up
                StreamingMultipartRequestResolver.deleteTempFile(tempFile);
            }
        } catch (IOException e) {
            logMetacat.error(
                "IOE: " + e.getMessage() + " for the data object " + pid.getValue(), e);
            throw new ServiceFailure(
                "1190", "File was not written: " + localId + " " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logMetacat.error(
                "D1NodeService.writeStreamToFile - no such checksum algorithm exception "
                    + e.getMessage() + " for the data object " + pid.getValue(), e);
            throw new ServiceFailure(
                "1190", "No such checksum algorithm: " + " " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(dataStream);
        }
        return newFile;
    }

    /**
     * Register the dataone identifier into database (the xml_documents/revisions, identifier
     * tables). It also generate a local Metacat doc id.
     * @param pid  the dataone identifier
     * @param action  insert or update
     * @param conn  the connection to db
     * @param user  the user who requests the action
     * @param docType  BIN for data objects; the format id for metadata objects
     * @param prePid  the old identifier which will be updated. If this is an update action, you
     *                 should specify it. For the insert action, it is ignored and can be null.
     * @return the generated Metacat local docid
     * @throws ServiceFailure
     * @throws InvalidRequest
     */
    protected String registerToDB(Identifier pid, Action action, DBConnection conn,
                                String user, String docType, Identifier prePid)
                                                    throws ServiceFailure, InvalidRequest {
        if (pid == null || pid.getValue() == null || pid.getValue().isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot register "
                                        + "a blank identifier into database.");
        }
        if (docType == null || docType.isBlank()) {
            throw new InvalidRequest("1181", "Metacat cannot register "
                    + pid.getValue() + " into database since the doc type is blank.");
        }
        String localId;
        if (action == Action.INSERT) {
            localId = DocumentUtil.generateDocumentId(1);
        } else {
            // Update action
            //localid should already exist in the identifier table, so just find it
            if (prePid == null || prePid.getValue() == null || prePid.getValue().isBlank()) {
                throw new InvalidRequest("1181", "Metacat cannot register "
                                   + "records into database since it tries to update a blank pid.");
            }
            if (docType.equals(DocumentImpl.BIN)) {
                localId = DocumentUtil.generateDocumentId(1);
            } else {
                try {
                    logMetacat.debug("looking in identifier table for pid " + prePid.getValue());
                    localId = IdentifierManager.getInstance().getLocalId(prePid.getValue());
                    logMetacat.debug("localId: " + localId + " for the pid " + prePid.getValue());
                    //increment the revision
                    String docid = localId.substring(0, localId.lastIndexOf("."));
                    String revS = localId.substring(localId.lastIndexOf(".") + 1);
                    int rev = Integer.parseInt(revS);
                    rev++;
                    localId = docid + "." + rev;
                    logMetacat.debug("incremented localId: " + localId);
                } catch (McdbDocNotFoundException e) {
                    throw new ServiceFailure(
                        "1190", "The object " + "pid " + pid.getValue()
                        + " should have been in the identifier table, but it wasn't: "
                        + e.getMessage());

                } catch (SQLException e) {
                    throw new ServiceFailure(
                        "1190", "Metacat couldn't identify if the pid " + pid.getValue()
                            + " is in the identifier table since " + e.getMessage());
                }
            }
        }
        logMetacat.debug("Mapping pid " + pid.getValue() + " with docid " + localId);
        try {
            IdentifierManager.getInstance().createMapping(pid.getValue(), localId, conn);
            String docName = docType;
            if (docType.equals(DocumentImpl.BIN)) {
                docName = localId;
            }
            logMetacat.debug("Register the docid " + localId + " with doc type " + docType
                             + " into xml_documents/xml_revsions table");
            DocumentImpl.registerDocument(docName, docType, conn, localId, user);
        } catch (PropertyNotFoundException | MetacatException | SQLException
                                                | AccessionNumberException e) {
            throw new ServiceFailure("1190", "Metacat couldn't register " + pid.getValue()
                                  + " into the database since " + e.getMessage());
        }
        return localId;
    }

    /**
     * Validate a scientific metadata object. It will throw an InvalidRequest if it is invalid.
     * @param object  the content of the object. This is a byte array and so far it has not caused
     *                the memory issue. In 3.1.0 release, it will be replaced.
     * @param formatId  format id of the object
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws ServiceException
     * @throws PropertyNotFoundException
     * @throws IOException
     * @throws SAXException
     * @throws MetacatException
     */
    protected void validateSciMeta(byte[] object, ObjectFormatIdentifier formatId)
                                         throws InvalidRequest, ServiceFailure, IOException {
        NonXMLMetadataHandler handler =
                NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
            if (handler != null) {
                // a non-xml metadata object path
                logMetacat.debug("Validate the non-XML scientific metadata object.");
                handler.validate(new ByteArrayInputStream(object));
            } else {
                // an XML object
                logMetacat.debug("Validate the XML scientific metadata object.");
                validateXmlSciMeta(object, formatId.getValue());
            }
    }

    /**
     * Validate an XML object. If it is not valid, an InvalidRequest will be thrown.
     * @param object  the content of the object. This is a byte array and so far it has not caused
     *                the memory issue. In 3.1.0 release, it will be replaced.
     * @param formatId  format id of the object
     * @throws IOException
     * @throws ServiceFailure
     * @throws InvalidRequest
     */
    protected void validateXmlSciMeta(byte[] object, String formatId) throws IOException,
                                                            ServiceFailure, InvalidRequest {
        boolean needValidation = false;
        String rule = null;
        String namespace = null;
        String schemaLocation = null;
        String doctext = new String(object, StandardCharsets.UTF_8);
        StringReader xmlReader = null;
        try {
            xmlReader = new StringReader(doctext);
            needValidation = needDTDValidation(xmlReader);
            if (needValidation) {
                // set a dtd base validation parser
                logMetacat.debug(
                    "MetacatHandler.handleInsertOrUpdateAction - the xml object will be "
                        + "validate by a dtd");
                rule = DocumentImpl.DTD;
            } else {
                XMLSchemaService.getInstance().doRefresh();
                xmlReader = new StringReader(doctext);
                namespace = XMLSchemaService.findDocumentNamespace(xmlReader);
                if (namespace != null) {
                    logMetacat.debug(
                        "MetacatHandler.handleInsertOrUpdateAction - the xml object will be "
                            + "validated by a schema which has a target namespace: "
                            + namespace);
                    schemaLocation = XMLSchemaService.getInstance()
                        .findNamespaceAndSchemaLocalLocation(formatId, namespace);
                    if (namespace.compareTo(DocumentImpl.EML2_0_0NAMESPACE) == 0
                        || namespace.compareTo(DocumentImpl.EML2_0_1NAMESPACE) == 0) {
                        // set eml2 base     validation parser
                        rule = DocumentImpl.EML200;
                        needValidation = true;
                        // using emlparser to check id validation
                        @SuppressWarnings("unused") EMLParser parser =
                            new EMLParser(doctext);
                    } else if (namespace.compareTo(DocumentImpl.EML2_1_0NAMESPACE) == 0
                        || namespace.compareTo(DocumentImpl.EML2_1_1NAMESPACE) == 0
                        || namespace.compareTo(DocumentImpl.EML2_2_0NAMESPACE) == 0) {
                        // set eml2 base validation parser
                        rule = DocumentImpl.EML210;
                        needValidation = true;
                        // using emlparser to check id validation
                        @SuppressWarnings("unused") EMLParser parser =
                            new EMLParser(doctext);
                    } else {
                        if (!XMLSchemaService.isNamespaceRegistered(namespace)) {
                            throw new ServiceFailure("1190", "The namespace " + namespace
                                                    + " used in the xml object hasn't been "
                                                    + "registered in the Metacat. Metacat "
                                                    + "can't validate the object and rejected"
                                                    + " it. Please contact the operator of "
                                                    + "the Metacat for regsitering the "
                                                    + "namespace.");
                        }
                        // set schema base validation parser
                        rule = DocumentImpl.SCHEMA;
                        needValidation = true;
                    }
                } else {
                    xmlReader = new StringReader(doctext);
                    String noNamespaceSchemaLocationAttr =
                        XMLSchemaService.findNoNamespaceSchemaLocationAttr(xmlReader);
                    if (noNamespaceSchemaLocationAttr != null) {
                        logMetacat.debug(
                            "MetacatHandler.handleInsertOrUpdateAction - the xml object will "
                                + "be validated by a schema which deoe NOT have a target "
                                + "name space.");
                        schemaLocation = XMLSchemaService.getInstance()
                            .findNoNamespaceSchemaLocalLocation(formatId,
                                                                noNamespaceSchemaLocationAttr);
                        rule = DocumentImpl.NONAMESPACESCHEMA;
                        needValidation = true;
                    } else {
                        logMetacat.debug(
                            "MetacatHandler.handleInsertOrUpdateAction - the xml object will "
                                + "NOT be validated.");
                        rule = "";
                        needValidation = false;
                    }

                }
            }
            xmlReader = new StringReader(doctext);
            Vector<XMLSchema> schemaList = XMLSchemaService.findSchemasInXML(xmlReader);
            xmlReader = new StringReader(doctext);
            // set the dtd part null;
            XMLReader parser = DocumentImpl.initializeParser(schemaList, null, rule, needValidation,
                                                             schemaLocation);
            try {
                parser.parse(new InputSource(xmlReader));
            } catch (SAXException e) {
                throw new InvalidRequest("1181", "Invalid metadata: " + e.getMessage());
            }
        } catch (ServiceException | MetacatException | PropertyNotFoundException | SAXException e) {
            throw new ServiceFailure("1190", "Metacat cannot validate the object since "
                                                                            + e.getMessage());
        } finally {
            if (xmlReader != null) {
                // We don't use try-resource since the xmlReade object is assigned multiple times.
                IOUtils.closeQuietly(xmlReader);
            }
        }
    }

    /**
     * Parse XML Document to look for <!DOCTYPE ... PUBLIC/SYSTEM ... > in order to decide whether
     * to use validation parser
     */
    private static boolean needDTDValidation(StringReader xmlreader) throws IOException {
        StringBuffer cbuff = new StringBuffer();
        java.util.Stack<String> st = new java.util.Stack<String>();
        boolean validate = false;
        boolean commented = false;
        int c;
        int inx;

        // read from the stream until find the keywords
        while ((st.empty() || st.size() < 4) && ((c = xmlreader.read()) != -1)) {
            cbuff.append((char) c);

            if ((inx = cbuff.toString().indexOf("<!--")) != -1) {
                commented = true;
            }

            // "<!DOCTYPE" keyword is found; put it in the stack
            if ((inx = cbuff.toString().indexOf("<!DOCTYPE")) != -1) {
                cbuff = new StringBuffer();
                st.push("<!DOCTYPE");
            }
            // "PUBLIC" keyword is found; put it in the stack
            if ((inx = cbuff.toString().indexOf("PUBLIC")) != -1) {
                cbuff = new StringBuffer();
                st.push("PUBLIC");
            }
            // "SYSTEM" keyword is found; put it in the stack
            if ((inx = cbuff.toString().indexOf("SYSTEM")) != -1) {
                cbuff = new StringBuffer();
                st.push("SYSTEM");
            }
            // ">" character is found; put it in the stack
            // ">" is found twice: fisrt from <?xml ...?>
            // and second from <!DOCTYPE ... >
            if ((inx = cbuff.toString().indexOf(">")) != -1) {
                cbuff = new StringBuffer();
                st.push(">");
            }
        }

        // close the stream
        xmlreader.reset();

        // check the stack whether it contains the keywords:
        // "<!DOCTYPE", "PUBLIC" or "SYSTEM", and ">" in this order
        if (st.size() == 4) {
            if ((st.pop()).equals(">") && ((st.peek()).equals("PUBLIC") | (st.pop()).equals(
                "SYSTEM")) && (st.pop()).equals("<!DOCTYPE")) {
                validate = true && !commented;
            }
        }

        logMetacat.info("MetacatHandler.needDTDValidation - Validation for dtd is " + validate);
        return validate;
    }


}
