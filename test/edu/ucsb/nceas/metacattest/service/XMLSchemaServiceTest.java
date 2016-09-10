package edu.ucsb.nceas.metacattest.service;

import org.apache.commons.io.FileUtils;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.service.XMLSchemaParser;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;

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
public class XMLSchemaServiceTest extends MCTestCase
{
    public static String withPrefix = "<eml:eml packageId=\"eml.2.1\" system=\"knb2\" xmlns:eml=\"eml://ecoinformatics.org/eml-2.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:stmml=\"http://www.xml-cml.org/schema/stmml\" xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.1 eml.xsd\"><tile>title</title></eml>";
    public static String withCommentAndPrefix="<!-- comment -->  <!-- comment -->"+withPrefix;              
    public static String defaultNamespace = "<root xmlns=\"eml://ecoinformatics.org/eml-2.1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:stmml=\"http://www.xml-cml.org/schema/stmml\" xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.1 eml.xsd\"><title>title1</title></root>";
    public static String noNamespace = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:stmml=\"http://www.xml-cml.org/schema/stmml\" xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.1 eml.xsd\"><title>title1</title></root>";
    
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
    suite.addTest(new XMLSchemaServiceTest("testFindNamespaceInDocuments"));
    suite.addTest(new XMLSchemaServiceTest("testFindNamespaceAndSchemaLocalLocation"));
    suite.addTest(new XMLSchemaServiceTest("testFindNoNamespaceSchemaLocalLocation"));
    suite.addTest(new XMLSchemaServiceTest("testIsNamespaceRegistered"));
    suite.addTest(new XMLSchemaServiceTest("testFindNoNamespaceSchemaLocationAttr"));
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
  
  public void testFindNamespaceInDocuments() throws Exception{
      StringReader reader = new StringReader(withPrefix);
      String namespace = XMLSchemaService.findDocumentNamespace(reader);
      assertTrue(namespace.equals("eml://ecoinformatics.org/eml-2.0.0"));
      
      reader = new StringReader(withCommentAndPrefix);
      namespace = XMLSchemaService.findDocumentNamespace(reader);
      assertTrue(namespace.equals("eml://ecoinformatics.org/eml-2.0.0"));
      
      File file = new File("./test/eml-sample.xml");
      reader = new StringReader(FileUtils.readFileToString(file));
      namespace = XMLSchemaService.findDocumentNamespace(reader);
      assertTrue(namespace.equals("eml://ecoinformatics.org/eml-2.0.1"));
      
      reader = new StringReader(defaultNamespace);
      String namespace2 = XMLSchemaService.findDocumentNamespace(reader);
      assertTrue(namespace2.equals("eml://ecoinformatics.org/eml-2.1.1"));
      
      reader = new StringReader(noNamespace);
      namespace = XMLSchemaService.findDocumentNamespace(reader);
      assertTrue(namespace == null);
  }
  
  public void testFindNamespaceAndSchemaLocalLocation() throws Exception {
      String formatId = null;
      String namespace = null;
      String location = null;
      try {
          location = XMLSchemaService.getInstance().findNamespaceAndSchemaLocalLocation(formatId, namespace);
          fail("we can't get here. The above line should throw an exception.");
      } catch (MetacatException e) {
          assertTrue(e.getMessage().contains("not registered in the Metacat"));
      }

      //registered formatid
      formatId = "http://www.isotc211.org/2005/gmd-noaa";
      namespace = null;
      location = XMLSchemaService.getInstance().findNamespaceAndSchemaLocalLocation(formatId, namespace);
      assertTrue(location.contains("/schema/isotc211-noaa/gmd/gmd.xsd"));
      
    //registered formatid
      formatId = "http://www.isotc211.org/2005/gmd-noaa";
      namespace = "http://www.isotc211.org/2005/gmd";
      location = XMLSchemaService.getInstance().findNamespaceAndSchemaLocalLocation(formatId, namespace);
      assertTrue(location.contains("/schema/isotc211-noaa/gmd/gmd.xsd"));
      
      //unregistered formatid
      formatId = "eml://ecoinformatics.org/eml-2.1.0";
      namespace = "eml://ecoinformatics.org/eml-2.1.0";
      location = XMLSchemaService.getInstance().findNamespaceAndSchemaLocalLocation(formatId, namespace);
      assertTrue(location.contains("/schema/eml-2.1.0/eml.xsd "));
      
      //unregistered formatid
      formatId = "http://www.isotc211.org/2005/gco";
      namespace = "http://www.isotc211.org/2005/gco";
      location = XMLSchemaService.getInstance().findNamespaceAndSchemaLocalLocation(formatId, namespace);
      assertTrue(location.contains("/schema/isotc211/gco/gco.xsd"));
      
      formatId = "http://foo.com";
      namespace = "http://foo.com";
      try {
          location = XMLSchemaService.getInstance().findNamespaceAndSchemaLocalLocation(formatId, namespace);
          fail("we can't get here. The above line should throw an exception.");
      } catch (MetacatException e) {
          assertTrue(e.getMessage().contains("not registered in the Metacat"));
      }
  }
  
