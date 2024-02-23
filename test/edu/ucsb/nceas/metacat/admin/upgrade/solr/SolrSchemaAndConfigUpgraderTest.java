package edu.ucsb.nceas.metacat.admin.upgrade.solr;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.service.ServiceService;

/**
 * A junit test for the class SolrUpgrade3_0_0
 * @author tao
 *
 */
public class SolrSchemaAndConfigUpgraderTest {

    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }

    /**
     * Test the upgrade method
     * @throws Exception
     */
    @Test
    public void testUpgrade() throws Exception {
        //ServiceService.getRealConfigDir
        try (MockedStatic<ServiceService> dummy = Mockito.mockStatic(ServiceService.class)) {
            dummy.when(() -> ServiceService.getRealConfigDir()).thenReturn("dummy");
            SolrConfigUpgrader mockConfigUpgrader = Mockito.mock(SolrConfigUpgrader.class);
            SolrSchemaUpgrader mockSchemaUpgrader = Mockito.mock(SolrSchemaUpgrader.class);
            //success
            Mockito.doNothing().when(mockConfigUpgrader).upgrade();
            Mockito.doNothing().when(mockSchemaUpgrader).upgrade();
            SolrSchemaAndConfigUpgrader upgrader = SolrSchemaAndConfigUpgrader.getInstance();
            upgrader.setSolrConfigUpgrader(mockConfigUpgrader);
            upgrader.setSolrSchemaUpgrader(mockSchemaUpgrader);
            upgrader.upgrade();
            //failure
            Mockito.doThrow(new IOException("fail to copy the configuration files"))
                                                              .when(mockConfigUpgrader).upgrade();
            try {
                upgrader.upgrade();
                fail("The test can't reach there since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof AdminException);
            }
            Mockito.doThrow(new IOException("fail to copy the schema files"))
                                                               .when(mockSchemaUpgrader).upgrade();
            try {
                upgrader.upgrade();
                fail("The test can't reach there since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof AdminException);
            }
        }
    }
}
