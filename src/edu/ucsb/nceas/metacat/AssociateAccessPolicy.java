package edu.ucsb.nceas.metacat;


import java.io.*;
import java.util.Vector;
import java.net.URL;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.BufferedWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;


/**This class is in order to fix a problem. It doesn't has functionality for 
 *Metacat. 
 *In Currently, some document in xml_document table doesn't have entries in 
 *xml_access table. This is okay during the old access policy.
 *But we changed the access policy and if there is no entry in xml_access table,
 * except owner, other person can not access it. So we need to associate with 
 *access policy in xml_access table for these doc ids. The same access policy 
 *of these docoments' data set will associate to them. 
 */
public class AssociateAccessPolicy {

 
 
  private DBConnection  conn = null;
  private Vector docIdInAccessTable=null;
  private Vector docIdWithoutAccessEntry=null;
  private Vector notFoundDataSetId=null;
  private Vector itsDataSetIdWithouAccessEntry=null;
  private Hashtable docIdMapDataSetId=null; 
  private Log logMetacat = LogFactory.getLog(AssociateAccessPolicy.class);
  
  /**
   * the main routine used to associate access policy
   */
  static public void main(String[] args) 
  {
     DBConnection localConn=null;
     int serialNumber = -1;
     AssociateAccessPolicy policy=null;
     try
     {
      localConn=DBConnectionPool.getDBConnection("AssociateAccessPolict.main");
      serialNumber=localConn.getCheckOutSerialNumber();
      policy = new AssociateAccessPolicy(localConn);
      policy.associateAccess();
      //localConn.close();   
     }//try
     catch (Exception e) 
     {
        System.err.println("Error in AssociateAccessPolicy.main");
        System.err.println(e.getMessage());
        e.printStackTrace(System.err);
     }
     finally
     {
       DBConnectionPool.returnDBConnection(localConn, serialNumber);
     }//finally
     
     if (!(policy.getNotFoundDataSetId()).isEmpty())
     {
         
         System.out.println("Following docid which could not find a mapped" 
                  +" dataset was associated with defualt policy:");
         for (int i=0; i<(policy.getNotFoundDataSetId()).size(); i++)
        {
           String str=(String)(policy.getNotFoundDataSetId()).elementAt(i);
           System.out.println(str);
        }//for
     }//if
     if (!(policy.geItsDataSetIdWithouAccessEntry()).isEmpty())
     {
         
         System.out.println("Following docid which's mapped dataset doesn't has"
                  +" an access entry was associated with defualt policy:");
         for (int i=0; i<(policy.geItsDataSetIdWithouAccessEntry()).size(); i++)
        {
           String str=(String)
                        (policy.geItsDataSetIdWithouAccessEntry()).elementAt(i);
           System.out.println(str);
        }//for
     }//if
  }
  
  /**
   * construct an instance of the DBQuery class 
   *
   * <p>Generally, one would call the findDocuments() routine after creating 
   * an instance to specify the search query</p>
   *
   * @param conn the JDBC connection that we use for the query
   * @param parserName the fully qualified name of a Java class implementing
   *                   the org.xml.sax.XMLReader interface
   */
  public AssociateAccessPolicy( DBConnection conn) 
                  throws IOException, 
                         SQLException, Exception
  {   
    this.conn = conn;
    this.docIdInAccessTable=new Vector();
    this.docIdWithoutAccessEntry=new Vector();
    this.notFoundDataSetId=new Vector();
    this.itsDataSetIdWithouAccessEntry=new Vector();
    this.docIdMapDataSetId=new Hashtable();
    getDocIdInAccessTable();
    getDocIdWithoutAccessEntry();
    getDocIdMapDataSetId();
   }
  
  /**
   * Get the docid which didn't found a dataset id to map it
   */
  public Vector getNotFoundDataSetId()
  {
    return notFoundDataSetId;
  }
  /**
   * Get the docid which it's mapped dataset doesn't has access entry
   */
  public Vector geItsDataSetIdWithouAccessEntry()
  {
    return itsDataSetIdWithouAccessEntry;
  }
 
