package edu.ucsb.nceas.metacat.dataone;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.ReadOnlyChecker;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.upgrade.UpdateDOI;
import edu.ucsb.nceas.metacat.common.query.EnabledQueryEngines;
import edu.ucsb.nceas.metacat.common.resourcemap.ResourceMapNamespaces;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.quota.QuotaServiceManager;
import edu.ucsb.nceas.metacat.dataone.resourcemap.ResourceMapModifier;
import edu.ucsb.nceas.metacat.doi.DOIException;
import edu.ucsb.nceas.metacat.doi.DOIServiceFactory;
import edu.ucsb.nceas.metacat.download.PackageDownloaderV1;
import edu.ucsb.nceas.metacat.download.PackageDownloaderV2;
import edu.ucsb.nceas.metacat.index.MetacatSolrEngineDescriptionHandler;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.index.queue.IndexGenerator;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.storage.ObjectInfo;
import edu.ucsb.nceas.metacat.storage.Storage;
import edu.ucsb.nceas.metacat.systemmetadata.MCSystemMetadata;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.systemmetadata.log.SystemMetadataDeltaLogger;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.XMLUtilities;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.formats.ObjectFormatInfo;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.ore.ResourceMapFactory;
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
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.mn.tier1.v2.MNCore;
import org.dataone.service.mn.tier1.v2.MNRead;
import org.dataone.service.mn.tier2.v2.MNAuthorization;
import org.dataone.service.mn.tier3.v2.MNStorage;
import org.dataone.service.mn.tier4.v2.MNReplication;
import org.dataone.service.mn.v2.MNPackage;
import org.dataone.service.mn.v2.MNQuery;
import org.dataone.service.mn.v2.MNView;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Ping;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Schedule;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.ServiceMethodRestriction;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.Synchronization;
import org.dataone.service.types.v1.util.AuthUtils;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.Property;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.speedbagit.SpeedBagException;
import org.dspace.foresite.OREException;
import org.dspace.foresite.OREParserException;
import org.dspace.foresite.ResourceMap;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents Metacat's implementation of the DataONE Member Node service API. Methods implement the
 * various MN* interfaces, and methods common to both Member Node and Coordinating Node interfaces
 * are found in the D1NodeService base class.
 *
 * Implements:
 * MNCore.ping()
 * MNCore.getLogRecords()
 * MNCore.getObjectStatistics()
 * MNCore.getOperationStatistics()
 * MNCore.getStatus()
 * MNCore.getCapabilities()
 * MNRead.get()
 * MNRead.getSystemMetadata()
 * MNRead.describe()
 * MNRead.getChecksum()
 * MNRead.listObjects()
 * MNRead.synchronizationFailed()
 * MNAuthorization.isAuthorized()
 * MNAuthorization.setAccessPolicy()
 * MNStorage.create()
 * MNStorage.update()
 * MNStorage.delete()
 * MNStorage.updateSystemMetadata()
 * MNReplication.replicate()
 * MNAdmin.reindex()
 */
