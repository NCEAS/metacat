<?xml version="1.0"?>
<!--
	*  '$RCSfile$'
	*      Authors: Michael Daigle
	*    Copyright: 2009 Regents of the University of California and the
	*               National Center for Ecological Analysis and Synthesis
	*  For Details: http://www.nceas.ucsb.edu/
	*
	*   '$Author: leinfelder $'
	*     '$Date: 2009-04-02 14:20:05 -0800 (Thu, 02 Apr 2009) $'
	* '$Revision: 4893 $'
	*
	* This program is free software; you can redistribute it and/or modify
	* it under the terms of the GNU General Public License as published by
	* the Free Software Foundation; either version 2 of the License, or
	* (at your option) any later version.
	*
	* This program is distributed in the hope that it will be useful,
	* but WITHOUT ANY WARRANTY; without even the implied warranty of
	* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	* GNU General Public License for more details.
	*
	* You should have received a copy of the GNU General Public License
	* along with this program; if not, write to the Free Software
	* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
	*
	* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
	* convert an XML file showing the resultset of a query
	* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:output method="html" />
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	<xsl:param name="servletURL"/>
	<xsl:param name ="authServiceURL"/>
    <xsl:param name ="authorizationURL"/>
	
	<xsl:template match="/">  
		<div class="row row-header" >
			<div class="col col1-header">Run Name</div>
			<div class="col col2-header">Status</div>
			<div class="col col3-header">Start Time</div>
			<div class="col col4-header">End Time</div>
			<div class="col col5-header">Interval</div>
			<div class="col col6-header">Action</div>
		</div>	
		<!-- div>jobs: <xsl:number value="count(scheduledWorkflowResultset/scheduledJob)" />	</div -->	
		<!-- This tests to see if there are returned documents,
			if there are not then don't show the query results -->
		<xsl:if test="count(scheduledWorkflowResultset/scheduledJob) &gt; 0">
			<xsl:for-each select="scheduledWorkflowResultset/scheduledJob">
				<div>
					<xsl:attribute name="class">
						<xsl:choose>
							<xsl:when test="position() mod 2 = 1">row row-odd</xsl:when>
							<xsl:when test="position() mod 2 = 0">row row-even</xsl:when>
						</xsl:choose>
					</xsl:attribute>

					<div class="col col1">	
						<xsl:value-of select='./name' />		
					</div>
					<div class="col col2">	
						<!--<xsl:value-of select='./status' />	-->
						<xsl:choose>
							<xsl:when test="./status = 'scheduled'">
								Enabled
							</xsl:when>
							<xsl:otherwise>
								Disabled
							</xsl:otherwise>
						</xsl:choose>	
					</div>
					<div class="col col3">	
						<xsl:value-of select='./startTime' />		
					</div>
					<div class="col col4">	
						<xsl:value-of select='./endTime' />		
					</div>
					<div class="col col5">	
						<xsl:value-of select='./intervalValue' />&#xa0;<xsl:value-of select='./intervalUnit' /> 		
					</div>
					<div class="col col6">	
						<a>
							<xsl:choose>
							<xsl:when test="./status = 'scheduled'">
								<xsl:attribute name="class">underlined</xsl:attribute>
								<xsl:attribute name="href"><xsl:value-of select='$servletURL' />?action=unscheduleWorkflow&amp;workflowjobname=<xsl:value-of select='./name' />&amp;sessionid=<xsl:value-of select='$sessid' />&amp;authServiceURL=<xsl:value-of select='$authServiceURL' />&amp;authorizationURL=<xsl:value-of select='$authorizationURL' />&amp;workflowname=<xsl:value-of select='./jobParam[@name=&apos;workflowname&apos;]/value' />&amp;workflowid=<xsl:value-of select='./jobParam[@name=&apos;workflowid&apos;]/value' />&amp;karid=<xsl:value-of select='./jobParam[@name=&apos;karid&apos;]/value' />&amp;qformat=<xsl:value-of select='$qformat' />&amp;forwardto=scheduleWorkflowRunMain.jsp</xsl:attribute>
								Disable
							</xsl:when>
							<xsl:otherwise>
								<xsl:attribute name="class">underlined</xsl:attribute>
								<xsl:attribute name="href"><xsl:value-of select='$servletURL' />?action=rescheduleWorkflow&amp;workflowjobname=<xsl:value-of select='./name' />&amp;sessionid=<xsl:value-of select='$sessid' />&amp;authServiceURL=<xsl:value-of select='$authServiceURL' />&amp;authorizationURL=<xsl:value-of select='$authorizationURL' />&amp;workflowname=<xsl:value-of select='./jobParam[@name=&apos;workflowname&apos;]/value' />&amp;workflowid=<xsl:value-of select='./jobParam[@name=&apos;workflowid&apos;]/value' />&amp;karid=<xsl:value-of select='./jobParam[@name=&apos;karid&apos;]/value' />&amp;qformat=<xsl:value-of select='$qformat' />&amp;forwardto=scheduleWorkflowRunMain.jsp</xsl:attribute>
								Enable
							</xsl:otherwise>
							</xsl:choose>
						</a>|						
						<xsl:choose>
						<xsl:when test="./status = 'unscheduled'">
							<a>
								<xsl:attribute name="class">underlined</xsl:attribute>
								<xsl:attribute name="href"><xsl:value-of select='$servletURL' />?action=deleteScheduledWorkflow&amp;workflowjobname=<xsl:value-of select='./name' />&amp;sessionid=<xsl:value-of select='$sessid' />&amp;authServiceURL=<xsl:value-of select='$authServiceURL' />&amp;authorizationURL=<xsl:value-of select='$authorizationURL' />&amp;workflowname=<xsl:value-of select='./jobParam[@name=&apos;workflowname&apos;]/value' />&amp;workflowid=<xsl:value-of select='./jobParam[@name=&apos;workflowid&apos;]/value' />&amp;karid=<xsl:value-of select='./jobParam[@name=&apos;karid&apos;]/value' />&amp;qformat=<xsl:value-of select='$qformat' />&amp;forwardto=scheduleWorkflowRunMain.jsp</xsl:attribute>
								Delete	
							</a>
						</xsl:when>
						<xsl:otherwise>
							Delete
						</xsl:otherwise>
						</xsl:choose>		
					</div>		
				</div>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>