/*
  *   '$RCSfile$'
  *     Purpose: Default javascript library for PARC project web pages 
  *              Using this library rather than placing javascript directly in 
  *              the PARC web documents allows us to globally change the 
  *              functions of the entire site in one easy place.
  *   Copyright: 2000 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *     Authors: Matt Jones, Christopher Jones
  *
  *    '$Author: leinfelder $'
  *      '$Date: 2008-06-17 14:16:32 -0600 (Tue, 17 Jun 2008) $'
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

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * *  CONFIGURATION SETTINGS - EDIT THESE FOR YOUR ENVIRONMENT * * * * 
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

 
/* loginStatus: Uses XHR request to check for current session login status */
function loginStatus(servletPath, cgiPrefix){
    var httpRequest = false;
    try {
          httpRequest = new XMLHttpRequest();
    } catch (trymicrosoft) {
        try {
            httpRequest = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (othermicrosoft) {
            try {
                httpRequest = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (failed) {
                request = null;
            }
        }
    }

    if (!httpRequest){
        alert("no httpRequest object");
        return false;
    }

    httpRequest.onreadystatechange = function() {
        if(httpRequest.readyState == 4) {
            if (httpRequest.status == 200) {
                var response = httpRequest.responseText;
                var login_block = document.getElementById('login_block');

                if (response.indexOf("public") > 0) {
                    login_block.innerHTML = '<a href="' + cgiPrefix + '/register-dataset.cgi?cfg=parc&' +
                                            'stage=loginform">Login</a>';
                }
                else
                {
                    login_block.innerHTML = '<a href="' + cgiPrefix + '/register-dataset.cgi?cfg=parc&' +
                                            'stage=logout">Logout</a>';
                }
            }
        }
    }

    httpRequest.open("GET", servletPath + "?action=getloggedinuserinfo", true);
    httpRequest.send(null);
}
