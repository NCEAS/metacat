<%@ page    language="java" %>
<%
/**
 * '$RCSfile$'
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 * '$Author$'
 * '$Date$'
 * '$Revision$'

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

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

<html> 
<head>
<title>Metacat Harvester Registration Login</title>
</head>

<body bgcolor="white" text="black" link="blue" vlink="purple" alink="red">
<br><center>
<h3>
<font color="#132B76" face=verdana size=2%>
<b>Metacat Harvester Registration Login</b>
</font>
</h3>

<form name="myform" 
      method="POST" 
      action="<%=SERVER_URL_WITH_CONTEXT%>/harvesterRegistrationLogin">

<table width=400 align=center cellspacing=1 cellpadding=1 
       border=0 bgcolor="#333366">
  <tr>
    <td>
      <table bgcolor="#ffffff" border="0" cellpadding="1" width='100%' >
        <tr > <td colspan=3 align=center >&nbsp;</td> </tr>
        <tr > 
          <td colspan=3 align=center >
            <font face=verdana size=1%>
              <b>Please  Enter Username, Organization, and Password</b>
            </font>
          </td> 
        </tr>
        <tr>
          <td width='10%'> &nbsp;</td>
          <td width="25%" bgcolor="#4682b4">
            <p align="center">
            <font color="white" face=verdana size=2%>
            <b>Username</b>
            </font>
          </td>
          <td><p><input type="text" name="uid" maxlength="100" size="28"></td>
        </tr>
        <tr>
          <td width='10%'> &nbsp;</td>
          <td width="25%" bgcolor="#4682b4">
            <p align="center">
            <font color="white" face=verdana size=2%>
            <b>Organization</b>
            </font>
          </td>
          <td>
            <input type="radio" name="o" value="NCEAS">NCEAS
            <input type="radio" name="o" value="LTER">LTER
            <input type="radio" name="o" value="NRS">NRS
            <br>
            <input type="radio" name="o" value="PISCO">PISCO
            <input type="radio" name="o" value="OBFS">OBFS
            <input type="radio" name="o" value="Unaffiliated">Unaffiliated
        </tr>
        <tr>
          <td width='10%'> &nbsp;</td>
          <td bgcolor="#4682b4">
            <p align="center">
            <font color="white" face=verdana size=2%>
            <b>Password</b>
            </font>
          </td>
          <td><p><input type="password" name="passwd" maxlength="60" size="28">
          </td>
        </tr>
        <tr> <td colspan=3 align=center >&nbsp;</td></tr>
      </table>
    </td>
  </tr>
</table>
<br>
<p>
<input type="submit" value="Login">
<input type="reset" value="Reset">
</form>
</body>
