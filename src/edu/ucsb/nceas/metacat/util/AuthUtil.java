/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements administrative methods 
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
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

package edu.ucsb.nceas.metacat.util;

import java.util.Calendar;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.AuthSession;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;

public class AuthUtil {
	
    public static Logger logMetacat = Logger.getLogger(AuthUtil.class);

	private static Vector<String> administrators = null;
	private static Vector<String> moderators = null;
	private static Vector<String> allowedSubmitters = null;
	private static Vector<String> deniedSubmitters = null;

	/**
	 * private constructor - all methods are static so there is no no need to
	 * instantiate.
	 */
	private AuthUtil() {}

	/**
	 * Get the administrators from metacat.properties
	 * 
	 * @return a Vector of Strings holding the administrators
	 */
	public static Vector<String> getAdministrators() throws MetacatUtilException {
		if (administrators == null) {
			populateAdministrators();
		}
		return administrators;
	}
	
	/**
	 * Get the allowed submitters from metacat.properties
	 * 
	 * @return a Vector of Strings holding the submitters
	 */
	public static Vector<String> getAllowedSubmitters() throws MetacatUtilException {
		if (allowedSubmitters == null) {			
			populateAllowedSubmitters();	
		}
		return allowedSubmitters;
	}
	
	/**
	 * Get the denied submitters from metacat.properties
	 * 
	 * @return a Vector of Strings holding the denied submitters
	 */
	public static Vector<String> getDeniedSubmitters() throws MetacatUtilException {
		if (deniedSubmitters == null) {
			populateDeniedSubmitters();
		}
		return deniedSubmitters;
	}
	
	/**
	 * Get the moderators from metacat.properties
	 * 
	 * @return a Vector of Strings holding the moderators
	 */
	public static Vector<String> getModerators() throws MetacatUtilException {
		if (moderators == null) {
			populateModerators();
		}
		return moderators;
	}
	
	/**
	 * Get the vector of administrator credentials from metacat.properties
	 * and put into global administrators list
	 */
	private static void populateAdministrators() throws MetacatUtilException {
		String administratorString = null;
		try {
			administratorString = 
				PropertyService.getProperty("auth.administrators");
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not get metacat property: auth.administrators. "
							+ "There will be no registered metacat adminstrators: "
							+ pnfe.getMessage());
		}
		administrators = StringUtil.toVector(administratorString, ':');
		
