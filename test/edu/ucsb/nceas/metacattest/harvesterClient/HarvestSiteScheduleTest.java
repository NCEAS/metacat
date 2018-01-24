/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis,
 *              and the University of New Mexico
 *              
 *  Purpose: To test the HarvestSiteSchedule class by using JUnit
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

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.harvesterClient.Harvester;
import edu.ucsb.nceas.metacat.harvesterClient.HarvestSiteSchedule;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;

import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests HarvestSiteSchedule code using JUnit.
 *
 * @author  costa
 */
public class HarvestSiteScheduleTest extends MCTestCase {

  private Harvester harvester;
  private HarvestSiteSchedule harvestSiteScheduleFuture; // future date next har
  private HarvestSiteSchedule harvestSiteSchedulePast; // past date next harvest
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
  public HarvestSiteScheduleTest(String name) {
    super(name);
  }
  

  /**
   * Sets up the HarvestSiteScheduleTest by creating a pair of 
   * HarvestSiteSchedule objects. The harvestSiteScheduleFuture object
   * is used for a site with a future date of next harvest (not due for 
   * harvest now), while the harvestSiteSchedulePast object is used for a site 
   * with a past date of next harvest (is due for harvest now).
   */
  protected void setUp() {
    boolean commandLineMode = true;
    String contactEmail = "jdoe@institution.edu";
    String dateLastHarvest = "2004-04-01 00:00:00.0";
    String dateNextHarvestFuture = "2056-01-01 00:00:00.0";
    String dateNextHarvestPast = "2000-01-01 00:00:00.0";
    String documentListURL = 
                "https://code.ecoinformatics.org/code/metacat/branches/METACAT_1_9_4_BRANCH/test/harvester/sampleHarvestList.xml";
    String errorMessage = "JUnit Testing";
    String ldapDN = "uid=jdoe,o=lter,dc=ecoinformatics,dc=org";
    String ldapPwd = "secretpassword";
    int siteScheduleID = 1;
    boolean test = true;
    String unit = "months";
    int updateFrequency = 1;

    harvester = new Harvester();
    Harvester.loadProperties(metacatContextDir);
    harvester.getConnection();  // initializes the database connection
    harvester.initLogIDs();
    harvester.setHarvestStartTime(new Date());

    harvestSiteScheduleFuture = new HarvestSiteSchedule(harvester,
                                                  siteScheduleID,
                                                  documentListURL,
                                                  ldapDN,
                                                  ldapPwd,
                                                  dateNextHarvestFuture,
                                                  dateLastHarvest,
                                                  updateFrequency,
                                                  unit,
                                                  contactEmail
                                                 );

    harvestSiteSchedulePast = new HarvestSiteSchedule(harvester,
                                                  siteScheduleID,
                                                  documentListURL,
                                                  ldapDN,
                                                  ldapPwd,
                                                  dateNextHarvestPast,
                                                  dateLastHarvest,
                                                  updateFrequency,
                                                  unit,
                                                  contactEmail
                                                 );

  }
  

  /**
   * Closes the database connection after the test completes.
   */
  protected void tearDown() {
    harvester.closeConnection();
  }
  

  /**
   * Tests the dueForHarvest() method in the case where a site is not due for
   * a harvest.
   */
  public void testDueForHarvestFalse() {
    boolean dueForHarvest;
    
    dueForHarvest = harvestSiteScheduleFuture.dueForHarvest();
    assertTrue(dueForHarvest == false);
  }
  

  /**
   * Tests the dueForHarvest() method in the case where a site is due for
   * a harvest.
   */
  public void testDueForHarvestTrue() {
    boolean dueForHarvest;
    
    dueForHarvest = harvestSiteSchedulePast.dueForHarvest();
    assertTrue(dueForHarvest == true);
  }


  /**
   * Tests the parseHarvestList() method.
   */
  public void testParseHarvestList() {
    boolean success = false;
    String schemaLocation =
         "eml://ecoinformatics.org/harvestList ./lib/harvester/harvestList.xsd";
         
    harvestSiteScheduleFuture.setSchemaLocation(schemaLocation);
    
    try {
      success = harvestSiteScheduleFuture.parseHarvestList();
      assertTrue(success == true);
    }
    catch (ParserConfigurationException e) {
      fail("ParserConfigurationException: " + e.getMessage());
    }    
  }


  /**
   * Tests the printOutput() method.
   */
  public void testPrintOutput() {
    harvestSiteScheduleFuture.printOutput(System.out);
  }
  

  /**
   * Tests that the harvesterDocument object was created successfully.
   */
  public void testHarvestSiteScheduleObject() {
    assertTrue(harvestSiteScheduleFuture != null);
    assertTrue(harvestSiteSchedulePast != null);
  }
  
  
  /**
   * Returns the test suite. The test suite consists of all methods in this
   * class whose names start with "test".
   * 
   * @return  a TestSuite object
   */
  public static Test suite() {
    return new TestSuite(HarvestSiteScheduleTest.class);
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
