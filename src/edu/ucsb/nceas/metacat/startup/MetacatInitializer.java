package edu.ucsb.nceas.metacat.startup;

import java.io.File;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.HashStoreConversionAdmin;
import edu.ucsb.nceas.metacat.admin.UpgradeStatus;
import edu.ucsb.nceas.metacat.util.DatabaseUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.ServiceFailure;

import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.Sitemap;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.index.queue.FailedIndexResubmitTimerTask;
import edu.ucsb.nceas.metacat.index.queue.IndexGenerator;
import edu.ucsb.nceas.metacat.index.queue.IndexGeneratorTimerTask;
import edu.ucsb.nceas.metacat.plugin.MetacatHandlerPluginManager;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.storage.Storage;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;


/**
 * A class to initialize Metacat
 * @author tao
 *
 */
@WebListener
public class MetacatInitializer implements ServletContextListener{
    public static final String APPLICATION_NAME = "metacat";
    private static boolean _sitemapScheduled = false;
    private static boolean fullInit = false;
    private static Log logMetacat = LogFactory.getLog(MetacatInitializer.class);
    private static Timer timer = new Timer();
    private static Storage storage;

    /**
     * An implementation of ServletContextListener that is called automatically by the servlet
     * container on startup, and used to verify that we have the essential components in place
     * and Metacat is initialized. So it can run successfully.
     * @param ServletContextEvent  the ServletContextEvent object will be used in Metacat
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        StartupRequirementsChecker checker = new StartupRequirementsChecker();
        // make sure the required components are ready
        checker.contextInitialized(sce);
        try {
            ServletContext context = sce.getServletContext();
            context.setAttribute("APPLICATION_NAME", APPLICATION_NAME);
            ServiceService.getInstance(context);
            logMetacat.debug("MetacatInitializer.init - ServiceService singleton created ");
            // Register preliminary services
            ServiceService.registerService("PropertyService", PropertyService.getInstance(context));
            ServiceService.registerService("SkinPropertyService",
                                                                SkinPropertyService.getInstance());
            ServiceService.registerService("SessionService", SessionService.getInstance());
            // Check to see if the user has requested to bypass configuration
            // (dev option) and check see if metacat has been configured.
            // If both are false then stop the initialization
            if (!Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"))
                && !ConfigurationUtil.bypassConfiguration()
                && !ConfigurationUtil.isMetacatConfigured()) {
                if (PropertyService.arePropertiesConfigured()) {
                    // Those methods are for the admin pages
                    DBConnectionPool.getInstance();
                    convertStorage();
                }
                fullInit = false;
                return;
            }
            initAfterMetacatConfig();
            fullInit = true;
        } catch (SQLException e) {
            String errorMessage = "SQL problem while initializing MetaCat: "
                    + e.getMessage();
            checker.abort(errorMessage, e);
        } catch (GeneralPropertyException gpe) {
            String errorMessage = "Could not retrieve property while initializing MetaCat: "
                    + gpe.getMessage();
            checker.abort(errorMessage, gpe);
        } catch (ServiceException se) {
            String errorMessage = "Service problem while initializing MetaCat: "
                + se.getMessage();
            checker.abort(errorMessage, se);
        } catch (UtilException ue) {
            String errorMessage = "Utility problem while initializing MetaCat: "
                + ue.getMessage();
            checker.abort(errorMessage, ue);
        } catch (ServletException e) {
            String errorMessage = "Problem to initialize K8s cluster: " + e.getMessage();
            checker.abort(errorMessage, e);
        } catch (MetacatUtilException e) {
            String errorMessage = "Problem to check if the Metacat instance is configured : "
                    + e.getMessage();
            checker.abort(errorMessage, e);
        } catch (AdminException e) {
            String errorMessage = "Problem to check the status of the storage conversion : "
                + e.getMessage();
            checker.abort(errorMessage, e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logMetacat.info("MetacatInitializer.destroy - Destroying MetacatServlet");
        ServiceService.stopAllServices();
        timer.cancel();
        DBConnectionPool.release();
    }

    /**
     * Initialize the remainder of Metacat. This is the part that can only
     * be initialized after metacat properties have been configured.
     * @throws ServiceException
     * @throws ServletException
     * @throws PropertyNotFoundException
     * @throws SQLException
     * @throws UtilException
     */
    protected void initAfterMetacatConfig() throws ServiceException, ServletException,
                                        PropertyNotFoundException, SQLException, UtilException {
            ServiceService.registerService("DatabaseService", DatabaseService.getInstance());
            // initialize DBConnection pool
            DBConnectionPool connPool = DBConnectionPool.getInstance();
            logMetacat.debug("MetacatInitializer.initAfterMetacatConfig - DBConnection "
                          + "pool initialized with size: " + connPool.getSizeOfDBConnectionPool());

            // Always check & auto-update DB and MN settings if running in Kubernetes
            if (Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"))) {
                K8sAdminInitializer.initializeK8sInstance();
            }

            initStorage();

            // register the XML schema service
            ServiceService.registerService("XMLSchemaService", XMLSchemaService.getInstance());

            SessionService.getInstance().unRegisterAllSessions();

            // Turn on sitemaps if appropriate
            initializeSitemapTask();

            // initialize the plugins
            MetacatHandlerPluginManager.getInstance();

            // initialize the RabbitMQ service
            ServiceService.registerService("IndexQueueService", IndexGenerator.getInstance());

            // set up the time task to reindex objects (for the dataone api)
            startIndexReGenerator();

            logMetacat.info("MetacatInitializer.initAfterMetacatConfig - Metacat ("
                                      + MetacatVersion.getVersionID() + ") initialized.");
    }


