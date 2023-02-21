/**
 *  '$RCSfile$'
 *  Copyright: 2023 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.systemmetadata;

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;


public class SystemMetadataManager {
    private static Log logMetacat = LogFactory.getLog(SystemMetadataManager.class);
    
    private static SystemMetadataManager manager = null;
    
    /**
     * Private constructor
     */
    private SystemMetadataManager() {
        
    }
    
    /**
     * Get the singleton SystemMetadataManager instance
     * @return  the instance of SystemMetadataManager
     */
    public static SystemMetadataManager getInstance() {
        if (manager == null) {
            synchronized (SystemMetadataManager.class) {
                if (manager == null) {
                    manager = new SystemMetadataManager();
                }
            }
        }
        return manager;
    }
    
    /**
     * Get the system metadata associated with the given identifier from the store
     * @param pid  the identifier to determine the system metadata
     * @return  the system metadata associated with the given identifier
     * @throws NotFound
     * @throws ServiceFailure
     * @throws InvalidRequest
     */
    public SystemMetadata get(Identifier pid) throws NotFound, ServiceFailure, InvalidRequest {
        SystemMetadata sm = null;
        try {
            if (pid != null) {
                logMetacat.debug("SystemMetadataManager.get - loading from store: " + pid.getValue());
                sm = IdentifierManager.getInstance().getSystemMetadata(pid.getValue());
            } else {
                
            }
            
        } catch (McdbDocNotFoundException e) {
            logMetacat.warn("could not load system metadata for: " +  pid.getValue());
            return null;
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return sm;
    }
   
    
    /**
     * Store a system metadata record into the store
     * @param sysmeta  the new system metadata will be inserted
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    public void store(SystemMetadata sysmeta) throws InvalidRequest, ServiceFailure {
        try {
            if (sysmeta != null) {
                Identifier pid = sysmeta.getIdentifier();
                if (pid != null && pid.getValue() != null & !pid.getValue().trim().equals("")) {
                    logMetacat.debug("SystemMetadataManager.store - storing System Metadata to store: " + pid.getValue());
                    IdentifierManager.getInstance().insertOrUpdateSystemMetadata(sysmeta);
                }
            }
        } catch (McdbDocNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (InvalidSystemMetadata e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    /**
     * Delete a system metadata record from the store
     * @param id  the identifier to determine the system metadata record
     * @throws NotFound
     * @throws ServiceFailure
     */
    public void delete(Identifier id) throws NotFound, ServiceFailure {
        if(id != null && id.getValue() != null && !id.getValue().trim().equals("")) {
            logMetacat.debug("SystemMetadataManager.delete - delete the identifier" + id.getValue());
            boolean success = IdentifierManager.getInstance().deleteSystemMetadata(id.getValue());
            if(!success) {
                throw new RuntimeException("SystemMetadataManager.delete - the system metadata of guid - " + id.getValue()+" can't be removed successfully.");
            }
        }
    }

}
