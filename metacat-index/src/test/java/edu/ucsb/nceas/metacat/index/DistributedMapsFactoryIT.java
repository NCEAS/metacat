package edu.ucsb.nceas.metacat.index;

import static org.junit.Assert.assertTrue;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Test;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;

public class DistributedMapsFactoryIT {
    /**
     * Test to get the system metadata map
     */
    @Test
    public void testGetSystemMetadataMap() throws Exception {
        IMap<Identifier, SystemMetadata> map = DistributedMapsFactory.getSystemMetadataMap();
        System.out.println("the size of the map is "+map.size());
        assertTrue("The size of the system metadata map should equal or be greater than 0", map.size() >= 0);
    }
    
    /**
     * Test to get the system metadata map
     */
    @Test
    public void testGetObjectPathMap() throws Exception {
        IMap<Identifier, String> map = DistributedMapsFactory.getObjectPathMap();
        System.out.println("the size of the map is "+map.size());
        assertTrue("The size of the object path map should equal or be greater than 0", map.size() >= 0);
    }
    
    /**
     * Test to get the identifier set
     */
    @Test
    public void getGetIdentifierSet() throws Exception {
        ISet identifiers = DistributedMapsFactory.getIdentifiersSet();
        System.out.println("the size of the map is "+identifiers.size());
        assertTrue("The size of the identifiers should equal or be greater than 0", identifiers.size() >= 0);
    }
}
