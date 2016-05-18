/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
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

package edu.ucsb.nceas.metacattest;

import java.util.Vector;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import junit.framework.Test;
import junit.framework.TestSuite;
// import java.io.File;

/**
 * A JUnit test for testing Access Control in Metacat
 */
public class AccessControlTest extends MCTestCase {

	private String newdocid = null;
	private String onlineDocid;
	private String onlinetestdatafile1 = "test/onlineDataFile1";

	/**
	 * Constructor to build the test
	 * 
	 * @param name
	 *            the name of the test method
	 */
	public AccessControlTest(String name) {
		super(name);
		newdocid = generateDocumentId();
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() throws Exception {
		metacatConnectionNeeded = true;
		super.setUp();
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
		suite.addTest(new AccessControlTest("initialize"));
		// Test basic functions
		suite.addTest(new AccessControlTest("document201Test"));
		suite.addTest(new AccessControlTest("document210Test"));
		suite.addTest(new AccessControlTest("AccessControlTest201ForPublic"));
		suite.addTest(new AccessControlTest("AccessControlTest210ForPublic"));
		suite.addTest(new AccessControlTest("test201AllowFirst"));
		suite.addTest(new AccessControlTest("test201DenyFirst"));
		suite.addTest(new AccessControlTest("test210AllowFirst"));
		suite.addTest(new AccessControlTest("test210DenyFirst"));

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
	 * Tests a version 2.0.1 EML document when permission order is allowFirst,
	 * the combination of allow and deny rules affect user to read, update and
	 * delete a document. Here are test cases 1.An user inserts a document with
	 * access rules (allowFirst) - allow READ rule for another user, deny READ
	 * rule for public. Another user reads this document - failure Another user
	 * updates this document(except access part) -failure Another user updates
	 * this document(access part) -failure Another user deletes this document -
	 * failure 2. The user updates this documents with access rules (allowFirst) -
	 * allow READ and WRITE rule for another user, deny READ and WRITE rule for
	 * public. Another user reads this document - failure Another user updates
	 * this document(except access part) -failure Another user updates this
	 * document(access part) -failure Another user deletes this document -
	 * failure 3. The user updates this documents with access rules (allowFirst) -
	 * allow ALL rule for another user, deny ALL rule for public. Another user
	 * reads this document - failure Another user updates this document(except
	 * access part) -failure Another user updates this document(access part)
	 * -failure Another user deletes this document - failure 4. The user updates
	 * this documents with access rules (allowFirst) - allow READ and WRITE rule
	 * for another user, deny WRITE rule for public. Another user reads this
	 * document - success Another user updates this document(except access part)
	 * -failure Another user updates this document(access part) -failure Another
	 * user deletes this document - failure 5. The user updates this documents
	 * with access rules (allowFirst) - allow READ and WRITE rule for another
	 * user, deny READ rule for public. Another user reads this document -
	 * failure Another user updates this document(except access part) - success
	 * Another user updates this document(access part) -failure Another user
	 * deletes this document - failure 6. The user updates this documents with
	 * access rules (allowFirst) - allow READ rule for another user, deny READ
	 * rule for a group (which another user is in the group) Another user reads
	 * this document - failure Another user updates this document(except access
	 * part) -failure Another user updates this document(access part) -failure
	 * Another user deletes this document - failure 7. The user updates this
	 * documents with access rules (allowFirst) - allow READ and WRITE rule for
	 * another user, deny READ and WRITE rule for a group (which another user is
	 * in the group) Another user reads this document - failure Another user
	 * updates this document(except access part) -failure Another user updates
	 * this document(access part) -failure Another user deletes this document -
	 * failure 8. The user updates this documents with access rules (allowFirst) -
	 * allow ALL rule for another user, deny ALL rule for a group (which another
	 * user is in the group) Another user reads this document - failure Another
	 * user updates this document(except access part) -failure Another user
	 * updates this document(access part) -failure Another user deletes this
	 * document - failure 9. The user updates this documents with access rules
	 * (allowFirst) - allow READ and WRITE rule for another user, deny WRITE
	 * rule for a group (which another user is in the group) Another user reads
	 * this document - success Another user updates this document(except access
	 * part) -failure Another user updates this document(access part) -failure
	 * Another user deletes this document - failure 10. The user updates this
	 * documents with access rules (allowFirst) - allow READ and WRITE rule for
	 * another user, deny READ rule for a group (which another user is in the
	 * group) Another user reads this document - failure Another user updates
	 * this document(except access part) - success Another user updates this
	 * document(access part) -failure Another user deletes this document -
	 * failure
	 */
	public void test201AllowFirst() {
		try {
			debug("\nRunning: test201AllowFirst test");
			String emlVersion = EML2_0_1;
			newdocid = generateDocumentId();
			// ====1 inserts a document with access rules (allowFirst) - allow
			// READ rule for another user,
			// deny READ rule for public.
			String accessRule1 = generateOneAccessRule(anotheruser, true, true, false,
					false, false);
			String accessRule2 = generateOneAccessRule("public", false, true, false,
					false, false);
			Vector<String> accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			String access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 1: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".1", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====2 inserts a document with access rules (allowFirst) - allow
			// READ and WRITE rule for another user,
			// deny READ and WRITE rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, true, true, false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 2: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".2", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".2", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====3 inserts a document with access rules (allowFirst) - allow
			// ALL rule for another user,
			// deny ALL rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, true, true);
			accessRule2 = generateOneAccessRule("public", false, true, true, true, true);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 3; the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".3", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".3", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".3", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====4 The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny WRITE rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, false, true, false,
					false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 4: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".4", testdocument, SUCCESS, false);
			m.logout();
			debug("logging out");
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".4", testdocument, SUCCESS, false);
			// fails to update this document
			updateDocumentId(newdocid + ".5", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".5", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".4", FAILURE, true);
			// logout
			m.logout();

			// ====5. The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny READ rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, true, false, false,
					false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 5: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".5", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".5", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".6", testdocument, SUCCESS, false);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".7", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".6", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====6 inserts a document with access rules (allowFirst) - allow
			// READ rule for another user,
			// deny READ rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and another user is
			// in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, false, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, false,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 6: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".7", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".7", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".8", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".8", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".7", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====7 inserts a document with access rules (allowFirst) - allow
			// READ and WRITE rule for another
			// user, deny READ and WRITE rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and
			// the other user is in this group)
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, true,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 7: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".8", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".8", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".9", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".9", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".8", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====8 inserts a document with access rules (allowFirst) - allow
			// ALL rule for another user,
			// deny ALL rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and the other user
			// is in this group)
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, true, true);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, true,
					true, true);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 8: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".9", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".9", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".10", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".10", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".9", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====9 The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny WRITE rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org
			// and another user is in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, false, true,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 9: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".10", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// succeed to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".10", testdocument, SUCCESS, false);
			// fails to update this document
			updateDocumentId(newdocid + ".11", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".11", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".10", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====10. The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny READ rule for a
			// group(cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and
			// another user is in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, false,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 10: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".11", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".11", testdocument, FAILURE, true);
			// succeed to update this document
			updateDocumentId(newdocid + ".12", testdocument, SUCCESS, false);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".13", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".12", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}
	}

	/**
	 * Tests a version 2.1.0 EML document when permission order is allowFirst,
	 * the combination of allow and deny rules affect user to read, update and
	 * delete a document. Here are test cases 1.An user inserts a document with
	 * access rules (allowFirst) - allow READ rule for another user, deny READ
	 * rule for public. Another user reads this document - failure Another user
	 * updates this document(except access part) -failure Another user updates
	 * this document(access part) -failure Another user deletes this document -
	 * failure 2. The user updates this documents with access rules (allowFirst) -
	 * allow READ and WRITE rule for another user, deny READ and WRITE rule for
	 * public. Another user reads this document - failure Another user updates
	 * this document(except access part) -failure Another user updates this
	 * document(access part) -failure Another user deletes this document -
	 * failure 3. The user updates this documents with access rules (allowFirst) -
	 * allow ALL rule for another user, deny ALL rule for public. Another user
	 * reads this document - failure Another user updates this document(except
	 * access part) -failure Another user updates this document(access part)
	 * -failure Another user deletes this document - failure 4. The user updates
	 * this documents with access rules (allowFirst) - allow READ and WRITE rule
	 * for another user, deny WRITE rule for public. Another user reads this
	 * document - success Another user updates this document(except access part)
	 * -failure Another user updates this document(access part) -failure Another
	 * user deletes this document - failure 5. The user updates this documents
	 * with access rules (allowFirst) - allow READ and WRITE rule for another
	 * user, deny READ rule for public. Another user reads this document -
	 * failure Another user updates this document(except access part) - success
	 * Another user updates this document(access part) -failure Another user
	 * deletes this document - failure 6. The user updates this documents with
	 * access rules (allowFirst) - allow READ rule for another user, deny READ
	 * rule for a group (which another user is in the group) Another user reads
	 * this document - failure Another user updates this document(except access
	 * part) -failure Another user updates this document(access part) -failure
	 * Another user deletes this document - failure 7. The user updates this
	 * documents with access rules (allowFirst) - allow READ and WRITE rule for
	 * another user, deny READ and WRITE rule for a group (which another user is
	 * in the group) Another user reads this document - failure Another user
	 * updates this document(except access part) -failure Another user updates
	 * this document(access part) -failure Another user deletes this document -
	 * failure 8. The user updates this documents with access rules (allowFirst) -
	 * allow ALL rule for another user, deny ALL rule for a group (which another
	 * user is in the group) Another user reads this document - failure Another
	 * user updates this document(except access part) -failure Another user
	 * updates this document(access part) -failure Another user deletes this
	 * document - failure 9. The user updates this documents with access rules
	 * (allowFirst) - allow READ and WRITE rule for another user, deny WRITE
	 * rule for a group (which another user is in the group) Another user reads
	 * this document - success Another user updates this document(except access
	 * part) -failure Another user updates this document(access part) -failure
	 * Another user deletes this document - failure 10. The user updates this
	 * documents with access rules (allowFirst) - allow READ and WRITE rule for
	 * another user, deny READ rule for a group (which another user is in the
	 * group) Another user reads this document - failure Another user updates
	 * this document(except access part) - success Another user updates this
	 * document(access part) -failure Another user deletes this document -
	 * failure
	 */
	public void test210AllowFirst() {
		try {
			debug("\nRunning: test210AllowFirst test");
			String emlVersion = EML2_1_0;
			newdocid = generateDocumentId();
			// ====1 inserts a document with access rules (allowFirst) - allow
			// READ rule for another user,
			// deny READ rule for public.
			String accessRule1 = generateOneAccessRule(anotheruser, true, true, false,
					false, false);
			String accessRule2 = generateOneAccessRule("public", false, true, false,
					false, false);
			Vector<String> accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			String access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 1: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".1", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====2 inserts a document with access rules (allowFirst) - allow
			// READ and WRITE rule for another user,
			// deny READ and WRITE rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, true, true, false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 2: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".2", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".2", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====3 inserts a document with access rules (allowFirst) - allow
			// ALL rule for another user,
			// deny ALL rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, true, true);
			accessRule2 = generateOneAccessRule("public", false, true, true, true, true);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 3; the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".3", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".3", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".3", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====4 The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny WRITE rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, false, true, false,
					false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 4: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".4", testdocument, SUCCESS, false);
			m.logout();
			debug("logging out");
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".4", testdocument, SUCCESS, false);
			// fails to update this document
			updateDocumentId(newdocid + ".5", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".5", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".4", FAILURE, true);
			// logout
			m.logout();

			// ====5. The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny READ rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, true, false, false,
					false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 5: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".5", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".5", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".6", testdocument, SUCCESS, false);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".7", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".6", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====6 inserts a document with access rules (allowFirst) - allow
			// READ rule for another user,
			// deny READ rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and another user is
			// in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, false, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, false,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 6: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".7", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".7", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".8", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".8", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".7", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====7 inserts a document with access rules (allowFirst) - allow
			// READ and WRITE rule for another
			// user, deny READ and WRITE rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and
			// the other user is in this group)
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, true,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 7: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".8", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".8", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".9", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".9", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".8", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====8 inserts a document with access rules (allowFirst) - allow
			// ALL rule for another user,
			// deny ALL rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and the other user
			// is in this group)
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, true, true);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, true,
					true, true);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 8: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".9", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".9", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".10", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".10", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".9", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====9 The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny WRITE rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org
			// and another user is in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, false, true,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 9: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".10", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// succeed to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".10", testdocument, SUCCESS, false);
			// fails to update this document
			updateDocumentId(newdocid + ".11", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".11", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".10", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====10. The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny READ rule for a
			// group(cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and
			// another user is in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, false,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, ALLOWFIRST);
			debug("Test 10: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".11", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".11", testdocument, FAILURE, true);
			// succeed to update this document
			updateDocumentId(newdocid + ".12", testdocument, SUCCESS, false);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".13", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".12", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}
	}

	/**
	 * NOTE: as of Metacat 2.0.0, denyFirst permOrder is not supported.
	 * Access rules with denyFirst are ignored and only the document owner 
	 * has access to the object (default).
	 * 
	 * Tests Tests a version 2.0.1 EML document when permission order is
	 * denyFirst, the combination of allow and deny rules affect user to read,
	 * update and delete a document. Here are test cases 1.An user inserts a
	 * document with access rules (denyFirst) - allow READ rule for another
	 * user, deny READ rule for public. Another user reads this document -
	 * success Another user updates this document(except access part) -failure
	 * Another user updates this document(access part) -failure Another user
	 * deletes this document - failure 2. The user updates this documents with
	 * access rules (denyFirst) - allow READ and WRITE rule for another user,
	 * deny READ and WRITE rule for public. Another user reads this document -
	 * success Another user updates this document(except access part) -success
	 * Another user updates this document(access part) -failure Another user
	 * deletes this document - failure 3. The user updates this documents with
	 * access rules (denyFirst) - allow ALL rule for another user, deny ALL rule
	 * for public. Another user reads this document - success Another user
	 * updates this document(except access part) -success Another user updates
	 * this document(access part) -success Another user deletes this document -
	 * success 4. The user updates this documents with access rules (denyFirst) -
	 * allow READ and WRITE rule for another user, deny WRITE rule for public.
	 * Another user reads this document - success Another user updates this
	 * document(except access part) -success Another user updates this
	 * document(access part) -failure Another user deletes this document -
	 * failure 5. The user updates this documents with access rules (denyFirst) -
	 * allow READ and WRITE rule for another user, deny READ rule for public.
	 * Another user reads this document - success Another user updates this
	 * document(except access part) - success Another user updates this
	 * document(access part) -failure Another user deletes this document -
	 * failure 6. The user updates this documents with access rules (denyFirst) -
	 * allow READ rule for another user, deny READ rule for a group (which
	 * another user is in the group) Another user reads this document - success
	 * Another user updates this document(except access part) -failure Another
	 * user updates this document(access part) -failure Another user deletes
	 * this document - failure 7. The user updates this documents with access
	 * rules (denyFirst) - allow READ and WRITE rule for another user, deny READ
	 * and WRITE rule for a group (which another user is in the group) Another
	 * user reads this document - success Another user updates this
	 * document(except access part) - success Another user updates this
	 * document(access part) -failure Another user deletes this document -
	 * failure 8. The user updates this documents with access rules (denyFirst) -
	 * allow ALL rule for another user, deny ALL rule for a group (which another
	 * user is in the group) Another user reads this document - success Another
	 * user updates this document(except access part) - success Another user
	 * updates this document(access part) - success Another user deletes this
	 * document - success 9. The user updates this documents with access rules
	 * (denyFirst) - allow READ and WRITE rule for another user, deny WRITE rule
	 * for a group (which another user is in the group) Another user reads this
	 * document - success Another user updates this document(except access part) -
	 * success Another user updates this document(access part) - failure Another
	 * user deletes this document - failure 10. The user updates this documents
	 * with access rules (denyFirst) - allow READ and WRITE rule for another
	 * user, deny READ rule for a group (which another user is in the group)
	 * Another user reads this document - success Another user updates this
	 * document(except access part) - success Another user updates this
	 * document(access part) -failure Another user deletes this document -
	 * failure
	 */
	public void test201DenyFirst() {
		debug("\nRunning: test201DenyFirstIgnore()");
		String emlVersion = EML2_0_1;
		try {
			newdocid = generateDocumentId();
			// ====1 inserts a document with access rules (denyFirst) - allow
			// READ rule for another user,
			// deny READ rule for public.
			// all are ignored
			String accessRule1 = generateOneAccessRule(anotheruser, true, true, false,
					false, false);
			String accessRule2 = generateOneAccessRule("public", false, true, false,
					false, false);
			Vector<String> accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			String access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 1: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".1", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====2 inserts a document with access rules (denyFirst) - allow
			// READ and WRITE rule for another user,
			// deny READ and WRITE rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, true, true, false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 2: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".2", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".3", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====3 inserts a document with access rules (denyFirst) - allow
			// ALL rule for another user,
			// deny ALL rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, true, true);
			accessRule2 = generateOneAccessRule("public", false, true, true, true, true);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 3: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".4", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".4", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".5", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true, true,
							true, true), null, null, null, null);
			updateDocumentId(newdocid + ".6", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".6", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			newdocid = generateDocumentId();
			// ====4 The user updates this documents with access rules
			// (denyFirst) - allow READ and WRITE
			// rule for another user, deny WRITE rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, false, true, false,
					false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 4: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".2", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====5. The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny READ rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, true, false, false,
					false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 5: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".10", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".10", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".11", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".12", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".12", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====6 inserts a document with access rules (denyFirst) - allow
			// READ rule for another user,
			// deny READ rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and another user is
			// in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, false, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, false,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 6: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".13", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".13", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".14", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".14", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".13", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====7 inserts a document with access rules (allowFirst) - allow
			// READ and WRITE rule for another
			// user, deny READ and WRITE rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and
			// the other user is in this group)
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, true,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 7: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".14", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".14", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".15", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".16", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".15", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====8 inserts a document with access rules (denyFirst) - allow
			// ALL rule for another user,
			// deny ALL rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and the other user
			// is in this group)
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, true, true);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, true,
					true, true);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 8: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".16", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".16", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".17", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true, true,
							true, true), null, null, null, null);
			updateDocumentId(newdocid + ".18", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".18", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			newdocid = generateDocumentId();
			// ====9 The user updates this documents with access rules
			// (denyFirst) - allow READ and WRITE
			// rule for another user, deny WRITE rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org
			// and another user is in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, false, true,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 9: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".20", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".21", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".20", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====10. The user updates this documents with access rules
			// (denyFirst) - allow READ and WRITE
			// rule for another user, deny READ rule for a
			// group(cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and
			// another user is in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, false,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 10: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".21", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".21", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".22", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".23", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".22", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}
	}

	/**
	 * NOTE: as of Metacat 2.0.0, denyFirst permOrder is not supported.
	 * Access rules with denyFirst are ignored and only the document owner 
	 * has access to the object (default).
	 * 
	 * Tests Tests a version 2.1.0 EML document when permission order is
	 * denyFirst, the combination of allow and deny rules affect user to read,
	 * update and delete a document. Here are test cases 
	 * 
	 * 1. A user inserts a document with access rules (denyFirst) - allow READ 
	 * rule for another user, deny READ rule for public. Another user reads this 
	 * document - success. Another user updates this document(except access part) 
	 * - failure. Another user updates this document(access part) -failure Another 
	 * user deletes this document - failure 
	 * 
	 * 2. The user updates this documents with access rules (denyFirst) - allow 
	 * READ and WRITE rule for another user, deny READ and WRITE rule for public. 
	 * Another user reads this document - success. Another user updates this 
	 * document(except access part) - success. Another user updates this 
	 * document(access part) -failure. Another user deletes this document - failure.
	 *  
	 * 3. The user updates this documents with access rules (denyFirst) - allow 
	 * ALL rule for another user, deny ALL rule for public. Another user reads 
	 * this document - success. Another user updates this document(except access part)
	 * - success. Another user updates this document(access part) - success. Another 
	 * user deletes this document - success.
	 * 
	 * 4. The user updates this documents with access rules (denyFirst) -
	 * allow READ and WRITE rule for another user, deny WRITE rule for public.
	 * Another user reads this document - success. Another user updates this
	 * document(except access part) - success. Another user updates this
	 * document(access part) -failure. Another user deletes this document -
	 * failure. 
	 * 
	 * 5. The user updates this documents with access rules (denyFirst) -
	 * allow READ and WRITE rule for another user, deny READ rule for public.
	 * Another user reads this document - success. Another user updates this
	 * document(except access part) - success. Another user updates this
	 * document(access part) - failure. Another user deletes this document -
	 * failure.
	 * 
	 * 6. The user updates this documents with access rules (denyFirst) -
	 * allow READ rule for another user, deny READ rule for a group (which
	 * another user is in the group). Another user reads this document - success.
	 * Another user updates this document(except access part) - failure. Another
	 * user updates this document(access part) -failure. Another user deletes
	 * this document - failure.
	 *  
	 * 7. The user updates this documents with access rules (denyFirst) - allow 
	 * READ and WRITE rule for another user, deny READ and WRITE rule for a 
	 * group (which another user is in the group). Another user reads this document
	 * - success. Another user updates this document(except access part) - success. 
	 * Another user updates this document(access part) -failure. Another user deletes 
	 * this document - failure. 
	 * 
	 * 8. The user updates this documents with access rules (denyFirst) -
	 * allow ALL rule for another user, deny ALL rule for a group (which another
	 * user is in the group). Another user reads this document - success. Another
	 * user updates this document(except access part) - success. Another user
	 * updates this document(access part) - success. Another user deletes this
	 * document - success.
	 *  
	 * 9. The user updates this documents with access rules (denyFirst) - allow 
	 * READ and WRITE rule for another user, deny WRITE rule for a group (which 
	 * another user is in the group). Another user reads this document - success. 
	 * Another user updates this document(except access part) - success. Another 
	 * user updates this document(access part) - failure. Another user deletes 
	 * this document - failure.
	 *  
	 * 10. The user updates this documents with access rules (denyFirst) - allow 
	 * READ and WRITE rule for another user, deny READ rule for a group (which 
	 * another user is in the group). Another user reads this document - success. 
	 * Another user updates this document(except access part) - success. Another 
	 * user updates this document(access part) -failure. Another user deletes 
	 * this document - failure.
	 */
	public void test210DenyFirst() {
		debug("\nRunning: test210DenyFirstIgnore()");
		String emlVersion = EML2_1_0;
		try {
			newdocid = generateDocumentId();
			// ====1 inserts a document with access rules (denyFirst) - allow
			// READ rule for another user,
			// deny READ rule for public.
			String accessRule1 = generateOneAccessRule(anotheruser, true, true, false,
					false, false);
			String accessRule2 = generateOneAccessRule("public", false, true, false,
					false, false);
			Vector<String> accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			String access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 1: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".1", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====2 inserts a document with access rules (denyFirst) - allow
			// READ and WRITE rule for another user,
			// deny READ and WRITE rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, true, true, false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 2: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".2", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".3", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====3 inserts a document with access rules (denyFirst) - allow
			// ALL rule for another user,
			// deny ALL rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, true, true);
			accessRule2 = generateOneAccessRule("public", false, true, true, true, true);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 3: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".4", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".4", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".5", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true, true,
							true, true), null, null, null, null);
			updateDocumentId(newdocid + ".6", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".6", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			newdocid = generateDocumentId();
			// ====4 The user updates this documents with access rules
			// (denyFirst) - allow READ and WRITE
			// rule for another user, deny WRITE rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, false, true, false,
					false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 4: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".2", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====5. The user updates this documents with access rules
			// (allowFirst) - allow READ and WRITE
			// rule for another user, deny READ rule for public.
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule("public", false, true, false, false,
					false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 5: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".10", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".10", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".11", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".12", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".12", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====6 inserts a document with access rules (denyFirst) - allow
			// READ rule for another user,
			// deny READ rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and another user is
			// in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, false, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, false,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 6: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".13", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".13", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".14", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".14", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".13", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====7 inserts a document with access rules (allowFirst) - allow
			// READ and WRITE rule for another
			// user, deny READ and WRITE rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and
			// the other user is in this group)
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, true,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 7: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".14", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".14", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".15", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".16", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".15", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====8 inserts a document with access rules (denyFirst) - allow
			// ALL rule for another user,
			// deny ALL rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and the other user
			// is in this group)
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, true, true);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, true,
					true, true);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 8: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".16", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".16", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".17", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true, true,
							true, true), null, null, null, null);
			updateDocumentId(newdocid + ".18", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".18", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			newdocid = generateDocumentId();
			// ====9 The user updates this documents with access rules
			// (denyFirst) - allow READ and WRITE
			// rule for another user, deny WRITE rule for a group
			// (cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org
			// and another user is in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, false, true,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 9: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".20", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".21", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".20", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();

			// ====10. The user updates this documents with access rules
			// (denyFirst) - allow READ and WRITE
			// rule for another user, deny READ rule for a
			// group(cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org and
			// another user is in this group).
			accessRule1 = generateOneAccessRule(anotheruser, true, true, true, false,
					false);
			accessRule2 = generateOneAccessRule(
					"cn=knb-usr,o=nceas,dc=ecoinformatics,dc=org", false, true, false,
					false, false);
			accessRules = new Vector<String>();
			accessRules.add(accessRule1);
			accessRules.add(accessRule2);
			access = getAccessBlock(accessRules, DENYFIRST);
			debug("Test 10: the access part is " + access);
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, access, null, null, null, null);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			updateDocumentId(newdocid + ".21", testdocument, SUCCESS, false);
			debug("logging out");
			m.logout();
			// login as another user
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			// fails to read this document
			readDocumentIdWhichEqualsDoc(newdocid + ".21", testdocument, FAILURE, true);
			// fails to update this document
			updateDocumentId(newdocid + ".22", testdocument, FAILURE, true);
			// fails to update access part
			testdocument = getTestEmlDoc(
					"Testing user can not read, write and delete a document", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".23", testdocument, FAILURE, true);
			// fails to delete the document
			deleteDocumentId(newdocid + ".22", FAILURE, true);
			// logout
			debug("logging out");
			m.logout();
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}
	}

	/***************************************************************************
	 * Test the case when no access is specified and owner is logged in No
	 * online or inline data is involved 
	 * 
     * 1) a user inserts a document and is able to read it, update it, set 
     * permissions on it and delete it.
     * 
	 * 2) another user is not able to do anything with the document when no access
	 * is specified for that user -> test what all the other user can do when
	 * read only, write only and change permissions only permissions are
	 * specified
	 * 
	 */
	public void document201Test() {
		try {
			debug("\ndocument201Test() : Starting");
			String emlVersion = EML2_0_1;

			newdocid = generateDocumentId();

			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// insert a 2.0.0 document
			testdocument = testEml_200_Header
					+ "<dataset scope=\"document\"><title>submitting eml200</title>"
					+ testEmlCreatorBlock + testEmlContactBlock + "</dataset></eml:eml>";

			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);

			// read the document
			readDocumentIdWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);

			// update it with 2.0.1 document
			testdocument = getTestEmlDoc("Updating eml200 with eml201", emlVersion, null,
					null, null, null, null, null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);

			debug("logging out");
			m.logout();

			newdocid = generateDocumentId();

			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// insert a document
			testdocument = getTestEmlDoc("Testing insert", emlVersion, null, null, null,
					null, null, null, null, null, null);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);

			// read the document
			readDocumentIdWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);

			// update the document
			testdocument = getTestEmlDoc("Testing update", emlVersion, null, null, null,
					null, null, null, null, null, null);
			Thread.sleep(10000);
			updateDocumentId(newdocid + ".2", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);

			// ///////////////////////////
			// check what the another user can do
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);

			// check if the user mentioned is able to read/update/delete the
			// document
			readDocumentIdWhichEqualsDoc(newdocid + ".2", testdocument, FAILURE, true);
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			deleteDocumentId(newdocid + ".2", FAILURE, true);

			// ///////////////////////////
			// update the document access control - read only
			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, false,
							false, false), null, null, null, null);
			updateDocumentId(newdocid + ".3", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".3", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			debug(testdocument);
			readDocumentIdWhichEqualsDoc(newdocid + ".3", testdocument, SUCCESS, false);

			// should not be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);

			// or the permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);

			// or delete the document
			deleteDocumentId(newdocid + ".3", FAILURE, true);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// /////////////////////////////////
			// update the document access control - write only
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, true,
							false, false), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".4", testdocument, SUCCESS, false);
			// System.out.println(testdocument);
			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".4", testdocument, FAILURE, true);

			// should be able to update the document

			// System.out.println(testdocument);
			updateDocumentId(newdocid + ".5", testdocument, SUCCESS, false);

			// but not the permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, true,
							false, true), null, null, null, null);
			updateDocumentId(newdocid + ".6", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocumentId(newdocid + ".5", FAILURE, true);
			// deleteDocumentId(newdocid + ".4", FAILURE, true);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - change permissions only
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							true, false), null, null, null, null);
			updateDocumentId(newdocid + ".6", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".6", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".6", testdocument, SUCCESS, false);

			// should also be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, true, false), null, null, null, null);
			updateDocumentId(newdocid + ".7", testdocument, SUCCESS, false);

			// and can chg the permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			// ERRRRRRRROR
			// updateDocumentId(newdocid + ".7", testdocument, SUCCESS, false);

			// try to delete the document
			// deleteDocumentId(newdocid + ".7", FAILURE, true);
			//deleteDocumentId(newdocid + ".7", SUCCESS, false);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - read & write
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, true,
							false, false), null, null, null, null);
			updateDocumentId(newdocid + ".8", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".8", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".8", testdocument, SUCCESS, false);

			// should be able to update the document
			updateDocumentId(newdocid + ".9", testdocument, SUCCESS, false);

			// but cant chg the permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			updateDocumentId(newdocid + ".10", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocumentId(newdocid + ".9", FAILURE, true);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - read & change permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, false,
							true, false), null, null, null, null);
			updateDocumentId(newdocid + ".10", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".10", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".10", testdocument, SUCCESS, false);
			
			// should also be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, true, false), null, null, null, null);
			updateDocumentId(newdocid + ".11", testdocument, SUCCESS, false);

			// and can chg the permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			// updateDocumentId(newdocid + ".11", testdocument, SUCCESS, false);

			// try to delete the document
			// deleteDocumentId(newdocid + ".11", FAILURE, true);
			//deleteDocumentId(newdocid + ".11", FAILURE, true);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - read & change permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, false,
							true, false), null, null, null, null);
			updateDocumentId(newdocid + ".12", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".12", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".12", testdocument, SUCCESS, false);

			// should be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, true, false), null, null, null, null);
			updateDocumentId(newdocid + ".13", testdocument, SUCCESS, false);
			
			// and can chg the permissions
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, false, true), null, null, null, null);
			// ERRRRRRRRRRRRRRRR
			// updateDocumentId(newdocid + ".12", testdocument, SUCCESS, false);

			// try to delete the document
			// deleteDocumentId(newdocid + ".13", FAILURE, true);
			//deleteDocumentId(newdocid + ".13", SUCCESS, false);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - R, W, CP
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, true, true,
							false), null, null, null, null);
			updateDocumentId(newdocid + ".14", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".14", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".14", testdocument, SUCCESS, false);

			// should be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, false, true), null, null, null, null);
			updateDocumentId(newdocid + ".15", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".15", testdocument, SUCCESS, false);

			// and can chg the permissions
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".16", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".16", testdocument, SUCCESS, false);

			// try to delete the document
			//deleteDocumentId(newdocid + ".16", FAILURE, true);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - all
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			updateDocumentId(newdocid + ".17", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".17", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".17", testdocument, SUCCESS, false);

			// should be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, false, true), null, null, null, null);
			updateDocumentId(newdocid + ".18", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".18", testdocument, SUCCESS, false);

			// and can chg the permissions
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".19", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".19", testdocument, SUCCESS, false);

			// try to delete the document
			deleteDocumentId(newdocid + ".19", FAILURE, true);

			debug("logging out");
			m.logout();

			// delete the document
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			deleteDocumentId(newdocid + ".19", SUCCESS, false);
			debug("logging out");
			m.logout();
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}

	}

	/***************************************************************************
	 * Test the case when no access is specified and owner is logged in No
	 * online or inline data is involved -> an user inserts a document and is
	 * able to read it, update it, set permissions on it and delete it ->
	 * another user is not able to do anything with the document when no access
	 * is specified for that user -> test what all the other user can do when
	 * read only, write only and change permissions only permissions are
	 * specified
	 * 
	 */
	public void document210Test() {
		try {
			debug("\nRunning: document210Test()");
			String emlVersion = EML2_1_0;

			newdocid = generateDocumentId();

			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// insert a 2.0.0 document
			testdocument = testEml_200_Header
					+ "<dataset scope=\"document\"><title>submitting eml200</title>"
					+ testEmlCreatorBlock + testEmlContactBlock + "</dataset></eml:eml>";

			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);

			// read the document
			readDocumentIdWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);

			// update it with 2.0.1 document
			testdocument = getTestEmlDoc("Updating eml200 with eml201", EML2_0_1, null,
					null, null, null, null, null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);

			// update it with 2.1.0 document
			testdocument = getTestEmlDoc("Updating eml201 with eml210", emlVersion, null,
					null, null, null, null, null, null, null, null);
			updateDocumentId(newdocid + ".3", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);

			debug("logging out");
			m.logout();

			newdocid = generateDocumentId();

			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// insert a document
			testdocument = getTestEmlDoc("Testing insert", emlVersion, null, null, null,
					null, null, null, null, null, null);
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);

			// read the document
			readDocumentIdWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);

			// update the document
			testdocument = getTestEmlDoc("Testing update", emlVersion, null, null, null,
					null, null, null, null, null, null);
			Thread.sleep(10000);
			updateDocumentId(newdocid + ".2", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);

			// ///////////////////////////
			// check what the another user can do
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);

			// check if the user mentioned is able to read/update/delete the
			// document
			readDocumentIdWhichEqualsDoc(newdocid + ".2", testdocument, FAILURE, true);
			updateDocumentId(newdocid + ".3", testdocument, FAILURE, true);
			deleteDocumentId(newdocid + ".2", FAILURE, true);

			// ///////////////////////////
			// update the document access control - read only
			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, false,
							false, false), null, null, null, null);
			updateDocumentId(newdocid + ".3", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".3", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			debug(testdocument);
			readDocumentIdWhichEqualsDoc(newdocid + ".3", testdocument, SUCCESS, false);

			// should not be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);

			// or the permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, FAILURE, true);

			// or delete the document
			deleteDocumentId(newdocid + ".3", FAILURE, true);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// /////////////////////////////////
			// update the document access control - write only
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, true,
							false, false), null, null, null, null);
			updateDocumentId(newdocid + ".4", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".4", testdocument, SUCCESS, false);
			// System.out.println(testdocument);
			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".4", testdocument, FAILURE, true);

			// should be able to update the document

			// System.out.println(testdocument);
			updateDocumentId(newdocid + ".5", testdocument, SUCCESS, false);

			// but not the permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, true,
							false, true), null, null, null, null);
			updateDocumentId(newdocid + ".6", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocumentId(newdocid + ".5", FAILURE, true);
			// deleteDocumentId(newdocid + ".4", FAILURE, true);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - change permissions only
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							true, false), null, null, null, null);
			updateDocumentId(newdocid + ".6", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".6", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".6", testdocument, SUCCESS, false);

			// should also be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, true, false), null, null, null, null);
			updateDocumentId(newdocid + ".7", testdocument, SUCCESS, false);

			// and can chg the permissions, of course
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			// ERRRRRRRROR
			// updateDocumentId(newdocid + ".7", testdocument, SUCCESS, false);

			// we don't want to delete the document yet try to delete the document
			// deleteDocumentId(newdocid + ".7", FAILURE, true);
			//deleteDocumentId(newdocid + ".6", FAILURE, true);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - read & write
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, true,
							false, false), null, null, null, null);
			updateDocumentId(newdocid + ".8", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".8", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".8", testdocument, SUCCESS, false);

			// should be able to update the document
			updateDocumentId(newdocid + ".9", testdocument, SUCCESS, false);

			// but cant chg the permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			updateDocumentId(newdocid + ".10", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocumentId(newdocid + ".9", FAILURE, true);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - read & change permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, false,
							true, false), null, null, null, null);
			updateDocumentId(newdocid + ".10", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".10", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".10", testdocument, SUCCESS, false);
			
			// should also be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, true, false), null, null, null, null);
			updateDocumentId(newdocid + ".11", testdocument, SUCCESS, false);

			// and can chg the permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			// updateDocumentId(newdocid + ".12", testdocument, SUCCESS, false);

			// try to delete the document
			// deleteDocumentId(newdocid + ".12", FAILURE, true);
			//deleteDocumentId(newdocid + ".12", SUCCESS, false);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - read & change permissions
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, false,
							true, false), null, null, null, null);
			updateDocumentId(newdocid + ".12", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".12", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".12", testdocument, SUCCESS, false);

			// should also be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, true, false), null, null, null, null);
			updateDocumentId(newdocid + ".13", testdocument, SUCCESS, false);
			
			// and can chg the permissions
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, false, true), null, null, null, null);
			// 
			updateDocumentId(newdocid + ".14", testdocument, SUCCESS, false);

			// try to delete the document
			// deleteDocumentId(newdocid + ".13", FAILURE, true);
			//deleteDocumentId(newdocid + ".14", SUCCESS, false);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - R, W, CP
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, true, true, true,
							false), null, null, null, null);
			updateDocumentId(newdocid + ".15", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".15", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".15", testdocument, SUCCESS, false);

			// should be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, false, true), null, null, null, null);
			updateDocumentId(newdocid + ".16", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".16", testdocument, SUCCESS, false);

			// and can chg the permissions
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".17", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".17", testdocument, SUCCESS, false);

			// try to delete the document
			//deleteDocumentId(newdocid + ".17", SUCCESS, false);

			debug("logging out");
			m.logout();
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// ///////////////////////////////
			// update the document access control - all
			testdocument = getTestEmlDoc("Testing update access block", emlVersion, null,
					null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), null, null, null, null);
			updateDocumentId(newdocid + ".18", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".18", testdocument, SUCCESS, false);

			// check if the user mentioned is able to read the document
			debug("logging out");
			m.logout();
			debug("logging in as: anotheruser=" + anotheruser + " anotherpassword="
					+ anotherpassword);
			m.login(anotheruser, anotherpassword);
			readDocumentIdWhichEqualsDoc(newdocid + ".18", testdocument, SUCCESS, false);

			// should be able to update the document
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, false,
							false, false, true), null, null, null, null);
			updateDocumentId(newdocid + ".19", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".19", testdocument, SUCCESS, false);

			// and can chg the permissions
			testdocument = getTestEmlDoc("Testing update from another user", emlVersion,
					null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null, null);
			updateDocumentId(newdocid + ".20", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".20", testdocument, SUCCESS, false);

			// try to delete the document
			deleteDocumentId(newdocid + ".20", FAILURE, true);

			debug("logging out");
			m.logout();

			// delete the document
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			deleteDocumentId(newdocid + ".20", SUCCESS, false);
			debug("logging out");
			m.logout();
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}

	}

	/***************************************************************************
	 * Test the case when no access is specified and public is logged in for eml
	 * version 2.0.1 docuements. Cases being checked: 1. public tries to read
	 * the document - Failure. 2. public tries to update the document - Failure.
	 * 3. public tries to update the inline data - Failure. 4. public tries to
	 * update the online data file - Failure. 5. public tries to update the
	 * access rules for the document - Failure. 6. public tries to update the
	 * access rules for the inline data - Failure. 7. public tries to update the
	 * access rules for the online data - Failure. 8. public tries to delete the
	 * document - Failure.
	 */
	public void AccessControlTest201ForPublic() {
		try {
			debug("\nRunning: AccessControlTest201ForPublic()");
			String emlVersion = EML2_0_1;

			String accessBlock = getAccessBlock(anotheruser, true, true, false, false,
					false);
			newdocid = generateDocumentId();

			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			onlineDocid = generateDocumentId();
			uploadDocumentId(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, null, null, null, null, null);

			// insert a document - get the docid
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, SUCCESS, false);

			// logoutand login as other user
			debug("logging out");
			m.logout();

			// read the document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", null, FAILURE, true);

			// update the document
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// update the inline data
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock2, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, null, null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// update the online data
			uploadDocumentId(onlineDocid + ".2", onlinetestdatafile1, FAILURE, false);

			// update the document access control
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, accessBlock, null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// update the document access control for inline data
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, null, accessBlock, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// update the document access control for online data
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, null, null, null, accessBlock, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// delete the document
			deleteDocumentId(newdocid + ".2", FAILURE, true);
			debug("logging out");
			m.logout();
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (MetacatException me) {
			fail("Metacat Error:\n" + me.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}

	}

	/***************************************************************************
	 * Test the case when no access is specified and public is logged in for eml
	 * version 2.1.0 docuements. Cases being checked: 1. public tries to read
	 * the document - Failure. 2. public tries to update the document - Failure.
	 * 3. public tries to update the inline data - Failure. 4. public tries to
	 * update the online data file - Failure. 5. public tries to update the
	 * access rules for the document - Failure. 6. public tries to update the
	 * access rules for the inline data - Failure. 7. public tries to update the
	 * access rules for the online data - Failure. 8. public tries to delete the
	 * document - Failure.
	 */
	public void AccessControlTest210ForPublic() {
		try {
			debug("\nRunning: AccessControlTest210ForPublic()");
			String emlVersion = EML2_1_0;

			String accessBlock = getAccessBlock(anotheruser, true, true, false, false,
					false);
			newdocid = generateDocumentId();

			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			onlineDocid = generateDocumentId();
			uploadDocumentId(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, null, null, null, null, null);

			// insert a document - get the docid
			insertDocumentId(newdocid + ".1", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(newdocid + ".1", testdocument, SUCCESS, false);

			// logoutand login as other user
			debug("logging out");
			m.logout();

			// read the document
			readDocumentIdWhichEqualsDoc(newdocid + ".1", null, FAILURE, true);

			// update the document
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// update the inline data
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock2, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, null, null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// update the online data
			uploadDocumentId(onlineDocid + ".2", onlinetestdatafile1, FAILURE, false);

			// update the document access control
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, accessBlock, null, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// update the document access control for inline data
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, null, accessBlock, null, null, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// update the document access control for online data
			testdocument = getTestEmlDoc("Testing insert", emlVersion,
					testEmlInlineBlock1, null, "ecogrid://knb/" + onlineDocid + ".1",
					null, null, null, null, accessBlock, null);
			updateDocumentId(newdocid + ".2", testdocument, FAILURE, false);

			// delete the document
			deleteDocumentId(newdocid + ".2", FAILURE, true);
			debug("logging out");
			m.logout();
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (MetacatException me) {
			fail("Metacat Error:\n" + me.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}

	}
}
