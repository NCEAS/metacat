package edu.ucsb.nceas.metacat.admin.upgrade.dataone;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.SyncAccessPolicy;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;

/**
 * A JUnit test for testing syncing access policies between MN -> CN after local
 * update by metacat services
 */
public class SyncAccessPolicyTest extends D1NodeServiceTest {

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
		metacatConnectionNeeded = true;
		super.setUp();

	}

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() {
		super.tearDown();
	}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(new SyncAccessPolicyTest("initialize"));
		suite.addTest(new SyncAccessPolicyTest("testIsEqual"));
		//suite.addTest(new SyncAccessPolicyTest("testSyncAccessPolicy"));
		//suite.addTest(new SyncAccessPolicyTest("testSyncEML201OnlineDataAccessPolicy"));

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
	 * 
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

			String nodeId =  Settings.getConfiguration().getString("dataone.nodeId");
            //System.out.println("the node id from the configuration file (metacat.properties) is =============================="+nodeId);
            if(nodeId != null && !nodeId.trim().equals("")) {
                //System.out.println("set the node id to be ============================== "+nodeId+" which comes from the configuration file");
                NodeReference nr = new NodeReference();
                nr.setValue(nodeId);
                sysmeta.setOriginMemberNode(nr);
                sysmeta.setAuthoritativeMemberNode(nr);
            }
			pid = MNodeService.getInstance(request).create(session, guid,
					object, sysmeta);
		} catch (Exception e) {
			e.printStackTrace();
			debug("Error creating pid: " + e.getMessage());
			fail();
		}
		return pid;
	}

	/**
	 * This test checks that access policy of a pid is synced with the CN after
	 * the docid is manually changed using the Metacat api (setaccess).
	 */
	public void testSyncAccessPolicy() {

		AccessPolicy cnAccessPolicy = null;
		AccessPolicy mnAccessPolicy = null;
		SystemMetadata cnSysMeta = null;
		SystemMetadata mnSysMeta = null;
		CNode cn = null;

		String response = null;
		debug("\nStarting sync access policy test");

		String localId = null;
		try {
			Identifier pid = null;
			pid = createTestPid();
			assertNotNull(pid);

			debug("Inserted new document: " + pid.getValue());
			boolean found = false;
			int attempts = 0;

			/*
			 * Determine the CN for the current host. This test must be run on a
			 * registered MN.
			 */
			try {
				cn = D1Client.getCN();
			} catch (ServiceFailure sf) {
				debug("Unable to get Coordinating node name for this MN");
				fail();
			}

			// We have to wait until the CN has harvested the new document,
			// otherwise we
			// will just get an error of "pid not found" when we request the CN
			// to
			// sync the access policies
			// (which is triggered by the Metacat setaccess call).

			debug("Checking for new pid on CN...");
			found = false;
			for (int i = 0; i < 6; i++) {
				attempts = i;
				Thread.sleep(1000 * 60);
				// Get the test document from the CN
				// Get sm, access policy for requested pid from the CN
				// Get sm, access policy for requested pid from the CN
				try {
					cnSysMeta = cn.getSystemMetadata(null, pid);
					debug("Got SM from CN");

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
			
			/* Read SM from MN */
			try {
				mnSysMeta = MNodeService.getInstance(request)
						.getSystemMetadata(null, pid);
				debug("Got SM from MN");
			} catch (Exception e) {
				fail("Error getting system metadata for new pid: "
						+ pid.getValue() + ". Message: " + e.getMessage());
			}
			
			SyncAccessPolicy syncAP = new SyncAccessPolicy();

			// access policies should match after initial harvest
			assertTrue(syncAP.isEqual(cnSysMeta.getAccessPolicy(), mnSysMeta.getAccessPolicy()));

			// find the local id for this pid
			try {
				localId = IdentifierManager.getInstance().getLocalId(
						pid.getValue());
			} catch (Exception e) {
				fail("Unable to retrieve localId for pid: " + pid.getValue());
			}

			debug("Updating permissions of localId: " + localId + ", guid: "
					+ pid.getValue() + ", username: " + username
					+ " read, allow, allowFirst");

			response = m.login(anotheruser, anotherpassword);
			debug("Logging in as user: " + anotheruser);
			debug("Adding access for user: " + username);

			/* Update the docid access policy with Metacat api */
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

			/* Reread SM from MN after updating access */
			try {
				mnSysMeta = MNodeService.getInstance(request)
						.getSystemMetadata(null, pid);
				debug("Got SM from MN");
			} catch (Exception e) {
				fail("Error getting system metadata for new pid: "
						+ pid.getValue() + ". Message: " + e.getMessage());
			}

			/* Check if the access policy was updated on the MN */
			mnAccessPolicy = mnSysMeta.getAccessPolicy();
			found = false;
			List<Subject> subjectList = null;
			List<AccessRule> accessRules = mnAccessPolicy.getAllowList();
			debug("Checking that access policy was added to MN");

			try {
				debug("Checking " + accessRules.size() + " access rules");
				for (AccessRule ar : accessRules) {
					subjectList = ar.getSubjectList();
					debug("Checking " + ar.sizeSubjectList()
							+ " subjects for this access rule");
					for (Subject sj : subjectList) {
						debug("Checking subject: " + sj.getValue());
						if (sj.getValue().equals(username)) {
							debug("user " + username + " found");
							found = true;
						}
						debug("MN done with subject: " + sj.getValue());
					}
				}
			} catch (Exception e) {
				debug("Error checking access policy: " + e.getMessage());
				fail();
			}

			if (!found)
				debug("user " + username + " not found in access policy");

			// assertTrue(found);

			/* Reread SM from CN */
			try {
				cnSysMeta = cn.getSystemMetadata(null, pid);
				debug("Got SM from CN");
			} catch (Exception e) {
				fail("Error getting system metadata for pid: " + pid.getValue()
						+ " from cn: " + e.getMessage());
			}

			debug("Done getting CN SM");

			/* Check if the access policy was updated on the MN */
			cnAccessPolicy = cnSysMeta.getAccessPolicy();
			found = false;
			subjectList = null;
			accessRules = cnAccessPolicy.getAllowList();
			debug("Checking that access policy was added to CN");
			debug("Checking " + accessRules.size() + " access rules");

			for (AccessRule ar : accessRules) {
				subjectList = ar.getSubjectList();
				debug("Checking " + ar.sizeSubjectList()
						+ " subjects for this access rule");
				for (Subject sj : subjectList) {
					debug("Checking subject: " + sj.getValue());
					if (sj.getValue().indexOf(username) >= 0) {
						debug("user " + username + " found");
						found = true;
					}
				}
			}
			if (!found)
				debug("user " + username + " not found in access policy");
			debug("Done checking access rules for CN");

			assertTrue(found);
			debug("Checking privs retrieved from CN");
			debug("Getting access policy for pid: " + pid.getValue());
			cnAccessPolicy = cnSysMeta.getAccessPolicy();
			debug("Diffing access policies (MN,CN) for pid: " + pid.getValue());
			debug("Comparing access policies...");

			Boolean apEqual = new Boolean(syncAP.isEqual(mnAccessPolicy,
					cnAccessPolicy));
			debug("Are access policies equal?: " + apEqual.toString());
			assert (apEqual == true);
		} catch (Exception e) {
			e.printStackTrace();
			debug("Error running syncAP test: " + e.getMessage());
			fail();
		} finally {
			if (localId != null)
				deleteDocumentId(localId, SUCCESS, true);
			try {
				m.logout();
			} catch (Exception e) {
				debug("Error logging out");
			}
		}

		debug("Done running testSyncAccessPolicy");
	}

	/*
	 * This test checks that the access policy of an online data object
	 * (described in EML 2.0.1 <online> section) is properly synced with the CN
	 * when the access policy is changed by modifying the access policy in the
	 * <additionalMetadata> section of the parent document.
	 */
	public void testSyncEML201OnlineDataAccessPolicy() {
		String newdocid = null;
		String onlineDocid = null;
		String onlinetestdatafile2 = "test/onlineDataFile2";
		SystemMetadata mnSysMeta = null;
		SystemMetadata cnSysMeta = null;
		CNode cn = null;

		try {
			debug("\nRunning: testSyncEML201OnlineDataAccessPolicy");
			String emlVersion = EML2_0_1;
			debug("logging in as: username=" + username + " password="
					+ password);
			m.login(username, password);

			/* Create a data object */
			onlineDocid = generateDocumentId();
			uploadDocumentId(onlineDocid + ".1", onlinetestdatafile2, SUCCESS,
					false);
			debug("Inserted new data object with localid: " + onlineDocid
					+ ".1");
			String accessRule1 = generateOneAccessRule("public", true, true,
					false, false, false);

			Vector<String> accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			String accessDoc = getAccessBlock(accessRules, ALLOWFIRST);
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid
							+ ".1", null, accessDoc, null, null, null, null);

			/* Insert the eml document that describes the data object */
			newdocid = generateDocumentId();
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);

			debug("Inserted document with localId: " + newdocid + ".1");
			/*
			 * Determine the D1 pid for the data object so that we can query the
			 * CN for it.
			 */
			String docidWithoutRev = DocumentUtil.getSmartDocId(onlineDocid
					+ ".1");
			int rev = IdentifierManager.getInstance().getLatestRevForLocalId(
					onlineDocid + ".1");
			String guid = IdentifierManager.getInstance().getGUID(
					docidWithoutRev, rev);

			Identifier pid = new Identifier();
			pid.setValue(guid);
			try {
				cn = D1Client.getCN();
			} catch (ServiceFailure sf) {
				debug("Unable to get Coordinating node name for this MN");
				fail();
			}

			/*
			 * Wait for the cn to harvest metadata for the data object. We are
			 * testing that an update of access rules in the parent eml document
			 * for the enclosed online data object will trigger metacat (via
			 * SyncAccessPolicy) to update the access policy on the CN, so the
			 * data object has to be present on the CN before we continue.
			 */
			debug("Checking for new data object pid " + pid.getValue()
					+ " on CN...");
			boolean found = false;
			int attempts = 0;
			for (int i = 0; i < 6; i++) {
				attempts = i;
				Thread.sleep(1000 * 60);
				// Get sm for data object pid from the CN
				try {
					cnSysMeta = cn.getSystemMetadata(null, pid);
				} catch (Exception e) {
					debug("Error getting system metadata for pid: "
							+ pid.getValue() + " from cn: " + e.getMessage());
					debug("Will request pid from CN again...");
					continue;
				}

				found = true;
				debug("Data object pid " + pid.getValue()
						+ " has been read from CN.");
				break;
			}

			if (!found) {
				fail("Error, cannot read system metadata for pid: " + pid
						+ " after " + attempts + " attempts");
			}

			// update the document access control for online data
			accessRule1 = generateOneAccessRule("public", true, true, false,
					false, false);
			String accessRule2 = generateOneAccessRule(anotheruser, true, true,
					false, false, false);

			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			String accessData = getAccessBlock(accessRules, ALLOWFIRST);
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid
							+ ".1", null, accessDoc, null, null, accessData,
					null);

			/*
			 * Updating the parent document should trigger metacat to update the
			 * access policy on the CN (via EML 201 parser and
			 * SyncAccessPolicy).
			 */
			updateDocumentId(newdocid + ".2", testdocument, SUCCESS, false);

			/* Get MN access policy for data object */
			try {
				// Get sm, access policy for requested localId
				mnSysMeta = IdentifierManager.getInstance().getSystemMetadata(
						pid.getValue());
			} catch (Exception e) {
				debug("Error getting system metadata for new pid: "
						+ pid.getValue() + ". Message: " + e.getMessage());
				fail();
			}
			/* Get CN access policy for data object */
			try {
				cnSysMeta = cn.getSystemMetadata(null, pid);
			} catch (Exception e) {
				debug("Error getting system metadata for pid: "
						+ pid.getValue() + " from cn: " + e.getMessage());
				fail();
			}

			/* Compare MN and CN access policies */
			debug("Getting CN,MN access policy for pid: " + pid.getValue());
			AccessPolicy mnAccessPolicy = mnSysMeta.getAccessPolicy();
			AccessPolicy cnAccessPolicy = cnSysMeta.getAccessPolicy();
			debug("Diffing access policies (MN,CN) for pid: " + pid.getValue());

			SyncAccessPolicy syncAP = new SyncAccessPolicy();
			debug("Comparing access policies...");

			Boolean apEqual = new Boolean(syncAP.isEqual(mnAccessPolicy,
					cnAccessPolicy));

			debug("Are access policies equal?: " + apEqual.toString());
			assert (apEqual == true);

		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		} finally {
			/* Delete the document */
			deleteDocumentId(newdocid + ".2", SUCCESS, true);
			deleteDocumentId(onlineDocid + ".1", SUCCESS, true);

			// logout
			debug("logging out");
			try {
				m.logout();
			} catch (Exception e) {
				debug("Error logging out");
			}
		}

		debug("Done running testSyncEML201OnlineDataAccessPolicy");
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
