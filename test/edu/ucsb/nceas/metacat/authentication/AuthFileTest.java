package edu.ucsb.nceas.metacat.authentication;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;


public class AuthFileTest extends MCTestCase {
    private static final String PASSWORDFILEPATH = "build/password";//please don't change the password file location to a production one since it will be deleted.
    private static final String GROUPNAME = "nceas-dev";
    private static final String DESCRIPITION = "Developers at NCEAS";
    private static final String GROUPNAME2 = "dataone-dev";
    private static final String DESCRIPITION2 = null;
    private static final String GROUPNAME3 = "dev";
    private static final String DESCRIPITION3 = "Developers";
    private static final String USERNAME = "uid=john,o=NCEAS,dc=ecoinformatics,dc=org";
    private static final String USERNAME2="uid=smith,o=unaffiliated,dc=ecoinformatics,dc=org";
    private static final String PLAINPASSWORD = "ecoinformatics";
    private static final String PLAINPASSWORD2 = "n%cea4s";
    private static final String HASHEDPASSWORD2 = "$2a$10$iMZXvVYs8nEUAWDFfcCF8ePEvzcnak32tx7TQAecsZcPGRouqSdse";
    private static final String PLAINPASSWORD3 = "q8w*er";
    private static final String HASHEDPASSWORD3 = "$2a$10$zO4Cw1p38xWeUh4DneMGCecg67yo2SN25m0wzWCJ9zu7FfRwLTvue";
    private static final String EMAILADDRESS = "john@nceas.ucsb.edu";
    private static final String SURNAME = "John";
    private static final String GIVENNAME = "Joe";
    private static final String ORGANIZATIONNAME = "NCEAS";
    /**
     * consstructor for the test
     */
     public AuthFileTest(String name)
     {
         super(name);
         
         // clear the test password file for subsequent runs
         File pwFile = new File(PASSWORDFILEPATH);
         if (pwFile.exists()) {
        	 pwFile.delete();
         }
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
         suite.addTest(new AuthFileTest("testChangePassword"));
         suite.addTest(new AuthFileTest("testGetUserInfo"));
         suite.addTest(new AuthFileTest("testGetUsers"));
         suite.addTest(new AuthFileTest("testGetGroups"));
         suite.addTest(new AuthFileTest("testAddRemoveUserToFromGroup"));
         suite.addTest(new AuthFileTest("testGetPrincipals"));
         return suite;
     }
     
     /**
      * Test the addGroup method
      * @throws Exception
      */
     public void testAddGroup() throws Exception{
         AuthFile authFile = new AuthFile(PASSWORDFILEPATH);
         authFile.addGroup(GROUPNAME, DESCRIPITION);
         try {
             authFile.addGroup(GROUPNAME, "Developers at NCEAS");
             assertTrue("We can't reach here since we can't add the group twice", false);
         } catch (AuthenticationException e) {
             
         }
         
     }
     
     /**
      * Test the addGroup method
      * @throws Exception
      */
     public void testAddUser() throws Exception{
         AuthFile authFile = new AuthFile(PASSWORDFILEPATH);
         String[]groups = {GROUPNAME};
         authFile.addUser(USERNAME, groups, PLAINPASSWORD, null, EMAILADDRESS, SURNAME, GIVENNAME, ORGANIZATIONNAME);
         //user a hash value of the PASSWORD
         authFile.addUser(USERNAME2, null, null, HASHEDPASSWORD2, null, null,null, null);
         try {
             authFile.addUser(USERNAME, groups, PLAINPASSWORD, null, null, null, null, null);
             assertTrue("We can't reach here since we can't add the user twice", false);
         } catch (AuthenticationException e) {
             
         }
         
     }
     
     /**
      * Test the authentication method
      * @throws Exception
      */
     public void testAuthenticate() throws Exception {
         AuthFile authFile = new AuthFile(PASSWORDFILEPATH);
         boolean success = authFile.authenticate(USERNAME, PLAINPASSWORD);
         if(!success) {
             assertTrue("The authentication should succeed.", false);
         }
         success = authFile.authenticate(USERNAME, "hello");
         if(success) {
             assertTrue("The authentication should NOT succeed.", false);
         }
         success = authFile.authenticate("hello", PLAINPASSWORD);
         if(success) {
             assertTrue("The authentication should NOT succeed.", false);
         }
         success = authFile.authenticate(USERNAME2, PLAINPASSWORD2);
         if(!success) {
             assertTrue("The authentication for "+USERNAME2 +" should succeed.", false);
         }
         success = authFile.authenticate(USERNAME2, HASHEDPASSWORD2);
         if(success) {
             assertTrue("The authentication should NOT succeed.", false);
         }
     }
     
