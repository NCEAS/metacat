package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;
import java.util.Random;
import java.util.Vector;

import static org.junit.Assert.fail;

/**
 * A JUnit test for testing Access Control in Metacat
 */
@RunWith(JUnit4.class)
public class PropertyServiceTest { // don't extend MCTestCase for JUnit 4

    private static final TestUtils testUtils = new TestUtils();
    private final Random random = new Random();

    public PropertyServiceTest() {
        super();
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
        testUtils.debugMsg("Test 1: read a single property from the properties file that should exist");
        try {
            String user = PropertyService.getProperty("test.mcUser");
            if (user == null || user.equals("")) {
                fail("Test 1: Reading property 'test.mcUser' returned no value.");
            }
            testUtils.debugMsg("Test 1: read property 'test.mcUser': " + user);
        } catch (PropertyNotFoundException pnfe) {
            fail("Test 1: Could not read property 'test.mcUser' : " + pnfe.getMessage());
        }
    }


    @Test
    public void readNonExistentProperty() {
        //====2 read a single property from the main properties  that shouldn't exist
        testUtils.debugMsg("Test 2: read a single property from the main properties  that shouldn't exist");
        try {
            String metacatURL = PropertyService.getProperty("test.this.doesn't.exist");
            fail("Test 2: shouldn't have successfully read property: test.this.doesn't.exist : "
                + metacatURL);
        } catch (PropertyNotFoundException pnfe) {
            testUtils.debugMsg("Test 2: expected failure reading property:'test.this.doesn't.exist' : "
                + pnfe.getMessage());
        }
    }

    @Test
    public void getPropertyNamesByGroup() {
        //====3 read group property names from the main properties
        testUtils.debugMsg("Test 3: read property group names 'organization.org'");
        Vector<String> orgList = PropertyService.getPropertyNamesByGroup("organization.org");
        if (orgList == null || orgList.size() == 0) {
            fail("Test 3: Empty vector returned when reading property group names "
                + "'organization.org'");
        }
    }

    @Test
    public void getPropertiesByGroup() {
        // ====4 read group properties from the main properties
        try {
            testUtils.debugMsg("Test 4: read property group 'organization.org'");
            Map<String, String> metacatProps =
                PropertyService.getPropertiesByGroup("organization.org");
            if (metacatProps == null || metacatProps.size() == 0) {
                fail("Test 4: Empty map returned when reading property group names "
                    + "'organization.org'");
            }
        } catch (PropertyNotFoundException pnfe) {
            fail("Test 4: Could not read property group names 'organization.org': "
                + pnfe.getMessage());
        }
    }

    @Test
    public void setProperty() {
        // ====5 write property to main properties
        try {
            String testValue = "testing" + random.nextInt();
            testUtils.debugMsg("Test 5: set property 'test.testProperty' : " + testValue);
            PropertyService.setProperty("test.testProperty", testValue);
            String testValue2 = PropertyService.getProperty("test.testProperty");
            if (!testValue.equals(testValue2)) {
                fail("Test 5: couldn't set 'test.testProperty' to " + testValue);
            }
        } catch (GeneralPropertyException pnfe) {
            fail("Test 5: Could not set property 'test.testProperty' : " + pnfe.getMessage());
        }
    }

    @Test
    public void persistProperties() {
        // ====6 set property and persist to main properties
        try {
            String testValue = "testing" + random.nextInt();
            testUtils.debugMsg("Test 6: set property 'test.testProperty' : " + testValue);
            PropertyService.setPropertyNoPersist("test.testProperty", testValue);
            PropertyService.persistProperties();
            String testValue2 = PropertyService.getProperty("test.testProperty");
            if (!testValue.equals(testValue2)) {
                fail("Test 6: couldn't set 'test.testProperty' to " + testValue);
            }
        } catch (GeneralPropertyException pnfe) {
            fail("Test 6: Could not set property 'test.testProperty' : " + pnfe.getMessage());
        }
    }

    @Test
    public void setPropertyNonExistent() {
        // ====7 try to write property to main properties that doesn't exist
        String testValue;
        try {
            testValue = "testing" + random.nextInt();
            testUtils.debugMsg("Test 7: set property 'test.property.nonexistent' : " + testValue);
            PropertyService.setProperty("test.property.nonexistent", testValue);
            fail("Test 7: shouldn't have been able to set 'test.property.nonexistent' to "
                + testValue + " since 'test.property.nonexistent' doesn't exist.");
        } catch (GeneralPropertyException pnfe) {
            testUtils.debugMsg("Test 7: expected failure writing to property:'test.property.nonexistent' : "
                + pnfe.getMessage());
        }
    }

    @Test
    public void setPropertyNoPersist() {
        // ====8 try to write property nonPersistent to main properties that doesn't exist
        String testValue;
        try {
            testValue = "testing" + random.nextInt();
            testUtils.debugMsg("Test 8: set property 'test.property.nonexistent' : " + testValue);
            PropertyService.setPropertyNoPersist("test.property.nonexistent", testValue);
            fail("Test 8: shouldn't have been able to set 'test.property.nonexistent' to "
                + testValue + " since 'test.property.nonexistent' doesn't exist.");
        } catch (GeneralPropertyException pnfe) {
            testUtils.debugMsg("Test 8: expected failure writing to property:'test.property.nonexistent' : "
                + pnfe.getMessage());
        }
    }

    public static class TestUtils extends MCTestCase {

        void debugMsg(String msg) {
            MCTestCase.debug(msg);
        }
    }
}
