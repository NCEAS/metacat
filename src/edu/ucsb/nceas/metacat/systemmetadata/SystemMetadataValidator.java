/**
 *  Copyright: 2023 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ucsb.nceas.metacat.systemmetadata;

import java.math.BigInteger;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;

/**
 * A class to validate the given system metadata object
 * @author tao
 *
 */
public class SystemMetadataValidator {
    private static Log logMetacat = LogFactory.getLog(SystemMetadataValidator.class);
    
    /**
     * Check if the given system metadata has the latest version.
     * If Metacat doesn't have a stored copy of the system metadata,
     * of course this given system is the latest version; if Metacat 
     * does have a copy, we will check two parts -
     * first, the last modified date should match the stored copy.
     * second, the serial version should equal or be greater than the stored copy
     * @param sysmeta
     * @return true if it has the latest version; otherwise false.
     * @throws InvalidSystemMetadata
     * @throws ServiceFailure 
     */
    public static boolean hasLatestVersion(SystemMetadata sysmeta) 
                                throws InvalidSystemMetadata, ServiceFailure {
        boolean hasLatestVersion = false;
        if (sysmeta == null) {
            throw new InvalidSystemMetadata("SystemMetadataValidator.hasLatestVersion - " +
                        " the given system metadata shouldn't be null.");
        } else {
            Identifier id = sysmeta.getIdentifier();
            if (id == null || id.getValue() == null || id.getValue().trim().equals("")) {
                throw new InvalidSystemMetadata("SystemMetadataValidator.hasLatestVersion - " +
                        " the identifier in the given system metadata shouldn't be null or blank.");
            } else {
                SystemMetadata storedSysmeta = SystemMetadataManager.getInstance().get(id);
                if (storedSysmeta == null) {
                    logMetacat.debug("SystemMetadataValidator.hasLatestVersion - " +
                            " Metacat doesn't have a stored copy for pid " + 
                            id.getValue() + " so it is a new system metadata." +
                            " So it has the latest version.");
                    hasLatestVersion = true;
                } else {
                    logMetacat.debug("SystemMetadataValidator.hasLatestVersion - " +
                            " Metacat does have a stored copy for pid " + 
                            id.getValue());
                    Date storedModifiedDate = storedSysmeta.getDateSysMetadataModified();
                    Date modifiedDate = sysmeta.getDateSysMetadataModified();
                    if (storedModifiedDate != null && 
                            (modifiedDate == null || 
                            storedModifiedDate.getTime() != modifiedDate.getTime())) {
                        throw new InvalidSystemMetadata("SystemMetadataValidator.hasLatestVersion - " +
                            "the given system metadata modification date is null or " +
                            " doesn't match our current system metadata modification date in " +
                            "the member node - " + storedModifiedDate.toString() +
                            ". Please check if you have got the latest version of the system " +
                                "metadata before the modification.");
                    }
                    BigInteger storedVersion = storedSysmeta.getSerialVersion();
                    BigInteger version = sysmeta.getSerialVersion();
                    if (storedVersion != null && 
                            (version == null || storedVersion.compareTo(version) > 0)) {
                        throw new InvalidSystemMetadata("SystemMetadataValidator.hasLatestVersion - " +
                                "the given system metadata serial version is null or " +
                                " is less than our current system metadata serial version in " +
                                "the member node - " + storedVersion.intValue() +
                                ". Please check if you have got the latest version of the system " +
                                    "metadata before the modification.");
                    }
                    hasLatestVersion = true;
                }
            }
        }
        return hasLatestVersion;
    }

}
