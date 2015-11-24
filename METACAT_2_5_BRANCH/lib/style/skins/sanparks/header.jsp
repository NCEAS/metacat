<%@ page language="java"%>
<%
	 /*
	 '$RCSfile$'
	 Copyright: 2003 Regents of the University of California and the
	 National Center for Ecological Analysis and Synthesis
	 '$Author$'
	 '$Date$'
	 '$Revision$'
	
	 This program is free software; you can redistribute it and/or modify
	 it under the terms of the GNU General Public License as published by
	 the Free Software Foundation; either version 2 of the License, or
	 (at your option) any later version.
	
	 This program is distributed in the hope that it will be useful,
	 but WITHOUT ANY WARRANTY; without even the implied warranty of
	 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	 GNU General Public License for more details.
	
	 You should have received a copy of the GNU General Public License
	 along with this program; if not, write to the Free Software
	 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
	 */
%>
<!--____________________________max_width____________________________________-->
<%@ include file="settings.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>SANParks - South African National Park Data Repository</title>
<link rel="stylesheet" type="text/css" href="<%=STYLE_SKINS_URL%>/sanparks/sanparks.css"/>
</head>

<body>
<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0" align="center">
	<!-- Header -->
	<tr style="background: #7BB865;">
		<td style="background: #124325;"><a href="http://www.SANParks.org/" title="SANParks.org Home"><img
			border="0" src="<%=STYLE_SKINS_URL%>/sanparks/images/logofade.jpg" alt="SANParks.org Home" /></a>
		</td>
		<td align="center" nowrap="nowrap" width="100%">
			<table width="100%" border="0" cellspacing="0" cellpadding="0">
				<tr height="92">
					<td colspan="3" nowrap="nowrap">
						<span class="headertitle">
							SANParks
							<br/>
							South African National Park Data Repository
						</span>
					</td>
				</tr>
				<tr valign="bottom" height="20" bgcolor="#124325">
					<td nowrap="nowrap">
						<a href="./" target="_top" class="headermenu">Repository Home</a>
					</td>
					<td nowrap="nowrap">
						<a href="<%=CGI_URL%>/register-dataset.cgi?cfg=sanparks" target="_top" class="headermenu">Register Data</a>
					</td>
					<td nowrap="nowrap">
						<a href="searchWorkflowRunMain.jsp" target="_top" class="headermenu">TPC Status</a>
					</td>
					<td nowrap="nowrap">
						<a href="searchWorkflowMain.jsp" target="_top" class="headermenu">TPC Workflows</a>
					</td>
				</tr>
			</table>
		</td>
		<td valign="bottom" align="right">
			<img src='<%=STYLE_SKINS_URL%>/sanparks/images/giraffe.jpg' alt='Giraffe' />
		</td>
	</tr>
</table>
</body>
</html>