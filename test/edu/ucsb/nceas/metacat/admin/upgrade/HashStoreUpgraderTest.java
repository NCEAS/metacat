package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.systemmetadata.ChecksumsManager;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;

/**
 * @author Tao
 * The test class for HashStoreUpgrader
 */
public class HashStoreUpgraderTest {
    MockedStatic<PropertyService> closeableMock;
    String backupPath = "build/temp." + System.currentTimeMillis();
    String hashStorePath = "build/hashStore";
    String documentPath = "test";
    String dataPath = "test/resources";

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        Properties withProperties = new Properties();
        withProperties.setProperty("application.datafilepath", dataPath);
        withProperties.setProperty("application.documentfilepath", documentPath);
        withProperties.setProperty("application.backupDir", backupPath);
        withProperties.setProperty("storage.hashstore.rootDirectory", hashStorePath);
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
        MetacatInitializer.initStorage();
    }

    @After
    public void tearDown() throws Exception {
        closeableMock.close();
    }

    /**
     * Test the method initCandidateList
     * @throws Exception
     */
    @Test
    public void testInitCandidateList() throws Exception {
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
        HashStoreUpgrader upgrader = new HashStoreUpgrader();
        ResultSet resultSet = upgrader.initCandidateList();
        boolean found = false;
        while (resultSet.next()) {
            String id = resultSet.getString(1);
            if (id.equals(pid.getValue())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    /**
     * Test the method initCandidateList which will omit the items which have records in the
     * checksum table
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
        HashStoreUpgrader upgrader = new HashStoreUpgrader();
        ResultSet resultSet = upgrader.initCandidateList();
        boolean found = false;
        while (resultSet.next()) {
            String id = resultSet.getString(1);
            if (id.equals(pid.getValue())) {
                found = true;
                break;
            }
        }
        assertFalse(found);
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
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta.setFormatId(formatId);
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
            Properties withProperties = new Properties();
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
        String localId = "eml-2.2.0.xml";
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
     * Test the upgrade method
     * @throws Exception
     */
    @Test
    public void testUpgradeData() throws Exception {
        String dataId = "HashStoreUpgraderTestUpgrade" + System.currentTimeMillis();
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
                Mockito.when(manager.get(pid)).thenReturn(sysmeta);
                Mockito.when(SystemMetadataManager.getInstance()).thenReturn(manager);
                // mock HashStoreUpgrader with the real methods
                HashStoreUpgrader upgrader = Mockito.mock(
                    HashStoreUpgrader.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
                // mock the initCandidate method
                Mockito.when(upgrader.initCandidateList())
                    .thenAnswer(invocation -> resultSetMock);
                upgrader.upgrade();
                File hashStoreRoot = new File(hashStorePath);
                assertTrue(hashStoreRoot.exists());
                assertNotNull(MetacatInitializer.getStorage().retrieveObject(pid));
                assertNotNull(MetacatInitializer.getStorage().retrieveMetadata(pid));
            }
        }
    }
}
