/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements properties methods for metacat
 *             skins
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

package edu.ucsb.nceas.metacat.properties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SkinUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.MetaDataProperty;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * A suite of utility classes for the skin configuration utility
 */
public class SkinPropertyService extends BaseService {

	private static SkinPropertyService skinService = null;

	private static boolean bypassAlreadyChecked = false;

	private static String BACKUP_DIR = null;

	private static Vector<String> skinNames = null;

	private static HashMap<String, SortedProperties> skinPropertiesMap = null;
	private static HashMap<String, SortedProperties> skinBackupPropertiesMap = null;
	private static HashMap<String, PropertiesMetaData> skinMetaDataMap = null;

	private static Logger logMetacat = Logger.getLogger(SkinPropertyService.class);

	/**
	 * private constructor since this is a singleton
	 * 
	 * @param servletContext
	 *            the context we will use to get relative paths
	 */
	private SkinPropertyService() throws ServiceException {
		try {
			_serviceName = "SkinPropertyService";
			
			initialize();
		} catch (GeneralPropertyException gpe) {
			throw new ServiceException(
					"Properties problem while initializing SkinPropertyService: "
							+ gpe.getMessage());
		} catch (IOException ioe) {
			throw new ServiceException("I/O Problem while initializing SkinPropertyService: "
					+ ioe.getMessage());
		}
	}

	/**
	 * Get the single instance of SkinPropertyService.
	 * 
	 * @param servletContext
	 *            the context we will use to get relative paths
	 * @return the single instance of SkinPropertyService
	 */
	public static SkinPropertyService getInstance() throws ServiceException {
		if (skinService == null) {
			skinService = new SkinPropertyService();
		}
		return skinService;
	}

	public boolean refreshable() {
		return true;
	}

	public void doRefresh() throws ServiceException {
		try {
			initialize();
		} catch (IOException ioe) {
			throw new ServiceException("Could not refresh SkinPropertyService due to"
					+ " I/O error: " + ioe.getMessage());
		} catch (GeneralPropertyException gpe) {
			throw new ServiceException("Could not refresh SkinPropertyService due to"
					+ " property error: " + gpe.getMessage());
		}
	}
	
	public void stop() throws ServiceException {
		return;
	}

	/**
	 * Initialize the singleton.
	 * 
	 * @param servletContext
	 *            the context we will use to get relative paths
	 */
	private void initialize() throws IOException, GeneralPropertyException,
			ServiceException {

		logMetacat.debug("Initializing SkinService");

		BACKUP_DIR = PropertyService.getProperty("application.backupDir");

		skinNames = SkinUtil.getSkinNames();

		skinPropertiesMap = new HashMap<String, SortedProperties>();
		skinBackupPropertiesMap = new HashMap<String, SortedProperties>();
		skinMetaDataMap = new HashMap<String, PropertiesMetaData>();

		try {
			for (String skinName : skinNames) {
				String propertyFilePath = ServiceService.getRealSkinDir()
						+ FileUtil.getFS() + skinName + FileUtil.getFS() + skinName
						+ ".properties";

				if (FileUtil.getFileStatus(propertyFilePath) < FileUtil.EXISTS_READ_WRITABLE) {
					logMetacat.error("Skin property file: " + propertyFilePath
							+ " does not exist read/writable. This skin will not be available.");
					continue;
				}

				SortedProperties skinProperties = new SortedProperties(propertyFilePath);
				skinProperties.load();
				skinPropertiesMap.put(skinName, skinProperties);

				String metaDataFilePath = ServiceService.getRealSkinDir()
						+ FileUtil.getFS() + skinName + FileUtil.getFS() + skinName
						+ ".properties.metadata.xml";
				if (FileUtil.getFileStatus(metaDataFilePath) > FileUtil.DOES_NOT_EXIST) {
					PropertiesMetaData skinMetaData = new PropertiesMetaData(metaDataFilePath);
					skinMetaDataMap.put(skinName, skinMetaData);
				} else {
					skinPropertiesMap.remove(skinName);
					logMetacat.error("Could not find skin property metadata file for skin: " 
							+ skinName + " at: " + metaDataFilePath  
							+ ". This skin will not be available.");
					continue;
				}

				String backupPropertyFilePath = BACKUP_DIR + FileUtil.getFS() + skinName
						+ ".properties.backup";
				if (FileUtil.getFileStatus(backupPropertyFilePath) > FileUtil.DOES_NOT_EXIST) {
					SortedProperties skinBackupProperties = new SortedProperties(
							backupPropertyFilePath);
					skinBackupProperties.load();
					skinBackupPropertiesMap.put(skinName, skinBackupProperties);
				} else {
					logMetacat.warn("Could not find backup properties for skin: "
							+ skinName + " at: " + backupPropertyFilePath);
				}
			}
		} catch (TransformerException te) {
			throw new GeneralPropertyException(te.getMessage());
		}
	}

