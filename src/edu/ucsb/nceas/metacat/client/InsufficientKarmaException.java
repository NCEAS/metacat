package edu.ucsb.nceas.metacat.client;

/**
 * Exception thrown when an error occurs because a Metacat server operation
 * requires higher access rights than the currently logged in user has.
 */
public class InsufficientKarmaException extends Exception {

    /**
     * Create a new InsufficientKarmaException.
     *
     * @param message The error or warning message.
     */
    public InsufficientKarmaException(String message) {
        super(message);
    }
}
