package edu.ucsb.nceas.metacat.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.hashstore.HashStore;
import org.dataone.hashstore.HashStoreFactory;
import org.dataone.hashstore.ObjectMetadata;
import org.dataone.hashstore.exceptions.HashStoreFactoryException;
import org.dataone.hashstore.exceptions.NonMatchingChecksumException;
import org.dataone.hashstore.exceptions.NonMatchingObjSizeException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.types.v1.Identifier;

import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * The HashStore implementation of the Storage interface.
 */
public class Storage {
    private static Log logMetacat = LogFactory.getLog(Storage.class);
    private static Storage storage;
    private static HashStore hashStore;
    private String defaultNamespace = "https://ns.dataone.org/service/types/v2.0#SystemMetadata";

    static {
        try {
            storage = new Storage();
        } catch (PropertyNotFoundException | IOException e) {
            logMetacat.error("Metacat cannot initialize the Storage class since " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Private constructor
     * @throws PropertyNotFoundException
     * @throws IOException
     * @throws HashStoreFactoryException
     */
    private Storage() throws PropertyNotFoundException,
                                                           HashStoreFactoryException, IOException {
        String className = PropertyService.getProperty("storage.className");
        String rootPath = PropertyService.getProperty("storage.hashstore.rootDirectory");
        String directoryDepth = "3";
        try {
            directoryDepth = PropertyService.getProperty("storage.hashstore.directory.depth");
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("Since " + e.getMessage() + ", Metacat uses the default value "
                            + directoryDepth);
        }
        String directoryNameWidth = "2";
        try {
            directoryNameWidth = PropertyService.getProperty("storage.hashstore.directory.width");
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("Since " + e.getMessage() + ", Metacat uses the default value "
                            + directoryNameWidth);
        }
        String fileNameAlgorithm = "SHA-256";
        try {
            fileNameAlgorithm = PropertyService.getProperty("storage.hashstore.fileNameAlgorithm");
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("Since " + e.getMessage() + ", Metacat uses the default value "
                            + fileNameAlgorithm);
        }
        try {
            defaultNamespace = PropertyService.getProperty("storage.hashstore.defaultNamespace");
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("Since " + e.getMessage() + ", Metacat uses the default value "
                            + defaultNamespace);
        }
        Properties storeProperties = new Properties();
        storeProperties.setProperty("storePath", rootPath);
        storeProperties.setProperty("storeDepth", directoryDepth);
        storeProperties.setProperty("storeWidth", directoryNameWidth);
        storeProperties.setProperty("storeAlgorithm", fileNameAlgorithm);
        storeProperties.setProperty("storeMetadataNamespace", defaultNamespace);
        hashStore = HashStoreFactory.getHashStore(className, storeProperties);
    }


    /**
     * Get the instance of the class through the singleton pattern
     * @return the instance of the class
     * @throws ServiceException
     * @throws PropertyNotFoundException
     */
    public static Storage getInstance() throws ServiceException, PropertyNotFoundException {
        return storage;
    }

    /**
     * The `storeObject` method is responsible for the atomic storage of objects to disk using a
     * given InputStream. Upon successful storage, the method returns a (ObjectInfo) object
     * containing relevant file information, such as the file's id (which can be used to locate
     * the object on disk), the file's size, and a hex digest dict of algorithms and checksums.
     * Storing an object with `store_object` also tags an object (creating references) which
     * allow the object to be discoverable.
     *
     * `storeObject` also ensures that an object is stored only once by synchronizing multiple
     * calls and rejecting calls to store duplicate objects. Note, calling `storeObject` without
     * a pid is a possibility, but should only store the object without tagging the object. It
     * is then the caller's responsibility to finalize the process by calling `tagObject` after
     * verifying the correct object is stored.
     *
     * The file's id is determined by calculating the object's content identifier based on the
     * store's default algorithm, which is also used as the permanent address of the file. The
     * file's identifier is then sharded using the store's configured depth and width, delimited
     * by '/' and concatenated to produce the final permanent address and is stored in the
     * `./[storePath]/objects/` directory.
     *
     * By default, the hex digest map includes the following hash algorithms: MD5, SHA-1,
     * SHA-256, SHA-384, SHA-512 - which are the most commonly used algorithms in dataset
     * submissions to DataONE and the Arctic Data Center. If an additional algorithm is
     * provided, the `storeObject` method checks if it is supported and adds it to the hex
     * digests dict along with its corresponding hex digest. An algorithm is considered
     * "supported" if it is recognized as a valid hash algorithm in
     * `java.security.MessageDigest` class.
     *
     * Similarly, if a file size and/or checksum & checksumAlgorithm value are provided,
     * `storeObject` validates the object to ensure it matches the given arguments before moving
     * the file to its permanent address.
     *
     * @param object              Input stream to file
     * @param pid                 Authority-based identifier
     * @param additionalAlgorithm Additional hex digest to include in hexDigests
     * @param checksum            Value of checksum to validate against
     * @param checksumAlgorithm   Algorithm of checksum submitted
     * @param objSize             Expected size of object to validate after storing
     * @return ObjectMetadata object encapsulating file information
     * @throws NoSuchAlgorithmException   When additionalAlgorithm or checksumAlgorithm is
     *                                    invalid
     * @throws IOException                I/O Error when writing file, generating checksums
     *                                    and/or moving file
     * @throws InvalidRequest             If a pid refs file already exists, meaning the pid is
     *                                    already referencing a file.
     * @throws InvalidSystemMetadata      The checksum or size don't match the calculation
     * @throws RuntimeException           Thrown when there is an issue with permissions,
     *                                    illegal arguments (ex. empty pid) or null pointers
     * @throws InterruptedException       When tagging pid and cid process is interrupted
     */
    public ObjectInfo storeObject(InputStream object, Identifier pid, String additionalAlgorithm,
                                      String checksum, String checksumAlgorithm, long objSize)
                                     throws NoSuchAlgorithmException, IOException, InvalidRequest,
                                     InvalidSystemMetadata, RuntimeException, InterruptedException {
        //This method checks the null value as well
        boolean valid = D1NodeService.isValidIdentifier(pid);
        if (valid) {
            try {
                ObjectMetadata objMeta = hashStore.storeObject(object, pid.getValue(),
                        additionalAlgorithm, checksum, checksumAlgorithm, objSize);
                return convertToObjectInfo(objMeta);
            } catch (NonMatchingChecksumException e) {
                throw new InvalidSystemMetadata("0000", "The given checksum doesn't match "
                                                + "Metacat's calculation " + e.getMessage());
            } catch (NonMatchingObjSizeException e) {
                throw new InvalidSystemMetadata("0000", "The given size doesn't match "
                        + "Metacat's calculation " + e.getMessage());
            }
        } else {
            throw new InvalidRequest("0000", "The stored pid should not be null, blank, or "
                                     + "containing the white spaces in the storeObject method.");
        }
    }


    /**
     * @see #storeObject(InputStream, Identifier, String, String, String, long)
     *
     *      Store an object only without reference files.
     */
    public ObjectInfo storeObject(InputStream object) throws NoSuchAlgorithmException,
            IOException, InvalidRequest, RuntimeException, InterruptedException {
        ObjectMetadata objMeta = hashStore.storeObject(object);
        return convertToObjectInfo(objMeta);
    }

    /**
     * Creates references that allow objects stored in HashStore to be discoverable. Retrieving,
     * deleting or calculating a hex digest of an object is based on a pid argument; and to
     * proceed, we must be able to find the object associated with the pid.
     *
     * @param pid Authority-based identifier
     * @param cid Content-identifier (hash identifier)
     * @throws IOException                Failure to create tmp file
     * @throws InvalidRequest             When pid refs file already exists
     * @throws NoSuchAlgorithmException   When algorithm used to calculate pid refs address
     *                                    does not exist
     * @throws FileNotFoundException      If refs file is missing during verification
     * @throws InterruptedException       When tagObject is waiting to execute but is
     *                                    interrupted
     */
    public void tagObject(Identifier pid, String cid) throws IOException,
            InvalidRequest, NoSuchAlgorithmException, FileNotFoundException, InterruptedException {
        //This method checks the null value as well
        boolean valid = D1NodeService.isValidIdentifier(pid);
        if (valid) {
            hashStore.tagObject(pid.getValue(), cid);
        } else {
            throw new InvalidRequest("0000", "The stored pid should not be null, blank, or "
                                    + "containing the white spaces in the tagObject method.");
        }
    }

    /**
     * Confirms that an ObjectMetadata's content is equal to the given values. If it does not
     * equal, it will throw an exception
     *
     * @param objectInfo        ObjectMetadata object with values
     * @param checksum          Value of checksum to validate against
     * @param checksumAlgorithm Algorithm of checksum submitted
     * @param objSize           Expected size of object to validate after storing
     * @throws IllegalArgumentException An expected value does not match
     * @throws IOException Issue with recalculating supported algo for checksum not found
     */
    public void verifyObject( ObjectInfo objectInfo, String checksum,
            String checksumAlgorithm, long objSize) throws IllegalArgumentException, IOException {
        hashStore.verifyObject(convertToObjectMetadata(objectInfo), checksum,
                                checksumAlgorithm, objSize);
    }

    /**
     * Adds/updates metadata (ex. `sysmeta`) to the HashStore by using a given InputStream, a
     * persistent identifier (`pid`) and metadata format (`formatId`). All metadata documents
     * for a given pid will be stored in the directory (under ../metadata) that is determined
     * by calculating the hash of the given pid, with the document name being the hash of the
     * metadata format (`formatId`).
     *
     * Note, multiple calls to store the same metadata content will all be accepted, but is not
     * guaranteed to execute sequentially.
     *
     * @param metadata Input stream to metadata document
     * @param pid      Authority-based identifier
     * @param formatId Metadata namespace/format
     * @return Path to metadata content identifier (string representing metadata address)
     * @throws IOException              When there is an error writing the metadata document
     * @throws IllegalArgumentException Invalid values like null for metadata, or empty pids and
     *                                  formatIds
     * @throws FileNotFoundException    When temp metadata file is not found
     * @throws InterruptedException     metadataLockedIds synchronization issue
     * @throws NoSuchAlgorithmException Algorithm used to calculate permanent address is not
     *                                  supported
     */
    public String storeMetadata(InputStream metadata, Identifier pid, String formatId)
            throws IOException, IllegalArgumentException, FileNotFoundException,
            InterruptedException, NoSuchAlgorithmException {
        //This method checks the null value as well
        boolean valid = D1NodeService.isValidIdentifier(pid);
        if (valid) {
            return hashStore.storeMetadata(metadata, pid.getValue(), formatId);
        } else {
            throw new IllegalArgumentException("The pid should not be null, blank, or containing "
                                                + "the white spaces in the storeMetadata method.");
        }
    }

    /**
     * @see #storeMetadata(InputStream, Identifier, String)
     *
     *      If the '(InputStream metadata, String pid)' signature is used, the metadata format
     *      stored will default to `sysmeta`.
     */
    public String storeMetadata(InputStream metadata, Identifier pid) throws IOException,
            IllegalArgumentException, FileNotFoundException, InterruptedException,
            NoSuchAlgorithmException {
        //This method checks the null value as well
        boolean valid = D1NodeService.isValidIdentifier(pid);
        if (valid) {
            return hashStore.storeMetadata(metadata, pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null, blank, or containing "
                                                + "the white spaces in the storeMetadata method.");
        }
    }

    /**
     * Returns an InputStream to an object from HashStore using a given persistent identifier.
     *
     * @param pid Authority-based identifier
     * @return Object InputStream
     * @throws IllegalArgumentException When pid is null or empty
     * @throws FileNotFoundException    When requested pid has no associated object
     * @throws IOException              I/O error when creating InputStream to object
     * @throws NoSuchAlgorithmException When algorithm used to calculate object address is not
     *                                  supported
     */
    public InputStream retrieveObject(Identifier pid) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        if (pid != null) {
            return hashStore.retrieveObject(pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " retrieveObject method.");
        }
    }

    /**
     * Returns an InputStream to the metadata content of a given pid and metadata namespace from
     * HashStore.
     *
     * @param pid      Authority-based identifier
     * @param formatId Metadata namespace/format
     * @return Metadata InputStream
     * @throws IllegalArgumentException When pid/formatId is null or empty
     * @throws FileNotFoundException    When requested pid+formatId has no associated object
     * @throws IOException              I/O error when creating InputStream to metadata
     * @throws NoSuchAlgorithmException When algorithm used to calculate metadata address is not
     *                                  supported
     */
    public InputStream retrieveMetadata(Identifier pid, String formatId)
            throws IllegalArgumentException, FileNotFoundException, IOException,
            NoSuchAlgorithmException {
        if (pid != null) {
            return hashStore.retrieveMetadata(pid.getValue(), formatId);
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " retrieveMetadata method.");
        }
    }

    /**
     * @see #retrieveMetadata(Identifier, String)
     *
     *      If `retrieveMetadata` is called with signature (String pid), the metadata
     *      document retrieved will be the given pid's 'sysmeta'
     */
    public InputStream retrieveMetadata(Identifier pid) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        if (pid != null) {
            return hashStore.retrieveMetadata(pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " retrieveMetadata method.");
        }
    }

