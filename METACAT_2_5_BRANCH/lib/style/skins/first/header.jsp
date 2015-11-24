<%@ page    language="java" %>
<%
/**
 * 
 * '$RCSfile$'
 * Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    '$Author$'
 *      '$Date$'
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
     
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */  
%>

<%@ include file="../../common/common-settings.jsp"%>
<%@ include file="../../common/configure-check.jsp"%>
<%@page import="edu.ucsb.nceas.metacat.service.SessionService"%>
<%@page import="java.util.Map"%>


<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@page import="java.util.Iterator"%>
<%@page import="edu.ucsb.nceas.metacat.service.SessionService"%>
<html>
<head>
<title>FIRST Data Repository</title>
<link rel="stylesheet" type="text/css"
	href="<%=STYLE_SKINS_URL%>/first/first.css"></link>
<script language="Javascript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/prototype-1.5.1.1/prototype.js">
</script>		
<script language="Javascript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/effects.js">
</script>
<script language="Javascript">
		
	function highlight() {
		//Effect.Pulsate('importantThings', { pulses: 1, duration: 2 });
		//Effect.Fade('importantThings');
		new Effect.Highlight('importantThings', { startcolor: '#ffff99', endcolor: '#ffffff' });
		//Effect.Shake('importantThings');
	}
</script>		
	
</head>

<body onload="highlight()">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
	<tr>
		<td width="75%" height="145" valign="middle" background="<%=STYLE_SKINS_URL%>/first/images/bkgrndgradient.gif">
			<table width="1094" border="0" cellspacing="0" cellpadding="0">
			  <tr>
			    <td width="1012"><img src="<%=STYLE_SKINS_URL%>/first/images/first_logo.png" width="1012" height="133" /></td>
			    <td width="82" valign="top"><br />
			      <table width="63" border="0" cellspacing="0" cellpadding="0">
			      <tr>
			        	<td>&nbsp;</td>
			      </tr>
			      <tr>
						<td>&nbsp;</td>
			      </tr>
			      <tr>
						<td>&nbsp;</td>
			      </tr>
			    </table>
			    </td>
			  </tr>
			</table>
		</td>
	</tr>
	<tr>
		<td height="48" valign="middle" align="center" bgcolor="#C8C4B9" background="<%=STYLE_SKINS_URL%>/first/images/navbarbackground.gif">
			<table width="780" border="0" cellspacing="0" cellpadding="0">
			  <tr>
			    <td><a href="index.jsp" target="_top">Home</a></td>
			    <td><a href="http://www.first2.org/" target="_top">FIRST</a></td>
			    <td><a href="about.jsp" target="_top">About</a></td>
						<%
						boolean loggedIn = SessionService.isSessionRegistered(request.getSession().getId());
						String[] docids = null;
						Map fields = null;
						if (loggedIn) {
							docids = 
								SessionService.getRegisteredSession(request.getSession().getId()).getDocumentCart().getDocids();
							fields = 
								SessionService.getRegisteredSession(request.getSession().getId()).getDocumentCart().getFields();
							%>
							<td>
								<a href="<%=SERVLET_URL%>?action=logout&qformat=first" target="_top">Logout</a>
							</td>
							<td>
							<div id="importantThings">
								<a href="cart.jsp" target="_top">
									Data Cart 
								</a>
								(Items: <%=docids.length %>, Fields: <%=fields.size() %>)
							</div>
							<!-- for the cart status -->
							<div style="display:none;">
								<form id='cartForm'>
									<%
									for (int i = 0; i < docids.length; i++) {
									%>							
										<input name="@packageId" value="<%=docids[i] %>" type="hidden"> 
									<%} %>
								</form>
							</div>
							<!-- for use when downloading merged data -->
							<div style="display:none;">
								<form id='fieldForm'>
									<%
									if (loggedIn) {
										Iterator fieldIter =  fields.keySet().iterator();
										while (fieldIter.hasNext()) {
											String key = (String) fieldIter.next();
											String value = (String) fields.get(key);
										%>
											<input type="hidden" name="<%=key %>" value="<%=value %>" />
										<%	
										}
									}	
									%>
								</form>
							</div>
							</td>
						<%	
						}
						else {
						%>
							<td>
								<a href="login.jsp" target="_top">Login</a>
							</td>
						<%	
						}
						%>						
			  </tr>
			</table>
		</td>
	</tr>
</table>		
</body>
</html>
