<?xml version="1.0"?>
<!--
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file that is valid with respect to the eml-variable.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html" 
        encoding="UTF-8" 
        doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
        doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
        indent="yes" />
    
    <xsl:template name="text">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="section">
        <div class="sectionText">
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="para">
        <p>
            <xsl:apply-templates />
        </p>
    </xsl:template>

    <!-- This template dynamically decides on h4-h6 depending on how deeply nested its parent section element is in the
    document. -->
    <xsl:template match="title">
        <xsl:choose>
            <xsl:when test="count(ancestor::section) > 2">
                <b>
                    <xsl:apply-templates />
                </b>
            </xsl:when>
            <xsl:when test="count(ancestor::section) > 1">
                <h6>
                    <xsl:apply-templates />
                </h6>
            </xsl:when>
            <xsl:otherwise>
                <h5>
                    <xsl:apply-templates />
                </h5>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="orderedlist">
        <ol>
            <xsl:apply-templates />
        </ol>
    </xsl:template>

    <xsl:template match="itemizedlist">
        <ul>
            <xsl:apply-templates />
        </ul>
    </xsl:template>

    <xsl:template match="listitem">
        <li>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template match="emphasis">
        <em>
            <xsl:apply-templates />
        </em>
    </xsl:template>

    <xsl:template match="literalLayout">
        <pre>
            <xsl:apply-templates />
        </pre>
    </xsl:template>

    <!-- Note: This template doesn't fully support ulink in that a single ulink can have multiple citetitle's and
    translations and this template only uses the first citetitle and disregards internationalization details.-->
    <xsl:template match="ulink">
        <xsl:element name="a">
            <xsl:attribute name="href">
                <xsl:value-of select="./@url" />
            </xsl:attribute>
            <xsl:attribute name="target">_default</xsl:attribute>
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>

    <xsl:template match="superscript">
        <sup>
            <xsl:apply-templates />
        </sup>
    </xsl:template>

    <xsl:template match="subscript">
        <sub>
            <xsl:apply-templates />
        </sub>
    </xsl:template>

    <xsl:template match="value">
        <xsl:if test="./text() != ''">
            <p class="translation">
                <xsl:if test="./@xml:lang != ''">
                    (<xsl:value-of select="./@xml:lang"/>)
                </xsl:if>
                <xsl:value-of select="./text()"/>
            </p>
        </xsl:if>
    </xsl:template>

    <xsl:template match="markdown">
    <!-- Here we use a <pre> element so clients that have either not enabled JS
    or disabled whichever JS library/ies we use to render Markdown in a pretty
    fashion still see something half reasonable because most browsers respect
    whitespace like newlines in <pre> elements -->
        <xsl:element name="pre">
            <xsl:attribute name="class">markdown</xsl:attribute>
            <xsl:value-of select="." />
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
