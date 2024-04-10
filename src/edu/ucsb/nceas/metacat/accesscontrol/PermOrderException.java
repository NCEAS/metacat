package edu.ucsb.nceas.metacat.accesscontrol;

import edu.ucsb.nceas.metacat.shared.BaseException;


/**
 * Exception thrown when an error occurs with a permission order
 */
public class PermOrderException extends BaseException {
	
	private static final long serialVersionUID = -8436697355629175917L;

	/**
	 * Create a new PermOrderException.
	 *
	 * @param message The error or warning message.
	 */
	public PermOrderException(String message) {
		super(message);
	}
	
	public PermOrderException(String message, BaseException deeperException) {
		super(message, deeperException);
	}
}
