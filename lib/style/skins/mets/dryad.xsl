<xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:datetime="http://exslt.org/dates-and-times" xmlns:encoder="xalan://java.net.URLEncoder"
	exclude-result-prefixes="xalan strings encoder datetime" version="1.0"
	xmlns:strings="http://exslt.org/strings">

	<xsl:import href="DryadUtils.xsl"/>
	
	<xsl:import href="../../common/resultset-table.xsl"/>
    <xsl:import href="../../common/eml-2/eml.xsl"/>
    <xsl:import href="../../common/fgdc/fgdc-root.xsl"/>
		
	<xsl:output method="html" />
	<xsl:param name="sessid" />
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	<xsl:param name="cgi-prefix"/>

	<xsl:variable name="meta" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata"/>
	<xsl:variable name="localize" select="$meta[@element='dryad'][@qualifier='localize'][.='true']"/>

	<xsl:template match="/">
		<xsl:variable name="datafiles" select=".//dim:field[@element='relation'][@qualifier='haspart']"/>

		<!-- my_doi and my_uri go together; there is a my_uri if no my_doi -->
		<xsl:variable name="my_doi"
			select=".//dim:field[@element='identifier'][not(@qualifier)][starts-with(., 'doi:')]"/>
		<xsl:variable name="my_uri"
			select=".//dim:field[@element='identifier'][@qualifier='uri'][not(starts-with(., 'doi'))]"/>

		<!-- Obtain an identifier if the item is harvested from KNB.
			   But we have to munge the URL to link to LTER instead of the raw XML. -->
		<xsl:variable name="knb_url_raw"
			select=".//dim:field[@element='identifier'][starts-with(.,'http://metacat')]"/>
		<xsl:variable name="knb_url">
			<xsl:if test="$knb_url_raw!=''">
				<xsl:value-of select="substring($knb_url_raw,0,string-length($knb_url_raw)-2)"/>lter
			</xsl:if>
		</xsl:variable>
		
		<!-- Obtain an identifier if the item is harvested from TreeBASE.
		But we have to munge the URL to link to TreeBASE instead of raw XML. -->
		<xsl:variable name="treebase_url_raw"
			select=".//dim:field[@element='identifier'][contains(., 'purl.org/phylo/treebase/')]"/>
		<xsl:variable name="treebase_url">
			<xsl:if test="$treebase_url_raw != ''">
				<xsl:choose>
					<xsl:when test="starts-with(., 'http:')">
						<xsl:value-of select="concat($treebase_url_raw, '?format=html')"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat('http://', $treebase_url_raw, '?format=html')"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:variable>

		<!-- CITATION FOR DATA FILE -->
		<xsl:if
			test="$meta[@element='xhtml_head_item'][contains(., 'DCTERMS.isPartOf')]
				and $meta[@element='request'][@qualifier='queryString'][not(contains(., 'show=full'))]
				and $meta[@element='authors'][@qualifier='package']">

			<xsl:variable name="article_doi"
				select="$meta[@element='identifier'][@qualifier='article'][. != '']"/>

			<div class="citation-view">
				<p class="ds-paragraph">When using this data, please cite the original article:</p>
				<blockquote>
					<xsl:variable name="citation" select="$meta[@element='citation'][@qualifier='article']"/>
					<xsl:choose>
						<xsl:when test="$citation != ''">
							<xsl:choose>
								<xsl:when test="$article_doi and not(contains($citation, $article_doi))">
									<xsl:copy-of select="$citation"/>
									<a>
										<xsl:attribute name="href">
											<xsl:choose>
												<xsl:when test="starts-with($article_doi, 'http')">
													<xsl:value-of select="$article_doi"/>
												</xsl:when>
												<xsl:when test="starts-with($article_doi, 'doi:')">
													<xsl:value-of
														select="concat('http://dx.doi.org/', substring-after($article_doi, 'doi:'))"
													/>
												</xsl:when>
											</xsl:choose>
										</xsl:attribute>
										<xsl:value-of select="$article_doi"/>
									</a>
								</xsl:when>
								<xsl:when test="$article_doi">
									<xsl:copy-of select="substring-before($citation, $article_doi)"/>
									<a>
										<xsl:attribute name="href">
											<xsl:value-of
												select="concat('http://dx.doi.org/', substring-after($article_doi, 'doi:'))"
											/>
										</xsl:attribute>
										<xsl:value-of select="$article_doi"/>
									</a>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="$citation"/>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:when>
						<xsl:otherwise>
							<xsl:variable name="journal" select="$meta[@element='publicationName']"/>
							<xsl:choose>
								<xsl:when test="$journal">
									<span style="font-style: italic;">Citation is not yet available for this article
										from <xsl:value-of select="$journal"/>. It will become available shortly after
										the article is published. <xsl:if test="$article_doi">
											<a>
												<xsl:attribute name="href">
													<xsl:choose>
														<xsl:when test="starts-with($article_doi, 'http')">
															<xsl:value-of select="$article_doi"/>
														</xsl:when>
														<xsl:when test="starts-with($article_doi, 'doi:')">
															<xsl:value-of
																select="concat('http://dx.doi.org/', substring-after($article_doi, 'doi:'))"
															/>
														</xsl:when>
													</xsl:choose>
												</xsl:attribute>
												<xsl:value-of select="$article_doi"/>
											</a>
										</xsl:if>
									</span>
								</xsl:when>
								<xsl:otherwise>
									<span style="font-style: italic;">Citation not yet available.</span>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
				</blockquote>
			</div>
		</xsl:if>

		
		<!-- General, non-citation, metadata display-->
		<html xmlns="http://www.w3.org/1999/xhtml" xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1">
		<!--<link type="text/css" rel="stylesheet" media="screen" href="/metacat/style/skins/default/default.css" />-->
		<xsl:call-template name="documenthead"/>
		<body id="Overview">
		<div id="main_wrapper">
		<xsl:call-template name="bodyheader"/>
		<div id="content_wrapper">
		
		<table class="ds-includeSet-table">
			<tr class="ds-table-row">
				<td>
					<xsl:choose>
						<xsl:when test="$treebase_url != ''">
							<span class="bold">View Full Content in TreeBASE</span>
						</xsl:when>
						<xsl:when test="$knb_url!=''">
							<span class="bold">View Full Content in KNB</span>
						</xsl:when>
						<xsl:when test="$datafiles!=''">
							<span class="bold">Dryad Package Identifier</span>
						</xsl:when>
						<xsl:otherwise>
							<span class="bold">Dryad File Identifier</span>
						</xsl:otherwise>
					</xsl:choose>
				</td>
				<td>
					<xsl:choose>
						<xsl:when test="$my_doi">
							<a>
								<xsl:attribute name="href">
									<xsl:call-template name="checkURL">
										<xsl:with-param name="url"
											select="concat('http://dx.doi.org/', substring-after($my_doi, 'doi:'))"/>
										<xsl:with-param name="localize" select="$localize"/>
									</xsl:call-template>
								</xsl:attribute>
								<xsl:value-of select="$my_doi"/>
							</a>
						</xsl:when>
						<xsl:when test="$my_uri">
							<a>
								<xsl:attribute name="href">
									<xsl:call-template name="checkURL">
										<xsl:with-param name="url" select="$my_uri"/>
										<xsl:with-param name="localize" select="$localize"/>
									</xsl:call-template>
								</xsl:attribute>
								<xsl:value-of select="$my_uri"/>
							</a>
						</xsl:when>
					</xsl:choose>
				</td>
			</tr>

			<xsl:if
				test=".//dim:field[@element='identifier'][not(@qualifier)][not(contains(., 'dryad.'))]">
				
				<xsl:variable name="dc-creators" select=".//dim:field[@element='creator'][@mdschema='dc']"/>

				<xsl:if test="$dc-creators != ''">
					<tr class="ds-table-row">
						<td>
							<xsl:choose>
								<xsl:when test="count($dc-creators) &gt; 1">
									<span class="bold">Authors</span>
								</xsl:when>
								<xsl:otherwise>
									<span class="bold">Author</span>
								</xsl:otherwise>
							</xsl:choose>
						</td>
						<td>
							<xsl:for-each select="$dc-creators">
								<xsl:value-of select="."/><br/>
							</xsl:for-each>
						</td>
					</tr>
				</xsl:if>
				
				<xsl:variable name="dc-publishers"
					select=".//dim:field[@element='publisher'][@mdschema='dc']"/>
				
				<xsl:if test="$dc-publishers != ''">
					<tr class="ds-table-row">
						<td>
							<xsl:choose>
								<xsl:when test="count($dc-publishers) &gt; 1">
									<span class="bold">Publishers</span>
								</xsl:when>
								<xsl:otherwise>
									<span class="bold">Publisher</span>
								</xsl:otherwise>
							</xsl:choose>
						</td>
						<td>
							<xsl:for-each select="$dc-publishers">
								<xsl:value-of select="."/><br/>
							</xsl:for-each>
						</td>
					</tr>
				</xsl:if>
				
				<xsl:variable name="dc-date"
					select=".//dim:field[@element='date'][not(@qualifier)][@mdschema='dc']"/>
				
				<xsl:if test="$dc-date != ''">
					<tr class="ds-table-row">
						<td>
								<span class="bold">Published</span>
						</td>
						<td>
							<xsl:value-of select="$dc-date[1]"/>
						</td>
					</tr>
				</xsl:if>
			</xsl:if>

			<xsl:variable name="description">
				<xsl:for-each select=".//dim:field[@element='description'][not(@qualifier='provenance')]">
					<xsl:copy-of select="node()"/>
					<br/>
				</xsl:for-each>
			</xsl:variable>

			<xsl:if test="$description!=''">
				<tr class="ds-table-row">
					<td>
						<xsl:choose>
							<xsl:when test="//dim:field[@element='relation'][@qualifier='ispartof']">
								<span class="bold">Description</span>
							</xsl:when>
							<xsl:otherwise>
								<span class="bold">Abstract</span>
							</xsl:otherwise>
						</xsl:choose>
					</td>
					<td>
						<xsl:copy-of select="$description"/>
					</td>
					<td> </td>
				</tr>
			</xsl:if>

			<xsl:variable name="describedBy">
				<xsl:for-each select=".//dim:field[@element='relation' and @qualifier='ispartof']">
					<a>
						<xsl:attribute name="href">
							<xsl:call-template name="checkURL">
								<xsl:with-param name="url" select="."/>
								<xsl:with-param name="localize" select="$localize"/>
							</xsl:call-template>
						</xsl:attribute>
						<xsl:copy-of select="."/>
					</a>
					<br/>
				</xsl:for-each>
			</xsl:variable>
			<xsl:if test="$describedBy!=''">
				<tr class="ds-table-row">
					<td>
						<span class="bold">Contained in Data Package</span>
					</td>
					<td>
						<xsl:copy-of select="$describedBy"/>
					</td>
					<td> </td>
				</tr>
			</xsl:if>

			<xsl:variable name="sciNames">
				<xsl:for-each select=".//dim:field[@element='ScientificName']">
					<xsl:copy-of select="node()"/>
					<br/>
				</xsl:for-each>
			</xsl:variable>
			<xsl:if test="$sciNames!=''">
				<tr class="ds-table-row">
					<td>
						<span class="bold">Scientific Names</span>
					</td>
					<td>
						<xsl:copy-of select="$sciNames"/>
					</td>
					<td> </td>
				</tr>
			</xsl:if>

			<xsl:variable name="spatialCoverage">
				<xsl:for-each select=".//dim:field[@element='coverage'][@qualifier='spatial']">
					<xsl:copy-of select="node()"/>
					<br/>
				</xsl:for-each>
			</xsl:variable>
			<xsl:if test="$spatialCoverage!=''">
				<tr class="ds-table-row">
					<td>
						<span class="bold">Spatial Coverage</span>
					</td>
					<td>
						<xsl:copy-of select="$spatialCoverage"/>
					</td>
					<td> </td>
				</tr>
			</xsl:if>

			<xsl:variable name="temporalCoverage">
				<xsl:for-each select=".//dim:field[@element='coverage'][@qualifier='temporal']">
					<xsl:copy-of select="node()"/>
					<br/>
				</xsl:for-each>
			</xsl:variable>
			<xsl:if test="$temporalCoverage!=''">
				<tr class="ds-table-row">
					<td>
						<span class="bold">Temporal Coverage</span>
					</td>
					<td>
						<xsl:copy-of select="$temporalCoverage"/>
					</td>
					<td> </td>
				</tr>
			</xsl:if>

			<xsl:variable name="keywords">
				<xsl:for-each select=".//dim:field[@element='subject'][@mdschema='dc'][not(@qualifier)]">
					<xsl:copy-of select="node()"/>
					<br/>
				</xsl:for-each>
			</xsl:variable>
			<xsl:if test="$keywords!=''">
				<tr class="ds-table-row">
					<td>
						<span class="bold">Keywords</span>
					</td>
					<td>
						<xsl:copy-of select="$keywords"/>
					</td>
					<td> </td>
				</tr>
			</xsl:if>

			<xsl:if test=".//dim:field[@element='identifier'][not(@qualifier)][contains(., 'dryad.')]">
				<tr class="ds-table-row">
					<td>
						<span class="bold">Date Deposited</span>
					</td>
					<td>
						<xsl:copy-of select=".//dim:field[@element='date' and @qualifier='accessioned']"/>
					</td>
					<td> </td>
				</tr>
			</xsl:if>

		</table>

		<!-- we only want this view from item view - not the administrative pages -->
		<xsl:if test="$meta[@qualifier='URI' and contains(.., 'handle')]">
			<div style="padding: 10px; margin-top: 5px; margin-bottom: 5px;">
				<a href="?show=full">Show Full Metadata</a>
			</div>
		</xsl:if>

		<xsl:variable name="embargoedDate"
			select=".//dim:field[@element='date' and @qualifier='embargoedUntil']"/>
		<xsl:variable name="embargoType">
			<xsl:choose>
				<xsl:when test=".//dim:field[@element='type' and @qualifier='embargo']">
					<xsl:value-of select=".//dim:field[@element='type' and @qualifier='embargo']"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>unknown</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$embargoedDate!=''">
				<!-- this all might be overkill, need to confirm embargoedDate element disappears after time expires -->
				<xsl:variable name="dateDiff">
					<xsl:call-template name="datetime:difference">
						<xsl:with-param name="start" select="datetime:date()"/>
						<xsl:with-param name="end" select="datetime:date($embargoedDate)"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:choose>
					<xsl:when test="$embargoedDate='9999-01-01' and $embargoType='oneyear'">
						<div id="embargo_notice">
							<i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.oneyear</i18n:text>
						</div>
					</xsl:when>
					<xsl:when
						test="$embargoedDate='9999-01-01' and ($embargoType='untilArticleAppears' or $embargoType='unknown')">
						<div id="embargo_notice">
							<i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.publication</i18n:text>
						</div>
					</xsl:when>
					<xsl:when test="not(starts-with($dateDiff, '-'))">
						<div id="embargo_notice">
							<i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource</i18n:text>
							<xsl:value-of select="$embargoedDate"/>
						</div>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="checkedAndNoEmbargo"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
				<xsl:call-template name="checkedAndNoEmbargo"/>
			</xsl:when>
		</xsl:choose>

		<xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>

		<xsl:if
			test=".//dim:field[@element='rights'][.='http://creativecommons.org/publicdomain/zero/1.0/']">
			<xsl:choose>
				<!-- this all might be overkill, need to confirm embargoedDate element disappears after time expires -->
				<xsl:when test="$embargoedDate!=''">
					<xsl:variable name="dateDiff">
						<xsl:call-template name="datetime:difference">
							<xsl:with-param name="start" select="datetime:date()"/>
							<xsl:with-param name="end" select="datetime:date($embargoedDate)"/>
						</xsl:call-template>
					</xsl:variable>
					<xsl:if test="starts-with($dateDiff, '-')">
						<div style="padding-top: 10px;">
							<i18n:text>xmlui.dri2xhtml.METS-1.0.license-cc0</i18n:text>
							<xsl:text> &#160; </xsl:text>
							<a href="http://creativecommons.org/publicdomain/zero/1.0/" target="_blank">
								<img src="knb/style/skins/mets/cc-zero.png"/>
							</a>
							<a href="http://opendefinition.org/">
								<img src="/metacat/style/skins/mets/opendata.png"/>
							</a>
						</div>
					</xsl:if>
				</xsl:when>
				<xsl:otherwise>
					<div style="padding-top: 10px;">
						<i18n:text>xmlui.dri2xhtml.METS-1.0.license-cc0</i18n:text>
						<xsl:text> &#160; </xsl:text>
						<a href="http://creativecommons.org/publicdomain/zero/1.0/" target="_blank">
							<img src="/metacat/style/skins/mets/cc-zero.png"/>
						</a>
						<a href="http://opendefinition.org/">
							<img src="/metacat/style/skins/mets/opendata.png"/>
						</a>
					</div>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
		</div>
		<xsl:call-template name="bodyfooter"/>
		</div>
		</body>
		</html>
		
	</xsl:template>
	
	<xsl:template name="documenthead">
		<head>
			<title>Data Repository
			
            </title>
                        
			<link rel="stylesheet" type="text/css"
				href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />
			<script language="Javascript" type="text/JavaScript"
				src="{$contextURL}/style/skins/{$qformat}/{$qformat}.js" />
			<script language="Javascript" type="text/JavaScript"
				src="{$contextURL}/style/common/branding.js" />
			<link rel="stylesheet" type="text/css"
				href="{$contextURL}/style/skins/{$qformat}/{$qformat}.css" />

			<script language="JavaScript">
				<![CDATA[
         		function submitform(action,form_ref) {
             		form_ref.action.value=action;
             		form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
             		form_ref.qformat.value="]]><xsl:value-of select="$qformat" /><![CDATA[";
		            form_ref.submit();
         		}
				]]>
			</script>
		</head>
	</xsl:template>
	
	<xsl:template name="bodyheader">
		<!-- header here -->
		<script language="JavaScript">
			insertTemplateOpening('<xsl:value-of select="$contextURL" />');
		</script>
	</xsl:template>
	
	<xsl:template name="bodyfooter">
		<!-- footer here -->
		<script language="JavaScript">
			insertTemplateClosing('<xsl:value-of select="$contextURL" />');
		</script>
	</xsl:template>

	<xsl:template name="checkedAndNoEmbargo">
		<table class="ds-table file-list">
			<tr class="ds-table-header-row"> </tr>
			<tr>
				<xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
					<xsl:with-param name="context" select="."/>
					<xsl:with-param name="primaryBitstream"
						select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"
					/>
				</xsl:apply-templates>
			</tr>
		</table>
	</xsl:template>


</xsl:stylesheet>
