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
