/**
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
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;

/**
 * A class represents the quota service for users
 * @author tao
 *
 */
public class QuotaService {
    public static final String USAGE = "usage";
    public static final String ACTIVE = "active";
    public static final String ARCHIVED = "archived";
    public static final String DELETED = "deleted";
    public static final String CREATEMETHOD = "create";
    public static final String UPDATEMETHOD = "update";
    public static final String PROPERTYNAMEOFPORTALNAMESPACE = "dataone.quotas.portal.namespaces";
    
    private static boolean storageEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.storage.enabled", false);
    private static boolean portalEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.portals.enabled", false);
    private static boolean replicationEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.replication.enabled", false);
    private static String nodeId = Settings.getConfiguration().getString("dataone.nodeId");
    private static int NUMOFTHREADS = Settings.getConfiguration().getInt("dataone.quotas.reportingThreadPoolSize", 5);
    private static boolean enabled = false; //If any of above variables are enabled, this variable will be true.
    private static ExecutorService executor = null;
    private static Log logMetacat  = LogFactory.getLog(QuotaService.class);
    
    private static QuotaService service = null;
    private BookKeeperClient client = null;
    private List<String> portalNameSpaces = null;
    
    /**
     * Private default constructor. The instance will have a bookeeperClient if the quota service is enabled.
     * @throws IOException 
     * @throws ServiceFailure 
     */
    private QuotaService() throws IOException, ServiceFailure {
        if (enabled) {
            client = BookKeeperClient.getInstance();
            executor = Executors.newFixedThreadPool(NUMOFTHREADS);
            portalNameSpaces = retrievePortalNameSpaces();
        }
    }
    
