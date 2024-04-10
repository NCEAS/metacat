package edu.ucsb.nceas.metacat.shared;

/**
 * Exception thrown when an error occurs in a configuration administrative
 * class
 */
public class BaseException extends Exception {


	private static final long serialVersionUID = 1331212806605660813L;
	
	private static String _coreMessage;
	
	/**
	 * Create a new AccessException.
	 *
	 * @param message The error or warning message.
	 */
	public BaseException(String message) {
		super(message);
		_coreMessage = message;
	}
	
	/**
	 * Create a new BaseException.
	 *
	 * @param message The error or warning message.
	 */
	public BaseException(String message, BaseException deeperException) {
		super(message + " --> " + deeperException.getMessage());
		_coreMessage = deeperException.getCoreMessage();
	}
	
	public String getCoreMessage() {
		return _coreMessage;
	}
	
	public void setCoreMessage(String coreMessage) {
		_coreMessage = coreMessage;
	}
	


}
