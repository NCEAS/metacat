<%@ page language="java" %>
<% 
/**
 *  '$RCSfile$'
 *    Copyright: 2008 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *
 *   '$Author: daigle $'
 *     '$Date: 2008-07-06 21:25:34 -0700 (Sun, 06 Jul 2008) $'
 * '$Revision: 4080 $'
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

<%
if( request.getSession().getAttribute("userId") != null) {  
%>
  <div class="header">
  	<ul>
  		<li><img src="<%= request.getContextPath() %>/docs/_static/metacat-logo-white.png" width="100px" align="right"/></li>
	    <li><a href="<%= request.getContextPath() %>/admin?configureType=login">log in as different user</a></li>
	    <li><a href="<%= request.getContextPath() %>/metacat?action=logout">logout</a></li>
	    <li><a href="<%= request.getContextPath() %>/docs" target="_blank">metacat user documentation</a></li>
	</ul>
  </div>
<% 
} else {
%>
  <div class="header">
  	<ul>
  		<li><img src="<%= request.getContextPath() %>/docs/_static/metacat-logo-white.png" width="100px" align="right"/></li>
    	<li><a href="<%= request.getContextPath() %>/admin?configureType=login">log in</a></li>
    	<li><a href="<%= request.getContextPath() %>/docs" target="_blank">metacat user documentation</a></li>
   	</ul>
  </div>
<%
}
%>
