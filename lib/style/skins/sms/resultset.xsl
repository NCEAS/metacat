<?xml version="1.0"?> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="/">
    <table width="100%">
      <tr>
        <th>
          Document ID
        </th>
        <th>
          &#160;
        </th>
        <th>
          Title
        </th>
        <th>
          Document Type
        </th>
      </tr>
      <xsl:apply-templates select="//document"/>
    </table>
    
    <!-- page navigation-->
    <div class="resultnavbar">
      <!--previous-->
      <xsl:choose>
        <xsl:when test="//pagestart = 0">
          &#8592; previous
        </xsl:when>
        <xsl:otherwise>
          <a>
            <xsl:attribute name="href">
              <xsl:if test="/resultset/query/pathquery/querygroup/queryterm/value!='%'">
                javascript:reloadSearchContent('/sms/metacat?action=query&amp;anyfield=<xsl:value-of select="/resultset/query/pathquery/querygroup/queryterm/value"/>&amp;qformat=sms&amp;returnfield=dataset/title&amp;pagesize=10&amp;pagestart=<xsl:value-of select="//previouspage"/>');
              </xsl:if>
              <xsl:if test="/resultset/query/pathquery/querygroup/queryterm/value='%'">
                javascript:reloadSearchContent('/sms/metacat?action=query&amp;anyfield=<xsl:value-of select="'%25'"/><xsl:value-of select="'25'"/>&amp;qformat=sms&amp;returnfield=dataset/title&amp;pagesize=10&amp;pagestart=<xsl:value-of select="//previouspage"/>');
              </xsl:if>
            </xsl:attribute>
              &#8592; previous
          </a>
        </xsl:otherwise>
      </xsl:choose>
            
      &#160; &#160; 
      
      <!--next-->
      <xsl:choose>
        <xsl:when test="//lastpage = 'true'">
          next &#8594;
        </xsl:when>
        <xsl:otherwise>
          <a>
          <xsl:attribute name="href">
            <xsl:choose>
            <xsl:when test="/resultset/query/pathquery/querygroup/queryterm/value!='%'">
              javascript:reloadSearchContent('/sms/metacat?action=query&amp;anyfield=<xsl:value-of select="/resultset/query/pathquery/querygroup/queryterm/value"/>&amp;qformat=sms&amp;returnfield=dataset/title&amp;pagesize=10&amp;pagestart=<xsl:value-of select="//nextpage"/>');
            </xsl:when>
            <xsl:when test="/resultset/query/pathquery/querygroup/queryterm/value='%'">
              javascript:reloadSearchContent('/sms/metacat?action=query&amp;anyfield=<xsl:value-of select="'%25'"/><xsl:value-of select="'25'"/>&amp;qformat=sms&amp;returnfield=dataset/title&amp;pagesize=10&amp;pagestart=<xsl:value-of select="//nextpage"/>');
            </xsl:when>
            </xsl:choose>
           
          </xsl:attribute>
            next &#8594;
          </a>
        </xsl:otherwise>
      </xsl:choose>
    </div>
  </xsl:template>
  
  <!--search results-->
  <xsl:template match="document">
    <tr>
      <td>
        <a><!--docid-->
        <xsl:attribute name="href">
          /sms/metacat?action=read&amp;qformat=sms&amp;docid=<xsl:value-of select="docid"/>
        </xsl:attribute>
        <xsl:value-of select="docid"/>
        </a>
      </td>
      <td><!--xml link-->
        <a>
          <xsl:attribute name="href">
            /sms/metacat?action=read&amp;qformat=xml&amp;docid=<xsl:value-of select="docid"/>
          </xsl:attribute>
          <img width="25px" src="style/skins/sms/xml-button.png"/>
        </a>
      </td>
      <td> <!--title of the doc if it's eml-->
        <xsl:choose>
          <xsl:when test="param[@name='dataset/title'] != ''">
            <xsl:value-of select="param[@name='dataset/title']"/>
          </xsl:when>
          <xsl:otherwise>
            No Title
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td><!--doc type-->
        <xsl:choose>
        <xsl:when test="doctype='eml://ecoinformatics.org/eml-2.0.1'">
          EML 2.0.1 Document
        </xsl:when>
        <xsl:when test="doctype='rdf:RDF'">
          RDF Ontology
        </xsl:when>
        <xsl:when test="doctype='http://daks.ucdavis.edu/sms-annot-1.0.0rc1'">
          SMS Annotation
        </xsl:when>
        <!--add more doctypes here-->
        <xsl:otherwise>
          <xsl:value-of select="doctype"/>
        </xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>
  
  
  

</xsl:stylesheet>
