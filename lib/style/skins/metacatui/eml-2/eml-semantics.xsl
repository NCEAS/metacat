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

    <xsl:template name="emlannotations">
        <h4>Annotations</h4>
        <xsl:call-template name="emlannotationtable">
            <xsl:with-param name="annotations" select="annotations/annotation" />
        </xsl:call-template>
    </xsl:template>
    <xsl:template name="emlannotationtag">
        <div>
            Annotation: <xsl:value-of select="./propertyURI" /> (<xsl:value-of select="./propertyURI/@label" />) <xsl:value-of select="./valueURI" /> (<xsl:value-of select="./valueURI/@label" />)
        </div>
    </xsl:template>

    <!-- This uses an $annotations to render each annotation instead of relying on an XPath
         so the template can easily support annotations grouped under an <annotations> parent
         element and also annotations under another parent element (e.g., dataset) -->
    <xsl:template name="emlannotationtable">
        <xsl:param name="annotations" />
    	<table class="table table-striped table-condensed">
            <thead>
                <tr>
                    <th>Property</th>
                    <th>Value</th>
                </tr>
                </thead>
            <tbody>
                <xsl:for-each select="$annotations">
                <tr>
                    <td>
                        <xsl:element name="span">
                            <xsl:attribute name="class">annotation</xsl:attribute>
                            <xsl:attribute name="data-uri"><xsl:value-of select="normalize-space(./propertyURI/text())" /></xsl:attribute>
                            <xsl:attribute name="data-label"><xsl:value-of select="normalize-space(./propertyURI/@label)" /></xsl:attribute>
                            <xsl:value-of select="normalize-space(./propertyURI/@label)" /> (<xsl:value-of select="normalize-space(./propertyURI/text())" />)
                        </xsl:element>
                    </td>
                    <td>
                        <xsl:element name="span">
                            <xsl:attribute name="class">annotation</xsl:attribute>
                            <xsl:attribute name="data-uri"><xsl:value-of select="normalize-space(./valueURI/text())" /></xsl:attribute>
                            <xsl:attribute name="data-label"><xsl:value-of select="normalize-space(./valueURI/@label)" /></xsl:attribute>
                            <xsl:value-of select="normalize-space(./valueURI/@label)" /> (<xsl:value-of select="normalize-space(./valueURI/text())" />)
                        </xsl:element>
                    </td>
                </tr>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>
</xsl:stylesheet>
