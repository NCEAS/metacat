/**
 *  '$RCSfile$'
 *    Purpose: A Class that handles scheduling tasks 
 *  Copyright: 2009 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author: daigle $'
 *     '$Date: 2009-03-25 13:41:15 -0800 (Wed, 25 Mar 2009) $'
 * '$Revision: 4861 $'
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

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.DateUtil;
import edu.ucsb.nceas.utilities.StatusUtil;
import edu.ucsb.nceas.utilities.UtilException;

public class SchedulerService extends BaseService {
	
	private static SchedulerService schedulerService = null;
	
	private static Logger logMetacat = Logger.getLogger(SchedulerService.class);
	
	private static Scheduler sched = null;

	/**
	 * private constructor since this is a singleton
	 */
	private SchedulerService() throws ServiceException {
		start();
	}
	
	/**
	 * Get the single instance of SchedulerService.
	 * 
	 * @return the single instance of SchedulerService
	 */
	public static SchedulerService getInstance() throws ServiceException {
		if (schedulerService == null) {
			schedulerService = new SchedulerService();
		}
		return schedulerService;
	}
	
	// this is a refreshable class
	public boolean refreshable() {
		return true;
	}

	// do the refresh
	public void doRefresh() throws ServiceException {
		stop();
		start();
	}

	// initialize the service
	public void start() throws ServiceException {
		try {
			// get the Quartz scheduler factory
			SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
			
			// get the scheduler
			sched = schedFact.getScheduler();
			sched.start();
			
			// get all existing jobs from the database
			ScheduledJobAccess jobAccess = new ScheduledJobAccess();
			HashMap<Long, ScheduledJobDAO> allJobsMap = jobAccess.getAllJobs(null);
			
			// reschedule each job that is in a SCHEDULED state.  
			for (Long jobId : allJobsMap.keySet()) {
				ScheduledJobDAO jobDAO = allJobsMap.get(jobId);
				String[] groups = {"scheduler_group"};
				if (jobDAO.getStatus().equals(StatusUtil.SCHEDULED)) {
					// send false as the last param so the reschedule method will not 
					// complain that the job is already in a SCHEDULED state.
					rescheduleJob(jobDAO, "scheduler_user", groups, false);
				}
			}			
			
		} catch (AccessException ae) {
			throw new ServiceException("SchedulerService.start - DB Access issue when starting scheduler: ", ae);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.start - Scheduler engine issue when starting scheduler: " + se.getMessage());
		}		
	}
	
	// Stop the scheduler
	public void stop() throws ServiceException {
		try {
			sched.shutdown();
			sched = null;
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.stop - Could not shut down scheduler: " + se.getMessage());
		}		
	}
	
	// this will eventually return the scheduler status
	protected Vector<String> getStatus() throws ServiceException {
		return new Vector<String>();
	}
	
	/**
	 * Schedule a job
	 * 
	 * @param jobDAO
	 *            the job data object to schedule
	 * @param username
	 *            the user that we will use to schedule
	 * @param groups
	 *            the user group that we will use to schedule
	 * @return a message saying that the job was scheduled
	 */
	public String scheduleJob(ScheduledJobDAO jobDAO, String username, String[] groups) throws ServiceException {
        
		// convert the start time to a calendar object
		Calendar startTimeCal = Calendar.getInstance();
        startTimeCal.setTime(jobDAO.getStartTime());
        
		// convert the start time to a calendar object
		Calendar endTimeCal = Calendar.getInstance();
        endTimeCal.setTime(jobDAO.getEndTime());
        
        // extract the job parameters from their data objects and put into a string map
        HashMap<String, String> jobParams = new HashMap<String, String>();
        HashMap<String, ScheduledJobParamDAO> jobParamDAOs = jobDAO.getAllJobParams();
        for (String paramName : jobParamDAOs.keySet()) {
        	jobParams.put(paramName, jobParamDAOs.get(paramName).getValue());   	
        }
        
        // schedule the job
		return scheduleJob(jobDAO.getName(), startTimeCal, endTimeCal, jobDAO.getIntervalValue(), 
				jobDAO.getIntervalUnit(), jobDAO.getClassName(), jobDAO.getGroupName(), 
				jobParams);
	}
	
	/**
	 * schedule a job
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param startCal
	 *            a calendar holding the start date of the job
	 * @param endCal
	 *            a calendar holding the end date of the job
	 * @param intervalValue
	 *            the run interval for the job
	 * @param intervalUnit
	 *            the unit of the run interval for the job
	 * @param jobClassName
	 *            the job class name
	 * @param jobGroup
	 *            the job group name
	 * @param jobParams
	 *            a map of additional job parameters
	 * @param username
	 *            the user name
	 * @param groups
	 *            the user's group name
	 * @return a message saying that the job was scheduled
	 */
	@SuppressWarnings("unchecked")
	public String scheduleJob(String jobName, Calendar startCal, Calendar endCal, int intervalValue, 
			String intervalUnit, String jobClassName, String jobGroup, HashMap<String, String> jobParams) 
	throws ServiceException {
        
        Class<Job> jobClass = null;
        try {
			jobClass = (Class<Job>) Class.forName(jobClassName);

			String startTimeStr = DateUtil.getHumanReadable(startCal);
			logMetacat.info("SchedulerService.scheduleJob - Scheduling job -- name: "
					+ jobName + ", class: " + jobClassName + ", start time: "
					+ startTimeStr + ", interval value: " + intervalValue
					+ ", interval unit: " + intervalUnit);

			// start the job in the job scheduler
			startJob(jobName, startCal, endCal, intervalValue, intervalUnit, jobClass, jobGroup,
					jobParams);

			// get a database access object and create the job in the database

			ScheduledJobAccess jobAccess = new ScheduledJobAccess();
			jobAccess.createJob(jobName, jobName, jobGroup, jobClass, startCal, endCal,
					intervalValue, intervalUnit, jobParams);
		} catch (AccessException ae) {
			try {
				deleteJob(jobName);
			} catch (Exception e) {
				// Not much we can do here but log this
				logMetacat.error("SchedulerService.scheduleJob - An access exception was thrown when writing job: "
						+ jobName + "to the db, and another exception was thrown when trying to remove the "
						+ "job from the scheduler.  The db and scheduler may be out of sync: " + e.getMessage());
			}
			throw new ServiceException("SchedulerService.scheduleJob - Error accessing db: ", ae);
		} catch (ClassNotFoundException cnfe) {
			throw new ServiceException("SchedulerService.scheduleJob - Could not find class with name: "
							+ jobClassName + " : " + cnfe.getMessage());
		} catch (UtilException ue) {
			throw new ServiceException("SchedulerService.scheduleJob - Could not schedule "
							+ "job due to a utility issue: " + ue.getMessage());
		}
		
		return "Scheduled: " + jobName;
	}
	
	/**
	 * schedule a job one time with a given delay. The job is registered in the
	 * scheduler, but not persisted to the database. The delay is calculated
	 * from the time that this method is called.
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param delay
	 *            the delay in seconds, minutes, hours or days from the time
	 *            this method is called (i.e. 30s, 5m, 2h, 1d)
	 * @param jobClassName
	 *            the job class name
	 * @param jobGroup
	 *            the job group name
	 * @param jobParams
	 *            a map of additional job parameters
	 * @param username
	 *            the user name
	 * @param groups
	 *            the user's group name
	 * @return a message saying that the job was scheduled
	 */
	@SuppressWarnings("unchecked")
	public String scheduleDelayedJob(String jobName, String delay, String jobClassName, 
			String jobGroup, HashMap<String, String> jobParams, 
			String username, String[] groups) throws ServiceException {
        
        Class<Job> jobClass = null;
        try {
			jobClass = (Class<Job>) Class.forName(jobClassName);
			
			Calendar startCal = getStartDateFromDelay(delay);

			logMetacat.info("SchedulerService.scheduleDelayedJob - Scheduling job -- name: "
					+ jobName + ", delay: " + delay + ", job class name: " + jobClassName);

			// start the job in the job scheduler
			startOneTimeJob(jobName, startCal, jobClass, jobGroup,
					jobParams);

		} catch (ClassNotFoundException cnfe) {
			throw new ServiceException("SchedulerService.scheduleJob - Could not find class with name: "
							+ jobClassName + " : " + cnfe.getMessage());
		} 
		
		return "Scheduled: " + jobName;
	}
	
	/**
	 * Unschedule a job. This removed it from the scheduler in memory and
	 * changed it's status to unscheduled in the database.
	 * 
	 * @param jobName
	 *            the name of the job to unschedule
	 * @param username
	 *            the user name
	 * @param groups
	 *            the user's group name
	 * @return a message saying the job was unscheduled
	 */
	public String unscheduleJob(String jobName, String username,
			String[] groups) throws ServiceException {
		
		ScheduledJobDAO jobDAO = null;
		try {
			ScheduledJobAccess jobAccess = new ScheduledJobAccess();
			jobDAO = jobAccess.getJobByName(jobName);
			if (jobDAO == null) {
				throw new ServiceException("SchedulerService.unscheduleJob - Could " 
						+ "not find job with name: " + jobName);
			}

			// remove the job from the scheduler
			sched.deleteJob(jobDAO.getName(), jobDAO.getGroupName());

			// change the status of the job to unscheduled in the database.
			jobDAO.setStatus(StatusUtil.UNSCHEDULED);
			jobAccess.updateJobStatus(jobDAO);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.unscheduleJob - Could not create "
							+ "scheduled job because of service issue: " + se.getMessage());
		} catch (AccessException ae) {
			throw new ServiceException("SchedulerService.unscheduleJob - Could not create "
							+ "scheduled job : " + jobDAO.getName() + " because of db access issue: ", ae);
		}
		
		return "Unscheduled: " + jobName;
	}
	
	/**
	 * Reschedule a job. This call will always check to make sure the status is not SCHEDULED
	 * @param jobDAO the job data object holding the information about the job to reschedule
	 * @param username
	 *            the user name
	 * @param groups
	 *            the user's group name
	 * @return a message saying that the job was rescheduled
	 */
	public String rescheduleJob(ScheduledJobDAO jobDAO, String username, String[] groups) throws ServiceException {
		return rescheduleJob(jobDAO, username, groups, true);
	}
	
	/**
	 * Reschedule a job.
	 * 
	 * @param jobDAO
	 *            the job data object holding the information about the job to
	 *            reschedule
	 * @param username
	 *            the user name
	 * @param groups
	 *            the user's group name
	 * @param checkStatus
	 *            if set to true, the method will check to make sure the status
	 *            is UNSCHEDULED before restarting. Otherwise, the method will
	 *            not check. This is so that we can restart a service at startup
	 *            that was running when metacat was shut down.
	 * @return a message saying that the job was rescheduled
	 */
	@SuppressWarnings("unchecked")
	public String rescheduleJob(ScheduledJobDAO jobDAO, String username, String[] groups, boolean checkStatus) throws ServiceException {
		
		try {
			ScheduledJobAccess jobAccess = new ScheduledJobAccess();
        
			if (jobDAO == null) {
				throw new ServiceException("SchedulerService.reScheduleJob - Cannot reschedule nonexistant job.");
			}
        
			// if we are checking status, make sure the job is in an UNSCHEDULED state in the db
			if (checkStatus && !jobDAO.getStatus().equals(StatusUtil.UNSCHEDULED)) {
				throw new ServiceException("SchedulerService.reScheduleJob - Cannot reschedule a job with status: " 
						+ jobDAO.getStatus() + ". Status must be 'unscheduled'.");
			}
        
			Calendar startCal = Calendar.getInstance();
	        startCal.setTime(jobDAO.getStartTime());
	        
			Calendar endCal = Calendar.getInstance();
	        endCal.setTime(jobDAO.getEndTime());
	        
	        HashMap<String, String> jobParams = new HashMap<String, String>();
	        HashMap<String, ScheduledJobParamDAO> jobParamDAOs = jobDAO.getAllJobParams();
	        for (String paramName : jobParamDAOs.keySet()) {
	        	jobParams.put(paramName, jobParamDAOs.get(paramName).getValue());   	
	        }
	        
	        Class<Job> jobClass = null;
	        String jobClassName = jobDAO.getClassName();
	        try {        	
	        	jobClass = (Class<Job>)Class.forName(jobClassName);     	
	        } catch (ClassNotFoundException cnfe) {
	        	throw new ServiceException("SchedulerService.scheduleJob - Could not find class with name: " 
	        			+ jobDAO.getClassName() + " : " + cnfe.getMessage());
	        } 
	        
	        String startTimeStr = DateUtil.getHumanReadable(startCal);
	        logMetacat.info("SchedulerService.rescheduleJob - name: " + jobDAO.getName() + ", class: " + jobClassName 
	        		+ ", start time: " + startTimeStr + ", interval value: " + jobDAO.getIntervalValue() 
	        		+ ", interval unit: " + jobDAO.getIntervalUnit());  
			
	        // start the job in the scheduler
			startJob(jobDAO.getName(), startCal, endCal, jobDAO.getIntervalValue(), jobDAO.getIntervalUnit(), jobClass, jobDAO.getGroupName(), jobParams);
	        
			// update the status in the database
			jobDAO.setStatus(StatusUtil.SCHEDULED);
			jobAccess.updateJobStatus(jobDAO);
			
		} catch (AccessException ae) {
			throw new ServiceException("SchedulerService.reScheduleJob - Could not reschedule "
					+ "job : " + jobDAO.getName() + " because of db access issue: ", ae);
		} catch (UtilException ue) {
			throw new ServiceException("SchedulerService.reScheduleJob - Could not reschedule "
					+ "job : " + jobDAO.getName() + " due to a utility issue: " + ue.getMessage());
		}
        		
		return "Rescheduled: " + jobDAO.getName();
	}

	/**
	 * Remove the job from the scheduler and set the job status to deleted in the database
	 * @param jobName
	 *            the string holding the name of the job to delete
	 * @param username
	 *            the user name
	 * @param groups
	 *            the user's group name
	 * @return a message saying that the job was deleted
	 */
	public String deleteJob(String jobName) throws ServiceException {
		
		ScheduledJobDAO jobDAO = null;
		try {	
			ScheduledJobAccess jobAccess = new ScheduledJobAccess();
			jobDAO = jobAccess.getJobByName(jobName);
		} catch (AccessException ae) {
			throw new ServiceException("SchedulerService.deleteJob - Could not delete "
					+ "scheduled job : " + jobDAO.getName() + " because of db access issue: ", ae);
		}
		
		return deleteJob(jobDAO);
	}
	
	/**
	 * Remove the job from the scheduler and set the job status to deleted in the database
	 * @param jobDAO
	 *            the job data object holding the information about the job to delete
	 * @param username
	 *            the user name
	 * @param groups
	 *            the user's group name
	 * @return a message saying that the job was deleted
	 */
	public String deleteJob(ScheduledJobDAO jobDAO) throws ServiceException {

		String groupName = "";
		try {

			sched.deleteJob(jobDAO.getName(), groupName);
			
			jobDAO.setStatus(StatusUtil.DELETED);
			ScheduledJobAccess jobAccess = new ScheduledJobAccess();
			jobAccess.updateJobStatus(jobDAO);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.deleteJob - Could not delete job: " + jobDAO.getName()
							+ " for group: " + groupName + " : " + se.getMessage());
		} catch (AccessException ae) {
			throw new ServiceException("SchedulerService.deleteJob - Could not delete "
					+ "scheduled job: " + jobDAO.getName() + " because of db access issue: ", ae);
		}
		
		return "Deleted: " + jobDAO.getName();
	}
	
	/**
	 * Get information about the job in XML format
	 * 
	 * @param jobId
	 *            the job for which we want the information
	 * @return an XML representation of the job
	 */
	public String getJobInfoXML(Long jobId) throws ServiceException {
		String jobInfoXML = "";
		
		try {
			ScheduledJobAccess jobAccess = new ScheduledJobAccess();
			ScheduledJobDAO scheduledJobDAO = jobAccess.getJob(jobId);
			
			jobInfoXML += 
				"<scheduledJobs>" + jobToXML(scheduledJobDAO) + "</scheduledJobs>";
			
		} catch (AccessException ae) {
			throw new ServiceException("SchedulerService.getJobInfoXML - Could not get job info for job: " 
					+ jobId, ae);
		}
		
		return jobInfoXML;
	}
	
	/**
	 * Get the information for jobs in a group in an xml format. A parameter
	 * key/value pair can be provided as well to limit the jobs returned.
	 * 
	 * @param groupName
	 *            the job group that we are searching for
	 * @param paramName
	 *            the parameter name that we are looking for. this is ignored if
	 *            null
	 * @param paramValue
	 *            the parameter value that we are looking for. this is ignored
	 *            if null
	 * @return an XML representation of the jobs.
	 */
	public String getJobsInfoXML(String groupName, String paramName, String paramValue) throws ServiceException {
		String jobInfoXML = "";
		
		try {
			ScheduledJobAccess jobAccess = new ScheduledJobAccess();
			HashMap<Long, ScheduledJobDAO> JobDAOMap = jobAccess.getJobsWithParameter(groupName, paramName, paramValue);
			
			jobInfoXML += "<scheduledWorkflowResultset>";
			for (Long jobDAOId : JobDAOMap.keySet()) {
				ScheduledJobDAO jobDAO = JobDAOMap.get(jobDAOId); 
				if (paramValue != null && paramName != null) {
					ScheduledJobParamDAO jobParamDAO = jobDAO.getJobParam(paramName);
					if(jobParamDAO != null && jobParamDAO.getValue().equals(paramValue)) {
						jobInfoXML +=  jobToXML(JobDAOMap.get(jobDAOId)); 
					}
				}
			}				
			jobInfoXML += "</scheduledWorkflowResultset>";
			
		} catch (AccessException ae) {
			throw new ServiceException("SchedulerService.getJobInfoXML - Could not get jobs info for group: " 
					+ groupName, ae);
		}
		
		return jobInfoXML;
	}
	
	/**
	 * Get the information for jobs in a group in an xml format. A parameter
	 * key/value pair can be provided as well to limit the jobs returned.
	 * 
	 * @param groupName
	 *            the job group that we are searching for
	 * @param paramName
	 *            the parameter name that we are looking for. this is ignored if
	 *            null
	 * @param paramValue
	 *            the parameter value that we are looking for. this is ignored
	 *            if null
	 * @return an XML representation of the jobs.
	 */
	public void getJobsInfoXML(String groupName, String paramName, String paramValue, PrintWriter pw) throws ServiceException {
		
		try {
			ScheduledJobAccess jobAccess = new ScheduledJobAccess();
			HashMap<Long, ScheduledJobDAO> JobDAOMap = jobAccess.getJobsWithParameter(groupName, paramName, paramValue);
			
			pw.print("<scheduledWorkflowResultset>");
			for (Long jobDAOId : JobDAOMap.keySet()) {
				ScheduledJobDAO jobDAO = JobDAOMap.get(jobDAOId); 
				if (paramValue != null && paramName != null) {
					ScheduledJobParamDAO jobParamDAO = jobDAO.getJobParam(paramName);
					if(jobParamDAO != null && jobParamDAO.getValue().equals(paramValue)) {
						pw.print(jobToXML(JobDAOMap.get(jobDAOId))); 
					}
				}
			}				
			pw.print("</scheduledWorkflowResultset>");
			
		} catch (AccessException ae) {
			throw new ServiceException("SchedulerService.getJobInfoXML - Could not get jobs info for group: " 
					+ groupName, ae);
		}
	}
	
	/**
	 * Convert a single job to XML
	 * @param scheduledJobDAO the job we want to convert
	 * @return an XML representation of the job
	 */
	public String jobToXML(ScheduledJobDAO scheduledJobDAO) throws ServiceException {
		String jobXML = "";

		if (scheduledJobDAO != null) {
			jobXML += "<scheduledJob>";
			jobXML += "<id>" + scheduledJobDAO.getId() + "</id>";
			jobXML += "<createTime>" + scheduledJobDAO.getCreateTime() + "</createTime>";
			jobXML += "<modTime>" + scheduledJobDAO.getModTime() + "</modTime>";
			jobXML += "<status>" + scheduledJobDAO.getStatus() + "</status>";
			jobXML += "<name>" + scheduledJobDAO.getName() + "</name>";
			jobXML += "<triggerName>" + scheduledJobDAO.getName() + "</triggerName>";
			jobXML += "<groupName>" + scheduledJobDAO.getGroupName() + "</groupName>";
			jobXML += "<className>" + scheduledJobDAO.getClassName() + "</className>";
			String startTimeString = null;
			try {
				startTimeString = 
					DateUtil.getHumanReadable(scheduledJobDAO.getStartTime());
			} catch (UtilException ue) {
				throw new ServiceException("SchedulerService.jobToXML - error getting human readable date for job: " 
						+ scheduledJobDAO.getId() + " ; " + ue.getMessage());
			}
			jobXML += "<startTime>" + startTimeString + "</startTime>";
			
			String endTimeString = null;
			try {
				if (scheduledJobDAO.getEndTime() != null) {
					endTimeString = DateUtil.getHumanReadable(scheduledJobDAO.getEndTime());
				}
			} catch (UtilException ue) {
				throw new ServiceException("SchedulerService.jobToXML - error getting human readable date for job: " 
						+ scheduledJobDAO.getId() + " ; " + ue.getMessage());
			}
			jobXML += "<endTime>" + endTimeString + "</endTime>";
			jobXML += "<intervalValue>" + scheduledJobDAO.getIntervalValue() + "</intervalValue>";
			jobXML += "<intervalUnit>" + scheduledJobDAO.getIntervalUnit() + "</intervalUnit>";

			HashMap<String, ScheduledJobParamDAO> jobParams = scheduledJobDAO
					.getAllJobParams();
			for (String jobParamKey : jobParams.keySet()) {
				jobXML += "<jobParam name='" + jobParams.get(jobParamKey).getKey() + "'>";
				jobXML += "<id>" + jobParams.get(jobParamKey).getId() + "</id>";
				jobXML += "<createTime>" + jobParams.get(jobParamKey).getCreateTime()
						+ "</createTime>";
				jobXML += "<modTime>" + jobParams.get(jobParamKey).getModTime()
						+ "</modTime>";
				jobXML += "<status>" + jobParams.get(jobParamKey).getStatus()
						+ "</status>";
				jobXML += "<jobId>" + jobParams.get(jobParamKey).getJobId() + "</jobId>";
				jobXML += "<key>" + jobParams.get(jobParamKey).getKey() + "</key>";
				jobXML += "<value>" + jobParams.get(jobParamKey).getValue() + "</value>";
				jobXML += "</jobParam>";
			}
			jobXML += "</scheduledJob>";
		}

		return jobXML;
	}
	
	/**
	 * Start a job in the scheduler
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param startCal
	 *            a calendar holding the start date of the job
	 * @param intervalValue
	 *            the run interval for the job
	 * @param intervalUnit
	 *            the unit of the run interval for the job
	 * @param jobClassName
	 *            the job class name
	 * @param jobGroup
	 *            the job group name
	 * @param jobParams
	 *            a map of additional job parameters
	 * @param username
	 *            the user name
	 * @param groups
	 *            the user's group name
	 */
	private void startJob(String jobName, Calendar startCal, Calendar endCal, int intervalValue, String intervalUnit,
			Class<Job> jobClass, String jobGroup, HashMap<String, String> jobParams) throws ServiceException { 
		
		JobDetail jobDetail = new JobDetail(jobName, jobGroup, jobClass);
		jobDetail.setJobDataMap(new JobDataMap(jobParams));
		
		// call the appropriate scheduling method depending on the schedule interval unit
		if (intervalUnit.equals("sec")) {
			scheduleSecondlyJob(jobName, jobClass, startCal, endCal, intervalValue, jobGroup, jobDetail);
		} else if (intervalUnit.equals("min")) {
			scheduleMinutelyJob(jobName, jobClass, startCal, endCal, intervalValue, jobGroup, jobDetail);
		} else if (intervalUnit.equals("hour")) {
			scheduleHourlyJob(jobName, jobClass, startCal, endCal, intervalValue, jobGroup, jobDetail);
		} else if (intervalUnit.equals("day")) {
			scheduleDailyJob(jobName, jobClass, startCal, endCal, intervalValue, jobGroup, jobDetail);
		} else if (intervalUnit.equals("week")) {
			scheduleWeeklyJob(jobName, jobClass, startCal, endCal, intervalValue, jobGroup, jobDetail);
		} else if (intervalUnit.equals("mon")) {
			scheduleMonthlyJob(jobName, jobClass, startCal, endCal, intervalValue, jobGroup, jobDetail);
		} else {
			throw new ServiceException("SchedulerService.scheduleJob - Could not interpret interval unit: " 
					+ intervalUnit + ". Unit must be sec, min, hour, day, week or mon");	
		}	
	}
	
	/**
	 * Schedule a job in the scheduler that has an interval based in seconds
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param jobClass
	 *            the job class object
	 * @param startTime
	 *            the time of the first run
	 * @param jobGroup
	 *            the group of this job
	 * @param jobDetail
	 *            the job detail object
	 */
	private void startOneTimeJob(String jobName, Calendar startTime, Class<Job> jobClass, String jobGroup, HashMap<String, String> jobParams) throws ServiceException {

		JobDetail jobDetail = new JobDetail(jobName, jobGroup, jobClass);
		jobDetail.setJobDataMap(new JobDataMap(jobParams));
		
		SimpleTrigger trigger = new SimpleTrigger();
		trigger.setName(jobName);
		trigger.setStartTime(startTime.getTime());
		trigger.setRepeatCount(1);

		try {
			sched.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.scheduleSecondlyJob - Could not create " 
					+ "scheduler: " + se.getMessage());
		}
	}
	
	/**
	 * Schedule a job in the scheduler that has an interval based in seconds
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param jobClass
	 *            the job class object
	 * @param startTime
	 *            the time of the first run
	 * @param interval
	 *            the interval in seconds between runs
	 * @param jobGroup
	 *            the group of this job
	 * @param jobDetail
	 *            the job detail object
	 */
	private void scheduleSecondlyJob(String jobName, Class<Job> jobClass, Calendar startTime, Calendar endTime, int interval, String jobGroup, JobDetail jobDetail) throws ServiceException {

		Trigger trigger = TriggerUtils.makeSecondlyTrigger(interval);
		trigger.setName(jobName);
		trigger.setStartTime(startTime.getTime());
		if (endTime != null) {
			trigger.setEndTime(endTime.getTime());
		}

		try {
			sched.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.scheduleSecondlyJob - Could not create " 
					+ "scheduler: " + se.getMessage());
		}
	}
	
	/**
	 * Schedule a job in the scheduler that has an interval based in minutes
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param jobClass
	 *            the job class object
	 * @param startTime
	 *            the time of the first run
	 * @param interval
	 *            the interval in minutes between runs
	 * @param jobGroup
	 *            the group of this job
	 * @param jobDetail
	 *            the job detail object
	 */
	private void scheduleMinutelyJob(String jobName, Class<Job> jobClass, Calendar startTime, Calendar endTime, int interval, String jobGroup, JobDetail jobDetail) throws ServiceException {

		Trigger trigger = TriggerUtils.makeMinutelyTrigger(interval);
		trigger.setName(jobName);
		trigger.setStartTime(startTime.getTime());
		if (endTime != null) {
			trigger.setEndTime(endTime.getTime());
		}

		try {
			sched.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.scheduleMinutelyJob - Could not create " 
					+ "scheduler: " + se.getMessage());
		}
	}
	
	/**
	 * Schedule a job in the scheduler that has an interval based in hours
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param jobClass
	 *            the job class object
	 * @param startTime
	 *            the time of the first run
	 * @param interval
	 *            the interval in hours between runs
	 * @param jobGroup
	 *            the group of this job
	 * @param jobDetail
	 *            the job detail object
	 */
	private void scheduleHourlyJob(String jobName, Class<Job> jobClass, Calendar startTime, Calendar endTime, int interval, String jobGroup, JobDetail jobDetail) throws ServiceException {

		Trigger trigger = TriggerUtils.makeHourlyTrigger(interval);
		trigger.setName(jobName);
		trigger.setStartTime(startTime.getTime());
		if (endTime != null) {
			trigger.setEndTime(endTime.getTime());
		}

		try {
			sched.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.scheduleHourlyJob - Could not create " 
					+  "scheduler: " + se.getMessage());
		}
	}
	
	/**
	 * Schedule a job in the scheduler that has an interval based in days
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param jobClass
	 *            the job class object
	 * @param startTime
	 *            the time of the first run
	 * @param interval
	 *            the interval in days between runs
	 * @param jobGroup
	 *            the group of this job
	 * @param jobDetail
	 *            the job detail object
	 */
	private void scheduleDailyJob(String jobName, Class<Job> jobClass, Calendar startTime, Calendar endTime, int interval, String jobGroup, JobDetail jobDetail) throws ServiceException {

		Trigger trigger = 
			TriggerUtils.makeDailyTrigger(startTime.get(Calendar.HOUR), startTime.get(Calendar.MINUTE));	
		trigger.setName(jobName);
		trigger.setStartTime(startTime.getTime());
		if (endTime != null) {
			trigger.setEndTime(endTime.getTime());
		}

		try {
			sched.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.scheduleHourlyJob - Could not create " 
					+ "scheduler: " + se.getMessage());
		}
	}
	
	/**
	 * Schedule a job in the scheduler that has an interval based in days
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param jobClass
	 *            the job class object
	 * @param startTime
	 *            the time of the first run
	 * @param interval
	 *            the interval in days between runs
	 * @param jobGroup
	 *            the group of this job
	 * @param jobDetail
	 *            the job detail object
	 */
	private void scheduleWeeklyJob(String jobName, Class<Job> jobClass, Calendar startTime, Calendar endTime, int interval, String jobGroup, JobDetail jobDetail) throws ServiceException {

		Trigger trigger = 
			TriggerUtils.makeWeeklyTrigger(startTime.get(Calendar.DAY_OF_WEEK), 
					startTime.get(Calendar.HOUR), startTime.get(Calendar.MINUTE));	
		trigger.setName(jobName);
		trigger.setStartTime(startTime.getTime());
		if (endTime != null) {
			trigger.setEndTime(endTime.getTime());
		}

		try {
			sched.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.scheduleHourlyJob - Could not create " 
					+ "scheduler: " + se.getMessage());
		}
	}
	
	/**
	 * Schedule a job in the scheduler that has an interval based in days
	 * 
	 * @param jobName
	 *            the name of the job
	 * @param jobClass
	 *            the job class object
	 * @param startTime
	 *            the time of the first run
	 * @param interval
	 *            the interval in days between runs
	 * @param jobGroup
	 *            the group of this job
	 * @param jobDetail
	 *            the job detail object
	 */
	private void scheduleMonthlyJob(String jobName, Class<Job> jobClass, Calendar startTime, Calendar endTime, int interval, String jobGroup, JobDetail jobDetail) throws ServiceException {

		Trigger trigger = 
			TriggerUtils.makeMonthlyTrigger(startTime.get(Calendar.DAY_OF_MONTH), 
					startTime.get(Calendar.HOUR), startTime.get(Calendar.MINUTE));	
		trigger.setName(jobName);
		trigger.setStartTime(startTime.getTime());
		if (endTime != null) {
			trigger.setEndTime(endTime.getTime());
		}

		try {
			sched.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException se) {
			throw new ServiceException("SchedulerService.scheduleHourlyJob - Could not create " 
					+ "scheduler: " + se.getMessage());
		}
	}
	
	/**
	 * Extract the start date from the delay value
	 * 
	 * @param delay
	 *            a string representing the start delay in <value><unit>
	 *            notation where value is an integer and unit is one of s,m,h or
	 *            d
	 * @return the calendar object holding the start date
	 */
	public Calendar getStartDateFromDelay(String delay) throws ServiceException {
		Calendar cal = Calendar.getInstance();
	
		char delayUnit = delay.trim().charAt(delay.length() - 1);
		String delayStrValue = delay.trim().substring(0, delay.length() - 1);
		int delayValue;
		try {
			delayValue = Integer.parseInt(delayStrValue);
		} catch (NumberFormatException nfe) {
			throw new ServiceException("SchedulerService.getStartDateFromDelay - Could not " 
					+ "parse delay value into an integer: " + delayStrValue + " : " + nfe.getMessage());
		}
		
		switch (delayUnit) {
		case 's':
		case 'S':
			cal.add(Calendar.SECOND, delayValue);
			break;
		case 'm':
		case 'M':
			cal.add(Calendar.MINUTE, delayValue);
			break;
		case 'h':
		case 'H':
			cal.add(Calendar.HOUR, delayValue);
			break;
		case 'd':
		case 'D':
			cal.add(Calendar.DAY_OF_YEAR, delayValue);
			break;
		default:
			throw new ServiceException("SchedulerService.getStartDateFromDelay - Could not " 
					+ "interpret delay unit: " + delayUnit + ". Unit must be s, m, h or d");	
		}
		
		return cal;
	}	
}
