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

  <xsl:output method="html" encoding="UTF-8"
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
      indent="yes" />  

  <!-- TODO: Figure out how to set the output method to get what I want -->
  <!-- TODO: ^ Figure out what I want to output -->
  <!-- TODO: Figuer out what this match statement should have in it -->
  <!-- TODO: Cover gmd:identificationInfo/SV_ServiceIdentification -->

  <xsl:template match="*[local-name()='MD_Metadata'] | *[local-name()='MI_Metadata']">
    <form class="form-horizontal">
      <div class="control-group entity">
        <h4>General</h4>
        <!-- fileIdentifier 1:1 -->
        <div class="control-group">
          <label class="control-label">Identifier</label>
          <div class="controls">
            <div class="controls-well">
                <xsl:value-of select="//gmd:fileIdentifier/gco:CharacterString/text()" />
            </div>
          </div>
        </div>

        <!-- TODO: language 1:1 -->
        <!-- TODO: characterSet 1:1 -->
        
        <!-- Parent Identifier 1:1 conditional -->
        <xsl:if test="//gmd:parentIdentifier">
          <div class="control-group">
            <label class="control-label">Parent Identifier</label>
            <div class="controls">
              <div class="controls-well">
                <xsl:value-of select="//gmd:parentIdentifier/gco:CharacterString/text()" />
              </div>
            </div>
          </div>
        </xsl:if>

        <!-- TODO: hierarchyLevel 0:inf -->
        <!-- TODO: hierarchyLevelName 0:inf -->

        <!-- Alternate identifier(s) 0:inf-->
        <!-- gmd:identifier is an optional aprt of the CI_Citation element -->
        <xsl:for-each select="//gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:identifier">
          <div class="control-group">
            <label class="control-label">Cited Identifier</label>
            <div class="controls">
              <div class="controls-well">
                <xsl:apply-templates />
              </div>
            </div>
          </div>
        </xsl:for-each>

        <!-- Abstract 1:inf-->
        <xsl:for-each select="//gmd:identificationInfo/gmd:MD_DataIdentification/gmd:abstract">
          <div class="control-group">
            <label class="control-label">Abstract</label>
            <div class="controls">
              <div class="controls-well">
                <xsl:apply-templates />
              </div>
            </div>
          </div>
        </xsl:for-each>

        <!--  Topic Categories -->
        <xsl:if test="//gmd:topicCategory">
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
        </xsl:if>
    
        <!-- Keywords

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
          <label class="control-label">Metadata Contact(s)</label>
          <div class="controls">
            <div class="controls-well">
              <xsl:for-each select="//gmd:contact">
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
      </div>

      <!-- TODO: otherEntities? -->

      <!-- Extent (geographic, temporal, vertical) -->
      <xsl:for-each select="//gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent">
        <xsl:apply-templates />
      </xsl:for-each>

      <!-- TODO Methods -->

    </form>
  </xsl:template>

  <!-- General, high-level templates -->

  <!-- TODO: Figure out how to do this: I want to capture the scenario where
  an element like gmd:individualName has on child gco:CharacterString or 
  equivalent but has a nilReasona attribute. -->
  <xsl:template match="@nilReason">
    <xsl:value-of select="@nilReason" />
  </xsl:template>

  <xsl:template match="gco:CharacterString">
    <xsl:value-of select="." />
  </xsl:template>

  <xsl:template match="gmx:Anchor">
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:value-of select="./@xlink:href" />
      </xsl:attribute>

      <!-- Create the attributes for a Bootstrap tooltip to hold the title -->
      <xsl:if test="./@xlink:title">
        <xsl:attribute name="data-toggle">tooltip</xsl:attribute>
        <xsl:attribute name="data-placement">top</xsl:attribute>
        <xsl:attribute name="title"><xsl:value-of select="./@xlink:title" /></xsl:attribute>
      </xsl:if>

      <xsl:value-of select="./text()" />
    </xsl:element>
  </xsl:template>

  <xsl:template match="gmd:URL">
    <xsl:variable name="url"><xsl:value-of select="./text()" /></xsl:variable>
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:value-of select="$url" />
      </xsl:attribute>
      <xsl:value-of select="$url" />
    </xsl:element>
  </xsl:template>
</xsl:stylesheet>