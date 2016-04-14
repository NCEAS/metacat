function getParams() {
    var paramsArray = document.location.href.split('?')[1].split('&');
    for (var i = 0; i < paramsArray.length; i++) {
        var temp = paramsArray[i].split('=');
        alert(unescape(temp[0]) + '=' + unescape(temp[1]));
    }
}

function addParagraph() {
    var newParaWidget = document.createElement("textarea");
    newParaWidget.setAttribute("rows", "6");
    newParaWidget.className="span8";
    newParaWidget.setAttribute("name", "methodPara");

    var TR = document.createElement("tr");
    TR.className='sectbody';
    var TD = createTD("1","left");
    var emptyTD = createTD("","right");
    TD.appendChild(newParaWidget);
    //TR.appendChild(emptyTD);
    TR.appendChild(TD);
    var addParaButton = document.getElementById("addparabutton1");
    var parent = addParaButton.parentNode;
    parent.insertBefore(TR, addParaButton);
}

function addMethod() {
    var index = incrementCount('methodCount');
    var methodButton = document.getElementById("addmethodbutton");
    var parent = methodButton.parentNode;
    
    parent.insertBefore(createHRRow(), methodButton);
    parent.insertBefore(addMethodTitle(), methodButton);
    parent.insertBefore(addMethodDescription(index), methodButton);
    parent.insertBefore(addMethodButton(index), methodButton);
}

function  addMethodButton(index) {
    var TR = document.createElement("tr");
    var buttonId = 'addparabutton' + index;
    TR.className='sectbody';
    TR.setAttribute("id", buttonId);
    var labelTD = createTD("","right");
    var TD = createTD("5","left");
    var button = document.createElement("input");
    button.setAttribute("type", "button");
    button.setAttribute("class", "btn"); 
    button.setAttribute("value", "Add Paragraph to Method Description"); 
    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
        button.attachEvent("onclick", new Function("addParagraph("+ index + ", '" + buttonId + "')"));
    } else {
        button.setAttribute("onclick", "addParagraph("+ index +", '" + buttonId + "')"); 
    }
    TD.appendChild(button);
    TR.appendChild(labelTD);
    TR.appendChild(TD);
    return TR;
}

function  addMethodTitle() {
    var TR = document.createElement("tr");
    TR.className='sectbody';
    var labelTD = createTD("","right");
    labelTD.appendChild(document.createTextNode("Method Title"));
    var TD = createTD("5","left");
    var textField = document.createElement("input");
    textField.setAttribute("type", "text"); 
    textField.setAttribute("name", "methodTitle"); 
    textField.className="longwidth";
    TD.appendChild(textField);
    TR.appendChild(labelTD);
    TR.appendChild(TD);
    return TR;
}

function  addMethodDescription(index) {
    var TR = document.createElement("tr");
    TR.className='sectbody';
    var labelTD = createTD("","right");
    labelTD.setAttribute("vAlign","top");
    labelTD.appendChild(document.createTextNode("Method Description"));
    var TD = createTD("5","left");
    var textArea = document.createElement("textarea");
    textArea.setAttribute("name", "methodPara"+index);
    textArea.setAttribute("rows", 6); 
    textArea.className="span8";
    TD.appendChild(textArea);
    TR.appendChild(labelTD);
    TR.appendChild(TD);
    return TR;
}

function createTextField(name, size, value) {
    var newField = document.createElement("input");
    newField.setAttribute("type", "text"); 
    newField.setAttribute("name", name); 
    newField.setAttribute("size", size); 
    newField.value = value; 
    return newField;
}

function createTextRow(count, varName, labelText, value) {
    var textField = createTextField(varName + count, "30", value);
    var row = createRow(count, textField, labelText);
    return row;
}

function createRow(count, elem, labelText) {
    var textTD = createTD("5");
    textTD.appendChild(elem);

    var labelTD = createTD("","right");

    var labelSpan = document.createElement("span");
    labelSpan.className = 'label';

    var label = document.createTextNode(labelText);
    labelSpan.appendChild(label);
    labelTD.appendChild(labelSpan);

    var TR = document.createElement("tr");
    TR.className='sectbody';
    TR.appendChild(labelTD);
    TR.appendChild(textTD);

    return TR;
}

function createHRRow() {
    var hrCell = createTD("6");
    var hr = document.createElement("hr");
    hr.setAttribute("width", "85%");
    hrCell.appendChild(hr);

    var hrTR = document.createElement("tr");
    hrTR.className = 'sectbody';
    hrTR.appendChild(hrCell);

    return hrTR;
}

function createHiddenInput(name, value) {
    var elem = document.createElement("input");
    elem.setAttribute("type", "hidden");
    elem.setAttribute("name", name);
    elem.setAttribute("value", value);
    return elem
}

function sortInputTags() {
    sortTagWithAttributeName("assocPartyFirstName", "addAssociatedParty");
    sortTagWithAttributeName("assocPartyLastName", "addAssociatedParty");
    sortTagWithAttributeName("assocPartyRole", "addAssociatedParty", "select");
    
    sortTagWithAttributeName("taxonName", "addTaxon");
    sortTagWithAttributeName("taxonRank", "addTaxon");
    
    sortTagWithAttributeName("keyword", "addKeyword");
    sortTagWithAttributeName("keywordType", "addKeyword");
    sortTagWithAttributeName("keywordTh", "addKeyword");
}


function sortTagWithAttributeName(tag, afterTag, parentTag) {
    var elem = document.getElementById(afterTag);
    
    elem = elem.nextSibling;
    
    while (elem && elem.nodeName == "TR") {
    var nodes = elem.getElementsByTagName("input");
        for(var i = 0; i < nodes.length; i++) {
            var node = nodes.item(i); 
            if (node.getAttribute("name")==tag) {
                    var parent = node.parentNode;
                    var clone = node.cloneNode(true);
                    parent.removeChild(node);
                    parent.appendChild(clone);
            }
        }
        if (parentTag != null) {
            var nodes = elem.getElementsByTagName(parentTag);
            for(var i = 0; i < nodes.length; i++) {
                var node = nodes.item(i); 
                if (node.getAttribute("name")==tag) {
                        var parent = node.parentNode;
                        var clone = node.cloneNode(true);
                        clone.value = node.value;
                        parent.removeChild(node);
                        parent.appendChild(clone);
                }
            }
        }
        elem = elem.nextSibling;
    }
}

