<%@ page language="java" %>
<% 
/**
 *  '$RCSfile$'
 *    Copyright: 2006 Regents of the University of California and the
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
<title>Metacat Configuration Succeeded</title>
<style language="Javascript">
body {
    font-family: Arial, sans-serif;
    font-size: 12pt;
    margin-left: 30px;
    margin-right: 30px;
    width: 700px;
}
.section {
    font-size: 16pt;
    font-weight: bold;
    border-bottom: solid #dddddd 1px;
}
</style>
</head>
<body>
<p><img src="metacat-logo-white.png" width="100px" align="right"/> 
<div class="document">
	<h2>Metacat Configuration Succeeded</h2>
	</p>
	
	<p>Congratulations!</p>
	<p>You have now successfully configured this installation of Metacat. All that
	   remains is to configure geoserver and restart the metacat servlet for the changes to take effect.
	</p>
	
	<br clear="right"/>
	<p class="section">Configuring Geoserver</p>
	<p>Metacat ships with <a href="http://docs.codehaus.org/display/GEOS/Home">Geoserver</a> to handle the spatial functionality.
	   Regardless of whether you plan on using the spatial functionality you should,
	   for security purposes, configure geoserver initially so that it doesn't use
	   the default password.
	   To configure geoserver with a new admin password:
	   <ol>
	    <li> Point your browser to 
	     <a href="<%= request.getScheme() + "://" +
	       request.getServerName() + ":" + request.getServerPort() +
	              request.getContextPath() %>/geoserver.jsp"><%= request.getScheme() + "://" +
		                      request.getServerName() + ":" + request.getServerPort() +
				                      request.getContextPath() %>/geoserver.jsp</a>. </li>
	     <li> Login using the default username and password ( <em>admin</em> / <em>geoserver</em> ) </li>
	    <li> Point your browser to 
	     <a href="<%= request.getScheme() + "://" +
	       request.getServerName() + ":" + request.getServerPort() +
	              request.getContextPath() %>/config/loginEdit.do"><%= request.getScheme() + "://" +
		                      request.getServerName() + ":" + request.getServerPort() +
				                      request.getContextPath() %>/config/loginEdit.do</a>, enter a new user and password then submit. </li>
	     <li> In the upper-left, click <em>Apply</em> then <em>Save</em> to save your new password.
	
	
	    </ol>
	</p>
	
	<br clear="right"/>
	<p class="section">Restarting Metacat</p>
	<p>The simplest way to restart metacat is to restart the entire servlet engine.
	   For Tomcat, this would mean calling "sudo /etc/init.d/tomcat6 restart" or
	   an equivalent command appropriate to your operating system. After restarting,
	   you can access your new Metacat server at the URL:
	   <a href="<%= request.getScheme() + "://" + 
	       request.getServerName() + ":" + request.getServerPort() +
	       request.getContextPath() %>"><%= request.getScheme() + "://" + 
	                request.getServerName() + ":" + request.getServerPort() +
	                request.getContextPath() %></a>
	</p>
</div>
</body>
</html>
