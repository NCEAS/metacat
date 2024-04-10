package edu.ucsb.nceas.metacat.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringReader;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;
import edu.ucsb.nceas.utilities.UtilException;

public class GeoserverUtil {
	
	private static Log logMetacat = LogFactory.getLog(GeoserverUtil.class);
	
	/**
	 * private constructor - all methods are static so there is no
     * no need to instantiate.
	 */
	private GeoserverUtil() {}
	
	
	/**
	 * Change the password on the geoserver. The loginAdmin method must have
	 * already been called using the same HttpClient that is passed to this
	 * method.
	 * 
	 * @param httpClient
	 *            the HttpClient we will use to post. This should have been used
	 *            in the login post.
	 * @param username
	 *            the new user name
	 * @param password
	 *            the new password
	 */
	public static void changePassword(String username, String password) 
		throws MetacatUtilException {
		try {	
			
			// the users file
			String usersFile = 
			 PropertyService.getProperty("geoserver.GEOSERVER_DATA_DIR")
				+ FileUtil.getFS() 
				+ "security"
				+ FileUtil.getFS()
				+ "users.properties";
			SortedProperties userProperties = new SortedProperties(usersFile);
			userProperties.load();
			/* looks like this:
				#Wed Jan 26 12:13:09 PST 2011
				admin=geoserver,ROLE_ADMINISTRATOR,enabled
			*/
			String value = password + ",ROLE_ADMINISTRATOR,enabled";
			userProperties.setProperty(username, value);
			
		} catch (Exception e) {
			throw new MetacatUtilException("Property error while changing default password: " 
				 + e.getMessage());
		}
	}
	
	/**
	 * Reports whether geoserver is configured.
	 * 
	 * @return a boolean that is true if geoserver is configured or bypassed
	 */
	public static boolean isGeoserverConfigured() throws MetacatUtilException {
		String geoserverConfiguredString = PropertyService.UNCONFIGURED;
		try {
			geoserverConfiguredString = PropertyService.getProperty("configutil.geoserverConfigured");
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("Could not determine if geoservice are configured: "
					+ pnfe.getMessage());
		}
		// geoserver is configured if not unconfigured
		return !geoserverConfiguredString.equals(PropertyService.UNCONFIGURED);
	}
	
	public static void writeConfig() throws MetacatUtilException {
		try {
			// the template geoserver web.xml file
			String configFileTemplate = 
				SystemUtil.getContextDir()
				+ FileUtil.getFS()
				+ "web.xml.geoserver";
			
			// the destination for the template
			String configFileDestination = 
				PropertyService.getProperty("application.deployDir")
				+ FileUtil.getFS()
				+ PropertyService.getProperty("geoserver.context")
				+ FileUtil.getFS() 
				+ "WEB-INF"
				+ FileUtil.getFS()
				+ "web.xml";
			
			// look up the configured value for the data dir, write it to the file
			String dataDir = PropertyService.getProperty("geoserver.GEOSERVER_DATA_DIR");
			String configContents = FileUtil.readFileToString(configFileTemplate, "UTF-8");
			configContents = configContents.replace("_GEOSERVER_DATA_DIR_VALUE_", dataDir);
			FileUtil.writeFile(configFileDestination, new StringReader(configContents), "UTF-8");			
		} catch (Exception pnfe) {
			throw new MetacatUtilException(
					"Property error while setting geoserver configuration. " +
					"Please verify geoserver installation. " + pnfe.getMessage());
		}
	
	}
	
	/**
	 * Get the server URL with the Geoserver context. This is made up of the server URL +
	 * file separator + the Geoserver context
	 * 
	 * @return string holding the server URL with context
	 */
	public static String getGeoserverContextURL() throws PropertyNotFoundException {
		return SystemUtil.getServerURL() + "/"
				+ PropertyService.getProperty("geoserver.context");
	}
}
