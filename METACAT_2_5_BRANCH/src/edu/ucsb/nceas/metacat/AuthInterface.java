/**
 *  '$RCSfile$'
 *    Purpose: An interface representing the methods that should be
 *             implemented by an authentication service
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
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

package edu.ucsb.nceas.metacat;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Vector;

/**
 * An interface representing the methods that should be
 * implemented by an authentication service.  The authentication service is
 * used to determine if a user is authenticated, and whether they are a member
 * of a particular group.
 */
public interface AuthInterface {
    public static final short USERDNINDEX = 0;
    public static final short USERCNINDEX = 1;
    public static final short USERORGINDEX = 2;
    public static final short USERORGUNITINDEX = 3;
    public static final short USEREMAILINDEX = 4;
    public static final short GROUPNAMEINDEX = 0;
    public static final short GROUPDESINDEX = 1;
    public static final short USERINFOCNINDEX = 0;
    public static final short USERINFOORGANIDEX = 1;
    public static final short USERINFOEMAILINDEX = 2;

  /**
   * Determine if a user/password are valid according to the authentication
   * service.
   *
   * @param user the name of the principal to authenticate
   * @param password the password to use for authentication
   * @returns boolean true if authentication successful, false otherwise
   */
  public boolean authenticate(String user, String password)
         throws ConnectException;

  /**
   * Get all users from the authentication service
   */
  public String[][] getUsers(String user, String password)
         throws ConnectException;

  /**
   * Get information for a user - name, organization and email address. 
   */
  public String[] getUserInfo(String user, String password)
         throws ConnectException;

  /**
   * Get the users for a particular group from the authentication service
   */
  public String[] getUsers(String user, String password, String group)
         throws ConnectException;

  /**
   * Get all groups from the authentication service
   */
  public String[][] getGroups(String user, String password)
         throws ConnectException;

  /**
   * Get the groups for a particular user from the authentication service
   */
  public String[][] getGroups(String user, String password, String foruser)
         throws ConnectException;

  /**
   * Get attributes describing a user or group
   *
   * @param user the user for which the attribute list is requested
   * @returns HashMap a map of attribute name to a Vector of values
   */
  public HashMap<String,Vector<String>> getAttributes(String foruser)
         throws ConnectException;

  /**
   * Get attributes describing a user or group
   *
   * @param user the user for which the attribute list is requested
   * @param authuser the user for authenticating against the service
   * @param password the password for authenticating against the service
   * @returns HashMap a map of attribute name to a Vector of values
   */
  public HashMap<String,Vector<String>> getAttributes(String user, String password, String foruser)
         throws ConnectException;

  /**
   * Get all groups and users from authentication service.
   * The output is formatted in XML.
   * @param user the user which requests the information
   * @param password the user's password
   */
  public String getPrincipals(String user, String password)
                throws ConnectException;
}
