/**
 *  '$RCSfile$'
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either DBVersion 2 of the License, or
 * (at your option) any later DBVersion.
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
