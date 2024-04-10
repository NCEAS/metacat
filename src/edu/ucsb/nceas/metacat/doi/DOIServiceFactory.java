package edu.ucsb.nceas.metacat.doi;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A factory class to initialize an instance of DOIService 
 * based on the configuration in the metacat.properties file
 * @author tao
 *
 */
public class DOIServiceFactory {
    private static DOIService doiService = null;
    
    /**
     * Get a singleton instance of DOIService
     * @return  the instance of DOIService
     * @throws PropertyNotFoundException 
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public static DOIService getDOIService() throws PropertyNotFoundException, InstantiationException, 
                                                    IllegalAccessException, ClassNotFoundException {
        if (doiService == null) {
            synchronized(DOIServiceFactory.class) {
                if (doiService == null) {
                    String className = PropertyService.getProperty("guid.doiservice.plugin.class");
                    Object object = Class.forName(className).newInstance();
                    doiService = (DOIService) object;
                }
            }
        }
        return doiService;
    }

}
