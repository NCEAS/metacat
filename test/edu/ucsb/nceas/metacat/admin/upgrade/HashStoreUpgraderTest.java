package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Tao
 * The test class for HashStoreUpgrader
 */
public class HashStoreUpgraderTest {

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        MetacatInitializer.initStorage();
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
            Mockito.when(IdentifierManager.getInstance())
                .thenReturn(mockManager);
            Properties withProperties = new Properties();
            withProperties.setProperty("application.documentfilepath", "test");
            try (MockedStatic<PropertyService> ignored
                     = LeanTestUtils.initializeMockPropertyService(withProperties)) {
                HashStoreUpgrader upgrader = new HashStoreUpgrader();
                Path path = upgrader.resolve(sysmeta);
                assertTrue(Files.exists(path));
                assertEquals(8724, Files.size(path));
            }
        }
    }

    /**
     * Test the resolve method for the data object
     * @throws Exception
     */
    @Test
    public void testResolveData() throws Exception {
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
        try (MockedStatic<IdentifierManager> ignore =
                 Mockito.mockStatic(IdentifierManager.class)) {
            IdentifierManager mockManager = Mockito.mock(IdentifierManager.class);
            Mockito.when(mockManager.getLocalId(dataId)).thenReturn(localId);
            Mockito.when(IdentifierManager.getInstance())
                .thenReturn(mockManager);
            Properties withProperties = new Properties();
            withProperties.setProperty("application.datafilepath", "test");
            try (MockedStatic<PropertyService> ignored
                     = LeanTestUtils.initializeMockPropertyService(withProperties)) {
                HashStoreUpgrader upgrader = new HashStoreUpgrader();
                Path path = upgrader.resolve(sysmeta);
                assertTrue(Files.exists(path));
                assertEquals(8724, Files.size(path));
            }
        }
    }
}
