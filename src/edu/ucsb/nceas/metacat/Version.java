/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat;

import java.util.Vector;

import edu.ucsb.nceas.utilities.StringUtil;


/**
 * @author jones
 *
 * Version represents the current version information for this Metacat instance.
 */
public class Version implements Comparable<Version>
{
	protected static int allowedVersionLevels = 4;
	protected int[] subversions = {0, 0, 0, 0}; 
	protected String versionString = null;
	
	/**
	 * Create a Version object, setting sub version levels based on the given
	 * version id.
	 * 
	 * @param versionID
	 */
    public Version(String versionID) {
    	setSubversions(versionID);
    	versionString = versionID;
    }
	
    /**
	 * Compare this Version object to another. Use the sub version levels to do
	 * the comparison. In that way, we can make sure that something like version
	 * 1.0.0.1 is less than 2.0.1.
	 * 
	 * @param anotherVersion
	 * @return -1 if this version is less than given version, 0 if they are
	 *         equal and 1 if this version is greater than given version.
	 */
	public int compareTo(Version anotherVersion) {
    	for (int i = 0; i < allowedVersionLevels; i++) {
    		if (this.getSubversion(i) < ((Version)anotherVersion).getSubversion(i)) {
    			return -1;
    		} else if (this.getSubversion(i) > ((Version)anotherVersion).getSubversion(i)) {
    			return 1;
    		}
    	}	
		return 0;
    }
	
	public int getSubversion(int level) {
		return subversions[level];
	}
	  
	/**
	 * Set each level (up to 4) of sub version based on the given version id.
	 * 
	 * @param versionID
	 *            a string representing a dotted version notation
	 */
    private void setSubversions(String versionID) throws NumberFormatException {
    	Vector<String> subversionStrings = StringUtil.toVector(versionID, '.');
    	for (int i = 0; i < subversionStrings.size() && i < allowedVersionLevels; i++) {
    		int subversion = Integer.parseInt(subversionStrings.elementAt(i));
    		this.subversions[i] = subversion;
    	}   	
    }
    
    public String getVersionString() {
    	return versionString;
    }
}
