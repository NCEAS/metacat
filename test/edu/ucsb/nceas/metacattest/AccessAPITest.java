/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author: daigle $'
 *     '$Date: 2009-08-14 14:26:08 -0700 (Fri, 14 Aug 2009) $'
 * '$Revision: 5027 $'
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

import java.io.IOException;
import java.io.StringReader;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlList;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.access.DocInfoHandler;
import edu.ucsb.nceas.utilities.access.XMLAccessDAO;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;


import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A JUnit test for testing Access Control API in Metacat.  Note that this is different
 * than the Access Control test.  That test is more concerned with making sure different
 * combinations of access rules act appropriately.  This test is more concerned with the
 * funtioning of the API itself.
 */
public class AccessAPITest extends MCTestCase {

	/**
	 * Constructor to build the test
	 * 
	 * @param name
	 *            the name of the test method
	 */
	public AccessAPITest(String name) {
		super(name);
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
		suite.addTest(new AccessAPITest("initialize"));
		// Test basic functions
		suite.addTest(new AccessAPITest("testSetSingleAccess"));
		suite.addTest(new AccessAPITest("testSetBlockAccess"));

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
	 * Test adding access records for single principals.
	 */
	public void testSetSingleAccess() {
		String principal = null;
		Vector<XMLAccessDAO> accessDAOList = null;
		Vector<XMLAccessDAO> realAccessDAOList = null;
		XMLAccessDAO accessDAO = null;
		String accessXML = null;
	
		try {
			debug("AccessAPITest.testSetSingleAccess Running");
			
			m.login(username, password);

			// insert a document for us to test with
			String docId = generateDocumentId() + ".1";
			String testDoc = getTestDocFromFile(testdatadir + "accessBlockTestFile1.xml");
			// debug("test doc: " + testDoc);
			insertDocumentId(docId, testDoc, SUCCESS, false);
					
			// test inserting a single access record with allowFirst permission order and allow read 
			// access.  We expect this to succeed.
			debug("AccessAPITest.testSetSingleAccess - Test 1, add user allowFirst allow read access");
			principal = "uid=test30,o=NCEAS,dc=ecoinformatics,dc=org";
			setSingleAccess(docId, principal, 
					"READ", 
					AccessControlInterface.ALLOW, 
					AccessControlInterface.ALLOWFIRST,
					true);
			accessDAOList = new Vector<XMLAccessDAO>();
			accessDAO = new XMLAccessDAO();
			accessDAO.setGuid(docId);
			accessDAO.setPrincipalName(principal);
			accessDAO.setPermission(new Long(AccessControlInterface.READ));
			accessDAO.setPermType(AccessControlInterface.ALLOW);
			accessDAO.setPermOrder(AccessControlInterface.ALLOWFIRST);
			accessDAOList.add(accessDAO);
			
			// get the access control for this doc id from metacat and make sure this
			// access record is in that list.
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);	
			compareAccessDAOs(realAccessDAOList, accessDAOList, false);	
			
			// test inserting a single access record with denyFirst permission order and allow read 
			// access.  We expect this to fail since a single doc cannot have multiple permission orders.
			debug("AccessAPITest.testSetSingleAccess - Test 2, add user denyFirst allow read/write access");
			principal = "uid=test31,o=NCEAS,dc=ecoinformatics,dc=org";
			setSingleAccess(docId, principal, 
					Integer.toString(AccessControlInterface.READ | AccessControlInterface.WRITE), 
					AccessControlInterface.ALLOW, 
					AccessControlInterface.DENYFIRST,
					false);


		} catch (AccessControlException ace) {
			fail("AccessAPITest.testSetSingleAccess - Access Control error: " + ace.getMessage());
		} catch (IOException ioe) {
			fail("AccessAPITest.testSetSingleAccess - I/O error: " + ioe.getMessage());
		} catch (MetacatAuthException mae) {
			fail("AccessAPITest.testSetSingleAccess - Metacat authentication error: " + mae.getMessage()); 
		} catch (MetacatException me) {
			fail("AccessAPITest.testSetSingleAccess - Metacat error: " + me.getMessage()); 
		} catch (InsufficientKarmaException ike) {
			fail("AccessAPITest.testSetSingleAccess - Insufficient karma error: " + ike.getMessage()); 
		} catch (MetacatInaccessibleException mie) {
			fail("AccessAPITest.testSetSingleAccess - Metacat Inaccessible Error: " + mie.getMessage());
		} 
	}
	
