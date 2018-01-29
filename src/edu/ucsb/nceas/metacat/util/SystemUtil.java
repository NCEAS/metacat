/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements system utility methods 
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;
import edu.ucsb.nceas.utilities.UtilException;

public class SystemUtil {

	private static Logger logMetacat = Logger.getLogger(SystemUtil.class);
	private static String METACAT_SERVLET = "metacat";
//	private static String METACAT_WEB_SERVLET = "metacatweb";
	private static int OS_CLASS = 0;
	
	// Class of OS.  If we need more granularity, we should create a version
	// list and access it separately.
	public static int WIN_OS = 1;
	public static int LINUX_OS = 2;
	public static int MAC_OS = 3;
	public static int OTHER_OS = 4;
	
	/**
	 * private constructor - all methods are static so there is no no need to
	 * instantiate.
	 */
	private SystemUtil() {}

	/**
	 * Get the OS for this system.
	 * @return an integer representing the class of OS.  Possibilities are:
	 *     WIN_OS = 1;
	 *     LINUX_OS = 2;
	 *     MAC_OS = 3;
	 *     OTHER_OS = 4;
	 */
	public static int getOsClass() {
		if (OS_CLASS > 0) {
			return OS_CLASS;
		}
		
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows")) {
			OS_CLASS =  WIN_OS;
		} else if (osName.startsWith("Linux")) {
			OS_CLASS =  LINUX_OS;
		} else if (osName.startsWith("Mac")) {
			OS_CLASS =  MAC_OS;
		} else if (osName.startsWith("Mac")) {
			OS_CLASS =  OTHER_OS;
		}
		
		return OS_CLASS;
	}
	
	/**
	 * Attempt to discover the server name. The name is retrieved from the http
	 * servlet request. This is used by configuration routines before the port
	 * has been populated in metacat.properties. it is possible the port that
	 * the user configures might be different than the name we get here. You
	 * should use getServerPort() instead of this method whenever possible.
	 * 
	 * @param request
	 *            the http servlet request we will use to find the server name
	 * 
	 * @return a string holding the server name
	 */
	public static String discoverServerName(HttpServletRequest request) {
		String serverName = request.getServerName();

		return serverName;
	}

	/**
	 * Attempt to discover the server port. The port is retrieved from the http
	 * servlet request. This is used by configuration routines before the port
	 * has been populated in metacat.properties. it is possible the port that
	 * the user configures might be different than the port we get here. You
	 * should use getServerPort() instead of this method whenever possible.
	 * 
	 * @param request
	 *            the http servlet request we will use to find the server port
	 * 
	 * @return a string holding the server port
	 */
	public static String discoverServerPort(HttpServletRequest request) {
		return Integer.toString(request.getServerPort());
	}
	
	/**
	 * Attempt to discover the server ssl port. The ssl port is assumed using
	 * the standard port. This is used by configuration routines before the port
	 * has been populated in metacat.properties. it is possible the prot that
	 * the user configures might be different than the port we get here. You
	 * should use getServerSSLPort() instead of this method whenever possible.
	 * 
	 * @param request
	 *            the http servlet request we will use to find the server port
	 * 
	 * @return a string holding the server ssl port
	 */
	public static String discoverServerSSLPort(HttpServletRequest request) {
		String serverPort = discoverServerPort(request);

		if (serverPort.length() == 4 && serverPort.charAt(0) == '8') {
			return "8443";
		}

		return "443";
	}

	/**
	 * Get the server URL which is made up of the server name + : + the http
	 * port number. Note that if the port is 80, it is left off.
	 * 
	 * @return string holding the server URL
	 */
	public static String getServerURL() throws PropertyNotFoundException {
	    String httpPort = PropertyService.getProperty("server.httpPort");
	    
	    String serverURL = "http://";
	    if(httpPort.equals("443") || httpPort.equals("8443"))
	    {
	        serverURL = "https://";
	    }
	    
	    serverURL += PropertyService.getProperty("server.name");
		
		if (!httpPort.equals("80") && !httpPort.equals("443")) {
			serverURL += ":" + httpPort;
		}

		return serverURL;
	}

	/**
	 * Get the secure server URL which is made up of the server name + : + the
	 * https port number. Note that if the port is 443, it is left off.
	 * 
	 * @return string holding the server URL
	 */
	public static String getSecureServerURL() throws PropertyNotFoundException {
		String ServerURL = "https://" + getSecureServer();
		return ServerURL;
	}
	
	/**
	 * Get the secure server  which is made up of the server name + : + the
	 * https port number. Note that if the port is 443, it is left off.
	 * NOTE: does NOT include "https://"
	 * @see getSecureServerURL()
	 * 
	 * @return string holding the secure server
	 */
	public static String getSecureServer() throws PropertyNotFoundException {
		String server = PropertyService.getProperty("server.name");
		String httpPort = PropertyService.getProperty("server.httpSSLPort");
		if (!httpPort.equals("443")) {
			server = server + ":" + httpPort;
		}

		return server;
	}

	/**
	 * Get the CGI URL which is made up of the server URL + file separator + the
	 * CGI directory
	 * 
	 * @return string holding the server URL
	 */
	public static String getCGI_URL() throws PropertyNotFoundException{
		return getContextURL() 
				+ PropertyService.getProperty("application.cgiDir");
	}

	/**
	 * Get the server URL with the context. This is made up of the server URL +
	 * file separator + the context
	 * 
	 * @return string holding the server URL with context
	 */
	public static String getContextURL() throws PropertyNotFoundException {
		return getServerURL() + "/"
				+ PropertyService.getProperty("application.context");
	}
	
	/**
	 * Get the secure server URL with the context. This is made up of the secure server URL +
	 * file separator + the context
	 * 
	 * @return string holding the server URL with context
	 */
	public static String getSecureContextURL() throws PropertyNotFoundException {
		return getSecureServerURL() + "/"
				+ PropertyService.getProperty("application.context");
	}

	/**
	 * Get the servlet URL. This is made up of the server URL with context +
	 * file separator + the metacat servlet name
	 * 
	 * @return string holding the servlet URL
	 */
	public static String getServletURL() throws PropertyNotFoundException {
		return getContextURL() + "/" + METACAT_SERVLET;
	}
	
