<?xml version="1.0"?>
<!--
	*  '$RCSfile$'
	*      Authors: Matt Jones, CHad Berkley
	*    Copyright: 2009 Regents of the University of California and the
	*               National Center for Ecological Analysis and Synthesis
	*  For Details: http://www.nceas.ucsb.edu/
	*
	*   '$Author: leinfelder $'
	*     '$Date: 2009-04-02 14:20:05 -0800 (Thu, 02 Apr 2009) $'
	* '$Revision: 4893 $'
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
