package edu.ucsb.nceas.metacat.restservice.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import edu.ucsb.nceas.metacat.systemmetadata.MCSystemMetadata;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.exceptions.MarshallingException;
import org.dataone.mimemultipart.MultipartRequest;
import org.dataone.mimemultipart.MultipartRequestResolver;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.storage.ObjectInfo;


/**
 * This class will stream the file parts of the multipart request into a temporary file.
 * During the streaming, the checksum of the tmp file will be calculated and maintained.
 * The temple file with the checksum can be moved to the permanent location
 * rather than copying (read/write). So we only need to read/write once and the
 * performance can be improved.
 * @author tao
 *
 */
public class StreamingMultipartRequestResolver extends MultipartRequestResolver {
    public static final String SYSMETA = "sysmeta";
    private static Log log = LogFactory.getLog(StreamingMultipartRequestResolver.class);
    private ServletFileUpload upload;
    private SystemMetadata sysMeta;
    private static boolean deleteOnExit =
                    Settings.getConfiguration().getBoolean("multipart.tempFile.deleteOnExit", false);

    /**
     * Constructor
     * @param tmpUploadDir  the directory will temporarily host the stored files from the file parts
     *                       in the http multiparts request.
     * @param maxUploadSize  the threshold size of files which can be allowed to upload
     */
    public StreamingMultipartRequestResolver(String tmpUploadDir, int maxUploadSize) {
        super(tmpUploadDir, maxUploadSize);
        // Create a new file upload handler
        this.upload = new ServletFileUpload();
        // Set overall request size constraint
        this.upload.setSizeMax(maxUploadSize);
    }

