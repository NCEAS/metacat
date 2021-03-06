<%@ page language="java" %>
<%@ page import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.PropertiesMetaData" %>
<%@ page import="edu.ucsb.nceas.utilities.MetaDataGroup,edu.ucsb.nceas.utilities.MetaDataProperty" %>
<% 
/**
 *
 *    Copyright: 2020 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
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

<title>DataONE Quota Service Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
	<h2>DataONE Quota Service Configuration</h2>
	
	<p>
		Configure the quota service 
	</p>
	<br clear="right"/>
	
	<%@ include file="page-message-section.jsp"%>
	
	<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
	                                        onsubmit="return submitForm(this);">
	
		<h3>Quota Service Configuration</h3>
		<div class="form-row">
            <div class="textinput-label"><label for="dataone.quotas.portals.enabled" title="Enable portal quota service">Enable portal quota service</label></div>
    
            <%
            boolean enable = (Boolean) request.getAttribute("dataone.quotas.portals.enabled");
            if (enable) { 
            %>
            <input type="checkbox" class="textinput" id="dataone.quotas.portals.enabled" 
                    name="dataone.quotas.portals.enabled"                                                                                         
                    value="true"
                    checked="checked"/>
            <% } else {%>
            <input type="checkbox" class="textinput" id="dataone.quotas.portals.enabled" 
                    name="dataone.quotas.portals.enabled"                                                                                         
                    value="true"/>
            <% } %>
        </div>
        <div class="form-row">
            <div class="textinput-label"><label for="dataone.quotas.bookkeeper.serviceUrl" title="Bookkeeper Service URL">Bookkeeper Service URL</label></div>
            <input class="textinput" id="dataone.quotas.bookkeeper.serviceUrl" 
                    name="dataone.quotas.bookkeeper.serviceUrl"                                                                                         
                    value="<%= request.getAttribute("dataone.quotas.bookkeeper.serviceUrl") %>"/> 
        </div>
		
		<div class="buttons-wrapper">
			<input type="hidden" name="configureType" value="quota"/>
			<input type="hidden" name="processForm" value="true"/>
			<input class=button type="submit" value="Update"/>
			<input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
		</div>
	</form>
</div>

<%@ include file="./footer-section.jsp"%>

</body>
</html>
