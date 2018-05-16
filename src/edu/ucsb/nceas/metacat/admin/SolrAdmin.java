/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements database configuration methods
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author: daigle $'
 *     '$Date: 2008-07-24 13:47:03 -0700 (Thu, 24 Jul 2008) $'
 * '$Revision: 4155 $'
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
import java.io.IOException;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.CoreStatus;
import org.dataone.service.exceptions.UnsupportedType;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.SolrServerFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * Control the display of the Solr configuration page and the processing
 * of the configuration values.
 */
public class SolrAdmin extends MetacatAdmin {

	private static SolrAdmin solrAdmin = null;
	private Logger logMetacat = Logger.getLogger(SolrAdmin.class);
	//possibilities:
    //1. Create - both core and solr-home doesn't exist. Create solr-home and register the core.
	public static final String CREATE = "create";
	//2. Register - core doesn't exist, but the solr-home directory does exist without schema update indication.
	public static final String REGISTER = "register";
	//3. RegisterWithUpdate - core doesn't exist, but the solr-home directory does exist with schema update indication.
    public static final String REGISTERANDUPDATE = "registerAndUpdate";
    //4. CreateWithWarning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home doesn't exist. 
	// Ask users if they really want to register the existing core with a new solr-home or keep the original one. If keeping the original one, no update will need.
	public static final String CREATEWITHWARN = "createWithWarn";
	//4.1 CreateOrUpdateWithWarning - core does exist, but its the instance directory is different to the solr-home in the properties file and solr home doesn't exist.
	//Ask users if they really want to register the existing core with a new solr-home or keep the original one. If keeping the original one, a schema update will need
	public static final String CREATEORUPDATEWITHWARN = "createOrUpdateWithWarn";
	//5. RegisterWithWarning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home does exist and no schema update. 
    // Ask users if they really want to register the existing core with a new solr-home or just skip configuration.
    public static final String REGISTERWITHWARN = "RegisterWithWarn";
     //6. RegisterAndUpdateWithWarning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home does exist and needing schema update. 
    // Ask users if they really want to register the existing core with a new solr-home or just skip configuration.
    public static final String REGISTERANDUPDATEWITHWARN = "RegisterAndUpdateWithWarn";
    //7. Skip - both core and solr-home does exist. And the core's instance directory is as same as the the solr-home. There is no schema update indication
	public static final String KEEP = "KEEP";
    //8. Update - both core and solr-home does exist. And the core's instance directory is as same as the the solr-home. There is a schema update indication
	public static final String UPDATE = "update";
	public static final String UNKNOWN = "Unkown";
	
	public static final String ACTION = "action";
	
	public static final String CONFIRMEDSOLRHOME = "confirmed-solr-home";
	public static final String CURRENTCOREINSTANCEDIR = "core-current-instance-dir";

	/**
	 * private constructor since this is a singleton
	 */
	private SolrAdmin() throws AdminException {

	}

	/**
	 * Get the single instance of SolrDAdmin.
	 * 
	 * @return the single instance of SolrAdmin
	 */
	public static SolrAdmin getInstance() throws AdminException {
		if (solrAdmin == null) {
		    synchronized(SolrAdmin.class) {
		        if(solrAdmin == null) {
		            solrAdmin = new SolrAdmin();
		        }
		    }
			
		}
		return solrAdmin;
	}

