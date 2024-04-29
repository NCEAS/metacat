package edu.ucsb.nceas.metacat.doi.ezid;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.ezid.profile.DataCiteProfile;
import edu.ucsb.nceas.ezid.profile.InternalProfile;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * To test the scenario that Metacat supports multiple shoulders.
 * The first shoulder is the primary one. It uses for both minting and registering DOIs.
 * The second and beyond shoulders are only for registering DOIs.
 * Currently the testing only works in the ezid stage environment.
 * In order to make this test class work, you need change the doi username/password
 * in the test/test.properties file.
 * @author tao
 *
 */
public class MultipleDOIShouldersIT {
    private static final String PROPERTY_SHOULDER_1 = "guid.doi.doishoulder.1";
    private static final String PROPERTY_SHOULDER_2 = "guid.doi.doishoulder.2";
    private static final String SHOULDER_1 = "doi:10.18739/A2";
    private static final String SHOULDER_2 = "doi:10.5063/";

    private D1NodeServiceTest d1NodeServiceTest;
    private MockHttpServletRequest request;
    private MockedStatic<PropertyService> closeableMock;

    /**
     * Set up the test fixtures
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        d1NodeServiceTest = new D1NodeServiceTest("initialize");
        final String passwdMsg =
                """
                \n* * * * * * * * * * * * * * * * * * *
                DOI CREDENTIALS NOT SET!
                Test requires specific values for
                'guid.doi.username' & 'guid.doi.password'
                in your test/test.properties file!
                * * * * * * * * * * * * * * * * * * *
                """;
        Properties testProperties = LeanTestUtils.getExpectedProperties();
        String user = testProperties.getProperty("guid.doi.username");
        String password = testProperties.getProperty("guid.doi.password");
        assertNotNull(passwdMsg, user);
        assertFalse(passwdMsg, user.isBlank());
        assertNotEquals(passwdMsg, "apitest", user);
        assertNotNull(passwdMsg, password);
        assertFalse(passwdMsg, password.isBlank());
        Properties withProperties = new Properties();
        withProperties.setProperty("guid.doi.enabled", "true");
        withProperties.setProperty("guid.doi.baseurl", "https://ezid-stg.cdlib.org/");
        withProperties.setProperty(PROPERTY_SHOULDER_1, SHOULDER_1);
        withProperties.setProperty(PROPERTY_SHOULDER_2 , SHOULDER_2);
        withProperties.setProperty("guid.doi.username", user);
        withProperties.setProperty("guid.doi.password", password);
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
        request = (MockHttpServletRequest)d1NodeServiceTest.getServletRequest();
    }

    /**
     * Remove the test fixtures
     */
    @After
    public void tearDown() {
        if (closeableMock != null) {
            closeableMock.close();
        }
    }


