package edu.ucsb.nceas.metacat.admin.upgrade;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.doi.ezid.EzidDOIService;
import edu.ucsb.nceas.metacat.doi.ezid.RegisterDOIIT;
import edu.ucsb.nceas.metacat.properties.PropertyService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UpdateDOIIT {
    private static final String UPDATETIMEKEY = "_updated";
    MockedStatic<PropertyService> closeableMock;
    private D1NodeServiceTest d1NodeTest;
    private MockHttpServletRequest request;


    @Before
    public void setUp() throws Exception {
        d1NodeTest = new D1NodeServiceTest("initialize");
        request = (MockHttpServletRequest)d1NodeTest.getServletRequest();
        d1NodeTest.setUp();
        final String passwdMsg =
            """
            \n* * * * * * * * * * * * * * * * * * *
            DOI PASSWORD IS NOT SET!
            Add a value for 'guid.doi.password'
            to your metacat-site.properties file!
            * * * * * * * * * * * * * * * * * * *
            """;
        try {
            assertFalse(passwdMsg, PropertyService.getProperty("guid.doi.password").isBlank());
        } catch (PropertyNotFoundException e) {
            fail(passwdMsg);
        }
        Properties withProperties = new Properties();
        withProperties.setProperty("server.name", "UpdateDOITestMock.edu");
        withProperties.setProperty("guid.doi.enabled", "true");
        withProperties.setProperty("guid.doi.username", "apitest");
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
    }

    @After
    public void tearDown() {
        try {
            closeableMock.close();
        } catch (Exception e) {
            //no need to handle - just a housekeeping failure
        }
        d1NodeTest.tearDown();
    }


    /*
     * Test the update of a pid and sid
     */
    @Test
    public void testUpdate() throws Exception {
     // get ezid config properties
        String ezidUsername = PropertyService.getProperty("guid.doi.username");
        String ezidPassword = PropertyService.getProperty("guid.doi.password");
        String ezidServiceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
        Session session = d1NodeTest.getTestSession();
        String emlFile = "test/eml-multiple-creators.xml";
        InputStream content = null;
        //Test the case that the identifier is a doi but no sid.
        String scheme = "DOI";
        String publishedPIDStr = null;
        String publishedSIDStr = null;
        long PIDUpdateTime = 0;
        long SIDUpdateTime = 0;
        try {
            Identifier publishedPID = MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
            publishedPIDStr = publishedPID.getValue();
            content = new FileInputStream(emlFile);
            SystemMetadata sysmeta = D1NodeServiceTest
                                .createSystemMetadata(publishedPID, session.getSubject(), content);
            content.close();
            sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
            content = new FileInputStream(emlFile);
            Identifier pid = d1NodeTest.mnCreate(session, publishedPID, content, sysmeta);
            content.close();
            assertEquals(publishedPID.getValue(), pid.getValue());
            // check for the metadata explicitly, using ezid service
            EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
            ezid.login(ezidUsername, ezidPassword);
            int count = 0;
            HashMap<String, String> metadata = null;
            do {
                metadata = ezid.getMetadata(publishedPID.getValue());
                Thread.sleep(2000);
                count++;
            } while ((metadata == null || metadata.get(EzidDOIService.DATACITE) == null) && count < 30);
            assertNotNull(metadata);
            String result = metadata.get(EzidDOIService.DATACITE);
            assertTrue(result.contains("Test EML package - public-readable from morpho"));
            assertTrue(result.contains(RegisterDOIIT.creatorsStr));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            String year = sdf.format(sysmeta.getDateUploaded());
            assertTrue(result.contains(year));
            assertTrue(result.contains("Dataset"));
            String updateTime = metadata.get(UPDATETIMEKEY);
            PIDUpdateTime = Long.parseLong(updateTime);
            content.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(content);
        }

        //Test the case that the identifier is non-doi but the sid is an doi
        try {
            Identifier guid = new Identifier();
            guid.setValue("tesCreateDOIinSid." + System.currentTimeMillis());
            Identifier publishedSID = MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
            publishedSIDStr = publishedSID.getValue();
            content = new FileInputStream(emlFile);
            SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), content);
            content.close();
            sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
            sysmeta.setSeriesId(publishedSID);
            content = new FileInputStream(emlFile);
            Identifier pid = d1NodeTest.mnCreate(session, guid, content, sysmeta);
            content.close();
            assertEquals(guid.getValue(), pid.getValue());
            // check for the metadata explicitly, using ezid service
            EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
            ezid.login(ezidUsername, ezidPassword);
            int count = 0;
            HashMap<String, String> metadata = null;
            do {
                metadata = ezid.getMetadata(publishedSID.getValue());
                Thread.sleep(2000);
                count++;
            } while ((metadata == null || metadata.get(EzidDOIService.DATACITE) == null) && count < 30);

            assertNotNull(metadata);
            String result = metadata.get(EzidDOIService.DATACITE);
            assertTrue(result.contains("Test EML package - public-readable from morpho"));
            assertTrue(result.contains(RegisterDOIIT.creatorsStr));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            String year = sdf.format(sysmeta.getDateUploaded());
            assertTrue(result.contains(year));
            assertTrue(result.contains("Dataset"));
            String updateTime = metadata.get(UPDATETIMEKEY);
            SIDUpdateTime = Long.parseLong(updateTime);
            content.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(content);
        }

        //update the datacite metadata by ids.
        Vector<String> ids = new Vector<String>();
        ids.add(publishedSIDStr);
        ids.add(publishedPIDStr);
        UpdateDOI updater = new UpdateDOI();
        updater.upgradeById(ids);
        Thread.sleep(8000);
        //make sure the pid's update changed, which means updating happened
        EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
        ezid.login(ezidUsername, ezidPassword);
        int count = 0;
        HashMap<String, String> metadata = null;
        String updateTime = null;
        long pidUpdate1 = -1;
        do {
            metadata = ezid.getMetadata(publishedPIDStr);
            updateTime = metadata.get(UPDATETIMEKEY);
            pidUpdate1 = Long.parseLong(updateTime);
            Thread.sleep(2000);
            count++;
        } while (pidUpdate1 ==  PIDUpdateTime && count < 30);
        assertNotNull(metadata);
        updateTime = metadata.get(UPDATETIMEKEY);
        pidUpdate1 = Long.parseLong(updateTime);
        assertTrue(pidUpdate1 > PIDUpdateTime);

        long sidUpdate1 = -1;
        do {
            metadata = ezid.getMetadata(publishedSIDStr);
            updateTime = metadata.get(UPDATETIMEKEY);
            sidUpdate1 = Long.parseLong(updateTime);
            Thread.sleep(2000);
            count++;
        } while ((sidUpdate1 == SIDUpdateTime) && count < 30);
        assertNotNull(metadata);
        assertTrue(sidUpdate1 > SIDUpdateTime);
    }
}
