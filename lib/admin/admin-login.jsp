<%@ page import="edu.ucsb.nceas.metacat.admin.MetacatAdminServlet" %>
<%
    final String context = request.getContextPath();
    final String preOrcidLoginUri =
            String.valueOf(application.getAttribute(MetacatAdminServlet.ATTR_LOGIN_PRE_ORCID_URI));
    final String orcidLoginFlowUri =
            String.valueOf(application.getAttribute(MetacatAdminServlet.ATTR_LOGIN_ORCID_FLOW_URI));
    final String metacatLoginUri =
            String.valueOf(application.getAttribute(MetacatAdminServlet.ATTR_LOGIN_METACAT_URI));
    final String cnBaseUrl =
            String.valueOf(application.getAttribute(MetacatAdminServlet.ATTR_CN_BASE_URL));
    final String oAuthBaseUrl = cnBaseUrl + MetacatAdminServlet.PATH_D1_PORTAL_OAUTH;
    final String cnTokenUrl = cnBaseUrl + MetacatAdminServlet.PATH_D1_PORTAL_TOKEN;

    String url = request.getRequestURL().toString();
    String hostUrl = url.substring(0, url.length() - request.getRequestURI().length());
    String cnLogoutUrl =
            cnBaseUrl + MetacatAdminServlet.PATH_D1_PORTAL_LOGOUT + hostUrl + preOrcidLoginUri;
    String orcidLoginTargetURL = hostUrl + orcidLoginFlowUri;

    boolean orcidFlow = Boolean.parseBoolean(
            String.valueOf(request.getAttribute(MetacatAdminServlet.ACTION_ORCID_FLOW)));
    boolean loggingOut = Boolean.parseBoolean(
            String.valueOf(request.getAttribute(MetacatAdminServlet.ACTION_LOGOUT)));
%>
<html>
<head>
    <title>Administrator Login Page</title>
    <%@include file="./head-section.jsp" %>
</head>
<body id="bodyContent">
<%@include file="./header-section.jsp" %>
<div class="document">
    <h2>Administrator Login</h2>
    <%@include file="./page-message-section.jsp" %>
    <div class="orcid-btn-wrapper">
        <div class="orcid-flex">
            <a href="<%= oAuthBaseUrl + orcidLoginTargetURL %>"
               class="signin orcid-btn update-orcid-sign-in-url orcid-flex"
               id="orcidLogin">
                <img src="<%= context %>/admin/images/orcid_64x64.png"
                     id="orcidLogo"
                     alt="orcid logo">
                <span>Sign in with ORCID</span>
            </a>
        </div>
    </div>
    <div id="errorModal" class="error-modal">
        <div class="modal-content">
            <p id="errorModalMessage">Error</p>
            <button onclick="document.getElementById('errorModal').style.display='none'">Close
            </button>
        </div>
    </div>
</div>
</body>
<script>
    window.onload = loginOnloadHandler('<%= metacatLoginUri %>', '<%= cnTokenUrl %>',
        '<%= cnLogoutUrl %>', '<%= orcidFlow %>', '<%= loggingOut %>');
</script>
</html>
