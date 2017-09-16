<?xml version="1.0"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:gml="http://www.opengis.net/gml/3.2" version="1.0">

    <xsl:template match="gml:Point">
        <!-- TODO -->
    </xsl:template>

    <!-- 
    gml:Polygon
    This is a very incomplete template for showing gml:Polygons 
    -->
    <xsl:template match="gml:Polygon">
        <xsl:variable name="coordinates">
            <xsl:value-of select="./gml:exterior/gml:LinearRing/gml:coordinates/@decimal" />
        </xsl:variable>

        <xsl:element name="div">
            <xsl:attribute name="class">control-group coordinates</xsl:attribute>
            <xsl:attribute name="data-content">coordinates</xsl:attribute>
            <xsl:attribute name="data-value"><xsl:value-of select="$coordinates" /></xsl:attribute>

            <label class="control-label">Coordinates</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:choose>
                        <xsl:when test="$coordinates and normalize-space($coordinates)!=''">
                            <xsl:value-of select="$coordinates" />                    
                        </xsl:when>
                        <xsl:otherwise>
                            Polygon display not supported by this display. Please see full metadata record for polygon information.
                        </xsl:otherwise>
                    </xsl:choose>
                </div>
            </div>
        </xsl:element>
    </xsl:template>

    <xsl:template match="gml:interior">
        <!-- TODO -->
    </xsl:template>

    <xsl:template match="gml:LinearRing">
        <!-- TODO -->
    </xsl:template>

    <xsl:template match="gml:LineString">
        <!-- TODO -->
    </xsl:template>
</xsl:stylesheet>
