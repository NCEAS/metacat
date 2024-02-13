<%@ page language="java"%>
<%@ page import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.*, edu.ucsb.nceas.metacat.properties.PropertyService, edu.ucsb.nceas.metacat.admin.AuthAdmin" %>
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
		<%@ include file="./head-section.jsp"%>
		<script language="javascript" type="text/javascript" src="<%= request.getContextPath() %>/style/common/jquery/jquery.js"></script>
	</head>
	<body>
	<%@ include file="./header-section.jsp"%>

	<div class="document">
		<h2>Authentication Configuration</h2>
		<p>LDAP & Password-based Authentication is no longer supported. Please use ORCID authentication.</p>
		<p>If you do not have an ORCID account, please sign up at <a href="https://orcid.org/" target="_blank">https://orcid.org/</a>
		</p>
		
		<br class="auth-header">
		<%@ include file="./page-message-section.jsp"%>

		<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
												onsubmit="return validateAndSubmitForm(this);">
		<% 
			// Metadata holds all group and properties metadata
			PropertiesMetaData metadata = (PropertiesMetaData)request.getAttribute("metadata");
			if (metadata != null) {
				// Each group describes a section of properties
				Map<Integer, MetaDataGroup> groupMap = metadata.getGroups();
				Set<Integer> groupIdSet = groupMap.keySet();
				for (Integer groupId : groupIdSet) {
					if (groupId == 0) {
						continue;
					}
					// For this group, display the header (group name)
					MetaDataGroup metaDataGroup = (MetaDataGroup)groupMap.get(groupId);
		%>
			<div id="<%= metaDataGroup.getName().replace(' ','_') %>">
				<h3><%= metaDataGroup.getName()  %></h3>
				<p><%= metaDataGroup.getDescription()  %></p>
		<%
					// Get all the properties in this group
					Map<Integer, MetaDataProperty> propertyMap = 
						metadata.getPropertiesInGroup(metaDataGroup.getIndex());
					Set<Integer> propertyIndexes = propertyMap.keySet();
					// Iterate through each property and display appropriately
					for (Integer propertyIndex : propertyIndexes) {
						MetaDataProperty metaDataProperty = propertyMap.get(propertyIndex);
						String fieldType = metaDataProperty.getFieldType();
		%>
					<div class="form-row">
		<%
						if (fieldType.equals("select")) {
		%>
							<div class="textinput-label"><label for="<%= metaDataProperty.getKey() %>"><%= metaDataProperty.getLabel() %></label></div>	
								<input class="textinput" id="<%= metaDataProperty.getKey() %>" name="<%= metaDataProperty.getKey() %>" 
									value="<%= request.getAttribute(metaDataProperty.getKey()) %>"                                                                                      
									type="text" readonly/> 				
								<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
							</div>   
		<%
						} else {
		%>
							<div class="textinput-label"><label for="<%= metaDataProperty.getKey() %>"><%= metaDataProperty.getLabel() %></label></div>	
								<input class="textinput" id="<%= metaDataProperty.getKey() %>" name="<%= metaDataProperty.getKey() %>" 
									value="<%= request.getAttribute(metaDataProperty.getKey()) %>"                                                                                      
									type="<%= fieldType %>"/> 				
								<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
							</div>   
		<%
						}
		%> 		    
		<%
						if (metaDataProperty.getDescription() != null) {
		%>
							<div class="textinput-description">[<%= metaDataProperty.getDescription() %>]</div>
		<%		
						}
						
					}
		%>
					</div>
		<%
				}
			}
		%>
		
		<div class="buttons-wrapper">
			<input type="hidden" name="configureType" value="auth"/>
			<input type="hidden" name="processForm" value="true"/>
			<input class="button" type="submit" value="Save"/>
			<input class="button" type="button" value="Cancel" onClick="forward('./admin')"> 
		</div>
		</form>
	</div>
	<script>
		window.onload = function() {
			var field = document.getElementById("auth.administrators");
			field.addEventListener("input", function(event) {
				var value = event.target.value.replace(/\D/g, "").slice(0, 16); // Remove non-digits and limit to 16 characters
				var formattedValue = value.replace(/(\d{4})(?=\d)/g, "$1-"); // Add hyphens
				event.target.value = formattedValue;
			});
		};
	</script>
	<%@ include file="./footer-section.jsp"%>
	</body>
</html>
