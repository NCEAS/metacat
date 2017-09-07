<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:gco="http://www.isotc211.org/2005/gco" version="1.0">
    <!-- TODO: gco:Date-->
    <!-- TODO: gco:DateTime-->
    <xsl:template match="gco:Date">
        <xsl:value-of select="." />
    </xsl:template>
    <xsl:template match="gco:DateTime">
        <xsl:value-of select="." />
    </xsl:template>
</xsl:stylesheet>
