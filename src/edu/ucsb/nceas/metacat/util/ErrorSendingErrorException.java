package edu.ucsb.nceas.metacat.util;
/**
 * Exception thrown when an error occurs in a utility method
 */
public class ErrorSendingErrorException extends Exception {

	private static final long serialVersionUID = 2470535012803585651L;

	/**
	 * Create a new AdminException.
	 *
	 * @param message The error or warning message.
	 */
	public ErrorSendingErrorException(String message) {
		super(message);
	}
}