     /**
      * Test the method getUserInfo
      * @throws Exception
      */
     public void testGetUserInfo() throws Exception {
         AuthFile authFile = new AuthFile(PASSWORDFILEPATH);
         String[] userInfo = authFile.getUserInfo(USERNAME, null);
         assertTrue("The common name for the user "+USERNAME+" should be "+GIVENNAME+" "+SURNAME, userInfo[0].equals(GIVENNAME+" "+SURNAME));
         assertTrue("The org name for the user "+USERNAME+" should be "+ORGANIZATIONNAME, userInfo[1].equals(ORGANIZATIONNAME));
         assertTrue("The email address for the user "+USERNAME+" should be "+EMAILADDRESS, userInfo[2].equals(EMAILADDRESS));
         userInfo = authFile.getUserInfo(USERNAME2, null);
         assertTrue("The common name for the user "+USERNAME2+" should be null.", userInfo[0] == null);
         assertTrue("The org name for the user "+USERNAME+" should be null.", userInfo[1]== null);
         assertTrue("The email address for the user "+USERNAME+" should be null.", userInfo[2]==null);
     }
     
     /**
      * Test the getUsers method
      * @throws Exception
      */
     public void testGetUsers() throws Exception {
         AuthFile authFile = new AuthFile(PASSWORDFILEPATH);
         String[][] users = authFile.getUsers(null, null);
         assertTrue("The file should have one user "+USERNAME, users[0][0].equals(USERNAME));
         assertTrue("The common name for the user "+USERNAME+" should be "+GIVENNAME+" "+SURNAME, users[0][1].equals(GIVENNAME+" "+SURNAME));
         assertTrue("The org name for the user "+USERNAME+" should be "+ORGANIZATIONNAME, users[0][2].equals(ORGANIZATIONNAME));
         assertTrue("The org unit name for the user "+USERNAME+" should be null ", users[0][3]== null);
         assertTrue("The email address for the user "+USERNAME+" should be "+EMAILADDRESS, users[0][4].equals(EMAILADDRESS));
         assertTrue("The file should have one user "+USERNAME2, users[1][0].equals(USERNAME2));
         assertTrue("The common name for the user "+USERNAME2+" should be null", users[1][1]==null);
         assertTrue("The org name for the user "+USERNAME2+" should be null ", users[1][2]== null);
         assertTrue("The org unit name for the user "+USERNAME2+" should be null ", users[1][3]== null);
         assertTrue("The email address for the user "+USERNAME2+" should be null.", users[1][4]==null);
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
         AuthFile authFile = new AuthFile(PASSWORDFILEPATH);
         String[][] groups = authFile.getGroups(null, null);
         assertTrue("The file should have one group associated with "+USERNAME, groups[0][0].equals(GROUPNAME));
         assertTrue("The group "+groups[0][0]+" should have the description "+DESCRIPITION, groups[0][1].equals(DESCRIPITION));
         String[][]groupForUser = authFile.getGroups(null, null, USERNAME);
         assertTrue("There should be at least one group for user "+USERNAME, groupForUser[0][0].equals(GROUPNAME));
         assertTrue("The group "+groups[0][0]+" should have the description "+DESCRIPITION, groups[0][1].equals(DESCRIPITION));
         groupForUser = authFile.getGroups(null, null, "user1");
         assertTrue("There shouldn't have any groups assoicated with user1 ", groupForUser==null);
         groupForUser = authFile.getGroups(null, null, USERNAME2);
         assertTrue("There shouldn't have any groups assoicated with user "+USERNAME2, groupForUser==null);
     }
     
     /**
      * Test the change password methods
      * @throws Exception
      */
     public void testChangePassword() throws Exception {
         AuthFile authFile = new AuthFile(PASSWORDFILEPATH);
         authFile.authenticate(USERNAME, PLAINPASSWORD);
         String newPassword = "hello";
         authFile.modifyPassWithPlain(USERNAME,newPassword);
         boolean success = authFile.authenticate(USERNAME, newPassword);
         assertTrue("The authentication should be successful with the new password", success);
         try {
             authFile.modifyPassWithPlain("user1", "new");
             assertTrue("Can't reach here since we tried to change the password for an unexisting user ", false);
         } catch (AuthenticationException e) {
             //System.out.println("Failed to change the password for a user: "+e.getMessage());
         }
         
         success = authFile.authenticate(USERNAME, "qws");
         assertTrue("The authentication should fail with a wrong password", !success);
         
         //test change the password with hashed version
         authFile.modifyPassWithHash(USERNAME, HASHEDPASSWORD3);
         success = authFile.authenticate(USERNAME, PLAINPASSWORD3);
         assertTrue("The authentication should be successful with the new password (after modifying the password with a hashed value", success);
         success = authFile.authenticate(USERNAME, HASHEDPASSWORD3);
         assertTrue("The authentication should faile when the user directly use the hash password.", !success);
         success = authFile.authenticate(USERNAME, newPassword);
         assertTrue("The authentication should be successful with a wrong password", !success);
     }
     
