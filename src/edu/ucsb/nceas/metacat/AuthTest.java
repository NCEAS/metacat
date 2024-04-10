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
public class AuthTest implements AuthInterface {
	private String authUrl = "";
	private String testUser = "test-user";
	private String testUserName = "Test User";
	private String testPassword = "test-password";
	private String testGroup = "test-group";
	private String testGroupDesc = "this is a test group";
	private String testOrg = "NCEAS";
	private String testOrgUnit = "UCSB";
	private String testEmail = "test-user@dummy.email.com";
	private String otherTestUser = "other-test-user";
	private String otherTestUserName = "Other Test User";
	private String otherTestGroup = "other-test-group";
	private String otherTestGroupDesc = "this is a another test group";
	private String otherTestOrg = "DUMMY";
	private String otherTestOrgUnit = "UCLA";
	private String otherTestEmail = "other-test-user@dummy.email.com";
	private String attributeName = "attribute-name";
	private String attributeValue1 = "attribute-value1";
	private String attributeValue2 = "attribute-value2";
	

  private static Log logMetacat = LogFactory.getLog(AuthTest.class);
  
    /**
	 * Construct an AuthTest
	 */
	public AuthTest() throws InstantiationException {	
		try {
			authUrl = PropertyService.getProperty("auth.url");
		}
			catch (PropertyNotFoundException pnfe) {
				throw new InstantiationException(
						"Could not instantiate AuthTest.  Property not found: "
								+ pnfe.getMessage());
			}
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
    if (user.equals(testUser) && password.equals(testPassword)) {
    	logMetacat.debug(user + " is authenticated");
    	return true;
    }
    
    logMetacat.debug(user + " could not be authenticated");
    return false;
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
    
    users[0][0] = testUser;
    
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

    userinfo[0] = testUser;
    userinfo[1] = testOrg;
    userinfo[2] = testEmail;

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

    users[0] = testUser;

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
    
    if (user.equals(testUser) && password.equals(testPassword)) {
    	if (foruser != null) {
    		groups[0][1] = testGroup;
    		groups[0][1] = testGroupDesc;
    	} else if (foruser.equals(otherTestUser)) {
    		groups[0][0] = otherTestGroup;
    		groups[0][1] = otherTestGroupDesc;
    	}
    }

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
    
    if (foruser.equals(otherTestUser)) {
    	Vector<String> attributeValues = new Vector<String>();
    	attributeValues.add(attributeValue1);
    	attributeValues.add(attributeValue2);
    	
    	attributes.put(attributeName, attributeValues);
    }

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
    out += "  <authSystem URI=\"" + authUrl +">\n";
    out += "    <group>\n";
    out += "      <groupname>" + testGroup + "</groupname>\n";
    out += "      <description>" + testGroupDesc + "</description>\n";
    out += "      <user>\n";
    out += "        <username>" + testUser +"</username>\n";
    out += "        <name>" + testUserName +"</name>\n";
    out += "        <organization>" + testOrg + "</organization>\n";
    out += "        <organizationUnitName>" + testOrgUnit + "</organizationUnitName>\n";
    out += "        <email>" + testEmail + "</email>\n";
    out += "      </user>\n";
    out += "    </group>\n";
    out += "    <group>\n";
    out += "      <groupname>" + otherTestGroup + "</groupname>\n";
    out += "      <description>" + otherTestGroupDesc + "</description>\n";
    out += "      <user>\n";
    out += "        <username>" + otherTestUser +"</username>\n";
    out += "        <name>" + otherTestUserName +"</name>\n";
    out += "        <organization>" + otherTestOrg + "</organization>\n";
    out += "        <organizationUnitName>" + otherTestOrgUnit + "</organizationUnitName>\n";
    out += "        <email>" + otherTestEmail + "</email>\n";
    out += "      </user>\n";
    out += "    </group>\n";
    out += "  </authSystem>\n";
    out += "</principals>";
    
    return out;
  }
}