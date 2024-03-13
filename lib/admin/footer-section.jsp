<%@ page %>
<%
    if (request.getSession().getAttribute("userId") != null) {
%>
<div class="footer">
    <p>You are logged in as: <%= userId %></p>
    <ul>
        <li><a href="<%= logoutUri %>">log out</a></li>
        <li><a href="<%= docsUri %>" target="_blank">metacat user documentation</a></li>
    </ul>
</div>
<%
} else {
%>
<div class="footer">
    <p>You are not logged in.</p>
    <ul>
        <li><a href="<%= loginUri %>">log in</a></li>
        <li><a href="<%= docsUri %>" target="_blank">metacat user documentation</a></li>
    </ul>
</div>
<%
    }
%>
