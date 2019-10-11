/**
 *  '$RCSfile$'
 *  Copyright: 2019 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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
    public MultipartRequest resolveMultipart(HttpServletRequest request) throws IOException, FileUploadException, Exception {
        Map<String, List<String>> mpParams = new HashMap<String, List<String>>();
        Map<String, File> mpFiles = new HashMap<String, File>();
        MultipartRequest multipartRequest = new MultipartRequest(request, mpFiles, mpParams);
        if (!isMultipartContent(request)) {
            return multipartRequest;
        }
        FileItemIterator iter = upload.getItemIterator(request);
        while (iter.hasNext()) {
            FileItemStream item = iter.next();
            String name = item.getFieldName();
            InputStream stream = item.openStream();
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
                    }
                    input.close();
                } else {
                    String algorithm = null;
                    if (sysMeta != null && sysMeta.getChecksum() != null ) {
                        //We are lucky and the system metadata has been processed
                        algorithm = sysMeta.getChecksum().getAlgorithm();
                    }
                    //if we haven't handle the system metadata part, we use the default algorithm.
                    if(algorithm == null || algorithm.trim().equals("")) {
                        log.info("StreamingMultipartRequestResolver.resoloveMulitpart - Metacat is handling the object stream before handling the system metadata stream. So we have to calculate the checksum by the default calgorithm " + defaultAlgorithm);
                        algorithm = defaultAlgorithm;
                    }
                    //decide the pid for debug purpose
                    String pid = null;
                    if (sysMeta != null && sysMeta.getIdentifier() != null ) {
                        pid = sysMeta.getIdentifier().getValue();
                    }
                    if(pid == null || pid.trim().equals("")) {
                        pid = "UNKNOWN";
                    }
                    File newFile = generatedTmpFile("upload");
                    CheckedFile checkedFile = writeStreamToCheckedFile(newFile,  stream, algorithm, pid);
                    mpFiles.put(name, checkedFile);
                }
            }
            if(stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    log.warn("Couldn't close the stream since" + e.getMessage());
                }
            }
        }
        return multipartRequest;
    }
    
    /**
     * Create a temporary new file
     * @return
     * @throws IOException
     */
    private File generatedTmpFile(String prefix) throws IOException {
        String newPrefix = prefix + "-" + System.currentTimeMillis();
        String suffix =  null;
        File newFile = null;
        try {
            newFile = File.createTempFile(newPrefix, suffix, tempDir);
        } catch (Exception e) {
            //try again if the first time fails
            newFile = File.createTempFile(newPrefix, suffix, tempDir);
        }
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
        DigestOutputStream os = new DigestOutputStream( new FileOutputStream(file), md);
        long length = IOUtils.copyLarge(dataStream, os);
        os.flush();
        os.close();
        String localChecksum = DatatypeConverter.printHexBinary(md.digest());
        checksum = new Checksum();
        checksum.setAlgorithm(checksumAlgorithm);
        checksum.setValue(localChecksum);
        log.info("StreamingMultipartRequestResolver.writeStreamToCheckedFile - the checksum calculated from the saved local file is " + localChecksum + " for the pid " + pid);
        CheckedFile checkedFile = new CheckedFile(file.getCanonicalPath(), checksum);
        return checkedFile;
    }
    
    /**
     * Get the system metadata object which was extracted from the sysmeta part.
     * The sysmeta wasn't stored in a file and was created an object directly.
     */
    public SystemMetadata getSystemMetadataPart() {
        return sysMeta;
    }

}
