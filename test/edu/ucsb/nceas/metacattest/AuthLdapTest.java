/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements a metadata catalog as a java Servlet
 *  Copyright: 2006 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones, Dan Higgins, Jivka Bojilova, Chad Berkley, Matthew Perry
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
package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;

import edu.ucsb.nceas.metacat.AuthLdap;
import junit.framework.Test;
import junit.framework.TestSuite;


/*
 * A junit test class to test public methods in AuthLdap class.
 */
public class AuthLdapTest extends MCTestCase
{
	
	/**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public AuthLdapTest(String name)
    {
        super(name);
        
    }
	
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new AuthLdapTest("initialize"));
        suite.addTest(new AuthLdapTest("getPrincipals"));
        suite.addTest(new AuthLdapTest("testAliasedAccount"));
        return suite;
    }

    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize()
    {
    	//System.out.println("here");
        assertTrue(1 == 1);
    }
    
    public void getPrincipals()
    {
    	try
    	{
    	    //String lterUser = PropertyService.getProperty("test.lterUser");
        	//System.out.println("before initilizing authldap object");
        	AuthLdap ldap = new AuthLdap();
        	//System.out.println("after initilizing authldap object");
    		
    		//System.out.println("before calling the getPrincipals method");
    	    String response = ldap.getPrincipals(username, password);
    	    //System.out.println("after calling the getPrincipals method \n"+response);
    	    if ( response != null)
    	    {
    	       assertTrue("Couldn't find user "+anotheruser,response.indexOf(anotheruser) != -1);
    	       //assertTrue("Couldn't find user "+lterUser,response.indexOf(lterUser) != -1);
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
     * To Do: add more methods test
     */
    
    public void testAliasedAccount() throws Exception {
        String alias = "uid=test2,o=unaffiliated,dc=ecoinformatics,dc=org";
        AuthLdap ldap = new AuthLdap();
        assertTrue("We should authenticate the alias dn "+alias,ldap.authenticate(alias, "kepler"));
        String[] info =ldap.getUserInfo(alias, null);
        assertTrue("The email address should be tao@nceas.ucsb.edu and should be "+info[2], info[2].equals("tao@nceas.ucsb.edu"));
    }

}
