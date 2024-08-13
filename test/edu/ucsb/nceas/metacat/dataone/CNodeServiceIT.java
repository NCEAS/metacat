package edu.ucsb.nceas.metacat.dataone;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.storage.ObjectInfo;
import edu.ucsb.nceas.metacat.systemmetadata.ChecksumsManager;
import org.dataone.client.v2.formats.ObjectFormatCache;
import edu.ucsb.nceas.metacat.object.handler.JsonLDHandlerTest;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
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
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A JUnit test for testing the dataone CNCore implementation
 */
public class CNodeServiceIT {

    private final D1NodeServiceTest d1NodeServiceTest;
    /**
     * constructor for the test
     */
    public CNodeServiceIT() {
        d1NodeServiceTest = new D1NodeServiceTest("CNodeServiceTest");
    }

    @Before
    public void setUp() throws Exception {
        d1NodeServiceTest.setUp();
    }

    @After
    public void tearDown() {
        d1NodeServiceTest.tearDown();
    }

    /**
     * test for registering standalone system metadata
     */
    @Test
    public void testRegisterSystemMetadata() {

        MCTestCase.printTestHeader("testRegisterSystemMetadata");

        try {
            Session testSession = d1NodeServiceTest.getTestSession();
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testRegisterSystemMetadata." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid,
                                                                            session.getSubject(), object);
            try {
                CNodeService.getInstance(d1NodeServiceTest.request)
                    .registerSystemMetadata(testSession, guid, sysmeta);
                fail(
                    "We shouldn't get there since the test session can't register system metadata");
            } catch (NotAuthorized ee) {
                System.out.println("exception was thrown as expected");
            }
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test the scenario that to use a delete id
     * @throws Exception
     */
    @Test
    public void testReusingDeletedId() throws Exception {
        Session session = D1NodeServiceTest.getCNSession();
        Identifier deletedId = new Identifier();
        deletedId.setValue("testResuingDeletedId234." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(deletedId, session.getSubject(), object);
        d1NodeServiceTest.cnCreate(session, deletedId, object, sysmeta);
        InputStream result =
            CNodeService.getInstance(d1NodeServiceTest.getServletRequest()).get(session, deletedId);
        assertNotNull(result);
        CNodeService.getInstance(d1NodeServiceTest.getServletRequest()).delete(session, deletedId);
        try {
            CNodeService.getInstance(d1NodeServiceTest.getServletRequest()).get(session, deletedId);
            fail("Tes can't get there since the id was deleted");
        } catch (Exception e) {
            assertTrue(e instanceof NotFound);
        }
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        try {
            d1NodeServiceTest.cnCreate(session, deletedId, object, sysmeta);
            fail("Tes can't get there since mnCreate was using a deleted id");
        } catch (Exception e) {
            assertTrue(e instanceof IdentifierNotUnique);
        }

    }

    /**
     * test for getting system metadata
     */
    @Test
    public void testGetSystemMetadata() {
        MCTestCase.printTestHeader("testGetSystemMetadata");

        try {
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testGetSystemMetadata." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
            // get it
            SystemMetadata retSysmeta =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            // check it
            assertEquals(sysmeta.getIdentifier().getValue(), retSysmeta.getIdentifier().getValue());
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void testGetLogRecords() {
        MCTestCase.printTestHeader("testGetLogRecords");
        try {

            Session session = d1NodeServiceTest.getCNSession();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date fromDate = sdf.parse("2010-01-01");
            Date toDate = new Date();
            Event event = Event.CREATE;
            int start = 0;
            int count = 1;

            Log log = CNodeService.getInstance(d1NodeServiceTest.request)
                .getLogRecords(session, fromDate, toDate, event.xmlValue(), null, start, count);
            assertNotNull(log);
            assertTrue(log.getCount() == count);
            assertTrue(log.getStart() == start);
            assertTrue(log.getTotal() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void testCreate() {
        MCTestCase.printTestHeader("testCreate");

        try {
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Date originalModificationDate = sysmeta.getDateSysMetadataModified();
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            Identifier pid = d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            assertEquals(guid, pid);
            SystemMetadata readSysmeta = SystemMetadataManager.getInstance().get(pid);
            assertTrue(
                originalModificationDate.getTime() == readSysmeta.getDateSysMetadataModified()
                    .getTime());
            ChecksumsManager manager = new ChecksumsManager();
            List<Checksum> checksums = manager.get(sysmeta.getIdentifier());
            assertEquals(5, checksums.size());
            boolean found = false;
            for (Checksum checksum1 : checksums) {
                if(checksum1.getAlgorithm().equals("MD5")) {
                    assertEquals(sysmeta.getChecksum().getValue(), checksum1.getValue());
                    found = true;
                }
            }
            assertTrue("Test should find a checksum with algorithm MD5", found);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test the scenario that the pid doesn't match the one in the system metadata
     * in the create request
     * @throws Exception
     */
    @Test
    public void testPidNotMatchSysmeta() throws Exception {
        Session session = D1NodeServiceTest.getCNSession();
        //a data file
        Identifier guid = new Identifier();
        guid.setValue("testPidNotMatchSysmeta." + System.currentTimeMillis());
        String objValue =
            "testPidNotMatchSysmeta231." + System.currentTimeMillis();
        InputStream object = new ByteArrayInputStream(objValue.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        // Set to another pid into system metadata to make it invalid
        Identifier another = new Identifier();
        another.setValue("testPidNotMatchSysmeta2." + System.currentTimeMillis());
        sysmeta.setIdentifier(another);
        object = new ByteArrayInputStream(objValue.getBytes(StandardCharsets.UTF_8));
        try {
            d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            fail("Test shouldn't get there since the system metadata has a different user");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest rather than "
                           + e.getClass().getName(), e instanceof InvalidRequest);
        }
        try {
            SystemMetadata readOne = CNodeService.getInstance(d1NodeServiceTest.getServletRequest())
                .getSystemMetadata(session, guid);
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof NotFound);
        }
        try {
            SystemMetadata readOne = CNodeService.getInstance(d1NodeServiceTest.getServletRequest())
                .getSystemMetadata(session, sysmeta.getIdentifier());
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof NotFound);
        }
        try {
            InputStream data = MetacatHandler.read(guid);
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof McdbException);
        }
        try {
            InputStream data = MetacatHandler.read(sysmeta.getIdentifier());
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof McdbException);
        }

        object = new ByteArrayInputStream(objValue.getBytes(StandardCharsets.UTF_8));
        ObjectInfo objectMetadata = D1NodeServiceTest.getStorage().storeObject(object);
        D1NodeServiceTest.getStorage().tagObject(sysmeta.getIdentifier(), objectMetadata.getCid());
        try (InputStream inputStream = MetacatHandler.read(sysmeta.getIdentifier())) {
            assertNotNull(inputStream);
        }
        try {
            CNodeService.getInstance(d1NodeServiceTest.getServletRequest())
                .create(session, guid, object, sysmeta);
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        try {
            InputStream data = MetacatHandler.read(guid);
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof McdbException);
        }
        try {
            InputStream data = MetacatHandler.read(sysmeta.getIdentifier());
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof McdbException);
        }

        object = new ByteArrayInputStream(objValue.getBytes(StandardCharsets.UTF_8));
        objectMetadata = D1NodeServiceTest.getStorage()
            .storeObject(object, sysmeta.getIdentifier(), null, sysmeta.getChecksum().getValue(),
                         sysmeta.getChecksum().getAlgorithm(), sysmeta.getSize().longValue());
        try (InputStream inputStream = MetacatHandler.read(sysmeta.getIdentifier())) {
            assertNotNull(inputStream);
        }
        try {
            CNodeService.getInstance(d1NodeServiceTest.getServletRequest()).create(session, guid,
                                                                                   object, sysmeta);
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        try {
            InputStream data = MetacatHandler.read(guid);
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof McdbException);
        }
        try {
            InputStream data = MetacatHandler.read(sysmeta.getIdentifier());
            fail("Test shouldn't get there since the object wasn't created");
        } catch (Exception e) {
            assertTrue(e instanceof McdbException);
        }
    }

    @Test
    public void testGet() {
        MCTestCase.printTestHeader("testGet");

        try {
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testGet." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            Identifier pid = d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            assertEquals(guid.getValue(), pid.getValue());
            System.out.println("the pid is+++++++++++++++++++++++++" + guid.getValue());
            // get it
            InputStream retObject = CNodeService.getInstance(d1NodeServiceTest.request).get(session, pid);
            // check it
            object.reset();
            assertTrue(IOUtils.contentEquals(object, retObject));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void testChecksum() {
        MCTestCase.printTestHeader("testChecksum");

        try {
            Session session = D1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testChecksum." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            // save it
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
            // check it
            Checksum checksum = CNodeService.getInstance(d1NodeServiceTest.request).getChecksum(session, guid);
            assertEquals(sysmeta.getChecksum().getValue(), checksum.getValue());
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void testListNodes() {
        MCTestCase.printTestHeader("testListNodes");

        try {
            CNodeService.getInstance(d1NodeServiceTest.request).listNodes();
        } catch (NotImplemented e) {
            // expecting not implemented
            assertTrue(true);
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void testReserveIdentifier() {
        MCTestCase.printTestHeader("testReserveIdentifier");

        try {
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testReserveIdentifier." + System.currentTimeMillis());
            // reserve it
            Identifier resultPid =
                CNodeService.getInstance(d1NodeServiceTest.request).reserveIdentifier(session, guid);
            assertNotNull(resultPid);
            assertEquals(guid.getValue(), resultPid.getValue());
        } catch (NotImplemented ni) {
            // this is not implemented in Metacat
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void testSearch() {
        MCTestCase.printTestHeader("testSearch");

        try {
            Session session = d1NodeServiceTest.getCNSession();

            // search for objects, but expect a NotImplemented exception
            try {
                ObjectList objectList =
                    CNodeService.getInstance(d1NodeServiceTest.request).search(session, null, null);
            } catch (NotImplemented ne) {
                assertTrue(true);
                return;
            }
            fail("Metacat should not implement CN.search");

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void testSetOwner() {
        MCTestCase.printTestHeader("testSetOwner");

        try {
            //v2 mn should fail
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testSetOwner." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V1MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);
            long serialVersion = 1L;
            // save it
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
            Subject rightsHolder = new Subject();
            rightsHolder.setValue("newUser");
            // set it
            Identifier retPid = CNodeService.getInstance(d1NodeServiceTest.request)
                .setRightsHolder(session, guid, rightsHolder, serialVersion);
            assertEquals(guid, retPid);
            // get it
            sysmeta = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            assertNotNull(sysmeta);
            // check it
            assertTrue(rightsHolder.equals(sysmeta.getRightsHolder()));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
        try {
            //v2 mn should fail
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testSetOwner." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V2MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);
            long serialVersion = 1L;
            // save it
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
            Subject rightsHolder = new Subject();
            rightsHolder.setValue("newUser");
            // set it
            Identifier retPid = CNodeService.getInstance(d1NodeServiceTest.request)
                .setRightsHolder(session, guid, rightsHolder, serialVersion);
            assertEquals(guid, retPid);
            // get it
            sysmeta = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            assertNotNull(sysmeta);
            // check it
            assertTrue(rightsHolder.equals(sysmeta.getRightsHolder()));

        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof NotAuthorized) {
                assertTrue(e.getMessage().contains(
                    "The Coordinating Node is not authorized to make systemMetadata changes"));
            } else {
                fail("Unexpected error: " + e.getMessage());
            }
        }
    }

    @Test
    public void testSetAccessPolicy() {
        MCTestCase.printTestHeader("testSetAccessPolicy");


        try {
            // trys to set access policy on an object whose authortiative memeber node is MNRead
            // v2. It should fail.
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testSetAccessPolicy." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V1MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);

            long serialVersion = 1L;

            // save it
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
            AccessPolicy accessPolicy = new AccessPolicy();
            AccessRule accessRule = new AccessRule();
            accessRule.addPermission(Permission.WRITE);
            Subject publicSubject = new Subject();
            publicSubject.setValue(Constants.SUBJECT_PUBLIC);
            accessRule.addSubject(publicSubject);
            accessPolicy.addAllow(accessRule);
            // set it
            boolean result = CNodeService.getInstance(d1NodeServiceTest.request)
                .setAccessPolicy(session, guid, accessPolicy, serialVersion);
            assertTrue(result);
            // check it
            result =
                CNodeService.getInstance(d1NodeServiceTest.request).isAuthorized(session, guid, Permission.WRITE);
            assertTrue(result);
        } catch (Exception e) {

            fail("Unexpected error: " + e.getMessage());

        }

        try {
            // trys to set access policy on an object whose authortiative memeber node is MNRead
            // v2. It should fail.
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testSetAccessPolicy." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V2MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);

            long serialVersion = 1L;

            // save it
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
            AccessPolicy accessPolicy = new AccessPolicy();
            AccessRule accessRule = new AccessRule();
            accessRule.addPermission(Permission.WRITE);
            Subject publicSubject = new Subject();
            publicSubject.setValue(Constants.SUBJECT_PUBLIC);
            accessRule.addSubject(publicSubject);
            accessPolicy.addAllow(accessRule);
            // set it
            boolean result = CNodeService.getInstance(d1NodeServiceTest.request)
                .setAccessPolicy(session, guid, accessPolicy, serialVersion);
            assertTrue(result);
            // check it
            result =
                CNodeService.getInstance(d1NodeServiceTest.request).isAuthorized(session, guid, Permission.WRITE);
            assertTrue(result);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof NotAuthorized) {
                assertTrue(e.getMessage().contains(
                    "The Coordinating Node is not authorized to make systemMetadata changes"));
            } else {
                fail("Unexpected error: " + e.getMessage());
            }
        }
    }

    @Test
    public void testIsAuthorized() {
        MCTestCase.printTestHeader("testIsAuthorized");

        try {
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testIsAuthorized." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            // save it
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
            // check it
            Subject publicSubject = new Subject();
            publicSubject.setValue(Constants.SUBJECT_PUBLIC);
            session.setSubject(publicSubject);
            // public read
            boolean result =
                CNodeService.getInstance(d1NodeServiceTest.request).isAuthorized(session, guid, Permission.READ);
            assertTrue(result);
            // not public write
            try {
                result = false;
                result =
                    CNodeService.getInstance(d1NodeServiceTest.request).isAuthorized(session, guid, Permission.WRITE);
                fail("Public WRITE should be denied");
            } catch (NotAuthorized nae) {
                result = true;
            }
            assertTrue(result);
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void testReplicationPolicy() {
        MCTestCase.printTestHeader("testReplicationPolicy");

        try {
            //v2 mn should fail
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testReplicationPolicy." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V1MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);
            long serialVersion = 1L;

            // save it
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());

            ReplicationPolicy policy = new ReplicationPolicy();
            NodeReference node = new NodeReference();
            node.setValue("testNode");
            policy.addPreferredMemberNode(node);
            // set it
            boolean result = CNodeService.getInstance(d1NodeServiceTest.request)
                .setReplicationPolicy(session, guid, policy, serialVersion);
            assertTrue(result);
            // get it
            sysmeta = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            assertNotNull(sysmeta);
            // check it
            assertEquals(
                policy.getPreferredMemberNode(0).getValue(),
                sysmeta.getReplicationPolicy().getPreferredMemberNode(0).getValue());

        } catch (Exception e) {

            fail("Unexpected error: " + e.getMessage());

        }
        try {
            //v2 mn should fail
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testReplicationPolicy." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V2MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);
            long serialVersion = 1L;

            // save it
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());

            ReplicationPolicy policy = new ReplicationPolicy();
            NodeReference node = new NodeReference();
            node.setValue("testNode");
            policy.addPreferredMemberNode(node);
            // set it
            boolean result = CNodeService.getInstance(d1NodeServiceTest.request)
                .setReplicationPolicy(session, guid, policy, serialVersion);
            assertTrue(result);
            // get it
            sysmeta = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            assertNotNull(sysmeta);
            // check it
            assertEquals(
                policy.getPreferredMemberNode(0).getValue(),
                sysmeta.getReplicationPolicy().getPreferredMemberNode(0).getValue());

        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof NotAuthorized) {
                assertTrue(e.getMessage().contains(
                    "The Coordinating Node is not authorized to make systemMetadata changes"));
            } else {
                fail("Unexpected error: " + e.getMessage());
            }

        }
    }

    @Test
    public void testReplicationStatus() {
        MCTestCase.printTestHeader("testReplicationStatus");

        try {
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testReplicationStatus." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            Date originalModificationDate = sysmeta.getDateSysMetadataModified();
            Replica replica = new Replica();
            NodeReference replicaMemberNode = new NodeReference();
            replicaMemberNode.setValue(MockCNode.getTestMN().getIdentifier().getValue());
            replica.setReplicationStatus(ReplicationStatus.REQUESTED);
            replica.setReplicaMemberNode(replicaMemberNode);
            replica.setReplicaVerified(Calendar.getInstance().getTime());
            sysmeta.addReplica(replica);
            // save it
            Identifier retGuid =
                CNodeService.getInstance(d1NodeServiceTest.request).registerSystemMetadata(session, guid, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
            // set it
            ReplicationStatus status = ReplicationStatus.QUEUED;
            BaseException failure =
                new NotAuthorized("000", "Mock exception for " + this.getClass().getName());
            //Test the failure of setReplicationStatus by a non-cn subject
            Session testSession = d1NodeServiceTest.getTestSession();
            try {
                CNodeService.getInstance(d1NodeServiceTest.request)
                    .setReplicationStatus(testSession, guid, replicaMemberNode, status, failure);
                fail(
                    "It can't reach here since the non-cn subject can't call setReplicationStatus");
            } catch (NotAuthorized ee) {

            }
            //Test the success of setReplicationStatus by a cn subject
            boolean result = CNodeService.getInstance(d1NodeServiceTest.request)
                .setReplicationStatus(session, guid, replicaMemberNode, status, failure);
            assertTrue(result);
            // get it
            sysmeta = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            BigInteger version = sysmeta.getSerialVersion();
            System.out.println("the version of system metadata is " + version.intValue());
            assertNotNull(sysmeta);
            // check it
            assertEquals(status, sysmeta.getReplica(0).getReplicationStatus());

            //set it failed.
            status = ReplicationStatus.FAILED;
            result = CNodeService.getInstance(d1NodeServiceTest.request)
                .setReplicationStatus(session, guid, replicaMemberNode, status, failure);
            assertTrue(result);
            // get it
            sysmeta = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            BigInteger version1 = sysmeta.getSerialVersion();
            System.out.println("the version of system metadata is " + version1.intValue());
            assertTrue(version1.compareTo(version) == 1);
            assertNotNull(sysmeta);
            // check it
            assertEquals(status, sysmeta.getReplica(0).getReplicationStatus());

            //set it failed again
            status = ReplicationStatus.FAILED;
            result = CNodeService.getInstance(d1NodeServiceTest.request)
                .setReplicationStatus(session, guid, replicaMemberNode, status, failure);
            assertTrue(result);
            // get it
            sysmeta = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            BigInteger version2 = sysmeta.getSerialVersion();
            System.out.println("the version of system metadata is " + version2.intValue());
            assertTrue(version2.compareTo(version1) == 0);
            assertNotNull(sysmeta);
            // check it
            assertEquals(status, sysmeta.getReplica(0).getReplicationStatus());

            //requeque it
            status = ReplicationStatus.QUEUED;
            result = CNodeService.getInstance(d1NodeServiceTest.request)
                .setReplicationStatus(session, guid, replicaMemberNode, status, failure);
            assertTrue(result);
            // get it
            sysmeta = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            BigInteger version3 = sysmeta.getSerialVersion();
            System.out.println("the version of system metadata is " + version3.intValue());
            assertTrue(version3.compareTo(version2) == 1);
            assertNotNull(sysmeta);
            // check it
            assertEquals(status, sysmeta.getReplica(0).getReplicationStatus());


            //Test the success of setReplicationStatus by a register mn subject
            //Session mnSession = getMNSessionFromCN();
            Subject mnSubject = MockCNode.getTestMN().getSubject(0);
            Session mnSession = new Session();
            mnSession.setSubject(mnSubject);
            status = ReplicationStatus.COMPLETED;
            result = CNodeService.getInstance(d1NodeServiceTest.request)
                .setReplicationStatus(mnSession, guid, replicaMemberNode, status, failure);
            sysmeta = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            BigInteger version4 = sysmeta.getSerialVersion();
            System.out.println("the version of system metadata is " + version4.intValue());
            assertTrue(version4.compareTo(version3) == 1);
            assertNotNull(sysmeta);
            // check it
            assertEquals(status, sysmeta.getReplica(0).getReplicationStatus());
            assertTrue(originalModificationDate.getTime() == sysmeta.getDateSysMetadataModified()
                .getTime());

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Run an initial test that always passes to check that the test harness is
     * working.
     */
    public void initialize() {
        MCTestCase.printTestHeader("initialize");
        assertTrue(1 == 1);
    }

    /**
     * We want to act as the CN itself
     * @throws ServiceFailure
     * @throws Exception
     */
    /*@Override
    public Session d1NodeServiceTest.getTestSession() throws Exception {
        Session session = super.d1NodeServiceTest.getTestSession();

        // use the first CN we find in the nodelist
        NodeList nodeList = D1Client.getCN().listNodes();
        for (Node node : nodeList.getNodeList()) {
            if ( node.getType().equals(NodeType.CN) ) {

                List<Subject> subjects = node.getSubjectList();
                for (Subject subject : subjects) {
                   session.setSubject(subject);
                   // we are done here
                   return session;
                }
            }
        }
        // in case we didn't find it
        return session;
    }*/


    /**
     * test to list the object formats registered in metacat
     */
    @Test
    public void testListFormats() throws Exception {

        MCTestCase.printTestHeader("testListFormats");

        // make sure we are set up
        d1NodeServiceTest.setUpFormats();

        // there should be at least 59 formats in the list
        int formatsCount = 59;
        ObjectFormatList objectFormatList;

        try {
            objectFormatList = CNodeService.getInstance(d1NodeServiceTest.request).listFormats();
            assertTrue(objectFormatList.getTotal() >= formatsCount);

        } catch (ServiceFailure e) {
            fail("Could not get the object format list: " + e.getMessage());

        } catch (NotImplemented e) {
            fail("Could not get the object format list: " + e.getMessage());

        }

    }

    /**
     * Test getting a single object format from the registered list
     */
    @Test
    public void testGetFormat() throws Exception {

        MCTestCase.printTestHeader("testGetFormat");

        // make sure we are set up
        d1NodeServiceTest.setUpFormats();

        String knownFormat = "text/plain";
        ObjectFormatIdentifier fmtid = new ObjectFormatIdentifier();
        fmtid.setValue(knownFormat);

        try {

            String result =
                CNodeService.getInstance(d1NodeServiceTest.request).getFormat(fmtid).getFormatId().getValue();
            System.out.println("Expected result: " + knownFormat);
            System.out.println("Found    result: " + result);
            assertTrue(result.equals(knownFormat));

        } catch (NullPointerException npe) {
            fail("The returned format was null: " + npe.getMessage());

        } catch (NotFound nfe) {
            fail("The format " + knownFormat + " was not found: " + nfe.getMessage());

        } catch (ServiceFailure sfe) {
            fail("The format " + knownFormat + " was not found: " + sfe.getMessage());

        } catch (NotImplemented nie) {
            fail("The getFormat() method has not been implemented: " + nie.getMessage());

        }
    }

    /**
     * Test getting a non-existent object format, returning NotFound
     */
    @Test
    public void testObjectFormatNotFoundException() {

        MCTestCase.printTestHeader("testObjectFormatNotFoundException");

        ObjectFormatIdentifier fmtid = new ObjectFormatIdentifier();
        String badFormat = "text/bad-format";
        fmtid.setValue(badFormat);

        try {
            ObjectFormat objectFormat = CNodeService.getInstance(d1NodeServiceTest.request).getFormat(fmtid);

        } catch (Exception e) {
            assertTrue(e instanceof NotFound);
        }

    }

    @Test
    public void readDeletedObject() {

        MCTestCase.printTestHeader("testCreate");

        try {
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            Identifier pid = d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            assertEquals(guid, pid);

            // use MN admin to delete
            session = d1NodeServiceTest.getCNSession();
            Identifier deletedPid = CNodeService.getInstance(d1NodeServiceTest.request).delete(session, pid);
            System.out.println("after deleting");
            assertEquals(pid.getValue(), deletedPid.getValue());
            // check that we cannot get the object
            session = d1NodeServiceTest.getTestSession();
            InputStream deletedObject = null;
            try {
                deletedObject = CNodeService.getInstance(d1NodeServiceTest.request).get(session, deletedPid);
            } catch (NotFound nf) {
                assertTrue(nf.getMessage().contains("deleted"));
            }
            try {
                SystemMetadata sysmeta2 =
                    CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, deletedPid);
            } catch (NotFound nf) {
                assertTrue(nf.getMessage().contains("deleted"));
            }

            try {
                DescribeResponse describeResponse =
                    CNodeService.getInstance(d1NodeServiceTest.request).describe(session, pid);
            } catch (NotFound nf) {
                assertTrue(nf.getMessage().contains("deleted"));
            }

            try {
                Checksum checksum = CNodeService.getInstance(d1NodeServiceTest.request).getChecksum(session, pid);
            } catch (NotFound nf) {
                assertTrue(nf.getMessage().contains("deleted"));
            }

            try {
                boolean isAuthorized =
                    CNodeService.getInstance(d1NodeServiceTest.request).isAuthorized(session, pid, Permission.READ);
            } catch (NotFound nf) {
                assertTrue(nf.getMessage().contains("deleted"));
            }
            assertNull(deletedObject);

            // Test to delete object which only has system metadata. CN only harvests the system for
            // data objects for saving space
            guid = new Identifier();
            guid.setValue("testDelete2." + System.currentTimeMillis());
            object = new ByteArrayInputStream("test1238973".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta2 =
                D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            // Store the system metadata only
            SystemMetadataManager.lock(guid);
            SystemMetadataManager.getInstance().store(sysmeta2);
            SystemMetadataManager.unLock(guid);
            SystemMetadata read = SystemMetadataManager.getInstance().get(guid);
            assertEquals(guid, read.getIdentifier());
            SystemMetadata sysmetaFromHash2;
            try (InputStream metaInput2 = D1NodeServiceTest.getStorage().retrieveMetadata(guid)) {
                sysmetaFromHash2 = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                                                                          metaInput2);
            }
            assertEquals(guid, sysmetaFromHash2.getIdentifier());
            try {
                MetacatHandler.read(guid);
                fail("Test can't get here since the object was not saved.");
            } catch (Exception ee) {
                assertTrue(ee instanceof McdbDocNotFoundException);
            }
            CNodeService.getInstance(d1NodeServiceTest.request)
                .delete(d1NodeServiceTest.getCNSession(), guid);
            SystemMetadata read2 = SystemMetadataManager.getInstance().get(guid);
            assertNull("The system metadata should be deleted.", read2);
            try {
                D1NodeServiceTest.getStorage().retrieveMetadata(guid);
                fail("Test shouldn't get there since the system metadata was deleted.");
            } catch (Exception ee) {
                assertTrue(ee instanceof FileNotFoundException);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test the method - get api  for a speicified SID
     */
    @Test
    public void testGetSID() {
        String str1 = "object1";
        String str2 = "object2";
        String str3 = "object3";
        try {
            //insert test documents with a series id
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue(d1NodeServiceTest.generateDocumentId());
            InputStream object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
            String sid1 = "sid." + System.nanoTime();
            Identifier seriesId = new Identifier();
            seriesId.setValue(sid1);
            System.out.println("the first sid is " + seriesId.getValue());
            sysmeta.setSeriesId(seriesId);
            object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
            d1NodeServiceTest.cnCreate(session, guid, object1, sysmeta);
            System.out.println("the first pid is " + guid.getValue());
            //test the get(pid) for v2
            InputStream result = CNodeService.getInstance(d1NodeServiceTest.request).get(session, guid);
            // go back to beginning of original stream
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result.available() > 0);
            assertTrue(IOUtils.contentEquals(result, object1));
            // test the get(id) for v2
            InputStream result1 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, seriesId);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result1.available() > 0);
            assertTrue(IOUtils.contentEquals(result1, object1));
            //test the get(pid) for v1
            InputStream result2 =
                edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .get(session, guid);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result2.available() > 0);
            assertTrue(IOUtils.contentEquals(result2, object1));
            //test the get(sid) for v1
            try {
                InputStream result3 =
                    edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                        .get(session, seriesId);
                fail("the get(sid) methoud should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }
            SystemMetadata metadata =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, seriesId);
            assertTrue(metadata.getIdentifier().getValue().equals(guid.getValue()));
            assertTrue(metadata.getSeriesId().getValue().equals(seriesId.getValue()));
            DescribeResponse describeResponse =
                CNodeService.getInstance(d1NodeServiceTest.request).describe(session, seriesId);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata.getFormatId().getValue());

            metadata = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            assertTrue(metadata.getIdentifier().getValue().equals(guid.getValue()));
            assertTrue(metadata.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = CNodeService.getInstance(d1NodeServiceTest.request).describe(session, guid);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata.getFormatId().getValue());

            org.dataone.service.types.v1.SystemMetadata sys1 =
                edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .getSystemMetadata(session, guid);
            assertTrue(metadata.getIdentifier().getValue().equals(guid.getValue()));

            try {
                org.dataone.service.types.v1.SystemMetadata sys2 =
                    edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                        .getSystemMetadata(session, seriesId);
                fail(
                    "the getSystemMetadata(sid) methoud should throw a not found exception for "
                        + "the sid "
                        + seriesId.getValue());
            } catch (NotFound nf2) {

            }

            describeResponse = edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                .describe(session, guid);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(), sys1.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                sys1.getFormatId().getValue());
            try {
                describeResponse =
                    edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                        .describe(session, seriesId);
                fail("the describe(sid) methoud should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound nf2) {

            }

            Checksum sum = CNodeService.getInstance(d1NodeServiceTest.request).getChecksum(session, guid);
            assertTrue(sum.getValue().equals("5b78f9689b9aab1ebc0f3c1df916dd97"));

            try {
                sum = CNodeService.getInstance(d1NodeServiceTest.request).getChecksum(session, seriesId);
                fail("the getCheckSum shouldn't work for sid");
            } catch (NotFound nf3) {

            }

            sum = edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                .getChecksum(session, guid);
            assertTrue(sum.getValue().equals("5b78f9689b9aab1ebc0f3c1df916dd97"));

            try {
                sum = edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .getChecksum(session, seriesId);
                fail("the getCheckSum shouldn't work for sid");
            } catch (NotFound nf3) {

            }

            boolean isAuthorized =
                CNodeService.getInstance(d1NodeServiceTest.request).isAuthorized(session, guid, Permission.READ);
            assertEquals(isAuthorized, true);

            isAuthorized =
                CNodeService.getInstance(d1NodeServiceTest.request).isAuthorized(session, seriesId, Permission.READ);
            assertEquals(isAuthorized, true);

            isAuthorized = edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                .isAuthorized(session, guid, Permission.READ);
            assertEquals(isAuthorized, true);

            try {
                isAuthorized = edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .isAuthorized(session, seriesId, Permission.READ);
                fail(
                    "we can't reach here since the v1 isAuthorized method doesn't suppport series"
                        + " id");
            } catch (NotFound e) {

            }

            //do a update with the same series id
            Thread.sleep(1000);
            Identifier newPid = new Identifier();
            newPid.setValue(d1NodeServiceTest.generateDocumentId() + "1");
            System.out.println("the second pid is " + newPid.getValue());
            InputStream object2 = new ByteArrayInputStream(str2.getBytes(StandardCharsets.UTF_8));
            SystemMetadata newSysMeta = D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object2);
            newSysMeta.setObsoletes(guid);
            newSysMeta.setSeriesId(seriesId);
            object2 = new ByteArrayInputStream(str2.getBytes(StandardCharsets.UTF_8));
            d1NodeServiceTest.cnCreate(session, newPid, object2, newSysMeta);
            //update the system metadata of previous version.
            sysmeta.setObsoletedBy(newPid);
            SystemMetadata read =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            BigInteger version = read.getSerialVersion();
            version = version.add(BigInteger.ONE);
            sysmeta.setSerialVersion(version);
            CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(session, guid, sysmeta);
            InputStream result4 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, guid);
            // go back to beginning of original stream
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result4.available() > 0);
            assertTrue(IOUtils.contentEquals(result4, object1));

            InputStream result5 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, newPid);
            // go back to beginning of original stream
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result5.available() > 0);
            assertTrue(IOUtils.contentEquals(result5, object2));


            InputStream result6 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, seriesId);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result6.available() > 0);
            assertTrue(IOUtils.contentEquals(result6, object2));
            //test the get(pid) for v1
            InputStream result7 =
                edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .get(session, guid);
            //System.out.println("+++++++++++++++++++++"+IOUtils.toString(result7));
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result7.available() > 0);
            assertTrue(IOUtils.contentEquals(result7, object1));

            InputStream result8 =
                edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .get(session, newPid);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result8.available() > 0);
            assertTrue(IOUtils.contentEquals(result8, object2));
            //test the get(sid) for v1
            try {
                InputStream result3 =
                    edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                        .get(session, seriesId);
                fail("the get(sid) methoud should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }

            SystemMetadata metadata1 =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, seriesId);
            assertTrue(metadata1.getIdentifier().getValue().equals(newPid.getValue()));
            assertTrue(metadata1.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = CNodeService.getInstance(d1NodeServiceTest.request).describe(session, seriesId);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata1.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata1.getFormatId().getValue());

            SystemMetadata metadata2 =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            assertTrue(metadata2.getIdentifier().getValue().equals(guid.getValue()));
            assertTrue(metadata2.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = CNodeService.getInstance(d1NodeServiceTest.request).describe(session, guid);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata2.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata2.getFormatId().getValue());

            SystemMetadata metadata3 =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, newPid);
            assertTrue(metadata3.getIdentifier().getValue().equals(newPid.getValue()));
            assertTrue(metadata3.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = CNodeService.getInstance(d1NodeServiceTest.request).describe(session, newPid);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata3.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata3.getFormatId().getValue());

            //do another update with different series id
            Thread.sleep(1000);
            String sid2 = "sid." + System.nanoTime();
            Identifier seriesId2 = new Identifier();
            seriesId2.setValue(sid2);
            System.out.println("the second sid is " + seriesId2.getValue());
            Identifier newPid2 = new Identifier();
            newPid2.setValue(d1NodeServiceTest.generateDocumentId() + "2");
            System.out.println("the third pid is " + newPid2.getValue());
            InputStream object3 = new ByteArrayInputStream(str3.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta3 = D1NodeServiceTest.createSystemMetadata(newPid2, session.getSubject(), object3);
            sysmeta3.setObsoletes(newPid);
            sysmeta3.setSeriesId(seriesId2);
            object3 = new ByteArrayInputStream(str3.getBytes(StandardCharsets.UTF_8));
            d1NodeServiceTest.cnCreate(session, newPid2, object3, sysmeta3);
            //update the system metadata of the previous version
            newSysMeta.setObsoletedBy(newPid2);
            SystemMetadata read2 =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, newPid);
            BigInteger version2 = read2.getSerialVersion();
            version2 = version2.add(BigInteger.ONE);
            newSysMeta.setSerialVersion(version2);
            CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(session, newPid, newSysMeta);

            InputStream result9 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, guid);
            // go back to beginning of original stream
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result9.available() > 0);
            assertTrue(IOUtils.contentEquals(result9, object1));

            InputStream result10 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, newPid);
            // go back to beginning of original stream
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result10.available() > 0);
            assertTrue(IOUtils.contentEquals(result10, object2));


            InputStream result11 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, newPid2);
            // go back to beginning of original stream
            object3.reset();
            // check
            assertTrue(object3.available() > 0);
            assertTrue(result11.available() > 0);
            assertTrue(IOUtils.contentEquals(result11, object3));

            InputStream result12 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, seriesId2);
            object3.reset();
            // check
            assertTrue(object3.available() > 0);
            assertTrue(result12.available() > 0);
            assertTrue(IOUtils.contentEquals(result12, object3));

