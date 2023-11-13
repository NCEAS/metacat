package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

/**
 * A JUnit test for testing Access Control in Metacat
 */
public class PropertyServiceTest { // don't extend MCTestCase for JUnit 4

    private final Random random = new Random();

    public PropertyServiceTest() {
        // need to instantiate PropertyService at least once
        assertTrue("error trying to set up PropertyService for unit testing",
            LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST));
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    @Before
    public void setUp() {
    }

    /**
     * Release any objects after tests are complete
     */
    @After
    public void tearDown() {
        try {
            PropertyService.setProperty("test.testProperty", "");
        } catch (GeneralPropertyException e) {
            LeanTestUtils.debug("tearDown(): Unable to clean up test properties");
        }
    }


    @Test
    public void getProperty() {
        //Read a single property from the properties file that should exist
        try {
            String userKey = "test.mcUser";
            assertEquals("Reading property 'test.mcUser' returned wrong value.",
                LeanTestUtils.getExpectedProperties().getProperty(userKey),
                PropertyService.getProperty(userKey));
        } catch (PropertyNotFoundException pnfe) {
            fail("Could not read property 'test.mcUser' : " + pnfe.getMessage());
        }
    }

    @Test
    public void getProperty_existsEmpty() {
        //read a property whose key exists, but has no associated value
        String testProperty = "test.testProperty";
        String expected = "";
        try {
            PropertyService.setProperty(testProperty, expected);

            assertEquals("Should have read a blank value for property: \"test.testProperty\"",
                         expected, PropertyService.getProperty(testProperty));
        } catch (PropertyNotFoundException e) {
            fail("Unexpected error calling PropertyService.getProperty(\""
                     + testProperty + "\") -- " + e.getMessage());
        } catch (GeneralPropertyException e) {
            fail("Unexpected error calling PropertyService.setProperty(\""
                     + testProperty + "\", \"" + expected + "\") -- " + e.getMessage());
        }
    }

    @Test(expected = PropertyNotFoundException.class)
    public void getProperty_NonExistent() throws PropertyNotFoundException {
        //====2 read a single property from the main properties  that shouldn't exist
        String nonExistentUser = PropertyService.getProperty("test.this.doesn't.exist");
        fail("Shouldn't have successfully read property: test.this.doesn't.exist : "
            + nonExistentUser);
    }

    @Test
    public void getPropertyNamesByGroup() {
        //====3 read group property names from the main properties
        String groupKey = "organization.org";
        Vector<String> orgList = PropertyService.getPropertyNamesByGroup(groupKey);
        if (orgList == null || orgList.size() == 0) {
            fail("Empty vector returned when reading property group names 'organization.org'");
        }
        String[] actual = orgList.toArray(new String[0]);
        Arrays.sort(actual);
        Set<String> immutableExpectedSet =
                                        LeanTestUtils.getExpectedProperties().stringPropertyNames();
        // After we moved from java 1.8 to 17, the immutableExpectedSet object is unmodifiable since
        // we use "--add-opens java.base/java.util=ALL-UNNAMED" to work around the issue that the
        // env variables are not allowed to be modified. We have to copy it to another Set object to
        // make it modifiable.
        Set<String> modifiableExpectedSet = new HashSet<String>(immutableExpectedSet);
        modifiableExpectedSet.removeIf(prop -> !prop.startsWith(groupKey));
        String[] expected = modifiableExpectedSet.toArray(new String[0]);
        Arrays.sort(expected);
        assertArrayEquals("unexpected values returned from getPropertyNamesByGroup().", expected,
            actual);
    }

    @Test
    public void getPropertiesByGroup() {
        // ====4 read group properties from the main properties
        String groupKey = "organization.org";
        Map<String, String> metacatProps = null;
        try {
            metacatProps = PropertyService.getPropertiesByGroup(groupKey);

        } catch (PropertyNotFoundException pnfe) {
            fail("Could not read property group names 'organization.org': " + pnfe.getMessage());
        }
        if (metacatProps == null || metacatProps.size() == 0) {
            fail("Empty map returned when reading property group names 'organization.org'");
        }
        Set<String> immutableExpectedSet =
                                        LeanTestUtils.getExpectedProperties().stringPropertyNames();
        // After we moved from java 1.8 to 17, the immutableExpectedSet object is unmodifiable since
        // we use "--add-opens java.base/java.util=ALL-UNNAMED" to work around the issue that the
        // env variables are not allowed to be modified. We have to copy it to another Set object to
        // make it modifiable.
        Set<String> modifiableExpectedSet = new HashSet<String>(immutableExpectedSet);
        modifiableExpectedSet.removeIf(prop -> !prop.startsWith(groupKey));

        assertEquals("unexpected number of properties found)", modifiableExpectedSet.size(),
            metacatProps.size());
    }

