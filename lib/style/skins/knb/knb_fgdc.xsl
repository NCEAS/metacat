<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:import href="../../common/fgdc/fgdc-root.xsl"/>
	
    <xsl:output indent="yes" method="html"/>
    <xsl:param name="docid"/>
    <xsl:param name="qformat"/>
    
    <xsl:param name="contextURL"/>
	<xsl:param name="cgi-prefix"/>
	
	<!-- stylesheets that include fgdc-root.xsl can enable/disable editing  -->
	<xsl:param name="enableFGDCediting">false</xsl:param>
    
    <xsl:template match="/">
        <html>
            <head>
                <link rel="stylesheet" type="text/css" href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" media="all"/>
                <script language="JavaScript" type="text/JavaScript" src="{$contextURL}/style/skins/{$qformat}/{$qformat}.js"/>
                <script language="JavaScript" type="text/JavaScript" src="{$contextURL}/style/common/branding.js"/>
                <title>FGDC Identification and Metacat Data Package Information</title>
            </head>
            <body>
                <script language="JavaScript">
                    insertTemplateOpening("<xsl:value-of select="$contextURL"/>");
                </script>
                
                <div class="centerContentBorder">
                    <div class="templatecontentareaclass">
                    
	                    <xsl:if test="*[local-name()='metadata']">     	
							<xsl:call-template name="metadata"/>
						</xsl:if>
                    
                    </div>
                </div>
                
                <script language="JavaScript">
                    insertTemplateClosing("<xsl:value-of select="$contextURL"/>");
                </script>
            </body>
        </html>
    </xsl:template>
    
</xsl:stylesheet>
