<%@ page language="java" %>
<%@ page import="java.util.Vector,edu.ucsb.nceas.metacat.database.DBVersion,edu.ucsb.nceas.metacat.MetacatVersion" %>

<%
	/**
 *  '$RCSfile$'
 *    Copyright: 2008 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
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
%>

<%
	MetacatVersion metacatVersion = (MetacatVersion)request.getAttribute("metacatVersion"); 	
	DBVersion databaseVersion = (DBVersion)request.getAttribute("databaseVersion");
	Vector<String> updateScriptList = (Vector<String> )request.getAttribute("updateScriptList");
	String supportEmail = (String)request.getAttribute("supportEmail");
%>

<html>
<head>

<title>Database Install/Upgrade Utility</title>
<link rel="stylesheet" type="text/css" 
        href="<%= request.getContextPath() %>/admin/admin.css"></link>
<script language="JavaScript" type="text/JavaScript" src="<%= request.getContextPath() %>/admin/admin.js"></script>

</head>
<body>
<%@ include file="./header-section.jsp"%>

<img src="<%= request.getContextPath() %>/metacat-logo.png" width="100px" align="right"/> 
<h2>Database Install/Upgrade Utility</h2>

<%@ include file="page-message-section.jsp"%>

<%
if (databaseVersion != null && databaseVersion.getVersionString().equals("0.0.0")) {
%>
     The system has detected that this is a new database. <br><br>
     Please hit the Continue button to upgrade your database to version: <%= metacatVersion.getVersionString() %>. <br><br>
     <div class=warning>Warning: this will reinitialize your database.  If this is not a new database, hit the Cancel button and contact support at <%= supportEmail %>. </div><br><br>
     The following scripts will be run:

        <ul>
<%
        for (int i = 0; i < updateScriptList.size(); i++) {
%>
                <li>
                	<%= updateScriptList.elementAt(i) %>
                </li>
<%
        }
%>
        </ul>
        <input class="left-button" type="button" value="Continue" onClick="forward('./admin?configureType=database&processForm=true')">
		<input class="button" type="button" value="Cancel" onClick="forward('./admin?configureType=configure&processForm=false')"> 
<%
} else if (databaseVersion != null) {
%>
     The system has detected the following database version: <%= databaseVersion.getVersionString() %> <br><br>
     Please hit the Continue button to upgrade your database to version: <%= metacatVersion.getVersionString() %> <br>
    <div class=warning>Warning: this will update your database.  If the detected versions do not seem correct, hit the Cancel button and contact support at <%= supportEmail %> </div><br><br> 
     The following scripts will be run:  <br>
        <ul>
<%
        for (int i = 0; i < updateScriptList.size(); i++) {
%>
                <li>
                	<%= updateScriptList.elementAt(i) %>
                </li>
<%
        }
%>
        </ul>
		<input class="left-button" type="button" value="Continue" onClick="forward('./admin?configureType=database&processForm=true')">
		<input class="button" type="button" value="Cancel" onClick="forward('./admin?configureType=configure&processForm=false')"> 
<%
} else {
%>
	<input class="left-button" disabled type="button" value="Continue" onClick="forward('./admin?configureType=database&processForm=true')">
	<input class="button" type="button" value="Cancel" onClick="forward('./admin?configureType=configure&processForm=false')"> 
<%
} 
%>

	<%@ include file="./footer-section.jsp"%>

</body>
</html>
