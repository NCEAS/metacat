<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:gmd="http://www.isotc211.org/2005/gmd"
    xmlns:gml="http://www.opengis.net/gml"
    xmlns:gco="http://www.isotc211.org/2005/gco"
    version="1.0">
    <xsl:template match="gmd:MD_Identifier">
        <xsl:apply-templates select="./gmd:code" />
    </xsl:template>
    <xsl:template match="gmd:RS_Identifier">
        <xsl:apply-templates select="./gmd:code" />
    </xsl:template>
    <xsl:template match="gmd:MD_TopicCategoryCode">
        <xsl:apply-templates />
    </xsl:template>
    <xsl:template match="gmd:MD_Keywords">
        <table class="table table-condensed">
            <thead>
                <tr>
                    <th>Keyword</th>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="./gmd:keyword">
                    <tr>
                        <td>
                            <xsl:apply-templates select="." />
                        </td>
                    </tr>
                </xsl:for-each>
            </tbody>
        </table>
        <xsl:if test="./gmd:type">
            <div class="control-group">
                <label class="control-label">Type</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:value-of select="./gmd:type/gmd:MD_KeywordTypeCode/text()" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:thesaurusName">
            <div class="control-group">
                <label class="control-label">Thesaurus</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:thesaurusName" />
                    </div>
                </div>
            </div>
        </xsl:if>
    </xsl:template>
    <xsl:template match="gmd:MD_Constraints">
        <div class="control-group">
            <label class="control-label">Resource Constraint</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:apply-templates />
                </div>
            </div>
        </div>
    </xsl:template>
    <xsl:template match="gmd:MD_LegalConstraints">
        <div class="control-group">
            <label class="control-label">Legal Constraint</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:for-each select="./gmd:useLimitation">
                        <div class="control-group">
                            <label class="control-label">Use Limitation</label>
                            <div class="controls">
                                <div class="controls-well">
                                    <xsl:apply-templates />
                                </div>
                            </div>
                        </div>
                    </xsl:for-each>
                    <xsl:for-each select="./gmd:accessConstraints">
                        <div class="control-group">
                            <label class="control-label">Access Constraint</label>
                            <div class="controls">
                                <div class="controls-well">
                                    <xsl:apply-templates />
                                </div>
                            </div>
                        </div>
                    </xsl:for-each>
                    <xsl:for-each select="./gmd:useConstraints">
                        <div class="control-group">
                            <label class="control-label">Use Constraints</label>
                            <div class="controls">
                                <div class="controls-well">
                                    <xsl:apply-templates />
                                </div>
                            </div>
                        </div>
                    </xsl:for-each>
                    <xsl:for-each select="./gmd:otherConstraints">
                        <div class="control-group">
                            <label class="control-label">Other Constraint</label>
                            <div class="controls">
                                <div class="controls-well">
                                    <xsl:apply-templates />
                                </div>
                            </div>
                        </div>
                    </xsl:for-each>
                </div>
            </div>
        </div>
    </xsl:template>
    <xsl:template match="gmd:MD_SecurityConstraints">
        <div class="control-group">
            <label class="control-label">Security Constraint</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:apply-templates />
                </div>
            </div>
        </div>
    </xsl:template>
    <!-- TODO: gmd:CI_PresentationFormCode-->
    <!-- TODO: gmd:CI_Series -->
    <xsl:template name="MD_contentInfo">
        <!-- TODO: Support more than MD_CoverageDescription and MI_CoverageDescription in this template -->
        <xsl:param name="contentInfo" />
        <table class="table table-bordered table-hover table-striped">
            <thead>
                <tr>
                    <th>Description</th>
                    <th>Dimension</th>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="$contentInfo/gmd:MD_CoverageDescription | $contentInfo/gmd:MI_CoverageDescription">
                    <tr>
                        <td><xsl:value-of select="./gmd:attributeDescription/gco:RecordType/text()" /></td>
                        <td>
                            <xsl:for-each select="./gmd:dimension">
                                <!-- Optional, either MD_RangeDimension or MD_Band or MI_Band -->
                                <xsl:apply-templates />
                            </xsl:for-each>
                        </td>
                    </tr>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>
    <xsl:template match="gmd:MD_RangeDimension">
        <!-- Either contains sequenceIdentifier or descriptor or both -->
        <!-- No units -->
        <xsl:apply-templates select="./gmd:sequenceIdentifier/*/text()" />
        <xsl:apply-templates select="./gmd:descriptor/*/text()" />
    </xsl:template>
    <xsl:template match="gmd:MD_Band">
        <!-- Has units -->
        <xsl:value-of select="./gmd:descriptor/*/text()" />
        <xsl:apply-templates select="./gmd:units" />
    </xsl:template>
    <xsl:template match="gmd:MI_Band">
        <!--  Has units -->
        <xsl:value-of select="./gmd:descriptor/*/text()" />
        <xsl:apply-templates select="./gmd:units" />
    </xsl:template>
    <xsl:template match="gmd:units">
        <xsl:variable name="unit">
            <xsl:choose>
                <!--  Prefer gml:name over gml:identifier if gml:name is present -->
                <xsl:when test="/*/gml:name">
                    <xsl:value-of select="./*/gml:name/text()" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="./*/gml:identifier/text()" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="concat(' (', $unit, ')')" />
    </xsl:template>
    <xsl:template match="gml:BaseUnit">
    </xsl:template>
    <xsl:template match="gml:ConventionalUnit">
    </xsl:template>
    <xsl:template match="gml:DerivedUnit">
    </xsl:template>
    <xsl:template match="gml:UnitDefinition">
        <xsl:value-of select="./*/gml:identifier/text()" />
        <xsl:value-of select="./*/gml:name/text()" />
    </xsl:template>
</xsl:stylesheet>
