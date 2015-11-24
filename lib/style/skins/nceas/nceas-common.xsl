<?xml version="1.0" encoding="UTF-8" ?>
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

	<xsl:output method="html" encoding="UTF-8"/>
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	<xsl:param name="cgi-prefix"/>
	
	<xsl:template name="documenthead">
		<head>
			<title>NCEAS Data Repository
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
			<p>Skip to <a href="#navigation">navigation</a>,
				<a href="#main_content">main content</a>,
				<a href="#secondary_content">secondary content</a>
				or to <a href="#search">search</a>.
			</p>
			<h1>
				<span></span>
				<a href="/">NCEAS</a>
			</h1>
		</div>
		<div id="navigation">
			<ul id="main_nav">

				<ul class="menu">
					<li class="collapsed">
						<a href="http://www.nceas.ucsb.edu">Home
						</a>
					</li>
					<li class="collapsed">
						<a href="http://data.nceas.ucsb.edu">
						   Repository</a>
					</li>
					<li class="collapsed">
						<a href="{$cgi-prefix}/register-dataset.cgi?cfg=nceas">
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
			<div id="footer_logos">
				<a href="http://www.msi.ucsb.edu/">
					<img src="{$contextURL}/style/skins/{$qformat}/images/logo_msi.jpg"
						alt="MSI: Marine Science Institute" height="66" width="132"/></a>
				<a href="http://www.nsf.gov/">
					<img src="{$contextURL}/style/skins/{$qformat}/images/logo_nsf.jpg"
						alt="NSF: National Science Foundation" height="66"
						width="70"/></a>
				<a href="http://www.ucsb.edu/">
					<img src="{$contextURL}/style/skins/{$qformat}/images/logo_ucsb.jpg"
						alt="UCSB: University of California at Santa Barbara"
						height="66" width="132"/></a>
			</div><!-- footer_logos -->
			<div id="footer_contact">
				<span class="contact_name">
					National Center for Ecological
					Analysis and Synthesis
				</span>

				<span class="contact_address">
					735 State Street, Suite 300, Santa
					Barbara, CA 93101
				</span>
				<span class="copyright">
					Copyright &#xA9; 2007 The Regents of
									the University of California, All
									Rights Reserved
								</span>
								<span class="copyright">
									<a href="http://www.ucsb.edu/"
										title="Visit the
UCSB website">
										UC Santa Barbara
									</a>
									, Santa Barbara CA 93106 &#x2022;
					(805) 893-8000
				</span>
				<span class="copyright">
					<a
						href="mailto:webmaster@nceas.ucsb.edu"
						title="E-mail the NCEAS webmaster">
						Contact
					</a>
					&#x2022;
					<a
						href="http://www.ucsb.edu/policies/terms-of-use.shtml">
						Terms of Use
					</a>
					&#x2022;
					<a href="http://www.nceas.ucsb.edu/accessibility"
						title="NCEAS is committed to the accessibility of its products for all users">
						Accessibility</a>
				</span>

				
			</div><!-- footer_contact -->
		</div>
	</xsl:template>

</xsl:stylesheet>
