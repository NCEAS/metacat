/**
 *  '$RCSfile$'
 *  Copyright: 2021 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
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
package edu.ucsb.nceas.metacat.object.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;

import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;


/**
 * The abstract class to save and validate non-xml meta data objects
 * @author tao
 *
 */
public abstract class NonXMLMetadataHandler {
    private static Log logMetacat = LogFactory.getLog(NonXMLMetadataHandler.class);
    private static String metadataStoragePath = null;
    static {
        try {
            metadataStoragePath = PropertyService.getProperty("application.documentfilepath");
        } catch (PropertyNotFoundException e) {
           logMetacat.error("NonXMLMetadataHandler.static - cannot find the metadata object storage path since " + e.getMessage());
        }
    }
    
    /**
     * Save the bytes to the disk with localhost as the default notification server in the replication process
     * @param source  the input stream contains the content of the meta data object
     * @param docType  the doc type of this object in the xml_document table. Usually it is the format id.
     * @param pid  the identifier associated with the input stream
     * @param expectedChecksum  the expected checksum for the saved file. The value usually comes from the system meta data
     * @param session  the user's session who makes this call
     * @param ipAddress  the ip address of the client who makes the call (for the log information)
     * @param userAgent  the user agent of the client who makes the call (for the log information)
     * @return  the local document id. It can be null.
     * @throws UnsupportedType
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     * @throws NotAuthorized 
     */
    public String save(InputStream source, String docType, Identifier pid, Checksum expectedChecksum, 
                        Session session, String ipAddress, String userAgent) 
                        throws UnsupportedType, ServiceFailure, InvalidRequest, InvalidSystemMetadata, NotAuthorized {
        String replicationNotificationServer = "localhost";
        return save(source, docType, pid, expectedChecksum, replicationNotificationServer, session, ipAddress, userAgent);
    }
    
    /**
     * Save the bytes to the disk
     * @param source  the input stream contains the content of the meta data object
     * @param docType  the doc type of this object in the xml_document table. Usually it is the format id.
     * @param pid  the identifier associated with the input stream
     * @param expectedChecksum  the expected checksum for the saved file. The value usually comes from the system meta data
     * @param replicationNtofication server  the server name notifying the replication (only for the replication process)
     * @param session  the user's session who makes this call
     * @param ipAddress  the ip address of the client who makes the call (for the log information)
     * @param userAgent  the user agent of the client who makes the call (for the log information)
     * @return  the local document id. It can be null.
     * @throws UnsupportedType
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     * @throws NotAuthorized 
     */
    public String save(InputStream source, String docType, Identifier pid, Checksum expectedChecksum, 
                        String replicationNotificationServer, Session session, String ipAddress, String userAgent) 
                        throws UnsupportedType, ServiceFailure, InvalidRequest, InvalidSystemMetadata, NotAuthorized {
        if (pid == null || pid.getValue() == null || pid.getValue().trim().equals("")) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the pid parameter should not be blank.");
        }
        File tmpFile = null;
        boolean canReset = false;
        InputStream forValidationStream = null;
        if (source instanceof DetailedFileInputStream) {
            logMetacat.debug("NonXMLMetadataHandler.save - in the DetailedFileInputStream route for pid " + pid.getValue());
            DetailedFileInputStream input = (DetailedFileInputStream) source;
            File sourceFile = input.getFile();
            try {
                forValidationStream = new FileInputStream(sourceFile);
            } catch (FileNotFoundException e) {
                throw new ServiceFailure("1190", "NonXMLMetadataHandler.save - cannot valid the meta data object " + 
                        " because cannot find the file associated with the detailed file input stream " + 
                        " for the object " + pid.getValue() + " since " + e.getMessage());
            }
        } else if ( source.markSupported()) {
            logMetacat.debug("NonXMLMetadataHandler.save - in the resetable input stream route for pid " + pid.getValue());
            forValidationStream = source;
            canReset = true;
        } else {
            logMetacat.debug("NonXMLMetadataHandler.save - in the another type of the input stream route for pid " + pid.getValue());
            FileOutputStream out = null;
            try {
                tmpFile = generateTempFile("NonXML");
                out = new FileOutputStream(tmpFile);
                IOUtils.copyLarge(source, out);
                forValidationStream = new FileInputStream(tmpFile);
                source = new FileInputStream(tmpFile);
            } catch (IOException e) {
                if (tmpFile != null) {
                    tmpFile.delete();
                }
                throw new ServiceFailure("1190", "NonXMLMetadataHandler.save - cannot save the meta data object " + 
                                          pid.getValue() + " into a temporary file since " + e.getMessage());
            } finally {
                try {
                    IOUtils.close(out);
                } catch (IOException e) {
                    logMetacat.warn("NonXMLMetadataHandler.save - cannot close the out put stream after saving the object for pid " 
                                    + pid.getValue() + "into a temporary file");
                }
            }
            
        }
        
