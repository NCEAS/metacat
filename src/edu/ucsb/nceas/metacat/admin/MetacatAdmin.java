/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements utility methods like:
 *             1/ Reding all doctypes from db connection
 *             2/ Reading DTD or Schema file from Metacat catalog system
 *             3/ Reading Lore type Data Guide from db connection
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jivka Bojilova
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
    public final static String IN_PROGRESS = "in progress";
    public final static String FAILED = "failed";
    public final static String COMPLETE = "complete";
    public final static String NOT_REQUIRED = "not required";
    public final static String PENDING = "pending";

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
    public static void updateUpgradeStatus(String propertyName, String status, boolean persist)
        throws GeneralPropertyException {
        PropertyService.setPropertyNoPersist(propertyName, status);
        //update the indicator of the whole upgrade process.
        if (status.equals(SUCCESS)) {
            // This sub upgrade process succeeded. If other sub process already succeeded, we need
            // to set the whole process success; otherwise, we keep its original value (do
            // nothing).
            Map<String, String> properties =
                PropertyService.getPropertiesByGroup("configutil.upgrade");
            Set<String> names = properties.keySet();
            boolean success = true;
            for (String name : names) {
                //we only look the sub processes (excluding the current one)
                if (!name.equals("configutil.upgrade.status") && !name.equals(propertyName)) {
                    if (!PropertyService.getProperty(name).equals(SUCCESS)) {
                        //found a failed or in_progress process. So the whole process should not
                        // be success
                        success = false;
                        break;
                    }
                }
            }
            if (success) {
                PropertyService.setPropertyNoPersist("configutil.upgrade.status", SUCCESS);
            }
        } else if (status.equals(FAILURE) || status.equals(IN_PROGRESS)) {
            //this sub upgrade process failed or is in progress, so the whole process will have
            // the same status as well
            PropertyService.setPropertyNoPersist("configutil.upgrade.status", status);
        } 
        if(persist && !Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"))) {
         // persist them all
            PropertyService.persistProperties();
            PropertyService.syncToSettings();
        }
    }

}
