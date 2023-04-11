package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A Class that provides a wrapper around standard java.util.Properties to provide
 * backwards compatibility with metacat's original properties implementation
 */
public class PropertiesWrapper {

    private static PropertiesWrapper propertiesWrapper = null;
    private static final String MAIN_CONFIG_FILE_NAME = "metacat.properties";
    private static final String MAIN_METADATA_FILE_NAME = "metacat.properties.metadata.xml";
    private static final String MAIN_BACKUP_FILE_NAME = "metacat.properties.backup";
    private static final String SITE_CONFIG_FILE_NAME = "metacat-site.properties";

    private static Properties mainProperties = null;
    private static String mainMetadataFilePath = null;
    String siteConfigFilePath = null;
    private static PropertiesMetaData mainMetaData = null;
    private static String mainBackupFilePath = null;
    private static SortedProperties mainBackupProperties = null;

    private static boolean bypassAlreadyChecked = false;

    private static final Log logMetacat = LogFactory.getLog(PropertiesWrapper.class);

    /**
     * private constructor since this is a singleton
     */
    private PropertiesWrapper() throws GeneralPropertyException {
        initialize();
    }

    protected static PropertiesWrapper getInstance()
        throws GeneralPropertyException {
        if (propertiesWrapper == null) {
            propertiesWrapper = new PropertiesWrapper();
        }
        return propertiesWrapper;
    }

