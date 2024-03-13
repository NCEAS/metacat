package edu.ucsb.nceas.metacat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import edu.ucsb.nceas.IntegrationTestUtils;
import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.MetacatHandler.Action;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;


import static org.mockito.ArgumentMatchers.any;

/**
 * The test class of MetacatHandler
 * @author tao
 *
 */
public class MetacatHandlerTest {
    private static final String test_file_path = "test/clienttestfiles/tpc02-water-flow-base.xml";
    private static final String test_file_checksum = "19776e05bc62d92ab24e0597ab6f12c6";
    private static final String another_test_file = "test/macbeth.xml";
    private static final String test_eml_file = "test/eml-2.2.0.xml";
    private static final String test_eml_file_checksum = "f4ea2d07db950873462a064937197b0f";
    private static final String test_eml_essdive_file = "test/eml-ess-dive.xml";
    private static final String test_eml_essdive_file_checksum = "24aafe49284350445bb1ff9281e3c3c5";
    private static final String eml_format = "https://eml.ecoinformatics.org/eml-2.2.0";
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
        String localId = "autogen." + System.currentTimeMillis() +".1";
        Identifier pid = new Identifier();
        pid.setValue("foo");//Just for the log purpose
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(test_file_checksum);
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
        assertTrue("The checksum from handler.read should be " + test_file_checksum + " rather than "
                    + readChecksum, readChecksum.equals(test_file_checksum));
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
        // since the target file does exist. This time it tests a different code route.
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
        // A new localId
        localId = "autogen." + System.currentTimeMillis() +".1";
        Checksum fakeChecksum = new Checksum();
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, "eml", pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        localId = "autogen." + System.currentTimeMillis() +".2";
        fakeChecksum.setAlgorithm(MD5);
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, "eml", pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        localId = "autogen." + System.currentTimeMillis() +".3";
        fakeChecksum.setAlgorithm("");
        fakeChecksum.setValue(anotherChecksum.getValue());
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, "eml", pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        localId = "autogen." + System.currentTimeMillis() +".4";
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
     * Test the saveBytes method
     * @throws Exception
     */
    @Test
    public void testSaveBytesAndReadWithInputStream() throws Exception {
        // Save a document file
        String localId = "autogen." + System.currentTimeMillis() +".1";
        Identifier pid = new Identifier();
        pid.setValue("foo21");//Just for the log purpose
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(test_file_checksum);
        InputStream dataStream = new FileInputStream(test_file_path);
        try {
            //checksum is null
            handler.saveBytes(dataStream, localId, null, "eml", pid);
            fail("The test should not get here since the local id is null");
        } catch (Exception e ) {
            assertTrue("Should be InvalidSystemMetadata, rather than " + e.getClass().getName(),
                      e instanceof InvalidSystemMetadata);
        }
        handler.saveBytes(dataStream, localId, checksum, "eml", pid);
        dataStream.close();
        File result = new File(documentDir + localId);
        assertTrue("File " + result + " should exist.", result.exists());
        InputStream in = handler.read(localId, "eml");
        String readChecksum = getChecksum(in, MD5);
        in.close();
        assertTrue("The checksum from handler.read should be " + test_file_checksum + " rather than "
                    + readChecksum, readChecksum.equals(test_file_checksum));
        // Running the saveBytes method again with the same local id should fail
        // since the target file does exist
        InputStream dataStream2 = new FileInputStream(another_test_file);
        Checksum anotherChecksum = new Checksum();
        anotherChecksum.setAlgorithm(MD5);
        String str = "ef4b3d26ad713ecc9aa5988bbdfeded7";
        anotherChecksum.setValue(str);
        try {
            handler.saveBytes(dataStream2, localId, anotherChecksum, "eml", pid);
            fail("Test should get here since the target file does exist");
        } catch (Exception e) {
            assertTrue("Should be ServiceFailure rather than " + e.getClass().getName(),
                       e instanceof ServiceFailure);
        }
        dataStream2.close();
        // Test checksum doesn't match
        InputStream dataStream3 = new FileInputStream(another_test_file);
        localId = "autogen." + System.currentTimeMillis() +".1";
        Checksum fakeChecksum = new Checksum();
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, DocumentImpl.BIN, pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        fakeChecksum.setAlgorithm(MD5);
        dataStream3 = new FileInputStream(another_test_file);
        localId = "autogen." + System.currentTimeMillis() +".2";
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, DocumentImpl.BIN, pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        fakeChecksum.setAlgorithm("");
        fakeChecksum.setValue(anotherChecksum.getValue());
        dataStream3 = new FileInputStream(another_test_file);
        localId = "autogen." + System.currentTimeMillis() +".3";
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, DocumentImpl.BIN, pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        fakeChecksum.setAlgorithm(MD5);
        fakeChecksum.setValue("5645334567F");
        dataStream3 = new FileInputStream(another_test_file);
        localId = "autogen." + System.currentTimeMillis() +".4";
        try {
            handler.saveBytes(dataStream3, localId, fakeChecksum, DocumentImpl.BIN, pid);
            fail("Test shouldn't get there since the declared checksum is incorrect.");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                       e instanceof InvalidSystemMetadata);
        }
        // Success if the checksum value is the upper case letters.
        fakeChecksum.setAlgorithm(MD5);
        fakeChecksum.setValue(str.toUpperCase());
        dataStream3 = new FileInputStream(another_test_file);
        localId = "autogen." + System.currentTimeMillis() +".5";
        handler.saveBytes(dataStream3, localId, fakeChecksum, DocumentImpl.BIN, pid);
        dataStream3.close();
        result = new File(dataDir + localId);
        assertTrue("File " + result + " should exist.", result.exists());
        in = handler.read(localId, "eml");
        String readFrom = getChecksum(in, MD5);
        assertTrue("The checksum from handler.read should be " + str + " rather than "
                + readFrom, readFrom.equals(str));
        in.close();
    }

    /**
     * Test the save method
     * @throws Excetpion
     */
    @Test
    public void testSaveDetailedInputStreamData() throws Exception {
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(test_file_checksum);
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testsave-" + System.currentTimeMillis());
        DetailedFileInputStream dataStream = generateDetailedInputStream(test_file_path, checksum);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        long originModificationDate = sysmeta.getDateSysMetadataModified().getTime();
        long originUploadDate = sysmeta.getDateUploaded().getTime();
        dataStream = generateDetailedInputStream(test_file_path, checksum);
        // null is the obsoleted system metadata. Inserting should succeed
        // True means changing the modification date in the system metadata
        handler.save(sysmeta, true, MetacatHandler.Action.INSERT, DocumentImpl.BIN,
                    dataStream, null, user);
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        String localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        InputStream readObj = handler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_file_checksum + " rather than "
                    + readChecksum, test_file_checksum.equals(readChecksum));
        assertNotEquals("The modification time in system metadata should change.",
                            originModificationDate, readSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                                            originUploadDate, readSys.getDateUploaded().getTime());

        // Updating
        Identifier newPid = new Identifier();
        newPid.setValue("MetacatHandler.testsave.new-" + System.currentTimeMillis());
        Checksum anotherChecksum = new Checksum();
        anotherChecksum.setAlgorithm(MD5);
        String anotherChecksumStr = "ef4b3d26ad713ecc9aa5988bbdfeded7";
        anotherChecksum.setValue(anotherChecksumStr);
        dataStream = generateDetailedInputStream(another_test_file, null);
        SystemMetadata newSys = D1NodeServiceTest.createSystemMetadata(newPid, owner, dataStream);
        newSys.setObsoletes(pid);
        sysmeta.setObsoletedBy(newPid);
        Checksum incorrectChecksum = new Checksum();
        incorrectChecksum.setAlgorithm(MD5);
        incorrectChecksum.setValue("234dfa343af");
        newSys.setChecksum(incorrectChecksum);
        try {
            // True means changing the modification date in the system metadata
            handler.save(newSys, true, MetacatHandler.Action.UPDATE,
                        DocumentImpl.BIN, dataStream, sysmeta, user);
            fail("Test cannot get here since the checksum is wrong in the system metadata");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                    e instanceof InvalidSystemMetadata);
        }
        // Since it is a transaction and the update process failed,
        // the old object shouldn't change anything and new object doesn't exist
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_file_checksum + " rather than "
                    + readChecksum, test_file_checksum.equals(readChecksum));
        // Newpid object doesn't exist.
        SystemMetadata newReadSys = SystemMetadataManager.getInstance().get(newPid);
        assertNull("The system metadata for the new object should be null.", newReadSys);
        try {
            localId = IdentifierManager.getInstance().getLocalId(newPid.getValue());
            fail("Test shouldn't get here since the local can't be found");
        } catch (Exception e) {
            assertTrue("The exception should be McdbDocNotFoundException, rather than "
                       + e.getClass().getName(), e instanceof McdbDocNotFoundException);
        }
        // This time, the upgrade should succeed.
        newSys.setChecksum(anotherChecksum);
        long originModificationDateOfSecondObj = newSys.getDateSysMetadataModified().getTime();
        long originUploadDateOfSecondObj = newSys.getDateUploaded().getTime();
        sysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        // Recreate the stream since it was closed in the previous failure
        dataStream = generateDetailedInputStream(another_test_file, null);
        // True means changing the modification date in the system metadata
        handler.save(newSys, true, MetacatHandler.Action.UPDATE,
                     DocumentImpl.BIN, dataStream, sysmeta, user);
        // Check the old object
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertTrue("The obsoletedBy of systemmeta from db should be " + newPid.getValue(),
                        newPid.getValue().equals(readSys.getObsoletedBy().getValue()));
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_file_checksum + " rather than "
                    + readChecksum, test_file_checksum.equals(readChecksum));
        assertNotEquals("The modification time of the obsoleted object in should change.",
                            originModificationDate, readSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time of the obsoleted object should not change.",
                        originUploadDate, readSys.getDateUploaded().getTime());
        // Newpid object exists.
        newReadSys = SystemMetadataManager.getInstance().get(newPid);
        assertTrue("The pid of systemmeta from db should be " + newPid.getValue() + " rather than "
                        + newReadSys.getIdentifier().getValue(),
                        newPid.getValue().equals(newReadSys.getIdentifier().getValue()));
        assertTrue("The obsoletes of systemmeta from db should be " + pid.getValue(),
                        pid.getValue().equals(newReadSys.getObsoletes().getValue()));
        localId = IdentifierManager.getInstance().getLocalId(newPid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The object should have the checksum " + anotherChecksumStr + " rather than "
                    + readChecksum, anotherChecksumStr.equals(readChecksum));
        assertNotEquals("The modification time in system metadata should change.",
                            originModificationDateOfSecondObj,
                            newReadSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                            originUploadDateOfSecondObj, newReadSys.getDateUploaded().getTime());
    }

    /**
     * Test the save method
     * @throws Excetpion
     */
    @Test
    public void testSaveDetailedInputStreamMetadata() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue(eml_format);
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(test_eml_file_checksum);
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testsave-" + System.currentTimeMillis());
        DetailedFileInputStream dataStream = generateDetailedInputStream(test_eml_file, checksum);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        Checksum incorrectChecksum = new Checksum();
        incorrectChecksum.setAlgorithm(MD5);
        incorrectChecksum.setValue("234dfa343af");
        sysmeta.setChecksum(incorrectChecksum);
        sysmeta.setFormatId(formatId);
        dataStream = generateDetailedInputStream(test_eml_file, checksum);
        try {
            // null is the obsoleted system metadata. Inserting should succeed
            handler.save(sysmeta, false, MetacatHandler.Action.INSERT, eml_format,
                        dataStream, null, user);
            fail("Test cannot get here since the checksum is wrong in the system metadata");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                    e instanceof InvalidSystemMetadata);
        }
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertNull("The systemmeta from db should be null.", readSys);
        String localId = null;
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
            fail("Test shouldn't get here since the local can't be found");
        } catch (Exception e) {
            assertTrue("The exception should be McdbDocNotFoundException, rather than "
                       + e.getClass().getName(), e instanceof McdbDocNotFoundException);
        }

        // This time the insert should succeed
        sysmeta.setChecksum(checksum);
        long originModificationDate = sysmeta.getDateSysMetadataModified().getTime();
        long originUploadDate = sysmeta.getDateUploaded().getTime();
        dataStream = generateDetailedInputStream(test_eml_file, checksum);
        // null is the obsoleted system metadata. Inserting should succeed
        // false means not to change the modification date of the system metadata
        handler.save(sysmeta, false, MetacatHandler.Action.INSERT, eml_format,
                        dataStream, null, user);
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        assertEquals("The modification time in system metadata should not change.",
                originModificationDate, readSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                                originUploadDate, readSys.getDateUploaded().getTime());
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        InputStream readObj = handler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_eml_file_checksum
                                    + " rather than " + readChecksum,
                                    test_eml_file_checksum.equals(readChecksum));

        // Updating
        Identifier newPid = new Identifier();
        newPid.setValue("MetacatHandler.testsave.new-" + System.currentTimeMillis());
        Checksum anotherChecksum = new Checksum();
        anotherChecksum.setAlgorithm(MD5);
        String anotherChecksumStr = "24aafe49284350445bb1ff9281e3c3c5";
        anotherChecksum.setValue(anotherChecksumStr);
        dataStream = generateDetailedInputStream(test_eml_essdive_file, null);
        SystemMetadata newSys = D1NodeServiceTest.createSystemMetadata(newPid, owner, dataStream);
        newSys.setObsoletes(pid);
        long originModificationDateOfSecondObj = newSys.getDateSysMetadataModified().getTime();
        long originUploadDateOfSecondObj = newSys.getDateUploaded().getTime();
        sysmeta.setObsoletedBy(newPid);
        sysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        // Recreate the stream since it was closed in the generating sysmeta method
        dataStream = generateDetailedInputStream(test_eml_essdive_file, null);
        // False means not change the modification date in the system metadata
        handler.save(newSys, false, MetacatHandler.Action.UPDATE,
                     eml_format, dataStream, sysmeta, user);
        // Check the objects
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertTrue("The obsoletedBy of systemmeta from db should be " + newPid.getValue(),
                        newPid.getValue().equals(readSys.getObsoletedBy().getValue()));
        assertNotEquals("The modification time of the obsoleted object in should change.",
                        originModificationDate, readSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time of the obsoleted object should not change.",
                                originUploadDate, readSys.getDateUploaded().getTime());
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_eml_file_checksum
                    + " rather than " + readChecksum, test_eml_file_checksum.equals(readChecksum));
        // Newpid object exists.
        SystemMetadata newReadSys = SystemMetadataManager.getInstance().get(newPid);
        assertTrue("The pid of systemmeta from db should be " + newPid.getValue() + " rather than "
                        + newReadSys.getIdentifier().getValue(),
                        newPid.getValue().equals(newReadSys.getIdentifier().getValue()));
        assertTrue("The obsoletes of systemmeta from db should be " + pid.getValue(),
                        pid.getValue().equals(newReadSys.getObsoletes().getValue()));
        localId = IdentifierManager.getInstance().getLocalId(newPid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The object should have the checksum " + anotherChecksumStr + " rather than "
                    + readChecksum, anotherChecksumStr.equals(readChecksum));
        assertEquals("The modification time in system metadata should not change.",
                originModificationDateOfSecondObj,
                newReadSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                originUploadDateOfSecondObj, newReadSys.getDateUploaded().getTime());
    }

    /**
     * Test the save method
     * @throws Excetpion
     */
    @Test
    public void testSaveInputStreamData() throws Exception {
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(test_file_checksum);
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testsave-" + System.currentTimeMillis());
        InputStream dataStream = new FileInputStream(test_file_path);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        long originModificationDate = sysmeta.getDateSysMetadataModified().getTime();
        long originUploadDate = sysmeta.getDateUploaded().getTime();
        dataStream = new FileInputStream(test_file_path);
        // null is the obsoleted system metadata. Inserting should succeed
        // False means not to change the modification date of system metadata
        handler.save(sysmeta, false, MetacatHandler.Action.INSERT, DocumentImpl.BIN,
                    dataStream, null, user);
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        String localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        InputStream readObj = handler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_file_checksum + " rather than "
                    + readChecksum, test_file_checksum.equals(readChecksum));
        assertEquals("The modification time in system metadata should not change.",
                originModificationDate, readSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                                originUploadDate, readSys.getDateUploaded().getTime());

        // Updating
        Identifier newPid = new Identifier();
        newPid.setValue("MetacatHandler.testsave.new-" + System.currentTimeMillis());
        Checksum anotherChecksum = new Checksum();
        anotherChecksum.setAlgorithm(MD5);
        String anotherChecksumStr = "ef4b3d26ad713ecc9aa5988bbdfeded7";
        anotherChecksum.setValue(anotherChecksumStr);
        dataStream = new FileInputStream(another_test_file);
        SystemMetadata newSys = D1NodeServiceTest.createSystemMetadata(newPid, owner, dataStream);
        newSys.setObsoletes(pid);
        sysmeta.setObsoletedBy(newPid);
        Checksum incorrectChecksum = new Checksum();
        incorrectChecksum.setAlgorithm(MD5);
        incorrectChecksum.setValue("234dfa343af");
        newSys.setChecksum(incorrectChecksum);
        try {
            // False means not to change the modification date
            handler.save(newSys, false, MetacatHandler.Action.UPDATE,
                        DocumentImpl.BIN, dataStream, sysmeta, user);
            fail("Test cannot get here since the checksum is wrong in the system metadata");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                    e instanceof InvalidSystemMetadata);
        }
        // Since it is a transaction and the update process failed,
        // the old object shouldn't change anything and new object doesn't exist
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_file_checksum + " rather than "
                    + readChecksum, test_file_checksum.equals(readChecksum));
        // Newpid object doesn't exist.
        SystemMetadata newReadSys = SystemMetadataManager.getInstance().get(newPid);
        assertNull("The system metadata for the new object should be null.", newReadSys);
        try {
            localId = IdentifierManager.getInstance().getLocalId(newPid.getValue());
            fail("Test shouldn't get here since the local can't be found");
        } catch (Exception e) {
            assertTrue("The exception should be McdbDocNotFoundException, rather than "
                       + e.getClass().getName(), e instanceof McdbDocNotFoundException);
        }
        // This time, the upgrade should succeed.
        newSys.setChecksum(anotherChecksum);
        sysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        long originModificationDateOfSecondObj = newSys.getDateSysMetadataModified().getTime();
        long originUploadDateOfSecondObj = newSys.getDateUploaded().getTime();
        // Recreate the stream since it was closed in the previous failure
        dataStream = new FileInputStream(another_test_file);
        // False means not changing the modification date in system metadata
        handler.save(newSys, false, MetacatHandler.Action.UPDATE,
                     DocumentImpl.BIN, dataStream, sysmeta, user);
        // Check the objects
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertTrue("The obsoletedBy of systemmeta from db should be " + newPid.getValue(),
                        newPid.getValue().equals(readSys.getObsoletedBy().getValue()));
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_file_checksum + " rather than "
                    + readChecksum, test_file_checksum.equals(readChecksum));
        // Newpid object exists.
        newReadSys = SystemMetadataManager.getInstance().get(newPid);
        assertTrue("The pid of systemmeta from db should be " + newPid.getValue() + " rather than "
                        + newReadSys.getIdentifier().getValue(),
                        newPid.getValue().equals(newReadSys.getIdentifier().getValue()));
        assertTrue("The obsoletes of systemmeta from db should be " + pid.getValue(),
                        pid.getValue().equals(newReadSys.getObsoletes().getValue()));
        assertEquals("The modification time in system metadata should not change.",
                        originModificationDateOfSecondObj,
                        newReadSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                        originUploadDateOfSecondObj, newReadSys.getDateUploaded().getTime());
        localId = IdentifierManager.getInstance().getLocalId(newPid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The object should have the checksum " + anotherChecksumStr + " rather than "
                    + readChecksum, anotherChecksumStr.equals(readChecksum));
    }

    /**
     * Test the save method
     * @throws Excetpion
     */
    @Test
    public void testSaveInputStreamMetadata() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue(eml_format);
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(test_eml_file_checksum);
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testsave-" + System.currentTimeMillis());
        InputStream dataStream = new FileInputStream(test_eml_file);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        Checksum incorrectChecksum = new Checksum();
        incorrectChecksum.setAlgorithm(MD5);
        incorrectChecksum.setValue("234dfa343af");
        sysmeta.setChecksum(incorrectChecksum);
        sysmeta.setFormatId(formatId);
        dataStream = new FileInputStream(test_eml_file);
        try {
         // null is the obsoleted system metadata. Inserting should succeed
            handler.save(sysmeta, true, MetacatHandler.Action.INSERT, eml_format,
                        dataStream, null, user);
            fail("Test cannot get here since the checksum is wrong in the system metadata");
        } catch (Exception e) {
            assertTrue("Should be InvalidSystemMetadata rather than " + e.getClass().getName(),
                    e instanceof InvalidSystemMetadata);
        }
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertNull("The systemmeta from db should be null.", readSys);
        String localId = null;
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
            fail("Test shouldn't get here since the local can't be found");
        } catch (Exception e) {
            assertTrue("The exception should be McdbDocNotFoundException, rather than "
                       + e.getClass().getName(), e instanceof McdbDocNotFoundException);
        }

        // This time the insert should succeed
        sysmeta.setChecksum(checksum);
        long originModificationDate = sysmeta.getDateSysMetadataModified().getTime();
        long originUploadDate = sysmeta.getDateUploaded().getTime();
        dataStream = new FileInputStream(test_eml_file);
        // null is the obsoleted system metadata. Inserting should succeed
        // True means changing the modification date in system metadata
        handler.save(sysmeta, true, MetacatHandler.Action.INSERT, eml_format,
                        dataStream, null, user);
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        assertNotEquals("The modification time in system metadata should change.",
                        originModificationDate, readSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                                originUploadDate, readSys.getDateUploaded().getTime());
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        InputStream readObj = handler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_eml_file_checksum
                    + " rather than " + readChecksum, test_eml_file_checksum.equals(readChecksum));

        // Updating
        Identifier newPid = new Identifier();
        newPid.setValue("MetacatHandler.testsave.new-" + System.currentTimeMillis());
        Checksum anotherChecksum = new Checksum();
        anotherChecksum.setAlgorithm(MD5);
        String anotherChecksumStr = "24aafe49284350445bb1ff9281e3c3c5";
        anotherChecksum.setValue(anotherChecksumStr);
        dataStream = new FileInputStream(test_eml_essdive_file);
        SystemMetadata newSys = D1NodeServiceTest.createSystemMetadata(newPid, owner, dataStream);
        newSys.setObsoletes(pid);
        sysmeta.setObsoletedBy(newPid);
        sysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        long originModificationDateOfSecondObj = newSys.getDateSysMetadataModified().getTime();
        long originUploadDateOfSecondObj = newSys.getDateUploaded().getTime();
        // Recreate the stream since it was closed in the generating sysmeta method
        dataStream = new FileInputStream(test_eml_essdive_file);
        handler.save(newSys, true, MetacatHandler.Action.UPDATE,
                     eml_format, dataStream, sysmeta, user);
        // Check the objects
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertTrue("The obsoletedBy of systemmeta from db should be " + newPid.getValue(),
                        newPid.getValue().equals(readSys.getObsoletedBy().getValue()));
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_eml_file_checksum
                    + " rather than " + readChecksum, test_eml_file_checksum.equals(readChecksum));
        // Newpid object exists.
        SystemMetadata newReadSys = SystemMetadataManager.getInstance().get(newPid);
        assertTrue("The pid of systemmeta from db should be " + newPid.getValue() + " rather than "
                        + newReadSys.getIdentifier().getValue(),
                        newPid.getValue().equals(newReadSys.getIdentifier().getValue()));
        assertTrue("The obsoletes of systemmeta from db should be " + pid.getValue(),
                        pid.getValue().equals(newReadSys.getObsoletes().getValue()));
        assertNotEquals("The modification time in system metadata should change.",
                            originModificationDateOfSecondObj,
                            newReadSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                originUploadDateOfSecondObj, newReadSys.getDateUploaded().getTime());
        localId = IdentifierManager.getInstance().getLocalId(newPid.getValue());
        readObj = handler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The object should have the checksum " + anotherChecksumStr + " rather than "
                    + readChecksum, anotherChecksumStr.equals(readChecksum));
    }

    /**
     * Test the save method to prevent overwriting an existing object
     * @throws Exception
     */
    @Test
    public void testSaveMethodWithMock() throws Exception {
        // Use the regular handler to save an eml object first
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue(eml_format);
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(test_eml_file_checksum);
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testsave-" + System.currentTimeMillis());
        InputStream dataStream = new FileInputStream(test_eml_file);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        dataStream = new FileInputStream(test_eml_file);
        // null is the obsoleted system metadata. Inserting should succeed
        String localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT, eml_format,
                        dataStream, null, user);
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        InputStream readObj = handler.read(localId, eml_format);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_eml_file_checksum
                    + " rather than " + readChecksum, test_eml_file_checksum.equals(readChecksum));
        // Check file directly
        File originalFile = new File(handler.getMetadataDir(), localId);
        assertTrue("The file should exist " + originalFile.getAbsoluteFile(),
                                                                       originalFile.exists());
        String fileChecksum = getChecksum(new FileInputStream(originalFile), MD5);
        assertTrue("The read object should have the checksum " + test_eml_file_checksum
                    + " rather than " + fileChecksum, test_eml_file_checksum.equals(fileChecksum));
        // Mock that registerToDB always return the localId, which mean the file path will always
        // be same.

        // Try to update the first object with same local id but different identifier
        MetacatHandler mockHandler = Mockito.mock(MetacatHandler.class,
                            withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        Mockito.doReturn(localId).when(mockHandler).registerToDB(any(Identifier.class),
                  any(MetacatHandler.Action.class), any(DBConnection.class),
                  any(String.class), any(String.class), any(Identifier.class));
        Identifier newPid = new Identifier();
        newPid.setValue("MetacatHandler.testsave.new-" + System.currentTimeMillis());
        Checksum anotherChecksum = new Checksum();
        anotherChecksum.setAlgorithm(MD5);
        anotherChecksum.setValue(test_eml_essdive_file_checksum);
        dataStream = new FileInputStream(test_eml_essdive_file);
        SystemMetadata newSys = D1NodeServiceTest.createSystemMetadata(newPid, owner, dataStream);
        newSys.setObsoletes(pid);
        sysmeta.setObsoletedBy(newPid);
        Date originalDate = readSys.getDateSysMetadataModified();
        sysmeta.setDateSysMetadataModified(originalDate);
        dataStream = new FileInputStream(test_eml_essdive_file);
        try {
            mockHandler.save(newSys, true, MetacatHandler.Action.UPDATE,
                                               eml_format, dataStream, sysmeta, user);
            fail("Test can't get here since the save method should throw an exception.");
        } catch (Exception e) {
            assertTrue("The exception should be ServiceFailure rather than "
                                            + e.getClass().getName(), e instanceof ServiceFailure);
        }
        // The first object doesn't change
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.",
                                                                        readSys.getObsoletedBy());
        assertTrue("The checksum from systemeta should be " + test_eml_file_checksum
                   + " rather than " + readSys.getChecksum().getValue(),
                   test_eml_file_checksum.equals(readSys.getChecksum().getValue()));
        assertEquals("The original date should be " + originalDate.getTime(),
                            originalDate.getTime(), readSys.getDateSysMetadataModified().getTime());
        readObj = handler.read(localId, eml_format);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum "
                    + test_eml_file_checksum + " rather than "
                    + readChecksum, test_eml_file_checksum.equals(readChecksum));
        originalFile = new File(handler.getMetadataDir(), localId);
        assertTrue("The file should exist " + originalFile.getAbsoluteFile(),
                                                                       originalFile.exists());
        String fileChecksum2 = getChecksum(new FileInputStream(originalFile), MD5);
        assertTrue("The read object should have the checksum "
                + test_eml_file_checksum + " rather than "
                + fileChecksum2, test_eml_file_checksum.equals(fileChecksum2));
        // The second object doesn't exist
        SystemMetadata secondSys = SystemMetadataManager.getInstance().get(newPid);
        assertNull("The system metadata for "  + newPid.getValue() + " should be null", secondSys);

        // The third try use a correct detail stream
        newPid = new Identifier();
        newPid.setValue("MetacatHandler.testsave.new2-" + System.currentTimeMillis());
        newSys.setIdentifier(newPid);
        DetailedFileInputStream dataStream3 =
                                generateDetailedInputStream(test_eml_essdive_file, anotherChecksum);
        try {
            mockHandler.save(newSys, true, MetacatHandler.Action.UPDATE,
                                            eml_format, dataStream3, sysmeta, user);
            fail("Test can't get here since the save method should throw an exception.");
        } catch (Exception e) {
            assertTrue("The exception should be ServiceFailure rather than "
                                            + e.getClass().getName(), e instanceof ServiceFailure);
        }
        // The first object doesn't change
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.",
                                                                        readSys.getObsoletedBy());
        assertTrue("The checksum from systemeta should be " + test_eml_file_checksum
                + " rather than " + readSys.getChecksum().getValue(),
                test_eml_file_checksum.equals(readSys.getChecksum().getValue()));
                            assertEquals("The original date should be " + originalDate.getTime(),
                         originalDate.getTime(), readSys.getDateSysMetadataModified().getTime());
        readObj = handler.read(localId, eml_format);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum "
                    + test_eml_file_checksum + " rather than "
                    + readChecksum, test_eml_file_checksum.equals(readChecksum));
        originalFile = new File(handler.getMetadataDir(), localId);
        assertTrue("The file should exist " + originalFile.getAbsoluteFile(),
                                                                       originalFile.exists());
        fileChecksum = getChecksum(new FileInputStream(originalFile), MD5);
        assertTrue("The read object should have the checksum "
                + test_eml_file_checksum + " rather than "
                + fileChecksum, test_eml_file_checksum.equals(fileChecksum));
        // The third object doesn't exist
        SystemMetadata thirdSys = SystemMetadataManager.getInstance().get(newPid);
        assertNull("The system metadata for "  + newPid.getValue() + " should be null", thirdSys);

        // Another try by using a wrong checksum in the detailed input stream object
        Checksum fakeChecksum = new Checksum();
        fakeChecksum.setAlgorithm(MD5);
        fakeChecksum.setValue("12234");
        DetailedFileInputStream dataStream4 =
                            generateDetailedInputStream(test_eml_essdive_file, fakeChecksum);
        newPid = new Identifier();
        newPid.setValue("MetacatHandler.testsave.new3-" + System.currentTimeMillis());
        newSys.setIdentifier(newPid);
        try {
            mockHandler.save(newSys, true, MetacatHandler.Action.UPDATE,
                                eml_format, dataStream4, sysmeta, user);
            fail("Test can't get here since the save method should throw an exception.");
        } catch (Exception e) {
            assertTrue("The exception should be ServiceFailure rather than "
                            + e.getClass().getName(), e instanceof ServiceFailure);
        }
        // The first object doesn't change
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.",
                                                                        readSys.getObsoletedBy());
        assertTrue("The checksum from systemeta should be "
                    + test_eml_file_checksum + " rather than " + readSys.getChecksum().getValue(),
                    test_eml_file_checksum.equals(readSys.getChecksum().getValue()));
        assertEquals("The original date should be " + originalDate.getTime(),
                         originalDate.getTime(), readSys.getDateSysMetadataModified().getTime());
        assertFalse("The checkshum shouldn't be " + test_eml_essdive_file_checksum,
                        test_eml_essdive_file_checksum.equals(readSys.getChecksum().getValue()));
        readObj = handler.read(localId, eml_format);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum "
                    + test_eml_file_checksum + " rather than "
                    + readChecksum, test_eml_file_checksum.equals(readChecksum));
        originalFile = new File(handler.getMetadataDir(), localId);
        assertTrue("The file should exist " + originalFile.getAbsoluteFile(),
                                                                       originalFile.exists());
        fileChecksum = getChecksum(new FileInputStream(originalFile), MD5);
        assertTrue("The read object should have the checksum "
                + test_eml_file_checksum + " rather than "
                + fileChecksum, test_eml_file_checksum.equals(fileChecksum));
        // The fourth object doesn't exist
        SystemMetadata fourSys = SystemMetadataManager.getInstance().get(newPid);
        assertNull("The system metadata for "  + newPid.getValue() + " should be null", fourSys);
    }


    /**
     * Test a failure of save process can roll back clearly
     * @throws Exception IOUtils.closeQuietly(dataStream);
     */
    @Test
    public void testSaveRollBack() throws Exception {
        // original database counts:
        long originSysCount = getRecordCount("systemmetadata");
        long originIdCount = getRecordCount("identifier");
        long originDocCount = getRecordCount("xml_documents");
        int originDocumentDirSize = (new File(handler.getMetadataDir()).list()).length;
        int originDataDirSize = (new File(handler.getDataDir()).list()).length;
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);

        // Try save a metadata inpustream in Metacat
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(test_eml_file_checksum);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testsave-" + System.currentTimeMillis());
        DetailedFileInputStream dataStream = generateDetailedInputStream(test_eml_file, checksum);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        dataStream = generateDetailedInputStream(test_eml_file, checksum);
        //Mock
        try (MockedStatic<DBConnectionPool> mockDbConnPool =
                                                Mockito.mockStatic(DBConnectionPool.class)) {
            DBConnection mockConnection = Mockito.mock(DBConnection.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            Mockito.when(DBConnectionPool.getDBConnection(any(String.class)))
                                                                .thenReturn(mockConnection);
            Mockito.doThrow(SQLException.class).when(mockConnection).commit();
            try {
                handler.save(sysmeta, true, MetacatHandler.Action.INSERT, eml_format,
                            dataStream, null, user);
                fail("Test shouldn't get there since the above method should throw an exception");
            } catch (Exception e) {
                assertTrue("It should be a ServiceFailure exception.", e instanceof ServiceFailure);
            }
        }
        // The failure change nothing.
        long sysCount = getRecordCount("systemmetadata");
        assertEquals("The records in systemmetadata should be " + originSysCount,
                                                            originSysCount, sysCount);
        long idCount = getRecordCount("identifier");
        assertEquals("The identifier count should be " + originIdCount, originIdCount, idCount);
        long docCount = getRecordCount("xml_documents");
        assertEquals("The xml_document count is " + originDocCount, originDocCount, docCount);
        int documentDirSize = (new File(handler.getMetadataDir()).list()).length;
        assertEquals("The document file count in the dir is " + originDocumentDirSize,
                                    originDocumentDirSize, documentDirSize);
        int dataDirSize = (new File(handler.getDataDir()).list()).length;
        assertEquals("The data file count in the dir is " + originDataDirSize,
                                                                originDataDirSize, dataDirSize);
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertNull("The systemmetadata for pid " + pid.getValue() + " should be null", readSys);

            // Insert a data object
            Identifier newPid = new Identifier();
            newPid.setValue("MetacatHandler.testsave2-" + System.currentTimeMillis());
            InputStream dataStream2 = new FileInputStream(test_file_path);
            SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(newPid, owner, dataStream2);
            dataStream2 = new FileInputStream(test_file_path);
            try (MockedStatic<DBConnectionPool> mockDbConnPool =
                    Mockito.mockStatic(DBConnectionPool.class)) {
                DBConnection mockConnection = Mockito.mock(DBConnection.class,
                        withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                Mockito.when(DBConnectionPool.getDBConnection(any(String.class)))
                                                                    .thenReturn(mockConnection);
                Mockito.doThrow(SQLException.class).when(mockConnection).commit();
                try {
                    handler.save(sysmeta2, true, MetacatHandler.Action.INSERT, DocumentImpl.BIN,
                            dataStream2, null, user);
                    fail("Test shouldn't get there since the above method should throw an exception");
                } catch (Exception e) {
                    assertTrue("It should be a ServiceFailure exception.", e instanceof ServiceFailure);
                }
            }
            // After a failed saving, nothing should change
            sysCount = getRecordCount("systemmetadata");
            assertEquals("The records in systemmetadata should be " + originSysCount,
                                                                originSysCount, sysCount);
            idCount = getRecordCount("identifier");
            assertEquals("The identifier count should be " + originIdCount, originIdCount, idCount);
            docCount = getRecordCount("xml_documents");
            assertEquals("The xml_document count is " + originDocCount, originDocCount, docCount);
            documentDirSize = (new File(handler.getMetadataDir()).list()).length;
            assertEquals("The document file count in the dir is " + originDocumentDirSize,
                                        originDocumentDirSize, documentDirSize);
            dataDirSize = (new File(handler.getDataDir()).list()).length;
            assertEquals("The data file count in the dir is " + originDataDirSize,
                                                                    originDataDirSize, dataDirSize);
            readSys = SystemMetadataManager.getInstance().get(newPid);
            assertNull("The systemmetadata for pid " + newPid.getValue() + " should be null", readSys);

    }

    /**
     * Test the intializeDir method
     * @throws Exception
     */
    @Test
    public void testIntializeDir() throws Exception {
        Path dir = handler.initializeDir("pom.xml");
        assertNull("The dir should be null since the path pom.xml is not directory.", dir);
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

    private long getRecordCount(String tableName) throws Exception {
        long count = -1;
        int serialNumber = -1;
        DBConnection conn = null;
        try {
            conn = DBConnectionPool.getDBConnection("MetacatHandlerTest.getRecordCount");
            serialNumber = conn.getCheckOutSerialNumber();
        } finally {
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
        String query = "SELECT count(*) FROM " + tableName;
        try (PreparedStatement statement = conn.prepareStatement(query)) {
             try (ResultSet rs = statement.executeQuery()) {
                 if (rs.next()) {
                     count = rs.getLong(1);
                 }
             }
        }
        return count;
    }

    /**
     * Generated a DetailedFileInputStream object with checksum and a temp file from the source file
     * @param filePath  the source file paht
     * @return a DetailedFileInputStream object
     * @throws Exception
     */
    private DetailedFileInputStream generateDetailedInputStream(String filePath) throws Exception{
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
}
