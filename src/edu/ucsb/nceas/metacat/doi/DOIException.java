package edu.ucsb.nceas.metacat.doi;

    
/**
 * An exception that encapsulates errors from the DOI service.
*/
public class DOIException extends Exception {
    
    /**
     * Constructor
     * @param msg  the eror message
     */
    public DOIException(String msg) {
        super(msg);
    }
}
