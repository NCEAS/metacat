<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:gmd="http://www.isotc211.org/2005/gmd" version="1.0">
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
</xsl:stylesheet>
