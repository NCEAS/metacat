/**
 *  '$RCSfile$'
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.security.GeneralSecurityException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.AuthInterface;
import edu.ucsb.nceas.metacat.AuthLdap;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * This an authentication class base on a username/password file.
 * It is an alternative authentication mechanism of the ldap authentication.
 * This is a singleton class and the password file looks like:
 *<?xml version="1.0" encoding="UTF-8" ?>
 * <subjects>
 *  <users>
 *      <user dn="uid=tao,o=NCEAS,dc=ecoinformatics,dc=org">
 *          <password>*******</password>
 *          <email>foo@foo.com</email>
 *          <surName>Smith</surName>
 *          <givenName>John</givenName>
 *          <group>nceas-dev</group>
 *      </user>
 *  </users>
 *  <groups>
 *    <group name="nceas-dev">
 *        <description>developers at NCEAS</description>
 *    </group>
 *  </groups>
 * </subjects>
 * http://commons.apache.org/proper/commons-configuration/userguide/howto_xml.html
 * @author tao
 *
 */
public class AuthFile implements AuthInterface {
    private static final String ORGANIZATIONNAME = "UNkown";
    private static final String ORGANIZATION = "organization";
    private static final String NAME = "name";
    private static final String DN = "dn";
    private static final String DESCRIPTION = "description";
    private static final String PASSWORD = "password";
    private static final String SLASH = "/";
    private static final String AT = "@";
    private static final String SUBJECTS = "subjects";
    private static final String USERS = "users";
    private static final String USER = "user";
    private static final String GROUPS = "groups";
    private static final String GROUP = "group";
    private static final String EMAIL = "email";
    private static final String SURNAME = "surName";
    private static final String GIVENNAME = "givenName";
    private static final String INITCONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"+
                                    "<"+SUBJECTS+">\n"+"<"+USERS+">\n"+"</"+USERS+">\n"+"<"+GROUPS+">\n"+"</"+GROUPS+">\n"+"</"+SUBJECTS+">\n";
   
    
    
    private static Log log = LogFactory.getLog(AuthFile.class);
    private static AuthFile authFile = null;
    private static XMLConfiguration userpassword = null;
    private String authURI = null;
    private static String passwordFilePath = null;
    private static AuthFileHashInterface hashClass = null;
    /**
     * Get the instance of the AuthFile
     * @return
     * @throws AuthenticationException
     */
    public static AuthFile getInstance() throws AuthenticationException {
        if(authFile == null) {
            authFile = new AuthFile();
        }
        return authFile;
    }
    
    /**
     * Get the instance of the AuthFile from specified password file
     * @return
     * @throws AuthenticationException
     */
    public static AuthFile getInstance(String passwordFile) throws AuthenticationException {
        passwordFilePath = passwordFile;
        if(authFile == null) {
            authFile = new AuthFile();
        }
        return authFile;
    }
    
