<%@ page    language="java" import="edu.ucsb.nceas.metacat.util.ConfigurationUtil" %>
<%
	/**
 *  '$RCSfile$'
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

<%@ include file="style/common/common-settings.jsp"%>

<% 

/**
 *  Does redirects depending on the "qformat" parameter passed in the request -
 *  redirect is to <web_context>/style/skins/{qformat}/index.jsp
 *  Defaults to <web_context>/style/skins/DEFAULT_STYLE/index.jsp if no value 
 *  is provided for qformat
 */
 
String qformat     = request.getParameter("qformat");
String addedParams = "";
String redirectURI = "";

if (ConfigurationUtil.isMetacatConfigured()) {
    //if qformat has not been set, set its value to "DEFAULT_STYLE"
    if (qformat==null) {
      qformat = DEFAULT_STYLE;
      addedParams = "?qformat="+qformat;
    }
    redirectURI = "/style/skins/"+qformat+"/"+addedParams;
} else {
    redirectURI = "/metacat";
}



%>

<jsp:forward page="<%= redirectURI %>" />
