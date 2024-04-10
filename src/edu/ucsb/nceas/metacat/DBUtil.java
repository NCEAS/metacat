package edu.ucsb.nceas.metacat;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A suite of utility classes for querying DB
 */
public class DBUtil {

  //private Connection	conn = null;
  private static Log logMetacat = LogFactory.getLog(DBUtil.class);
  private static final int MAXMUM = -2;
  public static final int NONEEXIST = -1;

  /**
   * main routine used for testing.
   * <p>
   * Usage: java DBUtil <-dt|-dg>
   *
   * @param -dt for selecting all doctypes
   *        -dg for selecting DataGuide
   */
  static public void main(String[] args) {
     
     if (args.length < 1)
     {
        System.err.println("Wrong number of arguments!!!");
        System.err.println("USAGE: java DBUtil <-dt | -ds [doctype] | -dl user>");
        return;
     } else {
        try {
                    
          // Open a connection to the database
          //Connection dbconn = util.openDBConnection();

          DBUtil dbutil = new DBUtil();
          
          if ( args[0].equals("-dt") ) {
            String doctypes = dbutil.readDoctypes();
            System.out.println(doctypes);
          } else if ( args[0].equals("-ds") ) {
            String doctype = null;
            if ( args.length == 2 ) { doctype = args[1]; }
            String dtdschema = dbutil.readDTDSchema(doctype);
            System.out.println(dtdschema);
          } else if ( args[0].equals("-dl") ) {
            String scope = "";
            if ( args.length == 2 ) { scope = args[1]; }
            String docid = dbutil.getMaxDocid(scope);
            System.out.println(docid);
          } else {
            System.err.println(
            "USAGE: java DBUtil <-dt | -ds [doctype] | -dg [doctype]>");
          }  

        } catch (Exception e) {
          //System.err.println("error in DBUtil.main");
          //System.err.println(e.getMessage());
          e.printStackTrace(System.err);
        }
     }
  }
  
  /**
   * Construct an instance of the utility class
   */
  public DBUtil() {
    //this.conn = conn;
  }

  /**
   * read all doctypes from db connection in XML format
   * select all Public Id from xml_catalog table
   */
  public String readDoctypes()
        throws SQLException  {

    Vector<String> doctypeList = new Vector<String>();
    DBConnection dbConn = null;
    int serialNumber = -1;
    PreparedStatement pstmt = null;
    try {

      dbConn=DBConnectionPool.
                  getDBConnection("DBUtil.readDoctypes");
      serialNumber=dbConn.getCheckOutSerialNumber();
      pstmt =
        dbConn.prepareStatement("SELECT public_id FROM xml_catalog " +
                              "WHERE entry_type = 'DTD'");

      pstmt.execute();
      ResultSet rs = pstmt.getResultSet();
      boolean tableHasRows = rs.next();
      while (tableHasRows) {
           doctypeList.addElement(rs.getString(1));
           tableHasRows = rs.next();
      }
      
      pstmt.close();

    } catch (SQLException e) {
      throw new SQLException("DBUtil.readDoctypes - SQL error: " + e.getMessage());
    }
    finally
    {
      try
      {
        pstmt.close();
      }//try
      finally
      {
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }//finally
    }//finally
       

    return formatToXML(doctypeList, "doctype");
  }

