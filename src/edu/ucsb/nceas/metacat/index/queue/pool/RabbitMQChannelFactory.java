package edu.ucsb.nceas.metacat.index.queue.pool;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * This factory is used to create the RabbitMQ channels in the channel pool.
 * @author Tao
 */
public class RabbitMQChannelFactory extends BasePooledObjectFactory<Channel> {
    public final static String INDEX_QUEUE_NAME = "index";
    public final static String EXCHANGE_NAME = "dataone-index";
    public final static String INDEX_ROUTING_KEY = "index";
    private final static Log logMetacat = LogFactory.getLog(RabbitMQChannelFactory.class);
    private static int rabbitMQMaxPriority = 10;
    static {
        try {
            String priorityStr = PropertyService.getProperty("index.rabbitmq.max.priority");
            try {
                rabbitMQMaxPriority = Integer.parseInt(priorityStr);
            } catch (NumberFormatException e) {
                logMetacat.warn(
                    "The value of the property of index.rabbitmq.max.priority " + priorityStr
                        + " is not a number. So Metacat uses the default value "
                        + rabbitMQMaxPriority);
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.warn(
                "The the property of index.rabbitmq.max.priority is not set. So Metacat uses the "
                    + "default value "
                    + rabbitMQMaxPriority);
        }
    }

    private ConnectionFactory factory = new ConnectionFactory();
    private Connection connection = null;
    private String rabbitMQhost = "localhost";
    private int rabbitMQport = 5672;

    /**
     * Constructor
     */
    public RabbitMQChannelFactory() {
        init();
    }

    /**
     * Initialize the connection factory and connection
     */
    private void init() {
        // Default values for the RabbitMQ message broker server. The value of
        //'localhost' is valid for a RabbitMQ server running on a 'bare metal'
        //server, inside a VM, or within a Kubernetes where Metacat and the
        //RabbitMQ server are running in containers that belong to the same Pod.
        //These defaults will be used if the properties file cannot be read.
        try {
            rabbitMQhost = PropertyService.getProperty("index.rabbitmq.hostname");
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("The property of index.rabbitmq.hostname is not found and Metacat "
                                + "uses the default one " + rabbitMQhost);
        }
        try {
            String portStr = PropertyService.getProperty("index.rabbitmq.hostport");
            try {
                rabbitMQport = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                logMetacat.warn("The value of property of index.rabbitmq.hostport " + portStr
                                    + " is not a number and Metacat uses the default one "
                                    + rabbitMQport);
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("The property of index.rabbitmq.hostport is not found and Metacat "
                                + "uses the default one " + rabbitMQport);
        }
        String rabbitMQusername;
        try {
            rabbitMQusername = PropertyService.getProperty("index.rabbitmq.username");
        } catch (PropertyNotFoundException e) {
            rabbitMQusername = "guest";
        }
        String rabbitMQpassword;
        try {
            rabbitMQpassword = PropertyService.getProperty("index.rabbitmq.password");
        } catch (PropertyNotFoundException e) {
            rabbitMQpassword = "guest";
        }
        factory.setHost(rabbitMQhost);
        factory.setPort(rabbitMQport);
        factory.setPassword(rabbitMQpassword);
        factory.setUsername(rabbitMQusername);
        // connection that will recover automatically
        factory.setAutomaticRecoveryEnabled(true);
        // attempt recovery every 10 seconds after a failure
        factory.setNetworkRecoveryInterval(10000);
        logMetacat.debug("Set RabbitMQ host to: " + rabbitMQhost);
        logMetacat.debug("Set RabbitMQ port to: " + rabbitMQport);
        try {
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            logMetacat.error("RabbitMQ connection factory can't create a connection since "
                                 + e.getMessage());
        }
    }

    @Override
    public void destroyObject(PooledObject<Channel> pooledObject)
        throws IOException, TimeoutException {
        Channel channel = pooledObject.getObject();
        channel.close();
    }

    @Override
    public boolean validateObject(PooledObject<Channel> pooledObject) {
        Channel channel = pooledObject.getObject();
        return channel.isOpen();
    }

    @Override
    public Channel create() throws IOException, TimeoutException {
        synchronized (this) {
            if (connection == null || !connection.isOpen()) {
                // If connection is not healthy, recreate one.
                connection = factory.newConnection();
            }
        }
        if (connection != null) {
            // Create a new RabbitMQ channel with parameters.
            boolean durable = true;
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "direct", durable);
            boolean exclusive = false;
            boolean autoDelete = false;
            Map<String, Object> argus = new HashMap<>();
            argus.put("x-max-priority", rabbitMQMaxPriority);
            logMetacat.debug("Set RabbitMQ max priority to: " + rabbitMQMaxPriority);
            channel.queueDeclare(INDEX_QUEUE_NAME, durable,
                                 exclusive, autoDelete, argus);
            channel.queueBind(INDEX_QUEUE_NAME, EXCHANGE_NAME,
                              INDEX_ROUTING_KEY);
            return channel;
        } else {
            throw new IOException("RabbitMQ connection factory can't create a new connection.");
        }
    }

    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        return new DefaultPooledObject<>(channel);
    }

    /**
     * Get the negotiated maximum channel number.
     * @return the maximum channel number permitted for this connection
     * @throws IOException
     * @throws TimeoutException
     */
    public int getChannelMax() throws IOException, TimeoutException {
        synchronized (this) {
            if (connection == null || !connection.isOpen()) {
                // If connection is not healthy, recreate one.
                connection = factory.newConnection();
            }
        }
        return connection.getChannelMax();
    }

    /**
     * Get the host name of the rabbitmq service
     * @return the host name of the rabbitmq service
     */
    public String getRabbitMQhost() {
        return rabbitMQhost;
    }

    /**
     * Get the port number of the rabbitmq service
     * @return the port number of the rabbitmq service
     */
    public int getRabbitMQport() {
        return rabbitMQport;
    }
}
