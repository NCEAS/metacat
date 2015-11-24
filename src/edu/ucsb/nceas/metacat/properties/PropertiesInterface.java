/**
 *  '$RCSfile$'
 *    Purpose: A Class that loads eml-access.xml file containing ACL 
 *             for a metadata document into relational DB
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jivka Bojilova
 *
 *   '$Author: daigle $'
 *     '$Date: 2009-03-25 14:04:06 -0800 (Wed, 25 Mar 2009) $'
 * '$Revision: 4863 $'
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

import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * This interface will handle properties access
 */
public interface PropertiesInterface {
	
	// system is configured
	public static final String CONFIGURED = "true"; 
	// system has never been configured
	public static final String UNCONFIGURED = "false"; 
	public static final String BYPASSED = "bypassed";
	
//	public PropertiesInterface getInstance() throws ServiceException;
	
	public String getProperty(String propertyName) throws PropertyNotFoundException;
	
	public  Vector<String> getPropertyNames();
	
	public Vector<String> getPropertyNamesByGroup(String groupName);
	
	public  Map<String, String> getPropertiesByGroup(String groupName) throws PropertyNotFoundException;
	
	public void addProperty(String propertyName, String value) throws GeneralPropertyException;
	
	public void setProperty(String propertyName, String newValue) throws GeneralPropertyException;
	
	public void setPropertyNoPersist(String propertyName, String newValue) throws GeneralPropertyException;
	
	public void persistProperties() throws GeneralPropertyException;
	
	public boolean checkAndSetProperty(HttpServletRequest request, String propertyName) throws GeneralPropertyException;

	public SortedProperties getMainBackupProperties() throws GeneralPropertyException;
	
	public  SortedProperties getAuthBackupProperties() throws GeneralPropertyException;
	
	public PropertiesMetaData getMainMetaData() throws GeneralPropertyException;
	
	public PropertiesMetaData getAuthMetaData() throws GeneralPropertyException;
	
	public void persistMainBackupProperties() throws GeneralPropertyException;
	
	public void persistAuthBackupProperties(ServletContext servletContext) throws GeneralPropertyException;
	
	public boolean arePropertiesConfigured()  throws GeneralPropertyException;
	
	public boolean doBypass() throws GeneralPropertyException;
	
	public void bypassConfiguration()  throws GeneralPropertyException;
 
}
