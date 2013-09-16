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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.IOUtils;
import org.dataone.client.ObjectFormatCache;
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
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.MonitorList;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.SystemMetadata;
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
    suite.addTest(new MNodeServiceTest("testGetOperationStatistics"));
    suite.addTest(new MNodeServiceTest("testGetCapabilities"));
    // include these when they are part of the MN interface definitions
    // suite.addTest(new MNodeServiceTest("testGetObjectStatistics"));
    // suite.addTest(new MNodeServiceTest("testGetStatus"));
    // MNAuthorization tests
    suite.addTest(new MNodeServiceTest("testIsAuthorized"));
    suite.addTest(new MNodeServiceTest("testIsEquivIdentityAuthorized"));
    suite.addTest(new MNodeServiceTest("testSetAccessPolicy"));
    // MNreplication tests
    suite.addTest(new MNodeServiceTest("testReplicate"));
    // MN packaging tests
    suite.addTest(new MNodeServiceTest("testGetPackage"));
    suite.addTest(new MNodeServiceTest("testGetOREPackage"));
    
    
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
              objectFormatId, replicaStatus, start, count);
        assertNotNull(objectList);
        assertTrue(objectList.getCount() == count);
        assertTrue(objectList.getStart() == 0);
        assertTrue(objectList.getTotal() > 1);
        
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

  public void testGetOperationStatistics() {
      printTestHeader("testGetOperationStatistics");
    try {
      Session session = getCNSession();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date startTime = sdf.parse("2010-01-01");
        Date endTime = new Date();
      MonitorList monitorList = 
        MNodeService.getInstance(request).getOperationStatistics(
            session, 
            startTime, 
            endTime, 
            session.getSubject(), 
            Event.CREATE, 
            null //formatId
            );
      
      assertNotNull(monitorList);
      // TODO: should probably test other parts of the information
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
          InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
          SystemMetadata sysmeta = createSystemMetadata(pid, session.getSubject(), object);
          Identifier retPid = MNodeService.getInstance(request).create(session, pid, object, sysmeta);
          assertEquals(retPid.getValue(), pid.getValue());
          
          // pretend the system metadata changed on the CN
          MNodeService.getInstance(request).systemMetadataChanged(session, 
                  retPid, 5000L, Calendar.getInstance().getTime());
          
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
        event, null, start, count);
      
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
			InputStream bagStream = MNodeService.getInstance(request).getPackage(session, pid);
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
			InputStream bagStream = MNodeService.getInstance(request).getPackage(session, pid);
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

		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected error: " + e.getMessage());
		}
	}
	
  
}
