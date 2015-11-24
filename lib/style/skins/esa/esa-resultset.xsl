<?xml version="1.0"?>
<!--
*  '$RCSfile$'
*      Authors: Matt Jones, CHad Berkley
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
* convert an XML file showing the resultset of a query
* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>
  <xsl:param name="cgi-prefix"/>
  <xsl:param name="sessid"/>
  <xsl:param name="contextURL"/>
  <xsl:param name="qformat">default</xsl:param>
  <xsl:param name="enableediting">false</xsl:param>
  <xsl:param name="isModerator">false</xsl:param>
  <xsl:param name="message"></xsl:param>
  <xsl:template match="/">
   <html>
      <head>
        <link rel="stylesheet" type="text/css" 
              href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />
        <script language="Javascript" type="text/JavaScript"
                src="{$contextURL}/style/skins/{$qformat}/{$qformat}.js" />
        <script language="Javascript" type="text/JavaScript"
                src="{$contextURL}/style/common/branding.js" />
        <script language="JavaScript">
          <![CDATA[
          function submitform(action,form_id) {
          	var form_ref = document.getElementById(form_id);
              form_ref.action.value=action;
              form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
              form_ref.qformat.value="]]><xsl:value-of select="$qformat" /><![CDATA[";
              form_ref.submit();
          }
          function submitCGIform(form_id) {
          	var form_ref = document.getElementById(form_id);
              form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
              form_ref.cfg.value="]]><xsl:value-of select="$qformat" /><![CDATA[";
              form_ref.submit();
          }
          ]]>
        </script>
      </head>

      <body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
        <script language="JavaScript">
          <![CDATA[
          insertTemplateOpening("]]><xsl:value-of select="$contextURL" /><![CDATA[");
          insertSearchBox("]]><xsl:value-of select="$contextURL" /><![CDATA[");
         ]]>
        </script>
        <table width="100%" align="center" border="0" cellpadding="2" cellspacing="0">
           <tr>
             <td align="left"><br></br><p class="emphasis"><xsl:value-of select="$message"/><br></br><xsl:number value="count(resultset/document)" /> data set(s) found <xsl:if test="$enableediting ='true' and $isModerator = 'true'">for moderation</xsl:if></p></td>
           </tr></table>