function incrementCount(_count) {
    var countField = document.getElementById(_count);
    var count = countField.getAttribute("value");
    count++;
    countField.setAttribute("value", count);
    return count;
}

function decrementCount(_count) {
    var countField = document.getElementById(_count);
    var count = countField.getAttribute("value");
    count--;
    countField.setAttribute("value", count);
    return count;
}

function setCount(_count, val) {
    var countField = document.getElementById(_count);
    var count = countField.getAttribute("value");
    countField.setAttribute("value", val);
    return count;
}

/** 
 * Deprecated in favor of createIconLink() method
 * 
function createImageLink(imgSrc, imgAlt, imgOnClick, cursor) {
    var link = document.createElement("a");
    var img = document.createElement("img");
    img.setAttribute("src", imgSrc);
    img.setAttribute("alt", imgAlt);
    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
        link.style.setAttribute("cursor", cursor);
        img.attachEvent("onclick", new Function(imgOnClick));
    } else {
        link.setAttribute("style", "cursor:"+cursor);
        img.setAttribute("onClick", imgOnClick);
    }
    img.setAttribute("border", "0");
    link.appendChild(img);        
    return link;
}
*/

function createIconLink(iconClass, iconAlt, iconOnClick, cursor) {
    var link = document.createElement("a");
    var icon = document.createElement("i");
    icon.setAttribute("class", iconClass);
    icon.setAttribute("alt", iconAlt);
    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
        link.style.setAttribute("cursor", cursor);
        icon.attachEvent("onclick", new Function(iconOnClick));
    } else {
        link.setAttribute("style", "cursor:"+cursor);
        icon.setAttribute("onClick", iconOnClick);
    }
    link.appendChild(icon);        
    return link;
}


function createTD(colSpan, align, cursor) {
    var td = document.createElement("td");
    if (colSpan != "")
        td.setAttribute("colSpan", colSpan);
    td.setAttribute("align", align);
    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
        td.style.setAttribute("cursor", cursor);
    } else {
        td.setAttribute("style", "cursor:"+cursor);
    }
    return td;
}


function cleanTextNodes(startTagId, endTagId) {
    var startTag = document.getElementById(startTagId);

    var bro = startTag.nextSibling;       

    while(bro) {
        if (bro.nodeName == "#text") {
            var temp = bro.nextSibling;
            bro.parentNode.removeChild(bro);
            bro = temp;
        } else {
            var id = bro.getAttribute("id");
            if (id == endTagId) {
              return;
            }
            bro = bro.nextSibling;
        }
    }

}

function delRow(evt) {
    evt = (evt) ? evt : ((window.event) ? window.event : null);
    if (evt) {
        // equalize W3C/IE models to get event target reference
        var elem = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
        if (elem) {
            try {
                var table = elem.parentNode.parentNode.parentNode.parentNode;
                var row = elem.parentNode.parentNode.parentNode;

                table.removeChild(row);
            }
            catch(e) {
                var msg = (typeof e == "string") ? e : ((e.message) ? e.message : "Unknown Error");
                alert("Error:\n" + msg);
                return;
            }
        }
    }
}

function moveUpRow(evt) {
    evt = (evt) ? evt : ((window.event) ? window.event : null);
    if (evt) {
        // equalize W3C/IE models to get event target reference
        var elem = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
        if (elem) {
            try {
                var table = elem.parentNode.parentNode.parentNode.parentNode;
                var row = elem.parentNode.parentNode.parentNode;
                
                var bro = row.previousSibling;
                if (bro != null && bro.previousSibling.nodeName =="TR") {
                    clone = row.cloneNode(true);
                    table.insertBefore(clone, bro);
                    table.removeChild(row);
                }
            }
            catch(e) {
                var msg = (typeof e == "string") ? e : ((e.message) ? e.message : "Unknown Error");
                alert("Error:\n" + msg);
                return;
            }
        }
    }
}

function moveDownRow(evt, lastTR) {
    evt = (evt) ? evt : ((window.event) ? window.event : null);
    if (evt) {
        // equalize W3C/IE models to get event target reference
        var elem = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
        if (elem) {
            try {
                var table = elem.parentNode.parentNode.parentNode.parentNode;
                var row = elem.parentNode.parentNode.parentNode;
                
                var _test = row.nextSibling;
                if (_test.getAttribute("id") == lastTR) {
                    return;
                }
                var bro = row.nextSibling.nextSibling;
                if (bro != null && bro.nodeName =="TR") {
                    clone = row.cloneNode(true);
                    table.insertBefore(clone, bro);
                    table.removeChild(row);
                }
            }
            catch(e) {
                var msg = (typeof e == "string") ? e : ((e.message) ? e.message : "Unknown Error");
                alert("Error:\n" + msg);
                return;
            }
        }
    }
}

