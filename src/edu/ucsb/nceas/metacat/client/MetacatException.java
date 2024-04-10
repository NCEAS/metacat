package edu.ucsb.nceas.metacat.client;

/**
 * Exception thrown when an error occurs during a metacat operation.
 */
public class MetacatException extends Exception {

    /**
     * Create a new MetacatException.
     *
     * @param message The error or warning message.
     */
    public MetacatException(String message) {
        super(message);
    }
}
