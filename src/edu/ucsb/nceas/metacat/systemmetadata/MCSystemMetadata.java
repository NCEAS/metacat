package edu.ucsb.nceas.metacat.systemmetadata;

import org.apache.commons.beanutils.BeanUtils;
import org.dataone.service.types.v2.SystemMetadata;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class of SystemMetadata which can hold multiple checksums
 * @author Jing Tao
 */
public class MCSystemMetadata extends SystemMetadata {

    private Map<String, String> checksums;
    /**
     * Default constructor
     */
    public MCSystemMetadata() {
        super();
        checksums = new HashMap<String, String>();
    }

    /**
     * Copy all properties from a SystemMetacat object to an MCSystemMetadata object
     * @param mcSysmeta  the destination object will accept the instance variables' values
     * @param sysmeta  the source object
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static void copy(MCSystemMetadata mcSysmeta,
                            org.dataone.service.types.v1.SystemMetadata sysmeta)
        throws InvocationTargetException, IllegalAccessException {
        BeanUtils.copyProperties(mcSysmeta, sysmeta);
    }

    /**
     * A utility method to convert an MCSystemMetadata object to a SystemMetadata object
     * @param mcSystemMetadata  the object which needs to be converted
     * @return a new SystemMetadata object which has all fields except the checksums information
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static SystemMetadata convert(MCSystemMetadata mcSystemMetadata)
        throws InvocationTargetException, IllegalAccessException {
        mcSystemMetadata.setChecksums(null);// Get rid of the extra field
        SystemMetadata systemMetadata = new SystemMetadata();
        BeanUtils.copyProperties(systemMetadata, mcSystemMetadata);
        return systemMetadata;
    }

    /**
     * Get the checksums associated with the MCSystemMetadata object
     * @return a map of hash algorithm names to their hex-encoded digest values
     */
    public Map<String, String> getChecksums() {
        return checksums;
    }

    /**
     * Set the checksums for the MCSystemMetadata object
     * @param checksums a map of hash algorithm names to their hex-encoded digest values
     */
    public void setChecksums(Map<String, String> checksums) {
        this.checksums = checksums;
    }
}
