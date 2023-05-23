/**
 *  Copyright: 2013 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.log4j.PropertyConfigurator;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrClient;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.ServiceFailure;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


import edu.ucsb.nceas.metacat.common.SolrServerFactory;
import edu.ucsb.nceas.metacat.common.query.EnabledQueryEngines;

/**
 * The start class of the index.
 * @author tao
 *
 */
public class ApplicationController implements Runnable {
    
    private static long DEFAULTINTERVAL = 7200000;
    private static String SOLRINDEXES = "solrIndexes";
    private static short FIRST = 0;

    private List<SolrIndex> solrIndexes = null;
    private List<Runnable> sysmetaListeners = new ArrayList<Runnable>();
    private static ApplicationContext context = null;
    private String springConfigFileURL = "/index-processor-context.xml";
    private String metacatPropertiesFile = null;
    private static int waitingTime = IndexGeneratorTimerTask.WAITTIME;
    private static int maxAttempts = IndexGeneratorTimerTask.MAXWAITNUMBER;
    private static long period = DEFAULTINTERVAL;
    Log log = LogFactory.getLog(ApplicationController.class);
    private static Date timeOfFirstRun = null;
    private static String queryIncludeArchivedParaName = null;
    private static String queryIncludeArchivedParaValue = null;
    
    
    /**
     * Constructor
     */
    /*public ApplicationController () throws Exception {
        init();
    }*/
    
    /**
     * Set the Spring configuration file url and metacat.properties file
     * @param springConfigFile  the path of the Spring configuration file
     * @param metacatPropertyFile  the path of the metacat.properties file
     */
    public ApplicationController(String springConfigFileURL, String metacatPropertiesFile) throws Exception {
        this.springConfigFileURL = springConfigFileURL;
        this.metacatPropertiesFile = metacatPropertiesFile;
        //init();
    }
    
    /**
     * Loads the metacat.prioerties into D1 Settings utility
     * this gives us access to all metacat properties as well as 
     * overriding any properties as needed. 
     * Note: in the junit test, we are using the test.properties rather than this properties file.
     * 
     * Makes sure shared Hazelcast configuration file location is set
     */
    private void initializeSharedConfiguration() {
        int times = 0;
        boolean foundProperty = false;
        while(true) {
            File metacatProperties = new File(metacatPropertiesFile);
            if(metacatProperties.exists()) {
                foundProperty = true;
                break;
            } else {
                try {
                        Thread.sleep(waitingTime);
                } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                        e.printStackTrace();
                }
            }
            times++;
            if(times >= maxAttempts) {
                log.error("ApplicationController.initialzeSharedConfiguration - MetacatIndex wait a while and still can't find the metacat.properties file.");
                break;//we still break the while loop and continue. But the properties in the metacat.properties can't be read.
            }
                
        }
        
        try {
            Settings.getConfiguration();
            Settings.augmentConfiguration(metacatPropertiesFile);
        } catch (ConfigurationException e) {
            log.error("Could not initialize shared Metacat properties. " + e.getMessage(), e);
        }
        
        // make sure hazelcast configuration is defined so that
        String hzConfigFileName = Settings.getConfiguration().getString("dataone.hazelcast.configFilePath");
        if (hzConfigFileName == null) {
            // use default metacat hazelcast.xml file in metacat deployment
            hzConfigFileName = 
                    Settings.getConfiguration().getString("application.deployDir") +
                    "/" +
                    Settings.getConfiguration().getString("application.context") + 
                    "/WEB-INF/hazelcast.xml";
            // set it for other parts of the code
            Settings.getConfiguration().setProperty("dataone.hazelcast.configFilePath", hzConfigFileName);
            //set data.hazelcast.location.clientconfig. This property will be used in d1_cn_index_processor module.
            //if we don't set this property, d1_cn_index_processor will use the default location /etc/dataone/storage.
            Settings.getConfiguration().setProperty("dataone.hazelcast.location.clientconfig", hzConfigFileName);
        }
        if(foundProperty) {
            period = Settings.getConfiguration().getLong("index.regenerate.interval");
        }
        
