var fullDocId;

//log the user in 
function login()
{
  var user = document.getElementById("un").value;
  var org = document.getElementById("org").value;
  var pass = document.getElementById("pw").value;
  var ldapUsername = 'uid=' + user + ',o=' + org + ',dc=ecoinformatics,dc=org';
  
  $.get("metacat", {username: ldapUsername, password: pass, action:"login", qformat:"xml"}, 
    function(data) {
      //alert('user ' + ldapUsername + ' logged in.  data:' + data);
      if(data.indexOf('<sessionId>') != -1)
      { //login successful
        //alert('user logged in');
        slideUp("#loginformdiv");
        setCookie("sms-login", true);
        setCookie("sms-user", user);
        setCookie("sms-org", org);
        setLoginHeader(true);
        checkLogin();
      }
      else
      { //login not successful
        alert('Sorry, your login failed.  Please try again.  If you need a username, please go to http://knb.ecoinformatics.org.');
        setCookie("sms-login", false);
      }
      
    }, "XML");
}

//update the users status on the page
function updateStatus()
{
  var url = window.location.href;
  if(url.indexOf('docid') != -1)
  { //if there's a docid in the url, set the cookie
    var docid = url.substring(url.indexOf("docid=") + 6, url.indexOf("&status"));
    var docidcookie = getCookie("sms-lastdocid");
    if(docid != docidcookie)
    { //set the cookie for next time
      setCookie("sms-lastdocid", docid);
      //slideDown("#uploadstatus");
      $('#uploadstatus').css("display", "block");
    }
    else
    { //hide the status
      $('#uploadstatus').css("display", "none");
      //slideUp("#uploadstatus");
    }
  }
  
}

//set the header when the user logs in
function setLoginHeader(loggedin)
{
  if(loggedin)
  {
    updateStatus();
    var user = getCookie("sms-user");
    $('#loginheader').replaceWith("<h2 style=\"text-align:center\" id=\"loginheader\">" 
      + user + " Logged In <a href=\"javascript:logout()\" style=\"font-size:70%\">[logout]</a></h2>");
    slideUp("#loginformdiv");
    $('#maindiv').css("display", "block");
    $('#bottomimg').css("bottom", "0px");
    getId();
  }
  else
  {
    $('#loginheader').replaceWith("<h2 style=\"text-align:center\" id=\"loginheader\">" 
      + "Please Log In</h2>");
    slideDown("#loginformdiv");
    $('#maindiv').css("display", "none");
    $('#bottomimg').css("bottom", "15px");
  }
}

//log the user out.
function logout()
{
  $.get("metacat", {action:"logout", qformat:"xml"});
  setLoginHeader(false);
  setCookie("sms-login", false);
}

//make sure the given docid is public then call checkLogin()
function checkPublicAccess(docid)
{
  var lastpublicaccess = getCookie("sms-last-public-access");
  if(lastpublicaccess == null || lastpublicaccess != docid)
  { //only make it public if it wasn't already made public
    makepublic(docid);
  }
  checkLogin();
}

//make sure the user is logged in.
function checkLogin()
{
  var currentTab = getCookie("sms-current-tab");
  
  if(getCookie("sms-login") == "true")
  {
    setLoginHeader(true);
    showDatasets();
    if(currentTab == null || currentTab == 'search')
    {
      showSearchPane();
    }
    else
    {
      if(currentTab == 'browse')
      {
        showBrowsePane();
      }
      else if(currentTab == 'upload')
      {
        showUploadPane();
      }
    }
  }
  else
  {
    setLoginHeader(false);
  }
}

//search the document base in metacat
function search()
{
  var searchval = document.getElementById("searchtextbox").value
  var url = '/sms/metacat?action=query&anyfield=' + searchval + '&returnfield=dataset/title&qformat=sms&pagesize=10&pagestart=0';
  setCookie("sms-searchval", searchval);
  setCookie("sms-pagestart", 0);
  reloadSearchContent(url);
}

//show all of the datasets in metacat
function showDatasets()
{
  var searchval = getCookie('sms-searchval');
  if(searchval == null || searchval == '')
  {
    searchval = '%25';
  }
  var currentTab = getCookie('sms-current-tab');
  var page;
  if(currentTab == null)
  {
    page = 0;
  }
  else if(currentTab == 'search' || currentTab == 'upload')
  {
    page = getCookie('sms-search-pagestart');
  }
  else
  {
    page = getCookie('sms-browse-pagestart');
    setCookie('sms-browse-content-loaded', 'true');
    searchval = '%25';
  }
  
  if(page)
  {
    reloadSearchContent('/sms/metacat?action=query&anyfield=' + searchval + '&returnfield=dataset/title&qformat=sms&pagesize=10&pagestart=' + page);
  }
  else
  {
    reloadSearchContent('/sms/metacat?action=query&anyfield=' + searchval + '&returnfield=dataset/title&qformat=sms&pagesize=10&pagestart=0');
  }
}

