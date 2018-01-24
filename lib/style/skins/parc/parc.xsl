<?xml version="1.0"?>
<!--
	*  '$RCSfile$'
	*      Authors: Matt Jones, Chad Berkley
	*    Copyright: 2000-2007 Regents of the University of California and the
	*               National Center for Ecological Analysis and Synthesis
	*  For Details: http://www.nceas.ucsb.edu/
	*
	*   '$Author: daigle $'
	*     '$Date: 2009-04-14 11:13:23 -0600 (Tue, 14 Apr 2009) $'
	* '$Revision: 4918 $'
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:eml="eml://ecoinformatics.org/eml-2.0.1" 
	            version="1.0">
    <xsl:import href="../../common/resultset-table.xsl"/>
    <!-- xsl:import href="../../common/eml-2.0.1/emlroot.xsl"/ -->
    <xsl:import href="../../common/eml-2/emlroot.xsl"/>
    <xsl:import href="../../common/fgdc/fgdc-root.xsl"/>
    <xsl:import href="parc-common.xsl"/>
	
	<xsl:template match="/">
		<html>
		
			<xsl:call-template name="documenthead"/>
			
			<body id="Overview" onload="loginStatus('{$contextURL}/metacat', '{$cgi-prefix}');">
				<div id="main_wrapper">
					
				    <xsl:call-template name="bodyheader"/>
				    
					<div id="content_wrapper">
					                        
                   		<xsl:if test="*[local-name()='eml']">     	
                            <xsl:call-template name="emldocument"/>
                        </xsl:if>
                        
                        <xsl:if test="*[local-name()='resultset']">     	
                            <xsl:call-template name="resultstable"/>
                        </xsl:if>
                        
                        <xsl:if test="*[local-name()='metadata']">     	
                            <xsl:call-template name="metadata"/>
                        </xsl:if>
                        
						<xsl:call-template name="bodyfooter"/>
						
					</div><!-- id="content_wrapper"-->
					
				</div><!-- id="main_wraper"-->
				
			</body>
			
		</html>
	</xsl:template>

</xsl:stylesheet>