        try {
            String timeStrOfFirstRun = Settings.getConfiguration().getString("index.regenerate.firsttime");
            determineTimeOfFirstRunRegeneratingThread(timeStrOfFirstRun);
        } catch (Exception e){
            log.warn("IndexGenerator.construtor - failed to get the time for the first run "+e.getMessage()+" and we will use the default one - 11:50 PM.");
            try {
                determineTimeOfFirstRunRegeneratingThread("11:50 PM");
            } catch (Exception ee) {
                Date now = new Date();
                timeOfFirstRun = new Date(now.getTime()+20*60*1000);
                log.info("ApplicationController.initializeSharedConfiguration - the ultimate time to run the regnerate index thread first time is "+timeOfFirstRun.toString());
            }
            
        }
        queryIncludeArchivedParaName = Settings.getConfiguration().getString("solr.query.append.includeArchived.name");
        queryIncludeArchivedParaValue = Settings.getConfiguration().getString("solr.query.append.includeArchived.value");
    }
    
    /**
     * Determine the time to run the regenerating thread first. 
     * If the given time already passed or only be less than 2 seconds to pass, we need to set the timeOfFirstRun to be 24 hours latter (the second day)
     * @param givenTime the given time to run. The format should like 10:00 PM
     */
    private void determineTimeOfFirstRunRegeneratingThread(String givenTime) throws ParseException {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        Date givenDate = format.parse(givenTime); //since it is short time, so the date will be 1/1/1970
        log.info("ApplicationController.determineTimeOfFirstRunRegeneratingThread - The time (given) to first time run the thread is "+givenDate.toString());
        Calendar date = new GregorianCalendar();
        date.setTime(givenDate);
        int hour = date.get(Calendar.HOUR_OF_DAY);
        log.info("ApplicationController.determineTimeOfFirstRunRegeneratingThread - The given hour is "+hour);
        int minute = date.get(Calendar.MINUTE);
        log.info("ApplicationController.determineTimeOfFirstRunRegeneratingThread - The given minutes is "+minute);
        //set the hour and minute to today
        Calendar today = new GregorianCalendar();
        today.set(Calendar.HOUR_OF_DAY, hour);
        today.set(Calendar.MINUTE, minute);
        timeOfFirstRun = today.getTime();
        log.info("ApplicationController.determineTimeOfFirstRunRegeneratingThread - The time (after transforming to today) to first time run the thread is "+timeOfFirstRun.toString());
        Date now = new Date();
        if((timeOfFirstRun.getTime() - now.getTime()) <2000) {
            //if the given time already passed or only be less than 2 seconds to pass, we need to set the timeOfFirstRun to be 24 hours latter (the second day)
            log.info("ApplicationController.determineTimeOfFirstRunRegeneratingThread - The time (after transforming to today) to first time run the thread "+timeOfFirstRun.toString()+" passed and we will delay it 24 hours");
            timeOfFirstRun = new Date(timeOfFirstRun.getTime()+24*3600*1000);
            //timeOfFirstRun = new Date(timeOfFirstRun.getTime()+2*3600*1000);
        }
        log.info("ApplicationController.determineTimeOfFirstRunRegeneratingThread - The final time of the first time running the thread is "+timeOfFirstRun.toString());
    }
    
    /**
     * Initialize the list of the SolrIndex objects from the configuration file.
     * Set the SolrServer implementation using the factory.
     */
    public void initialize() throws Exception {
        context = getContext();
        solrIndexes = (List<SolrIndex>) context.getBean(SOLRINDEXES);
        
        // use factory to create the correct impl
        SolrClient solrServer = null;
        try {
            solrServer = SolrServerFactory.createSolrServer();
        } catch (Exception e) {
            log.error("Could not create SolrServer form factory", e);
            throw e;
        }

        // start the SystemMetadata listener[s] (only expect there to be one)
        for (SolrIndex solrIndex: solrIndexes) {
            // set the solr server to use
            solrIndex.setSolrServer(solrServer);

            // start listening for events
            SystemMetadataEventListener smel = new SystemMetadataEventListener();
            smel.setSolrIndex(solrIndex);
            sysmetaListeners.add(smel);
        }
        
    }
    
    /**
     * Get the ApplicaionContext of Spring.
     */
    private ApplicationContext getContext() {
        if (context == null) {
            context = new ClassPathXmlApplicationContext(springConfigFileURL);
        }
        return context;
    }

    /**
     * Get the path of the Spring configuration file.
     * @return the path of the Spring configuration file.
     */
    public String getSpringConfigFile() {
        return this.springConfigFileURL;
    }
    
    /**
     * Get the list of the solr index.
     * @return the list of the solr index.
     */
    public List<SolrIndex> getSolrIndexes() {
        return this.solrIndexes;
    }
    
    
    /**
     * Start to generate indexes for those haven't been indexed in another thread.
     * It will create a timer to run this task periodically. 
     * If the property of "index.regenerate.interval" is less than 0, the thread would NOT run.
     */
    private void startIndexGenerator() {
        if(period > 0) {
            //SystemMetadataEventListener listener = sysmetaListeners.get(FIRST);
            IndexGeneratorTimerTask generator = new IndexGeneratorTimerTask();
            //Thread indexThread = new Thread(generator);
            //indexThread.start();
            Timer indexTimer = new Timer();
            //indexTimer.scheduleAtFixedRate(generator, Calendar.getInstance().getTime(), period);
            log.debug("ApplicationController.startIndexGenerate - the first time for runing the thread to reindex the failed objects is =============="+timeOfFirstRun.toString()+" and the period is "+period);
            indexTimer.schedule(generator, timeOfFirstRun, period);
        }
        
    }
    
    /**
     * Start the system metadata listener. Prior to call this method, we should call
     * initialize method first.
     * @throws ServiceFailure 
     * @throws FileNotFoundException 
     */
    private void startSysmetaListener() throws FileNotFoundException, ServiceFailure {
        if(sysmetaListeners != null) {
            //only expects one listener.
            for(Runnable listener : sysmetaListeners) {
                if(listener != null) {
                    listener.run();
                }
            }
        }
    }
    
    /**
     * It will initialize the shared metacat.properties and the SolrIndex, then start system metadata event listener and 
     *  generate indexes for those haven't been indexed in another thread. This method is for the MetacatIndexServlet.
     *  The reason we put those methods (init, startSysmetaListener, startIndex)in the run (another thread) is that the waiting readiness of the metacat wouldn't 
     *  block the start of the servlet container (e.g., tomcat).
     */
    public void run() {
        initializeSharedConfiguration();
        try {
            boolean isSolrEnabled = true;
            try {
               isSolrEnabled = EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.SOLRENGINE);
            } catch (Exception e) {
                log.error("ApplicationController.run - Metacat-index can't read the enabled query engine list from metacat.properties :"+e.getMessage());
            }
            //if the solr query engine is disabled, we stop here.
            if(!isSolrEnabled) {
                return;
            }
            initialize();
            startSysmetaListener();
            startIndexGenerator();//it will create another thread.
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Application.run "+e.getMessage());
        }
        
    }
    
    /**
     * Get the name of the query parameter for including archived objects
     * @return the name of the query parameter
     */
    public static String getIncludeArchivedQueryParaName() {
        if(queryIncludeArchivedParaName == null) {
            queryIncludeArchivedParaName = Settings.getConfiguration().getString("solr.query.append.includeArchived.name");
        }
        return queryIncludeArchivedParaName;
    }
    
    /**
     * Get the query parameter value for including archived objects
     * @return the value of the query parameter
     */
    public static String getIncludeArchivedQueryParaValue() {
        if(queryIncludeArchivedParaValue == null) {
            queryIncludeArchivedParaValue = Settings.getConfiguration().getString("solr.query.append.includeArchived.value");
        }
        return queryIncludeArchivedParaValue;
    }
}
