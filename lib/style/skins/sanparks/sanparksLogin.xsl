<?xml version="1.0"?>
<!--
  *  '$RCSfile$'
  *      Authors: Jivka Bojilova
  *    Copyright: 2000 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *  For Details: http://www.nceas.ucsb.edu/
  *
  *   '$Author: leinfelder $'
  *     '$Date: 2008-06-17 13:16:32 -0700 (Tue, 17 Jun 2008) $'
  * '$Revision: 4006 $'
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
  * convert an XML file with information about login action
  * into an HTML format suitable for rendering with modern web browsers.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

 <xsl:output method="html"/>
 <xsl:param name="contextURL"/>
 <xsl:param name="servletURL"/>
 <xsl:param name="cgi-prefix"/>
 <xsl:param name="qformat">sanparks</xsl:param>
 <xsl:param name="redirect">true</xsl:param>
 <xsl:template match="/">
  	<xsl:choose>
  	<xsl:when test="count(login) &gt; 0">
  		<xsl:choose>
  			<xsl:when test="$redirect = 'true'">
  				<script language="JavaScript">
					<![CDATA[
			            function redirect() {
			                location.href = "]]><xsl:value-of select="$cgi-prefix" /><![CDATA[/register-dataset.cgi?cfg=sanparks";
			            }
			            redirect();]]>
		         </script>
  			</xsl:when>
  			<xsl:otherwise>
  			
				<h3>Welcome, <xsl:value-of select="login/name"/>  </h3>
				<form name="logoutForm" onsubmit="submitLogoutFormIntoDiv('{$servletURL}', this, 'loginSection')">
					<input name="qformat" value="sanparks" type="hidden" />
					<input name="action" type="hidden" value="logout"/>
					<table>
						<tr valign="top">
							<td><p class="regtext">You are currently logged in.</p></td>
							<td align="right">
								<input name="action" type="submit" value="Logout" class="button_login" />
							</td>
						</tr>
						<tr valign="top">
							<td colspan="2"><p class="regtext">(<xsl:value-of select="login/message"/>)</p></td>
						</tr>	
						<tr>	
							<td colspan="2" class="regtext" align="center" valign="top">				
								<!-- reset pass --> 
								<a href="<%=USER_MANAGEMENT_URL%>" target="_parent">
									reset your password
								</a>
								|
								<!-- change pass --> 
								<a href="<%=USER_MANAGEMENT_URL%>" target="_parent">
									change your password
								</a>
							</td>
						</tr>
					</table>
				</form>
				
				<!-- File Upload Form --> 
				<br/>		
				<h3>Data Package Upload</h3>
				
				<table width="100%">
					<tr valign="top">
						<td align="right">
							<form method="post" action="{$contextURL}/style/skins/sanparks/upload.jsp">
								<input type="submit" value="Go >" class="button_login" />
							</form>
						</td>
					</tr>
				</table>
			</xsl:otherwise>
		</xsl:choose>		
	</xsl:when>
  	<xsl:otherwise>
		<h3>Login
		<a href="<%=USER_MANAGEMENT_URL%>" target="_new">
			<span class="regtext"> (request an account...)</span>
		</a>
		</h3>
		<form name="loginForm" id="loginForm" onsubmit="submitLoginFormIntoDivAndReload('{$servletURL}', this, 'loginSection')">
			<input name="qformat" type="hidden" value="sanparks" />
			<input name="action" type="hidden" value="login"/>
			<table>
				<tr valign="top">
					<td><span class="required">User name</span></td>
					<td><input name="shortusername" type="text" value="" style="width: 140" /></td>
					<td><input name="username" type="hidden" value="" /></td>
					
				</tr>
				<tr>
					<td><span class="required">Organization</span></td>
					<td><select name="organization" style="width: 140">
						<option value="SANParks" selected="">SANParks</option>
						<option value="SAEON">SAEON</option>
						<option value="NCEAS">NCEAS</option>
						<option value="unaffiliated">unaffiliated</option>
						</select></td>
				</tr>
				<tr>
					<td><span class="required">Password</span></td>
					<td><input name="password" value="" type="password"
						style="width: 140" maxlength="50" /></td>
				</tr>
				<tr>
					<td colspan="2" align="center">
						<input name="action" value="login" type="submit" class="button_login" />
					</td>
				</tr>
			</table>
		</form>
		
		<!-- File Upload Form --> 
		<br/>		
		<h3>Data Package Upload</h3>
		<p class="regtext">You must be logged into your user account before uploading a data set.</p>
		
	</xsl:otherwise>

	</xsl:choose>
  </xsl:template>
</xsl:stylesheet>

