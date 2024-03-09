package edu.ucsb.nceas.metacat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.PreparedStatement;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import edu.ucsb.nceas.IntegrationTestUtils;
import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.MetacatHandler.Action;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;


/**
 * The test class of MetacatHandler
 * @author tao
 *
 */
public class MetacatHandlerTest {
    private static final String test_file_path = "test/clienttestfiles/tpc02-water-flow-base.xml";
    private static final String another_test_file = "test/macbeth.xml";
    private static final String MD5 = "MD5";
    private MetacatHandler handler;
    private String dataDir;
    private String documentDir;

    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        handler = new MetacatHandler();
        documentDir = PropertyService.getProperty("application.documentfilepath");
        if (!documentDir.endsWith("/")) {
            documentDir = documentDir + "/";
        }
        dataDir = PropertyService.getProperty("application.datafilepath");
        if (!dataDir.endsWith("/")) {
            dataDir = dataDir + "/";
        }
    }

    /**
     * Test the validateSciMeta method
     * @throws Exception
     */
    @Test
    public void testValidateSciMeta() throws Exception {
        // Test a valid eml2.2.0 object
        File eml = new File("test/eml-2.2.0.xml");
        byte[] xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        handler.validateSciMeta(xmlBytes, formatId);
        // Test an invalid eml2.2.0 object - duplicated ids
        eml = new File("test/resources/eml-error-2.2.0.xml");
        xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        try {
            handler.validateSciMeta(xmlBytes, formatId);
            fail("Test can reach here since the eml object is invalid");
        } catch (Exception e) {
            assertTrue("The message should say the id must be unique",
                        e.getMessage().contains("unique"));
        }
        // Test an invalid eml 2.1.1 object
        formatId.setValue("eml://ecoinformatics.org/eml-2.1.1");
        eml = new File("test/resources/eml-error.xml");
        xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        try {
            handler.validateSciMeta(xmlBytes, formatId);
            fail("Test can reach here since the eml object is invalid");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest", e instanceof InvalidRequest);
            assertTrue("The message should say the element principal1 is incorrect",
                        e.getMessage().contains("principal1"));
        }
        // Test a valid eml 2.1.0 object
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        eml = new File("test/eml-sample.xml");
        xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        handler.validateSciMeta(xmlBytes, formatId);
        // Test a valid eml-beta 6
        formatId.setValue("-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN");
        eml = new File("./test/jones.204.22.xml");
        xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        handler.validateSciMeta(xmlBytes, formatId);
        // Test a valid json object
        File json = new File("test/json-ld.json");
        byte[] object = IOUtils.toByteArray(new FileInputStream(json));
        formatId.setValue(NonXMLMetadataHandlers.JSON_LD);
        handler.validateSciMeta(object, formatId);
        // Test a invalid json object
        json = new File("test/invalid-json-ld.json");
        object = IOUtils.toByteArray(new FileInputStream(json));
        try {
            handler.validateSciMeta(object, formatId);
            fail("Test can reach here since the json-ld object is invalid");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest",e instanceof InvalidRequest);
        }
    }

    /**
     * Test the registerToDB method
     * @throws Exception
     */
    @Test
    public void testRegisterToDB() throws Exception {
        int serialNumber = -1;
        DBConnection dbConn = null;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("MetacatHandlerTest.testRegisterToDB");
            serialNumber = dbConn.getCheckOutSerialNumber();
            Identifier pid = new Identifier();
            Identifier prePid = null;
            try {
                // Blank pid will be rejected
                handler.registerToDB(pid, Action.INSERT, dbConn, "user-foo", DocumentImpl.BIN, prePid);
                fail("Test can not reach here since it should throw an exception");
            } catch (Exception e) {
                assertTrue("The exception should be InvalidRequest rather than "
                            + e.getClass().getName(), e instanceof InvalidRequest);
            }
            pid.setValue("testRegisterToDBData1-" + System.currentTimeMillis());
            try {
                // doctype null will be rejected.
                handler.registerToDB(pid, Action.INSERT, dbConn, "user-foo", null, prePid);
                fail("Test can not reach here since it should throw a Exception");
            } catch (Exception e) {
                assertTrue("The exception should be InvalidRequest rather than "
                            + e.getClass().getName(), e instanceof InvalidRequest);
            }
            // Register a inserted data object
            String localId = handler.registerToDB(pid, Action.INSERT, dbConn, "user-data",
                                                    DocumentImpl.BIN, prePid);
            String docid = localId.substring(0, localId.lastIndexOf("."));
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                            " rev=? and guid like ? and docid like ? ", 1, pid.getValue(), docid));
            assertFalse("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                            " rev=? and guid like ? and docid like ? ", 2, pid.getValue(), docid));
            assertTrue("The xml_documents table should have value",
                    IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                    " rev=? and docid like ?", 1, docid));
            assertFalse("The xml_revisions table should not have value",
                   IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                   " docid like ?", docid));
            // Update the data object
            Identifier newPid = new Identifier();
            newPid.setValue("testRegisterToDBData2-" + System.currentTimeMillis());
            prePid = pid;
            localId = handler.registerToDB(newPid, Action.UPDATE, dbConn, "user-data",
                                        DocumentImpl.BIN, prePid);
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                            " rev=? and guid like ? and docid like ? ", 1, pid.getValue(), docid));
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                          " rev=? and guid like ? and docid like ? ", 2, newPid.getValue(), docid));
            assertTrue("The xml_documents table should have value",
                    IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                    " rev=? and docid like ?", 2, docid));
            assertTrue("The xml_revisions table should have value",
                   IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                   " rev=? and docid like ?", 1, docid));
            assertFalse("The xml_revisions table should not have value",
                    IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                    " rev=? and docid like ?", 2, docid));
            deleteRecords(docid);//clear
            // Register a inserted metadata object. The prePid will be ignored.
            pid.setValue("testRegisterToDBMetadata1-" + System.currentTimeMillis());
            localId = handler.registerToDB(pid, Action.INSERT, dbConn, "user-metadata",
                                                      "eml://ecoinformatics.org/eml-2.1.1", prePid);
            docid = localId.substring(0, localId.lastIndexOf("."));
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                            " rev=? and guid like ? and docid like ? ", 1, pid.getValue(), docid));
            assertFalse("The identifier table should not have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                            " rev=? and guid like ? and docid like ? ", 2, pid.getValue(), docid));
            assertTrue("The xml_documents table should have value",
                    IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                    " rev=? and docid like ?", 1, docid));
            assertFalse("The xml_revisions table should not have value",
                   IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                   " docid like ?", docid));
            // Register a updated metadata object
            newPid.setValue("testRegisterToDBMetadata2-" + System.currentTimeMillis());
            prePid = pid;
            localId = handler.registerToDB(newPid, Action.UPDATE, dbConn, "user-metadata",
                    "eml://ecoinformatics.org/eml-2.1.1", prePid);
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                            " rev=? and guid like ? and docid like ? ", 1, pid.getValue(), docid));
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                          " rev=? and guid like ? and docid like ? ", 2, newPid.getValue(), docid));
            assertTrue("The xml_documents table should have value",
                    IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                    " rev=? and docid like ?", 2, docid));
            assertTrue("The xml_revisions table should have value",
                   IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                   " rev=? and docid like ?", 1, docid));
            assertFalse("The xml_revisions table should not have value",
                    IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                    " rev=? and docid like ?", 2, docid));
            // update again
            Identifier newPid2 = new Identifier();
            newPid2.setValue("testRegisterToDBMetadata3-" + System.currentTimeMillis());
            prePid = newPid;
            localId = handler.registerToDB(newPid2, Action.UPDATE, dbConn, "user-metadata",
                    "eml://ecoinformatics.org/eml-2.1.1", prePid);
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                            " rev=? and guid like ? and docid like ? ", 1, pid.getValue(), docid));
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                          " rev=? and guid like ? and docid like ? ", 2, newPid.getValue(), docid));
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                         " rev=? and guid like ? and docid like ? ", 3, newPid2.getValue(), docid));
            assertFalse("The identifier table should not have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                         " rev=? and docid like ? ", 4, docid));
            assertTrue("The xml_documents table should have value",
                    IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                    " rev=? and docid like ?", 3, docid));
            assertTrue("The xml_revisions table should have value",
                   IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                   " rev=? and docid like ?", 1, docid));
            assertTrue("The xml_revisions table should have value",
                    IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                    " rev=? and docid like ?", 2, docid));
            assertFalse("The xml_revisions table should not have value",
                    IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                    " rev=? and docid like ?", 3, docid));
            deleteRecords(docid);//clear
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    private void deleteRecords(String docid) throws Exception {
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("DocumentImpl.delete");
            serialNumber = conn.getCheckOutSerialNumber();
            String deleteQuery = "DELETE FROM xml_documents WHERE docid = ?";
            try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteQuery)) {
                pstmtDelete.setString(1, docid);
                pstmtDelete.execute();
                //Usaga count increase 1
                conn.increaseUsageCount(1);
            }
            String deleteQuery2 = "DELETE FROM xml_revisions WHERE docid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteQuery2)) {
                pstmt.setString(1, docid);
                pstmt.execute();
            }
            String deleteQuery3 = "DELETE FROM identifier WHERE docid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteQuery3)) {
                pstmt.setString(1, docid);
                pstmt.execute();
            }
        } finally {
            if (conn != null) {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
    }

    /**
     * Test the saveBytes method
     * @throws Exception
     */
    @Test
    public void testSaveBytesAndReadWithDetailedInputStream() throws Exception {
        // Save a data file
        String checkStr = "19776e05bc62d92ab24e0597ab6f12c6";
        String localId = "autogen." + System.currentTimeMillis() +".1";
        Identifier pid = new Identifier();
        pid.setValue("foo");
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(checkStr);
        // check
        try {
            //inputstream is null
            handler.saveBytes(null, localId, checksum, DocumentImpl.BIN, pid);
            fail("The test should not get here since the input stream is null");
        } catch (Exception e ) {
            assertTrue("Should be an InvalidRequest exception, rather " + e.getClass().getName(),
                      e instanceof InvalidRequest);
        }
        DetailedFileInputStream dataStream = generateDetailedInputStream(test_file_path, checksum);
        try {
            //locaId is null
            handler.saveBytes(dataStream, null, checksum, DocumentImpl.BIN, pid);
            fail("The test should not get here since the local id is null");
        } catch (Exception e ) {
            assertTrue("Should be an InvalidRequest exception, rather than "
                        + e.getClass().getName(), e instanceof InvalidRequest);
        }
        try {
            //doc type is null
            handler.saveBytes(dataStream, localId, checksum, null, pid);
            fail("The test should not get here since the local id is null");
        } catch (Exception e ) {
            assertTrue("Should be an InvalidRequest exception, rather than "
                        + e.getClass().getName(), e instanceof InvalidRequest);
        }
        try {
            //checksum is null
            handler.saveBytes(dataStream, localId, null, DocumentImpl.BIN, pid);
            fail("The test should not get here since the local id is null");
        } catch (Exception e ) {
            assertTrue("Should be InvalidSystemMetadata, rather than " + e.getClass().getName(),
                      e instanceof InvalidSystemMetadata);
        }
        handler.saveBytes(dataStream, localId, checksum, DocumentImpl.BIN, pid);
        File result = new File(dataDir + localId);
        assertTrue("File " + result + " should exist.", result.exists());
        InputStream in = handler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(in, MD5);
        in.close();
        assertTrue("The checksum from handler.read should be " + checkStr + " rather than "
                    + readChecksum, readChecksum.equals(checkStr));
        // Running the saveBytes method again with the same local id should fail
        // since the target file does exist
        DetailedFileInputStream dataStream2 = generateDetailedInputStream(another_test_file);
        Checksum anotherChecksum = dataStream2.getExpectedChecksum();
        try {
            handler.saveBytes(dataStream2, localId, anotherChecksum, DocumentImpl.BIN, pid);
            fail("Test should get here since the target file does exist");
        } catch (Exception e) {
            assertTrue("Should be ServiceFailure rather than " + e.getClass().getName(),
                       e instanceof ServiceFailure);
        }
        // Running the saveBytes method again with the same local id should fail
        // since the target file does exist. This time test different code route.
        DetailedFileInputStream dataStream3 = generateDetailedInputStream(another_test_file, null);
        try {
            handler.saveBytes(dataStream3, localId, anotherChecksum, DocumentImpl.BIN, pid);
            fail("Test should get here since the target file does exist");
        } catch (Exception e) {
            assertTrue("Should be ServiceFailure rather than " + e.getClass().getName(),
                       e instanceof ServiceFailure);
        }
        // Test checksum doesn't match
        dataStream3 = generateDetailedInputStream(another_test_file);
        localId = "autogen." + System.currentTimeMillis() +".1";
        Checksum fakeChecksum = new Checksum();
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, "eml", pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        fakeChecksum.setAlgorithm(MD5);
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, "eml", pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        fakeChecksum.setAlgorithm("");
        fakeChecksum.setValue(anotherChecksum.getValue());
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, "eml", pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        fakeChecksum.setAlgorithm(MD5);
        fakeChecksum.setValue("56453F");
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, "eml", pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        // Success if the checksum value is the upper case letters.
        String str = "ef4b3d26ad713ecc9aa5988bbdfeded7";
        Checksum upper = new Checksum();
        upper.setAlgorithm(MD5);
        upper.setValue(str.toUpperCase());
        handler.saveBytes(dataStream3, localId, upper, "eml", pid);
        result = new File(documentDir + localId);
        assertTrue("File " + result + " should exist.", result.exists());
        in = handler.read(localId, DocumentImpl.BIN);
        String readFrom = getChecksum(in, MD5);
        assertTrue("The checksum from handler.read should be " + str + " rather than "
                + readFrom, readFrom.equals(str));
        in.close();
    }

    /**
     * Generated a DetailedFileInputStream object with checksum and a temp file from the source file
     * @param filePath  the source file paht
     * @return a DetailedFileInputStream object
     * @throws Exception
     */
    private DetailedFileInputStream generateDetailedInputStream(String filePath)
                                                                            throws Exception{
        byte[] buffer = new byte[1024];
        MessageDigest md5 = MessageDigest.getInstance(MD5);
        File tmpFile = File.createTempFile("MetacatHandler-test", null);
        try (FileInputStream dataStream = new FileInputStream(filePath)) {
            try (FileOutputStream os = new FileOutputStream(tmpFile)) {
                int bytesRead;
                while ((bytesRead = dataStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    md5.update(buffer, 0, bytesRead);
                }
            }
        }
        String md5Digest = DatatypeConverter.printHexBinary(md5.digest()).toLowerCase();
        Checksum checksum = new Checksum();
        checksum.setValue(md5Digest);
        checksum.setAlgorithm(MD5);
        return new DetailedFileInputStream(tmpFile, checksum);
    }

    private DetailedFileInputStream generateDetailedInputStream(String path, Checksum checksum)
                                                                                throws Exception{
        byte[] buffer = new byte[1024];
        File tmpFile = File.createTempFile("MetacatHandler-test", null);
        try (FileInputStream dataStream = new FileInputStream(path)) {
            try (FileOutputStream os = new FileOutputStream(tmpFile)) {
                int bytesRead;
                while ((bytesRead = dataStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
        return new DetailedFileInputStream(tmpFile, checksum);
    }

    /*
     * Note: this method write input stream into memory. So it can only handle the small files.
     */
    private String getChecksum(InputStream input, String checkAlgor) throws Exception {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream(2000000);
        MessageDigest md5 = MessageDigest.getInstance(checkAlgor);
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            md5.update(buffer, 0, bytesRead);
        }
        input.close();
        return DatatypeConverter.printHexBinary(md5.digest()).toLowerCase();
    }
}
