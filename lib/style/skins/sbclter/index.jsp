<%@ page language="java" contentType="text/html" %>
<%@ page isELIgnored="false" %> 
<%@ page import="java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%--
  demo lifted from p294  _JavaServer Pages_  by
  Hans Bergsten, 3rd ed., O'Reilly (wolf book, ch 15) --%>

<%-- page data coming from an array of hashmaps for now. db later --%>
<%@ include file="templates/jsp/portal_settings.jsp"%>
<%@ include file="templates/jsp/include_session_vars.jsp"%>
<%@ include file="include_indexdata2.jsp" %>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
  <title>Santa Barbara Coastal LTER: Data Catalog</title>
  <style type="text/css">
        @import url("http://sbc.lternet.edu/w3_recommended.css");
        @import url("http://sbc.lternet.edu/sbclter_wrapper.css");
  </style>
  <link rel="stylesheet" type="text/css"
          href="<%=STYLE_SKINS_URL%>/sbclter/sbclter.css" /> 

  <!-- thanks to son-of-suckerfish javascript for making the hover menus work in IE
  see http://www.htmldog.com/articles/suckerfish/dropdowns/  -->
  <script type="text/javascript"><!--//--><![CDATA[//><!--
     sfHover = function() {
        var sfEls = document.getElementById("nav").getElementsByTagName("li");
        for (var i=0; i<sfEls.length; i++) {
        sfEls[i].onmouseover=function() {
            this.className+=" sfhover";
             }
            sfEls[i].onmouseout=function() {
               this.className=this.className.replace(new RegExp(" sfhover\\b"), "");
             }
        }
     }
     if (window.attachEvent) window.attachEvent("onload", sfHover);
    //--><!]]>
  </script>

  <script src="http://www.google-analytics.com/urchin.js" type="text/javascript">
  </script>
  <script type="text/javascript">
    uacct = "UA-2011494-1";
    urchinTracker();
  </script>
</head>

<body>
	<!-- include the header frag -->
	<jsp:include page="sbc_pageheader.htmlf" />

	<!--begin the left sidebar area-->

	<!-- begin content -->
  <div id="content">
<!--  	<div class="content-area"> -->
    <div id="index-h3">
    		<h3>Browse Datasets by Research Area</h3>
     </div>
      <!-- login form area -->
      
      <table class="onehundred_percent">
      <tr><td style="width:60%">
      
         <!--  <div> -->
               <div class="right-padding"> 
          <p class="instructions">To view metadata (with links to data tables), 
          click on the Title. For some data packages, additional tools are available:
           sampling locations can be browsed using Google maps and data can be queried 
           (i.e., limit the data returned by date, station and parameter). 
           If these tools are available, a link will be at the right.</p>
          </div>
      </td>
      
      <td>
      <!-- begin search box area -->
     <div>
    <!--  <div id="search-box_right"> --> 
      <table class="group group_border">
        <tr>
          <th>Query Metacat</th> 
          <th>&nbsp;</th>
        </tr>
        <tr>
          <td>Show metadata for all SBC LTER datasets:</td>
        <td>           
        <a	href="<%=SERVLET_URL%>?action=squery&amp;qformat=sbclter&amp;query=
      	<%= java.net.URLEncoder.encode(
	"<?xml version=\"1.0\"?>" +
         "<pathquery version=\"1.2\">" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>" +
           "<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>" +
           "<returnfield>eml/dataset/title</returnfield>" +
           "<returnfield>eml/dataset/creator/individualName/surName</returnfield>" +
           "<returnfield>eml/dataset/creator/organizationName</returnfield>" +
           "<returnfield>eml/dataset/dataTable/entityName</returnfield>" +
           "<returnfield>eml/dataset/dataTable/physical/distribution/online/url</returnfield>" +
           "<querygroup operator=\"INTERSECT\">" +
             "<queryterm casesensitive=\"false\" searchmode=\"starts-with\">" +
               "<value>SBCLTER:</value>" +
               "<pathexpr>eml/dataset/title</pathexpr>" +
             "</queryterm>" +
  
             "<queryterm casesensitive=\"false\" searchmode=\"contains\">" +
               "<value>SBCLTER_</value>" +
               "<pathexpr>eml/dataset/title</pathexpr>" +
             "</queryterm>" +
               
             "<queryterm casesensitive=\"true\" searchmode=\"equals\">" +
               "<value>public</value>" +
               "<pathexpr>eml/dataset/access/allow/principal</pathexpr>" +
             "</queryterm>" +
           "</querygroup>" +
         "</pathquery>") %>"
	>
             Run Query
        </a>
      </td>
    </tr>
    <tr>
      <td>Show datasets containing term:</td>
      <td><em>available soon</em>
           <%--
         <form name="searchByTerm" action="search_sbcmetacat.jsp" method="POST">
          <input type="text" name="keyword" value="input your search term"> 
         </form>
         <p> you chose: <c:out value="${param.keyword}" /></p>
       --%>
      </td>
    </tr>
   </table>
  </div>
  
      </td></tr></table>
      
      
    
	<%-- fix this - read the book. 
  somehow, a bean should hold the array name to for each category. maybe this?
	<jsp:useBean id="dp_groupBean" class="java.lang.String" />

