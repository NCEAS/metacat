<%@ page language="java"%>

<!--
/**
  *  '$RCSfile$'
  *      Authors:     Duane Costa
  *      Copyright:   2005 University of New Mexico and
  *                   Regents of the University of California and the
  *                   National Center for Ecological Analysis and Synthesis
  *      For Details: http://www.nceas.ucsb.edu/
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
  */
-->

<%@ include file="settings.jsp"%>
<%@ include file="session_vars.jsp"%>

<html>

  <head>
    <link href="<%=STYLE_SKINS_URL%>/lter/lter.css" rel="stylesheet" type="text/css">
    <script language="javascript" type="text/javascript" src="<%=STYLE_SKINS_URL%>/lter/lter.js"></script>
    <title>Welcome to the LTER Data Catalog</title>
    <%
      Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
      String loggedInMessage = "";
      String username = (String) session.getAttribute("username");

      if ((loggedIn == null) || (loggedIn.booleanValue() == false)) {   
        loggedInMessage = "<p>" +
                          "You may search the data catalog without being " +
                          "<a href=\"index_login.jsp\" target=\"_top\">logged in</a> to your account, " +
                          "but you will have access only to <em>public</em> data." +
                          "</p>";
      }
      else {
        loggedInMessage = "<p class=\"emphasis\">" +
                          "Welcome, " + username + ", you are now logged in." +
                          "</p>";
      }
    %>
    <script type="text/javascript" language="Javascript1.1">

      function submitRequest(form) {
        var canSearch = true;

        if (form.searchValue.value == "") {              
          canSearch = 
             confirm("Show *all* data in the catalog?\n(This may take some time!)");
        }
        
        return canSearch;
      }

    </script>
  </head>
  
  <body>
    <table class="bdyMiddleTable" border="0" cellpadding="10" cellspacing="0">
      <tr>
        <td class="bdyMiddleTd" colspan='2'>
          <h1 align='center'>Welcome to the LTER Data Catalog</h1>                        
            Data are one of the most valuable products of the LTER
            program.  The LTER Network seeks to inform the LTER and
            broader scientific community by creating well designed
            and well documented databases and to provide fast,
            effective, and open access to LTER data via a network-wide
            information system designed to facilitate data exchange
            and integration. Currently, the LTER Data Catalog contains
            entries for over 3000 ecological datasets from 26 LTER
            Network research sites, in addition to numerous other
            ecological field stations and research instituitions.   
        </td>
      </tr>
      <tr>
        <td class="bdyMiddleTd" valign='top'>
          <h3 align='center'>Data Catalog</h3>
            <%= loggedInMessage %>
            <form name="searchForm" 
                  action="metacatsearchforward.jsp" 
                  method="POST"
                  onsubmit="return submitRequest(this)"
                  target="_top">                
              <table>
                <tr>
                  <td>&nbsp;</td>
                </tr>
                <tr>
                  <td>Search Term:</td>
                  <td><input type="text" name="searchValue"/></td>
                  <td>
                    <a target="_top" href="<%=STYLE_SKINS_URL%>/lter/index_advancedsearch.jsp">Advanced Search
                    </a>
                  </td>
                </tr>
                <!-- Test how a document looks when displayed in an iframe with fixed height.
                <tr>
                  <td><a target="_top" href="<%=STYLE_SKINS_URL%>/lter/index_document.jsp?docid=knb-lter-kbs.2486">Test Document</a></td>
                </tr>
                -->
                <tr>
                  <td>&nbsp;</td>
                  <td>
                    <input type="submit" value="Search"/>&nbsp;&nbsp;<input type="reset" />
                  </td>
                </tr>
              </table>
            </form>
        </td>
        <td class="bdyMiddleTd" valign='top'>
          <h3 align='center'>LTER Data Policies</h3>
            The <a href='http://www.lternet.edu/data/netpolicy.html'>
            LTER data policy</a> includes three specific sections
            designed to express shared network policies regarding
            the release of LTER data products, user registration for
            accessing data, and the licensing agreements specifying
            the conditions for data use.
     
          <h3 align='center'>Other Databases</h3>
            Additional information is available through these value-added data products:
            <ul>
              <li><a target="_top" href='http://www.fsl.orst.edu/climhy/'>LTER/USFS Climate / Hydrology Data</a>
              <li><a target="_top" href='http://intranet.lternet.edu/cgi-bin/anpp.pl'>Annual Net Primary Productivity Data</a>
            </ul>
        </td>
      </tr>
    </table>
  </body>
  
</html>
