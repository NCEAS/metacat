package edu.ucsb.nceas.metacat;

/**
 * Exception thrown when an error occurs in a configuration administrative
 * class
 */
public class AuthTLSException extends Exception {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 525418630212063646L;

	/**
	 * Create a new AuthTLSException.
	 *
	 * @param message The error or warning message.
	 */
	public AuthTLSException(String message) {
		super(message);
	}
}