            InputStream result16 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, seriesId);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result16.available() > 0);
            assertTrue(IOUtils.contentEquals(result16, object2));

            //test the get(pid) for v1
            InputStream result13 =
                edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .get(session, guid);
            object1.reset();
            // check
            assertTrue(object1.available() > 0);
            assertTrue(result13.available() > 0);
            assertTrue(IOUtils.contentEquals(result13, object1));

            InputStream result14 =
                edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .get(session, newPid);
            object2.reset();
            // check
            assertTrue(object2.available() > 0);
            assertTrue(result14.available() > 0);
            assertTrue(IOUtils.contentEquals(result14, object2));

            InputStream result15 =
                edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .get(session, newPid2);
            object3.reset();
            // check
            assertTrue(object3.available() > 0);
            assertTrue(result15.available() > 0);
            assertTrue(IOUtils.contentEquals(result15, object3));

            SystemMetadata metadata4 =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, seriesId);
            assertTrue(metadata4.getIdentifier().getValue().equals(newPid.getValue()));
            assertTrue(metadata4.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = CNodeService.getInstance(d1NodeServiceTest.request).describe(session, seriesId);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata4.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata4.getFormatId().getValue());

            SystemMetadata metadata5 =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, seriesId2);
            assertTrue(metadata5.getIdentifier().getValue().equals(newPid2.getValue()));
            assertTrue(metadata5.getSeriesId().getValue().equals(seriesId2.getValue()));
            describeResponse = CNodeService.getInstance(d1NodeServiceTest.request).describe(session, seriesId2);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata5.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata5.getFormatId().getValue());

            SystemMetadata metadata6 =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
            assertTrue(metadata6.getIdentifier().getValue().equals(guid.getValue()));
            assertTrue(metadata6.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = CNodeService.getInstance(d1NodeServiceTest.request).describe(session, guid);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata6.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata6.getFormatId().getValue());

            SystemMetadata metadata7 =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, newPid);
            assertTrue(metadata7.getIdentifier().getValue().equals(newPid.getValue()));
            assertTrue(metadata7.getSeriesId().getValue().equals(seriesId.getValue()));
            describeResponse = CNodeService.getInstance(d1NodeServiceTest.request).describe(session, newPid);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata7.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata7.getFormatId().getValue());

            SystemMetadata metadata8 =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, newPid2);
            assertTrue(metadata8.getIdentifier().getValue().equals(newPid2.getValue()));
            assertTrue(metadata8.getSeriesId().getValue().equals(seriesId2.getValue()));
            describeResponse = CNodeService.getInstance(d1NodeServiceTest.request).describe(session, newPid2);
            assertEquals(
                describeResponse.getDataONE_Checksum().getValue(),
                metadata8.getChecksum().getValue());
            assertEquals(
                describeResponse.getDataONE_ObjectFormatIdentifier().getValue(),
                metadata8.getFormatId().getValue());


            System.out.println("here===========================");
            //test the get(sid) for v1
            try {
                InputStream result3 =
                    edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                        .get(session, seriesId);
                fail("the get(sid) methoud should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }

            //test the get(sid) for v1
            try {
                InputStream result3 =
                    edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                        .get(session, seriesId2);
                fail("the get(sid) methoud should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }

            //test to get non-existing id for v2
            try {
                // the pid should be null when we try to get a no-exist sid
                Identifier non_exist_sid = new Identifier();
                non_exist_sid.setValue("no-sid-exist-123qwe");
                InputStream result3 = CNodeService.getInstance(d1NodeServiceTest.request).get(session, non_exist_sid);
                fail("the get(sid) methoud should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }

            try {
                // the pid should be null when we try to get a no-exist sid
                Identifier non_exist_sid = new Identifier();
                non_exist_sid.setValue("no-sid-exist-123qwe");
                SystemMetadata result3 =
                    CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, non_exist_sid);
                fail(
                    "the getSystemMetadata(sid) methoud should throw a not found exception for "
                        + "the sid "
                        + seriesId.getValue());
            } catch (NotFound ee) {

            }

            try {
                // the pid should be null when we try to get a no-exist sid
                Identifier non_exist_sid = new Identifier();
                non_exist_sid.setValue("no-sid-exist-123qwe");
                CNodeService.getInstance(d1NodeServiceTest.request).describe(session, non_exist_sid);
                fail("the describe(sid) methoud should throw a not found exception for the sid "
                         + seriesId.getValue());
            } catch (NotFound ee) {

            }


            //do another update with invalid series ids
            Thread.sleep(1000);
            Identifier newPid3 = new Identifier();
            newPid3.setValue(d1NodeServiceTest.generateDocumentId() + "3");
            System.out.println("the third pid is " + newPid3.getValue());
            InputStream object4 = new ByteArrayInputStream(str3.getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta4 = D1NodeServiceTest.createSystemMetadata(newPid3, session.getSubject(), object4);
            sysmeta4.setObsoletes(newPid2);
            sysmeta4.setSeriesId(seriesId);

            //test archive a series id by v1
            try {
                edu.ucsb.nceas.metacat.dataone.v1.MNodeService.getInstance(d1NodeServiceTest.request)
                    .archive(session, seriesId2);
                fail("we can't reach here since the v1 archive method doesn't support the sid ");
            } catch (NotFound nf2) {

            }

            // test delete a series id by v1
            Session mnSession = d1NodeServiceTest.getMNSession();
            try {
                edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .delete(mnSession, seriesId2);
                fail("we can't reach here since the v1 delete method doesn't support the sid ");
            } catch (NotFound nf2) {

            }

            // test archive a series id by v2
            MNodeService.getInstance(d1NodeServiceTest.request).archive(session, seriesId2);
            SystemMetadata archived =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, seriesId2);
            assertTrue(archived.getArchived());
            archived = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, newPid2);
            assertTrue(archived.getArchived());

            // test delete a series id by v2
            CNodeService.getInstance(d1NodeServiceTest.request).delete(session, seriesId2);
            try {
                CNodeService.getInstance(d1NodeServiceTest.request).get(session, seriesId2);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                System.out.println("the message is =============" + nf3.getMessage());
                //assertTrue(nf3.getMessage().indexOf("delete") >0);
            }

            try {
                CNodeService.getInstance(d1NodeServiceTest.request).get(session, newPid2);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                //System.out.println("the message is ============="+nf3.getMessage());
                assertTrue(nf3.getMessage().indexOf("delete") > 0);
            }

            try {
                edu.ucsb.nceas.metacat.dataone.v1.CNodeService.getInstance(d1NodeServiceTest.request)
                    .get(session, newPid2);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                System.out.println("the message is =============" + nf3.getMessage());
                assertTrue(nf3.getMessage().indexOf("delete") > 0);
            }

            //archive seriesId
            MNodeService.getInstance(d1NodeServiceTest.request).archive(mnSession, seriesId);
            archived = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, seriesId);
            assertTrue(archived.getArchived());
            archived = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, newPid);
            assertTrue(archived.getArchived());


            //delete seriesId
            CNodeService.getInstance(d1NodeServiceTest.request).delete(session, seriesId);
            try {
                CNodeService.getInstance(d1NodeServiceTest.request).get(session, newPid);
                fail("we can't reach here since the series id was deleted ");
            } catch (NotFound nf3) {
                //System.out.println("the message is ============="+nf3.getMessage());
                assertTrue(nf3.getMessage().indexOf("delete") > 0);
            }
            SystemMetadata meta =
                CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, seriesId);
            assertTrue(meta.getIdentifier().getValue().equals(guid.getValue()));

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test the listView methods.
     * @throws Excpetion
     */
    @Test
    public void testListViews() throws Exception {
        OptionList list = CNodeService.getInstance(d1NodeServiceTest.request).listViews();
        assertTrue(list.sizeOptionList() > 0);
        List<String> names = list.getOptionList();
        for (String name : names) {
            System.out.println("It has the view named " + name);
        }
    }

    @Test
    public void testUpdateSystemMetadata() throws Exception {
        String str1 = "object1";
        String str2 = "object2";
        String str3 = "object3";

        Date date = new Date();
        //insert test documents with a series id
        Session session = d1NodeServiceTest.getCNSession();
        Identifier guid = new Identifier();
        guid.setValue(d1NodeServiceTest.generateDocumentId());
        System.out.println("?????????????update the id without archive is " + guid.getValue());
        InputStream object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object1);
        String sid1 = "sid." + System.nanoTime();
        Identifier seriesId = new Identifier();
        seriesId.setValue(sid1);
        System.out.println("the first sid is " + seriesId.getValue());
        sysmeta.setSeriesId(seriesId);
        sysmeta.setArchived(false);
        object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.cnCreate(session, guid, object1, sysmeta);
        //Test the generating object succeeded.
        SystemMetadata metadata =
            CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
        assertTrue(metadata.getIdentifier().equals(guid));
        assertTrue(metadata.getArchived().equals(false));
        System.out.println("the checksum from request is " + metadata.getChecksum().getValue());
        assertTrue(metadata.getSize().equals(sysmeta.getSize()));

        //test to fail to update system metadata by a non-cn subject
        Session testSession = d1NodeServiceTest.getTestSession();
        SystemMetadata sysmeta1 =
            CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, guid);
        BigInteger version = sysmeta.getSerialVersion();
        version = version.add(BigInteger.ONE);
        sysmeta1.setSerialVersion(version);
        AccessPolicy accessPolicy = new AccessPolicy();
        AccessRule accessRule = new AccessRule();
        accessRule.addPermission(Permission.WRITE);
        Subject publicSubject = new Subject();
        publicSubject.setValue("hello");
        accessRule.addSubject(publicSubject);
        accessPolicy.addAllow(accessRule);
        AccessRule allow = new AccessRule();
        allow.addPermission(Permission.READ);
        Subject publicSubject2 = new Subject();
        publicSubject2.setValue(Constants.SUBJECT_PUBLIC);
        allow.addSubject(publicSubject2);
        accessPolicy.addAllow(allow);
        sysmeta1.setAccessPolicy(accessPolicy);
        try {
            CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(testSession, guid, sysmeta1);
            fail("It shouldn't get there since the test session can't update system metadata");
        } catch (NotAuthorized e) {

        }


        //update system metadata sucessfully
        SystemMetadata sysmeta1c = new SystemMetadata();
        BeanUtils.copyProperties(sysmeta1c, sysmeta1);
        Date newDate1 = sysmeta1.getDateSysMetadataModified();
        //sysmeta1c.setDateSysMetadataModified(date);
        CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(session, guid, sysmeta1c);
        SystemMetadata metadata2 =
            CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, seriesId);
        assertTrue(metadata2.getIdentifier().equals(guid));
        assertTrue(metadata2.getSeriesId().equals(seriesId));
        //assertTrue(metadata2.getArchived().equals(true));
        assertTrue(metadata2.getAccessPolicy().getAllowList().size() == 2);
        assertTrue(metadata2.getChecksum().getValue().equals(metadata.getChecksum().getValue()));
        assertTrue(metadata2.getDateSysMetadataModified().getTime() == newDate1.getTime());

        SystemMetadata sysmeta2 =
            CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, seriesId);
        version = sysmeta2.getSerialVersion();
        version = version.add(BigInteger.ONE);
        sysmeta2.setSerialVersion(version);
        Identifier newId = new Identifier();
        newId.setValue("newValue");
        sysmeta2.setIdentifier(newId);
        try {
            CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(session, guid, sysmeta2);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }

        newId.setValue("newValue");
        sysmeta2.setSeriesId(newId);
        try {
            CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(session, guid, sysmeta2);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }

        Date newDate = new Date();
        sysmeta2.setDateUploaded(newDate);
        try {
            CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(session, guid, sysmeta2);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }

        Checksum checkSum = new Checksum();
        checkSum.setValue("12345");
        sysmeta2.setChecksum(checkSum);
        try {
            CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(session, guid, sysmeta2);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }

        BigInteger size = new BigInteger("4000");
        sysmeta2.setSize(size);
        try {
            CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(session, guid, sysmeta2);
            fail("We shouldn't get there");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }

        // test cn.updateSystemMetadata will ignore the serial version and replica list
        Identifier id = new Identifier();
        id.setValue(d1NodeServiceTest.generateDocumentId());
        System.out.println("?????????????update the id with archive is " + id.getValue());
        object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta10 = D1NodeServiceTest.createSystemMetadata(id, session.getSubject(), object1);
        List<Replica> replicas = new ArrayList<Replica>();
        Replica replica1 = new Replica();
        NodeReference node1 = new NodeReference();
        node1.setValue("node1");
        replica1.setReplicaMemberNode(node1);
        replica1.setReplicationStatus(ReplicationStatus.FAILED);
        replica1.setReplicaVerified(date);
        replicas.add(replica1);
        Replica replica2 = new Replica();
        NodeReference node2 = new NodeReference();
        node2.setValue("node2");
        replica2.setReplicaMemberNode(node2);
        replica2.setReplicationStatus(ReplicationStatus.FAILED);
        replica2.setReplicaVerified(date);
        replicas.add(replica2);
        sysmeta10.setReplicaList(replicas);
        sysmeta10.setArchived(false);
        object1 = new ByteArrayInputStream(str1.getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.cnCreate(session, id, object1, sysmeta10);
        SystemMetadata result = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, id);
        assertTrue(result.getIdentifier().equals(id));
        System.out.println("the serial version is " + result.getSerialVersion().intValue());
        assertTrue(result.getSerialVersion().intValue() == 1);
        List<Replica> list1 = result.getReplicaList();
        assertTrue(list1.size() == 2);
        assertTrue(result.getReplica(0).getReplicaMemberNode().getValue().equals("node1"));
        assertTrue(result.getReplica(0).getReplicationStatus().equals(ReplicationStatus.FAILED));
        assertTrue(result.getReplica(0).getReplicaVerified().getTime() == date.getTime());
        assertTrue(result.getReplica(1).getReplicaMemberNode().getValue().equals("node2"));
        assertTrue(result.getReplica(1).getReplicationStatus().equals(ReplicationStatus.FAILED));
        assertTrue(result.getReplica(1).getReplicaVerified().getTime() == date.getTime());
        assertTrue(result.getArchived() == false);

        Date date2 = new Date();
        SystemMetadata sysmeta11 = new SystemMetadata();
        BeanUtils.copyProperties(sysmeta11, result);
        List<Replica> replicaList = new ArrayList<Replica>();
        Replica replica3 = new Replica();
        NodeReference node3 = new NodeReference();
        node3.setValue("node3");
        replica3.setReplicaMemberNode(node3);
        replica3.setReplicationStatus(ReplicationStatus.COMPLETED);
        replica3.setReplicaVerified(date2);
        replicaList.add(replica3);
        sysmeta11.setReplicaList(replicaList);
        sysmeta11.setSerialVersion(BigInteger.TEN);
        sysmeta11.setArchived(true);

        //make sure the sysmmeta11 has the new replca list and serial version
        assertTrue(sysmeta11.getSerialVersion().equals(BigInteger.TEN));
        assertTrue(sysmeta11.getReplicaList().size() == 1);
        assertTrue(sysmeta11.getReplica(0).getReplicaMemberNode().getValue().equals("node3"));
        assertTrue(
            sysmeta11.getReplica(0).getReplicationStatus().equals(ReplicationStatus.COMPLETED));
        assertTrue(sysmeta11.getReplica(0).getReplicaVerified().getTime() == date2.getTime());

        // update the system metadata with the new serial version and new replica list
        // the new serial version and replica list should be ignored.
        CNodeService.getInstance(d1NodeServiceTest.request).updateSystemMetadata(session, id, sysmeta11);
        SystemMetadata result2 = CNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, id);
        assertTrue(result2.getIdentifier().equals(id));
        System.out.println("the +++++ version is " + result2.getSerialVersion().intValue());
        assertTrue(result2.getSerialVersion().intValue() == 1);
        List<Replica> list2 = result.getReplicaList();
        assertTrue(list2.size() == 2);
        assertTrue(result2.getReplica(0).getReplicaMemberNode().getValue().equals("node1"));
        assertTrue(result2.getReplica(0).getReplicationStatus().equals(ReplicationStatus.FAILED));
        assertTrue(result2.getReplica(0).getReplicaVerified().getTime() == date.getTime());
        assertTrue(result2.getReplica(1).getReplicaMemberNode().getValue().equals("node2"));
        assertTrue(result2.getReplica(1).getReplicationStatus().equals(ReplicationStatus.FAILED));
        assertTrue(result2.getReplica(1).getReplicaVerified().getTime() == date.getTime());
        assertTrue(result2.getArchived() == true);

        // Test not checking the dateSystemMetadataModified field
        // Set a new modification date
        result2.setDateSysMetadataModified(new Date());
        long originModifiedDate = result2.getDateSysMetadataModified().getTime();
        CNodeService.getInstance(d1NodeServiceTest.request)
                                                    .updateSystemMetadata(session, id, result2);
        SystemMetadata result3 = CNodeService.getInstance(d1NodeServiceTest.request)
                                                                .getSystemMetadata(session, id);
        assertEquals(originModifiedDate, result3.getDateSysMetadataModified().getTime());
    }

    @Test
    public void testArchive() throws Exception {
        try {
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testArchive." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V1MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            Identifier pid = d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            CNodeService.getInstance(d1NodeServiceTest.request).archive(session, guid);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }

        try {

            Session session = d1NodeServiceTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testArchive." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V1MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            Identifier pid = d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            //another session without any permission shouldn't archive the object
            try {
                Session session2 = d1NodeServiceTest.getAnotherSession();
                CNodeService.getInstance(d1NodeServiceTest.request).archive(session2, guid);
                fail("Another session shouldn't archive the object");
            } catch (NotAuthorized ee) {

            }
            //rights holder should archive objects whose authoritative mn is a v1 node
            CNodeService.getInstance(d1NodeServiceTest.request).archive(session, guid);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }

        try {
            //a session has the change permission should archive objects whose authoritative mn
            // is a v1 node
            String user1 = "test12";
            String user2 = "test34";
            Session session = d1NodeServiceTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testArchive." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V1MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);
            Subject sub1 = new Subject();
            sub1.setValue(user1);
            AccessRule rule1 = new AccessRule();
            rule1.addSubject(sub1);
            rule1.addPermission(Permission.CHANGE_PERMISSION);
            Subject sub2 = new Subject();
            sub2.setValue(user2);
            AccessRule rule2 = new AccessRule();
            rule2.addSubject(sub2);
            rule2.addPermission(Permission.READ);
            AccessPolicy policy = new AccessPolicy();
            policy.addAllow(rule1);
            policy.addAllow(rule2);
            sysmeta.setAccessPolicy(policy);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            Identifier pid = d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            //read permission can't archive the object
            try {
                Session session2 = new Session();
                session2.setSubject(sub2);
                CNodeService.getInstance(d1NodeServiceTest.request).archive(session2, guid);
                fail("READ permision session shouldn't archive the object");
            } catch (NotAuthorized ee) {
                ee.printStackTrace();
            }
            Session session3 = new Session();
            session3.setSubject(sub1);
            CNodeService.getInstance(d1NodeServiceTest.request).archive(session, guid);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }

        try {
            //v2 mn should faile
            Session session = d1NodeServiceTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testArchive." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            NodeReference nr = new NodeReference();
            nr.setValue(MockCNode.V2MNNODEID);
            sysmeta.setOriginMemberNode(nr);
            sysmeta.setAuthoritativeMemberNode(nr);
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            Identifier pid = d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            CNodeService.getInstance(d1NodeServiceTest.request).archive(session, guid);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof NotAuthorized) {
                assertTrue(e.getMessage().contains(
                    "The Coordinating Node is not authorized to make systemMetadata changes"));
            } else {
                fail("Unexpected error: " + e.getMessage());
            }
        }
    }


    @Test
    public void testInvalidIds() throws Exception {
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testCreate.\n" + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        try {
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            fail("MNodeService should reject identifier with a whitespace");
        } catch (InvalidRequest e) {

        }

        guid.setValue("testCreate. " + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        try {
            object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            fail("MNodeService should reject identifier with a whitespace");
        } catch (InvalidRequest e) {

        }

    }


    /**
     * Test create and update json-ld objects
     * @throws Exception
     */
    @Test
    public void testInsertJson_LD() throws Exception {
        MCTestCase.printTestHeader("testInsertJson_LD");

        ObjectFormatIdentifier formatid = new ObjectFormatIdentifier();
        formatid.setValue(NonXMLMetadataHandlers.JSON_LD);

        //create a json-ld object successfully
        File tempValidJsonLDFile = JsonLDHandlerTest.generateTmpFile("temp-json-ld-valid");
        InputStream input = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
        OutputStream out = new FileOutputStream(tempValidJsonLDFile);
        IOUtils.copy(input, out);
        out.close();
        input.close();

        ObjectFormat mockFormat = new ObjectFormat();
        mockFormat.setFormatType("METADATA");

        Session session = d1NodeServiceTest.getCNSession();
        Identifier guid = new Identifier();
        guid.setValue("testInsertJson_LD." + System.currentTimeMillis());
        InputStream object = new FileInputStream(tempValidJsonLDFile);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatid);
        object.close();
        FileInputStream data = new FileInputStream(tempValidJsonLDFile);

        Identifier pid;

        //mock the format cache, so we no longer rely on a call to the real CN server
        ObjectFormatCache mockObjectFormatCache = Mockito.mock(ObjectFormatCache.class);
        Mockito.when(mockObjectFormatCache.getFormat((ObjectFormatIdentifier) (Mockito.any())))
            .thenReturn(mockFormat);
        try (MockedStatic<ObjectFormatCache> ignored =
                 Mockito.mockStatic(ObjectFormatCache.class)) {
            Mockito.when(ObjectFormatCache.getInstance()).thenReturn(mockObjectFormatCache);

            pid = d1NodeServiceTest.cnCreate(session, guid, data, sysmeta);
        }
        SystemMetadata result =
            MNodeService.getInstance(d1NodeServiceTest.request).getSystemMetadata(session, pid);
        assertEquals(result.getIdentifier(), guid);
        data.close();
        tempValidJsonLDFile.delete();

        //failed to create an object since it is an invalid json-ld object
        File tempInvalidJsonLDFile = JsonLDHandlerTest.generateTmpFile("temp-json-ld-invalid");
        input = new FileInputStream(new File(JsonLDHandlerTest.INVALID_JSON_LD_FILE_PATH));
        out = new FileOutputStream(tempInvalidJsonLDFile);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        Identifier newPid = new Identifier();
        newPid.setValue("testInsertJson_LD_3." + (System.currentTimeMillis()));
        object = new FileInputStream(tempInvalidJsonLDFile);
        SystemMetadata newMeta =
            D1NodeServiceTest.createSystemMetadata(newPid, session.getSubject(), object);
        newMeta.setFormatId(formatid);
        object.close();
        data = new FileInputStream(tempInvalidJsonLDFile);

        try (MockedStatic<ObjectFormatCache> ignored =
                 Mockito.mockStatic(ObjectFormatCache.class)) {
            //mock the format cache, so we no longer rely on a call to the real CN server
            Mockito.when(ObjectFormatCache.getInstance()).thenReturn(mockObjectFormatCache);

            d1NodeServiceTest.cnCreate(session, newPid, data, newMeta);
            fail("we shouldn't get here since the object is an invalid json-ld file");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        data.close();
        tempInvalidJsonLDFile.delete();
    }

    /**
     * Test the scenario the identifier doesn't match the one in the system metadata
     * in the create method
     * @throws Exception
     */
    @Test
    public void testIdNotMatchSysmetaInCreate() throws Exception {
        Session session = d1NodeServiceTest.getCNSession();
        //a data file
        Identifier guid = new Identifier();
        guid.setValue("testIdNotMatchSysmetaInCreate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        // Set to another pid into system metadata to make it invalid
        Identifier another = new Identifier();
        another.setValue("testIdNotMatchSysmetaInCreate2." + System.currentTimeMillis());
        sysmeta.setIdentifier(another);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        try {
            d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            fail("Test shouldn't get there since the system metadata has a different user");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest rather than "
                        + e.getClass().getName(), e instanceof InvalidRequest);
        }
        // Set back to make it valid and the create method should succeed
        sysmeta.setIdentifier(guid);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        Identifier pid = d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
        assertTrue("The returned pid should be " + guid.getValue(),
                    guid.getValue().equals(pid.getValue()));

        // An eml metadata object
        guid = new Identifier();
        guid.setValue("testIdNotMatchSysmetaInCreate." + System.currentTimeMillis());
        object = new FileInputStream(MockReplicationMNode.replicationSourceFile);
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta.setFormatId(formatId);
        // Make the system metadata not match the guid
        sysmeta.setIdentifier(another);
        object = new FileInputStream(MockReplicationMNode.replicationSourceFile);
        try {
            d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
            fail("Test shouldn't get there since the system metadata has a different user");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest rather than "
                        + e.getClass().getName(), e instanceof InvalidRequest);
        }
        // Set back to make it valid and the create method should succeed
        sysmeta.setIdentifier(guid);
        object = new FileInputStream(MockReplicationMNode.replicationSourceFile);
        pid = d1NodeServiceTest.cnCreate(session, guid, object, sysmeta);
        assertTrue("The returned pid should be " + guid.getValue(),
                                guid.getValue().equals(pid.getValue()));
    }
}
