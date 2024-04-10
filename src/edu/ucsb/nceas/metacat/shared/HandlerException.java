package edu.ucsb.nceas.metacat.shared;

/**
 * Exception thrown when an error occurs in a configuration administrative
 * class
 */
public class HandlerException extends Exception {

	private static final long serialVersionUID = 7997030775672963304L;

	/**
	 * Create a new AdminException.
	 *
	 * @param message The error or warning message.
	 */
	public HandlerException(String message) {
		super(message);
	}
}
