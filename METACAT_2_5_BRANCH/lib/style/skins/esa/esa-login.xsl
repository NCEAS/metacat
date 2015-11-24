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
  <xsl:template match="/">
   <html>
      <head>
        <link rel="stylesheet" type="text/css"
              href="./style/skins/esa/esa.css" />
        <script language="Javascript" type="text/JavaScript"
                src="./style/skins/esa/esa.js" />
        <script language="Javascript" type="text/JavaScript"
                src="./style/common/branding.js" />
      </head>

     <script language="JavaScript">
       <![CDATA[
          insertTemplateOpening("]]><xsl:value-of select="$contextURL" /><![CDATA[");
          insertSearchBox("]]><xsl:value-of select="$contextURL" /><![CDATA[");
         ]]>
     </script>
      <xsl:if test="count(login) &gt; 0">
      <xsl:choose>
      <xsl:when test="count(login/isModerator) &gt; 0">
           <script language="JavaScript">
           <![CDATA[
	   function search(){
		var searchForm = document.getElementById("search-form");
		var queryInput = document.createElement("input");
        	queryInput.setAttribute("type", "hidden");
        	queryInput.setAttribute("name", "query");
        	queryInput.setAttribute("value", "<pathquery version=\"1.2\"><querytitle>Moderator-Search</querytitle><returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype><returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype><returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype><returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype><returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype><returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN</returndoctype><returndoctype>-//NCEAS//resource//EN</returndoctype><returndoctype>-//NCEAS//eml-dataset//EN</returndoctype><returnfield>originator/individualName/surName</returnfield><returnfield>originator/individualName/givenName</returnfield><returnfield>creator/individualName/surName</returnfield><returnfield>creator/individualName/givenName</returnfield><returnfield>originator/organizationName</returnfield><returnfield>creator/organizationName</returnfield><returnfield>dataset/title</returnfield><returnfield>keyword</returnfield><querygroup operator=\"INTERSECT\"><queryterm searchmode=\"not-contains\" casesensitive=\"false\"><value>public</value><pathexpr>access/allow/principal</pathexpr></queryterm><queryterm searchmode=\"not-contains\" casesensitive=\"false\"><value>Revision Requested</value><pathexpr>additionalMetadata/moderatorComment</pathexpr></queryterm></querygroup></pathquery>");
		searchForm.appendChild(queryInput);	
		searchForm.submit();
    };
    ]]>
        </script>
      	<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="timerID=setTimeout('search()',20)">
          <p class="text_plain">Welcome <xsl:value-of select="login/name"/>. You are now logged in as moderator.</p>
	  <form id="search-form" action="./metacat" method="post">
		<input type="hidden" name="action" value="squery"/>
		<input type="hidden" name="qformat" value="esa"/>
		<input type="hidden" name="enableediting" value="true"/>
		<input type="hidden" name="message" value="You are now logged in as moderator."/>
    <input type="submit" name="submit query"/>
	</form>
    	</body>
      </xsl:when>
      <xsl:otherwise>
 <script language="JavaScript">
           <![CDATA[
		function search(){
		location.href = "./style/skins/esa";
        }]]>
        </script>
        <body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="timerID=setTimeout('search()',2000)">
          <p class="text_plain">Welcome <xsl:value-of select="login/name"/>. You will be automatically redirected to the homepage</p>
        </body>

      </xsl:otherwise>
      </xsl:choose>
      </xsl:if>
      <xsl:if test="count(unauth_login) &gt; 0">
      	<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
          <p class="text_plain"><xsl:value-of select="unauth_login/message" /></p>
    	</body>
      </xsl:if>
      <p class="text_plain">Return to the <a href="./style/skins/esa/" target="_top">Registry home</a>.
      </p>
      <script language="JavaScript">
       <![CDATA[
          insertTemplateClosing("]]<xsl:value-of select="$contextURL" /><![CDATA[");
          ]]>
      </script>

    </html>
    </xsl:template>

</xsl:stylesheet>

