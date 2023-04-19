package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.MetaDataProperty;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;

import javax.xml.transform.TransformerException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

/**
 * A Class that provides a wrapper around standard java.util.Properties to provide backwards
 * compatibility with metacat's original properties implementation
 */
public class PropertiesWrapper {

    private static PropertiesWrapper propertiesWrapper = null;
    private static SortedProperties mainBackupProperties = null;
    private static boolean bypassAlreadyChecked = false;
    private static final Log logMetacat = LogFactory.getLog(PropertiesWrapper.class);
    private Properties mainProperties = null;
    private String mainMetadataFilePath = null;
    private PropertiesMetaData mainMetaData = null;
    private String mainBackupFilePath = null;

    // Full Path to the default properties file
    // Example: /var/lib/tomcat/webapps/WEB-INF/metacat.properties
    private static Path defaultPropertiesFilePath = null;

    // Full Path to the site-specific properties file to be overlaid on top of the defaults
    // Example: /var/metacat/.metacat/metacat-site.properties
    private static Path sitePropertiesFilePath = null;

    // Full Path to the DIRECTORY containing the site-specific configuration files (such as
    // metacat-site.properties, metacat.properties.backup etc.). Example: /var/metacat/.metacat
    private static Path siteConfigDirPath;


    /**
     * private constructor since this is a singleton
     */
    private PropertiesWrapper() throws GeneralPropertyException {
        initialize();
    }

    /**
     * get the current instance of the PropertiesWrapper. To set specific property file locations
     * . use the <code>getNewInstance(Path, Path)</code> method.
     *
     * @return singleton instance of PropertiesWrapper
     * @throws GeneralPropertyException when things go wrong
     */
    protected static PropertiesWrapper getInstance() throws GeneralPropertyException {

        if (propertiesWrapper == null) {
            propertiesWrapper = new PropertiesWrapper();
        }
        return propertiesWrapper;
    }

    /**
     * get an instance of the PropertiesWrapper. <em>WARNING: all calls to this method will
     * re-initialize PropertiesWrapper with the newly-passed values of defaultPropertiesFilePath
     * and sitePropertiesFilePath.</em> If you just want to get an instance without affecting its
     * internals, call <code>getinstance()</code> instead.
     *
     * @param defaultPropertiesFilePath (Can be null) Full path to the default properties file
     *                                  (e.g. /var/lib/tomcat/webapps/WEB-INF/metacat.properties)
     * @param sitePropertiesFilePath (Can be null) Full path to the site properties file
     *                               (e.g. /var/metacat/.metacat/metacat-site.properties)
     * @return singleton instance of PropertiesWrapper
     * @throws GeneralPropertyException when things go wrong
     */
    protected static PropertiesWrapper getNewInstance(Path defaultPropertiesFilePath,
        Path sitePropertiesFilePath) throws GeneralPropertyException {

        PropertiesWrapper.defaultPropertiesFilePath = defaultPropertiesFilePath;
        PropertiesWrapper.sitePropertiesFilePath = sitePropertiesFilePath;
        if (sitePropertiesFilePath != null) {
            siteConfigDirPath = sitePropertiesFilePath.getParent();
        }
        propertiesWrapper = new PropertiesWrapper();
        return propertiesWrapper;
    }