  /**
   * read DTD or Schema file from Metacat's XML catalog system
   */
  public String readDTDSchema(String doctype)
        throws SQLException, MalformedURLException, IOException, PropertyNotFoundException
  {
    String systemID = null;
    PreparedStatement pstmt = null;
    StringBuffer cbuff = new StringBuffer();
    DBConnection dbConn = null;
    int serialNumber = -1;
    // get doctype's System ID from db catalog
    try {
    
      dbConn=DBConnectionPool.
                  getDBConnection("DBUtil.readDTDSchema");
      serialNumber=dbConn.getCheckOutSerialNumber();
      pstmt = dbConn.prepareStatement("SELECT system_id " + 
                                    "FROM xml_catalog " +
                                    "WHERE entry_type in ('DTD','Schema') " +
                                    "AND public_id LIKE ?");
      pstmt.setString(1, doctype);
      pstmt.execute();
      ResultSet rs = pstmt.getResultSet();
      boolean hasRow = rs.next();
      if (hasRow) {
        systemID = rs.getString(1);
        // system id may not have server url on front.  Add it if not.
        if (!systemID.startsWith("http://")) {
        	systemID = SystemUtil.getInternalContextURL() + systemID;
        }
      } else {
        throw new SQLException("DBUtil.readDTDSchema - Non-registered doctype: " + doctype);
      }
      pstmt.close();

    } catch (SQLException e) {
      throw new SQLException("DBUtil.readDTDSchema - " + e.getMessage());
    }
    finally
    {
      try
      {
        pstmt.close();
      }//try
      finally
      {
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }//finally
    }//finally

    // read from URL stream as specified by the System ID.
    try {
      // open a connection to this URL and return an InputStream
      // for reading from that connection
      InputStream istream = new URL(systemID).openStream();
      // create a buffering character-input stream
      // that uses a default-sized input buffer
      BufferedInputStream in = new BufferedInputStream(istream);

      // read the input and write into the string buffer
	    int inputByte;
	    while ( (inputByte = in.read()) != -1 ) {
        cbuff.append((char)inputByte);
	    }

      // the input stream must be closed
	    in.close();
	    
    } catch (MalformedURLException e) {
    	throw new MalformedURLException("DBUtil.readDTDSchema - Malformed URL Error: " + e.getMessage());
    } catch (IOException e) {
    	throw new IOException("DBUtil.readDTDSchema - I/O error: " + e.getMessage());
    } catch (SecurityException e) {
    	throw new IOException("DBUtil.readDTDSchema - Security error: " + e.getMessage());
    }
    
   return cbuff.toString();
  }

//  /**
//   * format the DataGuide ResultSet to XML
//   */
//  private String formatToXML(Vector resultset) {
//  
//    String currPath = null;
//    String currElement = null;
//    String prevElement = null;
//    StringBuffer result = new StringBuffer();
//    Enumeration<String> rs = resultset.elements(); 
//    Stack st = new Stack();
//    int i = 0;
//
//    result.append("<?xml version=\"1.0\"?>\n");
//    result.append("<resultset>\n"); 
//    
//    while (rs.hasMoreElements()) {
//        currPath = (String)rs.nextElement();
//        while ( !In(prevElement, currPath) ) {
//            currElement = (String)st.pop();
//            result.append(pad(" ",i--) + "</" + currElement + ">\n");
//            if ( st.empty() ) 
//                prevElement = null;
//            else    
//                prevElement = (String)st.peek();
//        }    
//        currElement = getElementFromPath(currPath);
//        st.push(currElement);
//        result.append(pad(" ",++i) + "<" + currElement + ">\n");
//        prevElement = currElement;
//    }
//    while ( !st.empty() ) {
//        prevElement = (String)st.pop();
//        result.append(pad(" ",i--) + "</" + prevElement + ">\n");
//    }    
//    result.append("</resultset>\n"); 
//
//    return result.toString();
//  }

//  /**
//   * check if element is in path like /elem1/elem2/elemn3
//   */
//  private boolean In(String element, String path) {
//    
//    if ( element == null ) return true;
//    return ( path.indexOf(element) != -1 );
//  }
//
//  /**
//   * get last element from path like /elem1/elem2/elemn3
//   */
//  private String getElementFromPath(String path) {
//    
//    return ( path.substring(path.lastIndexOf("/")+1) );
//  }
//
//  /**
//   * repeates the str n-times
//   */
//  private String pad(String str, int n) {
//    
//    String result = "";
//    for ( int i = 0; i < n; i++ )
//        result = result.concat(str);
//        
//    return result;    
//  }

  /**
   * format the ResultSet to XML
   */
  private String formatToXML(Vector<String> resultset, String tag) {
  
    String val = null;
    StringBuffer result = new StringBuffer();
    Enumeration<String> rs = resultset.elements(); 

    result.append("<?xml version=\"1.0\"?>\n");
    result.append("<resultset>\n"); 
    while (rs.hasMoreElements()) {
        val = rs.nextElement();
        result.append("   <" + tag + ">" + val + "</" + tag + ">\n");
    }
    result.append("</resultset>\n"); 
    
    return result.toString();
  }

