package edu.ucsb.nceas.metacat.object.handler;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.ObjectFormatIdentifier;

import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;

/**
 * The factory and utility methods for the NonXMLMetadataHandlers 
 * @author tao
 *
 */
public class NonXMLMetadataHandlers {
    
    public static String JSON_LD = "science-on-schema.org/Dataset;ld+json";
    
    private static Vector<String> nonXMLMetadataFormatList = new Vector<String>();
    
    static {
        nonXMLMetadataFormatList = XMLSchemaService.getInstance().getNonXMLMetadataFormatList();
    }
    
    private static Log logMetacat = LogFactory.getLog(CNodeService.class);
    
    /**
     * Create a NonXMLMetadataHandler instance to handle the given formatId. If null is returned,
     * this means that the given formatId is not a non-XML meta data type.
     * @param formatId  the object format identifier which will be processed
     * @return a instance of a handler which will handler this type. Null will be returned if Metacat
     * cannot find one for this object format identifier.
     */
    public static NonXMLMetadataHandler newNonXMLMetadataHandler(ObjectFormatIdentifier formatId) {
        NonXMLMetadataHandler handler = null;
        if (formatId == null) {
            return handler;
        }
        if (nonXMLMetadataFormatList != null && formatId.getValue() != null && 
                                    nonXMLMetadataFormatList.contains(formatId.getValue())) {
            if (formatId.getValue().equals(JSON_LD)) {
                handler = new JsonLDHandler();
            }
        }
        return handler;
    }
}
