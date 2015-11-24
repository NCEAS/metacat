/****************************************************************************
* Multiple Assessment download
* @param form containing the neceassary input items
* @return true/false for form submission
*****************************************************************************/
function multipleAssessmentSearch(submitFormObj, fieldFormObj) {
	
	//alert("submitFormObj=" + submitFormObj);
	//harvest the metadata fields we want to include
	var metadataObjs = new Array();
	var index = 0;
	for (var i=0; i < fieldFormObj.length; i++) {
		var formElement = fieldFormObj.elements[i];
		var metadataObj = new Object();
		metadataObj.name = formElement.name;
		metadataObj.value = formElement.value;
		metadataObjs[index] = metadataObj;
		index++;
	}
	
	//TODO option for all questions vs. just one
	//var questionId = submitFormObj.questionId.value;
	//alert("questionId=" + questionId);
	
	var documentObjects = new Array();
	var index = 0;
	if (submitFormObj.docid.length > 1) {
		for (var i=0; i < submitFormObj.docid.length; i++) {
			if (submitFormObj.docid[i].value != "") {
				var documentObject = new Object();
				documentObject.docid = submitFormObj.docid[i].value;
				documentObject.demographicData = submitFormObj[documentObject.docid + "demographicData"].value;
				
				var questionIds = new Array();
				for (var z = 0; z < submitFormObj[documentObject.docid].length; z++) {
					questionIds[z] = submitFormObj[documentObject.docid][z].value;
				}
			
				documentObject.questionIds = questionIds;
				documentObjects[index] = documentObject;
				index++;
			}
		}
	}
	else {
		//alert("submitFormObj.docid=" + submitFormObj.docid);
		if (submitFormObj.docid.value != "") {
			var documentObject = new Object();
			documentObject.docid = submitFormObj.docid.value;
			documentObject.demographicData = submitFormObj[documentObject.docid + "demographicData"].value;
				
			var questionIds = new Array();
			if (submitFormObj[documentObject.docid].length > 1) {
				for (var z = 0; z < submitFormObj[documentObject.docid].length; z++) {
					questionIds[z] = submitFormObj[documentObject.docid][z].value;
				}
			}
			else {
				questionIds[0] = submitFormObj[documentObject.docid].value;
			}
			
			documentObject.questionIds = questionIds;
			documentObjects[0] = documentObject;
		}
	}
	
	var itemMetadataCount = submitFormObj.metadataCount.value;
	
	var query = generateQuery(documentObjects, metadataObjs, itemMetadataCount);
	
	submitFormObj.dataquery.value = query;
	
	return true;
		
}

/****************************************************************************
* Single Assessment download
* @param form containing the neceassary input items
* @return true/false for form submission
*****************************************************************************/
function assessmentSearch(submitFormObj) {

	//harvest the metadata fields we want to include
	var metadataObjs = new Array();
	var index = 0;
	for (var i=0; i < submitFormObj.length; i++) {
		var formElement = submitFormObj.elements[i];
		if (formElement.type == "checkbox" && formElement.checked) {
			//ignore certain other checkboxes, kind of a hack 
			if (formElement.name == "includeQuestions") {
				continue;
			}
			var metadataObj = new Object();
			metadataObj.name = formElement.name;
			metadataObj.value = formElement.value;
			metadataObjs[index] = metadataObj;
			index++;
		}
	}
	
	//var checkBox = document.getElementById("searchAll");
	var docId = submitFormObj.docid.value;
	//alert("docId=" + docId);
	
	//do we want question metadata?
	var includeQuestions = submitFormObj.includeQuestions.checked;
	var questionIds = new Array();
	questionIds[0] = "";
	
	if (includeQuestions) {
		if (submitFormObj.assessmentItemId.length > 1) {
			for (var i=0; i < submitFormObj.assessmentItemId.length; i++) {
				questionIds[i] = submitFormObj.assessmentItemId[i].value;
			}
		}
		else {
			questionIds[0] = submitFormObj.assessmentItemId.value;
		}
	}
	
	//set up the list of objects to pass to the query assembler, just one document
	var documentObjects = new Array();
	var documentObject = new Object();
	documentObject.docid = docId;
	documentObject.questionIds = questionIds;
	documentObjects[0] = documentObject;
	
	var query = generateQuery(documentObjects, metadataObjs);
	
	submitFormObj.dataquery.value = query;
	
	return true;
		
}

