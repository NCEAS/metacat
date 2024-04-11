<%@ page language="java"%>
<%@ page
	import="edu.ucsb.nceas.metacat.util.OrganizationUtil,edu.ucsb.nceas.metacat.properties.PropertyService"%>

<html>
<head>

<title>Backup Directory Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>		
<div class="document">
	<h2>Backup Directory Configuration</h2>
	
	<p>
		Metacat will back up configuration values in a location outside of the application installation 
		directories.  In this way, you won't have to re-enter the entire configuration every time you reinstall 
		Metacat. 
	</p>
	
	<%
	  String backupDir = (String)request.getAttribute("backupBaseDir");
	  String backupDirStatus = (String)request.getAttribute("backupDirStatus");
	
	  if (backupDirStatus.equals("hiddenExistsPopulated")) {
	%>
	  <p>
	  	The following directory was discovered with existing backup files.  If this is not the 
	  	correct backup directory, please correct below.
	  </p>
	  
	  <h5><%= backupDir %></h5>
	  
	<%
	  } else if (backupDirStatus.equals("unknown")) {
	%>
	  <p>
	  The system could not discover an optimal backup location.  Please enter a location that you have
	  permissions to below.
	  </p>
	
	<%
	  } else {
	%>  
	  <p>
	  	The following directory was determined to be optimal for creating backup directories.  If 
	  	this is not correct, please correct below.
	  </p>
	   <h5><%= backupDir %></h5>
	<%
	  }
	%>
	
	<%@ include file="./page-message-section.jsp"%>
	
	<form name="backupform" method="post"
		action="<%= request.getContextPath() %>/admin" target="_top"
		onsubmit="return validateAndSubmitForm(this);" id="backupform">
	
	<table class="backup">
		<tr>
			<td>Backup File Directory:</td>
		</tr>
		<tr>
			<td><input class="backup-input" name="backup-dir" type="text"
				maxlength="256"
				value="<%= backupDir %>"></td>
		</tr>
		<tr>
			<td class="textinput-description">[Backup directory]</td>
		</tr>
	</table>
	
	<div class="buttons-wrapper">
		<input type="hidden" name="configureType" value="backup"/>
		<input type="hidden" name="processForm" value="true"/>
		<input class=button type="submit" value="Save"/>
	</div>
	</form>
</div>
</body>
</html>
