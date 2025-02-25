package edu.ucsb.nceas.metacat.index.queue;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader;
import edu.ucsb.nceas.metacat.index.queue.pool.RabbitMQChannelFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

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
    private final static String EXCHANGE_NAME = "dataone-index";
    private final static String INDEX_QUEUE_NAME = "index";
    private final static String INDEX_ROUTING_KEY = "index";
    private final static int MAX_TASK_SIZE = 10000;

    private static Connection rabbitMQconnection = null;
    private static IndexGenerator instance = null;
    private static GenericObjectPool<Channel> channelPool = null;
    private static int nThreads;
    private static ExecutorService executor = null;
    private static Set<Future> futures = Collections.synchronizedSet(new HashSet<>());

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
          logMetacat.error("IndexGenerator.constructor - "
                          + "There was a problem creating the IndexGenerator." 
                          + " The error message was: " + se.getMessage());
          throw se;
        }
    }
    
    /**
     * Initialize the RabbitMQ service
     * @throws ServiceException
     */
    private void init() throws ServiceException {
        // Default values for the RabbitMQ message broker server. The value of
        //'localhost' is valid for a RabbitMQ server running on a 'bare metal'
        //server, inside a VM, or within a Kubernetes where Metacat and the
        //RabbitMQ server are running in containers that belong to the same Pod.
        //These defaults will be used if the properties file cannot be read.
        String RabbitMQhost = Settings.getConfiguration().
                                  getString("index.rabbitmq.hostname", "localhost");
        int RabbitMQport = Settings.getConfiguration().
                                          getInt("index.rabbitmq.hostport", 5672);
        String RabbitMQusername = Settings.getConfiguration().
                                      getString("index.rabbitmq.username", "guest");
        String RabbitMQpassword = Settings.getConfiguration().
                                    getString("index.rabbitmq.password", "guest");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMQhost);
        factory.setPort(RabbitMQport);
        factory.setPassword(RabbitMQpassword);
        factory.setUsername(RabbitMQusername);
        // connection that will recover automatically
        factory.setAutomaticRecoveryEnabled(true);
        // attempt recovery every 10 seconds after a failure
        factory.setNetworkRecoveryInterval(10000);
        logMetacat.debug("IndexGenerator.init - Set RabbitMQ host to: " 
                         + RabbitMQhost);
        logMetacat.debug("IndexGenerator.init - Set RabbitMQ port to: " 
                         + RabbitMQport);

        // Setup the 'InProcess' queue with a routing key - messages consumed 
        //by this queue require that this routine key be used. The routine key 
        //INDEX_ROUTING_KEY sends messages to the index worker,
        try {
            rabbitMQconnection = factory.newConnection();
            RabbitMQChannelFactory channelFactory = new RabbitMQChannelFactory(rabbitMQconnection);
            channelPool = new GenericObjectPool<>(channelFactory);
        } catch (Exception e) {
            String error = "IndexGenerator.init - Cannot connect to the RabbitMQ queue: "
                            + INDEX_QUEUE_NAME + " at " + RabbitMQhost + " with port "
                            + RabbitMQport + " since " + e.getMessage();
            throw new ServiceException(error);
        }
        nThreads = Runtime.getRuntime().availableProcessors();
        nThreads--;                                 // Leave 1 main thread for execution
        nThreads = Math.max(1, nThreads);           // In case only 1 processor is available
        executor = Executors.newFixedThreadPool(nThreads);
        logMetacat.debug("The size of the thread pool to do the submission job is " + nThreads);
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
        logMetacat.debug("IndexGenerator.getLastSubdir - the last sub-directory is " 
                        + lastDir);
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
            String error = "IndexGenerator.publishToIndexQueue - "
                                    + "can't publish the index task for "
                                    + id.getValue() + " since the RabbitMQ channel pool"
                                    + " is null, which means Metacat cannot connect with RabbitMQ.";
            if (additionErrorMessage != null) {
                error = error + " And also Metacat can't save the failure index task into DB since "
                                + additionErrorMessage;
            }
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
            if (futures.size() >= MAX_TASK_SIZE) {
                //When it reaches the max size, we need to remove the complete futures from the
                // set. So we can avoid the issue of out of memory.
                HashStoreUpgrader.removeCompleteFuture(futures);
            }
        } catch (Exception e) {
            try {
                saveFailedTaskToDB(errorType, id, e.getMessage());
            } catch (SQLException sqle) {
                additionErrorMessage = sqle.getMessage();
            }
            String error = "IndexGenerator.publishToIndexQueue - "
                            + "can't publish the index task for "
                            + id.getValue() + " since "
                            + e.getMessage();
            if (additionErrorMessage != null) {
                error = error + ". And also Metacat can't save the failure index task into DB since "
                                + additionErrorMessage;
            }
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
        int times = 1;
        for (int i = 0; i <= times; i++) {
            boolean success = false;
            try {
                Channel channel = channelPool.borrowObject();
                try {
                    channel.basicPublish(
                        EXCHANGE_NAME, INDEX_ROUTING_KEY, basicProperties, null);
                    success = true;
                } catch (Exception ex) {
                    channelPool.invalidateObject(channel);
                    channel = null;
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
                    String error = "IndexGenerator.publishToIndexQueue - "
                        + "can't publish the index task for "
                        + id.getValue() + " since "
                        + e.getMessage();
                    if (additionErrorMessage1 != null) {
                        error = error
                            + ". And also Metacat can't save the failure index task into "
                            + "DB since "
                            + additionErrorMessage1;
                    }
                    logMetacat.error(error);
                }
            }
            if (success) {
                logMetacat.info(
                    "IndexGenerator.publish - The index task with the " + "object identifier "
                        + id.getValue() + ", the index type " + index_type
                        + " (null means Metacat doesn't have the object), " + " the priority "
                        + basicProperties.getPriority() + " was push into RabbitMQ with the exchange name "
                        + EXCHANGE_NAME + " at the " + i + "try.");
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
            channelPool.close();
            rabbitMQconnection.close();
            logMetacat.info("IndexGenerator.stop - stop the index queue service.");
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
     * Get the connection from the instance
     * @return  the connection in this instance
     */
    public static Connection getConnection() {
        return rabbitMQconnection;
    }
}