    /**
     * Deletes an object and its related data permanently from HashStore using a given
     * persistent identifier. If the `idType` is 'pid', the object associated with the pid will
     * be deleted if it is not referenced by any other pids, along with its reference files and
     * all metadata documents found in its respective metadata directory. If the `idType` is
     * 'cid', only the object will be deleted if it is not referenced by other pids.
     *
     * Notes: All objects are renamed at their existing path with a '_deleted' appended
     * to their file name before they are deleted.
     *
     * @param idType 'pid' or 'cid'
     * @param id     Authority-based identifier or content identifier
     * @throws IllegalArgumentException When pid is null or empty
     * @throws IOException              I/O error when deleting empty directories,
     *                                  modifying/deleting reference files
     * @throws NoSuchAlgorithmException When algorithm used to calculate an object or metadata's
     *                                  address is not supported
     * @throws InterruptedException     When deletion synchronization is interrupted
     */
    public void deleteObject(String idType, String id) throws IllegalArgumentException,
            IOException, NoSuchAlgorithmException, InterruptedException {
        hashStore.deleteObject(idType, id);
    }

    /**
     * Deletes an object and all relevant associated files (ex. system metadata, reference
     * files, etc.) based on a given pid. If other pids still reference the pid's associated
     * object, the object will not be deleted.
     *
     * @param pid Authority-based identifier
     * @see #deleteObject(String, String) for more details.
     */
    public void deleteObject(Identifier pid) throws IllegalArgumentException, IOException,
            NoSuchAlgorithmException, InterruptedException {
        if (pid != null) {
            hashStore.deleteObject(pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " deleteObject method.");
        }
    }

