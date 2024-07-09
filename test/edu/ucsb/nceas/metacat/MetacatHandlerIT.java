package edu.ucsb.nceas.metacat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
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
import edu.ucsb.nceas.metacat.object.handler.JsonLDHandlerTest;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.DocumentUtil;

import static org.mockito.ArgumentMatchers.any;

/**
 * The test class of MetacatHandler
 * @author tao
 *
 */
public class MetacatHandlerIT {
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
    private D1NodeServiceTest d1NodeServiceTest;

    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        handler = new MetacatHandler();
        // Initialize the storage system
        d1NodeServiceTest = new D1NodeServiceTest("initialize");
    }

    /**
     * Test the validateSciMeta method
     * @throws Exception
     */
    @Test
    public void testValidateSciMeta() throws Exception {
        // Test a valid eml2.2.0 object
        File eml = new File("test/eml-2.2.0.xml");
        try (MockedStatic<MetacatHandler> dummy = Mockito.mockStatic(MetacatHandler.class)) {
            Identifier id = new Identifier();
            id.setValue("testValidateSciMeta" + System.currentTimeMillis());
            dummy.when(() -> MetacatHandler.read(id)).thenReturn(new FileInputStream(eml));
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
            handler.validateSciMeta(id, formatId);
        }

        // Test an invalid eml2.2.0 object - duplicated ids
        eml = new File("test/resources/eml-error-2.2.0.xml");
        try (MockedStatic<MetacatHandler> dummy = Mockito.mockStatic(MetacatHandler.class)) {
            Identifier id = new Identifier();
            id.setValue("testValidateSciMeta" + System.currentTimeMillis());
            dummy.when(() -> MetacatHandler.read(id)).thenReturn(new FileInputStream(eml));
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
            try {
                handler.validateSciMeta(id, formatId);
                fail("Test can reach here since the eml object is invalid");
            } catch (Exception e) {
                assertTrue("The message should say the id must be unique",
                            e.getMessage().contains("unique"));
            }
        }

        // Test an invalid eml 2.1.1 object
        eml = new File("test/resources/eml-error.xml");
        try (MockedStatic<MetacatHandler> dummy = Mockito.mockStatic(MetacatHandler.class)) {
            Identifier id = new Identifier();
            id.setValue("testValidateSciMeta" + System.currentTimeMillis());
            dummy.when(() -> MetacatHandler.read(id)).thenReturn(new FileInputStream(eml));
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("eml://ecoinformatics.org/eml-2.1.1");
            try {
                handler.validateSciMeta(id, formatId);
                fail("Test can reach here since the eml object is invalid");
            } catch (Exception e) {
                assertTrue("The exception should be InvalidRequest", e instanceof InvalidRequest);
                assertTrue("The message should say the element principal1 is incorrect",
                            e.getMessage().contains("principal1"));
            }
        }

        // Test a valid eml 2.1.0 object
        eml = new File("test/eml-sample.xml");
        try (MockedStatic<MetacatHandler> dummy = Mockito.mockStatic(MetacatHandler.class)) {
            Identifier id = new Identifier();
            id.setValue("testValidateSciMeta" + System.currentTimeMillis());
            dummy.when(() -> MetacatHandler.read(id)).thenReturn(new FileInputStream(eml));
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
            handler.validateSciMeta(id, formatId);
        }

        // Test a valid eml-beta 6
        eml = new File("./test/jones.204.22.xml");
        try (MockedStatic<MetacatHandler> dummy = Mockito.mockStatic(MetacatHandler.class)) {
            Identifier id = new Identifier();
            id.setValue("testValidateSciMeta" + System.currentTimeMillis());
            dummy.when(() -> MetacatHandler.read(id)).thenReturn(new FileInputStream(eml));
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN");
            handler.validateSciMeta(id, formatId);
        }

        // Test a valid json object
        File json = new File("test/json-ld.json");
        try (MockedStatic<MetacatHandler> dummy = Mockito.mockStatic(MetacatHandler.class)) {
            Identifier id = new Identifier();
            id.setValue("testValidateSciMeta" + System.currentTimeMillis());
            dummy.when(() -> MetacatHandler.read(id)).thenReturn(new FileInputStream(json));
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue(NonXMLMetadataHandlers.JSON_LD);
            handler.validateSciMeta(id, formatId);
        }

        // Test a invalid json object
        json = new File("test/invalid-json-ld.json");
        try (MockedStatic<MetacatHandler> dummy = Mockito.mockStatic(MetacatHandler.class)) {
            Identifier id = new Identifier();
            id.setValue("testValidateSciMeta" + System.currentTimeMillis());
            dummy.when(() -> MetacatHandler.read(id)).thenReturn(new FileInputStream(json));
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue(NonXMLMetadataHandlers.JSON_LD);
            try {
                handler.validateSciMeta(id, formatId);
                fail("Test can reach here since the json-ld object is invalid");
            } catch (Exception e) {
                assertTrue("The exception should be InvalidRequest",e instanceof InvalidRequest);
            }
        }
    }

    /**
     * Test the read method
     * @throws Exception
     */
    @Test
    public void testRead() throws Exception {
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testRead-" + System.currentTimeMillis());
        FileInputStream dataStream = new FileInputStream(test_file_path);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        dataStream = new FileInputStream(test_file_path);
        d1NodeServiceTest.mnCreate(d1NodeServiceTest.getTestSession(), pid, dataStream, sysmeta);
        InputStream readObj = MetacatHandler.read(pid);
        String readChecksum = getChecksum(readObj, MD5);
        assertEquals("The read object should have the checksum " + test_file_checksum + " rather "
                       + "than " + readChecksum, test_file_checksum, readChecksum);
        String localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        readObj = MetacatHandler.read(localId, null);
        readChecksum = getChecksum(readObj, MD5);
        assertEquals("The read object should have the checksum " + test_file_checksum + " rather "
                         + "than " + readChecksum, test_file_checksum, readChecksum);
        pid.setValue("MetacatHandler.testRead.foo-" + System.currentTimeMillis());
        try  {
            MetacatHandler.read(pid);
            fail("Test can't get there since MetacatHandler read a non-existed pid");
        } catch (Exception e) {
            assertTrue(e instanceof McdbException);
        }
        try  {
            MetacatHandler.read("metacatHandlerRead123." + System.currentTimeMillis() + ".1", null);
            fail("Test can't get there since MetacatHandler read a non-existed pid");
        } catch (Exception e) {
            assertTrue(e instanceof McdbException);
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
            String newLocalId = handler.registerToDB(newPid, Action.UPDATE, dbConn, "user-data",
                                        DocumentImpl.BIN, prePid);
            String newDocid = newLocalId.substring(0, localId.lastIndexOf("."));
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                            " rev=? and guid like ? and docid like ? ", 1, pid.getValue(), docid));
            assertTrue("The identifier table should have value",
                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                       " rev=? and guid like ? and docid like ? ", 1, newPid.getValue(), newDocid));
            assertTrue("The xml_documents table should have value",
                    IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                    " rev=? and docid like ?", 1, docid));
            assertTrue("The xml_documents table should have value",
                    IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                    " rev=? and docid like ?", 1, newDocid));
            assertFalse("The xml_revisions table should not have value",
                   IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                   " rev=? and docid like ?", 1, docid));
            assertFalse("The xml_revisions table should not have value",
                    IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                    " rev=? and docid like ?", 1, newDocid));
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
     * Test the save method
     * @throws Exception
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
        FileInputStream dataStream = new FileInputStream(test_file_path);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        long originModificationDate = sysmeta.getDateSysMetadataModified().getTime();
        long originUploadDate = sysmeta.getDateUploaded().getTime();
        dataStream = new FileInputStream(test_file_path);
        D1NodeServiceTest.storeData(dataStream, sysmeta);
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
        InputStream readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
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
        dataStream = new FileInputStream(another_test_file);
        SystemMetadata newSys = D1NodeServiceTest.createSystemMetadata(newPid, owner, dataStream);
        newSys.setObsoletes(pid);
        sysmeta.setObsoletedBy(newPid);
        Checksum incorrectChecksum = new Checksum();
        incorrectChecksum.setAlgorithm(MD5);
        incorrectChecksum.setValue("234dfa343af");
        newSys.setChecksum(incorrectChecksum);
        try {
            // True means changing the modification date in the system metadata
            dataStream = new FileInputStream(another_test_file);
            D1NodeServiceTest.storeData(dataStream, newSys);
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
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertEquals("The read object has a wrong checksum ", test_file_checksum, readChecksum);
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
        dataStream = new FileInputStream(another_test_file);
        // True means changing the modification date in the system metadata
        D1NodeServiceTest.storeData(dataStream, newSys);
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
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertEquals("The read object should has a wrong checksum ", test_file_checksum,
                    readChecksum);
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
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertEquals("The handler.read has a wrong checksum ", anotherChecksumStr, readChecksum);
        assertNotEquals("The modification time in system metadata should change.",
                            originModificationDateOfSecondObj,
                            newReadSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                            originUploadDateOfSecondObj, newReadSys.getDateUploaded().getTime());
    }

    /**
     * Test the save method
     * @throws Exception
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
        FileInputStream dataStream = new FileInputStream(test_eml_file);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        Checksum incorrectChecksum = new Checksum();
        incorrectChecksum.setAlgorithm(MD5);
        incorrectChecksum.setValue("234dfa343af");
        sysmeta.setChecksum(incorrectChecksum);
        sysmeta.setFormatId(formatId);
        dataStream = new FileInputStream(test_eml_file);
        try {
            // null is the obsoleted system metadata. Inserting should succeed
            D1NodeServiceTest.storeData(dataStream, sysmeta);
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
        dataStream = new FileInputStream(test_eml_file);
        D1NodeServiceTest.storeData(dataStream, sysmeta);
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
        InputStream readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertEquals("The read object has a wrong checksum ", test_eml_file_checksum
                     , readChecksum);

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
        long originModificationDateOfSecondObj = newSys.getDateSysMetadataModified().getTime();
        long originUploadDateOfSecondObj = newSys.getDateUploaded().getTime();
        sysmeta.setObsoletedBy(newPid);
        sysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        // Recreate the stream since it was closed in the generating sysmeta method
        dataStream = new FileInputStream(test_eml_essdive_file);
        D1NodeServiceTest.storeData(dataStream, newSys);
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
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
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
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
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
     * @throws Exception
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
        D1NodeServiceTest.storeData(dataStream, sysmeta);
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
        InputStream readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
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
            dataStream = new FileInputStream(another_test_file);
            D1NodeServiceTest.storeData(dataStream, newSys);
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
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertEquals("The read object has a wrong checksum ", test_file_checksum, readChecksum);
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
        D1NodeServiceTest.storeData(dataStream, newSys);
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
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertEquals("The read object has a wrong checksum ", test_file_checksum, readChecksum);
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
        assertEquals("The modification time in system metadata should not change.",
                        originModificationDateOfSecondObj,
                        newReadSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time in system metadata should not change.",
                        originUploadDateOfSecondObj, newReadSys.getDateUploaded().getTime());
        localId = IdentifierManager.getInstance().getLocalId(newPid.getValue());
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertEquals("The read object has a wrong checksum ", anotherChecksumStr, readChecksum);
    }

    /**
     * Test the save method
     * @throws Exception
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
            D1NodeServiceTest.storeData(dataStream, sysmeta);
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
        D1NodeServiceTest.storeData(dataStream, sysmeta);
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
        InputStream readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
        String readChecksum = getChecksum(readObj, MD5);
        assertEquals("The read object has a wrong checksum ", test_eml_file_checksum, readChecksum);

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
        D1NodeServiceTest.storeData(dataStream, newSys);
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
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_eml_file_checksum
                    + " rather than " + readChecksum, test_eml_file_checksum.equals(readChecksum));
        assertNotEquals("The modification time of the obsoleted object in should change.",
                        originModificationDate, readSys.getDateSysMetadataModified().getTime());
        assertEquals("The upload time of the obsoleted object should not change.",
                        originUploadDate, readSys.getDateUploaded().getTime());
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
        readObj = MetacatHandler.read(localId, DocumentImpl.BIN);
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
        D1NodeServiceTest.storeData(dataStream, sysmeta);
        // null is the obsoleted system metadata. Inserting should succeed
        String localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT, eml_format,
                        dataStream, null, user);
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertTrue("The pid of systemmeta from db should be " + pid.getValue() + " rather than "
                              + readSys.getIdentifier().getValue(),
                              pid.getValue().equals(readSys.getIdentifier().getValue()));
        assertNull("The obsoletedBy of systemmeta from db should be null.", readSys.getObsoletedBy());
        InputStream readObj = MetacatHandler.read(localId, eml_format);
        String readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum " + test_eml_file_checksum
                    + " rather than " + readChecksum, test_eml_file_checksum.equals(readChecksum));
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
            assertTrue("The exception should be McdbException rather than "
                                            + e.getClass().getName(), e instanceof McdbException);
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
        readObj = MetacatHandler.read(localId, eml_format);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum "
                    + test_eml_file_checksum + " rather than "
                    + readChecksum, test_eml_file_checksum.equals(readChecksum));
        // The second object doesn't exist
        SystemMetadata secondSys = SystemMetadataManager.getInstance().get(newPid);
        assertNull("The system metadata for "  + newPid.getValue() + " should be null", secondSys);

        // The third try use a correct detail stream
        newPid = new Identifier();
        newPid.setValue("MetacatHandler.testsave.new2-" + System.currentTimeMillis());
        newSys.setIdentifier(newPid);
        FileInputStream dataStream3 =
                                new FileInputStream(test_eml_essdive_file);
        try {
            mockHandler.save(newSys, true, MetacatHandler.Action.UPDATE,
                                            eml_format, dataStream3, sysmeta, user);
            fail("Test can't get here since the save method should throw an exception.");
        } catch (Exception e) {
            assertTrue("The exception should be McdbException rather than "
                                            + e.getClass().getName(), e instanceof McdbException);
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
        readObj = MetacatHandler.read(localId, eml_format);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum "
                    + test_eml_file_checksum + " rather than "
                    + readChecksum, test_eml_file_checksum.equals(readChecksum));
        // The third object doesn't exist
        SystemMetadata thirdSys = SystemMetadataManager.getInstance().get(newPid);
        assertNull("The system metadata for "  + newPid.getValue() + " should be null", thirdSys);

        // Another try by using a wrong checksum in the detailed input stream object
        Checksum fakeChecksum = new Checksum();
        fakeChecksum.setAlgorithm(MD5);
        fakeChecksum.setValue("12234");
        FileInputStream dataStream4 =
                            new FileInputStream(test_eml_essdive_file);
        newPid = new Identifier();
        newPid.setValue("MetacatHandler.testsave.new3-" + System.currentTimeMillis());
        newSys.setIdentifier(newPid);
        try {
            mockHandler.save(newSys, true, MetacatHandler.Action.UPDATE,
                                eml_format, dataStream4, sysmeta, user);
            fail("Test can't get here since the save method should throw an exception.");
        } catch (Exception e) {
            assertTrue("The exception should be McdbException rather than "
                            + e.getClass().getName(), e instanceof McdbException);
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
        readObj = MetacatHandler.read(localId, eml_format);
        readChecksum = getChecksum(readObj, MD5);
        assertTrue("The read object should have the checksum "
                    + test_eml_file_checksum + " rather than "
                    + readChecksum, test_eml_file_checksum.equals(readChecksum));
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
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);

        // Try save a metadata inpustream in Metacat
        Checksum checksum = new Checksum();
        checksum.setAlgorithm(MD5);
        checksum.setValue(test_eml_file_checksum);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testsave-" + System.currentTimeMillis());
        FileInputStream dataStream = new FileInputStream(test_eml_file);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, dataStream);
        dataStream = new FileInputStream(test_eml_file);
        D1NodeServiceTest.storeData(dataStream, sysmeta);
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
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertNull("The systemmetadata for pid " + pid.getValue() + " should be null", readSys);

            // Insert a data object
            Identifier newPid = new Identifier();
            newPid.setValue("MetacatHandler.testsave2-" + System.currentTimeMillis());
            InputStream dataStream2 = new FileInputStream(test_file_path);
            SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(newPid, owner, dataStream2);
            dataStream2 = new FileInputStream(test_file_path);
            D1NodeServiceTest.storeData(dataStream2, sysmeta2);
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
            readSys = SystemMetadataManager.getInstance().get(newPid);
            assertNull("The systemmetadata for pid " + newPid.getValue() + " should be null", readSys);

    }


    /**
     * Test the save method for JsonLD
     * @throws Exception
     */
    @Test
    public void testSaveJsonLD() throws Exception {
        String user = "http://orcid.org/1234/4567";
        Subject subject = new Subject();
        subject.setValue(user);
        Checksum expectedChecksum = new Checksum();
        expectedChecksum.setAlgorithm("MD5");
        expectedChecksum.setValue(JsonLDHandlerTest.CHECKSUM_JSON_FILE);
        ObjectFormatIdentifier format = new ObjectFormatIdentifier();
        format.setValue(NonXMLMetadataHandlers.JSON_LD);
        SystemMetadata sysmeta = new SystemMetadata();

        //save the DetailedFileInputStream from the valid json-ld object without a checksum
        InputStream data = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        MetacatHandler handler = new MetacatHandler();
        Identifier pid = new Identifier();
        pid.setValue("test-id1-" + System.currentTimeMillis());
        sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
        sysmeta.setFormatId(format);
        data = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        D1NodeServiceTest.storeData(data, sysmeta);
        // True means changing the modification date in the system metadata
        String localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                                       NonXMLMetadataHandlers.JSON_LD, data, null, user);

        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile = MetacatInitializer.getStorage().findObject(sysmeta.getIdentifier());
        assertTrue(savedFile.exists());

        //save the DetaiedFileInputStream from the invalid json-ld object without a checksum
        data = new FileInputStream(new File(JsonLDHandlerTest.INVALID_JSON_LD_FILE_PATH));
        pid = new Identifier();
        pid.setValue("test-id3-" + System.currentTimeMillis());
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setFormatId(format);
            data = new FileInputStream(new File(JsonLDHandlerTest.INVALID_JSON_LD_FILE_PATH));
            D1NodeServiceTest.storeData(data, sysmeta);
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
     * Save the jsonLD object coming from a byte array input stream
     * @throws Exception
     */
    @Test
    public void testSaveJsonLDByteArray() throws Exception {
        String user = "http://orcid.org/1234/4567";
        Subject subject = new Subject();
        subject.setValue(user);
        ObjectFormatIdentifier format = new ObjectFormatIdentifier();
        format.setValue(NonXMLMetadataHandlers.JSON_LD);
        Checksum expectedChecksum = new Checksum();
        expectedChecksum.setAlgorithm("MD5");
        expectedChecksum.setValue(JsonLDHandlerTest.CHECKSUM_JSON_FILE);

        Checksum expectedChecksumForInvalidJson = new Checksum();
        expectedChecksumForInvalidJson.setAlgorithm("MD5");
        expectedChecksumForInvalidJson.setValue(JsonLDHandlerTest.CHECKSUM_INVALID_JSON_FILE);

        //save a valid json file with correct expected checksum
        String content =
                FileUtils.readFileToString(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH), "UTF-8");
        InputStream data = new ByteArrayInputStream(content.getBytes());
        MetacatHandler handler = new MetacatHandler();
        Identifier pid = new Identifier();
        pid.setValue("testbye-id1-" + System.currentTimeMillis());
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
        sysmeta.setFormatId(format);
        data = new ByteArrayInputStream(content.getBytes());
        D1NodeServiceTest.storeData(data, sysmeta);
        // True means changing the modification date in the system metadata
        String localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                                    NonXMLMetadataHandlers.JSON_LD, data, null, user);
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile = MetacatInitializer.getStorage().findObject(sysmeta.getIdentifier());
        assertTrue(savedFile.exists());


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
            D1NodeServiceTest.storeData(data, sysmeta);
            // True means changing the modification date in the system metadata
            localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                    NonXMLMetadataHandlers.JSON_LD, data, null, user);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidSystemMetadata);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));

        //save an invalid jsonld file
        content =FileUtils
                  .readFileToString(new File(JsonLDHandlerTest.INVALID_JSON_LD_FILE_PATH), "UTF-8");
        data = new ByteArrayInputStream(content.getBytes());
        pid = new Identifier();
        pid.setValue("testbye-id3-" + System.currentTimeMillis());
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setIdentifier(pid);
            sysmeta.setFormatId(format);
            sysmeta.setChecksum(expectedChecksumForInvalidJson);
            data = new ByteArrayInputStream(content.getBytes());
            D1NodeServiceTest.storeData(data, sysmeta);
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
        String user = "http://orcid.org/1234/4567";
        Subject subject = new Subject();
        subject.setValue(user);
        ObjectFormatIdentifier format = new ObjectFormatIdentifier();
        format.setValue(NonXMLMetadataHandlers.JSON_LD);
        Checksum expectedChecksum = new Checksum();
        expectedChecksum.setAlgorithm("MD5");
        expectedChecksum.setValue(JsonLDHandlerTest.CHECKSUM_JSON_FILE);

        Checksum expectedChecksumForInvalidJson = new Checksum();
        expectedChecksumForInvalidJson.setAlgorithm("MD5");
        expectedChecksumForInvalidJson.setValue(JsonLDHandlerTest.CHECKSUM_INVALID_JSON_FILE);

        //save a valid json file with correct expected checksum
        InputStream data = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        MetacatHandler handler = new MetacatHandler();
        Identifier pid = new Identifier();
        pid.setValue("testSaveFile-id1-" + System.currentTimeMillis());
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
        sysmeta.setIdentifier(pid);
        sysmeta.setFormatId(format);
        sysmeta.setChecksum(expectedChecksum);
        data = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        D1NodeServiceTest.storeData(data, sysmeta);
        // True means changing the modification date in the system metadata
        String localId = handler.save(sysmeta, true, MetacatHandler.Action.INSERT,
                            NonXMLMetadataHandlers.JSON_LD, data, null, user);
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile = MetacatInitializer.getStorage().findObject(sysmeta.getIdentifier());
        assertTrue(savedFile.exists());
        data.close();


        //save the  valid json-ld object with the wrong checksum
        data = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        pid = new Identifier();
        pid.setValue("testSaveFile-id2-" + System.currentTimeMillis());
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setIdentifier(pid);
            sysmeta.setFormatId(format);
            sysmeta.setChecksum(expectedChecksumForInvalidJson);
            data = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
            D1NodeServiceTest.storeData(data, sysmeta);
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
        data = new FileInputStream(new File(JsonLDHandlerTest.INVALID_JSON_LD_FILE_PATH));
        try {
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid, subject, data);
            sysmeta.setIdentifier(pid);
            sysmeta.setFormatId(format);
            sysmeta.setChecksum(expectedChecksumForInvalidJson);
            data = new FileInputStream(new File(JsonLDHandlerTest.INVALID_JSON_LD_FILE_PATH));
            D1NodeServiceTest.storeData(data, sysmeta);
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

}
