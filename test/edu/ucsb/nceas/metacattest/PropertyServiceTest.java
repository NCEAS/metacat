/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author: daigle $'
 *     '$Date: 2008-07-18 10:06:00 -0700 (Fri, 18 Jul 2008) $'
 * '$Revision: 4146 $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.ucsb.nceas.metacattest;

import java.util.Map;
import java.util.Random;
import java.util.Vector;

import edu.ucsb.nceas.MCTestCase;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A JUnit test for testing Access Control in Metacat
 */
public class PropertyServiceTest extends MCTestCase {

    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public PropertyServiceTest(String name) {
        super(name);
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() {

    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new PropertyServiceTest("initialize"));
        // Test basic functions
        suite.addTest(new PropertyServiceTest("mainPropertiesTest"));

        return suite;
    }

    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize() {
        assertTrue(1 == 1);
    }
    
    
    /**
     * Tests reading and writing from properties using the PropertyService class
     *1. read a single property from the properties file that should exist
     *   Read a test property from the PropertyService.  The value should be read 
     *   successfully.  If PropertyNotFoundException is thrown, the test case fails.
     *   Also, if the value returned is null, the test case fails.  Strictly speaking,
     *   it is not a failure for getProperty to return null, but we know that 
     *   'test.metacatURL' should have a value.
     *2. read a single property from the main properties that shouldn't exist
     *   Expect a PropertyNotFoundException here.  If the exception is not thrown,
     *   the test case fails.
     *3. read group property names from the main properties
     *   read an existing group of property names.  If the result set is zero
     *   length, the test case fails.
     *4. read group properties from the main properties
     *   Get the property values for the group names retrieved in test case 3.  If
     *   the properties map retrieved is empty, the test case fails.
     *5. write property to main properties
     *   Write a property that already exists to the metacat.properties file.  The 
     *   value is randomly generated.  The value is then retrieved from the 
     *   metacat.properties file.  If the retrieved value does not match the 
     *   generated value, or if an exception is thrown, the test case fails.
     *6. set property and persist to main properties
     *   This does the same as test case 5, but instead of using setProperty which
     *   sets the property in memory and persists it to file, this case uses 
     *   setPropertyNoPersist and persistProperties to do the set and persist
     *   atomically. 
     *7. try to write property to main properties that doesn't exist
     *   Attempt to set a property that doesn't exist in the properties
     *   file.  This should throw a PropertyNotFoundException.  Anything else
     *   causes the test case to fail.
     *8. try to write property nonPersistant to main properties that doesn't exist
     *   This does the same as test case 7, but uses the setPropertyNoPersist method.
     */
    public void mainPropertiesTest()
    {
    	Random random = new Random();
    	try {
    		debug("\nRunning: mainPropertiesTest test");
      
	        //====1 read a single property from the properties file that should exist
        	debug("Test 1: read a single property from the properties file that should exist");
	        try {
	        	String metacatURL = PropertyService.getProperty("test.metacatUrl");
	        	if (metacatURL == null || metacatURL.equals("")) {
	        		fail("Test 1: Reading property 'test.metacatURL' returned no value.");
	        	}
	        	debug("Test 1: read property 'test.metacatURL': " + metacatURL);
	        } catch (PropertyNotFoundException pnfe) {
	        	fail("Test 1: Could not read property 'test.metacatURL' : " + pnfe.getMessage());
	        }
	        
	        //====2 read a single property from the main properties  that shouldn't exist
	        debug("Test 2: read a single property from the main properties  that shouldn't exist");
	        try {
	        	String metacatURL = PropertyService.getProperty("test.this.doesn't.exist");
	        	fail("Test 2: shouldn't have successfully read property: test.this.doesn't.exist : " 
	        			+ metacatURL);
	        } catch (PropertyNotFoundException pnfe) {
	        	debug("Test 2: expected failure reading property:'test.this.doesn't.exist' : " 
	        			+ pnfe.getMessage());
	        }
	        
	        //====3 read group property names from the main properties  
	        debug("Test 3: read property group names 'organization.org'");
			Vector<String> orgList = 
				PropertyService.getPropertyNamesByGroup("organization.org");
			if (orgList == null || orgList.size() == 0) {
				fail("Test 3: Empty vector returned when reading property group names 'organization.org'");
			}
   
	        // ====4 read group properties from the main properties
	        try {
	        	debug("Test 4: read property group 'organization.org'");
	        	Map<String, String> metacatProps = PropertyService.getPropertiesByGroup("organization.org");
	        	if (metacatProps == null || metacatProps.size() == 0) {
	        		fail("Test 4: Empty map returned when reading property group names 'organization.org'");
	        	}
	        } catch (PropertyNotFoundException pnfe) {
	        	fail("Test 4: Could not read property group names 'organization.org': " + pnfe.getMessage());
	        }  
	        
	        // ====5 write property to main properties
	        try {
	        	String testValue = "testing" + random.nextInt();
	        	debug("Test 5: set property 'test.testProperty' : " + testValue);
	        	PropertyService.setProperty("test.testProperty", testValue);
	        	String testValue2 = PropertyService.getProperty("test.testProperty");
	        	if (!testValue.equals(testValue2)) {
	        		fail("Test 5: couldn't set 'test.testProperty' to " + testValue);
	        	}
	        } catch (PropertyNotFoundException pnfe) {
	        	fail("Test 5: Could not set property 'test.testProperty' : " + pnfe.getMessage());
	        }  
	        	        
	        // ====6 set property and persist to main properties
	        try {
	        	String testValue = "testing" + random.nextInt();
	        	debug("Test 6: set property 'test.testProperty' : " + testValue);
	        	PropertyService.setPropertyNoPersist("test.testProperty", testValue);
	        	PropertyService.persistProperties();
	        	String testValue2 = PropertyService.getProperty("test.testProperty");
	        	if (!testValue.equals(testValue2)) {
	        		fail("Test 6: couldn't set 'test.testProperty' to " + testValue);
	        	}
	        } catch (PropertyNotFoundException pnfe) {
	        	fail("Test 6: Could not set property 'test.testProperty' : " + pnfe.getMessage());
	        }  
	        
	        // ====7 try to write property to main properties that doesn't exist
	        String testValue;
	        try {
	        	testValue = "testing" + random.nextInt();
	        	debug("Test 7: set property 'test.property.nonexistant' : " + testValue);
	        	PropertyService.setProperty("test.property.nonexistant", testValue);
		        fail("Test 7: shouldn't have been able to set 'test.property.nonexistant' to "
		        		+ testValue + " since 'test.property.nonexistant' doesn't exist.");
	        } catch (PropertyNotFoundException pnfe) {
	        	debug("Test 7: expected failure writing to property:'test.property.nonexistant' : " 
                        + pnfe.getMessage());
	        }  
	        
	        // ====8 try to write property nonPersistant to main properties that doesn't exist
	        try {
	        	testValue = "testing" + random.nextInt();
	        	debug("Test 8: set property 'test.property.nonexistant' : " + testValue);
	        	PropertyService.setPropertyNoPersist("test.property.nonexistant", testValue);
		        fail("Test 8: shouldn't have been able to set 'test.property.nonexistant' to "
		        		+ testValue + " since 'test.property.nonexistant' doesn't exist.");
	        } catch (PropertyNotFoundException pnfe) {
	        	debug("Test 8: expected failure writing to property:'test.property.nonexistant' : " 
                        + pnfe.getMessage());
	        }  
	        
	        
	    } catch (Exception e) {
	        fail("General exception:\n" + e.getMessage());
	    }
    }
}
