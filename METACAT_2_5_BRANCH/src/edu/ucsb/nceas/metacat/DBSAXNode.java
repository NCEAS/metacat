/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents an XML node and its contents
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;

/**
 * A Class that represents an XML node and its contents and
 * can write its own representation to a database connection
 */
public class DBSAXNode extends BasicNode {

  private DBConnection	connection;
  private DBSAXNode	parentNode;
  private Logger logMetacat = Logger.getLogger(DBSAXNode.class);

  /**
   * Construct a new node instance for DOCUMENT nodes
   *
   * @param conn the JDBC Connection to which all information is written
   */
  public DBSAXNode (DBConnection conn, String docid) throws SAXException {

    super();
    this.connection = conn;
    this.parentNode = null;
    writeChildNodeToDB("DOCUMENT", null, null, docid);
    updateRootNodeID(getNodeID());
  }

  /**
   * Construct a new node instance for ELEMENT nodes
   *
   * @param conn the JDBC Connection to which all information is written
   * @param tagname the name of the node
   * @param parentNode the parent node for this node being created
   */
  public DBSAXNode (DBConnection conn, String qName, String lName,
                    DBSAXNode parentNode, long rootnodeid,
                    String docid, String doctype)
                                               throws SAXException {

    super(lName);
    this.connection = conn;
    this.parentNode = parentNode;
    setParentID(parentNode.getNodeID());
    setRootNodeID(rootnodeid);
    setDocID(docid);
    setNodeIndex(parentNode.incChildNum());
    writeChildNodeToDB("ELEMENT", qName, null, docid);
    //No writing XML Index from here. New Thread used instead.
    //updateNodeIndex(docid, doctype);
  }

  /**
   * Construct a new node instance for DTD nodes
   * This Node will write docname, publicId and systemId into db. Only
   * handle systemid  existed.(external dtd)
   *
   * @param conn the JDBC Connection to which all information is written
   * @param tagname the name of the node
   * @param parentNode the parent node for this node being created
   */
  public DBSAXNode (DBConnection conn, String docName, String publicId,
                    String systemId, DBSAXNode parentNode, long rootnodeid,
                    String docid) throws SAXException
  {

    super();
    this.connection = conn;
    this.parentNode = parentNode;
    setParentID(parentNode.getNodeID());
    setRootNodeID(rootnodeid);
    setDocID(docid);
    // insert dtd node only for external dtd definiation
    if (systemId != null)
    {
      //write docname to DB
      if (docName != null)
      {
        setNodeIndex(parentNode.incChildNum());
        writeDTDNodeToDB(DocumentImpl.DOCNAME, docName, docid);
      }
      //write publicId to DB
      if (publicId != null)
      {
        setNodeIndex(parentNode.incChildNum());
        writeDTDNodeToDB(DocumentImpl.PUBLICID, publicId, docid);
      }
      //write systemId to DB
      setNodeIndex(parentNode.incChildNum());
      writeDTDNodeToDB(DocumentImpl.SYSTEMID, systemId, docid);
    }
  }
  
  /** creates SQL code and inserts new node into DB connection */
  public long writeChildNodeToDB(String nodetype, String nodename,
                                 String data, String docid)
                                 throws SAXException {
    String limitedData = null;
    int leftover = 0;
    if (data != null) {
    	leftover = data.length();
    }
    int offset = 0;
    boolean moredata = true;
    long endNodeId = -1;

    // This loop deals with the case where there are more characters
    // than can fit in a single database text field (limit is
    // MAXDATACHARS). If the text to be inserted exceeds MAXDATACHARS,
    // write a series of nodes that are MAXDATACHARS long, and then the
    // final node contains the remainder
    while (moredata) {
        if (leftover > (DBSAXHandler.MAXDATACHARS)) {
        	limitedData = data.substring(offset, DBSAXHandler.MAXDATACHARS);
            leftover -= (DBSAXHandler.MAXDATACHARS - 1);
            offset += (DBSAXHandler.MAXDATACHARS - 1);
        } else {
        	if (data != null) {
        		limitedData = data.substring(offset, offset + leftover);
        	} else {
        		limitedData = null;
        	}
        	moredata = false;
        }
        
        endNodeId =  writeChildNodeToDBDataLimited(nodetype, nodename, limitedData, docid);
    }
    
    return endNodeId;
  }

