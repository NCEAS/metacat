<?xml version="1.0"?>
<!--
*  '$RCSfile: searchbox.xsl,v $'
*      Authors: Chris Jones
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
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" 
  xmlns:url="http://www.jclark.com/xt/java/java.net.URLEncoder" exclude-result-prefixes="url">
  
  <!-- This parameter gets overidden by the chosen default qformat -->
  <xsl:param name="qformat">default</xsl:param>
  <xsl:param name="servletURL"/>

  <xsl:output method="html" encoding="iso-8859-1" indent="yes" standalone="yes"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" />
    
  <xsl:template name="searchbox">
    <!-- begin search form area -->
    <xsl:comment>begin the search form area</xsl:comment>
    <div id="search-box">
      <table class="group group_border">
        <tr>
          <th>
            Show all Datasets:
          </th> 
          <th>
            Search by Keyword:
          </th>
        </tr>
        <tr>
          <td>           
   

         
      
          <xsl:variable name="my_query"><![CDATA[<?xml version="1.0"?>
             <pathquery version="1.2">
              <returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>
              <returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>
              <returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>
              <returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>
              <returnfield>eml/dataset/title</returnfield>
              <returnfield>eml/dataset/dataTable/entityName</returnfield>
              <returnfield>eml/dataset/creator/individualName/surName</returnfield>
              <returnfield>eml/dataset/creator/organizationName</returnfield>
              <returnfield>eml/dataset/dataTable/physical/distribution/online/url</returnfield>
              <querygroup operator="INTERSECT">
                <queryterm casesensitive="false" searchmode="starts-with">
                  <value>SBCLTER:</value>
                  <pathexpr>eml/dataset/title</pathexpr>
                </queryterm>
                <queryterm casesensitive="false" searchmode="equals">
                  <value>public</value>
                  <pathexpr>eml/dataset/access/allow/principal</pathexpr>
                </queryterm>
              </querygroup>
            </pathquery>]]>   <!-- end of cdata query -->
          </xsl:variable>
          <xsl:if test="function-available('url:encode')">
            <a href="{$servletURL}?action=squery&amp;qformat=sbclter&amp;query={url:encode($my_query)}">
              <xsl:text>All SBCLTER Data Packages </xsl:text> 
            </a>
          </xsl:if>         
        </td>
        <td>
          <em>Keyword Search not yet available</em>
        </td>
      </tr>
    </table>
  </div>
  <xsl:comment>end the search form area</xsl:comment>
  <!-- end search form area -->
  
</xsl:template>

</xsl:stylesheet>
