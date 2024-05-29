package edu.ucsb.nceas.metacat.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.dataone.hashstore.ObjectMetadata;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;

/**
 * The HashStore implementation of the Storage interface.
 */
public class HashStorage implements Storage {

    @Override
    public ObjectMetadata storeObject(InputStream object, Identifier pid, String additionalAlgorithm,
                                      String checksum, String checksumAlgorithm, long objSize)
                                     throws NoSuchAlgorithmException, IOException, InvalidRequest,
                                     RuntimeException, InterruptedException {
        return null;
    }


    @Override
    public ObjectMetadata storeObject(InputStream object) throws NoSuchAlgorithmException,
            IOException, InvalidRequest, RuntimeException, InterruptedException {
        return null;
    }

    @Override
    public ObjectMetadata storeObject(InputStream object, Identifier pid, String checksum,
                                      String checksumAlgorithm, long objSize)
                                 throws NoSuchAlgorithmException, IOException, InvalidRequest,
                                         RuntimeException, InterruptedException {
        return null;
    }

    @Override
    public ObjectMetadata storeObject(InputStream object, Identifier pid, String checksum,
                                     String checksumAlgorithm) throws NoSuchAlgorithmException,
                           IOException, InvalidRequest,RuntimeException, InterruptedException {
        return null;
    }

    @Override
    public ObjectMetadata storeObject(InputStream object, Identifier pid, String additionalAlgorithm)
                throws NoSuchAlgorithmException, IOException, InvalidRequest,
                       RuntimeException, InterruptedException {
        return null;
    }

    @Override
    public ObjectMetadata storeObject(InputStream object, Identifier pid, long objSize)
            throws NoSuchAlgorithmException, IOException, InvalidRequest,
            RuntimeException, InterruptedException {
        return null;
    }

    @Override
    public void tagObject(Identifier pid, String cid) throws IOException,
            InvalidRequest, NoSuchAlgorithmException, FileNotFoundException, InterruptedException {
        
    }

    @Override
    public boolean verifyObject( ObjectMetadata objectInfo, String checksum,
            String checksumAlgorithm, long objSize) throws IllegalArgumentException {
        return false;
    }

    @Override
    public String findObject(Identifier pid) throws NoSuchAlgorithmException, IOException, NotFound {
        return null;
    }

    @Override
    public String storeMetadata(InputStream metadata, Identifier pid, String formatId)
            throws IOException, IllegalArgumentException, FileNotFoundException,
            InterruptedException, NoSuchAlgorithmException {
        return null;
    }

    @Override
    public String storeMetadata(InputStream metadata, Identifier pid) throws IOException,
            IllegalArgumentException, FileNotFoundException, InterruptedException,
            NoSuchAlgorithmException {
        return null;
    }

    @Override
    public InputStream retrieveObject(Identifier pid) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        return null;
    }

    @Override
    public InputStream retrieveMetadata(Identifier pid, String formatId)
            throws IllegalArgumentException, FileNotFoundException, IOException,
            NoSuchAlgorithmException {
        return null;
    }

    @Override
    public InputStream retrieveMetadata(Identifier pid) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        return null;
    }

    @Override
    public void deleteObject(String idType, String id) throws IllegalArgumentException,
            IOException, NoSuchAlgorithmException, InterruptedException {
        
    }

    @Override
    public void deleteObject(Identifier pid) throws IllegalArgumentException, IOException,
            NoSuchAlgorithmException, InterruptedException {
        
    }

    @Override
    public void deleteMetadata(Identifier pid, String formatId) throws IllegalArgumentException,
            IOException, NoSuchAlgorithmException {
        
    }

    @Override
    public void deleteMetadata(Identifier pid) throws IllegalArgumentException, IOException,
            NoSuchAlgorithmException {
    }

    @Override
    public String getHexDigest(Identifier pid, String algorithm) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        return null;
    }

}
