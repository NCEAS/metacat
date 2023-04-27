package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.MetaDataProperty;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class BackupPropertiesDelegate {
    static boolean bypassAlreadyChecked = false;
    // Full Path to the DIRECTORY containing the configuration BACKUP files (such as
    // metacat.properties.backup etc.). Example: /var/metacat/.metacat
    static Path backupDirPath;
    private static SortedProperties mainBackupProperties = null;

    private static BackupPropertiesDelegate instance = null;
    String mainBackupFilePath = null;
    private static final Log logMetacat = LogFactory.getLog(PropertiesWrapper.class);


    private BackupPropertiesDelegate()
        throws GeneralPropertyException {

        if (Files.exists(getBackupDirPath())) {
            try {
                // write the file to ~/.metacat that contains the real path to the backup directory
                SystemUtil.writeStoredBackupFile(getBackupDirPath().toString());
            } catch (MetacatUtilException e) {
                logAndThrow("BackupPropertiesDelegate(): Problem writing the file that contains "
                    + "the real path to the backup directory: " + getBackupDirPath(), e);
            }

            // The mainBackupProperties hold properties that were backed up the last time the
            // application was configured. On disk, the file will look like a smaller version of
            // metacat.properties. It is located in the data storage directory, remote from the
            // application install directories.
            String MAIN_BACKUP_FILE_NAME = "metacat.properties.backup";
            mainBackupFilePath =
                Paths.get(getBackupDirPath().toString(), MAIN_BACKUP_FILE_NAME).toString();

            mainBackupProperties = new SortedProperties(mainBackupFilePath);
            try {
                mainBackupProperties.load();
            } catch (IOException e) {
                logAndThrow("BackupPropertiesDelegate(): Problem loading backup properties: "
                    + mainBackupFilePath, e);            }
        } else {
            // if backupPath is still null, can't write the backup properties. The system will
            // need to prompt the user for the properties and reconfigure
            logMetacat.error("Backup Path not available; can't write the backup properties: "
                + getBackupDirPath());
        }
    }

    protected static BackupPropertiesDelegate getInstance()
        throws GeneralPropertyException {
        if (instance == null) {
            instance = new BackupPropertiesDelegate();
        }
        return instance;
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
     * Writes out backup configurable properties to a file.
     */
    protected void persistMainBackupProperties() throws GeneralPropertyException {

        // Use the metadata to extract configurable properties from the
        // overall properties list, and store those properties.
        try {
            SortedProperties backupProperties = new SortedProperties(mainBackupFilePath);

            // Populate the backup properties for main metacat properties using
            // the associated metadata file
            PropertiesMetaData mainMetadata =
                new PropertiesMetaData(PropertyService.getMainMetadataFilePath().toString());

            Map<String, MetaDataProperty> mainKeyMap = mainMetadata.getProperties();
            Set<String> mainKeySet = mainKeyMap.keySet();
            for (String propertyKey : mainKeySet) {
                // don't backup passwords
                MetaDataProperty metaData = mainKeyMap.get(propertyKey);
                if (!metaData.getFieldType().equals(MetaDataProperty.PASSWORD_TYPE)) {
                    backupProperties.addProperty(propertyKey,
                        PropertyService.getProperty(propertyKey));
                }
            }

            // store the properties to file
            backupProperties.store();
            mainBackupProperties = new SortedProperties(mainBackupFilePath);
            mainBackupProperties.load();

        } catch (TransformerException te) {
            logAndThrow("Could not transform backup properties xml: ", te);
        } catch (IOException ioe) {
            logAndThrow("Could not backup configurable properties: ", ioe);
        }
    }

    /**
     * Reports whether properties are fully configured.
     *
     * @return a boolean that is true if properties are configured and false otherwise
     */
    protected boolean arePropertiesConfigured() throws GeneralPropertyException {
        String propertiesConfigured =
            PropertyService.getProperty("configutil.propertiesConfigured");
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
            logMetacat.debug(
                "canBypass() returning false, since already previously checked");
        } else {
            // check how dev.runConfiguration is set in metacat.properties
            String strRunConfiguration = PropertyService.getProperty("dev.runConfiguration");
            logMetacat.debug(
                "canBypass(): 'dev.runConfiguration property' set to: " + strRunConfiguration);
            boolean runConfiguration = Boolean.parseBoolean(strRunConfiguration);

            // MB: seems like this should be `bypassAlreadyChecked = true`, now we've checked it,
            // but this was the original logic, so leaving as-is for now...
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
                logAndThrow(
                    "Attempting to do bypass when system is not configured for it.", null);
            }
            // The system is bypassing the configuration utility. We need to
            // get the backup properties and replace existing properties with
            // backup values.  We do this for main and org properties.
            logMetacat.debug(
                "bypassConfiguration: setting main backup properties.");
            SortedProperties mainBackupProperties = getMainBackupProperties();
            Vector<String> backupPropertyNames = mainBackupProperties.getPropertyNames();
            for (String backupPropertyName : backupPropertyNames) {
                String value = mainBackupProperties.getProperty(backupPropertyName);
                PropertyService.setPropertyNoPersist(backupPropertyName, value);
            }
            logMetacat.debug(
                "bypassConfiguration: setting configutil sections to true.");
            PropertyService.setPropertyNoPersist("configutil.propertiesConfigured", "true");
            PropertyService.setPropertyNoPersist("configutil.authConfigured", "true");
            PropertyService.setPropertyNoPersist("configutil.skinsConfigured", "true");
            PropertyService.setPropertyNoPersist("configutil.databaseConfigured", "true");
            PropertyService.setPropertyNoPersist("configutil.geoserverConfigured", "bypassed");

            PropertyService.persistProperties();

        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error(
                "bypassConfiguration: Could not find property: " + pnfe.getMessage());
        } catch (GeneralPropertyException gpe) {
            logMetacat.error(
                "bypassConfiguration: General property error: " + gpe.getMessage());
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
    protected Path getBackupDirPath() throws GeneralPropertyException {

        if (isBlankPath(backupDirPath)) {
            try {
                backupDirPath = Paths.get(PropertyService.getProperty("application.backupDir"));
            } catch (InvalidPathException | PropertyNotFoundException e) {
                logMetacat.error(
                    "'getSitePropertiesPath(): application.backupDir' property not found: "
                        + e.getMessage());
            }
            if (isBlankPath(backupDirPath)) {
                try {
                    String storedBackupDir = SystemUtil.getStoredBackupDir();
                    if (storedBackupDir != null) {
                        backupDirPath = Paths.get(storedBackupDir);
                    }
                } catch (InvalidPathException | MetacatUtilException e) {
                    logMetacat.error(
                        "Problem calling SystemUtil.getStoredBackupDir(): " + e.getMessage(), e);
                }
            }
            if (isBlankPath(backupDirPath)
                && PropertyService.getRecommendedExternalDir() != null) {
                try {
                    backupDirPath = Paths.get(PropertyService.getRecommendedExternalDir(),
                        "." + ServiceService.getRealApplicationContext());
                } catch (InvalidPathException | ServiceException e) {
                    logMetacat.error(
                        "Problem getting site properties path from call to "
                            + "ServiceService.getRealApplicationContext(): " + e.getMessage(), e);
                }
            }
            if (!isBlankPath(backupDirPath)) {
                try {
                    PropertyService.setPropertyNoPersist("application.backupDir",
                        backupDirPath.toString());
                    SystemUtil.writeStoredBackupFile(backupDirPath.toString());
                } catch (GeneralPropertyException e) {
                    logMetacat.error(
                        "Problem trying to set property: 'application.backupDir' to value: "
                            + backupDirPath, e);
                    throw e;
                } catch (MetacatUtilException e) {
                    logAndThrow(
                        "Problem calling SystemUtil.writeStoredBackupFile() with "
                            + "sitePropertiesPath: " + backupDirPath, e);
                }
            } else {
                logAndThrow(
                    "CRITICAL: getSitePropertiesPath() Unable to find a suitable backup directory",
                    null);
            }
        }
        return backupDirPath;
    }

    private boolean isBlankPath(Path path) {
        return path == null || path.toString().trim().equals("");
    }

    private static void logAndThrow(String message, Exception e) throws GeneralPropertyException {
        String excepMsg = (e == null) ? "" : " : " + e.getMessage();
        GeneralPropertyException gpe = new GeneralPropertyException(message + excepMsg);
        gpe.fillInStackTrace();
        logMetacat.error(message, (e == null) ? gpe : e);
        throw gpe;
    }
}
