package edu.ucsb.nceas.metacat.restservice.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.formats.ObjectFormatInfo;
import org.dataone.exceptions.MarshallingException;
import org.dataone.mimemultipart.MultipartRequest;
import org.dataone.mimemultipart.MultipartRequestResolver;
import org.dataone.portal.TokenGenerator;
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
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.MediaType;
import org.dataone.service.types.v2.MediaTypeProperty;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.DateTimeMarshaller;
import org.dataone.service.util.ExceptionHandler;
import org.dataone.service.util.TypeMarshaller;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.ReadOnlyChecker;
import edu.ucsb.nceas.metacat.common.Settings;
import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeInputStream;
import edu.ucsb.nceas.metacat.dataone.D1AuthHelper;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.doi.DOIException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.D1ResourceHandler;
import edu.ucsb.nceas.metacat.restservice.multipart.CheckedFile;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.metacat.restservice.multipart.MultipartRequestWithSysmeta;
import edu.ucsb.nceas.metacat.restservice.multipart.StreamingMultipartRequestResolver;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * MN REST service implementation handler
 *
 * ******************
 *    MNCore
 *         ping() - GET /d1/mn/monitor/ping
 *         log() - GET /d1/mn/log
 *         **getObjectStatistics() - GET /d1/mn/monitor/object
 *         getOperationsStatistics - GET /d1/mn/monitor/event
 *         **getStatus - GET /d1/mn/monitor/status
 *         getCapabilities() - GET /d1/mn/ and /d1/mn/node
 *
 *    MNRead
 *         get() - GET /d1/mn/object/PID
 *         getSystemMetadata() - GET /d1/mn/meta/PID
 *         describe() - HEAD /d1/mn/object/PID
 *         getChecksum() - GET /d1/mn/checksum/PID
 *         listObjects() - GET /d1/mn/object
 *         synchronizationFailed() - POST /d1/mn/error
 *
 *    MNAuthorization
 *         isAuthorized() - GET /d1/mn/isAuthorized/PID
 *         setAccessPolicy() - PUT /d1/mn/accessRules/PID
 *
 *    MNStorage
 *         create() - POST /d1/mn/object/PID
 *         update() - PUT /d1/mn/object/PID
 *         delete() - DELETE /d1/mn/object/PID
 *         archive() - PUT /d1/mn/archive/PID
 *      updateSystemMetadata() - PUT /d1/mn/meta
 *    systemMetadataChanged() - POST /dirtySystemMetadata/PID
 *
 *    MNReplication
 *         replicate() - POST /d1/mn/replicate
 *         getReplica() - GET /d1/mn/replica
 *
 *    MNAdmin
 *         reindex() - PUT /d1/mn/index
 *         updateIdMetadata() - PUT /d1/mn/identifiers
 *
 * ******************
 * @author leinfelder
 *
 */
public class MNResourceHandler extends D1ResourceHandler {

    // MN-specific API Resources
    protected static final String RESOURCE_MONITOR = "monitor";
    protected static final String RESOURCE_REPLICATE = "replicate";
    protected static final String RESOURCE_REPLICAS = "replica";
    protected static final String RESOURCE_NODE = "node";
    protected static final String RESOURCE_ERROR = "error";
    protected static final String RESOURCE_META_CHANGED = "dirtySystemMetadata";
    protected static final String RESOURCE_GENERATE_ID = "generate";
    protected static final String RESOURCE_PUBLISH = "publish";
    protected static final String RESOURCE_PACKAGE = "packages";
    protected static final String RESOURCE_TOKEN = "token";
    protected static final String RESOURCE_WHOAMI = "whoami";
    //make the status of identifier (e.g. DOI) public
    protected static final String RESOURCE_PUBLISH_IDENTIFIER = "publishIdentifier";
    protected static final String RESOURCE_INDEX = "index";
    protected static final String RESOURCE_IDENTIFIERS = "identifiers";




    // shared executor
    private static ExecutorService executor = null;

    static {
        // use a shared executor service with nThreads == one less than available processors
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int nThreads = availableProcessors * 1;
        nThreads--;
        nThreads = Math.max(1, nThreads);
        executor = Executors.newFixedThreadPool(nThreads);
    }

    /**
     * Initializes new instance by setting servlet context,request and response
     * */
    public MNResourceHandler(ServletContext servletContext,
            HttpServletRequest request, HttpServletResponse response) {
        super(servletContext, request, response);
        logMetacat = LogFactory.getLog(MNResourceHandler.class);
    }

    @Override
    protected boolean isD1Enabled() {

        boolean enabled = false;
        try {
            enabled = Boolean.parseBoolean(PropertyService.getProperty("dataone.mn.services.enabled"));
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Could not check if DataONE is enabled: " + e.getMessage());
        }

        return enabled;
    }

