package edu.ucsb.nceas.metacat;

/**
 * Exception thrown when an error occurs in a configuration administrative
 * class
 */
public class ErrorHandledException extends Exception {

	private static final long serialVersionUID = 6155049982476516738L;

	/**
	 * Create a new AdminException.
	 *
	 * @param message The error or warning message.
	 */
	public ErrorHandledException(String message) {
		super(message);
	}
}
