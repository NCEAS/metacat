<%@page import="edu.ucsb.nceas.metacat.properties.PropertyService"%>
<%@page import="edu.ucsb.nceas.metacat.properties.SkinPropertyService"%>
<%
    /**
 *  '$RCSfile$'
 *    Copyright: 2000 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *
 *   '$Author: leinfelder $'
 *     '$Date: 2013-09-17 16:33:52 -0700 (Tue, 17 Sep 2013) $'
 * '$Revision: 8224 $'
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
<% 

String CFG = PropertyService.getProperty("application.default-style");
String email = SkinPropertyService.getProperty(CFG, "email.recipient");
%>
<%@ include file="common-settings.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
    <title>Metacat Default User Management</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <%@ include file="../../admin/head-section.jsp"%>
</head>
<body>
  
  <%@ include file="../../admin/header-section.jsp"%>
  <div class="document">
    <p>Please contact the administrator( <%=email%> ) to get a new account or reset password.
  </div>
 
</body>
</html>