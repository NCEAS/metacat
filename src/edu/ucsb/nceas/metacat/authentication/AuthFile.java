package edu.ucsb.nceas.metacat.authentication;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

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
 * The password file looks like:
 *<?xml version="1.0" encoding="UTF-8" ?>
 * <subjects>
 *  <users>
 *      <user dn="uid=tao,o=NCEAS,dc=ecoinformatics,dc=org">
 *          <password>*******</password>
 *          <email>foo@foo.com</email>
 *          <surName>Smith</surName>
 *          <givenName>John</givenName>
 *          <organization>NCEAS</organization>
 *          <memberof>nceas-dev</memberof>
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
    private static final String ORGANIZATIONNAME = "Unknown";
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
    private static final String MEMBEROF = "memberof";
    private static final String INITCONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"+
                                    "<"+SUBJECTS+">\n"+"<"+USERS+">\n"+"</"+USERS+">\n"+"<"+GROUPS+">\n"+"</"+GROUPS+">\n"+"</"+SUBJECTS+">\n";
   
    
    
    private static Log log = LogFactory.getLog(AuthFile.class);
    private static XMLConfiguration userpassword = null;
    private String authURI = null;
    private static String passwordFilePath = null;
    private static AuthFileHashInterface hashClass = null;
    private boolean readPathFromProperty = true;
    
    /**
     * Get the instance of the AuthFile
     * @return
     * @throws AuthenticationException
     */
    /*public static AuthFile getInstance() throws AuthenticationException {
        if(authFile == null) {
            authFile = new AuthFile();
        }
        return authFile;
    }*/
    
    /**
     * Get the instance of the AuthFile from specified password file
     * @return
     * @throws AuthenticationException
     */
    /*public static AuthFile getInstance(String passwordFile) throws AuthenticationException {
        passwordFilePath = passwordFile;
        if(authFile == null) {
            authFile = new AuthFile();
        }
        return authFile;
    }*/
    
    /**
     * Constructor
     */
    public AuthFile() throws AuthenticationException {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AuthenticationException(e.getMessage());
        }
        
    }
    
    /**
     * 
     * @param passwordFile
     * @throws AuthenticationException
     */
    public  AuthFile (String passwordFile) throws AuthenticationException {
        passwordFilePath = passwordFile;
        readPathFromProperty = false;
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
        if(readPathFromProperty || passwordFilePath == null) {
            passwordFilePath  = PropertyService.getProperty("auth.file.path");
        }
        //System.out.println("the password file path is ======================= "+passwordFilePath);
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
                boolean success = false;
                try {
                    success = parent.mkdirs();
                } catch (Exception e) {
                    throw new IOException("AuthFile.init - couldn't create the directory "+parent.getAbsolutePath()+ " since "+e.getMessage());
                }
                if(!success) {
                    throw new IOException("AuthFile.init - couldn't create the directory "+parent.getAbsolutePath()+ ", probably since the metacat doesn't have the write permission.");
                }
            }
            boolean success = false;
            try {
                success = passwordFile.createNewFile();
            }  catch (Exception e) {
                throw new IOException("AuthFile.init - couldn't create the file "+passwordFile.getAbsolutePath()+ " since "+e.getMessage());
            }
            if(!success) {
                throw new IOException("AuthFile.init - couldn't create the file "+parent.getAbsolutePath()+ ", probably since the metacat doesn't have the write permission.");
            }
            
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
        List<Object> users = userpassword.getList(USERS+SLASH+USER+"["+MEMBEROF+"='"+group+"']"+SLASH+AT+DN);
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
        List<Object> groups = userpassword.getList(USERS+SLASH+USER+"["+AT+DN+"='"+foruser+"']"+SLASH+MEMBEROF);
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
        //we only check if the user exists or not.
        if(!userExists(foruser)) {
            throw new ConnectException("NameNotFoundException - the user "+foruser+" doesn't exist in the password file.");
        }
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

            out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
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
            AuthFile authFile = new AuthFile();
            if(argus[1] != null && argus[1].equals(GROUPADD)) {
                handleGroupAdd(authFile,argus);
            } else if (argus[1] != null && argus[1].equals(USERADD)) {
                handleUserAdd(authFile,argus);
            } else if (argus[1] != null && argus[1].equals(USERMOD)) {
                handleUserMod(authFile, argus);
            } else if (argus[1] != null && argus[1].equals(USAGE)) {
                printUsage();
            } else {
                System.out.print("Error: the unknown action "+argus[1]);
            }
    }
    
    /*
     * Handle the groupAdd action in the main method
     */
    private static void handleGroupAdd(AuthFile authFile, String[]argus) throws AuthenticationException {
        HashMap<String, String> map = null;
        String G = "-g";
        String D = "-d";
        Vector<String> pairedOptions = new Vector<String>();
        pairedOptions.add(G);
        pairedOptions.add(D);
        int startIndex = 2;
        try {
            map = parseArgus(startIndex, argus, pairedOptions, null);
        } catch (Exception e ) {
            System.out.println("Error in the groupadd command: "+e.getMessage());
            System.exit(1);
        }
        String groupName = null;
        String description = null;
        if(map.keySet().size() == 0) {
            System.out.println("Error in the groupadd command: the \""+G+" group-name\" is required.");
            System.exit(1);
        } else if(map.keySet().size() ==1 || map.keySet().size() ==2) {
            groupName = map.get(G);
            if(groupName == null || groupName.trim().equals("")) {
                System.out.println("Error in the groupadd command : the \""+G+" group-name\" is required.");
                System.exit(1);
            }
            description = map.get(D);
            authFile.addGroup(groupName, description);
            System.out.println("Successfully added a group \""+groupName+"\" to the file authentication system");
        } else {
            printError(argus);
            System.exit(1);
        }
    }
    
    /*
     * Handle the userAdd action in the main method
     */
    private static void  handleUserAdd(AuthFile authFile,String[]argus) throws UnsupportedEncodingException, AuthenticationException{
      
        String I = "-i";
        String H = "-h";
        String DN = "-dn";
        String G = "-g";
        String E = "-e";
        String S = "-s";
        String F = "-f";
        String O = "-o";
        Vector<String> pairedOptions = new Vector<String>();
        pairedOptions.add(H);
        pairedOptions.add(DN);
        pairedOptions.add(G);
        pairedOptions.add(E);
        pairedOptions.add(S);
        pairedOptions.add(F);
        pairedOptions.add(O);
        Vector<String> singleOptions = new Vector<String>();
        singleOptions.add(I);
        
        HashMap<String, String> map = new HashMap<String, String>();
        int startIndex = 2;
        try {
            map = parseArgus(startIndex, argus, pairedOptions, singleOptions);
        } catch (Exception e) {
            System.out.println("Error in the useradd command: "+e.getMessage());
            System.exit(1);
        }
       
        String dn = map.get(DN);
        if(dn == null || dn.trim().equals("")) {
            System.out.println("The \"-dn user-distinguish-name\" is requried in the useradd command ."); 
            System.exit(1);
        } 
        String plainPassword = null;
        String hashedPassword = null;
       
        
        String input = map.get(I);
        String passHash = map.get(H);
        if(input != null && passHash != null) {
            System.out.println("Error in the useradd command: you only can choose either \"-i\" (input a password) or \"-h hashed-password\" (pass through a hashed passwword).");
            System.exit(1);
        } else if (input == null && passHash == null) {
            System.out.println("Error in the useradd command: you must choose either \"-i\" (input a password) or \"-h hashed-password\" (pass through a hashed password).");
            System.exit(1);
        } else if(input != null) {
            plainPassword = inputPassword();
            //System.out.println("============the plain password is "+plainPassword);
        } else if(passHash != null) {
            hashedPassword = passHash;
        }
        
        String group = map.get(G);
        //System.out.println("the groups name is "+group);
        String[] groups = null;
        if(group != null && !group.trim().equals("")) {
            groups = new String[1];
            groups[0]=group;
            //System.out.println("set the first element of the groups to "+groups[0]);
        }
        String email = map.get(E);
        String surname = map.get(S);
        String givenname = map.get(F);
        String organization = map.get(O);
        authFile.addUser(dn, groups, plainPassword, hashedPassword, email, surname, givenname, organization);
        System.out.println("Successfully added a user \""+dn+"\" to the file authentication system ");
    }
    
    /*
     * Handle modify a user's password or group information.
     */
    private static void handleUserMod(AuthFile authFile, String[] argus) throws AuthenticationException, UnsupportedEncodingException {
        String PASSWORD = "-password";
        String GROUP = "-group";
        if(argus.length < 3) {
            System.out.println("Error: the sub action \"-password\" or \"-group\" should follow the action \"usermod\"");
            System.exit(1);
        } else {
            if(argus[2] != null && argus[2].equals(PASSWORD)) {
                handleModifyPass(authFile, argus);
            } else if (argus[2] != null && argus[2].equals(GROUP)) {
                handleModifyGroup(authFile, argus);
            } else {
                System.out.println("Error: the sub action \""+argus[2]+"\" is unkown in the action \"usermod\"");
                System.exit(1);
            }
        }
    }
    
    /*
     * Handle the action to modify the password of a user
     */
    private static void handleModifyPass(AuthFile authFile, String[] argus) throws UnsupportedEncodingException, AuthenticationException {
        String DN = "-dn";
        String I = "-i";
        String H = "-h";
        Vector<String> pairedOptions = new Vector<String>();
        pairedOptions.add(H);
        pairedOptions.add(DN);
        Vector<String> singleOptions = new Vector<String>();
        singleOptions.add(I);
        HashMap<String, String> map = new HashMap<String, String>();
        int startIndex = 3;
        try {
            map = parseArgus(startIndex, argus, pairedOptions, singleOptions);
        } catch (Exception e) {
            System.out.println("Error in the usermod -password command: "+e.getMessage());
            System.exit(1);
        }
      
        String dn = map.get(DN);
        if(dn == null || dn.trim().equals("")) {
            System.out.println("Error in the usermod -password command: The \"-dn user-distinguish-name\" is requried."); 
            System.exit(1);
        }
        String plainPassword = null;
        String hashedPassword = null;
        
        String input = map.get(I);
        String passHash = map.get(H);
        if(input != null && passHash != null) {
            System.out.println("Error in the usermod -password command: you only can choose either \"-i\" (input a password) or \"-h hashed-password\" (pass through a hashed password).");
            System.exit(1);
        } else if (input == null && passHash == null) {
            System.out.println("Error in the usermod -password command: you must choose either \"-i\" (input a password) or \"-h hashed-password\" (pass through a hashed password).");
            System.exit(1);
        } else if(input != null) {
            plainPassword = inputPassword();
            //System.out.println("============the plain password is "+plainPassword);
            authFile.modifyPassWithPlain(dn, plainPassword);
            System.out.println("Successfully modified the password for the user \""+dn+"\".");
        } else if(passHash != null) {
            hashedPassword = passHash;
            authFile.modifyPassWithHash(dn, hashedPassword);
            System.out.println("Successfully modified the password for the user "+dn+"\".");
        }
    }
    
    /*
     * Handle the action adding/removing a user to/from a group
     */
    private static void handleModifyGroup(AuthFile authFile, String[] argus) throws AuthenticationException {
        String DN = "-dn";
        String A = "-a";
        String R = "-r";
        String G = "-g";
        Vector<String> pairedOptions = new Vector<String>();
        pairedOptions.add(G);
        pairedOptions.add(DN);
        Vector<String> singleOptions = new Vector<String>();
        singleOptions.add(A);
        singleOptions.add(R);
        HashMap<String, String> map = new HashMap<String, String>();
        int startIndex = 3;
        try {
            map = parseArgus(startIndex, argus, pairedOptions, singleOptions);
        } catch (Exception e) {
            System.out.println("Error in the usermod -group command: "+e.getMessage());
            System.exit(1);
        }
               
        String add = map.get(A);
        String remove = map.get(R);
        String group = map.get(G);
        String dn = map.get(DN);
        if(dn == null || dn.trim().equals("")) {
            System.out.println("Error in the usermod -group command: the \"-dn user-distinguish-name\" is required.");
            System.exit(1);
        }
        
        if(group == null || group.trim().equals("")) {
            System.out.println("Error in the usermod -group command: the \"-g group-name\" is required.");
            System.exit(1);
        }
        
        if(add != null && remove!= null) {
            System.out.println("Error in the usermod -group command: You can only choose either \"-a\" (add the user to the group) or \"-r\" (remove the user from the group).");
            System.exit(1);
        } else if (add == null && remove == null) {
            System.out.println("Error in the usermod -group command: You must choose either \"-a\" (add the user to the group) or \"-r\" (remove the user from the group).");
            System.exit(1);
        } else if (remove != null) {
            authFile.removeUserFromGroup(dn, group);
            System.out.println("Successfully removed the user "+dn+" from the group \""+group+"\".");
        } else {
            authFile.addUserToGroup(dn, group);
            System.out.println("Successfully added the user "+dn+" to the group \""+group+"\".");
        }
    }
    
    
    /**
     * Parse the arguments to get the pairs of option/value. If it is a single option (it doesn't need a value), the pair will be switch/switch. 
     * @param startIndex the index of arguments where we will start.
     * @param argus the arguments array will be parsed.
     * @param pairedOptions the known options which should be a pair
     * @param singleOptions the know options which just has a single value
     * @return the empty map if there is no pairs were found
     * @throws Exception if there is an illegal argument.
     */
    private static HashMap<String, String> parseArgus(int startIndex, String[]argus, Vector<String>pairedOptions, Vector<String>singleOptions) throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        if(argus != null) {
            for(int i=startIndex; i<argus.length; i++) {
                String arg = argus[i];
                if(map.containsKey(arg)) {
                    throw new Exception("The command line can't have duplicate options \""+arg+"\".");
                }
                
                if(singleOptions != null && singleOptions.contains(arg)) {
                    //we find a single option
                    if(i==argus.length-1) {
                        //it is the last argument, this is fine.
                        map.put(arg, arg);
                    } else if (i<argus.length -1) {
                        //it is not the last argument. 
                        if ((pairedOptions != null && pairedOptions.contains(argus[i+1])) || singleOptions.contains(argus[i+1])) {
                            //it follows an option, this is fine
                            map.put(arg, arg);
                        } else {
                            //it follows a vlaue, this is illegal
                            throw new Exception("The option \""+arg+"\" shouldn't be followed any value, e.g. "+ argus[i+1]+".");
                        }
                    }
                } else if (pairedOptions != null && pairedOptions.contains(arg)) {
                    //we found an option which should follow a vlaue
                    if(i==argus.length-1) {
                        //it is the last argument (no value follows it)
                        throw new Exception("The option \""+arg+"\" must be followed by a value");
                    } else {
                        //it is not the last argument and we need to check its following value
                        if (!pairedOptions.contains(argus[i+1]) && (singleOptions == null || !singleOptions.contains(argus[i+1]))) {
                            //it is NOT followed by an option, this is fine
                            map.put(arg, argus[i+1]);
                        } else {
                            //it is followed by an option, this is illegal
                            throw new Exception("The option \""+arg+"\" shouldn't be followed the option \""+ argus[i+1]+"\". It should be followed by a value.");
                        }
                    }
                } else {
                    //we found an argument is not an option
                    if(pairedOptions == null || !pairedOptions.contains(argus[i-1])) {
                        //the previous argument is not an option switch
                        throw new Exception("The \""+arg+"\" is an illegal argument");
                    }
                }
            } 
        }
        return map;
    }
    
    /*
     * Input the password
     */
    private static String inputPassword() throws UnsupportedEncodingException {
        String password = null;
        String quit = "q";
        Console console = System.console();
        if (console == null) {
            System.out.println("Sorry, we can't fetch the console from the system. You can't use the option \"-i\" to input a password. You have to use the option \"-h hashed-password\" to pass through a hashed passwprd in the useradd command. ");
            System.exit(1);
        }
  
        while(true) {
                System.out.print("Enter your new password (input 'q' to quit): ");
                String password1 = new String(console.readPassword());
                if(password1== null || password1.trim().equals("")) {
                    System.out.println("Error: the password can't be blank or null. Please try again.");
                    continue;
                } else if (password1.equals(quit)) {
                    System.exit(0);
                }
                System.out.print("Confirm your new password (input 'q' to quit): ");
                String password2 = new String(console.readPassword());
                if(password2 == null || password2.trim().equals("")) {
                    System.out.println("Error: the password can't be blank or null. Please try again.");
                    continue;
                }  else if (password2.equals(quit)) {
                    System.exit(0);
                }
                
                if(!password1.equals(password2)) {
                    System.out.println("Error: The second password does't match the first one. Please try again.");
                } else {
                    password = password1;
                    break;
                }
                
            
        }
        
        return password;
        
    }
    /*
     * Print out the usage statement
     */
    private static void printUsage() {
        System.out.println("Usage:\n"+
                        "./authFileManager.sh useradd -i -dn <user-distinguish-name> [-g <group-name> -e <email-address> -s <surname> -f <given-name> -o <organizationName>]\n" +
                        "./authFileManager.sh useradd -h <hashed-password> -dn <user-distinguish-name> [-g <group-name> -e <email-address> -s <surname> -f <given-name> -o <organizationName>]\n"+
                        "./authFileManager.sh groupadd -g group-name [-d description]\n" +
                        "./authFileManager.sh usermod -password -dn <user-distinguish-name> -i\n"+
                        "./authFileManager.sh usermod -password -dn <user-distinguish-name> -h <new-hashed-password>\n"+
                        "./authFileManager.sh usermod -group -a -dn <user-distinguish-name> -g <added-group-name>\n" +
                        "./authFileManager.sh usermod -group -r -dn <user-distinguish-name> -g <removed-group-name>\n"+
                        "Note:\n"+"1. Metacat currently uses Bcrypt algorithm to hash the password. The hashed password following the \"-h\" should be generated by a Bcrypt algorithm.\n"+
                        "  The hash string usually has $ signs which can interfere with the command line arguments. You should use two SINGLE quotes to wrap the entire hashed string.\n"+
                        "2. The user-distinguish-name must look like \"uid=john,o=something,dc=something,dc=something\" and the group-name must look like \"cn=dev,o=something,dc=something,dc=something\".\n"+
                        "3. if a value of an option has spaces, the value should be enclosed by the double quotes.\n"+
                        "  For example: ./authFileManager.sh groupadd -g cn=dev,o=something,dc=something,dc=something -d \"Developers at NCEAS\"\n"+
                        "4. \"-d description\" in the \"groupadd\" command is optional; \"-g groupname -e email-address -s surname -f given-name -o organizationName\" in the \"useradd\" command are optional as well.");
                       
                        
    }
    
    /*
     * Print out the statement to say it is a illegal command
     */
    private static void printError(String[] argus) {
        if(argus != null) {
            System.out.println("Error: it is an illegal command (probably with some illegal options): ");
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
            List<Object> existingGroups = userpassword.getList(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+SLASH+MEMBEROF);
            if(existingGroups != null && existingGroups.contains(group)) {
                throw new AuthenticationException("AuthFile.User.addUserToGroup - the user "+dn+ " already is the memember of the group "+group);
            }
            userpassword.addProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+" "+MEMBEROF, group);
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
            String key = USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+SLASH+MEMBEROF;
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
            if(groups != null) {
                for(int i=0; i<groups.length; i++) {
                    String group = groups[i];
                    if(group != null && !group.trim().equals("")) {
                        if(!groupExists(group)) {
                            throw new AuthenticationException("AuthFile.User.serialize - can't put the user into a non-existing group "+group);
                        }
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
                                  userpassword.addProperty(USERS+SLASH+USER+"["+AT+DN+"='"+dn+"']"+" "+MEMBEROF, group);
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
