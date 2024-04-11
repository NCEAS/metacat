<?xml version="1.0"?>
<!--
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file that is valid with respect to the eml-variable.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html" encoding="UTF-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
    indent="yes" />  
    
    <!-- style the identifier and system -->
    <xsl:template name="identifier">
      <xsl:param name="IDfirstColStyle"/>
      <xsl:param name="IDsecondColStyle"/>
      <xsl:param name="packageID"/>
      <xsl:param name="system"/>
      <xsl:if test="normalize-space(.)">
        <div class="control-group">
          <label class="control-label">Identifier</label>
          <div class="controls">
          	<div class="controls-well">
          		<xsl:value-of select="$packageID"/>
          	</div>
			<xsl:if test="$withHTMLLinks = '1'">
	          	<!-- stats loaded with ajax call -->
				<span id="stats"></span>
				<script language="JavaScript">
					if (window.loadStats) {
						loadStats(
							'stats', 
							'<xsl:value-of select="$packageID" />', 
							'<xsl:value-of select="$contextURL" />/metacat',
							'<xsl:value-of select="$qformat" />');
					}
				</script>
			</xsl:if>
			
          </div>
        </div>
      </xsl:if>
    </xsl:template>
    
    <!-- for citation information -->
    <xsl:template name="datasetcitation">
    
    	<cite class="citation">
    				<xsl:for-each select="creator">
    				
    					<xsl:choose>
							<xsl:when test="references!=''">
							 <xsl:variable name="ref_id" select="references"/>
							 <xsl:variable name="references" select="$ids[@id=$ref_id]" />
							 <xsl:for-each select="$references">
							 
							 	<!-- process the reference -->
							 	<xsl:if test="position() &gt; 1">
				        			<xsl:if test="last() &gt; 2">, </xsl:if>
				        			<xsl:if test="position() = last()"> and</xsl:if>
				        			<xsl:text> </xsl:text>
				        		</xsl:if>
				        		<xsl:call-template name="creatorCitation" />
				        		<xsl:if test="position() = last()">.</xsl:if>
				        		<xsl:text> </xsl:text>
				        		
							 </xsl:for-each>
							</xsl:when>
							<xsl:otherwise>
							
								<!-- just the creator element -->
								<xsl:if test="position() &gt; 1">
				        			<xsl:if test="last() &gt; 2">, </xsl:if>
				        			<xsl:if test="position() = last()"> and</xsl:if>
				        			<xsl:text> </xsl:text>
				        		</xsl:if>
				        		<xsl:call-template name="creatorCitation" />
				        		<xsl:if test="position() = last()">.</xsl:if>
				        		<xsl:text> </xsl:text>
				        		
							</xsl:otherwise>
						</xsl:choose>
		        		 		
		        	</xsl:for-each>
		        	
		        	<xsl:value-of select="substring(string(pubDate),1,4)"/>
		        	<xsl:if test="substring(string(pubDate),1,4) != ''">
		        		<xsl:text>. </xsl:text>    				     		
		        	</xsl:if>
		        	
		        	<!-- title -->
					<strong>
					<xsl:for-each select="./title">
			     		<xsl:call-template name="i18n">
			     			<xsl:with-param name="i18nElement" select="."/>
			     		</xsl:call-template>
		        		<xsl:text> </xsl:text>    				     		
			     	</xsl:for-each>				
					</strong>
					
					<!-- show link? -->
					<xsl:if test="$withHTMLLinks = '1'">
						<a id="viewMetadataCitationLink"> 
							<xsl:attribute name="href">
								<!--<xsl:value-of select="$viewURI"/><xsl:value-of select="$pid"/>-->
								<!-- <xsl:text>#view/</xsl:text><xsl:value-of select="$pid"/> -->
							</xsl:attribute>
							<!-- (<xsl:value-of select="$pid"/>) -->
						</a>				        		
					</xsl:if>
		</cite>
   					<a class="btn" id="downloadPackage">
   						<xsl:attribute name="href">
							<!-- <xsl:value-of select="$packageURI"/><xsl:value-of select="$pid"/> -->
						</xsl:attribute>
   						Download <i class="icon-arrow-down"></i>
   					</a>
   		<div id="downloadContents"></div>

   </xsl:template>
   
   <!--************** creates lsid dataset id **************-->
   <xsl:template name="lsid">
		<xsl:variable name="lsidString1" select="concat('urn:lsid:',string($lsidauthority),':')"/>
		<xsl:variable name="lsidString2" select="concat($lsidString1, substring-before(string(../@packageId),'.'), ':')"/>
		<xsl:variable name="lsidString3" select="concat($lsidString2, substring-before(substring-after(string(../@packageId),'.'),'.'), ':')"/>
		<xsl:variable name="lsidString4" select="concat($lsidString3, substring-after(substring-after(string(../@packageId),'.'),'.'))"/>
		<xsl:value-of select="$lsidString4"/>
   </xsl:template>
   
   <!--************** creates citation for a creator in "Last FM" format **************-->
   <xsl:template name="creatorCitation">
	   	<xsl:for-each select="individualName">	
	   		
	   		<xsl:value-of select="surName/text()"/>
	   		<xsl:text> </xsl:text>
	   		
	   		<xsl:for-each select="givenName">
	   			<xsl:value-of select="substring(string(.),1,1)"/>
	   		</xsl:for-each>
	   	</xsl:for-each>
	   	
	   	<!-- only show organization if the person is omitted  -->
	   	<xsl:if test="string(individualName/surName) = ''"> 
		   	<xsl:for-each select="organizationName">
		   		<xsl:value-of select="."/>
		   	</xsl:for-each>
	   	</xsl:if>
	   	
   </xsl:template>
    
 </xsl:stylesheet>
