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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:output method="html" />
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	
	<xsl:template match="/">
		<script language="JavaScript">
          <![CDATA[
          submitItemForm = function(action,docid,formId) {
          		var form_ref = document.getElementById(formId);
              form_ref.action.value=action;
              form_ref.docid.value=docid;
              //form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
              //form_ref.qformat.value="]]><xsl:value-of select="$qformat" /><![CDATA[";
              form_ref.qformat.value="first";
              form_ref.submit();
          }

          ]]>
        </script>
        
        <p class="emphasis">
			<xsl:number value="count(resultset/document)" /> items found
		</p>

		<!-- This tests to see if there are returned documents,
			if there are not then don't show the query results -->
		<xsl:if test="count(resultset/document) &gt; 0">

			<form action="{$contextURL}/metacat" method="POST">
				<xsl:attribute name="id">assessmentForm</xsl:attribute>
				<input type="hidden" name="qformat" value="first"/>
				<input type="hidden" name="sessionid">
					<xsl:attribute name="value">
						<xsl:value-of select="$sessid" />
					</xsl:attribute>	
				</input>
				<input type="hidden" name="action" value="read" />
				<input type="hidden" name="docid"/>
			</form>
			<!-- hidden form for the itemids -->
			<form action="{$contextURL}/metacat" method="POST" name="itemIdForm" id="itemIdForm">
			<table width="95%" align="left" border="0" cellpadding="0"
				cellspacing="0">

				<xsl:for-each select="resultset/document">
					<xsl:sort select="./param[@name='item/@title']" />
					<tr valign="top">
						<xsl:attribute name="class">
							<xsl:choose>
								<xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
								<xsl:when test="position() mod 2 = 0">roweven</xsl:when>
							</xsl:choose>
						</xsl:attribute>

						<td class="text_plain">
							<!--
							<a>
								<xsl:attribute name="href">javascript:submitItemForm('read','<xsl:value-of select="./docid" />', 'assessmentForm')</xsl:attribute>
								<xsl:text>&#187;&#160;</xsl:text>
								<xsl:value-of select="./param[@name='item/@title']" />
							</a>
							-->
							<a>
								<xsl:attribute name="href">
									<xsl:value-of select="$contextURL" />
									<xsl:text>/metacat?action=read</xsl:text>
									<xsl:text>&amp;docid=</xsl:text>
									<xsl:value-of select="./docid" />
									<xsl:text>&amp;qformat=first</xsl:text>
									<xsl:text>&amp;sessionid=</xsl:text>
									<xsl:value-of select="$sessid" />
								</xsl:attribute>	
								<xsl:value-of select="./param[@name='item/@title']" />
							</a>
							(<xsl:value-of select="./docid" />)
							<input name="itemIds" id="itemIds" type="hidden" >
								<xsl:attribute name="value">
									<xsl:value-of select="./docid" />
								</xsl:attribute>	
							</input>
								
						</td>
						
						<td class="text_plain">
							<xsl:for-each select="./param[@name='presentation/flow/response_lid/render_choice/material/mattext']">
								<xsl:value-of select="." disable-output-escaping="yes"/>
								<br/>
							</xsl:for-each>	
							<xsl:for-each select="./param[@name='presentation/flow/response_str/material/mattext']">
								<xsl:value-of select="." disable-output-escaping="yes"/>
								<br/>
							</xsl:for-each>
							<xsl:for-each select="./param[@name='presentation/flow/material/mattext']">
								<xsl:value-of select="." disable-output-escaping="yes"/>
								<br/>
							</xsl:for-each>
							<ul>
							<xsl:for-each
								select="./param[@name='response_label/material/mattext']">
								<xsl:sort select="."/>
								<li>
									<xsl:value-of select="." disable-output-escaping="yes"/>
								</li>	
							</xsl:for-each>
							</ul>
						</td>
						
						<td>
							<table>
								<tr valign="top">
									<td class="text_plain" colspan="2">
										Metadata
									</td>
								</tr>
								<tr valign="top">
									<td class="text_plain" colspan="2">
										Vocabulary:
										<xsl:for-each
											select="./param[@name='vocabulary']">
											<xsl:value-of select="." />
											<br />
										</xsl:for-each>
									</td>
								</tr>
								<tr valign="top">
									<td class="text_plain" nowrap="nowrap">
										<xsl:for-each
											select="./param[@name='fieldlabel']">
											<xsl:value-of select="." />:
											<br />
										</xsl:for-each>
									</td>
									
									<td class="text_plain" nowrap="nowrap">
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
			</form>

		</xsl:if>
	</xsl:template>

</xsl:stylesheet>