//	/**
//	 * Get the web front end servlet URL. This is made up of the server URL with 
//	 * context + file separator + the metacat web servlet name
//	 * 
//	 * @return string holding the web servlet URL
//	 */
//	public static String getWebServletURL() throws PropertyNotFoundException {
//		return getContextURL() + "/" + METACAT_WEB_SERVLET;
//	}

	/**
	 * Get the style skins URL. This is made up of the server URL with context +
	 * file separator + "style" + file separator + "skins"
	 * 
	 * @return string holding the style skins URL
	 */
	public static String getStyleSkinsURL() throws PropertyNotFoundException {
		return getContextURL() + "/" + "style" + "/" + "skins";
	}

	/**
	 * Get the style common URL. This is made up of the server URL with context +
	 * file separator + "style" + file separator + "common"
	 * 
	 * @return string holding the style common URL
	 */
	public static String getStyleCommonURL() throws PropertyNotFoundException {
		return getContextURL() + "/" + "style" + "/" + "common";
	}
	
	/**
	 * Get the metacat version by getting the string representation from
	 * metacat.properties and instantiating a MetacatVersion object.
	 * The info is set in build.properties and then populated into metacat.properties
	 * at build time using Ant token replacement.
	 * 
	 * @return a MetacatVersion object holding metacat version information
	 */
	public static MetacatVersion getMetacatVersion() throws PropertyNotFoundException {
		String metacatVersionString = 
			PropertyService.getProperty("application.metacatVersion");
		return new MetacatVersion(metacatVersionString);
	}
	
	/**
	 * Gets a string that holds some description about the release. Typically this is 
	 * used during release candidates for display purposes and might hold something
	 * like "Release Candidate 1".  Normally it is empty for final production release.
	 * The info is set in build.properties and then populated into metacat.properties
	 * at build time using Ant token replacement.
	 * 
	 * @return a MetacatVersion object holding metacat version information
	 */
	public static String getMetacatReleaseInfo() throws PropertyNotFoundException {
		return PropertyService.getProperty("application.metacatReleaseInfo");
	}

	/**
	 * Get the context directory. This is made up of the deployment directory + file
	 * separator + context
	 * 
	 * @return string holding the context directory
	 */
	public static String getContextDir() throws PropertyNotFoundException {
		return PropertyService.getProperty("application.deployDir") + FileUtil.getFS()
				+ PropertyService.getProperty("application.context");
	}

	/**
	 * Attempt to discover the context for this application. This is a best
	 * guess scenario. It is used by configuration routines before the context
	 * has been populated in metacat.properties. You should always use
	 * getApplicationContext() instead of this method if possible.
	 * 
	 * @param servletContext
	 *            the servlet context we will use to find the application context
	 * 
	 * @return a string holding the context
	 */
	public static String discoverApplicationContext(ServletContext servletContext) {
		String applicationContext = "";
		String realPath = servletContext.getRealPath("/");

		if (realPath.charAt(realPath.length() - 1) == '/') {
			realPath = realPath.substring(0, realPath.length() - 1);
		}
		
		int lastSlashIndex = realPath.lastIndexOf('/');
		if (lastSlashIndex != -1) {
			applicationContext = realPath.substring(lastSlashIndex + 1);
		}
				
		logMetacat.debug("application context: " + applicationContext);

		return applicationContext;
	}
	
	/**
	 * Gets the stored backup location.  This location is held in a file at
	 * <user_home>/.<application_context>/backup-location
	 * 
	 * @return a string holding the backup location.  Null if none could be found.
	 */
	public static String getStoredBackupDir() throws MetacatUtilException {
		String applicationContext = null;
		try {
			applicationContext = ServiceService.getRealApplicationContext();
			// Check if there is a file at
			// <user_home>/<application_context>/backup-location. If so, it
			// should contain one line that is a file that points to a writable
			// directory. If that is true, use that value as the backup dir.
			String storedBackupFileLoc = getUserHomeDir() + FileUtil.getFS() + "."
					+ applicationContext + FileUtil.getFS() + "backup-location";
			if (FileUtil.getFileStatus(storedBackupFileLoc) >= FileUtil.EXISTS_READABLE) {
				String storedBackupDirLoc = FileUtil
						.readFileToString(storedBackupFileLoc);
				if (FileUtil.isDirectory(storedBackupDirLoc)
						&& FileUtil.getFileStatus(storedBackupDirLoc) > FileUtil.EXISTS_READABLE) {
					return storedBackupDirLoc;
				}
			}
		} catch (UtilException ue) {
			logMetacat.warn("Utility problem finding backup location: " + ue.getMessage());
		} catch (ServiceException se) {
			logMetacat.warn("Could not get real application context: " + se.getMessage());
		}
		return null;
	}
	
	public static void writeStoredBackupFile(String backupPath) throws MetacatUtilException {
		String applicationContext = null;
		try {
			applicationContext = ServiceService.getRealApplicationContext();
			// Write the backup path to
			// <user_home>/.<application_context>/backup-location. 
			String storedBackupFileDir = getUserHomeDir() + FileUtil.getFS() + "." + applicationContext;
			String storedBackupFileLoc = storedBackupFileDir + FileUtil.getFS() + "backup-location";
			if (!FileUtil.isDirectory(storedBackupFileDir)) {
				FileUtil.createDirectory(storedBackupFileDir);
			}
			if (FileUtil.getFileStatus(storedBackupFileLoc) == FileUtil.DOES_NOT_EXIST) {
				FileUtil.createFile(storedBackupFileLoc);
			}		
			if (FileUtil.getFileStatus(storedBackupFileLoc) < FileUtil.EXISTS_READ_WRITABLE) {
				throw new UtilException("Stored backup location file is not writable: " + storedBackupFileLoc);
			}
			
			FileUtil.writeFile(storedBackupFileLoc, backupPath);
			
		} catch (UtilException ue) {
			logMetacat.warn("Utility error writing backup file: " + ue.getMessage());
		} catch (ServiceException se) {
			logMetacat.warn("Service error getting real application context: " + se.getMessage());
		} 
	}
	
	/**
	 * Attempt to discover the external (to the metacat installation)
	 * directory where metacat will hold backup files.   This functionality 
	 * is used to populate the configuration utility initially.  The user 
	 * can change the directory manually, so you can't rely on this method 
	 * to give you the actual directory.  Here are the steps taken to discover
	 * the directory:
	 * 
     * -- 1) Look for an existing hidden (.<application_context>) directory in a default system directory.  Get 
	 *       the default base directory for the OS.  (See application.windowsBackupBaseDir and 
	 *       application.linuxBackupBaseDir in metacat.properties.)  If a directory called 
	 *       <base_dir>/metacat/.<application_context> exists, return <base_dir>/metacat.
	 * -- 2) Otherwise, look for an existing hidden (.metacat) directory in the user directory. If a directory 
	 *       called <user_dir>/metacat/.<application_context> exists for the user that started tomcat, 
	 *       return <user_dir>/metacat.
	 * -- 3) Otherwise, look for an existing metacat directory in a default system directory.  Get 
	 *       the default base directory for the OS.  (See application.windowsBackupBaseDir and 
	 *       application.linuxBackupBaseDir in metacat.properties.)  If a directory called 
	 *       <base_dir>/metacat exists, return <base_dir>/metacat.
	 * -- 4) Otherwise, look for an existing metacat directory in the user directory. If a directory 
	 *       called <user_dir>/metacat/ exists for the user that started tomcat, return <user_dir>/metacat.
	 * -- 5) Otherwise, is the <base_dir> writable by the user that started tomcat?  If so, return 
	 *       <base_dir>/metacat
	 * -- 6) Does the <user_home> exist?  If so, return <user_home>/metacat
	 * -- 7) Otherwise, return null
	 *    
	 * @return a string holding the backup directory path
	 */
	public static String discoverExternalDir() throws MetacatUtilException {
		String applicationContext = null; 
		
		try {
			applicationContext = ServiceService.getRealApplicationContext();
			
			// Set the default location using the os
			String systemDir = "";
			if (getOsClass() == WIN_OS) {
				systemDir = "C:\\Program Files";
			} else {
				systemDir = "/var";
			}	
			String systemMetacatDir = systemDir + FileUtil.getFS() + "metacat";
			String systemBackupDir = systemMetacatDir + FileUtil.getFS() + "."
					+ applicationContext;

			String userHomeDir = getUserHomeDir();
			String userHomeMetacatDir = userHomeDir + FileUtil.getFS() + "metacat";
			String userHomeBackupDir = userHomeMetacatDir + FileUtil.getFS() + "." + applicationContext;

			// If <system_dir>/metacat/.<application_context> exists writable, 
			// return <system_dir>/metacat
			if ((FileUtil.getFileStatus(systemBackupDir) >= FileUtil.EXISTS_READ_WRITABLE)) {
				return systemMetacatDir;
			}

			// Otherwise if <user_dir>/metacat/.<application_context> exists writable, return
			// <user_dir>/metacat
			if ((FileUtil.getFileStatus(userHomeBackupDir) >= FileUtil.EXISTS_READ_WRITABLE)) {
				return userHomeMetacatDir;
			}

			// Otherwise if <system_dir>/metacat exists writable, create 
			// <system_dir>/metacat/.<application_context> and return <system_dir>/metacat
			if ((FileUtil.getFileStatus(systemMetacatDir) >= FileUtil.EXISTS_READ_WRITABLE)) {
				// go ahead and create the backup hidden dir
				FileUtil.createDirectory(systemBackupDir);
				return systemMetacatDir;
			}

			// Otherwise if <user_dir>/metacat exists writable, create 
			// <user_dir>/metacat/.<application_context> and return <user_dir>/metacat
			if ((FileUtil.getFileStatus(userHomeMetacatDir) >= FileUtil.EXISTS_READ_WRITABLE)) {
				// go ahead and create the backup hidden dir
				FileUtil.createDirectory(userHomeBackupDir);
				return userHomeMetacatDir;
			}
			
			// Otherwise if <system_dir> exists, create 
			// <system_dir>/metacat/.<application_context> and return <system_dir>/metacat
			if ((FileUtil.getFileStatus(systemDir) >= FileUtil.EXISTS_READ_WRITABLE)) {
				// go ahead and create the backup hidden dir
				FileUtil.createDirectory(systemBackupDir);
				return systemMetacatDir;
			}

			// Otherwise if <user_dir> exists, return <user_dir> create 
			// <user_dir>/metacat/.<application_context> and return <user_dir>/metacat
			if ((FileUtil.getFileStatus(userHomeDir) >= FileUtil.EXISTS_READ_WRITABLE)) {
				// go ahead and create the backup hidden dir
				FileUtil.createDirectory(userHomeBackupDir);
				return userHomeMetacatDir;
			}

		} catch (ServiceException se) {
			logMetacat.warn("Could not get real application context: " + se.getMessage());
		} catch (UtilException ue) {
			logMetacat.warn("Could not create directory: " + ue.getMessage());
		} 
		
		// Otherwise, return userHomeDir
		return null;
	}
	
	/**
	 * Store the location of the backup file location into a file at 
	 * <user_home>/<application_dir>/backup-location
	 * 
	 * @param externalDir the backup file location.
	 */
	public static void storeExternalDirLocation(String externalDir) {
		if (getUserHomeDir() != null) {
			String applicationContext = null;
			String storedBackupLocDir = null;
			String storedBackupLocFile = null;
			try {
				applicationContext = ServiceService.getRealApplicationContext();
				storedBackupLocDir = getUserHomeDir() + FileUtil.getFS() + "."
						+ applicationContext;
				storedBackupLocFile = storedBackupLocDir + FileUtil.getFS()
						+ "backup-location";
			
				FileUtil.createDirectory(storedBackupLocDir);
				FileUtil.writeFile(storedBackupLocFile, externalDir);
			} catch (ServiceException se) {
				logMetacat.error("Could not get real application directory while trying to write "
							+ "stored backup directory: "+ storedBackupLocFile + " : " + se.getMessage());
			} catch (UtilException ue) {
				logMetacat.error("Could not write backup location file into "
						+ "stored backup directory: "+ storedBackupLocFile + " : " + ue.getMessage());
			}
		} else {
			logMetacat.warn("Could not write out stored backup directory." 
					+ " User directory does not exist");
		}
	}

	/**
	 * Get the style skins directory. This is made up of the tomcat directory
	 * with context + file separator + "style" + file separator + "skins"
	 * 
	 * @return string holding the style skins directory
	 */
	public static String getStyleSkinsDir() throws PropertyNotFoundException {
		return getContextDir() + FileUtil.getFS() + "style" + FileUtil.getFS()
				+ "skins";
	}

	/**
	 * Get the SQL directory. This is made up of the context directory + file
	 * separator + sql
	 * 
	 * @return string holding the sql directory
	 */
	public static String getSQLDir() throws PropertyNotFoundException {
		return getContextDir() + FileUtil.getFS() + "WEB-INF" + FileUtil.getFS() + "sql";
	}

	/**
	 * Get the default style URL from metacat.properties.
	 * 
	 * @return string holding the default style URL
	 */
	public static String getDefaultStyleURL() throws PropertyNotFoundException {
		return getStyleCommonURL() + "/"
				+ PropertyService.getProperty("application.default-style");
	}

	/**
	 * Attempt to discover the deployment directory for this application. This is a
	 * best guess scenario. It is used by configuration routines before the
	 * deployment directory has been populated in metacat.properties. 
	 * 
	 * @param request
	 *            the http servlet request we will use to find the tomcat directory
	 * 
	 * @return a string holding the web application directory
	 */
	public static String discoverDeployDir(HttpServletRequest request) {
		ServletContext servletContext = request.getSession()
				.getServletContext();
		String realPath = servletContext.getRealPath(".");
		String contextPath = request.getContextPath();
		
		logMetacat.debug("realPath: " + realPath);
		logMetacat.debug("contextPath: " + contextPath);

		/*Pattern pattern = Pattern.compile(contextPath + "/\\.$");
		Matcher matcher = pattern.matcher(realPath);
		
		if (matcher.find()) {
			realPath = matcher.replaceFirst("");
		}*/
		int index = realPath.lastIndexOf(contextPath);
	    if(index != -1) {
	      realPath = realPath.substring(0,index);
	      if(realPath.equals("")) {
	        //if the realPath is "/metacat".
	        realPath="/";
	      }
	    }
	    logMetacat.info("SystemUtil.discoverDeployDir: the deploy dir is " + realPath);
		return realPath;
	}
	
	/**
	 * Get the current user's home directory
	 * 
	 * @return a string holding the home directory
	 */
	public static String getUserHomeDir() {
		return System.getProperty("user.home");
	}
	
	/**
	 * Get a list of xml paths that need to be indexed
	 */
	public static Vector<String> getPathsForIndexing() throws MetacatUtilException {
		Vector <String> indexPaths = null;
		try {
			indexPaths = 
				StringUtil.toVector(PropertyService.getProperty("xml.indexPaths"), ',');
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatUtilException("could not get index paths: " + pnfe.getMessage());
		}
		
		return indexPaths;
	}
	
	/**
	 * Get the url pointing to the user management page.
	 * @return the url.
	 * @throws PropertyNotFoundException
	 */
	public static String getUserManagementUrl() throws PropertyNotFoundException {
	    return PropertyService.getProperty("auth.userManagementUrl");
	}
 }
