<%@ page language="java" %>
<%@ page import="java.util.TimeZone" %>
<%@ page import="edu.ucsb.nceas.metacat.accesscontrol.AccessControlInterface" %>
<%@ page import="edu.ucsb.nceas.metacat.PermissionController" %>
<%@ page import="edu.ucsb.nceas.utilities.LSIDUtil" %>
<%
/*
*  '$RCSfile$'
*    Copyright: 2009 Regents of the University of California and the
*               National Center for Ecological Analysis and Synthesis
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

String karLsid = request.getParameter("karid");
String karId = LSIDUtil.getDocId(karLsid, true);
PermissionController permissionController = new PermissionController(karId);
boolean hasSchedulePermissions = 
	permissionController.hasPermission(request.getSession().getId(), AccessControlInterface.READSTRING);

%>
<%@ include file="settings.jsp"%>

<html>
    <head>
        <title>Sanparks TPC Report Search</title>
        <link rel="stylesheet" type="text/css" href="<%=STYLE_SKINS_URL%>/sanparks/sanparks.css"/>
        <link rel="stylesheet" type="text/css" href="<%=STYLE_SKINS_URL%>/sanparks/sanparks-tpc.css"/>
        <link rel="stylesheet" type="text/css" href="<%=STYLE_SKINS_URL%>/sanparks/sanparks-scheduled-jobs.css"/>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_SKINS_URL%>/sanparks/sanparks.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/style/skins/sanparks/searchWorkflowPathQuery.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/branding.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/widgets/form-fields-widget.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/prototype-1.5.1.1/prototype.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/ajax-utils.js"></script>
        <script language="JavaScript" type="text/JavaScript" src="<%=STYLE_COMMON_URL%>/scheduleWorkflow.js"></script>
    </head>
    <body onload="getWorkflowRunSection('<%=SERVLET_URL%>','<%=request.getParameter("workflowid")%>','<%=request.getParameter("workflowname")%>','<%=request.getParameter("karid")%>','<%=request.getSession().getId()%>','<%=authenticationServiceURL%>','<%=authorizationServiceURL%>','workflow-run-content')">
		<table class="page-section" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td>
        			<table class="center-content-section" cellpadding="0" cellspacing="0" border="0">
        				<tr>

           
    						<td class="schedule-section">  
    	    					<div class="content-subsection" id="workflow-summary-section"> 
<%
    	    						if (! hasSchedulePermissions) {
%>
									<div class="warning-header">You have view permissions only for this workflow</div>
<%
    	    						} 
%>		
									<div class="content-subsection-header">Workflow Summary</div> 
    								<div class="form-input-row" id="form-base-row">
    									<div class="summary-field" id='workflow-name-label' >Name: </div>  
<%
										if (request.getParameter("workflowname") != null) {
%>
										<div class="summary-value" id='workflow-name-value' ><%=request.getParameter("workflowname")%></div>
<%
										}
%>	  
										<br>
										<div class="summary-field" id='workflow-lsid-label' >LSID: </div>  
<%
										if (request.getParameter("karid") != null) {
%>
										<div class="summary-value" id='workflow-lsid-value' ><%=request.getParameter("workflowid")%></div>
<%
    	    								if (hasSchedulePermissions) {
%>
											(<jsp:text><![CDATA[<a href=]]></jsp:text><%=STYLE_SKINS_URL%><jsp:text><![CDATA[/sanparks/workflowAccessMain.jsp?karfilelsid=]]></jsp:text><jsp:expression>request.getParameter("karid")</jsp:expression><jsp:text><![CDATA[&workflowname=]]></jsp:text><jsp:expression>request.getParameter("workflowname")</jsp:expression><jsp:text><![CDATA[>Change Access Permissions</a>]]></jsp:text>)						
<%
    	    								}
										}
%>	  
									</div>
								</div>
								<div class="content-subsection" id="schedule-section">   
    								<div class="content-subsection-header">Schedule Workflow </div> 
        							<form action="<%=SERVLET_URL%>" name="workflowScheduler" id="workflowScheduler">
										<input name="qformat" value="sanparks" type="hidden" />
										<input name="action" value="scheduleWorkflow" type="hidden" />  
										<input name="forwardto" value="scheduleWorkflowRunMain.jsp" type="hidden" /> 
<%
										if (request.getParameter("workflowid") != null) {
%>
										<input name='workflowid' value='<%=request.getParameter("workflowid")%>' type='hidden' />
<%
										}
										if (request.getParameter("karid") != null) {
%>
										<input name='karid' value='<%=request.getParameter("karid")%>' type='hidden' />
<%
										}
										if (request.getParameter("workflowname") != null) {
%>
										<input name='workflowname' value='<%=request.getParameter("workflowname")%>' type='hidden' />
<%
										}
%>	  		 
          
 <%
                    if(CONTEXT_URL !=null){
 %>
                     <input name='sourceRepositoryBaseURL' value='<%=CONTEXT_URL%>' type='hidden' />
 <%
                    }
 %>        
 
 <%
                    if( authorizationPath !=null){
 %>
                     <input name='sourceAuthorPath' value='<%=authorizationPath%>' type='hidden' />
 <%
                    }
 %>                          
  
<%
                    if( queryPath !=null){
 %>
                     <input name='sourceQueryPath' value='<%=queryPath%>' type='hidden' />
<%
                    }
 %>                  
 
 <%
                    if(  workflowRunEngineName!=null){
 %>
                     <input name='workflowRunEngineName' value='<%=workflowRunEngineName%>' type='hidden' />
<%
                    }
 %>                      
  
<%
                    if( workflowRunEngineURL !=null){
 %> 
                    <input name='workflowRunEngineURL' value='<%=workflowRunEngineURL%>' type='hidden' />
<%
                    }
 %>         
 
 <%
                    if( request.getSession().getId() !=null){
 %>                   
                    <input name='sessionid' value='<%=request.getSession().getId()%>' type='hidden' />
 <%
                    }
 %>      
  
 <%
                    if( authenticationServiceURL !=null){
 %>                   
                    <input name='authServiceURL' value='<%=authenticationServiceURL%>' type='hidden' />
 <%
                    }
 %>                          
										<div class="form-input-row">
											<div class="field-label" id='start-time-label' >Start Time: </div>  
											<input class="date-input" name='starttime' id='starttime' /> 
											<div class="field-suffix"><%= TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT) %> &nbsp;&nbsp;(mm/dd/yyyy hh:mm:ss)</div>
										</div>
										<div class="form-input-row">
											<div class="field-label" id='start-time-label' >End Time: </div>  
											<input class="date-input" name='endtime' id='endtime' /> 
											<div class="field-suffix"><%= TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT) %> &nbsp;&nbsp;(mm/dd/yyyy hh:mm:ss)</div>
										</div>
										<div class="form-input-row">
											<div class="field-label" id='interval-label' >Interval: </div>  
											<input class="int-input" name='intervalvalue' id='intervalvalue' /> 
											<select class="dropdown-input" name='intervalunit'>
												<!-- option name="seconds" value="sec">seconds</option -->
												<!-- option name="minutes" value="min">minutes</option -->
												<option name="hours" value="hour">hours</option>
												<option name="days" value="day">days</option>
												<option name="weeks" value="week">weeks</option>
												<option name="months" value="mon">months</option>
											</select>
										</div>
										<div class="form-input-row">
                      <div class="field-label" id=resultDestination-label' >Result Destination: </div>  
                      <select class="dropdown-long-input" name='destinationRepositoryName'>
                        <%
                          for( int i = 0; i < repositoryList.size(); i++ )
                           {
                        %>
                            <option name='<%= repositoryList.elementAt( i ).toString() %>' value='<%= repositoryList.elementAt( i ).toString() %>' > 
                            <%=repositoryList.elementAt( i ).toString() %> </option>
                        <%
                          }
                        %>
                      </select>
                    </div>
										<br> <br>     		
					        			<input class="submit-button" value="Schedule" type="submit"
<%
					    	    		if (! hasSchedulePermissions) {
%>
											disabled="disabled"
<%
					    	    		}
%>      			
					        			> 
        							</form>
								</div>		
								<div class="content-subsection" id="workflow-run-section"> 
									<div class="content-subsection-header">Workflow Run Schedule</div> 
					    			<div class="workflow-run-content" id="workflow-run-content"></div>
								</div>
        					</td>
        				</tr>
        			</table>
        		</td>
        	</tr>
        </table>
    </body>
</html>
