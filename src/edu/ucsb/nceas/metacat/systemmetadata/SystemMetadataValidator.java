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
 *
 * @author tao
 */
public class SystemMetadataValidator {
    private static Log logMetacat = LogFactory.getLog(SystemMetadataValidator.class);

    /**
     * Check if the given system metadata has the latest version. If Metacat doesn't have a stored
     * copy of the system metadata, of course this given system is the latest version; if Metacat
     * does have a copy, we will check two parts - first, the last modified date should match the
     * stored copy. second, the serial version should equal or be greater than the stored copy
     *
     * @param sysmeta
     * @return true if it has the latest version; otherwise false.
     * @throws InvalidSystemMetadata
     * @throws ServiceFailure
     */
    public static boolean hasLatestVersion(SystemMetadata sysmeta)
        throws InvalidSystemMetadata, ServiceFailure {
        boolean hasLatestVersion = false;
        if (sysmeta == null) {
            throw new InvalidSystemMetadata("SystemMetadataValidator.hasLatestVersion - "
                                                + " the given system metadata shouldn't be null.");
        } else {
            Identifier id = sysmeta.getIdentifier();
            if (id == null || id.getValue() == null || id.getValue().trim().equals("")) {
                throw new InvalidSystemMetadata("SystemMetadataValidator.hasLatestVersion - "
                                                    + " the identifier in the given system "
                                                    + "metadata shouldn't be null or blank.");
            } else {
                SystemMetadata storedSysmeta = SystemMetadataManager.getInstance().get(id);
                if (storedSysmeta == null) {
                    logMetacat.debug("SystemMetadataValidator.hasLatestVersion - "
                                         + " Metacat doesn't have a stored copy for pid "
                                         + id.getValue() + " so it is a new system metadata."
                                         + " So it has the latest version.");
                    hasLatestVersion = true;
                } else {
                    logMetacat.debug("SystemMetadataValidator.hasLatestVersion - "
                                         + " Metacat does have a stored copy for pid "
                                         + id.getValue());
                    Date storedModifiedDate = storedSysmeta.getDateSysMetadataModified();
                    Date modifiedDate = sysmeta.getDateSysMetadataModified();
                    if (storedModifiedDate != null && (modifiedDate == null
                        || storedModifiedDate.getTime() != modifiedDate.getTime())) {
                        throw new InvalidSystemMetadata(
                            "SystemMetadataValidator.hasLatestVersion - "
                                + "the given system metadata modification date is null or "
                                + " doesn't match our current system metadata modification date in "
                                + "the member node - " + storedModifiedDate.toString()
                                + ". Please check if you have got the latest version of the system "
                                + "metadata before the modification.");
                    }
                    BigInteger storedVersion = storedSysmeta.getSerialVersion();
                    BigInteger version = sysmeta.getSerialVersion();
                    if (storedVersion != null && (version == null
                        || storedVersion.compareTo(version) > 0)) {
                        throw new InvalidSystemMetadata(
                            "SystemMetadataValidator.hasLatestVersion - "
                                + "the given system metadata serial version is null or "
                                + " is less than our current system metadata serial version in "
                                + "the member node - " + storedVersion.intValue()
                                + ". Please check if you have got the latest version of the system "
                                + "metadata before the modification.");
                    }
                    hasLatestVersion = true;
                }
            }
        }
        return hasLatestVersion;
    }

}
