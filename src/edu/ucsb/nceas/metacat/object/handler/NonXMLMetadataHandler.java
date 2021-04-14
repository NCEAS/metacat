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
import java.io.InputStream;

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

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
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
    private static Log logMetacat = LogFactory.getLog(CNodeService.class);
    private static String metadataStoragePath = null;
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
     * @param pid  the identifier associated with the input stream
     * @param expectedChecksum  the expected checksum for the saved file
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
    public String save(InputStream source, Identifier pid, Checksum expectedChecksum, 
                        Session session, String ipAddress, String userAgent) 
                        throws UnsupportedType, ServiceFailure, InvalidRequest, InvalidSystemMetadata, NotAuthorized {
        if (pid == null || pid.getValue() == null || pid.getValue().trim().equals("")) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the pid parameter should not be blank.");
        }
        if (!(source instanceof DetailedFileInputStream)) {
            throw new UnsupportedType("1140", "NonXMLMetadataHandler.save - Metacat only supports the DetailedFileInputStream object for saving " 
                                    + pid.getValue() +" into disk");
        }
        String localId = null;
        DetailedFileInputStream input = (DetailedFileInputStream) source;
        File sourceFile = input.getFile();
        boolean valid = false;
        try {
           valid = validate(new FileInputStream(sourceFile));
        } catch (FileNotFoundException e) {
            throw new ServiceFailure("1190", "NonXMLMetadataHandler.save - cannot valid the meta data object " + 
                                    " because cannot find the file associated with the detailed file input stream " + 
                                    " for the object " + pid.getValue() + " since " + e.getMessage());
        } catch (InvalidRequest e) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the metadata object " + pid.getValue() + " is invalid: " + e.getMessage());
        }
        
        if (!valid) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the metadata object " + pid.getValue() + " is invalid.");
        } else {
            if (metadataStoragePath == null) {
                throw new ServiceFailure("1190", "NonXMLMetadataHandler.save - cannot save the metadata object " + pid.getValue() + 
                        " into disk since the property - application.documentfilepath is not found in the metacat.properties file ");
            }
            //Save the meta data object to disk using "localId" as the name
            localId = D1NodeService.insertObject(input, pid, metadataStoragePath, session, expectedChecksum, ipAddress, userAgent); 
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
}
