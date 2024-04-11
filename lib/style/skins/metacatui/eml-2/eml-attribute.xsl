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
              
<xsl:param name="annotationId"/>              

<xsl:template name="attributelist">
   <xsl:param name="docid"/>
   <xsl:param name="entitytype"/>
   <xsl:param name="entityindex"/>

        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:call-template name="attributecommon">
               <xsl:with-param name="docid" select="$docid"/>
               <xsl:with-param name="entitytype" select="$entitytype"/>
               <xsl:with-param name="entityindex" select="$entityindex"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="attributecommon">
               <xsl:with-param name="docid" select="$docid"/>
               <xsl:with-param name="entitytype" select="$entitytype"/>
               <xsl:with-param name="entityindex" select="$entityindex"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
      
</xsl:template>


<xsl:template name="attributecommon">
   <xsl:param name="docid"/>
   <xsl:param name="entitytype"/>
   <xsl:param name="entityindex"/>

<div class="attributeList">
	<div class="row-fluid">
		<div class="span2">
			<!-- render the side nav -->
      <span class="nav-header">Variables</span>
      <table class="attributeListTable">
        <tbody>
        <xsl:for-each select="attribute">
          <xsl:variable name="attributeindex" select="position()"/>
          <tr>
            <td>
              <xsl:if test="annotation">
                <span class="icon-stack annotation-icon">
                  <i class="icon icon-certificate icon-stack-base"></i>
                  <i class="icon icon-ok"></i>
                </span>
              </xsl:if>
            </td>
            <td>    
              <xsl:if test="position() = 1">
                <xsl:attribute name="class">active</xsl:attribute>
              </xsl:if>
              <a data-toggle="tab">
                <xsl:attribute name="title"><xsl:value-of select="attributeName"/></xsl:attribute>
                <xsl:attribute name="href">#entity_<xsl:value-of select="$entityindex"/>_attribute_<xsl:value-of select="$attributeindex"/></xsl:attribute>
                  <xsl:choose>
                      <xsl:when test="references!=''">
                        <xsl:variable name="ref_id" select="references"/>
                        <xsl:variable name="references" select="$ids[@id=$ref_id]" />
                        <xsl:for-each select="$references">
                          <xsl:value-of select="attributeName"/>
                        </xsl:for-each>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:value-of select="attributeName"/>
                      </xsl:otherwise>
                </xsl:choose>
              </a>
            </td>
          </tr>
        </xsl:for-each>
        </tbody>
      </table>
		</div>	
		
		<div class="tab-content span10">
		
		
  <!-- render the attributes in order-->  
  <xsl:for-each select="attribute">
  	<xsl:variable name="attributeindex" select="position()"/>
  	
  	<!-- Mark each attribute section -->
	<div class="tab-pane">
		<xsl:attribute name="id">entity_<xsl:value-of select="$entityindex"/>_attribute_<xsl:value-of select="$attributeindex"/></xsl:attribute>
		<xsl:if test="position() = 1">
			<xsl:attribute name="class">tab-pane active</xsl:attribute>
		</xsl:if>
		
		<!-- for annotating this section -->
		<xsl:variable name="absolutePath" >
         	<xsl:for-each select="ancestor-or-self::*">
         		<xsl:text>/</xsl:text>			         	
         		<xsl:value-of select="local-name()" /> 
         		<xsl:if test="local-name() = 'dataTable'">
         			<xsl:text>[</xsl:text>
         				<xsl:value-of select="$entityindex" /> 
         			<xsl:text>]</xsl:text>			         	
         		</xsl:if>        		
         	</xsl:for-each>
         </xsl:variable>

 	<!--  Name -->
  	<div class="control-group">
		<label class="control-label">Name</label>
		<div class="controls controls-well">
			<xsl:choose>
				<xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		            <xsl:value-of select="attributeName"/>
		          </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		          <xsl:value-of select="attributeName"/>
		        </xsl:otherwise>
		     </xsl:choose>
		</div>
	</div>

  <xsl:if test="annotation">
    <div class="control-group">
      <label class="control-label">
        <span class="icon-stack annotation-icon">
          <i class="icon icon-certificate icon-stack-base"></i>
          <i class="icon icon-ok"></i>
        </span>
        Annotations
        <xsl:call-template name="annotation-info-tooltip" />
      </label>
      <div class="controls controls-well annotations-container">
        <xsl:for-each select="annotation">
          <xsl:call-template name="annotation">
            <xsl:with-param name="context" select="concat('Attribute &lt;strong&gt;', ../attributeName, '&lt;/strong&gt; in ', local-name(../../..), ' &lt;strong&gt;', ../../../entityName, '&lt;/strong&gt;')" />
          </xsl:call-template>
        </xsl:for-each>
      </div>
    </div>
  </xsl:if>

	<!--  Header for the section and annotation target -->
	<div class="annotation-target">
		<xsl:attribute name="id">sem_entity_<xsl:value-of select="$entityindex"/>_attribute_<xsl:value-of select="$attributeindex"/></xsl:attribute>
		<xsl:attribute name="resource">#xpointer(<xsl:value-of select="$absolutePath"/>[<xsl:value-of select="$attributeindex"/>])</xsl:attribute>
		
		<span class="annotation-attribute-name">
			PLACEHOLDER
			<!--  
			<xsl:choose>
				<xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		            <xsl:value-of select="attributeName"/>
		          </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		          <xsl:value-of select="attributeName"/>
		        </xsl:otherwise>
		     </xsl:choose>
		     -->
		</span>
	</div>	
  

	<!-- attribute label-->
	<div class="control-group">
		<label class="control-label">Label</label>
		<div class="controls controls-well">
		    <xsl:choose>
		         <xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		             <xsl:choose>
		                <xsl:when test="attributeLabel!=''">
		                     <xsl:for-each select="attributeLabel">
		                       <xsl:value-of select="."/>
		                         &#160;<br />
		                       </xsl:for-each>
		                </xsl:when>
		                <xsl:otherwise>
		                       &#160;
		                </xsl:otherwise>
		              </xsl:choose>
		          </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		             <xsl:choose>
		                <xsl:when test="attributeLabel!=''">
		                     <xsl:for-each select="attributeLabel">
		                       <xsl:value-of select="."/>
		                         &#160;<br/>
		                       </xsl:for-each>
		                </xsl:when>
		                <xsl:otherwise>
		                       &#160;
		                </xsl:otherwise>
		              </xsl:choose>
		        </xsl:otherwise>
		     </xsl:choose>
     	</div>
	</div>
  
  <!--another row for Semantics -->
  <xsl:if test="$annotationId != ''">
	  <div class="control-group">
		<label class="control-label">Measurement</label>
		<div class="controls controls-well">
			<!-- handle references -->
			<xsl:variable name="finalAttributeName">
				<xsl:choose>
					<xsl:when test="references!=''">
						<xsl:variable name="ref_id" select="references"/>
						<xsl:variable name="references" select="$ids[@id=$ref_id]" />
						<!-- test this - should only be a single value -->
						<xsl:value-of select="$references/attributeName"/>
					</xsl:when>
			        <xsl:otherwise>
			          	<xsl:value-of select="attributeName"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<!-- load annotation detail for attribute -->
			<div>
           		<xsl:attribute name="id">
           			<xsl:value-of select="$finalAttributeName"/>
           		</xsl:attribute>
           		Loading information for: <xsl:value-of select="$finalAttributeName"/>
           	</div>
           	<script language="JavaScript">
          			var params = 
				{
					'action': 'read',
					'docid': '<xsl:value-of select="$annotationId" />',
					'qformat': '<xsl:value-of select="$qformat" />',
					'attributeLabel': '<xsl:value-of select="$finalAttributeName" />',
					'showEntity': 'true'
				};
				load(
					'<xsl:value-of select="$contextURL" />/metacat',
					params, 
					'<xsl:value-of select="$finalAttributeName" />');
			</script>
		</div>
	</div>	
		
	</xsl:if>
	
  <!-- Third row for attribute defination-->
  <div class="control-group">
		<label class="control-label">Definition</label>
		<div class="controls controls-well">
	      <xsl:choose>
	         <xsl:when test="references!=''">
	          <xsl:variable name="ref_id" select="references"/>
	          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
	           <xsl:for-each select="$references">
	               <xsl:value-of select="attributeDefinition"/>
	           </xsl:for-each>
	        </xsl:when>
	        <xsl:otherwise>
	             <xsl:value-of select="attributeDefinition"/>
	        </xsl:otherwise>
	     </xsl:choose>
     </div>
    </div>


  <!-- The fourth row for attribute storage type-->
   <div class="control-group">
		<label class="control-label">Storage Type</label>
		<div class="controls controls-well">
	      <xsl:choose>
	         <xsl:when test="references!=''">
	          <xsl:variable name="ref_id" select="references"/>
	          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
	          <xsl:for-each select="$references">
	            <xsl:choose>
	              <xsl:when test="storageType!=''">
	                    <xsl:for-each select="storageType">
	                      <xsl:value-of select="."/>
	                       &#160;<br/>
	                    </xsl:for-each>
	              </xsl:when>
	              <xsl:otherwise>
	                       &#160;
	              </xsl:otherwise>
	            </xsl:choose>
	          </xsl:for-each>
	        </xsl:when>
	        <xsl:otherwise>
	           <xsl:choose>
	              <xsl:when test="storageType!=''">
	                    <xsl:for-each select="storageType">
	                      <xsl:value-of select="."/>
	                       &#160;<br/>
	                    </xsl:for-each>
	              </xsl:when>
	              <xsl:otherwise>
	                       &#160;
	              </xsl:otherwise>
	            </xsl:choose>
	        </xsl:otherwise>
	     </xsl:choose>
     </div>
    </div>

  <!-- The fifth row for meaturement type-->
  <div class="control-group">
		<label class="control-label">Measurement Type</label>
		<div class="controls controls-well">
		    <xsl:choose>
		         <xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		              <xsl:for-each select="measurementScale">
		                 <xsl:value-of select="local-name(./*)"/>
		              </xsl:for-each>
		         </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		              <xsl:for-each select="measurementScale">
		                 <xsl:value-of select="local-name(./*)"/>
		              </xsl:for-each>
		        </xsl:otherwise>
		     </xsl:choose>
	     </div>
     </div>

  <!-- The sixth row for meaturement domain-->
  <div class="control-group">
		<label class="control-label">Measurement Domain</label>
		<div class="controls controls-well">
		    <xsl:choose>
		         <xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		              <xsl:for-each select="measurementScale">
		                <xsl:call-template name="measurementscale">
		                    <xsl:with-param name="docid" select="$docid"/>
		                    <xsl:with-param name="entitytype" select="$entitytype"/>
		                    <xsl:with-param name="entityindex" select="$entityindex"/>
		                    <xsl:with-param name="attributeindex" select="$attributeindex"/>
		                </xsl:call-template>
		              </xsl:for-each>
		         </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		              <xsl:for-each select="measurementScale">
		                <xsl:call-template name="measurementscale">
		                      <xsl:with-param name="docid" select="$docid"/>
		                      <xsl:with-param name="entitytype" select="$entitytype"/>
		                      <xsl:with-param name="entityindex" select="$entityindex"/>
		                      <xsl:with-param name="attributeindex" select="$attributeindex"/>
		                </xsl:call-template>
		              </xsl:for-each>
		        </xsl:otherwise>
		     </xsl:choose>
	     </div>
     </div>

  <!-- The seventh row for missing value code-->
  <div class="control-group">
		<label class="control-label">Missing Value Code</label>
		<div class="controls controls-well">
		     <xsl:choose>
		         <xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		            <xsl:choose>
		              <xsl:when test="missingValueCode!=''">
		                    <table class="table table-striped">
		                    	<thead>
		                    		<tr>
		                    			<th>Code</th>
		                              	<th>Explanation</th>
		                            </tr>
		                        </thead>
		                       <xsl:for-each select="missingValueCode">
		                          <tr>
		                              <td><xsl:value-of select="code"/></td>
		                              <td><xsl:value-of select="codeExplanation"/></td>
		                          </tr>
		                       </xsl:for-each>
		                   </table>
		              </xsl:when>
		              <xsl:otherwise>
		                   &#160;
		              </xsl:otherwise>
		            </xsl:choose>
		          </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		           <xsl:choose>
		              <xsl:when test="missingValueCode!=''">
		                    <table class="table table-striped">
		                    	<thead>
		                    		<tr>
		                    			<th>Code</th>
		                              	<th>Explanation</th>
		                            </tr>
		                        </thead>
		                       <xsl:for-each select="missingValueCode">
		                          <tr>
		                              <td><xsl:value-of select="code"/></td>
		                              <td><xsl:value-of select="codeExplanation"/></td>
		                          </tr>
		                       </xsl:for-each>
		                   </table>
		              </xsl:when>
		              <xsl:otherwise>
		                   &#160;
		              </xsl:otherwise>
		            </xsl:choose>
		        </xsl:otherwise>
		     </xsl:choose>
	     </div>
     </div>


  <!-- The eighth row for accuracy report-->
  <div class="control-group">
		<label class="control-label">Accuracy Report</label>
		<div class="controls controls-well">
		    <xsl:choose>
		         <xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		            <xsl:choose>
		               <xsl:when test="accuracy!=''">
		                    <xsl:for-each select="accuracy">
		                          <xsl:value-of select="attributeAccuracyReport"/>
		                    </xsl:for-each>
		              </xsl:when>
		              <xsl:otherwise>
		                  &#160;
		              </xsl:otherwise>
		            </xsl:choose>
		          </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		           <xsl:choose>
		               <xsl:when test="accuracy!=''">
		                    <xsl:for-each select="accuracy">
		                          <xsl:value-of select="attributeAccuracyReport"/>
		                    </xsl:for-each>
		              </xsl:when>
		              <xsl:otherwise>
		                  &#160;
		              </xsl:otherwise>
		            </xsl:choose>
		        </xsl:otherwise>
		     </xsl:choose>
	     </div>
     </div>

  <!-- The nineth row for quality accuracy accessment -->
  <div class="control-group">
		<label class="control-label">Accuracy Assessment</label>
		<div class="controls controls-well">
		     <xsl:choose>
		         <xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		            <xsl:choose>
		               <xsl:when test="accuracy/quantitativeAttributeAccuracyAssessment!=''">
		                   <xsl:for-each select="accuracy">
		                     <table class="table table-striped">
		                       <xsl:for-each select="quantitativeAttributeAccuracyAssessment">
		                          <tr><td>Value</td>
		                              <td><xsl:value-of select="attributeAccuracyValue"/></td>
		                          </tr>
		                          <tr><td>Expl</td>
		                              <td><xsl:value-of select="attributeAccuracyExplanation"/></td>
		                          </tr>
		                      </xsl:for-each>
		                   </table>
		                 </xsl:for-each>
		             </xsl:when>
		             <xsl:otherwise>
		                  &#160;
		             </xsl:otherwise>
		           </xsl:choose>
		          </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		           <xsl:choose>
		               <xsl:when test="accuracy/quantitativeAttributeAccuracyAssessment!=''">
		                   <xsl:for-each select="accuracy">
		                     <table class="table table-striped">
		                       <xsl:for-each select="quantitativeAttributeAccuracyAssessment">
		                          <tr><td>Value</td>
		                              <td><xsl:value-of select="attributeAccuracyValue"/></td>
		                          </tr>
		                          <tr><td>Expl</td>
		                              <td><xsl:value-of select="attributeAccuracyExplanation"/></td>
		                          </tr>
		                      </xsl:for-each>
		                   </table>
		                 </xsl:for-each>
		             </xsl:when>
		             <xsl:otherwise>
		                  &#160;
		             </xsl:otherwise>
		           </xsl:choose>
		        </xsl:otherwise>
		     </xsl:choose>
	     </div>
     </div>

   <!-- The tenth row for coverage-->
  <div class="control-group">
		<label class="control-label">Coverage</label>
		<div class="controls controls-well">
		    <xsl:choose>
		         <xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		            <xsl:choose>
		               <xsl:when test="coverage!=''">
		                    <xsl:for-each select="coverage">
		                      <xsl:call-template name="attributecoverage">
		                         <xsl:with-param name="docid" select="$docid"/>
		                         <xsl:with-param name="entitytype" select="$entitytype"/>
		                         <xsl:with-param name="entityindex" select="$entityindex"/>
		                         <xsl:with-param name="attributeindex" select="$attributeindex"/>
		                      </xsl:call-template>
		                    </xsl:for-each>
		               </xsl:when>
		               <xsl:otherwise>
		                   &#160;
		               </xsl:otherwise>
		            </xsl:choose>
		         </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		          <xsl:choose>
		               <xsl:when test="coverage!=''">
		                    <xsl:for-each select="coverage">
		                      <xsl:call-template name="attributecoverage">
		                         <xsl:with-param name="docid" select="$docid"/>
		                         <xsl:with-param name="entitytype" select="$entitytype"/>
		                         <xsl:with-param name="entityindex" select="$entityindex"/>
		                         <xsl:with-param name="attributeindex" select="$attributeindex"/>
		                      </xsl:call-template>
		                    </xsl:for-each>
		               </xsl:when>
		               <xsl:otherwise>
		                   &#160;
		               </xsl:otherwise>
		            </xsl:choose>
		        </xsl:otherwise>
		     </xsl:choose>
	     </div>
     </div>


   <!-- The eleventh row for method-->
  <div class="control-group">
		<label class="control-label">Methods &amp; Sampling</label>
		<div class="controls controls-well">
		    <xsl:choose>
		         <xsl:when test="references!=''">
		          <xsl:variable name="ref_id" select="references"/>
		          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
		          <xsl:for-each select="$references">
		            <xsl:choose>
		               <xsl:when test="methods!=''">
		                   <xsl:for-each select="methods">
		                     <xsl:call-template name="attributemethod">
		                       <xsl:with-param name="docid" select="$docid"/>
		                       <xsl:with-param name="entitytype" select="$entitytype"/>
		                       <xsl:with-param name="entityindex" select="$entityindex"/>
		                       <xsl:with-param name="attributeindex" select="$attributeindex"/>
		                     </xsl:call-template>
		                   </xsl:for-each>
		               </xsl:when>
		               <xsl:otherwise>
		                   &#160;
		               </xsl:otherwise>
		            </xsl:choose>
		         </xsl:for-each>
		        </xsl:when>
		        <xsl:otherwise>
		           <xsl:choose>
		               <xsl:when test="methods!=''">
		                   <xsl:for-each select="methods">
		                     <xsl:call-template name="attributemethod">
		                       <xsl:with-param name="docid" select="$docid"/>
		                       <xsl:with-param name="entitytype" select="$entitytype"/>
		                       <xsl:with-param name="entityindex" select="$entityindex"/>
		                       <xsl:with-param name="attributeindex" select="$attributeindex"/>
		                     </xsl:call-template>
		                   </xsl:for-each>
		               </xsl:when>
		               <xsl:otherwise>
		                   &#160;
		               </xsl:otherwise>
		            </xsl:choose>
		        </xsl:otherwise>
		     </xsl:choose>
	     </div>
     </div>
     </div> <!-- end the attribute section -->
     
     </xsl:for-each>
  
  	</div>
  	
  </div>
