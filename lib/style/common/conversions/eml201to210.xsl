<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  version="1.0" xmlns:eml="eml://ecoinformatics.org/eml-2.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
<xsl:output method="xml" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:template match="/* ">
    <!--handle top level element-->
   <xsl:element name="eml:eml"> 
      <xsl:copy-of select="@*"/> 
      <xsl:attribute name="xsi:schemaLocation">eml://ecoinformatics.org/eml-2.1.0 eml.xsd</xsl:attribute>

     <!-- move the access sub tree to top level-->
     <xsl:apply-templates mode="copy-top-access-tree" select="/*/dataset/access"/>
     <xsl:apply-templates mode="copy-top-access-tree" select="/*/citation/access"/>
     <xsl:apply-templates mode="copy-top-access-tree" select="/*/software/access"/>
     <xsl:apply-templates mode="copy-top-access-tree" select="/*/protocol/access"/>	
     
      <xsl:for-each select="/*/*">
    	  <xsl:choose>
    	     <xsl:when test="name()='dataset'">
                  <xsl:element name="{name(.)}" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="handle-elements-under-main-module" select="."/>
    	           </xsl:element>
			 </xsl:when>

    	     <xsl:when test="name()='citation'">
    	         <xsl:element name="{name(.)}" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="handle-elements-under-main-module" select="."/>
    	           </xsl:element>
    	     </xsl:when>

    	     <xsl:when test="name()='software'">
    	         <xsl:element name="{name(.)}" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="handle-elements-under-main-module" select="."/>
    	           </xsl:element>
    	     </xsl:when>

    	     <xsl:when test="name()='protocol'">
    	         <xsl:element name="{name(.)}" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="handle-elements-under-main-module" select="."/>
    	           </xsl:element>
    	     </xsl:when>

    	     <xsl:when test="name()='additionalMetadata'">
				<xsl:apply-templates mode="handle-additionalMetadata" select="."/>
    	     </xsl:when>

    	   </xsl:choose>
    	 </xsl:for-each>
  </xsl:element>
  </xsl:template>

	<!-- handle make changes under main module (dataset, citation, protocol and software) -->
	<xsl:template mode="handle-elements-under-main-module" match="*">  		
		<xsl:for-each select="./*">
                  	<xsl:choose>
    	            	<xsl:when test="name()='access'">
    	               		<xsl:apply-templates mode="do-nothing" select="."/>
    	            	</xsl:when>
                     	<xsl:otherwise>
   	                        <xsl:apply-templates select="."/>							
                     	 </xsl:otherwise>
                  	</xsl:choose>
   		</xsl:for-each>     
	</xsl:template>
   
    <!-- main template which will copy nodes recursively-->
    <xsl:template match="*">  
        <xsl:element name="{name(.)}" namespace="{namespace-uri(.)}">  
             <xsl:copy-of select="@*"/>
             <xsl:apply-templates/>
        </xsl:element>
	</xsl:template>

	<!-- fixing attributeList/attribute/measurementScale/datetime -> .../dateTime -->
	<xsl:template match="attributeList/attribute/measurementScale/datetime">  
        <xsl:element name="dateTime" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="copy-no-ns" select="./*"/>
    	 </xsl:element>
	</xsl:template>
	
    <!-- change the name of element form method to methods -->
	<xsl:template match="dataTable/method">  
        <xsl:element name="methods" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="copy-no-ns" select="./*"/>
    	 </xsl:element>
	</xsl:template>

	<!-- change the name of element form method to methods -->
    <xsl:template match="spatialRaster/method">  
        <xsl:element name="methods" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="copy-no-ns" select="./*"/>
    	 </xsl:element>
	</xsl:template>

	<!-- change the name of element form method to methods -->
	<xsl:template match="spatialVector/method">  
        <xsl:element name="methods" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="copy-no-ns" select="./*"/>
    	 </xsl:element>
	</xsl:template>

	<!-- change the name of element form method to methods -->
	<xsl:template match="view/method">  
        <xsl:element name="methods" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="copy-no-ns" select="./*"/>
    	 </xsl:element>
	</xsl:template>

	<!-- change the name of element form method to methods -->
	<xsl:template match="storedProcedure/method">  
        <xsl:element name="methods" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="copy-no-ns" select="./*"/>
    	 </xsl:element>
	</xsl:template>

	<!-- change the name of element form method to methods -->
	<xsl:template match="otherEntity/method">  
        <xsl:element name="methods" namespace="{namespace-uri(.)}">  
           			    <xsl:copy-of select="@*"/> 
                    	<xsl:apply-templates mode="copy-no-ns" select="./*"/>
    	 </xsl:element>
	</xsl:template>


     <!-- Move the access tree of data file level from additionalMetadata part to physical/distribution or software/implementation/distribution part.
           If we find the id of physical/distribution is in aditionalMetadata/describe and it 
             has sibling of access subtree, copy the subtree to physical/distribution -->
     <xsl:template match="physical/distribution | software/implementation/distribution">
        <xsl:element name="distribution" namespace="{namespace-uri(.)}">
          <xsl:copy-of select="@*"/> 
          <xsl:apply-templates mode="copy-no-ns" select="./*"/>         
				<!--distribution doesn't have any access node yet. Move the subtree from addtionalMetadata to distribution-->
				<!--find the id in addtionalMetacat/describes-->
          		<xsl:variable name="id" select="@id"/>
				<!-- count how many additionalMetadata/access describing same distribution is -->
				<xsl:variable name="countAccessTree" select="count(/*/additionalMetadata[describes=$id]/access)"/>
				<xsl:choose>
					<xsl:when test="$countAccessTree=1">
          				<!-- only has one access tree, we need copy it to distribution-->
                     	 <xsl:apply-templates mode="copy-no-ns" select="/*/additionalMetadata[describes=$id]/access"/>
                	</xsl:when>
					<xsl:when test="$countAccessTree &gt; 1">
          				 <!-- has more than one access tree, we need merge them-->
						 <!--This means document have two or more addtionalMetadata
					          with access tree describing same distribution.
                       		<additionalMetadata>
                          		<describes>100</describe>
                          		<access>...</access>
                        	</additionalMetadata>
						 	<additionalMetadata>
                          		<describes>100</describe>
                          		<access>...</access>
                        	</additionalMetadata>-->
                         	<xsl:variable name="totalOrderNumber" select="count(/*/additionalMetadata[describes=$id]/access)"/>
						 	<xsl:variable name="totalAllowFirstNumber" select="count(/*/additionalMetadata[describes=$id]/access[@order='allowFirst'])"/>						  
						    <xsl:variable name="totalDenyFirstNumber" select="count(/*/additionalMetadata[describes=$id]/access[@order='denyFirst'])"/>
							<xsl:choose>
								<xsl:when test="$totalOrderNumber=$totalAllowFirstNumber or $totalOrderNumber=$totalDenyFirstNumber">	
								<!-- all access subtrees have same order, just merge it-->			                      	 
							  		<xsl:element name="access">
										<xsl:copy-of select="/*/additionalMetadata[describes=$id]/access/@*"/> 
										<xsl:for-each select="/*/additionalMetadata[describes=$id]/access">
											<xsl:apply-templates mode="copy-no-ns" select="./*"/>
										</xsl:for-each>
							 		</xsl:element>
								</xsl:when>
								<xsl:otherwise>
									<xsl:message terminate="yes">EML 2.0.1 document has more than one access subtrees in addtionalMetadata blocks describing same entity.
                                              However, attributes "order" have different value. It is illegitimate. Please fix the EML 2.0.1 document first.
                                    </xsl:message>
								</xsl:otherwise>
							</xsl:choose>						 
                	</xsl:when>		
			 </xsl:choose>
		</xsl:element>
     </xsl:template>


	<!-- copy access tree under dataset(or protocol, software and citation) to the top level -->
	<xsl:template mode="copy-top-access-tree" match="*">
         <xsl:apply-templates mode="copy-no-ns" select="."/>
 	</xsl:template>

	<!-- do nothing for this element (removing it)-->
 	<xsl:template mode="do-nothing" match="*">  
 	</xsl:template>

     <!-- copy node and children without namespace -->
	<xsl:template mode="copy-no-ns" match="*">  
        <xsl:element name="{name(.)}" namespace="{namespace-uri(.)}">  
           <xsl:copy-of select="@*"/> 
           <xsl:apply-templates mode="copy-no-ns"/>  
        </xsl:element> 
	</xsl:template>

	<!--Handle additionMetadata part. Here are 4 scenarios:
         1. <additionalMetadata>
					<describes>100</describes>
                     <foo>.....</foo>
             </additionalMetacata>
             The result will be (foo deosn't euqal access):
             <additionalMetadata>
					<describes>100</describes>
                     <metadata><foo>.....</foo></metadata>
             </additionalMetacata>
         2. <additionalMetadata>
					<describes>100</describes>
					<describes>200</describes>
                     <access>.....</access>
             </additionalMetacata>
            Both 100 and 200 are referenced ids of pysical/distribtuion or software/implementation/distribution.
            Since we moved the access part to distribution element, we don't need to keep the info. 
           So the output is blank - remvoing the additionalMetadata tree.
         3. <additionalMetadata>
					<describes>300</describes>
					<describes>400</describes>
                     <access>.....</access>
             </additionalMetacata>
            300 is the referenced ids of pysical/distribtuion or software/implementation/distribution. 
            But 400 is not. So output will be:
            <additionalMetadata>
					<describes>400</describes>
                     <metadata><access>.....</access></metadata>
             </additionalMetacata>
            And we will give an warning message: 400 is a not distribution id and the eml201 document does not follow EML pratice.
		4. <additionalMetadata>
                     <access>.....</access>
             </additionalMetacata>
            Since no describes, no access tree will move. So output will be:
            <additionalMetadata>
                     <metadata><access>.....</access></metadata>
             </additionalMetacata>
            And we will give an warning message:No distribution id in addtionalMetadata/describes and the eml201 document does not follow EML pratice.
	-->
	<xsl:template mode="handle-additionalMetadata" match="*">
			<!-- test if this additionalMetadata part has "access" element-->
			<xsl:variable name="accessCount" select="count(access)" />
			<xsl:choose>
				<xsl:when test="$accessCount &lt; 1">
					<!-- no access tree here. Scenario 1 -->
					<additionalMetadata>
    	             	<xsl:for-each select="./*">
    	           	           <xsl:choose>
    	           	               <xsl:when test="name()='describes'">
    	           	                    <xsl:apply-templates mode="copy-no-ns" select="."/>
    	           	               </xsl:when>
    	           	               <xsl:otherwise>
    	           	                   <metadata>
    	           	                      <xsl:apply-templates mode="copy-no-ns" select="."/>
    	           	                    </metadata>
    	           	                </xsl:otherwise>
    	           	           </xsl:choose>
    	           	  </xsl:for-each>
    	        	</additionalMetadata>
 				</xsl:when>
				<xsl:otherwise>
					<!--additionalMetadata has access child-->
					<xsl:variable name="describesCount" select="count(access)" />
					<xsl:choose>
						<xsl:when test="$describesCount=0">
							<!-- scenario 4 -->
							 <additionalMetadata>
 	  	           	                <metadata>
    	           	                      <xsl:apply-templates mode="copy-no-ns" select="./*"/>
    	           	                 </metadata>
    	        			</additionalMetadata>
							<xsl:message terminate="no">additonalMetadata has access element, but doesn't have any describes element which references id of physical/distribution 
																		or software/implementation/distribution elements. This document doesn't follow the EML practice.
                            </xsl:message>
						</xsl:when>
						<xsl:otherwise>
							<xsl:for-each select="./describes">
								 <xsl:variable name="describesID" select="."/>
								 <xsl:variable name="distribution" select="count(//physical/distribution[@id=$describesID] | //software/implementation/distribution[@id=$describesID] )"/>
									<xsl:if test="$distribution=0">
								 		<xsl:message terminate="no">We removed the access tree from additionalMetadata. However, described ID doesn't reference any distribution id</xsl:message>
									</xsl:if>
							</xsl:for-each>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
	</xsl:template>
              
</xsl:stylesheet>
