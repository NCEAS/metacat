<%@ page
        import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.*, edu.ucsb.nceas.metacat.properties.PropertyService, edu.ucsb.nceas.metacat.admin.AuthAdmin" %>
<html>
<head>

    <title>Authentication Configuration</title>
    <%@ include file="./head-section.jsp" %>
    <script language="javascript" type="text/javascript"
            src="<%= request.getContextPath() %>/style/common/jquery/jquery.js"></script>

    <SCRIPT LANGUAGE="JavaScript" TYPE="TEXT/JAVASCRIPT">
        createExclusionList();
    </SCRIPT>
</head>
<body>
<%@ include file="./header-section.jsp" %>
<div class="document">
    <h2>Authentication Configuration</h2>
    <p>Login with ORCID authentication.</p>
    <p>If you do not have an ORCID account, please sign up at <a href="https://orcid.org/"
                                                                 target="_blank">https://orcid.org/</a>
    </p>
    <br class="auth-header">
    <%@ include file="./page-message-section.jsp" %>
    <form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin"
          onsubmit="return validateAndSubmitForm(this);">
        <%
            // metadata holds all group and properties metadata
            PropertiesMetaData metadata = (PropertiesMetaData) request.getAttribute("metadata");
            if (metadata != null) {
                // each group describes a section of properties
                Map<Integer, MetaDataGroup> groupMap = metadata.getGroups();
                Set<Integer> groupIdSet = groupMap.keySet();
                for (Integer groupId : groupIdSet) {
                    if (groupId == 0) {
                        continue;
                    }
                    // for this group, display the header (group name)
                    MetaDataGroup metaDataGroup = (MetaDataGroup) groupMap.get(groupId);
        %>
        <div id="<%= metaDataGroup.getName().replace(' ','_') %>">
            <h3><%= metaDataGroup.getName()  %>
            </h3>
            <p><%= metaDataGroup.getDescription()  %>
            </p>
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

                    }
                    if (fieldType.equals("select")) {
            %>
            <div class="form-row">
                <div class="textinput-label"><label
                        for="<%= metaDataProperty.getKey() %>"><%= metaDataProperty.getLabel() %>
                </label></div>
                <select class="textinput" id="<%= metaDataProperty.getKey().replace('.', '_') %>"
                        name="<%= metaDataProperty.getKey() %>">
                    <%
                        String storedValue =
                                (String) request.getAttribute(metaDataProperty.getKey());
                        Vector<String> fieldOptionValues = metaDataProperty.getFieldOptionValues();
                        Vector<String> fieldOptionNames = metaDataProperty.getFieldOptionNames();
                        for (int i = 0; i < fieldOptionNames.size(); i++) {
                            boolean foundStoredValue = false;
                            if (storedValue != null && !storedValue.equals("")
                                    && storedValue.equals(fieldOptionNames.elementAt(i))) {
                                foundStoredValue = true;
                            }
                            if (foundStoredValue) {
                    %>
                    <option value="<%= fieldOptionValues.elementAt(i) %>"
                            selected="selected"><%= fieldOptionNames.elementAt(i) %>
                    </option>
                    <%
                    } else {
                    %>
                    <option value="<%= fieldOptionValues.elementAt(i) %>"><%= fieldOptionNames.elementAt(
                            i) %>
                    </option>
                    <%
                            }
                        }

                    %>

                </select>
                <i class="icon-question-sign"
                   onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>

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
                <div class="textinput-label"><label
                        for="<%= metaDataProperty.getKey() %>"><%= metaDataProperty.getLabel() %>
                </label></div>
                <input class="textinput" id="<%= metaDataProperty.getKey() %>"
                       name="<%= metaDataProperty.getKey() %>"
                       value="<%= request.getAttribute(metaDataProperty.getKey()) %>"
                       type="<%= fieldType %>"/>
                <i class="icon-question-sign"
                   onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
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
                <div class="textinput-label"><label
                        for="<%= metaDataProperty.getKey() %>"><%= metaDataProperty.getLabel() %>
                </label></div>
                <%
                    if (metaDataProperty.getKey().equals("auth.userManagementUrl")) {
                        String userManagementUrl =
                                (String) request.getAttribute(metaDataProperty.getKey());
                        if (userManagementUrl == null || userManagementUrl.equals("")) {
                            userManagementUrl =
                                    request.getScheme() + "://" + request.getServerName() + ":"
                                            + request.getServerPort() + request.getContextPath()
                                            + PropertyService.getProperty(
                                            "auth.defaultUserManagementPage");
                        }
                %>
                <input class="textinput" id="<%= metaDataProperty.getKey() %>"
                       name="<%= metaDataProperty.getKey() %>"
                       value="<%= userManagementUrl%>"
                       type="<%= fieldType %>"/>
                <%
                } else {
                %>
                <input class="textinput" id="<%= metaDataProperty.getKey() %>"
                       name="<%= metaDataProperty.getKey() %>"
                       value="<%= request.getAttribute(metaDataProperty.getKey()) %>"
                       type="<%= fieldType %>  "/>
                <%
                    }
                %>

                <i class="icon-question-sign"
                   onClick="helpWindow('<%= request.getContextPath() %>','<%= metaDataProperty.getHelpFile() %>')"></i>
            </div>
            <%
                if (metaDataProperty.getDescription() != null) {
            %>
            <div class="textinput-description">[<%= metaDataProperty.getDescription() %>]</div>
            <%
                        }
                    }
                }
            %>
        </div>
        <%
                }
            }
        %>

        <%
            String FILEGROUPNAME = "File-based_Authentication_Configuration";
            String LDAPGROUPNAME = "LDAP_Authentication_Configuration";
            String AUTHCLASSID = "auth_class";
        %>
        <script language="javascript" type="text/javascript">
            //this is for the first loading
            taggleLdapFileConfig();

            //this is for the user to change to different authentication class.
            $('#<%=AUTHCLASSID%>').change(function () {
                taggleLdapFileConfig();
            });

            function taggleLdapFileConfig() {
                if ($("#<%=AUTHCLASSID%> option:selected").text() == '<%=AuthAdmin.LDAPCLASS%>') {
                    $('#<%=FILEGROUPNAME%>').css('display', 'none');
                    $('#<%=LDAPGROUPNAME%>').css('display', 'block');
                } else if ($("#<%=AUTHCLASSID%> option:selected").text() == '<%=AuthAdmin.FILECLASS%>') {
                    $('#<%=LDAPGROUPNAME%>').css('display', 'none');
                    $('#<%=FILEGROUPNAME%>').css('display', 'block');
                }
            }
        </script>

        <div class="buttons-wrapper">
            <input type="hidden" name="configureType" value="auth"/>
            <input type="hidden" name="processForm" value="true"/>
            <input class="button" type="submit" value="Save"/>
            <input class="button" type="button" value="Cancel" onClick="forward('./admin')">
        </div>
    </form>
</div>
<%@ include file="./footer-section.jsp" %>
</body>
</html>
