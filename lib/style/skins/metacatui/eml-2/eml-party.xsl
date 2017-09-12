<?xml version="1.0"?>
<!--
  *  '$RCSfile$'
  *      Authors: Matthew Brooke
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
  * convert an XML file that is valid with respect to the eml-variable.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html" encoding="UTF-8"
              doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
              doctype-system="http://www.w3.org/TR/html4/loose.dtd"
              indent="yes" />  

  <!-- This module is for party member and it is self contained-->

  <xsl:template name="party">
      <xsl:param name="partyfirstColStyle"/>
      <div class="row-fluid">
        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:apply-templates mode="party">
             <xsl:with-param name="partyfirstColStyle" select="$partyfirstColStyle"/>
            </xsl:apply-templates>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates mode="party">
            <xsl:with-param name="partyfirstColStyle" select="$partyfirstColStyle"/>
          </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>
      </div>
  </xsl:template>

  <!-- *********************************************************************** -->


  <xsl:template match="individualName" mode="party">
      <xsl:param name="partyfirstColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
        <div class="control-group">
        	<label class="control-label">Individual</label>
        	<div class="controls" >
	           <b><xsl:value-of select="./salutation"/><xsl:text> </xsl:text>
	           <xsl:value-of select="./givenName"/><xsl:text> </xsl:text>
	           <xsl:value-of select="./surName"/></b>
        	</div>
        </div>	
      </xsl:if>
  </xsl:template>


  <xsl:template match="organizationName" mode="party">
      <xsl:param name="partyfirstColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
        <div class="control-group">
        	<label class="control-label">Organization</label>
        	<div class="controls">
        		<b><xsl:value-of select="."/></b>
        	</div>
        </div>
      </xsl:if>
  </xsl:template>


  <xsl:template match="positionName" mode="party">
      <xsl:param name="partyfirstColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
      <div class="control-group">
        	<label class="control-label">Position</label>
        	<div class="controls">
        		<xsl:value-of select="."/>
        	</div>
       </div>
      </xsl:if>
  </xsl:template>


  <xsl:template match="address" mode="party">
    <xsl:param name="partyfirstColStyle"/>
    <xsl:if test="normalize-space(.)!=''">
      <xsl:call-template name="addressCommon">
         <xsl:with-param name="partyfirstColStyle" select="$partyfirstColStyle"/>
      </xsl:call-template>
    </xsl:if>
    </xsl:template>

   <!-- This template will be call by other place-->
   <xsl:template name="address">
      <xsl:param name="partyfirstColStyle"/>
        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:call-template name="addressCommon">
             <xsl:with-param name="partyfirstColStyle" select="$partyfirstColStyle"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="addressCommon">
             <xsl:with-param name="partyfirstColStyle" select="$partyfirstColStyle"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

   <xsl:template name="addressCommon">
    <xsl:param name="partyfirstColStyle"/>
    <xsl:if test="normalize-space(.)!=''">
    <div class="control-group">
    	<label class="control-label">Address</label>
    	<div class="controls">
    		<address>
			    <xsl:for-each select="deliveryPoint">
			    	<xsl:value-of select="."/><xsl:text>, </xsl:text>
			    </xsl:for-each>
			    <br/>
			    <!-- only include comma if city exists... -->
			    <xsl:if test="normalize-space(city)!=''">
			        <xsl:value-of select="city"/><xsl:text>, </xsl:text>
			    </xsl:if>
			    <xsl:if test="normalize-space(administrativeArea)!='' or normalize-space(postalCode)!=''">
			        <xsl:value-of select="administrativeArea"/><xsl:text> </xsl:text><xsl:value-of select="postalCode"/><xsl:text> </xsl:text>
			    </xsl:if>
			    <xsl:if test="normalize-space(country)!=''">
			      <xsl:value-of select="country"/>
			    </xsl:if>
    		</address>
		</div> 
	</div>
    </xsl:if>
   </xsl:template>

  <xsl:template match="phone" mode="party">
      <xsl:param name="partyfirstColStyle"/>
      <div class="control-group">
      	<label class="control-label">Phone</label>
          <div class="controls">
             <xsl:value-of select="."/>
             <xsl:if test="normalize-space(./@phonetype)!=''">
               <xsl:text> (</xsl:text><xsl:value-of select="./@phonetype"/><xsl:text>)</xsl:text>
             </xsl:if>
          </div>
      </div>
  </xsl:template>


  <xsl:template match="electronicMailAddress" mode="party">
      <xsl:param name="partyfirstColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
       <div class="control-group">
       	<label class="control-label" >
            Email Address
          </label>
          <div class="controls">
            <a><xsl:attribute name="href">mailto:<xsl:value-of select="."/></xsl:attribute>
                    <xsl:value-of select="."/></a>
          </div>
       </div>

      </xsl:if>
  </xsl:template>


  <xsl:template match="onlineUrl" mode="party">
      <xsl:param name="partyfirstColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
      <div class="control-group">
      	<label class="control-label">Web Address</label>
        <div class="controls">
            <a><xsl:attribute name="href"><xsl:value-of select="."/></xsl:attribute>
            <xsl:value-of select="."/></a>
       	</div>
      </div>
    </xsl:if>
  </xsl:template>


    
    <xsl:template match="userId" mode="party">
        <xsl:param name="partyfirstColStyle"/>
        <xsl:if test="normalize-space(.)!=''">
            <div class="control-group">
                <label class="control-label">Id</label>
                <div class="controls">
                    <!-- Display the userId as a link when it appears to be an
                    ORCID. The display of ORCID information is subject to
                    ORCID's guidelines:

                        https://orcid.org/trademark-and-id-display-guidelines

                    We want to display the content as a hyperlinked ORCID when
                    we're reasonably sure the value of the userId element is an
                    ORCID and otherwise just display the non-hyperlinked value
                    of the element.

                    An example serialization of an ORCID in EML is

                        <userId directory="http://orcid.org">
                            http://orcid.org/0000-0003-1315-3818
                        </userId>
                    -->

                    <!-- Set up space-normalized variables for later use. -->
                    <xsl:variable name="directory" select="normalize-space(./@directory)" />
                    <xsl:variable name="value" select="normalize-space(.)" />
                    
                    <xsl:choose>
                        <xsl:when test="starts-with($directory, 'http://orcid.org') or starts-with($directory, 'https://orcid.org') or ($directory = '' and (starts-with($value, 'http://orcid.org') or starts-with(@value, 'https://orcid.org')))">
                            <!-- ORCID Logo -->
                            <a href="https://orcid.org">
                                <img src="img/orcid_64x64.png" style="display: inline; margin-right: 5px;" width="16" height="16" />
                            </a>
                            <!-- ORCID Link-->
                            <xsl:element name="a">
                                <xsl:choose>
                                    <xsl:when test="starts-with($value, 'http://orcid.org') or starts-with($value, 'https://orcid.org')">
                                        <xsl:attribute name="href">
                                            <xsl:value-of select="$value"/>
                                        </xsl:attribute>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:attribute name="href">
                                            <xsl:value-of select="concat('http://orcid.org/', $value)" />
                                        </xsl:attribute>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:value-of select="$value"/>
                            </xsl:element>
                        </xsl:when>
                        <xsl:otherwise>                            
                            <xsl:value-of select="."/>
                        </xsl:otherwise>
                    </xsl:choose>
                </div>
            </div>
        </xsl:if>
    </xsl:template>
    <xsl:template match="text()" mode="party" />
</xsl:stylesheet>
