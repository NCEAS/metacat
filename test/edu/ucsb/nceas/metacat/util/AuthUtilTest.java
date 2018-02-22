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

import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Pattern;

import com.hp.hpl.jena.sparql.pfunction.library.str;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;

import junit.framework.Test;
import junit.framework.TestSuite;
/**
 * A JUnit test class for the AuthUil class.
 * @author tao
 *
 */
public class AuthUtilTest extends MCTestCase {
    private static String  LDAP = "uid=test,o=NCEAS,dc=ecoinformatics,dc=org";
    private static String ORCID = "http\\://orcid.org/0023-0001-7868-2567\\";
    private static String LIST = LDAP+AuthUtil.DELIMITER+ORCID;
    private static String EXCPECTEDORCID = "http://orcid.org/0023-0001-7868-2567\\";
    private static String ADMIN ="auth.administrators";
    private static String ALLOW = "auth.allowedSubmitters";
    private static String DENEY = "auth.deniedSubmitters";
    private static String MODERATOR = "auth.moderators";


    /**
     * Constructor
     * @param name
     */
    public AuthUtilTest(String name) {
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
        suite.addTest(new AuthUtilTest("initialize"));
        suite.addTest(new AuthUtilTest("testSplit"));
        suite.addTest(new AuthUtilTest("testAllowedSubmitter"));
        suite.addTest(new AuthUtilTest("testAdmin"));
        suite.addTest(new AuthUtilTest("testDeniedSubmitter"));
        suite.addTest(new AuthUtilTest("testModerator"));
        return suite;
    }
    
    /**
     * init
     */
    public void initialize() {
        assertTrue(1==1);
    }
    
    public void testSplit() {
        Vector<String> results = AuthUtil.split(LIST, AuthUtil.DELIMITER, AuthUtil.ESCAPECHAR);
        assertTrue(results.elementAt(0).equals(LDAP));
        assertTrue(results.elementAt(1).equals(EXCPECTEDORCID));
    }
    
    /**
     * Test if the metacat can split the allowed submitters string correctly 
     */
    public void testAllowedSubmitter() throws Exception {
        String originStr = PropertyService.getProperty(ALLOW);
        //System.out.println("====the orginal string is "+originStr);
        PropertyService.setProperty(ALLOW, LIST);
        String newStr = PropertyService.getProperty(ALLOW);
        //System.out.println("====the new string is "+newStr);
        Vector<String> results = AuthUtil.getAllowedSubmitters();
        assertTrue(results.elementAt(0).equals(LDAP));
        assertTrue(results.elementAt(1).equals(EXCPECTEDORCID));
        System.out.println("=======the orcid id is "+results.elementAt(1));
        
        
        //set back the original value
        PropertyService.setProperty(ALLOW, originStr);
        String str = PropertyService.getProperty(ALLOW);
        //System.out.println("====the final string is "+str);
    }
    
    /**
     * Test if the metacat can split the denied submitters string correctly 
     */
    public void testDeniedSubmitter() throws Exception {
        String originStr = PropertyService.getProperty(DENEY);
        //System.out.println("====the orginal string is "+originStr);
        PropertyService.setProperty(DENEY, LIST);
        String newStr = PropertyService.getProperty(DENEY);
        //System.out.println("====the new string is "+newStr);
        Vector<String> results = AuthUtil.getDeniedSubmitters();
        assertTrue(results.elementAt(0).equals(LDAP));
        assertTrue(results.elementAt(1).equals(EXCPECTEDORCID));
        
        
        //set back the original value
        PropertyService.setProperty(DENEY, originStr);
        String str = PropertyService.getProperty(DENEY);
        //System.out.println("====the final string is "+str);
    }
    
    /**
     * Test if the metacat can split the moderator string correctly 
     */
    public void testModerator() throws Exception {
        String originStr = PropertyService.getProperty(MODERATOR);
        //System.out.println("====the orginal string is "+originStr);
        PropertyService.setProperty(MODERATOR, LIST);
        String newStr = PropertyService.getProperty(MODERATOR);
        //System.out.println("====the new string is "+newStr);
        Vector<String> results = AuthUtil.getModerators();
        assertTrue(results.elementAt(0).equals(LDAP));
        assertTrue(results.elementAt(1).equals(EXCPECTEDORCID));
        
        
        //set back the original value
        PropertyService.setProperty(MODERATOR, originStr);
        String str = PropertyService.getProperty(MODERATOR);
        //System.out.println("====the final string is "+str);
    }
    
    /**
     * Test if the metacat can split the admin string correctly 
     */
    public void testAdmin() throws Exception {
        String originStr = PropertyService.getProperty(ADMIN);
        //System.out.println("====the orginal string is "+originStr);
        PropertyService.setProperty(ADMIN, LIST);
        String newStr = PropertyService.getProperty(ADMIN);
        //System.out.println("====the new string is "+newStr);
        Vector<String> results = AuthUtil.getAdministrators();
        assertTrue(results.elementAt(0).equals(LDAP));
        assertTrue(results.elementAt(1).equals(EXCPECTEDORCID));
        
        
        //set back the original value
        PropertyService.setProperty(ADMIN, originStr);
        String str = PropertyService.getProperty(ADMIN);
        //System.out.println("====the final string is "+str);
    }
    
   
}
