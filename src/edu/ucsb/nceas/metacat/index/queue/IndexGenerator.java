package edu.ucsb.nceas.metacat.index.queue;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.index.IndexEventDAO;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;

/**
 * The IndexGenerator class will publish (send) the index information
 * to a RabbitMQ queue. A index worker will consume the information.
 * @author tao
 *
 */
public class IndexGenerator extends BaseService {
    
    //Those strings are the types of the index tasks.
    //The create is the index task type for the action when a new object was 
    //created. So the solr index will be generated.
    //delete is the index task type for the action when an object was deleted. 
    //So the solr index will be deleted sysmeta is the index task type for the 
    //action when the system metadata of an existing object was updated. 
    public final static String CREATE_INDEX_TYPE = "create";
    public final static String DELETE_INDEX_TYPE = "delete";
    //this handle for resource map only
    public final static String SYSMETA_CHANGE_TYPE = "sysmeta"; 
    
    public final static int HIGHEST_PRIORITY = 10; // some special cases
    public final static int HIGH_PRIORITY = 6; //use for the special cases
    //use for the operations such as create, update, updateSystem, delete, archive
    public final static int MEDIUM_PRIORITY = 4; 
    //use for resource map objects in the operations such as create, update, 
    //updateSystem, delete, archive
    public final static int MEDIUM_RESOURCEMAP_PRIORITY = 3; 
    //use for the bulk operations such as reindexing the whole corpus 
    public final static int LOW_PRIORITY = 1; 
    //The header name in the message to store the identifier
    private final static String HEADER_ID = "id"; 
    //The header name in the message to store the path of the object 
    private final static String HEADER_PATH = "path"; 
    //The header name in the message to store the index type
    private final static String HEADER_INDEX_TYPE = "index_type"; 
    private final static String EXCHANGE_NAME = "dataone-index";
    private final static String INDEX_QUEUE_NAME = "index";
    private final static String INDEX_ROUTING_KEY = "index";

    private static Connection RabbitMQconnection = null;
    private static Channel RabbitMQchannel = null;
    private static IndexGenerator instance = null;

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
        int RabbitMQMaxPriority = Settings.getConfiguration().
                                            getInt("index.rabbitmq.max.priority");
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
            boolean durable = true;
            RabbitMQconnection = factory.newConnection();
            RabbitMQchannel = RabbitMQconnection .createChannel();
            RabbitMQchannel.exchangeDeclare(EXCHANGE_NAME, "direct", durable);

            boolean exclusive = false;
            boolean autoDelete = false;
            Map<String, Object> argus = new HashMap<String, Object>();
            argus.put("x-max-priority", RabbitMQMaxPriority);
            logMetacat.debug("IndexGenerator.init - Set RabbitMQ max priority to: " 
                            + RabbitMQMaxPriority);
            RabbitMQchannel.queueDeclare(INDEX_QUEUE_NAME, durable, 
                                        exclusive, autoDelete, argus);
            RabbitMQchannel.queueBind(INDEX_QUEUE_NAME, EXCHANGE_NAME, 
                                    INDEX_ROUTING_KEY);
            
