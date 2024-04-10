package edu.ucsb.nceas.metacat.database;

import java.util.Vector;

import edu.ucsb.nceas.metacat.Version;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.StringUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * @author daigle@nceas.nceas.ucsb.edu
 *
 * DBVersion represents the current DBVersion information for this Metacat instance.
 */
public class DBVersion extends Version
{
	private Vector<String> updateScriptList = null;
	
	/**
	 * Create a database version. Get the update script list from properties for
	 * this version and save in a vector.
	 * 
	 * @param DBVersionId
	 *            a string representing the version of the database.
	 * @throws PropertyNotFoundException
	 */
    public DBVersion(String DBVersionId) throws PropertyNotFoundException {
    	super(DBVersionId);
		String updateScriptString = PropertyService.getProperty("database.upgradeVersion." + DBVersionId);
    	updateScriptList = StringUtil.toVector(updateScriptString, ',');
    }
	
    /**
     * Get the update script list for this version
     * @return a Vector of update script names.
     */
    public Vector<String> getUpdateScripts() {
    	return updateScriptList;
    }
}
