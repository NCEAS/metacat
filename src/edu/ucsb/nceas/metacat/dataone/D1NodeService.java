package edu.ucsb.nceas.metacat.dataone;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.Lock;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.client.v2.formats.ObjectFormatCache;
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
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v1.util.AuthUtils;
import org.dataone.service.util.Constants;

import edu.ucsb.nceas.metacat.AccessionNumber;
import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.DBTransform;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.EventLogData;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeByteArrayInputStream;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.quota.QuotaServiceManager;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandler;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.metacat.restservice.multipart.StreamingMultipartRequestResolver;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.SkinUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public abstract class D1NodeService {

    public static final String DELETEDMESSAGE =
        "The object with the PID has been deleted from the node.";
    public static final String METADATA = "METADATA";

    private static org.apache.commons.logging.Log logMetacat =
        LogFactory.getLog(D1NodeService.class);

    /** For logging the operations */
    protected HttpServletRequest request;
    protected String ipAddress = null;
    protected String userAgent = null;

    /* reference to the metacat handler */
    protected static MetacatHandler handler = new MetacatHandler();

    /**
     * limit paged results sets to a configured maximum
     */
    protected static int MAXIMUM_DB_RECORD_COUNT = 7000;

    static {
        try {
            MAXIMUM_DB_RECORD_COUNT =
                Integer.valueOf(PropertyService.getProperty("database.webResultsetSize"));
        } catch (Exception e) {
            logMetacat.warn("Could not set MAXIMUM_DB_RECORD_COUNT", e);
        }
    }

    /**
     * out-of-band session object to be used when not passed in as a method parameter
     */
    protected Session session2;

    /**
     * Constructor - used to set the metacatUrl from a subclass extending D1NodeService
     *
     * @param metacatUrl - the URL of the metacat service, including the ending /d1
     */
    public D1NodeService(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * retrieve the out-of-band session
     * @return
     */
    public Session getSession() {
        return session2;
    }

    /**
     * Set the out-of-band session
     * @param session
     */
    public void setSession(Session session) {
        this.session2 = session;
    }


    /**
     * A centralized point for accessing the CN Nodelist,
     * to make it easier to cache the nodelist in the future,
     * if it's seen as helpful performance-wise
     * @return
     * @throws ServiceFailure
     * @throws NotImplemented
     */
    protected NodeList getCNNodeList() throws ServiceFailure, NotImplemented {

        // are we allowed to do this? only CNs are allowed
        CNode cn = D1Client.getCN();
        logMetacat.debug("getCNNodeList - got CN instance");
        return cn.listNodes();
    }

    /**
     * This method provides a lighter weight mechanism than
     * getSystemMetadata() for a client to determine basic
     * properties of the referenced object.
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid - the identifier of the object to be described
     *
     * @return describeResponse - A set of values providing a basic description
     *                            of the object.
     *
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    public DescribeResponse describe(Session session, Identifier id)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        // delegate seriesId resolution and authorization to getSystemMetadata
        // just convert contents here.

        SystemMetadata sysmeta = getSystemMetadata(session, id);
        DescribeResponse describeResponse =
            new DescribeResponse(sysmeta.getFormatId(), sysmeta.getSize(),
                                 sysmeta.getDateSysMetadataModified(), sysmeta.getChecksum(),
                                 sysmeta.getSerialVersion());

        return describeResponse;

    }

    /**
     * Deletes an object from the Member Node, where the object is either a
     * data object or a science metadata object. No access checking.
     *
     * @param username - the name of the user who calls the method. This is only for logging.
     * @param pid - The object identifier to be deleted
     *
     * @return pid - the identifier of the object used for the deletion
     *
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    public Identifier delete(String username, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        String localId = null;

        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new ServiceFailure("1350", "The provided identifier was invalid.");
        }

        // check for the existing identifier
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        } catch (McdbDocNotFoundException e) {
            logMetacat.warn("D1NodeService.delete - the object itself with the provided identifier "
                                + pid.getValue()
                                + " doesn't exist in the system. But we will continute to delete "
                                + "the system metadata of the object.");
            //in cn, data objects only have system metadata without real data.
            try {
                SystemMetadata sysMeta = SystemMetadataManager.getInstance().get(pid);
                if (sysMeta != null) {
                    SystemMetadataManager.getInstance().delete(pid);
                    try {
                        MetacatSolrIndex.getInstance().submitDeleteTask(pid, sysMeta);
                    } catch (Exception ee) {
                        logMetacat.warn(
                            "D1NodeService.delete - the object with the provided identifier "
                                + pid.getValue()
                                + " was deleted. But the MN solr index can't be deleted.");
                    }
                    //since data objects were not registered in the identifier table, we use pid
                    // as the docid
                    EventLog.getInstance()
                        .log(request.getRemoteAddr(), request.getHeader("User-Agent"), username,
                             pid.getValue(), Event.DELETE.xmlValue());

                } else {
                    throw new ServiceFailure(
                        "1350", "Couldn't delete the object " + pid.getValue()
                        + ". Couldn't obtain the system metadata record.");

                }

            } catch (RuntimeException re) {
                throw new ServiceFailure(
                    "1350", "Couldn't delete " + pid.getValue() + ". The error message was: "
                    + re.getMessage());

            } catch (InvalidRequest ire) {
                throw new InvalidToken("1351", "Couldn't delete " + pid.getValue() + " since "
                                        + ire.getMessage());
            }
            return pid;
        } catch (SQLException e) {
            throw new ServiceFailure(
                "1350", "The object with the provided " + "identifier " + pid.getValue()
                + " couldn't be identified since " + e.getMessage());
        }

        try {
            // delete the document, as admin
            // the index task submission is handle in this method
            DocumentImpl.delete(localId, pid);
            EventLog.getInstance()
                .log(request.getRemoteAddr(), request.getHeader("User-Agent"), username, localId,
                     Event.DELETE.xmlValue());

        } catch (McdbDocNotFoundException e) {
            throw new NotFound("1340", "The provided identifier was invalid.");

        } catch (SQLException e) {
            throw new ServiceFailure(
                "1350", "There was a problem deleting the object." + "The error message was: "
                + e.getMessage());
        } catch (Exception e) { // for some reason DocumentImpl throws a general Exception
            throw new ServiceFailure(
                "1350", "There was a problem deleting the object." + "The error message was: "
                + e.getMessage());
        }

        return pid;
    }

    /**
     * Low level, "are you alive" operation. A valid ping response is
     * indicated by a HTTP status of 200.
     *
     * @return true if the service is alive
     *
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws InsufficientResources
     */
    public Date ping() throws NotImplemented, ServiceFailure, InsufficientResources {

        // test if we can get a database connection
        int serialNumber = -1;
        DBConnection dbConn = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("MNodeService.ping");
            serialNumber = dbConn.getCheckOutSerialNumber();
        } catch (SQLException e) {
            ServiceFailure sf = new ServiceFailure("", e.getMessage());
            sf.initCause(e);
            throw sf;
        } finally {
            // Return the database connection
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }

        return Calendar.getInstance().getTime();
    }

    /**
     * Adds a new object to the Node, where the object is either a data
     * object or a science metadata object. This method is called by clients
     * to create new data objects on Member Nodes or internally for Coordinating
     * Nodes
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid - The object identifier to be created
     * @param object - the object bytes
     * @param sysmeta - the system metadata that describes the object
     *
     * @return pid - the object identifier created
     *
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
        Session session, Identifier pid, InputStream object, SystemMetadata sysmeta,
        boolean changeModificationDate)
        throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
        InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest {

        Identifier resultPid = null;
        String localId = null;

        // check for null session
        if (session == null) {
            throw new InvalidToken("4894", "Session is required to WRITE to the Node.");
        }
        Subject subject = session.getSubject();

        Subject publicSubject = new Subject();
        publicSubject.setValue(Constants.SUBJECT_PUBLIC);
        // be sure the user is authenticated for create()
        if (subject == null || subject.getValue() == null || subject.equals(publicSubject)) {
            throw new NotAuthorized(
                "1100", "The provided identity does not have "
                + "permission to WRITE to the Node.");

        }

        // verify that pid == SystemMetadata.getIdentifier()
        logMetacat.debug(
            "Comparing pid|sysmeta_pid: " + pid.getValue() + "|" + sysmeta.getIdentifier()
                .getValue());
        if (!pid.getValue().equals(sysmeta.getIdentifier().getValue())) {
            throw new InvalidSystemMetadata("1180", "The supplied system metadata is invalid. "
                + "The identifier " + pid.getValue() + " does not match identifier"
                + "in the system metadata identified by " + sysmeta.getIdentifier().getValue()
                + ".");

        }

        if (sysmeta.getChecksum() == null) {
            logMetacat.error(
                "D1NodeService.create - the checksum object from the system metadata shouldn't be"
                    + " null for the object "
                    + pid.getValue());
            throw new InvalidSystemMetadata(
                "1180", "The checksum object from the system metadata shouldn't be null.");
        }


        // save the sysmeta
        try {
            // lock and unlock of the pid happens in the subclass
            SystemMetadataManager.getInstance().store(sysmeta, changeModificationDate);
        } catch (Exception e) {
            logMetacat.error(
                "D1Node.create - There was problem to save the system metadata: " + pid.getValue(),
                e);
            throw new ServiceFailure(
                "1190", "There was problem to save the system metadata: " + pid.getValue()
                + " since " + e.getMessage());
        }
        boolean isScienceMetadata = false;
        // Science metadata (XML) or science data object?
        // TODO: there are cases where certain object formats are science metadata
        // but are not XML (netCDF ...).  Handle this.
        if (isScienceMetadata(sysmeta)) {
            isScienceMetadata = true;
            // CASE METADATA:
            //String objectAsXML = "";
            try {
                NonXMLMetadataHandler handler =
                    NonXMLMetadataHandlers.newNonXMLMetadataHandler(sysmeta.getFormatId());
                if (handler != null) {
                    //non-xml metadata object path
                    if (ipAddress == null) {
                        ipAddress = request.getRemoteAddr();
                    }
                    if (userAgent == null) {
                        userAgent = request.getHeader("User-Agent");
                    }
                    EventLogData event =
                        new EventLogData(ipAddress, userAgent, null, null, "create");
                    localId = handler.save(object, sysmeta, session, event);
                } else {
                    String formatId = null;
                    if (sysmeta.getFormatId() != null) {
                        formatId = sysmeta.getFormatId().getValue();
                    }
                    localId =
                        insertOrUpdateDocument(object, "UTF-8", pid, session, "insert", formatId,
                                               sysmeta.getChecksum());
                }
            } catch (IOException e) {
                removeSystemMetaAndIdentifier(pid);
                String msg = "The Node is unable to create the object " + pid.getValue()
                    + " There was a problem converting the object to XML";
                logMetacat.error(msg, e);
                throw new ServiceFailure("1190", msg + ": " + e.getMessage());

            } catch (ServiceFailure e) {
                removeSystemMetaAndIdentifier(pid);
                logMetacat.error(
                    "D1NodeService.create - the node couldn't create the object " + pid.getValue()
                        + " since " + e.getMessage(), e);
                throw e;
            } catch (InvalidRequest e) {
                removeSystemMetaAndIdentifier(pid);
                logMetacat.error(
                    "D1NodeService.create - the node couldn't create the object " + pid.getValue()
                        + " since " + e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                removeSystemMetaAndIdentifier(pid);
                logMetacat.error(
                    "The node is unable to create the object: " + pid.getValue() + " since "
                        + e.getMessage(), e);
                throw new ServiceFailure(
                    "1190", "The node is unable to create the object: " + pid.getValue() + " since "
                    + e.getMessage());
            }

        } else {

            // DEFAULT CASE: DATA (needs to be checked and completed)
            try {
                if (ipAddress == null) {
                    ipAddress = request.getRemoteAddr();
                }
                if (userAgent == null) {
                    userAgent = request.getHeader("User-Agent");
                }
                EventLogData event = new EventLogData(ipAddress, userAgent, null, null, "create");
                localId = insertDataObject(object, pid, session, sysmeta.getChecksum(), event);
            } catch (ServiceFailure e) {
                removeSystemMetaAndIdentifier(pid);
                throw e;
            } catch (InvalidSystemMetadata e) {
                removeSystemMetaAndIdentifier(pid);
                throw e;
            } catch (NotAuthorized e) {
                removeSystemMetaAndIdentifier(pid);
                throw e;
            } catch (Exception e) {
                removeSystemMetaAndIdentifier(pid);
                throw new ServiceFailure(
                    "1190", "The node is unable to create the object " + pid.getValue() + " since "
                    + e.getMessage());
            }

        }

        //}

        logMetacat.debug("Done inserting new object: " + pid.getValue());

        // setting the resulting identifier failed. We will check if the object does exist.
        try {
            if (localId == null || !IdentifierManager.getInstance()
                .objectFileExists(localId, isScienceMetadata)) {
                removeSystemMetaAndIdentifier(pid);
                throw new ServiceFailure(
                    "1190", "The Node is unable to create the object. " + pid.getValue());
            }
        } catch (PropertyNotFoundException e) {
            removeSystemMetaAndIdentifier(pid);
            throw new ServiceFailure(
                "1190", "The Node is unable to create the object. " + pid.getValue() + " since "
                + e.getMessage());
        }


        try {
            // submit for indexing
            MetacatSolrIndex.getInstance().submit(sysmeta.getIdentifier(), sysmeta, false);
        } catch (Exception e) {
            logMetacat.warn("Couldn't create solr index for object " + pid.getValue());
        }

        resultPid = pid;

        logMetacat.info("create() complete for object: " + pid.getValue());

        return resultPid;
    }


    /**
     * Determine if an object with the given identifier already exists or not.
     * (Using IdentityManager.  Works for SID or PID)
     * @param id -  the ID to be checked.
     * @throws ServiceFailure if the system can't fulfill the check process
     * @throws IdentifierNotUnique if the object with the identifier does exist
     */
    protected static void objectExists(Identifier id) throws ServiceFailure, IdentifierNotUnique {
        // Check that the identifier does not already exist
        boolean idExists = false;
        if (id == null) {
            throw new IdentifierNotUnique("1120", "The requested identifier can't be null.");
        }
        logMetacat.debug("Checking if identifier exists: " + id.getValue());
        try {
            idExists = IdentifierManager.getInstance().identifierExists(id.getValue());
        } catch (SQLException e) {
            throw new ServiceFailure("1190", "The requested identifier " + id.getValue()
                + " couldn't be determined if it is unique since : " + e.getMessage());
        }
        if (idExists) {
            throw new IdentifierNotUnique("1120", "The requested identifier " + id.getValue()
                + " is already used by another object and "
                + " therefore can not be used for this object. Clients should choose"
                + "a new identifier that is unique and retry the operation or "
                + "use CN.reserveIdentifier() to reserve one.");

        }
    }

    /*
     * Roll-back method when inserting data object fails.
     */
    protected void removeSystemMetaAndIdentifier(Identifier id) {
        if (id != null) {
            try {
                SystemMetadataManager.getInstance().delete(id);
                logMetacat.info("D1NodeService.removeSystemMeta - the system metadata of object "
                                    + id.getValue() + " has been removed from db tables since "
                                    + "the object creation failed");
                if (IdentifierManager.getInstance().mappingExists(id.getValue())) {
                    String localId = IdentifierManager.getInstance().getLocalId(id.getValue());
                    IdentifierManager.getInstance().removeMapping(id.getValue(), localId);
                    logMetacat.info(
                        "D1NodeService.removeSystemMeta - the identifier " + id.getValue()
                            + " and local id " + localId
                            + " have been removed from the identifier table since the object "
                            + "creation failed");
                }
            } catch (Exception e) {
                logMetacat.warn(
                    "D1NodeService.removeSysteMeta - can't decide if the mapping of  the pid "
                        + id.getValue() + " exists on the identifier table.");
            }
        }
    }

    /*
     * Roll-back method when inserting data object fails.
     */
    protected void removeSolrIndex(SystemMetadata sysMeta) {
        sysMeta.setSerialVersion(sysMeta.getSerialVersion().add(BigInteger.ONE));
        sysMeta.setArchived(true);
        //sysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
        try {
            //MetacatSolrIndex.getInstance().submit(sysMeta.getIdentifier(), sysMeta, null, false);
            MetacatSolrIndex.getInstance().submitDeleteTask(sysMeta.getIdentifier(), sysMeta);
        } catch (Exception e) {
            logMetacat.warn(
                "Can't remove the solr index for pid " + sysMeta.getIdentifier().getValue());
        }

    }

    /*
     * Roll-back method when inserting data object fails.
     */
    protected static void removeIdFromIdentifierTable(Identifier id) {
        if (id != null) {
            try {
                if (IdentifierManager.getInstance().mappingExists(id.getValue())) {
                    String localId = IdentifierManager.getInstance().getLocalId(id.getValue());
                    IdentifierManager.getInstance().removeMapping(id.getValue(), localId);
                    logMetacat.info(
                        "MNodeService.removeIdFromIdentifierTable - the identifier " + id.getValue()
                            + " and local id " + localId
                            + " have been removed from the identifier table since the object "
                            + "creation failed");
                }
            } catch (Exception e) {
                logMetacat.warn(
                    "MNodeService.removeIdFromIdentifierTable - can't decide if the mapping of  "
                        + "the pid "
                        + id.getValue() + " exists on the identifier table.");
            }
        }
    }

    /**
     * Return the log records associated with a given event between the start and
     * end dates listed given a particular Subject listed in the Session
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param fromDate - the start date of the desired log records
     * @param toDate - the end date of the desired log records
     * @param event - restrict log records of a specific event type
     * @param start - zero based offset from the first record in the
     *                set of matching log records. Used to assist with
     *                paging the response.
     * @param count - maximum number of log records to return in the response.
     *                Used to assist with paging the response.
     *
     * @return the desired log records
     *
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    public Log getLogRecords(
        Session session, Date fromDate, Date toDate, String event, String pidFilter, Integer start,
        Integer count)
        throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {

        // only admin access to this method
        // see https://redmine.dataone.org/issues/2855
        D1AuthHelper authDel = new D1AuthHelper(request, null, "1460", "1490");
        authDel.doAdminAuthorization(session);
        // "Only the CN or admin is allowed to harvest logs from this node";

        Log log = new Log();
        IdentifierManager im = IdentifierManager.getInstance();
        EventLog el = EventLog.getInstance();
        if (fromDate == null) {
            logMetacat.debug("setting fromdate from null");
            fromDate = new Date(1);
        }
        if (toDate == null) {
            logMetacat.debug("setting todate from null");
            toDate = new Date();
        }

        if (start == null) {
            start = 0;
        }

        if (count == null) {
            count = 1000;
        }

        // safeguard against large requests
        if (count > MAXIMUM_DB_RECORD_COUNT) {
            count = MAXIMUM_DB_RECORD_COUNT;
        }

        String[] filterDocid = null;
        if (pidFilter != null && !pidFilter.trim().equals("")) {
            //check if the given identifier is a sid. If it is sid, choose the current pid of the
            // sid.
            Identifier pid = new Identifier();
            pid.setValue(pidFilter);
            String serviceFailureCode = "1490";
            Identifier headPid = getPIDForSID(pid, serviceFailureCode);
            if (headPid != null) {
                pidFilter = headPid.getValue();  // replaces the identifier value
            }
            try {
                String localId = im.getLocalId(pidFilter);
                filterDocid = new String[]{localId};
            } catch (Exception ex) {
                String msg = "Could not find localId for given pidFilter '" + pidFilter + "'";
                logMetacat.warn(msg, ex);
                //throw new InvalidRequest("1480", msg);
                return log; //return 0 record
            }
        }

        logMetacat.debug("fromDate: " + fromDate);
        logMetacat.debug("toDate: " + toDate);

        log = el.getD1Report(null, null, filterDocid, event,
                             new java.sql.Timestamp(fromDate.getTime()),
                             new java.sql.Timestamp(toDate.getTime()), false, start, count);

        logMetacat.info("getLogRecords");
        return log;
    }

    /**
     * Return the object identified by the given object identifier
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param id - the identifier for the given object
     *
     * TODO: The D1 Authorization API doesn't provide information on which
     * authentication system the Subject belongs to, and so it's not possible to
     * discern which Person or Group is a valid KNB LDAP DN.  Fix this.
     *
     * @return inputStream - the input stream of the given object
     *
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    public InputStream get(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
        String serviceFailureCode = "1030";
        String notFoundCode = "1020";

        Identifier headPID = getPIDForSID(pid, notFoundCode);
        if (headPID != null) {
            pid = headPID;
        }

        InputStream inputStream = null; // bytes to be returned
        boolean allowed = false;
        String localId; // the metacat docid for the pid

        // get the local docid from Metacat
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());

        } catch (McdbDocNotFoundException e) {
            throw new NotFound(
                "1020", "The object specified by " + pid.getValue()
                + " does not exist at this node.");
        } catch (SQLException e) {
            throw new ServiceFailure(
                "1030", "The object specified by " + pid.getValue()
                + " couldn't be identified at this node since " + e.getMessage());
        }

        // delegate authorization to isAuthorized method
        try {
            allowed = isAuthorized(session, pid, Permission.READ);
        } catch (InvalidRequest e) {
            throw new ServiceFailure("1030", e.getDescription());
        }

        // if the person is authorized, perform the read
        if (allowed) {
            SystemMetadata sm = SystemMetadataManager.getInstance().get(pid);
            ObjectFormat objectFormat = null;
            String type = null;
            try {
                objectFormat = ObjectFormatCache.getInstance().getFormat(sm.getFormatId());
            } catch (BaseException be) {
                logMetacat.warn("Could not lookup ObjectFormat for: " + sm.getFormatId(), be);
            }
            if (objectFormat != null) {
                type = objectFormat.getFormatType();
            }
            logMetacat.info(
                "D1NodeService.get - the data type for the object " + pid.getValue() + " is "
                    + type);
            try {
                inputStream = MetacatHandler.read(localId, type);
            } catch (McdbDocNotFoundException de) {
                String error = "";
                try {
                    if (IdentifierManager.getInstance().existsInIdentifierTable(pid)) {
                        error = DELETEDMESSAGE;
                    }
                } catch (Exception e) {
                    logMetacat.warn(
                        "Can't determine if the pid " + pid.getValue() + " is deleted or not.");
                }

                throw new NotFound(
                    "1020", "The object specified by " + pid.getValue()
                    + " does not exist at this node. " + error);
            } catch (Exception e) {
                throw new ServiceFailure(
                    "1030", "The object specified by " + pid.getValue()
                    + " could not be returned due to error: " + e.getMessage() + ". ");
            }
        }

        // if we fail to set the input stream
        if (inputStream == null) {
            String error = "";
            try {
                if (IdentifierManager.getInstance().existsInIdentifierTable(pid)) {
                    error = DELETEDMESSAGE;
                }
            } catch (Exception e) {
                logMetacat.warn(
                    "Can't determine if the pid " + pid.getValue() + " is deleted or not.");
            }
            throw new NotFound(
                "1020", "The object specified by " + pid.getValue()
                + " does not exist at this node. " + error);
        }

        // log the read event
        String principal = Constants.SUBJECT_PUBLIC;
        if (session != null && session.getSubject() != null) {
            principal = session.getSubject().getValue();
        }
        EventLog.getInstance()
            .log(request.getRemoteAddr(), request.getHeader("User-Agent"), principal, localId,
                 "read");

        return inputStream;
    }

    /**
     * Return the system metadata for a given object
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid - the object identifier for the given object
     *
     * @return inputStream - the input stream of the given system metadata object
     *
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    public SystemMetadata getSystemMetadata(Session session, Identifier id)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        String serviceFailureCode = "1090";
        String notFoundCode = "1420";
        String notAuthorizedCode = "1040";
        String invalidTokenCode = "1050";
        boolean needDeleteInfo = true;

        Identifier HeadOfSid = getPIDForSID(id, serviceFailureCode);
        if (HeadOfSid != null) {
            id = HeadOfSid;
        }
        //SystemMetadata sysmeta = getSeriesHead(pid,serviceFailureCode,notFoundCode);
        SystemMetadata sysmeta = null;
        try {
            sysmeta =
                getSystemMetadataForPID(id, serviceFailureCode, invalidTokenCode, notFoundCode,
                                        needDeleteInfo);
        } catch (InvalidRequest e) {
            throw new InvalidToken(invalidTokenCode, e.getMessage());
        }
        D1AuthHelper authDel = new D1AuthHelper(request, id, notAuthorizedCode, serviceFailureCode);
        authDel.doGetSysmetaAuthorization(session, sysmeta, Permission.READ);
        return sysmeta;

    }


    /**
     * Test if the user identified by the provided token has authorization
     * for the operation on the specified object.
     * Allowed subjects include:
     * 1. CNs
     * 2. Authoritative node
     * 3. Owner of the object
     * 4. Users with the specified permission in the access rules.
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param pid - The identifer of the resource for which access is being checked
     * @param operation - The type of operation which is being requested for the given pid
     *
     * @return true if the operation is allowed
     *
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotFound
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    public boolean isAuthorized(Session session, Identifier id, Permission permission)
        throws ServiceFailure, InvalidToken, NotFound, NotAuthorized, NotImplemented,
        InvalidRequest {

        if (permission == null) {
            throw new InvalidRequest("1761", "Permission was not provided or is invalid");
        }


        String serviceFailureCode = "1760";
        String notFoundCode = "1800";
        String notAuthorizedCode = "1820";
        String invalidRequestCode = "1761";
        boolean needDeleteInfo = true;
        Identifier HeadOfSid = getPIDForSID(id, serviceFailureCode);
        if (HeadOfSid != null) {
            id = HeadOfSid;
        }
        //SystemMetadata sysmeta = getSeriesHead(pid,serviceFailureCode,notFoundCode);
        SystemMetadata sysmeta =
            getSystemMetadataForPID(id, serviceFailureCode, invalidRequestCode, notFoundCode,
                                    needDeleteInfo);

        D1AuthHelper authDel = new D1AuthHelper(request, id, notAuthorizedCode, serviceFailureCode);
        authDel.doIsAuthorized(session, sysmeta, permission);

        return true;
    }


    /*
     * parse a logEntry and get the relevant field from it
     *
     * @param fieldname
     * @param entry
     * @return
     */
    private String getLogEntryField(String fieldname, String entry) {
        String begin = "<" + fieldname + ">";
        String end = "</" + fieldname + ">";
        String s = entry.substring(entry.indexOf(begin) + begin.length(), entry.indexOf(end));
        logMetacat.debug("entry " + fieldname + " : " + s);
        return s;
    }

    /**
     * Determine if a given object should be treated as an XML science metadata
     * object.
     *
     * @param sysmeta - the SystemMetadata describing the object
     * @return true if the object should be treated as science metadata
     */
    public static boolean isScienceMetadata(SystemMetadata sysmeta) {

        ObjectFormat objectFormat = null;
        boolean isScienceMetadata = false;

        try {
            objectFormat = ObjectFormatCache.getInstance().getFormat(sysmeta.getFormatId());
            if (objectFormat.getFormatType().equals("METADATA")) {
                isScienceMetadata = true;

            }
        } catch (NotFound e) {
            logMetacat.debug("There was a problem determining if the object identified by" + sysmeta
                .getIdentifier().getValue() + " is science metadata: " + e.getMessage());

        }

        return isScienceMetadata;

    }

    /**
     * Check for whitespace in the given pid.
     * null pids are also invalid by default
     * @param pid
     * @return
     */
    public static boolean isValidIdentifier(Identifier pid) {
        boolean valid = true;
        if (pid != null && pid.getValue() != null && pid.getValue().length() > 0) {
            for (int i = 0; i < pid.getValue().length(); i++) {
                char ch = pid.getValue().charAt(i);
                if (Character.isWhitespace(ch)) {
                    valid = false;
                    break;
                }
            }
        } else {
            valid = false;
        }
        return valid;
    }


    /**
     * Insert or update an XML document into Metacat
     *
     * @param xml - the XML document to insert or update
     * @param pid - the identifier to be used for the resulting object
     *
     * @return localId - the resulting docid of the document created or updated
     *
     */
    public String insertOrUpdateDocument(
        InputStream xmlStream, String encoding, Identifier pid, Session session,
        String insertOrUpdate, String formatId, Checksum checksum)
        throws ServiceFailure, IOException, PropertyNotFoundException, InvalidSystemMetadata {

        logMetacat.debug("Starting to insert xml document...");
        IdentifierManager im = IdentifierManager.getInstance();

        String checksumValue = checksum.getValue();
        logMetacat.info(
            "D1NodeService.insertOrUpdateDocument - the checksum value from the system metadata is "
                + checksumValue + " for the metdata object " + pid.getValue());
        if (checksumValue == null || checksumValue.trim().equals("")) {
            logMetacat.error(
                "D1NodeService.insertOrUpdateDocument - the checksum value from the system "
                + "metadata shouldn't be null or blank for the metadata object "
                    + pid.getValue());
            throw new InvalidSystemMetadata(
                "1180",
                "The checksum value from the system metadata shouldn't be null or blank for the "
                + "ojbect "
                    + pid.getValue());
        }
        String algorithm = checksum.getAlgorithm();
        logMetacat.info(
            "D1NodeService.insertOrUpdateDocument - the algorithm to calculate the checksum from "
            + "the system metadata is "
                + algorithm + " for the metadata object " + pid.getValue());
        if (algorithm == null || algorithm.trim().equals("")) {
            logMetacat.error(
                "D1NodeService.insertOrUpdateDocument - the algorithm to calculate the checksum "
                + "from the system metadata shouldn't be null or blank for the metadata object "
                    + pid.getValue());
            throw new InvalidSystemMetadata(
                "1180",
                "The algorithm to calculate the checksum from the system metadata shouldn't be "
                + "null or blank for the metadata object "
                    + pid.getValue());
        }

        //if the input stream is an object DetailedFileInputStream, it means this object already
        // has the checksum information.
        File tempFile = null;
        if (xmlStream instanceof DetailedFileInputStream) {
            logMetacat.info(
                "D1NodeService.insertOrUpdateDocument - in the detailedFileInputstream branch");
            boolean checksumMatched = false;
            DetailedFileInputStream stream = (DetailedFileInputStream) xmlStream;
            tempFile = stream.getFile();
            Checksum expectedChecksum = stream.getExpectedChecksum();
            if (expectedChecksum != null) {
                String expectedAlgorithm = expectedChecksum.getAlgorithm();
                String expectedChecksumValue = expectedChecksum.getValue();
                if (expectedAlgorithm != null && expectedAlgorithm.equalsIgnoreCase(algorithm)) {
                    //The algorithm is the same and the checksum is same, we don't need to check
                    // the checksum again
                    if (expectedChecksumValue != null && expectedChecksumValue.equalsIgnoreCase(
                        checksumValue)) {
                        logMetacat.info(
                            "D1NodeService.insertOrUpdateDocument - Metacat already verified the "
                            + "checksum of the object "
                                + pid.getValue());
                        checksumMatched = true;
                    } else {
                        logMetacat.error(
                            "D1NodeService.insertOrUpdateDocument - the check sum calculated from"
                            + " the saved local file is "
                                + expectedChecksumValue
                                + ". But it doesn't match the value from the system metadata "
                                + checksumValue + " for the object " + pid.getValue());
                        throw new InvalidSystemMetadata(
                            "1180",
                            "D1NodeService.insertOrUpdateDocument - the check sum calculated from"
                            + " the saved local file is "
                                + expectedChecksumValue
                                + ". But it doesn't match the value from the system metadata "
                                + checksumValue + " for the object " + pid.getValue());
                    }
                }
            }
            if (!checksumMatched && tempFile != null) {
                logMetacat.info(
                    "D1NodeService.insertOrUpdateDocument - mark the temp file to be deleted on "
                    + "exist.");
                //tempFile.deleteOnExit(); //since we will write bytes from the stream to the
                // disk in this case, the temp file can be deleted when the programm ends.
                StreamingMultipartRequestResolver.deleteTempFile(tempFile);
                tempFile =
                    null; //tempFile being null implicitly means that Metacat will write the
                    // bytes to the final destination.
            }
        }
        // generate pid/localId pair for sysmeta
        String localId = null;
        byte[] xmlBytes = IOUtils.toByteArray(xmlStream);
        IOUtils.closeQuietly(xmlStream);
        String xmlStr = new String(xmlBytes, encoding);
        if (insertOrUpdate.equals("insert")) {
            localId = im.generateLocalId(pid.getValue(), 1);

        } else {
            //localid should already exist in the identifier table, so just find it
            try {
                logMetacat.debug("Updating pid " + pid.getValue());
                logMetacat.debug("looking in identifier table for pid " + pid.getValue());

                localId = im.getLocalId(pid.getValue());

                logMetacat.debug("localId: " + localId);
                //increment the revision
                String docid = localId.substring(0, localId.lastIndexOf("."));
                String revS = localId.substring(localId.lastIndexOf(".") + 1, localId.length());
                int rev = new Integer(revS).intValue();
                rev++;
                docid = docid + "." + rev;
                localId = docid;
                logMetacat.debug("incremented localId: " + localId);

            } catch (McdbDocNotFoundException e) {
                throw new ServiceFailure(
                    "1030", "D1NodeService.insertOrUpdateDocument(): " + "pid " + pid.getValue()
                    + " should have been in the identifier table, but it wasn't: "
                    + e.getMessage());

            } catch (SQLException e) {
                throw new ServiceFailure(
                    "1030", "D1NodeService.insertOrUpdateDocument() -"
                    + " couldn't identify if the pid " + pid.getValue()
                    + " is in the identifier table since " + e.getMessage());
            }

        }

        String username = Constants.SUBJECT_PUBLIC;
        String[] groupnames = null;
        if (session != null) {
            username = session.getSubject().getValue();
            Set<Subject> otherSubjects = AuthUtils.authorizedClientSubjects(session);
            if (otherSubjects != null) {
                groupnames = new String[otherSubjects.size()];
                int i = 0;
                Iterator<Subject> iter = otherSubjects.iterator();
                while (iter.hasNext()) {
                    groupnames[i] = iter.next().getValue();
                    i++;
                }
            }
        }

        // do the insert or update action
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        if (userAgent == null) {
            userAgent = request.getHeader("User-Agent");
        }
        long start = System.currentTimeMillis();
        String result =
            handler.handleInsertOrUpdateAction(ipAddress, userAgent, username,
                                               groupnames, encoding, xmlBytes, formatId,
                                               checksum, tempFile, localId, insertOrUpdate);
        long end = System.currentTimeMillis();
        logMetacat.info(edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG + pid.getValue()
                            + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD
                            + " Parse and write the metadata object into database (if the "
                            + "multiparts handler hasn't calculated the checksum, it will write "
                            + "the content to the disk again)"
                            + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_DURATION
                            + (end - start) / 1000);
        boolean isScienceMetadata = true;
        if (result.indexOf("<error>") != -1 || !IdentifierManager.getInstance()
            .objectFileExists(localId, isScienceMetadata)) {
            String detailCode = "";
            if (insertOrUpdate.equals("insert")) {
                // make sure to remove the mapping so that subsequent attempts do not fail with
                // IdentifierNotUnique
                im.removeMapping(pid.getValue(), localId);
                detailCode = "1190";

            } else if (insertOrUpdate.equals("update")) {
                detailCode = "1310";

            }
            logMetacat.error(
                "D1NodeService.insertOrUpdateDocument - Error inserting or updating document: "
                    + pid.getValue() + " since " + result);
            throw new ServiceFailure(detailCode,
                                     "Error inserting or updating document: " + pid.getValue()
                                         + " since " + result);
        }
        logMetacat.info(
            "D1NodeService.insertOrUpdateDocument - Finsished inserting xml document with local id "
                + localId + " and its pid is " + pid.getValue());
        return localId;
    }

    /**
     * Insert a data object into Metacat
     * @param object  the input stream of the object will be inserted
     * @param pid  the pid associated with the object
     * @param session  the actor of this action
     * @param checksum  the expected checksum for this data object
     * @return the local id of the inserted object
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws NotAuthorized
     */
    protected String insertDataObject(
        InputStream object, Identifier pid, Session session, Checksum checksum)
        throws ServiceFailure, InvalidSystemMetadata, NotAuthorized {
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        if (userAgent == null) {
            userAgent = request.getHeader("User-Agent");
        }
        EventLogData event = new EventLogData(ipAddress, userAgent, null, null, "create");
        return insertDataObject(object, pid, session, checksum, event);

    }

    /**
     * Insert a data object into Metacat
     * @param object  the input stream of the object will be inserted
     * @param pid  the pid associated with the object
     * @param session  the actor of this action
     * @param checksum  the expected checksum for this data object
     * @param event  the event log information associated with this action
     * @return the local id of the inserted object
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws NotAuthorized
     */
    protected String insertDataObject(
        InputStream object, Identifier pid, Session session, Checksum checksum, EventLogData event)
        throws ServiceFailure, InvalidSystemMetadata, NotAuthorized {
        String dataFilePath = null;
        try {
            dataFilePath = PropertyService.getProperty("application.datafilepath");
        } catch (PropertyNotFoundException e) {
            ServiceFailure sf =
                new ServiceFailure("1190", "Lookup data file path" + e.getMessage());
            sf.initCause(e);
            throw sf;
        }
        return insertObject(object, DocumentImpl.BIN, pid, dataFilePath, session, checksum, event);

    }

    /**
     * Insert an object into the given directory
     * @param object  the input stream of the object will be inserted
     * @param docType  the doc type in the xml_document table
     * @param pid  the pid associated with the object
     * @param fileDirectory  the directory where the object will be inserted
     * @param session  the actor of this action
     * @param checksum  the expected checksum for this data object
     * @param event  the event log information associated with this action
     * @return the local id of the inserted object
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws NotAuthorized
     */
    public static String insertObject(
        InputStream object, String docType, Identifier pid, String fileDirectory, Session session,
        Checksum checksum, EventLogData event)
        throws ServiceFailure, InvalidSystemMetadata, NotAuthorized {

        String username = Constants.SUBJECT_PUBLIC;
        String[] groupnames = null;
        if (session != null) {
            username = session.getSubject().getValue();
            Set<Subject> otherSubjects = AuthUtils.authorizedClientSubjects(session);
            if (otherSubjects != null) {
                groupnames = new String[otherSubjects.size()];
                int i = 0;
                Iterator<Subject> iter = otherSubjects.iterator();
                while (iter.hasNext()) {
                    groupnames[i] = iter.next().getValue();
                    i++;
                }
            }
        }

        //if the user and groups are in the white list (allowed submitters) of the metacat
        // configuration
        boolean inWhitelist = false;
        try {
            inWhitelist = AuthUtil.canInsertOrUpdate(username, groupnames);
        } catch (Exception e) {
            ServiceFailure sf = new ServiceFailure(
                "1190",
                "Could not determinte if the user is allowed to upload data objects to this "
                + "Metacat:"
                    + e.getMessage());
            logMetacat.error(
                "D1NodeService.insertDataObject Could not determinte if the user is allowed to "
                + "upload data objects to this Metacat: - "
                    + e.getMessage(), e);
            throw sf;
        }
        if (!inWhitelist) {
            logMetacat.error("D1NodeService.insertDataObject - The provided identity " + username
                                 + " does not have " + "permission to WRITE to the Node.");
            throw new NotAuthorized(
                "1100", "The provided identity " + username + " does not have "
                + "permission to WRITE to the Node.");
        }

        String localId = null;
        try {
            // generate pid/localId pair for object
            logMetacat.debug("Generating a pid/localId mapping");
            IdentifierManager im = IdentifierManager.getInstance();
            localId = im.generateLocalId(pid.getValue(), 1);

            // Save the data file to disk using "localId" as the name
            logMetacat.debug("Case DATA: starting to write to disk.");
            File dataDirectory = new File(fileDirectory);
            dataDirectory.mkdirs();
            File newFile = writeStreamToFile(dataDirectory, localId, object, checksum, pid);

            // TODO: Check that the file size matches SystemMetadata

            // Register the file in the database (which generates an exception
            // if the localId is not acceptable or other untoward things happen
            try {
                logMetacat.debug("Registering document...");
                DocumentImpl.registerDocument(localId, docType, localId, username, groupnames);
                logMetacat.debug("Registration step completed.");

            } catch (SQLException e) {
                logMetacat.debug("SQLE: " + e.getMessage());
                e.printStackTrace(System.out);
                throw new ServiceFailure("1190", "Registration failed: " + e.getMessage());

            } catch (AccessionNumberException e) {
                logMetacat.debug("ANE: " + e.getMessage());
                e.printStackTrace(System.out);
                throw new ServiceFailure("1190", "Registration failed: " + e.getMessage());

            } catch (Exception e) {
                logMetacat.debug("Exception: " + e.getMessage());
                e.printStackTrace(System.out);
                throw new ServiceFailure("1190", "Registration failed: " + e.getMessage());
            }

            try {
                logMetacat.debug("Logging the creation event.");
                EventLog.getInstance()
                    .log(event.getIpAddress(), event.getUserAgent(), username, localId,
                         event.getEvent());
            } catch (Exception e) {
                logMetacat.warn(
                    "D1NodeService.insertDataObject - can't log the create event for the object "
                        + pid.getValue());
            }

            // Schedule replication for this data file, the "insert" action is important here!
            logMetacat.debug("Scheduling replication.");
            boolean isMeta = true;
            if (docType != null && docType.equals(DocumentImpl.BIN)) {
                isMeta = false;
            }
        } catch (ServiceFailure sfe) {
            removeIdFromIdentifierTable(pid);
            throw sfe;
        } catch (InvalidSystemMetadata ise) {
            removeIdFromIdentifierTable(pid);
            throw ise;
        }
        return localId;
    }

    /**
     * Insert a systemMetadata document and return its localId
     */
    public void insertSystemMetadata(SystemMetadata sysmeta) throws ServiceFailure {

        logMetacat.debug("Starting to insert SystemMetadata...");

        //insert the system metadata
        try {
            SystemMetadataManager.getInstance().store(sysmeta);
            // submit for indexing
            MetacatSolrIndex.getInstance().submit(sysmeta.getIdentifier(), sysmeta, false);
        } catch (Exception e) {
            throw new ServiceFailure("1190", e.getMessage());
        }
    }

    /**
     * Retrieve the list of objects present on the MN that match the calling parameters
     *
     * @param session - the Session object containing the credentials for the Subject
     * @param startTime - Specifies the beginning of the time range from which
     *                    to return object (>=)
     * @param endTime - Specifies the beginning of the time range from which
     *                  to return object (>=)
     * @param objectFormat - Restrict results to the specified object format
     * @param replicaStatus - Indicates if replicated objects should be returned in the list
     * @param start - The zero-based index of the first value, relative to the
     *                first record of the resultset that matches the parameters.
     * @param count - The maximum number of entries that should be returned in
     *                the response. The Member Node may return less entries
     *                than specified in this value.
     *
     * @return objectList - the list of objects matching the criteria
     *
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     */
    public ObjectList listObjects(
        Session session, Date startTime, Date endTime, ObjectFormatIdentifier objectFormatId,
        Identifier identifier, NodeReference nodeId, Integer start, Integer count)
        throws NotAuthorized, InvalidRequest, NotImplemented, ServiceFailure, InvalidToken {

        ObjectList objectList = null;

        try {
            // safeguard against large requests
            if (count == null || count > MAXIMUM_DB_RECORD_COUNT) {
                count = MAXIMUM_DB_RECORD_COUNT;
            }
            boolean isSid = false;
            if (identifier != null) {
                isSid = IdentifierManager.getInstance().systemMetadataSIDExists(identifier);
            }
            objectList = IdentifierManager.getInstance()
                .querySystemMetadata(startTime, endTime, objectFormatId, nodeId, start, count,
                                     identifier, isSid);
        } catch (Exception e) {
            throw new ServiceFailure("1580", "Error querying system metadata: " + e.getMessage());
        }

        return objectList;
    }


    /**
     * Update a systemMetadata document
     *
     * @param sysMeta - the system metadata object in the system to update
     */
    protected void updateSystemMetadata(SystemMetadata sysMeta) throws ServiceFailure {
        logMetacat.debug("D1NodeService.updateSystemMetadata() called.");
        try {

            boolean needUpdateModificationDate = true;
            updateSystemMetadata(sysMeta, needUpdateModificationDate);
        } catch (Exception e) {
            throw new ServiceFailure("4862", e.getMessage());
        }
    }

    /**
     * Update system metadata.
     *
     * @param sysMeta
     * @param needUpdateModificationDate
     * @throws ServiceFailure
     */
    private void updateSystemMetadata(
        SystemMetadata sysMeta, boolean needUpdateModificationDate) throws ServiceFailure {
        logMetacat.debug("D1NodeService.updateSystemMetadataWithoutLock() called.");
        // submit for indexing
        try {
            SystemMetadataManager.getInstance().store(sysMeta, needUpdateModificationDate);
            boolean isSysmetaChangeOnly = true;
            MetacatSolrIndex.getInstance()
                .submit(sysMeta.getIdentifier(), sysMeta, isSysmetaChangeOnly, false);
        } catch (Exception e) {
            throw new ServiceFailure("4862", e.getMessage());
        }
    }

    /**
     * Update the system metadata of the specified pid.
     *
     * @param session - the identity of the client which calls the method
     * @param pid - the identifier of the object which will be updated
     * @param sysmeta - the new system metadata
     * @return
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     * @throws InvalidToken
     */
    protected boolean updateSystemMetadata(
        Session session, Identifier pid, SystemMetadata sysmeta, boolean needUpdateModificationDate,
        SystemMetadata currentSysmeta, boolean fromCN)
        throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest, InvalidSystemMetadata,
        InvalidToken {

        // The lock to be used for this identifier
        Lock lock = null;

        // verify that guid == SystemMetadata.getIdentifier()
        if (sysmeta.getIdentifier() == null) {
            throw new InvalidRequest(
                "4863", "The identifier in the system metadata shouldn't be null");
        } else if (!pid.getValue().equals(sysmeta.getIdentifier().getValue())) {
            throw new InvalidRequest("4863", "The identifier in method call (" + pid.getValue()
                + ") does not match identifier in system metadata (" + sysmeta.getIdentifier()
                .getValue() + ").");
        }
        //compare serial version.
        logMetacat.debug(
            "The current dateUploaded is ============" + currentSysmeta.getDateUploaded());
        logMetacat.debug(
            "the dateUploaded in the new system metadata is " + sysmeta.getDateUploaded());
        if (currentSysmeta == null) {
            //do we need throw an exception?
            logMetacat.warn(
                "D1NodeService.updateSystemMetadata: Currently there is no system metadata in "
                + "this node associated with the pid "
                    + pid.getValue());
        } else {
            Identifier currentSid = currentSysmeta.getSeriesId();
            if (currentSid != null) {
                logMetacat.debug(
                    "In the branch that the sid is not null in the current system metadata and "
                    + "the current sid is "
                        + currentSid.getValue());
                //new sid must match the current sid
                Identifier newSid = sysmeta.getSeriesId();
                if (!isValidIdentifier(newSid)) {
                    throw new InvalidRequest(
                        "4869", "The series id in the system metadata is invalid in the request.");
                } else {
                    if (!newSid.getValue().equals(currentSid.getValue())) {
                        throw new InvalidRequest(
                            "4869", "The series id " + newSid.getValue()
                            + " in the system metadata doesn't match the current sid "
                            + currentSid.getValue());
                    }
                }
            } else {
                //current system metadata doesn't have a sid. So we can have those scenarios
                //1. The new sid may be null as well
                //2. If the new sid does exist, it may be an identifier which hasn't bee used.
                //3. If the new sid does exist, it may be an sid which equals the SID it obsoletes
                //4. If the new sid does exist, it may be an sid which equauls the SID it was
                // obsoleted by
                Identifier newSid = sysmeta.getSeriesId();
                if (newSid != null) {
                    //It matches the rules of the checkSidInModifyingSystemMetadata
                    checkSidInModifyingSystemMetadata(sysmeta, "4956", "4868");
                }
            }
            if (sysmeta.getFormatId() == null) {
                throw new InvalidRequest(
                    "4869",
                    "The formatId field is requried and shouldn't be null on the new system "
                    + "metadata of the object "
                        + sysmeta.getIdentifier().getValue());
            }
            if (sysmeta.getRightsHolder() == null) {
                throw new InvalidRequest(
                    "4869",
                    "The rightsHolder field is requried and shouldn't be null on the new system "
                    + "metadata of the object "
                        + sysmeta.getIdentifier().getValue());
            }
            checkModifiedImmutableFields(currentSysmeta, sysmeta);
            checkOneTimeSettableSysmMetaFields(currentSysmeta, sysmeta);
            if (currentSysmeta.getObsoletes() == null && sysmeta.getObsoletes() != null) {
                //we are setting a value to the obsoletes field, so we should make sure if there
                // is not object obsoletes the value
                String obsoletes = existsInObsoletes(sysmeta.getObsoletes());
                if (obsoletes != null) {
                    throw new InvalidSystemMetadata(
                        "4956", "There is an object with id " + obsoletes
                        + " already obsoletes the pid " + sysmeta.getObsoletes().getValue()
                        + ". You can't set the object " + pid.getValue() + " to obsolete the pid "
                        + sysmeta.getObsoletes().getValue() + " again.");
                }
            }
            checkCircularObsoletesChain(sysmeta);
            if (currentSysmeta.getObsoletedBy() == null && sysmeta.getObsoletedBy() != null) {
                //we are setting a value to the obsoletedBy field, so we should make sure that
                // the no another object obsoletes the pid we are updating.
                String obsoletedBy = existsInObsoletedBy(sysmeta.getObsoletedBy());
                if (obsoletedBy != null) {
                    throw new InvalidSystemMetadata(
                        "4956", "There is an object with id " + obsoletedBy
                        + " already is obsoleted by the pid " + sysmeta.getObsoletedBy().getValue()
                        + ". You can't set the pid " + pid.getValue()
                        + " to be obsoleted by the pid " + sysmeta.getObsoletedBy().getValue()
                        + " again.");
                }
            }
            checkCircularObsoletedByChain(sysmeta);
        }

        // do the actual update
        if (sysmeta.getArchived() != null && sysmeta.getArchived() == true && (
            (currentSysmeta.getArchived() != null && currentSysmeta.getArchived() == false)
                || currentSysmeta.getArchived() == null)) {
            boolean logArchive =
                false;//we log it as the update system metadata event. So don't log it again.
            if (fromCN) {
                logMetacat.debug(
                    "D1Node.update - this is to archive a cn object " + pid.getValue());
                try {
                    archiveCNObject(logArchive, session, pid, sysmeta, needUpdateModificationDate);
                } catch (NotFound e) {
                    throw new InvalidRequest(
                        "4869", "Can't find the pid " + pid.getValue() + " for archive.");
                }
            } else {
                logMetacat.debug(
                    "D1Node.update - this is to archive a MN object " + pid.getValue());
                try {
                    String quotaSubject = request.getHeader(QuotaServiceManager.QUOTASUBJECTHEADER);
                    QuotaServiceManager.getInstance()
                        .enforce(quotaSubject, session.getSubject(), sysmeta,
                                 QuotaServiceManager.ARCHIVEMETHOD);
                    archiveObject(logArchive, session, pid, sysmeta, needUpdateModificationDate);
                } catch (NotFound e) {
                    throw new InvalidRequest(
                        "4869", "Can't find the pid " + pid.getValue() + " for archive.");
                } catch (InsufficientResources e) {
                    throw new InvalidRequest(
                        "4869", "The user doesn't have enough quota to archive the pid "
                        + pid.getValue() + " since " + e.getMessage());
                }
            }
        } else {
            logMetacat.debug("D1Node.update - regularly update the system metadata of the pid "
                                 + pid.getValue());
            updateSystemMetadata(sysmeta, needUpdateModificationDate);
        }

        try {
            String localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
            EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"),
                                       session.getSubject().getValue(), localId,
                                       "updateSystemMetadata");
        } catch (McdbDocNotFoundException e) {
            // do nothing, no localId to log with
            logMetacat.warn(
                "Could not log 'updateSystemMetadata' event because no localId was found for pid: "
                    + pid.getValue());
        } catch (SQLException e) {
            logMetacat.warn(
                "Could not log 'updateSystemMetadata' event because the localId couldn't be "
                + "identified for the pid: " + pid.getValue());
        }
        return true;
    }


    /*
     * Check if the newMeta modifies an immutable field.
     */
    private void checkModifiedImmutableFields(SystemMetadata orgMeta, SystemMetadata newMeta)
        throws InvalidRequest {
        logMetacat.debug("in the start of the checkModifiedImmutableFields method");
        if (orgMeta != null && newMeta != null) {
            logMetacat.debug(
                "in the checkModifiedImmutableFields method when the org and new system metadata "
                + "is not null");
            if (newMeta.getIdentifier() == null) {
                throw new InvalidRequest(
                    "4869",
                    "The new version of the system metadata is invalid since the identifier is "
                    + "null");
            }
            if (!orgMeta.getIdentifier().equals(newMeta.getIdentifier())) {
                throw new InvalidRequest("4869",
                                         "The request is trying to modify an immutable field in "
                                         + "the SystemMeta: the new system meta's identifier "
                                             + newMeta.getIdentifier().getValue() + " is "
                                             + "different to the orginal one " + orgMeta
                                             .getIdentifier().getValue());
            }
            if (newMeta.getSize() == null) {
                throw new InvalidRequest(
                    "4869",
                    "The new version of the system metadata is invalid since the size is null");
            }
            if (!orgMeta.getSize().equals(newMeta.getSize())) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's size "
                        + newMeta.getSize().longValue() + " is " + "different to the orginal one "
                        + orgMeta.getSize().longValue());
            }
            if (newMeta.getChecksum() != null && orgMeta.getChecksum() != null) {
                if (!orgMeta.getChecksum().getValue().equals(newMeta.getChecksum().getValue())) {
                    logMetacat.error(
                        "The request is trying to modify an immutable field in the SystemMeta: "
                        + "the new system meta's checksum "
                            + newMeta.getChecksum().getValue() + " is "
                            + "different to the orginal one " + orgMeta.getChecksum().getValue());
                    throw new InvalidRequest(
                        "4869",
                        "The request is trying to modify an immutable field in the SystemMeta: "
                        + "the new system meta's checksum "
                            + newMeta.getChecksum().getValue() + " is "
                            + "different to the orginal one " + orgMeta.getChecksum().getValue());
                }
                if (!orgMeta.getChecksum().getAlgorithm()
                    .equals(newMeta.getChecksum().getAlgorithm())) {
                    logMetacat.error(
                        "The request is trying to modify an immutable field in the SystemMeta: "
                        + "the new system meta's checksum algorithm "
                            + newMeta.getChecksum().getAlgorithm() + " is "
                            + "different to the orginal one " + orgMeta.getChecksum()
                            .getAlgorithm());
                    throw new InvalidRequest(
                        "4869",
                        "The request is trying to modify an immutable field in the SystemMeta: "
                        + "the new system meta's checksum algorithm "
                            + newMeta.getChecksum().getAlgorithm() + " is "
                            + "different to the orginal one " + orgMeta.getChecksum()
                            .getAlgorithm());
                }
            } else if (orgMeta.getChecksum() != null && newMeta.getChecksum() == null) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's checksum is null and it is "
                        + "different to the orginal one " + orgMeta.getChecksum().getValue());
            }

            if (orgMeta.getSubmitter() != null) {
                logMetacat.debug(
                    "in the checkModifiedImmutableFields method and orgMeta.getSubmitter is not "
                    + "null and the orginal submiter is "
                        + orgMeta.getSubmitter().getValue());
            }

            if (newMeta.getSubmitter() != null) {
                logMetacat.debug(
                    "in the checkModifiedImmutableFields method and newMeta.getSubmitter is not "
                    + "null and the submiter in the new system metadata is "
                        + newMeta.getSubmitter().getValue());
            }
            if (orgMeta.getSubmitter() != null && newMeta.getSubmitter() != null && !orgMeta
                .getSubmitter().equals(newMeta.getSubmitter())) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's submitter "
                        + newMeta.getSubmitter().getValue() + " is "
                        + "different to the orginal one " + orgMeta.getSubmitter().getValue());
            } else if (orgMeta.getSubmitter() != null && newMeta.getSubmitter() == null) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's submitter is null and it is "
                        + "different to the orginal one " + orgMeta.getSubmitter().getValue());
            }

            if (orgMeta.getDateUploaded() != null && newMeta.getDateUploaded() != null
                && orgMeta.getDateUploaded().getTime() != newMeta.getDateUploaded().getTime()) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's date of uploaded "
                        + newMeta.getDateUploaded() + " is " + "different to the orginal one "
                        + orgMeta.getDateUploaded());
            } else if (orgMeta.getDateUploaded() != null && newMeta.getDateUploaded() == null) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's date of uploaded is null and it is "
                        + "different to the orginal one " + orgMeta.getDateUploaded());
            }

            if (orgMeta.getOriginMemberNode() != null && newMeta.getOriginMemberNode() != null
                && !orgMeta.getOriginMemberNode().equals(newMeta.getOriginMemberNode())) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's orginal member node  "
                        + newMeta.getOriginMemberNode().getValue() + " is "
                        + "different to the orginal one " + orgMeta.getOriginMemberNode()
                        .getValue());
            } else if (orgMeta.getOriginMemberNode() != null
                && newMeta.getOriginMemberNode() == null) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's orginal member node is null and it "
                        + " is " + "different to the orginal one " + orgMeta.getOriginMemberNode()
                        .getValue());
            }

            if (orgMeta.getSeriesId() != null && newMeta.getSeriesId() != null && !orgMeta
                .getSeriesId().equals(newMeta.getSeriesId())) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's series id  "
                        + newMeta.getSeriesId().getValue() + " is "
                        + "different to the orginal one " + orgMeta.getSeriesId().getValue());
            } else if (orgMeta.getSeriesId() != null && newMeta.getSeriesId() == null) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to modify an immutable field in the SystemMeta: the "
                    + "new system meta's series id is null and it is "
                        + "different to the orginal one " + orgMeta.getSeriesId().getValue());
            }

        }
    }

    /*
     * Some fields in the system metadata, such as obsoletes or obsoletedBy can be set only once.
     * After set, they are not allowed to be changed.
     */
    private void checkOneTimeSettableSysmMetaFields(SystemMetadata orgMeta, SystemMetadata newMeta)
        throws InvalidRequest {
        if (orgMeta.getObsoletedBy() != null) {
            if (newMeta.getObsoletedBy() == null || !orgMeta.getObsoletedBy()
                .equals(newMeta.getObsoletedBy())) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to reset the obsoletedBy field in the System Metadata "
                    + "of the Object "
                        + orgMeta.getIdentifier().getValue()
                        + ". This is illegal since the obsoletedBy field is already set and "
                        + "cannot be changed once set.");
            }
        }
        if (orgMeta.getObsoletes() != null) {
            if (newMeta.getObsoletes() == null || !orgMeta.getObsoletes()
                .equals(newMeta.getObsoletes())) {
                throw new InvalidRequest(
                    "4869",
                    "The request is trying to reset the obsoletes field in the System Metadata of"
                    + " the Object "
                        + orgMeta.getIdentifier().getValue()
                        + ". This is illegal since the obsoletes field is already set and cannot "
                        + "be changed once set.");
            }
        }
    }


    /**
     * Try to check the scenario of a circular obsoletes chain:
     * A obsoletes B
     * B obsoletes C
     * C obsoletes A
     * @param sys
     * @throws InvalidRequest
     */
    private void checkCircularObsoletesChain(SystemMetadata sys)
        throws InvalidRequest, ServiceFailure {
        if (sys != null && sys.getObsoletes() != null && sys.getObsoletes().getValue() != null
            && !sys.getObsoletes().getValue().trim().equals("")) {
            logMetacat.debug(
                "D1NodeService.checkCircularObsoletesChain - the object " + sys.getIdentifier()
                    .getValue() + " obsoletes " + sys.getObsoletes().getValue());
            if (sys.getObsoletes().getValue().equals(sys.getIdentifier().getValue())) {
                // the obsoletes field points to itself and creates a circular chain
                throw new InvalidRequest(
                    "4869",
                    "The obsoletes field and identifier of the system metadata has the same value "
                        + sys.getObsoletes().getValue()
                        + ". This creates a circular chain and it is illegal.");
            } else {
                Vector<Identifier> pidList = new Vector<Identifier>();
                pidList.add(sys.getIdentifier());
                SystemMetadata obsoletesSym =
                    SystemMetadataManager.getInstance().get(sys.getObsoletes());
                while (obsoletesSym != null && obsoletesSym.getObsoletes() != null
                    && obsoletesSym.getObsoletes().getValue() != null && !obsoletesSym
                    .getObsoletes().getValue().trim().equals("")) {
                    pidList.add(obsoletesSym.getIdentifier());
                    logMetacat.debug(
                        "D1NodeService.checkCircularObsoletesChain - the object " + obsoletesSym
                            .getIdentifier().getValue() + " obsoletes " + obsoletesSym
                            .getObsoletes().getValue());

                    if (pidList.contains(obsoletesSym.getObsoletes())) {
                        logMetacat.error(
                            "D1NodeService.checkCircularObsoletesChain - when Metacat updated the"
                            + " system metadata of object "
                                + sys.getIdentifier().getValue()
                                + ", it found the obsoletes field value " + sys.getObsoletes()
                                .getValue()
                                + " in its new system metadata creating a circular chain at the "
                                + "object "
                                + obsoletesSym.getObsoletes().getValue() + ". This is illegal");
                        throw new InvalidRequest(
                            "4869", "When Metacat updated the system metadata of object " + sys
                            .getIdentifier().getValue() + ", it found the obsoletes field value "
                            + sys.getObsoletes().getValue()
                            + " in its new system metadata creating a circular chain at the object "
                            + obsoletesSym.getObsoletes().getValue() + ". This is illegal");
                    } else {
                        obsoletesSym =
                            SystemMetadataManager.getInstance().get(obsoletesSym.getObsoletes());
                    }
                }
            }
        }
    }


    /**
     * Try to check the scenario of a circular obsoletedBy chain:
     * A obsoletedBy B
     * B obsoletedBy C
     * C obsoletedBy A
     * @param sys
     * @throws InvalidRequest
     */
    private void checkCircularObsoletedByChain(SystemMetadata sys)
        throws InvalidRequest, ServiceFailure {
        if (sys != null && sys.getObsoletedBy() != null && sys.getObsoletedBy().getValue() != null
            && !sys.getObsoletedBy().getValue().trim().equals("")) {
            logMetacat.debug(
                "D1NodeService.checkCircularObsoletedByChain - the object " + sys.getIdentifier()
                    .getValue() + " is obsoletedBy " + sys.getObsoletedBy().getValue());
            if (sys.getObsoletedBy().getValue().equals(sys.getIdentifier().getValue())) {
                // the obsoletedBy field points to itself and creates a circular chain
                throw new InvalidRequest(
                    "4869",
                    "The obsoletedBy field and identifier of the system metadata has the same "
                    + "value "
                        + sys.getObsoletedBy().getValue()
                        + ". This creates a circular chain and it is illegal.");
            } else {
                Vector<Identifier> pidList = new Vector<Identifier>();
                pidList.add(sys.getIdentifier());
                SystemMetadata obsoletedBySym =
                    SystemMetadataManager.getInstance().get(sys.getObsoletedBy());
                while (obsoletedBySym != null && obsoletedBySym.getObsoletedBy() != null
                    && obsoletedBySym.getObsoletedBy().getValue() != null && !obsoletedBySym
                    .getObsoletedBy().getValue().trim().equals("")) {
                    pidList.add(obsoletedBySym.getIdentifier());
                    logMetacat.debug(
                        "D1NodeService.checkCircularObsoletedByChain - the object " + obsoletedBySym
                            .getIdentifier().getValue() + " is obsoletedBy " + obsoletedBySym
                            .getObsoletedBy().getValue());

                    if (pidList.contains(obsoletedBySym.getObsoletedBy())) {
                        logMetacat.error(
                            "D1NodeService.checkCircularObsoletedByChain - When Metacat updated "
                            + "the system metadata of object "
                                + sys.getIdentifier().getValue()
                                + ", it found the obsoletedBy field value " + sys.getObsoletedBy()
                                .getValue()
                                + " in its new system metadata creating a circular chain at the "
                                + "object "
                                + obsoletedBySym.getObsoletedBy().getValue() + ". This is illegal");
                        throw new InvalidRequest(
                            "4869", "When Metacat updated the system metadata of object " + sys
                            .getIdentifier().getValue() + ", it found the obsoletedBy field value "
                            + sys.getObsoletedBy().getValue()
                            + " in its new system metadata creating a circular chain at the object "
                            + obsoletedBySym.getObsoletedBy().getValue() + ". This is illegal");
                    } else {
                        obsoletedBySym = SystemMetadataManager.getInstance()
                            .get(obsoletedBySym.getObsoletedBy());
                    }
                }
            }
        }
    }

    /**
     * Given a Permission, returns a list of all permissions that it encompasses
     * Permissions are hierarchical so that WRITE also allows READ.
     * @param permission
     * @return list of included Permissions for the given permission
     */
    protected static List<Permission> expandPermissions(Permission permission) {
        List<Permission> expandedPermissions = new ArrayList<Permission>();
        if (permission.equals(Permission.READ)) {
            expandedPermissions.add(Permission.READ);
        }
        if (permission.equals(Permission.WRITE)) {
            expandedPermissions.add(Permission.READ);
            expandedPermissions.add(Permission.WRITE);
        }
        if (permission.equals(Permission.CHANGE_PERMISSION)) {
            expandedPermissions.add(Permission.READ);
            expandedPermissions.add(Permission.WRITE);
            expandedPermissions.add(Permission.CHANGE_PERMISSION);
        }
        return expandedPermissions;
    }

    /*
     * Write a stream to a file
     *
     * @param dir - the directory to write to
     * @param localId - the file name to write to
     * @param data - the object bytes as an input stream
     *
     * @return newFile - the new file created
     *
     * @throws ServiceFailure
     */
    public static File writeStreamToFile(
        File dir, String localId, InputStream dataStream, Checksum checksum, Identifier pid)
        throws ServiceFailure, InvalidSystemMetadata {
        File newFile = null;

        logMetacat.debug("Case DATA: starting to write to disk.");
        newFile = new File(dir, localId);
        File tempFile = null;
        logMetacat.debug(
            "Filename for write is: " + newFile.getAbsolutePath() + " for the data object pid "
                + pid.getValue());

        try {
            String checksumValue = checksum.getValue();
            logMetacat.info(
                "D1NodeService.writeStreamToFile - the checksum value from the system "
                + "metadata is "
                    + checksumValue + " for the data object " + pid.getValue());
            if (checksumValue == null || checksumValue.trim().equals("")) {
                logMetacat.error(
                    "D1NodeService.writeStreamToFile - the checksum value from the system "
                    + "metadata shouldn't be null or blank for the data object "
                        + pid.getValue());
                throw new InvalidSystemMetadata(
                    "1180",
                    "The checksum value from the system metadata shouldn't be null or blank.");
            }
            String algorithm = checksum.getAlgorithm();
            logMetacat.info(
                "D1NodeService.writeStreamToFile - the algorithm to calculate the checksum "
                + "from the system metadata is "
                    + algorithm + " for the data object " + pid.getValue());
            if (algorithm == null || algorithm.trim().equals("")) {
                logMetacat.error(
                    "D1NodeService.writeStreamToFile - the algorithm to calculate the "
                    + "checksum from the system metadata shouldn't be null or blank for the "
                    + "data object "
                        + pid.getValue());
                throw new InvalidSystemMetadata(
                    "1180",
                    "The algorithm to calculate the checksum from the system metadata "
                    + "shouldn't be null or blank.");
            }
            long start = System.currentTimeMillis();
            //if the input stream is an object DetailedFileInputStream, it means this object
            // already has the checksum information.
            if (dataStream instanceof DetailedFileInputStream) {
                DetailedFileInputStream stream = (DetailedFileInputStream) dataStream;
                tempFile = stream.getFile();
                Checksum expectedChecksum = stream.getExpectedChecksum();
                if (expectedChecksum != null) {
                    String expectedAlgorithm = expectedChecksum.getAlgorithm();
                    String expectedChecksumValue = expectedChecksum.getValue();
                    if (expectedAlgorithm != null && expectedAlgorithm.equalsIgnoreCase(
                        algorithm)) {
                        //The algorithm is the same and the checksum is same, we just need to
                        // move the file from the temporary location (serialized by the
                        // multiple parts handler)  to the permanent location
                        if (expectedChecksumValue != null
                            && expectedChecksumValue.equalsIgnoreCase(checksumValue)) {
                            FileUtils.moveFile(tempFile, newFile);
                            long end = System.currentTimeMillis();
                            logMetacat.info(
                                "D1NodeService.writeStreamToFile - Metacat only needs the "
                                + "move the data file from temporary location to the "
                                + "permanent location for the object "
                                    + pid.getValue());
                            logMetacat.info(
                                edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG
                                    + pid.getValue()
                                    + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD
                                    + " Only move the data file from the temporary location "
                                    + "to the permanent location since the multiparts handler"
                                    + " has calculated the checksum"
                                    + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_DURATION
                                    + (end - start) / 1000);
                            return newFile;
                        } else {
                            logMetacat.error(
                                "D1NodeService.writeStreamToFile - the check sum calculated "
                                + "from the saved local file is "
                                    + expectedChecksumValue
                                    + ". But it doesn't match the value from the system "
                                    + "metadata "
                                    + checksumValue + " for the object " + pid.getValue());
                            throw new InvalidSystemMetadata(
                                "1180",
                                "D1NodeService.writeStreamToFile - the check sum calculated "
                                + "from the saved local file is "
                                    + expectedChecksumValue
                                    + ". But it doesn't match the value from the system "
                                    + "metadata "
                                    + checksumValue + " for the object " + pid.getValue());
                        }
                    } else {
                        logMetacat.info(
                            "D1NodeService.writeStreamToFile - the checksum algorithm which "
                            + "the multipart handler used is "
                                + expectedAlgorithm
                                + " and it is different to one on the system metadata "
                                + algorithm + ". So we have to calculate again.");
                    }
                }
            }
            //The input stream is not a DetaileFileInputStream or the algorithm doesn't
            // match, we have to calculate the checksum.
            MessageDigest md = MessageDigest.getInstance(algorithm);
            // write data stream to desired file
            DigestOutputStream os = new DigestOutputStream(new FileOutputStream(newFile), md);
            long length = IOUtils.copyLarge(dataStream, os);
            os.flush();
            os.close();
            String localChecksum = DatatypeConverter.printHexBinary(md.digest());
            logMetacat.info(
                "D1NodeService.writeStreamToFile - the check sum calculated from the saved "
                + "local file is "
                    + localChecksum);
            if (localChecksum == null || localChecksum.trim().equals("")
                || !localChecksum.equalsIgnoreCase(checksumValue)) {
                logMetacat.error(
                    "D1NodeService.writeStreamToFile - the check sum calculated from the "
                    + "saved local file is "
                        + localChecksum
                        + ". But it doesn't match the value from the system metadata "
                        + checksumValue + " for the object " + pid.getValue());
                boolean success = newFile.delete();
                logMetacat.info(
                    "delete the file " + newFile.getAbsolutePath() + " for the object "
                        + pid.getValue() + " sucessfully?" + success);
                throw new InvalidSystemMetadata(
                    "1180", "The checksum calculated from the saved local file is "
                    + localChecksum
                    + ". But it doesn't match the value from the system metadata "
                    + checksumValue + ".");
            }
            long end = System.currentTimeMillis();
            logMetacat.info(
                edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG + pid.getValue()
                    + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD
                    + " Need to read the data file from the temporary location and write it "
                    + "to the permanent location since the multiparts handler has NOT "
                    + "calculated the checksum"
                    + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_DURATION
                    + (end - start) / 1000);
            if (tempFile != null) {
                StreamingMultipartRequestResolver.deleteTempFile(tempFile);
            }
        } catch (FileNotFoundException e) {
            logMetacat.error(
                "FNF: " + e.getMessage() + " for the data object " + pid.getValue(), e);
            throw new ServiceFailure(
                "1190", "File not found: " + localId + " " + e.getMessage());
        } catch (IOException e) {
            logMetacat.error(
                "IOE: " + e.getMessage() + " for the data object " + pid.getValue(), e);
            throw new ServiceFailure(
                "1190", "File was not written: " + localId + " " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logMetacat.error(
                "D1NodeService.writeStreamToFile - no such checksum algorithm exception "
                    + e.getMessage() + " for the data object " + pid.getValue(), e);
            throw new ServiceFailure(
                "1190", "No such checksum algorithm: " + " " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(dataStream);
        }
        return newFile;
    }

    /**
     * Calls CN.listNodes() to assemble a list of nodes that have been registered with the
     * DataONE infrastructure
     * that match the given session subject
     * @param subject - the subject serving as the filter.
     * @return nodes - List of nodes from the registry with a matching session subject
     *
     * @throws ServiceFailure
     * @throws NotImplemented
     */
    protected List<Node> listNodesBySubject(Subject subject, NodeList nodelist)
        throws ServiceFailure, NotImplemented {
        List<Node> nodeList = new ArrayList<Node>();

        List<Node> nodes = nodelist.getNodeList();

        // find the node in the node list
        for (Node node : nodes) {

            List<Subject> nodeSubjects = node.getSubjectList();
            if (nodeSubjects != null) {
                // check if the session subject is in the node subject list
                for (Subject nodeSubject : nodeSubjects) {
                    if (nodeSubject.equals(subject)) { // subject of session == node subject
                        nodeList.add(node);
                    }
                }
            }
        }

        return nodeList;
    }

    /**
     * Archives an object, where the object is either a
     * data object or a science metadata object.
     * Note: it doesn't check the authorization; it doesn't lock the system metadata;it only
     * accept pid.
     * @param session - the Session object containing the credentials for the Subject
     * @param pid - The object identifier to be archived
     *
     * @return pid - the identifier of the object used for the archiving
     *
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    protected Identifier archiveObject(
        boolean log, Session session, Identifier pid, SystemMetadata sysMeta,
        boolean needModifyDate)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        String localId = null;
        String username = Constants.SUBJECT_PUBLIC;
        if (session == null) {
            throw new InvalidToken("1330", "No session has been provided");
        } else {
            username = session.getSubject().getValue();
        }
        // do we have a valid pid?
        if (pid == null || pid.getValue().trim().equals("")) {
            throw new ServiceFailure("1350", "The provided identifier was invalid.");
        }

        if (sysMeta == null) {
            throw new NotFound(
                "2911", "There is no system metadata associated with " + pid.getValue());
        }

        // check for the existing identifier
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        } catch (McdbDocNotFoundException e) {
            throw new NotFound(
                "1340", "The object with the provided " + "identifier was not found.");
        } catch (SQLException e) {
            throw new ServiceFailure(
                "1350", "The object with the provided identifier " + pid.getValue()
                + " couldn't be identified since " + e.getMessage());
        }
        try {
            DocumentImpl.archive(localId, pid, username);
            if (log) {
                try {
                    EventLog.getInstance()
                        .log(request.getRemoteAddr(), request.getHeader("User-Agent"), username,
                             localId, Event.DELETE.xmlValue());
                } catch (Exception e) {
                    logMetacat.warn(
                        "D1NodeService.archiveObject - can't log the delete event since "
                            + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new ServiceFailure(
                "1350", "There was a problem archiving the object." + "The error message was: "
                + e.getMessage());
        } catch (Exception e) { // for some reason DocumentImpl throws a general Exception
            throw new ServiceFailure(
                "1350", "There was a problem archiving the object." + "The error message was: "
                + e.getMessage());
        }


        return pid;
    }

    /**
     * Archive a object on cn and notify the replica. This method doesn't lock the system
     * metadata map. The caller should lock it.
     * This method doesn't check the authorization; this method only accept a pid.
     * It wouldn't notify the replca that the system metadata has been changed.
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
    protected void archiveCNObject(
        boolean log, Session session, Identifier pid, SystemMetadata sysMeta,
        boolean needModifyDate)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

        String localId = null; // The corresponding docid for this pid

        // Check for the existing identifier
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
            archiveObject(log, session, pid, sysMeta, needModifyDate);

        } catch (McdbDocNotFoundException e) {
            // This object is not registered in the identifier table. Assume it is of formatType
            // DATA, and set the archive flag. (i.e. the *object* doesn't exist on the CN)

            try {
                if (sysMeta != null) {
                    sysMeta.setArchived(true);

                    try {
                        SystemMetadataManager.getInstance().store(sysMeta, needModifyDate);
                    } catch (InvalidRequest ee) {
                        throw new InvalidToken(
                            "4972", "Couldn't archive the object " + pid.getValue()
                            + ". Couldn't obtain the system metadata record.");
                    }
                } else {
                    throw new ServiceFailure(
                        "4972", "Couldn't archive the object " + pid.getValue()
                        + ". Couldn't obtain the system metadata record.");

                }

            } catch (RuntimeException re) {
                throw new ServiceFailure(
                    "4972", "Couldn't archive " + pid.getValue() + ". The error message was: "
                    + re.getMessage());

            }

        } catch (SQLException e) {
            throw new ServiceFailure(
                "4972", "Couldn't archive the object " + pid.getValue()
                + ". The local id of the object with the identifier can't be identified since "
                + e.getMessage());
        }

    }


    /**
     * A utility method for v1 api to check the specified identifier exists as a pid
     * Uses the IdentifierManager to call the Identifier table directly - this detects
     * Identifiers for deleted objects (where the SystemMetadata doesn't exist, but the
     * Identifier remains)
     * @param identifier  the specified identifier
     * @param serviceFailureCode  the detail error code for the service failure exception
     * @param noFoundCode  the detail error code for the not found exception
     * @throws ServiceFailure
     * @throws NotFound
     */
    public void checkV1SystemMetaPidExist(
        Identifier identifier, String serviceFailureCode, String serviceFailureMessage,
        String noFoundCode, String notFoundMessage) throws ServiceFailure, NotFound {
        boolean exists = false;
        try {
            exists = IdentifierManager.getInstance().systemMetadataPIDExists(identifier);
        } catch (SQLException e) {
            throw new ServiceFailure(
                serviceFailureCode, serviceFailureMessage + " since " + e.getMessage());
        }
        if (!exists) {
            //the v1 method only handles a pid. so it should throw a not-found exception.
            // check if the pid was deleted.
            try {
                boolean existsIndentifierTable =
                    IdentifierManager.getInstance().existsInIdentifierTable(identifier);
                if (existsIndentifierTable) {
                    notFoundMessage = notFoundMessage + " " + DELETEDMESSAGE;
                }
            } catch (Exception e) {
                logMetacat.info(
                    "Couldn't determine if the not-found identifier " + identifier.getValue()
                        + " was deleted since " + e.getMessage());
            }
            throw new NotFound(noFoundCode, notFoundMessage);
        }
    }

    /**
     * Utility method to get the PID for an SID. If the specified identifier is not an SID
     * , null will be returned.
     * @see getSeriesHead(..) as well for situations where you need the SystemMetadata.  The
     * advantage of
     * this method is that it doesn't unmarshall systemmetadata, and doesn't throw NotFound
     * exceptions.
     * @param sid  the specified sid
     * @param serviceFailureCode  the detail error code for the service failure exception
     * @return the pid for the sid. If the specified identifier is not an SID, null will be
     * returned.
     * @throws ServiceFailure
     */
    protected Identifier getPIDForSID(Identifier sid, String serviceFailureCode)
        throws ServiceFailure {
        Identifier id = null;
        String serviceFailureMessage =
            "The PID couldn't be identified for the sid " + sid.getValue();
        // first to try if we can find the given identifier in the system metadata map. If it is
        // in the map (meaning this is not sid), null will be returned.
        if (sid != null && sid.getValue() != null) {
            try {
                if (!IdentifierManager.getInstance().systemMetadataPIDExists(sid)) {
                    //determine if the given pid is a sid or not.
                    if (IdentifierManager.getInstance().systemMetadataSIDExists(sid)) {
                        try {
                            //set the header pid for the sid if the identifier is a sid.
                            id = IdentifierManager.getInstance().getHeadPID(sid);
                        } catch (SQLException sqle) {
                            throw new ServiceFailure(
                                serviceFailureCode, serviceFailureMessage + " since "
                                + sqle.getMessage());
                        }

                    }
                }
            } catch (SQLException e) {
                throw new ServiceFailure(
                    serviceFailureCode, serviceFailureMessage + " since " + e.getMessage());
            }
        }
        return id;
    }

    /**
     * Get the system metadata for the given PID (not a sid).
     * @param pid
     * @param serviceFailureCode
     * @param invalidRequestCode
     * @return the system metadata associated with the pid
     * @throws ServiceFailure
     * @throws NotFound
     * @throws InvalidRequest
     */
    protected SystemMetadata getSystemMetadataForPID(
        Identifier pid, String serviceFailureCode, String invalidRequestCode, String notFoundCode,
        boolean needDeleteInfo) throws ServiceFailure, InvalidRequest, NotFound {
        SystemMetadata sysmeta = null;
        if (pid == null || StringUtils.isAnyBlank(pid.getValue())) {
            throw new InvalidRequest(
                invalidRequestCode, "The passed-in Identifier cannot be null or blank!!");
        }
        try {
            sysmeta = SystemMetadataManager.getInstance().get(pid);
        } catch (Exception e) {
            logMetacat.error(
                "An error occurred while getting system metadata for identifier " + pid.getValue()
                    + ". The error message was: " + e.getMessage(), e);
            throw new ServiceFailure(
                serviceFailureCode, "Can't get the system metadata for " + pid.getValue()
                + " since " + e.getMessage());
        }
        if (sysmeta == null) {
            String error = "No system metadata could be found for given PID: " + pid.getValue();
            if (needDeleteInfo) {
                boolean existsInIdentifierTable = false;
                try {
                    existsInIdentifierTable =
                        IdentifierManager.getInstance().existsInIdentifierTable(pid);
                } catch (Exception e) {
                    logMetacat.warn("Couldn't determine if the pid " + pid.getValue()
                                        + " exists in the identifier table. We assume it doesn't");
                }
                if (existsInIdentifierTable) {
                    error = error + ". " + DELETEDMESSAGE;
                }
            }
            throw new NotFound(notFoundCode, error);
        }
        return sysmeta;
    }


    /*
     * Determine if the sid is legitimate in CN.create and CN.registerSystemMetadata methods. It
     * also is used as a part of rules of the updateSystemMetadata method. Here are the rules:
     * A. If the sysmeta doesn't have an SID, nothing needs to be checked for the SID.
     * B. If the sysmeta does have an SID, it may be an identifier which doesn't exist in the
     * system.
     * C. If the sysmeta does have an SID and it exists as an SID in the system, those scenarios
     * are acceptable:
     *    i. The sysmeta has an obsoletes field, the SID has the same value as the SID of the
     * system metadata of the obsoleting pid.
     *    ii. The sysmeta has an obsoletedBy field, the SID has the same value as the SID of the
     * system metadata of the obsoletedBy pid.
     */
    protected boolean checkSidInModifyingSystemMetadata(
        SystemMetadata sysmeta, String invalidSystemMetadataCode, String serviceFailureCode)
        throws InvalidSystemMetadata, ServiceFailure {
        boolean pass = false;
        if (sysmeta == null) {
            throw new InvalidSystemMetadata(
                invalidSystemMetadataCode, "The system metadata is null in the request.");
        }
        Identifier sid = sysmeta.getSeriesId();
        if (sid != null) {
            // the series id exists
            if (!isValidIdentifier(sid)) {
                throw new InvalidSystemMetadata(
                    invalidSystemMetadataCode,
                    "The series id in the system metadata is invalid in the request.");
            }
            Identifier pid = sysmeta.getIdentifier();
            if (!isValidIdentifier(pid)) {
                throw new InvalidSystemMetadata(
                    invalidSystemMetadataCode,
                    "The pid in the system metadata is invalid in the request.");
            }
            //the series id equals the pid (new pid hasn't been registered in the system, so
            // IdentifierManager.getInstance().identifierExists method can't exclude this scenario )
            if (sid.getValue().equals(pid.getValue())) {
                throw new InvalidSystemMetadata(
                    invalidSystemMetadataCode, "The series id " + sid.getValue()
                    + " in the system metadata shouldn't have the same value of the pid.");
            }
            try {
                if (IdentifierManager.getInstance().identifierExists(sid.getValue())) {
                    //the sid exists in system
                    if (sysmeta.getObsoletes() != null) {
                        SystemMetadata obsoletesSysmeta =
                            SystemMetadataManager.getInstance().get(sysmeta.getObsoletes());
                        if (obsoletesSysmeta != null) {
                            Identifier obsoletesSid = obsoletesSysmeta.getSeriesId();
                            if (obsoletesSid != null && obsoletesSid.getValue() != null
                                && !obsoletesSid.getValue().trim().equals("")) {
                                if (sid.getValue().equals(obsoletesSid.getValue())) {
                                    pass = true;// the i of rule C
                                }
                            }
                        } else {
                            logMetacat.warn(
                                "D1NodeService.checkSidInModifyingSystemMetacat - Can't find the "
                                + "system metadata for the pid "
                                    + sysmeta.getObsoletes().getValue()
                                    + " which is the value of the obsoletes. So we can't check if"
                                    + " the sid "
                                    + sid.getValue() + " is legitimate ");
                        }
                    }
                    if (!pass) {
                        // the sid doesn't match the sid of the obsoleting identifier. So we
                        // check the obsoletedBy
                        if (sysmeta.getObsoletedBy() != null) {
                            SystemMetadata obsoletedBySysmeta =
                                SystemMetadataManager.getInstance().get(sysmeta.getObsoletedBy());
                            if (obsoletedBySysmeta != null) {
                                Identifier obsoletedBySid = obsoletedBySysmeta.getSeriesId();
                                if (obsoletedBySid != null && obsoletedBySid.getValue() != null
                                    && !obsoletedBySid.getValue().trim().equals("")) {
                                    if (sid.getValue().equals(obsoletedBySid.getValue())) {
                                        pass = true;// the ii of the rule C
                                    }
                                }
                            } else {
                                logMetacat.warn(
                                    "D1NodeService.checkSidInModifyingSystemMetacat - Can't find "
                                    + "the system metadata for the pid "
                                        + sysmeta.getObsoletes().getValue()
                                        + " which is the value of the obsoletedBy. So we can't "
                                        + "check if the sid "
                                        + sid.getValue() + " is legitimate.");
                            }
                        }
                    }
                    if (!pass) {
                        throw new InvalidSystemMetadata(
                            invalidSystemMetadataCode, "The series id " + sid.getValue()
                            + " in the system metadata exists in the system. And it doesn't match"
                            + " either previous object's sid or the next object's sid.");
                    }
                } else {
                    pass = true; //Rule B
                }
            } catch (SQLException e) {
                throw new ServiceFailure(
                    serviceFailureCode,
                    "Can't determine if the sid in the system metadata is unique or not since "
                        + e.getMessage());
            }

        } else {
            //no sid. Rule A.
            pass = true;
        }
        return pass;

    }

    public OptionList listViews(Session arg0)
        throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {
        OptionList views = new OptionList();
        views.setKey("views");
        views.setDescription("List of views for objects on the node");
        Vector<String> skinNames = null;
        try {
            skinNames = SkinUtil.getSkinNames();
        } catch (PropertyNotFoundException e) {
            throw new ServiceFailure("2841", e.getMessage());
        }
        for (String skinName : skinNames) {
            views.addOption(skinName);
        }
        return views;
    }

    public OptionList listViews()
        throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {
        return listViews(null);
    }

    public InputStream view(Session session, String format, Identifier id)
        throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented,
        NotFound {
        InputStream resultInputStream = null;

        String serviceFailureCode = "2831";
        String notFoundCode = "2835";
        String invalidRequestCode = "2833";
        boolean needDeleteInfo = false;
        Identifier HeadOfSid = getPIDForSID(id, serviceFailureCode);
        if (HeadOfSid != null) {
            id = HeadOfSid;
        }
        SystemMetadata sysmeta =
            getSystemMetadataForPID(id, serviceFailureCode, invalidRequestCode, notFoundCode,
                                    needDeleteInfo);
        InputStream object = this.get(session, sysmeta.getIdentifier());

        // authorization is delegated to the get() call, and using the ID
        // from the sysmeta guarantees that it's a PID, and so will
        // avoid duplicating series resolution attempts

        try {
            // can only transform metadata, really
            ObjectFormat objectFormat =
                ObjectFormatCache.getInstance().getFormat(sysmeta.getFormatId());
            if (objectFormat.getFormatType().equals("METADATA")) {
                // transform
                DBTransform transformer = new DBTransform();
                String documentContent = IOUtils.toString(object, "UTF-8");
                String sourceType = objectFormat.getFormatId().getValue();
                String targetType = "-//W3C//HTML//EN";
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Writer writer = new OutputStreamWriter(baos, "UTF-8");
                // TODO: include more params?
                Hashtable<String, String[]> params = new Hashtable<String, String[]>();
                String localId = null;
                try {
                    localId = IdentifierManager.getInstance().getLocalId(id.getValue());
                } catch (McdbDocNotFoundException e) {
                    throw new NotFound("1020", e.getMessage());
                }
                params.put("qformat", new String[]{format});
                params.put("docid", new String[]{localId});
                params.put("pid", new String[]{id.getValue()});
                addParamsFromSkinProperties(
                    params, format);//add more params from the skin properties file
                transformer.transformXMLDocument(documentContent, sourceType, targetType, format,
                                                 writer, params, null//sessionid
                );

                // finally, get the HTML back
                resultInputStream = new ContentTypeByteArrayInputStream(baos.toByteArray());
                ((ContentTypeByteArrayInputStream) resultInputStream).setContentType("text/html");

            } else {
                // just return the raw bytes
                resultInputStream = object;
            }
        } catch (IOException e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        } catch (PropertyNotFoundException e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        } catch (SQLException e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        } catch (ClassNotFoundException e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        } catch (ServiceException e) {
            ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
            sf.initCause(e);
            throw sf;
        }

        return resultInputStream;
    }

    /**
     * Read the properties file of the theme and add more parameters to the styel sheet.
     * The configuration on the theme properties file should look like:
     * stylesheet.parameters.1.name=serverName
     * stylesheet.parameters.1.value=server.name
     * stylesheet.parameters.2.name=organization
     * stylesheet.parameters.2.value=ESS-DIVE
     * The value can be either a name of metacat properties (e.g.server.name) or a real value (e
     * .g. ESS-DIVE).
     * @return the params with more name/value pairs from the theme properties file.
     * @throws ServiceException
     * @throws PropertyNotFoundException
     */
    static void addParamsFromSkinProperties(Hashtable<String, String[]> params, String format)
        throws ServiceException, PropertyNotFoundException {
        SkinPropertyService skinPropService = SkinPropertyService.getInstance();
        Vector<String> propertiesNames =
            skinPropService.getPropertyNamesByGroup(format, "stylesheet.parameters");
        logMetacat.debug("D1NodeService.addParasFromSkinProperties - the names of properties  are "
                             + propertiesNames);
        if (propertiesNames != null && !propertiesNames.isEmpty()) {
            String name = null;
            String value = null;
            for (String property : propertiesNames) {
                if (property.contains("name")) {
                    name = skinPropService.getProperty(format, property);
                } else if (property.contains("value")) {
                    try {
                        value = PropertyService.getInstance()
                            .getProperty(skinPropService.getProperty(format, property));
                    } catch (Exception e) {
                        //we can't find the property at the metacat properties. Fall back to
                        // treat the properties as the value
                        value = skinPropService.getProperty(format, property);
                    }
                    logMetacat.debug(
                        "D1NodeService.addParasFromSkinProperties - add the pair name " + name
                            + " and its value " + value + " the style sheet parameters list");
                    params.put(name, new String[]{value});
                }
            }
        }
    }

    /*
     * Determine if the given identifier exists in the obsoletes field in the system metadata table.
     * If the return value is not null, the given identifier exists in the given cloumn. The
     * return value is
     * the guid of the first row.
     */
    protected String existsInObsoletes(Identifier id) throws InvalidRequest, ServiceFailure {
        String guid = existsInFields("obsoletes", id);
        return guid;
    }

    /*
     * Determine if the given identifier exists in the obsoletes field in the system metadata table.
     * If the return value is not null, the given identifier exists in the given cloumn. The
     * return value is
     * the guid of the first row.
     */
    protected String existsInObsoletedBy(Identifier id) throws InvalidRequest, ServiceFailure {
        String guid = existsInFields("obsoleted_by", id);
        return guid;
    }

    /*
     * Determine if the given identifier exists in the given column in the system metadata table.
     * If the return value is not null, the given identifier exists in the given cloumn. The
     * return value is
     * the guid of the first row.
     */
    private String existsInFields(String column, Identifier id)
        throws InvalidRequest, ServiceFailure {
        String guid = null;
        if (id == null) {
            throw new InvalidRequest(
                "4863",
                "The given identifier is null and we can't determine if the guid exists in the "
                + "field "
                    + column + " in the systemmetadata table");
        }
        String sql = "SELECT guid FROM systemmetadata WHERE " + column + " = ?";
        int serialNumber = -1;
        DBConnection dbConn = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("D1NodeService.existsInFields");
            serialNumber = dbConn.getCheckOutSerialNumber();
            stmt = dbConn.prepareStatement(sql);
            stmt.setString(1, id.getValue());
            result = stmt.executeQuery();
            if (result.next()) {
                guid = result.getString(1);
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ServiceFailure("4862", "We can't determine if the id " + id.getValue()
                + " exists in field " + column + " in the systemmetadata table since "
                + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logMetacat.warn(
                        "We can close the PreparedStatment in D1NodeService.existsInFields since "
                            + e.getMessage());
                }
            }

        }
        return guid;

    }


    /**
     * Get the ip address from the service
     * @return the ip address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Set the ip address for the service
     * @param ipAddress  the address will be set
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Get the user agent from the service
     * @return
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Set the user agent for the service
     * @param userAgent  the user agent will be set
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Check if the access control was modified between two system metadata objects. It compares
     * two parts:
     * RightsHolder and AccessPolicy
     * @param originalSysmeta  the original system metadata object
     * @param newSysmeta  the new system metadata object
     * @return true if the access control was modified; false otherwise.
     */
    public static boolean isAccessControlDirty(
        SystemMetadata originalSysmeta, SystemMetadata newSysmeta) {
        boolean dirty = true;
        if (originalSysmeta == null && newSysmeta == null) {
            dirty = false;
            logMetacat.debug(
                "D1NodeService.isAccessControlDirty - is access control dirty?(both system "
                + "metadata objects are null) - "
                    + dirty);
        } else if (originalSysmeta != null && newSysmeta != null) {
            //first to check if the rights holder was changed.
            Subject originalRightsHolder = originalSysmeta.getRightsHolder();
            Subject newRigthsHolder = newSysmeta.getRightsHolder();
            if (originalRightsHolder == null && newRigthsHolder == null) {
                dirty = false;
                logMetacat.debug(
                    "D1NodeService.isAccessControlDirty - is the right holder dirty?(both right "
                    + "holder objects are null) - "
                        + dirty);
            } else if (originalRightsHolder != null && newRigthsHolder != null) {
                if (originalRightsHolder.compareTo(newRigthsHolder) == 0) {
                    dirty = false;
                } else {
                    dirty = true;
                }
                logMetacat.debug(
                    "D1NodeService.isAccessControlDirty - is the right holder dirty?(both right "
                    + "holder objects are not null) - "
                        + dirty + " since the original right holder is "
                        + originalRightsHolder.getValue() + " and the new rights holder is  "
                        + newRigthsHolder.getValue());
            } else {
                dirty = true;
                logMetacat.debug(
                    "D1NodeService.isAccessControlDirty - is the rights holder dirty?(one rights "
                    + "holder object is null; another is not) - "
                        + dirty);
            }
            if (!dirty) {
                //rights holder is not changed, we need to compare the access policy
                boolean isAccessPolicyEqual =
                    equals(originalSysmeta.getAccessPolicy(), newSysmeta.getAccessPolicy());
                logMetacat.debug(
                    "D1NodeService.isAccessControlDirty - do the access policies equal?(we need "
                    + "to compare access policy since the rights hloders are same) - "
                        + isAccessPolicyEqual);
                dirty = !isAccessPolicyEqual;
            }
            logMetacat.debug(
                "D1NodeService.isAccessControlDirty - is access control dirty?(both system "
                + "metadata objects are not null) - "
                    + dirty);
        } else {
            dirty = true;
            logMetacat.debug(
                "D1NodeService.isAccessControlDirty - is access control dirty?(one system "
                + "metadata object is null; another is not) - "
                    + dirty);
        }
        return dirty;
    }

    /**
     * Compare two AccessPolicy objects
     * @param ap1
     * @param ap2
     * @return true if they are same; false otherwise
     */
    public static boolean equals(AccessPolicy ap1, AccessPolicy ap2) {
        boolean equal = false;
        if (ap1 == null && ap2 == null) {
            equal = true;
        } else if (ap1 == null && ap2 != null) {
            if (ap2.getAllowList() == null || ap2.getAllowList().isEmpty()) {
                equal = true;
            }
        } else if (ap2 == null && ap1 != null) {
            if (ap1.getAllowList() == null || ap1.getAllowList().isEmpty()) {
                equal = true;
            }
        } else {
            List<AccessRule> list1 = ap1.getAllowList();
            List<AccessRule> list2 = ap2.getAllowList();
            if (list1 == null && list2 == null) {
                equal = true;
            } else if (list1 == null && list2 != null) {
                if (list2.isEmpty()) {
                    equal = true;
                }
            } else if (list2 == null && list1 != null) {
                if (list1.isEmpty()) {
                    equal = true;
                }
            } else {
                Map<String, List<Permission>> map1 = consolidateAccessRules(list1);
                Map<String, List<Permission>> map2 = consolidateAccessRules(list2);
                if (map1.size() == map2.size()) {
                    outerLoop:
                    for (String subject : map1.keySet()) {
                        if (map2.get(subject) == null) {
                            logMetacat.debug("D1NodeService.equals - found the subject " + subject
                                                 + " does exist in the first AccessPolicy "
                                                 + "but does not exist in the second AccessPolicy. "
                                                 + "So they do not equal.");
                            break;//find a subject doesn't exist on the second map. So they
                            // don't equal.
                        } else {
                            List<Permission> permissions1 = map1.get(subject);
                            List<Permission> permissions2 = map2.get(subject);
                            for (Permission permission1 : permissions1) {
                                if (!permissions2.contains(permission1)) {
                                    break outerLoop;
                                } else {
                                    permissions2.remove(permission1);
                                }
                            }
                            if (!permissions2.isEmpty()) {
                                break;//some permissions were left, so they don't equal
                            } else {
                                map2.remove(
                                    subject);//we are done for this subject and delete it from
                                    // the second map
                            }
                        }
                    }
                    if (map2.isEmpty()) {
                        equal = true;
                        logMetacat.debug(
                            "D1NodeService.equals - all access rules are matched. So they equal.");
                    }
                }
            }
        }
        logMetacat.debug(
            "D1NodeService.equals - does the two access policy object equal? - " + equal);
        return equal;
    }

    /**
     * Consolidate a list of AccessRule objects to a map object, which key is the subject value
     * and value is
     * a list of permissions. There are no duplicate values in the permission list.
     * @param rules  the AccessRule list will be consolidated
     * @return the map of subjects and permissions
     */
    private static Map<String, List<Permission>> consolidateAccessRules(List<AccessRule> rules) {
        Map<String, List<Permission>> consolidatedMap = new HashMap<String, List<Permission>>();
        for (AccessRule rule : rules) {
            if (rule != null) {
                List<Subject> subjects = rule.getSubjectList();
                List<Permission> permissions = rule.getPermissionList();
                if (subjects != null && permissions != null) {
                    for (Subject subject : subjects) {
                        if (subject != null && subject.getValue() != null && !subject.getValue()
                            .trim().equals("")) {
                            for (Permission permission : permissions) {
                                if (permission != null && !permission.toString().trim()
                                    .equals("")) {
                                    List<Permission> expandedPermissions =
                                        expandPermissions(permission);
                                    logMetacat.debug("D1NodeService.consolidateAccessRules - "
                                                         + "the expanded permission is "
                                                         + expandedPermissions
                                                         + " for the permission " + permission);
                                    if (!consolidatedMap.containsKey(subject.getValue())) {
                                        consolidatedMap.put(
                                            subject.getValue(), expandedPermissions);
                                        logMetacat.debug("D1NodeService.consolidateAccessRules - "
                                                             + "put the subject "
                                                             + subject.getValue()
                                                             + " and the permissions "
                                                             + expandedPermissions
                                                             + " into the map");
                                    } else {
                                        List<Permission> existedPermissions =
                                            consolidatedMap.get(subject.getValue());
                                        for (Permission expandedPermission : expandedPermissions) {
                                            if (!existedPermissions.contains(expandedPermission)) {
                                                existedPermissions.add(expandedPermission);
                                                logMetacat.debug(
                                                    "D1NodeService.consolidateAccessRules - "
                                                        + "add a new permssion "
                                                        + permission.toString()
                                                        + " for the subject " + subject.getValue()
                                                        + " into the map ");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return consolidatedMap;
    }
}
