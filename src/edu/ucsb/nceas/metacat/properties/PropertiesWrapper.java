/**
 *    Purpose: A Class that implements properties methods for metacat
 *  Copyright: 2023 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Author: Matthew Brooke
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

import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SystemUtil;

import edu.ucsb.nceas.utilities.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * A suite of utility classes for the metadata configuration utility
 */
public class PropertiesWrapper extends BaseService implements PropertiesInterface {

    private static final String MAIN_CONFIG_FILE_NAME = "metacat.properties";
    private static final String MAIN_METADATA_FILE_NAME = "metacat.properties.metadata.xml";
    private static final String MAIN_BACKUP_FILE_NAME = "metacat.properties.backup";
    private static String mainConfigFilePath = null;
    private static Properties mainProperties = null;
    private static String mainMetadataFilePath = null;
    private static PropertiesMetaData mainMetaData = null;
    private static String mainBackupFilePath = null;
    private static SortedProperties mainBackupProperties = null;

    private static boolean bypassAlreadyChecked = false;

    private static final Log logMetacat = LogFactory.getLog(PropertiesWrapper.class);
    private AuthPropertiesDelegate authPropertiesDelegate;

    /**
     * private constructor since this is a singleton
     */
    protected PropertiesWrapper() throws ServiceException {
        _serviceName = "PropertiesWrapper";
        initialize();
    }

    public boolean refreshable() {
        return true;
    }

    public void doRefresh() throws ServiceException {
        initialize();
    }

    public void stop() throws ServiceException {}

    /**
     * Initialize the singleton.
     */
    private void initialize() throws ServiceException {
        logMetacat.debug("Initializing ConfigurableProperties");
        try {
            mainConfigFilePath =
                PropertyService.CONFIG_FILE_DIR + FileUtil.getFS() + MAIN_CONFIG_FILE_NAME;
            mainMetadataFilePath =
                PropertyService.CONFIG_FILE_DIR + FileUtil.getFS() + MAIN_METADATA_FILE_NAME;

            // mainProperties will hold the primary configuration values for
            // metacat.
            mainProperties = new Properties();
            mainProperties.load(Files.newBufferedReader(Paths.get(mainConfigFilePath)));

            // include main metacat properties in d1 properties as overrides
            try {
                Settings.getConfiguration();
                Settings.augmentConfiguration(mainConfigFilePath);
            } catch (ConfigurationException e) {
                logMetacat.error("Could not augment DataONE properties. " + e.getMessage(), e);
            }

            // mainMetaData holds configuration information about main
            // properties. This is primarily used to display input fields on
            // the configuration page. The information is retrieved from an
            // xml metadata file
            mainMetaData = new PropertiesMetaData(mainMetadataFilePath);

            // The mainBackupProperties hold properties that were backed up
            // the last time the application was configured. On disk, the
            // file will look like a smaller version of metacat.properties.
            // It is stored in the data storage directory outside the
            // application directories.
            mainBackupFilePath = getSitePropsPath() + FileUtil.getFS() + MAIN_BACKUP_FILE_NAME;
            mainBackupProperties = new SortedProperties(mainBackupFilePath);
            mainBackupProperties.load();

            authPropertiesDelegate = new AuthPropertiesDelegate(getSitePropsPath());

        } catch (TransformerException te) {
            throw new ServiceException(
                "Transform problem while loading properties: " + te.getMessage());
        } catch (IOException ioe) {
            throw new ServiceException("I/O problem while loading properties: " + ioe.getMessage());
        } catch (GeneralPropertyException gpe) {
            throw new ServiceException(
                "General properties problem while loading properties: " + gpe.getMessage());
        } catch (MetacatUtilException ue) {
            throw new ServiceException(
                "Utilities problem while loading properties: " + ue.getMessage());
        }
    }

