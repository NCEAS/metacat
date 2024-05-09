package edu.ucsb.nceas.metacat.restservice.v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.formats.ObjectFormatInfo;
import org.dataone.exceptions.MarshallingException;
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
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectFormatList;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.DateTimeMarshaller;
import org.dataone.service.util.EncodingUtilities;
import org.dataone.service.util.ExceptionHandler;
import org.dataone.service.util.TypeMarshaller;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.dataone.v1.CNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.D1ResourceHandler;
import edu.ucsb.nceas.metacat.restservice.multipart.CheckedFile;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.metacat.restservice.multipart.MultipartRequestWithSysmeta;
import edu.ucsb.nceas.metacat.restservice.multipart.StreamingMultipartRequestResolver;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * CN REST service implementation handler
 * 
 * ****************** CNCore
 * create() - POST /d1/cn/object/PID
 * listFormats() - GET /d1/cn/formats getFormat() - GET /d1/cn/formats/FMTID
 * getLogRecords - GET /d1/cn/log reserveIdentifier() - POST /d1/cn/reserve
 * listNodes() - Not implemented registerSystemMetadata() - POST /d1/meta/PID
 * 
 * CNRead get() - GET /d1/cn/object/PID getSystemMetadata() - GET
 * /d1/cn/meta/PID resolve() - GET /d1/cn/resolve/PID assertRelation() - GET
 * /d1/cn/assertRelation/PID getChecksum() - GET /d1/cn/checksum search() - Not
 * implemented in Metacat
 * 
 * CNAuthorization setOwner() - PUT /d1/cn/owner/PID isAuthorized() - GET
 * /d1/cn/isAuthorized/PID setAccessPolicy() - POST /d1/cn/accessRules
 * 
 * CNIdentity - not implemented at all on Metacat
 * 
 * CNReplication setReplicationStatus() - PUT /replicaNotifications/PID
 * updateReplicationMetadata() - PUT /replicaMetadata/PID setReplicationPolicy()
 * - PUT /replicaPolicies/PID isNodeAuthorized() - GET
 * /replicaAuthorizations/PID
 * 
 * CNRegister -- not implemented at all in Metacat ******************
 * 
 * @author leinfelder
 * 
 */
public class CNResourceHandler extends D1ResourceHandler {

    /** CN-specific operations **/
    protected static final String RESOURCE_RESERVE = "reserve";
    protected static final String RESOURCE_FORMATS = "formats";
    protected static final String RESOURCE_RESOLVE = "resolve";
    protected static final String RESOURCE_OWNER = "owner";
    protected static final String RESOURCE_REPLICATION_POLICY = "replicaPolicies";
    protected static final String RESOURCE_REPLICATION_META = "replicaMetadata";
    protected static final String RESOURCE_REPLICATION_AUTHORIZED = "replicaAuthorizations";
    protected static final String RESOURCE_REPLICATION_NOTIFY = "replicaNotifications";
    private static Log logMetacat = LogFactory.getLog(CNResourceHandler.class);

