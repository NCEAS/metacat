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
  <!-- This module is for datatable module-->

  <xsl:template name="dataTable">
      <xsl:param name="datatablefirstColStyle"/>
      <xsl:param name="datatablesubHeaderStyle"/>
      <xsl:param name="docid"/>
      <xsl:param name="entityindex"/>
      
      <!-- start the data Table -->
      <div class="control-group dataTableContainer">

        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:call-template name="datatablecommon">
             <xsl:with-param name="datatablefirstColStyle" select="$datatablefirstColStyle"/>
             <xsl:with-param name="datatablesubHeaderStyle" select="$datatablesubHeaderStyle"/>
             <xsl:with-param name="docid" select="$docid"/>
             <xsl:with-param name="entityindex" select="$entityindex"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="datatablecommon">
             <xsl:with-param name="datatablefirstColStyle" select="$datatablefirstColStyle"/>
             <xsl:with-param name="datatablesubHeaderStyle" select="$datatablesubHeaderStyle"/>
             <xsl:with-param name="docid" select="$docid"/>
             <xsl:with-param name="entityindex" select="$entityindex"/>
          </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
      
	</div>

  </xsl:template>

  <xsl:template name="datatablecommon">
    <xsl:param name="datatablefirstColStyle"/>
    <xsl:param name="datatablesubHeaderStyle"/>
    <xsl:param name="docid"/>
    <xsl:param name="entityindex"/>
    <xsl:for-each select="entityName">
       <xsl:call-template name="entityName">
          <xsl:with-param name="entityfirstColStyle" select="$datatablefirstColStyle"/>
       </xsl:call-template>
    </xsl:for-each>

    <xsl:if test="annotation">
      <div class="control-group">
        <label class="control-label">
          Annotations
          <xsl:call-template name="annotation-info-tooltip" />
        </label>
        <div class="controls controls-well annotations-container">
          <xsl:for-each select="annotation">
            <xsl:call-template name="annotation">
              <xsl:with-param name="context" select="concat(local-name(..), ' &lt;strong&gt;', ../entityName, '&lt;/strong&gt;')" />
            </xsl:call-template>
          </xsl:for-each>
        </div>
      </div>
    </xsl:if>

    <xsl:for-each select="alternateIdentifier">
       <xsl:call-template name="entityalternateIdentifier">
          <xsl:with-param name="entityfirstColStyle" select="$datatablefirstColStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="entityDescription">
       <xsl:call-template name="entityDescription">
          <xsl:with-param name="entityfirstColStyle" select="$datatablefirstColStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="additionalInfo">
       <xsl:call-template name="entityadditionalInfo">
          <xsl:with-param name="entityfirstColStyle" select="$datatablefirstColStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    
    <!-- Display physical information, with distribution details -->
    <xsl:for-each select="physical">
        <xsl:call-template name="physical">
         <xsl:with-param name="physicalfirstColStyle" select="$datatablefirstColStyle"/>
         <xsl:with-param name="notshowdistribution"></xsl:with-param>
        </xsl:call-template>
    </xsl:for-each>
    
    <xsl:for-each select="caseSensitive">
       <xsl:call-template name="datatablecaseSensitive">
          <xsl:with-param name="datatablefirstColStyle" select="$datatablefirstColStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    
    <xsl:for-each select="numberOfRecords">
       <xsl:call-template name="datatablenumberOfRecords">
          <xsl:with-param name="datatablefirstColStyle" select="$datatablefirstColStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    
	<!-- Display distribution info -->
	<!-- NOTE: showing this as part of the physical section now -->
	<!--
    <xsl:for-each select="physical">
       <xsl:call-template name="showdistribution">
          <xsl:with-param name="docid" select="$docid"/>
          <xsl:with-param name="entityindex" select="$entityindex"/>
          <xsl:with-param name="physicalindex" select="position()"/>
          <xsl:with-param name="datatablefirstColStyle" select="$datatablefirstColStyle"/>
          <xsl:with-param name="datatablesubHeaderStyle" select="$datatablesubHeaderStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    -->
    
    <xsl:if test="coverage">
		<div class="control-group">
			<label class="control-label">Coverage Description</label>
			<div class="controls controls-well">
				<xsl:for-each select="coverage">
			        <xsl:call-template name="coverage">
			        </xsl:call-template>
			    </xsl:for-each>
			</div>
		</div>
    </xsl:if>
    
    <xsl:if test="method">
		<div class="control-group">
			<label class="control-label">Method Description</label>
			<div class="controls controls-well">
				<xsl:for-each select="method">
			        <xsl:call-template name="method">
			          <xsl:with-param name="methodfirstColStyle" select="$datatablefirstColStyle"/>
			          <xsl:with-param name="methodsubHeaderStyle" select="$datatablesubHeaderStyle"/>
			        </xsl:call-template>
			    </xsl:for-each>
			</div>
		</div>
    </xsl:if>
    
    <xsl:if test="constraint">
		<div class="control-group">
			<label class="control-label">Constraint</label>
			<div>
				<xsl:for-each select="constraint">
			        <xsl:call-template name="constraint">
			          <xsl:with-param name="constraintfirstColStyle" select="$datatablefirstColStyle"/>
			        </xsl:call-template>
			    </xsl:for-each>
			</div>
		</div>			
    </xsl:if>
    
     <xsl:if test="$withAttributes='1' or $displaymodule='printall'">
      <xsl:for-each select="attributeList">
       <xsl:call-template name="datatableattributeList">
         <xsl:with-param name="datatablefirstColStyle" select="$datatablefirstColStyle"/>
         <xsl:with-param name="datatablesubHeaderStyle" select="$datatablesubHeaderStyle"/>
         <xsl:with-param name="docid" select="$docid"/>
         <xsl:with-param name="entityindex" select="$entityindex"/>
       </xsl:call-template>
      </xsl:for-each>
     </xsl:if>

  </xsl:template>

  <xsl:template name="datatablecaseSensitive">
	<xsl:param name="datatablefirstColStyle"/>
	<div class="control-group">
       	<label class="control-label">Case Sensitive?</label>
       	<div class="controls controls-well">
       		<xsl:value-of select="."/>
       	</div>
	</div>

  </xsl:template>

  <xsl:template name="datatablenumberOfRecords">
	<xsl:param name="datatablefirstColStyle"/>
	<div class="control-group">
		<label class="control-label">Number Of Records</label>
		<div class="controls controls-well">
       		<xsl:value-of select="."/>
       	</div>
	</div>
  </xsl:template>

  <xsl:template name="showdistribution">
     <xsl:param name="datatablefirstColStyle"/>
     <xsl:param name="datatablesubHeaderStyle"/>
     <xsl:param name="docid"/>
     <xsl:param name="level">entitylevel</xsl:param>
     <xsl:param name="entitytype">dataTable</xsl:param>
     <xsl:param name="entityindex"/>
     <xsl:param name="physicalindex"/>

    <xsl:for-each select="distribution">
      
        <xsl:call-template name="distribution">
          <xsl:with-param name="docid" select="$docid"/>
          <xsl:with-param name="level" select="$level"/>
          <xsl:with-param name="entitytype" select="$entitytype"/>
          <xsl:with-param name="entityindex" select="$entityindex"/>
          <xsl:with-param name="physicalindex" select="$physicalindex"/>
          <xsl:with-param name="distributionindex" select="position()"/>
          <xsl:with-param name="disfirstColStyle" select="$datatablefirstColStyle"/>
          <xsl:with-param name="dissubHeaderStyle" select="$datatablesubHeaderStyle"/>
        </xsl:call-template>
        
      
    </xsl:for-each>
  </xsl:template>


  <xsl:template name="datatableattributeList">
    <xsl:param name="datatablefirstColStyle"/>
    <xsl:param name="datatablesubHeaderStyle"/>
    <xsl:param name="docid"/>
    <xsl:param name="entitytype">dataTable</xsl:param>
    <xsl:param name="entityindex"/>
    
    <h4>Attribute Information</h4>
    <xsl:call-template name="attributelist">
      <xsl:with-param name="docid" select="$docid"/>
      <xsl:with-param name="entitytype" select="$entitytype"/>
      <xsl:with-param name="entityindex" select="$entityindex"/>
    </xsl:call-template>

  </xsl:template>



</xsl:stylesheet>
