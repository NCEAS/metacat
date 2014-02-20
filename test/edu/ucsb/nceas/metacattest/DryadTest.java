/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;

/**
 * A JUnit test for testing Metacat when Non Ascii Characters are inserted
 */
public class DryadTest
    extends MCTestCase {
    
    private static final String DRYAD_TEST_DOC = "test/dryad-metadata-profile-sample.xml";

    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public DryadTest(String name) {
        super(name);
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() {
        try {
            System.err.println("Test Metacat: " + metacatUrl);
            m = MetacatFactory.createMetacatConnection(metacatUrl);
        }
        catch (MetacatInaccessibleException mie) {
            System.err.println("Metacat is: " + metacatUrl);
            fail("Metacat connection failed." + mie.getMessage());
        }
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new DryadTest("initialize"));
        // Test basic functions
        suite.addTest(new DryadTest("insertDoc"));
        return suite;
    }

    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize() {
        assertTrue(1 == 1);
    }
    
    /**
     * Test insert of Dryad document
     * @throws InsufficientKarmaException
     * @throws MetacatException
     * @throws IOException
     * @throws MetacatInaccessibleException
     */
    public void insertDoc() {
		try {
			m.login(username, password);
	    	String docid = this.generateDocumentId();
	    	docid += ".1";
			String documentContents = this.getTestDocFromFile(DRYAD_TEST_DOC);
			m.insert(docid, new StringReader(documentContents), null);
			InputStream results = m.read(docid);
			String resultString = IOUtils.toString(results);
			assertEquals(documentContents, resultString);
			m.logout();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
    	
    }

    
}
