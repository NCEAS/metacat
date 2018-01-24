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

import java.io.InputStream;
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
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.IOUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import junit.framework.Test;
import junit.framework.TestSuite;
import java.io.File;

import org.apache.commons.io.IOUtils;

/**
 * A JUnit test for testing Metacat when Non Ascii Characters are inserted
 */
public class InternationalizationTest
    extends MCTestCase {
    
    /**
     * Returns an xml squery that searches for the doc id in the
     * title of documents. This function is for eml-2.0.1+ only. For 
     * other eml versions, this function might have to modified.
     * NOTE: this override includes the "value" i18n element for eml 2.1.1
     */
	@Override
    protected String getTestEmlQuery(String titlePart, String emlVersion) {

    	String docType;
    	if (emlVersion.equals(EML2_0_1)) {
    		docType = "eml://ecoinformatics.org/eml-2.0.1";
    	} else if (emlVersion.equals(EML2_1_0)) {
    		docType = "eml://ecoinformatics.org/eml-2.1.0";
    	} else { //if (emlVersion.equals(EML2_1_1)) {
    		docType = "eml://ecoinformatics.org/eml-2.1.1";
    	}
    	
        String sQuery = "";
        sQuery = 
        	"<pathquery version=\"1.0\">" +
        		"<meta_file_id>unspecified</meta_file_id>" +
        		"<querytitle>unspecified</querytitle>" + 
        		"<returnfield>dataset/title</returnfield>" +
        		"<returnfield>dataset/title/value</returnfield>" +
        		"<returndoctype>" + docType + "</returndoctype>" +
        		"<querygroup operator=\"UNION\">" +
        			"<queryterm casesensitive=\"false\" searchmode=\"contains\">" +
        				"<value>" + titlePart + "</value>" +
        				"<pathexpr>dataset/title</pathexpr>" +
        			"</queryterm>" +
        			"<queryterm casesensitive=\"false\" searchmode=\"contains\">" +
    				"<value>" + titlePart + "</value>" +
    				"<pathexpr>dataset/title/value</pathexpr>" +
    			"</queryterm>" +
        		"</querygroup>" +
        	"</pathquery>";

        return sQuery;
    }

    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public InternationalizationTest(String name) {
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
        suite.addTest(new InternationalizationTest("initialize"));
        // Test basic functions
        //suite.addTest(new InternationalizationTest("unicodeCharacterTest"));
        suite.addTest(new InternationalizationTest("translation211Test"));
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
     * Test inserting and reading an EML 2.1.1 document with the multiple title translations. 
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query should succeed because we look for the same original and translation
     */
    public void translation211Test() {
    	debug("\nRunning: translation211Test");
        try {
            String newdocid = generateDocid();
            newdocid += ".1";
            m.login(username, password);
            
            String title_en_US = "Translation for document: " + newdocid;
            String title_zh_TW = "翻譯的文件: " + newdocid;

            String mixedTitle = 
            		title_en_US + 
            		"<value xml:lang=\"zh-TW\">" +
            		title_zh_TW +
            		"</value>";
            
            testdocument = getTestEmlDoc(mixedTitle, EML2_1_1);
            insertDocid(newdocid, testdocument, SUCCESS, false);

            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(newdocid, title_en_US, EML2_1_1, SUCCESS);
            queryDocWhichHasTitle(newdocid, title_zh_TW, EML2_1_1, SUCCESS);
            
            deleteDocid(newdocid, SUCCESS, false);
            
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
     * Test inserting and reading an EML 2.1.0 with Chinese
     */
    public void unicodeCharacterTest() {
    	debug("\nRunning: unicodeCharacterTest");
        try {
            
        	String filePath = "test/clienttestfiles/unicodeEML.xml";
            String testTitle = "測試中的數據包 (Test Chinese data package) _DOCID_";
            String newdocid = generateDocid() + ".1";
            testdocument = FileUtil.readFileToString(filePath, "UTF-8");
            
            // include the docid
            testdocument = testdocument.replaceAll("_DOCID_", newdocid);
            testTitle = testTitle.replaceAll("_DOCID_", newdocid);
            
            // login
            m.login(username, password);
            
            // insert
            insertDocid(newdocid, testdocument, SUCCESS, false);

            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, true);
            
            // this tests searching for the document in the database
            Thread.sleep(3000);
            queryDocWhichHasTitle(testTitle, testTitle, EML2_1_0, SUCCESS);
            
            // clean up
            //deleteDocid(newdocid, SUCCESS, false);
            
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
