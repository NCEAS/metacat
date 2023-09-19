package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class D1AdminCNUpdater {
    private final Log logMetacat = LogFactory.getLog(D1AdminCNUpdater.class);
    private static D1AdminCNUpdater instance = null;

    /**
     * private constructor since this is a singleton
     */
    private D1AdminCNUpdater() {
    }

    /**
     * Get the single instance
     *
     * @return the single, shared instance of this class
     */
    public static D1AdminCNUpdater getInstance() {
        if (instance == null) {
            instance = new D1AdminCNUpdater();
        }
        return instance;
    }

    /**
     * Implementation of the logic for handling changes to the Member Node configuration, for a
     * node that HAS NOT YET BEEN REGISTERED as part of the DataONE network. Basic steps are:
     * <ol><li>
     * Check the operator explicitly requested that the node be registered (vs. "local" changes).
     * If so...
     * </li><li>
     * Check the provided nodeId matches the Common Name field in the client certificate. If so...
     * </li><li>
     * Try to submit a registration request to the Coordinating Node (CN). If successful...
     * </li><li>
     * Check if the nodeId used to register is different from the nodeId in the database, and if
     * so, update the database
     * </li></ol>
     *
     * @param node the <code>org.dataone.service.types.v2.Node</code> object representing this
     *             Member Node.
     * @throws AdminException if an update cannot be carried out for any reason.
     *
     * @implNote package-private to allow unit testing
     */
    void configUnregisteredMN(Node node) throws AdminException, GeneralPropertyException {

        if (node == null) {
            throw new AdminException("configUnregisteredMN() received a null node!");
        }
        String nodeId = node.getIdentifier().getValue();
        String previousNodeId = getMostRecentNodeId();
        logMetacat.debug("configUnregisteredMN(): called with nodeId: " + nodeId
                             + ". Most recent previous nodeId was: " + previousNodeId);
        if (!canChangeNodeId()) {
            // Local change only (not permitted to register with the CN without operator consent)
            logMetacat.info("configUnregisteredMN(): Only a LOCAL nodeId change will be performed, "
                                + "since operator consent to registered with the CN was not "
                                + "provided.\nIf you wish to register Metacat as a DataONE Member "
                                + "Node, you must set the Property: "
                                + "'metacat.dataone.autoRegisterMemberNode' to match "
                                + "today's date (in UTC timezone) in `values.yaml`.");
            if (!nodeId.equals(previousNodeId)) {
                logMetacat.debug("configUnregisteredMN(): updating DataBase with new nodeId ("
                                     + nodeId + ")...");
                updateDBNodeIds(previousNodeId, nodeId);
                logMetacat.debug("LOCAL-ONLY MN NODE ID UPDATE FINISHED * * *");
            }
            return;
        }
        if (!nodeIdMatchesClientCert(nodeId)) {
            // nodeId does not match client cert
            String msg =
                "configUnregisteredMN: An attempt to register this node as a DataONE Member Node "
                    + "FAILED, because the new node Id (" + nodeId + ") does not agree with the "
                    + "'Subject CN' value in the client certificate";
            logMetacat.error(msg);
            throw new AdminException(msg);
        }
        // checks complete: try to register as a new Member Node with the CN:
        logMetacat.debug(
            "configUnregisteredMN(): * * * BEGIN REGISTRATION ATTEMPT * * * - nodeId " + nodeId);
        if (!registerWithCN(node)) {
            // registration attempt unsuccessful
            String msg =
                "configUnregisteredMN(): FAILED to register as a new Member Node with the CN";
            logMetacat.error(msg);
            throw new AdminException(msg);
        }
        logMetacat.debug("configUnregisteredMN(): SUCCESS: sent registration request to CN. ");
        // If nodeId has changed, we need to update the DB
        if (!nodeId.equals(previousNodeId)) {
            logMetacat.info(
                "configUnregisteredMN() nodeId has changed from " + previousNodeId + " to "
                    + nodeId + "; Updating DataBase...");
            updateDBNodeIds(previousNodeId, nodeId);
        }
        // if we're not running in a container, save that we submitted registration
        if (!isRunningInK8s()) {
            PropertyService.setPropertyNoPersist(
                "dataone.mn.registration.submitted", Boolean.TRUE.toString());
            // persist the properties
            PropertyService.persistProperties();
        }
        logMetacat.debug("CHANGES TO PREVIOUSLY-UNREGISTERED NODE FINISHED * * *");
    }

    /**
     * Implementation of the logic for handling changes to the Member Node configuration, (including
     * the special case of a change to the nodeId) for a node that HAS ALREADY BEEN REGISTERED as
     * part of the DataONE network. Basic steps are:
     * <ol><li>
     * Check if the nodeId of the provided Node is different from the nodeId in the
     * database, that was previously used to register this MN.
     * </li><li>
     * NodeId UNCHANGED? Make an idempotent call to the Coordinating Node (CN) to update all the
     * remaining MN configuration properties, then stop processing further steps and return.
     * </li><li>
     * NodeId WAS CHANGED? Check the operator explicitly requested that the node be registered
     * (vs. "local" changes). If so...
     * </li><li>
     * Check the provided nodeId matches the Common Name field in the client certificate. If so...
     * </li><li>
     * Make an idempotent call to the Coordinating Node (CN) to update all the MN configuration,
     * properties, including the new nodeId. If successful...
     * </li><li>
     * Update the nodeId in the database
     * </li></ol>
     *
     * @param node the <code>org.dataone.service.types.v2.Node</code> object representing this
     *             Member Node.
     * @throws AdminException if an update cannot be carried out for any reason.
     * @implNote package-private to allow unit testing
     */
    void configPreregisteredMN(Node node) throws AdminException {

        if (node == null) {
            throw new AdminException("configPreregisteredMN() received a null node!");
        }
        String nodeId = node.getIdentifier().getValue();
        String previousNodeId = getMostRecentNodeId();
        logMetacat.debug("configPreregisteredMN(): received new nodeId: " + nodeId
                             + ". Most recent previous nodeId was: " + previousNodeId);

        if (nodeId.equals(previousNodeId)) {
            // nodeId UNCHANGED: push an idempotent update of all other Node Capabilities to the CN
            if (!nodeIdMatchesClientCert(nodeId)) {
                String msg = "configPreregisteredMN: Can't push an update of Node Capabilities to"
                    + " the CN, because the configured node Id doesn't agree with the "
                    + "'Subject CN' value in the client certificate";
                logMetacat.error(msg);
                throw new AdminException(msg);
            }
            final String END = " an update of Member Node settings to the CN (nodeId unchanged)";
            if (updateCN(node)) {
                logMetacat.info("configPreregisteredMN: Successfully pushed" + END);
                return;
            } else {
                String msg = "configPreregisteredMN: *** FAILED *** to push" + END;
                logMetacat.error(msg);
                throw new AdminException(msg);
            }
        }
        if (!canChangeNodeId()) {
            // nodeId CHANGED, but not permitted to push update to the CN without operator consent
            String msg =
                "configPreregisteredMN: *Not Permitted* to push update to CN without operator "
                    + "consent. (An attempt to change Property: 'dataone.nodeId' from:"
                    + previousNodeId + " to: " + nodeId + " failed, because Property: "
                    + "'dataone.autoRegisterMemberNode' had not been set to match today's "
                    + "date in UTC timezone).";
            logMetacat.error(msg);
            throw new AdminException(msg);
        }
        if (!nodeIdMatchesClientCert(nodeId)) {
            // nodeId CHANGED, but does not match client cert
            String msg =
                "configPreregisteredMN: An attempt to change Property: 'dataone.nodeId' from:"
                    + previousNodeId + " to: " + nodeId + " failed, because the new node Id does "
                    + "not agree with the 'Subject CN' value in the client certificate";
            logMetacat.error(msg);
            throw new AdminException(msg);
        }
        // nodeId CHANGED and checks complete: try to push an update of Node Capabilities to the CN:
        logMetacat.debug("configPreregisteredMN():* * * BEGIN PREREGISTERED UPDATE ATTEMPT * * *"
                             + "(including a nodeId change from:" + previousNodeId + " to: "
                             + nodeId + ")");
        if (!updateCN(node)) {
            // update attempt unsuccessful
            String msg =
                "configPreregisteredMN(): Failed to push an update of Node Capabilities to CN "
                    + "(including a nodeId change from:" + previousNodeId + " to: " + nodeId + ")";
            logMetacat.error(msg);
            throw new AdminException(msg);
        }
        logMetacat.debug("configPreregisteredMN(): SUCCESS: pushed an update of Node "
                             + "Capabilities to CN. Now updating DataBase with new nodeId ("
                             + nodeId + ")...");
        updateDBNodeIds(previousNodeId, nodeId);
        logMetacat.debug("PREREGISTERED UPDATE FINISHED * * *");
    }

    /**
     * Check if the nodeId matches the "CN=" part of the client cert "Subject" field
     *
     * @return true if the nodeId matches the "CN=" part of the client cert "Subject" field
     * @implNote package-private to allow unit testing
     */
    boolean nodeIdMatchesClientCert(String nodeId) {

        boolean matches;
        String certPath = CertificateManager.getInstance().getCertificateLocation();
        if (certPath==null || !Files.isReadable(Paths.get(certPath))) {
            logMetacat.error(
                "nodeIdMatchesClientCert(): No Client cert found at location: " + certPath);
            matches = false;
        } else {
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
            // Subject is of the form: "CN=urn:node:TestBROOKELT,DC=dataone,DC=org", so the
            // commonName
            // will be the part between the end of "CN="...
            final int start = 3;
            // ... and the first comma:
            int firstComma = certSubject.indexOf(",", start);
            firstComma =
                (firstComma < start) ? certSubject.length() : firstComma; // in case no commas
            String commonName = certSubject.substring(start, firstComma);
            matches = commonName.equals(nodeId);
            String msg = "nodeIdMatchesClientCert(): " + String.valueOf(matches).toUpperCase()
                + "! (nodeId: " + nodeId + "; Common Name (CN) from client cert: " + commonName;
            if (matches) {
                logMetacat.info(msg);
            } else {
                logMetacat.error(msg);
            }
        }
        return matches;
    }

    /**
     * Determine whether we are permitted to send a nodeId change to the CN:
     * <p></p>
     * If we're running in a container/K8s, this code runs automatically on startup, so we need
     * explicit permission via the associated values.yaml flag. Specifically, the parameter
     * <code>.Values.metacat.dataone.autoRegisterMemberNode</code> must match today's date (in
     * yyyy-MM-dd format, in UTC timezone).
     * </p><p>
     * If we're running in a legacy deployment, we can go ahead, since the operator has already
     * given explicit consent by submitting the form in the Metacat Admin UI requesting the change.
     * </p>
     *
     * @return boolean true if it is OK to change the nodeId
     * @implNote package-private to allow unit testing
     */
    boolean canChangeNodeId() {
        boolean result;
        String autoRegDate = "";
        if (isRunningInK8s()) {
            logMetacat.debug("canChangeNodeId(): Containerized/Kubernetes deployment detected");
            ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String todaysDateUTC = utc.format(formatter);
            try {
                autoRegDate = PropertyService.getProperty("dataone.autoRegisterMemberNode");
                result = todaysDateUTC.equals(autoRegDate);
                logMetacat.debug("canChangeNodeId(): returning " + result
                                     + ", since '.Values.metacat.dataone.autoRegisterMemberNode'="
                                     + autoRegDate + ", and today's date in UTC timezone is: "
                                     + todaysDateUTC);
            } catch (PropertyNotFoundException e) {
                logMetacat.warn("canChangeNodeId(): DataONE Member Node (MN) NodeId "
                                    + "Registration/Update not possible:"
                                    + "'.Values.metacat.dataone.autoRegisterMemberNode' not set.");
                result = false;

            } catch (DateTimeParseException e) {
                logMetacat.warn("canChangeNodeId(): DataONE Member Node (MN) NodeId "
                                    + "Registration/Update not possible:"
                                    + "'.Values.metacat.dataone.autoRegisterMemberNode' read, but"
                                    + " can't parse date: " + autoRegDate);
                result = false;
            }
        } else { //legacy deployment: explicit consent already given by submitting the form
            logMetacat.debug("canChangeNodeId(): Legacy (non-containerized) deployment detected. "
                                 + "Returning TRUE, since explicit consent already given by "
                                 + "submitting the form ");
            result = true;
        }
        return result;
    }

    /**
     * Send this MN's registration request to the configured Coordinating Node (CN). (Note this does
     * not complete registration; the final step has to be carried out manually by a DataONE
     * admin.)
     *
     * @param mNode the Member Node to be registered
     * @return boolean <code>true</code> upon the CN receiving a successful registration request, or
     *     <code>false</code> otherwise
     * @implNote package-private to allow unit testing
     * @see "https://dataoneorg.github.io/api-documentation/apis/CN_APIs.html#CNRegister.register"
     */
    boolean registerWithCN(Node mNode) {

        boolean result;
        String nodeId = mNode.getIdentifier().getValue();
        final CNode cn;
        String CnUrl = "CNode is NULL";
        try {
            cn = D1Client.getCN();
            if (cn != null) {
                CnUrl = cn.getNodeBaseServiceUrl();
            }
            logMetacat.info("Registering node with DataONE CN: " + CnUrl);

            // Session is null, because libclient automatically sets up SSL session with client cert
            NodeReference mnRef = cn != null ? cn.register(null, mNode) : null;

            String returnedNodeId = (mnRef != null) ? mnRef.getValue() : null;
            result = (returnedNodeId != null && returnedNodeId.equals(nodeId));
            if (!result) {
                logMetacat.error("CNode.register() returned a node ID (" + returnedNodeId
                                     + ") not matching the nodeId that was sent (" + nodeId + ")");
            }
        } catch (IdentifierNotUnique e) {
            logMetacat.error("Attempt to register a Member Node with an ID that is already in use "
                                 + "by a different registered node (" + nodeId + "). CN URL is: "
                                 + CnUrl + "; error message was: " + e.getMessage(), e);
            result = false;

        } catch (BaseException e) {
            logMetacat.error(
                "Calling CNode.register() with CN URL: " + CnUrl + ", and nodeId: " + nodeId
                    + "; error message was: " + e.getMessage(), e);
            result = false;
        }
        return result;
    }

    /**
     * Send this MN's config details to the configured Coordinating Node (CN).
     *
     *  NOTE: According to: <a href=
     *  "https://dataoneorg.github.io/api-documentation/apis/CN_APIs.html#CNRegister.updateNodeCapabilities"
     *          >updateNodeCapabilities() API docs</a>
     *  "For updating the capabilities of the specified node. Most information is replaced
     *  by information in the new node, however, the node identifier, nodeType, ping,
     *  synchronization.lastHarvested, and synchronization.lastCompleteHarvest are
     *  preserved from the existing entry. Services in the old record not included in the
     *  new Node will be removed."
     *
     * @param mNode the Member Node whose config details will be sent to the CN
     * @return <code>true</code> if CN was successfully updated; <code>false</code> otherwise
     * @implNote package-private to allow unit testing
     * @see "https://dataoneorg.github.io/api-documentation/apis/CN_APIs.html"
     *      "#CNRegister.updateNodeCapabilities"
     */
    boolean updateCN(Node mNode) {
        boolean result;
        final CNode cn;
        String CnUrl = "CNode is NULL";
        try {
            cn = D1Client.getCN();
            if (cn != null) {
                CnUrl = cn.getNodeBaseServiceUrl();
            }
            logMetacat.info("Sending updated node capabilities to DataONE CN: " + CnUrl);

            // Session is null, because libclient automatically sets up SSL session with client cert
            result = cn != null && cn.updateNodeCapabilities(null, mNode.getIdentifier(), mNode);

        } catch (BaseException e) {
            final String nodeId =
                (mNode.getIdentifier() != null) ? mNode.getIdentifier().getValue() : "NULL!";
            logMetacat.error(
                "Calling CNode.updateNodeCapabilities() with CN URL: " + CnUrl + ", and nodeId: "
                    + nodeId + "; error message was: " + e.getMessage(), e);
            result = false;
        }
        return result;
    }

    /**
     * Get the most recent value of nodeId that was last recorded in the database
     *
     * @return most recent (String) value of nodeId that was last recorded in the database
     * @implNote package-private to allow unit testing
     */
    String getMostRecentNodeId() throws AdminException {

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
            final String msg = "SQL error while getting most recent node Id: " + e.getMessage();
            logMetacat.error(msg, e);
            AdminException ae = new AdminException(msg);
            ae.initCause(e);
            ae.fillInStackTrace();
            throw ae;
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
                    saveNewNodeIdRevision(nodeId);
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
     * Check to see if this node has already been registered with the Coordinating Node (CN)
     *
     * @param nodeId This node's ID
     * @return true if already registered; false otherwise
     * @implNote package-private to allow unit testing
     */
    boolean isNodeRegistered(String nodeId) {
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

    /**
     * Utility method that makes calls to <code>updateAuthoritativeMemberNodeId()</code> and
     * <code>saveNewNodeIdRevision()</code> and provides logging
     *
     * @param existingMemberNodeId the previous member node id
     * @param newMemberNodeId      the new member node id
     * @throws AdminException if a problem is encountered
     * @implNote package-private to allow unit testing
     */
    void updateDBNodeIds(String existingMemberNodeId, String newMemberNodeId) throws AdminException {
        logMetacat.debug(
            "Updating DataBase with new nodeId " + newMemberNodeId + "(from previous nodeId:"
                + existingMemberNodeId + ")...");
        int updatedRowCount = updateAuthoritativeMemberNodeId(existingMemberNodeId, newMemberNodeId);
        logMetacat.debug(
            "...updated 'authoritive_member_node' in 'systemmetadata' table (" + updatedRowCount
                + " rows affected)...");
        saveNewNodeIdRevision(newMemberNodeId);
        logMetacat.debug(
            "...added new node_id to 'node_id_revisions' table, and marked as 'is_most_recent'.");
    }

    /**
     * Update the 'systemmetadata' DB table by replacing all the 'AuthoritativeMemberNode' entries
     * that were set to the existingMemberNodeId, with the newMemberNodeId
     *
     * @param existingMemberNodeId the previous member node id
     * @param newMemberNodeId      the new member node id
     * @throws AdminException if a problem is encountered
     * @implNote package-private to allow unit testing
     */
    int updateAuthoritativeMemberNodeId(
        String existingMemberNodeId, String newMemberNodeId) throws AdminException {
        DBConnection dbConn = null;
        int serialNumber = -1;
        int updatedRowCount;
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
            logMetacat.error(msg, e);
            AdminException ae = new AdminException(msg);
            ae.initCause(e);
            ae.fillInStackTrace();
            throw ae;
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return updatedRowCount;
    }

    /**
     * Save the most recent value of nodeId to the database
     *
     * @param nodeId the nodeId to be saved
     * @throws AdminException if a problem is encountered
     * @implNote package-private to allow unit testing
     */
    void saveNewNodeIdRevision(String nodeId) throws AdminException {
        if (nodeId == null || nodeId.isEmpty()) {
            throw new AdminException(
                "saveNewNodeIdRevision(): nodeId (= " + nodeId + ") cannot be null or empty!");
        }
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            dbConn = DBConnectionPool.getDBConnection("D1Admin.saveNewNodeIdRevision");
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
            String msg = "saveNewNodeIdRevision(): SQL error while trying to INSERT most-recent "
                + "nodeId value (" + nodeId + "). Error message: " + e.getMessage();
            logMetacat.error(msg, e);
            AdminException ae = new AdminException(msg);
            ae.initCause(e);
            ae.fillInStackTrace();
            throw ae;
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Determine whether metacat is running in a containerized/kubernetes environment, by
     * checking for the environment variable `METACAT_IN_K8S` being set to `true`
     *
     * @return true if metacat is running in a containerized/kubernetes environment; false otherwise
     */
    private static boolean isRunningInK8s() {
        return Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"));
    }
}
