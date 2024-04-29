/*  '$RCSfile$'
 *  Copyright: 2018 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.util;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;

import junit.framework.Test;
import junit.framework.TestSuite;
/**
 * A JUnit test class for the SystemUil class.
 * @author tao
 *
 */
public class SystemUtilTest extends MCTestCase {
   
    /**
     * Constructor
     * @param name
     */
    public SystemUtilTest(String name) {
        super(name);
    }
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite();
        suite.addTest(new SystemUtilTest("initialize"));
        suite.addTest(new SystemUtilTest("testGetInternalURLs"));
         return suite;
    }
    
    /**
     * init
     */
    public void initialize() {
        assertTrue(1==1);
    }
    
    
    /**
     * Test the methods to get internal urls
     * @throws Exception
     */
    public void testGetInternalURLs() throws Exception {
        String expectedUrl = LeanTestUtils.getExpectedProperties()
            .getProperty("expected.internalURL", "http" + "://localhost:80");
        String internalURL = SystemUtil.getInternalServerURL();
        System.out.println("============the internal url is " + internalURL);
        System.out.println("=============the internal url has been replaced by the external url is "
            + SystemUtil.isInternalURLReplacedByExternal());
        if (!SystemUtil.isInternalURLReplacedByExternal()) {
            assertEquals(expectedUrl, internalURL);
        }
        internalURL = SystemUtil.getInternalServerURL();
        if (!SystemUtil.isInternalURLReplacedByExternal()) {
            assertEquals(expectedUrl, internalURL);
        }
        String contextURL = SystemUtil.getInternalContextURL();
        System.out.println("========================= the context url is " + contextURL);
        if (!SystemUtil.isInternalURLReplacedByExternal()) {
            assertEquals(expectedUrl + "/" + PropertyService.getProperty("application.context"),
                contextURL);
        }
    }
    
  
   
}
