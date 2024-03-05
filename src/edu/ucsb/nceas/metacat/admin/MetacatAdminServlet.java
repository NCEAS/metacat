package edu.ucsb.nceas.metacat.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsb.nceas.metacat.restservice.D1ResourceHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.DBTransform;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;

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
        throws IOException {

        // Check authorized and populate session
//        AdminAuthHandler authHandler = new AdminAuthHandler(getServletContext(), request, response);
//        authHandler.handle(D1ResourceHandler.GET);

        // Process the data and send back the response
        handleGetOrPost(request, response);
    }

    /** Handle "POST" method requests from HTTP clients */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        // Check authorized and populate session
//        AdminAuthHandler authHandler = new AdminAuthHandler(getServletContext(), request, response);
//        authHandler.handle(D1ResourceHandler.POST);

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
        throws IOException {
        String action = request.getParameter("configureType");
        logMetacat.info("MetacatAdminServlet.handleGetOrPost - Processing admin action: " + action);

        Vector<String> processingMessage = new Vector<>();
        Vector<String> processingErrors = new Vector<>();

        logMetacat.debug("\n****** START REQUEST PARAMETERS **********************\n"
                             + getAllParams(request)
                             + "\n****** END REQUEST PARAMETERS   **********************\n");

        try {
            if (!ConfigurationUtil.isBackupDirConfigured()) {
                // if the backup dir has not been configured, then show the
                // backup directory configuration screen.
                processingMessage.add("You must configure the backup directory"
                                          + " before you can continue with Metacat configuration.");
                RequestUtil.setRequestMessage(request, processingMessage);
                action = "backup";
                logMetacat.debug(
                    "MetacatAdminServlet.handleGetOrPost - Admin action changed to 'backup'");
            } else if (!AuthUtil.isAuthConfigured()) {
                // if authentication isn't configured, change the action to auth.
                // Authentication needs to be set up before we do anything else
                processingMessage.add("You must configure authentication before "
                                          + "you can continue with MetaCat configuration.");
                RequestUtil.setRequestMessage(request, processingMessage);
                action = "auth";
                logMetacat.debug(
                    "MetacatAdminServlet.handleGetOrPost - Admin action changed to 'auth'");

            } else if (!AuthUtil.isUserLoggedInAsAdmin(request)) {
                // If auth is configured, see if the user is logged in
                // as an administrator.  If not, they need to log in before
                // they can continue with configuration.
                processingMessage.add("You must log in as an administrative "
                                      + "user before you can continue with Metacat configuration.");
                RequestUtil.setRequestMessage(request, processingMessage);
                action = "login";
                logMetacat.debug(
                    "MetacatAdminServlet.handleGetOrPost - Admin action changed to 'login'");
            }

            if (action == null || action.equals("configure")) {
                // Forward the request main configuration page
                initialConfigurationParameters(request);
                RequestUtil.forwardRequest(request, response,
                                           "/admin/metacat-configuration"
                                               + ".jsp?configureType=configure",
                                           null);
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
                "MetacatAdminServlet.handleGetOrPost - Admin problem while handling request: "
                    + ae.getMessage();
            logMetacat.error(errorMessage);
            processingErrors.add(errorMessage);
        } catch (MetacatUtilException ue) {
            String errorMessage =
                "MetacatAdminServlet.handleGetOrPost - Utility problem while handling request: "
                    + ue.getMessage();
            logMetacat.error(errorMessage);
            processingErrors.add(errorMessage);
        }

        if (!processingErrors.isEmpty()) {
            RequestUtil.clearRequestMessages(request);
            RequestUtil.setRequestErrors(request, processingErrors);
            //something bad happened. We need to go back to the configuration
            //page and display the error message.
            //directly forwarding to the metacat-configuration.jsp page rather than
            // /admin, which will go through the servlet class again, can avoid an infinite loop.
            try {
                initialConfigurationParameters(request);
                RequestUtil.forwardRequest(request, response,
                                           "/admin/metacat-configuration"
                                               + ".jsp?configureType=configure",
                                           null);
            } catch (Exception e) {
                //We can't display the error message on a web page. Only print them out.
                logMetacat.error("MetacatAdminServlet.handleGetOrPost - couldn't"
                                     + " forward the error message to the metacat configuration "
                                     + "page since "
                                     + e.getMessage());
            }
        }
    }

    private static String getAllParams(HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();
        StringBuilder returnStr = new StringBuilder();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            returnStr.append(paramName).append(":\n");
            String[] paramValues = request.getParameterValues(paramName);
            if (paramValues != null) {
                for (String value : paramValues) {
                    returnStr.append("    ").append(value).append(";\n");
                }
            }
        }
        return returnStr.toString();
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
                "propsConfigured", PropertyService.arePropertiesConfigured());
            request.setAttribute("authConfigured", AuthUtil.isAuthConfigured());
            request.setAttribute(
                "metacatConfigured", ConfigurationUtil.isMetacatConfigured());
            request.setAttribute(
                "dataoneConfigured", PropertyService.getProperty("configutil.dataoneConfigured"));
            request.setAttribute(
                "ezidConfigured", PropertyService.getProperty("configutil.ezidConfigured"));
            request.setAttribute(
                "quotaConfigured", PropertyService.getProperty("configutil.quotaConfigured"));
            request.setAttribute(
                "solrserverConfigured",
                PropertyService.getProperty("configutil.solrserverConfigured"));
            request.setAttribute(
                "metacatServletInitialized", MetacatInitializer.isFullyInitialized());
            if (PropertyService.arePropertiesConfigured()) {
                request.setAttribute("databaseVersion", DBAdmin.getInstance().getDBVersion());
                request.setAttribute("contextURL", SystemUtil.getContextURL());
            }
        }
    }
}
