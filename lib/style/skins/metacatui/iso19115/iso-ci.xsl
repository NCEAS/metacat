<?xml version="1.0"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:gmd="http://www.isotc211.org/2005/gmd" 
    xmlns:gco="http://www.isotc211.org/2005/gco" version="1.0">
    <!-- CI_ResponsibleParty -->
    <xsl:template match="gmd:CI_ResponsibleParty">
        <div class="controls-well">
            <xsl:if test="./gmd:individualName and normalize-space(./gmd:individualName/gco:CharacterString/text())!=''">
                <div class="control-group">
                    <label class="control-label">Individual</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:individualName" />
                        </div>
                    </div>
                </div>
            </xsl:if>
            <xsl:if test="./gmd:organisationName and normalize-space(./gmd:organisationName/gco:CharacterString/text())!=''">
                <div class="control-group">
                    <label class="control-label">Organization</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:organisationName" />
                        </div>
                    </div>
                </div>
            </xsl:if>
            <xsl:if test="./gmd:positionName and normalize-space(./gmd:positionName/gco:CharacterString/text())!=''">
                <div class="control-group">
                    <label class="control-label">Position</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:positionName" />
                        </div>
                    </div>
                </div>
            </xsl:if>
            <xsl:if test="./gmd:role and normalize-space(./gmd:role/gmd:CI_RoleCode/text())!=''">
                <div class="control-group">
                    <label class="control-label">Role</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:role" />
                        </div>
                    </div>
                </div>
            </xsl:if>
            <xsl:if test="./gmd:contactInfo/gmd:CI_Contact/gmd:phone">
                <div class="control-group">
                    <label class="control-label">Phone</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:contactInfo/gmd:CI_Contact/gmd:phone" />
                        </div>
                    </div>
                </div>
            </xsl:if>
            <xsl:if test="./gmd:contactInfo/gmd:CI_Contact/gmd:address">
                <div class="control-group">
                    <label class="control-label">Address</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:contactInfo/gmd:CI_Contact/gmd:address" />
                        </div>
                    </div>
                </div>
            </xsl:if>
            <xsl:if test="./gmd:contactInfo/gmd:CI_Contact/gmd:onlineResource">
                <div class="control-group">
                    <label class="control-label">Online Resource</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:contactInfo/gmd:CI_Contact/gmd:onlineResource" />
                        </div>
                    </div>
                </div>
            </xsl:if>
            <xsl:if test="./gmd:contactInfo/gmd:CI_Contact/gmd:hoursOfService">
                <div class="control-group">
                    <label class="control-label">Hours of Service</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:contactInfo/gmd:CI_Contact/gmd:hoursOfService" />
                        </div>
                    </div>
                </div>
            </xsl:if>
            <xsl:if test="./gmd:contactInfo/gmd:CI_Contact/gmd:contactInstructions">
                <div class="control-group">
                    <label class="control-label">Contact Instructions</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates select="./gmd:contactInfo/gmd:CI_Contact/gmd:contactInstructions" />
                        </div>
                    </div>
                </div>
            </xsl:if>
        </div>
    </xsl:template>
    <xsl:template match="gmd:CI_RoleCode">
        <xsl:apply-templates />
    </xsl:template>
    <xsl:template match="gmd:CI_Telephone">
        <xsl:for-each select="./gmd:voice">
            <div class="control-group">
                <label class="control-label">Voice</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:for-each>
        <xsl:for-each select="./gmd:facsimile">
            <div class="control-group">
                <label class="control-label">Fax</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:for-each>
    </xsl:template>
    <xsl:template match="gmd:CI_Address">
        <xsl:for-each select="./gmd:deliveryPoint">
            <div class="control-group">
                <label class="control-label">Delivery Point</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:for-each>
        <xsl:if test="./gmd:city">
            <div class="control-group">
                <label class="control-label">City</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:city" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:administrativeArea">
            <div class="control-group">
                <label class="control-label">Administrative Area</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:administrativeArea" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:postalCode">
            <div class="control-group">
                <label class="control-label">Postal Code</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:postalCode" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:country">
            <div class="control-group">
                <label class="control-label">Country</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:country" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:for-each select="./gmd:electronicMailAddress">
            <div class="control-group">
                <label class="control-label">E-Mail</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:variable name="email" select="./gco:CharacterString/text()" />
                        <xsl:element name="a">
                            <xsl:attribute name="href">mailto:                
                                <xsl:value-of select="$email" />
                            </xsl:attribute>
                            <xsl:value-of select="$email" />
                        </xsl:element>
                    </div>
                </div>
            </div>
        </xsl:for-each>
    </xsl:template>
    <xsl:template match="gmd:CI_OnlineResource">
        <xsl:if test="./gmd:linkage">
            <div class="control-group">
                <label class="control-label">linkage</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:linkage" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:protocol">
            <div class="control-group">
                <label class="control-label">protocol</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:protocol" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:applicationProfile">
            <div class="control-group">
                <label class="control-label">applicationProfile</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:applicationProfile" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:name">
            <div class="control-group">
                <label class="control-label">name</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:name" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:description">
            <div class="control-group">
                <label class="control-label">description</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:description" />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:function">
            <div class="control-group">
                <label class="control-label">function</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates select="./gmd:function" />
                    </div>
                </div>
            </div>
        </xsl:if>
    </xsl:template>
    <xsl:template match="gmd:CI_Citation">
        <div class="control-group">
            <label class="control-label">Title</label>
            <div class="controls">
                <div class="controls-well">
                    <xsl:apply-templates select="./gmd:title" />
                </div>
            </div>
        </div>
        <xsl:if test="./gmd:alternateTitle">
            <xsl:for-each select="./gmd:alternateTitle">
                <div class="control-group">
                    <label class="control-label">Alternate Title</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates />
                        </div>
                    </div>
                </div>
            </xsl:for-each>
        </xsl:if>
        <xsl:for-each select="./gmd:date">
            <div class="control-group">
                <label class="control-label">Date</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:for-each>
        <xsl:if test="./gmd:edition">
            <xsl:for-each select="./gmd:edition">
                <div class="control-group">
                    <label class="control-label">Edition</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates />
                        </div>
                    </div>
                </div>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="./gmd:editionDate">
            <div class="control-group">
                <label class="control-label">Edition Date</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:identifier">
            <xsl:for-each select="./gmd:identifier">
                <div class="control-group">
                    <label class="control-label">Identifier</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates />
                        </div>
                    </div>
                </div>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="./gmd:citedResponsibleParty">
            <xsl:for-each select="./gmd:citedResponsibleParty">
                <div class="control-group">
                    <label class="control-label">Cited Responsible Party</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates />
                        </div>
                    </div>
                </div>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="./gmd:presentationForm">
            <xsl:for-each select="./gmd:presentationForm">
                <div class="control-group">
                    <label class="control-label">Presentation Form</label>
                    <div class="controls">
                        <div class="controls-well">
                            <xsl:apply-templates />
                        </div>
                    </div>
                </div>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="./gmd:series">
            <div class="control-group">
                <label class="control-label">Series</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:otherCitationDetails">
            <div class="control-group">
                <label class="control-label">Other Citation Details</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:collectiveTitle">
            <div class="control-group">
                <label class="control-label">Collective Title</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:ISBN">
            <div class="control-group">
                <label class="control-label">ISBN</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:if>
        <xsl:if test="./gmd:ISSN">
            <div class="control-group">
                <label class="control-label">ISSN</label>
                <div class="controls">
                    <div class="controls-well">
                        <xsl:apply-templates />
                    </div>
                </div>
            </div>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
