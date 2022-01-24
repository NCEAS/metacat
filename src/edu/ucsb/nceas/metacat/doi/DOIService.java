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
package edu.ucsb.nceas.metacat.doi;

import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * An interface for the DOI service
 * @author tao
 */
public interface DOIService {
    
    /**
     * Submits DOI metadata information about the object to DOI services
     * @param sysMeta
     * @return true if succeeded; false otherwise.
     * @throws EZIDException
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InterruptedException
     */
    public boolean registerDOI(SystemMetadata sysMeta) throws InvalidRequest, DOIException, NotImplemented, 
                                                                ServiceFailure, InterruptedException;

    /**
     * Generate a DOI using the DOI service as configured
     * @return  the identifier which was minted by the DOI service
     * @throws EZIDException
     * @throws InvalidRequest
     */
    public Identifier generateDOI() throws DOIException, InvalidRequest;
    
    /**
     * Refresh the status (enable or disable) of the DOI service from property file
     * @throws PropertyNotFoundException 
     */
    public void refreshStatus() throws PropertyNotFoundException;
}