    /**
     * This function is called from REST API servlet and handles each request to the servlet
     *
     * @param httpVerb (GET, POST, PUT or DELETE)
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
            resource = resource.substring(resource.indexOf("/") + 1);

            // default to node info
            if (resource.equals("")) {
                resource = RESOURCE_NODE;
            }

            // get the rest of the path info
            String extra = null;

            logMetacat.info("MNResourceHandler.handle - V2 handling verb " + httpVerb
                                + " request with resource '" + resource + "'");
            logMetacat.debug("resource: '" + resource + "'");
            boolean status = false;

            if (resource != null) {

                if (resource.startsWith(RESOURCE_NODE)) {
                    // node response
                    node();
                    status = true;
                } else if (resource.startsWith(RESOURCE_TOKEN)) {
                    logMetacat.debug("Using resource 'token'");
                    // get
                    if (httpVerb == GET) {
                        // after the command
                        getToken();
                        status = true;
                    }

                } else if (resource.startsWith(RESOURCE_WHOAMI)) {
                    logMetacat.debug("Using resource 'whoami'");
                    // get
                    if (httpVerb == GET) {
                        // after the command
                        whoami();
                        status = true;
                    }

                } else if (resource.startsWith(RESOURCE_IS_AUTHORIZED)) {
                    if (httpVerb == GET) {
                        // after the command
                        extra = parseTrailing(resource, RESOURCE_IS_AUTHORIZED);
                        extra = decode(extra);
                        // check the access rules
                        isAuthorized(extra);
                        status = true;
                        logMetacat.debug("done getting access");
                    }
                } else if (resource.startsWith(RESOURCE_META)) {
                    logMetacat.debug("Using resource 'meta'");
                    // get
                    if (httpVerb == GET) {
                        logMetacat.debug("Using resource 'meta' for GET");
                        // after the command
                        extra = parseTrailing(resource, RESOURCE_META);
                        extra = decode(extra);
                        getSystemMetadataObject(extra);
                        status = true;
                    } else if (httpVerb == PUT) {
                        logMetacat.debug("Using resource 'meta' for PUT");
                        updateSystemMetadata();
                        status = true;
                    }

                } else if (resource.startsWith(RESOURCE_OBJECTS)) {
                    logMetacat.debug("Using resource 'object'");
                    // after the command
                    extra = parseTrailing(resource, RESOURCE_OBJECTS);
                    extra = decode(extra);
                    logMetacat.debug("objectId: " + extra);
                    logMetacat.debug("verb:" + httpVerb);

                    if (httpVerb == GET) {
                        getObject(extra);
                        status = true;
                    } else if (httpVerb == POST) {
                        // part of the params, not the URL
                        putObject(null, FUNCTION_NAME_INSERT);
                        status = true;
                    } else if (httpVerb == PUT) {
                        putObject(extra, FUNCTION_NAME_UPDATE);
                        status = true;
                    } else if (httpVerb == DELETE) {
                        deleteObject(extra);
                        status = true;
                    } else if (httpVerb == HEAD) {
                        describeObject(extra);
                        status = true;
                    }

                } else if (resource.startsWith(RESOURCE_LOG)) {
                    logMetacat.debug("Using resource 'log'");
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
                    logMetacat.debug("Using resource 'checksum'");
                    // handle checksum requests
                    if (httpVerb == GET) {
                        // after the command
                        extra = parseTrailing(resource, Constants.RESOURCE_CHECKSUM);
                        extra = decode(extra);
                        checksum(extra);
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_MONITOR)) {
                    // there are various parts to monitoring
                    if (httpVerb == GET) {
                        // after the command
                        extra = parseTrailing(resource, RESOURCE_MONITOR);
                        extra = decode(extra);
                        // ping
                        if (extra.toLowerCase().equals("ping")) {
                            logMetacat.debug("processing ping request");
                            Date result = MNodeService.getInstance(request).ping();
                            // TODO: send to output
                            status = true;

                        } else if (extra.toLowerCase().equals("status")) {
                            logMetacat.debug("processing status request");
                            getStatus();
                            status = true;
                        }

                    }
                } else if (resource.startsWith(RESOURCE_REPLICATE)) {
                    if (httpVerb == POST) {
                        logMetacat.debug("processing replicate request");
                        replicate();
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_ERROR)) {
                    // sync error
                    if (httpVerb == POST) {
                        syncError();
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_META_CHANGED)) {
                    // system metadata changed
                    if (httpVerb == POST) {
                        systemMetadataChanged();
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_REPLICAS)) {
                    // get replica
                    if (httpVerb == GET) {
                        extra = parseTrailing(resource, RESOURCE_REPLICAS);
                        extra = decode(extra);
                        getReplica(extra);
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_QUERY)) {
                    logMetacat.debug("Using resource " + RESOURCE_QUERY);
                    // after the command
                    extra = parseTrailing(resource, RESOURCE_QUERY);
                    logMetacat.debug("query extra: " + extra);

                    String engine = null;
                    String query = null;

                    if (extra != null) {
                        // get the engine
                        int engineIndex = extra.length();
                        if (extra.indexOf("/") > -1) {
                            engineIndex = extra.indexOf("/");
                        }
                        engine = extra.substring(0, engineIndex);
                        engine = decode(engine);
                        logMetacat.debug("query engine: " + engine);

                        // check the query string first
                        query = request.getQueryString();

                        // if null, look at the whole endpoint
                        if (query == null) {
                            // get the query if it is there
                            query = extra.substring(engineIndex, extra.length());
                            if (query != null && query.length() == 0) {
                                query = null;
                            } else {
                                if (query.startsWith("/")) {
                                    query = query.substring(1);
                                }
                            }
                        }
                        query = decode(query);
                        logMetacat.debug("query: " + query);

                    }
                    logMetacat.debug("verb:" + httpVerb);
                    if (httpVerb == GET) {
                        doQuery(engine, query);
                        status = true;
                    } else if (httpVerb == POST) {
                        doPostQuery(engine);
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_GENERATE_ID)) {
                    // generate an id
                    if (httpVerb == POST) {
                        generateIdentifier();
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_PUBLISH) &&
                                            !resource.startsWith(RESOURCE_PUBLISH_IDENTIFIER)) {
                    logMetacat.debug("Using resource: " + RESOURCE_PUBLISH);
                    // PUT
                    if (httpVerb == PUT) {
                        // after the command
                        extra = parseTrailing(resource, RESOURCE_PUBLISH);
                        extra = decode(extra);
                        publish(extra);
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_PACKAGE)) {
                    logMetacat.debug("Using resource: " + RESOURCE_PACKAGE);
                    // after the command
                    extra = parseTrailing(resource, RESOURCE_PACKAGE);

                    String format = null;
                    String pid = null;

                    if (extra != null) {
                        // get the format
                        int formatIndex = extra.length();
                        if (extra.indexOf("/") > -1) {
                            formatIndex = extra.indexOf("/");
                        }
                        format = extra.substring(0, formatIndex);
                        format = decode(format);
                        logMetacat.debug("package format: " + format);

                        // get the pid if it is there
                        pid = extra.substring(formatIndex, extra.length());
                        if (pid != null && pid.length() == 0) {
                            pid = null;
                        } else {
                            if (pid.startsWith("/")) {
                                pid = pid.substring(1);
                            }
                        }
                        pid = decode(pid);
                        logMetacat.debug("pid: " + pid);

                    }

                    // get
                    if (httpVerb == GET) {

                        getPackage(format, pid);
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_VIEWS)) {
                    logMetacat.debug("Using resource " + RESOURCE_VIEWS);
                    // after the command
                    extra = parseTrailing(resource, RESOURCE_VIEWS);
                    logMetacat.debug("view extra: " + extra);

                    String format = null;
                    String pid = null;

                    if (extra != null) {
                        // get the format
                        int formatIndex = extra.length();
                        if (extra.indexOf("/") > -1) {
                            formatIndex = extra.indexOf("/");
                        }
                        format = extra.substring(0, formatIndex);
                        format = decode(format);
                        logMetacat.debug("view format: " + format);

                        // get the pid if it is there
                        pid = extra.substring(formatIndex, extra.length());
                        if (pid != null && pid.length() == 0) {
                            pid = null;
                        } else {
                            if (pid.startsWith("/")) {
                                pid = pid.substring(1);
                            }
                        }
                        pid = decode(pid);
                        logMetacat.debug("pid: " + pid);

                    }
                    logMetacat.debug("verb:" + httpVerb);
                    if (httpVerb == GET) {
                        doViews(format, pid);
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_PUBLISH_IDENTIFIER)) {
                    logMetacat.debug("Using resource: " + RESOURCE_PUBLISH_IDENTIFIER);
                    // PUT
                    if (httpVerb == PUT) {
                        // after the command
                        extra = parseTrailing(resource, RESOURCE_PUBLISH_IDENTIFIER);
                        extra = decode(extra);
                        publishIdentifier(extra);
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_INDEX)) {
                    logMetacat.debug("Using resource: " + RESOURCE_INDEX);
                    // PUT
                    if (httpVerb == PUT) {
                        extra = parseTrailing(resource, RESOURCE_INDEX);
                        extra = decode(extra);
                        logMetacat.debug("The objectId(extra) in index is: " + extra);
                        reindex(extra);
                        status = true;
                    } else {
                        throw new InvalidRequest("0000", "Metacat only supports the HTTP PUT method"
                                + " to index objects.");
                    }
                } else if (resource.startsWith(RESOURCE_IDENTIFIERS)) {
                    logMetacat.debug("Using resource: " + RESOURCE_IDENTIFIERS);
                    // PUT
                    if (httpVerb == PUT) {
                        extra = parseTrailing(resource, RESOURCE_IDENTIFIERS);
                        extra = decode(extra);
                        logMetacat.debug("The objectId(extra) in updateIdMetadata is: " + extra);
                        updateIdMetadata(extra);
                        status = true;
                    } else {
                        throw new InvalidRequest("0000", "Metacat only supports the HTTP PUT method"
                                + " to update identifiers' metadata.");
                    }
                } else {
                    throw new InvalidRequest("0000", "No resource matched for " + resource);
                }

                if (!status) {
                    throw new ServiceFailure("0000", "Unknown error, status = " + status);
                }
            } else {
                throw new InvalidRequest("0000", "No resource matched for " + resource);
            }
        } catch (BaseException be) {
            // report Exceptions as clearly as possible
            OutputStream out = null;
            try {
                out = response.getOutputStream();
            } catch (IOException e) {
                logMetacat.error("Could not get output stream from response", e);
            }
            serializeException(be, out);
        } catch (Exception e) {
            // report Exceptions as clearly and generically as possible
            logMetacat.error(e.getClass() + ": " + e.getMessage(), e);
            OutputStream out = null;
            try {
                out = response.getOutputStream();
            } catch (IOException ioe) {
                logMetacat.error("Could not get output stream from response", ioe);
            }
            ServiceFailure se = new ServiceFailure("0000", e.getMessage());
            serializeException(se, out);
        }
    }


    private void doQuery(String engine, String query) {

        OutputStream out = null;

        try {
            // NOTE: we set the session explicitly for the MNode instance since these methods
            //do not provide a parameter
            if (engine == null) {
                // just looking for list of engines
                MNodeService mnode = MNodeService.getInstance(request);
                mnode.setSession(session);
                QueryEngineList qel = mnode.listQueryEngines(session);
                response.setContentType("text/xml");
                response.setStatus(200);
                out = response.getOutputStream();
                TypeMarshaller.marshalTypeToOutputStream(qel, out);
                IOUtils.closeQuietly(out);
                return;
            } else {
                if (query != null) {
                    long start = System.currentTimeMillis();
                    MNodeService mnode = MNodeService.getInstance(request);
                    mnode.setSession(session);
                    InputStream stream = mnode.query(session, engine, query);

                    // set the content-type if we have it from the implementation
                    if (stream instanceof ContentTypeInputStream) {
                        response.setContentType(((ContentTypeInputStream) stream).getContentType());
                    }
                    response.setStatus(200);
                    out = response.getOutputStream();
                    // write the results to the output stream
                    IOUtils.copyLarge(stream, out);
                    long end = System.currentTimeMillis();
                    logMetacat.info(Settings.PERFORMANCELOG + Settings.PERFORMANCELOG_QUERY_METHOD
                                    + query + " Total query method"
                                    + Settings.PERFORMANCELOG_DURATION + (end-start)/1000);
                    IOUtils.closeQuietly(out);
                    return;
                } else {
                    MNodeService mnode = MNodeService.getInstance(request);
                    mnode.setSession(session);
                    QueryEngineDescription qed = mnode.getQueryEngineDescription(session, engine);
                    response.setContentType("text/xml");
                    response.setStatus(200);
                    out = response.getOutputStream();
                    TypeMarshaller.marshalTypeToOutputStream(qed, out);
                    IOUtils.closeQuietly(out);
                    return;
                }
            }


        } catch (BaseException be) {
            // report Exceptions as clearly as possible
            try {
                out = response.getOutputStream();
            } catch (IOException e) {
                logMetacat.error("Could not get output stream from response", e);
            }
            serializeException(be, out);
        } catch (Exception e) {
            // report Exceptions as clearly and generically as possible
            logMetacat.error(e.getClass() + ": " + e.getMessage(), e);
            try {
                out = response.getOutputStream();
            } catch (IOException ioe) {
                logMetacat.error("Could not get output stream from response", ioe);
            }
            ServiceFailure se = new ServiceFailure("0000", e.getMessage());
            serializeException(se, out);
        }
    }

    /*
     * Handle the solr query sent by the http post method
     */
    private void doPostQuery(String engine) {
        OutputStream out = null;
        try {
            // NOTE: we set the session explicitly for the MNode instance since these methods
            //do not provide a parameter
            collectMultipartParams();
            MNodeService mnode = MNodeService.getInstance(request);
            if(multipartparams == null || multipartparams.isEmpty()) {
                throw new InvalidRequest("2823",
                        "The request doesn't have any query information by the HTTP POST method.");
            }
            HashMap<String, String[]> params = new HashMap<String, String[]>();
            for(String key : multipartparams.keySet()) {
                List<String> values = multipartparams.get(key);
                logMetacat.debug("MNResourceHandler.doPostQuery -the key "+key +" has the value "+values);
                if(values != null) {
                    String[] arrayValues = values.toArray(new String[0]);
                    params.put(key, arrayValues);
                }
            }
            mnode.setSession(session);
            InputStream stream = mnode.postQuery(session, engine, params);
            // set the content-type if we have it from the implementation
            if (stream instanceof ContentTypeInputStream) {
                response.setContentType(((ContentTypeInputStream) stream).getContentType());
            }
            response.setStatus(200);
            out = response.getOutputStream();
            // write the results to the output stream
            IOUtils.copyLarge(stream, out);
            IOUtils.closeQuietly(out);
            return;
        } catch (BaseException be) {
            // report Exceptions as clearly as possible
            try {
                out = response.getOutputStream();
            } catch (IOException e) {
                logMetacat.error("Could not get output stream from response", e);
            }
            serializeException(be, out);
        } catch (Exception e) {
            // report Exceptions as clearly and generically as possible
            logMetacat.error(e.getClass() + ": " + e.getMessage(), e);
            try {
                out = response.getOutputStream();
            } catch (IOException ioe) {
                logMetacat.error("Could not get output stream from response", ioe);
            }
            ServiceFailure se = new ServiceFailure("0000", e.getMessage());
            serializeException(se, out);
        }
    }

