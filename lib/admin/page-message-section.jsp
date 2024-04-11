<%@ page language="java" import="java.util.Vector" %>

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