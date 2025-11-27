package edu.ucsb.nceas.metacat.doi.osti;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.osti_elink.OSTIElinkNotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integrating tests for the OstiDOIService class.
 * @author tao
 *
 */
public class OstiDOIServiceIT {
    private final static String TOKEN_FILE_PATH_NAME = "ostiService.v2.tokenFilePath";
    private final static String TOKEN_ENV_NAME = "METACAT_OSTI_TOKEN";
    private final static String R_STATUS = "SV";
    private OstiDOIService service = null;
    private final static int MAX_ATTEMPTS = 20;
    private D1NodeServiceTest d1NodeTest;
    private MockHttpServletRequest request;
    private MockedStatic<PropertyService> closeableMock;
    private Properties withProperties = new Properties();

    @Rule
    public EnvironmentVariablesRule environmentVariablesRule =
        new EnvironmentVariablesRule(TOKEN_ENV_NAME, null);

    /**
     * Set up the test fixtures
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        d1NodeTest = new D1NodeServiceTest("initialize");
        final String passwdMsg =
                """
                \n* * * * * * * * * * * * * * * * * * *
                DOI CREDENTIALS NOT SET!
                Test requires OSTI-specific values for
                'guid.doi.username' & 'guid.doi.password'
                in your test/test.properties file!
                * * * * * * * * * * * * * * * * * * *
                """;
        Properties testProperties = LeanTestUtils.getExpectedProperties();
        String ostiName = testProperties.getProperty("guid.doi.username");
        String ostiPass = testProperties.getProperty("guid.doi.password");
        String tokenPath = testProperties.getProperty(TOKEN_FILE_PATH_NAME);
        // We need to set up an env variable to pass the token to the osti-elink library
        String token = FileUtils.readFileToString(new File(tokenPath));
        environmentVariablesRule.set(TOKEN_ENV_NAME, token);
        assertNotNull(passwdMsg, ostiName);
        assertFalse(passwdMsg, ostiName.isBlank());
        assertNotEquals(passwdMsg, "apitest", ostiName);
        assertNotNull(passwdMsg, ostiPass);
        assertFalse(passwdMsg, ostiPass.isBlank());

        withProperties.setProperty("guid.doi.enabled", "true");
        withProperties.setProperty("guid.doiservice.plugin.class",
                                                "edu.ucsb.nceas.metacat.doi.osti.OstiDOIService");
        //withProperties.setProperty("guid.doi.baseurl", "https://www.osti.gov/elinktest/2416api");
        withProperties.setProperty("guid.doi.autoPublish", "false");
        withProperties.setProperty("guid.doi.enforcePublicReadableEntirePackage", "false");
        withProperties.setProperty("guid.doi.doishoulder.1", "doi:10.15485/");
        withProperties.setProperty("guid.doi.doishoulder.2", "doi:10.5072/");
        withProperties.setProperty("guid.doi.username", ostiName);
        withProperties.setProperty("guid.doi.password", ostiPass);
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
        request = (MockHttpServletRequest)d1NodeTest.getServletRequest();
        service = new OstiDOIService();
    }

    @After
    public void tearDown() throws Exception {
        if (closeableMock != null) {
            closeableMock.close();
        }
    }

    /**
     * Test the publish process when the autoPublish is off.
     * @throws Exception
     */
    @Test
    public void testPublishProcess() throws Exception {
        D1NodeServiceTest.printTestHeader("testPublishProcess");
        //set guid.doi.autoPublish off
        PropertyService.getInstance().setPropertyNoPersist("guid.doi.autoPublish", "false");
        service.refreshStatus();
        //Get the doi
        String emlFile = "test/eml-ess-dive.xml";
        Identifier doi = service.generateDOI();
        int count = 0;
        String meta = null;
        while (count < MAX_ATTEMPTS) {
            try {
                meta = service.getMetadata(doi);
                break;
            } catch (OSTIElinkNotFoundException e) {
                Thread.sleep(1000);
                count ++;
            }
        }
        count = 0;
        while (count < MAX_ATTEMPTS && meta != null && !meta.contains("\"title\":\"unknown\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("\"workflow_status\":\"SA\""));
        assertTrue(meta.contains("\"title\":\"unknown\""));

        //create an object with the doi
        Session session = d1NodeTest.getTestSession();
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(doi, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = d1NodeTest.mnCreate(session, doi, eml, sysmeta);
        eml.close();
        assertEquals(doi.getValue(), pid.getValue());
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("\"title\":\"Specific conductivity")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("\"title\":\"Specific conductivity"));
        assertTrue(meta.contains("\"workflow_status\":\"SA\""));

        //publish the object with a different session.
        try {
            Session session2 = d1NodeTest.getAnotherSession();
            MNodeService.getInstance(request).publishIdentifier(session2, doi);
            fail("We shouldn't get here since the session can't publish it");
        } catch (Exception e) {
            assertTrue( e instanceof NotAuthorized);
        }
        //publish the identifier with the doi
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("\"workflow_status\":\"" + R_STATUS+ "\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("\"title\":\"Specific conductivity"));
        assertTrue(meta.contains("\"workflow_status\":\"" + R_STATUS+ "\""));
    }

    /**
     * Test the publish process when the autoPublish is off and the sid is a doi
     * @throws Exception
     */
    @Test
    public void testPublishProcessForSID() throws Exception {
        D1NodeServiceTest.printTestHeader("testPublishProcessForSID");
        PropertyService.getInstance().setPropertyNoPersist("guid.doi.autoPublish", "false");
        service.refreshStatus();

        //create an object with a non-doi
        String emlFile = "test/eml-ess-dive.xml";
        Session session = d1NodeTest.getTestSession();
        Identifier guid =  new Identifier();
        guid.setValue("testPublishProcessForSID." + System.currentTimeMillis());
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = d1NodeTest.mnCreate(session, guid, eml, sysmeta);
        SystemMetadata readSys = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        eml.close();
        assertEquals(guid.getValue(), pid.getValue());

       //Get the doi
        Identifier doi = service.generateDOI();
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && meta != null && !meta.contains("<title>unknown</title>")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("status=\"Saved\""));
        assertTrue(meta.contains("<title>unknown</title>"));

        //update system metadata to set a doi as sid
        eml = new FileInputStream(emlFile);
        SystemMetadata sysmetaNew = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), eml);
        sysmetaNew.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        sysmetaNew.setDateUploaded(readSys.getDateUploaded());
        eml.close();
        sysmetaNew.setSeriesId(doi);
        sysmetaNew.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmetaNew);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("<title>Specific conductivity")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Saved\""));

        //publish the identifier with the doi
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));

        //updated the object whose serial id is a doi. The status of doi is pending.
        //after the update, the status of doi should still be pending
        Identifier guid2 =  new Identifier();
        guid2.setValue("testPublishProcessForSID2." + System.currentTimeMillis());
        eml = new FileInputStream(emlFile);
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), eml);
        sysmeta.setSeriesId(doi);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier newPid = d1NodeTest.mnUpdate(session, guid, eml, guid2, sysmeta);
        assertEquals(newPid.getValue(), guid2.getValue());
        Thread.sleep(3);
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
    }

    /**
     * Test the publish process when the autoPublish is on and the pid is a doi.
     * @throws Exception
     */
    @Test
    public void testAutoPublishProcess() throws Exception {
        D1NodeServiceTest.printTestHeader("testAutoPublishProcess");
        //set guid.doi.autoPublish off
        withProperties.setProperty("guid.doi.autoPublish", "true");
        closeableMock.close();
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
        service.refreshStatus();
        //Get the doi
        String emlFile = "test/eml-ess-dive.xml";
        Identifier doi = service.generateDOI();
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && meta != null && !meta.contains("<title>unknown</title>")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("status=\"Saved\""));
        assertTrue(meta.contains("<title>unknown</title>"));

        //create an object with the doi
        Session session = d1NodeTest.getTestSession();
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(doi, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = d1NodeTest.mnCreate(session, doi, eml, sysmeta);
        eml.close();
        assertEquals(doi.getValue(), pid.getValue());
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("<title>Specific conductivity")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));

        //publish the object with a different session.
        try {
            Session session2 = d1NodeTest.getAnotherSession();
            MNodeService.getInstance(request).publishIdentifier(session2, doi);
            fail("We shouldn't get here since the session can't publish it");
        } catch (Exception e) {
            assertTrue( e instanceof NotAuthorized);
        }
        //publish the identifier with the doi
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
    }

    /**
     * Test the publish process when the autoPublish is off and the sid is a doi
     * @throws Exception
     */
    @Test
    public void testAutoPublishProcessForSID() throws Exception {
        D1NodeServiceTest.printTestHeader("testAutoPublishProcessForSID");
        withProperties.setProperty("guid.doi.autoPublish", "true");
        closeableMock.close();
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
        service.refreshStatus();

        //create an object with a non-doi
        String emlFile = "test/eml-ess-dive.xml";
        Session session = d1NodeTest.getTestSession();
        Identifier guid =  new Identifier();
        guid.setValue("testPublishProcessForSID." + System.currentTimeMillis());
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = d1NodeTest.mnCreate(session, guid, eml, sysmeta);
        SystemMetadata readSys = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        eml.close();
        assertEquals(guid.getValue(), pid.getValue());

       //Get the doi
        Identifier doi = service.generateDOI();
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && meta != null && !meta.contains("<title>unknown</title>")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("status=\"Saved\""));
        assertTrue(meta.contains("<title>unknown</title>"));

        //update system metadata to set a doi as sid
        eml = new FileInputStream(emlFile);
        SystemMetadata sysmetaNew = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), eml);
        sysmetaNew.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        sysmetaNew.setDateUploaded(readSys.getDateUploaded());
        eml.close();
        sysmetaNew.setSeriesId(doi);
        sysmetaNew.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmetaNew);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("<title>Specific conductivity")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));

        //publish the identifier with the doi
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));

        //updated the object whose serial id is a doi. The status of doi is pending.
        //after the update, the status of doi should still be pending
        Identifier guid2 =  new Identifier();
        guid2.setValue("testPublishProcessForSID2." + System.currentTimeMillis());
        eml = new FileInputStream(emlFile);
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), eml);
        sysmeta.setSeriesId(doi);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier newPid = d1NodeTest.mnUpdate(session, guid, eml, guid2, sysmeta);
        assertEquals(newPid.getValue(), guid2.getValue());
        Thread.sleep(3);
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
    }

    /**
     * Test create an object with a doi which doesn't cotain the registered shoulder.
     * So this doi will not be handled
     * @throws Exception
     */
    @Test
    public void testUnregisteredShoulder() throws Exception {
        D1NodeServiceTest.printTestHeader("testUnregisteredShoulder");
        int appendix = (int) (Math.random() * 100);
        String doi = "doi:15/1289761W/" + System.currentTimeMillis() + appendix;
        Identifier guid = new Identifier();
        guid.setValue(doi);
        //create an object with the doi
        String emlFile = "test/eml-ess-dive.xml";
        Session session = d1NodeTest.getTestSession();
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = d1NodeTest.mnCreate(session, guid, eml, sysmeta);
        eml.close();
        assertEquals(guid.getValue(), pid.getValue());
        Thread.sleep(5000);
        try {
            String meta = service.getMetadata(guid);
            fail("we can't get here ");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
    }

    /***
     * Test to publishIdentifier with a private package to make all of them public
     * @throws Exception
     */
    @Test
    public void testPublishIdentifierPrivatePackageToPublic() throws Exception {
        D1NodeServiceTest.printTestHeader("testPublishIdentifierPrivatePackageToPublic");
        String user = "uid=test,o=nceas";
        Subject subject = new Subject();
        subject.setValue(user);

        Subject publicSub = new Subject();
        publicSub.setValue("public");
        Session publicSession = new Session();
        publicSession.setSubject(publicSub);

        withProperties.setProperty("guid.doi.enforcePublicReadableEntirePackage", "true");
        closeableMock.close();
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);

        //insert data
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testPublishPrivatePackageToPublic-data." + System.currentTimeMillis());
        System.out.println("the data file id is ==== "+guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        AccessRule rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        AccessPolicy access = new AccessPolicy();
        access.addAllow(rule);
        sysmeta.setAccessPolicy(access);
        d1NodeTest.mnCreate(session, guid, object, sysmeta);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //insert metadata
        String emlFile = "test/eml-ess-dive.xml";
        Identifier guid2 = new Identifier();
        guid2.setValue("testPublishPrivatePackageToPublic-metadata." + System.currentTimeMillis());
        System.out.println("the metadata  file id is ==== "+guid2.getValue());
        InputStream object2 = new FileInputStream(new File(emlFile));
        SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta2.setFormatId(formatId);
        AccessRule rule2 = new AccessRule();
        rule2.addSubject(subject);
        rule2.addPermission(Permission.WRITE);
        AccessPolicy access2 = new AccessPolicy();
        access2.addAllow(rule2);
        sysmeta2.setAccessPolicy(access2);
        object2 = new FileInputStream(new File(emlFile));
        d1NodeTest.mnCreate(session, guid2, object2, sysmeta2);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //Make sure both data and metadata objects have been indexed
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }

        //insert resource map
        Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
        List<Identifier> dataIds = new ArrayList<Identifier>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue("testPublishPrivatePackageToPublic-resourcemap." + System.currentTimeMillis());
        System.out.println("the resource file id is ==== "+resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 = new ByteArrayInputStream(resourceMapXML.getBytes("UTF-8"));
        SystemMetadata sysmeta3 = D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        AccessRule rule3 = new AccessRule();
        rule3.addSubject(subject);
        rule3.addPermission(Permission.WRITE);
        AccessPolicy access3 = new AccessPolicy();
        access3.addAllow(rule3);
        sysmeta3.setAccessPolicy(access3);
        d1NodeTest.mnCreate(session, resourceMapId, object3, sysmeta3);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //make sure the result map was indexed
        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }

        //Get a doi and put it into the series id
        Identifier doi = MNodeService.getInstance(request).generateIdentifier(session, "doi", null);
        SystemMetadata readSysmeta = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        readSysmeta.setSeriesId(doi);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, readSysmeta);
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("seriesId")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("seriesId"));
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, doi);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //publishIdentifier the metadata id
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));

        //the metadata identifiers (pid and sid) are public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
        MNodeService.getInstance(request).getSystemMetadata(publicSession, doi);

        //the resource map is public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);

        //the data object is public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
    }

    /***
     * Test to publishIdentifier with a private package to make some of them public.
     * The data file will not
     * @throws Exception
     */
    @Test
    public void testPublishIdentifierPrivatePackageToPartialPublic() throws Exception {
        D1NodeServiceTest.printTestHeader("testPublishIdentifierPrivatePackageToPartialPublic");
        String user = "uid=test,o=nceas";
        Subject subject = new Subject();
        subject.setValue(user);

        Subject publicSub = new Subject();
        publicSub.setValue("public");
        Session publicSession = new Session();
        publicSession.setSubject(publicSub);

        boolean enforcePublicEntirePackageInPublish = Boolean.parseBoolean(PropertyService
                                 .getProperty("guid.doi.enforcePublicReadableEntirePackage"));
        MNodeService.setEnforcePublisEntirePackage(enforcePublicEntirePackageInPublish);

        //insert data
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testPublishIdentifierPrivatePackageToPartialPublic-data." + System.currentTimeMillis());
        System.out.println("the data file id is ==== "+guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        AccessRule rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        AccessPolicy access = new AccessPolicy();
        access.addAllow(rule);
        sysmeta.setAccessPolicy(access);
        d1NodeTest.mnCreate(session, guid, object, sysmeta);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //insert metadata
        String emlFile = "test/eml-ess-dive.xml";
        Identifier guid2 = new Identifier();
        guid2.setValue("testPublishIdentifierPrivatePackageToPartialPublic-metadata." + System.currentTimeMillis());
        System.out.println("the metadata  file id is ==== "+guid2.getValue());
        InputStream object2 = new FileInputStream(new File(emlFile));
        SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta2.setFormatId(formatId);
        AccessRule rule2 = new AccessRule();
        rule2.addSubject(subject);
        rule2.addPermission(Permission.WRITE);
        AccessPolicy access2 = new AccessPolicy();
        access2.addAllow(rule2);
        sysmeta2.setAccessPolicy(access2);
        object2 = new FileInputStream(new File(emlFile));
        d1NodeTest.mnCreate(session, guid2, object2, sysmeta2);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //Make sure both data and metadata objects have been indexed
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }

        //insert resource map
        Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
        List<Identifier> dataIds = new ArrayList<Identifier>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue("testPublishIdentifierPrivatePackageToPartialPublic-resourcemap." + System.currentTimeMillis());
        System.out.println("the resource file id is ==== "+resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 = new ByteArrayInputStream(resourceMapXML.getBytes("UTF-8"));
        SystemMetadata sysmeta3 = D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        AccessRule rule3 = new AccessRule();
        rule3.addSubject(subject);
        rule3.addPermission(Permission.WRITE);
        AccessPolicy access3 = new AccessPolicy();
        access3.addAllow(rule3);
        sysmeta3.setAccessPolicy(access3);
        d1NodeTest.mnCreate(session, resourceMapId, object3, sysmeta3);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //make sure the result map was indexed
        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }

        //Get a doi and put it into the series id
        Identifier doi = MNodeService.getInstance(request).generateIdentifier(session, "doi", null);
        SystemMetadata readSysmeta = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        readSysmeta.setSeriesId(doi);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, readSysmeta);
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("seriesId")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("seriesId"));
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, doi);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //publishIdentifier the metadata id
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));

        //the metadata identifiers (pid and sid) are public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
        MNodeService.getInstance(request).getSystemMetadata(publicSession, doi);

        //the resource map is public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);

        //the data object is still not public readable
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
    }
}