/****************************************************************************
* Save fields for the attribute maping
* @param formId containing the neceassary input items
* @return true/false for form submission
*****************************************************************************/
function saveFields(formId, metacatURL) {

	var submitFormObj = document.getElementById(formId);
	
	//send the request to clear
	var myRequest = new Ajax.Request(
	metacatURL,
	{	method: 'post',
		parameters: { 
			action: 'editcart', 
			operation: 'clearfields'},
		evalScripts: true, 
		//onSuccess: function(transport) {alert('success: ' + transport.status);},
		onFailure: function(transport) {alert('failure clearing fields');}
	 });
	
	//go through the check boxes and set the ones we want
	var count = 0;
	for (var i=0; i < submitFormObj.length; i++) {
		var formElement = submitFormObj.elements[i];
		if (formElement.type == "checkbox" && formElement.checked) {
			//ignore certain other checkboxes, kind of a hack, but it's javascript...
			if (formElement.name == "includeQuestions") {
				continue;
			}
			if (formElement.name == "docid") {
				continue;
			}
			
			//send the request
			var myRequest = new Ajax.Request(
			metacatURL,
			{	method: 'post',
				parameters: { 
					action: 'editcart', 
					operation: 'addfield', 
					field: formElement.name, 
					path: formElement.value},
				evalScripts: true, 
				onSuccess: function(transport) {
					//alert('Selections saved: ' + operation); 
					//refresh after the save
					if (document.getElementById("ajaxCartResults")) {
						window.location.reload();
					}
					else {
						window.document.getElementById("iframeheaderclass").src=window.document.getElementById("iframeheaderclass").src;
					}
				}, 
				onFailure: function(transport) {alert('failure saving field: ' + formElement.name);}
			 });
		 	count++;
		}
	}
	
	//window.document.getElementById("iframeheaderclass").src = window.document.getElementById("iframeheaderclass").src;
	
	//alert(count + ' Field selections saved.'); 
	
	return true;
		
}

