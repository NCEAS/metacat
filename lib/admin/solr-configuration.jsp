<%@ page language="java" %>
<%@ page import="edu.ucsb.nceas.metacat.admin.SolrAdmin" %>

<% 
/**
 *  '$RCSfile$'
 *    Copyright: 2008 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *
 *   '$Author: daigle $'
 *     '$Date: 2008-07-29 10:31:02 -0700 (Tue, 29 Jul 2008) $'
 * '$Revision: 4176 $'
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
    String solrHomeValueInProp = (String)request.getAttribute("solrHomeValueInProp");     
    Boolean solrHomeExist  = (Boolean)request.getAttribute("solrHomeExist");
    String solrCoreName = (String)request.getAttribute("solrCore");
    String solrHomeForGivenCore = null;
    if(request.getAttribute("solrHomeForGivenCore") != null ) {
       solrHomeForGivenCore = (String)request.getAttribute("solrHomeForGivenCore");
    }
    String action = (String)request.getAttribute("action");
%>
<html>
<head>

<title>Solr Server Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
	<h2>SOLR Service Configuration</h2>
	<p>
        Configure the HTTP SOLR service to generate search indexes for objects
    </p>
	<div class="alert alert-warning">
	Please keep your SOLR server running while configure it.
	</div>
	<div class="alert alert-warning">
    Please make sure the Tomcat user has the permission to create the instance directory <%= solrHomeValueInProp %> if it is a new installation.
    </div>
	
	
	<!-- MCD TODO add geoserver instructions page -->
	<br clear="right"/>
	
	<%@ include file="page-message-section.jsp"%>
	
	<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
	                                        onsubmit="return submitForm(this);">
	
		<!-- <h3>HTTP SOLR server Configuration</h3> -->
		<%
		  if(action.equals(SolrAdmin.CREATE)) {
		%>
		  
		   <h3>The SOLR core - <%= solrCoreName %> with instance directory <%= solrHomeValueInProp %> will be created.<h3>
		   <div class="buttons-wrapper">
            <input class=button type="button" value="Create" onClick="forward('./admin?configureType=solrserver&processForm=true&action=create')">
            <input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=solrserver&bypass=true&processForm=true')">
            <input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
        </div>
		<%
		  }
		%>
		
		
	</form>
</div>

<%@ include file="./footer-section.jsp"%>

</body>
</html>
