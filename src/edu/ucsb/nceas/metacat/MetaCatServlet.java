package edu.ucsb.nceas.metacat;


import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Timer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;

import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.index.queue.FailedIndexResubmitTimerTask;
import edu.ucsb.nceas.metacat.index.queue.IndexGenerator;
import edu.ucsb.nceas.metacat.index.queue.IndexGeneratorTimerTask;
import edu.ucsb.nceas.metacat.plugin.MetacatHandlerPluginManager;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.spatial.SpatialHarvester;
import edu.ucsb.nceas.metacat.startup.K8sAdminInitializer;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SessionData;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;

/**
 * A metadata catalog server implemented as a Java Servlet
 * All actions are disabled since Metacat 3.0.0
 */
public class MetaCatServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static Timer timer = null;
    private static boolean _firstHalfInitialized = false;
    private static boolean _fullyInitialized = false;
    private MetacatHandler handler = null;
    private static Log logMetacat = LogFactory.getLog(MetaCatServlet.class);

    // Constants -- these should be final in a servlet
    public static final String SCHEMALOCATIONKEYWORD = ":schemaLocation";
    public static final String NONAMESPACELOCATION = ":noNamespaceSchemaLocation";
    public static final String EML2KEYWORD = ":eml";
    private static final String FALSE = "false";
    private static final String TRUE  = "true";
    public static final String APPLICATION_NAME = "metacat";
    public static final String DEFAULT_ENCODING = "UTF-8";
    
    /**
     * Initialize the servlet by creating appropriate database connections
     */
    public void init(ServletConfig config) throws ServletException {
    
        try {
            if(_firstHalfInitialized) {
                return;
            }
            
            super.init(config);
            
            ServletContext context = config.getServletContext();
            context.setAttribute("APPLICATION_NAME", APPLICATION_NAME);
            
            ServiceService serviceService = ServiceService.getInstance(context);
            logMetacat.debug("MetaCatServlet.init - ServiceService singleton created "
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
                return;
            }
            
            _firstHalfInitialized = true;
            
            initSecondHalf(context);
            
        } catch (ServiceException se) {
            String errorMessage =
                "Service problem while intializing MetaCat Servlet: " + se.getMessage();
            logMetacat.error("MetaCatServlet.init - " + errorMessage);
            throw new ServletException(errorMessage);
        } catch (MetacatUtilException mue) {
            String errorMessage = "Metacat utility problem while intializing MetaCat Servlet: "
                + mue.getMessage();
            logMetacat.error("MetaCatServlet.init - " + errorMessage);
            throw new ServletException(errorMessage);
        }
    }


    /**
     * Initialize the remainder of the servlet. This is the part that can only
     * be initialized after metacat properties have been configured
     *
     * @param context
     *            the servlet context of MetaCatServlet
     */
    public void initSecondHalf(ServletContext context) throws ServletException {
        try {
            ServiceService.registerService("DatabaseService", DatabaseService.getInstance());
            
            // initialize DBConnection pool
            DBConnectionPool connPool = DBConnectionPool.getInstance();
            logMetacat.debug("MetaCatServlet.initSecondHalf - DBConnection pool initialized: "
                                        + connPool.toString());

            // Always check & auto-update DB and MN settings if running in Kubernetes
            if (Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"))) {
                K8sAdminInitializer.initializeK8sInstance();
            }

            // register the XML schema service
            ServiceService.registerService("XMLSchemaService", XMLSchemaService.getInstance());
            
            // check if eml201 document were corrected or not. if not, correct eml201 documents.
            // Before Metacat 1.8.1, metacat uses tag RELEASE_EML_2_0_1_UPDATE_6 as eml
            // schema, which accidentily points to wrong version of eml-resource.xsd.
            String correctedEML201Doc = PropertyService
                                                  .getProperty("document.eml201DocumentCorrected");
            if (correctedEML201Doc != null && correctedEML201Doc.equals(FALSE)) {
                logMetacat.info("MetaCatServlet.initSecondHalf: Start to correct eml201 documents");
                EML201DocumentCorrector correct = new EML201DocumentCorrector();
                boolean success = correct.run();
                if (success) {
                    PropertyService.setProperty("document.eml201DocumentCorrected", TRUE);
                }
                logMetacat.info("MetaCatServlet.initSecondHalf - "
                                                            + "Finish to correct eml201 documents");
            }

            /*
             * If spatial option is turned on and set to regenerate the spatial
             * cache on restart, trigger the harvester regeneratation method
             */
            if (PropertyService.getProperty("spatial.runSpatialOption").equals("true") &&
                  PropertyService.getProperty("spatial.regenerateCacheOnRestart").equals("true")) {

                // Begin timer
                long before = System.currentTimeMillis();

                // if either the point or polygon shape files do not exist, then regenerate the
                // entire spatial cache this may be expensive with many documents
                SpatialHarvester sh = new SpatialHarvester();
                sh.regenerate();
                sh.destroy();

                // After running the first time, we want to to set
                // regenerateCacheOnRestart to false
                // so that it does not regenerate the cache every time tomcat is
                // restarted
                PropertyService.setProperty("spatial.regenerateCacheOnRestart", "false");

                // End timer
                long after = System.currentTimeMillis();
                logMetacat.info("MetaCatServlet.initSecondHalf - Spatial Harvester Time "
                        + (after - before) + "ms");

            } else {
                logMetacat.info("MetaCatServlet.initSecondHalf - Spatial cache is not set to "
                                        + "regenerate on restart");
            }
        
            // Set up the replication log file by setting the "replication.logfile.name"
            // system property and reconfiguring the log4j property configurator.
            String replicationLogPath = PropertyService.getProperty("replication.logdir")
                + FileUtil.getFS() + ReplicationService.REPLICATION_LOG_FILE_NAME;
            
            if (FileUtil.getFileStatus(replicationLogPath) == FileUtil.DOES_NOT_EXIST) {
                FileUtil.createFile(replicationLogPath);
            }

            if (FileUtil.getFileStatus(replicationLogPath) < FileUtil.EXISTS_READ_WRITABLE) {
                logMetacat.error("MetaCatServlet.initSecondHalf - Replication log file: "
                                    + replicationLogPath + " does not exist read/writable.");
            }
            
            System.setProperty("replication.logfile.name", replicationLogPath);
            
            SessionService.getInstance().unRegisterAllSessions();
            
             //Initialize Metacat Handler
            timer = new Timer();
            handler = new MetacatHandler(timer);

            // Turn on sitemaps if appropriate
            initializeSitemapTask(handler);

            // initialize the plugins
            MetacatHandlerPluginManager.getInstance();

            // initialize the RabbitMQ service
            ServiceService.registerService("IndexQueueService", IndexGenerator.getInstance());

            // set up the time task to reindex objects (for the dataone api)
            startIndexReGenerator();
            _fullyInitialized = true;
            
            logMetacat.warn("MetaCatServlet.initSecondHalf - Metacat ("
                            + MetacatVersion.getVersionID() + ") initialized.");
            
        } catch (SQLException e) {
            String errorMessage = "SQL problem while intializing MetaCat Servlet: "
                    + e.getMessage();
            logMetacat.error("MetaCatServlet.initSecondHalf - " + errorMessage);
            throw new ServletException(errorMessage);
        } catch (IOException ie) {
            String errorMessage = "IO problem while intializing MetaCat Servlet: "
                    + ie.getMessage();
            logMetacat.error("MetaCatServlet.initSecondHalf - " + errorMessage);
            throw new ServletException(errorMessage);
        } catch (GeneralPropertyException gpe) {
            String errorMessage = "Could not retrieve property while intializing MetaCat Servlet: "
                    + gpe.getMessage();
            logMetacat.error("MetaCatServlet.initSecondHalf - " + errorMessage);
            throw new ServletException(errorMessage);
        } catch (ServiceException se) {
            String errorMessage = "Service problem while intializing MetaCat Servlet: "
                + se.getMessage();
            logMetacat.error("MetaCatServlet.initSecondHalf - " + errorMessage);
            throw new ServletException(errorMessage);
        } catch (UtilException ue) {
            String errorMessage = "Utility problem while intializing MetaCat Servlet: "
                + ue.getMessage();
            logMetacat.error("MetaCatServlet.initSecondHalf - " + errorMessage);
            throw new ServletException(errorMessage);
        }
    }

    /**
     * Close all db connections from the pool
     */
    public void destroy() {
        try {
            ServiceService.stopAllServices();
            logMetacat.warn("MetaCatServlet.destroy - Destroying MetacatServlet");
        } finally {
            timer.cancel();
            DBConnectionPool.release();
        }
    }
    
    /** Handle "GET" method requests from HTTP clients */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        
        // Process the data and send back the response
        handleGetOrPost(request, response);
    }
    
    /** Handle "POST" method requests from HTTP clients */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        
        // Process the data and send back the response
        handleGetOrPost(request, response);
    }
    
    /**
     * Control servlet response depending on the action parameter specified
     */
    @SuppressWarnings("unchecked")
    private void handleGetOrPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String requestEncoding = request.getCharacterEncoding();
        if (requestEncoding == null) {
            logMetacat.debug("null requestEncoding, setting to application default: "
                                + DEFAULT_ENCODING);
            request.setCharacterEncoding(DEFAULT_ENCODING);
        }
        logMetacat.debug("requestEncoding: " + requestEncoding);
        
        // Update the last update time for this user if they are not new
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            SessionService.getInstance().touchSession(httpSession.getId());
        }
        
        // Each time metacat is called, check to see if metacat has been
        // configured. If not then forward to the administration servlet
        if (!ConfigurationUtil.isMetacatConfigured()) {
            try {
                RequestUtil.forwardRequest(request, response, "/admin?action=configure", null);
                return;
            } catch (MetacatUtilException mue) {
                logMetacat.error("MetacatServlet.handleGetOrPost - utility error when forwarding "
                                    + "to configuration screen: " + mue.getMessage());
                throw new ServletException("MetacatServlet.handleGetOrPost - utility error when"
                        + " forwarding to configuration screen: " + mue.getMessage());
            }
        }

        // if we get here, metacat is configured.  If we have not completed the
        // second half of the initialization, do so now.  This allows us to initially
        // configure metacat without a restart.
        logMetacat.info("MetacatServlet.handleGetOrPost - the _fullyInitailzied value is "
                            + _fullyInitialized);
        if (!_fullyInitialized) {
            initSecondHalf(request.getSession().getServletContext());
        }
        
        // If all DBConnection in the pool are free and DBConnection pool
        // size is greater than initial value, shrink the connection pool
        // size to initial value
        DBConnectionPool.shrinkDBConnectionPoolSize();

        // Debug message to print out the method which have a busy DBConnection
        try {
            @SuppressWarnings("unused")
            DBConnectionPool pool = DBConnectionPool.getInstance();
//            pool.printMethodNameHavingBusyDBConnection();
        } catch (SQLException e) {
            logMetacat.error("MetaCatServlet.handleGetOrPost - Error in "
                                + "MetacatServlet.handleGetOrPost: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            String ctype = request.getContentType();
            
            if (ctype != null && ctype.startsWith("multipart/form-data")) {
                handler.handleMultipartForm(request, response);
                return;
            } 

            String name = null;
            String[] value = null;
            String[] docid = new String[3];
            Hashtable<String, String[]> params = new Hashtable<String, String[]>();

            // Check if this is a simple read request that doesn't use the
            // "action" syntax
            // These URLs are of the form:
            // http://localhost:8180/metacat/metacat/docid/skinname
            // e.g., http://localhost:8180/metacat/metacat/test.1.1/knb
            String pathInfo = request.getPathInfo();
            if (pathInfo != null) {
                // otherwise carry on as usual
                handler.handleReadAction(params, request, response, "public", null, null);
                return;
            }

            Enumeration<String> paramlist =
                (Enumeration<String>) request.getParameterNames();
            while (paramlist.hasMoreElements()) {

                name = paramlist.nextElement();
                value = request.getParameterValues(name);

                // Decode the docid and mouse click information
                // THIS IS OBSOLETE -- I THINK -- REMOVE THIS BLOCK
                // 4/12/2007d
                // MBJ
                if (name.endsWith(".y")) {
                    docid[0] = name.substring(0, name.length() - 2);
                    params.put("docid", docid);
                    name = "ypos";
                }
                if (name.endsWith(".x")) {
                    name = "xpos";
                }

                params.put(name, value);
            }

            // handle param is emptpy
            if (params.isEmpty() || params == null) {
                handler.sendNotSupportMessage(response);
                return;
            }

            // if the user clicked on the input images, decode which image
            // was clicked then set the action.
            if (params.get("action") == null) {
                handler.sendNotSupportMessage(response);
                return;
            }

            String action = (params.get("action"))[0];
            logMetacat.info("MetaCatServlet.handleGetOrPost - Action is: " + action);

            // This block handles session management for the servlet
            // by looking up the current session information for all actions
            // other than "login" and "logout"
            String userName = null;
            String password = null;
            String[] groupNames = null;
            String sessionId = null;
            name = null;

            // handle login action
            if (action.equals("login")) {
                handler.handleLoginAction(params, request, response);
                // handle logout action
            } else if (action.equals("logout")) {
                handler.handleLogoutAction(params, request, response);
                // handle session validate request
            } else if (action.equals("validatesession")) {
                 handler.sendNotSupportMessage(response);
                // aware of session expiration on every request
            } else {
                SessionData sessionData = RequestUtil.getSessionData(request);
                
                if (sessionData != null) {
                    userName = sessionData.getUserName();
                    password = sessionData.getPassword();
                    groupNames = sessionData.getGroupNames();
                    sessionId = sessionData.getId();
                }

                logMetacat.info("MetaCatServlet.handleGetOrPost - The user is : " + userName);
            }
            
            // Now that we know the session is valid, we can delegate the
            // request to a particular action handler
            if (action.equals("query")) {
                handler.handleQuery(params, response, userName, groupNames, sessionId);
            } else if (action.equals("squery")) {
                handler.handleSQuery(params, response, userName, groupNames, sessionId);
            } else if (action.trim().equals("spatial_query")) {
                handler.handleSpatialQuery(params, response, userName, groupNames, sessionId);
            } else if (action.trim().equals("dataquery")) {
                handler.handleDataquery(params, response, sessionId);
            } else if (action.trim().equals("editcart")) {
                handler.handleEditCart(params, response, sessionId);
            } else if (action.equals("export")) {

                handler.handleExportAction(params, response, userName, groupNames, password);
            } else if (action.equals("read")) {
                handler.handleReadAction(params, request, response, userName, password,
                            groupNames);
                
            } else if (action.equals("readinlinedata")) {
                handler.handleReadInlineDataAction(params, request, response, userName, password,
                        groupNames);
            } else if (action.equals("insert") || action.equals("update")) {
               handler.sendNotSupportMessage(response);
            } else if (action.equals("delete")) {
                    handler.handleDeleteAction(params, request, response, userName,
                            groupNames);
            } else if (action.equals("validate")) {
                handler.handleValidateAction(response, params);
            } else if (action.equals("setaccess")) {
                handler.handleSetAccessAction(params, userName, request, response);
            } else if (action.equals("getaccesscontrol")) {
                handler.handleGetAccessControlAction(params, response, userName, groupNames);
            } else if (action.equals("isauthorized")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("getprincipals")) {
                handler.handleGetPrincipalsAction(response, userName, password);
            } else if (action.equals("getdoctypes")) {
                handler.handleGetDoctypesAction(params, response);
            } else if (action.equals("getdtdschema")) {
                handler.handleGetDTDSchemaAction(params, response);
            } else if (action.equals("getdocid")) {
                handler.handleGetDocid(params, response);
            } else if (action.equals("getlastdocid")) {
                handler.handleGetMaxDocidAction(params, response);
            } else if (action.equals("getalldocids")) {
                handler.handleGetAllDocidsAction(params, response);
            } else if (action.equals("isregistered")) {
                handler.handleIdIsRegisteredAction(params, response);
            } else if (action.equals("getrevisionanddoctype")) {
                handler.handleGetRevisionAndDocTypeAction(response, params);
            } else if (action.equals("getversion")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("getlog")) {
                handler.handleGetLogAction(params, request, response, userName,
                                                    groupNames, sessionId);
            } else if (action.equals("getloggedinuserinfo")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("buildindex")) {
                handler.handleBuildIndexAction(params, request, response, userName, groupNames);
            } else if (action.equals("reindex")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("reindexall")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("refreshServices")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("shrink")) {
                handler.sendNotSupportMessage(response);
            } else {
                handler.sendNotSupportMessage(response);
            }
        } catch (PropertyNotFoundException pnfe) {
            String errorString = "Critical property not found: " + pnfe.getMessage();
            logMetacat.error("MetaCatServlet.handleGetOrPost - " + errorString);
            throw new ServletException(errorString);
        } 
    }
    
    /**
     * Reports whether the MetaCatServlet has been fully initialized
     * 
     * @return true if fully intialized, false otherwise
     */
    public static boolean isFullyInitialized() {
        return _fullyInitialized;
    }
    
    public static boolean isReadOnly(HttpServletResponse response) throws IOException {
        boolean readOnly = false;
        ReadOnlyChecker checker = new ReadOnlyChecker();
        readOnly = checker.isReadOnly();
        if(readOnly) {
            PrintWriter out = response.getWriter();
            response.setContentType("text/xml");
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println("Metacat is in read-only mode and your request can't be fulfilled. "
                                + "Please try again later.");
            out.println("</error>");
            out.close();
        }
        return readOnly;
        }
        
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
        
        
        /**
         * Start to re-generate indexes for those haven't been indexed in another thread.
         * It will create a timer to run this task periodically.
         * If the property of "index.regenerate.interval" is less than 0, the thread would NOT run.
         */
        private static void startIndexReGenerator() {
            boolean regenerateIndex = false;
            try {
                regenerateIndex = (new Boolean(PropertyService
                                             .getProperty("index.regenerate.sincelastProcessDate")))
                                                .booleanValue();
            } catch (PropertyNotFoundException e) {
                logMetacat.debug("MetacatServlet.startIndexGenerate - the property "
                                  + "index.regenerate.sincelastProcessDate"
                                  + " is not found and Metacat will use " + regenerateIndex
                                  + " as the default one.");
            }
            long period = 86400000;//milliseconds
            try {
                period = (new Long(PropertyService
                                              .getProperty("index.regenerate.interval")))
                                                .longValue();
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
                regneratedFailedIndex = (new Boolean(PropertyService
                                               .getProperty("index.regenerate.failedObject")))
                                                 .booleanValue();
            } catch (PropertyNotFoundException e) {
                logMetacat.debug("MetacatServlet.startIndexGenerate - the property "
                        + "index.regenerate.failedObject"
                        + " is not found and Metacat will use "+ regneratedFailedIndex
                        + " as the default one.");
            }
            long delay = 1200000;
            try {
                delay = (new Long(PropertyService
                                    .getProperty("index.regenerate.failedTask.delay"))).longValue();
            } catch (PropertyNotFoundException | NumberFormatException e) {
                logMetacat.debug("MetacatServlet.startIndexGenerate - the property "
                        + "index.regenerate.failedTask.delay"
                       + " is not found and Metacat will use " + delay + " ms as the default one.");
            }
            long failedInterval = 3600000;
            try {
                failedInterval = (new Long(PropertyService
                                        .getProperty("index.regenerate.failedTask.interval")))
                                        .longValue();
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
         * Determine the time to run the regenerating thread first. 
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
}
