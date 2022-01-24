<?xml version="1.0"?>
<!--
  *  '$RCSfile$'
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
  * convert an XML file that is valid with respect to the eml-variable.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html" encoding="UTF-8"
              doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
              doctype-system="http://www.w3.org/TR/html4/loose.dtd"
              indent="yes" />  

   <xsl:template name="nonNumericDomain">
     <xsl:param name="nondomainfirstColStyle"/>
        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:call-template name="nonNumericDomainCommon">
             <xsl:with-param name="nondomainfirstColStyle" select="$nondomainfirstColStyle"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="nonNumericDomainCommon">
             <xsl:with-param name="nondomainfirstColStyle" select="$nondomainfirstColStyle"/>
           </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>


  <xsl:template name="nonNumericDomainCommon">
    <xsl:param name="nondomainfirstColStyle"/>
    <xsl:for-each select="enumeratedDomain">
      <xsl:call-template name="enumeratedDomain">
        <xsl:with-param name="nondomainfirstColStyle" select="$nondomainfirstColStyle"/>
      </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="textDomain">
      <xsl:call-template name="enumeratedDomain">
        <xsl:with-param name="nondomainfirstColStyle" select="$nondomainfirstColStyle"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="textDomain">
       <xsl:param name="nondomainfirstColStyle"/>
       <p>Text Domain</p>
       <div class="control-group">
			<label class="control-label">Definition</label>
            <div class="controls controls-well">
            	<xsl:value-of select="definition"/>
            </div>
        </div>
        <xsl:for-each select="pattern">
          <div class="control-group">
			<label class="control-label">Pattern</label>
            <div class="controls controls-well">
            	<xsl:value-of select="."/>
            </div>
          </div>
        </xsl:for-each>
        <xsl:if test="source">
          <div class="control-group">
			<label class="control-label">Source</label>
            <div class="controls controls-well">
            	<xsl:value-of select="source"/>
            </div>
          </div>
        </xsl:if>
  </xsl:template>

  <xsl:template name="enumeratedDomain">
     <xsl:param name="nondomainfirstColStyle"/>
     <xsl:if test="codeDefinition">
        <p>Enumerated Domain</p>
        	<thead>
        		<tr>
        			<th>Code</th>
        			<th>Definition</th>
        			<th>Source</th>
        		</tr>
        	</thead>
        	<xsl:for-each select="codeDefinition">
		      	<tr>
					<td class="{$secondColStyle}"><xsl:value-of select="code"/></td>
					<td class="{$secondColStyle}"><xsl:value-of select="definition"/></td>
					<td class="{$secondColStyle}"><xsl:value-of select="source"/></td>
				</tr>
			</xsl:for-each>
     </xsl:if>
     <xsl:if test="externalCodeSet">
     	<p>Enumerated Domain (External Set)</p>
        <div class="control-group">
			<label class="control-label">Set Name</label>
			<div class="controls controls-well">
				<xsl:value-of select="externalCodeSet/codesetName"/>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label">Citation</label>
			<div class="controls controls-well">
		        <xsl:for-each select="externalCodeSet/citation">
		           <xsl:call-template name="citation">
		                      <xsl:with-param name="citationfirstColStyle" select="$nondomainfirstColStyle"/>
		                   </xsl:call-template>
		        </xsl:for-each>
	        </div>
	    </div>
	    <div class="control-group">
			<label class="control-label">URL</label>
			<div class="controls controls-well"> 
		        <xsl:for-each select="externalCodeSet/codesetURL">
		        	<a><xsl:attribute name="href"><xsl:value-of select="."/></xsl:attribute><xsl:value-of select="."/></a>
		        </xsl:for-each>
	        </div>
        </div>
     </xsl:if>
     <xsl:if test="entityCodeList">
        <p>Enumerated Domain (Data Object)</p>
        <div class="control-group">
			<label class="control-label">Data Object Reference</label>
            <div class="controls controls-well">
            	<xsl:value-of select="entityCodeList/entityReference"/>
            </div>
		</div>
       <div class="control-group">
			<label class="control-label">Attribute Value Reference</label>
            <div class="controls controls-well">
            	<xsl:value-of select="entityCodeList/valueAttributeReference"/>
            </div>
       </div>
       <div class="control-group">
			<label class="control-label">Attribute Definition Reference</label>
            <div class="controls controls-well">
            	<xsl:value-of select="entityCodeList/definitionAttributeReference"/>
            </div>
       </div>
     </xsl:if>

  </xsl:template>


</xsl:stylesheet>
