<?xml version="1.0"?>
<!--
	* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
	* convert an XML file showing the resultset of a query
	* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">
    <xsl:template name="resultstable">
        <body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
            
            <p class="emphasis">
                <xsl:number value="count(resultset/document)" />
                data packages found
            </p>
            
            <!-- This tests to see if there are returned documents,
					if there are not then don't show the query results -->
            <xsl:if test="count(resultset/document) &gt; 0">
                
                <table class="resultstable" width="95%" align="center" border="0" cellpadding="0" cellspacing="0">
                    <tr>
                        <th class="tablehead_lcorner" align="right" valign="top">
                            <img src="{$contextURL}/style/skins/default/images/transparent1x1.gif" width="1" height="1" />
                        </th>
                        <th class="tablehead" style="text-align: left">
                            Title
                        </th>
                        <th width="15%" class="tablehead" style="text-align: left">
                            Contacts
                        </th>
                        <th width="15%" class="tablehead" style="text-align: left">
                            Organization
                        </th>
                        <th width="15%" class="tablehead" style="text-align: left">
                            Keywords
                        </th>
                        <xsl:if test="$enableediting = 'true'">
                            <th width="10%" class="tablehead" style="text-align: middle">
                                Actions
                            </th>
                        </xsl:if>
                        <th class="tablehead_rcorner" align="right" valign="top">
                            <img src="{$contextURL}/style/skins/default/images/transparent1x1.gif" width="1" height="1" />
                        </th>
                    </tr>
                    
                    <xsl:for-each select="resultset/document">
                        <xsl:sort select="./param[@name='dataset/title']" />
                        <tr valign="top" class="subpanel">
                            <xsl:attribute name="class">
                                <xsl:choose>
                                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                                </xsl:choose>
                            </xsl:attribute>
                            
                            <td width="10">&#160;</td>
                            <td class="text_plain">
                                <form action="{$contextURL}/metacat" method="POST">
                                    <xsl:attribute name="name"><xsl:value-of
                                        select="translate(./docid, '()-.', '____')" /></xsl:attribute>
                                    
                                    <input type="hidden" name="qformat" />
                                    <input type="hidden" name="sessionid" />
                                    <xsl:if
                                        test="$enableediting = 'true'">
                                        <input type="hidden" name="enableediting" value="{$enableediting}" />
                                    </xsl:if>
                                    <input type="hidden" name="action" value="read" />
                                    <input type="hidden" name="docid">
                                        <xsl:attribute
                                            name="value"><xsl:value-of
                                            select="./docid" /></xsl:attribute>
                                    </input>
                                    <xsl:for-each
                                        select="./relation">
                                        <input type="hidden" name="docid">
                                            <xsl:attribute
                                                name="value"><xsl:value-of
                                                select="./relationdoc" /></xsl:attribute>
                                        </input>
                                    </xsl:for-each>
                                    
                                    <a>
                                        <xsl:attribute
                                            name="href">javascript:submitform('read',document.<xsl:value-of
                                            select="translate(./docid, '()-.', '____')" />)</xsl:attribute>
                                        <xsl:text>
                                            &#187;&#160;
                                        </xsl:text>
                                        <xsl:choose>
                                            <xsl:when test="./param[@name='dataset/title']!=''">
                                            	<xsl:value-of select="./param[@name='dataset/title']" />
                                            	<xsl:if test="count(./param[@name='dataset/title/value']) != 0">
                                            		<br/>
	                                            	<xsl:for-each select="./param[@name='dataset/title/value']" >
														<xsl:value-of select="." />
														<br/>
	                                            	</xsl:for-each>
	                                            </xsl:if>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="./param[@name='citation/title']" />
                                                <xsl:value-of select="./param[@name='software/title']" />
                                                <xsl:value-of select="./param[@name='protocol/title']" />
                                                <xsl:value-of select="./param[@name='idinfo/citation/citeinfo/title']" />
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </a>
                                    <br />
                                    <br />
                                    <p>
                                        <pre>ID: <xsl:value-of select="./docid" /></pre>
                                    </p>
                                    
                                </form>
                            </td>
                            
                            <td class="text_plain">
                                <xsl:for-each
                                    select="./param[@name='originator/individualName/surName']">
                                    <xsl:value-of select="." />
                                    <br />
                                </xsl:for-each>
                                <xsl:for-each
                                    select="./param[@name='creator/individualName/surName']">
                                    <xsl:value-of select="." />
                                    <br />
                                </xsl:for-each>
                                <xsl:for-each
                                    select="./param[@name='idinfo/citation/citeinfo/origin']">
                                    <xsl:value-of select="." />
                                    <br />
                                </xsl:for-each>
                                
                            </td>
                            <td class="text_plain">
                                <xsl:value-of
                                    select="./param[@name='originator/organizationName']" />
                                <xsl:value-of
                                    select="./param[@name='creator/organizationName']" />
                                
                            </td>
                            
                            <td class="text_plain">
                                <xsl:for-each
                                    select="./param[@name='keyword']">
                                    <xsl:value-of select="." />
                                    <br />
                                </xsl:for-each>
                                <xsl:for-each
                                    select="./param[@name='keyword/value']">
                                    <xsl:value-of select="." />
                                    <br />
                                </xsl:for-each>
                                <xsl:for-each
                                    select="./param[@name='idinfo/keywords/theme/themekey']">
                                    <xsl:value-of select="." />
                                    <br />
                                </xsl:for-each>
                                
                            </td>
                            
                            <xsl:if test="$enableediting = 'true'">
                                <td class="text_plain">
                                    <form action="{$contextURL}/metacat" method="POST">
                                        <input type="hidden" name="action" value="read" />
                                        <input type="hidden" name="qformat" value="{$qformat}" />
                                        <input type="hidden" name="sessionid" value="{$sessid}" />
                                        <input type="hidden" name="docid">
                                            <xsl:attribute name="value">
                                                <xsl:value-of select="./docid" />
                                            </xsl:attribute>
                                        </input>
                                        <center>
                                            <input type="SUBMIT" value=" View " name="View">
                                            </input>
                                        </center>
                                    </form>
                                    <form action="{$cgi-prefix}/register-dataset.cgi" method="POST">
                                        <input type="hidden" name="stage" value="modify" />
                                        <input type="hidden" name="cfg" value="{$qformat}" />
                                        <input type="hidden" name="sessionid" value="{$sessid}" />
                                        <input type="hidden" name="docid">
                                            <xsl:attribute name="value">
                                                <xsl:value-of select="./docid" />
                                            </xsl:attribute>
                                        </input>
                                        <center>
                                            <input type="SUBMIT" value=" Edit " name="Edit">
                                            </input>
                                        </center>
                                    </form>
                                    <form action="{$cgi-prefix}/register-dataset.cgi" method="POST">
                                        <input type="hidden" name="stage" value="delete" />
                                        <input type="hidden" name="cfg" value="{$qformat}" />
                                        <input type="hidden" name="sessionid" value="{$sessid}" />
                                        <input type="hidden" name="docid">
                                            <xsl:attribute name="value">
                                                <xsl:value-of select="./docid" />
                                            </xsl:attribute>
                                        </input>
                                        <center>
                                            <input type="SUBMIT" value="Delete" name="Delete">
                                            </input>
                                        </center>
                                    </form>
                                </td>
                            </xsl:if>
                            <td width="10">&#160;</td>
                        </tr>
                        <tr>
                            <td width="10" class="searchresultslead"></td>
                            <td colspan="5" class="searchresultsdivider">&#160;</td>
                        </tr>
                        
                    </xsl:for-each>
                </table>
                
            </xsl:if>
        </body>
    </xsl:template>
    
</xsl:stylesheet>
