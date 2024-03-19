package edu.ucsb.nceas.metacat.admin;

import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;

/**
 * Control the display of the login page
 */
public class LoginAdmin extends MetacatAdmin {

    private static LoginAdmin Admin = null;
    private static Log logMetacat = LogFactory.getLog(LoginAdmin.class);

    /**
     * private constructor since this is a singleton
     */
    private LoginAdmin() {
    }

    /**
     * Get the single instance of LoginAdmin.
     *
     * @return the single instance of LoginAdmin
     */
    public static LoginAdmin getInstance() {
        if (Admin == null) {
            Admin = new LoginAdmin();
        }
        return Admin;
    }

    /**
     * Utility method to determine if an incoming request action requires intervention by this class
     *
     * @param request  The http request information
     * @param action   The `action` contained in the `MetacatAdminServlet.ACTION_PARAM` parameter
     *                 of the request
     * @return         boolean true if intervention is needed by this class; false otherwise
     */
    public boolean needsLoginAdminHandling(HttpServletRequest request, String action)
        throws MetacatUtilException {

        if (MetacatAdminServlet.ACTION_LOGOUT.equals(action)
            || MetacatAdminServlet.ACTION_LOGIN_MC.equals(action)) {
            logMetacat.debug(
                "Admin action is: " + action + "; intervention by LoginAdmin is required");
            return true;

        } else if (!AuthUtil.isUserLoggedInAsAdmin(request)) {
            logMetacat.debug("User is NOT logged in; intervention by LoginAdmin is required");
            return true;
        }
        return false;
    }

    /**
     * Handle all login-related cases:
     * 1. the user is not yet logged in, and has not started the orcid flow
     * 2. the user is not yet logged in, but is part-way through the orcid flow
     * 3. the user wishes to log out
     *
     * @param request  The http request information
     * @param response The http response to be sent back to the client
     */
    protected void handle(HttpServletRequest request, HttpServletResponse response)
        throws MetacatUtilException, AdminException {

        String action = request.getParameter(MetacatAdminServlet.ACTION_PARAM);
        logMetacat.debug("handling admin-login-related action: " + action);
        action = (action == null) ? "" : action;

        // login flow. 'configureType':
        // ACTION_LOGOUT:       Log user out
        // No action:           (& no auth token in request). Initial page, ready to start flow
        // ACTION_ORCID_FLOW:   user has logged in at orcid.org and has been redirected back to the
        //                      'target' url that was provided to orcid.org. That page then makes an
        //                      async call to the CN to retrieve an auth token
        // ACTION_LOGIN_MC:     (& has auth token in request). Final stage - either authenticates
        //                      and authorizes as an admin, or sends back to login page
        switch (action) {
            case MetacatAdminServlet.ACTION_LOGOUT -> logOutAdminUser(request, response);
            case MetacatAdminServlet.ACTION_ORCID_FLOW -> {
                String msg = "orcid.org login complete; now awaiting token...";
                logMetacat.debug(msg + "; action was " + action);
                addProcessingMessage(request,msg);

                handleOrcidRedirect(request, response);
            }
            case MetacatAdminServlet.ACTION_LOGIN_MC -> {
                String msg = "orcid.org login complete; now verifying token with Metacat...";
                logMetacat.debug(msg + "; action was " + action);
                addProcessingMessage(request,msg);

                doMetacatLogin(request, response);
            }
            default -> {
                logMetacat.debug("Action = " + action
                                     + " and User not logged in; sending to login flow start page");
                addProcessingMessage(request, "You must log in as an administrative user, "
                    + "before you can continue with Metacat configuration.");

                startLoginFlow(request, response);
            }
        }
    }

