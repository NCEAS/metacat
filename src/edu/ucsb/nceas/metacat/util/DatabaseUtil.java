/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements database administrative methods 
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

import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.database.DBVersion;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public class DatabaseUtil {
	
	private static Logger logMetacat = Logger.getLogger(DatabaseUtil.class);

	/**
	 * private constructor - all methods are static so there is no no need to
	 * instantiate.
	 */
	private DatabaseUtil() {}
	
	/**
	 * Reports whether database is fully configured.
	 * 
	 * @return a boolean that is true if database is not unconfigured and false
	 *         otherwise
	 */
	public static boolean isDatabaseConfigured() throws MetacatUtilException {
		String databaseConfiguredString = PropertyService.UNCONFIGURED;
		try {
			databaseConfiguredString = 
				PropertyService.getProperty("configutil.databaseConfigured");
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not determine if database is configured: "
					+ pnfe.getMessage());
		}
		return !databaseConfiguredString.equals(PropertyService.UNCONFIGURED);
	}	
	
	/**
	 * Gets the available upgrade versions for metacat by getting a list of 
	 * upgrade script from properties in metacat.properties that start with 
	 * "database.upgradeVersion". It then creats a DBVersion object for each 
	 * upgrade version.
	 * 
	 * @return a TreeSet of DBVersion objects holding individual version information
	 */
	public static TreeSet<DBVersion> getUpgradeVersions() throws PropertyNotFoundException {
		
		TreeSet<DBVersion> versionSet = new TreeSet<DBVersion>();
		
		Vector<String> upgradeVersionKeys = PropertyService.getPropertyNamesByGroup("database.upgradeVersion");
		for (String upgradeVersionKey : upgradeVersionKeys) {
			String upgradeVersionId = upgradeVersionKey.substring(24);
			logMetacat.debug("Creating DBVersion object for version: " + upgradeVersionId);
			DBVersion dbVersion = new DBVersion(upgradeVersionId);
			versionSet.add(dbVersion);
		}

		return versionSet;
	}
	
	/**
	 * Gets the available upgrade versions for metacat by getting a list of 
	 * upgrade script from properties in metacat.properties that start with 
	 * "database.upgradeVersion". It then creats a DBVersion object for each 
	 * upgrade version.
	 * 
	 * @return a TreeSet of DBVersion objects holding individual version information
	 */
	public static Map<String, String> getScriptSuffixes() throws PropertyNotFoundException {
		
		return PropertyService.getPropertiesByGroup("database.scriptsuffix");
	}
}
