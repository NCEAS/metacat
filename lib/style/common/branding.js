 /*
  *     Purpose: Default style sheet for KNB project web pages
  *              Using this stylesheet rather than placing styles directly in
  *              the KNB web documents allows us to globally change the
  *              formatting styles of the entire site in one easy place.
  */


/**
 *  NOTE: THIS SCRIPT EXPECTS YOU ALREADY TO HAVE IMPORTED THE FOLLOWING
 *  VARIABLES, WHICH ARE TYPICALLY DEFINED IN style/skins/qformat/qformat.js:
 *
 *   Location of the header that will be displayed at the top of the page
 *  HEADER_URL 
 *
 *   Location of the header that will be displayed at the top of the page
 *  LEFTCOL_URL 
 *
 *   Location of the header that will be displayed at the top of the page
 *  RIGHTCOL_URL 
 *
 *   Location of the header that will be displayed at the top of the page
 *  FOOTER_URL 
 *
 * header iframe class
 *  IFRAME_HEADER_CLASS
 *
 * left column iframe class
 *  IFRAME_LEFTCOL_CLASS
 *
 * right column iframe class
 *  IFRAME_RIGHTCOL_CLASS
 *
 * footer iframe class
 *  IFRAME_FOOTER_CLASS
 *
 * entire table class
 *  TEMPLATE_TABLE_CLASS
 *
 * header table-cell class. Note you should not set css "width" on this, since it 
 * includes a colspan
 *  TEMPLATE_HEADERROW_CLASS
 *
 * left column table-cell class. Note that restricting css "height" on this may 
 * affect visibility of the main content, since it's in the same table row 
 *  TEMPLATE_LEFTCOL_CLASS
 *
 * main central content table-cell class. Note that css attributes set here may 
 * apply to the content nested inside this cell
 *  TEMPLATE_CONTENTAREA_CLASS
 *
 * rigth column table-cell class. Note that restricting css "height" on this may 
 * affect visibility of the main content, since it's in the same table row 
 *  TEMPLATE_RIGHTCOL_CLASS
 *
 * footer table-cell class. Note you should not set "width" on this, since it 
 * includes a colspan
 *  TEMPLATE_FOOTERROW_CLASS
 */
 
function prependUrl(prefix, path) {

	var retUrl = path;
	if ( !_isBlank(path) ) {
		if ( !_isBlank(prefix) ) {
			//check if absolute
			if (path.indexOf("http") < 0) {
				//check if the server has been assumed with a leading "/"
				if (path.indexOf("/") != 0) {
					retUrl = prefix + "/" + path;
				}
			}
		}
	}
	return retUrl;
}


/**
 *  inserts the first half of the template table that surrounds the page's'
 *  content, including the the optional header and left column areas
 *  referenced by the HEADER_URL and LEFTCOL_URL settings
 */
function insertTemplateOpening(serverContextUrl) {

  //table opening tag
  document.write("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" "
                                  +" class=\""+TEMPLATE_TABLE_CLASS+"\">");
  //first row is header
  document.write("<tr><td "+_getColSpanString()+" class=\""+TEMPLATE_HEADERROW_CLASS+"\">");

  //make any relative paths into absolute paths
  HEADER_URL = prependUrl(serverContextUrl, HEADER_URL);
	
  //content for the header (if any)
  _createIFrameWithURL(HEADER_URL, IFRAME_HEADER_CLASS);

  document.write("</td></tr><tr>");

  //content for the left column (if any)
  if (!_isBlank(LEFTCOL_URL)) {

    document.write("<td class=\""+TEMPLATE_LEFTCOL_CLASS+"\">");

	//make any relative paths into absolute paths
	LEFTCOL_URL = prependUrl(serverContextUrl, LEFTCOL_URL);
  
    _createIFrameWithURL(LEFTCOL_URL, IFRAME_LEFTCOL_CLASS);

    document.write("</td>");
  }

  //main content area
  document.write("<td class=\""+TEMPLATE_CONTENTAREA_CLASS+"\">");
}

/**
 *  inserts the last half of the template table that surrounds the page's'
 *  content, including the optional right column and footer areas
 *  referenced by the RIGHTCOL_URL and FOOTER_URL settings
 */
function insertTemplateClosing(serverContextUrl) {

  //right column
  document.write("</td>");

  //content for the right column (if any)
  if (!_isBlank(RIGHTCOL_URL)) {

    document.write("<td class=\""+TEMPLATE_RIGHTCOL_CLASS+"\">");

	//make any relative paths into absolute paths
	RIGHTCOL_URL = prependUrl(serverContextUrl, RIGHTCOL_URL);
	
    _createIFrameWithURL(RIGHTCOL_URL, IFRAME_RIGHTCOL_CLASS);

    document.write("</td>");
  }

  //last row is footer
  document.write("</tr><tr><td "+_getColSpanString()+" class=\""
                                              +TEMPLATE_FOOTERROW_CLASS+"\">");

  //make any relative paths into absolute paths
  FOOTER_URL = prependUrl(serverContextUrl, FOOTER_URL);
	
  //content for the footer (if any)
  _createIFrameWithURL(FOOTER_URL, IFRAME_FOOTER_CLASS);

  //close table
  document.write("</td></tr></table>");

}


