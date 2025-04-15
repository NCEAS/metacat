package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Properties;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.never;

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
        UpgradeStatus currentStatus = HashStoreConversionAdmin.getStatus(currentVersion);
        try {
            try (MockedStatic<DBAdmin> ingore = Mockito.mockStatic(DBAdmin.class)) {
                Vector<String> versions = new Vector<>();
                versions.add(DBAdmin.VERSION_000);
                Mockito.when(DBAdmin.getNeededUpgradedVersions()).thenReturn(versions);
                HashStoreConversionAdmin.generateFinalVersionsAndClassesMap();
                assertEquals(0, HashStoreConversionAdmin.finalVersionAndClassMap.size());
                assertEquals(
                    UpgradeStatus.NOT_REQUIRED, HashStoreConversionAdmin.getStatus(currentVersion));
            }
        } finally {
            // Reset back the status
            HashStoreConversionAdmin.setStatus(currentVersion, currentStatus);
        }
    }

    @Test
    public void testGenerateFinalMapForRestartTomcat() throws Exception {
        String currentVersion = SystemUtil.getMetacatVersion().getVersionString();
        UpgradeStatus currentStatus = HashStoreConversionAdmin.getStatus(currentVersion);
        try {
            try (MockedStatic<DBAdmin> ingore = Mockito.mockStatic(DBAdmin.class)) {
                Vector<String> versions = new Vector<>();
                Mockito.when(DBAdmin.getNeededUpgradedVersions()).thenReturn(versions);
                HashStoreConversionAdmin.generateFinalVersionsAndClassesMap();
                if (currentStatus == UpgradeStatus.UNKNOWN || currentStatus == UpgradeStatus.PENDING) {
                    assertEquals(1, HashStoreConversionAdmin.finalVersionAndClassMap.size());
                    assertEquals(
                        UpgradeStatus.PENDING, HashStoreConversionAdmin.getStatus(currentVersion));
                } else if (currentStatus == UpgradeStatus.FAILED) {
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

    @Test
    public void testDisableHashStoreConversion() throws Exception {
        Properties withProperties = new Properties();
        withProperties.setProperty("storage.hashstore.disableConversion", "true");
        LeanTestUtils.initializeMockPropertyService(withProperties);
        try (MockedStatic<HashStoreConversionAdmin> mocked =
                 Mockito.mockStatic(HashStoreConversionAdmin.class, CALLS_REAL_METHODS)) {
            HashStoreConversionAdmin.convert();
            mocked.verify(() -> HashStoreConversionAdmin.generateFinalVersionsAndClassesMap(),
                          never());
        }
    }

    @Test
    public void testUpdateInProgressStatus() throws Exception {
        DBConnection conn = null;
        int serialNumber = -1;
        String version;
        UpgradeStatus originalStatus;
        //Get original version/status pair
        try {
            // check out DBConnection
            conn =
                DBConnectionPool.getDBConnection("HashStoreConversionAdminIT"
                                                     + ".testUpdateInProgressStatus");
            serialNumber = conn.getCheckOutSerialNumber();
            try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT version, storage_upgrade_status FROM version_history")) {
                ResultSet resultSet = pstmt.executeQuery();
                if (resultSet.next()) {
                    version = resultSet.getString(1);
                    String status = (String) resultSet.getObject(2);
                    originalStatus = UpgradeStatus.getStatus(status);
                } else {
                    throw new Exception("We can't find anything in the version_history table.");
                }
            }
        } finally {
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
        try {
            // check out DBConnection
            conn =
                DBConnectionPool.getDBConnection("HashStoreConversionAdminIT"
                                                     + ".testUpdateInProgressStatus");
            serialNumber = conn.getCheckOutSerialNumber();
            //Set the status of the version to in_progress
            try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE version_history SET storage_upgrade_status=? WHERE version=?")) {
                pstmt.setObject(1, UpgradeStatus.IN_PROGRESS.getValue(), Types.OTHER);
                pstmt.setString(2, version);
                pstmt.executeUpdate();
            }
            assertEquals(UpgradeStatus.IN_PROGRESS.getValue(),
                         HashStoreConversionAdmin.getStatus(version).getValue());
            //Modify the in_progress status to complete
            HashStoreConversionAdmin.updateInProgressStatus(UpgradeStatus.COMPLETE);
            assertEquals(UpgradeStatus.COMPLETE.getValue(),
                         HashStoreConversionAdmin.getStatus(version).getValue());
        } finally {
            HashStoreConversionAdmin.setStatus(version, originalStatus);
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
        assertEquals(originalStatus.getValue(),
                     HashStoreConversionAdmin.getStatus(version).getValue());
    }
}
