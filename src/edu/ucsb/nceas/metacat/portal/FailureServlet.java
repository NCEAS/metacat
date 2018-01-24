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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.uiuc.ncsa.myproxy.oa4mp.client.servlet.ClientServlet;

import java.io.PrintWriter;

/**
 * <p>Created by Jeff Gaynor<br>
 * on Aug 11, 2010 at  10:11:13 AM
 */
public class FailureServlet extends ClientServlet {
    protected void doIt(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Throwable {
        clearCookie(httpServletRequest, httpServletResponse); // clear out old session info
        httpServletResponse.setContentType("text/html");
        PrintWriter printWriter = httpServletResponse.getWriter();
        printWriter.println("<html>\n" +
                "<head><title>Failure</title></head>\n" +
                "<body><h1>Uh-oh...</h1>" +
                "<p>There was an error processing your request.</p>" +
                "<form name=\"input\" action=\"");
        printWriter.println(httpServletRequest.getContextPath() + "/\" method=\"get\">");
        printWriter.println("Click to go back to the main page<br><br>\n" +
                "<input type=\"submit\" value=\"Submit\" />\n" +
                "</form>\n" +
                "  </body>\n" +
                "</html>");
    }
}
