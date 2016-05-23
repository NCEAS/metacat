<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:dct="http://purl.org/dc/terms/"
	xmlns:dcx="http://ns.dataone.org/metadata/schema/onedcx/v1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns="http://purl.org/dc/terms/">


	<xsl:template name="onedcx">
		<article class="container" id="Metadata">
			<form class="form-horizontal">
				<xsl:for-each select="*">
	
					<xsl:call-template name="citation" />
					<xsl:call-template name="general" />
					<xsl:call-template name="keywords" />
					<xsl:call-template name="people" />
					<xsl:call-template name="geography" />
					<xsl:call-template name="temporal" />
	
				</xsl:for-each>
			</form>
			

		</article>
	</xsl:template>

	<xsl:template name="citation">
		<cite class="citation">
			<xsl:value-of
				select="normalize-space(dcx:dcTerms/dct:bibliographicCitation)" />
		</cite>
	</xsl:template>

	<xsl:template name="general">
		<h4>General Content</h4>
		<div class="control-group">
			<label class="control-label">Title</label>
			<div class="controls controls-well">
				<strong>
					<xsl:value-of select="normalize-space(dcx:simpleDc/dct:title)" />
				</strong>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label">Identifier</label>
			<div class="controls">
				<div class="controls-well">
					<xsl:value-of select="$pid" />
				</div>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label">Alternate Identifier</label>
			<div class="controls">
				<div class="controls-well">
					<xsl:value-of select="normalize-space(dcx:simpleDc/dct:identifier)" />
				</div>
			</div>
		</div>
		
		<div class="control-group">
			<label class="control-label">Online Access</label>
			<div class="controls controls-well">
				<xsl:for-each select="dcx:dcTerms/dct:references">
					<a>
						<xsl:attribute name="href">
							<xsl:value-of select="." />
						</xsl:attribute>
						<xsl:value-of select="." />
					</a>
				</xsl:for-each>
			</div>
		</div>
		
		
		<div class="control-group">
			<label class="control-label">Abstract</label>
			<div class="controls controls-well">
				<div class="sectionText">
					<p>
						<xsl:value-of select="normalize-space(dcx:dcTerms/dct:abstract)" />
					</p>
				</div>
			</div>
		</div>
	</xsl:template>

	<xsl:template name="keywords">
		<div class="row-fluid">
			<div class="control-group">
				<label class="control-label">Keywords (Dublin Core Terms subject)</label>
				<div class="controls controls-well">
					<table class="table table-striped table-condensed">
						<thead>
							<tr>
								<th>Keyword</th>
								<th>Type</th>
							</tr>
						</thead>
						<xsl:apply-templates select="dcx:simpleDc/dct:subject" />
					</table>
				</div>

			</div>
		</div>
	</xsl:template>

	<xsl:template name="keyword" match="dct:subject">
		<tr>
			<td>
				<xsl:value-of select="." />
			</td>
			<td></td>
		</tr>
	</xsl:template>

	<xsl:template name="people">
		<h4>Creators</h4>
		<xsl:for-each select="dcx:simpleDc/dct:creator">
			<div class="controls controls-well">
				<xsl:value-of select="." />
			</div>
		</xsl:for-each>

	</xsl:template>

	<xsl:template name="geography">
		<div class="row-fluid">
			<div data-content="geographicCoverage" class="row-fluid geographicCoverage">
				<h4>Geographic Region</h4>
				<div data-content="geographicDescription" class="control-group geographicDescription">
					<label class="control-label">Geographic Description</label>
					<div class="controls controls-well">
						<xsl:value-of
							select="//dcx:dcTerms/dct:spatial[not(@xsi:type='dcterms:Box')]" />
					</div>
				</div>

				<div data-content="boundingCoordinates" class="control-group boundingCoordinates">
					<label class="control-label">Bounding Coordinates</label>
					<div class="controls controls-well">					
						<xsl:for-each select="//dcx:dcTerms/dct:spatial[@xsi:type='Box']">
							<xsl:call-template name="extract-coordinates" />
						</xsl:for-each>
					</div>	
				</div>

			</div>
		</div>
	</xsl:template>

	<xsl:template name="extract-coordinates">
		<xsl:call-template name="show-coordinate">
			<xsl:with-param name="data" select="." />
			<xsl:with-param name="corner" select="'northlimit'" />
			<xsl:with-param name="label" select="'North'" />
			<xsl:with-param name="bound" select="'northBoundingCoordinate'" />
		</xsl:call-template>
		<xsl:call-template name="show-coordinate">
			<xsl:with-param name="data" select="." />
			<xsl:with-param name="corner" select="'southlimit'" />
			<xsl:with-param name="label" select="'South'" />
			<xsl:with-param name="bound" select="'southBoundingCoordinate'" />
		</xsl:call-template>
		<xsl:call-template name="show-coordinate">
			<xsl:with-param name="data" select="." />
			<xsl:with-param name="corner" select="'eastlimit'" />
			<xsl:with-param name="label" select="'East'" />
			<xsl:with-param name="bound" select="'eastBoundingCoordinate'" />
		</xsl:call-template>
		<xsl:call-template name="show-coordinate">
			<xsl:with-param name="data" select="." />
			<xsl:with-param name="corner" select="'westlimit'" />
			<xsl:with-param name="label" select="'West'" />
			<xsl:with-param name="bound" select="'westBoundingCoordinate'" />
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="show-coordinate">
		<xsl:param name="data" />
		<xsl:param name="corner" />
		<xsl:param name="label" />
		<xsl:param name="bound" />
		<xsl:variable name="coord"
			select="substring-before(substring-after($data,concat($corner,'=')),';')" />
		<div data-value="{$coord}" data-content="{$bound}" class="control-group {$bound}">
			<label class="control-label">
				<xsl:value-of select="$label" />
			</label>
			<div class="controls controls-well">
				<xsl:value-of select="$coord" />
				<xsl:text> degrees</xsl:text>
			</div>
		</div>
	</xsl:template>
	
	<xsl:template name="extract-period">
		<xsl:call-template name="show-temporal">
			<xsl:with-param name="data" select="." />
			<xsl:with-param name="field" select="'start'" />
			<xsl:with-param name="label" select="'Begin Date'" />
		</xsl:call-template>
		<xsl:call-template name="show-temporal">
			<xsl:with-param name="data" select="." />
			<xsl:with-param name="field" select="'end'" />
			<xsl:with-param name="label" select="'End Date'" />
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template name="show-temporal">
		<xsl:param name="data" />
		<xsl:param name="field" />
		<xsl:param name="label" />
		<xsl:variable name="date"
			select="substring-before(substring-after($data,concat($field,'=')),';')" />
		<label class="control-label">
			<xsl:value-of select="$label" />
		</label>
		<div class="controls controls-well">
			<xsl:value-of select="$date" />
		</div>
	</xsl:template>

	<xsl:template name="temporal">
		<div class="row-fluid">
			<div data-content="temporalCoverage" class="row-fluid temporalCoverage">
				<h4>Temporal Coverage</h4>
				<div class="control-group">
					<xsl:if test="//dcx:dcTerms/dct:temporal[@xsi:type='Period']/text() != ''">
						<label class="control-label">Date Range</label>
						<div class="controls controls-well">					
							<xsl:for-each select="//dcx:dcTerms/dct:temporal[@xsi:type='Period']">
								<xsl:call-template name="extract-period" />
							</xsl:for-each>
						</div>	
					</xsl:if>
					<xsl:if test="//dcx:dcTerms/dct:temporal[not(@xsi:type='Period')]/text() != ''">
						<label class="control-label">Single Date</label>
						<div class="controls controls-well">					
							<xsl:for-each select="//dcx:dcTerms/dct:temporal[not(@xsi:type='Period')]">
								<xsl:value-of select="." />
							</xsl:for-each>
						</div>	
					</xsl:if>
				</div>
			</div>
		</div>
	</xsl:template>

</xsl:stylesheet>
