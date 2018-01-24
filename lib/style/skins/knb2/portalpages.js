// JavaScript Document

var popupMsg = "If you need to create a new account, \n"
			  +"click the \"create new account\" link \n"
			  +"beneath the password text-box";

function allowSubmit(formObj) {

  if (trim(formObj.elements["loginAction"].value)!="Login") return true;

  //trim username & passwd:
  var username = trim(formObj.elements["username"].value);
  var organization  = trim(formObj.elements["organization"].value);
  var password      = trim(formObj.elements["password"].value);

  if (username=="") {
    alert("You must type a username. \n"+popupMsg);
	formObj.elements["username"].focus();
    return false;
  } 
  
  if (organization=="") {
    alert("You must select an organization. \n"+popupMsg);
	formObj.elements["organization"].focus();
    return false;
  } 
  
  if (password=="") {
    alert("You must type a password. \n"+popupMsg);
	formObj.elements["password"].focus();
    return false;
  }
  return true;
}	

function allowSearch(formObj) {

  var canSearch = true;
  var searchString = trim(formObj.elements["anyfield"].value);
  if (searchString=="") {
    if (confirm("Show *all* data?\n(this may take some time!)")) {
	  formObj.elements["anyfield"].value = "%";
	  canSearch = true;
	} else {
	  formObj.elements["anyfield"].focus();
	  canSearch = false;
	}
  } 
  return canSearch;
}


function allowAdvancedSearch(inputFormObj, submitFormObj) {

  var searchString = trim(inputFormObj.elements["searchValue"].value);
  
  if (searchString=="") searchString="%";
  
  if (inputFormObj.searchField.value=='anyfield') {
    
    submitFormObj.anyfield.value=searchString;
      
  } else if (inputFormObj.searchField.value=='title') {
    
    submitFormObj.title.value=searchString;
      
  } else if (inputFormObj.searchField.value=='surname') {
    
    submitFormObj.surName.value=searchString;
  }
  return true;
}

function keywordSearch(formObj, searchKeyword) {

  var searchString = trim(searchKeyword);
  
  if (searchString=="") searchString="%";
  
  formObj.anyfield.value=searchString;

  formObj.submit();

  return true;
}

function trim(stringToTrim) {

  return stringToTrim.replace(/^\s*/, '').replace(/\s*$/,'');
}
