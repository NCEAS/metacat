<%@ page    language="java" %>
<%
/**
 * '$RCSfile$'
 * Copyright: 2003 Regents of the University of California and the
 *            National Center for Ecological Analysis and Synthesis
 *  '$Author$'
 *    '$Date$'
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

<%@ include file="../../common/common-settings.jsp"%>
<%@ include file="../../common/configure-check.jsp"%>

<!--____________________________max_width____________________________________-->

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
  <title>Kepler Analytical Repository</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/kepler/kepler.css"></link>
</head>

<body>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td rowspan="3" width="15%" valign="top"> 
      <div align="left"><img src="<%=STYLE_SKINS_URL%>/kepler/kepler-logo.png"></div>
    </td>
    <td valign="middle" colspan="2" class="title">Kepler Analytical Component Repository</td>
  </tr>
  <tr> 
    <td class="spacerrow" valign="top" colspan="3">&nbsp;</td>
  </tr>
  <tr>
    <td valign="top" width="30%"> 
      <p><a href="http://www.kepler-project.org/" target="_top">Kepler Home</a></p>
    </td>
    <td valign="top" width="30%"> 
      <p><a href="<%=STYLE_SKINS_URL%>/kepler/index.jsp" target="_top">Repository Home</a></p>
    </td>
    <td valign="top" width="30%"> </td>
  </tr>
</table>
</body>
</html>
