package edu.ucsb.nceas.metacat.systemmetadata;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.MediaType;
import org.dataone.service.types.v2.MediaTypeProperty;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.ServletContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;


public class SystemMetadataManagerIT {

    private D1NodeServiceTest d1NodeTester;
    private MockHttpServletRequest request;

    /**
     * Set up
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
        d1NodeTester = new D1NodeServiceTest("initialize");
        request = (MockHttpServletRequest)d1NodeTester.getServletRequest();
    }

    @After
    public void tearDown() throws Exception {
        SystemMetadataManager.getInstance().refreshInstance();
    }


    /**
     * Method to test new system metadata field such as media type and file name.
     * Also test the delete method.
     */
    @Test
    public void testMediaTypeAndDelete() throws Exception {
        String fileName = "new file name";
        String name = "text/plain";
        String p1Name = "charset";
        String p1Value = "UTF8";
        String p2Name = "n2";
        String p2Value = "v2";
        IdentifierManager im = IdentifierManager.getInstance();

        //test system metadata write/read without mediatype and file name.
        String docid = "test." + new Date().getTime() + ".1";
        String guid = "guid:" + docid;
        //create a mapping (identifier-docid)
        im.createMapping(guid, docid);
        Session session = d1NodeTester.getTestSession();
        Identifier id = new Identifier();
        id.setValue(guid);
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta =
                           D1NodeServiceTest.createSystemMetadata(id, session.getSubject(), object);
        SystemMetadataManager.lock(id);
        SystemMetadataManager.getInstance().store(sysmeta);
        SystemMetadataManager.unLock(id);
        SystemMetadata read = im.getSystemMetadata(guid);
        assertTrue(read.getIdentifier().equals(id));
        assertTrue(read.getFileName() == null);
        assertTrue(read.getMediaType() == null);
        //remove the system metadata
        SystemMetadataManager.lock(id);
        SystemMetadataManager.getInstance().delete(id);
        SystemMetadataManager.unLock(id);
        //remove the mapping
        im.removeMapping(guid, docid);

       //test system metadata write/read with mediatype and file name.
        Thread.sleep(1000);
        docid = "test." + new Date().getTime() + ".1";
        guid = "guid:" + docid;
        //create a mapping (identifier-docid)
        im.createMapping(guid, docid);
        id = new Identifier();
        id.setValue(guid);
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id, session.getSubject(), object);
        sysmeta.setFileName(fileName);
        MediaType media = new MediaType();
        media.setName(name);
        MediaTypeProperty p1 = new MediaTypeProperty();
        p1.setName(p1Name);
        p1.setValue(p1Value);
        media.addProperty(p1);
        MediaTypeProperty p2 = new MediaTypeProperty();
        p2.setName(p2Name);
        p2.setValue(p2Value);
        media.addProperty(p2);
        sysmeta.setMediaType(media);
        SystemMetadataManager.lock(id);
        SystemMetadataManager.getInstance().store(sysmeta);
        SystemMetadataManager.unLock(id);
        read = im.getSystemMetadata(guid);
        assertTrue(read.getIdentifier().equals(id));
        assertTrue(read.getFileName().equals(fileName));
        MediaType type = read.getMediaType();
        assertTrue(type.getName().equals(name));
        List<MediaTypeProperty> list = type.getPropertyList();
        assertTrue(list.size() == 2);
        MediaTypeProperty item1 = list.get(0);
        assertTrue(item1.getName().equals(p1Name));
        assertTrue(item1.getValue().equals(p1Value));
        MediaTypeProperty item2 = list.get(1);
        assertTrue(item2.getName().equals(p2Name));
        assertTrue(item2.getValue().equals(p2Value));

        //Thread.sleep(100000);
        //remove the system metadata
        SystemMetadata sys = SystemMetadataManager.getInstance().get(id);
        assertTrue("The system metadata should exist for " + id.getValue(),
                                            sys.getIdentifier().getValue().equals(id.getValue()));
        SystemMetadataManager.lock(id);
        SystemMetadataManager.getInstance().delete(id);
        SystemMetadataManager.unLock(id);
        sys = SystemMetadataManager.getInstance().get(id);
        assertNull("The system metadata should be null after deleted ", sys);
        SystemMetadataManager.lock(id);
        SystemMetadataManager.getInstance().delete(id);
        SystemMetadataManager.unLock(id);

