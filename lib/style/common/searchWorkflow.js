 /*
  *   '$RCSfile$'
  *     Purpose: Basic funtions to support searching workflows
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

/*
 * Generate a workflow query string.  this assumes that search fields meet the
 * following criteria in the web page:
 * -- search input fields have an ID that starts with sf- 
 * -- if there is a search mode dropdown for an input field in the form, it's ID 
 *    should use the same convention as the input field, but start with sm-
 *    (i.e. the search mode input for the sf-firstname input would be sm-firstname) 
 */
function setWorkflowQueryFormField(formId) {
	var queryString = ""; 
	queryString += "<pathquery version='1.2'>";
	/*queryString += "<returndoctype>entity</returndoctype>";
	queryString += "<returndoctype>-//UC Berkeley//DTD MoML 1//EN</returndoctype>";
	queryString += "<returnfield>/entity/@name</returnfield>";
	queryString += "<returnfield>/entity/property[@name=\'KeplerDocumentation\']/property[@name=\'author\']/configure</returnfield>";
	queryString += "<returnfield>/entity/property[@name=\'KeplerDocumentation\']/property[@name=\'description\']/configure</returnfield>";
	queryString += "<returnfield>/entity/property[@name=\'KeplerDocumentation\']/property[@name=\'createDate\']/configure</returnfield>";
	queryString += "<returnfield>/entity/property[@name=\'KeplerDocumentation\']/property[@name=\'workflowId\']/configure</returnfield>";
	queryString += "<returnfield>/entity/property[@name=\'karLSID\']/@value</returnfield>";				
	queryString += "<returnfield>/entity/property[@name=\'entityId\']/@value</returnfield>";*/
	/*queryString += "<returndoctype>kar</returndoctype>";*/
	queryString += "<returndoctype>http://www.kepler-project.org/kar-2.0.0</returndoctype>";
	queryString += "<returndoctype>http://www.kepler-project.org/kar-2.1.0</returndoctype>";
  queryString += "<returnfield>karEntry/karEntryXML/entity/@name</returnfield>";
  queryString += "<returnfield>karEntry/karEntryXML/entity/property[@name=\'KeplerDocumentation\']/property[@name=\'author\']/configure</returnfield>";
  queryString += "<returnfield>karEntry/karEntryXML/entity/property[@name=\'KeplerDocumentation\']/property[@name=\'description\']/configure</returnfield>";
  queryString += "<returnfield>karEntry/karEntryXML/entity/property[@name=\'KeplerDocumentation\']/property[@name=\'createDate\']/configure</returnfield>";
  queryString += "<returnfield>karEntry/karEntryXML/entity/property[@name=\'KeplerDocumentation\']/property[@name=\'workflowId\']/configure</returnfield>";
  queryString += "<returnfield>mainAttributes/lsid</returnfield>";       
  queryString += "<returnfield>karEntry/karEntryXML/entity/property[@name=\'entityId\']/@value</returnfield>";
	queryString += "<returnfield>karEntry/karEntryXML/property[@name=\'WorkflowRun\']/@class</returnfield>";
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
	} /*else if (sfElement.name == 'date-executed') {
		pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
		pathExpr += "<value>" + sfElement.value + "</value>";
		pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/property/configure</pathexpr>";
		pathExpr += "</queryterm>";		
	} */ else if (sfElement.name == 'workflow-lsid') {
		pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
		pathExpr += "<value>entityId</value>";
		pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/@name</pathexpr>";
		pathExpr += "</queryterm>"; 
		pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
		pathExpr += "<value>" + sfElement.value + "</value>";
		pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/@value</pathexpr>";
		pathExpr += "</queryterm>";		
	}/* else if (sfElement.name == 'workflow-run-id') {
		pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
		pathExpr += "<value>" + sfElement.value + "</value>";
		pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/property/configure</pathexpr>";
		pathExpr += "</queryterm>"; 		
	} else if (sfElement.name == 'status') {
		pathExpr += "<queryterm casesensitive='false' searchmode='" + searchMode + "'>";
		pathExpr += "<value>" + sfElement.value + "</value>";
		pathExpr += "<pathexpr>karEntry/karEntryXML/entity/property/property/configure</pathexpr>";
		pathExpr += "</queryterm>"; 		
	}*/
	
	//alert("returning path expression: " + pathExpr);
	return pathExpr;
}

	
