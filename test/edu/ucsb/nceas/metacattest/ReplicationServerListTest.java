/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *    Purpose: To test the ReplicationServerList class by JUnit
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

package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.*;
import edu.ucsb.nceas.metacat.database.*;
import edu.ucsb.nceas.metacat.replication.*;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.*;
import java.util.Date;

/**
 * A JUnit test for testing Step class processing
 */
public class ReplicationServerListTest extends MCTestCase
{
    private static String metacatReplicationURL;
	 /* Initialize properties*/
	  static
	  {
		  try
		  {
			 PropertyService.getInstance();
			 metacatReplicationURL=
                     PropertyService.getProperty("junitreplicationurl");
		  }
		  catch(Exception e)
		  {
			  System.err.println("Exception in initialize option in MetacatServletNetTest "+e.getMessage());
		  }
	  }

     private ReplicationServerList serverList = null;
     private static final Log log = LogFactory.getLog("edu.ucsb.nceas.metacattest.ReplicationServerListTest");
 
  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   */
  public ReplicationServerListTest(String name)
  {
    super(name);
  }

  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   * @param list the ReplicationServerList will be passed int
   */
  public ReplicationServerListTest(String name, ReplicationServerList list)
  {
    super(name);
    serverList=list;
  }
 
  /**
   * Establish a testing framework by initializing appropriate objects
   */
  public void setUp()
 {
   
 }

  /**
   * Release any objects after tests are complete
   */
  public void tearDown()
  {
    //DBConnectionPool will be release
    DBConnectionPool.release();
  }

  /**
   * Create a suite of tests to be run together
   */
  public static Test suite()
  {
     //Get DBConnection pool, this is only for junit test.
    //Because DBConnection is singleton class. So there is only one DBConnection
    //pool in the program
    try
    {
      DBConnectionPool pool = DBConnectionPool.getInstance();
    }//try
    catch (Exception e)
    {
      log.debug("Error in ReplicationServerList() to get" +
                        " DBConnection pool"+e.getMessage());
    }//catch
    
    TestSuite suite = new TestSuite();
    suite.addTest(new ReplicationServerListTest("initialize"));
    
    try
    {
  
    //Add two server into xml_replication table
    URL dev = new URL(metacatReplicationURL+"?action=servercontrol&server=dev"+
                        "&subaction=add&replicate=1&datareplicate=1&hub=1");
    URL epsilon = new URL(metacatReplicationURL+"?action=servercontrol"+
             "&server=epsilon&subaction=add&replicate=0&datareplicate=1&hub=0");
    InputStream input = dev.openStream();
    input.close();
    input = epsilon.openStream();
    input.close();
    
    //create a new server list
    ReplicationServerList list = new ReplicationServerList();
    
    //Doing test test cases
    suite.addTest(new ReplicationServerListTest("testSize", list));
    
    suite.addTest(new ReplicationServerListTest("testServerAt0", list));
    suite.addTest(new ReplicationServerListTest("testServerAt1", list));
    
    suite.addTest(new ReplicationServerListTest
                                              ("testNonEmptyServerList", list));
    
    suite.addTest(new ReplicationServerListTest("testServerIsNotInList", list));
    suite.addTest(new ReplicationServerListTest("testServerIsInList", list));
    
    suite.addTest(new ReplicationServerListTest
                                              ("testGetLastCheckedDate", list));
    
    suite.addTest(new ReplicationServerListTest
                                        ("testGetReplicationValueFalse", list));
    
    suite.addTest(new ReplicationServerListTest
                                         ("testGetReplicationValueTrue", list));
    suite.addTest(new ReplicationServerListTest
                                    ("testGetDataReplicationValueFalse", list));
    
    suite.addTest(new ReplicationServerListTest("testGetHubValueTrue", list));
    suite.addTest(new ReplicationServerListTest("testGetHubValueFalse", list));
    
    //Delete this two server
    URL deleteDev = new URL(metacatReplicationURL+"?action=servercontrol" +
                                        "&server=dev&subaction=delete");
    URL deleteEpsilon = new URL(metacatReplicationURL+"?action=servercontrol" +
                                        "&server=epsilon&subaction=delete");
    input = deleteDev.openStream();
    input.close();
    input = deleteEpsilon.openStream();
    input.close();                                   
    }//try
    catch (Exception e)
    {
      log.debug("Error in ReplicationServerListTest.suite: "+
                                e.getMessage());
    }//catch
    
 
    
                              
    return suite;
  }
  


  /**
   * Run an initial test that always passes to check that the test
   * harness is working.
   */
  public void initialize()
  {
    assertTrue(1 == 1);
  }

   
  /**
   * Test the a empty server list is empty
   */
  public void testEmptyServerList()
  {
    
    assertTrue(serverList.isEmpty());
  }
  
  /**
   * Test the a non-empty server list is non-empty
   */
  public void testNonEmptyServerList()
  {
    
    assertTrue(!serverList.isEmpty());
  }
  
  /**
   * Test the size() method
   */
  public void testSize()
  {
    int size = serverList.size();
    assertTrue(size ==2);
  }
  
  /**
   * Test the method serverAt(0)
   */
  public void testServerAt0()
  {
    ReplicationServer server = serverList.serverAt(0);
    String serverName = server.getServerName();
    assertTrue(serverName.equals("dev"));
  }
  
  /**
   * Test the method serverAt(1)
   */
  public void testServerAt1()
  {
    ReplicationServer server = serverList.serverAt(1);
    String serverName = server.getServerName();
    assertTrue(serverName.equals("epsilon"));
  }
   
  
  /**
   * Test the a given server is not in the server list
   */
  public void testServerIsNotInList()
  {
    assertTrue(!serverList.isGivenServerInList("localhost"));
  }
  
  /**
   * Test the a given server is in the server list
   */
  public void testServerIsInList()
  {
    assertTrue(serverList.isGivenServerInList("dev"));
  }
  
  /**
   * Test the method getLastCheckedDate
   */
  public void testGetLastCheckedDate()
  {
    Date lastCheckedDate = serverList.getLastCheckedDate("dev");
    assertTrue(lastCheckedDate.equals("0001-01-01 BC"));
  }
  
  /**
   * Test the method getReplicationValue(resulst is true)
   */
  public void testGetReplicationValueTrue()
  {
    assertTrue(serverList.getReplicationValue("dev"));
  }
  
  /**
   * Test the method getReplicationValue(result is false)
   */
  public void testGetReplicationValueFalse()
  {
    assertTrue(!serverList.getReplicationValue("epsilon"));
  }
  
  /**
   * Test the method getDataReplicationValue(result is true)
   */
  public void testGetDataReplicationValueTrue()
  {
    assertTrue(serverList.getDataReplicationValue("dev"));
  }
  
  /**
   * Test the method getDataReplicationValue(result is false)
   */
  public void testGetDataReplicationValueFalse()
  {
    assertTrue(!serverList.getDataReplicationValue("epsilon"));
  }

  /**
   * Test the method getHubValue(result is true)
   */
  public void testGetHubValueTrue()
  {
    assertTrue(serverList.getHubValue("dev"));
  }
  
  /**
   * Test the method getHubValue(result is false)
   */
  public void testGetHubValueFalse()
  {
    assertTrue(!serverList.getHubValue("epsilon"));
  }  

   
}//ReplicationServerListTest
