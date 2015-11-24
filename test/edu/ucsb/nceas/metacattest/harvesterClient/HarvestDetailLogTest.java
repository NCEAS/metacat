/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis,
 *              and the University of New Mexico
 *              
 *  Purpose: To test the HarvestDetailLog class by using JUnit
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
import edu.ucsb.nceas.metacat.harvesterClient.HarvestDetailLog;
import edu.ucsb.nceas.metacat.harvesterClient.HarvestDocument;
import edu.ucsb.nceas.metacat.harvesterClient.HarvestSiteSchedule;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;

import java.sql.Connection;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests HarvestDetailLog code using JUnit.
 *
 * @author  costa
 */
public class HarvestDetailLogTest extends MCTestCase {

  private Harvester harvester;
  private HarvestDetailLog harvestDetailLog;
  private HarvestDocument harvestDocument;
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
  public HarvestDetailLogTest(String name) {
    super(name);
  }
  

  /**
   * Sets up the test by instantiating HarvestSiteSchedule, HarvestDocument,
   * and HarvestDetailLog objects.
   */
  protected void setUp() {
    Connection conn;
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
    String errorMessage = "JUnit testing";
    int harvestLogID;
    int identifier = 1;
    String ldapDN = "uid=jdoe,o=lter,dc=ecoinformatics,dc=org";
    String ldapPwd = "secretpassword";
    int revision = 1;
    String scope = "docname";
    int siteScheduleID = 1;
    boolean test = true;
    String unit = "months";
    int updateFrequency = 1;
  
    harvester = new Harvester();
    Harvester.loadProperties(metacatContextDir);
    conn = harvester.getConnection();  // initializes the database connection
    harvester.initLogIDs();
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
    
    harvestDetailLog = new HarvestDetailLog(harvester, conn, harvestLogID, 
                                            detailLogID, harvestDocument, 
                                            errorMessage);
  }
  

  /**
   * Closes the database connection when the test completes.
   */
  protected void tearDown() {
    harvester.closeConnection();
  }


  /**
   * Tests that the harvestDetailLog object was constructed.
   */
  public void testHarvestDetailLogObject() {
    assertTrue(harvestDetailLog != null);
  }
  
  
  /**
   * Tests the printOutput() method.
   */
  public void testPrintOutput() {
    harvestDetailLog.printOutput(System.out);
  }
  
  
  /**
   * Returns the test suite. The test suite consists of all methods in this
   * class whose names start with "test".
   * 
   * @return  a TestSuite object
   */
  public static Test suite() {
    return new TestSuite(HarvestDetailLogTest.class);
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
