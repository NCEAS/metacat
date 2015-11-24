/*  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author: berkley $'
 *     '$Date: 2010-05-03 14:26:08 -0700 (Fri, 14 Aug 2009) $'
 * '$Revision: 5027 $'
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

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;

/**
 * A JUnit test for testing the dataone MetacatPopulator class
 */
public class MetacatPopulatorTest extends MCTestCase 
{   
    /**
    * consstructor for the test
    */
    public MetacatPopulatorTest(String name)
    {
        super(name);
    }
  
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception 
    {
        super.setUp();
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() 
    {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new MetacatPopulatorTest("initialize"));
        suite.addTest(new MetacatPopulatorTest("testPopulate"));
        
        return suite;
    }
    
    public void testPopulate()
    {
        try
        {
            //ids to upload to knb-mn:
            //connolly.301, kgordon.17.27
            MetacatPopulator mp = new MetacatPopulator(
                    "http://knb.ecoinformatics.org/knb",  
                    /*"http://localhost:8080/knb"*/
                    "https://demo1.test.dataone.org/metacat/d1/mn", 
                    //"https://knb-test-1.test.dataone.org/metacat/d1/mn", 
                    /*"msucci"*//*"connolly.301"*/ 
                    "tao.1.1", //"frog", 
                    "uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org", 
                    "kepler");
            mp.populate();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail("Error: " + e.getMessage());
        }
    }
    
    /**
     * init
     */
    public void initialize()
    {
        assertTrue(1==1);
    }
}