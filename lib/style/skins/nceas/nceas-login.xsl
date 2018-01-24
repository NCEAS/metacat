<?xml version="1.0"?>
<!--
  *  '$RCSfile$'
  *      Authors: Jivka Bojilova
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
  * convert an XML file with information about login action
  * into an HTML format suitable for rendering with modern web browsers.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>
  <xsl:param name="qformat">default</xsl:param>
  <xsl:param name="contextURL"/>
  <xsl:param name="cgi-prefix"/>
  <xsl:template match="/">
   <html>
      <head>
        <link rel="stylesheet" type="text/css"
              href="{$contextURL}/style/skins/nceas/nceas.css" />
        <script language="Javascript" type="text/JavaScript"
                src="{$contextURL}/style/skins/nceas/nceas.js" />
        <script language="Javascript" type="text/JavaScript"
                src="{$contextURL}/style/common/branding.js" />
         <script language="JavaScript">
         <![CDATA[
            function search(){
                location.href = "]]><xsl:value-of select="$cgi-prefix" /><![CDATA[/register-dataset.cgi?cfg=nceas";
            }]]>
         </script>
      </head>
      <body onload="timerId=setTimeout('search()',2000)">
      <div id="main_wrapper">

      <div id="header">
          <p>Skip to <a href="#navigation">navigation</a>, <a href="#main_content">main
          content</a>, <a href="#secondary_content">secondary content</a> or to <a
          href="#search">search</a>.</p> <h1><span></span><a href="/">NCEAS</a></h1>
      </div>
      <div id="navigation">
          <p></p>
          <ul id="main_nav">
              <ul class="menu">
                  <li class="collapsed"><a href="http://www.nceas.ucsb.edu">Home</a></li>
                  <li class="collapsed"><a href="http://data.nceas.ucsb.edu">Repository</a></li>
                  <li class="collapsed"><a href="{$cgi-prefix}/register-dataset.cgi?cfg=nceas">Register</a></li>
            </ul>
        </ul>
    </div> <!-- navigation -->

    <div id="content_wrapper">
        <h1>NCEAS Data Repository</h1>

      <xsl:if test="count(login) &gt; 0">
          <p class="text_plain">
            Welcome <xsl:value-of select="login/name"/>. You will be automatically redirected to the data repository form.
          </p>
      </xsl:if>
      <xsl:if test="count(unauth_login) &gt; 0">
          <p class="text_plain">
            <xsl:value-of select="unauth_login/message" />
          </p>
      </xsl:if>
          <p class="text_plain">
            Return to the <a href="{$contextURL}/style/skins/nceas/">Registry home</a>.
          </p>
        </div>
        </div>
      </body>
    </html>
    </xsl:template>

</xsl:stylesheet>
