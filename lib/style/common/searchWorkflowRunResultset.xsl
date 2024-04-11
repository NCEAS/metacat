<?xml version="1.0"?>
<!--
	* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
	* convert an XML file showing the resultset of a query
	* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:import href="util.xsl"/>
	<xsl:output method="html" />
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	<xsl:variable name ="pdfExtension" select="'.pdf'"/>
	<xsl:variable name ="emptyString"/>
	
	<xsl:template match="/">  
		<div class="result-header-section">      
        	<div class="result-header">TPC Workflow Runs</div>
		</div>
		<div class="row row-header" >
			<div class="col run-col1-header">TPC Workflow Name</div>
			<div class="col run-col2-header">Status</div>
			<div class="col run-col3-header">Date Executed</div>
			<div class="col run-col4-header">Available Reports</div>
		</div>			
		<!-- This tests to see if there are returned documents,
			if there are not then don't show the query results -->
		<xsl:if test="count(resultset/document) &gt; 0">
			<xsl:for-each select="resultset/document">
				<xsl:sort select='./param[@name="karEntry/karEntryXML/property[@name=&apos;WorkflowRun&apos;]/property[@name=&apos;startTime&apos;]/@value"]' order='descending' />
                <xsl:sort select='./param[@name="karEntry/karEntryXML/property[@name=&apos;WorkflowRun&apos;]/property[@name=&apos;workflowName&apos;]/@value"]' />
				<!-- test if the document has the "workflowRun" property. If has, kept this document-->
				<xsl:variable name="workflowRunClassName">
					<xsl:value-of select='./param[@name="karEntry/karEntryXML/property[@name=&apos;WorkflowRun&apos;]/@class"]'/>
				</xsl:variable>
				
				<xsl:if test="$workflowRunClassName != $emptyString">				
					<xsl:variable name="karDocid">
                            <xsl:call-template name="extractDocidFromLsid">
                                <xsl:with-param name="lsidString" select='./param[@name="mainAttributes/lsid"]' />
                            </xsl:call-template>
					</xsl:variable>
					
				
					<div>
						<xsl:attribute name="class">
							<xsl:choose>
							<xsl:when test="position() mod 2 = 1">row row-odd</xsl:when>
							<xsl:when test="position() mod 2 = 0">row row-even</xsl:when>
							</xsl:choose>
						</xsl:attribute>

						<div class="col run-col1">	
							<xsl:value-of select='./param[@name="karEntry/karEntryXML/property[@name=&apos;WorkflowRun&apos;]/property[@name=&apos;workflowName&apos;]/@value"]' />		
						</div>
						<div class="col run-col2">	
							<xsl:choose>
							<xsl:when  test="not(./param[@name=&quot;karEntry/karEntryAttributes/tpcStatus&quot;])" >
							unknown
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select='./param[@name="karEntry/karEntryAttributes/tpcStatus"]' />	
							</xsl:otherwise>
							</xsl:choose>
						</div>
						<div class="col run-col3">	
						  <xsl:variable name="date" select='./param[@name="karEntry/karEntryXML/property[@name=&apos;WorkflowRun&apos;]/property[@name=&apos;startTime&apos;]/@value"]' />    
							<xsl:value-of select='substring-before($date, "T")' />		
						</div>
						<div class="col run-col4">	
							<xsl:for-each select='./param[@name="karEntry/karEntryAttributes/Name"]'>
                                <xsl:variable name="fileName" select='.' />             
                                <xsl:if test="contains($fileName, $pdfExtension)">
                                     <xsl:variable name="pdfFile">
                                         <xsl:value-of select="normalize-space($fileName)"/>
                                         </xsl:variable>
                                         <a>
                                            <xsl:attribute name="class">underlined</xsl:attribute>
                                            <xsl:attribute name="href">
                                                <xsl:value-of select='$contextURL' />/metacat?action=read&amp;docid=<xsl:value-of select="$karDocid" />&amp;archiveEntryName=<xsl:value-of select="$pdfFile" />
                                            </xsl:attribute>
                                            PDF
                                        </a> 
                             </xsl:if>                      
                            </xsl:for-each>
						<!--  | 
						<a>
							<xsl:attribute name="class">underlined</xsl:attribute>
							<xsl:attribute name="href">
								<xsl:value-of select='$contextURL' />/metacat?action=read&amp;docid=<xsl:value-of select='./param[@name="property[@name=&apos;KeplerDocumentation&apos;]/property[@name=&apos;archiveName&apos;]/configure"]' />&amp;archiveEntryName=<xsl:value-of select='./param[@name="property[@name=&apos;KeplerDocumentation&apos;]/property[@name=&apos;htmlReport&apos;]/configure"]' />
							</xsl:attribute>
							HTML
						</a --> 							 
						</div>		
					</div>
				</xsl:if>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>