    /**
     * Begin the login flow
     *
     * @param request  The http request information
     * @param response The http response to be sent back to the client
     * @throws AdminException if unable to forward request
     */
    protected void startLoginFlow(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {
        try {
            forwardToLoginStartPage(request, response, null);
        } catch (MetacatUtilException mue) {
            AdminException adminException = new AdminException(
                "Problem processing login; ca't forward to start page: " + mue.getMessage());
            adminException.fillInStackTrace();
            throw adminException;
        }
    }

    /**
     * Handle the case where the User has authenticated via ORCID and the orcid site has
     * redirected the user here. User does not yet have the token.
     *
     * @param request  The http request information
     * @param response The http response to be sent back to the client
     * @throws AdminException if unable to forward request
     */
    protected void handleOrcidRedirect(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {

        try {
            forwardToLoginStartPage(request, response, MetacatAdminServlet.ACTION_ORCID_FLOW);
        } catch (MetacatUtilException mue) {
            AdminException adminException = new AdminException(
                "Problem processing login; forwarding during orcid flow: " + mue.getMessage());
            adminException.fillInStackTrace();
            throw adminException;
        }
    }

    /**
     * Use the ORCID auth token to log the admin user in
     *
     * @param request  The http request information, including the jwt token, either in the
     *                 'Authorization' header, or an http-only cookie
     * @param response The http response to be sent back to the client
     * @throws AdminException if unable to forward request
     */
    protected void doMetacatLogin(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {

        Vector<String> processingSuccess = new Vector<>();
        Vector<String> processingErrors = new Vector<>();

        String userId = "NOT_SET";
        try {
            userId = AuthUtil.authenticateUserWithCN(request);

            if (AuthUtil.isAdministrator(userId, null)) {
                AuthUtil.setAuthCookie(request, response, -1);
            } else {
                processingErrors.add(userId + " is not on the Administrators list. Please contact"
                                         + " a metacat administrator if you need access.");
            }
        } catch (MetacatUtilException ue) {
            String errorMessage = "Could not log in (" + ue.getMessage() + "). Please try again";
            processingErrors.add(errorMessage);
            logMetacat.error(errorMessage);
        }

        try {
            if (!processingErrors.isEmpty()) {
                RequestUtil.setRequestErrors(request, processingErrors);
                logMetacat.debug("Processing errors found (" + processingErrors
                        + ").  User is not logged in; going back to login start page");
                logOutAdminUser(request, response);

            } else {
                logMetacat.debug("Admin user logged in - authenticated and authorized");
                processingSuccess.add(userId + " logged in as Administrator");
                RequestUtil.clearRequestMessages(request);
                RequestUtil.setRequestSuccess(request, processingSuccess);
                RequestUtil.forwardRequest(request, response,
                                           MetacatAdminServlet.PATH_ADMIN_HOMEPAGE,
                                           null);
            }
        } catch (MetacatUtilException mue) {
            AdminException adminException = new AdminException(
                "Problem while processing login; unable to forward request: " + mue.getMessage());
            adminException.fillInStackTrace();
            throw adminException;
        }
    }

    /**
     * Put user in a logged-out state by removing userId from session, and invalidating auth cookie.
     * Then return to login page
     *
     * @param request  The http request information
     * @param response The http response to be sent back to the client
     * @throws AdminException if unable to invalidate auth cookie or forward request
     */
    protected void logOutAdminUser(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {

        logMetacat.debug(
            "logOutAdminUser - logging out... " + request.getSession().getAttribute("userId"));

        request.getSession().removeAttribute("userId");

        addProcessingMessage(
            request,
            "You have logged out successfully. If you need to log in as a different user, you may"
                + " first need to clear your cookies.");

        try {
            AuthUtil.invalidateAuthCookie(request, response);
            request.getSession().invalidate();
            forwardToLoginStartPage(request, response, MetacatAdminServlet.ACTION_LOGOUT);
        } catch (MetacatUtilException mue) {

            AdminException adminException = new AdminException(
                "Problem processing logout; can't invalidate cookie or forward to start page: "
                    + mue.getMessage());
            adminException.fillInStackTrace();
            throw adminException;
        }
    }

    // Calls cleanRequest(), sets the optional provided `attribute` to `true` in the request, and
    // then forwards to `admin-login.jsp`
    private static void forwardToLoginStartPage(
        HttpServletRequest request, HttpServletResponse response, String attribute) throws MetacatUtilException {

        // clean up all messages except processingErrors
        Vector<String> processingErrors = (Vector<String>)request.getAttribute("processingErrors");
        cleanRequest(request);
        if (processingErrors != null) {
            RequestUtil.setRequestErrors(request, processingErrors);
        }

        if (attribute != null) {
            request.removeAttribute(attribute);
            request.setAttribute(attribute, true);
        }

        logMetacat.debug(
            "forwarding to login start page with PARAMETERS:\n"
                + RequestUtil.getParametersAsString(request)
                + "\nand ATTRIBUTES:\n"
                + RequestUtil.getAttributesAsString(request) + "\n");

        RequestUtil.forwardRequest(request, response, "/admin/admin-login.jsp", null);
    }

    private static void addProcessingMessage(HttpServletRequest request, String processingMessage) {

        Vector<String> messageVector = new Vector<>(1);
        messageVector.add(processingMessage);
        RequestUtil.setRequestMessage(request, messageVector);
    }

    // clear messages and remove all attributes from request except for "processingErrors"
    private static void cleanRequest(HttpServletRequest request) {

        RequestUtil.clearRequestMessages(request);

        Enumeration<String> attribList = request.getAttributeNames();
        if (attribList != null) {
            String next;
            while (attribList.hasMoreElements()) {
                next = attribList.nextElement();
                if (!"processingErrors".equals(next)) {
                    request.removeAttribute(next);
                }
            }
        }
    }

    /**
     * Required override from superclass.
     *
     * Validate the relevant configuration options submitted by the user. There are no options to
     * validate at this time as the user is not submitting a form. Only ORCID authentication is
     * available.
     *
     * @return A vector holding error messages for any fields that fail validation.
     */
    @Override
    protected Vector<String> validateOptions(HttpServletRequest request) {
        return new Vector<>();
    }
}
