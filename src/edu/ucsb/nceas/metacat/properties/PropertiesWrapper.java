package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;

import javax.xml.transform.TransformerException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * A Class that provides a wrapper around standard java.util.Properties to provide backwards
 * compatibility with metacat's original properties implementation
 */
public class PropertiesWrapper {

    private static final Log logMetacat = LogFactory.getLog(PropertiesWrapper.class);
    private static PropertiesWrapper propertiesWrapper = null;

    // Full Path to the default properties file
    // Example: /var/lib/tomcat/webapps/WEB-INF/metacat.properties
    private static Path defaultPropertiesFilePath = null;

    // Full Path to the site-specific properties file to be overlaid on top of the defaults
    // Example: /var/metacat/config/metacat-site.properties
    private static Path sitePropertiesFilePath = null;
    private Path mainMetadataFilePath = null;
    private PropertiesMetaData mainMetaData = null;

    // Composite of site-specific properties overlaid on top of default properties
    private Properties mainProperties = null;

    // default properties only; not including any site-specific properties
    protected Properties defaultProperties = null;

    // Contains mappings of properties.key=METACAT_ENV_KEY, used to retrieve secret credentials
    // from environment variables
    private final Properties envSecretKeyMappings = new Properties();

    /**
     * TODO: Eliminate the need for this list! It is here for legacy reasons;
     *       See GitHub Issue #1638: https://github.com/NCEAS/metacat/issues/1638
     * Whenever setPropertyNoPersist() is called, the code checks to see if the passed propertyName
     * parameter is listed in 'writeableDefaults'. If so, the property is added directly to
     * 'defaultProperties' instead of to 'mainProperties'. It will therefore end up being written
     * to the 'metacat.properties' file instead of the 'metacat-site.properties' file.
     * This breaks the design intent of the Properties setup, and has been done as a temporary
     * workaround preceding incremental improvements.
     * DO NOT ADD ANY MORE KEYS TO THIS LIST! - IT WILL BE DELETED when the code is refactored to
     * to handle upgrades across deployments for k8s and legacy installations. Basically, the
     * metacat.properties file should never need to be writeable. If you find yourself wanting to
     * write to it, there must be a better solution.
     */
    protected static final List<String> WRITEABLE_DEFAULTS = Arrays.asList(
        PropertyService.SITE_PROPERTIES_DIR_PATH_KEY,
        "configutil.propertiesConfigured",
        "configutil.authConfigured",
        "configutil.skinsConfigured",
        "configutil.databaseConfigured",
        "configutil.solrserverConfigured",
        "configutil.dataoneConfigured",
        "configutil.ezidConfigured",
        "configutil.quotaConfigured",
        "configutil.upgrade.status",
        "configutil.upgrade.database.status",
        "configutil.upgrade.java.status",
        "configutil.upgrade.solr.status"
    );

    /**
     * private constructor since this is a singleton
     */
    private PropertiesWrapper() throws GeneralPropertyException {
        initialize();
    }

    /**
     * Provides a string representation of all the properties seen by the application at runtime, in
     * the format:
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

    /**
     * get the current instance of the PropertiesWrapper. To set specific property file locations .
     * use the <code>getNewInstance(Path, Path)</code> method.
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
     * re-initialize PropertiesWrapper with the newly-passed values of defaultPropertiesFilePath and
     * sitePropertiesFilePath.</em> If you just want to get an instance without affecting its
     * internals, call <code>getinstance()</code> instead.
     *
     * @param defaultPropertiesFilePath (Can be null) Full path to the default properties file (e.g.
     *                                  /var/lib/tomcat/webapps/WEB-INF/metacat.properties)
     * @param sitePropertiesFilePath    (Can be null) Full path to the site properties file (e.g.
     *                                  /var/metacat/config/metacat-site.properties)
     * @return singleton instance of PropertiesWrapper
     * @throws GeneralPropertyException when things go wrong
     */
    protected static PropertiesWrapper getNewInstance(Path defaultPropertiesFilePath,
        Path sitePropertiesFilePath) throws GeneralPropertyException {

        PropertiesWrapper.defaultPropertiesFilePath = defaultPropertiesFilePath;
        PropertiesWrapper.sitePropertiesFilePath = sitePropertiesFilePath;
        propertiesWrapper = new PropertiesWrapper();
        return propertiesWrapper;
    }

