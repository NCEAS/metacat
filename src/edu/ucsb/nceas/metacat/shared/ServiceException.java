package edu.ucsb.nceas.metacat.shared;


/**
 * Exception thrown when an error occurs in a configuration administrative
 * class
 */
public class ServiceException extends BaseException {
	
	private static final long serialVersionUID = -2982801310798091071L;

	/**
	 * Create a new AdminException.
	 *
	 * @param message The error or warning message.
	 */
	public ServiceException(String message) {
		super(message);
	}
	
	public ServiceException(String message, BaseException deeperException) {
		super(message, deeperException);
	}
}
