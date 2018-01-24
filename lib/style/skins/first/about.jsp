<%@ page    language="java" %>
<%
/**
 * 
 * '$RCSfile$'
 * Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    '$Author: leinfelder $'
 *      '$Date: 2008-09-19 16:51:10 -0700 (Fri, 19 Sep 2008) $'
 * '$Revision: 4362 $'
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
<title>FIRST Assessment Metadata Repository</title>
<link rel="stylesheet" type="text/css"
	href="<%=STYLE_SKINS_URL%>/first/first.css">
<script language="JavaScript" type="text/JavaScript"
	src="<%=STYLE_SKINS_URL%>/first/first.js"></script>
<script language="JavaScript" type="text/JavaScript"
	src="<%=STYLE_SKINS_URL%>/first/search.js"></script>
<script language="JavaScript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/branding.js"></script>
<script language="Javascript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/prototype-1.5.1.1/prototype.js">
</script>	
</head>
<body>
<script language="JavaScript">
          insertTemplateOpening("<%=CONTEXT_URL%>");
          insertSearchBox("<%=CONTEXT_URL%>");
      </script>

<table width="100%" border="0" cellspacing="20" cellpadding="0">
	<tr>
		<th colspan="1">About</th>
	</tr>
	<tr>
		<td>
			<p class="intro">
				Welcome to the FIRST Assessment Repository. 
				The repository contains detailed information on assessment tools used
				[primarily] in biological and ecological science courses.
				Records herein adhere to a standard educational metadata specification 
				and enable educators to explore effective teaching models and techniques.
			</p>
			<p class="intro">	
				The aim is to associate outcome measures (i.e. student test scores) with the particular concepts
				on which the student is being tested and also associate the specific instructional techniques employed 
				when presenting that material to the student.
			</p>
				
			<p class="intro">
				The repository relies primarily on data collected from and submitted by
				professors and instructors.  While this system is still in active development,
				a quick search should reveal the depth, flexibility, and utility of the model. 
				And, of course, feedback is strongly encouraged and greatly appreciated.
			</p>
			<p class="intro">
				Information about the FIRST Project and it's activities is currently available through the 
				<a href="http://www.first2.org/">FIRST</a> site.
				
			<p class="intro">
				If you have any questions, comments or problems,
				please contact the repository developer and administrator at
				<a	href="mailto:leinfelder@nceas.ucsb.edu">leinfelder@nceas.ucsb.edu</a>
			</p>
		</td>
	</tr>
</table>

<script language="JavaScript">          
    insertTemplateClosing("<%=CONTEXT_URL%>");
</script>
</body>
</html>
