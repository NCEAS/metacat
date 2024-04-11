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


   <xsl:template name="project">
        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <div class="row-fluid project" data-content="project">
              <xsl:call-template name="projectcommon" />
            </div>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <div class="row-fluid project" data-content="project" id="project">
            <xsl:call-template name="projectcommon" />
          </div>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>



  <xsl:template name="projectcommon">
    <xsl:call-template name="projecttitle" />
    <xsl:call-template name="projectpersonnel" />
    <xsl:call-template name="projectabstract" />
    <xsl:call-template name="projectfunding" />
    <xsl:call-template name="projectaward" />
    <xsl:call-template name="projectstudyareadescription" />
    <xsl:call-template name="projectdesigndescription" />
    <xsl:call-template name="projectrelatedproject" />
  </xsl:template>



   <xsl:template name="projecttitle">
     <xsl:for-each select="title">
	   <div class="control-group">
	     <label class="control-label projectTitle">Title:</label>
	   	 <div class="controls controls-well">
		   <xsl:value-of select="./text()" />
	   	 </div>
	   </div>
     </xsl:for-each>
  </xsl:template>



  <xsl:template name="projectpersonnel">
	<div class="control-group">
		<label class="control-label projectPersonnel">Personnel:</label>
		<div class="controls control-well">
			<xsl:for-each select="personnel">

				<xsl:call-template name="party" />

			</xsl:for-each>
		</div>
	</div>
  </xsl:template>


   <xsl:template name="projectabstract">
     <xsl:for-each select="abstract">
       <div class="control-group">
         <label class="control-label projectAbstract">Abstract:</label>
         <div class="controls controls-well">
           <xsl:call-template name="text" />
         </div>
       </div>
     </xsl:for-each>
  </xsl:template>

  <xsl:template name="projectfunding">
     <xsl:for-each select="funding">
	   <div class="control-group">
	     <label class="control-label projectFunding">Funding:</label>
	     <div class="controls controls-well projectFundingValue" >
	       <xsl:call-template name="text" />
	     </div>
	   </div>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="projectaward">
    <xsl:for-each select="./award">
      <xsl:variable name="awardUrl" select="./awardUrl/text()" />
      <div class="control-group">
        <label class="control-label projectAward">Award:</label>
        <div class="controls controls-well projectFundingValue">
          <!-- funderName -->
          <div class="row-fluid">
            <div class="control-group">
              <label class="control-label">Funder</label>
              <div class="controls">
                <xsl:value-of select="./funderName/text()" />
              </div>
            </div>
          </div>
          <!-- funderIdentifier -->
          <xsl:for-each select="./funderIdentifier">
            <xsl:variable name="funderIdentifier" select="./text()" />
            <div class="row-fluid">
              <div class="control-group">
                <label class="control-label">Funder Identifier</label>
                <div class="controls">
                  <xsl:value-of select="$funderIdentifier" />
                </div>
              </div>
            </div>
          </xsl:for-each>
          <!-- awardNumber -->
          <xsl:if test="./awardNumber">
            <div class="row-fluid">
              <div class="control-group">
                <label class="control-label">Number</label>
                <div class="controls">
                  <xsl:value-of select="./awardNumber/text()" />
                </div>
              </div>
            </div>
          </xsl:if>
          <!-- title -->
          <div class="row-fluid">
            <div class="control-group">
              <label class="control-label">Title</label>
              <div class="controls">
                <xsl:value-of select="./title/text()" />
              </div>
            </div>
          </div>
          <!-- awardUrl -->
          <xsl:if test="./awardUrl">
            <div class="row-fluid">
              <div class="control-group">
                <label class="control-label">URL</label>
                <div class="controls">
                  <xsl:value-of select="$awardUrl" />
                </div>
              </div>
            </div>
          </xsl:if>
        </div>
      </div>
    </xsl:for-each>
  </xsl:template>

   <xsl:template name="projectstudyareadescription">
       <xsl:for-each select="studyAreaDescription">
            <div class="control-group">
                <label class="control-label projectStudyAreaDescription"><xsl:text>Study Area Descriptors:</xsl:text></label>
                <div class="controls controls-well">
                    <table class="table table-striped">
                        <thead>
                            <th>Name</th>
                            <th>Code</th>
                            <th>Value</th>
                            <th>Classification System</th>
                        </thead>
                        <tbody>
                            <xsl:for-each select="descriptor">
                                <tr>
                                    <xsl:for-each select="descriptorValue">
                                        <td>
                                            <xsl:value-of select="../@name" />
                                        </td>
                                        <td>
                                            <xsl:value-of select="./@name_or_id" />
                                        </td>
                                        <td>
                                            <xsl:value-of select="." />
                                        </td>
                                        <td>
                                            <xsl:choose>
                                                <xsl:when test="citation != ''">
                                                    <xsl:for-each select="citation">
                                                        <label class="control-label projectCitation">Citation:</label>
                                                        <div class="controls controls-well">
                                                            <xsl:call-template name="citation" />
                                                        </div>
                                                    </xsl:for-each>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:text>&#160;</xsl:text>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </td>
                                    </xsl:for-each>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table>
                </div>
            </div>
            
            <xsl:for-each select="citation">
               <div class="control-group">
 	   	           <label class="control-label projectCitation">Study Area Citation:</label>
                   <div class="controls controls-well">
                       <xsl:call-template name="citation" />
                   </div>
               </div>
               
            </xsl:for-each>
            
            <xsl:for-each select="coverage">
              <div class="control-group">
                <label class="control-label">Study Area Coverage:</label>
                <div class="controls controls-well">
                  <xsl:call-template name="coverage" />
                </div>
              </div>
            </xsl:for-each>
       </xsl:for-each>
   </xsl:template>



  <xsl:template name="projectdesigndescription">
    <xsl:for-each select="designDescription">
       <xsl:for-each select="description">
         <div class="control-group">
           <label class="control-label">Design Description:</label>
           <div class="controls controls-well">
             <xsl:call-template name="text"/>
           </div>
         </div>
      </xsl:for-each>
      <xsl:for-each select="citation">
        <div class="control-group">
          <label class="control-label">Design Citation:</label>
          <div class="controls controls-well">
             <xsl:call-template name="citation"/>
          </div>
       </div>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:template>



  <xsl:template name="projectrelatedproject">
    <xsl:for-each select="relatedProject">
       <div class="control-group">
       <label class="control-label">Related Project:</label>
          <div>
            <xsl:call-template name="project" />
         </div>
       </div>
    </xsl:for-each>
  </xsl:template>


</xsl:stylesheet>