    /**
     * Schedule a site map task
     */
    public static void initializeSitemapTask() {
        boolean sitemap_enabled = false;
        try {
            sitemap_enabled = Boolean
                                .parseBoolean(PropertyService.getProperty("sitemap.enabled"));
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.info("sitemap.enabled property not found so sitemaps are disabled");
        }
        if (sitemap_enabled) {
            // Schedule the sitemap generator to run periodically
            scheduleSitemapGeneration();
        }
    }

    /**
     * Schedule the sitemap generator to run periodically and update all of the sitemap files for
     * search indexing engines
     */
    protected static void scheduleSitemapGeneration() {
        if (_sitemapScheduled) {
            logMetacat.debug("MetacatHandler.scheduleSitemapGeneration: Tried to call "
                                 + "scheduleSitemapGeneration() when a sitemap was already "
                                 + "scheduled. Doing nothing.");

            return;
        }

        String directoryName = null;
        long sitemapInterval = 0;

        try {
            directoryName = SystemUtil.getContextDir() + FileUtil.getFS() + "sitemaps";
            sitemapInterval = Long.parseLong(PropertyService.getProperty("sitemap.interval"));
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("MetacatHandler.scheduleSitemapGeneration - "
                                 + "Could not run site map generation because property "
                                 + "could not be found: " + pnfe.getMessage());
        }

        File directory = new File(directoryName);
        directory.mkdirs();

        // Determine sitemap location and entry base URLs. Prepends the
        // secure server URL from the metacat configuration if the
        // values in the properties don't start with 'http' (e.g., '/view')
        String serverUrl = "";
        String locationBase = "";
        String entryBase = "";
        String portalBase = "";
        List<String> portalFormats = new ArrayList<>();

        try {
            serverUrl = SystemUtil.getServerURL();
            locationBase = PropertyService.getProperty("sitemap.location.base");
            entryBase = PropertyService.getProperty("sitemap.entry.base");
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("MetacatHandler.scheduleSitemapGeneration - "
                                 + "Could not run site map generation because property "
                                 + "could not be found: " + pnfe.getMessage());
        }

        try {
            portalBase = PropertyService.getProperty("sitemap.entry.portal.base");
            portalFormats.addAll(Arrays.asList(
                PropertyService.getProperty("sitemap.entry.portal.formats").split(";")));
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.info("MetacatHandler.scheduleSitemapGeneration - "
                                + "Could not get portal-specific sitemap properties: "
                                + pnfe.getMessage());
        }

        // Prepend server URL to locationBase if needed
        if (!locationBase.startsWith("http")) {
            locationBase = serverUrl + locationBase;
        }

        // Prepend server URL to entryBase if needed
        if (!entryBase.startsWith("http")) {
            entryBase = serverUrl + entryBase;
        }

        // Prepend server URL to portalBase if needed
        if (!portalBase.startsWith("http")) {
            portalBase = serverUrl + portalBase;
        }

        Sitemap smap = new Sitemap(directory, locationBase, entryBase, portalBase, portalFormats);
        long firstDelay = 10000; // in milliseconds

        timer.schedule(smap, firstDelay, sitemapInterval);
        _sitemapScheduled = true;
    }

