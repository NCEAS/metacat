<?xml version="1.0"?>
<!--
*  '$RCSfile$'
*      Authors: Matt Jones, CHad Berkley
*    Copyright: 2000 Regents of the University of California and the
*               National Center for Ecological Analysis and Synthesis
*  For Details: http://www.nceas.ucsb.edu/
*
*   '$Author$'
*     '$Date$'
* '$Revision$'
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
        <title>Components Search Results</title>
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

a
{
    color: inherit;
    font-weight: inherit;
    font-size: inherit; 
    text-decoration: underline;
}


div.body
{
    margin-left: 2em;
    margin-right: 2em;
}

th.tablehead
{
    text-align: left;
}

th.tablehead,
td.text_plain
{
    padding: 0.375em 0.75em 0;
}

th.tablehead:first-child,
td.text_plain:first-child
{
    padding: 0.375em 0.375em 0;
}

td.text_plain
{
    vertical-align: top;
}

tr.entry td
{
    border-bottom: 1px solid #ddd;
}

tr.entry + tr td
{
    border-bottom: 1px solid #066;
    padding-top: 0.375em;
    padding-bottom: 0.375em;
}

.dl
{
    background: #eeeeee url(<xsl:value-of select="contextURL"/>/style/skins/<xsl:value-of select="$qformat"/>/download2.png) 2px center no-repeat;
    border: 1px solid black;
    font-weight: bold;
    text-decoration: none;
    padding: 0.063em 0.375em 0.063em 20px;
    font-size: smaller;
}

        </style>
      </head>

      <body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
        <script language="JavaScript">
          <![CDATA[
          insertTemplateOpening("]]><xsl:value-of select="$contextURL" /><![CDATA[");
          insertSearchBox("]]><xsl:value-of select="$contextURL" /><![CDATA[");
         ]]>
        </script>

        <div class="body">
          <xsl:if test="/resultset/pagesize = 0">
            <p class="emphasis">
              <xsl:number value="count(resultset/document)" /> components found
            </p>
          </xsl:if>
          
          <!-- paging code here -->
          <xsl:if test="/resultset/pagesize &gt; 0">
          <table cellspacing="0" cellpadding="0" style="width: 50%;">
          <tr>
          <td>
          <xsl:choose>
            <xsl:when test="/resultset/previouspage != -1">
              <div class="emphasis">
                <a style="font-size: smaller;">
                          <xsl:attribute name="href">
                            <xsl:text>metacat?action=query&amp;operator=INTERSECT&amp;enableediting=false</xsl:text>
                            
                            <xsl:if test="count(/resultset/query/pathquery/querygroup/queryterm/value) &gt; 0">
                              <xsl:text>&amp;anyfield=</xsl:text>
                              <xsl:value-of select="/resultset/query/pathquery/querygroup/queryterm/value"/>
                            </xsl:if>
                            
                            <xsl:if test="count(/resultset/query/pathquery/querygroup/queryterm/value) &lt;= 0">
                              <xsl:text>&amp;anyfield=%25</xsl:text>
                            </xsl:if>
                            
                            <xsl:text>&amp;qformat=</xsl:text>
                            <xsl:value-of select="$qformat"/>
                            
                            <xsl:text>&amp;sessionid=</xsl:text>
                            <xsl:value-of select="$sessid"/>
                            
                            <xsl:for-each select="/resultset/query/pathquery/returndoctype">
                              <xsl:text>&amp;returndoctype=</xsl:text>
                              <xsl:value-of select="."/>
                            </xsl:for-each>
                            
                            <xsl:for-each select="/resultset/query/pathquery/returnfield">
                              <xsl:text>&amp;returnfield=</xsl:text>
                              <xsl:value-of select="."/>
                            </xsl:for-each>
                            
                            <xsl:text>&amp;pagestart=</xsl:text>
                            <xsl:value-of select="/resultset/previouspage"/>
                            
                            <xsl:text>&amp;pagesize=</xsl:text>
                            <xsl:value-of select="/resultset/pagesize"/>
                          </xsl:attribute>
    
                          <xsl:text>Previous Page</xsl:text>
                        </a>
              </div>
            </xsl:when>
            <xsl:otherwise>
              <div class="emphasis-grey">
                Previous Page
              </div>
            </xsl:otherwise>
          </xsl:choose>
          </td>

          <td>
          <xsl:choose>
            <xsl:when test="/resultset/lastpage = 'false'">
              <div class="emphasis">
                <a style="font-size: smaller;">
                          <xsl:attribute name="href">
                            <xsl:text>metacat?action=query&amp;operator=INTERSECT&amp;enableediting=false</xsl:text>
                            
                            <xsl:if test="count(/resultset/query/pathquery/querygroup/queryterm/value) &gt; 0">
                              <xsl:text>&amp;anyfield=</xsl:text>
                              <xsl:value-of select="/resultset/query/pathquery/querygroup/queryterm/value"/>
                            </xsl:if>
                            
                            <xsl:if test="count(/resultset/query/pathquery/querygroup/queryterm/value) &lt;= 0">
                              <xsl:text>&amp;anyfield=%25</xsl:text>
                            </xsl:if>
                            
                            <xsl:text>&amp;qformat=</xsl:text>
                            <xsl:value-of select="$qformat"/>
                            
                            <xsl:text>&amp;sessionid=</xsl:text>
                            <xsl:value-of select="$sessid"/>
                            
                            <xsl:for-each select="/resultset/query/pathquery/returndoctype">
                              <xsl:text>&amp;returndoctype=</xsl:text>
                              <xsl:value-of select="."/>
                            </xsl:for-each>
                            
                            <xsl:for-each select="/resultset/query/pathquery/returnfield">
                              <xsl:text>&amp;returnfield=</xsl:text>
                              <xsl:value-of select="."/>
                            </xsl:for-each>
                            
                            <xsl:text>&amp;pagestart=</xsl:text>
                            <xsl:value-of select="/resultset/nextpage"/>
                            
                            <xsl:text>&amp;pagesize=</xsl:text>
                            <xsl:value-of select="/resultset/pagesize"/>
                          </xsl:attribute>
    
                          <xsl:text>Next Page</xsl:text>
                        </a>
              </div>
            </xsl:when>
            <xsl:otherwise>
              <div class="emphasis-grey">
                Next Page
              </div>
            </xsl:otherwise>
          </xsl:choose>
          </td>
          </tr>
          </table>
          </xsl:if>
          <!-- end paging code -->
          
          <!-- This tests to see if there are returned documents,
              if there are not then don't show the query results -->

          <xsl:if test="count(resultset/document) &gt; 0">
            <table cellspacing="0" cellpadding="0" width="100%">
              <tr>
                <th class="tablehead" style="width: 1px;"></th>
				<th class="tablehead" style="width: 1px;">Kar</th>
                <th class="tablehead" style="width: 1px;">Component</th>
                <th class="tablehead" style="width: 15em;">Author</th>
                <th class="tablehead">Version</th>
                <th class="tablehead"></th>
              </tr>

              <xsl:for-each select="resultset/document">
                <xsl:sort select="./param[@name='karEntry/karEntryXML/entity/@name']"/>
                <xsl:variable name="sq">'</xsl:variable>
                <tr class="entry">

                  <td class="text_plain">
                    <xsl:value-of select="position()"/>.
                  </td>

				<td class="text_plain" style="white-space: nowrap;">
					<xsl:choose>
						<xsl:when test="./param[@name='karFileName']!=''">
						  <b style="font-size: larger;">
							<xsl:value-of select="substring-before(./param[@name='karFileName'], '.')"/>
						  </b>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="'Null'"/>
						</xsl:otherwise>	
					</xsl:choose>
                  </td>
					
                  <td class="text_plain">
					  <xsl:for-each select="./param[@name='karEntry/karEntryXML/entity/@name']">
						  
						  <xsl:value-of select="."/>
					      <xsl:if test="position() != last()">
                              <xsl:value-of select="', '"/>
                          </xsl:if>
					  </xsl:for-each>
                  </td>

                  <td class="text_plain">
                    <xsl:for-each select="./param[@name=concat('karEntry/karEntryXML/entity/property[@name=', $sq, 'author', $sq, ']/configure')]">
					      <xsl:value-of select="."/>
                          <xsl:if test="position() != last()">
                              <xsl:value-of select="', '"/>
                          </xsl:if>
					</xsl:for-each>
                  </td>

                  <td class="text_plain">
                    <xsl:for-each select="./param[@name=concat('karEntry/karEntryXML/entity/property[@name=', $sq, 'KeplerDocumentation', $sq, ']/property[@name=', $sq, 'version', $sq, ']/configure')]">
						  <xsl:value-of select="."/>
                          <xsl:if test="position() != last()">
                              <xsl:value-of select="', '"/>
                          </xsl:if>
					</xsl:for-each>
                  </td>

                  <td class="text_plain">
                    <a class="dl">
					 <xsl:variable name="karDocid">
                            <xsl:call-template name="extractDocidFromLsid">
                                <xsl:with-param name="lsidString" select="./param[@name='mainAttributes/lsid']" />
                            </xsl:call-template>
                        </xsl:variable>
                      <xsl:attribute name="href">
                        <!--<xsl:value-of select="$httpServer"/><xsl:text>/authority/data?lsid=</xsl:text>
                        <xsl:value-of select="./param[@name='mainAttributes/lsid']"/>-->
						 <xsl:text>metacat?</xsl:text>
                        <xsl:text>&amp;action=read&amp;docid=</xsl:text>
                        <xsl:value-of select="$karDocid"/>
                      </xsl:attribute>

                      <xsl:text>Download</xsl:text>
                    </a>
                  </td>
                </tr>

                <tr>
                  <td class="text_plain"></td>
                  <td class="text_plain" colspan="5">
                    <xsl:variable name="docClip" select="substring(./param[@name=concat('karEntry/karEntryXML/entity/property[@name=', $sq, 'KeplerDocumentation', $sq, ']/property[@name=', $sq, 'userLevelDocumentation', $sq, ']/configure')], 0, 200)"/>
                    <xsl:value-of select="$docClip"/>
                    <xsl:text> </xsl:text>
                    <!-- <xsl:if test="count(/entity/property[@name='KeplerDocumentation']/property[@name='userLevelDocumentation']/configure) &gt; 0">
                      <p><xsl:value-of select="/entity/property[@name='KeplerDocumentation']/property[@name='userLevelDocumentation']/configure"/></p>
                    </xsl:if> -->
                    <!-- <xsl:value-of select="substring(./param[@name=concat('entity/property[@name=', $sq, 'KeplerDocumentation', $sq, ']/property[@name=', $sq, 'userLevelDocumentation', $sq, ']/configure')], 0, 200 - $docClip)"/> -->
                    <!--<xsl:for-each select="./param[@name=concat('entity/property[@name=', $sq, 'KeplerDocumentation', $sq, ']/property[@name=', $sq, 'userLevelDocumentation', $sq, ']/configure/p')]">
                      <p>
                        <xsl:value-of select="."/>
                      </p>
                    </xsl:for-each>-->
                    <xsl:text>... [</xsl:text>

                    <a style="font-size: smaller;">
                      <xsl:attribute name="href">
                        <xsl:text>metacat?qformat=</xsl:text>
                        <xsl:value-of select="$qformat"/>
                        <xsl:text>&amp;sessionid=</xsl:text>
                        <xsl:value-of select="$sessid"/>
                        <xsl:text>&amp;action=read&amp;docid=</xsl:text>
                        <xsl:value-of select="./docid"/>

                        <xsl:if test="$enableediting = 'true'">
                          <xsl:text>&amp;enableediting=</xsl:text>
                          <xsl:value-of select="$enableediting"/>
                        </xsl:if>
                      </xsl:attribute>

                      <xsl:text>View Documentation</xsl:text>
                    </a>

                    <xsl:text>]</xsl:text>
                  </td>
                </tr>
              </xsl:for-each>
            </table>
          </xsl:if>
        </div>
        <script language="JavaScript">
          <![CDATA[
          insertTemplateClosing("]]><xsl:value-of select="$contextURL" /><![CDATA[");
         ]]>
        </script>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>

