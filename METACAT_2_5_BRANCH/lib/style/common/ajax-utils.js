 /*
  *   '$RCSfile$'
  *     Purpose: Basic Ajax utilities
  *   Copyright: 2009 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *     Authors: Michael Daigle
  *
  *    '$Author: daigle $'
  *      '$Date: 2008-07-06 21:25:34 -0700 (Sun, 06 Jul 2008) $'
  *  '$Revision: 4080 $'
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
 
/* submits a form via ajax and inserts the results into the given div  
 *  Params:
 *    url - url to hit
 *    formId - id of the form to submit
 *    divId - the name of the div where the results should be put
 */  
function submitFormIntoDiv(url, formId, divId) {
	//alert('Sending form: ' + formId + " to url: " + url);
	var formObj = document.getElementById(formId);
	//alert('Form object: ' + formObj);

	var myRequest = new Ajax.Updater(divId, url,
		{	method: 'post',
			parameters: Form.serialize(formObj)
		});
	//alert("after update");
}

/* submits parameters serialized from a form via ajax and inserts the results into the given div  
 *  Params:
 *    url - url to hit
 *    formId - id of the form to submit
 *    divId - the name of the div where the results should be put
 */  
function submitFormParasIntoDiv(url, formParas, divId) {
	//alert('Form object: ' + formObj);

	var myRequest = new Ajax.Updater(divId, url,
		{	method: 'post',
			parameters: formParas
		});
	//alert("after update");
}

/* submits a form via ajax and inserts the results into the given div  
 *  Params:
 *    url - url to hit
 *    formId - id of the form to submit
 *    divId - the name of the div where the results should be put
 */  
function submitFormObjIntoDiv(url, formObj, divId) {
	//alert('Form object: ' + formObj);

	var myRequest = new Ajax.Updater(divId, url,
		{	method: 'post',
			parameters: Form.serialize(formObj)
		});
	//alert("after update");
}

/* submits a form via ajax and inserts the results into the given div while it will reload the page.
 *  Params:
 *    url - url to hit
 *    formId - id of the form to submit
 *    divId - the name of the div where the results should be put
 */  
function submitFormParasIntoDivAndReload(url, formParas, divId) {
	var myRequest = new Ajax.Updater(divId, url,
			{	method: 'post',
				parameters: formParas,
				onSuccess: function(reponse) {
					window.location.reload();
				}
			});
}

/* submits a url via ajax and inserts the results into the given div  
 *  Params:
 *    url - url to hit
 *    divId - the name of the div where the results should be put
 */  
function submitUrlIntoDiv(url, divId) {
	//alert('Sending url: ' + url);

	var myRequest = new Ajax.Updater(divId, url,
		{	method: 'post'
		});
}
  