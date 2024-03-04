package edu.ucsb.nceas.metacat.admin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.CoreStatus;
import org.dataone.service.exceptions.UnsupportedType;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.admin.upgrade.UpgradeUtilityInterface;
import edu.ucsb.nceas.metacat.admin.upgrade.solr.SolrJvmVersionFinder;
import edu.ucsb.nceas.metacat.admin.upgrade.solr.SolrSchemaModificationException;
import edu.ucsb.nceas.metacat.common.SolrServerFactory;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DBVersion;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;

/**
 * Control the display of the Solr configuration page and the processing
 * of the configuration values.
 */
public class SolrAdmin extends MetacatAdmin {

    private static SolrAdmin solrAdmin = null;
    private static Log logMetacat = LogFactory.getLog(SolrAdmin.class);
    //possibilities:
    //1. Create - both core and solr-home doesn't exist. Create solr-home and register the core.
    public static final String CREATE = "create";
    //2. Register - core doesn't exist, but the solr-home directory does exist without schema
    // update indication.
    public static final String REGISTER = "register";
    //3. RegisterWithUpdate - core doesn't exist, but the solr-home directory does exist with schema
    // update indication.
    public static final String REGISTERANDUPDATE = "registerAndUpdate";
    //4. CreateWithWarning - core does exist, but its instance directory is different to the
    // solr-home in the properties file and the solr home doesn't exist.
    // Ask users to choose a new core name associating with the new solr-home or keep the original
    // one. If keeping the original one, no update will need.
    public static final String CREATEWITHWARN = "createWithWarn";
    //4.1 CreateOrUpdateWithWarning - core does exist, but the instance directory is different
    // to the solr-home in the properties file and the solr home doesn't exist.
    //Ask users to choose a new core name associating with the new solr-home or keep the original
    // one. If keeping the original one, a schema update will need
    public static final String CREATEORUPDATEWITHWARN = "createOrUpdateWithWarn";
    //5. RegisterWithWarning - core does exist, but its instance directory is different to the
    // solr-home in the properties file and the solr home does exist and no schema update.
    // Ask users to choose a new core name associating with the new solr-home or keep the original
    // one. If keeping the original one, nothing will be done.
    public static final String REGISTERWITHWARN = "RegisterWithWarn";
     //6. RegisterAndUpdateWithWarning - core does exist, but its instance directory is different
    // to the solr-home in the properties file and solr home does exist and needing schema update.
    // Ask users to choose a new core name associating with the new solr-home or keep the original
    // one. If keeping the original one, a schema update will need
    public static final String REGISTERANDUPDATEWITHWARN = "RegisterAndUpdateWithWarn";
    //7. Skip - both core and solr-home does exist. And the core's instance directory is as same
    // as the solr-home. There is no schema update indication
    public static final String KEEP = "KEEP";
    //8. Update - both core and solr-home does exist. And the core's instance directory is
    // as same as the solr-home. There is a schema update indication
    private static final String SOLRXMLFILENAME = "solr.xml";
    public static final String UPDATE = "update";
    public static final String UNKNOWN = "Unknown";
    public static final String ACTION = "action";
    public static final String CURRENTCOREINSTANCEDIR = "core-current-instance-dir";
    public static final String NEWSOLRCOREORNOT = "newSolrCoreOrNot";
    public static final String NEWSOLRCORE = "newSolrCore";
    public static final String EXISTINGCORE = "existingCore";
    public static final String SOLRCORENAME = "solrCoreName";
    public static final String NEWSOLCORENAME = "newSolrCoreName";

    private static final String CONF = "conf";
    private static final String DATA = "data";
    private static final String CORE_PROPERTY = "core.properties";
    private static final String SOLR_HOME = "SOLR_HOME";

    private SolrSchemaModificationException solrSchemaException = null;
    // This map maps the db version and relevant the upgrade class
    private Map<String, String> updateClassMap;
    private UnsupportedOperationException unsupportedOperException = null;

    /**
     * Private constructor since this is a singleton
     * @throws AdminException
     */
    private SolrAdmin() throws AdminException {
        try {
            updateClassMap = getSolrUpdateClasses();
        } catch (SQLException | PropertyNotFoundException e) {
            throw new AdminException(e.getMessage());
        }

    }

