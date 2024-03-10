package edu.ucsb.nceas.metacat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
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
import edu.ucsb.nceas.utilities.IOUtil;
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
    private static File dataDir = null;
    private static File metadataDir = null;

    public enum Action {INSERT, UPDATE};

    /**
     * Default constructor.
     */
    public MetacatHandler() {
        String documentPath = null;
        try {
            documentPath = PropertyService.getProperty("application.documentfilepath");
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Can't find the document file path property " + e.getMessage());
            documentPath = null;
        }
        metadataDir = initializeDir(documentPath);
        String dataPath = null;
        try {
            dataPath = PropertyService.getProperty("application.datafilepath");
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Can't find the data file path property " + e.getMessage());
            dataPath = null;
        }
        dataDir = initializeDir(dataPath);
    }

    /**
     * Create a directory based on the given path.
     * If Metacat can't read/write an exiting directory or create a new directory, or it is not
     * a directory, this method will return null;
     * @param path  the path of the directory
     * @return the file directory representing the path. Return null if bad things happen.
     */
    private File initializeDir(String path) {
        File dir = null;
        if (path != null && !path.isBlank()) {
            dir = new File(path);
            if (!dir.exists()) {
                try {
                    boolean success = dir.mkdirs();
                    if (!success) {
                        logMetacat.error("Can't create the object storage directory " + path);
                        dir = null;
                    }
                } catch (Exception e) {
                    logMetacat.error("Can't create the object storage directory " + path
                                    + " sicne " + e.getMessage());
                    dir = null;
                }
            } else if (!dir.canRead() || !dir.canWrite() || !dir.isDirectory()){
                //if we can read/write the directory or the file is not a directory. Set it null
                logMetacat.error("Metacat cannot read/write the object storage directory "
                                + path + " specified in the property. Or it is not a directory.");
                dir = null;
            }
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
                                      + "properties file or Tomcat can read/write it.");
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
     * @param pid  the pid of the object
     * @param sysmeta  the system metadata of the object
     * @param action  the action of the request: insert (new) or update
     * @param docType  the type of object - data (BIN) or metadata
     * @param object  the input stream contains the content of the object
     * @param preSys  the system metadata of the obsoleted object in the update action.
     *                It will be ignore in the update action.
     * @param user  the user initializing the request
     * @return the local Metacat docid for this object. It also serves as its file name.
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws IOException
     */
    public String save(Identifier pid, SystemMetadata sysmeta, Action action, String docType,
                        InputStream object, SystemMetadata preSys, String user)
                                                            throws InvalidRequest, ServiceFailure,
                                                             InvalidSystemMetadata, IOException {
        String localId = null;
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
        if (sysmeta == null) {
            throw new InvalidRequest("1181", "Metacat cannot save the object for "
                            + pid.getValue() + " into disk since its system metadata is blank");
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
            // So we put it in the memory. This has a been a long practice and doesn't cause issues.
            // The hash-map storage will fix it eventually
            byte[] metaBytes = IOUtils.toByteArray(object);
            validateSciMeta(metaBytes, sysmeta.getFormatId());
            if (object instanceof DetailedFileInputStream) {
                DetailedFileInputStream detailedStream = (DetailedFileInputStream) object;
                if (detailedStream.getExpectedChecksum() == null) {
                    dataStream = new ByteArrayInputStream(metaBytes);
                    IOUtils.closeQuietly(object);
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
                SystemMetadataManager.getInstance().store(sysmeta, true, conn);
                if (action == Action.UPDATE) {
                    if(preSys ==  null) {
                        throw new InvalidRequest("1181", "Metacat cannot save the object for "
                                + pid.getValue() + " into disk since the system metadata of the "
                                + "obsoleted object should not be blank.");
                    }
                    //It is update, we need to store the system metadata of the obsoleted pid as well
                    // Set needChangeModificationTime true
                    SystemMetadataManager.getInstance().store(preSys, true, conn);
                }
                // Save bytes into disk
                saveBytes(dataStream, localId, sysmeta.getChecksum(), docType, pid);
                conn.commit();
                conn.setAutoCommit(true);
            } catch (InvalidSystemMetadata e) {
                try {
                    conn.rollback();
                } catch (SQLException ee) {
                    throw new InvalidSystemMetadata("1180", "Metacat cannot save the object into "
                                                    + " disk since " + ee.getMessage()
                                                    + "Also, it cannot roll back the change in DB");
                }
                throw e;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException ee) {
                    throw new ServiceFailure("1190", "Metacat cannot save the object into "
                                                + " disk since " + ee.getMessage()
                                                + "Also, it cannot roll back the change in DB");
                }
                throw new ServiceFailure("1190", "Metacat cannot save the object into disk since "
                                          + e.getMessage());
            }
        } catch (SQLException e) {
            throw new ServiceFailure("1190", "Metacat cannot save the object into disk since "
                                    + " it can't get a DBConnection: "+ e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logMetacat.warn("Metacat cannot set back autoCommit true for DBConnection since "
                                + e.getMessage());
            }
            // It doesn't use the IOUtils.closeQuietly method for the purpose of the mock test
            try {
                if (dataStream != null) {
                    dataStream.close();
                }
            } catch (IOException ee) {
                logMetacat.warn("Metacat cannot finally close the input stream after saving the "
                                 + " object since " + ee.getMessage());
            }
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
        return localId;
    }

    /**
     * Read a file from Metacat's configured file system data directory.
     * @param dir  the directory where the file is located
     * @param filename  The file name of the file to read
     * @return fileInputStream  The file to read as a FileInputStream
     */
    private static FileInputStream readFromFilesystem(File dir, String filename)
        throws McdbDocNotFoundException {

        logMetacat.debug("MetacatHandler.readFromFilesystem() called.");

        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(new File(dir,filename));

        } catch (FileNotFoundException fnfe) {
            logMetacat.warn("There was an error reading the file " + filename + ". The error was: "
                                + fnfe.getMessage());
            throw new McdbDocNotFoundException(fnfe.getMessage());

        }

        return fileInputStream;
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
    protected File saveBytes(InputStream object, String localId, Checksum checksum,
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
        File dataDirectory = null;
        if (!docType.equals(DocumentImpl.BIN)) {
            dataDirectory = metadataDir;
        } else {
            dataDirectory = dataDir;
        }
        try {
            logMetacat.debug("File " + localId + " will be saved to "
                                     + dataDirectory.getCanonicalPath());
        } catch (IOException e) {
            //do nothing
        }
        return writeStreamToFile(dataDirectory, localId, object, checksum, pid);
    }

    /**
     * Write a stream to a file
     *
     * @param dir  the directory to write to
     * @param localId  the file name to write to
     * @param dataStream  the object bytes as an input stream
     * @param cheksum  the checksum from system metadata. We need to compare it to the one, which
     *                  Metacat calculate during the saving process.
     * @param pid  only for debugging purpose
     * @return newFile - the new file created
     *
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws InvalidRequest
     */
    private File writeStreamToFile(File dir, String localId, InputStream dataStream,
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
        File newFile = null;
        logMetacat.debug("Starting to write to disk.");
        newFile = new File(dir, localId);
        File tempFile = null;
        logMetacat.debug( "Filename for write is: " + newFile.getAbsolutePath()
                                + " for the data object pid " + pid.getValue());
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
                            // This method should throw an exception if the target file exists.
                            FileUtils.moveFile(tempFile, newFile);
                            long end = System.currentTimeMillis();
                            logMetacat.info("Metacat only needs the "
                                            + "move the data file from temporary location to the "
                                            + "permanent location for the object "
                                            + pid.getValue());
                            logMetacat.info(edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG
                                    + pid.getValue()
                       + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD
                                    + " Only move the data file from the temporary location "
                                    + "to the permanent location since the multiparts handler"
                                    + " has calculated the checksum"
                                    + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_DURATION
                                    + (end - start) / 1000);
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
            //The input stream is not a DetaileFileInputStream or the algorithm doesn't
            // match, we have to calculate the checksum.
            MessageDigest md = MessageDigest.getInstance(algorithm);//use the one from the systemeta
            // write data stream to desired file
            if (newFile.exists()) {
                // Something is wrong
                throw new ServiceFailure("1190", "The file " + localId + " already exists. "
                                        + "Metacat cannot continue to overwrite the file.");
            }
            try (DigestOutputStream os = new DigestOutputStream(new FileOutputStream(newFile), md)) {
                long length = IOUtils.copyLarge(dataStream, os);
                os.flush();
            }
            String localChecksum = DatatypeConverter.printHexBinary(md.digest());
            logMetacat.info("The check sum calculated from the finally saved process is "
                             + localChecksum);
            if (localChecksum == null || localChecksum.isBlank()
                || !localChecksum.equalsIgnoreCase(checksumValue)) {
                logMetacat.error(
                    "D1NodeService.writeStreamToFile - the check sum calculated from the "
                    + "saved local file is "
                        + localChecksum
                        + ". But it doesn't match the value from the system metadata "
                        + checksumValue + " for the object " + pid.getValue());
                boolean success = newFile.delete();
                logMetacat.info(
                    "delete the file " + newFile.getAbsolutePath() + " for the object "
                        + pid.getValue() + " sucessfully?" + success);
                throw new InvalidSystemMetadata("1180",
                        "The checksum calculated from the saved local file is "
                                + localChecksum
                                + ". But it doesn't match the value from the system metadata "
                                + checksumValue + ".");
            }
            long end = System.currentTimeMillis();
            logMetacat.info(edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG + pid.getValue()
                    + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD
                    + " Need to read the data file from the temporary location and write it "
                    + "to the permanent location since the multiparts handler has NOT "
                    + "calculated the checksum"
                    + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_DURATION
                    + (end - start) / 1000);
            if (tempFile != null) {
                StreamingMultipartRequestResolver.deleteTempFile(tempFile);
            }
        } catch (FileNotFoundException e) {
            logMetacat.error(
                "FNF: " + e.getMessage() + " for the data object " + pid.getValue(), e);
            throw new ServiceFailure(
                "1190", "File not found: " + localId + " " + e.getMessage());
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
            try {
                logMetacat.debug("looking in identifier table for pid " + prePid.getValue());
                localId = IdentifierManager.getInstance().getLocalId(prePid.getValue());
                logMetacat.debug("localId: " + localId + " for the pid " + prePid.getValue());
                //increment the revision
                String docid = localId.substring(0, localId.lastIndexOf("."));
                String revS = localId.substring(localId.lastIndexOf(".") + 1, localId.length());
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
            Vector<XMLSchema> schemaList = XMLSchemaService.getInstance().
                    findSchemasInXML(xmlReader);
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
