/**
 * This classe represents the list of possible namespaces of the resource map documents.
 * The list reads the properties in the metacat.propertities file.
 * @author tao
 *
 */
package edu.ucsb.nceas.metacat.common.resourcemap;

import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.ObjectFormatIdentifier;

import java.util.List;


public class ResourceMapNamespaces {
	
	private static final String RESOURCEMAPPROPERYNAME = "index.resourcemap.namespace";
	private static List<String> resourceMapNamespaces = null;
	    
	    /**
	     * Constructor
	     * @param solrIndex
	     * @param systemMetadataListener
	     */
	    public ResourceMapNamespaces() {
	    	init();
	    }
	    
	    /*
	     * Read the namespace list fromt the property file
	     */
	    private static void init() {
	    	resourceMapNamespaces = Settings.getConfiguration().getList(RESOURCEMAPPROPERYNAME);
	    	//System.out.println("Init list "+resourceMapNamespaces);
	    }
	    
	    /**
	     * Get the list of namespaces of the resourcemap. 
	     * @return the list of namespaces. It returns null if we can't find them.
	     */
	    public static List<String> getNamespaces() {
	    	if(resourceMapNamespaces == null) {
	    		init();
	    	}
	    	return resourceMapNamespaces;
	    }
	    
	    /**
	     * If the specified namespace is a resource map namespace.
	     * @param namespace  the specified namespace
	     * @return true if it is a namespace of the resource map.
	     */
	    public static boolean isResourceMap(String namespace) {
	    	boolean is = false;
	    	if(namespace != null && !namespace.trim().equals("")) {
	    		getNamespaces();
		    	if(resourceMapNamespaces != null && resourceMapNamespaces.contains(namespace)) {
		    		is = true;
		    	}
	    	}
	    	return is;
	    	
	    }
	    
	    /**
	     * If the specified ObjectFormatIdentifier is a resrouce map namespace.
	     * @param formatid  the specified format identifier.
	     * @return true if it is a namespace of the resource map.
	     */
	    public static boolean isResourceMap(ObjectFormatIdentifier formatId) {
	        boolean is = false;
	        if(formatId != null ) {
	        	is = isResourceMap(formatId.getValue());            
	        }
	        return is;
	    }
	
	
}