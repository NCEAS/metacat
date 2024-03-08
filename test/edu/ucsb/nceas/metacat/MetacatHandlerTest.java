package edu.ucsb.nceas.metacat;

import java.io.File;
import java.io.FileInputStream;
import java.sql.PreparedStatement;


import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import edu.ucsb.nceas.IntegrationTestUtils;
import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.MetacatHandler.Action;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;


/**
 * The test class of MetacatHandler
 * @author tao
 *
 */
public class MetacatHandlerTest {
    private MetacatHandler handler;

    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        handler = new MetacatHandler();
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
            assertTrue("The exception should be SAXException", e instanceof SAXException);
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
                fail("Test can not reach here since it should throw a MetacatException");
            } catch (Exception e) {
                assertTrue("The exception should be MetacatException ",
                            e instanceof MetacatException);
            }
            pid.setValue("testRegisterToDBData1-" + System.currentTimeMillis());
            try {
                // doctype null will be rejected.
                handler.registerToDB(pid, Action.INSERT, dbConn, "user-foo", null, prePid);
                fail("Test can not reach here since it should throw a MetacatException");
            } catch (Exception e) {
                assertTrue("The exception should be MetacatException ",
                            e instanceof MetacatException);
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

}