     /**
      * Test the addUserToGroup and removeUserFromGroup methods
      * @throws Exception
      */
     public void testAddRemoveUserToFromGroup() throws Exception{
         AuthFile authFile = new AuthFile(PASSWORDFILEPATH);
         try {
             authFile.addUserToGroup("user1", GROUPNAME);
             assertTrue("Can't reach here since we tried to add an unexisting user to a group", false);
         } catch(AuthenticationException e) {
             System.out.println("Failed to add a user to a group "+e.getMessage());
         }
         
         try {
             authFile.addUserToGroup(USERNAME, "group2");
             assertTrue("Can't reach here since we tried to add a user to an unexisting group", false);
         } catch(AuthenticationException e) {
             System.out.println("Failed to add a user to a group "+e.getMessage());
         }
         try {
             authFile.addUserToGroup(USERNAME, GROUPNAME);
             assertTrue("Can't reach here since the user is already in the group", false);
         } catch(AuthenticationException e) {
             System.out.println("Failed to add a user to a group "+e.getMessage());
         }
         authFile.addGroup(GROUPNAME2, null);
         authFile.addUserToGroup(USERNAME, GROUPNAME2);
         String[][]groups = authFile.getGroups(null, null, USERNAME);
         assertTrue("The user "+USERNAME+" should be in the group "+GROUPNAME2, groups[0][0].equals(GROUPNAME2)||groups[1][0].equals(GROUPNAME2));
         if(groups[0][0].equals(GROUPNAME2)) {
             assertTrue("The description of the group "+GROUPNAME2+" should be null.", groups[0][1]==null);
         }
         if(groups[1][0].equals(GROUPNAME2)) {
             assertTrue("The description of the group "+GROUPNAME2+" should be null.", groups[1][1]==null);
         }
         
         
         try {
             authFile.removeUserFromGroup("user1", GROUPNAME);
             assertTrue("Can't reach here since we tried to remove an unexisting user from a group", false);
         } catch(AuthenticationException e) {
             System.out.println("Failed to remove a user from a group "+e.getMessage());
         }
         
         try {
             authFile.removeUserFromGroup(USERNAME, "group2");
             assertTrue("Can't reach here since we tried to remove a user from an unexisting group", false);
         } catch(AuthenticationException e) {
             System.out.println("Failed to remove a user from a group "+e.getMessage());
         }
         authFile.addGroup(GROUPNAME3, DESCRIPITION3);
         try {
             authFile.removeUserFromGroup(USERNAME, GROUPNAME3);
             assertTrue("Can't reach here since the user is not in the group", false);
         } catch(AuthenticationException e) {
             System.out.println("Failed to remove a user from a group "+e.getMessage());
         }
         authFile.removeUserFromGroup(USERNAME, GROUPNAME2);
         groups = authFile.getGroups(null, null, USERNAME);
         assertTrue("The size of groups of the user "+USERNAME+" should be one rather than "+groups.length, groups.length ==1);
         assertTrue("The user "+USERNAME+" shouldn't be in the group "+GROUPNAME2, !groups[0][0].equals(GROUPNAME2));
         assertTrue("The user "+USERNAME+" should still be in the group "+GROUPNAME, groups[0][0].equals(GROUPNAME));
         assertTrue("The group "+groups[0][0]+" should have the description "+DESCRIPITION, groups[0][1].equals(DESCRIPITION));
         
         authFile.addUserToGroup(USERNAME2, GROUPNAME3);
         groups = authFile.getGroups(null, null, USERNAME2);
         assertTrue("The user "+USERNAME2+" should be in the group "+GROUPNAME3, groups[0][0].equals(GROUPNAME3));
         assertTrue("The group "+groups[0][0]+" should have the description "+DESCRIPITION3, groups[0][1].equals(DESCRIPITION3));
         String[] users = authFile.getUsers(null, null, GROUPNAME3);
         assertTrue("The user "+USERNAME2+" should be a member of the group "+GROUPNAME3, users[0].equals(USERNAME2));
         try {
             authFile.removeUserFromGroup(USERNAME2, GROUPNAME);
             assertTrue("We can't reach here since the user "+USERNAME2+" is not in the group "+GROUPNAME, false);
         } catch (Exception e) {
             
         }
         
     }
     
     /**
      * Test the getPrincipal
      * @throws Exception
      */
     public void testGetPrincipals() throws Exception {
         AuthFile authFile = new AuthFile(PASSWORDFILEPATH);
         System.out.println(""+authFile.getPrincipals(null, null));
     }
}
