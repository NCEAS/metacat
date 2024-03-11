/**
 * '$RCSfile$' Purpose: A Class that implements administrative methods Copyright: 2008 Regents of
 * the University of California and the National Center for Ecological Analysis and Synthesis
 * Authors: Michael Daigle
 *
 * '$Author$' '$Date$' '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307  USA
 */

package edu.ucsb.nceas.metacat.util;

import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;

import edu.ucsb.nceas.metacat.dataone.D1AuthHelper;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public class AuthUtil {

    public static Log logMetacat = LogFactory.getLog(AuthUtil.class);
    public static String DELIMITER = ":";
    public static String ESCAPECHAR = "\\";
    private static final String AUTH_COOKIE_NAME = "jwtToken";


    private static Vector<String> administrators = null;
    private static Vector<String> moderators = null;
    private static Vector<String> allowedSubmitters = null;
    private static Vector<String> deniedSubmitters = null;
    private static final int SESSION_TIMEOUT_MINUTES;
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 180;

    static {
        int sessionTimeoutMins;
        // get the timeout limit
        try {
            sessionTimeoutMins =
                Integer.parseInt(PropertyService.getProperty("auth.timeoutMinutes"));
        } catch (PropertyNotFoundException e) {
            logMetacat.error("No properties value found for auth.timeoutMinutes. Defaulting to "
                                 + DEFAULT_SESSION_TIMEOUT_MINUTES, e);
            sessionTimeoutMins = DEFAULT_SESSION_TIMEOUT_MINUTES;
        }
        SESSION_TIMEOUT_MINUTES = sessionTimeoutMins;
    }

    /**
     * private constructor - all methods are static so there is no need to instantiate.
     */
    private AuthUtil() {
    }

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
     * Get the vector of administrator credentials from metacat.properties and put into global
     * administrators list
     */
    private static void populateAdministrators() throws MetacatUtilException {
        String administratorString = null;
        try {
            administratorString = PropertyService.getProperty("auth.administrators");
        } catch (PropertyNotFoundException pnfe) {
            throw new MetacatUtilException("Could not get metacat property: auth.administrators. "
                                               + "There will be no registered metacat "
                                               + "adminstrators: "
                                               + pnfe.getMessage());
        }
        administrators = split(administratorString, ";", ESCAPECHAR);

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
     * Get the vector of allowed submitter credentials from metacat.properties and put into
     * global allowedSubmitters list
     */
    public static void populateAllowedSubmitters() throws MetacatUtilException {
        String allowedSubmitterString = null;
        try {
            allowedSubmitterString = PropertyService.getProperty("auth.allowedSubmitters");
        } catch (PropertyNotFoundException pnfe) {
            throw new MetacatUtilException(
                "Could not get metacat property: auth.allowedSubmitters. "
                    + "Anyone will be allowed to submit: " + pnfe.getMessage());
        }
        allowedSubmitters = split(allowedSubmitterString, DELIMITER, ESCAPECHAR);
    }

    /**
     * Get the vector of denied submitter credentials from metacat.properties and put into
     * global deniedSubmitters list
     */
    private static void populateDeniedSubmitters() throws MetacatUtilException {
        String deniedSubmitterString = null;
        try {
            deniedSubmitterString = PropertyService.getProperty("auth.deniedSubmitters");
        } catch (PropertyNotFoundException pnfe) {
            throw new MetacatUtilException(
                "Could not get metacat property: auth.deniedSubmitters: " + pnfe.getMessage());
        }
        deniedSubmitters = split(deniedSubmitterString, DELIMITER, ESCAPECHAR);
    }

    /**
     * Get the vector of moderator credentials from metacat.properties and put into global
     * administrators list
     */
    private static void populateModerators() throws MetacatUtilException {
        String moderatorString = null;
        try {
            moderatorString = PropertyService.getProperty("auth.moderators");
        } catch (PropertyNotFoundException pnfe) {
            throw new MetacatUtilException("Could not get metacat property: auth.moderators. "
                                               + "There will be no registered metacat moderators: "
                                               + pnfe.getMessage());
        }
        moderators = split(moderatorString, DELIMITER, ESCAPECHAR);
    }

    /**
     * Authenticate the user against the CN server and/or any locally-configured certificates ()
     * If the login is successful, add the session information to
     * the session list in SessionUtil.
     *
     * @param request the http request.
     */
    public static void logAdminUserIn(HttpServletRequest request) throws MetacatUtilException {

        String userId = getAuthenticatedAdminUserId(request);

        if (userId != null && !userId.isEmpty()) {
            RequestUtil.setUserId(request, userId);
            logMetacat.info("User successfully logged in: " + userId);
        } else {
            throw new MetacatUtilException(
                "Problem logging user in; getAuthenticatedAdminUserId returned: <" + userId + ">");
        }
    }

    /**
     * Checks to see if the user is logged in by retrieving the token from the auth header and
     * seeing if a corresponding Session exists in the global session list.
     *
     * @param request the http request that holds the auth header
     * @return org.dataone.service.types.v1.Session if the user is logged in; null otherwise
     */
//    public static Session getUserSession(HttpServletRequest request) {
//
//        SessionService sessionService = SessionService.getInstance();
//        if (sessionService == null) {
//            return null;
//        }
//        Session session = getAuthenticatedSessionFromRequest(request);
//
//        String sessionId = null;
//        if (session != null) {
//            sessionId = session.getSubject().getValue();
//        }
//
//        if (sessionId != null && sessionService.isSessionRegistered(sessionId)) {
//            // get the registered session data
//            SessionData sessionData = SessionService.getInstance().getRegisteredSession
//            (sessionId);
//            // get the last time the session was accessed
//            Calendar lastAccessedTime = sessionData.getLastAccessedTime();
//            // get the current time and set back "sessionTimoutInt" minutes
//            Calendar now = Calendar.getInstance();
//            now.add(Calendar.MINUTE, -SESSION_TIMEOUT_MINUTES);
//
//            // if the last accessed time is before now minus the timeout,
//            // the session has expired. Unregister it and return false.
//            if (lastAccessedTime.before(now)) {
//                sessionService.unRegisterSession(sessionId);
//                return null;
//            }
//            return session;
//        }
//        return null;
//    }

    /**
     * Attempts to authenticate the request using the auth token included in the request header as
     * "Authorization: Bearer $TOKEN"
     *
     * @param request the HttpServletRequest
     * @return org.dataone.service.types.v1.Session, if authentication is successful. Null otherwise
     */
//    private static Session getAuthenticatedSessionFromRequest(HttpServletRequest request) {
//
//        return PortalCertificateManager.getInstance().getSession(request);
//    }
    private static String getOrcidLast16(String orcid) {
        return orcid.substring(1 + orcid.lastIndexOf('/'));
    }


    /**
     * Checks to see if the user is logged in as admin by first checking if the user is logged in
     * and then seeing if the user's account is on the administrators list in metacat.properties.
     *
     * @param request the http request that holds the login session
     * @return boolean that is true if the user is logged in as admin, false otherwise
     */
    public static boolean isUserLoggedInAsAdmin(HttpServletRequest request)
        throws MetacatUtilException {

        // Can user be authenticated via header token?
        String userId = getAuthenticatedAdminUserId(request);

        if (userId == null || userId.isEmpty()) {
            return false;
        }

        // Can user be authorized as an administrator?
        return isAdministrator(userId, null);
    }

    // Try to authenticate user via jwt token in header.
    private static String getAuthenticatedAdminUserId(HttpServletRequest request) {

        HttpServletRequest requestWithHeader = getRequestWithAuthHeader(request);
        if (requestWithHeader == null) {
            return null;
        }

        Session adminSession = PortalCertificateManager.getInstance().getSession(requestWithHeader);

        if (adminSession == null) {
            logMetacat.debug("Auth token found, but unable to authenticate - "
                                 + "PortalCertificateManager returned a null session");
            return null;
        }
        return adminSession.getSubject().getValue();
    }

    public static HttpServletRequest getRequestWithAuthHeader(HttpServletRequest request) {

        String token = request.getHeader("Authorization");

        if (token != null && !token.isBlank()) {
            logMetacat.debug("Found existing auth header with token in request");
            return request;

        } else {
            logMetacat.debug("No auth token in header; trying cookie...");
            Cookie authCookie = RequestUtil.getCookie(request, AUTH_COOKIE_NAME);
            if (isCookieTokenEmpty(authCookie)) {
                logMetacat.debug("Couldn't find an auth token in cookie, either");
                return null;
            } else {
                logMetacat.debug("Found auth cookie: " + AUTH_COOKIE_NAME);
                if (!authCookie.isHttpOnly()) {
                    logMetacat.warn(
                        "Client security concern: Auth cookie was NOT created as HTTP-ONLY!");
                }
                return new AuthRequestWrapper(request, authCookie.getValue());
            }
        }
    }


    /**
     * Gets the user group names from the login session on the http request
     *
     * @param request
     *            the http request that holds the login session
     * @return String array that holds the user groups
     */
    public static String[] getGroupNames(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        ;
        SessionData sessionData = SessionService.getInstance().getRegisteredSession(sessionId);
        String[] groupNames = {""};

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
    public static String createLDAPString(
        String username, String organization, Vector<String> dnList) throws MetacatUtilException {

        if (username == null || organization == null || dnList == null || dnList.size() == 0) {
            throw new MetacatUtilException(
                "Could not generate LDAP user string.  One of the following is null: username, "
                    + "organization or dnlist");
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
            throw new MetacatUtilException(
                "Could not determine if LDAP is configured: " + pnfe.getMessage());
        }
        return !authConfiguredString.equals(PropertyService.UNCONFIGURED);
    }

    /**
     * Check if the specified user is part of the administrators list
     *
     * @param username the username or subject
     * @param groups   an optional list of the user's groups
     */
    public static boolean isAdministrator(String username, String[] groups)
        throws MetacatUtilException {

        if (username == null && groups == null) {
            logMetacat.warn("received null username AND groups - error in calling code?");
            return false;
        }
        Pattern orcidPattern =
            Pattern.compile("https?\\://orcid\\.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}(\\d|X)");

        boolean isOrcid = (username != null) && orcidPattern.matcher(username).matches();
        String user = isOrcid ? getOrcidLast16(username) : username;

        for (String nextAdmin : getAdministrators()) {
            String admin = isOrcid ? getOrcidLast16(nextAdmin) : nextAdmin;

            if (admin.equals(user)) {
                return true;
            }
            if (groups != null) {
                for (String group : groups) {
                    if (admin.equals(group)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if the specified user is part of the moderators list
     *
     * @param username
     *            the user login credentails
     * @param groups
     *            a list of the user's groups
     */
    public static boolean isModerator(String username, String[] groups)
        throws MetacatUtilException {
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
        boolean allow = onAccessList(getAllowedSubmitters(), username, groups);
        if (!allow) {
            //check if it is the mn subject
            D1AuthHelper helper = new D1AuthHelper(null, null, null, null);
            Session session = buildSession(username, groups);
            try {
                allow = helper.isLocalMNAdmin(session);
            } catch (ServiceFailure e) {
                throw new MetacatUtilException(e.getMessage());
            }
        }
        return allow;
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
        if (logMetacat.isDebugEnabled()) {
            logMetacat.debug("AuthUtil.canInsertOrUpdate - The user is " + username);
            if (groups == null) {
                logMetacat.debug("AuthUtil.canInsertOrUpdate -The group is null");
            } else {
                if (groups.length == 0) {
                    logMetacat.debug("AuthUtil.canInsertOrUpdate -The group is empty");
                } else {
                    logMetacat.debug(
                        "AuthUtil.canInsertOrUpdate -And this user is in the group(s)");
                    for (int i = 0; i < groups.length; i++) {
                        logMetacat.debug("AuthUtil.canInsertOrUpdate -Group " + groups[i]);
                    }
                }
            }
        }

        return (isAllowedSubmitter(username, groups) && !isDeniedSubmitter(username, groups));
    }

    /**
     * Check if the user is on a given access list.  This is true if either the
     * user or the user's group is on the list.
     *
     * @param accessList the list we want to check against
     * @param username the name of the user we want to check
     * @param groups a list of the user's groups
     */
    private static boolean onAccessList(
        Vector<String> accessList, String username, String[] groups) {

        // this should never happen.  All calls to this method should use the
        // appropriate getter to retrieve the accessList.  That should guarentee
        // that the access is at least an empty Vector.
        if (accessList == null) {
            return false;
        }

        // Check that the user is authenticated as an administrator account
        for (String accessString : accessList) {
            // is a user dn
            if (username != null && username.equals(accessString)) {
                logMetacat.debug(
                    "AuthUtil.onAccessList - user " + username + " is in the access list.");
                return true;
            }
            // check the given admin dn is a group dn...
            if (groups != null) {
                // is a group dn
                for (int j = 0; j < groups.length; j++) {
                    if (groups[j] != null && groups[j].equals(accessString)) {
                        logMetacat.debug("AuthUtil.onAccessList - user " + username
                                             + " has a grouup which is in the access list.");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Convert a delimited string to a Vector by splitting on a particular character
     * @param text  the text to be split into components
     * @param delimiter  the string to specify the delimiter
     * @param escapeChar  the string to escape a delimiter.
     * @return a vector holding the values. An empty vector will be returned if the text is null
     * or empty.
     */
    public static Vector<String> split(String text, String delimiter, String escapeChar) {
        Vector<String> results = new Vector<String>();
        if (text != null && text.length() > 0 && delimiter != null && escapeChar != null) {
            String regex = "(?<!" + Pattern.quote(escapeChar) + ")" + Pattern.quote(delimiter);
            logMetacat.debug("AuthUtil.split - The regex is " + regex);
            String[] strArray = text.split(regex);
            if (strArray != null) {
                for (int i = 0; i < strArray.length; i++) {
                    logMetacat.debug("AuthUtil.split - the splitted original value " + strArray[i]);
                    String remove =
                        strArray[i].replaceAll(Pattern.quote(escapeChar + delimiter), delimiter);
                    logMetacat.debug(
                        "AuthUtil.split - the value after removing escaped char is " + remove);
                    results.add(remove);
                }
            }
        }
        return results;
    }

    /**
     * Construct a session object base the given user and group name
     * @param user  the user name for the session
     * @param groups  the groups name for the session
     * @return a session object
     */
    private static Session buildSession(String user, String[] groups) {
        Session session = new Session();
        Subject userSubject = new Subject();
        userSubject.setValue(user);
        session.setSubject(userSubject);
        SubjectInfo subjectInfo = new SubjectInfo();
        Person person = new Person();
        person.setSubject(userSubject);
        if (groups != null && groups.length > 0) {
            for (String groupName : groups) {
                Group group = new Group();
                group.setGroupName(groupName);
                Subject groupSubject = new Subject();
                groupSubject.setValue(groupName);
                group.setSubject(groupSubject);
                subjectInfo.addGroup(group);
                person.addIsMemberOf(groupSubject);
            }
        }
        subjectInfo.addPerson(person);
        session.setSubjectInfo(subjectInfo);
        return session;
    }

    private static boolean isCookieTokenEmpty(Cookie cookie) {
        return cookie == null || cookie.getValue() == null || cookie.getValue().isBlank();
    }


    // Inner class to allow adding token to auth header, since HttpServletRequest is read-only
    private static class AuthRequestWrapper extends HttpServletRequestWrapper {

        private final String tokenHeaderValue;

        public AuthRequestWrapper(HttpServletRequest request, final String token) {
            super(request);
            tokenHeaderValue = "Bearer " + token;
        }

        @Override
        public String getHeader(String name) {

            if ("Authorization".equals(name)) {
                return tokenHeaderValue;
            }
            return super.getHeader(name);
        }
    }
}
