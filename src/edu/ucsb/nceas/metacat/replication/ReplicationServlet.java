/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements replication for metacat
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Chad Berkley
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

package edu.ucsb.nceas.metacat.replication;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dataone.client.auth.CertificateManager;

import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.ServiceException;

public class ReplicationServlet extends HttpServlet {

	private static final long serialVersionUID = -2898600143193513155L;

	private static Logger logReplication = Logger.getLogger("ReplicationLogging");
	private static Logger logMetacat = Logger.getLogger(ReplicationServlet.class);

	/**
	 * Initialize the servlet by creating appropriate database connections
	 */
	public void init(ServletConfig config) throws ServletException {

		try {
			// Register preliminary services
			ServiceService.registerService("ReplicationService", ReplicationService
					.getInstance());
			
		} catch (ServiceException se) {
			String errorMessage = "ReplicationServlet.init - Service problem while intializing Replication Servlet: "
					+ se.getMessage();
			logMetacat.error("ReplicationServlet.init - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error(errorMessage);
			throw new ServletException(errorMessage);
		} 
	}

	public void destroy() {
		//ServiceService.
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Process the data and send back the response
		handleGetOrPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Process the data and send back the response
		handleGetOrPost(request, response);
	}

	private void handleGetOrPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
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
		String server = null;

		try {
			// check if the server is included in the list of replicated servers
			server = ((String[]) params.get("server"))[0];

			// verify the client certificate on the request
			boolean isValid = false;
			String msg = "Client certificate is invalid";
			try {
				isValid = hasValidCertificate(request, server);
			} catch (Exception e) {
				msg = "Could not verify client certificate: " + e.getMessage();
				logMetacat.error(msg, e);
				logReplication.error(msg, e);
			}
			if (!isValid) {
				// send message to response
				out = response.getWriter();
				out.print("<error>");
				out.print(msg);
				out.print("</error>");
				out.close();
				return;
			}
			
			// we passed the test, now continue
			if (ReplicationService.getServerCodeForServerName(server) == 0) {
				logReplication.debug("ReplicationServlet.handleGetOrPost - Action \"" + action + "\" rejected for server: " + server);
				return;
			} else {
				logReplication.debug("ReplicationServlet.handleGetOrPost - Action \"" + action + "\" accepted for server: " + server);
			}
			
			// perform the correct action
			if (action.equals("readdata")) {
				OutputStream outStream = response.getOutputStream();
				//to get the data file.
				ReplicationService.handleGetDataFileRequest(outStream, params, response);
				outStream.close();
			} else if (action.equals("forcereplicatedatafile")) {
			    if(MetaCatServlet.isReadOnly(response)) {
                    return;
                }
				//read a specific docid from remote host, and store it into local host
				ReplicationService.handleForceReplicateDataFileRequest(params, request);
			} else if (action.equals("forcereplicate")) {
			    if(MetaCatServlet.isReadOnly(response)) {
                    return;
                }
				// read a specific docid from remote host, and store it into local host
				ReplicationService.handleForceReplicateRequest(params, response, request);
			} else if (action.equals("forcereplicatesystemmetadata")) {
			    if(MetaCatServlet.isReadOnly(response)) {
                    return;
                }
				ReplicationService.handleForceReplicateSystemMetadataRequest(params, response, request);
			} else if (action.equals(ReplicationService.FORCEREPLICATEDELETE)) {
			    if(MetaCatServlet.isReadOnly(response)) {
                    return;
                }
				// read a specific docid from remote host, and store it into local host
				ReplicationService.handleForceReplicateDeleteRequest(params, response, request, false);
			} else if (action.equals(ReplicationService.FORCEREPLICATEDELETEALL)) {
			    if(MetaCatServlet.isReadOnly(response)) {
                    return;
                }
				// read a specific docid from remote host, and store it into local host
				ReplicationService.handleForceReplicateDeleteRequest(params, response, request, true);
			} else if (action.equals("update")) {
			    if(MetaCatServlet.isReadOnly(response)) {
                    return;
                }
				// request an update list from the server
				ReplicationService.handleUpdateRequest(params, response);
			} else if (action.equals("read")) {
				// request a specific document from the server
				// note that this could be replaced by a call to metacatServlet
				// handleGetDocumentAction().
				ReplicationService.handleGetDocumentRequest(params, response);				
			} else if (action.equals("getlock")) {
				ReplicationService.handleGetLockRequest(params, response);
			} else if (action.equals("getdocumentinfo")) {
				ReplicationService.handleGetDocumentInfoRequest(params, response);
			} else if (action.equals("getsystemmetadata")) {
				ReplicationService.handleGetSystemMetadataRequest(params, response);
			} else if (action.equals("gettime")) {
				ReplicationService.handleGetTimeRequest(params, response);
			} else if (action.equals("getcatalog")) {
				ReplicationService.handleGetCatalogRequest(params, response, true);
			} else if (action.equals("test")) {
				response.setContentType("text/html");
				out = response.getWriter();
				out.println("<html><body>Test successfully</body></html>");
			}

		} catch (ServiceException e) {
			logMetacat.error("ReplicationServlet.handleGetOrPost - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationServlet.handleGetOrPost - Error in ReplicationServlet.handleGetOrPost: " + e.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private boolean hasValidCertificate(HttpServletRequest request, String server) throws InvalidNameException, URISyntaxException, ServiceException {
		// get the certificate from the request
		X509Certificate certificate = CertificateManager.getInstance().getCertificate(request);
		if (certificate != null) {
			String givenSubject = CertificateManager.getInstance().getSubjectDN(certificate);
			logMetacat.debug("Given certificate subject: " + givenSubject);

			// get the CN from the DN:
			String givenServerCN = null;
			LdapName ldapName = new LdapName(givenSubject);
			for (Rdn rdn: ldapName.getRdns()) {
				if (rdn.getType().equalsIgnoreCase("CN")) {
					givenServerCN = (String) rdn.getValue();
					logMetacat.debug("Given server CN: " + givenServerCN);
					break;
				}
			}
			
			// check the replication table for this server
			int serverCode = ReplicationService.getServerCodeForServerName(server);
			if (serverCode != 0) {
				// does it match (roughly) the certificate
				URI serverURI = new URI("https://" + server);
				String serverHost = serverURI.getHost();
				logMetacat.debug("Checking against registerd replication server host name: " + serverHost);
				// remove wildcard from certificate CN if it is a wildcard certificate
				givenServerCN = givenServerCN.replace("*", "");
				// match (ends with) same certificate name (domain)?
				return serverHost.endsWith(givenServerCN);
			}
		}
 		return false;
	}
}
