package edu.ucsb.nceas.metacat.util;

import edu.ucsb.nceas.LeanTestUtils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A junit test class for the DocumentUtil class
 * @author tao
 *
 */
public class DocumentUtilTest {
    private static Log logMetacat = LogFactory.getLog(DocumentUtilTest.class);
    private static final String systemMetadataStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" "
            + "standalone=\"yes\"?>\n"
            + "<ns3:systemMetadata xmlns:ns2=\"http://ns.dataone.org/service/types/v1\" "
            + "xmlns:ns3=\"http://ns.dataone.org/service/types/v2.0\">\n"
            + "    <serialVersion>0</serialVersion>\n"
            + "    <identifier>testDocumentInfo.1706231690474</identifier>\n"
            + "    <formatId>eml://ecoinformatics.org/eml-2.0.1</formatId>\n"
            + "    <size>12960</size>\n"
            + "    <checksum algorithm=\"MD5\">e9eba01da2e921f03c0239a3632e70ac</checksum>\n"
            + "    <submitter>cn=test,dc=dataone,dc=org</submitter>\n"
            + "    <rightsHolder>cn=test,dc=dataone,dc=org</rightsHolder>\n"
            + "    <accessPolicy>\n"
            + "        <allow>\n"
            + "            <subject>public</subject>\n"
            + "            <permission>read</permission>\n"
            + "        </allow>\n"
            + "    </accessPolicy>\n"
            + "    <replicationPolicy replicationAllowed=\"false\"/>\n"
            + "    <archived>false</archived>\n"
            + "    <dateUploaded>2024-01-26T01:14:53.770+00:00</dateUploaded>\n"
            + "    <dateSysMetadataModified>2024-01-26T01:14:53.807+00:00</dateSysMetadataModified>"
            + "    <originMemberNode>urn:node:METACAT_TEST</originMemberNode>\n"
            + "    <authoritativeMemberNode>urn:node:METACAT_TEST</authoritativeMemberNode>\n"
            + "</ns3:systemMetadata>\n";

    private static final String docInfoStart = "<documentinfo><docid>autogen.20240117145.1</docid>";
    private static final String docInfoEnd = "<docname>eml</docname>"
                         + "<doctype>eml://ecoinformatics.org/eml-2.0.1</doctype>"
                         + "<user_owner>cn=test,dc=dataone,dc=org</user_owner>"
                         + "<user_updated>cn=test,dc=dataone,dc=org</user_updated>"
                         + "<date_created>2024-01-25T08:00:00.000+00:00</date_created>"
                         + "<date_updated>2024-01-25T08:00:00.000+00:00</date_updated><rev>1</rev>"
                         + "<accessControl>"
                         + "<access authSystem=\"knb\" order=\"allowFirst\" "
                         + "id=\"autogen.2024012517145396000.1\" scope=\"document\">\n"
                         + "    <allow>\n"
                         + "      <principal>public</principal>\n"
                         + "      <permission>read</permission>\n"
                         + "    </allow>\n"
                         + "</access></accessControl></documentinfo>";
    private static final String docInfo = docInfoStart + "<systemMetadata>" + systemMetadataStr 
                                                + "</systemMetadata>" + docInfoEnd;

    private static final String docInfoWithoutSysmeta = docInfoStart + docInfoEnd;

    @Before
    public void setUp() {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
    }
    /**
     * Test the  getSystemMetadataContent method
     * @throws Exception
     */
    @Test
    public void testGetSystemMetadataContent() throws Exception {
        //The given doc info does have system metadata section
        String systemMetadata = DocumentUtil.getSystemMetadataContent(docInfo);
        assertTrue("The system metadata string " + systemMetadata + " doesn't match the value "
                    + systemMetadataStr, systemMetadata.equals(systemMetadataStr));
        //The given doc info doesn't have system metadata section
        systemMetadata = DocumentUtil.getSystemMetadataContent(docInfoWithoutSysmeta);
        assertNull("The system metadata part should be null since the given doc info "
                    + "doesn't have this section.", systemMetadata);
    }

    /**
     * Test the getContentWithoutSystemMetadata method
     * @throws Exception
     */
    @Test
    public void testGetContentWithoutSystemMetadata() throws Exception {
        //The given doc info does have system metadata section
        String docInfoWithoutSysmeta = DocumentUtil.getContentWithoutSystemMetadata(docInfo);
        assertFalse("Document info shouldn't have the system metadata section "
                    + systemMetadataStr, docInfoWithoutSysmeta.contains(systemMetadataStr));
        assertTrue("Document info should have this section "
                    + docInfoStart, docInfoWithoutSysmeta.contains(docInfoStart));
        assertTrue("Document info should have this section "
                    + docInfoEnd, docInfoWithoutSysmeta.contains(docInfoEnd));
        //The given doc info doesn't have system metadata section
        docInfoWithoutSysmeta = DocumentUtil.getContentWithoutSystemMetadata(docInfoWithoutSysmeta);
        assertFalse("Document info shouldn't have the system metadata section "
                    + systemMetadataStr, docInfoWithoutSysmeta.contains(systemMetadataStr));
        assertTrue("Document info should have this section "
                    + docInfoStart, docInfoWithoutSysmeta.contains(docInfoStart));
        assertTrue("Document info should have this section "
                    + docInfoEnd, docInfoWithoutSysmeta.contains(docInfoEnd));
    }

