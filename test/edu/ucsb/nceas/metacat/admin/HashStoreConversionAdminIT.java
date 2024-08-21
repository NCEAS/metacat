package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Tao
 * The Junit test class for HashStoreConversionAdmin
 */
public class HashStoreConversionAdminIT {

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }

    /**
     * Test the getCandidateUpdateClass method
     * @throws Exception
     */
    @Test
    public void testGetCandidateUpdateClass() throws Exception {
        HashStoreConversionAdmin.initVersionAndClassFromProperty();
        assertTrue(
            HashStoreConversionAdmin.versionAndClassMapInProperty.keySet().contains("3.1.0"));
        assertEquals(
            "edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader",
            HashStoreConversionAdmin.versionAndClassMapInProperty.get("3.1.0"));
        assertEquals(1, HashStoreConversionAdmin.versionAndClassMapInProperty.size());
    }

    @Test
    public void testGetVersionFromPropertyName() {
        String propertyName = "solr.upgradeUtility.3.0.0";
        String propertyPrefix = "solr.upgradeUtility";
        assertEquals("3.0.0", HashStoreConversionAdmin.getVersionFromPropertyName(propertyName,
                                                                                  propertyPrefix));
    }

    @Test
    public void testInitVersionAndClassFromProperty() throws Exception {
        HashStoreConversionAdmin.initVersionAndClassFromProperty();
        assertEquals(1, HashStoreConversionAdmin.versionAndClassMapInProperty.size());
        assertEquals(
            "edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader",
            HashStoreConversionAdmin.versionAndClassMapInProperty.get("3.1.0"));
    }

    @Test
    public void test() throws Exception {

    }
}
