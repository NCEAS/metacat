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
<%@ include file="templates/jsp/portal_settings.jsp"%>
<%@ include file="templates/jsp/include_session_vars.jsp"%>
<%
///////////////////////////////////////////////////////////////////////////////
// NOTE:
//
//  GLOBAL CONSTANTS (SETTINGS SUCH AS METACAT URL, LDAP DOMAIN	AND DEBUG
//  SWITCH) ARE ALL IN THE INCLUDE FILE "portal_settings.jsp"
///////////////////////////////////////////////////////////////////////////////
%>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" 
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
  <head>
    <title>PISCO Data Catalog</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link rel="stylesheet" type="text/css" 
          href="<%=STYLE_SKINS_URL%>/pisco/pisco.css" />
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
      <div id="banner"></div>
      <!--end the banner area-->
      <!--begin the right logo area-->
      <div id="right_logo"></div>
      <!--end the right logo area-->
    </div>
    <!--end the header area-->

    <!--begin the left sidebar area-->
    <!--
      these div's must have closing elements for the css to work. Don't
      reduce them to &lt;div id="blah" /&gt; 
    -->
   
    <!--
    The following div has purposefully been condensed to one line in order to
    deal with an MSIE bug that introduces whitespace incorrectly when
    rendering the CSS. Please keep it all on one line in the code. When not
    condensed, it would look like:
   
    <div id="left_sidebar">
      <img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_data_catalog_white.jpg" alt="data catalog" />
      <img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_search_orange.jpg" alt=""/>
      <img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_kelp.jpg" alt="" />
    </div>
    -->
    <div id="left_sidebar"><img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_data_catalog_white.jpg" alt="data catalog" /><img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_search_orange.jpg" alt=""/><img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_kelp.jpg" alt="" /></div>
   
    <!--
    The lines below may be used in the above div based on which images should
    be in the navigation bar.
    -->
    <!--img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_data_catalog_orange.jpg" alt="" /-->
    <!--img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_login_orange.jpg" alt="" /-->
    <!--img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_search_white.jpg" -->
    <!--img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_insert_white.jpg" alt=""/-->
    <!--img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_insert_orange.jpg" alt="" /-->
    <!--img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_modify_white.jpg" alt="" /-->
    <!--img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_modify_orange.jpg" alt="" /-->
    <!--img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_logout_orange.jpg" alt=""/-->
    <!--img src="<%=STYLE_SKINS_URL%>/pisco/images/nav_blank_blue.jpg" alt=""/-->
    <!--end the left sidebar area-->

    <!-- begin content form area -->
   
    <div id="content">

      <!-- begin login form area -->
      <!--
      <div id="loginbox">
        <form name="loginForm" method="POST" 
          action="<%=STYLE_SKINS_URL%>/pisco/index.jsp">
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
          <input type="hidden" name="organization"  value="PISCO">
          <input type="hidden" name="qformat" value="pisco">
          <input type="hidden" name="enableediting" value="true">
        </form> 
      </div>
      -->
      <!-- end login form area -->

      <!-- begin search form area -->
      <table class="group group_border">
        <tr>
          <th colspan="2">
            Category Search:
          </th>
        </tr>
        <tr>
          <td>
            <a
            href="<%=SERVLET_URL%>?action=squery&amp;qformat=pisco&amp;query=%3C?xml%20version=%221.0%22?%3E%3Cpathquery%20version=%221.2%22%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.1.1%3C/returndoctype%3Eeml://ecoinformatics.org/eml-2.1.0%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.1%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.0%3C/returndoctype%3E%3Creturnfield%3Edataset/title%3C/returnfield%3E%3Creturnfield%3EdataTable/entityName%3C/returnfield%3E%3Creturnfield%3Ecreator/individualName/surName%3C/returnfield%3E%3Creturnfield%3Ecreator/organizationName%3C/returnfield%3E%3Creturnfield%3EdataTable/physical/distribution/online/url%3C/returnfield%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3EPISCO:%3C/value%3E%3Cpathexpr%3Etitle%3C/pathexpr%3E%3C/queryterm%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EOceanographic%20Sensor%20Data%3C/value%3E%3Cpathexpr%3EkeywordSet/keyword%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EPISCO%20Categories%3C/value%3E%3Cpathexpr%3EkeywordSet/keywordThesaurus%3C/pathexpr%3E%3C/queryterm%3E%3C/querygroup%3E%3C/querygroup%3E%3C/pathquery%3E">
           <!--
           <?xml version="1.0"?>
             <pathquery version="1.2">
             <returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>
             <returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>
               <returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>
               <returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>
               <returnfield>dataset/title</returnfield>
               <returnfield>dataTable/entityName</returnfield>
               <returnfield>creator/individualName/surName</returnfield>
               <returnfield>creator/organizationName</returnfield>
               <returnfield>dataTable/physical/distribution/online/url</returnfield>
               <querygroup operator="INTERSECT">
                 <queryterm casesensitive="false" searchmode="starts-with">
                   <value>PISCO:</value>
                   <pathexpr>title</pathexpr>
                 </queryterm>
                 <querygroup operator="INTERSECT">
                   <queryterm casesensitive="true" searchmode="equals">
                     <value>Oceanographic Sensor Data</value>
                     <pathexpr>keywordSet/keyword</pathexpr>
                   </queryterm>
                   <queryterm casesensitive="true" searchmode="equals">
                     <value>PISCO Categories</value>
                     <pathexpr>keywordSet/keywordThesaurus</pathexpr>
                   </queryterm>
                 </querygroup>
               </querygroup>
             </pathquery>
             -->
                 Oceanographic Sensor Data
                 </a>
                 <br />

            <a
            href="<%=SERVLET_URL%>?action=squery&amp;qformat=pisco&amp;query=%3C?xml%20version=%221.0%22?%3E%3Cpathquery%20version=%221.2%22%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.1.1%3C/returndoctype%3Eeml://ecoinformatics.org/eml-2.1.0%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.1%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.0%3C/returndoctype%3E%3Creturnfield%3Edataset/title%3C/returnfield%3E%3Creturnfield%3EdataTable/entityName%3C/returnfield%3E%3Creturnfield%3Ecreator/individualName/surName%3C/returnfield%3E%3Creturnfield%3Ecreator/organizationName%3C/returnfield%3E%3Creturnfield%3EdataTable/physical/distribution/online/url%3C/returnfield%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3EPISCO:%3C/value%3E%3Cpathexpr%3Etitle%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3Episco%3C/value%3E%3Cpathexpr%3E/eml/@packageId%3C/pathexpr%3E%3C/queryterm%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EIntertidal%20Community%20Survey%20Data%3C/value%3E%3Cpathexpr%3EkeywordSet/keyword%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EPISCO%20Categories%3C/value%3E%3Cpathexpr%3EkeywordSet/keywordThesaurus%3C/pathexpr%3E%3C/queryterm%3E%3C/querygroup%3E%3C/querygroup%3E%3C/pathquery%3E">
             Intertidal Community Survey Data
             </a>
             <br />
            <a
            href="<%=SERVLET_URL%>?action=squery&amp;qformat=pisco&amp;query=%3C?xml%20version=%221.0%22?%3E%3Cpathquery%20version=%221.2%22%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.1.1%3C/returndoctype%3Eeml://ecoinformatics.org/eml-2.1.0%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.1%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.0%3C/returndoctype%3E%3Creturnfield%3Edataset/title%3C/returnfield%3E%3Creturnfield%3EdataTable/entityName%3C/returnfield%3E%3Creturnfield%3Ecreator/individualName/surName%3C/returnfield%3E%3Creturnfield%3Ecreator/organizationName%3C/returnfield%3E%3Creturnfield%3EdataTable/physical/distribution/online/url%3C/returnfield%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3EPISCO:%3C/value%3E%3Cpathexpr%3Etitle%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3Episco%3C/value%3E%3Cpathexpr%3E/eml/@packageId%3C/pathexpr%3E%3C/queryterm%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3ESubtidal%20Community%20Survey%20Data%3C/value%3E%3Cpathexpr%3EkeywordSet/keyword%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EPISCO%20Categories%3C/value%3E%3Cpathexpr%3EkeywordSet/keywordThesaurus%3C/pathexpr%3E%3C/queryterm%3E%3C/querygroup%3E%3C/querygroup%3E%3C/pathquery%3E">
             Subtidal Community Survey Data
             </a>
            <br />
               </td>
              <td>
                <a
                href="<%=SERVLET_URL%>?action=squery&amp;qformat=pisco&amp;query=%3C?xml%20version=%221.0%22?%3E%3Cpathquery%20version=%221.2%22%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.1.1%3C/returndoctype%3Eeml://ecoinformatics.org/eml-2.1.0%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.1%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.0%3C/returndoctype%3E%3Creturnfield%3Edataset/title%3C/returnfield%3E%3Creturnfield%3EdataTable/entityName%3C/returnfield%3E%3Creturnfield%3Ecreator/individualName/surName%3C/returnfield%3E%3Creturnfield%3Ecreator/organizationName%3C/returnfield%3E%3Creturnfield%3EdataTable/physical/distribution/online/url%3C/returnfield%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3EPISCO:%3C/value%3E%3Cpathexpr%3Etitle%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3Episco%3C/value%3E%3Cpathexpr%3E/eml/@packageId%3C/pathexpr%3E%3C/queryterm%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EMicrochemistry%20Data%3C/value%3E%3Cpathexpr%3EkeywordSet/keyword%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EPISCO%20Categories%3C/value%3E%3Cpathexpr%3EkeywordSet/keywordThesaurus%3C/pathexpr%3E%3C/queryterm%3E%3C/querygroup%3E%3C/querygroup%3E%3C/pathquery%3E">
            Microchemistry Data
            </a>
            <br />
            <a
            href="<%=SERVLET_URL%>?action=squery&amp;qformat=pisco&amp;query=%3C?xml%20version=%221.0%22?%3E%3Cpathquery%20version=%221.2%22%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.1.1%3C/returndoctype%3Eeml://ecoinformatics.org/eml-2.1.0%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.1%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.0%3C/returndoctype%3E%3Creturnfield%3Edataset/title%3C/returnfield%3E%3Creturnfield%3EdataTable/entityName%3C/returnfield%3E%3Creturnfield%3Ecreator/individualName/surName%3C/returnfield%3E%3Creturnfield%3Ecreator/organizationName%3C/returnfield%3E%3Creturnfield%3EdataTable/physical/distribution/online/url%3C/returnfield%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3EPISCO:%3C/value%3E%3Cpathexpr%3Etitle%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3Episco%3C/value%3E%3Cpathexpr%3E/eml/@packageId%3C/pathexpr%3E%3C/queryterm%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EIntertidal%20Recruitment%20Data%3C/value%3E%3Cpathexpr%3EkeywordSet/keyword%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EPISCO%20Categories%3C/value%3E%3Cpathexpr%3EkeywordSet/keywordThesaurus%3C/pathexpr%3E%3C/queryterm%3E%3C/querygroup%3E%3C/querygroup%3E%3C/pathquery%3E">
        Intertidal Recruitment Data
        </a>
        <br />
            <a
            href="<%=SERVLET_URL%>?action=squery&amp;qformat=pisco&amp;query=%3C?xml%20version=%221.0%22?%3E%3Cpathquery%20version=%221.2%22%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.1.1%3C/returndoctype%3Eeml://ecoinformatics.org/eml-2.1.0%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.1%3C/returndoctype%3E%3Creturndoctype%3Eeml://ecoinformatics.org/eml-2.0.0%3C/returndoctype%3E%3Creturnfield%3Edataset/title%3C/returnfield%3E%3Creturnfield%3EdataTable/entityName%3C/returnfield%3E%3Creturnfield%3Ecreator/individualName/surName%3C/returnfield%3E%3Creturnfield%3Ecreator/organizationName%3C/returnfield%3E%3Creturnfield%3EdataTable/physical/distribution/online/url%3C/returnfield%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3EPISCO:%3C/value%3E%3Cpathexpr%3Etitle%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22false%22%20searchmode=%22starts-with%22%3E%3Cvalue%3Episco%3C/value%3E%3Cpathexpr%3E/eml/@packageId%3C/pathexpr%3E%3C/queryterm%3E%3Cquerygroup%20operator=%22INTERSECT%22%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3ESubtidal%20Recruitment%20Data%3C/value%3E%3Cpathexpr%3EkeywordSet/keyword%3C/pathexpr%3E%3C/queryterm%3E%3Cqueryterm%20casesensitive=%22true%22%20searchmode=%22equals%22%3E%3Cvalue%3EPISCO%20Categories%3C/value%3E%3Cpathexpr%3EkeywordSet/keywordThesaurus%3C/pathexpr%3E%3C/queryterm%3E%3C/querygroup%3E%3C/querygroup%3E%3C/pathquery%3E">
        Subtidal Recruitment Data
        </a>
          </td>
        </tr>
     </table>
    <!-- end search form area -->
    </div>
    <!-- end content form area -->
  </body>
</html>
