package edu.ucsb.nceas.metacat.object.handler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;

import com.github.jsonldjava.utils.JsonUtils;


/**
 * The handler of JSON-LD objects to save bytes to disk
 * @author tao
 *
 */
public class JsonLDHandler extends NonXMLMetadataHandler {
    private static Log logMetacat = LogFactory.getLog(JsonLDHandler.class);
    
    /**
     * Default constructor
     */
    public JsonLDHandler() {
        
    }
    
    @Override
    public boolean validate(InputStream source) throws InvalidRequest {
        try {
            Object jsonObject = JsonUtils.fromInputStream(source);
        } catch (IOException e) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the metadata object is invalid: " + e.getMessage());
        }
        return true;
    }

}
