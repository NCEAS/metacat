<%@page import="org.dataone.client.auth.CertificateManager"%>
<%@page import="java.security.PrivateKey"%>
<%@page import="java.security.cert.X509Certificate"%>
<%@page import="org.dataone.portal.PortalCertificateManager"%>
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
<%@ include file="../../common/common-settings.jsp"%>
<%@ include file="../../common/configure-check.jsp"%>
<%
String certificateSubject = "(not logged in)";
//check for session-based certificate from the portal
String configurationFileName = application.getInitParameter("oa4mp:client.config.file");
String configurationFilePath = application.getRealPath(configurationFileName);
PortalCertificateManager portalManager = new PortalCertificateManager(configurationFilePath);
X509Certificate certificate = portalManager.getCertificate(request);
PrivateKey key = portalManager.getPrivateKey(request);
if (certificate != null && key != null) {
	certificateSubject = CertificateManager.getInstance().getSubjectDN(certificate);
}
%>
<html>
<head>
<title>Map legacy account to DataONE</title>

<link rel="stylesheet" type="text/css" href="<%=STYLE_COMMON_URL%>/jquery/jqueryui/css/smoothness/jquery-ui-1.8.6.custom.css">
<link rel="stylesheet" type="text/css" href="dataone.css" />	
<script language="javascript" type="text/javascript" src="<%=STYLE_COMMON_URL%>/jquery/jquery.js"></script>
<script language="javascript" type="text/javascript" src="<%=STYLE_COMMON_URL%>/jquery/jqueryui/js/jquery-ui-1.8.6.custom.min.js"></script>

</head>
<body onload="init()">
<!-- dataone logo header -->
<div class="logoheader">
	<h1></h1>
</div>

<p>
You are logged in as: <%=certificateSubject %>
</p>
<p>
<a href="<%=CONTEXT_URL%>/startRequest?target=<%=STYLE_SKINS_URL%>/dataone/account.jsp">Login or switch user...</a>
</p>

</body>
</html>
