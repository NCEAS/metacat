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
import org.dataone.service.types.v1.Checksum;
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
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
    ObjectFormatIdentifier eml220_format_id = new ObjectFormatIdentifier();


    @Before
    public void setUp() throws Exception {
        eml220_format_id.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        Properties withProperties = new Properties();
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
        closeableMock.close();
        File backupDir = new File(backupPath);
        backupDir.delete();
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
                assertEquals(0, upgrader.getInfo().length());
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
                assertEquals(0, upgrader.getInfo().length());
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
        String dataId = "testUpgradeWithIncorrectChecksum567"
            + System.currentTimeMillis();
        String localId = "eml-error-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new FileInputStream(new File(dataPath + "/" + localId));
        SystemMetadata sysmeta0 = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        sysmeta0.getChecksum().setValue("adfa");
        String metadataId = "testUpgradeWithIncorrectChecksum12"
            + System.currentTimeMillis();
        String metaLocalId = "eml-2.2.0.xml";
        Identifier metaPid = new Identifier();
        metaPid.setValue(metadataId);
        object = new FileInputStream(new File(documentPath + "/" + metaLocalId));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(metaPid, owner, object);
        sysmeta.setFormatId(eml220_format_id);
        sysmeta.getChecksum().setValue("edsf");
        // mock IdentifierManager
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataId)).thenReturn(metaLocalId);
            Mockito.when(mockManager.getLocalId(dataId)).thenReturn(localId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.getString(1)).thenReturn(metadataId).thenReturn(dataId);
            // mock only having one next
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(true).thenReturn(false);
            // mock getSystemMetadata
            try (MockedStatic<SystemMetadataManager> ignoredSysmeta =
                     Mockito.mockStatic(SystemMetadataManager.class)) {
                SystemMetadataManager manager = Mockito.mock(SystemMetadataManager.class);
                Mockito.when(manager.get(metaPid)).thenReturn(sysmeta);
                Mockito.when(manager.get(pid)).thenReturn(sysmeta0);
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
                assertTrue(upgrader.getInfo().contains("nonMatchingChecksum"));
                assertFalse(upgrader.getInfo().contains("general"));
                assertFalse(upgrader.getInfo().contains("noSuchAlgorithm"));
                assertFalse(upgrader.getInfo().contains("noChecksumInSysmeta"));
                assertFalse(upgrader.getInfo().contains("savingChecksumTableError"));
                Vector<String> content = readContentFromFileInDir();
                assertEquals(2, content.size());
                assertTrue(content.contains(dataId));
                assertTrue(content.contains(metadataId));
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
     * Test the upgrade method for both the metadata and data objects with the wrong checksum and
     * algorithm
     * @throws Exception
     */
    @Test
    public void testUpgradeWithIncorrectChecksumAndAlgorithm() throws Exception {
        String dataId = "testUpgradeWithIncorrectChecksumAndAlgorithm567"
            + System.currentTimeMillis();
        String localId = "eml-error-2.2.0.xml";
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue(dataId);
        InputStream object = new FileInputStream(new File(dataPath + "/" + localId));
        SystemMetadata sysmeta0 = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        sysmeta0.getChecksum().setValue("adfa");
        String metadataId = "testUpgradeWithIncorrectChecksumAndAlgorithm12"
            + System.currentTimeMillis();
        String metaLocalId = "eml-2.2.0.xml";
        Identifier metaPid = new Identifier();
        metaPid.setValue(metadataId);
        object = new FileInputStream(new File(documentPath + "/" + metaLocalId));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(metaPid, owner, object);
        sysmeta.setFormatId(eml220_format_id);
        sysmeta.getChecksum().setAlgorithm("edsf");
        // mock IdentifierManager
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(metadataId)).thenReturn(metaLocalId);
            Mockito.when(mockManager.getLocalId(dataId)).thenReturn(localId);
            Mockito.when(IdentifierManager.getInstance()).thenReturn(mockManager);
            // mock ResultSet
            ResultSet resultSetMock = Mockito.mock(ResultSet.class);
            Mockito.when(resultSetMock.getString(1)).thenReturn(metadataId).thenReturn(dataId);
            // mock only having one next
            Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(true).thenReturn(false);
            // mock getSystemMetadata
            try (MockedStatic<SystemMetadataManager> ignoredSysmeta =
                     Mockito.mockStatic(SystemMetadataManager.class)) {
                SystemMetadataManager manager = Mockito.mock(SystemMetadataManager.class);
                Mockito.when(manager.get(metaPid)).thenReturn(sysmeta);
                Mockito.when(manager.get(pid)).thenReturn(sysmeta0);
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
                assertTrue(upgrader.getInfo().contains("nonMatchingChecksum"));
                assertFalse(upgrader.getInfo().contains("general"));
                assertTrue(upgrader.getInfo().contains("noSuchAlgorithm"));
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


}