        //remove the mapping
        im.removeMapping(guid, docid);
    }

    /**
     * Test the updateSystemMetadata method should throw an IvalidSystemMetadata exception 
     * if the permission is wrongly spelled. 
     * https://github.com/NCEAS/metacat/issues/1323
     * @throws Exception
     */
    @Test
    public void testUpdateSystemmetadata() throws Exception {
        String typoPermission = "typo";
        Session session = d1NodeTester.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(d1NodeTester.generateDocumentId());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        AccessPolicy policy = new AccessPolicy();
        AccessRule rule = new AccessRule();
        Subject subject = new Subject();
        subject.setValue("cn=test,dc=org");
        rule.addSubject(subject);
        rule.addPermission(Permission.convert(typoPermission));
        policy.addAllow(rule);
        SystemMetadata meta = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        meta.setAccessPolicy(policy);
        DBConnection dbConn = null;
        int serialNumber = 1;
        try {
            // get a connection from the pool
            dbConn = DBConnectionPool
                    .getDBConnection("Metacathandler.handleInsertOrUpdateAction");
            serialNumber = dbConn.getCheckOutSerialNumber();
            try {
                SystemMetadataManager.getInstance().updateSystemMetadata(meta, dbConn);
                fail("Can't get there since an InvalidSystemMetadata exception should be thrown.");
            } catch (InvalidSystemMetadata e) {
                assertTrue(e.getMessage().contains(typoPermission));
            }

        } finally {
            // Return db connection
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }

    }

    /**
     * Test the different scenarios to save new system metadata with the different/same
     * modification dates
     * @throws Exception
     */
    @Test
    public void testCheckVersions() throws Exception {
        Identifier guid = new Identifier();
        guid.setValue(d1NodeTester.generateDocumentId());
        Session session = d1NodeTester.getTestSession();
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        long originModificationDate = sysmeta.getDateSysMetadataModified().getTime();
        long originUploadDate = sysmeta.getDateUploaded().getTime();
        // false means not to change the modification date
        SystemMetadataManager.lock(guid);
        SystemMetadataManager.getInstance().store(sysmeta, false,
                                                      SystemMetadataManager.SysMetaVersion.CHECKED);
        SystemMetadataManager.unLock(guid);
        SystemMetadata storedSysmeta = SystemMetadataManager.getInstance().get(guid);
        assertEquals("The DateSysMetadataModified field shouldn't change", originModificationDate,
                                        storedSysmeta.getDateSysMetadataModified().getTime());
        assertEquals("The DateUploaded field shouldn't change", originUploadDate,
                                        storedSysmeta.getDateUploaded().getTime());
        // Reset a new modification date
        sysmeta.setDateSysMetadataModified(new Date());
        originModificationDate = sysmeta.getDateSysMetadataModified().getTime();
        try {
            // True means Metacat needs to change the modification date
            SystemMetadataManager.lock(guid);
            SystemMetadataManager.getInstance().store(sysmeta, true,
                    SystemMetadataManager.SysMetaVersion.CHECKED);
            fail("Test can't get here since the modification date in the new system metadata "
                  + " does not match the one in the system.");
        } catch (Exception e) {
            assertTrue( e instanceof InvalidRequest);
        } finally {
            SystemMetadataManager.unLock(guid);
        }
        // Skip checking version will make the save method work
        // True means Metacat needs to change the modification date
        SystemMetadataManager.lock(guid);
        SystemMetadataManager.getInstance().store(sysmeta, true,
                                            SystemMetadataManager.SysMetaVersion.UNCHECKED);
        SystemMetadataManager.unLock(guid);
        storedSysmeta = SystemMetadataManager.getInstance().get(guid);
        assertNotEquals("The DateSysMetadataModified field should change.", originModificationDate,
                                        storedSysmeta.getDateSysMetadataModified().getTime());
        assertEquals("The DateUploaded field shouldn't change", originUploadDate,
                                        storedSysmeta.getDateUploaded().getTime());
    }

    /**
     * Test the rollback feature for creating
     * @throws Exception
     */
    @Test
    public void testRollBackCreate() throws Exception {
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testUpdate-" + System.currentTimeMillis());
        InputStream object =
            new ByteArrayInputStream("testtestRollBackCreate".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        //Mock the store method failed when Metacat does a database commit
        try (MockedStatic<DBConnectionPool> mockDbConnPool =
                 Mockito.mockStatic(DBConnectionPool.class)) {
            DBConnection mockConnection = Mockito.mock(
                DBConnection.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            Mockito.when(DBConnectionPool.getDBConnection(any(String.class)))
                .thenReturn(mockConnection);
            Mockito.doThrow(SQLException.class).when(mockConnection).commit();
            try {
                SystemMetadataManager.lock(pid);
                SystemMetadataManager.getInstance().store(sysmeta);
                fail("Test shouldn't get there since the above method should throw an exception");
            } catch (Exception e) {
                assertTrue("It should be a ServiceFailure exception.", e instanceof ServiceFailure);
            } finally {
                SystemMetadataManager.unLock(pid);
            }
        }
        // The system metadata read from db should be null
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertNull("The systemmetadata for pid " + pid.getValue() + " should be null", readSys);
        try {
            // Reading system metadata from hashstore should throw a FileNotFoundException
            D1NodeServiceTest.getStorage().retrieveMetadata(pid);
            fail("Test can't reach here since the pid should be removed.");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
    }

    /**
     * Test the rollback feature for updating
     * @throws Exception
     */
    @Test
    public void testRollBackUpdate() throws Exception {
        // First to create system metadata successfully
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("MetacatHandler.testUpdate-" + System.currentTimeMillis());
        InputStream object =
            new ByteArrayInputStream("testtestRollBackCreate".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        SystemMetadataManager.lock(pid);
        SystemMetadataManager.getInstance().store(sysmeta);
        SystemMetadataManager.unLock(pid);
        // Preserve the pid's system metadata
        SystemMetadata originalPidMeta = SystemMetadataManager.getInstance().get(pid);
        Date dateUploaded = originalPidMeta.getDateUploaded();
        Date dateModified = originalPidMeta.getDateSysMetadataModified();
        BigInteger version = originalPidMeta.getSerialVersion();
        // The system metadata read from db should not be null
        SystemMetadata readSys = SystemMetadataManager.getInstance().get(pid);
        assertNotNull("The systemmetadata for pid " + pid.getValue() + " should be null", readSys);
        // Reading system metadata from hashstore and compare it to the database one
        InputStream metaInput = D1NodeServiceTest.getStorage().retrieveMetadata(pid);
        SystemMetadata sysmetaFromHash =
            TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, metaInput);
        assertNull(sysmetaFromHash.getObsoletedBy());
        assertEquals(dateUploaded.getTime(), sysmetaFromHash.getDateUploaded().getTime());
        assertEquals(dateModified.getTime(), sysmetaFromHash.getDateSysMetadataModified().getTime());
        assertEquals(version.longValue(), sysmetaFromHash.getSerialVersion().longValue());
        MCSystemMetadataTest.compareValues(originalPidMeta, sysmetaFromHash);

        //Mock a failed update
        Identifier obsoleteById = new Identifier();
        obsoleteById.setValue("foo");
        readSys.setObsoletedBy(obsoleteById);
        readSys.setSerialVersion(BigInteger.TEN);
        assertEquals("foo", readSys.getObsoletedBy().getValue());
        assertNull(originalPidMeta.getObsoletedBy());
        assertEquals(BigInteger.TEN.intValue(), readSys.getSerialVersion().intValue());
        assertEquals(version.intValue(), originalPidMeta.getSerialVersion().intValue());
        assertNotEquals(version.intValue(), BigInteger.TEN.intValue());
        assertEquals(dateModified.getTime(), readSys.getDateSysMetadataModified().getTime());
        assertEquals(dateUploaded.getTime(), readSys.getDateUploaded().getTime());
        assertEquals(dateModified.getTime(), originalPidMeta.getDateSysMetadataModified().getTime());
        assertEquals(dateUploaded.getTime(), originalPidMeta.getDateUploaded().getTime());
        try (MockedStatic<DBConnectionPool> mockDbConnPool =
                 Mockito.mockStatic(DBConnectionPool.class)) {
            DBConnection mockConnection = Mockito.mock(DBConnection.class,
                                                       withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
            Mockito.when(DBConnectionPool.getDBConnection(any(String.class)))
                .thenReturn(mockConnection);
            Mockito.doThrow(SQLException.class).when(mockConnection).commit();
            try {
                SystemMetadataManager.lock(pid);
                SystemMetadataManager.getInstance().store(readSys);
                fail("Test shouldn't get there since the above method should throw an exception");
            } catch (Exception e) {
                assertTrue("It should be a ServiceFailure exception.", e instanceof ServiceFailure);
            } finally {
                SystemMetadataManager.unLock(pid);
            }
        }
        // The failure change nothing.
        // Make sure there are no changes on the system metadata of pid from db
        assertNull(originalPidMeta.getObsoletedBy());
        SystemMetadata readAgain = SystemMetadataManager.getInstance().get(pid);
        assertNull(readAgain.getObsoletedBy());
        assertEquals(dateUploaded.getTime(), readAgain.getDateUploaded().getTime());
        assertEquals(dateModified.getTime(), readAgain.getDateSysMetadataModified().getTime());
        assertEquals(version.longValue(), readAgain.getSerialVersion().longValue());
        assertNotEquals(BigInteger.TEN, readAgain.getSerialVersion().longValue());
        MCSystemMetadataTest.compareValues(originalPidMeta, readAgain);
        // Make sure there are no changes on the system metadata of pid from hashstore
        SystemMetadata sysmetaFromHash2;
        try (InputStream metaInput2 = D1NodeServiceTest.getStorage().retrieveMetadata(pid)) {
            sysmetaFromHash2 = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                                                                      metaInput2);
        }
        assertNull(sysmetaFromHash2.getObsoletedBy());
        assertEquals(dateUploaded.getTime(), sysmetaFromHash2.getDateUploaded().getTime());
        assertEquals(dateModified.getTime(), sysmetaFromHash2.getDateSysMetadataModified().getTime());
        assertEquals(version.intValue(), sysmetaFromHash2.getSerialVersion().intValue());
        assertNotEquals(BigInteger.TEN.intValue(), sysmetaFromHash2.getSerialVersion().intValue());
        MCSystemMetadataTest.compareValues(originalPidMeta, sysmetaFromHash2);

    }

    /**
     * Test the case ckeckLock == true. The store method will fail if we don't call the lock method
     * before it.
     * @throws Exception
     */
    @Test
    public void testCheckLock() throws Exception {
        try {
            String user = "http://orcid.org/1234/4567";
            Subject owner = new Subject();
            owner.setValue(user);
            Identifier pid = new Identifier();
            pid.setValue("SystemMetadataManager.testCheckLock-" + System.currentTimeMillis());
            InputStream object =
                new ByteArrayInputStream("testCheckLock".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
            SystemMetadataManager.getInstance().store(sysmeta);
        } catch (Exception e) {
            assertTrue(e instanceof ServiceFailure);
            assertTrue(e.getMessage().contains("lock"));
        }
    }

    /**
     * Test the case that ckeckLock == false. Even without calling SystemMetadataManager.lock,
     * the store method should work since we set the checkLock value false.
     * @throws Exception
     */
    @Test
    public void testCheckLockFalse() throws Exception {
        String user = "http://orcid.org/1234/4567";
        Subject owner = new Subject();
        owner.setValue(user);
        Identifier pid = new Identifier();
        pid.setValue("SystemMetadataManager.testCheckLockFalse-" + System.currentTimeMillis());
        InputStream object =
            new ByteArrayInputStream("testCheckLockFalse".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, owner, object);
        Properties withProperties = new Properties();
        SystemMetadataManager.getInstance().refreshInstance();
        withProperties.setProperty("systemMetadataManager.checkLock", "false");
        try (MockedStatic<PropertyService> mock = LeanTestUtils.initializeMockPropertyService(
            withProperties)) {
            mock.when(() -> PropertyService.getInstance((ServletContext) any()))
                .thenReturn(Mockito.mock(PropertyService.class));
            SystemMetadataManager.getInstance().store(sysmeta);
        }
    }

}
