<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>
    <xsl:template name="jsonld">
        <xsl:param name="science-meta"/>
        {
        "@context": "http://schema.org/",
        "@type": "Dataset",
        "@id": "<xsl:value-of select="*/@packageId" />",
        "description": "<xsl:value-of select="*/dataset/abstract" />",
        "name": "<xsl:value-of select="*/dataset/title" />",
        "name": "DataONE Dataset Download",
        "creator": [
        <xsl:for-each select="*/dataset/creator">
            {
            <xsl:if test="electronicMailAddress">
                "@id": "<xsl:value-of select="electronicMailAddress" />",
            </xsl:if>
            "@type": "Person",
            <xsl:if test="individualName/givenName">
                "givenName": "<xsl:value-of select="individualName/givenName" />",
            </xsl:if>
            <xsl:if test="individualName/surName">
                "familyName": "<xsl:value-of select="individualName/surName" />",
            </xsl:if>
            <xsl:if test="individualName/givenName and individualName/surName">
                "name": "<xsl:value-of select="individualName/givenName" />&#160;<xsl:value-of select="individualName/surName" />",
            </xsl:if>
            <xsl:if test="organizationName">
                "affiliation": "<xsl:value-of select="organizationName" />",
            </xsl:if>
            <xsl:if test="electronicMailAddress">
                "email": "<xsl:value-of select="electronicMailAddress" />"
            </xsl:if>
            }<xsl:if test="position() != last()">,</xsl:if>
        </xsl:for-each>
        ],
        "editor": [
        <xsl:for-each select="*/dataset/metadataProvider">
            {
            <xsl:if test="electronicMailAddress">
                "@id": "<xsl:value-of select="electronicMailAddress" />",
            </xsl:if>
            "@type": "Person",
            <xsl:if test="individualName/givenName">
                "givenName": "<xsl:value-of select="individualName/givenName" />",
            </xsl:if>
            <xsl:if test="individualName/surName">
                "familyName": "<xsl:value-of select="individualName/surName" />",
            </xsl:if>
            <xsl:if test="organizationName">
                "affiliation": "<xsl:value-of select="organizationName" />",
            </xsl:if>
            <xsl:if test="electronicMailAddress">
                "email": "<xsl:value-of select="electronicMailAddress" />"
            </xsl:if>
            }<xsl:if test="position() != last()">,</xsl:if>
        </xsl:for-each>
        ],
        "contributor": [
        <xsl:for-each select="*/dataset/associatedParty">
            {
            <xsl:if test="electronicMailAddress">
                "@id": "<xsl:value-of select="electronicMailAddress" />",
            </xsl:if>
            "@type": "Person",
            <xsl:if test="individualName/givenName">
                "givenName": "<xsl:value-of select="individualName/givenName" />",
            </xsl:if>
            <xsl:if test="individualName/surName">
                "familyName": "<xsl:value-of select="individualName/surName" />",
            </xsl:if>
            <xsl:if test="organizationName">
                "affiliation": "<xsl:value-of select="organizationName" />",
            </xsl:if>
            <xsl:if test="electronicMailAddress">
                "email": "<xsl:value-of select="electronicMailAddress" />"
            </xsl:if>
            }<xsl:if test="position() != last()">,</xsl:if>
        </xsl:for-each>
        ],
        <xsl:if test="*/dataset/pubDate">
            "datePublished": "<xsl:value-of select="*/dataset/pubDate" />",
        </xsl:if>
        <xsl:if test="*/dataset/intellectualRights/para">
            "license":"<xsl:value-of select="*/dataset/intellectualRights/para" />"
        </xsl:if>
        }
    </xsl:template>
</xsl:stylesheet>