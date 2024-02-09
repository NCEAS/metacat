/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements ldap configuration methods
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

package edu.ucsb.nceas.metacat.admin;

import java.net.ConnectException;
import java.util.Set;
import java.util.SortedMap;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.AuthSession;
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
	private static final String AUTHCLASSKEY = "auth.class";
	public static final String FILECLASS = "edu.ucsb.nceas.metacat.authentication.AuthFile";
    public static final String LDAPCLASS = "edu.ucsb.nceas.metacat.AuthLdap";
	/**
	 * private constructor since this is a singleton
	 */
	private AuthAdmin() {}

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
	 * @param request
	 *            the http request information
	 * @param response
	 *            the http response to be sent back to the client
	 */
	public void configureAuth(HttpServletRequest request,
			HttpServletResponse response) throws AdminException {

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

				// add the list of auth options and their values to the request
				Vector<String> propertyNames = PropertyService.getPropertyNamesByGroup("auth");
				for (String name : propertyNames) {
					request.setAttribute(name, PropertyService.getProperty(name));
				} 
				
				// add the list of organization options and their values to the request.  This is because
				// currently we use the organization.unaffiliated values to configure metacat client for 
				// password change and account creation.  Eventually, these should get moved to organization
				// level configuration.
				Vector<String> orgPropertyNames = PropertyService.getPropertyNamesByGroup("organization");
				for (String name : orgPropertyNames) {
					request.setAttribute(name, PropertyService.getProperty(name));
				} 

				// Check for any backup properties and apply them to the request.
				// These are properties from previous configurations. They keep
				// the user from having to re-enter all values when upgrading.
				// If this is a first time install, getBackupProperties will return
				// null.
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
				RequestUtil.forwardRequest(request, response,
						"/admin/auth-configuration.jsp", null);
			} catch (GeneralPropertyException gpe) {
				throw new AdminException("AuthAdmin.configureAuth - Problem getting property " + 
						"while initializing LDAP properties page: " + gpe.getMessage());
			} catch (MetacatUtilException mue) {
				throw new AdminException("AuthAdmin.configureAuth - Utility problem while initializing "
						+ "LDAP properties page:" + mue.getMessage());
			} 
		} else {
			// The configuration form is being submitted and needs to be
			// processed.
			Vector<String> processingSuccess = new Vector<String>();
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> validationErrors = new Vector<String>();

			try {
				// For each property, check if it is changed and save it
				PropertiesMetaData authMetaData = PropertyService
						.getAuthMetaData();

				// process the fields for the global options (group 1)
				SortedMap<Integer, MetaDataProperty> globalPropertyMap = authMetaData
						.getPropertiesInGroup(1);
				Set<Integer> globalPropertyIndexes = globalPropertyMap.keySet();
				for (Integer globalPropertyIndex : globalPropertyIndexes) {
					String globalPropertyKey = globalPropertyMap.get(
							globalPropertyIndex).getKey();
					PropertyService.checkAndSetProperty(request,
							globalPropertyKey);
				}
				
				//String authClassName = request.getParameter(AUTHCLASSKEY);
				//System.out.println("the auth class name from the request is "+authClassName);
				// process the fields for the file-based options (group 2)
				SortedMap<Integer, MetaDataProperty> filePropertyMap = authMetaData
						.getPropertiesInGroup(2);
				Set<Integer> filePropertyIndexes = filePropertyMap.keySet();
				for (Integer filePropertyIndex : filePropertyIndexes) {
					String filePropertyKey = filePropertyMap.get(
							filePropertyIndex).getKey();
					PropertyService.checkAndSetProperty(request,
							filePropertyKey);
				}
				
				// process the fields for the ldap-based options (group 3)
                SortedMap<Integer, MetaDataProperty> ldapPropertyMap = authMetaData
                        .getPropertiesInGroup(3);
                Set<Integer> ldapPropertyIndexes = ldapPropertyMap.keySet();
                for (Integer ldapPropertyIndex : ldapPropertyIndexes) {
                    String ldapPropertyKey = ldapPropertyMap.get(
                            ldapPropertyIndex).getKey();
                    PropertyService.checkAndSetProperty(request,
                            ldapPropertyKey);
                }

				// we need to write the options from memory to the properties
				// file
				PropertyService.persistProperties();
				PropertyService.syncToSettings();

				// Validate that the options provided are legitimate. Note that
				// we've allowed them to persist their entries. As of this point
				// there is no other easy way to go back to the configure form
				// and preserve their entries.
				validationErrors.addAll(validateOptions(request));


				// Write out the configurable properties to a backup file
				// outside the install directory.  Note that we allow them to
				// do this even if they have validation errors.  They will
				// need to go back and fix the errors before they can run metacat.
				
				// This is a special case, since it is possible that the backup directory
				// may not have been specified yet.  If not, authentication values need to be
				// persisted by the BackupAdmin when the backup directory is specified.
				String backupDir = PropertyService.getProperty("application.backupDir");
				if (backupDir != null) {
					PropertyService.persistAuthBackupProperties();
				}
			
			} catch (GeneralPropertyException gpe) {
				String errorMessage = "AuthAdmin.configureAuth - Problem getting or setting property while "
					+ "processing LDAP properties page: " + gpe.getMessage();
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
					PropertyService.setProperty("configutil.authConfigured",
							PropertyService.CONFIGURED);
					
					// Reload the main metacat configuration page
					processingSuccess.add("Authentication successfully configured");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response,
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("AuthAdmin.configureAuth - utility problem forwarding request while "
						+ "processing LDAP properties page: " + mue.getMessage());
			} catch (GeneralPropertyException gpe) {
				String errorMessage = "AuthAdmin.configureAuth - Problem getting or setting property while "
					+ "processing Authentication properties page: " + gpe.getMessage();
				logMetacat.error(errorMessage);
				processingErrors.add(errorMessage);
			}
		}
	}
	
	/**
	 * Validate the most important configuration options submitted by the user.
	 * 
	 * @return a vector holding error message for any fields that fail
	 *         validation.
	 */
	protected Vector<String> validateOptions(HttpServletRequest request) {
		Vector<String> errorVector = new Vector<String>();

		String adminUsers = request.getParameter("auth.administrators");
		Vector<String> adminUserList = StringUtil.toVector(adminUsers, ':');


		try {
			// AuthSessions were previously created based on the authentication class.
			// As LDAP and Password based authentication is being deprecated, we no
			// longer have to check for a valid class.
			// The user is expected to provide an ORCID ID, which must be a 16 digit string
			// Ensure that the user submits a 16 digit string
			// TODO: Check to see how user's get registered in the CN after registering for ORCID
			// - They should be registered
			AuthSession authSession = new AuthSession();
			for (String adminUser : adminUserList) {
				try {
					authSession.getAttributes(adminUser);
				} catch (ConnectException ce) {
					if (ce.getMessage() != null
							&& ce.getMessage().contains("NameNotFoundException")) {
						errorVector.add("User : " + adminUser + " is not in the specified identity service."+
							" If you chose to use the AuthFile as the authentication class, please add the user to the password file first.");
					} else {
						errorVector.add("Connection error while verifying Metacat " + 
								"Administrators : " + ce.getMessage());
					}
				}
			}
		} catch (InstantiationException ie) {
			errorVector.add("AuthAdmin.validateOptions - InstantiationException while verifying Metacat Administrators : "
							+ ie.getMessage());
		} catch (IllegalAccessException e) {
		    errorVector.add("AuthAdmin.validateOptions - IllegalAccessException : "
                  + e.getMessage());
        } catch (ClassNotFoundException e) {
            errorVector.add("AuthAdmin.validateOptions - ClassNotFoundException : "
                  + e.getMessage());
        } catch (Exception e) {
            errorVector.add("AuthAdmin.validateOptions - An exception : "+e.getMessage());
        }

		return errorVector;
	}
}
