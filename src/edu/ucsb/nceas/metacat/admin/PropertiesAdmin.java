/**
 *  '$RCSfile$'
 *    Purpose:  A Class that implements main property configuration methods
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

import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import edu.ucsb.nceas.utilities.SortedProperties;
import edu.ucsb.nceas.utilities.UtilException;

/**
 * Control the display of the main properties configuration page and the 
 * processing of the configuration values.
 */
public class PropertiesAdmin extends MetacatAdmin {
    private static String SLASH = "/";
    private static String DEFAULT_METACAT_CONTEXT = "metacat";
    private static String METACAT_PROPERTY_APPENDIX = "/WEB-INF/metacat.properties";
    private static PropertiesAdmin propertiesAdmin = null;
    private static Log logMetacat = LogFactory.getLog(PropertiesAdmin.class);

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
                PropertyService.setPropertyNoPersist("application.context",
                        ServiceService.getRealApplicationContext());
                PropertyService.setPropertyNoPersist("server.name", SystemUtil
                        .discoverServerName(request));
                PropertyService.setPropertyNoPersist("server.httpPort", SystemUtil
                        .discoverServerPort(request));
                PropertyService.setPropertyNoPersist("server.httpSSLPort",
                        SystemUtil.discoverServerSSLPort(request));
                PropertyService.setPropertyNoPersist("application.deployDir",
                        SystemUtil.discoverDeployDir(request));
                PropertyService.setPropertyNoPersist("application.sitePropertiesDir",
                        externalDir + FileUtil.getFS() + "config");
                PropertyService.setPropertyNoPersist("application.datafilepath",
                        externalDir + FileUtil.getFS() + "data");
                PropertyService.setPropertyNoPersist("application.inlinedatafilepath",
                        externalDir + FileUtil.getFS() + "inline-data");
                PropertyService.setPropertyNoPersist("application.documentfilepath",
                        externalDir + FileUtil.getFS() + "documents");
                PropertyService.setPropertyNoPersist("application.tempDir",
                        externalDir + FileUtil.getFS() + "temporary");
                PropertyService.setPropertyNoPersist("replication.logdir",
                        externalDir + FileUtil.getFS() + "logs");
                PropertyService.setPropertyNoPersist("solr.homeDir",
                        externalDir + FileUtil.getFS() + "solr-home");

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
                    PropertyService.setProperty("dataone.mn.baseURL", SystemUtil.getInternalContextURL()+"/"+
                            PropertyService.getProperty("dataone.serviceName")+"/" + PropertyService.getProperty("dataone.nodeType"));
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
                    String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + dataDir +
                    " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }
                
