package edu.ucsb.nceas.metacat.storage;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.MetacatHandlerIT;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.shared.ServiceException;

import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the HashStorage class
 * @author Jing Tao
 */
public class HashStorageTest {
    private static final String test_file_path = "test/clienttestfiles/tpc02-water-flow-base.xml";
    private static final String test_file_checksum = "19776e05bc62d92ab24e0597ab6f12c6";
    private static final String MD5 = "MD5";
    private static Storage storage;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        storage = HashStorage.getInstance("org.dataone.hashstore.filehashstore.FileHashStore");
        assertNotNull(storage);
    }

    /**
     * Test the getInstance method
     * @throws Exception
     */
    @Test
    public void testGetInstance() throws Exception {
        HashStorage.resetHashStorage();
        try {
            Storage storage = HashStorage.getInstance(null);
            fail("Test shouldn't get here since the class name is null");
        } catch (Exception e) {
            assertTrue(e instanceof ServiceException);
        }
        try {
            Storage storage = HashStorage.getInstance("foo");
            fail("Test shouldn't get here since the class name is null");
        } catch (Exception e) {
            assertTrue(e instanceof ServiceException);
        }

    }

    /**
     * Test the storeObject method
     * @throws Exception
     */
    @Test
    public void testStoreObject() throws Exception {
        FileInputStream object = new FileInputStream(test_file_path);
        // null pid
        Identifier pid = null;
        try {
            storage.storeObject(object, pid, null, test_file_checksum, MD5, -1);
            fail("Test can't get here since the pid is null.");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        pid = new Identifier();
        pid.setValue("HashStorageTestTestStoreData-" + System.currentTimeMillis());
        try {
            storage.storeObject(object, pid, null, "fooCheckSum", MD5, -1);
            fail("Test can't get here since the checksum of the file is wrong.");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidSystemMetadata);
        }
        try {
            object = new FileInputStream(test_file_path);
            storage.storeObject(object, pid, null, test_file_checksum, MD5, 20);
            fail("Test can't get here since the file size is wrong.");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidSystemMetadata);
        }
        object = new FileInputStream(test_file_path);
        storage.storeObject(object, pid, null, test_file_checksum, MD5, 242900);
        // Test the read action
        InputStream inputStream = storage.retrieveObject(pid);
        String readChecksum = MetacatHandlerIT.getChecksum(inputStream, MD5);
        assertEquals(test_file_checksum, readChecksum);
        // Test the delete action
        storage.deleteObject(pid);
        try {
            inputStream = storage.retrieveObject(pid);
            fail("Test can't get here since the pid was deleted");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
    }

    /**
     * Test the retrieveObject method
     * @throws Exception
     */
    @Test
    public void testRetrieveObject() throws Exception {
        Identifier pid = null;
        try {
            storage.retrieveObject(pid);
            fail("Test can't get here since the pid is null.");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        pid = new Identifier();
        pid.setValue("footestRetrieveObject" + System.nanoTime());
        try {
            storage.retrieveObject(pid);
            fail("Test can't get here since the object doesn't exist.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        // Regular retrieveObject is tested in the testStoreObject method
    }

    /**
     * Test the verityObject and tagObject method
     * @throws Exception
     */
    @Test
    public void testVerifyAndTagObject() throws Exception {
        FileInputStream object = new FileInputStream(test_file_path);
        ObjectInfo info = storage.storeObject(object);
        storage.verifyObject(info, test_file_checksum, MD5, 242900);
        Identifier pid = new Identifier();
        pid.setValue("testVerifyAndTagObject" + System.nanoTime());
        storage.tagObject(pid, info.getCid());
        InputStream inputStream = storage.retrieveObject(pid);
        String readChecksum = MetacatHandlerIT.getChecksum(inputStream, MD5);
        assertEquals(test_file_checksum, readChecksum);
    }

    /**
     * Test the delete object method
     * @throws Exception
     */
    @Test
    public void testDeleteObject() throws Exception {
        Identifier pid = null;
        try {
            storage.deleteObject(pid);
            fail("Test can't get here since the pid is null.");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        pid = new Identifier();
        pid.setValue("HashStorage.testDeleteObject123" + System.nanoTime());
        try {
            storage.deleteObject(pid);
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }

        // Regular deleting object is tested in the testStoreObject method.
    }

    /**
     * Test the storeMetadata, retrieveMetadata and deleteMetadata methods
     * @throws Exception
     */
    @Test
    public void testSystemMetadataOperations() throws Exception {
        Identifier pid = new Identifier();
        pid.setValue("HashStorage.testSystemMetadataOperations" + System.nanoTime());
        FileInputStream object = new FileInputStream(test_file_path);
        Subject subject = new Subject();
        subject.setValue("https://orcid.org/123456");
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, object);
        assertTrue(sysmeta instanceof SystemMetadata);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta.setFormatId(formatId);
        ByteArrayOutputStream sysOutput = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(sysmeta, sysOutput);
        byte[] sysContent = sysOutput.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(sysContent);
        storage.storeMetadata(input,pid);
        InputStream sysReadInput = storage.retrieveMetadata(pid);
        SystemMetadata readSysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                                                                            sysReadInput);
        assertEquals(pid.getValue(), readSysmeta.getIdentifier().getValue());
        assertEquals(test_file_checksum, readSysmeta.getChecksum().getValue());
        assertEquals(MD5, readSysmeta.getChecksum().getAlgorithm());
        assertEquals(242900, readSysmeta.getSize().longValue());

        // Test deleting
        storage.deleteMetadata(pid);
        try {
            storage.retrieveMetadata(pid);
            fail("Test shouldn't get here since the pid was deleted.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }

        // Test to get non-existing pid
        try {
            pid.setValue("HashStorage.testSystemMetadataOperations234" + System.nanoTime());
            storage.retrieveMetadata(pid);
            fail("Test shouldn't get here since the pid doesn't exist.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
    }
}
