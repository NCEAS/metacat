<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:gmd="http://www.isotc211.org/2005/gmd" 
    xmlns:gco="http://www.isotc211.org/2005/gco" 
    xmlns:gml="http://www.opengis.net/gml/3.2" 
    xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0">
    <xsl:template match="gmd:EX_Extent">
        <h4>Coverage</h4>
        <!-- description -->
        <xsl:if test=".//gmd:description">
            <div class="control-group">
                <label class="control-label">Description</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select=".//gmd:description" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <!-- Geographic -->
        <xsl:for-each select=".//gmd:geographicElement">
            <!--  Dynamically add the geographicCoverage class depending on 
            whether this is a bounding box or not because Google Static Maps are
             shown if the class is set but the current MetacatUI codebase only 
             supports rectangular bounding boxes and not geometries like 
             polygons.
             -->

            <xsl:element name="div">
                <xsl:choose>
                    <xsl:when test=".//gmd:EX_GeographicBoundingBox">
                        <xsl:attribute name="class">row-fluid geographicCoverage</xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="class">row-fluid</xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>

                <div class="control-group">
                    <label class="control-label">Geographic</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates />
                        </div>
                    </div>
                </div>
            </xsl:element>
        </xsl:for-each>
        <!-- Temporal -->
        <xsl:for-each select=".//gmd:temporalElement">
            <div class="control-group">
                <label class="control-label">Temporal</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:for-each>
        <!-- Vertical -->
        <xsl:for-each select=".//gmd:verticalElement">
            <div class="control-group">
                <label class="control-label">Vertical</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:for-each>
    </xsl:template>
    <!-- EX_GeographicExtent-->
    <xsl:template match="gmd:EX_GeographicExtent">
        <xsl:apply-templates />
    </xsl:template>
    <!-- EX_BoundingPolygon-->
    <xsl:template match="gmd:EX_BoundingPolygon">
        <xsl:apply-templates />
    </xsl:template>
    <!-- EX_GeographicDescription-->
    <xsl:template match="gmd:EX_GeographicDescription">
        <xsl:apply-templates />
    </xsl:template>
    <!-- EX_GeographicBoundingBox-->
    <xsl:template match="gmd:EX_GeographicBoundingBox">
        <!-- Set up variables for N E S W -->
        <xsl:variable name="north">
            <xsl:value-of select="./gmd:northBoundLatitude/gco:Decimal/text()" />
        </xsl:variable>
        <xsl:variable name="east">
            <xsl:value-of select="./gmd:eastBoundLongitude/gco:Decimal/text()" />
        </xsl:variable>
        <xsl:variable name="south">
            <xsl:value-of select="./gmd:southBoundLatitude/gco:Decimal/text()" />
        </xsl:variable>
        <xsl:variable name="west">
            <xsl:value-of select="./gmd:westBoundLongitude/gco:Decimal/text()" />
        </xsl:variable>
        <div data-value="{$north}" data-content="northBoundingCoordinate" class="control-group northBoundingCoordinate">
            <label class="control-label">North</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:value-of select="$north" />&#xa0; degrees                          
                </div>
            </div>
        </div>
        <div data-value="{$east}" data-content="eastBoundingCoordinate" class="control-group eastBoundingCoordinate">
            <label class="control-label">East</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:value-of select="$east" />&#xa0; degrees                          
                </div>
            </div>
        </div>
        <div data-value="{$south}" data-content="southBoundingCoordinate" class="control-group southBoundingCoordinate">
            <label class="control-label">South</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:value-of select="$south" />&#xa0; degrees                          
                </div>
            </div>
        </div>
        <div data-value="{$west}" data-content="westBoundingCoordinate" class="control-group westBoundingCoordinate">
            <label class="control-label">West</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:value-of select="$west" />&#xa0; degrees                          
                </div>
            </div>
        </div>
        <xsl:apply-templates select="./gmd:extentTypeCode" />
    </xsl:template>
    <!-- gmd:polygon -->
    <xsl:template match="gmd:polygon">
        <div class="control-group">
            <label class="control-label">Polygon</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:apply-templates />
                </div>
            </div>
        </div>
    </xsl:template>
    <!-- EX_TemporalExtent -->
    <xsl:template match="gmd:EX_TemporalExtent">
        <xsl:apply-templates select=".//gmd:extent/*" />
    </xsl:template>
    <!-- EX_VerticalExtent-->
    <xsl:template match="gmd:EX_VerticalExtent">
        <div class="control-group">
            <label class="control-label">Minimum</label>
            <div class="controls">
                <div class="controls-well">
            <xsl:value-of select="./gmd:minimumValue/gco:Real" />
                </div>
            </div>
        </div>

        <div class="control-group">
            <label class="control-label">Maximum</label>
            <div class="controls">
                <div class="controls-well">
            <xsl:value-of select="./gmd:maximumValue/gco:Real" />
                </div>
            </div>
        </div>

        <div class="control-group">
            <label class="control-label">CRS</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:apply-templates select="./gmd:verticalCRS" />
                </div>
            </div>
        </div>
    </xsl:template>

    <!-- TimeInstant-->
    <!-- TODO
        - Attributes: frame, calendarEraName, indeterminatePosition
    -->
    <xsl:template match="gml:TimeInstant">
        <label class="control-label">Instant</label>
        <div class="controls">
            <div class="controls-well">
                <xsl:if test="./gmd:description">
                    <label class="control-label">Description</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:if test="./gmd:description">
                                <xsl:value-of select=".//gmd:description" />
                            </xsl:if>
                        </div>
                    </div>
                </xsl:if>
                <label class="control-label">Time Position</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:value-of select=".//gml:timePosition" />
                    </div>
                </div>
                <xsl:if test=".//gml:timePosition/@calendarEraName">
                    <label class="control-label">Calendar Era Name</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:value-of select=".//gml:timePosition/@calendarEraName" />
                        </div>
                    </div>
                </xsl:if>
                <xsl:if test=".//gml:timePosition/@indeterminatePosition">
                    <label class="control-label">Time Position</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:value-of select=".//gml:timePosition/@indeterminatePosition" />
                        </div>
                    </div>
                </xsl:if>
            </div>
        </div>
    </xsl:template>
    <!-- TimePeriod-->
    <!-- TODO: 
        - All the attributes
        - timeInterval units, radix, factor
    -->
    <xsl:template match="gml:TimePeriod">
        <xsl:if test=".//gml:beginPosition">
            <div class="control-group">
                <label class="control-label">Begin</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:value-of select=".//gml:beginPosition" />
                    </div>
                    <xsl:if test=".//gml:beginPosition/@calendarEraName">
                        <span>
                            <xsl:value-of select=".//gml:beginPosition/@calendarEraName" /> (calendarEraName)                        
                        </span>
                    </xsl:if>
                    <xsl:if test=".//gml:beginPosition/@indeterminatePosition">
                        <span>
                            <xsl:value-of select=".//gml:beginPosition/@indeterminatePosition" /> (indeterminatePosition)                        
                        </span>
                    </xsl:if>
                </div>
            </div>
        </xsl:if>
        <xsl:if test=".//gml:endPosition">
            <div class="control-group">
                <label class="control-label">End</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:value-of select=".//gml:endPosition" />
                    </div>
                    <xsl:if test=".//gml:endPosition/@calendarEraName">
                        <span>
                            <xsl:value-of select=".//gml:endPosition/@calendarEraName" /> (calendarEraName)                        
                        </span>
                    </xsl:if>
                    <xsl:if test=".//gml:endPosition/@indeterminatePosition">
                        <span>
                            <xsl:value-of select=".//gml:endPosition/@indeterminatePosition" /> (indeterminatePosition)                        
                        </span>
                    </xsl:if>
                </div>
            </div>
        </xsl:if>
        <xsl:if test=".//gml:duration">
            <span>duration:                
                <xsl:value-of select=".//gml:duration" />
            </span>
        </xsl:if>
        <xsl:if test=".//gml:timeInterval">
            <span>timeInterval:                
                <xsl:value-of select=".//gml:timeInterval" />
            </span>
        </xsl:if>
    </xsl:template>
    <!-- extentTypeCode -->
    <xsl:template match="gmd:extentTypeCode">
        <div class="control-group extentTypeCode">
            <label class="control-label">Type</label>
            <div class="controls">
                <xsl:choose>
                    <xsl:when test="./gco:Boolean = 0">Exclusive</xsl:when>
                    <xsl:when test="./gco:Boolean = 1">Inclusive</xsl:when>
                    <xsl:when test="./@nilReason">
                        <xsl:value-of select="./@nilReason" />
                    </xsl:when>
                </xsl:choose>
            </div>
        </div>
    </xsl:template>
</xsl:stylesheet>