    /**
     * Deletes a metadata document (ex. `sysmeta`) permanently from HashStore using a given
     * persistent identifier and its respective metadata namespace.
     *
     * @param pid      Authority-based identifier
     * @param formatId Metadata namespace/format
     * @throws IllegalArgumentException When pid or formatId is null or empty
     * @throws IOException              I/O error when deleting metadata or empty directories
     * @throws NoSuchAlgorithmException When algorithm used to calculate object address is not
     *                                  supported
     * @throws InterruptedException     Issue with synchronization on metadata doc
     */
    public void deleteMetadata(Identifier pid, String formatId) throws IllegalArgumentException,
            IOException, NoSuchAlgorithmException, InterruptedException {
        if (pid != null) {
            hashStore.deleteMetadata(pid.getValue(), formatId);
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " deleteMetadata method.");
        }
    }

    /**
     * Deletes all metadata related for the given 'pid' from HashStore
     *
     * @param pid Authority-based identifier
     * @throws IllegalArgumentException If pid is invalid
     * @throws IOException              I/O error when deleting metadata or empty directories
     * @throws NoSuchAlgorithmException When algorithm used to calculate object address is not
     *                                  supported
     * @throws InterruptedException     Issue with synchronization on metadata doc
     */
    public void deleteMetadata(Identifier pid) throws IllegalArgumentException, IOException,
                                                NoSuchAlgorithmException, InterruptedException {
        if (pid != null) {
            hashStore.deleteMetadata(pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " deleteMetadata method.");
        }
    }

    /**
     * Calculates the hex digest of an object that exists in HashStore using a given persistent
     * identifier and hash algorithm.
     *
     * @param pid       Authority-based identifier
     * @param algorithm Algorithm of desired hex digest
     * @return String hex digest of requested pid
     * @throws IllegalArgumentException When pid or formatId is null or empty
     * @throws FileNotFoundException    When requested pid object does not exist
     * @throws IOException              I/O error when calculating hex digests
     * @throws NoSuchAlgorithmException When algorithm used to calculate object address is not
     *                                  supported
     */
    public String getHexDigest(Identifier pid, String algorithm) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        if (pid != null) {
            return hashStore.getHexDigest(pid.getValue(), algorithm);
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " getHexDigest method.");
        }
    }

    /**
     * Get the default namespace in the storage system
     * @return the default namespace
     */
    public String getDefaultNameSpace() {
        return defaultNamespace;
    }

    private ObjectInfo convertToObjectInfo(ObjectMetadata objMeta) {
        ObjectInfo info = null;
        if (objMeta != null) {
            info = new ObjectInfo(objMeta.getPid(), objMeta.getCid(),
                                  objMeta.getSize(), objMeta.getHexDigests());
        }
        return info;
    }

    private ObjectMetadata convertToObjectMetadata(ObjectInfo objInfo) {
        ObjectMetadata metadata = null;
        if (objInfo != null) {
            metadata = new ObjectMetadata(objInfo.getPid(), objInfo.getCid(),
                                          objInfo.getSize(), objInfo.getHexDigests());
        }
        return metadata;
    }

}
