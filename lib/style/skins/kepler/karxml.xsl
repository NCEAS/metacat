<?xml version="1.0" encoding="UTF-8"?>
<!--
*  '$RCSfile$'
*      Authors: Matt Jones, CHad Berkley
*    Copyright: 2000 Regents of the University of California and the
*               National Center for Ecological Analysis and Synthesis
*  For Details: http://www.nceas.ucsb.edu/
*
*   '$Author: tao $'
*     '$Date: 2010-08-13 11:10:50 -0700 (Fri, 13 Aug 2010) $'
* '$Revision: 5498 $'
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
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml"
    version="1.0">
  <xsl:import href="../../common/util.xsl"/>
  <xsl:output method="xml" encoding="utf-8"
              doctype-public="-//W3C//DTD XHTML 1.1//EN"
              doctype-system="http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"
              indent="yes" />

  <xsl:param name="sessid"/>
  <xsl:param name="qformat">default</xsl:param>
  <xsl:param name="enableediting">false</xsl:param>
  <xsl:param name="contextURL"/>
  <xsl:param name="httpServer"/>

  <xsl:template match="/">
    <html xml:lang="en-US">
      <head>
        <title><xsl:value-of select="//karEntry/karEntryXML/entity/@name"/></title>
        <link rel="stylesheet" type="text/css"
            href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />

		<script language="Javascript" type="text/JavaScript"
            src="{$contextURL}/style/skins/{$qformat}/{$qformat}.js">
            <xsl:text disable-output-escaping="yes">
            </xsl:text>
        </script>
        <script language="Javascript" type="text/JavaScript"
            src="{$contextURL}/style/common/branding.js">
          <xsl:text disable-output-escaping="yes">
          </xsl:text>
        </script>
        <style type="text/css">
          <xsl:text disable-output-escaping="yes">
body, a, p, td, tr, h3, h2, h1
{
    font-family: Arial, Helvetica, sans-serif;
}

body
{
    background: #ece9d8;
    margin: 0;
}

tr.top-level > td
{
    text-align: left;
}

tr.top-level > td
{
    background: white;
    border: 0.125em ridge white;
    padding: 0.375em;
}

td tr
{
    vertical-align: top;
}

