package edu.ucsb.nceas.metacat.restservice.multipart;

import java.io.File;

import org.dataone.service.types.v1.Checksum;

/**
 * A file object with the expected checksum
 * @author tao
 *
 */
public class CheckedFile extends File {
    
    private Checksum checksum = null;
    
    /**
     * Constructor
     * @param path 
     * @param checksum
     */
    public CheckedFile(String path, Checksum checksum) {
        super(path);
        this.checksum = checksum;
    }
    
    /**
     * Get the checksum of this file
     * @return the checksum of this file
     */
    public Checksum getChecksum() {
        return this.checksum;
    }

}
