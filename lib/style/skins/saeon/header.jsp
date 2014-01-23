<%@ page language="java"%>
<%
	/*
	'$RCSfile$'
	Copyright: 2003 Regents of the University of California and the
	National Center for Ecological Analysis and Synthesis
	'$Author$'
	'$Date$'
	'$Revision$'
	
	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
	 */
%>
<!--____________________________max_width____________________________________-->
<%@ include file="settings.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@page import="edu.ucsb.nceas.metacat.clientview.ClientViewHelper"%>
<html>
<head>
<title>SAEON - South African Environmental Observation Network
Repository</title>
<link rel="stylesheet" type="text/css" href="<%=STYLE_SKINS_URL%>/saeon/saeon.css" />
</head>

<body
	style="background-image: url('<%=STYLE_SKINS_URL%>/saeon/images/bg.png'); background-repeat: repeat-x;"
	class="section-front-page" dir="ltr">

<table align="center" width="100%" cellpadding="0" cellspacing="0">
	<tr>
		<td background="<%=STYLE_SKINS_URL%>/saeon/images/banner_background_left.jpg">&nbsp;</td>
		<td height="255" width="760"
			style="background-image: url('<%=STYLE_SKINS_URL%>/saeon/images/logo.jpg'); background-position: top center; background-repeat: no-repeat;">
		</td>
		<td background="<%=STYLE_SKINS_URL%>/saeon/images/banner_background_right.jpg">&nbsp;</td>
	</tr>
</table>
<div id="visual-portal-wrapper">

<div id="portal-top">

<div id="portal-header">
<a class="hiddenStructure" accesskey="2"
	href="http://www.saeon.ac.za//#documentContent">Skip to content.</a> 
	<a class="hiddenStructure" accesskey="6"
	href="http://www.saeon.ac.za//#portlet-navigation-tree">Skip to navigation</a>

<div id="portal-skinswitcher"></div>
<h5 class="hiddenStructure">Sections</h5>

<ul id="portal-globalnav">
	<li class="plain">
		<a href="http://www.saeon.ac.za/" 
		target="_top"
		style="background-color: White; font-weight: normal" 
		title="SAEON">SAEON</a></li>
	<li class="plain">
		<a href="./" 
		target="_top" 
		style="background-color: White; font-weight: normal" 
		title="Data Repository">Repository</a></li>
	<li class="plain">
		<a href="<%=CGI_URL%>/register-dataset.cgi?cfg=saeon"
		target="_top"
		style="background-color: White; font-weight: normal" 
		title="Register">Register</a></li>
</ul>
</div>

<%
Object obj = request.getSession().getAttribute("clientViewHelper");
ClientViewHelper cvh = null;
String loginHTML = "";
if (obj != null) {
	cvh = (ClientViewHelper) obj;
}
if (cvh != null && cvh.isLoggedIn()) {
	loginHTML = "<a target='_top' href='./index.jsp?action=Logout&qformat=saeon'> Logout </a>";
}
else {
	loginHTML = "<a target='_top' href='./index.jsp'> Login </a>";
}
%>

<h5 class="hiddenStructure">Personal tools</h5>
<ul id="portal-personaltools">
	<li><%=loginHTML %></li>
	<li><a href="<%=USER_MANAGEMENT_URL%>" target="_top"> Join </a></li>
</ul>

</body>
</html>