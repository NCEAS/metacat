/**
 *  '$RCSfile$'
 *  Copyright: 2007 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.Sitemap;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.utilities.FileUtil;

/**
 * Test the Sitemap class by generating the sitemaps in a separate directory.
 * 
 * @author Matt Jones
 */
public class SitemapTest extends MCTestCase {

    private String directoryName = "/tmp/sitemaps";

    /**
     * Initialize the Metacat environment so the test can run.
     */
    protected void setUp() throws Exception {
        super.setUp();

        DBConnectionPool pool = DBConnectionPool.getInstance();
        
		metacatConnectionNeeded = true;
		super.setUp();
    }

    /**
     * Test the static generateSitemaps() method.
     */
    public void testGenerateSitemaps() {	
    	try {
			debug("\nRunning: testGenerateSitemaps()");

			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);

			// insert 2.0.1 document
			String docid1 = generateDocumentId();
			debug("inserting docid: " + docid1 + ".1 which has no access section");
			testdocument = getTestEmlDoc("Doc with no access section", EML2_0_1, null,
					null, null, null, null, null, null, null, null);
			insertDocumentId(docid1 + ".1", testdocument, SUCCESS, false);
			readDocumentIdWhichEqualsDoc(docid1, testdocument, SUCCESS, false);

			String docid2 = generateDocumentId();
			debug("inserting docid: " + docid2 + ".1 which has public read/write section");
			Vector<String> accessRules1 = new Vector<String>();
			String accessRule1 = generateOneAccessRule("public", true, true, true, false, false);
			accessRules1.add(accessRule1);
			String accessBlock = getAccessBlock(accessRules1, ALLOWFIRST);
			testdocument = getTestEmlDoc(
					"Doc with public read and write", EML2_0_1,
					null, null, null, null, accessBlock, null, null, null, null);
			insertDocumentId(docid2 + ".1", testdocument, SUCCESS, false);
			
			String docid3 = generateDocumentId();
			debug("inserting docid: " + docid3 + ".1 which has which has " + username + " read/write section");
			Vector<String> accessRules2 = new Vector<String>();
			String accessRule2 = generateOneAccessRule(username, true, true, true, false, false);
			accessRules2.add(accessRule2);
			String accessBlock2 = getAccessBlock(accessRules2, ALLOWFIRST);
			testdocument = getTestEmlDoc(
					"Doc with public read and write", EML2_0_1,
					null, null, null, null, accessBlock2, null, null, null, null);
			insertDocumentId(docid3 + ".1", testdocument, SUCCESS, false);
			
			debug("logging out");
			m.logout();

			// create the directory if it does not exist
			FileUtil.createDirectory(directoryName);

			File directory = new File(directoryName);
			String urlRoot = "http://foo.example.com/ctx/metacat";
			String skin = "testskin";
			Sitemap smap = new Sitemap(directory, urlRoot, skin);
			smap.generateSitemaps();
			
			File sitemap1 = new File(directory, "metacat1.xml");
			assertTrue(sitemap1.exists() && sitemap1.isFile());
			
			String doc = FileUtil.readFileToString(directoryName + FileUtil.getFS() + "metacat1.xml");
			debug("**** sitemap doc *** \n");
			debug(doc);
			assertTrue(doc.indexOf("<?xml") >= 0);
			assertTrue(doc.indexOf("<urlset") >= 0);
			assertTrue(doc.indexOf("<url>") >= 0);
			assertTrue(doc.indexOf("http:") >= 0);
			
			// only docid 2 should show up in the sitemap.
			assertTrue(doc.indexOf(docid1) == -1);
			assertTrue(doc.indexOf(docid2) >= 0);
			assertTrue(doc.indexOf(docid3) == -1);
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}
    }
}
