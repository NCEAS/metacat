<%@ page    language="java" %>
<%
/**
 *  '$RCSfile$'
 *    Copyright: 2004 Regents of the University of California and the
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
<title>LTSS Data Registry</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/ltss/ltss.css"></link>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_SKINS_URL%>/ltss/ltss.js"></script>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_COMMON_URL%>/branding.js"></script>
</head>
<body>
      <script language="JavaScript">
          insertTemplateOpening("<%=CONTEXT_URL%>");
          insertSearchBox("<%=CONTEXT_URL%>");
      </script>
<table width="760" border="0" cellspacing="0" cellpadding="0">
  <tr><td colspan="5">
<p>
Welcome to the Long-term Studies Section Data Registry. This is a publically 
accessible registry describing <b>scientific data sets on ecology and the
environment</b>.  In this context, we refer to a data set as the collection
of data tables and objects that are associated with a study, experiment,
or monitoring program.  Exactly what constitutes a data set is determined
by each submitter.  Credit for the datasets in this registry goes to the
investigators who collected the data (see each individual registry entry
for citation and attribution information as well as usage rights).
</p>
</td></tr>
<tr><td colspan="5">
<p><b>Registry Tools</b></p>
<p>
<menu>
<li><span class="searchbox">Search for Data</span><br />
    <menu>
<form method="POST" action="<%=SERVLET_URL%>" target="_top">
  <input value="INTERSECT" name="operator" type="hidden">   
  <input size="14" name="anyfield" type="text" value="">
  <!-- <input name="organizationName" value="Long-term Studies Section" type="hidden">-->
  <input name="action" value="query" type="hidden">
  <input name="qformat" value="ltss" type="hidden">
  <input name="enableediting" value="true" type="hidden">
  <input name="operator" value="UNION" type="hidden">
  <input name="returnfield" value="originator/individualName/surName" type="hidden">
  <input name="returnfield" value="originator/individualName/givenName" type="hidden">
  <input name="returnfield" value="creator/individualName/surName" type="hidden">
  <input name="returnfield" value="creator/individualName/givenName" type="hidden">
  <input name="returnfield" value="originator/organizationName" type="hidden">
  <input name="returnfield" value="creator/organizationName" type="hidden">
  <input name="returnfield" value="dataset/title" type="hidden">
  <input name="returnfield" value="keyword" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.1.1" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.1.0" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.0.1" type="hidden">
  <input name="returndoctype" value="eml://ecoinformatics.org/eml-2.0.0" type="hidden">
  <input name="returndoctype" value="-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN" type="hidden">
  <input name="returndoctype" value="-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN" type="hidden">
  <input name="returndoctype" value="-//NCEAS//resource//EN" type="hidden">
  <input name="returndoctype" value="-//NCEAS//eml-dataset//EN" type="hidden">
  <input value="Search" type="submit">
</form>
      The registry search system is used to locate data sets of interest
      by searching through existing registered data sets.  
      Presently the search covers all fields, including
      author, title, abstract, keywords, and other documentation
      for each dataset.  Use a '%' symbol as a wildcard in searches
      (e.g., '%biodiversity%' would locate any phrase with the word
      biodiversity embedded within it).
      </menu>
  </li>
  <li><a href="<%=SERVLET_URL%>?action=query&amp;operator=INTERSECT&amp;anyfield=%25&amp;qformat=ltss&amp;enableediting=true&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.1&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.0&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.1&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.0&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN&amp;returnfield=dataset/title&amp;returnfield=keyword&amp;returnfield=originator/individualName/surName&amp;returnfield=creator/individualName/surName&amp;returnfield=originator/organizationName&amp;returnfield=creator/organizationName">Browse data sets</a><br />
    <menu>
    Browse all existing data sets by title.  This operation can be slow as the
    number of entries in the registry grows.
    </menu>
  </li>
  <p>&nbsp;</p>
  <li><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=ltss">Register a new 
       data set</a><br />
    <menu>
      The registration page is used to submit information about a <b>new</b>
      data set regarding ecology or the environment.  The documentation about 
      the data set will be submitted to the Registry.  You (and only
      you, or your designee) can return to edit the entry
      at a later point in time to add to or correct the
      description.  You can also use other tools such as <a
      href="http://knb.ecoinformatics.org/morphoportal.jsp">Morpho</a> to
      further document the set of data and directly attach the data to the
      registry entry.
    </menu>
  </li>
  <p>&nbsp;</p>
  
  
  <li><a href="<%=USER_MANAGEMENT_URL%>">Need an account? Forgot password?</a>
    <br />
    <menu>
      The account management tools are used to create and manage registry 
      accounts.   Accounts are free, and are used to identify contributors
      so that they can maintain their entries in the future.  
    </menu>
  </li>
</menu>
</p>
  </td></tr>
  <tr><td>
    <p>
    This project is a cooperative effort of the <a
    href="http://www.esa.org/longterm/">Long-Term Studies Section (LTSS)</a>
    of the ESA, the <a href="http://www.nceas.ucsb.edu">National
    Center for Ecological Analysis and Synthesis (NCEAS)</a> and the <a
    href="http://www.lternet.edu">Long-Term Ecological Research Network
    (LTER)</a>.  The Data Registry is based on software developed by
    the <a href="http://knb.ecoinformatics.org">Knowledge Network for
    Biocomplexity (KNB)</a>, and houses metadata that are compliant with <a
    href="http://knb.ecoinformatics.org/software/eml/">Ecological Metadata
    Language (EML)</a>.  Consequently, data registered in this registry
    also are accessible from the larger collection of data found in the
    <a href="http://knb.ecoinformatics.org">KNB registry</a>.  Other sites
    contributing to the KNB registry include:
    <ul>
    <li><a href="http://knb.ecoinformatics.org/knb/obfs">Organization of
    Biological Field Stations registry</a>  </li>
    <li><a href="http://knb.ecoinformatics.org/knb/style/skins/nrs">UC
    Natural Reserve System registry</a>  </li>
    <li><a href="http://knb.ecoinformatics.org/knb/style/skins/nceas">NCEAS
    registry</a>  </li>
    <li><a href="http://knb.ecoinformatics.org/knb/style/skins/specnet">
    SpecNet registry</a>  </li>
    </ul>
    </p>
  </td></tr>
</table>
<p>&nbsp;</p>
<p>&nbsp;</p>
<script language="JavaScript">          
    insertTemplateClosing("<%=CONTEXT_URL%>");
</script>
</body>
</html>
