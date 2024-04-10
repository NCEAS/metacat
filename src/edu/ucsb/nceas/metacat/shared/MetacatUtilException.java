package edu.ucsb.nceas.metacat.shared;
/**
 * Exception thrown when an error occurs in a utility method
 */
public class MetacatUtilException extends Exception {

	private static final long serialVersionUID = -253071242028406390L;

    /**
	 * Create a new AdminException.
	 *
	 * @param message The error or warning message.
	 */
	public MetacatUtilException(String message) {
		super(message);
	}
}
