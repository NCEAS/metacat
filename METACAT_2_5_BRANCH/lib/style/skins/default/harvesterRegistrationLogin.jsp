<%@ page language="java"
	import="java.util.Vector,edu.ucsb.nceas.metacat.util.OrganizationUtil"%>
<!--
  *  '$RCSfile$'
  *      Authors: Matt Jones, CHad Berkley
  *    Copyright: 2000 Regents of the University of California and the
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
  *
-->
<%@ include file="settings.jsp"%>
<%@ include file="session_vars.jsp"%>
<%
	Vector<String> organizationList = OrganizationUtil.getOrganizations();
%>
<head>
<title>Metacat Harvester Registration Login</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link href="<%=STYLE_SKINS_URL%>/default/default.css" rel="stylesheet"
	type="text/css">
<script language="javascript" type="text/javascript"
	src="<%=STYLE_SKINS_URL%>/default/default.js"></script>
</head>

<body>

	<h3>Metacat Harvester Registration Login</h3>

	<form name="myform" method="POST"
		action="<%=CONTEXT_URL%>/harvesterRegistrationLogin">

		<p>Please Enter Username, Organization, and Password</p>
		<table>
			<tr>
				<td>Username</td>
				<td>
					<input type="text" name="uid" maxlength="100" size="28">
				</td>
			</tr>
			<tr>
				<td>Organization</td>
				<td>
					<select name="o">
						<%
							for (String orgName : organizationList) {
						%>
						<option value="<%=orgName%>"><%=orgName%></option>
						<%
							}
						%>
					</select>
				</td>
			</tr>
			<tr>
				<td>Password</td>
				<td>
					<input type="password" name="passwd" maxlength="60" size="28">
				</td>
			</tr>
		</table>

		<input type="submit" value="Login"> <input type="reset"
			value="Reset">
	</form>
</body>
