package edu.ucsb.nceas.metacat.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public class MetacatHandlerPluginManager {
	
	private static MetacatHandlerPluginManager instance = null;
	
	private static Log log = LogFactory.getLog(MetacatHandlerPluginManager.class);
	
	private List<MetacatHandlerPlugin> handlers= new ArrayList<MetacatHandlerPlugin>();
	
	private MetacatHandlerPluginManager() {
		
		String[] configuredHandlers = null;
		try {
			String handlersString = PropertyService.getProperty("plugin.handlers");
			if (handlersString != null && handlersString.length() > 0) {
				configuredHandlers = handlersString.split(",");
				for (String className: configuredHandlers) {
					MetacatHandlerPlugin handlerInstance = null;
					try {
						Class<?> handlerClass = Class.forName(className);
						handlerInstance = (MetacatHandlerPlugin) handlerClass.newInstance();
					} catch (Exception e) {
						log.error("Problem initializing MetacatHandlerPlugin: " + className, e);
						continue;
					}
					handlers.add(handlerInstance);
				}
			}
		} catch (PropertyNotFoundException e) {
			log.warn("Could not find any MetacatPluginHandlers", e);
			return;
		}
	}
	
	public static MetacatHandlerPluginManager getInstance() throws ServiceException {
		if (instance == null) {
			try {
				instance = new MetacatHandlerPluginManager();
			} catch (Exception e) {
				throw new ServiceException(e.getMessage());
			}
		}
		return instance;
	}
	
	public void addHandler(MetacatHandlerPlugin plugin) {
		
	}
	
	public MetacatHandlerPlugin getHandler(String action) {
		for (MetacatHandlerPlugin plugin: handlers) {
			if (plugin.handlesAction(action)) {
				return plugin;
			}
		}
		return null;
	}
}
