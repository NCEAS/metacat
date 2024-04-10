package edu.ucsb.nceas.metacat.restservice.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;


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
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;


/**
 * This class will stream the file parts of the multipart request into a temporary file. 
 * During the streaming, the checksum of the tmp file will be calculated and maintained. 
 * The temple file with the checksum can be moved to the permanent location 
 * rather than copying (read/write). So we only need to read/write once and the performance can be improved.
 * @author tao
 *
 */
public class StreamingMultipartRequestResolver extends MultipartRequestResolver {
    
    public static final String SYSMETA = "sysmeta";
    private static Log log = LogFactory.getLog(StreamingMultipartRequestResolver.class);
    private ServletFileUpload upload;
    private SystemMetadata sysMeta = null;
    private String defaultAlgorithm = Settings.getConfiguration().getString("multipartresolver.checksum.algorithm.default", "MD5");
    private File tempDir = null;
    private static boolean deleteOnExit = Settings.getConfiguration().getBoolean("multipart.tempFile.deleteOnExit", false);
    
    /**
     * Constructor
     * @param tmpUploadDir  the directory will temporarily host the stored files from the file parts in the http multiparts request.
     * @param maxUploadSize  the threshold size of files which can be allowed to upload
     */
    public StreamingMultipartRequestResolver(String tmpUploadDir, int maxUploadSize) {
        super(tmpUploadDir, maxUploadSize);
        tempDir = new File(tmpUploadDir);
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
     */
    public MultipartRequest resolveMultipart(HttpServletRequest request) throws IOException, FileUploadException, InstantiationException, IllegalAccessException, MarshallingException, NoSuchAlgorithmException {
        Map<String, List<String>> mpParams = new HashMap<String, List<String>>();
        Map<String, File> mpFiles = new HashMap<String, File>();
        MultipartRequestWithSysmeta multipartRequest = new MultipartRequestWithSysmeta(request, mpFiles, mpParams);
        if (!isMultipartContent(request)) {
            return multipartRequest;
        }
        long start = 0;
        long end = 0;
        String pid = null;
        FileItemIterator iter = upload.getItemIterator(request);
        boolean sysmetaFirst = false;
        while (iter.hasNext()) {
            FileItemStream item = iter.next();
            String name = item.getFieldName();
            InputStream stream = item.openStream();
            try {
                if (item.isFormField()) {
                    //process form parts
                    String value = Streams.asString(stream);
                    log.debug("StreamingMultipartRequestResolver.resoloveMulitpart - form field " + name + " with value "+ value + " detected.");
                    if (mpParams.containsKey(name)) {
                        mpParams.get(name).add(value);
                    } else {
                        List<String> values = new ArrayList<String>();
                        values.add(value);
                        mpParams.put(name, values);
                    }
                } else {
                    log.debug("StreamingMultipartRequestResolver.resoloveMulitpart -File field " + name + " with file name " + item.getName() + " detected.");
                    // Process the input stream
                    if (name.equals(SYSMETA)) {
                        //copy the stream to a byte array output stream so we can read it multiple times. Since we don't know it is v1 or v2, we need to try two times.
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        IOUtils.copy(stream, os);
                        byte[] sysmetaBytes = os.toByteArray();
                        os.close();
                        ByteArrayInputStream input = new ByteArrayInputStream(sysmetaBytes);
                        try {
                            org.dataone.service.types.v2.SystemMetadata sysMeta2 = TypeMarshaller.unmarshalTypeFromStream(org.dataone.service.types.v2.SystemMetadata.class, input);
                            sysMeta = sysMeta2;
                        } catch (Exception e) {
                            //Transforming to the v2 systemmeta object failed. Try to transform to v1
                            input.reset();
                            sysMeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, input);
                            log.info("StreamingMultipartRequestResolver.resoloveMulitpart - the system metadata is v1 for the pid " + sysMeta.getIdentifier().getValue());
                        }
                        if (sysMeta != null && sysMeta.getIdentifier() != null ) {
                            pid = sysMeta.getIdentifier().getValue();
                        }
                        input.close();
                        multipartRequest.setSystemMetadata(sysMeta);
                    } else if (name.equals("object")){
                        start = System.currentTimeMillis();
                        if (sysMeta != null && sysMeta.getChecksum() != null && sysMeta.getChecksum().getAlgorithm() != null && !sysMeta.getChecksum().getAlgorithm().trim().equals("")) {
                            sysmetaFirst = true;
                            //We are lucky and the system metadata has been processed.
                            String algorithm = sysMeta.getChecksum().getAlgorithm();
                            log.info("StreamingMultipartRequestResolver.resoloveMulitpart - Metacat is handling the object stream AFTER handling the system metadata stream. StreamResolver will calculate the checksum using algorithm " + algorithm);
                            //decide the pid for debug purpose
                            if (sysMeta != null && sysMeta.getIdentifier() != null ) {
                                pid = sysMeta.getIdentifier().getValue();
                            }
                            if(pid == null || pid.trim().equals("")) {
                                pid = "UNKNOWN";
                            }
                            File newFile = generateTmpFile("checked-object");
                            CheckedFile checkedFile = writeStreamToCheckedFile(newFile,  stream, algorithm, pid);
                            mpFiles.put(name, checkedFile);
                        } else {
                            log.info("StreamingMultipartRequestResolver.resoloveMulitpart - Metacat is handling the object stream before handling the system metadata stream. StreamResolver can NOT calculate the checksum since we don't know the algorithm.");
                            File newFile = generateTmpFile("unchecked-object");
                            writeStreamToFile(newFile, stream);
                            Checksum checksum = null;//we don't have a checksum, so set it null.
                            CheckedFile checkedFile = new CheckedFile(newFile.getCanonicalPath(), checksum);
                            mpFiles.put(name, checkedFile);
                        }
                        end = System.currentTimeMillis();
                    } else {
                        File newFile = generateTmpFile("other");
                        writeStreamToFile(newFile, stream);
                        mpFiles.put(name, newFile);
                    }
                }
            } catch (Exception e) {
                //if an exception happened, we need to delete those temporary files
                Set<String> keys = mpFiles.keySet();
                for (String key : keys) {
                    File tempFile = mpFiles.get(key);
                    deleteTempFile(tempFile);
                    mpFiles.remove(key);
                }
                throw e;
            } finally {
                if(stream != null) {
                    try {
                        stream.close();
                    } catch (Exception e) {
                        log.warn("Couldn't close the stream since" + e.getMessage());
                    }
                }
            }
        }
        if (end > start && pid != null) {
            String predicate = null;
            if (sysmetaFirst) {
                predicate = "with";
            } else {
                predicate = "without";
            }
            log.info(edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG + 
                    pid + 
                    edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_CREATE_UPDATE_METHOD + 
                    " Write the object file from the http multipart to the disk " + 
                    predicate + 
                    " calculating the checksum" + 
                    edu.ucsb.nceas.metacat.common.Settings.PERFORMANCELOG_DURATION + 
                    (end-start)/1000);
        }
        return multipartRequest;
    }
    
