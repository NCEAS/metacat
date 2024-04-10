package edu.ucsb.nceas.metacat.util;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A utility class for configuring DataONE setting
 * @author tao
 *
 */
public class DataONEConfigUtil {
    /**
     * Reports whether dataONE is configured.
     * 
     * @return a boolean that is true if dataONE setting is configured or bypassed
     */
    public static boolean isDataOneConfigured() throws MetacatUtilException {
        String dataoneConfiguredString = PropertyService.UNCONFIGURED;
        try {
            dataoneConfiguredString = PropertyService.getProperty("configutil.dataoneConfigured");
        } catch (PropertyNotFoundException pnfe) {
            throw new MetacatUtilException("Could not determine if DataONE are configured: "
                    + pnfe.getMessage());
        }
        // geoserver is configured if not unconfigured
        return !dataoneConfiguredString.equals(PropertyService.UNCONFIGURED);
    }
}
