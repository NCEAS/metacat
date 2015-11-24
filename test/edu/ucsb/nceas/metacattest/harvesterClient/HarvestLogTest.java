/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis,
 *              and the University of New Mexico
 *              
 *  Purpose: To test the HarvestLog class by using JUnit
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

package edu.ucsb.nceas.metacattest.harvesterClient;

import edu.ucsb.nceas.metacat.harvesterClient.HarvestSiteSchedule;
import java.sql.Connection;
import java.util.Date;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.harvesterClient.Harvester;
import edu.ucsb.nceas.metacat.harvesterClient.HarvestDocument;
import edu.ucsb.nceas.metacat.harvesterClient.HarvestLog;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests HarvestLog code using JUnit.
 *
 * @author  costa
 */
public class HarvestLogTest extends MCTestCase {

  private Connection conn;
  private Harvester harvester;
  private HarvestLog harvestLogShallow;
  private HarvestLog harvestLogDeep;
  private HarvestSiteSchedule harvestSiteSchedule;
  /* Initialize Properties*/
  static
  {
	  try
	  {
		  PropertyService.getInstance();
	  }
	  catch(Exception e)
	  {
		  System.err.println("Exception in initialize option in MetacatServletNetTest "+e.getMessage());
	  }
  }

  /**
   * Constructor for this test.
   * 
   * @param name     name of the test case
   */
  public HarvestLogTest(String name) {
    super(name);
  }
  

  /**
   * Sets up the test by instantiating a HarvestSiteSchedule, HarvestDocument,
   * and two HarvestLog objects. The harvestLogShallow is a HarvestLog that
   * does not have an associated HarvestDetailLog. The harvestLogDeep is a
   * HarvestLog that does have an associated HarvestDetailLog.
   */
  protected void setUp() {
    boolean commandLineMode = true;
    String contactEmail = "jdoe@institution.edu";
    String dateLastHarvest = "2004-04-01 00:00:00.0";
    String dateNextHarvest = "2004-05-01 00:00:00.0";
    int detailLogID;
    String documentListURL = 
                 "http://www.institution.edu/~jdoe/public_html/harvestList.xml";
    String documentType = "eml://ecoinformatics.org/eml-2.0.0";
    String documentURL = 
                   "http://www.institution.edu/~jdoe/public_html/document1.xml";
    String errorMessage = "JUnit Testing";
    HarvestDocument harvestDocument = null;
    int harvestLogID;
    String harvestOperationCode = "harvester.HarvesterStartup";
    Date harvestStartTime = new Date();
    int identifier = 1;
    String ldapDN = "uid=jdoe,o=lter,dc=ecoinformatics,dc=org";
    String ldapPwd = "secretpassword";
    String message = "JUnit Testing";
    int revision = 1;
    String scope = "docname";
    int siteScheduleID = 1;
    int status = 0;
    boolean test = true;
    String unit = "months";
    int updateFrequency = 1;

    harvester = new Harvester();
    Harvester.loadProperties(metacatContextDir);
    conn = harvester.getConnection();  // initializes the database connection
    harvester.initLogIDs();
    harvestLogID = harvester.getHarvestLogID();
    harvestLogShallow = new HarvestLog(harvester, conn, harvestLogID, 
                                       harvestStartTime, status, 
                                       message, harvestOperationCode, 
                                       siteScheduleID);

    harvestLogID = harvester.getHarvestLogID();
    detailLogID = harvester.getDetailLogID();
    harvestSiteSchedule = new HarvestSiteSchedule(harvester,
                                                  siteScheduleID,
                                                  documentListURL,
                                                  ldapDN,
                                                  ldapPwd,
                                                  dateNextHarvest,
                                                  dateLastHarvest,
                                                  updateFrequency,
                                                  unit,
                                                  contactEmail
                                                 );

    harvestDocument = new HarvestDocument(harvester,
                                          harvestSiteSchedule,
                                          scope,
                                          identifier,
                                          revision,
                                          documentType,
                                          documentURL
                                        );
    

    harvestLogDeep = new HarvestLog(harvester, conn, harvestLogID, detailLogID, 
                                    harvestStartTime, status, message,
                                    harvestOperationCode, siteScheduleID,
                                    harvestDocument, errorMessage);    
  }
  

  /** 
   * Closes the database connection after the test completes.
   */
  protected void tearDown() {
    harvester.closeConnection();
  }
  

  /**
   * Tests the getCodeLevelValue() method by comparing the value returned for
   * "error" (lowest level) to the value returned for "debug" (highest level).
   */
  public void testGetCodeLevelValue() {
    int lowLevel = harvestLogShallow.getCodeLevelValue("error");
    int highLevel = harvestLogShallow.getCodeLevelValue("debug");
    
    assertTrue(lowLevel < highLevel);
  }
  

  /**
   * Tests the getExplanation() method. Check that a harvest operation code is
   * associated with an appropriate explanation string.
   */
  public void testGetExplanation() {
    String harvestOperationCode = "harvester.HarvesterStartup";
    String explanation;

    explanation = harvestLogShallow.getExplanation(harvestOperationCode);
    assertTrue(explanation.equals("Harvester start up"));    
  }
  

  /**
   * Tests the getHarvestOperationCodeLevel() method. Check that the method
   * returns an appropriate code level for a given harvest operation code.
   */
  public void testGetHarvestOperationCodeLevel() {
    String harvestOperationCode = "harvester.HarvesterStartup";
    String harvestOperationCodeLevel;

    harvestOperationCodeLevel = 
           harvestLogShallow.getHarvestOperationCodeLevel(harvestOperationCode);
    assertTrue(harvestOperationCodeLevel.equalsIgnoreCase("Info"));    
  }


  /**
   * Tests the construction of HarvestLog object, and a HarvestLog object
   * that contains a HarvestDetailLog object.
   */
  public void testHarvestLogObject() {
    assertTrue(harvestLogShallow != null);
    assertTrue(harvestLogDeep != null);
  }
  
  
  /**
   * Tests the printOutput() method when the code level of this operation is
   * greater than the maximum level we wish to print. This means that the output
   * should not be printed.
   */
  public void testPrintOutputExceedsMax() {
    System.out.println("No output should be printed:");
    harvestLogDeep.printOutput(System.out, "Error");
  }
  
  
  /**
   * Tests the printOutput() method when the code level of this operation is
   * less than the maximum level we wish to print. This means that the output
   * should be printed.
   */
  public void testPrintOutputWithinMax() {
    System.out.println("The log output should be printed:");
    harvestLogDeep.printOutput(System.out, "Debug");
  }
  
  
  /**
   * Returns the test suite. The test suite consists of all methods in this
   * class whose names start with "test".
   * 
   * @return  a TestSuite object
   */
  public static Test suite() {
    return new TestSuite(HarvestLogTest.class);
  }
  

  /**
   * The main program. Runs the test suite.
   * 
   * @param args   command line argument array.
   */
  public static void main(String args[]) {
    junit.textui.TestRunner.run(suite());
  }

}
