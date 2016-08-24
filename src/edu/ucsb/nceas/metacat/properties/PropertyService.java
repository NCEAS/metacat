/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements properties methods for metacat
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

import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.dataone.configuration.Settings;

import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * A suite of utility classes for the metadata configuration utility
 */
public class PropertyService extends BaseService {

	private static PropertyService propertyService = null;

	private static PropertiesInterface properties = null;

	// system is configured
	public static final String CONFIGURED = PropertiesInterface.CONFIGURED;
	// system has never been configured
	public static final String UNCONFIGURED = PropertiesInterface.UNCONFIGURED;
	public static final String BYPASSED = PropertiesInterface.BYPASSED;

	private static String DEFAULT_CONFIG_FILE_DIR = "WEB-INF";
	public static String CONFIG_FILE_DIR = null;
	public static String CONFIG_FILE_NAME = null;
	public static String CONFIG_FILE_PATH = null;
	
	public static String TEST_CONFIG_FILE_NAME = null;

	private static String DEFAULT_PROPERTY_CLASS_NAME = "edu.ucsb.nceas.metacat.properties.ConfigurableProperties";
	private static String PROPERTY_CLASS_NAME = null;

	private static String RECOMMENDED_EXTERNAL_DIR = null;

	private static Logger logMetacat = Logger.getLogger(PropertyService.class);

	/**
	 * private constructor since this is a singleton
	 * 
	 * @param servletContext
	 *            the context we will use to get relative paths
	 */
	private PropertyService() throws ServiceException {
		_serviceName = "PropertyService";
		
		initialize();
	}

	/**
	 * Get the single instance of PropertyService.
	 * 
	 * @return the single instance of PropertyService
	 */
	public static PropertyService getInstance(ServletContext context)
			throws ServiceException {
		if (propertyService == null) {
			
			String applicationName = (String)context.getAttribute("APPLICATION_NAME");
			
			CONFIG_FILE_DIR = context.getInitParameter("configFileDir");
			if (CONFIG_FILE_DIR == null) {
				String configDir = context.getRealPath(DEFAULT_CONFIG_FILE_DIR);
				CONFIG_FILE_DIR = configDir;
			}
			
			CONFIG_FILE_PATH = CONFIG_FILE_DIR + FileUtil.getFS() + applicationName + ".properties";

			PROPERTY_CLASS_NAME = context.getInitParameter("propertyClassName");
			if (PROPERTY_CLASS_NAME == null) {
				PROPERTY_CLASS_NAME = DEFAULT_PROPERTY_CLASS_NAME;
			}

			propertyService = new PropertyService();
		}
		return propertyService;
	}

	 /**
	 * Get the single instance of PropertyService for test purposes. In this
	 * case, we allow the configuration directory to be passed in.
	 *
	 * @param configDir the configuration directory we need to look in
	 * @return the single instance of PropertyService
	 */
	 public static PropertyService getInstance(String testConfigFileDir)
			throws ServiceException {
		if (propertyService == null) {
			CONFIG_FILE_DIR = testConfigFileDir;

			PROPERTY_CLASS_NAME = DEFAULT_PROPERTY_CLASS_NAME;

			propertyService = new PropertyService();
		}
		return propertyService;
	}
	 
	 /**
		 * Get the single instance of PropertyService for test purposes. In this
		 * case, we allow the configuration directory to be passed in.
		 *
		 * @param configDir the configuration directory we need to look in
		 * @return the single instance of PropertyService
		 */
		 public static PropertyService getInstance() throws ServiceException {
			if (propertyService == null) {
				throw new ServiceException("PropertyService.getInstance - cannot call " +
						"getInstance without parameters until property service has been created " +
						"with either servlet context or config file path.");
			}
			return propertyService;
		}

	public boolean refreshable() {
		return true;
	}

