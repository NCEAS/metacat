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
import java.text.DateFormat;
import java.text.ParseException;
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
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;


import edu.ucsb.nceas.metacat.shared.HandlerException;


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
    public static final String QUOTASUBJECTHEADER = "X-DataONE-Quota-Subject";
    
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
     * Metacat only needs one timer, but this method will be called on four different servlet init
     * methods. So the method has a indicator of already starting a timer and is synchronized to
     * make sure only one timer will be started.
     */
    public synchronized void startDailyCheck() {
        if (enabled && !timerStarted) {
            String startTimeStr = Settings.getConfiguration()
                                        .getString("dataone.quotas.dailyReportingUsagesTime");
            logMetacat.debug("QuotaServiceManager.startDailCheck - the property value of "
                                        + " dataone.quotas.dailyReportingUsagesTime is "
                                        + startTimeStr);
            Date startTime = null;
            try {
                startTime = combineDateAndGivenTime(new Date(), startTimeStr);
            } catch (Exception e) {
                logMetacat.error("QuotaServiceManager.startDailyCheck - Metacat can't figure out "
                         + " the time setting as the value of the property "
                         + "dataone.quotas.dailyReportingUsagesTime in the metacat.propertis since "
                         + e.getMessage()
                         + " So Metacat will use the default time 11.00 PM for reporting usages to "
                         + "the book keeper server every day.");
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
            logMetacat.info("QuotaServiceManager.startDailyCheck - the timer will start to check "
                            + " and report un-reported usages at "
                            + date + " at daily base. This message should only be shown once.");
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new FailedReportingAttemptChecker(executor, client),
                                        startTime, 24*3600*1000);//daily job
            //indicate that Metacat already started the timer and shouldn't start another one again.
            timerStarted = true;
        }
    }
    
    /**
     * Enforce quota service
     * @param quotaSubject  the subject of the quota which will be used
     * @param requestor  the subject of the user who requests the usage
     * @param sysmeta  the system metadata of the object which will use the quota
     * @param method  the method name which will call the createUsage method (create or update
     * @throws InsufficientResources 
     * @throws InvalidRequest 
     * @throws ServiceFailure 
     * @throws NotFound 
     * @throws NotImplemented 
     */
    public void enforce(String quotaSubject, Subject requestor, SystemMetadata sysmeta,
                                            String method) throws InsufficientResources,
                                        InvalidRequest, ServiceFailure, NotImplemented, NotFound {
        long start = System.currentTimeMillis();
        if (enabled) {
           try {
                if (requestor == null) {
                    throw new InvalidRequest("1102", "The quota requestor can't be null");
                }
                if (quotaSubject == null || quotaSubject.trim().equals("")) {
                    quotaSubject = requestor.getValue();//if we didn't get the quota subject information for the request header, we just assign it as the request's subject
                }
                QuotaTypeDeterminer determiner = new QuotaTypeDeterminer(portalNameSpaces);
                determiner.determine(sysmeta); //this method enforce the portal objects have the sid field in the system metadata
                String quotaType = determiner.getQuotaType();
                if (quotaType != null && quotaType.equals(QuotaTypeDeterminer.PORTAL)) {
                    String instanceId = determiner.getInstanceId();
                    logMetacat.debug("QuotaServiceManager.enforce - handle the portal quota service with instance id " + instanceId + " and pid " + sysmeta.getIdentifier().getValue());
                    enforcePortalQuota(quotaSubject,requestor, instanceId, sysmeta, method);
                    enforceStorageQuota(quotaSubject,requestor, sysmeta, method); //also enforce the storage quota service
                } else if (quotaType != null && quotaType.equals(QuotaTypeDeterminer.STORAGE)) {
                    enforceStorageQuota(quotaSubject,requestor, sysmeta, method);
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
     * @param quotaSubject  the subject of a quota
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
    private void enforcePortalQuota(String quotaSubject, Subject requestor,
                                        String instanceId, SystemMetadata sysmeta, String method)
                                        throws InvalidRequest, ServiceFailure,
                                        InsufficientResources, NotImplemented, NotFound,
                                        UnsupportedEncodingException {
        if (portalEnabled) {
            PortalQuotaService.getInstance(executor, client).enforce(quotaSubject, requestor,
                                                                instanceId, sysmeta, method);
        } else {
            logMetacat.info("QuotaServiceManager.enforcePrtaolQuota - the portal quota "
                            + "service is disabled");
        }
    }

    /**
     * Enforce the storage quota checking
     * @param quotaSubject  the subject of the quota
     * @param requestor  the requestor which request the quota
     * @param sysmeta  the system metadata of the object will use the quota
     * @param method  the dataone qpi method calls the checking
     * @throws NotImplemented
     */
    private void enforceStorageQuota(String quotaSubject, Subject requestor,
                                    SystemMetadata sysmeta, String method) throws NotImplemented {
        if (storageEnabled) {
            throw new NotImplemented("0000", "The storage quota service hasn't been implemented yet.");
        } else {
            logMetacat.info("QuotaServiceManager.enforceStorageQuota - the storage quota "
                            + "service is disabled");
        }
    }

    /**
     * This method will combine a given date and given time string (in short format) to
     * a new date. If the given time (e.g 10:00 AM) is before the given date (e.g 2:00
     * PM Aug 21, 2005), then the time will set to the following day, 10:00 AM Aug 22,
     * 2005. If the given time (e.g 10:00 AM) is after the given date
     * (e.g 8:00 AM Aug 21, 2005), the time will set to be 10:00 AM Aug 21, 2005.
     *
     * @param now  the date will be combine
     * @param givenTime
     *            the format should be "10:00 AM " or "2:00 PM"
     * @return the Date object
     * @throws HandlerException
     */
    protected static Date combineDateAndGivenTime(Date now, String givenTime)
                                                                        throws HandlerException {
        try {
            Date givenDate = parseTime(givenTime);
            Date newDate = null;
            String currentTimeString = getTimeString(now);
            Date currentTime = parseTime(currentTimeString);
            if (currentTime.getTime() >= givenDate.getTime()) {
                logMetacat.info(
                    "QuotaServiceManager.combineDateAndGivenTime - Today already pass the"
                        + " given time, we should set it as tomorrow");
                String dateAndTime = getDateString(now) + ", " + givenTime;
                Date combinationDate = parseDateTime(dateAndTime);
                // new date should plus 24 hours to make is the second day
                newDate = new Date(combinationDate.getTime() + 24 * 3600 * 1000);
            } else {
                logMetacat.info(
                    "QuotaServiceManager.combineDateAndGivenTime - Today haven't pass the"
                        + " given time, we should it as today");
                String dateAndTime = getDateString(now) + ", " + givenTime;
                newDate = parseDateTime(dateAndTime);
            }
            logMetacat.info(
                "QuotaServiceManager.combineDateAndGivenTime - final setting time is "
                    + newDate.toString());
            return newDate;
        } catch (ParseException pe) {
            throw new HandlerException(
                "QuotaServiceManager.combineDateAndGivenTime - " + "parsing error: "
                    + pe.getMessage());
        }
    }

    /**
     * Parse a given string to a Date object in short format. For example, given time is
     * 10:00 AM, the date will be return as Jan 1 1970, 10:00 AM
     * @param timeString  the time string which will be parsed
     * @return the Date object of the time string
     * @throws ParseException
     */
    private static Date parseTime(String timeString) throws ParseException {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        Date time = format.parse(timeString);
        logMetacat.debug(
            "QuotaServiceManager.parseTime - Date string is after parse a time string "
                + time.toString());
        return time;
    }

    /**
     * Parse a given string to date and time. The date format is long and time format is short.
     * @param timeString  the string of time
     * @return the Date object of the time string
     * @throws ParseException
     */
    private static Date parseDateTime(String timeString) throws ParseException {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
        Date time = format.parse(timeString);
        logMetacat.debug(
            "QuotaServiceManager.parseDateTime - Date string is after parse a time string "
                + time.toString());
        return time;
    }

    /**
     * Get a date string from a Date object. The date format will be long
     * @param now  the Date object which will be parsed
     * @return the string presentation (long) of the Date object
     */
    private static String getDateString(Date now) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
        String s = df.format(now);
        logMetacat.debug("QuotaServiceManager.getDateString - Today is " + s);
        return s;
    }

    /**
     * Get a time string from a Date object. The time format will be short
     * @param now  the Date object which will be parsed
     * @return the string presentation (short) of the Date object
     */
    private static String getTimeString(Date now) {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        String s = df.format(now);
        logMetacat.debug("QuotaServiceManager.getTimeString - Time is " + s);
        return s;
    }


}
