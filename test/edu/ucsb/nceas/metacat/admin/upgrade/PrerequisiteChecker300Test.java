package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.DBAdmin;
import edu.ucsb.nceas.metacat.database.DBVersion;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PrerequisiteChecker300Test {

    private PrerequisiteChecker300 checker;
    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        checker = new PrerequisiteChecker300();
    }
    /**
     * Test the check method
     * @throws Exception
     */
    @Test
    public void testCheck() throws Exception {
        try (MockedStatic<DBAdmin> ingored = Mockito.mockStatic(DBAdmin.class)) {
            DBAdmin mockDBAdmin = Mockito.mock(DBAdmin.class);
            Mockito.when(DBAdmin.getInstance()).thenReturn(mockDBAdmin);
            //return a new fresh database. The check will pass (without exception)
            Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(new DBVersion("0.0.0"));
            checker.check();
            //return a 1.2.0 version. The check will throw an exception 
            Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(new DBVersion("1.2.0"));
            try {
                checker.check();
                fail("Test can't reach here since it should throw an exception.");
            } catch (Exception e) {
                assertTrue("The exception class " + e.getClass().getName()
                                    + " should be AdminException ", e instanceof AdminException);
                assertTrue("The error message " + e.getMessage()
                            + " should have the information to generate system metadata",
                            e.getMessage().contains("system metadata"));
                assertTrue("The error message " + e.getMessage()
                            + " should have the information indicating start from 2.19.0",
                            e.getMessage().contains("2.19.0"));
            }
            //return a 1.2.0 version. The check will throw an exception 
            Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(new DBVersion("1.2.0"));
            try {
                checker.check();
                fail("Test can't reach here since it should throw an exception.");
            } catch (Exception e) {
                assertTrue("The exception class " + e.getClass().getName()
                                    + " should be AdminException ", e instanceof AdminException);
                assertTrue("The error message " + e.getMessage()
                            + " should have the information to generate system metadata",
                            e.getMessage().contains("system metadata"));
                assertTrue("The error message " + e.getMessage()
                            + " should have the information indicating start from 2.19.0",
                            e.getMessage().contains("2.19.0"));
            }
            //return a 1.9.5 version. The check will throw an exception 
            Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(new DBVersion("1.9.5"));
            try {
                checker.check();
                fail("Test can't reach here since it should throw an exception.");
            } catch (Exception e) {
                assertTrue("The exception class " + e.getClass().getName()
                                    + " should be AdminException ", e instanceof AdminException);
                assertTrue("The error message " + e.getMessage()
                            + " should have the information to generate system metadata",
                            e.getMessage().contains("system metadata"));
                assertTrue("The error message " + e.getMessage()
                            + " should have the information indicating start from 2.19.0",
                            e.getMessage().contains("2.19.0"));
            }
            //return a 2.0.0 version. The check will throw an exception 
            Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(new DBVersion("2.0.0"));
            try {
                checker.check();
                fail("Test can't reach here since it should throw an exception.");
            } catch (Exception e) {
                assertTrue("The exception class " + e.getClass().getName()
                                    + " should be AdminException ", e instanceof AdminException);
                assertFalse("The error message " + e.getMessage()
                            + " should NOT have the information to generate system metadata",
                            e.getMessage().contains("system metadata"));
                assertTrue("The error message " + e.getMessage()
                            + " should have the information indicating start from 2.19.0",
                            e.getMessage().contains("2.19.0"));
            }
            //return a 2.5.0 version. The check will throw an exception 
            Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(new DBVersion("2.5.0"));
            try {
                checker.check();
                fail("Test can't reach here since it should throw an exception.");
            } catch (Exception e) {
                assertTrue("The exception class " + e.getClass().getName()
                                    + " should be AdminException ", e instanceof AdminException);
                assertFalse("The error message " + e.getMessage()
                            + " should NOT have the information to generate system metadata",
                            e.getMessage().contains("system metadata"));
                assertTrue("The error message " + e.getMessage()
                            + " should have the information indicating start from 2.19.0",
                            e.getMessage().contains("2.19.0"));
            }
            //return a 2.19.0 version. The check will go through
            Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(new DBVersion("2.19.0"));
            checker.check();
            //return a 3.0.0 version. The check will go through
            Mockito.when(mockDBAdmin.getDBVersion()).thenReturn(new DBVersion("3.0.0"));
            checker.check();
        }
    }

}
