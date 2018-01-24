/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements administrative methods 
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

package edu.ucsb.nceas.metacat.util;

import java.util.Vector;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;

public class OrganizationUtil {

	/**
	 * private constructor - all methods are static so there is no no need to
	 * instantiate.
	 */
	private OrganizationUtil() {}

	/**
	 * gets a list of organization names from metacat.properties. Parses the csv
	 * string into a vector
	 * 
	 * @return a Vector of Strings that hold all available organizations
	 * 
	 * TODO MCD this should be retrieved from ldap instead of metacat.properties
	 */
	public static Vector<String> getOrganizations() throws MetacatUtilException {

		Vector<String> shortOrgNames = new Vector<String>();
		Vector<String> longOrgNames = null;
		
		longOrgNames = PropertyService.getPropertyNamesByGroup("organization.name");

		for (String longName : longOrgNames) {
			shortOrgNames.add(longName.substring(18));
		}
		return shortOrgNames;
	}
	
	/**
	 * gets a list of organization names from metacat.properties. Parses the csv
	 * string into a vector
	 * 
	 * @return a Vector of Strings that hold all available organizations
	 */
	public static Vector<String> getOrgDNs(String orgName) throws MetacatUtilException {

		String orgBaseList = null;
		try {
			orgBaseList = PropertyService.getProperty("organization.base." + orgName);
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not get metacat property: organization.base." 
					+ orgName + " : " + pnfe.getMessage());
		}
		// this will always return a vector (maybe an empty one)
		return StringUtil.toVector(orgBaseList, ',');
	}
	

	/**
	 * Reports whether LDAP is fully configured.
	 * 
	 * @return a boolean that is true if all sections are configured and false
	 *         otherwise
	 */
	public static boolean areOrganizationsConfigured() throws MetacatUtilException {
		String orgConfiguredString = PropertyService.UNCONFIGURED;
		try {
			orgConfiguredString = PropertyService.getProperty("configutil.organizationsConfigured");
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not determine if organizations are configured: "
					+ pnfe.getMessage());
		}
		return !orgConfiguredString.equals(PropertyService.UNCONFIGURED);
	}
}
