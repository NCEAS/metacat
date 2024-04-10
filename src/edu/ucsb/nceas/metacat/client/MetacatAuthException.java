package edu.ucsb.nceas.metacat.client;

/**
 * Exception thrown when a Metacat server denies a login authentication
 * request.  Either the username or password are incorrect.
 */
public class MetacatAuthException extends Exception {

    /**
     * Create a new MetacatAuthException.
     *
     * @param message The error or warning message.
     */
    public MetacatAuthException(String message) {
        super(message);
    }
}
