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
 * Control the display of the quota -service configuration page and the processing
 * of the configuration values.
 */
public class QuotaAdmin extends MetacatAdmin {

	private static QuotaAdmin quotaAdmin = null;
	private Log logMetacat = LogFactory.getLog(EZIDAdmin.class);

	/**
	 * private constructor since this is a singleton
	 */
	private QuotaAdmin() throws AdminException {

	}

	/**
	 * Get the single instance of QuotaDAdmin.
	 * 
	 * @return the single instance of quotaAdmin
	 */
	public static QuotaAdmin getInstance() throws AdminException {
		if (quotaAdmin == null) {
		    synchronized(QuotaAdmin.class) {
		        if(quotaAdmin == null) {
		            quotaAdmin = new QuotaAdmin();
		        }
		    }
			
		}
		return quotaAdmin;
	}
	
	
	/**
     * Handle configuration of the quota service
     * @param request
     *            the http request information
     * @param response
     *            the http response to be sent back to the client
     */
    public void configureQuota(HttpServletRequest request,
            HttpServletResponse response) throws AdminException {
        logMetacat.debug("QuotaAdmin.configQuota - the start of the method");
        String processForm = request.getParameter("processForm");
        String bypass = request.getParameter("bypass");
        String formErrors = (String) request.getAttribute("formErrors");

        if (processForm == null || !processForm.equals("true") || formErrors != null) {
            // The servlet configuration parameters have not been set, or there
            // were form errors on the last attempt to configure, so redirect to
            // the web form for configuring metacat
            logMetacat.debug("QuotaAdmin.configQuota - in the error handling routine");
            try {
                // get the current configuration values
                String enablePortalStr = PropertyService.getProperty("dataone.quotas.portals.enabled");
                String enableStorageStr = PropertyService.getProperty("dataone.quotas.storage.enabled");
                String enableReplicationStr = PropertyService.getProperty("dataone.quotas.replication.enabled");
                String baseurl = PropertyService.getProperty("dataone.quotas.bookkeeper.serviceUrl");
                boolean enablePortal = false;
                if (enablePortalStr != null) {
                    enablePortal = Boolean.parseBoolean(enablePortalStr);
                }
                boolean enableStorage = false;
                if (enableStorageStr != null) {
                    enableStorage = Boolean.parseBoolean(enableStorageStr);
                }
                boolean enableReplication = false;
                if (enableReplicationStr != null) {
                    enableReplication = Boolean.parseBoolean(enableReplicationStr);
                }
                request.setAttribute("dataone.quotas.portals.enabled", enablePortal);
                request.setAttribute("dataone.quotas.storage.enabled", enableStorage);
                request.setAttribute("dataone.quotas.replication.enabled", enableReplication);
                request.setAttribute("dataone.quotas.bookkeeper.serviceUrl", baseurl);
                
                
                // try the backup properties
                SortedProperties backupProperties = null;
                if ((backupProperties = 
                        PropertyService.getMainBackupProperties()) != null) {
                    Vector<String> backupKeys = backupProperties.getPropertyNames();
                    for (String key : backupKeys) {
                        String value = backupProperties.getProperty(key);
                        if(key != null && value != null && key.equals("dataone.quotas.bookkeeper.serviceUrl")) {
                            request.setAttribute(key, value);
                        } else if (value != null) {
                            request.setAttribute(key, Boolean.parseBoolean(value));
                        }
                    }
                }

                // Forward the request to the JSP page
                RequestUtil.forwardRequest(request, response,
                        "/admin/quota-configuration.jsp", null);
            } catch (GeneralPropertyException gpe) {
                throw new AdminException("QuotaAdmin.configureQuota - Problem getting or " + 
                        "setting property while initializing system properties page: " + gpe.getMessage());
            } catch (MetacatUtilException mue) {
                throw new AdminException("QuotaAdmin.configureQuota- utility problem while initializing "
                        + "system properties page:" + mue.getMessage());
            } 
        } else if (bypass != null && bypass.equals("true")) {
            logMetacat.debug("QuotaAdmin.configQuota - in the bypass routine...");
            Vector<String> processingErrors = new Vector<String>();
            Vector<String> processingSuccess = new Vector<String>();
            
            // Bypass the geoserver configuration. This will not keep
            // Metacat from running.
            try {
                PropertyService.setProperty("configutil.quotaConfigured",
                        PropertyService.BYPASSED);
                
            } catch (GeneralPropertyException gpe) {
                String errorMessage = "QuotaAdmin.configureQuota - Problem getting or setting property while "
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
                    processingSuccess.add("Quota configuration successfully bypassed");
                    RequestUtil.clearRequestMessages(request);
                    RequestUtil.setRequestSuccess(request, processingSuccess);
                    RequestUtil.forwardRequest(request, response, 
                            "/admin?configureType=configure&processForm=false", null);
                }
            } catch (MetacatUtilException mue) {
                throw new AdminException("QuotaAdmin.configureQuota - utility problem while processing the quota services "
                        + "quotaservices page: " + mue.getMessage());
            } 
        
        } else {
            logMetacat.debug("QuotaAdmin.configQuota - in the else routine...");
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
                
                String enablePortalStr = (String)request.getParameter("dataone.quotas.portals.enabled");
                String enableStorageStr = (String)request.getParameter("dataone.quotas.storage.enabled");
                String enableReplicationStr = (String)request.getParameter("dataone.quotas.replication.enabled");
                String baseurl = (String)request.getParameter("dataone.quotas.bookkeeper.serviceUrl");
                boolean enablePortal = false;
                if (enablePortalStr != null) {
                    enablePortal = Boolean.parseBoolean(enablePortalStr);
                }
                boolean enableStorage = false;
                if (enableStorageStr != null) {
                    enableStorage = Boolean.parseBoolean(enableStorageStr);
                }
                boolean enableReplication = false;
                if (enableReplicationStr != null) {
                    enableReplication = Boolean.parseBoolean(enableReplicationStr);
                }
                
                PropertyService.setPropertyNoPersist("dataone.quotas.portals.enabled", Boolean.toString(enablePortal));
                PropertyService.setPropertyNoPersist("dataone.quotas.storage.enabled", Boolean.toString(enableStorage));
                PropertyService.setPropertyNoPersist("dataone.quotas.replication.enabled", Boolean.toString(enableReplication));
                PropertyService.setPropertyNoPersist("dataone.quotas.bookkeeper.serviceUrl", baseurl);
                // persist them all
                PropertyService.persistProperties();
                PropertyService.syncToSettings();
                // save a backup in case the form has errors, we reload from these
                PropertyService.persistMainBackupProperties();
            }  catch (GeneralPropertyException gpe) {
                String errorMessage = "QuotaDAdmin.configureQuota - Problem getting or setting property while "
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
                    PropertyService.setProperty("configutil.quotaConfigured",
                            PropertyService.CONFIGURED);
                    
                    // Reload the main metacat configuration page
                    processingSuccess.add("Quota Service successfully configured");
                    RequestUtil.clearRequestMessages(request);
                    RequestUtil.setRequestSuccess(request, processingSuccess);
                    RequestUtil.forwardRequest(request, response, 
                            "/admin?configureType=configure&processForm=false", null);
                }
            } catch (MetacatUtilException mue) {
                throw new AdminException("QuotaAdmin.configureQuota - utility problem while processing ezidservices "
                        + "quotaservices page: " + mue.getMessage());
            } catch (GeneralPropertyException gpe) {
                throw new AdminException("QuotaAdmin.configureQuota - problem with properties while "
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
