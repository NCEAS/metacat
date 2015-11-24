<%@ page language="java"  %>
<%@ page import="edu.ucsb.nceas.metacat.database.DBVersion,edu.ucsb.nceas.metacat.MetacatVersion" %>
<%@ page import="edu.ucsb.nceas.metacat.properties.PropertyService" %>

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
	MetacatVersion metacatVersion = (MetacatVersion)request.getAttribute("metaCatVersion"); 	
	DBVersion databaseVersion = (DBVersion)request.getAttribute("databaseVersion");		
    Boolean propsConfigured = (Boolean)request.getAttribute("propsConfigured");	
    Boolean orgsConfigured = (Boolean)request.getAttribute("orgsConfigured");	
    Boolean authConfigured = (Boolean)request.getAttribute("authConfigured");	
    Boolean skinsConfigured = (Boolean)request.getAttribute("skinsConfigured");	
    Boolean dbConfigured = (Boolean)request.getAttribute("dbConfigured");
    Boolean replicationConfigured = (Boolean)request.getAttribute("replicationConfigured");
    String dataoneConfigured = (String)request.getAttribute("dataoneConfigured");
    String geoserverConfigured = (String)request.getAttribute("geoserverConfigured");
    Boolean metacatConfigured = (Boolean)request.getAttribute("metacatConfigured");
    Boolean metacatServletInitialized = (Boolean)request.getAttribute("metcatServletInitialized");
    String contextURL = (String)request.getAttribute("contextURL");
%>

<html>
<head>
<title>Metacat Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>

