package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

/**
 * A JUnit test for testing Access Control in Metacat
 */
@RunWith(JUnit4.class)
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
        //====1 read a single property from the properties file that should exist
        try {
            String userKey = "test.mcUser";
            assertEquals("Reading property 'test.mcUser' returned wrong value.",
                LeanTestUtils.getExpectedProperties().getProperty(userKey),
                PropertyService.getProperty(userKey));
        } catch (PropertyNotFoundException pnfe) {
            fail("Could not read property 'test.mcUser' : " + pnfe.getMessage());
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
        Set<String> expectedSet = LeanTestUtils.getExpectedProperties().stringPropertyNames();
        expectedSet.removeIf(prop -> !prop.startsWith(groupKey));
        String[] expected = expectedSet.toArray(new String[0]);
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
        Set<String> expectedSet = LeanTestUtils.getExpectedProperties().stringPropertyNames();
        expectedSet.removeIf(prop -> !prop.startsWith(groupKey));

        assertEquals("unexpected number of properties found)", expectedSet.size(),
            metacatProps.size());
    }

    @Test
    public void setProperty() {
        // ====5 write property to main properties
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
        // ====7 try to write property to main properties that doesn't exist
        String testValue = "testing" + random.nextInt();
        PropertyService.setProperty("test.property.nonexistent", testValue);
        fail("Shouldn't have been able to set 'test.property.nonexistent' to " + testValue
            + " since 'test.property.nonexistent' doesn't exist.");

    }

    @Test(expected = GeneralPropertyException.class)
    public void setPropertyNoPersist() throws GeneralPropertyException {
        // ====8 try to write property nonPersistent to main properties that doesn't exist
        String testValue = "testing" + random.nextInt();
        PropertyService.setPropertyNoPersist("test.property.nonexistent", testValue);
        fail("Shouldn't have been able to set 'test.property.nonexistent' to " + testValue
            + " since 'test.property.nonexistent' doesn't exist.");
    }

    @Test
    public void arePropertiesConfigured() throws Exception {
        String key = "configutil.propertiesConfigured";
        String errMsg = "PropertyService.arePropertiesConfigured() returned wrong value.";

        try (MockedStatic<PropertyService> mock = Mockito.mockStatic(PropertyService.class)) {
            mock.when(() -> PropertyService.getProperty(eq(key))).thenReturn("true");
            mock.when(() -> PropertyService.getProperty(
                    argThat((String s) -> !s.equals(key)))).thenCallRealMethod();
            mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod();

            assertTrue(errMsg, PropertyService.arePropertiesConfigured());

        } catch (GeneralPropertyException pnfe) {
            fail("Problem calling PropertyService: " + pnfe.getMessage());
        }

        try (MockedStatic<PropertyService> mock = Mockito.mockStatic(PropertyService.class)) {
            mock.when(() -> PropertyService.getProperty(eq(key))).thenReturn("false");
            mock.when(() -> PropertyService.getProperty(
                    argThat((String s) -> !s.equals(key)))).thenCallRealMethod();
            mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod();

            assertFalse(errMsg, PropertyService.arePropertiesConfigured());

        } catch (GeneralPropertyException pnfe) {
            fail("Problem calling PropertyService: " + pnfe.getMessage());
        }

        try (MockedStatic<PropertyService> mock = Mockito.mockStatic(PropertyService.class)) {
            mock.when(() -> PropertyService.getProperty(eq(key))).thenReturn("");
            mock.when(() -> PropertyService.getProperty(
                    argThat((String s) -> !s.equals(key)))).thenCallRealMethod();
            mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod();

            assertFalse(errMsg, PropertyService.arePropertiesConfigured());

        } catch (GeneralPropertyException pnfe) {
            fail("Problem calling PropertyService: " + pnfe.getMessage());
        }

        try (MockedStatic<PropertyService> mock = Mockito.mockStatic(PropertyService.class)) {
            mock.when(() -> PropertyService.getProperty(eq(key))).thenReturn(null);
            mock.when(() -> PropertyService.getProperty(
                    argThat((String s) -> !s.equals(key)))).thenCallRealMethod();
            mock.when(PropertyService::arePropertiesConfigured).thenCallRealMethod();

            assertFalse(errMsg, PropertyService.arePropertiesConfigured());

        } catch (GeneralPropertyException pnfe) {
            fail("Problem calling PropertyService: " + pnfe.getMessage());
        }
    }
}
