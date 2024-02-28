package edu.ucsb.nceas.metacat;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

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
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeReplicationTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


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
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            //check record
            assertTrue("The identifier table should have value",
                                  hasRecord("identifier", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                             hasRecord("systemmetadata", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                 hasRecord("xml_access", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        hasRecord("smreplicationpolicy", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       hasRecord("smreplicationstatus", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                       hasRecord("smmediatypeproperties", dbConn, " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                                       hasRecord("xml_documents", dbConn, " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        hasRecord("xml_revisions", dbConn, " docid like ?", docid));
            InputStream input = MetacatHandler.read(accnum);
            assertNotNull("The file should exist", input);
            input.close();

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
            sysmeta.setMediaType(type);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            MNodeService.getInstance(request).update(session, guid, object, newPid, sysmeta);
            //check record
            assertTrue("The identifier table should have value",
                                 hasRecord("identifier", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            hasRecord("systemmetadata", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The xml_access table should have value",
                                 hasRecord("xml_access", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                       hasRecord("smreplicationpolicy", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationstatus table should have value",
                     hasRecord("smreplicationstatus", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The smmediatypeproperties table should have value",
                     hasRecord("smmediatypeproperties", dbConn, " guid like ?", newPid.getValue()));
            String accnum2 = IdentifierManager.getInstance().getLocalId(newPid.getValue());
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertTrue("The xml_documents table should have value",
                                       hasRecord("xml_documents", dbConn, " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                           hasRecord("xml_revisions", dbConn, " docid like ? and rev=?", docid, 1));
            input = MetacatHandler.read(accnum2);
            assertNotNull("The file should exist", input);
            input.close();

            //Delete
            DocumentImpl.delete(accnum, guid);
            assertTrue("The identifier table should have value",
                                hasRecord("identifier", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The systemmetadata table should not have value",
                            hasRecord("systemmetadata", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The xml_access table should not have value",
                                hasRecord("xml_access", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        hasRecord("smreplicationpolicy", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       hasRecord("smreplicationstatus", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                      hasRecord("smmediatypeproperties", dbConn, " guid like ?", guid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertFalse("The xml_documents table should not have value",
                                        hasRecord("xml_documents", dbConn, " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        hasRecord("xml_revisions", dbConn, " docid like ?", docid));
            try {
                input = MetacatHandler.read(accnum);
                fail("The test can not get here since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof McdbException);
            }

            DocumentImpl.delete(accnum2, newPid);
            //check record
            assertTrue("The identifier table should have value",
                                 hasRecord("identifier", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The systemmetadata table should not have value",
                            hasRecord("systemmetadata", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The xml_access table should not have value",
                                 hasRecord("xml_access", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                       hasRecord("smreplicationpolicy", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                     hasRecord("smreplicationstatus", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                     hasRecord("smmediatypeproperties", dbConn, " guid like ?", newPid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertFalse("The xml_documents table should have value",
                                       hasRecord("xml_documents", dbConn, " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                           hasRecord("xml_revisions", dbConn, " docid like ? and rev=?", docid, 1));
            try {
                input = MetacatHandler.read(accnum2);
                fail("The test can not get here since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof McdbException);
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
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            object.close();
            //check record
            assertTrue("The identifier table should have value",
                                  hasRecord("identifier", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                             hasRecord("systemmetadata", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                 hasRecord("xml_access", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        hasRecord("smreplicationpolicy", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       hasRecord("smreplicationstatus", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                       hasRecord("smmediatypeproperties", dbConn, " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                                       hasRecord("xml_documents", dbConn, " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        hasRecord("xml_revisions", dbConn, " docid like ?", docid));
            InputStream input = MetacatHandler.read(accnum);
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
            sysmeta.setMediaType(type);
            object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
            MNodeService.getInstance(request).update(session, guid, object, newPid, sysmeta);
            object.close();
            //check record
            assertTrue("The identifier table should have value",
                                 hasRecord("identifier", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            hasRecord("systemmetadata", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The xml_access table should have value",
                                 hasRecord("xml_access", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                       hasRecord("smreplicationpolicy", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The smreplicationstatus table should have value",
                     hasRecord("smreplicationstatus", dbConn, " guid like ?", newPid.getValue()));
            assertTrue("The smmediatypeproperties table should have value",
                     hasRecord("smmediatypeproperties", dbConn, " guid like ?", newPid.getValue()));
            String accnum2 = IdentifierManager.getInstance().getLocalId(newPid.getValue());
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertTrue("The xml_documents table should have value",
                           hasRecord("xml_documents", dbConn, " docid like ? and rev=?", docid, 2));
            assertTrue("The xml_revisions table should have value",
                           hasRecord("xml_revisions", dbConn, " docid like ? and rev=?", docid, 1));
            input = MetacatHandler.read(accnum2);
            assertNotNull("The file should exist", input);
            input.close();

            //Delete
            DocumentImpl.delete(accnum, guid);
            assertTrue("The identifier table should have value",
                                hasRecord("identifier", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The systemmetadata table should not have value",
                            hasRecord("systemmetadata", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The xml_access table should not have value",
                                hasRecord("xml_access", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                        hasRecord("smreplicationpolicy", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                       hasRecord("smreplicationstatus", dbConn, " guid like ?", guid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                      hasRecord("smmediatypeproperties", dbConn, " guid like ?", guid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            // the version 2 still exists
            assertTrue("The xml_documents table should have value",
                            hasRecord("xml_documents", dbConn, " docid like ? and rev=?", docid, 2));
            assertFalse("The xml_revisions table should not have value",
                                        hasRecord("xml_revisions", dbConn, " docid like ?", docid));
            try {
                input = MetacatHandler.read(accnum);
                fail("The test can not get here since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof McdbException);
            }

            DocumentImpl.delete(accnum2, newPid);
            //check record
            assertTrue("The identifier table should have value",
                                 hasRecord("identifier", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The systemmetadata table should not have value",
                            hasRecord("systemmetadata", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The xml_access table should not have value",
                                 hasRecord("xml_access", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The smreplicationpolicy table should not have value",
                       hasRecord("smreplicationpolicy", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The smreplicationstatus table should not have value",
                     hasRecord("smreplicationstatus", dbConn, " guid like ?", newPid.getValue()));
            assertFalse("The smmediatypeproperties table should not have value",
                     hasRecord("smmediatypeproperties", dbConn, " guid like ?", newPid.getValue()));
            docid = DocumentUtil.getDocIdFromAccessionNumber(accnum2);
            assertFalse("The xml_documents table should have value",
                           hasRecord("xml_documents", dbConn, " docid like ? and rev=?", docid, 2));
            assertFalse("The xml_revisions table should not have value",
                           hasRecord("xml_revisions", dbConn, " docid like ? and rev=?", docid, 1));
            try {
                input = MetacatHandler.read(accnum2);
                fail("The test can not get here since it should throw an exception");
            } catch (Exception e) {
                assertTrue(e instanceof McdbException);
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
            sysmeta.setMediaType(type);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            //check record
            assertTrue("The identifier table should have value",
                                  hasRecord("identifier", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                             hasRecord("systemmetadata", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                                 hasRecord("xml_access", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                        hasRecord("smreplicationpolicy", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The smreplicationstatus table should not have value",
                       hasRecord("smreplicationstatus", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The smmediatypeproperties table should not have value",
                       hasRecord("smmediatypeproperties", dbConn, " guid like ?", guid.getValue()));
            String accnum = IdentifierManager.getInstance().getLocalId(guid.getValue());
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            assertTrue("The xml_documents table should have value",
                                       hasRecord("xml_documents", dbConn, " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                        hasRecord("xml_revisions", dbConn, " docid like ?", docid));
            InputStream input = MetacatHandler.read(accnum);
            assertNotNull("The file should exist", input);
            input.close();

            //Mock a failed deleting
            try (MockedStatic<PropertyService> mock =
                                            Mockito.mockStatic(PropertyService.class)) {
                Mockito.when(PropertyService.getProperty("application.datafilepath"))
                                                        .thenThrow(PropertyNotFoundException.class);
                try {
                    DocumentImpl.delete(accnum, guid);
                    fail("The test can't be here since the failed delete should throw an exception");
                } catch (Exception e) {
                    assertTrue("The exception class should be ServiceFailure",
                                                                      e instanceof ServiceFailure);
                }
            }
            //Records in the db shouldn't change
            assertTrue("The identifier table should have value",
                                hasRecord("identifier", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The systemmetadata table should have value",
                            hasRecord("systemmetadata", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The xml_access table should have value",
                   hasRecord("xml_access", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The smreplicationpolicy table should have value",
                        hasRecord("smreplicationpolicy", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The smreplicationstatus table should not have value",
                        hasRecord("smreplicationstatus", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The smmediatypeproperties table should not have value",
                      hasRecord("smmediatypeproperties", dbConn, " guid like ?", guid.getValue()));
            assertTrue("The xml_documents table should have value",
                                        hasRecord("xml_documents", dbConn, " docid like ?", docid));
            assertFalse("The xml_revisions table should not have value",
                                       hasRecord("xml_revisions", dbConn, " docid like ?", docid));
            input = MetacatHandler.read(accnum);
            assertNotNull("The file should exist", input);
            input.close();
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    private boolean hasRecord(String table, DBConnection dbconn, String whereClause,
                                                                String value) throws Exception {
        boolean hasRecord = false;
        String query = "SELECT * FROM " + table + " WHERE " + whereClause;
        try (PreparedStatement statement = dbconn.prepareStatement(query)) {
             statement.setString(1, value);
             try (ResultSet rs = statement.executeQuery()) {
                 if (rs.next()) {
                     hasRecord = true;
                 }
             }
        }
        return hasRecord;
    }

    private boolean hasRecord(String table, DBConnection dbconn, String whereClause,
                                                         String value, int rev) throws Exception {
        boolean hasRecord = false;
        String query = "SELECT * FROM " + table + " WHERE " + whereClause;
        try (PreparedStatement statement = dbconn.prepareStatement(query)) {
             statement.setString(1, value);
             statement.setInt(2, rev);
             try (ResultSet rs = statement.executeQuery()) {
                 if (rs.next()) {
                     hasRecord = true;
                 }
             }
        }
        return hasRecord;
    }

}