    /**
     * Retrieve the name space list of portal objects from settings
     * @return  list of portal name space
     */
    static List<String> retrievePortalNameSpaces() {
        return Settings.getConfiguration().getList(PROPERTYNAMEOFPORTALNAMESPACE);
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
     * 
     * @param subscriber  the subject of subscriber of the quota which will be used
     * @param requestor  the subject of the user who requests the usage
     * @param sysmeta  the system metadata of the object which will use the quota
     * @param method  the method name which will call the createUsage method (create or update)
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws IOException 
     * @throws InsufficientResources 
     * @throws NotFound 
     * @throws ClientProtocolException 
     */
    public void enforce(String subscriber, Subject requestor, SystemMetadata sysmeta, String method) throws ServiceFailure, InvalidRequest, ClientProtocolException, NotFound, InsufficientResources, IOException {
        long start = System.currentTimeMillis();
        QuotaTypeDeterminer determiner = new QuotaTypeDeterminer(portalNameSpaces);
        determiner.determine(sysmeta); //this method enforce the portal objects have the sid field in the system metadata
        if (enabled) {
            if (requestor == null) {
                throw new InvalidRequest("1102", "The quota subscriber, requestor can't be null");
            }
            if (subscriber == null || subscriber.trim().equals("")) {
                subscriber = requestor.getValue();//if we didn't get the subscriber information for the request header, we just assign it as the request's subject
            }
            String quotaType = determiner.getQuotaType();
            if (quotaType != null && quotaType.equals(QuotaTypeDeterminer.PORTAL) && method != null && method.equals(CREATEMETHOD)) {
                String instanceId = determiner.getInstanceId();
                logMetacat.debug("QuotaService.enforce - checking both portal and storage quota types for the instance " + instanceId);
                //this is to create a portal object. We should check both portal and storage quota
                double portalQuantity = 1;
                int portalQuotaId = hasSpace(subscriber, requestor.getValue(), QuotaTypeDeterminer.PORTAL, portalQuantity, instanceId);
                double storageQuantity = sysmeta.getSize().doubleValue();
                int storageQuotaId = hasSpace(subscriber, requestor.getValue(), QuotaTypeDeterminer.STORAGE, storageQuantity, instanceId);
                //if there are no InsufficientResource exceptions, we submit the usages to the remote bookkeeper server in another thread
                createUsage(portalQuotaId, instanceId, portalQuantity);
                createUsage(storageQuotaId, sysmeta.getIdentifier().getValue(), storageQuantity);
                
            } else if (quotaType != null && quotaType.equals(QuotaTypeDeterminer.STORAGE)) {
                String instanceId = sysmeta.getIdentifier().getValue();
                logMetacat.debug("QuotaService.enforce - only checking storage quota type for the instance " + instanceId);
                //others (including update a portal object) just check the storage quota
                double storageQuantity = sysmeta.getSize().doubleValue();
                int storageQuotaId = hasSpace(subscriber, requestor.getValue(), QuotaTypeDeterminer.STORAGE, storageQuantity, instanceId);
                //if there is no InsufficientResource exception, we submit the usages to the remote bookkeeper server in another thread
                createUsage(storageQuotaId, instanceId, storageQuantity);
            } else {
                throw new InvalidRequest("1102", "Metacat doesn't support the quota type " + quotaType + " for the pid " + sysmeta.getIdentifier().getValue());
            }
        }
        long end = System.currentTimeMillis();
        logMetacat.info("QuotaService.enforce - checking quota and reporting usage took " + (end-start)/1000 + " seconds.");
    }
    /**
     * Check if the quota has enough space for this request. If there is not enough space, an exception will be thrown
     * @param subscriber  the subject of subscriber of the quota which will be used 
     * @param requestor  the subject of the user who requests the usage
     * @param quotaType  the type of quota
     * @param quantity  the amount of the usage for the request
     * @return the quota id which will be used. -1 will be returned if the quota service is disabled
     * @throws InvalidRequest 
     * @throws IOException 
     * @throws ServiceFailure 
     * @throws NotFound 
     * @throws ClientProtocolException 
     * @throws InsufficientResources 
     */
     int hasSpace(String subscriber, String requestor, String quotaType, double quantity, String instanceId) throws InvalidRequest, ClientProtocolException, NotFound, ServiceFailure, IOException, InsufficientResources {
        int quotaId = -1;
        boolean hasSpace = false;
        if(enabled) {
            if (subscriber != null && !subscriber.trim().equals("") && quotaType != null && !quotaType.trim().equals("") && requestor != null && !requestor.trim().equals("")) {
                List<Quota> quotas = client.getInstance().listQuotas(subscriber, requestor, quotaType);
                for (Quota quota : quotas) {
                    if (quota != null) {
                        double hardLimit = quota.getHardLimit();
                        logMetacat.debug("QuotaService.hasSpace - the hardLimit in the quota " + subscriber + " with type " + quotaType + "is " + hardLimit + " and the request amount of usage is " + quantity + " for the instance id " + instanceId);
                        if (hardLimit >= quantity) {
                            quotaId = quota.getId();
                            hasSpace = true;
                            logMetacat.debug("QuotaService.hasSpace - the hardLimit in the quota is " + hardLimit + " and it is greater than or equals the request amount of usage " + quantity + ". So the request is granted for the instance id " + instanceId);
                            break;
                        }
                    }
                }
            } else {
                throw new InvalidRequest("1102", "The quota subscriber, requestor and quota type can't be null or blank for submit the instance " + instanceId);
            }
        } else {
            hasSpace = true;//if the service is not enabled, it is always true
            logMetacat.debug("QuotaService.hasSpace - the quota serive is disabled and the request is walways granted.");
        }
        if (!hasSpace) {
            throw new InsufficientResources("1160", "The subscriber " + subscriber + " doesn't have enough " + quotaType + " quota to fullfill the request for the instance id " + instanceId + ". Please contact " + subscriber + " to request more quota.");
        }
        return quotaId;
    }
    
    
    /**
     * Create a usage associated with the given quota id. It will create the usage by another thread.
     * Metacat only executes it if the service is enabled
     * @param quotaId  the quota id which the usage will be associated with
     * @param instanceId  the id of the usage instance (pid for the storage type, and sid for the portal type)  
     * @param quantity  the amount of the usage
     */
    void createUsage(int quotaId, String instanceId, double quantity) {
        if(enabled) {
            Usage usage = new Usage();
            usage.setObject(USAGE);
            usage.setQuotaId(quotaId);
            usage.setInstanceId(instanceId);
            usage.setQuantity(quantity);
            usage.setStatus(ACTIVE);
            CreateUsageTask task = new CreateUsageTask(usage, client);
            executor.submit(task);
        }
    }
}
