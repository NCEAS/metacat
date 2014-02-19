/**
 *  '$RCSfile$'
 *  Copyright: 2014 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author: slaughter $'
 *     '$Date: $'
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

package edu.ucsb.nceas.metacat.admin.upgrade.dataone;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Before;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.dataone.SyncAccessPolicy;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;

/**
 * A JUnit test for testing syncing access policies between MN -> CN after local
 * update by metacat services
 */
public class SyncAccessPolicyTest extends D1NodeServiceTest {

	private Metacat m;

	/**
	 * Constructor to build the test
	 * 
	 * @param name
	 *            the name of the test method
	 */
	public SyncAccessPolicyTest(String name) {
		super(name);
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	@Before
	public void setUp() throws Exception {
		// super.setUp();

		try {
			debug("Test Metacat: " + metacatUrl);
			m = MetacatFactory.createMetacatConnection(metacatUrl);
		} catch (MetacatInaccessibleException mie) {
			System.err.println("Metacat is: " + metacatUrl);
			fail("Metacat connection failed." + mie.getMessage());
		}
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
		suite.addTest(new SyncAccessPolicyTest("initialize"));
		suite.addTest(new SyncAccessPolicyTest("testIsEqual"));
		suite.addTest(new SyncAccessPolicyTest("testSyncAccessPolicy"));

		return suite;
	}

	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() {
		assertTrue(1 == 1);
	}

	/**
	 * constructs a "fake" session with a test subject
	 * @return
	 */
	@Override
	public Session getTestSession() throws Exception {
		Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(anotheruser);
        session.setSubject(subject);
        return session;
	}
	/**
	 * Test object creation
	 */
	public Identifier createTestPid() {
		printTestHeader("testCreate");
		Identifier pid = null;
		try {
			Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testSyncAP." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream(
					"test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid,
					session.getSubject(), object);

			pid = MNodeService.getInstance(request).create(session, guid,
					object, sysmeta);
		} catch (Exception e) {
			e.printStackTrace();
			debug("Error creating pid: " + e.getMessage());
			fail();
		}
		return pid;
	}

