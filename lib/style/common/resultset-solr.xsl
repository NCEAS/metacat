<?xml version="1.0"?>
<!--
* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
* convert an XML file showing the resultset of a query
* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:import href="./resultset-table-solr.xsl"/>
  
  <xsl:output method="html"/>
  <xsl:param name="sessid"/>
  <xsl:param name="qformat">default</xsl:param>
  <xsl:param name="enableediting">false</xsl:param>
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
        <script language="JavaScript">
          <![CDATA[
          function submitform(action,form_ref) {
              form_ref.action.value=action;
              form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
              form_ref.qformat.value="]]><xsl:value-of select="$qformat" /><![CDATA[";
              form_ref.submit();
          }

          ]]>
        </script>
      </head>

      <body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
      <script language="JavaScript">
      	<![CDATA[
          insertTemplateOpening("]]><xsl:value-of select="$contextURL" /><![CDATA[");
          insertSearchBox("]]><xsl:value-of select="$contextURL" /><![CDATA[");
         ]]>
      </script>
      <xsl:call-template name="resultstablesolr"/>
      <script language="JavaScript">
      	<![CDATA[
          insertTemplateClosing("]]><xsl:value-of select="$contextURL" /><![CDATA[");
         ]]>
      </script>
    </body>
    </html>
    </xsl:template>

</xsl:stylesheet>