td td
{
    padding: 0.1875em;
}
</xsl:text>
        </style>
      </head>

      <body>
		  <script language="JavaScript">
          <![CDATA[
          insertTemplateOpening("]]><xsl:value-of select="$contextURL" /><![CDATA[");
         ]]>
        </script>
        <table cellspacing="8" cellpadding="0" style="border: none; width: 100%;" class="main">
          <colgroup></colgroup>
          <colgroup style="width: 75%;"></colgroup>
          <colgroup style="width: 25%;"></colgroup>

          <tr class="top-level">
            <td colspan="3">
              <h1>
                <span style="padding-right: 2em;">
                  <xsl:value-of select="substring-before(//karFileName, '.')"/>
                </span>

                <span style="font-size: smaller;">
					<xsl:variable name="karDocid">
                            <xsl:call-template name="extractDocidFromLsid">
                                <xsl:with-param name="lsidString" select="//mainAttributes/lsid" />
                            </xsl:call-template>
                  </xsl:variable>
                     
                <a>
                   <!--<xsl:attribute name="href"><xsl:value-of select="$httpServer"/>/authority/data?lsid=<xsl:value-of select="/entity/property[@name='karId']/@value"/></xsl:attribute>-->
                    <xsl:attribute name="href">
                         <xsl:text>metacat?</xsl:text>
                        <xsl:text>&amp;action=read&amp;docid=</xsl:text>
                        <xsl:value-of select="$karDocid"/>
                      </xsl:attribute>
                  <xsl:text>Download</xsl:text>
                </a>
                  <!--<xsl:text> (</xsl:text>
                  <xsl:value-of select="//karEntry/karEntryXML/entity/property[@name='class']/@value"/>
                  <xsl:text>)</xsl:text>-->
                </span>
              </h1>
				
		
            </td>

            <!--<td>
              
            </td>-->
          </tr>
			
		 <xsl:for-each select="//karEntry">
		   <xsl:choose>
			   <xsl:when test="./karEntryXML/entity/@name!=''">
              <!-- this is workflow kar entry-->
                  <tr class="top-level">
                    <td>
					   <h3>
						   <xsl:value-of select="./karEntryXML/entity/@name"/>    
						</h3>      
                    </td>
                    
                    <td  colspan="2">
              
                        <table cellspacing="0" cellpadding="0">
             
							  <tr>
                                    <td colspan="2">
                                      <h4>Documentation</h4>
                                    </td>
                                  </tr>
							  <tr>
                                <td style="font-style: italic;">
                                  Author:
                                </td>
                                <td>
									<xsl:choose>
										<xsl:when test="count(karEntryXML/entity/property[@name='author']) &gt; 0">
											<xsl:for-each select="karEntryXML/entity/property[@name='author']">
												<xsl:if test="position() &gt; 1">
												  <xsl:text>, </xsl:text>
											    </xsl:if>
												<xsl:value-of select="configure"/>
										  </xsl:for-each> 
										</xsl:when>
										 <xsl:otherwise>
											  Unkown
										 </xsl:otherwise>
									</xsl:choose>                                
                                </td>
                              </tr>

                            <tr>
                                <td style="font-style: italic;">
                                  Version:
                                </td>
                                <td>
                                <xsl:choose>
                                        <xsl:when test="count(karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='version']) &gt; 0">
                                            <xsl:for-each select="karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='version']">
                                                <xsl:if test="position() &gt; 1">
                                                  <xsl:text>, </xsl:text>
                                                </xsl:if>
                                                <xsl:value-of select="configure"/>
                                          </xsl:for-each> 
                                        </xsl:when>
                                         <xsl:otherwise>
                                              Unkown
                                         </xsl:otherwise>
                                    </xsl:choose>                
                                </td>
                          </tr>              
                         
                  					
					  <!-- <xsl:value-of select="/entity/property[@name='KeplerDocumentation']/property[@name='description']/configure"/> -->                     
				 			
			
							<xsl:if test="count(karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='userLevelDocumentation']/configure) &gt; 0">
									  <tr class="top-level">
										<td colspan="2">    
										  <p><xsl:value-of select="karEntryXML/entity/property[@name='KeplerDocumentation']/property[@name='userLevelDocumentation']/configure"/></p>
									   </td>                   
									  </tr>
							 </xsl:if>        
							 <tr>
                                    <td colspan="2">
                                      <h4>Parameters</h4>
                                    </td>
                                  </tr>
								<xsl:choose>								
									<xsl:when test="count(karEntryXML/entity/property[@name='KeplerDocumentation']/property[substring(@name, 1, 5)='prop:']) &gt; 0">
									  <xsl:for-each select="karEntryXML/entity/property[@name='KeplerDocumentation']/property[substring(@name, 1, 5)='prop:']">
										<tr>
					                      <td style="font-style: italic;">
										    <xsl:value-of select="substring(@name, 6)"/>
										  </td>
										  <td>
											<xsl:value-of select="configure"/>
										  </td>
										</tr>
									  </xsl:for-each>
									</xsl:when>
								    <xsl:otherwise>
										<tr>
											<td>
												No description
											</td>
										</tr>
										
									</xsl:otherwise>	
								</xsl:choose>
                                <!--<tr>
									<td colspan="2">
										  <h4>Ports</h4>
									</td>
								 </tr>
								<xsl:choose>
									<xsl:when test="count(karEntryXML/entity/property[@name='KeplerDocumentation']/property[substring(@name, 1, 5)='port:']) &gt; 0">                 
									  <xsl:for-each select="karEntryXML/entity/property[@name='KeplerDocumentation']/property[substring(@name, 1, 5)='port:']">
										<tr>
										  <td style="font-style: italic;">
												<xsl:value-of select="substring(@name, 6)"/>
										  </td>
										  <td>
											<xsl:value-of select="configure"/>
										  </td>
										</tr>
									  </xsl:for-each>
									</xsl:when>
									<xsl:otherwise>
										<tr>
                                          <td style="font-style: italic;">
                                                 No description
                                          </td>
										</tr>                                      
                                    </xsl:otherwise>
								</xsl:choose> -->            
				  </table>   
			   </td> 
              </tr>
                   
              
			 </xsl:when>
			  <xsl:otherwise>
				  <!-- this is for workflow run, reporting and et al-->
				  <tr class="top-level">
                    <td>
					   <h3>
                         <xsl:value-of select="karEntryAttributes/Name"/>
						</h3>          
                    </td>
                    <td colspan="2"> 
						<table cellspacing="0" cellpadding="0">  
							<tr>
                               <td>
                                      <h4>Type</h4>
                                 </td>
							</tr>
							<tr>
                               <td>
                                      <xsl:value-of select="karEntryAttributes/type"/>
                                 </td>
                            </tr>                                
					  </table>
                    </td>
                  </tr>
			  </xsl:otherwise>
		   </xsl:choose>
		 </xsl:for-each>
        </table>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>

