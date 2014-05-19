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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;

public class OrcidServiceTest extends MCTestCase {

	
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
	
	public void findMatches() {
		
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
