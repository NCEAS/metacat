package edu.ucsb.nceas.metacat.object.handler;


import java.io.InputStream;

import org.dataone.service.exceptions.InvalidRequest;


/**
 * The abstract class to validate non-xml meta data objects
 * @author tao
 *
 */
public abstract class NonXMLMetadataHandler {

    /**
     *The abstract method to validate the non-xml object
     * @param source  the input stream contains the content of the meta data object
     * @return true if the content is valid; false otherwise.
     * @throws InvalidRequest  when the content is not valid
     */
    public abstract boolean validate(InputStream source) throws InvalidRequest;

}
