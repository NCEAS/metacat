package edu.ucsb.nceas.metacat.doi;

import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.ezid.EZIDException;

/**
 * An interface for the DOI service
 * @author tao
 */
public interface DOIService {
    
    /**
     * Submits DOI metadata information about the object to DOI services
     * @param sysMeta
     * @return
     * @throws EZIDException
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InterruptedException
     */
    public boolean registerDOI(SystemMetadata sysMeta) throws InvalidRequest, EZIDException, NotImplemented, 
                                                                ServiceFailure, InterruptedException;

    /**
     * Generate a DOI using the DOI service as configured
     * @return
     * @throws EZIDException
     * @throws InvalidRequest
     */
    public Identifier generateDOI() throws EZIDException, InvalidRequest;
}
