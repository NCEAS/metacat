/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements database configuration methods
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
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

package edu.ucsb.nceas.metacat.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SessionData;

/**
 * Control the display of the database configuration page and the processing
 * of the configuration values.
 */
public class ReplicationAdmin extends MetacatAdmin {

	private static ReplicationAdmin instance = null;
	private Logger logMetacat = Logger.getLogger(ReplicationAdmin.class);

	/**
	 * private constructor since this is a singleton
	 */
	private ReplicationAdmin() throws AdminException {

	}

	/**
	 * Get the single instance of D1Admin.
	 * 
	 * @return the single instance of D1Admin
	 */
	public static ReplicationAdmin getInstance() throws AdminException {
		if (instance == null) {
			instance = new ReplicationAdmin();
		}
		return instance;
	}

	/**
	 * Handle configuration of replication -- pass through to the other handler
	 * 
	 * @param request
	 *            the http request information
	 * @param response
	 *            the http response to be sent back to the client
	 * @throws ServiceException 
	 * @throws IOException 
	 * @throws MetacatUtilException 
	 */
	public void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws AdminException, ServiceException, IOException, MetacatUtilException {
		
		PrintWriter out = null;
		Hashtable<String, String[]> params = new Hashtable<String, String[]>();
		Enumeration<String> paramlist = request.getParameterNames();

		while (paramlist.hasMoreElements()) {
			String name = (String) paramlist.nextElement();
			String[] value = request.getParameterValues(name);
			params.put(name, value);
		}

		String action = "";
		if (!params.isEmpty() && params.get("action") != null) {
			action = ((String[]) params.get("action"))[0];
		}
		
		// start, stop, getall and servercontrol need to check if user is administrator
		HttpSession session = request.getSession(true);
		SessionData sessionData = null;
		String sessionId = "";
		String username = "";
		String[] groupnames = { "" };

		if (params.containsKey("sessionid")) {
			sessionId = ((String[]) params.get("sessionid"))[0];
			logMetacat.info("sessionid " + sessionId);
			if (SessionService.getInstance().isSessionRegistered(sessionId)) {
				logMetacat.info("Looking up id " + sessionId + " in registered sessions");
				sessionData = SessionService.getInstance().getRegisteredSession(sessionId);
			}
		}
		if (sessionData == null) {
			sessionData = new SessionData(session.getId(), 
					(String) session.getAttribute("username"), 
					(String[]) session.getAttribute("groups"),
					(String) session.getAttribute("password"), 
					(String) session.getAttribute("name"));
		}

		username = sessionData.getUserName();
		logMetacat.warn("The user name from session is: " + username);
		groupnames = sessionData.getGroupNames();
		if (!AuthUtil.isAdministrator(username, groupnames)) {
			String msg = "The user \"" + username
			+ "\" is not authorized for this action: " + action;
			out = response.getWriter();
			out.print("<error>");
			out.print(msg);
			out.print("</error>");
			out.close();
			logMetacat.warn(msg);
			return;
		}
		
		if (action.equals("stop")) {
			// stop the replication server
			ReplicationService.getInstance().stopReplication();
			out = response.getWriter();
			out.println("Replication Handler Stopped");
		} else if (action.equals("start")) {
			ReplicationService.getInstance().startReplication(params);
			out = response.getWriter();
			out.println("Replication Handler Started");
		} else if (action.equals("getall")) {
			ReplicationService.getInstance().runOnce();
			response.setContentType("text/html");
			out = response.getWriter();
			out.println("<html><body>\"Get All\" Done</body></html>");
		} else if (action.equals("servercontrol")) {
			ReplicationService.handleServerControlRequest(params, request, response);
		} else if (action.equals("resynchSystemMetadata")) {
			HazelcastService.getInstance().resynchInThread();
			response.setContentType("text/html");
			out = response.getWriter();
			out.println("<html><body>SystemMetadata resynch has been started</body></html>");
		} else {
			// Forward the request to the JSP page
			RequestUtil.forwardRequest(request, response, "/admin/replication-configuration.jsp", null);
		}
		
	}
	
	/**
	 * Validate the most important configuration options submitted by the user.
	 * 
	 * @return a vector holding error message for any fields that fail
	 *         validation.
	 */
	protected Vector<String> validateOptions(HttpServletRequest request) {
		Vector<String> errorVector = new Vector<String>();

		// TODO MCD validate options.

		return errorVector;
	}
}
