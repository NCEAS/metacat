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

  <xsl:template name="spatialVector">
      <xsl:param name="spatialvectorfirstColStyle"/>
      <xsl:param name="spatialvectorsubHeaderStyle"/>
      <xsl:param name="docid"/>
      <xsl:param name="entityindex"/>
      <table class="{$tabledefaultStyle}">
        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:call-template name="spatialVectorcommon">
             <xsl:with-param name="spatialvectorfirstColStyle" select="$spatialvectorfirstColStyle"/>
             <xsl:with-param name="spatialvectorsubHeaderStyle" select="$spatialvectorsubHeaderStyle"/>
             <xsl:with-param name="docid" select="$docid"/>
             <xsl:with-param name="entityindex" select="$entityindex"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <xsl:call-template name="spatialVectorcommon">
             <xsl:with-param name="spatialvectorfirstColStyle" select="$spatialvectorfirstColStyle"/>
             <xsl:with-param name="spatialvectorsubHeaderStyle" select="$spatialvectorsubHeaderStyle"/>
             <xsl:with-param name="docid" select="$docid"/>
             <xsl:with-param name="entityindex" select="$entityindex"/>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
      </table>
  </xsl:template>

  <xsl:template name="spatialVectorcommon">
    <xsl:param name="spatialvectorfirstColStyle"/>
    <xsl:param name="spatialvectorsubHeaderStyle"/>
    <xsl:param name="docid"/>
    <xsl:param name="entityindex"/>
    <xsl:for-each select="entityName">
       <xsl:call-template name="entityName">
          <xsl:with-param name="entityfirstColStyle" select="$spatialvectorfirstColStyle"/>
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
          <xsl:with-param name="entityfirstColStyle" select="$spatialvectorfirstColStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="entityDescription">
       <xsl:call-template name="entityDescription">
          <xsl:with-param name="entityfirstColStyle" select="$spatialvectorfirstColStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="additionalInfo">
       <xsl:call-template name="entityadditionalInfo">
          <xsl:with-param name="entityfirstColStyle" select="$spatialvectorfirstColStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    <!-- call physical moduel without show distribution(we want see it later)-->
    <xsl:if test="physical">
       <tr><td class="{$spatialvectorsubHeaderStyle}" colspan="2">
        Physical Structure Description:
      </td></tr>
    </xsl:if>
    <xsl:for-each select="physical">
      <tr><td colspan="2">
        <xsl:call-template name="physical">
         <xsl:with-param name="physicalfirstColStyle" select="$spatialvectorfirstColStyle"/>
         <xsl:with-param name="notshowdistribution">yes</xsl:with-param>
        </xsl:call-template>
        </td></tr>
     </xsl:for-each>

	<!-- Here to display distribution info-->
    <xsl:for-each select="physical">
       <xsl:call-template name="spatialVectorShowDistribution">
          <xsl:with-param name="docid" select="$docid"/>
          <xsl:with-param name="entityindex" select="$entityindex"/>
          <xsl:with-param name="physicalindex" select="position()"/>
          <xsl:with-param name="spatialvectorfirstColStyle" select="$spatialvectorfirstColStyle"/>
          <xsl:with-param name="spatialvectorsubHeaderStyle" select="$spatialvectorsubHeaderStyle"/>
       </xsl:call-template>
    </xsl:for-each>
    
    <xsl:if test="coverage">
       <tr><td class="{$spatialvectorsubHeaderStyle}" colspan="2">
        Coverage Description:
      </td></tr>
    </xsl:if>
    <xsl:for-each select="coverage">
      <tr><td colspan="2">
        <xsl:call-template name="coverage">
        </xsl:call-template>
      </td></tr>
    </xsl:for-each>
    <xsl:if test="method">
       <tr><td class="{$spatialvectorsubHeaderStyle}" colspan="2">
        Method Description:
      </td></tr>
    </xsl:if>
    <xsl:for-each select="method">
      <tr><td colspan="2">
        <xsl:call-template name="method">
          <xsl:with-param name="methodfirstColStyle" select="$spatialvectorfirstColStyle"/>
          <xsl:with-param name="methodsubHeaderStyle" select="$spatialvectorsubHeaderStyle"/>
        </xsl:call-template>
      </td></tr>
    </xsl:for-each>
    <xsl:if test="constraint">
       <tr><td class="{$spatialvectorsubHeaderStyle}" colspan="2">
        Constraint:
      </td></tr>
    </xsl:if>
    <xsl:for-each select="constraint">
      <tr><td colspan="2">
        <xsl:call-template name="constraint">
          <xsl:with-param name="constraintfirstColStyle" select="$spatialvectorfirstColStyle"/>
        </xsl:call-template>
      </td></tr>
    </xsl:for-each>
    <xsl:for-each select="geometry">
       <tr><td class="{$spatialvectorfirstColStyle}">
            Geometry:
            </td>
            <td class="{$secondColStyle}">
              <xsl:value-of select="."/>
            </td>
       </tr>
    </xsl:for-each>
    <xsl:for-each select="geometricObjectCount">
       <tr><td class="{$spatialvectorfirstColStyle}">
            Number of Geometric Objects:
            </td>
            <td class="{$secondColStyle}">
              <xsl:value-of select="."/>
            </td>
       </tr>
    </xsl:for-each>
    <xsl:for-each select="topologyLevel">
       <tr><td class="{$spatialvectorfirstColStyle}">
           Topolgy Level:
            </td>
            <td class="{$secondColStyle}">
              <xsl:value-of select="."/>
            </td>
       </tr>
    </xsl:for-each>
    <xsl:for-each select="spatialReference">
       <tr><td class="{$spatialvectorsubHeaderStyle}" colspan="2">
        Spatial Reference:
      </td></tr>
      <xsl:call-template name="spatialReference">
        <xsl:with-param name="spatialrasterfirstColStyle" select="$spatialvectorfirstColStyle"/>
      </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="horizontalAccuracy">
      <tr><td class="{$spatialvectorsubHeaderStyle}" colspan="2">
        Horizontal Accuracy:
      </td></tr>
      <xsl:call-template name="dataQuality">
        <xsl:with-param name="spatialrasterfirstColStyle" select="$spatialvectorfirstColStyle"/>
      </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="verticalAccuracy">
      <tr><td class="{$spatialvectorsubHeaderStyle}" colspan="2">
        Vertical Accuracy:
      </td></tr>
      <xsl:call-template name="dataQuality">
        <xsl:with-param name="spatialrasterfirstColStyle" select="$spatialvectorfirstColStyle"/>
      </xsl:call-template>
    </xsl:for-each>
    <xsl:if test="$withAttributes='1' or $displaymodule='printall'">
    <xsl:for-each select="attributeList">
      <xsl:call-template name="spatialVectorAttributeList">
        <xsl:with-param name="spatialvectorfirstColStyle" select="$spatialvectorfirstColStyle"/>
        <xsl:with-param name="spatialvectorsubHeaderStyle" select="$spatialvectorsubHeaderStyle"/>
        <xsl:with-param name="docid" select="$docid"/>
        <xsl:with-param name="entityindex" select="$entityindex"/>
      </xsl:call-template>
    </xsl:for-each>
    </xsl:if>
  </xsl:template>

  <xsl:template name="spatialVectorShowDistribution">
     <xsl:param name="spatialvectorfirstColStyle"/>
     <xsl:param name="spatialvectorsubHeaderStyle"/>
     <xsl:param name="docid"/>
     <xsl:param name="level">entitylevel</xsl:param>
     <xsl:param name="entitytype">spatialVector</xsl:param>
     <xsl:param name="entityindex"/>
     <xsl:param name="physicalindex"/>
     <xsl:for-each select="distribution">
      <tr><td colspan="2">
        <xsl:call-template name="distribution">
          <xsl:with-param name="docid" select="$docid"/>
          <xsl:with-param name="level" select="$level"/>
          <xsl:with-param name="entitytype" select="$entitytype"/>
          <xsl:with-param name="entityindex" select="$entityindex"/>
          <xsl:with-param name="physicalindex" select="$physicalindex"/>
          <xsl:with-param name="distributionindex" select="position()"/>
          <xsl:with-param name="disfirstColStyle" select="$spatialvectorfirstColStyle"/>
          <xsl:with-param name="dissubHeaderStyle" select="$spatialvectorsubHeaderStyle"/>
        </xsl:call-template>
      </td></tr>
    </xsl:for-each>
  </xsl:template>


  <xsl:template name="spatialVectorAttributeList">
    <xsl:param name="spatialvectorfirstColStyle"/>
    <xsl:param name="spatialvectorsubHeaderStyle"/>
    <xsl:param name="docid"/>
    <xsl:param name="entitytype">spatialVector</xsl:param>
    <xsl:param name="entityindex"/>
    <tr><td class="{$spatialvectorsubHeaderStyle}" colspan="2">
        <xsl:text>Attribute(s) Info:</xsl:text>
    </td></tr>
    <tr><td colspan="2">
         <xsl:call-template name="attributelist">
           <xsl:with-param name="docid" select="$docid"/>
           <xsl:with-param name="entitytype" select="$entitytype"/>
           <xsl:with-param name="entityindex" select="$entityindex"/>
         </xsl:call-template>
       </td>
    </tr>
  </xsl:template>



</xsl:stylesheet>
