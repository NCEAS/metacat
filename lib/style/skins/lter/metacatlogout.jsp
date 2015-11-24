<%@ page language="java"%>
<%@ page import="edu.ucsb.nceas.metacat.client.*" %>

<!--
/**
  *  '$RCSfile$'
  *      Authors:     Duane Costa
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

<html>
  
  <head>
    <title>LTER Data Catalog Logout</title>
    <%
        Metacat metacat = (Metacat) session.getAttribute("metacat");
        
        if (metacat != null) {
          metacat.logout();
        }
        
        session.setAttribute("loggedIn", new Boolean(false));
        response.sendRedirect("index.jsp");
    %>
  </head>

  <body>
  </body>
  
</html>