function addAssociatedParty() {
	var assocPartyFirstName  = document.getElementById("assocPartyFirstName");
	var assocPartyLastName   = document.getElementById("assocPartyLastName");
	var assocPartyRole       = document.getElementById("assocPartyRole");
	var assocPartyOrgName    = document.getElementById("assocPartyOrgName");
	var assocPartyEmail      = document.getElementById("assocPartyEmail");
	var assocPartyPhone      = document.getElementById("assocPartyPhone");
	var assocPartyFAX        = document.getElementById("assocPartyFAX");
	var assocPartyDelivery   = document.getElementById("assocPartyDelivery");
	var assocPartyCity       = document.getElementById("assocPartyCity");
	var assocPartyState      = document.getElementById("assocPartyState");
	var assocPartyStateOther = document.getElementById("assocPartyStateOther");
	var assocPartyZip        = document.getElementById("assocPartyZip");
	var assocPartyCountry    = document.getElementById("assocPartyCountry");

	if (assocPartyLastName.value != "" || assocPartyOrgName.value != "") {
	    var assocPartyCount = incrementCount("assocPartyCount");

			try {
				var assocPartyRow = createPartyRow(
					assocPartyCount, 
					assocPartyLastName.value, 
					assocPartyFirstName.value, 
					assocPartyRole.options[assocPartyRole.selectedIndex],
					assocPartyOrgName.value,   
					assocPartyEmail.value,     
					assocPartyPhone.value,     
					assocPartyFAX.value,       
					assocPartyDelivery.value,  
					assocPartyCity.value,      
					assocPartyState.options[assocPartyState.selectedIndex],     
					assocPartyStateOther.value,
					assocPartyZip.value,       
					assocPartyCountry.value);
			
			} catch(e) {
				var msg = (typeof e == "string") ? e : ((e.message) ? e.message : "Unknown Error");
				alert("Error:\n" + msg);
				return;
				
			}
			
			var assocPartyRowMarker = document.getElementById("assocPartyRowMarker");
			var parent = assocPartyRowMarker.parentNode;
			
			parent.insertBefore(assocPartyRow, assocPartyRowMarker);
			
			/* Clear the form */
			assocPartyFirstName.value = "";
			assocPartyLastName.value = "";
			assocPartyRole.selectedIndex = 0;
			assocPartyOrgName.value = "";
			assocPartyEmail.value = "";     
			assocPartyPhone.value = "";     
			assocPartyFAX.value = "";       
			assocPartyDelivery.value = "";  
			assocPartyCity.value = "";      
			assocPartyState.selectedIndex = 0;  
			assocPartyStateOther.value = "";
			assocPartyZip.value = "";       
			assocPartyCountry.value = "";

			/* Clear the selected row id */
			var selectedRowInput = document.getElementById("selectedRow");
			if ( selectedRowInput != null ) {
				selectedRowInput.parentElement.removeChild(selectedRowInput);
				
			}

      /* Hide the update party button if it is showing */
			hideUpdatePartyButton();
							
	} else {
		alert("Please enter a person's last name or an organization name at a minimum.");
		
	}
}

function createPartyRow(assocPartyCount, assocPartyLastName, assocPartyFirstName, assocPartyRole, assocPartyOrgName, assocPartyEmail, assocPartyPhone, assocPartyFAX, assocPartyDelivery, assocPartyCity, assocPartyState, assocPartyStateOther, assocPartyZip, assocPartyCountry) {    
    var assocPartyRow = document.createElement("tr");
		var idStr = Math.round(Math.random() * 10000000000000000).toString();
		assocPartyRow.setAttribute("id", idStr);
		
		var upCol = document.createElement("td");
		upCol.setAttribute("style", "text-align: center");
		upCol.appendChild(createIconLink("icon-arrow-up", "Move Up","moveUpRow(event)","pointer"));

		var downCol = document.createElement("td");
		downCol.setAttribute("style", "text-align: center");
		downCol.appendChild(createIconLink("icon-arrow-down", "Move Down", "moveDownRow(event, \"assocPartyRowMarker\")", "pointer"));

		var editCol = document.createElement("td");
		editCol.setAttribute("style", "text-align: center");
    editCol.appendChild(createIconLink("icon-pencil", "Edit", "editParty(event)", "pointer"));

		var delCol = document.createElement("td");
		delCol.setAttribute("style", "text-align: center");
    delCol.appendChild(createIconLink("icon-remove-sign", "Delete", "delRow(event)", "pointer"));

		var assocPartyFirstNameCol = document.createElement("td");
		var assocPartyFirstNameText = document.createTextNode(assocPartyFirstName);
		assocPartyFirstNameCol.appendChild(assocPartyFirstNameText);
		
		var assocPartyLastNameCol = document.createElement("td");
		var assocPartyLastNameText = document.createTextNode(assocPartyLastName);
		assocPartyLastNameCol.appendChild(assocPartyLastNameText);

		var assocPartyOrgNameCol = document.createElement("td");
		assocPartyOrgNameCol.setAttribute("colspan", "2");
		var assocPartyOrgNameText = document.createTextNode(assocPartyOrgName);
		assocPartyOrgNameCol.appendChild(assocPartyOrgNameText);

		var assocPartyRoleCol = document.createElement("td");
		var assocPartyRoleText = document.createTextNode(assocPartyRole.text);
		assocPartyRoleCol.appendChild(assocPartyRoleText);

    /*
    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
        editCol.attachEvent("onclick", new Function("assocPartyEditRow(event, 0, \"" + assocPartyFirstName  + "\",\""
                         + assocPartyLastName  + "\",\"" + assocPartyRole + "\")"));
    } else {
        editCol.setAttribute("onClick","assocPartyEditRow(event, 0, \"" + assocPartyFirstName  + "\",\""
                         + assocPartyLastName  + "\",\"" + assocPartyRole + "\")");
    }
		*/
		
    assocPartyRow.appendChild(createHiddenInput("partyFirstName", assocPartyFirstName));
    assocPartyRow.appendChild(createHiddenInput("partyLastName", assocPartyLastName));
    assocPartyRow.appendChild(createHiddenInput("partyRole", assocPartyRole.value));
    assocPartyRow.appendChild(createHiddenInput("partyOrgName", assocPartyOrgName));
    assocPartyRow.appendChild(createHiddenInput("partyEmail", assocPartyEmail));
    assocPartyRow.appendChild(createHiddenInput("partyPhone", assocPartyPhone));
    assocPartyRow.appendChild(createHiddenInput("partyFAX", assocPartyFAX));
    assocPartyRow.appendChild(createHiddenInput("partyDelivery", assocPartyDelivery));
    assocPartyRow.appendChild(createHiddenInput("partyCity", assocPartyCity));
    assocPartyRow.appendChild(createHiddenInput("partyState", assocPartyState.value));
    assocPartyRow.appendChild(createHiddenInput("partyStateOther", assocPartyStateOther));
    assocPartyRow.appendChild(createHiddenInput("partyZip", assocPartyZip));
    assocPartyRow.appendChild(createHiddenInput("partyCountry", assocPartyCountry));

		// Preferentially show the person, but default to the organization
		if ( assocPartyLastName != "" ) {
	    assocPartyRow.appendChild(assocPartyFirstNameCol);
	    assocPartyRow.appendChild(assocPartyLastNameCol);
			
		} else {
	    assocPartyRow.appendChild(assocPartyOrgNameCol);
			
		}
    assocPartyRow.appendChild(assocPartyRoleCol);

    assocPartyRow.appendChild(upCol);
    assocPartyRow.appendChild(downCol);
    assocPartyRow.appendChild(editCol);
    assocPartyRow.appendChild(delCol);
		    
    return assocPartyRow;

}

