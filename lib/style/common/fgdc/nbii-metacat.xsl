<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<!-- A National Park Service XSL template for displaying metadata in ArcInfo8.
     Created: 6/2/2003 Eric Compas 
		 See http://www.nature.nps.gov/im/units/mwr/gis/ for more information.-->

<!-- modified by M. Jones, NCEAS (UCSB) for use in the Metacat ecological data
     and metadata storage system.  Basically removed NPS-specific stylistic
     information that was hardcoded in the style -->

<!-- The "parameter" below is a variable that points to the directory where
     ArcGIS has been installed. The NPS Metadata Extension will try to
     automatically change this value. If for some reason that doesn't work, you
		 can change the value in the 'select=' to where Arc8 is installed on your
		 system. For example: "'C:\arcgis\arcexe83'" -->

<!-- The headerImage parameter was changed by Travis Stevens on
      8/24/2004 in order for online display.  The original code
      relied upon the NPS Metadata Tool and assumed that the
      image lived on in a local directory. -->

<xsl:param name="headerImage" select="'http://www.absc.usgs.gov/research/Denali/Images/headers/nps_header.jpg'"/>
<xsl:param name="altHeaderImage" select="'National Park Service, U.S. Department of the Interior'" />

<!-- TEMPLATE: main -->
<xsl:template match="/">
	<html>
		<head>
  		<title>NBII Metadata Summary</title>
  		<STYLE>
				body			{font-family:arial, verdana, helvetica, san-serif;
									color:black;
									background-color:#ffffff;
									text-align:left;
									}
  			h1 				{margin-left:0.00in;
          				position:relative;
          				top:0;
          				text-align:left;
          				font-size:16;
          				font-family:Arial,Verdana,sans-serif;
        					}
				p					{font-size:11;
									}
				dl 				{margin-top: 0px;
									margin-bottom: 0px;
									margin-left:0in;
									position:relative;
									left:0px;
									}
				dt 				{
									color:#000000;
									font-size:10pt;
									margin-bottom: 1px;
									}
				.nbii			{color:#006600;
									font-weight: bold;
									}
				.fgdc			{color:#0000cc;
									font-weight: bold;
									}
				dd 				{margin-left: 10px;
									margin-right: 0px;
									font-size:10pt;
									color:#000000;
									}
  			.titlebar	{color:#676769;
  								background-color:#f5f0dd;
  								border-right:solid 1px #d6d5d0;
  								border-left:solid 1px #d6d5d0;
  								border-bottom:solid 1px #d6d5d0;
  								padding:5px 5px 5px 5px;
  								}
  			.main			{background-color:#ffffff;
  								border-right:solid 1px #d6d5d0;
  								border-left:solid 1px #d6d5d0;
  								border-bottom:solid 1px #d6d5d0;
  								padding:5px 5px 5px 5px;
			</STYLE>
  	</head>
  	
  	
  	<body link="#666600" vlink="#999900" alink="#FF6666">
  	
  		<table width="700" cellpadding="0" cellspacing="0" border="0">
            <!--
  			<tr>
					<td colspan="2" class="header">
						<map name="header_map" id="header_map">
							<area shape="rect" coords="475,3,700,59" HREF="http://www.nps.gov" ALT="Visit ParkNet"/>
							<area shape="rect" coords="2,3,399,43" HREF="http://www.nps.gov/gis" ALT="NPS Geographic Information Systems"/>
						</map>
						<img border="0" src="{$headerImage}" alt="{$altHeaderImage}" usemap="#header_map" height="60" width="700" />
					</td>
				</tr>
                -->
  			<tr>
  				<td class="titlebar">
  					<h1>NBII metadata summary for: <xsl:value-of select="metadata/idinfo/citation/citeinfo/title"/></h1>
  					<dl>
							<xsl:if test="metadata/idinfo/citation/citeinfo/origin[. != '']">
								<dt><b>Originator: </b>
        					<xsl:value-of select="metadata/idinfo/citation/citeinfo/origin"/>
								</dt>
   						</xsl:if>
							<xsl:if test="metadata/idinfo/citation/citeinfo/pubdate[. != '']">
								<dt><b>Publication Date: </b>
       						<xsl:value-of select="metadata/idinfo/citation/citeinfo/pubdate"/>
								</dt>
							</xsl:if>
							<xsl:if test="metadata/idinfo/citation/citeinfo/onlink[. != '']">
								<dt><b>Online citation: </b>
        					<xsl:value-of select="metadata/idinfo/citation/citeinfo/onlink"/>
								</dt>
							</xsl:if>
							<xsl:if test="metadata/idinfo/descript/abstract[. != '']">
  							<dt><b>Abstract:</b></dt>
  							<dd><xsl:value-of select="metadata/idinfo/descript/abstract"/></dd>
							</xsl:if>
							<dt><b>Bounding coordinates:</b></dt>
							<dd>
							<dl>
								<dd><b>West: </b><xsl:value-of select="metadata/idinfo/spdom/bounding/westbc"/></dd>
								<dd><b>East: </b><xsl:value-of select="metadata/idinfo/spdom/bounding/eastbc"/></dd>
								<dd><b>North: </b><xsl:value-of select="metadata/idinfo/spdom/bounding/northbc"/></dd>
								<dd><b>South: </b><xsl:value-of select="metadata/idinfo/spdom/bounding/southbc"/></dd>
							</dl>
							</dd>
							<dt><b>Place Keywords: </b>
								<xsl:for-each select="metadata/idinfo/keywords/place/placekey">
				  				<xsl:value-of select="."/><xsl:if test="position()!=last()">, </xsl:if> 
								</xsl:for-each>
      				</dt>
      				<dt><b>Theme Keywords: </b>
								<xsl:for-each select="metadata/idinfo/keywords/theme/themekey">
				  				<xsl:value-of select="."/><xsl:if test="position()!=last()">, </xsl:if> 
								</xsl:for-each>
      				</dt>
      				<xsl:if test="metadata/spref/horizsys/cordsysn/projcsn[. != '']">
	        			<dt><b>Projection: </b>
  	        			<xsl:value-of select="metadata/spref/horizsys/cordsysn/projcsn"/>
								</dt>
     					</xsl:if>
      				<xsl:if test="metadata/spref/horizsys/cordsysn/geogcsn[. != '']">
        				<dt><b>Geographic: </b>
          				<xsl:value-of select="metadata/spref/horizsys/cordsysn/geogcsn"/>
								</dt>
      				</xsl:if>
							<dt><b>Metadata date: </b>
        				<xsl:value-of select="metadata/metainfo/metd"/>
							</dt>
  					</dl>
  				</td>
  			</tr>
  			<tr>
  				<td class="main">
						<p>The following section contains all the NBII metadata elements that exist within this metadata file. Colors indicate <span class="nbii">NBII elements</span> and <span class="fgdc">FGDC elements</span>. Please use the XML stylesheet or the <b>Parse with MP</b> tool to view with all FGDC elements.</p> 
						<dl>
  						<xsl:apply-templates />
						</dl>
					</td>
				</tr>
                <!--
				<tr bgcolor="#333333"> 
					<td width="554"><font size="1" color="#FFFFFF" face="Arial, Helvetica, sans-serif">Draft XSL Stylesheet 11/19/03 Eric Compas</font></td>
				</tr>
                -->
			</table>
		</body>
	</html>
</xsl:template>

<!-- TEMPLATE: idinfo -->
<xsl:template match="idinfo">

	<!-- start NBII element: description of geographic extent -->
	<xsl:if test="spdom/descgeog[. != '']">
		<dt class="nbii">Description of geographic extent:</dt>
  	<dd><xsl:value-of select="spdom/descgeog"/></dd>
		<BR/><br/>
	</xsl:if>
	<!-- end NBII element: description of geographic extent -->	


	<!-- start NBII element: bounding altitude -->
	<xsl:for-each select="spdom/boundalt">
		<dt class="nbii">Bounding altitude:</dt>
		<dd>
		<dl>
			<dt><span class="nbii">Altitude minimum: </span>
					<xsl:value-of select="altmin" /></dt>
			<dt><span class="nbii">Altitude maximum: </span>
					<xsl:value-of select="altmax" /></dt>
								
			<!-- TODO: altitude distance units (can't find element name) -->
		</dl>
		</dd>
	</xsl:for-each>
	<!-- end NBII element: bounding altitude -->	
	
	<!-- start NBII element: TAXONOMY -->
	<xsl:for-each select="taxonomy">
		<dt class="nbii">Taxonomy:</dt>
		<dd>
		<dl>
				
    	<!-- taxonomic keywords -->
      <xsl:for-each select="keywtax">
      	<dt class="nbii">Keywords/taxon:</dt>
				<dd>
      	<dl>
        	<dt>
						<xsl:for-each select="taxonkey[text()]">
          		<xsl:if test="position()=1"><span class="nbii">Taxonomic keywords: </span></xsl:if>
            	<xsl:value-of select="."/><xsl:if test="position()!=last()">, </xsl:if>
          	</xsl:for-each>
					</dt>
     			<dt><span class="nbii">Taxonomic keyword thesaurus: </span><xsl:value-of select="taxonkt"/></dt>
      	</dl>
				</dd>
				<BR/>
      </xsl:for-each>
      			
					<!-- taxonomic system -->
					<xsl:for-each select="taxonsys">
						<dt class="nbii">Taxonomic system:</dt>
						<dd>
						<dl>
							<xsl:for-each select="classsys">
								<dt class="nbii">Classification system or authority:</dt>
								<dd>
								<dl>
									<xsl:for-each select="classcit">
										<dt class="nbii">Classification system citation:</dt>
										<dd>
										<dl>
											<xsl:apply-templates select="citeinfo"/>
										</dl>
										</dd>
										<xsl:for-each select="classmod">
											<dt class="nbii">Classification system modification:</dt>
											<dd>
												<xsl:value-of select="."/>
											</dd>
										</xsl:for-each>
									</xsl:for-each>
								</dl>
								</dd>
								<BR/>
							</xsl:for-each>
							<xsl:for-each select="idref">
								<dt class="nbii">Identification reference:</dt>
								<dd>
								<dl>
									<xsl:apply-templates select="citeinfo"/>
								</dl>
								</dd>
								<BR/>
							</xsl:for-each>
							<xsl:for-each select="ider">
								<dt class="nbii">Identifier:</dt>
								<dd>
								<dl>
									<xsl:apply-templates select="citeinfo"/>
								</dl>
								</dd>
								<BR/>
							</xsl:for-each>
							<xsl:for-each select="taxonpro">
								<dt class="nbii">Taxonomic procedures:</dt>
								<dd><xsl:value-of select="."/></dd>
							</xsl:for-each>
							<xsl:for-each select="taxoncom">
								<dt class="nbii">Taxonomic Completeness:</dt>
								<dd><xsl:value-of select="."/></dd>
							</xsl:for-each>
							<xsl:for-each select="vouchers">
								<dt class="nbii">Vouchers:</dt>
								<dd>
								<dl>
									<xsl:for-each select="specimen">
										<dt class="nbii">Specimen:</dt>
										<dd><xsl:value-of select="."/></dd>
									</xsl:for-each>
									<xsl:for-each select="reposit">
										<dt class="nbii">Repository:</dt>
										<dd>
										<dl>
											<xsl:apply-templates select="citeinfo"/>
										</dl>
										</dd>
									</xsl:for-each>
								</dl>
								</dd>
							</xsl:for-each>
						</dl>
						</dd>	
						<BR/>
					</xsl:for-each>	
     			
					<!-- general taxonomic coverage -->
					<xsl:for-each select="taxoncom">
						<dt class="nbii">General taxonomic coverage:</dt>
						<dd><xsl:value-of select="."/></dd>
					</xsl:for-each>

     			<!-- taxonomic classification -->
	        <dt class="nbii">Taxonomic classification: </dt>
					<dd>
					<dl>
						<xsl:apply-templates select="taxoncl"/>
					</dl>
					</dd>

     		</dl>			
				</dd>
				<BR/>
			</xsl:for-each>
			<!-- end NBII element: TAXONOMY -->	
			
					<!-- begin NBII element: Analytical Tool -->
		<xsl:for-each select="tool">
  		<dt class="nbii">Analytical tool:</dt>
  		<dd>
  		<dl>
				<xsl:for-each select="tooldesc">
					<dt class="nbii">Analytical tool description:</dt>
					<dd><xsl:value-of select="." /></dd>
				</xsl:for-each>
				<xsl:for-each select="toolacc">
					<dt class="nbii">Tool access information:</dt>
					<dd>
					<dl>
						<xsl:for-each select="onlink">
							<dt>Online linkage:</dt>
							<dd><xsl:value-of select="." /></dd>
						</xsl:for-each>
						<xsl:for-each select="toolinst">
							<dt class="nbii">Tool access instructions:</dt>
							<dd><xsl:value-of select="." /></dd>
						</xsl:for-each>
						<xsl:for-each select="toolcomp">
							<dt class="nbii">Tool computer and operating system:</dt>
							<dd><xsl:value-of select="." /></dd>
						</xsl:for-each>
					</dl>
					</dd>
				</xsl:for-each>
				<xsl:for-each select="toolcont">
					<dt class="nbii">Tool contact:</dt>
					<dd>
					<dl>
						<xsl:apply-templates select="citeinfo"/>
					</dl>
					</dd>
				</xsl:for-each>
				<xsl:for-each select="toolcite">
					<dt class="nbii">Tool citation:</dt>
					<dd>
					<dl>
						<xsl:apply-templates select="citeinfo"/>
					</dl>
					</dd>
				</xsl:for-each>  			  			
  		</dl>
  		</dd>
		</xsl:for-each>
		<!-- end NBII element: Analytical Tool -->
</xsl:template>

<!-- TEMPLATE: taxonomic classification -->
<xsl:template match="taxoncl">
	<dt>
		<span class="nbii">Taxon rank name: </span>
		<xsl:value-of select="taxonrn"/>
		<span class="nbii"> Value: </span>
		<xsl:value-of select="taxonrv"/>
	</dt>
	<dt>
	<xsl:for-each select="common">
		<xsl:if test="position()=1"><span class="nbii">Applicable common names: </span></xsl:if>
    <xsl:value-of select="."/><xsl:if test="position()!=last()">, </xsl:if>
		<xsl:if test="position()=last()"><BR/><BR/></xsl:if>
  </xsl:for-each></dt>
	<dd>
	<dl>
		<!-- recurse -->
		<xsl:apply-templates />
	</dl>
	</dd>
</xsl:template>


<!-- TEMPLATE: data quality -->
<xsl:template match="dataqual">
	<xsl:for-each select="lineage">
		<!-- begin NBII element: methodology -->
		<xsl:for-each select="method">
			<dt class="nbii">Methodology:</dt>
			<dd>
			<dl>
				<xsl:for-each select="methtype">
					<dt class="nbii">Methodology type:</dt>
					<dd><xsl:value-of select="." /></dd>
				</xsl:for-each>
				<xsl:for-each select="methodid">
					<dt class="nbii">Methodology identifier:</dt>
					<dd>
					<dl>
						<dt>
						<xsl:for-each select="methkey[text()]">
			<xsl:if test="position()=1"><span class="nbii">Methodology keywords: </span></xsl:if>
		    <xsl:value-of select="."/><xsl:if test="position()!=last()">, </xsl:if>
		  </xsl:for-each>
						</dt>
						<xsl:for-each select="methkt">
							<dt><span class="nbii">Methodology keyword thesaurus: </span>
									<xsl:value-of select="." /></dt>
						</xsl:for-each>
					</dl>
					</dd>
				</xsl:for-each>
				<xsl:for-each select="methdesc">
					<dt class="nbii">Methodology description:</dt>
					<dd><xsl:value-of select="." /></dd>
				</xsl:for-each>
				<xsl:for-each select="methcite">
					<dt class="nbii">Methodology citation:</dt>
					<dd>
					<dl>
						<xsl:apply-templates select="citeinfo" />
					</dl>
					</dd>
				</xsl:for-each>
			</dl>
			</dd>
			<BR/>					
			</xsl:for-each>
		<!-- end NBII element: methodology -->
	</xsl:for-each>
</xsl:template>

<!-- TEMPLATE: distribution information -->
<xsl:template match="distinfo">
	<!-- start NBII element: ASCII file structure -->
	<xsl:for-each select="asciistr">
		<dt class="nbii">ASCII file structure:</dt>
		<dd>
		<dl>
			<xsl:for-each select="recdel">
				<dt><span class="nbii">Record delimiter: </span>
						<xsl:value-of select="." /></dt>
			</xsl:for-each>
			<xsl:for-each select="numheadl">
				<dt><span class="nbii">Number header lines: </span>
						<xsl:value-of select="." /></dt>
			</xsl:for-each>
			<xsl:for-each select="deschead">
				<dt class="nbii">Description of header content:</dt>
				<dd><xsl:value-of select="." /></dd>
			</xsl:for-each>
			<xsl:for-each select="orienta">
				<dt><span class="nbii">Orientation: </span>
						<xsl:value-of select="." /></dt>
			</xsl:for-each>
			<xsl:for-each select="casesens">
				<dt><span class="nbii">Case sensetive: </span>
						<xsl:value-of select="." /></dt>
			</xsl:for-each>
			<xsl:for-each select="authent">
				<dt class="nbii">Authentication:</dt>
				<dd><xsl:value-of select="." /></dd>
			</xsl:for-each>
			<xsl:for-each select="quotech">
				<dt><span class="nbii">Quote character: </span>
						<xsl:value-of select="." /></dt>
			</xsl:for-each>
			<xsl:for-each select="datafiel">
				<dt class="nbii">Data field:</dt>
				<dd>
				<dl>
					<xsl:for-each select="dfieldnm">
						<dt><span class="nbii">Data field name: </span>
							<xsl:value-of select="." /></dt>
					</xsl:for-each>
					<xsl:for-each select="missingv">
						<dt><span class="nbii">Missing value code: </span>
							<xsl:value-of select="." /></dt>
					</xsl:for-each>
					<xsl:for-each select="dfwidthd">
						<dt><span class="nbii">Data field width delimiter: </span>
							<xsl:value-of select="." /></dt>
					</xsl:for-each>
					<xsl:for-each select="dfwidth">
						<dt><span class="nbii">Data field width:</span>
							<xsl:value-of select="." /></dt>
					</xsl:for-each>
				</dl>
				</dd>
			</xsl:for-each>
		</dl>
		</dd>
	</xsl:for-each>									
	<!-- end NBII element: ASCII file structure -->
</xsl:template>									

<!-- Time Period Info -->
<xsl:template match="timeinfo">
  <dt class="fgdc">Time period information:</dt>
  <dd>
  <dl>
    <xsl:apply-templates select="sngdate"/>
    <xsl:apply-templates select="mdattim"/>
    <xsl:apply-templates select="rngdates"/>
    
    <!-- begin NBII section -->
    <xsl:apply-templates select="beggeol"/>
    <xsl:apply-templates select="endgeog"/>
    <!-- end NBII section -->
    
  </dl>
  </dd>
  <BR/>
</xsl:template>
<!-- Single Date/Time -->
<xsl:template match="sngdate">
  <dt><B>Single date/time:</B></dt>
  <dd>
  <dl>
    <xsl:for-each select="caldate">
      <dt class="nbii">Calendar date:
        <xsl:choose>
          <xsl:when test="text()[. = 'REQUIRED: The year (and optionally month, or month and day) for which the data set corresponds to the ground.']">
            <FONT color="#999999"><xsl:value-of select="."/></FONT>
          </xsl:when>
          <xsl:when test="text()[. = 'The year (and optionally month, or month and day) for which the data set corresponds to the ground.  REQUIRED.']">
            <FONT color="#999999"><xsl:value-of select="." /></FONT>
          </xsl:when>
          <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
        </xsl:choose>
      </dt>
    </xsl:for-each>
    <xsl:for-each select="time">
      <dt><B>Time of day:</B> <xsl:value-of select="."/></dt>
    </xsl:for-each>
    
    <!-- begin NBII element: geologic age -->
    <xsl:apply-templates select="geolage"/>
    <!-- end NBII element: geologic age -->
    
  </dl>
  </dd>
</xsl:template>
<!-- Multiple Date/Time -->
<xsl:template match="mdattim">
  <dt class="fgdc"><B>Multiple dates/times:</B></dt>
  <dd>
  <dl>
    <xsl:apply-templates select="sngdate"/>
  </dl>
  </dd>
</xsl:template>
<!-- Range of Dates/Times -->
<xsl:template match="rngdates">
  <dt class="fgdc">Range of dates/times:</dt>
  <dd>
  <dl>
    <xsl:for-each select="begdate">
      <dt class="fgdc">Beginning date: <xsl:value-of select="."/></dt>
    </xsl:for-each>
    <xsl:for-each select="begtime">
      <dt class="fgdc">Beginning time: <xsl:value-of select="."/></dt>
    </xsl:for-each>
    <xsl:for-each select="enddate">
      <dt class="fgdc">Ending date: <xsl:value-of select="."/></dt>
    </xsl:for-each>
    <xsl:for-each select="endtime">
      <dt class="fgdc">Ending time: <xsl:value-of select="." /></dt>
    </xsl:for-each>
  </dl>
  </dd>
</xsl:template>
<!-- start NBII element: beginning geologic age -->
<xsl:template match="beggeol">
	<dt class="nbii">Beginning geologic age:</dt>
	<dd>
	<dl>
		<xsl:apply-templates select="geolage"/>
	</dl>
	</dd>
</xsl:template>
<!-- end NBII element: beginning geologic age -->
<!-- start NBII element: ending geologic age -->
<xsl:template match="endgeol">
	<dt class="nbii">Ending geologic age:</dt>
	<dd>
	<dl>
		<xsl:apply-templates select="geolage"/>
	</dl>
	</dd>
</xsl:template>
<!-- end NBII element: ending geologic age -->
<!-- start NBII element: geologic age -->
<xsl:template match="geolage">
	<dt class="nbii">Geologic age:</dt>
	<dd>
	<dl>
		<xsl:for-each select="geolscal">
			<dt class="nbii">Geologic time scale:</dt>
			<dd><xsl:value-of select="."/></dd>
		</xsl:for-each>
		<xsl:for-each select="geolest">
			<dt class="nbii">Geologic age estimate:</dt>
			<dd><xsl:value-of select="."/></dd>
		</xsl:for-each>
		<xsl:for-each select="geolun">
			<dt class="nbii">Geologic age uncertainty:</dt>
			<dd><xsl:value-of select="."/></dd>
		</xsl:for-each>
		<xsl:for-each select="geolexpl">
			<dt class="nbii">Geologic age explanation:</dt>
			<dd><xsl:value-of select="."/></dd>
		</xsl:for-each>
		<xsl:for-each select="geolcit">
			<dt class="nbii">Geologic citation:</dt>
			<dd>
			<dl>
				<xsl:apply-templates select="citeinfo"/>
			</dl>
			</dd>
		</xsl:for-each>
	</dl>
	</dd>
</xsl:template>
<!-- end NBII element: geologic age -->

<!-- start citation element -->
<xsl:template match="citeinfo">
  <dt class="fgdc">Citation information:</dt>
  <dd>
  <dl>
  	<xsl:if test="origin[. != '']">
      <dt class="fgdc">Originators:</dt>
      <dd>
      <xsl:for-each select="origin">
        <xsl:value-of select="."/><xsl:if test="position()!=last()">, </xsl:if>
      </xsl:for-each>
      </dd>
    </xsl:if>
 		<xsl:if test="origin[. != '']"><BR/><BR/></xsl:if>
		
		<xsl:for-each select="title">
		      <DT><FONT color="#0000AA"><B>Title:</B></FONT></DT>
		      <DD><xsl:value-of select="."/></DD>
		    </xsl:for-each>
		    <xsl:for-each select="pubdate">
		      <DT>
		        <FONT color="#0000AA"><B>Publication date:</B></FONT> 
		        <xsl:value-of select="."/>
		      </DT>
		    </xsl:for-each>
		    <xsl:for-each select="pubtime">
		      <DT><FONT color="#0000AA"><B>Publication time:</B></FONT> <xsl:value-of select="."/></DT>
		    </xsl:for-each>
		    <xsl:for-each select="edition">
		      <DT><FONT color="#0000AA"><B>Edition:</B></FONT> <xsl:value-of select="."/></DT>
		    </xsl:for-each>
		    <xsl:for-each select="geoform">
		      <DT><FONT color="#0000AA"><B>Geospatial data presentation form:</B></FONT> <xsl:value-of select="."/></DT>
		    </xsl:for-each>
		    <xsl:for-each select="serinfo">
		      <DT><FONT color="#0000AA"><B>Series information:</B></FONT></DT>
		      <DD>
		      <DL>
		        <xsl:for-each select="sername">
		          <DT><FONT color="#0000AA"><B>Series name:</B></FONT> <xsl:value-of select="."/></DT>
		        </xsl:for-each>
		        <xsl:for-each select="issue">
		          <DT><FONT color="#0000AA"><B>Issue identification:</B></FONT> <xsl:value-of select="."/></DT>
		        </xsl:for-each>
		      </DL>
		      </DD>
		      <BR/>
		    </xsl:for-each>
		
		    <xsl:for-each select="pubinfo">
		      <DT><FONT color="#0000AA"><B>Publication information:</B></FONT></DT>
		      <DD>
		      <DL>
		        <xsl:for-each select="pubplace">
		          <DT><FONT color="#0000AA"><B>Publication place:</B></FONT> <xsl:value-of select="."/></DT>
		        </xsl:for-each>
		        <xsl:for-each select="publish">
		          <DT><FONT color="#0000AA"><B>Publisher:</B></FONT> <xsl:value-of select="."/></DT>
		        </xsl:for-each>
		      </DL>
		      </DD>
		      <BR/>
		    </xsl:for-each>
		
		    <xsl:for-each select="othercit">
		      <DT><FONT color="#0000AA"><B>Other citation details:</B></FONT></DT>
		      <DD><xsl:value-of select="."/></DD>
		      <BR/><BR/>
		    </xsl:for-each>
		
		    <xsl:for-each select="onlink">
		      <DT><FONT color="#0000AA"><B>Online linkage:</B></FONT> <A TARGET="viewer">
		        <xsl:attribute name="HREF"><xsl:value-of select="."/></xsl:attribute>
		        <xsl:value-of select="."/></A>
		      </DT>
		    </xsl:for-each>
		    <xsl:if test="onlink[. != '']"><BR/><BR/></xsl:if>
		
		    <xsl:for-each select="lworkcit">
		      <DT><FONT color="#0000AA"><B>Larger work citation:</B></FONT></DT>
		      <DD>
		      <DL>
		        <xsl:apply-templates select="citeinfo"/>
		      </DL>
		      </DD>
    </xsl:for-each>
 		
 		
 		
  </dl>
  </dd>
</xsl:template>

<!-- TEMPLATE: default (don't display unprocessed elements)-->
	<xsl:template match="text()" />
</xsl:stylesheet>

