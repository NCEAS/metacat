package edu.ucsb.nceas.metacat.restservice.multipart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.dataone.service.types.v1.Checksum;


/**
 * A FileInputStream class with more information, such as the object of source file itself and the expected checksum of the source file. 
 * @author tao
 *
 */
public class DetailedFileInputStream extends FileInputStream {
    private Checksum expectedChecksum = null;
    private File file = null;
    
    /**
     * Constructor
     * @param file  the source file where the input stream comes from
     * @param expectedChecksum  the expected checksum of the source file 
     * @throws FileNotFoundException
     */
    public DetailedFileInputStream (File file, Checksum expectedChecksum) throws FileNotFoundException {
        super(file);
        this.file = file;
        this.expectedChecksum = expectedChecksum;
    }
    
    /**
     * Get the expected checksum of the source file
     * @return the checksum of the source file
     */
    public Checksum getExpectedChecksum() {
        return this.expectedChecksum;
    }

    /**
     * Get the source file
     * @return the source file object
     */
    public File getFile() {
        return file;
    }
    
    
}
