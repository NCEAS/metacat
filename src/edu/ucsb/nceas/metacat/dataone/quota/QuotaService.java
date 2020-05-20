/**
 *    Purpose: Implements a service for managing a Hazelcast cluster member
 *  Copyright: 2020 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.dataone.quota;

import org.dataone.configuration.Settings;

/**
 * A class represents the quota service for users
 * @author tao
 *
 */
public class QuotaService {
    private BookKeeperClient client = null;
    private boolean storageEnabled = false;
    private boolean portalEnabled = false;
    private boolean replicationEnabled = false;
    private static QuotaService service = null;
    
    /**
     * Private default constructor
     */
    private QuotaService() {
        storageEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.storage.enabled");
        portalEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.portals.enabled");
        replicationEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.replication.enabled");
        client = BookKeeperClient.getInstance();
    }
    
    /**
     * Get the singleton instance of the service
     * @return the quota service instance
     */
    public static QuotaService getInstance() {
        if (service == null) {
            synchronized (QuotaService.class) {
                if (service == null) {
                    service = new QuotaService();
                }
            }
        }
        return service;
    }
    
    /**
     * Check if the quota has enough space for this request
     * @param submitterSubject  the subject of the submitter of the request
     * @param quotaSubject  the subject of quota will be used in the request
     * @param quotaName  the name of quota
     * @param usage  the usage for the request
     * @return true if the quota has the enough space;otherwise false
     */
    public boolean hasUsage(String submitterSubject, String quotaSubject, String quotaName, long usage) {
        boolean has = false;
        return has;
    }
    
    
    /**
     * Update the quota with the usage
     * @param submitterSubject  the subject of the submitter of the request
     * @param quotaSubject  the subject of quota will be used in the request
     * @param quotaName  the name of quota
     * @param usage  the usage for the request
     */
    public void updateUsage(String submitterSubject, String quotaSubject, String quotaName, long usage) {
        
    }
}
