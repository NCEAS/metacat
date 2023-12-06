package edu.ucsb.nceas.metacat.startup;

import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.index.queue.IndexGenerator;
import edu.ucsb.nceas.metacat.plugin.MetacatHandlerPluginManager;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;


/**
 * A class to initialize Metacat
 * @author tao
 *
 */
public class MetacatInitializer {
    public static final String APPLICATION_NAME = "metacat";
    private static Log logMetacat = LogFactory.getLog(MetacatInitializer.class);

    /**
     * Initialize Metacat
     * @param context  the ServletContext object will be used in Metacat
     * @param handler  the MetacatHandler object will be used in Metacat
     * @return true if it is fully initialized; false otherwise.
     * @throws ServletException
     */
    public static boolean init(ServletContext context, MetacatHandler handler)
                                                                        throws ServletException {
        boolean fullInit = false;
        try {
            context.setAttribute("APPLICATION_NAME", APPLICATION_NAME);
            ServiceService serviceService = ServiceService.getInstance(context);
            logMetacat.debug("MetacatInitializer.init - ServiceService singleton created "
                                    + serviceService);
            // Initialize the properties file
            String dirPath = ServiceService.getRealConfigDir();
            // Register preliminary services
            ServiceService.registerService("PropertyService", PropertyService.getInstance(context));
            ServiceService.registerService("SkinPropertyService",
                                                                SkinPropertyService.getInstance());
            ServiceService.registerService("SessionService", SessionService.getInstance());
            // Check to see if the user has requested to bypass configuration
            // (dev option) and check see if metacat has been configured.
            // If both are false then stop the initialization
            if (!ConfigurationUtil.bypassConfiguration() &&
                                    !ConfigurationUtil.isMetacatConfigured()) {
                return fullInit;
            }
            initAfterMetacatConfig(context,handler);
            fullInit = true;
            return fullInit;
        } catch (ServiceException se) {
            String errorMessage =
                "Service problem while intializing MetaCat Servlet: " + se.getMessage();
            logMetacat.error("MetacatInitializer.init - " + errorMessage);
            throw new ServletException(errorMessage);
        } catch (MetacatUtilException mue) {
            String errorMessage = "Metacat utility problem while intializing MetaCat Servlet: "
                + mue.getMessage();
            logMetacat.error("MetacatInitializer.init - " + errorMessage);
            throw new ServletException(errorMessage);
        }
    }
    
    /**
     * Initialize the remainder of the servlet. This is the part that can only
     * be initialized after metacat properties have been configured
     *
     * @param context  the servlet context of MetaCatServlet
     * @param handler   the MetacatHandler object which will be used in Metacat
     */
    public static void initAfterMetacatConfig(ServletContext context, MetacatHandler handler)
                                                                    throws ServletException {
        try {
            ServiceService.registerService("DatabaseService", DatabaseService.getInstance());
            // initialize DBConnection pool
            DBConnectionPool connPool = DBConnectionPool.getInstance();
            logMetacat.debug("MetacatInitializer.initAfterMetacatConfig - DBConnection "
                                + "pool initialized: " + connPool.toString());

            // Always check & auto-update DB and MN settings if running in Kubernetes
            if (Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"))) {
                K8sAdminInitializer.initializeK8sInstance();
            }

            // register the XML schema service
            ServiceService.registerService("XMLSchemaService", XMLSchemaService.getInstance());

            // Set up the replication log file by setting the "replication.logfile.name"
            // system property and reconfiguring the log4j property configurator.
            String replicationLogPath = PropertyService.getProperty("replication.logdir")
                + FileUtil.getFS() + ReplicationService.REPLICATION_LOG_FILE_NAME;
            
            if (FileUtil.getFileStatus(replicationLogPath) == FileUtil.DOES_NOT_EXIST) {
                FileUtil.createFile(replicationLogPath);
            }

            if (FileUtil.getFileStatus(replicationLogPath) < FileUtil.EXISTS_READ_WRITABLE) {
                logMetacat.error("MetacatInitializer.initAfterMetacatConfig- Replication log file: "
                                    + replicationLogPath + " does not exist read/writable.");
            }

            System.setProperty("replication.logfile.name", replicationLogPath);

            SessionService.getInstance().unRegisterAllSessions();

            // Turn on sitemaps if appropriate
            initializeSitemapTask(handler);

            // initialize the plugins
            MetacatHandlerPluginManager.getInstance();

            // initialize the RabbitMQ service
            ServiceService.registerService("IndexQueueService", IndexGenerator.getInstance());

            // set up the time task to reindex objects (for the dataone api)
            handler.startIndexReGenerator();

            logMetacat.info("MetacatInitializer.initAfterMetacatConfig - Metacat ("
                            + MetacatVersion.getVersionID() + ") initialized.");
        } catch (SQLException e) {
            String errorMessage = "SQL problem while intializing MetaCat Servlet: "
                    + e.getMessage();
            logMetacat.error("MetacatInitializer.initAfterMetacatConfig - " + errorMessage);
            throw new ServletException(errorMessage);
        } catch (GeneralPropertyException gpe) {
            String errorMessage = "Could not retrieve property while intializing MetaCat Servlet: "
                    + gpe.getMessage();
            logMetacat.error("MetacatInitializer.initAfterMetacatConfig - " + errorMessage);
            throw new ServletException(errorMessage);
        } catch (ServiceException se) {
            String errorMessage = "Service problem while intializing MetaCat Servlet: "
                + se.getMessage();
            logMetacat.error("MetacatInitializer.initAfterMetacatConfig - " + errorMessage);
            throw new ServletException(errorMessage);
        } catch (UtilException ue) {
            String errorMessage = "Utility problem while intializing MetaCat Servlet: "
                + ue.getMessage();
            logMetacat.error("MetacatInitializer.initAfterMetacatConfig - " + errorMessage);
            throw new ServletException(errorMessage);
        }
    }


    /**
     * Schedule a site map task
     * @param handler  the MetacatHandler object will be in the site map task
     */
    public static void initializeSitemapTask(MetacatHandler handler) {
        Boolean sitemap_enabled = false;
        try {
            sitemap_enabled = Boolean
                                .parseBoolean(PropertyService.getProperty("sitemap.enabled"));
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.info("sitemap.enabled property not found so sitemaps are disabled");
        }
        // Schedule the sitemap generator to run periodically
        handler.scheduleSitemapGeneration();
    }

}
