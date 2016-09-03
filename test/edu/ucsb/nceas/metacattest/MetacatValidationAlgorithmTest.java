/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: tao $'
 *     '$Date: 2016-09-02 17:58:12 -0900  $'
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

import java.io.File;
import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.xml.sax.*;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.AccessionNumber;
import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;

import org.apache.commons.io.FileUtils;


public class MetacatValidationAlgorithmTest extends MCTestCase {
    
    public MetacatValidationAlgorithmTest (String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        // Test basic functions
        suite.addTest(new MetacatValidationAlgorithmTest("testRegisteredDTDDucoment"));
        suite.addTest(new MetacatValidationAlgorithmTest("testRegisteredDTDInvalidDucoment"));
        suite.addTest(new MetacatValidationAlgorithmTest("testUnRegisteredDTDDucoment"));
        suite.addTest(new MetacatValidationAlgorithmTest("testValidationUnneededDucoment"));
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
     * Insert a test document which's dtd was registered, returning the docid that was used. 
     */
    public void testRegisteredDTDDucoment() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/jones.204.22.xml"), "UTF-8");
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = insertDocumentId(docid, xml, true, false);
            assertTrue(response.contains(docid));
        } catch (MetacatAuthException e) {
            fail(e.getMessage());
        } catch (MetacatInaccessibleException e) {
            fail(e.getMessage());
        }
    }
    
    /** 
     * Insert a test document which's dtd was registered. But the xml doesn't follow the dta and returning an error. 
     */
    public void testRegisteredDTDInvalidDucoment() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/jones.204.22.xml.invalid"), "UTF-8");
        String docid = generateDocumentId() + ".1";
        m.login(username, password);
        try {
            String response = m.insert(docid, new StringReader(xml), null);
            fail("we shouldn't get get since we inserted an invalid xml");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("<error>"));
        }
        
        
    }
    
    /** 
     * Insert a test document which's dtd was registered. But the xml doesn't follow the dta and returning an error. 
     */
    public void testUnRegisteredDTDDucoment() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/doc_with_unregister_dtd.xml"), "UTF-8");
        String docid = generateDocumentId() + ".1";
        m.login(username, password);
        try {
            String response = m.insert(docid, new StringReader(xml), null);
            fail("We can't get since the inserted xml has a unregistered dtd");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("<error>"));
        }
    }
    
    /** 
     * Insert test documents which don't need to be validated: with embeded dtd or no declaration at all. 
     */
    public void testValidationUnneededDucoment() throws Exception {
        //with embedded dtd
        String xml = FileUtils.readFileToString(new File("./test/doc_with_embedded_dtd.xml"), "UTF-8");
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = insertDocumentId(docid, xml, true, false);
            assertTrue(response.contains(docid));
        } catch (MetacatAuthException e) {
            fail(e.getMessage());
        } catch (MetacatInaccessibleException e) {
            fail(e.getMessage());
        }
        //without dtd and scheam declaration
        xml = FileUtils.readFileToString(new File("./test/doc_without_declaration.xml"), "UTF-8");
        docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = insertDocumentId(docid, xml, true, false);
            assertTrue(response.contains(docid));
        } catch (MetacatAuthException e) {
            fail(e.getMessage());
        } catch (MetacatInaccessibleException e) {
            fail(e.getMessage());
        }
    }
    

}