    private void doViews(String format, String pid) {

        OutputStream out = null;
        MNodeService mnode = MNodeService.getInstance(request);

        try {
            // get a list of views
            if (pid != null) {
                long start = System.currentTimeMillis();
                Identifier identifier = new Identifier();
                identifier.setValue(pid);
                InputStream stream = null;
                try {
                    stream = mnode.view(session, format, identifier);
                // set the content-type if we have it from the implementation
                if (stream instanceof ContentTypeInputStream) {
                    response.setContentType(((ContentTypeInputStream) stream).getContentType());
                }
                response.setStatus(200);
                out = response.getOutputStream();
                // write the results to the output stream
                IOUtils.copyLarge(stream, out);
                } finally {
                    if (stream != null) {
                    IOUtils.closeQuietly(stream);
                    }
                }
            long end = System.currentTimeMillis();
            IOUtils.closeQuietly(out);
            logMetacat.info(Settings.PERFORMANCELOG + pid + Settings.PERFORMANCELOG_VIEW_METHOD
                              + " Total view method" + Settings.PERFORMANCELOG_DURATION
                              + (end-start)/1000);
            return;
            } else {
                // TODO: list the registered views
                OptionList list = mnode.listViews(session);

                response.setContentType("text/xml");
                response.setStatus(200);
                TypeMarshaller.marshalTypeToOutputStream(list, response.getOutputStream());
                IOUtils.closeQuietly(response.getOutputStream());
            }


        } catch (BaseException be) {
            // report Exceptions as clearly as possible
            try {
                out = response.getOutputStream();
            } catch (IOException e) {
                logMetacat.error("Could not get output stream from response", e);
            }
            serializeException(be, out);
        } catch (Exception e) {
            // report Exceptions as clearly and generically as possible
            logMetacat.error(e.getClass() + ": " + e.getMessage(), e);
            try {
                out = response.getOutputStream();
            } catch (IOException ioe) {
                logMetacat.error("Could not get output stream from response", ioe);
            }
            ServiceFailure se = new ServiceFailure("0000", e.getMessage());
            serializeException(se, out);
        }
    }

