/**
 *  '$RCSfile$'
 *    Purpose:  A Class that implements skins configuration methods
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SkinUtil;
import edu.ucsb.nceas.utilities.MetaDataProperty;
import edu.ucsb.nceas.utilities.PropertiesMetaData;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * Control the display of the skins configuration page and the processing
 * of the configuration values.
 */
public class SkinsAdmin extends MetacatAdmin {

	private static SkinsAdmin skinsAdmin = null;
	private static Log logMetacat = LogFactory.getLog(SkinsAdmin.class);

	/**
	 * private constructor since this is a singleton
	 */
	private SkinsAdmin() {}

	/**
	 * Get the single instance of the MetaCatConfig.
	 * 
	 * @return the single instance of MetaCatConfig
	 */
	public static SkinsAdmin getInstance() {
		if (skinsAdmin == null) {
			skinsAdmin = new SkinsAdmin();
		}
		return skinsAdmin;
	}
	
	/**
	 * Handle configuration of the application the first time that Metacat
	 * starts or when it is explicitly called in the url. Collect necessary
	 * configuration information from the administrator.
	 * 
	 * @param request
	 *            the http request information
	 * @param response
	 *            the http response to be sent back to the client
	 */
	public void configureSkins(HttpServletRequest request, HttpServletResponse response)
			throws AdminException {

		String processForm = request.getParameter("processForm");
		String formErrors = (String) request.getAttribute("formErrors");

		if (processForm == null || !processForm.equals("true") || formErrors != null) {
			// The servlet configuration parameters have not been set, or there
			// were form errors on the last attempt to configure, so redirect to
			// the web form for configuring metacat
			try {
				Vector<String> skinNames = SkinUtil.getSkinNames();
				String defaultStyle = 
					PropertyService.getProperty("application.default-style");

				request.setAttribute("defaultStyle", defaultStyle);
				
				// add skin metadata to the request.  The configuration form
				// will use this data to determine what the configuration 
				// fields should look like.
				HashMap<String, PropertiesMetaData> propertyMetaData = 
					SkinPropertyService.getMetaData();
				request.setAttribute("metadataMap", propertyMetaData);
				
				// get a deep copy of properties so we can override with 
				// backup values without changing originals
				HashMap<String, SortedProperties> originalPropertyMap =
					SkinPropertyService.getProperties();
				HashMap<String, HashMap<String, String>> localPropertyMap =
					new HashMap<String, HashMap<String, String>>();
				HashMap<String, SortedProperties> backupPropertiesMap = 
					SkinPropertyService.getBackupProperties();
				
				// Just in case a skin is not configured correctly, we will want to remove 
				// it from the skins list
				Vector<String> removeSkins = new Vector<String>();
					
				for (String skinName : skinNames) {
					// first, get a deep copy of this skin's properties

					SortedProperties skinProperties = originalPropertyMap.get(skinName);
					if (skinProperties == null) {
						logMetacat.error("SkinsAdmin.configureSkins - Could not find properties for skin: " + skinName);
						removeSkins.add(skinName);
						continue;
					}

					HashMap<String, String> originalSkinProperties = 
						skinProperties.getProperties();
					HashMap<String, String> localSkinProperties = 
						new HashMap<String, String>(originalSkinProperties);

					// now get the backup properties for this skin.  Overwrite the 
					// properties with backup properties.  This allows previously 
					// set properties to be preserved in an application upgrade.
					SortedProperties backupProperties = backupPropertiesMap.get(skinName);
					if (backupProperties == null) {
						logMetacat.warn("SkinsAdmin.configureSkins - Could not find backup properties for skin: "
								+ skinName);
					} else {
						for (String propertyName : backupProperties.getPropertyNames()) {
							localSkinProperties.put(propertyName, 
									backupProperties.getProperty(propertyName));
						}
					}
								
					localPropertyMap.put(skinName, localSkinProperties);					
				}	
				
				// If there are any skins for which we could not get properties, remove them from 
				// the skins list we will send to the configuration page.
				for (String skinName : removeSkins) {
					skinNames.remove(skinName);
				}
				
				request.setAttribute("skinNameList", skinNames);
				request.setAttribute("skinProperties", localPropertyMap);
				
				// Forward the request to the JSP page
				RequestUtil.forwardRequest(request, response,
						"/admin/skins-configuration.jsp", null);

			} catch (GeneralPropertyException pnfe) {
				throw new AdminException("SkinsAdmin.configureSkins - Problem getting property while " + 
						"initializing skins properties page: " + pnfe.getMessage());
			} catch (MetacatUtilException mue) {
				throw new AdminException("SkinsAdmin.configureSkins - utility problem while initializing "
						+ "skins properties page:" + mue.getMessage());
			} 

		} else {
			// The configuration form is being submitted and needs to be
			// processed.
			Vector<String> processingSuccess = new Vector<String>();
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> validationErrors = new Vector<String>();

			try {
				//Default style is global.
				String defaultStyle = request.getParameter("application.default-style");
				PropertyService.setProperty("application.default-style", defaultStyle);
				
				// For each skin property, check if it is changed and save it
				Vector<String> skinNames = SkinUtil.getSkinNames();
				for (String skinName : skinNames) {
					PropertiesMetaData skinMetaData = 
						SkinPropertyService.getMetaData(skinName);
					
					if (skinMetaData == null) {
						logMetacat.error("SkinsAdmin.configureSkins - Could not find metadata for skin: " + skinName);
						continue;
					}
					
					Set<String> metaDataKeySet = skinMetaData.getKeys();
					for (String metaDataKey : metaDataKeySet) {
						MetaDataProperty metaDataProperty = 
							skinMetaData.getProperties().get(metaDataKey);
						String fieldType = metaDataProperty.getFieldType();
						String newValue = request.getParameter(skinName + "." + metaDataKey);
						if (fieldType != null && fieldType.equals(MetaDataProperty.CHECKBOX_TYPE)) {
							if (newValue != null && newValue.equals("on")) {
								SkinPropertyService.checkAndSetProperty("true", skinName, metaDataKey);
							} else {
								SkinPropertyService.checkAndSetProperty("false", skinName, metaDataKey);
							}								
						} else {		
							SkinPropertyService.checkAndSetProperty(request, skinName, metaDataKey);
						}
					}

					// we need to write the options from memory to the
					// properties file
					SkinPropertyService.persistProperties(skinName);

					// Validate that the options provided are legitimate. Note
					// that we've allowed them to persist their entries. As of 
					// this point there is no other easy way to go back to the 
					// configure form and preserve their entries.
					validationErrors.addAll(validateOptions(request, skinName));

					// write the backup properties to a location outside the
					// application directories so they will be available after
					// the next upgrade
					SkinPropertyService.persistBackupProperties(skinName);
					PropertyService.persistMainBackupProperties();
				}
				
				// Now that the options have been set, change the 'skinsConfigured'
				// option to 'true' so that normal metacat requests will go through
				PropertyService.setProperty("configutil.skinsConfigured",
						PropertyService.CONFIGURED);
			} catch (GeneralPropertyException gpe) {
				String errorMessage = "SkinsAdmin.configureSkins - problem setting property while processing skins "
						+ "properties page: " + gpe.getMessage();
				logMetacat.error(errorMessage);
				processingErrors.add(errorMessage);
			} catch (IOException ioe) {
				String errorMessage = "SkinsAdmin.configureSkins - IO problem while processing skins "
						+ "properties page: " + ioe.getMessage();
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
					// 'skinsConfigured' option to 'true'
					PropertyService.setProperty("configutil.skinsConfigured",
							PropertyService.CONFIGURED);

					// Reload the main metacat configuration page
					processingSuccess.add("Skins successfully configured");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response,
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("SkinsAdmin.configureSkins - utility problem while processing skins "
						+ "properties page: " + mue.getMessage());
			} catch (GeneralPropertyException gpe) {
				String errorMessage = "SkinsAdmin.configureSkins - problem setting property while processing skins "
						+ "properties page: " + gpe.getMessage();
				logMetacat.error(errorMessage);
				processingErrors.add(errorMessage);
			}
		}
	}
	
	protected Vector<String> validateOptions(HttpServletRequest request) {
		Vector<String> errorVector = new Vector<String>();
		Vector<String> skinNames = null;
		try {
			skinNames = SkinUtil.getSkinNames();
		} catch (PropertyNotFoundException pnfe) {
			errorVector.add("Could not find skin names: " + pnfe.getMessage());
		}
		
		for (String skinName : skinNames) {
			errorVector.addAll(validateOptions(request, skinName));
		}
		
		return errorVector;
	}
	
	/**
	 * Validate the most important configuration options submitted by the user.
	 * 
	 * @return a vector holding error message for any fields that fail
	 *         validation.
	 */
	protected Vector<String> validateOptions(HttpServletRequest request, String skinName) {
		Vector<String> errorVector = new Vector<String>();

		//TODO MCD validate options.

		return errorVector;
	}
}