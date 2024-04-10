package edu.ucsb.nceas.metacat.systemmetadata;

/**
 * An exception class indicate the given system metadata is invalid.
 * @author tao
 *
 */
public class InvalidSystemMetadata extends Exception {
    
    /**
     * Constructor with the error message
     * @param error
     */
    public InvalidSystemMetadata (String error) {
        super(error);
    }

}