  public void testFindNoNamespaceSchemaLocalLocation() throws Exception {
      String formatId = null;
      String nonamespaceURI = null;
      String location = null;
      try {
          location = XMLSchemaService.getInstance().findNoNamespaceSchemaLocalLocation(formatId, nonamespaceURI);
          fail("we can't get here. The above line should throw an exception.");
      } catch (MetacatException e) {
          assertTrue(e.getMessage().contains("not registered in the Metacat"));
      }
              
      assertTrue(location == null);
      
      formatId = "FGDC-STD-001-1998";
      nonamespaceURI = null;
      location = XMLSchemaService.getInstance().findNoNamespaceSchemaLocalLocation(formatId, nonamespaceURI);
      assertTrue(location.contains("/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd"));
      
      formatId = "FGDC-STD-001-1998";
      nonamespaceURI = "unkown";
      location = XMLSchemaService.getInstance().findNoNamespaceSchemaLocalLocation(formatId, nonamespaceURI);
      assertTrue(location.contains("/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd"));
      
      formatId = "FGDC-STD-001-1998";
      nonamespaceURI = "https://www.fgdc.gov/metadata/fgdc-std-001-1998.xsd";
      location = XMLSchemaService.getInstance().findNoNamespaceSchemaLocalLocation(formatId, nonamespaceURI);
      assertTrue(location.contains("/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd"));
      
      formatId = "FGDC-STD-001-1998";
      nonamespaceURI = "http://www.fgdc.gov/metadata/fgdc-std-001-1998.xsd";
      location = XMLSchemaService.getInstance().findNoNamespaceSchemaLocalLocation(formatId, nonamespaceURI);
      assertTrue(location.contains("/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd"));
      
      
      formatId = "FGDC-STD-001";
      nonamespaceURI = "http://www.fgdc.gov/metadata/fgdc-std-001-1998.xsd";
      location = XMLSchemaService.getInstance().findNoNamespaceSchemaLocalLocation(formatId, nonamespaceURI);
      assertTrue(location.contains("/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd"));
      
      formatId = "FGDC-STD-001";
      nonamespaceURI = "https://www.fgdc.gov/metadata/fgdc-std-001-1998.xsd";
      location = XMLSchemaService.getInstance().findNoNamespaceSchemaLocalLocation(formatId, nonamespaceURI);
      assertTrue(location.contains("/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd"));
      
      formatId = "foo-001";
      nonamespaceURI = "https://www.fgdc.gov/foo";
      try {
          location = XMLSchemaService.getInstance().findNoNamespaceSchemaLocalLocation(formatId, nonamespaceURI);
          fail("we can't get here. The above line should throw an exception.");
      } catch (MetacatException e) {
          assertTrue(e.getMessage().contains("not registered in the Metacat"));
      }
  }
  
  public void testIsNamespaceRegistered() throws Exception {
      String namespace = null;
      boolean registered = XMLSchemaService.getInstance().isNamespaceRegistered(namespace);
      assertTrue(registered == false);
      
      namespace = "http://foo";
      registered = XMLSchemaService.getInstance().isNamespaceRegistered(namespace);
      assertTrue(!registered);
      
      namespace = "eml://ecoinformatics.org/eml-2.0.1";
      registered = XMLSchemaService.getInstance().isNamespaceRegistered(namespace);
      assertTrue(registered);
  }
  
  public void testFindNoNamespaceSchemaLocationAttr() throws Exception {
      File file = new File("./test/eml-sample.xml");
      StringReader reader = new StringReader(FileUtils.readFileToString(file));
      String noNamespaceLocation = XMLSchemaService.findNoNamespaceSchemaLocationAttr(reader);
      assertTrue(noNamespaceLocation == null);
      
      file = new File("./test/fgdc.xml");
      reader = new StringReader(FileUtils.readFileToString(file));
      noNamespaceLocation = XMLSchemaService.findNoNamespaceSchemaLocationAttr(reader);
      assertTrue(noNamespaceLocation.equals("http://www.fgdc.gov/metadata/fgdc-std-001-1998.xsd"));
  }
}
