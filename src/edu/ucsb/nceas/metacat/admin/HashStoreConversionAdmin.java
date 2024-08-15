package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader;
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
    private static Log logMetacat = LogFactory.getLog(HashStoreConversionAdmin.class);
    private static HashStoreConversionAdmin hashStoreConverter = new HashStoreConversionAdmin();

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
                if (getStatus().equals(MetacatAdmin.IN_PROGRESS)) {
                    // Prevent doing upgrade again while another thread is doing the upgrade
                    return;
                }
                setStatus(MetacatAdmin.IN_PROGRESS);
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
        try {
            HashStoreUpgrader upgrader = new HashStoreUpgrader();
            upgrader.upgrade();
            String infoStr = upgrader.getInfo();
            if (infoStr != null && !infoStr.isBlank()) {
                setInfo(infoStr);
            }
            try {
                setStatus(PropertyService.CONFIGURED);
            } catch (AdminException ex) {
                logMetacat.error("Can't change the Hashstore conversion status to "
                                     + " done(true) since " + ex.getMessage());
            }
        } catch (Exception e) {
            addError("Some errors arose when Metacat converted its storage to HashStore. Please "
                         + "fix the issues and convert it again.");
            addError(e.getMessage());
            try {
                setStatus(MetacatAdmin.FAILURE);
            } catch (AdminException ex) {
                logMetacat.error("Can't change the Hashstore conversion status to "
                                     + "failure since " + ex.getMessage());
            }
        }
    }

    @Override
    protected Vector<String> validateOptions(HttpServletRequest request) {
        return new Vector<>();
    }

    /**
     * Get the status of conversion
     * @return the status. It can be converted, unconverted, in_progress and failed
     * @AdminException
     */
    public static String getStatus() throws AdminException {
        try {
            return PropertyService.getProperty("storage.hashstoreConverted");
        } catch (PropertyNotFoundException e) {
            throw new AdminException("Metacat cannot get the status of the hashstore conversion "
                                         + "since " + e.getMessage());
        }
    }

    /**
     * Check if the hashstore was converted
     * @return true if the status is true; otherwise false.
     * @AdminException
     */
    public static boolean isConverted() throws AdminException {
        return getStatus().equals(PropertyService.CONFIGURED);
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

    private static void setStatus(String status) throws AdminException {
        try {
            PropertyService.setProperty("storage.hashstoreConverted", status);
        } catch (GeneralPropertyException e) {
            throw new AdminException("Metacat cannot set the status " + status + " for Hashtore "
                                         + "conversion since " + e.getMessage());
        }
    }

    private static void addError(String errorMessage) {
        error.add(errorMessage);
    }

    private static void setInfo(String infoStr) {
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
     */
    public static ListOrderedMap<String, String> getUpdateClasses(String propertyPrefix)
        throws PropertyNotFoundException {
        ListOrderedMap<String, String> versionsAndClasses = new ListOrderedMap<>();
        Vector<String> neededupgradeVersions = DBAdmin.getNeededUpgradedVersions();
        Map<String, String> candidates = getCandidateUpdateClasses(propertyPrefix);
        Set<String> candidateVersions = candidates.keySet();
        int index = 0;
        if (candidateVersions != null) {
            for (String version : neededupgradeVersions) {
                if (candidateVersions.contains(version)) {
                    versionsAndClasses.put(index, version, candidates.get(version));
                    logMetacat.debug(
                        "Add version " + version + " and class " + candidates.get(version)
                            + " into the upgrade classes map with the index " + index
                            + " for the prefix " + propertyPrefix);
                    index++;
                }
            }
        }
        return versionsAndClasses;
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
