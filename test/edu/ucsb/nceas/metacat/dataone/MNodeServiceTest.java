/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author:$'
 *     '$Date:$'
 * '$Revision:$'
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

package edu.ucsb.nceas.metacat.dataone;




import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.utilities.IOUtil;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.Manifest;

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
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.ore.ResourceMapFactory;
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
import org.jibx.runtime.JiBXException;
import org.junit.After;
import org.junit.Before;

/**
 * A JUnit test to exercise the Metacat Member Node service implementation.
 * This also tests a few of the D1NodeService superclass methods
 * 
 * @author cjones
 *
 */
public class MNodeServiceTest extends D1NodeServiceTest {

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
    Settings.getConfiguration().setProperty("D1Client.cnClassName", MockCNode.class.getName());
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
    suite.addTest(new MNodeServiceTest("initialize"));
    // MNStorage tests
    suite.addTest(new MNodeServiceTest("testCreate"));
    suite.addTest(new MNodeServiceTest("testCreateInvalidIdentifier"));
    suite.addTest(new MNodeServiceTest("testUpdate"));
    // this requires MN certificate
    suite.addTest(new MNodeServiceTest("testDelete"));
    
    // MNRead tests
    suite.addTest(new MNodeServiceTest("testGet"));
    suite.addTest(new MNodeServiceTest("testGetChecksum"));
    suite.addTest(new MNodeServiceTest("testGetSystemMetadata"));
    suite.addTest(new MNodeServiceTest("testDescribe"));
    suite.addTest(new MNodeServiceTest("testListObjects"));
    // this requires CN certificate
    suite.addTest(new MNodeServiceTest("testSynchronizationFailed"));
    
    // MNCore tests
    suite.addTest(new MNodeServiceTest("testPing"));
    suite.addTest(new MNodeServiceTest("testGetLogRecords"));
    suite.addTest(new MNodeServiceTest("testGetCapabilities"));
    
    // MNAuthorization tests
    suite.addTest(new MNodeServiceTest("testIsAuthorized"));
    suite.addTest(new MNodeServiceTest("testIsEquivIdentityAuthorized"));
    suite.addTest(new MNodeServiceTest("testSetAccessPolicy"));
    // MNreplication tests
    suite.addTest(new MNodeServiceTest("testReplicate"));
    // MN packaging tests
    suite.addTest(new MNodeServiceTest("testGetPackage"));
    suite.addTest(new MNodeServiceTest("testGetOREPackage"));
    suite.addTest(new MNodeServiceTest("testReadDeletedObject"));
    suite.addTest(new MNodeServiceTest("testCreateAndUpdateXMLWithUnmatchingEncoding"));
    suite.addTest(new MNodeServiceTest("testGetSID"));
    suite.addTest(new MNodeServiceTest("testListViews"));
    suite.addTest(new MNodeServiceTest("testUpdateSystemMetadata"));
    
