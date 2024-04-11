<?xml version="1.0"?>
<!--
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file showing the resultset of a query
  * into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html"/>
  <xsl:param name="qformat">default</xsl:param>
  <xsl:param name="contextURL"/>
  <xsl:param name="cgi-prefix"/>
  <xsl:template match="/">
    <html>
      <head>
        <link rel="stylesheet" type="text/css" 
              href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />
      </head>

      <body>
        <table width="100%">
          <tr>
            <td rowspan="2"><img src="./images/KNBLogo.gif"/></td>
            <td colspan="7"><div class="title">Biocomplexity Data Search</div>
            </td>
          </tr>
          <tr>
            <td><a href="" class="toollink"> KNB </a></td>
            <td><a href="/data.html" class="toollink"> 
                Data </a></td>
            <td><a href="./people.html" class="toollink"> 
                People </a></td>
            <td><a href="./informatics" class="toollink"> 
                Informatics </a></td>
            <td><a href="./biodiversity" class="toollink"> 
                Biodiversity </a></td>
            <td><a href="./education" class="toollink"> 
                Education </a></td>
            <td><a href="./software" class="toollink"> 
                Software </a></td>
         </tr>
         <tr>
          <td align="right" valign="top" colspan="7">
            <form action="{$contextURL}/metacat" method="POST">
             Data Search: <input type="text" name="anyfield" size="10" />
             <input type="hidden" name="action" value="query" />   
             <input type="hidden" name="qformat" value="knb" />
             <input type="hidden" name="operator" value="UNION" />
             
             <input type="hidden" name="returnfield"
              value="creator/individualName/surName" />
             <input type="hidden" name="returnfield"
              value="creator/organizationName" />
             <input type="hidden" name="returnfield" 
              value="originator/individualName/surName" />
             <input type="hidden" name="returnfield" 
              value="originator/individualName/givenName" />
             <input type="hidden" name="returnfield" 
              value="originator/organizationName" />
             <input type="hidden" name="returnfield" 
              value="dataset/title" />
             <input type="hidden" name="returnfield" 
              value="keyword" />
             <input type="hidden" name="returndoctype"
              value="eml://ecoinformatics.org/eml-2.0.0" />
             <input type="hidden" name="returndoctype"
              value="eml://ecoinformatics.org/eml-2.0.1" />
             <input type="hidden" name="returndoctype"
              value="eml://ecoinformatics.org/eml-2.1.0" />
             <input type="hidden" name="returndoctype"
              value="eml://ecoinformatics.org/eml-2.1.1" /> 
             <input type="hidden" name="returndoctype"
              value="-//NCEAS//eml-dataset-2.0//EN" />
             <input type="hidden" name="returndoctype"
              value="-//NCEAS//resource//EN" />
             <input type="hidden" name="returndoctype"
              value="-//NCEAS//eml-dataset//EN" />
              <input type="hidden" name="returndoctype"
              value="-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN" />
              <input type="hidden" name="returndoctype"
              value="-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN" />
           </form>
          </td>
        </tr>  
       </table>
        
       <p><xsl:number value="count(resultset/document)" /> documents found.</p>
       <!-- This tests to see if there are returned documents,
            if there are not then don't show the query results -->
       <xsl:if test="count(resultset/document) &gt; 0">
           <h3>Browse data sets by keyword:</h3> 
           <ul>
               <xsl:apply-templates select="//param[@name='keyword' and not(. = preceding::param) and not(normalize-space(.) = '')]">
                   <xsl:sort select="."/>
               </xsl:apply-templates>
           </ul>
       </xsl:if>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="param">
     <li>
     <a>
       <xsl:attribute name="href">{$contextURL}/metacat?action=query&amp;operator=INTERSECT&amp;qformat=knb&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.1&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.0&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.0&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.1&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN&amp;returnfield=dataset/title&amp;returnfield=citation/title&amp;returnfield=software/title&amp;returnfield=protocol/title&amp;returnfield=keyword&amp;returnfield=originator/individualName/surName&amp;returnfield=creator/individualName/surName&amp;returnfield=originator/organizationName&amp;returnfield=creator/organizationName&amp;keyword=<xsl:value-of select="."/></xsl:attribute>
     <xsl:value-of select="."/>
     </a> (<xsl:value-of select="count(//param[.=current()])"/>)
     </li>
  </xsl:template>

</xsl:stylesheet>