    /**
     * Start to re-generate indexes for those haven't been indexed in another thread.
     * It will create a timer to run this task periodically.
     * If the property of "index.regenerate.interval" is less than 0, the thread would NOT run.
     */
    protected static void startIndexReGenerator() {
        boolean regenerateIndex = false;
        try {
            regenerateIndex = Boolean.parseBoolean(PropertyService
                                         .getProperty("index.regenerate.sincelastProcessDate"));
        } catch (PropertyNotFoundException e) {
            logMetacat.debug("MetacatServlet.startIndexGenerate - the property "
                              + "index.regenerate.sincelastProcessDate"
                              + " is not found and Metacat will use " + regenerateIndex
                              + " as the default one.");
        }
        long period = 86400000;//milliseconds
        try {
            period = Long.parseLong(PropertyService
                                          .getProperty("index.regenerate.interval"));
        } catch (PropertyNotFoundException | NumberFormatException e) {
            logMetacat.debug("MetacatServlet.startIndexGenerate - the property "
                          + "index.regenerate.interval"
                          + " is not found and Metacat will use " + period
                          + " ms as the default one.");
        }
        if(regenerateIndex && period > 0) {
            String timeStrOfFirstRun = "11:50 PM";
            try {
                timeStrOfFirstRun = PropertyService.getProperty("index.regenerate.firsttime");
            } catch (PropertyNotFoundException e) {
                logMetacat.debug("MetacatServlet.startIndexGenerate - the property "
                        + "index.regenerate.firsttime"
                        + " is not found and Metacat will use " + timeStrOfFirstRun
                        + " as the default one.");
            }
            Date timeOfFirstRun = determineTimeOfFirstRunRegeneratingThread(timeStrOfFirstRun);
            IndexGeneratorTimerTask generator = new IndexGeneratorTimerTask();
            timer.schedule(generator, timeOfFirstRun, period);
            logMetacat.info("MetacatServlet.startIndexGenerate - the "
                    + "first time for running the thread to reindex "
                    + "the objects is "
                    + timeOfFirstRun.toString()
                    + " and the period is " + period);
        }
        boolean regneratedFailedIndex = true;
        try {
            regneratedFailedIndex = Boolean.parseBoolean(PropertyService
                                           .getProperty("index.regenerate.failedObject"));
        } catch (PropertyNotFoundException e) {
            logMetacat.debug("MetacatServlet.startIndexGenerate - the property "
                    + "index.regenerate.failedObject"
                    + " is not found and Metacat will use "+ regneratedFailedIndex
                    + " as the default one.");
        }
        long delay = 1200000;
        try {
            delay = Long.parseLong(PropertyService
                                .getProperty("index.regenerate.failedTask.delay"));
        } catch (PropertyNotFoundException | NumberFormatException e) {
            logMetacat.debug("MetacatServlet.startIndexGenerate - the property "
                    + "index.regenerate.failedTask.delay"
                   + " is not found and Metacat will use " + delay + " ms as the default one.");
        }
        long failedInterval = 3600000;
        try {
            failedInterval = Long.parseLong(PropertyService
                                    .getProperty("index.regenerate.failedTask.interval"));
        } catch (PropertyNotFoundException | NumberFormatException e) {
            logMetacat.debug("MetacatServlet.startIndexGenerate - the property "
                    + "index.regenerate.failedTask.interval"
                    + " is not found and Metacat will use " + failedInterval
                    + " ms as the default one.");
        }
        if (regneratedFailedIndex && failedInterval > 0) {
            FailedIndexResubmitTimerTask task = new FailedIndexResubmitTimerTask();
            timer.schedule(task, delay, failedInterval);
            logMetacat.info("MetacatServlet.startIndexGenerate - the "
                    + "delay for running the thread to reindex "
                    + "the failed objects is " + delay
                    + " and the period is " + failedInterval);
        }
    }

