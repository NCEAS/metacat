<%@ page language="java" %>

<%
/*
*  '$RCSfile$'
*    Copyright: 2009 Regents of the University of California and the
*               National Center for Ecological Analysis and Synthesis
*
*   '$Author: daigle $'
*     '$Date: 2008-07-06 21:25:34 -0700 (Sun, 06 Jul 2008) $'
* '$Revision: 4080 $'
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
<%@ include file="settings.jsp"%>

<html>
    <head>
        <title>Sanparks TPC Report Search</title>
        <link rel="stylesheet" type="text/css" href="<%=STYLE_SKINS_URL%>/sanparks/sanparks.css"/>
		<link rel="stylesheet" type="text/css" href="<%=STYLE_SKINS_URL%>/sanparks/sanparks-scheduled-jobs.css"/>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_SKINS_URL%>/sanparks/sanparks.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/branding.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/prototype-1.5.1.1/prototype.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/ajax-utils.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_SKINS_URL%>/sanparks/workflowAccess.js"></script>
    </head>
    <body class="main-section" onload="getAccessSection('<%=SERVLET_URL%>','<%=request.getParameter("karfilelsid")%>','<%=request.getParameter("workflowname")%>','access-info-content')">
      
		<div class="content-section">
			<div class="content-subsection" id="access-info-section"> 
				<div class="content-subsection-header">Workflow Summary</div> 
    			<div class="form-input-row" id="form-base-row">
	    			<div class="summary-field" id='workflow-name-label' >Name: </div>  
					<div class="summary-value" id='workflow-name-value' ><%=request.getParameter("workflowname")%></div>	  
					<br/>
					<div class="summary-field" id='workflow-lsid-label' >Kar File LSID: </div>  
					<div class="summary-value" id='workflow-lsid-value' ><%=request.getParameter("karfilelsid")%></div>						
	  			</div>
			</div>

			<div class="access-info-content" id="access-info-content"></div>
</html>
