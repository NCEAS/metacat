<%@ page    language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>

<%
 /**
  *  '$RCSfile: index.jsp,v $'
  *    Copyright: 2000 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *  For Details: http://www.nceas.ucsb.edu/
  *
  *   '$Author: cjones $'
  *     '$Date: 2004/10/07 07:24:38 $'
  * '$Revision: 1.2 $'
  * 
  * adapted for displaying sbc's pubs list
  * 2005-july -- mob --
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
%>
<%@ include file="templates/jsp/portal_settings.jsp"%>
<%@ include file="templates/jsp/include_session_vars.jsp"%>
<%
///////////////////////////////////////////////////////////////////////////////
// NOTE:
//
//  GLOBAL CONSTANTS (SETTINGS SUCH AS METACAT URL, LDAP DOMAIN	AND DEBUG
//  SWITCH) ARE ALL IN THE INCLUDE FILE "portal_settings.jsp"
///////////////////////////////////////////////////////////////////////////////
%>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
  <head>
    <title>SBCLTER Publications Catalog</title>
    <style type="text/css">
        @import url("http://sbc.lternet.edu/w3_recommended.css");
        @import url("http://sbc.lternet.edu/sbclter_wrapper.css");
    </style>
    <meta http-equiv="Content-Type" content="text/html" charset="utf-8" />
    <link rel="stylesheet" type="text/css"
          href="<%= STYLE_SKINS_URL %>/sbclter/sbclter.css" />
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
  <c:import url="./templates/xsl/sbcCitationList.xsl" charEncoding="utf-8" var="stylesheet" />
  
   <!-- this statement for importing directly into a variable.  -->
   <%--
  <c:import url="${SERVLET_URL}?action=read&qformat=xml&docid=sbc_pubs_test.1" charEncoding="UTF-8" var="xmldoc" />  
  --%>
   
  <!-- this statement used when using reader -->
  <c:import url="${SERVLET_URL}?action=read&qformat=xml&docid=sbc_pubs_test.1" 
	varReader="xmlSource" >
	<x:parse var="xmldoc" xml="${xmlSource}" scope="application" />
  </c:import>
  
 <%--
  saving these lines. Could import both the xml and the style sheet and 
  tranform here, if that's all you're going to do. but if need to add the 
  filter, then you have to parse if first (above) and transform after you 
  have input from the filter box
    <c:import url="http://sbcdata.lternet.edu/catalog/metacat?action=read&qformat=xml&docid=sbc_pubs_test.1" charEncoding="utf-8" var="xmldoc" />
  <x:transform xslt="${stylesheet}" doc="${xmldoc}">
  </x:transform>
--%>
 
<!-- include the header frag -->
<jsp:include page="sbc_pageheader.htmlf" />

    <!-- begin page content -->
    <div id="content">
      <div class="content-area-dense">
	<table class="group">
	<tr>
	<td class="forty_percent">
        
	<div class="skipto-box">Skip to:<br>
 		<a class="submenu" href="#INPRESS">In Press</a> | 
 		<a class="submenu" href="#ART_CHAP">Articles and Chapters</a> |
 		<a class="submenu" href="#BOOKS">Books</a> |
		<a class="submenu" href="#THESES">Theses</a>
        <br/>
		<a class="submenu" href="#REP_CP">Reports</a> |
		<a class="submenu" href="#REP_CP">Conference Proceedings</a>
        <br/>
		<a class="submenu" href="#AV">Audio Visual</a> |
		<a class="submenu" href="#PRESENTATIONS">Presentations</a>
	</div> <!-- end of class skip-to -->
       
	</td>
	<td class="sixty_percent">
	<!-- begin login form area --> 
      <!-- end login form area -->
      
      <!-- begin search box area -->
      <%-- 
      code snippets in searchbox were lifted from _JavaServer Pages_  by
      Hans Bergsten, 3rd ed., O'Reilly (wolf book, ch 15) --%>
      
      <br/>            
      
	<%-- comment out in no-filter version 
	  
     <!--  Create a list of unique author-surNames in the XML feed  -->   
	<jsp:useBean id="uniqueCats" class="java.util.TreeMap" />
    <x:forEach select="$xmldoc/citationList/citation/creator/individualName/surName" var="category">
    <!-- convert the XPath node to a Java String -->
      <x:set var="catName" select="string($category)" />
      <c:set target="${uniqueCats}" property="${catName}" value="" />
    </x:forEach>
	--%>
   
     
    <div id="search-box_right">
     <em>Search box returning soon</em>
    
    <%-- 
	<!-- send list of unique authors to a select form -->
     <form action="publications.jsp">
       Select Author:
      <select name="selCat">
        <option value="ALL">SHOW ALL 
        <c:forEach items="${uniqueCats}" var="current">
          <option value="<c:out value="${current.key}" />"
            <c:if test="${param.selCat == current.key}">
              selected
            </c:if>>
            <c:out value="${current.key}" />
          </option>
        </c:forEach>
      </select>
      <input type="submit" value="Go">  
     </form>
     --%>
    </div>
    
    <!-- end search-box area -->
    
    
</td>
</tr>
</table>

<%--
    <!-- x:transform generates the rest of the page using pubs xml doc from metacat, 
    	xsl stylesheet, and the author selected above.  -->
    <c:choose>
    	<c:when test="${empty param.selCat || param.selCat == 'ALL'}">
		    <x:transform xslt="${stylesheet}" doc="${xmldoc}"/>
	</c:when>
	<c:otherwise>
		<x:transform xslt="${stylesheet}" doc="${xmldoc}">
	    		<x:param name="author" value="${param.selCat}"/>
		</x:transform>
	</c:otherwise>
    </c:choose>
    --%>
    
    
    <!-- this is the simple version -->
<x:transform xslt="${stylesheet}" doc="${xmldoc}"/>
    
    

        </div>  <!-- end id=content-area  -->

    </div> <!-- end id=content  -->



  </body>
</html>
