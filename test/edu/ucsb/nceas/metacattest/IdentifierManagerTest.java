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

import java.sql.SQLException;
import java.util.*;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.AccessionNumber;
import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;

public class IdentifierManagerTest extends MCTestCase {
    private String badGuid = "test:testIdThatDoesNotExist";
    
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
}