  /**
   * Get all docIds list in xml_access table
   */
  private void getDocIdInAccessTable()
                      throws SQLException, McdbException,Exception
  {
    PreparedStatement pStmt;
    String docId;
    ResultSet rs=null;
    
    //the query stirng
    String query="SELECT id.docid from xml_access xa, identifier id where xa.guid = id.guid";
    try
    {
      pStmt=conn.prepareStatement(query);
      //excute the query
      pStmt.execute();
      //get the result set
      rs=pStmt.getResultSet();
      //process the result
      while (rs.next())
      {
        
        docId=rs.getString(1);//the result docId 
        //put the result into docIdInAccessTable vetor
        if (!docIdInAccessTable.contains(docId))// delete duplicate docid
        {
          docIdInAccessTable.add(docId);
        }
      }//while
      //close the pStmt
      pStmt.close();
    }//try
    catch (SQLException e)
    {
    	logMetacat.error("Error in getDocidListForDataPackage: "
                            +e.getMessage());
    }//catch
    //System.out.println("docid in access table");
    /*for (int i=0; i<docIdInAccessTable.size(); i++)
    {
         String str=(String)docIdInAccessTable.elementAt(i);
         System.out.println(str);
    }*/
    //for
  }// getDocIdInAccessTable()
  
  /**
   * associateDefaultValue to docid
   * This docid either couldn't find a mapped dataset or it self is a dataset
   * @param docId, the docid which will be associate default access value
   */
  private void associateDefaultValue(String docId)
                      throws SQLException, McdbException,Exception
  {
    PreparedStatement pStmt;
    
    String query=null;
  
    //the query stirng
    //we let accessfileid blank becuause we couldn't know access file
    query="INSERT INTO xml_access " + 
            "(docid, principal_name, permission, perm_type, perm_order)" +
             "VALUES (?,'public','4','allow','allowFirst')";
    
    try
    {
      pStmt=conn.prepareStatement(query);
      //bind value
      pStmt.setString(1, docId);
      //excute the query
      logMetacat.debug("running sql: " + pStmt.toString());
      pStmt.execute();
      pStmt.close();
    }//try
    catch (SQLException e)
    {
      System.out.println("Error in associateDefaultValue: "
                            +e.getMessage());
    }//catch
    
  }// associateDefaultValue
  /**
   * Get all docIds which don't have an entry in xml_access table
   */
  private void getDocIdWithoutAccessEntry()
                                 throws SQLException, McdbException,Exception
  {
    PreparedStatement pStmt=null;
    String docId;
    ResultSet rs=null;

   
    //the query stirng
    String query="SELECT docid from xml_documents";
    try
    {
      pStmt=conn.prepareStatement(query);
      //excute the query
      pStmt.execute();
      //get the result set
      rs=pStmt.getResultSet();
      //process the result
      while (rs.next())
      {
       
        docId=rs.getString(1);//the result docId 
        //System.out.println("docid in document talbe "+docId);
        //If this docId is not in the docIdInAccessTable list,
        //put the result into docIdInAccessTable vetor
        if (!docIdInAccessTable.contains(docId))
        {
           docIdWithoutAccessEntry.add(docId);
        }
     
      }//while
      //close the pStmt
      pStmt.close();
    }//try
    catch (SQLException e)
    {
      pStmt.close();
      logMetacat.error("Error in getDocidListForDataPackage: "
                            +e.getMessage());
    }//catch
    //System.out.println("docid without access entry:");
    /*for (int i=0; i<docIdWithoutAccessEntry.size(); i++)
    {
         String str=(String)docIdWithoutAccessEntry.elementAt(i);
         System.out.println(str);
    }*/
    //for
  }//getDocIdWithoutAccessEntry()  
  
  /**
   * Find dataset docid for these id which doesn't have an entry in access table
   * The access policy of dataset docid will apply the id which doesn't have an
   * an entry in access table.
   * docid and datasetid which will be stored in a hashtable.
   * docid as a key, datasetid as value
   */
  private void getDocIdMapDataSetId()
                              throws SQLException, McdbException,Exception
  {
    
    PreparedStatement pStmt=null;
    ResultSet rs=null;
    String docId=null;
    String dataSetId=null;
    //make sure there is some documents ids which doesn't has an access entry
    if ( docIdWithoutAccessEntry.isEmpty())
    {
      throw new 
          Exception("Every docid in xml_documents table has access policy");
    }
    String query="SELECT docid from xml_relation where subject= ? or object= ?";
    try
    {
      //find a dataset id for those id which doesn't have access entry
      for (int i=0;i<docIdWithoutAccessEntry.size();i++)
      {
        docId=(String)docIdWithoutAccessEntry.elementAt(i);
        pStmt=conn.prepareStatement(query);
        //bind the value to query
        pStmt.setString(1, docId);
        pStmt.setString(2, docId);
        //execute the query
        pStmt.execute();
        rs=pStmt.getResultSet();
        //process the result
        if (rs.next()) //There are some records for the id in docId fields
        {
          dataSetId=rs.getString(1);
          //System.out.println("dataset id: "+dataSetId);
          //put docid and dataset id into hashtable
          docIdMapDataSetId.put(docId, dataSetId);
        }
        else
        {
         //if not found a dataset id, associateput it into notFoundDataSetId
         //and associate with default value
          //System.out.println("the id couldn't find data set id: "+docId);
          associateDefaultValue(docId);
          notFoundDataSetId.add(docId);
        }
        pStmt.close();
      }//for
    }//try
    catch (SQLException e)
    {
      pStmt.close();
      System.out.println("Error ingetDocIdMapDataSetId: "
                            +e.getMessage());
    }
   
  }//getDocIdMapDataSetId()
  