<!-- then later, pass the groupname into the template, as a variable, not a string. -->
             <c:set var="dp_group" value="streamchemistry_dps" />
              <c:out value="${dp_group}" />
              <%@ include file="templates/jsp/table_dp_list.tmpl" %>
--%>
    <table class="group onehundred_percent">
    <tr>
     	<th>a. Patterns, transport, and processing of organic and inorganic inputs to coastal reefs</th>
      </tr>
      <tr><td>
					<ul class="ul-bulleted-list">
          	<li>Stream Chemistry</li>
						<table class="group  onehundred_percent">
						 <c:forEach items="${streamchemistry_dps}" var="current_dp">
  						<tr>
   						<td><a href="<%=SERVLET_URL%>?action=read&qformat=sbclter&docid=${current_dp.docid}">${current_dp.name}</a> (${current_dp.docid})</td>
              <td class="text_align_right">
               <c:if test="${!empty current_dp.queryapp_controller}">
                  <a href="http://sbclter.msi.ucsb.edu/${current_dp.queryapp_controller}">Browse station map and/or query data</a> 
                  </c:if>
                  </td>
  					 </tr>
 						</c:forEach>
						</table>
						</li>

            <li>Hydrology</li>
                <table class="group  onehundred_percent">
                <c:forEach items="${hydrology_dps}" var="current_dp">
                  <tr>
                  <td><a href="<%=SERVLET_URL%>?action=read&qformat=sbclter&docid=${current_dp.docid}">${current_dp.name}</a> (${current_dp.docid})</td>
            <td class="text_align_right">
             <c:if test="${!empty current_dp.queryapp_controller}">
                  <a href="http://sbclter.msi.ucsb.edu/${current_dp.queryapp_controller}">Browse station map and/or query data</a> 
                  </c:if>
                  </td>
                  </tr>
                </c:forEach>
              </table>

            <li>Watershed Characteristics</li>
						     <table class="group  onehundred_percent">
                <c:forEach items="${gis_dps}" var="current_dp">
                  <tr>
                  <td><a href="<%=SERVLET_URL%>?action=read&qformat=sbclter&docid=${current_dp.docid}">${current_dp.name}</a> (${current_dp.docid})</td>
            <td class="text_align_right">
            <c:if test="${!empty current_dp.queryapp_controller}">
                  <a href="http://sbclter.msi.ucsb.edu/${current_dp.queryapp_controller}">Browse station map and/or query data</a> 
                  </c:if>
                  </td>
                  </tr>
                </c:forEach>
              </table>

						<li>Ocean Currents and Biogeochemistry
              <ul>
                <li>Core measurements</li>
								<table class="group  onehundred_percent">
  							<c:forEach items="${biogeochemistry_core_dps}" var="current_dp">
    							<tr>
									<td><a href="<%=SERVLET_URL%>?action=read&qformat=sbclter&docid=${current_dp.docid}">${current_dp.name}</a> (${current_dp.docid})</td>
                  <td  class="text_align_right">
                   <c:if test="${!empty current_dp.queryapp_controller}">
                  <a href="http://sbclter.msi.ucsb.edu/${current_dp.queryapp_controller}">Browse station map and/or query data</a> 
                  </c:if>
                  </td>
    							</tr>
  							</c:forEach>
							</table>
              <li>Experiments</li>
								<table class="group  onehundred_percent">
  							<c:forEach items="${biogeochemistry_campaign_dps}" var="current_dp">
    							<tr>
									<td><a href="<%=SERVLET_URL%>?action=read&qformat=sbclter&docid=${current_dp.docid}">${current_dp.name}</a> (${current_dp.docid})</td>
                  <td class="text_align_right">
                   <c:if test="${!empty current_dp.queryapp_controller}">
                  <a href="http://sbclter.msi.ucsb.edu/${current_dp.queryapp_controller}">Browse station map and/or query data</a> 
                  </c:if>
                  </td>
    							</tr>
  							</c:forEach>
							</table>              
              </ul> 
             </li><!-- closes currents and biogeo -->
					</ul>
          </td></tr>
          </table>


