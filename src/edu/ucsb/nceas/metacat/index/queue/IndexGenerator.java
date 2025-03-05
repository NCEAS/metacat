package edu.ucsb.nceas.metacat.index.queue;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader;
import edu.ucsb.nceas.metacat.index.queue.pool.RabbitMQChannelFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.index.IndexEventDAO;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;

/**
 * The IndexGenerator class will publish (send) the index information
 * to a RabbitMQ queue. An index worker will consume the information.
 * @author tao
 *
 */
public class IndexGenerator extends BaseService {

    //Those strings are the types of the index tasks.
    //"create" is the index task type for the action when a new object was
    //created. So the solr index will be generated.
    //delete is the index task type for the action when an object was deleted. 
    //So the solr index will be deleted sysmeta is the index task type for the 
    //action when the system metadata of an existing object was updated. 
    public final static String CREATE_INDEX_TYPE = "create";
    public final static String DELETE_INDEX_TYPE = "delete";
    //this handle for resource map only
    public final static String SYSMETA_CHANGE_TYPE = "sysmeta"; 

    //use for the operations such as create, update, updateSystem, delete, archive
    public final static int MEDIUM_PRIORITY = 4; 
    //use for resource map objects in the operations such as create, update, 
    //updateSystem, delete, archive
    public final static int MEDIUM_RESOURCEMAP_PRIORITY = 3; 
    //use for the bulk operations such as reindexing the whole corpus 
    public final static int LOW_PRIORITY = 1; 
    //The header name in the message to store the identifier
    private final static String HEADER_ID = "id"; 
    //The header name in the message to store the index type
    private final static String HEADER_INDEX_TYPE = "index_type"; 

    private final static long CLEAR_FUTURE_TASK_DELAY_MILLI = 300000; //5 minutes
    private final static long CLEAR_FUTURE_TASK_PERIOD_MILLI = 600000; //10 minutes
    private final static int DEFAULT_MAX_TASK_SIZE = 10000;

    private static volatile IndexGenerator instance = null;
    private static GenericObjectPool<Channel> channelPool = null;
    private static int nThreads;
    private static ExecutorService executor = null;
    private static Set<Future> futures = Collections.synchronizedSet(new HashSet<>());
    private static int maxTaskSize;
    private static Timer timer;
    private static Log logMetacat = LogFactory.getLog("IndexGenerator");
    
    /**
     * Private constructor
     */
    private IndexGenerator() throws ServiceException{
        super();
        _serviceName="IndexQueueService";
        try {
          init();
        } catch (ServiceException se) {
          logMetacat.error("There was a problem creating the IndexGenerator."
                          + " The error message was: " + se.getMessage());
          throw se;
        }
    }
    
    /**
     * Initialize the RabbitMQ channel pool and executor service pool
     * @throws ServiceException
     */
    private void init() throws ServiceException {
        nThreads = Runtime.getRuntime().availableProcessors();
        nThreads--;// Leave 1 main thread for execution
        logMetacat.debug("The number of threads based on the number of processors is " + nThreads);
        try {
            RabbitMQChannelFactory channelFactory = new RabbitMQChannelFactory();
            int channelMax = channelFactory.getChannelMax();
            logMetacat.debug("The max number of channels for one connection on the RabbitMQ "
                                 + "configuration is " + channelMax);
            //We choose the smaller number between channelMax and processor number
            // as the channel pool size and the thread pool size
            nThreads = Math.min(channelMax, nThreads);
            nThreads = Math.max(1, nThreads);// In case nThread is 0 or a negative number
            if (channelPool == null) {
                GenericObjectPoolConfig<Channel> config = new GenericObjectPoolConfig<>();
                config.setMaxTotal(nThreads);
                channelPool = new GenericObjectPool<>(channelFactory, config);
                logMetacat.debug(
                    "The max total channels in the channel pool is " + config.getMaxTotal());
                logMetacat.debug(
                    "The max idle channels in the channel pool is " + config.getMaxIdle());
                logMetacat.debug(
                    "The min idle channels in the channel pool is " + config.getMinIdle());
            }
        } catch (Exception e) {
            String error = "Cannot create channels connecting to the RabbitMQ queue: "
                            + RabbitMQChannelFactory.INDEX_QUEUE_NAME + " since " + e.getMessage();
            logMetacat.error(error, e);
            throw new ServiceException(error);
        }
        executor = Executors.newFixedThreadPool(nThreads);
        logMetacat.debug("The final size of the thread pool to do the submission job is "
                             + nThreads);
        try {
            String maxTaskSizeStr = PropertyService.getProperty("index.submitting.set.size");
            maxTaskSize = Integer.parseInt(maxTaskSizeStr);
        } catch (PropertyNotFoundException | NumberFormatException e) {
            maxTaskSize = DEFAULT_MAX_TASK_SIZE;
        }
        logMetacat.debug("The max number of index tasks the generator can hold is " + maxTaskSize);
        initFeatureSetClearTask();
    }

