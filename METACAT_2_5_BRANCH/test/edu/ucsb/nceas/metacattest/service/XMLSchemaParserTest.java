package edu.ucsb.nceas.metacattest.service;

import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.XMLSchemaParser;
import edu.ucsb.nceas.MCTestCase;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * Junit test for xml schema parser
 * @author tao
 *
 */
public class XMLSchemaParserTest extends MCTestCase
{
  private static String schemaLocation = "./test/servertestfiles/product.xsd";
  private static String INCLUDEDPATH = "./item.xsd";
  private static final String CONFIG_DIR_TEST = "./lib";
  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   */
  public XMLSchemaParserTest(String name)
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
    suite.addTest(new XMLSchemaParserTest("initialize"));
    suite.addTest(new XMLSchemaParserTest("testGetIncludedSchemaFilePaths"));
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
  
  public void testGetIncludedSchemaFilePaths()
  {
     
     try
     {
       //PropertyService.getInstance(CONFIG_DIR_TEST);
       FileInputStream schemaInputStream =
                                     new FileInputStream(new File(schemaLocation));
       
       XMLSchemaParser  parser =  new XMLSchemaParser(schemaInputStream);
       parser.parse();
       Vector<String>includedPaths = parser.getIncludedSchemaFilePathes();
       assertTrue("The length of the paths should be one.", includedPaths.size()==1);
       assertTrue("The path should be "+INCLUDEDPATH, 
                                includedPaths.elementAt(0).equals(INCLUDEDPATH));
     }
     catch(Exception e)
     {
       e.printStackTrace();
       fail("Error: "+e.getMessage());
     }
     
  }
}
