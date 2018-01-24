<%@ page errorPage="jsperrorpage.html" %>
<%@ page import="edu.ucsb.nceas.metacat.util.SystemUtil" %>
<%
	 /*
	 *   '$RCSfile$'
	 *     Authors: Matthew Brooke
	 *   Copyright: 2000 Regents of the University of California and the
	 *              National Center for Ecological Analysis and Synthesis
	 * For Details: http://www.nceas.ucsb.edu/
	 *
	 *    '$Author$'
	 *      '$Date$'
	 *  '$Revision$'
	 *
	 * Settings file for the default metacat skin
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
	// This is a special case where we do not include style/common/common-settings.jsp  
	String LOCAL_SERVLET_URL = SystemUtil.getServletURL();
%>