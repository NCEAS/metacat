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
	<xsl:variable name ="emptyString"/>

	<xsl:template match="/">  
	
		<div class="result-header-section">      
        	<div class="result-header">TPC Workflows</div>
		</div>
		<div class="row row-header" >
			<div class="col col1-header">TPC Name</div>
			<div class="col col2-header">Creator</div>
			<div class="col col3-header">Date Created</div>
			<div class="col col4-header">Action</div>
		</div>			
		<!-- This tests to see if there are returned documents,
			if there are not then don't show the query results -->
		<xsl:if test="count(resultset/document) &gt; 0">
			<xsl:for-each select="resultset/document">
				<xsl:sort select='./updatedate' order='descending' />
				<!--<xsl:sort select='./param[@name="/entity/@name"]' />
				<xsl:if test="./docname = 'entity'">-->
				<xsl:sort select='./param[@name="karEntry/karEntryXML/entity/@name"]' />
				<xsl:variable name="workflowRunClassName">
                    <xsl:value-of select='./param[@name="karEntry/karEntryXML/property[@name=&apos;WorkflowRun&apos;]/@class"]'/>
                </xsl:variable>
                <xsl:if test="$workflowRunClassName = $emptyString">
					<div>
						<xsl:attribute name="class">
							<xsl:choose>
								<xsl:when test="position() mod 2 = 1">row row-odd</xsl:when>
								<xsl:when test="position() mod 2 = 0">row row-even</xsl:when>
							</xsl:choose>
						</xsl:attribute>
						<xsl:variable name="karLSID">
							<xsl:value-of select='./param[@name="mainAttributes/lsid"]' />
						</xsl:variable>
						<xsl:variable name="karDocid">
							<xsl:call-template name="extractDocidFromLsid">
								<xsl:with-param name="lsidString" select="$karLSID" />
							</xsl:call-template>
						</xsl:variable>
						<xsl:variable name="workflowLSID">
							<xsl:value-of select='./param[@name="karEntry/karEntryXML/entity/property[@name=&apos;entityId&apos;]/@value"]'/>
						</xsl:variable>
						<xsl:variable name="karXmlDocid">
                            <xsl:value-of select='./docid'/>
                        </xsl:variable>
                        <!--<xsl:variable name="karXmlLSID">
                            <xsl:call-template name="lsid">
                                <xsl:with-param name="docid" select="$karXmlDocid" />
                            </xsl:call-template>
                        </xsl:variable>-->

						<div class="col col1">	
							<!--<xsl:value-of select='./param[@name="/entity/@name"]' />-->
							<xsl:value-of select='./param[@name="karEntry/karEntryXML/entity/@name"]' />		
						</div>
						<div class="col col2">	
							<!--<xsl:value-of select='./param[@name="property[@name=&apos;KeplerDocumentation&apos;]/property[@name=&apos;author&apos;]/configure"]' />-->
							<xsl:value-of select='./param[@name="karEntry/karEntryXML/entity/property[@name=&apos;KeplerDocumentation&apos;]/property[@name=&apos;author&apos;]/configure"]' />		
						</div>
						<div class="col col3">	
							<xsl:value-of select='./updatedate' />			
						</div>
						<div class="col col4">	
							<a>
								<xsl:attribute name="class">underlined</xsl:attribute>
								<!--<xsl:attribute name="href">./searchWorkflowRun.jsp?workflowid=<xsl:value-of select='./param[@name="/entity/property[@name=&apos;entityId&apos;]/@value"]' /></xsl:attribute>-->
								<xsl:attribute name="href">./searchWorkflowRunMain.jsp?workflowlsid=<xsl:value-of select="$workflowLSID" /></xsl:attribute>
									 View Runs
							</a>
							<span> | </span>
							<a>
								<xsl:attribute name="class">underlined</xsl:attribute>
								<!--<xsl:attribute name="href">./scheduleWorkflowRun.jsp?workflowid=<xsl:value-of select='./param[@name="/entity/property[@name=&apos;entityId&apos;]/@value"]' />&amp;karid=<xsl:value-of select='./param[@name="/entity/property[@name=&apos;karLSID&apos;]/@value"]' />&amp;workflowname=<xsl:value-of select='./param[@name="/entity/@name"]' /></xsl:attribute>-->
								<xsl:attribute name="href">./scheduleWorkflowRunMain.jsp?workflowid=<xsl:value-of select="$workflowLSID" />&amp;karid=<xsl:value-of select="normalize-space($karLSID)" />&amp;workflowname=<xsl:value-of select='./param[@name="karEntry/karEntryXML/entity/@name"]' /></xsl:attribute>	 
									 Schedule
							</a> 
							<span> | </span>
							<a>
								<xsl:attribute name="class">underlined</xsl:attribute>
								<xsl:attribute name="href">
									<!--<xsl:value-of select='$contextURL' />/metacat?action=read&amp;docid=<xsl:value-of select='./param[@name="/entity/property[@name=&apos;karLSID&apos;]/@value"]' />-->
									<xsl:value-of select='$contextURL' />/metacat?action=read&amp;docid=<xsl:value-of select="$karDocid" />
								</xsl:attribute>	 
									 Download
							</a> 
						</div>		
					</div>
				</xsl:if>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>
	
	<!--************** creates a fake lsid for kar xml file document**************-->
   <xsl:template name="lsid">
       <xsl:param name="docid"/>
       <xsl:variable name="colonString">
           <xsl:value-of select="translate($docid, '.', ':')" />
        </xsl:variable>
       <xsl:variable name="lsidString" select="concat('urn:lsid:','gamma.msi.ucsb.edu/OpenAuth',':',string($colonString))"/>      
        <xsl:value-of select="$lsidString"/>
   </xsl:template>
</xsl:stylesheet>