  /** creates SQL code and inserts new node into DB connection */
  public long writeChildNodeToDBDataLimited(String nodetype, String nodename,
                                 String data, String docid)
                                 throws SAXException
  {
    long nid = -1;
    try
    {

      PreparedStatement pstmt;
      
      if (nodetype == "DOCUMENT") {
        pstmt = connection.prepareStatement(
            "INSERT INTO xml_nodes " +
            "(nodetype, nodename, nodeprefix, docid) " +
            "VALUES (?, ?, ?, ?)");

        logMetacat.debug("DBSAXNode.writeChildNodeToDBDataLimited - inserting doc name: " + nodename);
      } else {
          pstmt = connection.prepareStatement(
              "INSERT INTO xml_nodes " +
              "(nodetype, nodename, nodeprefix, docid, " +
              "rootnodeid, parentnodeid, nodedata, nodeindex) " +
              "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
      }
      
      // Increase DBConnection usage count
      connection.increaseUsageCount(1);
      
      // Bind the values to the query
      pstmt.setString(1, nodetype);
      int idx;
      if ( nodename != null && (idx = nodename.indexOf(":")) != -1 ) {
        pstmt.setString(2, nodename.substring(idx+1));
        pstmt.setString(3, nodename.substring(0,idx));
      } else {
        pstmt.setString(2, nodename);
        pstmt.setString(3, null);
      }
      pstmt.setString(4, docid);
      if (nodetype != "DOCUMENT") {
        if (nodetype == "ELEMENT") {
          pstmt.setLong(5, getRootNodeID());
          pstmt.setLong(6, getParentID());
          pstmt.setString(7, data);
          pstmt.setInt(8, getNodeIndex());
        } else {
          pstmt.setLong(5, getRootNodeID());
          pstmt.setLong(6, getNodeID());
          pstmt.setString(7, data);
          pstmt.setInt(8, incChildNum());
        }
      }
      // Do the insertion
      logMetacat.debug("DBSAXNode.writeChildNodeToDBDataLimited - SQL insert: " + pstmt.toString());
      pstmt.execute();
      pstmt.close();

      // get the generated unique id afterward
      nid = DatabaseService.getInstance().getDBAdapter().getUniqueID(connection.getConnections(), "xml_nodes");
      //should increase connection usage!!!!!!

      if (nodetype.equals("DOCUMENT")) {
        // Record the root node id that was generated from the database
        setRootNodeID(nid);
      }

      if (nodetype.equals("DOCUMENT") || nodetype.equals("ELEMENT")) {

        // Record the node id that was generated from the database
        setNodeID(nid);

        // Record the node type that was passed to the method
        setNodeType(nodetype);

      }

    } catch (SQLException sqle) {
      logMetacat.error("DBSAXNode.writeChildNodeToDBDataLimited - SQL error inserting node: " + 
    		  "(" + nodetype + ", " + nodename + ", " + data + ") : " + sqle.getMessage());
      sqle.printStackTrace(System.err);
      throw new SAXException(sqle.getMessage());
    }
    return nid;
  }

  /**
   * update rootnodeid=nodeid for 'DOCUMENT' type of nodes only
   */
  public void updateRootNodeID(long nodeid) throws SAXException {
      try {
        PreparedStatement pstmt;
        pstmt = connection.prepareStatement(
              "UPDATE xml_nodes set rootnodeid = ? " +
              "WHERE nodeid = ?");
        // Increase DBConnection usage count
        connection.increaseUsageCount(1);

        // Bind the values to the query
        pstmt.setLong(1, nodeid);
        pstmt.setLong(2, nodeid);
        // Do the update
        pstmt.execute();
        pstmt.close();
      } catch (SQLException e) {
        System.out.println("Error in DBSaxNode.updateRootNodeID: " +
                           e.getMessage());
        throw new SAXException(e.getMessage());
      }
  }

  /**
   * creates SQL code to put nodename for the document node
   * into DB connection
   */
  public void writeNodename(String nodename) throws SAXException {
      try {
        PreparedStatement pstmt;
        pstmt = connection.prepareStatement(
              "UPDATE xml_nodes set nodename = ? " +
              "WHERE nodeid = ?");
        // Increase DBConnection usage count
        connection.increaseUsageCount(1);

        // Bind the values to the query
        pstmt.setString(1, nodename);
        pstmt.setLong(2, getNodeID());
        // Do the insertion
        pstmt.execute();
        pstmt.close();
      } catch (SQLException e) {
        System.out.println("Error in DBSaxNode.writeNodeName: " +
                           e.getMessage());
        throw new SAXException(e.getMessage());
      }
  }

 /** creates SQL code and inserts new node into DB connection */
  public long writeDTDNodeToDB(String nodename, String data, String docid)
                                 throws SAXException
  {
    long nid = -1;
    try
    {

      PreparedStatement pstmt;
      logMetacat.info("DBSAXNode.writeDTDNodeToDB - Insert dtd into db: "+nodename +" "+data);
  
      pstmt = connection.prepareStatement(
              "INSERT INTO xml_nodes " +
              "(nodetype, nodename, docid, " +
              "rootnodeid, parentnodeid, nodedata, nodeindex) " +
              "VALUES (?, ?, ?, ?, ?, ?, ?)");

       // Increase DBConnection usage count
      connection.increaseUsageCount(1);

      // Bind the values to the query
      pstmt.setString(1, DocumentImpl.DTD);
      pstmt.setString(2, nodename);
      pstmt.setString(3, docid);
      pstmt.setLong(4, getRootNodeID());
      pstmt.setLong(5, getParentID());
      pstmt.setString(6, data);
      pstmt.setInt(7, getNodeIndex());

      // Do the insertion
      pstmt.execute();
      pstmt.close();

      // get the generated unique id afterward
      nid = DatabaseService.getInstance().getDBAdapter().getUniqueID(connection.getConnections(), "xml_nodes");

    } catch (SQLException e) {
      System.out.println("Error in DBSaxNode.writeDTDNodeToDB");
      System.err.println("Error inserting node: (" + DocumentImpl.DTD + ", " +
                                                     nodename + ", " +
                                                     data + ")" );
      System.err.println(e.getMessage());
      e.printStackTrace(System.err);
      throw new SAXException(e.getMessage());
    }
    return nid;
  }


  /** get next node id from DB connection */
  private long generateNodeID() throws SAXException {
      long nid=0;
      PreparedStatement pstmt;
      DBConnection dbConn = null;
      int serialNumber = -1;
      try {
        // Get DBConnection
        dbConn=DBConnectionPool.getDBConnection("DBSAXNode.generateNodeID");
        serialNumber=dbConn.getCheckOutSerialNumber();
        String sql = "SELECT xml_nodes_id_seq.nextval FROM dual";
        pstmt = dbConn.prepareStatement(sql);
        pstmt.execute();
        ResultSet rs = pstmt.getResultSet();
        boolean tableHasRows = rs.next();
        if (tableHasRows) {
          nid = rs.getLong(1);
        }
        pstmt.close();
      } catch (SQLException e) {
        System.out.println("Error in DBSaxNode.generateNodeID: " +
                            e.getMessage());
        throw new SAXException(e.getMessage());
      }
      finally
      {
        // Return DBconnection
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }//finally

      return nid;
  }

  /** Add a new attribute to this node, or set its value */
  public long setAttribute(String attName, String attValue, String docid)
              throws SAXException
  {
    long nodeId = -1;
    if (attName != null)
    {
      // Enter the attribute in the hash table
      super.setAttribute(attName, attValue);

      // And enter the attribute in the database
      nodeId = writeChildNodeToDB("ATTRIBUTE", attName, attValue, docid);
    }
    else
    {
      System.err.println("Attribute name must not be null!");
      throw new SAXException("Attribute name must not be null!");
    }
    return nodeId;
  }

  /** Add a namespace to this node */
  public long setNamespace(String prefix, String uri, String docid)
              throws SAXException
  {
    long nodeId = -1;
    if (prefix != null)
    {
      // Enter the namespace in a hash table
      super.setNamespace(prefix, uri);
      // And enter the namespace in the database
      nodeId = writeChildNodeToDB("NAMESPACE", prefix, uri, docid);
    }
    else
    {
      System.err.println("Namespace prefix must not be null!");
      throw new SAXException("Namespace prefix must not be null!");
    }
    return nodeId;
  }



  /**
   * USED FROM SEPARATE THREAD RUNNED from DBSAXHandler on endDocument()
   * Update the node index (xml_index) for this node by generating
   * test strings that represent all of the relative and absolute
   * paths through the XML tree from document root to this node
   */
  public void updateNodeIndex(DBConnection conn, String docid, String doctype)
               throws SAXException
  {
    Hashtable pathlist = new Hashtable();
    boolean atStartingNode = true;
    boolean atRootDocumentNode = false;
    DBSAXNode nodePointer = this;
    StringBuffer currentPath = new StringBuffer();
    int counter = 0;

    // Create a Hashtable of all of the paths to reach this node
    // including absolute paths and relative paths
    while (!atRootDocumentNode) {
      if (atStartingNode) {
        currentPath.insert(0, nodePointer.getTagName());
        pathlist.put(currentPath.toString(), new Long(getNodeID()));
        counter++;
        atStartingNode = false;
      } else {
        currentPath.insert(0, "/");
        currentPath.insert(0, nodePointer.getTagName());
        pathlist.put(currentPath.toString(), new Long(getNodeID()));
        counter++;
      }

      // advance to the next parent node
      nodePointer = nodePointer.getParentNode();

      // If we're at the DOCUMENT node (root of DOM tree), add
      // the root "/" to make the absolute path
      if (nodePointer.getNodeType().equals("DOCUMENT")) {
        currentPath.insert(0, "/");
        pathlist.put(currentPath.toString(), new Long(getNodeID()));
        counter++;
        atRootDocumentNode = true;
      }
    }

    try {
      // Create an insert statement to reuse for all of the path insertions
      PreparedStatement pstmt = conn.prepareStatement(
              "INSERT INTO xml_index (nodeid, path, docid, doctype, " +
               "parentnodeid) " +
              "VALUES (?, ?, ?, ?, ?)");
      // Increase usage count
      conn.increaseUsageCount(1);

      pstmt.setString(3, docid);
      pstmt.setString(4, doctype);
      pstmt.setLong(5, getParentID());

      // Step through the hashtable and insert each of the path values
      Enumeration en = pathlist.keys();
      while (en.hasMoreElements()) {
        String path = (String)en.nextElement();
        Long nodeid = (Long)pathlist.get(path);
        pstmt.setLong(1, nodeid.longValue());
        pstmt.setString(2, path);

        pstmt.executeUpdate();
      }
      // Close the database statement
      pstmt.close();
    } catch (SQLException sqe) {
      System.err.println("SQL Exception while inserting path to index in " +
                         "DBSAXNode.updateNodeIndex for document " + docid);
      System.err.println(sqe.getMessage());
      throw new SAXException(sqe.getMessage());
    }
  }

  /** get the parent of this node */
  public DBSAXNode getParentNode() {
    return parentNode;
  }
}
