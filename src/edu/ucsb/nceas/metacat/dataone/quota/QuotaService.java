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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;
import org.dataone.bookkeeper.api.Quota;
import org.dataone.bookkeeper.api.Usage;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;

/**
 * A class represents the quota service for users
 * @author tao
 *
 */
public class QuotaService {
    private static boolean storageEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.storage.enabled", false);
    private static boolean portalEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.portals.enabled", false);
    private static boolean replicationEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.replication.enabled", false);
    private static int NUMOFTHREADS = Settings.getConfiguration().getInt("dataone.quotas.reportingThreadPoolSize", 5);
    private static boolean enabled = false; //If any of above variables are enabled, this variable will be true.
    private static ExecutorService executor = null;
    private static Log logMetacat  = LogFactory.getLog(QuotaService.class);
    
    private static QuotaService service = null;
    private BookKeeperClient client = null;
    
    /**
     * Private default constructor. The instance will have a bookeeperClient if the quota service is enabled.
     * @throws IOException 
     * @throws ServiceFailure 
     */
    private QuotaService() throws IOException, ServiceFailure {
        if (enabled) {
            client = BookKeeperClient.getInstance();
            executor = Executors.newFixedThreadPool(NUMOFTHREADS);
        }
    }
    
    /**
     * Get the singleton instance of the service
     * @return the quota service instance. The instance will have a bookeeperClient if the quota service is enabled.
     * @throws IOException 
     * @throws ServiceFailure 
     */
    public static QuotaService getInstance() throws IOException, ServiceFailure {
        enabled = storageEnabled || portalEnabled || replicationEnabled;
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
     * Check if the quota has enough space for this request.
     * @param subscriber  the subject of subscriber of the quota which will be used 
     * @param requestor  the subject of the user who requests the usage
     * @param quotaType  the type of quota
     * @param usage  the amount of the usage for the request
     * @return true if the quota has the enough space;otherwise false. If the service is not enabled, it always returns true.
     * @throws InvalidRequest 
     * @throws IOException 
     * @throws ServiceFailure 
     * @throws NotFound 
     * @throws ClientProtocolException 
     */
    public boolean hasSpace(String subscriber, String requestor, String quotaType, double usage) throws InvalidRequest, ClientProtocolException, NotFound, ServiceFailure, IOException {
        boolean hasSpace = false;
        if(enabled) {
            if (subscriber != null && !subscriber.trim().equals("") && quotaType != null && !quotaType.trim().equals("") && requestor != null && !requestor.trim().equals("")) {
                List<Quota> quotas = client.getInstance().listQuotas(subscriber, requestor, quotaType);
                for (Quota quota : quotas) {
                    if (quota != null) {
                        double hardLimit = quota.getHardLimit();
                        logMetacat.debug("QuotaService.hasSpace - the hardLimit in the quota " + subscriber + " with type " + quotaType + "is " + hardLimit + " and the request usage is " + usage);
                        if (hardLimit >= usage) {
                            hasSpace = true;
                            logMetacat.debug("QuotaService.hasSpace - the hardLimit in the quota is " + hardLimit + " and it is greater than or equals the request usage " + usage + ". So the request is granted.");
                            break;
                        }
                    }
                }
            } else {
                throw new InvalidRequest("0000", "The quota subscriber, requestor and quota type can't be null or blank");
            }
        } else {
            hasSpace = true;//if the service is not enabled, it is always true
            logMetacat.debug("QuotaService.hasSpace - the quota serive is disabled and the request is walways granted.");
        }
        return hasSpace;
    }
    
    
    /**
     * Update the quota with the usage. Metacat only executes it if the service is enabled
     * @param submitterSubject  the subject of the submitter of the request
     * @param quotaSubject  the subject of quota will be used in the request
     * @param quotaName  the name of quota
     * @param usage  the usage for the request
     */
    public void updateUsage(String submitterSubject, String quotaSubject, String quotaName, long usage) {
        if(enabled) {
            Usage usageObj = new Usage();
        }
        
    }
}
