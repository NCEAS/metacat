/**
 *  '$RCSfile$'
 *    Purpose: A class represent a server in xml_replcation table
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

import java.util.Date;

/**
 * A class express a entry in xml_replication. It include server name,
 * lastChechedDate, replication or not, dataReplication or not, hub or not
 */
 
public class ReplicationServer
{
  private String serverName = null; //server name
  private Date lastCheckedDate = null; //string of last 
  private boolean replication = false; //replciate xml document or not
  private boolean dataReplication = false; //replciate data file or not
                                           //it is relative to replcation
                                           //if replication is false, it should
                                           //be false
  private boolean hub = false; //it is hub or not. Hub means the localhost can
                               //replcate documents to the server if the 
                               //document's home server is not localhost
  /**
   * Constructor of ReplicationServer
   */
  public ReplicationServer()
  {
    this.serverName = null;
    this.lastCheckedDate = null;
    this.replication = false;
    this.dataReplication = false;
    this.hub = false;
  }//constructor
  
  /**
   * Get server name
   */
  public String getServerName()
  {
    return this.serverName;
  }//getServerName
  
  /**
   * Set a sting as server name
   * @param myServerName, the string will set to object's serverName
   */
  public void setServerName(String myServerName)
  {
    this.serverName = myServerName;
  }//setServerName
  
  /**
   * Get last checked date
   */
  public Date getLastCheckedDate()
  {
    return this.lastCheckedDate;
  }//getLastCheckedDate
  
  /**
   * Set a string as last checked date
   * @Param myLastCheckedDate, the string will set to object's lastCheckedDate
   */
  public void setLastCheckedDate(Date myLastCheckedDate)
  {
    this.lastCheckedDate = myLastCheckedDate;
  }//setLastCheckedDate
   
  /**
   * Get replication xml or not option
   */
  public boolean getReplication()
  {
    return this.replication;
  }//getReplication
  
  /**
   * Set replication option
   * @param myReplication, the option will set to object's replication
   */
  public void setReplication(boolean myReplication)
  {
    this.replication = myReplication;
  }//setReplication
  
  /**
   * Get datareplication xml or not option
   */
  public boolean getDataReplication()
  {
    return this.dataReplication;
  }//getDataReplication
  
  /**
   * Set data replication option
   * @param myDataReplication, the option will set to object's datareplication
   */
  public void setDataReplication(boolean myDataReplication)
  {
    this.dataReplication = myDataReplication;
  }//setDataReplication   
   

/**
   * Get hub option
   */
  public boolean getHub()
  {
    return this.hub;
  }//getHub
  
  /**
   * Set hub option
   * @param myHub, the option will set to object's hub option
   */
  public void setHub(boolean myHub)
  {
    this.hub = myHub;
  }//setHub     
  
}//class ReplicationServer

