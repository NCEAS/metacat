package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Tao
 * The Junit test class for HashStoreConversionAdmin
 */
public class HashStoreConversionAdminIT {

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
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
    public void testGenerateFinalMapForFreshInstallation() throws Exception {
        String currentVersion = SystemUtil.getMetacatVersion().getVersionString();
        UpdateStatus currentStatus = HashStoreConversionAdmin.getStatus(currentVersion);
        try {
            try (MockedStatic<DBAdmin> ingore = Mockito.mockStatic(DBAdmin.class)) {
                Vector<String> versions = new Vector<>();
                versions.add(DBAdmin.VERSION_000);
                Mockito.when(DBAdmin.getNeededUpgradedVersions()).thenReturn(versions);
                HashStoreConversionAdmin.generateFinalVersionsAndClassesMap();
                assertEquals(0, HashStoreConversionAdmin.finalVersionAndClassMap.size());
                assertEquals(
                    UpdateStatus.NOT_REQUIRED, HashStoreConversionAdmin.getStatus(currentVersion));
            }
        } finally {
            // Reset back the status
            HashStoreConversionAdmin.setStatus(currentVersion, currentStatus);
        }
    }

    @Test
    public void testGenerateFinalMapForRestartTomcat() throws Exception {
        String currentVersion = SystemUtil.getMetacatVersion().getVersionString();
        UpdateStatus currentStatus = HashStoreConversionAdmin.getStatus(currentVersion);
        try {
            try (MockedStatic<DBAdmin> ingore = Mockito.mockStatic(DBAdmin.class)) {
                Vector<String> versions = new Vector<>();
                Mockito.when(DBAdmin.getNeededUpgradedVersions()).thenReturn(versions);
                HashStoreConversionAdmin.generateFinalVersionsAndClassesMap();
                if (currentStatus == UpdateStatus.UNKNOWN || currentStatus == UpdateStatus.PENDING) {
                    assertEquals(1, HashStoreConversionAdmin.finalVersionAndClassMap.size());
                    assertEquals(
                        UpdateStatus.PENDING, HashStoreConversionAdmin.getStatus(currentVersion));
                } else if (currentStatus == UpdateStatus.FAILED) {
                    assertEquals(1, HashStoreConversionAdmin.finalVersionAndClassMap.size());
                    assertEquals(
                        currentStatus, HashStoreConversionAdmin.getStatus(currentVersion));
                } else {
                    assertEquals(0, HashStoreConversionAdmin.finalVersionAndClassMap.size());
                    assertEquals(
                        currentStatus, HashStoreConversionAdmin.getStatus(currentVersion));
                }

            }
        } finally {
            // Reset back the status
            HashStoreConversionAdmin.setStatus(currentVersion, currentStatus);
        }
    }
}
