package edu.ucsb.nceas;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Provides common setup and runtime functionality that is needed in multiple tests, but without
 * incorporating JUnit3-specific functionality (e.g. no "extends TestCase"), and importantly,
 * WITHOUT REQUIRING A RUNNING INSTANCE OF METACAT to allow testing. This class should therefore be
 * most useful for fast-running Unit tests, or integration tests that do <em>not</em> rely on
 * querying metacat, solr, postgres, etc. Keep it lean, fast and lightweight! :-)
 */
@RunWith(JUnit4.class)
public class LeanTestUtils {

    private static final Path DEFAULT_PROPS_FILE_PATH = Paths.get("lib/metacat.properties");
    private static final Path SITE_PROPS_FILE_PATH = Paths.get("test/test.properties");
    private static boolean printDebug = false;
    private static Properties expectedProperties;

    static {
        printDebug = Boolean.parseBoolean(getExpectedProperties().getProperty("test.printdebug"));
        debug("LeanTestUtils: 'test.printdebug' is TRUE");
    }

    /**
     * Prints the passed message to <code>System.err</code> if the property {"test.printdebug"} is
     * true. No action otherwise
     *
     * @param debugMessage the message to be printed to <code>System.err</code>
     */
    public static void debug(String debugMessage) {
        if (printDebug) {
            System.err.println(debugMessage);
        }
    }

    /**
     * Get a Properties object containing the default properties from lib/metacat.properties,
     * overlaid with the test properties from test/test.properties. These can be used to compare
     * with results for test assertions, or can be used to retrieve test-specific properties such as
     * the debug flag ("test.printdebug") or the metacat deployment location ("metacat.contextDir")
     *
     * @return java.util.Properties object containing the default properties from
     * lib/metacat.properties, overlaid with the test properties from test/test.properties
     */
    public static Properties getExpectedProperties() {

        if (expectedProperties == null) {
            assertTrue("LeanTestUtils.getExpectedProperties(): default properties files not found",
                doesPropertiesFileExist(DEFAULT_PROPS_FILE_PATH));
            assertTrue("LeanTestUtils.getExpectedProperties(): site properties files not found",
                doesPropertiesFileExist(SITE_PROPS_FILE_PATH));
            try {
                Properties metacatProps = new Properties();
                metacatProps.load(Files.newBufferedReader(DEFAULT_PROPS_FILE_PATH));
                expectedProperties = new Properties(metacatProps);
                expectedProperties.load(Files.newBufferedReader(SITE_PROPS_FILE_PATH));
            } catch (IOException e) {
                fail("I/O exception trying to load properties from " + DEFAULT_PROPS_FILE_PATH
                    + " and " + SITE_PROPS_FILE_PATH);
            }
            assertFalse("LeanTestUtils: properties are EMPTY", expectedProperties.isEmpty());
        }
        return expectedProperties;
    }

    /**
     * Lightweight initialization of the PropertyService without needing to have a servlet context
     * available.
     *
     * @param mode enum parameter defining how the PropertyService should be set up for testing:
     *             PropertiesMode.UNIT_TEST - unit/integration testing using properties in the dev
     *             repo, or PropertiesMode.LIVE_TEST - "live" testing" using properties in their
     *             "deployed" locations. @see <code>PropertiesMode</code>
     * @return true on successful completion; false otherwise
     */
    public static boolean initializePropertyService(PropertiesMode mode) {

        Path defaultPropsFilePath = (mode == PropertiesMode.UNIT_TEST) ?
            DEFAULT_PROPS_FILE_PATH :
            getLiveDefaultPropsPath();
        Path sitePropsFilePath = (mode == PropertiesMode.UNIT_TEST) ?
            SITE_PROPS_FILE_PATH :
            getLiveSitePropsPath(defaultPropsFilePath);

        boolean success;
        try {
            PropertyService propertyService =
                PropertyService.getInstanceForTesting(defaultPropsFilePath, sitePropsFilePath);
            success = (propertyService != null);
        } catch (ServiceException e) {
            debug("LeanTestUtils: Unexpected problem initializing PropertyService in " + mode
                + " mode: " + e.getMessage() + " -- defaultPropsFilePath was: "
                + defaultPropsFilePath + "and sitePropsFilePath was: " + sitePropsFilePath);
            e.printStackTrace();
            success = false;
        }
        debug("LeanTestUtils: PropertyService initialized in " + mode
            + " mode; using default properties from " + defaultPropsFilePath
            + " overlaid with configurable properties from " + sitePropsFilePath);
        return success;
    }

