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

