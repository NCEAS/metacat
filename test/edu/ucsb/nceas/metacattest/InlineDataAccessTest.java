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

/**
 * A JUnit test for testing Access Control for Inline data in Metacat
 */
public class InlineDataAccessTest
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
    private String testdocument = "";

    private Metacat m;

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
        "  <admin>                                                          " +
        "    <contact>                                                      " +
        "      <name>Operator</name>                                        " +
        "      <institution>PSI</institution>                               " +
        "    </contact>                                                     " +
        "  </admin>                                                         ";

    private String testEmlInlineBlock2 =
        "  <instrument>                                                     " +
        "    <instName>LCQ</instName>                                       " +
        "    <source type=\"ESI\"></source>                                 " +
        "    <detector type=\"EM\"></detector>                              " +
        "  </instrument>                                                    ";


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
	 * This function returns a valid eml 2.0 1 document
	 */
	private String get201TestEmlDoc(String title, String inlineData1, String inlineData2,
			String onlineUrl1, String onlineUrl2, String docAccessBlock,
			String inlineAccessBlock1, String inlineAccessBlock2,
			String onlineAccessBlock1, String onlineAccessBlock2) {

		String testDocument = testEml_201_Header;

		testDocument += "<dataset scope=\"document\"><title>" + title + "</title>"
				+ testEmlCreatorBlock;

		// The inline/online level access block sits at the same level as the 
		// inline element.
		if (inlineData1 != null) {
			testDocument += "<distribution scope=\"document\" id=\"inlineEntity1\">"
					+ "<inline>" + inlineData1 + "</inline></distribution>";
		}
		if (inlineData2 != null) {
			testDocument = testDocument
					+ "<distribution scope=\"document\" id=\"inlineEntity2\">"
					+ "<inline>" + inlineData2 + "</inline></distribution>";
		}
		if (onlineUrl1 != null) {
			testDocument = testDocument
					+ "<distribution scope=\"document\" id=\"InlineEntity1\">"
					+ "<inline><url function=\"download\">" + onlineUrl1
					+ "</url></inline></distribution>";
		}
		if (onlineUrl2 != null) {
			testDocument = testDocument
					+ "<distribution scope=\"document\" id=\"InlineEntity2\">"
					+ "<inline><url function=\"download\">" + onlineUrl2
					+ "</url></inline></distribution>";
		}
		testDocument += testEmlContactBlock;
		
		// The document level access block sits inside the dataset element.
		if (docAccessBlock != null) {
			testDocument += docAccessBlock;
		}

		testDocument += "</dataset>";

		// The inline access blocks live in additionalMetadata elements at 
		// the same level as the dataset.
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
			testDocument += "<describes>InlineEntity1</describes>";
			testDocument += onlineAccessBlock1;
			testDocument += "</additionalMetadata>";
		}

		if (onlineAccessBlock2 != null) {
			testDocument += "<additionalMetadata>";
			testDocument += "<describes>InlineEntity2</describes>";
			testDocument += onlineAccessBlock2;
			testDocument += "</additionalMetadata>";
		}

		testDocument += "</eml:eml>";

		debug("get201TestEmlDoc returning following document: " + testDocument);
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
					+ "<distribution><inline><url function=\"download\">" + onlineUrl1
					+ "</url></inline>";
			if (onlineAccessBlock1 != null) {
				testDocument += onlineAccessBlock1;
			}
			testDocument += "</distribution>";
		}
		if (onlineUrl2 != null) {
			testDocument = testDocument
					+ "<distribution><inline><url function=\"download\">" + onlineUrl2
					+ "</url></inline>";
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

		debug("get210TestEmlDoc returning following document: " + testDocument);
		return testDocument;
	}

    /**
	 * Constructor to build the test
	 * 
	 * @param name
	 *            the name of the test method
	 */
    public InlineDataAccessTest(String name) {
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
        suite.addTest(new InlineDataAccessTest("initialize"));

        suite.addTest(new InlineDataAccessTest("inlineData201CasesTest_1"));
        suite.addTest(new InlineDataAccessTest("inlineData210CasesTest_1"));
        suite.addTest(new InlineDataAccessTest("inlineData201CasesTest_2"));
        suite.addTest(new InlineDataAccessTest("inlineData210CasesTest_2"));
        suite.addTest(new InlineDataAccessTest("inlineData201CasesTest_3"));
        suite.addTest(new InlineDataAccessTest("inlineData210CasesTest_3"));
        suite.addTest(new InlineDataAccessTest("inlineData201CasesTest_4"));
        suite.addTest(new InlineDataAccessTest("inlineData210CasesTest_4"));
        suite.addTest(new InlineDataAccessTest("inlineData201CasesTest_5"));
        suite.addTest(new InlineDataAccessTest("inlineData210CasesTest_5"));
        suite.addTest(new InlineDataAccessTest("inlineData201CasesTest_6"));
        suite.addTest(new InlineDataAccessTest("inlineData210CasesTest_6"));

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
     * For EML 2.0.1, checking the following cases:
     * when only Inline data is uploaded by a user and
     * -> he tries to read it  - success
     * -> he tries to add same docid again  - failure
     * -> he tries to update it  - success
     * -> he removes it and adds it again - success
     * -> he tries to delete it  - success
     * -> he tries to read it after deleteing - failure
     */
    public void inlineData201CasesTest_1() {
		debug("\nRunning: inlineData201CasesTest_1()");
		try {
			newdocid = generateDocid();
			m.login(username, password);

			// insert a document
			testdocument = get201TestEmlDoc("Testing insert", testEmlInlineBlock1,
					null, null, null, null, null, null, null, null);

			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);

			// insert same document again
			insertDocid(newdocid + ".1", testdocument, FAILURE, false);

			// update by changing inline data
			testdocument = get201TestEmlDoc("Testing update inline",
					testEmlInlineBlock2, null, null, null, null, null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".2", testdocument, SUCCESS, true);

			// update by removing inline data
			testdocument = get201TestEmlDoc("Testing update inline", null, null,
					null, null, null, null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".3", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".3", testdocument, SUCCESS, true);

			// update by introducing inline data
			testdocument = get201TestEmlDoc("Testing update inline",
					testEmlInlineBlock1, null, null, null, null, null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".4", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".4", testdocument, SUCCESS, true);

			// read inline data only
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, SUCCESS,
					false);

			// delete the inline data
			// sleep needed only in case of inline data - jing said that
			// somehow the thread writing data to xml_index takes too much time
			// when used with inline data. hence if delete is requested too soon
			// database gives an error of xml_index records left with FK to
			// xml_document record which is going to be deleted.
			Thread.sleep(10000);

			deleteDocid(newdocid + ".4", SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".4", testdocument, SUCCESS, false);
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
	 * For EML 2.1.0, checking the following cases: when only Inline data is
	 * uploaded by a user and -> he tries to read it - success -> he tries to
	 * add same docid again - failure -> he tries to update it - success -> he
	 * removes it and adds it again - success -> he tries to delete it - success ->
	 * he tries to read it after deleteing - failure
	 */
    public void inlineData210CasesTest_1() {
		debug("\nRunning: inlineData210CasesTest_1()");
		try {
			newdocid = generateDocid();
			m.login(username, password);

			// insert a document
			testdocument = get210TestEmlDoc("Testing insert", testEmlInlineBlock1,
					null, null, null, null, null, null, null, null);

			debug("testdocument: " + testdocument);
			
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);

			// insert same document again
			insertDocid(newdocid + ".1", testdocument, FAILURE, false);

			// update by changing inline data
			testdocument = get210TestEmlDoc("Testing update inline",
					testEmlInlineBlock2, null, null, null, null, null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".2", testdocument, SUCCESS, true);

			// update by removing inline data
			testdocument = get210TestEmlDoc("Testing update inline", null, null,
					null, null, null, null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".3", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".3", testdocument, SUCCESS, true);

			// update by introducing inline data
			testdocument = get210TestEmlDoc("Testing update inline",
					testEmlInlineBlock1, null, null, null, null, null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".4", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".4", testdocument, SUCCESS, true);

			// read inline data only
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, SUCCESS,
					false);

			// delete the inline data
			// sleep needed only in case of inline data - jing said that
			// somehow the thread writing data to xml_index takes too much time
			// when used with inline data. hence if delete is requested too soon
			// database gives an error of xml_index records left with FK to
			// xml_document record which is going to be deleted.
			Thread.sleep(10000);

			deleteDocid(newdocid + ".4", SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".4", testdocument, SUCCESS, false);
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
	 * For EML 2.0.1, checking the following cases: when only inline data is 
	 * uploaded by a user and another user -> tries to read it - failure -> 
	 * tries to read inline data only - failure -> tries to update - failure -> 
	 * tries to delete it - failure
	 */
    public void inlineData201CasesTest_2() {
		debug("\nRunning: inlineData201CasesTest_2()");
		try {
			newdocid = generateDocid();
			m.login(username, password);

			// insert a document
			testdocument = get201TestEmlDoc("Testing insert", testEmlInlineBlock1,
					null, null, null, null, null, null, null, null);

			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);

			// login as another user
			m.logout();
			m.login(anotheruser, anotherpassword);

			// try to read document or data only
			readDocidWhichEqualsDoc(newdocid, testdocument, FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the document
			testdocument = get201TestEmlDoc("Testing insert", testEmlInlineBlock1,
					null, null, null, null, null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".1", SUCCESS, false);
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
	 * For EML 2.1.0, checking the following cases: when only inline data is 
	 * uploaded by a user and another user -> tries to read it - failure -> 
	 * tries to read inline data only - failure -> tries to update - failure -> 
	 * tries to delete it - failure
	 */
    public void inlineData210CasesTest_2() {
		debug("\nRunning: inlineData210CasesTest_2()");
		try {
			newdocid = generateDocid();
			m.login(username, password);

			// insert a document
			testdocument = get210TestEmlDoc("Testing insert", testEmlInlineBlock1,
					null, null, null, null, null, null, null, null);

			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);

			// login as another user
			m.logout();
			m.login(anotheruser, anotherpassword);

			// try to read document or data only
			readDocidWhichEqualsDoc(newdocid, testdocument, FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the document
			testdocument = get210TestEmlDoc("Testing insert", testEmlInlineBlock1,
					null, null, null, null, null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".1", SUCCESS, false);
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
	 * For EML 2.0.1, checking the following cases: 
	 * when only inline data is uploaded by a user with the following different 
	 * access controls in another document 1.read 2.write 3.change permission 
	 * 4.all And another user tries to do the following: -> tries to read it -> 
	 * tries to update it -> tries to set permissions on it -> tries to delete it
	 */
    public void inlineData201CasesTest_3() {
		debug("\nRunning: inlineData201CasesTest_3()");
		try {

			// ///////Case 1./////////////////////
			// insert an inline document - read only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, true, false, false, false), null, null, null, null);
			
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", SUCCESS, false);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, SUCCESS,
					false);

			// try to update the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, getAccessBlock(anotheruser,
							true, true, false, false, false), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to set the permissions for the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".1", SUCCESS, false);
			m.logout();

			// ///////Case 2./////////////////////
			// insert an inline document - write only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, true, false, false), null, null, null, null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, getAccessBlock(anotheruser,
							true, false, true, false, false), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".3", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".2", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".2", SUCCESS, false);
			m.logout();

			// ///////Case 3./////////////////////
			// insert an inline document - change permission only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, true, false), null, null, null, null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, SUCCESS,
					false);

			// try to update the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, getAccessBlock(anotheruser,
							true, false, false, true, false), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			// ERRRRRRRRRRRR
			// updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to delete the document
			//deleteDocid(newdocid + ".2", FAILURE, true);
			//m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".2", SUCCESS, false);
			m.logout();

			// ///////Case 4./////////////////////
			// insert an inline document - change permission only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", SUCCESS, false);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, SUCCESS,
					false);

			// try to update the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, true, true, true, false), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".3", testdocument, SUCCESS, false);

			// try to delete the document
			// sleep needed only in case of inline data - jing said that
			// somehow the thread writing data to xml_index takes too much time
			// when used with inline data. hence if delete is requested too soon
			// database gives an error of xml_index records left with FK to
			// xml_document record which is going to be deleted.
			Thread.sleep(10000);

			deleteDocid(newdocid + ".3", SUCCESS, false);
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
	 * For EML 2.1.0, checking the following cases: 
	 * when only inline data is uploaded by a user with the following different 
	 * access controls in another document 1.read 2.write 3.change permission 
	 * 4.all And another user tries to do the following: -> tries to read it -> 
	 * tries to update it -> tries to set permissions on it -> tries to delete it
	 */
    public void inlineData210CasesTest_3() {
		debug("\nRunning: inlineData210CasesTest_3()");
		try {

			// ///////Case 1./////////////////////
			// insert an inline document - read only
			debug("Case 1:");
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, true, false, false, false), null, null, null, null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", SUCCESS, false);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, SUCCESS,
					false);

			// try to update the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, getAccessBlock(anotheruser,
							true, true, false, false, false), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to set the permissions for the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".1", SUCCESS, false);
			m.logout();

			// ///////Case 2./////////////////////
			// insert an inline document - write only
			debug("Case 2:");
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			debug("Inserting doc: " + newdocid + ".1, which allows document level" 
					+ " write access for " + anotheruser + " and expect SUCCESS");
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, true, false, false), null, null, null, null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			debug("Try to read:" + newdocid + ".1 as user: " + anotheruser 
					+ " and expect FAILURE since write access does not imply read access");
			readDocid(newdocid + ".1", FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the inline data
			debug("Updating doc: " + newdocid + ".2, with new inline data." 
					+ " as: " + anotheruser + " and expect SUCCESS");
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, getAccessBlock(anotheruser,
							true, false, true, false, false), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".3", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".2", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".2", SUCCESS, false);
			m.logout();

			// ///////Case 3./////////////////////
			// insert an inline document - change permission only
			debug("Case 3:");
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, true, false), null, null, null, null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, getAccessBlock(anotheruser,
							true, false, false, true, false), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			// ERRRRRRRRRRRR
			// updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to delete the document
			//deleteDocid(newdocid + ".2", FAILURE, true);
			//m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".2", SUCCESS, false);
			m.logout();

			// ///////Case 4./////////////////////
			// insert an inline document - change permission only
			debug("Case 4:");
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", SUCCESS, false);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, SUCCESS,
					false);

			// try to update the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, true, true, true, false), null, null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".3", testdocument, SUCCESS, false);

			// try to delete the document
			// sleep needed only in case of inline data - jing said that
			// somehow the thread writing data to xml_index takes too much time
			// when used with inline data. hence if delete is requested too soon
			// database gives an error of xml_index records left with FK to
			// xml_document record which is going to be deleted.
			Thread.sleep(10000);

			deleteDocid(newdocid + ".3", SUCCESS, false);
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
	 * For EML 2.0.1, checking the following cases: when only Inline data is 
	 * uploaded by a user with the following different access controls specified 
	 * in addiotnal metadata in another document 1.read 2.write 3.change permission 
	 * 4.all And another user tries to do the following: -> tries to read it -> 
	 * tries to update it -> tries to set permissions on it -> tries to delete it
	 */
    public void inlineData201CasesTest_4() {
		debug("\nRunning: inlineData201CasesTest_4()");
		try {
			// ///////Case 1./////////////////////
			// insert an inline document - read only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, true, false, false, false), null, null,
					null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			// ERRRRRRRRRRRRRRR
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, null, getAccessBlock(
							anotheruser, true, true, false, false, false), null, null,
					null);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to set the permissions for the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			 try
	      {
	        Thread.sleep(5000);
	      }
	      catch(Exception e)
	      {
	        
	      }
			deleteDocid(newdocid + ".1", SUCCESS, false);
			m.logout();

			// ///////Case 2./////////////////////
			// insert an inline document - write only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, true, false, false), null, null,
					null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, null, getAccessBlock(
							anotheruser, true, false, true, false, false), null, null,
					null);
			// ERRRRRRRRRRRRRRRRRRRRRRRR
			// updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			try
			{
			  Thread.sleep(5000);
			}
			catch(Exception e)
			{
			  
			}
			deleteDocid(newdocid + ".1", SUCCESS, false);
			m.logout();

			// ///////Case 3./////////////////////
			// insert an inline document - change permission only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, true, false), null, null,
					null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, true, false), null, null,
					null);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to set the permissions for the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			// ERRRRRRRRRRRR
			// updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			try
      {
        Thread.sleep(5000);
      }
      catch(Exception e)
      {
        
      }
			deleteDocid(newdocid + ".1", SUCCESS, false);
			m.logout();

			// ///////Case 4./////////////////////
			// insert an inline document - change permission only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			// ERRRRRRRRRRRRRRR
			// readInlineDataWhichEqualsDoc(newdocid + ".1.1",
			// testEmlInlineBlock1, SUCCESS, false);

			// try to update the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			// ERRRRRR
			// updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, true, true, true, false), null, null, null);
			// ERRRRRRRRRR
			// updateDocid(newdocid + ".3", testdocument, SUCCESS, false);

			// try to delete the document
			deleteDocid(newdocid + ".3", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			try
      {
        Thread.sleep(5000);
      }
      catch(Exception e)
      {
        
      }
			deleteDocid(newdocid + ".1", SUCCESS, false);
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
	 * For EML 2.1.0, checking the following cases: when only Inline data is 
	 * uploaded by a user with the following different access controls specified 
	 * in addiotnal metadata in another document 1.read 2.write 3.change permission 
	 * 4.all And another user tries to do the following: -> tries to read it -> 
	 * tries to update it -> tries to set permissions on it -> tries to delete it
	 */
    public void inlineData210CasesTest_4() {
		debug("\nRunning: inlineData210CasesTest_4()");
		try {
			// ///////Case 1./////////////////////
			// insert an inline document - read only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, true, false, false, false), null, null,
					null);
			debug("testdocument: " + testdocument);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			// ERRRRRRRRRRRRRRR
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, null, getAccessBlock(
							anotheruser, true, true, false, false, false), null, null,
					null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to set the permissions for the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".1", SUCCESS, false);
			m.logout();

			// ///////Case 2./////////////////////
			// insert an inline document - write only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, true, false, false), null, null,
					null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, null, getAccessBlock(
							anotheruser, true, false, true, false, false), null, null,
					null);
			// ERRRRRRRRRRRRRRRRRRRRRRRR
			// updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			deleteDocid(newdocid + ".1", SUCCESS, false);
			m.logout();

			// ///////Case 3./////////////////////
			// insert an inline document - change permission only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, true, false), null, null,
					null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

			// try to update the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, true, false), null, null,
					null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, FAILURE, true);

			// try to set the permissions for the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			// ERRRRRRRRRRRR
			// updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to delete the document
			deleteDocid(newdocid + ".1", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			try
      {
        Thread.sleep(5000);
      }
      catch(Exception e)
      {
        
      }
			deleteDocid(newdocid + ".1", SUCCESS, false);
			m.logout();

			// ///////Case 4./////////////////////
			// insert an inline document - change permission only
			m.login(username, password);
			newdocid = generateDocid();

			// insert a document which gives read access to the inline document
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// login as another user
			m.login(anotheruser, anotherpassword);

			// try to read the document and the inline data
			readDocid(newdocid + ".1", FAILURE, true);
			// ERRRRRRRRRRRRRRR
			// readInlineDataWhichEqualsDoc(newdocid + ".1.1",
			// testEmlInlineBlock1, SUCCESS, false);

			// try to update the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, null, null, null, null, getAccessBlock(
							anotheruser, true, false, false, false, true), null, null,
					null);
			// ERRRRRR
			// updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try to set the permissions for the inline data
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, null, getAccessBlock(
							anotheruser, true, true, true, true, false), null, null, null);
			// ERRRRRRRRRR
			// updateDocid(newdocid + ".3", testdocument, SUCCESS, false);

			// try to delete the document
			deleteDocid(newdocid + ".3", FAILURE, true);
			m.logout();

			// delete the document
			m.login(username, password);
			try
      {
        Thread.sleep(5000);
      }
      catch(Exception e)
      {
        
      }
			deleteDocid(newdocid + ".1", SUCCESS, false);
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
	 * For EML 2.0.1, checking the following cases: -> when no inline data is 
	 * specified in the document but rules are specified in additional metadata -> 
	 * when a user has RW permission for inline data, can he delete it -> when 
	 * inline data with document refering to it is uploaded with read access for 
	 * metadata and no access for data
	 */
    public void inlineData201CasesTest_5() {
		debug("\nRunning: inlineData201CasesTest_5()");
		try {

			m.login(username, password);

			/////////Case 1
			debug("Case 1:");
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					null, null, null, null, null, getAccessBlock(anotheruser, true, true,
							false, false, false), null, null, null);
			newdocid = generateDocid();

			// try to insert the wrong document.  This document is wrong because
			// it has an access block but no distribution.
			insertDocid(newdocid + ".1", testdocument, FAILURE, false);
			m.logout();

			/////////Case 2
			debug("Case 2:");
			m.login(username, password);
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, true, true, false, false), null, null, null, null);
			newdocid = generateDocid();
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			m.login(anotheruser, anotherpassword);
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					null, null, null, null, getAccessBlock(anotheruser, true, true, true,
							false, false), null, null, null, null);
			/// ERRRRRRRRRRRR
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			/////////Case 3
			debug("Case 3:");
			// insert a document
			m.login(username, password);
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), getAccessBlock(anotheruser,
							false, false, false, false, true), null, null, null);
			newdocid = generateDocid();
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// try to read the Inline data
			m.login(anotheruser, anotherpassword);
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					"", null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), getAccessBlock(anotheruser, false, false,
							false, false, true), null, null, null);
			readDocidWhichEqualsDoc(newdocid + ".1", testdocument, SUCCESS, false);

			// try to update the rules for inline data
			// / ERRRRRRRRRRRR it lets you do that
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".2", testdocument, SUCCESS, false);

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
	 * For EML 2.1.0, checking the following cases: -> when no inline data is 
	 * specified in the document but rules are specified in additional metadata -> 
	 * when a user has RW permission for inline data, can he delete it -> when 
	 * inline data with document refering to it is uploaded with read access for 
	 * metadata and no access for data
	 */
    public void inlineData210CasesTest_5() {
		debug("\nRunning: inlineData210CasesTest_5()");
		try {

			/////////Case 2
			debug("Case 2:");
			m.login(username, password);
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, true, true, false, false), null, null, null, null);
			newdocid = generateDocid();
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			m.login(anotheruser, anotherpassword);
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					null, null, null, null, getAccessBlock(anotheruser, true, true, true,
							false, false), null, null, null, null);
			/// ERRRRRRRRRRRR
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			/////////Case 3
			debug("Case 3:");
			// insert a document
			m.login(username, password);
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), getAccessBlock(anotheruser,
							false, false, false, false, true), null, null, null);
			newdocid = generateDocid();
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
			m.logout();

			// try to read the Inline data
			m.login(anotheruser, anotherpassword);
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					"", null, null, null, getAccessBlock(anotheruser, true, false, false,
							false, true), getAccessBlock(anotheruser, false, false,
							false, false, true), null, null, null);
			readDocidWhichEqualsDoc(newdocid + ".1", testdocument, SUCCESS, false);

			// try to update the rules for inline data
			// shouldn't succeed
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, null, null, null, getAccessBlock(anotheruser,
							true, false, false, false, true), getAccessBlock(anotheruser,
							true, false, false, false, true), null, null, null);
			debug("inserting this: " + testdocument);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);
			readDocidWhichEqualsDoc(newdocid + ".2", testdocument, SUCCESS, false);

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
	 * for EML 2.0.1, checking the following cases: -> when inline data is 
	 * inserted and updated, do access rules apply to the old document
	 */
      public void inlineData201CasesTest_6() {
		debug("\nRunning: inlineData201CasesTest_6()");
		try {

			// insert the document
			m.login(username, password);
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, testEmlInlineBlock2, null, null, getAccessBlock(
							anotheruser, true, true, true, false, false), getAccessBlock(
							anotheruser, false, false, false, false, true),
					getAccessBlock(anotheruser, false, false, false, false, true), null,
					null);
			newdocid = generateDocid();
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);

			m.logout();

			// update the document
			m.login(anotheruser, anotherpassword);
			testdocument = get201TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, testEmlInlineBlock1, null, null, getAccessBlock(
							anotheruser, true, true, true, false, false), getAccessBlock(
							anotheruser, false, false, false, false, true),
					getAccessBlock(anotheruser, false, false, false, false, true), null,
					null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try reading the inline document
			readInlineDataWhichEqualsDoc(newdocid + ".2.1", testEmlInlineBlock1, FAILURE,
					true);

			System.out.print("Trying to read " + newdocid + ".1.1");
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

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
	 * for EML 2.1.0, checking the following cases: -> when inline data is
	 * inserted and updated, do access rules apply to the old document
	 */
	public void inlineData210CasesTest_6() {
		debug("\nRunning: inlineData210CasesTest_6()");
		try {

			// insert the document
			m.login(username, password);
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing insert",
					testEmlInlineBlock1, testEmlInlineBlock2, null, null, getAccessBlock(
							anotheruser, true, true, true, false, false), getAccessBlock(
							anotheruser, false, false, false, false, true),
					getAccessBlock(anotheruser, false, false, false, false, true), null,
					null);
			newdocid = generateDocid();
			insertDocid(newdocid + ".1", testdocument, SUCCESS, false);

			m.logout();

			// update the document
			m.login(anotheruser, anotherpassword);
			testdocument = get210TestEmlDoc("InlineDataAccessTest: Doing update",
					testEmlInlineBlock2, testEmlInlineBlock1, null, null, getAccessBlock(
							anotheruser, true, true, true, false, false), getAccessBlock(
							anotheruser, false, false, false, false, true),
					getAccessBlock(anotheruser, false, false, false, false, true), null,
					null);
			Thread.sleep(2000);
			updateDocid(newdocid + ".2", testdocument, SUCCESS, false);

			// try reading the inline document
			readInlineDataWhichEqualsDoc(newdocid + ".2.1", testEmlInlineBlock1, FAILURE,
					true);

			System.out.print("Trying to read " + newdocid + ".1.1");
			readInlineDataWhichEqualsDoc(newdocid + ".1.1", testEmlInlineBlock1, FAILURE,
					true);

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
	 * Insert a document into metacat. The expected result is passed as result
	 */

    private String insertDocid(String docid, String docText, boolean result,
                               boolean expectKarmaException) {
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
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expectKarmaException) {
                fail("Insufficient karma:\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error:\n" + me.getMessage());
            }
            else {
                System.err.println("Metacat Error: " + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
        return response;
    }

    /**
     * Insert a document into metacat. The expected result is passed as result
     */

//    private String uploadDocid(String docid, String filePath, boolean result,
//                               boolean expectedKarmaException) {
//        String response = null;
//        try {
//            response = m.upload(docid, new File(filePath));
//            if (result) {
//                assertTrue( (response.indexOf("<success>") != -1));
//                assertTrue(response.indexOf(docid) != -1);
//            }
//            else {
//                assertTrue( (response.indexOf("<success>") == -1));
//            }
//            System.err.println("respose from metacat: " + response);
//        }
//        catch (MetacatInaccessibleException mie) {
//            fail("Metacat Inaccessible:\n" + mie.getMessage());
//        }
//        catch (InsufficientKarmaException ike) {
//            if (!expectedKarmaException) {
//                fail("Insufficient karma:\n" + ike.getMessage());
//            }
//        }
//        catch (MetacatException me) {
//            if (result) {
//                fail("Metacat Error:\n" + me.getMessage());
//            }
//            else {
//                System.err.println("Metacat Error: " + me.getMessage());
//            }
//        }
//        catch (Exception e) {
//            fail("General exception:\n" + e.getMessage());
//        }
//        return response;
//    }

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
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expectedKarmaFailure) {
                fail("Insufficient karma:\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (! (expectedKarmaFailure &&
                   (me.getMessage().indexOf(
                "User tried to update an access module when they don't have \"ALL\" permission") !=
                    -1))) {
                fail("Metacat Error:\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
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
                fail("Insufficient karma:\n" + ike.getMessage());
            }

        }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error:\n" + me.getMessage());
            }
            else {
                System.err.println("Metacat Error:\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Read inline data from metacat. The expected result is passed as result
     */
    private void readInlineDataWhichEqualsDoc(String docid, String testDoc,
                                              boolean result,
                                              boolean expextedKarmaFailure) {
        try {
        	debug("before read, docid: " + docid);
            Reader r = new InputStreamReader(m.readInlineData(docid));
        	debug("after read, docid: " + docid);
            String doc = IOUtil.getAsString(r, true);
        	debug("after get as string, doc: " + doc);

            if (result) {
                if (!testDoc.equals(doc)) {
                    System.out.println("doc    :" + doc);
                    System.out.println("testDoc:" + testDoc);
                }

                assertTrue(testDoc.equals(doc));
            }
            else {
                System.out.println("doc    :" + doc);
                System.out.println("testDoc:" + testDoc);
                assertTrue(doc.indexOf("<error>") != -1);
            }
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expextedKarmaFailure) {
                fail("Insufficient karma:\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error:\n" + me.getMessage());
            }
            else {
                System.err.println("Metacat Error:\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
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
            System.err.println(response);
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expextedKarmaFailure) {
                fail("Insufficient karma:\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error:\n" + me.getMessage());
            }
            else {
                System.err.println("Metacat Error:\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
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
					System.out.println("doc ***********************");
					System.out.println(doc);
					System.out.println("end doc ***********************");
					System.out.println("testDoc ***********************");
					System.out.println(testDoc);
					System.out.println("end testDoc ***********************");
                }

                assertTrue(testDoc.equals(doc));
            }
            else {
                assertTrue(doc.indexOf("<error>") != -1);
            }
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
        	if (result) {
                fail("Metacat Error:\n" + ike.getMessage());
            }
            else {
                System.out.println("Metacat Error:\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error:\n" + me.getMessage());
            }
            else {
                System.out.println("Metacat Error:\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
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

        try {
        Thread.sleep(1100);
        } catch (InterruptedException ie) {
        	System.out.println("Warning: generateDocid() could not sleep 1100 ms");
        }
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

        return docid.toString();
    }
}
