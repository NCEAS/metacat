package edu.ucsb.nceas.metacat.shared;


/**
 * Exception thrown when an error occurs in a configuration administrative
 * class
 */
public class AccessException extends BaseException {
	
	private static final long serialVersionUID = -8436697355629175917L;

	/**
	 * Create a new AccessException.
	 *
	 * @param message The error or warning message.
	 */
	public AccessException(String message) {
		super(message);
	}
	
	public AccessException(String message, BaseException deeperException) {
		super(message, deeperException);
	}
}
