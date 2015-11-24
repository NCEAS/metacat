/**
 *  '$RCSfile$'
 *    Purpose: An abstract base class for scheduler classes.
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

import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class BaseScheduler {
	
	// Schedule a job by extracting job specific information and registering it with the scheduler 
	// service.  
	public abstract void scheduleJob(HttpServletRequest request, HttpServletResponse response, 
            Hashtable<String, String[]> params) throws MetacatSchedulerException;
	
	// Unschedule a job in the scheduler service. 
	public abstract void unscheduleJob(HttpServletRequest request, HttpServletResponse response, 
			Hashtable<String, String[]> params, String username, String[] groups) throws MetacatSchedulerException;
	
	// Unschedule a job in the scheduler service. 
	public abstract void rescheduleJob(HttpServletRequest request, HttpServletResponse response, 
			Hashtable<String, String[]> params, String username, String[] groups) throws MetacatSchedulerException;
	
	// Delete a job in the scheduler service. 
	public abstract void deleteJob(HttpServletRequest request, HttpServletResponse response, 
			Hashtable<String, String[]> params, String username, String[] groups) throws MetacatSchedulerException;
	
	// get all jobs frpm the scheduler service for a specific type of scheduler.
	public abstract void getJobs(HttpServletRequest request, HttpServletResponse response, 
			Hashtable<String, String[]> params, String username, String[] groups) throws MetacatSchedulerException;
}
