<?xml version="1.0"?>
<!--
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file that is valid with respect to the eml-variable.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:import href="eml-literature.xsl"/>

  <xsl:output method="html" encoding="UTF-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
    indent="yes" />  

  <!-- This module is for coverage and it is self contained(It is a table
       and will handle reference by it self)-->
  <xsl:template name="coverage">
        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
          <div class="row-fluid">
            <xsl:for-each select="geographicCoverage">
                <xsl:call-template name="geographicCoverage">
                </xsl:call-template>
            </xsl:for-each>
          </div>
          <div class="row-fluid">
             <xsl:for-each select="temporalCoverage">
                <xsl:call-template name="temporalCoverage">
                </xsl:call-template>
            </xsl:for-each>
          </div>
          <div class="row-fluid">
            <xsl:for-each select="taxonomicCoverage">
                <xsl:call-template name="taxonomicCoverage">
                </xsl:call-template>
            </xsl:for-each>
          </div>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <div class="row-fluid">
            <xsl:for-each select="geographicCoverage">
                <xsl:call-template name="geographicCoverage">
                </xsl:call-template>
            </xsl:for-each>
          </div>
          <div class="row-fluid">
            <xsl:for-each select="temporalCoverage">
                <xsl:call-template name="temporalCoverage">
                </xsl:call-template>
            </xsl:for-each>
          </div>
          <div class="row-fluid">
            <xsl:for-each select="taxonomicCoverage">
                <xsl:call-template name="taxonomicCoverage">
                </xsl:call-template>
            </xsl:for-each>
          </div>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

 <!-- ********************************************************************* -->
 <!-- **************  G E O G R A P H I C   C O V E R A G E  ************** -->
 <!-- ********************************************************************* -->
  <xsl:template name="geographicCoverage">
    <xsl:choose>
      <xsl:when test="references!=''">
        <xsl:variable name="ref_id" select="references"/>
        <xsl:variable name="references" select="$ids[@id=$ref_id]" />
        <xsl:for-each select="$references">
        	
          	<div class="row-fluid geographicCoverage" data-content="geographicCoverage">
              <xsl:call-template name="geographicCovCommon" />
            </div>
            
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
          <div class="row-fluid geographicCoverage" data-content="geographicCoverage">
            <xsl:call-template name="geographicCovCommon" />
          </div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="geographicCovCommon">
      <!-- Geographic Region -->
      <h4>Geographic Region</h4>
      <xsl:apply-templates select="geographicDescription"/>
      <xsl:apply-templates select="boundingCoordinates"/>
      <xsl:for-each select="datasetGPolygon">
          <xsl:if test="datasetGPolygonOuterGRing">
            <xsl:apply-templates select="datasetGPolygonOuterGRing"/>
          </xsl:if>
          <xsl:if test="datasetGPolygonExclusionGRing">
              <xsl:apply-templates select="datasetGPolygonExclusionGRing"/>
          </xsl:if>
     </xsl:for-each>
  </xsl:template>

  <xsl:template match="geographicDescription">
    <div class="control-group geographicDescription" data-content="geographicDescription">
		<label class="control-label">Geographic Description</label>
      	<div class="controls controls-well">
      		<xsl:value-of select="."/>
    	</div>
	</div>
  </xsl:template>

  <xsl:template match="boundingCoordinates">
      <div class="control-group boundingCoordinates" data-content="boundingCoordinates">
		<label class="control-label">Bounding Coordinates</label>
      	<div class="controls controls-well">
      		<xsl:apply-templates select="northBoundingCoordinate"/>
			<xsl:apply-templates select="southBoundingCoordinate"/>
			<xsl:apply-templates select="eastBoundingCoordinate"/>
			<xsl:apply-templates select="westBoundingCoordinate"/>
			<xsl:apply-templates select="boundingAltitudes"/>
      </div>
	</div>
  </xsl:template>

  <xsl:template match="westBoundingCoordinate">
  	<xsl:variable name="west"><xsl:value-of select="."/></xsl:variable>

    <div class="control-group westBoundingCoordinate" data-content="westBoundingCoordinate" data-value="{$west}">
		<label class="control-label"><xsl:text>West</xsl:text></label>        
        <div class="controls">
         	<xsl:value-of select="."/>&#160; degrees
        </div>
     </div>
  </xsl:template>

  <xsl:template match="eastBoundingCoordinate">
	<xsl:variable name="east"><xsl:value-of select="."/></xsl:variable>

    <div class="control-group eastBoundingCoordinate" data-content="eastBoundingCoordinate" data-value="{$east}">
		<label class="control-label"><xsl:text>East</xsl:text></label>
		<div class="controls">
			<xsl:value-of select="."/>&#160; degrees
		</div>
     </div>
  </xsl:template>

  <xsl:template match="northBoundingCoordinate">
   	<xsl:variable name="north"><xsl:value-of select="."/></xsl:variable>

    <div class="control-group northBoundingCoordinate" data-content="northBoundingCoordinate" data-value="{$north}">
		<label class="control-label"><xsl:text>North</xsl:text></label>        
        <div class="controls">
          <xsl:value-of select="."/>&#160; degrees
        </div>
     </div>
  </xsl:template>

  <xsl:template match="southBoundingCoordinate">
    <xsl:variable name="south"><xsl:value-of select="."/></xsl:variable>
  
    <div class="control-group southBoundingCoordinate" data-content="southBoundingCoordinate"  data-value="{$south}">
		<label class="control-label"><xsl:text>South</xsl:text></label>
		<div class="controls">
			<xsl:value-of select="."/>&#160; degrees
        </div>
    </div>
  </xsl:template>


  <xsl:template match="boundingAltitudes">

      <div class="control-group">
		<label class="control-label">Minimum Altitude</label>
		<div class="controls">
        	<xsl:apply-templates select="altitudeMinimum"/>
        </div>
      </div>  
      <div class="control-group">
		<label class="control-label">Maximum Altitude</label>
		<div class="controls">
        	<xsl:apply-templates select="altitudeMaximum"/>
        </div>
       </div>

  </xsl:template>

  <xsl:template match="altitudeMinimum">
     <xsl:value-of select="."/> &#160;<xsl:value-of select="../altitudeUnits"/>
  </xsl:template>

  <xsl:template match="altitudeMaximum">
    <xsl:value-of select="."/> &#160;<xsl:value-of select="../altitudeUnits"/>
  </xsl:template>

  <xsl:template match="datasetGPolygonOuterGRing">
    <div class="control-group">
		<label class="control-label"><xsl:text>G-Ploygon(Outer Ring)</xsl:text>
        </label>
        <div class="controls">
           <xsl:apply-templates select="gRingPoint"/>
           <xsl:apply-templates select="gRing"/>
        </div>
     </div>
  </xsl:template>

  <xsl:template match="datasetGPolygonExclusionGRing">
    <div class="control-group">
		<label class="control-label"><xsl:text>G-Ploygon(Exclusion Ring)</xsl:text>
        </label>
        <div class="controls">
           <xsl:apply-templates select="gRingPoint"/>
           <xsl:apply-templates select="gRing"/>
        </div>
     </div>
  </xsl:template>

  <xsl:template match="gRing">
    <xsl:text>(GRing) &#160;</xsl:text>
    <xsl:text>Latitude: </xsl:text>
    <xsl:value-of select="gRingLatitude"/>,
    <xsl:text>Longitude: </xsl:text>
    <xsl:value-of select="gRingLongitude"/><br/>
  </xsl:template>

  <xsl:template match="gRingPoint">
    <xsl:text>Latitude: </xsl:text>
    <xsl:value-of select="gRingLatitude"/>,
    <xsl:text>Longitude: </xsl:text>
    <xsl:value-of select="gRingLongitude"/><br/>
  </xsl:template>

