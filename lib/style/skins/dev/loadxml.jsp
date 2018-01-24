<%@ page    language="java" %>
<%
/**
 *  '$RCSfile$'
 *      Authors: Matt Jones
 *    Copyright: 2008 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 * 
 * This is an HTML document for loading an xml document into Oracle
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
   
<html>
<head>
<title>MetaCat</title>
<link rel="stylesheet" type="text/css" href="./rowcol.css">
</head>
<body class="emlbody">
<b>MetaCat XML Loader</b>
<p>
Upload, Change, or Delete an XML document using this form.
</p>
<form action="<%=SERVLET_URL%>" target="right" method="POST">
  <strong>1. Choose an action: </strong>
  <input type="radio" name="action" value="insert" checked> Insert
  <input type="radio" name="action" value="update"> Update
  <input type="radio" name="action" value="delete"> Delete
  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="submit" value="Process Action">
  <br />
  <strong>2. Provide a Document ID </strong>
  <input type="text" name="docid"> 
  &nbsp;&nbsp;&nbsp;
  <br />
  <strong>3. Provide XML text </strong> (not needed for Delete)
  <strong>
  <br />
  <textarea name="doctext" cols="65" rows="15"></textarea>
  <br />
  <strong>4. Provide DTD text for upload </strong> (optional; not needed for Delete)
  <br />
  <textarea name="dtdtext" cols="65" rows="15"></textarea>
  <br />
</form>
<p />
</body>
</html>
