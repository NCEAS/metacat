<%@page import="edu.ucsb.nceas.metacat.properties.PropertyService"%>
<%@page import="edu.ucsb.nceas.metacat.properties.SkinPropertyService"%>
<%
    /**
 * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
 * convert an XML file showing the resultset of a query
 * into an HTML format suitable for rendering with modern web browsers.
 */
%>
<% 

String CFG = PropertyService.getProperty("application.default-style");
String email = SkinPropertyService.getProperty(CFG, "email.recipient");
%>
<%@ include file="common-settings.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
    <title>Metacat Default User Management</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <%@ include file="../../admin/head-section.jsp"%>
</head>
<body>
  
  <%@ include file="../../admin/header-section.jsp"%>
  <div class="document">
    <p>Please contact the administrator( <%=email%> ) to get a new account or reset password.
  </div>
 
</body>
</html>