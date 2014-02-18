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
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.Constants;
import org.junit.Before;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.dataone.CNodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.SyncAccessPolicy;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A JUnit test for testing syncing access policies between MN -> CN after local
 * update by metacat services
 */
public class SyncAccessPolicyTest extends D1NodeServiceTest {

//	private static String username;
//	private static String password;
//	private static String anotheruser;
//	private static String anotherPassword;
//
//	static {
//		try {
//
//			username = PropertyService.getProperty("test.mcUser");
//			password = PropertyService.getProperty("test.mcPassword");
//			anotheruser = PropertyService.getProperty("test.mcAnotherUser");
//			anotherpassword = PropertyService
//					.getProperty("test.mcAnotherPassword");
//
//		} catch (PropertyNotFoundException pnfe) {
//			System.err.println("Could not get property in static block: "
//					+ pnfe.getMessage());
//		}
//	}

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

			AccessPolicy accessPolicy = sysmeta.getAccessPolicy();
			AccessRule allow = new AccessRule();
			allow.addPermission(Permission.CHANGE_PERMISSION);
			Subject publicSubject = new Subject();
			publicSubject.setValue(Constants.SUBJECT_PUBLIC);
			allow.addSubject(publicSubject);
			accessPolicy.addAllow(allow);
			sysmeta.setAccessPolicy(accessPolicy);

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

		debug("Logging in with user: " + username);
		String response = null;

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
			
			debug("Logging in with user: " + anotheruser + ", password: " + anotherpassword);
			try {
				response = m.login(anotheruser, anotherpassword);
				debug("Login response: " + response);
			} catch (Exception e) {
				debug("Unable to login: " + response);
				fail();
			}
			
			debug("Updating permissions of localId: " + localId + ", guid: "
					+ pid.getValue());

			// Now update the access policy on the local metacat using the
			// metacat api
			fieldValuePairs = new Hashtable<String, String[]>();
			fieldValuePairs.put("action", new String[] { "setaccess" });
			fieldValuePairs.put("docid", new String[] { localId });
			fieldValuePairs.put("principal", new String[] { username });
			fieldValuePairs.put("permission", new String[] { "read" });
			fieldValuePairs.put("permType", new String[] { "allow" });
			fieldValuePairs.put("permOrder", new String[] { "allowFirst" });
			debug("Updating access perms for docid: " + localId);
			try {
				resultXML = RequestUtil.get(metacatUrl, fieldValuePairs);
			} catch (Exception e) {
				debug("Error setting permissions on docid: " + localId);
				fail();
			}

			if (!resultXML.contains("success")) {
				debug("Unable to change access policy on MN, response: " + resultXML);
				fail();
			}

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
			assert (syncAP.isEqual(mnAccessPolicy, cnAccessPolicy));

			m.logout();

		} catch (Exception e) {
			e.printStackTrace();
			fail("Error running syncAP test: " + e.getMessage());
		}

		debug("Done running testSyncAccessPolicy");
	}
}