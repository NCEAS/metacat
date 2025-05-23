package edu.ucsb.nceas.metacat.startup;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.HashStoreConversionAdmin;
import edu.ucsb.nceas.metacat.admin.UpgradeStatus;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.index.queue.IndexGenerator;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

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
            //k8s doesn't care whether configutil.authConfigured - it relies on values.yaml
            assertEquals("Should always be true if running in k8s, and false otherwise",
                         System.getenv("METACAT_IN_K8S"),
                         String.valueOf(MetacatInitializer.isFullyInitialized()));
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
            //k8s doesn't care whether configutil.databaseConfigured - it relies on values.yaml
            assertEquals("Should always be true if running in k8s, and false otherwise",
                         System.getenv("METACAT_IN_K8S"),
                         String.valueOf(MetacatInitializer.isFullyInitialized()));        }
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

        // This is a configured metacat and the rabbitmq has a wrong host name.
        // So it should throw an exception during initialization on both non-k8s and k8s Metacats
        withProperties.setProperty("index.rabbitmq.hostname", newHost);
        try (MockedStatic<PropertyService> ps1Mock = LeanTestUtils.initializeMockPropertyService(
            withProperties)) {
            ps1Mock.when(() -> PropertyService.getInstance((ServletContext) any()))
                .thenReturn(Mockito.mock(PropertyService.class));
            try (MockedStatic<ConfigurationUtil> mockCfg = Mockito.mockStatic(
                ConfigurationUtil.class)) {
                mockCfg.when(ConfigurationUtil::isMetacatConfigured).thenReturn(true);
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
            }
        }
        // set Metacat non-configured temporarily.
        // Non-configured legacy Metacat wouldn't throw an exception. Non-configured k8s metacat
        // should throw an exception, though
        withProperties.setProperty("configutil.skinsConfigured", PropertyService.UNCONFIGURED);
        try (MockedStatic<PropertyService> psMock = LeanTestUtils.initializeMockPropertyService(
            withProperties)) {
            psMock.when(() -> PropertyService.getInstance((ServletContext) any()))
                .thenReturn(Mockito.mock(PropertyService.class));
            MetacatInitializer metacatInitializer = new MetacatInitializer();
            metacatInitializer.contextInitialized(event);
            //k8s doesn't care whether configutil.skinsConfigured - it relies on values.yaml
            assertEquals("Should always be true if running in k8s, and false otherwise",
                         System.getenv("METACAT_IN_K8S"),
                         String.valueOf(MetacatInitializer.isFullyInitialized()));
            assertFalse("Only non-k8s Metacats can get here",
                        Boolean.parseBoolean(System.getenv("METACAT_IN_K8S")));
        } catch (RuntimeException e) {
            // Non-configured k8s metacat should throw an exception from k8s initializer:
            assertTrue("Exception not expected, when NOT running in K8s",
                       Boolean.parseBoolean(System.getenv("METACAT_IN_K8S")));
            final String expected = newHost;
            assertTrue("Exception message DID NOT contain expected string: " + expected
                           + ". Entire message was:\n\n" + e.getMessage()
                           + "\n\nfrom exception: " + e,
                       e.getMessage().contains(expected));
        }
        IndexGenerator.refreshInstance();
    }

    /**
     * Test if the convertStorage method is called in the contextInitialized method
     *
     */
    @Test
    public void testConvertStorageInContextInitialized() throws Exception {
        long time = System.currentTimeMillis();

        withProperties.setProperty("configutil.propertiesConfigured", PropertyService.CONFIGURED);
        withProperties.setProperty("configutil.authConfigured", PropertyService.CONFIGURED);
        withProperties.setProperty("configutil.databaseConfigured", PropertyService.CONFIGURED);
        withProperties.setProperty("configutil.solrserverConfigured", PropertyService.CONFIGURED);
        withProperties.setProperty("configutil.dataoneConfigured", PropertyService.CONFIGURED);
        withProperties.setProperty("configutil.ezidConfigured", PropertyService.CONFIGURED);
        withProperties.setProperty("configutil.quotaConfigured", PropertyService.BYPASSED);
        withProperties.setProperty("application.backupDir", "build");
        try (MockedStatic<HashStoreConversionAdmin> mockStoreAdmin = Mockito.mockStatic(
            HashStoreConversionAdmin.class,
            withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS))) {
            // Since the conversion status is failed, the covertStorage will be kicked off (it
            // throws an exception by mocking) even though other configurations were done
            mockStoreAdmin.when(HashStoreConversionAdmin::getStatus)
                .thenReturn(UpgradeStatus.FAILED);
            MetacatInitializer mockInitializer = Mockito.mock(MetacatInitializer.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            Mockito.doThrow(new AdminException("Can't convert storage at " + time))
                .when(mockInitializer).convertStorage();
            if (testAsContainerized) {
                mockInitializer.contextInitialized(event);
            } else {
                try {
                    mockInitializer.contextInitialized(event);
                    fail("The initialization should fail and the test cannot get here");
                } catch (Exception e) {
                    assertNotNull(
                        "e.getMessage() returned unexpected type of RuntimeException: " + e,
                        e.getMessage());
                    assertTrue("Exception message DID NOT contain expected string: " + time
                                   + ". Entire message was:\n\n" + e.getMessage()
                                   + "\n\nfrom exception: " + e,
                               e.getMessage().contains(Long.toString(time)));
                }
            }
        }

        try (MockedStatic<HashStoreConversionAdmin> mockStoreAdmin = Mockito.mockStatic(
            HashStoreConversionAdmin.class,
            withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS))) {
            // Since the conversion status is not_required and other configuration were done, the
            // covertStorage will be NOT kicked off (it throws an exception by mocking)
            mockStoreAdmin.when(HashStoreConversionAdmin::getStatus)
                .thenReturn(UpgradeStatus.NOT_REQUIRED);
            MetacatInitializer mockInitializer = Mockito.mock(
                MetacatInitializer.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            Mockito.doThrow(
                    new AdminException("Can't convert storage at " + time + " does not " + "exist"))
                .when(mockInitializer).convertStorage();
            mockInitializer.contextInitialized(event);
        }

        try (MockedStatic<HashStoreConversionAdmin> mockStoreAdmin = Mockito.mockStatic(
            HashStoreConversionAdmin.class,
            withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS))) {
            // Since the conversion status is complete and other configuration were done, the
            // covertStorage will be NOT kicked off (it throws an exception by mocking)
            mockStoreAdmin.when(HashStoreConversionAdmin::getStatus)
                .thenReturn(UpgradeStatus.COMPLETE);
            MetacatInitializer mockInitializer = Mockito.mock(
                MetacatInitializer.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            Mockito.doThrow(
                    new AdminException("Can't convert storage at " + time + " does not " + "exist"))
                .when(mockInitializer).convertStorage();
            mockInitializer.contextInitialized(event);
        }

        // Test the scenario that initial status is in progress
        try (MockedStatic<HashStoreConversionAdmin> mockStoreAdmin = Mockito.mockStatic(
            HashStoreConversionAdmin.class,
            withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS))) {
            mockStoreAdmin.when(HashStoreConversionAdmin::getStatus)
                .thenReturn(UpgradeStatus.IN_PROGRESS).thenReturn(UpgradeStatus.IN_PROGRESS)
                .thenReturn(UpgradeStatus.COMPLETE);
            mockStoreAdmin.when(
                    () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)))
                .thenAnswer(invocation -> null);
            mockStoreAdmin.when(HashStoreConversionAdmin::convert)
                .thenAnswer(invocation -> null);
            MetacatInitializer mockInitializer = Mockito.mock(
                MetacatInitializer.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            mockInitializer.contextInitialized(event);
            if (testAsContainerized) {
                mockStoreAdmin.verify(
                    () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)),
                    times(0));
            } else {
                mockStoreAdmin.verify(
                    () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)),
                    times(1));
            }
        }
        // Test the scenario that initial status is complete
        try (MockedStatic<HashStoreConversionAdmin> mockStoreAdmin = Mockito.mockStatic(
            HashStoreConversionAdmin.class,
            withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS))) {
            mockStoreAdmin.when(HashStoreConversionAdmin::getStatus)
                .thenReturn(UpgradeStatus.FAILED).thenReturn(UpgradeStatus.COMPLETE)
                .thenReturn(UpgradeStatus.COMPLETE);
            mockStoreAdmin.when(
                    () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)))
                .thenAnswer(invocation -> null);
            mockStoreAdmin.when(HashStoreConversionAdmin::convert)
                .thenAnswer(invocation -> null);
            MetacatInitializer mockInitializer = Mockito.mock(
                MetacatInitializer.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            mockInitializer.contextInitialized(event);
            if (testAsContainerized) {
                mockStoreAdmin.verify(
                    () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)),
                    times(0));
            } else {
                mockStoreAdmin.verify(
                    () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)),
                    times(0));
            }
        }
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
