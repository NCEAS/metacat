<?xml version="1.0"?>
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
				xmlns:eml="eml://ecoinformatics.org/eml-2.1.0"
				version="1.0">
	<xsl:import href="./eml-2/eml.xsl"/>
	<xsl:include href="./emltojson-ld.xsl" />
	<xsl:output method="html" />
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	<xsl:param name="cgi-prefix"/>

	<xsl:template match="/">

		<html>

			<xsl:call-template name="documenthead"/>

			<body id="Overview">

				<div id="main_wrapper">

					<div id="content_wrapper">

						<xsl:if test="*[local-name()='eml']">
							<xsl:call-template name="emldocument"/>
						</xsl:if>

					</div>
				</div>

			</body>

		</html>

	</xsl:template>

	<xsl:template name="documenthead">
		<head>
			<title>DataONE Dataset
				<xsl:if test="*[local-name()='eml']">
					<xsl:text>: </xsl:text><xsl:value-of select="/eml:eml/dataset/title"/>
				</xsl:if>
			</title>
			<style>
				<xsl:value-of select="document('./styling/default.css')/*"/>
			</style>
			<script type="application/ld+json">
				<xsl:call-template name="jsonld">
					<xsl:with-param name="science-meta" select="./*"/>
				</xsl:call-template>
			</script>
		</head>
	</xsl:template>

	<xsl:template name="emldocument">
		<table xsl:use-attribute-sets="cellspacing" width="100%"
			   class="{$mainContainerTableStyle}">
			<tr>
				<td>
					<xsl:apply-templates select="*[local-name()='eml']"/>
				</td>
			</tr>
		</table>
	</xsl:template>

</xsl:stylesheet>
