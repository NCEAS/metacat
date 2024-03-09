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
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
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


/**
 * The test class of MetacatHandler
 * @author tao
 *
 */
public class MetacatHandlerTest {
    private static final String test_file_path = "test/clienttestfiles/tpc02-water-flow-base.xml";
    private static final String another_test_file = "test/macbeth.xml";
    private static final String test_eml_file = "test/eml-2.2.0.xml";
    private static final String test_eml_essdive_file = "test/eml-ess-dive.xml";
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
        String checkStr = "19776e05bc62d92ab24e0597ab6f12c6";
        String localId = "autogen." + System.currentTimeMillis() +".1";
        Identifier pid = new Identifier();
        pid.setValue("foo");//Just for the log purpose
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
        String checkStr = "19776e05bc62d92ab24e0597ab6f12c6";
        String localId = "autogen." + System.currentTimeMillis() +".1";
        Identifier pid = new Identifier();
        pid.setValue("foo21");//Just for the log purpose
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(checkStr);
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
        assertTrue("The checksum from handler.read should be " + checkStr + " rather than "
                    + readChecksum, readChecksum.equals(checkStr));
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
        String checkStr = "19776e05bc62d92ab24e0597ab6f12c6";
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(checkStr);
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testsave-" + System.currentTimeMillis());
        DetailedFileInputStream dataStream = generateDetailedInputStream(test_file_path, checksum);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        dataStream = generateDetailedInputStream(test_file_path, checksum);
        // null is the obsoleted system metadata. Inserting should succeed
        handler.save(pid, sysmeta, MetacatHandler.Action.INSERT, DocumentImpl.BIN,
                    dataStream, null, user);
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        String localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        InputStream readObj = handler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));

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
            handler.save(newPid, newSys, MetacatHandler.Action.UPDATE,
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
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));
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
        // Recreate the stream since it was closed in the previous failure
        dataStream = generateDetailedInputStream(another_test_file, null);
        handler.save(newPid, newSys, MetacatHandler.Action.UPDATE,
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
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));
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
    }

    /**
     * Test the save method
     * @throws Excetpion
     */
    @Test
    public void testSaveDetailedInputStreamMetadata() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue(eml_format);
        String checkStr = "f4ea2d07db950873462a064937197b0f";
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(checkStr);
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
            handler.save(pid, sysmeta, MetacatHandler.Action.INSERT, eml_format,
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
        dataStream = generateDetailedInputStream(test_eml_file, checksum);
        // null is the obsoleted system metadata. Inserting should succeed
        handler.save(pid, sysmeta, MetacatHandler.Action.INSERT, eml_format,
                        dataStream, null, user);
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        InputStream readObj = handler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));

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
        sysmeta.setObsoletedBy(newPid);
        sysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        // Recreate the stream since it was closed in the generating sysmeta method
        dataStream = generateDetailedInputStream(test_eml_essdive_file, null);
        handler.save(newPid, newSys, MetacatHandler.Action.UPDATE,
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
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));
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
    }

    /**
     * Test the save method
     * @throws Excetpion
     */
    @Test
    public void testSaveInputStreamData() throws Exception {
        String checkStr = "19776e05bc62d92ab24e0597ab6f12c6";
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(checkStr);
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testsave-" + System.currentTimeMillis());
        InputStream dataStream = new FileInputStream(test_file_path);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        dataStream = new FileInputStream(test_file_path);
        // null is the obsoleted system metadata. Inserting should succeed
        handler.save(pid, sysmeta, MetacatHandler.Action.INSERT, DocumentImpl.BIN,
                    dataStream, null, user);
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        String localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        InputStream readObj = handler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));

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
            handler.save(newPid, newSys, MetacatHandler.Action.UPDATE,
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
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));
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
        // Recreate the stream since it was closed in the previous failure
        dataStream = new FileInputStream(another_test_file);
        handler.save(newPid, newSys, MetacatHandler.Action.UPDATE,
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
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));
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
    }

    /**
     * Test the save method
     * @throws Excetpion
     */
    @Test
    public void testSaveInputStreamMetadata() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue(eml_format);
        String checkStr = "f4ea2d07db950873462a064937197b0f";
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(checkStr);
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
            handler.save(pid, sysmeta, MetacatHandler.Action.INSERT, eml_format,
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
        dataStream = new FileInputStream(test_eml_file);
        // null is the obsoleted system metadata. Inserting should succeed
        handler.save(pid, sysmeta, MetacatHandler.Action.INSERT, eml_format,
                        dataStream, null, user);
        readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        InputStream readObj = handler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));

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
        // Recreate the stream since it was closed in the generating sysmeta method
        dataStream = new FileInputStream(test_eml_essdive_file);
        handler.save(newPid, newSys, MetacatHandler.Action.UPDATE,
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
        assertTrue("The read object should have the checksum " + checkStr + " rather than "
                    + readChecksum, checkStr.equals(readChecksum));
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