    private String getSitePropsPath()
        throws PropertyNotFoundException, MetacatUtilException, ServiceException {

        String backupPath = getProperty("application.backupDir");
        if (backupPath == null || backupPath.equals("")) {
            backupPath = SystemUtil.getStoredBackupDir();
        }
        if ((backupPath == null || backupPath.equals(""))
            && PropertyService.getRecommendedExternalDir() != null) {
            backupPath = PropertyService.getRecommendedExternalDir()
                + FileUtil.getFS() + "." + ServiceService.getRealApplicationContext();
        }
        // if backupPath is still null, no reason to initialize the
        // backup properties. The system will need to prompt the user for
        // the backup properties and reinitialize ConfigurableProperties.
        if (backupPath != null && !backupPath.equals("")) {
            try {
                setProperty("application.backupDir", backupPath);
            } catch (GeneralPropertyException e) {
                logMetacat.error(
                    "Problem trying to set property 'application.backupDir' to value "
                        + backupPath, e);
            }
            SystemUtil.writeStoredBackupFile(backupPath);
        }
        return backupPath;
    }

    /**
     * Utility method to get a property value from the properties file
     *
     * @param propertyName the name of the property requested
     * @return the String value for the property
     */
    public String getProperty(String propertyName) throws PropertyNotFoundException {
        String returnVal = mainProperties.getProperty(propertyName);
        if (returnVal == null) {
            throw new PropertyNotFoundException("No property found with name " + propertyName);
        } else {
            return returnVal;
        }
    }

    /**
     * Get a set of all property names.
     *
     * @return Vector of property names
     */
    public Vector<String> getPropertyNames() {
        return new Vector<>(mainProperties.stringPropertyNames());
    }


    /**
     * Get a Set of all property names that start with the groupName prefix.
     *
     * @param groupName the prefix of the keys to search for.
     * @return enumeration of property names
     */
    public Vector<String> getPropertyNamesByGroup(String groupName) {

        groupName = groupName.trim();
        if (!groupName.endsWith(".")) {
            groupName += (".");
        }
        final String finalGroupName = groupName;
        Vector<String> propNames = getPropertyNames();
        propNames.removeIf(prop -> !prop.startsWith(finalGroupName));
        return propNames;
    }

    /**
     * Get a Map of all properties that start with the groupName prefix.
     *
     * @param groupName the prefix of the keys to search for.
     * @return Map of property names
     */
    public Map<String, String> getPropertiesByGroup(String groupName)
        throws PropertyNotFoundException {

        HashMap<String, String> groupPropertyMap = new HashMap<>();
        for (String key : getPropertyNamesByGroup(groupName)) {
            groupPropertyMap.put(key, getProperty(key));
        }
        return groupPropertyMap;
    }

    // TODO: MB - can we get rid of this? Default java.util.Properties behavior is to add a new
    //  entry if it doesn't already exist, when setProperty() is called; so addProperty() not needed
    /**
     * Utility method to add a property value both in memory and to the properties file
     *
     * @param propertyName the name of the property to add
     * @param value        the value for the property
     */
    public void addProperty(String propertyName, String value) throws GeneralPropertyException {
        mainProperties.setProperty(propertyName, value);
        try {
            store(mainProperties, mainConfigFilePath);
        } catch (IOException e) {
            throw new GeneralPropertyException(e.toString());
        }
    }

