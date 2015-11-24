/**
 *  '$RCSfile$'
 *    Purpose: An Exception thrown when an error occurs because a
 *             document with a given ID could not be found in the 
 *             metacat database.
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

/**
 * Exception thrown when an error occurs because a document with a
 * given ID could not be found in the metacat database.  
 */
public class McdbDocNotFoundException extends McdbException {

  // String sto stroe which docid couldn't find
  private String unfoundDocId = null;
  private String unfoundRevision = null;
  
  /**
   * Create a new McdbDocNotFoundException.
   */
  public McdbDocNotFoundException() {
    super();
    unfoundDocId = null;
    unfoundRevision = null;
  }

  /**
   * Create a new McdbDocNotFoundException.
   *
   * @param message The error or warning message.
   */
  public McdbDocNotFoundException(String message, String givenDocId, 
                                                    String givenRevision) {
    super(message);
    unfoundDocId = givenDocId;
    unfoundRevision = givenRevision;
  }

  /**
   * Create a new exception but only set the message.
   * @param message a message giving information about why the document was not found
   */
  public McdbDocNotFoundException(String message) {
      super(message);
  }
  
  /**
   * Create a new McdbDocNotFoundException.
   *
   * @param e The exception to tunnel inside this exception
   */
  public McdbDocNotFoundException(Exception e) {
    super(e);
  }

  /**
   * Create a new McdbDocNotFoundException.
   *
   * @param message The error or warning message.
   * @param e The exception to tunnel inside this exception
   */
  public McdbDocNotFoundException(String message, Exception e) {
    super(message, e);
  }
  
  /**
   * Method to get the docid which couldn't be found
   */
  public String getUnfoundDocId ()
  {
    return unfoundDocId;
  }// getUnfoundDocid
  
  /**
   * Method to get the docid's revsion which couldn't be found
   */
  public String getUnfoundRevision ()
  {
    return unfoundRevision;
  }// getUnfoundDocid
}