function editParty(evt) {
	var targetRow = evt.target.parentElement.parentElement.parentElement;
	var assocPartyTable = targetRow.parentNode.parentNode;
	var selectedRow = document.createElement("input");
	selectedRow.setAttribute("type", "hidden");
	selectedRow.setAttribute("id", "selectedRow");
	selectedRow.setAttribute("value", targetRow.getAttribute("id"));
	assocPartyTable.appendChild(selectedRow);
	
  var pFirstName = targetRow.firstChild;    
	document.getElementById("assocPartyFirstName").value = pFirstName.value;
	
  var pLastName = pFirstName.nextSibling;  
	document.getElementById("assocPartyLastName").value = pLastName.value;
	
  var pRole = pLastName.nextSibling;
	var assocPartyRole = document.getElementById("assocPartyRole");
	assocPartyRole.options.value = pRole.value;
	assocPartyRole.value = pRole.value;
	assocPartyRole.options[assocPartyRole.options.selectedIndex].selected = true;
  
	var pOrgName = pRole.nextSibling;       
	document.getElementById("assocPartyOrgName").value = pOrgName.value;
	
  var pEmail = pOrgName.nextSibling;    
	document.getElementById("assocPartyEmail").value = pEmail.value;
	
  var pPhone = pEmail.nextSibling;      
	document.getElementById("assocPartyPhone").value = pPhone.value;
	
  var pFAX = pPhone.nextSibling;      
	document.getElementById("assocPartyFAX").value = pFAX.value;
	
  var pDelivery = pFAX.nextSibling;        
	document.getElementById("assocPartyDelivery").value = pDelivery.value;
	
  var pCity = pDelivery.nextSibling;   
	document.getElementById("assocPartyCity").value = pCity.value;
	
  var pState = pCity.nextSibling;       
	var assocPartyState = document.getElementById("assocPartyState");
	assocPartyState.value = pState.value;
	assocPartyState.options.value = pState.value;
	assocPartyState.options[assocPartyState.options.selectedIndex].selected = true;
	
  var pStateOther = pState.nextSibling;      
	document.getElementById("assocPartyStateOther").value = pStateOther.value;
	
  var pZip = pStateOther.nextSibling; 
	document.getElementById("assocPartyZip").value = pZip.value;
	
  var pCountry = pZip.nextSibling;        
	document.getElementById("assocPartyCountry").value = pCountry.value;

  var updateButton = document.getElementById("updatePartyButton");
	updateButton.innerHTML = "";
	var text = document.createElement("span");
	var updatedText = document.createTextNode("Update Person or Organization");
	text.appendChild(updatedText);
	updateButton.appendChild(text);
	updateButton.setAttribute("style", "visibility: visible");
				
}

var timer;
function updateAssociatedParty() {
  
	var selectedRowId = document.getElementById("selectedRow").value;
	var selectedRow = document.getElementById(selectedRowId);
	
  var pFirstName = selectedRow.firstChild;    
	pFirstName.value = document.getElementById("assocPartyFirstName").value;
	
  var pLastName = pFirstName.nextSibling;  
	pLastName.value = document.getElementById("assocPartyLastName").value;
	
  var pRole = pLastName.nextSibling;   
	pRole.value = document.getElementById("assocPartyRole").value;
	
  var pOrgName = pRole.nextSibling;       
	pOrgName.value = document.getElementById("assocPartyOrgName").value;
	
  var pEmail = pOrgName.nextSibling;    
	pEmail.value = document.getElementById("assocPartyEmail").value;
	
  var pPhone = pEmail.nextSibling;      
	pPhone.value = document.getElementById("assocPartyPhone").value;
	
  var pFAX = pPhone.nextSibling;      
	pFAX.value = document.getElementById("assocPartyFAX").value;
	
  var pDelivery = pFAX.nextSibling;        
	pDelivery.value = document.getElementById("assocPartyDelivery").value;
	
  var pCity = pDelivery.nextSibling;   
	pCity.value = document.getElementById("assocPartyCity").value;
	
  var pState = pCity.nextSibling;       
	pState.value = document.getElementById("assocPartyState").value;
	
  var pStateOther = pState.nextSibling;      
	pStateOther.value = document.getElementById("assocPartyStateOther").value;
	
  var pZip = pStateOther.nextSibling; 
	pZip.value = document.getElementById("assocPartyZip").value;
	
  var pCountry = pZip.nextSibling;        
	pCountry.value = document.getElementById("assocPartyCountry").value;

  var updateButton = document.getElementById("updatePartyButton");
	updateButton.innerHTML = "";
	var text = document.createElement("span");
	var icon = document.createElement("i");
	var updatedText = document.createTextNode(" Successfully Updated");
	icon.setAttribute("class", "icon-ok");
	text.appendChild(icon);
	text.appendChild(updatedText);
	updateButton.appendChild(text);
	updateButton.blur();
	
	/* Clear the selected row id */
	var selectedRowInput = document.getElementById("selectedRow");
	selectedRowInput.parentElement.removeChild(selectedRowInput);
	
	/* Clear the form */
	document.getElementById("assocPartyFirstName").value = "";
	document.getElementById("assocPartyLastName").value = "";
	document.getElementById("assocPartyRole").selectedIndex = 0;
	document.getElementById("assocPartyOrgName").value = "";
	document.getElementById("assocPartyEmail").value = "";     
	document.getElementById("assocPartyPhone").value = "";     
	document.getElementById("assocPartyFAX").value = "";       
	document.getElementById("assocPartyDelivery").value = "";  
	document.getElementById("assocPartyCity").value = "";      
	document.getElementById("assocPartyState").selectedIndex = 0;  
	document.getElementById("assocPartyStateOther").value = "";
	document.getElementById("assocPartyZip").value = "";       
	document.getElementById("assocPartyCountry").value = "";
	
	timer = window.setTimeout(hideUpdatePartyButton, 2000);
	
}

