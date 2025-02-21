package edu.ucsb.nceas.metacat.index.queue.pool;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
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
    private final static String EXCHANGE_NAME = "dataone-index";
    private final static String INDEX_QUEUE_NAME = "index";
    private final static String INDEX_ROUTING_KEY = "index";
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

    private Connection connection;

    /**
     * Constructor
     * @param connection  the rabbitmq connection which will be used to generate channels
     */
    public RabbitMQChannelFactory(Connection connection) {
        this.connection = connection;
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
    public Channel create() throws IOException {
        // Create a new RabbitMQ channel with parameters.
        boolean durable = true;
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "direct", durable);
        boolean exclusive = false;
        boolean autoDelete = false;
        Map<String, Object> argus = new HashMap<>();
        argus.put("x-max-priority", rabbitMQMaxPriority);
        logMetacat.debug("IndexGenerator.init - Set RabbitMQ max priority to: "
                             + rabbitMQMaxPriority);
        channel.queueDeclare(INDEX_QUEUE_NAME, durable,
                                     exclusive, autoDelete, argus);
        channel.queueBind(INDEX_QUEUE_NAME, EXCHANGE_NAME,
                                  INDEX_ROUTING_KEY);
        return channel;
    }

    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        return new DefaultPooledObject<>(channel);
    }
}
