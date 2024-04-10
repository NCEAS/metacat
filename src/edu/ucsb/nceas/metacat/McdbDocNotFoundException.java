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
