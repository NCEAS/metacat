package edu.ucsb.nceas.metacat.object.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * JUnit test class for the JsonLDHandler class
 * @author tao
 *
 */
public class JsonLDHandlerTest {
    private static String metadataStoragePath = null;


    public static final String JSON_LD_FILE_PATH = "test/json-ld.json";
    private static final String CHECKSUM_JSON_FILE = "847e1655bdc98082804698dbbaf85c35";
    public static final String INVALID_JSON_LD_FILE_PATH = "test/invalid-json-ld.json";
    private static final String CHECKSUM_INVALID_JSON_FILE = "ede435691fa0c68e9a3c23697ffc92d4";
    private String user = "foo-jsonld";
    private Subject subject;

    /**
     * Set up the test fixtures
     * @throws PropertyNotFoundException
     */
    @Before
    public void setUp() throws PropertyNotFoundException {
        subject = new Subject();
        subject.setValue(user);
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        metadataStoragePath = PropertyService.getProperty("application.documentfilepath");
    }


    /**
     * Test the valid method
     * @throws Exception
     */
    @Test
    public void testInvalid() throws Exception {
        JsonLDHandler handler = new JsonLDHandler();
        InputStream input = new FileInputStream(new File(JSON_LD_FILE_PATH));
        assertTrue(handler.validate(input));
        input = new FileInputStream(new File(INVALID_JSON_LD_FILE_PATH));
        try {
            handler.validate(input);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        
    }

    /**
     * Test the save method
     * @throws Exception
     */
    @Test
    public void testSave() throws Exception {
        Checksum expectedChecksum = new Checksum();
        expectedChecksum.setAlgorithm("MD5");
        expectedChecksum.setValue(CHECKSUM_JSON_FILE);
        ObjectFormatIdentifier format = new ObjectFormatIdentifier();
        format.setValue(NonXMLMetadataHandlers.JSON_LD);
        SystemMetadata sysmeta = new SystemMetadata();

        //save the DetailedFileInputStream from the valid json-ld object without a checksum
        File temp1 = generateTmpFile("temp-json-ld-valid");
        InputStream input = new FileInputStream(new File(JSON_LD_FILE_PATH));
        OutputStream out = new FileOutputStream(temp1);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        Checksum checksum = null;
        DetailedFileInputStream data = new DetailedFileInputStream(temp1, checksum);
        MetacatHandler handler = new MetacatHandler();
        Identifier pid = new Identifier();
        pid.setValue("test-id1-" + System.currentTimeMillis());
        assertTrue(temp1.exists());
        sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
        sysmeta.setFormatId(format);
        data = new DetailedFileInputStream(temp1, checksum);
        // True means changing the modification date in the system metadata
        String localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                                       NonXMLMetadataHandlers.JSON_LD, data, null, user);

        assertFalse(temp1.exists());
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile = new File(metadataStoragePath, localId);
        assertTrue(savedFile.exists());
        DocumentImpl.deleteFromFileSystem(localId, true);
        assertFalse(savedFile.exists());

        //save the DetaiedFileInputStream from the valid json-ld object with the expected checksum
        File temp2 = generateTmpFile("temp2-json-ld-valid");
        input = new FileInputStream(new File(JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp2);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        data = new DetailedFileInputStream(temp2, expectedChecksum);
        pid = new Identifier();
        pid.setValue("test-id2-" + System.currentTimeMillis());
        assertTrue(temp2.exists());
        sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
        sysmeta.setFormatId(format);
        data = new DetailedFileInputStream(temp2, expectedChecksum);
        // True means changing the modification date in the system metadata
        localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                NonXMLMetadataHandlers.JSON_LD, data, null, user);
        assertTrue(!temp2.exists());
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile2 = new File(metadataStoragePath, localId);
        assertTrue(savedFile2.exists());
        DocumentImpl.deleteFromFileSystem(localId, true);
        assertTrue(!savedFile2.exists());

        Checksum expectedChecksumForInvalidJson = new Checksum();
        expectedChecksumForInvalidJson.setAlgorithm("MD5");
        expectedChecksumForInvalidJson.setValue(CHECKSUM_INVALID_JSON_FILE);

        //save the DetaiedFileInputStream from the invalid json-ld object without a checksum
        File temp3 = generateTmpFile("temp3-json-ld-valid");
        input = new FileInputStream(new File(INVALID_JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp3);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        checksum = null;
        data = new DetailedFileInputStream(temp3, checksum);
        pid = new Identifier();
        pid.setValue("test-id3-" + System.currentTimeMillis());
        assertTrue(temp3.exists());
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setFormatId(format);
            data = new DetailedFileInputStream(temp3, checksum);
            // True means changing the modification date in the system metadata
            localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                              NonXMLMetadataHandlers.JSON_LD, data, null, user);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
        temp3.delete();

        //save the DetaiedFileInputStream from the invalid json-ld object with the expected checksum
        File temp4 = generateTmpFile("temp4-json-ld-valid");
        input = new FileInputStream(new File(INVALID_JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp4);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        data = new DetailedFileInputStream(temp4, expectedChecksumForInvalidJson);
        pid = new Identifier();
        pid.setValue("test-id4-" + System.currentTimeMillis());
        assertTrue(temp4.exists());
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setFormatId(format);
            data = new DetailedFileInputStream(temp4, expectedChecksumForInvalidJson);
            // True means changing the modification date in the system metadata
            localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                            NonXMLMetadataHandlers.JSON_LD, data, null, user);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
        temp4.delete();

        //save the DetaiedFileInputStream from the valid json-ld object with a wrong checksum
        File temp5 = generateTmpFile("temp5-json-ld-valid");
        input = new FileInputStream(new File(JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp5);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        data = new DetailedFileInputStream(temp5, expectedChecksum);
        pid = new Identifier();
        pid.setValue("test-id5-" + System.currentTimeMillis());
        assertTrue(temp5.exists());
        try {
            checksum = new Checksum();
            checksum.setAlgorithm("MD5");
            checksum.setValue(CHECKSUM_INVALID_JSON_FILE);
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setFormatId(format);
            data = new DetailedFileInputStream(temp5, checksum);
            // True means changing the modification date in the system metadata
            localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                            NonXMLMetadataHandlers.JSON_LD, data, null, user);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidSystemMetadata);
        }
        assertFalse(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        temp5.delete();
    }

