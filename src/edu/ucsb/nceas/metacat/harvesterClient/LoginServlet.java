/**
 *  '$RCSfile$'
 *  Copyright: 2004 University of New Mexico and the 
 *                  Regents of the University of California
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.ucsb.nceas.metacat.harvesterClient;

import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUtils;
import javax.servlet.ServletOutputStream;
import edu.ucsb.nceas.metacat.AuthSession;

/**
 *  LoginServlet implements a Harvester servlet to login to Metacat
 */
public class LoginServlet extends HttpServlet {

  public void destroy() {
    // Close all connections
    System.out.println("Destroying LoginServlet");
  }

  /**
   *  Handle "GET" method requests from HTTP clients
   *
   *  @param  request   The request
   *  @param  response  The response
   *  @throws ServletException, java.io.IOException
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, java.io.IOException {
    // Process the data and send back the response
    handleGetOrPost(request, response);
  }

  /**
   *  Handle "POST" method requests from HTTP clients
   *
   *  @param  request   The request
   *  @param  response  The response
   *  @throws ServletException, java.io.IOException
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, java.io.IOException {
    // Process the data and send back the response
    handleGetOrPost(request, response);
  }

  /**
   *  Handle "GET" or "POST" method requests from HTTP clients
   *
   *  @param  request   The request
   *  @param  response  The response
   *  @throws ServletException, java.io.IOException
   */
  private void handleGetOrPost(HttpServletRequest request,
                               HttpServletResponse response)
          throws ServletException, java.io.IOException {
    AuthSession authSession = null;
    HttpSession httpSession;
    boolean isValid;
    PrintWriter out = response.getWriter();
    String passwd = request.getParameter("passwd");
    String user = request.getParameter("user");

    response.setContentType("text/plain");

    try {
      authSession = new AuthSession();
    } 
    catch (Exception e) {
      out.println("Error creating AuthSession: " + e.getMessage());
      return;
    }

    isValid = authSession.authenticate(request, user, passwd);
    
    if (isValid) {
      System.out.println(authSession.getMessage());
      httpSession = request.getSession(true);
      httpSession.setAttribute("username", user);
      httpSession.setAttribute("password", passwd);
      response.sendRedirect("../style/skins/dev/harvesterUpload.html");
    }
    else {
      out.println("Error authenticating Metacat login: " + 
                  authSession.getMessage());
    }
  }
}
