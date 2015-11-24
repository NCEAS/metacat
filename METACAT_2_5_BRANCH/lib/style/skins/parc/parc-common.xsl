<?xml version="1.0"?>
<!--
	*  '$RCSfile$'
	*      Authors: Matt Jones, Chad Berkley
	*    Copyright: 2000-2007 Regents of the University of California and the
	*               National Center for Ecological Analysis and Synthesis
	*  For Details: http://www.nceas.ucsb.edu/
	*
	*   '$Author: leinfelder $'
	*     '$Date: 2008-02-27 09:43:25 -0800 (Wed, 27 Feb 2008) $'
	* '$Revision: 3742 $'
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

	<xsl:output method="html" />
	<xsl:param name="sessid" />
	<xsl:param name="qformat">parc</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	<xsl:param name="cgi-prefix"/>
	
	<xsl:template name="documenthead">
		<head>
			<title>PARC Data Repository
			<xsl:if test="*[local-name()='eml']">     	
            	<xsl:text>: </xsl:text><xsl:value-of select="/eml:eml/dataset/title"/>
            </xsl:if>
            </title>
                        
			<link rel="stylesheet" type="text/css"
				href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />
			<script language="Javascript" type="text/JavaScript"
				src="{$contextURL}/style/skins/{$qformat}/{$qformat}.js" />
			<script language="Javascript" type="text/JavaScript"
				src="{$contextURL}/style/common/branding.js" />
			<script type="text/javascript"
				src="{$contextURL}/style/skins/nceas/navigation.js">
			</script>
			<link rel="stylesheet" type="text/css"
				href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />

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
	</xsl:template>
	
	<xsl:template name="bodyheader">
		<div id="header">
		</div>
		<div id="navigation">
			<ul id="main_nav">

				<ul class="menu">
					<li class="collapsed">
						<a href="http://www.palmyraresearch.org">Home
						</a>
					</li>
					<li class="collapsed">
						<a href="{$contextURL}">
						   Repository</a>
					</li>
					<li class="collapsed">
						<a href="{$cgi-prefix}/register-dataset.cgi?cfg=parc">
						   Register</a>
					</li>
          <li class="collapsed">
            <span id="login_block"></span>
          </li>
				</ul>
			</ul>
		</div><!-- navigation -->
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

	<xsl:template name="bodyfooter">
		<div id="footer">
		</div>
	</xsl:template>

</xsl:stylesheet>
