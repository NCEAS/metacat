package edu.ucsb.nceas.metacat.admin;

import java.util.Set;
import java.util.SortedMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.MetaDataProperty;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.SortedProperties;
import edu.ucsb.nceas.utilities.StringUtil;

/**
 * Control the display of the Authentication configuration page and the processing
 * of the configuration values.
 */
public class AuthAdmin extends MetacatAdmin {

	private static AuthAdmin authAdmin = null;
	private static Log logMetacat = LogFactory.getLog(AuthAdmin.class);
    public static final String FILECLASS = "edu.ucsb.nceas.metacat.authentication.AuthFile";
	public static final String LDAPCLASS = "edu.ucsb.nceas.metacat.AuthLdap";

	/**
	 * Private constructor since this is a singleton
	 */
	private AuthAdmin() {
	}

	/**
	 * Get the single instance of the MetaCatConfig.
	 *
	 * @return the single instance of MetaCatConfig
	 */
	public static AuthAdmin getInstance() {
		if (authAdmin == null) {
			authAdmin = new AuthAdmin();
		}
		return authAdmin;
	}

	/**
	 * Handle configuration of the Authentication properties
	 *
	 * @param request  the http request information
	 * @param response the http response to be sent back to the client
	 */
	public void configureAuth(HttpServletRequest request, HttpServletResponse response)
		throws AdminException {

		String processForm = request.getParameter("processForm");
		String formErrors = (String) request.getAttribute("formErrors");

		if (processForm == null || !processForm.equals("true") || formErrors != null) {
			// The servlet configuration parameters have not been set, or there
			// were form errors on the last attempt to configure, so redirect to
			// the web form for configuring metacat

			try {
				// Load the properties metadata file so that the JSP page can
				// use the metadata to construct the editing form
				PropertiesMetaData metadata = PropertyService.getAuthMetaData();
				request.setAttribute("metadata", metadata);
				request.setAttribute("groupMap", metadata.getGroups());

				// Add the list of auth options and their values to the request
				Vector<String> propertyNames = PropertyService.getPropertyNamesByGroup("auth");
				for (String name : propertyNames) {
					request.setAttribute("auth.administrators", PropertyService.getProperty(name));
				}

				// Add the list of organization options and their values to the request.
				// This is because currently we use the 'organization.unaffiliated' values
				// to configure metacat client for password change and account creation.
				// Eventually, these should get moved to organization level configuration.
				Vector<String> orgPropertyNames = PropertyService.getPropertyNamesByGroup(
					"organization"
				);
				for (String name : orgPropertyNames) {
					request.setAttribute(name, PropertyService.getProperty(name));
				}

				// Check for any backup properties and apply them to the request - these are
				// properties from previous configurations.
				// They keep the user from having to re-enter all values when upgrading.
				// If this is a first time install, 'getAuthBackupProperties' will return null.
				SortedProperties backupProperties = PropertyService.getAuthBackupProperties();
				if (backupProperties != null) {
					Vector<String> backupKeys = backupProperties.getPropertyNames();
					for (String key : backupKeys) {
						String value = backupProperties.getProperty(key);
						if (value != null) {
							request.setAttribute(key, value);
						}
					}
				}
				// Forward the request to the JSP page
				RequestUtil.forwardRequest(
					request, response, "/admin/auth-configuration.jsp", null
				);
			} catch (GeneralPropertyException gpe) {
				throw new AdminException(
					"AuthAdmin.configureAuth - Problem getting property "
						+ "while initializing LDAP properties page: " + gpe.getMessage()
				);
			} catch (MetacatUtilException mue) {
				throw new AdminException(
					"AuthAdmin.configureAuth - Utility problem while initializing "
						+ "LDAP properties page:" + mue.getMessage()
				);
			}
		} else {
			// The configuration form is being submitted and needs to be processed.
			Vector<String> processingSuccess = new Vector<String>();
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> validationErrors = new Vector<String>();

			try {
				// For each property, check if it has changed and save it
				PropertiesMetaData authMetaData = PropertyService.getAuthMetaData();

				// Process the fields for the global options (group 1)
				// Only ORCID will be processed as LDAP and password-based fields are deprecated.
				SortedMap<Integer, MetaDataProperty> globalPropertyMap = authMetaData
					.getPropertiesInGroup(1);
				Set<Integer> globalPropertyIndexes = globalPropertyMap.keySet();
				// Set properties/options in memory to prepare to persist
				for (Integer globalPropertyIndex : globalPropertyIndexes) {
					String globalPropertyKey = globalPropertyMap.get(
						globalPropertyIndex).getKey();
					PropertyService.checkAndSetProperty(request,
														globalPropertyKey);
				}

				// Write the options from memory to the properties file
				PropertyService.persistProperties();
				PropertyService.syncToSettings();

				// Validate that the options provided are legitimate.
				// Note: We've allowed them to persist their entries. As of this point there
				// is no other easy way to go back to the configure form and preserve their entries.
				validationErrors.addAll(validateOptions(request));

				// Write out the configurable properties to a backup file outside the install
				// directory. Note: We allow them to do this even if they have validation errors.
				// They will need to go back and fix the errors before they can run metacat.

				// This is a special case, since it is possible that the backup directory
				// may not have been specified yet.  If not, authentication values need to be
				// persisted by the BackupAdmin when the backup directory is specified.
				String backupDir = PropertyService.getProperty("application.backupDir");
				if (backupDir != null) {
					PropertyService.persistAuthBackupProperties();
				}

			} catch (GeneralPropertyException gpe) {
				String errorMessage =
					"AuthAdmin.configureAuth - Problem getting or setting property while "
						+ "processing properties page: " + gpe.getMessage();
				logMetacat.error(errorMessage);
				processingErrors.add(errorMessage);
			}

			try {
				if (validationErrors.size() > 0 || processingErrors.size() > 0) {
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestFormErrors(request, validationErrors);
					RequestUtil.setRequestErrors(request, processingErrors);
					RequestUtil.forwardRequest(request, response, "/admin", null);
				} else {
					// Now that the options have been set, change the
					// 'authConfigured' option to 'true'
					PropertyService.setProperty(
						"configutil.authConfigured", PropertyService.CONFIGURED
					);

					// Reload the main metacat configuration page
					processingSuccess.add("Authentication successfully configured");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(
						request, response, "/admin?configureType=configure&processForm=false", null
					);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException(
					"AuthAdmin.configureAuth - utility problem forwarding request while "
						+ "processing LDAP properties page: " + mue.getMessage()
				);
			} catch (GeneralPropertyException gpe) {
				String errorMessage =
					"AuthAdmin.configureAuth - Problem getting or setting property while "
						+ "processing Authentication properties page: " + gpe.getMessage();
				logMetacat.error(errorMessage);
				processingErrors.add(errorMessage);
			}
		}
	}

	/**
	 * Validate that a user has supplied an ORCID identifier by parsing the http request's
	 * 'auth.administrators' parameter.
	 *
	 * AuthSessions were previously created based on a selected authentication class.
	 * As LDAP and Password based authentication is being deprecated, we no longer have
	 * to check for a valid authentication class. The user is now (2024) expected
	 * to provide an ORCID ID of the form http://orcid.org/0000-0003-0958-4367 (http, NOT https)
	 *
	 * @param request Http request
	 * @return a vector holding error message for any fields that fail validation.
	 */
	protected Vector<String> validateOptions(HttpServletRequest request) {
		Vector<String> errorVector = new Vector<String>();
		String adminUsers = request.getParameter("auth.administrators");
		Vector<String> adminUserList = StringUtil.toVector(adminUsers, ';');

		// Ensure that user has supplied an ID
		if (adminUserList.size() == 0) {
			errorVector.add("Error: must provide at least one valid ORCID identifier.");
		} else {
			// match ORCID ID of the form http://orcid.org/0000-0003-0958-4367 (http, NOT https)
			String regex = "http://orcid.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}(\\d|X)";
			Pattern pattern = Pattern.compile(regex);
			for (String adminUser : adminUserList) {
				if (adminUser.isBlank()) {
					continue;
				}
				Matcher matcher = pattern.matcher(adminUser);
				boolean matched = matcher.matches();
				if (!matched) {
					if (adminUser.startsWith("https")) {
						errorVector.add(
							"Error: ORCID identifiers must start with http:, not https:");
					} else {
						errorVector.add("Error: ORCID identifiers must be of the form: "
											+ "http://orcid.org/0000-0003-0958-4367 or "
											+ "http://orcid.org/0000-0003-0589-346X");
					}
				}
			}
		}

		// If there is an error, user will have to correct the mistake.
		return errorVector;
	}
}
