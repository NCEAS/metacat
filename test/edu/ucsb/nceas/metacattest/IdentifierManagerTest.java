/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: jones $'
 *     '$Date: 2010-02-03 17:58:12 -0900 (Wed, 03 Feb 2010) $'
 * '$Revision: 5211 $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.ucsb.nceas.metacattest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.AccessionNumber;
import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.ServiceFailure;

public class IdentifierManagerTest extends D1NodeServiceTest {
    private String badGuid = "test:testIdThatDoesNotExist";
    
    public IdentifierManagerTest(String name) {
        super(name);
    }
    /**
     * Initialize the connection to metacat, and insert a document to be 
     * used for testing with a known docid.
     */
    public void setUp() {
        
        metacatConnectionNeeded = true;
        try {
            DBConnectionPool pool = DBConnectionPool.getInstance();
        } catch (SQLException e2) {
            fail(e2.getMessage());
        }
        
        try {
            super.setUp();
        } catch (Exception e1) {
            fail(e1.getMessage());
        }
    }
    
    /**
     * test getting a guid from the systemmetadata table
     */
    public void testGetGUID()
    {
        ph("testGetGUID");
        try
        {
            IdentifierManager im = IdentifierManager.getInstance();

            String docid = insertTestDocument();
            docid = docid.substring(0, docid.lastIndexOf("."));
            
            String gotGuid = im.getGUID(docid, 1);
            assertNotNull(gotGuid);
        }
        catch(Exception e)
        {
            fail("Unexpected exception in testGetGUID: " + e.getMessage());
        }
    }
    
    /**
     * test getting a list of all local ids
     */
    public void testGetAllLocalIds()
    {
        ph("testGetAllLocalIds");
        try
        {
            List l = IdentifierManager.getInstance().getAllLocalIds();
            for(int i=0; i<l.size(); i++)
            {
                System.out.println(l.get(i));
            }
            assertTrue(l.size() > 0);
        }
        catch(Exception e)
        {
            fail("Error in testGetAllLocalIds: " + e.getMessage());
        }
    }

    /** Test that IM instances can be created. */
    public void testGetInstance() {
        ph("testGetInstance");
        IdentifierManager im = IdentifierManager.getInstance();
        assertNotNull(im);
    }

