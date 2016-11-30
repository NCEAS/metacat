<?xml version="1.0"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:gmd="http://www.isotc211.org/2005/gmd"
  xmlns:gco="http://www.isotc211.org/2005/gco" 
  version="1.0">

  <xsl:template match="gmd:MD_Identifier">
    <xsl:apply-templates select="./gmd:code" />
  </xsl:template>

  <xsl:template match="gmd:MD_Keywords">
    <div><strong>Type:</strong>&#xa0;<xsl:value-of select="./gmd:type/gmd:MD_KeywordTypeCode/text()" /></div>
    <div><strong>Thesaurus:&#xa0;</strong><xsl:value-of select="./gmd:thesaurusName/gmd:CI_Citation/gmd:title/gco:CharacterString/text()" /></div>

    <ul>
      <xsl:for-each select="./gmd:keyword">
        <li><xsl:apply-templates select="." /></li>
      </xsl:for-each>
    </ul>
  </xsl:template>
</xsl:stylesheet>