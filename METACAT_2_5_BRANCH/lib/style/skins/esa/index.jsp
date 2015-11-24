<%@ page    language="java" %>
<%@ page import="edu.ucsb.nceas.metacat.properties.SkinPropertyService" %>

<%
/**
 *  '$RCSfile$'
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

 <%
	String esaHome = SkinPropertyService.getProperty("esa","registryurl");
 %>
 
 <%@ include file="../../common/common-settings.jsp"%>
 <%@ include file="../../common/configure-check.jsp"%>
 
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>ESA Data Registry</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/esa/esa.css"></link>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_SKINS_URL%>/esa/esa.js"></script>
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
                var checkBox = document.getElementById("searchCheckBox");
                var searchBox = document.getElementById("searchBox");
                var searchString = trim(searchBox.value);
                searchString = encodeXML(searchString);
                
                if (searchString=="") {
                        searchString="%";
                }

                if(checkBox.checked == false){
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
	                        abstractValue.value="";
	                        var keywordValue = document.getElementById("keywordValue");
	                        keywordValue.value="";
	                        var pid = document.getElementById("packageId");
        	                pid.value=searchString;
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
		                        var pid = document.getElementById("packageId");
	        	                pid.value="";
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
		                var pid = document.getElementById("packageId");
	        	        pid.value="";
		}

                var knbCheckBox = document.getElementById("knbCheckBox");
                if(knbCheckBox.checked == true){
                        document.forms[0].action = "http://knb.ecoinformatics.org/knb/servlet/metacat";
                        submitFormObj.qformat.value = "knb";
	                var pid = document.getElementById("packageId");
        	        pid.value="";
                }
                else {
                	document.forms[0].action = "<%=SERVLET_URL%>";
                	submitFormObj.qformat.value = "esa";
                }

                var actionField=document.createElement("input");
                actionField.setAttribute("type", "hidden");
                actionField.setAttribute("name", "action");
                actionField.setAttribute("value", "query");
                submitFormObj.appendChild(actionField);

                return true;
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

Welcome to the ESA Data Registry. This is a publicly accessible registry
describing <b>scientific data sets on ecology and the environment</b>.  The
data sets registered here are associated with articles published in the
journals of the Ecological Society of America.  They are registered here
in order to facilitate communication and data sharing by scientists.  See
individual registry entries for citation information as well as usage rights.

<br><br>
<tr><td colspan="5">
<b>Registry Tools</b>

    
<li>
<span class="searchbox"><a name="search">Search for Data Sets</a></span><br />
<ul><table width="760" border="0" cellspacing="0" cellpadding="0"><tr>
<td>

<form method="POST" action="<%=SERVLET_URL%>" target="_top" onSubmit="return checkSearch(this)">
  <input value="INTERSECT" name="operator" type="hidden">
  <input type="hidden" name="organizationName">
  <input type="hidden" name="surName">
  <input type="hidden" name="givenName">
  <input type="hidden" name="keyword">
  <input type="hidden" name="keyword/value" id="keywordValue">
  <input type="hidden" name="para">
  <input type="hidden" name="geographicDescription">
  <input type="hidden" name="literalLayout">
  <input type="hidden" name="@packageId" id="packageId">
  <input type="hidden" name="abstract/para" id="abstract">
  <input type="hidden" name="abstract/para/value" id="abstractValue">
  <input name="qformat" value="esa" type="hidden">
  <input name="returnfield" value="originator/individualName/surName" type="hidden">
  <input name="returnfield" value="originator/individualName/givenName" type="hidden">
  <input name="returnfield" value="creator/individualName/surName" type="hidden">
  <input name="returnfield" value="creator/individualName/givenName" type="hidden">
  <input name="returnfield" value="originator/organizationName" type="hidden">
  <input name="returnfield" value="creator/organizationName" type="hidden">
  <input name="returnfield" value="dataset/title" type="hidden">
  <input name="returnfield" value="dataset/title/value" type="hidden">
  <input name="returnfield" value="keyword" type="hidden">
  <input name="returnfield" value="keyword/value" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.1.1" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.1.0" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.0.1" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.0.0" type="hidden">
  <input name="returndoctype" value="-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN" type="hidden">
  <input name="returndoctype" value="-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN" type="hidden">
  <input name="returndoctype" value="-//NCEAS//resource//EN" type="hidden">
  <input name="returndoctype" value="-//NCEAS//eml-dataset//EN" type="hidden">
  &nbsp;<input size="14" name="anyfield" type="text" value="" id="searchBox">
  <input value="Search" type="submit">
</form></td>
<td><form>
<input name="search" type="radio" checked><span class="text_plain"> Search only within the ESA Data Registry</span></input><br>
<input name="search" type="radio" id="knbCheckBox"><span class="text_plain"> Search entire Knowledge Network for Biocomplexity</span></input>
</form></td>
</tr>
</table>
<form>
  <input name="searchAll" type="radio" checked><span >Search Title, Abstract, Keywords, Personnel (Quicker)</span></input><br>
  <input name="searchAll" type="radio" id="searchCheckBox"><span >Search all fields (Slower)</span></input>
</form>
  
      This tool allows you to search the registry for data 
      sets of interest. When you type text in the box and 
      click on the "Search" button, the search will only 
      be conducted within the title, author, abstract,
      and keyword fields. Checking the "Search All Fields" 
      box will search on these and all other existing 
      fields (this search will take more time). Checking 
      the "Search Knowledge Network for Biocomplexity" box 
      will allow you to search the Knowledge Network for 
      Biocomplexity (KNB) in addition to the ESA Data 
      Registry. The KNB is an international data repository 
      dedicated to facilitating ecological and environmental 
      research. Click <a href="http://knb.ecoinformatics.org/">
      here </a> for more information on the KNB.
     <br><br>
      You can use the '%' character as a wildcard in your 
      searches (e.g., '%biodiversity%' would locate any 
      phrase with the word biodiversity embedded within it).
      </ul>
  </li>
  <li><a href="<%=SERVLET_URL%>?action=query&amp;operator=INTERSECT&amp;anyfield=%25&amp;qformat=esa&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.1&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.0&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.1&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.0&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN&amp;returnfield=dataset/title&amp;returnfield=keyword&amp;returnfield=originator/individualName/surName&amp;returnfield=creator/individualName/surName&amp;returnfield=originator/organizationName&amp;returnfield=creator/organizationName">Browse data sets</a>    <ul>
      <p>Browse all existing data sets by title.  This operation can be slow as the
      number of entries in the registry grows.</p>
      
    </ul>
  </li>
  
  <li>
      <a href="map.jsp"> View Interactive Map </a> 
      <ul> <p> View and query the geographic coverages of the data sets. </p> </ul> 
  </li>
 
  <li><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=esa">Register a new 
       data set</a><br>
         <ul>
           <p>The ESA Data Registry form is for registering data sets associated with articles published in the journals of the Ecological Society of America. Other Ecological data sets can be registered with the Knowledge Network for Biocomplexity (<a href="http://knb.ecoinformatics.org">KNB</a>). 
           </p>
	   <br>
           <strong>Steps for registering an ESA data set</strong>
	    <br>
           <span class="greenbold">Step 1: Create an Account</span> <br>
           <p> <a href="<%=USER_MANAGEMENT_URL%>">Create an account</a> by registering with the <a href="http://knb.ecoinformatics.org">KNB</a>. Many scientists will already have accounts in the KNB, especially those 
from institutions like NCEAS and LTER. If you already have an account please use that existing account rather than creating a new one. </p>
           <span class="greenbold">Step 2: Login</span><br>
	   <p><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=esa&stage=loginform">Login to the ESA Registry</a> website with the account you created. Fill out the ESA Data Registry Form.</p>
           <span class="greenbold">Step 3: Register Data</span><br>
	  <p>Fill out the <a href="<%=CGI_URL%>/cgi-bin/register-dataset.cgi?cfg=esa">ESA Data Registry Form</a>. A page titled "Success" will appear when the form has been successfully submitted.             </p>
           <span class="greenbold">Step 4: Look for Feedback</span> <br>
<p>After you submit, watch for e-mail sent by the ESA moderator regarding whether your data set  has been accepted.</p>      </ul>
  
      
    <ul>
        Need an account? Forgot password? <a href="<%=USER_MANAGEMENT_URL%>">Click here</a>.
    </ul>
    <ul>
      The account management tools are used to create and manage registry 
      accounts.   Accounts are free, and are used to identify contributors
      so that they can maintain their entries in the future.  
    </ul>
  </li>
</ul>
</p>
</td>
</tr>
 
  <tr><td>
   <br>
   <b>About the Registry</b>
    <p>
    This project is a cooperative effort of the
    <a href="http://www.esa.org">Ecological Society of
    America</a> and the <a href="http://www.nceas.ucsb.edu">National
    Center for Ecological Analysis and Synthesis (NCEAS)</a>.  
    The Data Registry is based on software developed by
    the <a href="http://knb.ecoinformatics.org">Knowledge Network for
    Biocomplexity (KNB)</a>, and houses metadata that are compliant with <a
    href="http://knb.ecoinformatics.org/software/eml/">Ecological Metadata
    Language (EML)</a>.  Consequently, data found in this registry
    also are accessible from the larger collection of data found in the
    <a href="http://knb.ecoinformatics.org">Knowledge Network for 
    Biocomplexity (KNB)</a>.  
    </p>
  </td></tr>
</table>

<script language="JavaScript">          
    insertTemplateClosing("<%=CONTEXT_URL%>");
</script>
</body>
</html>
