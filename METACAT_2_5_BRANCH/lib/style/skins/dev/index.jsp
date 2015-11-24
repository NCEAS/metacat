<%@ page    language="java" %>
<% 
 /**
  *  '$RCSfile$'
  *      Authors: Matt Jones
  *    Copyright: 2000 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *  For Details: http://www.nceas.ucsb.edu/
  *
  *   '$Author$'
  *     '$Date$'
  * '$Revision$'
  * 
  * This is an HTML document for displaying metadata catalog tools
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

<%@ include file="./settings.jsp"%>

<html>
<head>
<title>KNB Data Search</title>
<meta HTTP-EQUIV="refresh" CONTENT="1;URL=<%=LOCAL_SERVLET_URL%>?action=query&operator=INTERSECT&anyfield=%25&qformat=default&returndoctype=eml://ecoinformatics.org/eml-2.1.1&returndoctype=eml://ecoinformatics.org/eml-2.1.0&returndoctype=eml://ecoinformatics.org/eml-2.0.1&returndoctype=eml://ecoinformatics.org/eml-2.0.0&returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN&returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN&returnfield=dataset/title&returnfield=citation/title&returnfield=software/title&returnfield=protocol/title&returnfield=keyword&returnfield=originator/individualName/surName&returnfield=creator/individualName/surName&returnfield=originator/organizationName&returnfield=creator/organizationName" />
</head>
<body bgcolor="WHITE">
</body>
</html>
