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
<!-- *********************** START SEARCHBOX TABLE ************************* -->
<table width="740" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td width="10" align="right" valign="top"><img src="images/panelhead_bg_lcorner.gif" width="10" height="21"></td>
    <td width="720" class="sectionheader">search for data on the 
      KNB</td>
    <td width="10" align="left" valign="top"><img src="images/panelhead_bg_rcorner.gif" width="10" height="21"></td>
  </tr>
  <tr> 
    <td colspan="3"> <table width="740" border="0" cellpadding="0" cellspacing="0" class="subpanel">
        <tr> 
          <td colspan="2"></td>
        </tr>
        <tr valign="baseline"> 
          <td colspan="2"><form action="<%=SERVLET_URL%>" name="searchForm"
                   method="post" target="_top" onSubmit="return allowSearch(this);">
              <%=sessionidField%> <%=SIMPLE_SEARCH_METACAT_POST_FIELDS%> 
              <table width="100%" border="0" cellpadding="5" cellspacing="0">
                <tr> 
                  <td width="94" rowspan="2" align="left" valign="top"><img src="images/search.jpg" width="94" height="80"></td>
                  <td colspan="2" valign="middle" class="text_example"><p> <%= loginStatus %>&nbsp&nbsp;(<a href="index.jsp#loginanchor" target="_top"><%=loginButtonLabel%></a>).&nbsp; You may search the KNB without 
                      being logged into your account, but will have access only 
                      to &quot;public&quot; data (see &quot;login &amp; registration&quot;)<br></br> 
                      Enter a search phrase (e.g. biodiversity) to search for 
                      data sets in the KNB, or click &quot;advanced search &quot; 
                      to enter more-detailed search criteria, or simply browse 
                      by category using the links below.</p></td>
                </tr>
                <tr valign="middle"> 
                  <td align="right" class="searchcat"> <input type="text" name="anyfield"  size="30" maxlength="200"> 
                    &nbsp;&nbsp;</td>
                  <td width="365" align="left" class="searchcat"> <input type="submit" value="Search KNB"> 
                    &nbsp;&nbsp;&nbsp;<a href="advancedsearch.jsp" target="_top"> 
                    &raquo;&nbsp;advanced&nbsp;search&nbsp;&laquo;</a></td>
                </tr>
              </table>
            </form></td>
        </tr>
        <%
/*
US Geography
------------
Northeast, Southeast, South, Midwest, Northwest, Southwest, Pacific Ocean, Atlantic Ocean, 
Great Lakes, (could also list states here)
*/
%>
        <tr> 
          <td width="375">&nbsp;</td>
          <td width="365">&nbsp;</td>
        </tr>
      </table></td>
  </tr>
</table>
<!-- ************************* END SEARCHBOX TABLE ************************* -->
