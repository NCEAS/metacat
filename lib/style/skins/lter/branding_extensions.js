 /*
  *   '$RCSfile$'
  *     Purpose: Default style sheet for KNB project web pages 
  *              Using this stylesheet rather than placing styles directly in 
  *              the KNB web documents allows us to globally change the 
  *              formatting styles of the entire site in one easy place.
  *   Copyright: 2000 Regents of the University of California and the
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

/**
 *  inserts an iframe into the document and assigns it the passed source URL
 *  and class attribute
 */
function _createAutoscrollIFrameWithURL(targetURL, cssClass) {


  if (_isBlank(targetURL)) {

    document.write("&nbsp;");

  } else {

    document.write("<iframe src=\""+targetURL+"\" class=\""+cssClass+"\" "
                  +" id=\"" + cssClass + "\""
                  +" name=\"" + cssClass + "\""
                  + "\" marginwidth=\"0\" scrolling=\"auto\" "
                  +" marginheight=\"0\" marginwidth=\"0\" "
                  +" border=\"0\" frameborder=\"0\" framespacing=\"0\" "
                  +" hspace=\"0\" vspace=\"0\">Your browser does not support"
                  +" the iframe tag. <a href=\""+targetURL+"\" "
                  +"target=\"_blank\">This content</a>"
                  +" should have been displayed at this location</iframe>");
  }
}
/**
 *  inserts the header referenced by the ADVANCED_BROWSEBOX_URL setting
 */
function insertAdvancedBrowseBox() { 

  if (!_isBlank(ADVANCED_BROWSEBOX_URL)) {

    _createIFrameWithURL(ADVANCED_BROWSEBOX_URL, IFRAME_ADVANCED_BROWSEBOX_CLASS);
  }

}

/**
 *  inserts the header referenced by the LOGINBOX_URL setting
 */
function insertLoginBox(loginFailure, serverContextUrl) { 

  if (!_isBlank(LOGINBOX_URL)) {
  
    LOGINBOX_URL = prependUrl(serverContextUrl, LOGINBOX_URL);

    if (!_isBlank(loginFailure)) {
      LOGINBOX_URL = LOGINBOX_URL + "?loginFailure=" + loginFailure;
    }
 
    _createIFrameWithURL(LOGINBOX_URL, IFRAME_LOGINBOX_CLASS);
  }

}


/**
 *  inserts a URL to the metacat server to read a document
 */
function insertDocument(servletURL, docid) { 

  if (!_isBlank(docid)) {
  
    documentURL = servletURL + "?action=read&docid=" + docid + "&qformat=lter&insertTemplate=0";

    _createAutoscrollIFrameWithURL(documentURL, IFRAME_DOCUMENT_CLASS);
  }

}
