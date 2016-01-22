/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements utility methods for a metadata catalog
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

import java.io.IOException;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SkinUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;

/**
 * Entry servlet for the metadata configuration utility
 */
public class MetacatAdminServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private Logger logMetacat = Logger.getLogger(MetacatAdminServlet.class);
	
    /**
	 * Initialize the servlet
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}
        
    /** Handle "GET" method requests from HTTP clients */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// Process the data and send back the response
		handleGetOrPost(request, response);
	}

	/** Handle "POST" method requests from HTTP clients */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// Process the data and send back the response
		handleGetOrPost(request, response);
	}
       
    /**
	 * Control servlet response depending on the action parameter specified
	 * 
	 * @param request
	 *            the http request information
	 * @param response
	 *            the http response to be sent back to the client
	 */
	private void handleGetOrPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String action = request.getParameter("configureType");
		logMetacat.debug("MetacatAdminServlet.handleGetOrPost - Processing admin action: " + action);
		Vector<String> processingMessage = new Vector<String>();
		Vector<String> processingErrors = new Vector<String>();

		try {
			// Update the last update time for this user if they are not new
			HttpSession httpSession = request.getSession(false);
			if (httpSession != null) {
				SessionService.getInstance().touchSession(httpSession.getId());
			}
			
			if (!ConfigurationUtil.isBackupDirConfigured()) {
				// if the backup dir has not been configured, then show the
				// backup directory configuration screen.
				processingMessage.add("You must configure the backup directory"
						+ " before you can continue with Metacat configuration.");
				RequestUtil.setRequestMessage(request, processingMessage);
				action = "backup";
				logMetacat.debug("MetacatAdminServlet.handleGetOrPost - Admin action changed to 'backup'");
			} else if (!AuthUtil.isAuthConfigured()) {
				// if authentication isn't configured, change the action to auth.  
				// Authentication needs to be set up before we do anything else
				processingMessage.add("You must configure authentication before "
						+ "you can continue with MetaCat configuration.");
				RequestUtil.setRequestMessage(request, processingMessage);
				action = "auth";
				logMetacat.debug("MetacatAdminServlet.handleGetOrPost - Admin action changed to 'auth'");
			} else if (!AuthUtil.isUserLoggedInAsAdmin(request)) {
				// If auth is configured, see if the user is logged in
				// as an administrator.  If not, they need to log in before
				// they can continue with configuration.
				processingMessage.add("You must log in as an administrative " + "" +
						"user before you can continue with MetaCat configuration.");
				RequestUtil.setRequestMessage(request, processingMessage);
				action = "login";
				logMetacat.debug("MetacatAdminServlet.handleGetOrPost - Admin action changed to 'login'");
			}  

			if (action == null || action.equals("configure")) {
				// Forward the request main configuration page
				request.setAttribute("metaCatVersion", SystemUtil.getMetacatVersion()); 
			    request.setAttribute("propsConfigured", new Boolean(PropertyService.arePropertiesConfigured()));
			    request.setAttribute("authConfigured", new Boolean(AuthUtil.isAuthConfigured()));
			    // TODO MCD figure out if we still need an org section
			    //request.setAttribute("orgsConfigured", new Boolean(OrganizationUtil.areOrganizationsConfigured()));
			    request.setAttribute("skinsConfigured", new Boolean(SkinUtil.areSkinsConfigured()));
			    request.setAttribute("metacatConfigured", new Boolean(ConfigurationUtil.isMetacatConfigured()));	
			    request.setAttribute("geoserverConfigured", 
			    		PropertyService.getProperty("configutil.geoserverConfigured"));
			    request.setAttribute("dataoneConfigured", 
			    		PropertyService.getProperty("configutil.dataoneConfigured"));
			    request.setAttribute("ezidConfigured", 
                        PropertyService.getProperty("configutil.ezidConfigured"));
			    request.setAttribute("metcatServletInitialized", MetaCatServlet.isFullyInitialized());
			    if (PropertyService.arePropertiesConfigured()) {
					request.setAttribute("databaseVersion", 
							DBAdmin.getInstance().getDBVersion());
					request.setAttribute("contextURL", SystemUtil.getContextURL());
				}
				RequestUtil.forwardRequest(request, response,
						"/admin/metacat-configuration.jsp?configureType=configure", null);
				return;
			} else if (action.equals("properties")) {
				// process properties
				PropertiesAdmin.getInstance().configureProperties(request,
						response);
				return;
			} else if (action.equals("skins")) {
				// process skins
				SkinsAdmin.getInstance().configureSkins(request, response);
				return;
			} else if (action.equals("database")) {
				// process database
				DBAdmin.getInstance().configureDatabase(request, response);
				return;
			} else if (action.equals("auth")) {
				// process authentication
				AuthAdmin.getInstance().configureAuth(request, response);
				return;
			} else if (action.equals("login")) {
				// process login
				LoginAdmin.getInstance().authenticateUser(request, response);
				return;
			} else if (action.equals("backup")) {
				// process login
				BackupAdmin.getInstance().configureBackup(request, response);
				return;
			} else if (action.equals("geoserver")) {
				// process geoserver password change
				GeoserverAdmin.getInstance().configureGeoserver(request, response);
				return;
			} else if (action.equals("dataone")) {
				// process dataone config
				D1Admin.getInstance().configureDataONE(request, response);
				return;
			} else if (action.equals("replication")) {
				// process replication config
				ReplicationAdmin.getInstance().handleRequest(request, response);
				return;
			} else if (action.equals("ezid")) {
                // process replication config
                EZIDAdmin.getInstance().configureEZID(request, response);
                return; 
			} else {
				String errorMessage = "MetacatAdminServlet.handleGetOrPost - Invalid action in configuration request: " + action;
				logMetacat.error(errorMessage);
				processingErrors.add(errorMessage);
			} 
			
			if (processingErrors.size() > 0) {
				RequestUtil.clearRequestMessages(request);
				RequestUtil.setRequestErrors(request,processingErrors);
				// if the action that threw an exception was "configure" just go straight to the metacat
				// configuration.  This will avoid a loop.  Otherwise, call the admin servlet with 
				// configuration action.
				if (action != null && action.equals("configure")) {
					RequestUtil.forwardRequest(request, response, "/admin/metacat-configuration.jsp", null);
				} else {
					RequestUtil.forwardRequest(request, response, "/admin?configureType=configure", null);
				}
			}
		} catch (GeneralPropertyException ge) {
			String errorMessage = 
				"MetacatAdminServlet.handleGetOrPost - Property problem while handling request: " + ge.getMessage();
			logMetacat.error(errorMessage);
			processingErrors.add(errorMessage);
		} catch (AdminException ae) {
			String errorMessage = 
				"MetacatAdminServlet.handleGetOrPost - Admin problem while handling request: " + ae.getMessage();
			logMetacat.error(errorMessage);
			processingErrors.add(errorMessage);
		} catch (MetacatUtilException ue) {
			String errorMessage = 
				"MetacatAdminServlet.handleGetOrPost - Utility problem while handling request: " + ue.getMessage();
			logMetacat.error(errorMessage);
			processingErrors.add(errorMessage);
		} catch (ServiceException e) {
			String errorMessage = 
				"MetacatAdminServlet.handleGetOrPost - Service problem while handling request: " + e.getMessage();
			logMetacat.error(errorMessage);
			processingErrors.add(errorMessage);
		}
	}       	
}