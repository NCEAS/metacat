<%@ page import="edu.ucsb.nceas.metacat.admin.MetacatAdminServlet" %>
<%@ page import="edu.ucsb.nceas.metacat.util.RequestUtil" %>
<%
    final String userId =
            String.valueOf(request.getSession().getAttribute(RequestUtil.ATTR_USER_ID));
    final String loginUri =
            String.valueOf(application.getAttribute(MetacatAdminServlet.ATTR_LOGIN_PRE_ORCID_URI));
    final String logoutUri =
            String.valueOf(application.getAttribute(MetacatAdminServlet.ATTR_LOGOUT_URI));
    final String docsUri = request.getContextPath() + "/docs";
%>
    <noscript>Please enable JavaScript to continue using this application.</noscript>
    <div class="header">
        <ul>
            <li><img src="<%= request.getContextPath() %>/docs/_static/metacat-logo-white.png"
                     width="100px" style="float:left" alt="metacat logo"/></li>
<%
    if (!"null".equals(userId) && !userId.isBlank()) {
%>
            <li><a href="<%= logoutUri %>">log out</a></li>
<%
    } else {
%>
            <li><a href="<%= loginUri %>">log in</a></li>
<%
    }
%>
            <li><a href="<%= docsUri %>" target="_blank">metacat user documentation</a></li>
        </ul>
    </div>
