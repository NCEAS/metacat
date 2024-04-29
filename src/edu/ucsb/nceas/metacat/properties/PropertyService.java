/**
 * Purpose: A Service that provides access to configuration properties methods for metacat.
 */

package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Vector;

/**
 * A suite of utility classes for the metadata configuration utility
 */
public class PropertyService extends BaseService {
    // system is configured
    public static final String CONFIGURED = "true";
    // system has never been configured
    public static final String UNCONFIGURED = "false";
    public static final String BYPASSED = "bypassed";
    public static final String SITE_PROPERTIES_FILENAME = "metacat-site.properties";
    public static final String SITE_PROPERTIES_DIR_PATH_KEY = "application.sitePropertiesDir";
    private static final String DEFAULT_CONFIG_FILE_DIR = "WEB-INF";
    private static final Log logMetacat = LogFactory.getLog(PropertyService.class);

    /**
     * The directory holding properties and other configuration files
     */
    public static String CONFIG_FILE_DIR = null;

    private static PropertyService propertyService = null;
    private static PropertiesWrapper properties = null;
    private static AuthPropertiesDelegate authPropertiesDelegate = null;
    private static BackupPropertiesDelegate backupPropertiesDelegate = null;
    private static String RECOMMENDED_EXTERNAL_DIR = null;

    /**
     * private constructor since this is a singleton
     *
     * @param defaultPropertiesFilePath (Can be null) Full path to the default properties file (e.g.
     *                                  /var/lib/tomcat/webapps/WEB-INF/metacat.properties)
     * @param sitePropertiesFilePath    (Can be null) Full path to the site properties file (e.g.
     *                                  /var/metacat/config/metacat-site.properties)
     * @throws ServiceException if initialization of Delegate objects fails
     */
    private PropertyService(Path defaultPropertiesFilePath, Path sitePropertiesFilePath)
        throws ServiceException {
        _serviceName = "PropertyService";
        try {
            initialize(defaultPropertiesFilePath, sitePropertiesFilePath);
        } catch (GeneralPropertyException e) {
            ServiceException se = new ServiceException(e.getMessage());
            se.fillInStackTrace();
            throw se;
        }
    }

    /**
     * Get the single instance of PropertyService.
     *
     * @param context a reference to the ServletContext
     * @return the single instance of PropertyService
     * @throws ServiceException if initialization of new instance fails
     */
    public static PropertyService getInstance(ServletContext context) throws ServiceException {
        if (propertyService == null) {
            String applicationName = (String) context.getAttribute("APPLICATION_NAME");
            CONFIG_FILE_DIR = context.getInitParameter("configFileDir");
            if (CONFIG_FILE_DIR == null) {
                CONFIG_FILE_DIR = context.getRealPath(DEFAULT_CONFIG_FILE_DIR);
            }
            Path defaultPropsPath = Paths.get(CONFIG_FILE_DIR, applicationName + ".properties");
            propertyService = new PropertyService(defaultPropsPath, null);
        }
        return propertyService;
    }

    /**
     * Get the single instance of PropertyService for test purposes. In this case, we allow the
     * configuration directory to be passed in.
     *
     * @param testConfigFileDir the test configuration directory we need to look in
     * @return the single instance of PropertyService
     * @throws ServiceException if initialization of new instance fails
     */
    public static PropertyService getInstance(String testConfigFileDir) throws ServiceException {
        if (propertyService == null) {
            CONFIG_FILE_DIR = testConfigFileDir;
            propertyService = new PropertyService(null, null);
        }
        return propertyService;
    }

    /**
     * Get the single instance of PropertyService for test purposes. In this case, we allow passing
     * in the paths (relative to the working directory) to both the default properties file (e.g.
     * lib/metacat.properties) and the site-specific (or in this case, the test-specific) properties
     * file (eg test/test.properties) to be overlaid on top of those defaults.
     *
     * @param testDefaultPropertiesFilePath the path to the default properties file (relative to the
     *                                      working directory)
     * @param testSitePropertiesFilePath    the path to the site-specific properties overlay file
     *                                      (relative to the working directory)
     * @return the single instance of PropertyService
     * @throws ServiceException if initialization of new instance fails
     */
    public static PropertyService getInstanceForTesting(Path testDefaultPropertiesFilePath,
        Path testSitePropertiesFilePath) throws ServiceException {
        if (propertyService == null) {
            CONFIG_FILE_DIR = testDefaultPropertiesFilePath.getParent().toString();
            logMetacat.info(
                "PropertyService.getInstance() received " + "testDefaultPropertiesFilePath: "
                    + testDefaultPropertiesFilePath + " & testSitePropertiesFilePath: "
                    + testSitePropertiesFilePath + "; setting CONFIG_FILE_DIR to: "
                    + CONFIG_FILE_DIR);
            propertyService =
                new PropertyService(testDefaultPropertiesFilePath, testSitePropertiesFilePath);
        }
        return propertyService;
    }

