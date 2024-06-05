package edu.ucsb.nceas.metacat.storage;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * The factory class to create a Storage instance
 */
public class StorageFactory {

    /**
     * Get the Storage implementation instance
     * @return  the Storage class
     * @throws PropertyNotFoundException
     * @throws ServiceException
     */
    public static Storage getStorage() throws PropertyNotFoundException, ServiceException {
        String className = PropertyService.getProperty("storage.className");
        if (className != null && className.startsWith("org.dataone.hashstore")) {
            return HashStorage.getInstance(className);
        } else {
            throw new ServiceException("StorageFactory.getStorage - Unrecognized the storage class "
                                + className + ". So Metacat can't initialize the storage system.");
        }
    }

}
