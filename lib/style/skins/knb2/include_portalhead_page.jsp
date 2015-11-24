<%@ page    language="java" %>
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
<%@ include file="PORTAL_SETTINGS.jsp"%>
<%@ include file="include_session_vars.jsp"%>  
<% 
  //remember previous rel root value...
  String prevRRValue  = relativeRoot;
  //now set rel root to parent dir...
  relativeRoot = "..";
%>
<head>
  <title>KNB :: The Knowledge Network for Biocomplexity</title>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
  <link href="portalpages.css" rel="stylesheet" type="text/css">
  <script language="JavaScript" type="text/JavaScript" src="portalpages.js"></script>
  </head>

<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
  <%@ include file="include_portalhead.jsp" %>
</body>
</html>

<% 
  //restore previous rel root value...
  relativeRoot = prevRRValue;
%>
