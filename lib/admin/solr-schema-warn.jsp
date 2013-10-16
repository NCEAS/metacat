<%@ page language="java" %>
<%@ page import="java.util.Vector,edu.ucsb.nceas.metacat.database.DBVersion,edu.ucsb.nceas.metacat.MetacatVersion" %>

<%
	/**
 *  '$RCSfile$'
 *    Copyright: 2008 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *
 *   '$Author: walker $'
 *     '$Date: 2013-09-19 11:59:52 -0700 (Thu, 19 Sep 2013) $'
 * '$Revision: 8245 $'
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

<title>Existing Customized Solr Schema Warning</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
	<h2>Database Install/Upgrade Utility</h2>
	
	<p><%@ include file="page-message-section.jsp"%></p>
    <p><div class="alert alert-error">
        Note: after configuring Metacat and restarting Tomcat, you have to issue a 'reindexall' action as an administrator to rebuild the Solr index.</div></p>
	<input class="button" type="button" value="Okay" onClick="forward('./admin?configureType=configure&processForm=false')"> 
	
</div>
	<%@ include file="./footer-section.jsp"%>

</body>
</html>
