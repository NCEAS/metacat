package edu.ucsb.nceas.metacat.dataone;



import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.object.handler.JsonLDHandlerTest;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.utilities.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.exceptions.MarshallingException;
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
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.junit.After;
import org.junit.Before;

/**
 *This really is a integration test. It should be run in a configured DataONE environment. 
 *It will upload a metadata object to a MN with the replication perference of the running node (the local host). 
 *Couple minutes later, it will check if the running node has the replica.
 *The test should be run on mn-sandbox-ucsb2.
 * 
 * @author Jing Tao
 *
 */
public class MNodeReplicationTest extends D1NodeServiceTest {
    public static final String replicationSourceFile = "./test/eml-sample.xml";
    protected static String sourceMNodeId = null;
    protected static int waitTime = 0;

  /**
   * Set up the test fixtures
   * 
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
     sourceMNodeId = PropertyService.getProperty("test.dataone.replication.sourceNodeId");
     System.out.println("The sourceNodeId is ++++++++++++++++++ "+sourceMNodeId);
     waitTime = (new Integer(PropertyService.getProperty("test.dataone.replication.waitingTime"))).intValue();
     System.out.println("The waiting time is ++++++++++++++++++ "+waitTime+ " seconds");
     System.out.println("The cn base url is "+Settings.getConfiguration().getString("D1Client.CN_URL"));
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
    suite.addTest(new MNodeReplicationTest("initialize"));
    suite.addTest(new MNodeReplicationTest("testReplicate"));
    suite.addTest(new MNodeReplicationTest("testReplicateData"));
    suite.addTest(new MNodeReplicationTest("testReplicateJsonLD"));
    return suite;
    
  }
  
  /**
   * Constructor for the tests
   * 
   * @param name - the name of the test
   */
  public MNodeReplicationTest(String name) {
    super(name);
    
  }

  /**
   * Initial blank test
   */
  public void initialize() {
    assertTrue(1 == 1);
    
  }
  
