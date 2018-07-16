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

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.IOUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A JUnit test for testing Metacat when Non Ascii Characters are inserted
 */
public class NonAsciiCharacterTest
    extends MCTestCase {

	static {
		try {
			username = PropertyService.getProperty("test.mcUser");
			password = PropertyService.getProperty("test.mcPassword");
			anotheruser = PropertyService.getProperty("test.mcAnotherUser");
			anotherpassword = PropertyService.getProperty("test.mcAnotherPassword");
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Could not get property in static block: " 
					+ pnfe.getMessage());
		}
	}

    /**
     * These variables are for eml-2.0.1 only. For other eml versions,
     * this function might have to modified
     */

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
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public NonAsciiCharacterTest(String name) {
        super(name);
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
        suite.addTest(new NonAsciiCharacterTest("initialize"));
        // Test basic functions
        suite.addTest(new NonAsciiCharacterTest("invalidXMLCharacters201Test"));
        suite.addTest(new NonAsciiCharacterTest("invalidXMLCharacters210Test"));
        suite.addTest(new NonAsciiCharacterTest("symbolEncodedFormat201Test"));
        suite.addTest(new NonAsciiCharacterTest("symbolEncodedFormat210Test"));
        suite.addTest(new NonAsciiCharacterTest("quote201Test"));
        suite.addTest(new NonAsciiCharacterTest("quote210Test"));
        suite.addTest(new NonAsciiCharacterTest("numericCharacterReferenceFormat201Test"));
        suite.addTest(new NonAsciiCharacterTest("numericCharacterReferenceFormat210Test"));
        suite.addTest(new NonAsciiCharacterTest("nonLatinUnicodeCharacter201Test"));
        suite.addTest(new NonAsciiCharacterTest("nonLatinUnicodeCharacter210Test"));

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
     * Test inserting an EML 2.0.1 document with > & <
     * should fail because this means an invalid xml document is being inserted
     */
    public void invalidXMLCharacters201Test() {
    	debug("\nRunning: invalidXMLCharacters201Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            testdocument = getTestEmlDoc("Checking > & < in doc: " + newdocid, EML2_0_1);
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);
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
     * Test inserting an EML 2.1.0 document with > & <
     * should fail because this means an invalid xml document is being inserted
     */
    public void invalidXMLCharacters210Test() {
    	debug("\nRunning: invalidXMLCharacters210Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            testdocument = getTestEmlDoc("Checking > & < in doc: " + newdocid, EML2_1_0);
            insertDocid(newdocid + ".1", testdocument, FAILURE, true);
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
     * Test inserting and reading an EML 2.0.1 document with &gt; &amp; &lt;
     * Read should succeed since the same document should be read 
     * back from disk that was submitted. Query should succeed as 
     * well because the characters in this test are not changed in
     * the database.
     */
    public void symbolEncodedFormat201Test() {
    	debug("\nRunning: symbolEncodedFormat201Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            
            String testTitle = 
            	"Checking &gt; &lt; &quot; &apos; &amp; in doc: " + newdocid  + ".1";
            testdocument = getTestEmlDoc(testTitle, EML2_0_1);
            insertDocid(newdocid  + ".1", testdocument, SUCCESS, false);
            
            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(newdocid  + ".1", testTitle, EML2_0_1, SUCCESS);
            
            deleteDocid(newdocid  + ".1", SUCCESS, false);
            
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
     * Test inserting and reading an EML 2.1.0 document with &gt; &amp; &lt;
     * Read should succeed since the same document should be read 
     * back from disk that was submitted. Query should succeed as 
     * well because the characters in this test are not changed in
     * the database.
     */
    public void symbolEncodedFormat210Test() {
    	debug("\nRunning: symbolEncodedFormat210Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            
            String testTitle = 
            	"Checking &gt; &lt; &quot; &apos; &amp; in doc: " + newdocid + ".1";
            testdocument = getTestEmlDoc(testTitle, EML2_1_0);
            insertDocid(newdocid  + ".1", testdocument, SUCCESS, false);
            
            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, false);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(newdocid  + ".1", testTitle, EML2_1_0, SUCCESS);
            
            deleteDocid(newdocid  + ".1", SUCCESS, false);
            
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
     * Test inserting and reading an EML 2.0.1 document with single quote and double quote.
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query should succeed because we look for the converted
     * character (&apos; and &quot;).
     */
    public void quote201Test() {
    	debug("\nRunning: quote201Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            
            String testTitle = "Checking ' ` \" in doc: " + newdocid  + ".1";
            String convertedTestTitle = "Checking &apos; ` &quot; in doc: " + newdocid  + ".1";
            
            testdocument = getTestEmlDoc(testTitle, EML2_0_1);
            insertDocid(newdocid + ".1", testdocument, SUCCESS, true);
            
            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(newdocid  + ".1", convertedTestTitle, EML2_0_1, SUCCESS);
            
            deleteDocid(newdocid  + ".1", SUCCESS, false);
            
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
     * Test inserting and reading an EML 2.1.0 document with single quote and double quote.
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query shoud succeed because we look for the converted
     * character (&apos; and &quot;).
     */
    public void quote210Test() {
    	debug("\nRunning: quote210Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            
            String testTitle = "Checking ' ` \" in doc: " + newdocid  + ".1";
            String convertedTestTitle = "Checking &apos; ` &quot; in doc: " + newdocid  + ".1";
            
            testdocument = getTestEmlDoc(testTitle, EML2_1_0);
            insertDocid(newdocid + ".1", testdocument, SUCCESS, true);
            
            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(newdocid  + ".1", convertedTestTitle, EML2_1_0, SUCCESS);
            
            deleteDocid(newdocid  + ".1", SUCCESS, false);
            
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
     * Test inserting and reading an EML 2.0.1 document with the code representation 
     * of a micro sign (&#181). Read should succeed since the same document should be 
     * read back from disk that was submitted.  Query should succeed because we look 
     * for the converted character (µ).
     */
    public void numericCharacterReferenceFormat201Test() {
    	debug("\nRunning: numericCharacterReferenceFormat201Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            
            String testTitle = "Checking &#181; in doc: " + newdocid  + ".1";
            String convertedTestTitle = "Checking µ in doc: " + newdocid  + ".1";
            
            testdocument = getTestEmlDoc(testTitle, EML2_0_1);
            insertDocid(newdocid + ".1", testdocument, SUCCESS, true);

            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(newdocid  + ".1", convertedTestTitle, EML2_0_1, SUCCESS);
            
            deleteDocid(newdocid  + ".1", SUCCESS, false);
  
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
     * Test inserting and reading an EML 2.1.0 document with the code representation 
     * of a micro sign (&#181). Read should succeed since the same document should be 
     * read back from disk that was submitted.  Query should succeed because we look 
     * for the converted character (µ).
     */
    public void numericCharacterReferenceFormat210Test() {
    	debug("\nRunning: numericCharacterReferenceFormat210Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            
            String testTitle = "Checking &#181; in doc: " + newdocid  + ".1";
            String convertedTestTitle = "Checking µ in doc: " + newdocid  + ".1";
            
            testdocument = getTestEmlDoc(testTitle, EML2_1_0);
            insertDocid(newdocid + ".1", testdocument, SUCCESS, true);
            
            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(newdocid  + ".1", convertedTestTitle, EML2_1_0, SUCCESS);
            
            deleteDocid(newdocid  + ".1", SUCCESS, false);
            
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
     * Test inserting and reading an EML 2.0.1 document with the micro sign (µ). 
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query should succeed because we look for the same 
     * character (µ).
     */
    public void nonLatinUnicodeCharacter201Test() {
    	debug("\nRunning: nonLatinUnicodeCharacter201Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            
            String testTitle = "Checking characters like µ in doc: " + newdocid  + ".1";
            
            testdocument = getTestEmlDoc(testTitle, EML2_0_1);
            
            debug("original test document:	" + testdocument);
            
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);
            
            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(newdocid  + ".1", testTitle, EML2_0_1, SUCCESS);
            
            deleteDocid(newdocid  + ".1", SUCCESS, false);
            
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
     * Test inserting and reading an EML 2.1.0 document with the micro sign (¬µ). 
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query should succeed because we look for the same 
     * character (¬µ).
     */
    public void nonLatinUnicodeCharacter210Test() {
    	debug("\nRunning: nonLatinUnicodeCharacter210Test");
        try {
            String newdocid = generateDocid();
            m.login(username, password);
            
            String testTitle = "Checking characters like µ in doc: " + newdocid  + ".1";
            
            testdocument = getTestEmlDoc(testTitle, EML2_1_0);
            insertDocid(newdocid + ".1", testdocument, SUCCESS, false);

            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(newdocid  + ".1", testTitle, EML2_1_0, SUCCESS);
            
            deleteDocid(newdocid  + ".1", SUCCESS, false);
            
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
                               boolean expectMetacatException) {
        String response = null;
        try {
        	
        	debug("doctext: " + docText);
        	
            response = m.insert(docid,
                                new StringReader(docText), null);
            System.err.println(response);
            if (result) {
                assertTrue( (response.indexOf("<success>") != -1));
                assertTrue(response.indexOf(docid) != -1);
            }
            else {
                assertTrue( (response.indexOf("<success>") == -1));
            }
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
                fail("Insufficient karma:\n" + ike.getMessage());
        }
        catch (MetacatException me) {
            if (!expectMetacatException) {
                fail("Metacat Error:\n" + me.getMessage());
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
            System.err.println("respose from metacat: " + response);
        }
        catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
        catch (InsufficientKarmaException ike) {
            if (!expectedKarmaException) {
                fail("Insufficient karma:\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error:\n" + me.getMessage());
            } else {
                System.err.println("Metacat Error:\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
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
            } else {
                System.err.println("Metacat Error:\n" + me.getMessage());
            }
        }
        catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }

        return response;
    }

    /**
     * Delete a document from metacat. The expected result is passed as result
     */
    private void deleteDocid(String docid, boolean result,
                             boolean expextedKarmaFailure) {
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
            if(!expextedKarmaFailure){
                fail("Insufficient karma:\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            if (result) {
                fail("Metacat Error:\n" + me.getMessage());
            } else {
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
            // System.err.println(response);
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
            fail("Metacat Error:\n" + me.getMessage());
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
                                         boolean expectedKarmaFailure) {
        try {
            Reader r = new InputStreamReader(m.read(docid), "UTF-8");
            //InputStream is = m.read(docid);
            String doc = IOUtil.getAsString(r, true);
            //String doc = IOUtils.toString(is);
            
            if (result) {

                if (!testDoc.equals(doc)) {
                    debug("doc    :" + doc);
                    debug("testDoc:" + testDoc);
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
            if (!expectedKarmaFailure) {
                fail("Insufficient karma:\n" + ike.getMessage());
            }
        }
        catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
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
}
