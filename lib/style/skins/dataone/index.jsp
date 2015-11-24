<%@ page language="java"%>
<%@page import="org.dataone.client.D1Client"%>
<%@page import="java.net.URL"%>
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
<%@page import="edu.ucsb.nceas.metacat.service.SessionService"%>
<%

//get the CN environment, dynamically
String cnURL = D1Client.getCN().getNodeBaseServiceUrl();
String portalURL = cnURL.substring(0, cnURL.lastIndexOf(new URL(cnURL).getPath())) + "/portal";
//String portalURL =  "https://cn.dataone.org/portal";

// what was the given DataONE identity?
String d1Identity = request.getParameter("d1Identity");
String token = request.getParameter("token");

// look up the username from the given session
String sessionId = request.getParameter("sessionId");
if (sessionId == null) {
	if (request.getSession() != null) {
		sessionId = request.getSession().getId();
	}
}
if (sessionId == null || !SessionService.getInstance().isSessionRegistered(sessionId)) {
	// redirect to the login page
	response.sendRedirect(STYLE_SKINS_URL + "/dataone/login.jsp");
	return;
}
String userName = SessionService.getInstance().getRegisteredSession(sessionId).getUserName();
%>

<html>
<head>
<title>Map legacy account to DataONE</title>

<link rel="stylesheet" type="text/css" href="<%=STYLE_COMMON_URL%>/jquery/jqueryui/css/smoothness/jquery-ui-1.8.6.custom.css">
<link rel="stylesheet" type="text/css" href="dataone.css" />	
<script language="javascript" type="text/javascript" src="<%=STYLE_COMMON_URL%>/jquery/jquery.js"></script>
<script language="javascript" type="text/javascript" src="<%=STYLE_COMMON_URL%>/jquery/jqueryui/js/jquery-ui-1.8.6.custom.min.js"></script>
<script type="text/javascript">


function makeAjaxCall(url, formId, divId, callback) {
	$('#' + divId).load(
		url, //url
		$("#" + formId).serialize(), //data
		function(response, status, xhr) {
			if (status == "error") {
				var msg = "Sorry but there was an error: ";
				$("#error").html(msg + response);
				$("#error").dialog('open');
			} else {
				// call the callback
				if (callback) {
					setTimeout(callback, 0);
				}
			}
		}
	);
}
function performMapping() {
	$('#mapForm [name="action"]').val('mapIdentity');
	makeAjaxCall(
			'performMapping.jsp', 
			'mapForm', 
			'result', 
			function() { $("#result").dialog('open'); });
	
}
function lookupToken() {
	var data = $('#tokenFrame').contents();
	$('#mapForm [name="token"]').val(data);
	alert('Loaded token: ' + data);
		
	  	
}
function lookupSubject() {
	var subjectFrame = $('#subjectFrame');
	var data = $('#subjectFrame').val();
	$('#mapForm [name="primarySubject"]').val(data);
	alert('Loaded subject: ' + data);
}
function initDialogs() {
	// make the result section a dialog (popup)
	$("#result").dialog(
			{	autoOpen: false,
				title: "Results",
				width: 450
			}
		);
	$("#error").dialog(
			{	autoOpen: false,
				title: "Error",
				width: 450
			}
		);
}
function initTabs() {
	$(function() {
		$("#tabs").tabs();
		$("#tabs").tabs("add", "#mapping", "Legacy account mapping");	
	});
}
function init() {
	initTabs();
	initDialogs();
	//lookupToken();
	//lookupSubject();
}
</script>

</head>
<body onload="init()">
<!-- dataone logo header -->
<div class="logoheader">
	<h1></h1>
</div>

<!-- load AJAX results here -->
<div id="result"></div>
<div id="error"></div>

<div id="tabs">
	<!-- place holder for tabs -->
	<ul></ul>

	<div id="mapping">
		<p>
		Use this form to connect your old <i>ecoinformatics account</i> to your new <i>CILogon identity</i>.
		(<a href="<%=portalURL %>/index.jsp">register</a> with DataONE)
		</p>

		Your current ecoinformatics account (<a href="<%=SERVLET_URL %>?action=logout&qformat=dataone">switch</a> to another account):
		<div>
		<%=userName %>
		</div>

		<br/>

		Will be mapped to this CILogon identity (<a href="<%=portalURL %>/startRequest?target=<%=STYLE_SKINS_URL%>/dataone/index.jsp">login</a>): 
		<iframe style="body {font-weight: bold}" id="subjectFrame" frameborder="0" marginwidth="0" height="30" width="100%" src="<%=portalURL %>/identity?action=getSubject">
		</iframe>
		
		<hr/>		

		Enter this temporary token: 
		<iframe id="tokenFrame" frameborder="0" marginwidth="0" height="30" width="100%" src="<%=portalURL %>/identity?action=getCookie">
		</iframe>
		<br/>

		<form action="" method="POST" id="mapForm">
			In the box below:
			<br/>
			<input type="text" name="token" size="50">
			<input type="hidden" name="secondarySubject" value="<%=userName %>">
			<input type="hidden" name="action" value="mapIdentity">
			<input type="button" value="Map Identity" onclick="performMapping()">
		</form>

		<hr/>

	</div>

</div>
</body>
</html>
