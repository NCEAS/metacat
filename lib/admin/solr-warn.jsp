<%@ page language="java" %>
<%@ page import="java.util.Vector,edu.ucsb.nceas.metacat.database.DBVersion,edu.ucsb.nceas.metacat.MetacatVersion" %>

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
	<h2>Solr Service Configuration</h2>
	
	<p><%@ include file="page-message-section.jsp"%></p>
	<input class="button" type="button" value="Okay" onClick="forward('./admin?configureType=configure&processForm=false')"> 
	
</div>
	<%@ include file="./footer-section.jsp"%>

</body>
</html>
