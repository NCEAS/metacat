/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents an XML Text node and its contents,
 *             and can build itself from a database connection
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 *
 *   '$Author: daigle $'
 *     '$Date: 2008-10-21 15:22:01 -0700 (Tue, 21 Oct 2008) $'
 * '$Revision: 4469 $'
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

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.accesscontrol.AccessSection;

/**
 * A Class that represents an XML access rule. It include principal and
 * permission
 */
public class DistributionSection {
	// We don't know what kind of distribution this is
	public static final int UNKNOWN_DISTRIBUTION = 0;
	// Standard data, usually located locally
	public static final int DATA_DISTRIBUTION = 1;
	// Online data, usually available via a URL
	public static final int ONLINE_DATA_DISTRIBUTION = 2;
	// Inline data, located within the metadata (until we extract it).
	public static final int INLINE_DATA_DISTRIBUTION = 3;
	// Offline data, other kinds of data.  Usually physical media that is not available online
	public static final int OFFLINE_DATA_DISTRIBUTION = 4;

	private int distributionId;
	private int distributionType;
	private String dataFileName;
	private AccessSection accessSection = null;
	private Logger logMetacat = Logger.getLogger(DistributionSection.class);


	public DistributionSection(int id) {
		setDistributionType(UNKNOWN_DISTRIBUTION);
		setDistributionId(id);
	}

	/** 
	 * Set the distribution id 
	 */
	public void setDistributionId(int id) {
		distributionId = id;
	}

	/** 
	 * Get the access type 
	 */
	public int getDistributionId() {
		return distributionId;
	}
	
	/** 
	 * Set the distribution type 
	 */
	public void setDistributionType(int type) {
		distributionType = type;
	}

	/** 
	 * Get the online data file name
	 */
	public String getDataFileName() {
		return dataFileName;
	}
	
	/** 
	 * Set the online data file name
	 */
	public void setDataFileName(String name) {
		dataFileName = name;
	}

	/** 
	 * Get the access type 
	 */
	public int getDistributionType() {
		return distributionType;
	}
	
	public void setAccessSection(AccessSection aSection) {
		accessSection = aSection;
	}
	
	public AccessSection getAccessSection() {
		return accessSection;
	}
	
}
