package edu.ucsb.nceas.metacat.accesscontrol;

import edu.ucsb.nceas.metacat.shared.BaseException;


/**
 * Exception thrown when an error occurs during access control
 */
public class AccessControlException extends BaseException {

	private static final long serialVersionUID = 6685974477000997612L;

	/**
	 * Create a new AccessControlException.
	 *
	 * @param message The error or warning message.
	 */
	public AccessControlException(String message) {
		super(message);
	}
	
	public AccessControlException(String message, BaseException deeperException) {
		super(message, deeperException);
	}
}
