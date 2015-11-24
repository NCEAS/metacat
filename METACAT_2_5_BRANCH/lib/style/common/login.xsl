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
              href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />
        <script language="Javascript" type="text/JavaScript"
                src="{$contextURL}/style/skins/{$qformat}/{$qformat}.js" />
        <script language="Javascript" type="text/JavaScript"
                src="{$contextURL}/style/common/branding.js" />
      </head>

      <script language="JavaScript">
      	<![CDATA[
          insertTemplateOpening("]]><xsl:value-of select="$contextURL" /><![CDATA[");
          insertSearchBox("]]><xsl:value-of select="$contextURL" /><![CDATA[");
        ]]>
      </script>

      <xsl:if test="count(login) &gt; 0">
 <script language="JavaScript">
           <![CDATA[
		function search(url){
		location.href = url;
        }]]>
        </script>

        <body>
        <xsl:attribute
          name="onload">javascript:timerId=setTimeout(search(
          '<xsl:value-of select="$cgi-prefix" />/register-dataset.cgi?cfg=<xsl:value-of select="$qformat" />'), 
            2000)
        </xsl:attribute>
          <p class="text_plain">Welcome <xsl:value-of select="login/name"/>. You will be automatically redirected to the data registry.</p>
        </body>

      </xsl:if>
      <xsl:if test="count(unauth_login) &gt; 0">
      	<body>
          <p class="text_plain"><xsl:value-of select="unauth_login/message" /></p>
    	</body>
      </xsl:if>
      <p class="text_plain">Return to the <a href="{$contextURL}/style/skins/{$qformat}" target="_top">homepage</a>.
      </p>
      <script language="JavaScript">
          <![CDATA[
          insertTemplateClosing("]]><xsl:value-of select="$contextURL" /><![CDATA[");
         ]]>
      </script>
    </html>
    </xsl:template>

</xsl:stylesheet>
