package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.IntegrationTestUtils;
import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.AccessionNumber;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.systemmetadata.ChecksumsManager;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;

/**
 * @author Tao
 * The test class for HashStoreUpgrader
 */
public class HashStoreUpgraderIT {
    private static final Log log = LogFactory.getLog(HashStoreUpgraderIT.class);
    private Properties withProperties;
    MockedStatic<PropertyService> closeableMock;
    String backupPath = "build/temp." + System.currentTimeMillis();
    String hashStorePath = "build/hashStore";
    String documentPath = "test";
    String dataPath = "test/resources";
    ObjectFormatIdentifier eml220_format_id = new ObjectFormatIdentifier();


    @Before
    public void setUp() throws Exception {
        eml220_format_id.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        withProperties = new Properties();
        withProperties.setProperty("application.datafilepath", dataPath);
        withProperties.setProperty("application.documentfilepath", documentPath);
        withProperties.setProperty("application.backupDir", backupPath);
        withProperties.setProperty("storage.hashstore.rootDirectory", hashStorePath);
        withProperties.setProperty("storage.hashstore.converterArrayLength", "2");
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
        MetacatInitializer.initStorage();
    }

    @After
    public void tearDown() throws Exception {
        if (!closeableMock.isClosed()) {
            closeableMock.close();
        }
        File backupDir = new File(backupPath);
        backupDir.delete();
    }

    /**
     * Test the method initCandidateList. It also contains the cases that objects exist in the
     * xml_documents/revisions table and identifier table, but doesn't exist in the
     * systemmetadata table.
     * @throws Exception
     */
    @Test
    public void testInitCandidateList() throws Exception {
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        // The object exists in the systemmetadata table
        Identifier pid = new Identifier();
        pid.setValue("testInitCandidateList-" + System.currentTimeMillis());
        InputStream object =
            new ByteArrayInputStream("testInitCandidateList".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        SystemMetadataManager.lock(pid);
        SystemMetadataManager.getInstance().store(sysmeta);
        SystemMetadataManager.unLock(pid);

        //Incomplete records (not in the systemmetadata table, but in the identifier table and
        // xml_documents/revisions tables)
        String prefix = "testInitCandidateList12" + "." + System.currentTimeMillis();
        String id1 = prefix + "." + 1;
        String id2 = prefix + "." + 2;
        Identifier pid1 = new Identifier();
        pid1.setValue("pid-" + id1);
        Identifier pid2 = new Identifier();
        pid2.setValue("pid-" + id2);
        String docName = "eml";
        String docType = "eml://ecoinformatics.org/eml-2.0.1";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection(
                "HashStoreUpgraderTest.testInitCandidateListWithNonDataONEObject");
            serialNumber = dbConn.getCheckOutSerialNumber();
            // register two docids in the xml_documents and xml_revisions tables.
            DocumentImpl.registerDocument(docName, docType, dbConn, id1, user);
            DocumentImpl.registerDocument(docName, docType, dbConn, id2, user);
            assertTrue("The xml_documents table should have the record " + id2,
                       IntegrationTestUtils.hasRecord("xml_documents", dbConn, " rev=? and docid like ?"
                           , 2, prefix));
            assertTrue("The xml_revisions table should have the record " + id1,
                       IntegrationTestUtils.hasRecord("xml_revisions", dbConn, " rev=? and docid like ?"
                           , 1, prefix));
            // Register the pid and docid into the identifier table
            IdentifierManager.getInstance().createMapping(pid1.getValue(), id1);
            IdentifierManager.getInstance().createMapping(pid2.getValue(), id2);
            assertEquals(id1, IdentifierManager.getInstance().getLocalId(pid1.getValue()));
            assertEquals(id2, IdentifierManager.getInstance().getLocalId(pid2.getValue()));
            // But the pid1 and pid2 are not in the systemmetadata table
            assertNull(SystemMetadataManager.getInstance().get(pid1));
            assertNull(SystemMetadataManager.getInstance().get(pid2));
        } finally {
            DBConnectionPool.returnDBConnection(
                dbConn, serialNumber);
        }
        boolean found = false;
        boolean found1 = false;
        boolean found2 = false;
        HashStoreUpgrader upgrader = new HashStoreUpgrader();
        ResultSet resultSet = upgrader.initCandidateList();
        while (resultSet.next()) {
            String id = resultSet.getString(1);
            if (id.equals(pid.getValue())) {
                found = true;
            }
            if (id.equals(pid1.getValue())) {
                found1 = true;
            }
            if (id.equals(pid2.getValue())) {
                found2 = true;
            }
            if (found && found1 && found2) {
                break;
            }
        }
        assertTrue(found && found1 && found2);
    }

    /**
     * Test the method initCandidateList which will omit the items which have records in the
     * checksum table. Also, it tests the deleted object - only has the record in the identifier
     * table, not in either the systemmetadata or xml_documents tables.
     * @throws Exception
     */
    @Test
    public void testInitCandidateListNotFound() throws Exception {
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("testInitCandidateList-" + System.currentTimeMillis());
        InputStream object =
            new ByteArrayInputStream("testInitCandidateList".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        SystemMetadataManager.lock(pid);
        SystemMetadataManager.getInstance().store(sysmeta);
        SystemMetadataManager.unLock(pid);
        // save it to the checksum table
        Map<String, String> checksums = new HashMap<>();
        checksums.put(sysmeta.getChecksum().getAlgorithm(), sysmeta.getChecksum().getValue());
        ChecksumsManager checksumsManager = new ChecksumsManager();
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection(
                "HashStoreUpgrader.upgrade");
            serialNumber = dbConn.getCheckOutSerialNumber();
            checksumsManager.save(pid, checksums, dbConn);
        } finally {
            DBConnectionPool.returnDBConnection(
                dbConn, serialNumber);
        }
        //Simulate a deleted object which is only in the identifier table
        String deletedDocId = "jing1023124b." + System.currentTimeMillis() + ".1";
        String deletedPid = "pid-" + deletedDocId;
        IdentifierManager.getInstance().createMapping(deletedPid, deletedDocId);
        assertEquals(deletedDocId, IdentifierManager.getInstance().getLocalId(deletedPid));
        Identifier identifier = new Identifier();
        identifier.setValue(deletedPid);
        assertNull(SystemMetadataManager.getInstance().get(identifier));

        HashStoreUpgrader upgrader = new HashStoreUpgrader();
        ResultSet resultSet = upgrader.initCandidateList();
        boolean found = false;
        while (resultSet.next()) {
            String id = resultSet.getString(1);
            if (id.equals(pid.getValue()) || id.equals(deletedPid)) {
                found = true;
                break;
            }
        }
        assertFalse(found);
    }

