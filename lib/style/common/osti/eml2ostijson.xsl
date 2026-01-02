<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" encoding="UTF-8" omit-xml-declaration="yes" indent="yes" media-type="application/json"/>
    <xsl:strip-space elements="*"/>

    <xsl:param name="access" select="'UNL'"/>
    <xsl:param name="country" select="'US'"/>
    <xsl:param name="data_type" select="'DA'"/>
    <xsl:param name="default_doe_contract" select="'AC02-05CH11231'"/>
    <xsl:param name="lang" select="'English'"/>
    <xsl:param name="osti_id"/>
    <xsl:param name="site_code"/>
    <xsl:param name="site_url"/>
    <xsl:param name="workflow_status" select="'SA'"/>


    <xsl:template match="@* | node()">

        <xsl:call-template name="elink2">
        </xsl:call-template>

    </xsl:template>

    <!-- Generates OSTI ELINK 2 from EML-->
    <xsl:template name="elink2">
        <xsl:param name="package_id"/>
        {
        <xsl:if test="$site_code">"site_ownership_code": "<xsl:value-of select="$site_code"/>",
        </xsl:if>
        "access_limitations": ["<xsl:value-of select="$access"/>"],
        "product_type": "<xsl:value-of select="$data_type"/>",
        <!--    Check for site url --><xsl:if test="$site_url">"site_url": "<xsl:value-of select="$site_url"/>",
        </xsl:if>
        <!--    Check for OSTI ID --><xsl:if test="$osti_id">"osti_id": <xsl:value-of select="$osti_id"/>,
        </xsl:if>
        <!--    Get Title -->
        "title": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="dataset/title"/></xsl:call-template>",
        <!--    Get Abstract -->
        <xsl:variable name="DescriptionTMP">
            <xsl:for-each select="dataset/abstract/para">
                <xsl:call-template name="transform-string"><xsl:with-param name="content" select="text()"/></xsl:call-template>
                <xsl:if test="position() != last()">
                    <xsl:text>\n</xsl:text>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        "description": "<xsl:value-of select="$DescriptionTMP"/>",
        <!--    Get Publication Date -->
        <xsl:if test="dataset/pubDate">"publication_date": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="dataset/pubDate"/></xsl:call-template>",</xsl:if>
        <!--    Geolocations    -->
        <xsl:if test="dataset/coverage/geographicCoverage">
        "geolocations": [
        <xsl:for-each select="dataset/coverage/geographicCoverage">
            {
                <!-- Get geolocation label-->
                <xsl:if test="geographicDescription">
                "label": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="geographicDescription"/></xsl:call-template>",
                </xsl:if>
                <!-- Get bounding box-->
                <xsl:if test="boundingCoordinates">
                "points": [
                    <xsl:if test="boundingCoordinates/northBoundingCoordinate">
                    {
                        "latitude": <xsl:value-of select="translate(normalize-space( boundingCoordinates/northBoundingCoordinate),' +', '')"/>,
                        "longitude": <xsl:value-of select="translate(normalize-space( boundingCoordinates/westBoundingCoordinate),' +', '')"/>
                    },
                    </xsl:if>
                    <xsl:if test="boundingCoordinates/southBoundingCoordinate">
                    {
                        "latitude": <xsl:value-of select="translate(normalize-space( boundingCoordinates/southBoundingCoordinate),' +', '')"/>,
                        "longitude": <xsl:value-of select="translate(normalize-space( boundingCoordinates/eastBoundingCoordinate),' +', '')"/>
                    }
                    </xsl:if>
                ],
                <xsl:choose>
                <xsl:when test="count(boundingCoordinates/*)=2">
                "type": "POINT"
                </xsl:when>
                <xsl:when test="count(boundingCoordinates/*)=4">
                "type": "BOX"
                </xsl:when>
                <xsl:otherwise>
                "type": "POLYGON"
                </xsl:otherwise>
            </xsl:choose>
            </xsl:if>
            }
            <xsl:if test="position() != last()">
            <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:for-each>
        ],
        </xsl:if>
        "keywords": [
        <xsl:for-each select="dataset/keywordSet">
            <xsl:for-each select="keyword">
                "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="."/></xsl:call-template>"
                <xsl:if test="position() != last()">
                    <xsl:text>,</xsl:text>
                </xsl:if>
            </xsl:for-each>
            <xsl:if test="position() != last()">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:for-each>
        ],
        "organizations": [
            <xsl:if test="dataset/project">
            {
                "type": "RESEARCHING",
                "name": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="dataset/project/title"/></xsl:call-template>"
            },
            </xsl:if>
            {
                "type": "SPONSOR",
                "name": "ESS-DIVE",
                "identifiers": [
                    {
                        "type": "CN_DOE",
                        "value": "<xsl:value-of select="$default_doe_contract"/>"
                    }
                ]
            },
            <xsl:for-each select="dataset/associatedParty[role[text()='fundingOrganization']]">
            {
                "type": "SPONSOR",
                "name": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="organizationName"/></xsl:call-template>"
            }
            <xsl:if test="position() != last()">
                <xsl:text>,</xsl:text>
            </xsl:if>
            </xsl:for-each>
        ],
        "identifiers": [
        {
            "type": "CN_DOE",
            "value": "<xsl:value-of select="$default_doe_contract"/>"
        }
        <xsl:if test="dataset/alternateIdentifier">
            <xsl:text>,</xsl:text>
            <xsl:for-each select="dataset/alternateIdentifier">
            {
                "type": "OTHER_ID",
                "value": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="."/></xsl:call-template>"
            }
            <xsl:choose>
            <xsl:when test="position() != last()">
                <xsl:text>,</xsl:text>
            </xsl:when>
            </xsl:choose>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="dataset/project/funding">
            <xsl:text>,</xsl:text>
            <xsl:for-each select="dataset/project/funding/para">
            {
                "type": "CN_NONDOE",
                <xsl:choose>
                <xsl:when test="starts-with(., 'DOE:DE-')">
                "value": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="substring-after(.,'DOE:DE-')"/></xsl:call-template>"
                </xsl:when>
                <xsl:otherwise>
                "value": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="substring-after(.,'DOE:')"/></xsl:call-template>"
                </xsl:otherwise>
                </xsl:choose>
            }
            <xsl:if test="position() != last()">
                <xsl:text>,</xsl:text>
            </xsl:if>
            </xsl:for-each>
        </xsl:if>
        ],
        "persons": [
        <xsl:for-each select="dataset/creator">
            <xsl:call-template name="format-person">
                <xsl:with-param name="person" select="."/>
                <xsl:with-param name="personType">AUTHOR</xsl:with-param>
            </xsl:call-template>
            <xsl:if test="position() != last()">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:if test="dataset/associatedParty[role='contributor']">
        <xsl:text>,</xsl:text>
        <xsl:for-each select="dataset/associatedParty[role='contributor']">
            <xsl:call-template name="format-person">
                <xsl:with-param name="person" select="."/>
                <xsl:with-param name="personType">CONTRIBUTING</xsl:with-param>
            </xsl:call-template>
            <xsl:if test="position() != last()">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:for-each>
        </xsl:if>
        <xsl:if test="dataset/contact[position() = 1]">
            <xsl:text>,</xsl:text>
            <xsl:call-template name="format-person">
                <xsl:with-param name="person" select="dataset/contact[position() = 1]"/>
                <xsl:with-param name="personType">CONTACT</xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        ]
        }
    </xsl:template>


        <!--######################################## Related References #######################################-->
        <!-- This might be hard... We need to have the relationship and type (DOI, URL, etc)
        -->


    <!-- transform-string
    Transforms the string for valid JSON

    This template does the following
    + normalizes spaces with normalize-space
    + translates the invalid characters to spaces
    + escapes quote and backslash (&#92;)

    content - the string to transform
    -->
    <xsl:template name="transform-string">
        <xsl:param name="content"/>
        <xsl:call-template name="escape-characters">
            <xsl:with-param name="text" select="translate(normalize-space($content),'&#x9;&#xa;&#xd;', ' ')"/>
        </xsl:call-template>
    </xsl:template>


    <!--  escape-characters string manipulation template that escapes characters for json.
    Algorithm reduces recursion depth (just in case text is large)
    Parameters:
    + text    - the string to escape
    -->
    <xsl:template name="escape-characters">
        <xsl:param name="text"/>
        <xsl:param name="left" select="1"/>
        <xsl:param name="right" select="string-length($text)"/>
        <xsl:choose>
            <xsl:when test="$left = $right">
                <xsl:comment>Output the character</xsl:comment>
                <xsl:variable name="letter" select="substring($text, $left, 1)"/>
                <xsl:choose>
                    <xsl:when test="contains('&#92;&quot;', $letter)">
                        <xsl:comment>Escape the character it is either a backslash or double quote</xsl:comment>
                        <xsl:text>&#92;</xsl:text><xsl:value-of select="$letter"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:comment>Output the character (no escape character needed)</xsl:comment>
                        <xsl:value-of select="$letter"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="$left &lt; $right">
                <xsl:comment>This makes sure recursion is shallow</xsl:comment>
                <xsl:variable name="middle" select="floor(($left+$right) div 2)"/>
                <xsl:call-template name="escape-characters">
                    <xsl:with-param name="text" select="$text"/>
                    <xsl:with-param name="left" select="$left"/>
                    <xsl:with-param name="right" select="$middle"/>
                </xsl:call-template>
                <xsl:call-template name="escape-characters">
                    <xsl:with-param name="text" select="$text"/>
                    <xsl:with-param name="left" select="$middle+1"/>
                    <xsl:with-param name="right" select="$right"/>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!--
    Format person
    Parameters:
    + person - the node of type person
    + personType - the person's "type" label or role (string)-->

    <xsl:template name="format-person">
        <xsl:param name="person"/>
        <xsl:param name="personType"/>
        {
        "type": "<xsl:value-of select="$personType"/>",
        <xsl:if test="$personType='CONTRIBUTING'">
        "contributor_type": "OTHER",
        </xsl:if>
        <xsl:if test="$person/userId">
        "orcid": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="$person/userId"/></xsl:call-template>",
        </xsl:if>
        "first_name": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="$person/individualName/givenName"/></xsl:call-template>",
        "last_name": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="$person/individualName/surName"/></xsl:call-template>"
        <xsl:if test="$person/organizationName or $person/electronicMailAddress ">
        <xsl:text>,</xsl:text>
        </xsl:if>
        <xsl:if test="$person/organizationName">
        "affiliations": [
            {"name": "<xsl:call-template name="transform-string"><xsl:with-param name="content" select="$person/organizationName"/></xsl:call-template>"}
            ]
        <xsl:if test="$person/electronicMailAddress">
        <xsl:text>,</xsl:text>
        </xsl:if>
        </xsl:if>
        <xsl:if test="$person/electronicMailAddress">
        "email": ["<xsl:call-template name="transform-string"><xsl:with-param name="content" select="$person/electronicMailAddress"/></xsl:call-template>"]
        </xsl:if>
        }
    </xsl:template>

</xsl:stylesheet>