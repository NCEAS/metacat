/**
 *  '$RCSfile$'
 *    Purpose: A class represent a replication server list in 
               xml_replcation table
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
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

package edu.ucsb.nceas.metacat.replication;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;

/**
 * A class represent a replication server list in xml_replcation table
 */
 
public class ReplicationServerList
{
  private static Vector<ReplicationServer> serverList = null; //Vector to store server list
  private static Logger logMetacat = Logger.getLogger(ReplicationServerList.class);

  /**
   * constructor of ReplicationServerList
   * It will build server list. If only local host exists, ther server list
   * will be null
   */
  public ReplicationServerList()
  {
    
    serverList = new Vector<ReplicationServer>();
    //Build serverList
    buildServerList();
  }//ReplicationServerList
  
  /**
   * Method to create server list from xml_replication table
   * If in xml_replication table, only local host exists, server list will be
   * empty
   */
  private void buildServerList()
  {
    ReplicationServer server = null; //element in server list
    DBConnection dbConn = null;//DBConnection
    int serialNumber = -1;//DBConnection serial number
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    boolean hasReplication = false;//Replication xml or not
    boolean hasDataReplication = false;//Replication data file or not
    boolean hasSystemMetadataReplication = false;//Replication for system metadata or not
    boolean isHub = false;//local host is the hub for this server or not
    
    try
    {
      //Get DBConnection
      dbConn=DBConnectionPool.
                  getDBConnection("ReplicationHandler.buildServerList");
      serialNumber=dbConn.getCheckOutSerialNumber();
      //Select fields from xml_replication table
      pstmt = dbConn.prepareStatement("select server, last_checked, replicate,"+
                    " datareplicate, hub from xml_replication");
      //Execute prepare statement
      pstmt.execute();
      //Get result set
      rs = pstmt.getResultSet();
      //Iterate over the result set
      boolean tableHasRows = rs.next();
      while(tableHasRows)
      {
        //Server name
        String serverName = rs.getString(1);
        logMetacat.info("ServerName: "+serverName);
        //Last check date
        Date lastChecked = rs.getDate(2);
        logMetacat.info("Last checked time: "+lastChecked);
        //Replication value
        int replication = rs.getInt(3);
        logMetacat.info("Replication value: "+replication);
        //Data replication value
        int dataReplication = rs.getInt(4);
        logMetacat.info("DataReplication value: "+dataReplication);
        //Hub value
        int hubValue = rs.getInt(5);
        logMetacat.info("Hub value: "+hubValue);
        //Get rid of local host
        if(!serverName.equals("localhost"))
        {
          
          //create a new object of Replication
          server = new ReplicationServer();
         
          //Set server name
          server.setServerName(serverName);
     
          //Set last check date
          server.setLastCheckedDate(lastChecked);
          //From replication value to determine hasReplication valuse
          if (replication ==1)
          {
            //If replication equals 1, it can replicate xml documents
            hasReplication = true;
          }//if
          else
          {
            //if replication is NOT 1, it can't replicate xml documents
            hasReplication = false;
          }//else
          //Set replication value
          server.setReplication(hasReplication);
           
          //From both replication and data replication value to determine
          //hasDataReplication value
          if (hasReplication && dataReplication ==1)
          { 
            //Only server can replicate xml (hasRplication == true) and
            // dataReplication is 1, local host can replicate data file
            hasDataReplication = true;
          }//if
          else
          {
            hasDataReplication = false;
          }//else
          //Set data replciation value
          server.setDataReplication(hasDataReplication);
          
          //Detemine isHub by hubValue
          if (hubValue ==1)
          {
            isHub = true;
          }//if
          else
          {
            isHub = false;
          }//else
          //Set hub value
          server.setHub(isHub);
          
          //Add this server into server list
          serverList.add(server);
        }//if
        //Go to new row      
        tableHasRows = rs.next();   
      }//while
     
    }//try
    catch(Exception e)
    {
      logMetacat.error("Error in ReplicationServerList."
                                    +"buildServerList(): "+e.getMessage());
    }//catch
    finally
    {
      try
      {
        //close result set
        rs.close();
        //close prepareed statement
        pstmt.close();
      }//try
      catch (SQLException sqlE)
      {
        logMetacat.error("Error in ReplicationHandler.buildServerList: "
                                +sqlE.getMessage());
      }//catch
      finally
      {
        //Return dbconnection too pool
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }//finally
    }//finally
  
  }//buildServerList
  