/**
 *  inserts the header referenced by the SEARCHBOX_URL setting
 */
function insertSearchBox(serverContextUrl) { 

  if (!_isBlank(SEARCHBOX_URL)) {
  
	//make any relative paths into absolute paths
	SEARCHBOX_URL = prependUrl(serverContextUrl, SEARCHBOX_URL);

    _createIFrameWithURL(SEARCHBOX_URL, IFRAME_SEARCHBOX_CLASS);
  }

}


/**
 *  inserts the header referenced by the SEARCHBOX_URL setting
 */
function insertMap(serverContextUrl) { 

  if (!_isBlank(MAP_URL)) {
  	//make any relative paths into absolute paths
	MAP_URL = prependUrl(serverContextUrl, MAP_URL);

    _createIFrameWithURL(MAP_URL, IFRAME_MAP_CLASS);
  }

}

/**
 *  inserts the header referenced by the ADVANCED_SEARCHBOX_URL setting
 */
function insertAdvancedSearchBox(serverContextUrl) { 

  if (!_isBlank(ADVANCED_SEARCHBOX_URL)) {
	//make any relative paths into absolute paths
	ADVANCED_SEARCHBOX_URL = prependUrl(serverContextUrl, ADVANCED_SEARCHBOX_URL);
	
    _createIFrameWithURL(ADVANCED_SEARCHBOX_URL, IFRAME_ADVANCED_SEARCHBOX_CLASS);
  }

}

/**
 *  inserts the header referenced by the LOGINBOX_URL setting
 */
function insertLoginBox(serverContextUrl) { 

  if (!_isBlank(LOGINBOX_URL)) {
  
  	//make any relative paths into absolute paths
	LOGINBOX_URL = prependUrl(serverContextUrl, LOGINBOX_URL);

    _createIFrameWithURL(LOGINBOX_URL, IFRAME_LOGINBOX_CLASS);
  }

}


/**
 *  inserts an iframe into the document and assigns it the passed source URL
 *  and class attribute
 */
function _createIFrameWithURL(targetURL, cssClass) {


  if (_isBlank(targetURL)) {

    document.write("&nbsp;");

  } else {

    document.write("<iframe src=\""+targetURL+"\" class=\""+cssClass+"\" "
                  +" id=\"" + cssClass + "\""
                  +" name=\"" + cssClass + "\""
				  + "\" marginwidth=\"0\" scrolling=\"no\" "
                  +" marginheight=\"0\" marginwidth=\"0\" scrolling=\"no\" "
                  +" border=\"0\" frameborder=\"0\" framespacing=\"0\" "
                  +" hspace=\"0\" vspace=\"0\">Your browser does not support"
                  +" the iframe tag. <a href=\""+targetURL+"\" "
                  +"target=\"_blank\">This content</a>"
                  +" should have been displayed at this location</iframe>");
  }
}



function _isBlank(testString) {

  return (  !testString
          || testString==null
          || (testString.replace(/^\s*/, '').replace(/\s*$/,'')==""));
}


function _getColSpanString() {

  var colspan = 1;
  if (!_isBlank(LEFTCOL_URL))  colspan++;
  if (!_isBlank(RIGHTCOL_URL)) colspan++;
  if (colspan==1) return "";
  else return " colspan=\""+colspan+"\" ";
} 

function submitbrowseform(action,form_ref) {
  form_ref.action.value=action;
  form_ref.submit();
}

/**
* This uses jQuery to load access log information
**/
function loadStats(divId, docId, url, qformat) {
	var params = 
	{
		'action': "getlog",
		'docid': docId,
		'qformat': qformat
	};
	load(url, params, divId);
}
/**
* This uses jQuery to call any Metacat method with the given params
**/
function load(url, params, divId) {
	try {
		// call back function when loading finishes
		var callback = 
			function(response, status, xhr) {
				// error
				if (status == "error") {
					var msg = "Sorry but there was an error: ";
					$("#error").html(msg + xhr.status + " " + xhr.statusText);
				}
			};
		// replace div with content	
		if (divId) {
			$("#" + divId).load(
				url, 
				params,
				callback);
			} else {
				// just post without replacing content
				$.post(
				url, 
				params,
				callback);
			}	
		} catch (e) {
			// do nothing - jQuery was probably not included
		}
}