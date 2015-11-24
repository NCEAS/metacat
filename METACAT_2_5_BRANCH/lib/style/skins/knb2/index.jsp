<%@ page    language="java"  import="java.util.Vector,edu.ucsb.nceas.metacat.util.OrganizationUtil"%>
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
	Vector<String> organizationList = OrganizationUtil.getOrganizations();
%>

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

			<div><span id="newsHeader">News</span>
			<ul>
		         <li>We have added a list of <a href="community.jsp">sites using KNB software</a>. </li>
			 <li> We are now <a href="callfordata.jsp">accepting nominations</a> for the inclusion of particularly valuable ecological
			data sets within the KNB. </li>
			 <li> Slides are available for the <a href="knbworkshop2.jsp">Second KNB Data Management
			Tools Workshop</a>, which was held February 2-4, 2005. </li>     
			 <li> Slides are available for the <a href="knbworkshop.jsp">First KNB Data Management
			Tools Workshop</a>, which was held September 28-30, 2004. </li>
			</ul>
			</div>

                </td>
              </tr>
            </table></td>
        </tr>
        <tr>
          <td class="loginStatus"><img src="images/transparent1x1.gif" width="200" height="5"></td>
        </tr>
        <tr>
          <td class="text_plain"><%@ include file="include_searchbox.jsp" %></td>
        </tr>

	<% if ( PropertyService.getProperty("spatial.runSpatialOption").equals("true") ) { %>
        <tr>
          <td class="text_plain"><%@ include file="include_map.jsp" %></td>
        </tr>
	<% } %>

        <tr>
          <td class="loginStatus"><img src="images/transparent1x1.gif" width="200" height="5"></td>
        </tr>
        <tr>
          <td class="text_plain"><%@ include file="include_browse.jsp" %></td>
        </tr>

        <tr>
          <td><img src="images/transparent1x1.gif" width="200" height="5"></td>
        </tr>
        <tr>
          <td class="text_plain"><table width="100%" border="0" cellspacing="0" cellpadding="0">
              <tr>
                <td width="49%" align="center" valign="top"> <table width="365" border="0" cellspacing="0" cellpadding="0">
                    <tr>
                      <td width="10"><img src="./images/panelhead_bg_lcorner.gif" width="10" height="21"></td>
                      <td width="344" class="sectionheader">login &amp; registration</td>
                      <td width="10"><img src="./images/panelhead_bg_rcorner.gif" width="10" height="21"></td>
                    </tr>
                    <tr>
                      <td height="246" colspan="3" valign="top"> <table width="364" border="0" cellpadding="0" cellspacing="0" class="subpanel">
                          <tr>
                            <td width="1" rowspan="9"><img src="./images/transparent1x1.gif" width="1" height="245"></td>
                            <td colspan="2" class="text_plain"><a name="loginanchor"></a>Logging
                              into your account enables you to search any additional,
                              non-public data for which you may have access priviliges:<br> </br>
                              <%= loginStatus %></td>
                            <td width="105" rowspan="3" align="right" valign="top"  style="padding-top: 5px; padding-right: 5px;"><img src="./images/pen.jpg" width="100" height="100"></td>
                          </tr>
                          <form name="loginform" method="post" action="index.jsp" target="_top" onSubmit="return allowSubmit(this);">
                            <input type="hidden" name="action"  value="login">
                            <input type="hidden" name="ldapusername"  value="">
                            <input type="hidden" name="qformat" value="knb">
                            <input type="hidden" name="enableediting" value="true">
                            <tr valign="middle">
                              <td align="left" valign="middle" class="text_plain">username:</td>
                              <td width="173" align="left" class="text_plain" style="padding-top: 2px; padding-bottom: 2px;">
                                <input name="username" type="text" style="width: 140px;" value="<%=typedUserName%>" <%=loginEnabledDisabled%>></td>
                            </tr>
                            <tr valign="middle">
                              <td height="28" align="left" valign="middle" class="text_plain">organization:</td>
                              <td align="left" class="text_plain" style="padding-top: 2px; padding-bottom: 2px;">
                                <select name="organization" style="width:140px;" <%=loginEnabledDisabled%>>
                                  <option value=""       <%=((posted_organization.length()<1)?                 "selected":"")%>>&#8212;
                                  choose one &#8212;</option>
<%
							for (String orgName : organizationList) {
%> 
                                  <option value="<%= orgName %>"        <%=((posted_organization.equalsIgnoreCase(orgName))?        "selected":"")%>><%= orgName %></option>
<%
							}
