<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="sessid"/>
    <xsl:param name="enableediting">false</xsl:param>
    <!-- This parameter gets overidden by the chosen default qformat -->
    <xsl:param name="qformat">default</xsl:param>
    <!-- this module for displaying a list of sbc citations, which is NOT an EML DOC -->
    <!-- parameters for searching, to be passed into stylesheet. defaults to all if no select 
    need 2 sets of quotes in select= " 'string' "
        <xsl:param name="author" select=" 'Reed' "/>   -->
    <!-- only using author at present -->
    <xsl:param name="author"/>
    <xsl:param name="keyword"/>
    <xsl:param name="year"/>
    
    <!-- another output attribute: media-type="text/xml" -->
    <xsl:preserve-space elements="text"/>
    <!--
    a parameter to use for counting authors or editors (using this? think not) -->
    <xsl:param name="nameCount"/>
    <!--
    parameters to use for storing contact author for reprint request  -->
    <xsl:param name="contactEmail"/>
    <xsl:param name="creatorEmail"/>
    <!-- 
    
   Variables for the DOI:
    url which can resolve DOIs (keep the trailing slash, so template can add the doi only)-->
    <xsl:variable name="doi_resolver_url">http://dx.doi.org/</xsl:variable>
    <!-- 
    text for the tooltip keep all on one line!-->
    <xsl:variable name="tooltip_text_DOI">Digital Object Identifer (DOI): an identifier for a resource on a digital network. Click on the DOI to go directly to the paper at the publisher\'s website. DOIs simplify maintenance of our database because a \'Resolver\' managed by the DOI Foundation handles the connection between the browser and the publisher\'s website</xsl:variable>
    <!--
    
    keys for grouping by year.    -->
    <!--  a key for grouping articles by pubdate -->
    <xsl:key name="articles-by-pubDate" match="citation[(article or chapter) and
        normalize-space(pubDate) ]" use="pubDate"/>
    <!--  
    a key for grouping presentations by pubdate -->
    <xsl:key name="presentations-by-pubDate" match="citation[(presentation) and
        normalize-space(pubDate)]" use="pubDate"/>
    <!--  
    a key for grouping articles by pubdate and author
    an attempt at more complicated grouping, to get the label and the citations to show together
    doesnt work yet. -->
    <xsl:key name="articles-by-pubDate_author" match="citation[(article or chapter) and
        normalize-space(pubDate)]" use="concat(pubDate,':',creator/individualName/surName)"/>
    <!--
    
      create a path to the tooltip.js (get tokens in for this)      /style/skins/{$qformat}  -->
    <xsl:variable name="javascript_path">/catalog/style/skins/sbclter</xsl:variable>
    <!--
    
    create a metacat url with variables (switch to tokens, please) -->
    <xsl:variable name="catalog">http://sbc.lternet.edu/catalog/</xsl:variable>
    <xsl:variable name="style">sbclter</xsl:variable>
    <xsl:variable name="metacat_url">
        <xsl:value-of select="$catalog"/>
        <xsl:text>metacat?action=read&amp;qformat=sbclter</xsl:text>
        <xsl:text>&amp;docid=</xsl:text>
        <!-- docid attached later -->
    </xsl:variable>
    <!--
    variable for sending reprint request via email -->
    <!--   <xsl:variable name="send_email">
           <a href="mailto:{$contactEmail}?subject=Reprint Request, via SBC-LTER pubsDB">request from author</a> 
    </xsl:variable>  -->
    <!-- 
    Change this match expression if reading single-citation docs
    
    
    -->
    <xsl:template match="/citationList">
   
        <html>
            <head>
                <script type="text/javascript"/>
                <!--
                        <script language="JavaScript" type="text/javascript" src="./wz_tooltip.js"></script>
             -->
                <!--
                <script language="JavaScript">
                    <![CDATA[
                    function resolve_doi {
                        document.getSelection();
                         if(!Qr){void(Qr=prompt('Enter DOI to resolve, e.g. 10.1000/202:',''))}
                         if(Qr)location.href='http://dx.doi.org/'+escape(Qr)+' '
                   ]]>
                    </script>
        -->
                <!-- begin the header area
        <xsl:call-template name="pageheader" />
        end the header area -->
            </head>
            <body>
                <table>
                    <tr>
                        <td>
                            <a name="INPRESS"/>
                            <h4>In Press</h4>
                        </td>
                    </tr>
                    <!-- matches only publish-able citations (no theses or presentations), 
			which also have an empty (or whitespace) pubDate -->
                    <xsl:for-each select="citation[(article or
                        chapter or
                        book or                 
                        editedBook or                
                        report or
                        audioVisual) and                     
                        not(normalize-space(pubDate)) and
                        creator/individualName/surName[contains(.,$author)] 
                        ]">
                        <xsl:sort select="creator/individualName/surName" order="ascending"/>
                        <tr>
                            <td class="eighty_percent">
                                <div class="hanging-paragraph smaller-font bottom-padding">
                                    <xsl:call-template name="citation">
                                        <xsl:with-param name="author"/>
                                    </xsl:call-template>
                                </div>
                            </td>
                            <td class="twenty_percent">
                                <div class="bottom-padding left-padding smaller-font">
                                    <xsl:call-template name="paper_link_doi"/>
                                </div>
                            </td>
                        </tr>
                    </xsl:for-each>
                    <tr>
                        <td>
                            <h4>Published</h4>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <a id="ART_CHAP"/>
                            <h5>Articles and Chapters</h5>
                        </td>
                    </tr>
                    <!-- citation[(article or chapter) and normalize-space(pubDate) and -->
                    <xsl:for-each select="
                        citation[generate-id(.)=generate-id(key('articles-by-pubDate',
                        pubDate))]/pubDate">
                        <xsl:sort select="." order="descending"/>
                        <tr>
                            <td>
                                <h5>
                                    <xsl:value-of select="."/>
                                </h5>
                            </td>
                        </tr>
                        <!--    <tr><td><xsl:value-of select="count(key('articles-by-pubDate',.))"/></td></tr> -->
                        <xsl:for-each select="key('articles-by-pubDate',.)">
                            <xsl:sort select="creator/individualName/surName" order="ascending"/>
                            <xsl:if test="creator/individualName/surName[contains(.,$author)] ">
                                <tr>
                                    <td class="eighty_percent">
                                        <div class="hanging-paragraph smaller-font bottom-padding">
                                            <xsl:call-template name="citation">
                                                <xsl:with-param name="author" select="$author"/>
                                            </xsl:call-template>
                                        </div>
                                    </td>
                                    <td class="twenty_percent">
                                        <div class="bottom-padding left-padding smaller-font">
                                            <xsl:call-template name="paper_link_doi"/>
                                        </div>
                                    </td>
                                </tr>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:for-each>
                    <tr>
                        <td>
                            <a id="BOOKS"/>
                            <h5>Books</h5>
                        </td>
                    </tr>
                    <xsl:for-each select="citation[(book or editedBook) and
                        normalize-space(pubDate) and
                        creator/individualName/surName[contains(.,$author)] ]">
                        <xsl:sort select="creator/individualName/surName" order="ascending"/>
                        <tr>
                            <td class="eighty_percent">
                                <div class="hanging-paragraph smaller-font bottom-padding">
                                    <xsl:call-template name="citation">
                                        <xsl:with-param name="author"/>
                                    </xsl:call-template>
                                </div>
                            </td>
                            <td class="twenty_percent">
                                <div class="bottom-padding left-padding smaller-font">
                                    <xsl:call-template name="paper_link_doi"/>
                                </div>
                            </td>
                        </tr>
                    </xsl:for-each>
                    <tr>
                        <td>
                            <a id="THESES"/>
                            <h5>Theses</h5>
                        </td>
                    </tr>
                    <xsl:for-each select="citation[thesis and
                        normalize-space(pubDate) and
                        creator/individualName/surName[contains(.,$author)] ] ">
                        <xsl:sort select="creator/individualName/surName" order="ascending"/>
                        <tr>
                            <td class="eighty_percent">
                                <div class="hanging-paragraph smaller-font bottom-padding">
                                    <xsl:call-template name="citation">
                                        <xsl:with-param name="author"/>
                                    </xsl:call-template>
                                </div>
                            </td>
                            <td class="twenty_percent">
                                <div class="bottom-padding left-padding smaller-font">
                                    <xsl:call-template name="paper_link_doi"/>
                                </div>
                            </td>
                        </tr>
                    </xsl:for-each>
                    <tr>
                        <td>
                            <a id="REP_CP"/>
                            <h5>Reports and Contributions to Conference Proceedings</h5>
                        </td>
                    </tr>
                    <xsl:for-each select="citation[(report or conferenceProceedings) and
                        normalize-space(pubDate) and
                        creator/individualName/surName[contains(.,$author)]  ]">
                        <xsl:sort select="creator/individualName/surName" order="ascending"/>
                        <tr>
                            <td class="eighty_percent">
                                <div class="hanging-paragraph smaller-font bottom-padding">
                                    <xsl:call-template name="citation">
                                        <xsl:with-param name="author"/>
                                    </xsl:call-template>
                                </div>
                            </td>
                            <td class="twenty_percent">
                                <div class="bottom-padding left-padding smaller-font">
                                    <xsl:call-template name="paper_link_doi"/>
                                </div>
                            </td>
                        </tr>
                    </xsl:for-each>
                    <tr>
                        <td>
                            <a id="AV"/>
                            <h5>Audio-Visual Resources</h5>
                        </td>
                    </tr>
                    <xsl:for-each select="citation[audioVisual and
                        normalize-space(pubDate) and
                        creator/individualName/surName[contains(.,$author)]  ] ">
                        <xsl:sort select="creator/individualName/surName" order="ascending"/>
                        <tr>
                            <td class="eighty_percent">
                                <div class="hanging-paragraph smaller-font bottom-padding">
                                    <xsl:call-template name="citation">
                                        <xsl:with-param name="author"/>
                                    </xsl:call-template>
                                </div>
                            </td>
                            <td class="twenty_percent">
                                <div class="bottom-padding left-padding smaller-font">
                                    <xsl:call-template name="paper_link_doi"/>
                                </div>
                            </td>
                        </tr>
                    </xsl:for-each>
                    <tr>
                        <td>
                            <a id="PRESENTATIONS"/>
                            <h4>Presentations</h4>
                        </td>
                    </tr>
                    <xsl:for-each select="citation[presentation and
                        normalize-space(pubDate) and
                        generate-id(.)=generate-id(key('presentations-by-pubDate',pubDate))]/pubDate
                        ">
                        <xsl:sort select="." order="descending"/>
                        <tr>
                            <td>
                                <h5>
                                    <xsl:value-of select="."/>
                                </h5>
                            </td>
                        </tr>
                        <xsl:for-each select="key('presentations-by-pubDate',.)">
                            <xsl:sort select="creator/individualName/surName" order="ascending"/>
                            <xsl:if test="creator/individualName/surName[contains(.,$author)] ">
                                <tr>
                                    <td class="eighty_percent">
                                        <div class="hanging-paragraph smaller-font bottom-padding">
                                            <xsl:call-template name="citation">
                                                <xsl:with-param name="author" select="$author"/>
                                            </xsl:call-template>
                                        </div>
                                    </td>
                                    <td class="twenty_percent">
                                        <div class="bottom-padding left-padding smaller-font">
                                            <xsl:call-template name="paper_link_doi"/>
                                        </div>
                                    </td>
                                </tr>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:for-each>
                </table>
                <script type="text/javascript" language="JavaScript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="$javascript_path"/>
                        <xsl:text>/wz_tooltip.js</xsl:text>
                    </xsl:attribute>
                </script>
                <!-- <script language="JavaScript" type="text/javascript" src="./wz_tooltip.js"></script>  -->
            </body>
        </html>
        <!-- begin the footer area
        <xsl:call-template name="pagefooter" />
         end the footer area -->
        <!-- 

	used to be closing body and html tags here









