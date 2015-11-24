<?xml version="1.0"?>
<!--
	*	 '$RCSfile$'
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
    <xsl:import href="../../common/resultset-table.xsl"/>
    <!-- xsl:import href="../../common/eml-2.0.1/emlroot.xsl"/ -->
    <xsl:import href="../../common/eml-2/emlroot.xsl"/>
    <xsl:import href="../../common/fgdc/fgdc_1.xsl"/>

	<xsl:output method="html" />
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	
	<xsl:template match="/">
		<html>
		
			<xsl:call-template name="documenthead"/>
			
			<body id="Overview">
			
				<div id="main_wrapper">
				
					<table class="templatecontentareaclass" cellpadding="0" cellspacing="0" border="0">
						<tr>
							<td colspan="3">
								<!--  header -->
							    <xsl:call-template name="bodyheader"/>
							</td>
						</tr>
						
						<tr>
							<td class="templateleftcolclass">
								<!-- left nav -->
								<xsl:call-template  name="leftnav" />						
							</td>
							
							<td valign="top">
								<!-- main content -->
								<div id="content_wrapper">
			                   		 
			                   		<xsl:if test="*[local-name()='eml']">     	
			                            <xsl:call-template name="emldocument"/>
			                        </xsl:if>
			                         
			                        <xsl:if test="*[local-name()='metadata']">     	
			                            <xsl:call-template name="fgdcdocument"/>
			                        </xsl:if>
			                        
			                        <xsl:if test="*[local-name()='resultset']">     	
			                            <xsl:call-template name="resultstable"/>
			                        </xsl:if>

								</div><!-- id="content_wrapper"-->
								
							</td>
							<td>
								<!-- right content if needed -->
							</td>
						</tr>
						<tr class="footerContent">
							<td colspan="3">
								<!-- footer -->
								<xsl:call-template name="bodyfooter"/>
							</td>
						</tr>
					</table>			
								
				</div><!-- id="main_wraper"-->
				
			</body>
			
		</html>
	</xsl:template>
	
	<xsl:template name="documenthead">
		<head>
			<title>SANParks
			<xsl:if test="*[local-name()='eml']">     	
            	<xsl:text>: </xsl:text><xsl:value-of select="/eml:eml/dataset/title"/>
            </xsl:if>
            </title>
                        
			<script language="Javascript" type="text/JavaScript"
				src="{$contextURL}style/skins/{$qformat}/{$qformat}.js" />
			<script language="Javascript" type="text/JavaScript"
				src="{$contextURL}/style/common/branding.js" />
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
	
	<xsl:template name="leftnav">
		<!--  left nav -->
		<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0" align="center">
			<!-- Left Nav -->
			<tr class="leftnav" valign="top">
				<td class="headermenu">Parks A-Z</td>
			</tr>
			<tr class="leftnav" valign="top">
				<td>
					<form action="{$contextURL}/style/skins/{$qformat}/index.jspx" method="post" target="_top" name="orgForm">
						<input name="organizationScope" type="hidden" value="" />
						<table>
							<tr>
								<td>
									<div id="nav">
									<ul class="level-1">
										<li>
											<a href="#" onclick="orgForm.organizationScope.value='';orgForm.submit();">All</a>
										</li>
										<li>
											<a href="#" onclick="orgForm.organizationScope.value='SANParks, South Africa';orgForm.submit();">SANParks</a>
											<ul class="level-2">
												<li><a href="#" onclick="orgForm.organizationScope.value='SANParks, South Africa';orgForm.submit();">All</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Addo National Park, South Africa';orgForm.submit();">Addo National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Agulhas National Park, South Africa';orgForm.submit();">Agulhas National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Augrabies Falls National Park, South Africa';orgForm.submit();">Augrabies Falls National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Bontebok National Park, South Africa';orgForm.submit();">Bontebok National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Camdeboo National Park, South Africa';orgForm.submit();">Camdeboo National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Golden Gate National Park, South Africa';orgForm.submit();">Golden Gate National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Karoo National Park, South Africa';orgForm.submit();">Karoo National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Knysna National Lake Area, South Africa';orgForm.submit();">Knysna National Lake Area</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Kruger National Park, South Africa';orgForm.submit();">Kruger National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Mapungubwe National Park, South Africa';orgForm.submit();">Mapungubwe National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Marakele National Park, South Africa';orgForm.submit();">Marakele National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Mokala National Park, South Africa';orgForm.submit();">Mokala National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Mountain Zebra National Park, South Africa';orgForm.submit();">Mountain Zebra National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Namaqua National Park, South Africa';orgForm.submit();">Namaqua National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Richtersveld National Park, South Africa';orgForm.submit();">Richtersveld National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Table Mountain National Park, South Africa';orgForm.submit();">Table Mountain National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Tankwa Karoo National Park, South Africa';orgForm.submit();">Tankwa Karoo National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Tsitsikamma National Park, South Africa';orgForm.submit();">Tsitsikamma National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='West Coast National Park, South Africa';orgForm.submit();">West Coast National Park</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Wilderness National Park, South Africa';orgForm.submit();">Wilderness National Park</a></li>
											</ul>
										</li>
										<li>
											<a href="#" onclick="orgForm.organizationScope.value='SAEON, South Africa';orgForm.submit();">SAEON</a>
											<ul class="level-2">
												<li><a href="#" onclick="orgForm.organizationScope.value='SAEON, South Africa';orgForm.submit();">All</a></li>
												<li><a href="#" onclick="orgForm.organizationScope.value='Elwandle SAEON Node, South Africa';orgForm.submit();">Elwandle SAEON Node</a></li>										
												<li><a href="#" onclick="orgForm.organizationScope.value='Ndlovu SAEON Node, South Africa';orgForm.submit();">Ndlovu SAEON Node</a></li>
											</ul>
										</li>
									</ul>		
									</div>
								</td>
							</tr>
						</table>			
					</form>	
				</td>
			</tr>
		</table>
		<!-- end left nav -->
	
	</xsl:template>
	
	<xsl:template name="bodyheader">
	
		<div id="header">
			<table class="iframeheaderclass" width="100%" height="100%" border="0" cellspacing="0" cellpadding="0" align="center">
				<!-- Header -->
				<tr>
					<td style="background: #124325;">
						<a href="http://www.SANParks.org/" title="SANParks.org Home">
							<img border="0" src="{$contextURL}/style/skins/{$qformat}/images/logofade.jpg" alt="SANParks.org Home" />
						</a>
					</td>
					<td align="center" nowrap="nowrap" width="100%">
						<table width="100%" border="0" cellspacing="0" cellpadding="0">
							<tr height="92">
								<td colspan="2" nowrap="nowrap">
									<span class="headertitle">
										SANParks
										<br/>
										South African National Park
										<br />
										Data Repository
									</span>
								</td>
								<td align="right">
									<!-- 
									<form name="loginForm" method="post" action="./" 
										onsubmit="return allowSubmit(this)">
										<input name="qformat" type="hidden" value="sanparks" />
										<table border="0" cellspacing="1" cellpadding="0">
											<tr>
												<td class="login">
													<span class="required">User name</span>	
												</td>
												<td class="login">
													<input class="login" name="username" type="text" value="" />
												</td>
												<td></td>
											</tr>
											<tr>
												<td class="login"><span class="required">Password</span></td>
												<td class="login">
													<input class="login" name="password" value="" type="password" maxlength="50" />
												</td>
												<td></td>
											</tr>
											<tr>
												<td class="login"><span class="required">Organization</span></td>
												<td class="login"><select class="login" name="organization">
													<option value="SANParks" selected="">SANParks</option>
													<option value="SAEON">SAEON</option>
													<option value="NCEAS">NCEAS</option>
													<option value="unaffiliated">unaffiliated</option>
												</select></td>
												<td class="login">
													<input name="action" 
													value="Login" type="submit" class="login" style="width: 50px;"/>
												</td>
											</tr>
										</table>
									</form>
									-->
								</td>
							</tr>
							<tr valign="bottom" height="20" bgcolor="#124325">
								<td nowrap="nowrap">
									<a href="{$contextURL}/style/skins/{$qformat}/index.jspx" target="_top" class="headermenu">Repository Home</a>
								</td>
								<td nowrap="nowrap">
									<a href="LINK HERE" class="headermenu">Register Data</a>
								</td>
								<td>
									<a href="LINK HERE" class="headermenu">Request an account</a>
								</td>
							</tr>
						</table>
					</td>
					<td valign="bottom" align="right">
						<!-- <img src='images/lilies.jpg' alt='Lilies' /> -->
						<img src='{$contextURL}/style/skins/{$qformat}/images/giraffe.jpg' alt='Giraffe' />
					</td>
				</tr>
			</table>
		</div>
		
	</xsl:template>
    
    <xsl:template name="emldocument">
    	<table xsl:use-attribute-sets="cellspacing" width="100%"
               class="{$mainContainerTableStyle}">
            <xsl:apply-templates select="*[local-name()='eml']"/>
        </table>
    </xsl:template>
    
    <xsl:template name="fgdcdocument">
    	<table xsl:use-attribute-sets="cellspacing" width="100%"
               class="{$mainContainerTableStyle}">
            <xsl:apply-templates select="*[local-name()='metadata']"/>
        </table>
    </xsl:template>

	<xsl:template name="bodyfooter">
		<div id="footer">
			<p>
                This project is a cooperative effort of the
                <a href="http://www.sanparks.org/">South African National Parks (SANParks)</a> and the 
                <a href="http://www.nceas.ucsb.edu">National Center for Ecological Analysis and Synthesis (NCEAS)</a>.
                The Data Repository is based on software developed by the 
                <a href="http://knb.ecoinformatics.org">Knowledge Network for Biocomplexity (KNB)</a>,
                and houses metadata that are compliant with 
                <a href="http://knb.ecoinformatics.org/software/eml/">Ecological Metadata Language (EML)</a>
                and the <a href="http://www.fgdc.gov/metadata">Federal Geographic Data Committee (FGDC)</a> specification.
                Other sites contributing to the KNB repository include:
                <ul>
                    <li>
                        <a href="http://knb.ecoinformatics.org/knb/obfs">Organization of Biological Field Stations registry</a>  
                    </li>
                    <li>
                        <a href="http://knb.ecoinformatics.org/knb/style/skins/nrs">UC Natural Reserve System registry</a>  
                    </li>
                    <li>
                        <a href="http://knb.ecoinformatics.org/knb/style/skins/nceas">NCEAS registry</a>  
                    </li>
                    <li>
                        <a href="http://knb.ecoinformatics.org/knb/style/skins/specnet">SpecNet registry</a>  
                    </li>
                </ul>
            </p>
		</div>
	</xsl:template>

</xsl:stylesheet>
