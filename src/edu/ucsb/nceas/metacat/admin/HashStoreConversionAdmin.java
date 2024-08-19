package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader;
import edu.ucsb.nceas.metacat.admin.upgrade.UpgradeUtilityInterface;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.DatabaseUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Executors;

/**
 * An admin class to convert the old style file store to a HashStore
 * @author Tao
 */
public class HashStoreConversionAdmin extends MetacatAdmin {
    private final static String PROPERTY_PREFIX = "storage.upgradeUtility";
    private static Log logMetacat = LogFactory.getLog(HashStoreConversionAdmin.class);
    private static HashStoreConversionAdmin hashStoreConverter = new HashStoreConversionAdmin();
    protected static ListOrderedMap<String, String> finalVersionAndClassMap = null;
    protected static ListOrderedMap<String, String> versionAndClassMapInProperty = null;
    protected static String propertyPrefix = PROPERTY_PREFIX;
    protected static Vector<String> error = new Vector<>();
    protected static Vector<String> info = new Vector<>();

    /**
     * Default private constructor
     */
    private HashStoreConversionAdmin() {

    }

    /**
     * Get an instance of the converter
     * @return the instance of the converter
     */
    public static HashStoreConversionAdmin getInstance() {
        return hashStoreConverter;
    }

    /**
     * Convert to hashstore
     * @param request
     *            the http request information
     * @param response
     *            the http response to be sent back to the client
     */
    public void convert(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {
        logMetacat.debug("HashStoreConversionAdmin.convert - the start of the method");
        String processForm = request.getParameter("processForm");

        if (processForm != null && processForm.equals("true")) {
            logMetacat.debug("HashStoreConversionAdmin.convert - in the else routine to do the "
                                 + "conversion");
            try {
                // Do the job of conversion
                RequestUtil.clearRequestMessages(request);
                response.sendRedirect(SystemUtil.getContextURL() + "/admin");
                // Make the admin jsp page return by doing the upgrade in another thread
                Executors.newSingleThreadExecutor().submit(() -> {
                    convert();
                });
            } catch (GeneralPropertyException gpe) {
                throw new AdminException("QHashStoreConversionAdmin.convert - problem with "
                                             + "properties: "
                                             + gpe.getMessage());
            } catch (IOException e) {
                throw new AdminException("QHashStoreConversionAdmin.convert - problem with "
                                             + "redirect url: "
                                             + e.getMessage());
            }
        }
    }

    /**
     * The method do the conversion job
     */
    public static void convert() {
        logMetacat.debug("Start of the convert method...");
        String currentVersion = null;
        try {
            generateFinalVersionsAndClassesMap();
            for (String version : finalVersionAndClassMap.keyList()) {
                logMetacat.debug("Convert storage for the Metacat version " + version);
                currentVersion = version;
                if (getStatus() == UpdateStatus.IN_PROGRESS || getStatus() == UpdateStatus.UNKNOWN
                    || getStatus() == UpdateStatus.NOT_REQUIRED
                    || getStatus() == UpdateStatus.COMPLETE) {
                    logMetacat.debug("The current status is " + getStatus().getValue() + " and we"
                                         + " should NOT run the conversion.");
                    // Prevent doing upgrade again while another thread is doing the upgrade
                    return;
                }
                setStatus(currentVersion, UpdateStatus.IN_PROGRESS);
                String className = finalVersionAndClassMap.get(version);
                logMetacat.debug(
                    "Metacat will run the class " + className + " for version " + version
                        + " to upgrade the storage.");
                UpgradeUtilityInterface utility;
                try {
                    utility = (UpgradeUtilityInterface) Class.forName(className)
                        .getDeclaredConstructor().newInstance();
                } catch (IllegalAccessException ee) {
                    logMetacat.debug("SolrAdmin.updateSolrSchema - Metacat could not get a "
                                         + "instance by the constructor. It will try to the "
                                         + "method of getInstance.");
                    utility = (UpgradeUtilityInterface) Class.forName(className)
                        .getDeclaredMethod("getInstance").invoke(null);
                }
                utility.upgrade();
                setStatus(currentVersion, UpdateStatus.COMPLETE);
                if (utility instanceof HashStoreUpgrader) {
                    HashStoreUpgrader upgrader = (HashStoreUpgrader) utility;
                    String infoStr = upgrader.getInfo();
                    if (infoStr != null && !infoStr.isBlank()) {
                        addInfo(infoStr);
                    }
                }
            }
            setStatusForPreviousVersions();
        } catch (Exception e) {
            logMetacat.error(
                "Metacat can't convert the storage to hashstore since " + e.getMessage(), e);
            addError("Some errors arose when Metacat converted its storage to HashStore. Please "
                         + "fix the issues and convert it again.");
            addError(e.getMessage());
            if (currentVersion != null) {
                try {
                    setStatus(currentVersion, UpdateStatus.FAILED);
                } catch (AdminException ex) {
                    logMetacat.error("Can't change the Hashstore conversion status to "
                                         + "failed since " + ex.getMessage());
                }
            }
        } finally {
            // Reset finalVersionAndClassMap to null
            finalVersionAndClassMap = null;
        }
    }

    @Override
    protected Vector<String> validateOptions(HttpServletRequest request) {
        return new Vector<>();
    }

    /**
     * Set the status to not required for previous versions, which don't need the conversion
     */
    private static void setStatusForPreviousVersions() {
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection(
                "HashStoreConversionAdmin.setStatusForPreviousVersions");
            serialNumber = conn.getCheckOutSerialNumber();
            try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE version_history SET storage_upgrade_status=? WHERE storage_upgrade_status"
                    + " IS NULL AND version != '3.1.0'")) {
                pstmt.setObject(1, UpdateStatus.NOT_REQUIRED.getValue(), Types.OTHER);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logMetacat.error("Cannot save the status " + UpdateStatus.NOT_REQUIRED.getValue()
                                 + " into database since " + e.getMessage());
        } finally {
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
    }
    /**
     * Get the status of conversion. It is the combined status of different versions
     * @return the status. It can be pending, not required, complete, in_progress, unknown and
     * failed. The UNKNOWN status will be default one.
     * @AdminException
     */
    public static UpdateStatus getStatus() throws AdminException {
        // TODO: using a single status for all possible storage conversions for different versions
        // is not a good idea. So the admin gui should have a item for each version. However, in
        // near future, we will not have another. So it is a low priority.
        try {
            // The first admin page will show pending since we don't know anything about db
            if (!DatabaseUtil.isDatabaseConfigured()) {
                return UpdateStatus.UNKNOWN;
            }
            generateFinalVersionsAndClassesMap();
        } catch (MetacatUtilException | PropertyNotFoundException e) {
            throw new AdminException("Metacat cannot get status of the storage update since it "
                                         + e.getMessage());
        }
        UpdateStatus status = null;
        ListOrderedMap<String, String> versionAndClassMap;
        if (finalVersionAndClassMap == null || finalVersionAndClassMap.isEmpty()) {
            //No upgrade need, we determine the version from the map from the property file
            versionAndClassMap = versionAndClassMapInProperty;
        } else {
            // There is a upgrade, we determine the version from the finalVersionAndClassMap
            versionAndClassMap = finalVersionAndClassMap;
        }
        // We combine the status for all status of the different versions
        for (String version : versionAndClassMap.keyList()) {
            UpdateStatus versionStatus = getStatus(version);
            switch (versionStatus) {
                case UNKNOWN -> {
                    // If one version is UNKNOWN, the status of whole versions is UNKNOWN
                    return UpdateStatus.UNKNOWN;
                }
                case FAILED -> {
                    // If one version failed, the status of whole versions is failed
                    return UpdateStatus.FAILED;
                }
                case PENDING -> {
                    // If one version is pending, the status of whole versions is pending
                    return UpdateStatus.PENDING;
                }
                case IN_PROGRESS -> {
                    // If one version is in progress, the status of whole versions is in progress
                    return UpdateStatus.IN_PROGRESS;
                }
                case COMPLETE -> {
                    // When it gets here, this means this is first version, or the previous version
                    // has the status of complete or not required (since the other status already
                    // cause the return)
                    // We think complete will overwrite not required, so we assign the complete
                    // status
                    status = UpdateStatus.COMPLETE;
                }
                case NOT_REQUIRED -> {
                    // When it gets here, this means this is first version, or the previous version
                    // has the status of complete or not required (since the other status already
                    // cause the return)
                    if (status == null || status == UpdateStatus.NOT_REQUIRED) {
                        // handle the first version (status is null) or the previous version is not
                        // required
                        status = UpdateStatus.NOT_REQUIRED;
                    } else {
                        // The previous status is complete. Complete will overwrite not required
                        status = UpdateStatus.COMPLETE;
                    }
                }
                default -> throw new AdminException(
                    "Not recognized the upgrade status " + versionStatus.getValue());
            }
        }
        if (status == null) {
            status = UpdateStatus.UNKNOWN;
        }
        return status;
    }

