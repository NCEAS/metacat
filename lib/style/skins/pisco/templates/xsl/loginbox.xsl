<?xml version="1.0"?>
<!--
*  '$RCSfile$'
*      Authors: Chris Jones
*    Copyright: 2000 Regents of the University of California and the
*         National Center for Ecological Analysis and Synthesis
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
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html" encoding="iso-8859-1" indent="yes" standalone="yes"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" />
    
  <xsl:template name="loginbox">
    <xsl:comment>begin the login form area</xsl:comment>
      <!-- begin login form area -->
      <div id="loginbox">
        <!--
        <form name="loginForm" method="POST" 
          action="{$contextURL}/style/skins/{$defaultStyle}/index.jsp">
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
          <input type="hidden" name="qformat" value="{$defaultStyle}">
          <input type="hidden" name="enableediting" value="true">
        </form> 
        -->
      </div>
    <xsl:comment>end the login form area</xsl:comment>
    <!-- end login form area -->
  </xsl:template>

</xsl:stylesheet>
