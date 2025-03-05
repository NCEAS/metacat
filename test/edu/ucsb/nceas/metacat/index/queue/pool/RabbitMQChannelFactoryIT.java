package edu.ucsb.nceas.metacat.index.queue.pool;

import com.rabbitmq.client.Channel;
import edu.ucsb.nceas.LeanTestUtils;
import org.apache.commons.pool2.PooledObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The junit test class for RabbitMQChannelFactory
 * @author Tao
 */
public class RabbitMQChannelFactoryIT {

    RabbitMQChannelFactory factory;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        factory = new RabbitMQChannelFactory();
    }

    /**
     * Test the create/wrap/destroy/validate methods
     */
    @Test
    public void testCreateAndWrapAndDestroyAndValidateObject() throws Exception {
        Channel channel = factory.create();
        assertTrue(channel.isOpen());
        PooledObject<Channel> pooledObject = factory.wrap(channel);
        assertTrue(factory.validateObject(pooledObject));
        factory.destroyObject(pooledObject);
        assertFalse(channel.isOpen());
        assertFalse(factory.validateObject(pooledObject));
    }

}