	/**
	 * Test setting a block of access for a document.  Each call should replace the access for that
	 * document.
	 */
	public void testSetBlockAccess() {
		String accessBlockXML = null;
		String accessXML = null;
		Vector<XMLAccessDAO> realAccessDAOList = null;
		Vector<XMLAccessDAO> accessBlockDAOList = null;
		
		try {
			debug("AccessAPITest.testSetBlockAccess - Running");
					
			// log in
			m.login(username, password);

			// insert a document for us to test with
			String docId = generateDocumentId() + ".1";
			String testDoc = getTestDocFromFile(testdatadir + "accessBlockTestFile1.xml");
			// debug("test doc: " + testDoc);
			insertDocumentId(docId, testDoc, SUCCESS, false);
			
			// replace access for doc using the access section in accessBlock1.xml, 
			// then test that the access was updated and that is the only access
			// for the doc
			debug("AccessAPITest.testSetBlockAccess - Test 1 using accessBlock1.xml");			
			accessBlockXML = getTestDocFromFile(testdatadir + "accessBlock1.xml");
			setBlockAccess(docId, accessBlockXML, true);
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);
			accessBlockDAOList = getAccessDAOList(docId, accessBlockXML);
			debugAccessDAOList(accessBlockDAOList);
			compareAccessDAOs(realAccessDAOList, accessBlockDAOList, true);

			// replace access for doc using the access section in accessBlock2.xml, 
			// then test that the access was updated and that is the only access
			// for the doc
			debug("AccessAPITest.testSetBlockAccess - Test 2 using accessBlock2.xml");			
			accessBlockXML = getTestDocFromFile(testdatadir + "accessBlock2.xml");
			setBlockAccess(docId, accessBlockXML, true);
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);
			accessBlockDAOList = getAccessDAOList(docId, accessBlockXML);
			debugAccessDAOList(accessBlockDAOList);
			compareAccessDAOs(realAccessDAOList, accessBlockDAOList, true);

