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

package edu.ucsb.nceas.metacat.admin.upgrade.dataone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.types.v1.Identifier;
import org.dspace.foresite.ResourceMap;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.admin.upgrade.dataone.GenerateSystemMetadata;
import edu.ucsb.nceas.metacat.dataone.SystemMetadataFactory;

/**
 * A JUnit test for testing system metadata generation
 */
public class GenerateSystemMetadataTest
    extends MCTestCase {
    
	/**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public GenerateSystemMetadataTest(String name) {
        super(name);
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() {
        
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
        suite.addTest(new GenerateSystemMetadataTest("initialize"));
        // Test basic functions
        //suite.addTest(new GenerateSystemMetadataTest("upgrade"));
        // test ORE generation
        suite.addTest(new GenerateSystemMetadataTest("testCreateResourceMap"));

        // requires manual configuration to actually pass
        //suite.addTest(new GenerateSystemMetadataTest("testOreExistsFor"));

        return suite;
    }

    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize() {
        assertTrue(1 == 1);
    }
    
    public void upgrade() throws Exception {
    	GenerateSystemMetadata upgrader = new GenerateSystemMetadata();
        upgrader.upgrade();
    }
    
	public void testCreateResourceMap() {
		
		try {
			Identifier resourceMapId = new Identifier();
			resourceMapId.setValue("doi://1234/AA/map.1.1");
			Identifier metadataId = new Identifier();
			metadataId.setValue("doi://1234/AA/meta.1.1");
			List<Identifier> dataIds = new ArrayList<Identifier>();
			Identifier dataId = new Identifier();
			dataId.setValue("doi://1234/AA/data.1.1");
			Identifier dataId2 = new Identifier();
			dataId2.setValue("doi://1234/AA/data.2.1");
			dataIds.add(dataId);
			dataIds.add(dataId2);
			Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
			idMap.put(metadataId, dataIds);
			ResourceMapFactory rmf = ResourceMapFactory.getInstance();
			ResourceMap resourceMap = rmf.createResourceMap(resourceMapId, idMap);
			assertNotNull(resourceMap);
			String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
			assertNotNull(rdfXml);
			System.out.println(rdfXml);
			
			// now put it back in an object
			Map<Identifier, Map<Identifier, List<Identifier>>> retPackageMap = ResourceMapFactory.getInstance().parseResourceMap(rdfXml);
            Identifier retPackageId = retPackageMap.keySet().iterator().next();   
            
            // Package Identifiers should match
            assertEquals(resourceMapId.getValue(), retPackageId.getValue());
            System.out.println("PACKAGEID IS: " + retPackageId.getValue());

            // Get the Map of metadata/data identifiers
            Map<Identifier, List<Identifier>> retIdMap = retPackageMap.get(retPackageId);
            			
			// same size
			assertEquals(idMap.keySet().size(), retIdMap.keySet().size());
			for (Identifier key : idMap.keySet()) {
			    System.out.println("  ORIGINAL: " + key.getValue());
			    List<Identifier> contained = idMap.get(key);
			    for (Identifier cKey : contained) {
		             System.out.println("    CONTAINS: " + cKey.getValue());
			    }
			}
            for (Identifier key : retIdMap.keySet()) {
                System.out.println("  RETURNED: " + key.getValue());
                List<Identifier> contained = idMap.get(key);
                for (Identifier cKey : contained) {
                     System.out.println("    CONTAINS: " + cKey.getValue());
                }
            }

			// same value
			assertEquals(idMap.keySet().iterator().next().getValue(), retIdMap.keySet().iterator().next().getValue());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	/**
	 * Requires ORE to be present and parsed in the solr index to pass
	 */
	public void testOreExistsFor() {
		String pid = "tao.1.1";
		Identifier guid = new Identifier();
		guid.setValue(pid);
		boolean exists = SystemMetadataFactory.oreExistsFor(guid);
		assertTrue(exists);
		guid.setValue("BAD");
		exists = SystemMetadataFactory.oreExistsFor(guid);
		assertFalse(exists);
	}
    
}