  /**
   *
   * 
   */
  public void testReplicate() {
      printTestHeader("testReplicate");
      try {
          
        //insert an object to the source node
        Session session = null;
        Identifier guid = new Identifier();
        guid.setValue("testReplicate." + System.currentTimeMillis());
        InputStream object = new FileInputStream(new File(replicationSourceFile));
        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
        SystemMetadata sysmeta = createSystemMetadata(guid, subject, object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta.setFormatId(formatId);
        //create a replication policy
        Node localNode = MNodeService.getInstance(request).getCapabilities();
        if(!localNode.isReplicate()) {
            throw new Exception("The local node "+localNode.getIdentifier().getValue()+" is configured to not to accept replicas!");
        }
        ReplicationPolicy rePolicy = new ReplicationPolicy();
        rePolicy.setReplicationAllowed(true);
        rePolicy.setNumberReplicas(new Integer(1));
        rePolicy.addPreferredMemberNode(localNode.getIdentifier());
        sysmeta.setReplicationPolicy(rePolicy);
        
        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue(sourceMNodeId);
        MNode sourceMN = D1Client.getMN(sourceNode);
        Node source = sourceMN.getCapabilities();
        if(!source.isSynchronize()) {
            throw new Exception("The source node "+source.getIdentifier().getValue()+" is configured to not to be synchronized to the cn!");
        }
        object = new FileInputStream(new File(replicationSourceFile));
        sysmeta.setAuthoritativeMemberNode(sourceNode);
        System.out.println("------------------------before creating the object into the source node "+sourceMNodeId+" with id "+guid.getValue());
        sourceMN.create(session, guid, object, sysmeta);
        System.out.println("scucessfully created the object into the source node "+sourceMNodeId+" with id "+guid.getValue());
        Thread.sleep(waitTime);
        MNode local = D1Client.getMN(localNode.getIdentifier());
        SystemMetadata sys = local.getSystemMetadata(session, guid);
        System.out.println("--------------The pid from the replica on the localhost is  "+sys.getIdentifier().getValue());
        assertTrue(sys.getIdentifier().equals(guid));
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Failed to test the replicate method : " + e.getMessage());
        fail("Failed to test the replicate method : " + e.getMessage());
      }
  }
  
  
  /**
   * Test to replicate a data object
   */
  public void testReplicateData() {
      printTestHeader("testReplicateData");
      try {
          
        //insert an object to the source node
        Session session = null;
        Identifier guid = new Identifier();
        guid.setValue("testReplicateData." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
        SystemMetadata sysmeta = createSystemMetadata(guid, subject, object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        //create a replication policy
        Node localNode = MNodeService.getInstance(request).getCapabilities();
        if(!localNode.isReplicate()) {
            throw new Exception("The local node "+localNode.getIdentifier().getValue()+" is configured to not to accept replicas!");
        }
        ReplicationPolicy rePolicy = new ReplicationPolicy();
        rePolicy.setReplicationAllowed(true);
        rePolicy.setNumberReplicas(new Integer(1));
        rePolicy.addPreferredMemberNode(localNode.getIdentifier());
        sysmeta.setReplicationPolicy(rePolicy);
        
        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue(sourceMNodeId);
        MNode sourceMN = D1Client.getMN(sourceNode);
        Node source = sourceMN.getCapabilities();
        if(!source.isSynchronize()) {
            throw new Exception("The source node "+source.getIdentifier().getValue()+" is configured to not to be synchronized to the cn!");
        }
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        sysmeta.setAuthoritativeMemberNode(sourceNode);
        System.out.println("------------------------before creating the object into the source node "+sourceMNodeId+" with id "+guid.getValue());
        sourceMN.create(session, guid, object, sysmeta);
        System.out.println("scucessfully created the object into the source node "+sourceMNodeId+" with id "+guid.getValue());
        Thread.sleep(waitTime);
        MNode local = D1Client.getMN(localNode.getIdentifier());
        SystemMetadata sys = local.getSystemMetadata(session, guid);
        System.out.println("--------------The pid of DATA from the replica on the localhost is  "+sys.getIdentifier().getValue());
        assertTrue(sys.getIdentifier().equals(guid));
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Failed to test the replicate method : " + e.getMessage());
        fail("Failed to test the replicate method : " + e.getMessage());
      }
  }
  
  /**
   * Test to replicate an JsonLD object
   */
  public void testReplicateJsonLD() {
      printTestHeader("testReplicateJsonLD");
      try {
        //insert an object to the source node
        Session session = null;
        Identifier guid = new Identifier();
        guid.setValue("testReplicateJsonLD." + System.currentTimeMillis());
        InputStream object = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
        SystemMetadata sysmeta = createSystemMetadata(guid, subject, object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue(NonXMLMetadataHandlers.JSON_LD);
        sysmeta.setFormatId(formatId);
        //create a replication policy
        Node localNode = MNodeService.getInstance(request).getCapabilities();
        if(!localNode.isReplicate()) {
            throw new Exception("The local node " + localNode.getIdentifier().getValue() + " is configured to not to accept replicas!");
        }
        ReplicationPolicy rePolicy = new ReplicationPolicy();
        rePolicy.setReplicationAllowed(true);
        rePolicy.setNumberReplicas(new Integer(1));
        rePolicy.addPreferredMemberNode(localNode.getIdentifier());
        sysmeta.setReplicationPolicy(rePolicy);
        
        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue(sourceMNodeId);
        MNode sourceMN = D1Client.getMN(sourceNode);
        Node source = sourceMN.getCapabilities();
        if(!source.isSynchronize()) {
            throw new Exception("The source node " + source.getIdentifier().getValue() + " is configured to not to be synchronized to the cn!");
        }
        object =new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        sysmeta.setAuthoritativeMemberNode(sourceNode);
        System.out.println("------------------------before creating the object into the source node "+sourceMNodeId+" with id " + guid.getValue());
        sourceMN.create(session, guid, object, sysmeta);
        System.out.println("scucessfully created the object into the source node "+sourceMNodeId+" with id " + guid.getValue());
        Thread.sleep(waitTime);
        MNode local = D1Client.getMN(localNode.getIdentifier());
        SystemMetadata sys = local.getSystemMetadata(session, guid);
        System.out.println("--------------The pid of JsonLD from the replica on the localhost is  " + sys.getIdentifier().getValue());
        assertTrue(sys.getIdentifier().equals(guid));
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Failed to test the replicate method : " + e.getMessage());
        fail("Failed to test the replicate method : " + e.getMessage());
      }
  }
 
}