function hideUpdatePartyButton() {
  var updateButton = document.getElementById("updatePartyButton");
	updateButton.setAttribute("style", "visibility: hidden");
	updateButton.setAttribute("value", "Update Person or Organization");
	window.clearTimeout(timer);
	
}

function createPartyRoleTypeSelect(name, value) {
    var newField = document.createElement("select");
    newField.setAttribute("name", name);
    var option1 = document.createElement("option");
    var text1 = document.createTextNode("Co-owner");
    option1.appendChild(text1);
    newField.appendChild(option1);
    var option2 = document.createElement("option");
    var text2 = document.createTextNode("Custodian/Steward");
    option2.appendChild(text2);
    newField.appendChild(option2);
    var option3 = document.createElement("option");
    var text3 = document.createTextNode("Metadata Provider");
    option3.appendChild(text3);
    newField.appendChild(option3)
    var option4 = document.createElement("option");
    var text4 = document.createTextNode("User");
    option4.appendChild(text4);
    newField.appendChild(option4);
        
    if (value == "Co-owner") {
        newField.selectedIndex = 0;
    } else if (value == "Custodian/Steward") {
        newField.selectedIndex = 1;
    } else if (value == "Metadata Provider") {
        newField.selectedIndex = 2;
    } else if (value == "User") {
        newField.selectedIndex = 3;
    }

    return newField;
}

function addTaxon() {   
    var taxonRank   = document.getElementById("taxonRank");
    var taxonName   = document.getElementById("taxonName");

    if (taxonRank.value !="" && taxonName.value != "") {
        var taxonCount = incrementCount("taxaCount");
        var row = createTaxonRow(taxonCount, taxonRank.value, taxonName.value);
        var taxonRowMarker = document.getElementById("addtaxarow");
        var parent = taxonRowMarker.parentNode;
        
        var taxonHR = document.getElementById("taxonHRRow");
        if (taxonHR == null) {
            var taxonHRRow = createHRRow();
            taxonHRRow.setAttribute("id", "taxonHRRow");
            parent.insertBefore(taxonHRRow, taxonRowMarker);
        }
        
        parent.insertBefore(row, taxonRowMarker);

        taxonRank.value = "";
        taxonName.value = "";
    } else {
        alert("Enter complete taxonomic information");
    }
}

function createTaxonRow(taxonCount, taxonRank, taxonName){    
    var TR = document.createElement("tr");
    TR.className='sectbody';

    var labelTD = createTD("5","left", "pointer");
    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
        labelTD.attachEvent("onclick",new Function("taxonEditRow(event, 0, \"" + taxonName
                         + "\",\"" + taxonRank  + "\")"));
    } else {
        labelTD.setAttribute("onClick","taxonEditRow(event, 0, \"" + taxonName
                         + "\",\"" + taxonRank  + "\")");
    }

    var text = "Rank: " + taxonRank + ", Name: " + taxonName;
    var label = document.createTextNode(text);

    labelTD.appendChild(label);
    labelTD.appendChild(createHiddenInput("taxonName", taxonName));
    labelTD.appendChild(createHiddenInput("taxonRank", taxonRank));
    
    var imgTD = createTD("","right");
    imgTD.className = 'rightCol';

    imgTD.appendChild(createIconLink("icon-arrow-up",
                                      "Move Up", "moveUpRow(event)",
                                      "pointer"));
    imgTD.appendChild(document.createTextNode(" "));
    imgTD.appendChild(createIconLink("icon-arrow-down",
                                      "Move Down", "moveDownRow(event, 'addtaxarow')",
                                      "pointer"));
    imgTD.appendChild(document.createTextNode(" "));
    imgTD.appendChild(createIconLink("icon-remove-sign",
                                      "Delete", "delRow(event)",
                                      "pointer"));

    TR.appendChild(imgTD);
    TR.appendChild(labelTD);
    
    return TR;
}

function taxonEditRow(evt, num,  taxonName ,  taxonRank) {
    evt = (evt) ? evt : ((window.event) ? window.event : null);
    if (evt) {
        // equalize W3C/IE models to get event target reference
        var elem = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
        if (elem && elem.nodeName == "TD") {
            try {
                var table = elem.parentNode;
                if (num == 0) {
                    table.removeChild(elem);
                    var TD = createTD("5","left", "pointer");
                    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
                        TD.attachEvent("onclick",new Function("taxonEditRow(event, 1, \"" + taxonName
                                       + "\",\"" + taxonRank  + "\")"));
                    } else {
                        TD.setAttribute("onClick","taxonEditRow(event, 1, \"" + taxonName
                                        + "\",\"" + taxonRank  + "\")");
                    }
          
                    TD.appendChild(document.createTextNode("Rank: "));
                    TD.appendChild(createTextField("taxonName", 15, taxonName));
                    TD.appendChild(document.createTextNode(" Name: "));
                    TD.appendChild(createTextField("taxonRank", 15, taxonRank));
                    table.appendChild(TD);
                } else {
                    var child = elem.childNodes;
                    taxonName = child.item(1).value;
                    taxonRank = child.item(3).value;
                    table.removeChild(elem);

                    var TD = createTD("5","left", "pointer");
                    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
                        TD.attachEvent("onclick",new Function("taxonEditRow(event, 0, \"" + taxonName
                                       + "\",\"" + taxonRank  + "\")"));
                    } else {
                        TD.setAttribute("onClick","taxonEditRow(event, 0, \"" + taxonName
                                        + "\",\"" + taxonRank  + "\")");
                    }                        
                    var text = "Rank: " + taxonName + ", Name: " + taxonRank;
                    var label = document.createTextNode(text);
                    TD.appendChild(label);
                    TD.appendChild(createHiddenInput("taxonName", taxonName));
                    TD.appendChild(createHiddenInput("taxonRank", taxonRank));
                    table.appendChild(TD);
                }
            } catch(e) {
                var msg = (typeof e == "string") ? e : ((e.message) ? e.message : "Unknown Error");
                alert("Error:\n" + msg);
                return;
            }
        }
    }
}


