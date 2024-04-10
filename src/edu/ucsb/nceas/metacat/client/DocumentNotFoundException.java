package edu.ucsb.nceas.metacat.client;

/**
 * Exception thrown when the requested docid does not exist on the server
 */
public class DocumentNotFoundException extends Exception {

    /**
     * Create a new InsufficientKarmaException.
     *
     * @param message The error or warning message.
     */
    public DocumentNotFoundException(String message) {
        super(message);
    }
}
