<%@ page language="java"%>

<% 
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
%>
 
<%@ include file="settings.jsp"%>
<%@ include file="session_vars.jsp"%>

<%
      Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
      String loginHref = "index_login.jsp";
      String loginText = "Login";

      if ((loggedIn != null) && (loggedIn.booleanValue() == true)) {
        loginHref = "metacatlogout.jsp";
        loginText = "Logout";
      }
%>

<html>
  <head>
    <title>
      leftcol.jsp
    </title>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <link href="<%=STYLE_SKINS_URL%>/lter/lter.css" rel="stylesheet" type="text/css">
  </head>

  <body class="sidenav">
    <p class="sidelarge">
      <a href="http://www.lternet.edu" target="_top">
        Home
      </a>
      <br />
      <a href="<%=loginHref%>" target="_top">
        <%=loginText%>
      </a>
      <br />
      <a href="<%=STYLE_SKINS_URL%>/lter/index.jsp" target="_top">
        Search
      </a>
      <br />
      <a href="<%=STYLE_SKINS_URL%>/lter/index_advancedbrowse.jsp" target="_top">
        Browse
      </a>
      <br />
    </p>
  </body>

</html>
