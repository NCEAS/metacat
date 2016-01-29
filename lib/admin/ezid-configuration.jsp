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

<title>EZID for DOI Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
	<h2>EZID for DOI Configuration</h2>
	
	<p>
		Configure the EZID service to generate and assign Digital Object Identifiers (DOIs) to objects
	</p>
	<!-- MCD TODO add geoserver instructions page -->
	<br clear="right"/>
	
	<%@ include file="page-message-section.jsp"%>
	
	<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
	                                        onsubmit="return submitForm(this);">
	
		<h3>EZID Service Configuration</h3>
		<div class="form-row">
            <div class="textinput-label"><label for="guid.ezid.enabled" title="Enable EZID service">Enable EZID service</label></div>
    
            <%
            boolean enable = (Boolean) request.getAttribute("guid.ezid.enabled");
            if (enable) { 
            %>
            <input type="checkbox" class="textinput" id="guid.ezid.enabled" 
                    name="guid.ezid.enabled"                                                                                         
                    value="true"
                    checked="checked"/>
            <% } else {%>
            <input type="checkbox" class="textinput" id="guid.ezid.enabled" 
                    name="guid.ezid.enabled"                                                                                         
                    value="true"/>
            <% } %>
            <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-enabled')"></i> 
        </div>
		<div class="form-row">
                    <div class="textinput-label"><label for="guid.ezid.username" title="EZID user name">User Name</label></div>
                    <input class="textinput" id="guid.ezid.username" 
                           name="guid.ezid.username"                                                                         
                           value="<%= request.getAttribute("guid.ezid.username") %>"/> 
                    <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-username')"></i>
        </div>
        <div class="form-row">
                    <div class="textinput-label"><label for="guid.ezid.password" title="Password">Password</label></div>
                    <input class="textinput"  id="guid.ezid.password" 
                           name="guid.ezid.password" 
                           type="password"                                                                                      
                           value="<%= request.getAttribute("guid.ezid.password") %>"/> 
                    <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-password')"></i>
        </div>
        <div class="form-row">
            <div class="textinput-label"><label for="guid.ezid.baseurl" title="EZID Service URL">Service Base URL</label></div>
            <input class="textinput" id="guid.ezid.baseurl" 
                    name="guid.ezid.baseurl"                                                                                         
                    value="<%= request.getAttribute("guid.ezid.baseurl") %>"/> 
            <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-baseurl')"></i>
        </div>
		<div class="form-row">
			<div class="textinput-label"><label for="guid.ezid.doishoulder.1" title="DOI shoulder">DOI Shoulder</label></div>
			<input class="textinput" id="guid.ezid.doishoulder.1" 
					name="guid.ezid.doishoulder.1" 	             		    	    	           		    	             			
					value="<%= request.getAttribute("guid.ezid.doishoulder.1") %>"/> 
			<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-doishoulder-1')"></i>
		</div>
	
		
		<div class="buttons-wrapper">
			<input type="hidden" name="configureType" value="ezid"/>
			<input type="hidden" name="processForm" value="true"/>
			<input class=button type="submit" value="Update"/>
			<!--<input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=ezid&bypass=true&processForm=true')"> -->
			<input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
		</div>
	</form>
</div>

<%@ include file="./footer-section.jsp"%>

</body>
</html>
