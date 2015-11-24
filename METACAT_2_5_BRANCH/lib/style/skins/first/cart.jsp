<%@ page    language="java" %>
<%
/**
 * 
 * '$RCSfile$'
 * Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    '$Author: leinfelder $'
 *      '$Date: 2008-08-22 16:48:56 -0700 (Fri, 22 Aug 2008) $'
 * '$Revision: 4305 $'
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

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@page import="edu.ucsb.nceas.metacat.service.SessionService"%>
<html>
<head>
<title>FIRST Assessment Metadata Repository</title>
<link rel="stylesheet" type="text/css"
	href="<%=STYLE_SKINS_URL%>/first/first.css">
<script language="JavaScript" type="text/JavaScript"
	src="<%=STYLE_SKINS_URL%>/first/first.js"></script>
<script language="JavaScript" type="text/JavaScript"
	src="<%=STYLE_SKINS_URL%>/first/search.js"></script>
<script language="JavaScript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/branding.js"></script>
<script language="Javascript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/prototype-1.5.1.1/prototype.js">
</script>	
<script language="Javascript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/effects.js">
</script>
<script language="Javascript">
		function checkBrowser() {
			Prototype.Browser.IE6 = Prototype.Browser.IE && parseInt(navigator.userAgent.substring(navigator.userAgent.indexOf("MSIE")+5)) == 6;
			Prototype.Browser.IE7 = Prototype.Browser.IE && parseInt(navigator.userAgent.substring(navigator.userAgent.indexOf("MSIE")+5)) == 7;
			Prototype.Browser.IE8 = Prototype.Browser.IE && !Prototype.Browser.IE6 && !Prototype.Browser.IE7;

			if (Prototype.Browser.IE6) {
				alert("NOTE: IE7, IE8, Firefox and Chrome are currently supported for data download.");
			}
		}
		function listAssessments(){
			var metacatURL = "<%=CONTEXT_URL%>/metacat";
		
			var docids = new Array();
			var doc = getIframeDocument("iframeheaderclass");
			var objs = doc.getElementsByName("@packageId");
			if (objs.length == 0) {
				return;
			}
			for (var i=0; i< objs.length; i++) {
				docids[i] = objs[i].value;
			}
				
			//generate the query to list assessments
			var queryString = 
				generateAssessmentListString(docids);
					
			callAjax(metacatURL, queryString, "first-assessment", "ajaxCartResults");
			
			Effect.Appear('ajaxCartResults');
			//Effect.BlindDown('ajaxCartResults');
		}
		
		function removeField(label) {
			var metacatURL = "<%=CONTEXT_URL%>/metacat";
			
			var myRequest = new Ajax.Request(
			metacatURL,
			{	method: 'post',
				parameters: { 
					action: 'editcart', 
					operation: 'removefield', 
					field: label, 
					path: null},
				evalScripts: true, 
				onSuccess: function(transport) {
					//in the cart, we should refresh the entire page
					window.location.reload();
				},
				onFailure: function(transport) {alert('failure saving field: ' + formElement.name);}
			 });
		}
		
		
		
   </script>
</head>
<body onload="checkBrowser();listAssessments()">
<script language="JavaScript">
          insertTemplateOpening("<%=CONTEXT_URL%>");
          insertSearchBox("<%=CONTEXT_URL%>");
      </script>

<table width="100%" border="0" cellspacing="20" cellpadding="0">
	<tr>
		<th colspan="2">Data Cart</th>
	</tr>
	<tr valign="top">
		<td colspan="1">
			<p class="emphasis">Selected Metadata Fields: 
				<a href="javascript:{}" onclick="Effect.Appear('fieldSelection')">Edit >></a>
			</p>
			<p>
				<table>
					<%
					String[] labels = 
						SessionService.getRegisteredSession(request.getSession().getId()).getDocumentCart().getLabels();
					for (int i = 0; i < labels.length; i++) {
					%>							
						<tr>
							<td>
								&nbsp;
								<a href="javascript:{}" onclick="removeField('<%=labels[i] %>')">
									<img src="<%=CONTEXT_URL%>/style/images/delete.gif" border="none"/>
								</a>
							</td>
							<td><%=labels[i] %> </td>
						</tr>	
					<%} %>
				</table>
			</p>
		</td>
		<td>
			<div id="fieldSelection" style="display:none;">
				<p class="emphasis">
					<a href="javascript:{}" onclick="Effect.Fade('fieldSelection')"> << Done</a>
				</p>	
				<form id="fieldForm" method="post" action="<%=CONTEXT_URL%>/metacat">
					<table>
						<tr>
							<th>Assessment</th>
							<th>Course</th>
						</tr>
						<tr>
							<td>
								<input type="checkbox" name="title" value="//assessment/title" />
									Title
								<br/>	
								<input type="checkbox" name="type" value="//assessment/type"/>
									Type
								<br/>	
								<input type="checkbox" name="duration" value="//assessment/duration"/>
									Duration
								<br/>	
								<input type="checkbox" name="groupGrading" value="//assessment/grading/@group"/>
									Group Grading
								<br/>	
								<input type="checkbox" name="groupSize" value="//assessment/grading/@size"/>
									Group Size
								<br/>	
								<input type="checkbox" name="groupGradingApproach" value="//assessment/grading"/>
									Group Grading Approach
							</td>	
							<td>
								<input type="checkbox" name="year" value="//course/year"/>
									Year
								<br/>	
								<input type="checkbox" name="term" value="//course/term"/>
									Term
								<br/>	
								<input type="checkbox" name="startDate" value="//course/coverage/rangeOfDates/beginDate/calendarDate"/>
									Start Date
								<br/>	
								<input type="checkbox" name="endDate" value="//course/coverage/rangeOfDates/endDate/calendarDate"/>						
									End Date
								<br/>	
								<input type="checkbox" name="courseTitle" value="//course/lom/general/title/string"/>
									Title
								<br/>	
								<input type="checkbox" name="courseId" value="//course/lom/general/identifier/entry"/>
									ID
								<br/>	
								<input type="checkbox" name="courseDescription" value="//course/lom/general/description/string"/>
									Description
							</td>
						</tr>
						<tr>
							<th>Institution</th>
							<th>Instructor</th>
						</tr>
						<tr>	
							<td>
								<input type="checkbox" name="institutionName" value="//institution/organizationName"/>
									Name
							</td>
							<td>
								<input type="checkbox" name="instructorName" value="//instructor/individualName/surName"/>
									Name
								<br/>	
								<input type="checkbox" name="instructorOrganization" value="//instructor/organizationName"/>
									Organization	
							</td>
						</tr>
						<tr>
							<td></td>
							<td>
								<input type="button" value="Save Field Selections" onclick="saveFields('fieldForm', '<%=CONTEXT_URL%>/metacat')"/>
							</td>	
						</tr>	
					</table>		
				</form>
			</div>
		</td>
	</tr>
	<tr>
		<th colspan="2">
			&nbsp;
		</th>
	</tr>
	<tr>
		<td colspan="2">
			<div id="ajaxCartResults" style="/*display:none;*/">
				<p class="emphasis">Data Cart is Empty</p>
			</div>
		</td>
	</tr>
</table>

<script language="JavaScript">          
    insertTemplateClosing("<%=CONTEXT_URL%>");
</script>
</body>
</html>
