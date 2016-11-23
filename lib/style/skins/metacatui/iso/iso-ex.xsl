<?xml version="1.0"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:gmd="http://www.isotc211.org/2005/gmd" 
  xmlns:gco="http://www.isotc211.org/2005/gco" 
  xmlns:gml="http://www.opengis.net/gml/3.2" version="1.0">

  <xsl:output method="html" encoding="UTF-8"
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
      indent="yes" />  

  <!-- EX_BoundingPolygon-->
  <!-- TODO: This whole thing -->
  <xsl:template name="ex_boundingpolygon" match="gmd:EX_BoundingPolygon">
    <xsl:apply-templates />
  </xsl:template>

  <!-- EX_Extent-->
  <!-- TODO: Convert this to EX_Extent and use apply-templates elsewhere-->
  <xsl:template name="extent">
    <!-- description -->
    <xsl:if test=".//gmd:description">
      <div class="control-group">
        <label class="control-label">Description</label>
        <div class="controls">
          <div class="controls-well">
            <xsl:value-of select=".//gmd:description/gco:CharacterString/text()" />
          </div>
        </div>
      </div>
    </xsl:if>

    <!-- Geographic -->
    <xsl:for-each select=".//gmd:geographicElement">
      <div class="control-group">
        <label class="control-label">Geographic</label>
        <div class="controls">
          <div class="controls-well">
            <xsl:apply-templates />
          </div>
        </div>
      </div>
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

  <!-- EX_GeographicBoundingBox-->
  <xsl:template name="ex_geographicboundingbox" match="gmd:EX_GeographicBoundingBox">
  	<xsl:variable name="north"><xsl:value-of select="./gmd:northBoundLongitude/gco:Decimal" /></xsl:variable>
  	<xsl:variable name="east"><xsl:value-of select="./gmd:eastBoundLongitude/gco:Decimal" /></xsl:variable>
  	<xsl:variable name="south"><xsl:value-of select="./gmd:southBoundLongitude/gco:Decimal" /></xsl:variable>
  	<xsl:variable name="west"><xsl:value-of select="./gmd:westBoundLongitude/gco:Decimal" /></xsl:variable>


    <div data-value="{$north}" data-content="northBoundingCoordinate" class="control-group northBoundingCoordinate">
      <label class="control-label">North</label>
      <div class="controls"><xsl:value-of select="$north" />&#xa0; degrees</div>
    </div>

    <div data-value="{$east}" data-content="eastBoundingCoordinate" class="control-group eastBoundingCoordinate">
      <label class="control-label">East</label>
      <div class="controls"><xsl:value-of select="$east" />&#xa0; degrees</div>
    </div>

    <div data-value="{$south}" data-content="southBoundingCoordinate" class="control-group southBoundingCoordinate">
      <label class="control-label">South</label>
      <div class="controls"><xsl:value-of select="$south" />&#xa0; degrees</div>
    </div>

    <div data-value="{$west}" data-content="westBoundingCoordinate" class="control-group westBoundingCoordinate">
      <label class="control-label">West</label>
      <div class="controls"><xsl:value-of select="$west" />&#xa0; degrees</div>
    </div>

    <span>
      <xsl:apply-templates select="./gmd:extentTypeCode" />
    </span>
  </xsl:template>

  <!-- EX_GeographicDescription-->
  <xsl:template name="ex_geographicdescription" match="gmd:EX_GeographicDescription">
    <xsl:apply-templates />
  </xsl:template>

  <!-- EX_GeographicExtent-->
  <xsl:template name="ex_geographicextent" match="gmd:EX_GeographicExtent">
    <xsl:apply-templates />
  </xsl:template>

  <!-- EX_TemporalExtent -->
  <xsl:template name="ex_temporalextent" match="gmd:EX_TemporalExtent">
    <span>temporal extent                
      <xsl:apply-templates select=".//gmd:extent/*" />
    </span>
  </xsl:template>

  <!-- EX_VerticalExtent-->
  <!-- TODO: Improve DOM structure -->
  <xsl:template name="verticalextent" match="gmd:EX_VerticalExtent">
    <span>minimumValue:     
      <xsl:value-of select="./gmd:minimumValue/gco:Real" />
    </span>
    <span>maximumValue:     
      <xsl:value-of select="./gmd:maximumValue/gco:Real" />
    </span>
    <!-- TODO: Flesh this out a bit more -->
    <span>verticalCRS:     
      <xsl:value-of select="./gmd:verticalCRS" />
    </span>
  </xsl:template>

  <!-- TimeInstant-->
  <!-- TODO
        - Attributes: frame, calendarEraName, indeterminatePosition
    -->
  <xsl:template name="timeinstant" match="gml:TimeInstant">
    <span>time instant</span>
    <span>description:             
      <xsl:value-of select=".//gmd:description" />
    </span>
    <span>
      <span>timePosition:             
        <xsl:value-of select=".//gml:timePosition" />
      </span>
      <xsl:if test=".//gml:timePosition/@calendarEraName">
        <span>
          <xsl:value-of select=".//gml:timePosition/@calendarEraName" /> (calendarEraName)
        </span>
      </xsl:if>
      <xsl:if test=".//gml:timePosition/@indeterminatePosition">
        <span>
          <xsl:value-of select=".//gml:timePosition/@indeterminatePosition" /> (indeterminatePosition)
        </span>
      </xsl:if>
    </span>
  </xsl:template>

  <!-- TimePeriod-->
  <!-- TODO: 
        - All the attributes
        - timeInterval units, radix, factor
    -->
  <xsl:template name="timeperiod" match="gml:TimePeriod">




    <xsl:if test=".//gml:beginPosition">
      <div class="control-group">
        <label class="control-label">Begin</label>
        <div class="controls">
          <span><xsl:value-of select=".//gml:beginPosition" /></span>
          <xsl:if test=".//gml:beginPosition/@calendarEraName">
            <span><xsl:value-of select=".//gml:beginPosition/@calendarEraName" /> (calendarEraName)</span>                        
          </xsl:if>
          <xsl:if test=".//gml:beginPosition/@indeterminatePosition">
            <span><xsl:value-of select=".//gml:beginPosition/@indeterminatePosition" /> (indeterminatePosition)</span>                        
          </xsl:if>
        </div>
      </div>
    </xsl:if>

    <xsl:if test=".//gml:endPosition">
      <div class="control-group">
        <label class="control-label">End</label>
        <div class="controls">
          <span><xsl:value-of select=".//gml:endPosition" /></span>
          <xsl:if test=".//gml:endPosition/@calendarEraName">
            <span><xsl:value-of select=".//gml:endPosition/@calendarEraName" /> (calendarEraName)</span>                        
          </xsl:if>
          <xsl:if test=".//gml:endPosition/@indeterminatePosition">
            <span><xsl:value-of select=".//gml:endPosition/@indeterminatePosition" /> (indeterminatePosition)</span>                        
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
  <!-- TODO: Work on language. Not sure what I want here.-->
  <xsl:template name="gmd_extent_type_code" match="gmd:extentTypeCode">
  <span>extentTypeCode:     
    <xsl:choose>
      <xsl:when test="./gco:Boolean = 0"><span>Exclusion</span></xsl:when>
      <xsl:when test="./gco:Boolean = 1"><span>Inclusion</span></xsl:when>
    </xsl:choose>
    </span>
  </xsl:template>
</xsl:stylesheet>