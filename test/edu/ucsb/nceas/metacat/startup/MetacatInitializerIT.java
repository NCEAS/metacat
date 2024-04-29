package edu.ucsb.nceas.metacat.startup;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.index.queue.IndexGenerator;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.configuration.Configuration;
import org.dataone.configuration.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * The IT test class for the MetacatInitializer class
 * @author tao
 *
 */
@RunWith(Parameterized.class)
public class MetacatInitializerIT {
    private ServletContextEvent event = null;
    private Properties withProperties;

    private static final boolean isK8sForReal = Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"));
    private final boolean testAsContainerized;
    private MockedStatic<K8sAdminInitializer> k8sAdminInitMock;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return isK8sForReal ? Arrays.asList(new Object[][]{{true}})
                            : Arrays.asList(new Object[][]{{true}, {false}});
    }

    public MetacatInitializerIT(boolean testAsContainerized) {
        this.testAsContainerized = testAsContainerized;
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
    }

    @Before
    public void setUp() throws Exception {
        withProperties = new Properties();
        event = getMockServletContextEvent();
        LeanTestUtils.debug("MetacatInitializerIT: testAsContainerized = " + testAsContainerized);
        if (!isK8sForReal) {
            LeanTestUtils.setTestEnvironmentVariable(
                "METACAT_IN_K8S", String.valueOf(testAsContainerized));
        }
        if (testAsContainerized) {
            k8sAdminInitMock = Mockito.mockStatic(K8sAdminInitializer.class);
        }
    }

    @After
    public void tearDown() {
        if (testAsContainerized) {
            k8sAdminInitMock.close();
        }
    }

    /**
     * Test the contextInitialized method
     */
    @Test
    public void testContextInitialized() {
        //This should be a full-configured Metacat
        MetacatInitializer metacatInitializer = new MetacatInitializer();
        metacatInitializer.contextInitialized(event);
        assertTrue(MetacatInitializer.isFullyInitialized());

        withProperties.setProperty("configutil.authConfigured", PropertyService.UNCONFIGURED);
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {

            //reset the fully-initialized flag to be false by creating a new instance
            metacatInitializer = new MetacatInitializer();
            metacatInitializer.contextInitialized(event);
            assertFalse(MetacatInitializer.isFullyInitialized());
        }
    }

    /**
     * Test the contextInitialized method when Postgres is down.
     * We mock DBConnectionPool throwing an exception to simulate that Postgres is down.
     */
    @Test
    public void testPostgresInContextInitialized() {
       long time = System.currentTimeMillis();
        MetacatInitializer metacatInitializer;

        withProperties.setProperty("configutil.databaseConfigured", PropertyService.CONFIGURED);
        try (MockedStatic<ConfigurationUtil> mockCfg = Mockito.mockStatic(ConfigurationUtil.class);
             MockedStatic<DBConnectionPool> staticMockDBConnPool =
                 Mockito.mockStatic(DBConnectionPool.class)) {

            mockCfg.when(ConfigurationUtil::isMetacatConfigured).thenReturn(true);
            staticMockDBConnPool.when(DBConnectionPool::getInstance).thenThrow(
                new SQLException("org.postgresql.util.PSQLException: FATAL: database \"metacat"
                        + time + "\" does not exist"));

            try {
                metacatInitializer = new MetacatInitializer();
                metacatInitializer.contextInitialized(event);
                fail("The initialization should fail and the test cannot get here");
            } catch (RuntimeException e) {
                assertNotNull(
                    "e.getMessage() returned unexpected type of RuntimeException: " + e,
                    e.getMessage());
                assertTrue("Exception message DID NOT contain expected string: " + time
                               + ". Entire message was:\n\n" + e.getMessage()
                               + "\n\nfrom exception: " + e,
                           e.getMessage().contains(Long.toString(time)));
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
            assertFalse(MetacatInitializer.isFullyInitialized());
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
                               + ". Entire message was:\n\n" + e.getMessage()
                               + "\n\nfrom exception: " + e,
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
                assertFalse(MetacatInitializer.isFullyInitialized());
            }
        }
        IndexGenerator.refreshInstance();
    }

    private static Configuration createMockConfig(String newHost) {

        Configuration mockD1Config = Mockito.mock(Configuration.class);

        // override hostname...
        when(mockD1Config.getString(eq("index.rabbitmq.hostname"), anyString())).thenReturn(
            newHost);

        // ...but for remaining values, retrieve those that are already in Settings
        Configuration config = Settings.getConfiguration();
        when(mockD1Config.getInt(eq("index.rabbitmq.hostport"), anyInt()))
            .thenReturn(config.getInt("index.rabbitmq.hostport", 5672));
        when(mockD1Config.getString(eq("index.rabbitmq.username"), anyString()))
            .thenReturn(config.getString("index.rabbitmq.username", "guest"));
        when(mockD1Config.getString(eq("index.rabbitmq.password"), anyString()))
            .thenReturn(config.getString("index.rabbitmq.password", "guest"));
        when(mockD1Config.getInt(eq("index.rabbitmq.max.priority")))
            .thenReturn(config.getInt("index.rabbitmq.max.priority"));

        // node properties
        when(mockD1Config.getString(eq("dataone.nodeName")))
            .thenReturn(config.getString("dataone.nodeName"));
        when(mockD1Config.getString(eq("dataone.nodeId")))
            .thenReturn(config.getString("dataone.nodeId"));
        when(mockD1Config.getString(eq("dataone.subject")))
            .thenReturn(config.getString("dataone.subject"));
        when(mockD1Config.getString(eq("dataone.contactSubject")))
            .thenReturn(config.getString("dataone.contactSubject"));
        when(mockD1Config.getString(eq("dataone.nodeDescription")))
            .thenReturn(config.getString("dataone.nodeDescription"));
        when(mockD1Config.getString(eq("dataone.nodeType")))
            .thenReturn(config.getString("dataone.nodeType"));

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
