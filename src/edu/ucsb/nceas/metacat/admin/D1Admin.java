package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Vector;

/**
 * Control the display of the database configuration page and the processing of the configuration
 * values.
 */
public class D1Admin extends MetacatAdmin {

    private static D1Admin instance = null;
    private final Log logMetacat = LogFactory.getLog(D1Admin.class);

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
                boolean update = isNodeRegistered(memberNodeId);
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
                throw new AdminException("D1Admin.configureDataONE - Problem getting or "
                                             + "setting property while initializing system "
                                             + "properties page: " + gpe.getMessage());
            } catch (MetacatUtilException mue) {
                throw new AdminException(
                    "D1Admin.configureDataONE - utility problem while initializing "
                        + "system properties page:" + mue.getMessage());
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
                throw new AdminException(
                    "D1Admin.configureDataONE - utility problem while processing dataone page: "
                        + mue.getMessage());
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

                    // get the current node id, so we know if we updated the value
//                    String existingMemberNodeId = PropertyService.getProperty("dataone.nodeId");

                    // update the property value
                    PropertyService.setPropertyNoPersist("dataone.nodeId", memberNodeId);

                    // persist them all
                    PropertyService.persistProperties();
                    PropertyService.syncToSettings();

                    // save a backup in case the form has errors, we reload from these
                    PropertyService.persistMainBackupProperties();

                    // Register/update as a DataONE Member Node
                    upregDataONEMemberNode();

                    // did we end up changing the member node id from what it used to be?
//                    if (!existingMemberNodeId.equals(memberNodeId)) {
//                        // update all existing system Metadata for this node id
//                        IdentifierManager.getInstance()
//                            .updateAuthoritativeMemberNodeId(existingMemberNodeId, memberNodeId);
//                    }


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

    private boolean isNodeRegistered(String nodeId) {
        // check if this is new or an update
        boolean exists = false;
        try {
            NodeList nodes = D1Client.getCN().listNodes();
            for (Node n : nodes.getNodeList()) {
                if (n.getIdentifier().getValue().equals(nodeId)) {
                    exists = true;
                    break;
                }
            }
        } catch (BaseException e) {
            logMetacat.error(
                "Could not check for node with DataONE (" + e.getCode() + "/" + e.getDetail_code()
                    + "): " + e.getDescription());
        }
        return exists;
    }

    //    private void registerDataONEMemberNode_ORIG()
//        throws BaseException, PropertyNotFoundException, GeneralPropertyException {
//
//        logMetacat.debug("Get the Node description.");
//        Node node = MNodeService.getInstance(null).getCapabilities();
//        logMetacat.debug("Setting client certificate location.");
//        String mnCertificatePath = PropertyService.getProperty("D1Client.certificate.file");
//        CertificateManager.getInstance().setCertificateLocation(mnCertificatePath);
//        CNode cn = D1Client.getCN(PropertyService.getProperty("D1Client.CN_URL"));
//
//        // check if this is new or an update
//        boolean update = isNodeRegistered(node.getIdentifier().getValue());
//
//        // Session is null, because the libclient code automatically sets up an
//        // SSL session for us using the client certificate provided
//        Session session = null;
//        if (update) {
//            logMetacat.debug("Updating node with DataONE. " + cn.getNodeBaseServiceUrl());
//            boolean result = cn.updateNodeCapabilities(session, node.getIdentifier(), node);
//        } else {
//            logMetacat.debug("Registering node with DataONE. " + cn.getNodeBaseServiceUrl());
//            NodeReference mnodeRef = cn.register(session, node);
//
//            // save that we submitted registration
//            PropertyService.setPropertyNoPersist(
//                "dataone.mn.registration.submitted", Boolean.TRUE.toString());
//
//            // persist the properties
//            PropertyService.persistProperties();
//        }
//
//    }

    /**
     * upreg: Either update ("up") or register ("reg") DataONE Member Node (MN) config, depending
     * upon whether this Metacat instance is already registered as a DataONE MN. Registration is
     * carried out only if the operator has indicated that registration is required.
     *
     * NOTE: The node description is retrieved from the getCapabilities() service, and so this
     * should only be called after the properties have been properly set up in Metacat.
     */
    private void upregDataONEMemberNode()
        throws BaseException, GeneralPropertyException, AdminException {

        logMetacat.debug("Get the Node description.");
        Node node = MNodeService.getInstance(null).getCapabilities();

        String mnCertificatePath = PropertyService.getProperty("D1Client.certificate.file");
        CertificateManager.getInstance().setCertificateLocation(mnCertificatePath);
        logMetacat.debug("DataONE MN Client certificate set: " + mnCertificatePath);

        // check if this is new or an update
        if (isNodeRegistered(node.getIdentifier().getValue())) {
            handleRegisteredUpdate(node);
        } else {

            // TODO


            // OLD CODE:
//            logMetacat.debug("Registering node with DataONE. " + cn.getNodeBaseServiceUrl());
//
//            NodeReference mnodeRef = cn.register(null, node);
//
//            // if we're not running in a container, save that we submitted registration
//            PropertyService.setPropertyNoPersist(
//                "dataone.mn.registration.submitted", Boolean.TRUE.toString());
//            // persist the properties
//            PropertyService.persistProperties();
        }
    }

    void handleRegisteredUpdate(Node node) throws AdminException {

        if (node == null) {
            throw new AdminException("handleRegisteredUpdate() received a null node!");
        }
        String nodeId = node.getIdentifier().getValue();
        String previousNodeId = getMostRecentNodeId();
        logMetacat.debug("handleRegisteredUpdate(): received new nodeId: " + nodeId
                             + ". Most recent previous nodeId was: " + previousNodeId);

        if (nodeId.equals(previousNodeId)) {
            // nodeId UNCHANGED: push an idempotent update of all other Node Capabilities to the CN
            final String END = " an update of Member Node settings to the CN (nodeId unchanged)";
            if (updateCN(node)) {
                logMetacat.info("handleRegisteredUpdate: Successfully pushed" + END);
                return;
            } else {
                String msg = "handleRegisteredUpdate: *** FAILED *** to push" + END;
                logMetacat.error(msg);
                throw new AdminException(msg);
            }
        }
        if (!canChangeNodeId()) {
            // nodeId CHANGED, but not permitted to push update to the CN without operator consent
            String msg =
                "handleRegisteredUpdate: *Not Permitted* to push update to CN without operator "
                    + "consent. (An attempt to change Property: 'dataone.nodeId' from:"
                    + previousNodeId + " to: " + nodeId + " failed, because Property: "
                    + "'dataone.autoRegisterMemberNode' had not been set to match today's date).";
            logMetacat.error(msg);
            throw new AdminException(msg);
        }
        if (!nodeIdMatchesClientCert(nodeId)) {
            // nodeId CHANGED, but does not match client cert
            String msg =
                "handleRegisteredUpdate: An attempt to change Property: 'dataone.nodeId' from:"
                    + previousNodeId + " to: " + nodeId + " failed, because the new node Id does "
                    + "not agree with the 'Subject CN' value in the client certificate";
            logMetacat.error(msg);
            throw new AdminException(msg);
        }
        // nodeId CHANGED and checks complete: try to push an update of Node Capabilities to the CN:
        logMetacat.debug("handleRegisteredUpdate(): BEGIN REGISTRATION UPDATE ATTEMPT "
                             + "(including a nodeId change from:" + previousNodeId + " to: "
                             + nodeId + ")");
        if (!updateCN(node)) {
            // ID changed, but does not match client cert
            String msg =
                "handleRegisteredUpdate(): Failed to push an update of Node Capabilities to CN "
                    + "(including a nodeId change from:" + previousNodeId + " to: " + nodeId + ")";
            logMetacat.error(msg);
            throw new AdminException(msg);
        }
        logMetacat.debug("handleRegisteredUpdate(): SUCCESS: pushed an update of Node "
                             + "Capabilities to CN. Now updating DataBase with new nodeId ("
                             + nodeId + ")...");
        int updatedRowCount = updateAuthoritativeMemberNodeId(previousNodeId, nodeId);
        logMetacat.debug("...updated authoritive_member_node (" + updatedRowCount
                             + "rows affected) in systemmetadata table...");
        saveMostRecentNodeId(nodeId);
        logMetacat.debug("...added node_id to node_id_revisions. REGISTERED UPDATE FINISHED");
    }

    /**
     * Check if the nodeId matches the "CN=" part of the client cert "Subject" field
     *
     * @return true if the nodeId matches the "CN=" part of the client cert "Subject" field
     *
     *     package-private to allow unit testing
     */
    boolean nodeIdMatchesClientCert(String nodeId) {

        X509Certificate clientCert = CertificateManager.getInstance().loadCertificate();
        String certSubject = CertificateManager.getInstance().getSubjectDN(clientCert);
        logMetacat.debug(
            "nodeIdMatchesClientCert() received nodeId: " + nodeId + ". Client cert 'Subject:' "
                + certSubject);
        if (certSubject == null || !certSubject.startsWith("CN=")) {
            logMetacat.error("nodeIdMatchesClientCert(): Client cert 'Subject:' (" + certSubject
                                + ") must begin with 'CN='. returning FALSE");
            return false;
        }
        // Subject is of the form: "CN=urn:node:TestBROOKELT,DC=dataone,DC=org", so the commonName
        // will be the part between the end of "CN="...
        final int start = 3;
        // ... and the first comma:
        int firstComma = certSubject.indexOf(",", start);
        firstComma = (firstComma < start) ? certSubject.length() : firstComma; // in case no commas
        String commonName = certSubject.substring(start, firstComma);
        boolean matches = commonName.equals(nodeId);
        logMetacat.debug("nodeIdMatchesClientCert(): " + String.valueOf(matches).toUpperCase()
                             + "! (nodeId was: " + nodeId
                             + " and Common Name (CN) from client cert was: " + commonName);
        return matches;
    }

    /**
     * Determine whether we are permitted to send a nodeId change to the CN:
     * <p></p>
     * If we're running in a container/K8s, this code runs automatically on startup, so we need
     * explicit permission via the associated values.yaml flag. Specifically, the parameter
     * <code>.Values.metacat.dataone.autoRegisterMemberNode</code> becomes the java property
     * <code>dataone.autoRegisterMemberNode</code>, and this must match today's date (in
     * yyyy-MM-dd format).
     * </p><p>
     * If we're running in a legacy deployment, we can go ahead, since this is triggered only if the
     * operator has submitted the form requesting the change.
     * </p>
     *
     * @return boolean true if it is OK to change the nodeId
     *
     *     package-private to allow unit testing
     */
    boolean canChangeNodeId() {
        boolean result;
        String autoRegDate = "";
        if (Boolean.parseBoolean(System.getenv("METACAT_IS_RUNNING_IN_A_CONTAINER"))) {
            try {
                autoRegDate = PropertyService.getProperty("dataone.autoRegisterMemberNode");
                result = LocalDate.now().equals(LocalDate.parse(autoRegDate));

            } catch (PropertyNotFoundException e) {
                logMetacat.warn("DataONE Member Node (MN) NodeId Registration/Update not possible:"
                                    + "\"dataone.autoRegisterMemberNode\" Property not set.");
                result = false;

            } catch (DateTimeParseException e) {
                logMetacat.warn("DataONE Member Node (MN) NodeId Registration/Update not possible:"
                                    + "\"dataone.autoRegisterMemberNode\" read, but can't parse "
                                    + "date: "
                                    + autoRegDate);
                result = false;
            }
        } else {
            result = true;
        }
        return result;
    }

    /**
     * @param node
     * @return package-private to allow unit testing
     */
    boolean updateCN(Node node) {
        boolean result;
        String cnUrl = "NOT SET";
        CNode cn = null;
        try {
            cn = D1Client.getCN();
            // Session is null, because libclient automatically sets up SSL session with client cert
            result = cn.updateNodeCapabilities(null, node.getIdentifier(), node);
        } catch (BaseException e) {
            logMetacat.error(
                "Calling CNode.updateNodeCapabilities() with CN URL: " + cn.getNodeBaseServiceUrl()
                    + ", and nodeId: " + node.getIdentifier().getValue());
            result = false;
        }
        return result;
    }

    /**
     * Get the most recent value of nodeId that was last recorded in the database
     *
     * @return most recent (String) value of nodeId that was last recorded in the database
     *
     *     package-private to allow unit testing
     */
    String getMostRecentNodeId() {

        DBConnection dbConn = null;
        int serialNumber = -1;
        String nodeId = "";
        try {
            dbConn = DBConnectionPool.getDBConnection("D1Admin.getMostRecentNodeId");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query = "SELECT node_id FROM node_id_revisions WHERE is_most_recent";
            PreparedStatement stmt = dbConn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                nodeId = rs.getString(1);
            }
            stmt.close();
            rs.close();
        } catch (SQLException e) {
            logMetacat.error("SQL error while getting most recent node Id: " + e.getMessage(), e);
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        if (nodeId.isEmpty()) {
            // new installation: no node_id_revisions in database yet. Init from properties file:
            try {
                nodeId = PropertyService.getProperty("dataone.nodeId");
            } catch (PropertyNotFoundException e) {
                logMetacat.error(
                    "\"dataone.nodeId\" not found in properties file: " + e.getMessage(), e);
            }
            if (!nodeId.isEmpty()) {
                logMetacat.info("no node_id_revisions found in database."
                                    + "Initializing with current nodeId from Properties: "
                                    + nodeId);
                try {
                    saveMostRecentNodeId(nodeId);
                } catch (AdminException e) {
                    logMetacat.warn("Unable to initialize node_id_revisions database table with "
                                        + "current nodeId from Properties (" + nodeId
                                        + "). Will try again next time. Error message: "
                                        + e.getMessage(), e);
                }
            }
        }
        logMetacat.debug("getMostRecentNodeId() returning: " + nodeId);
        return nodeId;
    }

    /**
     * Save the most recent value of nodeId to the database
     *
     * @param nodeId the nodeId to be saved
     * @throws AdminException           if a problem is encountered
     * @throws IllegalArgumentException if the nodeId is null or empty
     *
     *                                  package-private to allow unit testing
     */
    void saveMostRecentNodeId(String nodeId) throws AdminException {
        if (nodeId == null || nodeId.isEmpty()) {
            throw new AdminException(
                "saveMostRecentNodeId(): nodeId (= " + nodeId + ") cannot be null or empty!");
        }
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            dbConn = DBConnectionPool.getDBConnection("D1Admin.saveMostRecentNodeId");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query;
            PreparedStatement stmt;
            query = "UPDATE node_id_revisions SET is_most_recent = ?";
            stmt = dbConn.prepareStatement(query);
            stmt.setBoolean(1, false);
            stmt.execute();
            stmt.close();
            dbConn.increaseUsageCount(1);

            query = "INSERT INTO node_id_revisions (node_id, is_most_recent, date_created) "
                + "VALUES (?, true, CURRENT_DATE)";
            stmt = dbConn.prepareStatement(query);
            stmt.setString(1, nodeId);
            stmt.execute();
            stmt.close();
        } catch (SQLException e) {
            String msg = "saveMostRecentNodeId(): SQL error while trying to INSERT most-recent "
                + "nodeId value (" + nodeId + "). Error message: " + e.getMessage();
            logMetacat.error(msg);
            throw new AdminException(msg);
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    private int updateAuthoritativeMemberNodeId(
        String existingMemberNodeId, String newMemberNodeId) throws AdminException {
        DBConnection dbConn = null;
        int serialNumber = -1;
        int updatedRowCount = 0;
        PreparedStatement stmt = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("D1Admin.updateAuthoritativeMemberNodeId");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query = "UPDATE systemmetadata SET authoritive_member_node = ? "
                + " WHERE authoritive_member_node = ?";
            stmt = dbConn.prepareStatement(query);
            stmt.setString(1, newMemberNodeId);
            stmt.setString(2, existingMemberNodeId);
            updatedRowCount = stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            String msg = "updateAuthoritativeMemberNodeId(): SQL error (" + e.getMessage()
                + ") while trying to execute statement: " + stmt;
            logMetacat.error(msg);
            throw new AdminException(msg);
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return updatedRowCount;
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