    /**
     * Initialize the singleton.
     */
    private void initialize() throws GeneralPropertyException {
        logMetacat.debug("Initializing PropertiesWrapper");
        try {
            String mainConfigFilePath =
                PropertyService.CONFIG_FILE_DIR + FileUtil.getFS() + MAIN_CONFIG_FILE_NAME;
            mainMetadataFilePath =
                PropertyService.CONFIG_FILE_DIR + FileUtil.getFS() + MAIN_METADATA_FILE_NAME;

            // mainDefaultProperties will hold the default configuration from metacat.properties
            Properties mainDefaultProperties = new Properties();
            mainDefaultProperties.load(Files.newBufferedReader(Paths.get(mainConfigFilePath)));

            // mainProperties is the aggregation of the default props from metacat.properties,
            // overlaid with the site-specific configurable properties from metacatSite.properties
            mainProperties = new Properties(mainDefaultProperties);
            siteConfigFilePath = getSitePropertiesPath() + FileUtil.getFS() + SITE_CONFIG_FILE_NAME;
            Path sitePropsFilePathObj = Paths.get(siteConfigFilePath);
            if (!Files.exists(sitePropsFilePathObj)) {
                Files.createFile(sitePropsFilePathObj); // checks no file exists before creating
                logMetacat.info("created empty site properties file at: " + siteConfigFilePath);
            }
            mainProperties.load(Files.newBufferedReader(sitePropsFilePathObj));
            logMetacat.info("loaded defaults and site config from " + siteConfigFilePath
                + " into mainProperties.");
            store(); //to persist new value of site properties path

            // include main metacat properties in d1 properties as overrides
            try {
                Settings.getConfiguration();
                Settings.augmentConfiguration(mainConfigFilePath);
                Settings.augmentConfiguration(siteConfigFilePath);
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
            mainBackupFilePath =
                getSitePropertiesPath() + FileUtil.getFS() + MAIN_BACKUP_FILE_NAME;
            mainBackupProperties = new SortedProperties(mainBackupFilePath);
            mainBackupProperties.load();
        } catch (TransformerException te) {
            throw new GeneralPropertyException(
                "Transform problem while loading properties: " + te.getMessage());
        } catch (IOException ioe) {
            throw new GeneralPropertyException("I/O problem while loading properties: " + ioe.getMessage());
        }
    }

    /**
     * Utility method to get a property value from the properties file
     *
     * @param propertyName the name of the property requested
     * @return the String value for the property
     */
    protected String getProperty(String propertyName) throws PropertyNotFoundException {
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
    protected Vector<String> getPropertyNames() {
        return new Vector<>(mainProperties.stringPropertyNames());
    }


    /**
     * Get a Set of all property names that start with the groupName prefix.
     *
     * @param groupName the prefix of the keys to search for.
     * @return enumeration of property names
     */
    protected Vector<String> getPropertyNamesByGroup(String groupName) {

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
    protected Map<String, String> getPropertiesByGroup(String groupName)
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
    protected void addProperty(String propertyName, String value) throws GeneralPropertyException {
        mainProperties.setProperty(propertyName, value);
        store();
    }

    /**
     * Utility method to set a property value both in memory and to the properties file
     *
     * @param propertyName the name of the property requested
     * @param newValue     the new value for the property
     */
    protected void setProperty(String propertyName, String newValue) throws GeneralPropertyException {
        setPropertyNoPersist(propertyName, newValue);
        store();
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
    protected void setPropertyNoPersist(String propertyName, String newValue)
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
     * Save the properties to a properties file.
     */
    protected void persistProperties() throws GeneralPropertyException {
        store();
    }

    /**
     * Get the main backup properties file. These are configurable properties that are stored
     * outside the metacat install directories so the user does not need to re-enter all the
     * configuration information every time they do an upgrade.
     *
     * @return a SortedProperties object with the backup properties
     */
    protected SortedProperties getMainBackupProperties() {
        return mainBackupProperties;
    }

    /**
     * Get the main properties metadata. This is retrieved from an xml file that describes the
     * attributes of configurable properties.
     *
     * @return a PropertiesMetaData object with the main properties metadata
     */
    protected PropertiesMetaData getMainMetaData() {
        return mainMetaData;
    }

    /**
     * Writes out backup configurable properties to a file.
     */
    protected void persistMainBackupProperties() throws GeneralPropertyException {

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
     * Reports whether properties are fully configured.
     *
     * @return a boolean that is true if properties are configured and false otherwise
     */
    protected boolean arePropertiesConfigured() throws GeneralPropertyException {
        String propertiesConfigured = getProperty("configutil.propertiesConfigured");
        return propertiesConfigured != null && !propertiesConfigured.equals("false");
    }

    /**
     * Determine if the system is able to bypass configuration. If so, the system will look
     * for backup configuration files at startup time and use those to configure metacat. The bypass
     * options should only be set by developers. Production code should never bypass configuration.
     *
     * @return true if dev.runConfiguration is set to true in metacat.properties, and we have not
     * already checked for bypass; false otherwise.
     */
    protected boolean canBypass() throws PropertyNotFoundException {
        boolean result = false;
        // We only want to go through the check once to see if we want to bypass the configuration.
        // We don't want to run through all of this every time we hit metacat.
        if (bypassAlreadyChecked) {
            logMetacat.debug("canBypass() returning false, since already previously checked");
        } else {
            // check how dev.runConfiguration is set in metacat.properties
            String strRunConfiguration = getProperty("dev.runConfiguration");
            logMetacat.debug(
                "canBypass(): 'dev.runConfiguration property' set to: " + strRunConfiguration);
            boolean runConfiguration = Boolean.parseBoolean(strRunConfiguration);
            bypassAlreadyChecked = runConfiguration;
            result = !runConfiguration;
        }
        return result;
    }

    /**
     * Bypasses the metacat properties configuration utility. (Dev use only)
     */
    protected void bypassConfiguration() {
        try {
            if (!canBypass()) {
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
    protected void checkAndSetProperty(HttpServletRequest request, String propertyName)
        throws GeneralPropertyException {
        String value = getProperty(propertyName);
        String newValue = request.getParameter(propertyName);
        if (newValue != null && !newValue.trim().equals(value)) {
            setPropertyNoPersist(propertyName, newValue.trim());
        }
    }


    /**
     * store the properties to file. NOTE that this will persist only the properties that are:
     * (a) not included in mainDefaultProperties, or
     * (b) have changed from the values in mainDefaultProperties.
     * From the <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html">
     *     java.util.Properties javadoc</a>: "Properties from the defaults
     * table of this Properties table (if any) are not written out by this method."
     * Also, Properties class is thread-safe - no need to synchronize.
     */
    private void store() throws GeneralPropertyException {
        try (Writer output = new FileWriter(siteConfigFilePath)) {
            mainProperties.store(output, null);
        } catch (IOException e) {
            throw new GeneralPropertyException(e.toString());
        }
    }

    /**
     * Get the path to the directory where the site-specific properties (aka backup properties) are
     * stored
     *
     * @return String representation of the directory path
     * @throws GeneralPropertyException if there are issues retrieving or persisting the value
     */
     protected String getSitePropertiesPath() throws GeneralPropertyException {

        String sitePropertiesPath = getProperty("application.backupDir");
        if (sitePropertiesPath == null || sitePropertiesPath.equals("")) {
            try {
                sitePropertiesPath = SystemUtil.getStoredBackupDir();
            } catch (MetacatUtilException e) {
                logMetacat.error("Problem calling SystemUtil.getStoredBackupDir(): "
                        + e.getMessage(), e);
            }
        }
        if ((sitePropertiesPath == null || sitePropertiesPath.equals(""))
            && PropertyService.getRecommendedExternalDir() != null) {
            try {
                sitePropertiesPath = PropertyService.getRecommendedExternalDir()
                    + FileUtil.getFS() + "." + ServiceService.getRealApplicationContext();
            } catch (ServiceException e) {
                logMetacat.error("Problem calling ServiceService.getRealApplicationContext: "
                    + e.getMessage(), e);
            }
        }
         if (sitePropertiesPath != null && !sitePropertiesPath.equals("")) {
             try {
                 setPropertyNoPersist("application.backupDir", sitePropertiesPath);
                 SystemUtil.writeStoredBackupFile(sitePropertiesPath);
             } catch (GeneralPropertyException e) {
                 logMetacat.error(
                     "Problem trying to set property: 'application.backupDir' to value: "
                         + sitePropertiesPath, e);
                 throw e;
             } catch (MetacatUtilException e) {
                 String msg =
                     "Problem calling SystemUtil.writeStoredBackupFile() with sitePropertiesPath: "
                         + sitePropertiesPath + ". Exception was: " + e.getMessage();
                 logMetacat.error(msg, e);
                 throw new GeneralPropertyException(msg);
             }
         }
        return sitePropertiesPath;
    }
}
