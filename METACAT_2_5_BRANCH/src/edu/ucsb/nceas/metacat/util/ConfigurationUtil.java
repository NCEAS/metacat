/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements utility methods for a metadata catalog
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones, Jivka Bojilova
 *
 *   '$Author: daigle $'
 *     '$Date: 2009-08-04 14:32:58 -0700 (Tue, 04 Aug 2009) $'
 * '$Revision: 5015 $'
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

import org.apache.log4j.Logger;

import edu.ucsb.nceas.dbadapter.AbstractDatabase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.FileUtil;

/**
 * A suite of utility classes for the metadata catalog server
 */
public class ConfigurationUtil
{

    public static AbstractDatabase dbAdapter;
    
    private static Logger logMetacat = Logger.getLogger(ConfigurationUtil.class);

	/**
	 * Reports whether metacat is fully configured.
	 * 
	 * @return a boolean that is true if all sections are configured and 
	 * false otherwise
	 */
	public static boolean isMetacatConfigured() {
		boolean metacatConfigured = false;
		try {			
			metacatConfigured = PropertyService.arePropertiesConfigured()
					&& AuthUtil.isAuthConfigured()
					&& SkinUtil.areSkinsConfigured()
					&& DatabaseUtil.isDatabaseConfigured()
					&& GeoserverUtil.isGeoserverConfigured()
					&& isBackupDirConfigured()
					&& DataONEConfigUtil.isDataOneConfigured();
		} catch (MetacatUtilException ue) {
			logMetacat.error("Could not determine if metacat is configured due to utility exception: "
					+ ue.getMessage());
		} catch (GeneralPropertyException gpe) {
			logMetacat.error("Could not determine if metacat is configured due to property exception: "
					+ gpe.getMessage());
		}

		return metacatConfigured;
	}

	/**
	 * Check if the application.backupDir property is populated in
	 * metacat.properties and that it points to a writable directory.
	 * 
	 * @return false if the application.backupDir property does not point to a
	 *         writable directory.
	 */
	public static boolean isBackupDirConfigured() throws MetacatUtilException, PropertyNotFoundException {
		String backupDir = PropertyService.getProperty("application.backupDir");
		if (backupDir == null || backupDir.equals("")) {
			return false;
		}		
		if (FileUtil.getFileStatus(backupDir) < FileUtil.EXISTS_READ_WRITABLE) {
			return false;
		}	
		return true;
	}
		
	/**
	 * Reports whether the metacat configuration utility should be run. Returns
	 * false if 
	 *   -- dev.runConfiguration=false and 
	 *   -- backup properties file exists 
	 * Note that dev.runConfiguration should only be set to false when reinstalling the 
	 * same version of the application in development.
	 * 
	 * @return a boolean that is false if dev.runConfiguration is false and the
	 *         backup properties file exists.
	 */
	public static boolean bypassConfiguration() throws MetacatUtilException, ServiceException {
		try {
			// If the system is not configured to do bypass, return false.
			if (!PropertyService.doBypass()) {
				return false;
			}

			// Get the most likely backup files.  If these cannot be found, we 
			// cannot do the configuration bypass.
			String ExternalBaseDir = SystemUtil.discoverExternalDir();
			if (ExternalBaseDir == null) {
				logMetacat.error("bypassConfiguration: Could not find backup directory.");
				// could not find backup files ... force the configuration
				return false;
			}
			String realContext = ServiceService.getRealApplicationContext();
			PropertyService.setRecommendedExternalDir(ExternalBaseDir);
			PropertyService.setProperty("application.backupDir", 
					ExternalBaseDir + FileUtil.getFS() + "." + realContext);

			// Refresh the property service and skin property service.  This will pick up 
			// the backup directory and populate backup properties in caches.
			ServiceService.refreshService("PropertyService");
			ServiceService.refreshService("SkinPropertyService");

			// Call bypassConfiguration to make sure backup properties get persisted 
			// to active properties for both main and skin properties.
			PropertyService.bypassConfiguration();
			SkinPropertyService.bypassConfiguration();

			return true;
		} catch (GeneralPropertyException gpe) {
			throw new MetacatUtilException("Property error while discovering backup directory: "
					+ gpe.getMessage());
		} catch (MetacatUtilException mue) {
			throw new MetacatUtilException("Utility error while discovering backup directory: "
					+ mue.getMessage());
		}

	}
		
}
