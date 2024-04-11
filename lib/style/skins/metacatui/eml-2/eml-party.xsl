<?xml version="1.0"?>
<!--
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
  
  <xsl:template match="role" mode="party">
      <xsl:param name="partyfirstColStyle"/>
      <xsl:if test="normalize-space(.)!=''">
        <div class="control-group">
        	<label class="control-label">Role</label>
        	<div class="controls" >
        		
        		<xsl:variable name="role" select="." />
        		
        		<xsl:choose>
			        <xsl:when test="$role='principalInvestigator'">
			           <xsl:text>Principal Investigator</xsl:text>
			        </xsl:when>
			         <xsl:when test="$role='collaboratingPrincipalInvestigator'">
			           <xsl:text>Collaborating Principal Investigator</xsl:text>
			        </xsl:when>
			        <xsl:when test="$role='custodianSteward'">
			           <xsl:text>Custodian / Steward</xsl:text>
			        </xsl:when>
			        <xsl:when test="$role='coPrincipalInvestigator'">
			           <xsl:text>Co-Principal Investigator</xsl:text>
			        </xsl:when>
			        <xsl:otherwise>
			         	<xsl:value-of select="concat(
							  translate(
							    substring($role, 1, 1),
							    'abcdefghijklmnopqrstuvwxyz',
							    'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
							  ),
							  substring($role,2,string-length($role)-1)
							)"/>
			        </xsl:otherwise>
			     </xsl:choose>
        		
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
                                <img src="data:img/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA2ZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMC1jMDYwIDYxLjEzNDc3NywgMjAxMC8wMi8xMi0xNzozMjowMCAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDowNzgwMTE3NDA3MjA2ODExOTk0QzkzNTEzRjZEQTg1NyIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDozM0NDOEJGOEZGNTcxMUUxODdBOEVCODg2RjdCQ0QwOSIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDozM0NDOEJGN0ZGNTcxMUUxODdBOEVCODg2RjdCQ0QwOSIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ1M1IE1hY2ludG9zaCI+IDx4bXBNTTpEZXJpdmVkRnJvbSBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOjlDNTI0NkNBMDgyMDY4MTE5NUZFRDc5MUM2MUUwNEREIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjA3ODAxMTc0MDcyMDY4MTE5OTRDOTM1MTNGNkRBODU3Ii8+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+VsRlJAAABIRJREFUeNrsW01oE0EUnmxrodpiqlgsFGwOqR6UJjnZS2n1Il5avBS9tKFHD7U3D0IR9FwLepSkl4K3FKF40YYK/lzSFBWkhTZCpFjRRhoptgi+F2fDZrLZ7Mxm05lNHwybtMlk3zff+51ZH3FZ5lP9g3DB0QejB0aoylfSMDIwVmEkb0XeJt28P58LCvvhMgJjmF5rIQkYC3gFQHJSAkBXegzGuMukisOYBSDSUgBAFZ+mNK+noGncd2oiPodUn6nDitthxJSoafgElUfbjsHwEzkElY8CCAnXAQDlYxKsekU2AAhRVwCglF+yEcYOW9A5Dtk1CZ/HlOcGwWdT+U2J7J3HLwSqgaDZXHnVlCf0npeoDmIA8NC+ozVILnVNkO6TAzKBEKI68JsAj7cPnLpOLp+7V3yf/bVMXm/cVSI6aBZx3naoi3TfKXmPLEBGSCTjVKfqAFCbifHM3tLUVva3Y03tsvmEmJk/MGPADK/TQ8obZf9vnuT21mR0ijOWPoAWNku8MyMD0AxOtHSB8rvk49ZTsrO3Lmt0GDIWUM3MP6dFZsQVf/flgSrhcZpWkqUMgNXHkLFCGkOKLDAyYFJ0NqQ+Dl0OwAzMTKCzLcI9d6W5HMqYzgKfwfPviM6GCdDFsxPF99v5FfJy/XbZ526G3wjfMTrabG6ZfIUrmlwNpAPTZJ0BI7JzFnMLHKj82vdn5AM4WoeCOsf1MDisivFixEG2Xbsw5zTZGjb6gENhQCo7a2rfqGTH8SAkU3Bt7QXfETatPa4En5BXYGqCPqKgczON/YcieOPb+VTV5AoBOd85SnrPjJZknfgaQXjxeYz83t/i/n3UHU1gUHbao92jzT//dKMs60QQjIUYpxQA6FPF/hEIrDI3fy4y4TVcqEgFpA8B6FEti0llH5VRHkOxgPRoRJ0+H5N6PyxLxgSiQkgjigo6T5YFgdP8ZqAsAGZlOIbMhgJge7e0dvMLJEZKA8DWBGadKU8DUAtRGgCRFfcUAJ3tYSYyrAgBkFYVAHYTRqAeSCMAGSVXvy1S0oUqhMXcMu80GQRgVUUA2NQXIwKbF9iQVQQgqaLybI8Au0QCktTcPofnhvLG/qPeVxBpkaHuekcoQSTvC6K9Y93PrjxS/73YnkThPJEOwIJsAGCM90NujxVet3/AtC2GgsoLtsQWjAAgGrF6K3k1+NhRGuygH1hkgEZtAY+RxFWqArE95kD5uH50xrgzNEfkPf5WWHHcFNn4sVixkcohs/qLZqNHnE/1Y0QY5J0t/2erJA3dqbA1zpuq4jwHoPg3KHtroHQx9BnPGddke1wxKdke19i4qJIvELT9ZLVqcIr8P2PnNclR3azLYeodox4EIGp2aFKrkCImPGYK8UonyS2PyoJTRLcdUlz5NCgftmqIWHpMlRsm9N6HrD7g9cPSYVj9jNWHqvYEqeMIKMYEvNdANeVtMYBhQmM+MMEA0ZiPzDAgeOahKaF9AfpDAUlyhTi194TIl48enKzV3dCjtpOkPo/OztWqmXv08LTbPJX98fl/AgwAI1nRhwam7C0AAAAASUVORK5CYII=" style="display: inline; margin-right: 5px;" width="16" height="16" />
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
