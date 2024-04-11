<%@ page language="java"%>
<%@ page import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.*" %>

<%
	PropertiesMetaData metadata = (PropertiesMetaData)request.getAttribute("metadata");
 	Vector<String> ldapOrganizations = 
 		(Vector<String>)request.getAttribute("orgList"); 
%>

<html>
<head>

<title>Organization Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<div class="document">

	<h2>Organization Configuration</h2>
	Enter organization specific properties here. 
	<br class="ldap-header">
	
	<%@ include file="./page-message-section.jsp"%>
	
	
	<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
	                                        onsubmit="return submitForm(this);">
	<%
		if (metadata != null) {
			// each group describes a section of properties
			Map<Integer, MetaDataGroup> groupMap = metadata.getGroups();
			Set<Integer> groupIdSet = groupMap.keySet();
			for (Integer groupId : groupIdSet) {
				// for this group, display the header (group name)
				MetaDataGroup metaDataGroup = (MetaDataGroup)groupMap.get(groupId);
	%>
				<h3><%= metaDataGroup.getName()  %></h3>
	<%
	 			if (metaDataGroup.getComment() != null) {
	%>
	  				<div class="heading-comment"><%= metaDataGroup.getComment() %></div>
	<%
	 			}
	%>
				<br>
	<%
				for (String orgName : ldapOrganizations) {
	%>
				<table class="config-section">
					<tr>
					<td class="config-checkbox">
		  				<input class="org" type="checkbox" name="<%= orgName %>.cb" onClick="toggleHiddenTable(this, 'hiding-section-<%= orgName %>')"/>
		  			</td>
		  			<td class="config-checkbox-label">	
						<label for="<%= orgName %>.cb"><%=orgName%></label>
					</td> 
					</tr>
				</table>
				<table class="config-section-hiding" id="hiding-section-<%= orgName %>">  
	<%
					// get all the properties in this group
					Map<Integer, MetaDataProperty> propertyMap = 
						metadata.getPropertiesInGroup(metaDataGroup.getIndex());
					Set<Integer> orgIndexes = propertyMap.keySet();
		  			for (Integer orgIndex : orgIndexes) {
		  				MetaDataProperty orgProperty = propertyMap.get(orgIndex);
		  				String orgKeyName = orgProperty.getKey() + "." + orgName;
	%>		
					<tr>
					<td class="config-property-label" >	
		    			<label for="<%= orgKeyName %>" title="<%= orgProperty.getDescription() %>"><%= orgProperty.getLabel() %></label>
	     			</td>	
	     			<td class="config-property-input" >
						<input name="<%= orgKeyName %>" 
							value="<%= request.getAttribute(orgKeyName) %>"	  
	<% 
						if (orgProperty.getFieldType().equals("password")) { 
	%>           		    	    	           		    	             			
		           			type="password"       
	<%
		  				}
	%>    			
		           			alt="List of administrators for this installation in LDAP DN syntax (colon separated)"/>	           		    
					</td>
					<td class="config-question-mark">
						<i class="icon-question-sign" 
							 alt="<%= orgProperty.getDescription() %>" 
							 onClick="helpWindow('<%= request.getContextPath() %>', '<%= orgProperty.getHelpFile() %>')"></i>
					</td>
					</tr>	  
	<%
					if (orgProperty.getDescription() != null) {
	%>
		    	        <tr>
		    	        <td></td>
		    	        <td class="config-property-description" colspan="2" >
							<%= orgProperty.getDescription() %>
		    	        </td>
	<%
		    			}
		  			}
	%>
	      		</table>
	<%
				}
			}
		}
	%>
	
	  <input type="hidden" name="configureType" value="organization"/>
	  <input type="hidden" name="processForm" value="true"/>
	  <input class="button" type="submit" value="Save"/>
	  <input class="button" type="button" value="Cancel" onClick="forward('./admin')"> 
	
	</form>
</div>
</body>
</html>
