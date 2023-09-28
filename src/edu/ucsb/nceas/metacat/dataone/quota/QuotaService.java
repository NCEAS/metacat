package edu.ucsb.nceas.metacat.dataone.quota;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;

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
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;

/**
 * A class represents an abstract quota service. Its child classes, such as PortalQuotaService
 * and StorageQuotaService
 * should implement the abstract method - enforce.
 * @author tao
 *
 */
public abstract class QuotaService {
    private static Log logMetacat = LogFactory.getLog(QuotaService.class);
    public static String nodeId = Settings.getConfiguration().getString("dataone.nodeId");
    public static int DEFAULT_QUOTA_ID = -1;

    protected static ExecutorService executor = null;
    protected static BookKeeperClient client = null;


    /**
     * The method will be implemented by the child classes to enforce the quota service
     * @param quotaSubject  the subject of the quota which will be used
     * @param requestor  the subject of the user who requests the usage
     * @param instanceId  the id of the object will be applied the quota
     * @param sysmeta  the system metadata of the object which will use the quota
     * @param method  the method name which will call the createUsage method (create or update)
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws InsufficientResources
     * @throws NotImplemented
     */
    public abstract void enforce(
        String quotaSubject, Subject requestor, String instanceId, SystemMetadata sysmeta,
        String method)
        throws ServiceFailure, InvalidRequest, InsufficientResources, NotImplemented, NotFound,
        UnsupportedEncodingException;

    /**
     * Checking if the given pid is last one in this series chain hasn't been archived
     * @param pid  the pid of the object will be checked
     * @param sid  the id of the series chain will be checked
     * @return true if the pid is the last one hasn't been archived; otherwise false.
     * @throws SQLException
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    protected boolean isLastUnarchivedInChain(String pid, String sid)
        throws InvalidRequest, ServiceFailure {
        boolean lastOne = true;
        if (sid != null && !sid.trim().equals("") && pid != null && !pid.trim().equals("")) {
            try {
                List<String> guids = IdentifierManager.getInstance().getAllPidsInChain(sid);
                for (String guid : guids) {
                    if (!guid.equals(pid)) {
                        Identifier identifier = new Identifier();
                        identifier.setValue(guid);
                        SystemMetadata sysmeta =
                            SystemMetadataManager.getInstance().get(identifier);
                        if (sysmeta.getArchived() == null || !sysmeta.getArchived()) {
                            lastOne =
                                false;//found one which is not archived and its guid doesn't
                            // equal the pid
                            logMetacat.debug(
                                "QuotaService.isLastUnarchivedInChain - found the guid " + guid
                                    + " in the chain with sid " + sid
                                    + " hasn't been archived. So the whole chain hasn't been "
                                    + "archived.");
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                throw new ServiceFailure(
                    "1104",
                    "QuotaService.isLastUnarchivedInChain - Can't get the pids list in the chain "
                        + "with the sid "
                        + sid + " since " + e.getMessage());
            }

        } else {
            throw new InvalidRequest(
                "1102",
                "QuotaService.isLastUnarchivedInChain - the pid or sid can't be null or blank for"
                    + " the portal quota.");
        }
        return lastOne;
    }

    /**
     * Checking if the given pid is last one in this series chain hasn't been deleted
     * @param pid  the pid of the object will be checked
     * @param sid  the id of the series chain will be checked
     * @return true if the pid is the last one hasn't been deleted; otherwise false.
     * @throws SQLException
     * @throws InvalidRequest
     * @throws ServiceFailure
     */
    protected boolean isLastUndeletedInChain(String pid, String sid)
        throws InvalidRequest, ServiceFailure {
        boolean lastOne = false;
        if (sid != null && !sid.trim().equals("") && pid != null && !pid.trim().equals("")) {
            try {
                List<String> guids = IdentifierManager.getInstance().getAllPidsInChain(sid);
                if (guids.size() == 1) {
                    String guid = guids.get(0);
                    if (guid != null && guid.equals(pid)) {
                        lastOne =
                            true;//the series chain only has one element and it is the given pid
                        logMetacat.debug(
                            "QuotaService.isLastUndeletedInChain - found the pid " + pid
                                + " in the chain with sid " + sid
                                + " is the only object which hasn't been deleted.");
                    }
                }
            } catch (SQLException e) {
                throw new ServiceFailure(
                    "1104",
                    "QuotaService.isLastUndeletedInChain - Can't get the pids list in the chain "
                        + "with the sid "
                        + sid + " since " + e.getMessage());
            }

        } else {
            throw new InvalidRequest(
                "1102",
                "QuotaService.isLastUndeletedInChain - the pid or sid can't be null or blank for "
                    + "the portal quota.");
        }
        return lastOne;
    }

