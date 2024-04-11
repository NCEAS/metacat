<?xml version="1.0"?>
<!--
	* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
	* convert an XML file showing the resultset of a query
	* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:eml="eml://ecoinformatics.org/eml-2.1.1"
                version="1.0">

    <!-- import some XSLTs and use them -->
    <xsl:import href="../../common/resultset-table.xsl"/>
    <xsl:import href="../../common/resultset-table-solr.xsl"/>
    <xsl:import href="./fgdc/fgdc-root.xsl"/>
    <xsl:import href="../../common/dcx/onedcx-common.xsl"/>
    <xsl:import href="./eml-2/emlroot.xsl"/>
    <xsl:import href="./iso19115/isoroot.xsl"/>
    <xsl:import href="metacatui-common.xsl"/>

    <!-- default the pid parameter to the docid in cases where pid is not given -->
    <xsl:param name="pid">
        <xsl:value-of select="$docid"/>
    </xsl:param>
    <xsl:param name="d1BaseURI"><xsl:value-of select="$contextURL"/><![CDATA[/d1/mn/v1]]>
    </xsl:param>
    <xsl:param name="objectURI"><xsl:value-of select="$d1BaseURI"/><![CDATA[/object/]]>
    </xsl:param>
    <xsl:param name="packageURI"><xsl:value-of select="$d1BaseURI"/><![CDATA[/package/]]>
    </xsl:param>
    <xsl:param name="viewURI"><xsl:value-of select="$d1BaseURI"/><![CDATA[/views/]]><xsl:value-of select="$qformat"/><![CDATA[/]]>
    </xsl:param>

    <xsl:template match="/">
        <html>

            <xsl:call-template name="documenthead"/>

            <body id="metacatui-app">

                <!-- call some templates here to get things started -->
                <xsl:call-template name="bodyheader"/>

                <xsl:if test="*[local-name()='eml']">
                    <xsl:call-template name="emldocument"/>
                </xsl:if>

                <xsl:if test="*[local-name()='MD_Metadata'] | *[local-name()='MI_Metadata']">
                    <xsl:call-template name="isodocument"/>
                </xsl:if>

                <xsl:if test="*[local-name()='response']">
                    <xsl:call-template name="resultstablesolr"/>
                </xsl:if>

                <xsl:if test="*[local-name()='resultset']">
                    <xsl:call-template name="resultstable"/>
                </xsl:if>

                <!--  multiple possible metadata elements -->
                <xsl:if test="*[local-name()='metadata']">
                    <xsl:choose>
                        <xsl:when test="namespace-uri(*)='http://ns.dataone.org/metadata/schema/onedcx/v1.0'">
                            <xsl:call-template name="onedcx"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="metadata"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>

                <xsl:call-template name="bodyfooter"/>

            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>