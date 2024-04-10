package edu.ucsb.nceas.metacat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Exception thrown when an error occurs because a problem occurred in
 * the metacat database.  This is the general type of Exception that is
 * thrown whenever the server encounters an Error or Exception that is
 * outside of the scope of normal operation.  This class may be 
 * subclassed to provide more detailed informatin about the error.
 */
public class McdbException extends Exception {

  /**
   * @serial The embedded exception if wrapping an Exception
   */
  private Exception exception;

  /**
   * Create a new McdbException.
   */
  public McdbException() {
    super();
    this.exception = null;
  }

  /**
   * Create a new McdbException.
   *
   * @param message The error or warning message.
   */
  public McdbException(String message) {
    super(message);
    this.exception = null;
  }

  /**
   * Create a new McdbException.
   *
   * @param e The exception to tunnel inside this exception
   */
  public McdbException(Exception e) {
    super();
    this.exception = e;
  }

  /**
   * Create a new McdbException.
   *
   * @param message The error or warning message.
   * @param e The exception to tunnel inside this exception
   */
  public McdbException(String message, Exception e) {
    super(message);
    this.exception = e;
  }

  /**
   * Get the tunneled Exception
   */
  public Exception getException() {
    return exception;
  }

  /**
   * Get the message from this exception.
   *
   * <p>This returns the message from this exception, but if it
   * is null, and if the tunnelled exception is not null, then
   * it returns the message fromthe tunnelled exception.
   */
  public String getMessage() {
    String msg = super.getMessage();

    if (msg == null && exception != null) {
      return exception.getMessage();
    }

    return msg;
  }

  /**
   * Print the message from this exception in XML format.
   *
   * <p>This returns the message from this exception, but if it
   * is null, and if the tunnelled exception is not null, then
   * it returns the message from the tunnelled exception.
   */
  public void toXml(Writer pw) {
    String msg = super.getMessage();

    if (msg == null && exception != null) {
      msg = exception.getMessage();
    }

    try {
		pw.write("<?xml version=\"1.0\"?>");
		pw.write("<error>" + msg + "</error>");
	    pw.flush();
	} catch (IOException e) {
		e.printStackTrace();
	}
    
  }
}
