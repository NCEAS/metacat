 /*
  *     Purpose: Default style sheet for KNP project web pages 
  *              Using this stylesheet rather than placing styles directly in 
  *              the KNP web documents allows us to globally change the 
  *              formatting styles of the entire site in one easy place.
  */

var lastInputIndex = 0;
 
function getSearchModeList(searchModeString) {
	var abbrevArray = searchModeString.split('|');
	var smArray = new Array();
	
	for (i = 0; i < abbrevArray.length; i++) {
		if (abbrevArray[i] == 'co') {
			smArray.push(new Array('contains', 'contains','Contains'));
		} else if (abbrevArray[i] == 'sw') {
			smArray.push(new Array('starts-with', 'starts with', 'Starts With'));
		} else if (abbrevArray[i] == 'ew') {
			smArray.push(new Array('ends-with', 'ends with', 'Ends With'));
		} else if (abbrevArray[i] == 'eq') {
			smArray.push(new Array('equals', 'equals', 'Equals'));
		} else if (abbrevArray[i] == 'ne') {
			smArray.push(new Array('isnot-equal', 'is not equal', 'Is Not Equal'));
		} else if (abbrevArray[i] == 'gt') {
			smArray.push(new Array('greater-than', 'is greater than', 'Is Greater Than'));
		} else if (abbrevArray[i] == 'lt') {
			smArray.push(new Array('less-than', 'is less than', 'Is Less Than'));
		} else if (abbrevArray[i] == 'ge') {
			smArray.push(new Array('greater-than-equals', 'is greater than or equal to', 'Is Greater Than Or Equal To'));
		} else if (abbrevArray[i] == 'le') {
			smArray.push(new Array('less-than-equals', 'is less than or equal to', 'Is Less Than Or Equal To'));
		} else if (abbrevArray[i] == 'af') {
			smArray.push(new Array('greater-than', 'is after', 'Is After'));
		} else if (abbrevArray[i] == 'be') {
			smArray.push(new Array('less-than', 'is before', 'Is Before'));
		} else if (abbrevArray[i] == 'is') {
			smArray.push(new Array('equals', 'is', 'Is'));
		} else if (abbrevArray[i] == 'in') {
			smArray.push(new Array('isnot-equal', 'is not', 'Is Not'));
		}
	}
	
	return smArray;	
}
   
function addSearchDropdown(formName, inputLabel, inputIdBase, inputName, searchModes) {
	
	var formObj = document.getElementById(formName);
	var newdiv = document.createElement('div');
	var searchModeArray = getSearchModeList(searchModes);
  
  	var innerHtml = "";
	innerHtml += '    <div class="field-label dropdown-field-label">' + inputLabel + '</div>';
	innerHtml += '    <select class="dropdown-input" name="dd-' + inputIdBase + lastInputIndex + '">';
	for (i = 0; i < searchModeArray.length; i++ ) {
		innerHtml += '      <option value="' + searchModeArray[i][0] + '">' + searchModeArray[i][1] + '</option>';
	}
	innerHtml += '    </select>';
	innerHtml += '    <input class="text-input" id="tx-' + inputIdBase + lastInputIndex + '" name="' + inputName + '" type="text"/>';
	innerHtml += '  </div>' 

	newdiv.setAttribute('class','content-subsection');
	newdiv.innerHTML = innerHtml;
	formObj.appendChild(newdiv);
  
  	lastInputIndex++;
}

function addSearchDropdownBefore(selectObj) {
	//alert(selectObj.value);
	if (selectObj.value == "name") {
		addSearchSelectionBefore('tpcSearch','Name','name','co|eq|sw|ew','form-base-row');
	} else if (selectObj.value == "keyword") {
		addSearchSelectionBefore('tpcSearch','Keyword','keyword','co|eq|sw|ew','form-base-row');
	} else if (selectObj.value == "creator") {
		addSearchSelectionBefore('tpcSearch','Creator','creator','co|eq|sw|ew','form-base-row');
	} else if (selectObj.value == "description") {
		addSearchSelectionBefore('tpcSearch','Description','description','co|eq|sw|ew','form-base-row');
	} else if (selectObj.value == "date-executed") {
		addSearchSelectionBefore('tpcSearch','Date Executed','date-executed','be|af','form-base-row');
	} else if (selectObj.value == "workflow-run-lsid") {
		addSearchSelectionBefore('tpcSearch','Workflow-run-lsid','workflow-run-lsid','co|eq|sw|ew','form-base-row');
	} else if (selectObj.value == "workflow-lsid") {
		addSearchSelectionBefore('tpcSearch','Workflow-lsid','workflow-lsid','co|eq|sw|ew','form-base-row');
	} else if (selectObj.value == "status") {
		addSearchSelectionBefore('tpcSearch','Status','Status','is|in','form-base-row');
	} else if (selectObj.value == "date-created") {
		addSearchSelectionBefore('tpcSearch','Date Created','date-created','be|af','form-base-row');
	} /*else if (selectObj.value == "workflow-id") {
		addSearchSelectionBefore('tpcSearch','Workflow-id','workflow-id','co|eq|sw|ew','form-base-row');
	}*/	
}

function addSearchSelectionBefore(formId, inputLabel, inputName,  searchModes, beforeElementId) {
	//alert('in addDropdownInputBefore - formId: ' + formId + ' inputLabel: ' + inputLabel + ' inputName: ' + inputName + ' searchModes: ' + searchModes + ' beforeElementId: ' + beforeElementId);
	var formObj = document.getElementById(formId);
	var beforeObj = document.getElementById(beforeElementId);
	var newDiv = document.createElement('div');
	var searchModeArray = getSearchModeList(searchModes);
  
  	var innerHtml = '';
	innerHtml += '    <div class="field-label dropdown-field-label">' + inputLabel + '</div>';
	innerHtml += '    <select class="dropdown-input" id="sm-' + inputName + lastInputIndex + '">';
	for (i = 0; i < searchModeArray.length; i++ ) {
		innerHtml += '      <option value="' + searchModeArray[i][0] + '">' + searchModeArray[i][1] + '</option>';
	}
	innerHtml += '    </select>';
	innerHtml += '    <input class="text-input" id="sf-' + inputName + lastInputIndex + '" name="' + inputName + '" type="text"/>';
	innerHtml += '    <input class="field-remove-button" type="button" value="-" onclick="removeInput(\''+ formId + '\',\'cs-' + inputName + lastInputIndex + '\')"/>';     
	innerHtml += '</div>';   
	//alert('innerHtml: ' + innerHtml); 								

	// alert('formObj:' + formObj);
	newDiv.setAttribute('id', 'cs-' + inputName + lastInputIndex);
	newDiv.setAttribute('class', 'form-input-row');
	newDiv.innerHTML = innerHtml;
	// alert('newDiv:' + newDiv);
	formObj.insertBefore(newDiv, beforeObj);
		
	lastInputIndex++;
}

function removeInput(formId, inputId) {
	//alert('in remove input - formname: ' + formId + ' inputid: ' + inputId);
	var formObj = document.getElementById(formId);
	var inputObj = document.getElementById(inputId);

	if ((formObj != null) && (inputObj != null)) { 
		// alert('removing child: ' + inputObj.toString());
		formObj.removeChild(inputObj);
	}
}
  