-->
    </xsl:template>
    <!--
    -->
    <!--
    -->
    <xsl:template match="citation" name="citation">
        <xsl:param name="nameCount" select="count(creator)"/>
        <!-- 
        <tr>
            <td class="eighty_percent">  
                <div class="hanging-paragraph smaller-font bottom-padding"> -->
        <!--
                        ? put these (authors, date, title) in a tempate called citation_common? -->
        <!--
                        display authors with template, add punct.-->
        <xsl:call-template name="display_names">
            <xsl:with-param name="nameCount" select="$nameCount"/>
            <!-- doesnt seem to like this parameter. why? -->
        </xsl:call-template>
        <xsl:apply-templates select="creator/individualName"/>
        <!--
                        punctuate creators, add text if this is an edited book 
			note: jsp did not like the &#160; that was in the empty text string. alternative? 
			oxygen cleaned this to remove whitespace if I didnt put in. -->
        <xsl:choose>
            <xsl:when test="$nameCount=1">
                <xsl:choose>
                    <xsl:when test="editedBook">
                        <xsl:text>, ed. </xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text> </xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!-- more than one creator -->
                <xsl:choose>
                    <xsl:when test="editedBook">
                        <xsl:text>, eds. </xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>. </xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
        <!--
                        the date goes in only if published -->
        <xsl:if test="normalize-space(pubDate)">
            <xsl:value-of select="pubDate"/>
            <xsl:text>. </xsl:text>
        </xsl:if>
        <!-- 
                        the title goes in all. books are underlined. 
	            titles are mixed content: apply-templates invokes built-in rule 
	            for title text, italics template below. -->
        <xsl:choose>
            <xsl:when test="editedBook">
                <xsl:call-template name="display-book-title"/>
            </xsl:when>
            <xsl:when test="book">
                <xsl:call-template name="display-book-title"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="title"/>
                <xsl:text>. </xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        <!-- 


                        end of common area. 
                        add the rest of the citation using a template for each type
                    Note: the ifs are now takin care of in the type templates, and inpress pubs should
                    show whatever info is available. -->
        <!--         <xsl:if test="normalize-space(pubDate)"> -->
        <xsl:choose>
            <xsl:when test="article">
                <xsl:call-template name="article"/>
            </xsl:when>
            <xsl:when test="chapter">
                <xsl:call-template name="chapter"/>
            </xsl:when>
            <xsl:when test="book">
                <xsl:call-template name="book"/>
            </xsl:when>
            <xsl:when test="editedBook">
                <xsl:call-template name="editedBook"/>
            </xsl:when>
            <xsl:when test="thesis">
                <xsl:call-template name="thesis"/>
            </xsl:when>
            <xsl:when test="presentation">
                <xsl:call-template name="presentation"/>
            </xsl:when>
            <xsl:when test="report">
                <xsl:call-template name="report"/>
            </xsl:when>
            <xsl:when test="audioVisual">
                <xsl:call-template name="audioVisual"/>
            </xsl:when>
            <xsl:when test="conferenceProceedings">
                <xsl:call-template name="proceedings"/>
            </xsl:when>
        </xsl:choose>
        
        <!-- this bit  shows the sbc id. for now, it makes entries easier to find and edit, but it can also be used to pull up a single-page view  -->
        <xsl:text> (sbc-id: </xsl:text>
        <xsl:value-of select="alternateIdentifier[@system='sbclter-bibliography']"/>
        <xsl:text>)</xsl:text>
        
        
        <!--           </xsl:if>  -->
        <!-- 
                </div>
            </td> 
            <td class="twenty_percent">
                <div class="bottom-padding left-padding smaller-font">
         -->
        <!-- 
                    
                    put the link to paper or resource here
                    <xsl:call-template name="paper_link_doi"/>
                </div>
            </td>
         -->
        <!-- 
        
        save till later 
            <td class="fifteen_percent">
	    <div class="smaller-font bottom-padding">
                 <xsl:call-template name="dataset_link"/>
		</div>
            </td>
        </tr> -->
    </xsl:template>
    <!--
    template to display author's names (creator tree). also needed when citation is a chapter, for book's editors (editor tree) -->
    <xsl:template name="display_names" match="creator/individualName |
        chapter/editor/individualName">
        <!-- <xsl:param name="nameCount" select="$nameCount"/> -->
        <xsl:variable name="nameCount" select="count(../../creator | ../../editor)"/>
        <xsl:if test="$nameCount = 1">
            <xsl:call-template name="surName_inits"/>
        </xsl:if>
        <!--- 
                         2 authors: last, f. and f. last -->
        <xsl:if test="$nameCount = 2">
            <xsl:if test="position() = 1">
                <xsl:call-template name="surName_inits"/>
                <xsl:text> and </xsl:text>
                <xsl:text/>
            </xsl:if>
            <xsl:if test="position() = 2">
                <xsl:call-template name="inits_surName"/>
            </xsl:if>
        </xsl:if>
        <!-- 3+ authors: last, f., f. last, f. last and f. last -->
        <xsl:if test="$nameCount &gt;2 ">
            <xsl:choose>
                <xsl:when test="position() = 1">
                    <xsl:call-template name="surName_inits"/>
                    <xsl:text>, </xsl:text>
                </xsl:when>
                <xsl:when test="position() = last()-1">
                    <xsl:call-template name="inits_surName"/>
                    <xsl:text> and </xsl:text>
                </xsl:when>
                <xsl:when test="position() = last()">
                    <xsl:call-template name="inits_surName"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="inits_surName"/>
                    <xsl:text>, </xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
    <!--
    template for formatting author names -->
    <!-- surName, initials -->
    <xsl:template name="surName_inits">
        <xsl:value-of select="surName"/>
        <xsl:text>, </xsl:text>
        <xsl:for-each select="givenName">
            <xsl:value-of select="substring(.,1,1)"/>
            <xsl:choose>
                <xsl:when test="position() = last()">
                    <xsl:text>.</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>. </xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>
    <!--
    template for formatting author names -->
    <!--initials surName -->
    <xsl:template name="inits_surName">
        <xsl:for-each select="givenName">
            <xsl:value-of select="substring(.,1,1)"/>
            <xsl:text>. </xsl:text>
        </xsl:for-each>
        <xsl:value-of select="surName"/>
    </xsl:template>
    <!--
    TEMPLATE FOR BOOK TITLES (used conditionally)-->
    <xsl:template name="display-book-title">
        <u>
            <xsl:apply-templates select="title"/>
        </u>
        <xsl:text>.&#160; </xsl:text>
    </xsl:template>
    <!--
    TEMPLATE FOR chapter/bookTitle (always underlined) -->
    <xsl:template name="display-bookTitle">
        <xsl:if test="chapter">
            <u>
                <xsl:apply-templates select="chapter/bookTitle"/>
            </u>
        </xsl:if>
        <xsl:if test="conferenceProceedings">
            <u>
                <xsl:apply-templates select="conferenceProceedings/bookTitle"/>
            </u>
        </xsl:if>
        <xsl:text>.&#160;</xsl:text>
    </xsl:template>
    <!--
    TEMPLATE FOR ITALICs -->
    <xsl:template match="emphasis">
        <i>
            <xsl:apply-templates/>
        </i>
    </xsl:template>
    <!-- to be replaced with this, eventually?
    <xsl:template match="italic">
         <i><xsl:apply-templates/></i>
     </xsl:template>
    <xsl:template match="bold">
         <b><xsl:apply-templates/></b>
     </xsl:template>
    <xsl:template match="underline">
         <u><xsl:apply-templates/></u>
     </xsl:template>
