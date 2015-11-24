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

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.IOUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import junit.framework.Test;
import junit.framework.TestSuite;
import java.io.File;

/**
 * A JUnit test for testing Access Control for online data in Metacat
 */
public class OnlineDataAccessTest
    extends MCTestCase {

    private static String metacatUrl;
    private static String username;
	private static String password;
	private static String anotheruser;
	private static String anotherpassword;
	static {
		try {
		    metacatUrl = PropertyService.getProperty("test.metacatUrl");
			username = PropertyService.getProperty("test.mcUser");
			password = PropertyService.getProperty("test.mcPassword");
			anotheruser = PropertyService.getProperty("test.mcAnotherUser");
			anotherpassword = PropertyService.getProperty("test.mcAnotherPassword");
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Could not get property in static block: " 
					+ pnfe.getMessage());
		}
	}

    private String prefix = "test";
    private String newdocid = null;
    private String onlineDocid = null;
    private String testdocument = "";
    private String onlinetestdatafile1 = "test/onlineDataFile1";
    private String onlinetestdatafile2 = "test/onlineDataFile2";

    private Metacat m;

    private boolean SUCCESS = true;
    private boolean FAILURE = false;

    /**
     * These variables are for eml-2.0.1 only. For other eml versions,
     * this function might have to modified
     */

    private String testEml_200_Header =
        "<?xml version=\"1.0\"?><eml:eml" +
        " xmlns:eml=\"eml://ecoinformatics.org/eml-2.0.0\"" +
        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
        " packageId=\"eml.1.1\" system=\"knb\"" +
        " xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.0 eml.xsd\"" +
        " scope=\"system\">";
    
    private String testEml_201_Header =
        "<?xml version=\"1.0\"?><eml:eml" +
        " xmlns:eml=\"eml://ecoinformatics.org/eml-2.0.1\"" +
        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
        " packageId=\"eml.1.1\" system=\"knb\"" +
        " xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.1 eml.xsd\"" +
        " scope=\"system\">";
    
    private String testEml_210_Header =
        "<?xml version=\"1.0\"?><eml:eml" +
        " xmlns:eml=\"eml://ecoinformatics.org/eml-2.1.0\"" +
        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
        " packageId=\"eml.1.1\" system=\"knb\"" +
        " xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.1.0 eml.xsd\"" +
        " scope=\"system\">";

    private String testEmlCreatorBlock =
        "<creator scope=\"document\">                                       " +
        " <individualName>                                                  " +
        "    <surName>Smith</surName>                                       " +
        " </individualName>                                                 " +
        "</creator>                                                         ";

    private String testEmlContactBlock =
        "<contact scope=\"document\">                                       " +
        " <individualName>                                                  " +
        "    <surName>Jackson</surName>                                     " +
        " </individualName>                                                 " +
        "</contact>                                                         ";

    private String testEmlInlineBlock1 =
        "<inline>                                                           " +
        "  <admin>                                                          " +
        "    <contact>                                                      " +
        "      <name>Operator</name>                                        " +
        "      <institution>PSI</institution>                               " +
        "    </contact>                                                     " +
        "  </admin>                                                         " +
        "</inline>                                                          ";

    private String testEmlInlineBlock2 =
        "<inline>                                                           " +
        "  <instrument>                                                     " +
        "    <instName>LCQ</instName>                                       " +
        "    <source type=\"ESI\"></source>                                 " +
        "    <detector type=\"EM\"></detector>                              " +
        "  </instrument>                                                    " +
        "</inline>                                                          ";

    /**
     * This function returns an access block based on the params passed
     */
    protected String getAccessBlock(String principal, boolean grantAccess,
                                  boolean read, boolean write,
                                  boolean changePermission, boolean all) {
        String accessBlock = "<access " +
            "authSystem=\"ldap://ldap.ecoinformatics.org:389/dc=ecoinformatics,dc=org\"" +
            " order=\"allowFirst\" scope=\"document\">";

        if (grantAccess) {
            accessBlock += "<allow>";
        }
        else {
            accessBlock += "<deny>";
        }

        accessBlock = accessBlock + "<principal>" + principal + "</principal>";

        if (all) {
            accessBlock += "<permission>all</permission>";
        }
        else {
            if (read) {
                accessBlock += "<permission>read</permission>";
            }
            if (write) {
                accessBlock += "<permission>write</permission>";
            }
            if (changePermission) {
                accessBlock += "<permission>changePermission</permission>";
            }
        }

        if (grantAccess) {
            accessBlock += "</allow>";
        }
        else {
            accessBlock += "</deny>";
        }
        accessBlock += "</access>";

        return accessBlock;

    }

    /**
     * This function returns a valid eml document with no access rules
     * This function is for eml-2.0.1 only. For other eml versions,
     * this function might have to modified
     */
    private String get201TestEmlDoc(String title, String inlineData1,
                                 String inlineData2, String onlineUrl1,
                                 String onlineUrl2, String docAccessBlock,
                                 String inlineAccessBlock1,
                                 String inlineAccessBlock2,
                                 String onlineAccessBlock1,
                                 String onlineAccessBlock2) {

        String testDocument = "";
        testDocument = testDocument + testEml_201_Header +
            "<dataset scope=\"document\"><title>" + title + "</title>" +
            testEmlCreatorBlock;

        if (inlineData1 != null) {
            testDocument = testDocument
                + "<distribution scope=\"document\" id=\"inlineEntity1\">"
                + inlineData1 + "</distribution>";
        }
        if (inlineData2 != null) {
            testDocument = testDocument
                + "<distribution scope=\"document\" id=\"inlineEntity2\">"
                + inlineData2 + "</distribution>";
        }
        if (onlineUrl1 != null) {
            testDocument = testDocument
                + "<distribution scope=\"document\" id=\"onlineEntity1\">"
                + "<online><url function=\"download\">"
                + onlineUrl1 + "</url></online></distribution>";
        }
        if (onlineUrl2 != null) {
            testDocument = testDocument +
                "<distribution scope=\"document\" id=\"onlineEntity2\">"
                + "<online><url function=\"download\">"
                + onlineUrl2 + "</url></online></distribution>";
        }
        testDocument += testEmlContactBlock;

        if (docAccessBlock != null) {
            testDocument += docAccessBlock;
        }

        testDocument += "</dataset>";

        if (inlineAccessBlock1 != null) {
            testDocument += "<additionalMetadata>";
            testDocument += "<describes>inlineEntity1</describes>";
            testDocument += inlineAccessBlock1;
            testDocument += "</additionalMetadata>";
        }

        if (inlineAccessBlock2 != null) {
            testDocument += "<additionalMetadata>";
            testDocument += "<describes>inlineEntity2</describes>";
            testDocument += inlineAccessBlock2;
            testDocument += "</additionalMetadata>";
        }

        if (onlineAccessBlock1 != null) {
            testDocument += "<additionalMetadata>";
            testDocument += "<describes>onlineEntity1</describes>";
            testDocument += onlineAccessBlock1;
            testDocument += "</additionalMetadata>";
        }

        if (onlineAccessBlock2 != null) {
            testDocument += "<additionalMetadata>";
            testDocument += "<describes>onlineEntity2</describes>";
            testDocument += onlineAccessBlock2;
            testDocument += "</additionalMetadata>";
        }

        testDocument += "</eml:eml>";

		debug("Returning following document: " + testDocument);
        return testDocument;
    }
    
    /**
	 * This function returns a valid eml document
	 */
    private String get210TestEmlDoc(String title, String inlineData1,
                                 String inlineData2, String onlineUrl1,
                                 String onlineUrl2, String docAccessBlock,
                                 String inlineAccessBlock1,
                                 String inlineAccessBlock2,
                                 String onlineAccessBlock1,
                                 String onlineAccessBlock2) {

    	String testDocument = testEml_210_Header;
		
		// the document level access block sits at the same level and 
		// before the dataset element.
		if (docAccessBlock != null) {
			testDocument += docAccessBlock;
		}
		
        testDocument += "<dataset scope=\"document\"><title>" + title + "</title>"
				+ testEmlCreatorBlock  + testEmlContactBlock 
				+ "<dataTable>" 
				+ "  <entityName>Test Data</entityName>"
				+ "  <physical>" 
				+ "    <objectName>2.1.0 test physical</objectName>"
				+ "    <size unit=\"bytes\">1</size>"
				+ "    <characterEncoding>ASCII</characterEncoding>"
				+ "    <dataFormat>"
				+ "      <textFormat>"
				+ "        <numHeaderLines>1</numHeaderLines>"
				+ "        <attributeOrientation>column</attributeOrientation>"
				+ "        <simpleDelimited>"
				+ "          <fieldDelimiter>,</fieldDelimiter>"
				+ "        </simpleDelimited>"
				+ "      </textFormat>"
				+ "    </dataFormat>";

		// The inline/online level access block sits at the same level as the 
        // inline element.
		if (inlineData1 != null) {
			testDocument += "<distribution><inline>" + inlineData1 + "</inline>";
			if (inlineAccessBlock1 != null) {
				testDocument += inlineAccessBlock1;
			}
			testDocument += "</distribution>";
		}
		if (inlineData2 != null) {
			testDocument += "<distribution><inline>" + inlineData2 + "</inline>";
			if (inlineAccessBlock2 != null) {
				testDocument += inlineAccessBlock2;
			}
			testDocument += "</distribution>";
		}
		if (onlineUrl1 != null) {
			testDocument = testDocument
					+ "<distribution><online><url function=\"download\">" + onlineUrl1
					+ "</url></online>";
			if (onlineAccessBlock1 != null) {
				testDocument += onlineAccessBlock1;
			}
			testDocument += "</distribution>";
		}
		if (onlineUrl2 != null) {
			testDocument = testDocument
					+ "<distribution><online><url function=\"download\">" + onlineUrl2
					+ "</url></online>";
			if (onlineAccessBlock2 != null) {
				testDocument += onlineAccessBlock2;
			}
			testDocument += "</distribution>";
		}
		testDocument += 
			  "  </physical>" 
			+ "  <attributeList>"
			+ "    <attribute>"
			+ "      <attributeName>rain</attributeName>"
			+ "      <attributeLabel>Surface Rainfall</attributeLabel>"
			+ "      <attributeDefinition>The amount of rainfall on the sampling unit."
			+ "      </attributeDefinition>"
			+ "      <storageType>float</storageType>"
			+ "      <storageType typeSystem=\"http://java.sun.com/docs/books/jls/second_edition/html\">double</storageType>"
			+ "      <measurementScale>"
			+ "        <interval>"
			+ "          <unit><standardUnit>millimeter</standardUnit></unit>"
			+ "          <precision>0.5</precision>"
			+ "          <numericDomain id=\"nd.1\">"
			+ "            <numberType>real</numberType>"
			+ "            <bounds>"
			+ "              <minimum exclusive=\"false\">0</minimum>"
			+ "            </bounds>"
			+ "          </numericDomain>"
			+ "        </interval>"
			+ "      </measurementScale>"
			+ "    </attribute>"
			+ "  </attributeList>"
			+ "</dataTable></dataset></eml:eml>";

		debug("Returning following document: " + testDocument);
		return testDocument;
	}

    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public OnlineDataAccessTest(String name) {
        super(name);
        newdocid = generateDocid();
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() {
        try {
            System.err.println("Test Metacat: " + metacatUrl);
            m = MetacatFactory.createMetacatConnection(metacatUrl);
        }
        catch (MetacatInaccessibleException mie) {
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
        suite.addTest(new OnlineDataAccessTest("initialize"));
        suite.addTest(new OnlineDataAccessTest("onlineData201CasesTest_1"));
        suite.addTest(new OnlineDataAccessTest("onlineData210CasesTest_1"));
        suite.addTest(new OnlineDataAccessTest("onlineData201CasesTest_2"));
        suite.addTest(new OnlineDataAccessTest("onlineData210CasesTest_2"));
        suite.addTest(new OnlineDataAccessTest("onlineData201CasesTest_3"));
        suite.addTest(new OnlineDataAccessTest("onlineData210CasesTest_3"));
        suite.addTest(new OnlineDataAccessTest("onlineData201CasesTest_4"));
        suite.addTest(new OnlineDataAccessTest("onlineData210CasesTest_4"));
        suite.addTest(new OnlineDataAccessTest("onlineData201CasesTest_5"));
        suite.addTest(new OnlineDataAccessTest("onlineData201CasesTest_6"));
        suite.addTest(new OnlineDataAccessTest("onlineData210CasesTest_6"));
        
        suite.addTest(new OnlineDataAccessTest("onlineData210CasesTest_7"));

        return suite;
    }



    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize() {
        assertTrue(1 == 1);
    }


    /**
     * Checking the following cases on 2.0.1 version documents:
     * when only online data is uploaded by a user and
     * -> he tries to read it  - success
     * -> he tries to add same docid again  - failure
     * -> he tries to update it  - success
     * -> he tries to set permissions on it  - success
     * -> he tries to delete it  - success
     * -> he tries to read it after deleteing - failure
     */
    public void onlineData201CasesTest_1() {
        try {
        	debug("\nRunning: onlineData201CasesTest_1");
        	
        	// upload online data
        	onlineDocid = generateDocid();
        	m.login(username, password);
        	debug("\nUpload online data with id: "+ onlineDocid + ".1");
        	uploadDocid(onlineDocid + ".1",
        			onlinetestdatafile1, SUCCESS, false);

        	// try to read the data
        	readDocid(onlineDocid + ".1", SUCCESS, false);

        	// try to upload another data with same id
        	debug("\nUpload of (different) online data with same id: "+ onlineDocid + ".1");
        	uploadDocid(onlineDocid + ".1",
        			onlinetestdatafile2, FAILURE, false);

        	// try to upload another data with updated id
        	debug("\nUpload of data with updated id: "+ onlineDocid + ".2");

        	uploadDocid(onlineDocid + ".2",
        			onlinetestdatafile2, SUCCESS, false);

        	// try to set the permissions for the uploaded document
        	// the docid given is for the online document
        	testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
        			null, null,
        			"ecogrid://knb/" + onlineDocid + ".2",
        			null, getAccessBlock(anotheruser, true,
        					true, false, false, false),
        					null, null, null, null);
        	newdocid = generateDocid();
        	debug("\nInsert of document with id: "+ newdocid + ".1");
        	insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
        	m.logout();

        	// check if the permissions were set properly, permission is per-revision
        	m.login(anotheruser, anotherpassword);
        	readDocid(onlineDocid + ".1", FAILURE, true);
        	readDocid(onlineDocid + ".2", SUCCESS, false);
        	m.logout();

        	m.login(username, password);

        	// delete the document - able to delete .1
        	// but not able to delete .2 as no rules 
        	// written to access table when a document 
        	// is 'uploaded'
        	debug("\nDeleting .1 and .2 version of document with ids: "+ onlineDocid);
        	deleteDocid(onlineDocid + ".1", SUCCESS, false);
        	deleteDocid(onlineDocid + ".2", FAILURE, true);

        	// try to read the documents now
        	readDocid(onlineDocid + ".1", FAILURE, true);
        	readDocid(onlineDocid + ".2", FAILURE, true);

        	m.logout();

        }
        catch (MetacatAuthException mae) {
        	fail("Authorization failed (testLocation:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
        	fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
        	fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Checking the following cases on 2.1.0 version documents:
     * when only online data is uploaded by a user and
     * -> he tries to read it  - success
     * -> he tries to add same docid again  - failure
     * -> he tries to update it  - success
     * -> he tries to set permissions on it  - success
     * -> he tries to delete it  - success
     * -> he tries to read it after deleteing - failure
     */
    public void onlineData210CasesTest_1() {
        try {
			debug("\nRunning: onlineData210CasesTest_1");

            // upload online data
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1",
                        onlinetestdatafile1, SUCCESS, false);

            // try to read the data
            readDocid(onlineDocid + ".1", SUCCESS, false);

            // try to upload another data with same id
            uploadDocid(onlineDocid + ".1",
                        onlinetestdatafile2, FAILURE, false);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, getAccessBlock(anotheruser, true,
                true, false, false, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // check if the permissions were set properly
            m.login(anotheruser, anotherpassword);
            readDocid(onlineDocid + ".1", FAILURE, true);
            readDocid(onlineDocid + ".2", SUCCESS, false);
            m.logout();

            m.login(username, password);

            // delete the document - able to delete .1
            // but not able to delete .2 as no rules 
	        // written to access table when a document 
	        // is 'uploaded'
            deleteDocid(onlineDocid + ".1", SUCCESS, false);
            deleteDocid(onlineDocid + ".2", FAILURE, true);

            // try to read the documents now
            readDocid(onlineDocid + ".1", FAILURE, true);
            readDocid(onlineDocid + ".2", FAILURE, true);

            m.logout();

        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Checking the following cases on 2.0.1 version documents:
     * when only online data is uploaded by a user and another user
     * -> tries to read it  - failure
     * -> tries to add same docid again  - failure
     * -> tries to update it  - failure
     * -> tries to set permissions on it  - failure
     * -> tries to delete it  - failure
     */
    public void onlineData201CasesTest_2() {
        try {
			debug("\nRunning: onlineData201CasesTest_2");

            // upload an online document
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1",
                        onlinetestdatafile1, SUCCESS, false);

            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // login as another user
            m.logout();
            m.login(anotheruser, anotherpassword);

            // try to read the data
            readDocid(onlineDocid + ".2", FAILURE, true);

            // try to upload another document with same id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, FAILURE, false);

            // try to upload another document with updated id
            uploadDocid(onlineDocid + ".3",
                        onlinetestdatafile2, FAILURE, true);


            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         getAccessBlock(anotheruser, true,
                                         true, false, false, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);

            // delete the document - should not be able to delete .1
            // but should be able to delete .2
            deleteDocid(onlineDocid + ".1", FAILURE, true);
            deleteDocid(onlineDocid + ".2", FAILURE, true);

            m.logout();

        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Checking the following cases on 2.1.0 version documents:
     * when only online data is uploaded by a user and another user
     * -> tries to read it  - failure
     * -> tries to add same docid again  - failure
     * -> tries to update it  - failure
     * -> tries to set permissions on it  - failure
     * -> tries to delete it  - failure
     */
    public void onlineData210CasesTest_2() {
        try {
			debug("\nRunning: onlineData201CasesTest_2");

            // upload an online document
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1",
                        onlinetestdatafile1, SUCCESS, false);

            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // login as another user
            m.logout();
            m.login(anotheruser, anotherpassword);

            // try to read the data
            readDocid(onlineDocid + ".2", FAILURE, true);

            // try to upload another document with same id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, FAILURE, false);

            // try to upload another document with updated id
            uploadDocid(onlineDocid + ".3",
                        onlinetestdatafile2, FAILURE, true);


            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         getAccessBlock(anotheruser, true,
                                         true, false, false, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);

            // delete the document - should not be able to delete .1
            // but should be able to delete .2
            deleteDocid(onlineDocid + ".1", FAILURE, true);
            deleteDocid(onlineDocid + ".2", FAILURE, true);

            m.logout();

        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Checking the following cases on 2.0.1 version documents:
     * when only online data is uploaded by a user with the following different
     * access controls in another document
     *   1.read
     *   2.write
     *   3.change permission
     *   4.all
     * And another user tries to do the following:
     * -> tries to read it
     * -> tries to update it
     * -> tries to set permissions on it
     * -> tries to delete it
     */
    public void onlineData201CasesTest_3() {
        try {
			debug("\nRunning: onlineData201CasesTest_3");

            /////////Case 1./////////////////////
            // upload an online document - read only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                true, false, false, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", SUCCESS, false);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, FAILURE, true);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                false, false, false, true),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);

            // delete the document
            deleteDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();

            /////////Case 2/////////////////////
            // upload an online document - write only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Another insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                false, true, false, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", FAILURE, true);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Another insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, getAccessBlock(anotheruser, true,
                                         false, false, false, true),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);

            // delete the document
            deleteDocid(onlineDocid + ".2", FAILURE, true);
            m.logout();

            /////////Case 3/////////////////////
            // upload an online document - change permission only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Another insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                                         false, false, true, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", FAILURE, true);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                          onlinetestdatafile2, FAILURE, true);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                                         false, false, false, true),
                                         null, null, null, null);
            newdocid = generateDocid();
            // ERRRRRRRRRRRRRRRR
            // User does not have permission to update of access rules for data
            // insertDocid(newdocid + ".1", testdocument, SUCCESS, false);

            // delete the document
            deleteDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();

            /////////Case 4/////////////////////
            // upload an online document all
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                                         false, false, false, true),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", SUCCESS, false);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);

            m.logout();
            // delete the document
            deleteDocid(onlineDocid + ".1", FAILURE, false);

            m.logout();

        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Checking the following cases on 2.1.0 version documents:
     * when only online data is uploaded by a user with the following different
     * access controls in another document
     *   1.read
     *   2.write
     *   3.change permission
     *   4.all
     * And another user tries to do the following:
     * -> tries to read it
     * -> tries to update it
     * -> tries to set permissions on it
     * -> tries to delete it
     */
    public void onlineData210CasesTest_3() {
        try {
			debug("\nRunning: onlineData210CasesTest_3");

            /////////Case 1./////////////////////
            // upload an online document - read only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                true, false, false, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", SUCCESS, false);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, FAILURE, true);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, getAccessBlock(anotheruser, true,
                false, false, false, true),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);

            // delete the document
            deleteDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();

            /////////Case 2/////////////////////
            // upload an online document - write only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Another insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                false, true, false, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", FAILURE, true);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Another insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, getAccessBlock(anotheruser, true,
                                         false, false, false, true),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);

            // delete the document
            deleteDocid(onlineDocid + ".2", FAILURE, true);
            m.logout();

            /////////Case 3/////////////////////
            // upload an online document - change permission only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Another insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                                         false, false, true, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", FAILURE, true);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                          onlinetestdatafile2, FAILURE, true);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, getAccessBlock(anotheruser, true,
                                         false, false, false, true),
                                         null, null, null, null);
            newdocid = generateDocid();
            // ERRRRRRRRRRRRRRRR
            // User does not have permission to update of access rules for data
            // insertDocid(newdocid + ".1", testdocument, SUCCESS, false);

            // delete the document
            deleteDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();

            /////////Case 4/////////////////////
            // upload an online document all
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                                         false, false, false, true),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", SUCCESS, false);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false),
                                         null, null, null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);

            m.logout();
            // delete the document
            deleteDocid(onlineDocid + ".1", FAILURE, false);

            m.logout();

        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Checking the following cases on 2.0.1 version documents:
     * when only online data is uploaded by a user with the following different
     * access controls specified in addiotnal metadata in another document
     *   1.read
     *   2.write
     *   3.change permission
     *   4.all
     * And another user tries to do the following:
     * -> tries to read it
     * -> tries to update it
     * -> tries to set permissions on it
     * -> tries to delete it
     */
    public void onlineData201CasesTest_4() {
        try {
			debug("\nRunning: onlineData201CasesTest_4");

//            /////////Case 1./////////////////////
//            // upload an online document - read only
//            onlineDocid = generateDocid();
//            m.login(username, password);
//            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);
//
//            // upload a document which gives read access to the online document
//            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
//                                         null, null,
//                                         "ecogrid://knb/" + onlineDocid + ".1",
//                                         null, null, null, null,
//                                         getAccessBlock(anotheruser, true,
//                                         true, false, false, false), null);
//            newdocid = generateDocid();
//            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
//            m.logout();
//
//            // login as another user
//            m.login(anotheruser, anotherpassword);
//
//            // try to read the online data
//            readDocid(onlineDocid + ".1", SUCCESS, false);
//
//            // try to upload another data with updated id
//            uploadDocid(onlineDocid + ".2",
//                        onlinetestdatafile2, FAILURE, true);
//
//            // try to set the permissions for the uploaded document
//            // the docid given is for the online document
//            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
//                                         null, null,
//                                         "ecogrid://knb/" + onlineDocid + ".1",
//                                         null, null, null, null,
//                                         getAccessBlock(anotheruser, true,
//                                         false, false, false, true), null);
//            newdocid = generateDocid();
//            insertDocid(newdocid + ".1", testdocument, FAILURE, true);
//
//            // delete the document
//            deleteDocid(onlineDocid + ".1", FAILURE, true);
//            m.logout();

            /////////Case 2/////////////////////
            // upload an online document - write only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         false, true, false, false), null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", FAILURE, true);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         false, false, false, true), null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);

            // delete the document
            deleteDocid(onlineDocid + ".2", FAILURE, true);
            m.logout();

//            /////////Case 3/////////////////////
//            // upload an online document - change permission only
//            onlineDocid = generateDocid();
//            m.login(username, password);
//            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);
//
//            // upload a document which gives read access to the online document
//            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
//                                         null, null,
//                                         "ecogrid://knb/" + onlineDocid + ".1",
//                                         null, null, null, null,
//                                         getAccessBlock(anotheruser, true,
//                                         false, false, true, false), null);
//            newdocid = generateDocid();
//            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
//            m.logout();
//
//            // login as another user
//            m.login(anotheruser, anotherpassword);
//
//            // try to read the online data
//            readDocid(onlineDocid + ".1", FAILURE, true);
//
//            // try to upload another data with updated id
//            uploadDocid(onlineDocid + ".2",
//                        onlinetestdatafile2, FAILURE, true);
//
//            // try to set the permissions for the uploaded document
//            // the docid given is for the online document
//            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
//                                         null, null,
//                                         "ecogrid://knb/" + onlineDocid + ".1",
//                                         null, null, null, null,
//                                         getAccessBlock(anotheruser, true,
//                                         false, false, false, true), null);
//            newdocid = generateDocid();
//            // ERRRRRRRRRRRRRRRR
//            // User does not have permission to update of access rules for data
//            // insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
//
//            // delete the document
//            deleteDocid(onlineDocid + ".1", FAILURE, true);
//            m.logout();
//
//            /////////Case 4/////////////////////
//            // upload an online document all
//            onlineDocid = generateDocid();
//            m.login(username, password);
//            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);
//
//            // upload a document which gives read access to the online document
//            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
//                                         null, null,
//                                         "ecogrid://knb/" + onlineDocid + ".1",
//                                         null, null, null, null,
//                                         getAccessBlock(anotheruser, true,
//                                         false, false, false, true), null);
//            newdocid = generateDocid();
//            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
//            m.logout();
//
//            // login as another user
//            m.login(anotheruser, anotherpassword);
//
//            // try to read the online data
//            readDocid(onlineDocid + ".1", SUCCESS, false);
//
//            // try to upload another data with updated id
//            uploadDocid(onlineDocid + ".2",
//                        onlinetestdatafile2, SUCCESS, false);
//
//            // try to set the permissions for the uploaded document
//            // the docid given is for the online document
//            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
//                                         null, null,
//                                         "ecogrid://knb/" + onlineDocid + ".1",
//                                         null, null, null, null,
//                                         getAccessBlock(anotheruser, true,
//                                         true, false, false, false), null);
//            newdocid = generateDocid();
//            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
//
//            m.logout();
//            // delete the document
//            deleteDocid(onlineDocid + ".1", FAILURE, false);
//
//            m.logout();

        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Checking the following cases on 2.1.0 version documents:
     * when only online data is uploaded by a user with the following different
     * access controls specified in addiotnal metadata in another document
     *   1.read
     *   2.write
     *   3.change permission
     *   4.all
     * And another user tries to do the following:
     * -> tries to read it
     * -> tries to update it
     * -> tries to set permissions on it
     * -> tries to delete it
     */
    public void onlineData210CasesTest_4() {
        try {
			debug("\nRunning: onlineData210CasesTest_4");

            /////////Case 1./////////////////////
            // upload an online document - read only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         true, false, false, false), null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", SUCCESS, false);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, FAILURE, true);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         false, false, false, true), null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);

            // delete the document
            deleteDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();

            /////////Case 2/////////////////////
            // upload an online document - write only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         false, true, false, false), null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", FAILURE, true);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         false, false, false, true), null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);

            // delete the document
            deleteDocid(onlineDocid + ".2", FAILURE, true);
            m.logout();

            /////////Case 3/////////////////////
            // upload an online document - change permission only
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         false, false, true, false), null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", FAILURE, true);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, FAILURE, true);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         false, false, false, true), null);
            newdocid = generateDocid();
            // ERRRRRRRRRRRRRRRR
            // User does not have permission to update of access rules for data
            // insertDocid(newdocid + ".1", testdocument, SUCCESS, false);

            // delete the document
            deleteDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();

            /////////Case 4/////////////////////
            // upload an online document all
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            // upload a document which gives read access to the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         false, false, false, true), null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

            // login as another user
            m.login(anotheruser, anotherpassword);

            // try to read the online data
            readDocid(onlineDocid + ".1", SUCCESS, false);

            // try to upload another data with updated id
            uploadDocid(onlineDocid + ".2",
                        onlinetestdatafile2, SUCCESS, false);

            // try to set the permissions for the uploaded document
            // the docid given is for the online document
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".2",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         true, false, false, false), null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);

            m.logout();
            // delete the document
            deleteDocid(onlineDocid + ".1", FAILURE, false);

            m.logout();

        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Checking the following cases on 2.0.1 version documents:
     * -> when online data with document refering to it is uploaded with
     *    rules in additional metadata for an wrong entity id which
     *    doesnt exist
     * -> when online data with document refering to it is uploaded with
     *    rules in additional metadata for an entity which doesnt
     *    exist - wrong url
     */
    public void onlineData201CasesTest_5() {
        try {       	
			debug("\nRunning: onlineData201CasesTest_5");

            // upload online data
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);

            /////////Case 1
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert", null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         true, false, false, false));
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, FAILURE, false);

            /////////Case 2
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert", null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, null, null, null,
                                         getAccessBlock(anotheruser, true,
                                         true, false, false, false), null);
            newdocid = generateDocid();

            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();

        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
   
