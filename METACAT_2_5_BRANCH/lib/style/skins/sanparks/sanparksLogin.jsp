<?xml version="1.0" encoding="UTF-8"?>

<!-- to change the content type or response encoding change the following line -->
<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@ page import="edu.ucsb.nceas.metacat.clientview.ClientView" %>
<%@ page import="edu.ucsb.nceas.metacat.clientview.ClientViewHelper" %>
<%@ page import="edu.ucsb.nceas.metacat.clientview.ClientHtmlHelper" %>
<%@ page import="edu.ucsb.nceas.metacat.service.SessionService" %>

<%@ include file="settings.jsp" %>

<% 	
    ClientViewHelper clientViewHelper = null;
%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title>SANParks - South African National Park Data Repository</title>
	<link rel="stylesheet" type="text/css" href="<%= CONTEXT_URL %>/style/skins/sanparks/sanparks.css"/>
	<script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/prototype-1.5.1.1/prototype.js"></script>
	<script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/ajax-utils.js"></script>
	<script language="JavaScript" type="text/JavaScript" src="<%=STYLE_SKINS_URL%>/sanparks/sanparksLogin.js"></script>
</head>
	
<body>
	<div class="templatecontentareaclass" style="background: #FFFFFF;">
	<jsp:useBean id="clientViewBean" scope="session" class="edu.ucsb.nceas.metacat.clientview.ClientView" /> 
	<jsp:setProperty name="clientViewBean" property="*" /> 
	<% clientViewHelper = ClientViewHelper.clientViewHelperInstance(request); %>
		
	<table>
	<tr>
		<td colspan="3">
			<p class="regtext">
			Welcome to the SANParks Data Repository. 
			This is the primary source for comprehensive information about scientific 
			and research data sets collected throughout the South African National Park System.
			</p>
		</td>
	</tr>
	<tr valign="top">
	<td>
	
	<h2>Search for SANParks Data</h2>
	
	<p class="emphasis">Searching: 
		<%
			String organizationScope = request.getParameter("organizationScope");
			if (organizationScope == null) {
				organizationScope = "";
			}
			if (!organizationScope.equals("")) {
		%>
			<!-- set the map to use the correct scope -->
			<script type="text/javascript" >
				var dropDownTimer = null;
				
				//this syncs the map based on the input string location
				function setMapLocation(strLocation) {
				
					var mapFrameDocument = document.getElementById("mapFrame").contentDocument;
					if (!mapFrameDocument) {
						//alert("IE");
						mapFrameDocument = document.getElementById("mapFrame").contentWindow;
						if (mapFrameDocument.document) {
							mapFrameDocument = mapFrameDocument.document;
						}
						
					}
					//alert("mapFrame=" + mapFrameDocument.name);
					//alert("locations=" + mapFrameDocument.getElementsByTagName('locations'));
					
					//check if the dropdown is loaded in DOM
					if (mapFrameDocument.getElementsByName('locations').length == 0) {
						dropDownTimer = setTimeout("setMapLocation('" + strLocation + "')", 100);
						return false;
					}
					clearTimeout(dropDownTimer);
					
					var locationMenu = mapFrameDocument.getElementsByName('locations')[0];
					//alert("locationMenu=" + locationMenu);
					var locationOptions = locationMenu.options;
					//alert("locationOptions=" + locationOptions);
					//loop through the options to find the correct location based on input string
					for (var i=0; i < locationOptions.length; i++) {
						if (locationOptions[i].text == strLocation) {
							//set as selected
							locationMenu.selectedIndex = i;
							break;
						}
					}
					//alert("Focusing on selected location: " + locationMenu.options[locationMenu.selectedIndex].text);
					
					//the onchange command from select object
					locationMenu.onchange();
					//mapFrameDocument.config.objects.locationsSelect.setAoi(locationMenu.options[locationMenu.selectedIndex].value,'mainMap');
				
				}
				
				//kick it off
				dropDownTimer = 
					setTimeout(
					"setMapLocation('<%= organizationScope %>')",
					 100);
				
			</script>
			
			<%= organizationScope %>
		<%
			} else {
		%>
			All Organizations	
		<%
			}
		%>
	</p>
	
	<form id="searchform" name="searchform" method="post" 
			action="<%= SERVLET_URL %>" target="_top"
			onsubmit="setQueryFormField()">
		<p class="regtext">
		The repository search system is used to locate data sets of interest by 
		searching through existing registered data sets. 
		Presently the search covers all fields, including author, title, abstract, 
		keywords, and other documentation for each data set. 
		<br />
		Use a '%' symbol as a wildcard in searches (e.g., '%herbivore%' 
		would locate any phrase with the word herbivore embedded within it).
		</p>
		<input name="organizationScope" id="organizationScope" type="hidden" value="<%= organizationScope %>" />
		<input name="sessionid" id="sessionid" type="hidden" value="<%= request.getSession().getId() %>" />
		<input name="anyfield" type="text" id="anyfield" value="" size="14" />
		<input name="query" type="hidden" id="query" />
		<input name="qformat" type="hidden" value="sanparks"/>
		<input name="action" type="hidden" value="squery" />  
		<input type="submit" value="Search"  />
		<br/>
		<input type="checkbox" id="searchAll" name="searchAll" />Search all fields (slower)
		<p class="regtext">
		-Or-
		<br />
		Browse all existing data sets by title. This operation can be slow.
		</p>
		<input type="button" value="Browse All" onclick="setBrowseAll();form.submit()" />
	</form>
	
	</td>
	<td>
	
	<div id="loginSection">
