/**
 *  '$RCSfile$'
 *    Purpose: A Class that holds the data from the scheduled_job_params 
 *             table in the database. 
 *  Copyright: 2009 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author: daigle $'
 *     '$Date: 2009-03-23 13:56:56 -0800 (Mon, 23 Mar 2009) $'
 * '$Revision: 4854 $'
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

package edu.ucsb.nceas.metacat.scheduler;

import edu.ucsb.nceas.utilities.BaseDAO;

public class ScheduledJobParamDAO extends BaseDAO {

	private Long _jobId;
	private String _key;
	private String _value;
	
	// get parent job id
	public Long getJobId() {
		return _jobId;
	}
	
	// set parent job id
	public void setJobId(Long jobId) {
		_jobId = jobId;
	}
	
	// get parameter key
	public String getKey() {
		return _key;
	}
	
	// set parameter key
	public void setKey(String key) {
		_key = key;
	}
	
	// get parameter value
	public String getValue() {
		return _value;
	}
	
	// set parameter value
	public void setValue(String value) {
		_value = value;
	}
}