<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:gmx="http://www.isotc211.org/2005/gmx" 
    xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0">
    <xsl:template match="gmx:Anchor">
        <xsl:element name="a">
            <xsl:attribute name="href">
                <xsl:value-of select="./@xlink:href" />
            </xsl:attribute>
            <!-- Create the attributes for a Bootstrap tooltip to hold the title -->
            <xsl:if test="./@xlink:title">
                <xsl:attribute name="data-toggle">tooltip</xsl:attribute>
                <xsl:attribute name="data-placement">top</xsl:attribute>
                <xsl:attribute name="title">
                    <xsl:value-of select="./@xlink:title" />
                </xsl:attribute>
            </xsl:if>
            <xsl:value-of select="./text()" />
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
