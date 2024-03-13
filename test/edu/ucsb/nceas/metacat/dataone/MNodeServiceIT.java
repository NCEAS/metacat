package edu.ucsb.nceas.metacat.dataone;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.admin.upgrade.UpdateDOI;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.doi.DOIServiceFactory;
import edu.ucsb.nceas.metacat.doi.ezid.EzidDOIService;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.index.queue.FailedIndexResubmitTimerTaskIT;
import edu.ucsb.nceas.metacat.object.handler.JsonLDHandlerTest;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.metacat.util.AuthUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.ServiceMethodRestriction;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v2.Property;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.dspace.foresite.ResourceMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;


/**
 * A JUnit test to exercise the Metacat Member Node service implementation. This also tests a few of
 * the D1NodeService superclass methods
 *
 * @author cjones
 */
public class MNodeServiceIT {

    private static final String TEST_MN_SUBJECT = "CN=urn:node:METACAT1,DC=dataone,DC=org";
    private static String unmatchingEncodingFilePath = "test/incorrect-encoding-declaration.xml";
    private static final String CN_BASE_URL = "https://cn.dataone.org/cn";
    private static final String invalidEmlPath1 = "test/resources/eml-error-2.2.0.xml";
    private static final String invalidEmlPath2 = "test/resources/eml-error.xml";

    private D1NodeServiceTest d1NodeTest = null;
    private MockHttpServletRequest request = null;

    /**
     * Set up the test fixtures
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
        d1NodeTest = new D1NodeServiceTest("initialize");
        d1NodeTest.setUp();
        request = (MockHttpServletRequest)d1NodeTest.getServletRequest();
        Settings.getConfiguration().clearProperty("D1Client.CN_URL");
        Settings.getConfiguration().addProperty("D1Client.CN_URL", CN_BASE_URL);
        // set up the configuration for d1client
        Settings.getConfiguration().setProperty("D1Client.cnClassName", MockCNode.class.getName());
    }

    /**
     * Remove the test fixtures
     */
    @After
    public void tearDown() {
        d1NodeTest.tearDown();
    }


