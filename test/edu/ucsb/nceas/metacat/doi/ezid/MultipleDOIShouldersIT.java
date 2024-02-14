/**
 *  '$RCSfile$'
 *  Copyright: 2020 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
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
package edu.ucsb.nceas.metacat.doi.ezid;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;

import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.After;
import org.junit.Before;

import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.ezid.profile.DataCiteProfile;
import edu.ucsb.nceas.ezid.profile.InternalProfile;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.dataone.MockCNode;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * To test the scenario that Metacat supports multiple shoulders. 
 * The first shoulder is the primary one. It uses for both minting and registering DOIs.
 * The second and beyond shoulders are only for registering DOIs.
 * Currently the testing only works on the ezid stage environment. 
 * @author tao
 *
 */
public class MultipleDOIShouldersIT extends D1NodeServiceTest {
    private static final String PROPERTY_SHOULDER_1 = "guid.doi.doishoulder.1";
    private static final String PROPERTY_SHOULDER_2 = "guid.doi.doishoulder.2";
    private static final String SHOULDER_1 = "doi:10.18739/A2";
    private static final String SHOULDER_2 = "doi:10.5063/";
    
    /**
     * Constructor
     * @param name
     */
    public MultipleDOIShouldersIT(String name) {
        super(name);
    }
    
    /**
     * Set up the test fixtures
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // set up the configuration for d1client
        Settings.getConfiguration().setProperty("D1Client.cnClassName",
                MockCNode.class.getName());
        PropertyService.getInstance().setPropertyNoPersist(PROPERTY_SHOULDER_1, SHOULDER_1);
        PropertyService.getInstance().setPropertyNoPersist(PROPERTY_SHOULDER_2, SHOULDER_2);
    }

    /**
     * Remove the test fixtures
     */
    @After
    public void tearDown() {
    }

    /**
     * Build the test suite
     * @return
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new MultipleDOIShouldersIT("testPrimaryShoulder"));
        suite.addTest(new MultipleDOIShouldersIT("testSecondaryShoulder"));
        suite.addTest(new MultipleDOIShouldersIT("testNonExistedShoulder"));
        return suite;
    }
    
    /**
     * Test to mint and register an DOI by the primary (first) shoulder.
     * @throws Exception
     */
    public void testPrimaryShoulder() throws Exception {
        printTestHeader("testPrimaryShoulder");
        try {
            String ezidServiceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
            EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
          
            // Mint a DOI
            Session session = getTestSession();
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
            sysmeta = createSystemMetadata(guid, session.getSubject(), object);
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
            /*Set<String> keys = metadata.keySet();
            for (String key : keys) {
                System.out.println("=====the key " + key + " has the value of " + metadata.get(key));
            }*/
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
    public void testSecondaryShoulder() throws Exception {
        printTestHeader("testSecondShoulder");
        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        Identifier guid = new Identifier();
        guid.setValue(SHOULDER_2 + uuidStr);
        System.out.println("the guid is " + guid.getValue());
        
        Session session = getTestSession();
        SystemMetadata sysmeta = null;
        InputStream object = new FileInputStream(RegisterDOITest.EMLFILEPATH);
        sysmeta = createSystemMetadata(guid, session.getSubject(), object);
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
        /*Set<String> keys = metadata.keySet();
        for (String key : keys) {
            System.out.println("=====the key " + key + " has the value of " + metadata.get(key));
        }*/
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
    public void testNonExistedShoulder() throws Exception {
        printTestHeader("testNonExistedShoulder");
        UUID shoulderUuid = UUID.randomUUID();
        String shoulder = "doi:99.000/" + shoulderUuid.toString() + "/";
        
        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        Identifier guid = new Identifier();
        guid.setValue(shoulder + uuidStr);
        System.out.println("the guid is " + guid.getValue());
        
        Session session = getTestSession();
        SystemMetadata sysmeta = null;
        InputStream object = new FileInputStream(RegisterDOITest.EMLFILEPATH);
        sysmeta = createSystemMetadata(guid, session.getSubject(), object);
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
