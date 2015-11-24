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

<title>Geoserver Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
	<h2>Geoserver Configuration</h2>
	
	<p>
		Configure the Geoserver data directory, context and admin password. 
		The default data directory is located within the Metacat deployment.
		The context is assumed to be a sibling of your Metacat web application.
		Geoserver can further be customized by navigating to the Geoserver context and logging in as the admin user.
	</p>
	<!-- MCD TODO add geoserver instructions page -->
	<br clear="right"/>
	
	<%@ include file="page-message-section.jsp"%>
	
	<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
	                                        onsubmit="return submitForm(this);">
	
		<h3>Geoserver Password Configuration</h3>
		
		<div class="form-row">
			<div class="textinput-label"><label for="geoserver.context" title="Geoserver data directory">Geoserver Data Directory</label></div>
			<input class="textinput" id="geoserver.GEOSERVER_DATA_DIR" 
					name="geoserver.GEOSERVER_DATA_DIR" 	             		    	    	           		    	             			
					value="<%= request.getAttribute("geoserver.GEOSERVER_DATA_DIR") %>"/> 
			<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/geoserver.html#GEOSERVER_DATA_DIR')"></i>
		</div>
		<div class="form-row">
			<div class="textinput-label"><label for="spatial.regenerateCacheOnRestart" title="Regenerate spatial cache">Regenerate spatial cache</label></div>
	
			<%
			boolean regenerate = (Boolean) request.getAttribute("spatial.regenerateCacheOnRestart");
			if (regenerate) { 
			%>
			<input type="checkbox" class="textinput" id="spatial.regenerateCacheOnRestart" 
					name="spatial.regenerateCacheOnRestart" 	             		    	    	           		    	             			
					value="true"
					checked="checked"/>
			<% } else {%>
			<input type="checkbox" class="textinput" id="spatial.regenerateCacheOnRestart" 
					name="spatial.regenerateCacheOnRestart" 	             		    	    	           		    	             			
					value="true"/>
			<% } %>
			<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/geoserver.html#GEOSERVER_REGENERATE_CACHE')"></i>
		</div>
		<div class="form-row">
			<div class="textinput-label"><label for="geoserver.context" title="Geoserver context">Context</label></div>
			<input class="textinput" id="geoserver.context" 
					name="geoserver.context" 	             		    	    	           		    	             			
					value="<%= request.getAttribute("geoserver.context") %>"/> 
			<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/geoserver.html#GeoserverContext')"></i>
		</div>
	
		<div class="form-row">
					<div class="textinput-label"><label for="geoserver.username" title="Geoserver user name">User Name</label></div>
					<input class="textinput" id="geoserver.username" 
						   name="geoserver.username" readonly="readonly" 		    	    	           		    	             			
		           		   value="<%= request.getAttribute("geoserver.username") %>"/> 
					<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/geoserver.html#GeoserverUpdatePassword')"></i>
		</div>
		<div class="form-row">
					<div class="textinput-label"><label for="geoserver.password" title="Geoserver user name">Password</label></div>
					<input class="textinput"  id="geoserver.password" 
						   name="geoserver.password" 
						   type="password"	             		    	    	           		    	             			
		           		   value="<%= request.getAttribute("geoserver.password") %>"/> 
					<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/geoserver.html#GeoserverUpdatePassword')"></i>
		</div>
		<div class="buttons-wrapper">
			<input type="hidden" name="configureType" value="geoserver"/>
			<input type="hidden" name="processForm" value="true"/>
			<input class=button type="submit" value="Update"/>
			<input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=geoserver&bypass=true&processForm=true')">
			<input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
		</div>
	</form>
</div>

<%@ include file="./footer-section.jsp"%>

</body>
</html>