    /**
     * Save the jsonLD object coming from a byte array input stream
     * @throws Exception
     */
    @Test
    public void testSaveByteArray() throws Exception {
        ObjectFormatIdentifier format = new ObjectFormatIdentifier();
        format.setValue(NonXMLMetadataHandlers.JSON_LD);
        Checksum expectedChecksum = new Checksum();
        expectedChecksum.setAlgorithm("MD5");
        expectedChecksum.setValue(CHECKSUM_JSON_FILE);

        Checksum expectedChecksumForInvalidJson = new Checksum();
        expectedChecksumForInvalidJson.setAlgorithm("MD5");
        expectedChecksumForInvalidJson.setValue(CHECKSUM_INVALID_JSON_FILE);

        //save a valid json file with correct expected checksum
        String content = FileUtils.readFileToString(new File(JSON_LD_FILE_PATH), "UTF-8");
        InputStream data = new ByteArrayInputStream(content.getBytes());
        MetacatHandler handler = new MetacatHandler();
        Identifier pid = new Identifier();
        pid.setValue("testbye-id1-" + System.currentTimeMillis());
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
        sysmeta.setFormatId(format);
        data = new ByteArrayInputStream(content.getBytes());
        // True means changing the modification date in the system metadata
        String localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                                    NonXMLMetadataHandlers.JSON_LD, data, null, user);
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile = new File(metadataStoragePath, localId);
        assertTrue(savedFile.exists());
        DocumentImpl.deleteFromFileSystem(localId, true);
        assertTrue(!savedFile.exists());

