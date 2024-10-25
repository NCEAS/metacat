<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:strip-space elements="*"/>
    <xsl:param name="data_type" select="'SM'"/>
    <xsl:param name="country" select="'US'"/>
    <xsl:param name="lang" select="'English'"/>
    <xsl:param name="site_code" select="'ESS-DIVE'"/>
    <xsl:param name="site_url"/>
    <xsl:param name="subject" select="'54 ENVIRONMENTAL SCIENCES'"/>
    <xsl:param name="osti_id"/>
    <xsl:param name="default_doe_contract" select="'AC02-05CH11231'"/>
    <xsl:template match="dataset">
        <records>
            <record>
                <xsl:if test="$site_url">
                    <site_url><xsl:value-of select="$site_url"/></site_url>
                </xsl:if>
                <xsl:choose>
                    <!-- Edit existing -->
                    <xsl:when test="$osti_id">
                        <osti_id><xsl:value-of select="$osti_id"/></osti_id>
                    </xsl:when>
                    <!-- only create new if there is not osti_id and site url -->
                    <xsl:when test="not($osti_id) and not($site_url)">
                        <set_reserved/>
                    </xsl:when>
                </xsl:choose>
                <title>
                    <xsl:value-of select="title"/>
                </title>
                <xsl:if test="alternateIdentifier">
                    <xsl:variable name="AltIDListTMP">
                        <xsl:for-each select="alternateIdentifier">
                            <xsl:choose>
                                <xsl:when test="position() = 1">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="concat(';',.)"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:for-each>
                    </xsl:variable>
                    <!-- I need a for loop here which will concat altID values with ";" -->
                    <product_nos>
                        <xsl:value-of select="$AltIDListTMP"/>
                    </product_nos>
                </xsl:if>
                <!-- Submit ESS-DIVE Contract number as default contract number -->
                <contract_nos><xsl:value-of select="$default_doe_contract"/></contract_nos>
                <xsl:variable name="FundingListTMP">
                    <xsl:for-each select="project/funding/para">
                        <xsl:variable name="fundingID" select="."/>
                        <xsl:variable name="cleanID">
                            <xsl:choose>
                                <xsl:when test="starts-with($fundingID, 'DOE:')">
                                    <xsl:choose>
                                        <xsl:when test="starts-with($fundingID, 'DOE:DE-')">
                                            <xsl:value-of select="substring-after($fundingID,'DOE:DE-')"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="substring-after($fundingID,'DOE:')"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="$fundingID"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:choose>
                            <xsl:when test="position() = 1">
                                <xsl:value-of select="$cleanID"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="concat(';', $cleanID)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:variable>
                <!-- I need a for loop here which will concat project funding values with ";" and remove "DOE:" & "DOE:DE" prefixes-->
                <xsl:choose>
                    <xsl:when test="$FundingListTMP !=''">
                        <non-doe_contract_nos>
                            <xsl:value-of select="$FundingListTMP"/>
                        </non-doe_contract_nos>
                    </xsl:when>
                    <xsl:otherwise>
                        <non-doe_contract_nos/>
                    </xsl:otherwise>
                </xsl:choose>

                <originating_research_org>
                    <xsl:value-of select="project/title"/>
                </originating_research_org>

                <description>
                    <xsl:value-of select="abstract"/>
                </description>

                <xsl:variable name="SponsorListTMP">
                    <xsl:for-each select="associatedParty">
                        <xsl:if test="role='fundingOrganization'">
                            <xsl:choose>
                                <xsl:when test="position() = 1">
                                    <xsl:value-of select="organizationName"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="concat(';',organizationName)"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:if>
                    </xsl:for-each>
                </xsl:variable>
                <!-- I need a for loop here which will concat altID values with ";" -->
                <sponsor_org>
                    <xsl:value-of select="$SponsorListTMP"/>
                </sponsor_org>

                <related_resource/>
                <dataset_type><xsl:value-of select="$data_type"/></dataset_type>

                <xsl:if test="pubDate">
                    <publication_date>
                        <xsl:choose>
                            <xsl:when test="string-length(pubDate)=4">
                                <xsl:value-of select="pubDate"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:variable name="year">
                                    <xsl:value-of select="substring(pubDate/text(),1,4)"/>
                                </xsl:variable>
                                <xsl:variable name="month">
                                    <xsl:value-of select="substring(pubDate/text(),6,2)"/>
                                </xsl:variable>
                                <xsl:variable name="day">
                                    <xsl:value-of select="substring(pubDate/text(),9,2)"/>
                                </xsl:variable>
                                <xsl:value-of select="concat($month,'/',$day,'/',$year)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </publication_date>
                </xsl:if>

                <xsl:choose>
                    <xsl:when test="(contact/individualName/givenName) and (contact/individualName/surName)">
                        <contact_name>
                            <xsl:value-of
                                    select="concat(contact/individualName/givenName,' ', contact/individualName/surName)"/>
                        </contact_name>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="contact/individualName/surName">
                                <contact_name>
                                    <xsl:value-of select="contact/individualName/surName"/>
                                </contact_name>
                            </xsl:when>
                            <xsl:otherwise>
                                <contact_name>
                                    <xsl:value-of select="contact/individualName/givenName"/>
                                </contact_name>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>

                <xsl:if test="contact/electronicMailAddress">
                    <contact_email>
                        <xsl:value-of select="contact/electronicMailAddress"/>
                    </contact_email>
                </xsl:if>

                <xsl:if test="contact/organizationName">
                    <contact_org>
                        <xsl:value-of select="contact/organizationName"/>
                    </contact_org>
                </xsl:if>
                <site_input_code><xsl:value-of select="$site_code"/></site_input_code>
                <doi_infix></doi_infix>
                <subject_categories_code><xsl:value-of select="$subject"/></subject_categories_code>
                <language><xsl:value-of select="$lang"/></language>
                <country><xsl:value-of select="$country"/></country>

                <creatorsblock>
                    <xsl:for-each select="creator">
                        <creators_detail>
                            <xsl:if test="individualName/givenName">
                                <first_name>
                                    <xsl:value-of select="individualName/givenName"/>
                                </first_name>
                            </xsl:if>
                            <xsl:if test="individualName/surName">
                                <last_name>
                                    <xsl:value-of select="individualName/surName"/>
                                </last_name>
                            </xsl:if>
                            <xsl:if test="electronicMailAddress">
                                <private_email>
                                    <xsl:value-of select="electronicMailAddress"/>
                                </private_email>
                            </xsl:if>
                            <xsl:if test="organizationName">
                                <affiliation_name>
                                    <xsl:value-of select="organizationName"/>
                                </affiliation_name>
                            </xsl:if>
                            <xsl:if test="userId and userId/@directory='https://orcid.org'">
                                <orcid_id>
                                    <xsl:value-of select="userId"/>
                                </orcid_id>
                            </xsl:if>
                        </creators_detail>
                    </xsl:for-each>
                </creatorsblock>

                <xsl:if test="associatedParty and associatedParty/role='contributor'">
                    <contributors>
                        <xsl:for-each select="associatedParty">
                            <xsl:if test="role='contributor'">
                                <contributor contributorType="RelatedPerson">
                                    <xsl:if test="individualName/givenName">
                                        <first_name>
                                            <xsl:value-of select="individualName/givenName"/>
                                        </first_name>
                                    </xsl:if>
                                    <xsl:if test="individualName/surName">
                                        <last_name>
                                            <xsl:value-of select="individualName/surName"/>
                                        </last_name>
                                    </xsl:if>
                                    <xsl:if test="electronicMailAddress">
                                        <private_email>
                                            <xsl:value-of select="electronicMailAddress"/>
                                        </private_email>
                                    </xsl:if>
                                    <xsl:if test="organizationName">
                                        <affiliation_name>
                                            <xsl:value-of select="organizationName"/>
                                        </affiliation_name>
                                    </xsl:if>
                                    <xsl:if test="userId and userId/@directory='https://orcid.org'">
                                        <orcid_id>
                                            <xsl:value-of select="userId"/>
                                        </orcid_id>
                                    </xsl:if>
                                </contributor>
                            </xsl:if>
                        </xsl:for-each>
                    </contributors>
                </xsl:if>

                <xsl:if test="coverage/geographicCoverage">
                    <geolocations>
                        <xsl:for-each select="coverage/geographicCoverage">
                            <geolocation>
                                <xsl:if test="geographicDescription">
                                    <place>
                                        <xsl:value-of select="geographicDescription"/>
                                    </place>
                                </xsl:if>
                                <xsl:if test="boundingCoordinates">
                                    <boundingBox>
                                        <northLatitude>
                                            <xsl:choose>
                                                <xsl:when test="boundingCoordinates/northBoundingCoordinate &gt;= 0">
                                                    <xsl:value-of
                                                            select="concat('+',boundingCoordinates/northBoundingCoordinate)"/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="boundingCoordinates/northBoundingCoordinate"/>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </northLatitude>
                                        <southLatitude>
                                            <xsl:choose>
                                                <xsl:when test="boundingCoordinates/southBoundingCoordinate &gt;= 0">
                                                    <xsl:value-of
                                                            select="concat('+',boundingCoordinates/southBoundingCoordinate)"/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="boundingCoordinates/southBoundingCoordinate"/>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </southLatitude>
                                        <eastLongitude>
                                            <xsl:choose>
                                                <xsl:when test="boundingCoordinates/eastBoundingCoordinate &gt;= 0">
                                                    <xsl:value-of
                                                            select="concat('+',boundingCoordinates/eastBoundingCoordinate)"/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="boundingCoordinates/eastBoundingCoordinate"/>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </eastLongitude>
                                        <westLongitude>
                                            <xsl:choose>
                                                <xsl:when test="boundingCoordinates/westBoundingCoordinate &gt;= 0">
                                                    <xsl:value-of
                                                            select="concat('+',boundingCoordinates/westBoundingCoordinate)"/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="boundingCoordinates/westBoundingCoordinate"/>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </westLongitude>
                                    </boundingBox>
                                </xsl:if>
                            </geolocation>
                        </xsl:for-each>
                    </geolocations>
                </xsl:if>

                <xsl:variable name="KeywordList">
                    <xsl:for-each select="keywordSet/keyword">
                        <xsl:choose>
                            <xsl:when test="position() = 1">
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="concat('; ',.)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:variable>
                <keywords>
                    <xsl:value-of select="$KeywordList"/>
                </keywords>

                <xsl:if test="additionalInfo/section[title='Related References']/para">
                <xsl:variable name="RelatedReferences">
                    <xsl:for-each select="additionalInfo/section[title='Related References']/para">
                        <xsl:choose>
                            <xsl:when test="position() = 1">
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="concat('; ',.)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:variable>
                <related_resource>
                    <xsl:value-of select="$RelatedReferences"/>
                </related_resource>
                </xsl:if>

            </record>
        </records>
    </xsl:template>
</xsl:stylesheet>