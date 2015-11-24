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
import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;



import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.AccessionNumber;
import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;

import org.apache.commons.io.FileUtils;

public class ISO119139Test  extends MCTestCase {
   
    public ISO119139Test(String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        // Test basic functions
        suite.addTest(new ISO119139Test("TestInsertDocument"));
        return suite;
    }
    /**
     * Initialize the connection to metacat, and insert a document to be 
     * used for testing with a known docid.
     */
    public void setUp() throws Exception{
        metacatConnectionNeeded = true;
        super.setUp();
        

    }
    
  

    /** 
     * Insert a test document, returning the docid that was used. 
     */
    public void TestInsertDocument() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/isoTestNodc1.xml"), "UTF-8");
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = insertDocumentId(docid, xml, true, false);
        } catch (MetacatAuthException e) {
            fail(e.getMessage());
        } catch (MetacatInaccessibleException e) {
            fail(e.getMessage());
        }
    }
    
   
    
}
