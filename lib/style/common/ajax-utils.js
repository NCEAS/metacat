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
  