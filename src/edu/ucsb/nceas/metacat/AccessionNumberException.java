/**
 *  '$RCSfile$'
 *    Purpose: An Exception thrown when an error occurs because an 
 *             AccessionNumber was invalid or used incorrectly
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
 * Exception thrown when an error occurs because an AccessionNumber was 
 * invalid or used incorrectly.
 *
 * Accession numbers are invalid under the following conditions:
 *   1) INSERT mode, and the provided accesion # doesn't contain "sep"
 *   2) UPDATE mode, and no accession # provided
 *   3) UPDATE mode, and accession # doesn't exist in xml_documents table
 *   4) DELETE mode, and no accession # provided
 *   5) DELETE mode, and accession # doesn't exist in xml_documents table
 */
public class AccessionNumberException extends Exception {
  /**
   * Create a new AccessionNumberException.
   *
   * @param message The error or warning message.
   */
  public AccessionNumberException(String message) {
    super(message);
  }
}
