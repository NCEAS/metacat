package edu.ucsb.nceas.metacat.dataone.quota;

import org.dataone.bookkeeper.api.Usage;

/**
 * A child class of the Usage class of BookKeeper. It has a new field - local id which represents sequence id in the
 * local quota_usage table
 * @author tao
 *
 */
public class LocalUsage extends Usage {
    private int localId = -1;
    private String subscriber = null;
    private String quotaType = null;
    private String requestor = null;
    
    /**
     * Get the subscriber of the usage
     * @return the subscriber
     */
    public String getSubscriber() {
        return subscriber;
    }

    /**
     * Set the subscriber associated with the usage
     * @param subscriber  the value will be set
     */
    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }

    /**
     * Get the quota type of the usage
     * @return the quota type
     */
    public String getQuotaType() {
        return quotaType;
    }

    /**
     * Set the given quota type to the usage
     * @param quotaType  the type will be set
     */
    public void setQuotaType(String quotaType) {
        this.quotaType = quotaType;
    }

    /**
     * Get the requestor associated with the usage
     * @return the requestor
     */
    public String getRequestor() {
        return requestor;
    }

    /**
     * Set requestor for this usage
     * @param requestor the value will be set
     */
    public void setRequestor(String requestor) {
        this.requestor = requestor;
    }

    /**
     * Get the local id associated with the local usage
     * @return the local id
     */
    public int getLocalId() {
        return this.localId;
    }
    
    /**
     * Set the local id
     * @param localId  the local id
     */
    public void setLocalId(int localId) {
        this.localId = localId;
    }
    
}
