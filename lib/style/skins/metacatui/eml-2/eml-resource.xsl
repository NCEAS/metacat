<?xml version="1.0"?>
<!--
  *  '$RCSfile$'
  *      Authors: Matthew Brooke
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
  <!--<xsl:import href="eml-party.xsl"/>
  <xsl:import href="eml-distribution.xsl"/>
  <xsl:import href="eml-coverage.xsl"/>-->
  <xsl:output method="html" encoding="UTF-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
    indent="yes" />  

  <!-- This module is for resouce and it is self-contained (it is table)-->
  <xsl:template name="resource">
    <xsl:param name="resfirstColStyle"/>
    <xsl:param name="ressubHeaderStyle"/>
    <xsl:param name="creator">Data Set Creator(s)</xsl:param>
  </xsl:template>

  <!-- style the alternate identifier elements -->
  <xsl:template name="resourcealternateIdentifier" >
      <xsl:param name="system"/>
      <xsl:param name="resfirstColStyle"/>
      <xsl:param name="ressecondColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
      <div class="control-group">
      	<label class="control-label">Alternate Identifier</label>
        <div class="controls controls-well">
        	<xsl:call-template name="i18n">
       			<xsl:with-param name="i18nElement" select="."/>
       		</xsl:call-template>
            <xsl:if test="normalize-space($system)!=''">
                &#160;(<xsl:value-of select="$system"/>)
            </xsl:if>
        </div>
      </div>
      </xsl:if>
  </xsl:template>


  <!-- style the short name elements -->
  <xsl:template name="resourceshortName">
      <xsl:param name="resfirstColStyle"/>
      <xsl:param name="ressecondColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
      <div class="control-group">
      	<label class="control-label">Short Name</label>
        <div class="controls controls-well">
        	<xsl:call-template name="i18n">
       			<xsl:with-param name="i18nElement" select="."/>
       		</xsl:call-template>
        </div>
      </div>
      </xsl:if>
  </xsl:template>


  <!-- style the title element -->
  <xsl:template name="resourcetitle" >
      <xsl:param name="resfirstColStyle"/>
      <xsl:param name="ressecondColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
      <div class="control-group hidden">
      	<label class="control-label">Title</label>
        <div class="controls controls-well">
          <strong>
       		<xsl:call-template name="i18n">
       			<xsl:with-param name="i18nElement" select="."/>
       		</xsl:call-template>	
          </strong>
        </div>
      </div>
      </xsl:if>
  </xsl:template>

  <xsl:template name="resourcecreator" >
      <xsl:param name="resfirstColStyle"/>
       <xsl:call-template name="party">
              <xsl:with-param name="partyfirstColStyle" select="$resfirstColStyle"/>
       </xsl:call-template>
   </xsl:template>

  <xsl:template name="resourcemetadataProvider" >
      <xsl:param name="resfirstColStyle"/>
      <xsl:call-template name="party">
            <xsl:with-param name="partyfirstColStyle" select="$resfirstColStyle"/>
      </xsl:call-template>
  </xsl:template>

  <xsl:template name="resourceassociatedParty">
      <xsl:param name="resfirstColStyle"/>
      <xsl:call-template name="party">
          <xsl:with-param name="partyfirstColStyle" select="$resfirstColStyle"/>
      </xsl:call-template>
  </xsl:template>


  <xsl:template name="resourcepubDate">
      <xsl:param name="resfirstColStyle"/>
      <xsl:if test="normalize-space(../pubDate)!=''">
      	<div class="control-group">
      		<label class="control-label">Publication Date</label>
      		<div class="controls controls-well">
        		<xsl:value-of select="../pubDate"/>
      		</div>
      	</div>	
      </xsl:if>
  </xsl:template>


  <xsl:template name="resourcelanguage">
      <xsl:param name="resfirstColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
      <div class="control-group">
      		<label class="control-label">Language</label>
        	<div class="controls controls-well">
	        	<xsl:call-template name="i18n">
	       			<xsl:with-param name="i18nElement" select="."/>
	       		</xsl:call-template>
        	</div>
        </div>
      </xsl:if>
  </xsl:template>


  <xsl:template name="resourceseries">
      <xsl:param name="resfirstColStyle"/>
      <xsl:if test="normalize-space(../series)!=''">
      <div class="control-group">
      		<label class="control-label">Series</label>
      		<div class="controls controls-well">
        		<xsl:value-of select="../series"/>
        	</div>
        </div>
      </xsl:if>
  </xsl:template>


  <xsl:template name="resourceabstract">
     <xsl:param name="resfirstColStyle"/>
     <xsl:param name="ressecondColStyle"/>
     <div class="control-group">
       <label class="control-label">Abstract</label>
       <div class="controls controls-well">
         <xsl:call-template name="text">
           <xsl:with-param name="textfirstColStyle" select="$resfirstColStyle"/>
           <xsl:with-param name="textsecondColStyle" select="$ressecondColStyle"/>
         </xsl:call-template>
       </div>
     </div>
  </xsl:template>

  <xsl:template name="resourcekeywordSet">
		
		<xsl:if test="normalize-space(keyword)!=''">
  			<p>
  				<xsl:value-of select="keywordThesaurus"/>
  			</p>
  			<div>
	    		<table class="table table-striped table-condensed">
		        	<thead>
		        		<tr>
		        			<th>Keyword</th>
		        			<th>Type</th>
		        		</tr>
		        	</thead>
		          <xsl:for-each select="keyword">
		            <tr>
		            	<td>
			            	<xsl:call-template name="i18n">
				       			<xsl:with-param name="i18nElement" select="."/>
				       		</xsl:call-template>
				       	</td>
				       	<td>	
				            <xsl:if test="./@keywordType and normalize-space(./@keywordType)!=''">
				              <xsl:value-of select="./@keywordType"/>
				            </xsl:if>
			            </td>
		            </tr>
		          </xsl:for-each>
		        </table>
    		</div>
      			
	        
        </xsl:if>
		
  </xsl:template>

   <xsl:template name="resourceadditionalInfo">
     <xsl:param name="ressubHeaderStyle"/>
     <xsl:param name="resfirstColStyle"/>
     <div class="control-group">
      		<label class="control-label">Additional Information</label>
      		<div class="controls controls-well">
				<xsl:call-template name="text">
					<xsl:with-param name="textfirstColStyle" select="$resfirstColStyle"/>
				</xsl:call-template>
      		</div>
         </div>
  </xsl:template>


   <xsl:template name="resourceintellectualRights">
     <xsl:param name="resfirstColStyle"/>
     <xsl:param name="ressecondColStyle"/>
     <div class="control-group">
		<label class="control-label">Intellectual Rights</label>
		<div class="controls controls-well">
	       <xsl:call-template name="text">
	         <xsl:with-param name="textsecondColStyle" select="$ressecondColStyle"/>
	       </xsl:call-template>
		</div>
	</div>   
  </xsl:template>

   <xsl:template name="resourcedistribution">
     <xsl:param name="ressubHeaderStyle"/>
     <xsl:param name="resfirstColStyle"/>
     <xsl:param name="index"/>
     <xsl:param name="docid"/>
        <xsl:call-template name="distribution">
          <xsl:with-param name="disfirstColStyle" select="$resfirstColStyle"/>
          <xsl:with-param name="dissubHeaderStyle" select="$ressubHeaderStyle"/>
          <xsl:with-param name="level">toplevel</xsl:with-param>
          <xsl:with-param name="distributionindex" select="$index"/>
          <xsl:with-param name="docid" select="$docid"/>
        </xsl:call-template>
  </xsl:template>

  <xsl:template name="resourcecoverage">
     <xsl:param name="ressubHeaderStyle"/>
     <xsl:param name="resfirstColStyle"/>
        <xsl:call-template name="coverage">
        </xsl:call-template>
  </xsl:template>
  
	<!-- for displaying any nested translation element for i18nNonEmptyString type -->
	<xsl:template name="i18n">
		<xsl:param name="i18nElement"/>
		<!-- the primary value -->
		<xsl:if test="$i18nElement/text() != ''">
			<xsl:if test="./@xml:lang != ''">
				(<xsl:value-of select="./@xml:lang"/>)
			</xsl:if>
			<xsl:value-of select="$i18nElement/."/>
		</xsl:if>
		<!-- any translations -->
		<xsl:if test="count($i18nElement/value) > 0">
			<br/>
			<xsl:for-each select="$i18nElement/value">
				<xsl:if test="./@xml:lang != ''">
					(<xsl:value-of select="./@xml:lang"/>)
				</xsl:if>
				<xsl:value-of select="."/>
				<xsl:if test="position() != last()">
					<br/>
				</xsl:if>	
			</xsl:for-each>
		</xsl:if>
	</xsl:template>


</xsl:stylesheet>
