<?xml version="1.0"?>
<!--
*  '$RCSfile$'
*      Authors: Chris Jones
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

  <xsl:output method="html" encoding="iso-8859-1" indent="yes" standalone="yes"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" />
    
  <xsl:template name="page_leftsidebar">
    <!-- begin the left sidebar area -->
    <xsl:comment>begin the left sidebar area</xsl:comment>
    <xsl:comment>
      these div's must have closing elements for the css to work. Don't
      reduce them to &lt;div id="blah" /&gt; 
    </xsl:comment>
   
    <!--
    The following div has purposefully been condensed to one line in order to
    deal with an MSIE bug that introduces whitespace incorrectly when
    rendering the CSS. Please keep it all on one line in the code. When not
    condensed, it would look like:
   
    <div id="left_sidebar">
      <img src="{$contextURL}/style/skins/{$qformat}/images/nav_data_catalog_white.jpg" alt="data catalog" />
      <img src="{$contextURL}/style/skins/{$qformat}/images/nav_search_orange.jpg" alt=""/>
      <img src="{$contextURL}/style/skins/{$qformat}/images/nav_kelp.jpg" alt="" />
    </div>
    -->
    <div id="left_sidebar"><img src="{$contextURL}/style/skins/{$qformat}/images/nav_data_catalog_white.jpg" alt="data catalog" /><img src="@systemidserver@@style-skins-path@/{$qformat}/images/nav_search_orange.jpg" alt=""/><img src="@systemidserver@@style-skins-path@/{$qformat}/images/nav_kelp.jpg" alt="" /></div>
   
    <!--
    The lines below may be used in the above div based on which images should
    be in the navigation bar.
    -->
    <!--img src="{$contextURL}/style/skins/{$qformat}/images/nav_data_catalog_orange.jpg" alt="" /-->
    <!--img src="{$contextURL}/style/skins/{$qformat}/images/nav_login_orange.jpg" alt="" /-->
    <!--img src="{$contextURL}/style/skins/{$qformat}/images/nav_search_white.jpg" -->
    <!--img src="{$contextURL}/style/skins/{$qformat}/images/nav_insert_white.jpg" alt=""/-->
    <!--img src="{$contextURL}/style/skins/{$qformat}/images/nav_insert_orange.jpg" alt="" /-->
    <!--img src="{$contextURL}/style/skins/{$qformat}/images/nav_modify_white.jpg" alt="" /-->
    <!--img src="{$contextURL}/style/skins/{$qformat}/images/nav_modify_orange.jpg" alt="" /-->
    <!--img src="{$contextURL}/style/skins/{$qformat}/images/nav_logout_orange.jpg" alt=""/-->
    <!--img src="{$contextURL}/style/skins/{$qformat}/images/nav_blank_blue.jpg" alt=""/-->
    <!-- end the left sidebar area -->
    <xsl:comment>end the left sidebar area</xsl:comment>
  </xsl:template>

</xsl:stylesheet>