  /**
   * Method to determine the server list is empty or not
   */
  public boolean isEmpty()
  {
    return serverList.isEmpty();
  }//isEmpty
  
  /**
   * Method to get the size of serverList
   */
  public int size()
  {
    return serverList.size();
  }//size()
  
  /**
   * Method to add a new replciation server object to serverList
   * @Param newReplciationServer, the object need to be add
   */
  private synchronized void addNewReplicationServer
                                      (ReplicationServer newReplicationServer)
  {
    serverList.add(newReplicationServer);
  }//addNewReplicationServer
  
  /**
   * Method to get a server object given a index number
   * @param index, the given index number
   */
  public ReplicationServer serverAt(int index)
  {
    return (ReplicationServer)serverList.elementAt(index);
  }//serverAt
   

  /**
   * Method to determine if a given server in the replication server list
   * @param givenServerName, a server name will be check.
   */
  public boolean isGivenServerInList(String givenServerName)
  {
    boolean result = false;//Variable store the return value
    ReplicationServer server = null; //Element in vetor
    int size = 0;//Variable to store the size of vector serverList
    //Get the size of vector
    size = serverList.size();
    
    //If server list is empty
    if (size == 0||givenServerName == null||givenServerName.equals(""))
    {
      result = false;
      return result;
    }
    
    //Check every element in the vector
    for (int i = 0; i < size; i++)
    {
      //Get the ReplicationServer object indexed i in vector
      server = (ReplicationServer) serverList.elementAt(i);
      //If given name match a server's name, return true
      if ( givenServerName.equalsIgnoreCase(server.getServerName()))
      {
        result = true;
        return result;
      }//if
      
    }//for
    
    //If reach here, there is no server's name match the given name
    //flase will return
    return result;
  }//isGinvenServerInList
  
  /**
   * Method to determine its index in server list for a given server.
   * If couldn't find it, -1 will be returned
   * @param givenServerName, a server name will be check.
   */
  private synchronized int findIndexInServerList(String givenServerName)
  {
    int index = -1;//Variable store the return value
    ReplicationServer server = null; //Element in vetor
    int size = 0;//Variable to store the size of vector serverList
    //Get the size of vector
    size = serverList.size();
    
    //If server list is empty, -1 will be returned
    if (size == 0 || givenServerName ==null ||givenServerName.equals(""))
    {
      return index;
    }
    
    //Check every element in the vector
    for (int i = 0; i < size; i++)
    {
      //Get the ReplicationServer object indexed i in vector
      server = (ReplicationServer) serverList.elementAt(i);
      //If given name match a server's name, return true
      if ( givenServerName.equalsIgnoreCase(server.getServerName()))
      {
        index = i;
        return index;
      }//if
      
    }//for
    
    //If reach here, there is no server's name match the given name
    //-1 will return
    return index;
  }//isGinvenServerInList
  
  /**
   * To a given server name, try to get its lastcheck date.
   * If couldn't find the server in the server list, null will return
   * @param givenServerName, the server's name which we want to get last checked
   * out date
   */
  public synchronized Date getLastCheckedDate(String givenServerName)
  {
    int index = -1;//Variable to store the index
    ReplicationServer server = null;//Variable for replication server
    
    //Get the server's index in server list
    index = findIndexInServerList(givenServerName);
    //If index = -1, couldn't find this server, null will return
    if (index == -1)
    {
      return null;
    }//if
    //Get Replication server object
    server = (ReplicationServer) serverList.elementAt(index);
    //return its lastcheckeddate attributes in this object
    return server.getLastCheckedDate();
  }//getLastCehckedDate
   
  /**
   * To a given server name, try to get its xml replciation option
   * If couldn't find the server in the server list, false will return
   * @param givenServerName, the server's name which we want to get replication
   * value
   */
  public synchronized boolean getReplicationValue(String givenServerName)
  {
    int index = -1;//Variable to store the index
    ReplicationServer server = null;//Variable for replication server
    
    //Get the server's index in server list
    index = findIndexInServerList(givenServerName);
    //If index = -1, couldn't find this server, null will return
    if (index == -1)
    {
      return false;
    }//if
    //Get Replication server object
    server = (ReplicationServer) serverList.elementAt(index);
    //return this object's replication value
    return server.getReplication();
  }//getReplicationValue
  
