<%@ page language="java" %>
<%@ page import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.PropertiesMetaData" %>
<%@ page import="edu.ucsb.nceas.utilities.MetaDataGroup,edu.ucsb.nceas.utilities.MetaDataProperty" %>

<html>
<head>

<title>Metacat Properties Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
    <h2>Metacat Properties Configuration</h2>

    <p>Enter Metacat System properties here.  All Fields must be filled in before saving.
    </p>
    <br clear="right"/>

    <%@ include file="page-message-section.jsp"%>

    <form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin"
                                            onsubmit="return submitForm(this);">
    <%
        // metadata holds all group and properties metadata
        PropertiesMetaData metadata = (PropertiesMetaData)request.getAttribute("metadata");
        if (metadata != null) {
            // each group describes a section of properties
            Map<Integer, MetaDataGroup> groupMap = metadata.getGroups();
            Set<Integer> groupIdSet = groupMap.keySet();

            for (Integer groupId : groupIdSet) {
                // for this group, display the header (group name)
                MetaDataGroup metaDataGroup = (MetaDataGroup)groupMap.get(groupId);
                if (groupId == 0) {
                    // get all the properties in this group
                    Map<Integer, MetaDataProperty> propertyMap =
                        metadata.getPropertiesInGroup(metaDataGroup.getIndex());
                    Set<Integer> propertyIndexes = propertyMap.keySet();
                    // iterate through each property and display appropriately
                    for (Integer propertyIndex : propertyIndexes) {
                        MetaDataProperty metaDataProperty = propertyMap.get(propertyIndex);
                        String fieldType = metaDataProperty.getFieldType();
                        if (fieldType.equals("hidden")) {
    %>
                            <input type="hidden"
                                   name="<%= metaDataProperty.getKey() %>"
                                    value="<%= request.getAttribute(metaDataProperty.getKey()) %>"/>
    <%
                        }
                    }
                } else {
    %>
            <h3><%= metaDataGroup.getName()  %></h3>
    <%
                // get all the properties in this group
                Map<Integer, MetaDataProperty> propertyMap =
                    metadata.getPropertiesInGroup(metaDataGroup.getIndex());
                Set<Integer> propertyIndexes = propertyMap.keySet();
                // iterate through each property and display appropriately
                    for (Integer propertyIndex : propertyIndexes) {
                        MetaDataProperty metaDataProperty = propertyMap.get(propertyIndex);
                        String fieldType = metaDataProperty.getFieldType();
                        if (fieldType.equals("select")) {
    %>
                            <div class="form-row">
                            <div class="textinput-label"><label for="<%= metaDataProperty.getKey() %>"><%= metaDataProperty.getLabel() %></label></div>
                            <select class="textinput" name="<%= metaDataProperty.getKey() %>">
    <%
                            Vector<String> fieldOptionValues = metaDataProperty.getFieldOptionValues();
                            Vector<String> fieldOptionNames = metaDataProperty.getFieldOptionNames();
                            for (int i = 0; i < fieldOptionNames.size(); i++) {
    %>
                                <option value="<%= fieldOptionValues.elementAt(i) %>"
    <%
                                if (fieldOptionValues.elementAt(i).equals(request.getAttribute(metaDataProperty.getKey()))) {
    %>
                                    selected="yes"
    <%
                                }
    %>
                        > <%= fieldOptionNames.elementAt(i) %>
    <%
                            }
    %>
                            </select>
                        <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
                    </div>
    <%
                        } else if (fieldType.equals("password")) {
    %>
                    <div class="form-row">
                        <div class="textinput-label"><label for="<%= metaDataProperty.getKey() %>" ><%= metaDataProperty.getLabel() %></label></div>
                        <input class="textinput" id="<%= metaDataProperty.getKey() %>"
                                name="<%= metaDataProperty.getKey() %>"
                                value="<%= request.getAttribute(metaDataProperty.getKey()) %>"
                                type="<%= fieldType %>"/>
                        <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
                    </div>
    <%
                        } else if (fieldType.equals("hidden")) {
    %>
                            <input id="<%= metaDataProperty.getKey() %>"
                                    name="<%= metaDataProperty.getKey() %>"
                                    value="<%= request.getAttribute(metaDataProperty.getKey()) %>"
                                    type="<%= fieldType %>"/>
    <%
                        } else {
    %>
                    <div class="form-row">
                        <div class="textinput-label"><label for="<%= metaDataProperty.getKey() %> "><%= metaDataProperty.getLabel() %></label>    </div>
                        <input class="textinput" id="<%= metaDataProperty.getKey() %>"
                                name="<%= metaDataProperty.getKey() %>"
                                value="<%= request.getAttribute(metaDataProperty.getKey()) %>"
                                type="<%= fieldType %> "/>
                        <i class="icon-question-sign" onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
                    </div>
    <%
                        }

                        if (metaDataProperty.getDescription() != null && metaDataProperty.getDescription().trim().length() > 0 ) {
                            if (!metaDataProperty.getDescription().contains("new-badge")) {
    %>
                                <div class="textinput-description">[<%= metaDataProperty.getDescription() %>]</div>
    <%
                            } else {
    %>
                                <div class="textinput-description"><%= metaDataProperty.getDescription() %></div>
    <%
                            }
                        }
                    }
                }
            }
        }
    %>

                <input type="hidden" name="configureType" value="properties"/>
                <input type="hidden" name="processForm" value="true"/>
                <input class=button type="submit" value="Save"/>
                <input class=button type="button" value="Cancel" onClick="forward('./admin')">

    </form>

</div>
<%@ include file="./footer-section.jsp"%>

</body>
</html>