	/**
	 * Utility method to get a property value from the properties file for a
	 * specific skin.
	 * 
	 * @param skinName
	 *            the skin for which we want to retrieve the property
	 * @param propertyName
	 *            the name of the property requested
	 * @return the String value for the property
	 */
	public static String getProperty(String skinName, String propertyName)
			throws PropertyNotFoundException {
		SortedProperties skinProperties = skinPropertiesMap.get(skinName);
		if (skinProperties == null) {
			throw new PropertyNotFoundException("There is not property map for "
					+ skinName);
		}
		return skinProperties.getProperty(propertyName);
	}

	/**
	 * Get a set of all property names for a given skin.
	 * 
	 * @param skinName
	 *            the skin for which we want to retrieve the property names
	 * @return Set of property names
	 */
	public static Vector<String> getPropertyNames(String skinName)
			throws PropertyNotFoundException {
		SortedProperties skinProperties = skinPropertiesMap.get(skinName);
		if (skinProperties == null) {
			throw new PropertyNotFoundException("There is not property map for "
					+ skinName);
		}
		return skinProperties.getPropertyNames();
	}

	/**
	 * Get a Set of all property names that start with the groupName prefix.
	 * 
	 * @param groupName
	 *            the prefix of the keys to search for.
	 * @return Vector of property names
	 */
	public static Vector<String> getPropertyNamesByGroup(String skinName, String groupName)
			throws PropertyNotFoundException {
		SortedProperties skinProperties = skinPropertiesMap.get(skinName);
		if (skinProperties == null) {
			throw new PropertyNotFoundException("There is not property map for "
					+ skinName);
		}
		return skinProperties.getPropertyNamesByGroup(groupName);
	}

	/**
	 * Get the main backup properties file. These are configurable properties
	 * that are stored outside the metacat install directories so the user does
	 * not need to re-enter all the configuration information every time they do
	 * an upgrade.
	 * 
	 * @return a SortedProperties object with the backup properties
	 */
	public static HashMap<String, SortedProperties> getProperties() {
		return skinPropertiesMap;
	}

	/**
	 * Get the main backup properties file. These are configurable properties
	 * that are stored outside the metacat install directories so the user does
	 * not need to re-enter all the configuration information every time they do
	 * an upgrade.
	 * 
	 * @return a SortedProperties object with the backup properties
	 */
	public static SortedProperties getProperties(String skinName) {
		return skinPropertiesMap.get(skinName);
	}

	/**
	 * Get the main backup properties file. These are configurable properties
	 * that are stored outside the metacat install directories so the user does
	 * not need to re-enter all the configuration information every time they do
	 * an upgrade.
	 * 
	 * @return a SortedProperties object with the backup properties
	 */
	public static HashMap<String, SortedProperties> getBackupProperties() {
		return skinBackupPropertiesMap;
	}

	/**
	 * Get the main backup properties file. These are configurable properties
	 * that are stored outside the metacat install directories so the user does
	 * not need to re-enter all the configuration information every time they do
	 * an upgrade.
	 * 
	 * @return a SortedProperties object with the backup properties
	 */
	public static SortedProperties getBackupProperties(String skinName) {
		return skinBackupPropertiesMap.get(skinName);
	}