/****************************************************************************
* Query Generation function
* @param docObjs is an Array of Objects with "docid" (String) and "questionIds" (Array) properties
* @param metadataObjs is an Array of Objects with "name" and "value" properties (both String)
* @return generated query string
*****************************************************************************/
function generateQuery(docObjs, metadataObjs, itemMetadataCount) {
	//alert("calling method");
	
	//make parameters at some point
	var includeDemographicData = false;
	var questionMetadataCount = 2;
	if (itemMetadataCount) {
		questionMetadataCount = itemMetadataCount;
	}
	var questionChoiceCount = 5;
	
	//construct the assessment metadata attribute selection snippet
	var metadataAttributes = "";
	var index = 0;
	for (var j=0; j < metadataObjs.length; j++) {
		var metadataObj = metadataObjs[j];
		
		metadataAttributes += "<attribute index=\"";
		metadataAttributes += index;
		metadataAttributes += "\">";
		
		metadataAttributes += "<pathexpr label=\"";
		metadataAttributes += metadataObj.name;
		metadataAttributes += "\">";
		metadataAttributes += metadataObj.value;
		metadataAttributes += "</pathexpr>";
		
		metadataAttributes += "</attribute>";
		
		index++;
	}//metadataObjs loop
	
	
	//construct the begining of the query
	var tempQuery = 
        "<?xml version=\"1.0\"?>"
        + "<dataquery>"
			+ "<union order=\"true\">";
			
	for (var i=0; i < docObjs.length; i++) {

		var docId = docObjs[i].docid;
		var containsDemographicData = docObjs[i].demographicData;
		//alert("containsDemographicData=" + containsDemographicData);
		
		//get the question ids for this question
		var questionIds = docObjs[i].questionIds;
	
		//alert("questionIds=" + questionIds);
		
		//assemble the assessment metadata
		var metadataAttributeSelection = "";
		if (metadataAttributes.length > 0) {
			metadataAttributeSelection =
				"<datapackage id=\"" + docId + "\">"
					+ "<entity id=\"" + docId + "\">"
						+ metadataAttributes
					+ "</entity>"
				+ "</datapackage>";
		}
				
		//loop for each question item
		for (var k=0; k < questionIds.length; k++) {
			var questionId = questionIds[k];
		
			tempQuery +=
			"<query>"
			+ "<selection>";
			
			//select the data
			tempQuery +=
				"<datapackage id=\"" + docId + "\">"
					+ "<entity index=\"0\">"
						+ "<attribute index=\"0\"/>"
						+ "<attribute index=\"1\"/>"
						//DO NOT omit student id attribute - used for transpose
						+ "<attribute index=\"2\"/>"
						+ "<attribute index=\"3\"/>"
						+ "<attribute index=\"4\"/>"
						// the external response file
						+ "<attribute index=\"6\"/>"
					+ "</entity>"
				+ "</datapackage>";
				
			//select the demographic data
			if (includeDemographicData) {
				if (containsDemographicData) {
					tempQuery +=
						"<datapackage id=\"" + docId + "\">"
							+ "<entity index=\"1\">"
								//omit student id attribute
								+ "<attribute index=\"1\"/>"
								+ "<attribute index=\"2\"/>"
								+ "<attribute index=\"3\"/>"
							+ "</entity>"
						+ "</datapackage>";	
				}
				else {
					tempQuery +=
						"<staticItem name=\"demographic_1\" value=\"\" />"
						+ "<staticItem name=\"demographic_2\" value=\"\" />"
						+ "<staticItem name=\"demographic_3\" value=\"\" />";
				}
			}
				
			//select the metadata
			tempQuery += metadataAttributeSelection;
	
			//select the question metadata						
			if (questionId.length > 0) {
				tempQuery +=
				"<datapackage id=\"" + questionId + "\">"
					+ "<entity id=\"" + questionId + "\">"
						+ "<attribute name=\"qId\">"
							+ "<pathexpr label=\"qId\">//assessment/section/item/@ident</pathexpr>"
						+ "</attribute>"
						+ "<attribute name=\"qTitle\">"
							+ "<pathexpr label=\"qTitle\">//assessment/section/item/@title</pathexpr>"
						+ "</attribute>"
						+ "<attribute name=\"qLabel\">"
							+ "<pathexpr label=\"qLabel\">//assessment/section/item/presentation/@label</pathexpr>"
						+ "</attribute>"
						+ "<attribute name=\"qPrompt\">"
							+ "<pathexpr label=\"qPrompt\">(//assessment/section/item/presentation/flow/response_lid/render_choice/material/mattext)|(//assessment/section/item/presentation/flow/response_str/material/mattext)|(//assessment/section/item/presentation/flow/material/mattext)</pathexpr>"
						+ "</attribute>";
				//control multiple choices
				for (var p = 1; p <= questionChoiceCount; p++) {
					tempQuery +=
						"<attribute name=\"qChoice_" + p + "\">"
							+ "<pathexpr label=\"qChoice\">//assessment/section/item/presentation/flow/response_lid/render_choice/flow_label/response_label/material/mattext</pathexpr>"
						+ "</attribute>";
				}
				//at least one metadata field
				tempQuery +=
					"<attribute name=\"qMetadataVocabulary\">"
						+ "<pathexpr label=\"qMetadataVocabulary\">//assessment/section/item/itemmetadata/qtimetadata/vocabulary</pathexpr>"
					+ "</attribute>"
					+ "<attribute name=\"qMetadataValue\">"
						+ "<pathexpr label=\"qMetadataValue\">//assessment/section/item/itemmetadata/qtimetadata/qtimetadatafield/fieldentry</pathexpr>"
					+ "</attribute>";
				//control multiple metadata fields
				for (var q = 2; q <= questionMetadataCount; q++) {
					tempQuery +=
						"<attribute name=\"qMetadataVocabulary_" + q +"\">"
							+ "<pathexpr label=\"qMetadataVocabulary\">//assessment/section/item/itemmetadata/qtimetadata/vocabulary</pathexpr>"
						+ "</attribute>"
						+ "<attribute name=\"qMetadataValue_" + q +"\">"
							+ "<pathexpr label=\"qMetadataValue\">//assessment/section/item/itemmetadata/qtimetadata/qtimetadatafield/fieldentry</pathexpr>"
						+ "</attribute>";
				}
				tempQuery +=
					"</entity>"
				+ "</datapackage>";
			}
				
			tempQuery += "</selection>";
			
			//join to the question "table"
			if (questionId.length > 0) {
				tempQuery +=
					"<where>"
						+ "<condition type=\"join\">"
							+ "<left>"
								+ "<datapackage id=\"" + docId + "\">"
									+ "<entity index=\"0\">"
										+ "<attribute index=\"1\"/>"
									+ "</entity>"
								+ "</datapackage>"
							+ "</left>"
							+ "<operator>=</operator>"
							+ "<right>"
								+ "<datapackage id=\"" + questionId + "\">"
									+ "<entity id=\"" + questionId + "\">"
										+ "<attribute name=\"qId\">"
											+ "<pathexpr label=\"qId\">//assessment/section/item/@ident</pathexpr>"
										+ "</attribute>"
									+ "</entity>"
								+ "</datapackage>"
							+ "</right>"
						+ "</condition>"
					+ "</where>";
			}
			
			tempQuery += "</query>";
		
		} // for questionId loop
		
	
	} //for docObjs loop
				 
	tempQuery +=
		 "</union>"	
		 + "</dataquery>";
	
	//alert(tempQuery);
             
    return tempQuery;
}

