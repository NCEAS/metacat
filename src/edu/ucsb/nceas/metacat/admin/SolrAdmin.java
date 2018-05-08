/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements database configuration methods
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author: daigle $'
 *     '$Date: 2008-07-24 13:47:03 -0700 (Thu, 24 Jul 2008) $'
 * '$Revision: 4155 $'
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

import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.GeoserverUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * Control the display of the Solr configuration page and the processing
 * of the configuration values.
 */
public class SolrAdmin extends MetacatAdmin {

	private static SolrAdmin solrAdmin = null;
	private Logger logMetacat = Logger.getLogger(SolrAdmin.class);

	/**
	 * private constructor since this is a singleton
	 */
	private SolrAdmin() throws AdminException {

	}

	/**
	 * Get the single instance of SolrDAdmin.
	 * 
	 * @return the single instance of SolrAdmin
	 */
	public static SolrAdmin getInstance() throws AdminException {
		if (solrAdmin == null) {
		    synchronized(SolrAdmin.class) {
		        if(solrAdmin == null) {
		            solrAdmin = new SolrAdmin();
		        }
		    }
			
		}
		return solrAdmin;
	}

	/**
	 * Handle configuration of the solr the first time that Metacat starts
	 * or when it is explicitly called. Collect necessary update information
	 * from the administrator.
	 * 
	 * @param request
	 *            the http request information
	 * @param response
	 *            the http response to be sent back to the client
	 */
	public void configureSolr(HttpServletRequest request,
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
			    String baseURL = PropertyService.getProperty("solr.baseURL");
				//String username = PropertyService.getProperty("solr.admin.user");
				//String password = PropertyService.getProperty("solr.password");
				String coreName = PropertyService.getProperty("solr.coreName");
				String solrHome = PropertyService.getProperty("solr.homeDir");
				String osUser =  PropertyService.getProperty("solr.os.user");
				
				request.setAttribute("solr.baseURL", baseURL);
				//request.setAttribute("solr.admin.user", username);
				//request.setAttribute("solr.password", password);
				request.setAttribute("solr.coreName", coreName);
				request.setAttribute("solr.homeDir", solrHome);
				request.setAttribute("solr.os.user", osUser);
				
				// try the backup properties
                SortedProperties backupProperties = null;
                if ((backupProperties = 
                        PropertyService.getMainBackupProperties()) != null) {
                    Vector<String> backupKeys = backupProperties.getPropertyNames();
                    for (String key : backupKeys) {
                        String value = backupProperties.getProperty(key);
                        if(key != null && value != null && key.equals("solr.homeDir") && value.equals("/var/metacat/solr-home")) {
                           // skip the solrHome value from the if its value is solr-home
                        } else if (value != null) {
                            request.setAttribute(key, value);
                        }
                    }
                }

				// Forward the request to the JSP page
				RequestUtil.forwardRequest(request, response,
						"/admin/solr-configuration.jsp", null);
			} catch (GeneralPropertyException gpe) {
				throw new AdminException("SolrAdmin.configureSolr - Problem getting or " + 
						"setting property while initializing system properties page: " + gpe.getMessage());
			} catch (MetacatUtilException mue) {
				throw new AdminException("SolrAdmin.configureSolr- utility problem while initializing "
						+ "system properties page:" + mue.getMessage());
			} 
		} else if (bypass != null && bypass.equals("true")) {
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> processingSuccess = new Vector<String>();
			
			// Bypass the geoserver configuration. This will not keep
			// Metacat from running.
			try {
				PropertyService.setProperty("configutil.solrserverConfigured",
						PropertyService.BYPASSED);
				
			} catch (GeneralPropertyException gpe) {
				String errorMessage = "SolrAdmin.configureSolr - Problem getting or setting property while "
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
					processingSuccess.add("Solr configuration successfully bypassed");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response, 
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("Solr.configureSolr - utility problem while processing solr services "
						+ "solr services page: " + mue.getMessage());
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
				
				  String baseURL = PropertyService.getProperty("solr.baseURL");
	              //String username = PropertyService.getProperty("solr.admin.user");
	              //String password = PropertyService.getProperty("solr.password");
	              String coreName = PropertyService.getProperty("solr.coreName");
	              String solrHome = PropertyService.getProperty("solr.homeDir");
	              String osUser =  PropertyService.getProperty("solr.os.user");
				//if (username == null || password == null) {
					//validationErrors.add("User Name and Password cannot be null");
				//} else {
				    PropertyService.setPropertyNoPersist("solr.baseURL", baseURL);
					PropertyService.setPropertyNoPersist("solr.coreName", coreName);
					PropertyService.setPropertyNoPersist("solr.homeDir", solrHome);
					PropertyService.setPropertyNoPersist("solr.os.user", osUser);
					// persist them all
					PropertyService.persistProperties();
					PropertyService.syncToSettings();
                    // save a backup in case the form has errors, we reload from these
                    PropertyService.persistMainBackupProperties();
				//}
			}  catch (GeneralPropertyException gpe) {
				String errorMessage = "SolrAdmin.configureSolr - Problem getting or setting property while "
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
					PropertyService.setProperty("configutil.solrserverConfigured",
							PropertyService.CONFIGURED);
					
					// Reload the main metacat configuration page
					processingSuccess.add("Solr server was successfully configured");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response, 
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("SolrAdmin.configureSolr - utility problem while processing solr services "
						+ "solr services page: " + mue.getMessage());
			} catch (GeneralPropertyException gpe) {
				throw new AdminException("SolrAdmin.configureSolr - problem with properties while "
						+ "processing solr services configuration page: " + gpe.getMessage());
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
