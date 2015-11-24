/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis,
 *              and the University of New Mexico
 *              
 *  Purpose: To test the Harvester class by using JUnit
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
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Harvester code using JUnit.
 *
 * @author  costa
 */
public class HarvesterTest extends MCTestCase {

  private Harvester harvester;
	/* Initialize Properties */
	static {
		try {
			PropertyService.getInstance();
		} catch (Exception e) {
			System.err.println("Exception in initialize option in MetacatServletNetTest "
					+ e.getMessage());
		}
	}

  /**
	 * Constructor for this test.
	 * 
	 * @param name
	 *            name of the test case
	 */
  public HarvesterTest(String name) {
    super(name);
  }
  

  /**
   * Sets up the test by instantiating a Harvester object.
   */
  protected void setUp() {
    harvester = new Harvester();
  }
  

  /**
   * No clean-up actions necessary for these tests.
   */
  protected void tearDown() {
  }
  

  /**
   * Tests the dequoteText() string function. It converts single quotes to
   * double quotes.
   */
  public void testDequoteText() {
    String singleQuoteString = "I can't see how it's done!\n";
    String compareString = "I can\"t see how it\"s done!";
    String dequotedString = "";
    
    dequotedString = harvester.dequoteText(singleQuoteString);
    assertTrue(dequotedString.equals(compareString));
    System.out.println("Dequoted string: " + dequotedString);
  }
  
  
  /**
   * Tests that the Harvester object was created successfully.
   */
  public void testHarvesterObject() {
    assertTrue(harvester != null);
  }
  
  
    /**
	 * Tests loading of Harvester properties from a configuration file.
	 */
	public void testLoadProperties() {
	    boolean commandLineMode = true;
		String ctm = null;
		boolean test = true;

		Harvester.loadProperties(metacatContextDir);
		try {
			ctm = PropertyService.getProperty("harvester.connectToMetacat");
		} catch (PropertyNotFoundException pnfe) {
			fail("Could not get connectToMetacat property: "+ pnfe.getMessage());
		}
		assertTrue(ctm.equals("true") || ctm.equals("false"));
	}
  
  
  /**
	 * Prints the files in the current working directory. This may be useful for
	 * determining which directory all other tests are running in.
	 */
  public void testWorkingDirectory() {
    String[] dir = new java.io.File(".").list(); // Get files in current dir

    java.util.Arrays.sort(dir);                  // Sort the directory listing

    for (int i=0; i<dir.length; i++)
      System.out.println(dir[i]);                // Print the list
  }
  
  
  /**
   * Returns the test suite. The test suite consists of all methods in this
   * class whose names start with "test".
   * 
   * @return  a TestSuite object
   */
  public static Test suite() {
    return new TestSuite(HarvesterTest.class);
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
