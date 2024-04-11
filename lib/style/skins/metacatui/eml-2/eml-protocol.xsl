<?xml version="1.0"?>
<!--
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



  <xsl:template name="protocol">
    <xsl:param name="protocolfirstColStyle"/>
    <xsl:param name="protocolsubHeaderStyle"/>
    <table class="{$tabledefaultStyle}">
        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:call-template name="protocolcommon">
              <xsl:with-param name="protocolfirstColStyle" select="$protocolfirstColStyle"/>
              <xsl:with-param name="protocolsubHeaderStyle" select="$protocolsubHeaderStyle"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <xsl:call-template name="protocolcommon">
              <xsl:with-param name="protocolfirstColStyle" select="$protocolfirstColStyle"/>
              <xsl:with-param name="protocolsubHeaderStyle" select="$protocolsubHeaderStyle"/>
           </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </table>
  </xsl:template>

   <xsl:template name="protocolcommon">
        <xsl:param name="protocolfirstColStyle"/>
        <xsl:param name="protocolsubHeaderStyle"/>
        
        <!-- template for protocol shows minimum elements (author, title, dist) -->
		<xsl:call-template name="protocol_simple">
           <xsl:with-param name="protocolfirstColStyle" select="$protocolfirstColStyle"/>
           <xsl:with-param name="protocolsubHeaderStyle" select="$protocolsubHeaderStyle"/>
        </xsl:call-template>
        <xsl:for-each select="proceduralStep">
          <tr><td colspan="2" class="{$protocolsubHeaderStyle}">
              Step<xsl:text> </xsl:text><xsl:value-of select="position()"/>:
              </td>
          </tr>
          <xsl:call-template name="step">
              <xsl:with-param name="protocolfirstColStyle" select="$protocolfirstColStyle"/>
              <xsl:with-param name="protocolsubHeaderStyle" select="$protocolsubHeaderStyle"/>
          </xsl:call-template>
        </xsl:for-each>
        <xsl:call-template name="protocolAccess">
              <xsl:with-param name="protocolfirstColStyle" select="$protocolfirstColStyle"/>
              <xsl:with-param name="protocolsubHeaderStyle" select="$protocolsubHeaderStyle"/>
        </xsl:call-template>
  </xsl:template>

  <xsl:template name="step">
    <xsl:param name="protocolfirstColStyle"/>
    <xsl:param name="protocolsubHeaderStyle"/>
    <xsl:for-each select="description">
		<div class="control-group">
			<label class="control-label">Description</label>
			<div class="controls">
				<xsl:call-template name="text">
					<xsl:with-param name="textfirstColStyle" select="$protocolfirstColStyle"/>
				</xsl:call-template>
			</div>
      </div>
     </xsl:for-each>
    <xsl:for-each select="citation">
      <div class="control-group">
		<label class="control-label">Citation</label>
		<div class="controls">
          <xsl:call-template name="citation">
            <xsl:with-param name="citationfirstColStyle" select="$protocolfirstColStyle"/>
            <xsl:with-param name="citationsubHeaderStyle" select="$protocolsubHeaderStyle"/>
          </xsl:call-template>
        </div>
      </div>
    </xsl:for-each>
     <xsl:for-each select="protocol">
      <div class="control-group">
		<label class="control-label">Protocol</label>
		<div class="controls">
			<xsl:call-template name="protocol">
	            <xsl:with-param name="protocolfirstColStyle" select="$protocolfirstColStyle"/>
	            <xsl:with-param name="protocolsubHeaderStyle" select="$protocolsubHeaderStyle"/>
 			</xsl:call-template>
          </div>
      </div>
    </xsl:for-each>
    <xsl:for-each select="instrumentation">
        <div class="control-group">
			<label class="control-label">Instrument(s)</label>
          <div class="controls">
            <xsl:value-of select="."/>
          </div>
      </div>
    </xsl:for-each>
    <xsl:for-each select="software">
     <div class="control-group">
		<label class="control-label"></label>
		<div>	
          <xsl:call-template name="software">
            <xsl:with-param name="softwarefirstColStyle" select="$protocolfirstColStyle"/>
            <xsl:with-param name="softwaresubHeaderStyle" select="$protocolsubHeaderStyle"/>
          </xsl:call-template>
        </div>
      </div>
    </xsl:for-each>
    <xsl:for-each select="subStep">
		<div class="control-group">
			<label class="control-label">Substep<xsl:text> </xsl:text><xsl:value-of select="position()"/></label>
			<div class="controls">
			      <xsl:call-template name="step">
			          <xsl:with-param name="protocolfirstColStyle" select="$protocolfirstColStyle"/>
			          <xsl:with-param name="protocolsubHeaderStyle" select="$protocolsubHeaderStyle"/>
			      </xsl:call-template>
			</div>
		</div> 
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="protocolAccess">
    <xsl:param name="protocolfirstColStyle"/>
    <xsl:param name="protocolsubHeaderStyle"/>
    <xsl:for-each select="access">
      <tr><td colspan="2">
         <xsl:call-template name="access">
           <xsl:with-param name="accessfirstColStyle" select="$protocolfirstColStyle"/>
           <xsl:with-param name="accesssubHeaderStyle" select="$protocolsubHeaderStyle"/>
         </xsl:call-template>
         </td>
       </tr>
    </xsl:for-each>
  </xsl:template>
  
  <!-- this template creates a small table for a protocol tree with minimum required 
	content (title/creator/distribution). Only called in this stylesheet. It would be
	better to reuse the resource templates? but those currently are written for 
	toplevel, and that style is too prominent for this location. use modes? 
	but all calls to resource templates would be affected.
	-->
	<xsl:template name="protocol_simple">
		<xsl:param name="protocolfirstColStyle"/>
		<xsl:param name="protocolsubHeaderStyle"/>
		<!--<table class="{$tabledefaultStyle}">  -->
		
		<xsl:for-each select="creator/individualName/surName">
			<tr>	
				<td class="{$protocolfirstColStyle}">
				<xsl:text>Author: </xsl:text>
				</td>
				<td><xsl:value-of select="."/>
				</td>
			</tr>
		</xsl:for-each>
   
		<xsl:for-each select="title">
			<tr>
			  <td class="{$protocolfirstColStyle}">
			  <xsl:text>Title: </xsl:text>
			  </td>
			  <td><xsl:value-of select="."/>
			  </td>
			</tr>
		</xsl:for-each>
 
	<xsl:for-each select="distribution">
      <!--<tr>
        <td>
         the template 'distribution' in eml-distribution.2.0.0.xsl. seems to be for
				data tables. use the resourcedistribution template instead (eml-resource.2.0.0.xsl)  -->
            <xsl:call-template name="resourcedistribution">
              <xsl:with-param name="resfirstColStyle" select="$protocolfirstColStyle"/>
              <xsl:with-param name="ressubHeaderStyle" select="$protocolsubHeaderStyle"/> 
			</xsl:call-template>
		<!-- </td>
		</tr>  -->
		</xsl:for-each>
		<!-- </table> -->
	</xsl:template>
	
</xsl:stylesheet>
