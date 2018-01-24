 /*
  *   '$RCSfile$'
  *     Purpose: Default style sheet for SANPark project login pages 
  *              Using this stylesheet rather than placing styles directly in 
  *              the KNP web documents allows us to globally change the 
  *              formatting styles of the entire site in one easy place.
  *   Copyright: 2009 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *     Authors: Matt Jones
  *
  *    '$Author: daigle $'
  *      '$Date: 2009-09-04 15:12:17 -0700 (Fri, 04 Sep 2009) $'
  *  '$Revision: 5057 $'
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

function submitLoginFormIntoDiv(servletUrl, formObj, divId) {

	//var formObj = document.getElementById(formId);
	var shortUserName = formObj.shortusername.value;
	//alert("starting userName: " + shortUserName);
	var organization = formObj.organization.value;
	//alert("organization: " + organization);
	formObj.username.value = createLdapString(shortUserName, organization);
	var formParas = Form.serialize(formObj);
	//alert("ending userName: " + formObj.username.value);
	var loggingInContent = "<table><tr><td width='600px' align='center'><p>logging in</p></td></tr></table>"
	document.getElementById(divId).innerHTML = loggingInContent;
	
	submitFormParasIntoDiv(servletUrl, formParas, divId);

}

function submitLoginFormIntoDivAndReload(servletUrl, formObj, divId) {

	//var formObj = document.getElementById(formId);
	var shortUserName = formObj.shortusername.value;
	//alert("starting userName: " + shortUserName);
	var organization = formObj.organization.value;
	//alert("organization: " + organization);
	formObj.username.value = createLdapString(shortUserName, organization);
	var formParas = Form.serialize(formObj);
	//alert("ending userName: " + formObj.username.value);
	var loggingInContent = "<table><tr><td width='600px' align='center'><p>logging in</p></td></tr></table>"
	document.getElementById(divId).innerHTML = loggingInContent;
	
	submitFormParasIntoDivAndReload(servletUrl, formParas, divId);

}

function submitLogoutFormIntoDiv(servletUrl, formObj, divId) {
	var formParas = Form.serialize(formObj);
	var loggingOutContent = "<table><tr><td width='600px' align='center'><p>logging out</p></td></tr></table>"
	document.getElementById(divId).innerHTML = loggingOutContent;
	
	submitFormParasIntoDiv(servletUrl, formParas, divId);

}

function createLdapString(userName, organization) {
    var ldapTemplate = "uid=%1s,o=%2s,dc=ecoinformatics,dc=org";
    var tmp = ldapTemplate.replace("%1s", userName);
    var ldapUserName = tmp.replace("%2s", organization);
 
 	return ldapUserName;
}

