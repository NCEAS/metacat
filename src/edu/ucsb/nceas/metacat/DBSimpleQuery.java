/**
 *  '$RCSfile$'
 *    Purpose: A Class that searches a relational DB for elements and 
 *             attributes that have free text matches to the query string.  
 *             It returns a result set consisting of the root nodeid for 
 *             each document that satisfies the query
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

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Enumeration;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;

/** 
 * A Class that searches a relational DB for elements and attributes that
 * have free text matches to the query string.  It returns a result set 
 * consisting of the root nodeid for each document that satisfies the query
 */
public class DBSimpleQuery {

  //private Connection	conn = null;

  /**
   * the main routine used to test the DBSimpleQuery utility.
   * <p>
   * Usage: java DBSimpleQuery <query>
   *
   * @param query the text to search for in the element and attribute content
   */
  static public void main(String[] args) {
     
     if (args.length < 2)
     {
        System.err.println("Wrong number of arguments!!!");
        System.err.println("USAGE: java DBSimpleQuery <query> <doctype>");
        return;
     } else {
        try {
                    
          String query    = args[0];
          String doctype  = args[1];

          // Open a connection to the database
          //Connection dbconn = util.openDBConnection();

          // Execute the simple query
          DBSimpleQuery rd = new DBSimpleQuery();
          Hashtable nodelist = null;
          if (doctype.equals("any") || doctype.equals("ANY")) {
            nodelist = rd.findDocuments(query);
          } else {
            nodelist = rd.findDocuments(query, doctype);
          }

          // Print the reulting root nodes
          StringBuffer result = new StringBuffer();
          String document = null;
          String docid = null;
          result.append("<?xml version=\"1.0\"?>\n");
          result.append("<resultset>\n");
  // following line removed by Dan Higgins to avoid insertion of query XML inside returned XML doc
  //        result.append("  <query>" + query + "</query>\n");
          Enumeration doclist = nodelist.keys(); 
          while (doclist.hasMoreElements()) {
            docid = (String)doclist.nextElement();
            document = (String)nodelist.get(docid);
            result.append("  <document>\n    " + document + 
                          "\n  </document>\n");
          }
          result.append("</resultset>\n");

          System.out.println(result);

        } catch (Exception e) {
          System.err.println("Error in DBSimpleQuery.main");
          System.err.println(e.getMessage());
          e.printStackTrace(System.err);
        }
     }
  }
  
  /**
   * construct an instance of the DBSimpleQuery class 
   *
   * <p>Generally, one would call the findDocuments() routine after creating 
   * an instance to specify the search query</p>
   *
   * @param conn the JDBC connection that we use for the query
   */
  public DBSimpleQuery() 
                  throws IOException, 
                         SQLException, 
                         ClassNotFoundException
  {
    //this.conn = conn;
  }
  
  /** 
   * routine to search the elements and attributes looking to match query
   *
   * @param query the text to search for
   */
  public Hashtable findDocuments(String query) {
    return this.findDocuments(query, null);
  }

  /** 
   * routine to search the elements and attributes looking to match query
   *
   * @param query the text to search for
   * @param requestedDoctype the type of documents to return from the query
   */
  public Hashtable findDocuments(String query, String requestedDoctype) {
      Hashtable	 docListResult = new Hashtable();

      PreparedStatement pstmt = null;
      DBConnection dbConn = null;
      int serialNumber = -1;

      // Now look up the document id
      String docid = null;
      String docname = null;
      String doctype = null;
      String doctitle = null;
      //StringBuffer document = null; 

      try {
        dbConn=DBConnectionPool.
                  getDBConnection("DBSimpleQuery.findDocuments");
        serialNumber=dbConn.getCheckOutSerialNumber();
        if (requestedDoctype == null || 
            requestedDoctype.equals("any") || 
            requestedDoctype.equals("ANY")) {
          pstmt = dbConn.prepareStatement(
                "SELECT docid,docname,doctype,doctitle " +
                "FROM xml_documents " +
                "WHERE docid IN " +
                "(SELECT docid " +
                "FROM xml_nodes WHERE nodedata LIKE ? )");

                // Bind the values to the query
                pstmt.setString(1, query);
        } else {
          pstmt = dbConn.prepareStatement(
                "SELECT docid,docname,doctype,doctitle " +
                "FROM xml_documents " +
                "WHERE docid IN " +
                "(SELECT docid " +
                "FROM xml_nodes WHERE nodedata LIKE ? ) " +
                "AND doctype = ?");

                // Bind the values to the query
                pstmt.setString(1, query);
                pstmt.setString(2, requestedDoctype);
        }

        pstmt.execute();
        ResultSet rs = pstmt.getResultSet();
        boolean tableHasRows = rs.next();
        while (tableHasRows) {
          docid = rs.getString(1);
          docname = rs.getString(2);
          doctype = rs.getString(3);
          doctitle = rs.getString(4);

          StringBuffer document = new StringBuffer();
          document.append("<docid>").append(docid).append("</docid>");

          if (docname != null) {
            document.append("<docname>" + docname + "</docname>");
          }
          if (doctype != null) {
            document.append("<doctype>" + doctype + "</doctype>");
          }
          if (doctitle != null) {
            document.append("<doctitle>" + doctitle + "</doctitle>");
          }

          // Store the document id and the root node id
          docListResult.put(docid,(String)document.toString());

          // Advance to the next record in the cursor
          tableHasRows = rs.next();
        }
        pstmt.close();
      } catch (SQLException e) {
        System.out.println("Error in DBSimpleQuery.findDocuments: " + 
                            e.getMessage());
      }
      finally
      {
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }

    return docListResult;
  }
}
