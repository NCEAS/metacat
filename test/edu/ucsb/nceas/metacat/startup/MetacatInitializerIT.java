package edu.ucsb.nceas.metacat.startup;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import org.apache.commons.configuration.Configuration;
import org.dataone.configuration.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.mockito.MockedStatic;
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
    private Properties withProperties;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);

        withProperties = new Properties();
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

        withProperties.setProperty("configutil.authConfigured", PropertyService.UNCONFIGURED);
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {

            //reset the fully-initialized flag to be false by creating a new instance
            metacatInitializer = new MetacatInitializer();
            metacatInitializer.contextInitialized(event);
            assertFalse(metacatInitializer.isFullyInitialized());
        }
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

        MetacatInitializer metacatInitializer = new MetacatInitializer();

        withProperties.setProperty("database.connectionURI", newUri);

        try (MockedStatic<ConfigurationUtil> mockCfg = Mockito.mockStatic(ConfigurationUtil.class)) {
            mockCfg.when(() -> ConfigurationUtil.isMetacatConfigured()).thenReturn(true);
            try (MockedStatic<PropertyService> mock = LeanTestUtils.initializeMockPropertyService(
                withProperties)) {
                mock.when(() -> PropertyService.getInstance((ServletContext) any()))
                    .thenReturn(Mockito.mock(PropertyService.class));

                try {
                    metacatInitializer.contextInitialized(event);
                    fail("The initialization should fail and the test cannot get here");
                } catch (RuntimeException e) {
                    assertNotNull(
                        "e.getMessage() returned unexpected type of RuntimeException: " + e,
                        e.getMessage());
                    assertTrue(
                        "Exception message DID NOT contain expected string: " + time
                            + ". Entire message was:\n\n" + e.getMessage() + "\n\n<EOM>",
                        e.getMessage().contains(Long.toString(time)));
                }
            }
        }

        // set Metacat non-configured temporarily.
        // Non-configured Metacat shouldn't throw an exception.
        withProperties.setProperty("configutil.databaseConfigured", PropertyService.UNCONFIGURED);
        try (MockedStatic<PropertyService> mock = LeanTestUtils.initializeMockPropertyService(
            withProperties)) {
            mock.when(() -> PropertyService.getInstance((ServletContext) any()))
                .thenReturn(Mockito.mock(PropertyService.class));
            metacatInitializer = new MetacatInitializer();
            metacatInitializer.contextInitialized(event);
            assertFalse(metacatInitializer.isFullyInitialized());
        }
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

        Configuration mockD1Config = createMockConfig(newHost);

        try (MockedStatic<Settings> mock = Mockito.mockStatic(Settings.class)) {

            mock.when(Settings::getConfiguration).thenReturn(mockD1Config);

            IndexGenerator.refreshInstance();
            MetacatInitializer metacatInitializer = new MetacatInitializer();
            try {
                metacatInitializer.contextInitialized(event);
                fail("The initialization should fail and the test cannot get here");
            } catch (RuntimeException e) {
                assertTrue("Exception message DID NOT contain expected string: " + time
                               + ". Entire message was:\n\n" + e.getMessage() + "\n\n<EOM>",
                           e.getMessage().contains(Long.toString(time)));
            }

            // set Metacat non-configured temporarily.
            // Non-configured Metacat wouldn't throw an exception.
            withProperties.setProperty("configutil.skinsConfigured", PropertyService.UNCONFIGURED);
            try (MockedStatic<PropertyService> psMock = LeanTestUtils.initializeMockPropertyService(
                withProperties)) {
                psMock.when(() -> PropertyService.getInstance((ServletContext) any()))
                    .thenReturn(Mockito.mock(PropertyService.class));
                metacatInitializer = new MetacatInitializer();
                metacatInitializer.contextInitialized(event);
                assertFalse(metacatInitializer.isFullyInitialized());
            }
        }
    }

    private static Configuration createMockConfig(String newHost) {

        Configuration mockD1Config = Mockito.mock(Configuration.class);

        // override hostname...
        when(mockD1Config.getString(eq("index.rabbitmq.hostname"), anyString())).thenReturn(
            newHost);

        // ...but for remaining values, retrieve those that are already in Settings
        int port = Settings.getConfiguration().getInt("index.rabbitmq.hostport", 5672);
        String username = Settings.getConfiguration().getString("index.rabbitmq.username", "guest");
        String password = Settings.getConfiguration().getString("index.rabbitmq.password", "guest");
        int priority = Settings.getConfiguration().getInt("index.rabbitmq.max.priority");

        when(mockD1Config.getInt(eq("index.rabbitmq.hostport"), anyInt())).thenReturn(port);
        when(mockD1Config.getString(eq("index.rabbitmq.username"), anyString())).thenReturn(
            username);
        when(mockD1Config.getString(eq("index.rabbitmq.password"), anyString())).thenReturn(
            password);
        when(mockD1Config.getInt(eq("index.rabbitmq.max.priority"))).thenReturn(priority);

        return mockD1Config;
    }

    /**
     * Mock the ServletContextEvent to override getServletContext()
     * @return ServletContextEvent the mock ServletContextEvent
     * @throws PropertyNotFoundException
     */
    private ServletContextEvent getMockServletContextEvent() throws PropertyNotFoundException {
        ServletContext scMock = Mockito.mock(ServletContext.class);
        when(scMock.getRealPath("/")).thenReturn(SystemUtil.getContextDir());
        when(scMock.getRealPath("/WEB-INF"))
                                        .thenReturn(SystemUtil.getContextDir() + "/WEB-INF");
        when(scMock.getRealPath("WEB-INF"))
                                        .thenReturn(SystemUtil.getContextDir() + "/WEB-INF");
        when(scMock.getRealPath("/style/skins"))
                                        .thenReturn(SystemUtil.getContextDir() + "/style/skins");
        when(scMock.getAttribute("APPLICATION_NAME"))
                                                .thenReturn(MetacatInitializer.APPLICATION_NAME);
        ServletContextEvent servletContextEventMock = Mockito.mock(ServletContextEvent.class);
        when(servletContextEventMock.getServletContext()).thenReturn(scMock);
        return servletContextEventMock;
    }

}
