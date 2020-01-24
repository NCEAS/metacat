/**
 *  '$RCSfile$'
 *  Copyright: 2019 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
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
