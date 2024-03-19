
package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.database.DBVersion;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;
import edu.ucsb.nceas.utilities.UtilException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Vector;

/**
 * Control the display of the main properties configuration page and the 
 * processing of the configuration values.
 */
public class PropertiesAdmin extends MetacatAdmin {
    private static final String SLASH = "/";
    private static final String DEFAULT_METACAT_CONTEXT = "metacat";
    private static final String METACAT_PROPERTY_APPENDIX = "/WEB-INF/metacat.properties";
    private static PropertiesAdmin propertiesAdmin = null;
    private static final Log logMetacat = LogFactory.getLog(PropertiesAdmin.class);

    /**
     * private constructor since this is a singleton
     */
    private PropertiesAdmin() {}

    /**
     * Get the single instance of the MetaCatConfig.
     *
     * @return the single instance of MetaCatConfig
     */
    public static PropertiesAdmin getInstance() {
        if (propertiesAdmin == null) {
            propertiesAdmin = new PropertiesAdmin();
        }
        return propertiesAdmin;
    }

    /**
     * Handle configuration of the main application properties
     *
     * @param request
     *            the http request object
     * @param response
     *            the http response to be sent back to the client
     */
    public void configureProperties(HttpServletRequest request,
            HttpServletResponse response) throws AdminException {

        String processForm = request.getParameter("processForm");
        String formErrors = (String)request.getAttribute("formErrors");

        if (processForm == null || !processForm.equals("true") || formErrors != null) {
            // The servlet configuration parameters have not been set, or there
            // were form errors on the last attempt to configure, so redirect to
            // the web form for configuring metacat

            try {
                // Load the properties metadata file so that the JSP page can
                // use the metadata to construct the editing form
                PropertiesMetaData metadata = PropertyService.getMainMetaData();
                request.setAttribute("metadata", metadata);

                String externalDir = PropertyService.getRecommendedExternalDir();

                if (externalDir == null) {
                    throw new AdminException("Could not initialize property configuration "
                                    + "page recommended application external directory was null");
                }

                // Attempt to discover the following properties.  These will show
                // up in the configuration fields if nothing else is provided.
                final String applicationContext = "application.context";
                if (isNotSet(applicationContext)) {
                    PropertyService.setPropertyNoPersist(applicationContext,
                            ServiceService.getRealApplicationContext());
                }
                final String serverName = "server.name";
                if (isNotSet(serverName)) {
                    PropertyService.setPropertyNoPersist(serverName, SystemUtil
                            .discoverServerName(request));
                }
                final String serverPort = "server.port";
                if (isNotSet(serverPort)) {
                    PropertyService.setPropertyNoPersist(serverPort,
                            SystemUtil.discoverServerSSLPort(request));
                }
                final String serverHTTPS = "server.https";
                if (isNotSet(serverHTTPS)) {
                    PropertyService.setPropertyNoPersist(serverHTTPS, "true");
                }
                final String applicationDeployDir = "application.deployDir";
                if (isNotSet(applicationDeployDir)) {
                    PropertyService.setPropertyNoPersist(applicationDeployDir,
                            SystemUtil.discoverDeployDir(request));
                }
                final String applicationDataDir = "application.datafilepath";
                if (isNotSet(applicationDataDir)) {
                    PropertyService.setPropertyNoPersist(applicationDataDir,
                            externalDir + FileUtil.getFS() + "data");
                }
                final String applicationInlineDir = "application.inlinedatafilepath";
                if (isNotSet(applicationInlineDir)) {
                    PropertyService.setPropertyNoPersist(applicationInlineDir,
                            externalDir + FileUtil.getFS() + "inline-data");
                }
                final String applicationDocumentDir = "application.documentfilepath";
                if (isNotSet(applicationDocumentDir)) {
                    PropertyService.setPropertyNoPersist(applicationDocumentDir,
                            externalDir + FileUtil.getFS() + "documents");
                }
                final String applicationTempDir = "application.tempDir";
                if (isNotSet(applicationTempDir)) {
                    PropertyService.setPropertyNoPersist(applicationTempDir,
                            externalDir + FileUtil.getFS() + "temporary");
                }
                final String replicationLogDir = "replication.logdir";
                if (isNotSet(replicationLogDir)) {
                    PropertyService.setPropertyNoPersist(replicationLogDir,
                            externalDir + FileUtil.getFS() + "logs");
                }
                final String solrHomeDir = "solr.homeDir";
                if (isNotSet(solrHomeDir)) {
                    PropertyService.setPropertyNoPersist(solrHomeDir,
                            externalDir + FileUtil.getFS() + "solr-home");
                }
                
                //This section is to handle the cancel or error scenarios. 
                //So we don't need to persist properties (?)
                PropertyService.persistProperties();
                PropertyService.syncToSettings();

                // Add the list of properties from metacat.properties to the request
                Vector<String> propertyNames = PropertyService.getPropertyNames();
                for (String propertyName : propertyNames) {
                    request.setAttribute(propertyName, PropertyService.getProperty(propertyName));
                }

                // Check for any backup properties and apply them to the
                // request. These are properties from previous configurations. 
                // They keep the user from having to re-enter all values when 
                // upgrading. If this is a first time install, getBackupProperties 
                // will return null.
                SortedProperties backupProperties = null;
                if ((backupProperties =
                        PropertyService.getMainBackupProperties()) != null) {
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
                        "/admin/properties-configuration.jsp", null);

            } catch (GeneralPropertyException gpe) {
                throw new AdminException("PropertiesAdmin.configureProperties - Problem getting or " +
                        "setting property while initializing system properties page: " + gpe.getMessage());
            } catch (MetacatUtilException mue) {
                throw new AdminException("PropertiesAdmin.configureProperties - utility problem while initializing "
                        + "system properties page:" + mue.getMessage());
            } catch (ServiceException se) {
                throw new AdminException("PropertiesAdmin.configureProperties - Service problem while initializing "
                        + "system properties page:" + se.getMessage());
            }
        } else {
            // The configuration form is being submitted and needs to be
            // processed.
            Vector<String> validationErrors = new Vector<String>();
            Vector<String> processingErrors = new Vector<String>();
            Vector<String> processingSuccess = new Vector<String>();

            MetacatVersion metacatVersion = null;

            try {
                metacatVersion = SystemUtil.getMetacatVersion();

                // For each property, check if it is changed and save it
                Vector<String> propertyNames = PropertyService.getPropertyNames();
                for (String name : propertyNames) {
                    PropertyService.checkAndSetProperty(request, name);
                }
                // we need to write the options from memory to the properties
                // file
                PropertyService.persistProperties();

                //auto generate the dataone.mn.baseURL property
                try {
                    String mnUrl =
                        SystemUtil.getInternalContextURL() + "/"
                            + PropertyService.getProperty("dataone.serviceName") + "/"
                            + PropertyService.getProperty("dataone.nodeType");
                    PropertyService.setProperty("dataone.mn.baseURL", mnUrl);
                } catch (Exception ue) {
                    String errorString = "PropertiesAdmin.configureProperties - Could not set the property  dataone.mn.baseURL: " +
                    ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }

                // Validate that the options provided are legitimate. Note that
                // we've allowed them to persist their entries. As of this point
                // there is no other easy way to go back to the configure form
                // and preserve their entries.
                validationErrors.addAll(validateOptions(request));

                // Try to create data directories if necessary.
                String dataDir = PropertyService.getProperty("application.datafilepath");
                try {
                    FileUtil.createDirectory(dataDir);
                } catch (UtilException ue) {
                    String errorString = "PropertiesAdmin.configureProperties - Could not create "
                            + "the Metacat data directory: " + dataDir +
                            " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }

                // Try to create inline-data directories if necessary.
                String inlineDataDir = PropertyService.getProperty("application.inlinedatafilepath");
                try {
                    FileUtil.createDirectory(inlineDataDir);
                } catch (UtilException ue) {
                    String errorString = "PropertiesAdmin.configureProperties - Could not create "
                            + "the Metacat inline data directory: " + inlineDataDir +
                            " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }

                // Try to create document directories if necessary.
                String documentfilepath = PropertyService.getProperty("application.documentfilepath");
                try {
                    FileUtil.createDirectory(documentfilepath);
                } catch (UtilException ue) {
                    String errorString = "PropertiesAdmin.configureProperties - Could not create "
                           + "the Metacat document directory: " + documentfilepath +
                           " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }

                // Try to create temporary directories if necessary.
                String tempDir = PropertyService.getProperty("application.tempDir");
                try {
                    FileUtil.createDirectory(tempDir);
                } catch (UtilException ue) {
                    String errorString = "PropertiesAdmin.configureProperties - Could not create "
                           + "the Metacat temporary directory: " + tempDir +
                           " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }

                // Try to create temporary directories if necessary.
                String replLogDir = PropertyService.getProperty("replication.logdir");
                try {
                    FileUtil.createDirectory(replLogDir);
                } catch (UtilException ue) {
                    String errorString = "PropertiesAdmin.configureProperties - Could not create "
                           + "the Metacat replication log directory: " + replLogDir +
                           " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }

                //make sure the solrHome is not old version of Lucene
                String solrHomePath = PropertyService.getProperty("solr.homeDir");
                boolean isOldVersion = false;
                try {
                    SolrVersionChecker checker = new SolrVersionChecker();
                    isOldVersion = checker.isVersion_3_4(solrHomePath);
                } catch (Exception e) {
                    logMetacat.warn("PropertiesAdmin.configureProperties - we can't determine if "
                         + "the given solr home directory contains an old version of solr, since "
                         + e.getMessage() + ". We will assume it is not an old version.");
                }

                if(isOldVersion) {
                    validationErrors.add("The solr home you chose exists with an old version of SOLR. Please choose a new SOLR home!");
                }


                String indexContext = PropertyService.getProperty("index.context");
                //modify some params of the index context
                this.modifyIndexContextParams(indexContext);

                // set permissions on the registry cgi scripts, least on *nix systems
                try {
                    String cgiFiles =
                            PropertyService.getProperty("application.deployDir")
                            + FileUtil.getFS()
                            + PropertyService.getProperty("application.context")
                            + PropertyService.getProperty("application.cgiDir")
                            + FileUtil.getFS()
                            + "*.cgi";
                    String [] command = {"sh", "-c", "chmod +x " + cgiFiles};
                    Runtime rt = Runtime.getRuntime();
                    Process pr = rt.exec(command);
                    int ret = pr.waitFor();
                    if (ret > 0) {
                        logMetacat.error(IOUtils.toString(pr.getErrorStream()));
                    }
                } catch (Exception ignorable) {
                    /// just a warning
                    logMetacat.warn("Could not set permissions on the registry scripts: " + ignorable.getMessage(), ignorable);
                }

                // write the backup properties to a location outside the 
                // application directories so they will be available after
                // the next upgrade
                PropertyService.persistMainBackupProperties();

            } catch (GeneralPropertyException gpe) {
                String errorMessage = "PropertiesAdmin.configureProperties - Problem getting or setting property while "
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
                    PropertyService.setProperty("configutil.propertiesConfigured",
                            PropertyService.CONFIGURED);

                    // if the db version is already the same as the metacat version,
                    // update metacat.properties. Have to do this after
                    // propertiesConfigured is set to CONFIGURED
                    DBVersion dbVersion = DBAdmin.getInstance().getDBVersion();
                    if (dbVersion != null && metacatVersion != null &&
                            dbVersion.compareTo(metacatVersion) == 0) {
                        PropertyService.setProperty("configutil.databaseConfigured",
                                PropertyService.CONFIGURED);
                        //Also set the upgrade status to be success since the upgrade happened successfully at the previous upgrade
                        try {
                            boolean persist = true;
                            MetacatAdmin.updateUpgradeStatus("configutil.upgrade.database.status", MetacatAdmin.SUCCESS, persist);
                            MetacatAdmin.updateUpgradeStatus("configutil.upgrade.java.status", MetacatAdmin.SUCCESS, persist);
                        } catch (Exception e) {
                            logMetacat.warn("PropertiesAdmin.configureProperties - couldn't update the status of the upgrading process since " + e.getMessage());
                        }
                    }

                    // Reload the main metacat configuration page
                    processingSuccess.add("Properties successfully configured");
                    RequestUtil.clearRequestMessages(request);
                    RequestUtil.setRequestSuccess(request, processingSuccess);
                    RequestUtil.forwardRequest(request, response,
                            "/admin?configureType=configure&processForm=false", null);
                }
            } catch (MetacatUtilException mue) {
                throw new AdminException("PropertiesAdmin.configureProperties - utility problem while processing system "
                        + "properties page: " + mue.getMessage());
            } catch (GeneralPropertyException gpe) {
                throw new AdminException("PropertiesAdmin.configureProperties - problem with properties while "
                        + "processing system properties page: " + gpe.getMessage());
            }
        }
    }


    /**
     * In the web.xml of the Metacat-index context, there is a parameter:
     * <code>
     *
     *   <context-param>
     *       <param-name>metacat.properties.path</param-name>
     *       <param-value>/metacat/WEB-INF/metacat.properties</param-value>
     *       <description>The metacat.properties file for sibling metacat deployment. Note that the
     *       context can change</description>
     *   </context-param>
     *
     *  </code>
     *  It points to the default metacat context. If we rename the context, we need to change the
     *  value there.
     */
    private void modifyIndexContextParams(String indexContext) {

        if (indexContext != null) {
            String indexConfigFile = null;
            String metacatContext = null;
            try {
                if (StringUtils.isNotBlank(indexContext)) {
                    indexConfigFile =
                        Paths.get(PropertyService.getProperty("application.deployDir"),
                            indexContext, "WEB-INF", "web.xml").toString();
                } else {
                    throw new IllegalArgumentException(
                        "Error - blank Index Context received - " + indexContext);
                }
                metacatContext = PropertyService.getProperty("application.context");

            } catch (IllegalArgumentException | PropertyNotFoundException e) {
                String errorMessage = "PropertiesAdmin.modifyIndexContextParams - Problem getting/"
                    + "setting \"metacat.properties.path\" in web.xml for metacat-index context : "
                    + e.getMessage();
                logMetacat.error(errorMessage, e);
            }

            String webXmlContents;
            try {
                webXmlContents = FileUtil.readFileToString(indexConfigFile, "UTF-8");
                logMetacat.debug(
                    "modifyIndexContextParams(): editing web.xml file: " + indexConfigFile);

                webXmlContents = updateMetacatPropertiesPath(metacatContext, webXmlContents);

                FileUtil.writeFile(indexConfigFile, new StringReader(webXmlContents), "UTF-8");

            } catch (UtilException e) {
                String errorMessage = "PropertiesAdmin.modifyIndexContextParams - Problem reading "
                    + "from or writing to the web.xml file from path: " + indexConfigFile
                    + ". Error was: " + e.getMessage();
                logMetacat.error(errorMessage, e);
            }
        }
    }

    protected String updateMetacatPropertiesPath(String metacatContext, String webXmlContents) {
        if (metacatContext != null && !metacatContext.equals(DEFAULT_METACAT_CONTEXT)) {
            webXmlContents = webXmlContents.replace(
                SLASH + DEFAULT_METACAT_CONTEXT + METACAT_PROPERTY_APPENDIX,
                SLASH + metacatContext + METACAT_PROPERTY_APPENDIX);
        }
        return webXmlContents;
    }

    /**
     * Validate the most important configuration options submitted by the user.
     *
     * @param request
     *            the http request object
     *
     * @return a vector holding error message for any fields that fail
     *         validation.
     */
    protected Vector<String> validateOptions(HttpServletRequest request) {
        Vector<String> errorVector = new Vector<String>();

        // Test database connectivity
        try {
            String dbError = DBAdmin.getInstance().validateDBConnectivity(
                    request.getParameter("database.driver"),
                    request.getParameter("database.connectionURI"),
                    request.getParameter("database.user"),
                    request.getParameter("database.password"));
            if (dbError != null) {
                errorVector.add(dbError);
            }
        } catch (AdminException ae) {
            errorVector.add("Could not instantiate database admin: "
                    + ae.getMessage());
        }

        return errorVector;
    }
    
    /**
     * Determine if the given property is set or not
     * @param propertyKey  the name of the key which will be checked
     * @return true if the property is not set; otherwise false
     */
    private boolean isNotSet(String propertyKey) { 
        String property = null; 
        try { 
            property = PropertyService.getProperty(propertyKey); 
        } catch (PropertyNotFoundException ee) { 
            property = null; 
        }
        return (property == null || property.trim().equals("")); 
    }

}
