<%@ page language="java"%>
<%
	/**
	 *  '$RCSfile$'
	 *      Authors: Matt Jones
	 *    Copyright: 2008 Regents of the University of California and the
	 *               National Center for Ecological Analysis and Synthesis
	 *  For Details: http://www.nceas.ucsb.edu/
	 *
	 *   '$Author$'
	 *     '$Date$'
	 * '$Revision$'
	 * 
	 * This is an HTML document for loading an xml document into Oracle
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
%>
<%@page import="edu.ucsb.nceas.metacat.service.SessionService"%>
<%@page import="org.dataone.client.D1Client"%>
<%@page import="java.net.URL"%>
<%@page import="java.io.InputStream"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="org.dataone.service.types.v1.Subject"%>
<%@page import="org.dataone.service.types.v1.Session"%>
<%@page import="edu.ucsb.nceas.metacat.dataone.MNodeService"%>
<%

boolean success = false;

//gather the input
String primarySubject = null; // retrieved from portal service
String secondarySubject = request.getParameter("secondarySubject");
String token = request.getParameter("token");

// check the metacat session
String sessionId = request.getParameter("sessionid");
if (sessionId == null) {
	sessionId = request.getSession().getId();
}
boolean isMetacatSessionValid = SessionService.getInstance().validateSession(sessionId);

// get the CN environment, dynamically
String cnURL = D1Client.getCN().getNodeBaseServiceUrl();
String portalURL = cnURL.substring(0, cnURL.lastIndexOf(new URL(cnURL).getPath())) + "/portal";
//String portalURL =  "https://cn-dev.dataone.org/portal";

//check the DataONE token
String tokenURL = portalURL + "/identity?action=isAuthenticated&token=" + token;
InputStream tokenInputStream = new URL(tokenURL).openStream();
String tokenContent = IOUtils.toString(tokenInputStream, "UTF-8");
tokenContent = tokenContent.trim();
boolean isD1SessionValid = Boolean.parseBoolean(tokenContent);

// look up the D1 subject
String subjectURL = portalURL + "/identity?action=getSubject&token=" + token;
InputStream subjectStream = new URL(subjectURL).openStream();
primarySubject = IOUtils.toString(subjectStream, "UTF-8");
primarySubject = primarySubject.trim();

// call the mapping method as the MN
if (isMetacatSessionValid && isD1SessionValid) {
	Session mnSession = new Session();
	mnSession.setSubject(MNodeService.getInstance(request).getCapabilities().getSubject(0));
	// TODO: set up the certificate for this node
	mnSession = null;
	Subject s1 = new Subject();
	s1.setValue(primarySubject);
	Subject s2 = new Subject();
	s2.setValue(secondarySubject);
	
	success = D1Client.getCN().mapIdentity(mnSession, s1, s2);
}

if (success) {
%>
Mapping set from <%=primarySubject %> to <%=secondarySubject %>
<%	
} else {
%>
Could not set mapping set from <%=primarySubject %> to <%=secondarySubject %>
<%
}
%>