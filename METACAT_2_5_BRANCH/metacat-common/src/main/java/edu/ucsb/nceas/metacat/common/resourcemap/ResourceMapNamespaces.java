/**
 *  '$RCSfile$'
 *    Purpose: A class represents the possible namespaces of the resource map documents.
 *    Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
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