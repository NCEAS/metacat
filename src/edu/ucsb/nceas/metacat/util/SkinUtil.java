/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements administrative methods 
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
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

package edu.ucsb.nceas.metacat.util;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;

public class SkinUtil {
	
	private static Log logMetacat = LogFactory.getLog(SkinUtil.class);
	private static Vector<String> skinNames = null;

	/**
	 * private constructor - all methods are static so there is no no need to
	 * instantiate.
	 */
	private SkinUtil() {
	}
		
	/**
	 * Reports whether skins are fully configured.
	 * 
	 * @return a boolean that is true if skins are not unconfigured and false
	 *         otherwise
	 */
	public static boolean areSkinsConfigured() throws MetacatUtilException {
		try {
			return !PropertyService.getProperty("configutil.skinsConfigured").equals(
					PropertyService.UNCONFIGURED);
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not determine if database is configured: "
					+ pnfe.getMessage());
		}
	}

	/**
	 * Gets a list of available skin names by parsing a csv property from
	 * metacat.properties.
	 * 
	 * @return a Vector of Strings holding skin names
	 */
	public static Vector<String> getSkinNames() throws PropertyNotFoundException {
	    if(skinNames == null || skinNames.isEmpty()) {
	        String skinStringList = PropertyService.getProperty("skin.names");
	        skinNames = StringUtil.toVector(skinStringList, ',');
	    }
	    return skinNames;
	}
	
	/**
	 * Set the names of skin. We use this method for testing
	 * @param names  the name of skins will be set
	 */
	public static void setSkinName(Vector<String> names) {
	    skinNames = names;
	}

}