            // Channel will only send one request for each worker at a time. 
            //This is only for consumer, so we comment it out.
            //see https://www.rabbitmq.com/consumer-prefetch.html
            //RabbitMQchannel.basicQos(1);
            logMetacat.info("IndexGenerator.init - Connected to RabbitMQ queue " 
                            + INDEX_QUEUE_NAME);
        } catch (Exception e) {
            String error = "IndexGenerator.init - Cannot connect to the RabbitMQ queue: "
                            + INDEX_QUEUE_NAME + " at " + RabbitMQhost + " with port "
                            + RabbitMQport + " since " + e.getMessage();
            throw new ServiceException(error);
        }
       
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
     * @param index_type  the type of indexing, it can be delete, create or sysmeta
     * @param priority  the priority of the index task
     */
    public void publish(Identifier id, String index_type, int priority) 
                                    throws ServiceException, InvalidRequest {
        if (id == null || id.getValue() == null 
                       || id.getValue().trim().equals("")) {
            throw new InvalidRequest("0000", 
                    "IndexGenerator.publishToIndexQueue - the identifier can't " 
                    + "be null or blank.");
        }
        if (index_type == null || index_type.trim().equals("")) {
            throw new InvalidRequest("0000", "IndexGenerator.publishToIndexQueue" 
                                 + " - the index type can't be null or blank.");
        }
        String errorType = IndexEvent.CREATE_FAILURE_TO_QUEUE;
        String additionErrorMessage = null;
        if (index_type.equals(DELETE_INDEX_TYPE)) {
            errorType = IndexEvent.DELETE_FAILURE_TO_QUEUE;
        }
        if (RabbitMQchannel == null) {
            try {
                saveFailedTaskToDB(errorType, id, "RabbitMQchannel is null");
            } catch (SQLException e) {
                additionErrorMessage = e.getMessage();
            }
            String error = "IndexGenerator.publishToIndexQueue - "
                                    + "can't publish the index task for "
                                    + id.getValue() + " since the RabbitMQ channel "
                                    + " is null, which means Metacat cannot connect with RabbitMQ.";
            if (additionErrorMessage != null) {
                error = error + " And also Metacat can't save the failure index task into DB since "
                                + additionErrorMessage;
            }
            throw new ServiceException(error);
        }
        try {
            Map<String, Object> headers = new HashMap<String, Object>();
            headers.put(HEADER_ID, id.getValue());
            headers.put(HEADER_INDEX_TYPE, index_type);
            String filePath = null;
            if (!index_type.equals(DELETE_INDEX_TYPE)) {
                //we don't need the file path from the delete type since the 
                //path was deleted.
                filePath = getFilePath(id);
            }
            if (filePath != null) {
                headers.put(HEADER_PATH, filePath);
            }
            AMQP.BasicProperties basicProperties =
                     new AMQP.BasicProperties.Builder()
                    .contentType("text/plain")
                    .deliveryMode(2) // set this message to persistent
                    .priority(priority)
                    .headers(headers)
                    .build();
            RabbitMQchannel.basicPublish(EXCHANGE_NAME, INDEX_ROUTING_KEY,
                                            basicProperties, null);
            logMetacat.info("IndexGenerator.publish - The index task with the "
                            + "object identifier " + id.getValue()
                            + ", the index type " + index_type
                            + ", the file path " + filePath
                            + " (null means Metacat doesn't have the object), "
                            + " the priority " + priority
                            + " was push into RabbitMQ with the exchange name "
                            + EXCHANGE_NAME);
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
     * Get the relative file path for the identifier. 
     * This relative path is based on application.datafilepath (for data files) 
     * or application.documentfilepath (for document files)
     * For example, autogen.1.1 is the docid for guid foo.1 and it is a metadata 
     * object. The metadata objects are stored in the path 
     * /var/metacat/document/autogen.1.1. 
     * Since the application.documentfilepath is "/var/metacat/document", 
     * the relative file path will be autogen.1.1.
     * Note, the value can be null since cn doesn't store data objects.
     * @param id  the guid of object
     * @return  the relative file path
     * @throws ServiceException
     */
    protected static String getFilePath(Identifier id) throws ServiceException {
        String path = null;
        if (id == null || id.getValue() == null 
                || id.getValue().trim().equals("")) {
            throw new ServiceException("IndexGenerator.getFilePath - the " 
                                       + "identifier can't be null or blank.");
        }
        String docid = null;
        try {
            docid = IdentifierManager.getInstance().getLocalId(id.getValue());
        } catch (McdbDocNotFoundException e) {
            logMetacat.info("IndexGenerator.getFilePath - Metacat can't find " 
                            + "the docid for the identifier " + id.getValue() 
                            + ". This is possible since CN doesn't harvest data" 
                            + " objects at all.");
        } catch (SQLException e) {
           throw new ServiceException(e.getMessage());
        }
        if (docid != null) {
            path = docid;
        }
        logMetacat.debug("IndexGenerator.getFilePath - The relative file path " 
                      + "for the identifier " + id.getValue() + " is " + path);
        return path;
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
            RabbitMQchannel.close();
            RabbitMQconnection.close();
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

}
