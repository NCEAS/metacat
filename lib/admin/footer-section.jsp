<%@ page %>
<%
    if (request.getSession().getAttribute("userId") != null) {
%>
<div class="footer">
    <p>You are logged in as: <%= request.getSession().getAttribute("userId") %>
    </p>
    <ul>
        <li><a href="<%= application.getAttribute("logoutUri") %>">log in as different user</a></li>
        <li><a href="<%= application.getAttribute("logoutUri") %>">logout</a></li>
        <li><a href="<%= request.getContextPath() %>/docs/user/index.html" target="_blank">metacat
            user documentation</a></li>
    </ul>
</div>
<%
} else {
%>
<div class="footer">
    <p>You are not logged in.</p>
    <ul>
        <li><a href="<%= application.getAttribute("loginUri") %>">log in</a></li>
        <li><a href="<%= request.getContextPath() %>/docs/user/index.html" target="_blank">metacat
            user documentation</a></li>
    </ul>
</div>
<%
    }
%>