    return suite;
    
  }
  
  /**
   * Constructor for the tests
   * 
   * @param name - the name of the test
   */
  public MNodeServiceTest(String name) {
    super(name);
    
  }

  /**
   * Initial blank test
   */
  public void initialize() {
    assertTrue(1 == 1);
    
  }
  
  /**
   * Test getting a known object
   */
  public void testGet() {
    printTestHeader("testGet");

    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testGet." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      InputStream result = MNodeService.getInstance(request).get(session, guid);
      // go back to beginning of original stream
      object.reset();
      // check
      assertTrue(object.available() > 0);
      assertTrue(result.available() > 0);
      assertTrue(IOUtils.contentEquals(result, object));
      
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidToken e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (ServiceFailure e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (NotAuthorized e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (IdentifierNotUnique e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (UnsupportedType e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (InsufficientResources e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (InvalidSystemMetadata e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (NotImplemented e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (InvalidRequest e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (NotFound e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    }

  }

  /**
   * Test getting the system metadata of an object
   */
  public void testGetSystemMetadata() {    
    printTestHeader("testGetSystemMetadata");

    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testGetSystemMetadata." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      SystemMetadata newsysmeta = MNodeService.getInstance(request).getSystemMetadata(session, pid);
      assertEquals(newsysmeta.getIdentifier().getValue(), sysmeta.getIdentifier().getValue());
      assertEquals(newsysmeta.getSeriesId(), null);
      
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidToken e) {
      e.printStackTrace();      
      fail("Unexpected error: " + e.getMessage());

    } catch (ServiceFailure e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotAuthorized e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (IdentifierNotUnique e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (UnsupportedType e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InsufficientResources e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidSystemMetadata e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotImplemented e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidRequest e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotFound e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    }
    
  }

  /**
   * Test object creation
   */
  public void testCreate() {
    printTestHeader("testCreate");
    
    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testCreate." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      assertEquals(guid.getValue(), pid.getValue());
      
      Thread.sleep(1000);
      try {
          Identifier guid2 = new Identifier();
          guid2.setValue("testCreate." + System.currentTimeMillis());
          SystemMetadata sysmeta2 = createSystemMetadata(guid2, session.getSubject(), object);
          sysmeta2.setSeriesId(guid);
          MNodeService.getInstance(request).create(session, guid2, object, sysmeta2);
          fail("It should fail since the system metadata using an existing id as the sid");
      } catch (InvalidSystemMetadata ee) {
          
      }
      
      Thread.sleep(1000);
      try {
          Identifier guid3 = new Identifier();
          guid3.setValue("testCreate." + System.currentTimeMillis());
          SystemMetadata sysmeta3 = createSystemMetadata(guid3, session.getSubject(), object);
          sysmeta3.setSeriesId(guid3);
          MNodeService.getInstance(request).create(session, guid3, object, sysmeta3);
          fail("It should fail since the system metadata using the pid as the sid");
      } catch (InvalidSystemMetadata ee) {
          
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidToken e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (ServiceFailure e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotAuthorized e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (IdentifierNotUnique e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (UnsupportedType e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InsufficientResources e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidSystemMetadata e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotImplemented e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidRequest e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    }
      
  }

  /**
   * test object deletion
   */
  public void testDelete() {
    printTestHeader("testDelete");

    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testDelete." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      
      // use MN admin to delete
      session = getMNSession();
      Identifier deletedPid = MNodeService.getInstance(request).delete(session, pid);
      assertEquals(pid.getValue(), deletedPid.getValue());
      // check that we cannot get the object
      session = getTestSession();
      InputStream deletedObject = null;
      try {
    	  deletedObject = MNodeService.getInstance(request).get(session, deletedPid);
      } catch (NotFound nf) {
    	  // this is expected
      }
	  assertNull(deletedObject);
      
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      
    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } 

  }

  /**
   * Test object updating
   */
  public void testUpdate() {
    printTestHeader("testUpdate");
    
    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testUpdate." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier newPid = new Identifier();
      newPid.setValue("testUpdate." + (System.currentTimeMillis() + 1)); // ensure it is different from original
      Identifier pid = 
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      
      SystemMetadata newSysMeta = createSystemMetadata(newPid, session.getSubject(), object);
            
      // do the update
      Identifier updatedPid = 
        MNodeService.getInstance(request).update(session, pid, object, newPid, newSysMeta);
      
      // get the updated system metadata
      SystemMetadata updatedSysMeta = 
        MNodeService.getInstance(request).getSystemMetadata(session, updatedPid);

      assertEquals(updatedPid.getValue(), newPid.getValue());
//      assertTrue(updatedSysMeta.getObsolete(0).getValue().equals(pid.getValue()));
//      assertTrue(updatedSysMeta.getDerivedFrom(0).getValue().equals(pid.getValue()));        
      
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidToken e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (ServiceFailure e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotAuthorized e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (IdentifierNotUnique e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (UnsupportedType e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InsufficientResources e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidSystemMetadata e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotImplemented e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidRequest e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    }
  }

  /**
   * We currently expect this unit test to fail because it should rely on a different member node
   * to retrieve the object from. Currently it gets the object from itself and throws 
   * and expected error for duplicate entry.
   * 
   */
  public void testReplicate() {
      printTestHeader("testReplicate");
      try {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testReplicate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        // save locally
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        // get our node reference (attempting to replicate with self)
        NodeReference sourceNode = MNodeService.getInstance(request).getCapabilities().getIdentifier();
        // attempt to replicate with ourselves -- this should fail!
      boolean result = false;
      try {
        result = MNodeService.getInstance(request).replicate(session, sysmeta, sourceNode);
      } catch (Exception inu) {
        // we are expecting this to fail since we already have the doc
        result = true;
      }
      assertTrue(result);
      } catch (Exception e) {
        e.printStackTrace();
      fail("Probably not yet implemented: " + e.getMessage());
    }
  }

  /**
   * Test describing an object
   */
  public void testDescribe() {
    printTestHeader("testDescribe");

    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testGetSystemMetadata." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      DescribeResponse describeResponse = MNodeService.getInstance(request).describe(session, pid);
      assertEquals(describeResponse.getDataONE_Checksum().getValue(), sysmeta.getChecksum().getValue());
      assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), sysmeta.getFormatId().getValue());
      
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidToken e) {
      e.printStackTrace();      
      fail("Unexpected error: " + e.getMessage());

    } catch (ServiceFailure e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotAuthorized e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (IdentifierNotUnique e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
      
    } catch (UnsupportedType e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InsufficientResources e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidSystemMetadata e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotImplemented e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidRequest e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotFound e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    }
  }

  /**
   * Test getting the checksum of an object
   */
  public void testGetChecksum() {
    printTestHeader("testGetChecksum");

    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testGetChecksum." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      Checksum checksum = MNodeService.getInstance(request).getChecksum(session, pid, "MD5");
      assertEquals(checksum.getValue(), sysmeta.getChecksum().getValue());
    
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidToken e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (ServiceFailure e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotAuthorized e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (IdentifierNotUnique e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (UnsupportedType e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InsufficientResources e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidSystemMetadata e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotImplemented e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidRequest e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotFound e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    }
    
  }

  /**
   * Testing listing objects on the Member Node
   */
  public void testListObjects() {
      printTestHeader("testListObjects");
  
      try {
  
        Session session = getTestSession();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date startTime = sdf.parse("2010-01-01");
        Date endTime = new Date();
        ObjectFormatIdentifier objectFormatId = null;
        boolean replicaStatus = false;
        int start = 0;
        int count = 1;
      
        // insert at least one object 
        testCreate();
        // now check that we have at least one
        ObjectList objectList = 
          MNodeService.getInstance(request).listObjects(session, startTime, endTime, 
              objectFormatId, null, replicaStatus, start, count);
        assertNotNull(objectList);
        assertTrue(objectList.getCount() == count);
        assertTrue(objectList.getStart() == 0);
        assertTrue(objectList.getTotal() >= 1);
        
      } catch (Exception e) {
        e.printStackTrace();
        fail("Unexpected error: " + e.getMessage());
  
      }
  }

  public void testGetCapabilities() {
      printTestHeader("testGetCapabilities");
    try {
      Node node = MNodeService.getInstance(request).getCapabilities();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      TypeMarshaller.marshalTypeToOutputStream(node, baos);
      assertNotNull(node);
      // TODO: should probably test other parts of the node information
      
    } catch (JiBXException e) {
        e.printStackTrace();
        fail("The node instance couldn't be parsed correctly:" + e.getMessage());
        
    } catch (IOException e) {
        e.printStackTrace();
        fail("The node instance couldn't be read correctly:" + e.getMessage());
        
    } catch (Exception e) {
        e.printStackTrace();
        fail("Probably not yet implemented: " + e.getMessage());
        
    }
    
  }

  public void testPing() {

    try {
      Date mnDate = MNodeService.getInstance(request).ping();
      assertTrue(mnDate != null);
      
    } catch (NotImplemented e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (ServiceFailure e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    }  catch (InsufficientResources e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    }
    
  }

  public void testSynchronizationFailed() {
    printTestHeader("testSynchronizationFailed");
    try {
        Session session = getTestSession();
        
        // create the object
        Identifier pid = new Identifier();
        pid.setValue("testSynchronizationFailed." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(pid, session.getSubject(), object);
        Identifier retPid = MNodeService.getInstance(request).create(session, pid, object, sysmeta);
        assertEquals(retPid.getValue(), pid.getValue());
        
        // pretend the sync failed, act as CN
      SynchronizationFailed syncFailed = 
        new SynchronizationFailed("0000", "Testing Synch Failure");
      syncFailed.setPid(pid.getValue());
      session = getCNSession();
      MNodeService.getInstance(request).synchronizationFailed(session, syncFailed );
    } catch (Exception e) {
      e.printStackTrace();
        fail("Unexpected error: " + e.getMessage());
    }

  }

  public void testSystemMetadataChanged() {
      printTestHeader("testSystemMetadataChanged");
      try {
          Session session = getTestSession();
          
          // create the object
          Identifier pid = new Identifier();
          pid.setValue("testSystemMetadataChanged." + System.currentTimeMillis());
          Identifier sid = new Identifier();
          sid.setValue("testSystemMetadataChangedSid."+System.currentTimeMillis());
          InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
          SystemMetadata sysmeta = createSystemMetadata(pid, session.getSubject(), object);
          sysmeta.setSeriesId(sid);
          Identifier retPid = MNodeService.getInstance(request).create(session, pid, object, sysmeta);
          assertEquals(retPid.getValue(), pid.getValue());
          
          // pretend the system metadata changed on the CN
          MNodeService.getInstance(request).systemMetadataChanged(session, 
                  retPid, 5000L, Calendar.getInstance().getTime());
          MNodeService.getInstance(request).systemMetadataChanged(session, 
                  sid, 5000L, Calendar.getInstance().getTime());
          edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).systemMetadataChanged(session, 
                  retPid, 5000L, Calendar.getInstance().getTime());
          edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).systemMetadataChanged(session, 
                  sid, 5000L, Calendar.getInstance().getTime());
          
      } catch (Exception e) {
          if (e instanceof NotAuthorized) {
              // only CN subjects can call this
              // TODO: use a CN certificate in the tests
          } else {
              fail("Unexpected error: " + e.getMessage());
              
          }
      }

    }

  public void testGetLogRecords() {
    printTestHeader("testLogRecords");

    try {
	    Log log = null;
	    Session session = getCNSession();
	    Date fromDate = new Date();
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(fromDate);
	    calendar.roll(Calendar.YEAR, false);
	    fromDate = calendar.getTime();
	    Date toDate = new Date();
	    Event event = Event.CREATE;
	    int start = 0;
	    int count = 1;
    
      log = MNodeService.getInstance(request).getLogRecords(session, fromDate, toDate, 
        event.xmlValue(), null, start, count);
      
      assertNotNull(log);      
      assertTrue(log.getCount() == count);
      assertTrue(log.getStart() == start);
      assertTrue(log.getTotal() >= 1);
        
    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    }
  }

  /**
   * Testing setting access on a known object
   */
  public void testSetAccessPolicy() {
    printTestHeader("testSetAccess");
    
    //boolean accessWasSet = false;
    //
    //try {
    //  // create an object to set access on
    //  Session session = getTestSession();
    //  Identifier guid = new Identifier();
    //  guid.setValue("testSetAccess." + System.currentTimeMillis());
    //  InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
    //  SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
    //  Identifier pid = 
    //    MNodeService.getInstance(request).create(session, guid, object, sysmeta);
    //  // set the access
    //  AccessPolicy accessPolicy = new AccessPolicy();
    //  AccessRule allow = new AccessRule();
    //  allow.addPermission(Permission.WRITE);
    //  Subject publicSubject = new Subject();
    //  publicSubject.setValue(Constants.SUBJECT_PUBLIC);
    //  allow.addSubject(publicSubject);
    //  accessPolicy.addAllow(allow);
    //  
    //  accessWasSet = 
    //    MNodeService.getInstance(request).setAccessPolicy(session, pid, accessPolicy);
    //  assertTrue(accessWasSet);
    //  // test that it is enforced
    //  session.setSubject(publicSubject);
    //  boolean isAuthorized = MNodeService.getInstance(request).isAuthorized(session, pid, Permission.WRITE);
    //  assertTrue(isAuthorized);
    //
    //} catch (UnsupportedEncodingException e) {
    //  e.printStackTrace();
    //  
    //} catch (InvalidToken e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (ServiceFailure e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (NotAuthorized e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (IdentifierNotUnique e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (UnsupportedType e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (InsufficientResources e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (InvalidSystemMetadata e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (NotImplemented e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (InvalidRequest e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (NotFound e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //} catch (Exception e) {
    //  e.printStackTrace();
    //  fail("Unexpected error: " + e.getMessage());
    //  
    //}
      
  }

  /**
   * Test if a subject is authorized to read a known object
   */
  public void testIsAuthorized() {
    printTestHeader("testIsAuthorized");
    
    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testIsAuthorized." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier pid = 
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      boolean isAuthorized = 
        MNodeService.getInstance(request).isAuthorized(session, pid, Permission.READ);
      assertEquals(isAuthorized, true);
      
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidToken e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (ServiceFailure e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotAuthorized e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (IdentifierNotUnique e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (UnsupportedType e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InsufficientResources e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidSystemMetadata e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (NotImplemented e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (InvalidRequest e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    }
  }
  
  /**
   * Test if node admin is authorized to read a known object
   */
  public void testIsAdminAuthorized() {
    printTestHeader("testIsAdminAuthorized");
    
    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testIsAdminAuthorized." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier pid = 
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      
      // test as public - read
      boolean isAuthorized = 
        MNodeService.getInstance(request).isAuthorized(null, pid, Permission.READ);
      assertEquals(isAuthorized, true);
      
      // test as public - change perm
      isAuthorized = 
        MNodeService.getInstance(request).isAuthorized(null, pid, Permission.CHANGE_PERMISSION);
      assertEquals(isAuthorized, false);
      
      // test as admin
      isAuthorized = 
    	        MNodeService.getInstance(request).isAuthorized(getMNSession(), pid, Permission.CHANGE_PERMISSION);
    	      assertEquals(isAuthorized, true);
      
    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());

    } 
  }

  
  public void testIsEquivIdentityAuthorized() {
      printTestHeader("testIsEquivIdentityAuthorized");

      try {
          Session session = new Session();
          Subject s = new Subject();
          s.setValue("cn=test,dc=dataone,dc=org");
          session.setSubject(s);
          
          Identifier pid = new Identifier();
          pid.setValue("testIsEquivIdentityAuthorized." + System.currentTimeMillis());
          InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
          SystemMetadata sysmeta = createSystemMetadata(pid, session.getSubject(), object);
          
          // reset the access policy to only allow 'self' read (no public)
          AccessPolicy ap = new AccessPolicy();
          AccessRule ar = new AccessRule();
          List<Subject> sList = new ArrayList<Subject>();
          sList.add(session.getSubject());
          ar.setSubjectList(sList);
          List<Permission> permList = new ArrayList<Permission>();
          permList.add(Permission.CHANGE_PERMISSION);
          ar.setPermissionList(permList);
          ap.addAllow(ar);
          sysmeta.setAccessPolicy(ap);
          
          // save it
          Identifier retPid = CNodeService.getInstance(request).registerSystemMetadata(session, pid, sysmeta);
          assertEquals(pid.getValue(), retPid.getValue());
          
          //check it against an equivalent identity not listed in the access policy
          session.getSubject().setValue("cn=newSubject,dc=dataone,dc=org");
          SubjectInfo subjectInfo = new SubjectInfo();
          Person person = new Person();
          person.setSubject(session.getSubject());
          List<String> givenNames = new ArrayList<String>();
          givenNames.add("New");
          person.setGivenNameList(givenNames);
          person.setFamilyName("Subject");
          
          // add equivalent identities
          List<Subject> equivIdentities = new ArrayList<Subject>();
          Subject mappedSubject2 = new Subject();
          mappedSubject2.setValue("cn=test2,dc=dataone,dc=org");
          equivIdentities.add(mappedSubject2);
          
          Subject mappedSubject = new Subject();
          mappedSubject.setValue("cn=test,dc=dataone,dc=org");
          equivIdentities.add(mappedSubject);          
          
          person.setEquivalentIdentityList(equivIdentities);
          
          List<Person> personList = new ArrayList<Person>();
          personList.add(person);
          subjectInfo.setPersonList(personList);
          
          // update the session to include subject info with a mapped identity
          session.setSubjectInfo(subjectInfo);
          boolean result = CNodeService.getInstance(request).isAuthorized(session, pid, Permission.READ);
          assertTrue(result);
        
    } catch (Exception e) {
        e.printStackTrace();
        
    }
    
  }

/**
   * Test object creation failure when there is a space in the identifier
   */
  public void testCreateInvalidIdentifier() {
    printTestHeader("testCreateInvalidIdentifier");
    
    try {
      Session session = getTestSession();
      Identifier guid = new Identifier();
      guid.setValue("testCreate withspace." + System.currentTimeMillis());
      InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
      SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
      Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
      fail("Should not be able to create with whitespace in indentifier");
    } catch (InvalidRequest e) {
    	// expect that this request fails
        assertTrue(true);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error: " + e.getMessage());
    }
      
  }
  
	/**
	 * Test getting a known object
	 */
	public void testGetPackage() {
		printTestHeader("testGetPackage");

		try {
			Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testGetPackage." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
			InputStream bagStream = MNodeService.getInstance(request).getPackage(session, null, pid);
			File bagFile = File.createTempFile("bagit.", ".zip");
			IOUtils.copy(bagStream, new FileOutputStream(bagFile));
			BagFactory bagFactory = new BagFactory();
			Bag bag = bagFactory.createBag(bagFile);
			InputStream result = bag.getPayload().iterator().next().newInputStream();
			
			// go back to beginning of original stream
			object.reset();
			// check
			assertTrue(object.available() > 0);
			assertTrue(result.available() > 0);
			assertTrue(IOUtils.contentEquals(result, object));
			
			// clean up
			bagFile.delete();

		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected error: " + e.getMessage());
		}
	}
	
	
	/**
	 * Test getting a known object
	 */
	public void testGetOREPackage() {
		printTestHeader("testGetOREPackage");

		try {
			
			// construct the ORE package
			Identifier resourceMapId = new Identifier();
			//resourceMapId.setValue("doi://1234/AA/map.1.1");
			resourceMapId.setValue("testGetOREPackage." + System.currentTimeMillis());
			Identifier metadataId = new Identifier();
			metadataId.setValue("doi://1234/AA/meta.1." + + System.currentTimeMillis());
			List<Identifier> dataIds = new ArrayList<Identifier>();
			Identifier dataId = new Identifier();
			dataId.setValue("doi://1234/AA/data.1." + System.currentTimeMillis());
			Identifier dataId2 = new Identifier();
			dataId2.setValue("doi://1234/AA/data.2." + System.currentTimeMillis());
			dataIds.add(dataId);
			dataIds.add(dataId2);
			Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
			idMap.put(metadataId, dataIds);
			ResourceMapFactory rmf = ResourceMapFactory.getInstance();
			ResourceMap resourceMap = rmf.createResourceMap(resourceMapId, idMap);
			assertNotNull(resourceMap);
			String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
			assertNotNull(rdfXml);
			
			Session session = getTestSession();
			InputStream object = null;
			SystemMetadata sysmeta = null;
			
			// save the data objects (data just contains their ID)
			InputStream dataObject1 = new ByteArrayInputStream(dataId.getValue().getBytes("UTF-8"));
			sysmeta = createSystemMetadata(dataId, session.getSubject(), dataObject1);
			MNodeService.getInstance(request).create(session, dataId, dataObject1, sysmeta);
			// second data file
			InputStream dataObject2 = new ByteArrayInputStream(dataId2.getValue().getBytes("UTF-8"));
			sysmeta = createSystemMetadata(dataId2, session.getSubject(), dataObject2);
			MNodeService.getInstance(request).create(session, dataId2, dataObject2, sysmeta);
			// metadata file
			InputStream metadataObject = new ByteArrayInputStream(metadataId.getValue().getBytes("UTF-8"));
			sysmeta = createSystemMetadata(metadataId, session.getSubject(), metadataObject);
			MNodeService.getInstance(request).create(session, metadataId, metadataObject, sysmeta);
						
			// save the ORE object
			object = new ByteArrayInputStream(rdfXml.getBytes("UTF-8"));
			sysmeta = createSystemMetadata(resourceMapId, session.getSubject(), object);
			sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("http://www.openarchives.org/ore/terms").getFormatId());
			Identifier pid = MNodeService.getInstance(request).create(session, resourceMapId, object, sysmeta);
			
			// get the package we uploaded
			InputStream bagStream = MNodeService.getInstance(request).getPackage(session, null, pid);
			File bagFile = File.createTempFile("bagit.", ".zip");
			IOUtils.copy(bagStream, new FileOutputStream(bagFile));
			BagFactory bagFactory = new BagFactory();
			Bag bag = bagFactory.createBag(bagFile);
			Iterator<Manifest> manifestIter = bag.getTagManifests().iterator();
			while (manifestIter.hasNext()) {
				String filepath = manifestIter.next().getFilepath();
				BagFile entryFile = bag.getBagFile(filepath);
				InputStream result = entryFile.newInputStream();
				// check ORE
				if (filepath.contains(resourceMapId.getValue())) {
					object.reset();
					assertTrue(object.available() > 0);
					assertTrue(result.available() > 0);
					assertTrue(IOUtils.contentEquals(result, object));
				}
				// check metadata
				if (filepath.contains(metadataId.getValue())) {
					metadataObject.reset();
					assertTrue(metadataObject.available() > 0);
					assertTrue(result.available() > 0);
					assertTrue(IOUtils.contentEquals(result, metadataObject));
				}
				if (filepath.contains(dataId.getValue())) {
					dataObject1.reset();
					assertTrue(dataObject1.available() > 0);
					assertTrue(result.available() > 0);
					assertTrue(IOUtils.contentEquals(result, dataObject1));
				}
				if (filepath.contains(dataId2.getValue())) {
					dataObject2.reset();
					assertTrue(dataObject2.available() > 0);
					assertTrue(result.available() > 0);
					assertTrue(IOUtils.contentEquals(result, dataObject2));
				}
				
				
			}
			
			// clean up
			bagFile.delete();
			
			// test the ORE lookup
			List<Identifier> oreIds = MNodeService.getInstance(request).lookupOreFor(metadataId, true);
			assertTrue(oreIds.contains(resourceMapId));

		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected error: " + e.getMessage());
		}
	}
	
	/**
     * Test the extra "delete information" was added to the NotFoundException
     * if the object was delete in the following methods:
     * MN.get
     * MN.getSystemmetadata
     * MN.describe
     * MN.getChecksum
     * MN.getRelica
     */
    public void testReadDeletedObject() {
        printTestHeader("testDelete");

        try {
          Session session = getTestSession();
          Identifier guid = new Identifier();
          guid.setValue("testDelete." + System.currentTimeMillis());
          InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
          SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
          Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
          Thread.sleep(3000);
          // use MN admin to delete
          session = getMNSession();
          Identifier deletedPid = MNodeService.getInstance(request).delete(session, pid);
          System.out.println("after deleting");
          assertEquals(pid.getValue(), deletedPid.getValue());
          // check that we cannot get the object
          session = getTestSession();
          InputStream deletedObject = null;
          try {
              //System.out.println("before read ===============");
              deletedObject = MNodeService.getInstance(request).get(session, deletedPid);
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              assertTrue(nf.getMessage().contains("deleted"));
          }
          try {
              //System.out.println("before read ===============");
              SystemMetadata sysmeta2 = MNodeService.getInstance(request).getSystemMetadata(session, deletedPid);
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              //System.out.println("the exception is "+nf.getMessage());
              assertTrue(nf.getMessage().contains("deleted"));
          }
          
          try {
              //System.out.println("before read ===============");
              DescribeResponse describeResponse = MNodeService.getInstance(request).describe(session, pid);
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              //System.out.println("the exception is "+nf.getMessage());
              assertTrue(nf.getMessage().contains("deleted"));
          }
          
          try {
              //System.out.println("before read ===============");
              Checksum checksum = MNodeService.getInstance(request).getChecksum(session, pid, "MD5");
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              //System.out.println("the exception 3 is "+nf.getMessage());
              assertTrue(nf.getMessage().contains("deleted"));
          }
          
          try {
              //System.out.println("before read ===============");
              boolean isAuthorized = 
                      MNodeService.getInstance(request).isAuthorized(session, pid, Permission.READ);
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              System.out.println("the exception 4 is "+nf.getMessage());
              assertTrue(nf.getMessage().contains("deleted"));
          }
          
          assertNull(deletedObject);
          
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          
        } catch (Exception e) {
          e.printStackTrace();
          fail("Unexpected error: " + e.getMessage());

        } 
    }
    
    /**
     * Test to create and update a metadata which xml declaration is ASCII, but actually
     * has some special charaters. The saved document should has the same bytes as the origianl.
     */
    public void testCreateAndUpdateXMLWithUnmatchingEncoding() throws Exception {
          String algorithm = "md5";
          Session session = getTestSession();
          Identifier guid = new Identifier();
          guid.setValue("testCreateAndUpdate." + System.currentTimeMillis());
          InputStream object = new ByteArrayInputStream(FileUtils.readFileToByteArray(new File(unmatchingEncodingFilePath)));
          Checksum orgChecksum = ChecksumUtil.checksum(object, algorithm);
          //System.out.println("the original checksum is "+orgChecksum.getValue());
          SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
          Identifier pid = 
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
          InputStream readResult = MNodeService.getInstance(request).get(session, pid);
          byte[] readBytes = IOUtils.toByteArray(readResult);
          Checksum checksum1 = ChecksumUtil.checksum(readBytes, algorithm);
          //System.out.println("the read checksum1 is "+checksum1.getValue());
          assertEquals(orgChecksum.getValue(), checksum1.getValue());
          
          Identifier newPid = new Identifier();
          newPid.setValue("testCreateAndUpdate." + (System.currentTimeMillis() + 1)); // ensure it is different from original
          SystemMetadata newSysMeta = createSystemMetadata(newPid, session.getSubject(), object);
                
          // do the update
          Identifier updatedPid = 
            MNodeService.getInstance(request).update(session, pid, object, newPid, newSysMeta);
          InputStream readResult2 = MNodeService.getInstance(request).get(session, updatedPid);
          byte[] readBytes2 = IOUtils.toByteArray(readResult2);
          Checksum checksum2 = ChecksumUtil.checksum(readBytes2, algorithm);
          assertEquals(orgChecksum.getValue(), checksum2.getValue());
          //System.out.println("the read checksum2 is "+checksum2.getValue());

          
    }
  
    /**
     * Test the method - get api  for a speicified SID
     */
    public void testGetSID() {
        String str1 = "object1";
        String str2 = "object2";
        String str3 = "object3";
        Date fromDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fromDate);
        calendar.roll(Calendar.YEAR, false);
        fromDate = calendar.getTime();
        try {
            //insert test documents with a series id
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue(generateDocumentId());
            InputStream object1 = new ByteArrayInputStream(str1.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object1);
            String sid1= "sid."+System.nanoTime();
            Identifier seriesId = new Identifier();
            seriesId.setValue(sid1);
            System.out.println("the first sid is "+seriesId.getValue());
            sysmeta.setSeriesId(seriesId);
            MNodeService.getInstance(request).create(session, guid, object1, sysmeta);
            System.out.println("the first pid is "+guid.getValue());
            //test the get(pid) for v2
            InputStream result = MNodeService.getInstance(request).get(session, guid);
            // go back to beginning of original stream
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result.available() > 0);
            assertTrue(IOUtils.contentEquals(result, object1));
            // test the get(id) for v2
            InputStream result1 = MNodeService.getInstance(request).get(session, seriesId);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result1.available() > 0);
            assertTrue(IOUtils.contentEquals(result1, object1));
            //test the get(pid) for v1
            InputStream result2 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, guid);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result2.available() > 0);
            assertTrue(IOUtils.contentEquals(result2, object1));
            //test the get(sid) for v1
            try {
                InputStream result3 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, seriesId);
                fail("the get(sid) methoud should throw a not found exception for the sid "+seriesId.getValue());
            } catch (NotFound ee) {
                
            }
            SystemMetadata metadata = MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertTrue(metadata.getIdentifier().getValue().equals(guid.getValue()));
            assertTrue(metadata.getSeriesId().getValue().equals(seriesId.getValue()));
            DescribeResponse describeResponse = MNodeService.getInstance(request).describe(session, seriesId);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata.getFormatId().getValue());
            
            metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(metadata.getIdentifier().getValue().equals(guid.getValue()));
            assertTrue(metadata.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = MNodeService.getInstance(request).describe(session, guid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata.getFormatId().getValue());
            
            org.dataone.service.types.v1.SystemMetadata sys1=edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(metadata.getIdentifier().getValue().equals(guid.getValue()));
            
            try {
                org.dataone.service.types.v1.SystemMetadata sys2=edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
                fail("the getSystemMetadata(sid) methoud should throw a not found exception for the sid "+seriesId.getValue());
            } catch(NotFound nf2) {
                
            }
            
            describeResponse = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).describe(session, guid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), sys1.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), sys1.getFormatId().getValue());
            try {
                describeResponse = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).describe(session, seriesId);
                fail("the describe(sid) methoud should throw a not found exception for the sid "+seriesId.getValue());
            } catch(NotFound nf2) {
                
            }
            
            Checksum sum = MNodeService.getInstance(request).getChecksum(session, guid, "md5");
            assertTrue(sum.getValue().equals("5b78f9689b9aab1ebc0f3c1df916dd97"));
            
            try {
                sum = MNodeService.getInstance(request).getChecksum(session, seriesId, "md5");
                fail("the getCheckSum shouldn't work for sid");
            } catch(NotFound nf3) {
                
            }
            
            sum = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).getChecksum(session, guid, "md5");
            assertTrue(sum.getValue().equals("5b78f9689b9aab1ebc0f3c1df916dd97"));
            
            try {
                sum = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).getChecksum(session, seriesId, "md5");
                fail("the getCheckSum shouldn't work for sid");
            } catch(NotFound nf3) {
                
            }
            
            boolean isAuthorized = 
                    MNodeService.getInstance(request).isAuthorized(session, guid, Permission.READ);
            assertEquals(isAuthorized, true);
            
            isAuthorized = 
                    MNodeService.getInstance(request).isAuthorized(session, seriesId, Permission.READ);
            assertEquals(isAuthorized, true);
            
            isAuthorized = 
                    edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).isAuthorized(session, guid, Permission.READ);
            assertEquals(isAuthorized, true);
            
            try {
                isAuthorized = 
                        edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).isAuthorized(session, seriesId, Permission.READ);
                fail("we can't reach here since the v1 isAuthorized method doesn't suppport series id");
            } catch (NotFound e) {
                
            }
            
            Session cnsession = getCNSession();
            Date toDate = new Date();
            Event event = Event.READ;
            int start = 0;
            int count = 1;
          Log log = MNodeService.getInstance(request).getLogRecords(cnsession, null, null, 
            event.xmlValue(), seriesId.getValue(), start, count);
          
          assertNotNull(log);      
          assertTrue(log.getCount() == count);
          assertTrue(log.getStart() == start);
          assertTrue(log.getTotal() >= 1);
          assertTrue(log.getLogEntry(0).getIdentifier().equals(guid));

            //do a update with the same series id
            Thread.sleep(1000);
            Identifier newPid = new Identifier();
            newPid.setValue(generateDocumentId()+"1");
            System.out.println("the second pid is "+newPid.getValue());
            InputStream object2 = new ByteArrayInputStream(str2.getBytes("UTF-8"));
            SystemMetadata newSysMeta = createSystemMetadata(newPid, session.getSubject(), object2);
            newSysMeta.setObsoletes(guid);
            newSysMeta.setSeriesId(seriesId);
            MNodeService.getInstance(request).update(session, guid, object2, newPid, newSysMeta);
           
            InputStream result4 = MNodeService.getInstance(request).get(session, guid);
            // go back to beginning of original stream
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result4.available() > 0);
            assertTrue(IOUtils.contentEquals(result4, object1));
            
            InputStream result5 = MNodeService.getInstance(request).get(session, newPid);
            // go back to beginning of original stream
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result5.available() > 0);
            assertTrue(IOUtils.contentEquals(result5, object2));
            

            InputStream result6 = MNodeService.getInstance(request).get(session, seriesId);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result6.available() > 0);
            assertTrue(IOUtils.contentEquals(result6, object2));
            //test the get(pid) for v1
            InputStream result7 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, guid);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result7.available() > 0);
            assertTrue(IOUtils.contentEquals(result7, object1));
            
            InputStream result8 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, newPid);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result8.available() > 0);
            assertTrue(IOUtils.contentEquals(result8, object2));
            //test the get(sid) for v1
            try {
                InputStream result3 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, seriesId);
                fail("the get(sid) methoud should throw a not found exception for the sid "+seriesId.getValue());
            } catch (NotFound ee) {
                
            }
            
            SystemMetadata metadata1 = MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertTrue(metadata1.getIdentifier().getValue().equals(newPid.getValue()));
            assertTrue(metadata1.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = MNodeService.getInstance(request).describe(session, seriesId);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata1.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata1.getFormatId().getValue());
            
            SystemMetadata metadata2 = MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(metadata2.getIdentifier().getValue().equals(guid.getValue()));
            assertTrue(metadata2.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = MNodeService.getInstance(request).describe(session, guid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata2.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata2.getFormatId().getValue());
            
            SystemMetadata metadata3 = MNodeService.getInstance(request).getSystemMetadata(session, newPid);
            assertTrue(metadata3.getIdentifier().getValue().equals(newPid.getValue()));
            assertTrue(metadata3.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = MNodeService.getInstance(request).describe(session, newPid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata3.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata3.getFormatId().getValue());
            
            //do another update with different series id
            Thread.sleep(1000);
            String sid2 = "sid."+System.nanoTime();
            Identifier seriesId2= new Identifier();
            seriesId2.setValue(sid2);
            System.out.println("the second sid is "+seriesId2.getValue());
            Identifier newPid2 = new Identifier();
            newPid2.setValue(generateDocumentId()+"2");
            System.out.println("the third pid is "+newPid2.getValue());
            InputStream object3 = new ByteArrayInputStream(str3.getBytes("UTF-8"));
            SystemMetadata sysmeta3 = createSystemMetadata(newPid2, session.getSubject(), object3);
            sysmeta3.setObsoletes(newPid);
            sysmeta3.setSeriesId(seriesId2);
            MNodeService.getInstance(request).update(session, newPid, object3, newPid2, sysmeta3);
            
            InputStream result9 = MNodeService.getInstance(request).get(session, guid);
            // go back to beginning of original stream
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result9.available() > 0);
            assertTrue(IOUtils.contentEquals(result9, object1));
            
            InputStream result10 = MNodeService.getInstance(request).get(session, newPid);
            // go back to beginning of original stream
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result10.available() > 0);
            assertTrue(IOUtils.contentEquals(result10, object2));
            
            
            InputStream result11 = MNodeService.getInstance(request).get(session, newPid2);
            // go back to beginning of original stream
            object3.reset();
            // check
            assertTrue(object3.available() > 0);
            assertTrue(result11.available() > 0);
            assertTrue(IOUtils.contentEquals(result11, object3));
            
            InputStream result12 = MNodeService.getInstance(request).get(session, seriesId2);
            object3.reset();
            // check
            assertTrue(object3.available() > 0);
            assertTrue(result12.available() > 0);
            assertTrue(IOUtils.contentEquals(result12, object3));
            
            InputStream result16 = MNodeService.getInstance(request).get(session, seriesId);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result16.available() > 0);
            assertTrue(IOUtils.contentEquals(result16, object2));
           
            //test the get(pid) for v1
            InputStream result13 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, guid);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result13.available() > 0);
            assertTrue(IOUtils.contentEquals(result13, object1));
            
            InputStream result14 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, newPid);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result14.available() > 0);
            assertTrue(IOUtils.contentEquals(result14, object2));
            
            InputStream result15 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, newPid2);
            object3.reset();
            // check
            assertTrue(object3.available() > 0);
            assertTrue(result15.available() > 0);
            assertTrue(IOUtils.contentEquals(result15, object3));
            
            SystemMetadata metadata4 = MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertTrue(metadata4.getIdentifier().getValue().equals(newPid.getValue()));
            assertTrue(metadata4.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = MNodeService.getInstance(request).describe(session, seriesId);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata4.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata4.getFormatId().getValue());
            
            SystemMetadata metadata5 = MNodeService.getInstance(request).getSystemMetadata(session, seriesId2);
            assertTrue(metadata5.getIdentifier().getValue().equals(newPid2.getValue()));
            assertTrue(metadata5.getSeriesId().getValue().equals(seriesId2.getValue()));
            describeResponse = MNodeService.getInstance(request).describe(session, seriesId2);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata5.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata5.getFormatId().getValue());
            
            SystemMetadata metadata6 = MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(metadata6.getIdentifier().getValue().equals(guid.getValue()));
            assertTrue(metadata6.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = MNodeService.getInstance(request).describe(session, guid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata6.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata6.getFormatId().getValue());
            
            SystemMetadata metadata7 = MNodeService.getInstance(request).getSystemMetadata(session, newPid);
            assertTrue(metadata7.getIdentifier().getValue().equals(newPid.getValue()));
            assertTrue(metadata7.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = MNodeService.getInstance(request).describe(session, newPid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata7.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata7.getFormatId().getValue());
            
            SystemMetadata metadata8 = MNodeService.getInstance(request).getSystemMetadata(session, newPid2);
            assertTrue(metadata8.getIdentifier().getValue().equals(newPid2.getValue()));
            assertTrue(metadata8.getSeriesId().getValue().equals(seriesId2.getValue()));
            describeResponse = MNodeService.getInstance(request).describe(session, newPid2);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(), metadata8.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(), metadata8.getFormatId().getValue());
            
            
            
            
            //test the get(sid) for v1
            try {
                InputStream result3 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, seriesId);
                fail("the get(sid) methoud should throw a not found exception for the sid "+seriesId.getValue());
            } catch (NotFound ee) {
                
            }
            
            //test the get(sid) for v1
            try {
                InputStream result3 = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, seriesId2);
                fail("the get(sid) methoud should throw a not found exception for the sid "+seriesId.getValue());
            } catch (NotFound ee) {
                
            }
            
            //test to get non-existing id for v2
            try {
             // the pid should be null when we try to get a no-exist sid
                Identifier non_exist_sid = new Identifier();
                non_exist_sid.setValue("no-sid-exist-123qwe");
                InputStream result3 = MNodeService.getInstance(request).get(session, non_exist_sid);
                fail("the get(sid) methoud should throw a not found exception for the sid "+seriesId.getValue());
            } catch (NotFound ee) {
                
            }
            
            try {
                // the pid should be null when we try to get a no-exist sid
                   Identifier non_exist_sid = new Identifier();
                   non_exist_sid.setValue("no-sid-exist-123qwe");
                   SystemMetadata result3 = MNodeService.getInstance(request).getSystemMetadata(session, non_exist_sid);
                   fail("the getSystemMetadata(sid) methoud should throw a not found exception for the sid "+seriesId.getValue());
            } catch (NotFound ee) {
                   
            }
            
            try {
                // the pid should be null when we try to get a no-exist sid
                   Identifier non_exist_sid = new Identifier();
                   non_exist_sid.setValue("no-sid-exist-123qwe");
                    MNodeService.getInstance(request).describe(session, non_exist_sid);
                   fail("the describe(sid) methoud should throw a not found exception for the sid "+seriesId.getValue());
               } catch (NotFound ee) {
                   
               }
            
            toDate = new Date();
            event = Event.READ;
            start = 0;
            count = 1;
            log = MNodeService.getInstance(request).getLogRecords(cnsession, null, null, 
            event.xmlValue(), seriesId.getValue(), start, count);
          
            assertNotNull(log);      
            assertTrue(log.getCount() == count);
            assertTrue(log.getStart() == start);
            assertTrue(log.getTotal() >= 1);
            assertTrue(log.getLogEntry(0).getIdentifier().equals(newPid));
            
            //do another update with invalid series ids
            Thread.sleep(1000);
            Identifier newPid3 = new Identifier();
            newPid3.setValue(generateDocumentId()+"3");
            System.out.println("the third pid is "+newPid3.getValue());
            InputStream object4 = new ByteArrayInputStream(str3.getBytes("UTF-8"));
            SystemMetadata sysmeta4 = createSystemMetadata(newPid3, session.getSubject(), object4);
            sysmeta4.setObsoletes(newPid2);
            sysmeta4.setSeriesId(seriesId);
            try {
                MNodeService.getInstance(request).update(session, newPid2, object4, newPid3, sysmeta4);
                fail("we can't reach here since the sid is using an old one ");
            } catch (InvalidSystemMetadata eee) {
                
            } 
            
            sysmeta4.setSeriesId(newPid3);
            try {
                MNodeService.getInstance(request).update(session, newPid2, object4, newPid3, sysmeta4);
                fail("we can't reach here since the sid is using the pid ");
            } catch (InvalidSystemMetadata eee) {
                
            } 
            
            //test archive a series id by v1
            try {
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).archive(session, seriesId2);
                fail("we can't reach here since the v1 archive method doesn't support the sid ");
            } catch (NotFound nf2) {
                
            }
            
            // test delete a series id by v1
            Session mnSession = getMNSession();
            try {
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).delete(mnSession, seriesId2);
                fail("we can't reach here since the v1 delete method doesn't support the sid ");
            } catch (NotFound nf2) {
                
            }
            
            // test archive a series id by v2
            MNodeService.getInstance(request).archive(session, seriesId2);
            SystemMetadata archived = MNodeService.getInstance(request).getSystemMetadata(session, seriesId2);
            assertTrue(archived.getArchived());
            archived = MNodeService.getInstance(request).getSystemMetadata(session, newPid2);
            assertTrue(archived.getArchived());
            
            // test delete a series id by v2
            MNodeService.getInstance(request).delete(mnSession, seriesId2);
            try {
                MNodeService.getInstance(request).get(session, seriesId2);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                System.out.println("the message is ============="+nf3.getMessage());
                //assertTrue(nf3.getMessage().indexOf("delete") >0);
            }
            
            try {
                MNodeService.getInstance(request).get(session, newPid2);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                //System.out.println("the message is ============="+nf3.getMessage());
                assertTrue(nf3.getMessage().indexOf("delete") >0);
            }
            
            try {
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request).get(session, newPid2);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                System.out.println("the message is ============="+nf3.getMessage());
                assertTrue(nf3.getMessage().indexOf("delete") >0);
            }
            
            //archive seriesId
            MNodeService.getInstance(request).archive(mnSession, seriesId);
            archived = MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertTrue(archived.getArchived());
            archived = MNodeService.getInstance(request).getSystemMetadata(session, newPid);
            assertTrue(archived.getArchived());
            
            
            //delete seriesId
            MNodeService.getInstance(request).delete(mnSession, seriesId);
            try {
                MNodeService.getInstance(request).get(session, newPid);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                //System.out.println("the message is ============="+nf3.getMessage());
                assertTrue(nf3.getMessage().indexOf("delete") >0);
            }
            SystemMetadata meta = MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertTrue(meta.getIdentifier().getValue().equals(guid.getValue()));
            
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        
        
        
    }
    
    /**
     * Test the listView methods.
     * @throws Excpetion
     */
    public void testListViews() throws Exception {
        Session session = null;
        OptionList list = MNodeService.getInstance(request).listViews(session);
        assertTrue(list.sizeOptionList() >0);
        List<String> names = list.getOptionList();
        for(String name : names) {
            System.out.println("It has the view named "+name);
        }
    }
    
    public void testUpdateSystemMetadata() throws Exception {
        String str1 = "object1";
        String str2 = "object2";
        String str3 = "object3";

        //insert test documents with a series id
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(generateDocumentId());
        InputStream object1 = new ByteArrayInputStream(str1.getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object1);
        String sid1= "sid."+System.nanoTime();
        Identifier seriesId = new Identifier();
        seriesId.setValue(sid1);
        System.out.println("the first sid is "+seriesId.getValue());
        sysmeta.setSeriesId(seriesId);
        sysmeta.setArchived(false);
        MNodeService.getInstance(request).create(session, guid, object1, sysmeta);
        //Test the generating object succeeded. 
        SystemMetadata metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertTrue(metadata.getIdentifier().equals(guid));
        assertTrue(metadata.getArchived().equals(false));
        System.out.println("the checksum from request is "+metadata.getChecksum().getValue());
        assertTrue(metadata.getSize().equals(sysmeta.getSize()));
        
        //update system metadata sucessfully
        metadata.setArchived(true);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
        SystemMetadata metadata2 = MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
        assertTrue(metadata2.getIdentifier().equals(guid));
        assertTrue(metadata2.getSeriesId().equals(seriesId));
        assertTrue(metadata2.getArchived().equals(true));
        assertTrue(metadata2.getChecksum().getValue().equals(metadata.getChecksum().getValue()));
        
        Identifier newId = new Identifier();
        newId.setValue("newValue");
        metadata.setIdentifier(newId);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        
        newId.setValue("newValue");
        metadata.setSeriesId(newId);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        
        Date newDate = new Date();
        metadata.setDateUploaded(newDate);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        
        Checksum checkSum = new Checksum();
        checkSum.setValue("12345");
        metadata.setChecksum(checkSum);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        
        BigInteger size = new BigInteger("4000");
        metadata.setSize(size);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
}
    
}
