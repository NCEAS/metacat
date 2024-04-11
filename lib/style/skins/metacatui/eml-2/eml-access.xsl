<?xml version="1.0"?>
<!--
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file that is valid with respect to the eml-dataset.dtd
  * module of the Ecological Metadata Language (EML) into an HTML format
  * suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">


  <xsl:output method="html" encoding="UTF-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
    indent="yes" />  
  <xsl:template name="access">
    <xsl:param name="accessfirstColStyle"/>
    <xsl:param name="accesssubHeaderStyle"/>
        <xsl:choose>
         <xsl:when test="references!=''">
          <xsl:variable name="ref_id" select="references"/>
          <xsl:variable name="references" select="$ids[@id=$ref_id]" />
          <xsl:for-each select="$references">
            <xsl:call-template name="accessCommon">
             <xsl:with-param name="accessfirstColStyle" select="$accessfirstColStyle"/>
             <xsl:with-param name="accesssubHeaderStyle" select="$accesssubHeaderStyle"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="accessCommon">
             <xsl:with-param name="accessfirstColStyle" select="$accessfirstColStyle"/>
             <xsl:with-param name="accesssubHeaderStyle" select="$accesssubHeaderStyle"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>

  </xsl:template>
  <xsl:template name="accessCommon">
     <xsl:param name="accessfirstColStyle" />
     <xsl:param name="accesssubHeaderStyle"/>
        <xsl:call-template name="accesssystem">
           <xsl:with-param name="accessfirstColStyle" select="$accessfirstColStyle"/>
           <xsl:with-param name="accesssubHeaderStyle" select="$accesssubHeaderStyle"/>
        </xsl:call-template>
        <xsl:if test="normalize-space(./@order)='allowFirst' and (allow)">
            <xsl:call-template name="allow_deny">
                <xsl:with-param name="permission" select="'allow'"/>
                <xsl:with-param name="accessfirstColStyle" select="$accessfirstColStyle"/>
             </xsl:call-template>
        </xsl:if>
        <xsl:if test="(deny)">
           <xsl:call-template name="allow_deny">
                <xsl:with-param name="permission" select="'deny'"/>
                <xsl:with-param name="accessfirstColStyle" select="$accessfirstColStyle"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="normalize-space(acl/@order)='denyFirst' and (allow)">
            <xsl:call-template name="allow_deny">
                <xsl:with-param name="permission" select="'allow'"/>
                <xsl:with-param name="accessfirstColStyle" select="$accessfirstColStyle"/>
            </xsl:call-template>
        </xsl:if>


  </xsl:template>


  <xsl:template name="allow_deny">
   <xsl:param name="permission"/>
   <xsl:param name="accessfirstColStyle" />
   <xsl:choose>
       <xsl:when test="$permission='allow'">
		<div class="control-group">
			<label class="control-label">Allow</label>
			<div class="controls controls-well">
				<table class="table table-striped">
					<thead>
						<tr>
							<th>Permission</th>
							<th>Principal</th>
						</tr>
					</thead>
					<tbody>
						<xsl:for-each select="allow">
				            <tr>
				              <td>
				                <xsl:for-each select="./permission">
				                  <xsl:text>[</xsl:text><xsl:value-of select="."/><xsl:text>] </xsl:text>
				                </xsl:for-each>
				              </td>
				              <td>
				                <xsl:for-each select="principal">
				                  <xsl:value-of select="."/><br/>
				                </xsl:for-each>
				              </td>
				            </tr>
						</xsl:for-each>
					</tbody>
				</table>
			</div>
		</div>	
          
       </xsl:when>
       <xsl:otherwise>
			<div class="control-group">
		       	<label class="control-label">Deny</label>
				<div class="controls controls-well">
					<table class="table table-striped">
						<thead>
							<tr>
								<th>Permission</th>
								<th>Principal</th>
							</tr>
						</thead>
						<tbody>
							<xsl:for-each select="deny">
								<tr>
					              <td>
					                <xsl:for-each select="./permission">
					                  <xsl:text>[</xsl:text><xsl:value-of select="."/><xsl:text>] </xsl:text>
					                </xsl:for-each>
					              </td>
					              <td>
					                <xsl:for-each select="principal">
					                  <xsl:value-of select="."/><br/>
					                </xsl:for-each>
					              </td>
					            </tr>
							</xsl:for-each>
						</tbody>
					</table>
				</div>
			</div>
		</xsl:otherwise>
   </xsl:choose>

   </xsl:template>

  <xsl:template name="accesssystem">
       <xsl:param name="accessfirstColStyle" />
        <h4>Access Control</h4>
        <div class="control-group">
			<label class="control-label">Auth System</label>
			<div class="controls controls-well">
				<xsl:value-of select="./@authSystem"/>
			</div>
        </div>
        <div class="control-group">
			<label class="control-label">Order</label>
          	<div class="controls controls-well">
          		<xsl:value-of select="./@order"/>
          	</div>
        </div>
  </xsl:template>

</xsl:stylesheet>
