<%@ page language="java" %>
<%@page import="edu.ucsb.nceas.metacat.util.GeoserverUtil"%>
<!--
/**
  *  '$RCSfile$'
  *      Authors:     Matthew Perry
  *      Copyright:   2005 University of New Mexico and
  *                   Regents of the University of California and the
  *                   National Center for Ecological Analysis and Synthesis
  *      For Details: http://www.nceas.ucsb.edu/
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
  *
  */
-->
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