    /**
     * Test to mint and register an DOI by the primary (first) shoulder.
     * @throws Exception
     */
    @Test
    public void testPrimaryShoulder() throws Exception {
        D1NodeServiceTest.printTestHeader("testPrimaryShoulder");
        try {
            String ezidServiceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
            EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
          
            // Mint a DOI
            Session session = d1NodeServiceTest.getTestSession();
            Identifier guid = MNodeService.getInstance(request).generateIdentifier(session, "DOI", null);
            assertTrue(guid.getValue().startsWith(SHOULDER_1));
            
            // check that EZID knows about it
            HashMap<String, String> metadata = null;
            int count = 0;
            do {
                try {
                    metadata = ezid.getMetadata(guid.getValue());
                } catch (Exception e) {
                    Thread.sleep(RegisterDOITest.SLEEP_TIME);
                }
                count++;
            } while (metadata == null && count < RegisterDOITest.MAX_TIMES);
            assertNotNull(metadata);

            // add the actual object for the newly-minted DOI
            SystemMetadata sysmeta = null;
            InputStream object = new FileInputStream(RegisterDOITest.EMLFILEPATH);
            sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            object.close();
            object = new FileInputStream(RegisterDOITest.EMLFILEPATH);
            sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
            Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertEquals(guid.getValue(), pid.getValue());
            // check for the metadata for title element
            count = 0;
            metadata = null;
            do {
                try {
                    metadata = ezid.getMetadata(pid.getValue());
                    // check if the update thread finished yet, otherwise try again
                    if (metadata != null) {
                        String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
                        if (!registeredTarget.contains(pid.getValue())) {
                            // try fetching it again
                            metadata = null;
                        }
                    }
                } catch (Exception e) {
                    Thread.sleep(RegisterDOITest.SLEEP_TIME);
                }
                count++;
            } while (metadata == null && count < RegisterDOITest.MAX_TIMES);
            assertNotNull(metadata);
            assertTrue(metadata.containsKey(DataCiteProfile.TITLE.toString()));
            
            // check that the target URI was updated
            String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
            assertTrue(registeredTarget.contains(pid.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test to register an DOI with the secondary shoulder.
     * The DOI will be generated by ourself.
     * @throws Exception
     */
    @Test
    public void testSecondaryShoulder() throws Exception {
        D1NodeServiceTest.printTestHeader("testSecondShoulder");
        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        Identifier guid = new Identifier();
        guid.setValue(SHOULDER_2 + uuidStr);
        System.out.println("the guid is " + guid.getValue());
        
        Session session = d1NodeServiceTest.getTestSession();
        SystemMetadata sysmeta = null;
        InputStream object = new FileInputStream(RegisterDOITest.EMLFILEPATH);
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        object = new FileInputStream(RegisterDOITest.EMLFILEPATH);
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertEquals(guid.getValue(), pid.getValue());
        // check for the metadata for title element
        int count = 0;
        HashMap<String, String> metadata = null;
        String ezidServiceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
        EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
        do {
            try {
                metadata = ezid.getMetadata(pid.getValue());
                // check if the update thread finished yet, otherwise try again
                if (metadata != null) {
                    String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
                    if (!registeredTarget.contains(pid.getValue())) {
                        // try fetching it again
                        metadata = null;
                    }
                }
            } catch (Exception e) {
                Thread.sleep(RegisterDOITest.SLEEP_TIME);
            }
            count++;
        } while (metadata == null && count < RegisterDOITest.MAX_TIMES);
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("datacite"));
        String datacite = metadata.get("datacite");
        assertTrue(datacite.contains("Test EML package - public-readable from morpho"));
        // check that the target URI was updated
        String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
        assertTrue(registeredTarget.contains(pid.getValue()));
    }

    /**
     * Test to register an DOI with a shoulder which is not in the configuration file.
     * The DOI will be generated by ourself.
     * @throws Exception
     */
    @Test
    public void testNonExistedShoulder() throws Exception {
        D1NodeServiceTest.printTestHeader("testNonExistedShoulder");
        UUID shoulderUuid = UUID.randomUUID();
        String shoulder = "doi:99.000/" + shoulderUuid.toString() + "/";
        
        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        Identifier guid = new Identifier();
        guid.setValue(shoulder + uuidStr);
        System.out.println("the guid is " + guid.getValue());
        
        Session session = d1NodeServiceTest.getTestSession();
        SystemMetadata sysmeta = null;
        InputStream object = new FileInputStream(RegisterDOITest.EMLFILEPATH);
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        object = new FileInputStream(RegisterDOITest.EMLFILEPATH);
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertEquals(guid.getValue(), pid.getValue());
        // check for the metadata for title element
        int count = 0;
        HashMap<String, String> metadata = null;
        String ezidServiceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
        EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
        do {
            try {
                metadata = ezid.getMetadata(pid.getValue());
                // check if the update thread finished yet, otherwise try again
                if (metadata != null) {
                    String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
                    if (!registeredTarget.contains(pid.getValue())) {
                        // try fetching it again
                        metadata = null;
                    }
                }
            } catch (Exception e) {
                Thread.sleep(RegisterDOITest.SLEEP_TIME);
            }
            count++;
        } while (metadata == null && count < RegisterDOITest.MAX_TIMES);
        assertNull(metadata);
    }

}
