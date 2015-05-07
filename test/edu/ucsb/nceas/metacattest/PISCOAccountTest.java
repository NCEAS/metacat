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

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * The reason to add this specific test for a PISCO account is some network env
 * maybe fails since the PISCO ldap server have a fire wall. It seem the machines
 * in NCEAS work.
 * @author tao
 *
 */
public class PISCOAccountTest extends MetaCatServletTest{
    /**
     * Constructor to build the test
     * 
     * @param name
     *            the name of the test method
     */
    public PISCOAccountTest(String name) {
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
        suite.addTest(new PISCOAccountTest("testPiscoReferralLogin"));
        suite.addTest(new PISCOAccountTest("testPiscoReferralLoginFail"));
        return suite;
    }
    
    /**
     * Test the login to Other succesfully
     */
    public void testPiscoReferralLogin() {
        debug("\nRunning: testPiscoReferralLogin test");
        String user = piscouser;
        String passwd = piscopassword;
        debug("logging in pisco user: " + user + ":" + passwd);
        assertTrue(logIn(user, passwd));
        // assertTrue( withProtocol.getProtocol().equals("http"));
    }

    /**
     * Test the login to Other failed
     */
    public void testPiscoReferralLoginFail() {
        debug("\nRunning: testPiscoReferralLoginFail test");
        String user = piscouser;
        String passwd = "wrong";
        assertTrue(!logIn(user, passwd));
        // assertTrue( withProtocol.getProtocol().equals("http"));
    }
}