// MCD - Removed this test.  Access does not use additional metadata in 2.0.1  
//    /**
//     * Checking the following cases on 2.1.0 version documents:
//     * -> when online data with document refering to it is uploaded with
//     *    rules in additional metadata for an wrong entity id which
//     *    doesnt exist
//     * -> when online data with document refering to it is uploaded with
//     *    rules in additional metadata for an entity which doesnt
//     *    exist - wrong url
//     */
//    public void onlineData210CasesTest_5() {
//        try {       	
//			debug("\nRunning: onlineData210CasesTest_5");
//
//            // upload online data
//            onlineDocid = generateDocid();
//            m.login(username, password);
//
//            /////////Case 1
//            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert", null, null,
//                                         "ecogrid://knb/" + onlineDocid + ".1",
//                                         null, null, null, null, null,
//                                         getAccessBlock(anotheruser, true,
//                                         true, false, false, false));
//            newdocid = generateDocid();
//            insertDocid(newdocid + ".1", testdocument, FAILURE, false);
//
//            /////////Case 2
//            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert", null, null,
//                                         "ecogrid://knb/" + onlineDocid + ".1",
//                                         null, null, null, null,
//                                         getAccessBlock(anotheruser, true,
//                                         true, false, false, false), null);
//            newdocid = generateDocid();
//
//            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
//            m.logout();
//
//        }
//        catch (MetacatAuthException mae) {
//            fail("Authorization failed:\n" + mae.getMessage());
//        }
//        catch (MetacatInaccessibleException mie) {
//            fail("Metacat Inaccessible:\n" + mie.getMessage());
//        }
//        catch (Exception e) {
//            fail("General exception:\n" + e.getMessage());
//        }
//    }


    /**
     * Checking the following cases on 2.0.1 version documents:
     * -> when a document is added with no online data - it is updated (has
     *    access)  - then data is added - and a url to it is added and docid
     *    is updated - then the access is updated in document
     *    does it result in rules being applied on the data
     *    (Andrea Chadden was having problem is a similar case)
     * -> when online data with document refering to it is uploaded with read
     *    access for document and no access for docid and vice versa
     */
    public void onlineData201CasesTest_6() {
        try {
			debug("\nRunning: onlineData201CasesTest_6");
            // insert a document
            m.login(username, password);
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null, null,
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false), null, null,
                                         null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();


            // update document
            m.login(username, password);
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing update",
                                         null, null, null,
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false), null, null,
                                         null, null);
            updateDocid(newdocid + ".2", testdocument, SUCCESS, false);
            m.logout();


            // upload data and update the document
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);
            m.logout();

            // try to read the online data
            m.login(anotheruser, anotherpassword);
            readDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();

            // upload data and update the document
            m.login(username, password);
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing update",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false), null, null,
                                         null, null);
            updateDocid(newdocid + ".3", testdocument, SUCCESS, false);
            m.logout();

            // set read for document - no read for data
            m.login(username, password);
            testdocument = get201TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false), null, null,
                                         getAccessBlock(anotheruser, false,
                                         false, false, false, true), null);
            updateDocid(newdocid + ".4", testdocument, SUCCESS, false);
            m.logout();

            // try to read the online data
            m.login(anotheruser, anotherpassword);
            readDocid(newdocid + ".4", SUCCESS, false);
            readDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();


            // set read for document - no read for data
            m.login(username, password);
            testdocument = get201TestEmlDoc("Doing update", null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, false,
                                         false, false, false, true), null, null,
                                         getAccessBlock(anotheruser, true,
                                         true, false, false, false), null);
            updateDocid(newdocid + ".5", testdocument, SUCCESS, false);
            m.logout();

            // try to read the online data
            m.login(anotheruser, anotherpassword);
            readDocid(newdocid + ".5", FAILURE, true);
            readDocid(onlineDocid + ".1", SUCCESS, false);
            m.logout();
        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Checking the following cases on 2.1.0 version documents:
     * -> when a document is added with no online data - it is updated (has
     *    access)  - then data is added - and a url to it is added and docid
     *    is updated - then the access is updated in document
     *    does it result in rules being applied on the data
     *    (Andrea Chadden was having problem is a similar case)
     * -> when online data with document refering to it is uploaded with read
     *    access for document and no access for docid and vice versa
     */
    public void onlineData210CasesTest_6() {
        try {
			debug("\nRunning: onlineData210CasesTest_6");
            // insert a document
            m.login(username, password);
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null, null,
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false), null, null,
                                         null, null);
            newdocid = generateDocid();
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            m.logout();


            // update document
            m.login(username, password);
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing update",
                                         null, null, null,
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false), null, null,
                                         null, null);
            updateDocid(newdocid + ".2", testdocument, SUCCESS, false);
            m.logout();


            // upload data and update the document
            onlineDocid = generateDocid();
            m.login(username, password);
            uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);
            m.logout();

            // try to read the online data
            m.login(anotheruser, anotherpassword);
            readDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();

            // upload data and update the document
            m.login(username, password);
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing update",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false), null, null,
                                         null, null);
            updateDocid(newdocid + ".3", testdocument, SUCCESS, false);
            m.logout();

            // set read for document - no read for data
            m.login(username, password);
            testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
                                         null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, true,
                                         true, false, false, false), null, null,
                                         getAccessBlock(anotheruser, false,
                                         false, false, false, true), null);
            updateDocid(newdocid + ".4", testdocument, SUCCESS, false);
            m.logout();

            // try to read the online data
            m.login(anotheruser, anotherpassword);
            readDocid(newdocid + ".4", SUCCESS, false);
            readDocid(onlineDocid + ".1", FAILURE, true);
            m.logout();


            // set no read for document - read for data
            m.login(username, password);
            testdocument = get210TestEmlDoc("Doing update", null, null,
                                         "ecogrid://knb/" + onlineDocid + ".1",
                                         null, getAccessBlock(anotheruser, false,
                                         false, false, false, true), null, null,
                                         getAccessBlock(anotheruser, true,
                                         true, false, false, false), null);
            updateDocid(newdocid + ".5", testdocument, SUCCESS, false);
            m.logout();

            // try to read the online data
            m.login(anotheruser, anotherpassword);
            readDocid(newdocid + ".5", FAILURE, true);
            readDocid(onlineDocid + ".1", SUCCESS, false);
            m.logout();
        }
        catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }


    /**
     * Insert a document into metacat. The expected result is passed as result
     */

    private String insertDocid(String docid, String docText, boolean result,
                               boolean expectKarmaException) {
        System.out.println("docText being inserted:\n" + docText + "\n");
    	String response = null;
        try {
            response = m.insert(docid,
                                new StringReader(testdocument), null);
            if (result) {
                assertTrue( (response.indexOf("<success>") != -1));
                assertTrue(response.indexOf(docid) != -1);
            }
            else {
                assertTrue( (response.indexOf("<success>") == -1));
            }
            System.err.println(response);
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible (in insertDocid):\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expectKarmaException) {
                fail("Insufficient karma (in insertDocid):\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (result) {
               fail("Metacat Error (in insertDocid):\n" + me.getMessage());
           }
           else {
               System.err.println("Expected Metacat Error (in insertDocid): " + me.getMessage());
           }
        }
        catch (Exception e) {
            fail("General exception (in insertDocid):\n" + e.getMessage());
        }
        return response;
    }

    /**
     * Insert a document into metacat. The expected result is passed as result
     */

    private String uploadDocid(String docid, String filePath, boolean result,
                               boolean expectedKarmaException) {
        String response = null;
        try {
            response = m.upload(docid, new File(filePath));
            if (result) {
                assertTrue( (response.indexOf("<success>") != -1));
                assertTrue(response.indexOf(docid) != -1);
            }
            else {
                assertTrue( (response.indexOf("<success>") == -1));
            }
            System.err.println("response from metacat: " + response);
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible (in uploadDocid):\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expectedKarmaException) {
                fail("Insufficient karma (in uploadDocid):\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error (in uploadDocid):\n" + me.getMessage());
            }
            else {
                System.err.println("Expected Metacat Error (in uploadDocid): " + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception (in uploadDocid):\n" + e.getMessage());
        }
        return response;
    }

    /**
     * Update a document in metacat. The expected result is passed as result
     */
    private String updateDocid(String docid, String docText, boolean result,
                               boolean expectedKarmaFailure) {
        String response = null;
        try {
            response = m.update(docid,
                                new StringReader(testdocument), null);

            if (result) {
                assertTrue( (response.indexOf("<success>") != -1));
                assertTrue(response.indexOf(docid) != -1);
            }
            else {
                assertTrue( (response.indexOf("<success>") == -1));
            }
            System.err.println(response);
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible (in updateDocid):\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expectedKarmaFailure) {
                fail("Insufficient karma (in updateDocid):\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (! (expectedKarmaFailure &&
                   (me.getMessage().indexOf(
                "User tried to update an access module when they don't have \"ALL\" permission") !=
                    -1))) {
                fail("Metacat Error (in updateDocid):\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception (in updateDocid):\n" + e.getMessage());
        }

        return response;
    }

    /**
     * Delete a document into metacat. The expected result is passed as result
     */
    private void deleteDocid(String docid, boolean result,
                             boolean expectedKarmaFailure) {
        try {
            String response = m.delete(docid);
            if (result) {
                assertTrue(response.indexOf("<success>") != -1);
            }
            else {
                assertTrue(response.indexOf("<success>") == -1);
            }
            System.err.println(response);
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expectedKarmaFailure) {
                    fail("Insufficient karma (in deleteDocid):\n" + ike.getMessage());
                }

            }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error (in deleteDocid):\n" + me.getMessage());
            }
            else {
                System.err.println("Expected Metacat Error (in deleteDocid):\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception (in deleteDocid):\n" + e.getMessage());
        }
    }

    /**
     * Read a document from metacat. The expected result is passed as result
     */
    private void readDocid(String docid, boolean result,
                           boolean expextedKarmaFailure) {
        try {
            Reader r = new InputStreamReader(m.read(docid));
            String response = IOUtil.getAsString(r, true);

            if (!result) {
                assertTrue(response.indexOf("<success>") == -1);
            }
            // System.err.println(response);
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible (in readDocid):\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expextedKarmaFailure) {
                fail("Insufficient karma (in readDocid):\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error (in readDocid):\n" + me.getMessage());
            }
            else {
                System.err.println("Expected Metacat Error (in readDocid):\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception (in readDocid):\n" + e.getMessage());
        }
    }

    /**
     * Read a document from metacat and check if it is equal to a given string.
     * The expected result is passed as result
     */

    private void readDocidWhichEqualsDoc(String docid, String testDoc,
                                         boolean result,
                                         boolean expextedKarmaFailure) {
        try {
            Reader r = new InputStreamReader(m.read(docid));
            String doc = IOUtil.getAsString(r, true);
            if (result) {

                if (!testDoc.equals(doc)) {
                    System.out.println("doc    :" + doc);
                    System.out.println("testDoc:" + testDoc);
                }

                assertTrue(testDoc.equals(doc));
            }
            else {
                assertTrue(doc.indexOf("<error>") != -1);
            }
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible (in readDocidWhichEquals...):\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expextedKarmaFailure) {
                fail("Insufficient karma (in readDocidWhichEquals...):\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            fail("Metacat Error (in readDocidWhichEquals...):\n" + me.getMessage());
        }
        catch (Exception e) {
            fail("General exception (in readDocidWhichEquals...):\n" + e.getMessage());
        }

    }

    /**
     * Create a hopefully unique docid for testing insert and update. Does
     * not include the 'revision' part of the id.
     *
     * @return a String docid based on the current date and time
     */
    private String generateDocid() {
        StringBuffer docid = new StringBuffer(prefix);
        docid.append(".");

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs( -8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone( -8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY,
                       2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        docid.append(calendar.get(Calendar.YEAR));
        docid.append(calendar.get(Calendar.DAY_OF_YEAR));
        docid.append(calendar.get(Calendar.HOUR_OF_DAY));
        docid.append(calendar.get(Calendar.MINUTE));
        docid.append(calendar.get(Calendar.SECOND));
   	    //sometimes this number is not unique, so we append a random number
    	int random = (new Double(Math.random()*100)).intValue();
    	docid.append(random);
        
        return docid.toString();
    }

	/**
	 * Checking the following cases on 2.1.0 version documents:
	 * when a data file is uploaded without ALL permissions for other user
	 * and then the package has ALL permissions granted the [data file] should 
	 * not prevent a metadata update 
	 */
	public void onlineData210CasesTest_7() {
	    try {
			debug("\nRunning: onlineData210CasesTest_7");
			
	        // insert a document
	        m.login(username, password);
	        testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
	                                     null, null, null,
	                                     null, getAccessBlock(anotheruser, true,
	                                     true, false, false, false), null, null,
	                                     null, null);
	        newdocid = generateDocid();
	        insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
	        m.logout();
	
	        // update document
	        m.login(username, password);
	        testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing update",
	                                     null, null, null,
	                                     null, getAccessBlock(anotheruser, true,
	                                     true, false, false, false), null, null,
	                                     null, null);
	        updateDocid(newdocid + ".2", testdocument, SUCCESS, false);
	        m.logout();
	
	        // upload data and update the document
	        onlineDocid = generateDocid();
	        m.login(username, password);
	        uploadDocid(onlineDocid + ".1", onlinetestdatafile1, SUCCESS, false);
	        m.logout();
	
	        // try to read the online data
	        m.login(anotheruser, anotherpassword);
	        readDocid(onlineDocid + ".1", FAILURE, true);
	        m.logout();
	
	        // update the document to point at the data
	        m.login(username, password);
	        testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing update",
	                                     null, null,
	                                     "ecogrid://knb/" + onlineDocid + ".1",
	                                     null, null, null, null,
	                                     null, null);
	        updateDocid(newdocid + ".3", testdocument, SUCCESS, false);
	        m.logout();
	
	        // set read for document - nothing for data
	        m.login(username, password);
	        testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing insert",
	                                     null, null,
	                                     "ecogrid://knb/" + onlineDocid + ".1",
	                                     null, getAccessBlock(anotheruser, true,
	                                     true, false, false, false), null, null,
	                                     null, null);
	        updateDocid(newdocid + ".4", testdocument, SUCCESS, false);
	        m.logout();
	
	        // try to read the online data
	        m.login(anotheruser, anotherpassword);
	        readDocid(newdocid + ".4", SUCCESS, false);
	        readDocid(onlineDocid + ".1", FAILURE, true);
	        m.logout();
	        
	        // try to update the package as the other user (expect failure)
	        m.login(anotheruser, anotherpassword);
	        testdocument = get210TestEmlDoc("Doing update as other user", null, null,
                    "ecogrid://knb/" + onlineDocid + ".1",
                    null, getAccessBlock(anotheruser, true,
                    true, false, false, false), null, null,
                    getAccessBlock(anotheruser, true,
                    true, false, false, false), null);
	        updateDocid(newdocid + ".5", testdocument, FAILURE, true);
	        m.logout();
	        
	        // upload updated data
	        m.login(username, password);
	        uploadDocid(onlineDocid + ".2", onlinetestdatafile1, SUCCESS, false);
	        m.logout();
	        
	        // update the document
	        m.login(username, password);
	        testdocument = get210TestEmlDoc("OnlineDataAccessTest: Doing update",
	                                     null, null,
	                                     "ecogrid://knb/" + onlineDocid + ".2",
	                                     null, getAccessBlock(anotheruser, true,
	                                     true, false, false, false), null, null,
	                                     null, null);
	        updateDocid(newdocid + ".5", testdocument, SUCCESS, false);
	        m.logout();
	        
	        // try to update the package as the other user (expect failure)
	        m.login(anotheruser, anotherpassword);
	        testdocument = get210TestEmlDoc("Doing update as other user", null, null,
                    "ecogrid://knb/" + onlineDocid + ".2",
                    null, getAccessBlock(anotheruser, false,
                    false, false, false, true), null, null,
                    getAccessBlock(anotheruser, true,
                    true, true, true, true), null);
	        updateDocid(newdocid + ".6", testdocument, FAILURE, true);
	        m.logout();

	        // set  ALL for data package
	        m.login(username, password);
	        testdocument = get210TestEmlDoc("Doing update for package ALL", null, null,
	                                     "ecogrid://knb/" + onlineDocid + ".2",
	                                     null, getAccessBlock(anotheruser, true,
	                                     true, true, true, true), null, null,
	                                     getAccessBlock(anotheruser, true,
	                                     true, true, true, true), null);
	        updateDocid(newdocid + ".6", testdocument, SUCCESS, false);
	        m.logout();
	        
	        // try to read the online data
	        m.login(anotheruser, anotherpassword);
	        readDocid(newdocid + ".6", SUCCESS, false);
	        readDocid(onlineDocid + ".2", SUCCESS, false);
	        m.logout();
	        
	        // try to update the package as the other user (success)
	        m.login(anotheruser, anotherpassword);
	        testdocument = get210TestEmlDoc("Doing update as other user", null, null,
                    "ecogrid://knb/" + onlineDocid + ".2",
                    null, getAccessBlock(anotheruser, true,
                    true, true, true, true), null, null,
                    getAccessBlock(anotheruser, true,
                    true, true, true, true), null);
	        updateDocid(newdocid + ".7", testdocument, SUCCESS, false);
	        m.logout();
	        
	    }
	    catch (MetacatAuthException mae) {
	        fail("Authorization failed:\n" + mae.getMessage());
	    }
	    catch (MetacatInaccessibleException mie) {
	        fail("Metacat Inaccessible:\n" + mie.getMessage());
	    }
	    catch (Exception e) {
	        fail("General exception:\n" + e.getMessage());
	    }
	}
}
