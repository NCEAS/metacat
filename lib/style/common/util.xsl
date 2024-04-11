<?xml version="1.0"?>
<!--
	* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
	* convert an XML file showing the resultset of a query
	* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="html" />
	<xsl:variable name ="colon" select="':'"/>
    <xsl:variable name ="dot" select="'.'"/>
	
	<!--Template to extract docid from lsid string. The last three parts of lisd (separated by colon) are docid-->
    <xsl:template name="extractDocidFromLsid">
        <xsl:param name="lsidString" />
        <xsl:choose>
            <xsl:when test="contains($lsidString,$colon)">
                <xsl:variable name="subString" select="substring-after($lsidString,$colon)" />
                <xsl:choose>
                    <xsl:when test="contains($subString,$colon)">
                        <xsl:variable name="subString2" select="substring-after($subString,$colon)" />
                        <xsl:choose>
                            <xsl:when test="contains($subString2,$colon)">
                                <xsl:call-template name="extractDocidFromLsid">
                                    <xsl:with-param name="lsidString" select="$subString" />
                                </xsl:call-template>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:variable name="docidWithoutEmptyString" select='normalize-space($lsidString)'/>
                                <xsl:value-of select='translate($docidWithoutEmptyString, $colon, $dot)' />
                                <!--<xsl:value-of select="$lsidString" />-->
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>          
                </xsl:choose>       
            </xsl:when>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