//reload the search result table
function reloadSearchContent(url)
{
  var table;
  var div;
  var page = url.substring(url.indexOf('pagestart=') + 10, url.length);
  var currentTab = getCookie('sms-current-tab');
  if(currentTab == null || currentTab == 'search' || currentTab == 'upload')
  {
    table = '#searchresulttable';
    div = '#searchresultdiv'
    setCookie("sms-search-pagestart", page);
  }
  else if(currentTab == 'browse')
  {
    table = '#browseresulttable';
    div = '#browseresultdiv'
    setCookie("sms-browse-pagestart", page);
  }
  
  $(table).load(url);
}

//upload a file to metacat
function uploadfile()
{
  if(getCookie("sms-login") != "true")
  {
    alert('You cannot upload.  You are not logged in.');
    return;
  }
  if(!checkId(true))
  { //make sure the id is valid
    alert('The ID prefix you chose is not valid.  The prefix must be a string of alpha characters only.');
  }
  else
  {
    if(document.getElementById("datafile").value == null || document.getElementById("datafile").value == "")
    {
      alert('You must choose a file to upload.');
      return;
    }
    getId(true, true, true);
  }
}

//make a document public
function makepublic(docid)
{
  $.get("/sms/metacat?action=setaccess&docid=" + docid + 
        "&principal=public&permission=read&permType=allow&permOrder=allowFirst",
        function(data) {
          if(data.indexOf("<success>") != -1)
          {
            slideUp("#uploadstatus");
            $("#uploadstatus").html('<p>The document with id ' + 
              '<a href="/sms/metacat?action=read&docid=' + docid + '&qformat=sms">' + docid + 
              '</a> is now publicly readable.</p>');
            slideDown("#uploadstatus");
            setCookie("sms-last-public-access", docid);
          }
          else
          {
            alert('The access control changes for ' + docid + ' failed.  It is not publicly readable.');
          }
        }, "XML");
}

//get the next id and put it in the id text boxes
function getId(setFields, setForm, submitForm)
{
  if(setFields == null)
  {
    setFields = true;
  }
  
  if(setForm == null)
  {
    setForm = false;
  }
  
  if(submitForm == null)
  {
    submitForm = false;
  }
  
  var scopeStr = document.getElementById("docidfamily").value;
  //var scopeStr = $('#docidfamily').value;
  if(scopeStr == '' || scopeStr == null)
  {
    scopeStr = "sms";
  }
  
  $.get("metacat", {action:"getlastdocid", scope:scopeStr}, 
      function(data)
      {
        var docid = data.substring(data.indexOf("<docid>") + 7, data.indexOf("</docid>"));
        var nextid;
        if(docid == 'null')
        {
          nextid = 1;
        }
        else
        {
          nextid = docid.substring(docid.indexOf(".") + 1, docid.lastIndexOf("."));
          nextid++;
          //nextid = scopeStr + nextid + ".1"; 
        }
        //$('#docidtextfield').val(nextid);
        if(setFields)
        {
          $('#docidfamily').val(scopeStr);
          $('#docidnumber').val(nextid);
          $('#docidrevision').val("1");
        }
        fullDocId = scopeStr + "." + nextid + ".1";
        //alert('fullDocId: ' + fullDocId);
        if(setForm)
        {
          //alert('setting docid to ' + fullDocId);
          $('#docid').val(fullDocId);
        }
        
        if(submitForm)
        {
          $("form").submit();
        }
      }, 
      "XML");
}

//check for a valid docid
function checkId(setForm)
{
  if(setForm == null)
  {
    setForm = false;
  }
  getId(false, setForm, false);
  var scopeStr = document.getElementById("docidfamily").value;
  var numberStr = document.getElementById("docidnumber").value;
  var userDocid = scopeStr + "." + numberStr; 
  
  //fullDocId is a global var that gets set by getId()
  var nextnum = fullDocId.substring(fullDocId.indexOf(".") + 1, fullDocId.lastIndexOf("."));
  var regexp = "[^[[a-z]|[A-Z]]+]"; //search for anything thats not an alpha 
  var re = new RegExp(regexp);
  var match = re.test(scopeStr);
  if(match)
  { //if it matches, reject
    return false;
  }
  
  return true;
}