<%
	if (!clientViewHelper.isLoggedIn()) {
%>
		<h3>Login
		<a href="<%=USER_MANAGEMENT_URL%>" target="_new" >
		<span class="regtext"> (request an account...)</span>
		</a>
		</h3>
		<form name="loginForm" id="loginForm" method="post" onsubmit="submitLoginFormIntoDivAndReload('<%= SERVLET_URL %>', this, 'loginSection'); return false;">
		  <input name="qformat" type="hidden" value="sanparks" />
		  <input name="action" type="hidden" value="login"/>
		  <table>
		    <tr valign="top">
		      <td><span class="required">User name</span></td>
		      <td><input name="shortusername" type="text" value="" style="width: 140" /></td>
		      <td><input name="username" type="hidden" value="" /></td>
		    </tr>
		    <tr>
		      <td><span class="required">Organization</span></td>
		      <td><select name="organization" style="width: 140">
		            <option value="SANParks" selected="">SANParks</option>
		            <option value="SAEON">SAEON</option>
		            <option value="NCEAS">NCEAS</option>
		            <option value="unaffiliated">unaffiliated</option>
		          </select></td>
		    </tr>
		    <tr>
		      <td><span class="required">Password</span></td>
		      <td><input name="password" value="" type="password" style="width: 140" maxlength="50" /></td>
		    </tr>
		    <tr>
		      <td colspan="2" align="center">
		        <input name="loginSubmit" value="login" type="submit" class="button_login" />
		      </td>
		    </tr>
		  </table>
		</form>
<%
	} else {
%>
		<h3>Welcome,<%= clientViewBean.getUsername() %></h3>
		<form name="logoutForm" id="logoutForm" method="post" onsubmit="submitLogoutFormIntoDiv('<%= SERVLET_URL %>', this, 'loginSection'); return false;">
		  <input name="qformat" value="sanparks" type="hidden" />
		  <input name="action" type="hidden" value="logout"/>
		  <table>
			   <tr valign="top">
			     <td><p class="regtext">You are currently logged in.</p></td>
			     <td align="right"><input name="action" type="submit" value="logout" class="button_login" /></td>
			   </tr>
		    <tr valign="top">
		      <td colspan="2" width="600px"><p class="regtext"></p></td>
		      <!--  td colspan="2">< p class="regtext">(< % = clientViewBean.getMessage(ClientView.LOGIN_MESSAGE) % >)< / p ></td -->
			</tr>
			<tr>	
			   <td colspan="2" class="regtext" align="center" valign="top">		
		      <!-- reset pass -->
		        <a href="<%=USER_MANAGEMENT_URL%>" target="_parent">
		          reset your password
		        </a>
		        |
		        <!-- change pass -->
		        <a href="<%=USER_MANAGEMENT_URL%>" target="_parent">
		          change your password
		        </a>
		      </td>
		    </tr>
		  </table>
		</form>
<%		
	}
%>
	
	<!-- File Upload Form --> 
	<br />
	<h3>Data Package Upload</h3>
	
	<%
		if (clientViewHelper.isLoggedIn()) {
	%>
	<table width="100%">
		<tr valign="top">
			<td align="right">
				<form method="post" action="<%= CONTEXT_URL %>/style/skins/sanparks/upload.jsp">
					<input type="submit" value="Go >" class="button_login" />
				</form>
			</td>
		</tr>
	</table>			
	<%
		} else {
	%>
	
	<p class="regtext">
		You must be logged into your user account before uploading a data set.
	</p>
	<%
		}
	%>
	
	</div>
	</td>
	
	<!-- so the map frame doesn't overlap content -->
	<td width="50px"></td>
	
	</tr>
	
	<tr>
	<td colspan="2" align="center">
	
	
	<!-- Map here --> 
	<br />
	<h3>Spatial Search</h3>
	
		<!-- map frame -->
        <script language="JavaScript">
            insertMap("<%= CONTEXT_URL %>");
        </script>
	</td>
	
	<!-- so the map frame doesn't overlap content -->
	<td width="50px"></td>
	
	</tr>
	
	</table>
	
	</div>
</body>
</html>
