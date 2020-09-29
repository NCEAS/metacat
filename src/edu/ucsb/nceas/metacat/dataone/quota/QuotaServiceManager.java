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
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
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

import com.lmax.disruptor.InsufficientCapacityException;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.replication.ReplicationHandler;

/**
 * A class manages the quota service for users
 * @author tao
 *
 */
public class QuotaServiceManager {
    public static final String USAGE = "usage";
    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";
    public static final String DELETED = "deleted";
    public static final String CREATEMETHOD = "create";
    public static final String UPDATEMETHOD = "update";
    public static final String ARCHIVEMETHOD = "archive";
    public static final String DELETEMETHOD = "delete";
    public static final String PROPERTYNAMEOFPORTALNAMESPACE = "dataone.quotas.portal.namespaces";
    public static final String QUOTASUBSRIBERHEADER = "X-DataONE-Quota-Subject";
    
    private static boolean storageEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.storage.enabled", false);
    private static boolean portalEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.portals.enabled", false);
    private static boolean replicationEnabled = Settings.getConfiguration().getBoolean("dataone.quotas.replication.enabled", false);
    private static int NUMOFTHREADS = Settings.getConfiguration().getInt("dataone.quotas.reportingThreadPoolSize", 5);
    private static boolean enabled = false; //If any of above variables are enabled, this variable will be true.
    private static ExecutorService executor = null;
    private static Log logMetacat  = LogFactory.getLog(QuotaServiceManager.class);
    
    private static QuotaServiceManager service = null;
    private static BookKeeperClient client = null;
    private List<String> portalNameSpaces = null;
    private static boolean timerStarted = false;
    
    /**
     * Private default constructor. The instance will have a bookeeperClient if the quota service is enabled.
     * @throws IOException 
     * @throws ServiceFailure 
     */
    private QuotaServiceManager() throws ServiceFailure {
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
    public static QuotaServiceManager getInstance() throws ServiceFailure {
        enabled = storageEnabled || portalEnabled || replicationEnabled;
        if (service == null) {
            synchronized (QuotaServiceManager.class) {
                if (service == null) {
                    service = new QuotaServiceManager();
                }
            }
        }
        return service;
    }
    
    /**
     * Check if the quota service is enabled.
     * @return true if it is enabled; otherwise false.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Start a timer to check failed usage reporting and report them again in daily base.
     * Metacat only needs one timer, but this method will be called on four different servlet init methods. 
     * So the method has a indicator of already starting a timer and is synchronized to make sure only one timer will be started.
     */
    public synchronized void startDailyCheck() {
        if (enabled && !timerStarted) {
            String startTimeStr = Settings.getConfiguration().getString("dataone.quotas.dailyReportingUsagesTime");
            logMetacat.debug("QuotaServiceManager.startDailCheck - the property value of dataone.quotas.dailyReportingUsagesTime is " + startTimeStr);
            Date startTime = null;
            try {
                startTime = ReplicationHandler.combinateCurrentDateAndGivenTime(startTimeStr);
            } catch (Exception e) {
                logMetacat.error("QuotaServiceManager.startDailyCheck - Metacat can't figure out the time setting as the value of the property dataone.quotas.dailyReportingUsagesTime in the metacat.propertis since " +
                                  e.getMessage() + " So Metacat will use the default time 11.00 PM for reporting usages to the book keeper server every day.");
                Calendar date = new GregorianCalendar();
                // reset hour, minutes, seconds and millis
                date.set(Calendar.HOUR_OF_DAY, 23);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 0);
                date.set(Calendar.MILLISECOND, 0);
                startTime = date.getTime();
            }
            SimpleDateFormat format = new SimpleDateFormat(); 
            String date = format.format(startTime); 
            logMetacat.info("QuotaServiceManager.startDailyCheck------------------------the timer will start to check and report un-reported usages at " + date + " at daily base. This message should only be shown once.");
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new FailedReportingAttemptChecker(executor, client), startTime, 24*3600*1000);//daily job
            timerStarted = true;//indicate that Metacat already started the timer and shouldn't start another one again.
        }
    }
    
    /**
     * Enforce quota service
     * @param subscriber  the subject of subscriber of the quota which will be used
     * @param requestor  the subject of the user who requests the usage
     * @param sysmeta  the system metadata of the object which will use the quota
     * @param method  the method name which will call the createUsage method (create or update
     * @throws InsufficientResources 
     * @throws InvalidRequest 
     * @throws ServiceFailure 
     * @throws NotFound 
     * @throws NotImplemented 
     */
    public void enforce(String subscriber, Subject requestor, SystemMetadata sysmeta, String method) throws InsufficientResources, InvalidRequest, ServiceFailure, NotImplemented, NotFound {
        long start = System.currentTimeMillis();
        if (enabled) {
           try {
                if (requestor == null) {
                    throw new InvalidRequest("1102", "The quota subscriber, requestor can't be null");
                }
                if (subscriber == null || subscriber.trim().equals("")) {
                    subscriber = requestor.getValue();//if we didn't get the subscriber information for the request header, we just assign it as the request's subject
                }
                QuotaTypeDeterminer determiner = new QuotaTypeDeterminer(portalNameSpaces);
                determiner.determine(sysmeta); //this method enforce the portal objects have the sid field in the system metadata
                String quotaType = determiner.getQuotaType();
                if (quotaType != null && quotaType.equals(QuotaTypeDeterminer.PORTAL)) {
                    String instanceId = determiner.getInstanceId();
                    logMetacat.debug("QuotaServiceManager.enforce - handle the portal quota service with instance id " + instanceId + " and pid " + sysmeta.getIdentifier().getValue());
                    enforcePortalQuota(subscriber,requestor, instanceId, sysmeta, method);
                    enforceStorageQuota(subscriber,requestor, sysmeta, method); //also enforce the storage quota service
                } else if (quotaType != null && quotaType.equals(QuotaTypeDeterminer.STORAGE)) {
                    enforceStorageQuota(subscriber,requestor, sysmeta, method);
                } else {
                    throw new InvalidRequest("1102", "QuotaServiceManager.enforce - Metacat doesn't support the quota type " + quotaType + " for the pid " + sysmeta.getIdentifier().getValue());
                }
           } catch (UnsupportedEncodingException e) {
               throw new ServiceFailure("1190", "QuotaServiceManager.enforce - Metacat doesn't support the encoding format " + e.getMessage() + " for the pid " + sysmeta.getIdentifier().getValue());
           }
        } else {
            logMetacat.info("QuotaServiceManager.enforce - the quota services are disabled");
        }
        long end = System.currentTimeMillis();
        logMetacat.info("QuotaServiceManager.enforce - checking quota and reporting usage took " + (end-start)/1000 + " seconds.");
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
     * @throws UnsupportedEncodingException 
     */
    private void enforcePortalQuota(String subscriber, Subject requestor, String instanceId, SystemMetadata sysmeta, String method) throws InvalidRequest, 
                                                                             ServiceFailure, InsufficientResources, NotImplemented, NotFound, UnsupportedEncodingException {
        if (portalEnabled) {
            PortalQuotaService.getInstance(executor, client).enforce(subscriber, requestor, instanceId, sysmeta, method);
        } else {
            logMetacat.info("QuotaServiceManager.enforcePrtaolQuota - the portal quota service is disabled");
        }
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
        } else {
            logMetacat.info("QuotaServiceManager.enforceStorageQuota - the storage quota service is disabled");
        }
    }
    
}
