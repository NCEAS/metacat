package edu.ucsb.nceas.metacat.admin.upgrade;
/**
 *  '$RCSfile$'
 *    Purpose: A Class for upgrading the database to version 1.5
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Saurabh Garg
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


import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.SortedProperties;

public class Upgrade2_0_0 implements UpgradeUtilityInterface {

    public boolean upgrade() throws AdminException {
        boolean success = true;
        
        // empty replicated files
        UpgradeEmptyReplicatedDataFile emptyDataUpgrader = new UpgradeEmptyReplicatedDataFile();
        success = emptyDataUpgrader.upgrade();
        
        // node data datetime
        UpgradeNodeDataDatetime dateTimeUpgrade = new UpgradeNodeDataDatetime();
        success = success && dateTimeUpgrade.upgrade();
        
        // Optionally upgrade GUIDs to use DOI syntax if configured
        GenerateGlobalIdentifiers globalIDUpgrade = new GenerateGlobalIdentifiers();
        success = success && globalIDUpgrade.upgrade();

    	return success;
    }
    
    public static void main(String [] ags){

        try {
        	// set up the properties based on the test/deployed configuration of the workspace
        	SortedProperties testProperties = 
				new SortedProperties("test/test.properties");
			testProperties.load();
			String metacatContextDir = testProperties.getProperty("metacat.contextDir");
			PropertyService.getInstance(metacatContextDir + "/WEB-INF");
			// now run it
            Upgrade2_0_0 upgrader = new Upgrade2_0_0();
	        upgrader.upgrade();
	        
        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