  /**
   * Associate the access policy of dataset to the docid which the data set id
   * mapped
   */ 
   public void associateAccess()
                           throws SQLException, McdbException,Exception
   {
      String docId=null;
      String dataSetId=null;
      String accessFileId=null;
      String principal=null;
      int    permission=0;
      String permType=null;
      String permOrder=null;
      String beginTime=null;
      String endTime=null;
      int    ticketCount=-1;
      PreparedStatement pStmt=null;
      PreparedStatement insertStatement=null;
      ResultSet rs=null;
      String query=null;
      boolean hasRecord=false;
   
      //make sure there is some documents ids which doesn't has access entry
      if ( docIdWithoutAccessEntry.isEmpty())
      {
          throw new 
            Exception("Every docid in xml_documents table has access policy");
      }
      //every docid without access policy couldn't find a dataset to map
      //assign them default access policy value this aleady done in
      //getDocidMapDataSetId
      else if (docIdMapDataSetId.isEmpty())
      {
        
      }
      else
      {
        try
        {
          Enumeration docList = docIdMapDataSetId.keys();
          while (docList.hasMoreElements())
          {
            docId=(String)docList.nextElement();
            dataSetId=(String)docIdMapDataSetId.get(docId);
            query="select accessfileid, principal_name,permission,perm_type,"
                +"perm_order,begin_time,end_time,ticket_count from xml_access xa, identifier id "
                +"where id.docid = ? and id.guid = xa.guid";
            pStmt=conn.prepareStatement(query);
            //bind the value to query
            pStmt.setString(1, dataSetId);
            //excute the query
            pStmt.execute();
            rs=pStmt.getResultSet();
            //every entry for data set id 
            hasRecord=rs.next();
            //couldn't find access entry for dataset the docid mapped
            //assing defualt value to this docid
            if (!hasRecord)
            {
              
              itsDataSetIdWithouAccessEntry.add(docId);
              associateDefaultValue(docId);
            }
            //find the dataset access entry, apply this entry to docid
            else
            {
              
              while (hasRecord)
              {
                //get datasetid's access policy and store them into variables
                accessFileId=rs.getString(1);
                //System.out.println("accessfileid: "+accessFileId);
                principal=rs.getString(2);
                //System.out.println("principal: "+principal);
                permission=rs.getInt(3);
                //System.out.println("permission: "+permission);
                permType=rs.getString(4);
                //System.out.println("permType: "+permType);
                permOrder=rs.getString(5);
                //System.out.println("permOrder: "+permOrder);
                beginTime=rs.getString(6);
                //System.out.println("beginTime: "+beginTime);
                endTime=rs.getString(7);
                //System.out.println("endTime: "+endTime);
                ticketCount=rs.getInt(8);
                //System.out.println("ticketCount: "+ticketCount);
            
                insertStatement = conn.prepareStatement(
                  "INSERT INTO xml_access " + 
                  "(docid, principal_name, permission, perm_type, perm_order,"+
                  "ticket_count, accessfileid) VALUES "+
                  "(?,?,?,?,?,?,?)");
                // Bind the values to the query
                insertStatement.setString(1, docId);
                insertStatement.setString(2, principal);
                insertStatement.setInt(3, permission);
                insertStatement.setString(4, permType);
                insertStatement.setString(5, permOrder);
                insertStatement.setString(7, accessFileId);
                if ( ticketCount > 0 ) 
                { 
                  insertStatement.setString(6, "" + ticketCount);
                } 
                else 
                {
                  insertStatement.setString(6, null);
                }
                logMetacat.debug("running sql: " + insertStatement.toString());
                insertStatement.execute();
                hasRecord=rs.next();
              }//while
              insertStatement.close();
            }//else
            
          }//while
          pStmt.close();
        }//try
        catch (SQLException e)
        {
          pStmt.close();
          insertStatement.close();
          logMetacat.error("Error in getDocidListForDataPackadge: "
                            +e.getMessage());
        }//catch
        
      }//else
      
  }//AccociateAccess
 
}//class