    /**
     * Check if the quota has enough space for this request. If there is not enough space, an
     * exception will be thrown
     * @param checkEnoughSpace  indicator if we need to check if the found quota has enough space
     *                         for this usage
     * @param quotaSubject  the subject of the quota which will be used
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
     * @throws UnsupportedEncodingException
     */
    protected int checkQuota(
        boolean checkEnoughSpace, String quotaSubject, String requestor, String quotaType,
        double quantity, String instanceId)
        throws InvalidRequest, ServiceFailure, InsufficientResources, NotFound,
        UnsupportedEncodingException {
        int quotaId = DEFAULT_QUOTA_ID;
        boolean hasSpace = false;
        List<Quota> quotas = client.getInstance().listQuotas(quotaSubject, requestor, quotaType);
        for (Quota quota : quotas) {
            if (quota != null) {
                if (checkEnoughSpace) {
                    double hardLimit = 0;
                    double existedUsages = 0;
                    Double hardLimitObj = quota.getHardLimit();
                    if (hardLimitObj != null) {
                        hardLimit = hardLimitObj.doubleValue();
                    }
                    Double existedUsagesObj = quota.getTotalUsage();
                    if (existedUsagesObj != null) {
                        existedUsages = existedUsagesObj.doubleValue();
                    }
                    logMetacat.debug(
                        "QuotaService.lookUpQuotaId - need to check space: the hardLimit in the "
                            + "quota with the quota subject "
                            + quotaSubject + " with the type " + quotaType + "is " + hardLimit
                            + ", the existed usages is " + existedUsages
                            + " and the request amount of usage is " + quantity
                            + " for the instance id " + instanceId);
                    if (hardLimit >= existedUsages + quantity) {
                        quotaId = quota.getId();
                        hasSpace = true;
                        logMetacat.debug(
                            "QuotaService.lookUpQuotaId - the hardLimit in the quota is "
                                + hardLimit
                                + " and it is greater than or equals the request amount of usage "
                                + quantity + " plus existed usage " + existedUsages
                                + ". So the request is granted for the instance id " + instanceId);
                        break;
                    }
                } else {
                    logMetacat.debug(
                        "QuotaService.lookUpQuotaId - do NOT need to check space: found a quota "
                            + "with the quota subject "
                            + quotaSubject + " with the type " + quotaType + " for the instance id "
                            + instanceId);
                    quotaId = quota.getId();
                    hasSpace =
                        true;//since we don't need to check if it has enough space, so hasSpace
                    // is always true
                    logMetacat.debug(
                        "QuotaService.lookUpQuotaId - do NOT need to check space: found a quota "
                            + "with the quota id "
                            + quotaId + " with the quota subject " + quotaSubject
                            + " with the type " + quotaType + " for the instance id " + instanceId);
                    break;
                }
            }
        }
        if (!hasSpace) {
            throw new InsufficientResources(
                "1160", "The quota subject " + quotaSubject + " doesn't have enough " + quotaType
                + " quota to fulfil the request for the instance id " + instanceId
                + ". Please contact " + quotaSubject + " to request more quota.");
        }
        return quotaId;
    }


    /**
     * Create a usage associated with the given quota id. It will create the usage by another
     * thread.
     * Metacat executes it without checking if the service is enabled. So the caller should check
     * it.
     * @param quotaId  the quota id which the usage will be associated with
     * @param instanceId  the id of the usage instance (pid for the storage type, and sid for the
     *                   portal type)
     * @param quantity  the amount of the usage
     */
    void createUsage(int quotaId, String instanceId, double quantity) {
        logMetacat.debug(
            "QuotaService.createUsage - create a usage for the instance id " + instanceId
                + " for the quantity " + quantity + " with quota id " + quotaId);
        Usage usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.ACTIVE);
        usage.setNodeId(nodeId);
        CreateUsageTask task = new CreateUsageTask(usage, client);
        executor.submit(task);
    }

    /**
     * Update a usage with the archived status associated with the given quota id in the remote
     * book keeper server. Locally we will add a new record with the archived status
     * in the table. It will be run by another thread.
     * Metacat executes it without checking if the service is enabled. So the caller should check
     * it.
     * @param quotaId  the quota id which the usage will be associated with
     * @param instanceId  the id of the usage instance (pid for the storage type, and sid for the
     *                   portal type)
     * @param quantity  the amount of the usage
     */
    protected void updateUsage(int quotaId, String instanceId, double quantity) {
        logMetacat.debug(
            "QuotaService.updateUsage - update a usage with the instance id " + instanceId
                + " and quota id " + quotaId + " for the quantity " + quantity);
        Usage usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.INACTIVE);
        usage.setNodeId(nodeId);
        UpdateUsageTask task = new UpdateUsageTask(usage, client);
        executor.submit(task);
    }

    /**
     * Delete a usage associated with the given quota id in the remote book keeper server.
     * However, locally we will add a new record with the deleted status
     * in the table. It will be run by another thread.
     * Metacat executes it without checking if the service is enabled. So the caller should check
     * it.
     * @param quotaId  the quota id which the usage will be associated with
     * @param instanceId  the id of the usage instance (pid for the storage type, and sid for the
     *                   portal type)
     * @param quantity  the amount of the usage
     */
    protected void deleteUsage(int quotaId, String instanceId, double quantity) {
        logMetacat.debug(
            "QuotaService.deleteUsage - delete a usage with the instance id " + instanceId
                + " and quota id " + quotaId + " for the quantity " + quantity);
        Usage usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setNodeId(nodeId);
        usage.setStatus(QuotaServiceManager.DELETED);
        DeleteUsageTask task = new DeleteUsageTask(usage, client);
        executor.submit(task);
    }
}
