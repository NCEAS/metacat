<?xml version="1.0"?> 
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:owl="http://www.w3.org/2002/07/owl#"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  version="1.0">

  <xsl:template match="/">
  <html>
    <head>
      <link rel="stylesheet" type="text/css" href="style/skins/sms/sms.css"/>

    </head>
    <body>
      <h1>OWL Ontology</h1>
      <h2>Properties</h2>
      <xsl:apply-templates select="//owl:ObjectProperty"/>
      
      <hr/>
      <h2>Classes</h2>
      <xsl:apply-templates select="/rdf:RDF/owl:Class"/>
      
    </body>
  </html>
  </xsl:template>
  
  <xsl:template match="owl:ObjectProperty">
    <h3>Object Property <xsl:value-of select="@rdf:about"/></h3>
    <p><xsl:value-of select="rdfs:comment"/></p>
  </xsl:template>
  
  <xsl:template match="owl:Class">
    <h3>Class <xsl:value-of select="@rdf:about"/></h3>
    <p><xsl:value-of select="rdfs:comment"/></p>
  </xsl:template>
  
</xsl:stylesheet>
