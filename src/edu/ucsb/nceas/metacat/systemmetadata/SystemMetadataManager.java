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
import java.util.Vector;

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
    private final static int TIME_OUT = 3000;
    private static Vector<String> lockedIds = new Vector<String>(); 
    
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
     * Get the system metadata associated with the given identifier from the store.
     * If the returned value is null, this means the system metadata is not found
     * @param pid  the identifier to determine the system metadata
     * @return  the system metadata associated with the given identifier
     * @throws ServiceFailure
     */
    public SystemMetadata get(Identifier pid) throws ServiceFailure {
        SystemMetadata sm = null;
        try {
            if (pid != null && pid.getValue() != null && !pid.getValue().trim().equals("")) {
                logMetacat.debug("SystemMetadataManager.get - loading from store: " + pid.getValue());
                sm = IdentifierManager.getInstance().getSystemMetadata(pid.getValue());
            } 
        } catch (McdbDocNotFoundException e) {
            logMetacat.warn("could not load system metadata for: " +  pid.getValue());
            return null;
        } catch (Exception e) {
            throw new ServiceFailure("0000", e.getMessage());
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
        if (sysmeta != null) {
            Identifier pid = sysmeta.getIdentifier();
            if (pid != null && pid.getValue() != null & !pid.getValue().trim().equals("")) {
                //Check if there is another thread is storing the system metadata for the same pid
                //Event though the Vector class is thread-safe, we still need the synchronized keyword
                // to make sure the lockedIds.contains and lockedIds.add methods can be accessed by one thread (atomic).
                synchronized (lockedIds) {
                    while (lockedIds.contains(pid.getValue())) {
                        try {
                            lockedIds.wait(TIME_OUT);
                        } catch (InterruptedException e) {
                            logMetacat.info("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue() + 
                                             " the lock waiting was interrupted " + e.getMessage());
                        }
                    }
                    lockedIds.add(pid.getValue());
                }
                //Try to write the system metadata into db and remove the pid from the vector and wake up the waiting threads. 
                try {
                    logMetacat.debug("SystemMetadataManager.store - storing system metadata to store: " + pid.getValue());
                    IdentifierManager.getInstance().insertOrUpdateSystemMetadata(sysmeta);
                } catch (McdbDocNotFoundException e) {
                    throw new InvalidRequest("0000", "SystemMetadataManager.store - can't store the system metadata for pid " + pid.getValue() + " since " + e.getMessage());
                } catch (SQLException e) {
                    throw new ServiceFailure("0000", "SystemMetadataManager.store - can't store the system metadata for pid " + pid.getValue() + " since " + e.getMessage());
                } catch (InvalidSystemMetadata e) {
                    throw new InvalidRequest("0000", "SystemMetadataManager.store - can't store the system metadata for pid " + pid.getValue() + " since " + e.getMessage());
                } finally {
                    lockedIds.remove(pid.getValue());
                    lockedIds.notifyAll();
                }
            }
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
                throw new ServiceFailure("0000", "SystemMetadataManager.delete - the system metadata of guid - " + id.getValue()+" can't be removed successfully.");
            }
        }
    }
    
    /**
     * Lock the system metadata for the given id
     * @param id  the identifier of the system metadata will be locked
     */
    public void lock(Identifier id) {
        
    }
    
    /**
     * unlock the system metadata for the given id
     * @param id  the identifier of the system metadata will be unlocked
     */
    public void unlock(Identifier id) {
        
    }

}
