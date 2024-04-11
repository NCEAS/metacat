<%@ page language="java" %>
<%@page import="edu.ucsb.nceas.metacat.util.GeoserverUtil"%>

<%@ include file="../common-settings.jsp"%>
<%@ include file="../configure-check.jsp"%>
<%
	String GEOSERVER_URL = GeoserverUtil.getGeoserverContextURL();
%>
<!-- *********************** START Map ************************* -->
<html>
  <head>
	<link rel="stylesheet" type="text/css" href="<%=STYLE_COMMON_URL%>/spatial/map.css" />
    <script src="<%=STYLE_COMMON_URL%>/spatial/OpenLayers-2.13.1/OpenLayers.js"></script>
    <script src="<%=STYLE_COMMON_URL%>/spatial/map.js"></script>
    <script type="text/javascript">
        function init() {
            var bounds = new OpenLayers.Bounds(-180,-90,180,90);
            // make the map for this skin
            initMap("<%=GEOSERVER_URL%>", "<%=SERVLET_URL%>", "default", bounds);
        }
    </script>
  </head>
  <body onload="init()">
    <div id="map"></div>
	<div id="featureList">
		<form>
			<select id="locations" name="locations" onchange="zoomToLocation(this)">
				<%@ include file="locations.jsp"%>
			</select>
		</form>
	</div>
  </body>
</html>
