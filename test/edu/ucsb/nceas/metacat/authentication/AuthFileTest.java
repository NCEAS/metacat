/**  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.authentication;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;


public class AuthFileTest extends MCTestCase {
    private static final String PASSWORDFILEPATH = "build/password";
    private static final String GROUPNAME = "nceas-dev";
    /**
     * consstructor for the test
     */
     public AuthFileTest(String name)
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
         //suite.addTest(new AuthFileTest(""));
         suite.addTest(new AuthFileTest("testAddGroup"));
         return suite;
     }
     
     /**
      * Test the addGroup method
      * @throws Exception
      */
     public void testAddGroup() throws Exception{
         AuthFile authFile = AuthFile.getInstance(PASSWORDFILEPATH);
         authFile.addGroup(GROUPNAME);
         try {
             authFile.addGroup(GROUPNAME);
             assertTrue("We can't reach here since we can't add the group twice", false);
         } catch (AuthenticationException e) {
             
         }
         
     }
}
