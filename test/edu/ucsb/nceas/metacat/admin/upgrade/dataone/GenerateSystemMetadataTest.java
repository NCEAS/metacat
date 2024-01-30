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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;



/**
 * A JUnit test for testing system metadata generation
 */
public class GenerateSystemMetadataTest {


    /**
     * Test to create resource map
     */
    @Test
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
            Map<Identifier, Map<Identifier, List<Identifier>>> retPackageMap =
                                         ResourceMapFactory.getInstance().parseResourceMap(rdfXml);
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
            assertEquals(idMap.keySet().iterator().next().getValue(),
                                                retIdMap.keySet().iterator().next().getValue());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}

