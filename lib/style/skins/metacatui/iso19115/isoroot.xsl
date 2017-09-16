<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:gmd="http://www.isotc211.org/2005/gmd" 
    xmlns:gco="http://www.isotc211.org/2005/gco" version="1.0">
    <xsl:import href="iso-md.xsl"/>
    <xsl:import href="iso-ci.xsl"/>
    <xsl:import href="iso-ex.xsl"/>
    <xsl:import href="iso-gco.xsl"/>
    <xsl:import href="iso-gmd.xsl"/>
    <xsl:import href="iso-gml.xsl"/>
    <xsl:import href="iso-gmx.xsl"/>
    <xsl:output method="html" encoding="UTF-8" doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" indent="yes" />
    <xsl:template match="*[local-name()='MD_Metadata'] | *[local-name()='MI_Metadata']">
        <form class="form-horizontal">
            <div class="control-group entity">
                <h4>General</h4>
                <!-- fileIdentifier 0:1 -->
                <xsl:if test="./gmd:fileIdentifier and normalize-space(./gmd:fileIdentifier/text())!=''">
                    <div class="control-group">
                        <label class="control-label">File Identifier</label>
                        <div class="controls">
                            <div class="controls-well">
                                <xsl:apply-templates select="./gmd:fileIdentifier" />
                            </div>
                        </div>
                    </div>
                </xsl:if>
                <!-- TODO: language 0:1 -->
                <!-- TODO: characterSet 0:1 -->
                <!-- Parent Identifier 0:1 -->
                <xsl:if test="./gmd:parentIdentifier and normalize-space(./gmd:parentIdentifier/text())!=''">
                    <div class="control-group">
                        <label class="control-label">Parent Identifier</label>
                        <div class="controls">
                            <div class="controls-well">
                                <xsl:apply-templates select="./gmd:parentIdentifier" />
                            </div>
                        </div>
                    </div>
                </xsl:if>
                <!-- TODO: hierarchyLevel 0:∞ -->
                <!-- TODO: hierarchyLevelName 0:∞ -->
                <!-- Alternate identifier(s) 0:∞-->
                <!-- gmd:identifier is an optional part of the CI_Citation element -->
                <xsl:for-each select="./gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:identifier">
                    <xsl:if test="normalize-space(.//gmd:code/gco:CharacterString/text())!=''">
                        <div class="control-group">
                            <label class="control-label">Cited Identifier</label>
                            <div class="controls">
                                <div class="controls-well">
                                    <xsl:variable name="code">
                                        <xsl:value-of select=".//gmd:code/gco:CharacterString/text()" />
                                    </xsl:variable>
                            
                                    <xsl:choose>
                                        <xsl:when test="starts-with($code, 'http')">
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
                                </div>
                            </div>
                        </div>
                    </xsl:if>
                </xsl:for-each>
                
                <!-- Abstract 0:∞  Only shown if the abstract has content -->
                <xsl:for-each select="./gmd:identificationInfo/gmd:MD_DataIdentification/gmd:abstract">
                    <xsl:if test="normalize-space(./gco:CharacterString/text()) != ''">
                        <div class="control-group">
                            <label class="control-label">Abstract</label>
                            <div class="controls">
                                <div class="controls-well">
                                    <xsl:apply-templates />
                                </div>
                            </div>
                        </div>
                    </xsl:if>
                </xsl:for-each>
                <!-- Purpose 0:∞ -->
                <xsl:for-each select="./gmd:identificationInfo/gmd:MD_DataIdentification/gmd:purpose">
                    <xsl:if test="normalize-space(./gco:CharacterString/text()) != ''">
                        <div class="control-group">
                            <label class="control-label">Purpose</label>
                            <div class="controls">
                                <div class="controls-well">
                                    <xsl:apply-templates />
                                </div>
                            </div>
                        </div>
                    </xsl:if>
                </xsl:for-each>
                <!-- Publication date -->
                <xsl:for-each select="./gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:date">
                    <xsl:if test="./gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/text() = 'publication'">
                        <div class="control-group">
                            <label class="control-label">Publication Date</label>
                            <div class="controls">
                                <div class="controls-well">
                                    <xsl:apply-templates select="./gmd:CI_Date/gmd:date" />
                                </div>
                            </div>
                        </div>
                    </xsl:if>
                </xsl:for-each>
                <!-- Status  -->
                <xsl:for-each select="./gmd:status">
                    <div class="control-group">
                        <label class="control-label">Status</label>
                        <div class="controls">
                            <div class="controls-well">
                                <xsl:apply-templates />
                            </div>
                        </div>
                    </div>
                </xsl:for-each>
                <!-- Topic Categories

                These span multiple MD_DataIdentification Sections so all are
                grabbed and rendered in the same place.
                -->
                <xsl:if test=".//gmd:topicCategory">
                    <div class="control-group">
                        <label class="control-label">Topic Categories</label>
                        <div class="controls">
                            <div class="controls-well">
                                <table class="table table-condensed">
                                    <thead>
                                        <tr>
                                            <th>Topic</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <xsl:for-each select=".//gmd:topicCategory">
                                            <tr>
                                                <td>
                                                    <xsl:apply-templates />
                                                </td>
                                            </tr>
                                        </xsl:for-each>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </xsl:if>
                <!-- Keywords

                These span multiple MD_DataIdentification Sections so all are
                grabbed and rendered in the same place.

                Each <gmd:descriptiveKeywords> block should have one or more keywords in it
                with one thesaurus. So we render keywords from the same thesaurus together.
                -->
                <xsl:if test="//gmd:descriptiveKeywords">
                    <div class="control-group">
                        <label class="control-label">Descriptive Keywords</label>
                        <div class="controls">
                            <div class="controls-well">
                                <xsl:for-each select="//gmd:descriptiveKeywords">
                                    <xsl:apply-templates />
                                </xsl:for-each>
                            </div>
                        </div>
                    </div>
                </xsl:if>
            </div>
            <div class="control-group entity">
                <h4>People and Associated Parties</h4>
                <!-- Metadata Contact(s) 1:inf -->
                <div class="control-group">
                    <label class="control-label">Metadata Contacts</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:for-each select="./gmd:contact">
                                <xsl:apply-templates />
                            </xsl:for-each>
                        </div>
                    </div>
                </div>
                <!-- Data Set Contact(s) -->
                <div class="control-group">
                    <label class="control-label">Data Set Contacts</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:identificationInfo/gmd:MD_DataIdentification/gmd:pointOfContact" />
                        </div>
                    </div>
                </div>
                <!-- Cited responsible parties-->
                <div class="control-group">
                    <label class="control-label">Responsible Parties</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty" />
                        </div>
                    </div>
                </div>
            </div>
            <!-- Extent (geographic, temporal, vertical) -->
            <xsl:for-each select="./gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent">
                <xsl:apply-templates />
            </xsl:for-each>
            <xsl:if test=".//gmd:metadataConstraints">
                <div class="control-group entity">
                    <h4>Metadata Constraints</h4>
                    <xsl:for-each select=".//gmd:metadataConstraints">
                        <xsl:apply-templates />
                    </xsl:for-each>
                </div>
            </xsl:if>
            <xsl:if test=".//gmd:resourceConstraints">
                <div class="control-group entity">
                    <h4>Resource Constraints</h4>
                    <xsl:for-each select=".//gmd:resourceConstraints">
                        <xsl:apply-templates />
                    </xsl:for-each>
                </div>
            </xsl:if>
            <xsl:if test=".//gmd:supplementalInformation">
                <div class="control-group entity">
                    <h4>Supplemental Information</h4>
                    <xsl:for-each select=".//gmd:supplementalInformation">
                        <xsl:apply-templates />
                    </xsl:for-each>
                </div>
            </xsl:if>
        </form>
    </xsl:template>
    <!-- General, high-level templates -->
    <!-- TODO: Figure out how to do this: I want to capture the scenario where
    an element like gmd:individualName has no child gco:CharacterString or
    equivalent but has a nilReason attribute. -->
    <xsl:template match="*[not(*) and ./@nilReason]">
        <xsl:value-of select="@nilReason" />
    </xsl:template>
</xsl:stylesheet>
