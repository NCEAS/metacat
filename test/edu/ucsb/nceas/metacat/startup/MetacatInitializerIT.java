package edu.ucsb.nceas.metacat.startup;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.dataone.configuration.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mockito.Mockito;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.index.queue.IndexGenerator;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * The IT test class for the MetacatInitializer class
 * @author tao
 *
 */
public class MetacatInitializerIT {
    private ServletContextEvent event = null;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
        event = getMockServletContextEvent();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test the contextInitialized method
     * @throws IOException
     */
    @Test
    public void testContextInitialized() throws Exception {
        //This should be a full-configured Metacat
        MetacatInitializer metacatInitializer = new MetacatInitializer();
        metacatInitializer.contextInitialized(event);
        assertTrue(metacatInitializer.isFullyInitialized());
        // set Metacat non-configured temporarily.
        PropertyService
             .setPropertyNoPersist("configutil.authConfigured", PropertyService.UNCONFIGURED);
        //reset the fully-initialized flag to be false by creating a new instance
        metacatInitializer = new MetacatInitializer();
        metacatInitializer.contextInitialized(event);
        assertFalse(metacatInitializer.isFullyInitialized());
        PropertyService
                    .setPropertyNoPersist("configutil.authConfigured", PropertyService.CONFIGURED);
    }

    /**
     * Test the contextInitialized method when Postgres is down.
     * We use a wrong connection uri to simulate that Postgres is down.
     * @throws IOException
     */
    @Test
    public void testPostgresInContextInitialized() throws Exception {
        String originUri = PropertyService.getProperty("database.connectionURI");
        long time = System.currentTimeMillis();
        String newUri = originUri + time;
        PropertyService.setPropertyNoPersist("database.connectionURI", newUri);
        MetacatInitializer metacatInitializer = new MetacatInitializer();
        try {
            metacatInitializer.contextInitialized(event);
            fail("The initialization should fail and the test cannot get here");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains(Long.toString(time)));
        }

        // set Metacat non-configured temporarily.
        // Non-configured Metacat wouldn't throw an exception.
        PropertyService
             .setPropertyNoPersist("configutil.databaseConfigured", PropertyService.UNCONFIGURED);
        metacatInitializer = new MetacatInitializer();
        metacatInitializer.contextInitialized(event);
        assertFalse(metacatInitializer.isFullyInitialized());

        // set the original properties back
        PropertyService.setPropertyNoPersist("database.connectionURI", originUri);
        PropertyService
            .setPropertyNoPersist("configutil.databaseConfigured", PropertyService.CONFIGURED);
    }

    /**
     * Test the contextInitialized method when RabbitMQ is down.
     * We use a wrong connection uri to simulate that Postgres is down.
     * @throws IOException
     */
    @Test
    public void testRabbitMqInContextInitialized() throws Exception {
        String originHost = PropertyService.getProperty("index.rabbitmq.hostname");
        long time = System.currentTimeMillis();
        String newHost = originHost + time;
        Settings.getConfiguration().setProperty("index.rabbitmq.hostname", newHost);
        IndexGenerator.refreshInstance();
        MetacatInitializer metacatInitializer = new MetacatInitializer();
        try {
            metacatInitializer.contextInitialized(event);
            fail("The initialization should fail and the test cannot get here");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains(Long.toString(time)));
        }

        // set Metacat non-configured temporarily.
        // Non-configured Metacat wouldn't throw an exception.
        PropertyService
             .setPropertyNoPersist("configutil.skinsConfigured", PropertyService.UNCONFIGURED);
        metacatInitializer = new MetacatInitializer();
        metacatInitializer.contextInitialized(event);
        assertFalse(metacatInitializer.isFullyInitialized());

        // set the original properties back
        Settings.getConfiguration().setProperty("index.rabbitmq.hostname", originHost);
        PropertyService
            .setPropertyNoPersist("configutil.skinsConfigured", PropertyService.CONFIGURED);
        IndexGenerator.refreshInstance();
    }

    /**
     * Mock the ServletContextEvent to override getServletContext()
     * @return ServletContextEvent the mock ServletContextEvent
     * @throws PropertyNotFoundException
     */
    private ServletContextEvent getMockServletContextEvent() throws PropertyNotFoundException {
        ServletContext scMock = Mockito.mock(ServletContext.class);
        Mockito.when(scMock.getRealPath("/")).thenReturn(SystemUtil.getContextDir());
        Mockito.when(scMock.getRealPath("/WEB-INF"))
                                        .thenReturn(SystemUtil.getContextDir() + "/WEB-INF");
        Mockito.when(scMock.getRealPath("WEB-INF"))
                                        .thenReturn(SystemUtil.getContextDir() + "/WEB-INF");
        Mockito.when(scMock.getRealPath("/style/skins"))
                                        .thenReturn(SystemUtil.getContextDir() + "/style/skins");
        Mockito.when(scMock.getAttribute("APPLICATION_NAME"))
                                                .thenReturn(MetacatInitializer.APPLICATION_NAME);
        ServletContextEvent servletContextEventMock = Mockito.mock(ServletContextEvent.class);
        Mockito.when(servletContextEventMock.getServletContext()).thenReturn(scMock);
        return servletContextEventMock;
    }

}
