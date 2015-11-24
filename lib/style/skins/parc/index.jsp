<%@ page    language="java" %>
<%
/**
 *  '$RCSfile$'
 *      Authors: Matt Jones, Christopher Jones
 *    Copyright: 2000 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *
 *   '$Author: daigle $'
 *     '$Date: 2008-07-06 22:25:34 -0600 (Sun, 06 Jul 2008) $'
 * '$Revision: 4080 $'
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
<%@ include file="settings.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>Data Repository | Palmyra Atoll Research Consortium</title>
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
<body id="Overview" onload="loginStatus('<%=SERVLET_URL%>','<%=CGI_URL%>');">
    <div id="main_wrapper">

    <div id="header">
    </div>
    <div id="navigation">
        <ul id="main_nav">
            <ul class="menu">
                <li class="collapsed"><a href="http://www.palmyraresearch.org">Home</a></li>
                <li class="collapsed"><a href="<%=CONTEXT_URL%>">Repository</a></li>
                <li class="collapsed"><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=parc">Register</a></li>
                <li class="collapsed"><span id="login_block"></span></li>
            </ul>
        </ul>
    </div> <!-- navigation --> 

    <div id="content_wrapper">
        <h1>PARC Data Repository</h1>

<p class ="intro">Welcome to the PARC Data Repository. This repository contains information 
about the research data sets collected and collated as part of PARC funded activities. 
Information in the PARC Data Repository is concurrently available through the <a href="http://knb.ecoinformatics.org/index.jsp">Knowledge Network for Biocomplexity (KNB)</a>, an international data repository. </span></p>
<p class="intro">A number of the data sets were synthesized from multiple data sources that originated from the efforts of many contributors, while others originated from a single investigator. Credit for the data sets in this repository goes to the investigators who collected the data, as well as to the PARC working groups and scientists who compiled the data for synthetic purposes. See each data package for a list of the people and institutions involved. </p>
<p class="intro"> If you have any questions, comments or problems, please contact the repository administrator at <a href="mailto:cjones@msi.ucsb.edu">cjones@msi.ucsb.edu</a> </p>
<br>
<table class="tables regular" cellpadding="8" cellspacing="0">
  <tr class="sectheader">
    <td class="borderbottom"><a name="search" ></a>
        <p align="center" class="largetext"> Search for Data Sets </p></td>
  </tr>
  <tr class="sectbody"></tr>
 
  <tr >
    <td align="left">
    <form method="POST" 
    	action="<%=SERVLET_URL%>" target="_top"
    	id="searchForm" name="searchForm" 
    	onSubmit="return checkSearch(this)">
        <div class="searchresultsdividerPale">
        <p>
        <input value="UNION" name="operator" type="hidden">
&nbsp;
        <input size="14" name="searchstring" type="text" value="" id="searchBox">
        <input name="query" type="hidden">
        <input name="qformat" value="parc" type="hidden">
        <input name="enableediting" value="true" type="hidden">
        <input type="hidden" name="action" value="squery">
        <input value="Search" type="submit">
        </p>
        <p>
        <input name="search" type="radio" checked>
        Search Title, Abstract, Keywords, Personnel (Quicker) <br>
        <input name="search" type="radio" id="searchAll">
        Search all fields (Slower) <br>
        </p>
        </div>
        </form> 
        <div align="center">
          <p align="left">This tool allows you to search the registry for data sets of interest. When you type text in the box and click on the "Search" button, the search will only be conducted within the title, author, abstract, and keyword fields. Checking the "Search All Fields" box will search on these and all other existing fields (this search will take more time). <br>
                <br>
          You can use the '%' character as a wildcard in your searches (e.g., '%biodiversity%' would locate any phrase with the word biodiversity embedded within it). </p>
      </div></td>
  </tr>
  <tr >
    <td class="borderbottom"><div align="center">
    	<a href="javascript:browseAll('searchForm')">Browse existing PARC data sets</a>
<br/><br/>
<!--a href="<%=STYLE_SKINS_URL%>/parc/map.jsp"> View Interactive Map </a--> 
</div>
</td></tr></table>
<br><br>
<table class="tables regular" cellpadding="8" cellspacing="0">
  <tr class="sectheader">
    <td class="borderbottom" colspan="2" align="left"><span class="label"> </span>
        <p align="center" class="largetext"> Register information about a new PARC data set </p></td>
  </tr>
  
 
  <tr >
    <td  colspan="2" align="center"><div align="left">
      <p><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=parc">Register Information Online</a> - This is a simple way to provide information (metadata) about the existence of a data set. The registration page is used to submit information about a <b>new</b> data set associated with PARC research.</p>
  
      <p align="center">OR</p>
      </div></td>
  </tr>
  <tr >
    <td class="borderbottom"><div align="left">
      <p align="center"></p>
      <table width="100%"  border="0" align="center" >
        <tr>
          <td width="110" class="templatecontentareaclass"><a href="http://knb.ecoinformatics.org/morphoportal.jsp"><img src="<%=CONTEXT_URL%>/style/images/MorphoButterfly.jpg" alt="Morpho Software Butterfly" width="100" height="100" border="0" ></a></td>
          <td width="550" valign="top" class="templateleftcolclass"><p >Use <a href="http://knb.ecoinformatics.org/morphoportal.jsp"><span class="style2">Morpho</span></a> software to register, document and upload data sets. </p>
              <p>Morpho is a data management tool for ecologists. It was created to provide an easy-to-use, cross-platform application for accessing and manipulating metadata (e.g. documentation) and data (both locally and on the network). Morpho allows ecologists to create metadata, (i.e. describe their data in a standardized format), and create a catalog of data and metadata upon which to query, edit and view data collections. In addition, Morpho provides the means to access network servers, in order to query, view and retrieve relevant, public ecological data!</p></td>
        </tr>
      </table>
    </div>
  </td></tr></table>
<br />
<p class="intro">This repository is an effort of the <a href="http://palmyraresearch.org">Palmyra Atoll Research Consortium (PARC)</a> and
is based on software developed by the
<a href="http://knb.ecoinformatics.org">Knowledge Network for 
Biocomplexity (KNB)</a>, and houses metadata that are compliant with 
<a href="http://knb.ecoinformatics.org/software/eml/">Ecological Metadata 
Language (EML)</a>.
</p>

<div id="footer">
    </div> <!-- footer_logos -->
    <div id="footer_contact">
    </div> <!-- footer_contact -->
</div>

</div> <!-- id="content_wrapper"-->

</div>
</body>
</html>