  /**
   * get the latest Accession Number from a particular scope
   */
  public String getMaxDocid(String scope)
        throws SQLException  {

    String accnum = null;
    String sep = ".";
    try {
    	PropertyService.getProperty("document.accNumSeparator");
    } catch (PropertyNotFoundException pnfe) {
    	logMetacat.error("DBUtil.getMaxDocid - could not get property " + 
    			"'accNumSeparator'.  setting to '.': " + pnfe.getMessage());  	
    }
    PreparedStatement pstmt = null;
    DBConnection dbConn = null;
    int serialNumber = -1;
    try {
        dbConn=DBConnectionPool.
                  getDBConnection("DBUtil.getMaxDocid");
        serialNumber=dbConn.getCheckOutSerialNumber();
        pstmt =
        dbConn.prepareStatement(
            "SELECT docid, max(rev) FROM " +
            "( " +
                "SELECT docid, rev " + 
                "FROM xml_documents " +
                "WHERE docid LIKE ? " +
            "UNION " + 
                "SELECT docid, rev " + 
                "FROM xml_revisions " +
                "WHERE docid LIKE ?" +
            ") subquery GROUP BY docid"
            );

      pstmt.setString(1,scope + sep + "%");
      pstmt.setString(2,scope + sep + "%");
      pstmt.execute();
      ResultSet rs = pstmt.getResultSet();
      
      long max = 0;
      String temp = null;
      
      while(rs.next()){
    	  temp = rs.getString(1);
    	  if(temp != null){
    		  temp = temp.substring(temp.indexOf(scope) + scope.length() + 1);
    		  try {
    			  long localid = Long.parseLong(temp);
    			  if (localid > max) {
    				  max = localid;
    				  accnum = rs.getString(1) + sep + rs.getString(2);
    			  }
    		  } catch (NumberFormatException nfe){
    			  // ignore the exception as it is possible that the  
    			  // localid in the identifier is not an integer 
    		  }
    	  }
      }
      
      pstmt.close();

    } catch (SQLException e) {
      throw new SQLException("DBUtil.getMaxDocid(). " + e.getMessage());
    }
    finally
    {
      try
      {
        pstmt.close();
      }//try
      finally
      {
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }//finally
    }//finally

    return accnum;
  }
  
  /**
   * return true if the given docid is registered in either the xml_documents
   * or xml_revisions table
   */
  public boolean idExists(String docid)
    throws SQLException
  {
    Vector<String> v = getAllDocids(null);
    for(int i=0; i<v.size(); i++)
    {
      String id = (String)v.elementAt(i);
      if(id.trim().equals(docid.trim()))
      {
        return true;
      }
    }
    return false;
  }
  
  /**
   * return all docids with a given doctype on all servers
   */
  public static Vector<String> getAllDocidsByType(String doctype, boolean includeRevs) throws SQLException {
	  return getAllDocidsByType(doctype, includeRevs, -1);
  }
  
  /**
   * return all docids with a given doctype for a given server
   */
  public static Vector<String> getAllDocidsByType(String doctype, boolean includeRevs, int serverLocation) throws SQLException {
		Vector<String> resultVector = new Vector<String>();
		String sep = ".";
	    try {
	    	PropertyService.getProperty("document.accNumSeparator");
	    } catch (PropertyNotFoundException pnfe) {
	    	logMetacat.error("DBUtil.getAllDocidsByType - could not get property " + 
	    			"'accNumSeparator'.  setting to '.': " + pnfe.getMessage());  	
	    }
		PreparedStatement pstmt = null;
		DBConnection dbConn = null;
		int serialNumber = -1;
		try {
			dbConn = DBConnectionPool.getDBConnection("DBUtil.getAllDocidsByType");
			serialNumber = dbConn.getCheckOutSerialNumber();
			StringBuffer sb = new StringBuffer();

			sb.append("SELECT docid, rev FROM " + "( " + "SELECT docid, rev "
					+ "FROM xml_documents ");
			sb.append("WHERE true ");
			if (doctype != null) {
				sb.append("AND doctype LIKE ? ");
			}
			if (serverLocation > 0) {
				sb.append("AND server_location = ' " + serverLocation + "' ");
			}
			
			if (includeRevs) {
				sb.append("UNION " + "SELECT docid, rev " + "FROM xml_revisions ");
				sb.append("WHERE true ");
				if (doctype != null) {
					sb.append("AND doctype LIKE ?");
				}
				if (serverLocation > 0) {
					sb.append("AND server_location = ' " + serverLocation + "' ");
				}
			}
			sb.append(") subquery GROUP BY docid, rev");
			pstmt = dbConn.prepareStatement(sb.toString());

			if (doctype != null) {
				pstmt.setString(1, doctype);
				if (includeRevs) {
					pstmt.setString(2, doctype);
				}
			}
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();

			String id = null;
			String rev = null;
			while (rs.next()) {
				id = rs.getString(1);
				rev = rs.getString(2);
				if (id != null) {
					resultVector.addElement(id + sep + rev);
				}
			}

			pstmt.close();

		} catch (SQLException e) {
			throw new SQLException("DBUtil.getAllDocidsByType(). " + e.getMessage());
		} finally {
			try {
				pstmt.close();
			}// try
			finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}// finally
		}// finally

