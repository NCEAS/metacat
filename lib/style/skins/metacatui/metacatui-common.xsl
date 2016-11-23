<?xml version="1.0" encoding="UTF-8" ?>
<!--
	*  '$RCSfile$'
	*      Authors: Matt Jones, Chad Berkley
	*    Copyright: 2000-2007 Regents of the University of California and the
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
