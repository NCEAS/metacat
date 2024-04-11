<?xml version="1.0"?>
<!--
	* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
	* convert an XML file showing the resultset of a query
	* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">
    <xsl:template name="resultstablesolr">
        <body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
            
            <p class="emphasis">
                <xsl:number value="count(response/result/doc)" />
                data packages found
            </p>
            
            <!-- This tests to see if there are returned documents,
					if there are not then don't show the query results -->
            <xsl:if test="count(response/result/doc) &gt; 0">
                
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
                    
                    <xsl:for-each select="response/result/doc">                        
                        <xsl:variable name="viewLink">
                        	<xsl:value-of select="$contextURL" /><![CDATA[/metacat?action=read]]><![CDATA[&qformat=]]><xsl:value-of select="$qformat" /><![CDATA[&sessionid=]]><xsl:value-of select="$sessid" /><![CDATA[&pid=]]><xsl:value-of select="./str[@name='id']" /><xsl:if test="$enableediting = 'true'"><![CDATA[&enableediting=]]><xsl:value-of select="$enableediting" /></xsl:if>
                        </xsl:variable>
                        
                        <tr valign="top" class="subpanel">
                            <xsl:attribute name="class">
                                <xsl:choose>
                                    <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                                    <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
                                </xsl:choose>
                            </xsl:attribute>
                            
                            <td width="10">&#160;</td>
                            <td class="text_plain">
                            
	                            <a>
	                                <xsl:attribute name="href">
	                                	<xsl:value-of select="$viewLink" />
	                                </xsl:attribute>
	                                <xsl:text>
	                                    &#187;&#160;
	                                </xsl:text>
	                                
	                                <!-- <xsl:value-of select="./str[@name='title']" />-->
	                                <!-- <xsl:value-of select="./str[@name='author']" /> -->
	                                <xsl:for-each select="./arr[@name='authorLastName']" >
	                                	<xsl:value-of select="./str" />
	                                </xsl:for-each>.
	                                <xsl:value-of select="./str[@name='title']" />. <xsl:value-of select="./str[@name='pubDate']" />

	                            </a>
	                            <p>
	                                <b><xsl:value-of select="./str[@name='id']" /></b>
	                            </p>
	                            <p>
	                            	<xsl:value-of select="substring(./str[@name='abstract'], 0, 140)" />...
	                            	<a>
		                            	<xsl:attribute name="href">
		                                	<xsl:value-of select="$viewLink" />
		                                </xsl:attribute>
		                            	(more)
	                            	</a>
	                            </p>
	                            <br />
                            </td>
                            
                            <td class="text_plain">
                                <xsl:for-each
                                    select="./str[@name='author']">
                                    <xsl:value-of select="." />
                                    <br />
                                </xsl:for-each>
                                <xsl:for-each
                                    select="./str[@name='investigator']">
                                    <xsl:value-of select="." />
                                    <br />
                                </xsl:for-each>
                                <!--
                                <xsl:for-each
                                    select="./arr[@name='origin']">
                                    <xsl:value-of select="./str" />
                                    <br />
                                </xsl:for-each>
                                -->
                                <!--  
                                <xsl:for-each
                                    select="./str[@name='rightsHolder']">
                                    <xsl:value-of select="." />
                                    <br />
                                </xsl:for-each>
                                -->
                                
                            </td>
                            
                            <td class="text_plain">
                            	<xsl:for-each
                                    select="./arr[@name='contactOrganization']">
                                    <xsl:value-of select="./str" />
                                    <br />
                                </xsl:for-each>
                            </td>
                            
                            <td class="text_plain">
                                <xsl:for-each
                                    select="./arr[@name='keywords']">
                                    <xsl:value-of select="./str" />
                                    <br />
                                </xsl:for-each>
                                <xsl:for-each
                                    select="./arr[@name='keyConcept']">
                                    <xsl:value-of select="./str" />
                                    <br />
                                </xsl:for-each>
                            </td>
                            
                            <xsl:if test="$enableediting = 'true'">
                                <td class="text_plain">
                                   	<!-- for cgi-based editing -->
                                    <form action="{$cgi-prefix}/register-dataset.cgi" method="POST">
                                        <input type="hidden" name="stage" value="modify" />
                                        <input type="hidden" name="cfg" value="{$qformat}" />
                                        <input type="hidden" name="sessionid" value="{$sessid}" />
                                        <input type="hidden" name="pid">
                                            <xsl:attribute name="value">
                                                <xsl:value-of select="./str[@name='id']" />
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
                                        <input type="hidden" name="pid">
                                            <xsl:attribute name="value">
                                                <xsl:value-of select="./str[@name='id']" />
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