    /**
     * Initialize a static, mocked version of the PropertyService to use in testing. The mock is
     * initialized with the key=value pairs contained in the Properties object provided
     * ('withProperties'), which must contain at least one key-value pair. If a getProperty()
     * call is made with any key that is not included in `withProperties`, the result will be a
     * call to the original, non-mocked PropertyService class.
     *
     * <p><em>IMPORTANT:</em> from the <a href=
     * "https://javadoc.io/static/org.mockito/mockito-core/3.12.4/org/mockito/MockedStatic.html">
     * org.mockito.MockedStatic Interface javadoc</a>: "The mocking only affects the thread on
     * which this static mock was created, and it is not safe to use this object from another
     * If this thread. The static mock is released when this object's ScopedMock.close() method
     * is invoked. object is never closed, the static mock will remain active on the initiating
     * thread. It is therefore recommended to create this object within a try-with-resources
     * statement unless when managed explicitly, for example by using a JUnit rule or extension."
     * </p><p>
     * <em>Therefore, close() should be called on the returned mock to clean up after testing</em>;
     * see examples below:
     * </p><p>
     * Usage examples:
     * </p><ol><li><p>
     *     (cleanest) try-with-resources approach - automatically does cleanup on exiting block:
     * </p><code>
     *     Properties withProps = new Properties();
     *     withProps.setProperty("server.name", "UpdateDOITestMock.edu");
     *     try (MockedStatic<PropertyService> mock =
     *             LeanTestUtils.initializeMockPropertyService(withProps)) {
     *         mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod(); // for ex.
     *         // your test code here
     *     }
     * </code></li><li><p>
     *     (more verbose) JUnit @Before/setUp() and @After/tearDown(), or within test method itself:
     * </p><code>
     *     //global variable:
     *         MockedStatic<PropertyService> mock
     *     [...]
     *     // include in a test method, or in a @Before/setUp() method:
     *         Properties withProps = new Properties();
     *         withProps.setProperty("server.name", "UpdateDOITestMock.edu");
     *         mock = LeanTestUtils.initializeMockPropertyService(withProps);
     *         mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod(); // for ex.
     *
     *     // include at the end of the same test method, or in an @After/tearDown() method:
     *         try {
     *             mock.close();
     *         } catch (Exception e) {
     *             // probably no need to handle - just a housekeeping failure
     *         }
     * </code></li></ol>
     *
     * @param withProperties key-value pairs for the mock to return on receiving
     *                       calls to getProperty(). Must contain at least one key-value pair,
     *                       otherwise a test <code>fail</code> is triggered
     * @return AutoCloseable object, allowing the caller to invoke close() (either explicitly, or
     *         implicitly via try-with-resources) after testing is finished
     */
    public static MockedStatic<PropertyService> initializeMockPropertyService(Properties withProperties) {
        if (withProperties == null || withProperties.keySet().isEmpty()) {
            fail("LeanTestUtils.initializeMockPropertyService() received "
                    + ((withProperties == null) ? "NULL" : "EMPTY") + " 'withProperties' object");
        }
        MockedStatic<PropertyService> mock =
                Mockito.mockStatic(PropertyService.class);

        for (Object key : withProperties.keySet()) {
            final String keyStr = (String) key;
            mock.when(() -> PropertyService.getProperty(eq(keyStr)))
                    .thenReturn(withProperties.getProperty(keyStr));
        }
        // Ensure original PropertyService method called for any keys that are NOT included
        // in the withProperties object
        mock.when(() -> PropertyService.getProperty(argThat(
                (String s) -> !withProperties.containsKey(s)))).thenCallRealMethod();

        debug("LeanTestUtils: Mock PropertyService initialized, using provided properties: "
                + Arrays.toString(withProperties.keySet().toArray()));
        return mock;
    }


