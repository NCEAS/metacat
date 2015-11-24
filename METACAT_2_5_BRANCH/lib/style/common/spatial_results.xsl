<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html" indent="yes" encoding="ISO-8859-1" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" doctype-system="http://www.w3.org/TR/html4/loose.dtd" />

    <xsl:template match="/">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<title>Metacat Spatial Query Results</title>
<!--link href="http://knb.ecoinformatics.org/knb/style/common/spatial_results.css" media="screen" rel="Stylesheet" type="text/css"/-->
<link href="http://knb.ecoinformatics.org/knb/style/skins/knb/knb.css" type="text/css" rel="stylesheet"/>
<script src="http://knb.ecoinformatics.org/knb/style/skins/knb/knb.js" type="text/JavaScript" language="Javascript"></script>
<script src="http://knb.ecoinformatics.org/knb/style/common/branding.js" type="text/JavaScript" language="Javascript"></script>

<style type="text/css">
#query_results {
    width: 70%;
}

.mc_doc { margin: 9px 3px; }
.odd { background-color: #e6e6e6; }
.even { background-color: #ccc; }

.mc_doc a,
.mc_doc a:visited {
    /* docid */
    color: #05F;
    border: 1px solid #bcbcbc;
    background-color: #fff;
    padding: 5px 15px;
    font-weight: bold;
    font-size: 13pt;
    text-decoration: none;
}
.mc_doc a:visited {
    background-color: #efefef;
    color: #7AA4FF;
}
.mc_doc a:hover {
    border-color: #05f;
    background-color: #B8CFFF;
    color: #05f;
}

.mc_doc i {
    /* creator */
    padding-left: 10px;
    color: #666;
    font-size: 10pt;
}

blockquote {
    /* abstract */
    margin: 0px;
    padding: 1px 3px 9px 18px;
    color: #333;
    font-size: 11pt;
}
</style>

</head>

<body>
    <div id="mainTableAligmentStyle">
    <script language="JavaScript">insertTemplateOpening();</script>

    <h2>Metacat Spatial Query Results</h2>
    <xsl:apply-templates />

    </div>

</body>
</html>
</xsl:template>

<xsl:template match="metacatspatialdataset">
    <div id="query_results">
        <xsl:apply-templates />
    </div>
</xsl:template>

<xsl:template match="metacatspatialdocument[position() mod 2 = 1]">
    <p class="mc_doc odd">
    <xsl:call-template name="show_doc" />
    </p>
</xsl:template>

<xsl:template match="metacatspatialdocument">
    <p class="mc_doc even">
    <xsl:call-template name="show_doc" />
    </p>
</xsl:template>

<xsl:template name="show_doc">
    <a><xsl:attribute name="href">http://nebulous.msi.ucsb.edu:9999/knp/metacat?action=read&amp;docid=<xsl:value-of select="docid"/>&amp;qformat=knb</xsl:attribute><xsl:value-of select="docid"/></a>
    <i>
	<xsl:choose>
	<xsl:when test="creator/individualName/surName = ''">
		<xsl:value-of select="creator/individualName/givenName"/>
	</xsl:when>
	<xsl:otherwise>
		<xsl:value-of select="creator/individualName/surName"/>,
		<xsl:value-of select="creator/individualName/givenName"/>
	</xsl:otherwise>
	</xsl:choose>
	</i>
    <blockquote>
	<xsl:choose>
	<xsl:when test="title = 'null'">
		<!-- no title -->	
	</xsl:when>
	<xsl:otherwise>
		<xsl:value-of select="title"/>
	</xsl:otherwise>
	</xsl:choose>
	</blockquote>
</xsl:template>

</xsl:stylesheet>
