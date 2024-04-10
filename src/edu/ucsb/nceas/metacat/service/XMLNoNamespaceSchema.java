package edu.ucsb.nceas.metacat.service;

import edu.ucsb.nceas.metacat.DocumentImpl;


/**
 * This class represents the information for a schema without a namespace
 * @author tao
 *
 */
public class XMLNoNamespaceSchema extends XMLSchema {
    private String noNamespaceSchemaLocation = null;
    private static final String type = DocumentImpl.NONAMESPACESCHEMA;
   
    /**
     * Constructor of the object
     * @param noNamespaceSchemaLocation  the uri of the noNamespaceSchemaLocation of the schema
     * @param externalFileUri  the registered file url location for this schema
     * @param formatId  the format id of a xml instance of the schema
     */
    public XMLNoNamespaceSchema(String noNamespaceSchemaLocation, String externalFileUri, String formatId) {
        //since it doesn't have a namespace, the null value will be set for the namespace
        super(null, externalFileUri, formatId);
        this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
    }
    
    /**
     * Get the no-namespace-schemal-location uri
     * @return
     */
    public String getNoNamespaceSchemaLocation() {
        return noNamespaceSchemaLocation;
    }
    
    /**
     * Set the no-namespace-schemal-location uri
     * @param noNamespaceSchemaLocation
     */
    public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
        this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
    }
    
    /**
     * Return the type of schema. It always is "NoNamespaceSchema".
     * @return
     */
    public static String getType() {
        return type;
    }
}
