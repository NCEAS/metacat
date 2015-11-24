/**
 *  '$RCSfile$'
 *    Purpose: A Class that manages database access of scheduled task 
 *             information.
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.quartz.Job;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.BaseAccess;
import edu.ucsb.nceas.utilities.StatusUtil;

public class ScheduledJobAccess extends BaseAccess {
	
	private Logger logMetacat = Logger.getLogger(ScheduledJobAccess.class);
	
	// Constructor
	public ScheduledJobAccess() throws AccessException {}
	
	/**
	 * Get a job based on it's id
	 * 
	 * @param jobId
	 *            the id of the job in the database
	 * @return the scheduled job data object that represents the desired job
	 */ 
	public ScheduledJobDAO getJob(Long jobId) throws AccessException {
		ScheduledJobDAO jobDAO = null;

		// first get the job from the db and put it into a DAO
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		try {
			conn = DBConnectionPool.getDBConnection("ScheduledJobAccess.getJob");
    		serialNumber = conn.getCheckOutSerialNumber();
    		
			String sql = "SELECT * FROM scheduled_job WHERE id = ? AND status != 'deleted'"; 
			pstmt = conn.prepareStatement(sql);

			pstmt.setLong(1, jobId);
			
			String sqlReport = "ScheduledJobAccess.getJob - SQL: " + sql;
			sqlReport += " [" + jobId + "]";
			
			logMetacat.info(sqlReport);
			
			pstmt.execute();
			
			ResultSet resultSet = pstmt.getResultSet();
			if (resultSet.next()) {
				jobDAO = populateDAO(resultSet);
			}
			
		} catch (SQLException sqle) {
			throw new AccessException("ScheduledJobAccess.getJob - SQL error when getting scheduled job: " 
					+ jobId  + " : "  + sqle.getMessage());
		} finally {
			closeDBObjects(pstmt, conn, serialNumber, logMetacat);
		}	
		
		// Now get all the job parameters and put those into a list of job parameter
		// DAOs and add that list to the job DAO
		ScheduledJobParamAccess jobParamAccess = new ScheduledJobParamAccess();
		Vector<ScheduledJobParamDAO> jobParamList = 
			jobParamAccess.getJobParamsForJobId(jobDAO.getId());
		
		for(ScheduledJobParamDAO jobParamDAO : jobParamList) {
			jobDAO.addJobParam(jobParamDAO);
		}
		
		return jobDAO;		
	}
	
	/**
	 * Get a job by it's name
	 * @param jobName the name of the job to get
	 * @return the scheduled job data object that represents the desired job
	 */
	public ScheduledJobDAO getJobByName(String jobName) throws AccessException {
		ScheduledJobDAO jobDAO = null;

		// first get the job from the db and put it into a DAO
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		try {
			conn = DBConnectionPool.getDBConnection("ScheduledJobAccess.getJobByName");
    		serialNumber = conn.getCheckOutSerialNumber();
    
			String sql = "SELECT * FROM scheduled_job WHERE name = ? AND status != 'deleted'"; 
			pstmt = conn.prepareStatement(sql);

			pstmt.setString(1, jobName);
			
			String sqlReport = "ScheduledJobAccess.getJobByName - SQL: " + sql;
			sqlReport += " [" + jobName + "]";
			
			logMetacat.info(sqlReport);
			
			pstmt.execute();
			
			ResultSet resultSet = pstmt.getResultSet();
			if (resultSet.next()) {
				jobDAO = populateDAO(resultSet);
			} else {
				throw new AccessException("ScheduledJobAccess.getJobByName - could not find scheduled job with name: " 
						+ jobName);
			}
			
		} catch (SQLException sqle) {
			throw new AccessException("ScheduledJobAccess.getJobByName - SQL error when getting scheduled job by name: " 
					+ jobName  + " : "  + sqle.getMessage());
		} finally {
			closeDBObjects(pstmt, conn, serialNumber, logMetacat);
		}	
		
		// Now get all the job parameters and put those into a list of job parameter
		// DAOs and add that list to the job DAO
		ScheduledJobParamAccess jobParamAccess = new ScheduledJobParamAccess();
		Vector<ScheduledJobParamDAO> jobParamList = 
			jobParamAccess.getJobParamsForJobId(jobDAO.getId());
		
		for(ScheduledJobParamDAO jobParamDAO : jobParamList) {
			jobDAO.addJobParam(jobParamDAO);
		}
		
		return jobDAO;		
	}
	
	/**
	 * Get all jobs that have a given parameter with a given value
	 * 
	 * @param groupName
	 *            the group to which the job belongs. This keeps us from
	 *            returning unrelated jobs that just happen to have a similar
	 *            parameter
	 * @param paramName
	 *            the name of the parameter we are looking for
	 * @param paramValue
	 *            the value of the parameter we are looking for
	 * @return a HashMap of job data objects with all jobs in a given group that
	 *         have parameters that match our parameter.
	 */
	public HashMap<Long, ScheduledJobDAO> getJobsWithParameter(String groupName, String paramName, String paramValue) throws AccessException {

		// first get all jobs
		HashMap<Long, ScheduledJobDAO> allJobsMap = getAllJobs(groupName);
		HashMap<Long, ScheduledJobDAO> jobsWithParamMap = new HashMap<Long, ScheduledJobDAO>();
		
		// then iterate through and grab the ones that have the desired parameter
		for (Long jobDAOId : allJobsMap.keySet()) {
			ScheduledJobDAO jobDAO = allJobsMap.get(jobDAOId); 
			if (paramValue != null && paramName != null) {
				ScheduledJobParamDAO jobParamDAO = jobDAO.getJobParam(paramName);
				if(jobParamDAO != null && jobParamDAO.getValue().equals(paramValue)) {
					jobsWithParamMap.put(jobDAOId, jobDAO);
				}
			}
		}		
		
		return jobsWithParamMap;
	}
	
	/**
	 * Get all jobs for a given group.  A group is typically a category that coincides with
	 * the job type (has it's own job implementation).  
	 * @param groupName the name of the group we want to search
	 * @return a HashMap of job data objects for the desired group.
	 */
	public HashMap<Long, ScheduledJobDAO> getAllJobs(String groupName) throws AccessException {
		ScheduledJobDAO jobDAO = null;
		
		HashMap<Long, ScheduledJobDAO> allJobDAOs = new HashMap<Long, ScheduledJobDAO>();

		// Get all jobs where the status is not deleted
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		try {
			conn = DBConnectionPool.getDBConnection("ScheduledJobAccess.getAllJobs");
    		serialNumber = conn.getCheckOutSerialNumber();
    
			String sql = "SELECT * FROM scheduled_job WHERE status != 'deleted'"; 
			if (groupName != null) {
				sql += " AND group_name = ?" ;
			}
			pstmt = conn.prepareStatement(sql);
			
			String sqlReport = "ScheduledJobAccess.getAllJobs - SQL: " + sql;
			if (groupName != null) {
				pstmt.setString(1, groupName);
				
				sqlReport += " [" + groupName + "]";
			}
			
			logMetacat.info(sqlReport);
						
			pstmt.execute();
			
			ResultSet resultSet = pstmt.getResultSet();
			while (resultSet.next()) {
				jobDAO = populateDAO(resultSet);
				
				allJobDAOs.put(jobDAO.getId(), jobDAO);
			}
			
			// Here we grab all job params and put them into the associated jobs.  THis
			// takes a little stress off the database by avoiding a join.
			ScheduledJobParamAccess jobParamAccess = new ScheduledJobParamAccess();
			Vector<ScheduledJobParamDAO> jobParamList = 
				jobParamAccess.getAllJobParams();
			
			for(ScheduledJobParamDAO jobParamDAO : jobParamList) {			
				Long jobId = jobParamDAO.getJobId();
				if( allJobDAOs.containsKey(jobId)) {
					allJobDAOs.get(jobId).addJobParam(jobParamDAO);
				}
			}
			
			return allJobDAOs;
			
		} catch (SQLException sqle) {
			throw new AccessException("ScheduledJobAccess.getJobByName - SQL error when getting all jobs : "  
					+ sqle.getMessage());
		} finally {
			closeDBObjects(pstmt, conn, serialNumber, logMetacat);
		}	
				
	}
	
	/**
	 * Create a job in the database.
	 * 
	 * @param name
	 *            the name of the job. This should be unique
	 * @param triggerName
	 *            the name of the trigger that is registered in the scheduler
	 *            service
	 * @param groupName
	 *            the group of the job
	 * @param jobClass
	 *            the class that implements the job functionality. The name of
	 *            this class will be extracted and put into the database.
	 * @param startTime
	 *            the time when the job should first run
	 * @param intervalValue
	 *            the amount of time between job runs.
	 * @param intervalUnit
	 *            the unit of time that the intervalValue represents. Valid
	 *            values are s,m,h, d and w
	 * @param jobParams
	 *            a map of parameters that are associated with this job.
	 */
	public void createJob(String name, String triggerName, String groupName,
			Class<Job> jobClass, Calendar startTime, Calendar endTime, int intervalValue,
			String intervalUnit, HashMap<String, String> jobParams)
			throws AccessException {
		
		// Create and populate a job data object
		ScheduledJobDAO jobDAO = new ScheduledJobDAO();
		jobDAO.setStatus(StatusUtil.SCHEDULED);
		jobDAO.setName(name);
		jobDAO.setTriggerName(name);
		jobDAO.setGroupName(groupName);
		jobDAO.setClassName(jobClass.getName());
		jobDAO.setStartTime(new Timestamp(startTime.getTimeInMillis()));
		jobDAO.setEndTime(new Timestamp(endTime.getTimeInMillis()));
		jobDAO.setIntervalValue(intervalValue);
		jobDAO.setIntervalUnit(intervalUnit);

		createJob(jobDAO, jobParams);
	}
	
	/**
	 * Create a job in the database
	 * @param jobDAO the job data object that holds necessary information
	 * @param jobParams a map of parameters that are associated with this job.
	 */
	public void createJob(ScheduledJobDAO jobDAO, HashMap<String, String> jobParams) throws AccessException {			
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		
		// First insert the job
		try {
			conn = DBConnectionPool.getDBConnection("ScheduledJobAccess.createJob");
    		serialNumber = conn.getCheckOutSerialNumber();
   
			String sql = 
				"INSERT INTO scheduled_job (date_created, date_updated, status, name, trigger_name, group_name, class_name, start_time, end_time, interval_value, interval_unit) " 
				+ "VALUES(now(), now(), ?, ?, ?, ?, ?, ?, ?, ?, ?)";		
			pstmt = conn.prepareStatement(sql);
		
			pstmt.setString(1, jobDAO.getStatus());
			pstmt.setString(2, jobDAO.getName());
			pstmt.setString(3, jobDAO.getTriggerName());
			pstmt.setString(4, jobDAO.getGroupName());
			pstmt.setString(5, jobDAO.getClassName());
			pstmt.setTimestamp(6, jobDAO.getStartTime());
			pstmt.setTimestamp(7, jobDAO.getEndTime());
			pstmt.setInt(8, jobDAO.getIntervalValue());
			pstmt.setString(9, jobDAO.getIntervalUnit());
			
			String sqlReport = "ScheduledJobAccess.createJob - SQL: " + sql 
				+ " [" + jobDAO.getStatus() + ","
				+ jobDAO.getName() + ","
				+ jobDAO.getTriggerName() + ","
				+ jobDAO.getGroupName() + ","
				+ jobDAO.getClassName() + ",(Timestamp)"
				+ jobDAO.getStartTime().toString() + ",";
			if(jobDAO.getEndTime() == null) {
				sqlReport += "null,";
			} else {
				sqlReport += jobDAO.getEndTime().toString() + ",";
			}
			sqlReport += jobDAO.getIntervalValue() + ","
				+ jobDAO.getIntervalUnit() + "]";
			logMetacat.info(sqlReport);
			
			pstmt.execute();
			
		} catch (SQLException sqle) {
			throw new AccessException("ScheduledJobAccess.createJob - SQL error when creating scheduled job " 
					+ jobDAO.getName()  + " : "  + sqle.getMessage());
		} finally {
			closeDBObjects(pstmt, conn, serialNumber, logMetacat);
		}	
		
		// Then iterate through the job params and insert them into the db
		if (jobParams.size() > 0) {
			ScheduledJobParamAccess scheduledJobParamsAccess = null;
			ScheduledJobDAO updatedJobDAO = null;
			try {
				updatedJobDAO = getJobByName(jobDAO.getName());
				scheduledJobParamsAccess = new ScheduledJobParamAccess();
				scheduledJobParamsAccess.createJobParams(updatedJobDAO.getId(), jobParams);
			} catch (AccessException ae) {
				if (updatedJobDAO != null) {
					updatedJobDAO.setStatus(StatusUtil.DELETED);
					updateJobStatus(updatedJobDAO);
					scheduledJobParamsAccess.deleteJobParams(updatedJobDAO.getId());
				} else {
					logMetacat.warn("ScheduledJobAccess.createJob - Tried to delete non-existant scheduled job: " 
							+ jobDAO.getName());
				}
			}
		}
	}
	
	/**
	 * Update the status of a job in the database
	 * 
	 * @param jobDAO
	 *            the job data object that we want to change. The status should
	 *            already have been updated in this object
	 */
	public void updateJobStatus(ScheduledJobDAO jobDAO) throws AccessException {	
		
		if (jobDAO == null) {
			throw new AccessException("ScheduledJobAccess.updateJobStatus - job DAO cannot be null.");
		}
		
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		
		try {
			conn = DBConnectionPool.getDBConnection("ScheduledJobAccess.createJob");
    		serialNumber = conn.getCheckOutSerialNumber();
   
			String sql = "UPDATE scheduled_job SET status = ? WHERE id = ?";	
			
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, jobDAO.getStatus());
			pstmt.setLong(2, jobDAO.getId());
			
			String sqlReport = "ScheduledJobAccess.deleteJob - SQL: " + sql
				+  " [" + jobDAO.getStatus() + "," + jobDAO.getId() + "]";
			logMetacat.info(sqlReport);

			pstmt.execute();
		} catch (SQLException sqle) {
			throw new AccessException("ScheduledJobAccess.deleteJob - SQL error when " 
					+ "deleting scheduled job " + jobDAO.getName()  + " : "  + sqle.getMessage());
		} finally {
			closeDBObjects(pstmt, conn, serialNumber, logMetacat);
		}		
		
	}
	
	/**
	 * Populate a job data object with the current row in a resultset
	 * 
	 * @param resultSet
	 *            the result set which is already pointing to the desired row.
	 * @return a scheduled job data object
	 */
	protected ScheduledJobDAO populateDAO(ResultSet resultSet) throws SQLException {

		ScheduledJobDAO jobDAO = new ScheduledJobDAO();
		jobDAO.setId(resultSet.getLong("id"));
		jobDAO.setCreateTime(resultSet.getTimestamp("date_created"));
		jobDAO.setModTime(resultSet.getTimestamp("date_updated"));
		jobDAO.setStatus(resultSet.getString("status"));
		jobDAO.setName(resultSet.getString("name"));
		jobDAO.setTriggerName(resultSet.getString("trigger_name"));
		jobDAO.setGroupName(resultSet.getString("group_name"));
		jobDAO.setClassName(resultSet.getString("class_name"));
		jobDAO.setStartTime(resultSet.getTimestamp("start_time"));
		jobDAO.setEndTime(resultSet.getTimestamp("end_time"));
		jobDAO.setIntervalValue(resultSet.getInt("interval_value"));
		jobDAO.setIntervalUnit(resultSet.getString("interval_unit"));

		return jobDAO;
	}
	
}