        String localId = null;
        boolean valid = false;
        try {
           valid = validate(forValidationStream);
           if (canReset) {
               try {
                   source.reset();
               } catch (IOException e) {
                   throw new ServiceFailure("1190", "NonXMLMetadataHandler.save - cannot save the object " + 
                           " because Metacat cannot reset the input stream even though the inputstream " + 
                           source.getClass().getCanonicalName() + " for the object " + pid.getValue() + 
                           " claim sit is resetable since " + e.getMessage());
               }
               
           }
        } catch (InvalidRequest e) {
            try {
                if (forValidationStream != null) {
                    forValidationStream.close();
                }
            } catch (IOException ee) {
                logMetacat.warn("NonXMLMetadataHandler.save - cannot close the invalidation stream since " + ee.getMessage());
            }
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the metadata object " + pid.getValue() + 
                                    " is invalid: " + e.getMessage());
        }
        
        if (!valid) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the metadata object " + pid.getValue() + " is invalid.");
        } else {
            try {
                if (metadataStoragePath == null) {
                    throw new ServiceFailure("1190", "NonXMLMetadataHandler.save - cannot save the metadata object " + pid.getValue() + 
                            " into disk since the property - application.documentfilepath is not found in the metacat.properties file ");
                }
                //Save the meta data object to disk using "localId" as the name
                localId = D1NodeService.insertObject(source, docType, pid, metadataStoragePath, session, expectedChecksum, ipAddress, 
                                                    userAgent, replicationNotificationServer);
            } finally {
                if (tmpFile != null) {
                    tmpFile.delete();
                }
                try {
                    source.close();
                    if (forValidationStream != null) {
                        forValidationStream.close();
                    }
                } catch (IOException ee) {
                    logMetacat.warn("NonXMLMetadataHandler.save - cannot close the invalidation stream since " + ee.getMessage());
                }
                try {
                    source.close();
                } catch (IOException ee) {
                    logMetacat.warn("NonXMLMetadataHandler.save - cannot close the source stream since " + ee.getMessage());
                }
            }
        }
        return localId;
    }
    
    /**
     *The abstract method to validate the non-xml object 
     * @param source  the input stream contains the content of the meta data object
     * @return true if the content is valid; false otherwise.
     * @throws InvalidRequest  when the content is not valid
     */
    public abstract boolean validate(InputStream source) throws InvalidRequest;
    
    
    /**
     * Create a temporary file for the given prefix
     * @param prefix  the prefix of the temporary file
     * @return  the created file
     * @throws IOException
     */
    private static File generateTempFile(String prefix) throws IOException {
        File tmpDir = null;
        try {
            tmpDir = new File(PropertyService.getProperty("application.tempDir"));
        }
        catch(PropertyNotFoundException pnfe) {
            logMetacat.error("NonXMLMetadataHandler.generateTempFile: " +
                    "application.tmpDir not found.  Using /tmp instead.");
            tmpDir = new File("/tmp");
        }
        String newPrefix = prefix + "-" + System.currentTimeMillis();
        String suffix =  null;
        File newFile = null;
        try {
            newFile = File.createTempFile(newPrefix, suffix, tmpDir);
        } catch (Exception e) {
            //try again if the first time fails
            newFile = File.createTempFile(newPrefix, suffix, tmpDir);
        }
        logMetacat.debug("StreamingMultiplePartRequestResolver.generateTmepFile - the new file  is " + newFile.getCanonicalPath());
        return newFile;
    }
}
