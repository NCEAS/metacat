package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader;
import edu.ucsb.nceas.metacat.admin.upgrade.UpgradeUtilityInterface;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
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
import java.util.HashMap;
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
    // The classes will be run during the upgrade process. Please keep null as its initial value.
    protected static ListOrderedMap<String, String> versionsAndClasses = null;
    private static Vector<String> error = new Vector<>();
    private static Vector<String> info = new Vector<>();

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

        if (processForm == null && processForm.equals("true")) {
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
        String currentVersion = null;
        try {
            getUpdateClasses(PROPERTY_PREFIX);
            for (String version : versionsAndClasses.keyList()) {
                currentVersion = version;
                if (getStatus() == UpdateStatus.IN_PROGRESS
                    || getStatus() == UpdateStatus.COMPLETE) {
                    // Prevent doing upgrade again while another thread is doing the upgrade
                    return;
                }
                setStatus(currentVersion, UpdateStatus.IN_PROGRESS);
                String className = versionsAndClasses.get(version);
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
        }
    }

    @Override
    protected Vector<String> validateOptions(HttpServletRequest request) {
        return new Vector<>();
    }

    /**
     * Get the status of conversion. It is the combined status of different versions
     * @return the status. It can be pending, not required, complete, in_progress and failed
     * @AdminException
     */
    public static UpdateStatus getStatus() throws AdminException {
        // The first admin page will show pending since we don't know anything about db
        if (versionsAndClasses == null) {
            return UpdateStatus.PENDING;
        }
        UpdateStatus status = null;
        for (String version : versionsAndClasses.keyList()) {
            UpdateStatus versionStatus = getStatus(version);
            switch (versionStatus) {
                case FAILED -> {
                    // If one failed, the whole process is failed
                    return UpdateStatus.FAILED;
                }
                case PENDING -> {
                    // If one is pending, the whole process is pending
                    return UpdateStatus.PENDING;
                }
                case IN_PROGRESS -> {
                    // If one is in progress, the whole process is in progress
                    return UpdateStatus.IN_PROGRESS;
                }
                case COMPLETE -> {
                    //complete will overwrite not required
                    status = UpdateStatus.COMPLETE;
                }
                case NOT_REQUIRED -> {
                    if (status == null || status == UpdateStatus.NOT_REQUIRED) {
                        status = UpdateStatus.NOT_REQUIRED;
                    } else {
                        // This is the complete case. Complete will overwrite not required
                        status = UpdateStatus.COMPLETE;
                    }
                }
                default -> throw new AdminException(
                    "Not recognized the upgrade status " + versionStatus.getValue());
            }
        }
        if (status == null) {
            status = UpdateStatus.PENDING;
        }
        return status;
    }

    /**
     * Get the upgrade status for the given version
     * @param version  the version will check
     * @return the status. PENDING will be the default
     * @throws AdminException
     */
    protected static UpdateStatus getStatus(String version) throws AdminException {
        DBConnection conn = null;
        int serialNumber = -1;
        UpdateStatus status = UpdateStatus.PENDING;
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

    private static void setStatus(String version, UpdateStatus status) throws AdminException {
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
     * Get the ordered map which contains the versions and upgrade classes. This map removed the
     * upgrade classes which are not in the db upgrade range from the candidate classes.
     * @param propertyPrefix  it determines which process such solr or storage conversion
     * @return the map which contains the versions and upgrade classes
     * @throws PropertyNotFoundException
     * @throws AdminException
     */
    protected static void getUpdateClasses(String propertyPrefix)
        throws PropertyNotFoundException, AdminException {
        //only run once
        if (versionsAndClasses == null) {
            versionsAndClasses = new ListOrderedMap<>();
            Vector<String> neededupgradeVersions = DBAdmin.getNeededUpgradedVersions();
            Map<String, String> candidates = getCandidateUpdateClasses(propertyPrefix);
            Set<String> candidateVersions = candidates.keySet();
            int index = 0;
            if (candidateVersions != null) {
                for (String version : neededupgradeVersions) {
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
                    if (candidateVersions.contains(version)) {
                        versionsAndClasses.put(index, version, candidates.get(version));
                        setStatus(version, UpdateStatus.PENDING);// Initialize to pending status
                        logMetacat.debug(
                            "Add version " + version + " and class " + candidates.get(version)
                                + " into the upgrade classes map with the index " + index
                                + " for the prefix " + propertyPrefix);
                        index++;
                    }
                }
            }
        }
    }
    /**
     * Get the map between version and the update class name from the metacat.properties file
     * @param propertyPrefix  the property prefix such as solr.upgradeUtility
     * @return the map between version and the update class name;
     * @throws PropertyNotFoundException
     */
    protected static Map<String, String> getCandidateUpdateClasses(String propertyPrefix)
        throws PropertyNotFoundException {
        Map<String, String> versionClassNames = new HashMap<>();
        Map<String, String> classNames = PropertyService.getPropertiesByGroup(propertyPrefix);
        if (classNames != null) {
            for (String propertyName : classNames.keySet()) {
                String version = getVersionFromPropertyName(propertyName, propertyPrefix);
                versionClassNames.put(version, classNames.get(propertyName));
                logMetacat.debug(
                    "Add version " + version + " and the upgrader class name" + classNames.get(
                        propertyName) + " into " + "the candidate map");
            }
        }
        return versionClassNames;
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
