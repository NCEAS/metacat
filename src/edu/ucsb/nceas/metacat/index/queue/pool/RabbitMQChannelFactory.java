package edu.ucsb.nceas.metacat.index.queue.pool;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;

/**
 * This factory is used to create the RabbitMQ channels in the channel pool.
 * @author Tao
 */
public class RabbitMQChannelFactory implements PooledObjectFactory<Channel> {

    private Connection connection;

    /**
     * Constructor
     * @param connection  the rabbitmq connection which will be used to generate channels
     */
    public RabbitMQChannelFactory(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void activateObject(PooledObject<Channel> pooledObject) throws Exception {

    }

    @Override
    public void destroyObject(PooledObject<Channel> pooledObject) throws Exception {

    }

    @Override
    public PooledObject<Channel> makeObject() throws Exception {
        return null;
    }

    @Override
    public void passivateObject(PooledObject<Channel> pooledObject) throws Exception {

    }

    @Override
    public boolean validateObject(PooledObject<Channel> pooledObject) {
        return false;
    }

    @Override
    public void destroyObject(PooledObject<Channel> p, DestroyMode destroyMode) throws Exception {
        PooledObjectFactory.super.destroyObject(p, destroyMode);
    }
}
