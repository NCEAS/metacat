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
<style type="text/css">
<!--
.chalkboard {
	background-image: url(<%=STYLE_SKINS_URL%>/first/images/chalkboard_left.png);
	background-repeat: repeat-y;
	height: 100%;
}
-->
</style>	
</head>

<body>
<div id="chalkboard" class="chalkboard"></div>
</body>
</html>
