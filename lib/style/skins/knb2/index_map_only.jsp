<%@ page    language="java" %>
<%
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
%>
<%@ include file="PORTAL_SETTINGS.jsp"%>
<%@ include file="include_session_vars.jsp"%>
<%@ page import="edu.ucsb.nceas.metacat.properties.PropertyService" %>

<%
///////////////////////////////////////////////////////////////////////////////
//
// NOTE:
//
//  GLOBAL CONSTANTS (SETTINGS SUCH AS METACAT URL, LDAP DOMAIN	AND DEBUG
//  SWITCH) ARE ALL IN THE INCLUDE FILE "PORTAL_SETTINGS.jsp"
//
///////////////////////////////////////////////////////////////////////////////
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>KNB :: The Knowledge Network for Biocomplexity</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link href="./portalpages.css" rel="stylesheet" type="text/css">
<script language="JavaScript" type="text/JavaScript" src="./portalpages.js"></script>
</head>

<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
<table width="750" border="0" align="center" cellpadding="0" cellspacing="0" class="maintable">
  <tr>
    <td width="10" height="21" align="right" valign="top" class="title">&nbsp;</td>
    <td width="80" rowspan="2" align="center" valign="bottom" class="title"><img src="./images/KNBLogo_top.gif"></td>
    <td width="650" rowspan="2" align="center" valign="middle" class="title"><img src="./images/knbTitleText.gif" width="510" height="23" border="0"></td>
    <td width="10" height="21" align="left" valign="top" class="title">&nbsp;</td>
  </tr>
  <tr>
    <td width="10" align="right" valign="top" class="title">&nbsp;</td>
    <td width="10" align="left" valign="top" class="title">&nbsp;</td>
  </tr>
  <tr>
    <td width="10" align="right" valign="top"><img src="./images/transparent1x1.gif" width="10" height="3"></td>
    <td width="80" rowspan="2" align="center" valign="top" class="sectionheader"><img src="./images/KNBLogo_bottom.gif" width="80" height="25"></td>
    <td colspan="2" align="left" valign="top"><img src="./images/transparent1x1.gif" width="660" height="3"><img src="./images/transparent1x1.gif" width="1" height="1"></td>
  </tr>
  <tr>
    <td width="10" align="right" valign="top" class="sectionheader">&nbsp;</td>
    <td colspan="2" align="center" valign="top" class="sectionheader">&nbsp;</td>
  </tr>
  <tr>
    <td colspan="4"> <table width="100%" border="0" align="center" cellpadding="0" cellspacing="0">
        <tr>
          <td> <table width="100%" border="0" cellspacing="0" cellpadding="0" class="text_plain">
              <tr>
                <td><p class="text_plain">The <b>Knowledge Network for Biocomplexity (KNB)</b> is a
                    national network intended to facilitate ecological and environmental
                    research on biocomplexity.
                    
                    For scientists, the KNB is an efficient way to discover, access,
                    interpret, integrate and analyze complex <b>ecological data</b>
                    from a highly-distributed set of field stations, laboratories,
                    research sites, and individual researchers.</p>

                </td>
              </tr>
            </table></td>
        </tr>

	<% if ( PropertyService.getProperty("spatial.runSpatialOption").equals("true") ) { %>
        <tr>
          <td class="text_plain"><%@ include file="include_map.jsp" %></td>
        </tr>
	<% } %>

        </tr>
        <tr>
          <td align="right" valign="top"><img src="images/transparent1x1.gif" width="200" height="5"></td>
        </tr>
        <tr>
          <td class="text_plain"><table width="100%" border="0" cellspacing="0" cellpadding="0">
              <tr> 
                <td width="10" align="right" valign="top"><img src="images/panelhead_bg_lcorner.gif" width="10" height="21"></td>
                <td width="720" class="sectionheader">about the KNB project</td>
                <td width="10" align="left" valign="top"><img src="images/panelhead_bg_rcorner.gif" width="10" height="21"></td>
              </tr>
              <tr> 
                <td colspan="3"><table width="740" border="0" cellpadding="2" cellspacing="0" class="subpanel">
                    <tr align="center" valign="middle"> 
                      <td height="36" colspan="4" align="left" class="text_plain_smaller"><a href="<%=knbSiteUrl%>/home.html">The 
                        KNB Project</a> focuses on research into <a href="<%=knbSiteUrl%>/informatics">informatics</a> 
                        and <a href="<%=knbSiteUrl%>/biodiversity">biocomplexity</a>, through the 
                        development of <a href="<%=knbSiteUrl%>/software">software products</a> and 
                        by providing <a href="<%=knbSiteUrl%>/education">education, outreach and training</a> 
                      </td>
                    </tr>
                    <tr align="center" valign="middle"> 
                      <td colspan="2" align="left">&nbsp;</td>
                      <td colspan="2" align="right" class="text_plain_smaller"><a href="<%=knbSiteUrl%>/home.html">&gt;&gt; 
                        more information about the KNB Project... &gt;&gt;</a></td>
                    </tr>
                    <tr align="center" valign="middle"> 
                      <td colspan="2" align="left" class="text_plain_smaller">Sponsored 
                        and developed by:<br></td>
                      <td colspan="2" align="right">&nbsp;</td>
                    </tr>
                    <tr align="center" valign="middle"> 
                      <td width="25%"><a href="http://www.nceas.ucsb.edu" target="_blank"><img src="images/NCEASlogo_sm.gif" width="54" height="54" border="0"></a></td>
                      <td width="25%"><a href="http://www.ttu.edu" target="_blank"><img
 src="images/TTUlogo_sm.gif" alt="TTU Logo" width="43" height="43" border="0" align="middle"></a></td>
                      <td width="25%"><a href="http://www.lternet.edu" target="_blank"><img
 src="images/LTERlogo_sm.gif" alt="LTER Logo" width="40" height="40" border="0" align="middle"></a></td>
                      <td width="25%"><a href="http://www.sdsc.edu" target="_blank"><img src="images/SDSClogo_sm.gif" width="98" height="37" border="0"></a></td>
                    </tr>
                    <tr align="center" valign="middle"> 
                      <td width="25%"><a
 href="http://www.nceas.ucsb.edu" target="_blank" class="text_plain_smaller">National 
                        Center for Ecological Analysis and Synthesis</a> </td>
                      <td width="25%"><a href="http://www.ttu.edu" target="_blank"
 class="text_plain_smaller">Texas Tech University</a> </td>
                      <td width="25%"><a href="http://www.lternet.edu" target="_blank"
 class="text_plain_smaller">Long Term Ecological Research Network</a> </td>
                      <td width="25%"><a href="http://www.sdsc.edu" target="_blank"
 class="text_plain_smaller">San Diego Supercomputer Center</a> </td>
                    </tr>
                    <tr> 
                      <td align="left" valign="top" class="text_example" colspan="4"><img src="./images/transparent1x1.gif" width="200" height="10"></td>
                    </tr>
                    <tr> 
                      <td align="left" valign="top" class="text_example" colspan="4"><a
 href="http://www.nsf.gov" target="_blank">National Science Foundation</a> Knowledge 
                        and Distributed Intelligence Program</td>
                    </tr>
                    <tr> 
                      <td align="left" valign="top" class="text_example" colspan="4">This 
                        material is based upon work supported by the National Science 
                        Foundation under Grant No. DEB99&#8211;80154. Any opinions, 
                        findings and conclusions or recomendations expressed in this 
                        material are those of the author(s) and do not necessarily 
                        reflect the views of the National Science Foundation (NSF). 
                      </td>
                    </tr>
                  </table> </td>
              </tr>
            </table></td>
        </tr>
      </table></td>
  </tr>
</table>
</td>
  </tr>
</table>
</body>
</html>
