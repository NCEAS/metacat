package edu.ucsb.nceas.metacat.doi;

import org.junit.Test;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;

public class DOIServiceFactoryTest extends MCTestCase {
    
    /**
     * Test the method of getInstance
     * @throws Exception
     */
    @Test
    public void testGetDOIService() throws Exception {
        String className = PropertyService.getProperty("guid.doiservice.plugin.class");
        DOIService service = DOIServiceFactory.getDOIService();
        assertTrue(service.getClass().getCanonicalName().equals(className));
    }
}
