package edu.ucsb.nceas.metacat.index.queue;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;

import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;

/**
 * The RabbitMQService class will publish (send) the index information
 * to a RabbitMQ queue. A index worker will consume the information.
 * @author tao
 *
 */
public class IndexGenerator extends BaseService {
    
    //Those strings are the types of the index tasks.
    //The create is the index task type for the action when a new object was created. So the solr index will be generated.
    //delete is the index task type for the action when an object was deleted. So the solr index will be deleted
    //sysmeta is the index task type for the action when the system metadata of an existing object was updated. 
    public final static String CREATE_INDEXT_TYPE = "create";
    public final static String DELETE_INDEX_TYPE = "delete";
    public final static String SYSMETA_CHANGE_TYPE = "sysmeta"; //this handle for resource map only
    
    private final static String EXCHANGE_NAME = "dataone-index";
    private final static String INDEX_QUEUE_NAME = "index";
    private final static String INDEX_ROUTING_KEY = "index";
    
    // Default values for the RabbitMQ message broker server. The value of 'localhost' is valid for
    // a RabbitMQ server running on a 'bare metal' server, inside a VM, or within a Kubernetes
    // where Mmetacat and the RabbitMQ server are running in containers that belong
    // to the same Pod. These defaults will be used if the properties file cannot be read.
    private static String RabbitMQhost = Settings.getConfiguration().getString("index.rabbitmq.hostname", "localhost");
    private static int RabbitMQport = Settings.getConfiguration().getInt("index.rabbitmq.hostport", 5672);
    private static String RabbitMQusername = Settings.getConfiguration().getString("index.rabbitmq.username", "guest");
    private static String RabbitMQpassword = Settings.getConfiguration().getString("index.rabbitmq.password", "guest");
    private static com.rabbitmq.client.Connection RabbitMQconnection = null;
    private static com.rabbitmq.client.Channel RabbitMQchannel = null;
    private static IndexGenerator instance = null;
    
    private static Log logMetacat = LogFactory.getLog("IndexGenerator");
    
    /**
     * Private constructor
     */
    private IndexGenerator() {
        super();
        _serviceName="IndexQueueService";
        try {
          init();
        } catch (ServiceException se) {
          logMetacat.error("IndexGenerato.constructor - There was a problem creating the IndexGenerator. " +
                           "The error message was: " + se.getMessage());
        }
    }
    
    /**
     * Initialize the RabbitMQ service
     * @throws ServiceException
     */
    private void init() throws ServiceException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMQhost);
        factory.setPort(RabbitMQport);
        factory.setPassword(RabbitMQpassword);
        factory.setUsername(RabbitMQusername);
        // connection that will recover automatically
        factory.setAutomaticRecoveryEnabled(true);
        // attempt recovery every 10 seconds after a failure
        factory.setNetworkRecoveryInterval(10000);
        logMetacat.debug("IndexGenerator.init - Set RabbitMQ host to: " + RabbitMQhost);
        logMetacat.debug("IndexGenerator.init - Set RabbitMQ port to: " + RabbitMQport);

        // Setup the 'InProcess' queue with a routing key - messages consumed by this queue require that
        // this routine key be used. The routine key INDEX_ROUTING_KEY sends messages to the index worker,
        try {
            boolean durable = true;
            RabbitMQconnection = factory.newConnection();
            RabbitMQchannel = RabbitMQconnection .createChannel();
            RabbitMQchannel.exchangeDeclare(EXCHANGE_NAME, "direct", durable);

            boolean exclusive = false;
            boolean autoDelete = false;
            Map<String, Object> argus = null;
            RabbitMQchannel.queueDeclare(INDEX_QUEUE_NAME, durable, exclusive, autoDelete, argus);
            RabbitMQchannel.queueBind(INDEX_QUEUE_NAME, EXCHANGE_NAME, INDEX_ROUTING_KEY);
            
            // Channel will only send one request for each worker at a time.
            RabbitMQchannel.basicQos(1);
            logMetacat.info("IndexGenerator.init - Connected to RabbitMQ queue " + INDEX_QUEUE_NAME);
        } catch (Exception e) {
            logMetacat.error("IndexGenerator.init - Error connecting to RabbitMQ queue " + INDEX_QUEUE_NAME + " since " + e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }
    
    /**
     * Implement a Singleton pattern using "double checked locking" pattern.
     *
     * @return a singleton instance of the RabbitMQService class.
     */
    public static IndexGenerator getInstance() {
        if (instance == null) {
            synchronized (IndexGenerator.class) {
                if (instance == null) {
                    logMetacat.debug("IndexGenerator.getInstance - Creating new controller instance");
                    instance = new IndexGenerator();
                }
            }
        }
        return instance;
    }
    
    /**
     * Publish the given information to the index queue
     * @param id  the identifier of the object which will be indexed
     * @param index_type  the type of indexing, it can be delete, create or sysmeta
     * @param sysmeta  the system metadata associated with the id. This is optional
     */
    public void publishToIndexQueue(Identifier id, String index_type, SystemMetadata sysmeta) throws ServiceException {
        if (id == null || id.getValue() == null || id.getValue().trim().equals("")) {
            throw new ServiceException("IndexGenerator.publishToIndexQueue - the identifier can't be null or blank.");
        }
        if (index_type == null || index_type.trim().equals("")) {
            throw new ServiceException("IndexGenerator.publishToIndexQueue - the index type can't be null or blank.");
        }
        try {
            AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                    .contentType("text/plain")
                    .deliveryMode(2) // set this message to persistent
                    .build();
            RabbitMQchannel.basicPublish(EXCHANGE_NAME, INDEX_ROUTING_KEY, basicProperties, id.getValue().getBytes());
        } catch (Exception e) {
            throw new ServiceException("IndexGenerator.publishToIndexQueue - can't publish the index task for " 
                                        + id.getValue() + " since " + e.getMessage());
        }
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

}