    /**
     * Get the upgrade status for the given version
     * @param version  the version will check
     * @return the status. UNKNOWN will be the default
     * @throws AdminException
     */
    protected static UpdateStatus getStatus(String version) throws AdminException {
        DBConnection conn = null;
        int serialNumber = -1;
        UpdateStatus status = UpdateStatus.UNKNOWN;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("HashStoreConversionAdmin.getStatus");
            serialNumber = conn.getCheckOutSerialNumber();
            try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT storage_upgrade_status FROM version_history WHERE version=?")) {
                pstmt.setString(1, version);
                try (ResultSet resultSet = pstmt.executeQuery()) {
                    if (resultSet.next()) {
                        String statusStr =resultSet.getString(1);
                        if (statusStr != null && !statusStr.isBlank()) {
                            logMetacat.debug("The status of storage_upgrade_status from the db is"
                                                 + statusStr);
                            status = UpdateStatus.getStatus(statusStr);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new AdminException("Cannot get the status from the db for version " + version
                                         + " since " + e.getMessage());
        } finally {
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
        return status;
    }

    /**
     * Check if the hashstore was converted
     * @return true if the status is complete; otherwise false.
     * @AdminException
     */
    public static boolean isConverted() throws AdminException {
        return (getStatus() == UpdateStatus.COMPLETE || getStatus() == UpdateStatus.NOT_REQUIRED);
    }

    /**
     * Get the error message if the conversion fails
     * @return the error message
     */
    public static Vector<String> getError() {
        return error;
    }

    /**
     * Get the information that some conversion failed
     * @return the information
     */
    public static Vector<String> getInfo() {
        return info;
    }

    protected static void setStatus(String version, UpdateStatus status) throws AdminException {
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("HashStoreConversionAdmin.setStatus");
            serialNumber = conn.getCheckOutSerialNumber();
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE version_history SET "
                + "storage_upgrade_status=? " + "WHERE version=?")) {
                pstmt.setObject(1, status.getValue(), Types.OTHER);
                pstmt.setString(2, version);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new AdminException(
                "Cannot save the status " + status.getValue() + " into " + "database since "
                    + e.getMessage());
        } finally {
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
    }

    private static void addError(String errorMessage) {
        error.add(errorMessage);
    }

    private static void addInfo(String infoStr) {
        info.add("The HashStore conversion was done. However, some objects may not succeed. You "
                     + "have to manually fix the issues.");
        info.add(infoStr);
    }

    /**
     * Generate the final version-class ordered map which contains the versions and upgrade classes.
     * This map removed the upgrade classes which are not in the db upgrade range from the
     * candidate classes. If there is no db upgrade, it will use the versionAndClass map from the
     * property file and take a look the database - choose the one whose upgrade status is
     * pending, unknown and failed
     * @throws PropertyNotFoundException
     * @throws AdminException
     * @throws MetacatUtilException
     */
    protected static void generateFinalVersionsAndClassesMap()
        throws PropertyNotFoundException, AdminException, MetacatUtilException {
        //only run it when dbconfig is finished
        if (!DatabaseUtil.isDatabaseConfigured()) {
            return;
        }
        if (finalVersionAndClassMap == null || finalVersionAndClassMap.isEmpty()) {
            finalVersionAndClassMap = new ListOrderedMap<>();
            Vector<String> neededUpgradeVersionsForDB = DBAdmin.getNeededUpgradedVersions();
            initVersionAndClassFromProperty();
            if (neededUpgradeVersionsForDB != null && !neededUpgradeVersionsForDB.isEmpty()) {
                logMetacat.debug("There was a Metacat version upgrade in the configuration. So we"
                                     + " will combine the DB upgrade version range and the storage "
                                     + "class name from the property file");
                Set<String> versionsInProperty = versionAndClassMapInProperty.keySet();
                int index = 0;
                if (versionsInProperty != null) {
                    for (String version : neededUpgradeVersionsForDB) {
                        // handle a fresh installation case
                        if (version.equals(DBAdmin.VERSION_000)) {
                            logMetacat.debug(
                                "The Metacat instance is a fresh installation of " + SystemUtil
                                    .getMetacatVersion().getVersionString()
                                    + " and it will set the storage conversion "
                                    + UpdateStatus.NOT_REQUIRED.getValue());
                            setStatus(
                                SystemUtil.getMetacatVersion().getVersionString(),
                                UpdateStatus.NOT_REQUIRED);
                            return;
                        }
                        if (versionsInProperty.contains(version)) {
                            String className = versionAndClassMapInProperty.get(version);
                            boolean success = addVersionToFinalMap(version, index, className);
                            if (success) {
                                index++;
                            }
                        } else {
                            setStatus(version, UpdateStatus.NOT_REQUIRED);
                            logMetacat.debug(
                                "Add version " + version + " as the status "
                                    + UpdateStatus.NOT_REQUIRED.getValue()
                                    + " for the prefix " + propertyPrefix);
                        }
                    }
                }
            } else {
                logMetacat.debug("There was NO Metacat DB version upgrade in the configuration. So"
                                     + " we will use the version and the storage class name from "
                                     + "the property file, whose status is pending, unknown or "
                                     + "failed");
                List<String> versionsInProperty = versionAndClassMapInProperty.keyList();
                int index = 0;
                if (versionsInProperty != null) {
                    for (String version : versionsInProperty) {
                        String className = versionAndClassMapInProperty.get(version);
                        boolean success = addVersionToFinalMap(version, index, className);
                        if (success) {
                            index++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Add the version and className to the finalVersionAndClassMap at the given index position if
     * they are appropriate
     * @param version  the version will be added
     * @param index  the position in the finalVersionAndClassMap
     * @param className  the class name will be added
     * @return true if they were added; otherwise false
     * @throws AdminException
     */
    private static boolean addVersionToFinalMap(String version, int index, String className)
        throws AdminException {
        if (version != null && !version.isBlank() && className != null && !className.isBlank()) {
            // Only the status of unknown, pending and failure will be added.
            UpdateStatus status = getStatus(version);
            if (status == UpdateStatus.UNKNOWN || status == UpdateStatus.PENDING
                || status == UpdateStatus.FAILED) {
                finalVersionAndClassMap.put(index, version, className);
                // Initialize to the pending status
                setStatus(version, UpdateStatus.PENDING);
                logMetacat.debug(
                    "Add version " + version + " and class " + className
                        + " into the upgrade classes map with the index " + index
                        + " for the prefix " + propertyPrefix);
                return true;
            } else {
                logMetacat.debug("The status of the version " + version + " is "
                                     + status.getValue() + " so the " + className
                                     + " will NOT be added into the upgrade "
                                     + "classes map for the prefix " + propertyPrefix);
            }
        }
        return false;
    }

    /**
     * Initialize the map between version and the update class name from the metacat.properties file
     * @throws PropertyNotFoundException
     */
    protected static void initVersionAndClassFromProperty()
        throws PropertyNotFoundException {
        if (versionAndClassMapInProperty == null) {
            versionAndClassMapInProperty = new ListOrderedMap<>();
            Map<String, String> classNames = PropertyService.getPropertiesByGroup(propertyPrefix);
            int index = 0;
            if (classNames != null) {
                for (String propertyName : classNames.keySet()) {
                    String version = getVersionFromPropertyName(propertyName, propertyPrefix);
                    versionAndClassMapInProperty.put(index, version, classNames.get(propertyName));
                    index++;
                    logMetacat.debug(
                        "Add version " + version + " and the upgrader class name" + classNames.get(
                            propertyName) + " into " + "the candidate map");
                }
            }
        }
    }

    /**
     * Get the version from the update class property name with the convention like:
     * solr.upgradeUtility.3.0.0
     *
     * @param propertyName the property name like solr.upgradeUtility.3.0.0
     * @param prefix the prefix before the version part such as solr.upgradeUtility
     * @return the version part
     */
    protected static String getVersionFromPropertyName(String propertyName, String prefix) {
        return propertyName.substring((prefix + ".").length());
    }

}
