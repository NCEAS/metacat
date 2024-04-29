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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.utilities.FileUtil;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


/**
 * A JUnit test for testing Metacat when Non Ascii Characters are inserted
 */
public class InternationalizationTest extends D1NodeServiceTest {

    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public InternationalizationTest(String name) {
        super(name);
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
        suite.addTest(new InternationalizationTest("initialize"));
        // Test basic functions
        suite.addTest(new InternationalizationTest("unicodeCharacterTest"));
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
     * Test inserting and reading an EML 2.1.1 document with the multiple title translations. 
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query should succeed because we look for the same original and translation
     */
    public void translation211Test() {
    	debug("\nRunning: translation211Test");
        try {
            Session session = getTestSession();
            String newdocid = generateDocid();
            newdocid += ".1";
            String title_en_US = "Translation for document: " + newdocid;
            String title_zh_TW = "翻譯的文件: " + newdocid;

            String mixedTitle = 
            		title_en_US + 
            		"<value xml:lang=\"zh-TW\">" +
            		title_zh_TW +
            		"</value>";
            
            testdocument = getTestEmlDoc(mixedTitle, EML2_1_1);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(eml_2_1_1_format);
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, session);
            // this tests searching for the document in the database
            queryTile(title_en_US, newdocid, session);
            queryTile(title_zh_TW, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test inserting and reading an EML 2.1.0 with Chinese
     */
    public void unicodeCharacterTest() {
    	debug("\nRunning: unicodeCharacterTest");
        try {
            Session session = getTestSession();
        	String filePath = "test/clienttestfiles/unicodeEML.xml";
            String testTitle = "測試中的數據包 (Test Chinese data package) _DOCID_";
            String newdocid = generateDocid() + ".1";
            testdocument = FileUtil.readFileToString(filePath, "UTF-8");
            
            // include the docid
            testdocument = testdocument.replaceAll("_DOCID_", newdocid);
            testTitle = testTitle.replaceAll("_DOCID_", newdocid);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(eml_2_1_1_format);
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            readDocidWhichEqualsDoc(newdocid, testdocument, SUCCESS, session);
            // this tests searching for the document in the database
            queryTile(testTitle, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
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
