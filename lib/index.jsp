<%@ page    language="java" import="edu.ucsb.nceas.metacat.util.ConfigurationUtil" %>
<%
	/**
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