    /**
     * Test other methods are working OK, since they are static, so can't be tagged as @Test.
     * Beneficial side effects: (1) there's no need to add this class to the test excludes in
     * build.xml; (2) provides examples of how to use the methods in this class
     *
     * <em>Please add verifications here for any future methods you add!</em>
     */
    @Test
    public void verifyAllUtils() throws Exception {
        assertTrue(doesPropertiesFileExist(DEFAULT_PROPS_FILE_PATH));
        assertTrue(doesPropertiesFileExist(SITE_PROPS_FILE_PATH));
        assertTrue(initializePropertyService(PropertiesMode.UNIT_TEST));
        Properties expectedProps = getExpectedProperties();
        assertNotNull(expectedProps);
        Path defaults = Paths.get(expectedProps.getProperty("metacat.contextDir"), "WEB-INF",
            "metacat.properties");
        Path site =
            Paths.get(expectedProps.getProperty(PropertyService.SITE_PROPERTIES_DIR_PATH_KEY),
                PropertyService.SITE_PROPERTIES_FILENAME);
        assertNotNull("Default Properties path shouldn't be null!", defaults);
        assertNotNull("Site Properties path shouldn't be null!", site);

        String key = "server.name";
        String value = "UpdateDOITestMock.edu";
        Properties withProperties = new Properties();
        withProperties.setProperty(key, value);
        try (AutoCloseable ignored = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            assertEquals("Mock verification failed", value,
                    PropertyService.getProperty(key));
        } catch (GeneralPropertyException e) {
            fail("verifyAllUtils() Problem calling PropertyService: " + e.getMessage());
        }
    }

    private static Path getLiveDefaultPropsPath() {

        String metacatContextDir = getExpectedProperties().getProperty("metacat.contextDir");
        Path metacatPropsFilePath = Paths.get(metacatContextDir, "WEB-INF", "metacat.properties");
        if (!doesPropertiesFileExist(metacatPropsFilePath)) {
            fail("Problem finding deployed metacat.properties at " + metacatPropsFilePath
                + ". Check that you have started Tomcat (so the WAR files have been exploded), and "
                + "that test/test.properties contains the correct value for {metacat.contextDir}");
        }
        return metacatPropsFilePath;
    }

    private static Path getLiveSitePropsPath(Path metacatPropsPath) {

        Properties tempMetacatProps = null;
        try {
            tempMetacatProps = new Properties();
            tempMetacatProps.load(Files.newBufferedReader(metacatPropsPath));
        } catch (IOException e) {
            e.printStackTrace();
            fail("I/O exception trying to load properties from " + DEFAULT_PROPS_FILE_PATH);
        }
        Path sitePropsFilePath =
            Paths.get(tempMetacatProps.getProperty(PropertyService.SITE_PROPERTIES_DIR_PATH_KEY),
                PropertyService.SITE_PROPERTIES_FILENAME);
        if (!doesPropertiesFileExist(sitePropsFilePath)) {
            fail("Problem finding deployed metacat-site.properties at " + sitePropsFilePath
                + ". Check metacat.properties contains location for metacat-site.properties - key: "
                + PropertyService.SITE_PROPERTIES_DIR_PATH_KEY);
        }
        return sitePropsFilePath;
    }

    private static boolean doesPropertiesFileExist(Path propsPath) {
        boolean success;
        if (propsPath.toFile().exists()) {
            success = true;
        } else {
            debug("LeanTestUtils: Couldn't find properties file at: " + propsPath);
            success = false;
        }
        return success;
    }


    /**
     * Defines how the PropertyService should be set up for testing
     */
    public enum PropertiesMode {
        /**
         * UNIT_TEST - unit/integration testing using files from the local metacat dev directory:
         * <code>lib/metacat.properties</code> overlaid with <code>test/test.properties</code>
         */
        UNIT_TEST,

        /**
         * LIVE_TEST - "live" testing, using files from the deployed locations:
         * <code>[TOMCAT_WEBAPPS]/[metacat.context]/WEB-INF/metacat.properties</code> overlaid with
         * <code>[application.sitePropertiesDir]/metacat-site.properties</code>
         */
        LIVE_TEST
    }
}
