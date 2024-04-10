package edu.ucsb.nceas.metacat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * (on insert of XML document)
 * Generates a unique Accession Number or if provided check it
 * for uniqueness and register it into the db connection
 * (on update or delete of XML document)
 * Check for existance of provided Accession Number
 *
 */
public class AccessionNumber  {

  private String sitecode = null;
  private String sep = null;
  private String docid = null;
  private String rev = null;

    /**
	 * Construct an AccessionNumber
	 */
	private AccessionNumber() throws AccessionNumberException {
		try {
			this.sitecode = PropertyService.getProperty("document.sitecode");
			this.sep = PropertyService.getProperty("document.accNumSeparator");
		} catch (PropertyNotFoundException pnfe) {
			throw new AccessionNumberException("Could not retrieve property "
					+ "in constructor: " + pnfe.getMessage());
		}
	}

  /**
	 * NEW - WHEN CLIENT ALWAYS PROVIDE ACCESSION NUMBER INCLUDING REV IN IT
	 * Construct an AccessionNumber
	 * 
	 * @param conn
	 *            the db connection to read Accession number from
	 * @param accnum
	 *            the accession number to be checked for validness
	 */
  public AccessionNumber (String accnum, String action)
           throws AccessionNumberException, SQLException, NumberFormatException
 {
    this();

    this.rev = null;
    this.docid = accnum;
    if ( accnum != null ) {
      int firstIndex = accnum.indexOf(this.sep);
      int lastIndex = accnum.lastIndexOf(this.sep);
      if ( firstIndex != lastIndex ) {
        //this docid contains a revision number
        this.rev = accnum.substring(lastIndex + 1);
        this.docid = accnum.substring(0, lastIndex);
      }
    }

    // INSERT
    if ( action.equals("INSERT")) {

        if(rev != null){
            try {
                Integer.parseInt(rev);
            }
            catch (java.lang.NumberFormatException e) {
                throw new AccessionNumberException(
                    "Revision number is required");
            }
        }

      // check accession number for validness
      if ( docid == null ) {
        throw new AccessionNumberException("Accession number is required");

      // rev is not provided; throw an exception to prevent the insertion
      } else if ( rev == null ) {
        throw new AccessionNumberException
                  ("Revision number is required");

      // docid is used; throw an exception to prevent the insertion
      } else if ( accNumberUsed(docid) ) {
        throw new AccessionNumberException
                  ("Accession number " + docid + " is already in use");

      // rev is <> 1; throw an exception to prevent the insertion
      } /*else if ( !rev.equals("1")) {
        throw new AccessionNumberException("Revision number must be 1");
      }*/

    // UPDATE or DELETE
    } else if ( action.equals("UPDATE") || action.equals("DELETE")) {
      String l_rev = "";

      int reversionNumber = 1;

      if(rev != null){
          try{
              reversionNumber = Integer.parseInt(rev);
          } catch (java.lang.NumberFormatException e){
              throw new AccessionNumberException(
                      "Revision number is required");
          }
      }

      // Accession# is not provided; throw an exception to prevent the action
      if ( docid == null ) {
        throw new AccessionNumberException("Accession number is required");

      // rev is not provided; throw an exception to prevent the action
      } else if ( rev == null ) {
        throw new AccessionNumberException
                  ("Revision number is required");

      // Accession# is not current (not in xml_documents or xml_revisions); throw an exception
      } else if ( !accNumberIsCurrent(docid) ) {
        throw new AccessionNumberException
                  ("Document not found for Accession number " + docid);

      //Revision number is less than or equal the recent one; throw a exception
      } else if ( action.equals("UPDATE") &&
                  reversionNumber <= getLastRevisionNumber(docid) ) {
        throw new AccessionNumberException
                 ("Next revision number can't be less than or equal to "
                                              + getLastRevisionNumber(docid));

      // Revision number is not the recent one; throw an exception
      } else if ( action.equals("DELETE") &&
                  !rev.equals(l_rev = getLastRevision(docid)) ) {
        throw new AccessionNumberException
                  ("Last revision number is "+ l_rev);
      }
    }
  }

  /** check for existence of Accesssion Number xml_acc_numbers table */
  public static boolean accNumberUsed ( String accNumber )
                  throws SQLException {

    boolean hasAccNumber = false;
    DBConnection conn = null;
    int serialNumber = -1;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection("AccessionNumber.accNumberUsed");
      serialNumber=conn.getCheckOutSerialNumber();
      pstmt = conn.prepareStatement(
                "SELECT 'x' FROM xml_documents " +
                "WHERE docid = ? " +
                "UNION " +
                "SELECT 'x' FROM xml_revisions " +
                "WHERE docid = ?");
      pstmt.setString(1,accNumber);
      pstmt.setString(2,accNumber);
      pstmt.execute();
      rs = pstmt.getResultSet();
      hasAccNumber = rs.next();
      //pstmt.close();

    } catch (SQLException e) {
      throw new SQLException
      ("Error on AccessionNumber.accNumberUsed(accNumber): " + e.getMessage());
    }
    finally
    {
       try {
           if(rs != null) {
               rs.close();
           }
           if(pstmt != null) {
               pstmt.close();
           }
       } finally {
           DBConnectionPool.returnDBConnection(conn, serialNumber);
       }
      
    }

