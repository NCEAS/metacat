package edu.ucsb.nceas.metacat.dataone.quota;


import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v2.SystemMetadata;



/**
 * A class to decide the quota type (storage, portal and etc) based on the given name space
 * @author tao
 *
 */
public class QuotaTypeDeterminer {
    public static final String STORAGE = "storage";
    public static final String PORTAL = "portal";
    
    private static Log logMetacat  = LogFactory.getLog(QuotaTypeDeterminer.class);
    private List<String> portalNameSpaces = null;
    private String quotaType = null;
    private String instanceId = null;
    
    
    /**
     * Constructor
     * @param portalNameSpaces  list of portal objects' name space
     * @throws ServiceFailure 
     */
    public QuotaTypeDeterminer(List<String> portalNameSpaces) throws ServiceFailure {
        this.portalNameSpaces = portalNameSpaces;
        if (this.portalNameSpaces == null || this.portalNameSpaces.isEmpty()) {
            throw new ServiceFailure("4893", "The propery " + QuotaServiceManager.PROPERTYNAMEOFPORTALNAMESPACE + " in metacat.properties file can'be blank.");
        }
    }
    
    /**
     * Determine the quota type and instance id from the object format information in the system metadata
     * @param sysmeta  the system metadata associated with the request object
     * @throws InvalidRequest
     */
    public void determine(SystemMetadata sysmeta) throws InvalidRequest {
        quotaType = null;//reset the quota type to null
        if (sysmeta != null) {
            ObjectFormatIdentifier format = sysmeta.getFormatId();
            String formatId = format.getValue();
            for (String portalNamespace : portalNameSpaces) {
                logMetacat.debug("QuotaTypeDeterminer.determine - the portal namespace in the metacat.properties file is " + portalNamespace + " and the format id in the sysmeta is " + formatId);
                if (portalNamespace != null && portalNamespace.equals(formatId)) {
                    quotaType = PORTAL;
                    Identifier sid = sysmeta.getSeriesId();
                    if (sid != null) {
                        instanceId = sid.getValue();//the portal type uses sid
                        if (instanceId == null || instanceId.trim().equals("")) {
                            throw new InvalidRequest("4891", "The series id field in the syste metadata of portal objects can't be null.");
                        }
                    } else {
                        throw new InvalidRequest("4891", "The series id field in the syste metadata of portal objects can't be null.");
                    }
                    break;
                }
            }
            if (quotaType == null) {
                //Since the type hasn't been assigned a value after iterate the portal name spaces. So it should be a storage type
                quotaType = STORAGE;
                instanceId = sysmeta.getIdentifier().getValue();//the storage type uses pid
            }
            logMetacat.debug("QuotaTypeDeterminer.determine - the final quota type is " + quotaType + " and the instance id is " + instanceId);
        } else {
            throw new InvalidRequest("4891", "The system metadata can't be null when Metacat determines a quota type.");
        }
    }
    
    /**
     * Get the quota type determined in the determine method
     * @return  the string of the quota type
     */
    public String getQuotaType() {
        return this.quotaType;
    }
    
    /**
     * Get the instance id determined in the determine method
     * @return  the string of the instance id
     */
    public String getInstanceId() {
        return this.instanceId;
    }
}