	/**
	 * Get the main properties metadata. This is retrieved from an xml file that
	 * describes the attributes of configurable properties.
	 * 
	 * @return a PropertiesMetaData object with the main properties metadata
	 */
	public static HashMap<String, PropertiesMetaData> getMetaData() {
		return skinMetaDataMap;
	}

	/**
	 * Get the main properties metadata. This is retrieved from an xml file that
	 * describes the attributes of configurable properties.
	 * 
	 * @return a PropertiesMetaData object with the main properties metadata
	 */
	public static PropertiesMetaData getMetaData(String skinName) {
		return skinMetaDataMap.get(skinName);
	}

	/**
	 * Utility method to set a property value both in memory and to the
	 * properties file
	 * 
	 * @param propertyName
	 *            the name of the property requested
	 * @param newValue
	 *            the new value for the property
	 */
	public static void setProperty(String skinName, String propertyName, String newValue)
			throws IOException, GeneralPropertyException {
		SortedProperties skinProperties = skinPropertiesMap.get(skinName);
		if (skinProperties == null) {
			throw new GeneralPropertyException("There is not property map for "
					+ skinName);
		}
		skinProperties.setProperty(propertyName, newValue);
		skinProperties.store();

	}

	/**
	 * Utility method to set a property value in memory. This will NOT cause the
	 * property to be written to disk. Use this method to set multiple
	 * properties in a row without causing excessive I/O. You must call
	 * persistProperties() once you're done setting properties to have them
	 * written to disk.
	 * 
	 * @param propertyName
	 *            the name of the property requested
	 * @param newValue
	 *            the new value for the property
	 */
	public static void setPropertyNoPersist(String skinName, String propertyName,
			String newValue) throws GeneralPropertyException {
		SortedProperties skinProperties = skinPropertiesMap.get(skinName);
		if (skinProperties == null) {
			throw new GeneralPropertyException("There is not property map for "
					+ skinName);
		}
		skinProperties.setPropertyNoPersist(propertyName, newValue);
	}

	/**
	 * Save the properties to a properties file. Note, the order and comments
	 * will be preserved.
	 */
	public static void persistProperties(String skinName) throws IOException,
			GeneralPropertyException {
		SortedProperties skinProperties = skinPropertiesMap.get(skinName);
		if (skinProperties == null) {
			throw new GeneralPropertyException("There is not property map for "
					+ skinName);
		}
		skinProperties.store();
	}

	/**
	 * Save the properties to a properties file. Note, the order and comments
	 * will be preserved.
	 */
	public static void persistAllProperties() throws IOException,
			GeneralPropertyException {
		for (String skinName : skinNames) {
			persistProperties(skinName);
		}
	}

	/**
	 * Writes out backup configurable properties to a file.
	 */
	public static void persistBackupProperties(String skinName)
			throws GeneralPropertyException {
		try {
			String metaDataFilePath = ServiceService.getRealSkinDir() + FileUtil.getFS()
					+ skinName + FileUtil.getFS() + skinName + ".properties.metadata.xml";

			String backupPropertyFilePath = BACKUP_DIR + FileUtil.getFS() + skinName
					+ ".properties.backup";

			// Use the metadata to extract configurable properties from the
			// overall properties list, and store those properties.
			SortedProperties backupProperties = new SortedProperties(
					backupPropertyFilePath);

			// Populate the backup properties for main metacat properties using
			// the associated metadata file
			PropertiesMetaData skinMetadata = new PropertiesMetaData(metaDataFilePath);
			
			Map<String, MetaDataProperty> skinKeyMap = skinMetadata.getProperties();
			Set<String> skinKeySet = skinKeyMap.keySet();
			for (String propertyKey : skinKeySet) {
				// don't backup passwords
				MetaDataProperty metaData = skinKeyMap.get(propertyKey);
				if (!metaData.getFieldType().equals(MetaDataProperty.PASSWORD_TYPE)) {
					backupProperties.addProperty(propertyKey, getProperty(skinName, propertyKey));
				}
			}			

			// store the properties to file
			backupProperties.store();

		} catch (TransformerException te) {
			throw new GeneralPropertyException(
					"Could not transform backup properties xml: " + te.getMessage());
		} catch (IOException ioe) {
			throw new GeneralPropertyException(
					"Could not backup configurable properties: " + ioe.getMessage());
		} catch (ServiceException se) {
			throw new GeneralPropertyException("Could not get skins property file: "
					+ se.getMessage());
		}
	}

