/**
 *  Copyright: 2021 Regents of the University of California and the
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
