<%@ page    language="java" %>
<%
/**
 * 
 * '$RCSfile$'
 * Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    '$Author: leinfelder $'
 *      '$Date: 2008-08-13 15:09:01 -0700 (Wed, 13 Aug 2008) $'
 * '$Revision: 4235 $'
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
     
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */  
%>

<%@ include file="../../common/common-settings.jsp"%>
<%@ include file="../../common/configure-check.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>FIRST Data Repository</title>
<link rel="stylesheet" type="text/css"
	href="<%=STYLE_SKINS_URL%>/first/first.css"></link>
</head>

<body>
<table width="100%" height="145" border="0" cellspacing="0" cellpadding="0" background="<%=STYLE_SKINS_URL%>/first/images/bkgrndgradient.gif">
	<tr valign="top" align="center">
		<td width="75%" colspan="3">
			<p class="intro">
			This repository is an effort of the <a target="_top" href="http://www.nceas.ucsb.edu">National Center for Ecological
			Analysis and Synthesis (NCEAS)</a> and is based on software developed by
			the <a target="_top" href="http://knb.ecoinformatics.org">Knowledge Network for
			Biocomplexity (KNB)</a>.
			<br/> 
			Supported by the National Science Foundation under award 0618501.</p>
		</td>	
	</tr>
	<tr align="center" valign="top">
		<td>
			<img src="<%=STYLE_SKINS_URL%>/first/images/nceas.jpg" border="1"/>
		</td>
		<td>
			<img src="<%=STYLE_SKINS_URL%>/first/images/msu_green.gif"/>
		</td>
		<td>
			<img src="<%=STYLE_SKINS_URL%>/first/images/nsf.jpg" border="1"/>
		</td>
	</tr>
</table>		
</body>
</html>
