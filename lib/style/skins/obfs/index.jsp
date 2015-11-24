<%@ page    language="java" %>
<%
/**
 *  '$RCSfile$'
 *      Authors: Matt Jones
 *    Copyright: 2008 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 * 
 * This is an HTML document for displaying metadata catalog tools
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
 */
%>

<%@ include file="../../common/common-settings.jsp"%>
<%@ include file="../../common/configure-check.jsp"%>
  
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>OBFS Data Registry</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/obfs/obfs.css"></link>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_SKINS_URL%>/obfs/obfs.js"></script>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_COMMON_URL%>/branding.js"></script>
  <script language="Javascript">

		function encodeXML(theString) {
			return theString.replace(/&/g, '&amp;')
				.replace(/</g, '&lt;')
				.replace(/>/g, '&gt;')
				.replace(/"/g, '&quot;');
		}
		
        function trim(stringToTrim) {
                return stringToTrim.replace(/^\s*/, '').replace(/\s*$/,'');
        }

        function checkSearch(submitFormObj) {
        	var searchString = trim(submitFormObj.searchstring.value);
    		searchString = encodeXML(searchString);
			var checkBox = document.getElementById("searchAll");

                if (searchString=="") {
                       if (confirm("Show *all* data?")) {
            			searchString = "%";
          		} else {
            			return false;
          		}
                }

                if(!checkBox.checked && searchString!="%"){
                        submitFormObj.query.value = "<pathquery version=\"1.2\">"
                                                           +"<querytitle>Web-Search</querytitle>"
                                                           +"<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>"
                                                           +"<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>"
                                                           +"<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>"
                                                           +"<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>"
                                                           +"<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype>"
                                                           +"<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN</returndoctype>"
                                                           +"<returndoctype>-//NCEAS//resource//EN</returndoctype>"
                                                           +"<returndoctype>-//NCEAS//eml-dataset//EN</returndoctype>"
                                                           +"<returnfield>originator/individualName/surName</returnfield>"
                                                           +"<returnfield>originator/individualName/givenName</returnfield>"
                                                           +"<returnfield>creator/individualName/surName</returnfield>"
                                                           +"<returnfield>creator/individualName/givenName</returnfield>"
                                                           +"<returnfield>originator/organizationName</returnfield>"
                                                           +"<returnfield>creator/organizationName</returnfield>"
                                                           +"<returnfield>dataset/title/value</returnfield>"
                                                           +"<returnfield>dataset/title</returnfield>"
                                                           +"<returnfield>keyword</returnfield>"
                                                           +"<returnfield>keyword/value</returnfield>"
                                                           +"<querygroup operator=\"INTERSECT\">"
                                                                +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                        +"<value>Organization of Biological Field Stations</value>"
                                                                        +"<pathexpr>organizationName</pathexpr>"
                                                                +"</queryterm>"
                                                                +"<querygroup operator=\"UNION\">"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>surName</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>givenName</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>keyword</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>keyword/value</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
																				+"<pathexpr>para</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>geographicDescription</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>literalLayout</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>dataset/title</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>dataset/title/value</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>@packageId</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>abstract/para</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                                +"<value>" + searchString + "</value>"
                                                                                +"<pathexpr>abstract/para/value</pathexpr>"
                                                                        +"</queryterm>"
                                                                +"</querygroup>"
                                                          +"</querygroup>"
                                                  +"</pathquery>";
                } else {
                        queryTermString = "";
                        if(searchString != "%"){
                                queryTermString = "<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                        +"<value>" + searchString + "</value>"
                                                 +"</queryterm>";
                        }
                        submitFormObj.query.value = "<pathquery version=\"1.2\">"
                                                           +"<querytitle>Web-Search</querytitle>"
                                                           +"<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>"
                                                           +"<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>"
                                                           +"<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>"
                                                           +"<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>"
                                                           +"<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype>"
                                                           +"<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN</returndoctype>"
                                                           +"<returndoctype>-//NCEAS//resource//EN</returndoctype>"
                                                           +"<returndoctype>-//NCEAS//eml-dataset//EN</returndoctype>"
                                                           +"<returnfield>originator/individualName/surName</returnfield>"
                                                           +"<returnfield>originator/individualName/givenName</returnfield>"
                                                           +"<returnfield>creator/individualName/surName</returnfield>"
                                                           +"<returnfield>creator/individualName/givenName</returnfield>"
                                                           +"<returnfield>originator/organizationName</returnfield>"
                                                           +"<returnfield>creator/organizationName</returnfield>"
                                                           +"<returnfield>dataset/title</returnfield>"
                                                           +"<returnfield>dataset/title/value</returnfield>"
                                                           +"<returnfield>keyword</returnfield>"
                                                           +"<returnfield>keyword/value</returnfield>"
                                                           +"<querygroup operator=\"INTERSECT\">"
                                                                +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                                                        +"<value>Organization of Biological Field Stations</value>"
                                                                        +"<pathexpr>organizationName</pathexpr>"
                                                                +"</queryterm>"
                                                                + queryTermString
                                                           +"</querygroup>"
                                                    +"</pathquery>";

                }
                return true;
        }

        function searchAll(){
                var checkBox = document.getElementById("searchCheckBox");
                if(checkBox.checked == true){
                        alert("You have selected to search all possible existing fields. This search will take longer.");
                }
        }

  </script>
</head>
<body>
      <script language="JavaScript">
          insertTemplateOpening("<%=CONTEXT_URL%>");
          insertSearchBox("<%=CONTEXT_URL%>");
      </script>
<table width="760" border="0" cellspacing="0" cellpadding="0">
  <tr><td colspan="5">
<p>
Welcome to the OBFS Data Registry. This is the primary source for
comprehensive information about scientific and research data sets collected
within or under the auspices of the Organization of Biological Field Stations.
</p>
</td></tr>
<tr><td colspan="5">
<p>This project is a cooperative effort of <a href="http://www.obfs.org">OBFS</a>, 
the <a href="http://www.nceas.ucsb.edu">National Center for 
Ecological Analysis and Synthesis (NCEAS)</a>, the 
<a href="http://nrs.ucop.edu">UC Natural Reserve System</a>, and the 
<a href="http://www.lternet.edu">LTER Network Office</a>.
The Data Registry is based on software developed by the
<a href="http://knb.ecoinformatics.org">Knowledge Network for 
Biocomplexity (KNB)</a>, and
houses metadata that are compliant with 
<a href="http://knb.ecoinformatics.org/software/eml/">Ecological Metadata 
Language (EML)</a>.
</p>
</td></tr>
<tr><td colspan="5">
<p>
Credit for the data sets in this registry goes to the investigators who
collected the data, and also to the OBFS sites and system for providing 
an effective and pleasant environment for research and education at 
the individual research stations.
Our particular thanks go out to the OBFS reserve managers, scientists,
and stewards for their comments and continuing support.
</p>
</td></tr>
<tr><td colspan="5">
<p><b>Registry Tools</b></p>
<p>
<menu>
<li><span class="searchbox"><a name="search"> Search for Data Sets</a></span><br />
    <menu>
<form method="POST" action="<%=SERVLET_URL%>" target="_top" onSubmit="return checkSearch(this)">
  <input value="UNION" name="operator" type="hidden">
  &nbsp;<input size="14" name="searchstring" type="text" value="" id="searchBox">
  <input name="query" type="hidden"/>
  <input name="qformat" value="obfs" type="hidden">
  <input name="enableediting" value="true" type="hidden">
  <input type="hidden" name="action" value="squery">
  <input value="Search" type="submit">
</form>
<form>
  <input name="search" type="radio" checked><span class="text_plain"> Search Title, Abstract, Keywords, Personnel (Quicker)</span></input><br>
  <input name="search" type="radio" id="searchAll"><span class="text_plain"> Search all fields (Slower)</span></input><br>
</form>
   
      This tool allows you to search the registry for data
      sets of interest. When you type text in the box and
      click on the "Search" button, the search will only
      be conducted within the title, author, abstract,
      and keyword fields. Checking the "Search All Fields"
      box will search on these and all other existing
      fields (this search will take more time).
     <br><br>
      You can use the '%' character as a wildcard in your
      searches (e.g., '%biodiversity%' would locate any
      phrase with the word biodiversity embedded within it).
      </menu>
    <br><br>
  </li>

  <li><a href="<%=SERVLET_URL%>?action=query&amp;operator=INTERSECT&amp;anyfield=%25&amp;organizationName=Organization%20of%20Biological%20Field%20Stations&amp;qformat=obfs&amp;enableediting=true&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.1&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.0&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.1&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.0&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN&amp;returnfield=dataset/title&amp;returnfield=keyword&amp;returnfield=originator/individualName/surName&amp;returnfield=creator/individualName/surName&amp;returnfield=originator/organizationName&amp;returnfield=creator/organizationName">Browse existing OBFS data sets</a><br />
    <menu>
      The registry search system is used to locate data sets of interest
      by searching through existing registered data sets.  
      Presently the search covers all fields, including
      author, title, abstract, keywords, and other documentation
      for each dataset.  (More sophisticated search capabilities, 
      including boolean field searches, will be available in future.)
    </menu>
  </li>
  <p>&nbsp;</p>
  <li><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=obfs">Register a new OBFS 
       data set</a><br />
    <menu>
      The registration page is used to submit information about a <b>new</b>
      data set associated with OBFS research.  The documentation about the
      data set will be reviewed and then submitted to the Registry.
    </menu>
  </li>
  <p>&nbsp;</p>
  <li>
      <a href="map.jsp"> View Interactive Map </a>
      <menu> View and query the geographic coverages of the data sets. </menu>
  </li>

</menu>
</p>
  </td></tr>
</table>
<p>&nbsp;</p>
<script language="JavaScript">          
    insertTemplateClosing("<%=CONTEXT_URL%>");
</script>
</body>
</html>
