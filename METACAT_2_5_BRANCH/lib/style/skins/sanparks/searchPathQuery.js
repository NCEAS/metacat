function encodeXML(theString) {
	return theString.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;');
}

function generateQueryString(organizationScope, anyValue, searchFields) {
	// make sure it is valid XML
	var searchTerm = encodeXML(anyValue);
	
	var queryString = ""; 
	queryString += "<pathquery version='1.2'>";
	queryString += "<returndoctype>metadata</returndoctype>";
	queryString += "<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype>";
	queryString += "<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN</returndoctype>";
	queryString += "<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>";
	queryString += "<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>";
	queryString += "<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>";
	queryString += "<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>";
	queryString += "<returndoctype>-//NCEAS//eml-dataset-2.0//EN</returndoctype>";
	queryString += "<returndoctype>-//NCEAS//resource//EN</returndoctype>";
	queryString += "<returnfield>originator/individualName/surName</returnfield>";
	queryString += "<returnfield>originator/individualName/givenName</returnfield>";
	queryString += "<returnfield>originator/organizationName</returnfield>";
	queryString += "<returnfield>creator/individualName/surName</returnfield>";
	queryString += "<returnfield>creator/organizationName</returnfield>";
	queryString += "<returnfield>dataset/title</returnfield>";
	queryString += "<returnfield>dataset/title/value</returnfield>";
	queryString += "<returnfield>keyword</returnfield>";
	queryString += "<returnfield>keyword/value</returnfield>";
	queryString += "<returnfield>creator/individualName/givenName</returnfield>";
	queryString += "<returnfield>idinfo/citation/citeinfo/title</returnfield>";
	queryString += "<returnfield>idinfo/citation/citeinfo/origin</returnfield>";
	queryString += "<returnfield>idinfo/keywords/theme/themekey</returnfield>";
	
	queryString += "<querygroup operator='INTERSECT'>";
	
	//search particular fields, or all?
	if (searchFields.length > 0) {
		queryString += "<querygroup operator='UNION'>";
		for (var i = 0; i < searchFields.length; i++) {
			queryString += "<queryterm casesensitive='false' searchmode='contains'>";
			queryString += "<value>" + searchTerm + "</value>";
			queryString += "<pathexpr>" + searchFields[i] +"</pathexpr>";
			queryString += "</queryterm>";
		}
		queryString += "</querygroup>";
	}
	else {
		queryString += "<queryterm casesensitive='false' searchmode='contains'>";
		queryString += "<value>" + searchTerm + "</value>";
		queryString += "</queryterm>";
	}
	
	//now limit by the organization
	queryString += "<querygroup operator='UNION'>";
	
	for (var i = 0; i < organizationScope.length; i++) {
		queryString += "<queryterm casesensitive='false' searchmode='contains'>";
		queryString += "<value>" + organizationScope[i] + "</value>";
		queryString += "<pathexpr>placekey</pathexpr>";
		queryString += "</queryterm>";
		
		queryString += "<queryterm casesensitive='false' searchmode='contains'>";
		queryString += "<value>" + organizationScope[i] + "</value>";
		queryString += "<pathexpr>keyword</pathexpr>";
		queryString += "</queryterm>";
	}
	
	queryString += "</querygroup>";
	
	queryString += "</querygroup>";
	
	queryString += "</pathquery>";
	
	//alert(queryString);
	
	return queryString;
}

function setQueryFormField() {
	//alert('setQueryFormField');
	var queryField = document.getElementById("query");
	//alert('queryField=' + queryField);
	var anyfieldField = document.getElementById("anyfield");
	//alert('anyfieldField=' + anyfieldField);
	var organizationScopeField = document.getElementById("organizationScope");
	//alert('organizationScopeField=' + organizationScopeField.value);
	var searchAll = document.getElementById("searchAll");
	//alert('searchAll=' + searchAll.checked);
	
	//make the array for organization
	var orgArray = new Array();
	orgArray[0] = organizationScopeField.value;
	//if "All", include multiple entries
	if (organizationScopeField.value == '') {
		orgArray[0] = "SANParks, South Africa";
		orgArray[1] = "SAEON, South Africa";
	}
	
	//make the array for paths to search
	var searchFieldArray = new Array();
	if (!searchAll.checked) {
		var counter = 0;
		//EML fields
		searchFieldArray[counter++] = "abstract/para";
		searchFieldArray[counter++] = "abstract/para/value";
		searchFieldArray[counter++] = "surName";
		searchFieldArray[counter++] = "givenName";
		searchFieldArray[counter++] = "organizationName";		
		searchFieldArray[counter++] = "title";
		searchFieldArray[counter++] = "title/value";
		searchFieldArray[counter++] = "keyword";
		searchFieldArray[counter++] = "keyword/value";
		searchFieldArray[counter++] = "para";
		searchFieldArray[counter++] = "geographicDescription";
		searchFieldArray[counter++] = "literalLayout";
		searchFieldArray[counter++] = "@packageId";
		
		//FGDC fields
		searchFieldArray[counter++] = "abstract";
		searchFieldArray[counter++] = "idinfo/citation/citeinfo/title";
		searchFieldArray[counter++] = "idinfo/citation/citeinfo/origin";
		searchFieldArray[counter++] = "idinfo/keywords/theme/themekey";
		searchFieldArray[counter++] = "placekey";
	}
	
	//generate the query
	queryField.value = 
		generateQueryString(
			orgArray,
			anyfieldField.value,
			searchFieldArray);
	
	//alert(queryField.value);
}

function setBrowseAll() {
	//set the field to wildcard
	var anyfieldField = document.getElementById("anyfield");
	anyfieldField.value = "";
	
	//set the query
	setQueryFormField();
}