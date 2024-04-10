package edu.ucsb.nceas.metacat.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.dbadapter.AbstractDatabase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;

public class DatabaseService extends BaseService {
	
	private static DatabaseService databaseService = null;
	private static AbstractDatabase dbAdapter;	
	private static Log logMetacat = LogFactory.getLog(DatabaseService.class);

	/**
	 * private constructor since this is a singleton
	 */
	private DatabaseService() {
    	// Determine our db adapter class and create an instance of that class
        try {
            dbAdapter = (AbstractDatabase) createObject(PropertyService.getProperty("database.adapter"));
        } catch (Exception e) {
            logMetacat.error("Could not create dbAdaptor" + e.getMessage());
            e.printStackTrace();
        }
	}
	
	/**
	 * Get the single instance of DatabaseService.
	 * 
	 * @return the single instance of DatabaseService
	 */
	public static DatabaseService getInstance() {
		if (databaseService == null) {
			databaseService = new DatabaseService();
		}
		return databaseService;
	}
	
	public boolean refreshable() {
		return false;
	}
	
	public void doRefresh() throws ServiceException {
		return;
	}
	
	public void stop() throws ServiceException {
		return;
	}
	
    /**
	 * Instantiate a class using the name of the class at runtime
	 *
	 * @param className the fully qualified name of the class to instantiate
	 */
	public static Object createObject(String className) throws Exception {

		Object object = null;
		try {
			Class classDefinition = Class.forName(className);
			object = classDefinition.newInstance();
		} catch (InstantiationException e) {
			throw e;
		} catch (IllegalAccessException e) {
			throw e;
		} catch (ClassNotFoundException e) {
			throw e;
		}
		return object;
	}
	
	/**
	 * gets the database adapter
	 * 
	 * @return AbstractDatabase object for this application's database adapter.
	 */
	public AbstractDatabase getDBAdapter() {
		return dbAdapter;
	}

}
