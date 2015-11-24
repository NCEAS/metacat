<?xml version="1.0"?>
<!--
	*  '$RCSfile$'
	*      Authors: Matt Jones, CHad Berkley
	*    Copyright: 2000 Regents of the University of California and the
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
	version="1.0">

	<xsl:output method="html" />
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	<xsl:template match="/">
		<html>
			<head>
				<title>Search Results</title>
				<link rel="stylesheet" type="text/css"
					href="/metacat/style/skins/{$qformat}/{$qformat}.css" />
				<script language="Javascript" type="text/JavaScript"
					src="/metacat/style/skins/{$qformat}/{$qformat}.js" />
				<script language="Javascript" type="text/JavaScript"
					src="/metacat/style/common/branding.js" />
				<script language="JavaScript">
					<![CDATA[
					function submitform(action,form_ref) {
						form_ref.action.value=action;
						form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
						form_ref.qformat.value="]]><xsl:value-of select="$qformat" /><![CDATA[";
						form_ref.submit();
					}
					]]>
				</script>
			</head>

			<body leftmargin="0" topmargin="0" marginwidth="0"
				marginheight="0">
				<script language="JavaScript">
					insertTemplateOpening('<xsl:value-of select="$contextURL" />'); insertSearchBox('<xsl:value-of select="$contextURL" />');
				</script>

				<table style="width:100%;" align="center" border="0"
					cellpadding="5" cellspacing="0">
					<tr>
						<td align="left">
							<br />
							<p class="emphasis">
								<xsl:number
									value="count(resultset/document)" />
								total records found
							</p>
						</td>
					</tr>
				</table>

				<table align="center" border="0"
					cellpadding="0" cellspacing="0" style="width:98%;">
					<tr valign="top">
						<!-- ASSESSMENTS HERE  -->
						<xsl:if test="count(resultset/document[docname='edml']) &gt; 0">
							<td>
								<xsl:call-template name="assessmentResults"/>
							</td>	
						</xsl:if>
						
						<!-- QUESTIONS HERE  -->
						<xsl:if test="count(resultset/document[docname!='edml']) &gt; 0">
							<td>
								<xsl:call-template name="questionResults"/>
							</td>	
						</xsl:if>
					</tr>	
				</table>
				
				<script language="JavaScript">
					insertTemplateClosing('<xsl:value-of select="$contextURL" />');
				</script>
			</body>
		</html>
	</xsl:template>
	
	<xsl:template name="assessmentResults">
		<table style="width:95%;" align="center" border="0"
			cellpadding="0" cellspacing="0">
			<tr>
				<th class="tablehead" colspan="5">
					Assessments
				</th>
			</tr>	
			<tr>
				<th	style="text-align: left">
					Assessment
				</th>
				<th	style="text-align: left">
					Course
				</th>
				<th	style="text-align: left">
					Instructor[s]
				</th>
				<th	style="text-align: left">
					Organization[s]
				</th>
				<th	style="text-align: left">
					Keywords
				</th>
			</tr>
	
			<xsl:for-each
				select="resultset/document[docname='edml']">
				<xsl:sort
					select="./param[@name='dataset/title']" />
				<tr valign="top" class="subpanel">
					<xsl:attribute name="class">
	              			<xsl:choose>
						    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
						    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
						</xsl:choose>
					</xsl:attribute>
	
					<td class="text_plain">
						<form action="/metacat/metacat"
							method="POST">
							<xsl:attribute name="name">
								<xsl:value-of select="translate(./docid, '()-.', '____')" />
							</xsl:attribute>
	
							<input type="hidden"
								name="qformat" />
							<input type="hidden"
								name="sessionid" />
							<input type="hidden"
								name="action" value="read" />
							<input type="hidden"
								name="docid">
								<xsl:attribute name="value">
									<xsl:value-of select="./docid" />
								</xsl:attribute>
							</input>
							<xsl:for-each
								select="./relation">
								<input type="hidden"
									name="docid">
									<xsl:attribute name="value">
										<xsl:value-of select="./relationdoc" />
									</xsl:attribute>
								</input>
							</xsl:for-each>
	
							<a>
								<xsl:attribute name="href">javascript:submitform('read',document.<xsl:value-of	select="translate(./docid, '()-.', '____')" />)</xsl:attribute>
								<xsl:text>&#187;&#160;</xsl:text>
								<xsl:value-of select="./param[@name='assessment/title']"/>
							</a>
							<br />
							<br />
							<p>
								<pre>ID: <xsl:value-of select="./docid" /></pre>
							</p>
	
						</form>
					</td>
	
					<td class="text_plain">
						<xsl:value-of
							select="./param[@name='lom/general/title/string']" />
						<xsl:value-of
							select="./param[@name='lom/general/description/string']" />
					</td>
					<td class="text_plain">
						<xsl:for-each
							select="./param[@name='individualName/surName']">
							<xsl:value-of select="." />
							<br />
						</xsl:for-each>
					</td>
					<td class="text_plain">
						<xsl:for-each
							select="./param[@name='organizationName']">
							<xsl:value-of select="." />
							<br />
						</xsl:for-each>
					</td>
	
					<td class="text_plain">
						<xsl:for-each
							select="./param[@name='keyword']">
							<xsl:value-of select="." />
							<br />
						</xsl:for-each>
						<xsl:for-each
							select="./param[@name='lom/general/keyword/string']">
							<xsl:value-of select="." />
							<br />
						</xsl:for-each>
	
					</td>
	
				</tr>
				<tr class="searchresultsdivider">
					<td colspan="5">
						<img
							src="/metacat/style/skins/default/images/transparent1x1.gif"
							width="1" height="1" />
					</td>
				</tr>
	
			</xsl:for-each>
		</table>
	</xsl:template>
	
	<xsl:template name="questionResults">
		<table style="width:95%;" align="left" border="0" cellpadding="0"
			cellspacing="0">
			
			<tr>
				<th class="tablehead" colspan="3">
					Questions
				</th>
			</tr>	
			<tr>
				<th	style="text-align: left">
					Identifier
				</th>
				<th	style="text-align: left">
					Content
				</th>
				<th	style="text-align: left">
					Metadata/Keywords
				</th>
			</tr>

			<xsl:for-each select="resultset/document[docname!='edml']">
				<xsl:sort select="./param[@name='item/@title']" />
				<tr valign="top">
					<xsl:attribute name="class">
						<xsl:choose>
							<xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
							<xsl:when test="position() mod 2 = 0">roweven</xsl:when>
						</xsl:choose>
					</xsl:attribute>

					<td class="text_plain">
						<a>
							<xsl:attribute name="href">javascript:submitform('read',document.<xsl:value-of select="translate(./docid, '()-.', '____')" />)</xsl:attribute>
							<xsl:text>&#187;&#160;</xsl:text>
							<xsl:value-of select="./param[@name='item/@title']" />
							<!-- <xsl:value-of select="./param[@name='item/@ident']" />-->
						</a>
						(<xsl:value-of select="./docid" />)
						<form action="{$contextURL}/metacat" method="POST">
							<xsl:attribute name="name">
								<xsl:value-of select="translate(./docid, '()-.', '____')" />
							</xsl:attribute>

							<input type="hidden" name="qformat" value="first"/>
							<input type="hidden" name="sessionid" />
							<xsl:if	test="$enableediting = 'true'">
								<input type="hidden"
									name="enableediting" value="{$enableediting}" />
							</xsl:if>
							<input type="hidden" name="action" value="read" />
							<input type="hidden" name="docid">
								<xsl:attribute name="value">
									<xsl:value-of select="./docid" />
								</xsl:attribute>
							</input>
							<xsl:for-each select="./relation">
								<input type="hidden" name="docid">
									<xsl:attribute name="value">
										<xsl:value-of select="./relationdoc" />
									</xsl:attribute>
								</input>
							</xsl:for-each>
						</form>	
					</td>
					
					<td class="text_plain">
						<xsl:value-of select="./param[@name='presentation/flow/response_lid/render_choice/material/mattext']"/>
						
						<ul>
						<xsl:for-each
							select="./param[@name='response_label/material/mattext']">
							<li>
								<xsl:value-of select="." />
							</li>	
						</xsl:for-each>
						</ul>
					</td>
					
					<td>
						<table>
							<tr>
								<td class="text_plain" colspan="2">
									Metadata:
								</td>
							</tr>
							<tr>
								<td class="text_plain">
									<xsl:for-each
										select="./param[@name='fieldlabel']">
										<xsl:value-of select="." />
										<br />
									</xsl:for-each>
								</td>
								
								<td class="text_plain">
									<xsl:for-each
										select="./param[@name='fieldentry']">
										<xsl:value-of select="." />
										<br />
									</xsl:for-each>
								</td>
							</tr>
						</table>
					</td>

				</tr>
				<!-- 
				<tr>
					<td class="text_plain">
						<xsl:for-each
							select="./param[@name='objectives/material/mattext']">
							<xsl:value-of select="." />
							<br />
						</xsl:for-each>
					</td>
				</tr>
				-->
				<tr class="searchresultsdivider">
					<td colspan="3">
						<img
							src="{$contextURL}/style/skins/default/images/transparent1x1.gif"
							width="1" height="1" />
					</td>
				</tr>

			</xsl:for-each>
		</table>
	
	</xsl:template>

</xsl:stylesheet>