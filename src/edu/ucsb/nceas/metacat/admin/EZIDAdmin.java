package edu.ucsb.nceas.metacat.admin;

import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.GeoserverUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * Control the display of the ezid configuration page and the processing
 * of the configuration values.
 */
public class EZIDAdmin extends MetacatAdmin {

	private static EZIDAdmin ezidAdmin = null;
	private Log logMetacat = LogFactory.getLog(EZIDAdmin.class);

	/**
	 * private constructor since this is a singleton
	 */
	private EZIDAdmin() throws AdminException {

	}

	/**
	 * Get the single instance of EZIDAdmin.
	 * 
	 * @return the single instance of GeoserverAdmin
	 */
	public static EZIDAdmin getInstance() throws AdminException {
		if (ezidAdmin == null) {
		    synchronized(EZIDAdmin.class) {
		        if(ezidAdmin == null) {
		            ezidAdmin = new EZIDAdmin();
		        }
		    }
			
		}
		return ezidAdmin;
	}

	/**
	 * Handle configuration of the database the first time that Metacat starts
	 * or when it is explicitly called. Collect necessary update information
	 * from the administrator.
	 * 
	 * @param request
	 *            the http request information
	 * @param response
	 *            the http response to be sent back to the client
	 */
	public void configureEZID(HttpServletRequest request,
			HttpServletResponse response) throws AdminException {

		String processForm = request.getParameter("processForm");
		String bypass = request.getParameter("bypass");
		String formErrors = (String) request.getAttribute("formErrors");

		if (processForm == null || !processForm.equals("true") || formErrors != null) {
			// The servlet configuration parameters have not been set, or there
			// were form errors on the last attempt to configure, so redirect to
			// the web form for configuring metacat

			try {
				// get the current configuration values
			    String enablestr = PropertyService.getProperty("guid.doi.enabled");
			    String doiPluginClass = PropertyService.getProperty("guid.doiservice.plugin.class");
				String username = PropertyService.getProperty("guid.doi.username");
				String password = PropertyService.getProperty("guid.doi.password");
				String baseurl = PropertyService.getProperty("guid.doi.baseurl");
				String doishoulder = PropertyService.getProperty("guid.doi.doishoulder.1");
				boolean enable = false;
				if (enablestr != null) {
					enable = Boolean.parseBoolean(enablestr);
				}
				
				request.setAttribute("guid.doi.enabled", enable);
				request.setAttribute("guid.doiservice.plugin.class", doiPluginClass);
				request.setAttribute("guid.doi.username", username);
				request.setAttribute("guid.doi.password", password);
				request.setAttribute("guid.doi.baseurl", baseurl);
				request.setAttribute("guid.doi.doishoulder.1", doishoulder);
				
				
				// try the backup properties
                SortedProperties backupProperties = null;
                if ((backupProperties = 
                        PropertyService.getMainBackupProperties()) != null) {
                    Vector<String> backupKeys = backupProperties.getPropertyNames();
                    for (String key : backupKeys) {
                        String value = backupProperties.getProperty(key);
                        if(key != null && value != null && key.equals("guid.doi.enabled")) {
                            enable = Boolean.parseBoolean(value);
                            request.setAttribute("guid.doi.enabled", enable);
                        } else if (value != null) {
                            request.setAttribute(key, value);
                        }
                    }
                }

				// Forward the request to the JSP page
				RequestUtil.forwardRequest(request, response,
						"/admin/ezid-configuration.jsp", null);
			} catch (GeneralPropertyException gpe) {
				throw new AdminException("EZIDAdmin.configureEZID - Problem getting or " + 
						"setting property while initializing system properties page: " + gpe.getMessage());
			} catch (MetacatUtilException mue) {
				throw new AdminException("EZIDAdmin.configureEZID- utility problem while initializing "
						+ "system properties page:" + mue.getMessage());
			} 
		} else if (bypass != null && bypass.equals("true")) {
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> processingSuccess = new Vector<String>();
			
			// Bypass the geoserver configuration. This will not keep
			// Metacat from running.
			try {
				PropertyService.setProperty("configutil.ezidConfigured",
						PropertyService.BYPASSED);
				
			} catch (GeneralPropertyException gpe) {
				String errorMessage = "EZIDAdmin.configureEZID - Problem getting or setting property while "
					+ "processing system properties page: " + gpe.getMessage();
				logMetacat.error(errorMessage);
				processingErrors.add(errorMessage);
			}
			try {
				if (processingErrors.size() > 0) {
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestErrors(request, processingErrors);
					RequestUtil.forwardRequest(request, response, "/admin", null);
				} else {			
					// Reload the main metacat configuration page
					processingSuccess.add("EZID configuration successfully bypassed");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response, 
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("EZID.configureEZID - utility problem while processing ezid services "
						+ "ezidservices page: " + mue.getMessage());
			} 
		
		} else {
			// The configuration form is being submitted and needs to be
			// processed, setting the properties in the configuration file
			// then restart metacat

			// The configuration form is being submitted and needs to be
			// processed.
			Vector<String> validationErrors = new Vector<String>();
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> processingSuccess = new Vector<String>();

			try {
				// Validate that the options provided are legitimate. Note that
				// we've allowed them to persist their entries. As of this point
				// there is no other easy way to go back to the configure form
				// and preserve their entries.
				validationErrors.addAll(validateOptions(request));
				
				String enablestr = (String)request.getParameter("guid.doi.enabled");
				String pluginClass = (String)request.getParameter("guid.doiservice.plugin.class");
				String username = (String)request.getParameter("guid.doi.username");
                String password = (String)request.getParameter("guid.doi.password");
                String baseurl = (String)request.getParameter("guid.doi.baseurl");
                String doishoulder = (String)request.getParameter("guid.doi.doishoulder.1");
                boolean enable = false;
                if (enablestr != null) {
                    enable = Boolean.parseBoolean(enablestr);
                }
				
				
				if (username == null || password == null) {
					validationErrors.add("User Name and Password cannot be null");
				} else {
				    PropertyService.setPropertyNoPersist("guid.doi.enabled", Boolean.toString(enable));
				    PropertyService.setPropertyNoPersist("guid.doiservice.plugin.class", pluginClass);
					PropertyService.setPropertyNoPersist("guid.doi.username", username);
					PropertyService.setPropertyNoPersist("guid.doi.password", password);
					PropertyService.setPropertyNoPersist("guid.doi.baseurl", baseurl);
					PropertyService.setPropertyNoPersist("guid.doi.doishoulder.1", doishoulder);
					// persist them all
					PropertyService.persistProperties();
					PropertyService.syncToSettings();
                    // save a backup in case the form has errors, we reload from these
                    PropertyService.persistMainBackupProperties();
				}
			}  catch (GeneralPropertyException gpe) {
				String errorMessage = "EZIDAdmin.configureEZID - Problem getting or setting property while "
						+ "processing system properties page: " + gpe.getMessage();
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
					// 'propertiesConfigured' option to 'true'
					PropertyService.setProperty("configutil.ezidConfigured",
							PropertyService.CONFIGURED);
					
					// Reload the main metacat configuration page
					processingSuccess.add("EZID successfully configured");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response, 
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("EZIDAdmin.configureEZID - utility problem while processing ezidservices "
						+ "ezidservices page: " + mue.getMessage());
			} catch (GeneralPropertyException gpe) {
				throw new AdminException("EZIDAdmin.configureEZID - problem with properties while "
						+ "processing ezidservices configuration page: " + gpe.getMessage());
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

		// TODO MCD validate options.

		return errorVector;
	}
}
