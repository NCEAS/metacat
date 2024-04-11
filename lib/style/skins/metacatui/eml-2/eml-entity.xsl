<?xml version="1.0"?>
<!--
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file that is valid with respect to the eml-file.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
 

  <xsl:output method="html" encoding="UTF-8"
              doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
              doctype-system="http://www.w3.org/TR/html4/loose.dtd"
              indent="yes" />  
  <!-- This module only provide some templates. They can be called by other templates-->


  <xsl:template name="entityName">
    <xsl:param name="entityfirstColStyle"/>
    <div class="control-group">
		<label class="control-label">Entity Name</label>
		<div class="controls controls-well">
    		<strong><xsl:value-of select="."/></strong>
    	</div>
    </div>
  </xsl:template>
  
  <xsl:template name="entityalternateIdentifier">
     <xsl:param name="entityfirstColStyle"/>
     <div class="control-group">
		<label class="control-label">Alternate Identifier</label>
		<div class="controls controls-well">
            <xsl:value-of select="."/>
        </div>
     </div>
  </xsl:template>
  
  <xsl:template name="entityDescription">
      <xsl:param name="entityfirstColStyle"/> 
      <div class="control-group">
		<label class="control-label">Description</label>
		<div class="controls controls-well">
      		<xsl:value-of select="."/>
      	</div>
      </div>
  </xsl:template>
  
  <xsl:template name="entityadditionalInfo">
      <xsl:param name="entityfirstColStyle"/> 
      <div class="control-group">
		<label class="control-label">Additional Info</label>
		<div>
	        <xsl:call-template name="text"/>
	     </div>
	  </div>
  </xsl:template>
  

</xsl:stylesheet>