    /**
     * Test the method initCandidateList which will capture the non-dataone objects - no records
     * in the identifier table, no system metadata.
     * @throws Exception
     */
    @Test
    public void testInitCandidateListWithNonDataONEObject() throws Exception {
        String prefix = "testInitCandidateListWithNonDataONEObject" + "."
                        + System.currentTimeMillis();
        System.out.print("the prefix is " + prefix);
        String id1 = prefix + "." + 1;
        String id2 = prefix + "." + 2;
        String user = "http://orcid.org/1234/4567";
        String docName = "eml";
        String docType = "eml://ecoinformatics.org/eml-2.0.1";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection(
                "HashStoreUpgraderTest.testInitCandidateListWithNonDataONEObject");
            serialNumber = dbConn.getCheckOutSerialNumber();
            // register two docids in the xml_documents and xml_revisions tables.
            // But there are no system metadata and identifier records for them
            DocumentImpl.registerDocument(docName, docType, dbConn, id1, user);
            DocumentImpl.registerDocument(docName, docType, dbConn, id2, user);
            assertTrue("The xml_documents table should have the record " + id2,
                IntegrationTestUtils.hasRecord("xml_documents", dbConn, " rev=? and docid like ?"
                    , 2, prefix));
            assertTrue("The xml_revisions table should have the record " + id1,
                IntegrationTestUtils.hasRecord("xml_revisions", dbConn, " rev=? and docid like ?"
                    , 1, prefix));
            try {
                IdentifierManager.getInstance().getGUID(prefix, 1);
                fail("Test shouldn't reach here since " + id1 + " shouldn't exist in the "
                         + "identifer table");
            } catch (Exception e) {
                assertTrue(e instanceof McdbDocNotFoundException);
            }
            try {
                IdentifierManager.getInstance().getGUID(prefix, 2);
                fail("Test shouldn't reach here since " + id1 + " shouldn't exist in the "
                         + "identifer table");
            } catch (Exception e) {
                assertTrue(e instanceof McdbDocNotFoundException);
            }
            Identifier pid = new Identifier();
            pid.setValue(id1);
            SystemMetadata systemMetadata = SystemMetadataManager.getInstance().get(pid);
            assertNull(systemMetadata);
            pid.setValue(id2);
            systemMetadata = SystemMetadataManager.getInstance().get(pid);
            assertNull(systemMetadata);
        } finally {
            DBConnectionPool.returnDBConnection(
                dbConn, serialNumber);
        }
        HashStoreUpgrader upgrader = new HashStoreUpgrader();
        ResultSet resultSet = upgrader.initCandidateList();
        boolean found1 = false;
        boolean found2 = false;
        while (resultSet.next()) {
            String id = resultSet.getString(1);
            if (id.equals(id1)) {
                found1 = true;
            }
            if (id.equals(id2)) {
                found2 = true;
            }
            if (found2 && found1) {
                break;
            }
        }
        assertTrue("The id " + id2 + " should be found in the result set", found2);
        assertTrue("The id " + id1 + " should be found in the result set", found1);
    }

    /**
     * Test the resolve method in an CN
     * @throws Exception
     */
    @Test
    public void testResolveInCN() throws Exception {
        closeableMock.close();
        withProperties.setProperty("dataone.nodeType", "cn");
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
        String localDataId = "eml-error.xml";
        String localMetadataId = "eml-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        // There is no record in the identifier table
        String id = "testResolveInMN" + System.currentTimeMillis();
        Identifier pid = new Identifier();
        InputStream object =
            new ByteArrayInputStream(id.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        HashStoreUpgrader upgrader = new HashStoreUpgrader();
        pid.setValue(id);
        Path path = upgrader.resolve(sysmeta);
        assertNull(path);
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // The system metadata is a data object, but the path is in documents directory. It
            // should work as well
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(pid.getValue())).thenReturn(localMetadataId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            path = upgrader.resolve(sysmeta);
            assertTrue(Files.exists(path));
            assertEquals(8724, Files.size(path));
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // The system metadata is a data object, the path is in data directory. It
            // should work
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(pid.getValue())).thenReturn(localDataId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            path = upgrader.resolve(sysmeta);
            assertTrue(Files.exists(path));
            assertEquals(103649, Files.size(path));
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // Mock the identifier table returning a non-existing id
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(pid.getValue())).thenReturn("foo");
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            try {
                path = upgrader.resolve(sysmeta);
                fail("The test shouldn't get there since the docid doesn't exist");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
        }

        // For the metadata system metadata
        String metadataId = "testResolveMetadataInMN" + System.currentTimeMillis();
        Identifier metadataPid = new Identifier();
        metadataPid.setValue(metadataId);
        object =
            new ByteArrayInputStream(metadataId.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta2 =
            D1NodeServiceTest.createSystemMetadata(metadataPid, owner, object);
        sysmeta2.setFormatId(eml220_format_id);
        try {
            path = upgrader.resolve(sysmeta2);
        } catch (Exception e) {
            assertTrue(e instanceof McdbDocNotFoundException);
            assertTrue(e.getMessage().contains("identifier"));
            assertTrue(e.getMessage().contains(metadataId));
        }

        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // The system metadata is a metadata object, and the path is in documents directory. It
            // should work.
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataPid.getValue())).thenReturn(localMetadataId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            path = upgrader.resolve(sysmeta2);
            assertTrue(Files.exists(path));
            assertEquals(8724, Files.size(path));
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // The system metadata is for a metadata object, but the path is in data directory. It
            // should work as well.
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataPid.getValue())).thenReturn(localDataId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            path = upgrader.resolve(sysmeta2);
            assertTrue(Files.exists(path));
            assertEquals(103649, Files.size(path));
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // Mock to return a non-existing docid
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataPid.getValue())).thenReturn("food");
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            try {
                path = upgrader.resolve(sysmeta2);
                fail("The test shouldn't get there since the docid doesn't exist");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
        }
    }

    /**
     * Test the resolve method in an MN
     * @throws Exception
     */
    @Test
    public void testResolveInMN() throws Exception {
        String localDataId = "eml-error.xml";
        String localMetadataId = "eml-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        // There is no record in the identifier table
        String id = "testResolveInMN" + System.currentTimeMillis();
        Identifier pid = new Identifier();
        InputStream object =
            new ByteArrayInputStream(id.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        HashStoreUpgrader upgrader = new HashStoreUpgrader();
        pid.setValue(id);
        try {
            Path path = upgrader.resolve(sysmeta);
            fail("Test can't get here since there is no map in the identifier table.");
        } catch (Exception e) {
            assertTrue(e instanceof McdbDocNotFoundException);
            assertTrue(e.getMessage().contains("identifier"));
            assertTrue(e.getMessage().contains(id));
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // The system metadata is a data object, but the path is in documents directory. It
            // should work as well
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(pid.getValue())).thenReturn(localMetadataId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            Path path = upgrader.resolve(sysmeta);
            assertTrue(Files.exists(path));
            assertEquals(8724, Files.size(path));
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // The system metadata is a data object, the path is in data directory. It
            // should work
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(pid.getValue())).thenReturn(localDataId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            Path path = upgrader.resolve(sysmeta);
            assertTrue(Files.exists(path));
            assertEquals(103649, Files.size(path));
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // Mock the identifier table returning a non-existing id
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(pid.getValue())).thenReturn("foo");
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            try {
                Path path = upgrader.resolve(sysmeta);
                fail("The test shouldn't get there since the docid doesn't exist");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
        }

        // For the metadata system metadata
        String metadataId = "testResolveMetadataInMN" + System.currentTimeMillis();
        Identifier metadataPid = new Identifier();
        metadataPid.setValue(metadataId);
        object =
            new ByteArrayInputStream(metadataId.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta2 =
            D1NodeServiceTest.createSystemMetadata(metadataPid, owner, object);
        sysmeta2.setFormatId(eml220_format_id);
        try {
            Path path = upgrader.resolve(sysmeta2);
            fail("Test can't get here since there is no map in the identifier table.");
        } catch (Exception e) {
            assertTrue(e instanceof McdbDocNotFoundException);
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // The system metadata is a metadata object, and the path is in documents directory. It
            // should work.
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataPid.getValue())).thenReturn(localMetadataId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            Path path = upgrader.resolve(sysmeta2);
            assertTrue(Files.exists(path));
            assertEquals(8724, Files.size(path));
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // The system metadata is for a metadata object, but the path is in data directory. It
            // should work as well.
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataPid.getValue())).thenReturn(localDataId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            Path path = upgrader.resolve(sysmeta2);
            assertTrue(Files.exists(path));
            assertEquals(103649, Files.size(path));
        }
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            // Mock to return a non-existing docid
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataPid.getValue())).thenReturn("food");
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            try {
                Path path = upgrader.resolve(sysmeta2);
                fail("The test shouldn't get there since the docid doesn't exist");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
        }
    }

    /**
     * Test the resolve method for the metadata object
     * @throws Exception
     */
    @Test
    public void testResolveMetadata() throws Exception {
        String metadataId = "testResolveMetadata" + System.currentTimeMillis();
        String localId = "eml-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(metadataId);
        InputStream object =
            new ByteArrayInputStream("testInitCandidateList".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        sysmeta.setFormatId(eml220_format_id);
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataId)).thenReturn(localId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            HashStoreUpgrader upgrader = new HashStoreUpgrader();
            Path path = upgrader.resolve(sysmeta);
            assertTrue(Files.exists(path));
            assertEquals(8724, Files.size(path));
        }
    }

    /**
     * Test the resolve method for the data object
     * @throws Exception
     */
    @Test
    public void testResolveData() throws Exception {
        String dataId = "testResolveData" + System.currentTimeMillis();
        String localId = "eml-error-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object =
            new ByteArrayInputStream("testInitCandidateList".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(dataId)).thenReturn(localId);
            Mockito.when(IdentifierManager.getInstance())
                .thenReturn(mockManager);
            HashStoreUpgrader upgrader = new HashStoreUpgrader();
            Path path = upgrader.resolve(sysmeta);
            assertTrue(Files.exists(path));
            assertEquals(8718, Files.size(path));

        }
    }

    /**
     * Test the convertSystemMetadata memthod
     * @throws Exception
     */
    @Test
    public void testConvertSystemMetadata() throws Exception {
        String dataId = "testResolveData" + System.currentTimeMillis();
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object =
            new ByteArrayInputStream("testInitCandidateList".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        InputStream input = HashStoreUpgrader.convertSystemMetadata(sysmeta);
        SystemMetadata read = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, input);
        assertEquals(sysmeta.getIdentifier(), read.getIdentifier());
        assertEquals(sysmeta.getSize(), read.getSize());
        assertEquals(sysmeta.getChecksum().getValue(), read.getChecksum().getValue());
        assertEquals(sysmeta.getChecksum().getAlgorithm(), read.getChecksum().getAlgorithm());
        assertEquals(sysmeta.getDateSysMetadataModified().getTime(),
                     read.getDateSysMetadataModified().getTime());
        assertEquals(sysmeta.getDateUploaded().getTime(), read.getDateUploaded().getTime());
        assertEquals(sysmeta.getFormatId().getValue(), read.getFormatId().getValue());
    }

    /**
     * Test the upgrade method for the data objects
     * @throws Exception
     */
    @Test
    public void testUpgradeData() throws Exception {
        String dataId = "HashStoreUpgraderTestUpgradeData" + System.currentTimeMillis();
        String localId = "eml-error-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new FileInputStream(new File(dataPath + "/" + localId));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        SystemMetadataManager.lock(pid);
        // Storing systemmetadata will store it on both db and hashstore. This is a duplicated
        // step to the conversion. So we only use it in this method to check the saving records to
        // the checksum table (It needs the system metadata being in db)
        SystemMetadataManager.getInstance().store(sysmeta);
        SystemMetadataManager.unLock(pid);
        ChecksumsManager checksumsManager = new ChecksumsManager();
        List<Checksum> checksums =  checksumsManager.get(pid);
        assertTrue(checksums.isEmpty());
        // mock IdentifierManager
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(dataId)).thenReturn(localId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.getString(1)).thenReturn(dataId);
            // mock only having one next
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
            // mock getSystemMetadata
            try (MockedStatic<SystemMetadataManager> ignoredSysmeta =
                Mockito.mockStatic(SystemMetadataManager.class)) {
                SystemMetadataManager manager = Mockito.mock(SystemMetadataManager.class);
                Mockito.when(manager.get(pid)).thenReturn(sysmeta);
                Mockito.when(SystemMetadataManager.getInstance()).thenReturn(manager);
                // mock HashStoreUpgrader with the real methods
                HashStoreUpgrader upgrader = Mockito.mock(
                    HashStoreUpgrader.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                // mock the initCandidate method
                Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
                upgrader.upgrade();
                File hashStoreRoot = new File(hashStorePath);
                assertTrue(hashStoreRoot.exists());
                assertEquals(0, upgrader.getInfo().length());
                assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid));
                assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid));
                checksums =  checksumsManager.get(pid);
                assertEquals(5, checksums.size());
            }
        }
    }

    /**
     * Test the upgrade method for the metadata objects
     * @throws Exception
     */
    @Test
    public void testUpgradeMetadata() throws Exception {
        String dataId = "HashStoreUpgraderTestUpgradeMetadata" + System.currentTimeMillis();
        String localId = "eml-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new FileInputStream(new File(documentPath + "/" + localId));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        sysmeta.setFormatId(eml220_format_id);
        // mock IdentifierManager
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(dataId)).thenReturn(localId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.getString(1)).thenReturn(dataId);
            // mock only having one next
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
            // mock getSystemMetadata
            try (MockedStatic<SystemMetadataManager> ignoredSysmeta =
                     Mockito.mockStatic(SystemMetadataManager.class)) {
                SystemMetadataManager manager = Mockito.mock(SystemMetadataManager.class);
                Mockito.when(manager.get(pid)).thenReturn(sysmeta);
                Mockito.when(SystemMetadataManager.getInstance()).thenReturn(manager);
                // mock HashStoreUpgrader with the real methods
                HashStoreUpgrader upgrader = Mockito.mock(
                    HashStoreUpgrader.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                // mock the initCandidate method
                Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
                upgrader.upgrade();
                File hashStoreRoot = new File(hashStorePath);
                assertTrue(hashStoreRoot.exists());
                assertTrue(upgrader.getInfo().length() > 0);
                assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid));
                assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid));
            }
        }
    }

    /**
     * Test the upgrade method for both the metadata and data objects
     * @throws Exception
     */
    @Test
    public void testUpgradeMetadataAndMetacat() throws Exception {
        String dataId = "HashStoreUpgradertestUpgradeMetadataAndMetacat"
                                + System.currentTimeMillis();
        String localId = "eml-error-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new FileInputStream(new File(dataPath + "/" + localId));
        SystemMetadata dataSysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);

        String dataId2 = "HashStoreUpgradertestUpgradeMetadataAndMetacat78"
                            + System.currentTimeMillis();
        String localId2 = "eml-error.xml";
        Identifier pid2 = new Identifier();
        pid2.setValue(dataId2);
        object = new FileInputStream(new File(dataPath + "/" + localId2));
        SystemMetadata dataSysmeta2 = D1NodeServiceTest.createSystemMetadata(pid2, owner, object);

        String metadataId = "HashStoreUpgradertestUpgradeMetadataAndMetacat12"
                                                + System.currentTimeMillis();
        String metaLocalId = "eml-2.2.0.xml";
        Identifier metaPid = new Identifier();
        metaPid.setValue(metadataId);
        object = new FileInputStream(new File(documentPath + "/" + metaLocalId));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(metaPid, owner, object);
        sysmeta.setFormatId(eml220_format_id);
        // mock IdentifierManager
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataId)).thenReturn(metaLocalId);
            Mockito.when(mockManager.getLocalId(dataId)).thenReturn(localId);
            Mockito.when(mockManager.getLocalId(dataId2)).thenReturn(localId2);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.getString(1)).thenReturn(metadataId).thenReturn(dataId)
                                                    .thenReturn(dataId2);
            // mock only having one next
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(true).thenReturn(true)
                                                .thenReturn(false);
            // mock getSystemMetadata
            try (MockedStatic<SystemMetadataManager> ignoredSysmeta =
                     Mockito.mockStatic(SystemMetadataManager.class)) {
                SystemMetadataManager manager = Mockito.mock(SystemMetadataManager.class);
                Mockito.when(manager.get(metaPid)).thenReturn(sysmeta);
                Mockito.when(manager.get(pid)).thenReturn(dataSysmeta);
                Mockito.when(manager.get(pid2)).thenReturn(dataSysmeta2);
                Mockito.when(SystemMetadataManager.getInstance()).thenReturn(manager);
                // mock HashStoreUpgrader with the real methods
                HashStoreUpgrader upgrader = Mockito.mock(
                    HashStoreUpgrader.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                // mock the initCandidate method
                Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
                upgrader.upgrade();
                File hashStoreRoot = new File(hashStorePath);
                assertTrue(hashStoreRoot.exists());
                assertTrue(upgrader.getInfo().length() > 0);
                assertNotNull(MetacatInitializer.getStorage().retrieveObject(metaPid));
                assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(metaPid));
                assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid));
                assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid));
                assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid2));
                assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid2));
            }
        }
    }

    /**
     * Test the upgrade method for the data objects with wrong checksums
     * @throws Exception
     */
    @Test
    public void testUpgradeDataWithNonSupportChecksumAlgorithm() throws Exception {
        String dataId = "HashStoreUpgraderTestUpgrade" + System.currentTimeMillis();
        String localId = "eml-error-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new FileInputStream(new File(dataPath + "/" + localId));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        //set the wrong checksum
        sysmeta.getChecksum().setAlgorithm("foo");
        // mock IdentifierManager
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(dataId)).thenReturn(localId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.getString(1)).thenReturn(dataId);
            // mock only having one next
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
            // mock getSystemMetadata
            try (MockedStatic<SystemMetadataManager> ignoredSysmeta =
                     Mockito.mockStatic(SystemMetadataManager.class)) {
                SystemMetadataManager manager = Mockito.mock(SystemMetadataManager.class);
                Mockito.when(manager.get(pid)).thenReturn(sysmeta);
                Mockito.when(SystemMetadataManager.getInstance()).thenReturn(manager);
                // mock HashStoreUpgrader with the real methods
                HashStoreUpgrader upgrader = Mockito.mock(
                    HashStoreUpgrader.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                // mock the initCandidate method
                Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
                upgrader.upgrade();
                File hashStoreRoot = new File(hashStorePath);
                assertTrue(hashStoreRoot.exists());
                assertTrue(upgrader.getInfo().length() > 0);
                assertFalse(upgrader.getInfo().contains("nonMatchingChecksum"));
                assertFalse(upgrader.getInfo().contains("general"));
                assertTrue(upgrader.getInfo().contains("noSuchAlgorithm"));
                assertFalse(upgrader.getInfo().contains("noChecksumInSysmeta"));
                assertFalse(upgrader.getInfo().contains("savingChecksumTableError"));
                Vector<String> content = readContentFromFileInDir();
                assertEquals(1, content.size());
                assertTrue(content.contains(dataId));
                try {
                    MetacatInitializer.getStorage().retrieveObject(pid);
                    fail("Test can't get there since the pid " + pid + " was converted.");
                } catch(Exception e) {
                    assertTrue(e instanceof FileNotFoundException);
                }
                try {
                    MetacatInitializer.getStorage().retrieveMetadata(pid);
                    fail("Test can't get there since the pid " + pid + " was converted.");
                } catch(Exception e) {
                    assertTrue(e instanceof FileNotFoundException);
                }
            }
        }
    }

    /**
     * Test the upgrade method for both the metadata and data objects with the wrong checksums
     * @throws Exception
     */
    @Test
    public void testUpgradeWithIncorrectChecksum() throws Exception {
        D1NodeServiceTest d1NodeServiceTest = new D1NodeServiceTest("HashStoreUpgraderIT");
        Session session = d1NodeServiceTest.getTestSession();
        String dataId = "testUpgradeWithIncorrectChecksum567"
            + System.currentTimeMillis();
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new ByteArrayInputStream(dataId.getBytes());
        SystemMetadata sysmeta0 =
            D1NodeServiceTest.createSystemMetadata(pid, session.getSubject(), object);
        object = new ByteArrayInputStream(dataId.getBytes());
        d1NodeServiceTest.mnCreate(session, pid, object, sysmeta0);
        SystemMetadata read = SystemMetadataManager.getInstance().get(pid);
        String originDataChecksum = read.getChecksum().getValue();
        // delete the object from hash store to mock the old storage
        MetacatInitializer.getStorage().deleteObject(pid);
        try  {
            MetacatInitializer.getStorage().retrieveObject(pid);
            fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                     + "from the hashstore.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        try  {
            MetacatInitializer.getStorage().retrieveMetadata(pid);
            fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                     + "from the hashstore.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        String docid = IdentifierManager.getInstance().getLocalId(pid.getValue());
        Checksum checksum = new Checksum();
        checksum.setValue("foo");
        checksum.setAlgorithm(read.getChecksum().getAlgorithm());
        read.setChecksum(checksum);
        SystemMetadataManager.lock(pid);
        SystemMetadataManager.getInstance().store(read);
        SystemMetadataManager.unLock(pid);
        //Only delete the metadata from hashstore
        MetacatInitializer.getStorage().deleteMetadata(pid);
        read = SystemMetadataManager.getInstance().get(pid);
        long originalDataModificationTime = read.getDateSysMetadataModified().getTime();
        assertEquals("foo", read.getChecksum().getValue());
        // create the docid in the data directory to simulate the old storage system
        File oldDataFile = new File(dataPath + "/" + docid);
        try {
            object = new ByteArrayInputStream(dataId.getBytes());
            FileUtils.copyToFile(object, oldDataFile);
            File hashStore = new File(hashStorePath);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
            Mockito.when(resultSetMock.getString(1)).thenReturn(dataId);
            // mock HashStoreUpgrader with the real methods
            HashStoreUpgrader upgrader = Mockito.mock(
                HashStoreUpgrader.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            // mock the initCandidate method
            Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
            upgrader.upgrade();
            assertTrue(hashStore.exists());
            assertTrue(upgrader.getInfo().length() > 0);
            assertTrue(upgrader.getInfo().contains("nonMatchingChecksum"));
            assertFalse(upgrader.getInfo().contains("general"));
            assertFalse(upgrader.getInfo().contains("noSuchAlgorithm"));
            assertFalse(upgrader.getInfo().contains("noChecksumInSysmeta"));
            assertFalse(upgrader.getInfo().contains("savingChecksumTableError"));
            Vector<String> content = readContentFromFileInDir();
            assertEquals(2, content.size());
            assertTrue(content.elementAt(0).contains(dataId));
            assertTrue(content.elementAt(1).contains("@"));
            assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid));
            assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid));
            SystemMetadata newRead = SystemMetadataManager.getInstance().get(pid);
            SystemMetadata sysFromHash =
                TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                                                       MetacatInitializer.getStorage()
                                                           .retrieveMetadata(pid));
            assertEquals(
                originalDataModificationTime, newRead.getDateSysMetadataModified().getTime());
            assertEquals(
                originalDataModificationTime, sysFromHash.getDateSysMetadataModified().getTime());
            assertEquals(originDataChecksum, newRead.getChecksum().getValue());
            assertEquals(originDataChecksum, sysFromHash.getChecksum().getValue());
        } finally {
            oldDataFile.delete();
        }
    }

    /**
     * Test the scenario that a metadata object with incorrect checksum
     * @throws Exception
     */
    @Test
    public void testUpgradeWithIncorrectChecksumForMetadata() throws Exception {
        D1NodeServiceTest d1NodeServiceTest = new D1NodeServiceTest("HashStoreUpgraderIT");
        Session session = d1NodeServiceTest.getTestSession();
        String dataId = "testUpgradeWithIncorrectChecksumForMetadata123"
            + System.currentTimeMillis();
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new FileInputStream("test/eml-2.2.0.xml");
        SystemMetadata sysmeta0 =
            D1NodeServiceTest.createSystemMetadata(pid, session.getSubject(), object);
        sysmeta0.setFormatId(eml220_format_id);
        object = new FileInputStream("test/eml-2.2.0.xml");
        d1NodeServiceTest.mnCreate(session, pid, object, sysmeta0);
        SystemMetadata read = SystemMetadataManager.getInstance().get(pid);
        String originDataChecksum = read.getChecksum().getValue();
        // delete the object from hash store to mock the old storage
        MetacatInitializer.getStorage().deleteObject(pid);
        try  {
            MetacatInitializer.getStorage().retrieveObject(pid);
            fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                     + "from the hashstore.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        String docid = IdentifierManager.getInstance().getLocalId(pid.getValue());
        Checksum checksum = new Checksum();
        checksum.setValue("foo");
        checksum.setAlgorithm(read.getChecksum().getAlgorithm());
        read.setChecksum(checksum);
        SystemMetadataManager.lock(pid);
        SystemMetadataManager.getInstance().store(read);
        SystemMetadataManager.unLock(pid);
        MetacatInitializer.getStorage().deleteMetadata(pid);
        read = SystemMetadataManager.getInstance().get(pid);
        long originalDataModificationTime = read.getDateSysMetadataModified().getTime();
        assertEquals("foo", read.getChecksum().getValue());
        // create the docid in the data directory to simulate the old storage system
        // note: this is an eml document, but is purposely stored in the dataPath. The convert
        // sould find it.
        File oldDataFile = new File(dataPath + "/" + docid);
        try {
            object = new FileInputStream("test/eml-2.2.0.xml");
            FileUtils.copyToFile(object, oldDataFile);
            File hashStore = new File(hashStorePath);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
            Mockito.when(resultSetMock.getString(1)).thenReturn(dataId);
            // mock HashStoreUpgrader with the real methods
            HashStoreUpgrader upgrader = Mockito.mock(
                HashStoreUpgrader.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            // mock the initCandidate method
            Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
            upgrader.upgrade();
            assertTrue(hashStore.exists());
            assertTrue(upgrader.getInfo().length() > 0);
            assertTrue(upgrader.getInfo().contains("nonMatchingChecksum"));
            assertFalse(upgrader.getInfo().contains("general"));
            assertFalse(upgrader.getInfo().contains("noSuchAlgorithm"));
            assertFalse(upgrader.getInfo().contains("noChecksumInSysmeta"));
            assertFalse(upgrader.getInfo().contains("savingChecksumTableError"));
            Vector<String> content = readContentFromFileInDir();
            assertEquals(2, content.size());
            assertTrue(content.elementAt(0).contains(dataId));
            assertTrue(content.elementAt(1).contains("@"));
            assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid));
            assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid));
            SystemMetadata newRead = SystemMetadataManager.getInstance().get(pid);
            SystemMetadata sysFromHash =
                TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                                                       MetacatInitializer.getStorage()
                                                           .retrieveMetadata(pid));
            assertEquals(
                originalDataModificationTime, newRead.getDateSysMetadataModified().getTime());
            assertEquals(
                originalDataModificationTime, sysFromHash.getDateSysMetadataModified().getTime());
            assertEquals(originDataChecksum, newRead.getChecksum().getValue());
            assertEquals(eml220_format_id.getValue(), newRead.getFormatId().getValue());
            assertEquals(originDataChecksum, sysFromHash.getChecksum().getValue());
            assertEquals(eml220_format_id.getValue(), sysFromHash.getFormatId().getValue());
        } finally {
            oldDataFile.delete();
        }
    }

    /**
     * Test the upgrade method for both the metadata and data objects with the wrong checksum and
     * algorithm
     * @throws Exception
     */
    @Test
    public void testUpgradeWithIncorrectAlgorithm() throws Exception {
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        String metadataId = "testUpgradeWithIncorrectChecksumAndAlgorithm12"
            + System.currentTimeMillis();
        String metaLocalId = "eml-2.2.0.xml";
        Identifier metaPid = new Identifier();
        metaPid.setValue(metadataId);
        InputStream object = new FileInputStream(documentPath + "/" + metaLocalId);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(metaPid, owner, object);
        sysmeta.setFormatId(eml220_format_id);
        sysmeta.getChecksum().setAlgorithm("edsf");
        // mock IdentifierManager
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataId)).thenReturn(metaLocalId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.getString(1)).thenReturn(metadataId);
            // mock only having one next
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
            // mock getSystemMetadata
            try (MockedStatic<SystemMetadataManager> ignoredSysmeta =
                     Mockito.mockStatic(SystemMetadataManager.class)) {
                SystemMetadataManager manager = Mockito.mock(SystemMetadataManager.class);
                Mockito.when(manager.get(metaPid)).thenReturn(sysmeta);
                Mockito.when(SystemMetadataManager.getInstance()).thenReturn(manager);
                // mock HashStoreUpgrader with the real methods
                HashStoreUpgrader upgrader = Mockito.mock(
                    HashStoreUpgrader.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                // mock the initCandidate method
                Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
                upgrader.upgrade();
                File hashStoreRoot = new File(hashStorePath);
                assertTrue(hashStoreRoot.exists());
                assertTrue(upgrader.getInfo().length() > 0);
                assertFalse(upgrader.getInfo().contains("nonMatchingChecksum"));
                assertFalse(upgrader.getInfo().contains("general"));
                assertTrue(upgrader.getInfo().contains("noSuchAlgorithm"));
                assertFalse(upgrader.getInfo().contains("noChecksumInSysmeta"));
                try {
                    MetacatInitializer.getStorage().retrieveObject(metaPid);
                    fail("Test can't get there since the pid " + metaPid + " was not converted.");
                } catch(Exception e) {
                    assertTrue(e instanceof FileNotFoundException);
                }
                try {
                    MetacatInitializer.getStorage().retrieveMetadata(metaPid);
                    fail("Test can't get there since the pid " + metaPid + " was not converted.");
                } catch(Exception e) {
                    assertTrue(e instanceof FileNotFoundException);
                }
            }
        }
    }

    /**
     * Test the upgrade method for the data objects with a general error
     * @throws Exception
     */
    @Test
    public void testUpgradeDataWithGeneralError() throws Exception {
        String dataId =
            "HashStoreUpgradertestUpgradeDataWithGeneralError" + System.currentTimeMillis();
        String localId = "eml-error-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new FileInputStream(new File(dataPath + "/" + localId));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        // mock IdentifierManager
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(dataId)).thenReturn(localId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.getString(1)).thenReturn(dataId);
            // mock only having one next
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
            // mock getSystemMetadata
            try (MockedStatic<SystemMetadataManager> ignoredSysmeta =
                     Mockito.mockStatic(SystemMetadataManager.class)) {
                SystemMetadataManager manager = Mockito.mock(SystemMetadataManager.class);
                // No mock for the dataId, so it will cause a problem
                Identifier foo = new Identifier();
                foo.setValue("foo");
                Mockito.when(manager.get(foo)).thenReturn(sysmeta);
                Mockito.when(SystemMetadataManager.getInstance()).thenReturn(manager);
                // mock HashStoreUpgrader with the real methods
                HashStoreUpgrader upgrader = Mockito.mock(
                    HashStoreUpgrader.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                // mock the initCandidate method
                Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
                upgrader.upgrade();
                File hashStoreRoot = new File(hashStorePath);
                assertTrue(hashStoreRoot.exists());
                assertTrue(upgrader.getInfo().length() > 0);
                assertFalse(upgrader.getInfo().contains("nonMatchingChecksum"));
                assertTrue(upgrader.getInfo().contains("general"));
                assertFalse(upgrader.getInfo().contains("noSuchAlgorithm"));
                assertFalse(upgrader.getInfo().contains("noChecksumInSysmeta"));
                try {
                    MetacatInitializer.getStorage().retrieveObject(pid);
                    fail("Test can't get there since the pid " + pid + " was not converted.");
                } catch(Exception e) {
                    assertTrue(e instanceof FileNotFoundException);
                }
                try {
                    MetacatInitializer.getStorage().retrieveMetadata(pid);
                    fail("Test can't get there since the pid " + pid + " was not converted.");
                } catch(Exception e) {
                    assertTrue(e instanceof FileNotFoundException);
                }
            }
        }
    }

    /**
     * Test the writeToFile method
     * @throws Exception
     */
    @Test
    public void testWriteToFile() throws Exception {
        String message = "hello";
        String message1 = "hello1";
        File temp = File.createTempFile("test", ".text");
        try (BufferedWriter generalWriter = new BufferedWriter(
                 new FileWriter(temp, true))) {
            HashStoreUpgrader.writeToFile(message, generalWriter);
            HashStoreUpgrader.writeToFile(message1, generalWriter);
        }
        Vector<String> content = readContentFromFile(temp);
        assertEquals(2, content.size());
        assertEquals(message, content.get(0));
        assertEquals(message1, content.get(1));
    }

    /**
     * Test the writeToFile method with exceptions
     * @throws Exception
     */
    @Test
    public void testWriteToFileWithException() throws Exception {
        String message = "hello";
        String exceptionStr = "This is an exception";
        Exception exception = new Exception(exceptionStr);
        String message1 = "hello1";
        String exceptionStr1 = "This is an exception";
        Exception exception1 = new Exception(exceptionStr1);
        File temp = File.createTempFile("test", ".text");
        try (BufferedWriter generalWriter = new BufferedWriter(
            new FileWriter(temp, true))) {
            HashStoreUpgrader.writeToFile(message, exception, generalWriter);
            HashStoreUpgrader.writeToFile(message1, exception1, generalWriter);
        }
        Vector<String> content = readContentFromFile(temp);
        assertEquals(2, content.size());
        assertEquals(message + " " + exceptionStr, content.get(0));
        assertEquals(message1 + " " +exceptionStr1, content.get(1));
    }

    /**
     * Read the content of the first file in the backup directory
     * @return a vector of String. Each line is an element in the vector.
     * @throws IOException
     */
    private Vector<String> readContentFromFileInDir() throws IOException {
        Vector<String> content = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(backupPath))) {
            for (Path path : stream) {
                content = readContentFromFile(path.toFile());
                // we need only read one file
                break;
            }
        }
        return content;
    }

    /**
     * Read the content of the given file
     * @return a vector of String. Each line is an element in the vector.
     * @throws IOException
     */
    private Vector<String> readContentFromFile(File file) throws IOException {
        Vector<String> content = new Vector<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String str = reader.readLine();
        while (str != null) {
            content.add(str);
            str = reader.readLine();
        }
        return content;
    }

    /**
     * Test the scenario that the object hasn't been converted to the dataone object (no system
     * metadata, no record in the identifier table)
     * @throws Exception
     */
    @Test
    public void testUpgradeWithoutSystemMetadata() throws Exception {
        String fileName = "test/eml-2.2.0.xml";
        String prefix = "testUpgradeWithoutSysteMetadata" + "."
            + System.currentTimeMillis();
        System.out.print("the prefix is " + prefix);
        String id1 = prefix + "." + 1;
        String id2 = prefix + "." + 2;
        String id3 = prefix + "." + 3;
        Identifier pid1 = new Identifier();
        pid1.setValue(id1);
        Identifier pid2 = new Identifier();
        pid2.setValue(id2);
        Identifier pid3 = new Identifier();
        pid3.setValue(id3);
        String user = "http://orcid.org/1234/4567";
        String docName = "eml";
        String docType = "https://eml.ecoinformatics.org/eml-2.2.0";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection(
                "HashStoreUpgraderTest.testInitCandidateListWithNonDataONEObject");
            serialNumber = dbConn.getCheckOutSerialNumber();
            // register two docids in the xml_documents and xml_revisions tables.
            // But there are no system metadata and identifier records for them
            DocumentImpl.registerDocument(docName, docType, dbConn, id1, user);
            DocumentImpl.registerDocument(docName, docType, dbConn, id2, user);
            DocumentImpl.registerDocument(docName, docType, dbConn, id3, user);
            assertTrue("The xml_documents table should have the record " + id3,
                       IntegrationTestUtils.hasRecord("xml_documents", dbConn, " rev=? and docid like ?"
                           , 3, prefix));
            assertTrue("The xml_revisions table should have the record " + id2,
                       IntegrationTestUtils.hasRecord("xml_revisions", dbConn, " rev=? and docid like ?"
                           , 2, prefix));
            assertTrue("The xml_revisions table should have the record " + id1,
                       IntegrationTestUtils.hasRecord("xml_revisions", dbConn, " rev=? and docid like ?"
                           , 1, prefix));
            try {
                IdentifierManager.getInstance().getGUID(prefix, 1);
                fail("Test shouldn't reach here since " + id1 + " shouldn't exist in the "
                         + "identifer table");
            } catch (Exception e) {
                assertTrue(e instanceof McdbDocNotFoundException);
            }
            assertFalse(IdentifierManager.getInstance().mappingExists(id1));
            try {
                IdentifierManager.getInstance().getGUID(prefix, 2);
                fail("Test shouldn't reach here since " + id1 + " shouldn't exist in the "
                         + "identifer table");
            } catch (Exception e) {
                assertTrue(e instanceof McdbDocNotFoundException);
            }
            assertFalse(IdentifierManager.getInstance().mappingExists(id2));
            try {
                IdentifierManager.getInstance().getGUID(prefix, 3);
                fail("Test shouldn't reach here since " + id1 + " shouldn't exist in the "
                         + "identifer table");
            } catch (Exception e) {
                assertTrue(e instanceof McdbDocNotFoundException);
            }
            assertFalse(IdentifierManager.getInstance().mappingExists(id3));
            SystemMetadata systemMetadata = SystemMetadataManager.getInstance().get(pid1);
            assertNull(systemMetadata);
            systemMetadata = SystemMetadataManager.getInstance().get(pid2);
            assertNull(systemMetadata);
            systemMetadata = SystemMetadataManager.getInstance().get(pid3);
            assertNull(systemMetadata);
        } finally {
            DBConnectionPool.returnDBConnection(
                dbConn, serialNumber);
        }
        // Write files to the document directory
        File file = new File(fileName);
        File dest1 = new File(documentPath + "/" + id1);
        File dest2 = new File(documentPath + "/" + id2);
        File dest3 = new File(documentPath + "/" + id3);
        try {
            FileUtils.copyFile(file, dest1);
            FileUtils.copyFile(file, dest2);
            FileUtils.copyFile(file, dest3);
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            // mock only having three next
            Mockito.when(resultSetMock.getString(1)).thenReturn(id1).thenReturn(id2).thenReturn(id3);
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(true).thenReturn(true)
                .thenReturn(false);
            // mock HashStoreUpgrader with the real methods
            HashStoreUpgrader upgrader = Mockito.mock(HashStoreUpgrader.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            // mock the initCandidate method
            Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
            upgrader.upgrade();

            // Check the results.
            File hashStoreRoot = new File(hashStorePath);
            assertTrue(hashStoreRoot.exists());
            assertEquals(id1, IdentifierManager.getInstance().getGUID(prefix, 1));
            assertTrue(IdentifierManager.getInstance().mappingExists(id1));
            SystemMetadata systemMetadata1 = SystemMetadataManager.getInstance().get(pid1);
            assertNotNull(systemMetadata1);
            assertEquals(id1, systemMetadata1.getIdentifier().getValue());
            assertEquals(
                PropertyService.getProperty("dataone.nodeId"),
                systemMetadata1.getOriginMemberNode().getValue());
            assertEquals(
                PropertyService.getProperty("dataone.nodeId"),
                systemMetadata1.getAuthoritativeMemberNode().getValue());
            assertEquals(id2, systemMetadata1.getObsoletedBy().getValue());
            assertNull(systemMetadata1.getObsoletes());
            assertEquals("MD5", systemMetadata1.getChecksum().getAlgorithm());
            assertEquals(
                "f4ea2d07db950873462a064937197b0f", systemMetadata1.getChecksum().getValue());
            assertEquals(8724, systemMetadata1.getSize().intValue());
            assertEquals(user, systemMetadata1.getRightsHolder().getValue());
            assertEquals(user, systemMetadata1.getSubmitter().getValue());
            assertEquals(docType, systemMetadata1.getFormatId().getValue());

            assertEquals(id2, IdentifierManager.getInstance().getGUID(prefix, 2));
            assertTrue(IdentifierManager.getInstance().mappingExists(id2));
            SystemMetadata systemMetadata2 = SystemMetadataManager.getInstance().get(pid2);
            assertNotNull(systemMetadata2);
            assertEquals(id2, systemMetadata2.getIdentifier().getValue());
            assertEquals(
                PropertyService.getProperty("dataone.nodeId"),
                systemMetadata2.getOriginMemberNode().getValue());
            assertEquals(
                PropertyService.getProperty("dataone.nodeId"),
                systemMetadata2.getAuthoritativeMemberNode().getValue());
            assertEquals(id1, systemMetadata2.getObsoletes().getValue());
            assertEquals(id3, systemMetadata2.getObsoletedBy().getValue());
            assertEquals("MD5", systemMetadata2.getChecksum().getAlgorithm());
            assertEquals(
                "f4ea2d07db950873462a064937197b0f", systemMetadata2.getChecksum().getValue());
            assertEquals(8724, systemMetadata2.getSize().intValue());
            assertEquals(user, systemMetadata2.getRightsHolder().getValue());
            assertEquals(user, systemMetadata2.getSubmitter().getValue());
            assertEquals(docType, systemMetadata2.getFormatId().getValue());

            assertEquals(id3, IdentifierManager.getInstance().getGUID(prefix, 3));
            assertTrue(IdentifierManager.getInstance().mappingExists(id3));
            SystemMetadata systemMetadata3 = SystemMetadataManager.getInstance().get(pid3);
            assertNotNull(systemMetadata3);
            assertEquals(id3, systemMetadata3.getIdentifier().getValue());
            assertEquals(
                PropertyService.getProperty("dataone.nodeId"),
                systemMetadata3.getOriginMemberNode().getValue());
            assertEquals(
                PropertyService.getProperty("dataone.nodeId"),
                systemMetadata3.getAuthoritativeMemberNode().getValue());
            assertEquals(id2, systemMetadata3.getObsoletes().getValue());
            assertNull(systemMetadata3.getObsoletedBy());
            assertEquals("MD5", systemMetadata3.getChecksum().getAlgorithm());
            assertEquals(
                "f4ea2d07db950873462a064937197b0f", systemMetadata3.getChecksum().getValue());
            assertEquals(8724, systemMetadata3.getSize().intValue());
            assertEquals(user, systemMetadata3.getRightsHolder().getValue());
            assertEquals(user, systemMetadata3.getSubmitter().getValue());
            assertEquals(docType, systemMetadata3.getFormatId().getValue());

            assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid1));
            assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid1));
            assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid2));
            assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid2));
            assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid3));
            assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid3));

            SystemMetadata sysmetaFromHash =
                TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                                                                     MetacatInitializer.getStorage()
                                                                         .retrieveMetadata(pid1));
            compareValues(systemMetadata1, sysmetaFromHash);
            sysmetaFromHash = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                                                                     MetacatInitializer.getStorage()
                                                                         .retrieveMetadata(pid2));
            compareValues(systemMetadata2, sysmetaFromHash);
            sysmetaFromHash = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                                                       MetacatInitializer.getStorage()
                                                           .retrieveMetadata(pid3));
            compareValues(systemMetadata3, sysmetaFromHash);
        } finally {
            try {
                if (dest1.exists()) {
                    dest1.delete();
                }
            } catch (Exception e) {
            }
            try {
                if (dest2.exists()) {
                    dest2.delete();
                }
            } catch (Exception e) {
            }
            try {
                if (dest3.exists()) {
                    dest3.delete();
                }
            } catch (Exception e) {
            }
        }

    }

    private void compareValues(SystemMetadata mcSysmeta, SystemMetadata sysmeta)
        throws Exception {
        assertEquals(sysmeta.getIdentifier().getValue(), mcSysmeta.getIdentifier().getValue());
        assertEquals(sysmeta.getFormatId().getValue(), mcSysmeta.getFormatId().getValue());
        assertEquals(sysmeta.getSerialVersion().longValue(),
                     mcSysmeta.getSerialVersion().longValue());
        assertEquals(sysmeta.getSize().longValue(), mcSysmeta.getSize().longValue());
        assertEquals(sysmeta.getChecksum().getValue(), mcSysmeta.getChecksum().getValue());
        assertEquals(sysmeta.getChecksum().getAlgorithm(), mcSysmeta.getChecksum().getAlgorithm());
        assertEquals(sysmeta.getSubmitter().getValue(), mcSysmeta.getSubmitter().getValue());
        assertEquals(sysmeta.getRightsHolder().getValue(), mcSysmeta.getRightsHolder().getValue());
        assertEquals(sysmeta.getDateUploaded().getTime(), mcSysmeta.getDateUploaded().getTime());
        assertEquals(sysmeta.getDateSysMetadataModified().getTime(),
                     mcSysmeta.getDateSysMetadataModified().getTime());
        assertEquals(
            sysmeta.getOriginMemberNode().getValue(), mcSysmeta.getOriginMemberNode().getValue());
        assertEquals(sysmeta.getAuthoritativeMemberNode().getValue(),
                     mcSysmeta.getAuthoritativeMemberNode().getValue());
    }

    /**
     * Test an object created by DataONE api. Somehow the system does exist while the map in the
     * identifier table is missing. The conversion will fail since there is no map between pid
     * and docid. So it is hard to find the object's file path.
     * @throws Exception
     */
    @Test
    public void testMissingIdentifierMappingForDataONEobj() throws Exception {
        D1NodeServiceTest d1NodeServiceTest = new D1NodeServiceTest("HashStoreUpgraderIT");
        Session session = d1NodeServiceTest.getTestSession();
        String dataId = "testMissingIdentifierMapping1234"
            + System.currentTimeMillis();
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new ByteArrayInputStream(dataId.getBytes());
        SystemMetadata sysmeta0 =
            D1NodeServiceTest.createSystemMetadata(pid, session.getSubject(), object);
        object = new ByteArrayInputStream(dataId.getBytes());
        d1NodeServiceTest.mnCreate(session, pid, object, sysmeta0);
        SystemMetadata read = SystemMetadataManager.getInstance().get(pid);
        // delete the object from hash store to mock the old storage
        MetacatInitializer.getStorage().deleteObject(pid);
        try  {
            MetacatInitializer.getStorage().retrieveObject(pid);
            fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                     + "from the hashstore.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        try  {
            MetacatInitializer.getStorage().retrieveMetadata(pid);
            fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                     + "from the hashstore.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        String docid = IdentifierManager.getInstance().getLocalId(pid.getValue());
        // Deleting the records from checksums
        ChecksumsManager manager = new ChecksumsManager();
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("testMissingIdentifierMappingForDataONEobj");
            serialNumber = dbConn.getCheckOutSerialNumber();
            manager.delete(pid, dbConn);
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        // Deleting the mapping records in the identifier table
        IdentifierManager.getInstance().removeMapping(pid.getValue(), docid);

        // Make sure the docid is in the candidate list
        HashStoreUpgrader upgrader1 = new HashStoreUpgrader();
        ResultSet resultSet = upgrader1.initCandidateList();
        boolean found = false;
        while (resultSet.next()) {
            String id = resultSet.getString(1);
            if (id.equals(docid)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
        // Now we will have a fake pid with the value of docid
        Identifier fakeId = new Identifier();
        fakeId.setValue(docid);
        File oldDataFile = new File(dataPath + "/" + docid);
        try {
            // Create a docid in the old data file path to mock the object in the legacy storage
            object = new ByteArrayInputStream(dataId.getBytes());
            FileUtils.copyToFile(object, oldDataFile);
            File hashStore = new File(hashStorePath);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
            Mockito.when(resultSetMock.getString(1)).thenReturn(fakeId.getValue());
            // mock HashStoreUpgrader with the real methods
            HashStoreUpgrader upgrader = Mockito.mock(
                HashStoreUpgrader.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            // mock the initCandidate method
            Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
            upgrader.upgrade();
            assertTrue(hashStore.exists());
            assertTrue(upgrader.getInfo().length() > 0);
            assertFalse(upgrader.getInfo().contains("nonMatchingChecksum"));
            assertTrue(upgrader.getInfo().contains("general"));
            assertFalse(upgrader.getInfo().contains("noSuchAlgorithm"));
            assertFalse(upgrader.getInfo().contains("noChecksumInSysmeta"));
            assertFalse(upgrader.getInfo().contains("savingChecksumTableError"));
            Vector<String> content = readContentFromFileInDir();
            assertEquals(1, content.size());
            assertTrue(content.elementAt(0).contains(fakeId.getValue()));
            try  {
                MetacatInitializer.getStorage().retrieveObject(pid);
                fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                         + "from the hashstore.");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
            try  {
                MetacatInitializer.getStorage().retrieveMetadata(pid);
                fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                         + "from the hashstore.");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
            try  {
                MetacatInitializer.getStorage().retrieveObject(fakeId);
                fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                         + "from the hashstore.");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
            try  {
                MetacatInitializer.getStorage().retrieveMetadata(fakeId);
                fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                         + "from the hashstore.");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
            assertNull(SystemMetadataManager.getInstance().get(fakeId));
            assertEquals(dataId,
                         SystemMetadataManager.getInstance().get(pid).getIdentifier().getValue());
        } finally {
            oldDataFile.delete();
        }
    }

    /**
     * Test an object created by the old Metacat API. Somehow it wasn't converted to the dataone
     * object successfully in the previous upgrade. So it doesn't have records in the
     * systemmetadata and identifier table. Also, it doesn't start with autogen. The conversion
     * will succeed to create hashstore object with the identifier being the docid+rev. Also the
     * basic system metadata will be generated
     * @throws Exception
     */
    @Test
    public void testOldMetacatAPIobject() throws Exception {
        String nonAutoGenDocid = "foo." + System.currentTimeMillis();
        D1NodeServiceTest d1NodeServiceTest = new D1NodeServiceTest("HashStoreUpgraderIT");
        Session session = d1NodeServiceTest.getTestSession();
        String dataId = "testOldMetacatAPIobject"
            + System.currentTimeMillis();
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new ByteArrayInputStream(dataId.getBytes());
        SystemMetadata sysmeta0 =
            D1NodeServiceTest.createSystemMetadata(pid, session.getSubject(), object);
        object = new ByteArrayInputStream(dataId.getBytes());
        d1NodeServiceTest.mnCreate(session, pid, object, sysmeta0);
        SystemMetadata read = SystemMetadataManager.getInstance().get(pid);
        assertEquals(pid.getValue(), read.getIdentifier().getValue());
        // delete the object from hash store to mock the old storage
        MetacatInitializer.getStorage().deleteObject(pid);
        try  {
            MetacatInitializer.getStorage().retrieveObject(pid);
            fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                     + "from the hashstore.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        try  {
            MetacatInitializer.getStorage().retrieveMetadata(pid);
            fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                     + "from the hashstore.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        String docid = IdentifierManager.getInstance().getLocalId(pid.getValue());
        // Deleting the mapping records in the identifier table
        IdentifierManager.getInstance().removeMapping(pid.getValue(), docid);

        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("testMissingIdentifierMappingForDataONEobj");
            serialNumber = dbConn.getCheckOutSerialNumber();
            // Deleting the records from checksums
            ChecksumsManager manager = new ChecksumsManager();
            manager.delete(pid, dbConn);
            // Replace the docid from autogen. to the nonAutoGenDocid
            // Parse the localId into scope and rev parts
            AccessionNumber acc = new AccessionNumber(docid, "NOACTION");
            String docidWithoutRev = acc.getDocid();
            String sql = "update xml_documents set docid=? where docid=?;";
            PreparedStatement preparedStatement = dbConn.prepareStatement(sql);
            preparedStatement.setString(1, nonAutoGenDocid);
            preparedStatement.setString(2, docidWithoutRev);
            int size = preparedStatement.executeUpdate();
            assertEquals(1, size);
            // Append the revision 1 to the nonAutoGenDocid
            nonAutoGenDocid = nonAutoGenDocid + ".1";
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        // Deleting the systemmetadata
        SystemMetadataManager.lock(pid);
        SystemMetadataManager.getInstance().delete(pid);
        SystemMetadataManager.unLock(pid);

        // Make sure the docid is in the candidate list
        HashStoreUpgrader upgrader1 = new HashStoreUpgrader();
        ResultSet resultSet = upgrader1.initCandidateList();
        boolean found = false;
        while (resultSet.next()) {
            String id = resultSet.getString(1);
            if (id.equals(nonAutoGenDocid)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
        assertFalse(IdentifierManager.getInstance().mappingExists(nonAutoGenDocid));
        // Now we will have a fake pid with the value of nonAutoGenDocid
        Identifier fakeId = new Identifier();
        fakeId.setValue(nonAutoGenDocid);
        File oldDataFile = new File(dataPath + "/" + nonAutoGenDocid);
        try {
            // Create a docid in the old data file path to mock the object in the legacy storage
            object = new ByteArrayInputStream(dataId.getBytes());
            FileUtils.copyToFile(object, oldDataFile);
            File hashStore = new File(hashStorePath);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
            Mockito.when(resultSetMock.getString(1)).thenReturn(fakeId.getValue());
            // mock HashStoreUpgrader with the real methods
            HashStoreUpgrader upgrader = Mockito.mock(
                HashStoreUpgrader.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            // mock the initCandidate method
            Mockito.doReturn(resultSetMock).when(upgrader).initCandidateList();
            upgrader.upgrade();
            assertTrue(hashStore.exists());
            assertEquals(0, upgrader.getInfo().length());
            assertFalse(upgrader.getInfo().contains("nonMatchingChecksum"));
            assertFalse(upgrader.getInfo().contains("general"));
            assertFalse(upgrader.getInfo().contains("noSuchAlgorithm"));
            assertFalse(upgrader.getInfo().contains("noChecksumInSysmeta"));
            assertFalse(upgrader.getInfo().contains("savingChecksumTableError"));
            try  {
                MetacatInitializer.getStorage().retrieveObject(pid);
                fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                         + "from the hashstore.");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
            try  {
                MetacatInitializer.getStorage().retrieveMetadata(pid);
                fail("test should not get there since the pid " + pid.getValue()  + " was deleted "
                         + "from the hashstore.");
            } catch (Exception e) {
                assertTrue(e instanceof FileNotFoundException);
            }
            assertNotNull(MetacatInitializer.getStorage().retrieveObject(fakeId));
            assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(fakeId));
            assertNull(SystemMetadataManager.getInstance().get(pid));
            assertEquals(nonAutoGenDocid,
                         SystemMetadataManager.getInstance().get(fakeId).getIdentifier()
                             .getValue());
            assertTrue(IdentifierManager.getInstance().mappingExists(fakeId.getValue()));
        } finally {
            oldDataFile.delete();
        }
    }
}
