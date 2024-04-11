<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" />
  <html>
  <body>
  <xsl:template match="/">
    <h3><u>
      <xsl:value-of select="./param[@name='firstname']"/>
      <xsl:text>&nbsp;</xsl:text>
      <xsl:value-of select="./param[@name='lastname']"/>
    </u></h3>
    <ul>
      <li>
        <xsl:value-of select="./param[@name='email']"/>
      </li>
      <li>
        <xsl:value-of select="./param[@name='acceptlic']"/>
      </li>
      <li>
        <xsl:value-of select="./param[@name='product']"/>
      </li>
      <li>
        <xsl:value-of select="./param[@name='opersys']"/>
      </li>
      <li>
        <b><xsl:value-of select="./param[@name='emailnotice']"/></b>
      </li>
      <li>
        <xsl:value-of select="/download/date"/>
      </li>
    </ul>
  </xsl:tempate>
  </body>
  </html>
</xsl:stylesheet>
