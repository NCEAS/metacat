package edu.ucsb.nceas.metacat;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.sql.*;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A class to parse document ids
 * The docid is of the form siteCode.uniqueId.rev 
 */
public class DocumentIdentifier
{
  private String docid = null;
  private String rev = null;
  private String uniqueId = null;
  private String siteCode = null;
  private String separator = null;
  
  /**
	 * Constructor to build a docid object and parse an incoming string.
	 */
	public DocumentIdentifier(String docid) throws AccessionNumberException {
		this.docid = docid;
		try {
			this.separator = PropertyService.getProperty("document.accNumSeparator");
		} catch (PropertyNotFoundException pnfe) {
			throw new AccessionNumberException(
					"Could not get property 'accNumSeparator' :" + pnfe.getMessage());
		}

		if (docid.endsWith(this.separator)) {
			throw new AccessionNumberException("Accession number cannot end with "
					+ "a seperator.");
		}
		if (docid.startsWith(this.separator)) {
			throw new AccessionNumberException("Accession number cannot begin with "
					+ "a seperator.");
		}

		parseDocid();
	}
  
  /**
	 * parses the docid into its parts
	 */
  private void parseDocid() throws AccessionNumberException
  {
    try {
      int firstIndex = docid.indexOf(separator);
      int lastIndex = docid.lastIndexOf(separator);
      if(firstIndex != lastIndex)
      { //this docid contains a revision number
        rev = docid.substring(lastIndex + 1);
        uniqueId = docid.substring(firstIndex + 1, lastIndex);
        siteCode = docid.substring(0, firstIndex);
      }
      else
      {
        uniqueId = docid.substring(firstIndex + 1);
        siteCode = docid.substring(0, firstIndex);
        rev = getNewestRev();
      }
    } catch (StringIndexOutOfBoundsException e) {
      throw new 
      AccessionNumberException("Error in DocumentIdentifier.parseDocid(). " +
                               "Use accession number format as: " +
                               "sitecode" + separator + "uniqueid" + 
                               separator + "revisionid");
    } catch (SQLException e) {
      throw new 
      AccessionNumberException("Error in DocumentIdentifier.parseDocid(). " +
                               "DB Error when reading revisionid");
    } catch (ClassNotFoundException e) {
      throw new 
      AccessionNumberException("Error in DocumentIdentifier.parseDocid(). " +
                                e.getMessage());
    }
    
    try {
      if(rev.equals("newest")) {
        rev = getNewestRev();
      }
    } catch (SQLException e) {
      throw new 
      AccessionNumberException("Error in DocumentIdentifier.parseDocid(). " +
                               "DB Error when reading revisionid");
    } catch (ClassNotFoundException e) {
      throw new 
      AccessionNumberException("Error in DocumentIdentifier.parseDocid(). " +
                                e.getMessage());
    }
      
  }
  
  /**
   * returns the newest revision number for a document
   */
  private String getNewestRev() throws SQLException, ClassNotFoundException
  {
    PreparedStatement pstmt = null;
    DBConnection dbConn = null;
    int serialNumber = -1;
    
    try {
      /*try {
        //this connection is prone to error for some reason so we 
        //try to connect it three times before quiting.
        conn = util.openDBConnection();
      } catch(SQLException sqle) {
        try {
          conn = util.openDBConnection();
        } catch(SQLException sqlee) {
          try {
            conn = util.openDBConnection();
          } catch(SQLException sqleee) {
            System.out.println("error getting db connection in " + 
                               "MetacatReplication.handleGetDocumentRequest: " +
                               sqleee.getMessage());
          }
        }
      }*/
      dbConn=DBConnectionPool.
                  getDBConnection("DocumentIdentifier.getNewestRev");
      serialNumber=dbConn.getCheckOutSerialNumber();
      pstmt = dbConn.prepareStatement("select rev from xml_documents where docid like ? ");
      pstmt.setString(1, docid);
      pstmt.execute();
      ResultSet rs = pstmt.getResultSet();
      boolean tablehasrows = rs.next();
      if (tablehasrows) {
        String retStr = rs.getString(1);
        pstmt.close();
        //conn.close();
        return retStr;
      }
      //conn.close();
    } catch(SQLException e) {
      System.out.println("error in DocumentIdentifier.getNewestRev(): " +
                          e.getMessage());
      /*try {
       conn.close();
      } catch(SQLException e2) { throw e2; }*/
      throw e;
    }
    finally
    {
      try
      {
        pstmt.close();
      }//try
      finally
      {
        DBConnectionPool.returnDBConnection(dbConn,serialNumber);
      }//finally
    }//finally
        
    
    
    return "1";
  }
  
  private int countSeparator()
  {
    int count = 0;
    for(int i=0; i<docid.length(); i++)
    {
      if(docid.charAt(i) == '.')
      {  
        count++;
      }
    }
    return count;
  }
  
  /**
   * returns the revision number encoded in this docid
   */
  public String getRev()
  {
    return rev;
  }
  
  /**
   * returns the uniqueId encoded in this docid
   */
  public String getUniqueId()
  {
    return uniqueId;
  }
  
  /**
   * returns the siteCode encoded in this docid
   */
  public String getSiteCode()
  {
    return siteCode;
  }
  
  /**
   * returns the separator used in the accession number
   */
  public String getSeparator()
  {
    return separator;
  }
  
  /**
   * returns <siteCode><sepatator><uniqueId>
   */
  public String getIdentifier()
  {
    return siteCode + separator + uniqueId;
  }
  
  /**
   * returns the whole docid as a string
   */
  public String toString()
  {
    return docid;
  }
  
  /**
   * Test driver.  The first command line argument is the docid you want to 
   * create an object for.  For instance ">java DocumentIdentifer nceas.1.2"
   * will return "rev: 2 \n uniqueId: 1 \n siteCode: nceas \n"
   */
  public static void main(String args[]) 
  {
    try
    {
      DocumentIdentifier d = new DocumentIdentifier(args[0]);
      System.out.println("rev: " + d.getRev());
      System.out.println("uniqueId: " + d.getUniqueId());
      System.out.println("siteCode: " + d.getSiteCode());
    }
    catch(Exception e)
    {
      System.out.println("error: " + e.getMessage());
    }
  }
}
