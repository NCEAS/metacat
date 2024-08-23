package edu.ucsb.nceas.metacat.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
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
import org.dataone.service.types.v1.Identifier;

/**
 * Entry servlet for the metadata configuration utility
 */
public class MetacatAdminServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String ATTR_LOGIN_PRE_ORCID_URI = "loginPreOrcidUri";
    public static final String ATTR_LOGIN_ORCID_FLOW_URI = "loginOrcidFlowUri";
    public static final String ATTR_LOGIN_METACAT_URI = "loginMetacatUri";
    public static final String ATTR_LOGOUT_URI = "logoutUri";
    public static final String ATTR_CN_BASE_URL = "cnBaseUrl";

    public static final String ACTION_PARAM = "configureType";
    public static final String ACTION_ORCID_FLOW = "orcidFlow";
    public static final String ACTION_LOGIN_MC = "mcLogin";
    public static final String ACTION_LOGOUT = "logout";

    private static final String PATH_LOGOUT = "/admin?" + ACTION_PARAM + "=logout";
    private static final String PATH_LOGIN_PRE_ORCID = "/admin";
    private static final String PATH_LOGIN_ORCID_FLOW =
        "/admin?" + ACTION_PARAM + "=" + ACTION_ORCID_FLOW;
    private static final String PATH_LOGIN_METACAT =
        "/admin?" + ACTION_PARAM + "=" + ACTION_LOGIN_MC;
    public static final String PATH_ADMIN_HOMEPAGE =
        "/admin?" + ACTION_PARAM + "=configure&processForm=false";
    public static final String PATH_D1_PORTAL_OAUTH = "/portal/oauth?action=start&amp;target=";
    public static final String PATH_D1_PORTAL_TOKEN = "/portal/token";
    public static final String PATH_D1_PORTAL_LOGOUT = "/portal/logout?target=";

    private Log logMetacat = LogFactory.getLog(MetacatAdminServlet.class);

    /**
     * Initialize the servlet
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String cnUrl = "NOT SET";
        final String cnBaseUrl;
        try {
            cnUrl = PropertyService.getProperty("D1Client.CN_URL");
            // example D1Client.CN_URL:  'https://cn.dataone.org/cn' - so for the base url, we need
            // to retrieve just the scheme & host - in this case 'https://cn.dataone.org'
            cnBaseUrl = getDomainPart(cnUrl);
        } catch (PropertyNotFoundException e) {
            throw new RuntimeException("Cannot continue - no value found for D1Client.CN_URL in "
                                           + "metacat.properties or metacat-site.properties", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot continue - got D1Client.CN_URL (" + cnUrl
                                           + "), but unable to parse base URL from it", e);
        }
        // add to context; access using `application.getAttribute(String);`
        getServletContext().setAttribute(ATTR_CN_BASE_URL, cnBaseUrl);

        String contextPath = getServletContext().getContextPath();
        getServletContext().setAttribute(
            ATTR_LOGIN_PRE_ORCID_URI, contextPath + PATH_LOGIN_PRE_ORCID);
        getServletContext().setAttribute(
            ATTR_LOGIN_ORCID_FLOW_URI, contextPath + PATH_LOGIN_ORCID_FLOW);
        getServletContext().setAttribute(ATTR_LOGIN_METACAT_URI, contextPath + PATH_LOGIN_METACAT);
        getServletContext().setAttribute(ATTR_LOGOUT_URI, contextPath + PATH_LOGOUT);
    }

    /**
     * Handle "GET" method requests from HTTP clients
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Process the data and send back the response
        handleGetOrPost(request, response);
    }

    /**
     * Handle "POST" method requests from HTTP clients
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        // Process the data and send back the response
        handleGetOrPost(request, response);
    }

    /**
     * Control servlet response depending on the action parameter specified
     *
     * @param request  the http request information
     * @param response the http response to be sent back to the client
     */
    private void handleGetOrPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        String action = request.getParameter(ACTION_PARAM);
        logMetacat.info("MetacatAdminServlet - Processing admin action: " + action);

        Vector<String> processingMessage = new Vector<>();
        Vector<String> processingErrors = new Vector<>();

        logMetacat.debug("\n****** USERID IN SESSION: " + request.getSession()
            .getAttribute(RequestUtil.ATTR_USER_ID) + "**********************\n");

        logMetacat.debug("\n****** START REQUEST PARAMETERS **********************\n"
                             + RequestUtil.getParametersAsString(request)
                             + "\n****** END REQUEST PARAMETERS   **********************\n");

        try {
            if (!ConfigurationUtil.isBackupDirConfigured()) {
                // if the backup dir has not been configured, then show the
                // backup directory configuration screen.
                processingMessage.add("You must configure the backup directory"
                                          + " before you can continue with Metacat configuration.");
                RequestUtil.setRequestMessage(request, processingMessage);
                action = "backup";
                logMetacat.debug("MetacatAdminServlet - Admin action changed to 'backup'");
            } else if (!AuthUtil.isAuthConfigured()) {
                // if authentication isn't configured, change the action to auth.
                // Authentication needs to be set up before we do anything else
                processingMessage.add("You must configure authentication before "
                                          + "you can continue with MetaCat configuration.");
                RequestUtil.setRequestMessage(request, processingMessage);
                action = "auth";
                logMetacat.debug("MetacatAdminServlet - Admin action changed to 'auth'");
            } else if (LoginAdmin.getInstance().needsLoginAdminHandling(request, action)) {
                logMetacat.debug("MetacatAdminServlet - Admin action is: " + action
                                     + "; intervention by LoginAdmin is required");
                LoginAdmin.getInstance().handle(request, response);
                return;
            }

            if (action == null) {
                // Forward the request main configuration page
                action = "configure";
                logMetacat.debug("MetacatAdminServlet - null action changed to 'configure'");
            }

            switch (action) {
                case "configure" -> {
                    // Forward the request main configuration page
                    initialConfigurationParameters(request);
                    RequestUtil.forwardRequest(request, response,
                                               "/admin/metacat-configuration.jsp?" + ACTION_PARAM
                                                   + "=configure", null);
                }
                case "properties" ->
                    PropertiesAdmin.getInstance().configureProperties(request, response);
                case "database" -> DBAdmin.getInstance().configureDatabase(request, response);
                case "auth" -> AuthAdmin.getInstance().configureAuth(request, response);
                case "backup" -> BackupAdmin.getInstance().configureBackup(request, response);
                case "dataone" -> D1Admin.getInstance().configureDataONE(request, response);
                case "ezid" -> EZIDAdmin.getInstance().configureEZID(request, response);
                case "quota" -> QuotaAdmin.getInstance().configureQuota(request, response);
                case "solrserver" -> SolrAdmin.getInstance().configureSolr(request, response);
                case "refreshStylesheets" -> clearStylesheetCache(response);
                case "removeSysmetaLock" -> removeSysmetaLock(request, response);
                case "orcidFlow" -> {
                    // A temporary fix for getting error to refresh browser after log-in
                    // forward to the main admin page
                    initialConfigurationParameters(request);
                    RequestUtil.forwardRequest(request, response,
                                               "/admin/metacat-configuration.jsp", null);
                }
                case "storageConversion" -> HashStoreConversionAdmin.convert();
                default -> {
                    String errorMessage =
                        "MetacatAdminServlet - Invalid action in configuration request: " + action;
                    logMetacat.error(errorMessage);
                    processingErrors.add(errorMessage);
                }
            }

        } catch (GeneralPropertyException ge) {
            String errorMessage =
                "MetacatAdminServlet - Property problem while handling request: " + ge.getMessage();
            logMetacat.error(errorMessage);
            processingErrors.add(errorMessage);
        } catch (AdminException ae) {
            String errorMessage =
                "MetacatAdminServlet - Admin problem while handling request: " + ae.getMessage();
            logMetacat.error(errorMessage);
            processingErrors.add(errorMessage);
        } catch (MetacatUtilException ue) {
            String errorMessage =
                "MetacatAdminServlet - Utility problem while handling request: " + ue.getMessage();
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
                                           "/admin/metacat-configuration" + ".jsp?" + ACTION_PARAM
                                               + "=configure", null);
            } catch (Exception e) {
                //We can't display the error message on a web page. Only print them out.
                logMetacat.error("MetacatAdminServlet - couldn't forward the error message to "
                                     + "the metacat configuration page since " + e.getMessage());
            }
        }
    }

    // Get the scheme + host -- i.e. the first part of the url, up to the first slash after the
    // top-level domain. Example:
    //     input:    https://cn.dataone.org/cn/some/other/stuff?etc=true
    //     output:   https://cn.dataone.org/
    private static String getDomainPart(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String protocol = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();

        StringBuilder domainPart = new StringBuilder();
        domainPart.append(protocol).append("://").append(host);
        if (port != -1) {
            domainPart.append(":").append(port);
        }
        return domainPart.toString();
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
            String dbConfigured = PropertyService.getProperty("configutil.databaseConfigured");
            request.setAttribute("metaCatVersion", SystemUtil.getMetacatVersion());
            request.setAttribute("propsConfigured", PropertyService.arePropertiesConfigured());
            request.setAttribute("dbConfigured", dbConfigured);
            request.setAttribute("authConfigured", AuthUtil.isAuthConfigured());
            request.setAttribute("metacatConfigured", ConfigurationUtil.isMetacatConfigured());
            request.setAttribute(
                "dataoneConfigured", PropertyService.getProperty("configutil.dataoneConfigured"));
            request.setAttribute(
                "ezidConfigured", PropertyService.getProperty("configutil.ezidConfigured"));
            request.setAttribute(
                "quotaConfigured", PropertyService.getProperty("configutil.quotaConfigured"));
            request.setAttribute("solrserverConfigured",
                                 PropertyService.getProperty("configutil.solrserverConfigured"));
            request.setAttribute(
                "metacatServletInitialized", MetacatInitializer.isFullyInitialized());
            if (PropertyService.arePropertiesConfigured()) {
                request.setAttribute("databaseVersion", DBAdmin.getInstance().getDBVersion());
                request.setAttribute("contextURL", SystemUtil.getContextURL());
            }
            String hashStoreStatus = HashStoreConversionAdmin.getStatus().getValue();
            request.setAttribute("hashStoreStatus", hashStoreStatus  );
            // Add the db configure errors
            if (dbConfigured != null && dbConfigured.equals(MetacatAdmin.FAILURE)
                && DBAdmin.getError().size() > 0) {
                request.setAttribute("supportEmail", PropertyService.getProperty("email.recipient"));
                RequestUtil.setRequestErrors(request, DBAdmin.getError());
            }
            if (hashStoreStatus != null && hashStoreStatus.equals(MetacatAdmin.FAILED)
                && HashStoreConversionAdmin.getError().size() > 0) {
                request.setAttribute("supportEmail", PropertyService.getProperty("email.recipient"));
                RequestUtil.setRequestErrors(request, HashStoreConversionAdmin.getError());
            } else if (hashStoreStatus != null && hashStoreStatus.equals(
                MetacatAdmin.COMPLETE) && HashStoreConversionAdmin.getInfo().size() > 0) {
                request.setAttribute("supportEmail", PropertyService.getProperty("email.recipient"));
                RequestUtil.setRequestMessage(request, HashStoreConversionAdmin.getInfo());
            }
        }
    }

    /**
     * Remove only ONE pid from the system metadata lock queue. This is an admin method to
     * fix a deadlock in the system metadata modification lock.
     * @param response
     * @throws IOException
     */
    private void removeSysmetaLock(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
            String pid = request.getParameter("pid");
            logMetacat.debug("Remove the pid " + pid + " from the system metadata lock");
            if (pid != null && !pid.isBlank()) {
                Identifier id = new Identifier();
                id.setValue(pid);
                SystemMetadataManager.unLock(id);
                try (PrintWriter out = response.getWriter()) {
                    out.print("<success>");
                    out.print("Metacat has removed the pid " + pid + " from the system metadata lock.");
                    out.print("</success>");
                }
            } else {
                try (PrintWriter out = response.getWriter()) {
                    out.print("<error>");
                    out.print("The pid should not be blank.");
                    out.print("</error>");
                }
            }
    }
}
