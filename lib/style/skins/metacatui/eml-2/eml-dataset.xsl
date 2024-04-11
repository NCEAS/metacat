<?xml version="1.0"?>
<!--
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file that is valid with respect to the eml-dataset.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">


  <xsl:output method="html" encoding="UTF-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
    indent="yes" />

  <xsl:template match="dataset" mode="dataset">
      <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
             <xsl:call-template name="datasetmixed"/>
          </xsl:for-each>
       </xsl:when>
       <xsl:otherwise>
             <xsl:call-template name="datasetmixed"/>
       </xsl:otherwise>
      </xsl:choose>

  </xsl:template>

  <xsl:template name="datasetmixed">

     <!-- citation -->
	<xsl:for-each select=".">
		<xsl:call-template name="datasetcitation" />
    </xsl:for-each>

     <h4>General</h4>
			<xsl:if test="annotation">
				<div class="control-group">
				<label class="control-label">
					Annotations
					<xsl:call-template name="annotation-info-tooltip" />
				</label>
				<div class="controls controls-well annotations-container">
					<xsl:for-each select="annotation">
					<xsl:call-template name="annotation">
						<xsl:with-param name="context" select="concat('Dataset &lt;strong&gt;', ../@packageId, '&lt;/strong&gt;')" />
					</xsl:call-template>
					</xsl:for-each>
				</div>
				</div>
			</xsl:if>

             <!-- put in the title -->
             <xsl:if test="./title">
               <xsl:for-each select="./title">
                 <xsl:call-template name="resourcetitle">
                   <xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
                   <xsl:with-param name="ressecondColStyle" select="$secondColStyle"/>
                 </xsl:call-template>
               </xsl:for-each>
             </xsl:if>
             <!-- put in the short name -->
             <xsl:if test="shortName">
             <xsl:for-each select="./shortName">
             <xsl:call-template name="resourceshortName">
               <xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
               <xsl:with-param name="ressecondColStyle" select="$secondColStyle"/>
             </xsl:call-template>
             </xsl:for-each>
             </xsl:if>
             <!-- put in the identifier and system that the ID belongs to -->
             <xsl:if test="../@packageId">
	             <xsl:for-each select="../@packageId">
	             	<xsl:call-template name="identifier">
		               <xsl:with-param name="packageID" select="../@packageId"/>
		               <xsl:with-param name="system" select="../@system"/>
		               <xsl:with-param name="IDfirstColStyle" select="$firstColStyle"/>
		               <xsl:with-param name="IDsecondColStyle" select="$secondColStyle"/>
		             </xsl:call-template>
	             </xsl:for-each>
             </xsl:if>
             <!-- put in the alternate identifiers -->
             <xsl:if test="alternateIdentifier">
             <xsl:for-each select="alternateIdentifier">
               <xsl:call-template name="resourcealternateIdentifier">
                   <xsl:with-param name="system" select="./@system"/>
                   <xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
                 <xsl:with-param name="ressecondColStyle" select="$secondColStyle"/>
               </xsl:call-template>
             </xsl:for-each>
             </xsl:if>
             <!-- put in the text of the abstract-->
             <xsl:if test="./abstract">
             <xsl:for-each select="./abstract">
               <xsl:call-template name="resourceabstract">
                 <xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
                 <xsl:with-param name="ressecondColStyle" select="$secondColStyle"/>
               </xsl:call-template>
             </xsl:for-each>
             </xsl:if>
             <!-- put in the purpose of the dataset-->
             <xsl:if test="./purpose">
             <xsl:for-each select="./purpose">
               <xsl:call-template name="datasetpurpose">
                 <xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
                 <xsl:with-param name="ressecondColStyle" select="$secondColStyle"/>
               </xsl:call-template>
             </xsl:for-each>
             </xsl:if>
             <!-- put in the keyword sets -->
             <xsl:if test="keywordSet">
             	<div class="row-fluid">
					<div class="control-group">
						<label class="control-label">
							<xsl:text>Keywords</xsl:text>
						</label>
						<div class="controls controls-well">
							<xsl:for-each select="keywordSet">
								<xsl:call-template name="resourcekeywordSet" >
									<xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
									<xsl:with-param name="ressecondColStyle" select="$secondColStyle"/>
								</xsl:call-template>
							</xsl:for-each>
						</div>
					</div>
				</div>
             </xsl:if>


		     <!-- add in additionalInfo -->
		     <div class="control-group">
		         <xsl:if test="additionalInfo">
		           <label class="control-label"><xsl:text>Additional Info</xsl:text></label>
		           <xsl:for-each select="additionalInfo">
		       			<div class="controls controls-well">
			              <xsl:call-template name="text">
			                <xsl:with-param name="textfirstColStyle" select="$firstColStyle"/>
			              </xsl:call-template>
		          		</div>
		           </xsl:for-each>
		         </xsl:if>
		     </div>

             <!-- put in the publication date -->
             <xsl:if test="./pubDate">
               <xsl:for-each select="pubDate">
                <xsl:call-template name="resourcepubDate" >
                  <xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
                 </xsl:call-template>
               </xsl:for-each>
             </xsl:if>

             <!-- put in the language -->
             <xsl:if test="./language">
               <xsl:for-each select="language">
                 <xsl:call-template name="resourcelanguage" >
                   <xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
                  </xsl:call-template>
               </xsl:for-each>
             </xsl:if>

             <!-- put in the series -->
             <xsl:if test="./series">
               <xsl:for-each select="series">
                 <xsl:call-template name="resourceseries" >
                   <xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
                 </xsl:call-template>
               </xsl:for-each>
             </xsl:if>

     <h4>People and Associated Parties</h4>

       <!-- add in the creators -->
       <xsl:if test="creator">
         <div class="control-group">
         	<label class="control-label">
         		Data Set Creators
         	</label>
         	<div class="controls controls-well">
		         <xsl:for-each select="creator">

		         	<xsl:variable name="absolutePath" >
			         	<xsl:for-each select="ancestor-or-self::*">
			         		<xsl:text>/</xsl:text>
			         		<xsl:value-of select="local-name()" />
			         	</xsl:for-each>
			         </xsl:variable>
					<xsl:variable name="index" select="position()" />
		         	<div>
		         		<!--
		         		<div class="annotation-target">
			         		<xsl:attribute name="resource">#xpointer(<xsl:value-of select="$absolutePath"/>[<xsl:value-of select="$index"/>])</xsl:attribute>
			         		<xsl:attribute name="type">party</xsl:attribute>
			         		PARTY
			         	</div>
			         	-->
		               <xsl:call-template name="party">
		                 <xsl:with-param name="partyfirstColStyle" select="$firstColStyle"/>
		                 <xsl:with-param name="partysecondColStyle" select="$secondColStyle"/>
		               </xsl:call-template>
		         	</div>
		         </xsl:for-each>
         	</div>
         </div>
       </xsl:if>

       <!-- add in the contacts -->
       <xsl:if test="contact">
         <div class="control-group">
         	<label class="control-label">Data Set Contacts</label>
         	<div class="controls controls-well">
	         	<xsl:for-each select="contact">
	         		<xsl:variable name="absolutePath" >
			         	<xsl:for-each select="ancestor-or-self::*">
			         		<xsl:text>/</xsl:text>
			         		<xsl:value-of select="local-name()" />
			         	</xsl:for-each>
			         </xsl:variable>
					<xsl:variable name="index" select="position()" />
		         	<div>
		         		<!--
		         		<div class="annotation-target">
			         		<xsl:attribute name="resource">#xpointer(<xsl:value-of select="$absolutePath"/>[<xsl:value-of select="$index"/>])</xsl:attribute>
			         		<xsl:attribute name="type">party</xsl:attribute>
			         		PARTY
			         	</div>
			         	-->
	               <xsl:call-template name="party">
	                 <xsl:with-param name="partyfirstColStyle" select="$firstColStyle"/>
	                 <xsl:with-param name="partysecondColStyle" select="$secondColStyle"/>
	               </xsl:call-template>
	               </div>

	         	</xsl:for-each>
	         </div>
         </div>
       </xsl:if>

       <!-- add in the associatedParty  -->
       <xsl:if test="associatedParty">
         <div class="control-group">
         	<label class="control-label">Associated Parties</label>
         	<div class="controls controls-well">
				<xsl:for-each select="associatedParty">
					<xsl:variable name="absolutePath" >
			         	<xsl:for-each select="ancestor-or-self::*">
			         		<xsl:text>/</xsl:text>
			         		<xsl:value-of select="local-name()" />
			         	</xsl:for-each>
			         </xsl:variable>
					<xsl:variable name="index" select="position()" />
		         	<div>
		         		<!--
		         		<div class="annotation-target">
			         		<xsl:attribute name="resource">#xpointer(<xsl:value-of select="$absolutePath"/>[<xsl:value-of select="$index"/>])</xsl:attribute>
			         		<xsl:attribute name="type">party</xsl:attribute>
			         		PARTY
			         	</div>
			         	-->
					<xsl:call-template name="party">
						<xsl:with-param name="partyfirstColStyle" select="$firstColStyle"/>
						<xsl:with-param name="partysecondColStyle" select="$secondColStyle"/>
					</xsl:call-template>
					</div>
		         </xsl:for-each>
	         </div>
         </div>

       </xsl:if>

       <!-- add in the metadataProviders using a two column table -->
       <xsl:if test="metadataProvider">
		<div class="control-group">
         	<label class="control-label">Metadata Providers</label>
         	<div class="controls controls-well">
				<xsl:for-each select="metadataProvider">
					<xsl:variable name="absolutePath" >
			         	<xsl:for-each select="ancestor-or-self::*">
			         		<xsl:text>/</xsl:text>
			         		<xsl:value-of select="local-name()" />
			         	</xsl:for-each>
			         </xsl:variable>
					<xsl:variable name="index" select="position()" />
		         	<div>
		         		<!--
		         		<div class="annotation-target">
			         		<xsl:attribute name="resource">#xpointer(<xsl:value-of select="$absolutePath"/>[<xsl:value-of select="$index"/>])</xsl:attribute>
			         		<xsl:attribute name="type">party</xsl:attribute>
			         		PARTY
			         	</div>
			         	-->
	               <xsl:call-template name="party">
	                 <xsl:with-param name="partyfirstColStyle" select="$firstColStyle"/>
	                 <xsl:with-param name="partysecondColStyle" select="$secondColStyle"/>
	               </xsl:call-template>
					</div>
				</xsl:for-each>
		   </div>
		</div>
       </xsl:if>

       <!-- add in the publishers using a two column table -->
       <xsl:if test="publisher">
         <div class="control-group">
         	<label class="control-label">Data Set Publishers</label>
         	<div class="controls controls-well">
		         <xsl:for-each select="publisher">
		         	<xsl:variable name="absolutePath" >
			         	<xsl:for-each select="ancestor-or-self::*">
			         		<xsl:text>/</xsl:text>
			         		<xsl:value-of select="local-name()" />
			         	</xsl:for-each>
			         </xsl:variable>
					<xsl:variable name="index" select="position()" />
		         	<div>
		         		<!--
		         		<div class="annotation-target">
			         		<xsl:attribute name="resource">#xpointer(<xsl:value-of select="$absolutePath"/>[<xsl:value-of select="$index"/>])</xsl:attribute>
			         		<xsl:attribute name="type">party</xsl:attribute>
			         		PARTY
			         	</div>
			         	-->
	               <xsl:call-template name="party">
	                 <xsl:with-param name="partyfirstColStyle" select="$firstColStyle"/>
	                 <xsl:with-param name="partysecondColStyle" select="$secondColStyle"/>
	               </xsl:call-template>
					</div>
		         </xsl:for-each>
	         </div>
         </div>
       </xsl:if>

     <!-- add in the coverage info -->
     <!--  <h4>Context</h4> -->

     <!-- add in the geographic coverage info -->
     <div class="row-fluid">
           <xsl:if test="./coverage/geographicCoverage">
             <xsl:for-each select="./coverage/geographicCoverage[position()&lt;26]">
               <xsl:call-template name="geographicCoverage">
                 <xsl:with-param name="firstColStyle" select="$firstColStyle"/>
                 <xsl:with-param name="secondColStyle" select="$secondColStyle"/>
               </xsl:call-template>
             </xsl:for-each>

             <xsl:variable name="geoCount" select="count(./coverage/geographicCoverage)-25"/>

             <xsl:if test="count(./coverage/geographicCoverage)&gt;25">
               <div class="alert alert-info">
                 <xsl:value-of select="$geoCount"/> geographic regions are hidden.
               Download the full metadata document to see all geographic regions.</div>
             </xsl:if>

           </xsl:if>
       </div>
       <!-- add in the temporal coverage info -->
       <div class="row-fluid">
           <xsl:if test="./coverage/temporalCoverage">
             <xsl:for-each select="./coverage/temporalCoverage[position()&lt;26]">
               <xsl:call-template name="temporalCoverage">
                 <xsl:with-param name="firstColStyle" select="$firstColStyle"/>
                 <xsl:with-param name="secondColStyle" select="$secondColStyle"/>
               </xsl:call-template>
             </xsl:for-each>

             <xsl:variable name="tempCount" select="count(./coverage/temporalCoverage)-25"/>

             <xsl:if test="count(./coverage/temporalCoverage)&gt;25">
               <div class="alert alert-info">
                 <xsl:value-of select="$tempCount"/> temporal coverages are hidden.
               Download the full metadata document to see all temporal coverages.</div>
             </xsl:if>
           </xsl:if>
       </div>
       <!-- add in the taxonomic coverage info -->
       <div class="row-fluid">
           <xsl:if test="./coverage/taxonomicCoverage">
             <xsl:for-each select="./coverage/taxonomicCoverage[position()&lt;26]">
               <xsl:call-template name="taxonomicCoverage">
                 <xsl:with-param name="firstColStyle" select="$firstColStyle"/>
                 <xsl:with-param name="secondColStyle" select="$secondColStyle"/>
               </xsl:call-template>
             </xsl:for-each>

             <xsl:variable name="taxCount" select="count(./coverage/taxonomicCoverage)-25"/>

             <xsl:if test="count(./coverage/taxonomicCoverage)&gt;25">
               <div class="alert alert-info">
                 <xsl:value-of select="$taxCount"/> taxonomic ranges are hidden.
               Download the full metadata document to see all taxonomic ranges.</div>
             </xsl:if>
           </xsl:if>
       </div>

     <!-- add in the project  info -->

     <div id="dataset-project" class="row-fluid">
         <xsl:if test="./project">
           <xsl:call-template name="datasetproject">
             <xsl:with-param name="firstColStyle" select="$firstColStyle"/>
             <xsl:with-param name="secondColStyle" select="$secondColStyle"/>
           </xsl:call-template>
         </xsl:if>
     </div>

     <!-- add in the method info -->

     <div class="row-fluid">
         <xsl:if test="./methods">
           <xsl:for-each select="./methods">
             <xsl:call-template name="datasetmethod">
               <xsl:with-param name="methodfirstColStyle" select="$firstColStyle"/>
               <xsl:with-param name="methodsecondColStyle" select="$secondColStyle"/>
             </xsl:call-template>
           </xsl:for-each>
         </xsl:if>
     </div>

     <!-- create a second easy access table listing the data entities -->
     <xsl:if test="dataTable|spatialRaster|spatialVector|storedProcedure|view|otherEntity">
      <xsl:if test="$withEntityLinks='1' or $displaymodule = 'printall'">
              <xsl:call-template name="datasetentity"/>
      </xsl:if>
     </xsl:if>


       <!-- add in the intellectiual rights info -->
		 <xsl:if test="intellectualRights or licensed">
			<h4>Data Set Usage Rights</h4>
			<div class="row-fluid">
				<xsl:for-each select="licensed">
					<xsl:call-template name="resourceLicensed">
						<xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
						<xsl:with-param name="ressecondColStyle" select="$secondColStyle"/>
					</xsl:call-template>
				</xsl:for-each>
				<xsl:for-each select="intellectualRights">
					<xsl:call-template name="resourceintellectualRights">
						<xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
						<xsl:with-param name="ressecondColStyle" select="$secondColStyle"/>
					</xsl:call-template>
				</xsl:for-each>
			</div>
		 </xsl:if>

       <!-- add in the access control info -->
     <div class="row-fluid">
         <xsl:if test="access">
           <xsl:for-each select="access">
             <xsl:call-template name="access">
               <xsl:with-param name="accessfirstColStyle" select="$firstColStyle"/>
               <xsl:with-param name="accesssecondColStyle" select="$secondColStyle"/>
             </xsl:call-template>
           </xsl:for-each>
         </xsl:if>
     </div>
  </xsl:template>

  <xsl:template name="datasetresource">
     <div class="row-fluid">
        <xsl:call-template name="resource">
          <xsl:with-param name="resfirstColStyle" select="$firstColStyle"/>
          <xsl:with-param name="ressubHeaderStyle" select="$subHeaderStyle"/>
        </xsl:call-template>
     </div>
  </xsl:template>


  <xsl:template name="datasetpurpose">
    <xsl:for-each select="purpose">
      <div class="control-group">
      	<label class="control-label"><xsl:text>Purpose</xsl:text></label>
       	<div  class="controls">
            &#160;
              <xsl:call-template name="text">
                <xsl:with-param name="textfirstColStyle" select="$firstColStyle"/>
              </xsl:call-template>
          </div>
       </div>
     </xsl:for-each>
  </xsl:template>

  <xsl:template name="datasetmaintenance">
    <xsl:for-each select="maintenance">
      <div class="control-group">
      	<label class="control-label"><xsl:text>Maintenance</xsl:text></label>
      	<div class="controls">
		     <xsl:call-template name="mantenancedescription"/>
		      <div class="control-group">
      			<label class="control-label">Frequency</label>
		        <div class="controls" >
		           <xsl:value-of select="maintenanceUpdateFrequency"/>
		        </div>
		     </div>
		     <xsl:call-template name="datasetchangehistory"/>
		   </div>
		</div>
   	</xsl:for-each>
  </xsl:template>

  <xsl:template name="mantenancedescription">
   <xsl:for-each select="description">
     <div class="control-group">
      	<label class="control-label">Description</label>
        <div class="controls">
            <xsl:call-template name="text">
               <xsl:with-param name="textfirstColStyle" select="$firstColStyle"/>
             </xsl:call-template>
          </div>
     </div>
    </xsl:for-each>
  </xsl:template>

   <xsl:template name="datasetchangehistory">
   <xsl:if test="changeHistory">
     <div class="control-group">
      	<label class="control-label">History</label>
          <div class="controls">
              <xsl:for-each select="changeHistory">
                <xsl:call-template name="historydetails"/>
              </xsl:for-each>
          </div>
     </div>
     </xsl:if>
   </xsl:template>

   <xsl:template name="historydetails">
        <div class="control-group">
	      	<label class="control-label">scope</label>
	        <div class="controls">
	            <xsl:value-of select="changeScope"/>
	        </div>
        </div>
        <div class="control-group">
	      	<label class="control-label">old value</label>
	        <div class="controls">
	            <xsl:value-of select="oldValue"/>
        	</div>
       	</div>
        <div class="control-group">
      		<label class="control-label">change date</label>
            <div class="controls">
	            <xsl:value-of select="changeDate"/>
	        </div>
	    </div>
        <xsl:if test="comment and normalize-space(comment)!=''">
			<div class="control-group">
      			<label class="control-label">comment</label>
      			<div class="controls">
		            <xsl:value-of select="comment"/>
				</div>
			</div>
        </xsl:if>
  </xsl:template>

  <xsl:template name="datasetcontact">
    <div class="control-group">
      	<label class="control-label"><xsl:text>Contact</xsl:text></label>
	    <div class="controls">
		    <xsl:for-each select="contact">
			       <xsl:call-template name="party">
		              <xsl:with-param name="partyfirstColStyle" select="$firstColStyle"/>
		       </xsl:call-template>
	    	</xsl:for-each>
	    </div>
	</div>
  </xsl:template>

  <xsl:template name="datasetpublisher">
   <xsl:for-each select="publisher">
     <div class="control-group">
      	<label class="control-label"><xsl:text>Publisher</xsl:text></label>
     	<div class="controls">
	       <xsl:call-template name="party">
	              <xsl:with-param name="partyfirstColStyle" select="$firstColStyle"/>
	       </xsl:call-template>
     	</div>
     </div>
   </xsl:for-each>
  </xsl:template>

  <xsl:template name="datasetpubplace">
    <xsl:for-each select="pubPlace">
      <div class="control-group">
      	<label class="control-label">Publish Place</label>
        <div class="controls">
          <xsl:value-of select="."/>
		</div>
      </div>
   </xsl:for-each>
  </xsl:template>

  <xsl:template name="datasetmethod">
     <h4><xsl:text>Methods &amp; Sampling</xsl:text></h4>
     <xsl:for-each select=".">
        <xsl:call-template name="method">
          <xsl:with-param name="methodfirstColStyle" select="$firstColStyle"/>
        </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="datasetproject">
    <h4><xsl:text>Project Information</xsl:text></h4>
    <xsl:for-each select="project">
     <div class="row-fluid">
       <xsl:call-template name="project">
         <xsl:with-param name="projectfirstColStyle" select="$firstColStyle"/>
       </xsl:call-template>
     </div>
    </xsl:for-each>
  </xsl:template>

   <xsl:template name="datasetaccess">
    <xsl:for-each select="access">
      <div class="row-fluid">
        <xsl:call-template name="access">
          <xsl:with-param name="accessfirstColStyle" select="$firstColStyle"/>
          <xsl:with-param name="accesssubHeaderStyle" select="$subHeaderStyle"/>
        </xsl:call-template>
      </div>
    </xsl:for-each>
  </xsl:template>

	<xsl:template name="datasetentity">
		<xsl:if test="dataTable or spatialRaster or spatialVector or storedProcedure or view or otherEntity">
			<h4>
				<xsl:text>Data Table, Image, and Other Data Details</xsl:text>
			</h4>
		</xsl:if>


		<!--  <xsl:call-template name="xml" /> -->

		<xsl:choose>
			<xsl:when test="$displaymodule!='printall'">
					<xsl:for-each select="dataTable | spatialRaster | spatialVector | storedProcedure | view | otherEntity">
				<xsl:call-template name="entityurl">
					<xsl:with-param name="type"><xsl:value-of select="local-name()" /></xsl:with-param>
					<xsl:with-param name="showtype">
						<xsl:choose>
							<xsl:when test="local-name()='dataTable'">
								<xsl:text>Data Table</xsl:text>
							</xsl:when>
							<xsl:when test="local-name()='spatialRaster'">
								<xsl:text>Spatial Raster</xsl:text>
							</xsl:when>
							<xsl:when test="local-name()='spatialVector'">
								<xsl:text>Spatial Vector</xsl:text>
							</xsl:when>
							<xsl:when test="local-name()='storedProcedures'">
								<xsl:text>Stored Procedure</xsl:text>
							</xsl:when>
							<xsl:when test="local-name()='view'">
								<xsl:text>View</xsl:text>
							</xsl:when>
							<xsl:when test="local-name()='otherEntity'">
								<xsl:text>Other Entity</xsl:text>
							</xsl:when>
						</xsl:choose>
					</xsl:with-param>
					<xsl:with-param name="index" select="position()" />
				</xsl:call-template>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="dataTable | spatialRaster | spatialVector | storedProcedure | view | otherEntity">
					<xsl:variable name="currentNode" select="position()" />
          <xsl:variable name="dataId" select="./@id" />
						<div class="control-group entity">
							<div class="controls controls-well entitydetails" data-id="{$dataId}">
								<label class="control-label">
									<xsl:choose>
										<xsl:when test="local-name()='dataTable'">
											<xsl:text>Data Table</xsl:text>
										</xsl:when>
										<xsl:when test="local-name()='spatialRaster'">
											<xsl:text>Spatial Raster</xsl:text>
										</xsl:when>
										<xsl:when test="local-name()='spatialVector'">
											<xsl:text>Spatial Vector</xsl:text>
										</xsl:when>
										<xsl:when test="local-name()='storedProcedures'">
											<xsl:text>Stored Procedure</xsl:text>
										</xsl:when>
										<xsl:when test="local-name()='view'">
											<xsl:text>View</xsl:text>
										</xsl:when>
										<xsl:when test="local-name()='otherEntity'">
											<xsl:text>Other Entity</xsl:text>
										</xsl:when>
									</xsl:choose>
								</label>
								<xsl:call-template name="chooseentity">
									<xsl:with-param name="entitytype"><xsl:value-of select="local-name()" /></xsl:with-param>
									<xsl:with-param name="entityindex" select="$currentNode" />
								</xsl:call-template>
							</div>
						</div>
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="entityurl">
		<xsl:param name="showtype" />
		<xsl:param name="type" />
		<xsl:param name="index" />
		<xsl:choose>
			<xsl:when test="references!=''">
				<xsl:variable name="ref_id" select="references" />
				<xsl:variable name="references" select="$ids[@id=$ref_id]" />
				<xsl:for-each select="$references">
					<div class="control-group">
						<label class="control-label">
							View Metadata
						</label>
						<div class="controls controls-well">
							<a>
								<xsl:attribute name="href">
									<xsl:value-of select="$tripleURI" /><xsl:value-of select="$docid" />&amp;displaymodule=entity&amp;entitytype=<xsl:value-of select="$type"/>&amp;entityindex=<xsl:value-of select="$index"/>
								</xsl:attribute>
								<xsl:value-of select="./physical/objectName"/>
							</a>
						</div>
					</div>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<div class="control-group">
					<label class="control-label">
						<xsl:value-of select="$showtype"/>
					</label>
					<div class="controls controls-well">
						<xsl:value-of select="./entityName"/>
						(<a>
						<xsl:attribute name="href">
						<xsl:value-of select="$tripleURI"/><xsl:value-of select="$docid"/>&amp;displaymodule=entity&amp;entitytype=<xsl:value-of select="$type"/>&amp;entityindex=<xsl:value-of select="$index"/></xsl:attribute>
						View Metadata</a>
						<xsl:text> </xsl:text>
					    <xsl:choose>
						    <xsl:when test="./physical/distribution/online/url">
						    	|
						    	<xsl:variable name="URL" select="./physical/distribution/online/url"/>
					            <a>
									<xsl:choose>
										<xsl:when test="starts-with($URL,'ecogrid')">
											<xsl:variable name="URL1" select="substring-after($URL, 'ecogrid://')"/>
											<xsl:variable name="dataDocID" select="substring-after($URL1, '/')"/>
											<xsl:attribute name="href">
												<xsl:value-of select="$tripleURI"/><xsl:value-of select="$dataDocID"/>
											</xsl:attribute>
										</xsl:when>
										<xsl:otherwise>
											<xsl:attribute name="href"><xsl:value-of select="$URL"/></xsl:attribute>
										</xsl:otherwise>
									</xsl:choose>
								<xsl:attribute name="target">_blank</xsl:attribute>
								Download File <i class="icon-download" alt="download"></i>
								</a>
							</xsl:when>
						</xsl:choose>)
					</div>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

  <xsl:template match="text()" mode="dataset" />
  <xsl:template match="text()" mode="resource" />

</xsl:stylesheet>
