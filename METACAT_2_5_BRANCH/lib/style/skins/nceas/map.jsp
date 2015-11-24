<!--
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
-->
<%@ include file="settings.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>NCEAS Data Repository Interactive Map</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/nceas/nceas.css">
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_SKINS_URL%>/nceas/nceas.js"></script>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_COMMON_URL%>/branding.js"></script>
  <script type="text/javascript" 
          src="<%=STYLE_SKINS_URL%>/nceas/navigation.js"></script>
  <script type="text/javascript"
          src="<%=STYLE_SKINS_URL%>/nceas/search.js"></script>

</head>
<body id="Overview" onload="loginStatus('<%=SERVLET_URL%>', '<%=CGI_URL%>');">
    <div id="main_wrapper">

    <div id="header">
        <p>Skip to <a href="#navigation">navigation</a>, <a href="#main_content">main
        content</a>, <a href="#secondary_content">secondary content</a> or to <a
        href="#search">search</a>.</p> <h1><span></span><a href="/">NCEAS</a></h1>
    </div>
    <div id="navigation">
        <p></p>
        <ul id="main_nav">
            <ul class="menu">
                <li class="collapsed"><a href="http://www.nceas.ucsb.edu">Home</a></li>
                <li class="collapsed"><a href="http://data.nceas.ucsb.edu">Repository</a></li>
                <li class="collapsed"><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=nceas">Register</a></li>
                <li class="collapsed"><span id="login_block"></span></li>
            </ul>
        </ul>
    </div> <!-- navigation -->
    <div id="content_wrapper">
        <h1>NCEAS Data Repository Interactive Map</h1>

        <iframe scrolling="no" frameborder="0" width="780" height="420" src="<%=STYLE_COMMON_URL%>/spatial/map.jsp">
          You need iframe support 
        </iframe>

<div id="footer">
    <div id="footer_logos">
                <a href="http://www.msi.ucsb.edu/"><img
src="images/logo_msi.jpg" alt="MSI: Marine Science Institute" height="66"
width="132"></a><a href="http://www.nsf.gov/"><img src="images/logo_nsf.jpg"
alt="NSF: National Science 
Foundation" height="66" width="70"></a><a href="http://www.ucsb.edu/"><img
src="images/logo_ucsb.jpg" alt="UCSB: University of California at Santa Barbara"
height="66" width="132"></a>
    </div> <!-- footer_logos -->
    <div id="footer_contact">
        <span class="contact_name">National Center for Ecological Analysis and Synthesis</span>

        <span class="contact_address">735 State Street, Suite 300, Santa Barbara, CA 93101</span>
        <span class="copyright">Copyright &copy; 2007 The Regents of the
University of California, All Rights Reserved</span>
        <span class="copyright"><a href="http://www.ucsb.edu/" title="Visit the
UCSB website">UC Santa Barbara</a>, Santa Barbara CA 93106 &bull; (805)
893-8000</span>
        <span class="copyright"><a href="mailto:webmaster@nceas.ucsb.edu"
title="E-mail the NCEAS webmaster">Contact</a> &bull; <a
href="http://www.ucsb.edu/policies/terms-of-use.shtml">Terms of Use</a> &bull; <a
href="http://www.nceas.ucsb.edu/accessibility" title="NCEAS is committed to the
accessibility of its products for all users">Accessibility</a></span>

        
    </div> <!-- footer_contact -->
</div>

</div> <!-- id="content_wrapper"-->

</div>

</body>
</html>
