/**
 *  '$RCSfile$'
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
 * 
 *   '$Author: tao $'
 *     '$Date: 2008-07-06 21:25:34 -0700 (Sun, 06 Jul 2008) $'
 * '$Revision: 4080 $'
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
