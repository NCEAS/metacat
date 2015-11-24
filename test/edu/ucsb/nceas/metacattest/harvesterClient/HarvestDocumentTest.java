/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis,
 *              and the University of New Mexico
 *              
 *  Purpose: To test the HarvestDocument class by using JUnit
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
import edu.ucsb.nceas.metacat.harvesterClient.HarvestDocument;
import edu.ucsb.nceas.metacat.harvesterClient.HarvestSiteSchedule;
import edu.ucsb.nceas.metacat.harvesterClient.Harvester;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;

import java.io.StringReader;
import java.util.Date;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests HarvestDocument code using JUnit.
 *
 * @author  costa
 */
public class HarvestDocumentTest extends MCTestCase {

  private Harvester harvester;
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
  public HarvestDocumentTest(String name) {
    super(name);
  }
  

  /**
   * Sets up the test by instantiating HarvestSiteSchedule and HarvestDocument
   * objects.
   */
  protected void setUp() {
    boolean commandLineMode = true;
    String contactEmail = "jdoe@institution.edu";
    String dateLastHarvest = "2004-04-01 00:00:00.0";
    String dateNextHarvest = "2004-05-01 00:00:00.0";
    int detailLogID;
    String documentListURL = 
                 "https://code.ecoinformatics.org/code/metacat/branches/METACAT_1_9_4_BRANCH/test/harvester/sampleHarvestList.xml";
    String documentType = "eml://ecoinformatics.org/eml-2.0.0";
    String documentURL = 
                     "https://code.ecoinformatics.org/code/metacat/branches/METACAT_1_9_4_BRANCH/test/eml-sample.xml";
    String errorMessage = "JUnit Testing";
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
    harvester.getConnection();  // initializes the database connection
    harvester.initLogIDs();
    harvester.setHarvestStartTime(new Date());

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
  }
  

  /**
   * Closes the database connection after the test completes.
   */
  protected void tearDown() {
    harvester.closeConnection();
  }
  

  /**
   * Tests the getSiteDocument() method. The test succeeds if a non-null
   * StringReader is returned.
   */
  public void testGetSiteDocument() {
    StringReader stringReader = null;
    stringReader = harvestDocument.getSiteDocument();
    assertTrue(stringReader != null);
  }
  
  
  /**
   * Tests that the harvesterDocument object was created successfully.
   */
  public void testHarvestDocumentObject() {
    assertTrue(harvestDocument != null);
  }
  

  /**
   * Tests the metacatHighestRevision() method. This test ensures that -1
   * is returned for a non-existent document.
   */
  public void testMetacatHighestRevision() {
    int highestRevision;
    
    highestRevision = harvestDocument.metacatHighestRevision();
    assertTrue(highestRevision == -1);
  }


  /**
   * Tests the printOutput() method.
   */
  public void testPrintOutput() {
    harvestDocument.printOutput(System.out);
  }
  
  
  /**
   * Returns the test suite. The test suite consists of all methods in this
   * class whose names start with "test".
   * 
   * @return  a TestSuite object
   */
  public static Test suite() {
    return new TestSuite(HarvestDocumentTest.class);
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
