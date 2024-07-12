package edu.ucsb.nceas.metacat.systemmetadata;

import org.apache.commons.beanutils.BeanUtils;
import org.dataone.service.types.v2.SystemMetadata;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class of SystemMetadata which can hold multiple checksums
 * @Author  Jing Tao
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
     * @param mcSysmeta
     * @param sysmeta
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static void copy(MCSystemMetadata mcSysmeta,
                            org.dataone.service.types.v1.SystemMetadata sysmeta)
        throws InvocationTargetException, IllegalAccessException {
        BeanUtils.copyProperties(mcSysmeta, sysmeta);
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
