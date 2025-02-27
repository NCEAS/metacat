package edu.ucsb.nceas.metacat.index.queue;


import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.TooFewActualInvocations;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private Properties withProperties;
    MockedStatic<PropertyService> closeableMock;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        withProperties = new Properties();
        withProperties.setProperty("index.submitting.set.size", "3000000");
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
    }

    @After
    public void tearDown() throws Exception {
        if (!closeableMock.isClosed()) {
            closeableMock.close();
        }
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

    /**
     * Test to access (clear) the feature set which holds the submitting index tasks' features by
     * multiple threads
     * @throws Exception
     */
    @Test
    public void testClearFeatureSetByMultipleThread() throws Exception {
        Channel mockedChannel = Mockito.mock(Channel.class);
        GenericObjectPool<Channel> mockedPool = Mockito.mock(GenericObjectPool.class);
        Mockito.when(mockedPool.borrowObject()).thenReturn(mockedChannel);
        IndexGenerator.setChannelPool(mockedPool);
        IndexGenerator generator = IndexGenerator.getInstance();
        assertEquals(mockedPool, IndexGenerator.getChannelPool());
        Identifier identifier = null;
        String index_type = "create";
        int priority = 3;
        // Create 280000 features
        int numberOfFutures = 1000000;
        for (int i = 0 ; i < numberOfFutures; i++) {
            identifier = new Identifier();
            identifier.setValue("foo" + i );
            generator.publish(identifier, index_type, priority);
        }
        Set<Future> futures = IndexGenerator.getFutures();
        // It may contain other features from other test
        assertTrue(futures.size() >= numberOfFutures);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        Future future1 = executor.submit(() -> {
            HashStoreUpgrader.removeCompleteFuture(futures);
            return Integer.parseInt("1");
        });
        Future future2 = executor.submit(() -> {
            HashStoreUpgrader.removeCompleteFuture(futures);
            return Integer.parseInt("2");
        });
        Future future3 = executor.submit(() -> {
            HashStoreUpgrader.removeCompleteFuture(futures);
            return Integer.parseInt("3");
        });
        while (!future1.isDone() && !future2.isDone() && !future3.isDone()) {
            Thread.sleep(500);
        }
        assertEquals(1,future1.get());
        assertEquals(2,future2.get());
        assertEquals(3,future3.get());
        assertTrue(futures.size() < numberOfFutures);
    }

}
