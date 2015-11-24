<%@ page    language="java" %>
<%
/**
 *  '$RCSfile$'
 *      Authors: Matt Jones
 *    Copyright: 2000 Regents of the University of California and the
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
<title>SpecNet Data Registry</title>
  <link rel="stylesheet" type="text/css" 
        href="<%=STYLE_SKINS_URL%>/specnet/specnet.css"></link>
  <script language="JavaScript" type="text/JavaScript"
          src="<%=STYLE_SKINS_URL%>/specnet/specnet.js"></script>
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
Welcome to the SpecNet Data Registry. This is the primary source for
comprehensive information about scientific and research data sets collected
within or under the auspices of the SpecNet.
</p>
<p>&nbsp;</p>
<p>
This project is a cooperative effort of <a href="http://www.specnet.info">SpecNet</a>, 
the <a href="http://www.nceas.ucsb.edu">National Center for 
Ecological Analysis and Synthesis (NCEAS)</a> and the 
<a href="http://cea-crest.calstatela.edu">Center for Environmental Analysis 
(CEA-CREST)</a>. The Data Registry is based on software developed by the
<a href="http://knb.ecoinformatics.org">Knowledge Network for 
Biocomplexity (KNB)</a>, and
houses metadata that are compliant with 
<a href="http://knb.ecoinformatics.org/software/eml/">Ecological Metadata 
Language (EML)</a>.</p>
<p>&nbsp;</p>
<p>
Credit for the datasets in this registry goes to the investigators who
collected and processed the data.Our particular thanks go out to the SpecNet scientists,
and NCEAS staff for their comments and continuing support.
</p>
<p>&nbsp;</p>
<p><b>Registry Tools</b></p>
<p>
<menu>
  <li><a href="<%=SERVLET_URL%>?action=query&amp;operator=INTERSECT&amp;anyfield=%25&amp;organizationName=SpecNet&amp;qformat=specnet&amp;enableediting=true&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.1&amp;returndoctype=eml://ecoinformatics.org/eml-2.1.0&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.1&amp;returndoctype=eml://ecoinformatics.org/eml-2.0.0&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN&amp;returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN&amp;returnfield=dataset/title&amp;returnfield=keyword&amp;returnfield=originator/individualName/surName&amp;returnfield=creator/individualName/surName&amp;returnfield=originator/organizationName&amp;returnfield=creator/organizationName">Browse existing SpecNet data sets</a><br />
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
  <li><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=specnet">Register a new SpecNet 
       data set</a><br />
    <menu>
      The registration page is used to submit information about a <b>new</b>
      data set associated with SpecNet research.  The documentation about the
      data set will be reviewed and then submitted to the Registry.
    </menu>
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
