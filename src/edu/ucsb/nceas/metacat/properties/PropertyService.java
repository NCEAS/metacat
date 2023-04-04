/**
 * Purpose: A Class that implements properties methods for metacat
 * Copyright: 2008
 * Regents of the University of California and the National Center for Ecological
 * Analysis and Synthesis
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307  USA
 */

package edu.ucsb.nceas.metacat.properties;

import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    // system is configured
    public static final String CONFIGURED = "true";
    // system has never been configured
    public static final String UNCONFIGURED = "false";
    public static final String BYPASSED = "bypassed";
    private static final String DEFAULT_CONFIG_FILE_DIR = "WEB-INF";
    private static final Log logMetacat = LogFactory.getLog(PropertyService.class);
    public static String CONFIG_FILE_DIR = null;
    public static String CONFIG_FILE_PATH = null;
    private static PropertyService propertyService = null;
    private static PropertiesWrapper properties = null;
    private static AuthPropertiesDelegate authPropertiesDelegate = null;
    private static String RECOMMENDED_EXTERNAL_DIR = null;

    /**
     * private constructor since this is a singleton
     */
    private PropertyService() throws ServiceException {
        _serviceName = "PropertyService";
        initialize();
    }

    /**
     * Get the single instance of PropertyService.
     *
     * @param context a reference to the ServletContext
     * @return the single instance of PropertyService
     */
    public static PropertyService getInstance(ServletContext context) throws ServiceException {
        if (propertyService == null) {
            String applicationName = (String) context.getAttribute("APPLICATION_NAME");
            CONFIG_FILE_DIR = context.getInitParameter("configFileDir");
            if (CONFIG_FILE_DIR == null) {
                CONFIG_FILE_DIR = context.getRealPath(DEFAULT_CONFIG_FILE_DIR);
            }
            CONFIG_FILE_PATH = CONFIG_FILE_DIR + FileUtil.getFS() + applicationName + ".properties";

            propertyService = new PropertyService();
        }
        return propertyService;
    }

    /**
     * Get the single instance of PropertyService for test purposes. In this case, we allow the
     * configuration directory to be passed in.
     *
     * @param testConfigFileDir the test configuration directory we need to look in
     * @return the single instance of PropertyService
     */
    public static PropertyService getInstance(String testConfigFileDir) throws ServiceException {
        if (propertyService == null) {
            CONFIG_FILE_DIR = testConfigFileDir;
            propertyService = new PropertyService();
        }
        return propertyService;
    }

    /**
     * Get the single instance of PropertyService, only  after it has been previously instantiated
     * with a call to one of the other getInstance() methods passing either a reference to the
     * servlet context or the config file path
     *
     * @return the single instance of PropertyService
     */
    public static PropertyService getInstance() throws ServiceException {
        if (propertyService == null) {
            throw new ServiceException(
                "PropertyService.getInstance() - cannot call getInstance without parameters until" +
                    " " + "property service has been created with either servlet context or " +
                    "config file path.");
        }
        return propertyService;
    }

    /**
     * Utility method to get a property value from the properties file
     *
     * @param propertyName the name of the property requested
     * @return the String value for the property
     */
    public static String getProperty(String propertyName) throws PropertyNotFoundException {
        return properties.getProperty(propertyName);
    }

    /**
     * Get a set of all property names.
     *
     * @return Vector of property names
     */
    public static Vector<String> getPropertyNames() {
        return properties.getPropertyNames();
    }

    /**
     * Get a Set of all property names that start with the groupName prefix.
     *
     * @param groupName the prefix of the keys to search for.
     * @return enumeration of property names
     */
    public static Vector<String> getPropertyNamesByGroup(String groupName) {
        return properties.getPropertyNamesByGroup(groupName);
    }

    /**
     * Get a Map of all properties that start with the groupName prefix.
     *
     * @param groupName the prefix of the keys to search for.
     * @return Map of property names
     */
    public static Map<String, String> getPropertiesByGroup(String groupName)
        throws PropertyNotFoundException {
        return properties.getPropertiesByGroup(groupName);
    }

    /**
     * Utility method to set a property value both in memory and to the properties file
     *
     * @param propertyName the name of the property requested
     * @param newValue     the new value for the property
     */
    public static void setProperty(String propertyName, String newValue)
        throws GeneralPropertyException {
        properties.setProperty(propertyName, newValue);
        properties.persistProperties();
    }

    /**
     * Utility method to add a property value both in memory and to the properties file
     *
     * @param propertyName the name of the property to add
     * @param value     the value for the property
     */
    // TODO: MB - can we get rid of this? Default java.util.Properties behavior is to add a new
    //  entry if it doesn't already exist, when setProperty() is called; so addProperty() not needed
    public static void addProperty(String propertyName, String value)
        throws GeneralPropertyException {
        properties.addProperty(propertyName, value);
        properties.persistProperties();
    }

    /**
     * Utility method to set a property value in memory. This will NOT cause the property to be
     * written to disk. Use this method to set multiple properties in a row without causing
     * excessive I/O. You must call persistProperties() once you're done setting properties to have
     * them written to disk.
     *
     * @param propertyName the name of the property requested
     * @param newValue     the new value for the property
     */
    public static void setPropertyNoPersist(String propertyName, String newValue)
        throws GeneralPropertyException {
        properties.setPropertyNoPersist(propertyName, newValue);
    }

    /**
     * Save the properties to a properties file on disk.
     */
    public static void persistProperties() throws GeneralPropertyException {
        properties.persistProperties();
    }

    /**
     * Get the main backup properties file. These are configurable properties that are stored
     * outside the metacat install directories so the user does not need to re-enter all the
     * configuration information every time they do an upgrade.
     *
     * @return a SortedProperties object with the backup properties
     */
    public static SortedProperties getMainBackupProperties() throws GeneralPropertyException {
        return properties.getMainBackupProperties();
    }

    /**
     * Get the auth backup properties file. These are configurable properties that are stored
     * outside
     * the metacat install directories so the user does not need to re-enter all the configuration
     * information every time they do an upgrade.
     *
     * @return a SortedProperties object with the backup properties
     */
    public static SortedProperties getAuthBackupProperties() throws GeneralPropertyException {
        return authPropertiesDelegate.getAuthBackupProperties();
    }

    /**
     * Get the main properties metadata. This is retrieved from an xml file that describes the
     * attributes of configurable properties.
     *
     * @return a PropertiesMetaData object with the main properties metadata
     */
    public static PropertiesMetaData getMainMetaData() throws GeneralPropertyException {
        return properties.getMainMetaData();
    }

    /**
     * Get the auth properties metadata. This is retrieved from an xml file that describes the
     * attributes of configurable properties.
     *
     * @return a PropertiesMetaData object with the organization properties metadata
     */
    public static PropertiesMetaData getAuthMetaData() throws GeneralPropertyException {
        return authPropertiesDelegate.getAuthMetaData();
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
    public static void persistAuthBackupProperties() throws GeneralPropertyException {
        authPropertiesDelegate.persistAuthBackupProperties();
    }

    /**
     * Reports whether properties are fully configured.
     *
     * @return  returns true if all properties are configured, and false otherwise
     */
    public static boolean arePropertiesConfigured() throws GeneralPropertyException {
        return properties.arePropertiesConfigured();
    }

    /**
     * Determine if the system is able to bypass configuration. If so, the system will look
     * for backup or "site-specific" configuration files at startup time and use those to
     * configure metacat.
     * The bypass options should only be set by developers. Production code should never bypass
     * configuration.
     *
     * @return true if dev.runConfiguration is set to true in metacat.properties AND we have not
     * already checked for bypass; false otherwise.
     */
    public static boolean doBypass() throws GeneralPropertyException {
        return properties.doBypass();
    }

    /**
     * Bypasses the metacat properties configuration utility (for dev use only).
     */
    public static void bypassConfiguration() throws GeneralPropertyException {
        properties.bypassConfiguration();
    }

    /**
     * Take input from the user in an HTTP request about a property to be changed and update the
     * metacat property file with that new value if it has changed from the value that was
     * originally
     * set.
     *
     * @param request      that was generated by the user
     * @param propertyName the name of the property to be checked and set
     */
    public static void checkAndSetProperty(HttpServletRequest request, String propertyName)
        throws GeneralPropertyException {
        properties.checkAndSetProperty(request, propertyName);
    }

    /**
     * Returns the recommended external base directory. This is populated during initialization time
     * using the SystemUtil.discoverExternalBaseDir() method. This directory will be used to suggest
     * external user directories when the user configures metacat for the first time.
     *
     * @return a String holding the recommended external directory
     */
    public static String getRecommendedExternalDir() {
        if (RECOMMENDED_EXTERNAL_DIR == null) {
            try {
                RECOMMENDED_EXTERNAL_DIR = SystemUtil.discoverExternalDir();
            } catch (MetacatUtilException e) {
                logMetacat.error("Error calling SystemUtil.discoverExternalDir(): Not assigned: "
                        + e.getMessage(),e);
            }
        }
        return RECOMMENDED_EXTERNAL_DIR;
    }

    /**
     * Sets the recommended external directory. This is populated during initialization time
     * using the SystemUtil.discoverExternalDir() method. This directory will be used to suggest
     * external user directories when the user configures metacat for the first time.
     */
    public static void setRecommendedExternalDir(String extBaseDir) {
        RECOMMENDED_EXTERNAL_DIR = extBaseDir;
    }

    /**
     * The properties on the Setting class isn't synchronized with the change the Metacat properties
     * file. This method synchronizes (reloads) the properties' change to the Setting class when the
     * property file was modified.
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

    /**
     * Initialize the singleton.
     */
    private void initialize() throws ServiceException {
        logMetacat.debug("Initializing PropertyService");

        properties = PropertiesWrapper.getInstance();
        authPropertiesDelegate = properties.getAuthPropertiesDelegate();
    }

    public boolean refreshable() {
        return true;
    }

    public void doRefresh() throws ServiceException {
        initialize();
    }

    public void stop() throws ServiceException {
    }
}
