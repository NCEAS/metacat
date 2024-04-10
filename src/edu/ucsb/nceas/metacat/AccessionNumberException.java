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
