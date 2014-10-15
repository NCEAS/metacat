/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.IOUtils;
import org.dataone.client.D1Client;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectFormatList;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.Constants;

/**
 * A JUnit test for testing the dataone CNCore implementation
 */
public class CNodeServiceTest extends D1NodeServiceTest {   
    
    /**
    * constructor for the test
    */
    public CNodeServiceTest(String name)
    {
        super(name);
    }

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() 
	{
		TestSuite suite = new TestSuite();
		suite.addTest(new CNodeServiceTest("initialize"));
		
		suite.addTest(new CNodeServiceTest("testChecksum"));
		suite.addTest(new CNodeServiceTest("testCreate"));
		suite.addTest(new CNodeServiceTest("testGet"));
		suite.addTest(new CNodeServiceTest("testGetFormat"));
		suite.addTest(new CNodeServiceTest("testGetLogRecords"));
		suite.addTest(new CNodeServiceTest("testGetSystemMetadata"));
		suite.addTest(new CNodeServiceTest("testIsAuthorized"));
		suite.addTest(new CNodeServiceTest("testListFormats"));
		suite.addTest(new CNodeServiceTest("testListNodes"));
		suite.addTest(new CNodeServiceTest("testObjectFormatNotFoundException"));
		suite.addTest(new CNodeServiceTest("testRegisterSystemMetadata"));
		suite.addTest(new CNodeServiceTest("testReplicationPolicy"));
		suite.addTest(new CNodeServiceTest("testReplicationStatus"));
		suite.addTest(new CNodeServiceTest("testReserveIdentifier"));
		suite.addTest(new CNodeServiceTest("testSearch"));
		suite.addTest(new CNodeServiceTest("testSetAccessPolicy"));
		suite.addTest(new CNodeServiceTest("testSetOwner"));
		suite.addTest(new CNodeServiceTest("readDeletedObject"));
		return suite;
	}
	
	
	/**
	 * test for registering standalone system metadata
	 */
	public Identifier testRegisterSystemMetadata() {
	    printTestHeader("testRegisterSystemMetadata");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testRegisterSystemMetadata." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			Identifier retGuid = CNodeService.getInstance(request).registerSystemMetadata(session, guid, sysmeta);
			assertEquals(guid.getValue(), retGuid.getValue());
			return retGuid;
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
        return null;
	}
	
