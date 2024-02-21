/**
 * '$RCSfile$'
 * Purpose: A Class that implements login methods
 * Copyright: 2008 Regents of the University of California and the
 * National Center for Ecological Analysis and Synthesis
 * ors: Michael Daigle
 *
 * '$or: daigle $'
 * '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

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
	 * Handle configuration of the Authentication properties
	 * 
	 * @param request
	 *                 the http request information
	 * @param response
	 *                 the http response to be sent back to the client
	 */
	public void authenticateUser(HttpServletRequest request, HttpServletResponse response)
		throws AdminException {

		String processForm = request.getParameter("processForm");
		String formErrors = (String) request.getAttribute("formErrors");

		if (processForm == null || !processForm.equals("true") || formErrors != null) {
			try {
				request.setAttribute("adminList", AuthUtil.getAdministrators());
				// Forward the request to the JSP page
				RequestUtil.forwardRequest(request, response, "/admin/admin-login.jsp", null);
			} catch (MetacatUtilException mue) {
				throw new AdminException(
					"LoginAdmin.authenticateUser - Utility problem while "
						+ "processing login page: " + mue.getMessage()
				);
			}
		} else {
			// The configuration form is being submitted and needs to be processed.
			Vector<String> processingSuccess = new Vector<String>();
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> validationErrors = new Vector<String>();

			// See 'validateOptions' javadoc for more information.
			// validationErrors.addAll(validateOptions(request));

			// TODO:
			// - Extract user name and compare against administrator stored
			// - If it's verified, valid token, all set, we can move forward

			try {
				if (validationErrors.size() > 0 || processingErrors.size() > 0) {
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestFormErrors(request, validationErrors);
					RequestUtil.setRequestErrors(request, processingErrors);
					RequestUtil.forwardRequest(request, response, "/admin", null);
				} else {
					// Reload the main metacat configuration page
					processingSuccess.add("User logged in as: " + "str_placeholder");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setUserId(request, "str_placeholder");
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(
						request, response, "/admin?configureType=configure&processForm=false", null
					);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException(
					"LoginAdmin.authenticateUser - IO problem while processing login page: " + mue
						.getMessage()
				);
			}
		}
	}

	/**
	 * Validate the relevant configuration options submitted by the user.
	 * There are no options to validate at this time as the user is not submitting a form.
	 * Only ORCID authentication is available.
	 * 
	 * @return A vector holding error messages for any fields that fail validation.
	 */
	protected Vector<String> validateOptions(HttpServletRequest request) {
		Vector<String> errorVector = new Vector<String>();
		// There is no configuration option to validate
		return errorVector;
	}
}
