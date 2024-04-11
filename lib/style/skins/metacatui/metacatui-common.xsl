<?xml version="1.0" encoding="UTF-8" ?>
<!--
	* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
	* convert an XML file showing the resultset of a query
	* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:eml="eml://ecoinformatics.org/eml-2.0.1" 
	            version="1.0"> 

	<xsl:output method="html" encoding="UTF-8"/>
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	<xsl:param name="cgi-prefix"/>
	
	<xsl:template name="documenthead">
		<head>
		  <title></title>
		<!-- no content to render for head -->
		</head>
	</xsl:template>
	
	<xsl:template name="bodyheader">
		<!-- no content to render for body header -->
	</xsl:template>
    
	<xsl:template name="emldocument">
		<!-- CONTENT SECTION
		======================================================================= -->    
		<article id="Metadata" class="container">
			<xsl:apply-templates select="*[local-name()='eml']"/>
		</article>  		
	</xsl:template>

	<xsl:template name="isodocument">
		<article id="Metadata" class="container">
			<xsl:apply-templates select="*[local-name()='MD_Metadata'] | *[local-name()='MI_Metadata']"/>
		</article>  			
	</xsl:template>

	<xsl:template name="bodyfooter">
		<!-- no content to render for body footer -->
	</xsl:template>

</xsl:stylesheet>
