<%@ page errorPage="jsperrorpage.html" %>

<%
 /**
  *   '$RCSfile$'
  *     Authors: Matthew Brooke
  *   Copyright: 2000 Regents of the University of California and the
  *              National Center for Ecological Analysis and Synthesis
  * For Details: http://www.nceas.ucsb.edu/
  *
  *    '$Author$'
  *      '$Date$'
  *  '$Revision$'
  *
  * Settings file for the lter metacat skin
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

<%
//Global settings for metacat are in common-settings.jsp.  Use this file to
//override or add to global values for this skin.

  // if true, POST variables echoed at bottom of client's browser window in a big yellow box.
  // Set this value to override the global value
  // boolean     DEBUG_TO_BROWSER      = false;

  // Add any local post fields to COMMON_SEARCH_METACAT_POST_FIELDS, 
  // SIMPLE_SEARCH_METACAT_POST_FIELDS, and ADVANCED_SEARCH_METACAT_POST_FIELD here
  COMMON_SEARCH_METACAT_POST_FIELDS +=
            "<input type=\"hidden\" name=\"qformat\"       value=\"lter\"\\>\n";
            
  SIMPLE_SEARCH_METACAT_POST_FIELDS +=
      "<input type=\"hidden\" name=\"qformat\"       value=\"lter\"\\>\n";

  ADVANCED_SEARCH_METACAT_POST_FIELDS +=
      "<input type=\"hidden\" name=\"qformat\"       value=\"lter\"\\>\n";

%>
