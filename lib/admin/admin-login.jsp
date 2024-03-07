<%@ page import="edu.ucsb.nceas.metacat.admin.MetacatAdminServlet" %>
<%@ page %>
<%
    final String context = request.getContextPath();
    final String loginUri = String.valueOf(application.getAttribute("loginUri"));
    final String afterOrcidloginUri = loginUri + "&orcidDone=true";
    final String adminHomepageUri = context + MetacatAdminServlet.ADMIN_HOMEPAGE_PATH;
    final String cnBaseUrl = String.valueOf(application.getAttribute("cnBaseUrl"));
    final String oAuthBaseUrl = cnBaseUrl + MetacatAdminServlet.D1_PORTAL_OAUTH_PATH;

    final String cnTokenUrl = application.getAttribute("cnBaseUrl")
            + MetacatAdminServlet.D1_PORTAL_TOKEN_PATH;

    Iterable<String> adminList = (Vector<String>) request.getAttribute("adminList");

    boolean loggingOut = Boolean.parseBoolean(String.valueOf(request.getAttribute("logout")));

    String url = request.getRequestURL().toString();
    String targetURL = url.substring(0, url.length() - request.getRequestURI().length()) + loginUri;
%>
<html>
<head>
    <title> Administrator Login Page</title>
    <%@include file="./head-section.jsp" %>
</head>
<body id="bodyContent">
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
            <a href="<%= oAuthBaseUrl + targetURL %>" class="signin orcid-btn update-orcid-sign-in-url orcid-flex"
               id="orcidLogin">
                <img src="<%= context %>/admin/images/orcid_64x64.png"
                     alt="orcid logo">
                <span>Sign in with ORCID</span>
            </a>
        </div>
    </div>
</div>
</body>
<script>
    const token_key = "metacatAdminJWTToken";

    window.onload =

        function () {

            if (<%= loggingOut %> === true) {
                alert("logout and return to login page...");
                console.log("logout and return to login page...");
                localStorage.removeItem(token_key);

                const cnBase = '<%= cnBaseUrl %>';
                const cnDomain = cnBase.substring(cnBase.indexOf('://') + 3)
                clearCookie("hazelcast.sessionid", cnDomain, "/portal");
                clearCookie("JSESSIONID", cnDomain, "/portal");
                clearCookie("JSESSIONID", new URL('<%= url %>').host, '<%= context %>');
alert("cookies cleared: cnDomain = " + cnDomain + " & new URL('<%= url %>').host = " + new URL('<%= url %>').host)
                window.location.replace('<%= loginUri %>');
            }

            if (getLocalToken() === null) {
                console.log("No token found in local storage - querying CN")
                getTokenFromCN();
            } else {
                console.log("Token found in local storage; sending request")
                requestWithToken('<%= adminHomepageUri %>');
            }

            function getTokenFromCN() {
                const xhr = new XMLHttpRequest();

                xhr.onreadystatechange = function () {
                    if (xhr.readyState === XMLHttpRequest.DONE) {
                        if (xhr.status === 200 && xhr.responseText.length !== 0) {
                            let jwtAdminToken = xhr.responseText;
                            localStorage.setItem(token_key, jwtAdminToken);
                            console.log("saved token; sending login request to metacat. "
                                + "xhr.status = " + xhr.status);
                            requestWithToken('<%= afterOrcidloginUri %>');
                        } else {
                            console.log("Unable to retrieve token; need to log in via orcid; "
                                + "xhr.readyState = " + xhr.readyState + "; xhr.status = "
                                + xhr.status + "; xhr.responseText.length = "
                                + xhr.responseText.length);
                        }
                    }
                }
                xhr.open('GET', '<%= cnTokenUrl %>', true);
                xhr.setRequestHeader('Cache-Control', 'no-cache');
                xhr.withCredentials = true; // Include credentials (cookies) in the request
                xhr.send();
            }

            function requestWithToken(uri) {
                const xhr2 = new XMLHttpRequest();
                xhr2.open('GET', uri, true);
                xhr2.setRequestHeader('Authorization', 'Bearer ' + getLocalToken());
                xhr2.setRequestHeader('Cache-Control', 'no-cache');
                xhr2.withCredentials = true;

                xhr2.onreadystatechange = function() {
                    if (xhr2.readyState === XMLHttpRequest.DONE && (xhr2.status <= 302)) {
                        document.getElementById('bodyContent').innerHTML = xhr2.responseText;
                    }
                };
                console.log("requestWithToken(" + uri + "); token = " +
                    getLocalToken().substring(0,5) + "...");
                xhr2.send();
            }

            function getLocalToken() {
                return localStorage.getItem(token_key);
            }

            function clearCookie(name, domain, path) {
                document.cookie = name + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT; domain="
                    + domain + "; path=" + path;
            }
        }
</script>
</html>
