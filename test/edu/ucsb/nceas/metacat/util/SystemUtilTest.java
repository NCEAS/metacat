package edu.ucsb.nceas.metacat.util;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;

import junit.framework.Test;
import junit.framework.TestSuite;
/**
 * A JUnit test class for the SystemUil class.
 * @author tao
 *
 */
public class SystemUtilTest extends MCTestCase {
   
    /**
     * Constructor
     * @param name
     */
    public SystemUtilTest(String name) {
        super(name);
    }
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite();
        suite.addTest(new SystemUtilTest("initialize"));
        suite.addTest(new SystemUtilTest("testGetInternalURLs"));
         return suite;
    }
    
    /**
     * init
     */
    public void initialize() {
        assertTrue(1==1);
    }
    
    
    /**
     * Test the methods to get internal urls
     * @throws Exception
     */
    public void testGetInternalURLs() throws Exception {
        String expectedUrl = LeanTestUtils.getExpectedProperties()
            .getProperty("expected.internalURL", "http" + "://localhost:80");
        String internalURL = SystemUtil.getInternalServerURL();
        System.out.println("============the internal url is " + internalURL);
        System.out.println("=============the internal url has been replaced by the external url is "
            + SystemUtil.isInternalURLReplacedByExternal());
        if (!SystemUtil.isInternalURLReplacedByExternal()) {
            assertEquals(expectedUrl, internalURL);
        }
        internalURL = SystemUtil.getInternalServerURL();
        if (!SystemUtil.isInternalURLReplacedByExternal()) {
            assertEquals(expectedUrl, internalURL);
        }
        String contextURL = SystemUtil.getInternalContextURL();
        System.out.println("========================= the context url is " + contextURL);
        if (!SystemUtil.isInternalURLReplacedByExternal()) {
            assertEquals(expectedUrl + "/" + PropertyService.getProperty("application.context"),
                contextURL);
        }
    }
    
  
   
}
