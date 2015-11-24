/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents an XML Text node and its contents,
 *             and can build itself from a database connection
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

import java.util.Comparator;
import java.util.Stack;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.util.MetacatUtil;

/**
 * A Class that represents an XML Subtree
 */
public class SubTree implements Comparator
{
  protected String docId = null;
  protected String subTreeId = null;
  protected String startElementName = null;
  protected long   startNodeId = -1;
  protected long   endNodeId =   -1;
  private Stack<NodeRecord>  subTreeNodeStack = null;
  
  private static Logger logMetacat = Logger.getLogger(SubTree.class);

    /**
     * Defualt constructor
     */
    public SubTree()
    {

    }

    /**
     * Constructor of subtree
     */
    public SubTree(String myDocId, String mySubTreeId,
                  long myStartNodeId, long myEndNodeId)
    {
      this.docId = myDocId;
      logMetacat.info("Docid of Subtree: " + docId);
      this.subTreeId = mySubTreeId;
      logMetacat.info("id of Subtree: " + subTreeId);
      this.startNodeId = myStartNodeId;
      logMetacat.info("start node id of Subtree: " + startNodeId);
      this.endNodeId = myEndNodeId;
      logMetacat.info("end node id of subtree: " + endNodeId);

    }

    /**
     * Get subtree node stack
     */
    public Stack<NodeRecord> getSubTreeNodeStack() throws SAXException
    {
      try
      {
        subTreeNodeStack = getSubTreeNodeListFromDB();
      }
      catch (McdbException e)
      {
        throw new SAXException(e.getMessage());
      }
      return this.subTreeNodeStack;
    }

    /**
     * Set subtree node stack
     */
    public void setSubTreeNodeStack(Stack myStack)
    {
      this.subTreeNodeStack = myStack;
    }

    /** Set the a docId */
    public void setDocId(String myId)
    {
      logMetacat.info("set doc id: "+myId);
      this.docId = myId;
    }

    /** Get the docId */
    public String getDocId()
    {
      return this.docId;
    }


    /** Set the a subtreeId */
    public void setSubTreeId(String myId)
    {
      logMetacat.info("set sub tree id: "+myId);
      this.subTreeId = myId;
    }

    /** Get the subTreeId */
    public String getSubTreeId()
    {
      return this.subTreeId;
    }

    /**
     * Set a startElementName
     */
    public void setStartElementName(String elementName)
    {
      logMetacat.info("set start elementname: "+elementName);
      this.startElementName = elementName;
    }

    /**
     * Get startElementName
     */
    public String getStartElementName()
    {
      return this.startElementName;
    }

    /** Set a start node id */
    public void setStartNodeId(long nodeId)
    {
      logMetacat.info("set start node id: "+nodeId);
      this.startNodeId = nodeId;
    }

    /** Get start node id */
    public long getStartNodeId()
    {
      return this.startNodeId;
    }

    /** Set a end node id */
    public void setEndNodeId(long nodeId)
    {
      logMetacat.info("set end node id: "+nodeId);
      this.endNodeId = nodeId;
    }

    /** Get end node id */
    public long getEndNodeId()
    {
      return this.endNodeId;
    }

    /* Put a subtree node into a stack, on top is the start point of subtree*/
    private Stack getSubTreeNodeListFromDB() throws McdbException
    {
       Stack nodeRecordList = new Stack();
       // make sure it works
       if ( docId == null || startNodeId == -1 || endNodeId == -1)
       {
         return nodeRecordList;
       }
       PreparedStatement pstmt = null;
       DBConnection dbconn = null;
       int serialNumber = -1;

       long nodeid = 0;
       long parentnodeid = 0;
       long nodeindex = 0;
       String nodetype = null;
       String nodename = null;
       String nodeprefix = null;
       String nodedata = null;
       String sql = "SELECT nodeid, parentnodeid, nodeindex, " +
                    "nodetype, nodename, nodeprefix, nodedata " +
                    "FROM xml_nodes WHERE docid = ? AND nodeid >= ? AND " +
                    "nodeid <= ? ORDER BY nodeid DESC";
       try
       {
         dbconn=DBConnectionPool.
                    getDBConnection("SubTree.getSubTreeNodeList");
         serialNumber=dbconn.getCheckOutSerialNumber();
         pstmt = dbconn.prepareStatement(sql);

         // Bind the values to the query
         pstmt.setString(1, docId);
         pstmt.setLong(2, startNodeId);
         pstmt.setLong(3, endNodeId);

         pstmt.execute();
         ResultSet rs = pstmt.getResultSet();
         boolean tableHasRows = rs.next();

         while (tableHasRows)
         {
           nodeid = rs.getLong(1);
           parentnodeid = rs.getLong(2);
           nodeindex = rs.getLong(3);
           nodetype = rs.getString(4);
           nodename = rs.getString(5);
           nodeprefix = rs.getString(6);
           nodedata = rs.getString(7);
           nodedata = MetacatUtil.normalize(nodedata);
           // add the data to the node record list hashtable
           NodeRecord currentRecord = new NodeRecord(nodeid,parentnodeid,nodeindex,
                                      nodetype, nodename, nodeprefix, nodedata);
           nodeRecordList.push(currentRecord);

           // Advance to the next node
           tableHasRows = rs.next();
         }//while
         pstmt.close();

      } //try
      catch (SQLException e)
      {
        throw new McdbException("Error in SubTree.getSubTreeNodeList 1 " +
                              e.getMessage());
      }//catch
      finally
      {
        try
        {
          pstmt.close();
        }
        catch (SQLException ee)
        {
          logMetacat.error("error in SubTree.getSubTreeNodeList 2: "
                                    +ee.getMessage());
        }
        finally
        {
          DBConnectionPool.returnDBConnection(dbconn, serialNumber);
        }
      }//finally

      return nodeRecordList;

    }//getSubtreeNodeList

   /** methods from Comparator interface */
   public int compare(Object o1, Object o2)
   {
     SubTree tree1 = (SubTree) o1;
     SubTree tree2 = (SubTree) o2;
     if (tree1.getStartNodeId() > tree2.getStartNodeId())
     {
       return 1;
     }
     else if (tree1.getStartNodeId() < tree2.getStartNodeId())
     {
       return -1;
     }
     else
     {
       return 0;
     }

   }//cpmpare

   /** method from Comparator interface */
   public boolean equals(Object obj)
   {
     SubTree tree = (SubTree)obj;
     if (startNodeId == tree.getStartNodeId())
     {
       return true;
     }
     else
     {
       return false;
     }
   }

}