public class MNodeService extends D1NodeService
    implements MNAuthorization, MNCore, MNRead, MNReplication, MNStorage, MNQuery, MNView,
    MNPackage {

    //private static final String PATHQUERY = "pathquery";
    public static final String UUID_SCHEME = "UUID";
    public static final String DOI_SCHEME = "DOI";
    private static final String UUID_PREFIX = "urn:uuid:";

    private static String XPATH_EML_ID = "/eml:eml/@packageId";

    /* the logger instance */
    private static org.apache.commons.logging.Log logMetacat =
        LogFactory.getLog(MNodeService.class);

    /* A reference to a Coordinating Node */
    private CNode cn;

    // shared executor
    private static ExecutorService executor = null;
    private static boolean enforcePublicEntirePackageInPublish = true;
    private boolean needSync = true;
    private static UpdateDOI doiUpdater = null;
    private static MetacatSolrIndex metacatSolrIndex = null;

    static {
        // use a shared executor service with nThreads == one less than available processors
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int nThreads = availableProcessors;
        nThreads--;
        nThreads = Math.max(1, nThreads);
        executor = Executors.newFixedThreadPool(nThreads);
        try {
            enforcePublicEntirePackageInPublish = Boolean.parseBoolean(
                PropertyService.getProperty("guid.doi.enforcePublicReadableEntirePackage"));
        } catch (Exception e) {
            logMetacat.warn("MNodeService.static - couldn't get the value since " + e.getMessage());
        }
        try {
            doiUpdater = new UpdateDOI();
        } catch (ServiceFailure e) {
            logMetacat.error("MNodeService.static - can't get the DOI updater " + e.getMessage());
        }
        try {
            metacatSolrIndex = MetacatSolrIndex.getInstance();
        } catch (Exception e) {
            logMetacat.error("MNodeService.static - can't get the MetacatSolrIndex object "
                                                                                + e.getMessage());
        }
    }


    /**
     * Get an instance of MNodeService.
     *
     * @return instance - the instance of MNodeService
     */
    public static MNodeService getInstance(HttpServletRequest request) {
        return new MNodeService(request);
    }

    /**
     * Get an instance of MNodeService.
     *
     * @param request   the servlet request associated with the MNodeService instance
     * @param ipAddress the ip address associated with the MNodeService instance
     * @param userAgent the user agent associated with the MNodeService instance
     * @return the instance of MNodeService
     */
    public static MNodeService getInstance(HttpServletRequest request, String ipAddress,
        String userAgent) {
        MNodeService mnService = new MNodeService(request);
        mnService.setIpAddress(ipAddress);
        mnService.setUserAgent(userAgent);
        return mnService;
    }

    /**
     * Constructor, private for singleton access
     */
    private MNodeService(HttpServletRequest request) {
        super(request);
        // set the Member Node certificate file location
        CertificateManager.getInstance().setCertificateLocation(
            Settings.getConfiguration().getString("D1Client.certificate.file"));

        try {
            needSync = Boolean.parseBoolean(
                PropertyService.getProperty("dataone.nodeSynchronize"));
        } catch (PropertyNotFoundException e) {
            // TODO Auto-generated catch block
            logMetacat.warn(
                "MNodeService.constructor : can't find the property to indicate if the memeber "
                    + "node need to be synchronized. It will use the default value - true.");
        }
    }

    /**
     * Deletes an object from the Member Node, where the object is either a data object or a science
     * metadata object.
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param id     - The object identifier to be deleted
     * @return pid - the identifier of the object used for the deletion
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    @Override
    public Identifier delete(Session session, Identifier id)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        String serviceFailureCode = "2902";
        String notFoundCode = "2901";
        String notAuthorizedCode = "2900";
        String invalidTokenCode = "2903";
        boolean needDeleteInfo = false;
        if (isReadOnlyMode()) {
            throw new ServiceFailure("2902", ReadOnlyChecker.DATAONEERROR);
        }

        try {
            Identifier HeadOfSid = getPIDForSID(id, serviceFailureCode);
            if (HeadOfSid != null) {
                id = HeadOfSid;
            }
            SystemMetadataManager.lock(id);
            SystemMetadata sysmeta = null;
            try {
                sysmeta =
                    getSystemMetadataForPID(id, serviceFailureCode, invalidTokenCode, notFoundCode,
                                            needDeleteInfo);
            } catch (InvalidRequest e) {
                throw new InvalidToken(invalidTokenCode, e.getMessage());
            }

            try {
                D1AuthHelper authDel =
                    new D1AuthHelper(request, id, notAuthorizedCode, serviceFailureCode);
                authDel.doAdminAuthorization(session);
            } catch (NotAuthorized na) {
                NotAuthorized na2 = new NotAuthorized(
                    notAuthorizedCode,
                    "The provided identity does not have permission to delete objects on the Node.");
                na2.initCause(na);
                throw na2;
            }

            try {
                String quotaSubject = request.getHeader(QuotaServiceManager.QUOTASUBJECTHEADER);
                QuotaServiceManager.getInstance()
                    .enforce(quotaSubject, session.getSubject(), sysmeta,
                             QuotaServiceManager.DELETEMETHOD);
            } catch (InsufficientResources e) {
                throw new ServiceFailure(serviceFailureCode,
                                         "The user doesn't have enough quota to delete the pid "
                                             + id.getValue() + " since " + e.getMessage());
            } catch (InvalidRequest e) {
                throw new InvalidToken("2903",
                                       "The quota service in the delete action has an invalid request - "
                                           + e.getMessage());
            }
            // defer to superclass implementation
            return super.delete(session.getSubject().getValue(), id);
        } finally {
            SystemMetadataManager.unLock(id);
        }
    }

    /**
     * Updates an existing object by creating a new object identified by newPid on the Member Node
     * which explicitly obsoletes the object identified by pid through appropriate changes to the
     * SystemMetadata of pid and newPid
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - The identifier of the object to be updated
     * @param object  - the new object bytes
     * @param sysmeta - the new system metadata describing the object
     * @return newPid - the identifier of the new object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws InvalidRequest
     */
    @Override
    public Identifier update(Session session, Identifier pid, InputStream object, Identifier newPid,
        SystemMetadata sysmeta)
        throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
        InsufficientResources, NotFound, InvalidSystemMetadata, NotImplemented, InvalidRequest {
        try {
            long startTime = System.currentTimeMillis();
            if (isReadOnlyMode()) {
                throw new ServiceFailure("1310", ReadOnlyChecker.DATAONEERROR);
            }
            //transform a sid to a pid if it is applicable
            String serviceFailureCode = "1310";
            Identifier sid = getPIDForSID(pid, serviceFailureCode);
            if (sid != null) {
                pid = sid;
            }
            String localId = null;
            boolean allowed = false;
            boolean isScienceMetadata = false;
            if (session == null) {
                throw new InvalidToken("1210", "No session has been provided");
            }
            Subject subject = session.getSubject();
            // verify the pid is valid format
            if (!isValidIdentifier(pid)) {
                throw new InvalidRequest("1202", "The provided identifier is invalid.");
            }
            // verify the new pid is valid format
            if (!isValidIdentifier(newPid)) {
                throw new InvalidRequest("1202", "The provided identifier is invalid.");
            }
            if (!isValidIdentifier(sysmeta.getIdentifier())) {
                throw new InvalidRequest("1202",
                    "The provided identifier on the system metadata is invalid.");
            }
            if (!newPid.equals(sysmeta.getIdentifier())) {
                throw new InvalidRequest("1202",
                    "The new identifier " + newPid.getValue() + " doesn't match the identifier "
                        + sysmeta.getIdentifier().getValue() + " in the system metadata.");
            }
            if (newPid.equals(pid)) {
                throw new InvalidRequest(
                    "1202",
                    "The new identifier " + newPid.getValue() + " cannot " + "update itself.");
            }

            if (sysmeta.getFormatId() == null || sysmeta.getFormatId().getValue().isBlank()) {
                throw new InvalidSystemMetadata(
                    "1300", "The the format id from the system metadata shouldn't be null.");
            }

            // lock existing pid
            SystemMetadataManager.lock(pid);
            // make sure that the newPid doesn't exists
            boolean idExists = true;
            try {
                idExists = IdentifierManager.getInstance().identifierExists(newPid.getValue());
            } catch (SQLException e) {
                throw new ServiceFailure("1310", "The requested identifier " + newPid.getValue()
                    + " couldn't be determined if it is unique since : " + e.getMessage());
            }
            if (idExists) {
                throw new IdentifierNotUnique("1220", "The requested identifier " + newPid.getValue()
                    + " is already used by another object and"
                    + "therefore can not be used for this object. Clients should choose"
                    + "a new identifier that is unique and retry the operation or "
                    + "use CN.reserveIdentifier() to reserve one.");
            }
            // check for the existing identifier
            try {
                localId = IdentifierManager.getInstance().getLocalId(pid.getValue());

            } catch (McdbDocNotFoundException e) {
                throw new InvalidRequest("1202",
                    "The object with the provided " + "identifier was not found.");
            } catch (SQLException ee) {
                throw new ServiceFailure("1310",
                    "The object with the provided " + "identifier " + pid.getValue()
                        + " can't be identified since - " + ee.getMessage());
            }
            long end = System.currentTimeMillis();
            logMetacat.debug(
                "MNodeService.update - the time spending on checking the validation of the old pid "
                    + pid.getValue() + " and the new pid " + newPid.getValue() + " is " + (end
                    - startTime) + " milli seconds.");
            // set the originating node
            NodeReference originMemberNode = this.getCapabilities().getIdentifier();
            sysmeta.setOriginMemberNode(originMemberNode);
            // set the submitter to match the certificate
            sysmeta.setSubmitter(subject);
            // set the dates
            Date now = Calendar.getInstance().getTime();
            sysmeta.setDateSysMetadataModified(now);
            sysmeta.setDateUploaded(now);
            // make sure serial version is set to something
            BigInteger serialVersion = sysmeta.getSerialVersion();
            if (serialVersion == null) {
                sysmeta.setSerialVersion(BigInteger.ZERO);
            }
            long startTime2 = System.currentTimeMillis();
            // does the subject have WRITE ( == update) priveleges on the pid?
            //CN having the permission is allowed; user with the write permission and calling on the
            // authoritative node is allowed.
            // get the existing system metadata for the object
            String invalidRequestCode = "1202";
            String notFoundCode = "1280";
            SystemMetadata existingSysMeta =
                getSystemMetadataForPID(pid, serviceFailureCode, invalidRequestCode, notFoundCode,
                    true);
            D1AuthHelper authDel = null;
            try {
                authDel = new D1AuthHelper(request, pid, "1200", "1310");
                //if the user has the change permission, it will be all set; otherwise, we need to
                // check more.
                authDel.doUpdateAuth(session, existingSysMeta, Permission.CHANGE_PERMISSION,
                    this.getCurrentNodeId());
                allowed = true;
            } catch (ServiceFailure e) {
                throw new ServiceFailure("1310",
                    "Can't determine if the client has the permission to update the object with id "
                        + pid.getValue() + " since " + e.getDescription());
            } catch (NotAuthorized e) {
                //the user doesn't have the change permission. However, if it has the write
                // permission and doesn't modify the access rules, Metacat still allows it to update
                // the object
                try {
                    authDel.doUpdateAuth(session, existingSysMeta, Permission.WRITE,
                        this.getCurrentNodeId());
                    //now the user has the write the permission. If the access rules in the new and
                    // old system metadata are the same, it is fine; otherwise, Metacat throws an
                    // exception
                    if (D1NodeService.isAccessControlDirty(sysmeta, existingSysMeta)) {
                        throw new NotAuthorized("1200",
                            "Can't update the object with id " + pid.getValue()
                                + " since the user try to change the access rules without the change "
                                + "permission: " + e.getDescription());
                    }
                    allowed = true;
                } catch (ServiceFailure ee) {
                    throw new ServiceFailure("1310",
                        "Can't determine if the client has the permission to update the object with id "
                            + pid.getValue() + " since " + ee.getDescription());
                } catch (NotAuthorized ee) {
                    throw new NotAuthorized("1200",
                        "Can't update the object with id " + pid.getValue() + " since "
                            + ee.getDescription());
                }
            }
            isInAllowList(session); //check if the session can upload objects to this instance
            end = System.currentTimeMillis();
            logMetacat.debug(
                "MNodeService.update - the time spending on checking if the user has the permission "
                    + "to update the old pid " + pid.getValue() + " with the new pid "
                    + newPid.getValue() + " is " + (end - startTime2) + " milli seconds.");
            if (allowed) {
                long startTime3 = System.currentTimeMillis();
                //check the if it has enough quota if th quota service is enabled
                String quotaSubject = request.getHeader(QuotaServiceManager.QUOTASUBJECTHEADER);
                QuotaServiceManager.getInstance().enforce(quotaSubject, session.getSubject(), sysmeta,
                    QuotaServiceManager.UPDATEMETHOD);
                // check quality of SM
                if (sysmeta.getObsoletedBy() != null) {
                    throw new InvalidSystemMetadata("1300",
                        "Cannot include obsoletedBy when updating object");
                }
                if (sysmeta.getObsoletes() != null && !sysmeta.getObsoletes().getValue()
                    .equals(pid.getValue())) {
                    throw new InvalidSystemMetadata("1300",
                        "The identifier provided in obsoletes does not match old Identifier");
                }
                //Base on documentation, we can't update an archived object:
                //The update operation MUST fail with Exceptions.InvalidRequest on objects that have
                // the Types.SystemMetadata.archived property set to true.
                if (existingSysMeta.getArchived() != null && existingSysMeta.getArchived()) {
                    throw new InvalidRequest("1202",
                        "An archived object" + pid.getValue() + " can't be updated");
                }
                // check for previous update
                // see: https://redmine.dataone.org/issues/3336
                Identifier existingObsoletedBy = existingSysMeta.getObsoletedBy();
                if (existingObsoletedBy != null) {
                    throw new InvalidRequest("1202",
                        "The previous identifier has already been made obsolete by: "
                            + existingObsoletedBy.getValue());
                }
                //check the if client change the authoritative member node.
                if (sysmeta.getAuthoritativeMemberNode() == null || sysmeta.getAuthoritativeMemberNode()
                    .getValue().trim().equals("") || sysmeta.getAuthoritativeMemberNode().getValue()
                    .equals("null")) {
                    sysmeta.setAuthoritativeMemberNode(originMemberNode);
                } else if (existingSysMeta.getAuthoritativeMemberNode() != null
                    && !sysmeta.getAuthoritativeMemberNode().getValue()
                    .equals(existingSysMeta.getAuthoritativeMemberNode().getValue())) {
                    throw new InvalidRequest("1202", "The previous authoriativeMemberNode is "
                        + existingSysMeta.getAuthoritativeMemberNode().getValue()
                        + " and new authoriativeMemberNode is " + sysmeta.getAuthoritativeMemberNode()
                        .getValue()
                        + ". They don't match. Clients don't have the permission to change it.");
                }
                end = System.currentTimeMillis();
                logMetacat.debug(
                    "MNodeService.update - the time spending on checking the quality of the system "
                        + "metadata of the old pid " + pid.getValue() + " and the new pid "
                        + newPid.getValue() + " is " + (end - startTime3) + " milli seconds.");
                //check the sid in the system metadata. If it exists, it should be non-exist or match
                // the old sid in the previous system metadata.
                Identifier sidInSys = sysmeta.getSeriesId();
                if (sidInSys != null) {
                    if (!isValidIdentifier(sidInSys)) {
                        throw new InvalidSystemMetadata("1300",
                            "The provided series id in the system metadata is invalid.");
                    }
                    Identifier previousSid = existingSysMeta.getSeriesId();
                    if (previousSid != null) {
                        // there is a previous sid, if the new sid doesn't match it, the new sid
                        // should be non-existing.
                        if (!sidInSys.getValue().equals(previousSid.getValue())) {
                            try {
                                idExists = IdentifierManager.getInstance()
                                    .identifierExists(sidInSys.getValue());
                            } catch (SQLException e) {
                                throw new ServiceFailure("1310",
                                    "The requested identifier " + sidInSys.getValue()
                                        + " couldn't be determined if it is unique since : "
                                        + e.getMessage());
                            }
                            if (idExists) {
                                throw new InvalidSystemMetadata("1300",
                                    "The series id " + sidInSys.getValue()
                                        + " in the system metadata doesn't match the previous series "
                                        + "id " + previousSid.getValue()
                                        + ", so it should NOT exist. However, it was used by another "
                                        + "object.");
                            }
                        }
                    } else {
                        // there is no previous sid, the new sid should be non-existing.
                        try {
                            idExists =
                                IdentifierManager.getInstance().identifierExists(sidInSys.getValue());
                        } catch (SQLException e) {
                            throw new ServiceFailure("1310",
                                "The requested identifier " + sidInSys.getValue()
                                    + " couldn't be determined if it is unique since : "
                                    + e.getMessage());
                        }
                        if (idExists) {
                            throw new InvalidSystemMetadata("1300",
                                "The series id " + sidInSys.getValue()
                                    + " in the system metadata should NOT exist since the previous "
                                    + "series id is null." + "However, it was used by another object.");
                        }
                    }
                    //the series id equals the pid (new pid hasn't been registered in the system, so
                    // IdentifierManager.getInstance().identifierExists method can't exclude this
                    // scenario)
                    if (sidInSys.getValue().equals(newPid.getValue())) {
                        throw new InvalidSystemMetadata("1300", "The series id " + sidInSys.getValue()
                            + " in the system metadata shouldn't have the same value of the pid.");
                    }
                }
                long end2 = System.currentTimeMillis();
                logMetacat.debug(
                    "MNodeService.update - the time spending on checking the sid validation of the "
                        + "old pid " + pid.getValue() + " and the new pid " + newPid.getValue() + " is "
                        + (end2 - end) + " milli seconds.");
                // prep the new system metadata, add pid to the affected lists
                sysmeta.setObsoletes(pid);
                isScienceMetadata = isScienceMetadata(sysmeta);
                // do we have XML metadata or a data object?
                try {
                    String docType;
                    if (isScienceMetadata) {
                        docType = sysmeta.getFormatId().getValue();
                    } else {
                        docType = DocumentImpl.BIN;
                    }
                    // update the object
                    // handler will register the object into DB, and save systemmetadata and bytes
                    // for the new object. The sysmeta of the obsoleted object will be stored as well.
                    // True means always change the modification date when it saves system metadata.
                    localId = handler.save(sysmeta, true, MetacatHandler.Action.UPDATE, docType,
                                           object, existingSysMeta, subject.getValue());
                } catch (IllegalArgumentException e) {
                    throw new InvalidRequest("1102", "An InvalidRequest in the create method - "
                                            + e.getMessage());
                } catch (NoSuchAlgorithmException e) {
                    throw new UnsupportedType("1140", "An UnsupportedType in the create method - "
                            + e.getMessage());
                } catch (IOException | IllegalAccessException |InvocationTargetException ioe) {
                    throw new ServiceFailure("1310", "Metacat cannot update " + pid.getValue()
                                            + " by " + newPid.getValue() + " : " + ioe.getMessage());
                }catch (McdbException e) {
                    throw new ServiceFailure("1310", "Metacat cannot save the object "
                                        + pid.getValue() + " since it cannot find it in validation "
                                        + e.getMessage());
                }
                long end3 = System.currentTimeMillis();
                logMetacat.debug(
                    "MNodeService.update - the time spending on saving the object with the new pid "
                        + newPid.getValue() + " is " + (end3 - end2) + " milli seconds.");
                // Index the obsoleted object
                try {
                    // Set isSysmetaChangeOnly true, set the followRevisions false
                    MetacatSolrIndex.getInstance()
                                .submit(existingSysMeta.getIdentifier(), existingSysMeta, true, false);
                } catch (Exception e) {
                    logMetacat.error("MNodeService.update - Failed to submit the index task for "
                                                   + existingSysMeta.getIdentifier().getValue()
                                                   + " since " + e.getMessage());
                }
                try {
                    // Submit the new object for indexing, set the followRevisions false
                    MetacatSolrIndex.getInstance().submit(sysmeta.getIdentifier(), sysmeta, false);
                } catch (Exception e) {
                    logMetacat.error("MNodeService.update - Failed to submit the index task for "
                            + sysmeta.getIdentifier().getValue()
                            + " since " + e.getMessage());
                }
                long end4 = System.currentTimeMillis();
                logMetacat.debug(
                    "MNodeService.update - the time spending on updating/saving system metadata  of "
                        + "the old pid " + pid.getValue() + " and the new pid " + newPid.getValue()
                        + " and saving the log information is " + (end4 - end3) + " milli seconds.");
                // attempt to register the identifier - it checks if it is a doi
                try {
                    DOIServiceFactory.getDOIService().registerDOI(sysmeta);
                } catch (Exception e) {
                    String message = "MNodeService.update - The object " + newPid.getValue()
                        + " has been saved successfully on Metacat. "
                        + " However, the new metadata can't be registered on the DOI service: "
                        + e.getMessage();
                    logMetacat.error(message);
                }
                try {
                    logMetacat.debug("Logging the update event.");
                    EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"),
                                               session.getSubject().getValue(), localId, "update");
                } catch (Exception e) {
                    logMetacat.warn(
                        "D1NodeService.update - can't log the update event for the object "
                            + pid.getValue());
                }
                long end5 = System.currentTimeMillis();
                logMetacat.debug(
                    "MNodeService.update - the time spending on registering the doi (if it is doi ) "
                        + "of the new pid " + newPid.getValue() + " is " + (end5 - end4)
                        + " milli seconds.");

            } else {
                throw new NotAuthorized("1200", "The provided identity does not have "
                    + "permission to UPDATE the object identified by " + pid.getValue()
                    + " on the Member Node.");
            }

            long end6 = System.currentTimeMillis();
            logMetacat.debug(
                "MNodeService.update - the total time of updating the old pid " + pid.getValue()
                    + " whth the new pid " + newPid.getValue() + " is " + (end6 - startTime)
                    + " milli seconds.");
            return newPid;
        } catch (Exception fle) {
            // Metacat needs to delete object from hashstore
            try {
                // Metacat stores the object based on the pid in the system metadata,
                // so it deletes from there.
                if (sysmeta != null && sysmeta.getIdentifier() != null) {
                    MetacatInitializer.getStorage().deleteObject(sysmeta.getIdentifier());
                }
            } catch (Exception de) {
                logMetacat.error("Metacat couldn't delete the object "
                                + sysmeta.getIdentifier().getValue() + " since " + de.getMessage());
            }
            throw fle;
        } finally {
            SystemMetadataManager.unLock(pid);
        }
    }

    @Override
    public Identifier create(Session session, Identifier pid, InputStream object,
        SystemMetadata sysmeta)
        throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
        InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest {
        try {
            if (isReadOnlyMode()) {
                throw new ServiceFailure("1190", ReadOnlyChecker.DATAONEERROR);
            }
            // check for null session
            if (session == null) {
                throw new InvalidToken("1110", "Session is required to WRITE the objects to the Node.");
            }
            if (sysmeta == null) {
                throw new InvalidRequest("1102", "The system metadata object is null.");
            }
            // verify the pid is valid format
            if (!isValidIdentifier(pid)) {
                throw new InvalidRequest("1102", "The provided identifier is invalid.");
            }
            objectExists(pid);
            // set the submitter to match the certificate
            sysmeta.setSubmitter(session.getSubject());
            // set the originating node
            NodeReference originMemberNode = this.getCapabilities().getIdentifier();
            sysmeta.setOriginMemberNode(originMemberNode);
            // if no authoritative MN, set it to the same
            if (sysmeta.getAuthoritativeMemberNode() == null || sysmeta.getAuthoritativeMemberNode()
                .getValue().trim().equals("") || sysmeta.getAuthoritativeMemberNode().getValue()
                .equals("null")) {
                sysmeta.setAuthoritativeMemberNode(originMemberNode);
            }
            sysmeta.setArchived(false);
            // set the dates
            Date now = Calendar.getInstance().getTime();
            sysmeta.setDateSysMetadataModified(now);
            sysmeta.setDateUploaded(now);
            // set the serial version
            sysmeta.setSerialVersion(BigInteger.ZERO);
            // check that we are not attempting to subvert versioning
            if (sysmeta.getObsoletes() != null && sysmeta.getObsoletes().getValue() != null) {
                throw new InvalidSystemMetadata("1180", "The supplied system metadata is invalid. "
                    + "The obsoletes field cannot have a value when creating entries.");
            }
            if (sysmeta.getObsoletedBy() != null && sysmeta.getObsoletedBy().getValue() != null) {
                throw new InvalidSystemMetadata("1180", "The supplied system metadata is invalid. "
                    + "The obsoletedBy field cannot have a value when creating entries.");
            }
            // verify the sid in the system metadata
            Identifier sid = sysmeta.getSeriesId();
            boolean idExists = false;
            if (sid != null) {
                if (!isValidIdentifier(sid)) {
                    throw new InvalidSystemMetadata("1180", "The provided series id is invalid.");
                }
                try {
                    idExists = IdentifierManager.getInstance().identifierExists(sid.getValue());
                } catch (SQLException e) {
                    throw new ServiceFailure("1190", "The series identifier " + sid.getValue()
                        + " in the system metadata couldn't be determined if it is unique since : "
                        + e.getMessage());
                }
                if (idExists) {
                    throw new InvalidSystemMetadata("1180", "The series identifier " + sid.getValue()
                        + " is already used by another object and "
                        + "therefore can not be used for this object. Clients should choose"
                        + " a new identifier that is unique and retry the operation or "
                        + "use CN.reserveIdentifier() to reserve one.");
                }
                //the series id equals the pid (new pid hasn't been registered in the system, so
                // IdentifierManager.getInstance().identifierExists method can't exclude this scenario )
                if (sid.getValue().equals(pid.getValue())) {
                    throw new InvalidSystemMetadata("1180", "The series id " + sid.getValue()
                        + " in the system metadata shouldn't have the same value of the pid.");
                }
            }
            boolean allowed = false;
            try {
                allowed = isAuthorized(session, pid, Permission.WRITE);
            } catch (NotFound e) {
                // The identifier doesn't exist, writing should be fine.
                allowed = true;
            }
            if (!allowed) {
                throw new NotAuthorized("1100",
                    "Provited Identity doesn't have the WRITE permission on the pid " + pid.getValue());
            }
            logMetacat.debug("Allowed to create: " + pid.getValue());
            //check the if it has enough quota if th quota service is enabled
            String quotaSubject = request.getHeader(QuotaServiceManager.QUOTASUBJECTHEADER);
            try {
                QuotaServiceManager.getInstance().enforce(quotaSubject, session.getSubject(), sysmeta,
                    QuotaServiceManager.CREATEMETHOD);
            } catch (NotFound e) {
                throw new InvalidRequest("1102", "Can't find the resource " + e.getMessage());
            }
            // call the shared impl
            boolean changeModificationDate = true;
            Identifier resultPid = super.create(session, pid, object, sysmeta, changeModificationDate);
            // attempt to register the identifier - it checks if it is a doi
            try {
                DOIServiceFactory.getDOIService().registerDOI(sysmeta);
            } catch (Exception e) {
                String message = "MNodeService.create - The object " + pid.getValue()
                    + " has been created successfully on Metacat."
                    + " However, the metadata can't be registered on the DOI service: "
                    + e.getMessage();
                logMetacat.error(message);
            }
            // return
            return resultPid;
        } catch (Exception e) {
            // Metacat needs to delete object from hashstore
            try {
                // Metacat stores the object based on the pid in the system metadata,
                // so it deletes from there.
                if (sysmeta != null && sysmeta.getIdentifier() != null) {
                    MetacatInitializer.getStorage().deleteObject(sysmeta.getIdentifier());
                }
            } catch (Exception ee) {
                logMetacat.error("Metacat couldn't delete the object "
                                + sysmeta.getIdentifier().getValue() + " since " + ee.getMessage());
            }
            throw e;
        }
    }

    /**
     * Called by a Coordinating Node to request that the Member Node create a copy of the specified
     * object by retrieving it from another Member Node and storing it locally so that it can be
     * made accessible to the DataONE system.
     *
     * @param session    - the Session object containing the credentials for the Subject
     * @param sysmeta    - Copy of the CN held system metadata for the object
     * @param sourceNode - A reference to node from which the content should be retrieved. The
     *                   reference should be resolved by checking the CN node registry.
     * @return true if the replication succeeds
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidRequest
     */
    @Override
    public boolean replicate(Session session, SystemMetadata sysmeta, NodeReference sourceNode)
        throws NotImplemented, ServiceFailure, NotAuthorized, InvalidRequest, InsufficientResources,
        UnsupportedType {

        if (session != null && sysmeta != null && sourceNode != null) {
            logMetacat.info(
                "MNodeService.replicate() called with parameters: \n" + "\tSession.Subject      = "
                    + session.getSubject().getValue() + "\n" + "\tidentifier           = "
                    + sysmeta.getIdentifier().getValue() + "\n" + "\tSource NodeReference ="
                    + sourceNode.getValue());
        } else {
            throw new InvalidRequest("2153",
                "The provided session or systemmetdata or sourceNode should NOT be null.");
        }
        boolean result = false;
        String nodeIdStr = null;
        NodeReference nodeId = null;

        // get the referenced object
        Identifier pid = sysmeta.getIdentifier();
        // verify the pid is valid format
        if (!isValidIdentifier(pid)) {
            throw new InvalidRequest("2153",
                "The provided identifier in the system metadata is invalid.");
        }

        if (!NodeReplicationPolicyChecker.check(sourceNode, sysmeta)) {
            throw new InvalidRequest("2153",
                "The object " + pid.getValue() + " from sourceNode" + sourceNode.getValue()
                    + " is not allowed to replicate to this node based on the node replication "
                    + "policy.");
        }

        // get from the membernode
        // TODO: switch credentials for the server retrieval?

        InputStream object = null;
        Session thisNodeSession = null;
        SystemMetadata localSystemMetadata = null;
        BaseException failure = null;
        String localId = null;

        // TODO: check credentials
        // cannot be called by public
        if (session == null || session.getSubject() == null) {
            String msg =
                "No session was provided to replicate identifier " + sysmeta.getIdentifier()
                    .getValue();
            logMetacat.error(msg);
            throw new NotAuthorized("2152", msg);
        }

        // only allow cns call this method
        D1AuthHelper authDel = new D1AuthHelper(request, sysmeta.getIdentifier(), "2152", "2151");
        authDel.doCNOnlyAuthorization(session);


        logMetacat.debug("Allowed to replicate: " + pid.getValue());

        // get the local node id
        try {
            nodeIdStr = PropertyService.getProperty("dataone.nodeId");
            nodeId = new NodeReference();
            nodeId.setValue(nodeIdStr);

        } catch (PropertyNotFoundException e1) {
            String msg = "Couldn't get dataone.nodeId property: " + e1.getMessage();
            failure = new ServiceFailure("2151", msg);
            //setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.FAILED, failure);
            logMetacat.error(msg);
            //return true;
            throw new ServiceFailure("2151", msg);

        }

        try {
            try {
                // do we already have a replica?
                try {
                    localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
                    ObjectFormat objectFormat = null;
                    String type = null;
                    try {
                        objectFormat =
                            ObjectFormatCache.getInstance().getFormat(sysmeta.getFormatId());
                    } catch (BaseException be) {
                        logMetacat.warn(
                            "MNodeService.getReplica - Could not lookup ObjectFormat for: "
                                + sysmeta.getFormatId(), be);
                    }
                    if (objectFormat != null) {
                        type = objectFormat.getFormatType();
                    }
                    logMetacat.info(
                        "MNodeService.getReplica - the data type for the object " + pid.getValue()
                            + " is " + type);
                    // if we have a local id, get the local object
                    try {
                        object = MetacatHandler.read(localId, type);
                    } catch (Exception e) {
                        // NOTE: we may already know about this ID because it could be a data
                        // file described by a metadata file
                        // https://redmine.dataone.org/issues/2572
                        // TODO: fix this so that we don't prevent ourselves from getting replicas

                        // let the CN know that the replication failed
                        logMetacat.warn(
                            "Object content not found on this node despite having localId: "
                                + localId);
                        String msg = "Can't read the object bytes properly, replica is invalid.";
                        ServiceFailure serviceFailure = new ServiceFailure("2151", msg);
                        setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.FAILED,
                            serviceFailure);
                        logMetacat.warn(msg);
                        throw serviceFailure;

                    }

                } catch (McdbDocNotFoundException e) {
                    logMetacat.info("No replica found. Continuing.");

                } catch (SQLException ee) {
                    throw new ServiceFailure("2151",
                        "Couldn't identify the local id of the object with the specified "
                            + "identifier " + pid.getValue() + " since - " + ee.getMessage());
                }

                // no local replica, get a replica
                if (object == null) {
                    D1NodeVersionChecker checker = new D1NodeVersionChecker(sourceNode);
                    String nodeVersion = checker.getVersion("MNRead");
                    if (nodeVersion != null && nodeVersion.equals(D1NodeVersionChecker.V1)) {
                        //The source node is a v1 node, we use the v1 api
                        org.dataone.client.v1.MNode mNodeV1 =
                            org.dataone.client.v1.itk.D1Client.getMN(sourceNode);
                        object = mNodeV1.getReplica(thisNodeSession, pid);
                    } else if (nodeVersion != null && nodeVersion.equals(D1NodeVersionChecker.V2)) {
                        // session should be null to use the default certificate
                        // location set in the Certificate manager
                        MNode mn = D1Client.getMN(sourceNode);
                        object = mn.getReplica(thisNodeSession, pid);
                    } else {
                        throw new ServiceFailure("2151",
                            "The version of MNRead service is " + nodeVersion
                                + " in the source node " + sourceNode.getValue()
                                + " and it is not supported. Please check the information in the "
                                + "cn");
                    }

                    logMetacat.info(
                        "MNodeService.getReplica() called for identifier " + pid.getValue());

                }

            } catch (InvalidToken e) {
                String msg =
                    "Could not retrieve object to replicate (InvalidToken): " + e.getMessage();
                failure = new ServiceFailure("2151", msg);
                setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.FAILED,
                    failure);
                logMetacat.error(msg);
                throw new ServiceFailure("2151", msg);

            } catch (NotFound e) {
                String msg = "Could not retrieve object to replicate (NotFound): " + e.getMessage();
                failure = new ServiceFailure("2151", msg);
                setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.FAILED,
                    failure);
                logMetacat.error(msg);
                throw new ServiceFailure("2151", msg);

            } catch (NotAuthorized e) {
                String msg =
                    "Could not retrieve object to replicate (NotAuthorized): " + e.getMessage();
                failure = new ServiceFailure("2151", msg);
                setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.FAILED,
                    failure);
                logMetacat.error(msg);
                throw new ServiceFailure("2151", msg);
            } catch (NotImplemented e) {
                String msg =
                    "Could not retrieve object to replicate (mn.getReplica NotImplemented): "
                        + e.getMessage();
                failure = new ServiceFailure("2151", msg);
                setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.FAILED,
                    failure);
                logMetacat.error(msg);
                throw new ServiceFailure("2151", msg);
            } catch (ServiceFailure e) {
                String msg =
                    "Could not retrieve object to replicate (ServiceFailure): " + e.getMessage();
                failure = new ServiceFailure("2151", msg);
                setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.FAILED,
                    failure);
                logMetacat.error(msg);
                throw new ServiceFailure("2151", msg);
            } catch (InsufficientResources e) {
                String msg = "Could not retrieve object to replicate (InsufficientResources): "
                    + e.getMessage();
                failure = new ServiceFailure("2151", msg);
                setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.FAILED,
                    failure);
                logMetacat.error(msg);
                throw new ServiceFailure("2151", msg);
            }

            // verify checksum on the object, if supported
            logMetacat.info(
                "MNodeService.replicate - the class of object inputstream is " + object.getClass()
                    .getCanonicalName() + ". Does it support the reset method? The answer is "
                    + object.markSupported());

            // add it to local store
            Identifier retPid;
            try {
                // skip the MN.create -- this mutates the system metadata and we don't want it to
                if (localId == null) {
                    // TODO: this will fail if we already "know" about the identifier
                    // FIXME: see https://redmine.dataone.org/issues/2572
                    objectExists(pid);
                    boolean changedModificationDate = false;
                    // Store the input stream into hash store
                    ObjectInfo info = storeData(MetacatInitializer.getStorage(), object, sysmeta);
                    MCSystemMetadata mcSysMeta = new MCSystemMetadata();
                    MCSystemMetadata.copy(mcSysMeta, sysmeta);
                    mcSysMeta.setChecksums(info.hexDigests());
                    retPid = super.create(session, pid, object, mcSysMeta, changedModificationDate);
                    result = (retPid.getValue().equals(pid.getValue()));
                }

            } catch (Exception e) {
                String msg =
                    "Could not save object to local store (" + e.getClass().getName() + "): "
                        + e.getMessage();
                failure = new ServiceFailure("2151", msg);
                setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.FAILED,
                    failure);
                logMetacat.error(msg);
                throw new ServiceFailure("2151", msg);

            }
        } finally {
            IOUtils.closeQuietly(object);
        }


        // finish by setting the replication status
        setReplicationStatus(thisNodeSession, pid, nodeId, ReplicationStatus.COMPLETED, null);
        return result;

    }


    /**
     * Return the object identified by the given object identifier
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - the object identifier for the given object
     * @return inputStream - the input stream of the given object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    @Override
    public InputStream get(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        return super.get(session, pid);

    }

    /**
     * Returns a Checksum for the specified object using an accepted hashing algorithm
     *
     * @param session   - the Session object containing the credentials for the Subject
     * @param pid       - the object identifier for the given object
     * @param algorithm -  the name of an algorithm that will be used to compute a checksum of the
     *                  bytes of the object
     * @return checksum - the checksum of the given object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    @Override
    public Checksum getChecksum(Session session, Identifier pid, String algorithm)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, InvalidRequest,
        NotImplemented {

        Checksum checksum = null;
        String serviceFailure = "1410";
        String notFound = "1420";
        //Checkum only handles the pid, not sid
        checkV1SystemMetaPidExist(pid, serviceFailure,
            "The checksum for the object specified by " + pid.getValue() + " couldn't be returned ",
            notFound,
            "The object specified by " + pid.getValue() + " does not exist at this node.");
        InputStream inputStream = get(session, pid);

        try {
            checksum = ChecksumUtil.checksum(inputStream, algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceFailure("1410",
                "The checksum for the object specified by " + pid.getValue()
                    + "could not be returned due to an internal error: " + e.getMessage());
        } catch (IOException e) {
            throw new ServiceFailure("1410",
                "The checksum for the object specified by " + pid.getValue()
                    + "could not be returned due to an internal error: " + e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    logMetacat.warn(
                        "MNodeService.getChecksum - can't close the input stream which got the "
                            + "object content since " + e.getMessage());
                }
            }
        }

        if (checksum == null) {
            throw new ServiceFailure("1410",
                "The checksum for the object specified by " + pid.getValue()
                    + "could not be returned.");
        }

        return checksum;
    }

    /**
     * Return the system metadata for a given object
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - the object identifier for the given object
     * @return inputStream - the input stream of the given system metadata object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    @Override
    public SystemMetadata getSystemMetadata(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        return super.getSystemMetadata(session, pid);
    }

    /**
     * Retrieve the list of objects present on the MN that match the calling parameters
     *
     * @param session       - the Session object containing the credentials for the Subject
     * @param startTime     - Specifies the beginning of the time range from which to return object
     *                      (>=)
     * @param endTime       - Specifies the beginning of the time range from which to return object
     *                      (>=)
     * @param objectFormatId  - Restrict results to the specified object format
     * @param replicaStatus - Indicates if replicated objects should be returned in the list
     * @param identifier    - identifier
     * @param start         - The zero-based index of the first value, relative to the first record
     *                      of the resultset that matches the parameters.
     * @param count         - The maximum number of entries that should be returned in the response.
     *                      The Member Node may return less entries than specified in this value.
     * @return objectList - the list of objects matching the criteria
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    @Override
    public ObjectList listObjects(Session session, Date startTime, Date endTime,
        ObjectFormatIdentifier objectFormatId, Identifier identifier, Boolean replicaStatus,
        Integer start, Integer count)
        throws NotAuthorized, InvalidRequest, NotImplemented, ServiceFailure, InvalidToken {
        NodeReference nodeId = null;
        if (!replicaStatus) {
            //not include those objects whose authoritative node is not this mn
            nodeId = new NodeReference();
            try {
                String currentNodeId = PropertyService.getInstance()
                    .getProperty("dataone.nodeId"); // return only pids for which this mn
                nodeId.setValue(currentNodeId);
            } catch (Exception e) {
                throw new ServiceFailure("1580", e.getMessage());
            }
        }
        return super.listObjects(session, startTime, endTime, objectFormatId, identifier, nodeId,
            start, count);
    }

    /**
     * Return a description of the node's capabilities and services.
     *
     * @return node - the technical capabilities of the Member Node
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented - not thrown by this implementation
     */
    @Override
    public Node getCapabilities() throws ServiceFailure {

        String nodeName = null;
        String nodeId = null;
        String subject = null;
        String contactSubject = null;
        String nodeDesc = null;
        String nodeTypeString = null;
        NodeType nodeType = null;
        List<String> mnCoreServiceVersions = null;
        List<String> mnReadServiceVersions = null;
        List<String> mnAuthorizationServiceVersions = null;
        List<String> mnStorageServiceVersions = null;
        List<String> mnReplicationServiceVersions = null;
        List<String> mnPackageServiceVersions = null;
        List<String> mnQueryServiceVersions = null;
        List<String> mnViewServiceVersions = null;

        boolean nodeSynchronize = false;
        boolean nodeReplicate = false;
        List<String> mnCoreServiceAvailables = null;
        List<String> mnReadServiceAvailables = null;
        List<String> mnAuthorizationServiceAvailables = null;
        List<String> mnStorageServiceAvailables = null;
        List<String> mnReplicationServiceAvailables = null;
        List<String> mnPackageServiceAvailables = null;
        List<String> mnQueryServiceAvailables = null;
        List<String> mnViewServiceAvailables = null;
        Vector<String> allowedSubmitters = null;

        try {
            // get the properties of the node based on configuration information
            nodeName = Settings.getConfiguration().getString("dataone.nodeName");
            nodeId = Settings.getConfiguration().getString("dataone.nodeId");
            subject = Settings.getConfiguration().getString("dataone.subject");
            contactSubject = Settings.getConfiguration().getString("dataone.contactSubject");
            nodeDesc = Settings.getConfiguration().getString("dataone.nodeDescription");
            nodeTypeString = Settings.getConfiguration().getString("dataone.nodeType");
            nodeType = NodeType.convert(nodeTypeString);
            nodeSynchronize = Boolean.parseBoolean(
                                Settings.getConfiguration().getString("dataone.nodeSynchronize"));
            nodeReplicate = Boolean.parseBoolean(
                                Settings.getConfiguration().getString("dataone.nodeReplicate"));
            allowedSubmitters = AuthUtil.getAllowedSubmitters();

            // Set the properties of the node based on configuration information and
            // calls to current status methods
            String serviceName = SystemUtil.getContextURL() + "/" + PropertyService.getProperty(
                "dataone.serviceName");
            Node node = new Node();
            node.setBaseURL(serviceName + "/" + nodeTypeString);
            node.setDescription(nodeDesc);

            // set the node's health information
            node.setState(NodeState.UP);

            // set the ping response to the current value
            Ping canPing = new Ping();
            canPing.setSuccess(false);
            try {
                Date pingDate = ping();
                canPing.setSuccess(pingDate != null);
            } catch (BaseException e) {
                logMetacat.warn("MNodeService.getCapabilities - can't set the ping date since "
                                        + e.getMessage());
                // guess it can't be pinged
            }

            node.setPing(canPing);

            NodeReference identifier = new NodeReference();
            identifier.setValue(nodeId);
            node.setIdentifier(identifier);
            Subject s = new Subject();
            s.setValue(subject);
            node.addSubject(s);
            Subject contact = new Subject();
            contact.setValue(contactSubject);
            node.addContactSubject(contact);
            node.setName(nodeName);
            node.setReplicate(nodeReplicate);
            node.setSynchronize(nodeSynchronize);

            // services: MNAuthorization, MNCore, MNRead, MNReplication, MNStorage
            Services services = new Services();

            mnCoreServiceVersions =
                Settings.getConfiguration().getList("dataone.mnCore.serviceVersion");
            mnCoreServiceAvailables =
                Settings.getConfiguration().getList("dataone.mnCore.serviceAvailable");
            if (mnCoreServiceVersions != null && mnCoreServiceAvailables != null
                && mnCoreServiceVersions.size() == mnCoreServiceAvailables.size()) {
                for (int i = 0; i < mnCoreServiceVersions.size(); i++) {
                    String version = mnCoreServiceVersions.get(i);
                    boolean available = Boolean.parseBoolean(mnCoreServiceAvailables.get(i));
                    Service sMNCore = new Service();
                    sMNCore.setName("MNCore");
                    sMNCore.setVersion(version);
                    sMNCore.setAvailable(available);
                    services.addService(sMNCore);
                }
            }

            mnReadServiceVersions =
                Settings.getConfiguration().getList("dataone.mnRead.serviceVersion");
            mnReadServiceAvailables =
                Settings.getConfiguration().getList("dataone.mnRead.serviceAvailable");
            if (mnReadServiceVersions != null && mnReadServiceAvailables != null
                && mnReadServiceVersions.size() == mnReadServiceAvailables.size()) {
                for (int i = 0; i < mnReadServiceVersions.size(); i++) {
                    String version = mnReadServiceVersions.get(i);
                    boolean available = Boolean.parseBoolean(mnReadServiceAvailables.get(i));
                    Service sMNRead = new Service();
                    sMNRead.setName("MNRead");
                    sMNRead.setVersion(version);
                    sMNRead.setAvailable(available);
                    services.addService(sMNRead);
                }
            }

            mnAuthorizationServiceVersions =
                Settings.getConfiguration().getList("dataone.mnAuthorization.serviceVersion");
            mnAuthorizationServiceAvailables =
                Settings.getConfiguration().getList("dataone.mnAuthorization.serviceAvailable");
            if (mnAuthorizationServiceVersions != null && mnAuthorizationServiceAvailables != null
                && mnAuthorizationServiceVersions.size()
                == mnAuthorizationServiceAvailables.size()) {
                for (int i = 0; i < mnAuthorizationServiceVersions.size(); i++) {
                    String version = mnAuthorizationServiceVersions.get(i);
                    boolean available =
                                    Boolean.parseBoolean(mnAuthorizationServiceAvailables.get(i));
                    Service sMNAuthorization = new Service();
                    sMNAuthorization.setName("MNAuthorization");
                    sMNAuthorization.setVersion(version);
                    sMNAuthorization.setAvailable(available);
                    services.addService(sMNAuthorization);
                }
            }

            mnStorageServiceVersions =
                Settings.getConfiguration().getList("dataone.mnStorage.serviceVersion");
            mnStorageServiceAvailables =
                Settings.getConfiguration().getList("dataone.mnStorage.serviceAvailable");
            if (mnStorageServiceVersions != null && mnStorageServiceAvailables != null
                && mnStorageServiceVersions.size() == mnStorageServiceAvailables.size()) {
                for (int i = 0; i < mnStorageServiceVersions.size(); i++) {
                    String version = mnStorageServiceVersions.get(i);
                    boolean available = Boolean.parseBoolean(mnStorageServiceAvailables.get(i));
                    Service sMNStorage = new Service();
                    sMNStorage.setName("MNStorage");
                    sMNStorage.setVersion(version);
                    sMNStorage.setAvailable(available);
                    if (allowedSubmitters != null && !allowedSubmitters.isEmpty()) {
                        ServiceMethodRestriction createRestriction = new ServiceMethodRestriction();
                        createRestriction.setMethodName("create");
                        ServiceMethodRestriction updateRestriction = new ServiceMethodRestriction();
                        updateRestriction.setMethodName("update");
                        for (int j = 0; j < allowedSubmitters.size(); j++) {
                            Subject allowedSubject = new Subject();
                            allowedSubject.setValue(allowedSubmitters.elementAt(j));
                            createRestriction.addSubject(allowedSubject);
                            updateRestriction.addSubject(allowedSubject);
                        }
                        sMNStorage.addRestriction(createRestriction);
                        sMNStorage.addRestriction(updateRestriction);
                    }
                    services.addService(sMNStorage);
                }
            }

            mnReplicationServiceVersions =
                Settings.getConfiguration().getList("dataone.mnReplication.serviceVersion");
            mnReplicationServiceAvailables =
                Settings.getConfiguration().getList("dataone.mnReplication.serviceAvailable");
            if (mnReplicationServiceVersions != null && mnReplicationServiceAvailables != null
                && mnReplicationServiceVersions.size() == mnReplicationServiceAvailables.size()) {
                for (int i = 0; i < mnReplicationServiceVersions.size(); i++) {
                    String version = mnReplicationServiceVersions.get(i);
                    boolean available = Boolean.parseBoolean(mnReplicationServiceAvailables.get(i));
                    Service sMNReplication = new Service();
                    sMNReplication.setName("MNReplication");
                    sMNReplication.setVersion(version);
                    sMNReplication.setAvailable(available);
                    services.addService(sMNReplication);
                }
            }

            mnPackageServiceVersions =
                Settings.getConfiguration().getList("dataone.mnPackage.serviceVersion");
            mnPackageServiceAvailables =
                Settings.getConfiguration().getList("dataone.mnPackage.serviceAvailable");
            if (mnPackageServiceVersions != null && mnPackageServiceAvailables != null
                && mnPackageServiceVersions.size() == mnPackageServiceAvailables.size()) {
                for (int i = 0; i < mnPackageServiceVersions.size(); i++) {
                    String version = mnPackageServiceVersions.get(i);
                    boolean available = Boolean.parseBoolean(mnPackageServiceAvailables.get(i));
                    Service sMNPakcage = new Service();
                    sMNPakcage.setName("MNPackage");
                    sMNPakcage.setVersion(version);
                    sMNPakcage.setAvailable(available);
                    services.addService(sMNPakcage);
                }
            }

            mnQueryServiceVersions =
                Settings.getConfiguration().getList("dataone.mnQuery.serviceVersion");
            mnQueryServiceAvailables =
                Settings.getConfiguration().getList("dataone.mnQuery.serviceAvailable");
            if (mnQueryServiceVersions != null && mnQueryServiceAvailables != null
                && mnQueryServiceVersions.size() == mnQueryServiceAvailables.size()) {
                for (int i = 0; i < mnQueryServiceVersions.size(); i++) {
                    String version = mnQueryServiceVersions.get(i);
                    boolean available = Boolean.parseBoolean(mnQueryServiceAvailables.get(i));
                    Service sMNQuery = new Service();
                    sMNQuery.setName("MNQuery");
                    sMNQuery.setVersion(version);
                    sMNQuery.setAvailable(available);
                    services.addService(sMNQuery);
                }
            }

            mnViewServiceVersions =
                Settings.getConfiguration().getList("dataone.mnView.serviceVersion");
            mnViewServiceAvailables =
                Settings.getConfiguration().getList("dataone.mnView.serviceAvailable");
            if (mnViewServiceVersions != null && mnViewServiceAvailables != null
                && mnViewServiceVersions.size() == mnViewServiceAvailables.size()) {
                for (int i = 0; i < mnViewServiceVersions.size(); i++) {
                    String version = mnViewServiceVersions.get(i);
                    boolean available = Boolean.parseBoolean(mnViewServiceAvailables.get(i));
                    Service sMNView = new Service();
                    sMNView.setName("MNView");
                    sMNView.setVersion(version);
                    sMNView.setAvailable(available);
                    services.addService(sMNView);
                }
            }

            node.setServices(services);

            // Set the schedule for synchronization
            Synchronization synchronization = new Synchronization();
            Schedule schedule = new Schedule();
            Date now = new Date();
            schedule.setYear(
                PropertyService.getProperty("dataone.nodeSynchronization.schedule.year"));
            schedule.setMon(
                PropertyService.getProperty("dataone.nodeSynchronization.schedule.mon"));
            schedule.setMday(
                PropertyService.getProperty("dataone.nodeSynchronization.schedule.mday"));
            schedule.setWday(
                PropertyService.getProperty("dataone.nodeSynchronization.schedule.wday"));
            schedule.setHour(
                PropertyService.getProperty("dataone.nodeSynchronization.schedule.hour"));
            schedule.setMin(
                PropertyService.getProperty("dataone.nodeSynchronization.schedule.min"));
            schedule.setSec(
                PropertyService.getProperty("dataone.nodeSynchronization.schedule.sec"));
            synchronization.setSchedule(schedule);
            synchronization.setLastHarvested(now);
            synchronization.setLastCompleteHarvest(now);
            node.setSynchronization(synchronization);

            node.setType(nodeType);

            //add properties such as the Metacat version and upgrade status
            String upgradeStatus =
                Settings.getConfiguration().getString("configutil.upgrade.status");
            if (upgradeStatus != null && !upgradeStatus.trim().equals("")) {
                Property statusProperty = new Property();
                statusProperty.setKey("upgrade_status");
                statusProperty.setValue(upgradeStatus);
                node.addProperty(statusProperty);
            }
            try {
                String metacatVersion = MetacatVersion.getVersionFromDB();
                if (metacatVersion != null && !metacatVersion.trim().equals("")) {
                    Property versionProperty = new Property();
                    versionProperty.setKey("metacat_version");
                    versionProperty.setValue(metacatVersion);
                    node.addProperty(versionProperty);
                }
            } catch (SQLException e) {
                logMetacat.warn(
                    "MNodeService.getCapabilities - couldn't get the metacat version since "
                        + e.getMessage());
            }

            //add the property of read-only mode
            String readOnlyStatus = Settings.getConfiguration().getString("application.readOnlyMode");
            if (readOnlyStatus != null && !readOnlyStatus.trim().equals("")) {
                Property readOnlyStatusProperty = new Property();
                readOnlyStatusProperty.setKey("read_only_mode");
                readOnlyStatusProperty.setValue(readOnlyStatus);
                node.addProperty(readOnlyStatusProperty);
            }

            return node;

        } catch (PropertyNotFoundException pnfe) {
            String msg =
                "MNodeService.getCapabilities(): " + "property not found: " + pnfe.getMessage();
            logMetacat.error(msg);
            throw new ServiceFailure("2162", msg);
        } catch (MetacatUtilException me) {
            String msg =
                "MNodeService.getCapabilities(): " + "can't get the allowed submitters list since "
                    + me.getMessage();
            logMetacat.error(msg);
            throw new ServiceFailure("2162", msg);
        }
    }



    /**
     * A callback method used by a CN to indicate to a MN that it cannot complete synchronization of
     * the science metadata identified by pid.  Log the event in the metacat event log.
     *
     * @param session
     * @param syncFailed
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotImplemented
     */
    @Override
    public boolean synchronizationFailed(Session session, SynchronizationFailed syncFailed)
        throws NotImplemented, ServiceFailure, NotAuthorized {

        String localId;

        if (syncFailed.getPid() == null) {
            throw new ServiceFailure("2161", "The identifier cannot be null.");
        }
        Identifier pid = new Identifier();
        pid.setValue(syncFailed.getPid());
        boolean allowed;

        //are we allowed? only CNs
        D1AuthHelper authDel = new D1AuthHelper(request, pid, "2162", "2161");
        authDel.doCNOnlyAuthorization(session);


        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        } catch (McdbDocNotFoundException e) {
            throw new ServiceFailure("2161", "The identifier specified by " + syncFailed.getPid()
                + " was not found on this node.");

        } catch (SQLException e) {
            throw new ServiceFailure("2161",
                "Couldn't identify the local id of the identifier specified by "
                    + syncFailed.getPid() + " since " + e.getMessage());
        }
        // TODO: update the CN URL below when the CNRead.SynchronizationFailed
        // method is changed to include the URL as a parameter
        logMetacat.warn(
            "Synchronization for the object identified by " + pid.getValue() + " failed from "
                + syncFailed.getNodeId() + " with message: " + syncFailed.getDescription()
                + ". Logging the event to the Metacat EventLog as a 'syncFailed' event.");
        // TODO: use the event type enum when the SYNCHRONIZATION_FAILED event is added
        String principal = Constants.SUBJECT_PUBLIC;
        if (session != null && session.getSubject() != null) {
            principal = session.getSubject().getValue();
        }
        try {
            EventLog.getInstance()
                .log(request.getRemoteAddr(), request.getHeader("User-Agent"), principal, localId,
                    "synchronization_failed");
        } catch (Exception e) {
            throw new ServiceFailure("2161", "Could not log the error for: " + pid.getValue());
        }
        return true;
    }

    /**
     * Essentially a get() but with different logging behavior
     */
    @Override
    public InputStream getReplica(Session session, Identifier pid)
        throws NotAuthorized, NotImplemented, ServiceFailure, InvalidToken, NotFound {

        logMetacat.info("MNodeService.getReplica() called.");

        // cannot be called by public
        if (session == null) {
            throw new InvalidToken("2183", "No session was provided.");
        }

        logMetacat.info(
            "MNodeService.getReplica() called with parameters: \n" + "\tSession.Subject      = "
                + session.getSubject().getValue() + "\n" + "\tIdentifier           = "
                + pid.getValue());

        InputStream inputStream = null; // bytes to be returned
        boolean allowed = false;
        String localId; // the metacat docid for the pid

        // get the local docid from Metacat
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        } catch (McdbDocNotFoundException e) {
            throw new NotFound("2185",
                "The object specified by " + pid.getValue() + " does not exist at this node.");

        } catch (SQLException e) {
            throw new ServiceFailure("2181",
                "The local id of the object specified by " + pid.getValue()
                    + " couldn't be identified since " + e.getMessage());
        }

        Subject targetNodeSubject = session.getSubject();

        // check for authorization to replicate, null session to act as this source MN
        try {
            allowed = D1Client.getCN().isNodeAuthorized(null, targetNodeSubject, pid);
        } catch (InvalidToken e1) {
            throw new ServiceFailure("2181",
                "Could not determine if node is authorized: " + e1.getMessage());

        } catch (NotFound e1) {
            throw new NotFound("2185",
                "Could not find the object " + pid.getValue() + " in this node - "
                    + e1.getMessage());

        } catch (InvalidRequest e1) {
            throw new ServiceFailure("2181",
                "Could not determine if node is authorized: " + e1.getMessage());

        }

        logMetacat.info(
            "Called D1Client.isNodeAuthorized(). Allowed = " + allowed + " for identifier "
                + pid.getValue());

        // if the person is authorized, perform the read
        if (allowed) {
            SystemMetadata sm = MNodeService.getInstance(request).getSystemMetadata(session, pid);
            ObjectFormat objectFormat = null;
            String type = null;
            try {
                objectFormat = ObjectFormatCache.getInstance().getFormat(sm.getFormatId());
            } catch (BaseException be) {
                logMetacat.warn("MNodeService.getReplica - could not lookup ObjectFormat for: "
                    + sm.getFormatId(), be);
            }
            if (objectFormat != null) {
                type = objectFormat.getFormatType();
            }
            logMetacat.info(
                "MNodeService.getReplica - the data type for the object " + pid.getValue() + " is "
                    + type);
            try {
                inputStream = MetacatHandler.read(localId, type);
            } catch (Exception e) {
                throw new ServiceFailure("2181", "The object specified by " + pid.getValue()
                    + "could not be returned due to error: " + e.getMessage());
            }
        } else {
            throw new NotAuthorized("2182",
                "The pid " + pid.getValue() + " is not authorized to be read by the client.");
        }

        // if we fail to set the input stream
        if (inputStream == null) {
            throw new ServiceFailure("2181",
                "The object specified by " + pid.getValue() + " can't be returned from the node.");
        }

        // log the replica event
        String principal = null;
        if (session.getSubject() != null) {
            principal = session.getSubject().getValue();
        }
        EventLog.getInstance()
            .log(request.getRemoteAddr(), request.getHeader("User-Agent"), principal, localId,
                "replicate");

        return inputStream;
    }

    /**
     * A method to notify the Member Node that the authoritative copy of system metadata on the
     * Coordinating Nodes has changed.
     *
     * @param session                 Session information that contains the identity of the calling
     *                                user as retrieved from the X.509 certificate which must be
     *                                traceable to the CILogon service.
     * @param serialVersion           The serialVersion of the system metadata
     * @param dateSysMetaLastModified The time stamp for when the system metadata was changed
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws InvalidToken
     */
    public boolean systemMetadataChanged(Session session, Identifier pid, long serialVersion,
        Date dateSysMetaLastModified)
        throws NotImplemented, ServiceFailure, NotAuthorized, InvalidRequest, InvalidToken {
        boolean needCheckAuthoriativeNode = true;
        return systemMetadataChanged(needCheckAuthoriativeNode, session, pid, serialVersion,
            dateSysMetaLastModified);
    }

    /**
     * A method to notify the Member Node that the authoritative copy of system metadata on the
     * Coordinating Nodes has changed.
     *
     * @param needCheckAuthoriativeNode this is for the dataone version 2. In the version 2, there
     *                                  are two scenarios: 1. If the node is the authoritative node,
     *                                  it only accepts serial version and replica list. 2. If the
     *                                  node is a replica, it accepts everything. For the v1, api,
     *                                  the parameter should be false.
     * @param session                   Session information that contains the identity of the
     *                                  calling user as retrieved from the X.509 certificate which
     *                                  must be traceable to the CILogon service.
     * @param serialVersion             The serialVersion of the system metadata
     * @param dateSysMetaLastModified   The time stamp for when the system metadata was changed
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws InvalidToken
     */
    public boolean systemMetadataChanged(boolean needCheckAuthoriativeNode, Session session,
        Identifier pid, long serialVersion, Date dateSysMetaLastModified)
        throws NotImplemented, ServiceFailure, NotAuthorized, InvalidRequest, InvalidToken {

        if (isReadOnlyMode()) {
            throw new InvalidRequest("1334",
                "The Metacat member node is on the read-only mode and your request can't be "
                    + "fulfiled. Please try again later.");
        }
        // cannot be called by public
        if (session == null) {
            throw new InvalidToken("1332", "No session was provided.");
        }

        String serviceFailureCode = "1333";
        Identifier sid = getPIDForSID(pid, serviceFailureCode);
        if (sid != null) {
            pid = sid;
        }

        SystemMetadata currentLocalSysMeta = null;
        SystemMetadata newSysMeta = null;

        D1AuthHelper authDel = new D1AuthHelper(request, pid, "1331", serviceFailureCode);
        authDel.doCNOnlyAuthorization(session);

        try {
            SystemMetadataManager.lock(pid);
            // compare what we have locally to what is sent in the change notification
            try {
                currentLocalSysMeta = SystemMetadataManager.getInstance().get(pid);
            } catch (RuntimeException e) {
                String msg = "SystemMetadata for pid " + pid.getValue()
                    + " couldn't be updated because it couldn't be found locally: "
                    + e.getMessage();
                logMetacat.error(msg);
                ServiceFailure sf = new ServiceFailure("1333", msg);
                sf.initCause(e);
                throw sf;
            }

            if (currentLocalSysMeta == null) {
                throw new InvalidRequest("1334",
                                         "We can't find the system metadata in the node for the id "
                                             + pid.getValue());
            }
            if (currentLocalSysMeta.getSerialVersion().longValue() <= serialVersion) {
                try {
                    this.cn = D1Client.getCN();
                    newSysMeta = cn.getSystemMetadata(null, pid);
                } catch (NotFound e) {
                    // huh? you just said you had it
                    String msg = "On updating the local copy of system metadata " + "for pid "
                        + pid.getValue() + ", the CN reports it is not found."
                        + " The error message was: " + e.getMessage();
                    logMetacat.error(msg);
                    //ServiceFailure sf = new ServiceFailure("1333", msg);
                    InvalidRequest sf = new InvalidRequest("1334", msg);
                    sf.initCause(e);
                    throw sf;
                }

                //check about the sid in the system metadata
                Identifier newSID = newSysMeta.getSeriesId();
                if (newSID != null) {
                    if (!isValidIdentifier(newSID)) {
                        throw new InvalidRequest("1334",
                                                 "The series identifier in the new system metadata is invalid.");
                    }
                    Identifier currentSID = currentLocalSysMeta.getSeriesId();
                    if (currentSID != null && currentSID.getValue() != null) {
                        if (!newSID.getValue().equals(currentSID.getValue())) {
                            //newSID doesn't match the currentSID. The newSID shouldn't be used.
                            try {
                                if (IdentifierManager.getInstance()
                                    .identifierExists(newSID.getValue())) {
                                    throw new InvalidRequest("1334", "The series identifier "
                                        + newSID.getValue()
                                        + " in the new system metadata has been used by "
                                        + "another object.");
                                }
                            } catch (SQLException sql) {
                                throw new ServiceFailure("1333", "Couldn't determine if the SID "
                                    + newSID.getValue()
                                    + " in the system metadata exists in the node since "
                                    + sql.getMessage());
                            }

                        }
                    } else {
                        //newSID shouldn't be used
                        try {
                            if (IdentifierManager.getInstance()
                                .identifierExists(newSID.getValue())) {
                                throw new InvalidRequest("1334", "The series identifier "
                                    + newSID.getValue()
                                    + " in the new system metadata has been used by another "
                                    + "object.");
                            }
                        } catch (SQLException sql) {
                            throw new ServiceFailure("1333", "Couldn't determine if the SID "
                                + newSID.getValue()
                                + " in the system metadata exists in the node since "
                                + sql.getMessage());
                        }
                    }
                }
                // update the local copy of system metadata for the pid
                try {
                    if (needCheckAuthoriativeNode) {
                        //this is for the v2 api.
                        if (isAuthoritativeNode(pid)) {
                            //this is the authoritative node, so we only accept replica and
                            // serial version
                            logMetacat.debug(
                                "MNodeService.systemMetadataChanged - this is the authoritative "
                                    + "node for the pid " + pid.getValue());
                            List<Replica> replicas = newSysMeta.getReplicaList();
                            newSysMeta = currentLocalSysMeta;
                            newSysMeta.setSerialVersion(BigInteger.valueOf(serialVersion));
                            newSysMeta.setReplicaList(replicas);
                        } else {
                            //we need to archive the object in the replica node
                            logMetacat.debug("MNodeService.systemMetadataChanged - this is NOT the "
                                                 + "authoritative node for the pid "
                                                 + pid.getValue());
                            logMetacat.debug(
                                "MNodeService.systemMetadataChanged - the new value of archive is "
                                    + newSysMeta.getArchived() + " for the pid " + pid.getValue());
                            logMetacat.debug(
                                "MNodeService.systemMetadataChanged - the local value of archive "
                                    + "is " + currentLocalSysMeta.getArchived() + " for the pid "
                                    + pid.getValue());
                            if (newSysMeta.getArchived() != null && newSysMeta.getArchived() == true
                                && ((currentLocalSysMeta.getArchived() != null
                                && currentLocalSysMeta.getArchived() == false)
                                || currentLocalSysMeta.getArchived() == null)) {
                                logMetacat.debug(
                                    "MNodeService.systemMetadataChanged - start to archive object "
                                        + pid.getValue());
                                boolean logArchive = false;
                                boolean needUpdateModificationDate = false;
                                try {
                                    archiveObject(logArchive, session, pid, newSysMeta,
                                                  needUpdateModificationDate,
                                                  SystemMetadataManager.SysMetaVersion.UNCHECKED);
                                } catch (NotFound e) {
                                    throw new InvalidRequest("1334",
                                                             "Can't find the pid " + pid.getValue()
                                                                 + " for archive.");
                                }

                            } else if ((newSysMeta.getArchived() == null
                                || newSysMeta.getArchived() == false) && (
                                currentLocalSysMeta.getArchived() != null
                                    && currentLocalSysMeta.getArchived() == true)) {
                                throw new InvalidRequest(
                                    "1334", "The pid " + pid.getValue()
                                    + " has been archived and it can't be reset to false.");
                            }
                        }
                    }
                    // Set changeModifyTime false
                    SystemMetadataManager.getInstance()
                        .store(newSysMeta, false, SystemMetadataManager.SysMetaVersion.UNCHECKED);
                    logMetacat.info(
                        "Updated local copy of system metadata for pid " + pid.getValue()
                            + " after change notification from the CN.");

                } catch (RuntimeException e) {
                    String msg =
                        "SystemMetadata for pid " + pid.getValue() + " couldn't be updated: "
                            + e.getMessage();
                    logMetacat.error(msg);
                    ServiceFailure sf = new ServiceFailure("1333", msg);
                    sf.initCause(e);
                    throw sf;
                }

                try {
                    String localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
                    if (ipAddress == null) {
                        request.getRemoteAddr();
                    }
                    if (userAgent == null) {
                        userAgent = request.getHeader("User-Agent");
                    }
                    EventLog.getInstance()
                        .log(ipAddress, userAgent, session.getSubject().getValue(), localId,
                             "updateSystemMetadata");
                } catch (Exception e) {
                    // do nothing, no localId to log with
                    logMetacat.warn("MNodeService.systemMetadataChanged - Could not log "
                                        + "'updateSystemMetadata' event because no localId was found for pid: "
                                        + pid.getValue());
                }

            }


            if (currentLocalSysMeta.getSerialVersion().longValue() <= serialVersion) {
                // submit for indexing
                try {
                    boolean isSysmetaChangeOnly = true;
                    MetacatSolrIndex.getInstance()
                        .submit(newSysMeta.getIdentifier(), newSysMeta, isSysmetaChangeOnly, false);
                } catch (Exception e) {
                    logMetacat.error(
                        "Could not submit changed systemMetadata for indexing, pid: " + newSysMeta
                            .getIdentifier().getValue(), e);
                }
            }
        } finally {
            SystemMetadataManager.unLock(pid);
        }
        // Log the system metadata difference
        SystemMetadataDeltaLogger logger =
            new SystemMetadataDeltaLogger(session, currentLocalSysMeta, newSysMeta);
        logger.log();

        return true;

    }

    /*
     * Set the replication status for the object on the Coordinating Node
     *
     * @param session - the session for the this target node
     * @param pid - the identifier of the object being updated
     * @param nodeId - the identifier of this target node
     * @param status - the replication status to set
     * @param failure - the exception to include, if any
     */
    private void setReplicationStatus(Session session, Identifier pid, NodeReference nodeId,
        ReplicationStatus status, BaseException failure)
        throws ServiceFailure, NotImplemented, NotAuthorized, InvalidRequest {

        // call the CN as the MN to set the replication status
        try {
            this.cn = D1Client.getCN();
            this.cn.setReplicationStatus(session, pid, nodeId, status, failure);

        } catch (InvalidToken e) {
            String msg = "Could not set the replication status for " + pid.getValue()
                + " on the CN (InvalidToken): " + e.getMessage();
            logMetacat.error(msg);
            throw new ServiceFailure("2151", msg);

        } catch (NotFound e) {
            String msg = "Could not set the replication status for " + pid.getValue()
                + " on the CN (NotFound): " + e.getMessage();
            logMetacat.error(msg);
            throw new ServiceFailure("2151", msg);

        }
    }

    private SystemMetadata makePublicIfNot(SystemMetadata sysmeta, Identifier pid,
        boolean needIndex)
        throws ServiceFailure, InvalidToken, NotFound, NotImplemented, InvalidRequest {
        // check if it is publicly readable
        boolean isPublic = false;
        Subject publicSubject = new Subject();
        publicSubject.setValue(Constants.SUBJECT_PUBLIC);
        Session publicSession = new Session();
        publicSession.setSubject(publicSubject);
        AccessRule publicRule = new AccessRule();
        publicRule.addPermission(Permission.READ);
        publicRule.addSubject(publicSubject);

        // see if we need to add the rule
        try {
            isPublic = this.isAuthorized(publicSession, pid, Permission.READ);
        } catch (NotAuthorized na) {
            // well, certainly not authorized for public read!
        }
        if (!isPublic) {
            try {
                SystemMetadataManager.lock(pid);
                if (sysmeta.getAccessPolicy() != null) {
                    sysmeta.getAccessPolicy().addAllow(publicRule);
                } else {
                    AccessPolicy policy = new AccessPolicy();
                    policy.addAllow(publicRule);
                    sysmeta.setAccessPolicy(policy);
                }
                if (needIndex) {
                    // Set needToUpdateModificationTime true
                    this.updateSystemMetadata(sysmeta, true,
                                              SystemMetadataManager.SysMetaVersion.CHECKED);
                }
            } finally {
                SystemMetadataManager.unLock(pid);
            }
        }

        return sysmeta;
    }

    @Override
    public Identifier generateIdentifier(Session session, String scheme, String fragment)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest {

        // check for null session
        if (session == null) {
            throw new InvalidToken("2190",
                "Session is required to generate an Identifier at this Node.");
        }

        Identifier identifier = new Identifier();

        // handle different schemes
        if (scheme.equalsIgnoreCase(UUID_SCHEME)) {
            // UUID
            UUID uuid = UUID.randomUUID();
            identifier.setValue(UUID_PREFIX + uuid.toString());
        } else if (scheme.equalsIgnoreCase(DOI_SCHEME)) {
            // generate a DOI
            try {
                identifier = DOIServiceFactory.getDOIService().generateDOI();
            } catch (Exception e) {
                ServiceFailure sf =
                    new ServiceFailure("2191", "Could not generate DOI: " + e.getMessage());
                sf.initCause(e);
                throw sf;
            }
        } else {
            // default if we don't know the scheme
            if (fragment != null) {
                // for now, just autogen with fragment
                String autogenId = DocumentUtil.generateDocumentId(fragment, 0);
                identifier.setValue(autogenId);
            } else {
                // autogen with no fragment
                String autogenId = DocumentUtil.generateDocumentId(0);
                identifier.setValue(autogenId);
            }
        }

        // TODO: reserve the identifier with the CN. We can only do this when
        // 1) the MN is part of a CN cluster
        // 2) the request is from an authenticated user

        return identifier;
    }



    @Override
    public QueryEngineDescription getQueryEngineDescription(Session session, String engine)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, NotFound {
        if (engine != null && engine.equals(EnabledQueryEngines.SOLRENGINE)) {
            if (!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.SOLRENGINE)) {
                throw new NotImplemented("0000",
                    "MNodeService.getQueryEngineDescription - the query engine " + engine
                        + " hasn't been implemented or has been disabled.");
            }
            try {
                QueryEngineDescription qed =
                    MetacatSolrEngineDescriptionHandler.getInstance().getQueryEngineDescritpion();
                return qed;
            } catch (Exception e) {
                throw new ServiceFailure("Solr server error", e.getMessage());
            }
        } else {
            throw new NotFound("404",
                "The Metacat member node can't find the query engine - " + engine);
        }

    }

    @Override
    public QueryEngineList listQueryEngines(Session session)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented {
        QueryEngineList qel = new QueryEngineList();
        List<String> enables = EnabledQueryEngines.getInstance().getEnabled();
        for (String name : enables) {
            qel.addQueryEngine(name);
        }
        return qel;
    }

    @Override
    public InputStream query(Session session, String engine, String query)
        throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented,
        NotFound {
        Set<Subject> subjects = getQuerySubjects(session);
        boolean isMNadmin = isMNOrCNAdminQuery(session);
        if (engine != null && engine.equals(EnabledQueryEngines.SOLRENGINE)) {
            if (!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.SOLRENGINE)) {
                throw new NotImplemented("0000", "MNodeService.query - the query engine " + engine
                    + " hasn't been implemented or has been disabled.");
            }
            logMetacat.info("MNodeService.query - the solr query is === " + query);
            try {

                return MetacatSolrIndex.getInstance().query(query, subjects, isMNadmin);
            } catch (Exception e) {
                throw new ServiceFailure("Solr server error", e.getMessage());
            }
        } else {
            throw new NotImplemented("0000", "MNodeService.query - the query engine " + engine
                    + " is not supported.");
        }
    }


    /**
     * Handle the query sent by the http post method
     *
     * @param session identity information of the requester
     * @param engine  the query engine will be used. Now we only support solr
     * @param params  the query parameters with key/value pairs
     * @return
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     * @throws NotFound
     */
    public InputStream postQuery(Session session, String engine, HashMap<String, String[]> params)
        throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented,
        NotFound {
        Set<Subject> subjects = getQuerySubjects(session);
        boolean isMNadmin = isMNOrCNAdminQuery(session);
        if (engine != null && engine.equals(EnabledQueryEngines.SOLRENGINE)) {
            if (!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.SOLRENGINE)) {
                throw new NotImplemented("0000", "MNodeService.query - the query engine " + engine
                    + " hasn't been implemented or has been disabled.");
            }
            try {
                SolrParams solrParams = new MultiMapSolrParams(params);
                return MetacatSolrIndex.getInstance()
                    .query(solrParams, subjects, isMNadmin, SolrRequest.METHOD.POST);
            } catch (Exception e) {
                throw new ServiceFailure("2821", "Solr server error: " + e.getMessage());
            }
        } else {
            throw new NotImplemented("2824", "The query engine " + engine
                + " specified on the request isn't supported by the http post method. Now we only"
                + " support the solr engine.");
        }
    }

    /*
     * Extract all subjects from a given session. If the session is null, the public subject will
      be returned.
     */
    private Set<Subject> getQuerySubjects(Session session) {
        Set<Subject> subjects = null;
        if (session != null) {
            subjects = AuthUtils.authorizedClientSubjects(session);
        } else {
            //add the public user subject to the set 
            Subject subject = new Subject();
            subject.setValue(Constants.SUBJECT_PUBLIC);
            subjects = new HashSet<Subject>();
            subjects.add(subject);
        }
        return subjects;
    }

    /*
     * Determine if the given session is a local admin or cn subject.
     */
    private boolean isMNOrCNAdminQuery(Session session) throws ServiceFailure {
        boolean isMNadmin = false;
        if (session != null && session.getSubject() != null) {
            D1AuthHelper authDel = new D1AuthHelper(request, null, "2822", "2821");
            try {
                authDel.doAdminAuthorization(session);
                logMetacat.debug(
                    "MNodeService.isMNOrCNAdminQuery - this is a mn/cn admin session, it will "
                        + "bypass the access control rules.");
                isMNadmin = true;//bypass access rules since it is the admin
            } catch (NotAuthorized e) {
                logMetacat.debug(
                    "MNodeService.isMNOrCNAdminQuery - this is NOT a mn/cn admin session, it "
                        + "can't bypass the access control rules.");
            }
        }
        return isMNadmin;
    }

    /**
     * Given an existing Science Metadata PID, this method mints a DOI and updates the original
     * object "publishing" the update with the DOI. This includes updating the ORE map that
     * describes the Science Metadata+data.
     *
     * @param session
     * @param originalIdentifier
     * @throws InvalidRequest
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotFound
     * @throws InvalidSystemMetadata
     * @throws InsufficientResources
     * @throws UnsupportedType
     * @throws IdentifierNotUnique
     * @see 'https://projects.ecoinformatics.org/ecoinfo/issues/6014'
     */
    public Identifier publish(Session session, Identifier originalIdentifier)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest,
        NotFound, IdentifierNotUnique, UnsupportedType, InsufficientResources,
        InvalidSystemMetadata, IOException {

        String serviceFailureCode = "1030";
        Identifier sid = getPIDForSID(originalIdentifier, serviceFailureCode);
        if (sid != null) {
            originalIdentifier = sid;
        }
        // get the original SM
        SystemMetadata originalSystemMetadata = this.getSystemMetadata(session, originalIdentifier);

        // make copy of it using the marshaller to ensure DEEP copy
        SystemMetadata sysmeta = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TypeMarshaller.marshalTypeToOutputStream(originalSystemMetadata, baos);
            sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        }

        // mint a DOI for the new revision
        Identifier newIdentifier = this.generateIdentifier(session, MNodeService.DOI_SCHEME, null);

        // set new metadata values
        sysmeta.setIdentifier(newIdentifier);
        sysmeta.setObsoletes(originalIdentifier);
        sysmeta.setObsoletedBy(null);

        // ensure it is publicly readable
        sysmeta = makePublicIfNot(sysmeta, originalIdentifier, false);

        //Get the bytes
        InputStream inputStream = null;
        boolean isScienceMetadata = isScienceMetadata(sysmeta);
        //If it's a science metadata doc, we want to update the packageId first
        if (isScienceMetadata) {
            boolean isEML = false;
            //Get the formatId
            ObjectFormatIdentifier objFormatId = originalSystemMetadata.getFormatId();
            String formatId = objFormatId.getValue();
            //For all EML formats
            if (formatId.contains("ecoinformatics.org/eml")) {
                logMetacat.debug("~~~~~~~~~~~~~~~~~~~~~~MNodeService.publish - the object "
                    + originalIdentifier.getValue() + " with format id " + formatId
                    + " is an eml document.");
                isEML = true;
            } else {
                logMetacat.debug(
                    "MNodeService.publish - the object " + originalIdentifier.getValue()
                        + " with format id " + formatId + " is NOT an eml document.");
            }
            InputStream originalObject = this.get(session, originalIdentifier);

            //Edit the science metadata with the new package Id (EML)
            inputStream =
                editScienceMetadata(session, originalObject, originalIdentifier, newIdentifier,
                    isEML, sysmeta);
        } else {
            inputStream = this.get(session, originalIdentifier);
        }

        // Store the new object into hash store
        MCSystemMetadata mcSys = new MCSystemMetadata();
        try {
            MCSystemMetadata.copy(mcSys, sysmeta);
            ObjectInfo doiInfo = storeData(MetacatInitializer.getStorage(), inputStream, sysmeta);
            mcSys.setChecksums(doiInfo.hexDigests());
        } catch (NoSuchAlgorithmException | RuntimeException | InterruptedException |
                 InvocationTargetException | IllegalAccessException eee) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", eee.getMessage());
            sf.initCause(eee);
            throw sf;
        }
        // update the object
        this.update(session, originalIdentifier, inputStream, newIdentifier, mcSys);

        // update ORE that references the scimeta
        // first try the naive method, then check the SOLR index
        try {
            String localId =
                IdentifierManager.getInstance().getLocalId(originalIdentifier.getValue());

            Identifier potentialOreIdentifier = new Identifier();
            potentialOreIdentifier.setValue(SystemMetadataFactory.RESOURCE_MAP_PREFIX + localId);

            InputStream oreInputStream = null;
            try {
                oreInputStream = this.get(session, potentialOreIdentifier);
            } catch (NotFound nf) {
                // this is probably okay for many sci meta data docs
                logMetacat.warn(
                    "No potential ORE map found for: " + potentialOreIdentifier.getValue()
                        + " by the name convention.");
                potentialOreIdentifier = getNewestORE(session, originalIdentifier);
                if (potentialOreIdentifier != null) {
                    try {
                        oreInputStream = this.get(session, potentialOreIdentifier);
                    } catch (NotFound nf2) {
                        // this is probably okay for many sci meta data docs
                        logMetacat.warn(
                            "No potential ORE map found for: " + potentialOreIdentifier.getValue());
                    }
                }
            }
            if (oreInputStream != null) {
                logMetacat.info(
                    "MNodeService.publish - we find the old ore document " + potentialOreIdentifier
                        + " for the metacat object " + originalIdentifier);
                Identifier newOreIdentifier = MNodeService.getInstance(request)
                    .generateIdentifier(session, MNodeService.UUID_SCHEME, null);
                ResourceMapModifier modifier =
                    new ResourceMapModifier(potentialOreIdentifier, oreInputStream,
                        newOreIdentifier);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                modifier.replaceObsoletedId(originalIdentifier, newIdentifier, out,
                    session.getSubject());
                String resourceMapString = out.toString("UTF-8");

                // get the original ORE SM and update the values
                SystemMetadata originalOreSysMeta =
                    this.getSystemMetadata(session, potentialOreIdentifier);
                SystemMetadata oreSysMeta = new SystemMetadata();
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    TypeMarshaller.marshalTypeToOutputStream(originalOreSysMeta, baos);
                    oreSysMeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                        new ByteArrayInputStream(baos.toByteArray()));
                } catch (Exception e) {
                    // report as service failure
                    ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
                    sf.initCause(e);
                    throw sf;
                }

                oreSysMeta.setIdentifier(newOreIdentifier);
                oreSysMeta.setObsoletes(potentialOreIdentifier);
                oreSysMeta.setObsoletedBy(null);
                oreSysMeta.setSize(BigInteger.valueOf(resourceMapString.getBytes("UTF-8").length));
                oreSysMeta.setChecksum(ChecksumUtil.checksum(resourceMapString.getBytes("UTF-8"),
                    oreSysMeta.getChecksum().getAlgorithm()));
                oreSysMeta.setFileName("resourceMap_" + newOreIdentifier.getValue() + ".rdf.xml");

                // ensure ORE is publicly readable
                oreSysMeta = makePublicIfNot(oreSysMeta, potentialOreIdentifier, false);
                List<Identifier> dataIdentifiers =
                    modifier.getSubjectsOfDocumentedBy(newIdentifier);
                // ensure all data objects allow public read
                if (enforcePublicEntirePackageInPublish) {
                    List<String> pidsToSync = new ArrayList<String>();
                    for (Identifier dataId : dataIdentifiers) {
                        SystemMetadata dataSysMeta = this.getSystemMetadata(session, dataId);
                        dataSysMeta = makePublicIfNot(dataSysMeta, dataId, true);
                        pidsToSync.add(dataId.getValue());

                    }
                    SyncAccessPolicy sap = new SyncAccessPolicy();
                    try {
                        sap.sync(pidsToSync);
                    } catch (Exception e) {
                        // ignore
                        logMetacat.warn(
                            "Error attempting to sync access for data objects when publishing "
                                + "package");
                    }
                }
                // save the updated ORE
                logMetacat.info(
                    "MNodeService.publish - the new ore document is " + newOreIdentifier.getValue()
                        + " for the doi " + newIdentifier.getValue());
                MCSystemMetadata oreMCsysMeta = new MCSystemMetadata();
                try {
                    MCSystemMetadata.copy(oreMCsysMeta, oreSysMeta);
                    ObjectInfo oreInfo = storeData(MetacatInitializer.getStorage(),
                         new ByteArrayInputStream(resourceMapString.getBytes("UTF-8")), oreSysMeta);
                    oreMCsysMeta.setChecksums(oreInfo.hexDigests());
                } catch (NoSuchAlgorithmException | RuntimeException | InterruptedException |
                         InvocationTargetException | IllegalAccessException eee) {
                    // report as service failure
                    ServiceFailure sf = new ServiceFailure("1030", eee.getMessage());
                    sf.initCause(eee);
                    throw sf;
                }
                this.update(session, potentialOreIdentifier,
                    new ByteArrayInputStream(resourceMapString.getBytes("UTF-8")), newOreIdentifier,
                            oreMCsysMeta);

            } else {
                String error = " with null identifier.";
                if (potentialOreIdentifier != null) {
                    error = potentialOreIdentifier.getValue();
                }
                logMetacat.error("Metacat can't create a new resource map object to integrate the"
                                     + " new doi object " + newIdentifier.getValue() + " since"
                                     + " it can't read the original resource map " + error);
            }
        } catch (McdbDocNotFoundException e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        } catch (UnsupportedEncodingException e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        } catch (NoSuchAlgorithmException e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        } catch (SQLException e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        }

        return newIdentifier;
    }

    /**
     * Update a science metadata document with its new Identifier
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param object  - the InputStream for the XML object to be edited
     * @param pid     - the Identifier of the XML object to be updated
     * @param newPid  = the new Identifier to give to the modified XML doc
     * @return newObject - The InputStream for the modified XML object
     * @throws ServiceFailure
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     */
    public InputStream editScienceMetadata(Session session, InputStream object, Identifier pid,
        Identifier newPid, boolean isEML, SystemMetadata newSysmeta)
        throws ServiceFailure, IOException, UnsupportedEncodingException, InvalidToken,
        NotAuthorized, NotFound, NotImplemented {

        logMetacat.debug("D1NodeService.editScienceMetadata() called.");

        InputStream newObject = null;

        try {
            //Get the root node of the XML document
            byte[] xmlBytes = IOUtils.toByteArray(object);
            String xmlStr = new String(xmlBytes, "UTF-8");

            Document doc = XMLUtilities.getXMLReaderAsDOMDocument(new StringReader(xmlStr));
            org.w3c.dom.Node docNode = doc.getDocumentElement();

            //For all EML formats
            if (isEML) {
                //Update or add the id attribute
                XMLUtilities.addAttributeNodeToDOMTree(docNode, XPATH_EML_ID, newPid.getValue());
            }

            //The modified object InputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(docNode);
            Result outputTarget = new StreamResult(outputStream);
            TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
            byte[] output = outputStream.toByteArray();
            Checksum checksum =
                ChecksumUtil.checksum(output, newSysmeta.getChecksum().getAlgorithm());
            newObject = new ByteArrayInputStream(output);
            newSysmeta.setChecksum(checksum);
            newSysmeta.setSize(BigInteger.valueOf(output.length));
            logMetacat.debug(
                "MNNodeService.editScienceMetadata - the new checksum is " + checksum.getValue()
                    + " with algorithm " + checksum.getAlgorithm() + " for the new pid "
                    + newPid.getValue() + " which is published from the pid " + pid.getValue());
        } catch (TransformerException e) {
            throw new ServiceFailure("1030", "MNNodeService.editScienceMetadata(): "
                + "Could not update the ID in the XML document for " + "pid " + pid.getValue()
                + " : " + e.getMessage());
        } catch (IOException e) {
            throw new ServiceFailure("1030", "MNNodeService.editScienceMetadata(): "
                + "Could not update the ID in the XML document for " + "pid " + pid.getValue()
                + " : " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceFailure("1030", "MNNodeService.editScienceMetadata(): "
                + "Could not update the ID in the XML document for " + "pid " + pid.getValue()
                + " since the checksum can't be computed : " + e.getMessage());
        }

        return newObject;
    }

    /**
     * Determines if we already have registered an ORE map for this package NOTE: uses a solr query
     * to locate OREs for the object
     *
     * @param guid of the EML/packaging object
     * @return list of resource map identifiers for the given pid
     */
    public List<Identifier> lookupOreFor(Session session, Identifier guid,
        boolean includeObsolete) {
        // Search for the ORE if we can find it
        String pid = guid.getValue();
        List<Identifier> retList = null;
        try {
            String query =
                "fl=id,resourceMap&wt=xml&q=-obsoletedBy:[* TO *]+resourceMap:[* TO *]+id:\"" + pid
                    + "\"";
            if (includeObsolete) {
                query = "fl=id,resourceMap&wt=xml&q=resourceMap:[* TO *]+id:\"" + pid + "\"";
            }

            InputStream results = this.query(session, "solr", query);
            org.w3c.dom.Node rootNode =
                XMLUtilities.getXMLReaderAsDOMTreeRootNode(new InputStreamReader(results, "UTF-8"));
            //String resultString = XMLUtilities.getDOMTreeAsString(rootNode);
            org.w3c.dom.NodeList nodeList =
                XMLUtilities.getNodeListWithXPath(rootNode, "//arr[@name=\"resourceMap\"]/str");
            if (nodeList != null && nodeList.getLength() > 0) {
                retList = new ArrayList<Identifier>();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String found = nodeList.item(i).getFirstChild().getNodeValue();
                    Identifier oreId = new Identifier();
                    oreId.setValue(found);
                    retList.add(oreId);
                }
            }
        } catch (Exception e) {
            logMetacat.error(
                "Error checking for resourceMap[s] on pid " + pid + ". " + e.getMessage(), e);
        }

        return retList;
    }

    /**
     * Get the newest ore id which integrates the given metadata pid
     *
     * @param session     the subjects call the method
     * @param metadataPid the metadata pid which be integrated. It is a pid
     * @return the ore pid if we can find one; otherwise null will be returned
     */
    private Identifier getNewestORE(Session session, Identifier metadataPid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
        Identifier potentialOreIdentifier = null;
        if (metadataPid != null && !metadataPid.getValue().trim().equals("")) {
            List<Identifier> potentialOreIdentifiers = this.lookupOreFor(session, metadataPid);
            if (potentialOreIdentifiers != null && potentialOreIdentifiers.size() > 0) {
                int size = potentialOreIdentifiers.size();
                for (int i = size - 1; i >= 0; i--) {
                    Identifier id = potentialOreIdentifiers.get(i);
                    if (id != null && id.getValue() != null && !id.getValue().trim().equals("")) {
                        SystemMetadata sys = this.getSystemMetadata(session, id);
                        if (sys != null && sys.getObsoletedBy() == null) {
                            //found the non-obsoletedBy ore document.
                            logMetacat.debug(
                                "MNodeService.getNewestORE - found the ore map from the list when"
                                    + " the index is " + i + " and its pid is " + id.getValue());
                            potentialOreIdentifier = id;
                            break;
                        }
                    }
                }
            } else {
                logMetacat.warn(
                    "MNodeService.getNewestORE - No potential ORE map found for the metadata object"
                        + metadataPid.getValue() + " by the solr query.");
            }
        }
        return potentialOreIdentifier;
    }

    /**
     * Determines if we already have registered an ORE map for this package NOTE: uses a solr query
     * to locate OREs for the object
     *
     * @param guid of the EML/packaging object
     * @todo should be consolidate with the above method.
     */
    private List<Identifier> lookupOreFor(Session session, Identifier guid) {
        // Search for the ORE if we can find it
        String pid = guid.getValue();
        List<Identifier> retList = null;
        try {
            String query = "fl=id,resourceMap&wt=xml&q=id:\"" + pid + "\"";
            InputStream results = this.query(session, "solr", query);
            org.w3c.dom.Node rootNode =
                XMLUtilities.getXMLReaderAsDOMTreeRootNode(new InputStreamReader(results, "UTF-8"));
            //String resultString = XMLUtilities.getDOMTreeAsString(rootNode);
            org.w3c.dom.NodeList nodeList =
                XMLUtilities.getNodeListWithXPath(rootNode, "//arr[@name=\"resourceMap\"]/str");
            if (nodeList != null && nodeList.getLength() > 0) {
                retList = new ArrayList<Identifier>();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String found = nodeList.item(i).getFirstChild().getNodeValue();
                    logMetacat.debug("MNodeService.lookupOreRor - found the resource map" + found);
                    Identifier oreId = new Identifier();
                    oreId.setValue(found);
                    retList.add(oreId);
                }
            }
        } catch (Exception e) {
            logMetacat.error(
                "Error checking for resourceMap[s] on pid " + pid + ". " + e.getMessage(), e);
        }

        return retList;
    }

    /**
     * Returns a stream to a resource map
     *
     * @param session: The user's session
     * @param pid:     The resource map PID
     * @return A stream to the bagged package
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotImplemented
     */
    private ResourceMap serializeResourceMap(Session session, Identifier pid)
        throws InvalidToken, NotFound, InvalidRequest, ServiceFailure, NotAuthorized,
        InvalidRequest, NotImplemented {
        SystemMetadata sysMeta = this.getSystemMetadata(session, pid);
        ResourceMap resMap = null;
        try {
            InputStream oreInputStream = this.get(session, pid);
            resMap = ResourceMapFactory.getInstance().deserializeResourceMap(oreInputStream);
        } catch (OREException | URISyntaxException e) {
            logMetacat.error(
                "There was problem with the resource map. Check that that the resource map is "
                    + "valid.", e);
            throw new ServiceFailure(
                "There was problem with the resource map. Check that that the resource map is "
                    + "valid.", e.getMessage());
        } catch (UnsupportedEncodingException e) {
            logMetacat.error("The resource map has an unsupported encoding format.", e);
            throw new ServiceFailure("The resource map has an unsupported encoding format.",
                e.getMessage());
        } catch (OREParserException e) {
            logMetacat.error("Failed to parse the ORE.", e);
            throw new ServiceFailure("Failed to parse the ORE.", e.getMessage());
        }
        return resMap;
    }


    /**
     * Maps a resource map to a list to an object that contains all of the identifiers
     * (objects+system metadata)
     *
     * @param session: The user's session
     * @param orePid:  The pid of the ORE document
     * @throws ServiceFailure
     */
    private Map<Identifier, Map<Identifier, List<Identifier>>> parseResourceMap(Session session,
        Identifier orePid) throws ServiceFailure {

        // Container that holds the pids of all of the objects that are in a package
        Map<Identifier, Map<Identifier, List<Identifier>>> resourceMapStructure = null;
        try {
            InputStream oreInputStream = this.get(session, orePid);
            resourceMapStructure = ResourceMapFactory.getInstance().parseResourceMap(
                oreInputStream); //TODO: Check aggregates vs documents in parseResourceMap
        } catch (OREException | OREParserException | UnsupportedEncodingException |
            NotImplemented e) {
            throw new ServiceFailure(
                "Failed to parse the resource map. Check that the resource map is valid",
                e.getMessage());
        } catch (InvalidToken | NotAuthorized e) {
            logMetacat.error(
                "Invalid token while parsing the resource map. Check that you have permissions.",
                e);
        } catch (URISyntaxException e) {
            throw new ServiceFailure(
                "There was a malformation in the resource map. Check that the resource map is "
                    + "valid", e.getMessage());
        } catch (NotFound e) {
            throw new ServiceFailure(
                "Failed to locate the resource map. Check that the right pid was used.",
                e.getMessage());
        }
        if (resourceMapStructure == null) {
            throw new ServiceFailure("", "There was an error while parsing the resource map.");
        }
        return resourceMapStructure;
    }

    /**
     * Exports a data package to disk using the BagIt
     *
     * The Bagit 0.97 format corresponds to the V1 export format The Bagit 1.0 format corresponds to
     * the V2 export format
     *
     * @param session  Information about the user performing the request
     * @param formatId
     * @param pid      The pid of the resource map
     * @return A stream of a bag
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     * @throws NotFound
     */
    @Override
    public InputStream getPackage(Session session, ObjectFormatIdentifier formatId, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented,
        NotFound {
        if (formatId == null) {
            throw new InvalidRequest("2873", "The format id wasn't specified in the request. "
                + "Ensure that  the format id is properly set in the request.");
        } else if (!formatId.getValue().equals("application/bagit-097") && !formatId.getValue()
            .equals("application/bagit-1.0")) {
            throw new NotImplemented("",
                "The format " + formatId.getValue() + " is not a supported format.");
        }
        String serviceFailureCode = "2871";
        Identifier sid = getPIDForSID(pid, serviceFailureCode);
        if (sid != null) {
            pid = sid;
        }

        if (formatId.getValue().equals("application/bagit-097")) {
            // Use the Version 1 package format
            logMetacat.debug("Serving a download request for a Version 1 Package");
            PackageDownloaderV1 downloader = null;
            try {
                downloader = new PackageDownloaderV1(pid);
                SystemMetadata sysMeta = this.getSystemMetadata(session, pid);
                if (ObjectFormatCache.getInstance().getFormat(sysMeta.getFormatId()).getFormatType()
                    .equals("RESOURCE")) {
                    //Get the resource map as a map of Identifiers
                    InputStream oreInputStream = this.get(session, pid);
                    Map<Identifier, Map<Identifier, List<Identifier>>> resourceMapStructure =
                        ResourceMapFactory.getInstance().parseResourceMap(oreInputStream);
                    downloader.packagePids.addAll(resourceMapStructure.keySet());
                    //Loop through each object in this resource map
                    for (Map<Identifier, List<Identifier>> entries :
                        resourceMapStructure.values()) {
                        //Loop through each metadata object in this entry
                        Set<Identifier> metadataIdentifiers = entries.keySet();
                        for (Identifier metadataID : metadataIdentifiers) {
                            try {
                                //Get the system metadata for this metadata object
                                SystemMetadata metadataSysMeta =
                                    this.getSystemMetadata(session, metadataID);
                                // If it's supported metadata, create the PDF file out of it
                                if (ObjectFormatCache.getInstance()
                                    .getFormat(metadataSysMeta.getFormatId()).getFormatType()
                                    .equals("METADATA")) {
                                    InputStream metadataStream = this.get(session, metadataID);
                                    downloader.addSciPdf(metadataStream, metadataSysMeta,
                                        metadataID);
                                }
                            } catch (Exception e) {
                                logMetacat.error(e.toString());
                            }
                        }
                        downloader.packagePids.addAll(entries.keySet());
                        for (List<Identifier> dataPids : entries.values()) {
                            downloader.packagePids.addAll(dataPids);
                        }
                    }
                } else {
                    // just the lone pid in this package
                    //throw an invalid request exception
                    throw new InvalidRequest("2873",
                        "The given pid " + pid.getValue() + " is not a package "
                            + "id (resource map id). Please use a package id instead.");
                }

                /**
                 * Up to this point, the only file that has been added to the bag is the metadata
                 * pdf file.
                 * The next step is looping over each object in the package, determining its
                 * filename,
                 * getting an InputStream to it, and adding it to the bag.
                 */
                Set<Identifier> packagePidsUnique = new HashSet<>(downloader.packagePids);
                int index = 0;
                for (Identifier entryPid : packagePidsUnique) {
                    //Get the system metadata for each item
                    SystemMetadata entrySysMeta = this.getSystemMetadata(session, entryPid);

                    String objectFormatType =
                        ObjectFormatCache.getInstance().getFormat(entrySysMeta.getFormatId())
                            .getFormatType();
                    String fileName = null;

                    //Our default file name is just the ID + format type (e.g. walker.1.1-DATA)
                    fileName = entryPid.getValue().replaceAll("[^a-zA-Z0-9\\-\\.]", "_") + "-"
                        + objectFormatType;

                    // ensure there is a file extension for the object
                    String extension = ObjectFormatInfo.instance()
                        .getExtension(entrySysMeta.getFormatId().getValue());
                    fileName += extension;

                    // if SM has the file name, ignore everything else and use that
                    if (entrySysMeta.getFileName() != null) {
                        fileName = entrySysMeta.getFileName().replaceAll("[^a-zA-Z0-9\\-\\.]", "_");
                    }

                    // Add the stream of the file to the bag object & write to the pid mapping file
                    InputStream entryInputStream = this.get(session, entryPid);
                    boolean success = false;
                    try {
                        downloader.speedBag.addFile(entryInputStream,
                            Paths.get("data/", fileName).toString(), false);
                    } catch (SpeedBagException e) {
                        fileName = index++ + "-duplicate-" + fileName;
                        logMetacat.warn(
                            "Duplicate data filename, renaming file to add to bag: " + fileName, e);
                        downloader.speedBag.addFile(entryInputStream,
                            Paths.get("data/", fileName).toString(), false);
                    }
                    downloader.pidMapping.append(
                        entryPid.getValue() + "\t" + "data/" + fileName + "\n");
                }

                // Get a stream to the pid mapping file and add it as a tag file, in the bag root
                ByteArrayInputStream pidFile = new ByteArrayInputStream(
                    downloader.pidMapping.toString().getBytes(StandardCharsets.UTF_8));
                downloader.speedBag.addFile(pidFile, "pid-mapping.txt", true);
            } catch (SpeedBagException e) {
                // report as service failure
                ServiceFailure sf =
                    new ServiceFailure("1030", "Error creating the bag: " + e.getMessage());
                sf.initCause(e);
                throw sf;
            } catch (IOException e) {
                // report as service failure
                ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
                sf.initCause(e);
                throw sf;
            } catch (OREException e) {
                // report as service failure
                ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
                sf.initCause(e);
                throw sf;
            } catch (URISyntaxException e) {
                // report as service failure
                ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
                sf.initCause(e);
                throw sf;
            } catch (OREParserException e) {
                // report as service failure
                ServiceFailure sf = new ServiceFailure("1030", "There was an "
                    + "error while processing the resource map. Ensure that the resource map "
                    + "for the package is valid. " + e.getMessage());
                sf.initCause(e);
                throw sf;
            } catch (NoSuchAlgorithmException e) {
                ServiceFailure sf = new ServiceFailure("1030", "There was an "
                    + "error while adding a file to the archive. Please ensure that the "
                    + "checksumming algorithm is supported." + e.getMessage());
                sf.initCause(e);
                throw sf;
            }

            // The underlying speedbag object is ready to be served to the clinet, do that here
            try {
                return downloader.speedBag.stream();
            } catch (NullPointerException | IOException e) {
                ServiceFailure sf = new ServiceFailure("1030",
                    "There was an " + "error while streaming the downloaded data package. "
                        + e.getMessage());
                sf.initCause(e);
                throw sf;
            } catch (NoSuchAlgorithmException e) {
                ServiceFailure sf = new ServiceFailure("1030", "While creating the package "
                    + "download, an unsupported checksumming algorithm was encountered. "
                    + e.getMessage());
                sf.initCause(e);
                throw sf;
            }
        } else if (formatId.getValue().equals("application/bagit-1.0")) {

            logMetacat.debug("Serving a download request for a Version 2 Package");
            // Get the resource map. This is used various places downstream, which is why it's
            // not a stream. Note that
            // this throws if it can't be parsed because we depend on it for object pids.
            Map<Identifier, Map<Identifier, List<Identifier>>> resourceMapStructure =
                parseResourceMap(session, pid);
            // Holds the PID of every object in the resource map
            List<Identifier> pidsOfPackageObjects = new ArrayList<Identifier>();
            pidsOfPackageObjects.addAll(resourceMapStructure.keySet());

            for (Map<Identifier, List<Identifier>> entries : resourceMapStructure.values()) {
                pidsOfPackageObjects.addAll(entries.keySet());
                for (List<Identifier> dataPids : entries.values()) {
                    pidsOfPackageObjects.addAll(dataPids);
                }
            }
            // Get a ResourceMap object representing the resource map. Throw if we can't get it
            ResourceMap resourceMap = serializeResourceMap(session, pid);
            SystemMetadata resourceMapSystemMetadata = this.getSystemMetadata(session, pid);
            // Create the downloader that's responsible for creating the readme and bag archive.
            // Throws if something went wrong (we can't continue without a PackageDownloader)
            PackageDownloaderV2 downloader =
                new PackageDownloaderV2(pid, resourceMap, resourceMapSystemMetadata);

            List<Identifier> metadataIdentifiers = downloader.getCoreMetadataIdentifiers();
            // Iterate over all the pids and find get an input stream and potential disk location
            HashSet<Identifier> uniquePids = new HashSet<>(pidsOfPackageObjects);
            for (Identifier entryPid : uniquePids) {
                // Skip the resource map and the science metadata so that we don't write them to
                // the data direcotry
                if (metadataIdentifiers.contains(entryPid)) {
                    continue;
                }
                // Get the system metadata and a stream to the data file
                SystemMetadata entrySysMeta = this.getSystemMetadata(session, entryPid);
                InputStream objectInputStream = this.get(session, entryPid);
                // Add the stream to the downloader, which will handle finding its location
                downloader.addDataFile(entrySysMeta, objectInputStream);
                try {
                    downloader.addSystemMetadata(entrySysMeta);
                } catch (NoSuchAlgorithmException e) {
                    ServiceFailure sf = new ServiceFailure("1030", "While creating the package."
                        + "Could not add the system metadata to the zipfile. " + e.getMessage());
                    sf.initCause(e);
                    throw sf;
                }
            }
            try {
                List<Identifier> scienceMetadataIdentifiers =
                    downloader.getScienceMetadataIdentifiers();
                if (scienceMetadataIdentifiers != null && !scienceMetadataIdentifiers.isEmpty()) {
                    Identifier sciMetataId = scienceMetadataIdentifiers.get(0);
                    SystemMetadata systemMetadata = this.getSystemMetadata(session, sciMetataId);
                    InputStream scienceMetadataStream = this.get(session, sciMetataId);
                }
                HashSet<Identifier> uniqueSciPids = new HashSet<>(scienceMetadataIdentifiers);
                // Add the science metadata and their associated system metadatas to the downloader
                for (Identifier scienceMetadataIdentifier : uniqueSciPids) {
                    logMetacat.debug("Adding science metadata to the bag");
                    SystemMetadata systemMetadata =
                        this.getSystemMetadata(session, scienceMetadataIdentifier);
                    InputStream scienceMetadataStream =
                        this.get(session, scienceMetadataIdentifier);
                    downloader.addScienceMetadata(systemMetadata, scienceMetadataStream);
                }

                return downloader.download();
            } catch (NullPointerException e) {
                ServiceFailure sf = new ServiceFailure("1030",
                    "There was an " + "error while streaming the downloaded data package. "
                        + e.getMessage());
                sf.initCause(e);
                throw sf;
            }
        } else {
            ServiceFailure sf = new ServiceFailure("",
                "The download forma,t " + formatId.getValue() + " is not a " + "supported format.");
            throw sf;
        }
    }

    /**
     * Archives an object, where the object is either a data object or a science metadata object.
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid     - The object identifier to be archived
     * @return pid - the identifier of the object used for the archiving
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    public Identifier archive(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
        if (isReadOnlyMode()) {
            throw new ServiceFailure("2912", ReadOnlyChecker.DATAONEERROR);
        }
        boolean allowed = false;
        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new ServiceFailure("1350", "The provided identifier was invalid.");
        }

        String serviceFailureCode = "1350";
        Identifier sid = getPIDForSID(pid, serviceFailureCode);
        if (sid != null) {
            pid = sid;
        }
        // does the subject have archive (a D1 CHANGE_PERMISSION level) privileges on the pid?
        try {
            allowed = isAuthorized(session, pid, Permission.CHANGE_PERMISSION);
        } catch (InvalidRequest e) {
            throw new ServiceFailure("1350", e.getDescription());
        }

        if (allowed) {
            try {
                SystemMetadataManager.lock(pid);
                SystemMetadata sysmeta = SystemMetadataManager.getInstance().get(pid);
                //check the if it has enough quota if th quota service is enabled
                String quotaSubject = request.getHeader(QuotaServiceManager.QUOTASUBJECTHEADER);
                QuotaServiceManager.getInstance()
                    .enforce(quotaSubject, session.getSubject(), sysmeta,
                        QuotaServiceManager.ARCHIVEMETHOD);
                boolean needModifyDate = true;
                boolean logArchive = true;
                super.archiveObject(logArchive, session, pid, sysmeta, needModifyDate,
                                    SystemMetadataManager.SysMetaVersion.CHECKED);
            } catch (InsufficientResources e) {
                throw new ServiceFailure("2912",
                    "The user doesn't have enough quota to perform this request " + e.getMessage());
            } catch (InvalidRequest ee) {
                throw new InvalidToken("2913", "The request is invalid - " + ee.getMessage());
            } finally {
                SystemMetadataManager.unLock(pid);
            }
        } else {
            throw new NotAuthorized("1320", "The provided identity does not have "
                + "permission to archive the object on the Node.");
        }

        return pid;
    }

    /**
     * Update the system metadata of the specified pid.
     */
    @Override
    public boolean updateSystemMetadata(Session session, Identifier pid, SystemMetadata sysmeta)
        throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest, InvalidSystemMetadata,
        InvalidToken {

        if (isReadOnlyMode()) {
            throw new ServiceFailure("4868", ReadOnlyChecker.DATAONEERROR);
        }
        if (sysmeta == null) {
            throw new InvalidRequest("4869",
                "The system metadata object should NOT be null in the updateSystemMetadata "
                    + "request.");
        }
        if (pid == null || pid.getValue() == null) {
            throw new InvalidRequest("4869",
                "Please specify the id in the updateSystemMetadata request ");
        }

        if (session == null) {
            //TODO: many of the thrown exceptions do not use the correct error codes
            //check these against the docs and correct them
            throw new NotAuthorized("4861",
                "No Session - could not authorize for updating system metadata."
                    + "  If you are not logged in, please do so and retry the request.");
        }
        boolean success = false;
        try {
            SystemMetadataManager.lock(pid);
            //update the system metadata locally
            SystemMetadata currentSysmeta = SystemMetadataManager.getInstance().get(pid);
            if (currentSysmeta == null) {
                throw new InvalidRequest(
                    "4869",
                    "We can't find the current system metadata on the member node for the id "
                        + pid.getValue());
            }
            D1AuthHelper authDel = null;
            try {
                authDel = new D1AuthHelper(request, pid, "4861", "4868");
                authDel.doUpdateAuth(session, currentSysmeta, Permission.CHANGE_PERMISSION,
                                     this.getCurrentNodeId());
            } catch (ServiceFailure e) {
                throw new ServiceFailure("4868",
                                         "Can't determine if the client has the permission to update the system "
                                             + "metacat of the object with id " + pid.getValue()
                                             + " since " + e.getDescription());
            } catch (NotAuthorized e) {
                //the user doesn't have the change permission. However, if it has the write
                // permission and doesn't modify the access rules, Metacat still allows it to
                // update the system metadata
                try {
                    authDel.doUpdateAuth(session, currentSysmeta, Permission.WRITE,
                                         this.getCurrentNodeId());
                    //now the user has the write the permission. If the access rules in the new
                    // and old system metadata are the same, it is fine; otherwise, Metacat
                    // throws an exception
                    if (D1NodeService.isAccessControlDirty(sysmeta, currentSysmeta)) {
                        throw new NotAuthorized("4861",
                                                "Can't update the system metadata of the object with id "
                                                    + pid.getValue()
                                                    + " since the user try to change the access rules without the "
                                                    + "change permission: " + e.getDescription());
                    }
                } catch (ServiceFailure ee) {
                    throw new ServiceFailure(
                        "4868",
                        "Can't determine if the client has the permission to update the system "
                            + "metadata the object with id " + pid.getValue() + " since "
                            + ee.getDescription());
                } catch (NotAuthorized ee) {
                    throw new NotAuthorized("4861",
                                            "Can't update the system metadata of object with id "
                                                + pid.getValue() + " since " + ee.getDescription());
                }
            }
            Date currentModiDate = currentSysmeta.getDateSysMetadataModified();
            Date commingModiDate = sysmeta.getDateSysMetadataModified();
            if (commingModiDate == null) {
                throw new InvalidRequest("4869",
                                         "The system metadata modification date can't be null.");
            }
            if (currentModiDate != null && commingModiDate.getTime() != currentModiDate.getTime()) {
                throw new InvalidRequest("4869", "Your system metadata modification date is "
                    + commingModiDate.toString()
                    + ". It doesn't match our current system metadata modification date in "
                    + "the member node - " + currentModiDate.toString()
                    + ". Please check if you have got the newest version of the system "
                    + "metadata before the modification.");
            }
            //check the if client change the authoritative member node.
            if (currentSysmeta.getAuthoritativeMemberNode() != null
                && sysmeta.getAuthoritativeMemberNode() != null && !currentSysmeta
                .getAuthoritativeMemberNode().equals(sysmeta.getAuthoritativeMemberNode())) {
                throw new InvalidRequest(
                    "4869", "Current authoriativeMemberNode is " + currentSysmeta
                    .getAuthoritativeMemberNode().getValue()
                    + " but the value on the new system metadata is " + sysmeta
                    .getAuthoritativeMemberNode().getValue()
                    + ". They don't match. Clients don't have the permission to change it.");
            } else if (currentSysmeta.getAuthoritativeMemberNode() != null
                && sysmeta.getAuthoritativeMemberNode() == null) {
                throw new InvalidRequest(
                    "4869", "Current authoriativeMemberNode is " + currentSysmeta
                    .getAuthoritativeMemberNode().getValue()
                    + " but the value on the new system metadata is null. They don't match. "
                    + "Clients don't have the permission to change it.");
            } else if (currentSysmeta.getAuthoritativeMemberNode() == null
                && sysmeta.getAuthoritativeMemberNode() != null) {
                throw new InvalidRequest(
                    "4869",
                    "Current authoriativeMemberNode is null but the value on the new system "
                        + "metadata is not null. They don't match. Clients don't have the "
                        + "permission " + "to change it.");
            }
            checkAddRestrictiveAccessOnDOI(currentSysmeta, sysmeta);
            boolean needUpdateModificationDate = true;
            boolean fromCN = false;
            // Ignore the replica update since it is controlled by cn
            List<Replica> replicas = currentSysmeta.getReplicaList();
            sysmeta.setReplicaList(replicas);
            success = updateSystemMetadata(session, pid, sysmeta, needUpdateModificationDate,
                                           currentSysmeta, fromCN,
                                           SystemMetadataManager.SysMetaVersion.CHECKED);
        } finally {
            SystemMetadataManager.unLock(pid);
        }

        if (success) {
            // attempt to re-register the identifier (it checks if it is a doi)
            try {
                logMetacat.info("MNodeSerice.updateSystemMetadata - register doi if the pid "
                    + sysmeta.getIdentifier().getValue() + " is a doi");
                DOIServiceFactory.getDOIService().registerDOI(sysmeta);
            } catch (Exception e) {
                logMetacat.error("MNodeService.updateSystemMetadata - Could not [re]register DOI: "
                    + e.getMessage(), e);
            }
        }

        if (success && needSync) {
            logMetacat.debug(
                "MNodeService.updateSystemMetadata - the cn needs to be notified that the system "
                    + "metadata of object " + pid.getValue() + " has been changed ");
            this.cn = D1Client.getCN();
            //TODO
            //notify the cns the synchornize the new system metadata.
            // run it in a thread to avoid connection timeout
            Runnable runner = new Runnable() {
                private CNode cNode = null;
                private SystemMetadata sys = null;
                private Identifier id = null;

                @Override
                public void run() {
                    try {
                        if (this.cNode == null) {
                            logMetacat.warn(
                                "MNodeService.updateSystemMetadata - can't get the instance of "
                                    + "the CN. So can't call cn.synchronize to update the system "
                                    + "metadata in CN.");
                        } else if (id != null) {
                            logMetacat.info(
                                "MNodeService.updateSystemMetadata - calling cn.synchornized in "
                                    + "another thread for pid " + id.getValue());
                            this.cNode.synchronize(null, id);
                        } else {
                            logMetacat.warn(
                                "MNodeService.updateSystemMetadata - the pid is null. So can't "
                                    + "call cn.synchronize to update the system metadata in CN.");
                        }
                    } catch (BaseException e) {
                        logMetacat.error("It is a DataONEBaseException and its detail code is "
                            + e.getDetail_code() + " and its code is " + e.getCode());
                        logMetacat.error("Can't update the systemmetadata of pid " + id.getValue()
                            + " in CNs through cn.synchronize method since " + e.getMessage(), e);
                    } catch (Exception e) {
                        logMetacat.error("Can't update the systemmetadata of pid " + id.getValue()
                            + " in CNs through cn.synchronize method since " + e.getMessage(), e);
                    }
                }

                private Runnable init(CNode cn, SystemMetadata sys, Identifier id) {
                    this.cNode = cn;
                    this.sys = sys;
                    this.id = id;
                    return this;

                }
            }.init(cn, sysmeta, pid);
            // submit the task, and that's it
            if (executor != null) {
                executor.submit(runner);
            } else {
                logMetacat.warn(
                    "MNodeSerivce.updateSystemMetadata - since the executor service for "
                        + "submitting the call of cn.synchronize() is null, the system metadata "
                        + "change of the id " + pid.getValue()
                        + " can't go to cn through cn.synchronize.");
            }
        }
        return success;
    }

    /**
     * Get the status of the system. this is an unofficial dataone api method. Currently we only
     * reply the size of the index queue. The method will return the input stream of a xml instance.
     * In the future, we need to add a new dataone type to represent the result.
     *
     * @param session
     * @return the input stream which is the xml presentation of the status report
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws NotImplemented
     */
    public InputStream getStatus(Session session)
                                        throws NotAuthorized, ServiceFailure, NotImplemented{
        throw new NotImplemented("1253", "We don't support this feature anymore.");
    }

    /**
     * Make status of the given identifier (e.g. a DOI) public
     *
     * @param session   the subject who calls the method
     * @param identifier the identifier whose status will be public. It can be a pid or sid.
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws NotFound
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws DOIException
     */
    public void publishIdentifier(Session session, Identifier identifier)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest,
        NotFound, IdentifierNotUnique, UnsupportedType, InsufficientResources,
        InvalidSystemMetadata, DOIException {

        String invalidRequestCode = "1202";
        String notFoundCode = "1280";
        if (identifier == null || identifier.getValue().trim().equals("")) {
            throw new InvalidRequest(invalidRequestCode,
                "MNodeService.publishIdentifier - the identifier which needs to be published "
                    + "can't be null.");
        }
        String serviceFailureCode = "1310";
        Identifier pid = getPIDForSID(identifier,
            serviceFailureCode);//identifier can be a sid, now we got the pid
        if (pid == null) {
            pid = identifier;
        }
        logMetacat.info(
            "MNodeService.publishIdentifier - the PID for the id " + identifier.getValue() + " is "
                + pid.getValue());
        SystemMetadata existingSysMeta =
            getSystemMetadataForPID(pid, serviceFailureCode, invalidRequestCode, notFoundCode,
                true);
        D1AuthHelper authDel = new D1AuthHelper(request, pid, "1200", "1310");
        //if the user has the write permission, it will be all set
        authDel.doUpdateAuth(session, existingSysMeta, Permission.WRITE, this.getCurrentNodeId());
        existingSysMeta =
            makePublicIfNot(existingSysMeta, pid, true);//make the metadata file public
        Identifier oreIdentifier = getNewestORE(session, pid);
        if (oreIdentifier != null) {
            //make the result map public
            SystemMetadata oreSysmeta =
                getSystemMetadataForPID(oreIdentifier, serviceFailureCode, invalidRequestCode,
                    notFoundCode, true);
            oreSysmeta = makePublicIfNot(oreSysmeta, oreIdentifier, true);
            if (enforcePublicEntirePackageInPublish) {
                //make data objects public readable if needed
                InputStream oreInputStream = this.get(session, oreIdentifier);
                if (oreInputStream != null) {
                    Model model = ModelFactory.createDefaultModel();
                    model.read(oreInputStream, null);
                    List<Identifier> dataIdentifiers =
                        ResourceMapModifier.getSubjectsOfDocumentedBy(pid, model);
                    for (Identifier dataId : dataIdentifiers) {
                        SystemMetadata dataSysMeta = this.getSystemMetadata(session, dataId);
                        dataSysMeta = makePublicIfNot(dataSysMeta, dataId, true);
                    }
                }
            }
        }
        try {
            DOIServiceFactory.getDOIService().publishIdentifier(session, identifier);
        } catch (PropertyNotFoundException e) {
            throw new ServiceFailure("3196",
                "Can't publish the identifier since " + e.getMessage());
        } catch (DOIException e) {
            throw new ServiceFailure("3196",
                "Can't publish the identifier since " + e.getMessage());
        } catch (InstantiationException e) {
            throw new ServiceFailure("3196",
                "Can't publish the identifier since " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new ServiceFailure("3196",
                "Can't publish the identifier since " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new ServiceFailure("3196",
                "Can't publish the identifier since " + e.getMessage());
        }
    }

    /**
     * The admin API call to reindex a list of documents in the instance.
     * @param session  the identity of requester. It must have administrative permissions
     * @param identifiers  the list of objects' identifier which will be reindexed.
     * @return true if the reindex request is scheduled. If something went wrong, an exception will
     * be thrown.
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplementedException
     */
    public Boolean reindex(Session session, List<Identifier> identifiers) throws ServiceFailure,
                                            NotAuthorized, InvalidRequest, NotImplemented{
        String serviceFailureCode = "5901";
        String notAuthorizedCode = "5902";
        String notAuthorizedError =
                "The provided identity does not have permission to reindex objects on the Node: ";
        checkAdminPrivilege(session, serviceFailureCode, notAuthorizedCode, notAuthorizedError);
        handleReindexAction(identifiers);
        return Boolean.TRUE;
    }

    /**
     * The admin API call to reindex all documents in the instance.
     * @param session  the identity of requester. It must have administrative permissions
     * @return true if the reindex request is scheduled. If something went wrong, an exception will
     * be thrown.
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplementedException
     */
    public Boolean reindexAll(Session session) throws ServiceFailure, NotAuthorized,
                                                          InvalidRequest, NotImplemented {
        String serviceFailureCode = "5901";
        String notAuthorizedCode = "5902";
        String notAuthorizedError =
                "The provided identity does not have permission to reindex objects on the Node: ";
        checkAdminPrivilege(session, serviceFailureCode, notAuthorizedCode, notAuthorizedError);
        handleReindexAllAction();
        return Boolean.TRUE;
    }

    /**
     * Update all controlled identifiers' (such as DOI) metadata on the third party service.
     * @param session  the identity of requester. It must have administrative permissions
     * @return true if the reindex request is scheduled. If something went wrong, an exception will
     * be thrown.
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    public Boolean updateAllIdMetadata(Session session) throws ServiceFailure,
                                                    NotAuthorized, InvalidRequest, NotImplemented {
        String serviceFailureCode = "5906";
        String notAuthorizedCode = "5907";
        String notAuthorizedError = "The provided identity does not have permission to update "
                                    + "identifiers' metadata on the Node: ";
        if(doiUpdater == null) {
            throw new ServiceFailure(serviceFailureCode, "MNodeService.updateAllIdMetadata - "
                    + "the UpdateDOI object has not been initialized and is null. "
                    + "So Metacat can not submit the updateAllIdMetadata task by it.");
        }
        checkAdminPrivilege(session, serviceFailureCode, notAuthorizedCode, notAuthorizedError);
        final UpdateDOI udoi = doiUpdater;
        logMetacat.debug("MNodeService.updateAllIdMetadata");
        Runnable runner = new Runnable() {
            /**
             * Override
             */
            public void run() {
                try {
                    udoi.upgrade();
                } catch (AdminException e) {
                     logMetacat.error("MNodeService.updateAllIdMetadata - "
                           + "can not update all identifiers' metadata since " +e.getMessage());
                }
            }
        };
        if (executor != null) {
            executor.submit(runner);
        } else {
            throw new ServiceFailure(serviceFailureCode, "MNodeService.updateAllIdMetadata - "
                    + "the ExecutorService object has not been initialized and is null. "
                    + "So Metacat can not submit the updateAllIdMetadata task by it.");
        }
        return Boolean.TRUE;
    }

    /**
     * Update the given identifiers' (such as DOI) metadata on the third party service.
     * @param session  the identity of requester. It must have administrative permissions.
     * @param pids  the list of pids will be updated
     * @param formatIds  the list of format id to which the identifiers belong will be updated
     * @return true if the reindex request is scheduled. If something went wrong, an exception will
     * be thrown.
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    public Boolean updateIdMetadata(Session session, String[] pids, String[] formatIds)
            throws ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {
        String serviceFailureCode = "5906";
        String notAuthorizedCode = "5907";
        String notAuthorizedError = "The provided identity does not have permission to update "
                                    + "identifiers' metadata on the Node: ";
        checkAdminPrivilege(session, serviceFailureCode, notAuthorizedCode, notAuthorizedError);
        if (pids == null && formatIds == null) {
            throw new InvalidRequest("5908", "Users should specify values of pid or formatId in "
                                                  + "the update identifier metadata request.");
        }
        try {
            UpdateDOI udoi = new UpdateDOI();
            if (pids != null && pids.length > 0) {
                logMetacat.debug("MNodeService.updateIdMetadata - update a list of pids.");
                udoi.upgradeById(Arrays.asList(pids));
            }
            if (formatIds != null && formatIds.length > 0) {
                logMetacat.debug("MNodeService.updateIdMetadata - update a list of format ids.");
                udoi.upgradeByFormatId(Arrays.asList(formatIds));
            }
        } catch (AdminException e) {
            throw new ServiceFailure(serviceFailureCode, e.getMessage());
        }
        return Boolean.TRUE;
    }

    /**
     * Check if the given session has the admin privilege. If it does not have, a NotAuthorized
     * exception will be thrown.
     * @param session  the session will be checked.
     * @param serviceFailureCode  the detail code for the ServiceFailure exception
     * @param notAuthorizedCode  the detail code for the NotAuthorized exception
     * @param error  the error message will be in the exception
     * @throws NotAuthorized
     * @throws ServiceFailure
     */
    protected void checkAdminPrivilege(Session session, String serviceFailureCode,
                     String notAuthorizedCode, String error) throws NotAuthorized, ServiceFailure {
        if (session == null) {
            throw new NotAuthorized(notAuthorizedCode, error + " public");
        }
        try {
            Identifier identifier = null;
            D1AuthHelper authDel =
                new D1AuthHelper(request, identifier, notAuthorizedCode, serviceFailureCode);
            authDel.doAdminAuthorization(session);
        } catch (NotAuthorized na) {
            if (session.getSubject() != null) {
                throw new NotAuthorized(notAuthorizedCode, error
                        + session.getSubject().getValue());
            } else {
                throw new NotAuthorized(notAuthorizedCode, error + " public");
            }
        }
    }

    /**
     * Rebuild the index for one or more documents
     * @param pids  the list of identifier whose solr doc needs to be rebuilt
     */
    protected void handleReindexAction(List<Identifier> pids) {
        logMetacat.info("MNodeService.handleReindexAction - reindex some objects");
        if (pids == null) {
            return;
        }
        for (Identifier identifier : pids) {
            if (identifier != null) {
                logMetacat.debug("MNodeService.handleReindexAction: queueing doc index for pid "
                                                                    + identifier.getValue());
                try {
                    SystemMetadata sysMeta = SystemMetadataManager.getInstance().get(identifier);
                    if (sysMeta == null) {
                        logMetacat.debug("MNodeService.handleReindexAction: no system metadata was "
                                                      + "found for pid " + identifier.getValue());
                    } else {
                        // submit for indexing
                        MetacatSolrIndex.getInstance().submit(identifier, sysMeta, false);
                    }
                } catch (Exception e) {
                    logMetacat.info("MNodeService.handleReindexAction: Error submitting to "
                            + "index for pid " + identifier.getValue());
                }
            }
        }
    }


    /**
     * Rebuild the index for all documents in the systemMetadata table.
     */
    protected void handleReindexAllAction() {
        // Process all of the documents
        logMetacat.debug("MNodeService.handleReindexAllAction - "
                                               + "reindex all objects in this Metacat instance");
        Runnable indexAll = new Runnable() {
            public void run() {
                List<String> resourceMapFormats = ResourceMapNamespaces.getNamespaces();
                buildAllNonResourceMapIndex(resourceMapFormats);
                buildAllResourceMapIndex(resourceMapFormats);
            }
        };
        Thread thread = new Thread(indexAll);
        thread.start();
    }

    /**
     * Index all non-resourcemap objects first. We don't put the list of pids in a vector anymore.
     * @param resourceMapFormatList  the list of the resource map format
     */
    private void buildAllNonResourceMapIndex(List<String> resourceMapFormatList) {
        boolean firstTime = true;
        StringBuilder sql = new StringBuilder("select guid from systemmetadata");
        if (resourceMapFormatList != null && resourceMapFormatList.size() > 0) {
            for (String format : resourceMapFormatList) {
                if (format != null && !format.trim().equals("")) {
                    if (firstTime) {
                        sql.append(" where object_format !='");
                        sql.append(format);
                        sql.append("'");
                        firstTime = false;
                    } else {
                        sql.append(" and object_format !='");
                        sql.append(format);
                        sql.append("'");
                    }
                }
            }
            sql.append(" order by date_uploaded asc");
        }
        logMetacat.debug("MNodeService.buildAllNonResourceMapIndex - the final query is "
                                                                        + sql.toString());
        try {
            long size = buildIndexFromQuery(sql.toString());
            logMetacat.info(
                "MNodeService.buildAllNonResourceMapIndex - the number of non-resource map "
                    + "objects is "
                    + size + " being submitted to the index queue.");
        } catch (SQLException | ServiceFailure e) {
            logMetacat.error(
                "MNodeService.buildAllNonResourceMapIndex - can't index the objects since: "
                    + e.getMessage());
        }
    }

    /**
     * Index all resource map objects. We don't put the list of pids in a vector anymore.
     * @param resourceMapFormatList
     */
    private void buildAllResourceMapIndex(List<String> resourceMapFormatList) {
        StringBuilder sql = new StringBuilder("select guid from systemmetadata");
        if (resourceMapFormatList != null && resourceMapFormatList.size() > 0) {
            boolean firstTime = true;
            for (String format : resourceMapFormatList) {
                if (format != null && !format.trim().equals("")) {
                    if (firstTime) {
                        sql.append(" where object_format ='");
                        sql.append(format);
                        sql.append("'");
                        firstTime = false;
                    } else {
                        sql.append(" or object_format ='");
                        sql.append(format);
                        sql.append("'");
                    }
                }
            }
            sql.append(" order by date_uploaded asc");
        }
        logMetacat.info("MNodeService.buildAllResourceMapIndex - the final query is "
                                                                    + sql.toString());
        try {
            long size = buildIndexFromQuery(sql.toString());
            logMetacat.info(
                "MNodeService.buildAllResourceMapIndex - the number of resource map objects is "
                    + size + " being submitted to the index queue.");
        } catch (SQLException | ServiceFailure e) {
            logMetacat.error(
                "MNodeService.buildAllResourceMapIndex - can't index the objects since: "
                    + e.getMessage());
        }
    }

    /**
     * Build index of objects selecting from the given sql query.
     * @param sql  the query which will be used to executed to select identifiers for reindexing
     * @return the number of objects which were reindexed
     * @throws SQLException
     * @throws SericeFailure
     */
    private long buildIndexFromQuery(String sql) throws SQLException, ServiceFailure {
        DBConnection dbConn = null;
        long i = 0;
        int serialNumber = -1;
        if (metacatSolrIndex == null) {
            throw new ServiceFailure("0000", "The MetacatSolrIndex can't be initialized so "
                                        + "we can't regenerate all index for the Metacat instance");
        }
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("MNodeService.buildIndexFromQuery");
            serialNumber = dbConn.getCheckOutSerialNumber();
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String guid = null;
                try {
                    guid = rs.getString(1);
                    Identifier identifier = new Identifier();
                    identifier.setValue(guid);
                    SystemMetadata sysMeta = SystemMetadataManager.getInstance().get(identifier);
                    if (sysMeta != null) {
                        // submit for indexing
                        boolean isSysmetaChangeOnly = false;
                        boolean followRevisions = false;
                        metacatSolrIndex
                            .submit(identifier, sysMeta, isSysmetaChangeOnly, followRevisions,
                                    IndexGenerator.LOW_PRIORITY);
                        i++;
                        logMetacat.debug("MNodeService.buildIndexFromQuery - queued "
                                             + "SystemMetadata for indexing in the "
                                             + "buildIndexFromQuery on pid: " + guid);
                    }
                } catch (Exception ee) {
                    logMetacat.warn(
                        "MNodeService.buildIndexFromQuery - can't queue the object " + guid
                            + " for indexing since: " + ee.getMessage());
                }
            }
            rs.close();
            stmt.close();
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return i;
    }

    /**
     * Check if the new system meta data removed the public-readable access rule for an DOI object (
     * DOI can be in the identifier or sid fields)
     *
     * @param newSysMeta
     * @throws InvalidRequest
     */
    private void checkAddRestrictiveAccessOnDOI(SystemMetadata oldSysMeta,
        SystemMetadata newSysMeta) throws InvalidRequest {
        String doi = "doi:";
        boolean identifierIsDOI = false;
        boolean sidIsDOI = false;
        if (newSysMeta.getIdentifier() == null) {
            throw new InvalidRequest("4869",
                "In the MN.updateSystemMetadata method, the identifier shouldn't be null in the "
                    + "new version system metadata ");
        }
        String identifier = newSysMeta.getIdentifier().getValue();
        String sid = null;
        if (newSysMeta.getSeriesId() != null) {
            sid = newSysMeta.getSeriesId().getValue();
        }
        // determine if this identifier is an DOI
        if (identifier != null && identifier.startsWith(doi)) {
            identifierIsDOI = true;
        }
        // determine if this sid is an DOI
        if (sid != null && sid.startsWith(doi)) {
            sidIsDOI = true;
        }
        if (identifierIsDOI || sidIsDOI) {
            Subject publicUser = new Subject();
            publicUser.setValue("public");
            //We only apply this rule when the old system metadata allow the public user read
            // this object.
            boolean isOldSysmetaPublicReadable = false;
            AccessPolicy oldAccess = oldSysMeta.getAccessPolicy();
            if (oldAccess != null) {
                if (oldAccess.getAllowList() != null) {
                    for (AccessRule item : oldAccess.getAllowList()) {
                        if (item.getSubjectList() != null && item.getSubjectList()
                            .contains(publicUser)) {
                            if (item.getPermissionList() != null && item.getPermissionList()
                                .contains(Permission.READ)) {
                                isOldSysmetaPublicReadable = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (isOldSysmetaPublicReadable) {
                AccessPolicy access = newSysMeta.getAccessPolicy();
                if (access == null) {
                    throw new InvalidRequest("4869",
                        "In the MN.updateSystemMetadata method, the public-readable access rule "
                            + "shouldn't be removed for an DOI object " + identifier + " or SID "
                            + sid);
                } else {
                    boolean found = false;
                    if (access.getAllowList() != null) {
                        for (AccessRule item : access.getAllowList()) {
                            if (item.getSubjectList() != null && item.getSubjectList()
                                .contains(publicUser)) {
                                if (item.getPermissionList() != null && item.getPermissionList()
                                    .contains(Permission.READ)) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!found) {
                        throw new InvalidRequest("4869",
                            "In the MN.updateSystemMetadata method, the public-readable access "
                                + "rule shouldn't be removed for an DOI object " + identifier
                                + " or SID " + sid);
                    }
                }
            }
        }
    }

    protected NodeReference getCurrentNodeId() {
        return TypeFactory.buildNodeReference(
            Settings.getConfiguration().getString("dataone.nodeId"));
    }

    /**
     * Determine if the current node is the authoritative node for the given pid. (uses HZsysmeta
     * map)
     */
    protected boolean isAuthoritativeNode(Identifier pid) throws InvalidRequest, ServiceFailure {
        boolean isAuthoritativeNode = false;
        if (pid != null && pid.getValue() != null) {
            SystemMetadata sys = SystemMetadataManager.getInstance().get(pid);
            if (sys != null) {
                NodeReference node = sys.getAuthoritativeMemberNode();
                if (node != null) {
                    String nodeValue = node.getValue();
                    logMetacat.debug(
                        "The authoritative node for id " + pid.getValue() + " is " + nodeValue);
                    String currentNodeId = Settings.getConfiguration().getString("dataone.nodeId");
                    logMetacat.debug("The node id in metacat.properties is " + currentNodeId);
                    if (currentNodeId != null && !currentNodeId.trim().equals("")
                        && currentNodeId.equals(nodeValue)) {
                        logMetacat.debug("They are matching, so the authoritative mn of the object "
                            + pid.getValue() + " is the current node");
                        isAuthoritativeNode = true;
                    }
                } else {
                    throw new InvalidRequest("4869",
                        "Coudn't find the authoritative member node in the system metadata "
                            + "associated with the pid " + pid.getValue());
                }
            } else {
                throw new InvalidRequest("4869",
                    "Coudn't find the system metadata associated with the pid " + pid.getValue());
            }
        } else {
            throw new InvalidRequest("4869", "The request pid is null");
        }
        return isAuthoritativeNode;
    }


    /**
     * Check if the metacat is in the read-only mode.
     *
     * @return true if it is; otherwise false.
     */
    protected boolean isReadOnlyMode() {
        boolean readOnly = false;
        ReadOnlyChecker checker = new ReadOnlyChecker();
        readOnly = checker.isReadOnly();
        return readOnly;
    }

    /**
     * Set the value if Metacat need to make the entire package public during the publish process
     *
     * @param enforce enforce the entire package public readable or not
     */
    public static void setEnforcePublisEntirePackage(boolean enforce) {
        enforcePublicEntirePackageInPublish = enforce;
    }

    /**
     * This method is for testing only - replacing the real class by a stubbed
     * Mockito UpdateDOI class.
     * @param anotherDoiUpdater  the stubbed Mockito UpdateDOI class which will be used to
     *                      replace the real class
     */
    public static void setDOIUpdater(UpdateDOI anotherDoiUpdater) {
        doiUpdater = anotherDoiUpdater;
    }

    /**
     * This method is for testing only - replacing the real class by a stubbed
     * Mockito MetacatSolrIndex class.
     * @param solrIndex  the stubbed Mockito MetacatSolrIndex class which will be used to
     *                      replace the real class
     */
    public static void setMetacatSolrIndex(MetacatSolrIndex solrIndex) {
        metacatSolrIndex = solrIndex;
    }

    /**
     * Store the input stream into hash store
     * @param storage  the storage system which stores the object
     * @param inputStream  the object
     * @param sysmeta  the system metadata associated with the object
     * @return the metadata of the object
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     * @throws RuntimeException
     * @throws InterruptedException
     * @throws ServiceFailure
     */
    public static ObjectInfo storeData(Storage storage, InputStream inputStream,
                                    org.dataone.service.types.v1.SystemMetadata sysmeta)
                                    throws NoSuchAlgorithmException, IOException, InvalidRequest,
                                           InvalidSystemMetadata, RuntimeException,
                                           InterruptedException, ServiceFailure {
        //null is the additional algorithm
        if (sysmeta.getIdentifier() == null || sysmeta.getIdentifier().getValue() == null
                                        || sysmeta.getIdentifier().getValue().isBlank()) {
            throw new InvalidRequest("0000",
                                       "Metacat can't save an object whose identifier is blank");
        }
        if (sysmeta.getChecksum() == null || sysmeta.getChecksum().getValue() == null
                                     || sysmeta.getChecksum().getValue().isBlank()) {
            throw new InvalidRequest("0000",
                    "Metacat can't save an object whose checksum is blank");
        }
        if (sysmeta.getChecksum().getAlgorithm() == null
                                    || sysmeta.getChecksum().getAlgorithm().isBlank()) {
            throw new InvalidRequest("0000",
                            "Metacat can't save an object whose checksum algorithm is blank");
        }
        if (sysmeta.getSize() == null) {
            throw new InvalidRequest("0000",
                        "Metacat can't save an object whose size is blank in system metadata");
        }
        return storage.storeObject(inputStream, sysmeta.getIdentifier(), null,
                            sysmeta.getChecksum().getValue(),
                            sysmeta.getChecksum().getAlgorithm(),
                            sysmeta.getSize().longValue());
    }
}
