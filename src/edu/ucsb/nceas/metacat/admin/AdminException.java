package edu.ucsb.nceas.metacat.admin;

/**
 * Exception thrown when an error occurs in a configuration administrative
 * class
 */
public class AdminException extends Exception {
	
	private static final long serialVersionUID = -7190381642745794017L;

	/**
	 * Create a new AdminException.
	 *
	 * @param message The error or warning message.
	 */
	public AdminException(String message) {
		super(message);
	}
}
