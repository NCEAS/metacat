package edu.ucsb.nceas.metacat.dataone;



import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.D1NodeVersionChecker;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.utilities.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.junit.After;
import org.junit.Before;


/**
 * A JUnit test to exercise the Metacat Member Node service implementation.
 * This also tests a few of the D1NodeService superclass methods
 * 
 * @author cjones
 *
 */
public class D1NodeVersionCheckerTest extends MCTestCase {

    private static String unmatchingEncodingFilePath = "test/incorrect-encoding-declaration.xml";
  /**
   * Set up the test fixtures
   * 
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    // set up the configuration for d1client
    
  }

  /**
   * Remove the test fixtures
   */
  @After
  public void tearDown() {
  }
  
  /**
   * Build the test suite
   * @return
   */
  public static Test suite() {
    
    TestSuite suite = new TestSuite();
    suite.addTest(new D1NodeVersionCheckerTest("initialize"));
    // MNStorage tests
    suite.addTest(new D1NodeVersionCheckerTest("testGetVersion"));
    return suite;
    
  }
  
  /**
   * Constructor for the tests
   * 
   * @param name - the name of the test
   */
  public D1NodeVersionCheckerTest(String name) {
    super(name);
    
  }

  /**
   * Initial blank test
   */
  public void initialize() {
    assertTrue(1 == 1);
    
  }
  
  /**
   * Test getting versions from different cns
   */
  public void testGetVersion() throws Exception {
      Settings.getConfiguration().setProperty("D1Client.CN_URL", "https://cn.dataone.org/cn");
      NodeReference node = new NodeReference();
      node.setValue("urn:node:SEAD");
      D1NodeVersionChecker checker = new D1NodeVersionChecker(node);
      assertTrue(checker.getVersion("MNReplication") == null);
      assertTrue(checker.getVersion("MNRead").equals("v1"));
      assertTrue(checker.getVersion("MNCore").equals("v1"));
     
      
      Settings.getConfiguration().setProperty("D1Client.CN_URL", "https://cn.dataone.org/cn");
      node.setValue("urn:node:KNB");
      checker = new D1NodeVersionChecker(node);
      assertTrue(checker.getVersion("MNReplication").equals("v2"));
      node.setValue("urn:node:CN");
      checker = new D1NodeVersionChecker(node);
      assertTrue(checker.getVersion("MNReplication") == null);
      assertTrue(checker.getVersion("CNRegister").equals("v2"));
  }

  
    
}
