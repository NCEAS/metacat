/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the MetaCatURL class by JUnit
 *    Authors: Jing Tao
 *
 *   '$Author: tao $'
 *     '$Date: 2014-03-04 09:48:15 -0800 (Tue, 04 Mar 2014) $'
 * '$Revision: 8695 $'
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

import edu.ucsb.nceas.metacat.AuthLdap;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * The reason to add this specific test for a LTER account is LTET ldap server is not working right
 * now. We centralize all tests of LTER acounts here in order to simplify the management
 * @author tao
 *
 */
public class LTERAccountTest extends MetaCatServletTest{
    /**
     * Constructor to build the test
     * 
     * @param name
     *            the name of the test method
     */
    public LTERAccountTest(String name) {
        super(name);
       
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() {
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
    public static Test suite() {
        double number = 0;
       
        TestSuite suite = new TestSuite();
        suite.addTest(new LTERAccountTest("testGetPrincipals"));
        suite.addTest(new LTERAccountTest("testLterReferralLogin"));
        suite.addTest(new LTERAccountTest("testLterReferralLoginFail"));
        return suite;
    }
    
    public void testGetPrincipals()
    {
        try
        {
            String lterUser = PropertyService.getProperty("test.lterUser");
            //System.out.println("before initilizing authldap object");
            AuthLdap ldap = new AuthLdap();
            //System.out.println("after initilizing authldap object");
            
            //System.out.println("before calling the getPrincipals method");
            String response = ldap.getPrincipals(username, password);
            //System.out.println("after calling the getPrincipals method \n"+response);
            if ( response != null)
            {
               //assertTrue("Couldn't find user "+anotheruser,response.indexOf(anotheruser) != -1);
               assertTrue("Couldn't find user "+lterUser,response.indexOf(lterUser) != -1);
            }
            else
            {
                fail("the response is null in getPrincipal method");
            }
        }
        catch (Exception e)
        {
            //System.out.println("Error to get principals "+e.getMessage());
            fail("There is an exception in getPrincipals "+e.getMessage());
        }
        
    }
    
    /**
     * Test the login to lter succesfully
     */
    public void testLterReferralLogin() {
        debug("\nRunning: testLterReferralLogin test");
        String user = null;
        String passwd = null;
        try {
            user = PropertyService.getProperty("test.lterUser");
            passwd = PropertyService.getProperty("test.lterPassword");
        } catch (PropertyNotFoundException pnfe) {
            fail("Could not find property: " + pnfe.getMessage());
        }

        debug("Logging into lter: " + user + " : " + passwd);
        assertTrue(logIn(user, passwd));
        this.testLogOut();


    }

    /**
     * Test the login to lter failed
     */
    public void testLterReferralLoginFail() {
        debug("\nRunning: testLterReferralLoginFail test");
        String user = null;
    String passwd = "wrong";
    try {
      user = PropertyService.getProperty("test.lterUser");
    } catch (PropertyNotFoundException pnfe) {
      fail("Could not find property: " + pnfe.getMessage());
    }
        assertTrue(!logIn(user, passwd));
        // assertTrue( withProtocol.getProtocol().equals("http"));
        this.testLogOut();

    }
}
