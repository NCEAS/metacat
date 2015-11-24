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
 *    workflowId - get all runs for this id 
 *    workflowName - used for display purposes.  
 *    divId - the name of the div where the results should be put
 */  
function getAccessSection(url, karfilelsid, workflowname, divId) {
	var requestUrl = url + '?action=getaccesscontrol&docid=' + karfilelsid + 
		"&workflowname=" + workflowname + '&qformat=sanparks';
	//alert('getAccessSection - url: ' + requestUrl);
	var submitResults = submitUrlIntoDiv(requestUrl, divId);
}

function setPermission(formObj) {
	
	//alert("principal: " + formObj.principal.value);		
	if ( formObj.principal.value == null || formObj.principal.value == "") {
		alert("you must provide a pricipal");
		return;
	}

	var permission = "";
	if (formObj.permission_read_checkbox.checked) {
		permission += "READ";
	}
	if (formObj.permission_write_checkbox.checked) {
		if (permission.length > 0) {
			permission += "|";
		}
		permission += "WRITE";
	}
	if (formObj.permission_chmod_checkbox.checked) {
		if (permission.length > 0) {
			permission += "|";
		}
		permission += "CHANGEPERMISSION";
	}
//	if (formObj.permission_all_checkbox.checked) {
//		if (permission.length > 0) {
//			permission += "|";
//		}
//		permission += "ALL";
//	}
	//alert("permission: " + permission);
	formObj.permission.value = permission;
	
	if (permission == "") {
		alert("You must choose a permission value");
		return;
	}
}

function doNothing() {
}

	
