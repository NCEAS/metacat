/**
 *  '$RCSfile$'
 *    Purpose: A Class that holds the data from the scheduled_job 
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

import java.sql.Timestamp;
import java.util.HashMap;

import edu.ucsb.nceas.utilities.BaseDAO;


public class ScheduledJobDAO extends BaseDAO {
	
	public static String SECONDLY = "s";
	public static String MINUTELY = "m";
	public static String HOURLY = "h";
	public static String DAILY = "d";
	public static String WEEKLY = "w";

	private String _name;
	private String _triggerName;
	private String _groupName;
	private String _className;
	private Timestamp _startTime;
	private Timestamp _endTime;
	private int _intervalValue;
	private String _intervalUnit;
	private HashMap<String, ScheduledJobParamDAO> _jobParams = new HashMap<String, ScheduledJobParamDAO>();
	
	// get the name
	public String getName() {
		return _name;
	}
	
	// set the name
	public void setName(String name) {
		_name = name;
	}
	
	// get the trigger name
	public String getTriggerName() {
		return _triggerName;
	}
	
	// set the trigger name
	public void setTriggerName(String triggerName) {
		_triggerName = triggerName;
	}
	
	// get the group name
	public String getGroupName() {
		return _groupName;
	}
	
	// set the group name
	public void setGroupName(String groupName) {
		_groupName = groupName;
	}
	
	// get the class name
	public String getClassName() {
		return _className;
	}
	
	// set the class name
	public void setClassName(String className) {
		_className = className;
	}
	
	// get the start time
	public Timestamp getStartTime() {
		return _startTime;
	}
	
	// set the start time
	public void setStartTime(Timestamp startTime) {
		_startTime = startTime;
	}
	
	// get the end time
	public Timestamp getEndTime() {
		return _endTime;
	}
	
	// set the end time
	public void setEndTime(Timestamp endTime) {
		_endTime = endTime;
	}
	
	// get the interval value
	public int getIntervalValue() {
		return _intervalValue;
	}
	
	// set the interval value
	public void setIntervalValue(int intervalValue) {
		_intervalValue = intervalValue;
	}
	
	// get the interval unit
	public String getIntervalUnit() {
		return _intervalUnit;
	}
	
	// set the interval unit
	public void setIntervalUnit(String intervalUnit) {
		_intervalUnit = intervalUnit;
	}
	
	// get the job param with the given key
	public ScheduledJobParamDAO getJobParam(String key) {
		return _jobParams.get(key);
	}
	
	// get all the job params for this job
	public HashMap<String, ScheduledJobParamDAO> getAllJobParams() {
		return _jobParams;
	}
	
	// add a job param to this job
	public void addJobParam(ScheduledJobParamDAO jobParamDAO) {
		_jobParams.put(jobParamDAO.getKey(), jobParamDAO);
	}
}