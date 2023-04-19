/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements properties methods for metacat
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 *
 *   '$Author: daigle $'
 *     '$Date: 2009-08-14 17:38:05 -0700 (Fri, 14 Aug 2009) $'
 * '$Revision: 5028 $'
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

package edu.ucsb.nceas.metacat.properties;

import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * A suite of utility classes for the metadata configuration utility
 */
public class SimpleProperties extends BaseService implements PropertiesInterface {
	
	private static SortedProperties sortedProperties = null;
	
	private static Log logMetacat = LogFactory.getLog(SimpleProperties.class);

	/**
	 * private constructor since this is a singleton
	 */
	protected SimpleProperties() throws ServiceException {		
		_serviceName = "SimpleProperties";
				
		initialize();		
	}
	
	public boolean refreshable() {
		return true;
	}
	
	public void doRefresh() throws ServiceException {
		initialize();
	}
	
	public void stop() throws ServiceException {
		return;
	}
	
	/**
	 * Initialize the singleton.
	 */
	private void initialize() throws ServiceException {

		// TODO - MB: GET RID OF THIS CLASS ALTOGETHER
		ServiceException exception = new ServiceException("DEPRECATED CLASS - DO NOT USE!!");
		exception.fillInStackTrace();
		throw exception;

//		logMetacat.debug("Initializing SimpleProperties");
//
//		String mainConfigFilePath =
//			PropertyService.CONFIG_FILE_PATH;
//		sortedProperties = new SortedProperties(mainConfigFilePath);
//
//		try {
//			sortedProperties.load();
//		} catch (IOException ioe) {
//			throw new ServiceException("I/O problem while loading properties: "
//					+ ioe.getMessage());
//		}
	}

	/**
	 * Utility method to get a property value from the properties file
	 * 
	 * @param propertyName
	 *            the name of the property requested
	 * @return the String value for the property
	 */
	public String getProperty(String propertyName)
			throws PropertyNotFoundException {
		return sortedProperties.getProperty(propertyName);
	}
	
	/**
     * Get a set of all property names.
     * 
     * @return Set of property names  
     */
    public Vector<String> getPropertyNames() {   	
    	return sortedProperties.getPropertyNames();
    }
    

	/**
	 * Get a Set of all property names that start with the groupName prefix.
	 * 
	 * @param groupName
	 *            the prefix of the keys to search for.
	 * @return enumeration of property names
	 */
    public Vector<String> getPropertyNamesByGroup(String groupName) {   	
    	return sortedProperties.getPropertyNamesByGroup(groupName);
    }
    
	/**
	 * Get a Map of all properties that start with the groupName prefix.
	 * 
	 * @param groupName
	 *            the prefix of the keys to search for.
	 * @return Map of property names
	 */
    public Map<String, String> getPropertiesByGroup(String groupName) throws PropertyNotFoundException {   	
    	return sortedProperties.getPropertiesByGroup(groupName);
    }

	/**
	 * Utility method to add a property value both in memory and to the
	 * properties file
	 * 
	 * @param propertyName
	 *            the name of the property to add
	 * @param value
	 *            the new value for the property
	 */
	public void addProperty(String propertyName, String value) throws GeneralPropertyException {
		sortedProperties.addProperty(propertyName, value);
		sortedProperties.store();
	}
    
	/**
	 * Utility method to set a property value both in memory and to the
	 * properties file
	 * 
	 * @param propertyName
	 *            the name of the property requested
	 * @param newValue
	 *            the new value for the property
	 */
	public void setProperty(String propertyName, String newValue) throws GeneralPropertyException {
		sortedProperties.setProperty(propertyName, newValue);
		sortedProperties.store();
	}

	/**
	 * Utility method to set a property value in memory. This will NOT cause the
	 * property to be written to disk. Use this method to set multiple
	 * properties in a row without causing excessive I/O. You must call
	 * persistProperties() once you're done setting properties to have them
	 * written to disk.
	 * 
	 * @param propertyName
	 *            the name of the property requested
	 * @param newValue
	 *            the new value for the property
	 */
	public void setPropertyNoPersist(String propertyName, String newValue) throws GeneralPropertyException {
		sortedProperties.setPropertyNoPersist(propertyName, newValue);
	}

	/**
	 * Save the properties to a properties file. Note, the 
	 * order and comments will be preserved.
	 */
	public void persistProperties() throws GeneralPropertyException {
		sortedProperties.store();
	}
	
	/**
	 * Take input from the user in an HTTP request about an property to be changed
	 * and update the metacat property file with that new value if it has
	 * changed from the value that was originally set.
	 * 
	 * @param request
	 *            that was generated by the user
	 * @param propertyName
	 *            the name of the property to be checked and set
	 */
	public boolean checkAndSetProperty(HttpServletRequest request, String propertyName) 
			throws GeneralPropertyException {
		boolean changed = false;
		String value = getProperty(propertyName);
		String newValue = request.getParameter(propertyName);
		if (newValue != null && !newValue.trim().equals(value)) {
			setPropertyNoPersist(propertyName, newValue.trim());
			changed = true;
		}
		return changed;
	}
	
	public SortedProperties getMainBackupProperties() {
		return sortedProperties;
	}
	
	public  SortedProperties getAuthBackupProperties() throws GeneralPropertyException {
		throw new GeneralPropertyException("SimpleProperties.getAuthBackupProperties - " +
				"SimpleProperties does not support backup properties");
	}
	
	public PropertiesMetaData getMainMetaData() throws GeneralPropertyException {
		throw new GeneralPropertyException("SimpleProperties.getMainMetaData - " +
				"SimpleProperties does not support metadata");
	}
	
	public PropertiesMetaData getAuthMetaData() throws GeneralPropertyException {
		throw new GeneralPropertyException("SimpleProperties.getAuthMetaData - " +
				"SimpleProperties does not support auth metadata");
	}
	
	public void persistMainBackupProperties() throws GeneralPropertyException {
		throw new GeneralPropertyException("SimpleProperties.persistMainBackupProperties - " +
			"SimpleProperties does not support backup properties");
	}
	
	public void persistAuthBackupProperties(ServletContext servletContext) throws GeneralPropertyException {
		throw new GeneralPropertyException("SimpleProperties.persistAuthBackupProperties - " +
				"SimpleProperties does not support backup properties");
	}
	
	public boolean arePropertiesConfigured()  throws GeneralPropertyException {
		return true;
	}
	
	public boolean doBypass() throws GeneralPropertyException {
		throw new GeneralPropertyException("SimpleProperties.doBypass - " +
			"SimpleProperties does not support doBypass method.");
	}
	
	public void bypassConfiguration()  throws GeneralPropertyException {
		throw new GeneralPropertyException("SimpleProperties.doBypass - " +
			"SimpleProperties does not support bypassConfiguration method.");
	}

}
