<%@ page language="java" %>
<%@ page import="java.util.Set,java.util.HashMap,java.util.Map,java.util.SortedMap,java.util.Vector" %>
<%@ page import="edu.ucsb.nceas.utilities.PropertiesMetaData" %>
<%@ page import="edu.ucsb.nceas.utilities.MetaDataGroup,edu.ucsb.nceas.utilities.MetaDataProperty" %>
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

<%
 	Vector<String> skinNames = (Vector<String>)request.getAttribute("skinNameList"); 
	String defaultStyle = (String)request.getAttribute("defaultStyle");
%>

<html>
<head>

<title>Skins Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document"> 
	<h2>Skins Configuration</h2>
	
	<%@ include file="./page-message-section.jsp"%>
	
	<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
	                                        onsubmit="return validate_form();">
	
	
	<h3>Skins Configuration</h3>
	<div class="heading-comment message-text">
		NOTE: Metacat v2.2.0 includes a new mechanism for customizing the look and feel of your  
		Metacat deployment. MetacatUI themes can be deployed separately from the Metacat server
		allowing easier independent user interface customization. See the Metacat documentation 
		for more information on using this feature.	
	</div>
	<br/>
	<div class="heading-comment">
		Choose and configure the skins that will be available in this instance of Metacat
	</div>
	<br/>
	
	<%
		HashMap<String, HashMap<String, String>> allSkinProperties = 
			(HashMap<String, HashMap<String, String>>)request.getAttribute("skinProperties");
		HashMap<String, PropertiesMetaData> allMetaData = 
			(HashMap<String, PropertiesMetaData>)request.getAttribute("metadataMap");
	
		for (String skinName : skinNames) {
	%>
		<div class="form-row">
			<input class="checkradio" type="checkbox" 
	<%
			if (skinName.equals(defaultStyle)) {
	%>
				checked="yes"
	<%
			} 
	%>
				name="<%= skinName %>.cb" id="<%= skinName %>.cb"onClick="toggleHiddenTable(this, 'hiding-section-<%= skinName %>')"/>
			<label class="checkradio-label" for="<%= skinName %>.cb %>"><%= skinName %></label>
	<%
			if (skinName.equals(defaultStyle)) {
	%>		
				<div class="checkradio-label-inline" id="hiding-default-<%= skinName %>"> (default)</div>	
	<%
			} else {
	%>		
				<div class="checkradio-label-inline" style="display: none;" id="hiding-default-<%= skinName %>"> (default)</div>	
	<%
			} 
	%>
		</div>
		<div class="hiding-section" 
	<%
			if (skinName.equals(defaultStyle)) {
	%>
					style="display: block;"
	<%
			} 
	%>	
		id="hiding-section-<%= skinName %>">
	<%
			HashMap<String, String> skinProperties = allSkinProperties.get(skinName);
			PropertiesMetaData metaData = (PropertiesMetaData)allMetaData.get(skinName);
			Map<Integer, MetaDataGroup> metaDataGroups = metaData.getGroups();
			Set<Integer> groupKeys = metaDataGroups.keySet();
			for (Integer groupkey : groupKeys) {
				if (groupkey == 0) {
					continue;
				}
				SortedMap<Integer, MetaDataProperty> propertyMap = metaData.getPropertiesInGroup(groupkey);
				Set<Integer> propertyKeys = propertyMap.keySet();
	%>
						<input class="checkradio" type="radio" 
	<%
				if (skinName.equals(defaultStyle)) {
	%>
						checked
	<%
				} 
	%>
						name="application.default-style" id="<%= skinName %>-radio" value="<%= skinName %>" onChange="toggleHiddenDefaultText('default-flag', '<%= skinName %>')"/>
						<label class="checkradio-label" for="<%= skinName %>-radio"> Make  &quot;<%= skinName %>&quot; default </label>
	<%
				for (Integer propertyKey : propertyKeys) {
	%>		
	<%		
					MetaDataProperty metaDataProperty = propertyMap.get(propertyKey);							
					String fieldType = metaDataProperty.getFieldType(); 
					if (fieldType.equals("select")) {
	%>
					<div class="form-row"> 
						<div class="textinput-label"><label for="<%= metaDataProperty.getKey() %>" title="<%= metaDataProperty.getDescription() %>"><%= metaDataProperty.getLabel() %></label>  		
						<select class="textinput" name="<%= skinName %>.<%= metaDataProperty.getKey() %>">
	
	<%
						Vector<String> fieldOptionNames = metaDataProperty.getFieldOptionNames();
						Vector<String> fieldOptionValues = metaDataProperty.getFieldOptionValues();
						for (int i = 0; i < fieldOptionNames.size(); i++) {
	%>
							<option value="<%= fieldOptionValues.elementAt(i) %>"> <%= fieldOptionNames.elementAt(i) %>
	<%
						}
	%>
						</select>
						<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')""></i>
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
						<div class="textinput-label"><label for="<%= metaDataProperty.getKey() %>" title="<%= metaDataProperty.getDescription() %>"><%= metaDataProperty.getLabel() %></label></div>
						<input class="textinput" id="<%= skinName %>.<%= metaDataProperty.getKey() %>" name="<%= skinName %>.<%= metaDataProperty.getKey() %>" 	             		    	    	           		    	             			
								type="<%= fieldType %>"/> 
						<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
					</div>
	<%
						if (metaDataProperty.getDescription() != null) {
	%>
							<div class="textinput-description">[<%= metaDataProperty.getDescription() %>]</div>
	<%		
						}   		
					} else if (fieldType.equals("checkbox")) {
	%>
					<div class="form-row">
						<input class="checkradio" id="<%= skinName %>.<%= metaDataProperty.getKey() %>" name="<%= skinName %>.<%= metaDataProperty.getKey() %>" 	             		    	    	           		    	             			
							   type="<%= fieldType %>"
	<%
						if (skinProperties.get(metaDataProperty.getKey()).equals("true")) {
	%>
								checked="yes"
	<%		
						}   
	%>
							   /> 
						<label class="checkradio-label" for="<%= metaDataProperty.getKey() %>" title="<%= metaDataProperty.getDescription() %>"><%= metaDataProperty.getLabel() %></label>
						<i class="icon-question-sign" alt="<%= metaDataProperty.getDescription() %>" 
							 onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
					</div>
	<%
						if (metaDataProperty.getDescription() != null) {
	%>
							<div class="checkradio-description">[<%= metaDataProperty.getDescription() %>]</div>
	<%		
						}
					} else {
	%>
					<div class="form-row">
						<label class="textinput-label" for="<%= metaDataProperty.getKey() %>" title="<%= metaDataProperty.getDescription() %>"><%= metaDataProperty.getLabel() %></label>
						<input class="textinput" id="<%= skinName %>.<%= metaDataProperty.getKey() %>" name="<%= skinName %>.<%= metaDataProperty.getKey() %>" 
								value="<%= skinProperties.get(metaDataProperty.getKey()) %>"	             		    	    	           		    	             			
								type="<%= fieldType %>"/>	
						<i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
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
	
	%>
		</div>
	<%
		}
	%>
	<div class="buttons-wrapper">
		<input type="hidden" name="configureType" value="skins"/>
		<input type="hidden" name="processForm" value="true"/>
		<input class="button" type="submit" value="Save"/>
		<input class="button" type="button" value="Cancel" onClick="forward('./admin')"> 
	</div>
	</form>
</div>

<%@ include file="./footer-section.jsp"%>

</body>
</html>
