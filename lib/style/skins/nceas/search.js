function trim(stringToTrim) {
    return stringToTrim.replace(/^\s*/, '').replace(/\s*$/,'');
}

function checkSearch(submitFormObj) {
    var searchString = trim(submitFormObj.searchstring.value);
    var checkBox = document.getElementById("searchAll");

    if (searchString=="") {
        if (confirm("Show *all* data in the KNB?")) {
            searchString = "%";
        } else {
            return false;
        }
    }

    if (!checkBox.checked && searchString!="%") {
        submitFormObj.query.value = "<pathquery version=\"1.2\">"
            +"<querytitle>Web-Search</querytitle>"
            +"<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>"
            +"<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>"
            +"<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>"
            +"<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>"
            +"<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype>"
            +"<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN</returndoctype>"
            +"<returndoctype>-//NCEAS//resource//EN</returndoctype>"
            +"<returndoctype>-//NCEAS//eml-dataset//EN</returndoctype>"
            +"<returndoctype>metadata</returndoctype>"
            +"<returnfield>originator/individualName/surName</returnfield>"
            +"<returnfield>originator/individualName/givenName</returnfield>"
            +"<returnfield>creator/individualName/surName</returnfield>"
            +"<returnfield>creator/individualName/givenName</returnfield>"
            +"<returnfield>originator/organizationName</returnfield>"
            +"<returnfield>creator/organizationName</returnfield>"
            +"<returnfield>dataset/title</returnfield>"
            +"<returnfield>dataset/title/value</returnfield>"
            +"<returnfield>keyword</returnfield>"
            +"<returnfield>keyword/value</returnfield>"
            //fgdc fields
            +"<returnfield>idinfo/citation/citeinfo/title</returnfield>"
            +"<returnfield>idinfo/citation/citeinfo/origin</returnfield>"
			+"<returnfield>idinfo/keywords/theme/themekey</returnfield>"
            +"<querygroup operator=\"INTERSECT\">"
            	+"<querygroup operator=\"UNION\">"
	                +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
	                    +"<value>National Center for Ecological Analysis and Synthesis</value>"
	                    +"<pathexpr>organizationName</pathexpr>"
	                +"</queryterm>"
	                +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
	                    //+"<value>%</value>"
	                    +"<value>National Center for Ecological Analysis and Synthesis</value>"	                    
	                    +"<pathexpr>placekey</pathexpr>"
	                +"</queryterm>"
                +"</querygroup>"
                +"<querygroup operator=\"UNION\">"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>surName</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>givenName</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>keyword</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
	                    +"<value>" + searchString + "</value>"
	                    +"<pathexpr>keyword/value</pathexpr>"
	                +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>para</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>geographicDescription</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>literalLayout</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>title</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
	                    +"<value>" + searchString + "</value>"
	                    +"<pathexpr>title/value</pathexpr>"
	                +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>@packageId</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>abstract/para</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
	                    +"<value>" + searchString + "</value>"
	                    +"<pathexpr>abstract/para/value</pathexpr>"
	                +"</queryterm>"
                    
                    //FGDC fields
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>abstract</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>idinfo/citation/citeinfo/title</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>idinfo/citation/citeinfo/origin</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>idinfo/keywords/theme/themekey</pathexpr>"
                    +"</queryterm>"
                    +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                        +"<value>" + searchString + "</value>"
                        +"<pathexpr>placekey</pathexpr>"
                    +"</queryterm>"
                +"</querygroup>"
            +"</querygroup>"
            +"</pathquery>";

    } else {
        queryTermString = "";
        if (searchString != "%"){
            queryTermString = "<queryterm searchmode=\"contains\" casesensitive=\"false\">"
                                  +"<value>" + searchString + "</value>"
                              +"</queryterm>";
        }
        submitFormObj.query.value = "<pathquery version=\"1.2\">"
            +"<querytitle>Web-Search</querytitle>"
            +"<returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype>"
            +"<returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype>"
            +"<returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype>"
            +"<returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype>"
            +"<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype>"
            +"<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN</returndoctype>"
            +"<returndoctype>-//NCEAS//resource//EN</returndoctype>"
            +"<returndoctype>-//NCEAS//eml-dataset//EN</returndoctype>"
            +"<returndoctype>metadata</returndoctype>"
            +"<returnfield>originator/individualName/surName</returnfield>"
            +"<returnfield>originator/individualName/givenName</returnfield>"
            +"<returnfield>creator/individualName/surName</returnfield>"
            +"<returnfield>creator/individualName/givenName</returnfield>"
            +"<returnfield>originator/organizationName</returnfield>"
            +"<returnfield>creator/organizationName</returnfield>"
            +"<returnfield>dataset/title</returnfield>"
            +"<returnfield>dataset/title/value</returnfield>"
            +"<returnfield>keyword</returnfield>"
            +"<returnfield>keyword/value</returnfield>"
            //fgdc fields
            +"<returnfield>idinfo/citation/citeinfo/title</returnfield>"
            +"<returnfield>idinfo/citation/citeinfo/origin</returnfield>"
			+"<returnfield>idinfo/keywords/theme/themekey</returnfield>"
            +"<querygroup operator=\"INTERSECT\">"
                +"<querygroup operator=\"UNION\">"
	                +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
	                    +"<value>National Center for Ecological Analysis and Synthesis</value>"
	                    +"<pathexpr>organizationName</pathexpr>"
	                +"</queryterm>"
	                +"<queryterm searchmode=\"contains\" casesensitive=\"false\">"
	                    //+"<value>%</value>"
	                    +"<value>National Center for Ecological Analysis and Synthesis</value>"	                    
	                    +"<pathexpr>placekey</pathexpr>"
	                +"</queryterm>"
                +"</querygroup>"
                + queryTermString
            +"</querygroup>"
            +"</pathquery>";

    }
    return true;
}

function browseAll(searchFormId) {
	var searchForm = document.getElementById(searchFormId);
	var searchString = searchForm.searchstring;
    var checkBox = document.getElementById("searchAll");
    searchString.value="";
    checkBox.checked = true;
    if (checkSearch(searchForm)) {
		searchForm.submit();
	}

}

function searchAll(){
    var checkBox = document.getElementById("searchCheckBox");
    if (checkBox.checked == true) {
        alert("You have selected to search all possible existing fields. This search will take longer.");
    }
}
