/**
 *  '$RCSfile$'
 *    Purpose: A Class that handles scheduling workflow jobs 
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

package edu.ucsb.nceas.metacat.workflow;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.DBTransform;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.scheduler.MetacatSchedulerException;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.ErrorSendingErrorException;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.metacat.util.ResponseUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * @author daigle
 *
 */
public class WorkflowSchedulerClient {
	
	private static WorkflowSchedulerClient WorkflowSchedulerClient = null;
	
	private static Logger logMetacat = Logger.getLogger(WorkflowSchedulerClient.class);

	/**
	 * private constructor since this is a singleton
	 */
	private WorkflowSchedulerClient()  {}
	
	/**
	 * Get the single instance of SchedulerService.
	 * 
	 * @return the single instance of SchedulerService
	 */
	public static WorkflowSchedulerClient getInstance() {
		if (WorkflowSchedulerClient == null) {
			WorkflowSchedulerClient = new WorkflowSchedulerClient();
		}
		return WorkflowSchedulerClient;
	}
	
	/**
	 * Scheduling a workflow
	 * 
	 * @param request
	 *            the servlet request object
	 * @param response
	 *            the servlet response object
	 * @param params
	 *            the request parameters
	 * @param username
	 *            the user
	 * @param groups
	 *            the user's group
	 */
	public void scheduleJob(HttpServletRequest request, HttpServletResponse response, 
			Hashtable<String, String[]> params, String username,
			String[] groups) throws MetacatSchedulerException {

		try {
			logMetacat.debug("WorkflowSchedulerClient.scheduleJob");
			params.put("action", new String[] { "scheduleWorkflow" });
			params.put("sessionid", new String[] { RequestUtil.getSessionData(request).getId() });
			
			String schedulerUrl = PropertyService.getProperty("workflowScheduler.url");
			String result = RequestUtil.get(schedulerUrl, params);
			
			String forwardTos[] = params.get("forwardto");
			
			if (forwardTos == null || forwardTos.length == 0) {
				ResponseUtil.sendSuccessXML(response, result);		
			} else {		
				String forwardTo = forwardTos[0];
				String qformat = null;
				
				String qformats[] = params.get("qformat");
				if (qformats == null || qformats.length == 0) {
					qformat = MetacatUtil.XMLFORMAT;
				} else {
					qformat = qformats[0];
				}
				
				String destination = "/style/skins/" + qformat + "/" + forwardTo;
				
				RequestUtil.forwardRequest(request, response, destination, params);
			} 
			
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.scheduleJob - "
					+ "property error when scheduling job: " + pnfe.getMessage());
		} catch (MetacatUtilException mue) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.scheduleJob - "
					+ "utility issue when scheduling job: " + mue.getMessage());
		} catch (ErrorSendingErrorException esee) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.scheduleJob - " 
					+ "Issue sending error when scheduling job: " + esee.getMessage());			
		}
	}
	
	/**
	 * Unschedule a job
	 * 
	 * @param request
	 *            the servlet request object
	 * @param response
	 *            the servlet response object
	 * @param params
	 *            the request parameters
	 * @param username
	 *            the user
	 * @param groups
	 *            the user's group
	 */
	public void unScheduleJob(HttpServletRequest request, HttpServletResponse response,
			Hashtable<String, String[]> params, String username,
			String[] groups) throws MetacatSchedulerException {
		try {
			logMetacat.debug("WorkflowSchedulerClient.unScheduleJob");
			
			params.put("action", new String[] { "unscheduleWorkflow" });
			params.put("sessionid", new String[] { RequestUtil.getSessionData(request).getId() });
			
			String schedulerUrl = PropertyService.getProperty("workflowScheduler.url");
			String result = RequestUtil.get(schedulerUrl, params);
			
			String forwardTos[] = params.get("forwardto");
			
			if (forwardTos == null || forwardTos.length == 0) {
				ResponseUtil.sendSuccessXML(response, result);		
			} else {		
				String forwardTo = forwardTos[0];
				String qformat = null;
				
				String qformats[] = params.get("qformat");
				if (qformats == null || qformats.length == 0) {
					qformat = MetacatUtil.XMLFORMAT;
				} else {
					qformat = qformats[0];
				}
				
				String destination = "/style/skins/" + qformat + "/" + forwardTo;
				
				RequestUtil.forwardRequest(request, response, destination, params);
			} 
				
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.unScheduleJob - "
					+ "property error when unscheduling job: " + pnfe.getMessage());
		} catch (MetacatUtilException mue) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.unScheduleJob - "
					+ "utility issue when unscheduling job: " + mue.getMessage());
		} catch (ErrorSendingErrorException esee) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.unScheduleJob - " 
					+ "Issue sending error when unscheduling job: " + esee.getMessage());			
		}
	}
	
	/**
	 * reschedule job
	 * 
	 * @param request
	 *            the servlet request object
	 * @param response
	 *            the servlet response object
	 * @param params
	 *            the request parameters
	 * @param username
	 *            the user
	 * @param groups
	 *            the user's group
	 */
	@SuppressWarnings("unchecked")
	public void reScheduleJob(HttpServletRequest request, HttpServletResponse response, 
			Hashtable<String, String[]> params, String username,
			String[] groups) throws MetacatSchedulerException {
		 		
		try {
			logMetacat.debug("WorkflowSchedulerClient.reScheduleJob");
			
			params.put("action", new String[] { "rescheduleWorkflow" });
			params.put("sessionid", new String[] { RequestUtil.getSessionData(request).getId() });
			
			String schedulerUrl = PropertyService.getProperty("workflowScheduler.url");
			String result = RequestUtil.get(schedulerUrl, params);

			String forwardTos[] = params.get("forwardto");
			
			if (forwardTos == null || forwardTos.length == 0) {
				ResponseUtil.sendSuccessXML(response, result);		
			} else {										
				RequestUtil.forwardRequest(request, response, params);
			} 
			
		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.reScheduleJob - "
					+ "property error when rescheduling job: " + pnfe.getMessage());
		} catch (MetacatUtilException mue) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.reScheduleJob - "
					+ "utility issue  when rescheduling job: " + mue.getMessage());
		} catch (ErrorSendingErrorException esee) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.reScheduleJob - " 
					+ "Issue sending error when rescheduling job: " + esee.getMessage());			
		}
	}
	
	/**
	 * delete job - to be implemented
	 */
	public void deleteJob(HttpServletRequest request, HttpServletResponse response,
			Hashtable<String, String[]> params, String username,
			String[] groups) throws MetacatSchedulerException {
		try {
			logMetacat.debug("WorkflowSchedulerClient.deleteJob");
			
			params.put("action", new String[] { "deleteScheduledWorkflow" });
			params.put("sessionid", new String[] { RequestUtil.getSessionData(request).getId() });
			
			String schedulerUrl = PropertyService.getProperty("workflowScheduler.url");
			String result = RequestUtil.get(schedulerUrl, params);

			String forwardTos[] = params.get("forwardto");
			
			if (forwardTos == null || forwardTos.length == 0) {
				ResponseUtil.sendSuccessXML(response, result);		
			} else {		
				String forwardTo = forwardTos[0];
				String qformat = null;
				
				String qformats[] = params.get("qformat");
				if (qformats == null || qformats.length == 0) {
					qformat = MetacatUtil.XMLFORMAT;
				} else {
					qformat = qformats[0];
				}
				
				String destination = "/style/skins/" + qformat + "/" + forwardTo;
				
				RequestUtil.forwardRequest(request, response, destination, params);
			} 			

		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.deleteJob - "
					+ "property error when deleting job: " + pnfe.getMessage());
		} catch (MetacatUtilException mue) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.deleteJob - "
					+ "utility issue  when deleting job: " + mue.getMessage());
		} catch (ErrorSendingErrorException esee) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.deleteJob - "
					+ "Issue sending error when deleting job: " + esee.getMessage());
		}
	}
	
	/**
	 * get job information for a given workflow in xml format
	 * 
	 * @param request
	 *            the servlet request object
	 * @param response
	 *            the servlet response object
	 * @param params
	 *            the request parameters
	 * @param username
	 *            the user
	 * @param groups
	 *            the user's group
	 */
	public void getJobs(HttpServletRequest request, HttpServletResponse response,
			Hashtable<String, String[]> params, String username,
			String[] groups) throws MetacatSchedulerException {
		
		try {
			logMetacat.debug("WorkflowSchedulerClient.getJobs");
			params.put("action", new String[] { "getScheduledWorkflow" });
			
			String schedulerUrl = PropertyService.getProperty("workflowScheduler.url");
			String result = RequestUtil.get(schedulerUrl, params);
			
			String qformats[] = params.get("qformat");			
			String qformat = null;
			if (qformats == null || qformats.length == 0) {
				qformat = MetacatUtil.XMLFORMAT;
			} else {
				qformat = qformats[0];
			}
            
			PrintWriter out = response.getWriter();
			
			DBTransform dbt = new DBTransform();
            dbt.transformXMLDocument(result,"-//NCEAS//scheduledWorkflowResultset//EN", "-//W3C//HTML//EN",
                    qformat, out, params, null);

		} catch (PropertyNotFoundException pnfe) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.getJobs - "
					+ "Property error when getting jobs for workflow: " + pnfe.getMessage());
		} catch (IOException ioe) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.getJobs - "
					+ "I/O error when getting jobs for workflow: " + ioe.getMessage());
		} catch (MetacatUtilException mue) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.getJobs - "
					+ "Metacat utility error  when getting jobs for workflow: " + mue.getMessage());
		} catch (ClassNotFoundException cnfe) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.getJobs - "
					+ "Error finding class when getting jobs for workflow: " + cnfe.getMessage());
		} catch (SQLException sqle) {
			throw new MetacatSchedulerException("WorkflowSchedulerClient.getJobs - "
					+ "SQL error when getting jobs for workflow: " + sqle.getMessage());
		} 
	}
}