	public void doRefresh() throws ServiceException {
	    initialize();
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
	private void initialize() throws ServiceException {

		logMetacat.debug("Initializing PropertyService");
		
		try {
			Class<?> classDef = Class.forName(PROPERTY_CLASS_NAME);
			properties = (PropertiesInterface) classDef.newInstance();
		} catch (InstantiationException ie) {
			throw new ServiceException("Could not instantiate property class: "
					+ PROPERTY_CLASS_NAME + " " + ie.getMessage());
		} catch (IllegalAccessException iae) {
			throw new ServiceException("Access error when intantiating property class: "
					+ PROPERTY_CLASS_NAME + " " + iae.getMessage());
		} catch (ClassNotFoundException cnfe) {
			throw new ServiceException("Could not find property class: "
					+ PROPERTY_CLASS_NAME + " " + cnfe.getMessage());
		}
	}

	/**
	 * Utility method to get a property value from the properties file
	 * 
	 * @param propertyName
	 *            the name of the property requested
	 * @return the String value for the property
	 */
	public static String getProperty(String propertyName)
			throws PropertyNotFoundException {
		return properties.getProperty(propertyName);
	}

	/**
	 * Get a set of all property names.
	 * 
	 * @return Set of property names
	 */
	public static Vector<String> getPropertyNames() {
		return properties.getPropertyNames();
	}

	/**
	 * Get a Set of all property names that start with the groupName prefix.
	 * 
	 * @param groupName
	 *            the prefix of the keys to search for.
	 * @return enumeration of property names
	 */
	public static Vector<String> getPropertyNamesByGroup(String groupName) {
		return properties.getPropertyNamesByGroup(groupName);
	}

	/**
	 * Get a Map of all properties that start with the groupName prefix.
	 * 
	 * @param groupName
	 *            the prefix of the keys to search for.
	 * @return Map of property names
	 */
	public static Map<String, String> getPropertiesByGroup(String groupName)
			throws PropertyNotFoundException {
		return properties.getPropertiesByGroup(groupName);
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
	public static void setProperty(String propertyName, String newValue)
			throws GeneralPropertyException {
		properties.setProperty(propertyName, newValue);
		properties.persistProperties();
	}
	
	/**
	 * Utility method to add a property value both in memory and to the
	 * properties file
	 * 
	 * @param propertyName
	 *            the name of the property to add
	 * @param newValue
	 *            the value for the property
	 */
	public static void addProperty(String propertyName, String value)
			throws GeneralPropertyException {
		properties.addProperty(propertyName, value);
		properties.persistProperties();
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
	public static void setPropertyNoPersist(String propertyName, String newValue)
			throws GeneralPropertyException {
		properties.setPropertyNoPersist(propertyName, newValue);
	}

	/**
	 * Save the properties to a properties file. Note, the order and comments
	 * will be preserved.
	 */
	public static void persistProperties() throws GeneralPropertyException {
		properties.persistProperties();
	}

	/**
	 * Get the main backup properties file. These are configurable properties
	 * that are stored outside the metacat install directories so the user does
	 * not need to re-enter all the configuration information every time they do
	 * an upgrade.
	 * 
	 * @return a SortedProperties object with the backup properties
	 */
	public static SortedProperties getMainBackupProperties() throws GeneralPropertyException {
		return properties.getMainBackupProperties();
	}

	/**
	 * Get the auth backup properties file. These are configurable properties
	 * that are stored outside the metacat install directories so the user does
	 * not need to re-enter all the configuration information every time they do
	 * an upgrade.
	 * 
	 * @return a SortedProperties object with the backup properties
	 */
	public static SortedProperties getAuthBackupProperties() throws GeneralPropertyException{
		return properties.getAuthBackupProperties();
	}

	/**
	 * Get the main properties metadata. This is retrieved from an xml file that
	 * describes the attributes of configurable properties.
	 * 
	 * @return a PropertiesMetaData object with the main properties metadata
	 */
	public static PropertiesMetaData getMainMetaData() throws GeneralPropertyException{
		return properties.getMainMetaData();
	}

	/**
	 * Get the auth properties metadata. This is retrieved from an xml file that
	 * describes the attributes of configurable properties.
	 * 
	 * @return a PropertiesMetaData object with the organization properties
	 *         metadata
	 */
	public static PropertiesMetaData getAuthMetaData() throws GeneralPropertyException{
		return properties.getAuthMetaData();
	}

	/**
	 * Writes out backup configurable properties to a file.
	 */
	public static void persistMainBackupProperties() throws GeneralPropertyException {

		properties.persistMainBackupProperties();
	}

	/**
	 * Writes out backup configurable properties to a file.
	 */
	public static void persistAuthBackupProperties(ServletContext servletContext)
			throws GeneralPropertyException {

		properties.persistAuthBackupProperties(servletContext);
	}

	/**
	 * Reports whether properties are fully configured.
	 * 
	 * @return a boolean that is true if properties are not unconfigured and
	 *         false otherwise
	 */
	public static boolean arePropertiesConfigured() throws GeneralPropertyException {
		return properties.arePropertiesConfigured();
	}

	/**
	 * Determine if the system is configured to bypass configuration. If so, the
	 * system will look for backup configuration files at startup time and use
	 * those to configure metacat. The bypass options should only be set by
	 * developers. Production code should never bypass confguration.
	 * 
	 * @return true if dev.runConfiguration is set to true in metacat.properties
	 *         and we have not already checked for bypass, false otherwise.
	 */
	public static boolean doBypass() throws GeneralPropertyException {
		return properties.doBypass();
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
	public static void bypassConfiguration() throws GeneralPropertyException {
		properties.bypassConfiguration();
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
	public static boolean checkAndSetProperty(HttpServletRequest request, String propertyName)
			throws GeneralPropertyException {
		return properties.checkAndSetProperty(request, propertyName);
	}

	/**
	 * Sets the recommended external directory. This is populated during
	 * initialization time using the SystemUtil.discoverExternalDir() method.
	 * This directory will be used to suggest external user directories when the
	 * user configures metacat for the first time.
	 * 
	 */
	public static void setRecommendedExternalDir(String extBaseDir) {
		RECOMMENDED_EXTERNAL_DIR = extBaseDir;
	}

	/**
	 * Returns the recommended external base directory. This is populated during
	 * initialization time using the SystemUtil.discoverExternalBaseDir()
	 * method. This directory will be used to suggest external user directories
	 * when the user configures metacat for the first time.
	 * 
	 * @return a String holding the recommended external directory
	 */
	public static String getRecommendedExternalDir() {
		return RECOMMENDED_EXTERNAL_DIR;
	}
	
	/**
	 * The properties on the Setting class isn't synchronized with the change the Metacat properties file. 
	 * This method synchronizes (reloads) the properties' change to the Setting class when the property file was modified.
	 */
	public static void syncToSettings() throws GeneralPropertyException {
	    try {
	        Settings.getConfiguration();
	        Settings.augmentConfiguration(CONFIG_FILE_PATH);
	    } catch (ConfigurationException e) {
	        e.printStackTrace();
	        throw new GeneralPropertyException(e.getMessage());
	    }
	}

}