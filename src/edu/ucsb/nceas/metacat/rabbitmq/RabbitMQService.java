package edu.ucsb.nceas.metacat.rabbitmq;

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
public class RabbitMQService extends BaseService {
    
    public final static String CREATE_INDEXT_TYPE = "create";
    public final static String DELETE_INDEX_TYPE = "delete";
    public final static String SYSMETA_CHANGE_TYPE = "sysmetaChange"; //this handle for resource map only
    
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
    private static RabbitMQService instance = null;
    
    private static Log logMetacat = LogFactory.getLog("RabbitMQService");
    
    /**
     * Private constructor
     */
    private RabbitMQService() {
        super();
        _serviceName="HazelcastService";
        try {
          init();
          
        } catch (ServiceException se) {
          logMetacat.error("There was a problem creating the HazelcastService. " +
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
        logMetacat.debug("RabbitMQService.init - Set RabbitMQ host to: " + RabbitMQhost);
        logMetacat.debug("RabbitMQService.init - Set RabbitMQ port to: " + RabbitMQport);

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
            logMetacat.info("RabbitMQService.init - Connected to RabbitMQ queue " + INDEX_QUEUE_NAME);
        } catch (Exception e) {
            logMetacat.error("RabbitMQService.init - Error connecting to RabbitMQ queue " + INDEX_QUEUE_NAME + " since " + e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }
    
    /**
     * Implement a Singleton pattern using "double checked locking" pattern.
     *
     * @return a singleton instance of the RabbitMQService class.
     */
    public static RabbitMQService getInstance() {
        if (instance == null) {
            synchronized (RabbitMQService.class) {
                if (instance == null) {
                    logMetacat.debug("Creating new controller instance");
                    instance = new RabbitMQService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Publish the given inforamtion to the index queue
     * @param id
     * @param type
     * @param sysmeta
     * @param followRevision
     */
    public void publishToIndexQueue(Identifier id, String type, SystemMetadata sysmeta,
                boolean followRevision ) throws ServiceException {
        if (id == null || id.getValue() == null || id.getValue().trim().equals("")) {
            throw new ServiceException("RabbitMQService.publishToIndexQueue - the identifier can't be null or blank.");
        }
        if (type == null || type.trim().equals("")) {
            throw new ServiceException("RabbitMQService.publishToIndexQueue - the index type can't be null or blank.");
        }
        try {
            AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                    .contentType("text/plain")
                    .deliveryMode(2) // set this message to persistent
                    .build();
            RabbitMQchannel.basicPublish(EXCHANGE_NAME, INDEX_ROUTING_KEY, basicProperties, id.getValue().getBytes());
        } catch (Exception e) {
            throw new ServiceException("RabbitMQService.publishToIndexQueue - can't publish the index task for " 
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
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

}