//BEGIN BRL ALTERNATE IDENTIFIER
function addIdentifier() {   
    var identifier   = document.getElementById("identifier");

    if (identifier.value !="") {
        var identifierCount = incrementCount("identifierCount");
        var row = createIdentifierRow(identifierCount, identifier.value);
        var identifierRowMarker = document.getElementById("addidentifierrow");
        var parent = identifierRowMarker.parentNode;
        
        var identifierHR = document.getElementById("identifierHRRow");
        if (identifierHR == null) {
            var identifierHRRow = createHRRow();
            identifierHRRow.setAttribute("id", "identifierHRRow");
            parent.insertBefore(identifierHRRow, identifierRowMarker);
        }
        
        parent.insertBefore(row, identifierRowMarker);

        identifier.value = "";
    } else {
        alert("Enter complete alternate identifier information");
    }
}

function createIdentifierRow(identifierCount, identifier){    
    var TR = document.createElement("tr");
    TR.className='sectbody';

    var labelTD = createTD("5","left", "pointer");
    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
        labelTD.attachEvent("onclick",new Function("identifierEditRow(event, 0, \"" + identifier  + "\")"));
    } else {
        labelTD.setAttribute("onClick","identifierEditRow(event, 0, \"" + identifier  + "\")");
    }

    var text = "Identifier: " + identifier;
    var label = document.createTextNode(text);

    labelTD.appendChild(label);
    labelTD.appendChild(createHiddenInput("identifier", identifier));
    
    var imgTD = createTD("","right");
    imgTD.className = 'rightCol';

    imgTD.appendChild(createIconLink("icon-arrow-up",
                                      "Move Up", "moveUpRow(event)",
                                      "pointer"));
    imgTD.appendChild(document.createTextNode(" "));
    imgTD.appendChild(createIconLink("icon-arrow-down",
                                      "Move Down", "moveDownRow(event, 'addidentifierrow')",
                                      "pointer"));
    imgTD.appendChild(document.createTextNode(" "));
    imgTD.appendChild(createIconLink("icon-remove-sign",
                                      "Delete", "delRow(event)",
                                      "pointer"));

    TR.appendChild(imgTD);
    TR.appendChild(labelTD);
    
    return TR;
}

function identifierEditRow(evt, num, identifier) {
    evt = (evt) ? evt : ((window.event) ? window.event : null);
    if (evt) {
        // equalize W3C/IE models to get event target reference
        var elem = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
        if (elem && elem.nodeName == "TD") {
            try {
                var table = elem.parentNode;
                if (num == 0) {
                    table.removeChild(elem);
                    var TD = createTD("5","left", "pointer");
                    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
                        TD.attachEvent("onclick",new Function("identifierEditRow(event, 1, \"" + identifier  + "\")"));
                    } else {
                        TD.setAttribute("onClick","taxonEditRow(event, 1, \"" + identifier  + "\")");
                    }
          
                    
                    TD.appendChild(document.createTextNode(" Identifier: "));
                    TD.appendChild(createTextField("identifier", 15, identifier));
                    table.appendChild(TD);
                } else {
                    var child = elem.childNodes;
                    taxonName = child.item(1).value;
                    table.removeChild(elem);

                    var TD = createTD("5","left", "pointer");
                    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
                        TD.attachEvent("onclick",new Function("identifierEditRow(event, 0, \"" + identifier  + "\")"));
                    } else {
                        TD.setAttribute("onClick","identifierEditRow(event, 0, \"" + identifier  + "\")");
                    }                        
                    var text = "Identifier: " + identifier;
                    var label = document.createTextNode(text);
                    TD.appendChild(label);
                    TD.appendChild(createHiddenInput("identifier", identifier));
                    table.appendChild(TD);
                }
            } catch(e) {
                var msg = (typeof e == "string") ? e : ((e.message) ? e.message : "Unknown Error");
                alert("Error:\n" + msg);
                return;
            }
        }
    }
}
//END BRL ALTERNATE IDENTIFIER

function addKeyword() {
    var keyword   = document.getElementById("keyword");
    var keywordType   = document.getElementById("keywordType");
    var keywordTh = document.getElementById("keywordTh");

    if (keyword.value !="") {
        var keyCount = incrementCount("keyCount");
        var keyRow = createKeywordRow(keyCount, keyword.value, 
                                      keywordType.options[keywordType.selectedIndex].text, 
                                      keywordTh.options[keywordTh.selectedIndex].text);
        var keyRowMarker = document.getElementById("addkeyrow");
        var parent = keyRowMarker.parentNode;
        
        var keyHR = document.getElementById("keywordHRRow");
        if (keyHR == null) {
            var keyHRRow = createHRRow();
            keyHRRow.setAttribute("id", "keywordHRRow");
            parent.insertBefore(keyHRRow, keyRowMarker);
        }
        
        parent.insertBefore(keyRow, keyRowMarker);
    
        keyword.value = "";
        keywordType.selectedIndex = 0;
        keywordTh.selectedIndex = 0;
    } else {
        alert("Enter keyword");
    }
}

function createKeywordRow(keyCount, keyword, keywordType, keywordTh){    
    var TR = document.createElement("tr");
    TR.className='sectbody';

    var labelTD = createTD("5","left", "pointer");
    
    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
        labelTD.attachEvent("onclick", new Function("keywordEditRow(event, 0, \"" + keyword
            + "\",\"" + keywordType + "\",\"" + keywordTh  + "\")"));
    } else {
        labelTD.setAttribute("onClick","keywordEditRow(event, 0, \"" + keyword
            + "\",\"" + keywordType + "\",\"" + keywordTh  + "\")");
    }
              
    var text   = keyword + " (Type: " + keywordType + ", Thesaurus: " + keywordTh + ")";
    var label = document.createTextNode(text);

    labelTD.appendChild(label);
    labelTD.appendChild(createHiddenInput("keyword", keyword));
    labelTD.appendChild(createHiddenInput("keywordType", keywordType));
    labelTD.appendChild(createHiddenInput("keywordTh", keywordTh));
    
    var imgTD = createTD("","right");
    imgTD.className = 'rightCol';

    imgTD.appendChild(createIconLink("icon-arrow-up",
                                      "Move Up", "moveUpRow(event)",
                                      "pointer"));
    imgTD.appendChild(document.createTextNode(" "));
    imgTD.appendChild(createIconLink("icon-arrow-down",
                                      "Move Down", "moveDownRow(event, 'addkeyrow')",
                                      "pointer"));
    imgTD.appendChild(document.createTextNode(" "));
    imgTD.appendChild(createIconLink("icon-remove-sign",
                                      "Delete", "delRow(event)",
                                      "pointer"));

    TR.appendChild(imgTD);
    TR.appendChild(labelTD);
    
    return TR;
}