		return resultVector;
	}
  
  /**
   * get the latest Accession Number from a particular scope
   */
  public static Vector<String> getAllDocids(String scope)
        throws SQLException  {
    Vector<String> resultVector = new Vector<String>();
//    String accnum = null;
    String sep = ".";
    try {
    	PropertyService.getProperty("document.accNumSeparator");
    } catch (PropertyNotFoundException pnfe) {
    	logMetacat.error("could not get property 'accNumSeparator'.  setting to '.': " 
    			+ pnfe.getMessage());  	
    }
    PreparedStatement pstmt = null;
    DBConnection dbConn = null;
    int serialNumber = -1;
    try 
    {
      dbConn=DBConnectionPool.
                getDBConnection("DBUtil.getAllDocids");
      serialNumber=dbConn.getCheckOutSerialNumber();
      StringBuffer sb = new StringBuffer();
      
      sb.append("SELECT docid, rev FROM " +
                "( " +
                "SELECT docid, rev " + 
                "FROM xml_documents ");
      if(scope != null)
      {
        sb.append("WHERE docid LIKE ? ");
      }
      sb.append("UNION " + 
                "SELECT docid, rev " + 
                "FROM xml_revisions ");
      if(scope != null)
      {
        sb.append("WHERE docid LIKE ?");
      }
      sb.append(") subquery GROUP BY docid, rev");
      pstmt = dbConn.prepareStatement(sb.toString());

      if(scope != null)
      {
        pstmt.setString(1,scope + sep + "%");
        pstmt.setString(2,scope + sep + "%");
      }
      pstmt.execute();
      ResultSet rs = pstmt.getResultSet();
      
//      long max = 0;
      String id = null;
      String rev = null;
      while(rs.next()){
    	  id = rs.getString(1);
        rev = rs.getString(2);
    	  if(id != null){
    		  //temp = temp.substring(id.indexOf(scope) + scope.length() + 1);
          resultVector.addElement(id + sep + rev);
        }
      }
      
      pstmt.close();

    } catch (SQLException e) {
      throw new SQLException("DBUtil.getAllDocids - SQL error:  " + e.getMessage());
    }
    finally
    {
      try
      {
        pstmt.close();
      }//try
      finally
      {
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }//finally
    }//finally

    return resultVector;
  }
  
  /**
   * To a given docid, found a dataset docid which contains the the given docid
   * This will be done by searching xml_relation table
   * If couldn't find, null will be return
   * @param givenDocId, the docid which we want to find
   */
  public static String findDataSetDocIdForGivenDocument(String givenDocId)
  {
    // Prepared statement for sql
    PreparedStatement pStmt = null;
    // Result set
    ResultSet resultSet = null;
    // String to store the data set docid
    String dataSetDocId = null;
    // DBConnection will be checkout
    DBConnection dbConn = null;
    int serialNumber = -1;
    // String to store the sql command
    String sqlCommand = null;
    try
    {
      // Checkout DBConnection from pool
      dbConn=DBConnectionPool.
                  getDBConnection("DBUtil.findDataSetDocIdForGivenDocument");
      serialNumber=dbConn.getCheckOutSerialNumber();
      // SQL command to chose a docid from xm_relation table
      sqlCommand = "select docid from xml_relation where object like ? or " 
                                                    + "subject like ?";
      // Prepared statement
      pStmt = dbConn.prepareStatement(sqlCommand);
      // Bind variable
      pStmt.setString(1, givenDocId);
      pStmt.setString(2, givenDocId);
      // Execute prepared statement
      pStmt.execute();
      // Get result set
      resultSet = pStmt.getResultSet();
      
      // There has record
      if (resultSet.next())
      {
        // Put the docid into dataSetDocid
        dataSetDocId = resultSet.getString(1);
        return dataSetDocId;
      }//if
      else
      {
        // No record in xml_relation table for given doicd, null returned
        return dataSetDocId;
      }//else
    
    }//try
    catch ( SQLException e)
    {
      // Print out exception
      logMetacat.error("DBUtil.findDataSetDocIdForGivenDocument - " +
    		  "SQL error: " + e.getMessage());
      // return null
      return dataSetDocId;
     
    }//catch
    finally
    {
      try
      {
        // Close result set
        resultSet.close();
        // Close preparedStatement
        pStmt.close();
      }//try
      catch ( SQLException e)
      {
        // Print out exception
    	  logMetacat.error("DBUtil.findDataSetDocIdForGivenDocument - " +
    			  "error closing db resources: "  + e.getMessage());
     
      }//catch
      finally
      {
        // Return DBConnection to the pool
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }//finally
    }//finally
        
  }//findDataSetDocIdForGivenDocument
  
  /**
   * Method to get current revision and doctype for a given docid
   * The output will look like "rev;doctype"
   * @param givenDocId, the docid which we want 
   */
  public String getCurrentRevisionAndDocTypeForGivenDocument(String givenDocId)
                                                 throws SQLException
  {
    // DBConection for JDBC
    DBConnection dbConn = null;
    int serialNumber = -1;
    // Prepared Statement
    PreparedStatement pstmt = null;
    // String to store a docid without rev
    String docIdWithoutRevision = null;
    // SQL command
    String sqlCommand = null;
    // Result set
    ResultSet rs = null;
    // String to store the revision
    String revision = null;
    // String to store the doctype
    String docType = null;
    
    // Get docid without rev
    docIdWithoutRevision = DocumentUtil.getDocIdFromString(givenDocId);
    // SQL command is:
    sqlCommand = "select rev, doctype from xml_documents where docid like ?";
    
    try
    {
      // Check out the connection
      dbConn=DBConnectionPool.
         getDBConnection("DBUtil.getCurrentRevisionAndDocTypeForGivenDocument");
      serialNumber=dbConn.getCheckOutSerialNumber();
      
      // Prepare the sql command
      pstmt = dbConn.prepareStatement(sqlCommand);
      // Bin variable
      pstmt.setString(1, docIdWithoutRevision);
      // Execute the prepared statement
      pstmt.execute();
      // Get result set
      rs = pstmt.getResultSet();
      // If there is some record
      if (rs.next())
      {
        revision = rs.getString(1);
        docType = rs.getString(2);
      }//if
      else
      {
        //search xml_revision table
        Vector<Integer> revisionList = getRevListFromRevisionTable(docIdWithoutRevision);
        if(revisionList == null || revisionList.isEmpty())
        {
          // No record, throw a exception
          throw new 
                SQLException("DBUtil.getCurrentRevisionAndDocTypeForGivenDocument - " + 
                    "There is no record for given docid: " + givenDocId);
        }
        else
        {
          int maxRev = getMaxmumNumber(revisionList);
          if(maxRev == MAXMUM)
          {
            throw new 
            SQLException("DBUtil.getCurrentRevisionAndDocTypeForGivenDocument - " + 
                "There is no record for given docid: " + givenDocId);
          }
          revision = (new Integer(maxRev)).toString();
          sqlCommand = "select doctype from xml_revisions where docid like '"+docIdWithoutRevision+"' and rev="+maxRev;
          pstmt = dbConn.prepareStatement(sqlCommand);
          // Execute the prepared statement
          pstmt.execute();
          // Get result set
          rs = pstmt.getResultSet();
          // If there is some record
          if (rs.next())
          {
            docType = rs.getString(1);
          }//if
        }
       
      }//else
        
    }
    finally
    {
      try
      {
        // Close result set
        rs.close();
        // Close preparedStatement
        pstmt.close();
      }//try
      catch ( SQLException e)
      {
        // Print out exception
    	  logMetacat.error("DBUtil.getCurrentRevisionAndDocTypeForGivenDocument - " + 
    			  "Error closing db resources: " + e.getMessage());
     
      }//catch
      finally
      {
        // Return DBConnection to the pool
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }//finally
    }
    return revision+";"+docType;
  }//getCurrentRevisionAndDocTypeForGivenDocument
  
  /*
   * Gets the maxium number in a given vector.
   */
  private static int getMaxmumNumber(Vector<Integer>list)
  {
    Integer max = null;
    if(list != null)
    {
      for(int i=0; i<list.size(); i++)
      {
        if(i ==0)
        {
          max = list.elementAt(i);
        }
        else
        {
          if(max == null)
          {
            max = list.elementAt(i);
          }
          else
          {
            Integer current = list.elementAt(i);
            if(current != null && current.intValue() > max.intValue())
            {
              max = current;
            }
          }
        }
      }
    }
    if(max != null)
    {
      return max.intValue();
    }
    else
    {
      return MAXMUM;
    }
  }
 
  /**
   * Method to return max rev number in xml_revision for given docid.
   * @param docId
   * @return integer that holds max revision number
   * @throws SQLException
   */
  public static int getMaxRevFromRevisionTable(String docIdWithoutRev) throws SQLException
  {
	  int rev = NONEEXIST;
	  
	  Vector<Integer> revList = getRevListFromRevisionTable(docIdWithoutRev);
	  
	  for (Integer currentRev : revList) {
		  if (currentRev > rev) {
			  rev = currentRev;
		  }
	  }
	  
	  return rev;
  }
  
  /**
   * Method to return a rev list in xml_revision for given docid.
   * @param docId
   * @return is a vector which contains Integer object
   * @throws SQLException
   */
  public static Vector<Integer> getRevListFromRevisionTable(String docIdWithoutRev) throws SQLException
  {
      Vector<Integer> list = new Vector<Integer>();
      int rev = 1;
      PreparedStatement pStmt = null;
      DBConnection dbConn = null;
      int serialNumber = -1;
      // get rid of rev
      //docId = MetacatUtil.getDocIdFromString(docId);
      try {
          //check out DBConnection
          dbConn = DBConnectionPool
                  .getDBConnection("getRevListFromRevisionTable");
          serialNumber = dbConn.getCheckOutSerialNumber();

          pStmt = dbConn
                  .prepareStatement("SELECT rev FROM xml_revisions WHERE docid = ? ORDER BY rev ASC");
          pStmt.setString(1, docIdWithoutRev);
          pStmt.execute();

          ResultSet rs = pStmt.getResultSet();
          boolean hasRow = rs.next();
          while (hasRow) {
              rev = rs.getInt(1);
              logMetacat.info("DBUtil.getRevListFromRevisionTable - rev: " + rev + 
            		  " is added to list for docid: " + docIdWithoutRev);
              list.add(new Integer(rev));
              hasRow = rs.next();
              
          }
          pStmt.close();
      }//try
      finally {
          try {
              pStmt.close();
          } catch (Exception ee) {
        	  logMetacat.error("DBUtil.getRevListFromRevisionTable - Error closing " + 
        			  "prepared statement: " + ee.getMessage());
          } finally {
              DBConnectionPool.returnDBConnection(dbConn, serialNumber);
          }
      }//finally

      return list;
  }//getLatestRevisionNumber
  
  /**
   * Get last revision number from database for a docid If couldn't find an
   * entry, -1 will return The return value is integer because we want compare
   * it to there new one
   *
   * @param docid
   *            <sitecode>. <uniqueid>part of Accession Number
   */
  public static int getLatestRevisionInDocumentTable(String docIdWithoutRev) throws SQLException
  {
      int rev = 1;
      PreparedStatement pStmt = null;
      DBConnection dbConn = null;
      int serialNumber = -1;
      try {
          //check out DBConnection
          dbConn = DBConnectionPool
                  .getDBConnection("DBUtil.getLatestRevisionInDocumentTable");
          serialNumber = dbConn.getCheckOutSerialNumber();

          pStmt = dbConn
                  .prepareStatement("SELECT rev FROM xml_documents WHERE docid = ?");
          pStmt.setString(1, docIdWithoutRev);
          pStmt.execute();

          ResultSet rs = pStmt.getResultSet();
          boolean hasRow = rs.next();
          if (hasRow) {
              rev = rs.getInt(1);
              pStmt.close();
          } else {
              rev = NONEEXIST;
              pStmt.close();
          }
      }//try
      finally {
          try {
              pStmt.close();
          } catch (Exception ee) {
        	  logMetacat.error("DBUtil.getLatestRevisionInDocumentTable - Error closing " + 
        			  " prepared statement: " + ee.getMessage());
          } finally {
              DBConnectionPool.returnDBConnection(dbConn, serialNumber);
          }
      }//finally

      return rev;
  }
  
  
}
