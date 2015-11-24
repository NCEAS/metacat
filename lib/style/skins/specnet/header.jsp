<%@ page    language="java" %>
<%
/**
 * '$RCSfile$'
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 * '$Author$'
 * '$Date$'
 * '$Revision$'

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

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
  <title>SpecNet Data Registry</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/specnet/specnet.css"></link>
</head>

<body>
<table width="760" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td valign="middle" colspan="4" class="title">SpecNet Data Registry</td>
    <td rowspan="3" width="20%" valign="top"> 
      <div align="right"><img src="<%=STYLE_SKINS_URL%>/specnet/specnet-logo.png"></div>
    </td>
  </tr>
  <tr> 
    <td class="spacerrow" valign="top" colspan="4">&nbsp;</td>
  </tr>
  <tr> 
    <td valign="top" width="20%"> 
      <p><a href="http://www.specnet.info" target="_top"> SpecNet Home</a></p>
    </td>
    <td valign="top" width="20%"> 
      <p><a href="<%=STYLE_SKINS_URL%>/specnet/index.jsp" target="_top">Registry Home</a></p>
    </td>
    <td valign="top" width="20%"> 
      <p><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=specnet" target="_top">Register a <br>New Data Set</a></p>
    </td>
    <td valign="top" width="20%"> 
      <!--
      <p>
        <a href="<%=SERVLET_URL%>?action=query&operator=INTERSECT&anyfield=%25&organizationName=SpecNet&qformat=specnet&returndoctype=eml://ecoinformatics.org/eml-2.1.1&returndoctype=eml://ecoinformatics.org/eml-2.1.0&returndoctype=eml://ecoinformatics.org/eml-2.0.1&returndoctype=eml://ecoinformatics.org/eml-2.0.0&returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN&returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN&returnfield=dataset/title&returnfield=keyword&returnfield=originator/individualName/surName&returnfield=creator/individualName/surName&returnfield=originator/organizationName&returnfield=creator/organizationName" target="_top">Search for Data</a></p>
      -->
<p class="searchbox">
Search for Data<br />
<form method="POST" action="<%=SERVLET_URL%>" target="_top">
  <input value="INTERSECT" name="operator" type="hidden">   
  <input size="14" name="anyfield" type="text" value="">
  <input name="organizationName" value="SpecNet" type="hidden">
  <input name="action" value="query" type="hidden">
  <input name="qformat" value="specnet" type="hidden">
  <input name="enableediting" value="true" type="hidden">
  <input name="operator" value="UNION" type="hidden">
  <input name="returnfield" value="originator/individualName/surName" type="hidden">
  <input name="returnfield" value="originator/individualName/givenName" type="hidden">
  <input name="returnfield" value="creator/individualName/surName" type="hidden">
  <input name="returnfield" value="creator/individualName/givenName" type="hidden">
  <input name="returnfield" value="originator/organizationName" type="hidden">
  <input name="returnfield" value="creator/organizationName" type="hidden">
  <input name="returnfield" value="dataset/title" type="hidden">
  <input name="returnfield" value="keyword" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.1.1" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.1.0" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.0.1" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.0.0" type="hidden">
  <input name="returndoctype" value="-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN" type="hidden">
  <input name="returndoctype" value="-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN" type="hidden">
  <input name="returndoctype" value="-//NCEAS//resource//EN" type="hidden">
  <input name="returndoctype" value="-//NCEAS//eml-dataset//EN" type="hidden">
  <!-- <input value="Start Search" type="submit"> -->
</form>
</p>
    </td>
  </tr>
</table>
</body>
</html>