    /**
     * Handles notification of system metadata changes for the given identifier
     *
     * @param id  the identifier for the object
     * @throws InvalidToken
     * @throws InvalidRequest
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws NotImplemented
     */
    private void systemMetadataChanged()
        throws NotImplemented, ServiceFailure, NotAuthorized, InvalidRequest,
        InvalidToken {

        ReadOnlyChecker checker = new ReadOnlyChecker();
        boolean isReadOnlyMode = checker.isReadOnly();
        if(isReadOnlyMode) {
            throw new ServiceFailure("1333", ReadOnlyChecker.DATAONEERROR);
        }

        //final long serialVersion = 0L;
        String serialVersionStr = null;
        String dateSysMetaLastModifiedStr = null;

        // mkae sure we have the multipart params
        try {
            initMultipartParams();
        } catch (Exception e1) {
            throw new ServiceFailure("1333", "Could not collect the multipart params for the request");
        }

        // get the pid
        String id = null;
        try {
            id = multipartparams.get("pid").get(0);
        } catch (NullPointerException e) {
            String msg = "The 'pid' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("1334", msg);
        }
        final Identifier pid = new Identifier();
        pid.setValue(id);

        // get the serialVersion
        try {
            serialVersionStr = multipartparams.get("serialVersion").get(0);
        } catch (NullPointerException e) {
            String msg = "The 'serialVersion' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("1334", msg);

        }

        final long serialVersion = Long.parseLong(serialVersionStr);

        // get the dateSysMetaLastModified
        try {
            dateSysMetaLastModifiedStr = multipartparams.get("dateSysMetaLastModified").get(0);


        } catch (NullPointerException e) {
            String msg =
                "The 'dateSysMetaLastModified' must be provided as a " +
                "parameter and was not, or was an invalid representation of the timestamp.";
            logMetacat.error(msg);
            throw new InvalidRequest("1334", msg);

        }
        final Date dateSysMetaLastModified = DateTimeMarshaller
                                        .deserializeDateToUTC(dateSysMetaLastModifiedStr);

        // check authorization before sending to implementation
        D1AuthHelper authDel = new D1AuthHelper(request, pid, "1331", "????");
        authDel.doAdminAuthorization(session);

        // run it in a thread to avoid connection timeout
        final String ipAddress = request.getRemoteAddr();
        final String userAgent = request.getHeader("User-Agent");
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                   // call the service
                    MNodeService.getInstance(request, ipAddress, userAgent)
                       .systemMetadataChanged(session, pid, serialVersion, dateSysMetaLastModified);
                } catch (Exception e) {
                    logMetacat.error("Error running replication: " + e.getMessage(), e);
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        };
        // submit the task, and that's it
        executor.submit(runner);

