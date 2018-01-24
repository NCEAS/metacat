<%@ page     language="java" %>
<!--
/**
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
  */
-->
<%@ include file="settings.jsp"%>
<%@ include file="session_vars.jsp"%>
<!-- *********************** START SEARCHBOX TABLE ************************* -->
<html>
<head>
  <title>Metacat Data Catalog Search Page</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <link href="<%=STYLE_SKINS_URL%>/default/default.css" rel="stylesheet" type="text/css">
  <script language="javascript" 
    type="text/javascript" src="<%=STYLE_SKINS_URL%>/default/default.js">
  </script>
  <script language="javascript" type="text/javascript">
    function trim(stringToTrim) {
      return stringToTrim.replace(/^\s*/, '').replace(/\s*$/,'');
    }
    function allowSearch(formObj) {
      var canSearch = true;
      var searchString = trim(formObj.elements["anyfield"].value);
      if (searchString=="") {
        if (confirm("Show *all* data in the Catalog?\n(this may take some time!)            ")) {
	      formObj.elements["anyfield"].value = "%";
	      canSearch = true;
	      } else {
	        formObj.elements["anyfield"].focus();
	        canSearch = false;
	      }
      } 
      return canSearch;
    }
    function keywordSearch(formObj, searchKeyword) {

       var searchString = trim(searchKeyword);
  
        if (searchString=="") searchString="%";
  
        formObj.anyfield.value=searchString;

        if(checkSearch(formObj)){
             formObj.submit();
       }
       return true;
    }
    function checkSearch(submitFormObj) {
                var checkBox = document.getElementById("searchAll");
                var searchBox = document.getElementById("searchBox");
                var searchString = trim(searchBox.value);

                if (searchString=="") {
                        if (confirm("Show *all* data?")) {
          			searchString = "%";
        		} else {
          			return false;
        		}		
                }

                //if(document.forms[0].radios[0].checked){
		if(!checkBox.checked){
                        if(searchString!="%"){
                                searchBox.name = "title";
                                searchBox.id = "searchBox";

                                submitFormObj.surName.value = searchString;
                                submitFormObj.givenName.value = searchString;
                                submitFormObj.keyword.value = searchString;
                                submitFormObj.organizationName.value = searchString;
                                submitFormObj.para.value = searchString;
                                submitFormObj.geographicDescription.value = searchString;
                                submitFormObj.literalLayout.value = searchString;
                                submitFormObj.operator.value="UNION";
                                var abs = document.getElementById("abstract");
                                abs.value=searchString;
                                var abstractValue = document.getElementById("abstractValue");
                                abstractValue.value=searchString;
                                var keywordValue = document.getElementById("keywordValue");
                        		keywordValue.value=searchString;
                        } else {
                                searchBox.name = "anyfield";
                                searchBox.id = "searchBox";
                                searchBox.value = "%";

                                submitFormObj.surName.value = "";
                                submitFormObj.givenName.value = "";
                                submitFormObj.keyword.value = "";
                                submitFormObj.organizationName.value = "";
                                submitFormObj.para.value = "";
                                submitFormObj.geographicDescription.value = "";
                                submitFormObj.literalLayout.value = "";
                                submitFormObj.operator.value="INTERSECT";
                                var abs = document.getElementById("abstract");
                                abs.value="";
                                var abstractValue = document.getElementById("abstractValue");
                                abstractValue.value="";
                                var keywordValue = document.getElementById("keywordValue");
                        		keywordValue.value="";

			 }
                } else {
                        searchBox.name = "anyfield";
                        searchBox.id = "searchBox";

                        submitFormObj.surName.value = "";
                        submitFormObj.givenName.value = "";
                        submitFormObj.keyword.value = "";
                        submitFormObj.organizationName.value = "";
                        submitFormObj.para.value = "";
                        submitFormObj.geographicDescription.value = "";
                        submitFormObj.literalLayout.value = "";
                        submitFormObj.operator.value="INTERSECT";
                        var abs = document.getElementById("abstract");
                        abs.value="";
                        var abstractValue = document.getElementById("abstractValue");
                        abstractValue.value="";
                        var keywordValue = document.getElementById("keywordValue");
                        keywordValue.value="";
                }


                var radioInput = document.getElementsByName("search");
		       radioInput[0].name = "";

                return true;
        }
  </script>
</head>

