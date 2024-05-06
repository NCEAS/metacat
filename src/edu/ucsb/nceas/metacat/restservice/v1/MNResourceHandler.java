/**
 *  '$RCSfile$'
 *  Copyright: 2011 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
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
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.util.Constants;
import org.dataone.service.util.DateTimeMarshaller;
import org.dataone.service.util.ExceptionHandler;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.speedbagit.SpeedBagIt;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeInputStream;
import edu.ucsb.nceas.metacat.dataone.D1AuthHelper;
import edu.ucsb.nceas.metacat.dataone.v1.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.D1ResourceHandler;
import edu.ucsb.nceas.metacat.restservice.multipart.CheckedFile;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.metacat.restservice.multipart.MultipartRequestWithSysmeta;
import edu.ucsb.nceas.metacat.restservice.multipart.StreamingMultipartRequestResolver;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.ReadOnlyChecker;


/**
 * MN REST service implementation handler
 *
 * ******************
 * MNCore -- DONE
 *         ping() - GET /d1/mn/monitor/ping
 *         log() - GET /d1/mn/log
 *         **getObjectStatistics() - GET /d1/mn/monitor/object
 *         getOperationsStatistics - GET /d1/mn/monitor/event
 *         **getStatus - GET /d1/mn/monitor/status
 *         getCapabilities() - GET /d1/mn/ and /d1/mn/node
 *
 *     MNRead -- DONE
 *         get() - GET /d1/mn/object/PID
 *         getSystemMetadata() - GET /d1/mn/meta/PID
 *         describe() - HEAD /d1/mn/object/PID
 *         getChecksum() - GET /d1/mn/checksum/PID
 *         listObjects() - GET /d1/mn/object
 *         synchronizationFailed() - POST /d1/mn/error
 *
 *     MNAuthorization -- DONE
 *         isAuthorized() - GET /d1/mn/isAuthorized/PID
 *         setAccessPolicy() - PUT /d1/mn/accessRules/PID
 *
 *     MNStorage - DONE
 *         create() - POST /d1/mn/object/PID
 *         update() - PUT /d1/mn/object/PID
 *         delete() - DELETE /d1/mn/object/PID
 *         archive() - PUT /d1/mn/archive/PID

 *    systemMetadataChanged() - POST /dirtySystemMetadata/PID
 *
 *     MNReplication
 *         replicate() - POST /d1/mn/replicate
 *    getReplica() - GET /d1/mn/replica
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
    protected static final String RESOURCE_VIEWS = "views";
    protected static final String RESOURCE_TOKEN = "token";



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

            logMetacat.info("MNResourceHanlder.handle - V1 handling verb " + httpVerb + " request with resource '" + resource + "'");
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
                        // after the command
                        extra = parseTrailing(resource, RESOURCE_META);
                        extra = decode(extra);
                        getSystemMetadataObject(extra);
                        status = true;
                    }

                } else if (resource.startsWith(RESOURCE_OBJECTS)) {
                    logMetacat.debug("Using resource 'object'");
                    // after the command
                    extra = parseTrailing(resource, RESOURCE_OBJECTS);
                    logMetacat.debug("objectId(before decoded: " + extra);
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
                    }
                } else if (resource.startsWith(RESOURCE_GENERATE_ID)) {
                    // generate an id
                    if (httpVerb == POST) {
                        generateIdentifier();
                        status = true;
                    }
                } else if (resource.startsWith(RESOURCE_PUBLISH)) {
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
                    // get
                    if (httpVerb == GET) {
                        // after the command
                        extra = parseTrailing(resource, RESOURCE_PACKAGE);
                        extra = decode(extra);
                        getPackage(extra);
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

    private void doQuery(String engine, String query) {

        OutputStream out = null;

        try {
            // NOTE: we set the session explicitly for the MNode instance since these methods do not provide a parameter
            if (engine == null) {
                // just looking for list of engines
                MNodeService mnode = MNodeService.getInstance(request);
                mnode.setSession(session);
                QueryEngineList qel = mnode.listQueryEngines();
                response.setContentType("text/xml");
                response.setStatus(200);
                out = response.getOutputStream();
                TypeMarshaller.marshalTypeToOutputStream(qel, out);
                return;
            } else {
                if (query != null) {
                    MNodeService mnode = MNodeService.getInstance(request);
                    mnode.setSession(session);
                    InputStream stream = mnode.query(engine, query);

                    // set the content-type if we have it from the implementation
                    if (stream instanceof ContentTypeInputStream) {
                        //response.setContentType("application/octet-stream");
                        //response.setContentType("text/xml");
                        response.setContentType(((ContentTypeInputStream) stream).getContentType());
                    }
                    response.setStatus(200);
                    out = response.getOutputStream();
                    // write the results to the output stream
                    IOUtils.copyLarge(stream, out);
                    return;
                } else {
                    MNodeService mnode = MNodeService.getInstance(request);
                    mnode.setSession(session);
                    QueryEngineDescription qed = mnode.getQueryEngineDescription(engine);
                    response.setContentType("text/xml");
                    response.setStatus(200);
                    out = response.getOutputStream();
                    TypeMarshaller.marshalTypeToOutputStream(qed, out);
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

    private void doViews(String format, String pid) {

        OutputStream out = null;
        MNodeService mnode = MNodeService.getInstance(request);

        try {
            // get a list of views
            if (pid != null) {
                Identifier identifier = new Identifier();
                identifier.setValue(pid);
                InputStream stream = mnode.view(session, format, identifier);

                // set the content-type if we have it from the implementation
                if (stream instanceof ContentTypeInputStream) {
                    response.setContentType(((ContentTypeInputStream) stream).getContentType());
                }
                response.setStatus(200);
                out = response.getOutputStream();
                // write the results to the output stream
                IOUtils.copyLarge(stream, out);
                return;
            } else {
                // TODO: list the registered views
                BaseException ni = new NotImplemented("9999", "MN.listViews() is not implemented at this node");
                throw ni;
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
        long serialVersion = 0L;
        String serialVersionStr = null;
        Date dateSysMetaLastModified = null;
        String dateSysMetaLastModifiedStr = null;
        Identifier pid = null;

        // mkae sure we have the multipart params
        try {
            initMultipartParams();
        } catch (Exception e1) {
            throw new ServiceFailure("1333", "Could not collect the multipart params for the request");
        }

        // get the pid
        try {
            String id = multipartparams.get("pid").get(0);
            pid = new Identifier();
            pid.setValue(id);
        } catch (NullPointerException e) {
            String msg = "The 'pid' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("1334", msg);
        }

        // get the serialVersion
        try {
            serialVersionStr = multipartparams.get("serialVersion").get(0);
            serialVersion = new Long(serialVersionStr).longValue();

        } catch (NullPointerException e) {
            String msg = "The 'serialVersion' must be provided as a parameter and was not.";
            logMetacat.error(msg);
            throw new InvalidRequest("1334", msg);

        }

        // get the dateSysMetaLastModified
        try {
            dateSysMetaLastModifiedStr = multipartparams.get("dateSysMetaLastModified").get(0);
            dateSysMetaLastModified = DateTimeMarshaller.deserializeDateToUTC(dateSysMetaLastModifiedStr);

        } catch (NullPointerException e) {
            String msg = 
                "The 'dateSysMetaLastModified' must be provided as a " + 
                "parameter and was not, or was an invalid representation of the timestamp.";
            logMetacat.error(msg);
            throw new InvalidRequest("1334", msg);
            
        }

        // call the service
        MNodeService.getInstance(request).systemMetadataChanged(session, pid, serialVersion, dateSysMetaLastModified);
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
    private void generateIdentifier() throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest, IOException, MarshallingException {

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
    private boolean isAuthorized(String id) throws ServiceFailure, InvalidToken, NotFound, NotAuthorized, NotImplemented, InvalidRequest {
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
     * @throws InvalidToken
     */
    private void syncError() throws NotImplemented, ServiceFailure, NotAuthorized, InvalidRequest, MarshallingException, IOException, InstantiationException, IllegalAccessException, InvalidToken {
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
    private void checksum(String pid) throws NotImplemented, MarshallingException, IOException, InvalidToken, ServiceFailure, NotAuthorized, NotFound, InvalidRequest {
        String checksumAlgorithm = "MD5";
        try {
            checksumAlgorithm = PropertyService.getProperty("dataone.checksumAlgorithm.default");
        } catch(Exception e) {
            logMetacat.warn("Could not lookup configured default checksum algorithm, using: " + checksumAlgorithm);
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
            D1AuthHelper authDel = new D1AuthHelper(request, null, "2152", "????");
            authDel.doAdminAuthorization(session);
//            allowed = MNodeService.getInstance(request).isAdminAuthorized(session);
//            if (!allowed) {
//                String msg = "User is not an admin user";
//                NotAuthorized failure = new NotAuthorized("2152", msg);
//                throw failure;
//            }
        }

        // parse the systemMetadata
        Map<String, File> files = collectMultipartFiles();        
        final SystemMetadata sysmeta = TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, files.get("sysmeta"));

        String sn = multipartparams.get("sourceNode").get(0);
        logMetacat.debug("sourceNode: " + sn);
        final NodeReference sourceNode = new NodeReference();
        sourceNode.setValue(sn);

        // run it in a thread to avoid connection timeout
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                    MNodeService.getInstance(request).replicate(session, sysmeta, sourceNode);
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
     * @throws InsufficientResources
     */
    private void getReplica(String id)
        throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented,
        ServiceFailure, NotFound, InsufficientResources {

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
    private void describeObject(String pid) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented, InvalidRequest
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
        response.addHeader("DataONE-Checksum", dr.getDataONE_Checksum().getAlgorithm() + "," + dr.getDataONE_Checksum().getValue());
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
    private void getLog() throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented, IOException, MarshallingException
    {

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
            pidFilter = params.get("pidFilter")[0];
        } catch (Exception e) {
            logMetacat.warn("Could not parse pidFilter: " + e.getMessage());
        }

        logMetacat.debug("calling getLogRecords");
        Log log = MNodeService.getInstance(request).getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);

        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        TypeMarshaller.marshalTypeToOutputStream(log, out);

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
     * @throws InsufficientResources
     */
    protected void getObject(String pid) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, InvalidRequest, NotImplemented, IOException, MarshallingException, InsufficientResources {
        OutputStream out = null;

        if (pid != null) { //get a specific document
            Identifier id = new Identifier();
            id.setValue(pid);
            InputStream data = MNodeService.getInstance(request).get(session, id);
            SystemMetadata sm = MNodeService.getInstance(request).getSystemMetadata(session, id);

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
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename+"\"");
            out = response.getOutputStream();  
            IOUtils.copyLarge(data, out);

        }
        else
        { //call listObjects with specified params
            Date startTime = null;
            Date endTime = null;
            ObjectFormatIdentifier formatId = null;
            boolean replicaStatus = true;
            int start = 0;
            //TODO: make the max count into a const
            int count = 1000;
            Enumeration paramlist = request.getParameterNames();
            while (paramlist.hasMoreElements()) 
            { //parse the params and make the crud call
                String name = (String) paramlist.nextElement();
                String[] value = (String[])request.getParameterValues(name);

                if (name.equals("fromDate") && value != null)
                {
                    try
                    {
                      //startTime = dateFormat.parse(value[0]);
                        startTime = DateTimeMarshaller.deserializeDateToUTC(value[0]);
                        //startTime = parseDateAndConvertToGMT(value[0]);
                    }
                    catch(Exception e)
                    {  //if we can't parse it, just don't use the fromDate param
                        logMetacat.warn("Could not parse fromDate: " + value[0], e);
                        throw new InvalidRequest("1540", "Could not parse fromDate: " + value[0]+" since "+e.getMessage());
                        //startTime = null;
                    }
                }
                else if(name.equals("toDate") && value != null)
                {
                    try
                    {
                        endTime = DateTimeMarshaller.deserializeDateToUTC(value[0]);
                    }
                    catch(Exception e)
                    {  //if we can't parse it, just don't use the toDate param
                        logMetacat.warn("Could not parse toDate: " + value[0], e);
                        throw new InvalidRequest("1540", "Could not parse toDate: " + value[0]+" since "+e.getMessage());
                        //endTime = null;
                    }
                }
                else if(name.equals("formatId") && value != null)
                {
                    formatId = new ObjectFormatIdentifier();
                    formatId.setValue(value[0]);
                }
                else if(name.equals("replicaStatus") && value != null)
                {
                    if(value != null &&
                       value.length > 0 &&
                       (value[0].equalsIgnoreCase("false") || value[0].equalsIgnoreCase("no")))
                    {
                        replicaStatus = false;
                    }
                }
                else if(name.equals("start") && value != null)
                {
                    start = new Integer(value[0]).intValue();
                }
                else if(name.equals("count") && value != null)
                {
                    count = new Integer(value[0]).intValue();
                }
            }
            //make the crud call
            logMetacat.debug("session: " + session + " startTime: " + startTime +
                    " endTime: " + endTime + " formatId: " + 
                    formatId + " replicaStatus: " + replicaStatus + 
                    " start: " + start + " count: " + count);

            ObjectList ol =
                MNodeService.getInstance(request).listObjects(session, startTime, endTime,
                       formatId, replicaStatus, start, count);

            out = response.getOutputStream();
            response.setStatus(200);
            response.setContentType("text/xml");
            // Serialize and write it to the output stream
            TypeMarshaller.marshalTypeToOutputStream(ol, out);

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
    protected void getPackage(String pid) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented, IOException, InvalidRequest {

        Identifier id = new Identifier();
        id.setValue(pid);
        InputStream is = MNodeService.getInstance(request).getPackage(session, null, id);

        //Use the pid as the file name prefix, replacing all non-word characters
        String filename = pid.replaceAll("\\W", "_") + ".zip";

        response.setHeader("Content-Disposition", "inline; filename=\"" + filename+"\"");
        response.setContentType("application/zip");
        response.setStatus(200);
        OutputStream out = response.getOutputStream();

        // write it to the output stream
        IOUtils.copyLarge(is, out);
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
    protected void getSystemMetadataObject(String pid) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, InvalidRequest, NotImplemented, IOException, MarshallingException {

        Identifier id = new Identifier();
        id.setValue(pid);
        SystemMetadata sysmeta = MNodeService.getInstance(request).getSystemMetadata(session, id);

        response.setContentType("text/xml");
        response.setStatus(200);
        OutputStream out = response.getOutputStream();
        
        // Serialize and write it to the output stream
        TypeMarshaller.marshalTypeToOutputStream(sysmeta, out);
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
    protected void putObject(String trailingPid, String action) throws ServiceFailure, InvalidRequest, MarshallingException, InvalidToken, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, NotFound, IOException, InstantiationException, IllegalAccessException, NoSuchAlgorithmException, FileUploadException {
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

                Identifier pid = new Identifier();
                if (trailingPid == null) {
                    // get the pid string from the body and set the value
                    String pidString = multipartparams.get("pid").get(0);
                    if (pidString != null) {
                    pid.setValue(pidString);
                    
                  } else {
                      throw new InvalidRequest("1102", "The pid param must be included and contain the identifier.");

                  }
                } else {
                    // use the pid included in the URL
                    pid.setValue(trailingPid);
                }
                logMetacat.debug("putObject with pid " + pid.getValue());
                logMetacat.debug("Entering putObject: " + pid.getValue() + "/" + action);

                SystemMetadata smd = multiparts.getSystemMetadata();
                // ensure we have the system metadata
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
                    
                    Identifier rId = MNodeService.getInstance(request).update(session, pid, object, newPid, smd);
                    TypeMarshaller.marshalTypeToOutputStream(rId, out);
                } else {
                    throw new InvalidRequest("1000", "Operation must be create or update.");
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

        logMetacat.debug("Calling delete");
        MNodeService.getInstance(request).delete(session, id);
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
     */
    private void archive(String pid) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented, IOException, MarshallingException {

        OutputStream out = response.getOutputStream();
        response.setStatus(200);
        response.setContentType("text/xml");

        Identifier id = new Identifier();
        id.setValue(pid);

        logMetacat.debug("Calling archive");
        MNodeService.getInstance(request).archive(session, id);
        
        TypeMarshaller.marshalTypeToOutputStream(id, out);
        
    }

    protected SynchronizationFailed collectSynchronizationFailed() throws IOException, ServiceFailure, InvalidRequest, MarshallingException, InstantiationException, IllegalAccessException, ParserConfigurationException, SAXException  {

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

        SynchronizationFailed syncFailed = (SynchronizationFailed) ExceptionHandler.deserializeXml(sf, "Error deserializing exception");
        return syncFailed;
    }

}

