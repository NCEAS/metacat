package edu.ucsb.nceas.metacat;

import java.net.ConnectException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import java.lang.InstantiationException;
import java.util.HashMap;
import java.util.Vector;

/**
 * An implementation of the AuthInterface interface that
 * allows Metacat to use the LDAP protocol for directory services.
 * The LDAP authentication service is used to determine if a user
 * is authenticated, and whether they are a member of a particular group.
 */
public class AuthStub implements AuthInterface {
	

  private static Log logMetacat = LogFactory.getLog(AuthTest.class);
  
    /**
	 * Construct an AuthTest
	 */
	public AuthStub() throws InstantiationException {	
	}

  /**
	 * Determine if a user/password are valid according to the authentication
	 * service.
	 * 
	 * @param user
	 *            the name of the principal to authenticate
	 * @param password
	 *            the password to use for authentication
	 * @returns boolean true if authentication successful, false otherwise
	 */
  
  public boolean authenticate(String user, String password) throws ConnectException {
	  return true;
  }
  
  /**
   * Get all users from the authentication service
   *
   * @param user the user for authenticating against the service
   * @param password the password for authenticating against the service
   * @returns string array of all of the user names
   */
  public String[][] getUsers(String user, String password) throws 
    ConnectException {
    String[][] users = new String[1][1];
    
    users[0][0] = "bogusUser";
    
    return users;
  }

  
  /**
   * Get all users from the authentication service
   *
   * @param user the user for authenticating against the service
   * @param password the password for authenticating against the service
   * @returns string array of all of the user names
   */
  public String[] getUserInfo(String user, String password) throws 
    ConnectException {
    String[] userinfo = new String[3];

    userinfo[0] = "bogusUser";
    userinfo[1] = "bogusOrg";
    userinfo[2] = "bogusEmail";

    return userinfo;
  }

  /**
   * Get the users for a particular group from the authentication service
   *
   * @param user the user for authenticating against the service
   * @param password the password for authenticating against the service
   * @param group the group whose user list should be returned
   * @returns string array of the user names belonging to the group
   */
  public String[] getUsers(String user, String password, String group) throws 
    ConnectException {
    String[] users = null;

    users[0] = "bogusUser";

    return users;
  }

  /**
   * Get all groups from the authentication service
   *
   * @param user the user for authenticating against the service
   * @param password the password for authenticating against the service
   * @returns string array of the group names
   */
  public String[][] getGroups(String user, String password) throws 
    ConnectException {
    return getGroups(user, password, null);
  }

  /**
   * Get the groups for a particular user from the authentication service
   *
   * @param user the user for authenticating against the service
   * @param password the password for authenticating against the service
   * @param foruser the user whose group list should be returned
   * @returns string array of the group names
   */
  public String[][] getGroups(String user, String password, 
    String foruser) throws ConnectException {
    
    //build and return the groups array
    String groups[][] = new String[1][2];
    
    groups[0][1] = "bogusGroup";
    groups[0][1] = "bogusGroupDesc";

    return groups;

  }

  /**
   * Get attributes describing a user or group
   *
   * @param foruser the user for which the attribute list is requested
   * @returns HashMap a map of attribute name to a Vector of values
   */
  public HashMap<String,Vector<String>> getAttributes(String foruser) throws ConnectException {
    return getAttributes(null, null, foruser);
  }

  /**
   * Get attributes describing a user or group
   *
   * @param user the user for authenticating against the service
   * @param password the password for authenticating against the service
   * @param foruser the user whose attributes should be returned
   * @returns HashMap a map of attribute name to a Vector of values
   */
  public HashMap<String,Vector<String>> getAttributes(String user, String password, 
    String foruser) throws ConnectException {
    HashMap<String,Vector<String>> attributes = new HashMap<String,Vector<String>>();
    
    Vector<String> attributeValues = new Vector<String>();
    attributeValues.add("bogusValue1");
    attributeValues.add("bogusValue2");
    	
    attributes.put("bogusAttributeName", attributeValues);

    return attributes;
  }

  /**
   * Get all groups and users from authentication scheme.
   * The output is formatted in XML.
   * @param user the user which requests the information
   * @param password the user's password
   */
  public String getPrincipals(String user, String password) throws 
    ConnectException {
    String out = new String();
   
    out += "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n";
    out += "<principals>\n";
    out += "  <authSystem URI=\"bogusAuthUrl>\n";
    out += "    <group>\n";
    out += "      <groupname>bogusTestGroup</groupname>\n";
    out += "      <description>bogusTestGroupDesc</description>\n";
    out += "      <user>\n";
    out += "        <username>bogusTestUser</username>\n";
    out += "        <name>bogusTestUserName</name>\n";
    out += "        <organization>bogusTestOrg</organization>\n";
    out += "        <organizationUnitName>bogusTestOrgUnit</organizationUnitName>\n";
    out += "        <email>bogusTestEmail</email>\n";
    out += "      </user>\n";
    out += "    </group>\n";
    out += "    <group>\n";
    out += "      <groupname>bogusOtherTestGroup</groupname>\n";
    out += "      <description>bogusOtherTestGroupDesc</description>\n";
    out += "      <user>\n";
    out += "        <username>bogusOtherTestUser</username>\n";
    out += "        <name>bogusOtherTestUserName</name>\n";
    out += "        <organization>bogusOtherTestOrg</organization>\n";
    out += "        <organizationUnitName>bogusOtherTestOrgUnit</organizationUnitName>\n";
    out += "        <email>bogusOtherTestEmail</email>\n";
    out += "      </user>\n";
    out += "    </group>\n";
    out += "  </authSystem>\n";
    out += "</principals>";
    
    return out;
  }
}