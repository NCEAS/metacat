package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * A JUnit test for testing Access Control in Metacat
 */
@RunWith(JUnit4.class)
public class PropertyServiceTest { // don't extend MCTestCase for JUnit 4

    private final Random random = new Random();

    public PropertyServiceTest() {
        super();
        // need to instantiate PropertyService at least once
        try {
            assertNotNull(PropertyService.getTestInstance(Paths.get("lib/metacat.properties"),
                Paths.get("test/test" + ".properties")));
        } catch (ServiceException e) {
            fail("PropertyServiceTest constructor failed to instantiate PropertyService: " + e);
        }
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
    }


    @Test
    public void readOneProperty() {
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


    @Test
    public void readNonExistentProperty() {
        //====2 read a single property from the main properties  that shouldn't exist
        try {
            String nonExistentUser = PropertyService.getProperty("test.this.doesn't.exist");
            fail("Shouldn't have successfully read property: test.this.doesn't.exist : "
                + nonExistentUser);
        } catch (PropertyNotFoundException pnfe) {
            LeanTestUtils.debug("EXPECTED failure reading property:'test.this.doesn't.exist' : "
                + pnfe.getMessage());
        }
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

    @Test
    public void setPropertyNonExistent() {
        // ====7 try to write property to main properties that doesn't exist
        String testValue;
        try {
            testValue = "testing" + random.nextInt();
            PropertyService.setProperty("test.property.nonexistent", testValue);
            fail("Shouldn't have been able to set 'test.property.nonexistent' to " + testValue
                + " since 'test.property.nonexistent' doesn't exist.");
        } catch (GeneralPropertyException pnfe) {
            LeanTestUtils.debug("EXPECTED failure writing to property:'test.property.nonexistent' "
                + pnfe.getMessage());
        }
    }

    @Test
    public void setPropertyNoPersist() {
        // ====8 try to write property nonPersistent to main properties that doesn't exist
        String testValue;
        try {
            testValue = "testing" + random.nextInt();
            PropertyService.setPropertyNoPersist("test.property.nonexistent", testValue);
            fail("Shouldn't have been able to set 'test.property.nonexistent' to " + testValue
                + " since 'test.property.nonexistent' doesn't exist.");
        } catch (GeneralPropertyException pnfe) {
            LeanTestUtils.debug(
                "EXPECTED failure writing to property:'test.property.nonexistent' : "
                    + pnfe.getMessage());
        }
    }
}
