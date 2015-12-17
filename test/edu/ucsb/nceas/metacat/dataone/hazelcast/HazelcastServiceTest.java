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

package edu.ucsb.nceas.metacat.dataone.hazelcast;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.CNodeServiceTest;
import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * A JUnit superclass for testing the Hazelcast interactions
 */
public class HazelcastServiceTest extends MCTestCase {   
    
	private static HazelcastInstance hzMember;
	static {
	
		try {
			// initialize the configuration
			hzMember = HazelcastService.getInstance().getHazelcastInstance();
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
			
	}

	private HttpServletRequest request;

    /**
    * constructor for the test
    */
    public HazelcastServiceTest(String name) {
        super(name);

        // set up the fake request (for logging)
        request = new MockHttpServletRequest(null, null, null);
    }
    
    /**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() 
	{
		TestSuite suite = new TestSuite();
		suite.addTest(new HazelcastServiceTest("initialize"));
		suite.addTest(new HazelcastServiceTest("retrieveSystemMetadataFromMap"));
		suite.addTest(new HazelcastServiceTest("storeSystemMetadataToMap"));

		return suite;
	}
  
	
	public void retrieveSystemMetadataFromMap() {
		try {
			Identifier pid = null;
			// create the systemMetadata the normal way
			CNodeServiceTest cnst = new CNodeServiceTest("testRegisterSystemMetadata");
			cnst.setUp();
			pid = cnst.testRegisterSystemMetadata();
			assertNotNull(pid);
			// look it up from the "shared" map
			IMap<Object, Object> systemMetadataMap = hzMember.getMap(PropertyService.getProperty("dataone.hazelcast.storageCluster.systemMetadataMap"));
			SystemMetadata sm = (SystemMetadata) systemMetadataMap.get(pid);
			assertNotNull(sm);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void storeSystemMetadataToMap() {
		try {
			// create the systemMetadata and save to map
			CNodeServiceTest cnst = new CNodeServiceTest("testGetSystemMetadata");
			cnst.setUp();
			Session session = cnst.getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testCreate." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			SystemMetadata sysmeta = cnst.createSystemMetadata(guid, session.getSubject(), object);
			assertNotNull(sysmeta);
			// put it in the "shared" map
			System.out.println("Saving System Metadata to in-memory shared map: " + guid.getValue());
			IMap<Identifier, SystemMetadata> systemMetadataMap = hzMember.getMap(PropertyService.getProperty("dataone.hazelcast.storageCluster.systemMetadataMap"));
			systemMetadataMap.put(guid, sysmeta);
			
			// get it from the store
			SystemMetadata sysmetaFromStore = CNodeService.getInstance(request).getSystemMetadata(session, guid);
			assertNotNull(sysmetaFromStore);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
    /**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() {}
	
	
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
	 * print a header to start each test
	 */
	protected void printTestHeader(String testName)
	{
	    System.out.println();
	    System.out.println("*************** " + testName + " ***************");
	}
 
}
