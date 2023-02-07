<?xml version="1.0"?>
<!--
  *  '$RCSfile$'
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
                doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
                doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
                indent="yes"/>

    <xsl:template name="method">
        <xsl:param name="methodfirstColStyle"/>
        <xsl:param name="methodsubHeaderStyle"/>

        <xsl:if test="count(methodStep) > 0">
            <div class="control-group">
                <label class="control-label">Methods</label>
                <div class="controls">
                    <div class="accordion" id="methodAccordian">
                        <xsl:for-each select="methodStep">
                            <div class="accordion-group">
                                <div class="accordion-heading">
                                    <a class="accordion-toggle" data-toggle="collapse">
                                        <xsl:attribute name="data-target">#methodStep<xsl:value-of select="position()"/>
                                        </xsl:attribute>
                                        Step<xsl:text> </xsl:text><xsl:value-of select="position()"/>
                                    </a>
                                </div>
                                <div class="accordion-body collapse in">
                                    <xsl:attribute name="id">methodStep<xsl:value-of select="position()"/>
                                    </xsl:attribute>
                                    <div class="accordion-inner">
                                        <xsl:call-template name="methodStep">
                                            <xsl:with-param name="methodfirstColStyle" select="$methodfirstColStyle"/>
                                            <xsl:with-param name="methodsubHeaderStyle" select="$methodsubHeaderStyle"/>
                                        </xsl:call-template>
                                    </div>
                                </div>
                            </div>
                        </xsl:for-each>
                    </div>
                </div>
            </div>
        </xsl:if>

        <xsl:if test="count(sampling) > 0">
            <div class="control-group">
                <label class="control-label">Sampling</label>
                <div class="controls">
                    <div class="accordion" id="samplingAccordian">
                        <xsl:for-each select="sampling">
                            <div class="accordion-group">
                                <div class="accordion-heading">
                                    <a class="accordion-toggle" data-toggle="collapse">
                                        <xsl:attribute name="data-target">#samplingStep<xsl:value-of
                                                select="position()"/>
                                        </xsl:attribute>
                                        Sampling Step<xsl:text> </xsl:text><xsl:value-of select="position()"/>
                                    </a>
                                </div>
                                <div class="accordion-body collapse in">
                                    <xsl:attribute name="id">samplingStep<xsl:value-of select="position()"/>
                                    </xsl:attribute>
                                    <div class="accordion-inner">
                                        <xsl:call-template name="sampling">
                                            <xsl:with-param name="methodfirstColStyle" select="$methodfirstColStyle"/>
                                            <xsl:with-param name="methodsubHeaderStyle" select="$methodsubHeaderStyle"/>
                                        </xsl:call-template>
                                    </div>
                                </div>
                            </div>
                        </xsl:for-each>
                    </div>
                </div>
            </div>
        </xsl:if>

        <xsl:if test="count(qualityControl) > 0">
            <div class="control-group">
                <label class="control-label">Quality Control</label>
                <div class="controls">
                    <div class="accordion" id="qualityControlAccordian">
                        <xsl:for-each select="qualityControl">
                            <div class="accordion-group">
                                <div class="accordion-heading">
                                    <a class="accordion-toggle" data-toggle="collapse">
                                        <xsl:attribute name="data-target">#qualityControlStep<xsl:value-of
                                                select="position()"/>
                                        </xsl:attribute>
                                        Quality Control Step<xsl:text> </xsl:text><xsl:value-of select="position()"/>
                                    </a>
                                </div>
                                <div class="accordion-body collapse in">
                                    <xsl:attribute name="id">qualityControlStep<xsl:value-of select="position()"/>
                                    </xsl:attribute>
                                    <div class="accordion-inner">
                                        <xsl:call-template name="qualityControl">
                                            <xsl:with-param name="methodfirstColStyle" select="$methodfirstColStyle"/>
                                            <xsl:with-param name="methodsubHeaderStyle" select="$methodsubHeaderStyle"/>
                                        </xsl:call-template>
                                    </div>
                                </div>
                            </div>
                        </xsl:for-each>
                    </div>
                </div>
            </div>
        </xsl:if>

    </xsl:template>

    <!-- ******************************************
         Method step
         *******************************************-->

    <xsl:template name="methodStep">
        <xsl:param name="methodfirstColStyle"/>
        <xsl:param name="methodsubHeaderStyle"/>
        <xsl:call-template name="step">
            <xsl:with-param name="protocolfirstColStyle" select="$methodfirstColStyle"/>
            <xsl:with-param name="protocolsubHeaderStyle" select="$methodsubHeaderStyle"/>
        </xsl:call-template>
        <xsl:for-each select="dataSource">
            <div class="row-fluid">
                <xsl:apply-templates mode="dataset">
                </xsl:apply-templates>
            </div>
        </xsl:for-each>
    </xsl:template>

    <!-- *********************************************
         Sampling
         *********************************************-->

    <xsl:template name="sampling">
        <xsl:param name="methodfirstColStyle"/>
        <xsl:param name="methodsubHeaderStyle"/>
        <xsl:for-each select="studyExtent">
            <xsl:call-template name="studyExtent">
                <xsl:with-param name="methodfirstColStyle" select="$methodfirstColStyle"/>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:for-each select="samplingDescription">
            <div class="control-group">
                <label class="control-label">Sampling Description</label>
                <div class="controls">
                    <xsl:call-template name="text">
                        <xsl:with-param name="textfirstColStyle" select="$methodfirstColStyle"/>
                    </xsl:call-template>
                </div>
            </div>
        </xsl:for-each>
        <xsl:for-each select="spatialSamplingUnits">
            <xsl:call-template name="spatialSamplingUnits">
                <xsl:with-param name="methodfirstColStyle" select="$methodfirstColStyle"/>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:for-each select="citation">
            <div class="control-group">
                <label class="control-label">Sampling Citation</label>
                <div class="controls">
                    <strong>
                        <xsl:for-each select="./title">
                            <xsl:call-template name="i18n">
                                <xsl:with-param name="i18nElement" select="."/>
                            </xsl:call-template>
                            <xsl:text> </xsl:text>
                        </xsl:for-each>
                    </strong>
                    <xsl:call-template name="citation">
                        <xsl:with-param name="citationfirstColStyle" select="$methodfirstColStyle"/>
                        <xsl:with-param name="citationsubHeaderStyle" select="$methodsubHeaderStyle"/>
                    </xsl:call-template>
                </div>
            </div>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="studyExtent">
        <xsl:param name="methodfirstColStyle"/>
        <xsl:param name="methodsubHeaderStyle"/>
        <xsl:for-each select="coverage">
            <div class="control-group">
                <label class="control-label">Sampling Coverage</label>
                <div class="controls">
                    <xsl:call-template name="coverage">
                    </xsl:call-template>
                </div>
            </div>
        </xsl:for-each>
        <xsl:for-each select="description">
            <div class="control-group">
                <label class="control-label">Sampling Area And Frequency</label>
                <div class="controls">
                    <xsl:call-template name="text">
                        <xsl:with-param name="textfirstColStyle" select="$methodfirstColStyle"/>
                    </xsl:call-template>
                </div>
            </div>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="spatialSamplingUnits">
        <xsl:param name="methodfirstColStyle"/>
        <xsl:for-each select="referenceEntityId">
            <div class="control-group">
                <label class="control-label">Sampling Unit Reference</label>
                <div class="controls">
                    <xsl:value-of select="."/>
                </div>
            </div>
        </xsl:for-each>
        <xsl:for-each select="coverage">
            <div class="control-group">
                <label class="control-label">Sampling Unit Location</label>
                <div class="controls">
                    <xsl:call-template name="coverage">
                    </xsl:call-template>
                </div>
            </div>
        </xsl:for-each>
    </xsl:template>

    <!-- ***************************************
         quality control
         ***************************************-->
    <xsl:template name="qualityControl">
        <xsl:param name="methodfirstColStyle"/>
        <xsl:param name="methodsubHeaderStyle"/>
        <xsl:call-template name="step">
            <xsl:with-param name="protocolfirstColStyle" select="$methodfirstColStyle"/>
            <xsl:with-param name="protocolsubHeaderStyle" select="$methodsubHeaderStyle"/>
        </xsl:call-template>
    </xsl:template>

</xsl:stylesheet>

