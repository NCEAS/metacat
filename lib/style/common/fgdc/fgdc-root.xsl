<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:param name="publicRead"/>
    <xsl:param name="message"/>
    <xsl:param name="sessionid"></xsl:param>
    <xsl:param name="enableFGDCediting"/>
        
    <xsl:template name="metadata">
            <body>
                <div class="centerContentBorder">
                    <div class="templatecontentareaclass">
                        <xsl:apply-templates select="metadata/idinfo"/>
                        <br/>
                        <xsl:apply-templates
                            select="metadata/distinfo/resdesc[starts-with(text(),$docid)]/ancestor::distinfo"/>
                    </div>
                </div>
            </body>
    </xsl:template>
    
    <xsl:template match="idinfo">
        <table width="100%" border="0" cellspacing="0" class="subGroup subGroup_border onehundred_percent">
            <tr>
                <td colspan="2" align="center">
                    <xsl:value-of select="$message"/>
                </td>
            </tr>
            <tr>
                <td colspan="2" align="center">
                    <h2>FGDC Identification Information</h2>
                </td>
            </tr>
            <!-- don't want to conflict with the eml-physical.xsl template -->
            <xsl:for-each select="citation">
            	<xsl:call-template name="fgdcCitation"/>
            </xsl:for-each>
            <xsl:apply-templates select="descript"/>
            <xsl:apply-templates select="spdom"/>
            <xsl:apply-templates select="keywords"/>
        </table>
    </xsl:template>
    
    <xsl:template name="fgdcCitation">
        <xsl:apply-templates select="citeinfo"/>
    </xsl:template>
    
    <xsl:template match="citeinfo">
        <tr>
            <td colspan="2" class="tablehead">Citation Information</td>
        </tr>
        
        <tr>
            <xsl:apply-templates select="title"/>
        </tr>
        <tr>
            <xsl:call-template name="docid"/>
        </tr>
        <xsl:for-each select="origin">
	        <tr>
	            <xsl:apply-templates select="."/>
	        </tr>
	    </xsl:for-each> 
        <tr>
            <xsl:apply-templates select="pubdate"/>
        </tr>
        <tr>
            <xsl:apply-templates select="edition"/>
        </tr>
        <tr>
            <xsl:apply-templates select="geoform"/>
        </tr>
        <tr>
            <xsl:apply-templates select="pubinfo"/>
        </tr>
        <tr>
            <xsl:apply-templates select="othercit"/>
        </tr>
    </xsl:template>
    
    <xsl:template match="descript">
        <tr>
            <td colspan="2" class="tablehead">
                Description
            </td>
        </tr>
        <tr>
            <xsl:apply-templates select="abstract"/>
        </tr>
        <tr>
            <xsl:apply-templates select="purpose"/>
        </tr>
    </xsl:template>
    
    <xsl:template match="spdom">
        <tr>
            <xsl:apply-templates select="bounding"/>
        </tr>
    </xsl:template>
    
    <xsl:template match="keywords">
        <tr>
            <xsl:apply-templates select="theme"/>
        </tr>
        <tr>
            <xsl:apply-templates select="place"/>
        </tr>
    </xsl:template>
    
    <xsl:template match="theme">
        <td class="highlight" valign="top">Theme Keywords</td>
        <td class="secondCol">
            <xsl:apply-templates select="themekey[position()!=last()]">
                <xsl:with-param name="lastPlace">f</xsl:with-param>
            </xsl:apply-templates>
            <xsl:apply-templates select="themekey[position()=last()]">
                <xsl:with-param name="lastPlace">t</xsl:with-param>
            </xsl:apply-templates>
        </td>
    </xsl:template>
    
    <xsl:template match="place">
        <td class="highlight" valign="top">Place Keywords</td>
        <td class="secondCol">
            <xsl:apply-templates select="placekey[position()!=last()]">
                <xsl:with-param name="lastPlace">f</xsl:with-param>
            </xsl:apply-templates>
            <xsl:apply-templates select="placekey[position()=last()]">
                <xsl:with-param name="lastPlace">t</xsl:with-param>
            </xsl:apply-templates>
        </td>
    </xsl:template>
    
    <xsl:template match="themekey">
        <xsl:param name="lastPlace"/>
        <xsl:value-of select="."/>
        <xsl:if test="$lastPlace = 'f'">, </xsl:if>
    </xsl:template>
    
    <xsl:template match="placekey">
        <xsl:param name="lastPlace"/>
        <xsl:value-of select="."/>
        <xsl:if test="$lastPlace = 'f'">, </xsl:if>
    </xsl:template>
    
    <xsl:template match="bounding">
        <td class="highlight" valign="top">Bounding Coordinates</td>
        <td class="secondCol">
            <table class="subGroup subGroup_border">
                <tr>
                    <td class="highlight" align="right">West</td>
                    <td class="secondCol">
                        <xsl:value-of select="westbc"/>
                    </td>
                    <td class="highlight" align="right">East</td>
                    <td class="secondCol">
                        <xsl:value-of select="eastbc"/>
                    </td>
                </tr>
                <tr>
                    <td class="highlight" align="right">North</td>
                    <td class="secondCol">
                        <xsl:value-of select="northbc"/>
                    </td>
                    <td class="highlight" align="right">South</td>
                    <td class="secondCol">
                        <xsl:value-of select="southbc"/>
                    </td>
                </tr>
            </table>
        </td>
    </xsl:template>
    
    <xsl:template match="abstract">
        <td class="highlight" valign="top">Abstract</td>
        <td class="secondCol">
            <xsl:value-of select="."/>
        </td>
    </xsl:template>
    
    <xsl:template match="purpose">
        <td class="highlight" valign="top">Purpose</td>
        <td class="secondCol">
            <xsl:value-of select="."/>
        </td>
    </xsl:template>
    
    <xsl:template match="title">
        <td class="highlight">Title</td>
        <td class="secondCol">
            <xsl:value-of select="."/>
        </td>
    </xsl:template>
    
     <xsl:template name="docid">
        <td class="highlight">ID</td>
        <td class="secondCol">
            <xsl:value-of select="$docid"/>
        </td>
    </xsl:template>
    
    <xsl:template match="origin">
        <td class="highlight">Originator </td>
        <td class="secondCol">
            <xsl:value-of select="."/>
        </td>
    </xsl:template>
    
    <xsl:template match="pubdate">
        <td class="highlight">Publication Date</td>
        <td class="secondCol">
            <!-- <xsl:call-template name="long_date">-->
            <xsl:call-template name="date_as_is">
                <xsl:with-param name="date">
                    <xsl:value-of select="."/>
                </xsl:with-param>
            </xsl:call-template>
        </td>
    </xsl:template>
    
    <xsl:template match="edition">
        <td class="highlight">Edition</td>
        <td class="secondCol">
            <xsl:value-of select="."/>
        </td>
    </xsl:template>
    
    <xsl:template match="geoform">
        <td class="highlight" valign="top">Geospatial Data Presentation Form</td>
        <td class="secondCol">
            <xsl:value-of select="."/>
        </td>
    </xsl:template>
    
    <xsl:template match="othercit">
        <td class="highlight" valign="top">Other Citation Details</td>
        <td class="secondCol">
            <xsl:value-of select="."/>
        </td>
    </xsl:template>
    
    <xsl:template match="pubinfo">
        <td class="highlight" valign="top">Publication Information </td>
        <td class="secondCol">
            <table class="subGroup subGroup_border">
                <tr>
                    <xsl:apply-templates select="pubplace"/>
                </tr>
                <tr>
                    <xsl:apply-templates select="publish"/>
                </tr>
            </table>
        </td>
    </xsl:template>
    
    <xsl:template match="pubplace">
        <td class="highlight">Publication Place</td>
        <td class="secondCol">
            <xsl:value-of select="."/>
        </td>
    </xsl:template>
    
    <xsl:template match="publish">
        <td class="highlight">Publisher</td>
        <td class="secondCol">
            <xsl:value-of select="."/>
        </td>
    </xsl:template>
    
    <xsl:template match="distinfo">
        <table width="100%" border="0" cellspacing="0" class="subGroup subGroup_border">
            <tr>
                <td colspan="5" align="center">
                    <h2>Data Package Information</h2>
                </td>
            </tr>
            <tr>
                <td class="tablehead">
                    File type
                </td>
                <td class="tablehead" colspan="2">
                    File name (Doc Id)
                </td>
                <xsl:if test="$enableFGDCediting = 'true' and not($sessionid = '')">
                    <td align="left" colspan="2" class="tablehead">
                        Actions...
                    </td>
                </xsl:if>
            </tr>
            <tr>
                <xsl:apply-templates select="resdesc"/>
            </tr>
            <xsl:apply-templates select="stdorder/digform/digtopt/onlinopt/computer/networka"/>
        </table>
    </xsl:template>
    
    <xsl:template match="resdesc">
        <td class="highlight" align="left" valign="top">Metadata</td>
        <td class="secondCol" align="left" valign="top">
        	<xsl:value-of select="../custom"/> (<xsl:value-of select="."/>)
        	
        	<!-- update -->
		<xsl:if test="$enableFGDCediting = 'true' and not($sessionid = '')">
			<br/>
			<br/>
			<form method="post" action="{$contextURL}/style/common/ClientViewHelper.jspx"
                     enctype="multipart/form-data">
                   <input name="action" value="Update Metadata" type="hidden"/>
                   <input name="docid" type="hidden" value="{.}"/>
                   <input name="qformat" type="hidden" value="{$qformat}"/>
                   <input name="metadataDocId" type="hidden" value="{.}"/>
                   <input name="updateFile" type="file"/>
                   <input name="submit" value="Update" type="submit"/>
               </form>
		</xsl:if>
		
       </td>
       <!-- download -->
       <td class="secondCol" valign="bottom">
           <form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx">
               <input name="action" value="Download" type="submit"/>
               <input name="docid" type="hidden" value="{.}"/>
               <input name="qformat" type="hidden" value="{$qformat}"/>
               <input name="metadataDocId" type="hidden" value="{.}"/>
           </form>
       </td>
       
       <xsl:if test="$enableFGDCediting = 'true' and not($sessionid = '')">
       	<!-- delete -->
       	<td class="secondCol" valign="bottom">
           	<form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx"
                     enctype="text/html">
                   <input name="action" value="Delete" type="submit"/>
                   <input name="docid" type="hidden" value="{.}"/>
                   <input name="qformat" type="hidden" value="{$qformat}"/>
                   <input name="metadataDocId" type="hidden" value="{.}"/>
               </form>
           </td>
           <!-- set access -->
       	<td class="secondCol" valign="bottom">
			<form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx">
                   <input name="action" value="Set Access" type="submit"/>
                   <xsl:choose>
                       <xsl:when test="$publicRead = 'true'">
                           <input name="publicAccess" type="checkbox" checked=''/> Public read     
                       </xsl:when>
                       <xsl:otherwise>
                           <input name="publicAccess" type="checkbox"/> Public read                         
                       </xsl:otherwise>
                   </xsl:choose>
                   <input name="docid" type="hidden" value="{.}"/>
                   <input name="qformat" type="hidden" value="{$qformat}"/>
                   <input name="metadataDocId" type="hidden" value="{.}"/>
                   <input name="contentStandard" type="hidden" value="FGDC"/>
			</form>
		</td>
	</xsl:if>
   </xsl:template>
   
   <xsl:template match="networka">
       <tr>
           <xsl:apply-templates select="networkr"/>
       </tr>
   </xsl:template>
   
   <xsl:template match="networkr">
	<xsl:if test="not(./ancestor::digform/digtinfo/formcont = '')">
       <td class="highlight" align="left" valign="top">Data</td>
       <td class="secondCol" align="left" valign="top">
           <xsl:value-of select="./ancestor::digform/digtinfo/formcont"/> (<xsl:value-of select="."/>)
       	<!-- update -->
		<xsl:if test="$enableFGDCediting = 'true' and not($sessionid = '')">
			<br/>
			<br/>
               <form method="post" action="{$contextURL}/style/common/ClientViewHelper.jspx"
                     enctype="multipart/form-data">
                   <input name="action" value="Update Data" type="hidden"/>
                   <input name="docid" type="hidden" value="{.}"/>
                   <input name="qformat" type="hidden" value="{$qformat}"/>
                   <input name="metadataDocId" type="hidden" value="{$docid}"/>
                   <input name="updateFile" type="file"/>
                   <input name="submit" value="Update" type="submit"/>                    
               </form>
		</xsl:if>                        
       </td>
       <!-- download -->
       <td class="secondCol" valign="bottom">
           <form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx" enctype="application/octet-stream">
               <input name="action" value="Download" type="submit"/>
               <input name="docid" type="hidden" value="{.}"/>
               <input name="qformat" type="hidden" value="{$qformat}"/>
               <input name="metadataDocId" type="hidden" value="{$docid}"/>
           </form>
       </td>
       <xsl:if test="$enableFGDCediting = 'true' and not($sessionid = '')">
       	<!-- delete -->
       	<td class="secondCol" valign="bottom">
       		<form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx"
                     enctype="text/html">
                   <input name="action" value="Delete" type="submit"/>
                   <input name="docid" type="hidden" value="{.}"/>
                   <input name="qformat" type="hidden" value="{$qformat}"/>
                   <input name="metadataDocId" type="hidden" value="{$docid}"/>
               </form>
       	</td>
       	<!-- set access -->
       	<td class="secondCol" valign="bottom"></td>
       	
		</xsl:if>
	</xsl:if>
   </xsl:template>
   
   <xsl:template name="date_as_is">
       <xsl:param name="date"/>
       <xsl:value-of select="$date"/>
   </xsl:template>
   
   <xsl:template name="long_date">
       <xsl:param name="date"/>
       <!-- Day -->
       <xsl:value-of select="number(substring($date, 7, 2))"/>
       <xsl:text> </xsl:text>
       <!-- Month -->
       <xsl:variable name="month" select="number(substring($date, 5, 2))"/>
       <xsl:choose>
           <xsl:when test="$month=1">January</xsl:when>
           <xsl:when test="$month=2">February</xsl:when>
           <xsl:when test="$month=3">March</xsl:when>
           <xsl:when test="$month=4">April</xsl:when>
           <xsl:when test="$month=5">May</xsl:when>
           <xsl:when test="$month=6">June</xsl:when>
           <xsl:when test="$month=7">July</xsl:when>
           <xsl:when test="$month=8">August</xsl:when>
           <xsl:when test="$month=9">September</xsl:when>
           <xsl:when test="$month=10">October</xsl:when>
           <xsl:when test="$month=11">November</xsl:when>
           <xsl:when test="$month=12">December</xsl:when>
           <xsl:otherwise>INVALID MONTH</xsl:otherwise>
       </xsl:choose>
       <xsl:text> </xsl:text>
       <!-- Year -->
        <xsl:value-of select="substring($date, 1, 4)"/>
    </xsl:template>
    
</xsl:stylesheet>