        //save the valid json-ld object with the wrong checksum
        data = new ByteArrayInputStream(content.getBytes());
        pid = new Identifier();
        pid.setValue("testbye-id2-" + System.currentTimeMillis());
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setIdentifier(pid);
            sysmeta.setFormatId(format);
            sysmeta.setChecksum(expectedChecksumForInvalidJson);
            data = new ByteArrayInputStream(content.getBytes());
            // True means changing the modification date in the system metadata
            localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                    NonXMLMetadataHandlers.JSON_LD, data, null, user);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidSystemMetadata);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));

        //save an invalid jsonld file
        content = FileUtils.readFileToString(new File(INVALID_JSON_LD_FILE_PATH), "UTF-8");
        data = new ByteArrayInputStream(content.getBytes());
        pid = new Identifier();
        pid.setValue("testbye-id3-" + System.currentTimeMillis());
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setIdentifier(pid);
            sysmeta.setFormatId(format);
            sysmeta.setChecksum(expectedChecksumForInvalidJson);
            data = new ByteArrayInputStream(content.getBytes());
            // True means changing the modification date in the system metadata
            localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                            NonXMLMetadataHandlers.JSON_LD, data, null, user);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
    }

    /**
     * Save the jsonLD object coming from a regular file
     * @throws Exception
     */
    @Test
    public void testSaveFile() throws Exception {
        ObjectFormatIdentifier format = new ObjectFormatIdentifier();
        format.setValue(NonXMLMetadataHandlers.JSON_LD);
        Checksum expectedChecksum = new Checksum();
        expectedChecksum.setAlgorithm("MD5");
        expectedChecksum.setValue(CHECKSUM_JSON_FILE);

        Checksum expectedChecksumForInvalidJson = new Checksum();
        expectedChecksumForInvalidJson.setAlgorithm("MD5");
        expectedChecksumForInvalidJson.setValue(CHECKSUM_INVALID_JSON_FILE);

        //save a valid json file with correct expected checksum
        InputStream data = new FileInputStream(new File(JSON_LD_FILE_PATH));
        MetacatHandler handler = new MetacatHandler();
        Identifier pid = new Identifier();
        pid.setValue("testSaveFile-id1-" + System.currentTimeMillis());
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
        sysmeta.setIdentifier(pid);
        sysmeta.setFormatId(format);
        sysmeta.setChecksum(expectedChecksum);
        data = new FileInputStream(new File(JSON_LD_FILE_PATH));
        // True means changing the modification date in the system metadata
        String localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                            NonXMLMetadataHandlers.JSON_LD, data, null, user);
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile = new File(metadataStoragePath, localId);
        assertTrue(savedFile.exists());
        data.close();
        DocumentImpl.deleteFromFileSystem(localId, true);
        assertTrue(!savedFile.exists());

        //save the  valid json-ld object with the wrong checksum
        data = new FileInputStream(new File(JSON_LD_FILE_PATH));
        pid = new Identifier();
        pid.setValue("testSaveFile-id2-" + System.currentTimeMillis());
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setIdentifier(pid);
            sysmeta.setFormatId(format);
            sysmeta.setChecksum(expectedChecksumForInvalidJson);
            data = new FileInputStream(new File(JSON_LD_FILE_PATH));
            // True means changing the modification date in the system metadata
            localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                        NonXMLMetadataHandlers.JSON_LD, data, null, user);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            data.close();
            assertTrue(e instanceof InvalidSystemMetadata);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));

        //save an invalid jsonld file
        pid = new Identifier();
        pid.setValue("test-saveFile-id3-" + System.currentTimeMillis());
        data = new FileInputStream(new File(INVALID_JSON_LD_FILE_PATH));
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setIdentifier(pid);
            sysmeta.setFormatId(format);
            sysmeta.setChecksum(expectedChecksumForInvalidJson);
            data = new FileInputStream(new File(INVALID_JSON_LD_FILE_PATH));
            // True means changing the modification date in the system metadata
            localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                    NonXMLMetadataHandlers.JSON_LD, data, null, user);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            data.close();
            assertTrue(e instanceof InvalidRequest);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));

    }

    /*
     * A utility method to generate a temporary file.
     */
    public static File generateTmpFile(String prefix) throws IOException {
        String newPrefix = prefix + "-" + System.currentTimeMillis();
        String suffix =  null;
        File newFile = null;
        try {
            newFile = File.createTempFile(newPrefix, suffix, new File("."));
        } catch (Exception e) {
            //try again if the first time fails
            newFile = File.createTempFile(newPrefix, suffix, new File("."));
        }
        return newFile;
    }

    /**
     * Delete the record from the xml_documents table
     * @param localId
     * @throws SQLException
     */
    private static void deleteXMLDocuments(String localId) throws SQLException {
        String docId = DocumentUtil.getDocIdFromString(localId);
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pStmt = null;
        try {
            //check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("DocumentImpl.deleteXMLDocuments");
            serialNumber = conn.getCheckOutSerialNumber();
            //delete a record
            pStmt = conn.prepareStatement(
                    "DELETE FROM xml_documents WHERE docid = ? ");
            pStmt.setString(1, docId);
            pStmt.execute();
        } finally {
            try {
                pStmt.close();
            } finally {
                //return back DBconnection
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
    }
}
