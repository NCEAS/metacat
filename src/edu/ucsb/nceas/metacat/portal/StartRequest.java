/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package edu.ucsb.nceas.metacat.portal;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.uiuc.ncsa.myproxy.oa4mp.client.OA4MPResponse;
import edu.uiuc.ncsa.myproxy.oa4mp.client.servlet.ClientServlet;
import edu.uiuc.ncsa.myproxy.oa4mp.client.storage.AssetStoreUtil;
import edu.uiuc.ncsa.security.core.Identifier;
import edu.uiuc.ncsa.security.core.exceptions.ServerSideException;
import edu.uiuc.ncsa.security.servlet.JSPUtil;

/**
 * A very simple sample servlet showing how a portal can start delegation. This just does the
 * initial request then a redirect
 * so there is nothing to display to the user.
 * <p>Created by Jeff Gaynor<br>
 * on Jun 18, 2010 at  2:10:58 PM
 */
public class StartRequest extends ClientServlet {

	@Override
    protected void doIt(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        info("1.a. Starting transaction");
        OA4MPResponse gtwResp = null;
        // Drumroll please: here is the work for this call.
        try {
            Identifier id = AssetStoreUtil.createID();
            gtwResp = getOA4MPService().requestCert(id);
            // if there is a store, store something in it.
            Cookie cookie = new Cookie(OA4MP_CLIENT_REQUEST_ID, id.getUri().toString());
            response.addCookie(cookie);

        } catch (Throwable t) {

            if (t instanceof ServerSideException) {
                ServerSideException sse = (ServerSideException) t;
                //nothing was, in fact, returned from the server.
                if (!sse.isTrivial()) {
                    if (getCE().isDebugOn()) {
                        t.printStackTrace();
                    }
                    for (String key : sse.getQueryParameters().keySet()) {
                        request.setAttribute(key, sse.getQueryParameters().get(key));
                    }
                    String contextPath = request.getContextPath();
                    if (!contextPath.endsWith("/")) {
                        contextPath = contextPath + "/";
                    }
                    request.setAttribute("action", contextPath);
                    JSPUtil.handleException(sse.getCause(), request, response, "/pages/client-error.jsp");
                    if (sse.getRedirect() != null) {
                        response.sendRedirect(sse.getRedirect().toString());
                    }
                    return;
                }

                JSPUtil.handleException(t, request, response, "/pages/client-error.jsp");
                return;
            }
            throw t;
        }
        
        String target = request.getParameter("target");
    	if (target != null) {
        	request.getSession().setAttribute("target", target);
    	}
        response.sendRedirect(gtwResp.getRedirect().toString());
    }
}
