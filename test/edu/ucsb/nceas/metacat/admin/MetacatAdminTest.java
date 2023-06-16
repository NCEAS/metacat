package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * A JUnit test for testing the MetacatAdmin class
 */
public class MetacatAdminTest {

    private static final String DB_PROP = "configutil.upgrade.database.status";
    private static final String JAVA_PROP = "configutil.upgrade.java.status";
    private static final String SOLR_PROP = "configutil.upgrade.solr.status";
    private static final String ALL_PROP = "configutil.upgrade.status";
    private static final String NOT_SET = "";
    private static final boolean DO_NOT_PERSIST = false;

    @Before
    public void setUp() {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }

    /**
     * A test that will fail if anyone adds more of these configutil.upgrade.* properties
     * without adding them to the testcases here
     */
    @Test
    public void ensureAllPropertiesAreTested() throws Exception {

        List<String> existingPropsList = Arrays.asList(
            DB_PROP,
            JAVA_PROP,
            SOLR_PROP,
            ALL_PROP
        );
        Set<String> names =
            PropertyService.getPropertiesByGroup("configutil.upgrade").keySet();
        for (String name : names) {
            if (!existingPropsList.contains(name)) {
                fail("This property does not have a test associated with it: configutil.upgrade."
                + name + ". Please add it to the testcases in MetacatAdminTest!");
            }
        }
        List<String> incompletePropsList = Arrays.asList(DB_PROP, ALL_PROP);
        assertFalse(incompletePropsList.containsAll(names));
    }

    /**
     * Test the method of updateUpgradeStatus. Full list as of June 2023:
     * <code>
     *   configutil.upgrade.status=
     *   configutil.upgrade.database.status=
     *   configutil.upgrade.java.status=
     *   configutil.upgrade.solr.status=
     * </code>
     * Possible status values are "success", "failure" and "in_progress".
     * 'configutil.upgrade.status' is the overall indicator that the other statuses are all
     * "success" so:
     * - If status for database, java & solr all == "success", overall status == "success"
     * - If status for any of database, java & solr all != "success", then overall status
     *   should be either "failure" or "in_progress", depending on which was updated most recently
     */
    @Test
    public void testUpdateUpgradeStatus() throws Exception {

        // 0. ensure starting conditions: all props (db, java, solr, overall) are NOT SET (i.e.
        // == "")
        assertStatuses_db_java_solr_all(NOT_SET, NOT_SET, NOT_SET, NOT_SET);
        // (STATUS SUMMARY AT THIS POINT: db="", java="", solr="", overall="")

        // 1. set db to "in progress"; overall should update to "in_progress" too
        MetacatAdmin.updateUpgradeStatus(DB_PROP, MetacatAdmin.IN_PROGRESS, DO_NOT_PERSIST);

        //    (STATUS SUMMARY AT THIS POINT: db=ip, java="", solr="", overall=ip)
        assertStatuses_db_java_solr_all(
            MetacatAdmin.IN_PROGRESS, NOT_SET, NOT_SET, MetacatAdmin.IN_PROGRESS);


        // 2. set same one (db) to "failure"; overall should update to "failure" too
        MetacatAdmin.updateUpgradeStatus(DB_PROP, MetacatAdmin.FAILURE, DO_NOT_PERSIST);
        //    (STATUS SUMMARY AT THIS POINT: db=f, java="", solr="", overall=f)

        assertStatuses_db_java_solr_all(
            MetacatAdmin.FAILURE, NOT_SET, NOT_SET, MetacatAdmin.FAILURE);


        // 3. set a different one (java) to "success"; overall should stay as "failure"
        MetacatAdmin.updateUpgradeStatus(JAVA_PROP, MetacatAdmin.SUCCESS, DO_NOT_PERSIST);

        //    (STATUS SUMMARY AT THIS POINT: db=f, java=s, solr="", overall=f)
        assertStatuses_db_java_solr_all(
            MetacatAdmin.FAILURE, MetacatAdmin.SUCCESS, NOT_SET, MetacatAdmin.FAILURE);


        // 4. set a db to "success"; overall should stay as "failure"
        MetacatAdmin.updateUpgradeStatus(DB_PROP, MetacatAdmin.SUCCESS, DO_NOT_PERSIST);

        //    (STATUS SUMMARY AT THIS POINT: db=s, java=s, solr="", overall=f)
        assertStatuses_db_java_solr_all(
            MetacatAdmin.SUCCESS, MetacatAdmin.SUCCESS, NOT_SET, MetacatAdmin.FAILURE);


        // 5. set solr to "in progress"; overall should update to "in_progress" too
        MetacatAdmin.updateUpgradeStatus(SOLR_PROP, MetacatAdmin.IN_PROGRESS, DO_NOT_PERSIST);

        //    (STATUS SUMMARY AT THIS POINT: db=s, java=s, solr=ip, overall=ip)
        assertStatuses_db_java_solr_all(MetacatAdmin.SUCCESS, MetacatAdmin.SUCCESS,
                                        MetacatAdmin.IN_PROGRESS, MetacatAdmin.IN_PROGRESS);


        // 6. finally, set solr to "success"; overall should update to "success" too
        MetacatAdmin.updateUpgradeStatus(SOLR_PROP, MetacatAdmin.SUCCESS, DO_NOT_PERSIST);

        //    (STATUS SUMMARY AT THIS POINT: db=s, java=s, solr=s, overall=s)
        assertStatuses_db_java_solr_all(
            MetacatAdmin.SUCCESS, MetacatAdmin.SUCCESS, MetacatAdmin.SUCCESS, MetacatAdmin.SUCCESS);
    }

    private void assertStatuses_db_java_solr_all(
        String db, String java, String solr, String overall) {
        try {
            assertEquals("Wrong value for " + DB_PROP, db, PropertyService.getProperty(DB_PROP));
            assertEquals("Wrong value for " + JAVA_PROP, java, PropertyService.getProperty(JAVA_PROP));
            assertEquals("Wrong value for " + SOLR_PROP, solr, PropertyService.getProperty(SOLR_PROP));
            assertEquals("Wrong value for " + ALL_PROP, overall, PropertyService.getProperty(ALL_PROP));
        } catch (PropertyNotFoundException e) {
            fail("unexpected PropertyNotFoundException: " + e.getMessage());
        }
    }
}
