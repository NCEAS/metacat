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
	 * Handle configuration of the Authentication properties
	 *
	 * @param request  The http request information
	 * @param response The http response to be sent back to the client
	 * @throws AdminException
	 */
	public void authenticateUser(
		HttpServletRequest request, HttpServletResponse response)
		throws AdminException {

		String processForm = request.getParameter("processForm");
		String formErrors = (String) request.getAttribute("formErrors");

        if (processForm == null || !processForm.equals("true") || formErrors != null) {
			// The servlet configuration parameters have not been set, or there
			// were form errors on the last attempt to configure, so redirect to
			// the web form for configuring metacat

			try {
				request.setAttribute("adminList", AuthUtil.getAdministrators());
				// Forward the request to the JSP page
				RequestUtil.forwardRequest(request, response,
										   "/admin/admin-login.jsp", null);
			} catch (MetacatUtilException mue) {
				throw new AdminException("LoginAdmin.authenticateUser - Utility problem while " +
											 "processing login page: " + mue.getMessage());
			}
		} else {
			// The configuration form is being submitted and needs to be processed.
			Vector<String> processingSuccess = new Vector<>();
			Vector<String> processingErrors = new Vector<>();
			Vector<String> validationErrors = new Vector<>();

			try {
				AuthUtil.logUserIn(request);
			} catch (MetacatUtilException ue) {
				String errorMessage =
					"LoginAdmin.authenticateUser - Could not log in: " + ue.getMessage()
						+ ". Please try again";
				processingErrors.add(errorMessage);
				logMetacat.error(errorMessage);
			}

			try {
				if (!processingErrors.isEmpty()) {
					logMetacat.debug(
						"LoginAdmin - User is not authenticated, redirecting user " + "back home.");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestFormErrors(request, validationErrors);
					RequestUtil.setRequestErrors(request, processingErrors);
					RequestUtil.forwardRequest(request, response, "/admin", null);
				} else {
					logMetacat.debug("LoginAdmin - User has been authenticated and authorized");
					// Reload the main metacat configuration page
					processingSuccess.add("User logged in");
					RequestUtil.clearRequestMessages(request);
//				RequestUtil.setUserId(request, adminTokenUser);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(
						request, response, "/admin?configureType=configure&processForm=false",
						null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException(
					"LoginAdmin.authenticateUser - IO problem while processing login page: "
						+ mue.getMessage());
			}
		}
	}

    /**
	 * Validate the relevant configuration options submitted by the user. There are no options to
	 * validate at this time as the user is not submitting a form. Only ORCID authentication is
	 * available.
	 *
	 * @return A vector holding error messages for any fields that fail validation.
	 */
	protected Vector<String> validateOptions(HttpServletRequest request) {
		return new Vector<>();
	}
}
