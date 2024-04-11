<%@ page language="java" %>
<%@ page import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.PropertiesMetaData" %>
<%@ page import="edu.ucsb.nceas.utilities.MetaDataGroup,edu.ucsb.nceas.utilities.MetaDataProperty" %>

<html>
<head>

<title>DOI Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
	<h2>DOI Configuration</h2>
	
	<p>
		Configure the DOI service to generate and assign Digital Object Identifiers (DOIs) to objects
	</p>
	<!-- MCD TODO add geoserver instructions page -->
	<br clear="right"/>
	
	<%@ include file="page-message-section.jsp"%>
	
	<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
	                                        onsubmit="return submitForm(this);">
	    <h3>DOI Service Configuration</h3>
		<div class="form-row">
            <div class="textinput-label"><label for="guid.doi.enabled" title="Enable DOI service">Enable DOI service</label></div>
    
            <%
            boolean enable = (Boolean) request.getAttribute("guid.doi.enabled");
            if (enable) { 
            %>
            <input type="checkbox" class="textinput" id="guid.doi.enabled" 
                    name="guid.doi.enabled"                                                                                         
                    value="true"
                    checked="checked"/>
            <% } else {%>
            <input type="checkbox" class="textinput" id="guid.doi.enabled" 
                    name="guid.doi.enabled"                                                                                         
                    value="true"/>
            <% } %>
            <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-enabled')"></i> 
        </div>
        <div class="form-row">
            <div class="textinput-label"><label for="guid.doiservice.plugin.class" title="DOI user name">DOI Service</label></div>
            <select class="textinput" name="guid.doiservice.plugin.class" id="guid.doiservice.plugin.class">
                       <option value="edu.ucsb.nceas.metacat.doi.ezid.EzidDOIService"
                       <%
                            String pluginClassName = (String) request.getAttribute("guid.doiservice.plugin.class");
                            if (pluginClassName != null && pluginClassName.equals("edu.ucsb.nceas.metacat.doi.ezid.EzidDOIService")) {
                       %>
                                selected="yes"
                        <%
                            }
                        %>
                       >EZID</option>
                       <option value="edu.ucsb.nceas.metacat.doi.osti.OstiDOIService"
                       <%
                            if (pluginClassName != null && pluginClassName.equals("edu.ucsb.nceas.metacat.doi.osti.OstiDOIService")) {
                        %>
                                selected="yes"
                        <%
                            }
                        %>
                       >OSTI</option>
             </select>
             <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-username')"></i>
        </div>
		<div class="form-row">
                    <div class="textinput-label"><label for="guid.doi.username" title="DOI user name">User Name</label></div>
                    <input class="textinput" id="guid.doi.username" 
                           name="guid.doi.username"                                                                         
                           value="<%= request.getAttribute("guid.doi.username") %>"/> 
                    <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-username')"></i>
        </div>
        <div class="form-row">
                    <div class="textinput-label"><label for="guid.doi.password" title="Password">Password</label></div>
                    <input class="textinput"  id="guid.doi.password" 
                           name="guid.doi.password" 
                           type="password"                                                                                      
                           value="<%= request.getAttribute("guid.doi.password") %>"/> 
                    <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-password')"></i>
        </div>
        <div class="form-row">
            <div class="textinput-label"><label for="guid.doi.baseurl" title="DOI Service URL">Service Base URL</label></div>
            <input class="textinput" id="guid.doi.baseurl" 
                    name="guid.doi.baseurl"                                                                                         
                    value="<%= request.getAttribute("guid.doi.baseurl") %>"/> 
            <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','docs/metacat-properties.html#guid-ezid-baseurl')"></i>
        </div>
		<div class="form-row">
			<div class="textinput-label"><label for="guid.doi.doishoulder.1" title="DOI shoulder">DOI Shoulder</label></div>
			<input class="textinput" id="guid.doi.doishoulder.1" 
					name="guid.doi.doishoulder.1" 	             		    	    	           		    	             			
					value="<%= request.getAttribute("guid.doi.doishoulder.1") %>"/> 
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
