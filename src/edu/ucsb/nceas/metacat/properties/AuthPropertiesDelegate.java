package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.MetaDataProperty;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.SortedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * AuthPropertiesDelegate handles Authentication-related properties for metacat configuration. Since it is a singleton
 * class that has a private constructor, a shared instance of AuthPropertiesDelegate can be obtained by a call to the
 * static method AuthPropertiesDelegate.getInstance().
 *
 * However, PropertyService should be the only class instantiating AuthPropertiesDelegate and calling its methods. When
 * PropertyService receives Authentication-related method calls, they are simply "passed through" and delegated directly
 * to the corresponding methods in AuthPropertiesDelegate. This pattern was used to improve encapsulation, separation of
 * concerns, readability, and reasoning about how the codebase works.
 *
 * @see edu.ucsb.nceas.metacat.properties.PropertyService
 */
public class AuthPropertiesDelegate {
    protected static final String AUTH_METADATA_FILE_NAME = "auth.properties.metadata.xml";
    protected static final String AUTH_BACKUP_FILE_NAME = "auth.properties.backup";
    protected static String authMetadataFilePath = null;
    protected static PropertiesMetaData authPropertiesMetadata = null;
    protected static String authBackupFilePath = null;
    protected static SortedProperties authBackupProperties = null;
    private static AuthPropertiesDelegate authPropertiesDelegate;
    private static final Log logMetacat = LogFactory.getLog(AuthPropertiesDelegate.class);


    /**
     * Private constructor, since this is a Singleton - so we only allow getting a shared
     * instance through the static getInstance() method
     *
     * @throws GeneralPropertyException if an XML TransformerException or an IOException are thrown
     *                                  when trying to load the auth-related properties
     */
    private AuthPropertiesDelegate() throws GeneralPropertyException {

        authMetadataFilePath =
            PropertyService.CONFIG_FILE_DIR + FileUtil.getFS() + AUTH_METADATA_FILE_NAME;
        try {
            // authPropertiesMetadata holds configuration information about organization-level
            // properties. This is primarily used to display input fields on
            // the auth configuration page. The information is retrieved
            // from an xml metadata file dedicated just to auth properties.
            authPropertiesMetadata = new PropertiesMetaData(authMetadataFilePath);

            // The authBackupFilePath holds properties that were backed up
            // the last time the auth was configured. On disk, the file
            // will look like a smaller version of metacat.properties. It
            // is stored in the data storage directory outside the
            // application directories.
            authBackupFilePath = PropertyService.getBackupDirPath().toString()
                + FileUtil.getFS() + AUTH_BACKUP_FILE_NAME;
            authBackupProperties = new SortedProperties(authBackupFilePath);
            authBackupProperties.load();
        } catch (TransformerException te) {
            throw new GeneralPropertyException(
                "XML Transform problem while loading PropertiesMetaData: " + te.getMessage());
        } catch (IOException ioe) {
            throw new GeneralPropertyException("I/O problem while loading properties: " + ioe.getMessage());
        }
    }

    /**
     * Get a shared singleton AuthPropertiesDelegate instance by calling
     * AuthPropertiesDelegate.getInstance(). If the instance does not already exist, it will be
     * created on the first call. Subsequent calls will return this same instance.
     *
     * @return a shared singleton AuthPropertiesDelegate instance
     * @throws GeneralPropertyException if an XML TransformerException or an IOException are thrown
     *                                  when trying to load the auth-related properties
     */
    protected static AuthPropertiesDelegate getInstance() throws GeneralPropertyException {
        if (authPropertiesDelegate == null) {
            authPropertiesDelegate = new AuthPropertiesDelegate();
        }
        return authPropertiesDelegate;
    }

    /**
     * Get the auth backup properties file. These are configurable properties that are stored
     * outside the metacat install directories so the user does not need to re-enter all the
     * configuration information every time they do an upgrade.
     *
     * @return a SortedProperties object with the backup properties
     */
    protected SortedProperties getAuthBackupProperties() {
        return authBackupProperties;
    }

    /**
     * Get the auth properties metadata. This is retrieved from an xml file that describes the
     * attributes of configurable properties.
     *
     * @return a PropertiesMetaData object with the organization properties metadata
     */
    protected PropertiesMetaData getAuthPropertiesMetadata() {
        return authPropertiesMetadata;
    }

    /**
     * Writes out configurable properties to a backup file outside the metacat install directory,
     * so they are not lost if metacat installation is overwritten during an upgrade. These backup
     * properties are used by the admin page to populate defaults when the configuration is edited.
     * (They are also used to overwrite the main properties if bypassAuthConfiguration() is called)
     */
    protected void persistAuthBackupProperties()
        throws GeneralPropertyException {

        // Use the metadata to extract configurable properties from the
        // overall properties list, and store those properties.
        try {
            SortedProperties backupProperties = new SortedProperties(authBackupFilePath);

            // Populate the backup properties for auth properties using
            // the associated metadata file
            PropertiesMetaData authMetadata = new PropertiesMetaData(authMetadataFilePath);

            Map<String, MetaDataProperty> authKeyMap = authMetadata.getProperties();
            Set<String> authKeySet = authKeyMap.keySet();
            for (String propertyKey : authKeySet) {
                // don't backup passwords
                MetaDataProperty metaData = authKeyMap.get(propertyKey);
                if (!metaData.getFieldType().equals(MetaDataProperty.PASSWORD_TYPE)) {
                    backupProperties.addProperty(
                        propertyKey, PropertyService.getProperty(propertyKey));
                }
            }

            // store the properties to file
            backupProperties.store();
            authBackupProperties = new SortedProperties(authBackupFilePath);
            authBackupProperties.load();

        } catch (TransformerException te) {
            throw new GeneralPropertyException(
                "Could not transform backup properties xml: " + te.getMessage());
        } catch (IOException ioe) {
            throw new GeneralPropertyException(
                "Could not backup configurable properties: " + ioe.getMessage());
        }
    }

    /**
     * (for dev use only) Bypasses the auth properties configuration utility by using the
     * auth backup properties to overwrite the main properties.
     */
    protected void bypassAuthConfiguration() {
        Vector<String> authBackupPropertyNames
                = getAuthBackupProperties().getPropertyNames();
        try {
            for (String authBackupPropertyName : authBackupPropertyNames) {
                String value = getAuthBackupProperties().getProperty(authBackupPropertyName);
                PropertyService.setPropertyNoPersist(authBackupPropertyName, value);
            }
            PropertyService.setPropertyNoPersist("configutil.authConfigured", "true");
        } catch (GeneralPropertyException gpe) {
            logMetacat.error(
                    "bypassConfiguration: General property error: " + gpe.getMessage());
            // no further action needed, since this is a dev-only action, and worst-case is that it
            // results in the dev being prompted to configure metacat again
        }
    }
}
