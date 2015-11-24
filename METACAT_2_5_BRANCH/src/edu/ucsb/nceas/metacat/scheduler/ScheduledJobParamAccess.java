/**
 *  '$RCSfile$'
 *    Purpose: A Class that manages database access of scheduled task parameter 
 *             information.  These parameters allow us to maintain the concept of 
 *             a generic job with specific parameters.  Note that the methods in 
 *             this class are all protected.  They should only be accessed from the 
 *             ScheduledJobAccess class in conjunction with some action being taken 
 *             on a job.
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
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.BaseAccess;
import edu.ucsb.nceas.utilities.StatusUtil;

public class ScheduledJobParamAccess extends BaseAccess {
	
	private Logger logMetacat = Logger.getLogger(ScheduledJobParamAccess.class);
	
	// Constructor
	public ScheduledJobParamAccess() throws AccessException {}
	
	/**
	 * Insert job parameters into the database.
	 * 
	 * @param jobId
	 *            the id of the job that these parameters belong to
	 * @param jobParams
	 *            a map of the job parameters
	 */
	protected void createJobParams(Long jobId, HashMap<String, String> jobParams) throws AccessException {
		
		// iterate through and insert each job parameter
		for(String paramKey : jobParams.keySet()) {			
			ScheduledJobParamDAO jobParamsDAO = new ScheduledJobParamDAO();
			jobParamsDAO.setStatus(StatusUtil.ACTIVE);
			jobParamsDAO.setJobId(jobId);
			jobParamsDAO.setKey(paramKey);
			jobParamsDAO.setValue(jobParams.get(paramKey));
			
			PreparedStatement pstmt = null;
			DBConnection conn = null;
			int serialNumber = -1;
			
			try {
				conn = DBConnectionPool.getDBConnection("ScheduledJobParamAccess.createJobParams");
	    		serialNumber = conn.getCheckOutSerialNumber();

				String sql = 
					"INSERT INTO scheduled_job_params (date_created, date_updated, status, job_id, key, value) " 
					+ "VALUES(now(), now(), ?, ?, ?, ?)";		
				pstmt = conn.prepareStatement(sql);
			
				pstmt.setString(1, jobParamsDAO.getStatus());
				pstmt.setLong(2, jobParamsDAO.getJobId());
				pstmt.setString(3, jobParamsDAO.getKey());
				pstmt.setString(4, jobParamsDAO.getValue());
				
				logMetacat.info("SQL createJobParams - " + sql);
				logMetacat.info("SQL params:  [" + jobParamsDAO.getStatus() + ","
						+ jobParamsDAO.getJobId() + ","
						+ jobParamsDAO.getKey() + ","
						+ jobParamsDAO.getValue().toString() + "]");
				pstmt.execute();
				
			} catch (SQLException sqle) {
				// Just throw the exception.  The ScheduledJobAccess class should handle cleanup.
				throw new AccessException("ScheduledJobParamsAccess.createJobParams - SQL error when creating scheduled job parameter : "    
					 + sqle.getMessage());
			} finally {
				closeDBObjects(pstmt, conn, serialNumber, logMetacat);
			}	
		}
	}
	
	/**
	 * Remove change a job status to deleted in the database.
	 * 
	 * @param jobId
	 *            the id of the job to update
	 */
	protected void deleteJobParams(Long jobId) throws AccessException {
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		
		// change the status to deleted
		try {
			conn = DBConnectionPool.getDBConnection("ScheduledJobParamAccess.deleteJobParams");
    		serialNumber = conn.getCheckOutSerialNumber();

			String sql = "UPDATE scheduled_job_params SET status = ? WHERE jobId = ?";		
			pstmt = conn.prepareStatement(sql);
			
			pstmt.setString(1, StatusUtil.DELETED);
			pstmt.setLong(1, jobId);
			
			logMetacat.info("ScheduledJobParamsAccess.deleteJobParams - SQL: " + sql);

			pstmt.execute();
		} catch (SQLException sqle) {
			throw new AccessException("ScheduledJobParamsAccess.deleteJobParams - SQL error " 
					+ "when deleting scheduled job params for job" + jobId  + " : "  + sqle.getMessage());
		} finally {
			closeDBObjects(pstmt, conn, serialNumber, logMetacat);
		}	
		
	}	
	
	/**
	 * Get all the job parameters for a given job id
	 * 
	 * @param jobId
	 *            the job id whose parameters we want to return
	 * @return a list of the job parameter data objects
	 */
	protected Vector<ScheduledJobParamDAO> getJobParamsForJobId(Long jobId) throws AccessException {		
		Vector<ScheduledJobParamDAO> jobParamList = new Vector<ScheduledJobParamDAO>();
		
		// Get the job parameters for the job id
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		
		ScheduledJobParamDAO jobParamDAO = null;
		try {
			conn = DBConnectionPool.getDBConnection("ScheduledJobParamAccess.getJobParamsForJobId");
    		serialNumber = conn.getCheckOutSerialNumber();

			String sql = "SELECT * FROM scheduled_job_params WHERE job_id = ? AND status != 'deleted'"; 
			pstmt = conn.prepareStatement(sql);

			pstmt.setLong(1, jobId);
			
			logMetacat.info("SQL getJobParamsForJobId - " + sql);
			logMetacat.info("SQL params: [" + jobId + "]");
			
			pstmt.execute();
			
			ResultSet resultSet = pstmt.getResultSet();
			while (resultSet.next()) {
				jobParamDAO = populateDAO(resultSet);
				
				jobParamList.add(jobParamDAO);
			}
			
			return jobParamList;
			
		} catch (SQLException sqle) {
			throw new AccessException("ScheduledJobAccess.getJobParamsForJobId - SQL error when getting " 
					+ "scheduled job parameter for job id: " + jobId  + " : "  + sqle.getMessage());
		} finally {
			closeDBObjects(pstmt, conn, serialNumber, logMetacat);
		}
		
	}
	
	/**
	 * Get all job parameters
	 * 
	 * @return a list of all job parameters in the database
	 */
	protected Vector<ScheduledJobParamDAO> getAllJobParams() throws AccessException {		
		Vector<ScheduledJobParamDAO> jobParamList = new Vector<ScheduledJobParamDAO>();
		
		// get all job parameters
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		
		ScheduledJobParamDAO jobParamDAO = null;
		try {
			conn = DBConnectionPool.getDBConnection("ScheduledJobParamAccess.getAllJobParams");
    		serialNumber = conn.getCheckOutSerialNumber();

			String sql = "SELECT * FROM scheduled_job_params WHERE status != 'deleted'"; 
			pstmt = conn.prepareStatement(sql);
			
			logMetacat.info("SQL getAllJobParams - " + sql);
			
			pstmt.execute();
			
			ResultSet resultSet = pstmt.getResultSet();
			while (resultSet.next()) {
				jobParamDAO = populateDAO(resultSet);
				
				jobParamList.add(jobParamDAO);
			}
			
			return jobParamList;
			
		} catch (SQLException sqle) {
			throw new AccessException("ScheduledJobParamAccess.getAllJobParams - SQL error when getting " 
					+ "scheduled job parameters : "  + sqle.getMessage());
		} finally {
			closeDBObjects(pstmt, conn, serialNumber, logMetacat);
		}
		
	}
	
	/**
	 * Populate a job parameter data object with the current row in a resultset
	 * 
	 * @param resultSet
	 *            the result set which is already pointing to the desired row.
	 * @return a scheduled job data parameter object
	 */
	protected ScheduledJobParamDAO populateDAO(ResultSet resultSet) throws SQLException {

		ScheduledJobParamDAO jobParamDAO = new ScheduledJobParamDAO();
		jobParamDAO.setId(resultSet.getLong("id"));
		jobParamDAO.setCreateTime(resultSet.getTimestamp("date_created"));
		jobParamDAO.setModTime(resultSet.getTimestamp("date_updated"));
		jobParamDAO.setStatus(resultSet.getString("status"));
		jobParamDAO.setJobId(resultSet.getLong("job_id"));
		jobParamDAO.setKey(resultSet.getString("key"));
		jobParamDAO.setValue(resultSet.getString("value"));

		return jobParamDAO;
	}
 }