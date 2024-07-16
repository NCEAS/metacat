package edu.ucsb.nceas.metacat.systemmetadata;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.storage.ObjectInfo;
import edu.ucsb.nceas.metacat.storage.Storage;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test class for the ChecksumsManager
 * @author Tao
 */
public class ChecksumsManagerIT {
    private static Storage storage;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        if (storage == null) {
            synchronized(ChecksumsManagerIT.class) {
                if (storage == null) {
                    try {
                        MetacatInitializer.initStorage();
                        storage = MetacatInitializer.getStorage();
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Test the save method. The get and query methods are also tested
     * @throws Exception
     */
    @Test
    public void testSave() throws Exception {
        Identifier pid = new Identifier();
        pid.setValue("ChecksumsManagerIT.testSave-" + System.currentTimeMillis());
        String content = "ChecksumsManagerITtestSaveContent" + System.currentTimeMillis();
        InputStream object = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        Subject subject = new Subject();
        subject.setValue("test_user");
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, object);
        try {
            SystemMetadataManager.getInstance().lock(pid);
            SystemMetadataManager.getInstance().store(sysmeta);
        } finally {
            SystemMetadataManager.getInstance().unLock(pid);
        }
        ObjectInfo info = storage.storeObject(object, pid, null, sysmeta.getChecksum().getValue(),
                                       "MD5", -1);
        ChecksumsManager manager = new ChecksumsManager();
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("ChecksumsManagerIT.testSave");
            serialNumber = dbConn.getCheckOutSerialNumber();
            manager.save(pid, info.getHexDigests(), dbConn);
            List<Checksum> checksums = manager.get(pid);
            boolean found = false;
            Checksum targetChecksum = null;
            int index = 0;
            for (Checksum checksum : checksums) {
                if (checksum.getAlgorithm().equals("MD5")) {
                    assertEquals(sysmeta.getChecksum().getValue(), checksum.getValue());
                    targetChecksum = checksum;
                    found = true;
                }
                index++;
            }
            assertTrue("Test should find the checksum with algorithm MD5", found);
            assertEquals("ChecksumsManager should store five checksums", 5, index);
            List<Identifier> identifiers = manager.query(targetChecksum);
            found = false;
            for (Identifier identifier : identifiers) {
                if (identifier != null && identifier.getValue().equals(pid.getValue())) {
                    found = true;
                }
            }
            assertTrue("Test should find the identifier " + pid.getValue(), found);
            // Test the delete method
            manager.delete(pid, dbConn);
            checksums = manager.get(pid);
            assertEquals(0, checksums.size());
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Test the get method
     * @throws Exception
     */
    @Test
    public void testGet() throws Exception {
        ChecksumsManager manager = new ChecksumsManager();
        Identifier pid = null;
        try {
            manager.get(pid);
            fail("Test shouldn't get here since the pid is null.");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        pid = new Identifier();
        pid.setValue("foo-ChecksumsManagerIT");
        List<Checksum> list = manager.get(pid);
        assertEquals(0, list.size());
        //Regular test is in the testSave method
    }

    /**
     * Test the query method
     * @throws Exception
     */
    @Test
    public void testQuery() throws Exception {
        ChecksumsManager manager = new ChecksumsManager();
        Checksum checksum = null;
        try {
            manager.query(checksum);
            fail("Test shouldn't get here since checksum is null.");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        checksum = new Checksum();
        checksum.setAlgorithm("foo");
        checksum.setValue("foo");
        List<Identifier> list = manager.query(checksum);
        assertEquals(0, list.size());
        //Regular test is in the testSave method
    }
}
