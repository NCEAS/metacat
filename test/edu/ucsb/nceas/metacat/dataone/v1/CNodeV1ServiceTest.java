package edu.ucsb.nceas.metacat.dataone.v1;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.itk.D1Client;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidSystemMetadata;
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
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatList;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.Constants;

import edu.ucsb.nceas.metacat.dataone.v1.CNodeService;

/**
 * A JUnit test for testing the dataone CNCore implementation
 */
public class CNodeV1ServiceTest extends D1NodeV1ServiceTest {   
    
    /**
    * constructor for the test
    */
    public CNodeV1ServiceTest(String name)
    {
        super(name);
    }

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() 
	{
		TestSuite suite = new TestSuite();
		suite.addTest(new CNodeV1ServiceTest("initialize"));
		
		/*suite.addTest(new CNodeV1ServiceTest("testChecksum"));
		suite.addTest(new CNodeV1ServiceTest("testCreate"));
		suite.addTest(new CNodeV1ServiceTest("testGet"));
		suite.addTest(new CNodeV1ServiceTest("testGetFormat"));
		suite.addTest(new CNodeV1ServiceTest("testGetLogRecords"));*/
		suite.addTest(new CNodeV1ServiceTest("testGetSystemMetadata"));
		suite.addTest(new CNodeV1ServiceTest("testGetFormat"));
		/*suite.addTest(new CNodeV1ServiceTest("testIsAuthorized"));
		suite.addTest(new CNodeV1ServiceTest("testListFormats"));
		suite.addTest(new CNodeV1ServiceTest("testListNodes"));
		suite.addTest(new CNodeV1ServiceTest("testObjectFormatNotFoundException"));
		suite.addTest(new CNodeV1ServiceTest("testRegisterSystemMetadata"));
		suite.addTest(new CNodeV1ServiceTest("testReplicationPolicy"));
		suite.addTest(new CNodeV1ServiceTest("testReplicationStatus"));
		suite.addTest(new CNodeV1ServiceTest("testReserveIdentifier"));
		suite.addTest(new CNodeV1ServiceTest("testSearch"));
		suite.addTest(new CNodeV1ServiceTest("testSetAccessPolicy"));
		suite.addTest(new CNodeV1ServiceTest("testSetOwner"));
		suite.addTest(new CNodeV1ServiceTest("readDeletedObject"));
		suite.addTest(new CNodeV1ServiceTest("testGetSID"));*/
		return suite;
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
			SystemMetadata sysmeta = createV1SystemMetadata(guid, session.getSubject(), object);
			Session cnSession = getCNSession();
			Identifier retGuid = CNodeService.getInstance(request).create(cnSession, guid, object, sysmeta);
			assertEquals(guid.getValue(), retGuid.getValue());
			// get it
			SystemMetadata retSysmeta = CNodeService.getInstance(request).getSystemMetadata(session, guid);
			// check it
			assertEquals(sysmeta.getIdentifier().getValue(), retSysmeta.getIdentifier().getValue());
			assertTrue( retSysmeta instanceof org.dataone.service.types.v1.SystemMetadata);
			assertFalse( retSysmeta instanceof org.dataone.service.types.v2.SystemMetadata);
			System.out.println("=== the class name is "+retSysmeta.getClass().getName());
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
	}
	
	public void testGetFormat() throws Exception {
	 // make sure we are set up
	    setUpFormats();
	    
	    String knownFormat = "text/plain";
	    ObjectFormatIdentifier fmtid = new ObjectFormatIdentifier();
	    fmtid.setValue(knownFormat);
	    try {
	        
	        ObjectFormat format = CNodeService.getInstance(request).getFormat(fmtid);
	        assertTrue( format instanceof org.dataone.service.types.v1.ObjectFormat);
	        assertFalse( format instanceof org.dataone.service.types.v2.ObjectFormat);
	        System.out.println("=== the class name is "+format.getClass().getName());
	        String result = format.getFormatId().getValue();
	        System.out.println("Expected result: " + knownFormat);
	        System.out.println("Found    result: " + result);
	        assertTrue(result.equals(knownFormat));
	  
	    } catch (Exception npe) {      
	        fail("Can't get the returned format : " + npe.getMessage());
	    
	    }
	}
  
  
 
}
