<?xml version="1.0"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:gmd="http://www.isotc211.org/2005/gmd"
        version="1.0">

    <xsl:template match="gmd:URL">
        <xsl:variable name="url"><xsl:value-of select="./text()" /></xsl:variable>
        <xsl:element name="a">
            <xsl:attribute name="href">
                <xsl:value-of select="$url" />
            </xsl:attribute>
            <xsl:value-of select="$url" />
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>