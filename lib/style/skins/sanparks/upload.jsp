<%@ page    language="java" %>
<%
/*
*  '$RCSfile$'
*    Copyright: 2006 Regents of the University of California and the
*               National Center for Ecological Analysis and Synthesis
*
*   '$Author$'
*     '$Date$'
* '$Revision$'
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
%>
<%@ include file="settings.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <title>SANParks - South African National Park Data Repository</title>
        <link rel="stylesheet" href="<%=STYLE_SKINS_URL%>/default/default.css" type="text/css"/>
        <link rel="stylesheet" type="text/css" href="<%=STYLE_SKINS_URL%>/sanparks/sanparks.css"/>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_SKINS_URL%>/sanparks/sanparks.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/branding.js"></script>
    </head>
    <body>
        <script language="JavaScript">
            function getMapFrame() {
                return document.getElementById('mapFrame');
            }
            insertTemplateOpening("<%=CONTEXT_URL%>");
        </script>
        
        <jsp:include page="SaeonUpload.jspx" />
        
        <script language="JavaScript">
            //insertLoginBox();
            //insertSearchBox();
            insertTemplateClosing("<%=CONTEXT_URL%>");
        </script>
    </body>
</html>