/**
*	@param searchTerms - and object (hashtable) with, pay attention now:
	keys are search values
*   values are pathexprs
**/
function generateSearchString(
		searchTerms,
		assessmentItemIds,
		operator, 
		searchAssessments, 
		searchAssessmentItems) {
	var queryString = 
		"<pathquery version=\"1.2\">"
			+"<querytitle>Web-Search</querytitle>";
		
	/** assessments **/
	if (searchAssessments) {
	
		queryString +=
			"<returndoctype>edml://ecoinformatics.org/edml</returndoctype>"
			
			//assessment fields
               +"<returnfield>assessment/duration</returnfield>"
               +"<returnfield>assessment/title</returnfield>"
               +"<returnfield>assessment/@id</returnfield>"
               +"<returnfield>dataset/dataTable/entityName</returnfield>"
               +"<returnfield>lom/general/title/string</returnfield>"
               +"<returnfield>lom/general/keyword/string</returnfield>"
               +"<returnfield>individualName/surName</returnfield>"
               +"<returnfield>organizationName</returnfield>"
               
               +"<returnfield>assessmentItems/assessmentItem/assessmentItemId</returnfield>";
               
	}
						
	/** questions **/
	if (searchAssessmentItems) {
		queryString +=
			"<returndoctype>http://www.imsglobal.org/xsd/ims_qtiasiv1p2</returndoctype>"
			
			//question (qti) fields
			+"<returnfield>item/@title</returnfield>"
			+"<returnfield>item/@ident</returnfield>"
               +"<returnfield>qtimetadata/qtimetadatafield/fieldlabel</returnfield>"
               +"<returnfield>qtimetadata/qtimetadatafield/fieldentry</returnfield>"
			//classification
			+"<returnfield>fieldlabel</returnfield>"
			+"<returnfield>fieldentry</returnfield>"
			+"<returnfield>objectives/material/mattext</returnfield>"
			//question content
			+"<returnfield>presentation/flow/response_lid/render_choice/material/mattext</returnfield>"
			+"<returnfield>response_label/@ident</returnfield>"
			+"<returnfield>response_label/material/mattext</returnfield>";
	}		
    
	// a query group for search terms
	var termQueryString = "";
    termQueryString +=
    	"<querygroup operator=\"" + operator + "\">";                    
    for (var i in searchTerms) {
    	var key = i;
    	var value = searchTerms[i];
    	//only if we have a value  
    	if (value.length > 0) {                   
    		termQueryString +=
				"<queryterm searchmode=\"contains\" casesensitive=\"false\">";
				if (key != "anyValue") {
					termQueryString += "<pathexpr>" + key + "</pathexpr>";
				}	
				termQueryString += "<value>" + value + "</value>";
				termQueryString +="</queryterm>";
		}
    }
    termQueryString += "</querygroup>";
	
	// querygroup for assessmentItemIds
	var itemQueryString = "";
	if (assessmentItemIds) {
		//the pathexpr
		var pathexpr = "assessmentItems/assessmentItem/assessmentItemId";
		// a query group for search terms
		itemQueryString +=
	    	"<querygroup operator=\"" + "UNION" + "\">";
	    for (var i in assessmentItemIds) {
	    	var itemId = assessmentItemIds[i];
	    	//only if we have a value  
	    	if (itemId.length > 0) {                   
	    		itemQueryString +=
					"<queryterm searchmode=\"contains\" casesensitive=\"false\">";
					if (key != "anyValue") {
						itemQueryString += "<pathexpr>" + pathexpr + "</pathexpr>";
					}	
					itemQueryString += "<value>" + itemId + "</value>";
					itemQueryString +="</queryterm>";
			}
	    }
	    itemQueryString += "</querygroup>";
	}
	
	//combine the various conditions
	queryString +=
    	"<querygroup operator=\"" + "UNION" + "\">";
	queryString += termQueryString;
	queryString += itemQueryString;
	queryString += "</querygroup>";
	
	//end the pathquery
	queryString += "</pathquery>";
	
	return queryString;
				
}


