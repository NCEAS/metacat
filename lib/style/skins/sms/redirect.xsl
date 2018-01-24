<?xml version="1.0"?> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="/success">
    <html>
      <script type="text/javascript">
        function redirect()
        {
          var url = "/sms/?docid=" + "<xsl:value-of select="./docid"/>" + "&amp;status=success";
          window.location = url;
        }
      </script>
      <body onload="javascript:redirect()">
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="/error">
    <html>
      <script type="text/javascript">
        function redirect()
        {
          var url = "/sms/?status=fail";
          window.location = url;
        } 
      </script>
      <body onload="javascript:redirect()">
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>
