package edu.ucsb.nceas.metacattest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.ecoinformatics.eml.EMLParserException;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A JUnit test for testing Metacat when Non Ascii Characters are inserted
 */
public class NonAsciiCharacterIT {
    private Session session;
    private String testdocument;
    private D1NodeServiceTest d1NodeTest;
    private HttpServletRequest request;

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
     * Establish a testing framework by initializing appropriate objects
     * @throws Exception 
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        d1NodeTest = new D1NodeServiceTest("initialize");
        request = d1NodeTest.getServletRequest();
        session = d1NodeTest.getTestSession();
    }


    /**
     * Test inserting an EML 2.0.1 document with > & <
     * should fail because this means an invalid xml document is being inserted
     */
    @Test
    public void invalidXMLCharacters201Test() throws Exception {

        String newdocid = generateDocid();
        try {
            testdocument = MCTestCase.getTestEmlDoc("Checking > & < in doc: " + newdocid,
                                                    MCTestCase.EML2_0_1);
            //DaaONEAPI - MN.create
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_0_1_format);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            fail("It shouldn't get there since the uploaded object is invalid");
        } catch (Exception e) {
            assertTrue("The exception should be EMLParserException",
                    e instanceof EMLParserException);
            assertTrue(e.getMessage().contains("'&'"));
        }
    }

    /**
     * Test inserting an EML 2.1.0 document with > & <
     * should fail because this means an invalid xml document is being inserted
     */
    @Test
    public void invalidXMLCharacters210Test() throws Exception {

        String newdocid = generateDocid();
        try {
            testdocument = MCTestCase.getTestEmlDoc("Checking > & < in doc: " + newdocid,
                                                      MCTestCase.EML2_1_0);
            //DaaONEAPI - MN.create
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_1_0_format);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            fail("It shouldn't get there since the uploaded object is invalid");
        } catch (Exception e) {
            assertTrue("The exception should be EMLParserException",
                            e instanceof EMLParserException);
            assertTrue(e.getMessage().contains("'&'"));
        }
    }

    /**
     * Test inserting and reading an EML 2.0.1 document with &gt; &amp; &lt;
     * Read should succeed since the same document should be read 
     * back from disk that was submitted. Query should succeed as 
     * well because the characters in this test are not changed in
     * the database.
     */
    @Test
    public void symbolEncodedFormat201Test() {

        try {
            String newdocid = generateDocid();
            String testTitle = 
                    "Checking &gt; &lt; &quot; &apos; &amp; in doc: " + newdocid;
            String convertedTestTitle = 
                    "Checking &gt; &lt; &quot; &apos; &amp; in doc: " + newdocid;
            testdocument = MCTestCase.getTestEmlDoc(testTitle, MCTestCase.EML2_0_1);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_0_1_format);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeTest.readDocidWhichEqualsDoc(newdocid, testdocument, true, session);
            // this tests searching for the document in the database
            //queryTile(convertedTestTitle, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
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
    @Test
    public void symbolEncodedFormat210Test() {

        try {
            String newdocid = generateDocid();
            String testTitle = 
                "Checking &gt; &lt; &quot; &apos; &amp; in doc: " + newdocid;
            String convertedTestTitle = "Checking &gt; &lt; &quot; &apos; &amp; in doc: " 
                                    + newdocid;
            testdocument = MCTestCase.getTestEmlDoc(testTitle, MCTestCase.EML2_1_0);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta =
                       D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_1_0_format);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeTest.readDocidWhichEqualsDoc(newdocid, testdocument, MCTestCase.SUCCESS, session);
            // this tests searching for the document in the database
            //queryTile(convertedTestTitle, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test inserting and reading an EML 2.0.1 document with single quote and double quote.
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query should succeed because we look for the converted
     * character (&apos; and &quot;).
     */
    @Test
    public void quote201Test() {
        try {
            String newdocid = generateDocid();
            String testTitle = "Checking ' ` \" in doc: " + newdocid;
            String convertedTestTitle = "Checking &apos; ` &quot; in doc: " + newdocid;
            testdocument = MCTestCase.getTestEmlDoc(testTitle, MCTestCase.EML2_0_1);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_0_1_format);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeTest.readDocidWhichEqualsDoc(newdocid, testdocument, MCTestCase.SUCCESS, session);
            // this tests searching for the document in the database
            //queryTile(convertedTestTitle, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test inserting and reading an EML 2.1.0 document with single quote and double quote.
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query shoud succeed because we look for the converted
     * character (&apos; and &quot;).
     */
    @Test
    public void quote210Test() {
        try {
            String newdocid = generateDocid();
            String testTitle = "Checking ' ` \" in doc: " + newdocid;
            String convertedTestTitle = "Checking &apos; ` &quot; in doc: " + newdocid;
            testdocument = MCTestCase.getTestEmlDoc(testTitle, MCTestCase.EML2_1_0);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_1_0_format);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeTest.readDocidWhichEqualsDoc(newdocid, testdocument, MCTestCase.SUCCESS, session);
            // this tests searching for the document in the database
            //queryTile(testTitle, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test inserting and reading an EML 2.0.1 document with the code representation 
     * of a micro sign (&#181). Read should succeed since the same document should be 
     * read back from disk that was submitted.  Query should succeed because we look 
     * for the converted character (µ).
     */
    @Test
    public void numericCharacterReferenceFormat201Test() {
        try {
            String newdocid = generateDocid();
            String testTitle = "Checking &#181; in doc: " + newdocid;
            String convertedTestTitle = "Checking µ in doc: " + newdocid;
            testdocument = MCTestCase.getTestEmlDoc(testTitle, MCTestCase.EML2_0_1);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_0_1_format);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeTest.readDocidWhichEqualsDoc(newdocid, testdocument, MCTestCase.SUCCESS, session);
            // this tests searching for the document in the database
            d1NodeTest.queryTile(convertedTestTitle, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test inserting and reading an EML 2.1.0 document with the code representation 
     * of a micro sign (&#181). Read should succeed since the same document should be 
     * read back from disk that was submitted.  Query should succeed because we look 
     * for the converted character (µ).
     */
    @Test
    public void numericCharacterReferenceFormat210Test() {
        try {
            String newdocid = generateDocid();
            String testTitle = "Checking &#181; in doc: " + newdocid;
            String convertedTestTitle = "Checking µ in doc: " + newdocid;
            testdocument = MCTestCase.getTestEmlDoc(testTitle, MCTestCase.EML2_1_0);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_1_0_format);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeTest.readDocidWhichEqualsDoc(newdocid, testdocument, MCTestCase.SUCCESS, session);
            // this tests searching for the document in the database
            d1NodeTest.queryTile(convertedTestTitle, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test inserting and reading an EML 2.0.1 document with the micro sign (µ). 
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query should succeed because we look for the same 
     * character (µ).
     */
    @Test
    public void nonLatinUnicodeCharacter201Test() {
        try {
            String newdocid = generateDocid();
            String testTitle = "Checking characters like µ in doc: " + newdocid  + ".1";
            testdocument = MCTestCase.getTestEmlDoc(testTitle, MCTestCase.EML2_0_1);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta =
                         D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_0_1_format);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeTest.readDocidWhichEqualsDoc(newdocid, testdocument, MCTestCase.SUCCESS, session);
            // this tests searching for the document in the database
            d1NodeTest.queryTile(testTitle, newdocid, session);
            MNodeService.getInstance(request).archive(session, guid);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test inserting and reading an EML 2.1.0 document with the micro sign (¬µ). 
     * Read should succeed since the same document should be read back from disk 
     * that was submitted.  Query should succeed because we look for the same 
     * character (¬µ).
     */
    @Test
    public void nonLatinUnicodeCharacter210Test() {
        try {
            String newdocid = generateDocid();
            String testTitle = "Checking characters like µ in doc: " + newdocid;
            testdocument = MCTestCase.getTestEmlDoc(testTitle, MCTestCase.EML2_1_0);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(newdocid);
            SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(D1NodeServiceTest.eml_2_1_0_format);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            // this tests reading the document back from disk
            d1NodeTest.readDocidWhichEqualsDoc(newdocid, testdocument, MCTestCase.SUCCESS, session);
            // this tests searching for the document in the database
            d1NodeTest.queryTile(testTitle, newdocid, session);
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
        String prefix = "test";
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
        int random = (Double.valueOf(Math.random()*10000000)).intValue();
        docid.append(random);
        return docid.toString();
    }
}
