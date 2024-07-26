package edu.ucsb.nceas.metacat;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;

import edu.ucsb.nceas.metacat.systemmetadata.MCSystemMetadataTest;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.MediaType;
import org.dataone.service.types.v2.MediaTypeProperty;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import edu.ucsb.nceas.IntegrationTestUtils;
import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeReplicationTest;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.DocumentUtil;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;
import static org.mockito.ArgumentMatchers.any;



/**
 * The integrated class for the DocumentImpl class
 * @author tao
 *
 */
public class DocumentImplIT {

    private D1NodeServiceTest d1NodeTest;
    private MockHttpServletRequest request;

    /**
     * Set up
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
        d1NodeTest = new D1NodeServiceTest("initialize");
        request = (MockHttpServletRequest)d1NodeTest.getServletRequest();
    }

    /**
     * Test the delete method
     * @throws Exception
     */
    @Test
    public void testDeleteData() throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
             // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("SystemMetadataManager.deleteData");
            serialNumber = dbConn.getCheckOutSerialNumber();
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("DocumentImpl_deleteData." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest
                                            .createSystemMetadata(guid, session.getSubject(), object);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            //check record
            assertTrue("The identifier table should have value",
                                    IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The checksums table should have value",
                       IntegrationTestUtils.hasRecord("checksums", dbConn,
                                                      " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                                     IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                         IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                                    IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                                    IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                                   IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                 " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                                       IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                           " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                        " docid like ?", docid));
            InputStream input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
            SystemMetadata read = SystemMetadataManager.getInstance().get(guid);
            assertEquals(guid, read.getIdentifier());
            SystemMetadata sysmetaFromHash;
            try (InputStream metaInput = D1NodeServiceTest.getStorage().retrieveMetadata(guid)) {
                sysmetaFromHash = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                                                                          metaInput);
            }
            assertEquals(guid, sysmetaFromHash.getIdentifier());

            //update
            Identifier newPid = new Identifier();
            newPid.setValue("DocumentImpl_deleteData." + (System.currentTimeMillis() + 1)); 
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
            ReplicationPolicy policy = new ReplicationPolicy();
            NodeReference node = new NodeReference();
            node.setValue("test_node");
            policy.addPreferredMemberNode(node);
            policy.setReplicationAllowed(true);
            policy.setNumberReplicas(2);
            sysmeta.setReplicationPolicy(policy);
            Replica replica = new Replica();
            replica.setReplicaMemberNode(node);
            replica.setReplicationStatus(ReplicationStatus.COMPLETED);
            replica.setReplicaVerified(new Date());
            sysmeta.addReplica(replica);
            MediaTypeProperty property = new MediaTypeProperty();
            property.setName("test_media_name");
            property.setValue("test_media_value");
            MediaType type = new MediaType();
            type.addProperty(property);
            type.setName("test_name1");
            sysmeta.setMediaType(type);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            d1NodeTest.mnUpdate(session, guid, object, newPid, sysmeta);
            //check record
            assertTrue("The identifier table should have value",
                                 IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                             " guid like ?", newPid.getValue()));
            assertTrue("The checksums table should have value",
                       IntegrationTestUtils.hasRecord("checksums", dbConn,
                                                      " guid like ?", newPid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                " guid like ?", newPid.getValue()));
            assertTrue("The xml_access table should have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                       IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationstatus table should have value",
                     IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smmediatypeproperties table should have value",
                     IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                               " guid like ?", newPid.getValue()));
            String accnum2 = IdentifierManager.getInstance().getLocalId(newPid.getValue());
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertTrue("The xml_documents table should have value",
                                       IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                           " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                           IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                             " rev=? and docid like ?", 1, docid));
            input = MetacatHandler.read(accnum2, null);
            assertNotNull("The file should exist", input);
            input.close();


            //Delete
            SystemMetadataManager.lock(guid);
            DocumentImpl.delete(accnum, guid);
            SystemMetadataManager.unLock(guid);
            assertTrue("The identifier table should have value",
                                IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The systemmetadata table should not have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The checksums table should not have value",
                        IntegrationTestUtils.hasRecord("checksums", dbConn,
                                                       " guid like ?", guid.getValue()));
            assertFalse("The xml_access table should not have value",
                                IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                      IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                  " guid like ?", guid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertFalse("The xml_documents table should not have value",
                                        IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                        " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                        " docid like ?", docid));
            try {
                input = MetacatHandler.read(accnum, null);
                fail("The test can not get here since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof McdbDocNotFoundException);
            }
            SystemMetadata read2 = SystemMetadataManager.getInstance().get(guid);
            assertNull("The system metadata should be deleted.", read2);
            try {
                D1NodeServiceTest.getStorage().retrieveMetadata(guid);
                fail("Test shouldn't get there since the system metadata was deleted.");
            } catch (Exception ee) {
                assertTrue(ee instanceof FileNotFoundException);
            }

            SystemMetadataManager.lock(newPid);
            DocumentImpl.delete(accnum2, newPid);
            SystemMetadataManager.unLock(newPid);
            //check record
            assertTrue("The identifier table should have value",
                                 IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                              " guid like ?", newPid.getValue()));
            assertFalse("The systemmetadata table should not have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                            " guid like ?", newPid.getValue()));
            assertFalse("The checksums table should not have value",
                        IntegrationTestUtils.hasRecord("checksums", dbConn,
                                                       " guid like ?", newPid.getValue()));
            assertFalse("The xml_access table should not have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                " guid like ?", newPid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", newPid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                     IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                             " guid like ?", newPid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                     IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                             " guid like ?", newPid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertFalse("The xml_documents table should not have value",
                                       IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                           " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                           IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                           " rev=? and docid like ?", 1, docid));
            try {
                input = MetacatHandler.read(accnum2, null);
                fail("The test can not get here since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof McdbDocNotFoundException);
            }
            SystemMetadata read3 = SystemMetadataManager.getInstance().get(newPid);
            assertNull("The system metadata should be deleted.", read3);
            try {
                D1NodeServiceTest.getStorage().retrieveMetadata(newPid);
                fail("Test shouldn't get there since the system metadata was deleted.");
            } catch (Exception ee) {
                assertTrue(ee instanceof FileNotFoundException);
            }
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Test the delete method
     * @throws Exception
     */
    @Test
    public void testDeleteMetadata() throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
             // Get a database connection from the pool
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
            dbConn = DBConnectionPool.getDBConnection("SystemMetadataManager.deleteData");
            serialNumber = dbConn.getCheckOutSerialNumber();
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("DocumentImpl_deleteMetadata." + System.currentTimeMillis());
            InputStream object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
            SystemMetadata sysmeta = D1NodeServiceTest
                                          .createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            object.close();
            object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            object.close();
            //check record
            assertTrue("The identifier table should have value",
                                  IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                              " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                             IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                       IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                               " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                                       IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                       " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                        " docid like ?", docid));
            InputStream input = MetacatHandler.read(guid);
            assertNotNull("The file should exist", input);
            input.close();

            //update
            Identifier newPid = new Identifier();
            newPid.setValue("DocumentImpl_deleteData." + (System.currentTimeMillis() + 1));
            object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
            sysmeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
            object.close();
            sysmeta.setFormatId(formatId);
            ReplicationPolicy policy = new ReplicationPolicy();
            NodeReference node = new NodeReference();
            node.setValue("test_node");
            policy.addPreferredMemberNode(node);
            policy.setReplicationAllowed(true);
            policy.setNumberReplicas(2);
            sysmeta.setReplicationPolicy(policy);
            Replica replica = new Replica();
            replica.setReplicaMemberNode(node);
            replica.setReplicationStatus(ReplicationStatus.COMPLETED);
            replica.setReplicaVerified(new Date());
            sysmeta.addReplica(replica);
            MediaTypeProperty property = new MediaTypeProperty();
            property.setName("test_media_name");
            property.setValue("test_media_value");
            MediaType type = new MediaType();
            type.addProperty(property);
            type.setName("test_name");
            sysmeta.setMediaType(type);
            object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
            d1NodeTest.mnUpdate(session, guid, object, newPid, sysmeta);
            object.close();
            //check record
            assertTrue("The identifier table should have value",
                                 IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                             " guid like ?", newPid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                " guid like ?", newPid.getValue()));
            assertTrue("The xml_access table should have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                             " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                       IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationstatus table should have value",
                     IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smmediatypeproperties table should have value",
                     IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                             " guid like ?", newPid.getValue()));
            String accnum2 = IdentifierManager.getInstance().getLocalId(newPid.getValue());
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertTrue("The xml_documents table should have value",
                           IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                           " rev=? and docid like ?", 2, docid));
            assertTrue("The xml_revisions table should have value",
                           IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                              " rev=? and docid like ?", 1, docid));
            input = MetacatHandler.read(accnum2, null);
            assertNotNull("The file should exist", input);
            input.close();

            //Delete
            SystemMetadataManager.lock(guid);
            DocumentImpl.delete(accnum, guid);
            SystemMetadataManager.unLock(guid);
            assertTrue("The identifier table should have value",
                                IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The systemmetadata table should not have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The xml_access table should not have value",
                                IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                      IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                 " guid like ?", guid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            // the version 2 still exists
            assertTrue("The xml_documents table should have value",
                            IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                            " rev=? and docid like ?", 2, docid));
            assertFalse("The xml_revisions table should not have value",
                                        IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                        " docid like ?", docid));
            try {
                input = MetacatHandler.read(guid);
                fail("The test can not get here since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof McdbDocNotFoundException);
            }

            SystemMetadataManager.lock(newPid);
            DocumentImpl.delete(accnum2, newPid);
            SystemMetadataManager.unLock(newPid);
            //check record
            assertTrue("The identifier table should have value",
                                 IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                             " guid like ?", newPid.getValue()));
            assertFalse("The systemmetadata table should not have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                " guid like ?", newPid.getValue()));
            assertFalse("The xml_access table should not have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                     IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                     IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                               " guid like ?", newPid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertFalse("The xml_documents table should have value",
                           IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                       " rev=? and docid like ?", 2, docid));
            assertFalse("The xml_revisions table should not have value",
                           IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                           " rev=? and docid like ?", 1, docid));
            try {
                input = MetacatHandler.read(accnum2, null);
                fail("The test can not get here since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof McdbDocNotFoundException);
            }
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Test the delete method
     * @throws Exception
     */
    @Test
    public void testFailedDelete() throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
             // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("SystemMetadataManager.deleteData");
            serialNumber = dbConn.getCheckOutSerialNumber();
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("DocumentImpl_failedDeleteData." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest
                                          .createSystemMetadata(guid, session.getSubject(), object);
            ReplicationPolicy policy = new ReplicationPolicy();
            NodeReference node = new NodeReference();
            node.setValue("test_node");
            policy.addPreferredMemberNode(node);
            policy.setReplicationAllowed(true);
            policy.setNumberReplicas(2);
            sysmeta.setReplicationPolicy(policy);
            Replica replica = new Replica();
            replica.setReplicaMemberNode(node);
            replica.setReplicationStatus(ReplicationStatus.COMPLETED);
            replica.setReplicaVerified(new Date());
            sysmeta.addReplica(replica);
            MediaTypeProperty property = new MediaTypeProperty();
            property.setName("test_media_name");
            property.setValue("test_media_value");
            MediaType type = new MediaType();
            type.addProperty(property);
            type.setName("test_name");
            sysmeta.setMediaType(type);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            //check record
            assertTrue("The identifier table should have value",
                                  IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                  " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                             IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The smreplicationstatus table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The smmediatypeproperties table should not have value",
                       IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                                       IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                       " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                        " docid like ?", docid));
            InputStream input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();

            //Mock a failed deleting
            try (MockedStatic<DBConnectionPool> mockDbConnPool =
                     Mockito.mockStatic(DBConnectionPool.class)) {
                DBConnection mockConnection = Mockito.mock(
                    DBConnection.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                Mockito.when(DBConnectionPool.getDBConnection(any(String.class)))
                    .thenReturn(mockConnection);
                Mockito.doThrow(SQLException.class).when(mockConnection).commit();
                try {
                    SystemMetadataManager.lock(guid);
                    DocumentImpl.delete(accnum, guid);
                    fail("The test can't be here since the failed delete should throw an exception");
                } catch (Exception e) {
                    SystemMetadataManager.unLock(guid);
                    assertTrue("The exception class should be ServiceFailure",
                                                                      e instanceof ServiceFailure);
                }
            }
            //Records in the db shouldn't change
            assertTrue("The identifier table should have value",
                                IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                   IntegrationTestUtils.hasRecord("xml_access", dbConn, " guid like ?",
                                                               guid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                            " guid like ?", guid.getValue()));
            assertTrue("The smreplicationstatus table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                            " guid like ?", guid.getValue()));
            assertTrue("The smmediatypeproperties table should not have value",
                      IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The xml_documents table should have value",
                                        IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                        " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                       IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                       " docid like ?", docid));
            input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Test the archive method
     * @throws Exception
     */
    @Test
    public void testArchiveData() throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
             // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("SystemMetadataManager.deleteData");
            serialNumber = dbConn.getCheckOutSerialNumber();
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("DocumentImpl_archiveData." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest
                                            .createSystemMetadata(guid, session.getSubject(), object);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            //check record
            assertTrue("The identifier table should have value",
                                  IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                  " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                             IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                       IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                 " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                                       IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                       " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                        " docid like ?", docid));
            InputStream input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
            SystemMetadata sys = SystemMetadataManager.getInstance().get(guid);
            Date originalDateModified = sys.getDateSysMetadataModified();
            assertFalse("System metadata should have archived false", sys.getArchived());

            //archive
            String user = "test";
            // Set changeDateModified false
            SystemMetadataManager.lock(guid);
            DocumentImpl.archive(accnum, guid, user, false,
                                SystemMetadataManager.SysMetaVersion.UNCHECKED);
            SystemMetadataManager.unLock(guid);
            assertTrue("The identifier table should have value",
                                IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                      IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                  " guid like ?", guid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertFalse("The xml_documents table should have value",
                                        IntegrationTestUtils.hasRecord("xml_documents",
                                                                dbConn, " docid like ?", docid));
            assertTrue("The xml_revisions table should have value",
                                        IntegrationTestUtils.hasRecord("xml_revisions",
                                                                  dbConn, " docid like ?", docid));
            input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
            sys = SystemMetadataManager.getInstance().get(guid);
            assertTrue("System metadata should have archived true", sys.getArchived());
            Date currentDateModified = sys.getDateSysMetadataModified();
            assertEquals("The current dateModified should equal the original one.",
                        originalDateModified.getTime(), currentDateModified.getTime());

        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Test the archive method when it fails
     * @throws Exception
     */
    @Test
    public void testArchiveFailure() throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("SystemMetadataManager.deleteData");
            serialNumber = dbConn.getCheckOutSerialNumber();
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("DocumentImpl_archiveData." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest
                .createSystemMetadata(guid, session.getSubject(), object);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            //check record
            assertTrue("The identifier table should have value",
                       IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                      " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                       IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                      " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                       IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                      " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                       " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                       " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                        IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                       " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                       IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                      " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                        IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                       " docid like ?", docid));
            InputStream input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
            SystemMetadata originSys = SystemMetadataManager.getInstance().get(guid);
            Date originalDateUploaded = originSys.getDateUploaded();
            Date originalDateModified = originSys.getDateSysMetadataModified();
            BigInteger version = originSys.getSerialVersion();
            assertFalse("System metadata should have archived false", originSys.getArchived());

            //archive
            String user = "test";
            try (MockedStatic<DBConnectionPool> mockDbConnPool =
                     Mockito.mockStatic(DBConnectionPool.class)) {
                DBConnection mockConnection = Mockito.mock(DBConnection.class,
                                                           withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                Mockito.when(DBConnectionPool.getDBConnection(any(String.class)))
                    .thenReturn(mockConnection);
                Mockito.doThrow(SQLException.class).when(mockConnection).commit();
                try {
                    // Set changeDateModified false
                    SystemMetadataManager.lock(guid);
                    DocumentImpl.archive(accnum, guid, user, false,
                                         SystemMetadataManager.SysMetaVersion.CHECKED);
                    fail("Test shouldn't get there since the above method should throw an exception");
                } catch (Exception e) {
                    SystemMetadataManager.unLock(guid);
                    assertTrue("It should be a ServiceFailure exception.", e instanceof ServiceFailure);
                }
            }
            //Nothing should change since the rollback
            assertTrue("The identifier table should have value",
                       IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                      " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                       IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                      " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                       IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                      " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                       " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                       " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                        IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                       " guid like ?", guid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                        IntegrationTestUtils.hasRecord("xml_documents",
                                                       dbConn, " docid like ?", docid));
            assertFalse("The xml_revisions table should have value",
                       IntegrationTestUtils.hasRecord("xml_revisions",
                                                      dbConn, " docid like ?", docid));
            input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
            SystemMetadata sysRead = SystemMetadataManager.getInstance().get(guid);
            assertFalse("System metadata should have archived true", sysRead.getArchived());
            Date currentDateModified = sysRead.getDateSysMetadataModified();
            assertEquals("The current dateModified should equal the original one.",
                         originalDateModified.getTime(), currentDateModified.getTime());
            assertFalse(originSys.getArchived());
            assertEquals(originalDateModified.getTime(), sysRead.getDateSysMetadataModified().getTime());
            assertEquals(originalDateUploaded.getTime(), sysRead.getDateUploaded().getTime());
            assertEquals(version.intValue(), sysRead.getSerialVersion().intValue());
            MCSystemMetadataTest.compareValues(originSys, sysRead);
            // Make sure there are no changes on the system metadata of pid from hashstore
            InputStream metaInput = D1NodeServiceTest.getStorage().retrieveMetadata(guid);
            SystemMetadata sysmetaFromHash =
                TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, metaInput);
            assertNull(sysmetaFromHash.getObsoletedBy());
            assertFalse(sysmetaFromHash.getArchived());
            assertEquals(originalDateUploaded.getTime(),
                         sysmetaFromHash.getDateUploaded().getTime());
            assertEquals(originalDateModified.getTime(),
                         sysmetaFromHash.getDateSysMetadataModified().getTime());
            assertEquals(version.longValue(), sysmetaFromHash.getSerialVersion().longValue());
            MCSystemMetadataTest.compareValues(originSys, sysmetaFromHash);
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Test the archive method with the parameters of SysMetaVersion.CHECKED/UNCHECKED
     * @throws Exception
     */
    @Test
    public void testArchiveWithCheckSysMetaVersion() throws Exception {
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("DocumentImpl_archiveData." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeTest.mnCreate(session, guid, object, sysmeta);
        String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
        try (MockedStatic<SystemMetadataManager> mock =
                             Mockito.mockStatic(SystemMetadataManager.class, CALLS_REAL_METHODS)) {
            SystemMetadataManager mockManager = Mockito.mock(SystemMetadataManager.class,
                                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            // Set the new modification date so we can mock the checking or without checking
            Date original = new Date();
            SystemMetadata newSysmeta = SerializationUtils.clone(sysmeta);
            newSysmeta.setDateSysMetadataModified(original);
            // The first, second and third call return different results
            Mockito.when(mockManager.get(guid)).thenReturn(sysmeta).thenReturn(sysmeta)
                .thenReturn(newSysmeta);
            Mockito.when(SystemMetadataManager.getInstance()).thenReturn(mockManager);
            mock.when(() -> SystemMetadataManager.storeRollBack(any(Identifier.class),
                                                                any(Exception.class),
                                                                any(DBConnection.class),
                                                                any(SystemMetadata.class)))
                .thenReturn("hello");
            // Archive with checking should fail
            try {
                // False means not to change the dateModified field
                SystemMetadataManager.lock(guid);
                DocumentImpl.archive(accnum, guid, "test", false,
                                    SystemMetadataManager.SysMetaVersion.CHECKED);
                fail("Test cannot get there since the dataOfModified was change during archive");
            } catch (Exception e) {
                SystemMetadataManager.unLock(guid);
                assertTrue( e instanceof ServiceFailure);
            }
            // Using UNCHECKED, archive should succeed.
            // False means not to change the dateModified field
            SystemMetadataManager.lock(guid);
            DocumentImpl.archive(accnum, guid, "test", false,
                                SystemMetadataManager.SysMetaVersion.UNCHECKED);
            SystemMetadataManager.unLock(guid);
        }

    }

    /**
     * Test the archive method
     * @throws Exception
     */
    @Test
    public void testArchiveMetadata() throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
             // Get a database connection from the pool
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
            dbConn = DBConnectionPool.getDBConnection("SystemMetadataManager.deleteData");
            serialNumber = dbConn.getCheckOutSerialNumber();
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("DocumentImpl_archiveMetadata." + System.currentTimeMillis());
            InputStream object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
            SystemMetadata sysmeta = D1NodeServiceTest
                                          .createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            object.close();
            object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            object.close();
            //check record
            assertTrue("The identifier table should have value",
                                  IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                  " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                             IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                       IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                 " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                                       IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                       " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                        " docid like ?", docid));
            InputStream input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
            SystemMetadata sys = SystemMetadataManager.getInstance().get(guid);
            assertFalse("System metadata should have archived false", sys.getArchived());

            //update
            Identifier newPid = new Identifier();
            newPid.setValue("DocumentImpl_archiveMetaData." + (System.currentTimeMillis() + 1));
            object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
            sysmeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
            object.close();
            sysmeta.setFormatId(formatId);
            ReplicationPolicy policy = new ReplicationPolicy();
            NodeReference node = new NodeReference();
            node.setValue("test_node");
            policy.addPreferredMemberNode(node);
            policy.setReplicationAllowed(true);
            policy.setNumberReplicas(2);
            sysmeta.setReplicationPolicy(policy);
            Replica replica = new Replica();
            replica.setReplicaMemberNode(node);
            replica.setReplicationStatus(ReplicationStatus.COMPLETED);
            replica.setReplicaVerified(new Date());
            sysmeta.addReplica(replica);
            MediaTypeProperty property = new MediaTypeProperty();
            property.setName("test_media_name");
            property.setValue("test_media_value");
            MediaType type = new MediaType();
            type.addProperty(property);
            type.setName("test_name");
            sysmeta.setMediaType(type);
            object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
            d1NodeTest.mnUpdate(session, guid, object, newPid, sysmeta);
            object.close();
            //check record
            assertTrue("The identifier table should have value",
                                 IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                             " guid like ?", newPid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                            " guid like ?", newPid.getValue()));
            assertTrue("The xml_access table should have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                             " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                       IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationstatus table should have value",
                     IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smmediatypeproperties table should have value",
                     IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                               " guid like ?", newPid.getValue()));
            String accnum2 = IdentifierManager.getInstance().getLocalId(newPid.getValue());
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertTrue("The xml_documents table should have value",
                           IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                           " rev=? and docid like ?", 2, docid));
            assertFalse("The xml_documents table should not have value",
                           IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                           " rev=? and docid like ?", 1, docid));
            assertFalse("The xml_revisions table should not have value",
                    IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                            " rev=? and docid like ?", 2, docid));
            assertTrue("The xml_revisions table should have value",
                           IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                           " rev=? and docid like ?", 1, docid));
            input = MetacatHandler.read(accnum2, null);
            assertNotNull("The file should exist", input);
            input.close();
            sys = SystemMetadataManager.getInstance().get(guid);
            Date originalDateModified = sys.getDateSysMetadataModified();
            assertFalse("System metadata should have archived false", sys.getArchived());
            sys = SystemMetadataManager.getInstance().get(newPid);
            assertFalse("System metadata should have archived false", sys.getArchived());

            //Archive
            String user = "test";
            // Set changeDateModified true
            SystemMetadataManager.lock(guid);
            DocumentImpl.archive(accnum, guid, user, true,
                                SystemMetadataManager.SysMetaVersion.UNCHECKED);
            SystemMetadataManager.unLock(guid);
            assertTrue("The identifier table should have value",
                                IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                  " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                      IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                  " guid like ?", guid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            // the version 2 still exists
            assertTrue("The xml_documents table should have value",
                           IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                              " rev=? and docid like ?", 2, docid));
            assertFalse("The xml_documents table should not have value",
                          IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                              " rev=? and docid like ?", 1, docid));
            assertTrue("The xml_revisions table should have value",
                           IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                             " rev=? and docid like ?", 1, docid));
            assertFalse("The xml_revisions table should not have value",
                           IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                             " rev=? and docid like ?", 2, docid));
            input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
            sys = SystemMetadataManager.getInstance().get(guid);
            assertTrue("System metadata should have archived true", sys.getArchived());
            Date currentDateModified = sys.getDateSysMetadataModified();
            assertTrue("The current dateModified should be greater than the original one.",
                                    currentDateModified.getTime() > originalDateModified.getTime());
            sys = SystemMetadataManager.getInstance().get(newPid);
            assertFalse("System metadata should have archived false", sys.getArchived());

            // Set changeDateModified true
            SystemMetadataManager.lock(newPid);
            DocumentImpl.archive(accnum2, newPid, user, true,
                                SystemMetadataManager.SysMetaVersion.CHECKED);
            SystemMetadataManager.unLock(newPid);
            //check record
            assertTrue("The identifier table should have value",
                                IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The systemmetadata table should have value",
                           IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The xml_access table should have value",
                               IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                      IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationstatus table should have value",
                      IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", newPid.getValue()));
            assertTrue("The smmediatypeproperties table should have value",
                    IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                " guid like ?", newPid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertFalse("The xml_documents table should not have value",
                           IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                           " rev=? and docid like ?", 2, docid));
            assertFalse("The xml_documents table should not have value",
                           IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                           " rev=? and docid like ?", 1, docid));
            assertTrue("The xml_revisions table should have value",
                           IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                              " rev=? and docid like ?", 1, docid));
            assertTrue("The xml_revisions table should have value",
                           IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                              " rev=? and docid like ?", 2, docid));
            input = MetacatHandler.read(accnum2, null);
            assertNotNull("The file should exist", input);
            input.close();
            sys = SystemMetadataManager.getInstance().get(guid);
            assertTrue("System metadata should have archived true", sys.getArchived());
            sys = SystemMetadataManager.getInstance().get(newPid);
            assertTrue("System metadata should have archived true", sys.getArchived());
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Test the delete method
     * @throws Exception
     */
    @Test
    public void testFailedArchive() throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
             // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("SystemMetadataManager.testFailedArchive");
            serialNumber = dbConn.getCheckOutSerialNumber();
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("DocumentImpl_failedArchiveData." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest
                                          .createSystemMetadata(guid, session.getSubject(), object);
            ReplicationPolicy policy = new ReplicationPolicy();
            NodeReference node = new NodeReference();
            node.setValue("test_node");
            policy.addPreferredMemberNode(node);
            policy.setReplicationAllowed(true);
            policy.setNumberReplicas(2);
            sysmeta.setReplicationPolicy(policy);
            Replica replica = new Replica();
            replica.setReplicaMemberNode(node);
            replica.setReplicationStatus(ReplicationStatus.COMPLETED);
            replica.setReplicaVerified(new Date());
            sysmeta.addReplica(replica);
            MediaTypeProperty property = new MediaTypeProperty();
            property.setName("test_media_name");
            property.setValue("test_media_value");
            MediaType type = new MediaType();
            type.addProperty(property);
            type.setName("test_name");
            sysmeta.setMediaType(type);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            //check record
            assertTrue("The identifier table should have value",
                                  IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                  " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                             IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                 IntegrationTestUtils.hasRecord("xml_access", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The smreplicationstatus table should not have value",
                       IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                               " guid like ?", guid.getValue()));
            assertTrue("The smmediatypeproperties table should not have value",
                       IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                 " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                                       IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                           " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                        " docid like ?", docid));
            InputStream input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
            SystemMetadata sys = SystemMetadataManager.getInstance().get(guid);
            assertFalse("System metadata should have archived false", sys.getArchived());

            //Mock a failed archiving
            try (MockedStatic<DBConnectionPool> mockDbConnPool =
                     Mockito.mockStatic(DBConnectionPool.class)) {
                DBConnection mockConnection = Mockito.mock(DBConnection.class,
                                                           withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                Mockito.when(DBConnectionPool.getDBConnection(any(String.class)))
                    .thenReturn(mockConnection);
                Mockito.doThrow(SQLException.class).when(mockConnection).commit();
                try {
                    String user = "test";
                    // Set changeDateModified true
                    SystemMetadataManager.lock(guid);
                    DocumentImpl.archive(accnum, guid, user, true,
                                         SystemMetadataManager.SysMetaVersion.CHECKED);
                    fail("The test can't be here since archive should throw an exception");
                } catch (Exception e) {
                    SystemMetadataManager.unLock(guid);
                    assertTrue("The exception class should be ServiceFailure",
                               e instanceof ServiceFailure);
                }
            }
            //Records in the db shouldn't change
            assertTrue("The identifier table should have value",
                                IntegrationTestUtils.hasRecord("identifier", dbConn,
                                                                  " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            IntegrationTestUtils.hasRecord("systemmetadata", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                   IntegrationTestUtils.hasRecord("xml_access", dbConn, " guid like ?",
                                                  guid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                        IntegrationTestUtils.hasRecord("smreplicationpolicy", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The smreplicationstatus table should not have value",
                        IntegrationTestUtils.hasRecord("smreplicationstatus", dbConn,
                                                                " guid like ?", guid.getValue()));
            assertTrue("The smmediatypeproperties table should not have value",
                      IntegrationTestUtils.hasRecord("smmediatypeproperties", dbConn,
                                                                 " guid like ?", guid.getValue()));
            assertTrue("The xml_documents table should have value",
                                        IntegrationTestUtils.hasRecord("xml_documents", dbConn,
                                                                        " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                       IntegrationTestUtils.hasRecord("xml_revisions", dbConn,
                                                                       " docid like ?", docid));
            input = MetacatHandler.read(accnum, null);
            assertNotNull("The file should exist", input);
            input.close();
            sys = SystemMetadataManager.getInstance().get(guid);
            assertFalse("System metadata should have archived false", sys.getArchived());
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

}