<!-- This tests to see if there are returned documents,
            if there are not then don't show the query results -->

      <xsl:if test="count(resultset/document) &gt; 0">

         <table width="99%" align="center" border="0" cellpadding="0" cellspacing="0">
           <tr>
             <th class="tablehead_lcorner" align="right" valign="top"><img src="{$contextURL}/style/skins/default/images/transparent1x1.gif" width="1" height="1" /></th>
             <th class="tablehead" style="text-align: left">Title</th>
             <th width="15%" class="tablehead" style="text-align: left">Contacts</th>
             <th width="15%" class="tablehead" style="text-align: left">Organization</th>
             <th width="15%" class="tablehead" style="text-align: left">Keywords</th>
             <xsl:if test="$enableediting = 'true'">
               <th width="10%" class="tablehead" style="text-align: middle">Actions</th>
             </xsl:if>
             <th class="tablehead_rcorner" align="right" valign="top"><img src="{$contextURL}/style/skins/default/images/transparent1x1.gif" width="1" height="1" /></th>
           </tr>

         <xsl:for-each select="resultset/document">
           <xsl:sort select="./param[@name='dataset/title']"/>
           <tr valign="top" class="subpanel">
             <xsl:attribute name="class">
               <xsl:choose>
                 <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
                 <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
               </xsl:choose>
             </xsl:attribute>

             <td width="10">&#160;</td>
             <td class="text_plain">
	       
	     <xsl:choose>
             <xsl:when test="$enableediting = 'true'">
               <form action="{$cgi-prefix}/register-dataset.cgi" method="POST">
                 <xsl:attribute name="name">
                   <xsl:value-of select="translate(./docid, '()-.', '____')" />
                 </xsl:attribute>
                 <xsl:attribute name="id">
                   <xsl:value-of select="translate(./docid, '()-.', '____')" />
                 </xsl:attribute>
                 
                 <input type="hidden" name="cfg" />
                 <input type="hidden" name="sessionid" />
	 	 <input type="hidden" name="enableediting" value="{$enableediting}"/>
                 <input type="hidden" name="stage" value="read"/>
                 <input type="hidden" name="docid">
                   <xsl:attribute name="value">
                     <xsl:value-of select="./docid"/>
                   </xsl:attribute>
                 </input>
                 <a>
                   <xsl:attribute name="href">javascript:submitCGIform("<xsl:value-of select="translate(./docid, '()-.', '____')"/>")</xsl:attribute>
                   <xsl:text>&#187;&#160;</xsl:text>
                   <xsl:choose>
                     <xsl:when test="./param[@name='dataset/title']!=''">
                        <xsl:value-of select="./param[@name='dataset/title']"/>
                     </xsl:when>
                     <xsl:otherwise>
                       <xsl:value-of select="./param[@name='citation/title']"/>
                       <xsl:value-of select="./param[@name='software/title']"/>
                       <xsl:value-of select="./param[@name='protocol/title']"/>
                     </xsl:otherwise>
                   </xsl:choose>
                 </a><br />
                 <br/>
                 <p><pre>ID: <xsl:value-of select="./docid"/></pre></p>
               </form>
	      </xsl:when>
	      <xsl:otherwise>
		 <form action="{$contextURL}/metacat" method="POST">
                 <xsl:attribute name="name">
                   <xsl:value-of select="translate(./docid, '()-.', '____')" />
                 </xsl:attribute>
                 <xsl:attribute name="id">
                   <xsl:value-of select="translate(./docid, '()-.', '____')" />
                 </xsl:attribute>

                 <input type="hidden" name="qformat" />
                 <input type="hidden" name="sessionid" />
                 <xsl:if test="$enableediting = 'true'">
                           <input type="hidden" name="enableediting" value="{$enableediting}"/>
                 </xsl:if>
                 <input type="hidden" name="action" value="read"/>
                 <input type="hidden" name="docid">
                   <xsl:attribute name="value">
                     <xsl:value-of select="./docid"/>
                   </xsl:attribute>
                 </input>
                 <xsl:for-each select="./relation">
                   <input type="hidden" name="docid">
                     <xsl:attribute name="value" >
                       <xsl:value-of select="./relationdoc" />
                     </xsl:attribute>
                   </input>
                 </xsl:for-each>
                 <a>
                   <xsl:attribute name="href">javascript:submitform("read", "<xsl:value-of select="translate(./docid, '()-.', '____')"/>")</xsl:attribute>
                   <xsl:text>&#187;&#160;</xsl:text>
                   <xsl:choose>
                     <xsl:when test="./param[@name='dataset/title']!=''">
                        <xsl:value-of select="./param[@name='dataset/title']"/>
                     </xsl:when>
                     <xsl:otherwise>
                       <xsl:value-of select="./param[@name='citation/title']"/>
                       <xsl:value-of select="./param[@name='software/title']"/>
                       <xsl:value-of select="./param[@name='protocol/title']"/>
                     </xsl:otherwise>
                   </xsl:choose>
                 </a><br />
                 <br/>
                 <p><pre>ID: <xsl:value-of select="./docid"/></pre></p>
               </form>
	      </xsl:otherwise>
	      </xsl:choose>
             </td>

             <td class="text_plain">
               <xsl:for-each select="./param[@name='originator/individualName/surName']" >
                 <xsl:value-of select="." />
                 <br/>
                </xsl:for-each>
               <xsl:for-each select="./param[@name='creator/individualName/surName']" >
                 <xsl:value-of select="." />
                 <br/>
               </xsl:for-each>

             </td>
             <td class="text_plain">
                 <xsl:value-of select="./param[@name='originator/organizationName']" />
                 <xsl:value-of select="./param[@name='creator/organizationName']" />

             </td>

             <td class="text_plain">
               <xsl:for-each
                select="./param[@name='keyword']">
                 <xsl:value-of select="." />
                 <br/>
               </xsl:for-each>

             </td>
	   
             <xsl:if test="$enableediting = 'true'">
               <xsl:choose>
                     <xsl:when test="$isModerator = 'true'">
               		<td class="text_plain">
	       		<form action="{$cgi-prefix}/register-dataset.cgi" method="POST">
	               	<input type="hidden" name="stage" value="mod_accept"/>
	 	       	<input type="hidden" name="cfg" value="{$qformat}"/>
		       	<input type="hidden" name="sessionid"  value="{$sessid}"/>
                       	<input type="hidden" name="docid">
                       		<xsl:attribute name="value">
                          		<xsl:value-of select="./docid"/>
                       		</xsl:attribute>
                       	</input>
                       	<center>
		       		<input type="SUBMIT"  value=" Accept " name="Accept">
 	               		</input>
	               	</center>
	             </form>
	       	     <form action="{$cgi-prefix}/register-dataset.cgi" method="POST">
	               <input type="hidden" name="stage" value="mod_decline"/>
	 	       <input type="hidden" name="cfg" value="{$qformat}"/>
		       <input type="hidden" name="sessionid"  value="{$sessid}"/>
                   	<input type="hidden" name="docid">
                     		<xsl:attribute name="value">
                       			<xsl:value-of select="./docid"/>
                     		</xsl:attribute>
                   	</input>
                     	<center>
		        	<input type="SUBMIT"  value="Decline" name="Decline">
 	                 	</input>
	                </center>
	            </form>
                    <form action="{$cgi-prefix}/register-dataset.cgi" method="POST">
	               <input type="hidden" name="stage" value="mod_revise"/>
	 	       <input type="hidden" name="cfg" value="{$qformat}"/>
		       <input type="hidden" name="sessionid"  value="{$sessid}"/>
                   	<input type="hidden" name="docid">
                     		<xsl:attribute name="value">
                       			<xsl:value-of select="./docid"/>
                     		</xsl:attribute>
                   	</input>
                   	<center>
		             	<input type="SUBMIT"  value=" Revise " name="Revise">
 	                 	</input>
	               </center>
	             </form>
	           </td>	  
		  </xsl:when>
		  <xsl:otherwise>
               	     <td class="text_plain">
			<form action="{$cgi-prefix}/register-dataset.cgi" method="POST">
                       		<input type="hidden" name="stage" value="read"/>
                        	<input type="hidden" name="cfg" value="{$qformat}"/>
                                <input type="hidden" name="sessionid"  value="{$sessid}"/>
                       		<input type="hidden" name="docid">
                       			<xsl:attribute name="value">
                          			<xsl:value-of select="./docid"/>
                       			</xsl:attribute>
                       		</input>
                       		<center>
                       		<input type="SUBMIT"  value=" View " name="View"></input>
                       		</center>
                     	</form>
                 	<form action="{$cgi-prefix}/register-dataset.cgi" 
                       		method="POST">
                       <input type="hidden" name="stage" value="modify"/>        
                            <input type="hidden" name="cfg" value="{$qformat}"/>
                                        <input type="hidden" name="sessionid"  value="{$sessid}"/>
                   <input type="hidden" name="docid">
                     <xsl:attribute name="value">
                       <xsl:value-of select="./docid"/>
                     </xsl:attribute>
                   </input>
                   <center>
                             <input type="SUBMIT"  value=" Edit " name="Edit">
                         </input>
                       </center>
                     </form>
		     </td>
		  </xsl:otherwise>
             </xsl:choose>
             </xsl:if>
             <td width="10">&#160;</td>
             </tr>
             <tr class="searchresultsdivider"><td colspan="6">
             <img src="{$contextURL}/style/skins/default/images/transparent1x1.gif" width="1" height="1" /></td></tr>

          </xsl:for-each>
          </table>

       </xsl:if>
      <script language="JavaScript">
        <![CDATA[
          insertTemplateClosing("]]><xsl:value-of select="$contextURL" /><![CDATA[");
        ]]>
      </script>
    </body>
    </html>
    </xsl:template>

</xsl:stylesheet>