    /**
     * Get a property value from the properties file.
     *
     * @param propertyName the name of the property requested
     * @return the String value for the property, even if blank. Will never return null
     * @throws PropertyNotFoundException if the passed <code>propertyName</code> key is not in the
     *                                   properties at all
     */
    protected String getProperty(String propertyName) throws PropertyNotFoundException {
        String returnVal;
        if (mainProperties.getProperty(propertyName) != null) {
            returnVal = mainProperties.getProperty(propertyName);
        } else {
            logMetacat.error("did not find the property with key " + propertyName);
            throw new PropertyNotFoundException(
                "PropertiesWrapper.getProperty(): Key/name does not exist in Properties: "
                    + propertyName);
        }
        return returnVal;
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

        Map<String, String> groupPropertyMap = new HashMap<>();
        for (String key : getPropertyNamesByGroup(groupName)) {
            groupPropertyMap.put(key, getProperty(key));
        }
        return groupPropertyMap;
    }

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
    protected void setProperty(String propertyName, String newValue)
        throws GeneralPropertyException {
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
            throw new PropertyNotFoundException(
                "Property: " + propertyName + " could not be updated to: " + newValue
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
            logAndThrow("Could not transform backup properties xml: " + te.getMessage());
        } catch (IOException ioe) {
            logAndThrow("Could not backup configurable properties: " + ioe.getMessage());
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
     * Determine if the system is able to bypass configuration. If so, the system will look for
     * backup configuration files at startup time and use those to configure metacat. The bypass
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
                logAndThrow("Attempting to do bypass when system is not configured for it.");
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
     * Get the path to the directory where the site-specific properties (aka backup properties) are
     * stored
     *
     * @return java.nio.Path representation of the directory path
     * @throws GeneralPropertyException if there are issues retrieving or persisting the value
     */
    protected Path getSiteConfigDirPath() throws GeneralPropertyException {

        if (isBlankPath(siteConfigDirPath)) {
            if (!isBlankPath(sitePropertiesFilePath)) {
                siteConfigDirPath = sitePropertiesFilePath.getParent();
            }
            try {
                siteConfigDirPath = Paths.get(getProperty("application.backupDir"));
            } catch (InvalidPathException | PropertyNotFoundException e) {
                logMetacat.error(
                    "'getSitePropertiesPath(): application.backupDir' property not found: "
                        + e.getMessage());
            }
            if (isBlankPath(siteConfigDirPath)) {
                try {
                    String storedBackupDir = SystemUtil.getStoredBackupDir();
                    if (storedBackupDir != null) {
                        siteConfigDirPath = Paths.get(storedBackupDir);
                    }
                } catch (InvalidPathException | MetacatUtilException e) {
                    logMetacat.error(
                        "Problem calling SystemUtil.getStoredBackupDir(): " + e.getMessage(), e);
                }
            }
            if (isBlankPath(siteConfigDirPath)
                && PropertyService.getRecommendedExternalDir() != null) {
                try {
                    siteConfigDirPath = Paths.get(PropertyService.getRecommendedExternalDir(),
                        "." + ServiceService.getRealApplicationContext());
                } catch (InvalidPathException | ServiceException e) {
                    logMetacat.error("Problem getting site properties path from call to "
                            + "ServiceService.getRealApplicationContext(): "+ e.getMessage(), e);
                }
            }
            if (!isBlankPath(siteConfigDirPath)) {
                try {
                    setPropertyNoPersist("application.backupDir",
                        siteConfigDirPath.toString());
                    SystemUtil.writeStoredBackupFile(siteConfigDirPath.toString());
                } catch (GeneralPropertyException e) {
                    logMetacat.error(
                        "Problem trying to set property: 'application.backupDir' to value: "
                            + siteConfigDirPath, e);
                    throw e;
                } catch (MetacatUtilException e) {
                    logAndThrow("Problem calling SystemUtil.writeStoredBackupFile() with "
                        + "sitePropertiesPath: " + siteConfigDirPath + " -- Exception was: "
                        + e.getMessage());
                }
            } else {
                logAndThrow("CRITICAL: getSitePropertiesPath() Unable to find a suitable value "
                    + "for path to 'sitePropertiesPath' - site-specific properties & backups");
            }
        }
        return siteConfigDirPath;
    }

    protected void doRefresh() throws GeneralPropertyException {
        initialize();
    }

    protected Path getDefaultPropertiesFilePath() {
        return defaultPropertiesFilePath;
    }

    protected Path getSitePropertiesFilePath() {
        return sitePropertiesFilePath;
    }

    /**
     * Initialize the singleton.
     */
    private void initialize() throws GeneralPropertyException {

        logMetacat.debug("Initializing PropertiesWrapper");
        try {
            if (!isBlankPath(defaultPropertiesFilePath)) {
                if (!Files.exists(defaultPropertiesFilePath)) {
                    logAndThrow(
                        "PropertiesWrapper.initialize(): received non-existent Default Properties "
                            + "File Path: " + defaultPropertiesFilePath);
                }
            } else {
                defaultPropertiesFilePath =
                    Paths.get(PropertyService.CONFIG_FILE_DIR, "metacat.properties");
            }
            mainMetadataFilePath = PropertyService.CONFIG_FILE_DIR + FileUtil.getFS()
                + "metacat.properties.metadata.xml";

            String recommendedExternalDir = SystemUtil.discoverExternalDir();
            PropertyService.setRecommendedExternalDir(recommendedExternalDir);

            // defaultProperties will hold the default configuration from metacat.properties
            Properties defaultProperties = new Properties();
            defaultProperties.load(Files.newBufferedReader(defaultPropertiesFilePath));

            logMetacat.debug("PropertiesWrapper.initialize(): finished "
                + "loading metacat.properties into mainDefaultProperties");

            // mainProperties is the aggregation of the default props from metacat.properties...
            mainProperties = new Properties(defaultProperties);
            // ...overlaid with site-specific configurable properties from metacat-site.properties:
            if (!isBlankPath(sitePropertiesFilePath)) {
                if (!Files.exists(sitePropertiesFilePath)) {
                    logAndThrow(
                        "PropertiesWrapper.initialize(): non-existent Site Properties File Path: "
                            + sitePropertiesFilePath);
                }
            } else {
                sitePropertiesFilePath =
                    Paths.get(getSiteConfigDirPath().toString(), "metacat-site.properties");

                if (!Files.exists(sitePropertiesFilePath)) {
                    Files.createFile(
                        sitePropertiesFilePath); // performs atomic check-!exists-&-create
                    logMetacat.info(
                        "PropertiesWrapper.initialize(): no site properties file found; created: "
                            + sitePropertiesFilePath);
                }
            }
            mainProperties.load(Files.newBufferedReader(sitePropertiesFilePath));
            logMetacat.info("PropertiesWrapper.initialize(): populated mainProperties. Used "
                + defaultPropertiesFilePath + " as defaults; overlaid with "
                + sitePropertiesFilePath + " -- for a total of "
                + mainProperties.stringPropertyNames().size() + " Property entries");

            store(); //to persist new value of site properties path

            // include main metacat properties in d1 properties as overrides
            try {
                Settings.getConfiguration();
                //augment with BOTH properties files:
                Settings.augmentConfiguration(defaultPropertiesFilePath.toString());
                Settings.augmentConfiguration(sitePropertiesFilePath.toString());
            } catch (ConfigurationException e) {
                logMetacat.error("Could not augment DataONE properties. " + e.getMessage(), e);
            }

            // mainMetaData holds configuration information about main properties. This is primarily
            // used to display input fields on the configuration page. The information is retrieved
            // from an xml metadata file
            mainMetaData = new PropertiesMetaData(mainMetadataFilePath);

            String backupPath = getProperty("application.backupDir");
            if (StringUtils.isBlank(backupPath)) {
                backupPath = SystemUtil.getStoredBackupDir();
            }
            if (StringUtils.isBlank(backupPath) && recommendedExternalDir != null) {
                backupPath = recommendedExternalDir + FileUtil.getFS() + "."
                    + ServiceService.getRealApplicationContext();
            }

            if (StringUtils.isNotBlank(backupPath)) {
                setProperty("application.backupDir", backupPath);
                SystemUtil.writeStoredBackupFile(backupPath);

                // The mainBackupProperties hold properties that were backed up the last time the
                // application was configured. On disk, the file will look like a smaller version of
                // metacat.properties. It is located in the data storage directory, remote from the
                // application install directories.
                String MAIN_BACKUP_FILE_NAME = "metacat.properties.backup";
                mainBackupFilePath =
                    Paths.get(getSiteConfigDirPath().toString(), MAIN_BACKUP_FILE_NAME).toString();

                mainBackupProperties = new SortedProperties(mainBackupFilePath);
                mainBackupProperties.load();
            } else {
                // if backupPath is still null, can't write the backup properties. The system will
                // need to prompt the user for the properties and reconfigure
                logMetacat.error("");
            }
        } catch (TransformerException e) {
            logAndThrow("PropertiesWrapper.initialize(): problem loading PropertiesMetaData (from "
                + mainMetadataFilePath + ") -- " + e.getMessage());
        } catch (IOException e) {
            logAndThrow("PropertiesWrapper.initialize(): I/O problem while loading properties: "
                + e.getMessage());
        } catch (MetacatUtilException e) {
            logAndThrow("PropertiesWrapper.initialize(): Problem finding or writing backup file: "
                + e.getMessage());
        } catch (ServiceException e) {
            logAndThrow(
                "PropertiesWrapper.initialize(): problem calling getRealApplicationContext() -- "
                    + e.getMessage());
        }
    }

    /**
     * Provides a string representation of all the properties seen by the application at runtime,
     * in the format:
     * <ul>
     * <li>key1=property1</li>
     * <li>key2=property2</li>
     * <li>key3=property3</li>
     * </ul>
     * ...etc. If a problem is encountered when retrieving any of the properties, the output for
     * that key will be in the form:
     * (keyname)=EXCEPTION getting this property: (exception message)
     *
     * @return string representation of all the properties seen by the application at runtime
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String key : getPropertyNames()) {
            try {
                sb.append(key).append("=").append(getProperty(key)).append("\n");
            } catch (PropertyNotFoundException e) {
                sb.append(key).append("=").append("EXCEPTION getting this property: ")
                    .append(e.getMessage()).append("\n");
            }
        }
        return sb.toString();
    }

    private static void logAndThrow(String message) throws GeneralPropertyException {
        logMetacat.error(message);
        GeneralPropertyException gpe = new GeneralPropertyException(message);
        gpe.fillInStackTrace();
        throw gpe;
    }

    private boolean isBlankPath(Path path) {
        return path == null || path.toString().trim().equals("");
    }

    /**
     * store the properties to file. NOTE that this will persist only the properties that are: (a)
     * not included in mainDefaultProperties, or (b) have changed from the values in
     * mainDefaultProperties. From the <a
     * href="https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html">
     * java.util.Properties javadoc</a>: "Properties from the defaults table of this Properties
     * table (if any) are not written out by this method." Also, Properties class is thread-safe -
     * no need to synchronize.
     */
    private void store() throws GeneralPropertyException {
        try (Writer output = new FileWriter(sitePropertiesFilePath.toFile())) {
            mainProperties.store(output, null);
        } catch (IOException e) {
            logAndThrow("I/O exception trying to call mainProperties.store(): " + e.getMessage());
        }
    }
}