<!-- ********************************************************************* -->
<!-- ****************  T E M P O R A L   C O V E R A G E  **************** -->
<!-- ********************************************************************* -->

  <xsl:template name="temporalCoverage">
    <xsl:choose>
      <xsl:when test="references!=''">
        <xsl:variable name="ref_id" select="references"/>
        <xsl:variable name="references" select="$ids[@id=$ref_id]" />
        <xsl:for-each select="$references">
        	<div class="row-fluid temporalCoverage" data-content="temporalCoverage">
            	<xsl:call-template name="temporalCovCommon" />
            </div>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        	<div class="row-fluid temporalCoverage" data-content="temporalCoverage">
	            <xsl:call-template name="temporalCovCommon" />
	        </div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="temporalCovCommon" >
		<h4>Temporal Coverage</h4>
		<xsl:apply-templates select="singleDateTime"/>
		<xsl:apply-templates select="rangeOfDates"/>
  </xsl:template>

  <xsl:template match="singleDateTime">
    <div class="control-group">
		<label class="control-label">Single Date</label>
        <div class="controls controls-well">
             <xsl:call-template name="singleDateType" />
        </div>
     </div>
   </xsl:template>

  <xsl:template match="rangeOfDates">
 	<div class="control-group">
		<label class="control-label">Date Range</label>
		<div class="controls controls-well">
		     <div class="control-group">
				<label class="control-label">Begin</label>
		         <div class="controls">
		            <xsl:apply-templates select="beginDate"/>
		         </div>
		     </div>
		     <div class="control-group">    
		         <label class="control-label">End</label>
		         <div class="controls">
		            <xsl:apply-templates select="endDate"/>
		          </div>
		     </div>
	     </div>
     </div>
  </xsl:template>


  <xsl:template match="beginDate">
      <xsl:call-template name="singleDateType"/>
  </xsl:template>

  <xsl:template match="endDate">
      <xsl:call-template name="singleDateType"/>
  </xsl:template>

  <xsl:template name="singleDateType">
  	<div class="row-fluid">
		<xsl:if test="calendarDate">
          <xsl:value-of select="calendarDate"/>
          <xsl:if test="./time and normalize-space(./time)!=''">
            <xsl:text>&#160; at &#160;</xsl:text><xsl:apply-templates select="time"/>
          </xsl:if>
		</xsl:if>
		<xsl:if test="alternativeTimeScale">
			<xsl:apply-templates select="alternativeTimeScale"/>
		</xsl:if>
     </div>
  </xsl:template>


  <xsl:template match="alternativeTimeScale">

        <div class="control-group">
			<label class="control-label">Timescale</label>
			<div class="controls">
				<xsl:value-of select="timeScaleName"/>
			</div>
		</div>
        <div class="control-group">
			<label class="control-label">Time estimate</label>
			<div class="controls">
				<xsl:value-of select="timeScaleAgeEstimate"/>
			</div>
		</div>
        <xsl:if test="timeScaleAgeUncertainty and normalize-space(timeScaleAgeUncertainty)!=''">
        	<div class="control-group">
				<label class="control-label">Time uncertainty</label>
				<div class="controls">
					<xsl:value-of select="timeScaleAgeUncertainty"/>
				</div>
			</div>
        </xsl:if>
        <xsl:if test="timeScaleAgeExplanation and normalize-space(timeScaleAgeExplanation)!=''">
        	<div class="control-group">
				<label class="control-label">Time explanation</label>
				<div class="controls">
					<xsl:value-of select="timeScaleAgeExplanation"/>
				</div>
			</div>
        </xsl:if>
        <xsl:if test="timeScaleCitation and normalize-space(timeScaleCitation)!=''">
        	<div class="control-group">
				<label class="control-label">Citation</label>
				<div class="controls">
            		<xsl:apply-templates select="timeScaleCitation"/>
        		</div>
        	</div>
        </xsl:if>

  </xsl:template>

  <xsl:template match="timeScaleCitation">
     <!-- Using citation module here -->
     <xsl:call-template name="citation">
     </xsl:call-template>
  </xsl:template>

