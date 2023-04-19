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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Provides common setup and runtime functionality that is needed in multiple tests, but without
 * incorporating JUnit3-specific functionality (e.g. no "extends TestCase"), and importantly,
 * without requiring a running instance of metacat to allow testing. This class should therefore be
 * most useful for fast-running Unit tests, or integration tests that do <em>not</em> rely on
 * querying metacat, solr, postgres, etc. Keep it lean, fast and lightweight! :-)
 */
@RunWith(JUnit4.class)
public class LeanTestUtils {

    private static final Path METACAT_PROPS_FILE_PATH = Paths.get("lib/metacat.properties");
    private static final Path TEST_PROPS_FILE_PATH = Paths.get("test/test.properties");
    private static boolean printDebug = false;
    private static Properties expectedProperties;

    public LeanTestUtils() {
        printDebug = Boolean.parseBoolean(getExpectedProperties().getProperty("test.printdebug"));
        debug("LeanTestUtils: 'test.printdebug' is TRUE");
    }

    public static void debug(String debugMessage) {
        if (printDebug) {
            System.err.println(debugMessage);
        }
    }

    public static Properties getExpectedProperties() {

        if (expectedProperties == null) {
            assertTrue("LeanTestUtils: properties files not found", doPropertiesFilesExist());
            try {

                Properties metacatProps = new Properties();
                metacatProps.load(Files.newBufferedReader(METACAT_PROPS_FILE_PATH));
                expectedProperties = new Properties(metacatProps);
                expectedProperties.load(Files.newBufferedReader(TEST_PROPS_FILE_PATH));
            } catch (IOException e) {
                fail("I/O exception trying to load properties from " + METACAT_PROPS_FILE_PATH
                    + " and " + TEST_PROPS_FILE_PATH);
            }
            assertFalse("LeanTestUtils: properties are EMPTY", expectedProperties.isEmpty());
        }
        return expectedProperties;
    }

    /**
     * Lightweight initialization of the PropertyService without needing to have a servlet context
     * available
     *
     * @return true on successful completion; false otherwise
     */
    public static boolean initializePropertyService() {
        boolean success = false;
        if (doPropertiesFilesExist()) {
            try {
                PropertyService propertyService =
                    PropertyService.getTestInstance(METACAT_PROPS_FILE_PATH, TEST_PROPS_FILE_PATH);
                success = (propertyService != null);
            } catch (ServiceException e) {
                debug("Unexpected problem initializing PropertyService" + e.getMessage());
                e.printStackTrace();
                success = false;
            }
        }
        return success;
    }


    //
    //        boolean success;
    //        final String APP_CONTEXT = "/metacat";
    //        final String CONFIG_DIR = "lib";
    //
    //        try {
    //            ServletContext servletContext = Mockito.mock(ServletContext.class);
    //            Mockito.when(servletContext.getRealPath("/")).thenReturn(APP_CONTEXT);
    //            Mockito.when(servletContext.getRealPath("WEB-INF")).thenReturn(CONFIG_DIR);
    //            Mockito.when(servletContext.getInitParameter("configFileDir"))
    //                .thenReturn(CONFIG_DIR);
    //            Mockito.when(servletContext.getContextPath()).thenReturn(APP_CONTEXT);
    //            Mockito.when(servletContext.getAttribute("APPLICATION_NAME"))
    //                .thenReturn(APP_CONTEXT);
    //
    //            ServiceService.getInstance(servletContext);
    //            PropertyService propSvc = PropertyService.getInstance(servletContext);
    //            assertNotNull(propSvc);
    //            assertFalse("PropertyService.getPropertyNames() was empty",
    //                PropertyService.getPropertyNames().isEmpty());
    //            success = true;
    //        } catch (Exception e) {
    //            // catch (Exception e) OK here - conservatively catch everything and mark as
    //            failed
    //            System.err.println("Error initializing properties:  " + e.getMessage());
    //            e.printStackTrace();
    //            success = false;
    //        }
    //        return success;
    //    }

    /**
     * Test other methods are working OK, since they are static, so can't be tagged as @Test
     *
     * A beneficial side effect is that there's no need to add it to the test excludes in build.xml
     */
    @Test
    public void bogusTestToKeepAntHappy() {
        assertTrue(doPropertiesFilesExist());
        assertTrue(initializePropertyService());
        assertNotNull(getExpectedProperties());
    }

    private static boolean doPropertiesFilesExist() {
        boolean success;
        if (METACAT_PROPS_FILE_PATH.toFile().exists()) {
            if (TEST_PROPS_FILE_PATH.toFile().exists()) {
                success = true;
            } else {
                debug("Couldn't find test properties file at: " + TEST_PROPS_FILE_PATH);
                success = false;
            }
        } else {
            debug("Couldn't find metacat properties file at: " + METACAT_PROPS_FILE_PATH);
            success = false;
        }
        return success;
    }
}
