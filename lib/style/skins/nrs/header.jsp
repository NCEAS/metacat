<%@ page    language="java" %>
<%
/**
 * '$RCSfile$'
 * Copyright: 2003 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 * '$Author$'
 * '$Date$'
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
 */
 %>

 <%@ include file="../../common/common-settings.jsp"%>
 <%@ include file="../../common/configure-check.jsp"%>

<!--____________________________max_width____________________________________-->

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
  <title>UCNRS Data Registry</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/nrs/nrs.css"></link>
  <script language="Javascript">
        function trim(stringToTrim) {
                return stringToTrim.replace(/^\s*/, '').replace(/\s*$/,'');
        }

        function checkSearch(submitFormObj) {
                var searchString = trim(submitFormObj.searchstring.value);
                if (searchString=="") {
                        searchString="%";
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
                                                                        +"<value>University of California Natural Reserve System</value>"
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
                                                                                +"<pathexpr>title</pathexpr>"
                                                                        +"</queryterm>"
                                                                        +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
	                                                                        +"<value>" + searchString + "</value>"
	                                                                        +"<pathexpr>title/value</pathexpr>"
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
                return true;
        }
  </script>
</head>

<body>
<table width="760" class="headertopmargin" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td valign="middle" colspan="4" class="title">UC Natural Reserve System
    Data Registry</td>
    <td rowspan="3" width="20%" valign="top"> 
      <div align="right"><img src="<%=STYLE_SKINS_URL%>/nrs/nrs-logo.gif"></div>
    </td>
  </tr>
  <tr> 
    <td class="spacerrow" valign="top" colspan="4">&nbsp;</td>
  </tr>
  <tr> 
    <td valign="top" width="20%"> 
      <p><a href="http://www.ucnrs.org" target="_top"> NRS Home</a></p>
    </td>
    <td valign="top" width="20%"> 
      <p><a href="<%=STYLE_SKINS_URL%>/nrs/index.jsp" target="_top">Registry Home</a></p>
    </td>
    <td valign="top" width="20%"> 
      <p><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=nrs"
			target="_top">Register a<br>
		New Data Set</a></p>
    </td>
    <td valign="top" width="20%"> 
<p class="searchbox">
Search for Data<br />
<form method="POST" action="<%=SERVLET_URL%>" target="_top" onSubmit="return checkSearch(this)">
  <input value="INTERSECT" name="operator" type="hidden">   
  <input size="14" name="searchstring" type="text" value="">
  <input name="query" type="hidden"/>
  <input name="qformat" value="nrs" type="hidden">
  <input name="enableediting" value="true" type="hidden">
  <input type="hidden" name="action" value="squery">
  <!-- <input value="Start Search" type="submit"> -->
</form>
</p>
    </td>
  </tr>
</table>
</body>
</html>
