<%@ page %>
<%
    String userId = (String) request.getSession().getAttribute("userId");
    if (userId != null && !userId.isEmpty()) {
%>
<div class="header">
    <ul>
        <li><img src="<%= request.getContextPath() %>/docs/_static/metacat-logo-white.png"
                 width="100px" style="float:left" alt="metacat logo"/></li>
        <li><a href="<%= application.getAttribute("logoutUri") %>">log in as different user</a></li>
        <li><a href="<%= application.getAttribute("logoutUri") %>">logout</a></li>
        <li><a href="<%= request.getContextPath() %>/docs" target="_blank">metacat user
            documentation</a></li>
    </ul>
</div>
<%
    } else {
%>
<div class="header">
    <ul>
        <li><img src="<%= request.getContextPath() %>/docs/_static/metacat-logo-white.png"
                 width="100px" style="float:left" alt="metacat logo"/></li>
        <li><a href="<%= application.getAttribute("loginUri") %>">log in</a></li>
        <li><a href="<%= request.getContextPath() %>/docs" target="_blank">metacat user
            documentation</a></li>
    </ul>
</div>
<%
    }
%>
