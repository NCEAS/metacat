/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements a metadata catalog as a java Servlet
 *  Copyright: 2006 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones, Dan Higgins, Jivka Bojilova, Chad Berkley, Matthew Perry
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

package edu.ucsb.nceas.metacat;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import edu.ucsb.nceas.metacat.common.query.EnabledQueryEngines;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.plugin.MetacatHandlerPlugin;
import edu.ucsb.nceas.metacat.plugin.MetacatHandlerPluginManager;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.BaseException;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.spatial.SpatialHarvester;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.ErrorSendingErrorException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.ResponseUtil;
import edu.ucsb.nceas.metacat.util.SessionData;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.metacat.workflow.WorkflowSchedulerClient;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;

/**
 * A metadata catalog server implemented as a Java Servlet
 *
 * Valid actions are:
 * 
 * action=login
 *     username
 *     password
 *     qformat
 * action=logout
 *     qformat
 * action=query -- query the values of all elements and attributes and return a result set of nodes
 *     meta_file_id --
 *     returndoctype --
 *     filterdoctype --
 *     returnfield --
 *     owner --
 *     site --
 *     operator --
 *     casesensitive --
 *     searchmode --
 *     anyfield --
 * action=spatial_query -- run a spatial query.  these queries may include any of the
 *                         queries supported by the WFS / WMS standards
 *     xmax --
 *     ymax --
 *     xmin --
 *     ymin --
 *     skin --
 *     pagesize --
 *     pagestart --
 * action=squery -- structured query (see pathquery.dtd)
 *     query --
 *     pagesize --
 *     pagestart --
 * action=export -- export a zip format for data packadge
 *     docid -- 
 * action=read -- read any metadata/data file from Metacat and from Internet
 *     archiveEntryName --
 *     docid --
 *     qformat --
 *     metadatadocid --
 * action=readinlinedata -- read inline data only
 *     inlinedataid
 * action=insert -- insert an XML document into the database store
 *     qformat -- 
 *     docid --
 *     doctext --
 *     dtdtext --
 * action=insertmultipart -- insert an xml document into the database using multipart encoding
 *     qformat -- 
 *     docid --
 * action=update -- update an XML document that is in the database store
 *     qformat -- 
 *     docid --
 *     doctext --
 *     dtdtext --
 * action=delete -- delete an XML document from the database store
 *     docid --
 * action=validate -- validate the xml contained in valtext
 *     valtext --
 *     docid --
 * action=setaccess -- change access permissions for a user on a document.
 *     docid --
 *     principal --
 *     permission --
 *     permType --
 *     permOrder --
 * action=getaccesscontrol -- retrieve acl info for Metacat document
 *     docid -- 
 * action=getprincipals -- retrieve a list of principals in XML
 * action=getalldocids -- retrieves a list of all docids registered with the system
 *     scope --
 * action=getlastdocid --
 *     scope --
 *     username --
 * action=isregistered -- checks to see if the provided docid is registered
 *     docid --
 * action=getrevisionanddoctype -- get a document's revision and doctype from database 
 *     docid --
 * action=getversion -- 
 * action=getdoctypes -- retrieve all doctypes (publicID) 
 * action=getdtdschema -- retrieve a DTD or Schema file
 *     doctype --
 * action=getlog -- get a report of events that have occurred in the system
 *     ipAddress --  filter on one or more IP addresses>
 *     principal -- filter on one or more principals (LDAP DN syntax)
 *     docid -- filter on one or more document identifiers (with revision)
 *     event -- filter on event type (e.g., read, insert, update, delete)
 *     start -- filter out events before the start date-time
 *     end -- filter out events before the end date-time
 * action=getloggedinuserinfo -- get user info for the currently logged in user
 *     ipAddress --  filter on one or more IP addresses>
 *     principal -- filter on one or more principals (LDAP DN syntax)
 *     docid -- filter on one or more document identifiers (with revision)
 *     event -- filter on event type (e.g., read, insert, update, delete)
 *     start -- filter out events before the start date-time
 *     end -- filter out events before the end date-time
 * action=shrink -- Shrink the database connection pool size if it has grown and 
 *                  extra connections are no longer being used.
 * action=buildindex --
 *     docid --
 * action=refreshServices --
 * action=scheduleWorkflow -- Schedule a workflow to be run.  Scheduling a workflow 
 *                            registers it with the scheduling engine and creates a row
 *                            in the scheduled_job table.  Note that this may be 
 *                            extracted into a separate servlet.
 *     delay -- The amount of time from now before the workflow should be run.  The 
 *              delay can be expressed in number of seconds, minutes, hours and days, 
 *              for instance 30s, 2h, etc.
 *     starttime -- The time that the workflow should first run.  If both are provided
 *                  this takes precedence over delay.  The time should be expressed as: 
 *                  MM/dd/yyyy HH:mm:ss with the timezone assumed to be that of the OS.
 *     endtime -- The time when the workflow should end. The time should be expressed as: 
 *                  MM/dd/yyyy HH:mm:ss with the timezone assumed to be that of the OS.
 *     intervalvalue -- The numeric value of the interval between runs
 *     intervalunit -- The unit of the interval between runs.  Can be s, m, h, d for 
 *                     seconds, minutes, hours and days respectively
 *     workflowid -- The lsid of the workflow that we want to schedule.  This workflow
 *                   must already exist in the database.
 *     karid -- The karid for the workflow that we want to schedule.
 *     workflowname -- The name of the workflow.
 *     forwardto -- If provided, forward to this page when processing is done.
 *     qformat -- If provided, render results using the stylesheets associated with
 *                this skin.  Default is xml.
 * action=unscheduleWorkflow -- Unschedule a workflow.  Unscheduling a workflow 
 *                            removes it from the scheduling engine and changes the 
 *                            status in the scheduled_job table to " unscheduled.  Note 
 *                            that this may be extracted into a separate servlet.
 *     workflowjobname -- The job ID for the workflow run that we want to unschedule.  This
 *                      is held in the database as scheduled_job.name
 *     forwardto -- If provided, forward to this page when processing is done.
 *     qformat -- If provided, render results using the stylesheets associated with
 *                this skin.  Default is xml.
 * action=rescheduleWorkflow -- Unschedule a workflow.  Rescheduling a workflow 
 *                            registers it with the scheduling engine and changes the 
 *                            status in the scheduled_job table to " scheduled.  Note 
 *                            that this may be extracted into a separate servlet.
 *     workflowjobname -- The job ID for the workflow run that we want to reschedule.  This
 *                      is held in the database as scheduled_job.name
 *     forwardto -- If provided, forward to this page when processing is done.
 *     qformat -- If provided, render results using the stylesheets associated with
 *                this skin.  Default is xml.
 * action=deleteScheduledWorkflow -- Delete a workflow.  Deleting a workflow 
 *                            removes it from the scheduling engine and changes the 
 *                            status in the scheduled_job table to " deleted.  Note 
 *                            that this may be extracted into a separate servlet.
 *     workflowjobname -- The job ID for the workflow run that we want to delete.  This
 *                      is held in the database as scheduled_job.name
 *     forwardto -- If provided, forward to this page when processing is done.
 *     qformat -- If provided, render results using the stylesheets associated with
 *                this skin.  Default is xml.
 * action=reindex -- rebuild the solr index for the specified pids.
 *     pid -- the id of the document which will be rebuilt slor index.
 * action=reindexall -- rebuild the solr index for all objects in the systemmetadata table.
 *     
 * Here are some of the common parameters for actions
 *     doctype -- document type list returned by the query (publicID) 
 *     qformat=xml -- display resultset from query in XML 
 *     qformat=html -- display resultset from query in HTML 
 *     qformat=zip -- zip resultset from query
 *     docid=34 -- display the document with the document ID number 34 
 *     doctext -- XML text of the document to load into the database 
 *     acltext -- XML access text for a document to load into the database 
 *     dtdtext -- XML DTD text for a new DTD to load into Metacat XML Catalog 
 *     query -- actual query text (to go with 'action=query' or 'action=squery')
 *     valtext -- XML text to be validated 
 *     scope --can limit the query by the scope of the id
 *     docid --the docid to check
 *     datadoc -- data document name (id)
 */
