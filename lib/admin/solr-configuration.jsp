<%@ page language="java" %>
<%@ page import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.PropertiesMetaData" %>
<%@ page import="edu.ucsb.nceas.utilities.MetaDataGroup,edu.ucsb.nceas.utilities.MetaDataProperty" %>
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

<html>
<head>

<title>Solr Server Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
	<h2>EZID for DOI Configuration</h2>
	
	<p>
		Configure the HTTP SOLR service to generate search indexes for objects
	</p>
	<!-- MCD TODO add geoserver instructions page -->
	<br clear="right"/>
	
	<%@ include file="page-message-section.jsp"%>
	
	<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
	                                        onsubmit="return submitForm(this);">
	
		<h3>HTTP SOLR server Configuration</h3>
		<div class="form-row">
                    <div class="textinput-label"><label for="solr.baseURL" title="SOLR Base URL">Base URL</label></div>
                    <input class="textinput" id="solr.baseURL" 
                           name="solr.baseURL"                                                                         
                           value="<%= request.getAttribute("solr.baseURL") %>"/> 
                    <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#solr.baseURL')"></i>
        </div>
        <div class="form-row">
                    <div class="textinput-label"><label for="solr.coreName" title="Core Name">Core Name</label></div>
                    <input class="textinput" id="solr.coreName" 
                           name="solr.coreName"                                                                         
                           value="<%= request.getAttribute("solr.coreName") %>"/> 
                    <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#solr.coreName')"></i>
        </div>
        <div class="form-row">
                    <div class="textinput-label"><label for="solr.homeDir" title="Instance Directory">Instance Directory</label></div>
                    <input class="textinput" id="solr.homeDir" 
                           name="solr.homeDir"                                                                         
                           value="<%= request.getAttribute("solr.homeDir") %>"/> 
                    <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#solr.homeDir')"></i>
        </div>
         <div class="form-row">
                    <div class="textinput-label"><label for="solr.os.user" title="OS User Running SOLR Service">OS User Running SOLR Service</label></div>
                    <input class="textinput" id="solr.os.user" 
                           name="solr.os.user"                                                                         
                           value="<%= request.getAttribute("solr.homeDir") %>"/> 
                    <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#solr.os.user')"></i>
        </div>
		<div class="buttons-wrapper">
			<input type="hidden" name="processForm" value="true"/>
			<input class=button type="submit" value="Update"/>
			<!--<input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=solr&bypass=true&processForm=true')"> -->
			<input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
		</div>
	</form>
</div>

<%@ include file="./footer-section.jsp"%>

</body>
</html>
