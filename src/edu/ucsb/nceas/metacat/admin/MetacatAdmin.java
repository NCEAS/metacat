package edu.ucsb.nceas.metacat.admin;

import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.GeneralPropertyException;

/**
 * A suite of utility classes for querying DB
 * 
 */
public abstract class MetacatAdmin {
    
    public final static String SUCCESS = "success";
    public final static String FAILURE = "failure";
    public final static String IN_PROGRESS = "in_progress";
	
	/**
	 * Require subclasses to implement a properties validator.
	 * 
	 * @return a vector holding error message for any fields that fail
	 *         validation.
	 */
	protected abstract Vector<String> validateOptions(HttpServletRequest request);
	
	
	/**
	 * Update the status of an sub upgrade process (e.g. database). It will also update the status
	 * of the property which indicates the whole upgrade process (database, and java upgrade).
	 * @param propertyName  the name of property needs to be updated
	 * @param status  the new status should be set
	 * @throws GeneralPropertyException
	 */
	public static void updateUpgradeStatus(String propertyName, String status, boolean persist) throws GeneralPropertyException {
	    PropertyService.setPropertyNoPersist(propertyName, status);
	    //update the indicator of the whole upgrade process.
	    if (status.equals(SUCCESS)) {
	        // This sub upgrade process succeeded. If other sub process already succeeded, we need to set the whole process success; otherwise, we keep its original value (do nothing).
	        Map<String, String> properties = PropertyService.getPropertiesByGroup("configutil.upgrade");
	        Set<String> names = properties.keySet();
	        boolean success = true;
	        for (String name : names) {
	            //we only look the sub processes (excluding the current one)
	            if (!name.equals("configutil.upgrade.status") && !name.equals(propertyName)) {
	                if (!PropertyService.getProperty(name).equals(SUCCESS)) {
	                    //found a failed or in_progress process. So the whole process should not be success
	                    success = false;
	                    break;
	                }
	            }
	        }
	        if (success) {
	            PropertyService.setPropertyNoPersist("configutil.upgrade.status", SUCCESS);
	        }
	    } else if (status.equals(FAILURE) || status.equals(IN_PROGRESS)) {
            //this sub upgrade process failed or is in progress, so the whole process will have the same status as well
            PropertyService.setPropertyNoPersist("configutil.upgrade.status", status);
        } 
	    if(persist && !Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"))) {
	     // persist them all
	        PropertyService.persistProperties();
	        PropertyService.syncToSettings();
	    }
	}

}
