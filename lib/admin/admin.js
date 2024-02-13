 /*
  *   '$RCSfile$'
  *     Purpose: Default style sheet for admin web pages 
  *   Copyright: 2008 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *     Authors: Matt Jones
  *
  *    '$Author$'
  *      '$Date$'
  *  '$Revision$'
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

function createExclusionList() {
	exclusionList = new Array();
}

function addExclusion(exclusionName) {
	exclusionList.push(exclusionName);
}

function validateForm(form_ref) {  
    for (i = 0; i < form_ref.length; i++) {
        if (form_ref.elements[i].value == "") {
        	excludeThisField="false";
        	for (j = 0; j < exclusionList.length; j++) {
        		if (exclusionList[j] == form_ref.elements[i].id) {
        			excludeThisField="true";
        		}
        	}
        	if (excludeThisField == "true") {
            	alert("All form fields must be populated");
            	return false;
            }
        }
    }
    return true;
}

function submitForm(form_ref) {
	form_ref.submit();
}

function validateAndSubmitForm(form_ref) {
	if (!validateForm(form_ref)) {
		return false;
	}
	form_ref.submit();
}

function forward(location) {
	window.location = location;
}

function toggleHiddenRow(thisObj, id) {
	if (thisObj.checked) {
		showRow(id);
	} else {
		hideObject(id);
	}
}

function toggleHiddenInline(thisObj, id) {
	if (thisObj.checked) {
		showInline(id);
	} else {
		hideObject(id);
	}
}

function toggleHiddenTable(thisObj, id) {
	if (thisObj.checked) {
		showSection(id);
	} else {
		hideObject(id);
	}
}

function hideObject(objectID) 
{ 
	document.getElementById(objectID).style.display = 'none'; 
} 

function showRow(objectID) 
{ 
	document.getElementById(objectID).style.display = 'table-row'; 
} 

function showInline(objectID)
{
        document.getElementById(objectID).style.display = 'inline';
}

function showSection(objectID) 
{ 
	document.getElementById(objectID).style.display = 'table'; 
} 

function toggleHiddenDefaultText(radioName, activeSkinName) {
	radioList = document.getElementsByName(radioName);
	for (i = 0; i < radioList.length; i++) {
		radioId = radioList[i].id;
		nameArray = radioId.split("-", 1);
		radioSkinName = nameArray[0];
		if (radioSkinName == activeSkinName) {
			document.getElementById("hiding-default-" + radioSkinName).style.display = 'inline';
		} else {
			document.getElementById("hiding-default-" + radioSkinName).style.display = 'none';
		}
	}
}

function helpWindow(context, helpFile) {
	fileLoc = context + "/" + helpFile;
	window.open(fileLoc,'mywindow','width=1050,height=800,scrollbars=yes,location=no,status=no');
}

function toggleHiddenInputField(targetRadioId, inputFieldName) {
    radio = document.getElementById(targetRadioId);
    if (radio.checked) {
        document.getElementById(inputFieldName).style.display = 'inline';
    } else {
        document.getElementById(inputFieldName).style.display = 'none';
    }
    
}
