/**
 *  Copyright: 2021 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.doi.osti;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.doi.DOIException;
import edu.ucsb.nceas.metacat.doi.DOIService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * The implementation class for the OSTI 
 * (DOE Office of Scientific and Technical Information) DOI service
 * Details of OSTI eink: https://www.osti.gov/elink/241-6api.jsp
 * @author tao
 */
public class OstiDOIService implements DOIService{
    private Log logMetacat = LogFactory.getLog(OstiDOIService.class);
    private boolean doiEnabled = false;
    private String username = null;
    private String password = null;
    private String serviceBaseUrl = null;
    
    /**
     * Constructor
     */
    public OstiDOIService() {
        try {
            doiEnabled = new Boolean(PropertyService.getProperty("guid.doi.enabled")).booleanValue();
            serviceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
            username = PropertyService.getProperty("guid.doi.username");
            password = PropertyService.getProperty("guid.doi.password");
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("DOI support is not configured at this node.", e);
            return;
        }
    }
    
    /**
     * Submits DOI metadata information about the object to DOI services
     * This method do nothing in the OSTI implmenation
     * @param sysMeta
     * @return true if succeeded; false otherwise.
     * @throws EZIDException
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InterruptedException
     */
    public boolean registerDOI(SystemMetadata sysMeta) throws InvalidRequest, DOIException, NotImplemented, 
                                                                ServiceFailure, InterruptedException {
        return true;
    }

    /**
     * Generate a DOI using the DOI service as configured
     * @return  the identifier which was minted by the DOI service
     * @throws EZIDException
     * @throws InvalidRequest
     */
    public Identifier generateDOI() throws DOIException, InvalidRequest {
        return null;
    }
    
    /**
     * Refresh the status (enable or disable) of the DOI service from property file
     * @throws PropertyNotFoundException 
     */
    public void refreshStatus() throws PropertyNotFoundException {
        
    }
    
    /**
     * Publish an object for the given identifier. Because of the different mechanisms using on the different DOI services, 
     * the given identifier can have different semantic meaning. On the EZID service, the identifier is for an existing 
     * object which will be obsoleted by a new generated DOI. On the OSTI service, the identifier is an existing DOI and
     * Metacat only needs to generate the metadata for it and doesn't need to obsolete it. 
     * @param service  the MNodeService object which calls the method
     * @param session  the subjects call the method
     * @param identifer  the identifier of the object which will be published. 
     * @throws InvalidRequest 
     * @throws NotImplemented 
     * @throws NotAuthorized 
     * @throws ServiceFailure 
     * @throws InvalidToken 
     * @throws NotFound
     * @throws InvalidSystemMetadata 
     * @throws InsufficientResources 
     * @throws UnsupportedType 
     * @throws IdentifierNotUnique 
     */
    public Identifier publish(MNodeService service, Session session, Identifier identifier) throws InvalidToken, 
    ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest, NotFound, IdentifierNotUnique, 
    UnsupportedType, InsufficientResources, InvalidSystemMetadata, IOException {
        return null;
    }
}
