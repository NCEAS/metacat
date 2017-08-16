package edu.ucsb.nceas.metacat.dataone;

import org.apache.log4j.Logger;
import org.dataone.cn.index.messaging.IndexTaskMessagingClient;
import org.dataone.cn.index.messaging.IndexTaskMessagingClientFactory;

/**
 * This class will guarantee to get a single IndexTaskMesagingClient by the singleton pattern 
 * @author tao
 *
 */
public class IndexTaskMessagingClientManager {

    private IndexTaskMessagingClient indexTaskClient = null;
    private static IndexTaskMessagingClientManager manager = null;
    private static Logger logMetacat = Logger.getLogger(IndexTaskMessagingClientManager.class);
    
    /**
     * A thread safe method to get the instance of the manager
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public static IndexTaskMessagingClientManager getInstance() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if(manager == null) {
            synchronized (IndexTaskMessagingClientManager.class) {
                if(manager == null) {
                    manager = new IndexTaskMessagingClientManager();
                }
            }
        }
        return manager;
    }
    
    /**
     * Default private constructor
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private IndexTaskMessagingClientManager() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        initIndexTaskClient();
    }
    
    /*
     * Method to get the index tasks client.
     */
    private void initIndexTaskClient() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if(indexTaskClient == null) {
            indexTaskClient = IndexTaskMessagingClientFactory.getClient();
            logMetacat.info("IndexTaskMessagingClientManager.initIndexTaskClient - after generating the client .......................");
        }
    }
    
    /**
     * Get the client object which can submit the index tasks.
     * @return
     */
    public IndexTaskMessagingClient getMessagingClient() {
        return indexTaskClient;
    }
}
