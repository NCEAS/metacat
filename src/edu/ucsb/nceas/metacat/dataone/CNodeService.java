package edu.ucsb.nceas.metacat.dataone;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.cn.v2.CNAuthorization;
import org.dataone.service.cn.v2.CNCore;
import org.dataone.service.cn.v2.CNRead;
import org.dataone.service.cn.v2.CNReplication;
import org.dataone.service.cn.v2.CNView;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * Represents Metacat's implementation of the DataONE Coordinating Node service API. Methods
 * implement the various CN* interfaces, and methods common to both Member Node and Coordinating
 * Node interfaces are found in the D1NodeService super class.
 */
public class CNodeService extends D1NodeService
    implements CNAuthorization, CNCore, CNRead, CNReplication, CNView {

    /* the logger instance */
    private Log logMetacat = LogFactory.getLog(CNodeService.class);
    public final static String V2V1MISSMATCH =
        "The Coordinating Node is not authorized to make systemMetadata changes on this object. "
            + "Please make changes directly on the authoritative Member Node.";

    /**
     * singleton accessor
     */
    public static CNodeService getInstance(HttpServletRequest request) {
        return new CNodeService(request);
    }

    /**
     * Constructor, private for singleton access
     */
    private CNodeService(HttpServletRequest request) {
        super(request);
    }

    /**
     * Set the replication policy for an object given the object identifier It only is applied to
     * objects whose authoritative mn is a v1 node.
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - the object identifier for the given object
     * @param policy  - the replication policy to be applied
     * @return true or false
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws VersionMismatch
     */
    @Override
    public boolean setReplicationPolicy(
        Session session, Identifier pid, ReplicationPolicy policy, long serialVersion)
        throws NotImplemented, NotFound, NotAuthorized, ServiceFailure, InvalidRequest,
        InvalidToken, VersionMismatch {

        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new InvalidRequest("4883", "The provided identifier was invalid.");

        }

        //only allow pid to be passed
        String serviceFailure = "4882";
        String notFound = "4884";
        checkV1SystemMetaPidExist(pid, serviceFailure, "The object for given PID " + pid.getValue()
                                      + " couldn't be identified if it exists", notFound,
                                  "No object could be found for given PID: " + pid.getValue());

        // get the subject
        Subject subject = session.getSubject();

        // are we allowed to do this?
        if (!isAuthorized(session, pid, Permission.CHANGE_PERMISSION)) {
            throw new NotAuthorized(
                "4881", Permission.CHANGE_PERMISSION + " not allowed by " + subject.getValue()
                + " on " + pid.getValue());

        }

        SystemMetadata systemMetadata = null;
        try {
            try {
                if (IdentifierManager.getInstance().systemMetadataPIDExists(pid)) {
                    systemMetadata = SystemMetadataManager.getInstance().get(pid);

                }

                // did we get it correctly?
                if (systemMetadata == null) {
                    throw new NotFound(
                        "4884", "Couldn't find an object identified by " + pid.getValue());

                }
                D1NodeVersionChecker checker =
                    new D1NodeVersionChecker(systemMetadata.getAuthoritativeMemberNode());
                String version = checker.getVersion("MNRead");
                if (version == null) {
                    throw new ServiceFailure(
                        "4882",
                        "Couldn't determine the MNRead version of the authoritative member node "
                            + "for the pid "
                            + pid.getValue());
                } else if (version.equalsIgnoreCase(D1NodeVersionChecker.V2)) {
                    //we don't apply this method to an object whose authoritative node is v2
                    throw new NotAuthorized("4881", V2V1MISSMATCH);
                } else if (!version.equalsIgnoreCase(D1NodeVersionChecker.V1)) {
                    //we don't understand this version (it is not v1 or v2)
                    throw new InvalidRequest(
                        "4883", "The version of the MNRead is " + version
                        + " for the authoritative member node of the object " + pid.getValue()
                        + ". We don't support it.");
                }
                // does the request have the most current system metadata?
                if (systemMetadata.getSerialVersion().longValue() != serialVersion) {
                    String msg = "The requested system metadata version number " + serialVersion
                        + " differs from the current version at " + systemMetadata
                        .getSerialVersion().longValue()
                        + ". Please get the latest copy in order to modify it.";
                    throw new VersionMismatch("4886", msg);

                }

            } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
                throw new NotFound("4884", "No record found for: " + pid.getValue());
            } catch (SQLException e) {
                throw new ServiceFailure("4882", e.getMessage());
            }

            // set the new policy
            systemMetadata.setReplicationPolicy(policy);

            // update the metadata
            try {
                systemMetadata.setSerialVersion(
                    systemMetadata.getSerialVersion().add(BigInteger.ONE));
                SystemMetadataManager.getInstance().store(systemMetadata);
                notifyReplicaNodes(systemMetadata);

            } catch (RuntimeException e) {
                throw new ServiceFailure("4882", e.getMessage());

            }

        } catch (RuntimeException e) {
            throw new ServiceFailure("4882", e.getMessage());

        }

        return true;
    }

    /**
     * Deletes the replica from the given Member Node NOTE: MN.delete() may be an "archive"
     * operation. TBD.
     *
     * @param session
     * @param pid
     * @param nodeId
     * @param serialVersion
     * @return
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws VersionMismatch
     */
    @Override
    public boolean deleteReplicationMetadata(
        Session session, Identifier pid, NodeReference nodeId, long serialVersion)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented,
        VersionMismatch {

        // The lock to be used for this identifier
        //Lock lock = null;

        if (session == null) {
            throw new NotAuthorized(
                "4882",
                "Session cannot be null. It is not authorized for deleting the replication "
                    + "metadata of the object "
                    + pid.getValue());
        }

        D1AuthHelper authDel = new D1AuthHelper(request, pid, "4882", "4884");
        authDel.doCNOnlyAuthorization(session);

        SystemMetadata systemMetadata = null;
        try {
            try {
                if (IdentifierManager.getInstance().systemMetadataPIDExists(pid)) {
                    systemMetadata = SystemMetadataManager.getInstance().get(pid);
                }

                // did we get it correctly?
                if (systemMetadata == null) {
                    throw new NotFound(
                        "4884", "Couldn't find an object identified by " + pid.getValue());
                }

                // does the request have the most current system metadata?
                if (systemMetadata.getSerialVersion().longValue() != serialVersion) {
                    String msg = "The requested system metadata version number " + serialVersion
                        + " differs from the current version at " + systemMetadata
                        .getSerialVersion().longValue()
                        + ". Please get the latest copy in order to modify it.";
                    throw new VersionMismatch("4886", msg);
                }
            } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
                throw new NotFound("4884", "No record found for: " + pid.getValue());
            } catch (SQLException e) {
                throw new ServiceFailure("4882", e.getMessage());
            }

            // reflect that change in the system metadata
            List<Replica> updatedReplicas = new ArrayList<Replica>(systemMetadata.getReplicaList());
            for (Replica r : systemMetadata.getReplicaList()) {
                if (r.getReplicaMemberNode().equals(nodeId)) {
                    updatedReplicas.remove(r);
                    break;
                }
            }
            systemMetadata.setReplicaList(updatedReplicas);

            // update the metadata
            try {
                systemMetadata.setSerialVersion(
                    systemMetadata.getSerialVersion().add(BigInteger.ONE));
                //we don't need to update the modification date.
                boolean changeModificationDate = false;
                SystemMetadataManager.getInstance().store(systemMetadata, changeModificationDate);
            } catch (RuntimeException e) {
                throw new ServiceFailure("4882", e.getMessage());
            } catch (InvalidRequest e) {
                throw new InvalidToken("4882", e.getMessage());
            }

        } catch (RuntimeException e) {
            throw new ServiceFailure("4882", e.getMessage());
        }

        return true;

    }

    /**
     * Deletes an object from the Coordinating Node
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - The object identifier to be deleted
     * @return pid - the identifier of the object used for the deletion
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     */
    @Override
    public Identifier delete(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        String localId = null;      // The corresponding docid for this pid
        //Lock lock = null;           // The lock to be used for this identifier
        CNode cn = null;            // a reference to the CN to get the node list
        NodeType nodeType = null;   // the nodeType of the replica node being contacted
        List<Node> nodeList = null; // the list of nodes in this CN environment

        // check for a valid session
        if (session == null) {
            throw new InvalidToken("4963", "No session has been provided");
        }

        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new ServiceFailure("4962", "The provided identifier was invalid.");
        }

        String notAuthorizedCode = "4960";
        String notFoundCode = "4961";
        String serviceFailureCode = "4962";
        String invalidTokenCode = "4963";
        boolean needDeleteInfo = false;

        //SystemMetadata sysmeta = getSeriesHead(pid, notFoundCode, serviceFailureCode);
        Identifier HeadOfSid = getPIDForSID(pid, serviceFailureCode);
        if (HeadOfSid != null) {
            pid = HeadOfSid;
        }
        SystemMetadata sysmeta = null;
        try {
            sysmeta =
                getSystemMetadataForPID(pid, serviceFailureCode, invalidTokenCode, notFoundCode,
                                        needDeleteInfo);
        } catch (InvalidRequest e) {
            throw new InvalidToken(invalidTokenCode, e.getMessage());
        }

        D1AuthHelper authDel =
            new D1AuthHelper(request, pid, notAuthorizedCode, serviceFailureCode);
        authDel.doCNOnlyAuthorization(session);

        // Don't defer to superclass implementation without a locally registered identifier
        // Check for the existing identifier
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
            super.delete(session.getSubject().getValue(), pid);

        } catch (McdbDocNotFoundException e) {
            // This object is not registered in the identifier table. Assume it is of formatType
            // DATA,
            // and set the archive flag. (i.e. the *object* doesn't exist on the CN)

            try {
                //remove the systemmetadata object from the map and delete the records in the
                // systemmetadata database table
                //since this is cn, we don't need worry about the mn solr index.
                SystemMetadataManager.getInstance().delete(pid);
                String username = session.getSubject().getValue();//just for logging purpose
                //since data objects were not registered in the identifier table, we use pid as
                // the docid
                EventLog.getInstance()
                    .log(request.getRemoteAddr(), request.getHeader("User-Agent"), username,
                         pid.getValue(), Event.DELETE.xmlValue());

            } catch (RuntimeException re) {
                throw new ServiceFailure(
                    "4962", "Couldn't delete " + pid.getValue() + ". The error message was: "
                    + re.getMessage());

            } catch (InvalidRequest re) {
                throw new InvalidToken("4963", e.getMessage());
            }

        } catch (SQLException e) {
            throw new ServiceFailure(
                "4962", "Couldn't delete " + pid.getValue()
                + ". The local id of the object with the identifier can't be identified since "
                + e.getMessage());
        }

        // get the node list
        try {
            nodeList = getCNNodeList().getNodeList();

        } catch (Exception e) { // handle BaseException and other I/O issues

            // swallow errors since the call is not critical
            logMetacat.error("Can't inform MNs of the deletion of " + pid.getValue()
                                 + " due to communication issues with the CN: " + e.getMessage());

        }

        // notify the replicas
        if (sysmeta.getReplicaList() != null) {
            for (Replica replica : sysmeta.getReplicaList()) {
                NodeReference replicaNode = replica.getReplicaMemberNode();
                try {
                    if (nodeList != null) {
                        // find the node type
                        for (Node node : nodeList) {
                            if (node.getIdentifier().getValue().equals(replicaNode.getValue())) {
                                nodeType = node.getType();
                                break;

                            }
                        }
                    }

                    // only send call MN.delete() to avoid an infinite loop with the CN
                    if (nodeType != null && nodeType == NodeType.MN) {
                        Identifier mnRetId = D1Client.getMN(replicaNode).delete(null, pid);
                    }

                } catch (Exception e) {
                    // all we can really do is log errors and carry on with life
                    logMetacat.error("Error deleting pid: " + pid.getValue() + " from replica MN: "
                                         + replicaNode.getValue(), e);
                }
            }
        }

        return pid;

    }

    /**
     * Archives an object from the Coordinating Node
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - The object identifier to be deleted
     * @return pid - the identifier of the object used for the deletion
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    @Override
    public Identifier archive(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        // check for a valid session
        if (session == null) {
            throw new InvalidToken("4973", "No session has been provided");
        }

        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new ServiceFailure("4972", "The provided identifier was invalid.");
        }


        String serviceFailureCode = "4972";
        String notFoundCode = "4971";
        String notAuthorizedCode = "4970";
        String invalidTokenCode = "4973";
        boolean needDeleteInfo = false;
        //SystemMetadata sysmeta = getSeriesHead(pid, serviceFailureCode, notFoundCode);
        Identifier HeadOfSid = getPIDForSID(pid, serviceFailureCode);
        if (HeadOfSid != null) {
            pid = HeadOfSid;
        }
        SystemMetadata sysmeta = null;
        try {
            sysmeta =
                getSystemMetadataForPID(pid, serviceFailureCode, invalidTokenCode, notFoundCode,
                                        needDeleteInfo);
        } catch (InvalidRequest e) {
            throw new InvalidToken(invalidTokenCode, e.getMessage());
        }
        D1AuthHelper authDel =
            new D1AuthHelper(request, pid, notAuthorizedCode, serviceFailureCode);
        authDel.doIsAuthorized(session, sysmeta, Permission.CHANGE_PERMISSION);

        D1NodeVersionChecker checker =
            new D1NodeVersionChecker(sysmeta.getAuthoritativeMemberNode());
        String version = checker.getVersion("MNRead");
        if (version == null) {
            throw new ServiceFailure(
                "4972",
                "Couldn't determine the MNRead version of the authoritative member node for the "
                    + "pid "
                    + pid.getValue());
        } else if (version.equalsIgnoreCase(D1NodeVersionChecker.V2)) {
            //we don't apply this method to an object whose authoritative node is v2
            throw new NotAuthorized("4970", V2V1MISSMATCH);
        } else if (!version.equalsIgnoreCase(D1NodeVersionChecker.V1)) {
            //we don't understand this version (it is not v1 or v2)
            throw new NotImplemented(
                "4974", "The version of the MNRead is " + version
                + " for the authoritative member node of the object " + pid.getValue()
                + ". We don't support it.");
        }
        boolean needModifyDate = true;
        archiveCNObjectWithNotificationReplica(session, pid, sysmeta, needModifyDate);


        return pid;

    }


    /**
     * Archive a object on cn and notify the replica. This method doesn't lock the system metadata
     * map. The caller should lock it. This method doesn't check the authorization; this method only
     * accept a pid.
     *
     * @param session
     * @param pid
     * @param sysMeta
     * @param notifyReplica
     * @return
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     */
    private Identifier archiveCNObjectWithNotificationReplica(
        Session session, Identifier pid, SystemMetadata sysMeta, boolean needModifyDate)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
        boolean logArchive = true;
        archiveCNObject(logArchive, session, pid, sysMeta, needModifyDate);
        // notify the replicas
        notifyReplicaNodes(sysMeta);
        return pid;
    }


    /**
     * Set the obsoletedBy attribute in System Metadata
     *
     * @param session
     * @param pid
     * @param obsoletedByPid
     * @param serialVersion
     * @return
     * @throws NotImplemented
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidToken
     * @throws VersionMismatch
     */
    @Override
    public boolean setObsoletedBy(
        Session session, Identifier pid, Identifier obsoletedByPid, long serialVersion)
        throws NotImplemented, NotFound, NotAuthorized, ServiceFailure, InvalidRequest,
        InvalidToken, VersionMismatch {

        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new InvalidRequest("4942", "The provided identifier was invalid.");

        }

        // do we have a valid pid?
        if (obsoletedByPid == null || obsoletedByPid.getValue().trim().equals("")) {
            throw new InvalidRequest("4942", "The provided obsoletedByPid was invalid.");

        }

        try {
            if (IdentifierManager.getInstance().systemMetadataSIDExists(obsoletedByPid)) {
                throw new InvalidRequest(
                    "4942", "The provided obsoletedByPid " + obsoletedByPid.getValue()
                    + " is an existing SID. However, it must NOT be an SID.");
            }
        } catch (SQLException ee) {
            throw new ServiceFailure(
                "4941", "Couldn't determine if the obsoletedByPid " + obsoletedByPid.getValue()
                + " is an SID or not. The id shouldn't be an SID.");
        }


        // The lock to be used for this identifier
        //Lock lock = null;

        // get the subject
        Subject subject = session.getSubject();

        // are we allowed to do this?
        if (!isAuthorized(session, pid, Permission.WRITE)) {
            throw new NotAuthorized(
                "4881", Permission.WRITE + " not allowed by " + subject.getValue() + " on "
                + pid.getValue());

        }


        SystemMetadata systemMetadata = null;
        try {
            try {
                if (IdentifierManager.getInstance().systemMetadataPIDExists(pid)) {
                    systemMetadata = SystemMetadataManager.getInstance().get(pid);
                }

                // did we get it correctly?
                if (systemMetadata == null) {
                    throw new NotFound(
                        "4884", "Couldn't find an object identified by " + pid.getValue());
                }

                // does the request have the most current system metadata?
                if (systemMetadata.getSerialVersion().longValue() != serialVersion) {
                    String msg = "The requested system metadata version number " + serialVersion
                        + " differs from the current version at " + systemMetadata
                        .getSerialVersion().longValue()
                        + ". Please get the latest copy in order to modify it.";
                    throw new VersionMismatch("4886", msg);

                }

                //only apply to the object whose authoritative member node is v1.
                D1NodeVersionChecker checker =
                    new D1NodeVersionChecker(systemMetadata.getAuthoritativeMemberNode());
                String version = checker.getVersion("MNRead");
                if (version == null) {
                    throw new ServiceFailure(
                        "4941",
                        "Couldn't determine the MNRead version of the authoritative member node "
                            + "for the pid "
                            + pid.getValue());
                } else if (version.equalsIgnoreCase(D1NodeVersionChecker.V2)) {
                    //we don't apply this method to an object whose authoritative node is v2
                    throw new NotAuthorized("4945", V2V1MISSMATCH);
                } else if (!version.equalsIgnoreCase(D1NodeVersionChecker.V1)) {
                    //we don't understand this version (it is not v1 or v2)
                    throw new InvalidRequest(
                        "4942", "The version of the MNRead is " + version
                        + " for the authoritative member node of the object " + pid.getValue()
                        + ". We don't support it.");
                }

            } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
                throw new NotFound("4884", "No record found for: " + pid.getValue());

            } catch (SQLException ee) {
                throw new ServiceFailure("4882", ee.getMessage());
            }

            // set the new policy
            systemMetadata.setObsoletedBy(obsoletedByPid);

            // update the metadata
            try {
                systemMetadata.setSerialVersion(
                    systemMetadata.getSerialVersion().add(BigInteger.ONE));
                SystemMetadataManager.getInstance().store(systemMetadata);
            } catch (RuntimeException e) {
                throw new ServiceFailure("4882", e.getMessage());
            }

        } catch (RuntimeException e) {
            throw new ServiceFailure("4882", e.getMessage());
        }

        return true;
    }


    /**
     * Set the replication status for an object given the object identifier
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - the object identifier for the given object
     * @param status  - the replication status to be applied
     * @return true or false
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidToken
     * @throws NotFound
     */
    @Override
    public boolean setReplicationStatus(
        Session session, Identifier pid, NodeReference targetNode, ReplicationStatus status,
        BaseException failure)
        throws ServiceFailure, NotImplemented, InvalidToken, NotAuthorized, InvalidRequest,
        NotFound {

        // cannot be called by public
        if (session == null) {
            throw new NotAuthorized("4720", "Session cannot be null");
        }

        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new InvalidRequest("4730", "The provided identifier was invalid.");

        }

        // The lock to be used for this identifier
        //Lock lock = null;

        boolean allowed = false;
        int replicaEntryIndex = -1;
        List<Replica> replicas = null;
        // get the subject
        Subject subject = session.getSubject();
        logMetacat.debug(
            "ReplicationStatus for identifier " + pid.getValue() + " is " + status.toString());

        SystemMetadata systemMetadata = null;

        try {
            try {
                systemMetadata = SystemMetadataManager.getInstance().get(pid);
                // did we get it correctly?
                if (systemMetadata == null) {
                    logMetacat.debug("systemMetadata is null for " + pid.getValue());
                    throw new NotFound(
                        "4740", "Couldn't find an object identified by " + pid.getValue());

                }
                replicas = systemMetadata.getReplicaList();
                int count = 0;

                // was there a failure? log it
                if (failure != null && status.equals(ReplicationStatus.FAILED)) {
                    String msg =
                        "The replication request of the object identified by " + pid.getValue()
                            + " failed.  The error message was " + failure.getMessage() + ".";
                    logMetacat.error(msg);
                }

                if (replicas.size() > 0 && replicas != null) {
                    // find the target replica index in the replica list
                    for (Replica replica : replicas) {
                        String replicaNodeStr = replica.getReplicaMemberNode().getValue();
                        String targetNodeStr = targetNode.getValue();
                        logMetacat.debug("Comparing " + replicaNodeStr + " to " + targetNodeStr);

                        if (replicaNodeStr.equals(targetNodeStr)) {
                            replicaEntryIndex = count;
                            logMetacat.debug("replica entry index is: " + replicaEntryIndex);
                            break;
                        }
                        count++;

                    }
                }
                // are we allowed to do this? only CNs and target MNs are allowed
                List<Node> nodes = getCNNodeList().getNodeList();

                // find the node in the node list
                for (Node node : nodes) {

                    NodeReference nodeReference = node.getIdentifier();
                    logMetacat.debug("In setReplicationStatus(), Node reference is: "
                                         + nodeReference.getValue());

                    // allow target MN certs
                    if (targetNode.getValue().equals(nodeReference.getValue()) && node.getType()
                        .equals(NodeType.MN)) {
                        List<Subject> nodeSubjects = node.getSubjectList();

                        // check if the session subject is in the node subject list
                        for (Subject nodeSubject : nodeSubjects) {
                            logMetacat.debug("In setReplicationStatus(), comparing subjects: "
                                                 + nodeSubject.getValue() + " and "
                                                 + subject.getValue());
                            if (nodeSubject.equals(
                                subject)) { // subject of session == target node subject

                                // lastly limit to COMPLETED, INVALIDATED,
                                // and FAILED status updates from MNs only
                                if (status.equals(ReplicationStatus.COMPLETED) || status.equals(
                                    ReplicationStatus.INVALIDATED) || status.equals(
                                    ReplicationStatus.FAILED)) {
                                    allowed = true;
                                    break;

                                }
                            }
                        }
                    }
                }

                if (!allowed) {
                    //check for CN admin access
                    //allowed = isAuthorized(session, pid, Permission.WRITE);
                    D1AuthHelper authDel = new D1AuthHelper(request, pid, "4861", "????");
                    authDel.doCNOnlyAuthorization(session);
                    //                 allowed = isCNAdmin(session);
                    allowed = true;
                }

                if (!allowed) {
                    String msg = "The subject identified by " + subject.getValue()
                        + " is not a CN or MN, and does not have permission to set the "
                        + "replication status for "
                        + "the replica identified by " + targetNode.getValue() + ".";
                    logMetacat.info(msg);
                    throw new NotAuthorized("4720", msg);

                }

            } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
                throw new NotFound(
                    "4740", "No record found for: " + pid.getValue() + " : " + e.getMessage());

            }

            Replica targetReplica = new Replica();
            // set the status for the replica
            if (replicaEntryIndex != -1) {
                targetReplica = replicas.get(replicaEntryIndex);

                // don't allow status to change from COMPLETED to anything other
                // than INVALIDATED: prevents overwrites from race conditions
                if (targetReplica.getReplicationStatus().equals(ReplicationStatus.COMPLETED)
                    && !status.equals(ReplicationStatus.INVALIDATED)) {
                    throw new InvalidRequest(
                        "4730", "Status state change from " + targetReplica.getReplicationStatus()
                        + " to " + status.toString() + "is prohibited for identifier "
                        + pid.getValue() + " and target node " + targetReplica
                        .getReplicaMemberNode().getValue());
                }

                if (targetReplica.getReplicationStatus().equals(status)) {
                    //There is no change in the status, we do nothing.
                    return true;
                }

                targetReplica.setReplicationStatus(status);

                logMetacat.debug(
                    "Set the replication status for " + targetReplica.getReplicaMemberNode()
                        .getValue() + " to " + targetReplica.getReplicationStatus()
                        + " for identifier " + pid.getValue());

            } else {
                // this is a new entry, create it
                targetReplica.setReplicaMemberNode(targetNode);
                targetReplica.setReplicationStatus(status);
                targetReplica.setReplicaVerified(Calendar.getInstance().getTime());
                replicas.add(targetReplica);

            }

            systemMetadata.setReplicaList(replicas);

            // update the metadata
            try {
                systemMetadata.setSerialVersion(
                    systemMetadata.getSerialVersion().add(BigInteger.ONE));
                // Based on CN behavior discussion 9/16/15, we no longer want to
                // update the modified date for changes to the replica list
                boolean changeModificationDate = false;
                SystemMetadataManager.getInstance().store(systemMetadata, changeModificationDate);
                if (!status.equals(ReplicationStatus.QUEUED) && !status.equals(
                    ReplicationStatus.REQUESTED)) {

                    logMetacat.trace("METRICS:\tREPLICATION:\tEND REQUEST:\tPID:\t" + pid.getValue()
                                         + "\tNODE:\t" + targetNode.getValue() + "\tSIZE:\t"
                                         + systemMetadata.getSize().intValue());

                    logMetacat.trace(
                        "METRICS:\tREPLICATION:\t" + status.toString().toUpperCase() + "\tPID:\t"
                            + pid.getValue() + "\tNODE:\t" + targetNode.getValue() + "\tSIZE:\t"
                            + systemMetadata.getSize().intValue());
                }

                if (status.equals(ReplicationStatus.FAILED) && failure != null) {
                    logMetacat.warn(
                        "Replication failed for identifier " + pid.getValue() + " on target node "
                            + targetNode + ". The exception was: " + failure.getMessage());
                }

                // update the replica nodes about the completed replica when complete, failed or
                // invalid
                if (status.equals(ReplicationStatus.COMPLETED) || status.equals(
                    ReplicationStatus.FAILED) || status.equals(ReplicationStatus.INVALIDATED)) {
                    notifyReplicaNodes(systemMetadata);
                }

            } catch (RuntimeException e) {
                throw new ServiceFailure("4700", e.getMessage());

            }

        } catch (RuntimeException e) {
            String msg = "There was a RuntimeException getting the lock for " + pid.getValue();
            logMetacat.info(msg);

        }

        return true;
    }

    /**
     * Return the checksum of the object given the identifier
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - the object identifier for the given object
     * @return checksum - the checksum of the object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     */
    @Override
    public Checksum getChecksum(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        boolean isAuthorized = false;
        try {
            isAuthorized = isAuthorized(session, pid, Permission.READ);
        } catch (InvalidRequest e) {
            throw new ServiceFailure("1410", e.getDescription());
        }
        if (!isAuthorized) {
            throw new NotAuthorized("1400", Permission.READ + " not allowed on " + pid.getValue());
        }

        SystemMetadata systemMetadata = null;
        Checksum checksum = null;

        try {
            systemMetadata = SystemMetadataManager.getInstance().get(pid);
            if (systemMetadata == null) {
                String error = "";
                boolean existsInIdentifierTable = false;
                try {
                    existsInIdentifierTable =
                        IdentifierManager.getInstance().existsInIdentifierTable(pid);
                } catch (Exception e) {
                    logMetacat.warn("Couldn't determine if the " + pid.getValue()
                                        + " is in the identifier table since " + e.getMessage()
                                        + ". So we assume it is not there.");
                }
                if (existsInIdentifierTable) {
                    error = DELETEDMESSAGE;
                }
                throw new NotFound(
                    "1420", "Couldn't find an object identified by " + pid.getValue() + ". "
                    + error);
            }
            checksum = systemMetadata.getChecksum();

        } catch (RuntimeException e) {
            throw new ServiceFailure(
                "1410", "An error occurred getting the checksum for " + pid.getValue()
                + ". The error message was: " + e.getMessage());

        }

        return checksum;
    }

    /**
     * Resolve the location of a given object
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - the object identifier for the given object
     * @return objectLocationList - the list of nodes known to contain the object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     */
    @Override
    public ObjectLocationList resolve(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        throw new NotImplemented("4131", "resolve not implemented");

    }

    /**
     * Metacat does not implement this method at the CN level
     */
    @Override
    public ObjectList search(Session session, String queryType, String query)
        throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {

        throw new NotImplemented("4281", "Metacat does not implement CN.search");
    }

    /**
     * Returns the object format registered in the DataONE Object Format Vocabulary for the given
     * format identifier
     *
     * @param fmtid - the identifier of the format requested
     * @return objectFormat - the object format requested
     * @throws ServiceFailure
     * @throws NotFound
     * @throws InsufficientResources
     * @throws NotImplemented
     */
    @Override
    public ObjectFormat getFormat(ObjectFormatIdentifier fmtid)
        throws ServiceFailure, NotFound, NotImplemented {

        return ObjectFormatService.getInstance().getFormat(fmtid);

    }

    @Override
    public ObjectFormatIdentifier addFormat(
        Session session, ObjectFormatIdentifier formatId, ObjectFormat format)
        throws ServiceFailure, NotFound, NotImplemented, NotAuthorized, InvalidToken {

        logMetacat.debug(
            "CNodeService.addFormat() called.\n" + "format ID: " + format.getFormatId() + "\n"
                + "format name: " + format.getFormatName() + "\n" + "format type: "
                + format.getFormatType());

        // FIXME remove:
        if (true) {
            throw new NotImplemented("0000", "Implementation underway... Will need testing too...");
        }
        D1AuthHelper authDel = new D1AuthHelper(request, null, "????", "????");
        authDel.doCNOnlyAuthorization(session);

        String separator = ".";
        try {
            separator = PropertyService.getProperty("document.accNumSeparator");
        } catch (PropertyNotFoundException e) {
            logMetacat.warn(
                "Unable to find property \"document.accNumSeparator\"\n" + e.getMessage());
        }

        // find pids of last and next ObjectFormatList
        String OBJECT_FORMAT_DOCID = ObjectFormatService.OBJECT_FORMAT_PID_PREFIX;
        int lastRev = -1;
        try {
            lastRev = DBUtil.getLatestRevisionInDocumentTable(OBJECT_FORMAT_DOCID);
        } catch (SQLException e) {
            throw new ServiceFailure(
                "0000", "Unable to locate last revision of the object format list.\n"
                + e.getMessage());
        }
        int nextRev = lastRev + 1;
        String lastDocID = OBJECT_FORMAT_DOCID + separator + lastRev;
        String nextDocID = OBJECT_FORMAT_DOCID + separator + nextRev;

        Identifier lastPid = new Identifier();
        lastPid.setValue(lastDocID);
        Identifier nextPid = new Identifier();
        nextPid.setValue(nextDocID);

        logMetacat.debug("Last ObjectFormatList document ID: " + lastDocID + "\n"
                             + "Next ObjectFormatList document ID: " + nextDocID);

        // add new format to the current ObjectFormatList
        ObjectFormatList objectFormatList = ObjectFormatService.getInstance().listFormats();
        List<ObjectFormat> innerList = objectFormatList.getObjectFormatList();
        innerList.add(format);

        // get existing (last) sysmeta and make a copy
        SystemMetadata lastSysmeta = getSystemMetadata(session, lastPid);
        SystemMetadata nextSysmeta = new SystemMetadata();
        try {
            BeanUtils.copyProperties(nextSysmeta, lastSysmeta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ServiceFailure(
                "0000", "Unable to create system metadata for updated object format list.\n"
                + e.getMessage());
        }

        // create the new object format list, and update the old sysmeta with obsoletedBy
        createNewObjectFormatList(session, lastPid, nextPid, objectFormatList, nextSysmeta);
        updateOldObjectFormatList(session, lastPid, nextPid, lastSysmeta);

        // TODO add to ObjectFormatService local cache?

        return formatId;
    }

    /**
     * Creates the object for the next / updated version of the ObjectFormatList.
     *
     * @param session
     * @param lastPid
     * @param nextPid
     * @param objectFormatList
     * @param lastSysmeta
     */
    private void createNewObjectFormatList(
        Session session, Identifier lastPid, Identifier nextPid, ObjectFormatList objectFormatList,
        SystemMetadata lastSysmeta)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented {

        PipedInputStream is = new PipedInputStream();
        PipedOutputStream os = null;

        try {
            os = new PipedOutputStream(is);
            TypeMarshaller.marshalTypeToOutputStream(objectFormatList, os);
        } catch (MarshallingException | IOException e) {
            throw new ServiceFailure(
                "0000", "Unable to marshal object format list.\n" + e.getMessage());
        } finally {
            try {
                os.flush();
                os.close();
            } catch (IOException ioe) {
                throw new ServiceFailure(
                    "0000", "Unable to marshal object format list.\n" + ioe.getMessage());
            }
        }

        BigInteger docSize = lastSysmeta.getSize();
        try {
            docSize = BigInteger.valueOf(is.available());
        } catch (IOException e) {
            logMetacat.warn("Unable to set an accurate size for the new object format list.", e);
        }

        lastSysmeta.setIdentifier(nextPid);
        lastSysmeta.setObsoletes(lastPid);
        lastSysmeta.setSize(docSize);
        lastSysmeta.setSubmitter(session.getSubject());
        lastSysmeta.setDateUploaded(new Date());

        // create new object format list
        try {
            create(session, nextPid, is, lastSysmeta);
        } catch (IdentifierNotUnique | UnsupportedType | InsufficientResources |
                 InvalidSystemMetadata | InvalidRequest e) {
            throw new ServiceFailure(
                "0000", "Unable to create() new object format list" + e.getMessage());
        }
    }

    /**
     * Updates the SystemMetadata for the old version of the ObjectFormatList by setting the
     * obsoletedBy value to the pid of the new version of the ObjectFormatList.
     *
     * @param session
     * @param lastPid
     * @param obsoletedByPid
     * @param lastSysmeta
     * @throws ServiceFailure
     */
    private void updateOldObjectFormatList(
        Session session, Identifier lastPid, Identifier obsoletedByPid, SystemMetadata lastSysmeta)
        throws ServiceFailure {

        lastSysmeta.setObsoletedBy(obsoletedByPid);

        try {
            this.updateSystemMetadata(session, lastPid, lastSysmeta);
        } catch (NotImplemented | NotAuthorized | ServiceFailure | InvalidRequest |
                 InvalidSystemMetadata | InvalidToken e) {
            throw new ServiceFailure(
                "0000", "Unable to update metadata of old object format list.\n" + e.getMessage());
        }
    }

    /**
     * Returns a list of all object formats registered in the DataONE Object Format Vocabulary
     *
     * @return objectFormatList - The list of object formats registered in the DataONE Object Format
     *     Vocabulary
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InsufficientResources
     */
    @Override
    public ObjectFormatList listFormats() throws ServiceFailure, NotImplemented {

        return ObjectFormatService.getInstance().listFormats();
    }

    /**
     * Returns a list of nodes that have been registered with the DataONE infrastructure
     *
     * @return nodeList - List of nodes from the registry
     * @throws ServiceFailure
     * @throws NotImplemented
     */
    @Override
    public NodeList listNodes() throws NotImplemented, ServiceFailure {

        throw new NotImplemented("4800", "listNodes not implemented");
    }

    /**
     * Provides a mechanism for adding system metadata independently of its associated object, such
     * as when adding system metadata for data objects.
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - The identifier of the object to register the system metadata against
     * @param sysmeta - The system metadata to be registered
     * @return true if the registration succeeds
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     */
    @Override
    public Identifier registerSystemMetadata(
        Session session, Identifier pid, SystemMetadata sysmeta)
        throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest,
        InvalidSystemMetadata {

        // The lock to be used for this identifier
        //Lock lock = null;

        // TODO: control who can call this?
        if (session == null) {
            //TODO: many of the thrown exceptions do not use the correct error codes
            //check these against the docs and correct them
            throw new NotAuthorized(
                "4861", "No Session - could not authorize for registration."
                + "  If you are not logged in, please do so and retry the request.");
        } else {
            //only CN is allowed
            D1AuthHelper authDel = new D1AuthHelper(request, pid, "4861", "????");
            authDel.doCNOnlyAuthorization(session);
        }
        // the identifier can't be an SID
        try {
            if (IdentifierManager.getInstance().systemMetadataSIDExists(pid)) {
                throw new InvalidRequest(
                    "4863", "The provided identifier " + pid.getValue()
                    + " is a series id which is not allowed.");
            }
        } catch (SQLException sqle) {
            throw new ServiceFailure(
                "4862", "Couldn't determine if the pid " + pid.getValue() + " is a series id since "
                + sqle.getMessage());
        }

        // verify that guid == SystemMetadata.getIdentifier()
        logMetacat.debug(
            "Comparing guid|sysmeta_guid: " + pid.getValue() + "|" + sysmeta.getIdentifier()
                .getValue());
        if (!pid.getValue().equals(sysmeta.getIdentifier().getValue())) {
            throw new InvalidRequest("4863", "The identifier in method call (" + pid.getValue()
                + ") does not match identifier in system metadata (" + sysmeta.getIdentifier()
                .getValue() + ").");
        }

        //check if the sid is legitimate in the system metadata
        //checkSidInModifyingSystemMetadata(sysmeta, "4864", "4862");
        Identifier sid = sysmeta.getSeriesId();
        if (sid != null) {
            if (!isValidIdentifier(sid)) {
                throw new InvalidRequest(
                    "4863", "The series id in the system metadata is invalid in the request.");
            }
        }

        try {
            logMetacat.debug("Checking if identifier exists...");
            // Check that the identifier does not already exist
            try {
                if (IdentifierManager.getInstance().systemMetadataPIDExists(pid)) {
                    throw new InvalidRequest("4863",
                                             "The identifier is already in use by an existing "
                                                 + "object.");
                }
            } catch (SQLException ee) {
                throw new ServiceFailure(
                    "4862", "Error inserting system metadata: " + ee.getClass() + ": "
                    + ee.getMessage());
            }
            // insert the system metadata into the object store
            logMetacat.debug("Starting to insert SystemMetadata...");
            try {
                //for the object whose authoriative mn is v1. we need reset the modification date.
                //d1-sync already set the serial version. so we don't need do again.
                D1NodeVersionChecker checker =
                    new D1NodeVersionChecker(sysmeta.getAuthoritativeMemberNode());
                String version = checker.getVersion("MNRead");
                boolean changeModificationDate = false;
                if (version != null && version.equalsIgnoreCase(D1NodeVersionChecker.V1)) {
                    changeModificationDate = true;
                }
                SystemMetadataManager.getInstance().store(sysmeta, changeModificationDate);
            } catch (RuntimeException e) {
                logMetacat.error("Problem registering system metadata: " + pid.getValue(), e);
                throw new ServiceFailure(
                    "4862", "Error inserting system metadata: " + e.getClass() + ": "
                    + e.getMessage());

            }

        } catch (RuntimeException e) {
            throw new ServiceFailure(
                "4862", "Error inserting system metadata: " + e.getClass() + ": " + e.getMessage());

        }


        logMetacat.debug("Returning from registerSystemMetadata");

        try {
            String localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
            EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"),
                                       session.getSubject().getValue(), localId,
                                       "registerSystemMetadata");
        } catch (McdbDocNotFoundException e) {
            // do nothing, no localId to log with
            logMetacat.warn(
                "Could not log 'registerSystemMetadata' event because no localId was found for "
                    + "pid: "
                    + pid.getValue());
        } catch (SQLException ee) {
            // do nothing, no localId to log with
            logMetacat.warn(
                "Could not log 'registerSystemMetadata' event because the localId couldn't be "
                    + "identified for pid: "
                    + pid.getValue());
        }


        return pid;
    }

    /**
     * Given an optional scope and format, reserves and returns an identifier within that scope and
     * format that is unique and will not be used by any other sessions.
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - The identifier of the object to register the system metadata against
     * @param scope   - An optional string to be used to qualify the scope of the identifier
     *                namespace, which is applied differently depending on the format requested. If
     *                scope is not supplied, a default scope will be used.
     * @param format  - The optional name of the identifier format to be used, drawn from a
     *                DataONE-specific vocabulary of identifier format names, including several
     *                common syntaxes such as DOI, LSID, UUID, and LSRN, among others. If the format
     *                is not supplied by the caller, the CN service will use a default identifier
     *                format, which may change over time.
     * @return true if the registration succeeds
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws IdentifierNotUnique
     * @throws NotImplemented
     */
    @Override
    public Identifier reserveIdentifier(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, NotImplemented,
        InvalidRequest {

        throw new NotImplemented("4191", "reserveIdentifier not implemented on this node");
    }

    @Override
    public Identifier generateIdentifier(Session session, String scheme, String fragment)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest {
        throw new NotImplemented("4191", "generateIdentifier not implemented on this node");
    }

    /**
     * Checks whether the pid is reserved by the subject in the session param If the reservation is
     * held on the pid by the subject, we return true.
     *
     * @param session - the Session object containing the Subject
     * @param pid     - The identifier to check
     * @return true if the reservation exists for the subject/pid
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotFound            - when the pid is not found (in use or in reservation)
     * @throws NotAuthorized       - when the subject does not hold a reservation on the pid
     * @throws IdentifierNotUnique - when the pid is in use
     * @throws NotImplemented
     */

    @Override
    public boolean hasReservation(Session session, Subject subject, Identifier pid)
        throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, NotImplemented,
        InvalidRequest {

        throw new NotImplemented("4191", "hasReservation not implemented on this node");
    }

    /**
     * Changes ownership (RightsHolder) of the specified object to the subject specified by userId
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - Identifier of the object to be modified
     * @param userId  - The subject that will be taking ownership of the specified object.
     * @return pid - the identifier of the modified object
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotFound
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    @Override
    public Identifier setRightsHolder(
        Session session, Identifier pid, Subject userId, long serialVersion)
        throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, NotImplemented,
        InvalidRequest, VersionMismatch {

        // The lock to be used for this identifier
        //Lock lock = null;

        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new InvalidRequest("4442", "The provided identifier was invalid.");
        }

        // get the subject
        Subject subject = session.getSubject();

        String serviceFailureCode = "4490";
        String notFoundCode = "4460";
        String notAuthorizedCode = "4440";
        String invalidRequestCode = "4442";
        boolean needDeleteInfo = false;
        Identifier HeadOfSid = getPIDForSID(pid, serviceFailureCode);
        if (HeadOfSid != null) {
            pid = HeadOfSid;
        }
        SystemMetadata systemMetadata =
            getSystemMetadataForPID(pid, serviceFailureCode, invalidRequestCode, notFoundCode,
                                    needDeleteInfo);
        //SystemMetadata systemMetadata = getSeriesHead(pid, serviceFailureCode, notFoundCode,
        // invalidRequestCode);

        D1AuthHelper authDel =
            new D1AuthHelper(request, pid, notAuthorizedCode, serviceFailureCode);
        authDel.doIsAuthorized(session, systemMetadata, Permission.CHANGE_PERMISSION);

        try {
            try {
                // does the request have the most current system metadata?
                if (systemMetadata.getSerialVersion().longValue() != serialVersion) {
                    String msg = "The requested system metadata version number " + serialVersion
                        + " differs from the current version at " + systemMetadata
                        .getSerialVersion().longValue()
                        + ". Please get the latest copy in order to modify it.";
                    throw new VersionMismatch("4443", msg);
                }

                //only apply to the object whose authoritative member node is v1.
                D1NodeVersionChecker checker =
                    new D1NodeVersionChecker(systemMetadata.getAuthoritativeMemberNode());
                String version = checker.getVersion("MNRead");
                if (version == null) {
                    throw new ServiceFailure(
                        "4490",
                        "Couldn't determine the MNRead version of the authoritative member node "
                            + "storage version for the pid "
                            + pid.getValue());
                } else if (version.equalsIgnoreCase(D1NodeVersionChecker.V2)) {
                    //we don't apply this method to an object whose authoritative node is v2
                    throw new NotAuthorized("4440", V2V1MISSMATCH);
                } else if (!version.equalsIgnoreCase(D1NodeVersionChecker.V1)) {
                    //we don't understand this version (it is not v1 or v2)
                    throw new InvalidRequest(
                        "4442", "The version of the MNRead is " + version
                        + " for the authoritative member node of the object " + pid.getValue()
                        + ". We don't support it.");
                }

            } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
                throw new NotFound("4460", "No record found for: " + pid.getValue());

            }

            // set the new rights holder
            systemMetadata.setRightsHolder(userId);

            // update the metadata
            try {
                systemMetadata.setSerialVersion(
                    systemMetadata.getSerialVersion().add(BigInteger.ONE));
                SystemMetadataManager.getInstance().store(systemMetadata);
                notifyReplicaNodes(systemMetadata);

            } catch (RuntimeException e) {
                throw new ServiceFailure("4490", e.getMessage());

            }

        } catch (RuntimeException e) {
            throw new ServiceFailure("4490", e.getMessage());

        }

        return pid;
    }

    /**
     * Verify that a replication task is authorized by comparing the target node's Subject (from the
     * X.509 certificate-derived Session) with the list of subjects in the known, pending
     * replication tasks map.
     *
     * @param originatingNodeSession - Session information that contains the identity of the calling
     *                               user
     * @param targetNodeSubject      - Subject identifying the target node
     * @param pid                    - the identifier of the object to be replicated
     * @param replicatePermission    - the execute permission to be granted
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotFound
     */
    @Override
    public boolean isNodeAuthorized(
        Session originatingNodeSession, Subject targetNodeSubject, Identifier pid)
        throws NotImplemented, NotAuthorized, InvalidToken, ServiceFailure, NotFound,
        InvalidRequest {

        boolean isAllowed = false;
        SystemMetadata sysmeta = null;
        NodeReference targetNode = null;

        try {
            // get the target node reference from the nodes list
            List<Node> nodes = getCNNodeList().getNodeList();

            if (nodes != null) {
                for (Node node : nodes) {

                    if (node.getSubjectList() != null) {

                        for (Subject nodeSubject : node.getSubjectList()) {

                            if (nodeSubject.equals(targetNodeSubject)) {
                                targetNode = node.getIdentifier();
                                logMetacat.debug("targetNode is : " + targetNode.getValue());
                                break;
                            }
                        }
                    }

                    if (targetNode != null) {
                        break;
                    }
                }

            } else {
                String msg = "Couldn't get the node list from the CN";
                logMetacat.debug(msg);
                throw new ServiceFailure("4872", msg);

            }

            // can't find a node listed with the given subject
            if (targetNode == null) {
                String msg = "There is no Member Node registered with a node subject " + "matching "
                    + targetNodeSubject.getValue();
                logMetacat.info(msg);
                throw new NotAuthorized("4871", msg);

            }

            logMetacat.debug("Getting system metadata for identifier " + pid.getValue());

            sysmeta = SystemMetadataManager.getInstance().get(pid);
            if (sysmeta != null) {

                List<Replica> replicaList = sysmeta.getReplicaList();

                if (replicaList != null) {

                    // find the replica with the status set to 'requested'
                    for (Replica replica : replicaList) {
                        ReplicationStatus status = replica.getReplicationStatus();
                        NodeReference listedNode = replica.getReplicaMemberNode();
                        if (listedNode != null && targetNode != null) {
                            logMetacat.debug("Comparing " + listedNode.getValue() + " to "
                                                 + targetNode.getValue());

                            if (listedNode.getValue().equals(targetNode.getValue())
                                && status.equals(ReplicationStatus.REQUESTED)) {
                                isAllowed = true;
                                break;

                            }
                        }
                    }
                }
                logMetacat.debug(
                    "The " + targetNode.getValue() + " is allowed " + "to replicate: " + isAllowed
                        + " for " + pid.getValue());

            } else {
                logMetacat.debug("System metadata for identifier " + pid.getValue() + " is null.");
                String error = "";
                boolean existsInIdentifierTable = false;
                try {
                    existsInIdentifierTable =
                        IdentifierManager.getInstance().existsInIdentifierTable(pid);

                } catch (Exception e) {
                    logMetacat.warn("Couldn't determine if the " + pid.getValue()
                                        + " is in the identifier table since " + e.getMessage()
                                        + ". So we assume it is not there.");
                }
                if (existsInIdentifierTable) {
                    error = DELETEDMESSAGE;
                }
                throw new NotFound(
                    "4874", "Couldn't find an object identified by " + pid.getValue() + ". "
                    + error);

            }

        } catch (RuntimeException e) {
            ServiceFailure sf = new ServiceFailure("4872",
                                                   "Runtime Exception: Couldn't determine if node"
                                                       + " is allowed: "
                                                       + e.getMessage());
            sf.initCause(e);
            throw sf;

        }

        return isAllowed;

    }

    /**
     * Adds a new object to the Node, where the object is a science metadata object.
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - The object identifier to be created
     * @param object  - the object bytes
     * @param sysmeta - the system metadata that describes the object
     * @return pid - the object identifier created
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    public Identifier create(
        Session session, Identifier pid, InputStream object, SystemMetadata sysmeta)
        throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
        InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest {

        try {
            // verify the pid is valid format
            if (!isValidIdentifier(pid)) {
                throw new InvalidRequest("4891", "The provided identifier is invalid.");
            }
            if (session == null) {
                throw new InvalidToken("4894", "Session is required to WRITE to the Node.");
            }
            logMetacat.debug("CN.create -start to create the object with pid " + pid.getValue());
            // The lock to be used for this identifier
            //Lock lock = null;

            try {


                // are we allowed?
                boolean isAllowed = false;
                D1AuthHelper authDel = new D1AuthHelper(request, pid, "4861", "????");
                authDel.doCNOnlyAuthorization(session);

                isAllowed = true;
                // proceed if we're called by a CN
                if (isAllowed) {
                    objectExists(pid);

                    //check if the series id is legitimate. It uses the same rules of the method
                    // registerSystemMetadata
                    //checkSidInModifyingSystemMetadata(sysmeta, "4896", "4893");
                    Identifier sid = sysmeta.getSeriesId();
                    if (sid != null) {
                        if (!isValidIdentifier(sid)) {
                            throw new InvalidRequest(
                                "4891",
                                "The series id in the system metadata is invalid in the request.");
                        }
                    }
                    // create the coordinating node version of the document
                    logMetacat.debug(
                        "CN.create - after locking identifier, passing authorization check, "
                            + "continue to create the object "
                            + pid.getValue());
                    sysmeta.setSerialVersion(BigInteger.ONE);
                    //for the object whose authoritative mn is v1. we need reset the modification
                    // date.
                    //for the object whose authoritative mn is v2. we just accept the
                    // modification date.
                    D1NodeVersionChecker checker =
                        new D1NodeVersionChecker(sysmeta.getAuthoritativeMemberNode());
                    String version = checker.getVersion("MNRead");
                    boolean changeModificationDate = false;
                    if (version != null && version.equalsIgnoreCase(D1NodeVersionChecker.V1)) {
                        //sysmeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
                        changeModificationDate = true;
                    }
                    //sysmeta.setArchived(false); // this is a create op, not update

                    // the CN should have set the origin and authoritative member node fields
                    try {
                        sysmeta.getOriginMemberNode().getValue();
                        sysmeta.getAuthoritativeMemberNode().getValue();

                    } catch (NullPointerException npe) {
                        throw new InvalidSystemMetadata(
                            "4896",
                            "Both the origin and authoritative member node identifiers need to be"
                                + " set.");

                    }
                    pid = super.create(session, pid, object, sysmeta, changeModificationDate);

                } else {
                    String msg = "The subject listed as " + session.getSubject().getValue()
                        + " isn't allowed to call create() on a Coordinating Node for pid "
                        + pid.getValue();
                    logMetacat.error(msg);
                    throw new NotAuthorized("1100", msg);
                }

            } catch (RuntimeException e) {
                // Convert Hazelcast runtime exceptions to service failures
                String msg =
                    "There was a problem creating the object identified by " + pid.getValue()
                        + ". There error message was: " + e.getMessage();
                throw new ServiceFailure("4893", msg);

            }
        } finally {
            IOUtils.closeQuietly(object);
        }
        return pid;

    }

    /**
     * Set access for a given object using the object identifier and a Subject under a given
     * Session. This method only applies the objects whose authoritative mn is a v1 node.
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - the object identifier for the given object to apply the policy
     * @param policy  - the access policy to be applied
     * @return true if the application of the policy succeeds
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotFound
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    public boolean setAccessPolicy(
        Session session, Identifier pid, AccessPolicy accessPolicy, long serialVersion)
        throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, NotImplemented,
        InvalidRequest, VersionMismatch {


        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new InvalidRequest("4402", "The provided identifier was invalid.");
        }

        String serviceFailureCode = "4430";
        String notFoundCode = "4400";
        String notAuthorizedCode = "4420";
        String invalidRequestCode = "4402";
        boolean needDeleteInfo = false;
        Identifier HeadOfSid = getPIDForSID(pid, serviceFailureCode);
        if (HeadOfSid != null) {
            pid = HeadOfSid;
        }
        SystemMetadata systemMetadata =
            getSystemMetadataForPID(pid, serviceFailureCode, invalidRequestCode, notFoundCode,
                                    needDeleteInfo);

        D1AuthHelper authDel =
            new D1AuthHelper(request, pid, notAuthorizedCode, serviceFailureCode);
        authDel.doIsAuthorized(session, systemMetadata, Permission.CHANGE_PERMISSION);

        try {
            try {
                systemMetadata = SystemMetadataManager.getInstance().get(pid);
                if (systemMetadata == null) {
                    throw new NotFound(
                        "4400", "Couldn't find an object identified by " + pid.getValue());

                }
                // does the request have the most current system metadata?
                if (systemMetadata.getSerialVersion().longValue() != serialVersion) {
                    String msg = "The requested system metadata version number " + serialVersion
                        + " differs from the current version at " + systemMetadata
                        .getSerialVersion().longValue()
                        + ". Please get the latest copy in order to modify it.";
                    throw new VersionMismatch("4402", msg);

                }

                D1NodeVersionChecker checker =
                    new D1NodeVersionChecker(systemMetadata.getAuthoritativeMemberNode());
                String version = checker.getVersion("MNRead");
                if (version == null) {
                    throw new ServiceFailure(
                        "4430",
                        "Couldn't determine the version of MNRead of the authoritative member "
                            + "node for the pid "
                            + pid.getValue());
                } else if (version.equalsIgnoreCase(D1NodeVersionChecker.V2)) {
                    //we don't apply this method to an object whose authoritative node is v2
                    throw new NotAuthorized("4420", V2V1MISSMATCH);
                } else if (!version.equalsIgnoreCase(D1NodeVersionChecker.V1)) {
                    //we don't understand this version (it is not v1 or v2)
                    throw new InvalidRequest(
                        "4402", "The version of the MNRead is " + version
                        + " for the authoritative member node of the object " + pid.getValue()
                        + ". We don't support it.");
                }

            } catch (RuntimeException e) {
                // convert Hazelcast RuntimeException to NotFound
                throw new NotFound("4400", "No record found for: " + pid);

            }

            // set the access policy
            systemMetadata.setAccessPolicy(accessPolicy);

            // update the system metadata
            try {
                systemMetadata.setSerialVersion(
                    systemMetadata.getSerialVersion().add(BigInteger.ONE));
                SystemMetadataManager.getInstance().store(systemMetadata);
                notifyReplicaNodes(systemMetadata);

            } catch (RuntimeException e) {
                // convert Hazelcast RuntimeException to ServiceFailure
                throw new ServiceFailure("4430", e.getMessage());

            }

        } catch (RuntimeException e) {
            throw new ServiceFailure("4430", e.getMessage());

        }

        // TODO: how do we know if the map was persisted?
        return true;
    }

    /**
     * Full replacement of replication metadata in the system metadata for the specified object,
     * changes date system metadata modified
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - the object identifier for the given object to apply the policy
     * @param replica - the replica to be updated
     * @return
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws NotFound
     * @throws VersionMismatch
     */
    @Override
    public boolean updateReplicationMetadata(
        Session session, Identifier pid, Replica replica, long serialVersion)
        throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest, NotFound,
        VersionMismatch {

        // get the subject
        Subject subject = session.getSubject();

        // are we allowed to do this?
        if (session == null) {
            throw new NotAuthorized(
                "4851",
                "Session cannot be null. It is not authorized for updating the replication "
                    + "metadata of the object "
                    + pid.getValue());
        } else {
            D1AuthHelper authDel = new D1AuthHelper(request, pid, "4851", "4852");
            authDel.doCNOnlyAuthorization(session);
        }

        SystemMetadata systemMetadata = null;
        try {

            try {
                systemMetadata = SystemMetadataManager.getInstance().get(pid);
                // does the request have the most current system metadata?
                if (systemMetadata.getSerialVersion().longValue() != serialVersion) {
                    String msg = "The requested system metadata version number " + serialVersion
                        + " differs from the current version at " + systemMetadata
                        .getSerialVersion().longValue()
                        + ". Please get the latest copy in order to modify it.";
                    throw new VersionMismatch("4855", msg);
                }

            } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
                throw new NotFound(
                    "4854", "No record found for: " + pid.getValue() + " : " + e.getMessage());

            }

            // set the status for the replica
            List<Replica> replicas = systemMetadata.getReplicaList();
            NodeReference replicaNode = replica.getReplicaMemberNode();
            ReplicationStatus replicaStatus = replica.getReplicationStatus();
            int index = 0;
            for (Replica listedReplica : replicas) {

                // remove the replica that we are replacing
                if (replicaNode.getValue()
                    .equals(listedReplica.getReplicaMemberNode().getValue())) {
                    // don't allow status to change from COMPLETED to anything other
                    // than INVALIDATED: prevents overwrites from race conditions
                    if (!listedReplica.getReplicationStatus().equals(replicaStatus) && listedReplica
                        .getReplicationStatus().equals(ReplicationStatus.COMPLETED)
                        && !replicaStatus.equals(ReplicationStatus.INVALIDATED)) {
                        throw new InvalidRequest(
                            "4853", "Status state change from "
                            + listedReplica.getReplicationStatus() + " to "
                            + replicaStatus.toString() + "is prohibited for identifier "
                            + pid.getValue() + " and target node " + listedReplica
                            .getReplicaMemberNode().getValue());

                    }
                    replicas.remove(index);
                    break;

                }
                index++;
            }

            // add the new replica item
            replicas.add(replica);
            systemMetadata.setReplicaList(replicas);

            // update the metadata
            try {
                systemMetadata.setSerialVersion(
                    systemMetadata.getSerialVersion().add(BigInteger.ONE));
                // Based on CN behavior discussion 9/16/15, we no longer want to
                // update the modified date for changes to the replica list
                boolean changeModificationDate = false;
                SystemMetadataManager.getInstance().store(systemMetadata, changeModificationDate);
                // inform replica nodes of the change if the status is complete
                if (replicaStatus.equals(ReplicationStatus.COMPLETED)) {
                    notifyReplicaNodes(systemMetadata);
                }
            } catch (RuntimeException e) {
                logMetacat.info("Unknown RuntimeException thrown: " + e.getCause().getMessage());
                throw new ServiceFailure("4852", e.getMessage());

            }

        } catch (RuntimeException e) {
            logMetacat.info("Unknown RuntimeException thrown: " + e.getCause().getMessage());
            throw new ServiceFailure("4852", e.getMessage());

        }

        return true;

    }

    /**
     *
     */
    @Override
    public ObjectList listObjects(
        Session session, Date startTime, Date endTime, ObjectFormatIdentifier formatid,
        NodeReference nodeId, Identifier identifier, Integer start, Integer count)
        throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure {

        return super.listObjects(
            session, startTime, endTime, formatid, identifier, nodeId, start, count);
    }


    /**
     * Returns a list of checksum algorithms that are supported by DataONE.
     *
     * @return cal  the list of checksum algorithms
     * @throws ServiceFailure
     * @throws NotImplemented
     */
    @Override
    public ChecksumAlgorithmList listChecksumAlgorithms() throws ServiceFailure, NotImplemented {
        ChecksumAlgorithmList cal = new ChecksumAlgorithmList();
        cal.addAlgorithm("MD5");
        cal.addAlgorithm("SHA-1");
        return cal;

    }

    /**
     * Notify replica Member Nodes of system metadata changes for a given pid
     *
     * @param currentSystemMetadata - the up to date system metadata
     */
    public void notifyReplicaNodes(SystemMetadata currentSystemMetadata) {

        Session session = null;
        List<Replica> replicaList = currentSystemMetadata.getReplicaList();
        //MNode mn = null;
        NodeReference replicaNodeRef = null;
        CNode cn = null;
        NodeType nodeType = null;
        List<Node> nodeList = null;

        try {
            nodeList = getCNNodeList().getNodeList();

        } catch (Exception e) { // handle BaseException and other I/O issues

            // swallow errors since the call is not critical
            logMetacat.error("Can't inform MNs of system metadata changes due "
                                 + "to communication issues with the CN: " + e.getMessage());

        }

        if (replicaList != null) {

            // iterate through the replicas and inform  MN replica nodes
            for (Replica replica : replicaList) {
                String replicationVersion = null;
                replicaNodeRef = replica.getReplicaMemberNode();
                try {
                    if (nodeList != null) {
                        // find the node type
                        for (Node node : nodeList) {
                            if (node.getIdentifier().getValue().equals(replicaNodeRef.getValue())) {
                                nodeType = node.getType();
                                D1NodeVersionChecker checker =
                                    new D1NodeVersionChecker(replicaNodeRef);
                                replicationVersion = checker.getVersion("MNRead");
                                break;

                            }
                        }
                    }

                    // notify only MNs
                    if (replicationVersion != null && nodeType != null && nodeType == NodeType.MN) {
                        if (replicationVersion.equalsIgnoreCase(D1NodeVersionChecker.V2)) {
                            //connect to a v2 mn
                            MNode mn = D1Client.getMN(replicaNodeRef);
                            mn.systemMetadataChanged(session, currentSystemMetadata.getIdentifier(),
                                                     currentSystemMetadata.getSerialVersion()
                                                         .longValue(),
                                                     currentSystemMetadata.getDateSysMetadataModified());
                        } else if (replicationVersion.equalsIgnoreCase(D1NodeVersionChecker.V1)) {
                            //connect to a v1 mn
                            org.dataone.client.v1.MNode mn =
                                org.dataone.client.v1.itk.D1Client.getMN(replicaNodeRef);
                            mn.systemMetadataChanged(session, currentSystemMetadata.getIdentifier(),
                                                     currentSystemMetadata.getSerialVersion()
                                                         .longValue(),
                                                     currentSystemMetadata.getDateSysMetadataModified());
                        }

                    }

                } catch (Exception e) { // handle BaseException and other I/O issues

                    // swallow errors since the call is not critical
                    logMetacat.error("Can't inform " + replicaNodeRef.getValue()
                                         + " of system metadata changes due "
                                         + "to communication issues with the CN: "
                                         + e.getMessage());

                }
            }
        }
    }

    /**
     * Update the system metadata of the specified pid. Note: the serial version and the replica
     * list in the new system metadata will be ignored and the old values will be kept.
     */
    @Override
    public boolean updateSystemMetadata(Session session, Identifier pid, SystemMetadata sysmeta)
        throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest, InvalidSystemMetadata,
        InvalidToken {
        if (sysmeta == null) {
            throw new InvalidRequest(
                "4863",
                "The system metadata object should NOT be null in the updateSystemMetadata "
                    + "request.");
        }
        if (pid == null || pid.getValue() == null) {
            throw new InvalidRequest(
                "4863", "Please specify the id in the updateSystemMetadata request ");
        }

        if (session == null) {
            //TODO: many of the thrown exceptions do not use the correct error codes
            //check these against the docs and correct them
            throw new NotAuthorized(
                "4861", "No Session - could not authorize for updating system metadata."
                + "  If you are not logged in, please do so and retry the request.");
        } else {
            //only CN is allowed
            D1AuthHelper authDel = new D1AuthHelper(request, pid, "4861", "????");
            authDel.doCNOnlyAuthorization(session);
        }

        //update the system metadata locally
        boolean success = false;

        SystemMetadata currentSysmeta = SystemMetadataManager.getInstance().get(pid);

        if (currentSysmeta == null) {
            throw new InvalidRequest(
                "4863", "We can't find the current system metadata on the member node for the id "
                + pid.getValue());
        }
        // CN will ignore the coming serial version and replica list fields from the mn node.
        BigInteger currentSerialVersion = currentSysmeta.getSerialVersion();
        sysmeta.setSerialVersion(currentSerialVersion);
        List<Replica> replicas = currentSysmeta.getReplicaList();
        sysmeta.setReplicaList(replicas);
        boolean needUpdateModificationDate =
            false;//cn doesn't need to change the modification date.
        boolean fromCN = true;
        success =
            updateSystemMetadata(session, pid, sysmeta, needUpdateModificationDate, currentSysmeta,
                                 fromCN);

        return success;
    }

    @Override
    public boolean synchronize(Session session, Identifier pid)
        throws NotAuthorized, InvalidRequest, NotImplemented {
        throw new NotImplemented("0000", "CN query services are not implemented in Metacat.");

    }

    @Override
    public QueryEngineDescription getQueryEngineDescription(Session session, String queryEngine)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, NotFound {
        throw new NotImplemented("0000", "CN query services are not implemented in Metacat.");

    }

    @Override
    public QueryEngineList listQueryEngines(Session session)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented {
        throw new NotImplemented("0000", "CN query services are not implemented in Metacat.");

    }

    @Override
    public InputStream query(Session session, String queryEngine, String query)
        throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented,
        NotFound {
        throw new NotImplemented("0000", "CN query services are not implemented in Metacat.");

    }

    @Override
    public Node getCapabilities() throws NotImplemented, ServiceFailure {
        throw new NotImplemented("0000", "The CN capabilities are not stored in Metacat.");
    }

}