    return hasAccNumber;
  }

  // Check for existence of Accesssion Number in xml_documents or xml_revisions
  // table.  We check xml_revisions because document may have been deleted, which 
  // will remove it from xml_documents, but a revision still exists.
  private boolean accNumberIsCurrent(String accNumber) throws SQLException {

    boolean hasCurrentAccNumber = false;
    DBConnection conn = null;
    int serialNumber = -1;
    ResultSet rs = null;
    PreparedStatement pstmt = null;
    try {
     
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection("AccessionNumber.accNumberIsCurre");
      serialNumber=conn.getCheckOutSerialNumber();
      pstmt = conn.prepareStatement(
                "SELECT 'x' FROM xml_documents " +
                "WHERE docid = ?");
      pstmt.setString(1, accNumber);
      pstmt.execute();
      rs = pstmt.getResultSet();
      hasCurrentAccNumber = rs.next();
      if(!hasCurrentAccNumber)
      {
        //need to look xml_revision table;
        pstmt = conn.prepareStatement(
            "SELECT 'x' FROM xml_revisions " +
            "WHERE docid = ?");
        pstmt.setString(1, accNumber);
        pstmt.execute();
        rs = pstmt.getResultSet();
        hasCurrentAccNumber = rs.next();
      }
      //pstmt.close();

    } catch (SQLException e) {
      throw new SQLException(
          "Error on AccessionNumber.accNumberIsCurrent(String accNumber): " +
          e.getMessage());
    }
    finally
    {
       
       try {
           if(rs != null) {
               rs.close();
           }
           if(pstmt != null) {
               pstmt.close();
           }
       } finally {
           DBConnectionPool.returnDBConnection(conn, serialNumber);
       }
      
    }

    return hasCurrentAccNumber;
  }

  // get the recent revision number for docid
  private String getLastRevision(String docid) throws SQLException
  {
    String rev = "";
    DBConnection conn = null;
    int serialNumber = -1;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection("AccessionNumber.getLastRevision");
      serialNumber=conn.getCheckOutSerialNumber();
      pstmt = conn.prepareStatement
              ("SELECT rev FROM xml_documents WHERE docid = ?");
      pstmt.setString(1, docid);
      pstmt.execute();

      rs = pstmt.getResultSet();
      boolean hasRow = rs.next();
      rev = rs.getString(1);
      //pstmt.close();

    } catch (SQLException e) {
      throw new SQLException(
      "Error on AccessionNumber.getLastRevision(): " + e.getMessage());
    }
    finally
    {
        try {
            if(rs != null) {
                rs.close();
            }
            if(pstmt != null) {
                pstmt.close();
            }
        } finally {
            DBConnectionPool.returnDBConnection(conn,serialNumber);
        }
      
    }

    return rev;
  }

  /**
    * Get last revision number from database for a docid
    * The return value is integer because we want compare it to there new one
    * @param docid <sitecode>.<uniqueid> part of Accession Number
    */
  private int getLastRevisionNumber(String docId) throws SQLException
  {
    int rev = 1;
    DBConnection conn =null;
    int serialNumber = -1;
    PreparedStatement pStmt = null;
    ResultSet rs = null;
    try {
      
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection("AccessionNumber.getLastRevisionN");
      serialNumber=conn.getCheckOutSerialNumber();

      pStmt = conn.prepareStatement
              ("SELECT rev FROM xml_documents WHERE docid = ? ");
      pStmt.setString(1, docId);
      pStmt.execute();

      rs = pStmt.getResultSet();
      boolean hasRow = rs.next();
      if(hasRow)
      {
        rev = rs.getInt(1);
      }     
      //pStmt.close();

    } catch (SQLException e) {
      throw new SQLException(
      "Error on AccessionNumber.getLastRevision(): " + e.getMessage());
    }
    finally
    {
        try {
            if(rs != null) {
                rs.close();
            }
            if(pStmt != null) {
                pStmt.close();
            }
        } finally {
            DBConnectionPool.returnDBConnection(conn,serialNumber);
        }
      
    }
    return rev;
  }

  /**
   * returns the docid encoded in this accession number
   */
  public String getDocid() {

    return this.docid;
  }

  /**
   * returns the revision number encoded in this accession number
   */
  public String getRev()
  {
    return this.rev;
  }

}
