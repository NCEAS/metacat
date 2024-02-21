/**
 * '$RCSfile$'
 * Purpose: A Class that implements utility methods for a metadata catalog
 * Copyright: 2008 Regents of the University of California and the
 * National Center for Ecological Analysis and Synthesis
 * Authors: Michael Daigle
 *
 * '$Author$'
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.DBTransform;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;

import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.portal.TokenGenerator;
import java.util.Enumeration;

/**
 * Entry servlet for the metadata configuration utility
 */
public class MetacatAdminServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private Log logMetacat = LogFactory.getLog(MetacatAdminServlet.class);

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
     *                 the http request information
     * @param response
     *                 the http response to be sent back to the client
     */
    private void handleGetOrPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String action = request.getParameter("configureType");
        logMetacat.info("MetacatAdminServlet.handleGetOrPost - Processing admin action: " + action);
        Vector<String> processingMessage = new Vector<String>();
        Vector<String> processingErrors = new Vector<String>();

        try {
            // TODO: Double-check to see how user's get registered in the CN after registering for ORCID
            // Always check if user is an admin before redirecting user 
            boolean isAdminSession = false;

            // Get the enumeration of header names
            Enumeration<String> headerNames = request.getHeaderNames();
            
            // Iterate over the enumeration and print each header name
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                System.out.println(headerName);
            }

            String authHeader = request.getHeader("Authorization");
            // Check if the Authorization header is present and starts with "Bearer"
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Extract the token value after "Bearer "
                String token = authHeader.substring(7); // "Bearer ".length() == 7
                Session adminSession = TokenGenerator.getInstance().getSession(token);
                System.out.println(adminSession);
                // isAdminSession = true;
            } else {
                System.out.println("Authorization header not found");
            }

            if (!ConfigurationUtil.isBackupDirConfigured()) {
                // if the backup dir has not been configured, then show the
                // backup directory configuration screen.
                processingMessage.add(
                    "You must configure the backup directory"
                        + " before you can continue with Metacat configuration."
                );
                RequestUtil.setRequestMessage(request, processingMessage);
                action = "backup";
                logMetacat.debug(
                    "MetacatAdminServlet.handleGetOrPost - " + " Admin action changed to 'backup'"
                );
            } else if (!AuthUtil.isAuthConfigured()) {
                // if authentication isn't configured, change the action to auth.
                // Authentication needs to be set up before we do anything else
                processingMessage.add(
                    "You must configure authentication before "
                        + "you can continue with MetaCat configuration."
                );
                RequestUtil.setRequestMessage(request, processingMessage);
                action = "auth";
                logMetacat.debug(
                    "MetacatAdminServlet.handleGetOrPost - " + "Admin action changed to 'auth'"
                );
            } else if (!isAdminSession) {
                // If auth is configured, see if the user is logged in
                // as an administrator.  If not, they need to log in before
                // they can continue with configuration.
                processingMessage.add(
                    "You must log in as an administrative "
                        + "user before you can continue with Metacat configuration."
                );
                RequestUtil.setRequestMessage(request, processingMessage);
                action = "login";
                logMetacat.debug(
                    "MetacatAdminServlet.handleGetOrPost - " + "Admin action changed to 'login'"
                );
            }

            if (action == null || action.equals("configure")) {
                // Forward the request main configuration page
                initialConfigurationParameters(request);
                RequestUtil.forwardRequest(
                    request, response, "/admin/metacat-configuration.jsp?configureType=configure",
                    null
                );
                return;
            } else if (action.equals("properties")) {
                // process properties
                PropertiesAdmin.getInstance().configureProperties(request, response);
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
            } else if (action.equals("dataone")) {
                // TODO: Double check that orcid auth did not create issues here
                // process dataone config
                D1Admin.getInstance().configureDataONE(request, response);
                return;
            } else if (action.equals("ezid")) {
                // process replication config
                EZIDAdmin.getInstance().configureEZID(request, response);
                return;
            } else if (action.equals("quota")) {
                // process the quota config
                QuotaAdmin.getInstance().configureQuota(request, response);
                return;
            } else if (action.equals("solrserver")) {
                // process replication config
                SolrAdmin.getInstance().configureSolr(request, response);
                return;
            } else if (action.equals("refreshStylesheets")) {
                clearStylesheetCache(response);
                return;
            } else {
                String errorMessage = "MetacatAdminServlet.handleGetOrPost - "
                    + "Invalid action in configuration request: " + action;
                logMetacat.error(errorMessage);
                processingErrors.add(errorMessage);
            }

        } catch (GeneralPropertyException ge) {
            String errorMessage =
                "MetacatAdminServlet.handleGetOrPost - Property problem while handling request: "
                    + ge.getMessage();
            logMetacat.error(errorMessage);
            processingErrors.add(errorMessage);
        } catch (AdminException ae) {
            String errorMessage =
                "MetacatAdminServlet.handleGetOrPost - Admin problem while handling request: " + ae
                    .getMessage();
            logMetacat.error(errorMessage);
            processingErrors.add(errorMessage);
        } catch (MetacatUtilException ue) {
            String errorMessage =
                "MetacatAdminServlet.handleGetOrPost - Utility problem while handling request: "
                    + ue.getMessage();
            logMetacat.error(errorMessage);
            processingErrors.add(errorMessage);
        }

        if (processingErrors.size() > 0) {
            RequestUtil.clearRequestMessages(request);
            RequestUtil.setRequestErrors(request, processingErrors);
            //something bad happened. We need to go back to the configuration
            //page and display the error message.
            //directly forwarding to the metacat-configuration.jsp page rather than
            // /admin, which will go through the servlet class again, can avoid an infinite loop.
            try {
                initialConfigurationParameters(request);
                RequestUtil.forwardRequest(
                    request, response, "/admin/metacat-configuration.jsp?configureType=configure",
                    null
                );
            } catch (Exception e) {
                //Wow we can't display the error message on a web page. Only print them out.
                logMetacat.error(
                    "MetacatAdminServlet.handleGetOrPost - couldn't"
                        + " forward the error message to the metacat configuration page since " + e
                            .getMessage()
                );
            }
        }
    }

    /*
     * Method to set up the forceBuild true which will clear the style sheet map.
     */
    private void clearStylesheetCache(HttpServletResponse response) throws IOException {
        boolean forceRebuild = true;
        DBTransform.setForceRebuild(forceRebuild);
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();
        out.print("<success>");
        out.print("The style sheet cache has been cleared and they will be reload from the disk.");
        out.print("</success>");
        out.close();
    }

    /**
     * Initialize the configuration status on the http servlet request
     * 
     * @param request
     * @throws GeneralPropertyException
     * @throws AdminException
     * @throws MetacatUtilException
     */
    private void initialConfigurationParameters(HttpServletRequest request)
        throws GeneralPropertyException, AdminException, MetacatUtilException {
        if (request != null) {
            request.setAttribute("metaCatVersion", SystemUtil.getMetacatVersion());
            request.setAttribute(
                "propsConfigured", Boolean.valueOf(PropertyService.arePropertiesConfigured())
            );
            request.setAttribute("authConfigured", Boolean.valueOf(AuthUtil.isAuthConfigured()));
            request.setAttribute(
                "metacatConfigured", Boolean.valueOf(ConfigurationUtil.isMetacatConfigured())
            );
            request.setAttribute(
                "dataoneConfigured", PropertyService.getProperty("configutil.dataoneConfigured")
            );
            request.setAttribute(
                "ezidConfigured", PropertyService.getProperty("configutil.ezidConfigured")
            );
            request.setAttribute(
                "quotaConfigured", PropertyService.getProperty("configutil.quotaConfigured")
            );
            request.setAttribute(
                "solrserverConfigured", PropertyService.getProperty(
                    "configutil.solrserverConfigured"
                )
            );
            request.setAttribute(
                "metacatServletInitialized", MetacatInitializer.isFullyInitialized()
            );
            if (PropertyService.arePropertiesConfigured()) {
                request.setAttribute("databaseVersion", DBAdmin.getInstance().getDBVersion());
                request.setAttribute("contextURL", SystemUtil.getContextURL());
            }
        }
    }
}