function keywordEditRow(evt, num,  keyword,  keywordType, keywordTh) {
    evt = (evt) ? evt : ((window.event) ? window.event : null);
    if (evt) {
        // equalize W3C/IE models to get event target reference
        var elem = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
        if (elem && elem.nodeName == "TD") {
            try {
                var table = elem.parentNode;
                if (num == 0) {
                    table.removeChild(elem);
                    var TD = createTD("5","left", "pointer");
                    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
                        TD.attachEvent("onclick", new Function("keywordEditRow(event, 1, \"" + keyword
                        + "\",\"" + keywordType + "\",\"" + keywordTh  + "\")"));
                    } else {
                        TD.setAttribute("onClick","keywordEditRow(event, 1, \"" + keyword
                        + "\",\"" + keywordType + "\",\"" + keywordTh  + "\")");
                    }
                
                    TD.appendChild(createTextField("keyword", 15, keyword));
                    TD.appendChild(document.createTextNode(" Type: "));
                    TD.appendChild(createKeyTypeSelect("keywordType", keywordType));
                    TD.appendChild(document.createTextNode(" Thesaurus: "));
                    TD.appendChild(createKeyThSelect("keywordTh", keywordTh));
                    table.appendChild(TD);
                } else {
                    var child = elem.childNodes;
                    keyword = child.item(0).value;
                    var _keywordType = child.item(2);
                    keywordType = _keywordType.options[_keywordType.selectedIndex].text;
                    var _keywordTh = child.item(4);
                    keywordTh = _keywordTh.options[_keywordTh.selectedIndex].text;
                    table.removeChild(elem);

                    var TD = createTD("5","left", "pointer");
                    if (navigator.userAgent.toLowerCase().indexOf('msie')!= -1 && document.all) {
                        TD.attachEvent("onclick", new Function("keywordEditRow(event, 0, \"" + keyword
                        + "\",\"" + keywordType + "\",\"" + keywordTh  + "\")"));
                    } else {
                        TD.setAttribute("onClick","keywordEditRow(event, 0, \"" + keyword
                        + "\",\"" + keywordType + "\",\"" + keywordTh  + "\")");
                    }
                
                    var text   = keyword + " (Type: " + keywordType + ", Thesaurus: " + keywordTh + ")";
                    var label = document.createTextNode(text);
                    TD.appendChild(label);
                    TD.appendChild(createHiddenInput("keyword", keyword));
                    TD.appendChild(createHiddenInput("keywordType", keywordType));
                    TD.appendChild(createHiddenInput("keywordTh", keywordTh));
                    table.appendChild(TD);
                }
            } catch(e) {
                var msg = (typeof e == "string") ? e : ((e.message) ? e.message : "Unknown Error");
                alert("Error:\n" + msg);
                return;
            }
        }
    }
}

function createKeywordThesaurusRow(name) {        
    var keyRowTemplate = document.getElementById("keyThRow");
    var newField =  keyRowTemplate.cloneNode(true);
    var nodes = newField.getElementsByTagName("input");
    nodes.item(0).setAttribute("name", name);
    nodes.item(1).setAttribute("name", name);
    return newField;
}

function createKeyThSelect(name, value) {
    var newField = document.createElement("select");
    newField.setAttribute("name", name);
    var option1 = document.createElement("option");
    var text1 = document.createTextNode("None");
    option1.appendChild(text1);
    newField.appendChild(option1);
    var option2 = document.createElement("option");
    var text2 = document.createTextNode("GCMD");
    option2.appendChild(text2);
    newField.appendChild(option2);

    if (value == "None") {
        newField.selectedIndex = 0;
    } else if (value == "GCMD") {
        newField.selectedIndex = 1;
    }
    return newField;
}

function createKeyTypeSelect(name, value) {
    var newField = document.createElement("select");
    newField.setAttribute("name", name);
    var option1 = document.createElement("option");
    var text1 = document.createTextNode("None");
    option1.appendChild(text1);
    newField.appendChild(option1);
    var option2 = document.createElement("option");
    var text2 = document.createTextNode("Theme");
    option2.appendChild(text2);
    newField.appendChild(option2);
    var option3 = document.createElement("option");
    var text3 = document.createTextNode("Place");
    option3.appendChild(text3);
    newField.appendChild(option3)
    var option4 = document.createElement("option");
    var text4 = document.createTextNode("Stratum");
    option4.appendChild(text4);
    newField.appendChild(option4);
    var option5 = document.createElement("option");
    var text5 = document.createTextNode("Temporal");
    option5.appendChild(text5);
    newField.appendChild(option5);
    var option6 = document.createElement("option");
    var text6 = document.createTextNode("Taxonomic");
    option6.appendChild(text6);
    newField.appendChild(option6);        
    if (value == "None") {
        newField.selectedIndex = 0;
    } else if (value == "Theme") {
        newField.selectedIndex = 1;
    } else if (value == "Place") {
        newField.selectedIndex = 2;
    } else if (value == "Stratum") {
        newField.selectedIndex = 3;
    }else if (value == "Temporal") {
        newField.selectedIndex = 4;
    } else if (value == "Taxonomic") {
        newField.selectedIndex = 5;
    }
    return newField;
}

var basicInfoBit = 1;
var submitterBit = 1;
var dsoBit = 1;
var apBit = 1;
var abstractBit = 1;
var keywordBit = 1;
var temporalBit = 1;
var spatialBit = 1;
var taxonomicBit = 1;
var methodBit = 1;
var dscBit = 1;
var distBit = 1;
var uploadBit = 1;

function swap(evt, _node, nodeBit) {
    evt = (evt) ? evt : ((window.event) ? window.event : null);
    if (evt) {
        var elem = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
        
        // hack so that this works on safari...
        if (elem.nodeName != 'A') {
            elem = elem.parentNode;
        }
        elem.removeChild(elem.firstChild);
        var node = document.getElementById(_node);
        if (nodeBit) {
            elem.appendChild(document.createTextNode("Show"));
            node.className="hide";
            return 0;
        } else {
            elem.appendChild(document.createTextNode("Hide"));
            node.className="tables";
            return 1;
        }
    }        
}