  /**
   * To a given server name, try to get its data file replciation option
   * If couldn't find the server in the server list, false will return
   * @param givenServerName, the server's name which we want to get data file 
   * replication value
   */
  public synchronized boolean getDataReplicationValue(String givenServerName)
  {
    int index = -1;//Variable to store the index
    ReplicationServer server = null;//Variable for replication server
    
    //Get the server's index in server list
    index = findIndexInServerList(givenServerName);
    //If index = -1, couldn't find this server, null will return
    if (index == -1)
    {
      return false;
    }//if
    //Get Replication server object
    server = (ReplicationServer) serverList.elementAt(index);
    //Return this object's data replication value
    return server.getDataReplication();
  }//getDataReplicationValue
  

  /**
   * To a given server name, try to get its hub option
   * If couldn't find the server in the server list, false will return
   * @param givenServerName, the server's name which we want to get hub
   */
  public synchronized boolean getHubValue(String givenServerName)
  {
    int index = -1;//Variable to store the index
    ReplicationServer server = null;//Variable for replication server
    
    //Get the server's index in server list
    index = findIndexInServerList(givenServerName);
    //If index = -1, couldn't find this server, null will return
    if (index == -1)
    {
      return false;
    }//if
    //Get Replication server object
    server = (ReplicationServer) serverList.elementAt(index);
    //Return this object's hub value
    return server.getHub();
  }//getHubValue  
  
  /**
   * To a given server name, to check if it is in the server list.
   * If it is not, add it to server list and the xml_replication table
   * This method is for a document was replicated by a hub and the document's
   * home server is not in the replication table. The value for replicate,
   * datareplicate and hub are all false
   * @param givenServerName, the server's name which want to check
   */
  public synchronized void addToServerListIfItIsNot(String givenServerName)
  {
    // Variable to store the index for the givenServerName
    int index = -1;
    // Variable to store the new replciation server object
    ReplicationServer newServer = null;
    // For sql command
    PreparedStatement pStmt=null;
    // For check out DBConnection
    DBConnection dbConn = null;
    int serialNumber = -1;
    // Field value for xml_replication table
    int replicate = 0;
    int dataReplicate = 0;
    int hub = 0;
    
    // Get the the index
    index = findIndexInServerList(givenServerName);
    // if index ==-1, it not in the server list
    // Add the server to server list
    // Add the server to xml_replication table
    if (index ==-1)
    {
      // Create a new replication server object it's replicate, datareplicate
      // and hub values are default - false
      newServer = new ReplicationServer();
      // Set newServer's name as givenServerName
      newServer.setServerName(givenServerName);
      // Add newServer to serverList
      this.addNewReplicationServer(newServer);
      // Add this server to xml_table too
     try
      {
    	 Calendar cal = Calendar.getInstance();
         cal.set(1980, 1, 1);
        // Checkout DBConnection     
        dbConn=DBConnectionPool.
             getDBConnection("ReplicationSErverList.addToServerListIfItIsNot");
        serialNumber=dbConn.getCheckOutSerialNumber();
        // Inser it into xml_replication table
        /*pStmt = dbConn.prepareStatement("INSERT INTO xml_replication " +
                      "(server, last_checked, replicate, datareplicate, hub) "+
                       "VALUES ('" + givenServerName + "', to_date(" +
                       "'01/01/00', 'MM/DD/YY'), '" +
                       replicate +"', '"+dataReplicate+"','"+ hub + "')");*/
        pStmt = dbConn.prepareStatement("INSERT INTO xml_replication " +
                      "(server, last_checked, replicate, datareplicate, hub) "+
                       "VALUES (?, ?, ?, ?, ?, ?)");
        pStmt.setString(1, givenServerName);
		pStmt.setTimestamp(2, new Timestamp(cal.getTimeInMillis()) );
        pStmt.setInt(3, replicate);
        pStmt.setInt(4, dataReplicate);
        pStmt.setInt(5, hub);

        pStmt.execute();
       
        pStmt.close();
      }//try
      catch (Exception e)
      {
        logMetacat.error("Error in ReplicationServerList."+
                            "addToServerListIfItIsNot: " + e.getMessage());
      }//catch
      finally
      { 
        try
        {
          pStmt.close();
        }//try
        catch (Exception ee)
        { 
          logMetacat.error("Error in ReplicationServerList."+
                            "addToServerListIfItIsNot: " + ee.getMessage());
        }//catch
        finally
        {
          DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }//finally
      }//finally
    }//if
  }//addToServerListIfItIsNot
  
}//ReplicationServerList