-->
    <!-- 
    TEMPLATE for a conferenceLocation ADDRESS -->
    <xsl:template name="address">
        <!--  this template is called, so context node is citation, and match need to be in test statements -->
        <xsl:if test="descendant::deliveryPoint">
            <xsl:for-each select="descendant::deliveryPoint">
                <xsl:value-of select="."/>
                <!-- its not last delivery point, so a comma between -->
                <xsl:if test="position() != last()">, </xsl:if>
                <!--  if its the last deliveryPoint, and more address tags follow, put a comma. -->
                <xsl:if test="position() = last()">
                    <xsl:if test="following-sibling::*">
                        <xsl:text>, </xsl:text>
                    </xsl:if>
                </xsl:if>
            </xsl:for-each>
            <!-- finished with deliveryPoints -->
        </xsl:if>
        <xsl:if test="descendant::city">
            <xsl:value-of select="descendant::city"/>
            <xsl:if test="descendant::administrativeArea">
                <xsl:text>, </xsl:text>
            </xsl:if>
            <xsl:if test="descendant::country">
                <xsl:text>, </xsl:text>
            </xsl:if>
        </xsl:if>
        <xsl:if test="descendant::administrativeArea">
            <xsl:value-of select="descendant::administrativeArea"/>
            <xsl:if test="descendant::country">
                <xsl:text>, </xsl:text>
            </xsl:if>
        </xsl:if>
        <xsl:if test="descendant::country">
            <xsl:value-of select="descendant::country"/>
        </xsl:if>
        <!-- end with .  -->
        <xsl:text>.</xsl:text>
    </xsl:template>
    <!-- 
    TEMPLATE FOR ARTICLE REF -->
    <xsl:template name="article">
        <xsl:if test="descendant::journal[text()]">
            <xsl:value-of select="article/journal"/>
            <xsl:if test="descendant::volume[text()]">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="article/volume"/>
                <xsl:if test="descendant::pageRange[text()]">
                    <xsl:text>: </xsl:text>
                    <xsl:value-of select="article/pageRange"/>
                </xsl:if>
            </xsl:if>
        </xsl:if>
        <xsl:text>. </xsl:text>
    </xsl:template>
    <!-- 
    TEMPLATE FOR CHAPTER REF -->
    <xsl:template name="chapter">
        <xsl:text>in: </xsl:text>
        <xsl:call-template name="display-bookTitle"/>
        <xsl:call-template name="display_names"/>
        <xsl:apply-templates select="chapter/editor/individualName"/>
        <!-- add punctuation for the editors-->
        <xsl:choose>
            <xsl:when test="$nameCount=1">
                <xsl:text>,&#160;ed.&#160;</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <!-- more than one creator -->
                <xsl:text>,&#160;eds.&#160;</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:value-of select="chapter/publisher/organizationName"/>
        <xsl:text>.&#160;</xsl:text>
    </xsl:template>
    <!-- 
    TEMPLATE FOR BOOK REF -->
    <xsl:template name="book">
        <xsl:value-of select="book/publisher/organizationName"/>
        <xsl:text>. </xsl:text>
        <xsl:if test="descendant::totalPages[text()]">
            <xsl:value-of select="book/totalPages"/>
            <xsl:text>pp. </xsl:text>
        </xsl:if>
    </xsl:template>
    <!-- 
    TEMPLATE FOR editedBOOK REF -->
    <xsl:template name="editedBook">
        <xsl:value-of select="editedBook/publisher/organizationName"/>
        <xsl:text>. </xsl:text>
        <xsl:if test="editedBook/totalPages">
            <xsl:value-of select="."/>
            <xsl:text>pp. </xsl:text>
        </xsl:if>
    </xsl:template>
    <!-- 
    TEMPLATE FOR conferenceProceedings REF -->
    <xsl:template name="proceedings">
        <xsl:text> In: </xsl:text>
        <xsl:call-template name="display-bookTitle"/>
        <xsl:if test="descendant::pageRange[text()]">
            <xsl:text>pp</xsl:text>
            <xsl:value-of select="*/pageRange"/>
            <xsl:text>. </xsl:text>
        </xsl:if>
        <xsl:if test="descendant::conferenceDate[text()]">
            <xsl:value-of select="*/conferenceDate"/>
            <xsl:text>. </xsl:text>
        </xsl:if>
        <xsl:call-template name="address"/>
        <xsl:text>(</xsl:text>
        <xsl:value-of select="conferenceProceedings/publisher/organizationName"/>
        <xsl:text>).</xsl:text>
    </xsl:template>
    <!-- 
    TEMPLATE FOR report REF -->
    <xsl:template name="report">
        <xsl:if test="report/reportNumber">
            <xsl:text>Rpt. No. </xsl:text>
            <xsl:value-of select="report/reportNumber"/>
            <xsl:text>, </xsl:text>
        </xsl:if>
        <xsl:if test="report/publisher/organizationName">
            <xsl:value-of select="report/publisher/organizationName"/>
            <xsl:text>. </xsl:text>
        </xsl:if>
        <xsl:if test="report/totalPages">
            <xsl:value-of select="report/totalPages"/>
            <xsl:text>pp. </xsl:text>
        </xsl:if>
    </xsl:template>
    <!-- 
    TEMPLATE FOR THESIS REF -->
    <xsl:template name="thesis">
        <xsl:value-of select="thesis/degree"/>
        <xsl:text>, </xsl:text>
        <xsl:value-of select="thesis/institution/organizationName"/>
        <!--  look for text in descendants of sibs following institution  -->
        <xsl:choose>
            <xsl:when test="thesis/institution/following-sibling::descendant[text()]">
                <xsl:text>, </xsl:text>
                <xsl:if test="descendant::address[child::*[text()]]">
                    <xsl:call-template name="address"/>
                </xsl:if>
                <xsl:if test="descendant::totalPages[text()]">
                    <xsl:text>&#160;</xsl:text>
                    <xsl:value-of select="thesis/totalPages"/>
                    <xsl:text>pp.</xsl:text>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <!-- nothing follows institution -->
                <xsl:text>.</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- 
    TEMPLATE FOR AUDIO-VISUAL REF -->
    <xsl:template name="audioVisual">
        <xsl:value-of select="audioVisual/publisher/organizationName"/>
        <xsl:text>. </xsl:text>
        <xsl:if test="audioVisual/publicationPlace">
            <xsl:value-of select="audioVisual/publicationPlace"/>
            <xsl:text>. </xsl:text>
        </xsl:if>
    </xsl:template>
    <!-- 
    TEMPLATE FOR PRESENTATION REF -->
    <xsl:template name="presentation">
        <xsl:text> at: </xsl:text>
        <xsl:value-of select="presentation/conferenceName"/>
        <!-- if a citation has a address descendant with a child containing text, 
            add a comma and call address, otherwise end with a . -->
        <xsl:choose>
            <xsl:when test="descendant::address[child::*[text()]]">
                <xsl:text>, </xsl:text>
                <xsl:call-template name="address"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>.</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- 

    Include a link to the paper. FIrst look for a DOI. 
    then a pdf or doc (for inpress only?) or a ppt for presentation.
    default to author's email, defaults or text if no usable info. -->
    <!-- 
    
    
    *** you could reorganize the whole damn thing and create templates for accessing each type of resource. 
    which can hold the entire row. currently, parts of each , and now the second col is type-dependent. 
    then you can sort the big chunk of articles by date within it's own template! -->
    <xsl:template name="paper_link_doi">
        <xsl:variable name="paperUrl" select="distribution/online/url"/>
        <xsl:variable name="paperDoi" select="alternateIdentifier[@system='DOI']"/>
        <xsl:variable name="contactEmail" select="normalize-space(contact/electronicMailAddress)"/>
        <xsl:variable name="creatorEmail"
            select="normalize-space(creator[position()=1]/electronicMailAddress)"/>
        <xsl:choose>
            <xsl:when test="not(normalize-space(pubDate))">
            <!-- the paper is not yet published, ok to distribute a pdf or word doc (wont have a doi yet.) -->
                <xsl:choose>
                    <xsl:when test="contains($paperUrl,'.pdf')">
                        <a href="{$paperUrl}">view PDF document</a>
                    </xsl:when>
                    <xsl:when test="contains($paperUrl,'.doc')">
                        <a href="{$paperUrl}">MS-Word preprint</a>
                    </xsl:when>
                    <xsl:when test="contains($paperUrl,'.gov')">
                        <a href="{$paperUrl}">available</a>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- no distribution/url, so default to an email address -->
                        <xsl:call-template name="create_email_link">
                            <xsl:with-param name="contactEmail" select="$contactEmail"/>
                            <xsl:with-param name="creatorEmail" select="$creatorEmail"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!--  
                it's got a pub date, copyrights may apply -->
                <xsl:choose>
                    <!-- but if its a presentation, OK to distribute a powerpoint  -->
                <xsl:when test="presentation">
                    <xsl:choose>
                        <xsl:when test="contains($paperUrl,'.ppt')">
                            <a href="{$paperUrl}">PowerPoint document</a>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="create_email_link">
                                <xsl:with-param name="contactEmail" select="$contactEmail"/>
                                <xsl:with-param name="creatorEmail" select="$creatorEmail"/>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
            <xsl:otherwise>
                <!--  its got a pub date, and its not a presentation, so copyrights probably apply.
                look for a DOI first-->
                <!-- 
                (maybe will need a url to another catalog, eg, for a film, report or proceedings? -->
                <xsl:choose>
                    <xsl:when test="$paperDoi">
                        <!-- resolve the DOI, with a url  -->
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="$doi_resolver_url"/>
                                <xsl:value-of select="$paperDoi "/>
                            </xsl:attribute>
			<!--        
		    <xsl:value-of select="$paperDoi"/>
			-->
			access with DOI
                            <!-- available via paper's DOI -->
                        </a>
                        <br/>
                        <a href="http://doi.org">
                            <xsl:attribute name="onmouseover">
                                <xsl:text>return escape('</xsl:text>
                                <xsl:value-of select="$tooltip_text_DOI"/>
                                <xsl:text>')</xsl:text>
                            </xsl:attribute> (what's this?) </a>
                    </xsl:when>
                    <xsl:when test="contains($paperUrl,'.com')">
                        <a href="{$paperUrl}">see publisher website</a>
                    </xsl:when>
                    <xsl:when test="contains($paperUrl,'.gov')">
                        <a href="{$paperUrl}">available</a>
                    </xsl:when>
                    <xsl:when test="contains($paperUrl,'.html')">
                        <a href="{$paperUrl}">available through another catalog</a>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- email the contact author -->
                        <xsl:call-template name="create_email_link">
                            <xsl:with-param name="contactEmail" select="$contactEmail"/>
                            <xsl:with-param name="creatorEmail" select="$creatorEmail"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
            </xsl:choose>
    </xsl:template>
    <!-- 
    
    template for sending an email to a contact author -->
    <xsl:template name="create_email_link">
        <xsl:param name="contactEmail" select="$contactEmail"/>
        <xsl:param name="creatorEmail" select="$creatorEmail"/>
        <xsl:choose>
            <xsl:when test="$contactEmail">
                <xsl:variable name="email4requests" select="$contactEmail"/>
                <a href="mailto:{$email4requests}?subject=Reprint Request, via SBC-LTER
                    pubsDB">email author</a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="$creatorEmail">
                        <xsl:variable name="email4requests" select="$creatorEmail"/>
                        <a href="mailto:{$email4requests}?subject=Reprint Request, via
                            SBC-LTER pubs DB">email author</a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>contact author</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--
    
    template for including a link to a dataset-->
    <xsl:template name="dataset_link">
        <xsl:if test="normalize-space(datasetID)">
            <!--create the url to datasets -->
            <!-- might have more than one. -->
            <xsl:for-each select="datasetID">
                <xsl:variable name="docid" select="."/>
                <a href="{$metacat_url}{$docid}">View Dataset</a>
                <br/>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