	/**
	 * Handle configuration of the solr the first time that Metacat starts
	 * or when it is explicitly called. Collect necessary update information
	 * from the administrator.
	 * 
	 * @param request
	 *            the http request information
	 * @param response
	 *            the http response to be sent back to the client
	 */
	public void configureSolr(HttpServletRequest request,
			HttpServletResponse response) throws AdminException {

		String processForm = request.getParameter("processForm");
		String bypass = request.getParameter("bypass");
		String formErrors = (String) request.getAttribute("formErrors");

		if (processForm == null || !processForm.equals("true") || formErrors != null) {
			// The servlet configuration parameters have not been set, or there
			// were form errors on the last attempt to configure, so redirect to
			// the web form for configuring metacat
            // This branch will show the initial solr admin page.
			try {
				// get the current configuration values
			    String baseURL = PropertyService.getProperty("solr.baseURL");
				//String username = PropertyService.getProperty("solr.admin.user");
				//String password = PropertyService.getProperty("solr.password");
				String coreName = PropertyService.getProperty("solr.coreName");
				String solrHomePath = PropertyService.getProperty("solr.homeDir");
				String osUser =  PropertyService.getProperty("solr.osUser");
			
				
				boolean solrHomeExists = new File(solrHomePath).exists();
                if (solrHomeExists) {
                    // check it
                    if (!FileUtil.isDirectory(solrHomePath)) {
                        throw new AdminException("SolrAdmin.configureProperties - SOLR home is not a directory: " + solrHomePath);
                    }
                }
                request.setAttribute("solrHomeExist", (Boolean) solrHomeExists);
                request.setAttribute("solrHomeValueInProp", solrHomePath);
                //check the solr-home for given core name
                String solrHomeForGivenCore =getInstanceDir(coreName);
                request.setAttribute("solrCore", coreName);
                if(solrHomeForGivenCore != null) {
                   //the given core  exists
                    request.setAttribute("solrHomeForGivenCore", solrHomeForGivenCore);
                }
                
                boolean updateSchema = false;//it may change base on the metacat.properties file.
                logMetacat.info("SolrAmdin.confgureSolr -the solr-home on the properties is "+solrHomePath +" and doe it exist? "+solrHomeExists);
                logMetacat.info("SolrAmdin.confgureSolr - the instance directory for the core "+coreName +" is "+solrHomeForGivenCore+" If it is null, this means the core doesn't exit.");
                logMetacat.info("SolrAmdin.confgureSolr - in this upgrade/installation, do we need to update the schema file?"+updateSchema);
                if(solrHomeForGivenCore == null && !solrHomeExists) {
                    //action 1 - create (no core and no solr home)
                    request.setAttribute(ACTION, CREATE);
                } else if (solrHomeForGivenCore == null && solrHomeExists && !updateSchema) {
                    //action 2 - register (no core but having solr home and no schema update)
                    request.setAttribute(ACTION, REGISTER);
                } else if (solrHomeForGivenCore == null && solrHomeExists && updateSchema) {
                    //action 3 - register (no core but having solr home and having schema update)
                    request.setAttribute(ACTION, REGISTERANDUPDATE);
                } else if (solrHomeForGivenCore != null && !solrHomeForGivenCore.equals(solrHomePath) && !solrHomeExists && !updateSchema) {
                   //action 4. createWithWarnning - core does exist, but its instance directory is different to the solr-home in the properties file, and solr home doesn't exist. 
                    // Ask users if they really want to register the existing core with a new solr-home or keep the original one. If keeping the original one, no update will need.
                    request.setAttribute(ACTION, CREATEWITHWARN);
                } else if (solrHomeForGivenCore != null && !solrHomeForGivenCore.equals(solrHomePath) && !solrHomeExists && updateSchema) {
                    //4.1 CreateOrUpdateWithWarning - core does exist, but its the instance directory is different to the solr-home in the properties file and solr home doesn't exist.
                    //Ask users if they really want to register the existing core with a new solr-home or keep the original one. If keeping the original one, a schema update will need
                    request.setAttribute(ACTION, CREATEORUPDATEWITHWARN);
                } else if (solrHomeForGivenCore != null && !solrHomeForGivenCore.equals(solrHomePath) && solrHomeExists && !updateSchema) {
                  //action 5. RegisterWithWarnning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home does exist and no schema update. 
                   // Ask users if they really want to register the existing core with a new solr-home or just skip configuration.
                    request.setAttribute(ACTION, REGISTERWITHWARN);
                } else if(solrHomeForGivenCore != null && !solrHomeForGivenCore.equals(solrHomePath) && solrHomeExists && updateSchema) {
                    //action 6. RegisterAndUpdateWithWarnning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home does exist and needing schema update. 
                    // Ask users if they really want to register the existing core with a new solr-home or just skip configuration.
                    request.setAttribute(ACTION, REGISTERANDUPDATEWITHWARN);
                } else if (solrHomeForGivenCore != null && solrHomeForGivenCore.equals(solrHomePath) && solrHomeExists && !updateSchema) {
                    //action 7. Keep - both core and solr-home does exist. And the core's instance directory is as same as the the solr-home. There is no schema update indication
                    request.setAttribute(ACTION, KEEP);
                } else if (solrHomeForGivenCore != null && solrHomeForGivenCore.equals(solrHomePath) && solrHomeExists && updateSchema) {
                    //action 8. Update - both core and solr-home does exist. And the core's instance directory is as same as the the solr-home. There is a schema update indication
                    request.setAttribute(ACTION, UPDATE);
                } else {
                    request.setAttribute(ACTION, UNKNOWN);
                }
                
				// Forward the request to the JSP page
				RequestUtil.forwardRequest(request, response,
						"/admin/solr-configuration.jsp", null);
			} catch (GeneralPropertyException gpe) {
				throw new AdminException("SolrAdmin.configureSolr - Problem getting or " + 
						"setting property while initializing solr page: " + gpe.getMessage());
			} catch (MetacatUtilException mue) {
				throw new AdminException("SolrAdmin.configureSolr- utility problem while initializing "
						+ "solr page:" + mue.getMessage());
			} catch (UnsupportedType e) {
			    throw new AdminException("SolrAdmin.configureSolr- umsupported type problem while initializing "
                        + "solr page:" + e.getMessage());
            } catch (ParserConfigurationException e) {
                throw new AdminException("SolrAdmin.configureSolr- parser configuration problem while initializing "
                        + "solr page:" + e.getMessage());
            } catch (IOException e) {
                throw new AdminException("SolrAdmin.configureSolr- io problem while initializing "
                        + "solr page:" + e.getMessage());
            } catch (SAXException e) {
                throw new AdminException("SolrAdmin.configureSolr- SAX problem while initializing "
                        + "solr page:" + e.getMessage());
            } catch (SolrServerException e) {
                throw new AdminException("SolrAdmin.configureSolr- solr problem while initializing "
                        + "solr page:" + e.getMessage());
            } 
		} else if (bypass != null && bypass.equals("true")) {
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> processingSuccess = new Vector<String>();
			
			// Bypass the geoserver configuration. This will not keep
			// Metacat from running.
			try {
				PropertyService.setProperty("configutil.solrserverConfigured",
						PropertyService.BYPASSED);
				
			} catch (GeneralPropertyException gpe) {
				String errorMessage = "SolrAdmin.configureSolr - Problem getting or setting property while "
					+ "processing system properties page: " + gpe.getMessage();
				logMetacat.error(errorMessage);
				processingErrors.add(errorMessage);
			}
			try {
				if (processingErrors.size() > 0) {
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestErrors(request, processingErrors);
					RequestUtil.forwardRequest(request, response, "/admin", null);
				} else {			
					// Reload the main metacat configuration page
					processingSuccess.add("Solr configuration successfully bypassed");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response, 
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("Solr.configureSolr - utility problem while processing solr services "
						+ "solr services page: " + mue.getMessage());
			} 
		
		} else {
			// The configuration form is being submitted and needs to be
			// processed. This branch handles the action which users choose.
			Vector<String> validationErrors = new Vector<String>();
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> processingSuccess = new Vector<String>();

			try {
				// Validate that the options provided are legitimate. Note that
				// we've allowed them to persist their entries. As of this point
				// there is no other easy way to go back to the configure form
				// and preserve their entries.
				validationErrors.addAll(validateOptions(request));
				String action = request.getParameter(ACTION);
				logMetacat.info("SolrAdmin.configureSolr - the action which users choose is "+action);
				if(action != null && action.equals(CREATE)) {
				    //action 1 - create (no core and no solr home)
				   createSolrHome();
				   registerSolrCore();
				} else if (action != null && action.equals(REGISTER)) {
				    //action 2 - register (no core but having solr home and no schema update)
				    registerSolrCore();
				} else if (action != null && action.equals(REGISTERANDUPDATE)) {
				  //action 3 - register (no core but having solr home and having schema update)
				    registerSolrCore();
				    updateSolrSchema();
				} else if (action != null && (action.equals(CREATEWITHWARN) || action.equals(CREATEORUPDATEWITHWARN) || action.equals(REGISTERWITHWARN) || action.equals(REGISTERANDUPDATEWITHWARN))) {
                    //action 4. createWithWarnning - core does exist, but its instance directory is different to the solr-home in the properties file, and solr home doesn't exist. 
                    // Ask users if they really want to register the existing core with a new solr-home or just skip configuration.
				    //4.1 CreateOrUpdateWithWarning - core does exist, but its the instance directory is different to the solr-home in the properties file and solr home doesn't exist.
                    //Ask users if they really want to register the existing core with a new solr-home or keep the original one. If keeping the original one, a schema update will need
				    //action 5. RegisterWithWarnning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home does exist and no schema update. 
	                // Ask users if they really want to register the existing core with a new solr-home or just skip configuration.
				    //action 6. RegisterAndUpdateWithWarnning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home does exist and needing schema update. 
                    // Ask users if they really want to register the existing core with a new solr-home or just skip configuration.
				    String confirmedSolrHome = request.getParameter(CONFIRMEDSOLRHOME); 
				    logMetacat.info("SolrAdmin.configureSolr - the confirmed the solr home is "+confirmedSolrHome);
	                String coreCurrentInstanceDir = request.getParameter(CURRENTCOREINSTANCEDIR);
	                logMetacat.info("SolrAdmin.configureSolr - the current solr instance directory of the core is "+coreCurrentInstanceDir);
				    if (confirmedSolrHome != null && coreCurrentInstanceDir != null) {
                        if(confirmedSolrHome.equals(coreCurrentInstanceDir)) {
                            //user choose to use the current instance dir. So we need to reset solr home
                            PropertyService.setPropertyNoPersist("solr.homeDir", confirmedSolrHome);
                            // persist them all
                            PropertyService.persistProperties();
                            PropertyService.syncToSettings();
                            // save a backup in case the form has errors, we reload from these
                            PropertyService.persistMainBackupProperties();
                            if(action.equals(CREATEORUPDATEWITHWARN) || action.equals(REGISTERANDUPDATEWITHWARN)) {
                              //we also need to update the schema
                                updateSolrSchema();
                            }
                        } else {
                            //user choose to use the new solr home.
                            if(action.equals(CREATEWITHWARN) || action.equals(CREATEORUPDATEWITHWARN)) {
                                createSolrHome();
                            } else if (action.equals(REGISTERWITHWARN)) {
                                registerSolrCore();
                            } else if (action.equals(REGISTERANDUPDATEWITHWARN)) {
                                registerSolrCore();
                                updateSolrSchema();
                            }
                            
                        }
                    } else {
                        throw new AdminException("User doesn't confirm the solr home property or the core's current directory is null.");
                    }  
				} else if (action != null && action.equals(KEEP)) {
				  //action 7. Keep - both core and solr-home does exist. And the core's instance directory is as same as the the solr-home. There is no schema update indication
				   //do nothing
				    
				} else if (action != null && action.equals(UPDATE)) {
				  //action 8. Update - both core and solr-home does exist. And the core's instance directory is as same as the the solr-home. There is a schema update indication
				    updateSolrSchema();
				} else {
				    throw new Exception("The action "+action+" can't be handled in the solr configuration.");
				}
				//}
			}  catch (Exception gpe) {
				String errorMessage = "SolrAdmin.configureSolr - Problem processing the solr setting since " + gpe.getMessage();
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
					PropertyService.setProperty("configutil.solrserverConfigured",
							PropertyService.CONFIGURED);
					
					// Reload the main metacat configuration page
					processingSuccess.add("Solr server was successfully configured");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response, 
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("SolrAdmin.configureSolr - utility problem while processing solr services "
						+ "solr services page: " + mue.getMessage());
			} catch (GeneralPropertyException gpe) {
				throw new AdminException("SolrAdmin.configureSolr - problem with properties while "
						+ "processing solr services configuration page: " + gpe.getMessage());
			}
		}
	}

	/**
	 * Validate the most important configuration options submitted by the user.
	 * 
	 * @return a vector holding error message for any fields that fail
	 *         validation.
	 */
	protected Vector<String> validateOptions(HttpServletRequest request) {
		Vector<String> errorVector = new Vector<String>();

		// TODO MCD validate options.

		return errorVector;
	}
	

	/**
	 * Get the instance directory of a given core.
	 * @param coreName  the core will be looked for
	 * @return the instance directory of the core. The null will be return if we can't find the core.
	 * @throws UnsupportedType
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws SolrServerException
	 */
	private String getInstanceDir(String coreName) throws UnsupportedType, ParserConfigurationException, IOException, SAXException, SolrServerException {
	    String instanceDir = null;
	    SolrClient client = SolrServerFactory.createSolrServer();
	    CoreAdminRequest adminRequest = new CoreAdminRequest();
	    CoreStatus status = adminRequest.getCoreStatus(coreName, client);
	    if(status != null) {
	        try {
	            //The getInstanceDirectory method doesn't handle the scenario that the core doesn't exist. It will give a null exception.
	            //So it has to swallow the null pointer exception here.
	            instanceDir = status.getInstanceDirectory();
	        } catch (NullPointerException e) {
	           
	        }
	    }
	    return instanceDir;
	 }
	
	
	/**
	 * Create the solr home when it is necessary
	 * @throws PropertyNotFoundException 
	 */
	private void createSolrHome() throws Exception  {
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
                    } else {
                        String errorString = "SolrAdmin.createSolrHome - the source director : " + sourceDir+" which should contain the original solr configuration doesn't exist";
                        logMetacat.error(errorString);
                        throw new Exception(errorString);
                    }
                } catch (Exception ue) {    
                    String errorString = "SolrAdmin.createSolrHome - Could not initialize directory: " + solrHomePath +
                            " : " + ue.getMessage();
                    logMetacat.error(errorString);
                    throw new Exception(errorString);
                }
            } else {
                // check it
                if (!FileUtil.isDirectory(solrHomePath)) {
                    String errorString = "SolrAdmin.createSolrHome - existing SOLR home is not a directory: " + solrHomePath;
                    logMetacat.error(errorString);
                    throw new Exception(errorString);
                } 
            }
	}
	
	/**
	 * Register the given core on the solr server
	 * @throws Exception
	 */
	 private void registerSolrCore() throws Exception{
	     String coreName = PropertyService.getProperty("solr.coreName");
         String instanceDir = PropertyService.getProperty("solr.homeDir");
	     try {
	         SolrClient client = SolrServerFactory.createSolrServer();
	         CoreAdminRequest adminRequest = new CoreAdminRequest();
	         adminRequest.createCore(coreName, instanceDir, client);
	     } catch (Exception e) {
	         String error = "SolrAdmin.registerSolrCore - Couldn't register the solr core - "+ coreName +" with the instance directory - "+ instanceDir +" since "+e.getMessage();
	         logMetacat.error(error, e);
	          throw new Exception(error);
	     }
     }
	 
	 private void updateSolrSchema() {
	     
	 }
}
