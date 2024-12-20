package edu.ucsb.nceas.metacat.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DBVersion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import static org.mockito.Mockito.CALLS_REAL_METHODS;

/**
 * An integrated test class for the SolrAmdinIT
 * @author tao
 *
 */
public class SolrAdminIT {

    private static String version;
    private static boolean originStatus;
    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
        originStatus = getSolrUpgradedStatus(version);
        version = PropertyService.getProperty("application.metacatVersion");
    }

    @After
    public void tearDown() throws Exception {
        //set backup to original status
        SolrAdmin.updateSolrStatus(version, originStatus);
    }

    /**
     * Test the updateSolrStatus method
     * @throws Exception
     */
    @Test
    public void testUpdateSolrStatus() throws Exception {
        boolean newStatus = !originStatus;
        SolrAdmin.updateSolrStatus(version, newStatus);
        boolean status = getSolrUpgradedStatus(version);
        assertEquals("The solr upgraded status for version " + version + " should be " + newStatus
                       + " rather than" + status , newStatus, status);
        SolrAdmin.updateSolrStatus(version, originStatus);
        status = getSolrUpgradedStatus(version);
        assertEquals("The solr upgraded status for version " + version + " should be "
                        + originStatus + " rather than" + status , originStatus, status);
    }

    /**
     * Get the status of solrUpgraded from db_version
     * @param version  the given version which will be looked at
     * @return  true if it has been upgraded; otherwise false.
     * @throws SQLException
     */
    private static boolean getSolrUpgradedStatus(String version) throws SQLException {
        boolean upgraded = false;
        DBConnection dbConn = null;
        int serialNumber = -1;
        String sql = "SELECT solr_upgraded FROM version_history WHERE version = ?";
        try {
            dbConn = DBConnectionPool.getDBConnection("SolrAdmin.isFreshInstall");
            serialNumber = dbConn.getCheckOutSerialNumber();
            try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
                pstmt.setString(1, version);
                pstmt.execute();
                try (ResultSet rs = pstmt.getResultSet()) {
                    if (rs.next()) {
                        upgraded = rs.getBoolean(1);
                    }
                }
            }
        } finally {
            if (dbConn != null) {
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }
        }
        return upgraded;
    }

    /**
     * Test the isFreshInstall method
     * @throws Exception
     */
    @Test
    public void testIsFreshInstall() throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        int count = 0;
        String sql = "SELECT * FROM version_history";
        try {
            dbConn = DBConnectionPool.getDBConnection("SolrAdmin.isFreshInstall");
            serialNumber = dbConn.getCheckOutSerialNumber();
            try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
                pstmt.execute();
                try (ResultSet rs = pstmt.getResultSet()) {
                    while (rs.next()) {
                        count++;
                    }
                    if (count == 1) {
                        assertTrue("It should be a fresh installation", SolrAdmin.isFreshInstall());
                    } else {
                        assertFalse("It should be not a fresh installation",
                                                                        SolrAdmin.isFreshInstall());
                    }
                }
            }
        } finally {
            if (dbConn != null) {
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }
        }
    }

    /**
     * Test the getUnupgradedSolrVersions method
     * @throws Exception
     */
    @Test
    public void testGetUnupgradedSolrVersions() throws Exception {
        DBVersion dbVersion = new DBVersion(version);
        Vector<DBVersion> list;
        if (originStatus) {
            list = SolrAdmin.getNonUpgradedSolrVersions();
            assertFalse("Since version " + version + " is upgraded, it shouldn't be in the needed "
                                                 + "upgrade versions", contained(list, dbVersion));
        } else {
            list = SolrAdmin.getNonUpgradedSolrVersions();
            assertTrue("Since version " + version + " is not upgraded, it should be in the needed "
                                                 + "upgrade versions", contained(list, dbVersion));
        }
        //set a new status
        boolean newStatus = !originStatus;
        SolrAdmin.updateSolrStatus(version, newStatus);
        if (newStatus) {
            list = SolrAdmin.getNonUpgradedSolrVersions();
            assertFalse("Since version " + version + " is upgraded, it shouldn't be in the needed "
                                                + "upgrade versions", contained(list, dbVersion));
        } else {
            list = SolrAdmin.getNonUpgradedSolrVersions();
            assertTrue("Since version " + version + " is not upgraded, it should be in the needed "
                                                + "upgrade versions", contained(list, dbVersion));
        }
    }

    /**
     * Check if a target version is in the list
     * @param versions  the list containing the candidate versions
     * @param targetVersion  the target version will be searched
     * @return true if the target version is found in the list; otherwise false.
     * @throws Exception
     */
    private boolean contained(Vector<DBVersion> versions, DBVersion targetVersion) 
                                                                                throws Exception {
        boolean contained = false;
        if (versions == null || targetVersion == null) {
            throw new IllegalArgumentException("The parameters of DBVersion list or target "
                                                        + "DBVersion shouldn't be null");
        }
        for (DBVersion version : versions) {
            if(version.getVersionString().equals(targetVersion.getVersionString())) {
                contained = true;
            }
        }
        return contained;
    }

    /**
     * Test the getSolrUpdateClasses() method
     * @throws Exception
     */
    @Test
    public void testGetSolrUpdateClasses() throws Exception {
        try (MockedStatic<SolrAdmin> mocked =
                                        Mockito.mockStatic(SolrAdmin.class, CALLS_REAL_METHODS)) {
            // A fresh installation. So there are not solr update classes
            Mockito.when(SolrAdmin.isFreshInstall()).thenReturn(true);
            Map<String, String> updateClasses = SolrAdmin.getSolrUpdateClasses();
            assertEquals("Since it is fresh install, the solr upgrade class list should be 0.",
                                                                        updateClasses.size(), 0);

             // Not a fresh installation.
            Mockito.when(SolrAdmin.isFreshInstall()).thenReturn(false);
            if (originStatus) {
                updateClasses = SolrAdmin.getSolrUpdateClasses();
                assertFalse("Version " + version + " has been upgraded, it should not be in the "
                                + "upgrade list", updateClasses.keySet().contains(version));
            } else {
                updateClasses = SolrAdmin.getSolrUpdateClasses();
            }
            //set a new status
            boolean newStatus = !originStatus;
            SolrAdmin.updateSolrStatus(version, newStatus);
            if (newStatus) {
                updateClasses = SolrAdmin.getSolrUpdateClasses();
                assertFalse("Version " + version + " has been upgraded, it should not be in the "
                                + "upgrade list", updateClasses.keySet().contains(version));
            } else {
                updateClasses = SolrAdmin.getSolrUpdateClasses();
                assertTrue(
                    "Version " + version
                        + " has not been upgraded, it should be in the upgrade list",
                    updateClasses.keySet().contains(version));
            }
        }
    }

}
