/**  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.annotation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

public class OrcidServiceTest extends D1NodeServiceTest {

	
	/**
	 * constructor for the test
	 */
	public OrcidServiceTest(String name) {
		super(name);
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() throws Exception {
		super.setUp();
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
		suite.addTest(new OrcidServiceTest("testLookup"));
		suite.addTest(new OrcidServiceTest("findMatches"));

		return suite;
	}
	
	public void testLookup() {
		List<String> otherNames = Arrays.asList("Matthew Bentley Jones");
		String orcid = OrcidService.lookupOrcid(null, null, null, otherNames);
		assertEquals("http://orcid.org/0000-0003-0077-4738", orcid);
	}
	
	public void findMatches() throws Exception {
        
        // insert an object in case there are no objects on the server
        String path = "test/eml-datacite.xml";
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("findMatches." + System.currentTimeMillis());
        InputStream object = new FileInputStream(new File(path));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta.setFormatId(formatId);
        object = new FileInputStream(new File(path));
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        object.close();
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int times = 0;
        int tryAcccounts = 20;
        while ( (resultStr == null || !resultStr.contains("checksum")) && times <= tryAcccounts) {
            Thread.sleep(1000);
            times++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        
        
		int count = 0;
		Map<String, String> matches = new HashMap<String, String>();

		List<String> creators = OrcidService.lookupCreators(true);
		for (String creator: creators) {
			String orcid = OrcidService.lookupOrcid(null, null, null, Arrays.asList(creator));
			if (orcid != null) {
				matches.put(orcid, creator);
				count++;
			}
 		}
		assertTrue(count > 0);
		for (Entry<String, String> entry : matches.entrySet()) {
			System.out.println("Found ORCID: " + entry.getKey() + " for creator: " + entry.getValue());
		}
	}

}