<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
  <table width="750px" align="center" cellspacing="0" cellpadding="0" class="group group_border">
    <tr> 
      </td>
      <th class="sectionheader" width="750px" >
        search for data 
      </th>
    </tr>
    <tr> 
      <td colspan="3">
        <table width="100%" border="0" cellpadding="0" cellspacing="0" class="subpanel">
          <tr> 
            <td colspan="2"></td>
          </tr>
          <tr valign="baseline"> 
            <td colspan="2">
              <form action="<%=SERVLET_URL%>" name="searchForm" method="post" 
                target="_top" onSubmit="return checkSearch(this);">
                <%=sessionidField%>
                <%=SIMPLE_SEARCH_METACAT_POST_FIELDS%>
                <input type="hidden" name="organizationName">
  			    <input type="hidden" name="surName">
  			    <input type="hidden" name="givenName">
  			    <input type="hidden" name="keyword">
  			    <input type="hidden" name="keyword/value" id="keywordValue">
  			    <input type="hidden" name="para">
  			    <input type="hidden" name="geographicDescription">
  			    <input type="hidden" name="literalLayout">
  			    <input type="hidden" name="abstract/para" id="abstract">
				<input type="hidden" name="abstract/para/value" id="abstractValue">
                <table width="100%" border="0" cellpadding="5" cellspacing="0">
                  <tr> 
                    <td width="94" rowspan="2" align="left" valign="top">
                      <img src="<%=relativeRoot%>/images/search.jpg" 
                        width="94" height="80">
                    </td>
                    <td colspan="2" valign="middle" class="text_example">
                      <p> 
                        <%= loginStatus %> &nbsp;
                        (<a href="<%=STYLE_SKINS_URL%>/default/index.jsp#iframeloginboxclass" 
                          target="_top"><%=loginButtonLabel%></a>).&nbsp; 
                        You may search the data catalog without being logged 
                        into your account, but will have access only to 
                        &quot;public&quot; data (see &quot;login &amp; 
                        registration&quot;)
                      </p>
                      <p>
                        Enter a search phrase (e.g. biodiversity) to search 
                        for data sets in the data catalog, or simply browse 
                        by category using the links below.
                      </p>
                    </td>
                  </tr>
                  <tr valign="middle"> 
                    <td align="right" class="searchcat"> 
                      <input type="text" name="anyfield" size="30" 
                        maxlength="200" id="searchBox">&nbsp;&nbsp;
                    </td>
                    <td width="365" align="left" class="searchcat"> 
                      <input type="submit" value="Search Data Catalog">
                      &nbsp;&nbsp;&nbsp;
                      
                      <a target="_top" 
                        href="<%=STYLE_SKINS_URL%>/default/index_advanced.jsp"> 
                          &raquo;&nbsp;advanced&nbsp;search&nbsp;&laquo;</a>
                      
                    </td>
                  </tr>

                </table>
              </form>
            </td>
          </tr>
         <tr valign="baseline" >
             <td colspan="2">
				<form>
                 <table width="100%" border="0" cellpadding="0" cellspacing="0" >
		            <tr>
		               <td width="105px" ></td>
			           <td >
			                 <input name="search" type="radio" checked="checked">
								<span class="text_plain"> Search Title, Abstract, Keywords, Personnel (Quicker)</span>
							</input>
		               </td>
		           </tr>
		            <tr>
		                <td width="105px"></td>
			            <td>
			            	<input name="search" type="radio" id="searchAll">
								<span class="text_plain"> Search all fields (Slower)</span>
							</input>
		                </td>
		           </tr>
			      </table>
				</form>
             </td>
		  </tr>
          <tr> 
            <th width="375" class="searchcat">Taxonomy</th>
            <th width="365" class="searchcat">Habitat</th>
          </tr>
          <tr> 
            <td width="375" class="searchsubcat">
              <a href="#" onClick="keywordSearch(document.searchForm, 'plant')" class="searchsubcat">Plant,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'invertebrate')" class="searchsubcat">Invertebrate,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'mammal')" class="searchsubcat">Mammal,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'bird')" class="searchsubcat">Bird,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'reptile')" class="searchsubcat">Reptile,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'amphibian')" class="searchsubcat">Amphibian,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'fungi')" class="searchsubcat">Fungi,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'microbe')" class="searchsubcat">Microbe,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'virus')" class="searchsubcat">Virus</a>
            </td>
            <td width="365" class="searchsubcat">
              <a href="#" onClick="keywordSearch(document.searchForm, 'alpine')" class="searchsubcat">Alpine,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'aquatic')" class="searchsubcat">Aquatic,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'beach')" class="searchsubcat">Beach,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'benthic')" class="searchsubcat">Benthic,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'desert')" class="searchsubcat">Desert,</a> 
              <a href="#" onClick="keywordSearch(document.searchForm, 'estuar')" class="searchsubcat">Estuary,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'forest')" class="searchsubcat">Forest,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'grassland')" class="searchsubcat">Grassland,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'marine')" class="searchsubcat">Marine,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'montane')" class="searchsubcat">Montane,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'ocean')" class="searchsubcat">Oceanic,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'savanna')" class="searchsubcat">Savanna,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'shrubland')" class="searchsubcat">Shrubland,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'terrestrial')" class="searchsubcat">Terrestrial,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'tundra')" class="searchsubcat">Tundra,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'urban')" class="searchsubcat">Urban,</a>
              <a href="#" onClick="keywordSearch(document.searchForm, 'wetland')" class="searchsubcat">Wetland</a>
            </td>
          </tr>
          <tr> 
            <td width="375">&nbsp;</td>
            <td width="365">&nbsp;</td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
<!-- ************************* END SEARCHBOX TABLE ************************* -->
