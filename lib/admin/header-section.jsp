<%@ page %>
<%
    if( request.getSession().getAttribute("userId") != null) {
%>
<div class="header">
    <ul>
        <li><img src="<%= request.getContextPath() %>/docs/_static/metacat-logo-white.png"
                 width="100px" style="float:right" alt="metacat logo"/></li>
        <li><a href="<%= request.getContextPath() %>/admin?configureType=login">log in as different user</a></li>
        <li><a href="<%= request.getContextPath() %>/metacat?action=logout">logout</a></li>
        <li><a href="<%= request.getContextPath() %>/docs" target="_blank">metacat user documentation</a></li>
    </ul>
</div>
<%
} else {
%>
<div class="header">
    <ul>
        <li><img src="<%= request.getContextPath() %>/docs/_static/metacat-logo-white.png"
                 width="100px" style="float:right" alt="metacat logo"/></li>
        <li><a href="<%= request.getContextPath() %>/admin?configureType=login">log in</a></li>
        <li><a href="<%= request.getContextPath() %>/docs" target="_blank">metacat user documentation</a></li>
    </ul>
</div>
<%
    }
%>
<script>
    document.addEventListener('DOMContentLoaded', function () {
        const form = document.querySelector('form');
        form.addEventListener('submit', function (event) {
            event.preventDefault(); // Prevent the default form submission

            const formData = new FormData(form); // Collect form data
            const jwtToken = localStorage.getItem("metacatAdminJWTToken");

            fetch(form.action, {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + jwtToken,
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams(formData).toString(),
            })
                .then(response => response.json()) // Assuming the server responds with JSON
                .then(data => {
                    console.log(data); // Handle the response data
                })
                .catch(error => {
                    console.error('Error:', error);
                });
        });
    });
</script>
