<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:gmd="http://www.isotc211.org/2005/gmd" 
    xmlns:gco="http://www.isotc211.org/2005/gco" 
    xmlns:gmx="http://www.isotc211.org/2005/gmx"
    xmlns:gml="http://www.opengis.net/gml/3.2" 
    xmlns:xlink="http://www.w3.org/1999/xlink"
    version="1.0">

    <xsl:import href="iso-ci.xsl"/>
    <xsl:import href="iso-ex.xsl"/>
    <xsl:import href="iso-md.xsl"/>

    <!-- TODO: Figure out how to set the output method to get what I want -->
    <!-- TODO: ^ Figure out what I want to output -->
    <!-- TODO: Figuer out what this match statement should have in it -->
    <xsl:template match="/">

   	<form class="form-horizontal">
        <!-- Dataset title -->
        <span><xsl:value-of select="//gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString/text()" /></span>

        <!-- Identifier -->
        <div class="control-group">
            <label class="control-label">Identifier</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:value-of select="//gmd:fileIdentifier/gco:CharacterString/text()" />
                </div>
            </div>
        </div>

        <!-- Alternate identifier(s) -->
        <xsl:for-each select="//gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:identifier">
            <xsl:call-template name="alternateidentifier"/>
        </xsl:for-each>

        <!-- Abstract -->
        <div class="control-group">
            <label class="control-label">Abstract</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:value-of select="//gmd:abstract/gco:CharacterString/text()" />
                </div>
            </div>
        </div>

        <!--  Topic Categories -->
        <div class="control-group">
            <label class="control-label">Topic Categories</label>
            <div class="controls">
                <div class="controls-well">
                    <ul>
                        <xsl:for-each select="//gmd:topicCategory">
                            <li><xsl:value-of select="./gmd:MD_TopicCategoryCode/text()" /></li>                
                        </xsl:for-each>
                    </ul>
                </div>
            </div>
        </div>

        <!-- Keywords

            Each <gmd:descriptiveKeywords> block should have one or more keywords in it
            with one thesaurus. So we render keywords from the same thesaurus together.
        -->

        <xsl:for-each select="//gmd:descriptiveKeywords">
            <xsl:call-template name="topiccategory"/>
        </xsl:for-each>

        <!-- TODO: otherEntities? -->

        <!-- Metadata Contact(s) -->
        <div class="control-group">
            <label class="control-label">Metadata Contacts</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:apply-templates select="//gmd:contact" />
                </div>
            </div>
        </div>

        <!-- Data Set Contact(s) -->
        <div class="control-group">
            <label class="control-label">Data Set Contacts</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:apply-templates select="//gmd:identificationInfo/gmd:MD_DataIdentification/gmd:pointOfContact" />
                </div>
            </div>
        </div>

        <!-- Cited responsible parties-->
        <div class="control-group">
            <label class="control-label">Associated Parties</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:apply-templates select="//gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty" />
                </div>
            </div>
        </div>

        <!-- Extent (geographic, temporal, vertical) -->
        <xsl:for-each select="//gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent">
            <div class="control-group">
                <label class="control-label">Extent</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:call-template name="extent" />
                    </div>
                </div>
            </div>
        </xsl:for-each>

        <!-- TODO Methods -->

        </form>
    </xsl:template>

    <!-- General templates that didn't make it into the module-specific 
         stylesheets -->
    
    <!-- Alternate identifier -->
    <xsl:template name="alternateidentifier">
        <div class="control-group">
            <label class="control-label">Alternate Identifier</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:value-of select="./gmd:MD_Identifier/gmd:code/gmx:Anchor/text()" />
                </div>
            </div>
        </div>
    </xsl:template>

    
    <!-- Topic Category-->
    <!-- 
        TODO Type
        TODO Thesaurus
    -->
    <xsl:template name="topiccategory">
        <div class="control-group">
            <label class="control-label">Topic Categories</label>
            <div class="controls">
                <div class="controls-well">
                    <div>Type: <xsl:value-of select="./gmd:MD_Keywords/gmd:type/gmd:MD_KeywordTypeCode/text()" /></div>

                    <!-- TODO: The rest of this CI_Citation element -->
                    <div>Thesaurus: <xsl:value-of select="./gmd:MD_Keywords/gmd:thesaurusName/gmd:CI_Citation/gmd:title/gco:CharacterString/text()" /></div>

                    <ul>
                        <xsl:for-each select="./gmd:MD_Keywords/gmd:keyword">
                            <li><xsl:apply-templates select="." /></li>                
                        </xsl:for-each>
                    </ul>
                </div>
            </div>
        </div>
    </xsl:template>

    <!-- Contact -->
    <!-- TODO: Role-->
    <!-- TODO: ./gmd:contactInfo-->
    <xsl:template name="contact">
        <xsl:call-template name="ci_responsibleparty" />
    </xsl:template>


    <!-- High level templates -->
    
    <xsl:template match="gco:CharacterString">
        <xsl:value-of select="./text()" />
    </xsl:template>

    <xsl:template match="gmx:Anchor">
        <xsl:element name="a">
            <xsl:attribute name="href">
                <xsl:value-of select="./@xlink:href" />
            </xsl:attribute>
            <xsl:value-of select="./text()" />
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>