<?xml version="1.0"?>
<!--
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file that is valid with respect to the eml-variable.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:output method="html"
        encoding="UTF-8"
        doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
        doctype-system="http://www.w3.org/TR/html4/loose.dtd"
        indent="yes" />

    <xsl:template name="annotation">
        <xsl:param name="context" />
        <xsl:element name="div">
            <!-- The attributes on this element are redundant with the attributes on the direct children but were kept
            for backwards compatibility with older versions of MetacatUI. -->
            <xsl:attribute name="class">annotation</xsl:attribute>
            <xsl:attribute name="data-html">true</xsl:attribute>
            <xsl:attribute name="title"><xsl:value-of select="normalize-space(./valueURI/@label)" /></xsl:attribute>
            <xsl:attribute name="data-placement">bottom</xsl:attribute>
            <xsl:attribute name="data-content"><xsl:value-of select="concat(normalize-space(./propertyURI/@label), ' ', normalize-space(./valueURI/@label)) " /></xsl:attribute>
            <xsl:attribute name="data-context"><xsl:value-of select="$context" /></xsl:attribute>
            <xsl:attribute name="data-property-label"><xsl:value-of select="normalize-space(./propertyURI/@label)" /></xsl:attribute>
            <xsl:attribute name="data-property-uri"><xsl:value-of select="normalize-space(./propertyURI/text())" /></xsl:attribute>
            <xsl:attribute name="data-value-label"><xsl:value-of select="normalize-space(./valueURI/@label)" /></xsl:attribute>
            <xsl:attribute name="data-value-uri"><xsl:value-of select="normalize-space(./valueURI/text())" /></xsl:attribute>
            <div class="annotation-property">
                <xsl:attribute name="title"><xsl:value-of select="normalize-space(./propertyURI/@label)" /></xsl:attribute>
                <xsl:attribute name="data-label"><xsl:value-of select="normalize-space(./propertyURI/@label)" /></xsl:attribute>
                <xsl:attribute name="data-uri"><xsl:value-of select="normalize-space(./propertyURI/text())" /></xsl:attribute>
                <xsl:attribute name="data-placement">bottom</xsl:attribute>
                <xsl:attribute name="data-html">true</xsl:attribute>
                <xsl:attribute name="data-content"><xsl:value-of select="concat(normalize-space(./propertyURI/@label), ' ', normalize-space(./valueURI/@label)) " /></xsl:attribute>
                <xsl:value-of select="normalize-space(./propertyURI/@label)" />
            </div>
            <div class="annotation-value">
                <xsl:attribute name="title"><xsl:value-of select="normalize-space(./valueURI/@label)" /></xsl:attribute>
                <xsl:attribute name="data-label"><xsl:value-of select="normalize-space(./valueURI/@label)" /></xsl:attribute>
                <xsl:attribute name="data-uri"><xsl:value-of select="normalize-space(./valueURI/text())" /></xsl:attribute>
                <xsl:attribute name="data-placement">bottom</xsl:attribute>
                <xsl:attribute name="data-html">true</xsl:attribute>
                <xsl:attribute name="data-content"><xsl:value-of select="concat(normalize-space(./propertyURI/@label), ' ', normalize-space(./valueURI/@label)) " /></xsl:attribute>
                <div class="annotation-value-text">
                    <xsl:value-of select="normalize-space(./valueURI/@label)" />
                </div>
            </div>
        </xsl:element>
    </xsl:template>

    <xsl:template name="annotation-info-tooltip">
        <xsl:element name="span">
            <xsl:attribute name="style">display: inline-block; width: 1em; text-align: center;</xsl:attribute>
            <xsl:element name="i">
                <xsl:attribute name="class">tooltip-this icon icon-info-sign</xsl:attribute>
                <xsl:attribute name="data-toggle">tooltip</xsl:attribute>
                <xsl:attribute name="title">Annotations are rigorously-defined, expressive statements about portions of metadata. Each annotation represents a stand-alone, logical statement and uses terms from specific vocabularies that make it very clear what has been annotated. For more information about each annotation, click on the respective annotation to the right.</xsl:attribute>
            </xsl:element>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
