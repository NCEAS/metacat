package edu.ucsb.nceas.metacat.client;

/**
 * Exception thrown when a Metacat server is not accessible for the
 * operation requested.  This can be a network outage, or a configuration error.
 */
public class MetacatInaccessibleException extends Exception {

    /**
     * Create a new MetacatInaccessibleException.
     *
     * @param message The error or warning message.
     */
    public MetacatInaccessibleException(String message) {
        super(message);
    }
}