	/**
	 * Reports whether properties are fully configured.
	 * 
	 * @return a boolean that is true if properties are not unconfigured and
	 *         false otherwise
	 */
	public static boolean areSkinsConfigured() throws MetacatUtilException {
		try {
			return !PropertyService.getProperty("configutil.skinsConfigured").equals(
					PropertyService.UNCONFIGURED);
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not determine if skins are configured: "
					+ pnfe.getMessage());
		}
	}

	/**
	 * Take input from the user in an HTTP request about an property to be
	 * changed and update the metacat property file with that new value if it
	 * has changed from the value that was originally set.
	 * 
	 * @param request
	 *            that was generated by the user
	 * @param response
	 *            to send output back to the user
	 * @param propertyName
	 *            the name of the property to be checked and set
	 */
	public static void checkAndSetProperty(HttpServletRequest request, String skinName,
			String propertyName) throws GeneralPropertyException {
		String newValue = request.getParameter(skinName + "." + propertyName);
		checkAndSetProperty(newValue, skinName, propertyName);
	}

	/**
	 * Check user input against existing value and update the metacat property
	 * file with that new value if it has changed from the value that was
	 * originally set.
	 * 
	 * @param newValue
	 *            the value that was returned by the form
	 * @param skinname
	 *            the skin that we are checking
	 * @param propertyName
	 *            the name of the property to be checked and set
	 */
	public static void checkAndSetProperty(String newValue, String skinName,
			String propertyName) throws GeneralPropertyException {
		String oldValue = SkinPropertyService.getProperty(skinName, propertyName);
		if (newValue != null && !newValue.equals(oldValue)) {
			SkinPropertyService.setPropertyNoPersist(skinName, propertyName, newValue);
		}
	}

	/**
	 * Reports whether the metacat configuration utility should be run. Returns
	 * false if -- dev.runConfiguration=false and -- backup properties file
	 * exists Note that dev.runConfiguration should only be set to false when
	 * reinstalling the same version of the application in developement.
	 * 
	 * @return a boolean that is false if dev.runConfiguration is false and the
	 *         backup properties file exists.
	 */
	public static boolean bypassConfiguration() {
		boolean bypass = false;

		// We only want to go through the check once to see if we want to
		// bypass the configuration. We don't want to run through all of
		// this every time we hit metacat.
		if (bypassAlreadyChecked) {
			return bypass;
		}

		try {
			// check how dev.runConfiguration is set in metacat.properties
			String strRunConfiguration = PropertyService
					.getProperty("dev.runConfiguration");
			bypass = !(Boolean.parseBoolean(strRunConfiguration));

			// if the deb.runConfiguration is true, return false here.
			if (!bypass) {
				bypassAlreadyChecked = true;
				return false;
			}

			// the system is bypassing the configuration utility. We need to
			// get the backup properties and replace existing properties with
			// backup values. We do this for main and org properties.
			for (String skinName : skinNames) {
				SortedProperties backupProperties = getBackupProperties(skinName);
				Vector<String> backupPropertyNames = backupProperties.getPropertyNames();
				for (String backupPropertyName : backupPropertyNames) {
					String value = backupProperties.getProperty(backupPropertyName);
					backupProperties.setPropertyNoPersist(backupPropertyName, value);
				}
				backupProperties.store();
			}
		} catch (PropertyNotFoundException pnfe) {
			logMetacat.error("Could not find property: " + pnfe.getMessage());
		} catch (GeneralPropertyException gpe) {
			logMetacat.error("General property error: " + gpe.getMessage());
		}

		bypassAlreadyChecked = true;
		return bypass;
	}

}