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
    private static final String USERNAME = "uid=tao,o=NCEAS,dc=ecoinformatics,dc=org";
    private static final String PASSWORD = "ecoinformatics";
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
         suite.addTest(new AuthFileTest("testAddGroup"));
         suite.addTest(new AuthFileTest("testAddUser"));
         suite.addTest(new AuthFileTest("testAuthenticate"));
         suite.addTest(new AuthFileTest("testGetUsers"));
         suite.addTest(new AuthFileTest("testGetGroups"));
         suite.addTest(new AuthFileTest("testChangePassword"));
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
     
     /**
      * Test the addGroup method
      * @throws Exception
      */
     public void testAddUser() throws Exception{
         AuthFile authFile = AuthFile.getInstance(PASSWORDFILEPATH);
         String[]groups = {GROUPNAME};
         authFile.addUser(USERNAME, groups, PASSWORD);
         try {
             authFile.addUser(USERNAME, groups, PASSWORD);
             assertTrue("We can't reach here since we can't add the user twice", false);
         } catch (AuthenticationException e) {
             
         }
         
     }
     
     /**
      * Test the authentication method
      * @throws Exception
      */
     public void testAuthenticate() throws Exception {
         AuthFile authFile = AuthFile.getInstance(PASSWORDFILEPATH);
         boolean success = authFile.authenticate(USERNAME, PASSWORD);
         if(!success) {
             assertTrue("The authentication should succeed.", false);
         }
         success = authFile.authenticate(USERNAME, "hello");
         if(success) {
             assertTrue("The authentication should NOT succeed.", false);
         }
         success = authFile.authenticate("hello", PASSWORD);
         if(success) {
             assertTrue("The authentication should NOT succeed.", false);
         }
     }
     
     /**
      * Test the getUsers method
      * @throws Exception
      */
     public void testGetUsers() throws Exception {
         AuthFile authFile = AuthFile.getInstance(PASSWORDFILEPATH);
         String[][] users = authFile.getUsers(null, null);
         assertTrue("The file should have one user "+USERNAME, users[0][0].equals(USERNAME));
         String[]userInGroup = authFile.getUsers(null, null, GROUPNAME);
         assertTrue("There should be at least one user in the group "+GROUPNAME, userInGroup[0].equals(USERNAME));
         userInGroup = authFile.getUsers(null, null, "group1");
         assertTrue("There shouldn't have any users in the group1 ", userInGroup==null);
     }
     
     /**
      * Test the getGroups method
      * @throws Exception
      */
     public void testGetGroups() throws Exception {
         AuthFile authFile = AuthFile.getInstance(PASSWORDFILEPATH);
         String[][] groups = authFile.getGroups(null, null);
         assertTrue("The file should have one group associated with "+USERNAME, groups[0][0].equals(GROUPNAME));
         String[][]groupForUser = authFile.getGroups(null, null, USERNAME);
         assertTrue("There should be at least one group for user "+USERNAME, groupForUser[0][0].equals(GROUPNAME));
         groupForUser = authFile.getGroups(null, null, "user1");
         assertTrue("There shouldn't have any groups assoicated with user1 ", groupForUser==null);
     }
     
     /**
      * Test the change password methods
      * @throws Exception
      */
     public void testChangePassword() throws Exception {
         AuthFile authFile = AuthFile.getInstance(PASSWORDFILEPATH);
         String password = authFile.resetPassword(USERNAME);
         String newPassword = "hello";
         authFile.modifyPassword(USERNAME, password, newPassword);
         
         try {
             authFile.resetPassword("user1");
             assertTrue("Can't reach here since we tried to reset the password for an unexisting user ", false);
         } catch (AuthenticationException e) {
             
         }
         try {
             authFile.modifyPassword("user1", "old", "new");
             assertTrue("Can't reach here since we tried to change the password for an unexisting user ", false);
         } catch (AuthenticationException e) {
             
         }
     }
}