                // Try to create inline-data directories if necessary.
                String inlineDataDir = PropertyService.getProperty("application.inlinedatafilepath");
                try {
                    FileUtil.createDirectory(inlineDataDir);
                } catch (UtilException ue) {
                    String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + inlineDataDir +
                        " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }
                
                // Try to create document directories if necessary.
                String documentfilepath = PropertyService.getProperty("application.documentfilepath");
                try {
                    FileUtil.createDirectory(documentfilepath);
                } catch (UtilException ue) {    
                    String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + documentfilepath +
                        " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }
                
                // Try to create temporary directories if necessary.
                String tempDir = PropertyService.getProperty("application.tempDir");
                try {
                    FileUtil.createDirectory(tempDir);
                } catch (UtilException ue) {        
                    String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + tempDir +
                        " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    validationErrors.add(errorString);
                }
                
                // Try to create temporary directories if necessary.
                String replLogDir = PropertyService.getProperty("replication.logdir");
                try {
                    FileUtil.createDirectory(replLogDir);
                } catch (UtilException ue) {        
                    String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + replLogDir +
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
                    logMetacat.warn("PropertiesAdmin.confgureProperties - we can't determine if the given directory is a old version of solr  since "+e.getMessage()+". But we consider it is not an old version.");
                }
                
                if(isOldVersion) {
                    validationErrors.add("The solr home you chose exists with an old version of SOLR. Please choose a new SOLR home!");
                }


                String indexContext = PropertyService.getProperty("index.context");
                //modify some params of the index context
                this.modifyIndexContextParams(indexContext);
                
                // make sure hazelcast.xml uses a unique group name
                this.modifyHazelcastConfig();
                
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
     * <context-param>
     * <param-name>metacat.properties.path</param-name>
     * <param-value>/metacat/WEB-INF/metacat.properties</param-value>
     * <description>The metacat.properties file for sibling metacat deployment. Note that the context can change</description>
     *  </context-param>
     *  It points to the default metacat context - knb. If we rename the context, we need to change the value of there.
     *
     *  There is also a parameter:
     * <context-param>
     * <param-name>site.properties.path</param-name>
     * <param-value>/var/metacat/context/metacat-site.properties</param-value>
     * <description>The metacat-site.properties file for sibling metacat deployment.</description>
     * </context-param>
     * It points to the site-specific configured settings, that are overlaid on top of the
     * defaults in metacat.properties. If we change the location of this properties file, we need
     * to change this value.
     */
    private void modifyIndexContextParams(String indexContext) {

        if (indexContext != null) {
            String indexConfigFile = null;
            String metacatContext = null;
            String sitePropsDir = null;
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
                sitePropsDir = PropertyService.getProperty("application.sitePropertiesDir");

            } catch (IllegalArgumentException | PropertyNotFoundException e) {
                String errorMessage = "PropertiesAdmin.modifyIndexWebXml - Problem getting/setting "
                    + "the \"metacat.properties.path\" or the \"site.properties.path\" in the "
                    + "web.xml of the index context : " + e.getMessage();
                logMetacat.error(errorMessage, e);
            }

            String webXmlContents = null;
            try {
                webXmlContents = FileUtil.readFileToString(indexConfigFile, "UTF-8");

                logMetacat.debug("modifyIndexWebXml(): the web.xml file is: " + indexConfigFile);

                if (metacatContext != null && !metacatContext.equals(DEFAULT_METACAT_CONTEXT)) {
                    webXmlContents = webXmlContents.replace(
                        SLASH + DEFAULT_METACAT_CONTEXT + METACAT_PROPERTY_APPENDIX,
                        SLASH + metacatContext + METACAT_PROPERTY_APPENDIX);
                }
                String defaultSitePropertiesDir =
                    PropertyService.getDefaultProperty("application.sitePropertiesDir");
                if (sitePropsDir != null && !sitePropsDir.equals(defaultSitePropertiesDir)) {
                    webXmlContents = webXmlContents.replace(defaultSitePropertiesDir, sitePropsDir);
                }
                logMetacat.debug("modifyIndexWebXml(): Web.xml contents AFTER modification: \n"
                    + webXmlContents);

                FileUtil.writeFile(indexConfigFile, new StringReader(webXmlContents), "UTF-8");

            } catch (UtilException e) {
                String errorMessage = "PropertiesAdmin.modifyIndexWebXml - Problem reading from or "
                    + "writing to the web.xml file from path: " + indexConfigFile + ". Error was: "
                    + e.getMessage();
                logMetacat.error(errorMessage, e);
            }
        }
    }




    /**
     * Changes the Hazelcast group name to match the current context
     * This ensures we do not share the same group if multiple Metacat 
     * instances are running in the same Tomcat container.
     */
    private void modifyHazelcastConfig() {
        try {
            String metacatContext = PropertyService.getProperty("application.context");
            //System.out.println("the metacat context is ========================="+metacatContext);
            if (metacatContext != null) {
                String hzConfigFile = 
                                PropertyService.getProperty("application.deployDir")
                                + FileUtil.getFS()
                                + metacatContext
                                + FileUtil.getFS() 
                                + "WEB-INF"
                                + FileUtil.getFS()
                                + "hazelcast.xml";
                //System.out.println("============================== the web.xml file is "+indexConfigFile);
                String configContents = FileUtil.readFileToString(hzConfigFile, "UTF-8");
                //System.out.println("============================== the content of web.xml file is "+configContents);
                configContents = configContents.replace("<name>metacat</name>", "<name>" + metacatContext + "</name>");
                FileUtil.writeFile(hzConfigFile, new StringReader(configContents), "UTF-8");
            }
            
        } catch (Exception e) {
            String errorMessage = "PropertiesAdmin.configureProperties - Problem setting groupName in hazelcast.xml: " + e.getMessage();
            logMetacat.error(errorMessage);
        }
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
    

}
