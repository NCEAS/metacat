<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:param name="publicRead"/>
    <xsl:param name="message"/>
    <xsl:param name="sessionid"></xsl:param>
    <xsl:param name="enableFGDCediting"/>
        
    <xsl:template name="metadata">
        <article id="Metadata" class="container">
				    <xsl:apply-templates select="metadata/idinfo"/>
			        <br/>
			        <xsl:apply-templates
			            select="metadata/distinfo/resdesc[starts-with(text(),$docid)]/ancestor::distinfo"/>
	    </article>
        
    </xsl:template>
    
    <xsl:template match="idinfo">
                
        <!-- don't want to conflict with the eml-physical.xsl template -->
        <xsl:for-each select="citation/citeinfo">
        	<xsl:call-template name="fgdcBlockCitation"/>
        </xsl:for-each>
            
        <div class="form-horizontal">
        	<xsl:for-each select="citation">
	        	<xsl:call-template name="fgdcCitation"/>
	        </xsl:for-each>
            <xsl:apply-templates select="descript"/>
            <xsl:apply-templates select="spdom"/>
            <xsl:apply-templates select="keywords"/>
            
        </div>
    </xsl:template>
    
    <!-- for citation information -->
    <xsl:template name="fgdcBlockCitation">
    
    	<cite class="citation">
    			<span class="author">
    				<xsl:for-each select="origin">
		        		<xsl:if test="position() &gt; 1">
		        			<xsl:if test="last() &gt; 2">, </xsl:if>
		        			<xsl:if test="position() = last()"> and</xsl:if>
		        			<xsl:text> </xsl:text>
		        		</xsl:if>
		        		<xsl:value-of select="." />
		        		<xsl:if test="position() = last()">.</xsl:if>
		        		<xsl:text> </xsl:text>    		
		        	</xsl:for-each>
		      </span>   	
    			<span class="pubdate">
		        	<xsl:value-of select="substring(string(pubdate),1,4)"/>
		        	<xsl:if test="substring(string(pubdate),1,4) != ''">
		        		<xsl:text>. </xsl:text>    				     		
		        	</xsl:if>
		        </span>
		        <span class="title">		        	
		        	<!-- title -->
					<strong>
						<xsl:for-each select="./title">
			        		<xsl:value-of select="." />
			        		<xsl:text> </xsl:text>    				     		
				     	</xsl:for-each>				
					</strong>
				</span>
				<span class="id">					
					<!-- show link? -->
						ID: <xsl:value-of select="$pid"/>
				</span>			        		
    			<div>
    			
    				<form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx" enctype="application/octet-stream">
		               <input name="docid" type="hidden" value="{$docid}"/>
		               <input name="qformat" type="hidden" value="{$qformat}"/>
		               <input name="metadataDocId" type="hidden" value="{$docid}"/>
		               <input name="action" value="Download" type="hidden"/>
		               <button class="btn" type="submit" id="downloadPackage">
		               		<i class="icon-arrow-down"></i> Download Package
		               </button>
		           </form>
           			<!--  
   					<a class="btn">
   						<xsl:attribute name="href">
							<xsl:value-of select="$packageURI"/><xsl:value-of select="$pid"/>
						</xsl:attribute>
   						<i class="icon-arrow-down"></i> Download Package
   					</a>
   					-->
    			</div>	
 				
		</cite>

   </xsl:template>
    
    
    <xsl:template name="fgdcCitation">
        <xsl:apply-templates select="citeinfo"/>
    </xsl:template>
    
    <xsl:template match="citeinfo">
        <h4>Citation Information</h4>
        
        <xsl:apply-templates select="title"/>
        <xsl:call-template name="docid"/>
        <xsl:for-each select="origin">
            <xsl:apply-templates select="."/>
	    </xsl:for-each> 
        <xsl:apply-templates select="pubdate"/>
        <xsl:apply-templates select="edition"/>
        <xsl:apply-templates select="geoform"/>
        <xsl:apply-templates select="pubinfo"/>
        <xsl:apply-templates select="othercit"/>
        <xsl:apply-templates select="onlink"/>
    </xsl:template>
    
    <xsl:template match="descript">
        <h4>Description</h4>
        <xsl:apply-templates select="abstract"/>
        <xsl:apply-templates select="purpose"/>
    </xsl:template>
    
    <xsl:template match="spdom">
        <xsl:apply-templates select="bounding"/>
    </xsl:template>
    
    <xsl:template match="keywords">
	    <xsl:apply-templates select="theme"/>
	    <xsl:apply-templates select="place"/>
    </xsl:template>
    
    <xsl:template match="theme">
        <div class="control-group">
			<label class="control-label">Theme Keywords</label>
        	<div class="controls controls-well">
	            <xsl:apply-templates select="themekey[position()!=last()]">
	                <xsl:with-param name="lastPlace">f</xsl:with-param>
	            </xsl:apply-templates>
	            <xsl:apply-templates select="themekey[position()=last()]">
	                <xsl:with-param name="lastPlace">t</xsl:with-param>
	            </xsl:apply-templates>
        	</div>
        </div>	
    </xsl:template>
    
    <xsl:template match="place">
        <div class="control-group">
			<label class="control-label">Place Keywords</label>
        	<div class="controls controls-well">
	            <xsl:apply-templates select="placekey[position()!=last()]">
	                <xsl:with-param name="lastPlace">f</xsl:with-param>
	            </xsl:apply-templates>
	            <xsl:apply-templates select="placekey[position()=last()]">
	                <xsl:with-param name="lastPlace">t</xsl:with-param>
	            </xsl:apply-templates>
        	</div>
        </div>	
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
        <div class="control-group boundingCoordinates geographicCoverage" data-content="boundingCoordinates">
			<label class="control-label">Bounding Coordinates</label>
        	<div class="controls controls-well">
        	
        	  	<xsl:variable name="west"><xsl:value-of select="westbc"/></xsl:variable>
        	  	<xsl:variable name="east"><xsl:value-of select="eastbc"/></xsl:variable>
        	  	<xsl:variable name="north"><xsl:value-of select="northbc"/></xsl:variable>
        	  	<xsl:variable name="south"><xsl:value-of select="southbc"/></xsl:variable>

	            <div class="control-group westBoundingCoordinate" data-content="westBoundingCoordinate" data-value="{$west}">
					<label class="control-label">West</label>
	                <div class="controls">
                       	<xsl:value-of select="$west"/>
	                </div>
				</div> 
	            <div class="control-group eastBoundingCoordinate" data-content="eastBoundingCoordinate" data-value="{$east}">
					<label class="control-label">East</label>
                    <div class="controls">
                        <xsl:value-of select="$east"/>
                    </div>
				</div>
				<div class="control-group northBoundingCoordinate" data-content="northBoundingCoordinate" data-value="{$north}">
					<label class="control-label">North</label>
                    <div class="controls">
                        <xsl:value-of select="$north"/>
                    </div>
               	</div>
	            <div class="control-group southBoundingCoordinate" data-content="southBoundingCoordinate" data-value="{$south}">
					<label class="control-label">South</label>
                    <div class="controls">
                        <xsl:value-of select="$south"/>
                    </div>
	            </div>
	        </div> 
        </div>
    </xsl:template>
    
    <xsl:template match="abstract">
        <div class="control-group">
			<label class="control-label">Abstract</label>
	        <div class="controls controls-well">
	            <xsl:value-of select="."/>
	        </div>
	   	</div> 
    </xsl:template>
    
    <xsl:template match="purpose">
        <div class="control-group">
			<label class="control-label">Purpose</label>
	        <div class="controls controls-well">
	            <xsl:value-of select="."/>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="title">
        <div class="control-group">
			<label class="control-label">Title</label>
	        <div class="controls controls-well">
	            <xsl:value-of select="."/>
	        </div>
        </div>
    </xsl:template>
    
     <xsl:template name="docid">
        <div class="control-group">
			<label class="control-label">Identifier</label>
	        <div class="controls controls-well">
	            <xsl:value-of select="$pid"/>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="origin">
        <div class="control-group">
			<label class="control-label">Originator</label>
	        <div class="controls controls-well">
	            <xsl:value-of select="."/>
	        </div>
       	</div>
    </xsl:template>
    
    <xsl:template match="pubdate">
        <div class="control-group">
			<label class="control-label">Publication Date</label>
	        <div class="controls controls-well">
	            <!-- <xsl:call-template name="long_date">-->
	            <xsl:call-template name="date_as_is">
	                <xsl:with-param name="date">
	                    <xsl:value-of select="."/>
	                </xsl:with-param>
	            </xsl:call-template>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="onlink">
        <div class="control-group">
			<label class="control-label">Online Access</label>
	        <div class="controls controls-well">
	        	<a>
	        	<xsl:attribute name="href"><xsl:value-of select="."/></xsl:attribute>
	        	<xsl:value-of select="."/></a>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="edition">
        <div class="control-group">
			<label class="control-label">Edition</label>
	        <div class="controls controls-well">
	            <xsl:value-of select="."/>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="geoform">
        <div class="control-group">
			<label class="control-label">Geospatial Data Presentation Form</label>
	        <div class="controls controls-well">
	            <xsl:value-of select="."/>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="othercit">
        <div class="control-group">
			<label class="control-label">Other Citation Details</label>
	        <div class="controls controls-well">
	            <xsl:value-of select="."/>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="pubinfo">
        <div class="control-group">
			<label class="control-label">Publication Information</label>
	        <div class="controls controls-well">
	            <xsl:apply-templates select="pubplace"/>
	            <xsl:apply-templates select="publish"/>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="pubplace">
        <div class="control-group">
			<label class="control-label">Publication Place</label>
	        <div class="controls">
	            <xsl:value-of select="."/>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="publish">
        <div class="control-group">
			<label class="control-label">Publisher</label>
	        <div class="controls">
	            <xsl:value-of select="."/>
	        </div>
        </div>
    </xsl:template>
    
    <xsl:template match="distinfo">
    	<h4>Data Package Information</h4>
        <div class="form-horizontal">
            <xsl:apply-templates select="resdesc"/>
            <xsl:apply-templates select="stdorder/digform/digtopt/onlinopt/computer/networka"/>
        </div>
    </xsl:template>
    
    <xsl:template match="resdesc">
        <div class="control-group">
			<label class="control-label">Metadata</label>
        	<div class="controls controls-well">
        		
				<!-- download -->
				<form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx" class="form-inline">
					<xsl:value-of select="../custom"/> (<xsl:value-of select="."/>)
					<input name="action" value="Download" type="submit"/>
					<input name="docid" type="hidden" value="{.}"/>
					<input name="qformat" type="hidden" value="{$qformat}"/>
					<input name="metadataDocId" type="hidden" value="{.}"/>
				</form>

				<xsl:if test="$enableFGDCediting = 'true' and not($sessionid = '')">
	
					<!-- update -->
		       		<form method="post" action="{$contextURL}/style/common/ClientViewHelper.jspx"
	                     enctype="multipart/form-data" class="form-inline">
	                   <input name="action" value="Update Metadata" type="hidden"/>
	                   <input name="docid" type="hidden" value="{.}"/>
	                   <input name="qformat" type="hidden" value="{$qformat}"/>
	                   <input name="metadataDocId" type="hidden" value="{.}"/>
	                   <input name="updateFile" type="file"/>
	                   <input name="submit" value="Update" type="submit"/>
					</form>
				
					<!-- set access -->
					<form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx" class="form-inline">
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
		                   <input name="action" value="Set Access" type="submit"/>		                   
					</form>
				
					<!-- delete -->
		           	<form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx"
		                     enctype="text/html" class="form-inline">
		                   <input name="action" value="Delete" type="submit"/>
		                   <input name="docid" type="hidden" value="{.}"/>
		                   <input name="qformat" type="hidden" value="{$qformat}"/>
		                   <input name="metadataDocId" type="hidden" value="{.}"/>
					</form>
				</xsl:if>
			</div>
		</div>
   </xsl:template>
   
   <xsl:template match="networka">
   		<xsl:apply-templates select="networkr"/>
   </xsl:template>
   
   <xsl:template match="networkr">
	<xsl:if test="not(./ancestor::digform/digtinfo/formcont = '')">
       <div class="control-group">
			<label class="control-label">Data</label>
       		<div class="controls controls-well">
		        <!-- download -->
	           <form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx" enctype="application/octet-stream" class="form-inline">
		        	<xsl:value-of select="./ancestor::digform/digtinfo/formcont"/> (<xsl:value-of select="."/>)
					<input name="action" value="Download" type="submit"/>
					<input name="docid" type="hidden" value="{.}"/>
					<input name="qformat" type="hidden" value="{$qformat}"/>
					<input name="metadataDocId" type="hidden" value="{$docid}"/>
	           </form>
		       	<!-- update -->
				<xsl:if test="$enableFGDCediting = 'true' and not($sessionid = '')">
		               <form method="post" action="{$contextURL}/style/common/ClientViewHelper.jspx"
		                     enctype="multipart/form-data" class="form-inline">
		                   <input name="action" value="Update Data" type="hidden"/>
		                   <input name="docid" type="hidden" value="{.}"/>
		                   <input name="qformat" type="hidden" value="{$qformat}"/>
		                   <input name="metadataDocId" type="hidden" value="{$docid}"/>
		                   <input name="updateFile" type="file"/>
		                   <input name="submit" value="Update" type="submit"/>                    
		               </form>
		               <!-- delete -->
						<form method="get" action="{$contextURL}/style/common/ClientViewHelper.jspx"
		                     enctype="text/html" class="form-inline">
		                   <input name="action" value="Delete" type="submit"/>
		                   <input name="docid" type="hidden" value="{.}"/>
		                   <input name="qformat" type="hidden" value="{$qformat}"/>
		                   <input name="metadataDocId" type="hidden" value="{$docid}"/>
		               </form>	
				</xsl:if>                        
			</div>
		</div>	
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
