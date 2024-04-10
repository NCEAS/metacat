<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text"/>
  <xsl:template match="/">
         <xsl:for-each select="resultset/document">
           <xsl:sort select="./param[@name='dataset/creator/organizationName']"/>
           <xsl:value-of select="./docid"/>,<xsl:value-of select="./param[@name='creator/organizationName' and starts-with(text(),'NCEAS ')]"/><xsl:text>
</xsl:text>
         </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
