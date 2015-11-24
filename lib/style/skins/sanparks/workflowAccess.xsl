<?xml version="1.0"?>
<!--
	*  '$RCSfile$'
	*      Authors: Michael Daigle
	*    Copyright: 2009 Regents of the University of California and the
	*               National Center for Ecological Analysis and Synthesis
	*  For Details: http://www.nceas.ucsb.edu/
	*
	*   '$Author: leinfelder $'
	*     '$Date: 2009-04-02 14:20:05 -0800 (Thu, 02 Apr 2009) $'
	* '$Revision: 4893 $'
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
  <xsl:import href="../../common/util.xsl"/>
	<xsl:output method="html" />
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL" />
	<xsl:param name="servletURL" />
	<xsl:param name="workflowname" />
	<xsl:param name="docid" />
	<xsl:variable name="karDocid">
              <xsl:call-template name="extractDocidFromLsid">
                <xsl:with-param name="lsidString" select="$docid" />
              </xsl:call-template>
   </xsl:variable>
	
	<xsl:template match="/">  
 	
		<div class="content-subsection">
			<div class="content-subsection-header">Access Rules</div>
			<div class="row row-header" >
				<div class="col access_order_col_header">Access Order</div>
				<div class="col access_type_col_header">Access Type</div>
				<div class="col principal_col_header">Principal</div>
				<div class="col permissions_col_header">Permission</div>
			</div>
			<xsl:choose>
			<xsl:when test="/access/@order = 'denyFirst'">
				<xsl:for-each select="/access/allow | /access/deny">
					<div>
						<xsl:attribute name="class">
							<xsl:choose>
								<xsl:when test="position() mod 2 = 1">row row-odd</xsl:when>
								<xsl:when test="position() mod 2 = 0">row row-even</xsl:when>
							</xsl:choose>
						</xsl:attribute>
						<div class="col access_order_col">
							<xsl:value-of select="../@order" />
						</div>
						<div class="col access_type_col">
							<xsl:value-of select="local-name(.)" />
						</div>
						<div class="col principal_col">
							<xsl:value-of select="./principal"/>
						</div>	
						<div class="col permissions_col">
							<xsl:for-each select="./permission">
								<div class="permission_element">
									<xsl:if test=". != 'all'">
										<xsl:value-of select="."/>
									</xsl:if>
								</div>
							</xsl:for-each>
						</div>
					</div>
				</xsl:for-each>			
			</xsl:when>

			<xsl:when test="acl/resource/@order = 'allowFirst'">
				<xsl:for-each select="acl/resource/*/principal">
					<div>
						<xsl:attribute name="class">
							<xsl:choose>
								<xsl:when test="position() mod 2 = 1">row row-odd</xsl:when>
								<xsl:when test="position() mod 2 = 0">row row-even</xsl:when>
							</xsl:choose>
						</xsl:attribute>
						<div class="col access_order_col">denyFirst</div>
						<div class="col access_type_col">
							<xsl:value-of select="local-name(.)" />
						</div>
						<div class="col principal_col">
							<xsl:value-of select="."/>
						</div>	
						<div class="col permissions_col">
							<xsl:for-each select="../permission">
								<div class="permission_element">
									<xsl:if test=". != 'all'">
										<xsl:value-of select="."/>
									</xsl:if>
								</div>
							</xsl:for-each>
						</div>
					</div>
				</xsl:for-each>			
			</xsl:when>
			</xsl:choose>	
		</div><br/>
		<div class="content-subsection">
			<div class="content-subsection-header">Add Access Rule</div>
			<div class="change-access-row">
				<div class="col_label change_access_order_col">Access Order</div>
				<div class="col_label change_access_type_col">Access Type</div>
				<div class="col_label change_principal_col">Principal</div>
				<div class="col_label change_permissions_col">Permission</div>
			</div>
						
			<div class="change-access-row">
				<form name="addAccess" action="{$servletURL}" id="addAccess" onsubmit="setPermission(this)">
					<input name="action" value="setaccess" type="hidden" />  
					<input name="qformat" value="sanparks" type="hidden" />
					<input name="forwardto" value="workflowAccessMain.jsp" type="hidden" />
					<input name="permission" id="permission" type="hidden" />
					<input name="docid" value="{$karDocid}" type="hidden" />
					<input name="karfilelsid" value="{$docid}" type="hidden" />
					<input name="workflowname" value="{$workflowname}" type="hidden" />

					<xsl:choose>
					<xsl:when test="acl/resource/@order = 'allowFirst'">
						<input name="permOrder" value="allowFirst" type="hidden" />
						<div class="change_access_order_col access_info_text" name="permOrder" id="permOrder">allowFirst</div>
					</xsl:when>
					<xsl:when test="acl/resource/@order = 'denyFirst'">
						<input name="permOrder" value="denyFirst" type="hidden" />
						<div class="change_access_order_col access_info_text" name="permOrder" id="permOrder">denyFirst</div>
					</xsl:when>
					<xsl:otherwise>
						<select class="change_access_order_col" name="permOrder" id="permOrder">
							<option value="denyFirst">Deny First</option>
							<option value="allowFirst">Allow First</option>
						</select>  
					</xsl:otherwise>
					</xsl:choose> 
					<select class="change_access_type_col" name="permType" id="permType">
						<option value="allow">Allow</option>
						<option value="deny">Deny</option>
					</select>   
					<input class="change_principal_col" name="principal" id="principal" type="text" /> 
       				<div class="change_permissions_col">
       					<input type="checkbox" name="permission_read_checkbox" id="permission_read_checkbox">Read</input>
       					<input type="checkbox" name="permission_write_checkbox" id="permission_write_checkbox">Write</input>
       					<input type="checkbox" name="permission_chmod_checkbox" id="permission_chmod_checkbox">Chmod</input>
       					<!-- input type="checkbox" name="permission_all_checkbox" id="permission_all_checkbox">All</input -->
       				</div>
       		
       				<br/>
       				<input class="access-submit-button" value="Add" type="submit"/>

				</form>
			</div>
		</div>
		
	</xsl:template>

</xsl:stylesheet>