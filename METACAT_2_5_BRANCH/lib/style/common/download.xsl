<?xml version="1.0"?>
<!--
  *  '$RCSfile$'
  *      Authors: Chad Berkley
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
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" />
  <html>
  <body>
  <xsl:template match="/">
    <h3><u>
      <xsl:value-of select="./param[@name='firstname']"/>
      <xsl:text>&nbsp;</xsl:text>
      <xsl:value-of select="./param[@name='lastname']"/>
    </u></h3>
    <ul>
      <li>
        <xsl:value-of select="./param[@name='email']"/>
      </li>
      <li>
        <xsl:value-of select="./param[@name='acceptlic']"/>
      </li>
      <li>
        <xsl:value-of select="./param[@name='product']"/>
      </li>
      <li>
        <xsl:value-of select="./param[@name='opersys']"/>
      </li>
      <li>
        <b><xsl:value-of select="./param[@name='emailnotice']"/></b>
      </li>
      <li>
        <xsl:value-of select="/download/date"/>
      </li>
    </ul>
  </xsl:tempate>
  </body>
  </html>
</xsl:stylesheet>