    /**
     * Create a temporary new file
     * @return
     * @throws IOException
     */
    private File generateTmpFile(String prefix) throws IOException {
        String newPrefix = prefix + "-" + System.currentTimeMillis();
        String suffix =  null;
        File newFile = null;
        try {
            newFile = File.createTempFile(newPrefix, suffix, tempDir);
        } catch (Exception e) {
            //try again if the first time fails
            newFile = File.createTempFile(newPrefix, suffix, tempDir);
        }
        log.debug("StreamingMultiplePartRequestResolver.generateTmepFile - the new file  is " + newFile.getCanonicalPath());
        return newFile;
    }
    
    /**
     * Write the input stream into the given fileName and directory while calculate the checksum.
     * @param file  the file into which the stream will be written. It should exists already.
     * @param dataStream  the source stream
     * @param checksumAlgorithm  the algorithm will be used for calculating the checksum
     * @param pid  the pid of the object (only used for debug information)
     * @return  a CheckedFile object ( a File object with advertised checksum)
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static CheckedFile writeStreamToCheckedFile(File file, InputStream dataStream, String checksumAlgorithm, String pid) 
        throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        Checksum checksum = null;
        log.debug("StreamingMultipartRequestResolver.writeStreamToCheckedFile - filename for writting is: " + file.getAbsolutePath() + " for the pid " + pid + " by the algorithm " + checksumAlgorithm);
        MessageDigest md = MessageDigest.getInstance(checksumAlgorithm);
        // write data stream to desired file
        DigestOutputStream os = null;
        try {
            os = new DigestOutputStream(new FileOutputStream(file), md);
            long length = IOUtils.copyLarge(dataStream, os);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (Exception e) {
                    log.warn("StreamingMultipartRequestResolver.writeStreamToCheckedFile - couldn't close the file output stream since " + e.getMessage());
                }
            }
        }
        String localChecksum = DatatypeConverter.printHexBinary(md.digest());
        checksum = new Checksum();
        checksum.setAlgorithm(checksumAlgorithm);
        checksum.setValue(localChecksum);
        log.info("StreamingMultipartRequestResolver.writeStreamToCheckedFile - the checksum calculated from the saved local file is " + localChecksum + " for the pid " + pid);
        CheckedFile checkedFile = new CheckedFile(file.getCanonicalPath(), checksum);
        return checkedFile;
    }
    
    /**
     * Write the stream into a given file.
     * @param file
     * @param dataStream
     * @return
     * @throws IOException
     */
    private static File writeStreamToFile(File file, InputStream dataStream) throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            long length = IOUtils.copyLarge(dataStream, os);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (Exception e) {
                    log.warn("StreamingMultipartRequestResolver.writeStreamToFile - couldn't close the file output stream since " + e.getMessage());
                }
            }
        }
        return file;
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
                log.warn("StreamingMultiPartHandler.deleteTempFile - couldn't delete the temp file since " + e.getMessage());
            }
        }
    }

}