function handleOther(enable, node) {
    var textBox = document.getElementById(node);
    if (enable) {
        textBox.removeAttribute("disabled");   
    } else {
        textBox.value="";
        textBox.disabled = "true";   
    }
}

// include source for Multiple file uploader:
// http://the-stickman.com/web-development/javascript/

function MultiSelector(list_target, max) {

    this.list_target = list_target;
    this.count = 0;
    this.id = 0;
    var ucount =  document.getElementById("upCount");
    var fcount =  document.getElementById("fileCount");
    if (fcount.value > 0) {
        this.id = fcount.value;
    } else {
        // upCount contains pre-existing uploads, check this for editing existing packages
        if (ucount != null && ucount.value > 0) {
            this.id = ucount.value;
        }
    }

    if (max) {
        this.max = max;
    } else {
      this.max = -1;
    };

    /**
     * Add a new file input element
     */
    this.addElement = function( element ) {

        // Make sure it's a file input element
        if (element.tagName == 'INPUT' && element.type == 'file' ) {
            // Element name -- what number am I?
            element.name = 'file_' + this.id++;

            // Add reference to this object
            element.multi_selector = this;

            // What to do when a file is selected
            element.onchange = function() {
                // Increment file counter
                var fileCount = incrementCount("fileCount");
                // If pre-existing uploads exist, make sure the fileCount is synced
                if (ucount != null && ucount.value > 0) {
                    fileCount += ucount.value;
                    setCount("fileCount", fileCount);
                }

                // Clean up file text
                var comment_element = document.getElementById( 'file_comment' );
                if (comment_element) {
                    comment_element.parentNode.removeChild(comment_element);
                }

                // New file input
                var new_element = document.createElement( 'input' );
                new_element.type = 'file';

                // Add new element
                this.parentNode.insertBefore( new_element, this );

                // Apply 'update' to element
                this.multi_selector.addElement( new_element );

                // Update list
                this.multi_selector.addListRow( this );

                // Hide this: we can't use display:none because Safari doesn't like it
                this.style.position = 'absolute';
                this.style.left = '-1000px';
            };

            // If we've reached maximum number, disable input element
            if (this.max != -1 && this.count >= this.max ) {
                element.disabled = true;
            };
            
            // File element counter
            this.count++;

            // Most recent element
            this.current_element = element;

        } else {
            // This can only be applied to file input elements!
            alert( 'Error: not a file input element' );
        };
    };

    /**
     * Add a new row to the list of files
     */
    this.addListRow = function( element ) {
        // Row div
        var new_row = document.createElement( 'tr' );

        // Delete button
		var col_4 = document.createElement( 'td' );
        var new_row_button = document.createElement( 'input' );
        new_row_button.type = 'button';
        new_row_button.setAttribute("class", "btn"); 
        new_row_button.value = 'Delete';
		col_4.appendChild(new_row_button);

        // Permissions check boxes
        var radio_value_a = 'public';
        var radio_value_b = 'private';
		var col_3 = document.createElement('td');
		var col_2 = document.createElement('td');
        var new_radio_a = document.createElement('input');
        var new_radio_b = document.createElement('input');
        new_radio_a.type = new_radio_b.type = 'radio';
        // get the file upload name, use the number to keep track of the file we're setting permissions on
        new_radio_a.name = new_radio_b.name = 'uploadperm_' + element.name.split("_")[1];
        new_radio_a.value = radio_value_a;
        new_radio_b.value = radio_value_b;
        new_radio_a.checked = false;
        new_radio_b.checked = true; // when adding new files, default is private
         
        // References
        new_row.element = element;

        // Delete function
        new_row_button.onclick= function() {
            // Remove element from form
            this.parentNode.element.parentNode.removeChild( this.parentNode.element );

            // Remove this row from the list
            this.parentNode.parentNode.removeChild( this.parentNode );
            // Decrement counter
            this.parentNode.element.multi_selector.count--;

            // Re-enable input element (if it's disabled)
            this.parentNode.element.multi_selector.current_element.disabled = false;

            // Appease Safari
            //    without it Safari wants to reload the browser window
            //    which nixes your already queued uploads
            return false;
        };
       
        // filename may include path, show only file name itself
		var col_1 = document.createElement('td');
        var name_pattern = element.value.replace(/.*[\/\\](.*)/, "$1");

        // Set row value
        col_1.appendChild( document.createTextNode(name_pattern + " ") );
        new_row.appendChild( col_1 );
        
        // Add in radio buttons and their text
		col_2.appendChild( new_radio_a );
        col_2.appendChild( document.createTextNode(String.fromCharCode(160) + capitalize(radio_value_a)) );
        new_row.appendChild( col_2 );
		col_3.appendChild( new_radio_b );
        col_3.appendChild( document.createTextNode(String.fromCharCode(160) + capitalize(radio_value_b)) );
        new_row.appendChild( col_3 );

        // Add button
        new_row.appendChild( col_4 );

        // Add it to the list
        this.list_target.appendChild( new_row );
    };

};

// Append files to be deleted to an HTML array
function deleteFile(evt, file) {
    evt = (evt) ? evt : ((window.event) ? window.event : null);
    if (evt) {
        // equalize W3C/IE models to get event target reference
        var elem = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
        var element = document.getElementById("file_element");
        if (elem) {
            try {
                // Add a new hidden form element to delete exisiting files
                var delete_existing = document.createElement( 'input' );
                delete_existing.type = "hidden";
                delete_existing.name = "deletefile"; // HTML array
                delete_existing.value = file.name;
                element.parentNode.appendChild( delete_existing );

                // Remove this row from the list
                elem.parentNode.parentNode.removeChild( elem.parentNode );
            } catch(e) {
                var msg = (typeof e == "string") ? e : ((e.message) ? e.message : "Unknown Error");
                alert("Error:\n" + msg);
                return;
            }
        }
    }
}

function capitalize(word) {
    return word.charAt(0).toUpperCase() + word.substr(1).toLowerCase();
}
