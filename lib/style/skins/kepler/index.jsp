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

<%@ include file="../../common/common-settings.jsp"%>
<%@ include file="../../common/configure-check.jsp"%>
  
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>Kepler Analytical Repository</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/kepler/kepler.css"></link>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_SKINS_URL%>/kepler/kepler.js"></script>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_COMMON_URL%>/branding.js"></script>
</head>
<body>
      <script language="JavaScript">
          insertTemplateOpening("<%=CONTEXT_URL%>");
      </script>
      <br/>
      <script language="JavaScript">
          insertSearchBox("<%=CONTEXT_URL%>");
      </script>
<br/>

<table width="760" border="0" cellspacing="0" cellpadding="0">
  <tr><td colspan="5">

Welcome to the Kepler Analytical Repository.  This repository hosts Kepler
analytical components (actors) on the Ecogrid.  You can search for actors,
then download and import them into Kepler.  Eventually, you will be able
to search this repository directly from Kepler.

<p>&nbsp;</p>

  <p>&nbsp;</p>
<!--
  <li>Login
  <br />
  <menu>
  <form name="loginform" method="post" action="<%=SERVLET_URL%>"
  target="_top" onsubmit="return submitform(this);" id="loginform">
    <input type="hidden" name="action" value="login"> <input type=
    "hidden" name="username" value=""> <input type="hidden" name=
    "qformat" value="kepler"> <input type="hidden" name=
    "enableediting" value="false">

    <table>
      <tr valign="middle">
        <td align="left" valign="middle" class="text_plain">
        username:</td>

        <td width="173" align="left" class="text_plain" style=
        "padding-top: 2px; padding-bottom: 2px;"><input name="uid"
        type="text" style="width: 140px;" value="kepler"></td>
      </tr>

      <tr valign="middle">
        <td height="28" align="left" valign="middle" class=
        "text_plain">organization:</td>

        <td align="left" class="text_plain" style=
        "padding-top: 2px; padding-bottom: 2px;"><select name=
        "organization" style="width:140px;">
          <option value="unaffiliated">unaffiliated</option>
        </select></td>
      </tr>

      <tr valign="middle">
        <td width="85" align="left" valign="middle" class=
        "text_plain">password:</td>

        <td colspan="2" align="left" class="text_plain" style=
        "padding-top: 2px; padding-bottom: 2px;">
          <table width="100%" border="0" cellpadding="0"
          cellspacing="0">
            <tr>
              <td width="150" align="left"><input name="password"
              type="password" maxlength="50" style="width:140px;"
              value="kepler"></td>

              <td align="center" class="buttonBG_login">
              <input type="submit" name="loginAction" value="Login"
              class="button_login"></td>

              <td align="left">&nbsp;</td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </form>
  </menu>
  </li>
-->
</menu>
</p>
  </td></tr>
</table>
<p>&nbsp;</p>
<script language="JavaScript">          
    insertTemplateClosing("<%=CONTEXT_URL%>");
</script>
</body>
</html>
