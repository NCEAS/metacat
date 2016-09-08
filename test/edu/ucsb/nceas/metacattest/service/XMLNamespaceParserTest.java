package edu.ucsb.nceas.metacattest.service;

import org.apache.commons.io.FileUtils;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.service.XMLNamespaceParser;


import java.io.File;
import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * Junit test for XMLSchemaService
 * @author tao
 *
 */
public class XMLNamespaceParserTest extends MCTestCase {
    public static String withPrefix = "<eml:eml packageId=\"eml.2.1\" system=\"knb2\" xmlns:eml=\"http://eml-example\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:stmml=\"http://www.xml-cml.org/schema/stmml\" xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.1 eml.xsd\"><tile>title</title></eml>";
    public static String withCommentAndPrefix="<!-- comment -->  <!-- comment -->"+withPrefix;              
    public static String defaultNamespace = "<root xmlns=\"eml://ecoinformatics.org/eml-2.1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:stmml=\"http://www.xml-cml.org/schema/stmml\" xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.1 eml.xsd\"><title>title1</title></root>";
    public static String noNamespace = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:stmml=\"http://www.xml-cml.org/schema/stmml\" xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.1 eml.xsd\"><title>title1</title></root>";
    
  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   */
  public XMLNamespaceParserTest(String name) {
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
    suite.addTest(new XMLSchemaServiceTest("initialize"));
    suite.addTest(new XMLNamespaceParserTest("testGetNamespace"));
    suite.addTest(new XMLNamespaceParserTest("testGetNoNamespaceSchemaLocation"));
    return suite;
  }

  /**
   * Run an initial test that always passes to check that the test
   * harness is working.
   */
  public void initialize() {
    assertTrue(1 == 1);
  }
  
 
  
  public void testGetNamespace() throws Exception{
      StringReader reader = new StringReader(withPrefix);
      XMLNamespaceParser parser = new XMLNamespaceParser(reader);
      parser.parse();
      String namespace = parser.getNamespace();
      assertTrue(namespace.equals("http://eml-example"));
      
      reader = new StringReader(withCommentAndPrefix);
      parser = new XMLNamespaceParser(reader);
      parser.parse();
      namespace = parser.getNamespace();
      assertTrue(namespace.equals("http://eml-example"));
      
      File file = new File("./test/eml-sample.xml");
      reader = new StringReader(FileUtils.readFileToString(file));
      parser = new XMLNamespaceParser(reader);
      parser.parse();
      namespace = parser.getNamespace();
      assertTrue(namespace.equals("eml://ecoinformatics.org/eml-2.0.1"));
      
      reader = new StringReader(defaultNamespace);
      parser = new XMLNamespaceParser(reader);
      parser.parse();
      namespace = parser.getNamespace();
      assertTrue(namespace.equals("eml://ecoinformatics.org/eml-2.1.1"));
      
      reader = new StringReader(noNamespace);
      parser = new XMLNamespaceParser(reader);
      parser.parse();
      namespace = parser.getNamespace();
      assertTrue(namespace == null);
  }
  
  
  public void testGetNoNamespaceSchemaLocation() throws Exception{
      StringReader reader = new StringReader(withCommentAndPrefix);
      XMLNamespaceParser parser = new XMLNamespaceParser(reader);
      parser.parse();
      String namespace = parser.getNamespace();
      String noNamespaceSchemaLocation = parser.getNoNamespaceSchemaLocation();
      assertTrue(namespace.equals("http://eml-example"));
      assertTrue(noNamespaceSchemaLocation == null);
      
      File file = new File("./test/eml-sample.xml");
      reader = new StringReader(FileUtils.readFileToString(file));
      parser = new XMLNamespaceParser(reader);
      parser.parse();
      namespace = parser.getNamespace();
      noNamespaceSchemaLocation = parser.getNoNamespaceSchemaLocation();
      assertTrue(namespace.equals("eml://ecoinformatics.org/eml-2.0.1"));
      assertTrue(noNamespaceSchemaLocation == null);
      
      file = new File("./test/fgdc.xml");
      reader = new StringReader(FileUtils.readFileToString(file));
      parser = new XMLNamespaceParser(reader);
      parser.parse();
      namespace = parser.getNamespace();
      noNamespaceSchemaLocation = parser.getNoNamespaceSchemaLocation();
      assertTrue(namespace == null);
      assertTrue(noNamespaceSchemaLocation.equals("http://www.fgdc.gov/metadata/fgdc-std-001-1998.xsd"));
      
      reader = new StringReader(defaultNamespace);
      parser = new XMLNamespaceParser(reader);
      parser.parse();
      namespace = parser.getNamespace();
      noNamespaceSchemaLocation = parser.getNoNamespaceSchemaLocation();
      assertTrue(namespace.equals("eml://ecoinformatics.org/eml-2.1.1"));
      assertTrue(noNamespaceSchemaLocation == null);
      
      reader = new StringReader(noNamespace);
      parser = new XMLNamespaceParser(reader);
      parser.parse();
      namespace = parser.getNamespace();
      noNamespaceSchemaLocation = parser.getNoNamespaceSchemaLocation();
      assertTrue(namespace == null);
      assertTrue(noNamespaceSchemaLocation == null);
  }
}
