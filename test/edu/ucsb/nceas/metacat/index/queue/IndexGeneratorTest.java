package edu.ucsb.nceas.metacat.index.queue;


import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import edu.ucsb.nceas.LeanTestUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.TooFewActualInvocations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * A junit test for the IndexGenerator class
 * @author tao
 *
 */
public class IndexGeneratorTest {


    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }

    /**
     * Test the getLastSubdir method
     */
    @Test
    public void testGetLastSubdir() {
       String path = "/";
       String lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals(""));
       path = "/var/data/";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("data"));
       path = "/var/document";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("document"));
       path = "data";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("data"));
       path = "/var";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("var"));
       path = "/metacat/";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("metacat"));
       path = "//";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals(""));
       path = "///";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals(""));
    }

    /**
     * Test the publish method
     * @throws Exception
     */
    @Test
    public void testPublish() throws Exception {
        Channel mockedChannel = Mockito.mock(Channel.class);
        GenericObjectPool<Channel> mockedPool = Mockito.mock(GenericObjectPool.class);
        Mockito.when(mockedPool.borrowObject()).thenReturn(mockedChannel);
        IndexGenerator.setChannelPool(mockedPool);
        IndexGenerator generator = IndexGenerator.getInstance();
        assertEquals(mockedPool, IndexGenerator.getChannelPool());
        Identifier identifier = null;
        String index_type = "create";
        int priority = 3;
        try {
            generator.publish(identifier, index_type, priority);
            fail("Test can't get there since the publish method should throw an exception.");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        identifier = new Identifier();
        identifier.setValue("foo");
        generator.publish(identifier, index_type, priority);
        int times = 0;
        while (times < 200) {
            try {
                //Verify the channel object called the basicPublish method once
                Mockito.verify(mockedChannel, Mockito.times(1))
                    .basicPublish(anyString(), anyString(), any(AMQP.BasicProperties.class), any());
                break;
            } catch (TooFewActualInvocations e) {
                times++;
                Thread.sleep(10);
            }
        }
    }

}
