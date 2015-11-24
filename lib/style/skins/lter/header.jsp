<%@ page language="java"%>
<%
/**
 *  '$RCSfile$'
 *      Authors: Duane Costa
 *    Copyright: 2006 University of New Mexico and
 *               Regents of the University of California and the
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
 */
%>
<%@ include file="settings.jsp"%>
<%@ include file="session_vars.jsp"%>

<html>

  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
    <link href="<%=STYLE_SKINS_URL%>/lter/lter.css" rel="stylesheet" type="text/css">
    <script language="javascript" type="text/javascript" src="<%=STYLE_SKINS_URL%>/lter/lter.js"></script>
    <title>
      LTER Data Catalog
    </title>
  </head>

  <body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
    <table width="912px" height="100%" align="left" border="0" cellpadding="0" cellspacing="0">
      <tr>
        <td colspan="3" valign="top">
          <table width="100%" border="0" cellpadding="0" cellspacing="0">
            <tr>
              <td width="126" align="right" class="sidenav">
                &nbsp;
              </td>
              <td width="660" align="right" nowrap class="topnav">
                <a href="http://www.lternet.edu" target="_top">
                  LTER Home
                </a>
                |
                <a href="http://intranet.lternet.edu" target="_top">
                  Intranet
                </a>
                &nbsp;
              </td>
              <td width="126" class="sidenav">
                &nbsp;
              </td>

            </tr>
          </table>
        </td>
      </tr>
      <tr>
        <td height="100%" width="126" bgcolor="#006699" align="center">
          <img src="images/lterleft.jpg" width="126" height="92" border="0" usemap="#Map">
        </td>
        <td height="100%" width="660" bgcolor="#006699" align="center">
          <img src="images/ltermain.jpg" width="550" height="92">

        </td>
        <td height="100%" width="126" bgcolor="#006699">
          <img src="images/lterright.jpg" width="126" height="92">
        </td>
      </tr>
      <tr>
        <td colspan="3" class="sidenav"></td>
      </tr>
    </table>
  </body>

</html>