    /**
     * Get the single instance of SolrDAdmin.
     *
     * @return the single instance of SolrAdmin
     * @throws AdminException
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
     * @throws AdminException
     */
    public void configureSolr(HttpServletRequest request,
            HttpServletResponse response) throws AdminException {
        String processForm = request.getParameter("processForm");
        String bypass = request.getParameter("bypass");
        String formErrors = (String) request.getAttribute("formErrors");

        if (processForm == null || !processForm.equals("true") || formErrors != null) {
            // check the current solr status
            // and send back the possible actions which user can operate.
            checkSolrStatus(request, response);
        } else if (bypass != null && bypass.equals("true")) {
            // Bypass the solr configuration. This will not keep
            // Metacat from running.
            Vector<String> processingErrors = new Vector<String>();
            Vector<String> processingSuccess = new Vector<String>();
            try {
                PropertyService.setProperty("configutil.solrserverConfigured",
                        PropertyService.BYPASSED);
                //set the upgrade process to success even though we bypassed it.
                try {
                    boolean persist = true;
                    MetacatAdmin.updateUpgradeStatus("configutil.upgrade.solr.status",
                                                                    MetacatAdmin.SUCCESS, persist);
                } catch (Exception e) {
                    logMetacat.warn("SolrAdmin.configureSolr - couldn't update the status of the"
                                + " upgrading Solr process since " + e.getMessage());
                }

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
            // processed. This branch handles the action which the users choose.
            processResponse(request, response);
        }
    }

    /**
     * Check the current solr status - such as if the solr core/solr-home directory exists,
     * and send back actions which user can choose based on the status.
     * @param request  the request from the client
     * @param response  the response which will be sent back to the client
     * @throws AdminException
     */
    protected void checkSolrStatus(HttpServletRequest request, HttpServletResponse response)
                                                                            throws AdminException {
        // The servlet configuration parameters have not been set, or there
        // were form errors on the last attempt to configure, so redirect to
        // the web form for configuring metacat
        // This branch will show the initial solr admin page.
        try {
            // get the current configuration values
            String baseURL = PropertyService.getProperty("solr.baseURL");
            String coreName = PropertyService.getProperty("solr.coreName");
            String solrHomePath = PropertyService.getProperty("solr.homeDir");
            if(coreName == null || coreName.isBlank()) {
                throw new AdminException("The Solr core name shouldn't be null or blank. Please"
                  + " go back to the Metacat Global Properties admin page to configure again.");
            }

            if(solrHomePath == null || solrHomePath.isBlank()) {
                throw new AdminException("The Solr home path shouldn't be null or blank. Please"
                  + " go back to the Metacat Global Properties admin page to configure again.");
            }
            if (solrHomePath.endsWith("/")) {
                //remove the last "/"
                solrHomePath = solrHomePath.substring(0, solrHomePath.lastIndexOf("/"));
            }

            boolean solrHomeExists = new File(solrHomePath).exists();
            if (solrHomeExists) {
                // check it
                if (!FileUtil.isDirectory(solrHomePath)) {
                    throw new AdminException("SolrAdmin.configureProperties - SOLR home is not "
                                              + "a directory: " + solrHomePath);
                }
            }
            boolean solrHomeConfExists = new File(solrHomePath + "/" + CONF).exists();
            boolean solrHomeDataExists = new File(solrHomePath + "/"+ DATA).exists();
            boolean solrHomeCoreExists = new File(solrHomePath + "/" + CORE_PROPERTY).exists();
            request.setAttribute("solrHomeExist", (Boolean) solrHomeExists);
            request.setAttribute("solrHomeValueInProp", solrHomePath);

            boolean updateSchema = false;
            if(updateClassMap != null && !updateClassMap.isEmpty()) {
                updateSchema = true;
            }

            //check the solr-home for given core name
            String solrHomeForGivenCore = getInstanceDir(coreName);
            request.setAttribute("solrCore", coreName);
            if(solrHomeForGivenCore != null) {
               //the given core  exists
                request.setAttribute("solrHomeForGivenCore", solrHomeForGivenCore);
            }

            logMetacat.info("SolrAdmin.configureSolr -the solr-home on the properties is "
                            + solrHomePath + " and doe it exist? " + solrHomeExists);
            logMetacat.info("SolrAdmin.configureSolr - the instance directory for the core "
                             + coreName + " is " + solrHomeForGivenCore
                             + " If it is null, this means the core doesn't exit.");
            logMetacat.info("SolrAdmin.configureSolr - in this upgrade/installation, do we need"
                             + " to update the schema file?" + updateSchema);
            if(solrHomeForGivenCore == null && (!solrHomeExists
                    || (!solrHomeConfExists && !solrHomeCoreExists && !solrHomeDataExists))) {
                //action 1 - create (no core and no solr home)
                request.setAttribute(ACTION, CREATE);
            } else if (solrHomeForGivenCore == null && solrHomeExists && !updateSchema) {
                //action 2 - register (no core but having solr home and no schema update)
                request.setAttribute(ACTION, REGISTER);
            } else if (solrHomeForGivenCore == null && solrHomeExists && updateSchema) {
                //action 3 - register (no core but having solr home and having schema update)
                request.setAttribute(ACTION, REGISTERANDUPDATE);
            } else if (solrHomeForGivenCore != null && !solrHomeForGivenCore.equals(solrHomePath)
                                                      && !solrHomeExists && !updateSchema) {
               //action 4. createWithWarning - core does exist, but its instance directory is
               //different to the solr-home in the properties file, and solr home doesn't exist.
               // Ask users to choose a new core name associating with the new solr-home or
                // keep the original one. If keeping the original one, no update will need.
                request.setAttribute(ACTION, CREATEWITHWARN);
            } else if (solrHomeForGivenCore != null && !solrHomeForGivenCore.equals(solrHomePath)
                                                        && !solrHomeExists && updateSchema) {
                //4.1 CreateOrUpdateWithWarning - core does exist, but its the instance
                // directory is different to the solr-home in the properties file and solr home
                // doesn't exist.
                //Ask users to choose a new core name associating with the new solr-home or
                // keep the original one. If keeping the original one, a schema update will need
                request.setAttribute(ACTION, CREATEORUPDATEWITHWARN);
            } else if (solrHomeForGivenCore != null && !solrHomeForGivenCore.equals(solrHomePath)
                                                        && solrHomeExists && !updateSchema) {
              //action 5. RegisterWithWarning - core does exist, but its instance directory is
              // different to the solr-home in the properties file and solr home does exist and
              // no schema update.
              // Ask users to choose a new core name associating with the new solr-home or
                // keep the original one. If keeping the original one, nothing will be done.
                request.setAttribute(ACTION, REGISTERWITHWARN);
            } else if(solrHomeForGivenCore != null && !solrHomeForGivenCore.equals(solrHomePath)
                                                        && solrHomeExists && updateSchema) {
                //action 6. RegisterAndUpdateWithWarning - core does exist, but its instance
                // directory is different to the solr-home in the properties file and
                // solr home does exist and needing schema update.
                // Ask users to choose a new core name associating with the new solr-home or
                // keep the original one. If keeping the original one, a schema update will need
                request.setAttribute(ACTION, REGISTERANDUPDATEWITHWARN);
            } else if (solrHomeForGivenCore != null && solrHomeForGivenCore.equals(solrHomePath)
                                                        && solrHomeExists && !updateSchema) {
                //action 7. Keep - both core and solr-home does exist. And the core's instance
                //directory is as same as the solr-home. There is no schema update indication
                request.setAttribute(ACTION, KEEP);
            } else if (solrHomeForGivenCore != null && solrHomeForGivenCore.equals(solrHomePath)
                                                        && solrHomeExists && updateSchema) {
                //action 8. Update - both core and solr-home does exist. And the core's
                // instance directory is as same as the solr-home. There is a schema
                //update indication
                request.setAttribute(ACTION, UPDATE);
            } else {
                request.setAttribute(ACTION, UNKNOWN);
            }

            // Forward the request to the JSP page
            RequestUtil.forwardRequest(request, response,
                    "/admin/solr-configuration.jsp", null);
        } catch (GeneralPropertyException gpe) {
            throw new AdminException("SolrAdmin.configureSolr - Problem getting or " +
                    "setting property while initializing solr page: " + gpe.getMessage());
        } catch (MetacatUtilException mue) {
            throw new AdminException("SolrAdmin.configureSolr- utility problem while initializing "
                    + "solr page:" + mue.getMessage());
        } catch (UnsupportedType e) {
            throw new AdminException("SolrAdmin.configureSolr- unsupported type problem"
                                    + " while initializing " + "solr page:" + e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new AdminException("SolrAdmin.configureSolr- parser configuration problem "
                                    + "while initializing " + "solr page:" + e.getMessage());
        } catch (IOException e) {
            throw new AdminException("SolrAdmin.configureSolr- io problem while initializing "
                    + "solr page:" + e.getMessage());
        } catch (SAXException e) {
            throw new AdminException("SolrAdmin.configureSolr- SAX problem while initializing "
                    + "solr page:" + e.getMessage());
        } catch (SolrServerException e) {
            throw new AdminException("SolrAdmin.configureSolr- solr problem while initializing "
                    + "solr page:" + e.getMessage());
        }
    }

    /**
     * Process the user's choices to the solr server
     * @param request  the request from the client
     * @param response  the response which will be sent back to the client
     * @throws AdminException
     */
    protected void processResponse(HttpServletRequest request, HttpServletResponse response)
                                                                            throws AdminException {

        Vector<String> validationErrors = new Vector<String>();
        Vector<String> processingErrors = new Vector<String>();
        Vector<String> processingSuccess = new Vector<String>();

        try {
            // Validate that the options provided are legitimate. Note that
            // we've allowed them to persist their entries. As of this point
            // there is no other easy way to go back to the configure form
            // and preserve their entries.
            validationErrors.addAll(validateOptions(request));
            String action = request.getParameter(ACTION);
            logMetacat.info("SolrAdmin.configureSolr - the action which users choose is " + action);
            if (action == null || action.isBlank()) {
                throw new Exception("The action on the request can't be null or blank.");
            }
            switch (action) {
                case CREATE:
                    //action 1 - create (no core and no solr home)
                    try {
                        createSolrHome();
                        //set the solr_upgraded status true for the current Metacat version
                        updateSolrStatus(DBAdmin.getInstance().getDBVersion().getVersionString(),
                                        true);
                    }  catch (UnsupportedOperationException usoe) {
                        unsupportedOperException = usoe;
                    }
                    registerSolrCore();
                    break;
                case REGISTER:
                    //action 2 - register (no core but having solr home and no schema update)
                    registerSolrCore();
                    break;
                case REGISTERANDUPDATE:
                    //action 3 - register (no core but having solr home and having schema update)
                    registerSolrCore();
                    updateSolrSchema();
                    break;
                case CREATEWITHWARN, CREATEORUPDATEWITHWARN, REGISTERWITHWARN, REGISTERANDUPDATEWITHWARN:
                    handleWarningActions(action, request, processingErrors);
                    break;
                case KEEP:
                    //action 7. Keep - both core and solr-home does exist. And the core's instance
                    //directory is as same as the solr-home. There is no schema update indication
                    //do nothing
                    break;
                case UPDATE:
                    //action 8. Update - both core and solr-home does exist. And the core's instance
                    //directory is as same as the solr-home. There is a schema update indication
                    updateSolrSchema();
                    break;
                default:
                    throw new AdminException("The action " + action
                                             + " can't be handled in the solr configuration.");
            }
        }  catch (Exception gpe) {
            String errorMessage = "SolrAdmin.configureSolr - Problem processing the solr setting since "
                                                                        + gpe.getMessage();
            logMetacat.error(errorMessage);
            processingErrors.add(errorMessage);
        }

        try {
            boolean persist = true;
            if (validationErrors.size() > 0 || processingErrors.size() > 0) {
                //set the upgrade process to failure
                try {
                    MetacatAdmin.updateUpgradeStatus("configutil.upgrade.solr.status",
                                                                MetacatAdmin.FAILURE, persist);
                } catch (Exception e) {
                    logMetacat.warn("SolrAdmin.configureSolr - couldn't update the status of "
                                    + "the upgrading Solr process since " + e.getMessage());
                }
                RequestUtil.clearRequestMessages(request);
                RequestUtil.setRequestFormErrors(request, validationErrors);
                RequestUtil.setRequestErrors(request, processingErrors);
                RequestUtil.forwardRequest(request, response, "/admin", null);
            } else {
                // Now that the options have been set, change the
                // 'propertiesConfigured' option to 'true'
                PropertyService.setProperty("configutil.solrserverConfigured",
                        PropertyService.CONFIGURED);
                //set the upgrade process to success
                try {
                    MetacatAdmin.updateUpgradeStatus("configutil.upgrade.solr.status",
                                                                MetacatAdmin.SUCCESS, persist);
                } catch (Exception e) {
                    logMetacat.warn("SolrAdmin.configureSolr - couldn't update the status of the"
                                        + " upgrading Solr process since " + e.getMessage());
                }
                if(solrSchemaException != null) {
                    //Show the warning message
                    Vector<String> errorVector = new Vector<String>();
                    errorVector.add(solrSchemaException.getMessage());
                    RequestUtil.clearRequestMessages(request);
                    //request.setAttribute("supportEmail", supportEmail);
                    RequestUtil.setRequestErrors(request, errorVector);
                    RequestUtil.forwardRequest(request, response,
                                    "/admin/solr-schema-warn.jsp", null);
                } else if (unsupportedOperException != null) {
                        //Show the warning message
                        Vector<String> errorVector = new Vector<String>();
                        errorVector.add(unsupportedOperException.getMessage());
                        RequestUtil.clearRequestMessages(request);
                        //request.setAttribute("supportEmail", supportEmail);
                        RequestUtil.setRequestErrors(request, errorVector);
                        RequestUtil.forwardRequest(request, response,
                                        "/admin/solr-warn.jsp", null);

                } else {
                        // Reload the main metacat configuration page
                        processingSuccess.add("Solr server was successfully configured");
                        RequestUtil.clearRequestMessages(request);
                        RequestUtil.setRequestSuccess(request, processingSuccess);
                        RequestUtil.forwardRequest(request, response,
                                "/admin?configureType=configure&processForm=false", null);
                }
            }
        } catch (MetacatUtilException mue) {
            throw new AdminException("SolrAdmin.configureSolr - utility problem while processing"
                    + "solr services page: " + mue.getMessage());
        } catch (GeneralPropertyException gpe) {
            throw new AdminException("SolrAdmin.configureSolr - problem with properties while "
                    + "processing solr services configuration page: " + gpe.getMessage());
        }
    }

    /**
     * Method to handle a warning message in the request, which have user's response.
     * @param action   the action needs to be handled
     * @param request  the request from the client
     * @param processingErrors  the vector to store error message
     * @throws GeneralPropertyException
     * @throws AdminException
     * @throws UnsupportedType
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws SolrServerException
     * @throws ServiceException
     * @throws UtilException
     */
    protected void handleWarningActions(String action, HttpServletRequest request,
                            Vector<String> processingErrors) throws GeneralPropertyException,
                            AdminException, UnsupportedType, ParserConfigurationException,
                            IOException, SAXException, SolrServerException, ServiceException,
                            UtilException {
      //action 4. createWithWarning - core does exist, but its instance directory
        //is different to the solr-home in the properties file, and solr home doesn't exist.
        // Ask users to choose a new core name associating with the new solr-home or keep
        //the original one. If keeping the original one, no update will need.
        //4.1 CreateOrUpdateWithWarning - core does exist, but the instance directory
        //is different to the solr-home in the properties file and solr home doesn't exist.
        //Ask users to choose a new core name associating with the new solr-home or
        //keep the original one. If keeping the original one, a schema update will need
        //action 5. RegisterWithWarning - core does exist, but its instance directory
        //is different to the solr-home in the properties file and solr home does exist
        //and no schema update.
        // Ask users to choose a new core name associating with the new solr-home or
        //keep the original one. If keeping the original one, nothing will be done.
        //action 6. RegisterAndUpdateWithWarning - core does exist, but its instance
        //directory is different to the solr-home in the properties file and solr home
        //does exist and needing schema update.
        // Ask users to choose a new core name associating with the new solr-home or
        //keep the original one. If keeping the original one, a schema update will need
        String userChoice =  request.getParameter(NEWSOLRCOREORNOT);
        logMetacat.info("SolrAdmin.configureSolr - the user's choice is " + userChoice);
        if(userChoice != null && userChoice.equals(NEWSOLRCORE)) {
            //users choose to use a new core name to associate the solr-home
            String newCoreName = request.getParameter(NEWSOLCORENAME);
            logMetacat.info("SolrAdmin.configureSolr - the new solr core name users "
                                + " chose is " + newCoreName);
            if (newCoreName == null || newCoreName.isBlank()) {
                //users chose a null as the name.
                String error = "The new core name shouldn't be null or blank";
                processingErrors.add(error);
            } else {
                String instanceDirForNewCore = getInstanceDir(newCoreName);
                if(instanceDirForNewCore != null) {
                    //the new core still exists! we need to ask user to choose again.
                    String error = "The new core name " + newCoreName
                             + " is used by another core. Please choose another name";
                    processingErrors.add(error);
                } else {
                    //change the solr.coreName in the metacat.properties
                    PropertyService.setPropertyNoPersist("solr.coreName", newCoreName);
                    // persist them all
                    PropertyService.persistProperties();
                    PropertyService.syncToSettings();
                    // save a backup in case the form has errors, we reload from these
                    PropertyService.persistMainBackupProperties();
                    if(action.equals(CREATEWITHWARN) || action.equals(CREATEORUPDATEWITHWARN)) {
                        try {
                            createSolrHome();
                        }  catch (UnsupportedOperationException usoe) {
                            unsupportedOperException = usoe;
                        }
                        registerSolrCore();
                    } else if (action.equals(REGISTERWITHWARN)) {
                        registerSolrCore();
                    } else if (action.equals(REGISTERANDUPDATEWITHWARN)) {
                        registerSolrCore();
                        updateSolrSchema();
                    }
                }
            }
        } else if (userChoice != null && userChoice.equals(EXISTINGCORE)) {
            //users choose to keep the core name but use its current instance directory
            // as the solr-home we need to change the solr.homeDir in metacat.properties
            String currentSolrInstanceDir = request.getParameter(CURRENTCOREINSTANCEDIR);
            logMetacat.info("SolrAdmin.configureSolr - the current solr instance directory is "
                                                        + currentSolrInstanceDir);
            PropertyService.setPropertyNoPersist("solr.homeDir", currentSolrInstanceDir);
            // persist them all
            PropertyService.persistProperties();
            PropertyService.syncToSettings();
            // save a backup in case the form has errors, we reload from these
            PropertyService.persistMainBackupProperties();
            if(action.equals(CREATEORUPDATEWITHWARN)
                                        || action.equals(REGISTERANDUPDATEWITHWARN)) {
                //we also need to update the schema
                  updateSolrSchema();
              }

        } else {
            throw new AdminException("User's choice "+userChoice+" can't be understood.");
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

    /**
     * Get the instance directory of a given core.
     * @param coreName  the core will be looked for
     * @return the instance directory of the core.
     * @throws UnsupportedType
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws SolrServerException
     */
    private String getInstanceDir(String coreName) throws UnsupportedType,
                      ParserConfigurationException, IOException, SAXException, SolrServerException {
        String instanceDir = null;
        SolrClient client = SolrServerFactory.createSolrAdminClient();
        CoreStatus status = CoreAdminRequest.getCoreStatus(coreName, client);
        if(status != null) {
            try {
                instanceDir = status.getInstanceDirectory();
            } catch (NullPointerException e) {
                //The getInstanceDirectory method doesn't handle the scenario that the core
                //doesn't exist. It will give a null exception.
                logMetacat.warn("SolrAdmin.getInstanceDir - Solr didn't find the core " + coreName
                                + " So the instance directory will be null.");
            }
        }
        return instanceDir;
     }

    /**
     * Create the solr home when it is necessary
     * @throws PropertyNotFoundException
     * @throws IOException
     * @throws ServiceException
     * @throws UtilException
     */
    private void createSolrHome() throws PropertyNotFoundException, IOException,
                                                                  ServiceException, UtilException {
            // Try to create and initialize the solr-home directory if necessary.
            String solrHomePath = PropertyService.getProperty("solr.homeDir");
            String indexContext = PropertyService.getProperty("index.context");
            boolean solrHomeExists = new File(solrHomePath).exists();
            boolean solrHomeConfExists = new File(solrHomePath + "/" + CONF).exists();
            boolean solrHomeDataExists = new File(solrHomePath + "/"+ DATA).exists();
            boolean solrHomeCoreExists = new File(solrHomePath + "/" + CORE_PROPERTY).exists();
            if (!solrHomeExists || (!solrHomeConfExists && !solrHomeCoreExists && !solrHomeDataExists)) {
                try {
                    String metacatWebInf = ServiceService.getRealConfigDir();
                    String metacatIndexSolrHome = metacatWebInf + "/../../" + indexContext
                                                                + "/WEB-INF/classes/solr-home";
                    // only attempt to copy if we have the source directory to copy from
                    File sourceDir = new File(metacatIndexSolrHome);
                    if (sourceDir.exists()) {
                        FileUtil.createDirectory(solrHomePath);
                        OrFileFilter fileFilter = new OrFileFilter();
                        fileFilter.addFileFilter(DirectoryFileFilter.DIRECTORY);
                        //Check if solr.xml exists. If it exists, don't overwrite it.
                        boolean solrXmlExists = new File(solrHomePath + "/" + SOLRXMLFILENAME).exists();
                        if (!solrXmlExists) {
                            fileFilter.addFileFilter(new WildcardFileFilter("*"));
                        } else {
                            fileFilter
                             .addFileFilter(new NotFileFilter(new NameFileFilter(SOLRXMLFILENAME)));
                        }
                        FileUtils.copyDirectory(new File(metacatIndexSolrHome),
                                                               new File(solrHomePath), fileFilter );
                        //The solr home directory will be owned by the solr user,
                        //but the tomcat group has the permission to write
                        Path solrHomePathObj = Paths.get(solrHomePath);
                        final Set<PosixFilePermission> perms = new HashSet<>();
                        perms.add(PosixFilePermission.OWNER_READ);
                        perms.add(PosixFilePermission.OWNER_WRITE);
                        perms.add(PosixFilePermission.OWNER_EXECUTE);
                        perms.add(PosixFilePermission.GROUP_READ);
                        perms.add(PosixFilePermission.GROUP_WRITE);
                        perms.add(PosixFilePermission.GROUP_EXECUTE);
                        perms.add(PosixFilePermission.OTHERS_READ);
                        perms.add(PosixFilePermission.OTHERS_EXECUTE);
                        //change the owner to solr
                        try {
                            //change the file permission on the solr home directory recursively
                            Files.walkFileTree(solrHomePathObj, new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                                                                throws IOException {
                                    Files.setPosixFilePermissions(file, perms);
                                    return FileVisitResult.CONTINUE;
                                }
                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                                    throws IOException {
                                    Files.setPosixFilePermissions(dir, perms);
                                    return FileVisitResult.CONTINUE;
                                }
                            });

                        } catch (Exception e) {
                            String errorString = "SolrAdmin.createSolrHome - Metacat can't "
                                                + "recursively set the user group the write "
                                                + "permission on the solr home directory -"
                                                + solrHomePath + ". Please manually do this action";
                            logMetacat.error(errorString, e);
                            throw new UnsupportedOperationException(errorString);
                        }
                    } else {
                        String errorString = "SolrAdmin.createSolrHome - the source director : "
                                             + sourceDir + " which should contain the original "
                                             + "solr configuration doesn't exist";
                        logMetacat.error(errorString);
                        throw new IOException(errorString);
                    }
                } catch (IOException ue) {    
                    String errorString = "SolrAdmin.createSolrHome - Could not initialize directory: "
                                              + solrHomePath + " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    throw new IOException(errorString);
                } catch (ServiceException e) {
                    String errorString = "SolrAdmin.createSolrHome - Could not initialize directory: "
                                            + solrHomePath + " : " + e.getMessage();
                    logMetacat.error(errorString);
                    throw new ServiceException(errorString);
                } catch (UtilException e) {
                    String errorString = "SolrAdmin.createSolrHome - Could not initialize directory: "
                                          + solrHomePath + " : " + e.getMessage();
                    logMetacat.error(errorString);
                    throw new UtilException(errorString);
                } 
            } else {
                // check it
                if (!FileUtil.isDirectory(solrHomePath)) {
                    String errorString = "SolrAdmin.createSolrHome - existing SOLR home is not a "
                                                         + "directory: " + solrHomePath;
                    logMetacat.error(errorString);
                    throw new IOException(errorString);
                } else {
                    String errorString = "SolrAdmin.createSolrHome - existing SOLR home "
                                   + solrHomePath
                                   + " already has a core and we can't create blank core in there.";
                    logMetacat.error(errorString);
                    throw new IOException(errorString);
                }
            }
    }

    /**
     * Register the given core on the solr server
     * @throws PropertyNotFoundException
     * @throws FileNotFoundException
     * @throws AdminException
     */
     private void registerSolrCore() throws AdminException,
                                                 PropertyNotFoundException, FileNotFoundException{
         String coreName = PropertyService.getProperty("solr.coreName");
         String instanceDir = PropertyService.getProperty("solr.homeDir");
         try {
             SolrClient client = SolrServerFactory.createSolrAdminClient();
             CoreAdminRequest.createCore(coreName, instanceDir, client);
         } catch (Exception e) {
             String error = "SolrAdmin.registerSolrCore - Couldn't register the solr core - "
                                             + coreName +" with the instance directory - "
                                             + instanceDir +" since "+e.getMessage();
             logMetacat.error(error, e);
              throw new AdminException(error);
         }
         //modify the default solr_home variable on the env script file
         String solrEnvScriptPath = PropertyService.getProperty("solr.env.script.path");
         modifySolrHomeInSolrEnvScript(instanceDir, solrEnvScriptPath);
     }

     /**
      * It runs the update java classes.
      * @throws AdminException
      */
     private void updateSolrSchema() throws AdminException {
         if(updateClassMap != null) {
             for (String version : updateClassMap.keySet()) {
                 String className = updateClassMap.get(version);
                 logMetacat.debug("SolrAdmin.updateSolrSchema - will run the update class "
                                     + className + " for the db version " + version);
                 UpgradeUtilityInterface utility = null;
                 try {
                     try {
                         utility = (UpgradeUtilityInterface) Class.forName(className)
                                                         .getDeclaredConstructor().newInstance();
                     } catch (IllegalAccessException ee) {
                         logMetacat.debug("SolrAdmin.updateSolrSchema - Metacat could not get a "
                                               + "instance by the constructor. It will try to the "
                                               + "method of getInstance.");
                         utility = (UpgradeUtilityInterface) Class.forName(className)
                                                     .getDeclaredMethod("getInstance").invoke(null);
                     }
                     utility.upgrade();
                     try {
                         updateSolrStatus(version, true);
                     } catch (SQLException ee) {
                         throw new AdminException("Metacat cannot update the solr_upgraded status "
                                                 + ee.getMessage());
                     }
                 } catch (SolrSchemaModificationException e) {
                     //don't throw the exception and continue
                     solrSchemaException = e;
                     try {
                         updateSolrStatus(version, true);
                     } catch (SQLException ee) {
                         throw new AdminException("Metacat cannot update the solr_upgraded status "
                                                 + ee.getMessage());
                     }
                     continue;
                 } catch (Exception e) {
                     throw new AdminException("Solr.upgradeSolrSchema - error getting utility class: "
                             + className + ". Error message: "
                             + e.getMessage());
                 }
             }
         }
     }

    /**
     * This method will set the solr home in the script file which sets the environment variables.
     * It will like: SOLR_HOME=/var/metacat/solr-home2
     * @param solrHome  the solr home path will be set
     * @param envScriptPath the file path of the script file setting the environment variables
     * @throws FileNotFoundException
     * @throws AdminException
     */
    private void modifySolrHomeInSolrEnvScript(String solrHome, String envScriptPath)
                                                    throws AdminException, FileNotFoundException {
         if(solrHome != null && !solrHome.trim().equals("")) {
             if(envScriptPath != null && !envScriptPath.isBlank()) {
                 File envScriptFile = new File(envScriptPath);
                 if(envScriptFile.exists()) {
                     if(envScriptFile.canWrite() && envScriptFile.canRead()) {
                         Scanner scanner = new Scanner(envScriptFile);
                         StringBuffer buffer = new StringBuffer();
                         while(scanner.hasNextLine()) {
                             String text = scanner.nextLine();
                             if(text.startsWith(SOLR_HOME)) {
                                 //comment out the existing solr_home
                                 buffer.append("#").append(text);
                             } else {
                                 buffer.append(text);
                             }
                             buffer.append("\n");
                         }
                         //add the solr_home line at the end
                         buffer.append(SOLR_HOME).append("=\"").append(solrHome).append("\"");
                         scanner.close();
                         //write the string buffer with the new information back to the file
                         PrintWriter printer = new PrintWriter(envScriptFile);
                         printer.print(buffer);
                         printer.close();
                     } else {
                         throw new AdminException("Tomcat user doesn't have the write/read "
                                + "permission on the solr script file setting environment "
                                + "variables at this location " + envScriptPath
                                + ".\nPlease manually modify the file by adding a line - SOLR_HOME="
                                + solrHome);
                     }
                 } else {
                     throw new AdminException("The solr script file setting environment variables "
                                       + "doesn't exist at this path " + envScriptPath
                                    + ".\nPlease find the solr.in.sh file and manually modify the "
                                    + "file by adding a line - SOLR_HOME=" + solrHome);
                 }
             } else {
                 logMetacat.error("SolrAdmin.modifySolrHomeInSolrEnvScript - the path of the solr "
                         + "script file setting environment variables shouldn't be null or blank");
             }

         } else {
             logMetacat.error("SolrAdmin.modifySolrHomeInSolrEnvScript - the solr home string "
                                         + "shouldn't be null or blank.");
         }
    }

     /**
      * Get the map of upgrade classes should be run in the upgrade. If this is a fresh
      * installation, no classes will be added to it. The key of map is the db version and the value
      * is the update class.
      * @return the map of upgrade classes. An empty map will be return if nothing is found.
      * @throws SQLException
     * @throws PropertyNotFoundException
      */
     protected static Map<String, String> getSolrUpdateClasses() throws SQLException,
                                                                        PropertyNotFoundException {
         Map<String, String> classes = new HashMap<String, String>();
         if (isFreshInstall()) {
             logMetacat.debug("SolrAdmin.getSolrUpdateClasses - this is a fresh installation "
                                 + "and no upgrade classes will be applied.");
             return classes;
         }
         Vector<DBVersion> versions = getNonUpgradedSolrVersions();
         for (DBVersion version : versions) {
             if (version != null && version.getVersionString() != null
                                     && !version.getVersionString().isBlank()) {
                 //figured out the solr update class list which will be used by SolrAdmin
                 String solrKey = "solr.upgradeUtility." + version.getVersionString();
                 String solrClassName = null;
                 try {
                     solrClassName = PropertyService.getProperty(solrKey);
                     if(solrClassName != null && !solrClassName.isBlank()) {
                         logMetacat.debug("SolrAdmin.getSolrUpdateClasses - the class "
                             + solrClassName + " was added into the upgrade class map for version "
                             + version.getVersionString());
                         classes.put(version.getVersionString(), solrClassName);
                     }
                 } catch (PropertyNotFoundException pnfe) {
                     // there probably isn't a utility needed for this version
                     logMetacat.warn("No solr update utility defined for version: " + solrKey
                                     + " since "+ pnfe.getMessage());
                 } catch (Exception e) {
                     logMetacat.warn("Can't put the solr update utility class into a vector : "
                                         + e.getMessage());
                 }
             }
         }
         return classes;
     }

     /**
      * Get a list of db versions in which the solr db hasn't been upgraded
      * @return the list of db versions in which the solr db hasn't been upgraded. An empty vector
      *  will be returned if nothing was found.
      * @throws SQLException
     * @throws PropertyNotFoundException
      */
     protected static Vector<DBVersion> getNonUpgradedSolrVersions() throws SQLException,
                                                                        PropertyNotFoundException {
         Vector<DBVersion> versions = new Vector<DBVersion>();
         DBConnection dbConn = null;
         int serialNumber = -1;
         String query = "SELECT version FROM db_version WHERE solr_upgraded IS NOT true "
                                                     + "ORDER BY db_version_id ASC";
         try {
             dbConn = DBConnectionPool.getDBConnection("SolrAdmin.getNonUpgradedSolrVersions");
             serialNumber = dbConn.getCheckOutSerialNumber();
             try (PreparedStatement pstmt = dbConn.prepareStatement(query)) {
                 pstmt.execute();
                 try (ResultSet rs = pstmt.getResultSet()) {
                     while (rs.next()) {
                         DBVersion version = new DBVersion(rs.getString(1));
                         versions.add(version);
                     }
                 }
             }
         } finally {
             if (dbConn != null) {
                 DBConnectionPool.returnDBConnection(dbConn, serialNumber);
             }
         }
         return versions;
     }

     /**
      * Check if the db is a fresh installation. If the db_version has only one row, we consider it
      * a fresh installation.
      * @return ture if it is; otherwise false.
      * @throws SQLException
      */
     protected static boolean isFreshInstall() throws SQLException {
         boolean isFreshInstall = false;
         DBConnection dbConn = null;
         int serialNumber = -1;
         int count = 0;
         String sql = "SELECT version FROM db_version";
         try {
             dbConn = DBConnectionPool.getDBConnection("SolrAdmin.isFreshInstall");
             serialNumber = dbConn.getCheckOutSerialNumber();
             try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
                 pstmt.execute();
                 try (ResultSet rs = pstmt.getResultSet()) {
                     while (rs.next()) {
                         count++;
                     }
                     if (count == 1) {
                         isFreshInstall = true;
                     }
                 }
             }
         } finally {
             if (dbConn != null) {
                 DBConnectionPool.returnDBConnection(dbConn, serialNumber);
             }
         }
         return isFreshInstall;
     }

     /**
      * Update the solr_upgraded status for the given metacat version
      * @param version  the version of Metacat will be updated in the db_version table.
      * @param status  the status will be applied.
     * @throws SQLException
      */
     protected static void updateSolrStatus(String version, boolean status) throws SQLException {
         DBConnection dbConn = null;
         int serialNumber = -1;
         String sql = "UPDATE db_version SET solr_upgraded = ? WHERE version = ?";
         try {
             dbConn = DBConnectionPool.getDBConnection("SolrAdmin.updateSolrStatus");
             serialNumber = dbConn.getCheckOutSerialNumber();
             try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
                 pstmt.setBoolean(1, status);
                 pstmt.setString(2, version);
                 pstmt.execute();
                 logMetacat.debug("SolrAdmin.updateSolrStatus - update the solr_upgrade field to "
                                 + status + " for version " + version );
             }
         } finally {
             if (dbConn != null) {
                 DBConnectionPool.returnDBConnection(dbConn, serialNumber);
             }
         }
     }
}
