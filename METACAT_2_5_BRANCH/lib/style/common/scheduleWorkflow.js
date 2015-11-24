 /*
  *   '$RCSfile$'
  *     Purpose: Basic funtions to support showing the schedule workflows 
  *              pages
  *   Copyright: 2009 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *     Authors: Michael Daigle
  *
  *    '$Author: leinfelder $'
  *      '$Date: 2008-06-17 13:16:32 -0700 (Tue, 17 Jun 2008) $'
  *  '$Revision: 4006 $'
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
  
/* puts together the url to get the workflow section of a page given the workflow 
 * id.  
 *  Params:
 *    url - metacat url to hit
 *    workflowId - lsid of the workflow
 *    karid - the lsid of the kar file which contains the workflow
 *    sessionid - id of the session
 *    authServiceURL - the url of the authentication service
 *    authorizationURL - the url of the authorization service
 *    workflowName - used for display purposes.  
 *    divId - the name of the div where the results should be put
 */  
function getWorkflowRunSection(url, workflowId, workflowName, karid, sessionid,  authServiceURL, authorizationURL, divId) {
	var requestUrl = url + '?action=getScheduledWorkflow&workflowid=' + workflowId +'&karid='+karid+ '&qformat=sanparks&workflowname=' + workflowName
	                             +'&sessid='+sessionid+'&authServiceURL='+authServiceURL+'&authorizationURL='+authorizationURL;
	//alert('getWorkflowRunSection - url: ' + url + ' workflowId: ' + workflowId + ' workflowName: ' + workflowName + ' divId: ' + divId+' karid: '+karid+' authServiceURL: '+authServiceURL+'authorizationURL: '+authorizationURL);
	var submitResults = submitUrlIntoDiv(requestUrl, divId);
}

	
