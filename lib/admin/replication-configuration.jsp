<%@ page language="java" %>
<%@ page import="java.util.Set,java.util.Map,java.util.Vector,edu.ucsb.nceas.utilities.PropertiesMetaData" %>
<%@ page import="edu.ucsb.nceas.utilities.MetaDataGroup,edu.ucsb.nceas.utilities.MetaDataProperty" %>

<html>
<head>

<title>Replication Configuration</title>
<%@ include file="./head-section.jsp"%>
</head>

<FRAMESET ROWS="*,150"  FRAMEBORDER=0 BORDER=0>
  
  <FRAME SRC="<%= request.getContextPath() %>/admin/replication-configuration-include.jsp" BORDER=0 NAME="top">

  <FRAME SRC="<%= request.getContextPath() %>/admin?configureType=replication&action=servercontrol&subaction=list" name="bottom">

</FRAMESET>

</html>