function generateAssessmentSearchString(assessmentItemId) {
	var query = 
		"<pathquery version='1.2'>"
		     +"<querytitle>Containing-Assessment-Search</querytitle>"
		     
		     +"<returndoctype>edml://ecoinformatics.org/edml</returndoctype>"
		                           
		     +"<returnfield>assessment/duration</returnfield>"
             +"<returnfield>assessment/title</returnfield>"
             +"<returnfield>assessment/@id</returnfield>"
             +"<returnfield>dataset/dataTable/entityName</returnfield>"
             +"<returnfield>lom/general/title/string</returnfield>"
             +"<returnfield>lom/general/keyword/string</returnfield>"
             +"<returnfield>individualName/surName</returnfield>"
             +"<returnfield>organizationName</returnfield>"
                      
             +"<returnfield>assessmentItems/assessmentItem/assessmentItemId</returnfield>";
                      
		    if (assessmentItemId.length > 0) {
			    query += "<querygroup operator='UNION'>";
			    
			    //add the assessmentId if included
			    query +=
			     		"<queryterm searchmode='contains' casesensitive='false'>"
			                  +"<value>"
			                  + assessmentItemId
			                  +"</value>"
			                  +"<pathexpr>assessmentItemId</pathexpr>"
			          +"</queryterm>";
				
			    //close the query group      
			     query +=
			     "</querygroup>";
			}
			     
		query += "</pathquery>";
		
	return query;	
}

function generateAssessmentListString(assessmentIds) {
	var query = 
		"<pathquery version='1.2'>"
		     +"<querytitle>Assessment-List</querytitle>"
		     
		     +"<returndoctype>edml://ecoinformatics.org/edml</returndoctype>"
		                           
                      +"<returnfield>assessment/title</returnfield>"
                      +"<returnfield>assessment/@id</returnfield>"
                      +"<returnfield>assessment/duration</returnfield>"
           			  +"<returnfield>dataset/dataTable/entityName</returnfield>"
                      +"<returnfield>lom/general/title/string</returnfield>"
                      +"<returnfield>lom/general/keyword/string</returnfield>"
                      +"<returnfield>individualName/surName</returnfield>"
                      +"<returnfield>organizationName</returnfield>"
                      
                      +"<returnfield>assessmentItems/assessmentItem/assessmentItemId</returnfield>";
                      
		    if (assessmentIds.length > 0) {
			    query += "<querygroup operator='UNION'>";
			    
			    //add the assessmentId if included
			    for (var i=0; i < assessmentIds.length; i++) {
				    query +=
				     		"<queryterm searchmode='equals' casesensitive='false'>"
				                  +"<value>"
				                  + assessmentIds[i]
				                  +"</value>"
				                  +"<pathexpr>@packageId</pathexpr>"
				          +"</queryterm>";
				}
			    //close the query group      
			     query +=
			     "</querygroup>";
			}
			     
		query += "</pathquery>";
		
	return query;	
}

function callAjax(metacatURL, myQuery, qfmt, divId, callback) {
														
	//alert("calling ajax: " + metacatURL);
	//alert("myQuery: " + myQuery);
	
	//var myRequest = new Ajax.Request(
	var myUpdate = new Ajax.Updater(
		divId,
		metacatURL,
		{	method: 'post',
			parameters: { action: 'squery', qformat: qfmt, query: myQuery},
			evalScripts: true, 
			onComplete: callback,
			onFailure: function(transport) {alert('failure making ajax call');}
		 });
		 
	 //alert("done calling ajax");
}
function getIframeDocument(iframeId) {
	//look up the document
	var iframe = window.document.getElementById(iframeId);
	var doc = null;
	if (iframe.contentDocument) {
		doc = iframe.contentDocument;
	}
	else {
		doc = iframe.contentWindow.document;
	}
	return doc;
}
