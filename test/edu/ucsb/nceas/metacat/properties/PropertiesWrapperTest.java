package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import static org.junit.Assert.*;

@RunWith(Suite.class)
// Add new inner classes for tests grouped by functionality, to help with clarity, and so we
// don't have to share @Before setup code where it's not needed. Each new inner class name should
// be added the @Suite.SuiteClasses test suite definition, below
@Suite.SuiteClasses({
    PropertiesWrapperTest.EnvironmentVariablesTestSuite.class,
    PropertiesWrapperTest.WriteableDefaultsTestSuite.class
})
public class PropertiesWrapperTest {

    /**
     * Tests that exercise the ability to write values directly to the default properties
     * file, bypassing the site properties overlay functionality
     *
     * TODO: Eliminate the need for this mechanism! It is here for legacy reasons;
     *       See GitHub Issue #1638: https://github.com/NCEAS/metacat/issues/1638
     *
     * Whenever setPropertyNoPersist() is called, the code checks to see if the passed propertyName
     * parameter is listed in 'PropertiesWrapper.WRITEABLE_DEFAULTS'. If so, the property is
     * added directly to 'defaultProperties' instead of to 'mainProperties'. It will therefore end
     * up being written to the 'metacat.properties' file instead of the 'metacat-site.properties'
     * file. This breaks the design intent of the Properties setup, and has been done as a temporary
     * workaround preceding incremental improvements.
     */
    public static class WriteableDefaultsTestSuite {

        private final Random random = new Random();
        private static final String TEST_KEY = "test.testProperty";

        @Before
        public void setUp() {
            LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
            assertFalse(PropertiesWrapper.WRITEABLE_DEFAULTS.contains(TEST_KEY));
        }

        @After
        public void tearDown() throws Exception {
            LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
            PropertyService.setProperty(TEST_KEY, "");
            assertEquals("", LeanTestUtils.getLatestPropertiesFromDisk().getProperty(TEST_KEY));
        }

        /**
         * Sanity check - non-persistent set for a property that doesn't exist in WRITEABLE_DEFAULTS
         */
        @Test
        public void setPropertyNoPersist_nonWriteableDefault() throws GeneralPropertyException {
            String testValue = "testingNotWriteable-" + random.nextInt();
            PropertyService.setPropertyNoPersist(TEST_KEY, testValue);
            assertEquals(testValue, PropertyService.getProperty(TEST_KEY));
            //shouldn't have been written to disk yet:
            assertNotEquals(testValue,
                            LeanTestUtils.getLatestPropertiesFromDisk().getProperty(TEST_KEY));
            //and should NEVER be in the defaultProperties:
            assertNotEquals(testValue,
                         PropertiesWrapper.getInstance().defaultProperties.getProperty(TEST_KEY));
        }

        /**
         * Sanity check - persist a property that doesn't exist in WRITEABLE_DEFAULTS
         */
        @Test
        public void setProperty_nonWriteableDefault() throws GeneralPropertyException {
            String testValue = "testingNotWriteable-" + random.nextInt();
            PropertyService.setPropertyNoPersist(TEST_KEY, testValue);
            PropertyService.persistProperties();
            //now it should be on disk:
            assertEquals(testValue,
                         LeanTestUtils.getLatestPropertiesFromDisk().getProperty(TEST_KEY));
        }

        /**
         * nonPersistent write to a property that DOES exist in WRITEABLE_DEFAULTS
         */
        @Test
        public void setPropertyNoPersist_writeableDefault() throws GeneralPropertyException {
            String testValue = "testingWriteable-" + random.nextInt();
            assertTrue(PropertiesWrapper.WRITEABLE_DEFAULTS.size() > 1);
            String writeableKey = PropertiesWrapper.WRITEABLE_DEFAULTS.get(1);
            assertNotEquals(writeableKey, PropertyService.SITE_PROPERTIES_DIR_PATH_KEY);
            LeanTestUtils.debug("setPropertyNoPersist_writeableDefault() using key: "
                + writeableKey);

            PropertyService.setPropertyNoPersist(writeableKey, testValue);
            assertEquals(testValue, PropertyService.getProperty(writeableKey));

            //shouldn't have been written to disk yet:
            assertNotEquals(testValue,
                            LeanTestUtils.getLatestPropertiesFromDisk().getProperty(writeableKey));

            //and should be in the defaultProperties:
            assertEquals(testValue,
            PropertiesWrapper.getInstance().defaultProperties.getProperty(writeableKey));
        }


