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

import java.io.ByteArrayInputStream;
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
import org.dataone.service.types.v1.SystemMetadata;

import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.EventLogData;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.replication.ForceReplicationHandler;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
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
    private File tmpFile = null;
    static {
        try {
            metadataStoragePath = PropertyService.getProperty("application.documentfilepath");
        } catch (PropertyNotFoundException e) {
           logMetacat.error("NonXMLMetadataHandler.static - cannot find the metadata object storage path since " + e.getMessage());
        }
    }
    
    /**
     * Save the bytes to the disk 
     * @param source  the input stream contains the content of the meta data object
     * @param sysmeta  the sysmeta associated with the input stream
     * @param session  the user's session who makes this call
     * @param event  the event log information associated with this action
     * @return  the local document id. It can be null.
     * @throws UnsupportedType
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     * @throws NotAuthorized 
     */
    public String save(InputStream source, SystemMetadata sysmeta, Session session, EventLogData event) 
                        throws UnsupportedType, ServiceFailure, InvalidRequest, InvalidSystemMetadata, NotAuthorized {
        if (sysmeta == null) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the system metadata parameter should not be null.");
        }
        Identifier pid = sysmeta.getIdentifier();
        Checksum expectedChecksum = sysmeta.getChecksum();
        String docType = sysmeta.getFormatId().getValue();
        if (pid == null || pid.getValue() == null || pid.getValue().trim().equals("")) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the pid parameter should not be blank.");
        }
        logMetacat.debug("NonXMLMetadataHandler.save - save the object " + pid.getValue() + " with doctype " + docType);
        String localId = null;
        boolean valid = false;
        InputStream data = checkValidation(source, pid);
        try {
            if (metadataStoragePath == null) {
                throw new ServiceFailure("1190", "NonXMLMetadataHandler.save - cannot save the metadata object " + pid.getValue() + 
                        " into disk since the property - application.documentfilepath is not found in the metacat.properties file ");
            }
            //Save the meta data object to disk using "localId" as the name
            localId = D1NodeService.insertObject(data, docType, pid, metadataStoragePath, session, expectedChecksum, event);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
            try {
                source.close();
            } catch (IOException ee) {
                logMetacat.warn("NonXMLMetadataHandler.save - cannot close the source stream since " + ee.getMessage());
            }
            try {
                data.close();
            } catch (IOException ee) {
                logMetacat.warn("NonXMLMetadataHandler.save - cannot close the source stream since " + ee.getMessage());
            }
        }
        return localId;
    }
    
    
    /**
     * Save the bytes to the disk from a replication process
     * @param source  the input stream contains the content of the meta data object
     * @param sysmeta  the system meta data associated with the source
     * @param replicationNtofication server  the server name notifying the replication (only for the replication process)
     * @param event  the event log information associated with this action
     * @return  the local document id. It can be null.
     * @throws UnsupportedType
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     * @throws NotAuthorized 
     */
    public void saveReplication(InputStream source, String localId, SystemMetadata sysmeta, String owner, int serverCode, 
                                 String replicationNotificationServer, EventLogData event) 
                        throws UnsupportedType, ServiceFailure, InvalidRequest, InvalidSystemMetadata, NotAuthorized {
        if (sysmeta == null) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.saveReplication - the system metadata parameter should not be null.");
        }
        Identifier pid = sysmeta.getIdentifier();
        Checksum expectedChecksum = sysmeta.getChecksum();
        String docType = sysmeta.getFormatId().getValue();
        if (pid == null || pid.getValue() == null || pid.getValue().trim().equals("")) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.saveReplication - the pid parameter should not be blank.");
        }
        logMetacat.debug("NonXMLMetadataHandler.saveReplication - save the object " + pid.getValue() + " with doctype " + docType);
        InputStream data = checkValidation(source, pid);
        try {
            if (metadataStoragePath == null) {
                throw new ServiceFailure("1190", "NonXMLMetadataHandler.saveReplication - cannot save the metadata object " + pid.getValue() + 
                        " into disk since the property - application.documentfilepath is not found in the metacat.properties file ");
            }
            //Save the meta data object to disk using "localId" as the name
            D1NodeService.writeStreamToFile(new File(metadataStoragePath), localId, source, expectedChecksum, pid);
            
            //register the local id into the xml_table
            try {
                DocumentImpl.registerDocument(localId, docType, localId, owner, null, serverCode);
            } catch (Exception e) {
                boolean isMetadata = true;
                try {
                    DocumentImpl.deleteFromFileSystem(localId, isMetadata);
                } catch (Exception ee) {
                    logMetacat.error("NonXMLMetadataHandler.saveReplication - In the exception route, Metacat cannot delete " 
                                      + localId + " in order to undo the transaction since " + ee.getMessage());
                }
                throw new ServiceFailure("1190", "NonXMLMetadataHandler.saveReplication - cannot register the local id " 
                                         + localId + " for the pid "+ pid.getValue() + 
                                         " into the xml_table since " + e.getMessage());
            }
            
            //register the mapping of localId and pid in the identifier table
            IdentifierManager.getInstance().createMapping(pid.getValue(), localId);

            //log the event
            try {
                logMetacat.debug("Logging the creation event.");
                EventLog.getInstance().log(event.getIpAddress(), event.getUserAgent(), ReplicationService.REPLICATIONUSER, localId, "create");
            } catch (Exception e) {
                logMetacat.warn("D1NodeService.insertDataObject - can't log the create event for the object " + pid.getValue());
            }

            // Schedule replication for this file
            boolean isMeta = true;
            ForceReplicationHandler frh = new ForceReplicationHandler(localId, "insert", isMeta, replicationNotificationServer);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
            try {
                source.close();
            } catch (IOException ee) {
                logMetacat.warn("NonXMLMetadataHandler.save - cannot close the source stream since " + ee.getMessage());
            }
            try {
                data.close();
            } catch (IOException ee) {
                logMetacat.warn("NonXMLMetadataHandler.save - cannot close the source stream since " + ee.getMessage());
            }
        }
    }
    
    /**
     * This method consumes the source input to do the check of validation. 
     * It will return an input stream for the next step - saving the bytes
     * @param validationInput  the input stream of the content
     * @param pid  the identifier will be saved (for log information)
     * @return a input stream for the step
     * @throws ServiceFailure
     * @throws InvalidRequest
     */
    private InputStream checkValidation(InputStream validationInput, Identifier pid) throws ServiceFailure, InvalidRequest {
        InputStream data = null;
        boolean canReset = false;
        boolean needCloseValidationInput = false;
        if (validationInput instanceof DetailedFileInputStream) {
            logMetacat.debug("NonXMLMetadataHandler.checkValidation - in the DetailedFileInputStream route for pid " + pid.getValue());
            //Metacat can read an input stream from the file associated with the DetailedFileInputStream for the next step
            DetailedFileInputStream input = (DetailedFileInputStream) validationInput;
            File sourceFile = input.getFile();
            try {
                validationInput = new FileInputStream(sourceFile);
                data = input;
                needCloseValidationInput = true;
            } catch (FileNotFoundException e) {
                throw new ServiceFailure("1190", "NonXMLMetadataHandler.checkValidation - cannot valid the meta data object " + 
                        " because cannot find the file associated with the detailed file input stream " + 
                        " for the object " + pid.getValue() + " since " + e.getMessage());
            }
        } else if (validationInput.markSupported() && validationInput instanceof ByteArrayInputStream) {
            logMetacat.debug("NonXMLMetadataHandler.checkValidation - in the resetable input stream route for pid " + pid.getValue());
            //Metacat can reset input stream for the next step
            data = validationInput;
            canReset = true;
        } else {
            logMetacat.debug("NonXMLMetadataHandler.checkValidation - in the another type of the input stream route for pid " + pid.getValue());
            //Metacat has to save the source input stream into a temp file and read the file later for the next step
            FileOutputStream out = null;
            try {
                tmpFile = generateTempFile("NonXML");
                out = new FileOutputStream(tmpFile);
                IOUtils.copyLarge(validationInput, out);
                validationInput = new FileInputStream(tmpFile);
                data = new FileInputStream(tmpFile);
            } catch (IOException e) {
                if (tmpFile != null) {
                    tmpFile.delete();
                }
                throw new ServiceFailure("1190", "NonXMLMetadataHandler.checkValidation - cannot save the meta data object " + 
                                          pid.getValue() + " into a temporary file since " + e.getMessage());
            } finally {
                try {
                    IOUtils.close(out);
                } catch (IOException e) {
                    logMetacat.warn("NonXMLMetadataHandler.checkValidation - cannot close the out put stream after saving the object for pid " 
                                    + pid.getValue() + "into a temporary file");
                }
            }
            
        }
        //do the check validation job
        boolean valid = false;
        try {
           valid = validate(validationInput);
           if (canReset) {
               try {
                   data.reset();
               } catch (IOException e) {
                   throw new ServiceFailure("1190", "NonXMLMetadataHandler.checkValidation - cannot save the object " + 
                           " because Metacat cannot reset the input stream even though the inputstream " + 
                           data.getClass().getCanonicalName() + " for the object " + pid.getValue() + 
                           " claim sit is resetable since " + e.getMessage());
               }
           }
        } catch (InvalidRequest e) {
            try {
                if (tmpFile != null) {
                    tmpFile.delete();
                }
                if (validationInput != null) {
                    validationInput.close();
                }
            } catch (IOException ee) {
                logMetacat.warn("NonXMLMetadataHandler.checkValidation - cannot close the invalidation stream since " + ee.getMessage());
            }
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.checkValidation - the metadata object " + pid.getValue() + 
                                    " is invalid: " + e.getMessage());
        }
        
        if (!valid) {
            try {
                if (tmpFile != null) {
                    tmpFile.delete();
                }
                if (validationInput != null) {
                    validationInput.close();
                }
            } catch (IOException ee) {
                logMetacat.warn("NonXMLMetadataHandler.checkValidation - cannot close the invalidation stream since " + ee.getMessage());
            }
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.checkValidation - the metadata object " + pid.getValue() + " is invalid.");
        }
        if (needCloseValidationInput) {
            //this route handle that the validationInput stream was created as a new input stream. We need to close it. 
            try {
                validationInput.close();
            } catch (IOException e) {
                throw new ServiceFailure("1190", "NonXMLMetadataHandler.checkValidation - cannot save the object " + 
                        " because Metacat cannot reset the input stream even though the inputstream " + 
                        data.getClass().getCanonicalName() + " for the object " + pid.getValue() + 
                        " claim sit is resetable since " + e.getMessage());
            }
        }
        return data;
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
