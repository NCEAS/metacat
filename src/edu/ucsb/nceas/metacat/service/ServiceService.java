/**
 *  '$RCSfile$'
 *    Purpose: A Class that controls other services 
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author: daigle $'
 *     '$Date: 2008-10-09 09:53:18 -0700 (Thu, 09 Oct 2008) $'
 * '$Revision: 4429 $'
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

package edu.ucsb.nceas.metacat.service;

import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Hashtable;
import java.util.Set;

import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SystemUtil;

public class ServiceService {
	
	private static ServiceService serviceService = null;
	
    private static final String CONFIG_DIR = "WEB-INF";
	private static String REAL_CONFIG_DIR = "";
    
	private static final String SKIN_DIR = "/style/skins";
	private static String REAL_SKIN_DIR = "";
	
	public static String CONFIG_FILE_NAME = "";
	
	private static String REAL_APPLICATION_CONTEXT = null;
	
	private static Log logMetacat = LogFactory.getLog(ServiceService.class);
	private static Hashtable<String, BaseService> serviceList = new Hashtable<String, BaseService>();
	
	/**
	 * private constructor since this is a singleton
	 */
	private ServiceService(ServletContext servletContext) {
		REAL_CONFIG_DIR = servletContext.getRealPath(CONFIG_DIR);
		REAL_SKIN_DIR = servletContext.getRealPath(SKIN_DIR);
		
		CONFIG_FILE_NAME = servletContext.getInitParameter("configFileName");
		
		REAL_APPLICATION_CONTEXT = SystemUtil.discoverApplicationContext(servletContext);
	}
	
	/**
	 * Get the single instance of ServiceService.
	 * 
	 * @return the single instance of ServiceService
	 */
	public static ServiceService getInstance(ServletContext servletContext) {
		if (serviceService == null) {
			serviceService = new ServiceService(servletContext);
		}
		return serviceService;
	}
	
	/**
	 * Register a service with the system.
	 * 
	 * @param serviceName
	 *            the name of the service
	 * @param service
	 *            the singleton instance of the service
	 */
	public static void registerService(String serviceName, BaseService service)
			throws ServiceException {
		if (serviceList.containsKey(serviceName)) {
			throw new ServiceException("ServiceService.registerService - Service: " + serviceName + " is already registered."
					+ " Use ServiceService.reregister() to replace the service.");
		}
		logMetacat.info("ServiceService.registerService - Registering Service: " + serviceName);
		serviceList.put(serviceName, service);
	}
	
	/**
	 * Refresh a service.
	 * 
	 * @param serviceName
	 *            the name of the service to refresh
	 */
	public static void refreshService(String serviceName)
			throws ServiceException {
		if (!serviceList.containsKey(serviceName)) {
			throw new ServiceException("ServiceService.refreshService - Service: " + serviceName + " is not registered.");
		}
		
		BaseService baseService = serviceList.get(serviceName);
		if (!baseService.refreshable()) {
			throw new ServiceException("ServiceService.refreshService - Service: " + serviceName + " is not refreshable.");
		}
		logMetacat.info("ServiceService.refreshService - Refreshing Service: " + serviceName);
		baseService.refresh();
	}
	
	public static void stopAllServices() {
		Set<String> keySet = serviceList.keySet();
		
		for (String key : keySet) {
			try {
				logMetacat.info("ServiceService- stopAllServices: Stopping Service: " + key);
				serviceList.get(key).stop();
			} catch (ServiceException se) {
				logMetacat.error("ServiceService.stopAllServices - problem starting service: " 
						+ key + " : " + se.getMessage());
			}
		}
	}
	
	/**
	 * Convert the relative config directory to a full path
	 * @return the full config path
	 */
	public static String getRealConfigDir() throws ServiceException {
		if (serviceService == null) {
			throw new ServiceException("ServiceService.getRealConfigDir - Cannot access config dir before Service has been initialized");
		}
		return REAL_CONFIG_DIR;
	}
	
	/**
	 * Convert the relative skins directory to a full path
	 * @return the full skins directory path
	 */
	public static String getRealSkinDir() throws ServiceException {
		if (serviceService == null) {
			throw new ServiceException("ServiceService.getRealSkinDir - Cannot access skin dir before Service has been initialized");
		}
		return REAL_SKIN_DIR;
	}
	
	/**
	 * Get the servlet context name
	 * @return a string holding the context name
	 */
	public static String getRealApplicationContext() throws ServiceException {
		if (REAL_APPLICATION_CONTEXT == null) {
			throw new ServiceException("ServiceService.getRealApplicationContext - Application context name is null");
		}
		return REAL_APPLICATION_CONTEXT;
	}
	
}
