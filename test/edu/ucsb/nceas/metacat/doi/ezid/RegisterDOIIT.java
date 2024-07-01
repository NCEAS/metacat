package edu.ucsb.nceas.metacat.doi.ezid;


import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.ezid.profile.DataCiteProfile;
import edu.ucsb.nceas.ezid.profile.InternalProfile;
import edu.ucsb.nceas.ezid.profile.InternalProfileValues;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeReplicationTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.doi.DOIService;
import edu.ucsb.nceas.metacat.doi.DOIServiceFactory;
import edu.ucsb.nceas.metacat.doi.datacite.EML2DataCiteFactoryTest;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static edu.ucsb.nceas.MCTestCase.printTestHeader;
import static edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest.createSystemMetadata;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * A JUnit test to exercise the DOI registration for content added via the DataONE MN API
 *
 * @author leinfelder
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RegisterDOIIT {
    public static final int MAX_TIMES = 20;
    public static final int SLEEP_TIME = 1000;
    public static final String EMLFILEPATH = "test/tao.14563.1.xml";
    public static final String creatorsStr =
        "<creators><creator><creatorName>onlySurName</creatorName></creator><creator><creatorName"
            + ">National Center for Ecological Analysis and "
            + "Synthesis</creatorName></creator><creator><creatorName>Smith, "
            + "John</creatorName></creator><creator><creatorName>King, "
            + "Wendy</creatorName></creator><creator><creatorName>University of California Santa "
            + "Barbara</creatorName></creator></creators>";

    private static String serverName = "RegisterDOITest.edu";
    // get ezid config properties
    private String ezidUsername = null;
    private String ezidPassword = null;
    private EZIDService ezid = null;
    private DOIService doiService = null;
    private MockedStatic<PropertyService> mockProperties;
    private final D1NodeServiceTest d1NodeServiceTest;
    private final HttpServletRequest request;
    private Properties withProperties = new Properties();

    /**
     * Constructor for the tests
     */
    public RegisterDOIIT() {
        d1NodeServiceTest = new D1NodeServiceTest("RegisterDOITest");
        request = d1NodeServiceTest.getServletRequest();
    }

    /**
     * Set up the test fixtures
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        d1NodeServiceTest.setUp();

        Settings.getConfiguration().clearProperty("D1Client.CN_URL");
        Settings.getConfiguration().addProperty("D1Client.CN_URL", "https://cn.dataone.org/cn");

        // get ezid config properties
        ezidUsername = PropertyService.getProperty("guid.doi.username");
        ezidPassword = PropertyService.getProperty("guid.doi.password");
        String ezidServiceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
        assertNotEquals("ezidUsername is empty!", "", ezidUsername.trim());
        assertNotEquals("ezidPassword is empty!", "", ezidPassword.trim());
        assertNotEquals("ezidServiceBaseUrl is empty!", "", ezidServiceBaseUrl.trim());

        withProperties.setProperty("server.name", serverName);
        withProperties.setProperty("guid.doi.enabled", "true");
        mockProperties = LeanTestUtils.initializeMockPropertyService(withProperties);
        mockProperties.when(() -> PropertyService.setPropertyNoPersist(anyString(), anyString()))
            .thenCallRealMethod();

        assertEquals("guid.doi.enabled is FALSE", "true",
                     PropertyService.getProperty("guid.doi.enabled"));

        ezid = new EZIDService(ezidServiceBaseUrl);
        doiService = DOIServiceFactory.getDOIService();


    }

    /**
     * Remove the test fixtures
     */
    @After
    public void tearDown() {
        mockProperties.close();
        d1NodeServiceTest.tearDown();
    }

    /**
     * constructs a "fake" session with a test subject
     *
     * @return
     */
    private Session getTestSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(
            "CN=Benjamin Leinfelder A515,O=University of Chicago,C=US,DC=cilogon,DC=org");
        session.setSubject(subject);
        return session;
    }

    @Test
    public void testMintAndCreateDOI() {
        printTestHeader("testMintAndCreateDOI");
        testMintAndCreateDOI(null);
    }

    @Test
    public void testMintAndCreateForEML() {
        printTestHeader("testMintAndCreateForEML");
        String emlFile = EMLFILEPATH;
        try (InputStream content = new FileInputStream(emlFile)) {
            testMintAndCreateDOI(content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test object creation
     */
    private void testMintAndCreateDOI(InputStream inputStream) {
        printTestHeader("testMintAndCreateDOI - common");

        try {
            ezid.login(ezidUsername, ezidPassword);
            // Mint a DOI
            Session session = getTestSession();
            Identifier guid =
                MNodeService.getInstance(request).generateIdentifier(session, "DOI", null);

            // check that EZID knows about it
            HashMap<String, String> metadata = null;
            int count = 0;
            do {
                try {
                    Thread.sleep(SLEEP_TIME);
                    metadata = ezid.getMetadata(guid.getValue());
                } catch (Exception e) {

                }
                count++;
            } while (metadata == null && count < MAX_TIMES);
            assertNotNull(metadata);

            // add the actual object for the newly-minted DOI
            SystemMetadata sysmeta = null;
            InputStream object = null;
            boolean isMetadata = false;
            if (inputStream != null) {
                sysmeta = createSystemMetadata(guid, session.getSubject(), inputStream);
                inputStream.close();
                object = new FileInputStream(EMLFILEPATH);
                sysmeta.setFormatId(
                    ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                        .getFormatId());
                isMetadata = true;
            } else {
                object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
                sysmeta = createSystemMetadata(guid, session.getSubject(), object);
                object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            }

            Identifier pid =
                d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            assertEquals(guid.getValue(), pid.getValue());

            // check for the metadata for title element
            count = 0;
            metadata = null;
            do {
                try {
                    Thread.sleep(SLEEP_TIME);
                    metadata = ezid.getMetadata(pid.getValue());
                    // check if the update thread finished yet, otherwise try again
                    if (metadata != null && isMetadata) {
                        String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
                        if (!registeredTarget.contains(serverName)) {
                            // try fetching it again
                            metadata = null;
                        }
                    }
                } catch (Exception e) {

                }
                count++;
            } while (metadata == null && count < MAX_TIMES);
            assertNotNull(metadata);
            assertTrue(metadata.containsKey(DataCiteProfile.TITLE.toString()));

            // check that the target URI was updated
            if (isMetadata) {
                String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
                assertTrue(registeredTarget.contains(serverName));
            }
            if (isMetadata) {
                String creator = metadata.get(DataCiteProfile.CREATOR.toString());
                //assertTrue(creator.equals("John Doe;NCEAS"));
            }

            System.out.println("tested with DOI: " + pid.getValue());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test object creation
     */
    @Test
    public void testCreateDOI() {
        printTestHeader("testCreateDOI");

        try {
            // get ezid config properties
            String shoulder = PropertyService.getProperty("guid.doi.doishoulder.1");

            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue(shoulder + "/testCreateDOI." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            assertEquals(guid.getValue(), pid.getValue());

            // check for the metadata explicitly, using ezid service
            ezid.login(ezidUsername, ezidPassword);
            int count = 0;
            HashMap<String, String> metadata = null;
            do {
                try {
                    Thread.sleep(SLEEP_TIME);
                    metadata = ezid.getMetadata(pid.getValue());
                } catch (Exception e) {

                }
                count++;
            } while (metadata == null && count < MAX_TIMES);

            assertNotNull(metadata);
            assertTrue(metadata.containsKey(EzidDOIService.DATACITE));
            String datacite = metadata.get(EzidDOIService.DATACITE);
            System.out.println(datacite);
            assertTrue(datacite.contains(
                "CN=Benjamin Leinfelder A515,O=University of Chicago,C=US,DC=cilogon,DC=org"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test object publishing
     */
    @Test
    public void testPublishDOI() {
        printTestHeader("testPublishDOI");

        try {
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testPublishDOI." + System.currentTimeMillis());

            // use EML to test
            // TODO: include an ORE to really exercise it
            String emlFile = "test/eml-datacite.xml";
            InputStream content = null;
            try {
                content = new FileInputStream(emlFile);

                // create the initial version without DOI
                SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
                content.close();
                sysmeta.setFormatId(
                    ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.0.1")
                        .getFormatId());
                content = new FileInputStream(emlFile);
                Identifier pid =
                    d1NodeServiceTest.mnCreate(session, guid, content, sysmeta);
                content.close();
                assertEquals(guid.getValue(), pid.getValue());
                Thread.sleep(5000);

                // now publish it
                Identifier publishedIdentifier =
                    MNodeService.getInstance(request).publish(session, pid);

                //check if the package id was updated
                InputStream emlObj =
                    MNodeService.getInstance(request).get(session, publishedIdentifier);
                String emlStr = IOUtils.toString(emlObj, StandardCharsets.UTF_8);
                assertTrue(emlStr.contains("packageId=\"" + publishedIdentifier.getValue() + "\""));

                // check for the metadata explicitly, using ezid service
                ezid.login(ezidUsername, ezidPassword);
                int count = 0;
                HashMap<String, String> metadata = null;
                String result = null;
                do {
                    try {
                        Thread.sleep(SLEEP_TIME);
                        metadata = ezid.getMetadata(publishedIdentifier.getValue());
                        if (metadata != null) {
                            result = metadata.get(EzidDOIService.DATACITE);
                        }
                    } catch (Exception e) {

                    }
                    count++;
                } while (result == null && count < MAX_TIMES);
                assertNotNull(metadata);
                result = metadata.get(EzidDOIService.DATACITE);
                System.out.println("result is\n" + result);
                Node node = MNodeService.getInstance(null).getCapabilities();
                String id = publishedIdentifier.getValue();
                id = id.replaceFirst("doi:", "");
                //assertTrue(result.contains(EML2DataCiteFactoryTest.section));
                System.out.println(id + EML2DataCiteFactoryTest.section1);
                assertTrue(result.contains(id + EML2DataCiteFactoryTest.section1));
                assertTrue(result.contains(EML2DataCiteFactoryTest.section2));
                assertTrue(result.contains(EML2DataCiteFactoryTest.section3));
                assertTrue(result.contains(
                    EML2DataCiteFactoryTest.section4 + EML2DataCiteFactoryTest.section41));
                assertTrue(result.contains(EML2DataCiteFactoryTest.section5));
                assertTrue(result.contains(EML2DataCiteFactoryTest.section6));
                assertTrue(result.contains(EML2DataCiteFactoryTest.section7));
                content.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(content);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }


    /**
     * Test the cases that an DOI is in the SID field.
     */
    @Test
    public void testCreateDOIinSid() {
        printTestHeader("testCreateDOIinSid");
        String scheme = "DOI";
        try {
            Session session = getTestSession();
            String emlFile = "test/eml-multiple-creators.xml";
            InputStream content = null;
            //Test the case that the identifier is a doi but no sid.
            try {
                Identifier publishedIdentifier =
                    MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
                System.out.println(
                    "The doi on the identifier is " + publishedIdentifier.getValue());
                content = new FileInputStream(emlFile);
                SystemMetadata sysmeta =
                    createSystemMetadata(publishedIdentifier, session.getSubject(), content);
                content.close();
                sysmeta.setFormatId(
                    ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                        .getFormatId());
                content = new FileInputStream(emlFile);
                Identifier pid = d1NodeServiceTest.mnCreate(session, publishedIdentifier,
                                                            content, sysmeta);
                content.close();
                assertEquals(publishedIdentifier.getValue(), pid.getValue());
                // check for the metadata explicitly, using ezid service
                ezid.login(ezidUsername, ezidPassword);
                int count = 0;
                HashMap<String, String> metadata = null;
                String result = null;
                do {
                    try {
                        Thread.sleep(SLEEP_TIME);
                        metadata = ezid.getMetadata(publishedIdentifier.getValue());
                    } catch (Exception e) {

                    }
                    if (metadata != null) {
                        result = metadata.get(EzidDOIService.DATACITE);
                    }
                    count++;
                } while (result == null && count < MAX_TIMES);
                System.out.println(
                    "The doi on the identifier is " + publishedIdentifier.getValue());
                assertNotNull(metadata);
                result = metadata.get(EzidDOIService.DATACITE);
                System.out.println("the result is \n" + result);
                assertTrue(result.contains("Test EML package - public-readable from morpho"));
                assertTrue(result.contains(creatorsStr));
                //System.out.println("publisher =======is"+publisher);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                String year = sdf.format(sysmeta.getDateUploaded());
                assertTrue(result.contains(year));
                //System.out.println("publishing year =======is"+publishingYear);
                //System.out.println("resource type =======is"+resourceType);
                assertTrue(result.contains("Dataset"));
                content.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(content);
            }


            //Test the case that the identifier is non-doi but the sid is a doi
            try {
                Identifier guid = new Identifier();
                guid.setValue("tesCreateDOIinSid." + System.currentTimeMillis());
                System.out.println("The identifier is " + guid.getValue());
                Identifier publishedIdentifier =
                    MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
                System.out.println("The doi on the SID field is " + publishedIdentifier.getValue());
                content = new FileInputStream(emlFile);
                SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
                content.close();
                sysmeta.setFormatId(
                    ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                        .getFormatId());
                sysmeta.setSeriesId(publishedIdentifier);
                content = new FileInputStream(emlFile);
                Identifier pid =
                    d1NodeServiceTest.mnCreate(session, guid, content, sysmeta);
                content.close();
                assertEquals(guid.getValue(), pid.getValue());
                // check for the metadata explicitly, using ezid service
                ezid.login(ezidUsername, ezidPassword);
                int count = 0;
                HashMap<String, String> metadata = null;
                String result = null;
                do {
                    try {
                        Thread.sleep(SLEEP_TIME);
                        metadata = ezid.getMetadata(publishedIdentifier.getValue());
                        if (metadata != null) {
                            result = metadata.get(EzidDOIService.DATACITE);
                        }
                    } catch (Exception e) {

                    }
                    count++;
                } while (result == null && count < MAX_TIMES);

                assertNotNull(metadata);
                System.out.println("the metadata is " + metadata);
                result = metadata.get(EzidDOIService.DATACITE);
                System.out.println("the result is \n" + result);
                assertTrue(result.contains("Test EML package - public-readable from morpho"));
                assertTrue(result.contains(creatorsStr));
                //System.out.println("publisher =======is"+publisher);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                String year = sdf.format(sysmeta.getDateUploaded());
                assertTrue(result.contains(year));
                //System.out.println("publishing year =======is"+publishingYear);
                //System.out.println("resource type =======is"+resourceType);
                assertTrue(result.contains("Dataset"));
                content.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(content);
            }

            //Test the case that both identifier and sid are dois
            try {
                Identifier publishedIdentifier =
                    MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
                System.out.println(
                    "The doi in the identifier field is " + publishedIdentifier.getValue());
                Identifier doiSid =
                    MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
                System.out.println("The doi in the sid field is " + doiSid.getValue());
                content = new FileInputStream(emlFile);
                SystemMetadata sysmeta =
                    createSystemMetadata(publishedIdentifier, session.getSubject(), content);
                content.close();
                sysmeta.setFormatId(
                    ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                        .getFormatId());
                sysmeta.setSeriesId(doiSid);
                content = new FileInputStream(emlFile);
                Identifier pid = d1NodeServiceTest.mnCreate(session, publishedIdentifier,
                                                            content, sysmeta);
                content.close();
                assertEquals(publishedIdentifier.getValue(), pid.getValue());
                // check for the metadata explicitly, using ezid service
                ezid.login(ezidUsername, ezidPassword);
                int count = 0;
                //query the identifier
                HashMap<String, String> metadata = null;
                String result = null;
                do {
                    try {
                        Thread.sleep(SLEEP_TIME);
                        metadata = ezid.getMetadata(publishedIdentifier.getValue());
                        if (metadata != null) {
                            result = metadata.get(EzidDOIService.DATACITE);
                        }
                    } catch (Exception e) {

                    }
                    count++;
                } while (result == null && count < MAX_TIMES);

                assertNotNull(metadata);
                result = metadata.get(EzidDOIService.DATACITE);
                System.out.println("the result is \n" + result);
                assertTrue(result.contains("Test EML package - public-readable from morpho"));
                assertTrue(result.contains(creatorsStr));
                //System.out.println("publisher =======is"+publisher);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                String year = sdf.format(sysmeta.getDateUploaded());
                assertTrue(result.contains(year));
                //System.out.println("publishing year =======is"+publishingYear);
                //System.out.println("resource type =======is"+resourceType);
                assertTrue(result.contains("Dataset"));

                //query the sid
                HashMap<String, String> metadata2 = null;
                result = null;
                do {
                    try {
                        Thread.sleep(SLEEP_TIME);
                        metadata2 = ezid.getMetadata(doiSid.getValue());
                        if (metadata2 != null) {
                            result = metadata2.get(EzidDOIService.DATACITE);
                        }
                    } catch (Exception e) {

                    }
                    count++;
                } while (result == null && count < MAX_TIMES);

                assertNotNull(metadata2);
                result = metadata2.get(EzidDOIService.DATACITE);
                System.out.println("the result is \n" + result);
                assertTrue(result.contains("Test EML package - public-readable from morpho"));
                assertTrue(result.contains(creatorsStr));
                sdf = new SimpleDateFormat("yyyy");
                year = sdf.format(sysmeta.getDateUploaded());
                assertTrue(result.contains(year));
                assertTrue(result.contains("Dataset"));
                content.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(content);
            }

            //Test the case that either identifier or sid is a doi
            try {
                Identifier guid = new Identifier();
                guid.setValue("tesCreateDOIinSid." + System.currentTimeMillis());
                System.out.println("The identifier (non-doi) is " + guid.getValue());
                Identifier sid = new Identifier();
                sid.setValue("tesCreateDOIinSid-2." + System.currentTimeMillis());
                System.out.println("The sid field (non-doi) is " + sid.getValue());
                content = new FileInputStream(emlFile);
                SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
                content.close();
                sysmeta.setFormatId(
                    ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                        .getFormatId());
                sysmeta.setSeriesId(sid);
                content = new FileInputStream(emlFile);
                Identifier pid =
                    d1NodeServiceTest.mnCreate(session, guid, content, sysmeta);
                content.close();
                assertEquals(guid.getValue(), pid.getValue());
                // check for the metadata explicitly, using ezid service
                ezid.login(ezidUsername, ezidPassword);
                int count = 0;
                HashMap<String, String> metadata = null;
                do {
                    try {
                        Thread.sleep(SLEEP_TIME);
                        metadata = ezid.getMetadata(guid.getValue());
                    } catch (Exception e) {

                    }
                    count = count + 4; // it will be null, so we don't need to run too many times.
                } while (metadata == null && count < MAX_TIMES);
                System.out.println("the metadata is " + metadata);
                assertNull(metadata);
                count = 0;
                do {
                    try {
                        Thread.sleep(SLEEP_TIME);
                        metadata = ezid.getMetadata(sid.getValue());
                    } catch (Exception e) {

                    }
                    count = count + 4; // it will be null, so we don't need to run too many times.
                } while (metadata == null && count < MAX_TIMES);
                assertNull(metadata);
                content.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(content);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test change the access policy on an DOI object
     *
     * @throws Exception
     */
    @Test
    public void testUpdateAccessPolicyOnDOIObject() throws Exception {
        printTestHeader("testUpdateAccessPolicyOnDOIObject");
        String user = "uid=test,o=nceas";
        //create an doi object
        String scheme = "DOI";
        Session session = getTestSession();
        String emlFile = "test/eml-multiple-creators.xml";
        InputStream content = null;
        Identifier publishedIdentifier =
            MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
        System.out.println("The doi on the identifier is " + publishedIdentifier.getValue());
        content = new FileInputStream(emlFile);
        SystemMetadata sysmeta =
            createSystemMetadata(publishedIdentifier, session.getSubject(), content);
        content.close();
        sysmeta.setFormatId(
            ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                .getFormatId());
        content = new FileInputStream(emlFile);
        Identifier pid = d1NodeServiceTest.mnCreate(session, publishedIdentifier, content, sysmeta);
        content.close();
        assertEquals(publishedIdentifier.getValue(), pid.getValue());
        SystemMetadata meta =
            MNodeService.getInstance(request).getSystemMetadata(session, publishedIdentifier);
        //It should succeed to add a new access policy to the system metadata
        Subject subject = new Subject();
        subject.setValue(user);
        AccessRule rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        AccessPolicy access = meta.getAccessPolicy();
        access.addAllow(rule);
        meta.setAccessPolicy(access);
        boolean success = MNodeService.getInstance(request)
            .updateSystemMetadata(session, publishedIdentifier, meta);
        assertTrue(
            "The update should be successful since we don't restrict the access rules.", success);
        meta = MNodeService.getInstance(request).getSystemMetadata(session, publishedIdentifier);
        access = meta.getAccessPolicy();
        boolean find = false;
        for (AccessRule item : access.getAllowList()) {
            if (item != null && item.getSubject(0) != null && item.getSubject(0).getValue()
                .equals(user)) {
                find = true;
                break;
            }
        }
        assertTrue("We should find the user " + user + " on the access rules.", find);

        //It should fail to remove the allow rules for the public user.
        AccessPolicy newAccess = new AccessPolicy();
        for (AccessRule item : access.getAllowList()) {
            //don't allow the access rules for the user public
            if (item != null && item.getSubject(0) != null && !item.getSubject(0).getValue()
                .equals("public")) {
                newAccess.addAllow(item);
            }
        }
        meta.setAccessPolicy(newAccess);
        try {
            MNodeService.getInstance(request)
                .updateSystemMetadata(session, publishedIdentifier, meta);
            fail("We shouldn't get here since removing public-read access rules for a DOI object "
                     + "should fail.");
        } catch (InvalidRequest e) {
            assertTrue(e.getMessage().contains(publishedIdentifier.getValue()));
        }

        //test the doi on sid field
        Identifier guid = new Identifier();
        guid.setValue("testUpdateAccessPolicyOnDOIObject." + System.currentTimeMillis());
        System.out.println("The identifier is " + guid.getValue());
        Identifier sid =
            MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
        System.out.println("The doi on the sid is " + sid.getValue());
        content = new FileInputStream(emlFile);
        sysmeta = createSystemMetadata(guid, session.getSubject(), content);
        content.close();
        sysmeta.setFormatId(
            ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                .getFormatId());
        sysmeta.setSeriesId(sid);
        content = new FileInputStream(emlFile);
        pid = d1NodeServiceTest.mnCreate(session, guid, content, sysmeta);
        content.close();
        assertEquals(guid.getValue(), pid.getValue());
        meta = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        //It should succeed to add a new access policy to the system metadata
        subject = new Subject();
        subject.setValue(user);
        rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        access = meta.getAccessPolicy();
        access.addAllow(rule);
        meta.setAccessPolicy(access);
        success = MNodeService.getInstance(request).updateSystemMetadata(session, guid, meta);
        assertTrue(
            "The update should be successful since we don't restrict the access rules.", success);
        meta = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        access = meta.getAccessPolicy();
        boolean found = false;
        for (AccessRule item : access.getAllowList()) {
            if (item != null && item.getSubject(0) != null && item.getSubject(0).getValue()
                .equals(user)) {
                found = true;
                break;
            }
        }
        assertTrue("We should find the user " + user + " on the access rules.", found);

        //It should fail to remove the allow rules for the public user.
        newAccess = new AccessPolicy();
        for (AccessRule item : access.getAllowList()) {
            //don't allow the access rules for the user public
            if (item != null && item.getSubject(0) != null && !item.getSubject(0).getValue()
                .equals("public")) {
                newAccess.addAllow(item);
            }
        }
        meta.setAccessPolicy(newAccess);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, meta);
            fail("We shouldn't get here since removing public-read access rules for a DOI object "
                     + "should fail.");
        } catch (InvalidRequest e) {
            assertTrue(e.getMessage().contains(guid.getValue()));
            assertTrue(e.getMessage().contains(sid.getValue()));
        }
    }


    /**
     * Test change the access policy on an DOI object which is not public readable
     *
     * @throws Exception
     */
    @Test
    public void testUpdateAccessPolicyOnPrivateDOIObject() throws Exception {
        printTestHeader("testUpdateAccessPolicyOnPrivateDOIObject");
        String user = "uid=test,o=nceas";
        //create an doi object
        String scheme = "DOI";
        Session session = getTestSession();
        String emlFile = "test/eml-multiple-creators.xml";
        InputStream content = null;
        Identifier publishedIdentifier =
            MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
        System.out.println("The doi on the identifier is " + publishedIdentifier.getValue());
        content = new FileInputStream(emlFile);
        SystemMetadata sysmeta =
            createSystemMetadata(publishedIdentifier, session.getSubject(), content);
        sysmeta.setAccessPolicy(new AccessPolicy()); //nobody can read it except the rights holder
        content.close();
        sysmeta.setFormatId(
            ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                .getFormatId());
        content = new FileInputStream(emlFile);
        Identifier pid = d1NodeServiceTest.mnCreate(session, publishedIdentifier,
                                                    content, sysmeta);
        content.close();
        assertEquals(publishedIdentifier.getValue(), pid.getValue());
        SystemMetadata meta =
            MNodeService.getInstance(request).getSystemMetadata(session, publishedIdentifier);
        //It should succeed to add a new access policy to the system metadata
        Subject subject = new Subject();
        subject.setValue(user);
        AccessRule rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        AccessPolicy access = new AccessPolicy();
        access.addAllow(rule);
        meta.setAccessPolicy(access);
        boolean success = MNodeService.getInstance(request)
            .updateSystemMetadata(session, publishedIdentifier, meta);
        assertTrue(
            "The update should be successful even though there is no public readable rule on the "
                + "new access policy since the old access policy is private.", success);
        meta = MNodeService.getInstance(request).getSystemMetadata(session, publishedIdentifier);
        access = meta.getAccessPolicy();
        boolean find = false;
        for (AccessRule item : access.getAllowList()) {
            if (item != null && item.getSubject(0) != null && item.getSubject(0).getValue()
                .equals(user)) {
                find = true;
                break;
            }
        }
        assertTrue("We should find the user " + user + " on the access rules.", find);

        boolean findPublic = false;
        for (AccessRule item : access.getAllowList()) {
            if (item != null && item.getSubject(0) != null && item.getSubject(0).getValue()
                .equalsIgnoreCase("public")) {
                findPublic = true;
                break;
            }
        }
        assertFalse("We should not find the public user on the access rules.", findPublic);
        //System.out.println("The identifier is
        // ========================================="+publishedIdentifier.getValue());
        //test the doi on sid field
        Identifier guid = new Identifier();
        guid.setValue("testUpdateAccessPolicyOnDOIObject." + System.currentTimeMillis());
        System.out.println("The identifier is " + guid.getValue());
        Identifier sid =
            MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
        System.out.println("The doi on the sid is " + sid.getValue());
        content = new FileInputStream(emlFile);
        sysmeta = createSystemMetadata(guid, session.getSubject(), content);
        sysmeta.setAccessPolicy(new AccessPolicy());
        content.close();
        sysmeta.setFormatId(
            ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                .getFormatId());
        sysmeta.setSeriesId(sid);
        content = new FileInputStream(emlFile);
        pid = d1NodeServiceTest.mnCreate(session, guid, content, sysmeta);
        content.close();
        assertEquals(guid.getValue(), pid.getValue());
        meta = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        //It should succeed to add a new access policy to the system metadata
        subject = new Subject();
        subject.setValue(user);
        rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        access = new AccessPolicy();
        access.addAllow(rule);
        meta.setAccessPolicy(access);
        success = MNodeService.getInstance(request).updateSystemMetadata(session, guid, meta);
        assertTrue(
            "he update should be successful even though there is no public readable rule on the "
                + "new access policy since the old access policy is private.", success);
        meta = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        access = meta.getAccessPolicy();
        boolean found = false;
        for (AccessRule item : access.getAllowList()) {
            if (item != null && item.getSubject(0) != null && item.getSubject(0).getValue()
                .equals(user)) {
                found = true;
                break;
            }
        }
        assertTrue("We should find the user " + user + " on the access rules.", found);
        findPublic = false;
        for (AccessRule item : access.getAllowList()) {
            if (item != null && item.getSubject(0) != null && item.getSubject(0).getValue()
                .equalsIgnoreCase("public")) {
                findPublic = true;
                break;
            }
        }
        assertFalse("We should not find the public user on the access rules.", findPublic);
        //System.out.println("The identifier is ========================================="+guid
        // .getValue());

    }

    /**
     * Test to publish an eml 2.2.0 document
     *
     * @throws Exception
     */
    @Test
    public void testPublishEML220() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testPublishDOI." + System.currentTimeMillis());
        // use EML to test
        String emlFile = "test/eml-2.2.0.xml";
        InputStream content = null;
        content = new FileInputStream(emlFile);
        // create the initial version without DOI
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
        content.close();
        sysmeta.setFormatId(
            ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0")
                .getFormatId());
        content = new FileInputStream(emlFile);
        Identifier pid = d1NodeServiceTest.mnCreate(session, guid, content, sysmeta);
        content.close();
        assertEquals(guid.getValue(), pid.getValue());
        Thread.sleep(5000);
        // now publish it
        Identifier publishedIdentifier = MNodeService.getInstance(request).publish(session, pid);
        // check for the metadata explicitly, using ezid service
        ezid.login(ezidUsername, ezidPassword);
        int count = 0;
        HashMap<String, String> metadata = null;
        String result = null;
        do {
            try {
                Thread.sleep(SLEEP_TIME);
                metadata = ezid.getMetadata(publishedIdentifier.getValue());
                if (metadata != null) {
                    result = metadata.get(EzidDOIService.DATACITE);
                }
            } catch (Exception e) {

            }
            count++;
        } while (result == null && count < MAX_TIMES);
        assertNotNull(metadata);
        result = metadata.get(EzidDOIService.DATACITE);
        assertTrue(result.contains("EML Annotation Example"));
        assertTrue(result.contains("0000-0002-1209-5122"));
        assertTrue(result.contains("It can include multiple paragraphs"));
        content.close();

        //check if the package id was updated
        InputStream emlObj = MNodeService.getInstance(request).get(session, publishedIdentifier);
        String emlStr = IOUtils.toString(emlObj, StandardCharsets.UTF_8);
        assertTrue(emlStr.contains("packageId=\"" + publishedIdentifier.getValue() + "\""));

        //check the query
        String query = "q=id:" + "\"" + publishedIdentifier.getValue() + "\"";
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        do {
            try {
                Thread.sleep(SLEEP_TIME * 3);
                if (resultStr.contains("funding")) {
                    break;
                }
                stream = MNodeService.getInstance(request).query(session, "solr", query);
                resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            } catch (Exception e) {

            }
            count++;
        } while (count < MAX_TIMES);
        assertTrue(resultStr.contains("<arr name=\"funding\">"));
        assertTrue(resultStr.contains(
            "<str>Funding is from a grant from the National Science Foundation.</str>"));
        assertTrue(resultStr.contains("<arr name=\"funderName\">"));
        assertTrue(resultStr.contains("<str>National Science Foundation</str>"));
        assertTrue(resultStr.contains("<arr name=\"funderIdentifier\">"));
        assertTrue(resultStr.contains("<str>https://doi.org/10.13039/00000001</str>"));
        assertTrue(resultStr.contains("<arr name=\"awardNumber\">"));
        assertTrue(resultStr.contains("<str>1546024</str>"));
        assertTrue(resultStr.contains("<arr name=\"awardTitle\">"));
        assertTrue(resultStr.contains(
            "<str>Scientia Arctica: A Knowledge Archive for Discovery and Reproducible Science in"
                + " the Arctic</str>"));
        assertTrue(resultStr.contains("<arr name=\"sem_annotation\">"));
        assertTrue(resultStr.contains("<str>http://purl.dataone.org/odo/ECSO_00000512</str>"));
        assertTrue(resultStr.contains("<str>http://purl.dataone.org/odo/ECSO_00000512</str>"));
    }

    /**
     * When the autoPublish is true, to test the whole process: Reserve a doi, create an object use
     * the doi and publishIdentifier this doi.
     *
     * @throws Exception
     */
    @Test
    public void testPublishIdentifierProcessWithAutoPublishOn() throws Exception {
        printTestHeader("testPublishIdentifierProcessWithAutoPublishOn");
        try {
            PropertyService.setPropertyNoPersist("guid.doi.autoPublish", "true");
            doiService.refreshStatus();
            ezid.login(ezidUsername, ezidPassword);
            // Mint a DOI
            Session session = getTestSession();
            Identifier guid =
                MNodeService.getInstance(request).generateIdentifier(session, "DOI", null);
            // check that EZID knows about it and its status is reserved
            HashMap<String, String> metadata = null;
            int count = 0;
            do {
                try {
                    Thread.sleep(SLEEP_TIME);
                    metadata = ezid.getMetadata(guid.getValue());
                } catch (Exception e) {

                }
                count++;
            } while (metadata == null && count < MAX_TIMES);
            System.out.println("after reserve (auto on)\n" + metadata.toString());
            assertNotNull(metadata);
            assertEquals(metadata.get(InternalProfile.STATUS.toString()),
                         InternalProfileValues.RESERVED.toString());

            // add the actual object for the newly-minted DOI
            SystemMetadata sysmeta = null;
            InputStream object = new FileInputStream(EMLFILEPATH);
            sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            object.close();
            sysmeta.setFormatId(
                ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                    .getFormatId());
            object = new FileInputStream(EMLFILEPATH);
            Identifier pid =
                d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            assertEquals(guid.getValue(), pid.getValue());
            // check for the metadata for title element
            count = 0;
            metadata = null;
            do {
                try {
                    Thread.sleep(SLEEP_TIME);
                    metadata = ezid.getMetadata(pid.getValue());
                    // check if the update thread finished yet, otherwise try again
                    if (metadata != null) {
                        String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
                        if (!registeredTarget.contains(serverName)) {
                            // try fetching it again
                            metadata = null;
                        }
                    }
                } catch (Exception e) {

                }
                count++;
            } while (metadata == null && count < MAX_TIMES);
            assertNotNull(metadata);
            System.out.println("after crate object (auto on)\n" + metadata);
            assertTrue(metadata.containsKey(DataCiteProfile.TITLE.toString()));
            // check that the target URI was updated
            String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
            assertTrue(registeredTarget.contains(serverName));
            String creator = metadata.get(DataCiteProfile.CREATOR.toString());
            assertEquals(metadata.get(InternalProfile.STATUS.toString()),
                         InternalProfileValues.PUBLIC.toString());

            //publishIdentifier
            MNodeService.getInstance(request).publishIdentifier(session, guid);
            count = 0;
            metadata = null;
            do {
                try {
                    Thread.sleep(SLEEP_TIME);
                    metadata = ezid.getMetadata(pid.getValue());
                    // check if the update thread finished yet, otherwise try again
                    if (metadata != null) {
                        registeredTarget = metadata.get(InternalProfile.TARGET.toString());
                        if (!registeredTarget.contains(serverName)) {
                            // try fetching it again
                            metadata = null;
                        }
                    }
                } catch (Exception e) {

                }
                count = count + 4;
            } while (metadata == null && count < MAX_TIMES);
            assertNotNull(metadata);
            System.out.println("after publishIdentifier (auto on)\n" + metadata);
            assertTrue(metadata.containsKey(DataCiteProfile.TITLE.toString()));
            // check that the target URI was updated
            registeredTarget = metadata.get(InternalProfile.TARGET.toString());
            assertTrue(registeredTarget.contains(serverName));
            creator = metadata.get(DataCiteProfile.CREATOR.toString());
            assertEquals(metadata.get(InternalProfile.STATUS.toString()),
                         InternalProfileValues.PUBLIC.toString());
            assertEquals(metadata.get(InternalProfile.EXPORT.toString()),
                         InternalProfileValues.YES.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }


    /**
     * When the autoPublish is false, to test the whole process: Reserve a doi, create an object use
     * the doi and publishIdentifier this doi.
     */
    @Test
    public void testPublishIdentifierProcessWithAutoPublishOff() {
        printTestHeader("testPublishIdentifierProcessWithAutoPublishOff");
        try {
            PropertyService.setPropertyNoPersist("guid.doi.autoPublish", "false");
            doiService.refreshStatus();
            ezid.login(ezidUsername, ezidPassword);
            // Mint a DOI
            Session session = getTestSession();
            Identifier guid =
                MNodeService.getInstance(request).generateIdentifier(session, "DOI", null);
            // check that EZID knows about it and its status is reserved
            HashMap<String, String> metadata = null;
            int count = 0;
            do {
                try {
                    Thread.sleep(SLEEP_TIME);
                    metadata = ezid.getMetadata(guid.getValue());
                } catch (Exception e) {

                }
                count++;
            } while (metadata == null && count < MAX_TIMES);
            assertNotNull(metadata);
            assertEquals(metadata.get(InternalProfile.STATUS.toString()),
                         InternalProfileValues.RESERVED.toString());
            System.out.println("after reserve (auto off)\n" + metadata);
            // add the actual object for the newly-minted DOI
            SystemMetadata sysmeta = null;
            InputStream object = new FileInputStream(EMLFILEPATH);
            sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            object.close();
            sysmeta.setFormatId(
                ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0")
                    .getFormatId());
            object = new FileInputStream(EMLFILEPATH);
            Identifier pid =
                d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            assertEquals(guid.getValue(), pid.getValue());
            // check for the metadata for title element
            count = 0;
            metadata = null;
            do {
                try {
                    Thread.sleep(SLEEP_TIME);
                    metadata = ezid.getMetadata(pid.getValue());
                    // check if the update thread finished yet, otherwise try again
                    if (metadata != null) {
                        String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
                        if (!registeredTarget.contains(serverName)) {
                            // try fetching it again
                            metadata = null;
                        }
                    }
                } catch (Exception e) {

                }
                count++;
            } while (metadata == null && count < MAX_TIMES);
            System.out.println("after crate object (auto off)\n" + metadata.toString());
            assertNotNull(metadata);
            assertTrue(metadata.containsKey(DataCiteProfile.TITLE.toString()));
            // check that the target URI was updated
            String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
            assertTrue(registeredTarget.contains(serverName));
            String creator = metadata.get(DataCiteProfile.CREATOR.toString());
            assertEquals(metadata.get(InternalProfile.STATUS.toString()),
                         InternalProfileValues.RESERVED.toString());

            //publishIdentifier
            MNodeService.getInstance(request).publishIdentifier(session, guid);
            count = 0;
            metadata = null;
            do {
                try {
                    Thread.sleep(SLEEP_TIME);
                    metadata = ezid.getMetadata(pid.getValue());
                    // check if the update thread finished yet, otherwise try again
                    if (metadata != null) {
                        registeredTarget = metadata.get(InternalProfile.TARGET.toString());
                        if (!registeredTarget.contains(serverName)) {
                            // try fetching it again
                            metadata = null;
                        }
                    }
                } catch (Exception e) {

                }
                count++;
            } while (metadata == null && count < MAX_TIMES);
            assertNotNull(metadata);
            System.out.println("after publishIdentifier (auto off)\n" + metadata);
            assertTrue(metadata.containsKey(DataCiteProfile.TITLE.toString()));
            // check that the target URI was updated
            registeredTarget = metadata.get(InternalProfile.TARGET.toString());
            assertTrue(registeredTarget.contains(serverName));
            creator = metadata.get(DataCiteProfile.CREATOR.toString());
            assertEquals(metadata.get(InternalProfile.STATUS.toString()),
                         InternalProfileValues.PUBLIC.toString());
            assertEquals(metadata.get(InternalProfile.EXPORT.toString()),
                         InternalProfileValues.YES.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /***
     * Test to publish a private package to make all of them public
     * @throws Exception
     */
    @Test
    public void testPublishPrivatePackageToPublic() throws Exception {
        printTestHeader("testPublishPrivatePackageToPublic");
        String user = "uid=test,o=nceas";
        Subject subject = new Subject();
        subject.setValue(user);
        AccessRule rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        AccessPolicy access = new AccessPolicy();
        access.addAllow(rule);

        Subject publicSub = new Subject();
        publicSub.setValue("public");
        Session publicSession = new Session();
        publicSession.setSubject(publicSub);

        PropertyService.setPropertyNoPersist("guid.doi.enforcePublicReadableEntirePackage", "true");
        boolean enforcePublicEntirePackageInPublish = Boolean.parseBoolean(
            PropertyService.getProperty("guid.doi.enforcePublicReadableEntirePackage"));
        MNodeService.setEnforcePublisEntirePackage(enforcePublicEntirePackageInPublish);

        //insert data
        Session session = getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testPublishPrivatePackageToPublic-data." + System.currentTimeMillis());
        System.out.println("the data file id is ==== " + guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setAccessPolicy(access);
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //insert metadata
        Identifier guid2 = new Identifier();
        guid2.setValue("testPublishPrivatePackageToPublic-metadata." + System.currentTimeMillis());
        System.out.println("the metadata  file id is ==== " + guid2.getValue());
        InputStream object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta2 = createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        sysmeta2.setAccessPolicy(access);
        object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeServiceTest.mnCreate(session, guid2, object2, sysmeta2);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //Make sure both data and metadata objects have been indexed
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int account = 0;
        while ((resultStr == null || !resultStr.contains("checksum")) && account <= MAX_TIMES) {
            Thread.sleep(SLEEP_TIME);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(
            "get data: resultStr was NULL for query: " + query + "; possible indexing problem?",
            resultStr);
        assertTrue("get data: result did not contain \"checksum\" for query: " + query
                       + "; possible indexing problem?", resultStr.contains("checksum"));
        assertFalse(
            "get data: exceeded max number of tries to get query results for query: " + query
                + "; possible indexing problem?", account > MAX_TIMES);

        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        account = 0;
        while ((resultStr == null || !resultStr.contains("checksum")) && account <= MAX_TIMES) {
            Thread.sleep(SLEEP_TIME);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(
            "get metadata: resultStr was NULL for query: " + query + "; possible indexing problem?",
            resultStr);
        assertTrue("get metadata: result did not contain \"checksum\" for query: " + query
                       + "; possible indexing problem?", resultStr.contains("checksum"));
        assertFalse(
            "get metadata: exceeded max number of tries to get query results for query: " + query
                + "; possible indexing problem?", account > MAX_TIMES);

        //insert resource map
        Map<Identifier, List<Identifier>> idMap = new HashMap<>();
        List<Identifier> dataIds = new ArrayList<>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue(
            "testPublishPrivatePackageToPublic-resourcemap." + System.currentTimeMillis());
        System.out.println("the resource file id is ==== " + resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 =
            new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta3 =
            createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        sysmeta3.setAccessPolicy(access);
        d1NodeServiceTest.mnCreate(session, resourceMapId, object3, sysmeta3);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //make sure the result map was indexed
        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        account = 0;
        while ((resultStr == null || !resultStr.contains("checksum")) && account <= MAX_TIMES) {
            Thread.sleep(SLEEP_TIME);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull("get resourcemap: resultStr was NULL for query: " + query
                          + "; possible indexing problem?", resultStr);
        assertTrue("get resourcemap: result did not contain \"checksum\" for query: " + query
                       + "; possible indexing problem?", resultStr.contains("checksum"));
        assertFalse(
            "get resourcemap: exceeded max number of tries to get query results for query: " + query
                + "; possible indexing problem?", account > MAX_TIMES);

        //publish the metadata id
        Identifier publishedIdentifier = MNodeService.getInstance(request).publish(session, guid2);
        Thread.sleep(SLEEP_TIME);

        //the new identifier is public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, publishedIdentifier);
        //the old metadata object still is not public readable
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        //the old resource map still is not public readable
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        //new resource map object is public readable
        SystemMetadata oldResourceMapSys =
            MNodeService.getInstance(request).getSystemMetadata(session, resourceMapId);
        Identifier newResourceMapId = oldResourceMapSys.getObsoletedBy();
        assertNotNull("newResourceMapId was NULL", newResourceMapId);
        MNodeService.getInstance(request).getSystemMetadata(publicSession, newResourceMapId);
        //the data object is public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
    }

    /***
     * Test to publish a private package to make only metadata and resource map objects public.
     * The data objects are still private
     * @throws Exception
     */
    @Test
    public void testPublishPrivatePackageToPartialPublic() throws Exception {
        printTestHeader("testPublishPrivatePackageToPartialPublic");
        String user = "uid=test,o=nceas";
        Subject subject = new Subject();
        subject.setValue(user);
        AccessRule rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        AccessPolicy access = new AccessPolicy();
        access.addAllow(rule);

        Subject publicSub = new Subject();
        publicSub.setValue("public");
        Session publicSession = new Session();
        publicSession.setSubject(publicSub);

        PropertyService.setPropertyNoPersist(
            "guid.doi.enforcePublicReadableEntirePackage", "false");
        boolean enforcePublicEntirePackageInPublish = Boolean.parseBoolean(
            PropertyService.getProperty("guid.doi.enforcePublicReadableEntirePackage"));
        MNodeService.setEnforcePublisEntirePackage(enforcePublicEntirePackageInPublish);

        //insert data
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(
            "testPublishPrivatePackageToPartialPublic-data." + System.currentTimeMillis());
        System.out.println("the data file id is ==== " + guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setAccessPolicy(access);
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //insert metadata
        Identifier guid2 = new Identifier();
        guid2.setValue(
            "testPublishPrivatePackageToPartialPublic-metadata." + System.currentTimeMillis());
        System.out.println("the metadata  file id is ==== " + guid2.getValue());
        InputStream object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta2 = createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        sysmeta2.setAccessPolicy(access);
        object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeServiceTest.mnCreate(session, guid2, object2, sysmeta2);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //Make sure both data and metadata objects have been indexed
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int account = 0;
        while ((resultStr == null || !resultStr.contains("checksum")) && account <= MAX_TIMES) {
            Thread.sleep(SLEEP_TIME);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(
            "get data: resultStr was NULL for query: " + query + "; possible indexing problem?",
            resultStr);
        assertTrue("get data: result did not contain \"checksum\" for query: " + query
                       + "; possible indexing problem?", resultStr.contains("checksum"));
        assertFalse(
            "get data: exceeded max number of tries to get query results for query: " + query
                + "; possible indexing problem?", account > MAX_TIMES);

        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        account = 0;
        while ((resultStr == null || !resultStr.contains("checksum")) && account <= MAX_TIMES) {
            Thread.sleep(SLEEP_TIME);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(
            "get metadata: resultStr was NULL for query: " + query + "; possible indexing problem?",
            resultStr);
        assertTrue("get metadata: result did not contain \"checksum\" for query: " + query
                       + "; possible indexing problem?", resultStr.contains("checksum"));
        assertFalse(
            "get metadata: exceeded max number of tries to get query results for query: " + query
                + "; possible indexing problem?", account > MAX_TIMES);

        //insert resource map
        Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
        List<Identifier> dataIds = new ArrayList<Identifier>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue(
            "testPublishPrivatePackageToPartialPublic-resourcemap." + System.currentTimeMillis());
        System.out.println("the resource file id is ==== " + resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 =
            new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta3 =
            createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        sysmeta3.setAccessPolicy(access);
        d1NodeServiceTest.mnCreate(session, resourceMapId, object3, sysmeta3);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //make sure the result map was indexed
        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        account = 0;
        while ((resultStr == null || !resultStr.contains("checksum")) && account <= MAX_TIMES) {
            Thread.sleep(SLEEP_TIME);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull("get resourcemap: resultStr was NULL for query: " + query
                          + "; possible indexing problem?", resultStr);
        assertTrue("get resourcemap: result did not contain \"checksum\" for query: " + query
                       + "; possible indexing problem?", resultStr.contains("checksum"));
        assertFalse(
            "get resourcemap: exceeded max number of tries to get query results for query: " + query
                + "; possible indexing problem?", account > MAX_TIMES);

        //publish the metadata id
        Identifier publishedIdentifier = MNodeService.getInstance(request).publish(session, guid2);
        Thread.sleep(SLEEP_TIME);

        //the new identifier is public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, publishedIdentifier);
        //the old metadata object still is not public readable
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        //the old resource map still is not public readable
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        //new resource map object is public readable
        SystemMetadata oldResourceMapSys =
            MNodeService.getInstance(request).getSystemMetadata(session, resourceMapId);
        Identifier newResourceMapId = oldResourceMapSys.getObsoletedBy();
        int count = 0;
        while (newResourceMapId == null && count++ < MAX_TIMES) {
            Thread.sleep(SLEEP_TIME);
            newResourceMapId =
                MNodeService.getInstance(request).getSystemMetadata(session, resourceMapId)
                    .getObsoletedBy();
        }
        assertNotNull("newResourceMapId was NULL", newResourceMapId);


        MNodeService.getInstance(request).getSystemMetadata(publicSession, newResourceMapId);
        //the data object is not public readable
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
    }

}
