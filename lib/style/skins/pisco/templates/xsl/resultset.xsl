<?xml version="1.0"?>
<!--
*  '$RCSfile$'
*      Authors: Matt Jones, Chad Berkley
*    Copyright: 2000 Regents of the University of California and the
*         National Center for Ecological Analysis and Synthesis
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  
  <!-- import the header, footer, and sidebars for customized styling -->
  <xsl:import href="./pageheader.xsl"/>
  <xsl:import href="./pagefooter.xsl"/>
  <xsl:import href="./page_leftsidebar.xsl"/>
  <xsl:import href="./page_rightsidebar.xsl"/>
  <xsl:import href="./loginbox.xsl"/>
  <xsl:import href="./searchbox.xsl"/>

  <!-- send the resultset back to the browser styled in HTML -->
  <xsl:output method="html" encoding="iso-8859-1" indent="yes" standalone="yes"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" />

  <xsl:param name="cgi-prefix"/>
  <xsl:param name="sessid"/>
  <xsl:param name="enableediting">false</xsl:param>
  <xsl:param name="contextURL"/>
  <!-- This parameter gets overidden by the chosen default qformat -->
  <xsl:param name="qformat">default</xsl:param>
  <xsl:param name="servletURL"/>

  <!-- The main template matches the XML document root -->
  <xsl:template match="/">
   <html>
      <head>
        <link rel="stylesheet" type="text/css" 
              href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />
        <script language="Javascript" type="text/JavaScript"
          src="{$contextURL}/style/skins/{$qformat}/{$qformat}.js" />
        <script language="Javascript" type="text/JavaScript"
          src="{$contextURL}/style/common/branding.js" />
        <script language="JavaScript">
          <![CDATA[
          function submitform(action,form_ref) {
              form_ref.action.value=action;
              form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
              form_ref.qformat.value="]]><xsl:value-of select="$qformat" /><![CDATA[";
              form_ref.submit();
          }
          ]]>
        </script>
      </head>

      <body>
        <!-- begin the header area -->
        <xsl:call-template name="pageheader" />
        <!-- end the header area -->
   
        <!-- begin the left sidebar area -->
        <xsl:call-template name="page_leftsidebar" />
        <!-- end the left sidebar area -->
   
        <!-- begin the content area -->
        <div id="content">

          <!-- begin login form area -->
          <xsl:call-template name="loginbox" />
          <!-- end login form area -->

          <!-- begin search form area -->
          <xsl:call-template name="searchbox" />
          <!-- end search form area -->

          <!-- begin results section (XSLT generated) -->

          <!-- State how many package hits were returned -->
          <xsl:choose>
            <xsl:when test="count(resultset/document)=1">
              <p>
              <xsl:number value="count(resultset/document)" /> data package found:
              </p>
            </xsl:when>
            <xsl:otherwise>
              <p>
              <xsl:number value="count(resultset/document)" /> data packages found:
              </p>
            </xsl:otherwise>
          </xsl:choose>
          
          <!-- This tests to see if there are returned documents,
          if there are not then don't show the query results -->
          <xsl:if test="count(resultset/document) &gt; 0">

            <!-- create the results table, and style each of the returnfield that
            were specified in the original query -->
            <table class="group group_border">
              <tr>
                <th class="wide_column">Title</th>
                <th>Contacts</th>
                <th>Organization</th>
                <th>Associated Data</th>
                <xsl:if test="$enableediting = 'true'">
                  <th>Actions</th>
                </xsl:if>
              </tr>
          
            <xsl:for-each select="resultset/document">
              <xsl:sort select="./param[@name='dataset/title']"/>
              <tr>
                <td>
                <xsl:attribute name="class">
                  <xsl:choose>
                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                  </xsl:choose>
                </xsl:attribute>
          
                  <!--
                  Build a submit form to view the data package details, using hidden
                  fields to POST the query values.
                  --> 
                  <form action="{$servletURL}" method="POST">
              <xsl:attribute name="name">
                <xsl:value-of select="translate(./docid, '()-.', '')" />
              </xsl:attribute>
                    <input type="hidden" name="qformat" />
                    <input type="hidden" name="sessionid" />
              <xsl:if test="$enableediting = 'true'">
                <input type="hidden" name="enableediting" value="{$enableediting}"/>
              </xsl:if>
                    <input type="hidden" name="action" value="read"/>
                    <input type="hidden" name="docid">
                <xsl:attribute name="value">
                  <xsl:value-of select="./docid"/>
                </xsl:attribute>
                    </input>
              <xsl:for-each select="./relation">
                <input type="hidden" name="docid">
                  <xsl:attribute name="value" >
                    <xsl:value-of select="./relationdoc" />
                  </xsl:attribute>
                </input>
              </xsl:for-each>

              <!-- Create the link on the title column -->
              <a>
                <xsl:attribute name="href">javascript:submitform('read',document.<xsl:value-of select="translate(./docid, '()-.', '')"/>)</xsl:attribute>
                <xsl:choose>
                  <xsl:when test="./param[@name='dataset/title']!=''">
               <xsl:value-of select="./param[@name='dataset/title']"/>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="./param[@name='citation/title']"/>
                    <xsl:value-of select="./param[@name='software/title']"/>
                    <xsl:value-of select="./param[@name='protocol/title']"/>
                  </xsl:otherwise>
                </xsl:choose>
              </a>

              <!-- Include the docid in the title output -->
              <xsl:text>(</xsl:text>
                <xsl:value-of select="./docid"/>
              <xsl:text>)</xsl:text>
                  </form>
                </td>
          
              <!-- style the contacts returned -->
                <td>
                <xsl:attribute name="class">
                  <xsl:choose>
                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                  </xsl:choose>
                </xsl:attribute>
          
                  <xsl:for-each select="./param[@name='originator/individualName/surName']" >
              <xsl:value-of select="." />
                  <br />
             </xsl:for-each>
                  <xsl:for-each select="./param[@name='creator/individualName/surName']" >
              <xsl:value-of select="." />
                  <br />
                  </xsl:for-each>
                </td>

              <!-- style the contacts returned -->
                <td>
                <xsl:attribute name="class">
                  <xsl:choose>
                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                  </xsl:choose>
                </xsl:attribute>
          
              <xsl:value-of select="./param[@name='originator/organizationName']" />
              <xsl:value-of select="./param[@name='creator/organizationName']" />
                </td>
          
              <!-- style the keywords returned -->
              <!--
                <td>
                <xsl:attribute name="class">
                  <xsl:choose>
                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                  </xsl:choose>
                </xsl:attribute>
          
                  <xsl:for-each select="./param[@name='keyword']">
                  <xsl:value-of select="." />
                  <br />
                  </xsl:for-each>
                </td>
              -->

              <!-- create links to each of the associated raw data entities -->
                <td>
                <xsl:attribute name="class">
                  <xsl:choose>
                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                  </xsl:choose>
                </xsl:attribute>
                  <!-- take each entity name and link it to the physical object url-->
                  <xsl:for-each select="./param[@name='dataTable/entityName']">
                  <a>
                  <xsl:variable name="URL"
                    select="following-sibling::param[@name='dataTable/physical/distribution/online/url']" />
                    <!-- strip out the ecogrid:// syntax if it's there -->
                    <xsl:choose>
                      <xsl:when test="starts-with($URL,'ecogrid')">
                        <xsl:variable name="URL1" select="substring-after($URL, 'ecogrid://')"/>
                        <xsl:variable name="docID" select="substring-after($URL1, '/')"/>
                        <xsl:attribute name="href">
                          <!-- this needs to be on one line ... -->
                          <xsl:text>{$servletURL}?action=read&amp;qformat=</xsl:text><xsl:value-of select="$qformat"/><xsl:text>&amp;docid=</xsl:text><xsl:value-of select="$docID"/>
                        </xsl:attribute>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:attribute name="href"><xsl:value-of select="$URL"/></xsl:attribute>
                      </xsl:otherwise>
                    </xsl:choose>
                  <!-- show the entityName value as the link -->
                  <xsl:value-of select="." />
                  </a>
                  <br />
                  </xsl:for-each>
                </td>
              
                <xsl:if test="$enableediting = 'true'">
                <td>
                <xsl:attribute name="class">
                  <xsl:choose>
                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                  </xsl:choose>
                </xsl:attribute>
          
                  <form action="{$servletURL}" method="POST">
                    <input type="hidden" name="action" value="read"/>
               <input type="hidden" name="qformat" value="{$qformat}"/>
                 <input type="hidden" name="sessionid"  value="{$sessid}"/>
                    <input type="hidden" name="docid">
                    <xsl:attribute name="value">
                 <xsl:value-of select="./docid"/>
                    </xsl:attribute>
                    </input>
                    <center>
                    <input type="SUBMIT"  value=" View " name="View">
               </input>
                    </center>
                  </form>
              <form action="{$cgi-prefix}/register-dataset.cgi" 
                    method="POST">
                    <input type="hidden" name="stage" value="modify"/>  
                   <input type="hidden" name="cfg" value="{$qformat}"/>
                   <input type="hidden" name="sessionid"  value="{$sessid}"/>
                <input type="hidden" name="docid">
                  <xsl:attribute name="value">
                    <xsl:value-of select="./docid"/>
                  </xsl:attribute>
                </input>
                    <input type="SUBMIT"  value=" Edit " name="Edit">
                 </input>
                  </form>
              <form action="{$cgi-prefix}/register-dataset.cgi" 
                    method="POST">
                    <input type="hidden" name="stage" value="delete"/>  
                   <input type="hidden" name="cfg" value="{$qformat}"/>
                   <input type="hidden" name="sessionid"  value="{$sessid}"/>
                <input type="hidden" name="docid">
                  <xsl:attribute name="value">
                    <xsl:value-of select="./docid"/>
                  </xsl:attribute>
                </input>
                    <input type="submit"  value="Delete" name="Delete">
                 </input>
                  </form>
                </td>    
                </xsl:if>
                </tr>
             </xsl:for-each>
             </table>
           </xsl:if>

        </div>
        <!-- end content area -->

        <!-- begin the right sidebar area -->
        <xsl:call-template name="page_rightsidebar" />
        <!-- end the right sidebar area -->

        <!-- begin the footer area -->
        <xsl:call-template name="pagefooter" />
        <!-- end the footer area -->

      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>
