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

	