public class MetaCatServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private Timer timer = null;
    private static boolean _firstHalfInitialized = false;
    private static boolean _fullyInitialized = false;
    private MetacatHandler handler = null;
    
    // Constants -- these should be final in a servlet
    public static final String SCHEMALOCATIONKEYWORD = ":schemaLocation";
    public static final String NONAMESPACELOCATION = ":noNamespaceSchemaLocation";
    public static final String EML2KEYWORD = ":eml";
    private static final String FALSE = "false";
    private static final String TRUE  = "true";
    private static String LOG_CONFIG_NAME = null;
    public static final String APPLICATION_NAME = "metacat";
	public static final String DEFAULT_ENCODING = "UTF-8";
    
    /**
     * Initialize the servlet by creating appropriate database connections
     */
    public void init(ServletConfig config) throws ServletException {
    	Logger logMetacat = Logger.getLogger(MetaCatServlet.class);
    	try {
    		if(_firstHalfInitialized) {
    			return;
    		}
    		
            super.init(config);
            
            ServletContext context = config.getServletContext();
            context.setAttribute("APPLICATION_NAME", APPLICATION_NAME);
            
            ServiceService serviceService = ServiceService.getInstance(context);
            logMetacat.debug("MetaCatServlet.init - ServiceService singleton created " + serviceService);
            
            // Initialize the properties file
            String dirPath = ServiceService.getRealConfigDir();
            
            LOG_CONFIG_NAME = dirPath + "/log4j.properties";
            PropertyConfigurator.configureAndWatch(LOG_CONFIG_NAME);
            
            // Register preliminary services
            ServiceService.registerService("PropertyService", PropertyService.getInstance(context));         
            ServiceService.registerService("SkinPropertyService", SkinPropertyService.getInstance());
            ServiceService.registerService("SessionService", SessionService.getInstance()); 
    		
            // Check to see if the user has requested to bypass configuration 
            // (dev option) and check see if metacat has been configured.
    		// If both are false then stop the initialization
            if (!ConfigurationUtil.bypassConfiguration() && !ConfigurationUtil.isMetacatConfigured()) {
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
		
		Logger logMetacat = Logger.getLogger(MetaCatServlet.class);

		try {			
			ServiceService.registerService("DatabaseService", DatabaseService.getInstance());
			
			// initialize DBConnection pool
			DBConnectionPool connPool = DBConnectionPool.getInstance();
			logMetacat.debug("MetaCatServlet.initSecondHalf - DBConnection pool initialized: " + connPool.toString());
			
			// register the XML schema service
			ServiceService.registerService("XMLSchemaService", XMLSchemaService.getInstance());
			
			// check if eml201 document were corrected or not. if not, correct eml201 documents.
			// Before Metacat 1.8.1, metacat uses tag RELEASE_EML_2_0_1_UPDATE_6 as eml
			// schema, which accidentily points to wrong version of eml-resource.xsd.
			String correctedEML201Doc = PropertyService.getProperty("document.eml201DocumentCorrected");
			if (correctedEML201Doc != null && correctedEML201Doc.equals(FALSE)) {
				logMetacat.info("MetaCatServlet.initSecondHalf - Start to correct eml201 documents");
				EML201DocumentCorrector correct = new EML201DocumentCorrector();
				boolean success = correct.run();
				if (success) {
					PropertyService.setProperty("document.eml201DocumentCorrected", TRUE);
				}
				logMetacat.info("MetaCatServlet.initSecondHalf - Finish to correct eml201 documents");
			}

			// Index the paths specified in the metacat.properties
			checkIndexPaths();

			// initiate the indexing Queue
			IndexingQueue.getInstance();

			// start the IndexingThread if indexingTimerTaskTime more than 0.
			// It will index all the documents not yet indexed in the database
			int indexingTimerTaskTime = Integer.parseInt(PropertyService
					.getProperty("database.indexingTimerTaskTime"));
			int delayTime = Integer.parseInt(PropertyService
					.getProperty("database.indexingInitialDelay"));

			if (indexingTimerTaskTime > 0) {
				timer = new Timer();
				timer.schedule(new IndexingTimerTask(), delayTime, indexingTimerTaskTime);
			}
			
			/*
			 * If spatial option is turned on and set to regenerate the spatial
			 * cache on restart, trigger the harvester regeneratation method
			 */
			if (PropertyService.getProperty("spatial.runSpatialOption").equals("true") && 
					PropertyService.getProperty("spatial.regenerateCacheOnRestart").equals("true")) {

				// Begin timer
				long before = System.currentTimeMillis();

				// if either the point or polygon shape files do not exist, then regenerate the entire spatial cache
				// this may be expensive with many documents		
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
				logMetacat.info("MetaCatServlet.initSecondHalf - Spatial Harvester Time  " 
						+ (after - before) + "ms");

			} else {
				logMetacat.info("MetaCatServlet.initSecondHalf - Spatial cache is not set to regenerate on restart");
			}
		
			// Set up the replication log file by setting the "replication.logfile.name" 
			// system property and reconfiguring the log4j property configurator.
			String replicationLogPath = PropertyService.getProperty("replication.logdir") 
				+ FileUtil.getFS() + ReplicationService.REPLICATION_LOG_FILE_NAME;				
			
			if (FileUtil.getFileStatus(replicationLogPath) == FileUtil.DOES_NOT_EXIST) {
				FileUtil.createFile(replicationLogPath);
			}

			if (FileUtil.getFileStatus(replicationLogPath) < FileUtil.EXISTS_READ_WRITABLE) {
				logMetacat.error("MetaCatServlet.initSecondHalf - Replication log file: " + replicationLogPath 
						+ " does not exist read/writable.");
			}
			
			System.setProperty("replication.logfile.name", replicationLogPath);			
			PropertyConfigurator.configureAndWatch(LOG_CONFIG_NAME);
			
			SessionService.getInstance().unRegisterAllSessions();
			
	         //Initialize Metacat Handler
            handler = new MetacatHandler(timer);

			handler.set_sitemapScheduled(false);
			
			// initialize the plugins
			MetacatHandlerPluginManager.getInstance();
			
			// initialize the HazelcastService
			ServiceService.registerService("HazelcastService", HazelcastService.getInstance());

			_fullyInitialized = true;
			
			logMetacat.warn("MetaCatServlet.initSecondHalf - Metacat (" + MetacatVersion.getVersionID()
					+ ") initialized.");
			
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
    	Logger logMetacat = Logger.getLogger(MetaCatServlet.class);
    	
    	ServiceService.stopAllServices();
    	
        // Close all db connection
        logMetacat.warn("MetaCatServlet.destroy - Destroying MetacatServlet");
        timer.cancel();
        IndexingQueue.getInstance().setMetacatRunning(false);
        DBConnectionPool.release();
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
	 * Index the paths specified in the metacat.properties
	 */
    private void checkIndexPaths() {
    	Logger logMetacat = Logger.getLogger(MetaCatServlet.class);
    	logMetacat.debug("MetaCatServlet.checkIndexPaths - starting....");
    	if(!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.PATHQUERYENGINE)) {
    		logMetacat.info("MetaCatServlet.checkIndexPaths - the pathquery is disabled and it does nothing for checking path_index");
            return;
        }
    	logMetacat.debug("MetaCatServlet.checkIndexPaths - after checking is the pathquery enabled or not...");
        

        Vector<String> pathsForIndexing = null;
        try {  
        	pathsForIndexing = SystemUtil.getPathsForIndexing();
        }
        catch (MetacatUtilException ue) {
        	pathsForIndexing = null;
            logMetacat.error("MetaCatServlet.checkIndexPaths - not find index paths.  Setting " 
            		+ "pathsForIndexing to null: " + ue.getMessage());
        }
        
        if (pathsForIndexing != null && !pathsForIndexing.isEmpty()) {
            
            logMetacat.debug("MetaCatServlet.checkIndexPaths - Indexing paths specified in metacat.properties....");
            
            DBConnection conn = null;
            int serialNumber = -1;
            PreparedStatement pstmt = null;
            PreparedStatement pstmt1 = null;
            ResultSet rs = null;
            
            for (String pathIndex : pathsForIndexing) {
                logMetacat.debug("MetaCatServlet.checkIndexPaths - Checking if '" + pathIndex  + "' is indexed.... ");
                
                try {
                    //check out DBConnection
                    conn = DBConnectionPool.
                            getDBConnection("MetaCatServlet.checkIndexPaths");
                    serialNumber = conn.getCheckOutSerialNumber();
                    
                    pstmt = conn.prepareStatement(
                            "SELECT * FROM xml_path_index " + "WHERE path = ?");
                    pstmt.setString(1, pathIndex);
                    
                    pstmt.execute();
                    rs = pstmt.getResultSet();
                    
                    if (!rs.next()) {
                        logMetacat.debug("MetaCatServlet.checkIndexPaths - not indexed yet.");
                        rs.close();
                        pstmt.close();
                        conn.increaseUsageCount(1);
                        
                        logMetacat.debug("MetaCatServlet.checkIndexPaths - Inserting following path in xml_path_index: "
                                + pathIndex);
                        if(pathIndex.indexOf("@")<0){
                            pstmt = conn.prepareStatement("SELECT DISTINCT n.docid, "
                                    + "n.nodedata, n.nodedatanumerical, n.nodedatadate, n.parentnodeid"
                                    + " FROM xml_nodes n, xml_index i WHERE"
                                    + " i.path = ? and n.parentnodeid=i.nodeid and"
                                    + " n.nodetype LIKE 'TEXT' order by n.parentnodeid");
                        } else {
                            pstmt = conn.prepareStatement("SELECT DISTINCT n.docid, "
                                    + "n.nodedata, n.nodedatanumerical, n.nodedatadate, n.parentnodeid"
                                    + " FROM xml_nodes n, xml_index i WHERE"
                                    + " i.path = ? and n.nodeid=i.nodeid and"
                                    + " n.nodetype LIKE 'ATTRIBUTE' order by n.parentnodeid");
                        }
                        pstmt.setString(1, pathIndex);
                        pstmt.execute();
                        rs = pstmt.getResultSet();
                        
                        int count = 0;
                        logMetacat.debug("MetaCatServlet.checkIndexPaths - Executed the select statement for: "
                                + pathIndex);
                        
                        try {
                            while (rs.next()) {
                                
                                String docid = rs.getString(1);
                                String nodedata = rs.getString(2);
                                float nodedatanumerical = rs.getFloat(3);
                                Timestamp nodedatadate = rs.getTimestamp(4);
                                int parentnodeid = rs.getInt(5);
                                
                                if (!nodedata.trim().equals("")) {
                                    pstmt1 = conn.prepareStatement(
                                            "INSERT INTO xml_path_index"
                                            + " (docid, path, nodedata, "
                                            + "nodedatanumerical, nodedatadate, parentnodeid)"
                                            + " VALUES (?, ?, ?, ?, ?, ?)");
                                    
                                    pstmt1.setString(1, docid);
                                    pstmt1.setString(2, pathIndex);
                                    pstmt1.setString(3, nodedata);
                                    pstmt1.setFloat(4, nodedatanumerical);
                                    pstmt1.setTimestamp(5, nodedatadate);
                                    pstmt1.setInt(6, parentnodeid);
                                    
                                    pstmt1.execute();
                                    pstmt1.close();
                                    
                                    count++;
                                    
                                }
                            }
                        } catch (Exception e) {
                            logMetacat.error("MetaCatServlet.checkIndexPaths - Exception:" + e.getMessage());
                            e.printStackTrace();
                        }
                        
                        rs.close();
                        pstmt.close();
                        conn.increaseUsageCount(1);
                        
                        logMetacat.info("MetaCatServlet.checkIndexPaths - Indexed " + count + " records from xml_nodes for '"
                                + pathIndex + "'");
                        
                    } else {
                        logMetacat.debug("MetaCatServlet.checkIndexPaths - already indexed.");
                    }
                    
                    rs.close();
                    pstmt.close();
                    conn.increaseUsageCount(1);
                    
                } catch (Exception e) {
                    logMetacat.error("MetaCatServlet.checkIndexPaths - Error in MetaCatServlet.checkIndexPaths: "
                            + e.getMessage());
                }finally {
                    //check in DBonnection
                    DBConnectionPool.returnDBConnection(conn, serialNumber);
                }
                
                
            }
            
            logMetacat.debug("MetaCatServlet.checkIndexPaths - Path Indexing Completed");
        }
    }
    
    /**
	 * Control servlet response depending on the action parameter specified
	 */
	@SuppressWarnings("unchecked")
	private void handleGetOrPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Logger logMetacat = Logger.getLogger(MetaCatServlet.class);

		String requestEncoding = request.getCharacterEncoding();
		if (requestEncoding == null) {
			logMetacat.debug("null requestEncoding, setting to application default: " + DEFAULT_ENCODING);
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
				logMetacat.error("MetacatServlet.handleGetOrPost - utility error when forwarding to " + 
						"configuration screen: " + mue.getMessage());
				throw new ServletException("MetacatServlet.handleGetOrPost - utility error when forwarding to " + 
						"configuration screen: " + mue.getMessage());
			}
		}

		// if we get here, metacat is configured.  If we have not completed the 
		// second half of the initialization, do so now.  This allows us to initially
		// configure metacat without a restart.
		if (!_fullyInitialized) {
			initSecondHalf(request.getSession().getServletContext());
		}
		
		/*
		 * logMetacat.debug("Connection pool size: "
		 * +connPool.getSizeOfDBConnectionPool(),10); logMetacat.debug("Free
		 * DBConnection number: "
		 */
		// If all DBConnection in the pool are free and DBConnection pool
		// size is greater than initial value, shrink the connection pool
		// size to initial value
		DBConnectionPool.shrinkDBConnectionPoolSize();

		// Debug message to print out the method which have a busy DBConnection
		try {
			@SuppressWarnings("unused")
			DBConnectionPool pool = DBConnectionPool.getInstance();
//			pool.printMethodNameHavingBusyDBConnection();
		} catch (SQLException e) {
			logMetacat.error("MetaCatServlet.handleGetOrPost - Error in MetacatServlet.handleGetOrPost: " + e.getMessage());
			e.printStackTrace();
		}

		try {
			String ctype = request.getContentType();
			
			if (ctype != null && ctype.startsWith("multipart/form-data")) {
			    if(isReadOnly(response)) {
			        return;
			    }
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
				String[] path = pathInfo.split("/");
				if (path.length > 1) {
					String docidToRead = path[1];
					String docs[] = new String[1];
					docs[0] = docidToRead;
					logMetacat.debug("MetaCatServlet.handleGetOrPost - READING DOCID FROM PATHINFO: " + docs[0]);
					params.put("docid", docs);
					String skin = null;
					if (path.length > 2) {
						skin = path[2];
						String skins[] = new String[1];
						skins[0] = skin;
						params.put("qformat", skins);
					}
					
					// attempt to redirect to metacatui (#view/{pid}) if not getting the raw XML
					// see: https://projects.ecoinformatics.org/ecoinfo/issues/6546
					if (!skin.equals("xml")) {
						String uiContext = PropertyService.getProperty("ui.context");
						String docidNoRev = DocumentUtil.getDocIdFromAccessionNumber(docidToRead);
						int rev = DocumentUtil.getRevisionFromAccessionNumber(docidToRead);
						String pid = null;
						try {
							pid = IdentifierManager.getInstance().getGUID(docidNoRev, rev);
							response.sendRedirect(SystemUtil.getServerURL() + "/" + uiContext + "/#view/" + pid );
							return;
						} catch (McdbDocNotFoundException nfe) {
							logMetacat.warn("Could not locate PID for docid: " + docidToRead, nfe);
						}
					}
					
					// otherwise carry on as usual
					handler.handleReadAction(params, request, response, "public", null, null);
					return;
				}
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
				return;
			}

			// if the user clicked on the input images, decode which image
			// was clicked then set the action.
			if (params.get("action") == null) {
				PrintWriter out = response.getWriter();
				response.setContentType("text/xml");
				out.println("<?xml version=\"1.0\"?>");
				out.println("<error>");
				out.println("Action not specified");
				out.println("</error>");
				out.close();
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
				//PrintWriter out = response.getWriter();
				Writer out = new OutputStreamWriter(response.getOutputStream(), DEFAULT_ENCODING);
				handler.handleLoginAction(out, params, request, response);
				out.close();

				// handle logout action
			} else if (action.equals("logout")) {
				Writer out = new OutputStreamWriter(response.getOutputStream(), DEFAULT_ENCODING);
				handler.handleLogoutAction(out, params, request, response);
				out.close();

				// handle session validate request
			} else if (action.equals("validatesession")) {
				String token = request.getHeader("Authorization");
				
				// First check for a valid authentication token
				if ( token != null && ! token.equals("") ) {
					Writer out = new OutputStreamWriter(response.getOutputStream(), DEFAULT_ENCODING);
					SessionData sessionData = RequestUtil.getSessionData(request);
					
					response.setContentType("text/xml");
					out.write("<?xml version=\"1.0\"?>");
					out.write("<validateSession><status>");
					
					if ( sessionData != null ) {
						out.write("valid");
						
					} else {
						out.write("invalid");
						
					}
					out.write("</status>");
				    
					if (sessionData != null) {
						out.write("<userInformation>");
						out.write("<name>");
						out.write(sessionData.getUserName());
						out.write("</name>");
						out.write("<fullName>");
						out.write(sessionData.getName());
						out.write("</fullName>");
						String[] groups = sessionData.getGroupNames();
						if ( groups != null ) {
							for(String groupName : groups) {
							  out.write("<group>");
							  out.write(groupName);
							  out.write("</group>");
							}
						}
						out.write("</userInformation>");
					}

					out.write("<sessionId>" + sessionId + "</sessionId></validateSession>");				
					out.close();
					
				} else {
					// With no token, validate the sessionid
					Writer out = new OutputStreamWriter(response.getOutputStream(), DEFAULT_ENCODING);
					String idToValidate = null;
					String idsToValidate[] = params.get("sessionid");
					if (idsToValidate != null) {
						idToValidate = idsToValidate[0];
						
					} else {
						// use the sessionid from the cookie
						SessionData sessionData = RequestUtil.getSessionData(request);
						if (sessionData != null) {
							idToValidate = sessionData.getId();
							
						}
					}
					SessionService.getInstance().validateSession(out, response, idToValidate);
					out.close();
				}

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
		        Writer out = new OutputStreamWriter(response.getOutputStream(), DEFAULT_ENCODING);
				handler.handleQuery(out, params, response, userName, groupNames, sessionId);
				out.close();
			} else if (action.equals("squery")) {
				Writer out = new OutputStreamWriter(response.getOutputStream(), DEFAULT_ENCODING);
				if (params.containsKey("query")) {
					handler.handleSQuery(out, params, response, userName, groupNames, sessionId);
					out.close();
				} else {
					out.write("Illegal action squery without \"query\" parameter");
					out.close();
				}
			} else if (action.trim().equals("spatial_query")) {

				logMetacat
						.debug("MetaCatServlet.handleGetOrPost - ******************* SPATIAL QUERY ********************");
				Writer out = new OutputStreamWriter(response.getOutputStream(), DEFAULT_ENCODING);
				handler.handleSpatialQuery(out, params, response, userName, groupNames, sessionId);
				out.close();

			} else if (action.trim().equals("dataquery")) {

				logMetacat.debug("MetaCatServlet.handleGetOrPost - ******************* DATA QUERY ********************");
				handler.handleDataquery(params, response, sessionId);
			} else if (action.trim().equals("editcart")) {
				logMetacat.debug("MetaCatServlet.handleGetOrPost - ******************* EDIT CART ********************");
				handler.handleEditCart(params, response, sessionId);
			} else if (action.equals("export")) {

				handler.handleExportAction(params, response, userName, groupNames, password);
			} else if (action.equals("read")) {
				if (params.get("archiveEntryName") != null) {
					ArchiveHandler.getInstance().readArchiveEntry(params, request,
							response, userName, password, groupNames);
				} else {
					handler.handleReadAction(params, request, response, userName, password,
							groupNames);
				}
			} else if (action.equals("readinlinedata")) {
				handler.handleReadInlineDataAction(params, request, response, userName, password,
						groupNames);
			} else if (action.equals("insert") || action.equals("update")) {
			    if(isReadOnly(response)) {
                    return;
                }
				PrintWriter out = response.getWriter();
				if ((userName != null) && !userName.equals("public")) {
					handler.handleInsertOrUpdateAction(request.getRemoteAddr(), request.getHeader("User-Agent"), response, out, params, userName,
							groupNames, true, true, null);
				} else {
					response.setContentType("text/xml");
					out.println("<?xml version=\"1.0\"?>");
					out.println("<error>");
					String cleanMessage = StringEscapeUtils.escapeXml("Permission denied for user " + userName + " " + action);
					out.println(cleanMessage);
					out.println("</error>");
				}
				out.close();
			} else if (action.equals("delete")) {
			    if(isReadOnly(response)) {
                    return;
                }
				PrintWriter out = response.getWriter();
				if ((userName != null) && !userName.equals("public")) {
					handler.handleDeleteAction(out, params, request, response, userName,
							groupNames);
				} else {
					response.setContentType("text/xml");
					out.println("<?xml version=\"1.0\"?>");
					out.println("<error>");
					String cleanMessage = StringEscapeUtils.escapeXml("Permission denied for " + action);
					out.println(cleanMessage);
					out.println("</error>");
				}
				out.close();
			} else if (action.equals("validate")) {
				PrintWriter out = response.getWriter();
				handler.handleValidateAction(out, params);
				out.close();
			} else if (action.equals("setaccess")) {
			    if(isReadOnly(response)) {
                    return;
                }
				PrintWriter out = response.getWriter();
				handler.handleSetAccessAction(out, params, userName, request, response);
				out.close();
			} else if (action.equals("getaccesscontrol")) {
				PrintWriter out = response.getWriter();
				handler.handleGetAccessControlAction(out, params, response, userName, groupNames);
				out.close();
			} else if (action.equals("isauthorized")) {
				PrintWriter out = response.getWriter();
				DocumentUtil.isAuthorized(out, params, request, response);
				out.close();
			} else if (action.equals("getprincipals")) {
				Writer out = new OutputStreamWriter(response.getOutputStream(), DEFAULT_ENCODING);
				handler.handleGetPrincipalsAction(out, userName, password);
				out.close();
			} else if (action.equals("getdoctypes")) {
				PrintWriter out = response.getWriter();
				handler.handleGetDoctypesAction(out, params, response);
				out.close();
			} else if (action.equals("getdtdschema")) {
				PrintWriter out = response.getWriter();
				handler.handleGetDTDSchemaAction(out, params, response);
				out.close();
			} else if (action.equals("getdocid")) {
				handler.handleGetDocid(params, response);
			} else if (action.equals("getlastdocid")) {
				PrintWriter out = response.getWriter();
				handler.handleGetMaxDocidAction(out, params, response);
				out.close();
			} else if (action.equals("getalldocids")) {
				PrintWriter out = response.getWriter();
				handler.handleGetAllDocidsAction(out, params, response);
				out.close();
			} else if (action.equals("isregistered")) {
				PrintWriter out = response.getWriter();
				handler.handleIdIsRegisteredAction(out, params, response);
				out.close();
			} else if (action.equals("getrevisionanddoctype")) {
				PrintWriter out = response.getWriter();
				handler.handleGetRevisionAndDocTypeAction(out, params);
				out.close();
			} else if (action.equals("getversion")) {
				response.setContentType("text/xml");
				PrintWriter out = response.getWriter();
				out.println(MetacatVersion.getVersionAsXml());
				out.close();
			} else if (action.equals("getlog")) {
				handler.handleGetLogAction(params, request, response, userName, groupNames, sessionId);
			} else if (action.equals("getloggedinuserinfo")) {
				PrintWriter out = response.getWriter();
				response.setContentType("text/xml");
				out.println("<?xml version=\"1.0\"?>");
				out.println("\n<user>\n");
				out.println("\n<username>\n");
				out.println(userName);
				out.println("\n</username>\n");
				if (name != null) {
					out.println("\n<name>\n");
					out.println(name);
					out.println("\n</name>\n");
				}
				if (AuthUtil.isAdministrator(userName, groupNames)) {
					out.println("<isAdministrator></isAdministrator>\n");
				}
				if (AuthUtil.isModerator(userName, groupNames)) {
					out.println("<isModerator></isModerator>\n");
				}
				out.println("\n</user>\n");
				out.close();
			} else if (action.equals("buildindex")) {
			    if(isReadOnly(response)) {
                    return;
                }
				handler.handleBuildIndexAction(params, request, response, userName, groupNames);
			} else if (action.equals("reindex")) {
			    if(isReadOnly(response)) {
                    return;
                }
				handler.handleReindexAction(params, request, response, userName, groupNames);
			} else if (action.equals("reindexall")) {
			    if(isReadOnly(response)) {
                    return;
                }
                handler.handleReindexAllAction(params, request, response, userName, groupNames);
            } else if (action.equals("login") || action.equals("logout")) {
				/*
				 * } else if (action.equals("protocoltest")) { String testURL =
				 * "metacat://dev.nceas.ucsb.edu/NCEAS.897766.9"; try { testURL =
				 * ((String[]) params.get("url"))[0]; } catch (Throwable t) { }
				 * String phandler = System
				 * .getProperty("java.protocol.handler.pkgs");
				 * response.setContentType("text/html"); PrintWriter out =
				 * response.getWriter(); out.println("<body
				 * bgcolor=\"white\">"); out.println("<p>Handler property:
				 * <code>" + phandler + "</code></p>"); out.println("<p>Starting
				 * test for:<br>"); out.println(" " + testURL + "</p>"); try {
				 * URL u = new URL(testURL); out.println("<pre>");
				 * out.println("Protocol: " + u.getProtocol()); out.println("
				 * Host: " + u.getHost()); out.println(" Port: " + u.getPort());
				 * out.println(" Path: " + u.getPath()); out.println(" Ref: " +
				 * u.getRef()); String pquery = u.getQuery(); out.println("
				 * Query: " + pquery); out.println(" Params: "); if (pquery !=
				 * null) { Hashtable qparams =
				 * MetacatUtil.parseQuery(u.getQuery()); for (Enumeration en =
				 * qparams.keys(); en .hasMoreElements();) { String pname =
				 * (String) en.nextElement(); String pvalue = (String)
				 * qparams.get(pname); out.println(" " + pname + ": " + pvalue); } }
				 * out.println("</pre>"); out.println("</body>");
				 * out.close(); } catch (MalformedURLException mue) {
				 * System.out.println( "bad url from
				 * MetacatServlet.handleGetOrPost");
				 * out.println(mue.getMessage()); mue.printStackTrace(out);
				 * out.close(); }
				 */
			} else if (action.equals("refreshServices")) {
				// TODO MCD this interface is for testing. It should go through
				// a ServiceService class and only work for an admin user. Move 
				// to the MetacatAdminServlet
				ServiceService.refreshService("XMLSchemaService");
				return;
			} else if (action.equals("scheduleWorkflow")) {
			    if(isReadOnly(response)) {
                    return;
                }
				try {
					WorkflowSchedulerClient.getInstance().scheduleJob(request, response,
							params, userName, groupNames);
					return;
				} catch (BaseException be) {
					ResponseUtil.sendErrorXML(response,
							ResponseUtil.SCHEDULE_WORKFLOW_ERROR, be);
					return;
				}
			} else if (action.equals("unscheduleWorkflow")) {
			    if(isReadOnly(response)) {
                    return;
                }
				try {
					WorkflowSchedulerClient.getInstance().unScheduleJob(request,
							response, params, userName, groupNames);
					return;
				} catch (BaseException be) {
					ResponseUtil.sendErrorXML(response,
							ResponseUtil.UNSCHEDULE_WORKFLOW_ERROR, be);
					return;
				}
			} else if (action.equals("rescheduleWorkflow")) {
			    if(isReadOnly(response)) {
                    return;
                }
				try {
					WorkflowSchedulerClient.getInstance().reScheduleJob(request,
							response, params, userName, groupNames);
					return;
				} catch (BaseException be) {
					ResponseUtil.sendErrorXML(response,
							ResponseUtil.RESCHEDULE_WORKFLOW_ERROR, be);
					return;
				}
			} else if (action.equals("getScheduledWorkflow")) {
				try {
					WorkflowSchedulerClient.getInstance().getJobs(request, response,
							params, userName, groupNames);
					return;
				} catch (BaseException be) {
					ResponseUtil.sendErrorXML(response,
							ResponseUtil.GET_SCHEDULED_WORKFLOW_ERROR, be);
					return;
				}
			} else if (action.equals("deleteScheduledWorkflow")) {
			    if(isReadOnly(response)) {
                    return;
                }
				try {
					WorkflowSchedulerClient.getInstance().deleteJob(request, response,
							params, userName, groupNames);
					return;
				} catch (BaseException be) {
					ResponseUtil.sendErrorXML(response,
							ResponseUtil.DELETE_SCHEDULED_WORKFLOW_ERROR, be);
					return;
				}
			} else if (action.equals("shrink")) {
			     // handle shrink DBConnection request
                PrintWriter out = response.getWriter();
                if(!AuthUtil.isAdministrator(userName, groupNames)){
                    out.println("The user "+userName+ " is not the administrator of the Metacat and doesn't have the permission to call the method.");
                    out.close();
                    return;
                }
                boolean success = false;
                // If all DBConnection in the pool are free and DBConnection
                // pool
                // size is greater than initial value, shrink the connection
                // pool
                // size to initial value
                success = DBConnectionPool.shrinkConnectionPoolSize();
                if (success) {
                    // if successfully shrink the pool size to initial value
                    out.println("DBConnection Pool shrunk successfully.");
                }// if
                else {
                    out.println("DBConnection pool did not shrink successfully.");
                }
                // close out put
                out.close();
			} else {
				//try the plugin handler if it has an entry for handling this action
				MetacatHandlerPlugin handlerPlugin = MetacatHandlerPluginManager.getInstance().getHandler(action);
				if (handlerPlugin != null) {
				    if(isReadOnly(response)) {
	                    return;
	                }
					handlerPlugin.handleAction(action, params, request, response, userName, groupNames, sessionId);
				} 
				else {
					PrintWriter out = response.getWriter();
					out.println("<?xml version=\"1.0\"?>");
					out.println("<error>");
					String cleanMessage = StringEscapeUtils.escapeXml("Error: action: " + action + " not registered.  Please report this error.");
					out.println(cleanMessage);
					out.println("</error>");
					out.close();
				}
			}

			// Schedule the sitemap generator to run periodically
			handler.scheduleSitemapGeneration(request);


		} catch (PropertyNotFoundException pnfe) {
			String errorString = "Critical property not found: " + pnfe.getMessage();
			logMetacat.error("MetaCatServlet.handleGetOrPost - " + errorString);
			throw new ServletException(errorString);
		} catch (MetacatUtilException ue) {
			String errorString = "Utility error: " + ue.getMessage();
			logMetacat.error("MetaCatServlet.handleGetOrPost - " + errorString);
			throw new ServletException(errorString);
		} catch (ServiceException ue) {
			String errorString = "Service error: " + ue.getMessage();
			logMetacat.error("MetaCatServlet.handleGetOrPost - " + errorString);
			throw new ServletException(errorString);
		} catch (HandlerException he) {
			String errorString = "Handler error: " + he.getMessage();
			logMetacat.error("MetaCatServlet.handleGetOrPost - " + errorString);
			throw new ServletException(errorString);
		} catch (ErrorSendingErrorException esee) {
			String errorString = "Error sending error message: " + esee.getMessage();
			logMetacat.error("MetaCatServlet.handleGetOrPost - " + errorString);
			throw new ServletException(errorString);
		} catch (ErrorHandledException ehe) {
			// Nothing to do here.  We assume if we get here, the error has been 
			// written to ouput.  Continue on and let it display.
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
            out.println("The Metacat is on the read-only mode and your request can't be fulfiled. Please try again later.");
            out.println("</error>");
            out.close();
        }
        return readOnly;
    }
}
