package edu.ucsb.nceas.metacattest.service;

import edu.ucsb.nceas.metacat.service.XMLSchemaParser;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * Junit test for XMLSchemaService
 * @author tao
 *
 */
public class XMLSchemaServiceTest extends TestCase
{
  
  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   */
  public XMLSchemaServiceTest(String name)
  {
    super(name);
  }

  /**
   * Establish a testing framework by initializing appropriate objects
   */
  public void setUp()
  {
    
  }

  /**
   * Release any objects after tests are complete
   */
  public void tearDown()
  {
  }

  /**
   * Create a suite of tests to be run together
   */
  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTest(new XMLSchemaServiceTest("initialize"));
    suite.addTest(new XMLSchemaServiceTest("testGetBaseUrlFromSchemaURL"));
    return suite;
  }

  /**
   * Run an initial test that always passes to check that the test
   * harness is working.
   */
  public void initialize()
  {
    assertTrue(1 == 1);
  }
  
  public void testGetBaseUrlFromSchemaURL()
  {
      String url="http://www.example.com/metacat/example.xsd";
      String base = "http://www.example.com/metacat/";
      String baseURL = XMLSchemaService.getBaseUrlFromSchemaURL(url);
      assertTrue("The base url should be "+base, baseURL.equals(base));
      url = "www.example.com/example.xsd";
      baseURL = XMLSchemaService.getBaseUrlFromSchemaURL(url);
      assertTrue("The base url should be "+null, baseURL==null);
      url="http://www.example.com/metacat/";
      baseURL = XMLSchemaService.getBaseUrlFromSchemaURL(url);
      assertTrue("The base url should be "+url, baseURL.equals(url));
  }
}
