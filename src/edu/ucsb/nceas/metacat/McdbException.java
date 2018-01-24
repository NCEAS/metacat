/**
 *  '$RCSfile$'
 *    Purpose: An Exception thrown when an error occurs because a
 *             problem occurred in the metacat database
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

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