    /**
     * Get a property value.
     * 1. First check if the property has been overridden by an environment variable (override
     *    mappings are defined in the properties files as: "application.envSecretKeys")
     * 2. if no environment variable override is found, then return the regular value from the
     *    properties files, if it has been set. Otherwise, throw a PropertyNotFoundException
     * @param propertyName the name of the property requested
     * @return the String value for the property, even if blank. Will never return null
     * @throws PropertyNotFoundException if the passed <code>propertyName</code> key is not in the
     *                                   properties at all
     */
    protected String getProperty(String propertyName) throws PropertyNotFoundException {

        String returnVal = null;
        if (envSecretKeyMappings.containsKey(propertyName)) {
            String envKey = envSecretKeyMappings.getProperty(propertyName);
            returnVal = System.getenv(envKey);
            if (returnVal == null || returnVal.trim().isEmpty()) {
                logMetacat.info("Didn't find env secret: " + envKey
                                    + "; falling back to property: " + propertyName);
            } else {
                logMetacat.info(
                    "Using env var secret: " + envKey + " to override property: " + propertyName);
            }
        }
        if (returnVal == null || returnVal.trim().isEmpty()) {
            returnVal = mainProperties.getProperty(propertyName);
        }
        if (returnVal == null) {  // getProperty() returning an empty string is valid
            logMetacat.warn("did not find the property with key " + propertyName);
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
     * @throws PropertyNotFoundException if the passed <code>groupName</code> key is not in the
     *                                   properties at all
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
     * Utility method to set a property value both in memory and to the properties file
     *
     * @param propertyName the name of the property requested
     * @param newValue     the new value for the property
     * @throws GeneralPropertyException if there's a problem setting or storing the property
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
     * @throws GeneralPropertyException if the property wasn't found, or there's a problem
     *                                  responding to a change in the property
     *                                  "application.sitePropertiesDir"
     */
    protected void setPropertyNoPersist(String propertyName, String newValue)
        throws GeneralPropertyException {
        if (null == propertyName || null == newValue) {
            throw new GeneralPropertyException(
                "Illegal argument - at least one of params was NULL! key = " + propertyName
                    + "; value = " + newValue);
        }
        propertyName = propertyName.trim();
        if (null == mainProperties.getProperty(propertyName)) {
            // TODO: MB - can we get rid of this? Default java.util.Properties behavior is
            //  to add a new entry if it doesn't already exist, when setProperty() is called
            throw new PropertyNotFoundException(
                "Property: " + propertyName + " could not be updated to: " + newValue
                    + " because it does not already exist in properties.");
        }
        if (WRITEABLE_DEFAULTS.contains(propertyName)) {
            // TODO: Eliminate the need for setting default props! This is here for legacy reasons;
            //       See GitHub Issue #1638: https://github.com/NCEAS/metacat/issues/1638
            //       and see comment for WRITEABLE_DEFAULTS constant
            if (propertyName.equals(PropertyService.SITE_PROPERTIES_DIR_PATH_KEY)) {
                changeSitePropsPath(newValue);
            }
            defaultProperties.setProperty(propertyName, newValue);
            // Don't re-use/copy the above code - it needs to go away. The default properties
            // (i.e. those in the metacat.properties file) should never need to be set explicitly.
        } else {
            mainProperties.setProperty(propertyName, newValue);
        }
    }

    /**
     * Save the properties to the metacat-site.properties file.
     *
     * @throws GeneralPropertyException if an I/O Exception is encountered
     */
    protected void persistProperties() throws GeneralPropertyException {
        store();
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
     * implementation of abstract method in edu.ucsb.nceas.metacat.shared.BaseService, delegated
     * to by PropertyService, which extends BaseService
     *
     * @throws GeneralPropertyException if there is a problem with initialization
     */
    protected void doRefresh() throws GeneralPropertyException {
        initialize();
    }

    /**
     *  Get the path to the read-only, default properties in metacat.properties.
     */
    protected Path getDefaultPropertiesFilePath() {
        return defaultPropertiesFilePath;
    }

    /**
     *  Get the path to the metacat-site.properties file, which holds configurable properties used
     *  to override the defaults in metacat.properties.
     */
    protected Path getSitePropertiesFilePath() {
        return sitePropertiesFilePath;
    }

    /**
     *  Get the path to the main Metadata file, which holds configuration information about main
     *  properties. This is primarily used to display input fields on the configuration page. The
     *  information is retrieved from an xml metadata file
     */
    protected Path getMainMetadataFilePath() {
        return mainMetadataFilePath;
    }

    /**
     * The properties on the dataONE Setting class aren't synchronized with changes to the Metacat
     * properties files. This method synchronizes (reloads) the properties' changes to the Settings
     * class, and should be called whenever the property files are modified.
     *
     * NOTE that secrets passed as environment variables are accessible only by code that calls
     *  PropertyService.getProperty(). Calling syncToSettings() does NOT make them accessible via
     *  org.dataone.configuration.Settings. (See inline code comment for suggestions on how to
     *  implement, if this becomes necessary in future)
     * @throws GeneralPropertyException if there's a problem calling Settings.augmentConfiguration()
     */
    protected void syncToSettings() throws GeneralPropertyException {
        try {
            Settings.getConfiguration();
            Settings.augmentConfiguration(this.getDefaultPropertiesFilePath().toString());
            Settings.augmentConfiguration(this.getSitePropertiesFilePath().toString());
            // Calling syncToSettings() does NOT make secrets passed as environment variables
            // accessible via org.dataone.configuration.Settings.
            // If it becomes necessary to implement this in the future, see the getConfiguration()
            // method in the Setting class (as called above). It returns the object configuration
            // which holds the property values. We can call the method setProperty to add the new
            // key/value pairs for the env secrets.
            // However, it's not clear if setProperty will store the key/value pairs into a file
            // (which is a security issue we're trying to avoid by using env vars) - so this
            // needs more investigation. Here is the documentation for the method:
            // https://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration2/AbstractConfiguration.html#setProperty(java.lang.String,java.lang.Object)
        } catch (ConfigurationException e) {
            logAndThrow("PropertiesWrapper.syncToSettings(): augmenting DataONE settings: ", e);
        }
    }

    private void changeSitePropsPath(String newDir) throws GeneralPropertyException {
        // check if the proposed new site props dir already contains a properties file.
        // if yes, then just write the newDir property to defaultProperties
        // if no, then move the file from the old to the new location,
        // and then write the props to defaultProperties.
        // Then in both cases, update the sitePropertiesFilePath
        newDir = newDir.trim();
        Path newSitePropsPath = Paths.get(newDir, PropertyService.SITE_PROPERTIES_FILENAME);

        if (newSitePropsPath.equals(sitePropertiesFilePath)) { // check for no-op
            logMetacat.info(
                "Site properties file path has NOT been changed; new value matches old value: "
                    + newSitePropsPath);
        } else {
            if (Files.exists(newSitePropsPath)) {
                logMetacat.info(
                    "Site properties file already exists at new location (" + newSitePropsPath);
                //try to rename old file  to avoid confusion
                if (Files.isWritable(sitePropertiesFilePath)) {
                    Path oldFilePath = Paths.get(sitePropertiesFilePath.getParent().toString(),
                        PropertyService.SITE_PROPERTIES_FILENAME + ".OLD");
                    try {
                        Files.move(sitePropertiesFilePath, oldFilePath);
                        logMetacat.info("Old Site properties file renamed to: " + oldFilePath);
                    } catch (IOException e) {
                        logMetacat.error("Problem renaming old Site Properties file from: "
                            + sitePropertiesFilePath + " to: " + oldFilePath);
                        //don't rethrow - the rename is just a best-effort nice-to-have
                    }
                }
            } else {
                // move the file from the old to the new location
                try {
                    Files.createDirectories(newSitePropsPath.getParent()); // no-op if existing
                    Files.move(sitePropertiesFilePath, newSitePropsPath);
                    logMetacat.debug(
                        "Moved existing site properties file from: " + sitePropertiesFilePath
                            + " to: " + newSitePropsPath);
                } catch (IOException e) {
                    logAndThrow("Problem moving site properties file from " + sitePropertiesFilePath
                        + " to " + newSitePropsPath, e);
                }
            }
            sitePropertiesFilePath = newSitePropsPath;

            logMetacat.debug(
                "updated sitePropertiesFilePath from " + sitePropertiesFilePath + " to "
                    + newSitePropsPath
                    + " and added new directory location to in-memory DEFAULT properties: " + newDir);
        }
    }

    /**
     * Initialize the singleton.
     */
    private void initialize() throws GeneralPropertyException {

        logMetacat.debug("Initializing PropertiesWrapper");
        try {
            if (isNotBlankPath(defaultPropertiesFilePath)) {
                // a path to the default properties file was provided before initialization
                if (!Files.exists(defaultPropertiesFilePath)) {
                    logAndThrow(
                        "PropertiesWrapper.initialize(): received non-existent Default Properties "
                            + "File Path: " + defaultPropertiesFilePath, null);
                }
            } else {
                defaultPropertiesFilePath =
                    Paths.get(PropertyService.CONFIG_FILE_DIR, "metacat.properties");
            }
            String recommendedExternalDir = SystemUtil.discoverExternalDir();
            PropertyService.setRecommendedExternalDir(recommendedExternalDir);

            // defaultProperties will hold the default configuration from metacat.properties
            defaultProperties = new Properties();
            defaultProperties.load(Files.newBufferedReader(defaultPropertiesFilePath));

            logMetacat.debug("PropertiesWrapper.initialize(): finished "
                + "loading metacat.properties into defaultProperties");

            // mainProperties comprises (1) the default properties loaded from metacat.properties...
            mainProperties = new Properties(defaultProperties);

            // (2)...overlaid with site-specific properties from metacat-site.properties:
            initSitePropertiesFilePath();
            mainProperties.load(Files.newBufferedReader(sitePropertiesFilePath));
            logMetacat.info("PropertiesWrapper.initialize(): populated mainProperties. Used "
                + defaultPropertiesFilePath + " as defaults; overlaid with "
                + sitePropertiesFilePath + " -- for a total of "
                + mainProperties.stringPropertyNames().size() + " Property entries");

            initializeSecretsFromEnvVars();

            // include main metacat properties in d1 properties as overrides
            syncToSettings();

            // mainMetaData holds configuration information about main properties. This is primarily
            // used to display input fields on the configuration page. The information is retrieved
            // from an xml metadata file
            mainMetadataFilePath =
                Paths.get(PropertyService.CONFIG_FILE_DIR, "metacat.properties.metadata.xml");
            mainMetaData = new PropertiesMetaData(mainMetadataFilePath.toString());

        } catch (TransformerException e) {
            logAndThrow("PropertiesWrapper.initialize(): problem loading PropertiesMetaData (from "
                + mainMetadataFilePath + ")", e);
        } catch (IOException e) {
            logAndThrow("PropertiesWrapper.initialize(): I/O problem while loading properties: ",
                e);
        } catch (MetacatUtilException e) {
            logAndThrow("PropertiesWrapper.initialize(): SystemUtil.discoverExternalDir()", e);
        }
    }

    /**
     * From the property 'application.envSecretKeys', retrieve the colon-delimited mappings
     * between: camelCase / period-delimited properties keys (as found elsewhere in the
     * properties files), and SCREAMING_SNAKE_CASE environment variable keys. Then use these to
     * populate the envSecretKeyMappings Properties, used elsewhere to check for overrides when
     * getProperty() is called.
     * The property file entry should be in the form:
     * <code>
     *   application.secretKeys=         \
     *     :some.CamelCase=METACAT_VAR_A \
     *     :another.key=METACAT_VAR_B    \
     *     (...etc.)
     * </code>
     */
    private void initializeSecretsFromEnvVars() {

        String secretMappingsStr = mainProperties.getProperty("application.envSecretKeys");
        final String ENTRY_DELIMITER = ":";
        final String KEY_VAL_DELIMITER = "=";
        if (secretMappingsStr != null && !secretMappingsStr.isEmpty()) {
            String[] mappingList = secretMappingsStr.split(ENTRY_DELIMITER);
            for (String eachMapping : mappingList) {
                String[] keyValPair = eachMapping.split(KEY_VAL_DELIMITER);
                String key = keyValPair[0].trim();
                String value = keyValPair[1].trim();
                if (key.isEmpty() || value.isEmpty()) {
                    logMetacat.error(
                        "Badly-formed entry in 'application.envSecretKeys' property: ("
                            + key + ") = (" + value + "); skipping");
                    continue;
                }
                logMetacat.info("envSecretKeyMappings: If env var: " + value
                        + " is found later, its secret will be assigned to property: " + key);
                envSecretKeyMappings.setProperty(key, value);
            }
        } else {
            logMetacat.warn("No values found for 'application.envSecretKeys' in properties; "
                                 + "secrets will NOT be retrieved from environment variables");
            // no overrides specified, OK to log a warning and then ignore, since overrides optional
        }
    }

    private void initSitePropertiesFilePath() throws GeneralPropertyException {

        if (isNotBlankPath(sitePropertiesFilePath)) {
            logMetacat.debug("PropertiesWrapper.initSitePropertiesFilePath(): already set: "
                + sitePropertiesFilePath);
            if (!Files.exists(sitePropertiesFilePath)) {
                logAndThrow("PropertiesWrapper received non-existent Site Properties File Path: "
                    + sitePropertiesFilePath, null);
            }
        } else {
            //NOTE THIS ONLY READS *DEFAULT* PROPS; SITE PROPS NOT YET LOADED/OVERLAID!!
            // If the path has NOT been edited, it will be the default "/var/metacat/config" in
            // metacat.properties.
            // If the path HAS been edited, it should have been written through to the default
            // properties, to overwrite the original value - so we should be OK either way...
            sitePropertiesFilePath =
                Paths.get(getProperty(PropertyService.SITE_PROPERTIES_DIR_PATH_KEY),
                    PropertyService.SITE_PROPERTIES_FILENAME);

            if (!Files.exists(sitePropertiesFilePath)) {
                //EITHER:
                // (1) it's the first run, so there are no site props,
                // OR
                // (2) the site props exists and has already been stored at a path different from
                // the default, but this is a re-installation that has overwritten the changes in
                // metacat.properties
                //
                // For both cases, create an empty file, since none exists at this path. User will
                // need to reconfigure anyway, thus setting real path to site props for case (2)
                try {
                    Files.createDirectories(
                        sitePropertiesFilePath.getParent()); // no-op if already existing
                    Files.createFile(
                        sitePropertiesFilePath); // does atomic recheck-not-exists-&-create

                } catch (FileAlreadyExistsException e) {
                    logAndThrow("PropertiesWrapper.initSitePropertiesFilePath(): "
                        + "problem creating directory hierarchy for site properties file at: "
                        + sitePropertiesFilePath + "; one of these directories already exists, but"
                        + " is not a directory (i.e. there's something else in the way!)", e);

                } catch (UnsupportedOperationException e) {
                    logAndThrow("PropertiesWrapper.initSitePropertiesFilePath(): "
                        + "problem creating directory hierarchy for site properties file at: "
                        + sitePropertiesFilePath + "; could not set attributes for one of these "
                        + "directories", e);

                } catch (SecurityException e) {
                    logAndThrow("PropertiesWrapper.initSitePropertiesFilePath(): "
                        + "a security manager is installed and has denied checkPropertyAccess "
                        + "permission while creating directories or site properties file at: "
                        + sitePropertiesFilePath, e);

                } catch (IOException e) {
                    logAndThrow("PropertiesWrapper.initSitePropertiesFilePath(): a general I/O "
                        + "error occurred while trying to create directory hierarchy or actual "
                        + "file for site properties at: " + sitePropertiesFilePath
                        + "; see exception message for details", e);
                }
                logMetacat.info(
                    "PropertiesWrapper.initSitePropertiesFilePath(): no site properties file "
                        + "found; new one created at: " + sitePropertiesFilePath);
            }
        }
    }

    private static void logAndThrow(String message, Exception e) throws GeneralPropertyException {
        String excepMsg = (e == null) ? "" : " : " + e.getMessage();
        GeneralPropertyException gpe = new GeneralPropertyException(message + excepMsg);
        gpe.fillInStackTrace();
        logMetacat.error(message, (e == null) ? gpe : e);
        throw gpe;
    }

    /**
     * Store the properties to file. NOTE that this will persist only the properties that are:
     * (a) not included in defaultProperties, or (b) have changed from the values in
     * defaultProperties, by writing them to the metacat-site.properties file.
     * From the <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html">
     * java.util.Properties javadoc</a>: "Properties from the defaults table of this Properties
     * table (if any) are not written out by this method." Also, Properties class is thread-safe -
     * no need to synchronize.
     *
     * As a side effect, the metacat.properties file is also written back to disk. This is needed
     * for legacy reasons, and will be removed when the relevant code is refactored.
     * See GitHub Issue #1638: https://github.com/NCEAS/metacat/issues/1638 and also
     * @see PropertiesWrapper.WRITEABLE_DEFAULTS
     *
     * @throws GeneralPropertyException if an I/O Exception is encountered
     */
    private void store() throws GeneralPropertyException {
        try (Writer output = new FileWriter(sitePropertiesFilePath.toFile())) {
            mainProperties.store(output, null);
        } catch (IOException e) {
            logAndThrow("I/O exception trying to call mainProperties.store(): ", e);
        }
        // TODO: Eliminate the need for storing default props! It is here for legacy reasons;
        //       See GitHub Issue #1638: https://github.com/NCEAS/metacat/issues/1638
        //       and see comment for WRITEABLE_DEFAULTS constant
        try (Writer output = new FileWriter(defaultPropertiesFilePath.toFile())) {
            defaultProperties.store(output, null);
        } catch (IOException e) {
            logAndThrow("I/O exception trying to call defaultProperties.store(): ", e);
        }
        // Don't re-use/copy the above code - it needs to go away. The metacat.properties file
        // should never need to be written to.
    }

    private boolean isNotBlankPath(Path path) {
        return path != null && !path.toString().trim().equals("");
    }
}
