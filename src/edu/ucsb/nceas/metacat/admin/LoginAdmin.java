package edu.ucsb.nceas.metacat.admin;

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
     * Handle all login-related cases:
     * 1. the user is not yet logged in, and has not started the orcid flow
     * 2. the user is not yet logged in, but is part-wat through the orcid flow
     * 3. the user wishes to log out
     *
     * @param request
     * @param response
     */
    protected void handle(HttpServletRequest request, HttpServletResponse response)
        throws MetacatUtilException, AdminException {

        String action = request.getParameter(MetacatAdminServlet.ACTION_PARAM);
        logMetacat.debug("handling admin-login-related action: " + action);

        // login flow. 'configureType':
        // ACTION_LOGOUT:       Log user out
        // No action:           (& no auth token in request). Initial page, ready to start flow
        // ACTION_ORCID_FLOW:   user has logged in at orcid.org and has been redirected back to the
        //                      'target' url that was provided to orcid.org. That page then makes an
        //                      async call to the CN to retrieve an auth token
        // ACTION_LOGIN_MC:     (& has auth token in request). Final stage - either authenticates
        //                      and authorizes as an admin, or sends back to login page
        switch (action) {
            case MetacatAdminServlet.ACTION_LOGOUT -> {
                logOutAdminUser(request, response);
            }
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
            default -> { // includes the case where action == null
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
     * @throws AdminException
     */
    protected void startLoginFlow(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {
        try {
            forwardToLoginStartPage(request, response);
        } catch (MetacatUtilException mue) {
            throw new AdminException(
                "Utility problem while processing login page: " + mue.getMessage());
        }
    }

    /**
     * Handle the case where the User has authenticated via ORCID and should be providing the
     * jwt token, either in auth header or in an http-only cookie
     *
     * @param request  The http request information
     * @param response The http response to be sent back to the client
     * @throws AdminException
     */
    protected void handleOrcidRedirect(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {

        setAttributeTrue(request, MetacatAdminServlet.ACTION_ORCID_FLOW);
        try {
            forwardToLoginStartPage(request, response);
        } catch (MetacatUtilException mue) {
            throw new AdminException(
                "Utility problem while processing login page: " + mue.getMessage());
        }
    }

    /**
     * Use the ORCID auth token to log the admin user in
     *
     * @param request  The http request information
     * @param response The http response to be sent back to the client
     * @throws AdminException
     */
    protected void doMetacatLogin(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {

        Vector<String> processingSuccess = new Vector<>();
        Vector<String> processingErrors = new Vector<>();

        try {
            AuthUtil.logAdminUserIn(request);

        } catch (MetacatUtilException ue) {
            String errorMessage = "Could not log in: " + ue.getMessage() + ". Please try again";
            processingErrors.add(errorMessage);
            logMetacat.error(errorMessage);
        }

        try {
            if (!processingErrors.isEmpty()) {
                RequestUtil.clearRequestMessages(request);
                RequestUtil.setRequestErrors(request, processingErrors);
                logMetacat.debug("Processing errors found (" + processingErrors
                        + ").  User is not authenticated; going back to login start page");
                forwardToLoginStartPage(request, response);

            } else {
                logMetacat.debug("LoginAdmin - User has been authenticated and authorized");
                processingSuccess.add("User logged in");
                RequestUtil.clearRequestMessages(request);
                RequestUtil.setRequestSuccess(request, processingSuccess);
                RequestUtil.forwardRequest(request, response,
                                           MetacatAdminServlet.PATH_ADMIN_HOMEPAGE,
                                           null);
            }
        } catch (MetacatUtilException mue) {
            AdminException adminException = new AdminException(
                "LoginAdmin.logInAdminUser - IO problem while processing login page: "
                    + mue.getMessage());
            adminException.fillInStackTrace();
            throw adminException;
        }
    }

    /**
     * Put user in a logged-out state and return to login page
     * TODO - NEED TO INVALIDATE D1_PORTAL TOKEN. INVALIDATE ORCID LOGIN TOO?
     *
     * @param request  The http request information
     * @param response The http response to be sent back to the client
     * @throws AdminException
     */
    protected void logOutAdminUser(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {

        logMetacat.debug(
            "logOutAdminUser - logging out..." + request.getSession().getAttribute("userId"));

        request.getSession().removeAttribute("userId");
        setAttributeTrue(request, MetacatAdminServlet.ACTION_LOGOUT);

        addProcessingMessage(request, "Successfully logged out");
        try {
            forwardToLoginStartPage(request, response);
        } catch (MetacatUtilException mue) {
            throw new AdminException(
                "Problem processing logout: " + mue.getMessage());
        }
    }

    private static void forwardToLoginStartPage(
        HttpServletRequest request, HttpServletResponse response) throws MetacatUtilException {

        request.setAttribute("adminList", AuthUtil.getAdministrators());
        logMetacat.debug(
            "forwarding to Login start Page with params: " + RequestUtil.getParameters(request));
        // Forward the request to the JSP page
        RequestUtil.forwardRequest(request, response,
                                   "/admin/admin-login.jsp", null);
    }

    /**
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

    private static void addProcessingMessage(HttpServletRequest request, String processingMessage) {

        Vector<String> messageVector = new Vector<>(1);
        messageVector.add(processingMessage);
        RequestUtil.setRequestMessage(request, messageVector);
    }

    private static void setAttributeTrue(HttpServletRequest request, String attribute) {
        request.removeAttribute(attribute);
        request.setAttribute(attribute, true);
        System.out.println("request.getAttributeNames()");

    }


}
