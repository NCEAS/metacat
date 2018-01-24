<%@ page language="java"%>
<%@ page import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.*" %>
<%
	/**
 *  '$RCSfile$'
 *    Copyright: 2008 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

<title>Authentication Configuration</title>
<link rel="stylesheet" type="text/css" 
        href="<%= request.getContextPath() %>/admin/admin.css"></link>
<script language="JavaScript" type="text/JavaScript" src="<%= request.getContextPath() %>/admin/admin.js"></script>

<SCRIPT LANGUAGE="JavaScript" TYPE="TEXT/JAVASCRIPT">
<!--
	createExclusionList();
//-->
</SCRIPT>


</head>
<body>
<%@ include file="./header-section.jsp"%>

<img src="<%= request.getContextPath() %>/metacat-logo.png" width="100px" align="right"/> 
<h2>Authentication Configuration</h2>
Enter authentication service properties here. 
<br class="auth-header">

<%@ include file="./page-message-section.jsp"%>


<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
                                        onsubmit="return validateAndSubmitForm(this);">
<% 
	// metadata holds all group and properties metadata
    PropertiesMetaData metadata = (PropertiesMetaData)request.getAttribute("metadata");
	if (metadata != null) {
		// each group describes a section of properties
		Map<Integer, MetaDataGroup> groupMap = metadata.getGroups();
		Set<Integer> groupIdSet = groupMap.keySet();
		for (Integer groupId : groupIdSet) {
			if (groupId == 0) {
				continue;
			}
			// for this group, display the header (group name)
			MetaDataGroup metaDataGroup = (MetaDataGroup)groupMap.get(groupId);
%>
		<h3><%= metaDataGroup.getName()  %></h3>
		<%= metaDataGroup.getDescription()  %>
		<hr class="config-line">
<%
			// get all the properties in this group
			Map<Integer, MetaDataProperty> propertyMap = 
				metadata.getPropertiesInGroup(metaDataGroup.getIndex());
			Set<Integer> propertyIndexes = propertyMap.keySet();
			// iterate through each property and display appropriately
			for (Integer propertyIndex : propertyIndexes) {
				MetaDataProperty metaDataProperty = propertyMap.get(propertyIndex);
    			String fieldType = metaDataProperty.getFieldType(); 
    			if (metaDataProperty.getIsRequired()) {
%>
				<SCRIPT LANGUAGE="JavaScript" TYPE="TEXT/JAVASCRIPT">
				<!--
					addExclusion("<%= metaDataProperty.getKey() %>");
				//-->
				</SCRIPT> 		
<% 		
    			}
    			if (fieldType.equals("select")) {
%> 
				<div class="form-row">
					<img class="question-mark" src="style/images/help.png" 
	           		       onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"/>
     				<div class="textinput-label"><label for="<%= metaDataProperty.getKey() %>"><%= metaDataProperty.getLabel() %></label></div>	   	
					<select class="textinput" name="<%= metaDataProperty.getKey() %>">
<%
					Vector<String> fieldOptionValues = metaDataProperty.getFieldOptionValues();
					Vector<String> fieldOptionNames = metaDataProperty.getFieldOptionNames();
					for (int i = 0; i < fieldOptionNames.size(); i++) {
%>
						<option value="<%= fieldOptionValues.elementAt(i) %>"> <%= fieldOptionNames.elementAt(i) %>
<%
					}
%>
					</select>
				</div> 
<%
					if (metaDataProperty.getDescription() != null) {
%>
						<div class="textinput-description">[<%= metaDataProperty.getDescription() %>]</div>
<%		
					}
				} else if (fieldType.equals("password")) {
%>
				<div class="form-row">
					<img class="question-mark" src="style/images/help.png"  
	           		     onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"/>
					<div class="textinput-label"><label for="<%= metaDataProperty.getKey() %>"><%= metaDataProperty.getLabel() %></label></div>	
					<input class="textinput" id="<%= metaDataProperty.getKey() %>" name="<%= metaDataProperty.getKey() %>" 	             		    	    	           		    	             			
	           		    	value="<%= request.getAttribute(metaDataProperty.getKey()) %>"
	           		    	type="<%= fieldType %>"/> 
				</div> 
<%
					if (metaDataProperty.getDescription() != null) {
%>
						<div class="textinput-description">[<%= metaDataProperty.getDescription() %>]</div>
<%		
					}
				} else {
%>
				<div class="form-row">
					<img class="question-mark" src="style/images/help.png"  
					     onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"/>
					<div class="textinput-label"><label for="<%= metaDataProperty.getKey() %>"><%= metaDataProperty.getLabel() %></label></div>					
					<input class="textinput" id="<%= metaDataProperty.getKey() %>" name="<%= metaDataProperty.getKey() %>" 
	    			        value="<%= request.getAttribute(metaDataProperty.getKey()) %>"	             		    	    	           		    	             			
	           		    	type="<%= fieldType %>	"/>	
				</div>    		    
<%
					if (metaDataProperty.getDescription() != null) {
%>
						<div class="textinput-description">[<%= metaDataProperty.getDescription() %>]</div>
<%		
					}
				}
			}
		}
	}
%>

  <input type="hidden" name="configureType" value="auth"/>
  <input type="hidden" name="processForm" value="true"/>
  <input class="left-button" type="submit" value="Save"/>
  <input class="button" type="button" value="Cancel" onClick="forward('./admin')"> 

</form>

<%@ include file="./footer-section.jsp"%>

</body>
</html>
