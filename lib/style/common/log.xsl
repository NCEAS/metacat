<?xml version="1.0"?>
	<!--
		* '$RCSfile$' * Authors: Jivka Bojilova * Copyright: 2000 Regents of
		the University of California and the * National Center for Ecological
		Analysis and Synthesis * For Details: http://www.nceas.ucsb.edu/ * *
		'$Author$' * '$Date: 2008-06-17 13:29:31 -0700 (Tue, 17
		Jun 2008) $' * '$Revision$' * * This program is free software;
		you can redistribute it and/or modify * it under the terms of the GNU
		General Public License as published by * the Free Software Foundation;
		either version 2 of the License, or * (at your option) any later
		version. * * This program is distributed in the hope that it will be
		useful, * but WITHOUT ANY WARRANTY; without even the implied warranty
		of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
		GNU General Public License for more details. * * You should have
		received a copy of the GNU General Public License * along with this
		program; if not, write to the Free Software * Foundation, Inc., 59
		Temple Place, Suite 330, Boston, MA 02111-1307 USA * * This is an XSLT
		(http://www.w3.org/TR/xslt) stylesheet designed to * convert an XML
		file with information about login action * into an HTML format
		suitable for rendering with modern web browsers.
	-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">
	<xsl:output method="html" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="contextURL" />
	<xsl:param name="cgi-prefix" />
	<xsl:param name="simple">true</xsl:param>
	
		
	<xsl:key name="docIds" match="/log/logEntry/docid/text()" use="." />
	
	<xsl:template match="/">
		<html>
			<head>
				<link rel="stylesheet" type="text/css"
					href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />
				<script language="Javascript" type="text/JavaScript"
					src="{$contextURL}/style/skins/{$qformat}/{$qformat}.js" />
				<script language="Javascript" type="text/JavaScript"
					src="{$contextURL}/style/common/branding.js" />
			</head>

			<body>
			
				<xsl:choose>
					<xsl:when test="$simple='true'">
						<xsl:for-each select="./log/logEntry/docid/text()[generate-id(.)=generate-id(key('docIds',.)[1])]">
							<xsl:variable name="uniqueDocid" select="."/>
							Views: <xsl:value-of select="count(//log/logEntry[docid=$uniqueDocid and event='read'])" />														
						</xsl:for-each>						
					</xsl:when>
					<xsl:otherwise>
						<table class="subGroup subGroup_border">
							
							<xsl:for-each select="./log/logEntry/docid/text()[generate-id()=generate-id(key('docIds',.)[1])]">
								<xsl:variable name="uniqueDocid" select="string(.)"/>
								<tr>
									<th colspan="2">
										Usage Statistics
									</th>
								</tr>
								<tr>
									<td>
										Doc Id:
									</td>
									<td>	 
										<xsl:value-of select="$uniqueDocid" />										
									</td>
								</tr>
								<tr>
									<td>
										Read:
									</td>
									<td>	 
										<xsl:value-of select="count(//log/logEntry[docid=$uniqueDocid and string(event)='read'])" />
									</td>
								</tr>
								<tr>
									<td>									
										Insert: 
									</td>
									<td>	
										<xsl:value-of select="count(//log/logEntry[docid=$uniqueDocid and string(event)='insert'])" />
									</td>
								</tr>
								<tr>
									<td>
										Update: 
									</td>
									<td>	
										<xsl:value-of select="count(//log/logEntry[docid=$uniqueDocid and string(event)='update'])" />
									</td>
								</tr>
								<tr>
									<td>
										Delete: 
									</td>
									<td>	
										<xsl:value-of select="count(//log/logEntry[docid=$uniqueDocid and event/text() = 'delete'])" />
									</td>
								</tr>
								<tr>
									<td>
										Other: 
									</td>
									<td>	
										<xsl:value-of select="count(//log/logEntry[docid=$uniqueDocid and (event/text() != 'read' and event/text() != 'insert' and event/text() != 'update' and event/text() != 'delete')])" />
									</td>
								</tr>
								<tr>
									<td>
										Total: 
									</td>
									<td>	
										<xsl:value-of select="count(//log/logEntry[docid=$uniqueDocid])" />
									</td>
								</tr>
								
							</xsl:for-each>
						
						</table>
					</xsl:otherwise>
				</xsl:choose>	
			</body>
			
		</html>
	</xsl:template>
</xsl:stylesheet>