package edu.ucsb.nceas.metacat.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.dataone.hashstore.ObjectMetadata;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;

/**
 * The HashStore implementation of the Storage interface.
 */
public class HashStorage implements Storage {

    @Override
    public ObjectMetadata storeObject(InputStream object, String pid, String additionalAlgorithm,
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
    public ObjectMetadata storeObject(InputStream object, String pid, String checksum,
                                      String checksumAlgorithm, long objSize)
                                 throws NoSuchAlgorithmException, IOException, InvalidRequest,
                                         RuntimeException, InterruptedException {
        return null;
    }

    @Override
    public ObjectMetadata storeObject(InputStream object, String pid, String checksum,
                                     String checksumAlgorithm) throws NoSuchAlgorithmException,
                           IOException, InvalidRequest,RuntimeException, InterruptedException {
        return null;
    }

    @Override
    public ObjectMetadata storeObject(InputStream object, String pid, String additionalAlgorithm)
                throws NoSuchAlgorithmException, IOException, InvalidRequest,
                       RuntimeException, InterruptedException {
        return null;
    }

    @Override
    public ObjectMetadata storeObject(InputStream object, String pid, long objSize)
            throws NoSuchAlgorithmException, IOException, InvalidRequest,
            RuntimeException, InterruptedException {
        return null;
    }

    @Override
    public void tagObject(String pid, String cid) throws IOException,
            InvalidRequest, NoSuchAlgorithmException, FileNotFoundException, InterruptedException {
        
    }

    @Override
    public boolean verifyObject( ObjectMetadata objectInfo, String checksum,
            String checksumAlgorithm, long objSize) throws IllegalArgumentException {
        return false;
    }

    @Override
    public String findObject(String pid) throws NoSuchAlgorithmException, IOException, NotFound {
        return null;
    }

    @Override
    public String storeMetadata(InputStream metadata, String pid, String formatId)
            throws IOException, IllegalArgumentException, FileNotFoundException,
            InterruptedException, NoSuchAlgorithmException {
        return null;
    }

    @Override
    public String storeMetadata(InputStream metadata, String pid) throws IOException,
            IllegalArgumentException, FileNotFoundException, InterruptedException,
            NoSuchAlgorithmException {
        return null;
    }

    @Override
    public InputStream retrieveObject(String pid) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        return null;
    }

    @Override
    public InputStream retrieveMetadata(String pid, String formatId)
            throws IllegalArgumentException, FileNotFoundException, IOException,
            NoSuchAlgorithmException {
        return null;
    }

    @Override
    public InputStream retrieveMetadata(String pid) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        return null;
    }

    @Override
    public void deleteObject(String idType, String id) throws IllegalArgumentException,
            IOException, NoSuchAlgorithmException, InterruptedException {
        
    }

    @Override
    public void deleteObject(String pid) throws IllegalArgumentException, IOException,
            NoSuchAlgorithmException, InterruptedException {
        
    }

    @Override
    public void deleteMetadata(String pid, String formatId) throws IllegalArgumentException,
            IOException, NoSuchAlgorithmException {
        
    }

    @Override
    public void deleteMetadata(String pid) throws IllegalArgumentException, IOException,
            NoSuchAlgorithmException {
    }

    @Override
    public String getHexDigest(String pid, String algorithm) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        return null;
    }

}
