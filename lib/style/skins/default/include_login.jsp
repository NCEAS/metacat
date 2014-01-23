<%@ page     language="java"  import="java.util.Vector,edu.ucsb.nceas.metacat.util.OrganizationUtil"%>
<!--
/**
  *  '$RCSfile$'
  *      Authors: Matt Jones, CHad Berkley
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
  */
-->

<%@ include file="settings.jsp"%>
<%@ include file="session_vars.jsp"%>

<% 
	Vector<String> organizationList = OrganizationUtil.getOrganizations();
%>

<!-- *********************** START LOGIN TABLE ************************* -->
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <link href="<%=STYLE_SKINS_URL%>/default/default.css" rel="stylesheet" type="text/css">
  <script language="javascript" 
    type="text/javascript" src="<%=STYLE_SKINS_URL%>/default/default.js">
  </script>
  <script language="javascript" type="text/javascript">
    var popupMsg = "If you need to create a new account, \n"
                   +"click the \"create new account\" link";
    function trim(stringToTrim) {
      return stringToTrim.replace(/^\s*/, '').replace(/\s*$/,'');
    }
    function allowSubmit(formObj) {
      if (trim(formObj.elements["loginAction"].value)!="Login") return true;
      //trim username & passwd:
      var username = trim(formObj.elements["username"].value);
      var organization = trim(formObj.elements["organization"].value);
      var password = trim(formObj.elements["password"].value);
      if (username=="") {
        alert("You must type a username. \n"+popupMsg);
                formObj.elements["username"].focus();
        return false;
      } 
      if (organization=="") {
        alert("You must select an organization.\n"+popupMsg); 
                formObj.elements["organization"].focus();
        return false;
      } 
      if (password=="") {
        alert("You must type a password. \n"+popupMsg);
              formObj.elements["password"].focus();
        return false;
      }
      return true;
    } 
    <%=(isLoggedIn)?
        "   document.cookie = \"JSESSIONID=" + sess_sessionId + ";"
        +"                  path="          + CONTEXT_NAME  +  "\";\n"
        :"  document.cookie = \"JSESSIONID=" + sess_sessionId + ";"
        +"                  path="          + CONTEXT_NAME  +  ";"
        +"                  expires=Thu, 01-Jan-70 00:00:01 GMT\";\n"
    %>
  </script>
</head>

<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
  <table width="750px" align="center" border="0" cellspacing="0" cellpadding="0" class="group group_border">
    <tr> 
      <th class="sectionheader">login &amp; registration</th>
      </td>
    </tr>
    <tr>
      <td colspan="3">
        <table width="100%" class="subpanel" border="0" cellpadding="0" cellspacing="0">
          <tr>
            <td width="10">
              <img src="<%=STYLE_SKINS_URL%>/default/images/transparent1x1.gif" width="10" height="10">
				<a name="loginanchor"></a>
            </td>
            <td class="text_example">
                <p>
                  Logging into your account enables you to search any 
                  additional, non-public data for which you may have access 
                  privileges:
                </p>
                <%= loginStatus %>
            </td>
            <td width="10">
              <img src="<%=STYLE_SKINS_URL%>/default/images/transparent1x1.gif" width="10" height="10">
            </td>
          </tr>
          <tr> 
            <td width="10">
              <img src="<%=STYLE_SKINS_URL%>/default/images/transparent1x1.gif" width="10" height="10">
            </td>
            <td> 
              <form name="loginform" method="post" action="index.jsp" 
                target="_top" onSubmit="return allowSubmit(this);">
                <input type="hidden" name="action"  value="login">
                <input type="hidden" name="ldapusername"  value="">
                <input type="hidden" name="qformat" value="default">
              <table width="100%" border="0" cellpadding="0" cellspacing="0">
                <tr valign="middle"> 
                  <td align="left" valign="middle" class="text_plain_smaller">
                    username:
                  </td>
                  <td  align="left" 
                    style="padding-top: 2px; padding-bottom: 2px;">
                    <input name="username" type="text" style="width: 140px;" 
                      value="<%=typedUserName%>" <%=loginEnabledDisabled%>>
                  </td>
                  <td width="10px">
                    <img src="<%=STYLE_SKINS_URL%>/default/images/transparent1x1.gif" width="10">
                  </td>
                  <td align="left" class="text_plain">
                    <a href="<%=USER_MANAGEMENT_URL%>" target="_top">
                    create a new account</a>
                  </td>
                </tr>
                <tr valign="middle">
                  <td height="28" align="left" 
                    valign="middle" class="text_plain_smaller">
                    organization:
                  </td>
                  <td align="left" 
                    style="padding-top: 2px; padding-bottom: 2px;">
                    <select name="organization" style="width:140px;" 
                      <%=loginEnabledDisabled%> >
                      <option value="" <%=((posted_organization.length()<1)?     "selected":"")%>>&#8212; choose one &#8212;</option>
<%
			for (String orgName : organizationList) {
%>         
                      <option value="<%= orgName %>"   <%=((posted_organization.equalsIgnoreCase(orgName))?     "selected":"")%>><%= orgName %></option>
<%
			}
%>                      
                    </select>
                  </td>
                  <td width="10px">
                    <img src="<%=STYLE_SKINS_URL%>/default/images/transparent1x1.gif" width="10" >
                  </td>
                  <td align="left" class="text_plain">
                    <a href="<%=USER_MANAGEMENT_URL%>" target="_top">forgot your password?</a>
                  </td>
                </tr>
                <tr valign="middle"> 
                  <td width="85" align="left" valign="middle" 
                    class="text_plain_smaller">
                    password:
                  </td>
                  <td> 
                    <input name="password" type="password" maxlength="50" 
                      style="width:140px;" value="<%=posted_password%>" 
                    <%=loginEnabledDisabled%>>
                  </td>
                  <td width="10px">
                    <img src="<%=STYLE_SKINS_URL%>/default/images/transparent1x1.gif" width="10">
                  </td>
                  <td align="left" class="text_plain">
                    <a href="<%=USER_MANAGEMENT_URL%>" target="_top">change your password</a> 
                  </td>
                </tr>
                <tr>
                  <td align="center" colspan="2" class="<%= ((isLoggedIn)? "buttonBG_logout": "buttonBG_login") %>">
                    <input type="submit" name="loginAction" 
                      value="<%=loginButtonLabel%>" class="button_login" />
                  </td>
                  </td>
                  <td width="10">
                    <img src="<%=STYLE_SKINS_URL%>/default/images/transparent1x1.gif" width="10">
                  </td>
                  <td>
                    <img src="<%=STYLE_SKINS_URL%>/default/images/transparent1x1.gif" width="10">
                  </td>
                </tr>
              </table>
              </form>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>    
