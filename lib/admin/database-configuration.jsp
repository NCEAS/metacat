<%@ page language="java" %>
<%@ page import="java.util.Vector,edu.ucsb.nceas.metacat.database.DBVersion,edu.ucsb.nceas.metacat.MetacatVersion" %>


<%
    MetacatVersion metacatVersion = (MetacatVersion)request.getAttribute("metacatVersion");
    DBVersion databaseVersion = (DBVersion)request.getAttribute("databaseVersion");
    Vector<String> updateScriptList = (Vector<String> )request.getAttribute("updateScriptList");
    String supportEmail = (String)request.getAttribute("supportEmail");
%>

<html>
<head>

<title>Database Install/Upgrade Utility</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
    <h2>Database Install/Upgrade Utility</h2>
    <h3><em>It may take several minutes or longer, if you are upgrading Metacat from a 
            previous version. Please wait until the process is done.</em></h3>
    <p><%@ include file="page-message-section.jsp"%></p>
    
    <%
    if (databaseVersion != null && databaseVersion.getVersionString().equals("0.0.0")) {
    %>
         The system has detected that this is a new database. <br><br>
         Please hit the Continue button to upgrade your database to version: <%= metacatVersion.getVersionString() %>. <br><br>
         <div class="alert alert-error">Warning: this will reinitialize your database.  If this is not a new database, hit the Cancel button and contact support at <%= supportEmail %>. </div><br><br>
         The following scripts will be run:
    
            <ul>
    <%
            for (int i = 0; i < updateScriptList.size(); i++) {
    %>
                    <li>
                        <%= updateScriptList.elementAt(i) %>
                    </li>
    <%
            }
    %>
            </ul>
            <input class="button" type="button" value="Continue" onClick="forward('./admin?configureType=database&processForm=true')">
            <input class="button" type="button" value="Cancel" onClick="forward('./admin?configureType=configure&processForm=false')">
    <%
    } else if (databaseVersion != null) {
    %>
         The system has detected the following database version: <%= databaseVersion.getVersionString() %> <br><br>
         Please hit the Continue button to upgrade your database to version: <%= metacatVersion.getVersionString() %> <br>
        <div class="alert alert-error">Warning: this will update your database.  If the detected versions do not seem correct, hit the Cancel button and contact support at <%= supportEmail %> </div><br><br>
         The following scripts will be run:  <br>
            <ul>
    <%
            for (int i = 0; i < updateScriptList.size(); i++) {
    %>
                    <li>
                        <%= updateScriptList.elementAt(i) %>
                    </li>
    <%
            }
    %>
            </ul>
            <div class="buttons-wrapper">
                <input class="button" type="button" value="Continue" onClick="forward('./admin?configureType=database&processForm=true')">
                <input class="button" type="button" value="Cancel" onClick="forward('./admin?configureType=configure&processForm=false')">
            </div>
    <%
    } else {
    %>
            <div class="buttons-wrapper">
                <input class="button" disabled type="button" value="Continue" onClick="forward('./admin?configureType=database&processForm=true')">
                <input class="button" type="button" value="Cancel" onClick="forward('./admin?configureType=configure&processForm=false')">
            </div>
    <%
    }
    %>
</div>
    <%@ include file="./footer-section.jsp"%>

</body>
</html>
