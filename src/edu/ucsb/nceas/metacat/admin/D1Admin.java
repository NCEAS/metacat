package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.SortedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v2.Node;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Vector;

/**
 * Control the display of the database configuration page and the processing of the configuration
 * values.
 */
public class D1Admin extends MetacatAdmin {

    private static D1Admin instance = null;
    private final Log logMetacat = LogFactory.getLog(D1Admin.class);
    private final D1AdminCNUpdater d1AdminCNUpdater = D1AdminCNUpdater.getInstance();

    /**
     * private constructor since this is a singleton
     */
    private D1Admin() {
    }

    /**
     * Get the single instance of D1Admin.
     *
     * @return the single instance of D1Admin
     */
    public static D1Admin getInstance() throws AdminException {
        if (instance == null) {
            instance = new D1Admin();
        }
        return instance;
    }

    /**
     * Handle configuration of the database the first time that Metacat starts or when it is
     * explicitly called. Collect necessary update information from the administrator.
     *
     * @param request  the http request information
     * @param response the http response to be sent back to the client
     */
    public void configureDataONE(HttpServletRequest request, HttpServletResponse response)
        throws AdminException {

        String processForm = request.getParameter("processForm");
        String bypass = request.getParameter("bypass");
        String formErrors = (String) request.getAttribute("formErrors");

        if (processForm == null || !processForm.equals("true") || formErrors != null) {
            // The servlet configuration parameters have not been set, or there
            // were form errors on the last attempt to configure, so redirect to
            // the web form for configuring metacat

            try {

                // get the current configuration values
                String cnURL = PropertyService.getProperty("D1Client.CN_URL");
                String nodeName = PropertyService.getProperty("dataone.nodeName");
                String nodeDescription = PropertyService.getProperty("dataone.nodeDescription");
                String memberNodeId = PropertyService.getProperty("dataone.nodeId");
                String nodeSynchronize = PropertyService.getProperty("dataone.nodeSynchronize");
                String subject = PropertyService.getProperty("dataone.subject");
                String contactSubject = PropertyService.getProperty("dataone.contactSubject");
                String certpath = PropertyService.getProperty("D1Client.certificate.file");

                //the sync schedule
                String year =
                    PropertyService.getProperty("dataone.nodeSynchronization.schedule.year");
                String mon =
                    PropertyService.getProperty("dataone.nodeSynchronization.schedule.mon");
                String mday =
                    PropertyService.getProperty("dataone.nodeSynchronization.schedule.mday");
                String wday =
                    PropertyService.getProperty("dataone.nodeSynchronization.schedule.wday");
                String hour =
                    PropertyService.getProperty("dataone.nodeSynchronization.schedule.hour");
                String min =
                    PropertyService.getProperty("dataone.nodeSynchronization.schedule.min");
                String sec =
                    PropertyService.getProperty("dataone.nodeSynchronization.schedule.sec");

                // the replication policies
                String nodeReplicate = PropertyService.getProperty("dataone.nodeReplicate");
                String numReplicas =
                    PropertyService.getProperty("dataone.replicationpolicy.default.numreplicas");
                String preferredNodeList = PropertyService.getProperty(
                    "dataone.replicationpolicy.default.preferredNodeList");
                String blockedNodeList = PropertyService.getProperty(
                    "dataone.replicationpolicy.default.blockedNodeList");

                boolean synchronize = false;
                if (nodeSynchronize != null) {
                    synchronize = Boolean.parseBoolean(nodeSynchronize);
                }
                boolean replicate = false;
                if (nodeReplicate != null) {
                    replicate = Boolean.parseBoolean(nodeReplicate);
                }
                request.setAttribute("D1Client.CN_URL", cnURL);
                request.setAttribute("dataone.nodeName", nodeName);
                request.setAttribute("dataone.nodeDescription", nodeDescription);
                request.setAttribute("dataone.nodeId", memberNodeId);
                request.setAttribute("dataone.nodeSynchronize", Boolean.toString(synchronize));
                request.setAttribute("dataone.subject", subject);
                request.setAttribute("dataone.contactSubject", contactSubject);
                request.setAttribute("D1Client.certificate.file", certpath);

                // sync schedule
                request.setAttribute("dataone.nodeSynchronization.schedule.year", year);
                request.setAttribute("dataone.nodeSynchronization.schedule.mon", mon);
                request.setAttribute("dataone.nodeSynchronization.schedule.mday", mday);
                request.setAttribute("dataone.nodeSynchronization.schedule.wday", wday);
                request.setAttribute("dataone.nodeSynchronization.schedule.hour", hour);
                request.setAttribute("dataone.nodeSynchronization.schedule.min", min);
                request.setAttribute("dataone.nodeSynchronization.schedule.sec", sec);

                // replication policies
                request.setAttribute("dataone.nodeReplicate", Boolean.toString(replicate));
                request.setAttribute("dataone.replicationpolicy.default.numreplicas", numReplicas);
                request.setAttribute(
                    "dataone.replicationpolicy.default.preferredNodeList", preferredNodeList);
                request.setAttribute(
                    "dataone.replicationpolicy.default.blockedNodeList", blockedNodeList);

                // support email for error messaging
                request.setAttribute(
                    "supportEmail", PropertyService.getProperty("email.recipient"));

                // try the backup properties
                SortedProperties backupProperties = null;
                if ((backupProperties = PropertyService.getMainBackupProperties()) != null) {
                    Vector<String> backupKeys = backupProperties.getPropertyNames();
                    for (String key : backupKeys) {
                        String value = backupProperties.getProperty(key);
                        if (value != null) {
                            request.setAttribute(key, value);
                        }
                    }
                }

                // set the configuration state, so we know how to render the UI page buttons
                // if we have already configured once, we cannot skip this page
                request.setAttribute("configutil.dataoneConfigured",
                                     PropertyService.getProperty("configutil.dataoneConfigured"));

                // do we know if this is an update, pending verification, or a new registration?
                memberNodeId = (String) request.getAttribute("dataone.nodeId");
                boolean update = d1AdminCNUpdater.isNodeRegistered(memberNodeId);
                request.setAttribute("dataone.isUpdate", Boolean.toString(update));
                request.setAttribute(
                    "dataone.mn.registration.submitted", PropertyService.getProperty(
                        "dataone.mn.registration.submitted"));

                // enable the services?
                request.setAttribute("dataone.mn.services.enabled",
                                     PropertyService.getProperty("dataone.mn.services.enabled"));

                // Forward the request to the JSP page
                RequestUtil.forwardRequest(
                    request, response, "/admin/dataone-configuration.jsp", null);
            } catch (GeneralPropertyException gpe) {
                final String msg = "D1Admin.configureDataONE - Problem getting or "
                                             + "setting property while initializing system "
                                             + "properties page: " + gpe.getMessage();
                AdminException ae = new AdminException(msg);
                ae.initCause(gpe);
                ae.fillInStackTrace();
                throw ae;
            } catch (MetacatUtilException mue) {
                final String msg = "D1Admin.configureDataONE - utility problem while initializing "
                        + "system properties page:" + mue.getMessage();
                AdminException ae = new AdminException(msg);
                ae.initCause(mue);
                ae.fillInStackTrace();
                throw ae;
            } catch (ServiceFailure | NotImplemented e) {
                final String msg = "D1Admin.configureDataONE - problem determining whether node "
                    + "already registered:" + e.getMessage();
                AdminException ae = new AdminException(msg);
                ae.initCause(e);
                ae.fillInStackTrace();
                throw ae;
            }
        } else if (bypass != null && bypass.equals("true")) {
            Vector<String> processingErrors = new Vector<String>();
            Vector<String> processingSuccess = new Vector<String>();

            // Bypass the D1 configuration.
            // This will keep Metacat running
            try {
                PropertyService.setProperty(
                    "configutil.dataoneConfigured", PropertyService.BYPASSED);

            } catch (GeneralPropertyException gpe) {
                String errorMessage =
                    "D1Admin.configureDataONE - Problem getting or setting property while "
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
                    processingSuccess.add("DataONE configuration successfully bypassed");
                    RequestUtil.clearRequestMessages(request);
                    RequestUtil.setRequestSuccess(request, processingSuccess);
                    RequestUtil.forwardRequest(request, response,
                                               "/admin?configureType=configure&processForm=false",
                                               null);
                }
            } catch (MetacatUtilException mue) {
                final String msg = "D1Admin.configureDataONE - utility problem while redirecting "
                    + "to another page:" + mue.getMessage();
                AdminException ae = new AdminException(msg);
                ae.initCause(mue);
                ae.fillInStackTrace();
                throw ae;
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

                String cnURL = (String) request.getParameter("D1Client.CN_URL");
                String nodeName = (String) request.getParameter("dataone.nodeName");
                String nodeDescription = (String) request.getParameter("dataone.nodeDescription");
                String memberNodeId = (String) request.getParameter("dataone.nodeId");
                String nodeSynchronize = (String) request.getParameter("dataone.nodeSynchronize");
                String subject = (String) request.getParameter("dataone.subject");
                String contactSubject = (String) request.getParameter("dataone.contactSubject");
                String certpath = (String) request.getParameter("D1Client.certificate.file");

                // the sync schedule
                String year =
                    (String) request.getParameter("dataone.nodeSynchronization.schedule.year");
                String mon =
                    (String) request.getParameter("dataone.nodeSynchronization.schedule.mon");
                String mday =
                    (String) request.getParameter("dataone.nodeSynchronization.schedule.mday");
                String wday =
                    (String) request.getParameter("dataone.nodeSynchronization.schedule.wday");
                String hour =
                    (String) request.getParameter("dataone.nodeSynchronization.schedule.hour");
                String min =
                    (String) request.getParameter("dataone.nodeSynchronization.schedule.min");
                String sec =
                    (String) request.getParameter("dataone.nodeSynchronization.schedule.sec");

                // the replication policies
                String nodeReplicate = (String) request.getParameter("dataone.nodeReplicate");
                String numReplicas =
                    (String) request.getParameter("dataone.replicationpolicy.default.numreplicas");
                String preferredNodeList = (String) request.getParameter(
                    "dataone.replicationpolicy.default.preferredNodeList");
                String blockedNodeList = (String) request.getParameter(
                    "dataone.replicationpolicy.default.blockedNodeList");

                boolean synchronize = false;
                if (nodeSynchronize != null) {
                    synchronize = Boolean.parseBoolean(nodeSynchronize);
                }
                boolean replicate = false;
                if (nodeReplicate != null) {
                    replicate = Boolean.parseBoolean(nodeReplicate);
                }

                // enable services as a whole?
                boolean servicesEnabled = false;
                String servicesEnabledString =
                    (String) request.getParameter("dataone.mn.services.enabled");
                if (servicesEnabledString != null) {
                    servicesEnabled = Boolean.parseBoolean(servicesEnabledString);
                }

                // process the values, checking for nulls etc.
                if (nodeName == null) {
                    validationErrors.add("nodeName cannot be null");
                } else {

                    PropertyService.setProperty("D1Client.CN_URL", cnURL);
                    Settings.getConfiguration().setProperty("D1Client.CN_URL", cnURL);
                    PropertyService.setPropertyNoPersist("dataone.nodeName", nodeName);
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeDescription", nodeDescription);
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeSynchronize", Boolean.toString(synchronize));
                    PropertyService.setPropertyNoPersist("dataone.subject", subject);
                    PropertyService.setPropertyNoPersist("dataone.contactSubject", contactSubject);
                    PropertyService.setPropertyNoPersist("D1Client.certificate.file", certpath);

                    // the sync schedule
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeSynchronization.schedule.year", year);
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeSynchronization.schedule.mon", mon);
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeSynchronization.schedule.mday", mday);
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeSynchronization.schedule.wday", wday);
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeSynchronization.schedule.hour", hour);
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeSynchronization.schedule.min", min);
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeSynchronization.schedule.sec", sec);

                    // the replication policies
                    PropertyService.setPropertyNoPersist(
                        "dataone.nodeReplicate", Boolean.toString(replicate));
                    PropertyService.setPropertyNoPersist(
                        "dataone.replicationpolicy.default.numreplicas", numReplicas);
                    PropertyService.setPropertyNoPersist(
                        "dataone.replicationpolicy.default.preferredNodeList", preferredNodeList);
                    PropertyService.setPropertyNoPersist(
                        "dataone.replicationpolicy.default.blockedNodeList", blockedNodeList);

                    // services
                    PropertyService.setPropertyNoPersist(
                        "dataone.mn.services.enabled", Boolean.toString(servicesEnabled));

                    // update the property value
                    PropertyService.setPropertyNoPersist("dataone.nodeId", memberNodeId);

                    // persist them all
                    PropertyService.persistProperties();
                    PropertyService.syncToSettings();

                    // save a backup in case the form has errors, we reload from these
                    PropertyService.persistMainBackupProperties();

                    // Register/update as a DataONE Member Node
                    upRegD1MemberNode();

                    // dataone system metadata generation:
                    // we can generate this after the registration/configuration
                    // it will be more controlled and deliberate that way -BRL

                    // write the backup properties to a location outside the
                    // application directories, so they will be available after
                    // the next upgrade
                    PropertyService.persistMainBackupProperties();
                }
            } catch (GeneralPropertyException gpe) {
                String errorMessage =
                    "D1Admin.configureDataONE - Problem getting or setting property while "
                        + "processing system properties page: " + gpe.getMessage();
                logMetacat.error(errorMessage);
                processingErrors.add(errorMessage);
            } catch (Exception e) {
                String errorMessage = "D1Admin.configureDataONE error: " + e.getMessage();
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
                    // 'dataoneConfigured' option to 'true'
                    PropertyService.setProperty("configutil.dataoneConfigured",
                                                PropertyService.CONFIGURED);

                    // Reload the main metacat configuration page
                    processingSuccess.add("DataONE successfully configured");
                    RequestUtil.clearRequestMessages(request);
                    RequestUtil.setRequestSuccess(request, processingSuccess);
                    RequestUtil.forwardRequest(request, response,
                                               "/admin?configureType=configure&processForm=false",
                                               null);
                }
            } catch (MetacatUtilException mue) {
                throw new AdminException(
                    "D1Admin.configureDataONE - utility problem while processing dataone "
                        + "configuration: " + mue.getMessage());
            } catch (GeneralPropertyException gpe) {
                throw new AdminException("D1Admin.configureDataONE - problem with properties while "
                                             + "processing geoservices configuration page: "
                                             + gpe.getMessage());
            }
        }
    }

    /**
     * upReg: Either update ("up") or register ("reg") DataONE Member Node (MN) config, depending
     * upon whether this Metacat instance is already registered as a DataONE MN. Registration is
     * carried out only if the operator has indicated that registration is required.
     *
     * NOTE: The node description is retrieved from the getCapabilities() service, and so this
     * should only be called after the properties have been properly set up in Metacat.
     */
    public void upRegD1MemberNode() throws GeneralPropertyException, AdminException {

        logMetacat.debug("Get the Node description.");
        Node node = null;
        try {
            node = MNodeService.getInstance(null).getCapabilities();
        } catch (ServiceFailure e) {
            String msg
                = "upRegD1MemberNode(): ServiceFailure calling MNodeService::getCapabilities: "
                + e.getMessage();
            logMetacat.error(msg, e);
            AdminException ae = new AdminException(msg);
            ae.initCause(e);
            ae.fillInStackTrace();
            throw ae;
        }
        String mnCertificatePath = PropertyService.getProperty("D1Client.certificate.file");
        CertificateManager.getInstance().setCertificateLocation(mnCertificatePath);
        logMetacat.debug("DataONE MN Client certificate set: " + mnCertificatePath);

        // check if this is new or an update
        boolean isNodeRegistered;
        try {
            isNodeRegistered = d1AdminCNUpdater.isNodeRegistered(node.getIdentifier().getValue());
        } catch (BaseException e) {
            String msg = "upRegD1MemberNode(): Exception checking if node is registered: "
                + e.getMessage();
            logMetacat.error(msg, e);
            AdminException ae = new AdminException(msg);
            ae.initCause(e);
            ae.fillInStackTrace();
            throw ae;
        }
        if (isNodeRegistered) {
            logMetacat.info("* * * Handling config changes: PREREGISTERED D1 MEMBER NODE...");
            d1AdminCNUpdater.configPreregisteredMN(node);
        } else {
            logMetacat.info("* * * Handling config changes: UNREGISTERED D1 MEMBER NODE...");
            d1AdminCNUpdater.configUnregisteredMN(node);
        }
    }

    /**
     * Validate the most important configuration options submitted by the user.
     *
     * @return a vector holding error message for any fields that fail validation.
     */
    @Override
    protected Vector<String> validateOptions(HttpServletRequest request) {
        Vector<String> errorVector = new Vector<String>();

        // TODO MCD validate options.

        return errorVector;
    }

}