    /**
     * Constructor
     * @param request  the request that the handler will handle
     * @param response  the response that the handler will send back
     */
    public CNResourceHandler(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    /**
     * This function is called from REST API servlet and handles each request to
     * the servlet
     * 
     * @param httpVerb
     *            (GET, POST, PUT or DELETE)
     */
    @Override
    public void handle(byte httpVerb) {
        // prepare the handler
        super.handle(httpVerb);

        try {

            // only service requests if we have D1 configured
            if (!isD1Enabled()) {
                ServiceFailure se = new ServiceFailure("0000", "DataONE services are not enabled on this node");
                serializeException(se, response.getOutputStream());
                return;
            }

            // get the resource
            String resource = request.getPathInfo();
            if (resource == null) {
                throw new InvalidRequest("0000", "The resource should not be null.");
            }
            resource = resource.substring(resource.indexOf("/") + 1);

            // for the rest of the resouce
            String extra = null;

            logMetacat.debug("handling verb " + httpVerb
                    + " request with resource '" + resource + "'");
            boolean status = false;
            if (resource.startsWith(RESOURCE_ACCESS_RULES)
                    && httpVerb == PUT) {
                logMetacat.debug("Setting access policy");
                // after the command
                extra = parseTrailing(resource, RESOURCE_ACCESS_RULES);
                extra = decode(extra);
                setAccess(extra);
                status = true;
                logMetacat.debug("done setting access");

            } else if (resource.startsWith(RESOURCE_META)) {
                logMetacat.debug("Using resource: " + RESOURCE_META);

                // after the command
                extra = parseTrailing(resource, RESOURCE_META);
                extra = decode(extra);
                // get
                if (httpVerb == GET) {
                    getSystemMetadataObject(extra);
                    status = true;
                }
                // post to register system metadata
                if (httpVerb == POST) {
                    registerSystemMetadata();
                    status = true;
                }

            } else if (resource.startsWith(RESOURCE_RESERVE)) {
                // reserve the ID (in params)
                if (httpVerb == POST) {
                    reserve();
                    status = true;
                }
            } else if (resource.startsWith(RESOURCE_RESOLVE)) {

                // after the command
                extra = parseTrailing(resource, RESOURCE_RESOLVE);
                extra = decode(extra);
                // resolve the object location
                if (httpVerb == GET) {
                    resolve(extra);
                    status = true;
                }
            } else if (resource.startsWith(RESOURCE_OWNER)) {

                // after the command
                extra = parseTrailing(resource, RESOURCE_OWNER);
                extra = decode(extra);
                // set the owner
                if (httpVerb == PUT) {
                    owner(extra);
                    status = true;
                }
            } else if (resource.startsWith(RESOURCE_IS_AUTHORIZED)) {

                // after the command
                extra = parseTrailing(resource, RESOURCE_IS_AUTHORIZED);
                extra = decode(extra);
                // authorized?
                if (httpVerb == GET) {
                    isAuthorized(extra);
                    status = true;
                }
            } else if (resource.startsWith(RESOURCE_OBJECTS)) {
                logMetacat.debug("Using resource 'object'");
                logMetacat
                        .debug("D1 Rest: Starting resource processing...");

                // after the command
                extra = parseTrailing(resource, RESOURCE_OBJECTS);
                extra = decode(extra);
                logMetacat.debug("objectId: " + extra);
                logMetacat.debug("verb:" + httpVerb);

                if (httpVerb == GET) {
                    if (extra != null) {
                        getObject(extra);
                    } else {
                        listObjects();
                    }
                    status = true;
                } else if (httpVerb == POST) {
                    putObject(FUNCTION_NAME_INSERT);
                    status = true;
                } else if (httpVerb == HEAD) {
                    describeObject(extra);
                    status = true;
                } else if (httpVerb == DELETE) {
                    deleteObject(extra);
                    status = true;
                }

            } else if (resource.startsWith(RESOURCE_FORMATS)) {
                logMetacat.debug("Using resource: " + RESOURCE_FORMATS);

                // after the command
                extra = parseTrailing(resource, RESOURCE_FORMATS);
                extra = decode(extra);
                // handle each verb
                if (httpVerb == GET) {
                    if (extra == null) {
                        // list the formats collection
                        listFormats();
                    } else {
                        // get the specified format
                        getFormat(extra);
                    }
                    status = true;
                }

            } else if (resource.startsWith(RESOURCE_LOG)) {
                logMetacat.debug("Using resource: " + RESOURCE_LOG);
                // handle log events
                if (httpVerb == GET) {
                    getLog();
                    status = true;
                }

            } else if (resource.startsWith(Constants.RESOURCE_ARCHIVE)) {
                logMetacat.debug("Using resource " + Constants.RESOURCE_ARCHIVE);
                // handle archive events
                if (httpVerb == PUT) {
                    extra = parseTrailing(resource, Constants.RESOURCE_ARCHIVE);
                    extra = decode(extra);
                    archive(extra);
                    status = true;
                }
            } else if (resource.startsWith(Constants.RESOURCE_CHECKSUM)) {
                logMetacat.debug("Using resource: " + Constants.RESOURCE_CHECKSUM);

                // after the command
                extra = parseTrailing(resource, Constants.RESOURCE_CHECKSUM);
                extra = decode(extra);
                // handle checksum requests
                if (httpVerb == GET) {

                    if (extra != null && extra.length() > 0) {
                        checksum(extra);
                        status = true;
                    } else {
                        listChecksumAlgorithms();
                        status = true;
                    }

                }

            } else if (resource.startsWith(RESOURCE_REPLICATION_POLICY)
                    && httpVerb == PUT) {

                logMetacat.debug("Using resource: "
                        + RESOURCE_REPLICATION_POLICY);
                // get the trailing pid
                extra = parseTrailing(resource, RESOURCE_REPLICATION_POLICY);
                extra = decode(extra);
                setReplicationPolicy(extra);
                status = true;

            } else if (resource.startsWith(RESOURCE_REPLICATION_META)
                    && httpVerb == PUT) {

                logMetacat.debug("Using resource: "
                        + RESOURCE_REPLICATION_META);
                // get the trailing pid
                extra = parseTrailing(resource, RESOURCE_REPLICATION_META);
                extra = decode(extra);
                updateReplicationMetadata(extra);
                status = true;

            } else if (resource.startsWith(RESOURCE_REPLICATION_NOTIFY)
                    && httpVerb == PUT) {

                logMetacat.debug("Using resource: "
                        + RESOURCE_REPLICATION_NOTIFY);
                // get the trailing pid
                extra = parseTrailing(resource, RESOURCE_REPLICATION_NOTIFY);
                extra = decode(extra);
                setReplicationStatus(extra);
                status = true;

            } else if (resource.startsWith(RESOURCE_REPLICATION_AUTHORIZED)
                    && httpVerb == GET) {

                logMetacat.debug("Using resource: "
                        + RESOURCE_REPLICATION_AUTHORIZED);
                // get the trailing pid
                extra = parseTrailing(resource,
                        RESOURCE_REPLICATION_AUTHORIZED);
                extra = decode(extra);
                isNodeAuthorized(extra);
                status = true;

            } else if (resource.startsWith(Constants.RESOURCE_MONITOR_PING)) {
                if (httpVerb == GET) {
                    // after the command
                    extra = parseTrailing(resource, Constants.RESOURCE_MONITOR_PING);
                    extra = decode(extra);
                    logMetacat.debug("processing ping request");
                    Date result = CNodeService.getInstance(request).ping();
                    // TODO: send to output
                    status = true;
                }
            } else if (resource.startsWith(Constants.RESOURCE_META_OBSOLETEDBY)
                    && httpVerb == PUT) {

                logMetacat.debug("Using resource: "
                        + Constants.RESOURCE_META_OBSOLETEDBY);
                // get the trailing pid
                extra = parseTrailing(resource, Constants.RESOURCE_META_OBSOLETEDBY);
                extra = decode(extra);
                setObsoletedBy(extra);
                status = true;
            } else if (resource.startsWith(Constants.RESOURCE_REPLICATION_DELETE_REPLICA)
                    && httpVerb == PUT) {

                logMetacat.debug("Using resource: "
                        + Constants.RESOURCE_REPLICATION_DELETE_REPLICA);
                // get the trailing pid
                extra = parseTrailing(resource, Constants.RESOURCE_REPLICATION_DELETE_REPLICA);
                extra = decode(extra);
                deleteReplica(extra);
                status = true;
            }

            if (!status) {
                throw new ServiceFailure("0000", "Unknown error, status = "
                        + status);
            }
        } catch (BaseException be) {
            // report Exceptions as clearly and generically as possible
            OutputStream out = null;
            try {
                out = response.getOutputStream();
            } catch (IOException ioe) {
                logMetacat.error("Could not get output stream from response",
                        ioe);
            }
            serializeException(be, out);
        } catch (Exception e) {
            // report Exceptions as clearly and generically as possible
            logMetacat.error(e.getClass() + ": " + e.getMessage(), e);
            OutputStream out = null;
            try {
                out = response.getOutputStream();
            } catch (IOException ioe) {
                logMetacat.error("Could not get output stream from response",
                        ioe);
            }
            ServiceFailure se = new ServiceFailure("0000", e.getMessage());
            serializeException(se, out);
        }
    }

    /**
     * Get the checksum for the given guid
     *
     * @param guid
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws IOException
     * @throws MarshallingException
     */
    private void checksum(String guid) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotFound, InvalidRequest, NotImplemented,
            MarshallingException, IOException {
        Identifier guidid = new Identifier();
        guidid.setValue(guid);
        logMetacat.debug("getting checksum for object " + guid);
        Checksum c = CNodeService.getInstance(request).getChecksum(session,
                guidid);
        logMetacat.debug("got checksum " + c.getValue());
        response.setStatus(200);
        logMetacat.debug("serializing response");
        TypeMarshaller.marshalTypeToOutputStream(c, response.getOutputStream());
        logMetacat.debug("done serializing response.");

    }

    /**
     * get the logs based on passed params. Available params are token,
     * fromDate, toDate, event. See
     * http://mule1.dataone.org/ArchitectureDocs/mn_api_crud
     * .html#MN_crud.getLogRecords for more info
     *
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws IOException
     * @throws MarshallingException
     * @throws InsufficientResources 
     */
    private void getLog() throws InvalidToken, ServiceFailure, NotAuthorized,
            InvalidRequest, NotImplemented, IOException, MarshallingException, InsufficientResources {

        Date fromDate = null;
        Date toDate = null;
        Event event = null;
        Integer start = null;
        Integer count = null;
        String pidFilter = null;

        try {
            String fromDateS = params.get("fromDate")[0];
            logMetacat.debug("param fromDateS: " + fromDateS);
            fromDate = DateTimeMarshaller.deserializeDateToUTC(fromDateS);
        } catch (Exception e) {
            logMetacat.warn("Could not parse fromDate: " + e.getMessage());
        }
        try {
            String toDateS = params.get("toDate")[0];
            logMetacat.debug("param toDateS: " + toDateS);
            toDate = DateTimeMarshaller.deserializeDateToUTC(toDateS);
        } catch (Exception e) {
            logMetacat.warn("Could not parse toDate: " + e.getMessage());
        }
        try {
            String eventS = params.get("event")[0];
            event = Event.convert(eventS);
        } catch (Exception e) {
            logMetacat.warn("Could not parse event: " + e.getMessage());
        }
        logMetacat.debug("fromDate: " + fromDate + " toDate: " + toDate);

        try {
            start = Integer.parseInt(params.get("start")[0]);
        } catch (Exception e) {
            logMetacat.warn("Could not parse start: " + e.getMessage());
        }
        try {
            count = Integer.parseInt(params.get("count")[0]);
        } catch (Exception e) {
            logMetacat.warn("Could not parse count: " + e.getMessage());
        }

        try {
            pidFilter = params.get("pidFilter")[0];
        } catch (Exception e) {
            logMetacat.warn("Could not parse pidFilter: " + e.getMessage());
        }

        logMetacat.debug("calling getLogRecords");
        org.dataone.service.types.v1.Log log = CNodeService.getInstance(request).getLogRecords(session,
                fromDate, toDate, event, pidFilter, start, count);

        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        TypeMarshaller.marshalTypeToOutputStream(log, out);

    }

    /**
     * Implements REST version of DataONE CRUD API --> get
     *
     * @param guid
     *            ID of data object to be read
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws IOException
     */
    protected void getObject(String guid) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotFound, InvalidRequest, NotImplemented,
            IOException {

        Identifier id = new Identifier();
        id.setValue(guid);

        SystemMetadata sm = CNodeService.getInstance(request)
                .getSystemMetadata(session, id);

        // set the headers for the content
        String mimeType = ObjectFormatInfo.instance().getMimeType(sm.getFormatId().getValue());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        String extension = ObjectFormatInfo.instance().getExtension(sm.getFormatId().getValue());
        String filename = id.getValue();
        if (extension != null && filename != null && !filename.endsWith(extension)) {
            filename = id.getValue() + extension;
        }
        response.setContentType(mimeType);
        response.setHeader("Content-Disposition", "inline; filename=" + filename);

        InputStream data = CNodeService.getInstance(request).get(session, id);

        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        IOUtils.copyLarge(data, out);

    }

    /**
     * Implements REST version of DataONE CRUD API --> getSystemMetadata
     *
     * @param guid
     *            ID of data object to be read
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws IOException
     * @throws MarshallingException
     */
    protected void getSystemMetadataObject(String guid) throws InvalidToken,
            ServiceFailure, NotAuthorized, NotFound, InvalidRequest,
            NotImplemented, IOException, MarshallingException {

        Identifier id = new Identifier();
        id.setValue(guid);
        SystemMetadata sysmeta = CNodeService.getInstance(request)
                .getSystemMetadata(session, id);

        response.setContentType("text/xml");
        response.setStatus(200);
        OutputStream out = response.getOutputStream();

        // Serialize and write it to the output stream
        TypeMarshaller.marshalTypeToOutputStream(sysmeta, out);
    }

    /**
     * Earthgrid API > Put Service >Put Function : calls MetacatHandler >
     * handleInsertOrUpdateAction
     *
     * @param guid
     *            - ID of data object to be inserted or updated. If action is
     *            update, the pid is the existing pid. If insert, the pid is the
     *            new one
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws IdentifierNotUnique
     * @throws MarshallingException
     * @throws NotImplemented
     * @throws InvalidSystemMetadata
     * @throws InsufficientResources
     * @throws UnsupportedType
     * @throws NotAuthorized
     * @throws InvalidToken
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws FileUploadException 
     * @throws NoSuchAlgorithmException 
     */
    protected void putObject(String action) throws ServiceFailure,
            InvalidRequest, IdentifierNotUnique, MarshallingException, InvalidToken,
            NotAuthorized, UnsupportedType, InsufficientResources,
            InvalidSystemMetadata, NotImplemented, IOException,
            InstantiationException, IllegalAccessException, NoSuchAlgorithmException, FileUploadException {
            CheckedFile objFile = null;
            try {
                // Read the incoming data from its Mime Multipart encoding
                MultipartRequestWithSysmeta multiparts = collectObjectFiles();
                DetailedFileInputStream object = null;
                Map<String, File> files = multiparts.getMultipartFiles();
                objFile = (CheckedFile) files.get("object");
                // ensure we have the object bytes
                if (objFile == null) {
                    throw new InvalidRequest("1102", "The object param must contain the object bytes.");
                }
                object = new DetailedFileInputStream(objFile, objFile.getChecksum());

                // get the encoded pid string from the body and make the object
                String pidString = multipartparams.get("pid").get(0);
                Identifier pid = new Identifier();
                pid.setValue(pidString);

                logMetacat.debug("putObject: " + pid.getValue() + "/" + action);

                SystemMetadata smd = multiparts.getSystemMetadata();
                // ensure we have the system metadata
                if ( smd == null ) {
                    throw new InvalidRequest("1102", "The sysmeta param must contain the system metadata document.");

                }


                if (action.equals(FUNCTION_NAME_INSERT)) { // handle inserts

                    logMetacat.debug("Commence creation...");

                    logMetacat.debug("creating object with pid " + pid.getValue());
                    Identifier rId = CNodeService.getInstance(request).create(session, pid, object, smd);

                    OutputStream out = response.getOutputStream();
                    response.setStatus(200);
                    response.setContentType("text/xml");

                    TypeMarshaller.marshalTypeToOutputStream(rId, out);

                } else {
                    throw new InvalidRequest("1000", "Operation must be create.");
                }
            } catch (Exception e) {
            if(objFile != null) {
                //objFile.deleteOnExit();
                StreamingMultipartRequestResolver.deleteTempFile(objFile);
            }
            throw e;
         }
    }

    /**
     * List the object formats registered with the system
     *
     * @throws NotImplemented
     * @throws InsufficientResources
     * @throws NotFound
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws IOException
     * @throws MarshallingException
     */
    private void listFormats() throws InvalidRequest, ServiceFailure, NotFound,
            InsufficientResources, NotImplemented, IOException, MarshallingException {
        logMetacat.debug("Entering listFormats()");

        ObjectFormatList objectFormatList = CNodeService.getInstance(request)
                .listFormats();
        // get the response output stream
        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        // style the object with a processing directive
        String stylesheet = null;
        try {
            stylesheet = PropertyService.getProperty("dataone.types.xsl.v1");
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("Could not locate DataONE types XSLT: "
                    + e.getMessage());
        }

        TypeMarshaller.marshalTypeToOutputStream(objectFormatList, out,
                stylesheet);

    }
    
    private void listChecksumAlgorithms() throws IOException, ServiceFailure,
            NotImplemented, MarshallingException {
        logMetacat.debug("Entering listFormats()");

        ChecksumAlgorithmList result = CNodeService.getInstance(request).listChecksumAlgorithms();

        // get the response output stream
        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        TypeMarshaller.marshalTypeToOutputStream(result, out);

    }

    /**
     * http://mule1.dataone.org/ArchitectureDocs-current/apis/CN_APIs.html#CNRead.describe
     * @param pid
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    private void describeObject(String pid) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented, InvalidRequest
    {
        response.setContentType("text/xml");

        Identifier id = new Identifier();
        id.setValue(pid);

        DescribeResponse dr = null;
        try {
            dr = CNodeService.getInstance(request).describe(session, id);
        } catch (BaseException e) {
            response.setStatus(e.getCode());
            response.addHeader("DataONE-Exception-Name", e.getClass().getName());
            response.addHeader("DataONE-Exception-DetailCode", e.getDetail_code());
            response.addHeader("DataONE-Exception-Description", e.getDescription());
            response.addHeader("DataONE-Exception-PID", id.getValue());
            return;
        }

        response.setStatus(200);
        //response.addHeader("pid", pid);
        response.addHeader("DataONE-Checksum", dr.getDataONE_Checksum().getAlgorithm() + "," + dr.getDataONE_Checksum().getValue());
        response.addHeader("Content-Length", dr.getContent_Length() + "");
        response.addHeader("Last-Modified", DateTimeMarshaller.serializeDateToUTC(dr.getLast_Modified()));
        response.addHeader("DataONE-ObjectFormat", dr.getDataONE_ObjectFormatIdentifier().getValue());
        response.addHeader("DataONE-SerialVersion", dr.getSerialVersion().toString());

    }

    /**
     * Handle delete 
     * @param pid ID of data object to be deleted
     * @throws IOException
     * @throws InvalidRequest 
     * @throws NotImplemented 
     * @throws NotFound 
     * @throws NotAuthorized 
     * @throws ServiceFailure 
     * @throws InvalidToken 
     * @throws MarshallingException 
     */
    private void deleteObject(String pid) throws IOException, InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented, InvalidRequest, MarshallingException 
    {

        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        Identifier id = new Identifier();
        id.setValue(pid);

        logMetacat.debug("Calling delete for identifier " + pid);
        CNodeService.getInstance(request).delete(session, id);
        TypeMarshaller.marshalTypeToOutputStream(id, out);
        
    }

    /**
     * Archives the given pid
     * @param pid
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws IOException
     * @throws MarshallingException
     * @throws InvalidRequest 
     */
    private void archive(String pid) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented, IOException, MarshallingException, InvalidRequest {

        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        Identifier id = new Identifier();
        id.setValue(pid);

        logMetacat.debug("Calling archive");
        CNodeService.getInstance(request).archive(session, id);

        TypeMarshaller.marshalTypeToOutputStream(id, out);

    }

    /**
     * Return the requested object format
     *
     * @param fmtidStr
     *            the requested format identifier as a string
     * @throws NotImplemented
     * @throws InsufficientResources
     * @throws NotFound
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws IOException
     * @throws MarshallingException
     */
    private void getFormat(String fmtidStr) throws InvalidRequest,
            ServiceFailure, NotFound, InsufficientResources, NotImplemented,
            IOException, MarshallingException {
        logMetacat.debug("Entering listFormats()");

        ObjectFormatIdentifier fmtid = new ObjectFormatIdentifier();
        fmtid.setValue(fmtidStr);

        // get the specified object format
        ObjectFormat objectFormat = CNodeService.getInstance(request)
                .getFormat(fmtid);

        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        TypeMarshaller.marshalTypeToOutputStream(objectFormat, out);

    }

    /**
     * Reserve the given Identifier
     *
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws IdentifierNotUnique
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    private void reserve() 
        throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, 
        NotImplemented, InvalidRequest {
        Identifier pid = null;
        String scope = null;
        String format = null;

        // Parse the params out of the multipart form data
        logMetacat.debug("Parsing reserve parameters from the mime multipart entity");
        try {
            collectMultipartParams();

        } catch (FileUploadException e1) {
            String msg = "FileUploadException: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4210", msg);

        } catch (IOException e1) {
            String msg = "IOException: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4210", msg);

        } catch (Exception e1) {
            String msg = "Exception: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4210", msg);

        }

        // gather the params
        try {
            String id = multipartparams.get("pid").get(0);
            pid = new Identifier();
            pid.setValue(id);
            
        } catch (NullPointerException e) {
            String msg = "The 'pid' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4200", msg);
 
        }

        // call the implementation
        try {
            Identifier resultPid = CNodeService.getInstance(request).reserveIdentifier(session, pid);
            OutputStream out = response.getOutputStream();
            response.setStatus(200);
            response.setContentType("text/xml");
            // send back the reserved pid
            TypeMarshaller.marshalTypeToOutputStream(resultPid, out);

        } catch (IOException e) {
            String msg = "Couldn't write the identifier to the response output stream: " +
                e.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4210", msg);

        } catch (MarshallingException e) {
            String msg = "Couldn't marshall the identifier to the response output stream: " +
            e.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4210", msg);

        }
    }

    /**
     *
     * @param id
     * @throws InvalidRequest
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws IOException
     * @throws MarshallingException
     */
    private void resolve(String id) throws InvalidRequest, InvalidToken,
            ServiceFailure, NotAuthorized, NotFound, NotImplemented,
            IOException, MarshallingException {
        Identifier pid = new Identifier();
        pid.setValue(id);
        ObjectLocationList locationList = CNodeService.getInstance(request)
                .resolve(session, pid);
        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");
        TypeMarshaller.marshalTypeToOutputStream(locationList, out);

    }

    /**
     * Set the owner of a resource
     *
     * @param id
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotFound
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws VersionMismatch 
     */
    private void owner(String id) 
        throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, 
        NotImplemented, InvalidRequest, InstantiationException, 
        IllegalAccessException, VersionMismatch {

        Identifier pid = new Identifier();
        pid.setValue(id);

        long serialVersion = 0L;
        String serialVersionStr = null;
        String userIdStr = null;
        Subject userId = null;

        // Parse the params out of the multipart form data
        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Parsing rights holder parameters from the mime multipart entity");
        try {
            collectMultipartParams();
            
        } catch (FileUploadException e1) {
            String msg = "FileUploadException: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4490", msg);

        } catch (IOException e1) {
            String msg = "IOException: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4490", msg);

        } catch (Exception e1) {
            String msg = "Exception: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4490", msg);

        }

        // get the serialVersion
        try {
            serialVersionStr = multipartparams.get("serialVersion").get(0);
            serialVersion = Long.parseLong(serialVersionStr);

        } catch (NumberFormatException nfe) {
            String msg = "The 'serialVersion' must be provided as a positive integer and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4442", msg);

        } catch (NullPointerException e) {
            String msg = "The 'serialVersion' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4442", msg);

        }

        // get the subject userId that will become the rights holder
        try {
            userIdStr = multipartparams.get("userId").get(0);
            userId = new Subject();
            userId.setValue(userIdStr);

        } catch (NullPointerException e) {
            String msg = "The 'serialVersion' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4442", msg);

        }

        // set the rights holder
        Identifier retPid = CNodeService.getInstance(request).setRightsHolder(session, pid, userId, serialVersion);

        try {
            OutputStream out = response.getOutputStream();
            response.setStatus(200);
            response.setContentType("text/xml");
            TypeMarshaller.marshalTypeToOutputStream(retPid, out);

        } catch (IOException e) {
            String msg = "Couldn't write the identifier to the response output stream: " +
                e.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4490", msg);

        } catch (MarshallingException e) {
            String msg = "Couldn't marshall the identifier to the response output stream: " +
            e.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4490", msg);

        }
    }

    /**
     * Processes the authorization check for given id
     *
     * @param id
     * @return
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotFound
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    private boolean isAuthorized(String id) throws ServiceFailure,
            InvalidToken, NotFound, NotAuthorized, NotImplemented,
            InvalidRequest {
        Identifier pid = new Identifier();
        pid.setValue(id);
        String permission = params.get("action")[0];
        boolean result = CNodeService.getInstance(request).isAuthorized(
                session, pid, Permission.convert(permission));
        response.setStatus(200);
        response.setContentType("text/xml");
        return result;
    }

    /**
     * Register System Metadata without data or metadata object
     *
     * @param pid
     *            identifier for System Metadata entry
     * @throws MarshallingException
     * @throws FileUploadException
     * @throws IOException
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvalidToken 
     */
    protected void registerSystemMetadata()
            throws ServiceFailure, InvalidRequest, IOException,
            FileUploadException, MarshallingException, NotImplemented, NotAuthorized,
            InvalidSystemMetadata, InstantiationException,
            IllegalAccessException, InvalidToken {

        // Read the incoming data from its Mime Multipart encoding
        Map<String, File> files = collectMultipartFiles();

        // get the encoded pid string from the body and make the object
        String pidString = multipartparams.get("pid").get(0);
        Identifier pid = new Identifier();
        pid.setValue(pidString);

        logMetacat.debug("registerSystemMetadata: " + pid);

        // get the system metadata from the request
        File smFile = files.get("sysmeta");
        FileInputStream sysmeta = new FileInputStream(smFile);
        SystemMetadata systemMetadata = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmeta);

        logMetacat.debug("registering system metadata with pid " + pid.getValue());
        Identifier retGuid = CNodeService.getInstance(request).registerSystemMetadata(session, pid, systemMetadata);

        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        TypeMarshaller.marshalTypeToOutputStream(retGuid, out);

    }

    /**
     * set the access perms on a document
     *
     * @throws MarshallingException
     * @throws InvalidRequest
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws NotFound
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws VersionMismatch 
     */
    protected void setAccess(String pid) throws MarshallingException, InvalidToken,
            ServiceFailure, NotFound, NotAuthorized, NotImplemented,
            InvalidRequest, IOException, InstantiationException,
            IllegalAccessException, ParserConfigurationException, SAXException, VersionMismatch {

        long serialVersion = 0L;
        String serialVersionStr = null;

        // parse the accessPolicy
        Map<String, File> files = collectMultipartFiles();        
        AccessPolicy accessPolicy = TypeMarshaller.unmarshalTypeFromFile(AccessPolicy.class, files.get("accessPolicy"));;

        // get the serialVersion
        try {
            serialVersionStr = multipartparams.get("serialVersion").get(0);
            serialVersion = Long.parseLong(serialVersionStr);

        } catch (NumberFormatException nfe) {
            String msg = "The 'serialVersion' must be provided as a positive integer and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4402", msg);

        } catch (NullPointerException e) {
            String msg = "The 'serialVersion' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4402", msg);

        }

        Identifier id = new Identifier();
        id.setValue(pid);

        CNodeService.getInstance(request).setAccessPolicy(session, id,
                accessPolicy, serialVersion);

    }

    /**
     * List the objects
     *
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotFound
     * @throws IOException
     * @throws MarshallingException
     * @throws Exception
     */
    private void listObjects() throws InvalidToken, ServiceFailure,
            NotAuthorized, InvalidRequest, NotImplemented, NotFound,
            IOException, MarshallingException {

        Date startTime = null;
        Date endTime = null;
        ObjectFormatIdentifier formatId = null;
        boolean replicaStatus = true;
        int start = 0;
        int count = 1000;
        Enumeration<String> paramlist = request.getParameterNames();
        while (paramlist.hasMoreElements()) {
            // parse the params and make the call
            String name = paramlist.nextElement();
            String[] values = request.getParameterValues(name);
            String value = null;
            if (values != null && values.length > 0) {
                value = values[0];
                value = EncodingUtilities.decodeString(value);
            }

            if (name.equals("fromDate") && value != null) {
                try {
                    startTime = DateTimeMarshaller.deserializeDateToUTC(value);
                } catch (Exception e) {
                    // if we can't parse it, just don't use the startTime param
                    logMetacat.warn("Could not parse fromDate: " + value,e);
                    throw new InvalidRequest("1540", "Could not parse fromDate: " + value+" since "+e.getMessage());
                    //startTime = null;
                }
            } else if (name.equals("toDate") && value != null) {
                try {
                    endTime = DateTimeMarshaller.deserializeDateToUTC(value);
                } catch (Exception e) {
                    // if we can't parse it, just don't use the endTime param
                    logMetacat.warn("Could not parse toDate: " + value, e);
                    throw new InvalidRequest("1540", "Could not parse toDate: " + value+" since "+e.getMessage());
                    //endTime = null;
                }
            } else if (name.equals("formatId") && value != null) {
                formatId = new ObjectFormatIdentifier();
                formatId.setValue(value);
            } else if (name.equals("replicaStatus") && value != null) {
                replicaStatus = Boolean.parseBoolean(value);
            } else if (name.equals("start") && value != null) {
                start = Integer.valueOf(value);
            } else if (name.equals("count") && value != null) {
                count = Integer.valueOf(value);
            }
        }
        // make the call
        logMetacat.debug("session: " + session + " fromDate: " + startTime
                + " toDate: " + endTime + " formatId: " + formatId
                + " replicaStatus: " + replicaStatus + " start: " + start
                + " count: " + count);        

        // get the list
        ObjectList ol = CNodeService.getInstance(request).listObjects(session,
                startTime, endTime, formatId, replicaStatus, start, count);

        // send it
        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        // style the object with a processing directive
        String stylesheet = null;
        try {
            stylesheet = PropertyService.getProperty("dataone.types.xsl.v1");
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("Could not locate DataONE types XSLT: "
                    + e.getMessage());
        }

        // Serialize and write it to the output stream
        TypeMarshaller.marshalTypeToOutputStream(ol, out, stylesheet);
    }

    /**
     * Pass the request to get node replication authorization to CNodeService
     *
     * @param pid
     *            the identifier of the object to get authorization to replicate
     * 
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotFound
     * @throws InvalidRequest
     */
    public boolean isNodeAuthorized(String pid) throws NotImplemented,
            NotAuthorized, InvalidToken, ServiceFailure, NotFound,
            InvalidRequest {

        boolean result = false;
        Subject targetNodeSubject = new Subject();
        String nodeSubject = null;
        String replPermission = null;

        // get the pid
        Identifier identifier = new Identifier();
        identifier.setValue(pid);

        // get the target node subject
        try {
            nodeSubject = params.get("targetNodeSubject")[0];
            targetNodeSubject.setValue(nodeSubject);

        } catch (NullPointerException e) {
            String msg = "The 'targetNodeSubject' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4873", msg);

        }

        result = CNodeService.getInstance(request).isNodeAuthorized(session, targetNodeSubject, identifier);

        response.setStatus(200);
        response.setContentType("text/xml");
        return result;

    }

    /**
     * Pass the request to set the replication policy to CNodeService
     *
     * @param pid
     *            the identifier of the object to set the replication policy on
     * 
     * @throws NotImplemented
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidToken
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws MarshallingException
     * @throws VersionMismatch 
     */
    public boolean setReplicationPolicy(String pid) throws NotImplemented,
            NotFound, NotAuthorized, ServiceFailure, InvalidRequest,
            InvalidToken, IOException, InstantiationException,
            IllegalAccessException, MarshallingException, VersionMismatch {

        boolean result = false;
        long serialVersion = 0L;
        String serialVersionStr = null;

        Identifier identifier = new Identifier();
        identifier.setValue(pid);

        // parse the policy
        Map<String, File> files = collectMultipartFiles();
        ReplicationPolicy policy = TypeMarshaller.unmarshalTypeFromFile(ReplicationPolicy.class, files.get("policy"));

        // get the serialVersion
        try {
            serialVersionStr = multipartparams.get("serialVersion").get(0);
            serialVersion = Long.parseLong(serialVersionStr);

        } catch (NullPointerException e) {
            String msg = "The 'serialVersion' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4883", msg);

        }
        result = CNodeService.getInstance(request).setReplicationPolicy(
                session, identifier, policy, serialVersion);
        response.setStatus(200);
        response.setContentType("text/xml");
        return result;

    }

    /**
     * Update the system metadata for a given pid, setting it to be obsoleted
     * by the obsoletedByPid
     *
     * @param pid
     * @return
     * @throws NotImplemented
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidToken
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws VersionMismatch
     */
    public boolean setObsoletedBy(String pid) 
        throws NotImplemented, NotFound, NotAuthorized, ServiceFailure, 
        InvalidRequest, InvalidToken, InstantiationException, 
        IllegalAccessException, VersionMismatch {

        boolean result = false;
        long serialVersion = 0L;
        String serialVersionStr = null;

        Identifier identifier = new Identifier();
        identifier.setValue(pid);
        String obsoletedByPidString = null;
        Identifier obsoletedByPid = null;

        // Parse the params out of the multipart form data
        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Parsing rights holder parameters from the mime multipart entity");
        try {
            collectMultipartParams();

        } catch (FileUploadException e1) {
            String msg = "FileUploadException: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4941", msg);

        } catch (IOException e1) {
            String msg = "IOException: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4941", msg);

        } catch (Exception e1) {
            String msg = "Exception: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4941", msg);

        }

        // get the obsoletedByPid
        try {
            obsoletedByPidString = multipartparams.get("obsoletedByPid").get(0);
            obsoletedByPid = new Identifier();
            obsoletedByPid.setValue(obsoletedByPidString);
        } catch (NullPointerException e) {
            String msg = "The 'obsoletedByPid' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4883", msg);
        }

        // get the serialVersion
        try {
            serialVersionStr = multipartparams.get("serialVersion").get(0);
            serialVersion = Long.parseLong(serialVersionStr);

        } catch (NumberFormatException nfe) {
            String msg = "The 'serialVersion' must be provided as a positive integer and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4942", msg);
                        
        } catch (NullPointerException e) {
            String msg = "The 'serialVersion' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4942", msg);

        }
        result = CNodeService.getInstance(request).setObsoletedBy(session,
            identifier, obsoletedByPid, serialVersion);
        response.setStatus(200);
        response.setContentType("text/xml");
        return result;

    }

    /**
     * Delete the replica entry with the given nodeId for the given pid
     *
     * @param pid
     * @return
     * @throws NotImplemented
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidToken
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws VersionMismatch
     */
    public boolean deleteReplica(String pid) 
        throws NotImplemented, NotFound, NotAuthorized, ServiceFailure, 
        InvalidRequest, InvalidToken, InstantiationException, 
        IllegalAccessException, VersionMismatch {

        boolean result = false;
        long serialVersion = 0L;
        String serialVersionStr = null;

        Identifier identifier = new Identifier();
        identifier.setValue(pid);

        NodeReference nodeId = null;
        
        // Parse the params out of the multipart form data
        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Parsing delete replica parameters from the mime multipart entity");
        try {
            collectMultipartParams();

        } catch (FileUploadException e1) {
            String msg = "FileUploadException: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4951", msg);

        } catch (IOException e1) {
            String msg = "IOException: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4951", msg);

        } catch (Exception e1) {
            String msg = "Exception: Couldn't parse the mime multipart information: " +
            e1.getMessage();
            logMetacat.debug(msg);
            throw new ServiceFailure("4951", msg);

        }

        // get the nodeId param
        try {
            String nodeIdString = multipartparams.get("nodeId").get(0);
            nodeId = new NodeReference();
            nodeId.setValue(nodeIdString);

        } catch (NullPointerException e) {
            String msg = "The 'nodeId' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4952", msg);
        }

        // get the serialVersion
        try {
            serialVersionStr = multipartparams.get("serialVersion").get(0);
            serialVersion = Long.parseLong(serialVersionStr);

        } catch (NumberFormatException nfe) {
            String msg = "The 'serialVersion' must be provided as a positive integer and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4952", msg);

        } catch (NullPointerException e) {
            String msg = "The 'serialVersion' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4952", msg);

        }
        result = CNodeService.getInstance(request).deleteReplicationMetadata(
                session, identifier, nodeId, serialVersion);
        response.setStatus(200);
        response.setContentType("text/xml");
        return result;

    }

    /**
     * Pass the request to set the replication status to CNodeService
     *
     * @param pid
     *            the identifier of the object to set the replication status on
     *
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotFound
     * @throws MarshallingException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws IOException 
     */
    public boolean setReplicationStatus(String pid) throws ServiceFailure,
            NotImplemented, InvalidToken, NotAuthorized, InvalidRequest,
            NotFound {

        boolean result = false;
        Identifier identifier = new Identifier();
        identifier.setValue(pid);
        BaseException failure = null;
        ReplicationStatus status = null;
        String replicationStatus = null;
        NodeReference targetNodeRef = null;
        String targetNode = null;

        // Parse the params out of the multipart form data
        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Parsing ReplicaStatus from the mime multipart entity");

        try {
            // parse the failure, if we have it
            Map<String, File> files = collectMultipartFiles();
            if (files.containsKey("failure")) {
                failure = ExceptionHandler.deserializeXml(new FileInputStream(files.get("failure")),
                        "Replication failed for an unknown reason.");
            }

        } catch (Exception e2) {
            throw new ServiceFailure("4700", "Couldn't resolve the multipart request: " +
                e2.getMessage());

        }

        // get the replication status param
        try {
            replicationStatus = multipartparams.get("status").get(0);
            status = ReplicationStatus.convert(replicationStatus);

        } catch (NullPointerException npe) {

            logMetacat.debug("The 'status' parameter was not found in the "
                    + "multipartparams map.  Trying the params map.");

            try {
                replicationStatus = params.get("status")[0];
                status = ReplicationStatus.convert(replicationStatus
                        .toLowerCase());

            } catch (Exception e) {
                String msg = "The 'status' must be provided as a parameter and was not.";
                logMetacat.error(msg);
                throw new InvalidRequest("4730", msg);

            }

        }

        // get the target node reference param
        try {
            targetNode = multipartparams.get("nodeRef").get(0);
            targetNodeRef = new NodeReference();
            targetNodeRef.setValue(targetNode);

        } catch (NullPointerException e) {
            logMetacat.debug("The 'nodeRef' parameter was not found in the "
                    + "multipartparams map.  Trying the params map.");

            try {
                targetNode = params.get("nodeRef")[0];
                targetNodeRef = new NodeReference();
                targetNodeRef.setValue(targetNode);

            } catch (Exception e1) {
                String msg = "The 'nodeRef' must be provided as a parameter and was not.";
                logMetacat.error(msg);
                throw new InvalidRequest("4730", msg);

            }

        }

        result = CNodeService.getInstance(request).setReplicationStatus(
                session, identifier, targetNodeRef, status, failure);
        response.setStatus(200);
        response.setContentType("text/xml");
        return result;

    }

    /**
     * Pass the request to update the replication metadata to CNodeService
     *
     * @param pid
     *            the identifier of the object to update the replication
     *            metadata on
     *
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotFound
     * @throws VersionMismatch
     * @throws MarshallingException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public boolean updateReplicationMetadata(String pid) throws ServiceFailure,
            NotImplemented, InvalidToken, NotAuthorized, InvalidRequest,
            NotFound, VersionMismatch, InstantiationException, IllegalAccessException, IOException, MarshallingException {

        boolean result = false;
        long serialVersion = 0L;
        String serialVersionStr = null;
        Identifier identifier = new Identifier();
        identifier.setValue(pid);

        // parse the replica
        Map<String, File> files = collectMultipartFiles();        
        Replica replica = TypeMarshaller.unmarshalTypeFromFile(Replica.class, files.get("replicaMetadata"));

        // get the serialVersion
        try {
            serialVersionStr = multipartparams.get("serialVersion").get(0);
            serialVersion = Long.parseLong(serialVersionStr);

        } catch (NullPointerException e) {
            String msg = "The 'serialVersion' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("4853", msg);

        }

        result = CNodeService.getInstance(request).updateReplicationMetadata(
                session, identifier, replica, serialVersion);
        response.setStatus(200);
        response.setContentType("text/xml");
        return result;

    }

}
