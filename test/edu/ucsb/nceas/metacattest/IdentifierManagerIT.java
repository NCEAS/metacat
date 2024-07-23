package edu.ucsb.nceas.metacattest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;

import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;

/**
 * Test the IdentifierManager class
 */
public class IdentifierManagerIT {
    private String badGuid = "test:testIdThatDoesNotExist";
    private D1NodeServiceTest d1NodeServiceTest;
    private HttpServletRequest request;

    /**
     * Initialize the connection to metacat, and insert a document to be 
     * used for testing with a known docid.
     */
    @Before
    public void setUp() {
        d1NodeServiceTest = new D1NodeServiceTest("initialize");
        request = d1NodeServiceTest.getServletRequest();
        try {
            DBConnectionPool pool = DBConnectionPool.getInstance();
        } catch (SQLException e2) {
            fail(e2.getMessage());
        }
    }

    /**
     * test getting a guid from the systemmetadata table
     */
    @Test
    public void testGetGUID() {
        ph("testGetGUID");
        try {
            IdentifierManager im = IdentifierManager.getInstance();
            String guid = insertTestDocument();
            String docidWithRev = im.getLocalId(guid);
            String docid = docidWithRev.substring(0, docidWithRev.lastIndexOf("."));
            String gotGuid = im.getGUID(docid, 1);
            assertTrue(guid.equals(gotGuid));
        } catch(Exception e) {
            fail("Unexpected exception in testGetGUID: " + e.getMessage());
        }
    }

    /**
     * test getting a list of all local ids
     */
    @Test
    public void testGetAllLocalIds() {
        ph("testGetAllLocalIds");
        try {
            List l = IdentifierManager.getInstance().getAllLocalIds();
            for(int i=0; i<l.size(); i++)
            {
                System.out.println(l.get(i));
            }
            assertTrue(l.size() > 0);
        } catch(Exception e) {
            fail("Error in testGetAllLocalIds: " + e.getMessage());
        }
    }

    /** Test that IM instances can be created. */
    @Test
    public void testGetInstance() {
        ph("testGetInstance");
        IdentifierManager im = IdentifierManager.getInstance();
        assertNotNull(im);
    }

