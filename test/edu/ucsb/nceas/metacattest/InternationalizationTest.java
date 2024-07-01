package edu.ucsb.nceas.metacattest;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.utilities.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


/**
 * A JUnit test for testing Metacat when Non Ascii Characters are inserted
 */
public class InternationalizationTest {

    private D1NodeServiceTest d1NodeServiceTest;
    private HttpServletRequest request;


    /**
     * Set up the test frame work
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        d1NodeServiceTest = new D1NodeServiceTest("initialize");
        request = d1NodeServiceTest.getServletRequest();
    }

    /**
     * Test inserting and reading an EML 2.1.1 document with the multiple title translations. 
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query should succeed because we look for the same original and translation
     */
    @Test
    public void translation211Test() {
        try {
            Session session = d1NodeServiceTest.getTestSession();
            String newdocid = generateDocid();
            newdocid += ".1";
            String title_en_US = "Translation for document: " + newdocid;
            String title_zh_TW = "翻譯的文件: " + newdocid;

            String mixedTitle = title_en_US + "<value xml:lang=\"zh-TW\">" + title_zh_TW + "</value>";

            String testdocument = D1NodeServiceTest.getTestEmlDoc(mixedTitle,
                                                                        D1NodeServiceTest.EML2_1_1);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_1_1_format);
            d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeServiceTest.readDocidWhichEqualsDoc(newdocid, testdocument, MCTestCase.SUCCESS, session);
            // this tests searching for the document in the database
            d1NodeServiceTest.queryTile(title_en_US, newdocid, session);
            d1NodeServiceTest.queryTile(title_zh_TW, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test inserting and reading an EML 2.1.0 with Chinese
     */
    @Test
    public void unicodeCharacterTest() {

        try {
            Session session = d1NodeServiceTest.getTestSession();
            String filePath = "test/clienttestfiles/unicodeEML.xml";
            String testTitle = "測試中的數據包 (Test Chinese data package) _DOCID_";
            String newdocid = generateDocid() + ".1";
            String testdocument = FileUtil.readFileToString(filePath, "UTF-8");

            // include the docid
            testdocument = testdocument.replaceAll("_DOCID_", newdocid);
            testTitle = testTitle.replaceAll("_DOCID_", newdocid);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_1_1_format);
            d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeServiceTest.readDocidWhichEqualsDoc(newdocid, testdocument, MCTestCase.SUCCESS, session);
            // this tests searching for the document in the database
            d1NodeServiceTest.queryTile(testTitle, newdocid, session);
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
        StringBuffer docid = new StringBuffer("test");
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
        int random = (Double.valueOf(Math.random()*100)).intValue();
        docid.append(random);
        return docid.toString();
    }
}
