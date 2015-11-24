<?xml version="1.0"?>
<!--
*  '$RCSfile: resultset.xsl,v $'
*      Authors: Matt Jones, Chad Berkley
*    Copyright: 2000 Regents of the University of California and the
*         National Center for Ecological Analysis and Synthesis
*  For Details: http://www.nceas.ucsb.edu/
*
*   '$Author: cjones $'
*     '$Date: 2004/10/05 23:50:46 $'
* '$Revision: 1.1 $'
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
<!-- 2006-05-01 mob: link to data entity is now a form, submits to a cgi requesting some user info, then on to data. 
required another javascript (original one contains metacat params) and a template to generate the form.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <!-- import the header, footer, and sidebars for customized styling -->
  <xsl:import href="./pageheader.xsl"/>
  <xsl:import href="./pagefooter.xsl"/>
  <xsl:import href="./page_leftsidebar.xsl"/>
  <xsl:import href="./page_rightsidebar.xsl"/>
  <xsl:import href="./loginbox.xsl"/>
  <xsl:import href="./searchbox.xsl"/>
  
  <xsl:param name="cgi-prefix"/>
  <xsl:param name="sessid"/>
  <xsl:param name="enableediting">false</xsl:param>
  <xsl:param name="contextURL"/>
  <!-- This parameter gets overidden by the chosen default qformat -->
  <xsl:param name="qformat">default</xsl:param>
  <xsl:param name="servletURL"/>

  <!-- send the resultset back to the browser styled in HTML -->
  <xsl:output method="html" encoding="iso-8859-1" indent="yes" standalone="yes"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" />
	
	<!-- this param set to require a user to accept a datause agreement 
  <xsl:param name="visit_datauseagreement">true</xsl:param> -->

  <!-- The main template matches the XML document root -->
  <xsl:template match="/">
   <html>
      <head>
        <link rel="stylesheet" type="text/css" 
              href="{$contextURL}/style/skins/sbclter/sbclter.css" />
        <script language="Javascript" type="text/JavaScript"
          src="{$contextURL}/style/skins/sbclter/sbclter.js" />
        <script language="Javascript" type="text/JavaScript"
          src="{$contextURL}/style/common/branding.js" />
        <script language="JavaScript">
          <![CDATA[
          function submitform(action,form_ref) {
              form_ref.action.value=action;
              form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
              form_ref.qformat.value="sbclter";
              form_ref.submit();
          }
          ]]>
        </script>
        <script language="JavaScript">
          <![CDATA[  
          function view_entity(form_ref) {
               form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
              form_ref.qformat.value="sbclter";
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
          <div id="data-catalog-area">

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
                 <!--<th>Type</th> -->
                <th class="wide_column">Title (follow link for metadata)</th>
                <th>Principal Investigators</th>
                <!-- <th>Organization</th> -->
                <th>Download Data Tables (no metadata)</th>
                <xsl:if test="$enableediting = 'true'">
                  <th>Actions</th>
                </xsl:if>
              </tr>
          
            <xsl:for-each select="resultset/document">
              <xsl:sort select="./param[@name='eml/dataset/title']"/>
              <tr>
              
               <!-- style the keyword table-cell for data type-->
               <!-- removed: too many keywords
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
              
              
 <!-- style the Title table-cell for data type-->
                <td>
                <xsl:attribute name="class">
                  <xsl:choose>
                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                  </xsl:choose>
                </xsl:attribute>
          
              <!--
               Build a submit form to view the data package details, using hidden fields to POST the query values.
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
                  <xsl:when test="./param[@name='eml/dataset/title']!=''">
               <xsl:value-of select="./param[@name='eml/dataset/title']"/>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="./param[@name='eml/citation/title']"/>
                    <xsl:value-of select="./param[@name='eml/software/title']"/>
                    <xsl:value-of select="./param[@name='eml/protocol/title']"/>
                  </xsl:otherwise>
                </xsl:choose>
              </a>

              <!-- Include the docid in the title output -->
              <xsl:text>(</xsl:text>
                <xsl:value-of select="./docid"/>
              <xsl:text>)</xsl:text>
                  </form>
                </td>
          
                
 <!-- style the table-cell for PIs -->
                <td>
                <xsl:attribute name="class">
                  <xsl:choose>
                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                  </xsl:choose>
                </xsl:attribute>
          
                  <xsl:for-each select="./param[@name='eml/dataset/creator/individualName/surName']" >
              <xsl:value-of select="." />
                  <br />
             </xsl:for-each> 
             </td>
 
<!-- style the table-cell for the data tables-->
							<!--
                 create links to each of this package's data entities. 
                 needed to edit the url first, then pass it to the form template with param.
                 mob replaced this section 2006-05-01 -->              
                <td>
                <xsl:attribute name="class">
                  <xsl:choose>
                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                  </xsl:choose>
                </xsl:attribute>                 
                  <!-- Check the url, edit string if it uses ecogrid protocol. -->               
                  <xsl:for-each select="./param[@name='eml/dataset/dataTable/entityName']"> 
                    <xsl:variable name="entity_name" select="."/>
                  <xsl:variable name="URL"
                    select="following-sibling::param[@name='eml/dataset/dataTable/physical/distribution/online/url']" />                 
                  <xsl:choose>
                    <!-- 
                      if the content of url is to external data table -->
                    <xsl:when test="starts-with($URL,'http')">            
                      <xsl:variable name="URL1" select="$URL"/>
                        <xsl:call-template name="data_use_agreement_form">
                            <xsl:with-param name="entity_name" select="$entity_name"/>
                            <xsl:with-param name="URL1" select="$URL1"/>
                        </xsl:call-template>
                    </xsl:when>                 
                    <!-- 
                      if URL uses ecogrid protocol, strip off table name, then create metacat query  -->
                    <xsl:when test="starts-with($URL,'ecogrid')">                          
                      <xsl:variable name="URLsubstr" select="substring-after($URL, 'ecogrid://')"/>
                      <xsl:variable name="docID" select="substring-after($URLsubstr, '/')"/>
                       <xsl:variable name="URL1">
                       	<xsl:text>{$servletURL}?action=read&amp;qformat=sbclter</xsl:text><xsl:text>&amp;docid=</xsl:text><xsl:value-of select="$docID"/>
                           
                        <!-- <xsl:text>http://sbcdata.lternet.edu/catalog/metacat?action=read&amp;qformat=</xsl:text>
                        	<xsl:value-of select="$qformat"/>
                          <xsl:text>&amp;docid=</xsl:text>
                          <xsl:value-of select="$docID"/> -->
                       </xsl:variable>
                       <xsl:call-template name="data_use_agreement_form">
                            <xsl:with-param name="entity_name" select="$entity_name"/>
                            <xsl:with-param name="URL1" select="$URL1"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                         <!-- 
                         else, assume the url content is the just name of a metacat table -->
                       <xsl:variable name="URL1">
                          <xsl:text>{$servletURL}?action=read&amp;qformat=sbclter</xsl:text>
                           <xsl:text>&amp;docid=</xsl:text>
                           <xsl:value-of select="$URL"/>
                       </xsl:variable>
                       <xsl:call-template name="data_use_agreement_form">
                            <xsl:with-param name="entity_name" select="$entity_name"/>
                            <xsl:with-param name="URL1" select="$URL1"/>
                       </xsl:call-template>
                    </xsl:otherwise>                
                  </xsl:choose>
                  </xsl:for-each>                 
                </td>             
  
								
								
								
	               <!-- 
								 
                   if editing is turned on, add some buttons -->
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
               <input type="hidden" name="qformat" value="sbclter"/>
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
                   <input type="hidden" name="cfg" value="sbclter"/>
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
                   <input type="hidden" name="cfg" value="sbclter"/>
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

         </div>  <!-- end data-catalog-area -->
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
  <!-- 


  template to display data use agreement form. 
  mob, 2006-05-01 -->
  <xsl:template name="data_use_agreement_form">
      <xsl:param name="entity_name"/>
      <xsl:param name="URL1"/>
      <!-- create form to pass url and entity's name to cgi with data agreement page. form name must be unique/dynamic -->
      <form action="{$cgi-prefix}/data-use-agreement.cgi" method="POST"> 
      <!-- <form action="http://sbcdata.lternet.edu/cgi-bin/data-use-agreement.cgi" method="POST">      -->
      	<xsl:attribute name="name">
		<xsl:value-of select="translate($entity_name,',:()-. ' ,'')" />
        </xsl:attribute>
        <input type="hidden" name="qformat" />
        <input type="hidden" name="sessionid" />
        <xsl:if test="$enableediting = 'true'">
		<input type="hidden" name="enableediting" value="{$enableediting}"/>
        </xsl:if>
	<input type="hidden" name="url">
		<xsl:attribute name="value">
              	 	<xsl:value-of select="$URL1"/>
              	</xsl:attribute>
        </input>   
        <input type="hidden" name="entityName">
             <xsl:attribute name="value">
               <xsl:value-of select="$entity_name"/>
             </xsl:attribute>
        </input>
        <a>      
        	<xsl:attribute name="href">javascript:view_entity(document.<xsl:value-of select="translate($entity_name,',:()-. ' ,'')" />)</xsl:attribute>
		<xsl:value-of select="$entity_name"/>     <!-- the entity name forms the anchor text --> 
	 </a>                           
	 <br/>                     
    </form>
  </xsl:template>
</xsl:stylesheet>