    /**
     * Utility method to set a property value both in memory and to the properties file
     *
     * @param propertyName the name of the property requested
     * @param newValue     the new value for the property
     */
    public void setProperty(String propertyName, String newValue) throws GeneralPropertyException {
        setPropertyNoPersist(propertyName, newValue);
        try {
            store(mainProperties, mainConfigFilePath);
        } catch (IOException e) {
            throw new GeneralPropertyException(e.toString());
        }
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
    public void setPropertyNoPersist(String propertyName, String newValue)
        throws GeneralPropertyException {
        if (null == mainProperties.getProperty(propertyName)) {
            // TODO: MB - can we get rid of this? Default java.util.Properties behavior is
            //  to add a new entry if it doesn't already exist, when setProperty() is called
            throw new PropertyNotFoundException("Property: " + propertyName
                + " could not be updated to: " + newValue
                + " because it does not already exist in properties.");
        }
        mainProperties.setProperty(propertyName, newValue);
    }

    /**
     * Save the properties to a properties file. Note, the order and comments will be preserved.
     */
    public void persistProperties() throws GeneralPropertyException {
        try {
            store(mainProperties, mainConfigFilePath);
        } catch (IOException e) {
            throw new GeneralPropertyException(e.toString());
        }
    }

    /**
     * Get the main backup properties file. These are configurable properties that are stored
     * outside the metacat install directories so the user does not need to re-enter all the
     * configuration information every time they do an upgrade.
     *
     * @return a SortedProperties object with the backup properties
     */
    public SortedProperties getMainBackupProperties() {
        return mainBackupProperties;
    }

    /**
     * Get the auth backup properties file. These are configurable properties that are stored
     * outside the metacat install directories so the user does not need to re-enter all the
     * configuration information every time they do an upgrade.
     *
     * @return a SortedProperties object with the backup properties
     */
    public SortedProperties getAuthBackupProperties() {
        return authPropertiesDelegate.getAuthBackupProperties();
    }

    /**
     * Get the main properties metadata. This is retrieved from an xml file that describes the
     * attributes of configurable properties.
     *
     * @return a PropertiesMetaData object with the main properties metadata
     */
    public PropertiesMetaData getMainMetaData() {
        return mainMetaData;
    }

    /**
     * Get the auth properties metadata. This is retrieved from an xml file that describes the
     * attributes of configurable properties.
     *
     * @return a PropertiesMetaData object with the organization properties metadata
     */
    public PropertiesMetaData getAuthMetaData() {
        return authPropertiesDelegate.getAuthMetaData();
    }

    /**
     * Writes out backup configurable properties to a file.
     */
    public void persistMainBackupProperties() throws GeneralPropertyException {

        // Use the metadata to extract configurable properties from the
        // overall properties list, and store those properties.
        try {
            SortedProperties backupProperties = new SortedProperties(mainBackupFilePath);

            // Populate the backup properties for main metacat properties using
            // the associated metadata file
            PropertiesMetaData mainMetadata = new PropertiesMetaData(mainMetadataFilePath);

            Map<String, MetaDataProperty> mainKeyMap = mainMetadata.getProperties();
            Set<String> mainKeySet = mainKeyMap.keySet();
            for (String propertyKey : mainKeySet) {
                // don't backup passwords
                MetaDataProperty metaData = mainKeyMap.get(propertyKey);
                if (!metaData.getFieldType().equals(MetaDataProperty.PASSWORD_TYPE)) {
                    backupProperties.addProperty(propertyKey, getProperty(propertyKey));
                }
            }

            // store the properties to file
            backupProperties.store();
            mainBackupProperties = new SortedProperties(mainBackupFilePath);
            mainBackupProperties.load();

        } catch (TransformerException te) {
            throw new GeneralPropertyException(
                "Could not transform backup properties xml: " + te.getMessage());
        } catch (IOException ioe) {
            throw new GeneralPropertyException(
                "Could not backup configurable properties: " + ioe.getMessage());
        }
    }

    /**
     * Writes out backup configurable properties to a file.
     */
    public void persistAuthBackupProperties(ServletContext servletContext)
        throws GeneralPropertyException {
        authPropertiesDelegate.persistAuthBackupProperties(servletContext);
    }

    /**
     * Reports whether properties are fully configured.
     *
     * @return a boolean that is true if properties are configured and false otherwise
     */
    public boolean arePropertiesConfigured() throws GeneralPropertyException {
        String propertiesConfigured = getProperty("configutil.propertiesConfigured");
        return propertiesConfigured != null && !propertiesConfigured.equals(UNCONFIGURED);
    }

    /**
     * Determine if the system is configured to bypass configuration. If so, the system will look
     * for backup configuration files at startup time and use those to configure metacat. The bypass
     * options should only be set by developers. Production code should never bypass configuration.
     *
     * @return true if dev.runConfiguration is set to true in metacat.properties, and we have not
     * already checked for bypass; false otherwise.
     */
    public boolean doBypass() throws PropertyNotFoundException {
        // We only want to go through the check once to see if we want to
        // bypass the configuration. We don't want to run through all of
        // this every time we hit metacat.
        if (bypassAlreadyChecked) {
            logMetacat.debug(
                "bypassConfiguration not performing full bypass check.  Bypass set to false");
            return false;
        }

        // check how dev.runConfiguration is set in metacat.properties
        String strRunConfiguration = getProperty("dev.runConfiguration");
        boolean runConfiguration = Boolean.parseBoolean(strRunConfiguration);
        logMetacat.debug(
            "bypassConfiguration: dev.runConfiguration property set to: " + strRunConfiguration);

        // if the dev.runConfiguration is true, return false here.
        if (runConfiguration) {
            bypassAlreadyChecked = true;
            return false;
        }

        return true;
    }

    /**
     * Bypasses the metacat properties configuration utility. (Dev use only)
     */
    public void bypassConfiguration() {
        try {
            boolean doBypass = doBypass();

            if (!doBypass) {
                throw new GeneralPropertyException(
                    "Attempting to do bypass when system is not configured for it.");
            }

            // The system is bypassing the configuration utility. We need to
            // get the backup properties and replace existing properties with
            // backup values.  We do this for main and org properties.
            logMetacat.debug("bypassConfiguration: setting main backup properties.");
            SortedProperties mainBackupProperties = getMainBackupProperties();
            Vector<String> backupPropertyNames = mainBackupProperties.getPropertyNames();
            for (String backupPropertyName : backupPropertyNames) {
                String value = mainBackupProperties.getProperty(backupPropertyName);
                setPropertyNoPersist(backupPropertyName, value);
            }

            logMetacat.debug("bypassConfiguration: setting auth backup properties.");
            Vector<String> authBackupPropertyNames
                = authPropertiesDelegate.getAuthBackupProperties().getPropertyNames();
            for (String authBackupPropertyName : authBackupPropertyNames) {
                String value
                    = authPropertiesDelegate.getAuthBackupProperties().getProperty(
                        authBackupPropertyName);
                setPropertyNoPersist(authBackupPropertyName, value);
            }

            logMetacat.debug("bypassConfiguration: setting configutil sections to true.");
            setPropertyNoPersist("configutil.propertiesConfigured", "true");
            setPropertyNoPersist("configutil.authConfigured", "true");
            setPropertyNoPersist("configutil.skinsConfigured", "true");
            setPropertyNoPersist("configutil.databaseConfigured", "true");
            setPropertyNoPersist("configutil.geoserverConfigured", "bypassed");

            persistProperties();

        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("bypassConfiguration: Could not find property: " + pnfe.getMessage());
        } catch (GeneralPropertyException gpe) {
            logMetacat.error("bypassConfiguration: General property error: " + gpe.getMessage());
        }

        bypassAlreadyChecked = true;
    }

    /**
     * Take input from the user in an HTTP request about a property to be changed and update the
     * metacat property file with that new value if it has changed from the value that was
     * originally set.
     *
     * @param request      that was generated by the user
     * @param propertyName the name of the property to be checked and set
     */
    // TODO: MB - can we get rid of this? AFAICT, only callers do not use the boolean return value
    //  (but double-check!), so a simple "setProperty" call should suffice (assuming we get rid of
    //  "addProperty" and the PropertyNotFoundException)
    public boolean checkAndSetProperty(HttpServletRequest request, String propertyName)
        throws GeneralPropertyException {
        boolean changed = false;
        String value = getProperty(propertyName);
        String newValue = request.getParameter(propertyName);
        if (newValue != null && !newValue.trim().equals(value)) {
            setPropertyNoPersist(propertyName, newValue.trim());
            changed = true;
        }
        return changed;
    }

    //Properties class is thread-safe - no need to synchronize. See:
    // https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html
    private void store(Properties properties, String FilePath) throws IOException {
        try (Writer output = new FileWriter(FilePath)) {
            properties.store(output, null);
        }
    }
}
