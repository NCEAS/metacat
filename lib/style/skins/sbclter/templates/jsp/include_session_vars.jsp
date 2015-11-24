<%@ page import="edu.ucsb.nceas.metacat.client.*" %>
<% 
 /**
  *  '$RCSfile: include_session_vars.jsp,v $'
  *      Authors: Matt Jones, Chad Berkley
  *    Copyright: 2000 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *  For Details: http://www.nceas.ucsb.edu/
  *
  *   '$Author: cjones $'
  *     '$Date: 2004/10/05 23:50:45 $'
  * '$Revision: 1.1 $'
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
  * This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
  * convert an XML file showing the resultset of a query
  * into an HTML format suitable for rendering with modern web browsers.
  */

  //////////////////////////////////////////////////////////////////////////////
  //
  // NOTE:
  //
  //  GLOBAL CONSTANTS (SETTINGS SUCH AS METACAT URL, LDAP DOMAIN  AND DEBUG 
  //  SWITCH) ARE ALL IN THE INCLUDE FILE "PORTAL_SETTINGS.jsp"
  //
  //////////////////////////////////////////////////////////////////////////////

  // GLOBAL VARIABLES //////////////////////////////////////////////////////////
  String      loginStatus              = null;
  String      sess_sessionId           = null;
  String      sessionidField           = null;
  String      metacatResponse          = null;
  String      posted_ldapUserName      = null;
  String      typedUserName            = null;
  String      posted_organization      = null;
  String      posted_password          = null;
  String      loginAction              = null;
  String      loginButtonLabel         = null;
  String      loginEnabledDisabled     = null;
  Metacat     metacat                  = null;
  HttpSession rfSession                = null;
  boolean     isLoggedIn               = false;
  
  //////////////////////////////////////////////////////////////////////////////  
  
  try {
    rfSession = request.getSession(true);
    
  } catch(Exception e) {
  
    throw new ServletException("trying to get session: "+e);
  }
  
  Object sess_sessionIdObj = rfSession.getAttribute("sess_sessionIdObj");
  
  if (sess_sessionIdObj!=null) {
    sess_sessionId = (String)sess_sessionIdObj;
    
    // if sess_sessionIdObj is empty string, that means user has logged out
    // (had problems if I set it to null, so used empty string instead)
    if (sess_sessionId.length() < 1) sess_sessionId = null;
    else isLoggedIn = true;
    
  } else {
    
    sess_sessionId = null;
  }
  
  typedUserName   = request.getParameter("username");
  typedUserName   = (typedUserName!=null)? typedUserName.trim() : "";
  
  posted_organization = request.getParameter("organization");
  posted_organization = (posted_organization!=null)? posted_organization.trim() : "";

  posted_ldapUserName = "uid=" + typedUserName 
                        + ",o=" + posted_organization + LDAP_DOMAIN;
  
  posted_password = request.getParameter("password");
  posted_password = (posted_password!=null)? posted_password.trim() : "";
  
  loginAction     = request.getParameter("loginAction");
  loginAction     = (loginAction!=null)? loginAction.trim() : "";
  
  //////////////////////////////////////////////////////////////////////////////  
  
  if (loginAction.equals(LOGOUT_LABEL)) { 
  // DO LOGOUT //////////////////////////////////////////////////////////////    
  
    if (sess_sessionId!=null && rfSession.getAttribute("sess_metacatObj")!=null) {
      metacat = (Metacat)(rfSession.getAttribute("sess_metacatObj"));
      metacat.logout();      
    }
    sess_sessionId = null;
    rfSession.setAttribute("sess_sessionIdObj", "");
    isLoggedIn = false;
    
  } else if (loginAction.equals(LOGIN_LABEL)) {  
  // DO LOGIN ////////////////////////////////////////////////////////////////  
  
    if (sess_sessionId!=null) isLoggedIn = true;    
    else {
    
      // get metacat object - either cached from session...
      if (rfSession.getAttribute("sess_metacatObj")!=null) {
        
        metacat = (Metacat)(rfSession.getAttribute("sess_metacatObj"));
      
      } else {   // ...or create it if it doesn't already exist
        try {
          metacat = MetacatFactory.createMetacatConnection(SERVLET_URL);
        } catch (MetacatInaccessibleException mie) {
          throw new ServletException("Metacat connection to "+SERVLET_URL
                                                +" failed." + mie.getMessage());
        }
        if (metacat==null) {
          throw new ServletException("Metacat connection to "+SERVLET_URL
                                          +" failed - Metacat object is NULL!");
        }
        rfSession.setAttribute("sess_metacatObj", metacat);
      }
      // now do login...
      try {
          metacatResponse = metacat.login(posted_ldapUserName, posted_password);
          
          if (metacatResponse!=null && metacatResponse.indexOf("<login>") >= 0) {
            sess_sessionId = metacat.getSessionId();
            rfSession.setAttribute("sess_sessionIdObj", 
                                  ((sess_sessionId!=null)? sess_sessionId: "") );
            isLoggedIn = true;
          } else {
            loginStatus = "<em class=\"italic\">Incorrect username or password - please try again</em>";
            isLoggedIn = false;
          }
      } catch (MetacatAuthException mae) {
          loginStatus = "<em class=\"italic\">Incorrect username or password! - please try again</em>";
          isLoggedIn = false;
      } catch (MetacatInaccessibleException mie) {
          isLoggedIn = false;
          loginStatus = "<em class=\"italic\">ERROR logging in - problems connecting - please try later</em>";
      }
    }
  }
  
  //////////////////////////////////////////////////////////////////////////////  

  if (isLoggedIn) {
    loginButtonLabel     = LOGOUT_LABEL;
    loginEnabledDisabled = "disabled";
    if (sess_sessionId!=null && sess_sessionId.length()>0) {
      sessionidField = "<input type=\"hidden\" name=\"sessionid\"     value=\""
                                                          +sess_sessionId+"\">";
    } else {
      sessionidField = "";
    }
    // if loginStatus has already been set above, don't overwrite it...
    if (loginStatus==null) loginStatus 
                          = "<em class=\"italic\">logged in as: " +posted_ldapUserName+"</em>";
  } else {
    loginButtonLabel     = LOGIN_LABEL;
    loginEnabledDisabled = "";
    sessionidField = "";
    // if loginStatus has already been set above, don't overwrite it...
    if (loginStatus==null) loginStatus 
                      = "<em class=\"italic\">Not logged in: </em>";
  }

  if (DEBUG_TO_BROWSER) {
%>  
<div id="debug">              
  <ul>
    <li>SERVLET_URL:&nbsp;<%=SERVLET_URL%></li>
    <li>rfSession:&nbsp;<%=rfSession%></li>
    <li>metacatResponse:&nbsp;<%=metacatResponse%></li>
    <li>posted_ldapUserName:&nbsp;<%=posted_ldapUserName%></li>
    <li>posted_password:&nbsp;<%=posted_password%></li>
    <li>isLoggedIn:&nbsp;<%=isLoggedIn%></li>
    <li>sess_sessionId LOCAL:&nbsp;<%=sess_sessionId%></li>
    <li>sess_sessionIdObj FROM SESSION:&nbsp;<%=rfSession.getAttribute("sess_sessionIdObj")%></li>
  </ul>
</div>
<%
  }
%>