<!-- ********************************************************************* -->
<!-- ***************  T A X O N O M I C   C O V E R A G E  *************** -->
<!-- ********************************************************************* -->
  <xsl:template name="taxonomicCoverage">
     <xsl:choose>
      <xsl:when test="references!=''">
        <xsl:variable name="ref_id" select="references"/>
        <xsl:variable name="references" select="$ids[@id=$ref_id]" />
        <xsl:for-each select="$references">
        	<div class="row-fluid taxonomicCoverage" data-content="taxonomicCoverage">
            	<xsl:call-template name="taxonomicCovCommon" />
            </div>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        	<div class="row-fluid taxonomicCoverage" data-content="taxonomicCoverage">
	          <xsl:call-template name="taxonomicCovCommon" />
	         </div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template name="taxonomicCovCommon">
      <h4>Taxonomic Range</h4>

      <xsl:apply-templates select="taxonomicSystem"/>
      <xsl:apply-templates select="generalTaxonomicCoverage"/>
      <xsl:for-each select="taxonomicClassification">
          <xsl:apply-templates select="."/>
      </xsl:for-each>
  </xsl:template>


 <xsl:template match="taxonomicSystem">
     <div class="control-group">
		<label class="control-label"><xsl:text>Taxonomic System</xsl:text></label>
        <div class="controls controls-well">
              <xsl:apply-templates select="./*"/>
        </div>
     </div>
  </xsl:template>


  <xsl:template match="classificationSystem">
     <xsl:for-each select="classificationSystemCitation">
        <div class="control-group">
			<label class="control-label">Classification Citation</label>
          	<div class="controls">
	           <xsl:call-template name="citation">
	             <xsl:with-param name="citationfirstColStyle" select="$firstColStyle"/>
	             <xsl:with-param name="citationsubHeaderStyle" select="$subHeaderStyle"/>
	           </xsl:call-template>
	         </div>
        </div>
     </xsl:for-each>
     <xsl:if test="classificationSystemModifications and normalize-space(classificationSystemModifications)!=''">
      <div class="control-group">
		<label class="control-label">Modification</label>
        <div class="controls">
          <xsl:value-of select="classificationSystemModifications"/>
        </div>
      </div>
     </xsl:if>
  </xsl:template>


  <xsl:template match="identificationReference">
      <div class="control-group">
		<label class="control-label">ID Reference</label>
        <div class="controls">
             <xsl:call-template name="citation">
                <xsl:with-param name="citationfirstColStyle" select="$firstColStyle"/>
                <xsl:with-param name="citationsubHeaderStyle" select="$subHeaderStyle"/>
             </xsl:call-template>
          </div>
     </div>
  </xsl:template>

  <xsl:template match="identifierName">
      <div class="control-group">
		<label class="control-label">ID Name</label>
        <div class="controls">
           <xsl:call-template name="party">
             <xsl:with-param name="partyfirstColStyle" select="$firstColStyle"/>
           </xsl:call-template>
        </div>
      </div>
  </xsl:template>

  <xsl:template match="taxonomicProcedures">
    <div class="control-group">
		<label class="control-label"><xsl:text>Procedures</xsl:text></label>
		<div class="controls">
        	<xsl:value-of select="."/>
        </div>
    </div>
  </xsl:template>

  <xsl:template match="taxonomicCompleteness">
    <div class="control-group">
		<label class="control-label"><xsl:text>Completeness</xsl:text></label>
		<div class="contorls">
        	<xsl:value-of select="."/>
        </div>
    </div>
  </xsl:template>

  <xsl:template match="vouchers">
      <div class="control-group">
		<label class="control-label">Vouchers</label>
        <div class="controls">
	        <xsl:apply-templates select="specimen"/>
	        <xsl:apply-templates select="repository"/>
        </div>
      </div>
  </xsl:template>

  <xsl:template match="specimen">
    <div class="control-group">
		<label class="control-label"><xsl:text>Specimen</xsl:text></label>
		<div class="controls">
        	<xsl:value-of select="."/>
        </div>
    </div>
  </xsl:template>

  <xsl:template match="repository">
    <div class="control-group">
		<label class="control-label">Repository</label>
        <div class="controls">
            <xsl:for-each select="originator">
               <xsl:call-template name="party">
                 <xsl:with-param name="partyfirstColStyle" select="$firstColStyle"/>
               </xsl:call-template>
            </xsl:for-each>
        </div>
    </div>
  </xsl:template>


  <xsl:template match="generalTaxonomicCoverage">
      <div class="control-group">
		<label class="control-label"><xsl:text>General Coverage</xsl:text></label>
        <div class="controls controls-well">
             <xsl:value-of select="."/>
        </div>
      </div>
  </xsl:template>


  <xsl:template match="taxonomicClassification">
    <div class="control-group">
		<label class="control-label"><xsl:text>Classification</xsl:text></label>
		<div class="controls controls-well">
        	<xsl:apply-templates select="./*" mode="nest"/>
        </div>
    </div>
  </xsl:template>

  <xsl:template match="taxonRankName" mode="nest" >
      <div class="control-group">
		<label class="control-label"><xsl:text>Rank Name</xsl:text></label>
		<div class="controls">
        	<xsl:value-of select="."/>
        </div>
   	</div>
  </xsl:template>

  <xsl:template match="taxonRankValue" mode="nest">
      <div class="control-group">
		<label class="control-label"><xsl:text>Rank Value</xsl:text></label>
		<div class="controls">
        	<xsl:value-of select="."/>
       	</div>
    </div>
  </xsl:template>

  <xsl:template match="commonName" mode="nest">
      <div class="control-group">
		<label class="control-label"><xsl:text>Common Name</xsl:text></label>
		<div class="controls">
            <xsl:value-of select="."/>
        </div>
      </div>
  </xsl:template>

  <xsl:template match="taxonomicClassification" mode="nest">
    <div class="control-group">
		<label class="control-label"><xsl:text>Classification</xsl:text></label>
        <div class="controls">
             <xsl:apply-templates select="./*" mode="nest"/>
        </div>
     </div>
  </xsl:template>

</xsl:stylesheet>
