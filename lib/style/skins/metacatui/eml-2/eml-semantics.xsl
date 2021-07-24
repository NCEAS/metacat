<?xml version="1.0"?>
<!--
  *  '$RCSfile$'
  *      Authors: Bryce Mecum <mecum@nceas.ucsb.edu>
  *    Copyright: 2019 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *  For Details: http://www.nceas.ucsb.edu/
  *
  *   '$Author$'
  *     '$Date$'
  * '$Revision$'
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  *
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
