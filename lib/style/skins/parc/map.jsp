<!--
  *  '$RCSfile$'
  *      Authors: Matt Jones
  *    Copyright: 2000 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *  For Details: http://www.nceas.ucsb.edu/
  *
  *   '$Author: tao $'
  *     '$Date: 2008-06-10 15:06:14 -0600 (Tue, 10 Jun 2008) $'
  * '$Revision: 3979 $'
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
-->
<%@ include file="settings.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>PARC Data Repository Interactive Map</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/parc/parc.css">
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_SKINS_URL%>/parc/parc.js"></script>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_COMMON_URL%>/branding.js"></script>
  <script type="text/javascript" 
          src="<%=STYLE_SKINS_URL%>/parc/navigation.js"></script>
  <script type="text/javascript"
          src="<%=STYLE_SKINS_URL%>/parc/search.js"></script>

</head>
<body id="Overview" onload="loginStatus('<%=SERVLET_URL%>', '<%=CGI_URL%>');">
    <div id="main_wrapper">

    <div id="header">
    </div>
    <div id="navigation">
        <p></p>
        <ul id="main_nav">
            <ul class="menu">
                <li class="collapsed"><a href="<%=SERVER_URL%>">Home</a></li>
                <li class="collapsed"><a href="<%=CONTEXT_URL%>">Repository</a></li>
                <li class="collapsed"><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=parc">Register</a></li>
                <li class="collapsed"><span id="login_block"></span></li>
            </ul>
        </ul>
    </div> <!-- navigation -->
    <div id="content_wrapper">
        <h1>PARC Data Repository Interactive Map</h1>

        <iframe scrolling="no" frameborder="0" width="780" height="420" src="<%=STYLE_COMMON_URL%>/spatial/map.jsp">
          You need iframe support 
        </iframe>

<div id="footer">
    <div id="footer_logos">
    </div> <!-- footer_contact -->
</div>

</div> <!-- id="content_wrapper"-->

</div>

</body>
</html>
