<%@ page    language="java" %>
<% 
 /**
  *  '$RCSfile$'
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
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file showing the resultset of a query
  * into an HTML format suitable for rendering with modern web browsers.
  */
%>
<%@ include file="settings.jsp"%>
<%@ include file="session_vars.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>

<head>
<title>Metacat Data Catalog </title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
	<link rel="stylesheet" type="text/css" 
		  href="<%=STYLE_SKINS_URL%>/lter/lter.css">
	<script language="javascript" type="text/javascript"
			src="<%=STYLE_SKINS_URL%>/lter/lter.js"></script>
	<script language="javascript" type="text/javascript"
			src="<%=STYLE_COMMON_URL%>/branding.js"></script>
  <script language="javascript" type="text/javascript" src="<%=STYLE_SKINS_URL%>/lter/branding_extensions.js"></script>
</head>

<body>
  <script language="javascript">
	  insertTemplateOpening("<%=CONTEXT_URL%>");
      // insertLoginBox("<%=CONTEXT_URL%>");
	  insertAdvancedSearchBox("<%=CONTEXT_URL%>");
    insertTemplateClosing("<%=CONTEXT_URL%>");
  </script>
</body>

</html>