//show the search tab
function showSearchPane()
{
  setCookie('sms-current-tab', 'search');
  //hide all, then slide down the search pane
  $('#uploaddiv').hide();
  $('#browseresultdiv').hide();
  $('#searchdiv').fadeIn("slow");
  switchTabs('search');
}

//show the upload tab
function showUploadPane()
{
  setCookie('sms-current-tab', 'upload');
  //hide all, then slide down the upload pane
  $('#searchdiv').hide();
  $('#browseresultdiv').hide();
  $('#uploaddiv').fadeIn("slow");
  $('#uploadetabimg').hide();
  $('#uploadtabimgsel').show();
  switchTabs('upload');
}

//show the browse tab
function showBrowsePane()
{
  setCookie('sms-current-tab', 'browse');
  var page = getCookie('sms-browse-pagestart');
  if(!page)
  {
    page = 0;
  }
  var contentLoaded = getCookie('sms-browse-content-loaded');
  if(!contentLoaded)
  {
    reloadSearchContent('/sms/metacat?action=query&anyfield=%25&returnfield=dataset/title&qformat=sms&pagesize=10&pagestart=' + page);
  }
  //hide all, then slide down the browse pane
  $('#searchdiv').hide();
  $('#uploaddiv').hide();
  $('#browseresultdiv').fadeIn("slow");
  $('#browsetabimg').hide();
  $('#browsetabimgsel').show();
  switchTabs('browse');
}

//switch to a given tab
function switchTabs(tab)
{
  if(tab == 'browse')
  {
    $('#searchtabimg').show();
    $('#uploadtabimg').show();
    $('#browsetabimg').hide();
    
    $('#uploadtabimgsel').hide();
    $('#browsetabimgsel').show();
    $('#searchtabimgsel').hide();
  }
  else if(tab == 'search')
  {
    $('#searchtabimg').hide();
    $('#uploadtabimg').show();
    $('#browsetabimg').show();
    
    $('#uploadtabimgsel').hide();
    $('#browsetabimgsel').hide();
    $('#searchtabimgsel').show();
  }
  else if(tab == 'upload')
  {
    $('#searchtabimg').show();
    $('#uploadtabimg').hide();
    $('#browsetabimg').show();
    
    $('#uploadtabimgsel').show();
    $('#browsetabimgsel').hide();
    $('#searchtabimgsel').hide();
  }
}

//slide an element up
function slideUp(id)
{
  $(id).slideUp("slow");
}

//slide and element down
function slideDown(id)
{
  $(id).slideDown("slow");
}

//set a cookie
function setCookie( name, value, expires, path, domain, secure ) 
{
  // set time, it's in milliseconds
  var today = new Date();
  today.setTime( today.getTime() );
  
  /*
  if the expires variable is set, make the correct 
  expires time, the current script below will set 
  it for x number of days, to make it for hours, 
  delete * 24, for minutes, delete * 60 * 24
  */
  if ( expires )
  {
    expires = expires * 1000 * 60 * 60 * 24;
  }
  var expires_date = new Date( today.getTime() + (expires) );
  
  document.cookie = name + "=" +escape( value ) +
  ( ( expires ) ? ";expires=" + expires_date.toGMTString() : "" ) + 
  ( ( path ) ? ";path=" + path : "" ) + 
  ( ( domain ) ? ";domain=" + domain : "" ) +
  ( ( secure ) ? ";secure" : "" );
}

//get a cookie
function getCookie( check_name ) {
	// first we'll split this cookie up into name/value pairs
	// note: document.cookie only returns name=value, not the other components
	var a_all_cookies = document.cookie.split( ';' );
	var a_temp_cookie = '';
	var cookie_name = '';
	var cookie_value = '';
	var b_cookie_found = false; // set boolean t/f default f
	
	for ( i = 0; i < a_all_cookies.length; i++ )
	{
		// now we'll split apart each name=value pair
		a_temp_cookie = a_all_cookies[i].split( '=' );
		
		// and trim left/right whitespace while we're at it
		cookie_name = a_temp_cookie[0].replace(/^\s+|\s+$/g, '');
	
		// if the extracted name matches passed check_name
		if ( cookie_name == check_name )
		{
			b_cookie_found = true;
			// we need to handle case where cookie has no value but exists (no = sign, that is):
			if ( a_temp_cookie.length > 1 )
			{
				cookie_value = unescape( a_temp_cookie[1].replace(/^\s+|\s+$/g, '') );
			}
			// note that in cases where cookie is initialized but no value, null is returned
			return cookie_value;
			break;
		}
		a_temp_cookie = null;
		cookie_name = '';
	}
	if ( !b_cookie_found )
	{
		return null;
	}
}		