    /**
     * Initialize a repeatable timer task to clear the future which is completed in the future set
     */
    private void initFeatureSetClearTask() {
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (futures.size() >= DEFAULT_MAX_TASK_SIZE) {
                    //When it reaches the max size, we need to remove the completed futures from the
                    // set. So we can avoid the issue of out of memory.
                    try {
                        HashStoreUpgrader.removeCompleteFuture(futures);
                        logMetacat.debug("Cleared the completed index tasks from the future set");
                    } catch (Exception e) {
                        // Failure of removing a task doesn't interrupt the workflow
                        logMetacat.warn("Metacat couldn't remove the completed index tasks: "
                                            + e.getMessage());
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(task, CLEAR_FUTURE_TASK_DELAY_MILLI, CLEAR_FUTURE_TASK_PERIOD_MILLI);
    }

    /**
     * Get the last sub-directory in the path.
     * If the path is /var/data, data will be returned. 
     * @param path  the path will be analyzed.
     * @return  the last part of path
     */
    protected static String getLastSubdir(String path) {
        String lastDir = null;
        if (path != null) {
            if (path.endsWith("/")) {
                //remove the last "/"
                path = path.substring(0, path.lastIndexOf("/"));
            }
            int index = path.lastIndexOf("/");
            lastDir = path.substring(index+1);
        }
        logMetacat.debug("The last sub-directory is " + lastDir);
        return lastDir;
    }
    
    /**
     * Implement a Singleton pattern using "double checked locking" pattern.
     *
     * @return a singleton instance of the RabbitMQService class.
     */
    public static IndexGenerator getInstance() throws ServiceException {
        if (instance == null) {
            synchronized (IndexGenerator.class) {
                if (instance == null) {
                    logMetacat.debug("IndexGenerator.getInstance - " 
                                     + "Creating new controller instance");
                    try {
                        instance = new IndexGenerator();
                    } catch (ServiceException e) {
                        logMetacat.debug("IndexGenerator.getInstance - failed " 
                                       + "to create the IndexGenerator instance" 
                                       + " and set it to null.");
                        instance = null;
                        throw e;
                    }
                }
            }
        }
        return instance;
    }

    /**
     * This method sets the instance to null and forces the getInstance method
     * to create a new instance.
     * This method is only used for a testing purpose!
     */
    public static void refreshInstance() {
        synchronized (IndexGenerator.class) {
            instance = null;
        }
    }

    /**
     * Publish the given information to the index queue
     * @param id  the identifier of the object which will be indexed
     * @param index_type  the type of indexing, it can be - delete, create or sysmeta
     * @param priority  the priority of the index task
     */
    public void publish(Identifier id, String index_type, int priority) 
                                    throws ServiceException, InvalidRequest {
        if (id == null || id.getValue() == null 
                       || id.getValue().isBlank()) {
            throw new InvalidRequest("0000", 
                    "IndexGenerator.publishToIndexQueue - the identifier can't " 
                    + "be null or blank.");
        }
        if (index_type == null || index_type.isBlank()) {
            throw new InvalidRequest("0000", "IndexGenerator.publishToIndexQueue" 
                                 + " - the index type can't be null or blank.");
        }
        String errorType = IndexEvent.CREATE_FAILURE_TO_QUEUE;
        String additionErrorMessage = null;
        if (index_type.equals(DELETE_INDEX_TYPE)) {
            errorType = IndexEvent.DELETE_FAILURE_TO_QUEUE;
        }
        if (channelPool == null) {
            try {
                saveFailedTaskToDB(errorType, id, "RabbitMQchannelPool is null");
            } catch (SQLException e) {
                additionErrorMessage = e.getMessage();
            }
            String error = "Can't publish the index task for " + id.getValue()
                + " since the RabbitMQ channel pool is null, which means Metacat "
                + "cannot connect with RabbitMQ.";
            if (additionErrorMessage != null) {
                error = error + " And also Metacat can't save the failure index task into DB since "
                                + additionErrorMessage;
            }
            logMetacat.error(error);
            throw new ServiceException(error);
        }
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(HEADER_ID, id.getValue());
            headers.put(HEADER_INDEX_TYPE, index_type);
            AMQP.BasicProperties basicProperties =
                     new AMQP.BasicProperties.Builder()
                    .contentType("text/plain")
                    .deliveryMode(2) // set this message to persistent
                    .priority(priority)
                    .headers(headers)
                    .build();
            final String errorTypeFinal = errorType;
            Future<?> future = executor.submit(() -> {
                submitMessageInThread(errorTypeFinal, id, basicProperties, index_type);
            });
            futures.add(future);
            if (futures.size() >= maxTaskSize) {
                //When it reaches the max size, we need to remove the completed futures from the
                // set. So we can avoid the issue of out of memory.
                try {
                    HashStoreUpgrader.removeCompleteFuture(futures);
                } catch (Exception e) {
                    // Failure of removing a task doesn't interrupt the workflow
                    logMetacat.warn("Metacat couldn't remove the completed index tasks: "
                                        + e.getMessage());
                }
            }
        } catch (Exception e) {
            try {
                saveFailedTaskToDB(errorType, id, e.getMessage());
            } catch (SQLException sqle) {
                additionErrorMessage = sqle.getMessage();
            }
            String error = "Can't publish the index task for "
                            + id.getValue() + " since " + e.getMessage();
            if (additionErrorMessage != null) {
                error = error + ". And also Metacat can't save the failure index task into DB since "
                                + additionErrorMessage;
            }
            logMetacat.error(error);
            throw new ServiceException(error);
        }
    }

    /**
     * The method will be used to submit an index task in a thread
     * @param errorTypeFinal  the error type
     * @param id  the identifier of pid in the index task
     * @param basicProperties  the properties associated with the index task
     * @param index_type  the index type of the index task
     */
    private void submitMessageInThread(String errorTypeFinal, Identifier id,
                                       AMQP.BasicProperties basicProperties, String index_type) {
        // If the first time publish fails, Metacat will try to check out another channel and
        // give another shoot.
        int times = nThreads;
        for (int i = 0; i <= times; i++) {
            boolean success = false;
            try {
                Channel channel = channelPool.borrowObject();
                try {
                    channel.basicPublish(
                        RabbitMQChannelFactory.EXCHANGE_NAME,
                        RabbitMQChannelFactory.INDEX_ROUTING_KEY, basicProperties,
                        null);
                    success = true;
                } catch (Exception ex) {
                    channelPool.invalidateObject(channel);
                    channel = null;
                    throw ex;
                } finally {
                    if (channel != null) {
                        channelPool.returnObject(channel);
                    }
                }
            } catch (Exception e) {
                if (i == times) {
                    // Only logs the error in the second time try
                    String additionErrorMessage1= null;
                    try {
                        saveFailedTaskToDB(errorTypeFinal, id, e.getMessage());
                    } catch (SQLException sqle) {
                        additionErrorMessage1 = sqle.getMessage();
                    }
                    String error = "Can't publish the index task for "
                        + id.getValue() + " since " + e.getMessage();
                    if (additionErrorMessage1 != null) {
                        error = error
                            + ". And also Metacat can't save the failure index task into "
                            + "DB since " + additionErrorMessage1;
                    }
                    logMetacat.error(error);
                }
            }
            if (success) {
                logMetacat.debug(
                    "The index task with the " + "object identifier "
                        + id.getValue() + ", the index type " + index_type
                        + " (null means Metacat doesn't have the object), " + " the priority "
                        + basicProperties.getPriority()
                        + " was push into RabbitMQ with the exchange name "
                        + RabbitMQChannelFactory.EXCHANGE_NAME
                        + " at the " + i + " try.");
                break;
            }
        }
    }

    /**
     * Save the failed index tasks into database
     * @param action  the failed action
     * @param id  the identifier of the object which needs to be indexed
     * @param description  description of the failure
     * @throws SQLException
     */
    private void saveFailedTaskToDB(String action, Identifier id, String description) 
                                                                    throws SQLException {
        IndexEvent event = new IndexEvent();
        event.setAction(action);
        event.setDate(Calendar.getInstance().getTime());
        event.setDescription(description);
        event.setIdentifier(id);
        IndexEventDAO.getInstance().add(event);
    }

    /**
     * This service is not refreshable
     */
    @Override
    public boolean refreshable() {
        return false;
    }

    @Override
    protected void doRefresh() throws ServiceException {
        //do nothing
    }

    /**
     * Stop the service
     */
    @Override
    public void stop() throws ServiceException {
        try {
            if (channelPool != null) {
                channelPool.close();
            }
            logMetacat.info("Stop the index queue service.");
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }
    
    /**
     * Get the number of messages in the index queue
     * @return  the number of messages
     */
    public int size() {
       return 0;
    }

    /**
     * This method is used for testing to replace the channel pool by a mock pool
     * @param pool
     */
    protected static void setChannelPool(GenericObjectPool<Channel> pool) {
        channelPool = pool;
    }

    /**
     * Get the channel pool of this object. It is for testing only.
     * @return the channel pool object
     */
    protected static GenericObjectPool<Channel> getChannelPool() {
        return channelPool;
    }

    /**
     * Get the feature set of this object. It is for testing only
     * @return the feature set
     */
    protected static Set<Future> getFutures() {
        return futures;
    }
}