    @Override
    /**
     * This method parses the a http request and writes them into the temporary directory as checked files.
     * @param request  the request needs to be resolved
     * @return multipartRequest with the data structure including form fields and file items.
     * @throws InvalidRequest
     * @throws IOException
     * @throws FileUploadException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws MarshallingException
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     * @throws RuntimeException
     * @throws ServiceFailure
     * @throws InvalidSystemMetadata
     * @throws InvocationTargetException
     */
    public MultipartRequest resolveMultipart(HttpServletRequest request)
        throws IOException, FileUploadException, InstantiationException, IllegalAccessException,
        MarshallingException, NoSuchAlgorithmException, InvalidRequest, InvalidSystemMetadata,
        ServiceFailure, RuntimeException, InterruptedException, InvocationTargetException {
        Map<String, List<String>> mpParams = new HashMap<String, List<String>>();
        Map<String, File> mpFiles = new HashMap<String, File>();
        MultipartRequestWithSysmeta multipartRequest =
                                        new MultipartRequestWithSysmeta(request, mpFiles, mpParams);
        if (!isMultipartContent(request)) {
            return multipartRequest;
        }
        long start = 0;
        long end = 0;
        String pid = null;
        FileItemIterator iter = upload.getItemIterator(request);
        boolean sysmetaFirst = false;
        boolean objTaggedWithPid = false;
        ObjectInfo objectMetadata = null;
        int sysmetaIndex = 0;
        int objectIndex = 0;
        try {
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                try (InputStream stream = item.openStream()) {
                    if (item.isFormField()) {
                        //process form parts
                        String value = Streams.asString(stream);
                        log.debug("StreamingMultipartRequestResolver.resoloveMulitpart - form field "
                                    + name + " with value "+ value + " detected.");
                        if (mpParams.containsKey(name)) {
                            mpParams.get(name).add(value);
                        } else {
                            List<String> values = new ArrayList<String>();
                            values.add(value);
                            mpParams.put(name, values);
                        }
                    } else {
                        log.debug("StreamingMultipartRequestResolver.resoloveMulitpart -File field "
                                        + name + " with file name " + item.getName() + " detected.");
                        // Process the input stream
                        if (name.equals(SYSMETA)) {
                            if (sysmetaIndex >= 1) {
                                throw new InvalidRequest("0000", "MultipartRequest can only have "
                                                        + "one system metadata part.");
                            }
                            //copy the stream to a byte array output stream so we can read it multiple
                            //times. Since we don't know it is v1 or v2, we need to try two times.
                            byte[] sysmetaBytes;
                            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                                IOUtils.copy(stream, os);
                                sysmetaBytes = os.toByteArray();
                            }
                            try (ByteArrayInputStream input = new ByteArrayInputStream(sysmetaBytes)) {
                                try {
                                    org.dataone.service.types.v2.SystemMetadata sysMeta2 =
                                                TypeMarshaller.unmarshalTypeFromStream(
                                                  org.dataone.service.types.v2.SystemMetadata.class, input);
                                    sysMeta = sysMeta2;
                                } catch (Exception e) {
                                    //Transforming to the v2 systemmeta object failed. Try to transform to v1
                                    input.reset();
                                    sysMeta =
                                        TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, input);
                                    log.info("StreamingMultipartRequestResolver.resoloveMulitpart - "
                                             + "the system metadata is v1 for the pid "
                                             + sysMeta.getIdentifier().getValue());
                                }
                                checkSystemMetadata();
                                pid = sysMeta.getIdentifier().getValue();
                                if (objectMetadata != null) {
                                    //this means Metacat already stored the object before process sysmeta
                                    String checksum = sysMeta.getChecksum().getValue();
                                    String algorithm = sysMeta.getChecksum().getAlgorithm();
                                    long size = sysMeta.getSize().longValue();
                                    MetacatInitializer.getStorage()
                                        .deleteIfInvalidObject(objectMetadata, checksum, algorithm,
                                                               size);
                                    Identifier id = new Identifier();
                                    id.setValue(pid);
                                    // Hashstore will throw an exception if the id already is used.
                                    MetacatInitializer.getStorage()
                                                                .tagObject(id, objectMetadata.getCid());
                                    // attach the multiple checksums into the system metadata object
                                    MCSystemMetadata mcSysMeta = new MCSystemMetadata();
                                    MCSystemMetadata.copy(mcSysMeta, sysMeta);
                                    mcSysMeta.setChecksums(objectMetadata.getHexDigests());
                                    sysMeta = mcSysMeta;
                                    objTaggedWithPid = true;
                                }
                                multipartRequest.setSystemMetadata(sysMeta);
                                sysmetaIndex++;
                            }
                        } else if (name.equals("object")) {
                            start = System.currentTimeMillis();
                            if (objectIndex >= 1) {
                                throw new InvalidRequest("0000", "MultipartRequest can only have "
                                                        + "one object part.");
                            }
                            if (sysMeta != null) {
                                sysmetaFirst = true;
                                checkSystemMetadata();
                                //We are lucky and the system metadata has been processed.
                                long size = sysMeta.getSize().longValue();
                                log.info("StreamingMultipartRequestResolver.resoloveMulitpart - "
                                        + "Metacat is handling the object stream AFTER handling the "
                                        + "system metadata stream. StreamResolver will store the object"
                                        + " with identifier " + sysMeta.getIdentifier().getValue()
                                        + " , declared size " + size
                                        + "and calculating checksum using algorithm "
                                        + sysMeta.getChecksum().getAlgorithm());
                                // Note: please DO assign objectMetadata in this statement
                                // Hashstore will throw an exception if the id already is used.
                                objectMetadata = MNodeService
                                       .storeData(MetacatInitializer.getStorage(), stream, sysMeta);
                                // attach the multiple checksums into the system metadata object
                                MCSystemMetadata mcSysMeta = new MCSystemMetadata();
                                MCSystemMetadata.copy(mcSysMeta, sysMeta);
                                mcSysMeta.setChecksums(objectMetadata.getHexDigests());
                                sysMeta = mcSysMeta;
                                // The above storeObject method implicitly tagged the id with
                                // the cid. So we set objTaggedWithPid true.
                                objTaggedWithPid = true;
                            } else {
                                log.info("StreamingMultipartRequestResolver.resoloveMulitpart - Metacat "
                                          + "is handling the object stream before handling the system "
                                          + "metadata stream. StreamResolver can NOT calculate the "
                                          + "checksum since we don't know the algorithm.");
                                // Note: please DO assign objectMetadata in this statement
                                objectMetadata = MetacatInitializer.getStorage().storeObject(stream);
                            }
                            objectIndex++;
                            end = System.currentTimeMillis();
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (objectMetadata != null) {
                // The object was stored into HashStore successfully and we need to delete them
                try {
                    if (objTaggedWithPid) {
                        Identifier id = new Identifier();
                        id.setValue(pid);
                        MetacatInitializer.getStorage().deleteObject(id);
                    } else {
                        try {
                            Map<String, String> hexDigests = objectMetadata.getHexDigests();
                            Set<String> keys = hexDigests.keySet();
                            String algorithm = "SHA-256";
                            for (String key : keys) {
                                algorithm = key;
                                break;
                            }
                            String checksum = hexDigests.get(algorithm);
                            MetacatInitializer.getStorage()
                                .deleteIfInvalidObject(objectMetadata, checksum, algorithm,
                                                       objectMetadata.getSize() + 1);
                        } catch (InvalidSystemMetadata ee) {
                            log.info("Metacat purpose used a wrong size to trigger the deleting "
                                         + ee.getMessage());
                        }
                    }
                } catch (Exception ee) {
                    log.error("StreamingMultipartRequestResolver.resoloveMulitpart - failed to "
                             + "delete the object with pid " + pid + " or the object with cid "
                             + objectMetadata.getCid() + " since " + ee.getMessage());
                }
            }
            throw e;
        }

        if (end > start && pid != null) {
            String predicate = null;
            if (sysmetaFirst) {
                predicate = "with";
            } else {
                predicate = "without";
            }
            log.info(edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG
                    + pid
                    + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD
                    + " Write the object file from the http multipart to the disk "
                    + predicate
                    + " calculating the checksum"
                    + edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_DURATION
                    + (end-start)/1000);
        }
        return multipartRequest;
    }

    private void checkSystemMetadata() throws InvalidRequest {
        if(sysMeta == null) {
            throw new InvalidRequest("0000", "StreamingMultipartRequestResolver.checkSystemMetadata"
                                     + " - the system metadata object is null.");
        }
        if (sysMeta.getChecksum() == null || sysMeta.getChecksum().getValue().isBlank()
                || sysMeta.getChecksum().getAlgorithm() == null
                || sysMeta.getChecksum().getAlgorithm().isBlank()) {
                throw new InvalidRequest("0000",
                        "StreamingMultipartRequestResolver.checkSystemMetadata - "
                        + "The system metadata object should have both checksum and "
                        + " checksum algorithm values.");
        }
        if (sysMeta.getIdentifier() == null || sysMeta.getIdentifier().getValue().isBlank()) {
            throw new InvalidRequest("0000",
                    "StreamingMultipartRequestResolver.checkSystemMetadata - "
                    + "The system metadata object should have an id.");
        }
        BigInteger objSize = sysMeta.getSize();
        if (objSize == null || objSize.longValue() <= 0) {
            throw new InvalidRequest("0000",
                    "StreamingMultipartRequestResolver.checkSystemMetadata - "
                    + "The system metadata object should a valid size value.");
        }
    }



    /**
     * Get the system metadata object which was extracted from the sysmeta part.
     * The sysmeta wasn't stored in a file and was created an object directly.
     */
    public SystemMetadata getSystemMetadataPart() {
        return sysMeta;
    }

    /**
     * Delete a temp file either immediately or on program exists according to the configuration
     * @param temp  the file will be deleted
     */
    public static void deleteTempFile(File temp) { 
        if (temp != null) {
            try {
                if(deleteOnExit) {
                    temp.deleteOnExit();
                    log.debug("StreamingMultiPartHandler.deleteTempFile - marked the temp deleting on exit");
                } else {
                    temp.delete();
                    log.debug("StreamingMultiPartHandler.deleteTempFile - deleted the temp file immediately");
                }
            } catch (Exception e) {
                log.warn("StreamingMultiPartHandler.deleteTempFile - couldn't delete the temp file since "
                            + e.getMessage());
            }
        }
    }

}