    @Test
    public void setProperty() {
        // Write property to main properties
        try {
            String testValue = "testing" + random.nextInt();
            PropertyService.setProperty("test.testProperty", testValue);
            String retrievedTestValue = PropertyService.getProperty("test.testProperty");
            assertEquals("couldn't set 'test.testProperty' to " + testValue, retrievedTestValue,
                testValue);
        } catch (GeneralPropertyException pnfe) {
            fail("Could not set property 'test.testProperty' : " + pnfe.getMessage());
        }
    }

    @Test(expected = GeneralPropertyException.class)
    public void setProperty_nullKey() throws GeneralPropertyException {
        PropertyService.setProperty(null, "testing");
    }

    @Test(expected = GeneralPropertyException.class)
    public void setProperty_nullValue() throws GeneralPropertyException {
        PropertyService.setProperty("test.key", null);
    }

    @Test
    public void persistProperties() {
        // ====6 set property and persist to main properties
        try {
            String testValue = "testing" + random.nextInt();
            PropertyService.setPropertyNoPersist("test.testProperty", testValue);
            PropertyService.persistProperties();
            String retrievedTestValue = PropertyService.getProperty("test.testProperty");
            assertEquals("persistProperties test failed", retrievedTestValue, testValue);
        } catch (GeneralPropertyException pnfe) {
            fail("Could not set property 'test.testProperty' : " + pnfe.getMessage());
        }
    }

    @Test(expected = GeneralPropertyException.class)
    public void setPropertyNonExistent() throws GeneralPropertyException {
        // Try to write to a property that doesn't exist in main properties
        String testValue = "testing" + random.nextInt();
        PropertyService.setProperty("test.property.nonexistent", testValue);
        fail("Shouldn't have been able to set 'test.property.nonexistent' to " + testValue
            + " since 'test.property.nonexistent' doesn't exist.");
    }

    @Test(expected = GeneralPropertyException.class)
    public void setPropertyNoPersist() throws GeneralPropertyException {
        // Try to do  nonPersistent write to a property that doesn't exist in main properties
        String testValue = "testing" + random.nextInt();
        PropertyService.setPropertyNoPersist("test.property.nonexistent", testValue);
        fail("Shouldn't have been able to set 'test.property.nonexistent' to " + testValue
            + " since 'test.property.nonexistent' doesn't exist.");
    }

    @Test
    public void arePropertiesConfigured() {
        String key = "configutil.propertiesConfigured";
        String errMsg = "PropertyService.arePropertiesConfigured() returned wrong value.";

        Properties props = new Properties();

        props.setProperty("bogusKey", "someValue");
        try (MockedStatic<PropertyService> mock =
                     LeanTestUtils.initializeMockPropertyService(props)) {
            mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod();

            assertFalse(errMsg, PropertyService.arePropertiesConfigured());

        } catch (GeneralPropertyException pnfe) {
            fail("Problem calling PropertyService: " + pnfe.getMessage());
        }

        props.setProperty(key, "true");
        try (MockedStatic<PropertyService> mock =
                     LeanTestUtils.initializeMockPropertyService(props)) {
            mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod();

            assertTrue(errMsg, PropertyService.arePropertiesConfigured());

        } catch (GeneralPropertyException pnfe) {
            fail("Problem calling PropertyService: " + pnfe.getMessage());
        }

        props.setProperty(key, "false");
        try (MockedStatic<PropertyService> mock =
                     LeanTestUtils.initializeMockPropertyService(props)) {
            mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod();

            assertFalse(errMsg, PropertyService.arePropertiesConfigured());

        } catch (GeneralPropertyException pnfe) {
            fail("Problem calling PropertyService: " + pnfe.getMessage());
        }

        props.setProperty(key, "");
        try (MockedStatic<PropertyService> mock =
                     LeanTestUtils.initializeMockPropertyService(props)) {
            mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod();

            assertFalse(errMsg, PropertyService.arePropertiesConfigured());

        } catch (GeneralPropertyException pnfe) {
            fail("Problem calling PropertyService: " + pnfe.getMessage());
        }

        props.setProperty(key, "maybe");
        try (MockedStatic<PropertyService> mock =
                     LeanTestUtils.initializeMockPropertyService(props)) {
            mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod();

            assertFalse(errMsg, PropertyService.arePropertiesConfigured());

        } catch (GeneralPropertyException pnfe) {
            fail("Problem calling PropertyService: " + pnfe.getMessage());
        }
    }
}