			// replace access for doc using the access section in accessBlock3.xml, 
			// then test that the access was updated and that is the only access
			// for the doc
			debug("AccessAPITest.testSetBlockAccess - Test 3 using accessBlock3.xml");			
			accessBlockXML = getTestDocFromFile(testdatadir + "accessBlock3.xml");
			setBlockAccess(docId, accessBlockXML, true);
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);
			accessBlockDAOList = getAccessDAOList(docId, accessBlockXML);
			debugAccessDAOList(accessBlockDAOList);
			compareAccessDAOs(realAccessDAOList, accessBlockDAOList, true);
			
			// replace access for doc using the access section in accessBlock4.xml, 
			// then test that the access was updated and that is the only access
			// for the doc
			debug("AccessAPITest.testSetBlockAccess - Test 4 using accessBlock4.xml");			
			accessBlockXML = getTestDocFromFile(testdatadir + "accessBlock4.xml");
			setBlockAccess(docId, accessBlockXML, true);
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);
			accessBlockDAOList = getAccessDAOList(docId, accessBlockXML);
			debugAccessDAOList(accessBlockDAOList);
			compareAccessDAOs(realAccessDAOList, accessBlockDAOList, true);
			
			// replace access for doc using the access section in accessBlock5.xml, 
			// then test that the access was updated and that is the only access
			// for the doc
			debug("AccessAPITest.testSetBlockAccess - Test 5 using accessBlock5.xml");			
			accessBlockXML = getTestDocFromFile(testdatadir + "accessBlock5.xml");
			setBlockAccess(docId, accessBlockXML, true);
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);
			accessBlockDAOList = getAccessDAOList(docId, accessBlockXML);
			debugAccessDAOList(accessBlockDAOList);
			compareAccessDAOs(realAccessDAOList, accessBlockDAOList, true);
			
			// replace access for doc using the access section in accessBlock6.xml, 
			// then test that the access was updated and that is the only access
			// for the doc
			debug("AccessAPITest.testSetBlockAccess - Test 6 using accessBlock6.xml");			
			accessBlockXML = getTestDocFromFile(testdatadir + "accessBlock6.xml");
			setBlockAccess(docId, accessBlockXML, true);
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);
			accessBlockDAOList = getAccessDAOList(docId, accessBlockXML);
			debugAccessDAOList(accessBlockDAOList);
			compareAccessDAOs(realAccessDAOList, accessBlockDAOList, true);
			
			// replace access for doc using the access section in accessBlock7.xml, 
			// then test that the access was updated and that is the only access
			// for the doc
			debug("AccessAPITest.testSetBlockAccess - Test 7 using accessBlock7.xml");			
			accessBlockXML = getTestDocFromFile(testdatadir + "accessBlock7.xml");
			setBlockAccess(docId, accessBlockXML, true);
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);
			accessBlockDAOList = getAccessDAOList(docId, accessBlockXML);
			debugAccessDAOList(accessBlockDAOList);
			compareAccessDAOs(realAccessDAOList, accessBlockDAOList, true);
			
			// replace access for doc using the access section in accessBlock8.xml, 
			// then test that the access was updated and that is the only access
			// for the doc
			debug("AccessAPITest.testSetBlockAccess - Test 8 using accessBlock8.xml");			
			accessBlockXML = getTestDocFromFile(testdatadir + "accessBlock8.xml");
			setBlockAccess(docId, accessBlockXML, true);
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);
			accessBlockDAOList = getAccessDAOList(docId, accessBlockXML);
			debugAccessDAOList(accessBlockDAOList);
			compareAccessDAOs(realAccessDAOList, accessBlockDAOList, true);
			
			// replace access for doc using the access section in accessBlock9.xml, 
			// then test that the access was updated and that is the only access
			// for the doc
			debug("AccessAPITest.testSetBlockAccess - Test 9 using accessBlock9.xml");			
			accessBlockXML = getTestDocFromFile(testdatadir + "accessBlock9.xml");
			setBlockAccess(docId, accessBlockXML, true);
			accessXML = m.getAccessControl(docId);
			realAccessDAOList = getAccessDAOList(docId, accessXML);
			accessBlockDAOList = getAccessDAOList(docId, accessBlockXML);
			debugAccessDAOList(accessBlockDAOList);
			compareAccessDAOs(realAccessDAOList, accessBlockDAOList, true);
			
		} catch (AccessControlException ace) {
			fail("AccessAPITest.testSetBlockAccess - Access Control error: " + ace.getMessage());
		} catch (IOException ioe) {
			fail("AccessAPITest.testSetBlockAccess - I/O error: " + ioe.getMessage());
		} catch (MetacatAuthException mae) {
			fail("AccessAPITest.testSetBlockAccess - Metacat authentication error: " + mae.getMessage()); 
		} catch (MetacatException me) {
			fail("AccessAPITest.testSetBlockAccess - Metacat error: " + me.getMessage()); 
		} catch (InsufficientKarmaException ike) {
			fail("AccessAPITest.testSetBlockAccess - Metacat error: " + ike.getMessage()); 
		} catch (MetacatInaccessibleException mie) {
			fail("AccessAPITest.testSetBlockAccess - Metacat Inaccessible Error: " + mie.getMessage());
		}
	}

	/**
	 * Insert a single row of access for a document.
	 * 
	 * @param docId
	 *            the id of the document to update
	 * @param principal
	 *            the principal credentails
	 * @param permission
	 *            the permission
	 * @param permType
	 *            the permission type
	 * @param permOrder
	 *            the permission order
	 * @param successExpected
	 *            if true, we expect the insert to succeed, otherwise, we expect
	 *            it to fail
	 * @return the metacat response xml
	 */
	private String setSingleAccess(String docId, String principal, String permission, 
			String permType, String permOrder, boolean successExpected) {
		debug("AccessAPITest.setSingleAccess - docid: " + docId + ", prinicpal: " + 
				principal + ", permission: " + permission + ", permType: " + permType + 
				", permOrder: " + permOrder + ", errorExpected: " + successExpected);
		String response = null;
		try {
			// set the access using metacat client
			response = m.setAccess(docId, principal, permission, permType, permOrder); 

			debug("AccessAPITest.setSingleAccess - response: " + response);
			
			if (successExpected) {
				assertTrue(response, (response.indexOf("<success>") != -1));
			} else {
				assertTrue(response, (response.indexOf("<success>") == -1));
			}			
		} catch (MetacatInaccessibleException mie) {
			fail("AccessAPITest.setSingleAccess - Metacat inaccessible: " + mie.getMessage());
		} catch (InsufficientKarmaException ike) {

		} catch (MetacatException me) {
			if (successExpected) {
				fail("AccessAPITest.setSingleAccess - Metacat error: " + me.getMessage());
			} else {
				debug("AccessAPITest.setSingleAccess - Expected Metacat error: " + me.getMessage());
			}
		} catch (Exception e) {
			fail("AccessAPITest.setSingleAccess - General error: " + e.getMessage());
		}

		return response;
	}
	
	/**
	 * Replace a block of access
	 * 
	 * @param docId
	 *            the id of the doc we want to update
	 * @param accessBlockXML
	 *            the access xml section
	 * @param successExpected
	 *            if true, we expect the insert to succeed, otherwise, we expect
	 *            it to fail
	 * @return the metacat response xml
	 */
	private String setBlockAccess(String docId, String accessBlockXML, boolean successExpected) {
		debug("AccessAPITest.setBlockAccess - docid: " + docId + ", accessBlockXML: " + 
				accessBlockXML + ", errorExpected: " + successExpected);
		String response = null;
		try {
			// set the access using metacat client
			response = m.setAccess(docId, accessBlockXML); 

			debug("AccessAPITest.setBlockAccess - response: " + response);
			
			if (successExpected) {
				assertTrue(response, (response.indexOf("<success>") != -1));
			} else {
				assertTrue(response, (response.indexOf("<success>") == -1));
			}			
		} catch (MetacatInaccessibleException mie) {
			fail("AccessAPITest.setBlockAccess - Metacat inaccessible: " + mie.getMessage());
		} catch (InsufficientKarmaException ike) {

		} catch (MetacatException me) {
			if (successExpected) {
				fail("AccessAPITest.setBlockAccess - Metacat error: " + me.getMessage());
			} else {
				debug("AccessAPITest.setBlockAccess - Expected Metacat error: " + me.getMessage());
			}
		} catch (Exception e) {
			fail("AccessAPITest.setBlockAccess - General error: " + e.getMessage());
		}

		return response;
	}
	
	/**
	 * Compare two lists of XML Access DAO objects. The first list is the master
	 * list and the second is the sub list. All elements of the sub list must be
	 * in the master list or a failed assertion is generated. If the exactMatch
	 * parameter is true, the master and sub lists must be the same, otherwise,
	 * the sub list can be a subset of the master list.
	 * 
	 * @param mainAccessDAOList
	 *            The main list of DAOs
	 * @param subAccessDAOList
	 *            The sub list of DAOs
	 * @param exactMatch
	 *            if true, the master and sub lists must be exactly the same,
	 *            otherwise, the sublist must only be a subset of the master
	 */
	private void compareAccessDAOs(Vector<XMLAccessDAO> mainAccessDAOList, 
			Vector<XMLAccessDAO> subAccessDAOList, boolean exactMatch) {
		
		
		//debug("test access DAO: ");
		//debugAccessDAOList(subAccessDAOList);
		//debug("\naccess DAOs from database: ");
		//debugAccessDAOList(mainAccessDAOList);
		
		// if exactMatch is true, both lists must be the same size.  The next section will
		// make sure elements match.
		if (exactMatch) {
			if (mainAccessDAOList.size() != subAccessDAOList.size()) {
				fail("AccessAPITest.compareAccessDAOs - access DAO list sizes do not match.  " + 
						"Primary DAO list size: " + mainAccessDAOList.size() + 
						", secondary DAO list size: " + subAccessDAOList.size());
			}
		}
		
		
		// iterate through the sub list and make sure all its elements are in the master list
		for (XMLAccessDAO subAccessDAO : subAccessDAOList) {
			boolean matchFound = false;
			for (XMLAccessDAO mainAccessDAO : mainAccessDAOList) {		
			    /*System.out.println("subAccessDAO.docid: '" + subAccessDAO.getDocId() + "'");
			    System.out.println("mainAccessDAO.docid: '" + mainAccessDAO.getDocId() + "'");
			    
			    System.out.println("subAccessDAO.permOrder: '" + subAccessDAO.getPermOrder() + "'");
                System.out.println("mainAccessDAO.permOrder: '" + mainAccessDAO.getPermOrder() + "'");
                
                System.out.println("subAccessDAO.permtype: '" + subAccessDAO.getPermType() + "'");
                System.out.println("mainAccessDAO.docid: '" + mainAccessDAO.getPermType() + "'");
                
                System.out.println("subAccessDAO.princname: '" + subAccessDAO.getPrincipalName() + "'");
                System.out.println("mainAccessDAO.princname: '" + mainAccessDAO.getPrincipalName() + "'");
                
                System.out.println("subAccessDAO.perm: '" + subAccessDAO.getPermission() + "'");
                System.out.println("mainAccessDAO.perm: '" + mainAccessDAO.getPermission() + "'");*/
				if (subAccessDAO.getGuid().equals(mainAccessDAO.getGuid()) &&
						subAccessDAO.getPermOrder().equals(mainAccessDAO.getPermOrder()) &&
						subAccessDAO.getPermType().equals(mainAccessDAO.getPermType()) &&
						subAccessDAO.getPrincipalName().equals(mainAccessDAO.getPrincipalName()) &&
						subAccessDAO.getPermission().equals(mainAccessDAO.getPermission())) {
					matchFound = true;
					break;
				}			
			}
			
			if (!matchFound) {
				fail("AccessAPITest.compareAccessDAOs - secondary access DAO does not exist in " + 
						"primary DAO list");
			}
		}
	}
	
	/**
	 * Get a vector of access DAO objects from an EML 2.1.0-like access section
	 * of xml
	 * 
	 * @param docId
	 *            the doc id that we are getting access for. this makes it
	 *            easier to create a DocInfoHandler object.
	 * @param accessXML
	 *            the access xml we want to parse.
	 * @return a list of XML access DAO objects.
	 */
	private Vector<XMLAccessDAO> getAccessDAOList(String docId, String accessXML) 
			throws AccessControlException {
		
		Vector<XMLAccessDAO> accessControlList = null;
		XMLReader parser = null;
		DocInfoHandler docInfoHandler = new DocInfoHandler(docId); 
		ContentHandler chandler = docInfoHandler;

		try {
			// Get an instance of the parser.  the DocInfoHandler will handle 
			// the extraction of access info.
			String parserName = PropertyService.getProperty("xml.saxparser");
			parser = XMLReaderFactory.createXMLReader(parserName);
	
			// Turn off validation
			parser.setFeature("http://xml.org/sax/features/validation", false);
			parser.setContentHandler((ContentHandler)chandler);
			parser.setErrorHandler((ErrorHandler)chandler);
	
			parser.parse(new InputSource(new StringReader(accessXML)));
				
			accessControlList = docInfoHandler.getAccessControlList();
		} catch (PropertyNotFoundException pnfe) {
			throw new AccessControlException("AccessAPITest.getAccessDAOList - " + 
					"property error: " + pnfe.getMessage());
		} catch (IOException ioe) {
			throw new AccessControlException("AccessAPITest.getAccessDAOList - " + 
					"I/O error: " + ioe.getMessage());
		} catch (SAXException se) {
			throw new AccessControlException("AccessAPITest.getAccessDAOList - " + 
					"SAX error: " + se.getMessage());
		}
        
        return accessControlList;
	}
	
	/**
	 * Print out the contents of an access DAO list
	 * @param accessDAOList
	 */
	private void debugAccessDAOList(Vector<XMLAccessDAO> accessDAOList) {
		for (XMLAccessDAO xmlAccessDAO : accessDAOList) {
			debug("Guid:      " + xmlAccessDAO.getGuid());
			debug("Perm Order:  " + xmlAccessDAO.getPermOrder());
			debug("Perm Type:   " + xmlAccessDAO.getPermType());
			debug("Principal    " + xmlAccessDAO.getPrincipalName());
			String permissionStr = 
				AccessControlList.txtValue(xmlAccessDAO.getPermission().intValue());
			debug("Permission   " + permissionStr);
		}
	}
}
