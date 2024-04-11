<?xml version="1.0"?>
<!--
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file with information about login action
  * into an HTML format suitable for rendering with modern web browsers.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>
  <xsl:param name="qformat">metacatui</xsl:param>
  <xsl:param name="contextURL"/>
  <xsl:param name="cgi-prefix"/>
  <xsl:template match="/">
   <html>
      <head>
         <script language="JavaScript">
         <![CDATA[
            function redirect(){
                location.href = "]]><xsl:value-of select="$cgi-prefix" /><![CDATA[/register-dataset.cgi?cfg=metacatui";
            }]]>
         </script>
      </head>
      <!-- Immediately redirect to avoid having to style this perfunctory page -->
      <body onload="redirect()">
      <!-- 
      <xsl:if test="count(login) &gt; 0">
          <p>
            Welcome <xsl:value-of select="login/name"/>. You will be automatically redirected to the data repository form.
          </p>
      </xsl:if>
      <xsl:if test="count(unauth_login) &gt; 0">
          <p class="text_plain">
            <xsl:value-of select="unauth_login/message" />
          </p>
      </xsl:if>
          <p>
            Return to the <a href="{$contextURL}/style/skins/metacatui/">Registry home</a>.
          </p>
       -->
      </body>
    </html>
    </xsl:template>

</xsl:stylesheet>