		String d1NodeAdmin = null;
		try {
			d1NodeAdmin = PropertyService.getProperty("dataone.subject");
			administrators.add(d1NodeAdmin);
		} catch (PropertyNotFoundException e) {
			String msg = "Could not get metacat property: dataone.subject "
					+ "There will be no registered DataONE adminstrator";
			logMetacat.error(msg, e);
			
		}
	}
	
	/**
	 * Get the vector of allowed submitter credentials from metacat.properties
	 * and put into global allowedSubmitters list
	 */
	private static void populateAllowedSubmitters() throws MetacatUtilException {
		String allowedSubmitterString = null;
		try {
			allowedSubmitterString = PropertyService.getProperty("auth.allowedSubmitters");
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not get metacat property: auth.allowedSubmitters. "
					+ "Anyone will be allowed to submit: "
					+ pnfe.getMessage());
		}		
		allowedSubmitters = StringUtil.toVector(allowedSubmitterString, ':');		
	}
	
	/**
	 * Get the vector of denied submitter credentials from metacat.properties
	 * and put into global deniedSubmitters list
	 */
	private static void populateDeniedSubmitters() throws MetacatUtilException {
		String deniedSubmitterString = null;
		try {
			deniedSubmitterString = PropertyService.getProperty("auth.deniedSubmitters");
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not get metacat property: auth.deniedSubmitters: "
					+ pnfe.getMessage());
		}		
		deniedSubmitters = StringUtil.toVector(deniedSubmitterString, ':');		
	}
	
	/**
	 * Get the vector of moderator credentials from metacat.properties
	 * and put into global administrators list
	 */
	private static void populateModerators() throws MetacatUtilException {
		String moderatorString = null;
		try {
			moderatorString = 
				PropertyService.getProperty("auth.moderators");
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not get metacat property: auth.moderators. "
							+ "There will be no registered metacat moderators: "
							+ pnfe.getMessage());
		}
		moderators = StringUtil.toVector(moderatorString, ':');
	}

	/**
	 * log the user in against ldap.  If the login is successful, add
	 * the session information to the session list in SessionUtil.
	 * 
	 * @param request the http request.
	 */
	public static boolean logUserIn(HttpServletRequest request, String userName, String password) throws MetacatUtilException {
		AuthSession authSession = null;

		// make sure we have username and password.
		if (userName == null || password == null) {
			throw new MetacatUtilException("null username or password when logging user in");
		}

		// Create auth session
		try {
			authSession = new AuthSession();
		} catch (Exception e) {
			throw new MetacatUtilException("Could not instantiate AuthSession: "
					+ e.getMessage());
		}
		// authenticate user against ldap
		if(!authSession.authenticate(request, userName,password)) {
			throw new MetacatUtilException(authSession.getMessage());
		}
		
		// if login was successful, add the session information to the
		// global session list.
		HttpSession session = authSession.getSessions();
		String sessionId = session.getId();
		
		try {
		SessionService.getInstance().registerSession(sessionId, 
				(String) session.getAttribute("username"), 
				(String[]) session.getAttribute("groupnames"),
				(String) session.getAttribute("password"),
				(String) session.getAttribute("name"));
		} catch (ServiceException se) {
			throw new MetacatUtilException("Problem registering session: " + se.getMessage());
		}
		
		return true;
	}

	/**
	 * Checks to see if the user is logged in by grabbing the session from the
	 * request and seeing if it exists in the global session list.
	 * 
	 * @param request the http request that holds the login session
	 * @return boolean that is true if the user is logged in, false otherwise
	 */
	public static boolean isUserLoggedIn(HttpServletRequest request) throws MetacatUtilException{
		SessionData sessionData = null;
		String sessionId = request.getSession().getId();

		try {

			if (sessionId != null && SessionService.getInstance().isSessionRegistered(sessionId)) {
				// get the registered session data
				sessionData = SessionService.getInstance().getRegisteredSession(sessionId);

				// get the timeout limit
				String sessionTimeout = PropertyService.getProperty("auth.timeoutMinutes");
				int sessionTimeoutInt = Integer.parseInt(sessionTimeout);

				// get the last time the session was accessed
				Calendar lastAccessedTime = sessionData.getLastAccessedTime();
				// get the current time and set back "sessionTimoutInt" minutes
				Calendar now = Calendar.getInstance();
				now.add(Calendar.MINUTE, 0 - sessionTimeoutInt);

				// if the last accessed time is before now minus the timeout,
				// the session has expired. Unregister it and return false.
				if (lastAccessedTime.before(now)) {
					SessionService.getInstance().unRegisterSession(sessionId);
					return false;
				}

				return true;
			}
			
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not determine if user is logged in because " 
					+ "of property error: " + pnfe.getMessage());
		} catch (NumberFormatException nfe) {
			throw new MetacatUtilException("Could not determine if user is logged in because " 
					+ "of number conversion error: " + nfe.getMessage());
		}

		return false;
	}

	/**
	 * Checks to see if the user is logged in as admin by first checking if the
	 * user is logged in and then seeing if the user's account is on the
	 * administrators list in metacat.properties.
	 * 
	 * @param request
	 *            the http request that holds the login session
	 * @return boolean that is true if the user is logged in as admin, false
	 *         otherwise
	 */
	public static boolean isUserLoggedInAsAdmin(HttpServletRequest request) throws MetacatUtilException {
		if (!isUserLoggedIn(request)) {
			return false;
		}

		String userName = getUserName(request);
		boolean isAdmin = isAdministrator(userName, null);

		return isAdmin;
	}

	/**
	 * Gets the user name from the login session on the http request
	 * 
	 * @param request
	 *            the http request that holds the login session
	 * @return String that holds the user name
	 */
	public static String getUserName(HttpServletRequest request) {
		String userName = (String)request.getSession().getAttribute("username");

		return userName;
	}

	/**
	 * Gets the user group names from the login session on the http request
	 * 
	 * @param request
	 *            the http request that holds the login session
	 * @return String array that holds the user groups
	 */
	public static String[] getGroupNames(HttpServletRequest request) {
		String sessionId = request.getSession().getId();;
		SessionData sessionData = SessionService.getInstance().getRegisteredSession(sessionId);
		String[] groupNames = { "" };

		if (sessionData != null) {
			groupNames = sessionData.getGroupNames();
		}

		return groupNames;
	}

	/**
	 * Creates an ldap credentail string from the username, organization
	 * and dn list.
	 * 
	 * @param username the user name
	 * @param organization the organization
	 * @param dnList a list of dns
	 * @return String holding the ldap login string
	 */	
	public static String createLDAPString(String username, String organization,
			Vector<String> dnList) throws MetacatUtilException {

		if (username == null || organization == null || dnList == null || dnList.size() == 0) {
			throw new MetacatUtilException("Could not generate LDAP user string.  One of the following is null: username, organization or dnlist");
		}

		String ldapString = "uid=" + username + ",o=" + organization;

		for (String dn : dnList) {
			ldapString += "," + dn;
		}

		return ldapString;
	}

	/**
	 * Reports whether LDAP is fully configured.
	 * 
	 * @return a boolean that is true if all sections are configured and false
	 *         otherwise
	 */
	public static boolean isAuthConfigured() throws MetacatUtilException {
		String authConfiguredString = PropertyService.UNCONFIGURED;
		try {
			authConfiguredString = PropertyService.getProperty("configutil.authConfigured");
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not determine if LDAP is configured: "
					+ pnfe.getMessage());
		}
		return !authConfiguredString.equals(PropertyService.UNCONFIGURED);
	}

	/**
	 * Check if the specified user is part of the administrators list
	 * 
	 * @param username
	 *            the user login credentails
	 * @param groups
	 *            a list of the user's groups
	 */
	public static boolean isAdministrator(String username, String[] groups)
			throws MetacatUtilException {
		return onAccessList(getAdministrators(), username, groups);
	}

	/**
	 * Check if the specified user is part of the moderators list
	 * 
	 * @param username
	 *            the user login credentails
	 * @param groups
	 *            a list of the user's groups
	 */
	public static boolean isModerator(String username, String[] groups) throws MetacatUtilException{
		return onAccessList(getModerators(), username, groups);
	}

	/**
	 * Check if the specified user is part of the moderators list
	 * 
	 * @param username
	 *            the user login credentails
	 * @param groups
	 *            a list of the user's groups
	 */
	public static boolean isAllowedSubmitter(String username, String[] groups)
			throws MetacatUtilException {
		if (getAllowedSubmitters().size() == 0) {
			// no allowedSubmitters list specified -
			// hence everyone should be allowed
			return true;
		}
		return (onAccessList(getAllowedSubmitters(), username, groups));
	}

	/**
	 * Check if the specified user is part of the moderators list
	 * 
	 * @param username
	 *            the user login credentails
	 * @param groups
	 *            a list of the user's groups
	 */
	public static boolean isDeniedSubmitter(String username, String[] groups)
			throws MetacatUtilException {
		return (onAccessList(getDeniedSubmitters(), username, groups));
	}

	/**
	 * Check if the specified user can insert the document
	 * 
	 * @param username
	 *            the user login credentails
	 * @param groups
	 *            a list of the user's groups
	 */
	public static boolean canInsertOrUpdate(String username, String[] groups)
			throws MetacatUtilException {
		return (isAllowedSubmitter(username, groups) && !isDeniedSubmitter(username,
				groups));
	}

	/**
	 * Check if the user is on a given access list.  This is true if either the 
	 * user or the user's group is on the list.
	 * 
	 * @param accessList the list we want to check against
	 * @param username the name of the user we want to check
	 * @param groups a list of the user's groups
	 */
	private static boolean onAccessList(Vector<String> accessList, String username,
			String[] groups) {

		// this should never happen.  All calls to this method should use the 
		// appropriate getter to retrieve the accessList.  That should guarentee
		// that the access is at least an empty Vector.
		if (accessList == null) {
			return false;
		}

		// Check that the user is authenticated as an administrator account
		for (String accessString : accessList) {
			// check the given admin dn is a group dn...
			if (groups != null && accessString.startsWith("cn=")) {
				// is a group dn
				for (int j = 0; j < groups.length; j++) {
					if (groups[j] != null && groups[j].equals(accessString)) {
						return true;
					}
				}
			} else {
				// is a user dn
				if (username != null && username.equals(accessString)) {
					return true;
				}
			}
		}
		return false;
	}

}