    /**
     * Get the single instance of PropertyService, only  after it has been previously instantiated
     * with a call to one of the other getInstance() methods passing either a reference to the
     * servlet context or the config file path
     *
     * @return the single instance of PropertyService
     * @throws ServiceException if there was an attempt to call getInstance() without parameters,
     *                          before a PropertyService instance has first been created with either
     *                          a servlet context or config file path.
     */
    public static PropertyService getInstance() throws ServiceException {
        if (propertyService == null) {
            ServiceException se = new ServiceException("PropertyService.getInstance() - cannot "
                + "call getInstance without parameters until property service has been created "
                + "with either servlet context or config file path.");
            se.fillInStackTrace();
            throw se;
        }
        return propertyService;
    }

    /**
     * Get a property value.
     * 1. First check if the property has been overridden by an environment variable (override
     *    mappings are defined in the properties files as: "application.envSecretKeys")
     * 2. if no environment variable override is found, then return the regular value from the
     *    properties files, if it has been set. Otherwise, throw a PropertyNotFoundException
     *
     * @param propertyName the name of the property requested
     * @return the String value for the property, even if blank. Will never return null
     * @throws PropertyNotFoundException if the passed <code>propertyName</code> key is not in the
     *                                   properties at all
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
     * @throws PropertyNotFoundException if the passed <code>groupName</code> key is not in the
     *                                   properties at all
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
     * @throws GeneralPropertyException if there are problems manipulating the required properties
     */
    public static void setProperty(String propertyName, String newValue)
        throws GeneralPropertyException {
        properties.setProperty(propertyName, newValue);
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
     * @throws GeneralPropertyException if there are problems manipulating the required properties
     */
    public static void setPropertyNoPersist(String propertyName, String newValue)
        throws GeneralPropertyException {
        properties.setPropertyNoPersist(propertyName, newValue);
    }

    /**
     * Save the properties to a properties file on disk.
     *
     * @throws GeneralPropertyException if there are problems manipulating the properties
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
    public static SortedProperties getMainBackupProperties() {
        return backupPropertiesDelegate.getMainBackupProperties();
    }

    /**
     * Get the auth backup properties file. These are configurable properties that are stored
     * outside the metacat install directories so the user does not need to re-enter all the
     * configuration information every time they do an upgrade.
     *
     * @return a SortedProperties object with the backup properties
     */
    public static SortedProperties getAuthBackupProperties() {
        return authPropertiesDelegate.getAuthBackupProperties();
    }

    /**
     * Get the main properties metadata. This is retrieved from an xml file that describes the
     * attributes of configurable properties.
     *
     * @return a PropertiesMetaData object with the main properties metadata
     */
    public static PropertiesMetaData getMainMetaData() {
        return properties.getMainMetaData();
    }

    /**
     * Get the auth properties metadata. This is retrieved from an xml file that describes the
     * attributes of configurable properties.
     *
     * @return a PropertiesMetaData object with the organization properties metadata
     */
    public static PropertiesMetaData getAuthMetaData() {
        return authPropertiesDelegate.getAuthPropertiesMetadata();
    }

    /**
     * Writes out configurable properties to a backup file outside the metacat install directory,
     * so they are not lost if metacat installation is overwritten during an upgrade. These backup
     * properties are used by the admin page to populate defaults when the configuration is edited.
     * (They are also used to overwrite the main properties if bypassConfiguration() is called)
     *
     * @throws GeneralPropertyException if there are problems manipulating the required properties
     */
    public static void persistMainBackupProperties() throws GeneralPropertyException {
        backupPropertiesDelegate.persistMainBackupProperties();
    }

    /**
     * Writes out configurable properties to a backup file outside the metacat install directory,
     * so they are not lost if metacat installation is overwritten during an upgrade. These backup
     * properties are used by the admin page to populate defaults when the configuration is edited.
     * (They are also used to overwrite the main properties if bypassConfiguration() is called)
     *
     * @throws GeneralPropertyException if there are problems manipulating the required properties
     */
    public static void persistAuthBackupProperties() throws GeneralPropertyException {
        authPropertiesDelegate.persistAuthBackupProperties();
    }

    /**
     * Reports whether properties are fully configured, by checking the value of the
     * property "configutil.propertiesConfigured", which is set from the Admin flow.
     *
     * @return a boolean that is true if properties are configured and false otherwise
     * @throws GeneralPropertyException if there are problems getting the
     *                                  "configutil.propertiesConfigured" property
     * @see edu.ucsb.nceas.metacat.admin.PropertiesAdmin
     */
    public static boolean arePropertiesConfigured() throws GeneralPropertyException {
        String propertiesConfigured =
                PropertyService.getProperty("configutil.propertiesConfigured");
        return propertiesConfigured != null && propertiesConfigured.equals("true");
    }

    /**
     * Determine if the system is able to bypass configuration. If so, the system will look for
     * backup or "site-specific" configuration files at startup time and use those to configure
     * metacat. The bypass options should only be set by developers. Production code should never
     * bypass configuration.
     *
     * @return true if dev.runConfiguration is set to true in metacat.properties AND we have not
     * already checked for bypass; false otherwise.
     * @throws GeneralPropertyException if there are problems manipulating the required properties
     */
    public static boolean doBypass() throws GeneralPropertyException {
        return backupPropertiesDelegate.canBypass();
    }

    /**
     * (for dev use only) Bypasses the properties configuration utility by using the backup
     * properties to overwrite the main properties.
     */
    public static void bypassConfiguration() {
        logMetacat.debug("bypassConfiguration(): setting main backup properties.");
        backupPropertiesDelegate.bypassConfiguration();
        logMetacat.debug("bypassConfiguration(): setting auth backup properties.");
        authPropertiesDelegate.bypassAuthConfiguration();
    }

    /**
     * Take input from the user in an HTTP request about a property to be changed and update the
     * metacat property file with that new value if it has changed from the value that was
     * originally set.
     *
     * @param request      that was generated by the user
     * @param propertyName the name of the property to be checked and set
     */
    // TODO: MB - can we get rid of this? AFAICT, a simple "setProperty" call should suffice
    @Deprecated
    public static void checkAndSetProperty(HttpServletRequest request, String propertyName)
        throws GeneralPropertyException {
        String value = getProperty(propertyName);
        String newValue = propertyName == null ? null : request.getParameter(propertyName);
        if (newValue != null && !newValue.trim().equals(value)) {
            setPropertyNoPersist(propertyName, newValue.trim());
        }
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
                    + e.getMessage(), e);
            }
        }
        return RECOMMENDED_EXTERNAL_DIR;
    }

    /**
     * Sets the recommended external directory. This is populated during initialization time using
     * the SystemUtil.discoverExternalDir() method. This directory will be used to suggest external
     * user directories when the user configures metacat for the first time.
     */
    public static void setRecommendedExternalDir(String extBaseDir) {
        RECOMMENDED_EXTERNAL_DIR = extBaseDir;
    }

    /**
     * The properties on the dataONE Setting class isn't synchronized with changes to the Metacat
     * properties files. This method synchronizes (reloads) the properties' changes to the Settings
     * class, and should be called whenever the property files are modified.
     * @throws GeneralPropertyException if there's a problem calling Settings.augmentConfiguration()
     */
    public static void syncToSettings() throws GeneralPropertyException {
        properties.syncToSettings();
    }

    /**
     *  Get the path to the main Metadata file, which holds configuration information about main
     *  properties. This is primarily used to display input fields on the configuration page. The
     *  information is retrieved from an xml metadata file
     */
    public static Path getMainMetadataFilePath() {
        return properties.getMainMetadataFilePath();
    }

    /**
     * Get the path to the directory where the backup properties are stored, and as a
     * side effect, update the properties file to save this path as "application.backupDir"
     *
     * @return java.nio.Path representation of the directory path
     * @throws GeneralPropertyException if there are issues retrieving or persisting the value
     */
    public static Path getBackupDirPath() throws GeneralPropertyException {
        return backupPropertiesDelegate.getBackupDirPath();
    }

    @Override
    public boolean refreshable() {
        return true;
    }

    @Override
    public void doRefresh() throws ServiceException {
        try {
            properties.doRefresh();
        } catch (GeneralPropertyException e) {
            ServiceException se = new ServiceException(e.getMessage());
            se.fillInStackTrace();
            throw se;
        }
    }

    @Override
    public void stop() throws ServiceException {
    }

    /**
     * Initialize the singleton.
     */
    private void initialize(Path defaultPropertiesFilePath, Path sitePropertiesFilePath)
        throws GeneralPropertyException {

        logMetacat.debug("Initializing PropertyService");
        properties =
            PropertiesWrapper.getNewInstance(defaultPropertiesFilePath, sitePropertiesFilePath);

        backupPropertiesDelegate = BackupPropertiesDelegate.getInstance();

        authPropertiesDelegate = AuthPropertiesDelegate.getInstance();

        logMetacat.debug(
            "\n* * * PropertyService.initialize() finished. Properties contents: * * *\n\n"
                + properties.toString());
    }
}
