package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.metacat.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * A JUnit test for testing Step class processing
 */
public class MetaCatURLTest extends TestCase
{
  private MetacatURL withProtocol =null;
  private String withHttp="http://dev.nceas.ucsb.edu/tao/test.txt";
  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   */
  public MetaCatURLTest(String name)
  {
    super(name);
  }

  /**
   * Establish a testing framework by initializing appropriate objects
   */
  public void setUp()
  {
    try 
    {
     
      withProtocol=new MetacatURL(withHttp);
        
    } 
    catch (Exception e) 
    {
      fail("Caught exception while setting up MetaCatURL test.");
    }
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
    suite.addTest(new MetaCatURLTest("initialize"));
    suite.addTest(new MetaCatURLTest("testGetProtocol"));
    suite.addTest(new MetaCatURLTest("testToString"));
    suite.addTest(new MetaCatURLTest("testGetParameter"));
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

  /**
   * Test if the getProtocol() function works given a url
   */
  public void testGetProtocol()
  {
    assertTrue( withProtocol.getProtocol().equals("http"));
  }
  
  /**
   * Test if the toString() function works given a url
   */
  public void testToString()
  {
    assertTrue((withProtocol.toString()).equals(withHttp));
  }
  
  /**
   * Test if the getParam() function works given a url
   */
  public void testGetParameter()
  {
    String [] str= new String[2];
    str=withProtocol.getParam(0);
    assertTrue(str[0].equals("httpurl"));
    assertTrue(str[1].equals("http://dev.nceas.ucsb.edu/tao/test.txt"));
    str=withProtocol.getParam(1);
    assertTrue(str[0].equals("filename"));
    assertTrue(str[1].equals("test.txt"));
  }
  

}
