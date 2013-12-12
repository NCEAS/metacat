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
 *      <user name="uid=tao,o=NCEAS,dc=ecoinformatics,dc=org">
 *          <password>*******</password>
 *          <group>nceas-dev</group>
 *      </user>
 *  </users>
 *  <groups>
 *    <group name="nceas-dev"/>
 *  </groups>
 * </subjects>
 * http://commons.apache.org/proper/commons-configuration/userguide/howto_xml.html
 * @author tao
 *
 */
public class AuthFile implements AuthInterface {
    private static final String ORGANIZATION = "UNkown";
    private static final String NAME = "name";
    private static final String UID = "uid";
    private static final String DESCRIPTION = "description";
    private static final String PASSWORD = "password";
    private static final String SLASH = "/";
    private static final String AT = "@";
    private static final String SUBJECTS = "subjects";
    private static final String USERS = "users";
    private static final String USER = "user";
    private static final String GROUPS = "groups";
    private static final String GROUP = "group";
    private static final String INITCONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"+
                                    "<"+SUBJECTS+">\n"+"<"+USERS+">\n"+"</"+USERS+">\n"+"<"+GROUPS+">\n"+"</"+GROUPS+">\n"+"</"+SUBJECTS+">\n";
    
    private static final byte[] SALT = {
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };
    private static Log log = LogFactory.getLog(AuthFile.class);
    private static AuthFile authFile = null;
    private XMLConfiguration userpassword = null;
    private String authURI = null;
    private static String passwordFilePath = null;
    private static  char[] masterPass = "enfldsgbnlsngdlksdsgm".toCharArray();
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
            throw new AuthenticationException(e.getMessage());
        }
        
    }
    
    /*
     * Initialize the user/password configuration
     */
    private void init() throws PropertyNotFoundException, IOException, ConfigurationException {
        if(passwordFilePath == null) {
            passwordFilePath  = PropertyService.getProperty("auth.file.path");
        }
        File passwordFile = new File(passwordFilePath);
        try {
            String password = PropertyService.getProperty("auth.file.pass");
            if(password != null && !password.trim().equals("")) {
                masterPass = password.toCharArray();
            }
            authURI = SystemUtil.getContextURL();
        }catch(PropertyNotFoundException e) {
            log.warn("AuthFile.init - can't find the auth.file.pass in the metacat.properties. Metacat will use the default one as password.");
        }
       
        //if the password file doesn't exist, create a new one and set the initial content
        if(!passwordFile.exists()) {
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
        String passwordRecord = userpassword.getString(USERS+SLASH+USER+"["+AT+UID+"='"+user+"']"+SLASH+PASSWORD);
        if(passwordRecord != null) {
            try {
                passwordRecord = decrypt(passwordRecord);
            } catch (Exception e) {
                throw new AuthenticationException("AuthFile.authenticate - can't decrypt the password for the user "+user+" since "+e.getMessage());
            }
            if(passwordRecord.equals(password)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    /**
     * Get all users. This is two-dimmention array. Each row is a user. The first element of
     * a row is the user name.
     */
    public String[][] getUsers(String user, String password)
                    throws ConnectException {
        List<Object> users = userpassword.getList(USERS+SLASH+USER+SLASH+AT+UID);
        if(users != null && users.size() > 0) {
            String[][] usersArray = new String[users.size()][1];
            for(int i=0; i<users.size(); i++) {
                usersArray[i][0] = (String) users.get(i);
            }
            return usersArray;
        }
        return null;
    }
    
    @Override
    public String[] getUserInfo(String user, String password)
                    throws ConnectException {
        // TODO Auto-generated method stub
        return null;
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
        List<Object> users = userpassword.getList(USERS+SLASH+USER+"["+GROUP+"='"+group+"']"+SLASH+AT+UID);
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
     * group. The first column is the group name. The null will return if no group found.
     */
    public String[][] getGroups(String user, String password)
                    throws ConnectException {
        List<Object> groups = userpassword.getList(GROUPS+SLASH+GROUP+SLASH+AT+NAME);
        if(groups!= null && groups.size() >0) {
            String[][] groupsArray = new String[groups.size()][1];
            for(int i=0; i<groups.size(); i++) {
                groupsArray[i][0] = (String) groups.get(i);
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
        List<Object> groups = userpassword.getList(USERS+SLASH+USER+"["+AT+UID+"='"+foruser+"']"+SLASH+GROUP);
        if(groups != null && groups.size() > 0) {
            String[][] groupsArray = new String[groups.size()][1];
            for(int i=0; i<groups.size(); i++) {
                groupsArray[i][0] = (String) groups.get(i);
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
                    + "\" organization=\"" + ORGANIZATION + "\">\n");

            // get all groups for directory context
            String[][] groups = getGroups(user, password);
            String[][] users = getUsers(user, password);
            int userIndex = 0;

            // for the groups and users that belong to them
            if (groups != null && users != null && groups.length > 0) {
                for (int i = 0; i < groups.length; i++) {
                    out.append("    <group>\n");
                    out.append("      <groupname>" + groups[i][0] + "</groupname>\n");
                    if(groups[i].length > 1) {
                        out.append("      <description>" + groups[i][1] + "</description>\n");
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
                                if(users[userIndex].length >=2) {
                                    out.append("        <name>" + users[userIndex][1]
                                                    + "</name>\n");
                                }
                                if(users[userIndex].length >=3) {
                                    out.append("        <email>" + users[userIndex][2]
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
                    if(users[userIndex].length >=2) {
                        out.append("      <name>" + users[j][1] + "</name>\n");
                    }
                    if(users[userIndex].length >=3) {
                        out.append("      <email>" + users[j][2] + "</email>\n");
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
    public void addUser(String userName, String[] groups, String password) throws AuthenticationException{
        if(userName == null || userName.trim().equals("")) {
            throw new AuthenticationException("AuthFile.addUser - can't add a user whose name is null or blank.");
        }
        if(password == null || password.trim().equals("")) {
            throw new AuthenticationException("AuthFile.addUser - can't add a user whose password is null or blank.");
        }
        try {
            password = encrypt(password);
        } catch (Exception e) {
            throw new AuthenticationException("AuthFile.addUser - can't encript the password since "+e.getMessage());
        }
        
        if(!userExists(userName)) {
            if(userpassword != null) {
              userpassword.addProperty(USERS+" "+USER+AT+UID, userName);
              userpassword.addProperty(USERS+SLASH+USER+"["+AT+UID+"='"+userName+"']"+" "+PASSWORD, password);
              if(groups != null) {
                  for(int i=0; i<groups.length; i++) {
                      String group = groups[i];
                      if(group != null && !group.trim().equals("")) {
                          if(groupExists(group)) {
                              userpassword.addProperty(USERS+SLASH+USER+"["+AT+UID+"='"+userName+"']"+" "+GROUP, group);
                          }
                      }
                  }
              }
              //userpassword.reload();
             }
        } else {
            throw new AuthenticationException("AuthFile.addUser - can't add the user "+userName+" since it already exists.");
        }
    }
    
    /**
     * Add a group into the file
     * @param groupName the name of group
     */
    public void addGroup(String groupName) throws AuthenticationException{
        if(groupName == null || groupName.trim().equals("")) {
            throw new AuthenticationException("AuthFile.addGroup - can't add a group whose name is null or blank.");
        }
        if(!groupExists(groupName)) {
            if(userpassword != null) {
              userpassword.addProperty(GROUPS+" "+GROUP+AT+NAME, groupName);
              //userpassword.reload();
             }
        } else {
            throw new AuthenticationException("AuthFile.addGroup - can't add the group "+groupName+" since it already exists.");
        }
    }
    
    /**
     * Reset the password for the user
     * @param userName  the name of the user. The user should already exist
     * @param password  the password of the user.
     * @return
     */
    public String resetPassword(String userName) throws AuthenticationException {
        String password = new String(RandomPasswordGenerator.generatePswd(10, 12, 4, 3, 2));
        changePassword(userName, password);
        return password;
    }
    
    /**
     * Change the password of the user to the new one. But we need to know the old password
     * @param usrName the specified user.   
     * @param oldPassword the old password of the user      
     * @param newPassword the new password which will be set
     */
    public void modifyPassword(String userName, String oldPassword, String newPassword) throws AuthenticationException {
        if(!authenticate(userName, oldPassword)) {
            throw new AuthenticationException("AuthFile.modifyUserPassword - the username or the old password is not correct");
        }
        changePassword(userName, newPassword);
    }
    
    /**
     * Add a user to a group
     * @param userName  the name of the user. the user should already exist
     * @param group  the name of the group. the group should already exist
     */
    public void addUserToGroup(String userName, String group) throws AuthenticationException {
        if(!userExists(userName)) {
            throw new AuthenticationException("AuthFile.addUserToGroup - the user "+userName+ " doesn't exist.");
        }
        if(!groupExists(group)) {
            throw new AuthenticationException("AuthFile.addUserToGroup - the group "+group+ " doesn't exist.");
        }
        List<Object> existingGroups = userpassword.getList(USERS+SLASH+USER+"["+AT+UID+"='"+userName+"']"+SLASH+GROUP);
        if(existingGroups.contains(group)) {
            throw new AuthenticationException("AuthFile.addUserToGroup - the user "+userName+ " already is the memember of the group "+group);
        }
        userpassword.addProperty(USERS+SLASH+USER+"["+AT+UID+"='"+userName+"']"+" "+GROUP, group);
    }
    
    /**
     * Remove a user from a group.
     * @param userName  the name of the user. the user should already exist.
     * @param group the name of the group
     */
    public void removeUserFromGroup(String userName, String group) throws AuthenticationException{
        if(!userExists(userName)) {
            throw new AuthenticationException("AuthFile.removeUserFromGroup - the user "+userName+ " doesn't exist.");
        }
        if(!groupExists(group)) {
            throw new AuthenticationException("AuthFile.removeUserFromGroup - the group "+group+ " doesn't exist.");
        }
        String key = USERS+SLASH+USER+"["+AT+UID+"='"+userName+"']"+SLASH+GROUP;
        List<Object> existingGroups = userpassword.getList(key);
        if(!existingGroups.contains(group)) {
            throw new AuthenticationException("AuthFile.removeUserFromGroup - the user "+userName+ " isn't the memember of the group "+group);
        } else {
            userpassword.clearProperty(key+"[.='"+group+"']");
        }
    }
    
    /**
     * Change the password of the user to the specified one
     * @param userName
     * @param password
     */
    private void changePassword(String userName, String password) throws AuthenticationException{
        if(!userExists(userName)) {
            throw new AuthenticationException("AuthFile.changePassword - can't change the password for the user "+userName+" since it doesn't eixt.");
        }
        String encryped = null;
        try {
            encryped = encrypt(password);
        } catch (Exception e) {
            throw new AuthenticationException("AuthFile.changepassword - can't encrype the new password for the user "+userName+" since "+e.getMessage());
        }
        userpassword.setProperty(USERS+SLASH+USER+"["+AT+UID+"='"+userName+"']"+SLASH+PASSWORD, encryped);
    }
    
    /**
     * If the specified user name exist or not
     * @param userName the name of the user
     * @return true if the user eixsit
     */
    private boolean userExists(String userName) throws AuthenticationException{
        if(userName == null || userName.trim().equals("")) {
            throw new AuthenticationException("AuthFile.userExist - can't judge if a user exists when its name is null or blank.");
        }
        List<Object> users = userpassword.getList(USERS+SLASH+USER+SLASH+AT+UID);
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
    private boolean groupExists(String groupName) throws AuthenticationException{
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
     * Encrypt a string
     */
    private static String encrypt(String property) throws GeneralSecurityException, UnsupportedEncodingException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        //System.out.println("===================== tha master password "+masterPass);
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(masterPass));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
    }

    /*
     * Transform a byte array to a string
     */
    private static String base64Encode(byte[] bytes) {
        return Base64.encodeBase64String(bytes);
    }

    /*
     * Decrypt a string
     */
    private static String decrypt(String property) throws GeneralSecurityException, IOException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(masterPass));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    }

    /*
     * Transform a string to a byte array
     */
    private static byte[] base64Decode(String property) throws IOException {
        return Base64.decodeBase64(property);
    }
    
    /**
     * A internal class to generate random passowrd
     * @author tao
     *
     */
    static class RandomPasswordGenerator {
        private static final String ALPHA_CAPS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        private static final String ALPHA   = "abcdefghijklmnopqrstuvwxyz";
        private static final String NUM     = "0123456789";
        private static final String SPL_CHARS   = "!$^_-/";
     
        public static char[] generatePswd(int minLen, int maxLen, int noOfCAPSAlpha,
                int noOfDigits, int noOfSplChars) {
            if(minLen > maxLen)
                throw new IllegalArgumentException("Min. Length > Max. Length!");
            if( (noOfCAPSAlpha + noOfDigits + noOfSplChars) > minLen )
                throw new IllegalArgumentException
                ("Min. Length should be atleast sum of (CAPS, DIGITS, SPL CHARS) Length!");
            Random rnd = new Random();
            int len = rnd.nextInt(maxLen - minLen + 1) + minLen;
            char[] pswd = new char[len];
            int index = 0;
            for (int i = 0; i < noOfCAPSAlpha; i++) {
                index = getNextIndex(rnd, len, pswd);
                pswd[index] = ALPHA_CAPS.charAt(rnd.nextInt(ALPHA_CAPS.length()));
            }
            for (int i = 0; i < noOfDigits; i++) {
                index = getNextIndex(rnd, len, pswd);
                pswd[index] = NUM.charAt(rnd.nextInt(NUM.length()));
            }
            for (int i = 0; i < noOfSplChars; i++) {
                index = getNextIndex(rnd, len, pswd);
                pswd[index] = SPL_CHARS.charAt(rnd.nextInt(SPL_CHARS.length()));
            }
            for(int i = 0; i < len; i++) {
                if(pswd[i] == 0) {
                    pswd[i] = ALPHA.charAt(rnd.nextInt(ALPHA.length()));
                }
            }
            return pswd;
        }
     
        private static int getNextIndex(Random rnd, int len, char[] pswd) {
            int index = rnd.nextInt(len);
            while(pswd[index = rnd.nextInt(len)] != 0);
            return index;
        }
    }

}
