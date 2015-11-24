<%@ page    language="java" %>
<%
/**
 * '$RCSfile$'
 * Copyright: 2008 Regents of the University of California and the
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



<table width="50%" border="0" cellspacing="0" cellpadding="0">
	<tr>
	  <td colspan="2">
	    <p style="font-size: 12px;font-family:arial;">
      The repository search system is used to locate Kepler 
	      analytical components of interest
	      </p>
	  </td>
	</tr>
</table>
<br/>
<table width="50%" border="0" cellspacing="0" cellpadding="0">
	<tr>
	  <td>
<form method="get" action="<%=SERVLET_URL%>" target="_top">
  <input value="INTERSECT" name="operator" type="hidden">
  <input id="searchText" size="14" name="anyfield" type="text" value="">
  <input name="action" value="query" type="hidden">
  <input name="qformat" value="kepler" type="hidden">
  <input name="enableediting" value="false" type="hidden">
  <input name="operator" value="UNION" type="hidden">
  <input name="pagesize" value="10" type="hidden">
  <input name="pagestart" value="0" type="hidden">
  <input name="returnfield" value="karFileName" type="hidden">
  <input name="returnfield" value="karEntry/karEntryXML/entity/@name" type="hidden">
  <input name="returnfield" value="mainAttributes/lsid" type="hidden">
  <input name="returnfield" value="karEntry/karEntryXML/entity/property[@name='author']/configure" type="hidden">
  <input name="returnfield" value="karEntry/karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='version']/configure" type="hidden">
  <input name="returnfield" value="karEntry/karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='description']/configure" type="hidden">
  <input name="returnfield" value="karEntry/karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='userLevelDocumentation']/configure" type="hidden">
  <input name="returndoctype" value="http://www.kepler-project.org/kar-2.0.0" type="hidden">
  <input name="returndoctype" value="http://www.kepler-project.org/kar-2.1.0" type="hidden">
  <input value="Search Components" type="submit">
</form>
	    </td>
	  <td align="right">
<form id="browseForm" method="get" action="<%=SERVLET_URL%>" target="_top">
  <input value="INTERSECT" name="operator" type="hidden">
  <input size="14" name="anyfield" type="hidden" value="">
  <input name="action" value="query" type="hidden">
  <input name="qformat" value="kepler" type="hidden">
  <input name="enableediting" value="false" type="hidden">
  <input name="operator" value="UNION" type="hidden">
  <input name="pagesize" value="10" type="hidden">
  <input name="pagestart" value="0" type="hidden">
 <input name="returnfield" value="karEntry/karEntryXML/entity/@name" type="hidden">
 <input name="returnfield" value="karFileName" type="hidden">
  <input name="returnfield" value="mainAttributes/lsid" type="hidden">
  <input name="returnfield" value="karEntry/karEntryXML/entity/property[@name='author']/configure" type="hidden">
  <input name="returnfield" value="karEntry/karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='version']/configure" type="hidden">
  <input name="returnfield" value="karEntry/karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='description']/configure" type="hidden">
  <input name="returnfield" value="karEntry/karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='userLevelDocumentation']/configure" type="hidden">
  <input name="returndoctype" value="http://www.kepler-project.org/kar-2.0.0" type="hidden">
  <input name="returndoctype" value="http://www.kepler-project.org/kar-2.1.0" type="hidden">
  <input value="Browse All Components" type="Submit">
</form>
	    </td>
	</tr>
      <tr>
	<td colspan="2">
	    <p style="font-size: 12px;font-family:arial;">
      Use a '%' symbol as a wildcard in searches
      (e.g., '%Constant%' would locate any phrase with the word
      'Constant' embedded within it).
	    </p>
	  </td>
	</tr>
	</table>