%> 
                                </select></td>
                            </tr>
                            <tr valign="middle">
                              <td width="85" align="left" valign="middle" class="text_plain">password:</td>
                              <td colspan="2" align="left" class="text_plain" style="padding-top: 2px; padding-bottom: 2px;">
                                <table width="100%" border="0" cellpadding="0" cellspacing="0">
                                  <tr>
                                    <td width="150" align="left"> <input name="password" type="password" maxlength="50" style="width:140px;"
                                      value="<%=posted_password%>" <%=loginEnabledDisabled%>></td>
                                    <td align="center"class="<%= ((isLoggedIn)? "buttonBG_logout": "buttonBG_login") %>"><input                                      type="submit" name="loginAction" value="<%=loginButtonLabel%>" class="button_login" /></td>
                                    <td align="left">&nbsp;</td>
                                  </tr>
                                </table></td>
                            </tr>
                          </form>
                          <tr valign="middle">
                            <td colspan="2"><img src="./images/transparent1x1.gif" width="20" height="10"></td>
                            <td><img src="./images/transparent1x1.gif" width="10" height="5"></td>
                          </tr>
                          <tr valign="middle">
                            <td height="20">&nbsp;</td>
                            <td height="20" align="left"><a href="<%=USER_MANAGEMENT_URL%>" target="_blank">Need an account? Forgot password?</a></td>
                            <td height="20">&nbsp;</td>
                          </tr>
                          <tr valign="middle">
                            <td colspan="3"><img src="./images/transparent1x1.gif" width="20" height="2"></td>
                          </tr>
                      </table></td>
                    </tr>
                  </table></td>
                <td width="10">&nbsp;</td>
                <td width="49%" align="center" valign="top"> <table width="365" border="0" cellpadding="0" cellspacing="0">
                    <tr>
                      <td width="10"><img src="./images/panelhead_bg_lcorner.gif" width="10" height="21"></td>
                      <td width="344" class="sectionheader">data management</td>
                      <td width="10"><img src="./images/panelhead_bg_rcorner.gif" width="10" height="21"></td>
                    </tr>
                    <tr>
                      <td colspan="3"><table width="364" border="0" cellpadding="0" cellspacing="0" class="subpanel">
                          <tr valign="top">
                            <td width="1" rowspan="8"><img src="./images/transparent1x1.gif" width="1" height="245"></td>
                            <td valign="top"> <table width="100%" border="0" cellpadding="0" cellspacing="0">
                                <tr class="text_plain"> 
                                  <td colspan="2" valign="bottom" class="text_plain"><b>Online 
                                    Data Registry</b><br> </td>
                                  <td width="105" rowspan="3" align="right" valign="top" style="padding-top: 5px; padding-right: 5px;"><img src="images/MorphoButterfly.jpg" width="100" height="100"></td>
                                </tr>
                                <tr class="text_plain"> 
                                  <td width="22" class="text_plain" valign="top"><img src="./images/blue_dots.gif" width="8" height="10"></td>
                                  <td width="234" class="text_plain" valign="top"><%
																	/***************
																	NOT YET USED
																	String registerAnchor = (isLoggedIn)?
																	  "<a href=\"/cgi-bin/register-dataset.cgi?cfg=knb\">" : 
																		 "<a href=\"#\" onclick=\"javascript:alert('You must be logged in to use the online registry!')\">";
																		
																		***********/
																		%> <a href="/cgi-bin/register-dataset.cgi?cfg=knb<%=((sess_sessionId!=null)? "&sessionid="+sess_sessionId: "")%>">Register 
                                    your dataset online</a></td>
                                </tr>
                                <tr class="text_plain"> 
                                  <td colspan="2" class="text_plain" valign="bottom"><b>Morpho</b> 
                                    is easy-to-use data management software. Use it 
                                    to:</td>
                                </tr>
                                <tr valign="baseline" class="text_plain"> 
                                  <td class="text_plain"><img src="./images/blue_dots.gif" width="8" height="10"></td>
                                  <td class="text_plain" colspan="2">query, view, 
                                    retrieve and manipulate ecological data from the 
                                    KNB network</td>
                                </tr>
                                <tr valign="baseline" class="text_plain"> 
                                  <td class="text_plain"><img src="./images/blue_dots.gif" width="8" height="10"></td>
                                  <td class="text_plain" colspan="2">create, view 
                                    and manipulate your own datasets, and specify 
                                    access control to manage their availability </td>
                                </tr>
                                <tr class="text_plain"> 
                                  <td colspan="3" align="center" style="height: 20px"> <p><a href="<%=knbSiteUrl%>/morphoportal.jsp" target="_self">Morpho: 
                                      more information and downloads</a></p></td>
                                </tr>
                                <tr class="text_plain"> 
                                  <td colspan="3" style="height: 40px" align="center"> <p><b style="margin-top: 15px">Quick 
                                      Download for:</b><br>
                                      <a href="<%=MORPHO_DOWNLOAD_LINK_WINDOWS%>">Windows</a>&nbsp;&nbsp;::&nbsp;&nbsp;<a href="<%=MORPHO_DOWNLOAD_LINK_MACOSX%>">Mac 
                                      OS X</a>&nbsp;&nbsp;::&nbsp;&nbsp;<a href="<%=MORPHO_DOWNLOAD_LINK_LINUX%>">Linux</a></p></td>
                                </tr>
                              </table></td>
                          </tr>
                        </table></td>
                    </tr>
                  </table></td>
              </tr>
            </table></td>
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
