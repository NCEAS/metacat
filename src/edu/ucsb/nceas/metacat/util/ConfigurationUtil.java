package edu.ucsb.nceas.metacat.util;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.HashStoreConversionAdmin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.dbadapter.AbstractDatabase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.FileUtil;

/**
 * A suite of utility classes for the metadata catalog server
 */
public class ConfigurationUtil {

    public static AbstractDatabase dbAdapter;

    private static Log logMetacat = LogFactory.getLog(ConfigurationUtil.class);

    /**
     * Reports whether metacat is fully configured.
     *
     * @return a boolean that is true if all sections are configured and
     * false otherwise
     */
    public static boolean isMetacatConfigured() {
        boolean metacatConfigured = false;
        try {
            metacatConfigured = PropertyService.arePropertiesConfigured()
                    && AuthUtil.isAuthConfigured()
                    && DatabaseUtil.isDatabaseConfigured()
                    && isBackupDirConfigured()
                    && DataONEConfigUtil.isDataOneConfigured()
                    && isEZIDConfigured()
                    && HashStoreConversionAdmin.isConverted();
        } catch (MetacatUtilException ue) {
            logMetacat.error("Could not determine if metacat is configured due to utility exception: "
                    + ue.getMessage());
        } catch (GeneralPropertyException gpe) {
            logMetacat.error("Could not determine if metacat is configured due to property exception: "
                    + gpe.getMessage());
        } catch (AdminException e) {
            logMetacat.error("Could not determine if metacat is configured due to the admin "
                                 + "exception: " + e.getMessage());
        }

        return metacatConfigured;
    }

    /**
     * Check if the application.backupDir property is populated in
     * metacat.properties and that it points to a writable directory.
     *
     * @return false if the application.backupDir property does not point to a
     *         writable directory.
     */
    public static boolean isBackupDirConfigured() throws MetacatUtilException, PropertyNotFoundException {
        String backupDir = PropertyService.getProperty("application.backupDir");
        if (backupDir == null || backupDir.equals("")) {
            return false;
        }
        if (FileUtil.getFileStatus(backupDir) < FileUtil.EXISTS_READ_WRITABLE) {
            return false;
        }
        return true;
    }

    public static boolean isEZIDConfigured() throws MetacatUtilException {
        String ezidConfiguredString = PropertyService.UNCONFIGURED;
        try {
            ezidConfiguredString = PropertyService.getProperty("configutil.ezidConfigured");
        } catch (PropertyNotFoundException pnfe) {
            throw new MetacatUtilException("Could not determine if the ezid service are configured: "
                    + pnfe.getMessage());
        }
        // geoserver is configured if not unconfigured
        return !ezidConfiguredString.equals(PropertyService.UNCONFIGURED);
    }

    /**
     * Reports whether the metacat configuration utility should be run. Returns
     * false if
     *   -- dev.runConfiguration=false and
     *   -- backup properties file exists
     * Note that dev.runConfiguration should only be set to false when reinstalling the
     * same version of the application in development.
     *
     * @return a boolean that is false if dev.runConfiguration is false and the
     *         backup properties file exists.
     */
    public static boolean bypassConfiguration() throws MetacatUtilException, ServiceException {
        try {
            // If the system is not configured to do bypass, return false.
            if (!PropertyService.doBypass()) {
                return false;
            }

            // Get the most likely backup files.  If these cannot be found, we
            // cannot do the configuration bypass.
            String ExternalBaseDir = SystemUtil.discoverExternalDir();
            if (ExternalBaseDir == null) {
                logMetacat.error("bypassConfiguration: Could not find backup directory.");
                // could not find backup files ... force the configuration
                return false;
            }
            String realContext = ServiceService.getRealApplicationContext();
            PropertyService.setRecommendedExternalDir(ExternalBaseDir);
            PropertyService.setProperty("application.backupDir",
                    ExternalBaseDir + FileUtil.getFS() + "." + realContext);

            // Refresh the property service and skin property service.  This will pick up
            // the backup directory and populate backup properties in caches.
            ServiceService.refreshService("PropertyService");
            ServiceService.refreshService("SkinPropertyService");

            // Call bypassConfiguration to make sure backup properties get persisted
            // to active properties for both main and skin properties.
            PropertyService.bypassConfiguration();
            SkinPropertyService.bypassConfiguration();

            return true;
        } catch (GeneralPropertyException gpe) {
            throw new MetacatUtilException("Property error while discovering backup directory: "
                    + gpe.getMessage());
        } catch (MetacatUtilException mue) {
            throw new MetacatUtilException("Utility error while discovering backup directory: "
                    + mue.getMessage());
        }

    }

}