        // thread was started, so we return success
        response.setStatus(200);
    }

    /**
     * Handles identifier generation calls
     *
     * @throws InvalidRequest
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws IOException
     * @throws MarshallingException
     */
    private void generateIdentifier() throws InvalidToken, ServiceFailure, NotAuthorized,
                                NotImplemented, InvalidRequest, IOException, MarshallingException {

        // make sure we have the multipart params
        try {
            initMultipartParams();
        } catch (Exception e1) {
            throw new ServiceFailure("1333", "Could not collect the multipart params for the request");
        }

        // get the scheme
        String scheme = null;
        try {
            scheme = multipartparams.get("scheme").get(0);
        } catch (NullPointerException e) {
            String msg = "The 'scheme' parameter was not provided, using default";
            logMetacat.warn(msg);
        }

        // get the fragment
        String fragment = null;
        try {
            fragment = multipartparams.get("fragment").get(0);
        } catch (NullPointerException e) {
            String msg = "The 'fragment' parameter was not provided, using default";
            logMetacat.warn(msg);
        }

        // call the service
        Identifier identifier = MNodeService.getInstance(request).generateIdentifier(session, scheme, fragment);
        response.setStatus(200);
        response.setContentType("text/xml");
        OutputStream out = response.getOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(identifier, out);
        IOUtils.closeQuietly(out);
    }

    /**
     * Checks the access policy
     * @param id
     * @return
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotFound
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidRequest
     */
    private boolean isAuthorized(String id) throws ServiceFailure, InvalidToken, NotFound,
                                                    NotAuthorized, NotImplemented, InvalidRequest {
        Identifier pid = new Identifier();
        pid.setValue(id);
        Permission permission = null;
        try {
            String perm = params.get("action")[0];
            permission = Permission.convert(perm);
        } catch (Exception e) {
            logMetacat.warn("No permission specified");
        }
        boolean result = MNodeService.getInstance(request).isAuthorized(session, pid, permission);
        response.setStatus(200);
        response.setContentType("text/xml");
        return result;
    }

    private void getToken() throws Exception {

        if (this.session != null) {
            String userId = this.session.getSubject().getValue();
            String fullName = null;
            try {
                Person person = this.session.getSubjectInfo().getPerson(0);
                fullName = person.getGivenName(0) + " " + person.getFamilyName();
            } catch (Exception e) {
                logMetacat.warn(e.getMessage(), e);
            }

            String token = null;
            token = TokenGenerator.getInstance().getJWT(userId, fullName);

            response.setStatus(200);
            response.setContentType("text/plain");
            OutputStream out = response.getOutputStream();
            out.write(token.getBytes(MetaCatServlet.DEFAULT_ENCODING));
            out.close();
        } else {
            response.setStatus(401);
            response.setContentType("text/plain");
            OutputStream out = response.getOutputStream();
            out.write("No session information found".getBytes(MetaCatServlet.DEFAULT_ENCODING));
            out.close();
        }

    }

    private void whoami() throws Exception {

        if (this.session != null) {
            Subject subject = this.session.getSubject();
            SubjectInfo subjectInfo = null;
            try {
                subjectInfo = this.session.getSubjectInfo();
            } catch (Exception e) {
                logMetacat.warn(e.getMessage(), e);
            }

            response.setStatus(200);
            response.setContentType("text/plain");
            OutputStream out = response.getOutputStream();

            if (subjectInfo != null) {
                TypeMarshaller.marshalTypeToOutputStream(subjectInfo, out);
            } else {
                TypeMarshaller.marshalTypeToOutputStream(subject, out);
            }

            out.close();
        } else {
            response.setStatus(401);
            response.setContentType("text/plain");
            OutputStream out = response.getOutputStream();
            out.write("No session information found".getBytes(MetaCatServlet.DEFAULT_ENCODING));
            out.close();
        }

    }

    /**
     * Get the status of the system.
     * It showed the size of the index queue. But it is no longer supported.
     */
    private void getStatus() throws NotImplemented {
        throw new NotImplemented("0000", "The index status feature is no longer supported.");
    }

    /**
     * Processes failed synchronization message
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws MarshallingException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     */
    private void syncError() throws NotImplemented, ServiceFailure, NotAuthorized, InvalidRequest,
                MarshallingException, IOException, InstantiationException, IllegalAccessException {
        SynchronizationFailed syncFailed = null;
        try {
            syncFailed = collectSynchronizationFailed();
        } catch (ParserConfigurationException e) {
            throw new ServiceFailure("2161", e.getMessage());
        } catch (SAXException e) {
            throw new ServiceFailure("2161", e.getMessage());
        }

        MNodeService.getInstance(request).synchronizationFailed(session, syncFailed);
    }

    /**
     * Calculate the checksum
     * @throws NotImplemented
     * @throws MarshallingException
     * @throws IOException
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws InvalidRequest
     */
    private void checksum(String pid) throws NotImplemented, MarshallingException, IOException,
                            InvalidToken, ServiceFailure, NotAuthorized, NotFound, InvalidRequest {
        String checksumAlgorithm = "MD5";
        try {
            checksumAlgorithm = PropertyService.getProperty("dataone.checksumAlgorithm.default");
        } catch(Exception e) {
            logMetacat.warn("Could not lookup configured default checksum algorithm, using: "
                                                + checksumAlgorithm);
        }

        Identifier pidid = new Identifier();
        pidid.setValue(pid);
        try {
            checksumAlgorithm = params.get("checksumAlgorithm")[0];
        } catch(Exception e) {
            //do nothing.  use the default
            logMetacat.warn("No algorithm specified, using default: " + checksumAlgorithm);
        }
        logMetacat.debug("getting checksum for object " + pid + " with algorithm " + checksumAlgorithm);

        Checksum c = MNodeService.getInstance(request).getChecksum(session, pidid, checksumAlgorithm);
        logMetacat.debug("got checksum " + c.getValue());
        response.setStatus(200);
        logMetacat.debug("serializing response");
        TypeMarshaller.marshalTypeToOutputStream(c, response.getOutputStream());
        logMetacat.debug("done serializing response.");
        IOUtils.closeQuietly(response.getOutputStream());

    }

    /**
     * handle the replicate action for MN
     * @throws MarshallingException
     * @throws FileUploadException
     * @throws IOException
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvalidToken
     */
    private void replicate()
        throws ServiceFailure, InvalidRequest, IOException, FileUploadException,
        MarshallingException, NotImplemented, NotAuthorized, InsufficientResources,
        UnsupportedType, InstantiationException, IllegalAccessException, InvalidToken {

        logMetacat.debug("in POST replicate()");
        ReadOnlyChecker checker = new ReadOnlyChecker();
        boolean isReadOnlyMode = checker.isReadOnly();
        if(isReadOnlyMode) {
            throw new ServiceFailure("2151", ReadOnlyChecker.DATAONEERROR);
        }

        // somewhat unorthodox, but the call is asynchronous and we'd like to return this info sooner
        boolean allowed = false;
        if (session == null) {
            String msg = "No session was provided.";
            NotAuthorized failure = new NotAuthorized("2152", msg);
            throw failure;
        } else {
            // TODO: should we refactore replicate() in MNodeservice to not replicate, it would
            //avoid a possible second listNodes call...
            D1AuthHelper authDel = new D1AuthHelper(request, null, "2152", "????");
            authDel.doAdminAuthorization(session);
        }

        // parse the systemMetadata
        Map<String, File> files = collectMultipartFiles();
        final SystemMetadata sysmeta = TypeMarshaller
                                .unmarshalTypeFromFile(SystemMetadata.class, files.get("sysmeta"));

        String sn = multipartparams.get("sourceNode").get(0);
        logMetacat.debug("sourceNode: " + sn);
        final NodeReference sourceNode = new NodeReference();
        sourceNode.setValue(sn);

        // run it in a thread to avoid connection timeout
        final String ipAddress = request.getRemoteAddr();
        final String userAgent = request.getHeader("User-Agent");
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                    MNodeService.getInstance(request, ipAddress, userAgent)
                                                        .replicate(session, sysmeta, sourceNode);
                } catch (Exception e) {
                    logMetacat.error("Error running replication: " + e.getMessage(), e);
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        };
        // submit the task, and that's it
        executor.submit(runner);

        // thread was started, so we return success
        response.setStatus(200);

    }

    /**
     * Handle the getReplica action for the MN
     * @param id  the identifier for the object
     * @throws NotFound
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws InvalidToken
     * @throws InvalidRequest
     */
    private void getReplica(String id)
        throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented,
        ServiceFailure, NotFound {

        Identifier pid = new Identifier();
        pid.setValue(id);
        OutputStream out = null;
        InputStream dataBytes = null;

        try {
            // call the service
            dataBytes = MNodeService.getInstance(request).getReplica(session, pid);

            response.setContentType("application/octet-stream");
            response.setStatus(200);
            out = response.getOutputStream();
            // write the object to the output stream
            IOUtils.copyLarge(dataBytes, out);
            IOUtils.closeQuietly(out);
        } catch (IOException e) {
            String msg = "There was an error writing the output: " + e.getMessage();
            logMetacat.error(msg);
            throw new ServiceFailure("2181", msg);

        }

    }

    /**
     * Get the Node information
     *
     * @throws MarshallingException
     * @throws IOException
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotImplemented
     */
    private void node()
        throws MarshallingException, IOException, NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest {

        Node n = MNodeService.getInstance(request).getCapabilities();

        response.setContentType("text/xml");
        response.setStatus(200);
        TypeMarshaller.marshalTypeToOutputStream(n, response.getOutputStream());
        IOUtils.closeQuietly(response.getOutputStream());

    }

    /**
     * MN_crud.describe()
     * http://mule1.dataone.org/ArchitectureDocs/mn_api_crud.html#MN_crud.describe
     * @param pid
     * @throws InvalidRequest
     * @throws NotImplemented
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     */
    private void describeObject(String pid) throws InvalidToken, ServiceFailure, NotAuthorized,
                                                          NotFound, NotImplemented, InvalidRequest
    {

        response.setContentType("text/xml");

        Identifier id = new Identifier();
        id.setValue(pid);

        DescribeResponse dr = null;
        try {
            dr = MNodeService.getInstance(request).describe(session, id);
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
        response.addHeader("DataONE-Checksum", dr.getDataONE_Checksum().getAlgorithm() + ","
                                                + dr.getDataONE_Checksum().getValue());
        response.addHeader("Content-Length", dr.getContent_Length() + "");
        response.addHeader("Last-Modified", DateTimeMarshaller.serializeDateToUTC(dr.getLast_Modified()));
        response.addHeader("DataONE-ObjectFormat", dr.getDataONE_ObjectFormatIdentifier().getValue());
        response.addHeader("DataONE-SerialVersion", dr.getSerialVersion().toString());


    }

    /**
     * get the logs based on passed params.  Available
     * See http://mule1.dataone.org/ArchitectureDocs/mn_api_crud.html#MN_crud.getLogRecords
     * for more info
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws IOException
     * @throws MarshallingException
     */
    private void getLog() throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest,
                                                NotImplemented, IOException, MarshallingException
    {

        Date fromDate = null;
        Date toDate = null;
        String event = null;
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
            event = params.get("event")[0];
        } catch (Exception e) {
            logMetacat.warn("Could not parse event: " + e.getMessage());
        }
        logMetacat.debug("fromDate: " + fromDate + " toDate: " + toDate);

        try {
            start =  Integer.parseInt(params.get("start")[0]);
        } catch (Exception e) {
            logMetacat.warn("Could not parse start: " + e.getMessage());
        }
        try {
            count =  Integer.parseInt(params.get("count")[0]);
        } catch (Exception e) {
            logMetacat.warn("Could not parse count: " + e.getMessage());
        }

        try {
            pidFilter = params.get("idFilter")[0];
        } catch (Exception e) {
            logMetacat.warn("Could not parse pidFilter: " + e.getMessage());
        }

        logMetacat.debug("calling getLogRecords");
        Log log = MNodeService.getInstance(request)
                        .getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);

        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        TypeMarshaller.marshalTypeToOutputStream(log, out);
        IOUtils.closeQuietly(out);
    }



    /**
     * Implements REST version of DataONE CRUD API --> get
     * @param pid ID of data object to be read
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws IOException
     * @throws MarshallingException
     */
    protected void getObject(String pid) throws InvalidToken, ServiceFailure, NotAuthorized,
                    NotFound, InvalidRequest, NotImplemented, IOException, MarshallingException {
        OutputStream out = null;

        if (pid != null) { //get a specific document
            long start = System.currentTimeMillis();
            Identifier id = new Identifier();
            id.setValue(pid);

            SystemMetadata sm = MNodeService.getInstance(request).getSystemMetadata(session, id);

            // set the headers for the content
            String mimeType = null;
            String charset = null;
            ObjectFormat objectFormat = null;

            try {
                objectFormat = ObjectFormatCache.getInstance().getFormat(sm.getFormatId());
            } catch (BaseException be) {
                logMetacat.warn("Could not lookup ObjectFormat for: " + sm.getFormatId(), be);
            }
            // do we have mediaType/encoding in SM?
            MediaType mediaType = sm.getMediaType();
            if (mediaType == null && objectFormat != null) {
                try {
                    mediaType = objectFormat.getMediaType();
                } catch (Exception e) {
                    logMetacat.warn("Could not lookup MediaType for: " + sm.getFormatId(), e);
                }
            }
            if (mediaType != null) {
                mimeType = mediaType.getName();
                if (mediaType.getPropertyList() != null) {
                    Iterator<MediaTypeProperty> iter = mediaType.getPropertyList().iterator();
                    while (iter.hasNext()) {
                        MediaTypeProperty mtp = iter.next();
                        if (mtp.getName().equalsIgnoreCase("charset")) {
                            charset = mtp.getValue();
                            mimeType += "; charset=" + charset;
                            break;
                        }
                    }
                }
            }
            // check object format

            // use the fallback from v1 impl
            if (mimeType == null) {
                mimeType = ObjectFormatInfo.instance().getMimeType(sm.getFormatId().getValue());

                // still null?
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
            }

            // check for filename in SM first
            String filename = sm.getFileName();
            // then fallback to using id and extension
            if (filename == null) {
                String extension = null;
                if(objectFormat != null) {
                    extension = objectFormat.getExtension();
                }
                if (extension == null) {
                    extension = ObjectFormatInfo.instance().getExtension(sm.getFormatId().getValue());
                }
                filename = id.getValue();
                if (extension != null && filename != null && !filename.endsWith(extension)) {
                    filename = id.getValue() + "." + extension;
                }
            }
            response.setContentType(mimeType);
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename+"\"");
            InputStream data = null;
            try {
                data = MNodeService.getInstance(request).get(session, id);
                out = response.getOutputStream();
                response.setStatus(200);
                IOUtils.copyLarge(data, out);
                IOUtils.closeQuietly(out);
            } finally {
                if (data != null) {
                   IOUtils.closeQuietly(data);
                }
            }
            long end = System.currentTimeMillis();
            logMetacat.info(Settings.PERFORMANCELOG + pid + Settings.PERFORMANCELOG_GET_METHOD
                    + " Total get method" + Settings.PERFORMANCELOG_DURATION + (end-start)/1000);
        } else {
            //call listObjects with specified params
            Date startTime = null;
            Date endTime = null;
            ObjectFormatIdentifier formatId = null;
            Identifier identifier = null;
            boolean replicaStatus = true;
            int start = 0;
            //TODO: make the max count into a const
            int count = 1000;
            Enumeration paramlist = request.getParameterNames();
            while (paramlist.hasMoreElements()) {
                //parse the params and make the crud call
                String name = (String) paramlist.nextElement();
                String[] value = (String[])request.getParameterValues(name);

                if (name.equals("fromDate") && value != null) {
                    try {
                        startTime = DateTimeMarshaller.deserializeDateToUTC(value[0]);
                    } catch(Exception e) {
                        //if we can't parse it, just don't use the fromDate param
                        logMetacat.warn("Could not parse fromDate: " + value[0], e);
                        throw new InvalidRequest("1540", "Could not parse fromDate: "
                                                + value[0] + " since " + e.getMessage());
                    }
                } else if(name.equals("toDate") && value != null) {
                    try {
                        endTime = DateTimeMarshaller.deserializeDateToUTC(value[0]);
                    } catch(Exception e) {
                        //if we can't parse it, just don't use the toDate param
                        logMetacat.warn("Could not parse toDate: " + value[0], e);
                        throw new InvalidRequest("1540", "Could not parse toDate: "
                                                + value[0] + " since " +e.getMessage());
                    }
                } else if(name.equals("formatId") && value != null) {
                    formatId = new ObjectFormatIdentifier();
                    formatId.setValue(value[0]);
                } else if(name.equals("identifier") && value != null) {
                    identifier = new Identifier();
                    identifier.setValue(value[0]);
                } else if(name.equals("replicaStatus") && value != null) {
                    if(value != null &&
                       value.length > 0 &&
                       (value[0].equalsIgnoreCase("false") || value[0].equalsIgnoreCase("no"))) {
                        replicaStatus = false;
                    }
                } else if(name.equals("start") && value != null) {
                    start = Integer.parseInt(value[0]);
                } else if(name.equals("count") && value != null) {
                    count = Integer.parseInt(value[0]);
                }
            }
            //make the crud call
            logMetacat.debug("session: " + session + " startTime: " + startTime +
                    " endTime: " + endTime + " formatId: " +
                    formatId + " replicaStatus: " + replicaStatus +
                    " start: " + start + " count: " + count);

            ObjectList ol =
                MNodeService.getInstance(request).listObjects(session, startTime, endTime,
                       formatId, identifier, replicaStatus, start, count);

            out = response.getOutputStream();
            response.setStatus(200);
            response.setContentType("text/xml");
            // Serialize and write it to the output stream
            TypeMarshaller.marshalTypeToOutputStream(ol, out);
            IOUtils.closeQuietly(out);
        }

    }


    /**
     * Retrieve data package as Bagit zip
     * @param pid
     * @throws NotImplemented
     * @throws NotFound
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws IOException
     * @throws InvalidRequest
     */
    protected void getPackage(String format, String pid) throws InvalidToken, ServiceFailure,
                            NotAuthorized, NotFound, NotImplemented, IOException, InvalidRequest {
        long start = System.currentTimeMillis();
        Identifier id = new Identifier();
        id.setValue(pid);
        ObjectFormatIdentifier formatId = null;
        if (format != null) {
            formatId = new ObjectFormatIdentifier();
            formatId.setValue(format);
        }
        InputStream is = null;
        try {
            is = MNodeService.getInstance(request).getPackage(session, formatId , id);

            //Use the pid as the file name prefix, replacing all non-word characters
            String filename = pid.replaceAll("\\W", "_") + ".zip";

            response.setHeader("Content-Disposition", "inline; filename=\"" + filename+"\"");
            response.setContentType("application/zip");
            response.setStatus(200);
            OutputStream out = response.getOutputStream();

            // write it to the output stream
            IOUtils.copyLarge(is, out);
            IOUtils.closeQuietly(out);
            long end = System.currentTimeMillis();
            logMetacat.info(Settings.PERFORMANCELOG + pid
                                    + Settings.PERFORMANCELOG_GET_PACKAGE_METHOD
                                    + " Total getPackage method"
                                    + Settings.PERFORMANCELOG_DURATION + (end-start)/1000);

        } finally {
            IOUtils.closeQuietly(is);
        }
   }

    protected void publish(String pid) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotFound, NotImplemented, IOException,
            MarshallingException, InvalidRequest, IdentifierNotUnique,
            UnsupportedType, InsufficientResources, InvalidSystemMetadata {

        // publish the object
        Identifier originalIdentifier = new Identifier();
        originalIdentifier.setValue(pid);
        Identifier newIdentifier = MNodeService.getInstance(request).publish(session, originalIdentifier);

        response.setStatus(200);
        response.setContentType("text/xml");
        OutputStream out = response.getOutputStream();

        // write new identifier to the output stream
        TypeMarshaller.marshalTypeToOutputStream(newIdentifier, out);
        IOUtils.closeQuietly(out);
    }

    /**
     * Make the status of the identifier public
     * @param identifier
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
     * @throws IOException
     * @throws MarshallingException
     */
    protected void publishIdentifier(String identifier) throws InvalidToken, ServiceFailure,
                             NotAuthorized, NotImplemented, InvalidRequest, NotFound,
                             IdentifierNotUnique, UnsupportedType, InsufficientResources,
                            InvalidSystemMetadata, DOIException, IOException, MarshallingException {
        Identifier id = new Identifier();
        id.setValue(identifier);
        MNodeService.getInstance(request).publishIdentifier(session, id);
        //the publish started in another thread, we just set the status to success
        response.setStatus(200);
    }

    /**
     * Retrieve System Metadata
     * @param pid
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws InvalidRequest
     * @throws NotImplemented
     * @throws IOException
     * @throws MarshallingException
     */
    protected void getSystemMetadataObject(String pid) throws InvalidToken, ServiceFailure,
                                        NotAuthorized, NotFound, InvalidRequest, NotImplemented,
                                                             IOException, MarshallingException {

        Identifier id = new Identifier();
        id.setValue(pid);
        SystemMetadata sysmeta = MNodeService.getInstance(request).getSystemMetadata(session, id);

        response.setContentType("text/xml");
        response.setStatus(200);
        OutputStream out = response.getOutputStream();

        // Serialize and write it to the output stream
        TypeMarshaller.marshalTypeToOutputStream(sysmeta, out);
        IOUtils.closeQuietly(out);
   }


    /**
     * Inserts or updates the object
     *
     * @param pid - ID of data object to be inserted or updated.  If action is update, the pid
     *               is the existing pid.  If insert, the pid is the new one
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws MarshallingException
     * @throws NotImplemented
     * @throws InvalidSystemMetadata
     * @throws InsufficientResources
     * @throws UnsupportedType
     * @throws IdentifierNotUnique
     * @throws NotAuthorized
     * @throws InvalidToken
     * @throws NotFound
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws FileUploadException
     * @throws NoSuchAlgorithmException
     */
    protected void putObject(String trailingPid, String action) throws ServiceFailure,
                            InvalidRequest, MarshallingException, InvalidToken, NotAuthorized,
                            IdentifierNotUnique, UnsupportedType, InsufficientResources,
                            InvalidSystemMetadata, NotImplemented, NotFound, IOException,
                                            InstantiationException, IllegalAccessException,
                                                NoSuchAlgorithmException, FileUploadException {
        CheckedFile objFile = null;
        try {
            long start = System.currentTimeMillis();
            // Read the incoming data from its Mime Multipart encoding
            MultipartRequestWithSysmeta multiparts = collectObjectFiles();
            Map<String, File> files = multiparts.getMultipartFiles();
            objFile = (CheckedFile) files.get("object");
            // ensure we have the object bytes
            if (objFile == null) {
                throw new InvalidRequest("1102", "The object param must contain the object bytes.");
            }
            DetailedFileInputStream object = new DetailedFileInputStream(objFile, objFile.getChecksum());

                Identifier pid = new Identifier();
                if (trailingPid == null) {
                    // get the pid string from the body and set the value
                    String pidString = multipartparams.get("pid").get(0);
                    if (pidString != null) {
                    pid.setValue(pidString);

                  } else {
                      throw new InvalidRequest("1102",
                                      "The pid param must be included and contain the identifier.");

                  }
                } else {
                    // use the pid included in the URL
                    pid.setValue(trailingPid);
                }
                logMetacat.debug("putObject with pid " + pid.getValue());
                logMetacat.debug("Entering putObject: " + pid.getValue() + "/" + action);

                SystemMetadata smd = (SystemMetadata) multiparts.getSystemMetadata();
                if  ( smd == null ) {
                    throw new InvalidRequest("1102", "The sysmeta param must contain the system metadata document.");

                }

                response.setStatus(200);
                response.setContentType("text/xml");
                OutputStream out = response.getOutputStream();

                if (action.equals(FUNCTION_NAME_INSERT)) {
                    // handle inserts
                    logMetacat.debug("Commence creation...");

                    logMetacat.debug("creating object with pid " + pid.getValue());
                    Identifier rId = MNodeService.getInstance(request).create(session, pid, object, smd);
                    TypeMarshaller.marshalTypeToOutputStream(rId, out);

                } else if (action.equals(FUNCTION_NAME_UPDATE)) {
                    // handle updates

                    // construct pids
                    Identifier newPid = null;
                    try {
                        String newPidString = multipartparams.get("newPid").get(0);
                        newPid = new Identifier();
                        newPid.setValue(newPidString);
                    } catch (Exception e) {
                        logMetacat.error("Could not get newPid from request");
                    }
                    logMetacat.debug("Commence update...");
                    Identifier rId = MNodeService.getInstance(request)
                                                .update(session, pid, object, newPid, smd);
                    TypeMarshaller.marshalTypeToOutputStream(rId, out);
                } else {
                    throw new InvalidRequest("1000", "Operation must be create or update.");
                }
                IOUtils.closeQuietly(out);
                long end = System.currentTimeMillis();
                logMetacat.info(Settings.PERFORMANCELOG + pid.getValue()
                        + Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD
                        + " Total create/update method" + Settings.PERFORMANCELOG_DURATION
                            + (end-start)/1000);
        } catch (Exception e) {
            if(objFile != null) {
                StreamingMultipartRequestResolver.deleteTempFile(objFile);
            }
            throw e;
        }
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
    private void deleteObject(String pid) throws IOException, InvalidToken, ServiceFailure,
                    NotAuthorized, NotFound, NotImplemented, InvalidRequest, MarshallingException {
        long start = System.currentTimeMillis();
        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        Identifier id = new Identifier();
        id.setValue(pid);

        logMetacat.debug("Calling delete");
        MNodeService.getInstance(request).delete(session, id);
        TypeMarshaller.marshalTypeToOutputStream(id, out);
        long end = System.currentTimeMillis();
        IOUtils.closeQuietly(out);
        logMetacat.info(Settings.PERFORMANCELOG + pid + Settings.PERFORMANCELOG_DELETE_METHOD
                + " Total delete method" + Settings.PERFORMANCELOG_DURATION + (end-start)/1000);

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
     */
    private void archive(String pid) throws InvalidToken, ServiceFailure, NotAuthorized,
                                    NotFound, NotImplemented, IOException, MarshallingException {
        long start = System.currentTimeMillis();
        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        Identifier id = new Identifier();
        id.setValue(pid);

        logMetacat.debug("Calling archive");
        MNodeService.getInstance(request).archive(session, id);

        TypeMarshaller.marshalTypeToOutputStream(id, out);
        IOUtils.closeQuietly(out);
        long end = System.currentTimeMillis();
        logMetacat.info(Settings.PERFORMANCELOG + pid + Settings.PERFORMANCELOG_ARCHIVE_METHOD
                 + " Total archive method" + Settings.PERFORMANCELOG_DURATION + (end-start)/1000);
    }

    protected SynchronizationFailed collectSynchronizationFailed() throws IOException,
                       ServiceFailure, InvalidRequest, MarshallingException,
                       InstantiationException, IllegalAccessException,
                       ParserConfigurationException, SAXException {

        // Read the incoming data from its Mime Multipart encoding
        logMetacat.debug("Disassembling MIME multipart form");
        InputStream sf = null;

        // handle MMP inputs
        File tmpDir = getTempDirectory();
        logMetacat.debug("temp dir: " + tmpDir.getAbsolutePath());
        MultipartRequestResolver mrr =
            new MultipartRequestResolver(tmpDir.getAbsolutePath(), MAX_UPLOAD_SIZE, 0);
        MultipartRequest mr = null;
        try {
            mr = mrr.resolveMultipart(request);
        } catch (Exception e) {
            throw new ServiceFailure("2161",
                    "Could not resolve multipart: " + e.getMessage());
        }
        logMetacat.debug("resolved multipart request");
        Map<String, File> files = mr.getMultipartFiles();
        if (files == null || files.keySet() == null) {
            throw new InvalidRequest("2163",
                    "must have multipart file with name 'message'");
        }
        logMetacat.debug("got multipart files");

        multipartparams = mr.getMultipartParameters();

        File sfFile = files.get("message");
        if (sfFile == null) {
            throw new InvalidRequest("2163",
                    "Missing the required file-part 'message' from the multipart request.");
        }
        logMetacat.debug("sfFile: " + sfFile.getAbsolutePath());
        sf = new FileInputStream(sfFile);

        SynchronizationFailed syncFailed = (SynchronizationFailed) ExceptionHandler
                                            .deserializeXml(sf, "Error deserializing exception");
        return syncFailed;
    }

    /**
     * Update the system metadata for a specified identifier
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IOException
     * @throws MarshallingException
     * @throws NotImplemented
     * @throws NotAuthorized
     * @throws InvalidSystemMetadata
     * @throws InvalidToken
     */
    protected void updateSystemMetadata() throws ServiceFailure, InvalidRequest,
                                    InstantiationException, IllegalAccessException,
                                    IOException, MarshallingException, NotImplemented,
                                        NotAuthorized, InvalidSystemMetadata, InvalidToken {
        // Read the incoming data from its Mime Multipart encoding
        Map<String, File> files = collectMultipartFiles();

        // get the encoded pid string from the body and make the object
        String pidString = multipartparams.get("pid").get(0);
        Identifier pid = new Identifier();
        pid.setValue(pidString);

        logMetacat.debug("updateSystemMetadata: " + pid);

        // get the system metadata from the request
        File smFile = files.get("sysmeta");
        FileInputStream sysmeta = new FileInputStream(smFile);
        SystemMetadata systemMetadata = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmeta);

        logMetacat.debug("updating system metadata with pid " + pid.getValue());

        MNodeService.getInstance(request).updateSystemMetadata(session, pid, systemMetadata);
    }

    /**
     * Handle the reindex request
     * @param pid  the pid which will be indexed. It can be null, which means we will do a batch
     *             reindex based on the query part
     * @throws InvalidRequest
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws IOException
     */
    protected void reindex(String pid) throws InvalidRequest, ServiceFailure,
                                                     NotAuthorized, NotImplemented, IOException {
        boolean all = false;
        List<Identifier> identifiers = new ArrayList<Identifier>();
        if (pid != null) {
            //handle to reindex a single pid
            logMetacat.debug("MNResourceHandler.reindex - reindex one pid (part of url) " + pid);
            Identifier id = new Identifier();
            id.setValue(pid);
            identifiers.add(id);
            MNodeService.getInstance(request).reindex(session, identifiers);
        } else {
            //handle the batch of reindex tasks based on query
            logMetacat.debug("MNResourceHandler.reindex - reindex objects based on the query part");
            String[] allValueArray = params.get("all");
            if (allValueArray != null) {
                if (allValueArray.length != 1) {
                    throw new InvalidRequest("5903", "The \"all\" should only have one value");
                } else {
                    String allValue = allValueArray[0];
                    if (allValue != null && allValue.equalsIgnoreCase("true")) {
                        all = true;
                    }
                }
            }
            logMetacat.debug("MNResourceHandler.reindex - the \"all\" value is " + all);
            if (!all) {
                String[] ids = params.get("pid");
                if (ids != null) {
                    for (String id : ids) {
                        if (id != null && !id.trim().equals("")) {
                            Identifier identifier = new Identifier();
                            identifier.setValue(id);
                            identifiers.add(identifier);
                        }
                    }
                    MNodeService.getInstance(request).reindex(session, identifiers);
                } else {
                    throw new InvalidRequest("5903", "Users should specify the \"pid\" value "
                                                                                + "for reindexing");
                }
            } else {
                MNodeService.getInstance(request).reindexAll(session);
            }
        }
        response.setStatus(200);
        response.setContentType("text/xml");
        try (OutputStream out = response.getOutputStream()) {
            out.write(getSuccessScheduleText().getBytes());
        }
    }

    /**
     * Handle the request to update identifiers' (such as DOI) metadata on the third party service
     * @param pid  the pid which will be updated. It can be null, which means we will do a batch of
     *             update based on the query part.
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws NotImplemented
     * @throws IOException
     */
    protected void updateIdMetadata(String pid) throws ServiceFailure, NotAuthorized,
                                                      InvalidRequest, NotImplemented, IOException {
        boolean all = false;
        String[] identifiers = null;
        String[] formatIds = null;
        if (pid != null) {
            //handle to update a single pid's metadata
            logMetacat.debug("MNResourceHandler.updateIdMetadata - update one "
                                                             + "pid (part of url) " + pid);
            identifiers = new String[1];
            identifiers[0] = pid;
            MNodeService.getInstance(request)
                                                .updateIdMetadata(session, identifiers, formatIds);
        } else {
            //handle a batch of updating ids' metadata tasks based on query
            logMetacat.debug("MNResourceHandler.updateIdMetadata - updateIdMetadata "
                                                                    + "based on the query part");
            String[] allValueArray = params.get("all");
            if (allValueArray != null) {
                if (allValueArray.length != 1) {
                    throw new InvalidRequest("5908", "The \"all\" should only have one value");
                } else {
                    String allValue = allValueArray[0];
                    if (allValue != null && allValue.equalsIgnoreCase("true")) {
                        all = true;
                    }
                }
            }
            logMetacat.debug("MNResourceHandler.updateIdMetadata - the \"all\" value is " + all);
            if (!all) {
                identifiers = params.get("pid");
                formatIds = params.get("formatId");
                MNodeService.getInstance(request)
                                                 .updateIdMetadata(session, identifiers, formatIds);
            } else {
                MNodeService.getInstance(request).updateAllIdMetadata(session);
            }
        }
        response.setStatus(200);
        response.setContentType("text/xml");
        try (OutputStream out = response.getOutputStream()) {
            out.write(getSuccessScheduleText().getBytes());
        }
    }

    /**
     * Get the text of a successful scheduling.
     * @return the string of successful scheduling information
     */
    private String getSuccessScheduleText() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                    <scheduled>true</scheduled>
               """;
    }
}

