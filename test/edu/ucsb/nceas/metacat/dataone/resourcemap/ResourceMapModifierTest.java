/**
 *  '$RCSfile$'
 *  Copyright: 2019 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.dataone.resourcemap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

import org.dataone.service.types.v1.Identifier;

import edu.ucsb.nceas.MCTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Junit test class for the class of ResourceMapModifier
 * @author tao
 *
 */
public class ResourceMapModifierTest extends MCTestCase {
    
    private static String RESOURCEMAP_FILEPATH = "test/resourcemap-with-prov.xml";
    private static String ORIGINAL_RESOURCEMAP_PID = "urn:uuid:e62c781c-643b-41f3-a0b0-9f6cbd80a708";
    private static String NEW_RESOURCEMAP_PID = "urn:uuid:e62c781c-643b-41f3-a0b0-9f6cbd80a719";
    private static String ORIGNAL_METADATA_PID = "urn:uuid:c0e0d342-7cc1-4eaa-9648-c6d9f7ed8b1f";
    private static String NEW_METADATA_PID = "doi:10.5072/FK27D2ZR71";
    
    /**
     * Constructor
     * @param name
     */
    public ResourceMapModifierTest (String name) {
        super(name);
    }
    
    /**
     * Test suite
     * @return
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new ResourceMapModifierTest("testReplaceObsoletedIds"));
        return suite;
    }
    
    /**
     * Test the method of replaceObsoletedIds
     * @throws Exception
     */
    public void testReplaceObsoletedIds() throws Exception {
        File resourceMapFile = new File(RESOURCEMAP_FILEPATH);
        assertTrue(resourceMapFile.exists());
        FileInputStream resourceMapInputStream = new FileInputStream(resourceMapFile);
        Identifier origin_resourceMap_id = new Identifier();
        origin_resourceMap_id.setValue(ORIGINAL_RESOURCEMAP_PID);
        Identifier new_resourceMap_id = new Identifier();
        new_resourceMap_id.setValue(NEW_RESOURCEMAP_PID);
        ResourceMapModifier modifier = new ResourceMapModifier(origin_resourceMap_id, new_resourceMap_id);
        HashMap<Identifier, Identifier> obsoletedBys = new HashMap<Identifier, Identifier>();
        Identifier origin_metadata_id = new Identifier();
        origin_metadata_id.setValue(ORIGNAL_METADATA_PID);
        Identifier new_metadata_id = new Identifier();
        new_metadata_id.setValue(NEW_METADATA_PID);
        obsoletedBys.put(origin_metadata_id, new_metadata_id);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        modifier.replaceObsoletedIds(obsoletedBys, resourceMapInputStream, out);
        String outStr = out.toString("UTF-8");
        System.out.println(outStr);
        resourceMapInputStream.close();
    }
}
