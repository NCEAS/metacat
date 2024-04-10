package edu.ucsb.nceas.metacat.restservice.multipart;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dataone.mimemultipart.MultipartRequest;
import org.dataone.service.types.v1.SystemMetadata;

/**
 * A class extends from the MultipartRequest class with the extra information of system meta data
 * @author tao
 *
 */
public class MultipartRequestWithSysmeta extends MultipartRequest {
    private SystemMetadata systemMetadata = null;
    
    
    /**
     * Constructor
     * @param request
     * @param mpFiles
     * @param mpParams
     */
    public MultipartRequestWithSysmeta(HttpServletRequest request, Map<String, File> mpFiles, Map<String, List<String>> mpParams) {
        super(request, mpFiles, mpParams);
    }
    
    
    /**
     * Get the system meta data associate with this request
     * @return the system meta data 
     */
    public SystemMetadata getSystemMetadata() {
        return systemMetadata;
    }

    /**
     * Set the system meta data for this request
     * @param systemMetadata  the system meta data will be set
     */
    public void setSystemMetadata(SystemMetadata systemMetadata) {
        this.systemMetadata = systemMetadata;
    }


}
