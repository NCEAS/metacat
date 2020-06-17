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
import java.sql.SQLException;
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
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;

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
    public static final String ARCHIVEMETHOD = "archive";
    public static final String DELETEMETHOD = "delete";
    public static final String PROPERTYNAMEOFPORTALNAMESPACE = "dataone.quotas.portal.namespaces";
    public static final String QUOTASUBSRIBERHEADER = "X-DataONE-subscriber";
    
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
    private QuotaService() throws ServiceFailure {
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
    public static QuotaService getInstance() throws ServiceFailure {
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
     * @throws NotImplemented 
     */
    public void enforce(String subscriber, Subject requestor, SystemMetadata sysmeta, String method) throws ServiceFailure, InvalidRequest, InsufficientResources, NotImplemented {
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
            if (quotaType != null && quotaType.equals(QuotaTypeDeterminer.PORTAL)) {
                String instanceId = determiner.getInstanceId();
                enforcePortalQuota(subscriber,requestor, instanceId, sysmeta, method);
            } else if (quotaType != null && quotaType.equals(QuotaTypeDeterminer.STORAGE)) {
                enforceStorageQuota(subscriber,requestor, sysmeta, method);
            } else {
                throw new InvalidRequest("1102", "Metacat doesn't support the quota type " + quotaType + " for the pid " + sysmeta.getIdentifier().getValue());
            }
        }
        long end = System.currentTimeMillis();
        logMetacat.info("QuotaService.enforce - checking quota and reporting usage took " + (end-start)/1000 + " seconds.");
    }
    
    /**
     * Enforce the portal quota checking
     * @param subscriber  the subscriber of a quota
     * @param requestor  the requestor which request the quota
     * @param instanceId  the sid of the portal object
     * @param sysmeta  the system metadata of the object will use the quota
     * @param method  the dataone qpi method calls the checking
     * @throws InvalidRequest
     * @throws ClientProtocolException
     * @throws NotFound
     * @throws ServiceFailure
     * @throws InsufficientResources
     * @throws IOException
     * @throws NotImplemented
     */
    private void enforcePortalQuota(String subscriber, Subject requestor, String instanceId, SystemMetadata sysmeta, String method) throws InvalidRequest, 
                                                                             ServiceFailure, InsufficientResources, NotImplemented {
        if (portalEnabled) {
            logMetacat.debug("QuotaService.enforcePortalQuota - checking both portal and storage quota types for the instance " + instanceId);
            //this is to create a portal object. We should check both portal and storage quota
            double portalQuantity = 1;
            //double storageQuantity = sysmeta.getSize().doubleValue();
            //int storageQuotaId = hasSpace(subscriber, requestor.getValue(), QuotaTypeDeterminer.STORAGE, storageQuantity, instanceId);
            //report a portal usage in another thread
            if (method != null && method.equals(CREATEMETHOD)) {
                boolean checkSpace = true;
                int portalQuotaId = lookUpQuotaId(checkSpace, subscriber, requestor.getValue(), QuotaTypeDeterminer.PORTAL, portalQuantity, instanceId);
                createUsage(portalQuotaId, instanceId, portalQuantity);//report the usage in another thread
            } else if (method != null && method.equals(ARCHIVEMETHOD)) {
                if (isLastUnarchivedInChain(sysmeta.getIdentifier().getValue(), instanceId)) {
                    //take action only we are archiving the last object which hasn't been archived in the sid chain
                    boolean checkSpace = false;
                    int portalQuotaId = lookUpQuotaId(checkSpace, subscriber, requestor.getValue(), QuotaTypeDeterminer.PORTAL, portalQuantity, instanceId);
                    updateUsage(portalQuotaId, instanceId, portalQuantity);
                } else {
                    logMetacat.debug("QuotaService.enforcePortalQuota - Metacat is not archiving the last object which hasn't been archived in the series chain " + instanceId + ". It needs to do nothing for the portal quota");
                }
             } else if (method != null && method.equals(DELETEMETHOD)) {
                 if (isLastUnDletedInChain(sysmeta.getIdentifier().getValue(), instanceId)) {
                     //take action only we are deleting the last object which hasn't been deleted in the sid chain
                     boolean checkSpace = false;
                     int portalQuotaId = lookUpQuotaId(checkSpace, subscriber, requestor.getValue(), QuotaTypeDeterminer.PORTAL, portalQuantity, instanceId);
                     deleteUsage(portalQuotaId, instanceId, portalQuantity);
                 } else {
                     logMetacat.debug("QuotaService.enforcePortalQuota - Metacat is not deleting the last object in the series chain " + instanceId + ". It needs to do nothing for the portal quota");
                 }
            } else if (method != null && method.equals(UPDATEMETHOD)) {
                logMetacat.info("QuotaService.enforcePortalQuota - Metacat is updating an object in the series chain " + instanceId + ". It needs to do nothing for the portal quota.");
            } else {
                throw new InvalidRequest("1102", "In the portal quota checking process, Metacat doesn't support the method  " + method  + " for the pid " + sysmeta.getIdentifier().getValue());
            }
            enforceStorageQuota(subscriber, requestor, sysmeta, method);
        }
    }
    
    /**
     * Checking if the given pid is last one in this series chain hasn't been archived
     * @param pid  the pid of the object will be checked
     * @param sid  the id of the series chain will be checked
     * @return true if the pid is the last one hasn't been archived; otherwise fals.
     * @throws SQLException 
     * @throws InvalidRequest 
     * @throws ServiceFailure 
     */
    boolean isLastUnarchivedInChain(String pid, String sid) throws InvalidRequest, ServiceFailure {
        boolean lastOne = true;
        if (sid != null && !sid.trim().equals("") && pid != null && !pid.trim().equals("")) {
            try {
                List<String> guids = IdentifierManager.getInstance().getAllPidsInChain(sid);
                for (String guid : guids) {
                    if (!guid.equals(pid)) {
                        Identifier identifier = new Identifier();
                        identifier.setValue(guid);
                        SystemMetadata sysmeta = HazelcastService.getInstance().getSystemMetadataMap().get(identifier);
                        if(!sysmeta.getArchived()) {
                            lastOne = false;//found one which is not archived and its guid doesn't equals the pid
                            logMetacat.debug("QuotaService.isLastUnarchivedInChain - found the guid " + guid + " in the chain with sid " + sid +" hasn't been archived. So the whole chain hasn't been archived.");
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                throw new ServiceFailure("1104", "QuotaService.isLastUnarchivedInChain - Can't get the pids list in the chain with the sid " + sid + " since " + e.getMessage());
            }
            
        } else {
            throw new InvalidRequest("1102", "QuotaService.isLastUnarchivedInChain - the pid or sid can't be null or blank for the portal quota.");
        }
        return lastOne;
    }
    
    /**
     * Checking if the given pid is last one in this series chain hasn't been deleted
     * @param pid  the pid of the object will be checked
     * @param sid  the id of the series chain will be checked
     * @return true if the pid is the last one hasn't been deleted; otherwise fals.
     * @throws SQLException 
     * @throws InvalidRequest 
     * @throws ServiceFailure 
     */
    boolean isLastUnDletedInChain(String pid, String sid) throws InvalidRequest, ServiceFailure {
        boolean lastOne = false;
        if (sid != null && !sid.trim().equals("") && pid != null && !pid.trim().equals("")) {
            try {
                List<String> guids = IdentifierManager.getInstance().getAllPidsInChain(sid);
                if (guids.size() ==1) {
                    String guid = guids.get(0);
                    if (guid != null && guid.equals(pid)) {
                        lastOne = true;//the series chain only has one element and it is the given pid
                        logMetacat.debug("QuotaService.isLastUnDletedInChain - found the pid " + pid + " in the chain with sid " + sid +" is the only object which hasn't been deleted.");
                    }
                }
            } catch (SQLException e) {
                throw new ServiceFailure("1104", "QuotaService.isLastUnDletedInChain - Can't get the pids list in the chain with the sid " + sid + " since " + e.getMessage());
            }
            
        } else {
            throw new InvalidRequest("1102", "QuotaService.isLastUnDletedInChain - the pid or sid can't be null or blank for the portal quota.");
        }
        return lastOne;
    }
    
    /**
     * Enforce the storage quota checking
     * @param subscriber  the subscriber of a quota
     * @param requestor  the requestor which request the quota
     * @param sysmeta  the system metadata of the object will use the quota
     * @param method  the dataone qpi method calls the checking
     * @throws NotImplemented
     */
    private void enforceStorageQuota(String subscriber, Subject requestor, SystemMetadata sysmeta, String method) throws NotImplemented {
        if (storageEnabled) {
            throw new NotImplemented("0000", "The storage quota service hasn't been implemented yet.");
        }
    }
    /**
     * Check if the quota has enough space for this request. If there is not enough space, an exception will be thrown
     * @param checkEnoughSpace  indicator if we need to check if the found quota has enough space for this usage
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
     int lookUpQuotaId(boolean checkEnoughSpace, String subscriber, String requestor, String quotaType, double quantity, String instanceId) throws InvalidRequest, ServiceFailure, InsufficientResources {
        int quotaId = -1;
        boolean hasSpace = false;
        if(enabled) {
            if (subscriber != null && !subscriber.trim().equals("") && quotaType != null && !quotaType.trim().equals("") && requestor != null && !requestor.trim().equals("")) {
                List<Quota> quotas = client.getInstance().listQuotas(subscriber, requestor, quotaType);
                for (Quota quota : quotas) {
                    if (quota != null) {
                        if (checkEnoughSpace) {
                            double hardLimit = quota.getHardLimit();
                            logMetacat.debug("QuotaService.lookUpQuotaId - need to check space: the hardLimit in the quota " + subscriber + " with type " + quotaType + "is " + hardLimit + " and the request amount of usage is " + quantity + " for the instance id " + instanceId);
                            if (hardLimit >= quantity) {
                                quotaId = quota.getId();
                                hasSpace = true;
                                logMetacat.debug("QuotaService.lookUpQuotaId - the hardLimit in the quota is " + hardLimit + " and it is greater than or equals the request amount of usage " + quantity + ". So the request is granted for the instance id " + instanceId);
                                break;
                            }
                        } else {
                            logMetacat.debug("QuotaService.lookUpQuotaId - do NOT need to check space: found a quota with subscriber " + subscriber + " with type " + quotaType  + " for the instance id " + instanceId);
                            quotaId = quota.getId();
                            hasSpace = true;//since we don't need to check if it has enough space, so hasSpace is always true
                            logMetacat.debug("QuotaService.lookUpQuotaId - do NOT need to check space: found a quota with the quota id " + quotaId + " subscriber " + subscriber + " with type " + quotaType  + " for the instance id " + instanceId);
                            break;
                        }
                    }
                }
            } else {
                throw new InvalidRequest("1102", "The quota subscriber, requestor and quota type can't be null or blank for submit the instance " + instanceId);
            }
        } else {
            hasSpace = true;//if the service is not enabled, it is always true
            logMetacat.debug("QuotaService.lookUpQuotaId - the quota serive is disabled and the request is walways granted.");
        }
        if (!hasSpace) {
            throw new InsufficientResources("1160", "The subscriber " + subscriber + " doesn't have enough " + quotaType + " quota to fullfill the request for the instance id " + instanceId + ". Please contact " + subscriber + " to request more quota.");
        }
        return quotaId;
    }
    
    
    
    /**
     * Create a usage associated with the given quota id. It will create the usage by another thread.
     * Metacat executes it without checking if the service is enabled. So the caller should check it.
     * @param quotaId  the quota id which the usage will be associated with
     * @param instanceId  the id of the usage instance (pid for the storage type, and sid for the portal type)  
     * @param quantity  the amount of the usage
     */
    void createUsage(int quotaId, String instanceId, double quantity) {
        Usage usage = new Usage();
        usage.setObject(USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(ACTIVE);
        CreateUsageTask task = new CreateUsageTask(usage, client);
        executor.submit(task);
    }
    
    /**
     * Update a usage with the archived status associated with the given quota id in the remote book keeper server. Locally we will add a new record with the archived status 
     * in the table. It will be run by another thread.
     * Metacat executes it without checking if the service is enabled. So the caller should check it.
     * @param quotaId  the quota id which the usage will be associated with
     * @param instanceId  the id of the usage instance (pid for the storage type, and sid for the portal type)  
     * @param quantity  the amount of the usage
     */
    void updateUsage(int quotaId, String instanceId, double quantity) {
        Usage usage = new Usage();
        usage.setObject(USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(ARCHIVED);
        UpdateUsageTask task = new UpdateUsageTask(usage, client);
        executor.submit(task);
    }
    
    /**
     * Delete a usage associated with the given quota id in the remote book keeper server. However, locally we will add a new record with the deleted status 
     * in the table. It will be run by another thread.
     * Metacat executes it without checking if the service is enabled. So the caller should check it.
     * @param quotaId  the quota id which the usage will be associated with
     * @param instanceId  the id of the usage instance (pid for the storage type, and sid for the portal type)  
     * @param quantity  the amount of the usage
     */
    void deleteUsage(int quotaId, String instanceId, double quantity) {
        Usage usage = new Usage();
        usage.setObject(USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(DELETED);
        DeleteUsageTask task = new DeleteUsageTask(usage, client);
        executor.submit(task);
    }
}
