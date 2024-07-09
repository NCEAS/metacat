package edu.ucsb.nceas.metacat.storage;

import edu.ucsb.nceas.LeanTestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test the class the StorageFactory
 * @author tao
 */
public class StorageFactoryTest {
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }
    /**
     * Test the getStorage method
     * @throws Exception
     */
    @Test
    public void testGetStorage() throws Exception {
        Storage storage = StorageFactory.getStorage();
        assertTrue(storage instanceof HashStorage);
    }
}