    /**
     * Test the method of getAccessionNumberFromEcogridIdentifier
     * @throws Exception
     */
    @Test
    public void testGetAccessionNumberFromEcogridIdentifier() throws Exception {
        String str = "ecogrid://knb/tao.1.1";
        String result = DocumentUtil.getAccessionNumberFromEcogridIdentifier(str);
        assertTrue("The result should be tao.1.1 from the string " + str, result.equals("tao.1.1"));
        str = "https://knb/tao.1.1";
        result = DocumentUtil.getAccessionNumberFromEcogridIdentifier(str);
        assertNull("The result should null from the string " + str, result);
        str = null;
        assertNull("The result should null from the string " + str, result);
    }

    /**
     * Test the method of getDocIdFromString
     * @throws Exception
     */
    @Test
    public void testGetDocIdFromString() throws Exception {
        String input = "foo.2";
        String result = DocumentUtil.getDocIdFromString(input);
        assertTrue("The result should be foo.2 from the string " + input, result.equals("foo.2"));
        input = "foo.3.1";
        result = DocumentUtil.getDocIdFromString(input);
        assertTrue("The result should be foo.3 from the string " + input, result.equals("foo.3"));
        input = "foo";
        result = DocumentUtil.getDocIdFromString(input);
        assertNull("The result should null from the string " + input, result);
        input = null;
        result = DocumentUtil.getDocIdFromString(input);
        assertNull("The result should null from the string " + input, result);
    }

    /**
     * Test the method of getVersionFromString
     * @throws Exception
     */
    @Test
    public void testGetVersionFromString() throws Exception {
        String input = "foo.1.1";
        int rev = DocumentUtil.getVersionFromString(input);
        assertEquals("The revision from " + input + "should be 1", rev, 1);
        input = "foo.1";
        rev = DocumentUtil.getVersionFromString(input);
        assertEquals("The revision from " + input + "should be -1", rev, -1);
        input = "foo";
        rev = DocumentUtil.getVersionFromString(input);
        assertEquals("The revision from " + input + "should be -1", rev, -2);
        input = null;
        try {
            rev = DocumentUtil.getVersionFromString(input);
            fail("Test can't reach there since the input is null. It should throw an exception");
        } catch (Exception e) {
            logMetacat.info("We should get the exception " + e.getMessage());
        }
        input = "foo.2.str";
        try {
            rev = DocumentUtil.getVersionFromString(input);
            fail("Test can't reach there since the input is " + input
                        + ". It should throw an exception");
        } catch (Exception e) {
            logMetacat.info("We should get the exception " + e.getMessage());
        }
    }

    /**
     * Test the method of getRevisionStringFromString
     * @throws Exception
     */
    @Test
    public void testGetRevisionStringFromString() throws Exception {
        String input = "foo.1.1";
        String rev = DocumentUtil.getRevisionStringFromString(input);
        assertTrue("The revision from " + input + "should be 1", rev.equals("1"));
        input = "foo.1";
        rev = DocumentUtil.getRevisionStringFromString(input);
        assertNull("The revision from " + input + "should be null", rev);
        input = "foo";
        rev = DocumentUtil.getRevisionStringFromString(input);
        assertNull("The revision from " + input + "should be null", rev);
        input = null;
        try {
            rev = DocumentUtil.getRevisionStringFromString(input);
            fail("Test can't reach there since the input is null. It should throw an exception");
        } catch (Exception e) {
            logMetacat.info("We should get the exception " + e.getMessage());
        }
    }

    /**
     * Test the method of getSmartDocId
     * @throws Exception
     */
    @Test
    public void testGetSmartDocId() throws Exception {
        String input = "foo.3.1";
        String docid = DocumentUtil.getSmartDocId(input);
        assertEquals("The docid should be foo.3 from the input " + input, docid, "foo.3");
        input = "foo.3";
        docid = DocumentUtil.getSmartDocId(input);
        assertEquals("The docid should be foo.3 from the input " + input, docid, "foo.3");
        input = "foo";
        docid = DocumentUtil.getSmartDocId(input);
        assertNull("The docid should be null from the input " + input, docid);
        input = null;
        docid = DocumentUtil.getSmartDocId(input);
        assertNull("The docid should be foo.3 from the input " + input, docid);
    }

    /**
     * Test the method of generateDocumentId
     * @throws Exception
     */
    @Test
    public void testGenerateDocumentId() throws Exception {
        int rev = 1;
        String docid = DocumentUtil.generateDocumentId(rev);
        assertTrue("The docid " + docid + " should contain autogen", docid.startsWith("autogen"));
        int resultRev = DocumentUtil.getVersionFromString(docid);
        assertEquals("The revision should be 1 for docid " + docid, rev, resultRev);
        String firstPart = DocumentUtil.getDocIdFromString(docid);
        assertTrue("The docid " + docid +" should startWith "
                        + firstPart, docid.startsWith(firstPart));
    }
}
