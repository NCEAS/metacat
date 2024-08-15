package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Tao
 * The Junit test class for HashStoreConversionAdmin
 */
public class HashStoreConversionAdminTest {

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
        Map<String, String> versionsAndClasses =
            HashStoreConversionAdmin.getCandidateUpdateClasses("storage.upgradeUtility");
        assertTrue(versionsAndClasses.keySet().contains("3.1.0"));
        assertEquals(
            "edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader",
            versionsAndClasses.get("3.1.0"));
    }
}
