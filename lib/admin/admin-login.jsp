<%
    Vector<String> adminList = (Vector<String>) request.getAttribute("adminList");
    boolean loggingOut = Boolean.parseBoolean((String)request.getAttribute("logout"));
    if (loggingOut) {
        request.getSession().removeAttribute("userId");
    }
    final String contextPath = request.getContextPath();
    String url = request.getRequestURL().toString();
    String baseURL = url.substring(0, url.length() - request.getRequestURI().length())
            + contextPath + "/";
    String targetURL = baseURL + "admin?loginAction=Login&amp;orcidDone=true";
    // TODO - use cn url from properties instead
    String oAuthUrl = "https://cn.dataone.org/portal/oauth?action=start&amp;target=" + targetURL;
%>
<html>
<head>
    <title> Administrator Login Page</title>
    <%@include file="./head-section.jsp" %>
</head>
<body>
<%@include file="./header-section.jsp" %>

<div class="document">
    <h2> Administrator Login</h2>
    <p> Account login page .</p>

    <%@include file="./page-message-section.jsp" %>
    <table class="admin-login">
        <tr>
            <td><h4>Administrators</h4></td>
            <td>
                <label>
                    <select class="username-input" name="username">
                        <%
                            if (adminList != null) {
                                for (String adminName : adminList) {
                        %>
                        <option><%=adminName %></option>
                        <%
                                }
                            }
                        %>
                    </select>
                </label>
            </td>
        </tr>
        <tr>
            <td></td>
            <td class="textinput-description">[Administrator ORCID]</td>
        </tr>
    </table>
    <div class="orcid-btn-wrapper">
        <div class="orcid-flex">
            <a href="<%= oAuthUrl %>" class="signin orcid-btn update-orcid-sign-in-url orcid-flex"
               id="orcidLogin">
                <img src="<%= contextPath %>/admin/images/orcid_64x64.png"
                     alt="orcid logo">
                <span>Sign in with ORCID</span>
            </a>
        </div>
    </div>
</div>
</body>
<script>

    window.onload =

        function () {

            const backToLogin = <%= loggingOut %>;
            alert("onload: loggingOut = " + backToLogin);

            if (backToLogin === true) {
                alert("Back To Login... <%= baseURL %>/admin/admin-login.jsp?configureType=logout");
                window.location.replace(
                    "<%= baseURL %>/admin/admin-login.jsp?configureType=logout");
            }

            if (getLocalToken() === null) {
                console.log("No token found in local storage - querying CN")
                getTokenFromCN();
            } else {
                console.log("Token found in local storage; sending request")
                requestWithToken("/admin/metacat-configuration.jsp?configureType=configure");
            }

            function getTokenFromCN() {
                const xhr = new XMLHttpRequest();

                xhr.onreadystatechange = function () {
                    if (xhr.readyState === XMLHttpRequest.DONE) {
                        if (xhr.status === 200 && xhr.responseText.length !== 0) {
                            let jwtAdminToken = xhr.responseText;
                            localStorage.setItem("metacatAdminJWTToken", jwtAdminToken);
                            console.log("saved token; sending login request to metacat. "
                                + "xhr.status = " + xhr.status);
                            requestWithToken("/admin/?loginAction=Login&orcidDone=true");
                        } else {
                            console.log("Unable to retrieve token; need to log in via orcid; "
                                + "xhr.readyState = " + xhr.readyState + "; xhr.status = "
                                + xhr.status + "; xhr.responseText.length = "
                                + xhr.responseText.length);
                            alert("Unable to retrieve token; need to log in via orcid; "
                                + "xhr.readyState = " + xhr.readyState + "; xhr.status = "
                                + xhr.status + "; xhr.responseText.length = "
                                + xhr.responseText.length + " window.location.pathname = " + window.location.pathname);
                            // if (window.location.pathname === '/metacat/admin/')
                        }
                    }
                }
                xhr.open('GET', 'https://cn.dataone.org/portal/token', true);
                xhr.setRequestHeader('Cache-Control', 'no-cache');
                xhr.withCredentials = true; // Include credentials (cookies) in the request
                xhr.send();
            }

            function requestWithToken(uri) {
                const xhr2 = new XMLHttpRequest();
                const targetUrl = '<%= contextPath %>' + uri;
                xhr2.open('GET', targetUrl, true);
                xhr2.setRequestHeader('Authorization', 'Bearer ' + getLocalToken());
                xhr2.setRequestHeader('Cache-Control', 'no-cache');
                xhr2.withCredentials = true;

                xhr2.onreadystatechange = function() {
                    if (xhr2.readyState === XMLHttpRequest.DONE && (xhr2.status <= 302)) {
                        document.open();
                        document.write(xhr2.responseText);
                        document.close();
                    }
                };
                console.log("requestWithToken(" + targetUrl + "); token = " +
                    getLocalToken().substring(0,5) + "...");
                xhr2.send();
            }

            function getLocalToken() {
                return localStorage.getItem("metacatAdminJWTToken");
            }
        }
</script>
</html>
