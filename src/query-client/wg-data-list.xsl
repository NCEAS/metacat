<?xml version="1.0"?>
<!--
*  '$RCSfile$'
*    Copyright: 2005 Regents of the University of California and the
*               National Center for Ecological Analysis and Synthesis
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
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text"/>
  <xsl:template match="/">
         <xsl:for-each select="resultset/document">
           <xsl:sort select="./param[@name='dataset/creator/organizationName']"/>
           <xsl:value-of select="./docid"/>,<xsl:value-of select="./param[@name='creator/organizationName' and starts-with(text(),'NCEAS ')]"/><xsl:text>
</xsl:text>
         </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
