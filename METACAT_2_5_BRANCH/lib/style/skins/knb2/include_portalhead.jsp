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
  
  // if including page has not set a value for the display title, 
  // use the default one from the settings file
  if (include_portalhead_title==null) include_portalhead_title = DEFAULT_PORTALHEAD_TITLE;
%>
<!-- ********************* START PORTAL HEADER TABLE *********************** -->
<table width="100%" cellpadding="0" cellspacing="0">
  <tr>
    <td width="10" align="right" valign="top" class="title">&#160;</td>

    <td width="80" align="center" valign="bottom" class="title">
      <a target="_top" href="index.jsp">
        <img
        src="images/KNBLogo_top.gif"
         border="0" /></a></td>

    <td align="left" valign="middle" class="title">
      <p class="title"><%=include_portalhead_title%></p></td>
  </tr>

  <tr>
    <td width="10" align="right" valign="top" class="maintable">
      <img
      src="images/transparent1x1.gif"
       width="10" height="3" /></td>

    <td width="80" rowspan="2" align="center" valign="top"
    class="sectionheader">
      <a target="_top" href="index.jsp">
        <img
        src="images/KNBLogo_bottom.gif"
         width="80" height="25" border="0" /></a></td>

    <td align="left" valign="top" class="toollink" width="100%">
    <img src="images/transparent1x1.gif"
       width="600" height="3" /></td>
  </tr>
  <tr>
    <td width="10" align="right" valign="top" class="sectionheader">
    &#160;</td>

    <td align="left" valign="middle" class="sectionheader" style="text-align: left">
    &#160;&#160;&#160;<a target="_top" href="index.jsp"
    class="toollink">Home</a></td>
  </tr>
</table>
<!-- *********************** END PORTAL HEADER TABLE *********************** -->
<% 
  // reset title to null, ready for next use
  include_portalhead_title=null;
%>
