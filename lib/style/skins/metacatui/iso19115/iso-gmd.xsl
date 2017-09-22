<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:gmd="http://www.isotc211.org/2005/gmd"
    xmlns:gco="http://www.isotc211.org/2005/gco"
    xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0">
    <xsl:template match="gmd:URL">
        <xsl:variable name="url">
            <xsl:value-of select="./text()" />
        </xsl:variable>
        <xsl:element name="a">
            <xsl:attribute name="href">
                <xsl:value-of select="$url" />
            </xsl:attribute>
            <xsl:value-of select="$url" />
        </xsl:element>
    </xsl:template>
    <xsl:template match="gmd:date">
        <xsl:apply-templates />
    </xsl:template>
    <xsl:template match="gmd:dateStamp">
        <xsl:apply-templates />
    </xsl:template>
    <xsl:template match="gmd:dateType">
        <xsl:apply-templates />
    </xsl:template>
    <xsl:template match="gmd:resourceConstraints">
        <!-- Show the title if it's present -->
        <xsl:if test="../@xlink:title">
            <div class="control-group">
                <label class="control-label">Title</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="../@xlink:title" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:apply-templates />
    </xsl:template>
    <xsl:template match="gmd:distributionInfo">
        <xsl:apply-templates />
    </xsl:template>
    <!-- BDM: This is a short template for the thesaurus rather than a full 
    implementation. I did this for space reasons in the view. At some later 
    point we could consider showing the full thesaurus citation some other way.
    -->
    <xsl:template match="gmd:thesaurusName">
        <xsl:value-of select="./gmd:CI_Citation/gmd:title" />
    </xsl:template>

    <!-- gmd:code -->
    <xsl:template match="gmd:code">
        <xsl:variable name="code">
            <xsl:value-of select="./gco:CharacterString/text()" />
        </xsl:variable>
   
        <xsl:choose>
            <xsl:when test="starts-with($code, 'http://')">
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$code" />
                    </xsl:attribute>
                    <xsl:value-of select="$code" />
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$code" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- gmd:verticalCRS -->
    <xsl:template match="gmd:verticalCRS">
        <xsl:choose>
            <xsl:when test="./@xlink:href and ./@xlink:title">
                <xsl:element name="a">
                    <xsl:attribute name="href"><xsl:value-of select="./@xlink:href" /></xsl:attribute>
                    <xsl:value-of select="./@xlink:title" />
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>See metadata record for full CRS information.</xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