    /**
     * Constructor
     */
    private AuthFile() throws AuthenticationException {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AuthenticationException(e.getMessage());
        }
        
    }
    
    /*
     * Initialize the user/password configuration
     */
    private void init() throws PropertyNotFoundException, IOException, ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        if(passwordFilePath == null) {
            passwordFilePath  = PropertyService.getProperty("auth.file.path");
        }
        File passwordFile = new File(passwordFilePath);
        
        authURI = SystemUtil.getContextURL();
        String hashClassName = PropertyService.getProperty("auth.file.hashClassName");
        Class classDefinition = Class.forName(hashClassName);
        Object object = classDefinition.newInstance();
        hashClass = (AuthFileHashInterface) object;
        
        //if the password file doesn't exist, create a new one and set the initial content
        if(!passwordFile.exists()) {
            File parent = passwordFile.getParentFile();
            if(!parent.exists()) {
                parent.mkdirs();
            }
            passwordFile.createNewFile();
            OutputStreamWriter writer = null;
            FileOutputStream output = null;
            try {
              output = new FileOutputStream(passwordFile);
              writer = new OutputStreamWriter(output, "UTF-8");
              writer.write(INITCONTENT);
            } finally {
              writer.close();
              output.close();
            }
          }
          userpassword = new XMLConfiguration(passwordFile);
          userpassword.setExpressionEngine(new XPathExpressionEngine());
          userpassword.setAutoSave(true);
          userpassword.setDelimiterParsingDisabled(true);
          userpassword.setAttributeSplittingDisabled(true);
    }
    
    @Override
    public boolean authenticate(String user, String password)
                    throws AuthenticationException {
        boolean match = false;
        String passwordRecord = userpassword.getString(USERS+SLASH+USER+"["+AT+DN+"='"+user+"']"+SLASH+PASSWORD);
        if(passwordRecord != null) {
            try {
                match = hashClass.match(password, passwordRecord);
            } catch (Exception e) {
                throw new AuthenticationException(e.getMessage());
            }
            
        }
        return match;
    }
    
    @Override
    /**
     * Get all users. This is two-dimmention array. Each row is a user. The first element of
     * a row is the user name. The second element is common name. The third one is the organization name (null).
     * The fourth one is the organization unit name (null). The fifth one is the email address.
     */
    public String[][] getUsers(String user, String password)
                    throws ConnectException {
        List<Object> users = userpassword.getList(USERS+SLASH+USER+SLASH+AT+DN);
        if(users != null && users.size() > 0) {
            String[][] usersArray = new String[users.size()][5];
            for(int i=0; i<users.size(); i++) {
                
                String dn = (String)users.get(i);
                usersArray[i][AuthInterface.USERDNINDEX] = dn; //dn
                String[] userInfo = getUserInfo(dn, password);
                usersArray[i][AuthInterface.USERCNINDEX] = userInfo[AuthInterface.USERINFOCNINDEX];//common name
                usersArray[i][AuthInterface.USERORGINDEX] = userInfo[AuthInterface.USERINFOORGANIDEX];//organization name. We set null
                usersArray[i][AuthInterface.USERORGUNITINDEX] = null;//organization ou name. We set null.
                usersArray[i][AuthInterface.USEREMAILINDEX] = userInfo[AuthInterface.USERINFOEMAILINDEX];
               
            }
            return usersArray;
        }
        return null;
    }
    
    @Override
    /**
     * Get an array about the user. The first column is the common name, the second column is the organization name.
     * The third column is the email address. It always returns an array. But the elements of the array can be null.
     */
    public String[] getUserInfo(String user, String password)
                    throws ConnectException {
        String[] userinfo = new String[3];
        User aUser = new User();
        aUser.setDN(user);
        String surname = null;
        List<Object> surNames = userpassword.getList(USERS+SLASH+USER+"["+AT+DN+"='"+user+"']"+SLASH+SURNAME);
        if(surNames != null && !surNames.isEmpty()) {
            surname = (String)surNames.get(0);
        }
        aUser.setSurName(surname);
        String givenName = null;
        List<Object> givenNames = userpassword.getList(USERS+SLASH+USER+"["+AT+DN+"='"+user+"']"+SLASH+GIVENNAME);
        if(givenNames != null && !givenNames.isEmpty()) {
            givenName = (String)givenNames.get(0);
        }
        aUser.setGivenName(givenName);
        userinfo[AuthInterface.USERINFOCNINDEX] = aUser.getCn();//common name
        String organization = null;
        List<Object> organizations = userpassword.getList(USERS+SLASH+USER+"["+AT+DN+"='"+user+"']"+SLASH+ORGANIZATION);
        if(organizations != null && !organizations.isEmpty()) {
            organization = (String)organizations.get(0);
        }
        userinfo[AuthInterface.USERINFOORGANIDEX] = organization;//organization name.
        aUser.setOrganization(organization);
        List<Object> emails = userpassword.getList(USERS+SLASH+USER+"["+AT+DN+"='"+user+"']"+SLASH+EMAIL);
        String email = null;
        if(emails != null && !emails.isEmpty() ) {
            email = (String)emails.get(0);
        }
        aUser.setEmail(email);
        userinfo[AuthInterface.USERINFOEMAILINDEX] = email;
        return userinfo;
    }
    
    
    @Override
    /**
     * Get the users for a particular group from the authentication service
     * The null will return if there is no user.
     * @param user
     *            the user for authenticating against the service
     * @param password
     *            the password for authenticating against the service
     * @param group
     *            the group whose user list should be returned
     * @returns string array of the user names belonging to the group
     */
    public String[] getUsers(String user, String password, String group)
                    throws ConnectException {
        List<Object> users = userpassword.getList(USERS+SLASH+USER+"["+GROUP+"='"+group+"']"+SLASH+AT+DN);
        if(users != null && users.size() > 0) {
            String[] usersArray = new String[users.size()];
            for(int i=0; i<users.size(); i++) {
                usersArray[i] = (String) users.get(i);
            }
            return usersArray;
        }
        return null;
    }
    
    @Override
    /**
     * Get all groups from the authentication service. It returns a two dimmension array. Each row is a
     * group. The first column is the group name. The second column is the description. The null will return if no group found.
     */
    public String[][] getGroups(String user, String password)
                    throws ConnectException {
        List<Object> groups = userpassword.getList(GROUPS+SLASH+GROUP+SLASH+AT+NAME);
        if(groups!= null && groups.size() >0) {
            String[][] groupsArray = new String[groups.size()][2];
            for(int i=0; i<groups.size(); i++) {
                String groupName = (String) groups.get(i);
                groupsArray[i][AuthInterface.GROUPNAMEINDEX] = groupName;
                String description = null;
                List<Object>descriptions = userpassword.getList(GROUPS+SLASH+GROUP+"["+AT+NAME+"='"+groupName+"']"+SLASH+DESCRIPTION);
                if(descriptions != null && !descriptions.isEmpty()) {
                    description = (String)descriptions.get(0);
                }
                groupsArray[i][AuthInterface.GROUPDESINDEX] = description; 
            }
            return groupsArray;
        }
        return null;
    }
    
    @Override
    /**
     * Get groups from a specified user. It returns two dimmension array. Each row is a
     * group. The first column is the group name. The null will return if no group found.
     */
    public String[][] getGroups(String user, String password, String foruser)
                    throws ConnectException {
        List<Object> groups = userpassword.getList(USERS+SLASH+USER+"["+AT+DN+"='"+foruser+"']"+SLASH+GROUP);
        if(groups != null && groups.size() > 0) {
            String[][] groupsArray = new String[groups.size()][2];
            for(int i=0; i<groups.size(); i++) {
                String groupName = (String) groups.get(i);
                groupsArray[i][AuthInterface.GROUPNAMEINDEX] = groupName;
                String description = null;
                List<Object>descriptions = userpassword.getList(GROUPS+SLASH+GROUP+"["+AT+NAME+"='"+groupName+"']"+SLASH+DESCRIPTION);
                if(descriptions != null && !descriptions.isEmpty()) {
                    description = (String)descriptions.get(0);
                }
                groupsArray[i][AuthInterface.GROUPDESINDEX] = description; 
            }
            return groupsArray;
        }
        return null;
    }
    
    @Override
    public HashMap<String, Vector<String>> getAttributes(String foruser)
                    throws ConnectException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public HashMap<String, Vector<String>> getAttributes(String user,
                    String password, String foruser) throws ConnectException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getPrincipals(String user, String password)
                    throws ConnectException {
            StringBuffer out = new StringBuffer();

            out.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n");
            out.append("<principals>\n");
            out.append("  <authSystem URI=\"" +authURI
                    + "\" organization=\"" + ORGANIZATIONNAME + "\">\n");

            // get all groups for directory context
            String[][] groups = getGroups(user, password);
            String[][] users = getUsers(user, password);
            int userIndex = 0;

            // for the groups and users that belong to them
            if (groups != null && users != null && groups.length > 0) {
                for (int i = 0; i < groups.length; i++) {
                    out.append("    <group>\n");
                    out.append("      <groupname>" + groups[i][AuthInterface.GROUPNAMEINDEX] + "</groupname>\n");
                    if(groups[i].length > 1) {
                        out.append("      <description>" + groups[i][AuthInterface.GROUPDESINDEX] + "</description>\n");
                    }
                    String[] usersForGroup = getUsers(user, password, groups[i][0]);
                    if(usersForGroup != null) {
                        for (int j = 0; j < usersForGroup.length; j++) {
                            userIndex = AuthLdap.searchUser(usersForGroup[j], users);
                            out.append("      <user>\n");

                            if (userIndex < 0) {
                                out.append("        <username>" + usersForGroup[j]
                                        + "</username>\n");
                            } else {
                                out.append("        <username>" + users[userIndex][0]
                                        + "</username>\n");
                                if(users[userIndex][AuthInterface.USERCNINDEX] != null) {
                                    out.append("        <name>" + users[userIndex][AuthInterface.USERCNINDEX]
                                                    + "</name>\n");
                                }
                                if(users[userIndex][AuthInterface.USERORGINDEX] != null) {
                                    out.append("        <organization>" + users[userIndex][AuthInterface.USERORGINDEX]
                                                    + "</organization>\n");
                                }
                                
                                if(users[userIndex][AuthInterface.USERORGUNITINDEX] != null) {
                                    out.append("      <organizationUnitName>"
                                                    + users[userIndex][AuthInterface.USERORGUNITINDEX]
                                                    + "</organizationUnitName>\n");
                                }
                                if(users[userIndex][AuthInterface.USEREMAILINDEX] != null) {
                                    out.append("        <email>" + users[userIndex][AuthInterface.USEREMAILINDEX]
                                                    + "</email>\n");
                                }
                               
                            }

                            out.append("      </user>\n");
                        }
                    }
                   
                    out.append("    </group>\n");
                }
            }

            if (users != null) {
                // for the users not belonging to any grou8p
                for (int j = 0; j < users.length; j++) {
                    out.append("    <user>\n");
                    out.append("      <username>" + users[j][0] + "</username>\n");
                    if(users[j][AuthInterface.USERCNINDEX] != null) {
                        out.append("        <name>" + users[j][AuthInterface.USERCNINDEX]
                                        + "</name>\n");
                    }
                    if(users[j][AuthInterface.USERORGINDEX] != null) {
                        out.append("        <organization>" + users[j][AuthInterface.USERORGINDEX]
                                        + "</organization>\n");
                    }
                    
                    if(users[j][AuthInterface.USERORGUNITINDEX] != null) {
                        out.append("      <organizationUnitName>"
                                        + users[j][AuthInterface.USERORGUNITINDEX]
                                        + "</organizationUnitName>\n");
                    }
                    if(users[j][AuthInterface.USEREMAILINDEX] != null) {
                        out.append("        <email>" + users[j][AuthInterface.USEREMAILINDEX]
                                        + "</email>\n");
                    }
                   
                    out.append("    </user>\n");
                }
            }

            out.append("  </authSystem>\n");
        
        out.append("</principals>");
        return out.toString();
    }
    
    /**
     * Add a user to the file
     * @param userName the name of the user
     * @param groups  the groups the user belong to. The group should exist in the file
     * @param password  the password of the user
     */
    public void addUser(String dn, String[] groups, String plainPass, String hashedPass, String email, String surName, String givenName, String organization) throws AuthenticationException{
       User user = new User();
       user.setDN(dn);
       user.setGroups(groups);
       user.setPlainPass(plainPass);
       user.setHashedPass(hashedPass);
       user.setEmail(email);
       user.setSurName(surName);
       user.setGivenName(givenName);
       user.setOrganization(organization);
       user.serialize();
    }
    
    /**
     * Add a group into the file
     * @param groupName the name of group
     */
    public void addGroup(String groupName, String description) throws AuthenticationException{
        if(groupName == null || groupName.trim().equals("")) {
            throw new AuthenticationException("AuthFile.addGroup - can't add a group whose name is null or blank.");
        }
        if(!groupExists(groupName)) {
            if(userpassword != null) {
              userpassword.addProperty(GROUPS+" "+GROUP+AT+NAME, groupName);
              if(description != null && !description.trim().equals("")) {
                  userpassword.addProperty(GROUPS+SLASH+GROUP+"["+AT+NAME+"='"+groupName+"']"+" "+DESCRIPTION, description);
              }
              //userpassword.reload();
             }
        } else {
            throw new AuthenticationException("AuthFile.addGroup - can't add the group "+groupName+" since it already exists.");
        }
    }
    
   
    
    /**
     * Change the password of the user to the new one which is hashed
     * @param usrName the specified user.   
     * @param newPassword the new password which will be set
     */
    public void modifyPassWithHash(String userName, String newHashPassword) throws AuthenticationException {
       User user = new User();
       user.setDN(userName);
       user.modifyHashPass(newHashPassword);
    }
    
    /**
     * Change the password of the user to the new one which is plain. However, only the hashed version will be serialized.
     * @param usrName the specified user.   
     * @param newPassword the new password which will be set
     */
    public void modifyPassWithPlain(String userName, String newPlainPassword) throws AuthenticationException {
        User user = new User();
        user.setDN(userName);
        user.modifyPlainPass(newPlainPassword);
    }
    
    
    /**
     * Add a user to a group
     * @param userName  the name of the user. the user should already exist
     * @param group  the name of the group. the group should already exist
     */
    public void addUserToGroup(String userName, String group) throws AuthenticationException {
        User user = new User();
        user.setDN(userName);
        user.addToGroup(group);
    }
    
    /**
     * Remove a user from a group.
     * @param userName  the name of the user. the user should already exist.
     * @param group the name of the group
     */
    public void removeUserFromGroup(String userName, String group) throws AuthenticationException{
        User user = new User();
        user.setDN(userName);
        user.removeFromGroup(group);
    }
    
  
    
    /**
     * If the specified user name exist or not
     * @param userName the name of the user
     * @return true if the user eixsit
     */
    private synchronized boolean userExists(String userName) throws AuthenticationException{
        if(userName == null || userName.trim().equals("")) {
            throw new AuthenticationException("AuthFile.userExist - can't judge if a user exists when its name is null or blank.");
        }
        List<Object> users = userpassword.getList(USERS+SLASH+USER+SLASH+AT+DN);
        if(users != null && users.contains(userName)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * If the specified group exist or not
     * @param groupName the name of the group
     * @return true if the user exists
     */
    private synchronized boolean groupExists(String groupName) throws AuthenticationException{
        if(groupName == null || groupName.trim().equals("")) {
            throw new AuthenticationException("AuthFile.groupExist - can't judge if a group exists when its name is null or blank.");
        }
        List<Object> groups = userpassword.getList(GROUPS+SLASH+GROUP+SLASH+AT+NAME);
        if(groups != null && groups.contains(groupName)) {
            return true;
        } else {
            return false;
        }
    }
    
    /*
     * Encrypt a plain text
     */
    private static String encrypt(String plain)  {
      return hashClass.hash(plain);
    }
    
    
    /**
     * A method is used to help administrator to manage users and groups
     * @param argus
     * @throws Exception
     */
    public static void main(String[] argus) throws Exception {
            String USERADD = "useradd";
            String USERMOD = "usermod";
            String GROUPADD = "groupadd";
            String USAGE = "usage";
            if(argus == null || argus.length ==0) {
              System.out.println("Please make sure that there are two arguments - \"$BASE_WEB_INF\" and\" $@\" after the class name edu.ucsb.nceas.metacat.authentication.AuthFile in the script file.");
              System.exit(1);
            } else if(argus.length ==1) {
                printUsage();
                System.exit(1);
            }
            PropertyService.getInstance(argus[0]);
            AuthFile authFile = AuthFile.getInstance();
            if(argus[1] != null && argus[1].equals(GROUPADD)) {
                handleGroupAdd(authFile,argus);
            } else if (argus[1] != null && argus[1].equals(USERADD)) {
                handleUserAdd(authFile,argus);
            } else if (argus[1] != null && argus[1].equals(USERMOD)) {
                
            } else if (argus[1] != null && argus[1].equals(USAGE)) {
                printUsage();
            } else {
                System.out.print("The unknown action "+argus[1]);
            }
    }
    
    /*
     * Handle the groupAdd action in the main method
     */
    private static void handleGroupAdd(AuthFile authFile, String[]argus) throws AuthenticationException {
        HashMap<String, String> map = new <String, String>HashMap();
        String DASHG = "-g";
        String DASHD = "-d";
        for(int i=2; i<argus.length; i++) {
            String arg = argus[i];
            
            if(map.containsKey(arg)) {
                System.out.println("The command line for groupadd can't have the duplicated options "+arg+".");
                System.exit(1);
            }
            
            if(arg.equals(DASHG) && i<argus.length-1) {
                map.put(arg, argus[i+1]);
            } else if (arg.equals(DASHD) && i<argus.length-1) {
                map.put(arg, argus[i+1]);
            } else if(!arg.equals(DASHG) && !arg.equals(DASHD)) {
                //check if the previous argument is -g or -d
                if(!argus[i-1].equals(DASHG) || !argus[i-1].equals(DASHD)) {
                    System.out.println("An illegal argument "+arg+" in the groupadd command ");
                }
            }
        } 
        String groupName = null;
        String description = null;
        if(map.keySet().size() ==1 || map.keySet().size() ==2) {
            groupName = map.get(DASHG);
            if(groupName == null) {
                System.out.println("The "+DASHG+" group-name is required in the groupadd command line.");
            }
            description = map.get(DASHD);
            authFile.addGroup(groupName, description);
            System.out.println("Successfully add a group "+groupName+" to the file authentication system");
        } else {
            printError(argus);
            System.exit(1);
        }
    }
    
    /*
     * Handle the userAdd action in the main method
     */
    private static void  handleUserAdd(AuthFile authFile,String[]argus) {
        String I = "-i";
        String H = "-h";
        String DN = "-dn";
        String G = "-g";
        String E = "-e";
        String S = "-s";
        String F = "-f";
        String O= "-o";
        HashMap<String, String> map = new <String, String>HashMap();
        
    }
    
    /*
     * Print out the usage statement
     */
    private static void printUsage() {
        System.out.println("Usage:\n"+
                        "./authFileManager.sh useradd -i -dn user-distinguish-name -g groupname -e email-address -s surname -f given-name -o organizationName\n" +
                        "./authFileManager.sh useradd -h hashed-password -dn user-distinguish-name -g groupname -e email-address -s surname -f given-name -o organizationName\n"+
                        "./authFileManager.sh groupadd -g group-name -d description\n" +
                        "./authFileManager.sh usermod -password -dn user-distinguish-name -i\n"+
                        "./authFileManager.sh usermod -password -dn user-distinguish-name -h new-hashed-password\n"+
                        "./authFileManager.sh usermod -group -a -dn user-disinguish-name -g added-group-name\n" +
                        "./authFileManager.sh usermod -group -r -dn user-distinguish-name -g removed-group-name\n"+
                        "Note:\n1. if a value of an option has spaces, the value should be enclosed by the double quotes.\n"+
                        "  For example: ./authFileManager.sh groupadd -g nceas-dev -d \"Developers at NCEAS\"\n"+
                        "2. \"-d description\" in groupadd is optional; \"-g groupname -e email-address -s surname -f given-name -o organizationName\" in useradd are optional as well.");
                       
                        
    }
    
    /*
     * Print out the statement to say it is a illegal command
     */
    private static void printError(String[] argus) {
        if(argus != null) {
            System.out.println("It is an illegal command: ");
            for(int i=0; i<argus.length; i++) {
                if(i!= 0) {
                    System.out.print(argus[i]+" ");
                }
            }
            System.out.println("");
        }
       
    }

    
    /**
     * An class represents the information for a user. 
     * @author tao
     *
     */
    private class User {
        private String dn = null;//the distinguish name
        private String plainPass = null;
        private String hashedPass = null;
        private String email = null;
        private String surName = null;
        private String givenName = null;
        private String cn = null;//the common name
        private String[] groups = null;
        private String organization = null;
        
        /**
         * Get the organization of the user
         * @return
         */
        public String getOrganization() {
            return organization;
        }
        
        /**
         * Set the organization for the user.
         * @param organization
         */
        public void setOrganization(String organization) {
            this.organization = organization;
        }
        /**
         * Get the distinguish name of the user
         * @return the distinguish name 
         */
        public String getDN() {
            return this.dn;
        }
        
        /**
         * Set the distinguish name for the user
         * @param dn the specified dn
         */
        public void setDN(String dn) {
            this.dn = dn;
        }
        
        /**
         * Get the plain password for the user. This value will NOT be serialized to
         * the password file
         * @return the plain password for the user
         */
        public String getPlainPass() {
            return plainPass;
        }
        
        /**
         * Set the plain password for the user.
         * @param plainPass the plain password will be set.
         */
        public void setPlainPass(String plainPass) {
            this.plainPass = plainPass;
        }
        
        /**
         * Get the hashed password of the user
         * @return the hashed password of the user
         */
        public String getHashedPass() {
            return hashedPass;
        }
        
        /**
         * Set the hashed the password for the user.
         * @param hashedPass the hashed password will be set.
         */
        public void setHashedPass(String hashedPass) {
            this.hashedPass = hashedPass;
        }
        
        /**
         * Get the email of the user
         * @return the email of the user
         */
        public String getEmail() {
            return email;
        }
        
        /**
         * Set the email address for the user
         * @param email the eamil address will be set
         */
        public void setEmail(String email) {
            this.email = email;
        }
        
        /**
         * Get the surname of the user
         * @return the surname of the user
         */
        public String getSurName() {
            return surName;
        }
        
        /**
         * Set the surname of the user
         * @param surName
         */
        public void setSurName(String surName) {
            this.surName = surName;
        }
        
        /**
         * Get the given name of the user
         * @return the given name of the user
         */
        public String getGivenName() {
            return givenName;
        }
        
        /**
         * Set the GivenName of the user
         * @param givenName
         */
        public void setGivenName(String givenName) {
            this.givenName = givenName;
        }
        
        /**
         * Get the common name of the user. If the cn is null, the GivenName +SurName will
         * be returned
         * @return the common name
         */
        public String getCn() {
            if(cn != null) {
                return cn;
            } else {
                if (givenName != null && surName != null) {
                    return givenName+" "+surName;
                } else if (givenName != null) {
                    return givenName;
                } else if (surName != null ) {
                    return surName;
                } else {
                    return null;
                }
            }
        }
        
        /**
         * Set the common name for the user
         * @param cn
         */
        public void setCn(String cn) {
            this.cn = cn;
        }
        
        /**
         * Get the groups of the user belong to
         * @return
         */
        public String[] getGroups() {
            return groups;
        }
        
        /**
         * Set the groups of the user belong to
         * @param groups
         */
        public void setGroups(String[] groups) {
            this.groups = groups;
        }
        
        /**
         * Add the user to a group and serialize the change to the password file.
         * @param group the group which the user will join
         * @throws AuthenticationException 
         */
        public void addToGroup(String group) throws AuthenticationException {
            if(group == null || group.trim().equals("")) {
                throw new IllegalArgumentException("AuthFile.User.addToGroup - the group can't be null or blank");
            }
            if(!userExists(dn)) {
                throw new AuthenticationException("AuthFile.User.addUserToGroup - the user "+dn+ " doesn't exist.");
            }
            if(!groupExists(group)) {
                throw new AuthenticationException("AuthFile.User.addUserToGroup - the group "+group+ " doesn't exist.");
            }
            List<Object> existingGroups = userpassword.getList(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+SLASH+GROUP);
            if(existingGroups != null && existingGroups.contains(group)) {
                throw new AuthenticationException("AuthFile.User.addUserToGroup - the user "+dn+ " already is the memember of the group "+group);
            }
            userpassword.addProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+" "+GROUP, group);
            //add information to the memory
            if(groups == null) {
                if(existingGroups == null || existingGroups.isEmpty()) {
                    groups = new String[1];
                    groups[0] = group;
                } else {
                    groups = new String[existingGroups.size()+1];
                    for(int i=0; i<existingGroups.size(); i++) {
                        groups[i] = (String)existingGroups.get(i);
                    }
                    groups[existingGroups.size()] = group;
                }
                
            } else {
                String[] oldGroups = groups;
                groups = new String[oldGroups.length+1];
                for(int i=0; i<oldGroups.length; i++) {
                    groups[i]= oldGroups[i];
                }
                groups[oldGroups.length] = group;
                
            }
        }
        
        /**
         * Remove the user from a group and serialize the change to the password file
         * @param group
         * @throws AuthenticationException
         */
        public void removeFromGroup(String group) throws AuthenticationException {
            if(!userExists(dn)) {
                throw new AuthenticationException("AuthFile.User.removeUserFromGroup - the user "+dn+ " doesn't exist.");
            }
            if(!groupExists(group)) {
                throw new AuthenticationException("AuthFile.User.removeUserFromGroup - the group "+group+ " doesn't exist.");
            }
            String key = USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+SLASH+GROUP;
            List<Object> existingGroups = userpassword.getList(key);
            if(!existingGroups.contains(group)) {
                throw new AuthenticationException("AuthFile.User.removeUserFromGroup - the user "+dn+ " isn't the memember of the group "+group);
            } else {
                userpassword.clearProperty(key+"[.='"+group+"']");
            }
            //change the value in the memory.
            if(groups != null) {
                boolean contains = false;
                for(int i=0; i<groups.length; i++) {
                    if(groups[i].equals(group)) {
                        contains = true;
                        break;
                    }
                }
                String[] newGroups = new String[groups.length-1];
                int k =0;
                for(int i=0; i<groups.length; i++) {
                    if(!groups[i].equals(group)) {
                       newGroups[k] = groups[i];
                       k++;
                    }
                }
                groups = newGroups;
            }
        }
        
        /**
         * Modify the hash password and serialize it to the password file
         * @param hashPass
         * @throws AuthenticationException
         */
        public void modifyHashPass(String hashPass) throws AuthenticationException {
            if(hashPass == null || hashPass.trim().equals("")) {
                throw new AuthenticationException("AuthFile.User.modifyHashPass - can't change the password to the null or blank.");
            }
            if(!userExists(dn)) {
                throw new AuthenticationException("AuthFile.User.modifyHashPass - can't change the password for the user "+dn+" since it doesn't eixt.");
            }
            userpassword.setProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+SLASH+PASSWORD, hashPass);
            setHashedPass(hashPass);
      
        }
        
        /**
         * Modify the plain password and serialize its hash version to the password file
         * @param plainPass
         * @throws AuthenticationException 
         */
        public void modifyPlainPass(String plainPass) throws AuthenticationException {
            if(plainPass == null || plainPass.trim().equals("")) {
                throw new AuthenticationException("AuthFile.User.modifyPlainPass - can't change the password to the null or blank.");
            }
            if(!userExists(dn)) {
                throw new AuthenticationException("AuthFile.User.modifyPlainPass - can't change the password for the user "+dn+" since it doesn't eixt.");
            }
            String hashPassword = null;
            try {
                hashPassword = encrypt(plainPass);
            } catch (Exception e) {
                throw new AuthenticationException("AuthFile.User.modifyPlainPass - can't encript the password since "+e.getMessage());
            }
            userpassword.setProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+SLASH+PASSWORD, hashPassword);
            setPlainPass(plainPass);
        }
        
        /**
         * Add the user to the password file. 
         */
        public void serialize() throws AuthenticationException {
            if(dn == null || dn.trim().equals("")) {
                throw new AuthenticationException("AuthFile.User.serialize - can't add a user whose name is null or blank.");
            }
            if(hashedPass == null || hashedPass.trim().equals("")) {
                if(plainPass == null || plainPass.trim().equals("")) {
                    throw new AuthenticationException("AuthFile.User.serialize - can't add a user whose password is null or blank.");
                } else {
                    try {
                        hashedPass = encrypt(plainPass);
                    } catch (Exception e) {
                        throw new AuthenticationException("AuthFile.User.serialize - can't encript the password since "+e.getMessage());
                    }
                }
            }

            if(!userExists(dn)) {
                if(userpassword != null) {
                  userpassword.addProperty(USERS+" "+USER+AT+DN, dn);
                  userpassword.addProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+" "+PASSWORD, hashedPass);
                  
                  if(email != null && !email.trim().equals("")) {
                      userpassword.addProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+" "+EMAIL, email);
                  }
                  
                  if(surName != null && !surName.trim().equals("")) {
                      userpassword.addProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+" "+SURNAME, surName);
                  }
                  
                  if(givenName != null && !givenName.trim().equals("")) {
                      userpassword.addProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+" "+GIVENNAME, givenName);
                  }
                  
                  if(organization != null && !organization.trim().equals("")) {
                      userpassword.addProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+" "+ORGANIZATION, organization);
                  }

                  if(groups != null) {
                      for(int i=0; i<groups.length; i++) {
                          String group = groups[i];
                          if(group != null && !group.trim().equals("")) {
                              if(groupExists(group)) {
                                  userpassword.addProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+" "+GROUP, group);
                              }
                          }
                      }
                  }
                  //userpassword.reload();
                 }
            } else {
                throw new AuthenticationException("AuthFile.User.serialize - can't add the user "+dn+" since it already exists.");
            }
        }
    }

}
