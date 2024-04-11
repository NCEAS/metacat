<%@ page errorPage="jsperrorpage.html"%>
<%@page import="edu.ucsb.nceas.metacat.util.ConfigurationUtil"%>

<%
if (!ConfigurationUtil.isMetacatConfigured()) {
%>
	<jsp:forward page="/metacat" />
<%
}
%>

