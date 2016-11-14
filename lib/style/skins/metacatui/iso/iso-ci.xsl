<?xml version="1.0"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:gmd="http://www.isotc211.org/2005/gmd" 
  version="1.0">

  <!-- CI_ResponsibleParty -->
  <xsl:template name="ci_responsibleparty" match="gmd:CI_ResponsibleParty">
    <xsl:if test="./gmd:individualName">
      <div class="control-group">
        <label class="control-label">Individual</label>
        <div class="controls">
          <div class="controls-well">
            <xsl:apply-templates select="./gmd:individualName/*" />
          </div>
        </div>
      </div>
    </xsl:if>
    <xsl:if test="./gmd:organisationName">
      <div class="control-group">
        <label class="control-label">Organization</label>
        <div class="controls">
          <div class="controls-well">
            <xsl:apply-templates select="./gmd:organisationName/*" />
          </div>
        </div>
      </div>
    </xsl:if>
    <xsl:if test="./gmd:positionName">
      <div class="control-group">
        <label class="control-label">Position</label>
        <div class="controls">
          <div class="controls-well">
            <xsl:apply-templates select="./gmd:positionName/*" />
          </div>
        </div>
      </div>
    </xsl:if>
    <xsl:if test="./gmd:role">
      <div class="control-group">
        <label class="control-label">Role</label>
        <div class="controls">
          <div class="controls-well">
            <xsl:value-of select="./gmd:role/gmd:CI_RoleCode" />
          </div>
        </div>
      </div>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>