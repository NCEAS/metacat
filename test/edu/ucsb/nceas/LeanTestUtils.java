package edu.ucsb.nceas;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        new LeanTestUtils();
    }

    public LeanTestUtils() {
        printDebug = Boolean.parseBoolean(getExpectedProperties().getProperty("test.printdebug"));
        debug("LeanTestUtils: 'test.printdebug' is TRUE");
    }

    /**
     * prints the passed message to System.err if the property {"test.printdebug"} is true. No
     * action otherwise
     *
     * @param debugMessage the message to be printed to System.err
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
     * Test other methods are working OK, since they are static, so can't be tagged as @Test. A
     * beneficial side effect is that there's no need to add this class to the test excludes in
     * build.xml
     */
    @Test
    public void verifyAllUtils() {
        assertTrue(doesPropertiesFileExist(DEFAULT_PROPS_FILE_PATH));
        assertTrue(doesPropertiesFileExist(SITE_PROPS_FILE_PATH));
        assertTrue(initializePropertyService(PropertiesMode.UNIT_TEST));
        Properties expectedProps = getExpectedProperties();
        assertNotNull(expectedProps);
        Path defaults = Paths.get(expectedProps.getProperty("metacat.contextDir"), "WEB-INF",
            "metacat.properties");
        assertEquals(defaults, getLiveDefaultPropsPath());
        Path site =
            Paths.get(expectedProps.getProperty(PropertyService.SITE_PROPERTIES_DIR_PATH_KEY),
                PropertyService.SITE_PROPERTIES_FILENAME);
        assertEquals(site, getLiveSitePropsPath(defaults));
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