    /** Test that known LocalId's can be looked up from GUIDs. */
    @Test
    public void testGetLocalId() {
        ph("testGetLocalId");
        try {
            IdentifierManager im = IdentifierManager.getInstance();
            String guid = insertTestDocument();
            String docidWithRev = im.getLocalId(guid);
            String docid = docidWithRev.substring(0, docidWithRev.lastIndexOf("."));
            String idReturned = im.getGUID(docid, 1);
            assertTrue(guid.equals(idReturned));
        } catch (McdbDocNotFoundException e) {
            fail(e.getMessage());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            String guid = "testGetLocalIdfoo" + System.currentTimeMillis();
            IdentifierManager.getInstance().getLocalId(guid);
            fail("Test shouldn't get here since the guid doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof McdbDocNotFoundException);
        }
    }
    
    /** Test that unknown LocalId's return the proper exception. */
    @Test
    public void testGetLocalIdNotFound() {
        ph("testGetLocalIdNotFound");
        IdentifierManager im = IdentifierManager.getInstance();
        String idReturned;
        try {
            idReturned = im.getLocalId(badGuid);
            fail("Failed: getLocalID() should have returned an document not " + 
                 "found exception but did not.");
        } catch (McdbDocNotFoundException e) {
            assertNotNull(e);
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    /** 
     * Test that an identifier is present in the system when it should
     *  be, and that it is not present when it shouldn't be. 
     */
    @Test
    public void testIdentifierExists() throws Exception{
        ph("testIdentifierExists");
        String goodGuid  = "";
        IdentifierManager im = IdentifierManager.getInstance();
        goodGuid = insertTestDocument();
        assertTrue(im.identifierExists(goodGuid));
        assertFalse(im.identifierExists(badGuid));
    }
    
    /**
     * Test that we are able to create mappings from guids to localIds, and that
     * improperly formatted docids generate the proper exceptions.  This also tests
     * getLocalId() and getGUID()
     */
    @Test
    public void testCreateMapping() {
       ph("testCreateMapping");
       try {
            IdentifierManager im = IdentifierManager.getInstance();
            String docid = "test." + new Date().getTime() + ".1";
            String guid = "guid:" + docid;
            im.createMapping(guid, docid);
            String guiddocid = docid.substring(0, docid.length() - 2);
            System.out.println("guiddocid: " + guiddocid);
            String guid2 = im.getGUID(guiddocid, 1);
            assertTrue(guid2.equals(guid));
            String docid2 = im.getLocalId(guid);
            assertTrue(docid.equals(docid2));
        } catch (McdbDocNotFoundException e) {
            e.printStackTrace();
            fail("createMapping failed to create proper localId from guid: " + e.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    /**
     * test the local id creation
     */
    @Test
    public void testGenerateLocalId() {
      IdentifierManager im = IdentifierManager.getInstance();
      String localid = im.generateLocalId("mynewid." + new Date().getTime(), 1);
      System.out.println("localid: " + localid);
      assertTrue(localid != null);
    }

    /**
     * Test the method systemMetadataPIDExist
     */
    @Test
    public void testSystemMetadataPIDExists() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(D1NodeServiceTest.generateDocumentId());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        boolean exist = IdentifierManager.getInstance().systemMetadataPIDExists(guid);
        assertTrue(exist);
        Thread.sleep(1000);
        guid.setValue(D1NodeServiceTest.generateDocumentId());
        exist = IdentifierManager.getInstance().systemMetadataPIDExists(guid);
        assertTrue(!exist);
    }

    /**
     * Test the method of systemMetadataSIDExist
     * @throws exception
     */
    @Test
    public void testSystemMetadataSIDExists() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(D1NodeServiceTest.generateDocumentId());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest
                                    .createSystemMetadata(guid, session.getSubject(), object);
        String sid1= "sid."+System.nanoTime();
        Identifier seriesId = new Identifier();
        seriesId.setValue(sid1);
        System.out.println("the first sid is "+seriesId.getValue());
        sysmeta.setSeriesId(seriesId);
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        boolean exist = IdentifierManager.getInstance().systemMetadataPIDExists(guid);
        assertTrue(exist);
        exist = IdentifierManager.getInstance().systemMetadataSIDExists(guid);
        assertTrue(!exist);
        exist = IdentifierManager.getInstance().systemMetadataSIDExists(seriesId);
        assertTrue(exist);
        exist = IdentifierManager.getInstance().systemMetadataPIDExists(seriesId);
        assertTrue(!exist);
        Thread.sleep(1000);
        sid1= "sid."+System.nanoTime();
        seriesId.setValue(sid1);
        exist = IdentifierManager.getInstance().systemMetadataSIDExists(seriesId);
        assertTrue(!exist);
    }
    /**
     * Test the method - getHeadPID for a speicified SID
     */
    @Test
    public void testGetHeadPID() {
        
        try {
            //case-1  P1(S1) <-> P2(S1), S1 = P2 (Type 1)
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue(D1NodeServiceTest.generateDocumentId());
            InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
            String sid1= "sid."+System.nanoTime();
            Identifier seriesId = new Identifier();
            seriesId.setValue(sid1);
            System.out.println("the first sid is "+seriesId.getValue());
            sysmeta.setSeriesId(seriesId);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            System.out.println("the first pid is "+guid.getValue());
            Identifier head = IdentifierManager.getInstance().getHeadPID(seriesId);
            System.out.println("the head 1 is "+head.getValue());
            assertTrue(head.getValue().equals(guid.getValue()));
            assertTrue(IdentifierManager.getInstance().systemMetadataSIDExists(seriesId.getValue()));
            assertTrue(!IdentifierManager.getInstance().systemMetadataPIDExists(seriesId.getValue()));
            assertTrue(IdentifierManager.getInstance().systemMetadataPIDExists(guid.getValue()));
            assertTrue(!IdentifierManager.getInstance().systemMetadataSIDExists(guid.getValue()));
            
            //do a update with the same series id
            Thread.sleep(1000);
            Identifier newPid = new Identifier();
            newPid.setValue(D1NodeServiceTest.generateDocumentId()+"1");
            System.out.println("the second pid is "+newPid.getValue());
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            SystemMetadata newSysMeta = D1NodeServiceTest
                                    .createSystemMetadata(newPid, session.getSubject(), object);
            newSysMeta.setObsoletes(guid);
            newSysMeta.setSeriesId(seriesId);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.mnUpdate(session, guid, object, newPid, newSysMeta);
            
            //check
            SystemMetadata meta = CNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(meta.getObsoletedBy().equals(newPid));
            assertTrue(meta.getObsoletes() == null);
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, newPid);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(guid));
            
            System.out.println("case 1: =======");
            // the pid should be the newPid when we try to get the sid1
            head = IdentifierManager.getInstance().getHeadPID(seriesId);
            assertTrue(head.getValue().equals(newPid.getValue()));
            
            
            // the pid should be null when we try to get a no-exist sid
            Identifier non_exist_sid = new Identifier();
            non_exist_sid.setValue("no-sid-exist-123qwe");
            assertTrue(IdentifierManager.getInstance().getHeadPID(non_exist_sid) == null);
            
            //case-2 P1(S1) ? P2(S1), S1 = P2
            //http://jenkins-1.dataone.org/documentation/unstable/API-Documentation-development/design/ContentMutability.html
            System.out.println("case 2======= ");
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case2 = new Identifier();
            pid1_case2.setValue(D1NodeServiceTest.generateDocumentId());
            String sid_case2_str= "sid."+System.nanoTime();
            Identifier sid_case2 = new Identifier();
            sid_case2.setValue(sid_case2_str);
            SystemMetadata sysmeta_case2 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case2, session.getSubject(), object);
            sysmeta_case2.setSeriesId(sid_case2);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case2, object, sysmeta_case2);
            
            Thread.sleep(1000);
            Identifier pid2_case2 = new Identifier();
            pid2_case2.setValue(D1NodeServiceTest.generateDocumentId());
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            sysmeta = D1NodeServiceTest.createSystemMetadata(pid2_case2, session.getSubject(), object);
            sysmeta.setSeriesId(sid_case2);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case2, object, sysmeta);
            head = IdentifierManager.getInstance().getHeadPID(sid_case2);
            assertTrue(head.getValue().equals(pid2_case2.getValue()));

            //case-3  P1(S1) <- P2(S1), S1 = P2, Discouraged, but not error condition,
            //S1 = P2 (P1 and P2 are type 1 ends, not an ideal chain )
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case3 = new Identifier();
            pid1_case3.setValue(D1NodeServiceTest.generateDocumentId());
            String sid_case3_str= "sid."+System.nanoTime();
            Identifier sid_case3 = new Identifier();
            sid_case3.setValue(sid_case3_str);
            SystemMetadata sysmeta_case3 = D1NodeServiceTest
                                    .createSystemMetadata(pid1_case3, session.getSubject(), object);
            sysmeta_case3.setSeriesId(sid_case3);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case3, object, sysmeta_case3);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case3 = new Identifier();
            pid2_case3.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case3 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case3, session.getSubject(), object);
            sysmeta2_case3.setSeriesId(sid_case3);
            sysmeta2_case3.setObsoletes(pid1_case3);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case3, object, sysmeta2_case3);
            
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case3);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes() == null);
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case3);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid1_case3));

            System.out.println("case 3: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case3);
            assertTrue(head.equals(pid2_case3));
            
            //case-4 P1(S1) <-> P2(S1) <-> P3(S2), S1 = P2 (Type 2), S2 = P3 (Type 1)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case4 = new Identifier();
            pid1_case4.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case4 = new Identifier();
            sid_case4.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case4 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case4, session.getSubject(), object);
            sysmeta_case4.setSeriesId(sid_case4);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case4, object, sysmeta_case4);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case4 = new Identifier();
            pid2_case4.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case4 = D1NodeServiceTest
                                    .createSystemMetadata(pid2_case4, session.getSubject(), object);
            sysmeta2_case4.setSeriesId(sid_case4);
            sysmeta2_case4.setObsoletes(pid1_case4);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case4, object, sysmeta2_case4);
            
            sysmeta_case4.setObsoletedBy(pid2_case4);
            BigInteger version = BigInteger.ONE.add(BigInteger.ONE);
            sysmeta_case4.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case4, sysmeta_case4);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case4 = new Identifier();
            pid3_case4.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid2_case4 = new Identifier();
            sid2_case4.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta3_case4 = D1NodeServiceTest
                                    .createSystemMetadata(pid3_case4, session.getSubject(), object);
            sysmeta3_case4.setSeriesId(sid2_case4);
            sysmeta3_case4.setObsoletes(pid2_case4);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case4, object, sysmeta3_case4);
            
            sysmeta2_case4.setObsoletedBy(pid3_case4);
            version = version.add(BigInteger.ONE);
            sysmeta2_case4.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case4, sysmeta2_case4);
            
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case4);
            assertTrue(meta.getObsoletedBy().equals(pid2_case4));
            assertTrue(meta.getObsoletes() == null);
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case4);
            assertTrue(meta.getObsoletedBy().equals(pid3_case4));
            assertTrue(meta.getObsoletes().equals(pid1_case4));
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case4);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid2_case4));
            
            System.out.println("case 4: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case4);
            assertTrue(head.equals(pid2_case4));
            head = IdentifierManager.getInstance().getHeadPID(sid2_case4);
            assertTrue(head.equals(pid3_case4));
            
            
            //case-5 P1(S1) <- P2(S1) <- P3(S2), S1 = P2 (P1 and P2 are type 1 ends, not an ideal chain), S2 = P3 (Type 1)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case5 = new Identifier();
            pid1_case5.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case5 = new Identifier();
            sid_case5.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta1_case5 = D1NodeServiceTest
                                    .createSystemMetadata(pid1_case5, session.getSubject(), object);
            sysmeta1_case5.setSeriesId(sid_case5);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case5, object, sysmeta1_case5);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case5 = new Identifier();
            pid2_case5.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case5 = D1NodeServiceTest
                                    .createSystemMetadata(pid2_case5, session.getSubject(), object);
            sysmeta2_case5.setSeriesId(sid_case5);
            sysmeta2_case5.setObsoletes(pid1_case5);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case5, object, sysmeta2_case5);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case5 = new Identifier();
            pid3_case5.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid2_case5 = new Identifier();
            sid2_case5.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta3_case5 = D1NodeServiceTest
                                    .createSystemMetadata(pid3_case5, session.getSubject(), object);
            sysmeta3_case5.setSeriesId(sid2_case5);
            sysmeta3_case5.setObsoletes(pid2_case5);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case5, object, sysmeta3_case5);
            
          //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case5);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes() == null);
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case5);
            assertTrue(meta.getObsoletedBy() == null );
            assertTrue(meta.getObsoletes().equals(pid1_case5));
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case5);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid2_case5));
            
            System.out.println("case 5: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case5);
            assertTrue(head.equals(pid2_case5));
            head = IdentifierManager.getInstance().getHeadPID(sid2_case5);
            assertTrue(head.equals(pid3_case5));
            
            
            //case-6 P1(S1) <-> P2(S1) <-> P3(), S1 = P2 (Type 2)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case6 = new Identifier();
            pid1_case6.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case6 = new Identifier();
            sid_case6.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case6 = D1NodeServiceTest
                                    .createSystemMetadata(pid1_case6, session.getSubject(), object);
            sysmeta_case6.setSeriesId(sid_case6);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case6, object, sysmeta_case6);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case6 = new Identifier();
            pid2_case6.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case6 = D1NodeServiceTest
                                    .createSystemMetadata(pid2_case6, session.getSubject(), object);
            sysmeta2_case6.setSeriesId(sid_case6);
            sysmeta2_case6.setObsoletes(pid1_case6);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case6, object, sysmeta2_case6);
            
            sysmeta_case6.setObsoletedBy(pid2_case6);
            version = BigInteger.ONE.add(BigInteger.ONE);
            sysmeta_case6.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case6, sysmeta_case6);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case6 = new Identifier();
            pid3_case6.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case6 = D1NodeServiceTest
                                    .createSystemMetadata(pid3_case6, session.getSubject(), object);
            sysmeta3_case6.setObsoletes(pid2_case6);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case6, object, sysmeta3_case6);
            
            sysmeta2_case6.setObsoletedBy(pid3_case6);
            version = version.add(BigInteger.ONE);
            sysmeta2_case6.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case6, sysmeta2_case6);
            
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case6);
            assertTrue(meta.getObsoletedBy().equals(pid2_case6));
            assertTrue(meta.getObsoletes() == null);
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case6);
            assertTrue(meta.getObsoletedBy().equals(pid3_case6));
            assertTrue(meta.getObsoletes().equals(pid1_case6));
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case6);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid2_case6));
            
            System.out.println("case 6: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case6);
            assertTrue(head.equals(pid2_case6));
            
            
            //case-7 P1(S1) <-> P2(S1) <-> P3() <-> P4(S2), S1 = P2 (Type 2), S2 = P4 (Type 1)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case7 = new Identifier();
            pid1_case7.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case7 = new Identifier();
            sid_case7.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case7 = D1NodeServiceTest
                                    .createSystemMetadata(pid1_case7, session.getSubject(), object);
            sysmeta_case7.setSeriesId(sid_case7);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case7, object, sysmeta_case7);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case7 = new Identifier();
            pid2_case7.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case7 = D1NodeServiceTest
                                    .createSystemMetadata(pid2_case7, session.getSubject(), object);
            sysmeta2_case7.setSeriesId(sid_case7);
            sysmeta2_case7.setObsoletes(pid1_case7);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case7, object, sysmeta2_case7);
            
            sysmeta_case7.setObsoletedBy(pid2_case7);
            version = version.add(BigInteger.ONE);
            sysmeta_case7.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case7, sysmeta_case7);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case7 = new Identifier();
            pid3_case7.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case7 = D1NodeServiceTest
                                    .createSystemMetadata(pid3_case7, session.getSubject(), object);
            sysmeta3_case7.setObsoletes(pid2_case7);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case7, object, sysmeta3_case7);
            
            sysmeta2_case7.setObsoletedBy(pid3_case7);
            version = version.add(BigInteger.ONE);
            sysmeta2_case7.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case7, sysmeta2_case7);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid4_case7 = new Identifier();
            pid4_case7.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid2_case7 = new Identifier();
            sid2_case7.setValue("sid." + System.nanoTime());
            SystemMetadata sysmeta4_case7 = D1NodeServiceTest
                                    .createSystemMetadata(pid4_case7, session.getSubject(), object);
            sysmeta4_case7.setObsoletes(pid3_case7);
            sysmeta4_case7.setSeriesId(sid2_case7);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid4_case7, object, sysmeta4_case7);

            sysmeta3_case7.setObsoletedBy(pid4_case7);
            version = version.add(BigInteger.ONE);
            sysmeta3_case7.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid3_case7, sysmeta3_case7);

            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case7);
            assertTrue(meta.getObsoletedBy().equals(pid2_case7));
            assertTrue(meta.getObsoletes() == null);
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case7);
            assertTrue(meta.getObsoletedBy().equals(pid3_case7));
            assertTrue(meta.getObsoletes().equals(pid1_case7));
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case7);
            assertTrue(meta.getObsoletedBy().equals(pid4_case7));
            assertTrue(meta.getObsoletes().equals(pid2_case7));
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid4_case7);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid3_case7));
            
            System.out.println("case 7: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case7);
            assertTrue(head.equals(pid2_case7));
            head = IdentifierManager.getInstance().getHeadPID(sid2_case7);
            assertTrue(head.equals(pid4_case7));

            //case-8  P1(S1) <-> P2(S1) -> ?? <- P4(S1), S1 = P4, (Type 1) (Error, but will happen)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case8 = new Identifier();
            pid1_case8.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case8 = new Identifier();
            sid_case8.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case8 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case8, session.getSubject(), object);
            sysmeta_case8.setSeriesId(sid_case8);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case8, object, sysmeta_case8);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case8 = new Identifier();
            pid2_case8.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case8 = D1NodeServiceTest
                                    .createSystemMetadata(pid2_case8, session.getSubject(), object);
            sysmeta2_case8.setSeriesId(sid_case8);
            sysmeta2_case8.setObsoletes(pid1_case8);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case8, object, sysmeta2_case8);

            sysmeta_case8.setObsoletedBy(pid2_case8);
            version = version.add(BigInteger.ONE);
            sysmeta_case8.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case8, sysmeta_case8);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case8 = new Identifier();
            pid3_case8.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case8 = D1NodeServiceTest
                                    .createSystemMetadata(pid3_case8, session.getSubject(), object);
            sysmeta3_case8.setObsoletes(pid2_case8);
            sysmeta3_case8.setSeriesId(sid_case8);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case8, object, sysmeta3_case8);

            sysmeta2_case8.setObsoletedBy(pid3_case8);
            version = version.add(BigInteger.ONE);
            sysmeta2_case8.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case8, sysmeta2_case8);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid4_case8 = new Identifier();
            pid4_case8.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta4_case8 = D1NodeServiceTest
                                    .createSystemMetadata(pid4_case8, session.getSubject(), object);
            sysmeta4_case8.setObsoletes(pid3_case8);
            sysmeta4_case8.setSeriesId(sid_case8);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid4_case8, object, sysmeta4_case8);

            //delete pid3_case8 
            CNodeService.getInstance(request).delete(session, pid3_case8);
            try {
                CNodeService.getInstance(request).getSystemMetadata(session, pid3_case8);
                fail("The pid "+pid3_case8.getValue()+" should be deleted.");
            } catch (NotFound e) {
                assertTrue(true);
            }

            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case8);
            assertTrue(meta.getObsoletedBy().equals(pid2_case8));
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case8);
            assertTrue(meta.getObsoletedBy().equals(pid3_case8));
            assertTrue(meta.getObsoletes().equals(pid1_case8));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid4_case8);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid3_case8));

            System.out.println("case 8: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case8);
            assertTrue(head.equals(pid4_case8));

            //case-9  P1(S1) <-> P2(S1) ?? <- P4(S1), S1 = P4 (P2 and P4 are type 1 ends, not an ideal chain)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case9 = new Identifier();
            pid1_case9.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case9 = new Identifier();
            sid_case9.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case9 = D1NodeServiceTest
                                    .createSystemMetadata(pid1_case9, session.getSubject(), object);
            sysmeta_case9.setSeriesId(sid_case9);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case9, object, sysmeta_case9);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case9 = new Identifier();
            pid2_case9.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case9 = D1NodeServiceTest
                                    .createSystemMetadata(pid2_case9, session.getSubject(), object);
            sysmeta2_case9.setSeriesId(sid_case9);
            sysmeta2_case9.setObsoletes(pid1_case9);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case9, object, sysmeta2_case9);
            
            sysmeta_case9.setObsoletedBy(pid2_case9);
            version = version.add(BigInteger.ONE);
            sysmeta_case9.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case9, sysmeta_case9);
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case9);
            assertTrue(meta.getObsoletedBy().equals(pid2_case9));
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case9 = new Identifier();
            pid3_case9.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case9 = D1NodeServiceTest
                                    .createSystemMetadata(pid3_case9, session.getSubject(), object);
            sysmeta3_case9.setObsoletes(pid2_case9);
            sysmeta3_case9.setSeriesId(sid_case9);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case9, object, sysmeta3_case9);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid4_case9 = new Identifier();
            pid4_case9.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta4_case9 = D1NodeServiceTest
                                .createSystemMetadata(pid4_case9, session.getSubject(), object);
            sysmeta4_case9.setObsoletes(pid3_case9);
            sysmeta4_case9.setSeriesId(sid_case9);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid4_case9, object, sysmeta4_case9);

            //delete pid3_case8 
            CNodeService.getInstance(request).delete(session, pid3_case9);
            try {
                CNodeService.getInstance(request).getSystemMetadata(session, pid3_case9);
                fail("The pid "+pid3_case9.getValue()+" should be deleted.");
            } catch (NotFound e) {
                assertTrue(true);
            }

            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case9);
            assertTrue(meta.getObsoletedBy().equals(pid2_case9));
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case9);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid1_case9));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid4_case9);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid3_case9));

            System.out.println("case 9: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case9);
            assertTrue(head.equals(pid4_case9));

            //case-10   P1(S1) <-> P2(S1) -> XX <- P4(S1), S1 = P4, (Type 1) 
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case10 = new Identifier();
            pid1_case10.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case10 = new Identifier();
            sid_case10.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case10 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case10, session.getSubject(), object);
            sysmeta_case10.setSeriesId(sid_case10);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case10, object, sysmeta_case10);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case10 = new Identifier();
            pid2_case10.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case10 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case10, session.getSubject(), object);
            sysmeta2_case10.setSeriesId(sid_case10);
            sysmeta2_case10.setObsoletes(pid1_case10);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case10, object, sysmeta2_case10);
            
            sysmeta_case10.setObsoletedBy(pid2_case10);
            version = version.add(BigInteger.ONE);
            sysmeta_case10.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case10, sysmeta_case10);
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case10);
            assertTrue(meta.getObsoletedBy().equals(pid2_case10));
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case10 = new Identifier();
            pid3_case10.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case10 = D1NodeServiceTest
                                .createSystemMetadata(pid3_case10, session.getSubject(), object);
            sysmeta3_case10.setObsoletes(pid2_case10);
            sysmeta3_case10.setSeriesId(sid_case10);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case10, object, sysmeta3_case10);

            sysmeta2_case10.setObsoletedBy(pid3_case10);
            version = version.add(BigInteger.ONE);
            sysmeta2_case10.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case10, sysmeta2_case10);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid4_case10 = new Identifier();
            pid4_case10.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta4_case10 = D1NodeServiceTest
                                .createSystemMetadata(pid4_case10, session.getSubject(), object);
            sysmeta4_case10.setObsoletes(pid3_case10);
            sysmeta4_case10.setSeriesId(sid_case10);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid4_case10, object, sysmeta4_case10);

            //delete pid3_case10 
            CNodeService.getInstance(request).delete(session, pid3_case10);
            try {
                CNodeService.getInstance(request).getSystemMetadata(session, pid3_case10);
                fail("The pid "+pid3_case10.getValue()+" should be deleted.");
            } catch (NotFound e) {
                //e.printStackTrace();
                assertTrue(true);
            }

           //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case10);
            assertTrue(meta.getObsoletedBy().equals(pid2_case10));
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case10);
            assertTrue(meta.getObsoletedBy().equals(pid3_case10));
            assertTrue(meta.getObsoletes().equals(pid1_case10));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid4_case10);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid3_case10));

            System.out.println("case 10: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case10);
            assertTrue(head.equals(pid4_case10));

            //case-11   P1(S1) <-> P2(S1) <-> [archived:P3(S1)], S1 = P3, (Type 1)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case11 = new Identifier();
            pid1_case11.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case11 = new Identifier();
            sid_case11.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case11 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case11, session.getSubject(), object);
            sysmeta_case11.setSeriesId(sid_case11);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case11, object, sysmeta_case11);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case11 = new Identifier();
            pid2_case11.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case11 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case11, session.getSubject(), object);
            sysmeta2_case11.setSeriesId(sid_case11);
            sysmeta2_case11.setObsoletes(pid1_case11);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case11, object, sysmeta2_case11);

            sysmeta_case11.setObsoletedBy(pid2_case11);
            version = version.add(BigInteger.ONE);
            sysmeta_case11.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case11, sysmeta_case11);
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case11);
            assertTrue(meta.getObsoletedBy().equals(pid2_case11));

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case11 = new Identifier();
            pid3_case11.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case11 = D1NodeServiceTest
                                .createSystemMetadata(pid3_case11, session.getSubject(), object);
            sysmeta3_case11.setObsoletes(pid2_case11);
            sysmeta3_case11.setSeriesId(sid_case11);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case11, object, sysmeta3_case11);

            sysmeta2_case11.setObsoletedBy(pid3_case11);
            version = version.add(BigInteger.ONE);
            sysmeta2_case11.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case11, sysmeta2_case11);

            //archive pid3_case11 
            sysmeta3_case11.setArchived(true);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid3_case11, sysmeta3_case11);
            //CNodeService.getInstance(request).archive(session, pid3_case11);

            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case11);
            assertTrue(meta.getObsoletedBy().equals(pid2_case11));
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case11);
            assertTrue(meta.getObsoletedBy().equals(pid3_case11));
            assertTrue(meta.getObsoletes().equals(pid1_case11));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case11);
            assertTrue(meta.getArchived());
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid2_case11));

            System.out.println("case 11: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case11);
            assertTrue(head.equals(pid3_case11));

            //case-12   P1(S1) <-> P2(S1) -> ??, S1 = P2, (Type 2) (Error, but will happen)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case12 = new Identifier();
            pid1_case12.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case12 = new Identifier();
            sid_case12.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case12 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case12, session.getSubject(), object);
            sysmeta_case12.setSeriesId(sid_case12);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case12, object, sysmeta_case12);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case12 = new Identifier();
            pid2_case12.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case12 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case12, session.getSubject(), object);
            sysmeta2_case12.setSeriesId(sid_case12);
            sysmeta2_case12.setObsoletes(pid1_case12);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case12, object, sysmeta2_case12);

            sysmeta_case12.setObsoletedBy(pid2_case12);
            version = version.add(BigInteger.ONE);
            sysmeta_case12.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case12, sysmeta_case12);
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case12);
            assertTrue(meta.getObsoletedBy().equals(pid2_case12));

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case12 = new Identifier();
            pid3_case12.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case12 = D1NodeServiceTest
                                .createSystemMetadata(pid3_case12, session.getSubject(), object);
            sysmeta3_case12.setObsoletes(pid2_case12);
            sysmeta3_case12.setSeriesId(sid_case12);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case12, object, sysmeta3_case12);

            sysmeta2_case12.setObsoletedBy(pid3_case12);
            version = version.add(BigInteger.ONE);
            sysmeta2_case12.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case12, sysmeta2_case12);

            //archive pid3_case12 
            MNodeService.getInstance(request).delete(session, pid3_case12);
            try {
                meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case12);
                fail("The pid "+pid3_case12.getValue()+" should be deleted.");
            } catch (NotFound ee) {
                assertTrue(true);
            }

            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case12);
            assertTrue(meta.getObsoletedBy().equals(pid2_case12));
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case12);
            assertTrue(meta.getObsoletedBy().equals(pid3_case12));
            assertTrue(meta.getObsoletes().equals(pid1_case12));

            System.out.println("case 12: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case12);
            assertTrue(head.equals(pid2_case12));

            //case-13   P1(S1) <- P2(S1) -> ??, S1 = P2 (P1 is a type 1 end and P2 is a type 2 end,
            //not an ideal chain)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case13 = new Identifier();
            pid1_case13.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case13 = new Identifier();
            sid_case13.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case13 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case13, session.getSubject(), object);
            sysmeta_case13.setSeriesId(sid_case13);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case13, object, sysmeta_case13);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case13 = new Identifier();
            pid2_case13.setValue(D1NodeServiceTest.generateDocumentId());
            Thread.sleep(1000);
            Identifier pid3_case13 = new Identifier();
            pid3_case13.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case13 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case13, session.getSubject(), object);
            sysmeta2_case13.setSeriesId(sid_case13);
            sysmeta2_case13.setObsoletes(pid1_case13);
            sysmeta2_case13.setObsoletedBy(pid3_case13);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case13, object, sysmeta2_case13);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case13);
            assertTrue(meta.getObsoletedBy().equals(pid3_case13));
            assertTrue(meta.getObsoletes().equals(pid1_case13));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid1_case13);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes() == null);

            System.out.println("case 13: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case13);
            assertTrue(head.equals(pid2_case13));

            //case-14   P1(S1) <- P2(S1) -> P3(S2), S1 = P2 (P1 is a type one end and P2 is a type 2
            //end, not an ideal chain)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case14 = new Identifier();
            pid1_case14.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case14 = new Identifier();
            sid_case14.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case14 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case14, session.getSubject(), object);
            sysmeta_case14.setSeriesId(sid_case14);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case14, object, sysmeta_case14);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case14 = new Identifier();
            pid2_case14.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case14 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case14, session.getSubject(), object);
            sysmeta2_case14.setSeriesId(sid_case14);
            sysmeta2_case14.setObsoletes(pid1_case14);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case14, object, sysmeta2_case14);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case14 = new Identifier();
            pid3_case14.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid2_case14 = new Identifier();
            sid2_case14.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta3_case14 = D1NodeServiceTest
                                .createSystemMetadata(pid3_case14, session.getSubject(), object);
            sysmeta3_case14.setSeriesId(sid2_case14);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case14, object, sysmeta3_case14);

            sysmeta2_case14.setObsoletedBy(pid3_case14);
            version = version.add(BigInteger.ONE);
            sysmeta2_case14.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case14, sysmeta2_case14);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case14);
            assertTrue(meta.getObsoletedBy().equals(pid3_case14));
            assertTrue(meta.getObsoletes().equals(pid1_case14));
            
            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid1_case14);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case14);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes() == null);

            System.out.println("case 14: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case14);
            assertTrue(head.equals(pid2_case14));
            head = IdentifierManager.getInstance().getHeadPID(sid2_case14);
            assertTrue(head.equals(pid3_case14));

            //case-15    P1(S1) <-> P2(S1) ?? <- P4(S1) <-> P5(S2), S1 = P4 (P2 is a type 1 end
            // and P4 is a type 2 end, not an ideal chain)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case15 = new Identifier();
            pid1_case15.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case15 = new Identifier();
            sid_case15.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case15 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case15, session.getSubject(), object);
            sysmeta_case15.setSeriesId(sid_case15);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case15, object, sysmeta_case15);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case15 = new Identifier();
            pid2_case15.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case15 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case15, session.getSubject(), object);
            sysmeta2_case15.setSeriesId(sid_case15);
            sysmeta2_case15.setObsoletes(pid1_case15);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case15, object, sysmeta2_case15);

            sysmeta_case15.setObsoletedBy(pid2_case15);
            version = version.add(BigInteger.ONE);
            sysmeta_case15.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case15, sysmeta_case15);
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case15);
            assertTrue(meta.getObsoletedBy().equals(pid2_case15));

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case15 = new Identifier();
            pid3_case15.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case15 = D1NodeServiceTest
                                .createSystemMetadata(pid3_case15, session.getSubject(), object);
            sysmeta3_case15.setSeriesId(sid_case15);
            sysmeta3_case15.setObsoletes(pid2_case15);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case15, object, sysmeta3_case15);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid4_case15 = new Identifier();
            pid4_case15.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta4_case15 = D1NodeServiceTest
                                .createSystemMetadata(pid4_case15, session.getSubject(), object);
            sysmeta4_case15.setSeriesId(sid_case15);
            sysmeta4_case15.setObsoletes(pid3_case15);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid4_case15, object, sysmeta4_case15);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid5_case15 = new Identifier();
            pid5_case15.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid2_case15 = new Identifier();
            sid2_case15.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta5_case15 = D1NodeServiceTest
                                .createSystemMetadata(pid5_case15, session.getSubject(), object);
            sysmeta5_case15.setSeriesId(sid2_case15);
            sysmeta5_case15.setObsoletes(pid4_case15);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid5_case15, object, sysmeta5_case15);

            sysmeta4_case15.setObsoletedBy(pid5_case15);
            version = version.add(BigInteger.ONE);
            sysmeta4_case15.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid4_case15, sysmeta4_case15);

            CNodeService.getInstance(request).delete(session, pid3_case15);
            try {
                meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case15);
                fail("The pid "+pid3_case15.getValue()+" should be deleted.");
            } catch (NotFound ee) {
                assertTrue(true);
            }
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case15);
            assertTrue(meta.getObsoletedBy().equals(pid2_case15));
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case15);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid1_case15));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid4_case15);
            assertTrue(meta.getObsoletedBy().equals(pid5_case15));
            assertTrue(meta.getObsoletes().equals(pid3_case15));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid5_case15);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid4_case15));
            
            System.out.println("case 15: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case15);
            assertTrue(head.equals(pid4_case15));
            head = IdentifierManager.getInstance().getHeadPID(sid2_case15);
            assertTrue(head.equals(pid5_case15));

          //case-16   P1(S1) <- P2(S1) -> ?? <-P4(S2) S1 = P2 (P1 is a type 1 end and P2 is a type
            //2 ends, not an ideal chain), S2=P4 (rule1)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case16 = new Identifier();
            pid1_case16.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case16 = new Identifier();
            sid_case16.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case16 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case16, session.getSubject(), object);
            sysmeta_case16.setSeriesId(sid_case16);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case16, object, sysmeta_case16);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case16 = new Identifier();
            pid2_case16.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case16 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case16, session.getSubject(), object);
            sysmeta2_case16.setSeriesId(sid_case16);
            sysmeta2_case16.setObsoletes(pid1_case16);
            d1NodeServiceTest.cnCreate(session, pid2_case16, object, sysmeta2_case16);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case16 = new Identifier();
            pid3_case16.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case16 = D1NodeServiceTest
                                .createSystemMetadata(pid3_case16, session.getSubject(), object);
            sysmeta3_case16.setSeriesId(sid_case16);
            sysmeta3_case16.setObsoletes(pid2_case16);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case16, object, sysmeta3_case16);

            sysmeta2_case16.setObsoletedBy(pid3_case16);
            version = version.add(BigInteger.ONE);
            sysmeta2_case16.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case16, sysmeta2_case16);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid4_case16 = new Identifier();
            pid4_case16.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid2_case16 = new Identifier();
            sid2_case16.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta4_case16 = D1NodeServiceTest
                                .createSystemMetadata(pid4_case16, session.getSubject(), object);
            sysmeta4_case16.setSeriesId(sid2_case16);
            sysmeta4_case16.setObsoletes(pid3_case16);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid4_case16, object, sysmeta4_case16);

            CNodeService.getInstance(request).delete(session, pid3_case16);
            try {
                meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case16);
                fail("The pid "+pid3_case16.getValue()+" should be deleted.");
            } catch (NotFound ee) {
                assertTrue(true);
            }
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case16);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case16);
            assertTrue(meta.getObsoletedBy().equals(pid3_case16));
            assertTrue(meta.getObsoletes().equals(pid1_case16));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid4_case16);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid3_case16));

            System.out.println("case 16: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case16);
            assertTrue(head.equals(pid2_case16));
            head = IdentifierManager.getInstance().getHeadPID(sid2_case16);
            assertTrue(head.equals(pid4_case16));

            //case-17   P1(S1) <- P2(S1) -> ?? <-P4(S1) S1 = P4 (P1 and P4 are type 1 ends, not an ideal chain)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case17 = new Identifier();
            pid1_case17.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case17 = new Identifier();
            sid_case17.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case17 = D1NodeServiceTest
                                    .createSystemMetadata(pid1_case17, session.getSubject(), object);
            sysmeta_case17.setSeriesId(sid_case17);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case17, object, sysmeta_case17);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case17 = new Identifier();
            pid2_case17.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case17 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case17, session.getSubject(), object);
            sysmeta2_case17.setSeriesId(sid_case17);
            sysmeta2_case17.setObsoletes(pid1_case17);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case17, object, sysmeta2_case17);
   
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case17 = new Identifier();
            pid3_case17.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case17 = D1NodeServiceTest
                                .createSystemMetadata(pid3_case17, session.getSubject(), object);
            sysmeta3_case17.setSeriesId(sid_case17);
            sysmeta3_case17.setObsoletes(pid2_case17);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case17, object, sysmeta3_case17);

            sysmeta2_case17.setObsoletedBy(pid3_case17);
            version = version.add(BigInteger.ONE);
            sysmeta2_case17.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case17, sysmeta2_case17);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid4_case17 = new Identifier();
            pid4_case17.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta4_case17 = D1NodeServiceTest
                                .createSystemMetadata(pid4_case17, session.getSubject(), object);
            sysmeta4_case17.setSeriesId(sid_case17);
            sysmeta4_case17.setObsoletes(pid3_case17);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid4_case17, object, sysmeta4_case17);

            CNodeService.getInstance(request).delete(session, pid3_case17);
            try {
                meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case17);
                fail("The pid "+pid3_case17.getValue()+" should be deleted.");
            } catch (NotFound ee) {
                assertTrue(true);
            }
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case17);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case17);
            assertTrue(meta.getObsoletedBy().equals(pid3_case17));
            assertTrue(meta.getObsoletes().equals(pid1_case17));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid4_case17);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid3_case17));

            System.out.println("case 17: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case17);
            assertTrue(head.equals(pid4_case17));

            //case-18    P1(S1) <->P2(S1) -> ?? ???<-P5(S1) S1 = P5 (P2 is a type 2 end and P4 is
            //a type 1 end, not an ideal chain)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case18 = new Identifier();
            pid1_case18.setValue(D1NodeServiceTest.generateDocumentId());
            Identifier sid_case18 = new Identifier();
            sid_case18.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case18 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case18, session.getSubject(), object);
            sysmeta_case18.setSeriesId(sid_case18);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case18, object, sysmeta_case18);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case18 = new Identifier();
            pid2_case18.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta2_case18 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case18, session.getSubject(), object);
            sysmeta2_case18.setSeriesId(sid_case18);
            sysmeta2_case18.setObsoletes(pid1_case18);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case18, object, sysmeta2_case18);

            sysmeta_case18.setObsoletedBy(pid2_case18);
            version = version.add(BigInteger.ONE);
            sysmeta_case18.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid1_case18, sysmeta_case18);

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case18 = new Identifier();
            pid3_case18.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta3_case18 = D1NodeServiceTest
                                    .createSystemMetadata(pid3_case18, session.getSubject(), object);
            sysmeta3_case18.setSeriesId(sid_case18);
            sysmeta3_case18.setObsoletes(pid2_case18);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case18, object, sysmeta3_case18);
            
            sysmeta2_case18.setObsoletedBy(pid3_case18);
            version = version.add(BigInteger.ONE);
            sysmeta2_case18.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid2_case18, sysmeta2_case18);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid4_case18 = new Identifier();
            pid4_case18.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta4_case18 = D1NodeServiceTest
                                   .createSystemMetadata(pid4_case18, session.getSubject(), object);
            sysmeta4_case18.setSeriesId(sid_case18);
            sysmeta4_case18.setObsoletes(pid3_case18);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid4_case18, object, sysmeta4_case18);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid5_case18 = new Identifier();
            pid5_case18.setValue(D1NodeServiceTest.generateDocumentId());
            SystemMetadata sysmeta5_case18 = D1NodeServiceTest
                                   .createSystemMetadata(pid5_case18, session.getSubject(), object);
            sysmeta5_case18.setSeriesId(sid_case18);
            sysmeta5_case18.setObsoletes(pid4_case18);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid5_case18, object, sysmeta5_case18);
            
            sysmeta4_case18.setObsoletedBy(pid5_case18);
            version = version.add(BigInteger.ONE);
            sysmeta4_case18.setSerialVersion(version);
            MNodeService.getInstance(request).updateSystemMetadata(session, pid4_case18, sysmeta4_case18);
            
            CNodeService.getInstance(request).delete(session, pid3_case18);
            try {
                meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid3_case18);
                fail("The pid "+pid3_case18.getValue()+" should be deleted.");
            } catch (NotFound ee) {
                assertTrue(true);
            }
            
            CNodeService.getInstance(request).delete(session, pid4_case18);
            try {
                meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid4_case18);
                fail("The pid "+pid4_case18.getValue()+" should be deleted.");
            } catch (NotFound ee) {
                assertTrue(true);
            }
            //check
            meta = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case18);
            assertTrue(meta.getObsoletedBy().equals(pid2_case18));
            assertTrue(meta.getObsoletes() == null);

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid2_case18);
            assertTrue(meta.getObsoletedBy().equals(pid3_case18));
            assertTrue(meta.getObsoletes().equals(pid1_case18));

            meta =  CNodeService.getInstance(request).getSystemMetadata(session, pid5_case18);
            assertTrue(meta.getObsoletedBy() == null);
            assertTrue(meta.getObsoletes().equals(pid4_case18));

            System.out.println("case 18: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case18);
            assertTrue(head.equals(pid5_case18));

            //case-19 This is about the time stamps messing up.
           //P1(S1,t1) <-P2(S1,t2) <-P3(S1, t3) S1 = P3 (t1, t2 and t3 are the time stamp
            //while t1 >t2 > t3. P1, P2 and P3 are a type 1 end, not an ideal chain.
           //So we will pick up P1 as the head since t1 is the lastest one. However, if we also
            //consider the obsoletes chain, we will pick up P3)
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case19 = new Identifier();
            pid3_case19.setValue(D1NodeServiceTest.generateDocumentId());
            System.out.println("pid3 is "+pid3_case19.getValue());
            Thread.sleep(1000);
            Identifier pid2_case19 = new Identifier();
            pid2_case19.setValue(D1NodeServiceTest.generateDocumentId());
            System.out.println("pid2 is "+pid2_case19.getValue());
            Thread.sleep(1000);
            Identifier pid1_case19 = new Identifier();
            pid1_case19.setValue(D1NodeServiceTest.generateDocumentId());
            System.out.println("pid1 is "+pid1_case19.getValue());

            Identifier sid_case19 = new Identifier();
            sid_case19.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta3_case19 = D1NodeServiceTest
                                .createSystemMetadata(pid3_case19, session.getSubject(), object);
            sysmeta3_case19.setSeriesId(sid_case19);
            sysmeta3_case19.setObsoletes(pid2_case19);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid3_case19, object, sysmeta3_case19);
            SystemMetadata sys3 = CNodeService.getInstance(request).getSystemMetadata(session, pid3_case19);
            Date time3 = sys3.getDateUploaded();
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            SystemMetadata sysmeta2_case19 = D1NodeServiceTest
                                .createSystemMetadata(pid2_case19, session.getSubject(), object);
            sysmeta2_case19.setSeriesId(sid_case19);
            sysmeta2_case19.setObsoletes(pid1_case19);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid2_case19, object, sysmeta2_case19);
            SystemMetadata sys2 = CNodeService.getInstance(request).getSystemMetadata(session, pid2_case19);
            Date time2 = sys2.getDateUploaded();

            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            SystemMetadata sysmeta1_case19 = D1NodeServiceTest
                                .createSystemMetadata(pid1_case19, session.getSubject(), object);
            sysmeta1_case19.setSeriesId(sid_case19);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            d1NodeServiceTest.cnCreate(session, pid1_case19, object, sysmeta1_case19);
            SystemMetadata sys1 = CNodeService.getInstance(request).getSystemMetadata(session, pid1_case19);
            Date time1 = sys1.getDateUploaded();

            //time1 > time2 > time3
            assertTrue(time1.getTime()>time2.getTime());
            assertTrue(time2.getTime()>time3.getTime());
            
            System.out.println("case 19: =======");
            Identifier head2 = IdentifierManager.getInstance().getHeadPID(sid_case19);
            assertTrue(head2.equals(pid3_case19));

        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /** 
     * Insert a test document, returning the docid that was used. 
     */
    private String insertTestDocument() throws Exception{
        String path = "test/eml-2.2.0.xml";
        Session session = getTestSession();
        String metadataIdStr = D1NodeServiceTest.generateDocumentId() + ".1";
        Identifier metadataId = new Identifier();
        metadataId.setValue(metadataIdStr);
        InputStream metadataObject = new FileInputStream(new File(path));
        SystemMetadata sysmeta = D1NodeServiceTest.
                        createSystemMetadata(metadataId, session.getSubject(), metadataObject);
        metadataObject.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta.setFormatId(formatId);
        metadataObject = new FileInputStream(new File(path));
        d1NodeServiceTest.mnCreate(session, metadataId, metadataObject, sysmeta);
        return metadataIdStr;
    }

    private void ph(String s) {
        System.out.println("*********************** " + s + " ****************************");
    }

    /**
     * Test the querySystemMetadata method
     * @throws Exception
     */
    @Test
    public void testQuerySystemMetadata() throws Exception {
        String nodeIdStr="rtgf:test:ert";
        Date startTime = null;
        Date endTime = null;
        ObjectFormatIdentifier objectFormatId = null;
        NodeReference nodeId = null;
        int start = 0;
        int count =1000;
        Identifier identifier = null;
        boolean isSID = false;
        ObjectList list = IdentifierManager.getInstance().querySystemMetadata(startTime, endTime,
                objectFormatId, nodeId, start, count, identifier, isSID);
        int size1= list.sizeObjectInfoList();
        assertTrue( size1>0);
        nodeId = new NodeReference();
        String currentNodeId = Settings.getConfiguration().getString("dataone.nodeId");
        nodeId.setValue(currentNodeId);
        list = IdentifierManager.getInstance().querySystemMetadata(startTime, endTime,
                objectFormatId, nodeId, start, count, identifier, isSID);
        int size2= list.sizeObjectInfoList();
        assertTrue( size2 > 0);
        assertTrue( size1 >= size2);
        nodeId.setValue("there_bei_we12");
        list = IdentifierManager.getInstance().querySystemMetadata(startTime, endTime,
                objectFormatId, nodeId, start, count, identifier, isSID);
        int size3 = list.sizeObjectInfoList();
        assertTrue(size3==0);

        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(D1NodeServiceTest.generateDocumentId());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
        nodeId.setValue(nodeIdStr);
        sysmeta.setAuthoritativeMemberNode(nodeId);
        String sid1= "sid."+System.nanoTime();
        Identifier seriesId = new Identifier();
        seriesId.setValue(sid1);
        System.out.println("the first sid is "+seriesId.getValue());
        sysmeta.setSeriesId(seriesId);
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        Thread.sleep(5000);
        list = IdentifierManager.getInstance().querySystemMetadata(startTime, endTime,
                objectFormatId, nodeId, start, count, identifier, isSID);
        int size4 = list.sizeObjectInfoList();
        assertTrue(size4 > 0);
    }


    /**
     * Test the existsInIdentifierTable method
     * @throws Exception
     */
    @Test
    public void testExistsInIdentifierTable() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(D1NodeServiceTest.generateDocumentId());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        assertTrue("The identifier " + guid.getValue() + " should exist on the talbe.",
                                    IdentifierManager.getInstance().existsInIdentifierTable(guid));
        Identifier guid2 = new Identifier();
        guid2.setValue(D1NodeServiceTest.generateDocumentId());
        assertFalse("The identifier " + guid2.getValue() + " shouldn't exist on the talbe.",
                                IdentifierManager.getInstance().existsInIdentifierTable(guid2));
    }

    /**
     * Test if local id exists in the xml_revision table
     */
    @Test
    public void testExistsInXmlRevisionTable() {
        try {
            String localId = "test.12";
            int rev =1456789090;
            assertFalse("The object " + localId + " should not exists in the xml_revisions table.",
                        IdentifierManager.getInstance().existsInXmlLRevisionTable(localId, rev));
        } catch (Exception e) {
            fail("testExistsInXmlRevisiontable failed since" +e.getMessage());
        }

    }
    /**
     * We want to act as the CN itself
     * @throws ServiceFailure 
     * @throws Exception 
     */
    public Session getTestSession() throws Exception {
        Session session = d1NodeServiceTest.getTestSession();

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
    }

    /**
     * Test the getGUIDs method for either the guid matches the scheme or the series id matches the scheme
     * @throws Exception
     */
    @Test
    public void getGetGUIDs() throws Exception {
        String urnScheme = "urn:uuid:";
        Session session = getTestSession();
        
        //create an object whose identifier is a uuid
        UUID uuid = UUID.randomUUID();
        String str1 = uuid.toString();
        Identifier guid = new Identifier();
        guid.setValue(urnScheme+str1); 
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        
        //create an object whose identifier is not a uuid, but its series id is
        Identifier guid2 = new Identifier();
        guid2.setValue(D1NodeServiceTest.generateDocumentId());
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta2 = D1NodeServiceTest
                                        .createSystemMetadata(guid2, session.getSubject(), object);
        UUID uuid2 = UUID.randomUUID();
        String str2 = uuid2.toString();
        Identifier seriesId = new Identifier();
        seriesId.setValue(urnScheme+str2); 
        sysmeta2.setSeriesId(seriesId);
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        d1NodeServiceTest.mnCreate(session, guid2, object, sysmeta2);
        
        String nodeId = MNodeService.getInstance(request).getCapabilities().getIdentifier().getValue();
        List<String> ids = IdentifierManager.getInstance()
                                           .getGUIDs("application/octet-stream", nodeId, urnScheme);
        //System.out.println("========the ids is \n"+ids);
        assertTrue(ids.contains(guid.getValue()));
        assertTrue(ids.contains(guid2.getValue()));
    }
    
    /**
     * Test the method of 
     * @throws Exception
     */
    @Test
    public void textGetAllPidsInChain() throws Exception {
        String urnScheme = "urn:uuid:";
        Session session = getTestSession();
        
        //create an object whose identifier is a uuid
        UUID uuid = UUID.randomUUID();
        String str1 = uuid.toString();
        Identifier guid = new Identifier();
        guid.setValue(urnScheme+str1); 
        
        uuid = UUID.randomUUID();
        String sidStr = uuid.toString();
        Identifier sid = new Identifier();
        sid.setValue(urnScheme+sidStr); 
        
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setSeriesId(sid);
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        
        //create an object whose identifier is not a uuid, but its series id is
        Identifier guid2 = new Identifier();
        guid2.setValue(D1NodeServiceTest.generateDocumentId());
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta2 = D1NodeServiceTest
                                    .createSystemMetadata(guid2, session.getSubject(), object);
        sysmeta2.setSeriesId(sid);
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        d1NodeServiceTest.mnUpdate(session, guid, object, guid2, sysmeta2);
        
        List<String> pids = IdentifierManager.getInstance().getAllPidsInChain(sid.getValue());
        assertTrue(pids.size() == 2);
        assertTrue(pids.contains(guid.getValue()));
        assertTrue(pids.contains(guid2.getValue()));
    }
    
    /**
     * Test the getGUIDsByTimeRange
     * @throws Exception
     */
    @Test
    public void testGetGUIDsByTimeRange() throws Exception {
        String urnScheme = "urn:uuid:";
        Session session = getTestSession();
        UUID uuid = UUID.randomUUID();
        String str1 = uuid.toString();
        Identifier guid = new Identifier();
        guid.setValue(urnScheme + str1); 
        
        Calendar cal = Calendar.getInstance();
        cal.set(2000, Calendar.MARCH,04);
        Date date2000 = cal.getTime();
        Date date1 = new Date();
        List<String> list = IdentifierManager.getInstance().getGUIDsByTimeRange(null, date1);
        assertTrue(list.size() > 0);
        assertTrue(!list.contains(urnScheme + str1));
        
        //create an object whose identifier is a uuid
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        Thread.sleep(1000);
        Date date2 = new Date();
        List<String> list2 = IdentifierManager.getInstance().getGUIDsByTimeRange(date2000, date2);
        assertTrue(list2.contains(urnScheme + str1));
        assertTrue(list2.get(list2.size()-1).equals(urnScheme + str1));
        List<String> list3 = IdentifierManager.getInstance().getGUIDsByTimeRange(date2000, null);
        assertTrue(list3.contains(urnScheme + str1));
        assertTrue(list3.get(list3.size()-1).equals(urnScheme + str1));
        List<String> list4 = IdentifierManager.getInstance().getGUIDsByTimeRange(null, null);
        assertTrue(list4.contains(urnScheme + str1));
        assertTrue(list2.get(list2.size()-1).equals(urnScheme + str1));
        List<String> list5 = IdentifierManager.getInstance().getGUIDsByTimeRange(date2, null);
        if (list5 != null) {
            assertTrue(!list5.contains(urnScheme + str1));
        }
    }
}
