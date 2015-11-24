<?xml version="1.0"?>
<!--
  *  '$RCSfile$'
  *      Authors: Matt Jones
  *    Copyright: 2000 Regents of the University of California and the
  *         National Center for Ecological Analysis and Synthesis
  *  For Details: http://www.nceas.ucsb.edu/
  *
  *   '$Author: barteau $'
  *     '$Date: 2008-01-15 17:12:44 -0800 (Tue, 15 Jan 2008) $'
  * '$Revision: 3689 $'
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
  * convert an XML file that is valid with respect to the eml-dataset.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:import href="../../../../common/eml-2/eml.xsl"/>
  <xsl:import href="./pageheader.xsl"/>
  <xsl:import href="./pagefooter.xsl"/>
  <xsl:import href="./page_leftsidebar.xsl"/>
  <xsl:import href="./page_rightsidebar.xsl"/>

  <xsl:output method="html" encoding="iso-8859-1"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
    indent="yes" />  
  <!-- global variables to store id node set in case to be referenced-->
  <xsl:variable name="ids" select="//*[@id!='']"/>

  <xsl:template match="/">
    <html>
      <head>
        <link rel="stylesheet" type="text/css"
              href="./style/skins/sbclter/sbclter.css" />
      </head>
      <body>
        <!-- begin the header area -->
        <xsl:call-template name="pageheader" />
        <!-- end the header area -->
   
        <!-- begin the left sidebar area -->
        <xsl:call-template name="page_leftsidebar" />
        <!-- end the left sidebar area -->
   
        <!-- begin the content area -->
        <div id="{$mainTableAligmentStyle}">
          <xsl:apply-templates select="*[local-name()='eml']"/>
        </div>
        <!-- end the content area -->
   
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