<!-- begin part b) primary production -->				
     <table class="group onehundred_percent">
    <tr>
     	<th>b. Patterns and controls of biomass and primary production</th>
      </tr>
      <tr><td>     
          <ul class="ul-bulleted-list">
            <li>Biomass and primary production of giant kelp</li>
            <table class="group  onehundred_percent">
             <c:forEach items="${biomasspp_kelp_dps}" var="current_dp">
              <tr>
              <td><a href="<%=SERVLET_URL%>?action=read&qformat=sbclter&docid=${current_dp.docid}">${current_dp.name}</a> (${current_dp.docid})</td>
              <td class="text_align_right">
               <c:if test="${!empty current_dp.queryapp_controller}">
                  <a href="http://sbclter.msi.ucsb.edu/${current_dp.queryapp_controller}">Browse station map and/or query data</a> 
                  </c:if>
              </td>
             </tr>
            </c:forEach>
            </table>
            </li>

            <li>Biomass and primary production of phytoplankton in the Santa Barbara Channel
                <table class="group  onehundred_percent">
                <c:forEach items="${biomasspp_phyto_dps}" var="current_dp">
                  <tr>
                  <td><a href="<%=SERVLET_URL%>?action=read&qformat=sbclter&docid=${current_dp.docid}">${current_dp.name}</a> (${current_dp.docid})</td>
            <td class="text_align_right">
             <c:if test="${!empty current_dp.queryapp_controller}">
                  <a href="http://sbclter.msi.ucsb.edu/${current_dp.queryapp_controller}">Browse station map and/or query data</a> 
                  </c:if>
            </td>
                  </tr>
                </c:forEach>
              </table>
						</li>
					</ul>
          </td></tr></table>

<!-- begin part c) population dynamics -->				
     <table class="group onehundred_percent">
    <tr>
     	<th>c. Disturbance and population dynamics of kelp forest communities</th>
      </tr>
      <tr><td>     
          <ul class="ul-bulleted-list">
            <li>Kelp forest community structure and dynamics</li>
            <table class="group  onehundred_percent">
             <c:forEach items="${population_dps}" var="current_dp">
              <tr>
              <td><a href="<%=SERVLET_URL%>?action=read&qformat=sbclter&docid=${current_dp.docid}">${current_dp.name}</a> (${current_dp.docid})</td>
              <td class="text_align_right">
                <c:if test="${!empty current_dp.queryapp_controller}">
                  <a href="http://sbclter.msi.ucsb.edu/${current_dp.queryapp_controller}">Browse station map and/or query data</a> 
                  </c:if>
                  </td>
             </tr>
            </c:forEach>
            </table>
            </li>
					</ul>
          </td></tr></table>


			<!-- begin part d) trophic structure -->				
     <table class="group onehundred_percent">
    <tr>
     	<th>d.  Species interactions, trophic structure and food web dynamics</th>
      </tr>
      <tr><td>     
          <ul class="ul-bulleted-list">
            <li>Food web studies using stable isotope ratio analysis</li>
            <table class="group  onehundred_percent">
             <c:forEach items="${foodweb_dps}" var="current_dp">
              <tr>
              <td><a href="<%=SERVLET_URL%>?action=read&qformat=sbclter&docid=${current_dp.docid}">${current_dp.name}</a> (${current_dp.docid})</td>
              <td class="text_align_right">
               <c:if test="${!empty current_dp.queryapp_controller}">
                  <a href="http://sbclter.msi.ucsb.edu/${current_dp.queryapp_controller}">Browse station map and/or query data</a> 
                  </c:if>
              </td>
             </tr>
            </c:forEach>
            </table>
            </li>
					</ul>
          </td></tr>
          </table>





<!--</div>  close id content-area -->
</div> <!-- close class content -->
</body>
</html>