    /**
     * Determine the time to run the regenerating thread in the first time.
     * If the given time already passed or only be less than 2 seconds to pass,
     * we need to set the timeOfFirstRun to be 24 hours latter (the second day)
     * @param givenTime the given time to run. The format should like 10:00 PM.
     * It uses the default time zone set in the host.
     */
    private static Date determineTimeOfFirstRunRegeneratingThread(String givenTime) {
        Date timeOfFirstRun = null;
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        Date givenDate = null;
        try {
            givenDate = format.parse(givenTime);
        } catch (ParseException e) {
            try {
                logMetacat.warn("The given start time string "
                                + givenTime + " can't be parsed since "
                                + e.getMessage() + " and we will use the "
                                + " default time - 11:50 PM");
                givenDate = format.parse("11:50 PM");
            } catch (ParseException ee) {
                givenDate = new Date();
            }
        }
        logMetacat.debug("The time (given) to first time run the thread is "
                          + givenDate.toString());
        Calendar date = new GregorianCalendar();
        date.setTime(givenDate);
        int hour = date.get(Calendar.HOUR_OF_DAY);
        logMetacat.debug("The given hour is " + hour);
        int minute = date.get(Calendar.MINUTE);
        logMetacat.debug("The given minutes is " + minute);
        //set the hour and minute to today
        Calendar today = new GregorianCalendar();
        today.set(Calendar.HOUR_OF_DAY, hour);
        today.set(Calendar.MINUTE, minute);
        timeOfFirstRun = today.getTime();
        logMetacat.debug("The time (after transforming to today) to "
                        + "first time run the thread is "
                        + timeOfFirstRun.toString());
        Date now = new Date();
        if((timeOfFirstRun.getTime() - now.getTime()) <2000) {
            //if the given time has already passed, or is less than 2
            //seconds in the future, we need to set the timeOfFirstRun to be
            //24 hours latter (i.e. the second day)
            logMetacat.debug("The time (after transforming to today) to "
                 + "first time run the thread " + timeOfFirstRun.toString()
                 + " passed and we will delay it 24 hours");
            timeOfFirstRun = new Date(timeOfFirstRun.getTime()+24*3600*1000);
        }
        logMetacat.debug("The final time of the first time running the thread is "
                        + timeOfFirstRun.toString());
        return timeOfFirstRun;
    }

    /**
     * Get the status of full-initialization.
     * @return true if the Metacat instance is fully initialized; false otherwise.
     */
    public static boolean isFullyInitialized() {
        return fullInit;
    }

    /**
     * Get the storage instance
     * @return the instance of storage
     * @throws ServiceFailure
     */
    public static Storage getStorage() throws ServiceFailure {
        if (storage == null) {
            throw new ServiceFailure("", "The storage system hasn't been initialized.");
        }
        return storage;
    }

    /**
     * Initialize the storage system.
     * @throws PropertyNotFoundException
     * @throws ServiceException
     */
    public static synchronized void initStorage() throws PropertyNotFoundException, ServiceException {
        if (storage == null) {
            storage = Storage.getInstance();
        }
    }

    /**
     * Start to convert the storage if the db is configured and the storage conversion status is
     * PENDING or FAILED
     * @throws MetacatUtilException
     * @throws AdminException
     */
    protected void convertStorage()
        throws MetacatUtilException, AdminException, GeneralPropertyException {
        logMetacat.debug("Start of the convertStorage method in the MetacatInitializer class. "
                             + "This statement is before checking the DB' status.");
        if (DatabaseUtil.isDatabaseConfigured() && PropertyService.arePropertiesConfigured()) {
            UpgradeStatus status = HashStoreConversionAdmin.getStatus();
            if (status == UpgradeStatus.IN_PROGRESS) {
                logMetacat.debug("The hashstore conversion status is IN PROGRESS. This means "
                                     + "the previous conversion was interrupted and Metacat will "
                                     + "set the status FAILED to continue the process.");
                HashStoreConversionAdmin.updateInProgressStatus(UpgradeStatus.FAILED);
            }
            if (status == UpgradeStatus.PENDING || status == UpgradeStatus.FAILED) {
                logMetacat.debug("Metacat starts an auto storage conversion when the database is "
                                     + "configured: " + DatabaseUtil.isDatabaseConfigured()
                    + "and the storage conversion status is PENDING or FAILED. Its status is "
                    + status.getValue() + ". So the conversion will start.");
                Executors.newSingleThreadExecutor().submit(() -> {
                    HashStoreConversionAdmin.convert();
                });
            }
        }
    }
}
