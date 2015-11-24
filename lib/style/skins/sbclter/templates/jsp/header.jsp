<%@ page    language="java" %>
<%
 /**
  *  '$RCSfile: index.jsp,v $'
  *    Copyright: 2000 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *  For Details: http://www.nceas.ucsb.edu/
  *
  *   '$Author: cjones $'
  *     '$Date: 2004/10/07 07:24:38 $'
  * '$Revision: 1.2 $'
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
  * into an HTML format suitable for rendering with modern web browsers
  */
%>
<%
      //* mob - created header.jsp.  includes r&l logos, banner, header menu
      //* searchbox for each page.  to get the urlencoded urls from the searchbox into resultset. probably not the best way. - but could use it for other pages
      //*/
%>

<%@ include file="portal_settings.jsp"%>
<%@ include file="include_session_vars.jsp"%>
<%
///////////////////////////////////////////////////////////////////////////////
// NOTE:
//
//  GLOBAL CONSTANTS (SETTINGS SUCH AS METACAT URL, LDAP DOMAIN	AND DEBUG
//  SWITCH) ARE ALL IN THE INCLUDE FILE "portal_settings.jsp"
///////////////////////////////////////////////////////////////////////////////
%>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
  <head>
    <title>SBCLTER Data Catalog</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link rel="stylesheet" type="text/css"
          href="<%=STYLE_SKINS_URL%>/sbclter/sbclter.css" />
  </head>

  <body>
    <!--begin the header area-->
    <!--
      these div's must have closing elements for the css to work. Don't
      reduce them to &lt;div id="blah" /&gt; 
    -->
    <div id="header">
      <!--begin the left logo area-->
      <div id="left_logo"></div>
      <!--end the left logo area-->
      <!--begin the banner area-->
      <div id="banner">
        <div class="header-title">
          Santa Barbara Coastal
        </div>
        <div class="header-subtitle">
          Long-Term Ecological Research
        </div>
      </div>
      <!--end the banner area-->
      <!--begin the right logo area-->
      <div id="right_logo"></div>
      <!--end the right logo area-->
    </div>
    <!--end the header area-->

    <!--begin the left sidebar area-->
    <!--
    <div id="left_sidebar"><img src="<%=STYLE_SKINS_URL%>/sbclter/images/nav_data_catalog_white.jpg" alt="data catalog" /><img src="<%=STYLE_SKINS_URL%>/sbclter/images/nav_search_orange.jpg" alt=""/><img src="<%=STYLE_SKINS_URL%>/sbclter/images/nav_kelp.jpg" alt="" /></div>
    -->
   
    <!-- begin content -->
   
    <div id="content">

      <!-- this should be in header -->
      <div class="header-menu">
        <a class="menu" href="index.html">Home</a> |
        <a class="menu" href="sites/index.html">Site</a> |
        <a class="menu" href="people/index.html">People</a> |
        <a class="menu" href="research/index.html">Research</a> |
        <a class="menu" href="data/index.html">Data</a> |
        <a class="menu" href="education/index.html">Education</a> |
        <a class="menu" href="affiliates/index.html">Affiliates</a>
      </div>                                                  

      <div class="content-area">


      <!-- begin login form area -->
      <!--
      <div id="loginbox">
        <form name="loginForm" method="POST" 
          action="<%=STYLE_SKINS_URL%>/sbclter/index.jsp">
          <label class="login"><%=loginStatus%></label>
          <% if (!isLoggedIn) {%> 
          <label class="login" for="username">username</label>
          <input class="login" type="text" name="username" value="<%=typedUserName%>" />
          <label class="login" for="password">password</label>
          <input class="login" type="password" name="password" />
          <input type="submit" name="loginAction" value="<%=loginButtonLabel%>" class="submit" />
          <% } else { %>
          <input type="submit" name="loginAction" value="<%=loginButtonLabel%>" class="submit" />
          <% } %>
          <input type="hidden" name="action" value="login">
          <input type="hidden" name="ldapusername"  value="">
          <input type="hidden" name="organization"  value="LTER">
          <input type="hidden" name="qformat" value="sbclter">
          <input type="hidden" name="enableediting" value="true">
        </form> 
      </div>
      -->
      <!-- end login form area -->


      <!-- begin search box area -->
     <div id="search-box">
      <table class="group group_border">
        <tr>
         <th>
            Data by Research group:
          </th> 
          <th>
            Keyword Search:
          </th>
        </tr>

        <tr>
        <td>
	<a 
	href="<%= SERVLET_URL %>?action=squery&amp;qformat=sbclter&amp;query=
      	<%= java.net.URLEncoder.encode(
	"<?xml version=\"1.0\"?>" +
         "<pathquery version=\"1.2\">" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>" +
           "<returnfield>dataset/title</returnfield>" +
           "<returnfield>dataTable/entityName</returnfield>" +
           "<returnfield>creator/individualName/surName</returnfield>" +
           "<returnfield>creator/organizationName</returnfield>" +
           "<returnfield>dataTable/physical/distribution/online/url</returnfield>" +
           "<querygroup operator=\"INTERSECT\">" +
             "<queryterm casesensitive=\"false\" searchmode=\"starts-with\">" +
               "<value>SBCLTER: Land</value>" +
               "<pathexpr>title</pathexpr>" +
             "</queryterm>" +
           "</querygroup>" +
         "</pathquery>") %>"
	>
            SBCLTER - Land
        </a>
        <br />

	<a 
	href="<%= SERVLET_URL %>?action=squery&amp;qformat=sbclter&amp;query=
      	<%= java.net.URLEncoder.encode(
	"<?xml version=\"1.0\"?>" +
         "<pathquery version=\"1.2\">" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>" +
           "<returnfield>dataset/title</returnfield>" +
           "<returnfield>dataTable/entityName</returnfield>" +
           "<returnfield>creator/individualName/surName</returnfield>" +
           "<returnfield>creator/organizationName</returnfield>" +
           "<returnfield>dataTable/physical/distribution/online/url</returnfield>" +
           "<querygroup operator=\"INTERSECT\">" +
             "<queryterm casesensitive=\"false\" searchmode=\"starts-with\">" +
               "<value>SBCLTER: Reef</value>" +
               "<pathexpr>title</pathexpr>" +
             "</queryterm>" +
           "</querygroup>" +
         "</pathquery>") %>"
	>           
              SBCLTER - Reef
        </a>
        <br />
             
             
	<a 
	href="<%= SERVLET_URL %>?action=squery&amp;qformat=sbclter&amp;query=
      	<%= java.net.URLEncoder.encode(
	"<?xml version=\"1.0\"?>" +
         "<pathquery version=\"1.2\">" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>" +
           "<returnfield>dataset/title</returnfield>" +
           "<returnfield>dataTable/entityName</returnfield>" +
           "<returnfield>creator/individualName/surName</returnfield>" +
           "<returnfield>creator/organizationName</returnfield>" +
           "<returnfield>dataTable/physical/distribution/online/url</returnfield>" +
           "<querygroup operator=\"INTERSECT\">" +
             "<queryterm casesensitive=\"false\" searchmode=\"starts-with\">" +
               "<value>SBCLTER: Ocean</value>" +
               "<pathexpr>title</pathexpr>" +
             "</queryterm>" +
           "</querygroup>" +
         "</pathquery>") %>"
	>           
             SBCLTER - Ocean
        </a>
        <br />
            
            
        <a 
	href="<%= SERVLET_URL %>?action=squery&amp;qformat=sbclter&amp;query=
      	<%= java.net.URLEncoder.encode(
	"<?xml version=\"1.0\"?>" +
         "<pathquery version=\"1.2\">" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>" +
           "<returnfield>dataset/title</returnfield>" +
           "<returnfield>dataTable/entityName</returnfield>" +
           "<returnfield>creator/individualName/surName</returnfield>" +
           "<returnfield>creator/organizationName</returnfield>" +
           "<returnfield>dataTable/physical/distribution/online/url</returnfield>" +
           "<querygroup operator=\"INTERSECT\">" +
             "<queryterm casesensitive=\"false\" searchmode=\"starts-with\">" +
               "<value>SBCLTER:</value>" +
               "<pathexpr>title</pathexpr>" +
             "</queryterm>" +
           "</querygroup>" +
         "</pathquery>") %>"
	>
             All SBCLTER Data Packages
        </a>

          </td>
	<td>
       <em>Search not yet available</em>




	</td>
        </tr>
     </table>
    </div>
    <!-- end search-box area -->



          </div>  <!-- end  content-area (starts after quick-links table) -->

          <!-- end mob's 20041103 add -->

      </div> <!-- end content-area -->
     </div> <!-- end data-catalog-area -->

    </div>    <!-- end content -->
  </body>
</html>
