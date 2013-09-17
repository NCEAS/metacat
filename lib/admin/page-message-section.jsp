<%@ page language="java" import="java.util.Vector" %>
<% 
/**
 *  '$RCSfile$'
 *    Copyright: 2008 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
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

<%  
   if (request.getAttribute("processingSuccess") != null && ((Vector<String>)request.getAttribute("processingSuccess")).size() > 0) { 
%> 
   <div class="alert alert-success">
<%     
      Vector<String> processingSuccesses = (Vector<String>)request.getAttribute("processingSuccess");
      for (String processingSuccess : processingSuccesses) { 
%>
    	  <%= processingSuccess %> <br> 
<%     
       }   
%>
	</div>
<%      
   }

   if (request.getAttribute("processingMessage") != null && ((Vector<String>)request.getAttribute("processingMessage")).size() > 0) { 
%> 
   <div class="alert">
<%     
      Vector<String> processingMessages = (Vector<String>)request.getAttribute("processingMessage");
      for (String processingMessage : processingMessages) { 
%>
    	   <%= processingMessage %> <br> 
<%     
       } 
%>
  	</div>
<%
   } 
   
   if (request.getAttribute("formFieldErrors") != null && ((Vector<String>)request.getAttribute("formFieldErrors")).size() > 0) { 
%>
   <div class="alert alert-error">
      Please correct the following form errors and resubmit: <br><br>
<%     
      Vector<String> formErrors = (Vector<String>)request.getAttribute("formFieldErrors");
      for (String formError : formErrors) { 
%>
    	  <%= formError %> <br> 
<%     
       }     
%>      
   </div>
<% 
   } else if (request.getAttribute("processingErrors") != null && ((Vector<String>)request.getAttribute("processingErrors")).size() > 0) { 
%>
   <div class="alert alert-error">
      The following errors occurred.  Please correct errors if possible or contact your system adminstrator or contact support at <%= (String)request.getAttribute("supportEmail") %> <br><br>
<%     
      Vector<String> processingErrors = (Vector<String>)request.getAttribute("processingErrors");
      for (String processingError : processingErrors) { 
%>
    	  <%= processingError %> <br> 
<%     
       }     
%>      
   </div>

<%
   }
%>