    /** Test that known LocalId's can be looked up from GUIDs. */
    public void testGetLocalId() {
        ph("testGetLocalId");
        IdentifierManager im = IdentifierManager.getInstance();
        String docidWithRev = insertTestDocument();
        String docid = docidWithRev.substring(0, docidWithRev.lastIndexOf("."));
        String guid;
        String idReturned;
        try {
            guid = im.getGUID(docid, 1);
            idReturned = im.getLocalId(guid);
            assertEquals(docidWithRev, idReturned);
            
        } catch (McdbDocNotFoundException e) {
            fail(e.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    /** Test that unknown LocalId's return the proper exception. */
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
    public void testIdentifierExists() {
        ph("testIdentifierExists");
        
        String goodGuid  = "";
        String accString = "";
        String docid     = "";
        int rev          = 0;
        
        try {
          IdentifierManager im = IdentifierManager.getInstance();
          accString = insertTestDocument();
          AccessionNumber accNumber = new AccessionNumber(accString, "NONE");
          docid = accNumber.getDocid();
          rev = new Integer(accNumber.getRev());
          goodGuid = im.getGUID(docid, rev);
          assertTrue(im.identifierExists(goodGuid));
          assertFalse(im.identifierExists(badGuid));
          
        } catch ( McdbDocNotFoundException dnfe ) {
          fail("The document " + docid + "couldn't be found. The error was: " +
               dnfe.getMessage());
          
        } catch ( AccessionNumberException ane ) {
          fail("The accession number could not be created for docid" +
               accString + ". The error was: " + ane.getMessage());
        
        } catch ( NumberFormatException nfe ) {
          fail("The revision number could not be created for docid" + 
               accString + ". The error was: " + nfe.getMessage());
          
           } catch ( SQLException sqle ) {
             fail("The accession number could not be created for docid" + 
                  accString + ". The error was: " + sqle.getMessage());

        }
    }
    
    /**
     * Test that we are able to create mappings from guids to localIds, and that
     * improperly formatted docids generate the proper exceptions.  This also tests
     * getLocalId() and getGUID()
     */
    public void testCreateMapping() {
       ph("testCreateMapping");
       try
       {
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
    public void testGenerateLocalId() {
      IdentifierManager im = IdentifierManager.getInstance();
      String localid = im.generateLocalId("mynewid." + new Date().getTime(), 1);
      System.out.println("localid: " + localid);
      assertTrue(localid != null);
    }
    
    /**
     * Test the method - getHeadPID for a speicified SID
     */
    public void testGetHeadPID() {
        
        try {
            //insert test documents with a series id
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue(generateDocumentId());
            InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            String sid1= "sid."+System.nanoTime();
            Identifier seriesId = new Identifier();
            seriesId.setValue(sid1);
            System.out.println("the first sid is "+seriesId.getValue());
            sysmeta.setSeriesId(seriesId);
            MNodeService.getInstance(request).create(session, guid, object, sysmeta);
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
            newPid.setValue(generateDocumentId()+"1");
            System.out.println("the second pid is "+newPid.getValue());
            SystemMetadata newSysMeta = createSystemMetadata(newPid, session.getSubject(), object);
            newSysMeta.setObsoletes(guid);
            newSysMeta.setSeriesId(seriesId);
            MNodeService.getInstance(request).update(session, guid, object, newPid, newSysMeta);
            System.out.println("case 1: =======");
            // the pid should be the newPid when we try to get the sid1
            head = IdentifierManager.getInstance().getHeadPID(seriesId);
            assertTrue(head.getValue().equals(newPid.getValue()));
            
            
            // the pid should be null when we try to get a no-exist sid
            Identifier non_exist_sid = new Identifier();
            non_exist_sid.setValue("no-sid-exist-123qwe");
            assertTrue(IdentifierManager.getInstance().getHeadPID(non_exist_sid) == null);
            
            //case-2
            // http://jenkins-1.dataone.org/documentation/unstable/API-Documentation-development/design/ContentMutability.html
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case2 = new Identifier();
            pid1_case2.setValue(generateDocumentId());
            String sid_case2_str= "sid."+System.nanoTime();
            Identifier sid_case2 = new Identifier();
            sid_case2.setValue(sid_case2_str);
            SystemMetadata sysmeta_case2 = createSystemMetadata(pid1_case2, session.getSubject(), object);
            sysmeta_case2.setSeriesId(sid_case2);
            CNodeService.getInstance(request).create(session, pid1_case2, object, sysmeta_case2);
            
            Thread.sleep(1000);
            Identifier pid2_case2 = new Identifier();
            pid2_case2.setValue(generateDocumentId());
            sysmeta = createSystemMetadata(pid2_case2, session.getSubject(), object);
            sysmeta.setSeriesId(sid_case2);
            try {
                CNodeService.getInstance(request).create(session, pid2_case2, object, sysmeta);
                fail("we shouldn't get here and an InvalidSystemMetacat exception should be thrown.");
            } catch (InvalidSystemMetadata e) {
                System.out.println("case 2======= Invalid system metadata");
            }
            
            
            //case-3
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case3 = new Identifier();
            pid1_case3.setValue(generateDocumentId());
            String sid_case3_str= "sid."+System.nanoTime();
            Identifier sid_case3 = new Identifier();
            sid_case3.setValue(sid_case3_str);
            SystemMetadata sysmeta_case3 = createSystemMetadata(pid1_case3, session.getSubject(), object);
            sysmeta_case3.setSeriesId(sid_case3);
            CNodeService.getInstance(request).create(session, pid1_case3, object, sysmeta_case3);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case3 = new Identifier();
            pid2_case3.setValue(generateDocumentId());
            SystemMetadata sysmeta2_case3 = createSystemMetadata(pid2_case3, session.getSubject(), object);
            sysmeta2_case3.setSeriesId(sid_case3);
            sysmeta2_case3.setObsoletes(pid1_case3);
            CNodeService.getInstance(request).create(session, pid2_case3, object, sysmeta2_case3);
            System.out.println("case 3: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case3);
            assertTrue(head.equals(pid2_case3));
            
            //case-4
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case4 = new Identifier();
            pid1_case4.setValue(generateDocumentId());
            Identifier sid_case4 = new Identifier();
            sid_case4.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case4 = createSystemMetadata(pid1_case4, session.getSubject(), object);
            sysmeta_case4.setSeriesId(sid_case4);
            CNodeService.getInstance(request).create(session, pid1_case4, object, sysmeta_case4);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case4 = new Identifier();
            pid2_case4.setValue(generateDocumentId());
            SystemMetadata sysmeta2_case4 = createSystemMetadata(pid2_case4, session.getSubject(), object);
            sysmeta2_case4.setSeriesId(sid_case4);
            sysmeta2_case4.setObsoletes(pid1_case4);
            CNodeService.getInstance(request).create(session, pid2_case4, object, sysmeta2_case4);
            
            sysmeta_case4.setObsoletedBy(pid2_case4);
            CNodeService.getInstance(request).updateSystemMetadata(session, pid1_case4, sysmeta_case4);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case4 = new Identifier();
            pid3_case4.setValue(generateDocumentId());
            Identifier sid2_case4 = new Identifier();
            sid2_case4.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta3_case4 = createSystemMetadata(pid3_case4, session.getSubject(), object);
            sysmeta3_case4.setSeriesId(sid2_case4);
            sysmeta3_case4.setObsoletes(pid2_case4);
            CNodeService.getInstance(request).create(session, pid3_case4, object, sysmeta3_case4);
            
            sysmeta2_case4.setObsoletedBy(pid3_case4);
            CNodeService.getInstance(request).updateSystemMetadata(session, pid2_case4, sysmeta2_case4);
            
            
            System.out.println("case 4: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case4);
            assertTrue(head.equals(pid2_case4));
            head = IdentifierManager.getInstance().getHeadPID(sid2_case4);
            assertTrue(head.equals(pid3_case4));
            
            
            //case-5
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case5 = new Identifier();
            pid1_case5.setValue(generateDocumentId());
            Identifier sid_case5 = new Identifier();
            sid_case5.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta1_case5 = createSystemMetadata(pid1_case5, session.getSubject(), object);
            sysmeta1_case5.setSeriesId(sid_case5);
            CNodeService.getInstance(request).create(session, pid1_case5, object, sysmeta1_case5);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case5 = new Identifier();
            pid2_case5.setValue(generateDocumentId());
            SystemMetadata sysmeta2_case5 = createSystemMetadata(pid2_case5, session.getSubject(), object);
            sysmeta2_case5.setSeriesId(sid_case5);
            sysmeta2_case5.setObsoletes(pid1_case5);
            CNodeService.getInstance(request).create(session, pid2_case5, object, sysmeta2_case5);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case5 = new Identifier();
            pid3_case5.setValue(generateDocumentId());
            Identifier sid2_case5 = new Identifier();
            sid2_case5.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta3_case5 = createSystemMetadata(pid3_case5, session.getSubject(), object);
            sysmeta3_case5.setSeriesId(sid2_case5);
            sysmeta3_case5.setObsoletes(pid2_case5);
            CNodeService.getInstance(request).create(session, pid3_case5, object, sysmeta3_case5);
            
            System.out.println("case 5: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case5);
            assertTrue(head.equals(pid2_case5));
            head = IdentifierManager.getInstance().getHeadPID(sid2_case5);
            assertTrue(head.equals(pid3_case5));
            
            
            //case-6
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case6 = new Identifier();
            pid1_case6.setValue(generateDocumentId());
            Identifier sid_case6 = new Identifier();
            sid_case6.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case6 = createSystemMetadata(pid1_case6, session.getSubject(), object);
            sysmeta_case6.setSeriesId(sid_case6);
            CNodeService.getInstance(request).create(session, pid1_case6, object, sysmeta_case6);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case6 = new Identifier();
            pid2_case6.setValue(generateDocumentId());
            SystemMetadata sysmeta2_case6 = createSystemMetadata(pid2_case6, session.getSubject(), object);
            sysmeta2_case6.setSeriesId(sid_case6);
            sysmeta2_case6.setObsoletes(pid1_case6);
            CNodeService.getInstance(request).create(session, pid2_case6, object, sysmeta2_case6);
            
            sysmeta_case6.setObsoletedBy(pid2_case6);
            CNodeService.getInstance(request).updateSystemMetadata(session, pid1_case6, sysmeta_case6);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case6 = new Identifier();
            pid3_case6.setValue(generateDocumentId());
            SystemMetadata sysmeta3_case6 = createSystemMetadata(pid3_case6, session.getSubject(), object);
            sysmeta3_case6.setObsoletes(pid2_case6);
            CNodeService.getInstance(request).create(session, pid3_case6, object, sysmeta3_case6);
            
            sysmeta2_case6.setObsoletedBy(pid3_case6);
            CNodeService.getInstance(request).updateSystemMetadata(session, pid2_case6, sysmeta2_case6);
            
            System.out.println("case 6: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case6);
            assertTrue(head.equals(pid2_case6));
            
            
            //case-7
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid1_case7 = new Identifier();
            pid1_case7.setValue(generateDocumentId());
            Identifier sid_case7 = new Identifier();
            sid_case7.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta_case7 = createSystemMetadata(pid1_case7, session.getSubject(), object);
            sysmeta_case7.setSeriesId(sid_case7);
            CNodeService.getInstance(request).create(session, pid1_case7, object, sysmeta_case7);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid2_case7 = new Identifier();
            pid2_case7.setValue(generateDocumentId());
            SystemMetadata sysmeta2_case7 = createSystemMetadata(pid2_case7, session.getSubject(), object);
            sysmeta2_case7.setSeriesId(sid_case7);
            sysmeta2_case7.setObsoletes(pid1_case7);
            CNodeService.getInstance(request).create(session, pid2_case7, object, sysmeta2_case7);
            
            sysmeta_case7.setObsoletedBy(pid2_case7);
            CNodeService.getInstance(request).updateSystemMetadata(session, pid1_case7, sysmeta_case7);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid3_case7 = new Identifier();
            pid3_case7.setValue(generateDocumentId());
            SystemMetadata sysmeta3_case7 = createSystemMetadata(pid3_case7, session.getSubject(), object);
            sysmeta3_case7.setObsoletes(pid2_case7);
            CNodeService.getInstance(request).create(session, pid3_case7, object, sysmeta3_case7);
            
            sysmeta2_case7.setObsoletedBy(pid3_case7);
            CNodeService.getInstance(request).updateSystemMetadata(session, pid2_case7, sysmeta2_case7);
            
            Thread.sleep(1000);
            object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            Identifier pid4_case7 = new Identifier();
            pid4_case7.setValue(generateDocumentId());
            Identifier sid2_case7 = new Identifier();
            sid2_case7.setValue("sid."+System.nanoTime());
            SystemMetadata sysmeta4_case7 = createSystemMetadata(pid4_case7, session.getSubject(), object);
            sysmeta4_case7.setObsoletes(pid3_case7);
            sysmeta4_case7.setSeriesId(sid2_case7);
            CNodeService.getInstance(request).create(session, pid4_case7, object, sysmeta4_case7);
            
            sysmeta3_case7.setObsoletedBy(pid4_case7);
            CNodeService.getInstance(request).updateSystemMetadata(session, pid3_case7, sysmeta3_case7);
            
            System.out.println("case 7: =======");
            head = IdentifierManager.getInstance().getHeadPID(sid_case7);
            assertTrue(head.equals(pid2_case7));
            head = IdentifierManager.getInstance().getHeadPID(sid2_case7);
            assertTrue(head.equals(pid4_case7));
         
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
    }

    /** 
     * Insert a test document, returning the docid that was used. 
     */
    private String insertTestDocument() {
        String accessBlock = getAccessBlock("public", true, true,
                false, false, false);
        String emldoc = getTestEmlDoc("Test identifier manager", EML2_1_0, null,
                null, "http://fake.example.com/somedata", null,
                accessBlock, null, null,
                null, null);
        System.out.println("inserting doc: " + emldoc);
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = insertDocumentId(docid, emldoc, true, false);
        } catch (MetacatAuthException e) {
            fail(e.getMessage());
        } catch (MetacatInaccessibleException e) {
            fail(e.getMessage());
        }
        return docid;
    }
    
    private void ph(String s)
    {
        System.out.println("*********************** " + s + " ****************************");
    }
    
    /**
     * We want to act as the CN itself
     * @throws ServiceFailure 
     * @throws Exception 
     */
    @Override
    public Session getTestSession() throws Exception {
        Session session = super.getTestSession();
        
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
    
}
