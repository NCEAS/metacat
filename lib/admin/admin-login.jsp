<%@ page language="java" %> 
<%@ page import="edu.ucsb.nceas.metacat.util.OrganizationUtil,edu.ucsb.nceas.metacat.properties.PropertyService" %>
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
	Vector<String> adminList = (Vector<String>)request.getAttribute("adminList");;
%>

<html>
	<head>

	<title>Database Install/Upgrade Utility</title>
	<%@ include file="./head-section.jsp"%>

	</head>
	<body>
	<%@ include file="./header-section.jsp"%>
		
		<div class="document">
			<h2>Administrator Login</h2>
			
			<p>Account login page.</p>
			
			<%@ include file="./page-message-section.jsp"%>
			<table class="admin-login">
				<tr>
					<td><h4>Administrators</h4></td>
					<td>
						<select class="username-input" name="username">
			<%
						if (adminList != null) {
							for(String adminName : adminList) {
			%>
							<option><%= adminName %></option>
			<%
							}
						}
			%>
					</select>
					</td>
				</tr>
				<tr>
					<td></td>
					<td class="textinput-description">[Fully qualified administrator username]</td>
				</tr>
			</table>
			<div class="orcid-btn-wrapper">
				<div class="orcid-flex">
					<a href=<%= "https://cn.dataone.org/portal/oauth?action=start&amp;target=" + request.getRequestURL() %> class="signin orcid-btn update-orcid-sign-in-url
					orcid-flex" id="orcidLogin">
						<img src="<%= request.getContextPath() %>/admin/images/orcid_64x64.png">
						<span>Sign in with ORCID</span>
					</a>
				</div>
			</div>
		</div>
	</body>
	<script>
	    window.onload = function() {
	        const localJWTToken = localStorage.getItem("metacatAdminJWTToken");

            if (localJWTToken !== null) {
                // Get request with token
                console.log("JWT Token Found! Token: " + localJWTToken)

                var xhr2 = new XMLHttpRequest();

                xhr2.onreadystatechange = function() {
                    if (xhr2.readyState === XMLHttpRequest.DONE) {
                        if (xhr2.status === 200) {
                            // Reload page
                            const adminUrl = '<%= request.getContextPath()%>/admin?configureType=login&processForm=true'
                            console.log("Admin URL Manual:" + adminUrl)
                            window.location.replace(adminUrl)
                        }
                    };
                }

                xhr2.open('GET', '.', true);
                xhr2.setRequestHeader('Authorization', 'Bearer ' + localJWTToken);
                xhr2.withCredentials = true;
                xhr2.send();

            } else {
                // Attempt to get the JWT token
                var xhr = new XMLHttpRequest();

                xhr.onreadystatechange = function() {
                    if (xhr.readyState === XMLHttpRequest.DONE) {
                        if (xhr.status === 200 && xhr.responseText.length !== 0) {
                            let jwtAdminToken = xhr.responseText;
                            console.log("Retrieved Token:" + jwtAdminToken)
                            localStorage.setItem("metacatAdminJWTToken", jwtAdminToken);
                            let setToken = localStorage.getItem("metacatAdminJWTToken");
                            console.log("Confirm local storage item has been set: " + setToken)
                            window.location.reload()
                        } else {
                            console.log("Unable to retrieve token: " + xhr.status)
                        }
                    };
                }

                xhr.open('GET', 'https://cn.dataone.org/portal/token', true);
                xhr.withCredentials = true; // Include credentials (cookies) in the request
                xhr.send();
            }
	    }
	</script>
</html>
