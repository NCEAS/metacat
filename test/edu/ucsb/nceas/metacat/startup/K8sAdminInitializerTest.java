package edu.ucsb.nceas.metacat.startup;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.admin.D1Admin;
import edu.ucsb.nceas.metacat.admin.DBAdmin;
import edu.ucsb.nceas.metacat.admin.HashStoreConversionAdmin;
import edu.ucsb.nceas.metacat.admin.UpgradeStatus;
import edu.ucsb.nceas.metacat.database.DBVersion;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.ServletException;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.withSettings;

public class K8sAdminInitializerTest {

    private static final String CONTAINERIZED = "METACAT_IN_K8S";


    @Test(expected = ServletException.class)
    public void initializeK8sInstance_exception() throws Exception {
        // Legacy mode
        LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "false");
        K8sAdminInitializer.initializeK8sInstance();
    }

    @Test
    public void verifyK8s() throws Exception {
        // Legacy mode
        LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "true");
        K8sAdminInitializer.verifyK8s();
        assertTrue("Should get here without throwing an exception", true);
    }

    @Test(expected = ServletException.class)
    public void verifyK8s_exception() throws Exception {
        // Legacy mode
        LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "false");
        K8sAdminInitializer.verifyK8s();
    }

    @Test
    public void initK8sDBConfig() throws Exception {

        final String V0 = "0.0.0";
        final String V2 = "2.19.0";
        final String V3 = "3.0.0";
        final String LATEST = "3.0.1";
        MetacatVersion mcV_LATEST = new MetacatVersion(LATEST);
        LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "true");

        try (MockedStatic<PropertyService> ignored = Mockito.mockStatic(PropertyService.class)) {
            PropertyService mockProps = Mockito.mock(PropertyService.class);
            Mockito.when(PropertyService.getInstance()).thenReturn(mockProps);
            Mockito.when(PropertyService.getProperty(anyString())).thenReturn("MOCK");

            DBVersion dbV_0 = new DBVersion(V0);
            DBVersion dbV_2 = new DBVersion(V2);
            DBVersion dbV_3 = new DBVersion(V3);
            DBVersion dbV_LATEST = new DBVersion(LATEST);

            try (MockedStatic<DBAdmin> ignored2 = Mockito.mockStatic(DBAdmin.class)) {
                DBAdmin mockDBAdmin = Mockito.mock(DBAdmin.class);
                Mockito.when(DBAdmin.getInstance()).thenReturn(mockDBAdmin);
                doNothing().when(mockDBAdmin).upgradeDatabase();

                try (MockedStatic<SystemUtil> ignored3 = Mockito.mockStatic(SystemUtil.class)) {

                    Mockito.when(SystemUtil.getMetacatVersion()).thenReturn(mcV_LATEST);

                    // starting condition: Verify that upgradeDatabase() not yet called (times =0)
                    Mockito.verify(mockDBAdmin, Mockito.times(0)).upgradeDatabase();

                    // DB v0.0.0 (new) & MC v3.0.1; Verify that upgradeDatabase() was called
                    // (times =1)
                    Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(dbV_0);
                    K8sAdminInitializer.initK8sDBConfig();
                    Mockito.verify(mockDBAdmin, Mockito.times(1)).upgradeDatabase();

                    // DB v2.19.0 & MC v3.0.1; Verify that upgradeDatabase() was called again
                    // (times =2)
                    Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(dbV_2);
                    K8sAdminInitializer.initK8sDBConfig();
                    Mockito.verify(mockDBAdmin, Mockito.times(2)).upgradeDatabase();

                    // DB v3.0.0 & MC v3.0.1; Verify that upgradeDatabase() was called again
                    // (times =3)
                    Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(dbV_3);
                    K8sAdminInitializer.initK8sDBConfig();
                    Mockito.verify(mockDBAdmin, Mockito.times(3)).upgradeDatabase();

                    // DB v3.0.1 & MC v3.0.1; Verify that upgradeDatabase() was NOT called (still
                    // =3)
                    Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(dbV_LATEST);
                    K8sAdminInitializer.initK8sDBConfig();
                    Mockito.verify(mockDBAdmin, Mockito.times(3)).upgradeDatabase();
                }
            }
        }
    }

    @Test
    public void initK8sD1Admin() throws Exception {

        try (MockedStatic<D1Admin> ignored = Mockito.mockStatic(D1Admin.class)) {
            D1Admin mockD1Admin = Mockito.mock(D1Admin.class);
            Mockito.when(D1Admin.getInstance()).thenReturn(mockD1Admin);
            doNothing().when(mockD1Admin).upRegD1MemberNode();

            // K8s mode
            Mockito.verify(mockD1Admin, Mockito.times(0)).upRegD1MemberNode();
            LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "true");
            K8sAdminInitializer.initK8sD1Admin();

            // Verify that upRegD1MemberNode() was called
            Mockito.verify(mockD1Admin, Mockito.times(1)).upRegD1MemberNode();
        }
    }

    @Test
    public void initK8sStorageUpgrade() throws Exception {
        LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "true");
        try (MockedStatic<HashStoreConversionAdmin> mockStoreAdmin = Mockito.mockStatic(
            HashStoreConversionAdmin.class,
            withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS))) {
            mockStoreAdmin.when(HashStoreConversionAdmin::getStatus)
                .thenReturn(UpgradeStatus.IN_PROGRESS);
            mockStoreAdmin.when(
                    () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)))
                .thenAnswer(invocation -> null);
            mockStoreAdmin.when(HashStoreConversionAdmin::convert)
                .thenAnswer(invocation -> null);
            K8sAdminInitializer.initK8sStorageUpgrade();
            mockStoreAdmin.verify(
                () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)),
                times(1));
            mockStoreAdmin.verify(HashStoreConversionAdmin::convert, times(1));
        }
        try (MockedStatic<HashStoreConversionAdmin> mockStoreAdmin = Mockito.mockStatic(
            HashStoreConversionAdmin.class,
            withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS))) {
            mockStoreAdmin.when(HashStoreConversionAdmin::getStatus)
                .thenReturn(UpgradeStatus.FAILED);
            mockStoreAdmin.when(
                    () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)))
                .thenAnswer(invocation -> null);
            mockStoreAdmin.when(HashStoreConversionAdmin::convert)
                .thenAnswer(invocation -> null);
            K8sAdminInitializer.initK8sStorageUpgrade();
            mockStoreAdmin.verify(
                () -> HashStoreConversionAdmin.updateInProgressStatus(any(UpgradeStatus.class)),
                times(0));
            mockStoreAdmin.verify(HashStoreConversionAdmin::convert, times(1));
        }
    }
}
