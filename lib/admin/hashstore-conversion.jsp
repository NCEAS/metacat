<%@ page language="java" %>
<%@ page import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.PropertiesMetaData" %>
<%@ page import="edu.ucsb.nceas.utilities.MetaDataGroup,edu.ucsb.nceas.utilities.MetaDataProperty" %>


<html>
<head>

<title>Hashstore Conversion</title>
<%@ include file="./head-section.jsp"%>
</head>
<body>
<%@ include file="./header-section.jsp"%>

<div class="document">
    <h2>DOI Configuration</h2>
    <p>
        Metacat will convert the storage system from the legacy file system to Hashstore after you
        click the okay button. Note: The process may take several hours.
    </p>
    <br clear="right"/>
    <%@ include file="page-message-section.jsp"%>
    <form method="POST" name="configuration_form" action="<%= request.getContextPath() %>/admin" 
                                            onsubmit="return submitForm(this);">
        <div class="buttons-wrapper">

            <input class=button type="button" value="Okay" onClick="forward('./admin?configureType=hashstore&start=true&processForm=true')"
            <input class=button type="button" value="Cancel" onClick="forward('./admin')"> 
        </div>
    </form>
</div>

<%@ include file="./footer-section.jsp"%>

</body>
</html>