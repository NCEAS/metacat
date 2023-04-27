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
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;

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
     * Get a property value from the properties file.
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
     * @param value        the value for the property
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
        return backupPropertiesDelegate.getMainBackupProperties();
    }

    /**
     * Get the auth backup properties file. These are configurable properties that are stored
     * outside the metacat install directories so the user does not need to re-enter all the
     * configuration information every time they do an upgrade.
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
        backupPropertiesDelegate.persistMainBackupProperties();
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
     * @return returns true if all properties are configured, and false otherwise
     */
    public static boolean arePropertiesConfigured() throws GeneralPropertyException {
        return backupPropertiesDelegate.arePropertiesConfigured();
    }

    /**
     * Determine if the system is able to bypass configuration. If so, the system will look for
     * backup or "site-specific" configuration files at startup time and use those to configure
     * metacat. The bypass options should only be set by developers. Production code should never
     * bypass configuration.
     *
     * @return true if dev.runConfiguration is set to true in metacat.properties AND we have not
     * already checked for bypass; false otherwise.
     */
    public static boolean doBypass() throws GeneralPropertyException {
        return backupPropertiesDelegate.canBypass();
    }

    /**
     * Bypasses the metacat properties configuration utility (for dev use only).
     */
    public static void bypassConfiguration() throws GeneralPropertyException {
        logMetacat.debug("bypassConfiguration(): setting main backup properties.");
        backupPropertiesDelegate.bypassConfiguration();
        logMetacat.debug("bypassConfiguration(): setting auth backup properties.");
        authPropertiesDelegate.bypassAuthConfiguration(properties);
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
     * class, and shoudl be called whenever the property files are modified.
     */
    public static void syncToSettings() throws GeneralPropertyException {
        try {
            Settings.getConfiguration();
            Settings.augmentConfiguration(properties.getDefaultPropertiesFilePath().toString());
            Settings.augmentConfiguration(properties.getSitePropertiesFilePath().toString());
        } catch (ConfigurationException e) {
            GeneralPropertyException gpe = new GeneralPropertyException(e.getMessage());
            gpe.fillInStackTrace();
            throw gpe;
        }
    }

    /**
     * Get the DEFAULT property value from the default properties file. Ignore any overriding
     * values in the site properties file
     *
     * @param propertyName the name of the DEFAULT property requested
     * @return the String value for the DEFAULT property, even if blank, or null if the property key
     * is not found
     */
    public static String getDefaultProperty(String propertyName) {
        return properties.getDefaultProperty(propertyName);
    }

    public static Path getMainMetadataFilePath() {
        return properties.getMainMetadataFilePath();
    }

    public static Path getBackupDirPath() throws GeneralPropertyException {
        return backupPropertiesDelegate.getBackupDirPath();
    }

    public boolean refreshable() {
        return true;
    }

    public void doRefresh() throws ServiceException {
        try {
            properties.doRefresh();
        } catch (GeneralPropertyException e) {
            ServiceException se = new ServiceException(e.getMessage());
            se.fillInStackTrace();
            throw se;
        }
    }

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
