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
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * The HashStore implementation of the Storage interface.
 */
public class HashStorage implements Storage {
    private static Log logMetacat = LogFactory.getLog(HashStorage.class);
    private static HashStorage hashStorage;
    private HashStore hashStore;

    /**
     * Private constructor
     * @param className the name of the implementation class
     * @throws PropertyNotFoundException
     * @throws IOException
     * @throws HashStoreFactoryException
     */
    private HashStorage(String className) throws PropertyNotFoundException,
                                                           HashStoreFactoryException, IOException {
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
        String defaultNamespace = "https://ns.dataone.org/service/types/v2.0#SystemMetadata";
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
     * @param className the name of the implementation class
     * @return the instance of the class
     * @throws ServiceException
     * @throws PropertyNotFoundException
     */
    public static HashStorage getInstance(String className) throws ServiceException,
                                                                    PropertyNotFoundException {
        if(hashStorage == null) {
            synchronized(HashStorage.class) {
                if (hashStorage == null) {
                    try {
                        hashStorage = new HashStorage(className);
                    } catch (IOException e) {
                        throw new ServiceException("HashStore initialization failed since "
                                                   + e.getMessage());
                    }
                }
             }
        }
        return hashStorage;
    }

    @Override
    public ObjectInfo storeObject(InputStream object, Identifier pid, String additionalAlgorithm,
                                      String checksum, String checksumAlgorithm, long objSize)
                                     throws NoSuchAlgorithmException, IOException, InvalidRequest,
                                     RuntimeException, InterruptedException {
        if (pid != null) {
            ObjectMetadata objMeta = hashStore.storeObject(object, pid.getValue(),
                                        additionalAlgorithm, checksum, checksumAlgorithm, objSize);
            return convertToObjectInfo(objMeta);
        } else {
            throw new InvalidRequest("0000", "The stored pid should not be null in the"
                                                + " storeObject method.");
        }
    }


    @Override
    public ObjectInfo storeObject(InputStream object) throws NoSuchAlgorithmException,
            IOException, InvalidRequest, RuntimeException, InterruptedException {
        ObjectMetadata objMeta = hashStore.storeObject(object);
        return convertToObjectInfo(objMeta);
    }

    @Override
    public void tagObject(Identifier pid, String cid) throws IOException,
            InvalidRequest, NoSuchAlgorithmException, FileNotFoundException, InterruptedException {
        if (pid != null) {
            hashStore.tagObject(pid.getValue(), cid);
        } else {
            throw new InvalidRequest("0000", "The stored pid should not be null in"
                                                + " the tagObject method.");
        }
    }

    @Override
    public void verifyObject( ObjectInfo objectInfo, String checksum,
            String checksumAlgorithm, long objSize) throws IllegalArgumentException, IOException {
        hashStore.verifyObject(convertToObjectMetadata(objectInfo), checksum,
                                checksumAlgorithm, objSize);
    }

    @Override
    public String storeMetadata(InputStream metadata, Identifier pid, String formatId)
            throws IOException, IllegalArgumentException, FileNotFoundException,
            InterruptedException, NoSuchAlgorithmException {
        if (pid != null) {
            return hashStore.storeMetadata(metadata, pid.getValue(), formatId);
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " storeMetadata method.");
        }
    }

    @Override
    public String storeMetadata(InputStream metadata, Identifier pid) throws IOException,
            IllegalArgumentException, FileNotFoundException, InterruptedException,
            NoSuchAlgorithmException {
        if (pid != null) {
            return hashStore.storeMetadata(metadata, pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " storeMetadata method.");
        }
    }

    @Override
    public InputStream retrieveObject(Identifier pid) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        if (pid != null) {
            return hashStore.retrieveObject(pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " retrieveObject method.");
        }
    }

    @Override
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

    @Override
    public InputStream retrieveMetadata(Identifier pid) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        if (pid != null) {
            return hashStore.retrieveMetadata(pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " retrieveMetadata method.");
        }
    }

    @Override
    public void deleteObject(String idType, String id) throws IllegalArgumentException,
            IOException, NoSuchAlgorithmException, InterruptedException {
        hashStore.deleteObject(idType, id);
    }

    @Override
    public void deleteObject(Identifier pid) throws IllegalArgumentException, IOException,
            NoSuchAlgorithmException, InterruptedException {
        if (pid != null) {
            hashStore.deleteObject(pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " deleteObject method.");
        }
    }

    @Override
    public void deleteMetadata(Identifier pid, String formatId) throws IllegalArgumentException,
            IOException, NoSuchAlgorithmException, InterruptedException {
        if (pid != null) {
            hashStore.deleteMetadata(pid.getValue(), formatId);
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " deleteMetadata method.");
        }
    }

    @Override
    public void deleteMetadata(Identifier pid) throws IllegalArgumentException, IOException,
                                                NoSuchAlgorithmException, InterruptedException {
        if (pid != null) {
            hashStore.deleteMetadata(pid.getValue());
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                + " deleteMetadata method.");
        }
    }

    @Override
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
     * If the object doesn't exist, it return null.
     */
    @Override
    public File findObject(Identifier pid) throws NoSuchAlgorithmException,
                                                            IllegalArgumentException, IOException {
        File object = null;
        if (pid != null) {
             Map<String, String> map = hashStore.findObject(pid.getValue());
             if (map != null) {
                 String path = map.get("cid_object_path");
                 if (path != null && !path.isBlank()) {
                     object = new File(path);
                    if (!object.exists()) {
                        object = null;
                    }
                 }
             }
             if (object != null) {
                 logMetacat.debug("The file path for object " + pid.getValue() + " is "
                                                                     + object.getAbsolutePath());
             } else {
                 logMetacat.debug("The file path for object " + pid.getValue() + " is null.");
             }
        } else {
            throw new IllegalArgumentException("The pid should not be null in the"
                                                        + " findObject method.");
        }

        return object;
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