<body>
<%@ include file="./header-section.jsp"%>
<div class="document">
	<h2>Metacat Configuration</h2>
	
	<p>All of the following sections must be in a configured state for Metacat to run properly:</p>
	<br class="main-header">
	
	<%@ include file="page-message-section.jsp"%>
	
	<table class="configuration-table">
	
	<%
		if (propsConfigured != null && propsConfigured) {
	%>
	        <tr>
	        <td class="configured-tag"><i class="icon-ok"></i> configured </td>
			<td class="property-title"> Metacat Global Properties </td> 
			<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=properties"> <i class="icon-cogs"></i>Reconfigure Now</a> </td>
	        </tr>
	<%
		} else {
	%>    		
	        <tr>
	 		<td class="unconfigured-tag">unconfigured </td>  
	 		<td class="property-title"> Metacat Global Properties </td>
			<td class="configure-link"> <a href="<%= request.getContextPath() %>/admin?configureType=properties"><i class="icon-cogs"></i> Configure Now</a> </td>			
	    	</tr>
	<%
		}
	
		if (authConfigured != null && authConfigured) {
	%>
	        <tr>
	        <td class="configured-tag"><i class="icon-ok"></i>configured </td>
			<td class="property-title"> Authentication Configuration </td> 
			<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=auth"><i class="icon-cogs"></i>Reconfigure Now</a> </td>
	        </tr>
	<%
		} else {
	%>    		
	        <tr>
	 		<td class=unconfigured-tag>unconfigured</td>  
	 		<td class=property-title> Authentication Configuration </td>
			<td class=configure-link><a href="<%= request.getContextPath() %>/admin?configureType=auth"><i class="icon-cogs"></i>Configure Now</a> </td>			
	    	</tr>
	<%
		}
		
	    if (skinsConfigured != null && skinsConfigured) {
	%>
	        <tr>
	        <td class="configured-tag"><i class="icon-ok"></i>configured</td>
			<td class="property-title"> Skins Specific Properties </td> 
			<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=skins"><i class="icon-cogs"></i>Reconfigure Now</a> </td>
	        </tr>
	<%
		} else {
	%>    		
	        <tr>
	 		<td class="unconfigured-tag">unconfigured </td>  
	 		<td class="property-title"> Skins Specific Properties </td>
			<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=skins"><i class="icon-cogs"></i>Configure Now</a> </td>			
	    	</tr>
	<%
		}  
	    
	    if ((dbConfigured != null && dbConfigured) ||
	    		(metacatVersion != null && databaseVersion != null && 
	    				metacatVersion.compareTo(databaseVersion) == 0)) {
	%>
	    	<tr>
	    	<td class="configured-tag"><i class="icon-ok"></i> configured </td>
	    	<td class="property-title"> Database Installation/Upgrade </td> 
			<td class="configure-link inactive"> Version: <%=databaseVersion.getVersionString()%> </td>    		
	    	</tr>
	<%
		} else {
	%>    		
	    	<tr>
	    	<td class="unconfigured-tag">unconfigured </td>  
	    	<td class="property-title"> Database Installation/Upgrade </td>
	<%
		if (propsConfigured != null && propsConfigured) {
	%>
	        	
			<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=database"><i class="icon-cogs"></i> Configure Now</a> </td>			
	        	
	<%
				        		} else {
				        	%> 
			<td class="configure-link inactive"> Configure Global Properties First </td>
	<%
		}
	%>     	
	    	</tr>
	<%
		}
	
	    if (geoserverConfigured != null && geoserverConfigured.equals(PropertyService.CONFIGURED)) {
	%>
	    	<tr>
	    	<td class="configured-tag"><i class="icon-ok"></i> configured </td>
	    	<td class="property-title"> Geoserver Configuration </td> 
			<td class="configure-link"> <a href="<%= request.getContextPath() %>/admin?configureType=geoserver"><i class="icon-cogs"></i> Reconfigure Now</a> </td>   		
	    	</tr>
	<%
		} else if (geoserverConfigured != null && geoserverConfigured.equals(PropertyService.BYPASSED)){
	%>    		
	    	<tr>
	    	<td class="configured-tag"><i class="icon-ok"></i> bypassed </td>  
			<td class="property-title"> Geoserver Configuration </td>  
	<%
			if (propsConfigured != null && propsConfigured) {
	%>
	        	
			<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=geoserver"><i class="icon-cogs"></i>Reconfigure Now</a> </td>			       	
	<%
			} else {
	%> 
			<td class="configure-link inactive"> Configure Global Properties First </td>
	<%
			}
				%>     	
	    	</tr>
	<%
		} else {
	%>    		
	    	<tr>
	    	<td class="unconfigured-tag">unconfigured </td>  
	    	<td class="property-title"> Geoserver Configuration </td>   
	<%
			if (propsConfigured != null && propsConfigured) {
	%>
	        	
			<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=geoserver"><i class="icon-cogs"></i> Configure Now</a> </td>			
	        	
	<%
			} else {
	%> 
			<td class="configure-link inactive"> Configure Global Properties First </td>
	<%
			}
	%>     	
	    	</tr>
	<%
		}
	%>
	
	<%
	
	    if (dataoneConfigured != null && dataoneConfigured.equals(PropertyService.CONFIGURED)) {
	%>
	    	<tr>
	    	<td class="configured-tag"><i class="icon-ok"></i> configured</td>
	    	<td class="property-title"> Dataone Configuration </td> 
			<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=dataone"><i class="icon-cogs"></i>Reconfigure Now</a> </td>   		
	    	</tr>
	<%
		} else if (dataoneConfigured != null && dataoneConfigured.equals(PropertyService.BYPASSED)){
	%>    		
	    	<tr>
	    	<td class="configured-tag"><i class="icon-ok"></i> bypassed </td>  
			<td class="property-title"> Dataone Configuration </td>  
	<%
			if (propsConfigured != null && propsConfigured) {
	%>
	        	
			<td class="configure-link"> <a href="<%= request.getContextPath() %>/admin?configureType=dataone"><i class="icon-cogs"></i> Reconfigure Now</a> </td>			       	
	<%
			} else {
	%> 
			<td class="configure-link inactive"> Configure Global Properties First </td>
	<%
			}
				%>     	
	    	</tr>
	<%
		} else {
	%>    		
	    	<tr>
	    	<td class="unconfigured-tag">unconfigured </td>  
	    	<td class="property-title"> Dataone Configuration </td>   
	<%
			if (propsConfigured != null && propsConfigured) {
	%>
	        	
			<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=dataone"> <i class="icon-cogs"></i> Configure Now</a> </td>			
	        	
	<%
			} else {
	%> 
			<td class="configure-link inactive"> Configure Global Properties First </td>
	<%
			}
	%>     	
	    	</tr>
	<%
		}
	%>
	
	<!-- replication -->
	   	<tr>
	   	<td class="configured-tag"><i class="icon-ok"></i> configured </td>  
	   	<td class="property-title"> Replication Configuration </td>   
	<%
		if (propsConfigured != null && propsConfigured) {
	%>
	        	
		<td class="configure-link"><a href="<%= request.getContextPath() %>/admin?configureType=replication"> <i class="icon-cogs"></i> Reconfigure Now</a> </td>			
	        	
	<%
		} else {
	%> 
		<td class="configure-link inactive"> Configure Global Properties First </td>
	<%
		}
	%>     	
	   	</tr>
	
	
	
	<!--  -->
	</table>
	
	<%
		if (metacatConfigured != null && metacatConfigured) {
	%>
		<br clear="right"/>
	<%
			if (metacatServletInitialized != null && metacatServletInitialized) {
	%>	
		<h3>Restarting Metacat</h3>
		<p> Since this is a reconfiguration, you will need to restart Metacat after any changes.</p>
		
		<p>The simplest way to restart metacat is to restart the entire servlet engine.
		   For Tomcat, this would mean calling "sudo /etc/init.d/tomcat6 restart" or
		   an equivalent command appropriate to your operating system. After restarting,
		   you can access your new Metacat server at the URL:
	      <a href="<%= contextURL %>"><%= contextURL  %></a>
		</p>
	<%
			} else {
	%> 	
				<div class="alert alert-success"><i class="icon-thumbs-up"></i>Configuration of Metacat is complete.  You can <a href="<%= request.getContextPath() %>">go to metacat</a> 
				now, or, if you are using the Metacat indexing component, restart Tomcat so that both webapps are initialized with these settings.  
				Note that this may take some time while the system initializes with the new configuration values.</div>
	<%
			}
		}
	%>			
</div>

	<%@ include file="./footer-section.jsp"%>

</body>
</html>