</div>

 </xsl:template>


<xsl:template name="singleattribute">
   <xsl:param name="docid"/>
   <xsl:param name="entitytype"/>
   <xsl:param name="entityindex"/>
   <xsl:param name="attributeindex"/>

   <table class="{$tableattributeStyle}">
        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:call-template name="singleattributecommon">
               <xsl:with-param name="docid" select="$docid"/>
               <xsl:with-param name="entitytype" select="$entitytype"/>
               <xsl:with-param name="entityindex" select="$entityindex"/>
               <xsl:with-param name="attributeindex" select="$attributeindex"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="singleattributecommon">
               <xsl:with-param name="docid" select="$docid"/>
               <xsl:with-param name="entitytype" select="$entitytype"/>
               <xsl:with-param name="entityindex" select="$entityindex"/>
               <xsl:with-param name="attributeindex" select="$attributeindex"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
  </table>
</xsl:template>


<xsl:template name="singleattributecommon">
   <xsl:param name="docid"/>
   <xsl:param name="entitytype"/>
   <xsl:param name="entityindex"/>
   <xsl:param name="attributeindex"/>

  <!-- First row for attribute name-->
  <tr><th class="rowodd">Column Name</th>
  <xsl:for-each select="attribute">
   <xsl:if test="position() = $attributeindex">
      <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <th><xsl:value-of select="attributeName"/></th>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <th><xsl:value-of select="attributeName"/></th>
        </xsl:otherwise>
     </xsl:choose>
   </xsl:if>
  </xsl:for-each>
  </tr>

  <!-- Second row for attribute label-->
  <tr><th class="rowodd">Column Label</th>
   <xsl:for-each select="attribute">
    <xsl:if test="position() = $attributeindex">
    <xsl:variable name="stripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
              </xsl:choose>
    </xsl:variable>
    <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
             <xsl:choose>
                <xsl:when test="attributeLabel!=''">
                  <td colspan="1" align="center" class="{$stripes}">
                     <xsl:for-each select="attributeLabel">
                       <xsl:value-of select="."/>
                         &#160;<br />
                       </xsl:for-each>
                  </td>
                </xsl:when>
                <xsl:otherwise>
                   <td colspan="1" align="center" class="{$stripes}">
                       &#160;<br />
                   </td>
                </xsl:otherwise>
              </xsl:choose>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
             <xsl:choose>
                <xsl:when test="attributeLabel!=''">
                  <td colspan="1" align="center" class="{$stripes}">
                     <xsl:for-each select="attributeLabel">
                       <xsl:value-of select="."/>
                         &#160;<br/>
                       </xsl:for-each>
                  </td>
                </xsl:when>
                <xsl:otherwise>
                   <td colspan="1" align="center" class="{$stripes}">
                       &#160;<br />
                   </td>
                </xsl:otherwise>
              </xsl:choose>
        </xsl:otherwise>
     </xsl:choose>
    </xsl:if>
   </xsl:for-each>
  </tr>

  <!-- Third row for attribute defination-->
  <tr><th class="rowodd">Definition</th>
    <xsl:for-each select="attribute">
     <xsl:if test="position() = $attributeindex">
      <xsl:variable name="stripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
              </xsl:choose>
      </xsl:variable>
      <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
           <xsl:for-each select="$references">
             <td colspan="1" align="center" class="{$stripes}">
               <xsl:value-of select="attributeDefinition"/>
             </td>
           </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <td colspan="1" align="center" class="{$stripes}">
             <xsl:value-of select="attributeDefinition"/>
          </td>
        </xsl:otherwise>
     </xsl:choose>
    </xsl:if>
   </xsl:for-each>
  </tr>

  <!-- The fourth row for attribute storage type-->
   <tr><th class="rowodd">Type of Value</th>
     <xsl:for-each select="attribute">
      <xsl:if test="position() = $attributeindex">
      <xsl:variable name="stripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
              </xsl:choose>
      </xsl:variable>
      <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:choose>
              <xsl:when test="storageType!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                    <xsl:for-each select="storageType">
                      <xsl:value-of select="."/>
                       &#160;<br/>
                    </xsl:for-each>
                 </td>
              </xsl:when>
              <xsl:otherwise>
                  <td colspan="1" align="center" class="{$stripes}">
                       &#160;
                   </td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <xsl:choose>
              <xsl:when test="storageType!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                    <xsl:for-each select="storageType">
                      <xsl:value-of select="."/>
                       &#160;<br/>
                    </xsl:for-each>
                 </td>
              </xsl:when>
              <xsl:otherwise>
                  <td colspan="1" align="center" class="{$stripes}">
                       &#160;
                   </td>
              </xsl:otherwise>
            </xsl:choose>
        </xsl:otherwise>
     </xsl:choose>
    </xsl:if>
   </xsl:for-each>
  </tr>

  <!-- The fifth row for meaturement type-->
  <tr><th class="rowodd">Measurement Type</th>
   <xsl:for-each select="attribute">
    <xsl:if test="position() = $attributeindex">
    <xsl:variable name="stripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
              </xsl:choose>
    </xsl:variable>
    <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <td colspan="1" align="center" class="{$stripes}">
              <xsl:for-each select="measurementScale">
                 <xsl:value-of select="local-name(./*)"/>
              </xsl:for-each>
            </td>
         </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <td colspan="1" align="center" class="{$stripes}">
              <xsl:for-each select="measurementScale">
                 <xsl:value-of select="local-name(./*)"/>
              </xsl:for-each>
           </td>
        </xsl:otherwise>
     </xsl:choose>
    </xsl:if>
   </xsl:for-each>
  </tr>

  <!-- The sixth row for meaturement domain-->
  <tr><th class="rowodd">Measurement Domain</th>
   <xsl:for-each select="attribute">
    <xsl:if test="position() = $attributeindex">
    <xsl:variable name="stripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
              </xsl:choose>
    </xsl:variable>
     <xsl:variable name="innerstripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$innercolevenStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$innercoloddStyle"/></xsl:when>
              </xsl:choose>
    </xsl:variable>
    <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <td colspan="1" align="center" class="{$stripes}">
              <xsl:for-each select="measurementScale">
                <xsl:call-template name="measurementscale">
                    <xsl:with-param name="docid" select="$docid"/>
                    <xsl:with-param name="entitytype" select="$entitytype"/>
                    <xsl:with-param name="entityindex" select="$entityindex"/>
                    <xsl:with-param name="attributeindex" select="position()"/>
                </xsl:call-template>
              </xsl:for-each>
            </td>
         </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <td colspan="1" align="center" class="{$stripes}">
              <xsl:for-each select="measurementScale">
                <xsl:call-template name="measurementscale">
                      <xsl:with-param name="docid" select="$docid"/>
                      <xsl:with-param name="entitytype" select="$entitytype"/>
                      <xsl:with-param name="entityindex" select="$entityindex"/>
                      <xsl:with-param name="attributeindex" select="position()"/>
                </xsl:call-template>
              </xsl:for-each>
           </td>
        </xsl:otherwise>
     </xsl:choose>
    </xsl:if>
   </xsl:for-each>
  </tr>


  <!-- The seventh row for missing value code-->
  <tr><th class="rowodd">Missing Value Code</th>
    <xsl:for-each select="attribute">
     <xsl:if test="position() = $attributeindex">
      <xsl:variable name="stripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
              </xsl:choose>
     </xsl:variable>
     <xsl:variable name="innerstripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$innercolevenStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$innercoloddStyle"/></xsl:when>
              </xsl:choose>
     </xsl:variable>
     <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:choose>
              <xsl:when test="missingValueCode!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                    <table>
                       <xsl:for-each select="missingValueCode">
                          <tr><td class="{$innerstripes}"><b>Code</b></td>
                              <td class="{$innerstripes}"><xsl:value-of select="code"/></td></tr>
                          <tr><td class="{$innerstripes}"><b>Expl</b></td>
                               <td class="{$innerstripes}"><xsl:value-of select="codeExplanation"/></td>
                          </tr>
                       </xsl:for-each>
                   </table>
                 </td>
              </xsl:when>
              <xsl:otherwise>
                <td colspan="1" class="{$stripes}">
                   &#160;
                </td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <xsl:choose>
              <xsl:when test="missingValueCode!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                    <table>
                       <xsl:for-each select="missingValueCode">
                          <tr><td class="{$innerstripes}"><b>Code</b></td>                              <td class="{$innerstripes}"><xsl:value-of select="code"/></td></tr>
                          <tr><td class="{$innerstripes}"><b>Expl</b></td>
                               <td class="{$innerstripes}"><xsl:value-of select="codeExplanation"/></td>
                          </tr>
                       </xsl:for-each>
                   </table>
                 </td>
              </xsl:when>
              <xsl:otherwise>
                <td colspan="1" align="center" class="{$stripes}">
                   &#160;
                </td>
              </xsl:otherwise>
            </xsl:choose>
        </xsl:otherwise>
     </xsl:choose>
     </xsl:if>
   </xsl:for-each>
  </tr>


  <!-- The eighth row for accuracy report-->
  <tr><th class="rowodd">Accuracy Report</th>
     <xsl:for-each select="attribute">
     <xsl:if test="position() = $attributeindex">
     <xsl:variable name="stripes">
         <xsl:choose>
             <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
             <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
         </xsl:choose>
    </xsl:variable>
    <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:choose>
               <xsl:when test="accuracy!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                    <xsl:for-each select="accuracy">
                          <xsl:value-of select="attributeAccuracyReport"/>
                    </xsl:for-each>
                 </td>
              </xsl:when>
              <xsl:otherwise>
                <td colspan="1" align="center" class="{$stripes}">
                  &#160;
                </td>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <xsl:choose>
               <xsl:when test="accuracy!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                    <xsl:for-each select="accuracy">
                          <xsl:value-of select="attributeAccuracyReport"/>
                    </xsl:for-each>
                 </td>
              </xsl:when>
              <xsl:otherwise>
                <td colspan="1" align="center" class="{$stripes}">
                  &#160;
                </td>
              </xsl:otherwise>
            </xsl:choose>
        </xsl:otherwise>
     </xsl:choose>
   </xsl:if>
  </xsl:for-each>
  </tr>

  <!-- The nineth row for quality accuracy accessment -->
  <tr><th class="rowodd">Accuracy Assessment</th>
     <xsl:for-each select="attribute">
     <xsl:if test="position() = $attributeindex">
     <xsl:variable name="stripes">
         <xsl:choose>
             <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
             <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
         </xsl:choose>
    </xsl:variable>
    <xsl:variable name="innerstripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$innercolevenStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$innercoloddStyle"/></xsl:when>
              </xsl:choose>
     </xsl:variable>
     <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:choose>
               <xsl:when test="accuracy/quantitativeAttributeAccuracyAssessment!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                   <xsl:for-each select="accuracy">
                     <table>
                       <xsl:for-each select="quantitativeAttributeAccuracyAssessment">
                          <tr><td class="{$innerstripes}"><b>Value</b></td>
                              <td class="{$innerstripes}"><xsl:value-of select="attributeAccuracyValue"/></td>
                          </tr>
                          <tr><td class="{$innerstripes}"><b>Expl</b></td>
                              <td class="{$innerstripes}"><xsl:value-of select="attributeAccuracyExplanation"/></td>
                          </tr>
                      </xsl:for-each>
                   </table>
                 </xsl:for-each>
               </td>
             </xsl:when>
             <xsl:otherwise>
                <td colspan="1" align="center" class="{$stripes}">
                  &#160;
                </td>
             </xsl:otherwise>
           </xsl:choose>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <xsl:choose>
               <xsl:when test="accuracy/quantitativeAttributeAccuracyAssessment!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                   <xsl:for-each select="accuracy">
                     <table>
                       <xsl:for-each select="quantitativeAttributeAccuracyAssessment">
                          <tr><td class="{$innerstripes}"><b>Value</b></td>
                              <td class="{$innerstripes}"><xsl:value-of select="attributeAccuracyValue"/></td>
                          </tr>
                          <tr><td class="{$innerstripes}"><b>Expl</b></td>
                              <td class="{$innerstripes}"><xsl:value-of select="attributeAccuracyExplanation"/></td>
                          </tr>
                      </xsl:for-each>
                   </table>
                 </xsl:for-each>
               </td>
             </xsl:when>
             <xsl:otherwise>
                <td colspan="1" align="center" class="{$stripes}">
                  &#160;
                </td>
             </xsl:otherwise>
           </xsl:choose>
        </xsl:otherwise>
     </xsl:choose>
   </xsl:if>
  </xsl:for-each>
  </tr>

   <!-- The tenth row for coverage-->
  <tr><th class="rowodd">Coverage</th>
   <xsl:for-each select="attribute">
    <xsl:if test="position() = $attributeindex">
    <xsl:variable name="index" select="position()"/>
    <xsl:variable name="stripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
              </xsl:choose>
    </xsl:variable>
    <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:choose>
               <xsl:when test="coverage!=''">
                  <td colspan="1" align="center" class="{$stripes}">
                    <xsl:for-each select="coverage">
                      <xsl:call-template name="attributecoverage">
                         <xsl:with-param name="docid" select="$docid"/>
                         <xsl:with-param name="entitytype" select="$entitytype"/>
                         <xsl:with-param name="entityindex" select="$entityindex"/>
                         <xsl:with-param name="attributeindex" select="$index"/>
                      </xsl:call-template>
                    </xsl:for-each>
                  </td>
               </xsl:when>
               <xsl:otherwise>
                  <td colspan="1" align="center" class="{$stripes}">
                   &#160;
                  </td>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:choose>
               <xsl:when test="coverage!=''">
                  <td colspan="1" align="center" class="{$stripes}">
                    <xsl:for-each select="coverage">
                      <xsl:call-template name="attributecoverage">
                         <xsl:with-param name="docid" select="$docid"/>
                         <xsl:with-param name="entitytype" select="$entitytype"/>
                         <xsl:with-param name="entityindex" select="$entityindex"/>
                         <xsl:with-param name="attributeindex" select="$index"/>
                      </xsl:call-template>
                    </xsl:for-each>
                  </td>
               </xsl:when>
               <xsl:otherwise>
                  <td colspan="1" align="center" class="{$stripes}">
                   &#160;
                  </td>
               </xsl:otherwise>
            </xsl:choose>
        </xsl:otherwise>
     </xsl:choose>
    </xsl:if>
   </xsl:for-each>
  </tr>


   <!-- The eleventh row for method-->
  <tr><th class="rowodd">Methods &amp; Sampling</th>
   <xsl:for-each select="attribute">
    <xsl:if test="position() = $attributeindex">
    <xsl:variable name="index" select="position()"/>
    <xsl:variable name="stripes">
              <xsl:choose>
                <xsl:when test="position() mod 2 = 0"><xsl:value-of select="$colevenStyle"/></xsl:when>
                <xsl:when test="position() mod 2 = 1"><xsl:value-of select="$coloddStyle"/></xsl:when>
              </xsl:choose>
    </xsl:variable>
    <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:choose>
               <xsl:when test="methods!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                   <xsl:for-each select="methods">
                     <xsl:call-template name="attributemethod">
                       <xsl:with-param name="docid" select="$docid"/>
                       <xsl:with-param name="entitytype" select="$entitytype"/>
                       <xsl:with-param name="entityindex" select="$entityindex"/>
                       <xsl:with-param name="attributeindex" select="$index"/>
                     </xsl:call-template>
                   </xsl:for-each>
                 </td>
               </xsl:when>
               <xsl:otherwise>
                 <td colspan="1" align="center" class="{$stripes}">
                   &#160;
                 </td>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <xsl:choose>
               <xsl:when test="methods!=''">
                 <td colspan="1" align="center" class="{$stripes}">
                   <xsl:for-each select="methods">
                     <xsl:call-template name="attributemethod">
                       <xsl:with-param name="docid" select="$docid"/>
                       <xsl:with-param name="entitytype" select="$entitytype"/>
                       <xsl:with-param name="entityindex" select="$entityindex"/>
                       <xsl:with-param name="attributeindex" select="$index"/>
                     </xsl:call-template>
                   </xsl:for-each>
                 </td>
               </xsl:when>
               <xsl:otherwise>
                 <td colspan="1" align="center" class="{$stripes}">
                   &#160;
                 </td>
               </xsl:otherwise>
            </xsl:choose>
        </xsl:otherwise>
     </xsl:choose>
    </xsl:if>
   </xsl:for-each>
  </tr>
 </xsl:template>


