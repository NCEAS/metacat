/**
 *  '$RCSfile$'
 *    Purpose:  A Class that implements main property configuration methods
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

package edu.ucsb.nceas.metacat.admin;

import java.io.File;
import java.io.StringReader;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.database.DBVersion;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.SortedProperties;
import edu.ucsb.nceas.utilities.UtilException;

/**
 * Control the display of the main properties configuration page and the 
 * processing of the configuration values.
 */
public class PropertiesAdmin extends MetacatAdmin {
    private static String BACKSLASH = "/";
    private static String DEFAULTMETACATCONTEXT = "metacat";
    private static String METACATPROPERTYAPPENDIX = "/WEB-INF/metacat.properties";
	private static PropertiesAdmin propertiesAdmin = null;
	private static Logger logMetacat = Logger.getLogger(PropertiesAdmin.class);

	/**
	 * private constructor since this is a singleton
	 */
	private PropertiesAdmin() {}

	/**
	 * Get the single instance of the MetaCatConfig.
	 * 
	 * @return the single instance of MetaCatConfig
	 */
	public static PropertiesAdmin getInstance() {
		if (propertiesAdmin == null) {
			propertiesAdmin = new PropertiesAdmin();
		}
		return propertiesAdmin;
	}
	
	/**
	 * Handle configuration of the main application properties
	 * 
	 * @param request
	 *            the http request object
	 * @param response
	 *            the http response to be sent back to the client
	 */
	public void configureProperties(HttpServletRequest request,
			HttpServletResponse response) throws AdminException {

		String processForm = request.getParameter("processForm");
		String formErrors = (String)request.getAttribute("formErrors");

		if (processForm == null || !processForm.equals("true") || formErrors != null) {
			// The servlet configuration parameters have not been set, or there
			// were form errors on the last attempt to configure, so redirect to
			// the web form for configuring metacat

			try {
				// Load the properties metadata file so that the JSP page can
				// use the metadata to construct the editing form
				PropertiesMetaData metadata = PropertyService.getMainMetaData();
				request.setAttribute("metadata", metadata);

				String externalDir = PropertyService.getRecommendedExternalDir();
				
				if (externalDir == null) {
					throw new AdminException("Could not initialize property configuration "
									+ "page recommended application backup directory was null");
				}
				
				// Attempt to discover the following properties.  These will show
				// up in the configuration fields if nothing else is provided.
				PropertyService.setPropertyNoPersist("application.context",
						ServiceService.getRealApplicationContext());
				PropertyService.setPropertyNoPersist("server.name", SystemUtil
						.discoverServerName(request));
				PropertyService.setPropertyNoPersist("server.httpPort", SystemUtil
						.discoverServerPort(request));
				PropertyService.setPropertyNoPersist("server.httpSSLPort",
						SystemUtil.discoverServerSSLPort(request));
				PropertyService.setPropertyNoPersist("application.deployDir",
						SystemUtil.discoverDeployDir(request));
				PropertyService.setPropertyNoPersist("application.datafilepath",
						externalDir + FileUtil.getFS() + "data");
				PropertyService.setPropertyNoPersist("application.inlinedatafilepath",
						externalDir + FileUtil.getFS() + "inline-data");
				PropertyService.setPropertyNoPersist("application.documentfilepath",
						externalDir + FileUtil.getFS() + "documents");
				PropertyService.setPropertyNoPersist("application.tempDir",
						externalDir + FileUtil.getFS() + "temporary");
				PropertyService.setPropertyNoPersist("replication.logdir",
						externalDir + FileUtil.getFS() + "logs");
				PropertyService.setPropertyNoPersist("solr.homeDir",
						externalDir + FileUtil.getFS() + "solr-home");

				PropertyService.persistProperties();

				// Add the list of properties from metacat.properties to the request
				Vector<String> propertyNames = PropertyService.getPropertyNames();
				for (String propertyName : propertyNames) {
					request.setAttribute(propertyName, PropertyService.getProperty(propertyName));
				}

				// Check for any backup properties and apply them to the
				// request. These are properties from previous configurations. 
				// They keep the user from having to re-enter all values when 
				// upgrading. If this is a first time install, getBackupProperties 
				// will return null.
				SortedProperties backupProperties = null;
				if ((backupProperties = 
						PropertyService.getMainBackupProperties()) != null) {
					Vector<String> backupKeys = backupProperties.getPropertyNames();
					for (String key : backupKeys) {
						String value = backupProperties.getProperty(key);
						if (value != null) {
							request.setAttribute(key, value);
						}
					}
				}

				// Forward the request to the JSP page
				RequestUtil.forwardRequest(request, response,
						"/admin/properties-configuration.jsp", null);

			} catch (GeneralPropertyException gpe) {
				throw new AdminException("PropertiesAdmin.configureProperties - Problem getting or " + 
						"setting property while initializing system properties page: " + gpe.getMessage());
			} catch (MetacatUtilException mue) {
				throw new AdminException("PropertiesAdmin.configureProperties - utility problem while initializing "
						+ "system properties page:" + mue.getMessage());
			} catch (ServiceException se) {
				throw new AdminException("PropertiesAdmin.configureProperties - Service problem while initializing "
						+ "system properties page:" + se.getMessage());
			} 
		} else {
			// The configuration form is being submitted and needs to be
			// processed.
			Vector<String> validationErrors = new Vector<String>();
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> processingSuccess = new Vector<String>();

			MetacatVersion metacatVersion = null;
			
			try {
				metacatVersion = SystemUtil.getMetacatVersion();
				
				// For each property, check if it is changed and save it
				Vector<String> propertyNames = PropertyService.getPropertyNames();
				for (String name : propertyNames) {
					PropertyService.checkAndSetProperty(request, name);
				}

				// we need to write the options from memory to the properties
				// file
				PropertyService.persistProperties();

				// Validate that the options provided are legitimate. Note that
				// we've allowed them to persist their entries. As of this point
				// there is no other easy way to go back to the configure form
				// and preserve their entries.
				validationErrors.addAll(validateOptions(request));
				
				// Try to create data directories if necessary.
				String dataDir = PropertyService.getProperty("application.datafilepath");
				try {
					FileUtil.createDirectory(dataDir);
				} catch (UtilException ue) {
					String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + dataDir +
					" : " + ue.getMessage();
					logMetacat.error(errorString);
					validationErrors.add(errorString);
				}
				
				// Try to create inline-data directories if necessary.
				String inlineDataDir = PropertyService.getProperty("application.inlinedatafilepath");
				try {
					FileUtil.createDirectory(inlineDataDir);
				} catch (UtilException ue) {
					String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + inlineDataDir +
						" : " + ue.getMessage();
					logMetacat.error(errorString);
					validationErrors.add(errorString);
				}
				
				// Try to create document directories if necessary.
				String documentfilepath = PropertyService.getProperty("application.documentfilepath");
				try {
					FileUtil.createDirectory(documentfilepath);
				} catch (UtilException ue) {	
					String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + documentfilepath +
						" : " + ue.getMessage();
					logMetacat.error(errorString);
					validationErrors.add(errorString);
				}
				
				// Try to create temporary directories if necessary.
				String tempDir = PropertyService.getProperty("application.tempDir");
				try {
					FileUtil.createDirectory(tempDir);
				} catch (UtilException ue) {		
					String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + tempDir +
						" : " + ue.getMessage();
					logMetacat.error(errorString);
					validationErrors.add(errorString);
				}
				
				// Try to create temporary directories if necessary.
				String replLogDir = PropertyService.getProperty("replication.logdir");
				try {
					FileUtil.createDirectory(replLogDir);
				} catch (UtilException ue) {		
					String errorString = "PropertiesAdmin.configureProperties - Could not create directory: " + replLogDir +
						" : " + ue.getMessage();
					logMetacat.error(errorString);
					validationErrors.add(errorString);
				}
				
				// Try to create and initialize the solr-home directory if necessary.
				String solrHomePath = PropertyService.getProperty("solr.homeDir");
				String indexContext = PropertyService.getProperty("index.context");
				boolean solrHomeExists = new File(solrHomePath).exists();
				if (!solrHomeExists) {
					try {
						String metacatWebInf = ServiceService.getRealConfigDir();
						String metacatIndexSolrHome = metacatWebInf + "/../../" + indexContext + "/WEB-INF/classes/solr-home";
						// only attempt to copy if we have the source directory to copy from
						File sourceDir = new File(metacatIndexSolrHome);
						if (sourceDir.exists()) {
							FileUtil.createDirectory(solrHomePath);
							OrFileFilter fileFilter = new OrFileFilter();
							fileFilter.addFileFilter(DirectoryFileFilter.DIRECTORY);
							fileFilter.addFileFilter(new WildcardFileFilter("*"));
							FileUtils.copyDirectory(new File(metacatIndexSolrHome), new File(solrHomePath), fileFilter );
						}
					} catch (Exception ue) {	
						String errorString = "PropertiesAdmin.configureProperties - Could not initialize directory: " + solrHomePath +
								" : " + ue.getMessage();
						logMetacat.error(errorString);
						validationErrors.add(errorString);
					}
				} else {
					// check it
					if (!FileUtil.isDirectory(solrHomePath)) {
						String errorString = "PropertiesAdmin.configureProperties - SOLR home is not a directory: " + solrHomePath;
						logMetacat.error(errorString);
						validationErrors.add(errorString);
					}
				}
				
				//modify some params of the index context
				this.modifyIndexContextParams(indexContext);
				
				// make sure hazelcast.xml uses a unique group name
				this.modifyHazelcastConfig();
				
				// set permissions on the registry cgi scripts, least on *nix systems
				try {
					String cgiFiles = 
							PropertyService.getProperty("application.deployDir") 
							+ FileUtil.getFS() 
							+ PropertyService.getProperty("application.context") 
							+ PropertyService.getProperty("application.cgiDir")
							+ FileUtil.getFS() 
							+ "*.cgi";
					String [] command = {"sh", "-c", "chmod +x " + cgiFiles};
					Runtime rt = Runtime.getRuntime();
					Process pr = rt.exec(command);
					int ret = pr.waitFor();
					if (ret > 0) {
						logMetacat.error(IOUtils.toString(pr.getErrorStream()));
					}
				} catch (Exception ignorable) {
					/// just a warning
					logMetacat.warn("Could not set permissions on the registry scripts: " + ignorable.getMessage(), ignorable);
				}
				
				// write the backup properties to a location outside the 
				// application directories so they will be available after
				// the next upgrade
				PropertyService.persistMainBackupProperties();

			} catch (GeneralPropertyException gpe) {
				String errorMessage = "PropertiesAdmin.configureProperties - Problem getting or setting property while "
						+ "processing system properties page: " + gpe.getMessage();
				logMetacat.error(errorMessage);
				processingErrors.add(errorMessage);
			} 
			
			try {
				if (validationErrors.size() > 0 || processingErrors.size() > 0) {
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestFormErrors(request, validationErrors);
					RequestUtil.setRequestErrors(request, processingErrors);
					RequestUtil.forwardRequest(request, response, "/admin", null);
				} else {
					// Now that the options have been set, change the
					// 'propertiesConfigured' option to 'true'
					PropertyService.setProperty("configutil.propertiesConfigured",
							PropertyService.CONFIGURED);
					
					// if the db version is already the same as the metacat version,
					// update metacat.properties. Have to do this after
					// propertiesConfigured is set to CONFIGURED
					DBVersion dbVersion = DBAdmin.getInstance().getDBVersion();
					if (dbVersion != null && metacatVersion != null && 
							dbVersion.compareTo(metacatVersion) == 0) {
						PropertyService.setProperty("configutil.databaseConfigured", 
								PropertyService.CONFIGURED);
					}
					
					// Reload the main metacat configuration page
					processingSuccess.add("Properties successfully configured");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response, 
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("PropertiesAdmin.configureProperties - utility problem while processing system "
						+ "properties page: " + mue.getMessage());
			} catch (GeneralPropertyException gpe) {
				throw new AdminException("PropertiesAdmin.configureProperties - problem with properties while "
						+ "processing system properties page: " + gpe.getMessage());
			}
		}
	}
	
	/**
	 * In the web.xml of the Metacat-index context, there is a parameter:
	 * <context-param>
     * <param-name>metacat.properties.path</param-name>
     * <param-value>/metacat/WEB-INF/metacat.properties</param-value>
     * <description>The metacat.properties file for sibling metacat deployment. Note that the context can change</description>
     *  </context-param>
     *  It points to the default metacat context - knb. If we rename the context, we need to change the value of there.
	 */
	private void modifyIndexContextParams(String indexContext) {
	    if(indexContext != null) {
	        try {
	            String metacatContext = PropertyService.getProperty("application.context");
	            //System.out.println("the metacat context is ========================="+metacatContext);
	            if(metacatContext != null && !metacatContext.equals(DEFAULTMETACATCONTEXT)) {
	                String indexConfigFile = 
	                                PropertyService.getProperty("application.deployDir")
	                                + FileUtil.getFS()
	                                + indexContext
	                                + FileUtil.getFS() 
	                                + "WEB-INF"
	                                + FileUtil.getFS()
	                                + "web.xml";
	                //System.out.println("============================== the web.xml file is "+indexConfigFile);
	                String configContents = FileUtil.readFileToString(indexConfigFile, "UTF-8");
	                //System.out.println("============================== the content of web.xml file is "+configContents);
	                configContents = configContents.replace(BACKSLASH+DEFAULTMETACATCONTEXT+METACATPROPERTYAPPENDIX, BACKSLASH+metacatContext+METACATPROPERTYAPPENDIX);
	                FileUtil.writeFile(indexConfigFile, new StringReader(configContents), "UTF-8");
	            }
                
            } catch (Exception e) {
                String errorMessage = "PropertiesAdmin.configureProperties - Problem getting/setting the \"metacat.properties.path\" in the web.xml of the index context : " + e.getMessage();
                logMetacat.error(errorMessage);
            }
	    }
	}
	
	/**
	 * Changes the Hazelcast group name to match the current context
	 * This ensures we do not share the same group if multiple Metacat 
	 * instances are running in the same Tomcat container.
	 */
	private void modifyHazelcastConfig() {
        try {
            String metacatContext = PropertyService.getProperty("application.context");
            //System.out.println("the metacat context is ========================="+metacatContext);
            if (metacatContext != null) {
                String hzConfigFile = 
                                PropertyService.getProperty("application.deployDir")
                                + FileUtil.getFS()
                                + metacatContext
                                + FileUtil.getFS() 
                                + "WEB-INF"
                                + FileUtil.getFS()
                                + "hazelcast.xml";
                //System.out.println("============================== the web.xml file is "+indexConfigFile);
                String configContents = FileUtil.readFileToString(hzConfigFile, "UTF-8");
                //System.out.println("============================== the content of web.xml file is "+configContents);
                configContents = configContents.replace("<name>DataONE</name>", "<name>" + metacatContext + "</name>");
                FileUtil.writeFile(hzConfigFile, new StringReader(configContents), "UTF-8");
            }
            
        } catch (Exception e) {
            String errorMessage = "PropertiesAdmin.configureProperties - Problem setting groupName in hazelcast.xml: " + e.getMessage();
            logMetacat.error(errorMessage);
        }
	}

	/**
	 * Validate the most important configuration options submitted by the user.
	 * 
	 * @param request
	 *            the http request object
	 * 
	 * @return a vector holding error message for any fields that fail
	 *         validation.
	 */
	protected Vector<String> validateOptions(HttpServletRequest request) {
		Vector<String> errorVector = new Vector<String>();

		// Test database connectivity
		try {
			String dbError = DBAdmin.getInstance().validateDBConnectivity(
					request.getParameter("database.driver"),
					request.getParameter("database.connectionURI"),
					request.getParameter("database.user"),
					request.getParameter("database.password"));
			if (dbError != null) {
				errorVector.add(dbError);
			}
		} catch (AdminException ae) {
			errorVector.add("Could not instantiate database admin: "
					+ ae.getMessage());
		}

		return errorVector;
	}
	

}