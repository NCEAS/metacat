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
import javax.servlet.http.HttpServletResponse;

import edu.ucsb.nceas.utilities.StringUtil;
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
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public class AuthUtil {

    public static Log logMetacat = LogFactory.getLog(AuthUtil.class);
    public static String DELIMITER = ":";
    public static String ESCAPECHAR = "\\";
    public static final String AUTH_COOKIE_NAME = "jwtToken";


    private static Vector<String> administrators = null;
    private static Vector<String> moderators = null;
    private static Vector<String> allowedSubmitters = null;
    private static Vector<String> deniedSubmitters = null;
    private static final Pattern ORCID_PATTERN =
        Pattern.compile("https?://orcid\\.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}(\\d|X)");

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
                                               + "administrators: "
                                               + pnfe.getMessage());
        }
        administrators = StringUtil.toVector(administratorString, ';');

        String d1NodeAdmin = null;
        try {
            d1NodeAdmin = PropertyService.getProperty("dataone.subject");
            administrators.add(d1NodeAdmin);
        } catch (PropertyNotFoundException e) {
            String msg = "Could not get metacat property: dataone.subject "
                + "There will be no registered DataONE administrator";
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
     * @return the userId if successful
     * @throws MetacatUtilException if there's a problem adding the userId to the session
     */
    public static String authenticateUserWithCN(HttpServletRequest request) throws MetacatUtilException {

        String userId = getCNAuthenticatedUserId(request);

        if (userId != null && !userId.isBlank()) {
            RequestUtil.setUserId(request, userId);
            logMetacat.info("User successfully authenticated with CN: " + userId);
        } else {
            throw new MetacatUtilException(
                "Problem authenticating user with CN; getCNAuthenticatedUserId returned: <" + userId + ">");
        }
        return userId;
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

        String userId = (String) request.getSession().getAttribute(RequestUtil.ATTR_USER_ID);
        if (userId == null || userId.isBlank()) {
            return false;
        }

        // Can user be authorized as an administrator?
        return isAdministrator(userId, null);
    }

    // Try to authenticate user via jwt token in header.
    private static String getCNAuthenticatedUserId(HttpServletRequest request) {

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

    /**
     * Look for a valid jwt auth token, first in the request Authorization header, or if not
     * found there, in a secure http-only cookie. The returned HttpServletRequest will either be
     * null, if no token was found, or will contain an Authorization header containing the token
     *
     * @param request  an HttpServletRequest possibly containing a jwt token in either an
     *                  'Authorization' header, or in a secure http-only cookie (@see
     *                  AUTH_COOKIE_NAME)
     * @return an HttpServletRequest containing the token in an 'Authorization' header, or null if
     *                  no token was found
     */
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
     * Look for a valid jwt auth token, first in the request Authorization header, or if not
     * found there, in a secure http-only cookie. Then return the token as a String, or null if
     * no token was found.
     *
     * @param request  an HttpServletRequest possibly containing a jwt token in either an
     *                  'Authorization' header, or in a secure http-only cookie (@see
     *                  AUTH_COOKIE_NAME)
     * @return a String containing the token
     */
    public static String getTokenFromRequest(HttpServletRequest request) {

        HttpServletRequest requestWithHeader = getRequestWithAuthHeader(request);
        if (requestWithHeader == null) {
            logMetacat.debug("No auth token found in request");
            return null;
        }
        //Authorization: Bearer $TOKEN
        String header = requestWithHeader.getHeader("Authorization");
        String token = null;
        if (header != null && !header.isBlank()
            && header.startsWith("Bearer ") && header.length() > 7) {
            token = header.substring(7);  // Bearer $TOKEN
            logMetacat.debug(
                "Found auth header with token from request: " + token.substring(0,5) + "...");
        } else {
            logMetacat.debug("Found non-valid auth token header: <" + header + ">");
        }
        return token;
    }

    /**
     * Set a secure auth cookie that contains the jwt token
     *
     * @param request
     * @param response
     * @param timeoutMins the timeout value to set on the cookie. If timeoutMins is negative, the
     *                    value will be set from the "session.timeoutMinutes" config property
     * @throws MetacatUtilException
     */
    public static void setAuthCookie(HttpServletRequest request, HttpServletResponse response,
                                     int timeoutMins) throws MetacatUtilException {

        String token = getTokenFromRequest(request);
        if (token == null || token.isBlank()) {
            MetacatUtilException mue = new MetacatUtilException("Could not get token from request");
            mue.fillInStackTrace();
            throw mue;
        }

        boolean isHttps;
        String domain;
        try {
            isHttps = Boolean.parseBoolean(PropertyService.getProperty("server.https"));
            domain = PropertyService.getProperty("server.name");
            if (timeoutMins < 0) {
                timeoutMins =
                    Integer.parseInt(PropertyService.getProperty("session.timeoutMinutes"));
            }
        } catch (PropertyNotFoundException e) {
            MetacatUtilException mue = new MetacatUtilException(
                "Error getting properties: 'server.https' or 'server.name'");
            mue.fillInStackTrace();
            throw mue;
        }
        String cookieHeader = AUTH_COOKIE_NAME + "=" + token
                + "; Domain=" + domain + "; "
                + ((isHttps) ? "Secure; " : "")
                + "HttpOnly; SameSite=Strict; Max-Age=" + (60 * timeoutMins);

        response.setHeader("Set-Cookie", cookieHeader);
    }

    public static void invalidateAuthCookie(
        HttpServletRequest request, HttpServletResponse response) throws MetacatUtilException {
        setAuthCookie(request, response, 0);
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

        boolean isOrcid = (username != null) && ORCID_PATTERN.matcher(username).matches();
        if (!isOrcid) {
            logMetacat.warn("received an admin username that is not an ORCID: " + username);
        }

        for (String nextAdmin : getAdministrators()) {

            if (nextAdmin.equals(username)) {
                return true;
            }
            if (groups != null) {
                for (String group : groups) {
                    if (nextAdmin.equals(group)) {
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
     *            the user login credentials
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
     *            the user login credentials
     * @param groups
     *            a list of the user's groups
     */
    public static boolean isAllowedSubmitter(String username, String[] groups)
        throws MetacatUtilException {
        if (getAllowedSubmitters().isEmpty()) {
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
     *            the user login credentials
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
     *            the user login credentials
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
                    for (String group : groups) {
                        logMetacat.debug("AuthUtil.canInsertOrUpdate -Group " + group);
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
        // appropriate getter to retrieve the accessList.  That should guarantee
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
                for (String group : groups) {
                    if (group != null && group.equals(accessString)) {
                        logMetacat.debug("AuthUtil.onAccessList - user " + username
                                             + " has a group which is in the access list.");
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
        Vector<String> results = new Vector<>();
        if (text != null && !text.isBlank() && delimiter != null && escapeChar != null) {
            String regex = "(?<!" + Pattern.quote(escapeChar) + ")" + Pattern.quote(delimiter);
            logMetacat.debug("AuthUtil.split - The regex is " + regex);
            String[] strArray = text.split(regex);
            if (strArray != null) {
                for (String s : strArray) {
                    logMetacat.debug("AuthUtil.split - the split original value " + s);
                    String remove = s.replaceAll(Pattern.quote(escapeChar + delimiter), delimiter);
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
        if (groups != null) {
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