<xsl:template name="measurementscale">
   <xsl:param name="stripes"/>
   <xsl:param name="docid"/>
   <xsl:param name="entitytype"/>
   <xsl:param name="entityindex"/>
   <xsl:param name="attributeindex"/>
   <table class="table table-striped">
    <xsl:for-each select="nominal">
         <xsl:call-template name="attributenonnumericdomain">
               <xsl:with-param name="docid" select="$docid"/>
               <xsl:with-param name="entitytype" select="$entitytype"/>
               <xsl:with-param name="entityindex" select="$entityindex"/>
               <xsl:with-param name="attributeindex" select="$attributeindex"/>
               <xsl:with-param name="stripes" select="$stripes"/>
       </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="ordinal">
       <xsl:call-template name="attributenonnumericdomain">
               <xsl:with-param name="docid" select="$docid"/>
               <xsl:with-param name="entitytype" select="$entitytype"/>
               <xsl:with-param name="entityindex" select="$entityindex"/>
               <xsl:with-param name="attributeindex" select="$attributeindex"/>
               <xsl:with-param name="stripes" select="$stripes"/>
       </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="interval">
       <xsl:call-template name="intervalratio">
         <xsl:with-param name="stripes" select="$stripes"/>
       </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="ratio">
       <xsl:call-template name="intervalratio">
         <xsl:with-param name="stripes" select="$stripes"/>
       </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="dateTime">
       <xsl:call-template name="datetime">
          <xsl:with-param name="stripes" select="$stripes"/>
       </xsl:call-template>
    </xsl:for-each>
   </table>
 </xsl:template>

 <xsl:template name="attributenonnumericdomain">
   <xsl:param name="stripes"/>
   <xsl:param name="docid"/>
   <xsl:param name="entitytype"/>
   <xsl:param name="entityindex"/>
   <xsl:param name="attributeindex"/>
   <xsl:for-each select="nonNumericDomain">
     <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:call-template name="attributenonnumericdomaincommon">
                <xsl:with-param name="docid" select="$docid"/>
                <xsl:with-param name="entitytype" select="$entitytype"/>
                <xsl:with-param name="entityindex" select="$entityindex"/>
                <xsl:with-param name="attributeindex" select="$attributeindex"/>
                <xsl:with-param name="stripes" select="$stripes"/>
            </xsl:call-template>
         </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
            <xsl:call-template name="attributenonnumericdomaincommon">
               <xsl:with-param name="docid" select="$docid"/>
               <xsl:with-param name="entitytype" select="$entitytype"/>
               <xsl:with-param name="entityindex" select="$entityindex"/>
               <xsl:with-param name="attributeindex" select="$attributeindex"/>
               <xsl:with-param name="stripes" select="$stripes"/>
            </xsl:call-template>
        </xsl:otherwise>
     </xsl:choose>
   </xsl:for-each>
 </xsl:template>

 <xsl:template name="attributenonnumericdomaincommon">
    <xsl:param name="stripes"/>
    <xsl:param name="docid"/>
    <xsl:param name="entitytype"/>
    <xsl:param name="entityindex"/>
    <xsl:param name="attributeindex"/>
    <!-- if numericdomain only has one text domain,
        it will be displayed inline otherwise will show a link-->
    <xsl:choose>
      <xsl:when test="count(textDomain)=1 and not(enumeratedDomain)">      
        <tr><td class="{$stripes}">Definition</td>
            <td class="{$stripes}"><xsl:value-of select="textDomain/definition"/>
            </td>
        </tr>
        <xsl:for-each select="textDomain/pattern">
          <tr><td class="{$stripes}">Pattern</td>
            <td class="{$stripes}"><xsl:value-of select="."/>
            </td>
          </tr>
        </xsl:for-each>
        <xsl:for-each select="textDomain/source">
          <tr><td class="{$stripes}">Source</td>
            <td class="{$stripes}"><xsl:value-of select="."/>
            </td>
          </tr>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
      	<xsl:choose>
      		<xsl:when test="$displaymodule='printall'">
      			<!-- Show it all -->
				<xsl:for-each select="enumeratedDomain">
					<xsl:call-template name="enumeratedDomain">
						<xsl:with-param name="nondomainfirstColStyle" select="$firstColStyle"/>
					</xsl:call-template>
				</xsl:for-each>
      		</xsl:when>
      		<xsl:otherwise>
      			<a><xsl:attribute name="href"><xsl:value-of select="$tripleURI"/><xsl:value-of select="$docid"/>&amp;displaymodule=attributedomain&amp;entitytype=<xsl:value-of select="$entitytype"/>&amp;entityindex=<xsl:value-of select="$entityindex"/>&amp;attributeindex=<xsl:value-of select="$attributeindex"/></xsl:attribute>
				Domain Info</a>
      		</xsl:otherwise>
      	</xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
 </xsl:template>

 <xsl:template name="intervalratio">
    <xsl:param name="stripes"/>
    <xsl:if test="unit/standardUnit">
      <tr><td class="{$stripes}"><b>Unit</b></td>
            <td class="{$stripes}"><xsl:value-of select="unit/standardUnit"/>
            </td>
      </tr>
    </xsl:if>
    <xsl:if test="unit/customUnit">
      <tr><td class="{$stripes}"><b>Unit</b></td>
            <td class="{$stripes}"><xsl:value-of select="unit/customUnit"/>
            </td>
      </tr>
   </xsl:if>
   <xsl:for-each select="precision">
      <tr><td class="{$stripes}"><b>Precision</b></td>
            <td class="{$stripes}"><xsl:value-of select="."/>
            </td>
      </tr>
   </xsl:for-each>
   <xsl:for-each select="numericDomain">
      <xsl:call-template name="numericDomain">
         <xsl:with-param name="stripes" select="$stripes"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>


 <xsl:template name="numericDomain">
     <xsl:param name="stripes"/>
       <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <tr><td class="{$stripes}"><b>Type</b></td>
                <td class="{$stripes}"><xsl:value-of select="numberType"/>
                </td>
            </tr>
            <xsl:for-each select="bounds">
              <tr><td class="{$stripes}"><b>Min</b></td>
                  <td class="{$stripes}">
                    <xsl:for-each select="minimum">
                      <xsl:value-of select="."/>&#160;
                    </xsl:for-each>
                  </td>
              </tr>
              <tr><td class="{$stripes}"><b>Max</b></td>
                  <td class="{$stripes}">
                    <xsl:for-each select="maximum">
                      <xsl:value-of select="."/>&#160;
                    </xsl:for-each>
                  </td>
              </tr>
            </xsl:for-each>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <tr><td class="{$stripes}"><b>Type</b></td>
                <td class="{$stripes}"><xsl:value-of select="numberType"/>
                </td>
            </tr>
            <xsl:for-each select="bounds">
              <tr><td class="{$stripes}"><b>Min</b></td>
                  <td class="{$stripes}">
                    <xsl:for-each select="minimum">
                      <xsl:value-of select="."/>&#160;
                    </xsl:for-each>
                  </td>
              </tr>
              <tr><td class="{$stripes}"><b>Max</b></td>
                  <td class="{$stripes}">
                    <xsl:for-each select="maximum">
                      <xsl:value-of select="."/>&#160;
                    </xsl:for-each>
                  </td>
              </tr>
            </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

 <xsl:template name="datetime">
    <xsl:param name="stripes"/>
    <tr><td class="{$stripes}"><b>Format</b></td>
         <td class="{$stripes}">
            <xsl:value-of select="formatString"/>
         </td>
    </tr>
     <tr><td class="{$stripes}"><b>Precision</b></td>
         <td class="{$stripes}">
            <xsl:value-of select="dateTimePrecision"/>
         </td>
    </tr>
    <xsl:call-template name="timedomain"/>
 </xsl:template>


 <xsl:template name="timedomain">
    <xsl:param name="stripes"/>
      <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:for-each select="bounds">
              <tr><td class="{$stripes}"><b>Min</b></td>
                  <td class="{$stripes}">
                    <xsl:for-each select="minimum">
                      <xsl:value-of select="."/>&#160;
                    </xsl:for-each>
                  </td>
              </tr>
              <tr><td class="{$stripes}"><b>Max</b></td>
                  <td class="{$stripes}">
                    <xsl:for-each select="maximum">
                      <xsl:value-of select="."/>&#160;
                    </xsl:for-each>
                  </td>
              </tr>
            </xsl:for-each>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
           <xsl:for-each select="bounds">
              <tr><td class="{$stripes}"><b>Min</b></td>
                  <td class="{$stripes}">
                    <xsl:for-each select="minimum">
                      <xsl:value-of select="."/>&#160;
                    </xsl:for-each>
                  </td>
              </tr>
              <tr><td class="{$stripes}"><b>Max</b></td>
                  <td class="{$stripes}">
                    <xsl:for-each select="maximum">
                      <xsl:value-of select="."/>&#160;
                    </xsl:for-each>
                  </td>
              </tr>
            </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

 <xsl:template name="attributecoverage">
    <xsl:param name="docid"/>
    <xsl:param name="entitytype"/>
    <xsl:param name="entityindex"/>
    <xsl:param name="attributeindex"/>
    <xsl:choose>
    	<xsl:when test="$displaymodule='printall'">
			<xsl:call-template name="coverage"></xsl:call-template>
    	</xsl:when>
    	<xsl:otherwise>
			<a><xsl:attribute name="href"><xsl:value-of select="$tripleURI"/><xsl:value-of select="$docid"/>&amp;displaymodule=attributecoverage&amp;entitytype=<xsl:value-of select="$entitytype"/>&amp;entityindex=<xsl:value-of select="$entityindex"/>&amp;attributeindex=<xsl:value-of select="$attributeindex"/></xsl:attribute>
			<b>Coverage Info</b></a>
    	</xsl:otherwise>
    </xsl:choose>
 
 </xsl:template>

 <xsl:template name="attributemethod">
    <xsl:param name="docid"/>
    <xsl:param name="entitytype"/>
    <xsl:param name="entityindex"/>
    <xsl:param name="attributeindex"/>
    <xsl:choose>
    	<xsl:when test="$displaymodule='printall'">
			<xsl:call-template name="method">
				<xsl:with-param name="methodfirstColStyle" select="$firstColStyle"/>
				<xsl:with-param name="methodsubHeaderStyle" select="$firstColStyle"/>
			</xsl:call-template>
    	</xsl:when>
    	<xsl:otherwise>
			<a><xsl:attribute name="href"><xsl:value-of select="$tripleURI"/><xsl:value-of select="$docid"/>&amp;displaymodule=attributemethod&amp;entitytype=<xsl:value-of select="$entitytype"/>&amp;entityindex=<xsl:value-of select="$entityindex"/>&amp;attributeindex=<xsl:value-of select="$attributeindex"/></xsl:attribute>
			<b>Method Info</b></a>
    	</xsl:otherwise>
    </xsl:choose>

 </xsl:template>

</xsl:stylesheet>