    /**
     * Test getting a known object
     */
    @Test
    public void testGet() {
        D1NodeServiceTest.printTestHeader("testGet");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testGet." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            InputStream result = MNodeService.getInstance(request).get(session, guid);
            // go back to beginning of original stream
            object.reset();
            // check
            assertTrue(object.available() > 0);
            assertTrue(result.available() > 0);
            assertTrue(IOUtils.contentEquals(result, object));

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());

        }
    }

    /**
     * Test getting the system metadata of an object
     */
    @Test
    public void testGetSystemMetadata() {
        D1NodeServiceTest.printTestHeader("testGetSystemMetadata");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testGetSystemMetadata." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            SystemMetadata newsysmeta =
                MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertEquals(newsysmeta.getIdentifier().getValue(), sysmeta.getIdentifier().getValue());
            assertEquals(newsysmeta.getSeriesId(), null);

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }

    }

    /**
     * Test object creation
     */
    @Test
    public void testCreate() {
        D1NodeServiceTest.printTestHeader("testCreate");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertEquals(guid.getValue(), pid.getValue());

            Thread.sleep(1000);
            try {
                Identifier guid2 = new Identifier();
                guid2.setValue("testCreate." + System.currentTimeMillis());
                SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object);
                sysmeta2.setSeriesId(guid);
                MNodeService.getInstance(request).create(session, guid2, object, sysmeta2);
                fail("It should fail since the system metadata using an existing id as the sid");
            } catch (InvalidSystemMetadata ee) {

            }

            Thread.sleep(1000);
            try {
                Identifier guid3 = new Identifier();
                guid3.setValue("testCreate." + System.currentTimeMillis());
                SystemMetadata sysmeta3 = D1NodeServiceTest.createSystemMetadata(guid3, session.getSubject(), object);
                sysmeta3.setSeriesId(guid3);
                MNodeService.getInstance(request).create(session, guid3, object, sysmeta3);
                fail("It should fail since the system metadata using the pid as the sid");
            } catch (InvalidSystemMetadata ee) {

            }
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test object creation
     */
    @Test
    public void testMissMatchChecksumInCreate() {
        D1NodeServiceTest.printTestHeader("testMissMatchChecksumInCreate");
        Identifier guid = new Identifier();
        guid.setValue("testCreate." + System.currentTimeMillis());
        Session session = null;
        try {
            session = d1NodeTest.getTestSession();
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Node localNode = MNodeService.getInstance(request).getCapabilities();
            ReplicationPolicy rePolicy = new ReplicationPolicy();
            rePolicy.setReplicationAllowed(true);
            rePolicy.setNumberReplicas(3);
            rePolicy.addPreferredMemberNode(localNode.getIdentifier());
            sysmeta.setReplicationPolicy(rePolicy);
            Checksum checksum = new Checksum();
            checksum.setAlgorithm("md5");
            checksum.setValue("098F6BCD4621D373CADE4E832627B4F9");
            sysmeta.setChecksum(checksum);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("It should fail since the checksum doesn't match.");
        } catch (InvalidSystemMetadata ee) {
            try {
                MNodeService.getInstance(request).getSystemMetadata(session, guid);
                fail("We shouldn't get here since the guid " + guid.getValue() + " was deleted.");
            } catch (NotFound e) {
                //here is okay.
            } catch (Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
            try {
                assertFalse(IdentifierManager.getInstance().identifierExists(guid.getValue()));
            } catch (Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }


    /**
     * Test miss-match checksum for metacat object.
     */
    @Test
    public void testMissMatchMetadataCreate() {
        D1NodeServiceTest.printTestHeader("testMissMatchMetadataCreate");
        Identifier guid = new Identifier();
        guid.setValue("testCreate." + System.currentTimeMillis());
        Session session = null;
        try {
            session = d1NodeTest.getTestSession();
            InputStream object =
                new FileInputStream(MockReplicationMNode.replicationSourceFile);
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
            sysmeta.setFormatId(formatId);
            Node localNode = MNodeService.getInstance(request).getCapabilities();
            ReplicationPolicy rePolicy = new ReplicationPolicy();
            rePolicy.setReplicationAllowed(true);
            rePolicy.setNumberReplicas(3);
            rePolicy.addPreferredMemberNode(localNode.getIdentifier());
            sysmeta.setReplicationPolicy(rePolicy);
            Checksum checksum = new Checksum();
            checksum.setAlgorithm("md5");
            checksum.setValue("098F6BCD4621D373CADE4E832627B4F9");
            sysmeta.setChecksum(checksum);
            object = new FileInputStream(MockReplicationMNode.replicationSourceFile);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("It should fail since the checksum doesn't match.");
        } catch (InvalidSystemMetadata ee) {
            try {
                MNodeService.getInstance(request).getSystemMetadata(session, guid);
                fail("We shouldn't get here since the guid " + guid.getValue() + " was deleted.");
            } catch (NotFound e) {
                //here is okay.
            } catch (Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
            try {
                assertFalse(IdentifierManager.getInstance().identifierExists(guid.getValue()));
            } catch (Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * test object deletion
     */
    @Test
    public void testDelete() {
        D1NodeServiceTest.printTestHeader("testDelete");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testDelete." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);

            // use MN admin to delete
            session = d1NodeTest.getMNSession();
            Identifier deletedPid = MNodeService.getInstance(request).delete(session, pid);
            assertEquals(pid.getValue(), deletedPid.getValue());
            // check that we cannot get the object
            session = d1NodeTest.getTestSession();
            InputStream deletedObject = null;
            try {
                deletedObject = MNodeService.getInstance(request).get(session, deletedPid);
            } catch (NotFound nf) {
                // this is expected
            }
            assertNull(deletedObject);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());

        }

    }

    /**
     * Test the archive method
     * @throws Exception
     */
    @Test
    public void testArchive() throws Exception {
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        MNodeService.getInstance(request).archive(session, guid);
        SystemMetadata result = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertTrue(result.getArchived());
        System.out.println("the identifier is ===================" + pid.getValue());

        //test to archive an obsoleted object
        Identifier guid1 = new Identifier();
        guid1.setValue("testArchive." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid1, session.getSubject(), object);
        sysmeta.setArchived(false);
        MNodeService.getInstance(request).create(session, guid1, object, sysmeta);

        Identifier guid2 = new Identifier();
        guid2.setValue("testArchive2." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata newSysMeta = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object);
        newSysMeta.setObsoletes(guid1);
        newSysMeta.setArchived(false);
        MNodeService.getInstance(request).update(session, guid1, object, guid2, newSysMeta);
        System.out.println("The object " + guid1.getValue() + " has been updated by the object "
                               + guid2.getValue());
        MNodeService.getInstance(request).archive(session, guid1);
        SystemMetadata sys1 = MNodeService.getInstance(request).getSystemMetadata(session, guid1);
        assertEquals(sys1.getIdentifier(), guid1);
        assertTrue((boolean) sys1.getArchived());
        SystemMetadata sys2 = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        assertEquals(sys2.getIdentifier(), guid2);
        assertFalse((boolean) sys2.getArchived());


        //test to archive an obsoleted object again (by use the updateSystemMetadata method)
        Identifier guid3 = new Identifier();
        guid3.setValue("testArchive3." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid3, session.getSubject(), object);
        sysmeta.setArchived(false);
        MNodeService.getInstance(request).create(session, guid3, object, sysmeta);

        Identifier guid4 = new Identifier();
        guid4.setValue("testArchive4." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        newSysMeta = D1NodeServiceTest.createSystemMetadata(guid4, session.getSubject(), object);
        newSysMeta.setObsoletes(guid3);
        newSysMeta.setArchived(false);
        MNodeService.getInstance(request).update(session, guid3, object, guid4, newSysMeta);
        System.out.println("The object " + guid3.getValue() + " has been updated by the object "
                               + guid4.getValue());
        SystemMetadata sysFromServer =
            MNodeService.getInstance(request).getSystemMetadata(session, guid3);
        sysFromServer.setArchived(true);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid3, sysFromServer);
        SystemMetadata sys3 = MNodeService.getInstance(request).getSystemMetadata(session, guid3);
        assertEquals(sys3.getIdentifier(), guid3);
        assertTrue(sys3.getArchived());
        SystemMetadata sys4 = MNodeService.getInstance(request).getSystemMetadata(session, guid4);
        assertEquals(sys4.getIdentifier(), guid4);
        assertFalse(sys4.getArchived());
    }

    /**
     * Test object updating
     */
    @Test
    public void testUpdate() {
        D1NodeServiceTest.printTestHeader("testUpdate");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testUpdate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier newPid = new Identifier();
            newPid.setValue(
                "testUpdate." + (System.currentTimeMillis() + 1)); // ensure it is different from
            // original
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);

            //test the case that the new pid doesn't match the identifier on the system metadata
            Identifier newPid_6 = new Identifier();
            newPid_6.setValue("testUpdateFailed." + System.currentTimeMillis());
            Identifier newPid_7 = new Identifier();
            newPid_7.setValue("testUpdateFailedAnother." + System.currentTimeMillis());
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata newMeta = D1NodeServiceTest.createSystemMetadata(newPid_7, session.getSubject(), object);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            try {
                MNodeService.getInstance(request).update(session, pid, object, newPid_6, newMeta);
                fail("we shouldn't get here since the new pid doesn't match the identifier on the "
                         + "system metadata in the update method");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(newPid_6.getValue()));
                assertTrue(e.getMessage().contains(newPid_7.getValue()));
            }

            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata newSysMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
            newSysMeta.setArchived(true);
            System.out.println("the pid is =======!!!!!!!!!!!! " + pid.getValue());
            // do the update
            Identifier updatedPid =
                MNodeService.getInstance(request).update(session, pid, object, newPid, newSysMeta);

            // get the updated system metadata
            SystemMetadata updatedSysMeta =
                MNodeService.getInstance(request).getSystemMetadata(session, updatedPid);

            assertEquals(updatedPid.getValue(), newPid.getValue());

            //try to update an archived object and need to get an exception
            Identifier newPid2 = new Identifier();
            newPid2.setValue(
                "testUpdate." + (System.currentTimeMillis() + 2)); // ensure different from original
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata newSysMeta2 =
                D1NodeServiceTest.createSystemMetadata(newPid2, session.getSubject(), object);
            try {
                updatedPid = MNodeService.getInstance(request)
                    .update(session, newPid, object, newPid2, newSysMeta2);
                fail("update an archived object should get an invalid request exception");
            } catch (Exception ee) {
                assertTrue(ee instanceof InvalidRequest);
            }


            //update the authoritative node on the existing pid (newPid)
            SystemMetadata meta =
                MNodeService.getInstance(request).getSystemMetadata(session, newPid);
            BigInteger version = meta.getSerialVersion();
            version = version.add(BigInteger.ONE);
            newSysMeta.setSerialVersion(version);
            NodeReference newMN = new NodeReference();
            newMN.setValue("urn:node:river1");
            newSysMeta.setAuthoritativeMemberNode(newMN);
            newSysMeta.setArchived(false);
            try {
                MNodeService.getInstance(request).updateSystemMetadata(session, newPid, newSysMeta);
            } catch (InvalidRequest ee) {
                assertTrue(ee.getMessage().contains("urn:node:river1"));
            }

            try {
                updatedPid = MNodeService.getInstance(request)
                    .update(session, newPid, object, newPid2, newSysMeta2);
                fail("update an object on non-authoritative node should get an exception");
            } catch (Exception ee) {
                assertTrue(ee instanceof InvalidRequest);
            }
            //cn can succeed even though it updates an object on the non-authoritative node.
            Session cnSession = d1NodeTest.getCNSession();
            try {
                updatedPid = MNodeService.getInstance(request)
                    .update(cnSession, newPid, object, newPid2, newSysMeta2);
                fail("updating an object's authoritative node should get an exception");
                //assertEquals(updatedPid.getValue(), newPid2.getValue());
            } catch (InvalidRequest ee) {
                //assertTrue(ee.getMessage().contains(newPid.getValue()));
            }

            //test update an object with new access rules
            Subject write = new Subject();
            write.setValue("Write");
            Session writeSession = new Session();
            writeSession.setSubject(write);
            Subject change = new Subject();
            change.setValue("Change");
            Session changeSession = new Session();
            changeSession.setSubject(change);

            Identifier guid20 = new Identifier();
            guid20.setValue("testUpdatewithAccessChange." + System.currentTimeMillis());
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(guid20, session.getSubject(), object);
            AccessRule writeRule = new AccessRule();
            writeRule.addSubject(write);
            writeRule.addPermission(Permission.WRITE);
            sysmeta.getAccessPolicy().addAllow(writeRule);
            AccessRule changeRule = new AccessRule();
            changeRule.addSubject(change);
            changeRule.addPermission(Permission.CHANGE_PERMISSION);
            sysmeta.getAccessPolicy().addAllow(changeRule);
            MNodeService.getInstance(request).create(session, guid20, object, sysmeta);

            //the write user fails to update the object since it modified the access rules of the
            // original one
            Identifier guid21 = new Identifier();
            guid21.setValue("testUpdatewithAccessChange2." + System.currentTimeMillis());
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            updatedSysMeta = D1NodeServiceTest.createSystemMetadata(guid21, session.getSubject(), object);
            try {
                MNodeService.getInstance(request)
                    .update(writeSession, guid20, object, guid21, updatedSysMeta);
                fail("The write-permission-only user can't change the access rules");
            } catch (Exception ee) {
                assertTrue(ee instanceof NotAuthorized);
            }

            //the write user fails to update the object since it modified the rights holder (need
            // the change permission)
            guid21 = new Identifier();
            guid21.setValue("testUpdatewithAccessChange21." + System.currentTimeMillis());
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            updatedSysMeta = D1NodeServiceTest.createSystemMetadata(guid21, session.getSubject(), object);
            updatedSysMeta.getAccessPolicy().addAllow(writeRule);
            updatedSysMeta.getAccessPolicy().addAllow(changeRule);
            Subject newRightsHolder = new Subject();
            newRightsHolder.setValue("foo");
            updatedSysMeta.setRightsHolder(newRightsHolder);
            try {
                MNodeService.getInstance(request)
                    .update(writeSession, guid20, object, guid21, updatedSysMeta);
                fail("The write-permission-only user can't change the rights holder");
            } catch (Exception ee) {
                assertTrue(ee instanceof NotAuthorized);
            }

            //the write user can update the object without modifying access rules
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            updatedSysMeta = D1NodeServiceTest.createSystemMetadata(guid21, session.getSubject(), object);
            updatedSysMeta.getAccessPolicy().addAllow(writeRule);
            updatedSysMeta.getAccessPolicy().addAllow(changeRule);
            MNodeService.getInstance(request)
                .update(writeSession, guid20, object, guid21, updatedSysMeta);

            //the change user can update the object even with the modified access rules
            Identifier guid22 = new Identifier();
            guid22.setValue("testUpdatewithAccessChange3." + System.currentTimeMillis());
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            updatedSysMeta = D1NodeServiceTest.createSystemMetadata(guid22, session.getSubject(), object);
            updatedSysMeta.getAccessPolicy().addAllow(changeRule);
            MNodeService.getInstance(request)
                .update(changeSession, guid21, object, guid22, updatedSysMeta);

            //the change user can update the rights holder
            Identifier guid23 = new Identifier();
            guid23.setValue("testUpdatewithAccessChange4." + System.currentTimeMillis());
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            updatedSysMeta = D1NodeServiceTest.createSystemMetadata(guid23, session.getSubject(), object);
            updatedSysMeta.getAccessPolicy().addAllow(changeRule);
            updatedSysMeta.setRightsHolder(newRightsHolder);
            MNodeService.getInstance(request)
                .update(changeSession, guid22, object, guid23, updatedSysMeta);

            //test update an object with modified authoritative member node
            guid.setValue("testUpdate2." + System.currentTimeMillis());
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            newPid = new Identifier();
            newPid.setValue(
                "testUpdate3." + (System.currentTimeMillis() + 1)); //ensure different from original
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata newSysmeta321 =
                D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
            NodeReference node = new NodeReference();
            node.setValue("foo");
            newSysmeta321.setAuthoritativeMemberNode(node);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            try {
                MNodeService.getInstance(request)
                    .update(session, guid, object, newPid, newSysmeta321);
                fail("It can't reach here since it tried to update an object with different "
                         + "authoritative member node");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
            }
            //test when the authoritative member node to be null
            newSysmeta321.setAuthoritativeMemberNode(null);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            MNodeService.getInstance(request).update(session, guid, object, newPid, newSysmeta321);
            SystemMetadata retrive1 =
                MNodeService.getInstance(request).getSystemMetadata(session, newPid);
            NodeReference originMemberNode =
                MNodeService.getInstance(request).getCapabilities().getIdentifier();
            assertEquals(retrive1.getAuthoritativeMemberNode().getValue(),
                         originMemberNode.getValue());

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test object updating
     */
    @Test
    public void testMissMatchedCheckSumUpdate() {
        D1NodeServiceTest.printTestHeader("testMissMatchedCheckSumUpdate");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testUpdate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            System.out.println("========= the old pid is " + guid.getValue());
            Identifier newPid = new Identifier();
            newPid.setValue(
                "testUpdate." + (System.currentTimeMillis() + 1)); // ensure different from original
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);

            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata newSysMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
            Checksum checksum = newSysMeta.getChecksum();
            checksum.setValue("foo-checksum");
            newSysMeta.setChecksum(checksum);
            System.out.println("========= the new pid is " + newPid.getValue());
            // do the update and it should fail
            try {
                MNodeService.getInstance(request).update(session, pid, object, newPid, newSysMeta);
                fail("we shouldn't get here since the checksum is wrong");
            } catch (InvalidSystemMetadata ee) {
                try {
                    MNodeService.getInstance(request).getSystemMetadata(session, newPid);
                    fail("we shouldn't get here since the newPid " + newPid.getValue()
                             + " shouldn't be created.");
                } catch (NotFound eeee) {

                }

            } catch (Exception eee) {
                eee.printStackTrace();
                fail("Unexpected error in the update: " + eee.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());

        }
    }


    /**
     * Test object updating
     */
    @Test
    public void testMissMatchedChecksumUpdateSciMetadata() {
        D1NodeServiceTest.printTestHeader("testMissMatchedChecksumUpdateSciMetadata");

        try {
            String st1 =
                "<eml:eml xmlns:eml=\"eml://ecoinformatics.org/eml-2.1.1\" xmlns:xsi=\"http://www"
                    + ".w3.org/2001/XMLSchema-instance\" packageId=\"tao.13397.1\" system=\"knb\" "
                    + "xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.1.1 eml.xsd\">"
                    + "<access" + " authSystem=\"knb\" order=\"allowFirst\">"
                    + "<allow><principal>public"
                    + "</principal><permission>read</permission></allow></access>" + "<dataset"
                    + "><title>test</title><creator id=\"1445543475577\"><individualName><surName"
                    + ">test</surName></individualName></creator>" + "<contact id=\"1445543479900"
                    + "\"><individualName><surName>test</surName></individualName></contact"
                    + "></dataset></eml:eml>";
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testUpdate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(st1.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(
                ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.1")
                    .getFormatId());
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            System.out.println("=================the old pid is " + guid.getValue());

            String st2 =
                "<eml:eml xmlns:eml=\"eml://ecoinformatics.org/eml-2.1.1\" xmlns:xsi=\"http://www"
                    + ".w3.org/2001/XMLSchema-instance\" packageId=\"tao.13397.1\" system=\"knb\" "
                    + "xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.1.1 eml.xsd\">"
                    + "<access" + " authSystem=\"knb\" order=\"allowFirst\">"
                    + "<allow><principal>public"
                    + "</principal><permission>read</permission></allow></access>" + "<dataset"
                    + "><title>test2</title><creator "
                    + "id=\"1445543475577\"><individualName><surName>test</surName"
                    + "></individualName></creator>" + "<contact id=\"1445543479900"
                    + "\"><individualName><surName>test</surName></individualName></contact"
                    + "></dataset></eml:eml>";
            Identifier newPid = new Identifier();
            newPid.setValue(
                "testUpdate." + (System.currentTimeMillis() + 1)); // ensure different from original
            System.out.println("=================the new pid is " + newPid.getValue());
            object = new ByteArrayInputStream(st2.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
            sysmeta2.setFormatId(
                ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.1")
                    .getFormatId());
            sysmeta2.setObsoletes(guid);
            Checksum sum1 = sysmeta2.getChecksum();
            sum1.setValue("foo checksum");
            sysmeta2.setChecksum(sum1);
            object = new ByteArrayInputStream(st2.getBytes(StandardCharsets.UTF_8));
            // do the update and it should fail
            try {
                MNodeService.getInstance(request).update(session, guid, object, newPid, sysmeta2);
                fail("we shouldn't get here since the checksum is wrong");
            } catch (InvalidSystemMetadata ee) {
                try {
                    MNodeService.getInstance(request).getSystemMetadata(session, newPid);
                    fail("we shouldn't get here since the newPid " + newPid.getValue()
                             + " shouldn't be created.");
                } catch (NotFound eeee) {
                    // Do nothing
                }
            } catch (Exception eee) {
                fail("Unexpected error in the update: " + eee.getMessage());
            }
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());

        }
    }


    /**
     * Test object updating
     */
    @Test
    public void testUpdateSciMetadata() {
        D1NodeServiceTest.printTestHeader("testUpdate");

        try {
            String st1 =
                "<eml:eml xmlns:eml=\"eml://ecoinformatics.org/eml-2.1.1\" xmlns:xsi=\"http://www"
                    + ".w3.org/2001/XMLSchema-instance\" packageId=\"tao.13397.1\" system=\"knb\" "
                    + "xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.1.1 eml.xsd\">"
                    + "<access" + " authSystem=\"knb\" order=\"allowFirst\">"
                    + "<allow><principal>public"
                    + "</principal><permission>read</permission></allow></access>" + "<dataset"
                    + "><title>test</title><creator id=\"1445543475577\"><individualName><surName"
                    + ">test</surName></individualName></creator>" + "<contact id=\"1445543479900"
                    + "\"><individualName><surName>test</surName></individualName></contact"
                    + "></dataset></eml:eml>";
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testUpdate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(st1.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(
                ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.1")
                    .getFormatId());
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);

            String st2 =
                "<eml:eml xmlns:eml=\"eml://ecoinformatics.org/eml-2.1.1\" xmlns:xsi=\"http://www"
                    + ".w3.org/2001/XMLSchema-instance\" packageId=\"tao.13397.1\" system=\"knb\" "
                    + "xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.1.1 eml.xsd\">"
                    + "<access" + " authSystem=\"knb\" order=\"allowFirst\">"
                    + "<allow><principal>public"
                    + "</principal><permission>read</permission></allow></access>" + "<dataset"
                    + "><title>test2</title><creator "
                    + "id=\"1445543475577\"><individualName><surName>test</surName"
                    + "></individualName></creator>" + "<contact id=\"1445543479900"
                    + "\"><individualName><surName>test</surName></individualName></contact"
                    + "></dataset></eml:eml>";
            Identifier newPid = new Identifier();
            newPid.setValue(
                "testUpdate." + (System.currentTimeMillis() + 1)); // ensure different from original
            System.out.println("=================the pid is " + newPid.getValue());
            object = new ByteArrayInputStream(st2.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
            sysmeta2.setFormatId(
                ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.1")
                    .getFormatId());
            sysmeta2.setObsoletes(guid);
            Checksum sum1 = sysmeta2.getChecksum();
            System.out.println("the checksum before sending is " + sum1.getValue());
            object = new ByteArrayInputStream(st2.getBytes(StandardCharsets.UTF_8));
            MNodeService.getInstance(request).update(session, guid, object, newPid, sysmeta2);
            SystemMetadata meta =
                MNodeService.getInstance(request).getSystemMetadata(session, newPid);
            System.out.println(
                "the checksum getting from the server is " + meta.getChecksum().getValue());
            assertEquals(meta.getChecksum().getValue(), sum1.getValue());
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test the replicate method. The getReplica method is from a MockMN.
     */
    @Test
    public void testReplicate() {
        D1NodeServiceTest.printTestHeader("testReplicate");
        try {
            Session session = d1NodeTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testReplicate." + System.currentTimeMillis());
            System.out.println(
                "======================the id need to be replicated is " + guid.getValue());
            InputStream object =
                new FileInputStream(MockReplicationMNode.replicationSourceFile);
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
            sysmeta.setFormatId(formatId);
            NodeReference sourceNode = new NodeReference();
            sourceNode.setValue(MockReplicationMNode.NODE_ID);
            sysmeta.setAuthoritativeMemberNode(sourceNode);
            sysmeta.setOriginMemberNode(sourceNode);
            boolean result = false;
            result = MNodeService.getInstance(request).replicate(session, sysmeta, sourceNode);
            assertTrue(result);
            SystemMetadata sys = MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertEquals(sys.getIdentifier(), guid);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Probably not yet implemented: " + e.getMessage());
        }
    }

    /**
     * Test describing an object
     */
    @Test
    public void testDescribe() {
        D1NodeServiceTest.printTestHeader("testDescribe");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testGetSystemMetadata." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            DescribeResponse describeResponse =
                MNodeService.getInstance(request).describe(session, pid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         sysmeta.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         sysmeta.getFormatId().getValue());

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test getting the checksum of an object
     */
    @Test
    public void testGetChecksum() {
        D1NodeServiceTest.printTestHeader("testGetChecksum");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testGetChecksum." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            Checksum checksum = MNodeService.getInstance(request).getChecksum(session, pid, "MD5");
            assertEquals(checksum.getValue(), sysmeta.getChecksum().getValue());

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Testing listing objects on the Member Node
     */
    @Test
    public void testListObjects() {
        D1NodeServiceTest.printTestHeader("testListObjects");

        try {

            Session session = d1NodeTest.getTestSession();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startTime = sdf.parse("2010-01-01");
            Date endTime = new Date();
            ObjectFormatIdentifier objectFormatId = null;
            boolean replicaStatus = false;
            int start = 0;
            int count = 1;

            // insert at least one object
            testCreate();
            // now check that we have at least one
            ObjectList objectList = MNodeService.getInstance(request)
                .listObjects(session, startTime, endTime, objectFormatId, null, replicaStatus,
                             start, count);
            assertNotNull(objectList);
            assertEquals(objectList.getCount(), count);
            assertEquals(0, objectList.getStart());
            assertTrue(objectList.getTotal() >= 1);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());

        }
    }

    /**
     * Test the method of getCapablilites
     * @throws Exception
     */
    @Test
    public void testGetCapabilities() throws Exception {
        D1NodeServiceTest.printTestHeader("testGetCapabilities");
        String originAllowedSubmitters =
            PropertyService.getProperty("auth.allowedSubmitters");
        try {
            Node node = MNodeService.getInstance(request).getCapabilities();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TypeMarshaller.marshalTypeToOutputStream(node, baos);
            assertNotNull(node);
            // check the service restriction. First, there is no any service restrictions
            Services services = node.getServices();
            List<Service> list = services.getServiceList();
            boolean hasV1MNStorage = false;
            boolean hasV2MNStorage = false;
            for (Service service : list) {
                if (service.getName().equals("MNStorage") && service.getVersion().equals("v1")) {
                    hasV1MNStorage = true;
                    List<ServiceMethodRestriction> restrictions = service.getRestrictionList();
                    assertTrue(restrictions == null || restrictions.isEmpty());
                }
                if (service.getName().equals("MNStorage") && service.getVersion().equals("v2")) {
                    hasV2MNStorage = true;
                    List<ServiceMethodRestriction> restrictions = service.getRestrictionList();
                    assertTrue(restrictions == null || restrictions.isEmpty());
                }
            }
            assertTrue(hasV1MNStorage);
            assertTrue(hasV2MNStorage);
            // check the service restriction. Second, there are some service restrictions
            PropertyService.setPropertyNoPersist(
                "auth.allowedSubmitters",
                "http\\://orcid.org/0000-0002-1209-5268:cn=parc,o=PARC,dc=ecoinformatics,dc=org");
            AuthUtil.populateAllowedSubmitters();//make the allowedSubimtters effective
            node = MNodeService.getInstance(request).getCapabilities();
            services = node.getServices();
            list = services.getServiceList();
            hasV1MNStorage = false;
            hasV2MNStorage = false;
            for (Service service : list) {
                if (service.getName().equals("MNStorage") && service.getVersion().equals("v1")) {
                    hasV1MNStorage = true;
                    ServiceMethodRestriction restriction1 = service.getRestriction(0);
                    assertEquals("create", restriction1.getMethodName());
                    assertEquals(
                        "http://orcid.org/0000-0002-1209-5268",
                        restriction1.getSubject(0).getValue());
                    assertEquals(
                        "cn=parc,o=PARC,dc=ecoinformatics,dc=org",
                        restriction1.getSubject(1).getValue());
                    ServiceMethodRestriction restriction2 = service.getRestriction(1);
                    assertEquals("update", restriction2.getMethodName());
                    assertEquals(
                        "http://orcid.org/0000-0002-1209-5268",
                        restriction2.getSubject(0).getValue());
                    assertEquals(
                        "cn=parc,o=PARC,dc=ecoinformatics,dc=org",
                        restriction2.getSubject(1).getValue());
                }
                if (service.getName().equals("MNStorage") && service.getVersion().equals("v2")) {
                    hasV2MNStorage = true;
                    ServiceMethodRestriction restriction1 = service.getRestriction(0);
                    assertEquals("create", restriction1.getMethodName());
                    assertEquals(
                        "http://orcid.org/0000-0002-1209-5268",
                        restriction1.getSubject(0).getValue());
                    assertEquals(
                        "cn=parc,o=PARC,dc=ecoinformatics,dc=org",
                        restriction1.getSubject(1).getValue());
                    ServiceMethodRestriction restriction2 = service.getRestriction(1);
                    assertEquals("update", restriction2.getMethodName());
                    assertEquals(
                        "http://orcid.org/0000-0002-1209-5268",
                        restriction2.getSubject(0).getValue());
                    assertEquals(
                        "cn=parc,o=PARC,dc=ecoinformatics,dc=org",
                        restriction2.getSubject(1).getValue());
                }
            }
            assertTrue(hasV1MNStorage);
            assertTrue(hasV2MNStorage);
            List<Property> properties = node.getPropertyList();
            for (int i = 0; i < properties.size(); i++) {
                Property property = properties.get(i);
                if (property.getKey().equals("read_only_mode")) {
                    assertEquals("false", property.getValue());
                }
            }
        } catch (Exception e) {
            fail("The node instance couldn't be parsed correctly:" + e.getMessage());
        } finally {
            PropertyService.setPropertyNoPersist("auth.allowedSubmitters", originAllowedSubmitters);
            AuthUtil.populateAllowedSubmitters();//make the allowedSubimtters effective
        }

    }

    /**
     * Test the method of ping
     */
    @Test
    public void testPing() {

        try {
            Date mnDate = MNodeService.getInstance(request).ping();
            assertNotNull(mnDate);

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }

    }

    /**
     * Test the scenario of synchronization fails
     */
    @Test
    public void testSynchronizationFailed() {
        D1NodeServiceTest.printTestHeader("testSynchronizationFailed");
        try {
            Session session = d1NodeTest.getTestSession();

            // create the object
            Identifier pid = new Identifier();
            pid.setValue("testSynchronizationFailed." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, session.getSubject(), object);
            Identifier retPid =
                MNodeService.getInstance(request).create(session, pid, object, sysmeta);
            assertEquals(retPid.getValue(), pid.getValue());

            // pretend the sync failed, act as CN
            SynchronizationFailed syncFailed =
                new SynchronizationFailed("0000", "Testing Synch Failure");
            syncFailed.setPid(pid.getValue());
            session = d1NodeTest.getCNSession();
            MNodeService.getInstance(request).synchronizationFailed(session, syncFailed);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }

    }

    /**
     * The the method of systemMetadataChanged
     */
    @Test
    public void testSystemMetadataChanged() {
        D1NodeServiceTest.printTestHeader("testSystemMetadataChanged");
        try {
            Session session = d1NodeTest.getTestSession();

            // create the object
            Identifier pid = new Identifier();
            pid.setValue("testSystemMetadataChanged." + System.currentTimeMillis());
            Identifier sid = new Identifier();
            sid.setValue("testSystemMetadataChangedSid." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, session.getSubject(), object);
            sysmeta.setSeriesId(sid);
            Identifier retPid =
                MNodeService.getInstance(request).create(session, pid, object, sysmeta);
            assertEquals(retPid.getValue(), pid.getValue());

            // pretend the system metadata changed on the CN
            MNodeService.getInstance(request)
                .systemMetadataChanged(session, retPid, 5000L, Calendar.getInstance().getTime());
            MNodeService.getInstance(request)
                .systemMetadataChanged(session, sid, 5000L, Calendar.getInstance().getTime());
            edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                .systemMetadataChanged(session, retPid, 5000L, Calendar.getInstance().getTime());
            edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                .systemMetadataChanged(session, sid, 5000L, Calendar.getInstance().getTime());

        } catch (Exception e) {
            if (e instanceof NotAuthorized) {
                // only CN subjects can call this
                // TODO: use a CN certificate in the tests
            } else {
                fail("Unexpected error: " + e.getMessage());

            }
        }

    }

    /**
     * Test the getLogRecords method
     */
    @Test
    public void testGetLogRecords() {
        D1NodeServiceTest.printTestHeader("testLogRecords");

        try {
            Log log = null;
            Session session = d1NodeTest.getCNSession();
            Date fromDate = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(fromDate);
            calendar.roll(Calendar.YEAR, false);
            fromDate = calendar.getTime();
            Date toDate = new Date();
            Event event = Event.CREATE;
            int start = 0;
            int count = 1;

            log = MNodeService.getInstance(request)
                .getLogRecords(session, fromDate, toDate, event.xmlValue(), null, start, count);

            assertNotNull(log);
            assertTrue(log.getCount() == count);
            assertTrue(log.getStart() == start);
            assertTrue(log.getTotal() >= 1);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());

        }
    }

    /**
     * Test if a subject is authorized to read a known object
     */
    @Test
    public void testIsAuthorized() {
        D1NodeServiceTest.printTestHeader("testIsAuthorized");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testIsAuthorized." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            //non-public readable
            AccessPolicy accessPolicy = new AccessPolicy();
            AccessRule allow = new AccessRule();
            allow.addPermission(Permission.READ);
            Subject subject = new Subject();
            subject.setValue("cn=test2,dc=dataone,dc=org");
            allow.addSubject(subject);
            accessPolicy.addAllow(allow);
            sysmeta.setAccessPolicy(accessPolicy);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            boolean isAuthorized =
                MNodeService.getInstance(request).isAuthorized(session, pid, Permission.READ);
            assertTrue(isAuthorized);
            isAuthorized =
                MNodeService.getInstance(request).isAuthorized(session, pid, Permission.WRITE);
            assertTrue(isAuthorized);
            try {
                isAuthorized =
                    MNodeService.getInstance(request).isAuthorized(null, pid, Permission.READ);
                fail("we can reach here");
            } catch (NotAuthorized ee) {

            }


            Session session2 = d1NodeTest.getAnotherSession();
            isAuthorized =
                MNodeService.getInstance(request).isAuthorized(session2, pid, Permission.READ);
            assertTrue(isAuthorized);

            try {
                isAuthorized =
                    MNodeService.getInstance(request).isAuthorized(session2, pid, Permission.WRITE);
                fail("we can reach here");
            } catch (NotAuthorized ee) {

            }


            try {
                isAuthorized = MNodeService.getInstance(request)
                    .isAuthorized(session2, pid, Permission.CHANGE_PERMISSION);
                fail("we can reach here");
            } catch (NotAuthorized ee) {

            }

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test if node admin is authorized to read a known object
     */
    @Test
    public void testIsAdminAuthorized() {
        D1NodeServiceTest.printTestHeader("testIsAdminAuthorized");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testIsAdminAuthorized." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);

            // test as public - read
            boolean isAuthorized =
                MNodeService.getInstance(request).isAuthorized(null, pid, Permission.READ);
            assertTrue(isAuthorized);

            // test as public - change perm
            try {
                isAuthorized = MNodeService.getInstance(request)
                        .isAuthorized(null, pid, Permission.CHANGE_PERMISSION);
                fail("It can't reach there since public doesn't have the change permission on pid "
                                  + pid);
            } catch (Exception e) {
                assertTrue( e instanceof NotAuthorized);
            }

            //test write by another session
            Session session2 = d1NodeTest.getAnotherSession();
            try {
                isAuthorized = MNodeService.getInstance(request)
                                            .isAuthorized(session2, pid, Permission.WRITE);
                fail("It can't reach there since the user " +session2.getSubject().getValue()
                            + " doesn't have the write permission on pid " + pid.getValue());
            } catch (Exception e) {
                assertTrue( e instanceof NotAuthorized);
            }

            // test as admin
            isAuthorized = MNodeService.getInstance(request)
                .isAuthorized(d1NodeTest.getMNSession(), pid, Permission.CHANGE_PERMISSION);
            assertTrue(isAuthorized);
            // test as cn
            isAuthorized = MNodeService.getInstance(request)
                .isAuthorized(d1NodeTest.getCNSession(), pid, Permission.CHANGE_PERMISSION);
            assertTrue(isAuthorized);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());

        }
    }

    /**
     * Test the scenario that if the equivalent identity can act as same as the original one
     */
    @Test
    public void testIsEquivIdentityAuthorized() {
        D1NodeServiceTest.printTestHeader("testIsEquivIdentityAuthorized");

        try {
            Session session = new Session();
            Subject s = new Subject();
            s.setValue("cn=test,dc=dataone,dc=org");
            session.setSubject(s);

            Identifier pid = new Identifier();
            pid.setValue("testIsEquivIdentityAuthorized." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, session.getSubject(), object);

            // reset the access policy to only allow 'self' read (no public)
            AccessPolicy ap = new AccessPolicy();
            AccessRule ar = new AccessRule();
            List<Subject> sList = new ArrayList<Subject>();
            sList.add(session.getSubject());
            ar.setSubjectList(sList);
            List<Permission> permList = new ArrayList<Permission>();
            permList.add(Permission.CHANGE_PERMISSION);
            ar.setPermissionList(permList);
            ap.addAllow(ar);
            sysmeta.setAccessPolicy(ap);

            // save it
            Identifier retPid =
                CNodeService.getInstance(request).registerSystemMetadata(session, pid, sysmeta);
            assertEquals(pid.getValue(), retPid.getValue());

            //check it against an equivalent identity not listed in the access policy
            session.getSubject().setValue("cn=newSubject,dc=dataone,dc=org");
            SubjectInfo subjectInfo = new SubjectInfo();
            Person person = new Person();
            person.setSubject(session.getSubject());
            List<String> givenNames = new ArrayList<String>();
            givenNames.add("New");
            person.setGivenNameList(givenNames);
            person.setFamilyName("Subject");

            // add equivalent identities
            List<Subject> equivIdentities = new ArrayList<Subject>();
            Subject mappedSubject2 = new Subject();
            mappedSubject2.setValue("cn=test2,dc=dataone,dc=org");
            equivIdentities.add(mappedSubject2);

            Subject mappedSubject = new Subject();
            mappedSubject.setValue("cn=test,dc=dataone,dc=org");
            equivIdentities.add(mappedSubject);

            person.setEquivalentIdentityList(equivIdentities);

            List<Person> personList = new ArrayList<Person>();
            personList.add(person);
            subjectInfo.setPersonList(personList);

            // update the session to include subject info with a mapped identity
            session.setSubjectInfo(subjectInfo);
            boolean result =
                CNodeService.getInstance(request).isAuthorized(session, pid, Permission.READ);
            assertTrue(result);

        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    /**
     * Test object creation failure when there is a space in the identifier
     */
    @Test
    public void testCreateInvalidIdentifier() {
        D1NodeServiceTest.printTestHeader("testCreateInvalidIdentifier");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate withspace." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("Should not be able to create with whitespace in indentifier");
        } catch (InvalidRequest e) {
            // expect that this request fails
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }

    }

    /**
     * Test getting a known object
     */
    @Test
    public void testGetPackage() {
        D1NodeServiceTest.printTestHeader("testGetPackage");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testGetPackage." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            ObjectFormatIdentifier format = new ObjectFormatIdentifier();
            format.setValue("application/bagit-097");
            InputStream bagStream =
                MNodeService.getInstance(request).getPackage(session, format, pid);

        } catch (InvalidRequest e) {

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }


    /**
     * Test getting a known object
     */
    @Test
    public void testGetOREPackage() {
        D1NodeServiceTest.printTestHeader("testGetOREPackage");
        try {
            // construct the ORE package
            Identifier resourceMapId = new Identifier();
            //resourceMapId.setValue("doi://1234/AA/map.1.1");
            resourceMapId.setValue("testGetOREPackage." + System.currentTimeMillis());
            Identifier metadataId = new Identifier();
            metadataId.setValue("doi://1234/AA/meta.1." + System.currentTimeMillis());
            List<Identifier> dataIds = new ArrayList<>();
            Identifier dataId = new Identifier();
            dataId.setValue("doi://1234/AA/data.1." + System.currentTimeMillis());
            Identifier dataId2 = new Identifier();
            dataId2.setValue("doi://1234/AA/data.2." + System.currentTimeMillis());
            dataIds.add(dataId);
            dataIds.add(dataId2);
            Map<Identifier, List<Identifier>> idMap = new HashMap<>();
            idMap.put(metadataId, dataIds);
            ResourceMapFactory rmf = ResourceMapFactory.getInstance();
            ResourceMap resourceMap = rmf.createResourceMap(resourceMapId, idMap);
            assertNotNull(resourceMap);
            String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
            assertNotNull(rdfXml);

            Session session = d1NodeTest.getTestSession();
            InputStream object = null;
            SystemMetadata sysmeta = null;

            // save the data objects (data just contains their ID)
            InputStream dataObject1 = new ByteArrayInputStream(dataId.getValue().getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(dataId, session.getSubject(), dataObject1);
            MNodeService.getInstance(request).create(session, dataId, dataObject1, sysmeta);
            String query = "q=id:" + "\"" + dataId.getValue() + "\"";
            InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
            String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            int account = 0;
            while ((resultStr == null || !resultStr.contains("checksum"))
                && account <= D1NodeServiceTest.MAX_TRIES) {
                Thread.sleep(500);
                account++;
                stream = MNodeService.getInstance(request).query(session, "solr", query);
                resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            }
            // second data file
            InputStream dataObject2 =
                new ByteArrayInputStream(dataId2.getValue().getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(dataId2, session.getSubject(), dataObject2);
            MNodeService.getInstance(request).create(session, dataId2, dataObject2, sysmeta);
            query = "q=id:" + "\"" + dataId2.getValue() + "\"";
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            account = 0;
            while ((resultStr == null || !resultStr.contains("checksum"))
                && account <= D1NodeServiceTest.MAX_TRIES) {
                Thread.sleep(500);
                account++;
                stream = MNodeService.getInstance(request).query(session, "solr", query);
                resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            }

            // metadata file
            InputStream metadataObject =
                new ByteArrayInputStream(metadataId.getValue().getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(metadataId, session.getSubject(), metadataObject);
            MNodeService.getInstance(request).create(session, metadataId, metadataObject, sysmeta);
            query = "q=id:" + "\"" + metadataId.getValue() + "\"";
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            account = 0;
            while ((resultStr == null || !resultStr.contains("checksum"))
                && account <= D1NodeServiceTest.MAX_TRIES) {
                Thread.sleep(500);
                account++;
                stream = MNodeService.getInstance(request).query(session, "solr", query);
                resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            }

            // save the ORE object
            object = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object);
            sysmeta.setFormatId(
                ObjectFormatCache.getInstance().getFormat("http://www.openarchives.org/ore/terms")
                    .getFormatId());
            Identifier pid =
                MNodeService.getInstance(request).create(session, resourceMapId, object, sysmeta);
            query = "q=id:" + resourceMapId.getValue();
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            account = 0;
            while ((resultStr == null || !resultStr.contains("checksum"))
                && account <= D1NodeServiceTest.MAX_TRIES) {
                Thread.sleep(500);
                account++;
                stream = MNodeService.getInstance(request).query(session, "solr", query);
                resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            }
            // get the package we uploaded
            ObjectFormatIdentifier format = new ObjectFormatIdentifier();
            format.setValue("application/bagit-097");
            InputStream bagStream =
                MNodeService.getInstance(request).getPackage(session, format, pid);
            File bagFile = File.createTempFile("bagit.", ".zip");
            IOUtils.copy(bagStream, new FileOutputStream(bagFile));
            // Check that the resource map is the same
            String bagPath = bagFile.getAbsolutePath();
            ZipFile zipFile = new ZipFile(bagPath);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // Check if it's the ORE
                if (entry.getName().contains("testGetOREPackage")) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    object.reset();
                    assertTrue(IOUtils.contentEquals(stream2, object));
                }
                // Check if it's the science metadata
                else if (entry.getName().contains("meta.1")) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    metadataObject.reset();
                    assertTrue(IOUtils.contentEquals(stream2, metadataObject));
                }
                // Check if it's the first data file
                else if (entry.getName().contains("data.1")) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    dataObject1.reset();
                    assertTrue(IOUtils.contentEquals(stream2, dataObject1));
                }
                // Check if it's the second data file
                else if (entry.getName().contains("data.2")) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    dataObject2.reset();
                    assertTrue(IOUtils.contentEquals(stream2, dataObject2));
                }
            }
            // clean up
            bagFile.delete();
            Identifier doi = MNodeService.getInstance(request).publish(session, metadataId);
            query = "q=id:" + "\"" + doi.getValue() + "\"";
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            account = 0;
            while ((resultStr == null || !resultStr.contains("resourceMap"))
                && account <= D1NodeServiceTest.MAX_TRIES) {
                Thread.sleep(1000);
                account++;
                stream = MNodeService.getInstance(request).query(session, "solr", query);
                resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            }
            System.out.println("+++++++++++++++++++ the metadataId on the ore package is "
                                   + metadataId.getValue());
            List<Identifier> oreIds =
                MNodeService.getInstance(request).lookupOreFor(session, doi, true);
            assertEquals(1, oreIds.size());
            List<Identifier> oreId2 =
                MNodeService.getInstance(request).lookupOreFor(session, dataId, true);
            account = 0;
            while (oreId2.size() != 2 && account <= D1NodeServiceTest.MAX_TRIES) {
                Thread.sleep(500);
                account++;
                oreId2 = MNodeService.getInstance(request).lookupOreFor(session, dataId, true);
            }
            assertEquals(2, oreId2.size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test to publish a package
     */
    @Test
    public void testPublishPackage() {
        D1NodeServiceTest.printTestHeader("testPublishPackage");

        try {

            // construct the ORE package
            Identifier resourceMapId = new Identifier();
            //resourceMapId.setValue("doi://1234/AA/map.1.1");
            resourceMapId.setValue("testGetOREPackage." + System.currentTimeMillis());
            Identifier metadataId = new Identifier();
            metadataId.setValue("doi://2234/AA/meta.1." + System.currentTimeMillis());
            List<Identifier> dataIds = new ArrayList<Identifier>();
            Identifier dataId = new Identifier();
            dataId.setValue("doi://2234/AA/data.1." + System.currentTimeMillis());
            Identifier dataId2 = new Identifier();
            dataId2.setValue("doi://2234/AA/data.2." + System.currentTimeMillis());
            dataIds.add(dataId);
            dataIds.add(dataId2);
            Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
            idMap.put(metadataId, dataIds);
            ResourceMapFactory rmf = ResourceMapFactory.getInstance();
            ResourceMap resourceMap = rmf.createResourceMap(resourceMapId, idMap);
            assertNotNull(resourceMap);
            String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
            assertNotNull(rdfXml);

            Session session = d1NodeTest.getTestSession();
            InputStream object = null;
            SystemMetadata sysmeta = null;

            // save the data objects (data just contains their ID)
            InputStream dataObject1 = new ByteArrayInputStream(dataId.getValue().getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(dataId, session.getSubject(), dataObject1);
            MNodeService.getInstance(request).create(session, dataId, dataObject1, sysmeta);
            // second data file
            InputStream dataObject2 =
                new ByteArrayInputStream(dataId2.getValue().getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(dataId2, session.getSubject(), dataObject2);
            MNodeService.getInstance(request).create(session, dataId2, dataObject2, sysmeta);
            // metadata file
            InputStream metadataObject =
                new ByteArrayInputStream(metadataId.getValue().getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(metadataId, session.getSubject(), metadataObject);
            MNodeService.getInstance(request).create(session, metadataId, metadataObject, sysmeta);

            // save the ORE object
            Thread.sleep(10000);
            object = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object);
            sysmeta.setFormatId(
                ObjectFormatCache.getInstance().getFormat("http://www.openarchives.org/ore/terms")
                    .getFormatId());
            Identifier pid =
                MNodeService.getInstance(request).create(session, resourceMapId, object, sysmeta);

            Thread.sleep(30000);
            List<Identifier> oreId3 =
                MNodeService.getInstance(request).lookupOreFor(session, dataId, true);
            assertNotNull("ORE NOT FOUND FOR " + dataId.getValue(), oreId3);
            assertEquals(1, oreId3.size());
            //publish the package
            Identifier doi = MNodeService.getInstance(request).publish(session, metadataId);
            // test the ORE lookup
            Thread.sleep(30000);
            System.out.println("+++++++++++++++++++ the metadataId on the ore package is "
                                   + metadataId.getValue());
            List<Identifier> oreIds =
                MNodeService.getInstance(request).lookupOreFor(session, doi, true);
            assertEquals(1, oreIds.size());
            List<Identifier> oreId2 =
                MNodeService.getInstance(request).lookupOreFor(session, dataId, true);
            assertEquals(2, oreId2.size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test to publish a private package
     */
    @Test
    public void testPublishPrivatePackage() {
        D1NodeServiceTest.printTestHeader("testPublishPrivatePackage");

        try {

            // construct the ORE package
            Identifier resourceMapId = new Identifier();
            //resourceMapId.setValue("doi://1234/AA/map.1.1");
            resourceMapId.setValue("testGetOREPackage." + System.currentTimeMillis());
            Identifier metadataId = new Identifier();
            metadataId.setValue("doi://2235/AA/meta.1." + System.currentTimeMillis());
            List<Identifier> dataIds = new ArrayList<>();
            Identifier dataId = new Identifier();
            dataId.setValue("doi://2235/AA/data.1." + System.currentTimeMillis());
            Identifier dataId2 = new Identifier();
            dataId2.setValue("doi://2235/AA/data.2." + System.currentTimeMillis());
            dataIds.add(dataId);
            dataIds.add(dataId2);
            Map<Identifier, List<Identifier>> idMap = new HashMap<>();
            idMap.put(metadataId, dataIds);
            ResourceMapFactory rmf = ResourceMapFactory.getInstance();
            ResourceMap resourceMap = rmf.createResourceMap(resourceMapId, idMap);
            assertNotNull(resourceMap);
            String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
            assertNotNull(rdfXml);

            Session session = d1NodeTest.getTestSession();
            InputStream object = null;
            SystemMetadata sysmeta = null;

            // save the data objects (data just contains their ID)
            InputStream dataObject1 = new ByteArrayInputStream(dataId.getValue().getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(dataId, session.getSubject(), dataObject1);
            sysmeta.setAccessPolicy(null);
            MNodeService.getInstance(request).create(session, dataId, dataObject1, sysmeta);
            try {
                MNodeService.getInstance(request).get(d1NodeTest.getThirdSession(), dataId);
                fail("we shouldn't get here");
            } catch (NotAuthorized e) {

            }
            // second data file
            InputStream dataObject2 =
                new ByteArrayInputStream(dataId2.getValue().getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(dataId2, session.getSubject(), dataObject2);
            sysmeta.setAccessPolicy(null);
            MNodeService.getInstance(request).create(session, dataId2, dataObject2, sysmeta);
            try {
                MNodeService.getInstance(request).get(d1NodeTest.getThirdSession(), dataId2);
                fail("we shouldn't get here");
            } catch (NotAuthorized e) {

            }
            // metadata file
            InputStream metadataObject =
                new ByteArrayInputStream(metadataId.getValue().getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(metadataId, session.getSubject(), metadataObject);
            sysmeta.setAccessPolicy(null);
            MNodeService.getInstance(request).create(session, metadataId, metadataObject, sysmeta);
            try {
                MNodeService.getInstance(request).get(d1NodeTest.getThirdSession(), metadataId);
                fail("we shouldn't get here");
            } catch (NotAuthorized e) {

            }

            // save the ORE object
            Thread.sleep(10000);
            object = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));
            sysmeta = D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object);
            sysmeta.setAccessPolicy(null);
            sysmeta.setFormatId(
                ObjectFormatCache.getInstance().getFormat("http://www.openarchives.org/ore/terms")
                    .getFormatId());
            Identifier pid =
                MNodeService.getInstance(request).create(session, resourceMapId, object, sysmeta);
            try {
                MNodeService.getInstance(request).get(d1NodeTest.getThirdSession(), resourceMapId);
                fail("we shouldn't get here");
            } catch (NotAuthorized e) {

            }

            Thread.sleep(30000);
            List<Identifier> oreId3 =
                MNodeService.getInstance(request).lookupOreFor(session, dataId, true);
            assertNotNull("ORE NOT FOUND FOR " + dataId
                              + " -- could be an auth issue? Check indexer logs? Owner is: "
                              + session.getSubject(), oreId3);
            assertEquals(1, oreId3.size());
            //publish the package
            Identifier doi = MNodeService.getInstance(request).publish(session, metadataId);
            // test the ORE lookup
            Thread.sleep(30000);
            System.out.println("+++++++++++++++++++ the metadataId on the ore package is "
                                   + metadataId.getValue());
            List<Identifier> oreIds =
                MNodeService.getInstance(request).lookupOreFor(session, doi, true);
            assertEquals(1, oreIds.size());
            List<Identifier> oreId2 =
                MNodeService.getInstance(request).lookupOreFor(session, dataId, true);
            assertEquals(2, oreId2.size());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test the extra "delete information" was added to the NotFoundException if the object was
     * delete in the following methods: MN.get MN.getSystemmetadata MN.describe MN.getChecksum
     * MN.getRelica
     */
    @Test
    public void testReadDeletedObject() {
        D1NodeServiceTest.printTestHeader("testDelete");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testDelete." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            Thread.sleep(3000);
            // use MN admin to delete
            session = d1NodeTest.getMNSession();
            Identifier deletedPid = MNodeService.getInstance(request).delete(session, pid);
            System.out.println("after deleting");
            assertEquals(pid.getValue(), deletedPid.getValue());
            // check that we cannot get the object
            session = d1NodeTest.getTestSession();
            InputStream deletedObject = null;
            try {
                //System.out.println("before read ===============");
                deletedObject = MNodeService.getInstance(request).get(session, deletedPid);
                //System.out.println("after read ===============");
            } catch (NotFound nf) {
                assertTrue(nf.getMessage().contains("deleted"));
            }
            try {
                //System.out.println("before read ===============");
                SystemMetadata sysmeta2 =
                    MNodeService.getInstance(request).getSystemMetadata(session, deletedPid);
                //System.out.println("after read ===============");
            } catch (NotFound nf) {
                //System.out.println("the exception is "+nf.getMessage());
                assertTrue(nf.getMessage().contains("deleted"));
            }

            try {
                //System.out.println("before read ===============");
                DescribeResponse describeResponse =
                    MNodeService.getInstance(request).describe(session, pid);
                //System.out.println("after read ===============");
            } catch (NotFound nf) {
                //System.out.println("the exception is "+nf.getMessage());
                assertTrue(nf.getMessage().contains("deleted"));
            }

            try {
                //System.out.println("before read ===============");
                Checksum checksum =
                    MNodeService.getInstance(request).getChecksum(session, pid, "MD5");
                //System.out.println("after read ===============");
            } catch (NotFound nf) {
                //System.out.println("the exception 3 is "+nf.getMessage());
                assertTrue(nf.getMessage().contains("deleted"));
            }

            try {
                //System.out.println("before read ===============");
                boolean isAuthorized =
                    MNodeService.getInstance(request).isAuthorized(session, pid, Permission.READ);
                //System.out.println("after read ===============");
            } catch (NotFound nf) {
                System.out.println("the exception 4 is " + nf.getMessage());
                assertTrue(nf.getMessage().contains("deleted"));
            }

            assertNull(deletedObject);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());

        }
    }

    /**
     * Test to create and update a metadata which xml declaration is ASCII, but actually has some
     * special charaters. The saved document should has the same bytes as the origianl.
     */
    @Test
    public void testCreateAndUpdateXMLWithUnmatchingEncoding() throws Exception {
        String algorithm = "md5";
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testCreateAndUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream(
            FileUtils.readFileToByteArray(new File(unmatchingEncodingFilePath)));
        Checksum orgChecksum = ChecksumUtil.checksum(object, algorithm);
        //System.out.println("the original checksum is "+orgChecksum.getValue());
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        InputStream readResult = MNodeService.getInstance(request).get(session, pid);
        byte[] readBytes = IOUtils.toByteArray(readResult);
        Checksum checksum1 = ChecksumUtil.checksum(readBytes, algorithm);
        //System.out.println("the read checksum1 is "+checksum1.getValue());
        assertEquals(orgChecksum.getValue(), checksum1.getValue());

        Identifier newPid = new Identifier();
        newPid.setValue(
            "testCreateAndUpdate." + (System.currentTimeMillis() + 1)); // ensure it is different
        // from original
        object = new ByteArrayInputStream(
            FileUtils.readFileToByteArray(new File(unmatchingEncodingFilePath)));
        SystemMetadata newSysMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);

        // do the update
        Identifier updatedPid =
            MNodeService.getInstance(request).update(session, pid, object, newPid, newSysMeta);
        InputStream readResult2 = MNodeService.getInstance(request).get(session, updatedPid);
        byte[] readBytes2 = IOUtils.toByteArray(readResult2);
        Checksum checksum2 = ChecksumUtil.checksum(readBytes2, algorithm);
        assertEquals(orgChecksum.getValue(), checksum2.getValue());
        //System.out.println("the read checksum2 is "+checksum2.getValue());


    }

    /**
     * Test the method - get api  for a speicified SID
     */
    @Test
    public void testGetSID() {
        String str1 = "object1";
        String str2 = "object2";
        String str3 = "object3";
        Date fromDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fromDate);
        calendar.roll(Calendar.YEAR, false);
        fromDate = calendar.getTime();
        try {
            //insert test documents with a series id
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue(d1NodeTest.generateDocumentId());
            InputStream object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
            String sid1 = "sid." + System.nanoTime();
            Identifier seriesId = new Identifier();
            seriesId.setValue(sid1);
            System.out.println("the first sid is " + seriesId.getValue());
            sysmeta.setSeriesId(seriesId);
            MNodeService.getInstance(request).create(session, guid, object1, sysmeta);
            System.out.println("the first pid is " + guid.getValue());
            //test the get(pid) for v2
            InputStream result = MNodeService.getInstance(request).get(session, guid);
            // go back to beginning of original stream
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result.available() > 0);
            assertTrue(IOUtils.contentEquals(result, object1));
            // test the get(id) for v2
            InputStream result1 = MNodeService.getInstance(request).get(session, seriesId);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result1.available() > 0);
            assertTrue(IOUtils.contentEquals(result1, object1));
            //test the get(pid) for v1
            InputStream result2 =
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .get(session, guid);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result2.available() > 0);
            assertTrue(IOUtils.contentEquals(result2, object1));
            //test the get(sid) for v1
            try {
                InputStream result3 =
                    edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                        .get(session, seriesId);
                fail("the get(sid) method should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }
            SystemMetadata metadata =
                MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertEquals(metadata.getIdentifier().getValue(), guid.getValue());
            assertEquals(metadata.getSeriesId().getValue(), seriesId.getValue());
            DescribeResponse describeResponse =
                MNodeService.getInstance(request).describe(session, seriesId);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata.getFormatId().getValue());

            metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertEquals(metadata.getIdentifier().getValue(), guid.getValue());
            assertEquals(metadata.getSeriesId().getValue(), seriesId.getValue());
            describeResponse = MNodeService.getInstance(request).describe(session, guid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata.getFormatId().getValue());

            org.dataone.service.types.v1.SystemMetadata sys1 =
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .getSystemMetadata(session, guid);
            assertEquals(metadata.getIdentifier().getValue(), guid.getValue());

            try {
                org.dataone.service.types.v1.SystemMetadata sys2 =
                    edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                        .getSystemMetadata(session, seriesId);
                fail("the getSystemMetadata(sid) method should throw a not found exception for "
                         + "the sid " + seriesId.getValue());
            } catch (NotFound nf2) {

            }

            describeResponse = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                .describe(session, guid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         sys1.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         sys1.getFormatId().getValue());
            try {
                describeResponse =
                    edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                        .describe(session, seriesId);
                fail("the describe(sid) method should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound nf2) {

            }

            Checksum sum = MNodeService.getInstance(request).getChecksum(session, guid, "md5");
            assertEquals("5b78f9689b9aab1ebc0f3c1df916dd97", sum.getValue());

            try {
                sum = MNodeService.getInstance(request).getChecksum(session, seriesId, "md5");
                fail("the getCheckSum shouldn't work for sid");
            } catch (NotFound nf3) {

            }

            sum = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                .getChecksum(session, guid, "md5");
            assertEquals("5b78f9689b9aab1ebc0f3c1df916dd97", sum.getValue());

            try {
                sum = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .getChecksum(session, seriesId, "md5");
                fail("the getCheckSum shouldn't work for sid");
            } catch (NotFound nf3) {

            }

            boolean isAuthorized =
                MNodeService.getInstance(request).isAuthorized(session, guid, Permission.READ);
            assertTrue(isAuthorized);

            isAuthorized =
                MNodeService.getInstance(request).isAuthorized(session, seriesId, Permission.READ);
            assertTrue(isAuthorized);

            isAuthorized = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                .isAuthorized(session, guid, Permission.READ);
            assertTrue(isAuthorized);

            try {
                isAuthorized = edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .isAuthorized(session, seriesId, Permission.READ);
                fail("we can't reach here since the v1 isAuthorized method doesn't suppport series"
                         + " id");
            } catch (NotFound e) {

            }

            Session cnsession = d1NodeTest.getCNSession();
            Date toDate = new Date();
            Event event = Event.READ;
            int start = 0;
            int count = 1;
            Log log = MNodeService.getInstance(request)
                .getLogRecords(cnsession, null, null, event.xmlValue(), seriesId.getValue(), start,
                               count);

            assertNotNull(log);
            assertEquals(log.getCount(), count);
            assertEquals(log.getStart(), start);
            assertTrue(log.getTotal() >= 1);
            assertEquals(log.getLogEntry(0).getIdentifier(), guid);

            //do a update with the same series id
            Thread.sleep(1000);
            Identifier newPid = new Identifier();
            newPid.setValue(d1NodeTest.generateDocumentId() + "1");
            System.out.println("the second pid is " + newPid.getValue());
            InputStream object2 = new ByteArrayInputStream(str2.getBytes(StandardCharsets.UTF_8));
            SystemMetadata newSysMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object2);
            newSysMeta.setObsoletes(guid);
            newSysMeta.setSeriesId(seriesId);
            MNodeService.getInstance(request).update(session, guid, object2, newPid, newSysMeta);

            InputStream result4 = MNodeService.getInstance(request).get(session, guid);
            // go back to beginning of original stream
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result4.available() > 0);
            assertTrue(IOUtils.contentEquals(result4, object1));

            InputStream result5 = MNodeService.getInstance(request).get(session, newPid);
            // go back to beginning of original stream
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result5.available() > 0);
            assertTrue(IOUtils.contentEquals(result5, object2));


            InputStream result6 = MNodeService.getInstance(request).get(session, seriesId);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result6.available() > 0);
            assertTrue(IOUtils.contentEquals(result6, object2));
            //test the get(pid) for v1
            InputStream result7 =
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .get(session, guid);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result7.available() > 0);
            assertTrue(IOUtils.contentEquals(result7, object1));

            InputStream result8 =
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .get(session, newPid);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result8.available() > 0);
            assertTrue(IOUtils.contentEquals(result8, object2));
            //test the get(sid) for v1
            try {
                InputStream result3 =
                    edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                        .get(session, seriesId);
                fail("the get(sid) method should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }

            SystemMetadata metadata1 =
                MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertEquals(metadata1.getIdentifier().getValue(), newPid.getValue());
            assertEquals(metadata1.getSeriesId().getValue(), seriesId.getValue());
            describeResponse = MNodeService.getInstance(request).describe(session, seriesId);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata1.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata1.getFormatId().getValue());

            SystemMetadata metadata2 =
                MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertEquals(metadata2.getIdentifier().getValue(), guid.getValue());
            assertEquals(metadata2.getSeriesId().getValue(), seriesId.getValue());
            describeResponse = MNodeService.getInstance(request).describe(session, guid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata2.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata2.getFormatId().getValue());

            SystemMetadata metadata3 =
                MNodeService.getInstance(request).getSystemMetadata(session, newPid);
            assertEquals(metadata3.getIdentifier().getValue(), newPid.getValue());
            assertEquals(metadata3.getSeriesId().getValue(), seriesId.getValue());
            describeResponse = MNodeService.getInstance(request).describe(session, newPid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata3.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata3.getFormatId().getValue());

            //do another update with different series id
            Thread.sleep(1000);
            String sid2 = "sid." + System.nanoTime();
            Identifier seriesId2 = new Identifier();
            seriesId2.setValue(sid2);
            System.out.println("the second sid is " + seriesId2.getValue());
            Identifier newPid2 = new Identifier();
            newPid2.setValue(d1NodeTest.generateDocumentId() + "2");
            System.out.println("the third pid is " + newPid2.getValue());
            InputStream object3 = new ByteArrayInputStream(str3.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta3 = D1NodeServiceTest.createSystemMetadata(newPid2, session.getSubject(), object3);
            sysmeta3.setObsoletes(newPid);
            sysmeta3.setSeriesId(seriesId2);
            MNodeService.getInstance(request).update(session, newPid, object3, newPid2, sysmeta3);

            InputStream result9 = MNodeService.getInstance(request).get(session, guid);
            // go back to beginning of original stream
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result9.available() > 0);
            assertTrue(IOUtils.contentEquals(result9, object1));

            InputStream result10 = MNodeService.getInstance(request).get(session, newPid);
            // go back to beginning of original stream
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result10.available() > 0);
            assertTrue(IOUtils.contentEquals(result10, object2));


            InputStream result11 = MNodeService.getInstance(request).get(session, newPid2);
            // go back to beginning of original stream
            object3.reset();
            // check
            assertTrue(object3.available() > 0);
            assertTrue(result11.available() > 0);
            assertTrue(IOUtils.contentEquals(result11, object3));

            InputStream result12 = MNodeService.getInstance(request).get(session, seriesId2);
            object3.reset();
            // check
            assertTrue(object3.available() > 0);
            assertTrue(result12.available() > 0);
            assertTrue(IOUtils.contentEquals(result12, object3));

            InputStream result16 = MNodeService.getInstance(request).get(session, seriesId);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result16.available() > 0);
            assertTrue(IOUtils.contentEquals(result16, object2));

            //test the get(pid) for v1
            InputStream result13 =
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .get(session, guid);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result13.available() > 0);
            assertTrue(IOUtils.contentEquals(result13, object1));

            InputStream result14 =
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .get(session, newPid);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result14.available() > 0);
            assertTrue(IOUtils.contentEquals(result14, object2));

            InputStream result15 =
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .get(session, newPid2);
            object3.reset();
            // check
            assertTrue(object3.available() > 0);
            assertTrue(result15.available() > 0);
            assertTrue(IOUtils.contentEquals(result15, object3));

            SystemMetadata metadata4 =
                MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertEquals(metadata4.getIdentifier().getValue(), newPid.getValue());
            assertEquals(metadata4.getSeriesId().getValue(), seriesId.getValue());
            describeResponse = MNodeService.getInstance(request).describe(session, seriesId);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata4.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata4.getFormatId().getValue());

            SystemMetadata metadata5 =
                MNodeService.getInstance(request).getSystemMetadata(session, seriesId2);
            assertEquals(metadata5.getIdentifier().getValue(), newPid2.getValue());
            assertEquals(metadata5.getSeriesId().getValue(), seriesId2.getValue());
            describeResponse = MNodeService.getInstance(request).describe(session, seriesId2);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata5.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata5.getFormatId().getValue());

            SystemMetadata metadata6 =
                MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertEquals(metadata6.getIdentifier().getValue(), guid.getValue());
            assertEquals(metadata6.getSeriesId().getValue(), seriesId.getValue());
            describeResponse = MNodeService.getInstance(request).describe(session, guid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata6.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata6.getFormatId().getValue());

            SystemMetadata metadata7 =
                MNodeService.getInstance(request).getSystemMetadata(session, newPid);
            assertEquals(metadata7.getIdentifier().getValue(), newPid.getValue());
            assertEquals(metadata7.getSeriesId().getValue(), seriesId.getValue());
            describeResponse = MNodeService.getInstance(request).describe(session, newPid);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata7.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata7.getFormatId().getValue());

            SystemMetadata metadata8 =
                MNodeService.getInstance(request).getSystemMetadata(session, newPid2);
            assertEquals(metadata8.getIdentifier().getValue(), newPid2.getValue());
            assertEquals(metadata8.getSeriesId().getValue(), seriesId2.getValue());
            describeResponse = MNodeService.getInstance(request).describe(session, newPid2);
            assertEquals(describeResponse.getDataONE_Checksum().getValue(),
                         metadata8.getChecksum().getValue());
            assertEquals(describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                         metadata8.getFormatId().getValue());


            //test the get(sid) for v1
            try {
                InputStream result3 =
                    edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                        .get(session, seriesId);
                fail("the get(sid) method should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }

            //test the get(sid) for v1
            try {
                InputStream result3 =
                    edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                        .get(session, seriesId2);
                fail("the get(sid) method should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }

            //test to get non-existing id for v2
            try {
                // the pid should be null when we try to get a no-exist sid
                Identifier non_exist_sid = new Identifier();
                non_exist_sid.setValue("no-sid-exist-123qwe");
                InputStream result3 = MNodeService.getInstance(request).get(session, non_exist_sid);
                fail("the get(sid) method should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }

            try {
                // the pid should be null when we try to get a no-exist sid
                Identifier non_exist_sid = new Identifier();
                non_exist_sid.setValue("no-sid-exist-123qwe");
                SystemMetadata result3 =
                    MNodeService.getInstance(request).getSystemMetadata(session, non_exist_sid);
                fail("the getSystemMetadata(sid) method should throw a not found exception for "
                         + "the sid " + seriesId.getValue());
            } catch (NotFound ee) {

            }

            try {
                // the pid should be null when we try to get a no-exist sid
                Identifier non_exist_sid = new Identifier();
                non_exist_sid.setValue("no-sid-exist-123qwe");
                MNodeService.getInstance(request).describe(session, non_exist_sid);
                fail("the describe(sid) method should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }

            toDate = new Date();
            event = Event.READ;
            start = 0;
            count = 1;
            log = MNodeService.getInstance(request)
                .getLogRecords(cnsession, null, null, event.xmlValue(), seriesId.getValue(), start,
                               count);

            assertNotNull(log);
            assertEquals(log.getCount(), count);
            assertEquals(log.getStart(), start);
            assertTrue(log.getTotal() >= 1);
            assertEquals(log.getLogEntry(0).getIdentifier(), newPid);

            //do another update with invalid series ids
            Thread.sleep(1000);
            Identifier newPid3 = new Identifier();
            newPid3.setValue(d1NodeTest.generateDocumentId() + "3");
            System.out.println("the third pid is " + newPid3.getValue());
            InputStream object4 = new ByteArrayInputStream(str3.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta4 = D1NodeServiceTest.createSystemMetadata(newPid3, session.getSubject(), object4);
            sysmeta4.setObsoletes(newPid2);
            sysmeta4.setSeriesId(seriesId);
            try {
                MNodeService.getInstance(request)
                    .update(session, newPid2, object4, newPid3, sysmeta4);
                fail("we can't reach here since the sid is using an old one ");
            } catch (InvalidSystemMetadata eee) {

            }

            sysmeta4.setSeriesId(newPid3);
            try {
                MNodeService.getInstance(request)
                    .update(session, newPid2, object4, newPid3, sysmeta4);
                fail("we can't reach here since the sid is using the pid ");
            } catch (InvalidSystemMetadata eee) {

            }

            //test archive a series id by v1
            try {
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .archive(session, seriesId2);
                fail("we can't reach here since the v1 archive method doesn't support the sid ");
            } catch (NotFound nf2) {

            }

            // test delete a series id by v1
            Session mnSession = d1NodeTest.getMNSession();
            try {
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .delete(mnSession, seriesId2);
                fail("we can't reach here since the v1 delete method doesn't support the sid ");
            } catch (NotFound nf2) {

            }

            // test archive a series id by v2
            MNodeService.getInstance(request).archive(session, seriesId2);
            SystemMetadata archived =
                MNodeService.getInstance(request).getSystemMetadata(session, seriesId2);
            assertTrue(archived.getArchived());
            archived = MNodeService.getInstance(request).getSystemMetadata(session, newPid2);
            assertTrue(archived.getArchived());

            // test delete a series id by v2
            MNodeService.getInstance(request).delete(mnSession, seriesId2);
            try {
                MNodeService.getInstance(request).get(session, seriesId2);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                System.out.println("the message is =============" + nf3.getMessage());
                //assertTrue(nf3.getMessage().indexOf("delete") >0);
            }

            try {
                MNodeService.getInstance(request).get(session, newPid2);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                //System.out.println("the message is ============="+nf3.getMessage());
                assertTrue(nf3.getMessage().indexOf("delete") > 0);
            }

            try {
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(request)
                    .get(session, newPid2);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                System.out.println("the message is =============" + nf3.getMessage());
                assertTrue(nf3.getMessage().indexOf("delete") > 0);
            }

            //archive seriesId
            MNodeService.getInstance(request).archive(mnSession, seriesId);
            archived = MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertTrue(archived.getArchived());
            archived = MNodeService.getInstance(request).getSystemMetadata(session, newPid);
            assertTrue(archived.getArchived());


            //delete seriesId
            MNodeService.getInstance(request).delete(mnSession, seriesId);
            try {
                MNodeService.getInstance(request).get(session, newPid);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                //System.out.println("the message is ============="+nf3.getMessage());
                assertTrue(nf3.getMessage().indexOf("delete") > 0);
            }
            SystemMetadata meta =
                MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
            assertEquals(meta.getIdentifier().getValue(), guid.getValue());

        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Test the listView methods.
     *
     * @throws Exception
     */
    @Test
    public void testListViews() throws Exception {
        Session session = null;
        OptionList list = MNodeService.getInstance(request).listViews(session);
        assertTrue(list.sizeOptionList() > 0);
        List<String> names = list.getOptionList();
        for (String name : names) {
            System.out.println("It has the view named " + name);
        }
    }

    /**
     * Test the method updateSystemMetadata
     * @throws Exception
     */
    @Test
    public void testUpdateSystemMetadata() throws Exception {
        String str1 = "object1";
        String str2 = "object2";
        String str3 = "object3";

        Date date = new Date();
        Thread.sleep(2000);
        //insert test documents with a series id
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(d1NodeTest.generateDocumentId());
        InputStream object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
        String sid1 = "sid." + System.nanoTime();
        Identifier seriesId = new Identifier();
        seriesId.setValue(sid1);
        System.out.println("the first sid is " + seriesId.getValue());
        sysmeta.setSeriesId(seriesId);
        sysmeta.setArchived(false);
        MNodeService.getInstance(request).create(session, guid, object1, sysmeta);
        //Test the generating object succeeded.
        SystemMetadata metadata =
            MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertEquals(metadata.getIdentifier(), guid);
        assertEquals(false, (boolean) metadata.getArchived());
        System.out.println("the checksum from request is " + metadata.getChecksum().getValue());
        assertEquals(metadata.getSize(), sysmeta.getSize());
        System.out.println("the identifier is " + guid.getValue());

        Date current = sysmeta.getDateSysMetadataModified();
        //updating system metadata failed since the date doesn't match
        sysmeta.setArchived(true);
        sysmeta.setDateSysMetadataModified(date);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmeta);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        //update system metadata sucessfully
        sysmeta.setDateSysMetadataModified(current);
        BigInteger serialVersion = metadata.getSerialVersion();
        System.out.println("the identifier is ----------------------- " + guid.getValue());
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmeta);
        SystemMetadata metadata2 =
            MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
        assertEquals(metadata2.getIdentifier(), guid);
        assertEquals(metadata2.getSeriesId(), seriesId);
        assertEquals(true, (boolean) metadata2.getArchived());
        assertEquals(metadata2.getChecksum().getValue(), metadata.getChecksum().getValue());
        assertTrue(metadata2.getDateSysMetadataModified().getTime() > current.getTime());

        Identifier newId = new Identifier();
        newId.setValue("newValue");
        sysmeta.setIdentifier(newId);
        serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta.setSerialVersion(serialVersion);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmeta);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }

        newId.setValue("newValue");
        sysmeta.setSeriesId(newId);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmeta);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }

        Date newDate = new Date();
        sysmeta.setDateUploaded(newDate);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmeta);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }

        Checksum checkSum = new Checksum();
        checkSum.setValue("12345");
        sysmeta.setChecksum(checkSum);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmeta);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }

        BigInteger size = new BigInteger("4000");
        sysmeta.setSize(size);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmeta);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
    }

    /**
     * Test the updateSystemmetadata method by users with different permission
     *
     * @throws Exception
     */
    @Test
    public void testUpdateSystemMetadataPermission() throws Exception {
        Subject read = new Subject();
        read.setValue("Read");
        Session readSession = new Session();
        readSession.setSubject(read);
        AccessRule readRule = new AccessRule();
        readRule.addPermission(Permission.READ);
        readRule.addSubject(read);

        Subject write = new Subject();
        write.setValue("Write");
        Session writeSession = new Session();
        writeSession.setSubject(write);
        AccessRule writeRule = new AccessRule();
        writeRule.addPermission(Permission.WRITE);
        writeRule.addSubject(write);

        Subject change = new Subject();
        change.setValue("Change");
        Session changeSession = new Session();
        changeSession.setSubject(change);
        AccessRule changeRule = new AccessRule();
        changeRule.addPermission(Permission.CHANGE_PERMISSION);
        changeRule.addSubject(change);

        Subject rightsHolder = new Subject();
        rightsHolder.setValue("rightsHolder");
        Subject newRightsHolder = new Subject();
        newRightsHolder.setValue("newRightsHolder");

        String str1 = "object1";
        Thread.sleep(1000);
        //insert test documents with a series id
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(d1NodeTest.generateDocumentId());
        InputStream object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
        sysmeta.setRightsHolder(rightsHolder);
        AccessPolicy policy = new AccessPolicy();
        policy.addAllow(readRule);
        policy.addAllow(writeRule);
        policy.addAllow(changeRule);
        sysmeta.setAccessPolicy(policy);
        MNodeService.getInstance(request).create(session, guid, object1, sysmeta);
        SystemMetadata readSys =
            MNodeService.getInstance(request).getSystemMetadata(readSession, guid);
        assertEquals(3, readSys.getAccessPolicy().sizeAllowList());

        //Read permission user can't update system metadata
        try {
            MNodeService.getInstance(request).updateSystemMetadata(readSession, guid, sysmeta);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        readSys = MNodeService.getInstance(request).getSystemMetadata(readSession, guid);
        assertEquals(3, readSys.getAccessPolicy().sizeAllowList());

        //Write permission user can't update the right holder
        object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        SystemMetadata newSysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
        newSysmeta.setRightsHolder(newRightsHolder);
        AccessPolicy policy1 = new AccessPolicy();
        policy1.addAllow(readRule);
        policy1.addAllow(writeRule);
        policy1.addAllow(changeRule);
        newSysmeta.setAccessPolicy(policy1);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(writeSession, guid, newSysmeta);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        readSys = MNodeService.getInstance(request).getSystemMetadata(readSession, guid);
        assertEquals(3, readSys.getAccessPolicy().sizeAllowList());

        //Write permission user can't update the access policy
        newSysmeta.setRightsHolder(rightsHolder);
        AccessPolicy policy2 = new AccessPolicy();
        policy2.addAllow(readRule);
        newSysmeta.setAccessPolicy(policy2);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(writeSession, guid, newSysmeta);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }

        //Write permission user can update file name
        readSys = MNodeService.getInstance(request).getSystemMetadata(readSession, guid);
        assertEquals(3, readSys.getAccessPolicy().sizeAllowList());
        newSysmeta.setRightsHolder(rightsHolder);
        newSysmeta.setAccessPolicy(policy);
        newSysmeta.setFileName("foo");
        newSysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        newSysmeta.setDateUploaded(readSys.getDateUploaded());
        MNodeService.getInstance(request).updateSystemMetadata(writeSession, guid, newSysmeta);
        readSys = MNodeService.getInstance(request).getSystemMetadata(readSession, guid);
        assertEquals("foo", readSys.getFileName());
        assertEquals(3, readSys.getAccessPolicy().sizeAllowList());
        assertEquals("rightsHolder", readSys.getRightsHolder().getValue());

        //Change permission user can update the right holder
        newSysmeta.setRightsHolder(newRightsHolder);
        newSysmeta.setAccessPolicy(policy);
        newSysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        MNodeService.getInstance(request).updateSystemMetadata(changeSession, guid, newSysmeta);
        readSys = MNodeService.getInstance(request).getSystemMetadata(readSession, guid);
        assertEquals("newRightsHolder", readSys.getRightsHolder().getValue());
        assertEquals("foo", readSys.getFileName());
        assertEquals(3, readSys.getAccessPolicy().sizeAllowList());

        //Change permission user can update the access policy
        policy2 = new AccessPolicy();
        policy2.addAllow(readRule);
        policy2.addAllow(changeRule);
        newSysmeta.setAccessPolicy(policy2);
        newSysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        MNodeService.getInstance(request).updateSystemMetadata(changeSession, guid, newSysmeta);
        readSys = MNodeService.getInstance(request).getSystemMetadata(readSession, guid);
        assertEquals("newRightsHolder", readSys.getRightsHolder().getValue());
        assertEquals("foo", readSys.getFileName());
        assertEquals(2, readSys.getAccessPolicy().sizeAllowList());

        //change permission user can update file name
        newSysmeta.setFileName("newfoo");
        newSysmeta.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        MNodeService.getInstance(request).updateSystemMetadata(changeSession, guid, newSysmeta);
        readSys = MNodeService.getInstance(request).getSystemMetadata(readSession, guid);
        assertEquals("newfoo", readSys.getFileName());
        assertEquals("newRightsHolder", readSys.getRightsHolder().getValue());
        assertEquals(2, readSys.getAccessPolicy().sizeAllowList());
    }

    /**
     * Test the scenarios to update the obsolets and obsoletedBy fields
     * @throws Exception
     */
    @Test
    public void testUpdateObsoletesAndObsoletedBy() throws Exception {
        String str1 = "object1";
        String str2 = "object2";
        String str3 = "object3";

        //insert two documents
        Session session = d1NodeTest.getTestSession();
        Identifier guid1 = new Identifier();
        guid1.setValue(d1NodeTest.generateDocumentId());
        InputStream object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta1 = D1NodeServiceTest.createSystemMetadata(guid1, session.getSubject(), object1);
        MNodeService.getInstance(request).create(session, guid1, object1, sysmeta1);
        //Test the generating object succeeded.
        SystemMetadata metadata =
            MNodeService.getInstance(request).getSystemMetadata(session, guid1);
        assertEquals(metadata.getIdentifier(), guid1);
        assertEquals(metadata.getSize(), sysmeta1.getSize());

        Identifier guid2 = new Identifier();
        guid2.setValue(d1NodeTest.generateDocumentId());
        InputStream object2 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object2);
        MNodeService.getInstance(request).create(session, guid2, object2, sysmeta2);
        //Test the generating object succeeded.
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        assertEquals(metadata.getIdentifier(), guid2);
        assertEquals(metadata.getSize(), sysmeta2.getSize());


        //update the system metadata without touching the obsoletes and obsoletdBy
        AccessPolicy accessPolicy = new AccessPolicy();
        AccessRule allow = new AccessRule();
        allow.addPermission(Permission.WRITE);
        Subject subject = new Subject();
        subject.setValue("user_foo");
        allow.addSubject(subject);
        accessPolicy.addAllow(allow);
        sysmeta1.setAccessPolicy(accessPolicy);
        BigInteger serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta1.setSerialVersion(serialVersion);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid1, sysmeta1);
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid1);
        assertEquals(metadata.getIdentifier(), guid1);
        assertNull(metadata.getObsoletedBy());
        assertNull(metadata.getObsoletes());
        Session newSession = new Session();
        newSession.setSubject(subject);
        boolean isAuthorized =
            MNodeService.getInstance(request).isAuthorized(newSession, guid1, Permission.WRITE);
        assertTrue(isAuthorized);

        sysmeta2.setAccessPolicy(accessPolicy);
        serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta2.setSerialVersion(serialVersion);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, sysmeta2);
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        assertEquals(metadata.getIdentifier(), guid2);
        assertNull(metadata.getObsoletes());
        assertNull(metadata.getObsoletedBy());
        assertEquals(metadata.getChecksum().getValue(), sysmeta2.getChecksum().getValue());
        isAuthorized =
            MNodeService.getInstance(request).isAuthorized(newSession, guid2, Permission.WRITE);
        assertTrue(isAuthorized);


        //update obsolets and obsoletedBy sucessfully - set p2 obsoletes p1
        sysmeta1.setObsoletedBy(guid2);
        serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta1.setSerialVersion(serialVersion);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid1, sysmeta1);
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid1);
        assertEquals(metadata.getIdentifier(), guid1);
        assertEquals(metadata.getObsoletedBy(), guid2);
        assertNull(metadata.getObsoletes());
        assertEquals(metadata.getChecksum().getValue(), sysmeta1.getChecksum().getValue());

        sysmeta2.setObsoletes(guid1);
        serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta2.setSerialVersion(serialVersion);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, sysmeta2);
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        assertEquals(metadata.getIdentifier(), guid2);
        assertEquals(metadata.getObsoletes(), guid1);
        assertNull(metadata.getObsoletedBy());
        assertEquals(metadata.getChecksum().getValue(), sysmeta2.getChecksum().getValue());


        //update obsolets and obsoletedBy sucessfully - set p1 obsoletedBy p2
        sysmeta1.setObsoletedBy(guid2);
        serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta1.setSerialVersion(serialVersion);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid1, sysmeta1);
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid1);
        assertEquals(metadata.getIdentifier(), guid1);
        assertEquals(metadata.getObsoletedBy(), guid2);
        assertNull(metadata.getObsoletes());
        assertEquals(metadata.getChecksum().getValue(), sysmeta1.getChecksum().getValue());

        sysmeta2.setObsoletes(guid1);
        serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta2.setSerialVersion(serialVersion);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, sysmeta2);
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        assertEquals(metadata.getIdentifier(), guid2);
        assertEquals(metadata.getObsoletes(), guid1);
        assertNull(metadata.getObsoletedBy());
        assertEquals(metadata.getChecksum().getValue(), sysmeta2.getChecksum().getValue());


        //resetting the obsoletes and obsoletedBy fails
        Identifier newId = new Identifier();
        newId.setValue("newValue");
        sysmeta1.setObsoletedBy(newId);
        serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta1.setSerialVersion(serialVersion);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid1, sysmeta1);
            fail("We shouldn't get there");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e instanceof InvalidRequest);
        }

        sysmeta2.setObsoletes(newId);
        serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta2.setSerialVersion(serialVersion);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid2, sysmeta2);
            fail("We shouldn't get there");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e instanceof InvalidRequest);
        }


        //insert another object
        Identifier guid5 = new Identifier();
        guid5.setValue(d1NodeTest.generateDocumentId());
        object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta5 = D1NodeServiceTest.createSystemMetadata(guid5, session.getSubject(), object1);
        MNodeService.getInstance(request).create(session, guid5, object1, sysmeta5);
        //Test the generating object succeeded.
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid5);
        assertEquals(metadata.getIdentifier(), guid5);
        assertEquals(metadata.getSize(), sysmeta5.getSize());


        //Setting p5 obsoletes p1 fails since p2 already obsoletes p1
        sysmeta5.setObsoletes(guid1);
        serialVersion = metadata.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta5.setSerialVersion(serialVersion);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid5, sysmeta5);
            fail("We shouldn't get there");
        } catch (Exception e) {
            e.printStackTrace();

        }


        //Setting p5 obsoletedBy p2 fails since p1 already is obsoletedBy p2
        sysmeta5.setObsoletes(null);
        sysmeta5.setObsoletedBy(guid2);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid5, sysmeta5);
            fail("We shouldn't get there");
        } catch (Exception e) {
            e.printStackTrace();

        }


        //Setting p5 obsoletes p2 succeeds since the obsoletes of p5 is null and noone obsoletes
        // p2.
        sysmeta5.setObsoletedBy(null);
        sysmeta5.setObsoletes(guid2);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid5, sysmeta5);
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid5);
        assertEquals(metadata.getIdentifier(), guid5);
        assertEquals(metadata.getObsoletes(), guid2);
        assertNull(metadata.getObsoletedBy());
        assertEquals(metadata.getChecksum().getValue(), sysmeta5.getChecksum().getValue());


        //Setting p2 obsoletedBy p5 succeeds since the obsoletedBy of p2 is null and p5 doesn't
        // obsolete anything
        sysmeta2.setObsoletes(guid1);
        sysmeta2.setObsoletedBy(guid5);
        serialVersion = sysmeta2.getSerialVersion();
        serialVersion = serialVersion.add(BigInteger.ONE);
        sysmeta2.setSerialVersion(serialVersion);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, sysmeta2);
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        assertEquals(metadata.getIdentifier(), guid2);
        assertEquals(metadata.getObsoletes(), guid1);
        assertEquals(metadata.getObsoletedBy(), guid5);
        assertEquals(metadata.getChecksum().getValue(), sysmeta2.getChecksum().getValue());
    }

    /**
     * Test to create a metacat object which uses the isotc211 noaa variant.
     *
     * @throws Exception
     */
    @Test
    public void testCreateNOAAObject() throws Exception {
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testNoaa." + System.currentTimeMillis());
        InputStream object = new FileInputStream("test/sciencemetadata-noaa.xml");
        InputStream sysmetaInput = new FileInputStream("test/sysmeta-noaa.xml");
        SystemMetadata sysmeta =
            TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmetaInput);
        sysmeta.setIdentifier(guid);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertEquals(pid.getValue(), guid.getValue());
    }


    /**
     * Test the permission control on the updateSystemMetadata method
     *
     * @throws Exception
     */
    @Test
    public void testPermissionOfUpdateSystemmeta() throws Exception {
        String str = "object1";

        Date date = new Date();
        Thread.sleep(2000);
        //insert a test document
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(d1NodeTest.generateDocumentId());
        InputStream object1 = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
        MNodeService.getInstance(request).create(session, guid, object1, sysmeta);
        //Test the generating object succeeded.
        SystemMetadata metadata =
            MNodeService.getInstance(request).getSystemMetadata(session, guid);

        //Test cn session -success
        Session cnSession = d1NodeTest.getCNSession();
        MNodeService.getInstance(request).updateSystemMetadata(cnSession, guid, metadata);

        //Test mn session - success
        Session mnSession = d1NodeTest.getMNSession();
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        MNodeService.getInstance(request).updateSystemMetadata(mnSession, guid, metadata);

        //Test the owner session -success
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);

        //Test another session -failed
        Session anotherSession = d1NodeTest.getAnotherSession();
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(anotherSession, guid, metadata);
            fail("Another user can't update the system metadata");
        } catch (NotAuthorized e) {

        }

    }


    /**
     * Test if the updateSystemmetadata method can catch the circular obsoletes fields chain
     */
    @Test
    public void testUpdateSystemMetadataWithCircularObsoletesChain() throws Exception {

        Date date = new Date();
        Thread.sleep(1000);
        String str = "object1";
        //insert a test document
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(d1NodeTest.generateDocumentId());
        InputStream object1 = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
        MNodeService.getInstance(request).create(session, guid, object1, sysmeta);

        //Test the generating object succeeded.
        SystemMetadata metadata =
            MNodeService.getInstance(request).getSystemMetadata(session, guid);
        Thread.sleep(1000);
        Identifier guid1 = new Identifier();
        guid1.setValue(d1NodeTest.generateDocumentId());
        object1 = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid1, session.getSubject(), object1);
        MNodeService.getInstance(request).create(session, guid1, object1, sysmeta);
        //Test the generating object succeeded.
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid1);

        Thread.sleep(1000);
        Identifier guid2 = new Identifier();
        guid2.setValue(d1NodeTest.generateDocumentId());
        object1 = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object1);
        MNodeService.getInstance(request).create(session, guid2, object1, sysmeta);
        //Test the generating object succeeded.
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);

        // test to create a circular obsoletes chain: guid obsoletes guid
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        metadata.setObsoletes(guid);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata which the obsoletes field is itself");
        } catch (InvalidRequest e) {
            assertTrue(
                "The update system metadata should fail and the error message should have the "
                    + "wording - circular chain", e.getMessage().contains("a circular chain"));
        }

        // guid obsolete guid1
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        metadata.setObsoletes(guid1);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);

        // guid1 obsoletedBy guid
        // guid1  obsoletes  guid2
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid1);
        metadata.setObsoletes(guid2);
        metadata.setObsoletedBy(guid);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid1, metadata);

        // crete a circular obsolete chain:
        // guid2 obsoletes guid
        //guid2 obsoletedBy guid1
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        metadata.setObsoletes(guid);
        metadata.setObsoletedBy(guid1);

        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid2, metadata);
            fail("We can't update the system metadata which has a circular obsoletes chain");
        } catch (InvalidRequest e) {
            assertTrue(
                "The update system metadata should fail and the error message should have the "
                    + "wording - circular chain", e.getMessage().contains("a circular chain"));
        }

        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        metadata.setObsoletedBy(guid1);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, metadata);
    }


    /**
     * Test if the updateSystemmetadata method can catch the circular obsoletedBy chain
     */
    @Test
    public void testUpdateSystemMetadataWithCircularObsoletedByChain() throws Exception {

        Date date = new Date();
        Thread.sleep(1000);
        String str = "object1";
        //insert a test document
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(d1NodeTest.generateDocumentId());
        InputStream object1 = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
        MNodeService.getInstance(request).create(session, guid, object1, sysmeta);

        //Test the generating object succeeded.
        SystemMetadata metadata =
            MNodeService.getInstance(request).getSystemMetadata(session, guid);
        Thread.sleep(1000);
        Identifier guid1 = new Identifier();
        guid1.setValue(d1NodeTest.generateDocumentId());
        object1 = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid1, session.getSubject(), object1);
        MNodeService.getInstance(request).create(session, guid1, object1, sysmeta);
        //Test the generating object succeeded.
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid1);

        Thread.sleep(1000);
        Identifier guid2 = new Identifier();
        guid2.setValue(d1NodeTest.generateDocumentId());
        object1 = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object1);
        MNodeService.getInstance(request).create(session, guid2, object1, sysmeta);
        //Test the generating object succeeded.
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);


        // guid obsolete guid1
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        metadata.setObsoletes(guid1);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);

        // guid1  obsoletes  guid2
        // guid1 obsoletedBy guid1 (a circular chain)
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid1);
        metadata.setObsoletes(guid2);
        metadata.setObsoletedBy(guid1);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid1, metadata);
            fail("We can't update the system metadata which the obsoletes field is itself");
        } catch (InvalidRequest e) {
            assertTrue(
                "The update system metadata should fail and the error message should have the "
                    + "wording - circular chain", e.getMessage().contains("a circular chain"));
        }


        // guid1  obsoletes  guid2
        // guid1 obsoletedBy guid
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid1);
        metadata.setObsoletes(guid2);
        metadata.setObsoletedBy(guid);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid1, metadata);


        //guid2 obsoletedBy guid1
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        metadata.setObsoletedBy(guid1);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, metadata);


        //guid2 obsoletedBy guid1
        //guid1 obsoletedBy guid
        //guid  obsoletedBy guid2 (created a circle)
        metadata = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        metadata.setObsoletedBy(guid2);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata which has a circular obsoletes chain");
        } catch (InvalidRequest e) {
            assertTrue(
                "The update system metadata should fail and the error message should have the "
                    + "wording - circular chain", e.getMessage().contains("a circular chain"));
        }

    }

    /**
     * Test the scenarios to update immutable fields on system metadata
     * @throws Exception
     */
    @Test
    public void testUpdateSystemMetadataImmutableFields() throws Exception {
        Date date = new Date();
        Thread.sleep(1000);
        String str = "object1";
        //insert a test document
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(d1NodeTest.generateDocumentId());
        InputStream object1 = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
        Identifier sid = new Identifier();
        sid.setValue(d1NodeTest.generateDocumentId());
        sysmeta.setSeriesId(sid);
        MNodeService.getInstance(request).create(session, guid, object1, sysmeta);

        //Test the generating object succeeded.
        SystemMetadata metadata =
            MNodeService.getInstance(request).getSystemMetadata(session, guid);
        Thread.sleep(1000);

        //check identifier
        Identifier newId = new Identifier();
        newId.setValue("newValue123456newValuedfdfasdfasdfasdfcbsrtddf");
        metadata.setIdentifier(newId);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata which has new identifier");
        } catch (InvalidRequest e) {
            //System.out.println("Error 1- "+e.getMessage());
            assertTrue(
                "The update system metadata should fail since the identifier was changed on the "
                    + "system metadata.", e.getMessage().contains(newId.getValue()));
        }

        metadata.setIdentifier(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata whose identifier is null");
        } catch (InvalidRequest e) {
            assertTrue("The update system metadata should fail since the identifier is null on the "
                           + "system metadata", e.getMessage().contains("shouldn't be null"));
        }
        metadata.setIdentifier(guid);//reset back the identifier

        ObjectFormatIdentifier formatId = metadata.getFormatId();
        metadata.setFormatId(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata whose format id is null");
        } catch (InvalidRequest e) {
            assertTrue(
                "The update system metadata should fail since the format id is null on the system"
                    + " metadata", e.getMessage().contains("The formatId field "));
        }

        metadata.setFormatId(formatId);//reset the format id

        Subject rightsHolder = metadata.getRightsHolder();
        metadata.setRightsHolder(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata whose rights holder is null");
        } catch (InvalidRequest e) {
            assertTrue(
                "The update system metadata should fail since the righs holder  is null on the "
                    + "system metadata", e.getMessage().contains("The rightsHolder field "));
        }

        //change to a new rightsHolder
        Subject newRightsHolder = new Subject();
        newRightsHolder.setValue("newSubject");
        metadata.setRightsHolder(newRightsHolder);

        BigInteger size = metadata.getSize();
        BigInteger newSize = new BigInteger("4");
        metadata.setSize(newSize);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its size was changed");
        } catch (InvalidRequest e) {
        }

        metadata.setSize(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its size null");
        } catch (InvalidRequest e) {
        }

        metadata.setSize(size); //reset it back

        Checksum check = metadata.getChecksum();
        String originalChecksumAlgorithm = check.getAlgorithm();
        String originalValue = check.getValue();
        Checksum newCheck = new Checksum();
        newCheck.setValue("12345");
        newCheck.setAlgorithm(originalChecksumAlgorithm);
        metadata.setChecksum(newCheck);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its checksum was changed");
        } catch (InvalidRequest e) {
            assertTrue("The update system metadata should fail since the checksum was changed",
                       e.getMessage().contains("12345"));
        }

        metadata.setChecksum(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its checksum is null");
        } catch (InvalidRequest e) {
            assertTrue("The update system metadata should fail since the checksum was changed",
                       e.getMessage().contains("checksum"));
        }

        //change the checksum algorithm
        newCheck = new Checksum();
        newCheck.setValue(originalValue);
        newCheck.setAlgorithm("SHA-256");
        metadata.setChecksum(newCheck);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its checksum was changed");
        } catch (InvalidRequest e) {
            assertTrue(
                "The update system metadata should fail since the checksum algorithm was changed",
                e.getMessage().contains("SHA-256"));
        }

        //change the checksum algorithm
        newCheck = new Checksum();
        newCheck.setValue(originalValue);
        newCheck.setAlgorithm(null);
        metadata.setChecksum(newCheck);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its checksum was changed");
        } catch (InvalidRequest e) {
            assertTrue(
                "The update system metadata should fail since the checksum algorithm was changed",
                e.getMessage().contains("algorithm"));
        }

        metadata.setChecksum(check);

        Subject submitter = metadata.getSubmitter();
        metadata.setSubmitter(newRightsHolder);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its submitter was changed");
        } catch (InvalidRequest e) {
        }
        metadata.setSubmitter(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its submitter is null");
        } catch (InvalidRequest e) {
        }

        metadata.setSubmitter(submitter);

        Date uploadDate = metadata.getDateUploaded();
        metadata.setDateUploaded(new Date());
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its upload date was changed");
        } catch (InvalidRequest e) {

        }
        metadata.setDateUploaded(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its upload date is null");
        } catch (InvalidRequest e) {
        }

        metadata.setDateUploaded(uploadDate);

        NodeReference node = metadata.getOriginMemberNode();
        NodeReference newNode = new NodeReference();
        newNode.setValue("newNode");
        metadata.setOriginMemberNode(newNode);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its original node was changed");
        } catch (InvalidRequest e) {
        }
        metadata.setOriginMemberNode(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its original node is null");
        } catch (InvalidRequest e) {
        }
        metadata.setOriginMemberNode(node);

        Identifier newSid = new Identifier();
        newSid.setValue("newSid123adfadffadfieredfesllkiju898765");
        metadata.setSeriesId(newSid);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its series id was changed");
        } catch (InvalidRequest e) {
        }
        metadata.setSeriesId(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
            fail("We can't update the system metadata since its series id is null");
        } catch (InvalidRequest e) {
        }

        metadata.setSeriesId(sid);

        metadata.setArchived(true);
        AccessPolicy policy = new AccessPolicy();
        AccessRule allow = new AccessRule();
        allow.addPermission(Permission.CHANGE_PERMISSION);
        allow.addSubject(rightsHolder);
        policy.addAllow(allow);
        metadata.setAccessPolicy(policy);
        //successfully update system metadata when the rights holder and access policy were changed
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata);
    }

    /**
     * Test the scenario to update authoritative member node field in system metadata
     * @throws Exception
     */
    @Test
    public void testUpdateAuthoritativeMN() throws Exception {
        String str1 = "object1";
        Date date = new Date();
        Thread.sleep(2000);
        //insert test documents with a series id
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(d1NodeTest.generateDocumentId());
        InputStream object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
        String sid1 = "sid." + System.nanoTime();
        Identifier seriesId = new Identifier();
        seriesId.setValue(sid1);
        System.out.println("the first sid is " + seriesId.getValue());
        sysmeta.setSeriesId(seriesId);
        sysmeta.setArchived(false);
        MNodeService.getInstance(request).create(session, guid, object1, sysmeta);
        //Test the generating object succeeded.
        SystemMetadata metadata =
            MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertEquals(metadata.getIdentifier(), guid);
        assertEquals(false, (boolean) metadata.getArchived());
        System.out.println("the checksum from request is " + metadata.getChecksum().getValue());
        assertEquals(metadata.getSize(), sysmeta.getSize());
        System.out.println("the identifier is " + guid.getValue());

        System.out.println("the identifier is ----------------------- " + guid.getValue());
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmeta);
        SystemMetadata metadata2 =
            MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
        assertEquals(metadata2.getIdentifier(), guid);
        assertEquals(metadata2.getSeriesId(), seriesId);
        assertFalse((boolean) metadata2.getArchived());
        NodeReference oldValue = metadata2.getAuthoritativeMemberNode();

        NodeReference newNode = new NodeReference();
        newNode.setValue("newNode");
        metadata2.setAuthoritativeMemberNode(newNode);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata2);
            fail("The updateSystemMeta should fail since it tried to update the authoritative "
                     + "member node");
        } catch (InvalidRequest e) {
            assertTrue(e.getMessage().contains(newNode.getValue()));
        } catch (NotAuthorized ee) {

        }
        metadata2.setAuthoritativeMemberNode(null);
        try {
            MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata2);
            fail("The updateSystemMeta should fail since it tried to update the authoritative "
                     + "member node");
        } catch (InvalidRequest e) {

        } catch (NotAuthorized ee) {

        }
        metadata2.setAuthoritativeMemberNode(oldValue);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, metadata2);
        SystemMetadata metadata3 =
            MNodeService.getInstance(request).getSystemMetadata(session, seriesId);
        assertEquals(metadata3.getIdentifier(), guid);
        assertEquals(metadata3.getSeriesId(), seriesId);

    }

    /**
     * Test the scenario to handle valid ids
     * @throws Exception
     */
    @Test
    public void testInvalidIds() throws Exception {
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testCreate.\t" + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        try {
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("MNodeService should reject identifier with a whitespace");
        } catch (InvalidRequest e) {

        }

        guid.setValue("testCreate. " + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        try {
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("MNodeService should reject identifier with a whitespace");
        } catch (InvalidRequest e) {

        }

        guid.setValue("testCreate." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);

        Identifier newPid = new Identifier();
        newPid.setValue(
            "testUpdate. " + (System.currentTimeMillis() + 1)); // ensure different from original
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata newSysMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
        try {
            Identifier updatedPid =
                MNodeService.getInstance(request).update(session, guid, object, newPid, newSysMeta);
            fail("MNodeService should reject identifier with a whitespace");
        } catch (InvalidRequest e) {

        }

        newPid.setValue(
            "testUpdate.\f" + (System.currentTimeMillis() + 1)); // ensure different from original
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        newSysMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
        try {
            Identifier updatedPid =
                MNodeService.getInstance(request).update(session, guid, object, newPid, newSysMeta);
            fail("MNodeService should reject identifier with a whitespace");
        } catch (InvalidRequest e) {

        }

    }

    /**
     * Test if the allow submitter list works.
     *
     * @throws Exception
     */
    @Test
    public void testAllowList() throws Exception {
        D1NodeServiceTest.printTestHeader("testAllowList");
        //Get original value of allow list
        String originalAllowedSubmitterString =
            PropertyService.getProperty("auth.allowedSubmitters");
        String group = "CN=knb-data-admins,DC=dataone,DC=org";
        PropertyService.setPropertyNoPersist("auth.allowedSubmitters", group);
        String newAllowedSubmitterString = PropertyService.getProperty("auth.allowedSubmitters");
        AuthUtil.populateAllowedSubmitters();
        //Using test session should fail
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testAllowList." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        try {
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("testAllowList - the test session shouldn't be allowed to create an object");
        } catch (Exception e) {
            assertTrue("The exception should be NotAuthorized", e instanceof NotAuthorized);
        }
        //use a session with the subject of the MN to create an object
        Subject subject = new Subject();
        subject.setValue(TEST_MN_SUBJECT);
        session.setSubject(subject);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertEquals(pid.getValue(), guid.getValue());

        //restore the original setting
        PropertyService.setPropertyNoPersist("auth.allowedSubmitters",
                                             originalAllowedSubmitterString);
        AuthUtil.populateAllowedSubmitters();
    }

    /**
     * Test create and update json-ld objects
     *
     * @throws Exception
     */
    @Test
    public void testInsertJson_LD() throws Exception {
        D1NodeServiceTest.printTestHeader("testInsertJson_LD");

        ObjectFormatIdentifier formatid = new ObjectFormatIdentifier();
        formatid.setValue(NonXMLMetadataHandlers.JSON_LD);

        //create a json-ld object successfully
        File temp1 = JsonLDHandlerTest.generateTmpFile("temp-json-ld-valid");
        InputStream input = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        OutputStream out = new FileOutputStream(temp1);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testInsertJson_LD." + System.currentTimeMillis());
        InputStream object = new FileInputStream(temp1);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatid);
        object.close();
        Checksum checksum = null;
        DetailedFileInputStream data = new DetailedFileInputStream(temp1, checksum);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, data, sysmeta);
        SystemMetadata result = MNodeService.getInstance(request).getSystemMetadata(session, pid);
        assertEquals(result.getIdentifier(), guid);
        data.close();
        temp1.delete();

        //fail to update the object since the new object is an invalid json-ld object
        File temp2 = JsonLDHandlerTest.generateTmpFile("temp-json-ld-valid");
        input = new FileInputStream(new File(JsonLDHandlerTest.INVALID_JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp2);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        Identifier newPid = new Identifier();
        newPid.setValue(
            "testInsertJson_LD_2." + (System.currentTimeMillis() + 1)); //ensure different from orig
        object = new FileInputStream(temp2);
        SystemMetadata newMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
        newMeta.setFormatId(formatid);
        object.close();
        data = new DetailedFileInputStream(temp2, newMeta.getChecksum());
        try {
            MNodeService.getInstance(request).update(session, pid, data, newPid, newMeta);
            fail("we shouldn't get here since the new object is an invalid json-ld file");
        } catch (Exception e) {
            System.out.println("the message is +++++++++++ " + e.getMessage());
            assertTrue(e instanceof InvalidRequest);
        }
        data.close();
        temp2.delete();

        //successfully update the object
        File temp3 = JsonLDHandlerTest.generateTmpFile("temp-json-ld-valid");
        input = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp3);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        newPid = new Identifier();
        newPid.setValue(
            "testInsertJson_LD_2." + (System.currentTimeMillis() + 1)); //ensure different from orig
        object = new FileInputStream(temp3);
        newMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
        newMeta.setFormatId(formatid);
        object.close();
        data = new DetailedFileInputStream(temp3, newMeta.getChecksum());
        MNodeService.getInstance(request).update(session, pid, data, newPid, newMeta);
        data.close();
        result = MNodeService.getInstance(request).getSystemMetadata(session, newPid);
        assertEquals(result.getIdentifier(), newPid);
        temp3.delete();

        //failed to create an object since it is an invalid json-ld object
        File temp4 = JsonLDHandlerTest.generateTmpFile("temp-json-ld-valid");
        input = new FileInputStream(new File(JsonLDHandlerTest.INVALID_JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp4);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        newPid = new Identifier();
        newPid.setValue("testInsertJson_LD_3." + (System.currentTimeMillis()));
        object = new FileInputStream(temp4);
        newMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
        newMeta.setFormatId(formatid);
        object.close();
        data = new DetailedFileInputStream(temp4, newMeta.getChecksum());
        try {
            MNodeService.getInstance(request).create(session, newPid, data, newMeta);
            fail("we shouldn't get here since the object is an invalid json-ld file");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        data.close();
        temp4.delete();
    }

    /**
     * Test the event log behavior in the create and update methods.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndUpdateEventLog() throws Exception {
        D1NodeServiceTest.printTestHeader("testInsertJson_LD");

        Session session = d1NodeTest.getTestSession();

        //a data file
        Identifier guid = new Identifier();
        guid.setValue("dataTestCreateAndUpdateEventLog." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        ResultSet result = getEventLogs(guid);
        assertTrue(result.next());
        assertEquals("create", result.getString(1));
        assertFalse(result.next());
        result.close();

        Identifier guid2 = new Identifier();
        guid2.setValue("dataTestCreateAndUpdateEventLog2." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        MNodeService.getInstance(request).update(session, guid, object, guid2, sysmeta2);
        result = getEventLogs(guid2);
        assertTrue(result.next());
        assertEquals("update", result.getString(1));
        assertFalse(result.next());
        result.close();

        // a non-xml metadata
        ObjectFormatIdentifier formatid = new ObjectFormatIdentifier();
        formatid.setValue(NonXMLMetadataHandlers.JSON_LD);
        Identifier guid3 = new Identifier();
        guid3.setValue("nonXmlMetadataTestCreateAndUpdateEventLog." + System.currentTimeMillis());
        object = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        SystemMetadata sysmeta3 = D1NodeServiceTest.createSystemMetadata(guid3, session.getSubject(), object);
        sysmeta3.setFormatId(formatid);
        object.close();
        object = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        MNodeService.getInstance(request).create(session, guid3, object, sysmeta3);
        object.close();
        result = getEventLogs(guid3);
        assertTrue(result.next());
        assertEquals("create", result.getString(1));
        assertFalse(result.next());
        result.close();

        Identifier guid4 = new Identifier();
        guid4.setValue("nonXmlMetadataTestCreateAndUpdateEventLog23." + System.currentTimeMillis());
        object = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        SystemMetadata sysmeta4 = D1NodeServiceTest.createSystemMetadata(guid4, session.getSubject(), object);
        sysmeta4.setFormatId(formatid);
        object.close();
        object = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        MNodeService.getInstance(request).update(session, guid3, object, guid4, sysmeta4);
        object.close();
        result = getEventLogs(guid4);
        assertTrue(result.next());
        assertEquals("update", result.getString(1));
        assertFalse(result.next());
        result.close();


        // an ISO file
        formatid = new ObjectFormatIdentifier();
        formatid.setValue("http://www.isotc211.org/2005/gmd");
        Identifier guid7 = new Identifier();
        guid7.setValue("isoTestCreateAndUpdateEventLog." + System.currentTimeMillis());
        object = new FileInputStream(new File("test/isoTestNodc1.xml"));
        SystemMetadata sysmeta7 = D1NodeServiceTest.createSystemMetadata(guid7, session.getSubject(), object);
        sysmeta7.setFormatId(formatid);
        object.close();
        object = new FileInputStream(new File("test/isoTestNodc1.xml"));
        MNodeService.getInstance(request).create(session, guid7, object, sysmeta7);
        object.close();
        result = getEventLogs(guid7);
        assertTrue(result.next());
        assertEquals("The event of log should be create", "create", result.getString(1));
        assertFalse(result.next());
        result.close();

        Thread.sleep(10000);
        Identifier guid8 = new Identifier();
        guid8.setValue("isoTestCreateAndUpdateEventLog2." + System.currentTimeMillis());
        object = new FileInputStream(new File("test/isoTestNodc1.xml"));
        SystemMetadata sysmeta8 = D1NodeServiceTest.createSystemMetadata(guid8, session.getSubject(), object);
        sysmeta8.setFormatId(formatid);
        object.close();
        object = new FileInputStream(new File("test/isoTestNodc1.xml"));
        MNodeService.getInstance(request).update(session, guid7, object, guid8, sysmeta8);
        object.close();
        result = getEventLogs(guid8);
        assertTrue(result.next());
        assertEquals("update", result.getString(1));
        assertFalse(result.next());
        result.close();
    }

    /**
     * Get the result set of the event logs for the given identifier
     */
    private ResultSet getEventLogs(Identifier guid) throws Exception {
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pStmt = null;
        ResultSet result = null;
        String docId = IdentifierManager.getInstance().getLocalId(guid.getValue());
        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("MNodeServiceTest.getEventLogs");
            serialNumber = conn.getCheckOutSerialNumber();
            //delete a record
            pStmt = conn.prepareStatement("select event FROM access_log WHERE docid = ? ");
            pStmt.setString(1, docId);
            pStmt.execute();
            result = pStmt.getResultSet();
        } finally {
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
        return result;
    }


    /**
     * Test to create and update object when DOI setting is disabled
     */
    @Test
    public void testCreateAndUpdateWithDoiDisabled() throws Exception {
        D1NodeServiceTest.printTestHeader("testCreateAndUpdateWithDoiDisabled");
        String originDOIstatusStr = PropertyService.getProperty("guid.doi.enabled");
        System.out.println("the dois status is ++++++++++++++ " + originDOIstatusStr);
        try {
            Session session = d1NodeTest.getTestSession();
            PropertyService.setPropertyNoPersist("guid.doi.enabled", "false");//disable doi
            DOIServiceFactory.getDOIService().refreshStatus();
            try {
                //make sure the service of doi is disabled
                MNodeService.getInstance(request).generateIdentifier(session, "doi", null);
                fail("we shouldn't get here since generating doi should fail when the feature is "
                         + "disabled");
            } catch (Exception e) {
                assertTrue(e instanceof ServiceFailure);
                assertTrue(e.getMessage().contains("DOI scheme is not enabled at this node"));
            }
            Identifier guid = new Identifier();
            guid.setValue("testCreateAndUpdateWithDoiDisabled." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier newPid = new Identifier();
            newPid.setValue(
                "testCreateAndUpdateWithDoiDisabled-2." + (System.currentTimeMillis() + 1)); //
            // ensure it is different from original
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            SystemMetadata getSysMeta =
                MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertEquals(pid.getValue(), getSysMeta.getIdentifier().getValue());

            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata newSysMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
            // do the update
            Identifier updatedPid =
                MNodeService.getInstance(request).update(session, pid, object, newPid, newSysMeta);

            // get the updated system metadata
            SystemMetadata updatedSysMeta =
                MNodeService.getInstance(request).getSystemMetadata(session, updatedPid);
            assertEquals(updatedPid.getValue(), updatedSysMeta.getIdentifier().getValue());

            try {
                //publish will fail too
                MNodeService.getInstance(request).publish(session, updatedPid);
                fail("we shouldn't get here since publishing should fail when the feature is "
                         + "disabled");
            } catch (Exception e) {
                assertTrue(e instanceof ServiceFailure);
                assertTrue(e.getMessage().contains("DOI scheme is not enabled at this node"));
            }
        } finally {
            PropertyService.setPropertyNoPersist("guid.doi.enabled", originDOIstatusStr);
            DOIServiceFactory.getDOIService().refreshStatus();
        }
    }

    /**
     * Test object creation FGDC objects
     */
    @Test
    public void testCreateAndUpdateFGDC() throws Exception {
        D1NodeServiceTest.printTestHeader("testCreateAndUpdateFGDC");
        ObjectFormatIdentifier format = new ObjectFormatIdentifier();
        format.setValue("FGDC-STD-001-1998");
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testCreateAndUpdateFGDC." + System.currentTimeMillis());
        InputStream object = new FileInputStream("test/fgdc.xml");
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(format);
        object.close();
        object = new FileInputStream("test/fgdc.xml");
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertEquals(guid.getValue(), pid.getValue());
        object.close();

        Thread.sleep(2000);
        Identifier guid2 = new Identifier();
        guid2.setValue("testCreateAndUpdateFGDC2." + System.currentTimeMillis());
        object = new FileInputStream("test/fgdc.xml");
        SystemMetadata sysmeta2 = D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object);
        object.close();
        sysmeta2.setFormatId(format);
        object = new FileInputStream("test/fgdc.xml");
        MNodeService.getInstance(request).update(session, guid, object, guid2, sysmeta2);
        object.close();
    }


    /**
     * Test the reindex api
     *
     * @throws Exception
     */
    @Test
    public void testReindex() throws Exception {
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testReindex." + System.currentTimeMillis());
        InputStream object =
            new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta.setFormatId(formatId);
        object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeTest.mnCreate(session, guid, object, sysmeta);
        //Make sure the metadata objects have been indexed
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        String version = FailedIndexResubmitTimerTaskIT.getSolrDocVersion(resultStr);
        //second object
        Identifier guid1 = new Identifier();
        guid1.setValue("testCreateFailure." + System.currentTimeMillis());
        object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid1, session.getSubject(), object);
        object.close();
        sysmeta.setFormatId(formatId);
        object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeTest.mnCreate(session, guid1, object, sysmeta);
        //Make sure the metadata objects have been indexed
        String query1 = "q=id:" + guid1.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query1);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query1);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        String version1 = FailedIndexResubmitTimerTaskIT.getSolrDocVersion(resultStr);

        List<Identifier> identifiers = new ArrayList<Identifier>();
        identifiers.add(guid);
        identifiers.add(guid1);
        try {
            MNodeService.getInstance(request).reindex(session, identifiers);
            fail("We shouldn't get here since the session doesn't have permission to reindex.");
        } catch (NotAuthorized e) {
            assertTrue(e.getMessage().contains(session.getSubject().getValue()));
        }

        try {
            MNodeService.getInstance(request).reindex(session, identifiers);
            fail("We shouldn't get here since the session doesn't have permission to reindex.");
        } catch (NotAuthorized e) {
            assertTrue(e.getMessage().contains(session.getSubject().getValue()));
        }

        Session adminSession = d1NodeTest.getMNSession();
        MNodeService.getInstance(request).reindex(adminSession, identifiers);

        boolean versionChanged = false;
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        String newVersion = null;
        while (!versionChanged && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            newVersion = FailedIndexResubmitTimerTaskIT.getSolrDocVersion(resultStr);
            versionChanged = !newVersion.equals(version);
        }
        assertTrue(versionChanged);

        versionChanged = false;
        stream = MNodeService.getInstance(request).query(session, "solr", query1);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        String newVersion1 = null;
        while (!versionChanged && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query1);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            newVersion1 = FailedIndexResubmitTimerTaskIT.getSolrDocVersion(resultStr);
            versionChanged = !newVersion1.equals(version1);
        }
        assertTrue(versionChanged);

        // Use Mockito to test the reindexAll method
        MetacatSolrIndex solrIndex = Mockito.mock(MetacatSolrIndex.class);
        Mockito.doNothing().when(solrIndex).submit(any(Identifier.class), any(SystemMetadata.class),
                anyBoolean(), anyBoolean(),anyInt());
        MNodeService.setMetacatSolrIndex(solrIndex);
        try {
            MNodeService.getInstance(request).reindexAll(session);
            fail("The test shouldn't get there since the session doesn't have the privilege "
                        + "to reindex all objects on this instance.");
        } catch (Exception e) {
            assertTrue( e instanceof NotAuthorized);
        }
        MNodeService.getInstance(request).reindexAll(adminSession);
        Thread.sleep(100); //reindexAll is called in another thread.
        Mockito.verify(solrIndex, Mockito.atLeast(1)).submit(any(Identifier.class),
                                    any(SystemMetadata.class), anyBoolean(), anyBoolean(),anyInt());
    }

    /**
     * Test the failure of updating doi during the create method
     * @throws Exception
     */
    @Test
    public void testFailedDoiUpdateInCreate() throws Exception {
        Session session = d1NodeTest.getTestSession();
        Properties withProperties = new Properties();
        withProperties.setProperty("guid.doi.password", "foo");
        try (MockedStatic<PropertyService> ignored =
                LeanTestUtils.initializeMockPropertyService(withProperties)) {
            Identifier guid = new Identifier();
            guid.setValue("doi:10.5072/FK2meta.1." + System.currentTimeMillis());
            InputStream object =
                    new FileInputStream(MNodeReplicationTest.replicationSourceFile);
                SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
                object.close();
                ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
                formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
                sysmeta.setFormatId(formatId);
                object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
                try {
                    d1NodeTest.mnCreate(session, guid, object, sysmeta);
                } catch (Exception e) {
                    fail("The failure of updating doi shouldn't disrupt the create method.");
                }
                object.close();
        }
    }

    /**
     * Test the create method with the invalid eml objects
     * @throws Exception
     */
    @Test
    public void testCreateWithInvalidEML() throws Exception {
        D1NodeServiceTest.printTestHeader("testCreateWithInvalidEML");
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testCreateWithInvalidEML1." + System.currentTimeMillis());
        InputStream object = new FileInputStream(new File(invalidEmlPath1));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        sysmeta.setFormatId(formatId);
        object = new FileInputStream(new File(invalidEmlPath1));
        try {
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("The test cannot get here since it inserted an invalid eml object.");
        } catch(Exception e) {
            object.close();
            assertTrue("The error message must say the eml id should be unique",
                                    e.getMessage().contains("ID attributes must be unique"));
        }

        guid = new Identifier();
        guid.setValue("testCreateWithInvalidEML2." + System.currentTimeMillis());
        ObjectFormatIdentifier formatId2 = new ObjectFormatIdentifier();
        formatId2.setValue("eml://ecoinformatics.org/eml-2.1.1");
        object = new FileInputStream(new File(invalidEmlPath2));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId2);
        object.close();
        object = new FileInputStream(new File(invalidEmlPath2));
        try {
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("The test cannot get here since it inserted an invalid eml object.");
        } catch(Exception e) {
            object.close();
            assertTrue("The error message must say the elemement principal1 is invalid.",
                        e.getMessage().contains("principal1"));
        }
    }

    /**
     * Test the updateIdMetadata api
     * @throws Exception
     */
    @Test
    public void testUpdateIdMetadata() throws Exception {
        //first doi object
        Session session = d1NodeTest.getTestSession();
        Session adminSession = d1NodeTest.getMNSession();
        String emlFile = "test/eml-multiple-creators.xml";
        InputStream content;
        String scheme = "DOI";
        Identifier publishedPID1 = MNodeService.getInstance(request)
                                                    .generateIdentifier(session, scheme, null);
        content = new FileInputStream(emlFile);
        SystemMetadata sysmeta = D1NodeServiceTest
                            .createSystemMetadata(publishedPID1, session.getSubject(), content);
        content.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.1.0");
        sysmeta.setFormatId(formatId);
        content = new FileInputStream(emlFile);
        MNodeService.getInstance(request).create(session, publishedPID1, content, sysmeta);
        content.close();
        long originalUpdateTimeOfPid1 = getIdentifierUpdateDate(publishedPID1.getValue());
        //second doi object
        emlFile = "test/eml-2.2.0.xml";
        Identifier publishedPID2 = MNodeService.getInstance(request)
                                                        .generateIdentifier(session, scheme, null);
        content = new FileInputStream(emlFile);
        SystemMetadata sysmeta2 = D1NodeServiceTest
                .createSystemMetadata(publishedPID2, session.getSubject(), content);
        content.close();
        ObjectFormatIdentifier formatId1 = new ObjectFormatIdentifier();
        formatId1.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta2.setFormatId(formatId1);
        content = new FileInputStream(emlFile);
        MNodeService.getInstance(request).create(session, publishedPID2, content, sysmeta2);
        content.close();
        long originalUpdateTimeOfPid2 = getIdentifierUpdateDate(publishedPID2.getValue());
        String[] ids = null;
        String[] formats = null;
        try {
            MNodeService.getInstance(request).updateIdMetadata(adminSession, ids, formats);
            fail("Since ids and formats are null, test can't reach there.");
        } catch (Exception e) {
            assertTrue("The exception "+ e.getClass().getName() + " should be InvalidRequest",
                                                                    e instanceof InvalidRequest);
        }

        ids = new String[1];
        formats = null;
        ids[0] = publishedPID1.getValue();
        try {
            MNodeService.getInstance(request).updateIdMetadata(session, ids, formats);
            fail("Since the session is not an admin, test can't reach there.");
        } catch (Exception e) {
            assertTrue("The exception "+ e.getClass().getName() + " should be NotAuthorized",
                                                                       e instanceof NotAuthorized);
        }
        MNodeService.getInstance(request).updateIdMetadata(adminSession, ids, formats);
        long firstUpdateTimeOfPid1 = getNewDOIUpdateTime(ids[0], originalUpdateTimeOfPid1);
        assertNotEquals("The update time for "+ ids[0] + " should change", firstUpdateTimeOfPid1,
                                                                          originalUpdateTimeOfPid1);

        ids = new String[2];
        ids[0] = publishedPID1.getValue();
        ids[1] = publishedPID2.getValue();
        formats = null;
        MNodeService.getInstance(request).updateIdMetadata(adminSession, ids, formats);
        long secondUpdateTimeOfPid1 = getNewDOIUpdateTime(ids[0], firstUpdateTimeOfPid1);
        assertNotEquals("The update time for "+ ids[0] + " should change", secondUpdateTimeOfPid1,
                                                                    firstUpdateTimeOfPid1);
        long firstUpdateTimeOfPid2 = getNewDOIUpdateTime(ids[1], originalUpdateTimeOfPid2);
        assertNotEquals("The update time for "+ ids[1] + " should change", firstUpdateTimeOfPid2,
                                                                 originalUpdateTimeOfPid2);

        ids = null;
        formats = new String[2];
        formats[0] = "eml://ecoinformatics.org/eml-2.1.0";
        formats[1] = "https://eml.ecoinformatics.org/eml-2.2.0";
        MNodeService.getInstance(request).updateIdMetadata(adminSession, ids, formats);
        long thirdUpdateTimeOfPid1 =
                            getNewDOIUpdateTime(publishedPID1.getValue(), secondUpdateTimeOfPid1);
        assertNotEquals("The update time for "+ publishedPID1.getValue() + " should change",
                                                thirdUpdateTimeOfPid1, secondUpdateTimeOfPid1);
        long secondUpdateTimeOfPid2 =
                              getNewDOIUpdateTime(publishedPID2.getValue(), firstUpdateTimeOfPid2);
        assertNotEquals("The update time for "+ publishedPID2.getValue() + " should change",
                                                    secondUpdateTimeOfPid2, firstUpdateTimeOfPid2);

        ids = new String[1];
        ids[0] = publishedPID1.getValue();
        formats = new String[1];
        formats[0] = "https://eml.ecoinformatics.org/eml-2.2.0";
        MNodeService.getInstance(request).updateIdMetadata(adminSession, ids, formats);
        long fourthUpdateTimeOfPid1 =
                            getNewDOIUpdateTime(publishedPID1.getValue(), thirdUpdateTimeOfPid1);
        assertNotEquals("The update time for "+ publishedPID1.getValue() + " should change",
                                                    fourthUpdateTimeOfPid1, thirdUpdateTimeOfPid1);
        long thirdUpdateTimeOfPid2 =
                              getNewDOIUpdateTime(publishedPID2.getValue(), secondUpdateTimeOfPid2);
        assertNotEquals("The update time for "+ publishedPID2.getValue() + " should change",
                                                    thirdUpdateTimeOfPid2, secondUpdateTimeOfPid2);
    }

    /**
     * Test the updateAllIdMetadata method
     * @throws Exception
     */
    @Test
    public void testUpdateAllIdMetadata() throws Exception {
        //use mockito to test updateAllIdMetadata
        Session session = d1NodeTest.getTestSession();
        Session adminSession = d1NodeTest.getMNSession();
        UpdateDOI doiUpdater = Mockito.mock(UpdateDOI.class);
        Mockito.when(doiUpdater.upgrade()).thenReturn(true);
        MNodeService.setDOIUpdater(doiUpdater);
        try {
            MNodeService.getInstance(request).updateAllIdMetadata(session);
            fail("The test shouldn't get there since the session doesn't have the privilege "
                        + "to update the all doi's metadata");
        } catch (Exception e) {
            assertTrue( e instanceof NotAuthorized);
        }
        MNodeService.getInstance(request).updateAllIdMetadata(adminSession);
        Thread.sleep(1000); //updateAllIdMetadata is called in another thread.
        Mockito.verify(doiUpdater, Mockito.times(1)).upgrade();
    }

    /**
     * Test the scenario the identifier doesn't match the one in the system metadata
     * in the create and update methods.
     * @throws Exception
     */
    @Test
    public void testIdNotMatchSysmetaInCreateAndUpdate() throws Exception {
        Session session = d1NodeTest.getTestSession();
        //a data file
        Identifier guid = new Identifier();
        guid.setValue("testIdNotMatchSysmetaInCreateAndUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        // Set to another pid into system metadata to make it invalid
        Identifier another = new Identifier();
        another.setValue("testIdNotMatchSysmetaInCreateAndUpdate2." + System.currentTimeMillis());
        sysmeta.setIdentifier(another);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        try {
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("Test shouldn't get there since the system metadata has a different user");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest rather than "
                        + e.getClass().getName(), e instanceof InvalidRequest);
        }
        // Set back to make it valid and the create method should succeed
        sysmeta.setIdentifier(guid);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertTrue("The returned pid should be " + guid.getValue(),
                    guid.getValue().equals(pid.getValue()));
        // Update it by a wrong pid in the systemetadata
        Identifier newId = new Identifier();
        newId.setValue("testIdNotMatchSysmetaInCreateAndUpdate." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setIdentifier(another);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        try {
            MNodeService.getInstance(request).update(session, pid, object, newId, sysmeta);
            fail("Test shouldn't get there since the system metadata has a different user");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest rather than "
                        + e.getClass().getName(), e instanceof InvalidRequest);
        }
        // Set back to make it valid
        sysmeta.setIdentifier(newId);
        pid = MNodeService.getInstance(request).update(session, pid, object, newId, sysmeta);
        assertTrue("The returned pid should be " + newId.getValue(),
                            newId.getValue().equals(pid.getValue()));

        // An eml metadata object
        guid = new Identifier();
        guid.setValue("testIdNotMatchSysmetaInCreateAndUpdate." + System.currentTimeMillis());
        object = new FileInputStream(MockReplicationMNode.replicationSourceFile);
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta.setFormatId(formatId);
        // Make the system metadata not match the guid
        sysmeta.setIdentifier(another);
        object = new FileInputStream(MockReplicationMNode.replicationSourceFile);
        try {
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            fail("Test shouldn't get there since the system metadata has a different user");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest rather than "
                        + e.getClass().getName(), e instanceof InvalidRequest);
        }
        // Set back to make it valid and the create method should succeed
        sysmeta.setIdentifier(guid);
        object = new FileInputStream(MockReplicationMNode.replicationSourceFile);
        pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertTrue("The returned pid should be " + guid.getValue(),
                                guid.getValue().equals(pid.getValue()));
        // Update it by a wrong pid in the systemetadata
        newId = new Identifier();
        newId.setValue("testIdNotMatchSysmetaInCreateAndUpdate2." + System.currentTimeMillis());
        object = new FileInputStream(MockReplicationMNode.replicationSourceFile);
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        sysmeta.setIdentifier(another);
        try {
            MNodeService.getInstance(request).update(session, pid, object, newId, sysmeta);
            fail("Test shouldn't get there since the system metadata has a different user");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest rather than "
                        + e.getClass().getName(), e instanceof InvalidRequest);
        }
        // Set back to make it valid
        sysmeta.setIdentifier(newId);
        object = new FileInputStream(MockReplicationMNode.replicationSourceFile);
        pid = MNodeService.getInstance(request).update(session, pid, object, newId, sysmeta);
        assertTrue("The returned pid should be " + newId.getValue(),
                                    newId.getValue().equals(pid.getValue()));
    }

    /**
     * Get a new update time from the doi metadata, which should be different to the original one.
     * If we can't get a different update time after multiple tries, it throws an exception.
     * @param pid  the identifier of the object
     * @param original  the original update time
     * @return a new update time
     * @throws Exception
     */
    private long getNewDOIUpdateTime(String pid, long original) throws Exception {
        long newTime = getIdentifierUpdateDate(pid);
        int count = 0;
        while (newTime == original && count < D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(600);
            count++;
            newTime = getIdentifierUpdateDate(pid);
        }
        if (newTime == original) {
            throw new Exception("We can't get a different update time for pid " + pid);
        }
        return newTime;
    }

    /**
     * Get the update time of the given pid on the doi metadata
     * @param pid
     * @return
     * @throws Exception
     */
    private long getIdentifierUpdateDate(String pid) throws Exception {
        String ezidUsername = PropertyService.getProperty("guid.doi.username");
        String ezidPassword = PropertyService.getProperty("guid.doi.password");
        String ezidServiceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
        EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
        ezid.login(ezidUsername, ezidPassword);
        int count = 0;
        HashMap<String, String> metadata;
        do {
            metadata = ezid.getMetadata(pid);
            Thread.sleep(600);
            count++;
        } while ((metadata == null || metadata.get(EzidDOIService.DATACITE) == null) &&
                                                               count < D1NodeServiceTest.MAX_TRIES);
        assertNotNull(metadata);
        String result = metadata.get(EzidDOIService.DATACITE);
        assertTrue(result.contains("<creatorName>"));
        String updateTime = metadata.get("_updated");
        return Long.parseLong(updateTime);
    }
}
