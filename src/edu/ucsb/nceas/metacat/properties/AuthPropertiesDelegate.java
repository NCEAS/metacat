/**
 *    Purpose: A Class that handles Authentication properties for metacat configuration
 *  Copyright: 2023 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Author:  Matthew Brooke
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307  USA
 */
package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.MetaDataProperty;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.SortedProperties;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class AuthPropertiesDelegate {
    protected static final String AUTH_METADATA_FILE_NAME = "auth.properties.metadata.xml";
    protected static final String AUTH_BACKUP_FILE_NAME = "auth.properties.backup";
    protected static String authMetadataFilePath = null;
    protected static PropertiesMetaData authMetaData = null;
    protected static String siteAuthPropsFilePath = null;
    protected static SortedProperties authBackupProperties = null;
    private static AuthPropertiesDelegate authPropertiesDelegate;

    private AuthPropertiesDelegate(String sitePropertiesPath) throws GeneralPropertyException {

        authMetadataFilePath =
            PropertyService.CONFIG_FILE_DIR + FileUtil.getFS() + AUTH_METADATA_FILE_NAME;
        try {
            // authMetaData holds configuration information about organization-level
            // properties. This is primarily used to display input fields on
            // the auth configuration page. The information is retrieved
            // from an xml metadata file dedicated just to auth properties.
            authMetaData = new PropertiesMetaData(authMetadataFilePath);

            // The siteAuthPropsFile holds properties that were backed up
            // the last time the auth was configured. On disk, the file
            // will look like a smaller version of metacat.properties. It
            // is stored in the data storage directory outside the
            // application directories.
            siteAuthPropsFilePath = sitePropertiesPath + FileUtil.getFS() + AUTH_BACKUP_FILE_NAME;
            authBackupProperties = new SortedProperties(siteAuthPropsFilePath);
            authBackupProperties.load();
        } catch (TransformerException te) {
            throw new GeneralPropertyException(
                "Transform problem while loading properties: " + te.getMessage());
        } catch (IOException ioe) {
            throw new GeneralPropertyException("I/O problem while loading properties: " + ioe.getMessage());
        }
    }

    protected static AuthPropertiesDelegate getInstance(String sitePropertiesPath)
        throws GeneralPropertyException {
        if (authPropertiesDelegate == null) {
            authPropertiesDelegate = new AuthPropertiesDelegate(sitePropertiesPath);
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
    protected PropertiesMetaData getAuthMetaData() {
        return authMetaData;
    }

    /**
     * Writes out backup configurable properties to a file.
     */
    protected void persistAuthBackupProperties()
        throws GeneralPropertyException {

        // Use the metadata to extract configurable properties from the
        // overall properties list, and store those properties.
        try {
            SortedProperties backupProperties = new SortedProperties(siteAuthPropsFilePath);

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
            authBackupProperties = new SortedProperties(siteAuthPropsFilePath);
            authBackupProperties.load();

        } catch (TransformerException te) {
            throw new GeneralPropertyException(
                "Could not transform backup properties xml: " + te.getMessage());
        } catch (IOException ioe) {
            throw new GeneralPropertyException(
                "Could not backup configurable properties: " + ioe.getMessage());
        }
    }

    protected void bypassAuthConfiguration(PropertiesWrapper properties) throws GeneralPropertyException {
        Vector<String> authBackupPropertyNames
            = getAuthBackupProperties().getPropertyNames();
        for (String authBackupPropertyName : authBackupPropertyNames) {
            String value = getAuthBackupProperties().getProperty(authBackupPropertyName);
            properties.setPropertyNoPersist(authBackupPropertyName, value);
        }
    }
}