	/**
	 * test for getting system metadata
	 */
	public void testGetSystemMetadata() {
	    printTestHeader("testGetSystemMetadata");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testGetSystemMetadata." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			Identifier retGuid = CNodeService.getInstance(request).registerSystemMetadata(session, guid, sysmeta);
			assertEquals(guid.getValue(), retGuid.getValue());
			// get it
			SystemMetadata retSysmeta = CNodeService.getInstance(request).getSystemMetadata(session, guid);
			// check it
			assertEquals(sysmeta.getIdentifier().getValue(), retSysmeta.getIdentifier().getValue());
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testGetLogRecords() {
	    printTestHeader("testGetLogRecords");
	    try {

		    Session session = getTestSession();
		    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		    Date fromDate = sdf.parse("2010-01-01");
		    Date toDate = new Date();
		    Event event = Event.CREATE;
		    int start = 0;
		    int count = 1;
	    
		    Log log = CNodeService.getInstance(request).getLogRecords(session, fromDate, toDate, 
		    	event, null, start, count);
		    assertNotNull(log);
		    assertTrue(log.getCount() == count);
		    assertTrue(log.getStart() == start);
		    assertTrue(log.getTotal() > 0);
	    } catch (Exception e) {
		    e.printStackTrace();
		    fail("Unexpected error: " + e.getMessage());
	    } 
	}
	
	public void testCreate() {
	    printTestHeader("testCreate");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testCreate." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			Identifier pid = CNodeService.getInstance(request).create(session, guid, object, sysmeta);
			assertEquals(guid, pid);
        } catch(Exception e) {
        	e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testGet() {
	    printTestHeader("testGet");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testGet." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			Identifier pid = CNodeService.getInstance(request).create(session, guid, object, sysmeta);
			assertEquals(guid.getValue(), pid.getValue());
			// get it
			InputStream retObject = CNodeService.getInstance(request).get(session, pid);
			// check it
			object.reset();
			assertTrue(IOUtils.contentEquals(object, retObject));
        } catch(Exception e) {
        	e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testChecksum() {
	    printTestHeader("testChecksum");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testChecksum." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			// save it
			Identifier retGuid = CNodeService.getInstance(request).registerSystemMetadata(session, guid, sysmeta);
			assertEquals(guid.getValue(), retGuid.getValue());
			// check it
			Checksum checksum = CNodeService.getInstance(request).getChecksum(session, guid);
			assertEquals(sysmeta.getChecksum().getValue(), checksum.getValue());
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testListNodes() {
	    printTestHeader("testListNodes");

	    try {
	    	CNodeService.getInstance(request).listNodes();
        } catch(NotImplemented e) {
        	// expecting not implemented
            assertTrue(true);
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testReserveIdentifier() {
	    printTestHeader("testReserveIdentifier");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testReserveIdentifier." + System.currentTimeMillis());
			// reserve it
			Identifier resultPid = CNodeService.getInstance(request).reserveIdentifier(session, guid);
			assertNotNull(resultPid);
			assertEquals(guid.getValue(), resultPid.getValue());
	    } catch(NotImplemented ni) {
        	// this is not implemented in Metacat
            assertTrue(true);	
        } catch(Exception e) {
        	e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testSearch() {
	    printTestHeader("testSearch");

	    try {
            Session session = getTestSession();
			
			// search for objects, but expect a NotImplemented exception
			try {
				ObjectList objectList = CNodeService.getInstance(request).search(session, null, null);
			} catch (NotImplemented ne) {
				assertTrue(true);
				return;
			}
			fail("Metacat should not implement CN.search");
			
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testSetOwner() {
	    printTestHeader("testSetOwner");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testSetOwner." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			long serialVersion = 1L;
			// save it
			Identifier retGuid = CNodeService.getInstance(request).registerSystemMetadata(session, guid, sysmeta);
			assertEquals(guid.getValue(), retGuid.getValue());
			Subject rightsHolder = new Subject();
			rightsHolder.setValue("newUser");
			// set it
			Identifier retPid = CNodeService.getInstance(request).setRightsHolder(session, guid, rightsHolder, serialVersion);
			assertEquals(guid, retPid);
			// get it
			sysmeta = CNodeService.getInstance(request).getSystemMetadata(session, guid);
			assertNotNull(sysmeta);
			// check it
			assertTrue(rightsHolder.equals(sysmeta.getRightsHolder()));
			
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testSetAccessPolicy() {
	    printTestHeader("testSetAccessPolicy");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testSetAccessPolicy." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
	    long serialVersion = 1L;

			// save it
			Identifier retGuid = CNodeService.getInstance(request).registerSystemMetadata(session, guid, sysmeta);
			assertEquals(guid.getValue(), retGuid.getValue());
			AccessPolicy accessPolicy = new AccessPolicy();
			AccessRule accessRule = new AccessRule();
			accessRule.addPermission(Permission.WRITE);
			Subject publicSubject = new Subject();
			publicSubject.setValue(Constants.SUBJECT_PUBLIC);
			accessRule.addSubject(publicSubject);
			accessPolicy.addAllow(accessRule);
			// set it
			boolean result = CNodeService.getInstance(request).setAccessPolicy(session, guid, accessPolicy, serialVersion );
			assertTrue(result);
			// check it
			result = CNodeService.getInstance(request).isAuthorized(session, guid, Permission.WRITE);
			assertTrue(result);
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testIsAuthorized() {
	    printTestHeader("testIsAuthorized");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testIsAuthorized." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			// save it
			Identifier retGuid = CNodeService.getInstance(request).registerSystemMetadata(session, guid, sysmeta);
			assertEquals(guid.getValue(), retGuid.getValue());
			// check it
			Subject publicSubject = new Subject();
			publicSubject.setValue(Constants.SUBJECT_PUBLIC);
			session.setSubject(publicSubject);
			// public read
			boolean result = CNodeService.getInstance(request).isAuthorized(session, guid, Permission.READ);
			assertTrue(result);
			// not public write
			try {
				result = false;
				result = CNodeService.getInstance(request).isAuthorized(session, guid, Permission.WRITE);
				fail("Public WRITE should be denied");
			} catch (NotAuthorized nae) {
				result = true;
			}
			assertTrue(result);
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testReplicationPolicy() {
	    printTestHeader("testReplicationPolicy");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testReplicationPolicy." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
	    long serialVersion = 1L;

			// save it
			Identifier retGuid = CNodeService.getInstance(request).registerSystemMetadata(session, guid, sysmeta);
			assertEquals(guid.getValue(), retGuid.getValue());
			
			ReplicationPolicy policy = new ReplicationPolicy();
			NodeReference node = new NodeReference();
			node.setValue("testNode");
			policy.addPreferredMemberNode(node );
			// set it
			boolean result = CNodeService.getInstance(request).setReplicationPolicy(session, guid, policy, serialVersion);
			assertTrue(result);
			// get it
			sysmeta = CNodeService.getInstance(request).getSystemMetadata(session, guid);
			assertNotNull(sysmeta);
			// check it
			assertEquals(policy.getPreferredMemberNode(0).getValue(), sysmeta.getReplicationPolicy().getPreferredMemberNode(0).getValue());
			
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testReplicationStatus() {
	    printTestHeader("testReplicationStatus");

	    try {
            Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testReplicationStatus." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			Replica replica = new Replica();
			NodeReference replicaMemberNode = new NodeReference();
			replicaMemberNode.setValue("testNode");
			replica.setReplicationStatus(ReplicationStatus.REQUESTED);
			replica.setReplicaMemberNode(replicaMemberNode);
			replica.setReplicaVerified(Calendar.getInstance().getTime());
			sysmeta.addReplica(replica );
			// save it
			Identifier retGuid = CNodeService.getInstance(request).registerSystemMetadata(session, guid, sysmeta);
			assertEquals(guid.getValue(), retGuid.getValue());
			// set it
			ReplicationStatus status = ReplicationStatus.COMPLETED;
			BaseException failure = new NotAuthorized("000", "Mock exception for " + this.getClass().getName());
			boolean result = CNodeService.getInstance(request).setReplicationStatus(session, guid, replicaMemberNode, status, failure);
			assertTrue(result);
			// get it
			sysmeta = CNodeService.getInstance(request).getSystemMetadata(session, guid);
			assertNotNull(sysmeta);
			// check it
			assertEquals(status, sysmeta.getReplica(0).getReplicationStatus());
			
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() 
	{
	    printTestHeader("initialize");
		assertTrue(1 == 1);
	}
	
	/**
	 * We want to act as the CN itself
	 * @throws ServiceFailure 
	 * @throws Exception 
	 */
	@Override
	public Session getTestSession() throws Exception {
		Session session = super.getTestSession();
		
		// use the first CN we find in the nodelist
        NodeList nodeList = D1Client.getCN().listNodes();
        for (Node node : nodeList.getNodeList()) {
            if ( node.getType().equals(NodeType.CN) ) {
                
                List<Subject> subjects = node.getSubjectList();
                for (Subject subject : subjects) {
                   session.setSubject(subject);
                   // we are done here
                   return session;
                }
            }
        }
        // in case we didn't find it
        return session;
	}
	

	/**
	 * test to list the object formats registered in metacat
	 */
	public void testListFormats() {
		
    printTestHeader("testListFormats");
    
    // make sure we are set up
    setUpFormats();
    
    // there should be at least 59 formats in the list
  	int formatsCount = 59;
  	ObjectFormatList objectFormatList;
  	
  	try {
	    objectFormatList = CNodeService.getInstance(request).listFormats();
	  	assertTrue(objectFormatList.getTotal() >= formatsCount);
  	
  	} catch (ServiceFailure e) {
  		fail("Could not get the object format list: " + e.getMessage());

    } catch (NotImplemented e) {
  		fail("Could not get the object format list: " + e.getMessage());

    }
    
	}
	
  /**
   * Test getting a single object format from the registered list
   */
  public void testGetFormat() {
  	
    printTestHeader("testGetFormat");

    // make sure we are set up
    setUpFormats();
    
    String knownFormat = "text/plain";
    ObjectFormatIdentifier fmtid = new ObjectFormatIdentifier();
    fmtid.setValue(knownFormat);
  	
    try {
	    
			String result = 
				CNodeService.getInstance(request).getFormat(fmtid).getFormatId().getValue();
	  	System.out.println("Expected result: " + knownFormat);
	  	System.out.println("Found    result: " + result);
	  	assertTrue(result.equals(knownFormat));
  
    } catch (NullPointerException npe) {	  
	    fail("The returned format was null: " + npe.getMessage());
    
    } catch (NotFound nfe) {     
    	fail("The format " + knownFormat + " was not found: " + nfe.getMessage());
    	
    } catch (ServiceFailure sfe) {
    	fail("The format " + knownFormat + " was not found: " + sfe.getMessage());

    } catch (NotImplemented nie) {
    	fail("The getFormat() method has not been implemented: " + nie.getMessage());

    }
  	
  }
	
  /**
   * Test getting a non-existent object format, returning NotFound
   */
  public void testObjectFormatNotFoundException() {
  
    printTestHeader("testObjectFormatNotFoundException");

    ObjectFormatIdentifier fmtid = new ObjectFormatIdentifier();
  	String badFormat = "text/bad-format";
  	fmtid.setValue(badFormat);
  	
  	try {
  		
	    ObjectFormat objectFormat = 
	    	CNodeService.getInstance(request).getFormat(fmtid);
      
  	} catch (Exception e) {
	    
  		assertTrue(e instanceof NotFound);
  	}
  	
  }
  
  public void readDeletedObject() {
      printTestHeader("testCreate");

      try {
          Session session = getTestSession();
          Identifier guid = new Identifier();
          guid.setValue("testCreate." + System.currentTimeMillis());
          InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
          SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
          Identifier pid = CNodeService.getInstance(request).create(session, guid, object, sysmeta);
          assertEquals(guid, pid);
          
          Thread.sleep(3000);
          // use MN admin to delete
          session = getMNSession();
          Identifier deletedPid = CNodeService.getInstance(request).delete(session, pid);
          System.out.println("after deleting");
          assertEquals(pid.getValue(), deletedPid.getValue());
          // check that we cannot get the object
          session = getTestSession();
          InputStream deletedObject = null;
          try {
              //System.out.println("before read ===============");
              deletedObject = CNodeService.getInstance(request).get(session, deletedPid);
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              //System.out.println("the exception is1 "+nf.getMessage());
              //nf.printStackTrace();
              assertTrue(nf.getMessage().contains("deleted"));
          }
          try {
              //System.out.println("before read ===============");
              SystemMetadata sysmeta2 = CNodeService.getInstance(request).getSystemMetadata(session, deletedPid);
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              //System.out.println("the exception is "+nf.getMessage());
              //nf.printStackTrace();
              assertTrue(nf.getMessage().contains("deleted"));
          }
          
          try {
              //System.out.println("before read ===============");
              DescribeResponse describeResponse = CNodeService.getInstance(request).describe(session, pid);
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              //System.out.println("the exception is "+nf.getMessage());
              //nf.printStackTrace();
              assertTrue(nf.getMessage().contains("deleted"));
          }
          
          try {
              //System.out.println("before read ===============");
              Checksum checksum = CNodeService.getInstance(request).getChecksum(session, pid);
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              //System.out.println("the exception 3 is "+nf.getMessage());
              //nf.printStackTrace();
              assertTrue(nf.getMessage().contains("deleted"));
          }
          
          try {
              //System.out.println("before read ===============");
              boolean isAuthorized = 
                      CNodeService.getInstance(request).isAuthorized(session, pid, Permission.READ);
              //System.out.println("after read ===============");
          } catch (NotFound nf) {
              //System.out.println("the exception 4 is "+nf.getMessage());
              //nf.printStackTrace();
              assertTrue(nf.getMessage().contains("deleted"));
          }
          
         
          
          assertNull(deletedObject);
      } catch(Exception e) {
          e.printStackTrace();
          fail("Unexpected error: " + e.getMessage());
      }
  }
 
}
