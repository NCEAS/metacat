/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * *  CONFIGURATION SETTINGS - EDIT THESE FOR YOUR ENVIRONMENT * * * * 
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

 
//  These settings allow you to include and display common content (eg a common 
//  header) on all your pages, in much the same way as a frameset allows you to 
//  do, but through the use of iframes and a table instead. You can include up 
//  to 4 external pages, each one within the header, footer, left or right areas
//
//  looks like this (if you're using a fixed width font to display these notes):
//    ___________________
//    |     header      |
//    |-----------------|
//    | |             | |
//    | |             | |
//    |L|   content   |R|
//    | |             | |
//    | |             | |
//    |-----------------|
//    |     footer      | 
//    -------------------
//
//  Each area may display another page on the local site, or a page on a 
//  different server, or may be set to display nothing (in which case an iframe 
//  will not be drawn, although the containing table cell will still need to be 
//  resized using the css style - see below) 
//
//  NOTES:
//
//  1) if you have any links in the included documents, the target attribute for 
//     these *MUST* be set to _top, otherwise the new document will be displayed 
//     inside the small iframe areas, instead of replacing the entire page!
//     - example: <a href="index.html" target="_top">HOME</a>
//   
//  2) you will need to set the correct iframe size, in order to accomodate 
//     each of these areas on the page. The default location for these size 
//     settings is in the default.css file - see the "IFRAME_XXXXXX_CLASS" 
//     variables (below) for the name of the style to edit
//
//  3) you will also need to set the correct table cell sizes and/or overall 
//     table size for similar reasons. The default location for these size 
//     settings is in the default.css file - see the "TEMPLATE_XXXXXX_CLASS" 
//     variables (below) for the name of the style to edit



////////////////////////////////////////////////////////////////////////////////
//  Edit these variables to define the content that will be loaded into the 
//  various iframes. Each may be a relative path to another page on the local 
//  site, or a full URL to a page on a remote server, or may be set to the empty 
//  string if no content is required at that position on the page (and in which  
//  case an iframe will not be drawn, although an empty table cell will still  
//  exist unless it is resized smaller) . 
//  ( e.g. if you do not want a header to be included, set: HEADER_URL="";)
////////////////////////////////////////////////////////////////////////////////


//  Location of the header that will be displayed at the top of the page
var HEADER_URL 
  = "@systemidserver@/@context@@style-skins-relpath@/<@scope@>/header.html";

// Location of the search box that will be displayed above the  
//  results on the results page (optional)
var SEARCHBOX_URL 
  = "";
  //= "@systemidserver@/@context@@style-skins-relpath@/<@scope@>/searchform.html";

//  Location of the header that will be displayed at the top of the page
var LEFTCOL_URL 
  = "";
  
//  Location of the header that will be displayed at the top of the page
var RIGHTCOL_URL 
  = "";
  
//  Location of the header that will be displayed at the top of the page
var FOOTER_URL 
  = "";
  



/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
/* * * * * * * * *  MAY CHANGE THE FOLLOWING, BUT SHOULDN'T NEED TO* * * * * */ 
/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

  
////////////////////////////////////////////////////////////////////////////////
//  Edit the default.css file to set the correct iframe sizes to accomodate the 
//  header, footer, left and right areas.
//  The following variables set the names of the styles that will be applied to 
//  each of the iframes - they can be anythign you wish, provided you use the 
//  same names for your classes in the css file
//  NOTE: these styles apply only to each container frame, *NOT* to the document 
//  within it!
////////////////////////////////////////////////////////////////////////////////

//header iframe class
var IFRAME_HEADER_CLASS         = "iframeheaderclass";

//(metacat only) search box iframe class
var IFRAME_SEARCHBOX_CLASS      = "iframesearchboxclass";

//left column iframe class
var IFRAME_LEFTCOL_CLASS        = "iframeleftcolclass";

//right column iframe class
var IFRAME_RIGHTCOL_CLASS       = "iframerightcolclass";

//footer iframe class
var IFRAME_FOOTER_CLASS         = "iframefooterclass";


////////////////////////////////////////////////////////////////////////////////
//  Edit the default.css file to set the correct table sizes to accomodate the 
//  header, footer, left and right iframes.
//  The following variables set the names of the styles that will be applied to 
//  each of the table cells (or the table itself - see below) - they can be 
//  anything you wish, provided you use the same names for your classes in the 
//  css file
//  NOTE: these styles apply only to each table cell, *NOT* to the document 
//  inside the iframe that is nested within it! (the exception is 
//  TEMPLATE_CONTENTAREA_CLASS, since the content probably isn't within an 
//  iframe - so style elements in this class will apply to the content istelf)
////////////////////////////////////////////////////////////////////////////////

//entire table class
var TEMPLATE_TABLE_CLASS        = "templatetableclass";

//header table-cell class. Note you should not set css "width" on this, since it 
//includes a colspan
var TEMPLATE_HEADERROW_CLASS    = "templateheaderrowclass";

//left column table-cell class. Note that restricting css "height" on this may 
//affect visibility of the main content, since it's in the same table row 
var TEMPLATE_LEFTCOL_CLASS      = "templateleftcolclass";

//main central content table-cell class. Note that css attributes set here may 
//apply to the content nested inside this cell
var TEMPLATE_CONTENTAREA_CLASS  = "templatecontentareaclass";

//rigth column table-cell class. Note that restricting css "height" on this may 
//affect visibility of the main content, since it's in the same table row 
var TEMPLATE_RIGHTCOL_CLASS     = "templaterightcolclass";

//footer table-cell class. Note you should not set "width" on this, since it 
//includes a colspan
var TEMPLATE_FOOTERROW_CLASS    = "templatefooterrowclass";


