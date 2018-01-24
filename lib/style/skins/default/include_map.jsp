<%@ page     language="java" %>
<!--
/**
  *  '$RCSfile$'
  *      Authors:     Matthew Perry
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
<!-- *********************** START Map ************************* -->
<html>
<head>
  <title>Metacat Data Catalog Map</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <link href="<%=STYLE_SKINS_URL%>/default/default.css" rel="stylesheet" type="text/css">

  <script language="javascript" 
    type="text/javascript" src="<%=STYLE_SKINS_URL%>/default/default.js">
  </script>
</head>
<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
  <table width="750px" align="center" cellspacing="0" cellpadding="0" class="group group_border" >
    <tr> 
      <th class="sectionheader">
        Data Catalog Map
      </th>
    </tr>
    <tr>       
      <td>
        <table width="100%" cellspacing="0" cellpadding="0" border="0" class="subpanel">
          <tr><td> 
           <iframe scrolling="no" frameborder="0" width="780" height="520" src="<%=STYLE_COMMON_URL%>/spatial/map.jsp"> You need iframe support </iframe>
          </td></tr>
         </table>
      </td>
    </tr>
  </table>
</body>
</html>


