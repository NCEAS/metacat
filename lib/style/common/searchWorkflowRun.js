/*
 * Generate a workflow query string.  this assumes that search fields meet the
 * following criteria in the web page:
 * -- search input fields have an ID that starts with sf_ 
 * -- the search input field name is the xpath of the value to search
 * -- if there is a search mode dropdown for an input field in the form, it's ID 
 *    should use the same convention as the input field, but start with sm_
 *    (i.e. the search mode input for the sf_firstname input would be sm_firstname) 
 * -- the value 
 */
 
function setWorkflowQueryFormField(formId) {
	var queryString = ""; 
	queryString += "<pathquery version='1.2'>";
	/*queryString += "<returndoctype>property</returndoctype>";
	queryString += "<returndoctype>-//UC Berkeley//DTD MoML 1//EN</returndoctype>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'user\']/@value</returnfield>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'description\']/@value</returnfield>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'startTime\']/@value</returnfield>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'workflowLSID\']/@value</returnfield>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'tpcStatus\']/@value</returnfield>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'workflowName\']/@value</returnfield>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'pdfReport\']/@value</returnfield>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'htmlReport\']/@value</returnfield>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'karLSID\']/@value</returnfield>";
	queryString += "<returnfield>/property[@class=\'org.kepler.util.WorkflowRun\']/property[@name=\'ReportInstance_pdf\']/@value</returnfield>";*/
	
	/*queryString += "<returndoctype>kar</returndoctype>";*/
	queryString += "<returndoctype>http://www.kepler-project.org/kar-2.0.0</returndoctype>";
	queryString += "<returndoctype>http://www.kepler-project.org/kar-2.1.0</returndoctype>";
  queryString += "<returnfield>karEntry/karEntryXML/property[@name=\'WorkflowRun\']/property[@name=\'user\']/@value</returnfield>";
  queryString += "<returnfield>karEntry/karEntryXML/property[@name=\'WorkflowRun\']/property[@name=\'description\']/@value</returnfield>";
  queryString += "<returnfield>karEntry/karEntryXML/property[@name=\'WorkflowRun\']/property[@name=\'startTime\']/@value</returnfield>";
  queryString += "<returnfield>karEntry/karEntryXML/property[@name=\'WorkflowRun\']/property[@name=\'workflowLSID\']/@value</returnfield>";
  queryString += "<returnfield>karEntry/karEntryAttributes/tpcStatus</returnfield>";
  queryString += "<returnfield>karEntry/karEntryXML/property[@name=\'WorkflowRun\']/property[@name=\'workflowName\']/@value</returnfield>";
  queryString += "<returnfield>mainAttributes/lsid</returnfield>";
  queryString += "<returnfield>karEntry/karEntryXML/property[@name=\'WorkflowRun\']/@class</returnfield>";
  queryString += "<returnfield>karEntry/karEntryAttributes/Name</returnfield>";
	
	queryString += "<querygroup operator='INTERSECT'>";	
	
	var elementList = document.getElementById(formId).elements;
	for(var i = 0; i < elementList.length; i++) {
	//alert("form element: " + elementList[i].id);
		if((elementList[i].id.indexOf("sf-") == 0) && (elementList[i].value != '')) {					
			queryString += getQueryTerm(elementList[i]);
		}
	} 
	
	queryString += "</querygroup>";	
	queryString += "</pathquery>";
	
	//alert(queryString);
	
	var queryField = document.getElementById("query");
	
	queryField.value = queryString;
}

/*
 * Generate individual query terms for all the search input fields in a search 
 * form.  There must be a case for each search field handle explicitly below.  
 * This assumes:
 * -- search input fields have an ID that starts with sf- 
 * -- if there is a search mode dropdown for an input field in the form, it's ID 
 *    should use the same convention as the input field, but start with sm-
 *    (i.e. the search mode input for the sf-firstname input would be sm-firstname) 
 */
function getQueryTerm(sfElement) {
	var baseId = sfElement.id.substring(3, sfElement.id.length);		
	var searchMode = "contains";
	var selector = document.getElementById("sm-" + baseId);
	if (selector != null) {
		searchMode = selector.value;
	}
	
	var pathExpr = '';
	if (sfElement.name == 'name') {
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>" + sfElement.value + "</value>";
    /*pathExpr += "<pathexpr>entity/@name</pathexpr>";*/
    pathExpr += "<pathexpr>karEntry/karEntryXML/entity/@name</pathexpr>";
    pathExpr += "</queryterm>";     
  } else if (sfElement.name == 'keyword') {
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>" + sfElement.value + "</value>";
    pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/@value</pathexpr>";
    pathExpr += "</queryterm>";     
  } else if (sfElement.name == 'creator') {
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>" + sfElement.value + "</value>";
    pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/property/configure</pathexpr>";
    pathExpr += "</queryterm>";     
  } else if (sfElement.name == 'description') {
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>" + sfElement.value + "</value>";
    pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/property/configure</pathexpr>";
    pathExpr += "</queryterm>";     
  } else if (sfElement.name == 'date-created') {
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>" + sfElement.value + "</value>";
    pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/property/configure</pathexpr>";
    pathExpr += "</queryterm>";     
  } else if (sfElement.name == 'date-executed') {
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>" + sfElement.value + "</value>";
    pathExpr += "<pathexpr>karEntry/karEntryXML/property/property/@value</pathexpr>";
    pathExpr += "</queryterm>";   
  } else if (sfElement.name == 'workflow-lsid') {
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>entityId</value>";
    pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/@name</pathexpr>";
    pathExpr += "</queryterm>"; 
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>" + sfElement.value + "</value>";
    pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/@value</pathexpr>";
    pathExpr += "</queryterm>";   
  }else if (sfElement.name == 'workflow-run-lsid') {
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>" + sfElement.value + "</value>";
    pathExpr += "<pathexpr>karEntry/karEntryXML/property/property/@value</pathexpr>";
    pathExpr += "</queryterm>";     
  }else if (sfElement.name == 'status') {
    pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
    pathExpr += "<value>" + sfElement.value + "</value>";
    pathExpr += "<pathexpr>karEntry/karEntryXML/property/property/@value</pathexpr>";
    pathExpr += "</queryterm>";     
  }
	
	//alert("returning path expression: " + pathExpr);
	return pathExpr;
}

	