	public void testSyncAccessPolicy() {

		AccessPolicy cnAccessPolicy = null;
		AccessPolicy mnAccessPolicy = null;
		SystemMetadata cnSysMeta = null;
		SystemMetadata mnSysMeta = null;
		String resultXML = null;

		String response = null;
		debug("Logging in with user: " + anotheruser + ", password: "
				+ anotherpassword);
		try {
			response = m.login(anotheruser, anotherpassword);
			debug("Login response: " + response);
		} catch (Exception e) {
			debug("Unable to login: " + response);
			fail();
		}

		try {
			debug("\nStarting sync access policy test");

			Identifier pid = null;
			// create the systemMetadata the normal way

			pid = createTestPid();
			assertNotNull(pid);

			debug("Inserted new document: " + pid.getValue());
			try {
				// Get sm, access policy for requested localId
				mnSysMeta = IdentifierManager.getInstance().getSystemMetadata(
						pid.getValue());
			} catch (Exception e) {
				debug("Error getting system metadata for new pid: "
						+ pid.getValue() + ". Message: " + e.getMessage());
				fail();
			}

			CNode cn = null;

			try {
				cn = D1Client.getCN();
			} catch (ServiceFailure sf) {
				debug("Unable to get Coordinating node name for this MN");
				fail();
			}

			boolean found = false;
			int attempts = 0;

			// We have to wait until the CN has harvested the new document,
			// otherwise we
			// will just get an error of "pid not found" when we request the CN
			// to
			// sync the access policies
			// (which is triggered by the Metacat setaccess call).

			debug("Checking for new docid on CN...");
			found = false;
			for (int i = 0; i < 6; i++) {
				attempts = i;
				Thread.sleep(1000 * 60);
				// Get the test document from the CN
				// Get sm, access policy for requested pid from the CN
				try {
					cnSysMeta = cn.getSystemMetadata(pid);
				} catch (Exception e) {
					debug("Error getting system metadata for pid: "
							+ pid.getValue() + " from cn: " + e.getMessage());
					debug("Will request pid from CN again...");
					continue;
				}

				found = true;
				debug("Document " + pid.getValue() + " has been read from CN.");
				break;
			}

			if (!found) {
				fail("Error, cannot read system metadata for pid: " + pid
						+ " after " + attempts + " attempts");
			}

			Hashtable<String, String[]> fieldValuePairs = new Hashtable<String, String[]>();
			fieldValuePairs = new Hashtable<String, String[]>();

			String localId = null;
			try {
				localId = IdentifierManager.getInstance().getLocalId(
						pid.getValue());
			} catch (Exception e) {
				debug("Unable to retrieve localId for pid: " + pid.getValue());
				fail();
			}

			debug("Updating permissions of localId: " + localId + ", guid: "
					+ pid.getValue() + ", username: " + username + " read, allow, allowFirst");

			try {
				response = m.setAccess(localId, username, 
						AccessControlInterface.READSTRING, 
	    				AccessControlInterface.ALLOW, 
	    				AccessControlInterface.ALLOWFIRST);
			} catch (Exception e) {
				debug("Response from setaccess: " + response);
				debug("Error setting access for localId: " + e.getMessage());
				fail();
			}
			
			debug("Response from setaccess: " + response);
			debug("Retrieving updated docid from CN to check if perms were updated...");

			// Get the test document from the CN
			// Get sm, access policy for requested pid from the CN
			try {
				cnSysMeta = cn.getSystemMetadata(pid);
			} catch (Exception e) {
				debug("Error getting system metadata for pid: "
						+ pid.getValue() + " from cn: " + e.getMessage());
				fail();
			}

			debug("Checking privs retrieved from CN");
			// Check if privs for the new user showed up on the CN
			mnAccessPolicy = mnSysMeta.getAccessPolicy();
			debug("Getting access policy for pid: " + pid.getValue());
			cnAccessPolicy = cnSysMeta.getAccessPolicy();
			debug("Diffing access policies (MN,CN) for pid: " + pid.getValue());
			SyncAccessPolicy syncAP = new SyncAccessPolicy();
			debug("Comparing access policies...");
			
			Boolean apEqual = new Boolean (syncAP.isEqual(mnAccessPolicy, cnAccessPolicy));
			debug("Access policies are equal is " + apEqual.toString());
			assert (apEqual == true);

			m.logout();

		} catch (Exception e) {
			e.printStackTrace();
			fail("Error running syncAP test: " + e.getMessage());
		}

		debug("Done running testSyncAccessPolicy");
	}
	
	public void testIsEqual() {
		AccessPolicy ap1 = new AccessPolicy();
		AccessRule ar1 = new AccessRule();
		ar1.addPermission(Permission.READ);
		Subject subject1 = new Subject();
		subject1.setValue(username);
		ar1.addSubject(subject1);
		ap1.addAllow(ar1);
		
		AccessPolicy ap2 = new AccessPolicy();
		AccessRule ar2 = new AccessRule();
		ar2.addPermission(Permission.READ);
		Subject subject2 = new Subject();
		subject2.setValue(username);
		ar2.addSubject(subject2);
		ap2.addAllow(ar2);
		
		boolean isEqual = false;
		SyncAccessPolicy syncAP = new SyncAccessPolicy();
		
		// try something that should be true
		isEqual = syncAP.isEqual(ap1, ap2);
		assertTrue(isEqual);
		
		// try something that makes them not equal
		Subject subject3 = new Subject();
		subject3.setValue(anotheruser);
		ar2.addSubject(subject3);
		
		isEqual = syncAP.isEqual(ap1, ap2);
		assertFalse(isEqual);
		
		isEqual = syncAP.isEqual(ap1, null);
		assertFalse(isEqual);
	}
}