        /**
         * Persistent write to a property that DOES exist in WRITEABLE_DEFAULTS. Should be
         * written to metacat.properties, NOT to site properties!
         */
        @Test
        public void setProperty_writeableDefault() throws GeneralPropertyException {

            final Path TEST_PROPS_PATH = Paths.get("test/test.properties");
            final Path DEFAULT_PROPS_PATH = Paths.get("lib/metacat.properties");

            String testValue = "testingWriteable-" + random.nextInt();
            assertTrue(PropertiesWrapper.WRITEABLE_DEFAULTS.size() > 1);
            String writeableKey = PropertiesWrapper.WRITEABLE_DEFAULTS.get(1);
            assertNotEquals(writeableKey, PropertyService.SITE_PROPERTIES_DIR_PATH_KEY);
            LeanTestUtils.debug("setProperty_writeableDefault() using key: "
                                    + writeableKey);

            PropertyService.setProperty(writeableKey, testValue);
            assertEquals(testValue, PropertyService.getProperty(writeableKey));
            //should have been written to disk
            assertEquals(testValue,
                            LeanTestUtils.getLatestPropertiesFromDisk().getProperty(writeableKey));

            // but check it's been written to the default properties file (metacat.properties)...
            Properties defaultProps = getPropertiesFromPath(DEFAULT_PROPS_PATH);
            // verify properties loaded ok:
            assertFalse("metacat.properties not loaded, from: " + DEFAULT_PROPS_PATH,
                        defaultProps.getProperty("application.metacatVersion").trim().isEmpty());

            assertEquals(testValue, defaultProps.getProperty(writeableKey));

            // and check it's NOT to site properties! (Note that for
            // LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST) mode,
            // the "site properties" file is, in fact, test/test.properties)
            Properties siteProps = getPropertiesFromPath(TEST_PROPS_PATH);
            // verify properties loaded ok:
            assertFalse("test.properties is not loaded, from: " + TEST_PROPS_PATH,
                siteProps.getProperty("metacat.contextDir").trim().isEmpty());

            assertNotEquals(testValue,siteProps.getProperty(writeableKey));
        }

        private Properties getPropertiesFromPath(Path propsPath) {
            Properties props = new Properties();
            try {
                props.load(Files.newBufferedReader(propsPath));
            } catch (IOException e) {
                fail("I/O exception trying to load properties from " + propsPath);
            }
            assertFalse("LeanTestUtils: latestProperties EMPTY!", props.isEmpty());

            return props;
        }
    }

    /**
     * Tests that exercise the ability to override secret credential properties by setting the
     * values as environment variables
     */
    public static class EnvironmentVariablesTestSuite {
        private final Map<String, String> testEnvSecrets = new HashMap<>();
        private final Properties expectedSecrets = new Properties();
        private PropertiesWrapper propertiesWrapper;

        @Before
        public void setUpEnv() throws Exception {

            // These should be a subset of 'application.envSecretKeys' in metacat.properties
            final String[] secretEnvVarKeysList = {
                "POSTGRES_USER",
                "POSTGRES_PASSWORD",
                "METACAT_GUID_DOI_PASSWORD",
                "METACAT_REPLICATION_PRIVATE_KEY_PASSWORD",
                "METACAT_DATAONE_CERT_FROM_HTTP_HEADER_PROXY_KEY"
            };
            // These should be a subset of 'application.envSecretKeys' in metacat.properties
            final String[] secretPropsKeysList = {
                "database.user",
                "database.password",
                "guid.doi.password",
                "replication.privatekey.password",
                "dataone.certificate.fromHttpHeader.proxyKey"
            };
            assertEquals(secretEnvVarKeysList.length, secretPropsKeysList.length);

            for (int i = 0; i < secretEnvVarKeysList.length; i++) {
                String envKey=secretEnvVarKeysList[i];
                String propKey=secretPropsKeysList[i];
                String value = "TestValueFor_" + propKey;
                testEnvSecrets.put(envKey, value);
                expectedSecrets.setProperty(propKey, value);
            }
            // Add a key for a bogus secret to env, with no value. No default values in props file
            testEnvSecrets.put("METACAT_NOT_SET", " ");

            // Add a key for a real secret to env, with no value. Should fall back to existing property
            // guid.doi.username=METACAT_GUID_DOI_USERNAME; fallback is to guid.doi.username from props
            // These should be included in 'application.envSecretKeys' in metacat.properties
            testEnvSecrets.put("METACAT_GUID_DOI_USERNAME", "");
            expectedSecrets.setProperty("guid.doi.username",
                LeanTestUtils.getExpectedProperties().getProperty("guid.doi.username"));

            LeanTestUtils.setTestEnvironmentVariables(testEnvSecrets);

            assertTrue(
                LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST));

            propertiesWrapper = PropertiesWrapper.getInstance();
        }

        @Test
        public void checkEnvSecretsSet() {
            for (Map.Entry<String, String> entry : testEnvSecrets.entrySet()) {
                String envKey = entry.getKey();
                assertEquals(testEnvSecrets.get(envKey), System.getenv(envKey));
            }
        }

        @Test
        public void initializeSecretsFromEnvVars() {
            // props with expectedSecrets values
            expectedSecrets.forEach((key, value) -> {
                try {
                    assertEquals("key: "+key+"; expected value: " + value, value,
                                 propertiesWrapper.getProperty((String)key));
                } catch (PropertyNotFoundException e) {
                    fail("unexpected exception " + e.getMessage());
                }
            });
        }

        @Test
        public void initializeSecretsFromEnvVars_fallback() {
            // expected env secrets but which haven't been set - should default to values in props file
            try {
                assertEquals(LeanTestUtils.getExpectedProperties().getProperty("guid.doi.username"),
                    propertiesWrapper.getProperty("guid.doi.username"));
            } catch (PropertyNotFoundException e) {
                fail("unexpected exception " + e.getMessage());
            }
        }

        @Test
        public void initializeSecretsFromEnvVars_noFallback() {
            // expected env secrets but which haven't been set - no default values in props file
            // metacat.notSet=METACAT_NOT_SET
            try {
                assertEquals("", propertiesWrapper.getProperty("metacat.notSet"));
                fail("Expected a PropertyNotFoundException to be thrown, but it wasn't");
            } catch (PropertyNotFoundException e) {
                assertNotNull(e);
            }
        }
    }
}
