<%@ page language="java" %>
<%@ page import="edu.ucsb.nceas.metacat.admin.SolrAdmin" %>

<%
    String solrHomeValueInProp = (String)request.getAttribute("solrHomeValueInProp");     
    Boolean solrHomeExist  = (Boolean)request.getAttribute("solrHomeExist");
    String solrCoreName = (String)request.getAttribute("solrCore");
    String solrHomeForGivenCore = null;
    if(request.getAttribute("solrHomeForGivenCore") != null ) {
       solrHomeForGivenCore = (String)request.getAttribute("solrHomeForGivenCore");
    }
    String action = (String)request.getAttribute("action");
%>
<html>
<head>

<title>Solr Server Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
	<h2>Solr Service Configuration</h2>
	<p>
        Configure the HTTP Solr service to generate search indexes for objects
    </p>
	<div class="alert alert-warning">
	Please keep your Solr server running while configure it.
	</div>
	<div class="alert alert-warning">
    Please make sure the Tomcat user has the permission to create the instance directory <%= solrHomeValueInProp %> if it is a new installation.
    </div>
	
	
	<!-- MCD TODO add geoserver instructions page -->
	<br clear="right"/>
	
	<%@ include file="page-message-section.jsp"%>
	
	<form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
	                                        onsubmit="return submitForm(this);">
	
		<!-- <h3>HTTP Solr server Configuration</h3> -->
		<%
		  //1. Create - create a new solr core and register it.
		  if(action.equals(SolrAdmin.CREATE)) {
		%>
		<h3>The Solr core - <%= solrCoreName %> with the Solr home directory <%= solrHomeValueInProp %> will be created.<h3>
		<div class="buttons-wrapper">
            <input class=button type="button" value="Create" onClick="forward('./admin?configureType=solrserver&processForm=true&action=create')">
            <input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=solrserver&bypass=true&processForm=true')">
            <input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
        </div>
		<%
		  }
		%>
		
		<%
		  //2. Register - core doesn't exist, but the solr-home directory does exist without schema update indication.
          if(action.equals(SolrAdmin.REGISTER)) {
        %>
        <h3>The Solr core - <%= solrCoreName %> with the Solr home directory <%= solrHomeValueInProp %> will be registered in the Solr server.<h3>
        <div class="buttons-wrapper">
            <input class=button type="button" value="Register" onClick="forward('./admin?configureType=solrserver&processForm=true&action=register')">
            <input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=solrserver&bypass=true&processForm=true')">
            <input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
        </div>
        <%
          }
        %>
        
		<%
          //3. RegisterWithUpdate - core doesn't exist, but the solr-home directory does exist with schema update indication.
          if(action.equals(SolrAdmin.REGISTERANDUPDATE)) {
        %>
        <h3>The Solr core - <%= solrCoreName %> with the Solr home directory <%= solrHomeValueInProp %> will be registered in the Solr server. The index schema will be updated as well<h3>
        <div class="buttons-wrapper">
            <input class=button type="button" value="Register" onClick="forward('./admin?configureType=solrserver&processForm=true&action=registerAndUpdate')">
            <input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=solrserver&bypass=true&processForm=true')">
            <input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
        </div>
        <%
          }
        %>

         <%
          //4. CreateWithWarnning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home doesn't exist.
          //4.1 CreateOrUpdateWithWarning - core does exist, but its the instance directory is different to the solr-home in the properties file and solr home doesn't exist.
          //Ask users if they really want to register the existing core with a new solr-home or keep the original one. If keeping the original one, a schema update will need
          //5. RegisterWithWarnning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home does exist and no schema update. 
          //6. RegisterAndUpdateWithWarnning - core does exist, but its instance directory is different to the solr-home in the properties file and solr home does exist and needing schema update. 
           // Ask users if they really want to register the existing core with a new solr-home or just skip configuration.
          if(action.equals(SolrAdmin.REGISTERANDUPDATEWITHWARN) || action.equals(SolrAdmin.CREATEORUPDATEWITHWARN) || action.equals(SolrAdmin.CREATEWITHWARN) || action.equals(SolrAdmin.REGISTERWITHWARN)) {
        %>
        <div class="block">
        The Solr core - &quot;<%= solrCoreName %>&quot; does exist. However, its current home directory &quot;<%= solrCoreName %>&quot; is different to &quot;<%= solrHomeValueInProp %>&quot;  which you specified on the properties admin page.
        </div>
        <div>
        Please choose either to use its current Solr home directory or a different core name.
        </div>
        <div class="radio-wrapper">
            <input class="checkradio" type="radio" name="<%= SolrAdmin.NEWSOLRCOREORNOT %>" id="<%= SolrAdmin.EXISTINGCORE %>" value="<%= SolrAdmin.EXISTINGCORE %>" onChange="toggleHiddenInputField('<%= SolrAdmin.NEWSOLRCORE %>', '<%= SolrAdmin.NEWSOLCORENAME %>)"/>
            <label class="checkradio-label" > Use the current Solr home directory &quot;<%= solrHomeForGivenCore %>&quot; associated with the core &quot;<%= solrCoreName %>&quot;</label>
            <div style="clear:both"></div>
        </div>
         <div class="radio-wrapper">
            <input class="checkradio" type="radio" name="<%= SolrAdmin.NEWSOLRCOREORNOT %>" id="<%= SolrAdmin.NEWSOLRCORE %>" value="<%= SolrAdmin.NEWSOLRCORE %>" onChange="toggleHiddenInputField('<%= SolrAdmin.NEWSOLRCORE %>', '<%= SolrAdmin.NEWSOLCORENAME %>')"/>
            <label class="checkradio-label" > Use a new core with the directory  &quot;<%= solrHomeValueInProp %>&quot; specified on the property admin page</label>
            <div style="clear:both"></div>
        </div>
         <div class="form-row">
                    <input class="hiddenabletextinput"  id="<%= SolrAdmin.NEWSOLCORENAME %>" name="<%= SolrAdmin.NEWSOLCORENAME %>" placeholder="Name of New Core  "/> 
        </div>
        <div class="buttons-wrapper">
            <input type="hidden" name="configureType" value="solrserver"/>
            <input type="hidden" name="processForm" value="true"/>
            <input type="hidden" name="<%= SolrAdmin.CURRENTCOREINSTANCEDIR %>" value="<%= solrHomeForGivenCore %>"/>
            <input type="hidden" name="<%= SolrAdmin.SOLRCORENAME %>" value="<%= solrCoreName %>"/>
            <input type="hidden" name="action" value="<%= action %>"/>
            <input class=button type="submit" value="Create/Register">
            <input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=solrserver&bypass=true&processForm=true')">
            <input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
        </div>
        <%
          }
        %>
   
        <%
          //7. KEEP - both core and solr-home does exist. And the core's instance directory is as same as the the solr-home. There is no schema update indication
          if(action.equals(SolrAdmin.KEEP)) {
        %>
        <h3>The Solr core - <%= solrCoreName %> with the Solr home directory <%= solrHomeValueInProp %> does exist and the schema does not need update. Please click the OK button.<h3>
        <div class="buttons-wrapper">
            <input class=button type="button" value="OK" onClick="forward('./admin?configureType=solrserver&processForm=true&action=KEEP')">
            <input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=solrserver&bypass=true&processForm=true')">
            <input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
        </div>
        <%
          }
        %>
        
        <%
          //8. Update - both core and solr-home does exist. And the core's instance directory is as same as the the solr-home. There is a schema update indication
          if(action.equals(SolrAdmin.UPDATE)) {
        %>
        <h3>The Solr core - <%= solrCoreName %> with the Solr home directory <%= solrHomeValueInProp %> does exist but the schema needs update. Please click the UPDATE button.<h3>
        <div class="buttons-wrapper">
            <input class=button type="button" value="Update" onClick="forward('./admin?configureType=solrserver&processForm=true&action=update')">
            <input class=button type="button" value="Bypass" onClick="forward('./admin?configureType=solrserver&bypass=true&processForm=true')">
            <input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
        </div>
        <%
          }
        %>
	</form>
</div>

<%@ include file="./footer-section.jsp"%>

</body>
</html>
