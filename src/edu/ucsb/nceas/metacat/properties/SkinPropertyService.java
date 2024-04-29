package edu.ucsb.nceas.metacat.properties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SkinUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
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

    private static Log logMetacat = LogFactory.getLog(SkinPropertyService.class);

    /**
     * private constructor since this is a singleton
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
     * @return List of property names
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
    public static SortedProperties getBackupProperties(String skinName) {
        return skinBackupPropertiesMap.get(skinName);
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